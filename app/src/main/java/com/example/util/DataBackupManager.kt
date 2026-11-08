package com.example.util

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.entity.ActiveTimerDraftEntity
import com.example.data.entity.AppSettingEntity
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
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupSummary(
    val exportTimestamp: Long,
    val routineEntriesCount: Int,
    val sleepCount: Int,
    val healthCount: Int,
    val waterCount: Int,
    val medicationsCount: Int,
    val intakesCount: Int,
    val studyTasksCount: Int,
    val studySessionsCount: Int,
    val brainDumpCount: Int,
    val remindersCount: Int,
    val meditationSessionsCount: Int = 0,
    val customAlarmsCount: Int = 0,
    val registeredCodesCount: Int = 0
)

data class DatabaseIntegrityStats(
    val totalRoutines: Int,
    val totalSleep: Int,
    val totalHealth: Int,
    val totalWater: Int,
    val totalMedications: Int,
    val totalIntakes: Int,
    val totalCategories: Int,
    val totalTasks: Int,
    val totalSessions: Int,
    val totalBrainDump: Int,
    val totalReminders: Int,
    val enabledReminders: Int,
    val totalMeditationSessions: Int,
    val totalCustomAlarms: Int = 0,
    val totalRegisteredCodes: Int = 0,
    val isConsistent: Boolean,
    val isEncrypted: Boolean = false
)

object DataBackupManager {

    const val CURRENT_BACKUP_SCHEMA_VERSION = 5
    const val MIN_SUPPORTED_BACKUP_SCHEMA_VERSION = 1
    const val MAX_SUPPORTED_BACKUP_SCHEMA_VERSION = 5

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_LENGTH_BITS = 256
    const val PBKDF2_ITERATIONS = 100_000
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    const val FORMAT_ENCRYPTED_BACKUP = "encrypted_backup"

    fun isEncryptedBackup(content: String): Boolean {
        return try {
            val trimmed = content.trim()
            if (!trimmed.startsWith("{")) return false
            val root = JSONObject(trimmed)
            root.optString("format") == FORMAT_ENCRYPTED_BACKUP ||
                (root.has("ciphertext") && root.has("crypto"))
        } catch (_: Exception) {
            false
        }
    }

    private fun deriveKey(
        passphrase: CharArray,
        salt: ByteArray,
        iterations: Int = PBKDF2_ITERATIONS
    ): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptPayload(
        plainText: String,
        passphrase: CharArray,
        iterations: Int = PBKDF2_ITERATIONS
    ): String {
        require(passphrase.isNotEmpty()) { "Passphrase cannot be empty" }
        val salt = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val secretKey = deriveKey(passphrase, salt, iterations)

        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val plainBytes = plainText.toByteArray(Charsets.UTF_8)
        val cipherBytes = cipher.doFinal(plainBytes)

        val encoder = Base64.getEncoder()
        val saltB64 = encoder.encodeToString(salt)
        val ivB64 = encoder.encodeToString(iv)
        val ciphertextB64 = encoder.encodeToString(cipherBytes)

        val root = JSONObject()
        root.put("app", "Rhythm & Focus")
        root.put("format", FORMAT_ENCRYPTED_BACKUP)
        root.put("version", 1)

        val crypto = JSONObject()
        crypto.put("algorithm", "AES-256-GCM")
        crypto.put("kdf", PBKDF2_ALGORITHM)
        crypto.put("iterations", iterations)
        crypto.put("salt", saltB64)
        crypto.put("iv", ivB64)
        root.put("crypto", crypto)

        root.put("ciphertext", ciphertextB64)

        return root.toString(2)
    }

    fun decryptPayload(
        encryptedJson: String,
        passphrase: CharArray
    ): String {
        require(passphrase.isNotEmpty()) { "Passphrase cannot be empty" }
        val root = JSONObject(encryptedJson)
        val app = root.optString("app")
        if (app != "Rhythm & Focus") {
            throw IllegalArgumentException("Invalid backup file: App identifier missing or mismatched")
        }
        val crypto = root.optJSONObject("crypto")
            ?: throw IllegalArgumentException("Missing crypto metadata in encrypted backup")
        val iterations = crypto.optInt("iterations", PBKDF2_ITERATIONS)
        val saltB64 = crypto.optString("salt")
        val ivB64 = crypto.optString("iv")
        val ciphertextB64 = root.optString("ciphertext")

        if (saltB64.isNullOrEmpty() || ivB64.isNullOrEmpty() || ciphertextB64.isNullOrEmpty()) {
            throw IllegalArgumentException("Corrupted backup: Missing encryption components")
        }

        val decoder = Base64.getDecoder()
        val salt = decoder.decode(saltB64)
        val iv = decoder.decode(ivB64)
        val cipherBytes = decoder.decode(ciphertextB64)

        val secretKey = deriveKey(passphrase, salt, iterations)
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(cipherBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun JSONObject.optNullableString(name: String): String? {
        return if (has(name) && !isNull(name)) {
            val s = getString(name)
            if (s == "null") null else s
        } else null
    }

    suspend fun exportBackup(database: AppDatabase, passphrase: String? = null): String {
        val root = JSONObject()
        root.put("app", "Rhythm & Focus")
        root.put("version", 1)
        root.put("schemaVersion", CURRENT_BACKUP_SCHEMA_VERSION)
        root.put("exportTimestamp", System.currentTimeMillis())

        // 1. Routine Anchors & Entries
        val anchorsArr = JSONArray()
        database.routineDao().getAllAnchorsSync().forEach { a ->
            val o = JSONObject()
            o.put("id", a.id)
            o.put("name", a.name)
            o.put("description", a.description)
            o.put("defaultTargetMinutes", a.defaultTargetMinutes)
            o.put("sortOrder", a.sortOrder)
            o.put("isEnabled", a.isEnabled)
            anchorsArr.put(o)
        }
        root.put("routineAnchors", anchorsArr)

        val entriesArr = JSONArray()
        database.routineDao().getAllEntriesSync().forEach { e ->
            val o = JSONObject()
            o.put("id", e.id)
            o.put("date", e.date)
            o.put("anchorId", e.anchorId)
            o.put("targetTimeMinutes", e.targetTimeMinutes)
            if (e.actualTimeMinutes != null) o.put("actualTimeMinutes", e.actualTimeMinutes)
            o.put("status", e.status)
            if (e.notes != null) o.put("notes", e.notes)
            if (e.disruptionNote != null) o.put("disruptionNote", e.disruptionNote)
            o.put("updatedAt", e.updatedAt)
            entriesArr.put(o)
        }
        root.put("routineEntries", entriesArr)

        // 2. Sleep Entries
        val sleepArr = JSONArray()
        database.sleepDao().getAllSleepSync().forEach { s ->
            val o = JSONObject()
            o.put("id", s.id)
            o.put("date", s.date)
            o.put("bedtimeMinutes", s.bedtimeMinutes)
            o.put("wakeTimeMinutes", s.wakeTimeMinutes)
            o.put("durationMinutes", s.durationMinutes)
            o.put("qualityRating", s.qualityRating)
            if (s.notes != null) o.put("notes", s.notes)
            o.put("updatedAt", s.updatedAt)
            sleepArr.put(o)
        }
        root.put("sleepEntries", sleepArr)

        // 3. Health Checkins
        val healthArr = JSONArray()
        database.healthDao().getAllHealthSync().forEach { h ->
            val o = JSONObject()
            o.put("id", h.id)
            o.put("date", h.date)
            if (h.mood != null) o.put("mood", h.mood)
            if (h.energy != null) o.put("energy", h.energy)
            if (h.stress != null) o.put("stress", h.stress)
            if (h.irritability != null) o.put("irritability", h.irritability)
            if (h.activationEnergy != null) o.put("activationEnergy", h.activationEnergy)
            if (h.symptomsNotes != null) o.put("symptomsNotes", h.symptomsNotes)
            o.put("updatedAt", h.updatedAt)
            healthArr.put(o)
        }
        root.put("healthCheckins", healthArr)

        // 4. Water Entries
        val waterArr = JSONArray()
        database.waterDao().getAllWaterSync().forEach { w ->
            val o = JSONObject()
            o.put("id", w.id)
            o.put("date", w.date)
            o.put("volumeMl", w.volumeMl)
            o.put("timestamp", w.timestamp)
            waterArr.put(o)
        }
        root.put("waterEntries", waterArr)

        // 5. Medications & Intakes
        val medsArr = JSONArray()
        database.medicationDao().getAllMedicationsSync().forEach { m ->
            val o = JSONObject()
            o.put("id", m.id)
            o.put("name", m.name)
            o.put("dosage", m.dosage)
            o.put("unit", m.unit)
            o.put("instructions", m.instructions)
            o.put("scheduledTimeMinutes", m.scheduledTimeMinutes)
            o.put("reminderEnabled", m.reminderEnabled)
            o.put("isActive", m.isActive)
            if (m.notes != null) o.put("notes", m.notes)
            if (m.dosageForm != null) o.put("dosageForm", m.dosageForm)
            o.put("scheduledTimesJson", m.scheduledTimesJson)
            o.put("repeatDays", m.repeatDays)
            o.put("startDate", m.startDate)
            if (m.endDate != null) o.put("endDate", m.endDate)
            o.put("isArchived", m.isArchived)
            medsArr.put(o)
        }
        root.put("medications", medsArr)

        val intakesArr = JSONArray()
        database.medicationDao().getAllIntakesSync().forEach { i ->
            val o = JSONObject()
            o.put("id", i.id)
            o.put("date", i.date)
            o.put("medicationId", i.medicationId)
            o.put("scheduledTimeMinutes", i.scheduledTimeMinutes)
            if (i.actualActionTime != null) o.put("actualActionTime", i.actualActionTime)
            o.put("status", i.status)
            if (i.notes != null) o.put("notes", i.notes)
            o.put("medicationName", i.medicationName)
            o.put("dosageSnapshot", i.dosageSnapshot)
            o.put("isCorrected", i.isCorrected)
            if (i.correctedAt != null) o.put("correctedAt", i.correctedAt)
            intakesArr.put(o)
        }
        root.put("medicationIntakes", intakesArr)

        // 6. Study Categories, Tasks, Sessions
        val catArr = JSONArray()
        database.studyCategoryDao().getAllCategoriesSync().forEach { c ->
            val o = JSONObject()
            o.put("id", c.id)
            o.put("name", c.name)
            if (c.description != null) o.put("description", c.description)
            o.put("colorHex", c.colorHex)
            o.put("iconName", c.iconName)
            o.put("sortOrder", c.sortOrder)
            o.put("isArchived", c.isArchived)
            catArr.put(o)
        }
        root.put("studyCategories", catArr)

        val tasksArr = JSONArray()
        database.studyDao().getTasksListSync().forEach { t ->
            val o = JSONObject()
            o.put("id", t.id)
            o.put("title", t.title)
            o.put("category", t.category)
            o.put("status", t.status)
            o.put("isDailyPriority", t.isDailyPriority)
            o.put("estimatedMinutes", t.estimatedMinutes)
            o.put("dateCreated", t.dateCreated)
            if (t.dateCompleted != null) o.put("dateCompleted", t.dateCompleted)
            if (t.nextSmallStep != null) o.put("nextSmallStep", t.nextSmallStep)
            o.put("sortOrder", t.sortOrder)
            tasksArr.put(o)
        }
        root.put("studyTasks", tasksArr)

        val sessionsArr = JSONArray()
        database.studyDao().getAllSessionsSync().forEach { s ->
            val o = JSONObject()
            o.put("id", s.id)
            if (s.taskId != null) o.put("taskId", s.taskId)
            o.put("taskTitle", s.taskTitle)
            o.put("category", s.category)
            o.put("durationMinutes", s.durationMinutes)
            o.put("sessionType", s.sessionType)
            o.put("date", s.date)
            o.put("timestamp", s.timestamp)
            if (s.startTimeMillis != null) o.put("startTimeMillis", s.startTimeMillis)
            if (s.endTimeMillis != null) o.put("endTimeMillis", s.endTimeMillis)
            o.put("completionStatus", s.completionStatus)
            if (s.learnedNotes != null) o.put("learnedNotes", s.learnedNotes)
            sessionsArr.put(o)
        }
        root.put("studySessions", sessionsArr)

        // 7. Brain Dump Notes
        val notesArr = JSONArray()
        database.brainDumpDao().getAllNotesSync().forEach { n ->
            val o = JSONObject()
            o.put("id", n.id)
            if (n.title != null) o.put("title", n.title)
            o.put("content", n.content)
            if (n.tag != null) o.put("tag", n.tag)
            o.put("isPinned", n.isPinned)
            o.put("isArchived", n.isArchived)
            o.put("createdAt", n.createdAt)
            o.put("updatedAt", n.updatedAt)
            notesArr.put(o)
        }
        root.put("brainDumpNotes", notesArr)

        // 8. Reminders
        val remindersArr = JSONArray()
        database.reminderDao().getAllRemindersListSync().forEach { r ->
            val o = JSONObject()
            o.put("id", r.id)
            o.put("title", r.title)
            o.put("category", r.category)
            o.put("timeMinutes", r.timeMinutes)
            o.put("repeatDays", r.repeatDays)
            o.put("isEnabled", r.isEnabled)
            o.put("soundEnabled", r.soundEnabled)
            o.put("vibrateEnabled", r.vibrateEnabled)
            o.put("isUrgent", r.isUrgent)
            o.put("voiceAnnouncementEnabled", r.voiceAnnouncementEnabled)
            if (r.spokenAnnouncementText != null) o.put("spokenAnnouncementText", r.spokenAnnouncementText)
            if (r.customAudioUri != null) o.put("customAudioUri", r.customAudioUri)
            if (r.customAudioTitle != null) o.put("customAudioTitle", r.customAudioTitle)
            if (r.customAudioCollectionJson != null) o.put("customAudioCollectionJson", r.customAudioCollectionJson)
            o.put("audioPlaybackMode", r.audioPlaybackMode)
            if (r.lastPlayedAudioUri != null) o.put("lastPlayedAudioUri", r.lastPlayedAudioUri)
            if (r.referenceId != null) o.put("referenceId", r.referenceId)
            if (r.customMessage != null) o.put("customMessage", r.customMessage)
            o.put("createdAt", r.createdAt)
            remindersArr.put(o)
        }
        root.put("reminders", remindersArr)

        // 9. Meditation Sessions
        val meditationArr = JSONArray()
        database.meditationDao().getAllSessionsSync().forEach { m ->
            val o = JSONObject()
            o.put("id", m.id)
            o.put("techniqueId", m.techniqueId)
            o.put("title", m.title)
            o.put("durationMinutes", m.durationMinutes)
            o.put("completedAt", m.completedAt)
            if (m.reflectionNotes != null) o.put("reflectionNotes", m.reflectionNotes)
            o.put("status", m.status)
            meditationArr.put(o)
        }
        root.put("meditationSessions", meditationArr)

        // 10. App Settings
        val settingsArr = JSONArray()
        database.settingsDao().getAllSettingsSync().forEach { s ->
            val o = JSONObject()
            o.put("key", s.key)
            o.put("value", s.value)
            settingsArr.put(o)
        }
        root.put("appSettings", settingsArr)

        // 11. Custom Alarms
        val alarmsArr = JSONArray()
        database.customAlarmDao().getAllAlarmsSync().forEach { a ->
            val o = JSONObject()
            o.put("id", a.id)
            o.put("title", a.title)
            o.put("timeMinutes", a.timeMinutes)
            o.put("repeatDays", a.repeatDays)
            o.put("isEnabled", a.isEnabled)
            o.put("vibrateEnabled", a.vibrateEnabled)
            if (a.customAudioUri != null) o.put("customAudioUri", a.customAudioUri)
            if (a.customAudioTitle != null) o.put("customAudioTitle", a.customAudioTitle)
            o.put("crescendoDurationSeconds", a.crescendoDurationSeconds)
            o.put("isLooping", a.isLooping)
            if (a.wallpaperUri != null) o.put("wallpaperUri", a.wallpaperUri)
            o.put("wallpaperPresetColor", a.wallpaperPresetColor)
            o.put("timeClockFont", a.timeClockFont)
            o.put("missionType", a.missionType)
            if (a.targetBarcode != null) o.put("targetBarcode", a.targetBarcode)
            if (a.targetBarcodeLabel != null) o.put("targetBarcodeLabel", a.targetBarcodeLabel)
            if (a.targetBarcodeFormat != null) o.put("targetBarcodeFormat", a.targetBarcodeFormat)
            if (a.targetBarcodeLocation != null) o.put("targetBarcodeLocation", a.targetBarcodeLocation)
            if (a.registeredCodeId != null) o.put("registeredCodeId", a.registeredCodeId)
            o.put("chainedBackupAlarmEnabled", a.chainedBackupAlarmEnabled)
            o.put("createdAt", a.createdAt)
            alarmsArr.put(o)
        }
        root.put("customAlarms", alarmsArr)

        // 12. Registered Codes
        val codesArr = JSONArray()
        database.registeredCodeDao().getAllRegisteredCodesSync().forEach { c ->
            val o = JSONObject()
            o.put("id", c.id)
            o.put("codeValue", c.codeValue)
            o.put("codeFormat", c.codeFormat)
            o.put("label", c.label)
            if (c.locationHint != null) o.put("locationHint", c.locationHint)
            o.put("isCustomGenerated", c.isCustomGenerated)
            o.put("designTemplate", c.designTemplate)
            if (c.designConfigJson != null) o.put("designConfigJson", c.designConfigJson)
            o.put("createdAt", c.createdAt)
            if (c.lastScannedAt != null) o.put("lastScannedAt", c.lastScannedAt)
            codesArr.put(o)
        }
        root.put("registeredCodes", codesArr)

        val plainJson = root.toString(2)
        return if (passphrase.isNullOrBlank()) {
            plainJson
        } else {
            encryptPayload(plainJson, passphrase.toCharArray())
        }
    }

    fun validateBackup(jsonString: String, passphrase: String? = null): Result<BackupSummary> {
        val payload = if (isEncryptedBackup(jsonString)) {
            if (passphrase.isNullOrBlank()) {
                return Result.failure(
                    IllegalArgumentException("This backup is encrypted with a password. Please provide the password to restore.")
                )
            }
            try {
                decryptPayload(jsonString, passphrase.toCharArray())
            } catch (e: Exception) {
                return Result.failure(
                    IllegalArgumentException("Incorrect password or corrupted backup file.")
                )
            }
        } else {
            jsonString
        }

        return try {
            val root = JSONObject(payload)
            if (!root.has("app") || root.getString("app") != "Rhythm & Focus") {
                return Result.failure(IllegalArgumentException("Invalid backup file: App identifier missing or mismatched"))
            }

            val schemaVersion = root.optInt("schemaVersion", 1)
            if (schemaVersion !in MIN_SUPPORTED_BACKUP_SCHEMA_VERSION..MAX_SUPPORTED_BACKUP_SCHEMA_VERSION) {
                return Result.failure(
                    IllegalArgumentException(
                        "Unsupported backup schema version: $schemaVersion. Supported versions are $MIN_SUPPORTED_BACKUP_SCHEMA_VERSION to $MAX_SUPPORTED_BACKUP_SCHEMA_VERSION."
                    )
                )
            }

            val summary = BackupSummary(
                exportTimestamp = root.optLong("exportTimestamp", System.currentTimeMillis()),
                routineEntriesCount = root.optJSONArray("routineEntries")?.length() ?: 0,
                sleepCount = root.optJSONArray("sleepEntries")?.length() ?: 0,
                healthCount = root.optJSONArray("healthCheckins")?.length() ?: 0,
                waterCount = root.optJSONArray("waterEntries")?.length() ?: 0,
                medicationsCount = root.optJSONArray("medications")?.length() ?: 0,
                intakesCount = root.optJSONArray("medicationIntakes")?.length() ?: 0,
                studyTasksCount = root.optJSONArray("studyTasks")?.length() ?: 0,
                studySessionsCount = root.optJSONArray("studySessions")?.length() ?: 0,
                brainDumpCount = root.optJSONArray("brainDumpNotes")?.length() ?: 0,
                remindersCount = root.optJSONArray("reminders")?.length() ?: 0,
                meditationSessionsCount = root.optJSONArray("meditationSessions")?.length() ?: 0,
                customAlarmsCount = root.optJSONArray("customAlarms")?.length() ?: 0,
                registeredCodesCount = root.optJSONArray("registeredCodes")?.length() ?: 0
            )
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(
        database: AppDatabase,
        jsonString: String,
        remapAutoIds: Boolean = false,
        passphrase: String? = null
    ): Result<BackupSummary> {
        val payload = if (isEncryptedBackup(jsonString)) {
            if (passphrase.isNullOrBlank()) {
                return Result.failure(
                    IllegalArgumentException("This backup is encrypted with a password. Please provide the password to restore.")
                )
            }
            try {
                decryptPayload(jsonString, passphrase.toCharArray())
            } catch (e: Exception) {
                return Result.failure(
                    IllegalArgumentException("Incorrect password or corrupted backup file.")
                )
            }
        } else {
            jsonString
        }

        val validation = validateBackup(payload)
        if (validation.isFailure) return validation

        return try {
            val root = JSONObject(payload)

            // Parse Routine Anchors
            val anchorsList = mutableListOf<RoutineAnchorEntity>()
            root.optJSONArray("routineAnchors")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    anchorsList.add(
                        RoutineAnchorEntity(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            description = o.optString("description", ""),
                            defaultTargetMinutes = o.getInt("defaultTargetMinutes"),
                            sortOrder = o.optInt("sortOrder", i + 1),
                            isEnabled = o.optBoolean("isEnabled", true)
                        )
                    )
                }
            }

            // Parse Routine Entries
            val entriesList = mutableListOf<RoutineEntryEntity>()
            root.optJSONArray("routineEntries")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    entriesList.add(
                        RoutineEntryEntity(
                            id = o.optLong("id", 0L),
                            date = o.getString("date"),
                            anchorId = o.getString("anchorId"),
                            targetTimeMinutes = o.getInt("targetTimeMinutes"),
                            actualTimeMinutes = if (o.has("actualTimeMinutes") && !o.isNull("actualTimeMinutes")) o.getInt("actualTimeMinutes") else null,
                            status = o.getString("status"),
                            notes = o.optNullableString("notes"),
                            disruptionNote = o.optNullableString("disruptionNote"),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse Sleep
            val sleepList = mutableListOf<SleepEntryEntity>()
            root.optJSONArray("sleepEntries")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    sleepList.add(
                        SleepEntryEntity(
                            id = o.optLong("id", 0L),
                            date = o.getString("date"),
                            bedtimeMinutes = o.getInt("bedtimeMinutes"),
                            wakeTimeMinutes = o.getInt("wakeTimeMinutes"),
                            durationMinutes = o.getInt("durationMinutes"),
                            qualityRating = o.getInt("qualityRating"),
                            notes = o.optNullableString("notes"),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse Health
            val healthList = mutableListOf<HealthCheckinEntity>()
            root.optJSONArray("healthCheckins")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    healthList.add(
                        HealthCheckinEntity(
                            id = o.optLong("id", 0L),
                            date = o.getString("date"),
                            mood = if (o.has("mood") && !o.isNull("mood")) o.getInt("mood") else null,
                            energy = if (o.has("energy") && !o.isNull("energy")) o.getInt("energy") else null,
                            stress = if (o.has("stress") && !o.isNull("stress")) o.getInt("stress") else null,
                            irritability = if (o.has("irritability") && !o.isNull("irritability")) o.getInt("irritability") else null,
                            activationEnergy = if (o.has("activationEnergy") && !o.isNull("activationEnergy")) o.getInt("activationEnergy") else null,
                            symptomsNotes = o.optNullableString("symptomsNotes"),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse Water
            val waterList = mutableListOf<WaterEntryEntity>()
            root.optJSONArray("waterEntries")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    waterList.add(
                        WaterEntryEntity(
                            id = o.optLong("id", 0L),
                            date = o.getString("date"),
                            volumeMl = o.getInt("volumeMl"),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse Medications
            val medsList = mutableListOf<MedicationEntity>()
            root.optJSONArray("medications")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    medsList.add(
                        MedicationEntity(
                            id = o.optLong("id", 0L),
                            name = o.getString("name"),
                            dosage = o.getString("dosage"),
                            unit = o.getString("unit"),
                            instructions = o.optString("instructions", ""),
                            scheduledTimeMinutes = o.getInt("scheduledTimeMinutes"),
                            reminderEnabled = o.optBoolean("reminderEnabled", false),
                            isActive = o.optBoolean("isActive", true),
                            notes = o.optNullableString("notes"),
                            dosageForm = o.optNullableString("dosageForm"),
                            scheduledTimesJson = o.optString("scheduledTimesJson", ""),
                            repeatDays = o.optString("repeatDays", "DAILY"),
                            startDate = o.optString("startDate", ""),
                            endDate = o.optNullableString("endDate"),
                            isArchived = o.optBoolean("isArchived", false)
                        )
                    )
                }
            }

            // Parse Intakes
            val intakesList = mutableListOf<MedicationIntakeEntity>()
            root.optJSONArray("medicationIntakes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    intakesList.add(
                        MedicationIntakeEntity(
                            id = o.optLong("id", 0L),
                            date = o.getString("date"),
                            medicationId = o.getLong("medicationId"),
                            scheduledTimeMinutes = o.getInt("scheduledTimeMinutes"),
                            actualActionTime = if (o.has("actualActionTime") && !o.isNull("actualActionTime")) o.getLong("actualActionTime") else null,
                            status = o.getString("status"),
                            notes = o.optNullableString("notes"),
                            medicationName = o.optString("medicationName", ""),
                            dosageSnapshot = o.optString("dosageSnapshot", ""),
                            isCorrected = o.optBoolean("isCorrected", false),
                            correctedAt = if (o.has("correctedAt") && !o.isNull("correctedAt")) o.getLong("correctedAt") else null
                        )
                    )
                }
            }

            // Parse Categories
            val catList = mutableListOf<StudyCategoryEntity>()
            root.optJSONArray("studyCategories")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    catList.add(
                        StudyCategoryEntity(
                            id = o.optLong("id", 0L),
                            name = o.getString("name"),
                            description = o.optNullableString("description"),
                            colorHex = o.optString("colorHex", "#4D6B53"),
                            iconName = o.optString("iconName", "category"),
                            sortOrder = o.optInt("sortOrder", i + 1),
                            isArchived = o.optBoolean("isArchived", false)
                        )
                    )
                }
            }

            // Parse Tasks
            val tasksList = mutableListOf<StudyTaskEntity>()
            root.optJSONArray("studyTasks")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    tasksList.add(
                        StudyTaskEntity(
                            id = o.optLong("id", 0L),
                            title = o.getString("title"),
                            category = o.getString("category"),
                            status = o.optString("status", "NOT_STARTED"),
                            isDailyPriority = o.optBoolean("isDailyPriority", false),
                            estimatedMinutes = o.optInt("estimatedMinutes", 30),
                            dateCreated = o.getString("dateCreated"),
                            dateCompleted = o.optNullableString("dateCompleted"),
                            nextSmallStep = o.optNullableString("nextSmallStep"),
                            sortOrder = o.optInt("sortOrder", 0)
                        )
                    )
                }
            }

            // Parse Sessions
            val sessionsList = mutableListOf<StudySessionEntity>()
            root.optJSONArray("studySessions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    sessionsList.add(
                        StudySessionEntity(
                            id = o.optLong("id", 0L),
                            taskId = if (o.has("taskId") && !o.isNull("taskId")) o.getLong("taskId") else null,
                            taskTitle = o.getString("taskTitle"),
                            category = o.getString("category"),
                            durationMinutes = o.getInt("durationMinutes"),
                            sessionType = o.getString("sessionType"),
                            date = o.getString("date"),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                            startTimeMillis = if (o.has("startTimeMillis") && !o.isNull("startTimeMillis")) o.getLong("startTimeMillis") else null,
                            endTimeMillis = if (o.has("endTimeMillis") && !o.isNull("endTimeMillis")) o.getLong("endTimeMillis") else null,
                            completionStatus = o.optString("completionStatus", "COMPLETED"),
                            learnedNotes = o.optNullableString("learnedNotes")
                        )
                    )
                }
            }

            // Parse Brain Dump Notes
            val notesList = mutableListOf<BrainDumpNoteEntity>()
            root.optJSONArray("brainDumpNotes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    notesList.add(
                        BrainDumpNoteEntity(
                            id = o.optLong("id", 0L),
                            title = o.optNullableString("title"),
                            content = o.getString("content"),
                            tag = o.optNullableString("tag"),
                            isPinned = o.optBoolean("isPinned", false),
                            isArchived = o.optBoolean("isArchived", false),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse Reminders
            val remindersList = mutableListOf<ReminderEntity>()
            root.optJSONArray("reminders")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    remindersList.add(
                        ReminderEntity(
                            id = o.optLong("id", 0L),
                            title = o.getString("title"),
                            category = o.getString("category"),
                            timeMinutes = o.getInt("timeMinutes"),
                            repeatDays = o.optString("repeatDays", "DAILY"),
                            isEnabled = o.optBoolean("isEnabled", true),
                            soundEnabled = o.optBoolean("soundEnabled", true),
                            vibrateEnabled = o.optBoolean("vibrateEnabled", true),
                            isUrgent = o.optBoolean("isUrgent", false),
                            voiceAnnouncementEnabled = o.optBoolean("voiceAnnouncementEnabled", false),
                            spokenAnnouncementText = o.optNullableString("spokenAnnouncementText"),
                            customAudioUri = o.optNullableString("customAudioUri"),
                            customAudioTitle = o.optNullableString("customAudioTitle"),
                            customAudioCollectionJson = o.optNullableString("customAudioCollectionJson"),
                            audioPlaybackMode = o.optString("audioPlaybackMode", "DEFAULT"),
                            lastPlayedAudioUri = o.optNullableString("lastPlayedAudioUri"),
                            referenceId = o.optNullableString("referenceId"),
                            customMessage = o.optNullableString("customMessage"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse Meditation Sessions
            val meditationList = mutableListOf<com.example.data.entity.MeditationSessionEntity>()
            root.optJSONArray("meditationSessions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    meditationList.add(
                        com.example.data.entity.MeditationSessionEntity(
                            id = o.optLong("id", 0L),
                            techniqueId = o.getString("techniqueId"),
                            title = o.getString("title"),
                            durationMinutes = o.getInt("durationMinutes"),
                            completedAt = o.optLong("completedAt", System.currentTimeMillis()),
                            reflectionNotes = o.optNullableString("reflectionNotes"),
                            status = o.optString("status", "COMPLETED")
                        )
                    )
                }
            }

            // Parse Settings
            val settingsList = mutableListOf<AppSettingEntity>()
            root.optJSONArray("appSettings")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    settingsList.add(
                        AppSettingEntity(
                            key = o.getString("key"),
                            value = o.getString("value")
                        )
                    )
                }
            }

            // Parse Custom Alarms
            val alarmsList = mutableListOf<com.example.data.entity.CustomAlarmEntity>()
            root.optJSONArray("customAlarms")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    alarmsList.add(
                        com.example.data.entity.CustomAlarmEntity(
                            id = o.optLong("id", 0L),
                            title = o.getString("title"),
                            timeMinutes = o.getInt("timeMinutes"),
                            repeatDays = o.optString("repeatDays", "WEEKDAYS"),
                            isEnabled = o.optBoolean("isEnabled", true),
                            vibrateEnabled = o.optBoolean("vibrateEnabled", true),
                            customAudioUri = if (o.has("customAudioUri") && !o.isNull("customAudioUri")) o.getString("customAudioUri") else null,
                            customAudioTitle = if (o.has("customAudioTitle") && !o.isNull("customAudioTitle")) o.getString("customAudioTitle") else "Default Alarm Tone",
                            crescendoDurationSeconds = o.optInt("crescendoDurationSeconds", 60),
                            isLooping = o.optBoolean("isLooping", true),
                            wallpaperUri = if (o.has("wallpaperUri") && !o.isNull("wallpaperUri")) o.getString("wallpaperUri") else null,
                            wallpaperPresetColor = o.optString("wallpaperPresetColor", "MIDNIGHT_INDIGO"),
                            timeClockFont = o.optString("timeClockFont", "MODERN_DIGITAL"),
                            missionType = o.optString("missionType", "NONE"),
                            targetBarcode = if (o.has("targetBarcode") && !o.isNull("targetBarcode")) o.getString("targetBarcode") else null,
                            targetBarcodeLabel = if (o.has("targetBarcodeLabel") && !o.isNull("targetBarcodeLabel")) o.getString("targetBarcodeLabel") else null,
                            targetBarcodeFormat = if (o.has("targetBarcodeFormat") && !o.isNull("targetBarcodeFormat")) o.getString("targetBarcodeFormat") else null,
                            targetBarcodeLocation = if (o.has("targetBarcodeLocation") && !o.isNull("targetBarcodeLocation")) o.getString("targetBarcodeLocation") else null,
                            registeredCodeId = if (o.has("registeredCodeId") && !o.isNull("registeredCodeId")) o.getLong("registeredCodeId") else null,
                            chainedBackupAlarmEnabled = o.optBoolean("chainedBackupAlarmEnabled", true),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse Registered Codes
            val codesList = mutableListOf<com.example.data.entity.RegisteredCodeEntity>()
            root.optJSONArray("registeredCodes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    codesList.add(
                        com.example.data.entity.RegisteredCodeEntity(
                            id = o.optLong("id", 0L),
                            codeValue = o.getString("codeValue"),
                            codeFormat = o.optString("codeFormat", "QR_CODE"),
                            label = o.getString("label"),
                            locationHint = if (o.has("locationHint") && !o.isNull("locationHint")) o.getString("locationHint") else null,
                            isCustomGenerated = o.optBoolean("isCustomGenerated", false),
                            designTemplate = o.optString("designTemplate", "MINIMAL_BW"),
                            designConfigJson = if (o.has("designConfigJson") && !o.isNull("designConfigJson")) o.getString("designConfigJson") else null,
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            lastScannedAt = if (o.has("lastScannedAt") && !o.isNull("lastScannedAt")) o.getLong("lastScannedAt") else null
                        )
                    )
                }
            }

            // Atomic database execution within transaction
            database.withTransaction {
                if (!remapAutoIds) {
                    if (anchorsList.isNotEmpty()) database.routineDao().insertAnchorsList(anchorsList)
                    if (entriesList.isNotEmpty()) database.routineDao().insertEntriesList(entriesList)
                    if (sleepList.isNotEmpty()) database.sleepDao().insertSleepList(sleepList)
                    if (healthList.isNotEmpty()) database.healthDao().insertHealthList(healthList)
                    if (waterList.isNotEmpty()) database.waterDao().insertWaterList(waterList)
                    if (medsList.isNotEmpty()) database.medicationDao().insertMedicationsList(medsList)
                    if (intakesList.isNotEmpty()) database.medicationDao().insertIntakesList(intakesList)
                    if (catList.isNotEmpty()) database.studyCategoryDao().insertCategoriesList(catList)
                    if (tasksList.isNotEmpty()) database.studyDao().insertTasksList(tasksList)
                    if (sessionsList.isNotEmpty()) database.studyDao().insertSessionsList(sessionsList)
                    if (notesList.isNotEmpty()) database.brainDumpDao().insertNotesList(notesList)
                    if (remindersList.isNotEmpty()) database.reminderDao().insertRemindersList(remindersList)
                    if (meditationList.isNotEmpty()) database.meditationDao().insertSessionsList(meditationList)
                    if (alarmsList.isNotEmpty()) database.customAlarmDao().insertAlarmsList(alarmsList)
                    if (codesList.isNotEmpty()) database.registeredCodeDao().insertCodesList(codesList)
                    if (settingsList.isNotEmpty()) database.settingsDao().insertSettingsList(settingsList)
                } else {
                    // Remap auto-generated IDs to 0L so SQLite generates fresh primary keys without overwriting existing records
                    if (anchorsList.isNotEmpty()) database.routineDao().insertAnchorsList(anchorsList)
                    if (catList.isNotEmpty()) database.studyCategoryDao().insertCategoriesList(catList)
                    if (settingsList.isNotEmpty()) database.settingsDao().insertSettingsList(settingsList)

                    if (entriesList.isNotEmpty()) {
                        database.routineDao().insertEntriesList(entriesList.map { it.copy(id = 0L) })
                    }
                    if (sleepList.isNotEmpty()) {
                        database.sleepDao().insertSleepList(sleepList.map { it.copy(id = 0L) })
                    }
                    if (healthList.isNotEmpty()) {
                        database.healthDao().insertHealthList(healthList.map { it.copy(id = 0L) })
                    }
                    if (waterList.isNotEmpty()) {
                        database.waterDao().insertWaterList(waterList.map { it.copy(id = 0L) })
                    }

                    // Relational mapping for medications and medication intakes/reminders
                    val medIdMap = mutableMapOf<Long, Long>()
                    for (med in medsList) {
                        val originalId = med.id
                        val newId = database.medicationDao().insertMedication(med.copy(id = 0L))
                        medIdMap[originalId] = newId
                    }

                    if (intakesList.isNotEmpty()) {
                        val remappedIntakes = intakesList.map { intake ->
                            val remappedMedId = medIdMap[intake.medicationId] ?: intake.medicationId
                            intake.copy(id = 0L, medicationId = remappedMedId)
                        }
                        database.medicationDao().insertIntakesList(remappedIntakes)
                    }

                    // Relational mapping for study tasks and study sessions
                    val taskIdMap = mutableMapOf<Long, Long>()
                    for (task in tasksList) {
                        val originalTaskId = task.id
                        val newTaskId = database.studyDao().insertTask(task.copy(id = 0L))
                        taskIdMap[originalTaskId] = newTaskId
                    }

                    if (sessionsList.isNotEmpty()) {
                        val remappedSessions = sessionsList.map { session ->
                            val remappedTaskId = session.taskId?.let { taskIdMap[it] ?: it }
                            session.copy(id = 0L, taskId = remappedTaskId)
                        }
                        database.studyDao().insertSessionsList(remappedSessions)
                    }

                    if (notesList.isNotEmpty()) {
                        database.brainDumpDao().insertNotesList(notesList.map { it.copy(id = 0L) })
                    }

                    if (remindersList.isNotEmpty()) {
                        val remappedReminders = remindersList.map { reminder ->
                            val refId = reminder.referenceId
                            val updatedRefId = if (refId != null && refId.startsWith("med:")) {
                                val parts = refId.removePrefix("med:").split("_")
                                val oldMedId = parts[0].toLongOrNull()
                                if (oldMedId != null && medIdMap.containsKey(oldMedId)) {
                                    val newMedId = medIdMap[oldMedId]!!
                                    val suffix = if (parts.size > 1) "_${parts.subList(1, parts.size).joinToString("_")}" else ""
                                    "med:$newMedId$suffix"
                                } else refId
                            } else refId

                            reminder.copy(id = 0L, referenceId = updatedRefId)
                        }
                        database.reminderDao().insertRemindersList(remappedReminders)
                    }

                    if (meditationList.isNotEmpty()) {
                        database.meditationDao().insertSessionsList(meditationList.map { it.copy(id = 0L) })
                    }
                    if (alarmsList.isNotEmpty()) {
                        database.customAlarmDao().insertAlarmsList(alarmsList.map { it.copy(id = 0L) })
                    }
                    if (codesList.isNotEmpty()) {
                        database.registeredCodeDao().insertCodesList(codesList.map { it.copy(id = 0L) })
                    }
                }

                // Re-ensure basic defaults if list was empty
                database.routineDao().insertDefaultAnchors(AppDatabase.DEFAULT_ANCHORS)
                database.studyCategoryDao().insertDefaultCategories(AppDatabase.DEFAULT_CATEGORIES)
                database.reminderDao().insertDefaultReminders(AppDatabase.DEFAULT_REMINDERS)
            }

            Result.success(validation.getOrThrow())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDatabaseIntegrityStats(database: AppDatabase): DatabaseIntegrityStats {
        val totalRoutines = database.routineDao().getEntriesCount()
        val totalSleep = database.sleepDao().getSleepCount()
        val totalHealth = database.healthDao().getHealthCount()
        val totalWater = database.waterDao().getWaterCount()
        val totalMedications = database.medicationDao().getMedicationsCount()
        val totalIntakes = database.medicationDao().getIntakesCount()
        val totalCategories = database.studyCategoryDao().getCategoriesCount()
        val totalTasks = database.studyDao().getTasksCount()
        val totalSessions = database.studyDao().getSessionsCount()
        val totalBrainDump = database.brainDumpDao().getNotesCount()
        val totalReminders = database.reminderDao().getRemindersCount()
        val enabledReminders = database.reminderDao().getEnabledRemindersCount()
        val totalMeditationSessions = database.meditationDao().getSessionsCount()
        val totalCustomAlarms = database.customAlarmDao().getAlarmsCount()
        val totalRegisteredCodes = database.registeredCodeDao().getCodesCount()

        // Integrity check: Categories > 0, Anchors > 0
        val isConsistent = totalCategories > 0 && AppDatabase.DEFAULT_ANCHORS.isNotEmpty()

        val isEncrypted = AppDatabase.appContext?.let { ctx ->
            val dbFile = ctx.getDatabasePath("rhythm_focus_database")
            dbFile.exists() && !com.example.data.security.DatabaseKeyManager.isPlaintextSqliteDatabase(dbFile)
        } ?: false

        return DatabaseIntegrityStats(
            totalRoutines = totalRoutines,
            totalSleep = totalSleep,
            totalHealth = totalHealth,
            totalWater = totalWater,
            totalMedications = totalMedications,
            totalIntakes = totalIntakes,
            totalCategories = totalCategories,
            totalTasks = totalTasks,
            totalSessions = totalSessions,
            totalBrainDump = totalBrainDump,
            totalReminders = totalReminders,
            enabledReminders = enabledReminders,
            totalMeditationSessions = totalMeditationSessions,
            totalCustomAlarms = totalCustomAlarms,
            totalRegisteredCodes = totalRegisteredCodes,
            isConsistent = isConsistent,
            isEncrypted = isEncrypted
        )
    }
}
