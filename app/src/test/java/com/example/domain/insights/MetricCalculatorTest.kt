package com.example.domain.insights

import com.example.data.entity.HealthCheckinEntity
import com.example.data.entity.MedicationEntity
import com.example.data.entity.MedicationIntakeEntity
import com.example.data.entity.RoutineAnchorEntity
import com.example.data.entity.RoutineEntryEntity
import com.example.data.entity.SleepEntryEntity
import com.example.data.entity.StudySessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MetricCalculatorTest {

    // --- 1. Sleep Duration & Calculations ---

    @Test
    fun calculateDuration_spansMidnight() {
        // Bedtime 23:15 (1395m), Wake: 07:30 (450m)
        val duration = MetricCalculator.calculateSleepDuration(1395, 450)
        assertEquals(495, duration) // 8h 15m
    }

    @Test
    fun calculateDuration_sameDayNap() {
        // Bedtime 13:00 (780m), Wake: 14:30 (870m)
        val duration = MetricCalculator.calculateSleepDuration(780, 870)
        assertEquals(90, duration) // 1h 30m
    }

    @Test
    fun averageDuration_ignoresMissingDays() {
        val period = listOf(
            "2026-11-01", "2026-11-02", "2026-11-03", "2026-11-04",
            "2026-11-05", "2026-11-06", "2026-11-07"
        )
        val entries = listOf(
            SleepEntryEntity(date = "2026-11-01", bedtimeMinutes = 1380, wakeTimeMinutes = 420, durationMinutes = 480, qualityRating = 4),
            SleepEntryEntity(date = "2026-11-03", bedtimeMinutes = 1410, wakeTimeMinutes = 390, durationMinutes = 420, qualityRating = 3),
            SleepEntryEntity(date = "2026-11-06", bedtimeMinutes = 1350, wakeTimeMinutes = 450, durationMinutes = 540, qualityRating = 5)
        )

        val summary = MetricCalculator.calculateSleepMetrics(period, entries)
        assertEquals(3, summary.recordedDaysCount)
        assertEquals(7, summary.totalDaysInPeriod)
        assertEquals(480, summary.averageDurationMinutes) // (480 + 420 + 540) / 3 = 480
        assertEquals(480, summary.dailyDurations["2026-11-01"])
        assertNull(summary.dailyDurations["2026-11-02"])
    }

    @Test
    fun averageDuration_allMissing() {
        val period = listOf("2026-11-01", "2026-11-02", "2026-11-03")
        val summary = MetricCalculator.calculateSleepMetrics(period, emptyList())

        assertEquals(0, summary.recordedDaysCount)
        assertNull(summary.averageDurationMinutes)
        assertNull(summary.bedtimeConsistencyMinutes)
        assertEquals(ConsistencyTier.INSUFFICIENT_DATA, summary.bedtimeConsistencyTier)
    }

    @Test
    fun bedtimeConsistency_calculatesCircularStandardDeviation() {
        // Bedtimes spanning midnight:
        // 23:00 (1380 -> -60), 23:30 (1410 -> -30), 23:15 (1395 -> -45), 00:00 (0 -> 0), 23:45 (1425 -> -15)
        // Normalized values: -60, -30, -45, 0, -15 (Mean = -30)
        // Diffs: -30, 0, -15, 30, 15 -> Squared: 900, 0, 225, 900, 225 = 2250 -> / 4 = 562.5 -> sqrt ≈ 23.71
        val bedtimes = listOf(1380, 1410, 1395, 0, 1425)
        val stdDev = MetricCalculator.calculateCircularStandardDeviation(bedtimes)
        assertNotNull(stdDev)
        assertEquals(23.71, stdDev!!, 0.05)
    }

    @Test
    fun bedtimeConsistency_insufficientData() {
        val stdDev = MetricCalculator.calculateCircularStandardDeviation(listOf(1380))
        assertNull(stdDev)
    }

    // --- 2. Social Rhythm Metric (SRM-5) Tests ---

    @Test
    fun anchorHit_within45Minutes_isRegular() {
        val period = listOf("2026-11-04")
        val anchor = RoutineAnchorEntity(
            id = "out_of_bed",
            name = "Getting out of bed",
            description = "Leaving bed",
            defaultTargetMinutes = 420, // 07:00
            sortOrder = 1
        )
        val entry = RoutineEntryEntity(
            date = "2026-11-04",
            anchorId = "out_of_bed",
            targetTimeMinutes = 420,
            actualTimeMinutes = 460, // 07:40 (+40m)
            status = "COMPLETED"
        )

        val summary = MetricCalculator.calculateSrmMetrics(period, listOf(anchor), listOf(entry))
        assertEquals(1, summary.totalRegularHits)
        assertEquals(1, summary.totalRecordedEvents)
        val hit = summary.dailySummaries.first().anchorResults.first()
        assertTrue(hit.isRegular)
        assertEquals(40, hit.deltaMinutes)
        assertFalse(hit.isSkipped)
        assertFalse(hit.isUnrecorded)
    }

    @Test
    fun anchorHit_atExact45Boundary_isRegular() {
        val period = listOf("2026-11-04")
        val anchor = RoutineAnchorEntity(
            id = "dinner",
            name = "Dinner",
            description = "Evening meal",
            defaultTargetMinutes = 1170, // 19:30
            sortOrder = 4
        )
        val entry = RoutineEntryEntity(
            date = "2026-11-04",
            anchorId = "dinner",
            targetTimeMinutes = 1170,
            actualTimeMinutes = 1125, // 18:45 (-45m boundary)
            status = "COMPLETED"
        )

        val summary = MetricCalculator.calculateSrmMetrics(period, listOf(anchor), listOf(entry))
        assertTrue(summary.dailySummaries.first().anchorResults.first().isRegular)
    }

    @Test
    fun anchorHit_beyond45Minutes_isNonRegular() {
        val period = listOf("2026-11-04")
        val anchor = RoutineAnchorEntity(
            id = "main_activity",
            name = "Main activity",
            description = "Start work",
            defaultTargetMinutes = 600, // 10:00
            sortOrder = 3
        )
        val entry = RoutineEntryEntity(
            date = "2026-11-04",
            anchorId = "main_activity",
            targetTimeMinutes = 600,
            actualTimeMinutes = 646, // 10:46 (+46m)
            status = "COMPLETED"
        )

        val summary = MetricCalculator.calculateSrmMetrics(period, listOf(anchor), listOf(entry))
        assertEquals(0, summary.totalRegularHits)
        assertEquals(1, summary.totalRecordedEvents)
        val hit = summary.dailySummaries.first().anchorResults.first()
        assertFalse(hit.isRegular)
        assertEquals(46, hit.deltaMinutes)
    }

    @Test
    fun weeklyRegularity_computesRatio() {
        val period = listOf("2026-11-01", "2026-11-02")
        val anchor1 = RoutineAnchorEntity(id = "a1", name = "A1", description = "", defaultTargetMinutes = 400, sortOrder = 1)
        val anchor2 = RoutineAnchorEntity(id = "a2", name = "A2", description = "", defaultTargetMinutes = 600, sortOrder = 2)

        // Day 1: a1 regular (hit), a2 irregular (+60m)
        // Day 2: a1 regular (hit), a2 unrecorded
        val entries = listOf(
            RoutineEntryEntity(date = "2026-11-01", anchorId = "a1", targetTimeMinutes = 400, actualTimeMinutes = 410, status = "COMPLETED"),
            RoutineEntryEntity(date = "2026-11-01", anchorId = "a2", targetTimeMinutes = 600, actualTimeMinutes = 660, status = "COMPLETED"),
            RoutineEntryEntity(date = "2026-11-02", anchorId = "a1", targetTimeMinutes = 400, actualTimeMinutes = 390, status = "COMPLETED")
        )

        val summary = MetricCalculator.calculateSrmMetrics(period, listOf(anchor1, anchor2), entries)
        assertEquals(4, summary.totalExpectedEvents) // 2 days * 2 anchors
        assertEquals(3, summary.totalRecordedEvents) // 3 logged
        assertEquals(2, summary.totalRegularHits) // 2 hits
        assertEquals(2.0 / 3.0, summary.regularHitsRatio!!, 0.001)
        assertEquals(2.0 / 4.0, summary.weeklyRegularityScore!!, 0.001)
    }

    @Test
    fun skippedAnchor_handledExplicitly() {
        val period = listOf("2026-11-04")
        val anchor = RoutineAnchorEntity(id = "a1", name = "A1", description = "", defaultTargetMinutes = 400, sortOrder = 1)
        val entry = RoutineEntryEntity(
            date = "2026-11-04",
            anchorId = "a1",
            targetTimeMinutes = 400,
            actualTimeMinutes = null,
            status = "SKIPPED"
        )

        val summary = MetricCalculator.calculateSrmMetrics(period, listOf(anchor), listOf(entry))
        assertEquals(1, summary.totalRecordedEvents)
        assertEquals(0, summary.totalRegularHits)
        val hit = summary.dailySummaries.first().anchorResults.first()
        assertTrue(hit.isSkipped)
        assertFalse(hit.isRegular)
        assertFalse(hit.isUnrecorded)
    }

    // --- 3. Medication Adherence Tests ---

    @Test
    fun adherence_onTimeDose() {
        val period = listOf("2026-11-04") // Wednesday
        val med = MedicationEntity(
            id = 10,
            name = "Sertraline",
            dosage = "50",
            unit = "mg",
            instructions = "With breakfast",
            scheduledTimeMinutes = 480, // 08:00 AM
            repeatDays = "DAILY",
            startDate = "2026-11-01"
        )

        // Action taken at 08:25 AM (25m delta -> within 60m on-time window)
        val actionMillis = LocalDate.parse("2026-11-04")
            .atTime(8, 25)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intake = MedicationIntakeEntity(
            date = "2026-11-04",
            medicationId = 10,
            scheduledTimeMinutes = 480,
            actualActionTime = actionMillis,
            status = "TAKEN"
        )

        val summary = MetricCalculator.calculateMedicationAdherence(period, listOf(med), listOf(intake))
        assertEquals(1, summary.totalScheduledDoses)
        assertEquals(1, summary.dosesTakenOnTime)
        assertEquals(0, summary.dosesTakenDelayed)
        assertEquals(1.0, summary.adherenceRate!!, 0.001)
        assertEquals(1.0, summary.onTimeRate!!, 0.001)
    }

    @Test
    fun adherence_delayedDose() {
        val period = listOf("2026-11-04")
        val med = MedicationEntity(
            id = 10,
            name = "Sertraline",
            dosage = "50",
            unit = "mg",
            instructions = "",
            scheduledTimeMinutes = 480, // 08:00 AM
            repeatDays = "DAILY",
            startDate = "2026-11-01"
        )

        // Action taken at 10:15 AM (135m delta -> >60m delayed)
        val actionMillis = LocalDate.parse("2026-11-04")
            .atTime(10, 15)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intake = MedicationIntakeEntity(
            date = "2026-11-04",
            medicationId = 10,
            scheduledTimeMinutes = 480,
            actualActionTime = actionMillis,
            status = "TAKEN"
        )

        val summary = MetricCalculator.calculateMedicationAdherence(period, listOf(med), listOf(intake))
        assertEquals(1, summary.totalScheduledDoses)
        assertEquals(0, summary.dosesTakenOnTime)
        assertEquals(1, summary.dosesTakenDelayed)
        assertEquals(1.0, summary.adherenceRate!!, 0.001) // Taken
        assertEquals(0.0, summary.onTimeRate!!, 0.001) // Not on time
    }

    @Test
    fun adherence_weeklyRate() {
        val period = listOf("2026-11-01", "2026-11-02", "2026-11-03")
        val med = MedicationEntity(
            id = 1,
            name = "Vitamin D",
            dosage = "1000",
            unit = "IU",
            instructions = "",
            scheduledTimeMinutes = 540,
            repeatDays = "DAILY",
            startDate = "2026-11-01"
        )

        // Day 1: taken on time
        // Day 2: skipped
        // Day 3: unrecorded (no entry)
        val intake1 = MedicationIntakeEntity(
            date = "2026-11-01",
            medicationId = 1,
            scheduledTimeMinutes = 540,
            status = "TAKEN"
        )
        val intake2 = MedicationIntakeEntity(
            date = "2026-11-02",
            medicationId = 1,
            scheduledTimeMinutes = 540,
            status = "SKIPPED"
        )

        val summary = MetricCalculator.calculateMedicationAdherence(period, listOf(med), listOf(intake1, intake2))
        assertEquals(3, summary.totalScheduledDoses)
        assertEquals(1, summary.dosesTakenOnTime)
        assertEquals(0, summary.dosesTakenDelayed)
        assertEquals(1, summary.dosesSkipped)
        assertEquals(1, summary.dosesUnrecorded)
        assertEquals(1.0 / 3.0, summary.adherenceRate!!, 0.001)
    }

    @Test
    fun adherence_respectsStartAndEndDate() {
        val period = listOf("2026-11-01", "2026-11-02", "2026-11-03", "2026-11-04")
        val med = MedicationEntity(
            id = 1,
            name = "Antibiotic",
            dosage = "500",
            unit = "mg",
            instructions = "",
            scheduledTimeMinutes = 480,
            repeatDays = "DAILY",
            startDate = "2026-11-03", // Starts on day 3
            endDate = "2026-11-03" // Ends on day 3
        )

        val intake = MedicationIntakeEntity(
            date = "2026-11-03",
            medicationId = 1,
            scheduledTimeMinutes = 480,
            status = "TAKEN"
        )

        val summary = MetricCalculator.calculateMedicationAdherence(period, listOf(med), listOf(intake))
        // Only 2026-11-03 is scheduled
        assertEquals(1, summary.totalScheduledDoses)
        assertEquals(1, summary.dosesTakenOnTime)
        assertEquals(1.0, summary.adherenceRate!!, 0.001)
    }

    // --- 4. Focus & Learning Tests ---

    @Test
    fun studyHours_dateBoundedRangeQuery() {
        val period = listOf("2026-11-01", "2026-11-02")
        val sessions = listOf(
            StudySessionEntity(taskTitle = "Binary Search", category = "DSA", durationMinutes = 60, sessionType = "DEEP_WORK", date = "2026-11-01"),
            StudySessionEntity(taskTitle = "Graph BFS", category = "DSA", durationMinutes = 90, sessionType = "DEEP_WORK", date = "2026-11-01"),
            StudySessionEntity(taskTitle = "Compose Layouts", category = "Frontend", durationMinutes = 45, sessionType = "POMODORO", date = "2026-11-02"),
            StudySessionEntity(taskTitle = "Old Session", category = "Backend", durationMinutes = 120, sessionType = "DEEP_WORK", date = "2026-10-31") // Out of range
        )

        val summary = MetricCalculator.calculateFocusMetrics(period, sessions)
        assertEquals(195, summary.totalDurationMinutes) // 60 + 90 + 45 = 195
        assertEquals(3, summary.totalSessionsCount)
        assertEquals(2, summary.activeDaysCount)
        assertEquals(97, summary.averageMinutesPerActiveDay) // 195 / 2 = 97
        assertEquals(2, summary.categoryShares.size)
        assertEquals("DSA", summary.categoryShares[0].category)
        assertEquals(150, summary.categoryShares[0].durationMinutes)
        assertEquals((150.0 / 195.0) * 100.0, summary.categoryShares[0].percentage, 0.01)
    }

    // --- 5. Wellness & Trajectory Tests ---

    @Test
    fun wellness_averagesAndTrajectory() {
        val period = listOf("2026-11-01", "2026-11-02", "2026-11-03", "2026-11-04")
        val checkins = listOf(
            HealthCheckinEntity(date = "2026-11-01", mood = 2, energy = 2),
            HealthCheckinEntity(date = "2026-11-02", mood = 3, energy = 3),
            HealthCheckinEntity(date = "2026-11-03", mood = 4, energy = 4),
            HealthCheckinEntity(date = "2026-11-04", mood = 5, energy = 4)
        )

        val summary = MetricCalculator.calculateWellnessMetrics(period, checkins)
        assertEquals(4, summary.recordedDaysCount)
        assertEquals(3.5, summary.averageMood!!, 0.01) // (2+3+4+5)/4 = 3.5
        assertEquals(3.25, summary.averageEnergy!!, 0.01) // (2+3+4+4)/4 = 3.25
        assertEquals(MetricTrend.TRENDING_HIGHER, summary.moodTrend)
        assertEquals(MetricTrend.TRENDING_HIGHER, summary.energyTrend)
    }

    @Test
    fun wellness_emptyCheckins() {
        val period = listOf("2026-11-01", "2026-11-02")
        val summary = MetricCalculator.calculateWellnessMetrics(period, emptyList())

        assertEquals(0, summary.recordedDaysCount)
        assertNull(summary.averageMood)
        assertNull(summary.averageEnergy)
        assertEquals(MetricTrend.INSUFFICIENT_DATA, summary.moodTrend)
    }
}
