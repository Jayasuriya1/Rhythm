package com.example.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.MedicationEntity
import com.example.data.repository.AppRepository
import com.example.util.DateTimeUtils
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodayDashboardTest {

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

    @Test
    fun recordMedicationIntake_updatesDoseStatusAndIntakeRecords() = runBlocking {
        val today = DateTimeUtils.todayDateString()
        val med = MedicationEntity(
            id = 1,
            name = "Vitamin D3",
            dosage = "1000",
            unit = "IU",
            instructions = "With breakfast",
            scheduledTimeMinutes = 480, // 08:00 AM
            repeatDays = "DAILY",
            startDate = today,
            endDate = null,
            isActive = true
        )
        repository.saveMedication(med)

        // 1-Tap dose recording from Today screen
        viewModel.recordMedicationIntake(med, 480, "TAKEN", null)

        val intakes = repository.getMedicationIntakesForDate(today).first()
        assertEquals(1, intakes.size)
        val intake = intakes[0]
        assertEquals(med.id, intake.medicationId)
        assertEquals(480, intake.scheduledTimeMinutes)
        assertEquals("TAKEN", intake.status)
        assertEquals(today, intake.date)
    }

    @Test
    fun markRoutineDoneNow_recordsAnchorCompletion() = runBlocking {
        repository.ensureDefaultAnchors()
        val anchors = repository.routineAnchors.first()
        assertTrue(anchors.isNotEmpty())

        val targetAnchor = anchors.first()
        viewModel.markRoutineDoneNow(targetAnchor)

        val today = DateTimeUtils.todayDateString()
        val entries = repository.getRoutineEntriesForDate(today).first()
        val entry = entries.firstOrNull { it.anchorId == targetAnchor.id }
        assertNotNull(entry)
        assertEquals("COMPLETED", entry?.status)
    }

    @Test
    fun addWater_increasesTotalDailyHydration() = runBlocking {
        viewModel.addWater(250)
        viewModel.addWater(500)

        val today = DateTimeUtils.todayDateString()
        val entries = repository.getWaterEntriesForDate(today).first()
        assertEquals(2, entries.size)
        val totalMl = entries.sumOf { it.volumeMl }
        assertEquals(750, totalMl)
    }
}
