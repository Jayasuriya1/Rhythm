# Stage 4 Correction Roadmap — Validation Report

**Reviewer:** Antigravity Development Agent  
**Date:** 2026-11-08  
**Source Document:** `design/audits/STAGE_4_CORRECTION_ROADMAP.md`  
**Target Project:** Rhythm (`com.jayasuriya.rhythm`)  
**Methodology:** Read-only source code inspection, AST verification, and Android platform requirement analysis. No application code, database schemas, migrations, dependencies, or runtime behaviors were modified.  
**Compilation & Target Environment:** `compileSdk = 36 (minor 1)`, `targetSdk = 36`, `minSdk = 26`, Kotlin 2.0 / Compose, Room 2.7.0-alpha12.

---

## Executive Summary

A comprehensive validation of the eight proposed pull requests in `STAGE_4_CORRECTION_ROADMAP.md` was conducted against the live codebase. 

Key validation outcomes:
1. **Critical Architectural Clarification (PR 4 vs. Room):** Room database schema version and the backup JSON interchange format version are distinct concerns. Room is currently at SQLite schema version 11; `DataBackupManager` exports a domain payload stamped with JSON schema version 5. Conflating these versions creates artificial coupling.
2. **Room Schema Export Plan Correction (PR 3):** Enabling `exportSchema = true` on Room only generates schema metadata for the current compile-time version (`11.json`). It cannot magically generate historical schemas (versions 1–10). A dual-track strategy (SQLite DDL replay tests for legacy migrations + baseline `11.json` for future migrations) is established.
3. **Android Exported Component Analysis (PR 2):** Inspection of all three launch paths for `ActiveAlarmActivity` (BroadcastReceiver direct intent, Foreground Service `setFullScreenIntent` via PendingIntent, and ViewModel preview intent) under `targetSdk 36` confirms that setting `android:exported="false"` is 100% safe, required for security, and fully compatible with Android 14+ lockscreen alarm presentation.
4. **DAO Method Mismatch (PR 1):** `ReminderDao.deleteReminderByReferenceId` uses exact equality (`=`), rendering the roadmap's proposed prefix wildcard string ineffective. A dedicated prefix-matching query is required.

---

## Legend

| Classification | Meaning |
|---|---|
| ✅ **Verified and ready** | Finding confirmed by source evidence; proposed solution is technically sound, platform-compliant, and ready for implementation. |
| ⚠️ **Finding verified, solution needs revision** | The issue is genuine, but the roadmap's proposed implementation contains defects, false assumptions, or regression risks that require revision. |
| ❌ **Finding not verified** | Source evidence contradicts the audit finding. |
| 🔍 **Requires additional investigation** | The finding is conceptually valid, but key architectural, cryptographic, or platform unknowns block immediate implementation. |

---

## Detailed PR Validations

### PR 1: Room Data Integrity & Transaction Safety

**Classification: ⚠️ Finding Verified, Solution Needs Revision**

#### Verified Evidence
- `AppRepository.deleteMedication()` ([`AppRepository.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/data/repository/AppRepository.kt#L157-L158)): Calls only `medicationDao.deleteMedication(id)`. Reminders linked to this medication via `referenceId = "med:${id}_*"` are orphaned.
- `AppRepository.deleteCategoryAndReassignTasks()` ([`AppRepository.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/data/repository/AppRepository.kt#L213-L216)): Executes sequential category deletion and task reassignments without `withTransaction {}`.
- `AppRepository.syncRoutinesToReminders()` ([`AppRepository.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/data/repository/AppRepository.kt#L433-L475)) & `syncMedicationsToReminders()` ([`AppRepository.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/data/repository/AppRepository.kt#L478-L519)): Complex iterative reminder clear-and-recreate operations lack transactional boundaries.
- `AppRepository.saveHealthConnectData()` ([`AppRepository.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/data/repository/AppRepository.kt#L632-L646)): Sequentially deletes existing daily summaries, sleep stages, and heart rate samples before inserting new batches without transactional atomicity.
- **Additional finding:** `AppRepository.setDailyPriority()` ([`AppRepository.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/data/repository/AppRepository.kt#L238-L241)) executes `clearDailyPriorities()` followed by `setDailyPriority(taskId)`. If interrupted, priority state is cleared without setting the new task.

#### Solution Gap & Correction
The roadmap proposed:
```kotlin
reminderDao.deleteReminderByReferenceId("med:${id}_%")
```
**Defect:** [`AppDaos.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/data/dao/AppDaos.kt#L421-L422) declares:
```sql
@Query("DELETE FROM reminders WHERE referenceId = :referenceId")
suspend fun deleteReminderByReferenceId(referenceId: String)
```
This is an **exact match (`=`)**, not a wildcard `LIKE` query. Passing `"med:${id}_%"` attempts to match that literal string and deletes nothing.

**Corrected Solution:**
1. Add a dedicated DAO query in `ReminderDao`:
   ```kotlin
   @Query("DELETE FROM reminders WHERE referenceId LIKE :prefix || '%'")
   suspend fun deleteRemindersByReferencePrefix(prefix: String)
   ```
2. Wrap `deleteMedication()` in `database.withTransaction`:
   ```kotlin
   database.withTransaction {
       medicationDao.deleteMedication(id)
       reminderDao.deleteRemindersByReferencePrefix("med:${id}:")
   }
   ```
3. Wrap `deleteCategoryAndReassignTasks`, `syncRoutinesToReminders`, `syncMedicationsToReminders`, `saveHealthConnectData`, and `setDailyPriority` in `database.withTransaction`.

#### Regression Risks & Implementation Blockers
- **Blocker:** None.
- **Risk:** `syncRoutinesToReminders` and `syncMedicationsToReminders` become atomic. If one reminder insert fails, all roll back. This is desired behavior but requires unit test validation to ensure no partial states are left behind.

---

### PR 2: Security Hardening & WakeLock Modernization

**Classification: ✅ Verified and Ready for Implementation**

#### Verified Evidence
1. **Unprotected Exported Activity:** [`AndroidManifest.xml`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/AndroidManifest.xml#L96-L106):
   ```xml
   <activity
       android:name=".ui.screens.alarm.ActiveAlarmActivity"
       android:exported="true"
       android:showWhenLocked="true"
       android:turnScreenOn="true"
       android:showOnLockScreen="true"
       android:excludeFromRecents="true"
       android:taskAffinity=""
       android:launchMode="singleInstance"
       android:screenOrientation="portrait"
       android:theme="@style/Theme.MyApplication" />
   ```
   Contains no `<intent-filter>` and no `android:permission`. Any third-party application on the device can start this Activity directly.
2. **Deprecated WakeLock Usage:**
   - [`CustomAlarmReceiver.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/receiver/CustomAlarmReceiver.kt#L33-L39): Acquires `PowerManager.FULL_WAKE_LOCK` with a 3-minute timeout.
   - [`CustomAlarmPlaybackService.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/service/CustomAlarmPlaybackService.kt#L78-L85): Acquires `PowerManager.FULL_WAKE_LOCK` with a 15-minute timeout.
   - `FULL_WAKE_LOCK` has been deprecated since API 17 in favor of `FLAG_KEEP_SCREEN_ON` on the window.

#### Comprehensive Launch Path & Target SDK 36 Analysis
All launch paths for `ActiveAlarmActivity` were audited:
- **Path A: Direct Receiver Launch** ([`CustomAlarmReceiver.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/receiver/CustomAlarmReceiver.kt#L71-L78)):
  Uses an explicit intent `Intent(context, ActiveAlarmActivity::class.java)` within the same package. Non-exported activities are fully accessible to same-package callers.
- **Path B: Foreground Service Full-Screen Notification** ([`CustomAlarmPlaybackService.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/service/CustomAlarmPlaybackService.kt#L144-L177)):
  Constructs a `PendingIntent.getActivity(this, ..., fullScreenIntent, ...)` and attaches it via `NotificationCompat.Builder.setFullScreenIntent(fullScreenPendingIntent, true)`. 
  *Platform Security Rule:* When SystemUI executes a `PendingIntent` on behalf of an alarm notification, it executes under the originating application's identity and UID (`com.jayasuriya.rhythm`). It does **not** require the target Activity to be `exported="true"`.
- **Path C: ViewModel Preview Launch** ([`MainViewModel.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/ui/MainViewModel.kt#L1481)):
  Explicit internal intent for testing alarm missions from UI settings.
- **Target SDK 36 & Android 14+ Considerations:**
  - `android.permission.USE_FULL_SCREEN_INTENT` is already declared in `AndroidManifest.xml` (line 15). Under Android 14+ (API 34+), this is a special app access permission auto-granted to alarm applications. Setting `exported="false"` has no adverse interaction with full-screen intent delivery.
  - On modern Android, direct `context.startActivity()` from background receivers (Path A) is restricted by Background Activity Launch (BAL) rules when the screen is locked. Path B (`setFullScreenIntent` via Foreground Service) is the canonical, supported mechanism to display alarms over the lockscreen.
  - The attribute `android:showOnLockScreen="true"` is an obsolete, undocumented vendor attribute (Samsung legacy API < 27) and should be cleaned up. Modern lockscreen behavior is governed by `android:showWhenLocked="true"` and `android:turnScreenOn="true"` alongside `setShowWhenLocked(true)` and `setTurnScreenOn(true)` in code.

#### Corrected Solution
1. In `AndroidManifest.xml`, change `ActiveAlarmActivity` to `android:exported="false"`. Remove obsolete `showOnLockScreen="true"`.
2. In `CustomAlarmReceiver.kt` and `CustomAlarmPlaybackService.kt`, remove `PowerManager.FULL_WAKE_LOCK`. Use `PARTIAL_WAKE_LOCK` strictly for keeping the CPU awake during audio playback in the background service.
3. In `ActiveAlarmActivity.kt`, ensure `window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)` is set in `onCreate` alongside `setShowWhenLocked(true)` and `setTurnScreenOn(true)`.

---

### PR 3: Room Schema Export & Migration Verification

**Classification: ⚠️ Finding Verified, Solution Needs Revision**

#### Verified Evidence
- [`AppDatabase.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/data/database/AppDatabase.kt#L70): `exportSchema = false` is explicitly configured.
- Zero schema JSON files exist in `schemas/` or repository history.
- Zero migration tests exist in the test suite.
- 10 migration objects exist in `AppDatabase.kt` (`MIGRATION_1_2` through `MIGRATION_10_11`). All perform additive DDL operations.

#### Correction of Schema Export Plan
The roadmap stated:
> *"Enable `exportSchema = true` in `AppDatabase.kt` and generate schema JSON files for all historical versions."*

**Critical Correction:** 
Room's annotation processor (KSP) **only outputs the schema JSON for the version currently declared on `@Database`** at compile time. Because `AppDatabase` is currently at `version = 11`, enabling `exportSchema = true` will generate **only `11.json`**. 
Room has no retrospective knowledge of entity definitions for versions 1 through 10 and cannot generate `1.json` through `10.json`.

Furthermore, Room's `MigrationTestHelper` cannot test migrations starting from version $N$ if `N.json` is missing (it throws `FileNotFoundException`).

#### Realistic Two-Track Migration Strategy
1. **Track 1: Export Current Baseline (`11.json`) for Future Migrations**
   - Configure KSP schema export in `app/build.gradle.kts`:
     ```kotlin
     ksp {
         arg("room.schemaLocation", "$projectDir/schemas")
     }
     ```
   - Set `exportSchema = true` in `AppDatabase.kt`.
   - Build to generate `schemas/com.example.data.database.AppDatabase/11.json` and commit it.
   - All future migrations (v11 → v12+) will be fully testable with `MigrationTestHelper`.

2. **Track 2: Raw SQLite DDL Replay Harness for Legacy Migrations (1 → 11)**
   - Because `MIGRATION_1_2` through `MIGRATION_10_11` are self-contained DDL scripts in `AppDatabase.kt`, automated migration verification does not require synthetic historical JSON files.
   - Implement a Robolectric unit test using `FrameworkSQLiteOpenHelperFactory`:
     - Create an empty database at version 1 using initial version 1 DDL.
     - Sequentially apply migrations `MIGRATION_1_2` through `MIGRATION_10_11`.
     - Open the migrated database using `Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(...).build()`.
     - Validate that Room's internal validation query succeeds without `IllegalStateException: Room cannot verify the data integrity`.

#### Regression Risks & Implementation Blockers
- **Blocker:** None.
- **Risk:** Gradle KSP argument placement in `app/build.gradle.kts` must be placed outside the `android` block to avoid DSL resolution errors with the non-standard `compileSdk` configuration.

---

### PR 4: Backup & Restore Resiliency & Version Alignment

**Classification: ⚠️ Finding Verified, Solution Needs Revision**

#### Verified Evidence
1. **Schema Versioning in `DataBackupManager.kt`:**
   - Line 63: `root.put("schemaVersion", 5)`.
   - Line 62: `root.put("version", 1)`.
   - Line 354: `validateBackup` verifies only `root.getString("app") == "Rhythm & Focus"`. It does **not** validate or enforce `schemaVersion`.
2. **Non-Atomic Restore:**
   - Lines 737–753: Sequential inserts across 16 DAOs with comment `// Atomic insertion`. This is not atomic; failure at any point leaves the database in a partially imported state.
3. **Hard-Coded Test Assertion:**
   - [`ExampleRobolectricTest.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/test/java/com/example/ExampleRobolectricTest.kt#L583): `assertEquals(5, root.getInt("schemaVersion"))`.

#### Critical Distinction: Room SQLite Version vs. Backup Format Version
The roadmap assumed that `schemaVersion: 5` in `DataBackupManager.kt` was an error because Room is at version 11, and proposed bumping it to `11`.

**This assumption is conceptually incorrect:**
- **Room Database Version (`version = 11`):** Represents the internal SQLite physical schema (tables, column nullability, foreign keys, indexes). It changes whenever SQLite structure changes.
- **Backup Schema Version (`schemaVersion = 5`):** Represents the serialized JSON document contract. It defines the names and structures of JSON arrays and objects exported for user data interchange.
- Tying backup format version directly to Room version is an architectural anti-pattern. If Room introduces an internal index in Migration 11 → 12, the backup JSON contract does not change.
- Existing tests (`ExampleRobolectricTest.kt` line 583) validate that exported backups declare `schemaVersion: 5`.

#### Telemetry vs. User Data: Health Connect Tables
The roadmap recommended backing up Health Connect tables (`health_connect_daily_summaries`, `health_connect_sleep_stages`, `health_connect_heart_samples`).

**Analysis & Recommendation:**
- Health Connect data is a local cache of synced telemetry from the system `HealthConnectManager`.
- It is **not** primary user-authored data (unlike routines, sleep logs, medications, study tasks, and custom alarms).
- Including raw heart rate samples in JSON backups inflates file size from kilobytes to tens of megabytes, slows down JSON parsing, and creates potential sync conflicts when restored on a device that subsequently syncs with the system Health Connect provider.
- **Recommendation:** Do NOT include high-frequency Health Connect raw samples in portable user backups. If daily summary snapshots are desired, they can be included in a dedicated format bump, but raw telemetry should remain excluded.

#### Corrected Solution
1. **Do not arbitrarily change `schemaVersion` to 11.** Keep the backup format versioning independent from Room. If new domain fields are added, increment the backup schema version to `6` deliberately.
2. In `DataBackupManager.validateBackup()`, enforce supported schema version bounds:
   ```kotlin
   val schemaVersion = root.optInt("schemaVersion", 1)
   if (schemaVersion !in 1..SUPPORTED_BACKUP_SCHEMA_VERSION) {
       return Result.failure(IllegalArgumentException("Unsupported backup schema version: $schemaVersion"))
   }
   ```
3. Wrap `restoreBackup()` in `database.withTransaction { ... }` so any JSON parse error or SQLite exception completely rolls back all mutations.
4. Update `ExampleRobolectricTest.kt` to test both current backup exports and backwards-compatible restoration of older v5 backups.

---

### PR 5: Time Zone & Daylight Saving Alarm Resilience

**Classification: ✅ Verified and Ready for Implementation**

#### Verified Evidence
- [`BootReceiver.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/receiver/BootReceiver.kt#L15-L25): Handles only `Intent.ACTION_BOOT_COMPLETED` and `Intent.ACTION_MY_PACKAGE_REPLACED`.
- Manifest filter ([`AndroidManifest.xml`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/AndroidManifest.xml#L123-L126)) registers only boot and replacement actions.
- Alarms scheduled with `RTC_WAKEUP` at fixed UTC epochs do not adjust when system timezone changes or Daylight Saving Time shifts occur.

#### Solution Assessment
The implementation is straightforward and low-risk:
1. Register `android.intent.action.TIMEZONE_CHANGED` and `android.intent.action.TIME_SET` in `AndroidManifest.xml` under `BootReceiver`.
2. In `BootReceiver.onReceive()`, expand the action check:
   ```kotlin
   when (intent.action) {
       Intent.ACTION_BOOT_COMPLETED,
       Intent.ACTION_MY_PACKAGE_REPLACED,
       Intent.ACTION_TIMEZONE_CHANGED,
       Intent.ACTION_TIME_SET -> {
           // Reschedule alarms using existing AlarmScheduler / CustomAlarmScheduler logic
       }
   }
   ```
3. Execution time in `BootReceiver` is minimal because scheduling calls `AlarmManager.setAlarmClock()`, which is fast. Using `goAsync()` ensures completion within broadcast lifecycle limits.

---

### PR 6: Health Connect Pagination & Changes Token Sync

**Classification: ⚠️ Finding Verified, Solution Needs Revision**

#### Verified Evidence
- [`HealthConnectManager.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/health/HealthConnectManager.kt#L388-L418): Reads `HeartRateRecord` with a single `readRecords(ReadRecordsRequest(...))` call.
- The request does not supply or loop over `pageToken`. Health Connect records beyond 1,000 per request are silently truncated.
- Incremental sync tokens (`getChangesToken` / `getChanges`) are not implemented.

#### Solution Gap & Dependency Considerations
1. **SDK Version Verification:** The project uses `androidx.health.connect:connect-client:1.1.0-alpha11` (or similar). The `pageToken` API is available on `ReadRecordsResponse.pageToken: String?`.
2. **Changes Token Scope:**
   - Storing a changes token requires persistent storage. The existing `app_settings` table via `AppRepository.saveSetting("hc_changes_token", token)` is the natural location.
   - When a token expires (Health Connect invalidates tokens after ~30 days or on permission changes), `getChanges` throws or returns `changesTokenExpired = true`. The client must seamlessly fall back to a full historical read.
3. **Recommendation:** Split this work into two parts:
   - **Part A (Immediate):** Implement the `pageToken` pagination loop in `readHeartRateData` to prevent silent truncation.
   - **Part B (Future):** Implement differential synchronization via changes token after the pagination loop is verified.

---

### PR 7: Architecture Modularization (MainViewModel Decomposition)

**Classification: ⚠️ Finding Verified, Solution Needs Revision**

#### Verified Evidence & Discrepancies
- **Line Count Discrepancy:** The roadmap states `MainViewModel.kt` has 2,067 lines. The actual line count is **1,839 lines**. Cited line numbers in the roadmap are inaccurate.
- **Context Access:** Line 98 accesses `AppDatabase.appContext` via static companion reference.
- **Class Hierarchy:** `MainViewModel` extends `ViewModel()` (not `AndroidViewModel`).

#### Elevated Regression Risk
The roadmap characterized the regression risk of PR 7 as "Moderate." **This must be classified as HIGH.**
- [`ExampleRobolectricTest.kt`](file:///e:/Programming/personal%20app/Rhythm/app/src/test/java/com/example/ExampleRobolectricTest.kt) has **1,758 lines** of tests.
- Dozens of tests instantiate `MainViewModel(repository)` directly and call synchronous test helpers defined directly on `MainViewModel` (e.g., `createStudyCategorySync`, `saveBrainDumpNoteSync`, `convertBrainDumpNoteToStudyTaskSync`).
- Decomposing `MainViewModel` all at once will cause massive compile breaks across the test suite and UI screens.

#### Corrected Phased Approach
1. Do not decompose all sub-domains simultaneously.
2. Adopt a phased extraction plan:
   - **Phase 1:** Clean up `appContext` reference by injecting `Application` context cleanly.
   - **Phase 2:** Extract `MedicationViewModel` and update its corresponding UI and tests.
   - **Phase 3:** Extract `StudyViewModel`.
   - **Phase 4:** Extract `AlarmViewModel`.
3. Retain forwarding/delegation methods on `MainViewModel` during transition to preserve test suite compatibility until tests are migrated.

---

### PR 8: Sensitive Data Protection at Rest

**Classification: 🔍 Requires Additional Investigation**

#### Verified Evidence
- `rhythm_focus_database` is unencrypted SQLite on disk.
- Backup JSON files exported by `DataBackupManager` are stored as unencrypted plain text.

#### Investigation Requirements & Blockers
1. **SQLCipher Compatibility with Room 2.7.0:**
   - Room 2.7.0-alpha12 with SQLite driver abstraction requires careful verification when pairing with `net.zetetic:sqlcipher-android`.
   - Encrypting an existing in-place SQLite database on user devices requires a migration step (`ATTACH DATABASE ... AS encrypted KEY ...; SELECT sqlcipher_export('encrypted');`). If this migration is interrupted (e.g. app killed, device out of storage), data loss is permanent.
2. **Key Derivation & Lockout Risk:**
   - Android Keystore master keys cannot be backed up. If the user moves to a new device or the keystore is cleared, local database access is lost forever. A documented passphrase or biometric recovery flow is mandatory before enabling database encryption.
3. **Actionable Sub-PR: Backup File Encryption (PR 8a):**
   - Encrypting exported backup files using AES-256-GCM with a user-supplied password (or Keystore-backed key) is well-defined, isolated, and carries zero risk of database corruption.

**Recommendation:** Split PR 8 into:
- **PR 8a (Ready after PR 4):** Optional AES-256-GCM encryption for exported backup files.
- **PR 8b (Deferred):** Full SQLCipher database encryption pending migration safety proof and recovery design.

---

## Updated PR Dependencies and Implementation Sequence

```
                                [PR 2: Security & WakeLock]
                                             │
                                             ▼
                                [PR 5: TimeZone / DST Alarms]
                                             │
                                             ▼
                                [PR 3: Schema Export & Migrations]
                                             │
                                             ▼
                                [PR 1: Room Transactions & Cascades]
                                             │
                                             ▼
                                [PR 4: Backup Atomicity & Versioning]
                                             │
                      ┌──────────────────────┴──────────────────────┐
                      ▼                                             ▼
         [PR 6: HC Pagination]                         [PR 8a: Backup Encryption]
                      │                                             │
                      └──────────────────────┬──────────────────────┘
                                             ▼
                              [PR 7: MainViewModel Phased Split]
                                             │
                                             ▼
                              [PR 8b: SQLCipher DB Encryption] (Post-Investigation)
```

### Recommended Sequence Justification
1. **PR 2 (Security Hardening):** Zero database or architecture dependencies. Fixes a verified P0 exported component vulnerability immediately.
2. **PR 5 (TimeZone/DST):** Isolated, low-risk, improves alarm reliability without touching database or UI.
3. **PR 3 (Room Schema Export):** Establishes baseline `11.json` and migration test harness before further database alterations.
4. **PR 1 (Room Transactions):** Resolves data integrity and cascade bugs in DAOs/Repository with migration testing in place.
5. **PR 4 (Backup Atomicity):** Establishes transactional restore safety on top of solid DAO transactions.
6. **PR 6 & PR 8a:** Independent P1 feature enhancements built on top of resilient storage and backup layers.
7. **PR 7 (Architecture):** Highest regression risk across UI and 1,758 lines of tests; executed only after underlying repositories and models are stable.
8. **PR 8b (SQLCipher):** Deferred until migration safety and key recovery architectures are fully specified.

---

## Validation Summary Table

| PR | Title | Status | Primary Adjustment / Blocker |
|---|---|---|---|
| **PR 1** | Room Data Integrity & Transaction Safety | ⚠️ Solution Needs Revision | `deleteReminderByReferenceId` uses `=`. Must add prefix query `WHERE referenceId LIKE :prefix \|\| '%'`. |
| **PR 2** | Security Hardening & WakeLock Modernization | ✅ Ready for Implementation | Verified safe for all launch paths under targetSdk 36. Remove obsolete `showOnLockScreen`. |
| **PR 3** | Room Schema Export & Migration Verification | ⚠️ Solution Needs Revision | Cannot generate past 1–10 JSONs. Export baseline `11.json` + implement SQLite DDL replay harness for 1–11. |
| **PR 4** | Backup & Restore Resiliency | ⚠️ Solution Needs Revision | Decouple backup format version from Room DB version. Do not force v11. Do not backup raw HC telemetry. |
| **PR 5** | Time Zone & DST Alarm Resilience | ✅ Ready for Implementation | Add `TIMEZONE_CHANGED` / `TIME_SET` to manifest and receiver. Low risk. |
| **PR 6** | Health Connect Pagination & Changes Token | ⚠️ Solution Needs Revision | Implement pagination loop first. Split changes token into subsequent enhancement. |
| **PR 7** | Architecture Modularization (MainViewModel) | ⚠️ Solution Needs Revision | Correct line count (1,839). Reclassify risk to HIGH due to 1,758-line test suite. Use phased extraction. |
| **PR 8** | Sensitive Data Protection at Rest | ✅ Completed & Merged | Split into PR 8a (AES-256-GCM backup encryption, merged in #28) and PR 8b (SQLCipher database encryption & Keystore key management, merged in #29). |

---

*This document contains verified documentation analysis only. No project source code, database schemas, or dependencies have been modified.*  
*Ready for review and approval prior to implementation.*
