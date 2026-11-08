package com.example.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseKeyManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `isSqlCipherSupported returns false on Robolectric desktop test environment`() {
        // Robolectric runs on desktop JVM (Windows/Linux) where Android .so binaries cannot be loaded
        assertFalse(DatabaseKeyManager.isSqlCipherSupported())
    }

    @Test
    fun `isPlaintextSqliteDatabase accurately detects standard SQLite database header`() {
        val tempPlainDb = File.createTempFile("test_plain", ".db")
        try {
            FileOutputStream(tempPlainDb).use { fos ->
                fos.write("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII))
                fos.write(ByteArray(100))
            }
            assertTrue(DatabaseKeyManager.isPlaintextSqliteDatabase(tempPlainDb))
        } finally {
            tempPlainDb.delete()
        }
    }

    @Test
    fun `isPlaintextSqliteDatabase rejects encrypted or non-sqlite binary files`() {
        val tempEncDb = File.createTempFile("test_enc", ".db")
        try {
            FileOutputStream(tempEncDb).use { fos ->
                fos.write(byteArrayOf(0x00, 0x1A, 0x2B, 0x3C, 0x4D, 0x5E, 0x6F, 0x7A, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88.toByte()))
                fos.write(ByteArray(100))
            }
            assertFalse(DatabaseKeyManager.isPlaintextSqliteDatabase(tempEncDb))
        } finally {
            tempEncDb.delete()
        }
    }

    @Test
    fun `isPlaintextSqliteDatabase returns false for non-existent or empty files`() {
        val nonExistent = File(context.cacheDir, "non_existent_test.db")
        assertFalse(DatabaseKeyManager.isPlaintextSqliteDatabase(nonExistent))

        val emptyFile = File.createTempFile("empty", ".db")
        try {
            assertFalse(DatabaseKeyManager.isPlaintextSqliteDatabase(emptyFile))
        } finally {
            emptyFile.delete()
        }
    }

    @Test
    fun `getOrCreateDatabaseKey produces 256-bit key and returns cached instance`() {
        val key1 = DatabaseKeyManager.getOrCreateDatabaseKey(context)
        assertNotNull(key1)
        assertEquals(32, key1.size) // 256 bits

        val key2 = DatabaseKeyManager.getOrCreateDatabaseKey(context)
        assertEquals(key1, key2)
    }
}

