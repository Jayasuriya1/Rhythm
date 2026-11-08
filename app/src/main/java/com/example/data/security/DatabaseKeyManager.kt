package com.example.data.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object DatabaseKeyManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "rhythm_db_encryption_master_key"
    private const val PREFS_NAME = "rhythm_database_security_prefs"
    private const val PREF_ENCRYPTED_DB_KEY = "encrypted_db_passphrase"
    private const val PREF_DB_KEY_IV = "db_passphrase_iv"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val DB_KEY_SIZE_BYTES = 32

    private val SQLITE_HEADER_PREFIX = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    @Volatile
    private var cachedKey: ByteArray? = null

    fun isSqlCipherSupported(): Boolean {
        val fingerprint = Build.FINGERPRINT ?: ""
        val hardware = Build.HARDWARE ?: ""
        val device = Build.DEVICE ?: ""
        if (fingerprint.contains("robolectric", ignoreCase = true) ||
            hardware.contains("robolectric", ignoreCase = true) ||
            device.contains("robolectric", ignoreCase = true)
        ) {
            return false
        }
        return true
    }

    fun isPlaintextSqliteDatabase(dbFile: File): Boolean {
        if (!dbFile.exists() || dbFile.length() < 16) return false
        return try {
            val buffer = ByteArray(16)
            FileInputStream(dbFile).use { it.read(buffer) }
            buffer.take(15) == SQLITE_HEADER_PREFIX.take(15)
        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    fun getOrCreateDatabaseKey(context: Context): ByteArray {
        cachedKey?.let { return it }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedB64 = prefs.getString(PREF_ENCRYPTED_DB_KEY, null)
        val ivB64 = prefs.getString(PREF_DB_KEY_IV, null)

        if (encryptedB64 != null && ivB64 != null) {
            try {
                val secretKey = getOrCreateMasterKey()
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val iv = Base64.decode(ivB64, Base64.NO_WRAP)
                val encryptedBytes = Base64.decode(encryptedB64, Base64.NO_WRAP)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
                val rawKey = cipher.doFinal(encryptedBytes)
                cachedKey = rawKey
                return rawKey
            } catch (_: Exception) {
                // In case of Keystore clearing, proceed to re-generate or fallback
            }
        }

        // Generate new 256-bit passphrase
        val rawKey = ByteArray(DB_KEY_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
        try {
            val secretKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(rawKey)

            prefs.edit()
                .putString(PREF_ENCRYPTED_DB_KEY, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                .putString(PREF_DB_KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
        } catch (_: Exception) {
            // Fallback for restricted environments
        }

        cachedKey = rawKey
        return rawKey
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val entry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}

