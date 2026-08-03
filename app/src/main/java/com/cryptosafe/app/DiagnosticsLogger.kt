package com.cryptosafe.app

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import com.cryptosafe.app.security.SecurePasswordStorage
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * سجل تشخيص محلي بالكامل وآمن:
 * 1. يلتقط أي انهيار قاتل (FATAL) عبر معالج الاستثناءات العام.
 * 2. عند كل إقلاع يرصد إن كانت الجلسة السابقة انتهت بشكل غير طبيعي
 *    (API 30+: عبر ApplicationExitInfo حتى للانهيارات الأصلية/ANR،
 *    وإلا عبر علامة جلسة).
 * 3. يسجل حالة قاعدة البيانات عند الإقلاع (وجود الملف، حجمه،
 *    وجود مفتاح التشفير) — بدون أي محتوى حساس.
 *
 * ضمان الخصوصية: الملف يحتوي فقط تواريخ زمنية، أسماء كلاسات،
 * وأسطر stack trace (كلاس.دالة(ملف:سطر)). لا يُكتب هنا أبداً:
 * محتوى الرسائل، كلمات المرور، المفاتيح، أو رسائل الاستثناءات النصية.
 */
object DiagnosticsLogger {
    private const val MAX_LOG_BYTES = 262_144L
    private const val MAX_TRACE_LINES = 80
    private const val DIR = "diagnostics"
    private const val LOG_FILE = "events.log"
    private const val OLD_FILE = "events.old.log"
    private const val EXPORT_FILE = "diagnostics_export.txt"
    private const val PREFS = "diagnostics_prefs"
    private const val KEY_SESSION = "session_active"

    private var appContext: Context? = null
    private var initialized = false

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun initialize(app: Context) {
        if (initialized) return
        appContext = app.applicationContext
        val context = appContext ?: return

        // معالج الانهيارات يُسجَّل فوراً؛ كل العمل الثقيل (قراءة/كتابة ملفات،
        // استعلام أسباب الخروج) بالخلفية حتى لا يؤخر أول إطار للتطبيق.
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logThrowable(throwable, thread)
            previous?.uncaughtException(thread, throwable)
        }

        initialized = true

        Thread {
            try {
                rotateIfOversized()
                logEvent(
                    "INFO",
                    "app_start" +
                        " version=${getAppVersion(context)}" +
                        " sdk=${Build.VERSION.SDK_INT}" +
                        " device=${Build.MANUFACTURER} ${Build.MODEL}" +
                        " locale=${Locale.getDefault().toLanguageTag()}"
                )
                detectAbnormalTermination(context)
                logDatabaseState(context)
            } catch (_: Exception) {
            }
        }.start()
    }

    private fun detectAbnormalTermination(context: Context) {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val reasons = am.getHistoricalProcessExitReasons(context.packageName, 0, 5)
                var loggedHeader = false
                for (reason in reasons) {
                    val abnormal = reason.reason == ApplicationExitInfo.REASON_CRASH ||
                        reason.reason == ApplicationExitInfo.REASON_CRASH_NATIVE ||
                        reason.reason == ApplicationExitInfo.REASON_ANR ||
                        reason.reason == ApplicationExitInfo.REASON_INITIALIZATION_FAILURE
                    if (!abnormal) continue
                    if (!loggedHeader) {
                        logEvent("WARN", "previous_abnormal_termination_detected")
                        loggedHeader = true
                    }
                    logEvent("PREV", "reason=${reason.reason} process=${reason.processName}")
                    try {
                        reason.getTraceInputStream()?.bufferedReader()?.use { reader ->
                            appendTrace(extractFrames(reader.readText()))
                        }
                    } catch (_: Exception) {
                    }
                }
            } catch (e: Exception) {
                logEvent("WARN", "exit_reason_query_failed class=${e.javaClass.simpleName}")
            }
        } else {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_SESSION, false)) {
                logEvent("WARN", "previous_session_unexpected_termination")
            }
            prefs.edit().putBoolean(KEY_SESSION, true).apply()
        }
    }

    /** يُستدعى عند إغلاق التطبيق بشكل طبيعي لتصفير علامة الجلسة (الأجهزة القديمة فقط). */
    fun markCleanExit(context: Context) {
        if (Build.VERSION.SDK_INT >= 30) return
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SESSION, false).apply()
        } catch (_: Exception) {
        }
    }

    /** يسجل حالة قاعدة البيانات دون أي محتوى حساس. */
    fun logDatabaseState(context: Context) {
        try {
            val db = context.getDatabasePath("cryptosafe.db")
            val exists = db.exists()
            val size = if (exists) db.length() else -1L
            val hasPassphrase = SecurePasswordStorage.getDatabasePassphrase() != null
            logEvent("INFO", "db_state exists=$exists size=$size passphrase_stored=$hasPassphrase")
        } catch (e: Exception) {
            logEvent("WARN", "db_state_query_failed class=${e.javaClass.simpleName}")
        }
    }

    fun logEvent(level: String, message: String) {
        val context = appContext ?: return
        val line = "[${dateFormat.format(Date())}] [$level] $message\n"
        writeFile(context, line)
    }

    private fun logThrowable(throwable: Throwable, thread: Thread) {
        val context = appContext ?: return
        val sb = StringBuilder()
        sb.append("[${dateFormat.format(Date())}] [FATAL] uncaught_${throwable.javaClass.name}\n")
        sb.append("    thread=${thread.name}\n")
        appendStackFrames(sb, throwable)
        writeFile(context, sb.toString())
    }

    private fun appendStackFrames(sb: StringBuilder, throwable: Throwable) {
        var current: Throwable? = throwable
        var depth = 0
        while (current != null && depth < 6) {
            if (depth > 0) sb.append("    caused_by ${current.javaClass.name}\n")
            val frames = current.stackTrace
            for (i in 0 until minOf(frames.size, MAX_TRACE_LINES)) {
                val f = frames[i]
                sb.append("    at ${f.className}.${f.methodName}(${f.fileName}:${f.lineNumber})\n")
            }
            current = current.cause
            depth++
        }
    }

    /** يبقي أسطر الـ stack trace فقط ويرفض أي شيء آخر (رسائل، محتوى). */
    private fun extractFrames(trace: String): String {
        val sb = StringBuilder()
        trace.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("at ") || trimmed.startsWith("Caused by:")) {
                sb.append(line).append('\n')
            }
        }
        return sb.toString()
    }

    private fun appendTrace(trace: String) {
        if (trace.isBlank()) return
        val context = appContext ?: return
        writeFile(context, "\n--- previous_trace ---\n$trace--- end ---\n")
    }

    private fun rotateIfOversized() {
        val context = appContext ?: return
        try {
            val f = File(File(context.filesDir, DIR), LOG_FILE)
            if (f.exists() && f.length() > MAX_LOG_BYTES) rotate(context)
        } catch (_: Exception) {
        }
    }

    private fun rotate(context: Context) {
        try {
            val dir = File(context.filesDir, DIR)
            File(dir, OLD_FILE).delete()
            File(dir, LOG_FILE).renameTo(File(dir, OLD_FILE))
        } catch (_: Exception) {
        }
    }

    @Synchronized
    private fun writeFile(context: Context, content: String) {
        try {
            val dir = File(context.filesDir, DIR)
            if (!dir.exists()) dir.mkdirs()
            val f = File(dir, LOG_FILE)
            if (f.exists() && f.length() + content.length > MAX_LOG_BYTES) rotate(context)
            FileWriter(f, true).use { it.append(content) }
        } catch (_: Exception) {
        }
    }

    /** يعيد ملف تصدير واحد يضم السجل الحالي + السابق (إن وجد). */
    fun getExportFile(): File? {
        val context = appContext ?: return null
        return try {
            val dir = File(context.filesDir, DIR)
            if (!dir.exists()) return null
            val out = File(dir, EXPORT_FILE)
            out.delete()
            FileWriter(out).use { writer ->
                val old = File(dir, OLD_FILE)
                if (old.exists()) writer.append(old.readText())
                val current = File(dir, LOG_FILE)
                if (current.exists()) writer.append(current.readText())
            }
            if (out.length() > 0) out else null
        } catch (_: Exception) {
            null
        }
    }

    /** معاينة مختصرة للسجل (آخر الأحرف) لعرضها داخل الإعدادات. */
    fun getLogPreview(maxChars: Int = 4000): String {
        val file = getExportFile() ?: return ""
        return try {
            file.readText().takeLast(maxChars)
        } catch (_: Exception) {
            ""
        }
    }

    fun deleteLogs() {
        val context = appContext ?: return
        try {
            val dir = File(context.filesDir, DIR)
            File(dir, LOG_FILE).delete()
            File(dir, OLD_FILE).delete()
            File(dir, EXPORT_FILE).delete()
        } catch (_: Exception) {
        }
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName}(${info.versionCode})"
        } catch (_: Exception) {
            "unknown"
        }
    }
}
