package com.cryptosafe.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [Box::class, Message::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun boxDao(): BoxDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        
        
        private var openedWithPassphrase: ByteArray? = null

        fun getInstance(context: Context, passphrase: ByteArray): AppDatabase {
            val instance = INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, passphrase).also {
                    INSTANCE = it
                    openedWithPassphrase = passphrase.copyOf()
                }
            }
            val opened = openedWithPassphrase
            if (opened != null && !opened.contentEquals(passphrase)) {
                com.cryptosafe.app.DiagnosticsLogger.logEvent(
                    "WARN",
                    "db_open_passphrase_mismatch"
                )
            }
            return instance
        }

        
        
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE boxes ADD COLUMN encryptionSalt TEXT")
            }
        }

        private fun buildDatabase(context: Context, passphrase: ByteArray): AppDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "cryptosafe.db"
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}