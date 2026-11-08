# Rhythm — Stage 4: Correction Roadmap

**Status:** Proposed / Awaiting Approval  
**Audit Source:** `design/audits/ARCHITECTURE_AUDIT.md`, `DATA_INTEGRITY_AUDIT.md`, `PRIVACY_SECURITY_AUDIT.md`  
**Package:** `com.jayasuriya.rhythm` (`com.example.*`)  
**Guiding Constraint:** Strictly non-destructive. Never reset or corrupt the Room database; preserve all user data, alarms, medication history, and category colors.

---

## 1. Roadmap Overview & PR Sequence

The findings from Stage 4 are structured into **8 discrete, independently reviewable Pull Requests (PRs)** ordered strictly by priority and risk profile:

```
[PR 1: P0] Room Data Integrity & Transaction Safety (Multi-step operations, cascading cleanup)
     |
[PR 2: P0] Security Hardening (ActiveAlarmActivity export fix, wake lock modernization)
     |
[PR 3: P0] Room Schema Export & Migration Verification (exportSchema=true, MigrationTestHelper)
     |
[PR 4: P1] Backup & Restore Resiliency (Schema version 11 alignment, atomic restore, ID safety)
     |
[PR 5: P1] Time Zone & Daylight Saving Alarm Resilience (BroadcastReceiver for time/zone changes)
     |
[PR 6: P1] Health Connect Pagination & Changes Token (Pagination loop, delta sync)
     |
[PR 7: P2] Architecture Modularization (Decompose MainViewModel into 6 domain ViewModels)
     |
[PR 8: P3] Sensitive Data Protection at Rest (Optional SQLCipher / Keystore encryption)
```

---

## 2. Detailed Pull Request Specifications

---

### PR 1: Room Data Integrity & Transaction Safety
- **Priority:** **P0 (Immediate Core Data Safety)**
- **Problem & Evidence:**
  - `AppRepository.kt` lines 213–216 (`deleteCategoryAndReassignTasks`), lines 433–475 (`syncRoutinesToReminders`), lines 478–519 (`syncMedicationsToReminders`), and lines 632–646 (`saveHealthConnectData`) perform multi-step database mutations outside of Room transactions.
  - When deleting a medication (`deleteMedication(id)` at line 157), associated reminders (`referenceId = "med:${id}_%"`) are never deleted, leaving alarms ringing forever for deleted medications.
- **Exact Files Expected to Change:**
  - `app/src/main/java/com/example/data/dao/AppDaos.kt`
  - `app/src/main/java/com/example/data/dao/HealthConnectDao.kt`
  - `app/src/main/java/com/example/data/repository/AppRepository.kt`
- **Proposed Solution:**
  1. Add `@Transaction` annotations to composite DAO methods or wrap repository operations with `database.withTransaction { ... }`.
  2. In `AppRepository.deleteMedication(id)`, execute an atomic transaction that deletes the medication AND cancels/removes all associated reminders (`reminderDao.deleteReminderByReferenceId("med:${id}_%")`).
  3. In `AppRepository.saveHealthConnectData`, wrap the delete and insert operations in a transaction so existing hypnograms/heart samples are never lost if an insert fails.
- **Database Migration Requirements:** None. (Application-level repository & DAO annotations only; schema remains v11).
- **Backward Compatibility Requirements:** Fully backward-compatible; existing records and database files are preserved.
- **Regression Risks:** Low. Must ensure cancelling alarm PendingIntents handles non-existent IDs gracefully.
- **Tests Required:**
  - Robolectric test verifying `deleteMedication` removes associated reminders and cancels pending alarms.
  - Test verifying atomic rollback in `saveHealthConnectData` if an error occurs.
- **Expected Outcome:** Guarantees ACID atomicity across all multi-step data mutations; prevents orphaned alarm execution.

---

### PR 2: Security Hardening & WakeLock Modernization
- **Priority:** **P0 (Critical Application Security)**
- **Problem & Evidence:**
  - `app/src/main/AndroidManifest.xml` line 98 declares `<activity android:name=".ui.screens.alarm.ActiveAlarmActivity" android:exported="true" ... />` without intent filters or permissions, allowing any rogue third-party app to launch the lockscreen alarm dismiss UI.
  - `CustomAlarmReceiver.kt` lines 33–39 and `CustomAlarmPlaybackService.kt` lines 78–85 acquire deprecated `FULL_WAKE_LOCK` for up to 15 minutes.
- **Exact Files Expected to Change:**
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/com/example/receiver/CustomAlarmReceiver.kt`
  - `app/src/main/java/com/example/service/CustomAlarmPlaybackService.kt`
  - `app/src/main/java/com/example/ui/screens/alarm/ActiveAlarmActivity.kt`
- **Proposed Solution:**
  1. Change `android:exported="true"` to `android:exported="false"` on `ActiveAlarmActivity` in `AndroidManifest.xml`.
  2. Remove `FULL_WAKE_LOCK`. Utilize `FLAG_KEEP_SCREEN_ON` on the `ActiveAlarmActivity` window with `setTurnScreenOn(true)` and `setShowWhenLocked(true)`.
  3. Limit background wake locks in `CustomAlarmPlaybackService` to `PARTIAL_WAKE_LOCK` with strict immediate release upon `stopAlarm()`.
- **Database Migration Requirements:** None.
- **Backward Compatibility Requirements:** Compatible across Android 8.0 through Android 15+.
- **Regression Risks:** Must verify alarms continue to visually light up lockscreen and ring reliably.
- **Tests Required:**
  - Manifest verification test proving `ActiveAlarmActivity` is not exported.
  - Robolectric test verifying alarm start/stop service lifecycle.
- **Expected Outcome:** Eliminates remote attack vector; fixes system wake lock resource drain.

---

### PR 3: Room Schema Export & Migration Verification
- **Priority:** **P0 (Schema Safety & Release Gate)**
- **Problem & Evidence:**
  - `AppDatabase.kt` line 70 sets `exportSchema = false`.
  - Schema JSON files are not generated into version control.
  - Zero automated tests exist for migrations `MIGRATION_1_2` through `MIGRATION_10_11`.
- **Exact Files Expected to Change:**
  - `app/build.gradle.kts` (Configure KSP `room.schemaLocation`)
  - `app/src/main/java/com/example/data/database/AppDatabase.kt` (`exportSchema = true`)
  - `app/src/test/java/com/example/MigrationTest.kt` (New automated test file)
- **Proposed Solution:**
  1. Set `exportSchema = true` in `AppDatabase.kt` and configure `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` in `build.gradle.kts`.
  2. Generate baseline schema for version 11.
  3. Implement `MigrationTest.kt` utilizing `MigrationTestHelper` to validate all migration paths from v1 to v11 against SQLite schemas.
- **Database Migration Requirements:** None. Preserves schema version 11.
- **Backward Compatibility Requirements:** Mandatory. Guarantees that users upgrading from any previous app version will not experience crashes.
- **Regression Risks:** None.
- **Tests Required:**
  - `MigrationTestHelper` tests for sequential and cumulative migrations.
- **Expected Outcome:** Complete automated verification of database upgrades.

---

### PR 4: Backup & Restore Resiliency & Version Alignment
- **Priority:** **P1 (User Data Protection)**
- **Problem & Evidence:**
  - `DataBackupManager.kt` line 63 hardcodes `schemaVersion: 5` while Room is at version 11.
  - `restoreBackup()` (lines 737–754) uses `OnConflictStrategy.REPLACE` without remapping IDs, silently overwriting pre-existing local records.
  - Restore insertion runs in 16 separate uncommitted operations without rollback.
  - Health Connect tables are omitted from backup.
- **Exact Files Expected to Change:**
  - `app/src/main/java/com/example/util/DataBackupManager.kt`
  - `app/src/test/java/com/example/ExampleRobolectricTest.kt`
- **Proposed Solution:**
  1. Update declared `schemaVersion` to `11` in `DataBackupManager.kt`.
  2. Wrap `restoreBackup()` in `database.withTransaction { ... }` so any JSON parse error rolls back all table mutations.
  3. Include `health_connect_daily_summaries` in export and restore.
  4. Provide an "Append / Merge" option that zeroes auto-generated primary keys (`id = 0L`) during restore to prevent overwriting existing user data.
- **Database Migration Requirements:** None.
- **Backward Compatibility Requirements:** Must successfully parse and restore older backup files created with `schemaVersion <= 5`.
- **Regression Risks:** Low.
- **Tests Required:**
  - Test verifying rollback on corrupted restore JSON.
  - Test verifying backward compatibility with older v5 backups.
- **Expected Outcome:** Fail-safe backup restoration without data corruption or accidental overwriting.

---

### PR 5: Time Zone & Daylight Saving Alarm Resilience
- **Priority:** **P1 (Alarm Reliability)**
- **Problem & Evidence:**
  - `BootReceiver.kt` lines 16–17 only filters `ACTION_BOOT_COMPLETED` and `ACTION_MY_PACKAGE_REPLACED`.
  - When a user travels across time zones or when daylight saving time changes, exact alarms scheduled via `RTC_WAKEUP` are not recalculated, causing alarms to fire at incorrect local times.
- **Exact Files Expected to Change:**
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/com/example/receiver/BootReceiver.kt`
- **Proposed Solution:**
  1. Add `<action android:name="android.intent.action.TIMEZONE_CHANGED" />` and `<action android:name="android.intent.action.TIME_SET" />` to `BootReceiver`'s intent filter.
  2. In `BootReceiver.onReceive`, trigger immediate recalculation and rescheduling of all enabled reminders and custom alarms using `AlarmScheduler.calculateNextTriggerMillis()`.
- **Database Migration Requirements:** None.
- **Backward Compatibility Requirements:** Compatible across all Android versions.
- **Regression Risks:** Minimal; idempotent alarm rescheduling.
- **Tests Required:**
  - Test verifying alarm recalculation upon timezone change intent.
- **Expected Outcome:** Alarms always fire at the correct local time regardless of travel or daylight saving shifts.

---

### PR 6: Health Connect Pagination & Changes Token Sync
- **Priority:** **P1 (Wearable Telemetry Completeness)**
- **Problem & Evidence:**
  - `HealthConnectManager.kt` lines 388–418 reads `HeartRateRecord` without checking `pageToken`. Samples beyond 1,000 per request are silently lost.
  - Sync reads full date ranges repeatedly rather than utilizing incremental change tokens.
- **Exact Files Expected to Change:**
  - `app/src/main/java/com/example/health/HealthConnectManager.kt`
- **Proposed Solution:**
  1. Implement a `while (pageToken != null)` pagination loop for `readRecords(ReadRecordsRequest(...))` across all record types.
  2. Implement `getChangesToken` and `getChanges` to synchronize only records modified since `lastSyncedAt`.
- **Database Migration Requirements:** None.
- **Backward Compatibility Requirements:** Preserves all existing `HealthConnectDailySummaryEntity` entries.
- **Regression Risks:** Low.
- **Tests Required:**
  - Unit test verifying multi-page record aggregation.
- **Expected Outcome:** Eliminates dropped heart rate samples and improves sync performance.

---

### PR 7: Architecture Modularization (MainViewModel Decomposition)
- **Priority:** **P2 (Maintainability & Clean Architecture)**
- **Problem & Evidence:**
  - `MainViewModel.kt` is 2,067 lines long and couples 13 feature domains.
  - Relies on static `AppDatabase.appContext` fallback, risking memory leaks.
- **Exact Files Expected to Change:**
  - `app/src/main/java/com/example/ui/MainViewModel.kt` (Refactored / decomposed)
  - New domain ViewModels in `app/src/main/java/com/example/ui/viewmodels/`:
    - `StudyViewModel.kt`
    - `RoutineViewModel.kt`
    - `HealthViewModel.kt`
    - `AlarmViewModel.kt`
    - `BrainDumpViewModel.kt`
    - `SettingsViewModel.kt`
  - Screen composables updated to receive focused domain ViewModels.
- **Proposed Solution:**
  1. Decompose `MainViewModel` into 6 single-responsibility ViewModels extending `AndroidViewModel(application)`.
  2. Provide a clean `ViewModelProvider.Factory` or constructor injection without static context fallbacks.
  3. Scope ViewModels to individual screen navigation destinations.
- **Database Migration Requirements:** None.
- **Backward Compatibility Requirements:** No impact on database or user settings.
- **Regression Risks:** Moderate UI regression risk during navigation argument wiring. Requires comprehensive screen-by-screen testing.
- **Tests Required:**
  - Focused Robolectric tests for each individual domain ViewModel.
- **Expected Outcome:** Drastically improves code maintainability, reduces recomposition scope, and eliminates Activity memory leaks.

---

### PR 8: Sensitive Data Protection at Rest
- **Priority:** **P3 (Hardened Defense in Depth)**
- **Problem & Evidence:**
  - `rhythm_focus_database` stores prescription medication names and psychiatric check-in notes in plaintext SQLite.
- **Exact Files Expected to Change:**
  - `app/src/main/java/com/example/data/database/AppDatabase.kt`
  - `app/src/main/java/com/example/util/DataBackupManager.kt`
- **Proposed Solution:**
  1. Provide optional user-enabled password protection for local backups using AES-256-GCM.
  2. Evaluate SQLCipher database encryption for PHI data with transparent migration.
- **Database Migration Requirements:** Encrypted database migration wrapper if enabled.
- **Backward Compatibility Requirements:** Must support reading existing unencrypted database.
- **Regression Risks:** High (risk of data lockout if user forgets encryption password).
- **Tests Required:**
  - Encryption and decryption round-trip tests.
- **Expected Outcome:** Defense-in-depth protection for sensitive health records on rooted or compromised devices.
