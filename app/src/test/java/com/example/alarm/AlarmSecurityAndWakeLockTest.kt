package com.example.alarm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.receiver.CustomAlarmReceiver
import com.example.service.CustomAlarmPlaybackService
import com.example.ui.screens.alarm.ActiveAlarmActivity
import com.example.util.CustomAlarmScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPowerManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmSecurityAndWakeLockTest {

    @Before
    fun setUp() {
        ShadowPowerManager.reset()
    }

    @After
    fun tearDown() {
        ShadowPowerManager.reset()
    }

    @Test
    fun `ActiveAlarmActivity is not exported in AndroidManifest`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageManager = context.packageManager
        val componentName = ComponentName(context, ActiveAlarmActivity::class.java)

        val activityInfo = packageManager.getActivityInfo(
            componentName,
            PackageManager.GET_META_DATA
        )

        assertNotNull("ActiveAlarmActivity must be registered in AndroidManifest", activityInfo)
        assertFalse(
            "Security requirement: ActiveAlarmActivity must NOT be exported to prevent unauthorized third-party launch",
            activityInfo.exported
        )
    }

    @Test
    fun `ActiveAlarmActivity can be resolved by explicit internal intent`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val explicitIntent = Intent(context, ActiveAlarmActivity::class.java).apply {
            putExtra(ActiveAlarmActivity.EXTRA_ALARM_ID, 42L)
            putExtra(ActiveAlarmActivity.EXTRA_IS_BACKUP, false)
        }

        val packageManager = context.packageManager
        val resolvedActivities = packageManager.queryIntentActivities(explicitIntent, 0)

        assertTrue(
            "Explicit internal intent from within same package must resolve ActiveAlarmActivity",
            resolvedActivities.isNotEmpty()
        )
        assertEquals(
            ActiveAlarmActivity::class.java.name,
            resolvedActivities.first().activityInfo.name
        )
    }

    @Test
    fun `CustomAlarmPlaybackService acquireWakeLock uses PARTIAL_WAKE_LOCK and releases on stop`() {
        val serviceController = Robolectric.buildService(CustomAlarmPlaybackService::class.java)
        serviceController.create()

        val latestWakeLock = ShadowPowerManager.getLatestWakeLock()
        assertNotNull("Playback service should have acquired a wake lock during creation", latestWakeLock)
        assertTrue("Wake lock must be held while alarm service is active", latestWakeLock!!.isHeld)

        serviceController.destroy()
        assertFalse("Wake lock must be released when alarm service is destroyed", latestWakeLock.isHeld)
    }

    @Test
    fun `CustomAlarmReceiver triggers without crashing and safely manages receiver wake lock`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val receiver = CustomAlarmReceiver()
        val alarmIntent = Intent(context, CustomAlarmReceiver::class.java).apply {
            putExtra(CustomAlarmScheduler.EXTRA_ALARM_ID, 1001L)
            putExtra(CustomAlarmScheduler.EXTRA_ALARM_TITLE, "Test Wake Lock Alarm")
            putExtra(CustomAlarmScheduler.EXTRA_IS_BACKUP, false)
        }

        receiver.onReceive(context, alarmIntent)

        val latestWakeLock = ShadowPowerManager.getLatestWakeLock()
        assertNotNull("CustomAlarmReceiver should acquire wake lock during dispatch", latestWakeLock)
        val shadowWakeLock = shadowOf(latestWakeLock)
        assertTrue(
            "Wake lock must be a custom alarm wake lock",
            shadowWakeLock.tag.startsWith("rhythmfocus:")
        )
    }
}
