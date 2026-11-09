package com.example.ui.screens.insights

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.HealthCheckinEntity
import com.example.data.entity.MedicationEntity
import com.example.data.entity.MedicationIntakeEntity
import com.example.data.entity.RoutineAnchorEntity
import com.example.data.entity.RoutineEntryEntity
import com.example.data.entity.SleepEntryEntity
import com.example.data.entity.StudySessionEntity
import com.example.data.repository.AppRepository
import com.example.domain.insights.MetricCalculator
import com.example.ui.MainViewModel
import com.example.util.DateTimeUtils
import com.example.util.ReportExportManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class InsightsScreenTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        repository = AppRepository(db)
        viewModel = MainViewModel(repository, context)
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun drainLooper() {
        repeat(5) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            ShadowLooper.idleMainLooper()
        }
    }

    @Test
    fun insightsRangeQueries_returnPeriodBoundedData() = runBlocking {
        val startDate = "2026-11-01"
        val endDate = "2026-11-03"

        // Insert in-range and out-of-range sleep entries
        repository.saveSleepEntry(SleepEntryEntity(date = "2026-10-31", bedtimeMinutes = 1380, wakeTimeMinutes = 420, durationMinutes = 480, qualityRating = 4))
        repository.saveSleepEntry(SleepEntryEntity(date = "2026-11-01", bedtimeMinutes = 1380, wakeTimeMinutes = 420, durationMinutes = 480, qualityRating = 4))
        repository.saveSleepEntry(SleepEntryEntity(date = "2026-11-02", bedtimeMinutes = 1410, wakeTimeMinutes = 450, durationMinutes = 480, qualityRating = 5))
        repository.saveSleepEntry(SleepEntryEntity(date = "2026-11-04", bedtimeMinutes = 1380, wakeTimeMinutes = 420, durationMinutes = 480, qualityRating = 3))

        drainLooper()

        val periodSleep = viewModel.getSleepBetween(startDate, endDate).first()
        assertEquals(2, periodSleep.size)
        assertEquals("2026-11-01", periodSleep[0].date)
        assertEquals("2026-11-02", periodSleep[1].date)
    }

    @Test
    fun insightsIntegration_calculatesAllMetricSummariesFromDb() = runBlocking {
        val period = listOf("2026-11-01", "2026-11-02", "2026-11-03")
        val startDate = "2026-11-01"
        val endDate = "2026-11-03"

        // 1. Sleep: 2 days logged, 1 unrecorded
        repository.saveSleepEntry(SleepEntryEntity(date = "2026-11-01", bedtimeMinutes = 1380, wakeTimeMinutes = 420, durationMinutes = 480, qualityRating = 4))
        repository.saveSleepEntry(SleepEntryEntity(date = "2026-11-02", bedtimeMinutes = 1410, wakeTimeMinutes = 450, durationMinutes = 480, qualityRating = 4))

        // 2. SRM: 2 entries for default anchors
        repository.ensureDefaultAnchors()
        repository.saveRoutineEntry(RoutineEntryEntity(date = "2026-11-01", anchorId = "out_of_bed", targetTimeMinutes = 420, actualTimeMinutes = 430, status = "COMPLETED"))
        repository.saveRoutineEntry(RoutineEntryEntity(date = "2026-11-02", anchorId = "out_of_bed", targetTimeMinutes = 420, actualTimeMinutes = 450, status = "COMPLETED"))

        // 3. Medication: 1 med, 2 doses taken on time
        val med = MedicationEntity(id = 1, name = "Lithium", dosage = "300", unit = "mg", instructions = "", scheduledTimeMinutes = 480, repeatDays = "DAILY", startDate = "2026-11-01")
        repository.saveMedication(med)
        repository.recordMedicationIntake(MedicationIntakeEntity(date = "2026-11-01", medicationId = 1, scheduledTimeMinutes = 480, status = "TAKEN"))
        repository.recordMedicationIntake(MedicationIntakeEntity(date = "2026-11-02", medicationId = 1, scheduledTimeMinutes = 480, status = "TAKEN"))

        // 4. Health Check-In
        repository.saveHealthCheckin(HealthCheckinEntity(date = "2026-11-01", mood = 4, energy = 4))
        repository.saveHealthCheckin(HealthCheckinEntity(date = "2026-11-02", mood = 4, energy = 3))

        // 5. Study Sessions
        repository.recordStudySession(StudySessionEntity(taskTitle = "Trees", category = "DSA", durationMinutes = 60, sessionType = "DEEP_WORK", date = "2026-11-01"))

        drainLooper()

        val sleepList = viewModel.getSleepBetween(startDate, endDate).first()
        val routineEntries = viewModel.getRoutineEntriesBetween(startDate, endDate).first()
        val routineAnchors = repository.routineAnchors.first()
        val medIntakes = viewModel.getMedicationIntakesBetween(startDate, endDate).first()
        val allMeds = repository.allMedications.first()
        val healthList = viewModel.getHealthBetween(startDate, endDate).first()
        val studyList = viewModel.getStudySessionsBetween(startDate, endDate).first()

        val sleepSummary = MetricCalculator.calculateSleepMetrics(period, sleepList)
        val srmSummary = MetricCalculator.calculateSrmMetrics(period, routineAnchors, routineEntries)
        val medSummary = MetricCalculator.calculateMedicationAdherence(period, allMeds, medIntakes)
        val wellnessSummary = MetricCalculator.calculateWellnessMetrics(period, healthList)
        val focusSummary = MetricCalculator.calculateFocusMetrics(period, studyList)

        assertEquals(2, sleepSummary.recordedDaysCount)
        assertEquals(480, sleepSummary.averageDurationMinutes)

        assertEquals(2, srmSummary.totalRegularHits)
        assertTrue(srmSummary.totalRecordedEvents >= 2)

        assertEquals(3, medSummary.totalScheduledDoses)
        assertEquals(2, medSummary.dosesTakenOnTime)
        assertEquals(1, medSummary.dosesUnrecorded) // Day 3 not yet taken

        assertEquals(2, wellnessSummary.recordedDaysCount)
        assertEquals(4.0, wellnessSummary.averageMood!!, 0.01)

        assertEquals(60, focusSummary.totalDurationMinutes)
        assertEquals(1, focusSummary.totalSessionsCount)

        // Generate Export Summary Text
        val exportText = ReportExportManager.generateSummaryText(
            startDate = startDate,
            endDate = endDate,
            sleepMetrics = sleepSummary,
            srmMetrics = srmSummary,
            medicationMetrics = medSummary,
            wellnessMetrics = wellnessSummary,
            focusMetrics = focusSummary,
            includePrivateNotes = false
        )

        assertTrue(exportText.contains("Average duration: 8h 0m"))
        assertTrue(exportText.contains("Average Mood: 4.0 / 5"))
        assertTrue(exportText.contains("Total focus time: 1h 0m"))
        assertTrue(exportText.contains("PRIVATE NOTES (REDACTED BY USER PREFERENCE)"))
    }
}
