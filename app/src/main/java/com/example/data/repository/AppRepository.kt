package com.example.data.repository

import com.example.data.database.AppDatabase
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

class AppRepository(private val database: AppDatabase) {

    private val routineDao = database.routineDao()
    private val sleepDao = database.sleepDao()
    private val healthDao = database.healthDao()
    private val waterDao = database.waterDao()
    private val medicationDao = database.medicationDao()
    private val studyDao = database.studyDao()
    private val settingsDao = database.settingsDao()

    // Routine
    val routineAnchors: Flow<List<RoutineAnchorEntity>> = routineDao.getAllAnchors()

    suspend fun ensureDefaultAnchors() {
        routineDao.insertDefaultAnchors(AppDatabase.DEFAULT_ANCHORS)
    }

    suspend fun updateAnchor(anchor: RoutineAnchorEntity) {
        routineDao.updateAnchor(anchor)
    }

    fun getRoutineEntriesForDate(date: String): Flow<List<RoutineEntryEntity>> {
        return routineDao.getEntriesForDate(date)
    }

    fun getRoutineEntriesBetween(startDate: String, endDate: String): Flow<List<RoutineEntryEntity>> {
        return routineDao.getEntriesBetweenDates(startDate, endDate)
    }

    suspend fun saveRoutineEntry(entry: RoutineEntryEntity) {
        routineDao.insertOrUpdateEntry(entry)
    }

    // Sleep
    fun getSleepForDate(date: String): Flow<SleepEntryEntity?> {
        return sleepDao.getSleepForDate(date)
    }

    fun getRecentSleepHistory(limit: Int = 14): Flow<List<SleepEntryEntity>> {
        return sleepDao.getRecentSleepHistory(limit)
    }

    fun getSleepBetween(startDate: String, endDate: String): Flow<List<SleepEntryEntity>> {
        return sleepDao.getSleepBetweenDates(startDate, endDate)
    }

    suspend fun saveSleepEntry(sleep: SleepEntryEntity) {
        sleepDao.insertOrUpdateSleep(sleep)
    }

    suspend fun deleteSleep(date: String) {
        sleepDao.deleteSleepForDate(date)
    }

    // Health
    fun getHealthForDate(date: String): Flow<HealthCheckinEntity?> {
        return healthDao.getHealthForDate(date)
    }

    fun getRecentHealthHistory(limit: Int = 14): Flow<List<HealthCheckinEntity>> {
        return healthDao.getRecentHealthHistory(limit)
    }

    fun getHealthBetween(startDate: String, endDate: String): Flow<List<HealthCheckinEntity>> {
        return healthDao.getHealthBetweenDates(startDate, endDate)
    }

    suspend fun saveHealthCheckin(health: HealthCheckinEntity) {
        healthDao.insertOrUpdateHealth(health)
    }

    // Water
    fun getWaterEntriesForDate(date: String): Flow<List<WaterEntryEntity>> {
        return waterDao.getWaterEntriesForDate(date)
    }

    fun getWaterBetween(startDate: String, endDate: String): Flow<List<WaterEntryEntity>> {
        return waterDao.getWaterBetweenDates(startDate, endDate)
    }

    suspend fun addWater(date: String, volumeMl: Int) {
        waterDao.insertWaterEntry(
            WaterEntryEntity(
                date = date,
                volumeMl = volumeMl,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeWaterEntry(id: Long) {
        waterDao.deleteWaterEntry(id)
    }

    // Medication
    val activeMedications: Flow<List<MedicationEntity>> = medicationDao.getActiveMedications()
    val allMedications: Flow<List<MedicationEntity>> = medicationDao.getAllMedications()

    fun getMedicationIntakesForDate(date: String): Flow<List<MedicationIntakeEntity>> {
        return medicationDao.getIntakesForDate(date)
    }

    suspend fun saveMedication(medication: MedicationEntity): Long {
        return medicationDao.insertMedication(medication)
    }

    suspend fun updateMedication(medication: MedicationEntity) {
        medicationDao.updateMedication(medication)
    }

    suspend fun deleteMedication(id: Long) {
        medicationDao.deleteMedication(id)
    }

    suspend fun recordMedicationIntake(intake: MedicationIntakeEntity) {
        medicationDao.insertOrUpdateIntake(intake)
    }

    // Study
    val allStudyTasks: Flow<List<StudyTaskEntity>> = studyDao.getAllTasks()
    val dailyPriorityTask: Flow<StudyTaskEntity?> = studyDao.getDailyPriority()

    suspend fun saveStudyTask(task: StudyTaskEntity): Long {
        return studyDao.insertTask(task)
    }

    suspend fun updateStudyTask(task: StudyTaskEntity) {
        studyDao.updateTask(task)
    }

    suspend fun deleteStudyTask(id: Long) {
        studyDao.deleteTask(id)
    }

    suspend fun setDailyPriority(taskId: Long) {
        studyDao.clearDailyPriorities()
        studyDao.setDailyPriority(taskId)
    }

    suspend fun clearDailyPriority() {
        studyDao.clearDailyPriorities()
    }

    fun getRecentStudySessions(limit: Int = 30): Flow<List<StudySessionEntity>> {
        return studyDao.getRecentSessions(limit)
    }

    fun getStudySessionsForDate(date: String): Flow<List<StudySessionEntity>> {
        return studyDao.getSessionsForDate(date)
    }

    fun getStudySessionsBetween(startDate: String, endDate: String): Flow<List<StudySessionEntity>> {
        return studyDao.getSessionsBetweenDates(startDate, endDate)
    }

    suspend fun recordStudySession(session: StudySessionEntity) {
        studyDao.insertSession(session)
    }

    // Database Reset / Management for Settings
    suspend fun clearAllData() {
        database.clearAllTables()
        ensureDefaultAnchors()
    }
}
