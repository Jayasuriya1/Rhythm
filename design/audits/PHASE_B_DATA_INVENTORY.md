# Phase B: Data Inventory & Metric Audit

**Audit Date:** November 9, 2026  
**Audited Target:** `com.jayasuriya.rhythm` (Schema Version 11)  
**Auditor:** Principal Android Engineer & Product Architect  
**Methodology:** Comprehensive AST schema analysis, Room DAO query audit, Health Connect data pipeline verification, and reactive StateFlow lifecycle inspection.

---

## 1. Executive Summary

Rhythm's local data storage is built upon Room 2.6.1 with SQLCipher AES-256 database encryption, containing **20 Room entities** and **10 Data Access Objects (DAOs)**. In addition, the application integrates with Google Health Connect for verified biometric imports (sleep stages, heart rate time-series, steps, HRV, and SpO2).

### Key Architectural Findings:
1. **Schema & Encryption Integrity:** The database schema is strictly at Version 11 with `exportSchema = true` and automated migration verification tests (`DatabaseMigrationTest.kt`). Tables are encrypted at rest using a 256-bit passphrase managed by Android KeyStore (`DatabaseKeyManager.kt`).
2. **Zero Network Transmission:** `android.permission.INTERNET` is completely absent from `AndroidManifest.xml`. All data processing, storage, analytics, and exports occur 100% locally on the device.
3. **Data Availability vs. Insight Exposure Disconnect:** While the database faithfully captures granular circadian anchors, sleep records, medication intakes, focus sessions, and vitals, the current `InsightsScreen.kt` only exposes a fraction of this data.
4. **Query & Calculation Gaps in UI:**
   - **Study Sessions:** Fetched via `getRecentStudySessions(limit = 20)`. If a user completes more than 20 sessions across a 7-day window, older sessions in that week are dropped from the sum.
   - **Social Rhythm Metric (SRM):** Anchors are recorded in `routine_entries`, but no regularity index or target deviation metrics are calculated or displayed.
   - **Medication Adherence:** Granular dose status (`TAKEN`, `SKIPPED`, `DELAYED`, `MISSED`) is stored in `medication_intakes`, but completely missing from the `InsightsScreen`.
   - **Health Connect Integration:** Rich hypnograms and resting heart rate data reside in Room, but are isolated to sub-screens without surfacing in the primary weekly reflection.

---

## 2. Comprehensive Room Entity Inventory (Schema Version 11)

| # | Entity Class | Table Name | Primary Key | Key Indexes | Stored Metrics & Types | Retention / Cardinality | Current UI Exposure |
|---|---|---|---|---|---|---|---|
| 1 | `RoutineAnchorEntity` | `routine_anchors` | `id: String` | None | `name`, `description`, `defaultTargetMinutes: Int`, `sortOrder: Int`, `isEnabled: Boolean` | 5 default circadian anchors (Config) | TodayScreen, RoutineScreen |
| 2 | `RoutineEntryEntity` | `routine_entries` | `id: Long` (auto) | Unique `(date, anchorId)` | `date: String`, `anchorId: String`, `targetTimeMinutes: Int`, `actualTimeMinutes: Int?`, `status: String`, `notes: String?`, `disruptionNote: String?`, `updatedAt: Long` | Daily events (~5/day) | TodayScreen, RoutineScreen |
| 3 | `SleepEntryEntity` | `sleep_entries` | `id: Long` (auto) | Unique `(date)` | `date: String`, `bedtimeMinutes: Int`, `wakeTimeMinutes: Int`, `durationMinutes: Int`, `qualityRating: Int` (1–5), `notes: String?`, `updatedAt: Long` | 1 per day | TodayScreen, InsightsScreen, SleepScreen |
| 4 | `HealthCheckinEntity` | `health_checkins` | `id: Long` (auto) | Unique `(date)` | `date: String`, `mood: Int?` (1–5), `energy: Int?` (1–5), `stress: Int?` (1–5), `irritability: Int?` (1–5), `activationEnergy: Int?` (1–5), `symptomsNotes: String?`, `updatedAt: Long` | 1 per day | TodayScreen, InsightsScreen |
| 5 | `WaterEntryEntity` | `water_entries` | `id: Long` (auto) | None | `date: String`, `volumeMl: Int`, `timestamp: Long` | Multiple entries per day | TodayScreen (Today sum only) |
| 6 | `MedicationEntity` | `medications` | `id: Long` (auto) | None | `name: String`, `dosage: String`, `unit: String`, `instructions: String`, `scheduledTimeMinutes: Int`, `scheduledTimesJson: String`, `repeatDays: String`, `startDate: String`, `endDate: String?`, `isActive: Boolean`, `isArchived: Boolean` | User prescriptions (~2–10 active) | TodayScreen, MedicationScreen |
| 7 | `MedicationIntakeEntity` | `medication_intakes` | `id: Long` (auto) | Unique `(date, medicationId, scheduledTimeMinutes)` | `date: String`, `medicationId: Long`, `scheduledTimeMinutes: Int`, `actualActionTime: Long?`, `status: String` (`TAKEN`, `SKIPPED`, `DELAYED`, `MISSED`, `UNRECORDED`), `medicationName: String`, `dosageSnapshot: String`, `isCorrected: Boolean` | Multi-dose records per med/day | TodayScreen, MedicationScreen |
| 8 | `StudyCategoryEntity` | `study_categories` | `id: Long` (auto) | None | `name: String`, `description: String?`, `colorHex: String`, `iconName: String`, `sortOrder: Int`, `isArchived: Boolean` | User categories (3 defaults: DSA, Frontend, Backend) | StudyScreen |
| 9 | `StudyTaskEntity` | `study_tasks` | `id: Long` (auto) | None | `title: String`, `category: String`, `status: String`, `isDailyPriority: Boolean`, `estimatedMinutes: Int`, `dateCreated: String`, `dateCompleted: String?`, `nextSmallStep: String?` | User tasks (~10–100) | TodayScreen, StudyScreen |
| 10 | `StudySessionEntity` | `study_sessions` | `id: Long` (auto) | None | `taskId: Long?`, `taskTitle: String`, `category: String`, `durationMinutes: Int`, `sessionType: String` (`POMODORO`, `DEEP_WORK`, `STOPWATCH`, `CUSTOM`), `date: String`, `timestamp: Long`, `completionStatus: String`, `learnedNotes: String?` | Logged sessions (~1–10/day) | TodayScreen, InsightsScreen (truncated to 20), StudyScreen |
| 11 | `ActiveTimerDraftEntity` | `active_timer_draft` | `id: Int` (=1) | None | `taskId: Long?`, `taskTitle: String`, `category: String`, `mode: String`, `targetSeconds: Int`, `startWallClockMillis: Long`, `accumulatedElapsedSeconds: Int`, `isRunning: Boolean`, `isPaused: Boolean`, `isCompleted: Boolean`, `currentPhase: String`, `protocolJson: String?` | Single row (state persistence) | Fullscreen Timer, TodayScreen |
| 12 | `BrainDumpNoteEntity` | `brain_dump_notes` | `id: Long` (auto) | None | `title: String?`, `content: String`, `tag: String?`, `isPinned: Boolean`, `isArchived: Boolean`, `createdAt: Long`, `updatedAt: Long` | User thoughts & quick notes | TodayScreen, BrainDumpScreen |
| 13 | `AppSettingEntity` | `app_settings` | `key: String` | None | `value: String` | Key-value application preferences | SettingsScreen, MainViewModel |
| 14 | `ReminderEntity` | `reminders` | `id: Long` (auto) | None | `title: String`, `category: String`, `timeMinutes: Int`, `repeatDays: String`, `isEnabled: Boolean`, `soundEnabled: Boolean`, `vibrateEnabled: Boolean`, `isUrgent: Boolean`, `referenceId: String?`, `customMessage: String?`, `customAudioUri: String?` | Scheduled reminders (~5–20) | TodayScreen, ReminderCenterScreen |
| 15 | `MeditationSessionEntity` | `meditation_sessions` | `id: Long` (auto) | None | `techniqueId: String`, `title: String`, `durationMinutes: Int`, `completedAt: Long`, `reflectionNotes: String?`, `status: String` | Logged mindfulness sessions | MeditationScreen |
| 16 | `CustomAlarmEntity` | `custom_alarms` | `id: Long` (auto) | None | `title: String`, `timeMinutes: Int`, `repeatDays: String`, `isEnabled: Boolean`, `vibrateEnabled: Boolean`, `customAudioUri: String?`, `crescendoDurationSeconds: Int`, `missionType: String`, `registeredCodeId: Long?`, `chainedBackupAlarmEnabled: Boolean` | Alarms (~2–10) | AlarmSectionScreen |
| 17 | `RegisteredCodeEntity` | `registered_codes` | `id: Long` (auto) | `codeValue` | `codeValue: String`, `codeFormat: String`, `label: String`, `locationHint: String?`, `isCustomGenerated: Boolean`, `designTemplate: String`, `designConfigJson: String?`, `lastScannedAt: Long?` | Physical barcodes / QR codes | QrCodeStudioScreen, MyCodesLibraryScreen |
| 18 | `HealthConnectDailySummaryEntity` | `health_connect_daily_summaries` | `date: String` | Unique `(date)` | `steps: Long`, `distanceMeters: Double`, `activeCaloriesKcal: Double`, `totalCaloriesKcal: Double`, `sleepDurationMinutes: Int`, `sleepStartTimeEpoch: Long?`, `sleepEndTimeEpoch: Long?`, `restingHeartRateBpm: Double?`, `avgHeartRateBpm: Double?`, `minHeartRateBpm: Double?`, `maxHeartRateBpm: Double?`, `hrvRmssdMs: Double?`, `respiratoryRateRpm: Double?`, `oxygenSaturationPercent: Double?`, `waterMl: Int`, `exerciseMinutes: Int`, `primarySourceApp: String?`, `sourcesListJson: String`, `hasSleepStages: Boolean` | 1 per synced day | HealthConnectDashboardScreen, SleepAnalyticsScreen, HeartRateAnalyticsScreen |
| 19 | `HealthConnectSleepStageEntity` | `health_connect_sleep_stages` | `id: Long` (auto) | `date`, `(stageStartTimeEpoch, stageEndTimeEpoch)` | `date: String`, `sessionStartTimeEpoch: Long`, `stageStartTimeEpoch: Long`, `stageEndTimeEpoch: Long`, `stageType: String` (`DEEP`, `LIGHT`, `REM`, `AWAKE`, `SLEEPING`, `UNKNOWN`), `durationMinutes: Int`, `sourceApp: String?` | 10–40 stages per night | SleepAnalyticsScreen (Hypnogram) |
| 20 | `HealthConnectHeartSampleEntity` | `health_connect_heart_samples` | `id: Long` (auto) | `date`, `timestampEpoch` | `date: String`, `timestampEpoch: Long`, `bpm: Double`, `sourceApp: String?`, `sampleType: String` (`SERIES`, `RESTING`, `WORKOUT`) | Hundreds of samples per day | HeartRateAnalyticsScreen |

---

## 3. Metric-by-Metric Health & Recording Reliability Audit

### 3.1 Sleep Metrics
- **Data Source:** Manual logging via `SleepEntryEntity` OR automated wearable sync via `HealthConnectDailySummaryEntity` & `HealthConnectSleepStageEntity`.
- **Primary Metrics:**
  - Bedtime (minutes from midnight).
  - Wake Time (minutes from midnight).
  - Sleep Duration (minutes).
  - Sleep Quality Rating (1–5).
  - Hypnogram Stages: Deep, Light, REM, Awake duration and percentage.
- **Reliability Assessment:** **EXCELLENT**. Duration handles midnight crossover correctly (`(1440 - bedtimeMin) + wakeTimeMin`).
- **Audit Findings & Deficiencies:**
  - `InsightsScreen` currently only computes a basic 7-day average duration. It lacks bedtime and wake time consistency (standard deviation), which is the most critical circadian marker for users.

### 3.2 Social Rhythm Metric (SRM-5)
- **Data Source:** `routine_entries` joined with `routine_anchors`.
- **Canonical 5 Anchors:**
  1. `out_of_bed`: Getting out of bed (Target default: 07:00 / 420m).
  2. `first_contact`: First social contact / interaction (Target default: 09:00 / 540m).
  3. `main_activity`: Starting primary work/study (Target default: 10:00 / 600m).
  4. `dinner`: Evening meal timing (Target default: 19:30 / 1170m).
  5. `getting_into_bed`: Winding down into bed (Target default: 23:00 / 1380m).
- **Primary Metrics:** Target time, Actual time, Status (`COMPLETED`, `SKIPPED`, `UNRECORDED`), Disruption notes.
- **Reliability Assessment:** **DATA LAYER READY / UI UNIMPLEMENTED**.
- **Audit Findings & Deficiencies:**
  - Although the data layer supports full circadian anchor tracking, the application currently computes **zero SRM metrics**. No regularity score (events within ±45 minutes of target) is provided to help users observe routine stability.

### 3.3 Mood, Energy & Wellness Check-Ins
- **Data Source:** `health_checkins`.
- **Primary Metrics (1–5 discrete scales):**
  - Mood: 1 (Very Low) to 5 (Great).
  - Energy: 1 (Exhausted) to 5 (High).
  - Stress: 1 (Calm / Low) to 5 (Very High).
  - Irritability: 1 (None) to 5 (Severe).
  - Activation Energy: 1 (Sluggish) to 5 (Elevated / Agitated).
  - Freeform symptoms and context notes (`symptomsNotes`).
- **Reliability Assessment:** **GOOD**. Null values are preserved when specific sub-scales are left unrated.
- **Audit Findings & Deficiencies:**
  - `InsightsScreen` displays only the last 5 days as individual rows. There are no weekly averages, no trend indicators (e.g. steady, improving, fluctuating), and no correlation visualizations (e.g. how consistent sleep relates to mood).

### 3.4 Medication Adherence
- **Data Source:** `medications` (schedules) and `medication_intakes` (dose event logs).
- **Primary Metrics:**
  - Scheduled Doses Count for active medications on date $D$.
  - Intake Status: `TAKEN`, `SKIPPED`, `DELAYED`, `MISSED`, `UNRECORDED`.
  - Action Timestamp: Wall-clock epoch when user took medication.
  - Delay Delta: Difference between actual action time and scheduled time.
- **Reliability Assessment:** **VERY HIGH**. Includes immutable snapshot fields (`medicationName`, `dosageSnapshot`) so historical logs remain accurate even if prescriptions are edited or archived.
- **Audit Findings & Deficiencies:**
  - Completely absent from `InsightsScreen`. Users cannot review weekly adherence, on-time frequency, or scheduled vs taken ratios.

### 3.5 Focus Sessions & Deep Work
- **Data Source:** `study_sessions`.
- **Primary Metrics:**
  - Session Duration (minutes).
  - Mode: `POMODORO`, `DEEP_WORK`, `STOPWATCH`, `CUSTOM`.
  - Completion Status: `COMPLETED`, `INTERRUPTED`, `PARTIAL`.
  - Category: DSA, Frontend, Backend, or user custom categories.
- **Reliability Assessment:** **GOOD WITH QUERY DEFECT**.
- **Audit Findings & Deficiencies:**
  - **Defect:** `MainViewModel.recentStudySessions` executes `getRecentStudySessions(limit = 20)`. The `InsightsScreen` filters these 20 sessions by `it.date in past7Days`. If a user completes more than 20 sessions in a week, older sessions are clipped, causing underreported weekly study time. A date-bounded range query (`getSessionsBetweenDates`) must be used instead.

### 3.6 Water & Hydration
- **Data Source:** `water_entries`.
- **Primary Metrics:** Volume (ml), timestamp.
- **Reliability Assessment:** **EXCELLENT**.
- **Audit Findings & Deficiencies:**
  - Monitored on `TodayScreen`, but has no weekly or 7-day trend card on `InsightsScreen`.

### 3.7 Health Connect Biometrics (Wearables)
- **Data Source:** `health_connect_daily_summaries`, `health_connect_sleep_stages`, `health_connect_heart_samples`.
- **Primary Metrics:** Resting HR (bpm), Average HR, Min/Max HR, HRV RMSSD (ms), SpO2 (%), Respiratory Rate (rpm), Daily Steps, Active/Total Calories, Sleep Stages.
- **Reliability Assessment:** **EXCELLENT**. Missing vitals are stored as `null`, avoiding distorted zeros. Source apps (e.g. Samsung Health, Google Fit, Whoop) are attributed.
- **Audit Findings & Deficiencies:**
  - Data is isolated in dedicated analytics sub-screens and absent from the unified `InsightsScreen`.

---

## 4. Summary of Data Integrity Strengths & Action Items

### Strengths:
1. Strict relational constraints and unique composite indexes prevent duplicate entries.
2. Complete absence of network permissions ensures air-gapped user privacy.
3. Preserves nullability for unrecorded days and unentered metrics.
4. Historical medication logs are snapshot-protected against prescription edits.

### Required Action Items for Phase B Implementation:
1. **Fix Study Session Range Query:** Replace `getRecentStudySessions(20)` with `getSessionsBetweenDates(startDate, endDate)` in metric calculations.
2. **Build Dedicated Metric Calculation Engine:** Create `MetricCalculator` domain engine to compute honest weekly statistics for Sleep, SRM, Medication, Mood, and Study.
3. **Expose Missing Observational Cards:** Add SRM Regularity, Medication Adherence, and Multi-Day Mood trends to `InsightsScreen`.
4. **Implement Local Summary Export:** Replace the mock `PlannedExportCard` with functional on-device report generation.
