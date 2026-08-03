package com.cryptosafe.app.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cryptosafe.app.data.AppDatabase
import com.cryptosafe.app.security.SecurePasswordStorage
import java.util.concurrent.TimeUnit

class AutoDeleteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val appContext = applicationContext
            val dbFile = appContext.getDatabasePath("cryptosafe.db")
            val hasKey = SecurePasswordStorage.getDatabasePassphrase() != null
            if (dbFile.exists() && dbFile.length() > 0 && !hasKey) {
                com.cryptosafe.app.DiagnosticsLogger.logEvent("WARN", "auto_delete_worker_skipped_key_missing")
                return Result.failure()
            }
            val passphrase = SecurePasswordStorage.getOrCreateDatabasePassphrase()
            val database = AppDatabase.getInstance(appContext, passphrase)
            val boxDao = database.boxDao()

            val boxes = boxDao.getAllBoxesSync()

            for (box in boxes) {
                val autoDeleteHours = box.autoDeleteHours
                if (autoDeleteHours != null && autoDeleteHours > 0) {
                    val cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(autoDeleteHours.toLong())
                    boxDao.deleteMessagesBefore(box.id, cutoffTime)
                }
            }

            Result.success()
        } catch (e: Exception) {
            com.cryptosafe.app.DiagnosticsLogger.logEvent(
                "WARN",
                "auto_delete_worker_failed class=${e.javaClass.simpleName}"
            )
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "auto_delete_messages"

        fun schedule(context: Context) {
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<AutoDeleteWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            androidx.work.WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }
    }
}
