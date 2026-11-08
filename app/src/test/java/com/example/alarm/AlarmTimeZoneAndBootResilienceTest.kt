package com.example.alarm

import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.CustomAlarmEntity
import com.example.data.entity.ReminderEntity
import com.example.receiver.BootReceiver
import com.example.util.AlarmScheduler
import com.example.util.CustomAlarmScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmTimeZoneAndBootResilienceTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `BootReceiver is exported and registered for boot, package replaced, timezone and time change`() {
        val packageManager = context.packageManager
        val componentName = ComponentName(context, BootReceiver::class.java)

        val receiverInfo = packageManager.getReceiverInfo(
            componentName,
            PackageManager.GET_META_DATA
        )

        assertNotNull("BootReceiver must be registered in AndroidManifest", receiverInfo)
        assertTrue(
            "BootReceiver must be exported to receive system broadcasts",
            receiverInfo.exported
        )

        // Verify intent resolution for all 4 required system actions
        val actions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED
        )

        for (action in actions) {
            val queryIntent = Intent(action).setPackage(context.packageName)
            val resolved = packageManager.queryBroadcastReceivers(queryIntent, 0)
            assertTrue(
                "BootReceiver must resolve broadcast action $action",
                resolved.any { it.activityInfo.name == BootReceiver::class.java.name }
            )
        }
    }

    @Test
    fun `BootReceiver onReceive safely processes timezone and time set intents`() {
        val receiver = BootReceiver()

        val timeZoneIntent = Intent(Intent.ACTION_TIMEZONE_CHANGED)
        receiver.onReceive(context, timeZoneIntent)

        val timeSetIntent = Intent(Intent.ACTION_TIME_CHANGED)
        receiver.onReceive(context, timeSetIntent)

        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, bootIntent)
    }

    @Test
    fun `rescheduleAllAlarms schedules enabled reminders and custom alarms in AlarmManager`() = runTest {
        // Insert an enabled custom alarm
        val customAlarm = CustomAlarmEntity(
            id = 501L,
            title = "Morning Focus",
            timeMinutes = 420, // 07:00 AM
            repeatDays = "DAILY",
            isEnabled = true
        )
        db.customAlarmDao().insertAlarm(customAlarm)

        // Insert an enabled reminder
        val reminder = ReminderEntity(
            id = 601L,
            title = "Hydration Check",
            timeMinutes = 600, // 10:00 AM
            repeatDays = "DAILY",
            category = "Health",
            isEnabled = true
        )
        db.reminderDao().insertReminder(reminder)

        // Run rescheduling logic
        BootReceiver.rescheduleAllAlarms(context, db)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms

        assertTrue("AlarmManager should have scheduled alarms after rescheduling", scheduledAlarms.isNotEmpty())
    }

    @Test
    fun `calculateNextTriggerMillis adapts target epoch to active time zone`() {
        val testTimeMinutes = 450 // 07:30 AM

        // Set timezone to UTC
        val utcTz = TimeZone.getTimeZone("UTC")
        TimeZone.setDefault(utcTz)
        val triggerUtc = AlarmScheduler.calculateNextTriggerMillis(testTimeMinutes, "DAILY")

        val utcCal = Calendar.getInstance(utcTz).apply {
            timeInMillis = triggerUtc
        }
        assertEquals(7, utcCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, utcCal.get(Calendar.MINUTE))

        // Shift timezone to New York (UTC-5 / UTC-4)
        val nyTz = TimeZone.getTimeZone("America/New_York")
        TimeZone.setDefault(nyTz)
        val triggerNy = AlarmScheduler.calculateNextTriggerMillis(testTimeMinutes, "DAILY")

        val nyCal = Calendar.getInstance(nyTz).apply {
            timeInMillis = triggerNy
        }
        assertEquals(7, nyCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, nyCal.get(Calendar.MINUTE))

        // Shift timezone to Tokyo (UTC+9)
        val tokyoTz = TimeZone.getTimeZone("Asia/Tokyo")
        TimeZone.setDefault(tokyoTz)
        val triggerTokyo = CustomAlarmScheduler.calculateNextTriggerMillis(testTimeMinutes, "DAILY")

        val tokyoCal = Calendar.getInstance(tokyoTz).apply {
            timeInMillis = triggerTokyo
        }
        assertEquals(7, tokyoCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, tokyoCal.get(Calendar.MINUTE))
    }
}
