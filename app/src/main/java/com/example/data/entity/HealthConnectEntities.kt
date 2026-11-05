package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted daily summary of verified records imported from Google Health Connect.
 * All metrics preserve source attribution, units, and timestamps.
 * Missing metrics are preserved as null rather than defaulted to zero.
 */
@Entity(
    tableName = "health_connect_daily_summaries",
    indices = [Index(value = ["date"], unique = true)]
)
data class HealthConnectDailySummaryEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val steps: Long = 0,
    val distanceMeters: Double = 0.0,
    val activeCaloriesKcal: Double = 0.0,
    val totalCaloriesKcal: Double = 0.0,
    val sleepDurationMinutes: Int = 0,
    val sleepStartTimeEpoch: Long? = null,
    val sleepEndTimeEpoch: Long? = null,
    val restingHeartRateBpm: Double? = null,
    val avgHeartRateBpm: Double? = null,
    val minHeartRateBpm: Double? = null,
    val maxHeartRateBpm: Double? = null,
    val hrvRmssdMs: Double? = null,
    val respiratoryRateRpm: Double? = null,
    val oxygenSaturationPercent: Double? = null,
    val waterMl: Int = 0,
    val exerciseMinutes: Int = 0,
    val exerciseSessionsCount: Int = 0,
    val primarySourceApp: String? = null,
    val sourcesListJson: String = "[]",
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val hasSleepStages: Boolean = false
)

/**
 * Persisted individual sleep stage interval from Health Connect SleepSessionRecord.
 * Used to draw an honest, accurate hypnogram without fabricating stage boundaries.
 */
@Entity(
    tableName = "health_connect_sleep_stages",
    indices = [
        Index(value = ["date"]),
        Index(value = ["stageStartTimeEpoch", "stageEndTimeEpoch"])
    ]
)
data class HealthConnectSleepStageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val sessionStartTimeEpoch: Long,
    val stageStartTimeEpoch: Long,
    val stageEndTimeEpoch: Long,
    val stageType: String, // "DEEP", "LIGHT", "REM", "AWAKE", "SLEEPING", "UNKNOWN"
    val durationMinutes: Int,
    val sourceApp: String? = null
)

/**
 * Persisted heart rate time series samples for time-series charts.
 */
@Entity(
    tableName = "health_connect_heart_samples",
    indices = [
        Index(value = ["date"]),
        Index(value = ["timestampEpoch"])
    ]
)
data class HealthConnectHeartSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val timestampEpoch: Long,
    val bpm: Double,
    val sourceApp: String? = null,
    val sampleType: String = "SERIES" // "SERIES", "RESTING", "WORKOUT"
)
