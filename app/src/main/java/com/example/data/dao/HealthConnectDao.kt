package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.HealthConnectDailySummaryEntity
import com.example.data.entity.HealthConnectHeartSampleEntity
import com.example.data.entity.HealthConnectSleepStageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthConnectDao {

    @Query("SELECT * FROM health_connect_daily_summaries WHERE date = :date LIMIT 1")
    fun getSummaryForDate(date: String): Flow<HealthConnectDailySummaryEntity?>

    @Query("SELECT * FROM health_connect_daily_summaries WHERE date = :date LIMIT 1")
    suspend fun getSummaryForDateSync(date: String): HealthConnectDailySummaryEntity?

    @Query("SELECT * FROM health_connect_daily_summaries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getSummariesInRange(startDate: String, endDate: String): Flow<List<HealthConnectDailySummaryEntity>>

    @Query("SELECT * FROM health_connect_daily_summaries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getSummariesInRangeSync(startDate: String, endDate: String): List<HealthConnectDailySummaryEntity>

    @Query("SELECT * FROM health_connect_daily_summaries ORDER BY date DESC LIMIT :limit")
    fun getRecentSummaries(limit: Int): Flow<List<HealthConnectDailySummaryEntity>>

    @Query("SELECT * FROM health_connect_daily_summaries ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentSummariesSync(limit: Int): List<HealthConnectDailySummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummary(summary: HealthConnectDailySummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummaries(summaries: List<HealthConnectDailySummaryEntity>)

    // Sleep stages
    @Query("SELECT * FROM health_connect_sleep_stages WHERE date = :date ORDER BY stageStartTimeEpoch ASC")
    fun getSleepStagesForDate(date: String): Flow<List<HealthConnectSleepStageEntity>>

    @Query("SELECT * FROM health_connect_sleep_stages WHERE date = :date ORDER BY stageStartTimeEpoch ASC")
    suspend fun getSleepStagesForDateSync(date: String): List<HealthConnectSleepStageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepStages(stages: List<HealthConnectSleepStageEntity>)

    @Query("DELETE FROM health_connect_sleep_stages WHERE date = :date")
    suspend fun deleteSleepStagesForDate(date: String)

    // Heart samples
    @Query("SELECT * FROM health_connect_heart_samples WHERE date = :date ORDER BY timestampEpoch ASC")
    fun getHeartSamplesForDate(date: String): Flow<List<HealthConnectHeartSampleEntity>>

    @Query("SELECT * FROM health_connect_heart_samples WHERE date = :date ORDER BY timestampEpoch ASC")
    suspend fun getHeartSamplesForDateSync(date: String): List<HealthConnectHeartSampleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartSamples(samples: List<HealthConnectHeartSampleEntity>)

    @Query("DELETE FROM health_connect_heart_samples WHERE date = :date")
    suspend fun deleteHeartSamplesForDate(date: String)

    @Query("SELECT COUNT(*) FROM health_connect_daily_summaries")
    suspend fun getSummariesCount(): Int
}
