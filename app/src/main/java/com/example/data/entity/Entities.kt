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
    val unit: String, // e.g. "mg", "mcg", "g", "mL", "tablet", "capsule", "drops", "puff", "other"
    val instructions: String, // e.g. "Take with food"
    val scheduledTimeMinutes: Int, // primary scheduled time (e.g. 08:00 -> 480)
    val reminderEnabled: Boolean = false,
    val isActive: Boolean = true,
    val notes: String? = null,
    val dosageForm: String? = null, // e.g. "Tablet", "Capsule", "Liquid", "Inhaler", "Drops", "Injection", "Patch", "Topical", "Other"
    val scheduledTimesJson: String = "", // comma-separated minutes e.g. "480,1200". If empty, fallback to [scheduledTimeMinutes]
    val repeatDays: String = "DAILY", // "DAILY", or comma-separated "MON,TUE,WED,THU,FRI,SAT,SUN"
    val startDate: String = "", // YYYY-MM-DD
    val endDate: String? = null, // YYYY-MM-DD
    val isArchived: Boolean = false
) {
    fun getScheduledTimes(): List<Int> {
        if (scheduledTimesJson.isNotBlank()) {
            val parsed = scheduledTimesJson.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 0..1439 }
            if (parsed.isNotEmpty()) return parsed.sorted()
        }
        return listOf(scheduledTimeMinutes)
    }

    fun isScheduledForDayOfWeek(dayAbbr: String): Boolean {
        if (repeatDays.equals("DAILY", ignoreCase = true) || repeatDays.isBlank()) return true
        val days = repeatDays.split(",").map { it.trim().uppercase() }
        return days.contains(dayAbbr.uppercase())
    }

    fun isEffectiveOnDate(dateString: String): Boolean {
        if (startDate.isNotBlank() && dateString < startDate) return false
        if (!endDate.isNullOrBlank() && dateString > endDate) return false
        return true
    }
}

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
    val notes: String? = null,
    val medicationName: String = "", // snapshot so history is preserved if med is deleted/edited
    val dosageSnapshot: String = "", // e.g. "50 mg"
    val isCorrected: Boolean = false,
    val correctedAt: Long? = null
)

@Entity(tableName = "study_categories")
data class StudyCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val colorHex: String = "#4D6B53",
    val iconName: String = "category",
    val sortOrder: Int = 0,
    val isArchived: Boolean = false
)

@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // Backed by StudyCategoryEntity.name
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
    val startTimeMillis: Long? = null,
    val endTimeMillis: Long? = null,
    val completionStatus: String = "COMPLETED", // "COMPLETED", "INTERRUPTED", "PARTIAL"
    val learnedNotes: String? = null
)

@Entity(tableName = "active_timer_draft")
data class ActiveTimerDraftEntity(
    @PrimaryKey val id: Int = 1, // Single active timer state record
    val taskId: Long? = null,
    val taskTitle: String = "Focused Study",
    val category: String = "General",
    val mode: String = "POMODORO", // "POMODORO", "DEEP_WORK", "CUSTOM", "STOPWATCH", "PROTOCOL"
    val targetSeconds: Int = 25 * 60,
    val startWallClockMillis: Long = 0L,
    val accumulatedElapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val currentPhase: String = "FOCUS", // "FOCUS", "SHORT_BREAK", "LONG_BREAK", "COOLDOWN", "COMPLETED"
    val phaseElapsedSeconds: Int = 0,
    val phaseTargetSeconds: Int = 25 * 60,
    val protocolJson: String? = null,
    val passedBreakPointIds: String = "",
    val activeBreakExtensionSeconds: Int = 0
)

@Entity(tableName = "brain_dump_notes")
data class BrainDumpNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String? = null,
    val content: String,
    val tag: String? = null, // e.g. "Idea", "Study", "Todo", "Health", "Personal"
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun displayTitle(): String {
        if (!title.isNullOrBlank()) return title
        val firstLine = content.lines().firstOrNull { it.isNotBlank() } ?: "Untitled Thought"
        return if (firstLine.length > 35) firstLine.take(32) + "..." else firstLine
    }
}

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "WAKE_UP", "BEDTIME", "WIND_DOWN", "MEAL", "MEDICATION", "ROUTINE_ANCHOR", "STUDY_BREAK", "HYDRATION", "CUSTOM"
    val timeMinutes: Int, // 0..1439
    val repeatDays: String = "DAILY", // "DAILY", or "MON,TUE,WED,THU,FRI,SAT,SUN"
    val isEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val isUrgent: Boolean = false, // Bypasses quiet hours
    val referenceId: String? = null, // e.g. "anchor:out_of_bed" or "med:3"
    val customMessage: String? = null,
    val voiceAnnouncementEnabled: Boolean = false, // Optional TTS announcement
    val spokenAnnouncementText: String? = null, // Custom voice message, e.g. "It's time for your medication"
    val customAudioUri: String? = null, // Persisted content:// URI for custom alarm audio file
    val customAudioTitle: String? = null, // User-friendly filename e.g. "Morning Sunlight.mp3"
    val customAudioCollectionJson: String? = null, // JSON array of [{"uri": "...", "title": "..."}] for random collection
    val audioPlaybackMode: String = "DEFAULT", // "DEFAULT", "CUSTOM_SINGLE", "CUSTOM_RANDOM"
    val lastPlayedAudioUri: String? = null, // Prevents consecutive duplicate plays in random collection
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isScheduledForDayOfWeek(dayAbbr: String): Boolean {
        if (repeatDays.equals("DAILY", ignoreCase = true) || repeatDays.isBlank()) return true
        if (repeatDays.equals("WEEKDAYS", ignoreCase = true)) {
            return listOf("MON", "TUE", "WED", "THU", "FRI").contains(dayAbbr.uppercase())
        }
        if (repeatDays.equals("WEEKENDS", ignoreCase = true)) {
            return listOf("SAT", "SUN").contains(dayAbbr.uppercase())
        }
        val days = repeatDays.split(",").map { it.trim().uppercase() }
        return days.contains(dayAbbr.uppercase())
    }

    fun formattedSchedule(): String {
        return when {
            repeatDays.equals("DAILY", ignoreCase = true) || repeatDays.isBlank() -> "Every day"
            repeatDays.equals("WEEKDAYS", ignoreCase = true) -> "Weekdays (Mon–Fri)"
            repeatDays.equals("WEEKENDS", ignoreCase = true) -> "Weekends (Sat–Sun)"
            else -> repeatDays.split(",").joinToString(", ") { it.trim().capitalizeUS() }
        }
    }

    fun getAudioCollection(): List<Pair<String, String>> {
        if (customAudioCollectionJson.isNullOrBlank()) return emptyList()
        return try {
            val list = mutableListOf<Pair<String, String>>()
            val jsonArray = org.json.JSONArray(customAudioCollectionJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(obj.getString("uri") to obj.optString("title", "Track ${i + 1}"))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun formattedTime(): String {
        val hours = timeMinutes / 60
        val minutes = timeMinutes % 60
        val isPm = hours >= 12
        val displayHour = when {
            hours == 0 -> 12
            hours > 12 -> hours - 12
            else -> hours
        }
        val amPm = if (isPm) "PM" else "AM"
        return String.format(java.util.Locale.US, "%d:%02d %s", displayHour, minutes, amPm)
    }

    fun effectiveVoiceAnnouncement(): String {
        if (!spokenAnnouncementText.isNullOrBlank()) {
            return spokenAnnouncementText
        }
        return "It's time for your $title reminder."
    }
}

private fun String.capitalizeUS(): String {
    return this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString() }
}

@Entity(tableName = "meditation_sessions")
data class MeditationSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val techniqueId: String, // "mindful_breathing", "body_scan", "grounding_senses", "mindful_observation", "study_break"
    val title: String,
    val durationMinutes: Int,
    val completedAt: Long = System.currentTimeMillis(),
    val reflectionNotes: String? = null,
    val status: String = "COMPLETED" // "COMPLETED", "STOPPED_EARLY"
)

@Entity(tableName = "custom_alarms")
data class CustomAlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timeMinutes: Int, // 0..1439
    val repeatDays: String = "WEEKDAYS",
    val isEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val customAudioUri: String? = null,
    val customAudioTitle: String? = "Default Alarm Tone",
    val crescendoDurationSeconds: Int = 60,
    val isLooping: Boolean = true,
    val wallpaperUri: String? = null,
    val wallpaperPresetColor: String = "MIDNIGHT_INDIGO",
    val timeClockFont: String = "MODERN_DIGITAL",
    val missionType: String = "NONE", // "NONE", "BARCODE"
    val targetBarcode: String? = null,
    val targetBarcodeLabel: String? = null,
    val chainedBackupAlarmEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun formattedTime(): String {
        val hour24 = timeMinutes / 60
        val minute = timeMinutes % 60
        val isPm = hour24 >= 12
        val hour12 = when (hour24) {
            0 -> 12
            in 1..12 -> hour24
            else -> hour24 - 12
        }
        val amPm = if (isPm) "PM" else "AM"
        return String.format(java.util.Locale.US, "%02d:%02d %s", hour12, minute, amPm)
    }

    fun formattedSchedule(): String {
        return when {
            repeatDays.equals("DAILY", ignoreCase = true) || repeatDays.isBlank() -> "Every day"
            repeatDays.equals("WEEKDAYS", ignoreCase = true) -> "Weekdays (Mon–Fri)"
            repeatDays.equals("WEEKENDS", ignoreCase = true) -> "Weekends (Sat–Sun)"
            else -> {
                val days = repeatDays.split(",").map { it.trim() }
                if (days.size == 7) "Every day" else days.joinToString(", ") { it.capitalizeUS() }
            }
        }
    }

    fun isScheduledForDay(dayAbbr: String): Boolean {
        if (repeatDays.equals("DAILY", ignoreCase = true) || repeatDays.isBlank()) return true
        if (repeatDays.equals("WEEKDAYS", ignoreCase = true)) {
            return listOf("MON", "TUE", "WED", "THU", "FRI").contains(dayAbbr.uppercase())
        }
        if (repeatDays.equals("WEEKENDS", ignoreCase = true)) {
            return listOf("SAT", "SUN").contains(dayAbbr.uppercase())
        }
        val days = repeatDays.split(",").map { it.trim().uppercase() }
        return days.contains(dayAbbr.uppercase())
    }
}
