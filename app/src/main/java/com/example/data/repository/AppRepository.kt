package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.entity.ActiveTimerDraftEntity
import com.example.data.entity.BrainDumpNoteEntity
import com.example.data.entity.HealthCheckinEntity
import com.example.data.entity.MedicationEntity
import com.example.data.entity.MedicationIntakeEntity
import com.example.data.entity.ReminderEntity
import com.example.data.entity.RoutineAnchorEntity
import com.example.data.entity.RoutineEntryEntity
import com.example.data.entity.SleepEntryEntity
import com.example.data.entity.StudyCategoryEntity
import com.example.data.entity.StudySessionEntity
import com.example.data.entity.StudyTaskEntity
import com.example.data.entity.WaterEntryEntity
import com.example.util.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppRepository(private val database: AppDatabase) {

    private val routineDao = database.routineDao()
    private val sleepDao = database.sleepDao()
    private val healthDao = database.healthDao()
    private val waterDao = database.waterDao()
    private val medicationDao = database.medicationDao()
    private val studyCategoryDao = database.studyCategoryDao()
    val studyDao = database.studyDao()
    private val timerDraftDao = database.timerDraftDao()
    private val brainDumpDao = database.brainDumpDao()
    private val settingsDao = database.settingsDao()
    val reminderDao = database.reminderDao()
    private val meditationDao = database.meditationDao()

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
    val archivedMedications: Flow<List<MedicationEntity>> = medicationDao.getArchivedMedications()
    val distinctMedicationNames: Flow<List<String>> = medicationDao.getDistinctMedicationNames()

    fun getMedicationIntakesForDate(date: String): Flow<List<MedicationIntakeEntity>> {
        return medicationDao.getIntakesForDate(date)
    }

    fun getRecentIntakeHistory(limit: Int = 100): Flow<List<MedicationIntakeEntity>> {
        return medicationDao.getRecentIntakeHistory(limit)
    }

    fun getIntakeHistoryForMedication(medicationId: Long): Flow<List<MedicationIntakeEntity>> {
        return medicationDao.getIntakeHistoryForMedication(medicationId)
    }

    suspend fun getMedicationById(id: Long): MedicationEntity? {
        return medicationDao.getMedicationById(id)
    }

    suspend fun getDistinctMedicationNamesSync(): List<String> {
        return medicationDao.getDistinctMedicationNamesSync()
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

    suspend fun archiveMedication(id: Long, isArchived: Boolean) {
        medicationDao.setMedicationArchived(id = id, isArchived = isArchived, isActive = !isArchived)
    }

    suspend fun recordMedicationIntake(intake: MedicationIntakeEntity) {
        medicationDao.insertOrUpdateIntake(intake)
    }

    suspend fun deleteIntakeRecord(id: Long) {
        medicationDao.deleteIntake(id)
    }

    // Study Categories
    val allCategories: Flow<List<StudyCategoryEntity>> = studyCategoryDao.getAllCategories()
    val activeCategories: Flow<List<StudyCategoryEntity>> = studyCategoryDao.getActiveCategories()

    suspend fun ensureDefaultCategories() {
        studyCategoryDao.insertDefaultCategories(AppDatabase.DEFAULT_CATEGORIES)
    }

    suspend fun findCategoryByName(name: String): StudyCategoryEntity? {
        return studyCategoryDao.findCategoryByName(name.trim())
    }

    suspend fun createCategory(category: StudyCategoryEntity): Long {
        return studyCategoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: StudyCategoryEntity) {
        studyCategoryDao.updateCategory(category)
    }

    suspend fun archiveCategory(categoryId: Long, isArchived: Boolean) {
        studyCategoryDao.setCategoryArchived(categoryId, isArchived)
    }

    suspend fun deleteCategoryAndReassignTasks(categoryId: Long, categoryName: String, fallbackCategory: String = "General") {
        studyCategoryDao.reassignTaskCategory(categoryName, fallbackCategory)
        studyCategoryDao.deleteCategory(categoryId)
    }

    // Study Tasks
    val allStudyTasks: Flow<List<StudyTaskEntity>> = studyDao.getAllTasks()
    val dailyPriorityTask: Flow<StudyTaskEntity?> = studyDao.getDailyPriority()

    suspend fun getStudyTaskById(id: Long): StudyTaskEntity? {
        return studyDao.getTaskById(id)
    }

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

    // Active Timer Draft
    val activeTimerDraft: Flow<ActiveTimerDraftEntity?> = timerDraftDao.getActiveDraft()

    suspend fun getTimerDraftSync(): ActiveTimerDraftEntity? {
        return timerDraftDao.getDraftSync()
    }

    suspend fun saveTimerDraft(draft: ActiveTimerDraftEntity) {
        timerDraftDao.saveDraft(draft)
    }

    suspend fun clearTimerDraft() {
        timerDraftDao.clearDraft()
    }

    // Brain Dump
    val activeBrainDumpNotes: Flow<List<BrainDumpNoteEntity>> = brainDumpDao.getActiveNotes()
    val allBrainDumpNotes: Flow<List<BrainDumpNoteEntity>> = brainDumpDao.getAllNotes()
    val archivedBrainDumpNotes: Flow<List<BrainDumpNoteEntity>> = brainDumpDao.getArchivedNotes()

    fun searchBrainDumpNotes(query: String): Flow<List<BrainDumpNoteEntity>> {
        return brainDumpDao.searchNotes(query)
    }

    suspend fun getBrainDumpNoteById(id: Long): BrainDumpNoteEntity? {
        return brainDumpDao.getNoteById(id)
    }

    suspend fun saveBrainDumpNote(note: BrainDumpNoteEntity): Long {
        return brainDumpDao.insertNote(note)
    }

    suspend fun updateBrainDumpNote(note: BrainDumpNoteEntity) {
        brainDumpDao.updateNote(note)
    }

    suspend fun deleteBrainDumpNote(id: Long) {
        brainDumpDao.deleteNote(id)
    }

    suspend fun setBrainDumpNotePinned(id: Long, isPinned: Boolean) {
        brainDumpDao.setNotePinned(id, isPinned)
    }

    suspend fun setBrainDumpNoteArchived(id: Long, isArchived: Boolean) {
        brainDumpDao.setNoteArchived(id, isArchived)
    }

    suspend fun convertNoteToStudyTask(
        note: BrainDumpNoteEntity,
        category: String = "General",
        estimatedMinutes: Int = 25
    ): Long {
        val task = StudyTaskEntity(
            title = note.displayTitle(),
            category = category,
            estimatedMinutes = estimatedMinutes,
            dateCreated = com.example.util.DateTimeUtils.todayDateString()
        )
        return studyDao.insertTask(task)
    }

    // Reminders
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()
    val enabledReminders: Flow<List<ReminderEntity>> = reminderDao.getEnabledReminders()

    suspend fun ensureDefaultReminders() {
        val count = reminderDao.getRemindersCount()
        if (count == 0) {
            reminderDao.insertDefaultReminders(AppDatabase.DEFAULT_REMINDERS)
            return
        }
        for (def in AppDatabase.DEFAULT_REMINDERS) {
            val existing = def.referenceId?.let { reminderDao.getReminderByReferenceId(it) }
                ?: reminderDao.getReminderByTitleAndTime(def.title, def.timeMinutes)
            if (existing == null) {
                reminderDao.insertReminder(def)
            }
        }
    }

    suspend fun cleanupDuplicateReminders(context: android.content.Context? = null): Int {
        val all = reminderDao.getAllRemindersListSync()
        if (all.isEmpty()) return 0

        val groups = all.groupBy { r ->
            if (!r.referenceId.isNullOrBlank()) {
                "ref:${r.referenceId.trim().lowercase()}"
            } else {
                "title:${r.title.trim().lowercase()}_${r.timeMinutes}_${r.category}"
            }
        }

        val idsToDelete = mutableListOf<Long>()
        for ((_, group) in groups) {
            if (group.size > 1) {
                val keep = group.sortedWith(
                    compareByDescending<ReminderEntity> { it.isEnabled }
                        .thenBy { it.id }
                ).first()

                val duplicates = group.filter { it.id != keep.id }
                for (dup in duplicates) {
                    idsToDelete.add(dup.id)
                    if (context != null) {
                        AlarmScheduler.cancelAlarm(context, dup.id)
                    }
                }
            }
        }

        if (idsToDelete.isNotEmpty()) {
            reminderDao.deleteRemindersByIds(idsToDelete)
        }
        return idsToDelete.size
    }

    suspend fun getEnabledRemindersCount(): Int {
        return reminderDao.getEnabledRemindersCount()
    }

    suspend fun getEnabledRemindersSync(): List<ReminderEntity> {
        return reminderDao.getEnabledRemindersSync()
    }

    suspend fun getReminderById(id: Long): ReminderEntity? {
        return reminderDao.getReminderById(id)
    }

    suspend fun saveReminder(reminder: ReminderEntity): Long {
        return reminderDao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: ReminderEntity) {
        reminderDao.updateReminder(reminder)
    }

    suspend fun deleteReminder(id: Long) {
        reminderDao.deleteReminder(id)
    }

    suspend fun setReminderEnabled(id: Long, isEnabled: Boolean) {
        reminderDao.setReminderEnabled(id, isEnabled)
    }

    // Meditation Sessions
    val allMeditationSessions: Flow<List<com.example.data.entity.MeditationSessionEntity>> = meditationDao.getAllSessions()
    val recentMeditationSessions: Flow<List<com.example.data.entity.MeditationSessionEntity>> = meditationDao.getRecentSessions(20)

    suspend fun saveMeditationSession(session: com.example.data.entity.MeditationSessionEntity): Long {
        return meditationDao.insertSession(session)
    }

    suspend fun deleteMeditationSession(id: Long) {
        meditationDao.deleteSession(id)
    }

    suspend fun getMeditationSessionsCount(): Int {
        return meditationDao.getSessionsCount()
    }

    // Settings
    fun getSetting(key: String): Flow<com.example.data.entity.AppSettingEntity?> {
        return settingsDao.getSetting(key)
    }

    suspend fun saveSetting(key: String, value: String) {
        settingsDao.saveSetting(com.example.data.entity.AppSettingEntity(key, value))
    }

    suspend fun syncRoutinesToReminders() {
        val anchors = routineDao.getAllAnchors().first()
        for (anchor in anchors) {
            val refId = when (anchor.id) {
                "out_of_bed" -> "default:wake_up"
                "dinner" -> "default:dinner"
                "getting_into_bed" -> "default:bedtime"
                else -> "anchor:${anchor.id}"
            }
            val existing = reminderDao.getReminderByReferenceId(refId)
                ?: reminderDao.getReminderByReferenceId("anchor:${anchor.id}")
            val category = when (anchor.id) {
                "out_of_bed" -> "WAKE_UP"
                "getting_into_bed" -> "BEDTIME"
                "dinner" -> "MEAL"
                else -> "ROUTINE_ANCHOR"
            }
            if (existing != null) {
                reminderDao.updateReminder(
                    existing.copy(
                        title = anchor.name,
                        timeMinutes = anchor.defaultTargetMinutes,
                        category = category,
                        referenceId = refId
                    )
                )
            } else {
                reminderDao.insertReminder(
                    ReminderEntity(
                        title = anchor.name,
                        category = category,
                        timeMinutes = anchor.defaultTargetMinutes,
                        repeatDays = "DAILY",
                        isEnabled = anchor.isEnabled,
                        soundEnabled = true,
                        vibrateEnabled = true,
                        isUrgent = false,
                        referenceId = refId,
                        customMessage = anchor.description
                    )
                )
            }
        }
    }

    suspend fun syncMedicationsToReminders() {
        val meds = medicationDao.getAllMedications().first()
        for (med in meds) {
            if (med.isArchived || !med.isActive) continue
            val scheduledTimes = med.getScheduledTimes()
            for ((index, timeMin) in scheduledTimes.withIndex()) {
                val refId = "med:${med.id}_$index"
                val existing = reminderDao.getReminderByReferenceId(refId)
                val dosageDisplay = if (med.dosage.isNotBlank()) " (${med.dosage} ${med.unit})" else ""
                val title = "${med.name}$dosageDisplay"
                val customMessage = if (med.instructions.isNotBlank()) med.instructions else "Scheduled dose"

                if (existing != null) {
                    reminderDao.updateReminder(
                        existing.copy(
                            title = title,
                            timeMinutes = timeMin,
                            repeatDays = med.repeatDays,
                            isEnabled = med.reminderEnabled,
                            isUrgent = true,
                            customMessage = customMessage
                        )
                    )
                } else if (med.reminderEnabled) {
                    reminderDao.insertReminder(
                        ReminderEntity(
                            title = title,
                            category = "MEDICATION",
                            timeMinutes = timeMin,
                            repeatDays = med.repeatDays,
                            isEnabled = true,
                            soundEnabled = true,
                            vibrateEnabled = true,
                            isUrgent = true,
                            referenceId = refId,
                            customMessage = customMessage
                        )
                    )
                }
            }
        }
    }

    // Database Reset / Management for Settings
    suspend fun clearAllData() {
        database.clearAllTables()
        ensureDefaultAnchors()
        ensureDefaultCategories()
        ensureDefaultReminders()
    }

    suspend fun exportDataBackup(): String {
        return com.example.util.DataBackupManager.exportBackup(database)
    }

    suspend fun restoreDataBackup(jsonString: String): Result<com.example.util.BackupSummary> {
        return com.example.util.DataBackupManager.restoreBackup(database, jsonString)
    }

    suspend fun getDatabaseIntegrityStats(): com.example.util.DatabaseIntegrityStats {
        return com.example.util.DataBackupManager.getDatabaseIntegrityStats(database)
    }
}
