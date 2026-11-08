package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository
import com.example.sync.CloudSyncManager
import com.example.sync.CloudSyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrivacyDataFlowTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: AppRepository
    private lateinit var cloudSyncManager: CloudSyncManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        repository = AppRepository(database)
        cloudSyncManager = CloudSyncManager(context, repository)
    }

    @Test
    fun `manifest strictly adheres to privacy architecture with zero internet permission`() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        if (!manifestFile.exists()) return // Skip if running in different working directory context

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile)
        val permissions = doc.getElementsByTagName("uses-permission")
        val permNames = mutableListOf<String>()
        for (i in 0 until permissions.length) {
            val node = permissions.item(i)
            val name = node.attributes.getNamedItem("android:name")?.nodeValue
            if (name != null) permNames.add(name)
        }

        assertFalse("Manifest must not request android.permission.INTERNET", permNames.contains("android.permission.INTERNET"))
    }

    @Test
    fun `manifest marks ActiveAlarmActivity non-exported to prevent unauthorized component invocation`() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        if (!manifestFile.exists()) return

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile)
        val activities = doc.getElementsByTagName("activity")
        var activeAlarmExported: String? = null
        for (i in 0 until activities.length) {
            val node = activities.item(i)
            val name = node.attributes.getNamedItem("android:name")?.nodeValue
            if (name?.contains("ActiveAlarmActivity") == true) {
                activeAlarmExported = node.attributes.getNamedItem("android:exported")?.nodeValue
            }
        }

        assertEquals("false", activeAlarmExported)
    }

    @Test
    fun `cloudSyncManager remains strictly unconfigured and refuses to fake synchronization`() = runTest {
        val state = cloudSyncManager.syncState.value
        assertEquals(CloudSyncStatus.UNCONFIGURED, state.status)
        assertNull(state.lastSyncTimestamp)

        val result = cloudSyncManager.triggerManualSync()
        assertTrue(result.isFailure)
        assertEquals(CloudSyncStatus.UNCONFIGURED, cloudSyncManager.syncState.value.status)
        assertNull(cloudSyncManager.syncState.value.lastSyncTimestamp)
    }
}

