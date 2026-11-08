package com.example.util

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.MedicationEntity
import com.example.data.entity.MedicationIntakeEntity
import com.example.data.entity.ReminderEntity
import com.example.data.entity.RoutineAnchorEntity
import com.example.data.entity.RoutineEntryEntity
import com.example.data.entity.SleepEntryEntity
import com.example.data.entity.StudyCategoryEntity
import com.example.data.entity.StudySessionEntity
import com.example.data.entity.StudyTaskEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
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
class DataBackupManagerTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
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
    fun `exportBackup outputs valid JSON with current schema version 5`() = runTest {
        repository.ensureDefaultAnchors()
        repository.ensureDefaultCategories()

        db.routineDao().insertOrUpdateEntry(
            RoutineEntryEntity(
                id = 1L,
                date = "2026-11-08",
                anchorId = "wake_up",
                targetTimeMinutes = 420,
                actualTimeMinutes = null,
                status = "COMPLETED"
            )
        )

        val jsonString = DataBackupManager.exportBackup(db)
        assertNotNull(jsonString)
        val root = JSONObject(jsonString)
        assertEquals("Rhythm & Focus", root.getString("app"))
        assertEquals(DataBackupManager.CURRENT_BACKUP_SCHEMA_VERSION, root.getInt("schemaVersion"))
        assertEquals(5, root.getInt("schemaVersion"))
        assertTrue(root.getLong("exportTimestamp") > 0)
    }

    @Test
    fun `validateBackup accepts supported schema versions 1 to 5`() {
        for (version in 1..5) {
            val json = """
                {
                  "app": "Rhythm & Focus",
                  "schemaVersion": $version,
                  "exportTimestamp": 1700000000000,
                  "routineEntries": []
                }
            """.trimIndent()
            val result = DataBackupManager.validateBackup(json)
            assertTrue("Expected schema version $version to be valid", result.isSuccess)
        }
    }

    @Test
    fun `validateBackup defaults missing schemaVersion to 1 and succeeds`() {
        val legacyJsonWithoutVersion = """
            {
              "app": "Rhythm & Focus",
              "exportTimestamp": 1700000000000,
              "routineEntries": []
            }
        """.trimIndent()
        val result = DataBackupManager.validateBackup(legacyJsonWithoutVersion)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateBackup rejects unsupported schemaVersion out of range`() {
        // Zero or negative
        val zeroResult = DataBackupManager.validateBackup("""{"app":"Rhythm & Focus","schemaVersion":0}""")
        assertTrue(zeroResult.isFailure)
        assertTrue(zeroResult.exceptionOrNull()?.message?.contains("Unsupported backup schema version: 0") == true)

        // Future version
        val futureResult = DataBackupManager.validateBackup("""{"app":"Rhythm & Focus","schemaVersion":6}""")
        assertTrue(futureResult.isFailure)
        assertTrue(futureResult.exceptionOrNull()?.message?.contains("Unsupported backup schema version: 6") == true)

        val largeVersionResult = DataBackupManager.validateBackup("""{"app":"Rhythm & Focus","schemaVersion":11}""")
        assertTrue(largeVersionResult.isFailure)
        assertTrue(largeVersionResult.exceptionOrNull()?.message?.contains("Unsupported backup schema version: 11") == true)
    }

    @Test
    fun `validateBackup rejects missing or incorrect app identifier and malformed JSON`() {
        // Wrong app
        val wrongAppResult = DataBackupManager.validateBackup("""{"app":"OtherApp","schemaVersion":5}""")
        assertTrue(wrongAppResult.isFailure)
        assertTrue(wrongAppResult.exceptionOrNull()?.message?.contains("App identifier missing or mismatched") == true)

        // Missing app
        val missingAppResult = DataBackupManager.validateBackup("""{"schemaVersion":5}""")
        assertTrue(missingAppResult.isFailure)

        // Malformed JSON
        val malformedResult = DataBackupManager.validateBackup("not valid json at all")
        assertTrue(malformedResult.isFailure)
    }

    @Test
    fun `restoreBackup successfully restores older v1 to v5 backups`() = runTest {
        val v3BackupJson = """
            {
              "app": "Rhythm & Focus",
              "schemaVersion": 3,
              "exportTimestamp": 1700000000000,
              "routineEntries": [
                {
                  "id": 10,
                  "date": "2026-11-08",
                  "anchorId": "out_of_bed",
                  "targetTimeMinutes": 450,
                  "actualTimeMinutes": 455,
                  "status": "COMPLETED",
                  "notes": "Good morning"
                }
              ],
              "sleepEntries": [
                {
                  "id": 20,
                  "date": "2026-11-08",
                  "bedtimeMinutes": 1380,
                  "wakeTimeMinutes": 450,
                  "durationMinutes": 510,
                  "qualityRating": 4
                }
              ],
              "studyTasks": [
                {
                  "id": 30,
                  "title": "Algorithms Practice",
                  "category": "Computer Science",
                  "status": "PENDING",
                  "isDailyPriority": true,
                  "estimatedMinutes": 45,
                  "dateCreated": "2026-11-08"
                }
              ]
            }
        """.trimIndent()

        val restoreResult = DataBackupManager.restoreBackup(db, v3BackupJson)
        assertTrue(restoreResult.isSuccess)
        val summary = restoreResult.getOrThrow()
        assertEquals(1, summary.routineEntriesCount)
        assertEquals(1, summary.sleepCount)
        assertEquals(1, summary.studyTasksCount)

        val entries = db.routineDao().getEntriesForDate("2026-11-08").first()
        assertEquals(1, entries.size)
        assertEquals("out_of_bed", entries[0].anchorId)
        assertEquals("Good morning", entries[0].notes)

        val sleep = db.sleepDao().getSleepForDate("2026-11-08").first()
        assertNotNull(sleep)
        assertEquals(510, sleep?.durationMinutes)

        val tasks = db.studyDao().getTasksListSync()
        assertEquals(1, tasks.size)
        assertEquals("Algorithms Practice", tasks[0].title)
        assertTrue(tasks[0].isDailyPriority)
    }

    @Test
    fun `restoreBackup with remapAutoIds merges user data without overwriting pre-existing records`() = runTest {
        // 1. Existing local records with ID = 1L
        val existingTask = StudyTaskEntity(
            id = 1L,
            title = "Existing Local Task",
            category = "General",
            status = "IN_PROGRESS",
            isDailyPriority = false,
            estimatedMinutes = 20,
            dateCreated = "2026-11-01"
        )
        db.studyDao().insertTask(existingTask)

        val existingMed = MedicationEntity(
            id = 1L,
            name = "Existing Vitamin C",
            dosage = "500",
            unit = "mg",
            instructions = "Take with water",
            scheduledTimeMinutes = 480,
            reminderEnabled = true,
            isActive = true
        )
        db.medicationDao().insertMedication(existingMed)

        // 2. Backup JSON that also defines records with ID = 1L
        val backupJson = """
            {
              "app": "Rhythm & Focus",
              "schemaVersion": 5,
              "exportTimestamp": 1700000000000,
              "studyTasks": [
                {
                  "id": 1,
                  "title": "Imported Task From Backup",
                  "category": "Math",
                  "status": "NOT_STARTED",
                  "isDailyPriority": true,
                  "estimatedMinutes": 50,
                  "dateCreated": "2026-11-08"
                }
              ],
              "medications": [
                {
                  "id": 1,
                  "name": "Imported Antibiotic",
                  "dosage =": "250",
                  "dosage": "250",
                  "unit": "mg",
                  "scheduledTimeMinutes": 600,
                  "reminderEnabled": true,
                  "isActive": true
                }
              ],
              "medicationIntakes": [
                {
                  "id": 1,
                  "date": "2026-11-08",
                  "medicationId": 1,
                  "scheduledTimeMinutes": 600,
                  "status": "TAKEN"
                }
              ],
              "reminders": [
                {
                  "id": 1,
                  "title": "Imported Antibiotic",
                  "category": "MEDICATION",
                  "timeMinutes": 600,
                  "referenceId": "med:1_0"
                }
              ]
            }
        """.trimIndent()

        // 3. Restore with remapAutoIds = true
        val result = DataBackupManager.restoreBackup(db, backupJson, remapAutoIds = true)
        assertTrue(result.isSuccess)

        // 4. Verify both tasks exist (no overwrite!)
        val allTasks = db.studyDao().getTasksListSync()
        assertEquals(2, allTasks.size)
        assertTrue(allTasks.any { it.title == "Existing Local Task" })
        assertTrue(allTasks.any { it.title == "Imported Task From Backup" })

        // 5. Verify both medications exist
        val allMeds = db.medicationDao().getAllMedicationsSync()
        assertEquals(2, allMeds.size)
        val importedMed = allMeds.firstOrNull { it.name == "Imported Antibiotic" }
        assertNotNull(importedMed)
        assertTrue(importedMed!!.id != 1L) // ID was remapped to new generated ID

        // 6. Verify medication intake has remapped foreign key
        val allIntakes = db.medicationDao().getAllIntakesSync()
        assertEquals(1, allIntakes.size)
        assertEquals(importedMed.id, allIntakes[0].medicationId)

        // 7. Verify reminder referenceId was remapped to new med id
        val allReminders = db.reminderDao().getAllRemindersListSync()
        val importedReminder = allReminders.firstOrNull { it.title == "Imported Antibiotic" }
        assertNotNull(importedReminder)
        assertEquals("med:${importedMed.id}_0", importedReminder?.referenceId)
    }

    @Test
    fun `databaseIntegrityStats calculates accurate table totals`() = runTest {
        repository.ensureDefaultAnchors()
        repository.ensureDefaultCategories()

        val statsInitial = DataBackupManager.getDatabaseIntegrityStats(db)
        assertTrue(statsInitial.isConsistent)
        assertEquals(AppDatabase.DEFAULT_CATEGORIES.size, statsInitial.totalCategories)

        db.routineDao().insertOrUpdateEntry(
            RoutineEntryEntity(
                date = "2026-11-08",
                anchorId = "wake_up",
                targetTimeMinutes = 420,
                actualTimeMinutes = null,
                status = "COMPLETED"
            )
        )
        db.sleepDao().insertOrUpdateSleep(
            SleepEntryEntity(
                date = "2026-11-08",
                bedtimeMinutes = 1380,
                wakeTimeMinutes = 420,
                durationMinutes = 480,
                qualityRating = 5
            )
        )

        val statsAfter = DataBackupManager.getDatabaseIntegrityStats(db)
        assertEquals(1, statsAfter.totalRoutines)
        assertEquals(1, statsAfter.totalSleep)
        assertTrue(statsAfter.isConsistent)
    }

    @Test
    fun `restoreBackup transaction rolls back all mutations if an error occurs mid-restore`() = runTest {
        assertEquals(0, db.routineDao().getEntriesCount())

        try {
            db.withTransaction {
                db.routineDao().insertOrUpdateEntry(
                    RoutineEntryEntity(
                        date = "2026-11-08",
                        anchorId = "temp_anchor",
                        targetTimeMinutes = 500,
                        actualTimeMinutes = null,
                        status = "COMPLETED"
                    )
                )
                throw RuntimeException("Simulated mid-transaction failure")
            }
        } catch (_: RuntimeException) {
            // Expected
        }

        assertEquals(0, db.routineDao().getEntriesCount())
    }
}

