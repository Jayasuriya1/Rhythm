package com.example.data

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.HealthConnectDailySummaryEntity
import com.example.data.entity.HealthConnectHeartSampleEntity
import com.example.data.entity.HealthConnectSleepStageEntity
import com.example.data.entity.MedicationEntity
import com.example.data.entity.ReminderEntity
import com.example.data.entity.StudyCategoryEntity
import com.example.data.entity.StudyTaskEntity
import com.example.data.repository.AppRepository
import com.example.util.AlarmScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomDataIntegrityAndTransactionTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `deleteMedication atomically deletes medication, cascading removes reminders and cancels alarms`() = runTest {
        // 1. Create medication
        val med = MedicationEntity(
            id = 42L,
            name = "Amoxicillin",
            dosage = "500",
            unit = "mg",
            instructions = "Take after breakfast and dinner",
            scheduledTimeMinutes = 480,
            scheduledTimesJson = "[480, 1200]",
            reminderEnabled = true,
            isActive = true
        )
        db.medicationDao().insertMedication(med)

        // 2. Create another medication to verify isolation
        val otherMed = MedicationEntity(
            id = 99L,
            name = "Vitamin D",
            dosage = "1000",
            unit = "IU",
            instructions = "Daily",
            scheduledTimeMinutes = 540,
            reminderEnabled = true,
            isActive = true
        )
        db.medicationDao().insertMedication(otherMed)

        // 3. Create reminders linked to medication 42 and other medication 99
        val rem1 = ReminderEntity(
            id = 4201L,
            title = "Amoxicillin (500 mg)",
            category = "MEDICATION",
            timeMinutes = 480,
            referenceId = "med:42_0",
            isEnabled = true
        )
        val rem2 = ReminderEntity(
            id = 4202L,
            title = "Amoxicillin (500 mg)",
            category = "MEDICATION",
            timeMinutes = 1200,
            referenceId = "med:42_1",
            isEnabled = true
        )
        val otherRem = ReminderEntity(
            id = 9901L,
            title = "Vitamin D (1000 IU)",
            category = "MEDICATION",
            timeMinutes = 540,
            referenceId = "med:99_0",
            isEnabled = true
        )
        db.reminderDao().insertReminder(rem1)
        db.reminderDao().insertReminder(rem2)
        db.reminderDao().insertReminder(otherRem)

        // 4. Schedule exact alarms for all 3 reminders
        AlarmScheduler.scheduleAlarm(context, rem1)
        AlarmScheduler.scheduleAlarm(context, rem2)
        AlarmScheduler.scheduleAlarm(context, otherRem)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)
        assertEquals(3, shadowAlarmManager.scheduledAlarms.size)

        // 5. Delete medication 42 with cascading cleanup
        repository.deleteMedication(42L, context)

        // 6. Verify medication 42 is deleted, 99 is preserved
        assertNull(db.medicationDao().getMedicationById(42L))
        assertNotNull(db.medicationDao().getMedicationById(99L))

        // 7. Verify reminders for med 42 are deleted, reminder for 99 is preserved
        val remainingReminders = db.reminderDao().getAllRemindersListSync()
        assertEquals(1, remainingReminders.size)
        assertEquals(9901L, remainingReminders[0].id)
        assertEquals("med:99_0", remainingReminders[0].referenceId)
    }

    @Test
    fun `archiveMedication atomically archives medication and disables associated reminders`() = runTest {
        val med = MedicationEntity(
            id = 77L,
            name = "Ibuprofen",
            dosage = "200",
            unit = "mg",
            instructions = "As needed",
            scheduledTimeMinutes = 600,
            scheduledTimesJson = "[600]",
            reminderEnabled = true,
            isActive = true
        )
        db.medicationDao().insertMedication(med)

        val reminder = ReminderEntity(
            id = 7701L,
            title = "Ibuprofen",
            category = "MEDICATION",
            timeMinutes = 600,
            referenceId = "med:77_0",
            isEnabled = true
        )
        db.reminderDao().insertReminder(reminder)

        repository.archiveMedication(77L, true)

        val archivedMed = db.medicationDao().getMedicationById(77L)
        assertNotNull(archivedMed)
        assertTrue(archivedMed!!.isArchived)
        assertFalse(archivedMed.isActive)

        val updatedReminder = db.reminderDao().getReminderById(7701L)
        assertNotNull(updatedReminder)
        assertFalse("Associated reminders must be disabled when medication is archived", updatedReminder!!.isEnabled)
    }

    @Test
    fun `deleteCategoryAndReassignTasks atomically deletes category and reassigns tasks`() = runTest {
        val category = StudyCategoryEntity(
            id = 10L,
            name = "Advanced Calculus",
            colorHex = "#FF0000",
            iconName = "math",
            sortOrder = 1
        )
        db.studyCategoryDao().insertCategory(category)

        val task1 = StudyTaskEntity(
            id = 101L,
            title = "Problem Set 1",
            category = "Advanced Calculus",
            status = "PENDING",
            isDailyPriority = false,
            estimatedMinutes = 90,
            dateCreated = "2026-11-08",
            sortOrder = 1
        )
        val task2 = StudyTaskEntity(
            id = 102L,
            title = "Problem Set 2",
            category = "Advanced Calculus",
            status = "PENDING",
            isDailyPriority = false,
            estimatedMinutes = 60,
            dateCreated = "2026-11-08",
            sortOrder = 2
        )
        db.studyDao().insertTask(task1)
        db.studyDao().insertTask(task2)

        repository.deleteCategoryAndReassignTasks(10L, "Advanced Calculus", "General")

        assertNull(db.studyCategoryDao().getCategoryById(10L))
        val tasks = db.studyDao().getTasksListSync()
        assertEquals(2, tasks.size)
        assertTrue(tasks.all { it.category == "General" })
    }

    @Test
    fun `setDailyPriority atomically clears previous priority and sets new priority`() = runTest {
        val task1 = StudyTaskEntity(
            id = 1L,
            title = "Old Priority Task",
            category = "Study",
            status = "PENDING",
            isDailyPriority = true,
            estimatedMinutes = 30,
            dateCreated = "2026-11-08",
            sortOrder = 1
        )
        val task2 = StudyTaskEntity(
            id = 2L,
            title = "New Priority Task",
            category = "Study",
            status = "PENDING",
            isDailyPriority = false,
            estimatedMinutes = 45,
            dateCreated = "2026-11-08",
            sortOrder = 2
        )
        db.studyDao().insertTask(task1)
        db.studyDao().insertTask(task2)

        repository.setDailyPriority(2L)

        val updatedTask1 = db.studyDao().getTaskById(1L)
        val updatedTask2 = db.studyDao().getTaskById(2L)
        assertNotNull(updatedTask1)
        assertNotNull(updatedTask2)
        assertFalse(updatedTask1!!.isDailyPriority)
        assertTrue(updatedTask2!!.isDailyPriority)
    }

    @Test
    fun `saveHealthConnectData atomically replaces daily summary, sleep stages, and heart samples`() = runTest {
        val date = "2026-11-08"

        // Initial dataset
        val initialSummary = HealthConnectDailySummaryEntity(
            date = date,
            steps = 5000L,
            distanceMeters = 3500.0,
            activeCaloriesKcal = 200.0,
            totalCaloriesKcal = 1600.0,
            sleepDurationMinutes = 420
        )
        val initialStages = listOf(
            HealthConnectSleepStageEntity(id = 1L, date = date, sessionStartTimeEpoch = 1000L, stageStartTimeEpoch = 1000L, stageEndTimeEpoch = 2000L, stageType = "LIGHT", durationMinutes = 60)
        )
        val initialHeart = listOf(
            HealthConnectHeartSampleEntity(id = 1L, date = date, timestampEpoch = 1500L, bpm = 65.0, sampleType = "RESTING")
        )

        repository.saveHealthConnectData(initialSummary, initialStages, initialHeart)

        // Updated dataset for same date
        val updatedSummary = initialSummary.copy(steps = 10000L)
        val updatedStages = listOf(
            HealthConnectSleepStageEntity(id = 2L, date = date, sessionStartTimeEpoch = 1000L, stageStartTimeEpoch = 1000L, stageEndTimeEpoch = 3000L, stageType = "DEEP", durationMinutes = 120),
            HealthConnectSleepStageEntity(id = 3L, date = date, sessionStartTimeEpoch = 1000L, stageStartTimeEpoch = 3000L, stageEndTimeEpoch = 5000L, stageType = "REM", durationMinutes = 90)
        )
        val updatedHeart = listOf(
            HealthConnectHeartSampleEntity(id = 2L, date = date, timestampEpoch = 2500L, bpm = 72.0, sampleType = "EXERCISE")
        )

        repository.saveHealthConnectData(updatedSummary, updatedStages, updatedHeart)

        val fetchedSummary = db.healthConnectDao().getSummaryForDateSync(date)
        assertNotNull(fetchedSummary)
        assertEquals(10000L, fetchedSummary!!.steps)

        val fetchedStages = db.healthConnectDao().getSleepStagesForDateSync(date)
        assertEquals(2, fetchedStages.size)
        assertEquals("DEEP", fetchedStages[0].stageType)

        val fetchedHeart = db.healthConnectDao().getHeartSamplesForDateSync(date)
        assertEquals(1, fetchedHeart.size)
        assertEquals(72.0, fetchedHeart[0].bpm, 0.01)
    }

    @Test
    fun `withTransaction rolls back all operations upon failure`() = runTest {
        val initialMed = MedicationEntity(
            id = 500L,
            name = "Initial Med",
            dosage = "10",
            unit = "mg",
            instructions = "Daily",
            scheduledTimeMinutes = 400
        )
        db.medicationDao().insertMedication(initialMed)

        try {
            db.withTransaction {
                db.medicationDao().deleteMedication(500L)
                // Simulate an exception mid-transaction
                throw IllegalStateException("Simulated network/DB crash")
            }
        } catch (_: IllegalStateException) {
            // Expected
        }

        // Verify medication 500 still exists because transaction rolled back
        val medAfterRollback = db.medicationDao().getMedicationById(500L)
        assertNotNull("Medication must not be deleted if transaction rolled back", medAfterRollback)
        assertEquals("Initial Med", medAfterRollback!!.name)
    }
}

