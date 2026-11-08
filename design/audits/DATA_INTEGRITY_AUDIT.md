# Rhythm — Stage 4: Room Database & Data Integrity Audit

**Status:** Completed  
**Audit Date:** November 2026  
**Audited Scope:** Room Database (Version 11), 20 Entities, 12 DAOs, Schema Migrations, Backup/Restore, Data Lifecycles, and Health Connect Mapping  
**Target Package:** `com.jayasuriya.rhythm` (`com.example.*`)  
**Evaluation Principle:** Evidence-based audit strictly grounded in actual source code and schema definitions.

---

## 1. Executive Summary

Rhythm uses a local Room database (`rhythm_focus_database`, currently at schema **version 11**) to store sensitive user data including medication schedules, intake compliance, sleep entries, psychiatric/mood check-ins, study session history, thought notes, dedicated custom alarms, and Google Health Connect vitals.

While the data layer has mature domain models and clean DAO query definitions, the audit identified **four critical data integrity vulnerabilities**:
1. **Zero Room Transactions (`@Transaction` / `withTransaction`) across all multi-step database mutations**, leaving operations susceptible to database corruption or partial state if interrupted.
2. **Absence of Foreign Key Constraints and Cascading Actions**, leading to orphaned records (e.g. deleting a medication leaves active ringing reminders and orphaned intake records).
3. **Room Schema Export Disabled (`exportSchema = false`) with Zero Automated Migration Tests**, meaning historical schema migrations (v1 through v11) have never been validated by automated tooling.
4. **Backup and Restore Flaws in `DataBackupManager.kt`**, including a hardcoded mismatched schema version (`5` vs Room `11`), non-atomic restore insertion, ID collision risks, and complete omission of Health Connect tables.

---

## 2. Complete Room Entity Inventory

The database declares **20 entities** across two packages:
- `com.example.data.entity.Entities.kt` (17 entities)
- `com.example.data.entity.HealthConnectEntities.kt` (3 entities)

| # | Entity Name | Table Name | Primary Key | Key Indices & Constraints | Description |
|---|---|---|---|---|---|
| 1 | `RoutineAnchorEntity` | `routine_anchors` | `id: String` | None | Social rhythm circadian anchors (wake up, dinner, into bed). |
| 2 | `RoutineEntryEntity` | `routine_entries` | `id: Long` (Auto) | Unique: `(date, anchorId)` | Daily check-in timestamps against anchors. |
| 3 | `SleepEntryEntity` | `sleep_entries` | `id: Long` (Auto) | Unique: `(date)` | Self-reported sleep duration and quality rating. |
| 4 | `HealthCheckinEntity` | `health_checkins` | `id: Long` (Auto) | Unique: `(date)` | Self-reported mood, energy, stress, and irritability (1–5). |
| 5 | `WaterEntryEntity` | `water_entries` | `id: Long` (Auto) | None | Hydration intake logs with volume in mL. |
| 6 | `MedicationEntity` | `medications` | `id: Long` (Auto) | None | Prescribed medications, dosage, schedule JSON, and recurrence. |
| 7 | `MedicationIntakeEntity` | `medication_intakes` | `id: Long` (Auto) | Unique: `(date, medicationId, scheduledTimeMinutes)` | Intake compliance records ("TAKEN", "SKIPPED", "DELAYED"). |
| 8 | `StudyCategoryEntity` | `study_categories` | `id: Long` (Auto) | None | Categories for study tasks (DSA, JavaScript, Backend). |
| 9 | `StudyTaskEntity` | `study_tasks` | `id: Long` (Auto) | None | Study tasks, daily priorities, and micro-actions. |
| 10 | `StudySessionEntity` | `study_sessions` | `id: Long` (Auto) | None | Completed focus sessions with timestamps and notes. |
| 11 | `ActiveTimerDraftEntity` | `active_timer_draft` | `id: Int` (Fixed = 1) | None | Singleton record preserving active timer across process restarts. |
| 12 | `BrainDumpNoteEntity` | `brain_dump_notes` | `id: Long` (Auto) | None | Captured thoughts, ideas, tags, and pin/archive status. |
| 13 | `AppSettingEntity` | `app_settings` | `key: String` | None | Key-value store for app settings, theme, audio, and sync state. |
| 14 | `ReminderEntity` | `reminders` | `id: Long` (Auto) | None | Scheduled notification reminders with audio, TTS, and repetition. |
| 15 | `MeditationSessionEntity` | `meditation_sessions` | `id: Long` (Auto) | None | Completed guided breathing and mindfulness sessions. |
| 16 | `CustomAlarmEntity` | `custom_alarms` | `id: Long` (Auto) | None | Full-screen dedicated alarms with barcode mission failsafes. |
| 17 | `RegisteredCodeEntity` | `registered_codes` | `id: Long` (Auto) | Index: `(codeValue)` | Registered QR codes and barcodes used for alarm dismissal. |
| 18 | `HealthConnectDailySummaryEntity` | `health_connect_daily_summaries` | `date: String` | Unique: `(date)` | Persisted daily summary of verified Health Connect metrics. |
| 19 | `HealthConnectSleepStageEntity` | `health_connect_sleep_stages` | `id: Long` (Auto) | Index: `(date)`, Index: `(stageStartTime, stageEndTime)` | Real wearable hypnogram stages (Deep, Light, REM, Awake). |
| 20 | `HealthConnectHeartSampleEntity` | `health_connect_heart_samples` | `id: Long` (Auto) | Index: `(date)`, Index: `(timestampEpoch)` | High-frequency heart rate series samples imported from sensors. |

---

## 3. Entity Relationship Diagram (ERD)

```
       +-----------------------+                    +------------------------+
       |  RoutineAnchorEntity  |                    |  StudyCategoryEntity   |
       +-----------------------+                    +------------------------+
       | PK id: String         |                    | PK id: Long            |
       |    name: String       |                    |    name: String (Soft) |
       +-----------+-----------+                    +-----------+------------+
                   |                                            |
                   | (anchorId soft ref)                        | (category soft ref)
                   v                                            v
       +-----------------------+                    +------------------------+
       |  RoutineEntryEntity   |                    |    StudyTaskEntity     |
       +-----------------------+                    +------------------------+
       | PK id: Long           |                    | PK id: Long            |
       |    anchorId: String   |                    |    category: String    |
       |    date: String       |                    +-----------+------------+
       +-----------------------+                                |
                                                                | (taskId soft ref)
       +-----------------------+                                v
       |   MedicationEntity    |                    +------------------------+
       +-----------------------+                    |   StudySessionEntity   |
       | PK id: Long           |                    +------------------------+
       |    name: String       |                    | PK id: Long            |
       +-----------+-----------+                    |    taskId: Long?       |
                   |                                |    category: String    |
                   | (medicationId soft ref)        +------------------------+
                   v
       +-----------------------+                    +------------------------+
       | MedicationIntakeEntity|                    |  RegisteredCodeEntity  |
       +-----------------------+                    +------------------------+
       | PK id: Long           |                    | PK id: Long            |
       |    medicationId: Long |                    |    codeValue: String   |
       |    date: String       |                    +-----------+------------+
       +-----------------------+                                |
                                                                | (registeredCodeId)
       +-----------------------+                                v
       |    ReminderEntity     |                    +------------------------+
       +-----------------------+                    |   CustomAlarmEntity    |
       | PK id: Long           |                    +------------------------+
       |    referenceId: String|                    | PK id: Long            |
       +-----------------------+                    |    registeredCodeId:L? |
                                                    |    targetBarcode: Str? |
                                                    +------------------------+
```

---

## 4. CRUD and Transaction Analysis

### Finding DATA-01: Complete Absence of Room Transactions (`@Transaction` / `withTransaction`)
- **Exact File:** `app/src/main/java/com/example/data/repository/AppRepository.kt` (Lines 213–216, 433–475, 478–519, 632–646) and `app/src/main/java/com/example/util/DataBackupManager.kt` (Lines 737–759)
- **Severity:** **CRITICAL**
- **Type:** Verified Defect / ACID Violation
- **Code Evidence:**
  The repository performs complex multi-table mutations without wrapping them in an atomic transaction:
  1. `deleteCategoryAndReassignTasks(categoryId, categoryName, fallbackCategory)`:
     ```kotlin
     studyCategoryDao.reassignTaskCategory(categoryName, fallbackCategory) // UPDATE study_tasks
     studyCategoryDao.deleteCategory(categoryId)                           // DELETE FROM study_categories
     ```
     If an unhandled exception or process termination occurs after line 1, tasks are updated but the old category remains.
  2. `saveHealthConnectData(summary, sleepStages, heartSamples)`:
     ```kotlin
     healthConnectDao.upsertDailySummary(summary)
     if (sleepStages.isNotEmpty()) {
         healthConnectDao.deleteSleepStagesForDate(summary.date) // DELETE
         healthConnectDao.insertSleepStages(sleepStages)        // INSERT
     }
     if (heartSamples.isNotEmpty()) {
         healthConnectDao.deleteHeartSamplesForDate(summary.date) // DELETE
         healthConnectDao.insertHeartSamples(heartSamples)        // INSERT
     }
     ```
     `deleteSleepStagesForDate` runs before `insertSleepStages`. If inserting fails (e.g., disk full, memory constraint), all existing sleep stages for that day are permanently deleted with zero recovery.
  3. `DataBackupManager.restoreBackup(database, jsonString)`:
     Executes 16 consecutive `insert...List` calls without a transaction. If table 10 fails, tables 1–9 have already been inserted into user data without rollback.
- **Impact:**
  - Violates Atomicity and Consistency.
  - Leaves the local SQLite database corrupted or in a broken half-written state on any unexpected crash.

---

### Finding DATA-02: Missing Foreign Key Constraints and Cascade Deletion Rules
- **Exact File:** `app/src/main/java/com/example/data/entity/Entities.kt` (Lines 113–129, 218–239, 312–335)
- **Entities:** `MedicationIntakeEntity`, `ReminderEntity`, `CustomAlarmEntity`
- **Severity:** **HIGH**
- **Type:** Verified Defect / Referential Integrity Failure
- **Code Evidence:**
  1. `MedicationIntakeEntity` defines `val medicationId: Long` without `@ForeignKey`:
     When a medication is deleted via `AppRepository.deleteMedication(id)`:
     ```kotlin
     suspend fun deleteMedication(id: Long) {
         medicationDao.deleteMedication(id)
     }
     ```
     The associated `MedicationIntakeEntity` records remain in the database with an invalid `medicationId`. Although `MedicationIntakeEntity` preserves a `medicationName` snapshot, relational queries joining on `medicationId` produce orphan results.
  2. When a medication is deleted, associated alarms in `reminders` table are **never deleted**:
     Medication reminders are keyed by `referenceId = "med:${med.id}_$index"`.
     When `deleteMedication(id)` is called, the repository **never deletes** reminders matching `"med:$id_%"`.
     As a result, deleted medications continue to trigger daily alarms and notifications indefinitely.
  3. `CustomAlarmEntity` defines `val registeredCodeId: Long?` without `@ForeignKey`:
     Deleting a registered QR code from `registered_codes` table leaves `CustomAlarmEntity.registeredCodeId` dangling, referencing a non-existent code ID.
- **Impact:**
  - Orphaned database records accumulate indefinitely.
  - Deleted medications continue to fire daily audible reminder alarms, confusing users.

---

### Finding DATA-03: Missing Indexes on High-Frequency Query Columns
- **Exact File:** `app/src/main/java/com/example/data/entity/Entities.kt` (Lines 64–70, 142–154, 156–170, 218–238, 301–310, 312–335)
- **Severity:** **MEDIUM**
- **Type:** Performance & Scalability Risk
- **Code Evidence:**
  The following tables lack indexes on columns filtered or sorted frequently in DAO queries:
  - `water_entries`: Filtered by `date` (`SELECT * FROM water_entries WHERE date = :date`) but has no index on `date`.
  - `study_tasks`: Queried by `isDailyPriority` and `category` (`SELECT * FROM study_tasks ORDER BY isDailyPriority DESC`), with no index on `isDailyPriority` or `category`.
  - `study_sessions`: Queried by `date` (`WHERE date = :date`) and `timestamp`, with no index on `date` or `timestamp`.
  - `reminders`: Queried by `referenceId` (`WHERE referenceId = :referenceId LIMIT 1`) and `category`, with no index on `referenceId` or `category`.
  - `meditation_sessions`: Ordered by `completedAt DESC`, with no index on `completedAt`.
  - `custom_alarms`: Ordered by `timeMinutes ASC`, with no index on `timeMinutes`.
- **Impact:**
  - Forces SQLite to perform full table scans (`SCAN TABLE`) on every date change or screen refresh.
  - As user history accumulates over months (e.g. hundreds of water and session logs), query latency increases on the main thread during initial Flow emissions.

---

## 5. Migration History and Safety Assessment

### Current Schema Configuration
- **Database Class:** `com.example.data.database.AppDatabase`
- **Current Version:** `11`
- **Schema Export:** `exportSchema = false` (Line 70)
- **Downgrade Policy:** `fallbackToDestructiveMigrationOnDowngrade()` (Line 366)

### Historical Migrations Sequence
| Migration | From $\rightarrow$ To | DDL Summary | Safety Evaluation |
|---|---|---|---|
| `MIGRATION_1_2` | 1 $\rightarrow$ 2 | Created `study_categories`, `active_timer_draft`. Added columns to `study_sessions`. | Safe (Additive) |
| `MIGRATION_2_3` | 2 $\rightarrow$ 3 | Created `brain_dump_notes`. | Safe (Additive) |
| `MIGRATION_3_4` | 3 $\rightarrow$ 4 | Added dosage and schedule columns to `medications` & `medication_intakes`. | Safe (Additive) |
| `MIGRATION_4_5` | 4 $\rightarrow$ 5 | Created `reminders` table. | Safe (Additive) |
| `MIGRATION_5_6` | 5 $\rightarrow$ 6 | Added voice announcement columns to `reminders`. Created `meditation_sessions`. | Safe (Additive) |
| `MIGRATION_6_7` | 6 $\rightarrow$ 7 | Added custom audio URI & audio collection columns to `reminders`. | Safe (Additive) |
| `MIGRATION_7_8` | 7 $\rightarrow$ 8 | Created `custom_alarms` table. | Safe (Additive) |
| `MIGRATION_8_9` | 8 $\rightarrow$ 9 | Added protocol & micro-break columns to `active_timer_draft`. | Safe (Additive) |
| `MIGRATION_9_10` | 9 $\rightarrow$ 10 | Created `registered_codes` table. Added barcode format/location columns to `custom_alarms`. | Safe (Additive) |
| `MIGRATION_10_11` | 10 $\rightarrow$ 11 | Created 3 Health Connect tables (`health_connect_daily_summaries`, `health_connect_sleep_stages`, `health_connect_heart_samples`). | Safe (Additive) |

### Migration Vulnerability Analysis
- **Critical Risk — `exportSchema = false`:** Room requires `exportSchema = true` to save JSON schema definitions to version control. Because schema export is disabled:
  1. Room cannot automatically generate schema diffs.
  2. The project cannot run `androidx.room.testing.MigrationTestHelper` in unit tests to verify that upgrading from v1 to v11 preserves SQLite data.
  3. If a typo exists in any `CREATE TABLE` DDL statement, Room will throw `IllegalStateException: Room cannot verify data integrity` upon startup, crashing the app on the user's device.
- **Safety Rule Compliance:** The application uses `.fallbackToDestructiveMigrationOnDowngrade()`. It does **not** call `fallbackToDestructiveMigration()`, which is good practice as it prevents accidental data destruction upon forward version upgrades.

---

## 6. Backup and Restore Analysis (`DataBackupManager.kt`)

### Finding DATA-04: Mismatched Backup Schema Version
- **Exact File:** `app/src/main/java/com/example/util/DataBackupManager.kt` (Line 63)
- **Severity:** **HIGH**
- **Type:** Verified Defect / Compatibility Risk
- **Code Evidence:**
  ```kotlin
  suspend fun exportBackup(database: AppDatabase): String {
      val root = JSONObject()
      root.put("app", "Rhythm & Focus")
      root.put("version", 1)
      root.put("schemaVersion", 5) // HARDCODED 5! Room is at version 11
      root.put("exportTimestamp", System.currentTimeMillis())
  ```
- **Impact:**
  - The backup JSON declares `schemaVersion: 5` despite containing v11 tables (`customAlarms`, `registeredCodes`, `meditationSessions`).
  - If a restore parser verifies `schemaVersion`, it encounters an inconsistency between the declared version and the actual entity schemas.

---

### Finding DATA-05: Non-Atomic Restore and ID Collision Overwrite
- **Exact File:** `app/src/main/java/com/example/util/DataBackupManager.kt` (Lines 737–754)
- **Severity:** **HIGH**
- **Type:** Verified Defect / Silent Data Overwrite
- **Code Evidence:**
  All DAO insert methods used during restore use `OnConflictStrategy.REPLACE` with pre-existing primary keys:
  ```kotlin
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTasksList(tasks: List<StudyTaskEntity>)
  ```
  If a user has 10 tasks on Device A (IDs 1–10) and restores a backup from Device B (which also had IDs 1–10 for completely different tasks), Device A's tasks are **silently overwritten and deleted without warning**.
- **Impact:**
  - Backup restore is not merge-safe.
  - A partial restore failure leaves the user with mixed data from two states.

---

### Finding DATA-06: Omission of Health Connect & Binary Assets in Backup
- **Exact File:** `app/src/main/java/com/example/util/DataBackupManager.kt` (Lines 66–348)
- **Severity:** **MEDIUM**
- **Type:** Functional Limitation
- **Code Evidence:**
  - `health_connect_daily_summaries`, `health_connect_sleep_stages`, and `health_connect_heart_samples` are completely omitted from `exportBackup()`.
  - For custom wallpaper and audio files, only the string URI (e.g. `content://com.android.providers.media...`) is exported. When restored on a new device or after an OS reinstall, these content URIs become invalid dead links, reverting alarms and focus wallpapers to defaults.

---

## 7. Health Connect Data Mapping Audit

### Implementation Verification
- **Exact File:** `app/src/main/java/com/example/health/HealthConnectManager.kt`
- **Supported Records:**
  - `StepsRecord`, `DistanceRecord`, `TotalCaloriesBurnedRecord`, `ActiveCaloriesBurnedRecord`, `ExerciseSessionRecord`
  - `SleepSessionRecord` (with genuine `SleepStage` hypnograms)
  - `HeartRateRecord` (high-frequency bpm series)
  - `RestingHeartRateRecord`
  - `HeartRateVariabilityRmssdRecord` (HRV)
  - `RespiratoryRateRecord`
  - `OxygenSaturationRecord` (SpO2)
  - `HydrationRecord`
- **Wearable Deduplication Rule:** Grouping by source package (`it.metadata.dataOrigin.packageName`) and taking the maximum source to prevent double-counting phone sensors and smartwatches.
- **Finding HC-01 (Missing Pagination):**
  `client.readRecords(ReadRecordsRequest(...))` does not check `pageToken` in `ReadRecordsResponse`. If a date has more than 1,000 heart rate records, only the first page is read; all subsequent samples are silently ignored.
- **Finding HC-02 (Missing Incremental Changes Token):**
  Health Connect provides `getChangesToken` and `getChanges` for incremental background sync. The app currently queries raw time ranges on every sync, performing redundant reads of unchanged historical records.

---

## 8. Summary of Data Integrity Recommendations
1. **Wrap all repository and restore operations in `db.withTransaction { }`**.
2. **Add cascading delete logic in repository**: When deleting a medication, atomically delete its associated reminders (`referenceId LIKE 'med:%'`).
3. **Enable `exportSchema = true` in `AppDatabase.kt`** and add `MigrationTestHelper` unit tests for all 10 migrations.
4. **Fix `DataBackupManager.kt`**: Update `schemaVersion` to `11`, wrap restore in a single transaction, and sanitize/remap auto-generated IDs upon restore.
5. **Add missing database indexes** on `water_entries(date)`, `study_tasks(isDailyPriority)`, `study_sessions(date)`, and `reminders(referenceId)`.
6. **Implement pagination loop** for Health Connect `readRecords` responses.
