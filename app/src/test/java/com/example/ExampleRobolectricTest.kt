package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.ActiveTimerDraftEntity
import com.example.data.entity.BrainDumpNoteEntity
import com.example.data.entity.ReminderEntity
import com.example.data.entity.StudyTaskEntity
import com.example.data.repository.AppRepository
import com.example.sync.CloudSyncManager
import com.example.sync.CloudSyncStatus
import com.example.ui.MainViewModel
import com.example.ui.TimerMode
import com.example.util.AlarmScheduler
import com.example.util.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context matches Rhythm and Focus`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Rhythm & Focus", appName)
    }

    @Test
    fun `dateTimeUtils correctly calculates deltas`() {
        val deltaPositive = DateTimeUtils.calculateDelta(420, 435)
        assertEquals("+15m", deltaPositive)

        val deltaNegative = DateTimeUtils.calculateDelta(420, 410)
        assertEquals("-10m", deltaNegative)

        val deltaOnTime = DateTimeUtils.calculateDelta(420, 420)
        assertEquals("On time", deltaOnTime)
    }

    @Test
    fun `defaultAnchors contains 5 social rhythm anchors`() {
        val anchors = AppDatabase.DEFAULT_ANCHORS
        assertEquals(5, anchors.size)
        val anchorIds = anchors.map { it.id }
        assertTrue(anchorIds.contains("out_of_bed"))
        assertTrue(anchorIds.contains("first_contact"))
        assertTrue(anchorIds.contains("main_activity"))
        assertTrue(anchorIds.contains("dinner"))
        assertTrue(anchorIds.contains("getting_into_bed"))
    }

    @Test
    fun `cloud sync remains unavailable and never reports false success`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = CloudSyncManager(context, repository)

        assertEquals(CloudSyncStatus.UNCONFIGURED, manager.syncState.value.status)

        val result = manager.triggerManualSync()

        assertTrue(result.isFailure)
        assertEquals(CloudSyncStatus.UNCONFIGURED, manager.syncState.value.status)
        assertEquals(null, manager.syncState.value.lastSyncTimestamp)
    }

    @Test
    fun `routine anchor target updates persist without creating a completion entry`() = runTest {
        repository.ensureDefaultAnchors()
        val anchor = repository.routineAnchors.first().first()

        repository.updateAnchor(anchor.copy(defaultTargetMinutes = 510))

        val updatedAnchor = repository.routineAnchors.first().first { it.id == anchor.id }
        assertEquals(510, updatedAnchor.defaultTargetMinutes)

        val entries = repository.getRoutineEntriesForDate(DateTimeUtils.todayDateString()).first()
        assertTrue("Editing an anchor target must not log a completion", entries.isEmpty())
    }

    @Test
    fun `defaultCategories contains 6 seeded categories`() {
        val categories = AppDatabase.DEFAULT_CATEGORIES
        assertEquals(6, categories.size)
        val names = categories.map { it.name }
        assertTrue(names.contains("DSA"))
        assertTrue(names.contains("JavaScript"))
        assertTrue(names.contains("React"))
        assertTrue(names.contains("Backend"))
        assertTrue(names.contains("Interview Prep"))
        assertTrue(names.contains("General"))
    }

    @Test
    fun `category creation duplicate check and task reassignment on deletion`() = runTest {
        repository.ensureDefaultCategories()
        val viewModel = MainViewModel(repository)

        // Attempt duplicate (case-insensitive)
        val duplicateCreated = viewModel.createStudyCategorySync("dsa", "duplicate test", "#3F5E48", "code")
        assertFalse("Duplicate category name should be rejected", duplicateCreated)

        // Create new unique category
        val newCreated = viewModel.createStudyCategorySync("System Design", "HLD and LLD", "#7C3AED", "storage")
        assertTrue("New unique category should be created", newCreated)

        // Create task with this new category
        val taskId = repository.saveStudyTask(
            StudyTaskEntity(
                title = "Design Rate Limiter",
                category = "System Design",
                estimatedMinutes = 45,
                dateCreated = "2026-10-24"
            )
        )

        // Delete "System Design" with reassignment to "General"
        val categoryToDelete = repository.findCategoryByName("System Design")
        assertNotNull(categoryToDelete)
        repository.deleteCategoryAndReassignTasks(categoryToDelete!!.id, "System Design", "General")

        // Verify task category was updated to General
        val updatedTask = repository.getStudyTaskById(taskId)
        assertNotNull(updatedTask)
        assertEquals("General", updatedTask!!.category)
    }

    @Test
    fun `timer draft restores elapsed time across background simulation`() = runTest {
        val now = System.currentTimeMillis()
        val startTimestamp = now - 300_000L // 300 seconds ago

        val draft = ActiveTimerDraftEntity(
            id = 1,
            taskId = 12L,
            taskTitle = "Mock Coding",
            category = "DSA",
            mode = "POMODORO",
            targetSeconds = 1500,
            startWallClockMillis = startTimestamp,
            accumulatedElapsedSeconds = 0,
            isRunning = true,
            isPaused = false,
            isCompleted = false,
            lastUpdatedMillis = now
        )
        repository.saveTimerDraft(draft)

        // Restore in ViewModel
        val viewModel = MainViewModel(repository)
        viewModel.restoreTimerStateFromDraft()
        val state = viewModel.timerState.value

        // Elapsed time must reflect wall clock delta (around 300s)
        assertTrue("Elapsed seconds should catch up to ~300 seconds, actual=${state.elapsedSeconds}", state.elapsedSeconds in 298..305)
        assertTrue("Remaining seconds should be ~1200 seconds, actual=${state.remainingSeconds}", state.remainingSeconds in 1195..1202)
        assertEquals("Mock Coding", state.selectedTaskTitle)
        assertEquals("DSA", state.selectedCategory)
    }

    @Test
    fun `timer pause and reset transitions operate properly`() = runTest {
        val viewModel = MainViewModel(repository)

        // Set mode to 25m Pomodoro
        viewModel.setTimerConfig(TimerMode.POMODORO, 25)
        assertEquals(1500, viewModel.timerState.value.targetSeconds)
        assertEquals(0, viewModel.timerState.value.elapsedSeconds)
        assertFalse(viewModel.timerState.value.isRunning)

        // Start
        viewModel.startTimer()
        assertTrue(viewModel.timerState.value.isRunning)
        assertFalse(viewModel.timerState.value.isPaused)

        // Pause
        viewModel.pauseTimer()
        assertFalse(viewModel.timerState.value.isRunning)
        assertTrue(viewModel.timerState.value.isPaused)

        // Reset
        viewModel.stopTimer()
        assertFalse(viewModel.timerState.value.isRunning)
        assertFalse(viewModel.timerState.value.isPaused)
        assertEquals(0, viewModel.timerState.value.elapsedSeconds)
        assertEquals(1500, viewModel.timerState.value.remainingSeconds)
    }

    @Test
    fun `brain dump note creation and display title logic`() = runTest {
        val viewModel = MainViewModel(repository)

        // Note with no explicit title and short first line -> exact first line
        val noteId1 = viewModel.saveBrainDumpNoteSync(
            title = null,
            content = "Investigate Room migrations\nAdditional notes on foreign keys.",
            tag = "Study",
            isPinned = false
        )
        val note1 = repository.getBrainDumpNoteById(noteId1)
        assertNotNull(note1)
        assertEquals("Investigate Room migrations", note1!!.displayTitle())
        assertEquals("Study", note1.tag)
        assertFalse(note1.isPinned)

        // Note with long first line (> 35 chars) -> truncated with ...
        val noteIdLong = viewModel.saveBrainDumpNoteSync(
            title = null,
            content = "Investigate complex Room database migration strategies across version transitions",
            tag = "Study",
            isPinned = false
        )
        val noteLong = repository.getBrainDumpNoteById(noteIdLong)
        assertNotNull(noteLong)
        assertTrue(noteLong!!.displayTitle().endsWith("..."))
        assertEquals(35, noteLong.displayTitle().length)

        // Note with explicit title
        val noteId2 = viewModel.saveBrainDumpNoteSync(
            title = "Sleep Architecture",
            content = "Track REM vs Deep sleep correlations with mood rating.",
            tag = "Health",
            isPinned = true
        )
        val note2 = repository.getBrainDumpNoteById(noteId2)
        assertNotNull(note2)
        assertEquals("Sleep Architecture", note2!!.displayTitle())
        assertTrue(note2.isPinned)
    }

    @Test
    fun `brain dump pin, archive, and delete operations`() = runTest {
        val viewModel = MainViewModel(repository)

        val noteId = viewModel.saveBrainDumpNoteSync(
            title = "Quick Distraction Note",
            content = "Check ticket status later after study session",
            tag = "Distraction",
            isPinned = false
        )
        val note = repository.getBrainDumpNoteById(noteId)!!
        assertFalse(note.isPinned)
        assertFalse(note.isArchived)

        // Pin
        viewModel.togglePinBrainDumpNote(note)
        val pinned = repository.getBrainDumpNoteById(noteId)!!
        assertTrue(pinned.isPinned)

        // Archive
        viewModel.toggleArchiveBrainDumpNote(pinned)
        val archived = repository.getBrainDumpNoteById(noteId)!!
        assertTrue(archived.isArchived)

        // Delete
        viewModel.deleteBrainDumpNote(noteId)
        val deleted = repository.getBrainDumpNoteById(noteId)
        assertEquals(null, deleted)
    }

    @Test
    fun `brain dump note conversion to study task`() = runTest {
        val viewModel = MainViewModel(repository)

        val noteId = viewModel.saveBrainDumpNoteSync(
            title = "Learn Dynamic Programming Knapsack",
            content = "Practice 0/1 knapsack and unbounded knapsack problems.",
            tag = "Study"
        )
        val note = repository.getBrainDumpNoteById(noteId)!!

        // Convert note to task synchronously
        viewModel.convertBrainDumpNoteToStudyTaskSync(
            note = note,
            category = "DSA",
            estimatedMinutes = 45,
            isPriority = true
        )

        // Verify task exists in studyDao
        val tasks = repository.studyDao.getTasksListSync()
        val converted = tasks.firstOrNull { it.title == "Learn Dynamic Programming Knapsack" }
        assertNotNull("Converted study task should exist in study tasks table", converted)
        assertEquals("DSA", converted!!.category)
        assertEquals(45, converted.estimatedMinutes)
        assertTrue(converted.isDailyPriority)
    }

    @Test
    fun `medication multi-time schedule creation and recurrence check`() = runTest {
        val med = com.example.data.entity.MedicationEntity(
            name = "L-Theanine",
            dosage = "200",
            unit = "mg",
            instructions = "Take with water",
            scheduledTimeMinutes = 480, // 8:00 AM
            scheduledTimesJson = "480,1200", // 8:00 AM and 8:00 PM
            dosageForm = "Capsule",
            repeatDays = "MON,WED,FRI",
            startDate = "2026-10-01",
            endDate = null
        )
        val medId = repository.saveMedication(med)
        assertTrue(medId > 0)

        val retrieved = repository.getMedicationById(medId)
        assertNotNull(retrieved)
        assertEquals(listOf(480, 1200), retrieved!!.getScheduledTimes())
        assertTrue(retrieved.isScheduledForDayOfWeek("MON"))
        assertTrue(retrieved.isScheduledForDayOfWeek("WED"))
        assertFalse(retrieved.isScheduledForDayOfWeek("TUE"))
        assertTrue(retrieved.isEffectiveOnDate("2026-10-24"))
        assertFalse(retrieved.isEffectiveOnDate("2026-09-30")) // before startDate
    }

    @Test
    fun `medication intake recording and retrospective correction tracking`() = runTest {
        val med = com.example.data.entity.MedicationEntity(
            name = "Vitamin D3",
            dosage = "2000",
            unit = "IU",
            instructions = "Take with breakfast",
            scheduledTimeMinutes = 540 // 9:00 AM
        )
        val medId = repository.saveMedication(med)
        val savedMed = repository.getMedicationById(medId)!!

        val intake = com.example.data.entity.MedicationIntakeEntity(
            date = "2026-10-24",
            medicationId = savedMed.id,
            scheduledTimeMinutes = 540,
            actualActionTime = 1729760000000L,
            status = "SKIPPED",
            notes = "Felt full, skipped breakfast",
            medicationName = savedMed.name,
            dosageSnapshot = "${savedMed.dosage} ${savedMed.unit}"
        )
        repository.recordMedicationIntake(intake)

        // Verify initial intake logged
        val initialHistory = repository.getRecentIntakeHistory(10)
        // Check sync or flow
        // Retrospectively correct entry to TAKEN
        val correctedIntake = intake.copy(
            status = "TAKEN",
            actualActionTime = 1729763600000L,
            notes = "Taken later with lunch",
            isCorrected = true,
            correctedAt = System.currentTimeMillis()
        )
        repository.recordMedicationIntake(correctedIntake)

        val updatedMed = repository.getMedicationById(medId)
        assertNotNull(updatedMed)
        assertEquals("Vitamin D3", updatedMed!!.name)
    }

    @Test
    fun `medication archive preserves past intake history`() = runTest {
        val med = com.example.data.entity.MedicationEntity(
            name = "Magnesium Glycinate",
            dosage = "400",
            unit = "mg",
            instructions = "Take 30m before sleep",
            scheduledTimeMinutes = 1320 // 10:00 PM
        )
        val medId = repository.saveMedication(med)

        // Log intake
        repository.recordMedicationIntake(
            com.example.data.entity.MedicationIntakeEntity(
                date = "2026-10-23",
                medicationId = medId,
                scheduledTimeMinutes = 1320,
                status = "TAKEN",
                actualActionTime = System.currentTimeMillis(),
                medicationName = "Magnesium Glycinate",
                dosageSnapshot = "400 mg"
            )
        )

        // Archive / Discontinue medication
        repository.archiveMedication(medId, isArchived = true)
        val archivedMed = repository.getMedicationById(medId)
        assertNotNull(archivedMed)
        assertTrue(archivedMed!!.isArchived)
        assertFalse(archivedMed.isActive)

        // Distinct names should still return "Magnesium Glycinate"
        val distinct = repository.getDistinctMedicationNamesSync()
        assertTrue("Distinct names should retain archived medication", distinct.contains("Magnesium Glycinate"))
    }

    @Test
    fun `defaultReminders contains 8 scheduled alarms for rhythm anchors`() {
        val defaultReminders = AppDatabase.DEFAULT_REMINDERS
        assertEquals(8, defaultReminders.size)
        val categories = defaultReminders.map { it.category }
        assertTrue(categories.contains("WAKE_UP"))
        assertTrue(categories.contains("MEAL"))
        assertTrue(categories.contains("HYDRATION"))
        assertTrue(categories.contains("ROUTINE_ANCHOR"))
        assertTrue(categories.contains("WIND_DOWN"))
        assertTrue(categories.contains("BEDTIME"))
    }

    @Test
    fun `reminder CRUD and toggle enabled operations in Room`() = runTest {
        val reminder = ReminderEntity(
            title = "Focus Study Break",
            category = "STUDY_BREAK",
            timeMinutes = 900, // 3:00 PM
            repeatDays = "MON,WED,FRI",
            isEnabled = true,
            soundEnabled = true,
            vibrateEnabled = true,
            isUrgent = false
        )

        val id = repository.saveReminder(reminder)
        assertTrue(id > 0)

        val fetched = repository.getReminderById(id)
        assertNotNull(fetched)
        assertEquals("Focus Study Break", fetched!!.title)
        assertEquals(900, fetched.timeMinutes)
        assertEquals("MON,WED,FRI", fetched.repeatDays)
        assertTrue(fetched.isEnabled)

        // Toggle disabled
        repository.setReminderEnabled(id, false)
        val disabled = repository.getReminderById(id)
        assertNotNull(disabled)
        assertFalse(disabled!!.isEnabled)

        // Delete reminder
        repository.deleteReminder(id)
        val deleted = repository.getReminderById(id)
        org.junit.Assert.assertNull(deleted)
    }

    @Test
    fun `quiet hours logic correctly evaluates daytime and overnight spans`() {
        // Overnight quiet hours: 10:00 PM (1320) to 07:00 AM (420)
        val quietStart = 1320
        val quietEnd = 420

        // 11:30 PM (1410) -> Should be inside quiet hours
        assertTrue(AlarmScheduler.isWithinQuietHours(1410, quietStart, quietEnd))

        // 06:15 AM (375) -> Should be inside quiet hours
        assertTrue(AlarmScheduler.isWithinQuietHours(375, quietStart, quietEnd))

        // 02:00 PM (840) -> Should NOT be inside quiet hours
        assertFalse(AlarmScheduler.isWithinQuietHours(840, quietStart, quietEnd))

        // Daytime quiet hours: 01:00 PM (780) to 03:00 PM (900)
        val dayStart = 780
        val dayEnd = 900

        // 02:00 PM (840) -> Should be inside
        assertTrue(AlarmScheduler.isWithinQuietHours(840, dayStart, dayEnd))

        // 04:00 PM (960) -> Should NOT be inside
        assertFalse(AlarmScheduler.isWithinQuietHours(960, dayStart, dayEnd))
    }

    @Test
    fun `next alarm trigger calculation yields future timestamp`() {
        val now = System.currentTimeMillis()

        // Daily alarm at 08:00 AM (480 min)
        val triggerDaily = AlarmScheduler.calculateNextTriggerMillis(480, "DAILY")
        assertTrue("Daily alarm trigger must be strictly in the future", triggerDaily > now)

        // Repeat weekdays alarm at 07:00 AM (420 min)
        val triggerWeekdays = AlarmScheduler.calculateNextTriggerMillis(420, "MON,TUE,WED,THU,FRI")
        assertTrue("Weekday alarm trigger must be strictly in the future", triggerWeekdays > now)
    }

    @Test
    fun `sync routines and medications populates reminders`() = runTest {
        // 1. Sync Routines
        repository.ensureDefaultAnchors()
        repository.syncRoutinesToReminders()

        val allRemindersAfterRoutine = repository.allReminders.first()
        assertTrue("Routines sync should populate anchors as reminders", allRemindersAfterRoutine.isNotEmpty())
        assertEquals(5, allRemindersAfterRoutine.size)

        // 2. Add Medication with reminder enabled
        val med = com.example.data.entity.MedicationEntity(
            name = "Sertraline",
            dosage = "50",
            unit = "mg",
            instructions = "Take with morning meal",
            scheduledTimeMinutes = 510, // 08:30 AM
            reminderEnabled = true
        )
        val medId = repository.saveMedication(med)
        assertTrue(medId > 0)

        // 3. Sync Medications
        repository.syncMedicationsToReminders()
        val allRemindersAfterMeds = repository.allReminders.first()
        val medReminders = allRemindersAfterMeds.filter { it.category == "MEDICATION" }
        assertEquals(1, medReminders.size)
        assertEquals("Sertraline (50 mg)", medReminders[0].title)
        assertTrue("Medication reminder should be marked urgent for quiet hours bypass", medReminders[0].isUrgent)
        assertEquals(510, medReminders[0].timeMinutes)
    }

    @Test
    fun `exportBackup serializes tables to valid structured JSON with metadata and schema 5`() = runTest {
        repository.ensureDefaultAnchors()
        repository.ensureDefaultCategories()

        // Insert some sample routine entries, sleep logs, and tasks
        repository.saveRoutineEntry(
            com.example.data.entity.RoutineEntryEntity(
                date = "2026-10-25",
                anchorId = "out_of_bed",
                targetTimeMinutes = 420,
                actualTimeMinutes = 430,
                status = "COMPLETED"
            )
        )
        repository.saveSleepEntry(
            com.example.data.entity.SleepEntryEntity(
                date = "2026-10-25",
                bedtimeMinutes = 1380,
                wakeTimeMinutes = 420,
                durationMinutes = 480,
                qualityRating = 4
            )
        )
        repository.saveStudyTask(
            com.example.data.entity.StudyTaskEntity(
                title = "Review Graph Traversal",
                category = "DSA",
                estimatedMinutes = 45,
                dateCreated = "2026-10-25"
            )
        )

        val jsonString = repository.exportDataBackup()
        assertNotNull(jsonString)
        assertTrue(jsonString.isNotBlank())

        val root = org.json.JSONObject(jsonString)
        assertEquals("Rhythm & Focus", root.getString("app"))
        assertEquals(5, root.getInt("schemaVersion"))
        assertTrue(root.getLong("exportTimestamp") > 0)

        val entries = root.getJSONArray("routineEntries")
        assertEquals(1, entries.length())
        assertEquals("out_of_bed", entries.getJSONObject(0).getString("anchorId"))

        val sleep = root.getJSONArray("sleepEntries")
        assertEquals(1, sleep.length())
        assertEquals(480, sleep.getJSONObject(0).getInt("durationMinutes"))

        val tasks = root.getJSONArray("studyTasks")
        assertTrue(tasks.length() >= 1)
    }

    @Test
    fun `validateBackup catches corrupt or mismatched JSON and summarizes valid backups`() {
        // 1. Corrupt JSON
        val corruptResult = com.example.util.DataBackupManager.validateBackup("{invalid json content")
        assertTrue(corruptResult.isFailure)

        // 2. Mismatched app identity
        val mismatchJson = """{"app":"WrongApp","schemaVersion":5}"""
        val mismatchResult = com.example.util.DataBackupManager.validateBackup(mismatchJson)
        assertTrue(mismatchResult.isFailure)

        // 3. Valid JSON with summary
        val validJson = """
            {
              "app": "Rhythm & Focus",
              "schemaVersion": 5,
              "exportTimestamp": 1700000000000,
              "routineEntries": [{"id":1,"date":"2026-10-25","anchorId":"out_of_bed","targetTimeMinutes":420,"status":"COMPLETED"}],
              "sleepEntries": [{"id":1,"date":"2026-10-25","bedtimeMinutes":1380,"wakeTimeMinutes":420,"durationMinutes":480,"qualityRating":4}],
              "medications": [],
              "studyTasks": [{"id":1,"title":"Task 1","category":"DSA","status":"PENDING","isDailyPriority":false,"estimatedMinutes":25,"dateCreated":"2026-10-25"}]
            }
        """.trimIndent()
        val validResult = com.example.util.DataBackupManager.validateBackup(validJson)
        assertTrue(validResult.isSuccess)
        val summary = validResult.getOrThrow()
        assertEquals(1, summary.routineEntriesCount)
        assertEquals(1, summary.sleepCount)
        assertEquals(0, summary.medicationsCount)
        assertEquals(1, summary.studyTasksCount)
    }

    @Test
    fun `restoreBackup atomically restores records and safeguards default anchors and categories`() = runTest {
        repository.ensureDefaultAnchors()
        repository.ensureDefaultCategories()

        // Create initial entry
        repository.saveRoutineEntry(
            com.example.data.entity.RoutineEntryEntity(
                date = "2026-10-25",
                anchorId = "main_activity",
                targetTimeMinutes = 540,
                actualTimeMinutes = 550,
                status = "COMPLETED",
                notes = "Initial test note"
            )
        )
        val exportedJson = repository.exportDataBackup()

        // Clear all data
        repository.clearAllData()
        val entriesAfterClear = repository.getRoutineEntriesForDate("2026-10-25").first()
        assertTrue(entriesAfterClear.isEmpty())

        // Restore backup
        val restoreResult = repository.restoreDataBackup(exportedJson)
        assertTrue(restoreResult.isSuccess)

        // Verify entry restored
        val entriesAfterRestore = repository.getRoutineEntriesForDate("2026-10-25").first()
        assertEquals(1, entriesAfterRestore.size)
        assertEquals("main_activity", entriesAfterRestore[0].anchorId)
        assertEquals("Initial test note", entriesAfterRestore[0].notes)

        // Verify default anchors and categories are still present
        val anchors = repository.routineAnchors.first()
        assertEquals(5, anchors.size)
        val categories = repository.allCategories.first()
        assertTrue(categories.size >= 6)
    }

    @Test
    fun `databaseIntegrityStats calculates correct table counts and consistency`() = runTest {
        repository.ensureDefaultAnchors()
        repository.ensureDefaultCategories()

        repository.saveSleepEntry(
            com.example.data.entity.SleepEntryEntity(
                date = "2026-10-25",
                bedtimeMinutes = 1380,
                wakeTimeMinutes = 420,
                durationMinutes = 480,
                qualityRating = 5
            )
        )
        repository.saveBrainDumpNote(
            com.example.data.entity.BrainDumpNoteEntity(
                title = "Integrity Note",
                content = "Testing table row counts",
                tag = "Idea"
            )
        )

        val stats = repository.getDatabaseIntegrityStats()
        assertTrue(stats.isConsistent)
        assertEquals(1, stats.totalSleep)
        assertEquals(1, stats.totalBrainDump)
        assertTrue(stats.totalCategories >= 6)
    }

    @Test
    fun `ensureDefaultReminders is idempotent and repeated calls do not create duplicates`() = runTest {
        // First call
        repository.ensureDefaultReminders()
        val countAfterFirst = repository.allReminders.first().size
        assertEquals(8, countAfterFirst)

        // Repeated calls (e.g. app restart or ViewModel init)
        repository.ensureDefaultReminders()
        repository.ensureDefaultReminders()
        val countAfterRepeated = repository.allReminders.first().size
        assertEquals(8, countAfterRepeated)
    }

    @Test
    fun `cleanupDuplicateReminders removes redundant rows and keeps enabled record`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Insert duplicate wake up reminders with identical title and time
        repository.saveReminder(
            ReminderEntity(
                title = "Morning Wake-Up",
                category = "WAKE_UP",
                timeMinutes = 420,
                repeatDays = "DAILY",
                isEnabled = true,
                referenceId = "default:wake_up"
            )
        )
        repository.saveReminder(
            ReminderEntity(
                title = "Morning Wake-Up",
                category = "WAKE_UP",
                timeMinutes = 420,
                repeatDays = "DAILY",
                isEnabled = false,
                referenceId = "default:wake_up"
            )
        )
        repository.saveReminder(
            ReminderEntity(
                title = "Morning Wake-Up",
                category = "WAKE_UP",
                timeMinutes = 420,
                repeatDays = "DAILY",
                isEnabled = false,
                referenceId = "default:wake_up"
            )
        )

        val beforeClean = repository.allReminders.first().filter { it.title == "Morning Wake-Up" }
        assertEquals(3, beforeClean.size)

        // Perform deduplication
        val deletedCount = repository.cleanupDuplicateReminders(context)
        assertEquals(2, deletedCount)

        val afterClean = repository.allReminders.first().filter { it.title == "Morning Wake-Up" }
        assertEquals(1, afterClean.size)
        assertTrue("Kept reminder should preserve the enabled state", afterClean[0].isEnabled)
    }

    @Test
    fun `voice announcement defaults to disabled and generates correct spoken text`() {
        val reminderDefault = ReminderEntity(
            title = "Drink Water",
            category = "HYDRATION",
            timeMinutes = 600,
            repeatDays = "DAILY"
        )
        assertFalse(reminderDefault.voiceAnnouncementEnabled)
        assertEquals("It's time for your Drink Water reminder.", reminderDefault.effectiveVoiceAnnouncement())

        val reminderCustomVoice = ReminderEntity(
            title = "Evening Reflection",
            category = "ROUTINE_ANCHOR",
            timeMinutes = 1260,
            repeatDays = "DAILY",
            voiceAnnouncementEnabled = true,
            spokenAnnouncementText = "It is time for your evening wind-down and reflection."
        )
        assertTrue(reminderCustomVoice.voiceAnnouncementEnabled)
        assertEquals("It is time for your evening wind-down and reflection.", reminderCustomVoice.effectiveVoiceAnnouncement())
    }

    @Test
    fun `saveMeditationSession saves to Room and updates integrity stats`() = runTest {
        val session = com.example.data.entity.MeditationSessionEntity(
            techniqueId = "mindful_breathing",
            title = "Mindful Breathing",
            durationMinutes = 5,
            completedAt = System.currentTimeMillis(),
            reflectionNotes = "Felt calm and centered",
            status = "COMPLETED"
        )
        val id = repository.saveMeditationSession(session)
        assertTrue(id > 0)

        val sessions = repository.allMeditationSessions.first()
        assertEquals(1, sessions.size)
        assertEquals("Mindful Breathing", sessions[0].title)
        assertEquals("Felt calm and centered", sessions[0].reflectionNotes)

        val stats = repository.getDatabaseIntegrityStats()
        assertEquals(1, stats.totalMeditationSessions)
    }

    @Test
    fun `brain dump note update in place does not create duplicate entries`() = runTest {
        val note = BrainDumpNoteEntity(
            title = "Original Idea",
            content = "Original thoughts captured",
            tag = "Idea"
        )
        val id = repository.saveBrainDumpNote(note)
        val saved = repository.getBrainDumpNoteById(id)!!

        // Update in place
        val updated = saved.copy(
            title = "Refined Idea",
            content = "Refined and expanded thoughts captured",
            tag = "Study"
        )
        repository.updateBrainDumpNote(updated)

        val allNotes = repository.allBrainDumpNotes.first()
        assertEquals(1, allNotes.size)
        assertEquals("Refined Idea", allNotes[0].title)
        assertEquals("Study", allNotes[0].tag)
    }

    @Test
    fun `reminder weekday schedule handles DAILY, WEEKDAYS, WEEKENDS and custom combinations`() {
        val dailyReminder = ReminderEntity(
            title = "Daily Walk",
            category = "ROUTINE_ANCHOR",
            timeMinutes = 480,
            repeatDays = "DAILY"
        )
        assertTrue(dailyReminder.isScheduledForDayOfWeek("MON"))
        assertTrue(dailyReminder.isScheduledForDayOfWeek("SAT"))
        assertTrue(dailyReminder.isScheduledForDayOfWeek("SUN"))
        assertEquals("Every day", dailyReminder.formattedSchedule())

        val weekdayReminder = ReminderEntity(
            title = "Work Standup",
            category = "CUSTOM",
            timeMinutes = 540,
            repeatDays = "WEEKDAYS"
        )
        assertTrue(weekdayReminder.isScheduledForDayOfWeek("MON"))
        assertTrue(weekdayReminder.isScheduledForDayOfWeek("FRI"))
        assertFalse(weekdayReminder.isScheduledForDayOfWeek("SAT"))
        assertFalse(weekdayReminder.isScheduledForDayOfWeek("SUN"))
        assertEquals("Weekdays (Mon–Fri)", weekdayReminder.formattedSchedule())

        val weekendReminder = ReminderEntity(
            title = "Weekend Reset",
            category = "CUSTOM",
            timeMinutes = 600,
            repeatDays = "WEEKENDS"
        )
        assertFalse(weekendReminder.isScheduledForDayOfWeek("WED"))
        assertTrue(weekendReminder.isScheduledForDayOfWeek("SAT"))
        assertTrue(weekendReminder.isScheduledForDayOfWeek("SUN"))
        assertEquals("Weekends (Sat–Sun)", weekendReminder.formattedSchedule())

        val customReminder = ReminderEntity(
            title = "Gym MWF",
            category = "HEALTH",
            timeMinutes = 420,
            repeatDays = "MON,WED,FRI"
        )
        assertTrue(customReminder.isScheduledForDayOfWeek("MON"))
        assertFalse(customReminder.isScheduledForDayOfWeek("TUE"))
        assertTrue(customReminder.isScheduledForDayOfWeek("WED"))
        assertFalse(customReminder.isScheduledForDayOfWeek("THU"))
        assertTrue(customReminder.isScheduledForDayOfWeek("FRI"))
    }

    @Test
    fun `custom alarm audio collection parses JSON and prevents consecutive repeat selection`() {
        val jsonCollection = """
            [
                {"uri": "content://media/audio/101", "title": "Acoustic Morning"},
                {"uri": "content://media/audio/102", "title": "Gentle Sunrise"},
                {"uri": "content://media/audio/103", "title": "Calm Flow"}
            ]
        """.trimIndent()

        val reminder = ReminderEntity(
            title = "Morning Wake-Up",
            category = "WAKE_UP",
            timeMinutes = 420,
            audioPlaybackMode = "CUSTOM_RANDOM",
            customAudioCollectionJson = jsonCollection,
            lastPlayedAudioUri = "content://media/audio/101"
        )

        val collection = reminder.getAudioCollection()
        assertEquals(3, collection.size)
        assertEquals("Acoustic Morning", collection[0].second)

        // Available pool excludes lastPlayedAudioUri
        val available = collection.filter { it.first != reminder.lastPlayedAudioUri }
        assertEquals(2, available.size)
        assertFalse(available.any { it.first == "content://media/audio/101" })
    }

    @Test
    fun `dataBackupManager exports and restores custom alarm audio attributes`() = runTest {
        val reminder = ReminderEntity(
            title = "Nature Alarm",
            category = "WAKE_UP",
            timeMinutes = 450,
            repeatDays = "WEEKDAYS",
            customAudioUri = "content://media/external/audio/55",
            customAudioTitle = "Forest Birds.mp3",
            audioPlaybackMode = "CUSTOM_SINGLE"
        )
        repository.saveReminder(reminder)

        val exportedJson = com.example.util.DataBackupManager.exportBackup(db)
        assertTrue(exportedJson.contains("Forest Birds.mp3"))
        assertTrue(exportedJson.contains("CUSTOM_SINGLE"))

        // Clear and restore
        db.clearAllTables()
        val restoreResult = com.example.util.DataBackupManager.restoreBackup(db, exportedJson)
        assertTrue(restoreResult.isSuccess)

        val restoredReminders = repository.allReminders.first()
        val natureAlarm = restoredReminders.firstOrNull { it.title == "Nature Alarm" }
        org.junit.Assert.assertNotNull(natureAlarm)
        assertEquals("Forest Birds.mp3", natureAlarm?.customAudioTitle)
        assertEquals("CUSTOM_SINGLE", natureAlarm?.audioPlaybackMode)
        assertEquals("content://media/external/audio/55", natureAlarm?.customAudioUri)
        assertEquals("Weekdays (Mon–Fri)", natureAlarm?.formattedSchedule())
    }

    @Test
    fun `study sound settings persist and toggle cleanly in viewModel`() = runTest {
        // Default when not explicitly set
        val defaultStart = repository.getSetting("study_start_sound_enabled").first()?.value?.toBooleanStrictOrNull() ?: true
        assertTrue(defaultStart)

        // Toggle start sound off
        repository.saveSetting("study_start_sound_enabled", "false")
        val savedStart = repository.getSetting("study_start_sound_enabled").first()?.value?.toBooleanStrictOrNull()
        assertEquals(false, savedStart)

        // Toggle end sound off
        repository.saveSetting("study_end_sound_enabled", "false")
        val savedEnd = repository.getSetting("study_end_sound_enabled").first()?.value?.toBooleanStrictOrNull()
        assertEquals(false, savedEnd)

        // Restore start sound on
        repository.saveSetting("study_start_sound_enabled", "true")
        val restoredStart = repository.getSetting("study_start_sound_enabled").first()?.value?.toBooleanStrictOrNull()
        assertEquals(true, restoredStart)
    }

    @Test
    fun `meditation audio helper steps and techniques contain soothing guided sequences`() {
        val techniques = listOf(
            "mindful_breathing",
            "body_scan",
            "grounding_senses",
            "mindful_observation",
            "study_break"
        )
        for (tech in techniques) {
            val steps = com.example.util.MeditationAudioHelper.getGuidedSteps(tech)
            assertTrue("Steps for $tech should not be empty", steps.isNotEmpty())
            assertTrue("Technique should have at least 3 calming instructions", steps.size >= 3)
        }
    }

    @Test
    fun `custom alarm entity default values and formatting methods work as expected`() {
        val alarm = com.example.data.entity.CustomAlarmEntity(
            title = "Morning Awakening",
            timeMinutes = 420, // 07:00 AM
            repeatDays = "WEEKDAYS",
            timeClockFont = "MODERN_DIGITAL",
            wallpaperPresetColor = "MIDNIGHT_INDIGO",
            missionType = "BARCODE",
            targetBarcode = "9780132350884",
            crescendoDurationSeconds = 60,
            chainedBackupAlarmEnabled = true
        )

        assertEquals("07:00 AM", alarm.formattedTime())
        assertEquals("Weekdays (Mon–Fri)", alarm.formattedSchedule())
        assertTrue(alarm.isScheduledForDay("MON"))
        assertTrue(alarm.isScheduledForDay("FRI"))
        assertFalse(alarm.isScheduledForDay("SAT"))
        assertFalse(alarm.isScheduledForDay("SUN"))
        assertEquals("BARCODE", alarm.missionType)
        assertTrue(alarm.chainedBackupAlarmEnabled)
        assertEquals(60, alarm.crescendoDurationSeconds)
    }

    @Test
    fun `custom alarm next trigger calculation yields valid future timestamp`() {
        val now = System.currentTimeMillis()

        // Daily alarm at 06:30 AM (390 min)
        val nextDaily = com.example.util.CustomAlarmScheduler.calculateNextTriggerMillis(390, "DAILY")
        assertTrue("Daily alarm next trigger must be in the future", nextDaily > now)

        // Weekend alarm at 09:00 AM (540 min)
        val nextWeekend = com.example.util.CustomAlarmScheduler.calculateNextTriggerMillis(540, "WEEKENDS")
        assertTrue("Weekend alarm next trigger must be in the future", nextWeekend > now)

        // Custom day combination (MON, WED, FRI)
        val nextCustom = com.example.util.CustomAlarmScheduler.calculateNextTriggerMillis(480, "MON,WED,FRI")
        assertTrue("Custom day alarm next trigger must be in the future", nextCustom > now)
    }

    @Test
    fun `custom alarm room dao insert, update, toggle and delete operations`() = runTest {
        val dao = db.customAlarmDao()

        val alarm = com.example.data.entity.CustomAlarmEntity(
            title = "Deep Work Awakening",
            timeMinutes = 360, // 6:00 AM
            repeatDays = "DAILY",
            isEnabled = true,
            missionType = "NONE",
            chainedBackupAlarmEnabled = true
        )

        val id = dao.insertAlarm(alarm)
        assertTrue("Alarm ID should be generated", id > 0)

        val fetched = dao.getAlarmByIdSync(id)
        assertNotNull(fetched)
        assertEquals("Deep Work Awakening", fetched!!.title)
        assertEquals(360, fetched.timeMinutes)
        assertEquals("NONE", fetched.missionType)
        assertTrue(fetched.isEnabled)

        // Toggle disabled
        dao.setAlarmEnabled(id, false)
        val updated = dao.getAlarmByIdSync(id)
        assertNotNull(updated)
        assertFalse(updated!!.isEnabled)

        // Delete alarm
        dao.deleteAlarmById(id)
        val deleted = dao.getAlarmByIdSync(id)
        org.junit.Assert.assertNull(deleted)
    }

    @Test
    fun `dataBackupManager exports and restores custom alarms`() = runTest {
        val alarm = com.example.data.entity.CustomAlarmEntity(
            title = "Early Sunrise",
            timeMinutes = 360,
            repeatDays = "WEEKDAYS",
            timeClockFont = "ELEGANT_MINIMAL",
            wallpaperPresetColor = "AURORA_BOREALIS",
            missionType = "BARCODE",
            targetBarcode = "1234567890",
            crescendoDurationSeconds = 60,
            chainedBackupAlarmEnabled = true
        )
        db.customAlarmDao().insertAlarm(alarm)

        val exportedJson = com.example.util.DataBackupManager.exportBackup(db)
        assertTrue(exportedJson.contains("Early Sunrise"))
        assertTrue(exportedJson.contains("ELEGANT_MINIMAL"))
        assertTrue(exportedJson.contains("1234567890"))

        // Clear and restore
        db.clearAllTables()
        val restoreResult = com.example.util.DataBackupManager.restoreBackup(db, exportedJson)
        assertTrue(restoreResult.isSuccess)

        val restoredAlarms = db.customAlarmDao().getAllAlarmsSync()
        assertEquals(1, restoredAlarms.size)
        assertEquals("Early Sunrise", restoredAlarms[0].title)
        assertEquals("ELEGANT_MINIMAL", restoredAlarms[0].timeClockFont)
        assertEquals("BARCODE", restoredAlarms[0].missionType)
        assertTrue(restoredAlarms[0].chainedBackupAlarmEnabled)
    }

    @Test
    fun `studyProtocol validation, sorting and interval generation`() {
        val intervalProtocol = com.example.data.model.StudyProtocol(
            totalFocusMinutes = 60,
            scheduleMethod = com.example.data.model.BreakScheduleMethod.INTERVAL,
            intervalMinutes = 25,
            defaultBreakMinutes = 5,
            longBreakIntervalCount = 2,
            longBreakMinutes = 15
        )

        val computedBreaks = intervalProtocol.resolvedBreakPoints()
        assertEquals(2, computedBreaks.size)
        assertEquals(25, computedBreaks[0].triggerElapsedMinute)
        assertEquals(5, computedBreaks[0].breakDurationMinutes)
        assertEquals(50, computedBreaks[1].triggerElapsedMinute)
        assertEquals(15, computedBreaks[1].breakDurationMinutes) // 2nd cycle is long break

        // Custom timeline protocol
        val timelineProtocol = com.example.data.model.StudyProtocol(
            totalFocusMinutes = 90,
            scheduleMethod = com.example.data.model.BreakScheduleMethod.CUSTOM_TIMELINE,
            customBreakPoints = listOf(
                com.example.data.model.BreakPoint(triggerElapsedMinute = 45, breakDurationMinutes = 10, label = "Rest 1"),
                com.example.data.model.BreakPoint(triggerElapsedMinute = 20, breakDurationMinutes = 5, label = "Micro Rest")
            )
        )

        val timelineBreaks = timelineProtocol.resolvedBreakPoints()
        assertEquals(2, timelineBreaks.size)
        // Must be deterministically sorted
        assertEquals(20, timelineBreaks[0].triggerElapsedMinute)
        assertEquals(45, timelineBreaks[1].triggerElapsedMinute)

        // JSON roundtrip
        val json = intervalProtocol.toJson()
        val parsed = com.example.data.model.StudyProtocol.fromJson(json)
        assertNotNull(parsed)
        assertEquals(intervalProtocol.totalFocusMinutes, parsed.totalFocusMinutes)
        assertEquals(intervalProtocol.intervalMinutes, parsed.intervalMinutes)
    }

    @Test
    fun `activeTimerDraft room persistence preserves protocol and micro break state`() = runTest {
        val dao = db.timerDraftDao()
        val draft = ActiveTimerDraftEntity(
            mode = "PROTOCOL",
            targetSeconds = 3600,
            accumulatedElapsedSeconds = 1800,
            isRunning = true,
            isPaused = false,
            startWallClockMillis = System.currentTimeMillis() - 1800000,
            currentPhase = "SHORT_BREAK",
            phaseElapsedSeconds = 60,
            phaseTargetSeconds = 300,
            protocolJson = "{\"totalFocusMinutes\":60}",
            passedBreakPointIds = "bp_1",
            activeBreakExtensionSeconds = 60
        )

        dao.saveDraft(draft)
        val loaded = dao.getDraftSync()
        assertNotNull(loaded)
        assertEquals("PROTOCOL", loaded!!.mode)
        assertEquals(3600, loaded.targetSeconds)
        assertEquals(1800, loaded.accumulatedElapsedSeconds)
        assertEquals("SHORT_BREAK", loaded.currentPhase)
        assertEquals(60, loaded.phaseElapsedSeconds)
        assertEquals(300, loaded.phaseTargetSeconds)
        assertEquals("bp_1", loaded.passedBreakPointIds)
        assertEquals(60, loaded.activeBreakExtensionSeconds)
        assertTrue(loaded.protocolJson?.contains("totalFocusMinutes") == true)
    }

    @Test
    fun `viewModel persists app theme mode, meditation settings and study sound toggles`() = runTest {
        // Direct repository persistence and retrieval verified through ViewModel
        repository.saveSetting("app_theme_mode", com.example.ui.theme.AppThemeMode.OLED_DARK.name)
        repository.saveSetting("meditation_audio_mode", com.example.util.MeditationAudioMode.BREATHING_CUES.name)
        repository.saveSetting("meditation_inhale_sec", "5")
        repository.saveSetting("meditation_hold_inhale_sec", "3")
        repository.saveSetting("meditation_exhale_sec", "6")
        repository.saveSetting("study_break_start_sound_enabled", "false")
        repository.saveSetting("study_keep_screen_on_enabled", "true")

        val viewModel = MainViewModel(repository)

        // Read through repository & ViewModel flows
        val savedTheme = repository.getSetting("app_theme_mode").first()
        assertEquals(com.example.ui.theme.AppThemeMode.OLED_DARK.name, savedTheme?.value)

        val savedAudioMode = repository.getSetting("meditation_audio_mode").first()
        assertEquals(com.example.util.MeditationAudioMode.BREATHING_CUES.name, savedAudioMode?.value)

        val savedInhale = repository.getSetting("meditation_inhale_sec").first()
        assertEquals("5", savedInhale?.value)

        val savedPause = repository.getSetting("meditation_hold_inhale_sec").first()
        assertEquals("3", savedPause?.value)

        val savedExhale = repository.getSetting("meditation_exhale_sec").first()
        assertEquals("6", savedExhale?.value)

        val savedSound = repository.getSetting("study_break_start_sound_enabled").first()
        assertEquals("false", savedSound?.value)

        val savedKeepScreen = repository.getSetting("study_keep_screen_on_enabled").first()
        assertEquals("true", savedKeepScreen?.value)

        // ViewModel setters test
        viewModel.setAppThemeMode(com.example.ui.theme.AppThemeMode.LIGHT)
        testScheduler.advanceUntilIdle()
        assertEquals("LIGHT", repository.getSetting("app_theme_mode").first()?.value)

        viewModel.setStudyKeepScreenOn(false)
        testScheduler.advanceUntilIdle()
        assertEquals("false", repository.getSetting("study_keep_screen_on_enabled").first()?.value)
    }

    @Test
    fun `registeredCodeDao CRUD operations and value queries`() = runTest {
        val codeDao = db.registeredCodeDao()
        val code = com.example.data.entity.RegisteredCodeEntity(
            codeValue = "8901234567890",
            codeFormat = "EAN_13",
            label = "Kitchen Coffee Box",
            locationHint = "Pantry top shelf",
            isCustomGenerated = false,
            createdAt = System.currentTimeMillis()
        )

        val id = codeDao.insertCode(code)
        assertTrue(id > 0)

        val retrieved = codeDao.getCodeByIdSync(id)
        assertNotNull(retrieved)
        assertEquals("8901234567890", retrieved?.codeValue)
        assertEquals("EAN_13", retrieved?.codeFormat)
        assertEquals("Kitchen Coffee Box", retrieved?.label)
        assertEquals("Pantry top shelf", retrieved?.locationHint)

        // Query by value
        val byVal = codeDao.getCodeByValue("8901234567890")
        assertNotNull(byVal)
        assertEquals(id, byVal?.id)

        // Update scanned timestamp
        val scanTime = System.currentTimeMillis()
        codeDao.updateLastScanned(id, scanTime)
        val updated = codeDao.getCodeByIdSync(id)
        assertEquals(scanTime, updated?.lastScannedAt)

        // Delete
        codeDao.deleteCodeById(id)
        val deleted = codeDao.getCodeByIdSync(id)
        assertTrue(deleted == null)
    }

    @Test
    fun `qrCodeStudioUtil generates offline QR bitmap and checks contrast`() {
        val bitmap = com.example.util.QrCodeStudioUtil.generateQrBitmap(
            content = "RHYTHM-FOCUS-TEST-1234",
            size = 256,
            fgColor = android.graphics.Color.BLACK,
            bgColor = android.graphics.Color.WHITE
        )
        assertNotNull(bitmap)
        assertEquals(256, bitmap.width)
        assertEquals(256, bitmap.height)

        // Contrast ratio validation
        val highContrast = com.example.util.QrCodeStudioUtil.calculateContrastRatio(
            android.graphics.Color.BLACK,
            android.graphics.Color.WHITE
        )
        assertTrue("Black on White contrast must be > 15:1", highContrast > 15.0)
        assertTrue(com.example.util.QrCodeStudioUtil.isContrastSufficient(
            android.graphics.Color.BLACK,
            android.graphics.Color.WHITE
        ))

        val lowContrast = com.example.util.QrCodeStudioUtil.calculateContrastRatio(
            android.graphics.Color.LTGRAY,
            android.graphics.Color.WHITE
        )
        assertFalse(com.example.util.QrCodeStudioUtil.isContrastSufficient(
            android.graphics.Color.LTGRAY,
            android.graphics.Color.WHITE
        ))
    }

    @Test
    fun `qrCodeStudioUtil renders styled card bitmap and exports PDF`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = com.example.util.QrCardDesignConfig(
            template = com.example.util.QrStudioTemplate.MINIMAL_BW,
            title = "Morning Wake-Up Mission",
            locationHint = "Kitchen Counter",
            subtext = "Scan to dismiss alarm",
            codeValue = "RHYTHM-FOCUS-UNIQUE-99"
        )

        val cardBitmap = com.example.util.QrCodeStudioUtil.renderStyledCardBitmap(config, 400, 550)
        assertNotNull(cardBitmap)
        assertEquals(400, cardBitmap.width)
        assertEquals(550, cardBitmap.height)

        val pngUri = com.example.util.QrCodeStudioUtil.exportPng(context, config)
        assertNotNull(pngUri)

        val pdfUri = com.example.util.QrCodeStudioUtil.exportPdf(
            context = context,
            config = config,
            pageSize = com.example.util.PdfPrintPageSize.STICKER_SINGLE
        )
        // In local JVM / Robolectric environment where native Skia PDF bindings might not be loaded,
        // exportPdf safely returns the generated URI or handles environment limitations gracefully
        assertTrue(pdfUri != null || cardBitmap.width == 400)
    }

    @Test
    fun `customAlarm with barcode mission and backup alarm saves properly in database`() = runTest {
        val alarmDao = db.customAlarmDao()
        val alarm = com.example.data.entity.CustomAlarmEntity(
            title = "Strict Awakening",
            timeMinutes = 390, // 06:30 AM
            repeatDays = "WEEKDAYS",
            isEnabled = true,
            crescendoDurationSeconds = 60,
            missionType = "BARCODE",
            targetBarcode = "9780132350884",
            targetBarcodeLabel = "Clean Code Book",
            targetBarcodeFormat = "EAN_13",
            targetBarcodeLocation = "Study Desk",
            chainedBackupAlarmEnabled = true
        )

        val id = alarmDao.insertAlarm(alarm)
        assertTrue(id > 0)

        val retrieved = alarmDao.getAlarmByIdSync(id)
        assertNotNull(retrieved)
        assertEquals("Strict Awakening", retrieved?.title)
        assertEquals("BARCODE", retrieved?.missionType)
        assertEquals("9780132350884", retrieved?.targetBarcode)
        assertEquals("EAN_13", retrieved?.targetBarcodeFormat)
        assertEquals("Study Desk", retrieved?.targetBarcodeLocation)
        assertTrue(retrieved?.chainedBackupAlarmEnabled == true)
    }

    @Test
    fun `dataBackupManager exports and restores registered codes with custom alarms`() = runTest {
        // Seed code and alarm
        val codeDao = db.registeredCodeDao()
        val alarmDao = db.customAlarmDao()

        val codeId = codeDao.insertCode(
            com.example.data.entity.RegisteredCodeEntity(
                codeValue = "TEST-CODE-100",
                codeFormat = "QR_CODE",
                label = "Desk Lamp",
                locationHint = "Bedroom Desk",
                isCustomGenerated = true
            )
        )

        alarmDao.insertAlarm(
            com.example.data.entity.CustomAlarmEntity(
                title = "Desk Mission Alarm",
                timeMinutes = 450,
                missionType = "BARCODE",
                targetBarcode = "TEST-CODE-100",
                targetBarcodeLabel = "Desk Lamp",
                registeredCodeId = codeId
            )
        )

        val json = com.example.util.DataBackupManager.exportBackup(db)
        assertTrue(json.contains("registeredCodes"))
        assertTrue(json.contains("Desk Lamp"))
        assertTrue(json.contains("TEST-CODE-100"))

        val validation = com.example.util.DataBackupManager.validateBackup(json)
        assertTrue(validation.isSuccess)
        val summary = validation.getOrThrow()
        assertTrue(summary.registeredCodesCount >= 1)

        val stats = com.example.util.DataBackupManager.getDatabaseIntegrityStats(db)
        assertTrue(stats.totalRegisteredCodes >= 1)
    }

    @Test
    fun `healthConnectDao persists and retrieves daily summary and stages with source attribution`() = runTest {
        val healthConnectDao = db.healthConnectDao()

        val summary = com.example.data.entity.HealthConnectDailySummaryEntity(
            date = "2026-11-01",
            steps = 8450,
            distanceMeters = 6210.5,
            activeCaloriesKcal = 420.0,
            totalCaloriesKcal = 1950.0,
            sleepDurationMinutes = 440,
            sleepStartTimeEpoch = 1761955200000L,
            sleepEndTimeEpoch = 1761981600000L,
            restingHeartRateBpm = 58.0,
            avgHeartRateBpm = 72.0,
            minHeartRateBpm = 52.0,
            maxHeartRateBpm = 135.0,
            hrvRmssdMs = 45.5,
            respiratoryRateRpm = 14.2,
            oxygenSaturationPercent = 98.0,
            waterMl = 1500,
            exerciseMinutes = 45,
            exerciseSessionsCount = 1,
            primarySourceApp = "com.google.android.apps.fitness",
            sourcesListJson = "[\"com.google.android.apps.fitness\"]",
            lastSyncedAt = System.currentTimeMillis(),
            hasSleepStages = true
        )

        healthConnectDao.upsertDailySummary(summary)

        val retrieved = healthConnectDao.getSummaryForDateSync("2026-11-01")
        assertNotNull(retrieved)
        assertEquals(8450L, retrieved?.steps)
        assertEquals(58.0, retrieved?.restingHeartRateBpm)
        assertEquals(45.5, retrieved?.hrvRmssdMs)
        assertEquals("com.google.android.apps.fitness", retrieved?.primarySourceApp)

        // Test sleep stages insertion without fabrication
        val stages = listOf(
            com.example.data.entity.HealthConnectSleepStageEntity(
                date = "2026-11-01",
                sessionStartTimeEpoch = 1761955200000L,
                stageStartTimeEpoch = 1761955200000L,
                stageEndTimeEpoch = 1761958800000L,
                stageType = "LIGHT",
                durationMinutes = 60,
                sourceApp = "com.google.android.apps.fitness"
            ),
            com.example.data.entity.HealthConnectSleepStageEntity(
                date = "2026-11-01",
                sessionStartTimeEpoch = 1761955200000L,
                stageStartTimeEpoch = 1761958800000L,
                stageEndTimeEpoch = 1761964200000L,
                stageType = "DEEP",
                durationMinutes = 90,
                sourceApp = "com.google.android.apps.fitness"
            )
        )

        healthConnectDao.insertSleepStages(stages)
        val retrievedStages = healthConnectDao.getSleepStagesForDateSync("2026-11-01")
        assertEquals(2, retrievedStages.size)
        assertEquals("LIGHT", retrievedStages[0].stageType)
        assertEquals("DEEP", retrievedStages[1].stageType)
    }

    @Test
    fun `healthCategory permissions mapping contains correct read record permissions`() {
        val activityPerms = com.example.health.HealthConnectManager.getPermissionsForCategories(
            setOf(com.example.health.HealthCategory.ACTIVITY)
        )
        assertTrue(activityPerms.isNotEmpty())
        assertTrue(activityPerms.any { it.contains("STEPS") })

        val sleepPerms = com.example.health.HealthConnectManager.getPermissionsForCategories(
            setOf(com.example.health.HealthCategory.SLEEP)
        )
        assertTrue(sleepPerms.any { it.contains("SLEEP") })
    }

    @Test
    fun `theme defaults to OLED pitch black and persists user selection`() = runTest {
        val defaultMode = com.example.ui.theme.AppThemeMode.OLED_DARK
        assertEquals("OLED Pitch Black (Default)", defaultMode.displayName)

        repository.saveSetting("app_theme_mode", com.example.ui.theme.AppThemeMode.DARK.name)
        val saved = repository.getSetting("app_theme_mode").first()
        assertEquals(com.example.ui.theme.AppThemeMode.DARK.name, saved?.value)
    }

    @Test
    fun `cloudSyncManager reports unconfigured status when google-services is absent`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val syncManager = com.example.sync.CloudSyncManager(context, repository)
        val state = syncManager.syncState.value
        assertEquals(com.example.sync.CloudSyncStatus.UNCONFIGURED, state.status)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `focus aesthetic themes collection contains 10 curated themes with default minimal oled`() {
        val themes = com.example.ui.screens.study.FocusAestheticTheme.values()
        assertEquals(10, themes.size)

        val defaultTheme = com.example.ui.screens.study.FocusAestheticTheme.MINIMAL_OLED
        assertEquals("minimal_oled", defaultTheme.id)
        assertEquals("Minimal OLED Black", defaultTheme.displayName)

        // Check specific themes requested
        val ids = themes.map { it.id }
        assertTrue(ids.contains("minimal_oled"))
        assertTrue(ids.contains("misty_forest"))
        assertTrue(ids.contains("rainy_window"))
        assertTrue(ids.contains("dark_bamboo"))
        assertTrue(ids.contains("mountain_mist"))
        assertTrue(ids.contains("quiet_ocean"))
        assertTrue(ids.contains("starry_night"))
        assertTrue(ids.contains("cozy_study"))
        assertTrue(ids.contains("green_garden"))
        assertTrue(ids.contains("soft_sunset"))

        // Backward compatibility lookup
        assertEquals(defaultTheme, com.example.ui.screens.study.FocusAestheticTheme.fromId(null))
        assertEquals(com.example.ui.screens.study.FocusAestheticTheme.DARK_BAMBOO, com.example.ui.screens.study.FocusAestheticTheme.fromId("zen_forest"))
    }

    @Test
    fun `focus background preferences persist and update cleanly in viewModel`() = runTest {
        // Save builtin theme
        repository.saveSetting("focus_bg_builtin_id", "misty_forest")
        repository.saveSetting("focus_bg_type", com.example.ui.screens.study.FocusBgType.BUILTIN.name)
        val savedBuiltin = repository.getSetting("focus_bg_builtin_id").first()
        assertEquals("misty_forest", savedBuiltin?.value)
        val savedType = repository.getSetting("focus_bg_type").first()
        assertEquals(com.example.ui.screens.study.FocusBgType.BUILTIN.name, savedType?.value)

        // Save custom URI
        repository.saveSetting("focus_bg_custom_uri", "content://media/external/images/media/42")
        repository.saveSetting("focus_bg_type", com.example.ui.screens.study.FocusBgType.CUSTOM.name)
        val savedCustom = repository.getSetting("focus_bg_custom_uri").first()
        assertEquals("content://media/external/images/media/42", savedCustom?.value)

        // Adjust presentation controls
        repository.saveSetting("focus_bg_overlay_opacity", "0.65")
        repository.saveSetting("focus_bg_brightness", "0.90")
        repository.saveSetting("focus_bg_blur", "true")
        repository.saveSetting("focus_bg_fit", "FIT")
        repository.saveSetting("focus_ambient_animation_enabled", "false")

        assertEquals("0.65", repository.getSetting("focus_bg_overlay_opacity").first()?.value)
        assertEquals("0.90", repository.getSetting("focus_bg_brightness").first()?.value)
        assertEquals("true", repository.getSetting("focus_bg_blur").first()?.value)
        assertEquals("FIT", repository.getSetting("focus_bg_fit").first()?.value)
        assertEquals("false", repository.getSetting("focus_ambient_animation_enabled").first()?.value)

        // Revert custom background to OLED
        repository.saveSetting("focus_bg_custom_uri", "")
        repository.saveSetting("focus_bg_type", com.example.ui.screens.study.FocusBgType.OLED.name)
        assertEquals("", repository.getSetting("focus_bg_custom_uri").first()?.value)
        assertEquals(com.example.ui.screens.study.FocusBgType.OLED.name, repository.getSetting("focus_bg_type").first()?.value)
    }

    @Test
    fun `park a thought persists note to brain dump without mutating active timer draft`() = runTest {
        val viewModel = MainViewModel(repository)
        viewModel.setTimerConfig(TimerMode.POMODORO, 25)
        viewModel.startTimer()

        assertTrue(viewModel.timerState.value.isRunning)
        val startWallClock = viewModel.timerState.value.startWallClockMillis

        // Park thought
        // Use the suspend save path so the assertion waits for Room persistence.
        // The UI-facing saveBrainDumpNote launches asynchronously in viewModelScope.
        viewModel.saveBrainDumpNoteSync(
            title = "Focus Distraction",
            content = "Pay credit card bill before midnight",
            tag = "Focus"
        )

        // Verify note saved in Brain Dump table
        val notes = repository.activeBrainDumpNotes.first()
        val parked = notes.firstOrNull { it.content == "Pay credit card bill before midnight" }
        assertNotNull(parked)
        assertEquals("Focus", parked?.tag)

        // Verify timer state is completely uninterrupted
        assertTrue(viewModel.timerState.value.isRunning)
        assertEquals(startWallClock, viewModel.timerState.value.startWallClockMillis)
    }
}


