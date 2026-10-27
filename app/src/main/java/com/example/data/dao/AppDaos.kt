package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ActiveTimerDraftEntity
import com.example.data.entity.AppSettingEntity
import com.example.data.entity.BrainDumpNoteEntity
import com.example.data.entity.HealthCheckinEntity
import com.example.data.entity.MedicationEntity
import com.example.data.entity.MedicationIntakeEntity
import com.example.data.entity.MeditationSessionEntity
import com.example.data.entity.ReminderEntity
import com.example.data.entity.RoutineAnchorEntity
import com.example.data.entity.RoutineEntryEntity
import com.example.data.entity.SleepEntryEntity
import com.example.data.entity.StudyCategoryEntity
import com.example.data.entity.StudySessionEntity
import com.example.data.entity.StudyTaskEntity
import com.example.data.entity.WaterEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routine_anchors ORDER BY sortOrder ASC")
    fun getAllAnchors(): Flow<List<RoutineAnchorEntity>>

    @Query("SELECT * FROM routine_anchors ORDER BY sortOrder ASC")
    suspend fun getAllAnchorsSync(): List<RoutineAnchorEntity>

    @Query("SELECT * FROM routine_entries ORDER BY date ASC")
    suspend fun getAllEntriesSync(): List<RoutineEntryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultAnchors(anchors: List<RoutineAnchorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnchorsList(anchors: List<RoutineAnchorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntriesList(entries: List<RoutineEntryEntity>)

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

    @Query("SELECT COUNT(*) FROM routine_entries")
    suspend fun getEntriesCount(): Int
}

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_entries WHERE date = :date LIMIT 1")
    fun getSleepForDate(date: String): Flow<SleepEntryEntity?>

    @Query("SELECT * FROM sleep_entries ORDER BY date DESC LIMIT :limit")
    fun getRecentSleepHistory(limit: Int = 14): Flow<List<SleepEntryEntity>>

    @Query("SELECT * FROM sleep_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getSleepBetweenDates(startDate: String, endDate: String): Flow<List<SleepEntryEntity>>

    @Query("SELECT * FROM sleep_entries ORDER BY date ASC")
    suspend fun getAllSleepSync(): List<SleepEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepList(entries: List<SleepEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSleep(sleep: SleepEntryEntity)

    @Query("DELETE FROM sleep_entries WHERE date = :date")
    suspend fun deleteSleepForDate(date: String)

    @Query("SELECT COUNT(*) FROM sleep_entries")
    suspend fun getSleepCount(): Int
}

@Dao
interface HealthDao {
    @Query("SELECT * FROM health_checkins WHERE date = :date LIMIT 1")
    fun getHealthForDate(date: String): Flow<HealthCheckinEntity?>

    @Query("SELECT * FROM health_checkins ORDER BY date DESC LIMIT :limit")
    fun getRecentHealthHistory(limit: Int = 14): Flow<List<HealthCheckinEntity>>

    @Query("SELECT * FROM health_checkins WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getHealthBetweenDates(startDate: String, endDate: String): Flow<List<HealthCheckinEntity>>

    @Query("SELECT * FROM health_checkins ORDER BY date ASC")
    suspend fun getAllHealthSync(): List<HealthCheckinEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthList(entries: List<HealthCheckinEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHealth(health: HealthCheckinEntity)

    @Query("SELECT COUNT(*) FROM health_checkins")
    suspend fun getHealthCount(): Int
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_entries WHERE date = :date ORDER BY timestamp ASC")
    fun getWaterEntriesForDate(date: String): Flow<List<WaterEntryEntity>>

    @Query("SELECT * FROM water_entries WHERE date >= :startDate AND date <= :endDate ORDER BY timestamp ASC")
    fun getWaterBetweenDates(startDate: String, endDate: String): Flow<List<WaterEntryEntity>>

    @Query("SELECT * FROM water_entries ORDER BY timestamp ASC")
    suspend fun getAllWaterSync(): List<WaterEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterList(entries: List<WaterEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterEntry(entry: WaterEntryEntity)

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteWaterEntry(id: Long)

    @Query("DELETE FROM water_entries WHERE date = :date")
    suspend fun clearWaterForDate(date: String)

    @Query("SELECT COUNT(*) FROM water_entries")
    suspend fun getWaterCount(): Int
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE isActive = 1 AND isArchived = 0 ORDER BY scheduledTimeMinutes ASC")
    fun getActiveMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications ORDER BY isArchived ASC, scheduledTimeMinutes ASC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE isArchived = 1 ORDER BY scheduledTimeMinutes ASC")
    fun getArchivedMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationById(id: Long): MedicationEntity?

    @Query("SELECT DISTINCT name FROM medications WHERE name != '' ORDER BY name ASC")
    fun getDistinctMedicationNames(): Flow<List<String>>

    @Query("SELECT DISTINCT name FROM medications WHERE name != '' ORDER BY name ASC")
    suspend fun getDistinctMedicationNamesSync(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedication(id: Long)

    @Query("UPDATE medications SET isArchived = :isArchived, isActive = :isActive WHERE id = :id")
    suspend fun setMedicationArchived(id: Long, isArchived: Boolean, isActive: Boolean)

    @Query("SELECT * FROM medication_intakes WHERE date = :date ORDER BY scheduledTimeMinutes ASC")
    fun getIntakesForDate(date: String): Flow<List<MedicationIntakeEntity>>

    @Query("SELECT * FROM medication_intakes ORDER BY date DESC, scheduledTimeMinutes DESC LIMIT :limit")
    fun getRecentIntakeHistory(limit: Int = 100): Flow<List<MedicationIntakeEntity>>

    @Query("SELECT * FROM medication_intakes WHERE medicationId = :medicationId ORDER BY date DESC, scheduledTimeMinutes DESC")
    fun getIntakeHistoryForMedication(medicationId: Long): Flow<List<MedicationIntakeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateIntake(intake: MedicationIntakeEntity)

    @Query("DELETE FROM medication_intakes WHERE id = :id")
    suspend fun deleteIntake(id: Long)

    @Query("SELECT * FROM medications ORDER BY id ASC")
    suspend fun getAllMedicationsSync(): List<MedicationEntity>

    @Query("SELECT * FROM medication_intakes ORDER BY date ASC, scheduledTimeMinutes ASC")
    suspend fun getAllIntakesSync(): List<MedicationIntakeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicationsList(medications: List<MedicationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntakesList(intakes: List<MedicationIntakeEntity>)

    @Query("SELECT COUNT(*) FROM medications")
    suspend fun getMedicationsCount(): Int

    @Query("SELECT COUNT(*) FROM medication_intakes")
    suspend fun getIntakesCount(): Int
}

@Dao
interface StudyCategoryDao {
    @Query("SELECT * FROM study_categories ORDER BY sortOrder ASC, id ASC")
    fun getAllCategories(): Flow<List<StudyCategoryEntity>>

    @Query("SELECT * FROM study_categories ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllCategoriesSync(): List<StudyCategoryEntity>

    @Query("SELECT * FROM study_categories WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    fun getActiveCategories(): Flow<List<StudyCategoryEntity>>

    @Query("SELECT * FROM study_categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findCategoryByName(name: String): StudyCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultCategories(categories: List<StudyCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: StudyCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoriesList(categories: List<StudyCategoryEntity>)

    @Update
    suspend fun updateCategory(category: StudyCategoryEntity)

    @Query("DELETE FROM study_categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    @Query("UPDATE study_categories SET isArchived = :isArchived WHERE id = :id")
    suspend fun setCategoryArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE study_tasks SET category = :newCategory WHERE category = :oldCategory")
    suspend fun reassignTaskCategory(oldCategory: String, newCategory: String)

    @Query("SELECT COUNT(*) FROM study_categories")
    suspend fun getCategoriesCount(): Int
}

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_tasks ORDER BY isDailyPriority DESC, sortOrder ASC, id DESC")
    fun getAllTasks(): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks ORDER BY isDailyPriority DESC, sortOrder ASC, id DESC")
    suspend fun getTasksListSync(): List<StudyTaskEntity>

    @Query("SELECT * FROM study_tasks WHERE isDailyPriority = 1 LIMIT 1")
    fun getDailyPriority(): Flow<StudyTaskEntity?>

    @Query("SELECT * FROM study_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): StudyTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasksList(tasks: List<StudyTaskEntity>)

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

    @Query("SELECT * FROM study_sessions ORDER BY timestamp ASC")
    suspend fun getAllSessionsSync(): List<StudySessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionsList(sessions: List<StudySessionEntity>)

    @Query("SELECT COUNT(*) FROM study_tasks")
    suspend fun getTasksCount(): Int

    @Query("SELECT COUNT(*) FROM study_sessions")
    suspend fun getSessionsCount(): Int
}

@Dao
interface TimerDraftDao {
    @Query("SELECT * FROM active_timer_draft WHERE id = 1 LIMIT 1")
    fun getActiveDraft(): Flow<ActiveTimerDraftEntity?>

    @Query("SELECT * FROM active_timer_draft WHERE id = 1 LIMIT 1")
    suspend fun getDraftSync(): ActiveTimerDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: ActiveTimerDraftEntity)

    @Query("DELETE FROM active_timer_draft WHERE id = 1")
    suspend fun clearDraft()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    fun getSetting(key: String): Flow<AppSettingEntity?>

    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettingsSync(): List<AppSettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: AppSettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettingsList(settings: List<AppSettingEntity>)
}

@Dao
interface BrainDumpDao {
    @Query("SELECT * FROM brain_dump_notes WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getActiveNotes(): Flow<List<BrainDumpNoteEntity>>

    @Query("SELECT * FROM brain_dump_notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<BrainDumpNoteEntity>>

    @Query("SELECT * FROM brain_dump_notes ORDER BY createdAt ASC")
    suspend fun getAllNotesSync(): List<BrainDumpNoteEntity>

    @Query("SELECT * FROM brain_dump_notes WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<BrainDumpNoteEntity>>

    @Query("SELECT * FROM brain_dump_notes WHERE isArchived = 0 AND (content LIKE '%' || :query || '%' OR (title IS NOT NULL AND title LIKE '%' || :query || '%') OR (tag IS NOT NULL AND tag LIKE '%' || :query || '%')) ORDER BY isPinned DESC, updatedAt DESC")
    fun searchNotes(query: String): Flow<List<BrainDumpNoteEntity>>

    @Query("SELECT * FROM brain_dump_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): BrainDumpNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: BrainDumpNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotesList(notes: List<BrainDumpNoteEntity>)

    @Update
    suspend fun updateNote(note: BrainDumpNoteEntity)

    @Query("DELETE FROM brain_dump_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("UPDATE brain_dump_notes SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setNotePinned(id: Long, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE brain_dump_notes SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setNoteArchived(id: Long, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM brain_dump_notes")
    suspend fun getNotesCount(): Int
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY timeMinutes ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 ORDER BY timeMinutes ASC")
    fun getEnabledReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1")
    suspend fun getEnabledRemindersSync(): List<ReminderEntity>

    @Query("SELECT * FROM reminders ORDER BY timeMinutes ASC")
    suspend fun getAllRemindersListSync(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE referenceId = :referenceId LIMIT 1")
    suspend fun getReminderByReferenceId(referenceId: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE category = :category ORDER BY timeMinutes ASC")
    fun getRemindersByCategory(category: String): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultReminders(reminders: List<ReminderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemindersList(reminders: List<ReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)

    @Query("UPDATE reminders SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setReminderEnabled(id: Long, isEnabled: Boolean)

    @Query("DELETE FROM reminders WHERE referenceId = :referenceId")
    suspend fun deleteReminderByReferenceId(referenceId: String)

    @Query("SELECT COUNT(*) FROM reminders")
    suspend fun getRemindersCount(): Int

    @Query("SELECT COUNT(*) FROM reminders WHERE isEnabled = 1")
    suspend fun getEnabledRemindersCount(): Int

    @Query("SELECT * FROM reminders WHERE title = :title AND timeMinutes = :timeMinutes LIMIT 1")
    suspend fun getReminderByTitleAndTime(title: String, timeMinutes: Int): ReminderEntity?

    @Query("DELETE FROM reminders WHERE id IN (:ids)")
    suspend fun deleteRemindersByIds(ids: List<Long>)
}

@Dao
interface MeditationDao {
    @Query("SELECT * FROM meditation_sessions ORDER BY completedAt DESC")
    fun getAllSessions(): Flow<List<MeditationSessionEntity>>

    @Query("SELECT * FROM meditation_sessions ORDER BY completedAt DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 20): Flow<List<MeditationSessionEntity>>

    @Query("SELECT * FROM meditation_sessions ORDER BY completedAt ASC")
    suspend fun getAllSessionsSync(): List<MeditationSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MeditationSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionsList(sessions: List<MeditationSessionEntity>)

    @Query("DELETE FROM meditation_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT COUNT(*) FROM meditation_sessions")
    suspend fun getSessionsCount(): Int
}

@Dao
interface CustomAlarmDao {
    @Query("SELECT * FROM custom_alarms ORDER BY timeMinutes ASC")
    fun getAllAlarms(): Flow<List<com.example.data.entity.CustomAlarmEntity>>

    @Query("SELECT * FROM custom_alarms ORDER BY timeMinutes ASC")
    suspend fun getAllAlarmsSync(): List<com.example.data.entity.CustomAlarmEntity>

    @Query("SELECT * FROM custom_alarms WHERE id = :id LIMIT 1")
    fun getAlarmById(id: Long): Flow<com.example.data.entity.CustomAlarmEntity?>

    @Query("SELECT * FROM custom_alarms WHERE id = :id LIMIT 1")
    suspend fun getAlarmByIdSync(id: Long): com.example.data.entity.CustomAlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: com.example.data.entity.CustomAlarmEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultAlarms(alarms: List<com.example.data.entity.CustomAlarmEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarmsList(alarms: List<com.example.data.entity.CustomAlarmEntity>)

    @Update
    suspend fun updateAlarm(alarm: com.example.data.entity.CustomAlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: com.example.data.entity.CustomAlarmEntity)

    @Query("DELETE FROM custom_alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Long)

    @Query("UPDATE custom_alarms SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setAlarmEnabled(id: Long, isEnabled: Boolean)

    @Query("SELECT COUNT(*) FROM custom_alarms")
    suspend fun getAlarmsCount(): Int
}


