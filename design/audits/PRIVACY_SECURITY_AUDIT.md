# Rhythm — Stage 4: Privacy & Mobile Security Audit

**Status:** Completed  
**Audit Date:** November 2026  
**Audited Scope:** Personal Data Handling, Android Permissions, Manifest Security, Local Storage Encryption, Exported Components, Logging & Telemetry  
**Target Package:** `com.jayasuriya.rhythm` (`com.example.*`)  
**Evaluation Principle:** Zero-trust security evaluation strictly grounded in Android security architecture and actual implementation evidence.

---

## 1. Executive Summary

Rhythm is designed as a privacy-first, offline-capable application. It stores deeply personal and sensitive user information—including psychiatric and mood assessments, prescription medication schedules and compliance history, continuous cardiac telemetry, sleep patterns, and uninhibited thoughts captured in Brain Dump notes.

### Overall Privacy & Security Assessment
- **Privacy Posture:** **STRONG (8.5/10)**. No third-party trackers, no analytics SDKs, no ad networks, no unauthorized telemetry, and an explicit refusal to implement false cloud sync.
- **Security Posture:** **FAIR (6.0/10)**. A critical exported activity vulnerability was discovered in `AndroidManifest.xml` (`ActiveAlarmActivity`), alongside unencrypted local storage at rest and cleartext backup file exports.

---

## 2. Sensitive Personal Data Inventory

The application processes and persists the following categories of sensitive user data:

| Category | Specific Attributes | Sensitivity Level | Regulatory / Privacy Impact |
|---|---|---|---|
| **Prescription Medications** | Drug names, dosage amount, unit, instructions, intake timestamps, skip/delayed flags, dosage forms. | **CRITICAL** | Protected Health Information (PHI) / Special Category Data under GDPR Art. 9. |
| **Psychiatric & Mood Metrics** | Mood score (1–5), energy (1–5), stress (1–5), irritability (1–5), activation energy (1–5), symptom notes. | **CRITICAL** | Highly sensitive mental health telemetry. |
| **Biometric & Cardiac Vitals** | Heart rate time series (bpm), resting heart rate, HRV (RMSSD), respiratory rate, blood oxygen (SpO2). | **HIGH** | Biometric and physiological data imported via Health Connect. |
| **Sleep Patterns** | Bedtime, wake time, sleep duration, quality rating, and detailed hypnogram stages (Deep, Light, REM, Awake). | **HIGH** | Personal routine and behavioral habits. |
| **Private Thoughts (Brain Dump)** | Unfiltered thought captures, idea notes, personal tags ("Distraction", "Idea", "Personal"). | **HIGH** | Personal cognitive and private journal records. |
| **Household Barcode Mappings** | Physical barcode digits, custom labels, and location hints (e.g. "Kitchen top pantry", "Bathroom medicine cabinet"). | **MEDIUM** | Physical layout and household possession data. |
| **Circadian Routine Anchors** | Wake-up times, first social contact, work blocks, dinner times, and routine compliance. | **MEDIUM** | Daily schedule and lifestyle habits. |

---

## 3. Vulnerability Findings by Severity

### Finding SEC-01: Exported Activity Without Permission Gate or Intent Filter (`ActiveAlarmActivity`)
- **Exact File:** `app/src/main/AndroidManifest.xml` (Lines 96–106)
- **Component:** `<activity android:name=".ui.screens.alarm.ActiveAlarmActivity" android:exported="true" ... />`
- **Severity:** **CRITICAL**
- **Type:** Verified Security Vulnerability
- **Code Evidence:**
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
- **Vulnerability Mechanism:**
  1. `ActiveAlarmActivity` is explicitly declared with `android:exported="true"`, but contains **no `<intent-filter>`** and **no `android:permission`** gate.
  2. Because it has `android:showWhenLocked="true"` and `android:turnScreenOn="true"`, **any third-party app installed on the device** (even one with zero permissions) can issue an explicit Intent:
     ```kotlin
     val intent = Intent().apply {
         setClassName("com.jayasuriya.rhythm", "com.example.ui.screens.alarm.ActiveAlarmActivity")
         putExtra("extra_alarm_id", 1L)
         flags = Intent.FLAG_ACTIVITY_NEW_TASK
     }
     context.startActivity(intent)
     ```
  3. This allows an unauthorized third-party application to:
     - Arbitrarily wake the device screen and display the full-screen alarm overlay over the lock screen.
     - Interfere with the user's alarms, force dismiss alarms, or manipulate the alarm state machine.
- **Remediation:**
  Change `android:exported="true"` to `android:exported="false"`. `ActiveAlarmActivity` is only ever launched internally by `CustomAlarmReceiver` and `CustomAlarmPlaybackService` within the same package. It must never be exported.

---

### Finding SEC-02: Unencrypted Local Storage at Rest (Plaintext SQLite)
- **Exact File:** `app/src/main/java/com/example/data/database/AppDatabase.kt` (Lines 360–368)
- **Component:** `rhythm_focus_database`
- **Severity:** **HIGH**
- **Type:** Potential Risk / Architectural Limitation
- **Code Evidence:**
  ```kotlin
  val instance = Room.databaseBuilder(
      context.applicationContext,
      AppDatabase::class.java,
      "rhythm_focus_database"
  )
  ```
- **Vulnerability Mechanism:**
  The SQLite database file `rhythm_focus_database` is stored unencrypted in `/data/data/com.jayasuriya.rhythm/databases/`. While Android sandboxing protects private internal storage on non-rooted devices:
  1. On rooted devices or devices running custom ROMs, all medication records, psychiatric check-in scores, and thoughts are readable in plaintext.
  2. In local device backup scenarios (`adb backup` if enabled) or file inspection tools, private medical data can be accessed without a cryptographic key.
- **Remediation:**
  Evaluate optional SQLCipher integration (`net.zetetic:sqlcipher-android`) with user opt-in, or utilize the Android Keystore to encrypt sensitive database fields (such as medication names, symptoms notes, and thought contents).

---

### Finding SEC-03: Cleartext Export of Sensitive Medical & Psychiatric Data
- **Exact File:** `app/src/main/java/com/example/util/DataBackupManager.kt` (Lines 59–348)
- **Function:** `DataBackupManager.exportBackup()`
- **Severity:** **HIGH**
- **Type:** Privacy & Data Leakage Risk
- **Code Evidence:**
  `exportBackup()` produces an unencrypted, plaintext JSON string containing:
  - Exact prescription medication names, dosages, and compliance timestamps.
  - Daily mood, stress, energy, and psychiatric symptom notes.
  - Full text of all Brain Dump thoughts.
- **Vulnerability Mechanism:**
  When the user saves this backup file to the device's public Downloads or Documents directory (via Storage Access Framework), any application with storage access permissions or any cloud sync tool (Google Drive, Dropbox, Nextcloud) can read the user's sensitive medical history and psychiatric notes in cleartext.
- **Remediation:**
  Offer optional password-based AES-256-GCM encryption on exported `.rhythmbackup` files using PBKDF2/Argon2 key derivation.

---

### Finding SEC-04: Deprecated Full WakeLock Usage Without Guaranteed Immediate Release
- **Exact Files:**
  - `app/src/main/java/com/example/receiver/CustomAlarmReceiver.kt` (Lines 33–39)
  - `app/src/main/java/com/example/service/CustomAlarmPlaybackService.kt` (Lines 78–85)
- **Severity:** **MEDIUM**
- **Type:** Battery & System Resource Risk
- **Code Evidence:**
  ```kotlin
  val screenWakeLock = powerManager?.newWakeLock(
      PowerManager.FULL_WAKE_LOCK or
              PowerManager.ACQUIRE_CAUSES_WAKEUP or
              PowerManager.ON_AFTER_RELEASE,
      "rhythmfocus:alarm_receiver_wakelock"
  )
  screenWakeLock?.acquire(3 * 60 * 1000L) // 3-minute screen activation
  ```
- **Vulnerability Mechanism:**
  `PowerManager.FULL_WAKE_LOCK` has been deprecated since API level 17. Holding a 3-minute full wake lock forcibly keeps the display backlight on even if the user places the phone in their pocket or dismisses the alarm through headphones, leading to unnecessary battery drain.
- **Remediation:**
  Use `FLAG_KEEP_SCREEN_ON` on the `ActiveAlarmActivity` window combined with `setTurnScreenOn(true)` and `setShowWhenLocked(true)`, and acquire only a short `PARTIAL_WAKE_LOCK` for background playback.

---

### Finding SEC-05: Sensitive Operational Logging to Logcat
- **Exact Files:**
  - `app/src/main/java/com/example/util/CustomAlarmScheduler.kt` (Lines 65, 92, 130)
  - `app/src/main/java/com/example/receiver/CustomAlarmReceiver.kt` (Line 28)
- **Severity:** **LOW**
- **Type:** Information Disclosure Risk
- **Code Evidence:**
  ```kotlin
  Log.i(TAG, "Custom alarm received: id=$alarmId, title=$alarmTitle, isBackup=$isBackup")
  Log.i(TAG, "Scheduled exact alarm ${alarm.id} for $triggerMillis (${alarm.formattedTime()})")
  ```
- **Vulnerability Mechanism:**
  Alarm titles can contain personal medication reminders or sensitive appointments (e.g. "Therapy Session", "Take Lithium 300mg"). Emitting alarm titles to Android system Logcat allows any process with `READ_LOGS` on older platforms or ADB access to read user alarm labels.
- **Remediation:**
  Strip operational string titles from release Logcat statements, logging only opaque numeric IDs.

---

## 4. Manifest & Permissions Audit

### 4.1 Permission Analysis
| Permission | Protection Level | Declared Usage | Compliance Status |
|---|---|---|---|
| `android.permission.VIBRATE` | Normal | Haptic feedback on alarms and timer intervals. | **Compliant** |
| `android.permission.POST_NOTIFICATIONS` | Dangerous | Runtime notification posting on Android 13+. | **Compliant** (Requested in context) |
| `android.permission.SCHEDULE_EXACT_ALARM` | Normal / App-op | Scheduling exact alarms for routine anchors and wake-up. | **Compliant** (Checked with `canScheduleExactAlarms()`) |
| `android.permission.USE_EXACT_ALARM` | Normal | Alarm and timer execution. | **Compliant** |
| `android.permission.USE_FULL_SCREEN_INTENT` | Normal | Displaying incoming alarm over lock screen. | **Compliant** |
| `android.permission.CAMERA` | Dangerous | Barcode / QR scanner dismissal mission. | **Compliant** (Scoped to camera preview only; no storage) |
| `android.permission.SYSTEM_ALERT_WINDOW` | Special App-op | Optional floating Brain Dump thought bubble. | **Compliant** (Opt-in; guarded by `Settings.canDrawOverlays()`) |
| `android.permission.health.READ_*` | Special Health | 12 granular read permissions for Google Health Connect. | **Compliant** (Requested via `HealthPermission.getReadPermission`) |

### 4.2 Storage Permission Compliance
- The application **does not** request `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, or `MANAGE_EXTERNAL_STORAGE`.
- Media and wallpaper selection uses the zero-permission Android Photo Picker and Storage Access Framework (`ActivityResultContracts.OpenDocument` / `PickVisualMedia`), complying fully with Google Play Developer Program policies.

### 4.3 Network & Cloud Security
- **Internet Permission:** `android.permission.INTERNET` is not explicitly declared in `AndroidManifest.xml`.
- **Network Traffic:** Verified zero HTTP/HTTPS network calls, zero third-party analytical endpoints, and zero unauthenticated telemetry.
- **Cloud Sync:** `CloudSyncManager.kt` explicitly avoids network connections until authenticated transfer with user consent is engineered.

---

## 5. Privacy & Security Recommendations Summary

1. **Immediate P0 Fix:** Set `android:exported="false"` on `ActiveAlarmActivity` in `AndroidManifest.xml`.
2. **Deprecate `FULL_WAKE_LOCK`:** Replace with `FLAG_KEEP_SCREEN_ON` on the Activity window and short `PARTIAL_WAKE_LOCK` in the playback service.
3. **Add Optional Backup Encryption:** Provide password-derived AES-GCM encryption on exported JSON backups.
4. **Sanitize Logcat:** Remove sensitive user-defined alarm titles from log statements.
5. **Evaluate SQLCipher:** Assess on-device database encryption for prescription and psychiatric data.
