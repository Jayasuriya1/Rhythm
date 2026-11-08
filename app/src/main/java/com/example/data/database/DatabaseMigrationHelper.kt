package com.example.data.database

import android.content.Context
import android.util.Log
import com.example.data.security.DatabaseKeyManager
import java.io.File

object DatabaseMigrationHelper {

    private const val TAG = "DatabaseMigrationHelper"
    private const val DB_NAME = "rhythm_focus_database"

    fun migratePlaintextToEncryptedIfNeeded(context: Context, key: ByteArray) {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists() || dbFile.length() == 0L) {
            // New database, Room will create it encrypted
            return
        }

        if (!DatabaseKeyManager.isPlaintextSqliteDatabase(dbFile)) {
            // Already encrypted or non-standard
            return
        }

        Log.i(TAG, "Plaintext SQLite database detected at ${dbFile.absolutePath}. Initiating safe in-place migration to SQLCipher.")

        val backupFile = File(dbFile.parentFile, "$DB_NAME.plaintext_bak")
        val encryptedTempFile = File(dbFile.parentFile, "$DB_NAME.encrypted_tmp")

        try {
            // 1. Copy plaintext DB to temporary backup
            dbFile.copyTo(backupFile, overwrite = true)

            // 2. Export plaintext DB to encrypted destination using SQLCipher export
            net.zetetic.database.sqlcipher.SQLiteDatabase.openOrCreateDatabase(
                dbFile.absolutePath,
                "".toByteArray(),
                null,
                null
            ).use { plainDb ->
                // Convert passphrase bytes to hex string for ATTACH KEY
                val hexKey = key.joinToString("") { "%02x".format(it) }
                plainDb.rawExecSQL("ATTACH DATABASE '${encryptedTempFile.absolutePath}' AS encrypted KEY \"x'$hexKey'\";")
                plainDb.rawExecSQL("SELECT sqlcipher_export('encrypted');")
                plainDb.rawExecSQL("DETACH DATABASE encrypted;")
            }

            // 3. Verify encrypted database can be opened with the key
            net.zetetic.database.sqlcipher.SQLiteDatabase.openOrCreateDatabase(
                encryptedTempFile.absolutePath,
                key,
                null,
                null
            ).use { encDb ->
                val version = encDb.version
                Log.i(TAG, "Verification successful. Encrypted DB version: $version")
            }

            // 4. Atomically replace original with encrypted
            if (encryptedTempFile.renameTo(dbFile)) {
                backupFile.delete()
                Log.i(TAG, "In-place database encryption migration completed successfully.")
            } else {
                encryptedTempFile.copyTo(dbFile, overwrite = true)
                encryptedTempFile.delete()
                backupFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate plaintext database to SQLCipher, restoring backup", e)
            if (backupFile.exists()) {
                backupFile.copyTo(dbFile, overwrite = true)
                backupFile.delete()
            }
            if (encryptedTempFile.exists()) {
                encryptedTempFile.delete()
            }
            throw e
        }
    }
}

