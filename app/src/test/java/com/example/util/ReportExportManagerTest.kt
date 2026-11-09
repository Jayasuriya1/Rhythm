package com.example.util

import com.example.data.entity.BrainDumpNoteEntity
import com.example.data.entity.HealthCheckinEntity
import com.example.data.entity.RoutineEntryEntity
import com.example.domain.insights.CategoryFocusShare
import com.example.domain.insights.ConsistencyTier
import com.example.domain.insights.FocusMetricSummary
import com.example.domain.insights.MedicationAdherenceSummary
import com.example.domain.insights.MetricTrend
import com.example.domain.insights.SleepMetricSummary
import com.example.domain.insights.SrmMetricSummary
import com.example.domain.insights.WellnessMetricSummary
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportExportManagerTest {

    @Test
    fun generateSummaryText_redactsPrivateNotesByDefault() {
        val sleep = SleepMetricSummary(
            recordedDaysCount = 6,
            totalDaysInPeriod = 7,
            averageDurationMinutes = 465,
            bedtimeConsistencyMinutes = 24.5,
            wakeTimeConsistencyMinutes = 18.0,
            bedtimeConsistencyTier = ConsistencyTier.CONSISTENT,
            wakeTimeConsistencyTier = ConsistencyTier.CONSISTENT,
            dailyDurations = emptyMap()
        )
        val srm = SrmMetricSummary(
            totalExpectedEvents = 35,
            totalRecordedEvents = 30,
            totalRegularHits = 24,
            regularHitsRatio = 0.80,
            weeklyRegularityScore = 0.68,
            dailySummaries = emptyList()
        )
        val meds = MedicationAdherenceSummary(
            totalScheduledDoses = 14,
            dosesTakenOnTime = 12,
            dosesTakenDelayed = 1,
            dosesSkipped = 0,
            dosesUnrecorded = 1,
            adherenceRate = 13.0 / 14.0,
            onTimeRate = 12.0 / 14.0
        )
        val wellness = WellnessMetricSummary(
            recordedDaysCount = 5,
            averageMood = 3.8,
            averageEnergy = 3.4,
            averageStress = 2.1,
            averageIrritability = 1.6,
            averageActivation = 2.0,
            moodTrend = MetricTrend.STEADY,
            energyTrend = MetricTrend.TRENDING_HIGHER
        )
        val focus = FocusMetricSummary(
            totalDurationMinutes = 360,
            totalSessionsCount = 6,
            completedSessionsCount = 6,
            activeDaysCount = 4,
            averageMinutesPerActiveDay = 90,
            averageMinutesPerCalendarDay = 51,
            categoryShares = listOf(CategoryFocusShare("DSA", 240, 66.7), CategoryFocusShare("Frontend", 120, 33.3))
        )

        val healthCheckin = HealthCheckinEntity(
            date = "2026-11-04",
            mood = 3,
            energy = 3,
            symptomsNotes = "Severe anxiety episode and headache"
        )
        val brainDump = BrainDumpNoteEntity(
            title = "Personal Dilemma",
            content = "Extremely private mental thoughts not meant for public viewing"
        )

        // Generate with includePrivateNotes = false (default)
        val report = ReportExportManager.generateSummaryText(
            startDate = "2026-11-01",
            endDate = "2026-11-07",
            sleepMetrics = sleep,
            srmMetrics = srm,
            medicationMetrics = meds,
            wellnessMetrics = wellness,
            focusMetrics = focus,
            includePrivateNotes = false,
            healthCheckins = listOf(healthCheckin),
            brainDumpNotes = listOf(brainDump)
        )

        // Verifications
        assertTrue(report.contains("RHYTHM — PERSONAL RHYTHM & HEALTH SUMMARY"))
        assertTrue(report.contains("Period: 2026-11-01 to 2026-11-07"))
        assertTrue(report.contains("Average duration: 7h 45m"))
        assertTrue(report.contains("Routine regularity ratio: 80% of recorded events"))
        assertTrue(report.contains("Recorded intake rate: 92%"))
        assertTrue(report.contains("Average Mood: 3.8 / 5"))
        assertTrue(report.contains("DSA: 4h 0m"))
        assertTrue(report.contains("PRIVATE NOTES (REDACTED BY USER PREFERENCE)"))

        // Redaction verification: private text MUST NOT appear in report
        assertFalse(report.contains("Severe anxiety episode"))
        assertFalse(report.contains("Extremely private mental thoughts"))
    }

    @Test
    fun generateSummaryText_includesPrivateNotesWhenOptedIn() {
        val sleep = SleepMetricSummary(
            recordedDaysCount = 1,
            totalDaysInPeriod = 7,
            averageDurationMinutes = 480,
            bedtimeConsistencyMinutes = null,
            wakeTimeConsistencyMinutes = null,
            bedtimeConsistencyTier = ConsistencyTier.INSUFFICIENT_DATA,
            wakeTimeConsistencyTier = ConsistencyTier.INSUFFICIENT_DATA,
            dailyDurations = emptyMap()
        )
        val srm = SrmMetricSummary(0, 0, 0, null, null, emptyList())
        val meds = MedicationAdherenceSummary(0, 0, 0, 0, 0, null, null)
        val wellness = WellnessMetricSummary(1, 4.0, 3.0, null, null, null, MetricTrend.INSUFFICIENT_DATA, MetricTrend.INSUFFICIENT_DATA)
        val focus = FocusMetricSummary(0, 0, 0, 0, 0, 0, emptyList())

        val healthCheckin = HealthCheckinEntity(
            date = "2026-11-05",
            symptomsNotes = "Mild tension after coffee"
        )
        val routineEntry = RoutineEntryEntity(
            date = "2026-11-05",
            anchorId = "main_activity",
            targetTimeMinutes = 600,
            actualTimeMinutes = 680,
            status = "COMPLETED",
            disruptionNote = "Delayed train transit"
        )

        val report = ReportExportManager.generateSummaryText(
            startDate = "2026-11-01",
            endDate = "2026-11-07",
            sleepMetrics = sleep,
            srmMetrics = srm,
            medicationMetrics = meds,
            wellnessMetrics = wellness,
            focusMetrics = focus,
            includePrivateNotes = true,
            healthCheckins = listOf(healthCheckin),
            routineEntries = listOf(routineEntry)
        )

        assertTrue(report.contains("PRIVATE NOTES & OBSERVATIONS (CONFIDENTIAL)"))
        assertTrue(report.contains("Mild tension after coffee"))
        assertTrue(report.contains("Delayed train transit"))
    }

    @Test
    fun generateSummaryText_handlesEmptyMetricsGracefully() {
        val emptySleep = SleepMetricSummary(0, 7, null, null, null, ConsistencyTier.INSUFFICIENT_DATA, ConsistencyTier.INSUFFICIENT_DATA, emptyMap())
        val emptySrm = SrmMetricSummary(0, 0, 0, null, null, emptyList())
        val emptyMeds = MedicationAdherenceSummary(0, 0, 0, 0, 0, null, null)
        val emptyWellness = WellnessMetricSummary(0, null, null, null, null, null, MetricTrend.INSUFFICIENT_DATA, MetricTrend.INSUFFICIENT_DATA)
        val emptyFocus = FocusMetricSummary(0, 0, 0, 0, 0, 0, emptyList())

        val report = ReportExportManager.generateSummaryText(
            startDate = "2026-11-01",
            endDate = "2026-11-07",
            sleepMetrics = emptySleep,
            srmMetrics = emptySrm,
            medicationMetrics = emptyMeds,
            wellnessMetrics = emptyWellness,
            focusMetrics = emptyFocus,
            includePrivateNotes = false
        )

        assertTrue(report.contains("Average duration: Unrecorded"))
        assertTrue(report.contains("Routine regularity ratio: Unrecorded"))
        assertTrue(report.contains("Average Mood: --"))
        assertTrue(report.contains("Total focus time: 0h 0m across 0 sessions"))
    }
}
