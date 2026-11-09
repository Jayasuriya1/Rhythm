package com.example.util

import com.example.data.entity.BrainDumpNoteEntity
import com.example.data.entity.HealthCheckinEntity
import com.example.data.entity.RoutineEntryEntity
import com.example.domain.insights.FocusMetricSummary
import com.example.domain.insights.MedicationAdherenceSummary
import com.example.domain.insights.MetricTrend
import com.example.domain.insights.SleepMetricSummary
import com.example.domain.insights.SrmMetricSummary
import com.example.domain.insights.WellnessMetricSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExportManager {

    /**
     * Generates a structured personal and clinician summary report in Plain Text / Markdown.
     * When [includePrivateNotes] is false (default), all subjective symptoms and thought contents are redacted.
     */
    fun generateSummaryText(
        startDate: String,
        endDate: String,
        sleepMetrics: SleepMetricSummary,
        srmMetrics: SrmMetricSummary,
        medicationMetrics: MedicationAdherenceSummary,
        wellnessMetrics: WellnessMetricSummary,
        focusMetrics: FocusMetricSummary,
        includePrivateNotes: Boolean = false,
        healthCheckins: List<HealthCheckinEntity> = emptyList(),
        routineEntries: List<RoutineEntryEntity> = emptyList(),
        brainDumpNotes: List<BrainDumpNoteEntity> = emptyList()
    ): String {
        val sb = StringBuilder()
        val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        sb.appendLine("==================================================")
        sb.appendLine("RHYTHM — PERSONAL RHYTHM & HEALTH SUMMARY")
        sb.appendLine("==================================================")
        sb.appendLine("Period: $startDate to $endDate")
        sb.appendLine("Generated: $generatedAt (Local Time)")
        sb.appendLine("Privacy Mode: ${if (includePrivateNotes) "Full (Private Notes Included)" else "Redacted (Metrics Only, No Private Notes)"}")
        sb.appendLine("Notice: This is a personal observational record, not a medical diagnosis.")
        sb.appendLine()

        // 1. Sleep & Circadian Timing
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("1. SLEEP & CIRCADIAN TIMING")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("Days with recorded sleep: ${sleepMetrics.recordedDaysCount} of ${sleepMetrics.totalDaysInPeriod}")
        if (sleepMetrics.averageDurationMinutes != null) {
            val avgH = sleepMetrics.averageDurationMinutes / 60
            val avgM = sleepMetrics.averageDurationMinutes % 60
            sb.appendLine("Average duration: ${avgH}h ${avgM}m")
        } else {
            sb.appendLine("Average duration: Unrecorded")
        }
        if (sleepMetrics.bedtimeConsistencyMinutes != null) {
            sb.appendLine("Bedtime timing variation: ±${sleepMetrics.bedtimeConsistencyMinutes.toInt()} minutes (${sleepMetrics.bedtimeConsistencyTier.name.lowercase().replaceFirstChar { it.uppercase() }})")
        } else {
            sb.appendLine("Bedtime timing variation: Insufficient data (< 2 entries)")
        }
        if (sleepMetrics.wakeTimeConsistencyMinutes != null) {
            sb.appendLine("Wake time timing variation: ±${sleepMetrics.wakeTimeConsistencyMinutes.toInt()} minutes")
        }
        sb.appendLine()

        // 2. Social Rhythm Metric (SRM-5)
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("2. SOCIAL RHYTHM METRIC (SRM-5)")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("Target window: Within ±45 minutes of scheduled anchor target")
        sb.appendLine("Total regular anchor events: ${srmMetrics.totalRegularHits} of ${srmMetrics.totalRecordedEvents} recorded events")
        if (srmMetrics.regularHitsRatio != null) {
            val pct = (srmMetrics.regularHitsRatio * 100).toInt()
            sb.appendLine("Routine regularity ratio: $pct% of recorded events")
        } else {
            sb.appendLine("Routine regularity ratio: Unrecorded")
        }
        sb.appendLine("Total scheduled target events: ${srmMetrics.totalExpectedEvents}")
        sb.appendLine()

        // 3. Medication Adherence
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("3. MEDICATION SCHEDULE ADHERENCE")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("Total scheduled doses: ${medicationMetrics.totalScheduledDoses}")
        sb.appendLine("Doses taken on time (within 60m): ${medicationMetrics.dosesTakenOnTime}")
        sb.appendLine("Doses taken delayed: ${medicationMetrics.dosesTakenDelayed}")
        sb.appendLine("Doses marked skipped: ${medicationMetrics.dosesSkipped}")
        sb.appendLine("Doses unrecorded: ${medicationMetrics.dosesUnrecorded}")
        if (medicationMetrics.adherenceRate != null) {
            val pct = (medicationMetrics.adherenceRate * 100).toInt()
            sb.appendLine("Recorded intake rate: $pct%")
        }
        sb.appendLine()

        // 4. Wellness Check-Ins (Mood & Energy)
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("4. MOOD & ENERGY OBSERVATIONS (1–5 scale)")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("Check-in days: ${wellnessMetrics.recordedDaysCount} of ${sleepMetrics.totalDaysInPeriod}")
        sb.appendLine("Average Mood: ${wellnessMetrics.averageMood?.let { String.format(Locale.US, "%.1f / 5", it) } ?: "--"}")
        sb.appendLine("Average Energy: ${wellnessMetrics.averageEnergy?.let { String.format(Locale.US, "%.1f / 5", it) } ?: "--"}")
        sb.appendLine("Average Stress: ${wellnessMetrics.averageStress?.let { String.format(Locale.US, "%.1f / 5", it) } ?: "--"}")
        sb.appendLine("Average Irritability: ${wellnessMetrics.averageIrritability?.let { String.format(Locale.US, "%.1f / 5", it) } ?: "--"}")
        sb.appendLine("Average Activation Energy: ${wellnessMetrics.averageActivation?.let { String.format(Locale.US, "%.1f / 5", it) } ?: "--"}")
        sb.appendLine("Mood Trend Trajectory: ${formatTrend(wellnessMetrics.moodTrend)}")
        sb.appendLine("Energy Trend Trajectory: ${formatTrend(wellnessMetrics.energyTrend)}")
        sb.appendLine()

        // 5. Focus & Deep Work
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("5. FOCUS & DEEP WORK SESSIONS")
        sb.appendLine("--------------------------------------------------")
        val focusH = focusMetrics.totalDurationMinutes / 60
        val focusM = focusMetrics.totalDurationMinutes % 60
        sb.appendLine("Total focus time: ${focusH}h ${focusM}m across ${focusMetrics.totalSessionsCount} sessions")
        sb.appendLine("Completed sessions: ${focusMetrics.completedSessionsCount}")
        sb.appendLine("Active study days: ${focusMetrics.activeDaysCount}")
        if (focusMetrics.categoryShares.isNotEmpty()) {
            sb.appendLine("Category Breakdown:")
            focusMetrics.categoryShares.forEach { share ->
                val h = share.durationMinutes / 60
                val m = share.durationMinutes % 60
                val pct = String.format(Locale.US, "%.1f%%", share.percentage)
                sb.appendLine("  • ${share.category}: ${h}h ${m}m ($pct)")
            }
        }
        sb.appendLine()

        // 6. Private Notes (Controlled by includePrivateNotes toggle)
        if (includePrivateNotes) {
            sb.appendLine("--------------------------------------------------")
            sb.appendLine("6. PRIVATE NOTES & OBSERVATIONS (CONFIDENTIAL)")
            sb.appendLine("--------------------------------------------------")

            val notesWithText = healthCheckins.filter { !it.symptomsNotes.isNullOrBlank() }
            if (notesWithText.isNotEmpty()) {
                sb.appendLine("Health & Symptom Notes:")
                notesWithText.forEach { c ->
                    sb.appendLine("  [${c.date}]: ${c.symptomsNotes}")
                }
                sb.appendLine()
            }

            val disruptions = routineEntries.filter { !it.disruptionNote.isNullOrBlank() }
            if (disruptions.isNotEmpty()) {
                sb.appendLine("Routine Disruption Notes:")
                disruptions.forEach { r ->
                    sb.appendLine("  [${r.date} - ${r.anchorId}]: ${r.disruptionNote}")
                }
                sb.appendLine()
            }

            val recentBrainDumps = brainDumpNotes.take(10)
            if (recentBrainDumps.isNotEmpty()) {
                sb.appendLine("Recent Brain Dump Reflections:")
                recentBrainDumps.forEach { b ->
                    sb.appendLine("  • ${b.displayTitle()}: ${b.content.lines().firstOrNull() ?: ""}")
                }
                sb.appendLine()
            }
        } else {
            sb.appendLine("--------------------------------------------------")
            sb.appendLine("6. PRIVATE NOTES (REDACTED BY USER PREFERENCE)")
            sb.appendLine("--------------------------------------------------")
            sb.appendLine("Subjective freeform notes and thoughts were excluded from this export.")
            sb.appendLine()
        }

        sb.appendLine("==================================================")
        sb.appendLine("END OF RHYTHM SUMMARY")
        sb.appendLine("==================================================")

        return sb.toString()
    }

    private fun formatTrend(trend: MetricTrend): String = when (trend) {
        MetricTrend.TRENDING_HIGHER -> "Trending higher over the period"
        MetricTrend.TRENDING_LOWER -> "Trending lower over the period"
        MetricTrend.STEADY -> "Steady"
        MetricTrend.INSUFFICIENT_DATA -> "Insufficient data (< 3 entries)"
    }
}
