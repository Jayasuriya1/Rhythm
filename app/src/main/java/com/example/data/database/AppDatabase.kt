package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.HealthDao
import com.example.data.dao.MedicationDao
import com.example.data.dao.RoutineDao
import com.example.data.dao.SettingsDao
import com.example.data.dao.SleepDao
import com.example.data.dao.StudyDao
import com.example.data.dao.WaterDao
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
        StudyTaskEntity::class,
        StudySessionEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routineDao(): RoutineDao
    abstract fun sleepDao(): SleepDao
    abstract fun healthDao(): HealthDao
    abstract fun waterDao(): WaterDao
    abstract fun medicationDao(): MedicationDao
    abstract fun studyDao(): StudyDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rhythm_focus_database"
                )
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
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    database.routineDao().insertDefaultAnchors(DEFAULT_ANCHORS)
                }
            }
        }
    }
}
