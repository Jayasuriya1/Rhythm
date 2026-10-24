package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "routine_anchors")
data class RoutineAnchorEntity(
    @PrimaryKey val id: String, // e.g. "out_of_bed", "first_contact", "main_activity", "dinner", "into_bed"
    val name: String,
    val description: String,
    val defaultTargetMinutes: Int, // minutes from 00:00 (e.g. 7:00 AM -> 420)
    val sortOrder: Int,
    val isEnabled: Boolean = true
)

@Entity(
    tableName = "routine_entries",
    indices = [Index(value = ["date", "anchorId"], unique = true)]
)
data class RoutineEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val anchorId: String,
    val targetTimeMinutes: Int,
    val actualTimeMinutes: Int?, // NULL means not yet recorded or skipped
    val status: String, // "COMPLETED", "SKIPPED", "UNRECORDED"
    val notes: String? = null,
    val disruptionNote: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sleep_entries",
    indices = [Index(value = ["date"], unique = true)]
)
data class SleepEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD (morning of wake up)
    val bedtimeMinutes: Int, // previous night bedtime in minutes from 00:00
    val wakeTimeMinutes: Int, // morning wake time
    val durationMinutes: Int, // calculated total minutes
    val qualityRating: Int, // 1 to 5
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "health_checkins",
    indices = [Index(value = ["date"], unique = true)]
)
data class HealthCheckinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val mood: Int? = null, // 1 to 5 (Low to Great)
    val energy: Int? = null, // 1 to 5 (Exhausted to High)
    val stress: Int? = null, // 1 to 5 (Calm to High)
    val irritability: Int? = null, // 1 to 5 (Low to High)
    val activationEnergy: Int? = null, // 1 to 5 (Normal to Elevated/Unusual)
    val symptomsNotes: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "water_entries")
data class WaterEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val volumeMl: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String,
    val unit: String, // e.g. "mg", "tablets", "capsule", "ml"
    val instructions: String, // e.g. "Take with food"
    val scheduledTimeMinutes: Int, // e.g. 08:00 -> 480
    val reminderEnabled: Boolean = false,
    val isActive: Boolean = true,
    val notes: String? = null
)

@Entity(
    tableName = "medication_intakes",
    indices = [Index(value = ["date", "medicationId", "scheduledTimeMinutes"], unique = true)]
)
data class MedicationIntakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val medicationId: Long,
    val scheduledTimeMinutes: Int,
    val actualActionTime: Long? = null, // timestamp when user tapped
    val status: String, // "TAKEN", "SKIPPED", "DELAYED", "MISSED", "UNRECORDED"
    val notes: String? = null
)

@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "DSA", "JavaScript", "React", "Backend", "Interview Prep", "General"
    val status: String = "NOT_STARTED", // "NOT_STARTED", "IN_PROGRESS", "COMPLETED", "SKIPPED"
    val isDailyPriority: Boolean = false,
    val estimatedMinutes: Int = 30,
    val dateCreated: String,
    val dateCompleted: String? = null,
    val nextSmallStep: String? = null, // "I'm stuck" micro-action
    val sortOrder: Int = 0
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val taskTitle: String,
    val category: String,
    val durationMinutes: Int,
    val sessionType: String, // "POMODORO", "DEEP_WORK", "STOPWATCH", "CUSTOM"
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val learnedNotes: String? = null
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
