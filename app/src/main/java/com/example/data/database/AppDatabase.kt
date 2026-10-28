package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.HealthDao
import com.example.data.dao.MedicationDao
import com.example.data.dao.MeditationDao
import com.example.data.dao.ReminderDao
import com.example.data.dao.RoutineDao
import com.example.data.dao.SettingsDao
import com.example.data.dao.SleepDao
import com.example.data.dao.BrainDumpDao
import com.example.data.dao.StudyCategoryDao
import com.example.data.dao.StudyDao
import com.example.data.dao.TimerDraftDao
import com.example.data.dao.WaterDao
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        RoutineAnchorEntity::class,
        RoutineEntryEntity::class,
        SleepEntryEntity::class,
        HealthCheckinEntity::class,
        WaterEntryEntity::class,
        MedicationEntity::class,
        MedicationIntakeEntity::class,
        StudyCategoryEntity::class,
        StudyTaskEntity::class,
        StudySessionEntity::class,
        ActiveTimerDraftEntity::class,
        BrainDumpNoteEntity::class,
        AppSettingEntity::class,
        ReminderEntity::class,
        MeditationSessionEntity::class,
        com.example.data.entity.CustomAlarmEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routineDao(): RoutineDao
    abstract fun sleepDao(): SleepDao
    abstract fun healthDao(): HealthDao
    abstract fun waterDao(): WaterDao
    abstract fun medicationDao(): MedicationDao
    abstract fun studyCategoryDao(): StudyCategoryDao
    abstract fun studyDao(): StudyDao
    abstract fun timerDraftDao(): TimerDraftDao
    abstract fun brainDumpDao(): BrainDumpDao
    abstract fun settingsDao(): SettingsDao
    abstract fun reminderDao(): ReminderDao
    abstract fun meditationDao(): MeditationDao
    abstract fun customAlarmDao(): com.example.data.dao.CustomAlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `colorHex` TEXT NOT NULL,
                        `iconName` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `active_timer_draft` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `taskId` INTEGER,
                        `taskTitle` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `targetSeconds` INTEGER NOT NULL,
                        `startWallClockMillis` INTEGER NOT NULL,
                        `accumulatedElapsedSeconds` INTEGER NOT NULL,
                        `isRunning` INTEGER NOT NULL,
                        `isPaused` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `lastUpdatedMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL("ALTER TABLE `study_sessions` ADD COLUMN `startTimeMillis` INTEGER")
                db.execSQL("ALTER TABLE `study_sessions` ADD COLUMN `endTimeMillis` INTEGER")
                db.execSQL("ALTER TABLE `study_sessions` ADD COLUMN `completionStatus` TEXT NOT NULL DEFAULT 'COMPLETED'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `brain_dump_notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT,
                        `content` TEXT NOT NULL,
                        `tag` TEXT,
                        `isPinned` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `medications` ADD COLUMN `dosageForm` TEXT")
                db.execSQL("ALTER TABLE `medications` ADD COLUMN `scheduledTimesJson` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `medications` ADD COLUMN `repeatDays` TEXT NOT NULL DEFAULT 'DAILY'")
                db.execSQL("ALTER TABLE `medications` ADD COLUMN `startDate` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `medications` ADD COLUMN `endDate` TEXT")
                db.execSQL("ALTER TABLE `medications` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE `medication_intakes` ADD COLUMN `medicationName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `medication_intakes` ADD COLUMN `dosageSnapshot` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `medication_intakes` ADD COLUMN `isCorrected` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `medication_intakes` ADD COLUMN `correctedAt` INTEGER")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reminders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `timeMinutes` INTEGER NOT NULL,
                        `repeatDays` TEXT NOT NULL DEFAULT 'DAILY',
                        `isEnabled` INTEGER NOT NULL DEFAULT 1,
                        `soundEnabled` INTEGER NOT NULL DEFAULT 1,
                        `vibrateEnabled` INTEGER NOT NULL DEFAULT 1,
                        `isUrgent` INTEGER NOT NULL DEFAULT 0,
                        `referenceId` TEXT,
                        `customMessage` TEXT,
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `voiceAnnouncementEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `spokenAnnouncementText` TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `meditation_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `techniqueId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `completedAt` INTEGER NOT NULL,
                        `reflectionNotes` TEXT,
                        `status` TEXT NOT NULL DEFAULT 'COMPLETED'
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `customAudioUri` TEXT")
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `customAudioTitle` TEXT")
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `customAudioCollectionJson` TEXT")
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `audioPlaybackMode` TEXT NOT NULL DEFAULT 'DEFAULT'")
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `lastPlayedAudioUri` TEXT")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_alarms` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `timeMinutes` INTEGER NOT NULL,
                        `repeatDays` TEXT NOT NULL DEFAULT 'WEEKDAYS',
                        `isEnabled` INTEGER NOT NULL DEFAULT 1,
                        `vibrateEnabled` INTEGER NOT NULL DEFAULT 1,
                        `customAudioUri` TEXT,
                        `customAudioTitle` TEXT,
                        `crescendoDurationSeconds` INTEGER NOT NULL DEFAULT 60,
                        `isLooping` INTEGER NOT NULL DEFAULT 1,
                        `wallpaperUri` TEXT,
                        `wallpaperPresetColor` TEXT NOT NULL DEFAULT 'MIDNIGHT_INDIGO',
                        `timeClockFont` TEXT NOT NULL DEFAULT 'MODERN_DIGITAL',
                        `missionType` TEXT NOT NULL DEFAULT 'NONE',
                        `targetBarcode` TEXT,
                        `targetBarcodeLabel` TEXT,
                        `chainedBackupAlarmEnabled` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `active_timer_draft` ADD COLUMN `currentPhase` TEXT NOT NULL DEFAULT 'FOCUS'")
                db.execSQL("ALTER TABLE `active_timer_draft` ADD COLUMN `phaseElapsedSeconds` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `active_timer_draft` ADD COLUMN `phaseTargetSeconds` INTEGER NOT NULL DEFAULT 1500")
                db.execSQL("ALTER TABLE `active_timer_draft` ADD COLUMN `protocolJson` TEXT")
                db.execSQL("ALTER TABLE `active_timer_draft` ADD COLUMN `passedBreakPointIds` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `active_timer_draft` ADD COLUMN `activeBreakExtensionSeconds` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rhythm_focus_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_ANCHORS = listOf(
            RoutineAnchorEntity(
                id = "out_of_bed",
                name = "Getting out of bed",
                description = "Waking up and physically leaving bed to begin the day",
                defaultTargetMinutes = 420, // 07:00 AM
                sortOrder = 1
            ),
            RoutineAnchorEntity(
                id = "first_contact",
                name = "First social contact",
                description = "First conversation, phone call, or meaningful interaction",
                defaultTargetMinutes = 540, // 09:00 AM
                sortOrder = 2
            ),
            RoutineAnchorEntity(
                id = "main_activity",
                name = "Main daily activity",
                description = "Starting primary study block, project work, or job",
                defaultTargetMinutes = 600, // 10:00 AM
                sortOrder = 3
            ),
            RoutineAnchorEntity(
                id = "dinner",
                name = "Dinner",
                description = "Regular evening meal timing",
                defaultTargetMinutes = 1170, // 07:30 PM
                sortOrder = 4
            ),
            RoutineAnchorEntity(
                id = "getting_into_bed",
                name = "Getting into bed",
                description = "Winding down and settling into bed to rest",
                defaultTargetMinutes = 1380, // 11:00 PM
                sortOrder = 5
            )
        )

        val DEFAULT_CATEGORIES = listOf(
            StudyCategoryEntity(
                name = "DSA",
                description = "Data structures, algorithms, and complexity practice",
                colorHex = "#3F5E48",
                iconName = "code",
                sortOrder = 1
            ),
            StudyCategoryEntity(
                name = "JavaScript",
                description = "Modern JS, async programming, and engine internals",
                colorHex = "#D4A373",
                iconName = "javascript",
                sortOrder = 2
            ),
            StudyCategoryEntity(
                name = "React",
                description = "React components, state architecture, and Compose UI",
                colorHex = "#3B82F6",
                iconName = "web",
                sortOrder = 3
            ),
            StudyCategoryEntity(
                name = "Backend",
                description = "APIs, SQL databases, servers, and microservices",
                colorHex = "#7C3AED",
                iconName = "storage",
                sortOrder = 4
            ),
            StudyCategoryEntity(
                name = "Interview Prep",
                description = "Mock coding, behavioral preparation, and system design",
                colorHex = "#EA580C",
                iconName = "psychology",
                sortOrder = 5
            ),
            StudyCategoryEntity(
                name = "General",
                description = "General study, technical reading, and research",
                colorHex = "#6B7280",
                iconName = "school",
                sortOrder = 6
            )
        )

        val DEFAULT_REMINDERS = listOf(
            ReminderEntity(
                title = "Morning Wake-Up",
                category = "WAKE_UP",
                timeMinutes = 420, // 07:00 AM
                repeatDays = "DAILY",
                isEnabled = true,
                soundEnabled = true,
                vibrateEnabled = true,
                isUrgent = false,
                referenceId = "default:wake_up",
                customMessage = "Gentle start to your day — hydrate and step outside"
            ),
            ReminderEntity(
                title = "Breakfast & Nourishment",
                category = "MEAL",
                timeMinutes = 480, // 08:00 AM
                repeatDays = "DAILY",
                isEnabled = true,
                soundEnabled = true,
                vibrateEnabled = true,
                isUrgent = false,
                referenceId = "default:breakfast",
                customMessage = "Stable breakfast fuels steady rhythm"
            ),
            ReminderEntity(
                title = "Mid-Day Hydration Pause",
                category = "HYDRATION",
                timeMinutes = 660, // 11:00 AM
                repeatDays = "DAILY",
                isEnabled = true,
                soundEnabled = false,
                vibrateEnabled = true,
                isUrgent = false,
                referenceId = "default:hydration_morning",
                customMessage = "Take a sip of water and reset posture"
            ),
            ReminderEntity(
                title = "Lunch & Rest Break",
                category = "MEAL",
                timeMinutes = 780, // 01:00 PM
                repeatDays = "DAILY",
                isEnabled = true,
                soundEnabled = true,
                vibrateEnabled = true,
                isUrgent = false,
                referenceId = "default:lunch",
                customMessage = "Step away from screens for 20 minutes"
            ),
            ReminderEntity(
                title = "Afternoon Hydration",
                category = "HYDRATION",
                timeMinutes = 930, // 03:30 PM
                repeatDays = "DAILY",
                isEnabled = true,
                soundEnabled = false,
                vibrateEnabled = true,
                isUrgent = false,
                referenceId = "default:hydration_afternoon",
                customMessage = "Quick glass of water for evening focus"
            ),
            ReminderEntity(
                title = "Dinner Anchor",
                category = "ROUTINE_ANCHOR",
                timeMinutes = 1170, // 07:30 PM
                repeatDays = "DAILY",
                isEnabled = true,
                soundEnabled = true,
                vibrateEnabled = true,
                isUrgent = false,
                referenceId = "default:dinner",
                customMessage = "Evening routine anchor time"
            ),
            ReminderEntity(
                title = "Bedtime Wind-Down",
                category = "WIND_DOWN",
                timeMinutes = 1320, // 10:00 PM
                repeatDays = "DAILY",
                isEnabled = true,
                soundEnabled = true,
                vibrateEnabled = true,
                isUrgent = false,
                referenceId = "default:wind_down",
                customMessage = "Dim lights and park active thoughts"
            ),
            ReminderEntity(
                title = "Bedtime Rest",
                category = "BEDTIME",
                timeMinutes = 1380, // 11:00 PM
                repeatDays = "DAILY",
                isEnabled = true,
                soundEnabled = true,
                vibrateEnabled = true,
                isUrgent = false,
                referenceId = "default:bedtime",
                customMessage = "Restful sleep restores tomorrow's focus"
            )
        )

        val DEFAULT_ALARMS = listOf(
            com.example.data.entity.CustomAlarmEntity(
                title = "Morning Wake-Up",
                timeMinutes = 420, // 07:00 AM
                repeatDays = "WEEKDAYS",
                isEnabled = true,
                vibrateEnabled = true,
                customAudioTitle = "Gentle Sunrise Chime",
                crescendoDurationSeconds = 60,
                isLooping = true,
                wallpaperPresetColor = "SUNRISE_GOLD",
                timeClockFont = "MODERN_DIGITAL",
                missionType = "NONE",
                chainedBackupAlarmEnabled = true
            ),
            com.example.data.entity.CustomAlarmEntity(
                title = "Deep Focus Kickoff",
                timeMinutes = 540, // 09:00 AM
                repeatDays = "WEEKDAYS",
                isEnabled = false,
                vibrateEnabled = true,
                customAudioTitle = "Mindful Focus Bell",
                crescendoDurationSeconds = 30,
                isLooping = true,
                wallpaperPresetColor = "FOREST_EMERALD",
                timeClockFont = "MONOSPACE",
                missionType = "NONE",
                chainedBackupAlarmEnabled = true
            )
        )
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    database.routineDao().insertDefaultAnchors(DEFAULT_ANCHORS)
                    database.studyCategoryDao().insertDefaultCategories(DEFAULT_CATEGORIES)
                    database.reminderDao().insertDefaultReminders(DEFAULT_REMINDERS)
                    database.customAlarmDao().insertDefaultAlarms(DEFAULT_ALARMS)
                }
            }
        }
    }
}
