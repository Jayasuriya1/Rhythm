package com.example.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.CustomAlarmEntity
import com.example.data.entity.ReminderEntity
import com.example.data.repository.AppRepository
import com.example.ui.theme.AppThemeMode
import com.example.util.MeditationAudioMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccessibilityPolishTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(db)
        viewModel = MainViewModel(repository, context)
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun drainLooper() {
        repeat(10) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            ShadowLooper.idleMainLooper()
        }
    }

    private suspend fun drainUntilComplete(job: Job) {
        var attempts = 0
        while (!job.isCompleted && attempts < 100) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            ShadowLooper.idleMainLooper()
            attempts++
            delay(10)
        }
        drainLooper()
    }

    private suspend fun getSettingValue(key: String): String? {
        repeat(5) {
            val value = db.settingsDao().getAllSettingsSync().firstOrNull { it.key == key }?.value
            if (value != null) return value
            delay(10)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            ShadowLooper.idleMainLooper()
        }
        return db.settingsDao().getAllSettingsSync().firstOrNull { it.key == key }?.value
    }

    @Test
    fun appThemeMode_updatesSettingsPersistence() = runBlocking {
        val job1 = viewModel.setAppThemeMode(AppThemeMode.OLED_DARK)
        drainUntilComplete(job1)
        assertEquals(AppThemeMode.OLED_DARK.name, getSettingValue("app_theme_mode"))

        val job2 = viewModel.setAppThemeMode(AppThemeMode.LIGHT)
        drainUntilComplete(job2)
        assertEquals(AppThemeMode.LIGHT.name, getSettingValue("app_theme_mode"))
    }

    @Test
    fun quietHoursSwitches_updatePreferencesState() = runBlocking {
        val j1 = viewModel.setQuietHoursEnabled(false)
        val j2 = viewModel.setQuietHoursUrgentBypass(false)
        val j3 = viewModel.setQuietHoursTime(1380, 420)
        drainUntilComplete(j1)
        drainUntilComplete(j2)
        drainUntilComplete(j3)

        assertEquals("false", getSettingValue("quiet_hours_enabled"))
        assertEquals("false", getSettingValue("quiet_hours_urgent_bypass"))
        assertEquals("1380", getSettingValue("quiet_hours_start_minutes"))
        assertEquals("420", getSettingValue("quiet_hours_end_minutes"))
    }

    @Test
    fun meditationSettings_updatePacingAndAudioMode() = runBlocking {
        viewModel.setMeditationAudioMode(MeditationAudioMode.BREATHING_CUES)
        val pacingJob = viewModel.setMeditationBreathingPacing(4, 7, 8, 0)
        drainUntilComplete(pacingJob)

        assertEquals(MeditationAudioMode.BREATHING_CUES.name, getSettingValue("meditation_audio_mode"))
        assertEquals("4", getSettingValue("meditation_inhale_sec"))
        assertEquals("7", getSettingValue("meditation_hold_inhale_sec"))
        assertEquals("8", getSettingValue("meditation_exhale_sec"))
        assertEquals("0", getSettingValue("meditation_hold_exhale_sec"))
    }

    @Test
    fun alarmToggle_updatesEntityPersistence() = runBlocking {
        val testAlarm = CustomAlarmEntity(
            title = "Morning Wake Up",
            timeMinutes = 420,
            isEnabled = false
        )
        val alarmId = repository.saveAlarm(testAlarm)
        val inserted = repository.getAlarmByIdSync(alarmId)
        assertNotNull(inserted)

        viewModel.toggleAlarm(context, inserted!!, isEnabled = true)
        drainLooper()

        val updated = repository.getAlarmByIdSync(alarmId)
        assertTrue(updated!!.isEnabled)
    }

    @Test
    fun reminderMasterToggle_andItemToggle_operateSafely() = runBlocking {
        val masterJob = viewModel.setRemindersMasterEnabled(false, context)
        drainUntilComplete(masterJob)
        assertEquals("false", getSettingValue("reminders_master_enabled"))

        val reminder = ReminderEntity(
            title = "Drink Water",
            timeMinutes = 600,
            category = "HYDRATION",
            isEnabled = true
        )
        val reminderId = repository.saveReminder(reminder)
        val inserted = repository.getReminderById(reminderId)
        assertNotNull(inserted)

        viewModel.toggleReminderEnabled(inserted!!, isEnabled = false, context = context)
        drainLooper()

        val updated = repository.getReminderById(reminderId)
        assertFalse(updated!!.isEnabled)
    }
}
