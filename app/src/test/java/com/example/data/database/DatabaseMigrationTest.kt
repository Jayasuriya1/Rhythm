package com.example.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.CustomAlarmEntity
import com.example.data.entity.HealthConnectDailySummaryEntity
import com.example.data.entity.RegisteredCodeEntity
import com.example.data.entity.ReminderEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseMigrationTest {

    private lateinit var context: Context
    private val testDbName = "migration_test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(testDbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(testDbName)
    }

    @Test
    fun `schema baseline 11 json exists and contains all required entities`() {
        // Schema is exported into app/schemas/com.example.data.database.AppDatabase/11.json
        val schemaFile = File("schemas/com.example.data.database.AppDatabase/11.json").takeIf { it.exists() }
            ?: File("app/schemas/com.example.data.database.AppDatabase/11.json")
        assertTrue("Baseline schema 11.json must exist in schemas directory", schemaFile.exists())

        val content = schemaFile.readText()
        val json = JSONObject(content)
        assertEquals(1, json.getInt("formatVersion"))

        val dbJson = json.getJSONObject("database")
        assertEquals(11, dbJson.getInt("version"))

        val entities = dbJson.getJSONArray("entities")
        val entityNames = mutableSetOf<String>()
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            entityNames.add(entity.getString("tableName"))
        }

        val expectedTables = listOf(
            "routine_anchors",
            "routine_entries",
            "sleep_entries",
            "health_checkins",
            "water_entries",
            "medications",
            "medication_intakes",
            "study_categories",
            "study_tasks",
            "study_sessions",
            "active_timer_draft",
            "brain_dump_notes",
            "app_settings",
            "reminders",
            "meditation_sessions",
            "custom_alarms",
            "registered_codes",
            "health_connect_daily_summaries",
            "health_connect_sleep_stages",
            "health_connect_heart_samples"
        )

        for (table in expectedTables) {
            assertTrue("11.json must contain entity table '$table'", entityNames.contains(table))
        }
    }

    @Test
    fun `sequential migration from Version 1 to Version 11 preserves data and satisfies schema integrity`() = runTest {
        // Step 1: Create a raw SQLite database at Version 1 with Version 1 schema and seed data
        createVersion1Database(context, testDbName)

        // Step 2: Open and migrate to Version 11 using Room with all sequential migrations
        val migratedDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            testDbName
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9,
            AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11
        ).build()

        try {
            // Step 3: Verify Version 1 seeded data was preserved across all 10 migrations
            val routineAnchors = migratedDb.routineDao().getAllAnchorsSync()
            assertEquals(1, routineAnchors.size)
            assertEquals("wake_up", routineAnchors[0].id)
            assertEquals("Morning Wake", routineAnchors[0].name)

            val studyTasks = migratedDb.studyDao().getTasksListSync()
            assertEquals(1, studyTasks.size)
            assertEquals("Initial Task", studyTasks[0].title)

            val medications = migratedDb.medicationDao().getAllMedicationsSync()
            assertEquals(1, medications.size)
            assertEquals("Aspirin", medications[0].name)
            assertEquals("DAILY", medications[0].repeatDays) // default populated by MIGRATION_3_4
            assertEquals(false, medications[0].isArchived)

            val settings = migratedDb.settingsDao().getAllSettingsSync()
            val themeSetting = settings.firstOrNull { it.key == "theme_mode" }
            assertNotNull(themeSetting)
            assertEquals("DARK", themeSetting!!.value)

            // Step 4: Verify read/write functionality on tables created in intermediate migrations
            // Table from v5 & v6 & v7: reminders
            val reminder = ReminderEntity(
                id = 101L,
                title = "Hydration Reminder",
                category = "Health",
                timeMinutes = 600,
                repeatDays = "DAILY",
                isEnabled = true
            )
            migratedDb.reminderDao().insertReminder(reminder)
            val allReminders = migratedDb.reminderDao().getAllRemindersListSync()
            assertTrue(allReminders.any { it.id == 101L })

            // Table from v8 & v10: custom_alarms
            val alarm = CustomAlarmEntity(
                id = 201L,
                title = "Lab Alarm",
                timeMinutes = 480,
                repeatDays = "WEEKDAYS",
                isEnabled = true
            )
            migratedDb.customAlarmDao().insertAlarm(alarm)
            val allAlarms = migratedDb.customAlarmDao().getAllAlarmsSync()
            assertTrue(allAlarms.any { it.id == 201L })

            // Table from v10: registered_codes
            val code = RegisteredCodeEntity(
                id = 301L,
                codeValue = "QR-12345",
                codeFormat = "QR_CODE",
                label = "Desk Barcode"
            )
            migratedDb.registeredCodeDao().insertCode(code)
            val allCodes = migratedDb.registeredCodeDao().getAllRegisteredCodesSync()
            assertTrue(allCodes.any { it.id == 301L })

            // Table from v11: health_connect_daily_summaries
            val summary = HealthConnectDailySummaryEntity(
                date = "2026-11-08",
                steps = 8500L,
                distanceMeters = 6200.0,
                activeCaloriesKcal = 340.0,
                totalCaloriesKcal = 1950.0,
                sleepDurationMinutes = 450,
                waterMl = 2000,
                exerciseMinutes = 45,
                exerciseSessionsCount = 1,
                sourcesListJson = "[]",
                lastSyncedAt = System.currentTimeMillis(),
                hasSleepStages = false
            )
            migratedDb.healthConnectDao().upsertDailySummary(summary)
            val fetchedSummary = migratedDb.healthConnectDao().getSummaryForDateSync("2026-11-08")
            assertNotNull(fetchedSummary)
            assertEquals(8500L, fetchedSummary!!.steps)

        } finally {
            migratedDb.close()
        }
    }

    private fun createVersion1Database(context: Context, dbName: String) {
        val dbHelperConfig = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create all 10 baseline Version 1 tables
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `routine_anchors` (
                            `id` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `defaultTargetMinutes` INTEGER NOT NULL,
                            `sortOrder` INTEGER NOT NULL,
                            `isEnabled` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `routine_entries` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `date` TEXT NOT NULL,
                            `anchorId` TEXT NOT NULL,
                            `targetTimeMinutes` INTEGER NOT NULL,
                            `actualTimeMinutes` INTEGER,
                            `status` TEXT NOT NULL,
                            `notes` TEXT,
                            `disruptionNote` TEXT,
                            `updatedAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `sleep_entries` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `date` TEXT NOT NULL,
                            `bedtimeMinutes` INTEGER NOT NULL,
                            `wakeTimeMinutes` INTEGER NOT NULL,
                            `durationMinutes` INTEGER NOT NULL,
                            `qualityRating` INTEGER NOT NULL,
                            `notes` TEXT,
                            `updatedAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `health_checkins` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `date` TEXT NOT NULL,
                            `mood` INTEGER,
                            `energy` INTEGER,
                            `stress` INTEGER,
                            `irritability` INTEGER,
                            `activationEnergy` INTEGER,
                            `symptomsNotes` TEXT,
                            `updatedAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `water_entries` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `date` TEXT NOT NULL,
                            `volumeMl` INTEGER NOT NULL,
                            `timestamp` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `medications` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `dosage` TEXT NOT NULL,
                            `unit` TEXT NOT NULL,
                            `instructions` TEXT NOT NULL,
                            `scheduledTimeMinutes` INTEGER NOT NULL,
                            `reminderEnabled` INTEGER NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `notes` TEXT
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `medication_intakes` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `date` TEXT NOT NULL,
                            `medicationId` INTEGER NOT NULL,
                            `scheduledTimeMinutes` INTEGER NOT NULL,
                            `actualActionTime` INTEGER,
                            `status` TEXT NOT NULL,
                            `notes` TEXT
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `study_tasks` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `isDailyPriority` INTEGER NOT NULL,
                            `estimatedMinutes` INTEGER NOT NULL,
                            `dateCreated` TEXT NOT NULL,
                            `dateCompleted` TEXT,
                            `nextSmallStep` TEXT,
                            `sortOrder` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `study_sessions` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `taskId` INTEGER,
                            `taskTitle` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `durationMinutes` INTEGER NOT NULL,
                            `sessionType` TEXT NOT NULL,
                            `date` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            `learnedNotes` TEXT
                        )
                        """.trimIndent()
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `app_settings` (
                            `key` TEXT NOT NULL,
                            `value` TEXT NOT NULL,
                            PRIMARY KEY(`key`)
                        )
                        """.trimIndent()
                    )

                    // Version 1 unique indexes
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_routine_entries_date_anchorId` ON `routine_entries` (`date`, `anchorId`)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sleep_entries_date` ON `sleep_entries` (`date`)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_health_checkins_date` ON `health_checkins` (`date`)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_medication_intakes_date_medicationId_scheduledTimeMinutes` ON `medication_intakes` (`date`, `medicationId`, `scheduledTimeMinutes`)")

                    // Seed test data in Version 1
                    db.execSQL("INSERT INTO `routine_anchors` VALUES ('wake_up', 'Morning Wake', 'Early wake', 420, 1, 1)")
                    db.execSQL("INSERT INTO `study_tasks` VALUES (1, 'Initial Task', 'Coding', 'PENDING', 1, 60, '2026-11-01', NULL, 'Start', 1)")
                    db.execSQL("INSERT INTO `medications` VALUES (1, 'Aspirin', '100', 'mg', 'With water', 480, 1, 1, 'Notes')")
                    db.execSQL("INSERT INTO `app_settings` VALUES ('theme_mode', 'DARK')")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // No-op for v1 creation helper
                }
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(dbHelperConfig)
        val db = helper.writableDatabase
        db.close()
        helper.close()
    }
}
