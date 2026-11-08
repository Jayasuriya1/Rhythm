package com.example.ui.viewmodels

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.CustomAlarmEntity
import com.example.data.entity.MedicationEntity
import com.example.data.entity.RegisteredCodeEntity
import com.example.data.entity.ReminderEntity
import com.example.data.entity.RoutineAnchorEntity
import com.example.data.entity.StudyTaskEntity
import com.example.data.repository.AppRepository
import com.example.ui.TimerMode
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
class DomainViewModelsTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun routineViewModel_recordsCompletionAndAnchorTargetUpdates() = runBlocking {
        repository.ensureDefaultAnchors()
        val viewModel = RoutineViewModel(repository)

        val anchors = repository.routineAnchors.first()
        assertTrue("Should have default anchors", anchors.isNotEmpty())

        val wakeAnchor = anchors.first { it.id.contains("bed", ignoreCase = true) || it.name.contains("wake", ignoreCase = true) }
        viewModel.updateRoutineAnchorTarget(wakeAnchor, 420) // 07:00

        val updatedAnchor = repository.routineAnchors.first().first { it.id == wakeAnchor.id }
        assertEquals(420, updatedAnchor.defaultTargetMinutes)

        // Mark routine done
        viewModel.markRoutineDoneNowSync(updatedAnchor, 415)
        val entries = repository.getRoutineEntriesForDate(viewModel.selectedDate.value).first()
        assertEquals(1, entries.size)
        assertEquals("COMPLETED", entries.first().status)
        assertEquals(415, entries.first().actualTimeMinutes)
    }

    @Test
    fun brainDumpViewModel_createsNotesAndConvertsToStudyTask() = runBlocking {
        val viewModel = BrainDumpViewModel(repository)

        val noteId = viewModel.saveBrainDumpNoteSync(
            title = "Algorithmic Complexity",
            content = "Study amortized time bounds and masters theorem.",
            tag = "Study",
            isPinned = true
        )
        assertTrue(noteId > 0)

        val note = repository.getBrainDumpNoteById(noteId)
        assertNotNull(note)
        assertEquals("Algorithmic Complexity", note!!.displayTitle())
        assertTrue(note.isPinned)

        // Convert note to study task
        val taskId = viewModel.convertBrainDumpNoteToStudyTaskSync(
            note = note,
            category = "Algorithms",
            estimatedMinutes = 50,
            isPriority = true
        )
        assertTrue(taskId > 0)

        val task = repository.getStudyTaskById(taskId)
        assertNotNull(task)
        assertEquals("Algorithmic Complexity", task!!.title)
        assertEquals("Algorithms", task.category)
        assertTrue(task.isDailyPriority)
    }

    @Test
    fun healthViewModel_recordsSleepAndMedication() = runBlocking {
        val viewModel = HealthViewModel(repository, context)

        // Sleep
        viewModel.saveSleepRecord(bedtimeMin = 1380, wakeTimeMin = 420, quality = 4, notes = "Restful sleep")
        val sleep = repository.getSleepForDate(viewModel.selectedDate.value).first()
        assertNotNull(sleep)
        assertEquals(480, sleep!!.durationMinutes) // 8 hours
        assertEquals(4, sleep.qualityRating)

        // Medication
        val med = MedicationEntity(
            id = 0L,
            name = "Vitamin D3",
            dosage = "2000",
            unit = "IU",
            instructions = "Take with breakfast",
            scheduledTimeMinutes = 510,
            reminderEnabled = true,
            isActive = true,
            isArchived = false
        )
        val medId = repository.saveMedication(med)
        val savedMed = repository.getMedicationById(medId)
        assertNotNull(savedMed)

        // Record Intake
        viewModel.recordMedicationIntake(
            medication = savedMed!!,
            scheduledTimeMinutes = 510,
            status = "TAKEN",
            notes = "With food"
        )
        val intakes = repository.getMedicationIntakesForDate(viewModel.selectedDate.value).first()
        assertEquals(1, intakes.size)
        assertEquals("TAKEN", intakes.first().status)
    }

    @Test
    fun alarmViewModel_managesRemindersAndCustomAlarms() = runBlocking {
        val viewModel = AlarmViewModel(repository, context)

        // Reminder
        val reminder = ReminderEntity(
            id = 0L,
            title = "Evening Routine",
            timeMinutes = 1260, // 21:00
            repeatDays = "DAILY",
            isEnabled = true,
            category = "Routine"
        )
        val reminderId = repository.saveReminder(reminder)
        assertTrue(reminderId > 0)

        val allReminders = repository.allReminders.first()
        assertEquals(1, allReminders.size)
        assertEquals("Evening Routine", allReminders.first().title)

        // Custom Alarm
        val customAlarm = CustomAlarmEntity(
            id = 0L,
            title = "Morning Wake Up",
            timeMinutes = 6 * 60 + 30, // 06:30
            repeatDays = "WEEKDAYS",
            isEnabled = true,
            vibrateEnabled = true
        )
        val alarmId = repository.saveAlarm(customAlarm)
        assertTrue(alarmId > 0)

        val alarms = repository.allAlarms.first()
        assertEquals(1, alarms.size)
        assertEquals("Morning Wake Up", alarms.first().title)

        // Registered QR Code
        val qrCode = RegisteredCodeEntity(
            id = 0L,
            label = "Desk Lamp QR",
            codeValue = "QR_LAMP_001",
            codeFormat = "QR_CODE"
        )
        val codeId = repository.saveRegisteredCode(qrCode)
        assertTrue(codeId > 0)

        val duplicate = viewModel.checkCodeDuplicate("QR_LAMP_001")
        assertNotNull(duplicate)
        assertEquals(codeId, duplicate!!.id)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun settingsViewModel_updatesThemeAndBreathingSettings() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val viewModel = SettingsViewModel(repository, context)

            viewModel.setAppThemeMode(AppThemeMode.LIGHT)
            testScheduler.advanceUntilIdle()
            ShadowLooper.idleMainLooper()

            val savedTheme = repository.getSetting("app_theme_mode").first()
            assertEquals(AppThemeMode.LIGHT.name, savedTheme?.value)

            viewModel.setBreathingDurations(inhale = 4, pause = 7, exhale = 8)
            testScheduler.advanceUntilIdle()
            ShadowLooper.idleMainLooper()

            val inhale = repository.getSetting("meditation_inhale_sec").first()
            val hold = repository.getSetting("meditation_hold_inhale_sec").first()
            val exhale = repository.getSetting("meditation_exhale_sec").first()

            assertEquals("4", inhale?.value)
            assertEquals("7", hold?.value)
            assertEquals("8", exhale?.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun studyViewModel_handlesCategoryValidationAndTimerConfig() = runBlocking {
        repository.ensureDefaultCategories()
        val viewModel = StudyViewModel(repository, context)

        // Duplicate rejection
        val duplicateCreated = viewModel.createStudyCategorySync("dsa", "Duplicate", "#000000", "code")
        assertFalse("Duplicate category name must be rejected", duplicateCreated)

        // Unique creation
        val uniqueCreated = viewModel.createStudyCategorySync("Quantum Computing", "Algorithms", "#00FF00", "memory")
        assertTrue("Unique category should be created", uniqueCreated)

        // Timer configuration
        viewModel.setTimerConfig(TimerMode.DEEP_WORK, 50)
        val timerState = viewModel.timerState.value
        assertEquals(50 * 60, timerState.targetSeconds)
        assertEquals(50 * 60, timerState.remainingSeconds)
        assertFalse(timerState.isRunning)
    }
}

