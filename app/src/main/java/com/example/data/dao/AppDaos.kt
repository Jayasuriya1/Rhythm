package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.AppSettingEntity
import com.example.data.entity.HealthCheckinEntity
import com.example.data.entity.MedicationEntity
import com.example.data.entity.MedicationIntakeEntity
import com.example.data.entity.RoutineAnchorEntity
import com.example.data.entity.RoutineEntryEntity
import com.example.data.entity.SleepEntryEntity
import com.example.data.entity.StudySessionEntity
import com.example.data.entity.StudyTaskEntity
import com.example.data.entity.WaterEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routine_anchors ORDER BY sortOrder ASC")
    fun getAllAnchors(): Flow<List<RoutineAnchorEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultAnchors(anchors: List<RoutineAnchorEntity>)

    @Update
    suspend fun updateAnchor(anchor: RoutineAnchorEntity)

    @Query("SELECT * FROM routine_entries WHERE date = :date")
    fun getEntriesForDate(date: String): Flow<List<RoutineEntryEntity>>

    @Query("SELECT * FROM routine_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getEntriesBetweenDates(startDate: String, endDate: String): Flow<List<RoutineEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateEntry(entry: RoutineEntryEntity)

    @Query("DELETE FROM routine_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)
}

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_entries WHERE date = :date LIMIT 1")
    fun getSleepForDate(date: String): Flow<SleepEntryEntity?>

    @Query("SELECT * FROM sleep_entries ORDER BY date DESC LIMIT :limit")
    fun getRecentSleepHistory(limit: Int = 14): Flow<List<SleepEntryEntity>>

    @Query("SELECT * FROM sleep_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getSleepBetweenDates(startDate: String, endDate: String): Flow<List<SleepEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSleep(sleep: SleepEntryEntity)

    @Query("DELETE FROM sleep_entries WHERE date = :date")
    suspend fun deleteSleepForDate(date: String)
}

@Dao
interface HealthDao {
    @Query("SELECT * FROM health_checkins WHERE date = :date LIMIT 1")
    fun getHealthForDate(date: String): Flow<HealthCheckinEntity?>

    @Query("SELECT * FROM health_checkins ORDER BY date DESC LIMIT :limit")
    fun getRecentHealthHistory(limit: Int = 14): Flow<List<HealthCheckinEntity>>

    @Query("SELECT * FROM health_checkins WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getHealthBetweenDates(startDate: String, endDate: String): Flow<List<HealthCheckinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHealth(health: HealthCheckinEntity)
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_entries WHERE date = :date ORDER BY timestamp ASC")
    fun getWaterEntriesForDate(date: String): Flow<List<WaterEntryEntity>>

    @Query("SELECT * FROM water_entries WHERE date >= :startDate AND date <= :endDate ORDER BY timestamp ASC")
    fun getWaterBetweenDates(startDate: String, endDate: String): Flow<List<WaterEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterEntry(entry: WaterEntryEntity)

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteWaterEntry(id: Long)

    @Query("DELETE FROM water_entries WHERE date = :date")
    suspend fun clearWaterForDate(date: String)
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY scheduledTimeMinutes ASC")
    fun getActiveMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications ORDER BY scheduledTimeMinutes ASC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedication(id: Long)

    @Query("SELECT * FROM medication_intakes WHERE date = :date")
    fun getIntakesForDate(date: String): Flow<List<MedicationIntakeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateIntake(intake: MedicationIntakeEntity)
}

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_tasks ORDER BY isDailyPriority DESC, sortOrder ASC, id DESC")
    fun getAllTasks(): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks WHERE isDailyPriority = 1 LIMIT 1")
    fun getDailyPriority(): Flow<StudyTaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTaskEntity): Long

    @Update
    suspend fun updateTask(task: StudyTaskEntity)

    @Query("DELETE FROM study_tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("UPDATE study_tasks SET isDailyPriority = 0")
    suspend fun clearDailyPriorities()

    @Query("UPDATE study_tasks SET isDailyPriority = 1 WHERE id = :id")
    suspend fun setDailyPriority(id: Long)

    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 30): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE date = :date ORDER BY timestamp ASC")
    fun getSessionsForDate(date: String): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getSessionsBetweenDates(startDate: String, endDate: String): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    fun getSetting(key: String): Flow<AppSettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: AppSettingEntity)
}
