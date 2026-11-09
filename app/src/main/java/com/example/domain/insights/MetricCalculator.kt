package com.example.domain.insights

import com.example.data.entity.HealthCheckinEntity
import com.example.data.entity.MedicationEntity
import com.example.data.entity.MedicationIntakeEntity
import com.example.data.entity.RoutineAnchorEntity
import com.example.data.entity.RoutineEntryEntity
import com.example.data.entity.SleepEntryEntity
import com.example.data.entity.StudySessionEntity
import com.example.util.DateTimeUtils
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class ConsistencyTier {
    CONSISTENT,
    MODERATE,
    VARIABLE,
    INSUFFICIENT_DATA
}

enum class MetricTrend {
    TRENDING_HIGHER,
    TRENDING_LOWER,
    STEADY,
    INSUFFICIENT_DATA
}

enum class DoseStatus {
    TAKEN_ON_TIME,
    TAKEN_DELAYED,
    SKIPPED,
    UNRECORDED
}

data class SleepMetricSummary(
    val recordedDaysCount: Int,
    val totalDaysInPeriod: Int,
    val averageDurationMinutes: Int?,
    val bedtimeConsistencyMinutes: Double?,
    val wakeTimeConsistencyMinutes: Double?,
    val bedtimeConsistencyTier: ConsistencyTier,
    val wakeTimeConsistencyTier: ConsistencyTier,
    val dailyDurations: Map<String, Int?>
)

data class AnchorHitResult(
    val anchorId: String,
    val anchorName: String,
    val targetTimeMinutes: Int,
    val actualTimeMinutes: Int?,
    val deltaMinutes: Int?,
    val isRegular: Boolean,
    val isSkipped: Boolean,
    val isUnrecorded: Boolean
)

data class SrmDaySummary(
    val date: String,
    val anchorResults: List<AnchorHitResult>,
    val regularHitsCount: Int,
    val totalEnabledAnchorsCount: Int
)

data class SrmMetricSummary(
    val totalExpectedEvents: Int,
    val totalRecordedEvents: Int,
    val totalRegularHits: Int,
    val regularHitsRatio: Double?,
    val weeklyRegularityScore: Double?,
    val dailySummaries: List<SrmDaySummary>
)

data class ScheduledDoseInstance(
    val date: String,
    val medicationId: Long,
    val medicationName: String,
    val scheduledTimeMinutes: Int,
    val intakeStatus: DoseStatus,
    val actualActionTimeMillis: Long?
)

data class MedicationAdherenceSummary(
    val totalScheduledDoses: Int,
    val dosesTakenOnTime: Int,
    val dosesTakenDelayed: Int,
    val dosesSkipped: Int,
    val dosesUnrecorded: Int,
    val adherenceRate: Double?,
    val onTimeRate: Double?
)

data class CategoryFocusShare(
    val category: String,
    val durationMinutes: Int,
    val percentage: Double
)

data class FocusMetricSummary(
    val totalDurationMinutes: Int,
    val totalSessionsCount: Int,
    val completedSessionsCount: Int,
    val activeDaysCount: Int,
    val averageMinutesPerActiveDay: Int,
    val averageMinutesPerCalendarDay: Int,
    val categoryShares: List<CategoryFocusShare>
)

data class WellnessMetricSummary(
    val recordedDaysCount: Int,
    val averageMood: Double?,
    val averageEnergy: Double?,
    val averageStress: Double?,
    val averageIrritability: Double?,
    val averageActivation: Double?,
    val moodTrend: MetricTrend,
    val energyTrend: MetricTrend
)

object MetricCalculator {

    const val SRM_HIT_WINDOW_MINUTES = 45
    const val MEDICATION_ON_TIME_WINDOW_MINUTES = 60

    /**
     * Calculates sleep duration handling midnight crossover.
     * Bedtime 23:15 (1395) and Wake 07:30 (450) -> 495 minutes.
     */
    fun calculateSleepDuration(bedtimeMinutes: Int, wakeTimeMinutes: Int): Int {
        val b = ((bedtimeMinutes % 1440) + 1440) % 1440
        val w = ((wakeTimeMinutes % 1440) + 1440) % 1440
        return if (w >= b) {
            w - b
        } else {
            (1440 - b) + w
        }
    }

    /**
     * Calculates sleep duration, bedtime/waketime circular standard deviation and consistency.
     */
    fun calculateSleepMetrics(
        periodDates: List<String>,
        sleepEntries: List<SleepEntryEntity>
    ): SleepMetricSummary {
        val sleepMap = sleepEntries.associateBy { it.date }
        val dailyDurations = periodDates.associateWith { sleepMap[it]?.durationMinutes }
        val validEntries = periodDates.mapNotNull { sleepMap[it] }

        if (validEntries.isEmpty()) {
            return SleepMetricSummary(
                recordedDaysCount = 0,
                totalDaysInPeriod = periodDates.size,
                averageDurationMinutes = null,
                bedtimeConsistencyMinutes = null,
                wakeTimeConsistencyMinutes = null,
                bedtimeConsistencyTier = ConsistencyTier.INSUFFICIENT_DATA,
                wakeTimeConsistencyTier = ConsistencyTier.INSUFFICIENT_DATA,
                dailyDurations = dailyDurations
            )
        }

        val avgDuration = validEntries.map { it.durationMinutes }.average().roundToInt()

        val bedtimeStdDev = calculateCircularStandardDeviation(validEntries.map { it.bedtimeMinutes })
        val wakeTimeStdDev = calculateStandardDeviation(validEntries.map { it.wakeTimeMinutes.toDouble() })

        return SleepMetricSummary(
            recordedDaysCount = validEntries.size,
            totalDaysInPeriod = periodDates.size,
            averageDurationMinutes = avgDuration,
            bedtimeConsistencyMinutes = bedtimeStdDev,
            wakeTimeConsistencyMinutes = wakeTimeStdDev,
            bedtimeConsistencyTier = categorizeConsistency(bedtimeStdDev),
            wakeTimeConsistencyTier = categorizeConsistency(wakeTimeStdDev),
            dailyDurations = dailyDurations
        )
    }

    /**
     * Normalizes bedtime values around midnight:
     * Values in 12:00 PM..11:59 PM (720..1439) become negative (-720..-1).
     * Values in 12:00 AM..11:59 AM (0..719) stay positive (0..719).
     * Then calculates sample standard deviation.
     */
    fun calculateCircularStandardDeviation(minutesList: List<Int>): Double? {
        if (minutesList.size < 2) return null
        val normalized = minutesList.map { m ->
            val norm = ((m % 1440) + 1440) % 1440
            if (norm >= 720) (norm - 1440).toDouble() else norm.toDouble()
        }
        return calculateStandardDeviation(normalized)
    }

    fun calculateStandardDeviation(values: List<Double>): Double? {
        if (values.size < 2) return null
        val mean = values.average()
        val sumSquaredDiffs = values.sumOf { (it - mean) * (it - mean) }
        return sqrt(sumSquaredDiffs / (values.size - 1))
    }

    private fun categorizeConsistency(stdDev: Double?): ConsistencyTier {
        if (stdDev == null) return ConsistencyTier.INSUFFICIENT_DATA
        return when {
            stdDev <= 30.0 -> ConsistencyTier.CONSISTENT
            stdDev <= 60.0 -> ConsistencyTier.MODERATE
            else -> ConsistencyTier.VARIABLE
        }
    }

    /**
     * Calculates Social Rhythm Metric (SRM-5) regularity across canonical circadian anchors.
     */
    fun calculateSrmMetrics(
        periodDates: List<String>,
        anchors: List<RoutineAnchorEntity>,
        entries: List<RoutineEntryEntity>
    ): SrmMetricSummary {
        val enabledAnchors = anchors.filter { it.isEnabled }.sortedBy { it.sortOrder }
        if (enabledAnchors.isEmpty() || periodDates.isEmpty()) {
            return SrmMetricSummary(
                totalExpectedEvents = 0,
                totalRecordedEvents = 0,
                totalRegularHits = 0,
                regularHitsRatio = null,
                weeklyRegularityScore = null,
                dailySummaries = emptyList()
            )
        }

        val entryMap = entries.groupBy { it.date to it.anchorId }
            .mapValues { it.value.first() }

        var totalRecorded = 0
        var totalRegular = 0
        val dailySummaries = mutableListOf<SrmDaySummary>()

        for (date in periodDates) {
            val dayResults = mutableListOf<AnchorHitResult>()
            var dayRegularCount = 0

            for (anchor in enabledAnchors) {
                val entry = entryMap[date to anchor.id]
                val targetMinutes = entry?.targetTimeMinutes ?: anchor.defaultTargetMinutes

                if (entry == null) {
                    dayResults.add(
                        AnchorHitResult(
                            anchorId = anchor.id,
                            anchorName = anchor.name,
                            targetTimeMinutes = targetMinutes,
                            actualTimeMinutes = null,
                            deltaMinutes = null,
                            isRegular = false,
                            isSkipped = false,
                            isUnrecorded = true
                        )
                    )
                } else if (entry.status.equals("SKIPPED", ignoreCase = true)) {
                    totalRecorded++
                    dayResults.add(
                        AnchorHitResult(
                            anchorId = anchor.id,
                            anchorName = anchor.name,
                            targetTimeMinutes = targetMinutes,
                            actualTimeMinutes = null,
                            deltaMinutes = null,
                            isRegular = false,
                            isSkipped = true,
                            isUnrecorded = false
                        )
                    )
                } else if (entry.actualTimeMinutes == null) {
                    dayResults.add(
                        AnchorHitResult(
                            anchorId = anchor.id,
                            anchorName = anchor.name,
                            targetTimeMinutes = targetMinutes,
                            actualTimeMinutes = null,
                            deltaMinutes = null,
                            isRegular = false,
                            isSkipped = false,
                            isUnrecorded = true
                        )
                    )
                } else {
                    totalRecorded++
                    val actual = entry.actualTimeMinutes
                    val delta = actual - targetMinutes
                    val isRegular = abs(delta) <= SRM_HIT_WINDOW_MINUTES
                    if (isRegular) {
                        totalRegular++
                        dayRegularCount++
                    }
                    dayResults.add(
                        AnchorHitResult(
                            anchorId = anchor.id,
                            anchorName = anchor.name,
                            targetTimeMinutes = targetMinutes,
                            actualTimeMinutes = actual,
                            deltaMinutes = delta,
                            isRegular = isRegular,
                            isSkipped = false,
                            isUnrecorded = false
                        )
                    )
                }
            }

            dailySummaries.add(
                SrmDaySummary(
                    date = date,
                    anchorResults = dayResults,
                    regularHitsCount = dayRegularCount,
                    totalEnabledAnchorsCount = enabledAnchors.size
                )
            )
        }

        val totalExpected = periodDates.size * enabledAnchors.size
        val regularHitsRatio = if (totalRecorded > 0) totalRegular.toDouble() / totalRecorded else null
        val weeklyRegularityScore = if (totalExpected > 0) totalRegular.toDouble() / totalExpected else null

        return SrmMetricSummary(
            totalExpectedEvents = totalExpected,
            totalRecordedEvents = totalRecorded,
            totalRegularHits = totalRegular,
            regularHitsRatio = regularHitsRatio,
            weeklyRegularityScore = weeklyRegularityScore,
            dailySummaries = dailySummaries
        )
    }

    /**
     * Calculates honest medication adherence and on-time adherence.
     */
    fun calculateMedicationAdherence(
        periodDates: List<String>,
        medications: List<MedicationEntity>,
        intakes: List<MedicationIntakeEntity>
    ): MedicationAdherenceSummary {
        val activeMeds = medications.filter { it.isActive && !it.isArchived }
        if (activeMeds.isEmpty() || periodDates.isEmpty()) {
            return MedicationAdherenceSummary(
                totalScheduledDoses = 0,
                dosesTakenOnTime = 0,
                dosesTakenDelayed = 0,
                dosesSkipped = 0,
                dosesUnrecorded = 0,
                adherenceRate = null,
                onTimeRate = null
            )
        }

        val intakeMap = intakes.associateBy { Triple(it.date, it.medicationId, it.scheduledTimeMinutes) }

        var totalScheduled = 0
        var takenOnTime = 0
        var takenDelayed = 0
        var skipped = 0
        var unrecorded = 0

        for (date in periodDates) {
            val dayAbbr = DateTimeUtils.getDayOfWeekAbbreviation(date)

            for (med in activeMeds) {
                if (!med.isEffectiveOnDate(date)) continue
                if (!med.isScheduledForDayOfWeek(dayAbbr)) continue

                val scheduledTimes = med.getScheduledTimes()
                for (time in scheduledTimes) {
                    totalScheduled++
                    val intake = intakeMap[Triple(date, med.id, time)]

                    if (intake == null) {
                        unrecorded++
                    } else if (intake.status.equals("SKIPPED", ignoreCase = true)) {
                        skipped++
                    } else if (intake.status.equals("TAKEN", ignoreCase = true)) {
                        val isDelayed = if (intake.actualActionTime != null) {
                            val actionMinutes = extractMinutesOfDay(intake.actualActionTime)
                            abs(actionMinutes - time) > MEDICATION_ON_TIME_WINDOW_MINUTES
                        } else {
                            false
                        }

                        if (isDelayed) {
                            takenDelayed++
                        } else {
                            takenOnTime++
                        }
                    } else {
                        unrecorded++
                    }
                }
            }
        }

        val takenTotal = takenOnTime + takenDelayed
        val adherenceRate = if (totalScheduled > 0) takenTotal.toDouble() / totalScheduled else null
        val onTimeRate = if (totalScheduled > 0) takenOnTime.toDouble() / totalScheduled else null

        return MedicationAdherenceSummary(
            totalScheduledDoses = totalScheduled,
            dosesTakenOnTime = takenOnTime,
            dosesTakenDelayed = takenDelayed,
            dosesSkipped = skipped,
            dosesUnrecorded = unrecorded,
            adherenceRate = adherenceRate,
            onTimeRate = onTimeRate
        )
    }

    private fun extractMinutesOfDay(epochMillis: Long): Int {
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        return zdt.hour * 60 + zdt.minute
    }

    /**
     * Calculates focus session metrics across categories.
     */
    fun calculateFocusMetrics(
        periodDates: List<String>,
        sessions: List<StudySessionEntity>
    ): FocusMetricSummary {
        val dateSet = periodDates.toSet()
        val validSessions = sessions.filter { it.date in dateSet }

        val totalMinutes = validSessions.sumOf { it.durationMinutes }
        val totalCount = validSessions.size
        val completedCount = validSessions.count { it.completionStatus.equals("COMPLETED", ignoreCase = true) }
        val activeDaysCount = validSessions.map { it.date }.distinct().size

        val avgActive = if (activeDaysCount > 0) totalMinutes / activeDaysCount else 0
        val avgCalendar = if (periodDates.isNotEmpty()) totalMinutes / periodDates.size else 0

        val categoryGroups = validSessions.groupBy { it.category }
        val categoryShares = categoryGroups.map { (cat, list) ->
            val catMinutes = list.sumOf { it.durationMinutes }
            val pct = if (totalMinutes > 0) (catMinutes.toDouble() / totalMinutes) * 100.0 else 0.0
            CategoryFocusShare(
                category = cat,
                durationMinutes = catMinutes,
                percentage = pct
            )
        }.sortedByDescending { it.durationMinutes }

        return FocusMetricSummary(
            totalDurationMinutes = totalMinutes,
            totalSessionsCount = totalCount,
            completedSessionsCount = completedCount,
            activeDaysCount = activeDaysCount,
            averageMinutesPerActiveDay = avgActive,
            averageMinutesPerCalendarDay = avgCalendar,
            categoryShares = categoryShares
        )
    }

    /**
     * Calculates wellness rolling averages and trajectories for mood and energy.
     */
    fun calculateWellnessMetrics(
        periodDates: List<String>,
        checkins: List<HealthCheckinEntity>
    ): WellnessMetricSummary {
        val checkinMap = checkins.associateBy { it.date }
        val validCheckins = periodDates.mapNotNull { checkinMap[it] }

        if (validCheckins.isEmpty()) {
            return WellnessMetricSummary(
                recordedDaysCount = 0,
                averageMood = null,
                averageEnergy = null,
                averageStress = null,
                averageIrritability = null,
                averageActivation = null,
                moodTrend = MetricTrend.INSUFFICIENT_DATA,
                energyTrend = MetricTrend.INSUFFICIENT_DATA
            )
        }

        val moodValues = validCheckins.mapNotNull { it.mood?.toDouble() }
        val energyValues = validCheckins.mapNotNull { it.energy?.toDouble() }
        val stressValues = validCheckins.mapNotNull { it.stress?.toDouble() }
        val irritabilityValues = validCheckins.mapNotNull { it.irritability?.toDouble() }
        val activationValues = validCheckins.mapNotNull { it.activationEnergy?.toDouble() }

        val avgMood = if (moodValues.isNotEmpty()) moodValues.average() else null
        val avgEnergy = if (energyValues.isNotEmpty()) energyValues.average() else null
        val avgStress = if (stressValues.isNotEmpty()) stressValues.average() else null
        val avgIrritability = if (irritabilityValues.isNotEmpty()) irritabilityValues.average() else null
        val avgActivation = if (activationValues.isNotEmpty()) activationValues.average() else null

        val moodTrend = calculateTrend(validCheckins.mapNotNull { it.mood?.toDouble() })
        val energyTrend = calculateTrend(validCheckins.mapNotNull { it.energy?.toDouble() })

        return WellnessMetricSummary(
            recordedDaysCount = validCheckins.size,
            averageMood = avgMood,
            averageEnergy = avgEnergy,
            averageStress = avgStress,
            averageIrritability = avgIrritability,
            averageActivation = avgActivation,
            moodTrend = moodTrend,
            energyTrend = energyTrend
        )
    }

    /**
     * Calculates linear regression slope for trend trajectory.
     */
    fun calculateTrend(values: List<Double>): MetricTrend {
        if (values.size < 3) return MetricTrend.INSUFFICIENT_DATA
        val n = values.size
        val xMean = (n - 1) / 2.0
        val yMean = values.average()

        var numerator = 0.0
        var denominator = 0.0

        for (i in values.indices) {
            val xDiff = i - xMean
            val yDiff = values[i] - yMean
            numerator += xDiff * yDiff
            denominator += xDiff * xDiff
        }

        if (denominator == 0.0) return MetricTrend.STEADY
        val slope = numerator / denominator

        return when {
            slope > 0.15 -> MetricTrend.TRENDING_HIGHER
            slope < -0.15 -> MetricTrend.TRENDING_LOWER
            else -> MetricTrend.STEADY
        }
    }
}
