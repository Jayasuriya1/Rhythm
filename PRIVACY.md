# Privacy Policy & Data-Flow Inventory for Rhythm

**Last Updated:** November 2026  
**Application ID:** `com.jayasuriya.rhythm`  
**License:** Apache License 2.0  

---

## 1. Core Privacy Philosophy

Rhythm is designed as an **offline-first, zero-telemetry personal companion** for social rhythm stabilization, sleep tracking, medication schedules, and focused study. We believe personal wellness and cognitive data belong entirely to the user.

- **Zero Third-Party Trackers:** No ad networks, no analytics SDKs (Google Analytics, Mixpanel, etc.), and no tracking pixels are embedded in the app.
- **Zero Unsanctioned Cloud Transmission:** No personal records, medication logs, mood check-ins, or thought notes are transmitted across the network.
- **No Background Telemetry:** The app operates completely without an active internet connection.

---

## 2. Comprehensive Data-Flow Inventory

| Data Domain | Local Storage & Technology | Network Transmission | User Controls & Encryption |
|---|---|---|---|
| **Social Rhythm Anchors & Routine Entries** | Local Room SQLite Database (`rhythm_focus_database`) | **None (0% egress)** | Stored on-device. Encrypted at rest via SQLCipher AES-256 with Android Keystore. |
| **Prescription Medications & Intake Logs** | Local Room SQLite Database (`medications`, `medication_intakes`) | **None (0% egress)** | Stored on-device. Encrypted at rest via SQLCipher AES-256. |
| **Mental Health & Daily Check-Ins** | Local Room SQLite Database (`health_checkins`) | **None (0% egress)** | Stored on-device. Encrypted at rest via SQLCipher AES-256. |
| **Brain Dump (Distraction Notes)** | Local Room SQLite Database (`brain_dump_notes`) | **None (0% egress)** | Stored on-device. Encrypted at rest via SQLCipher AES-256. |
| **Cardiac, Sleep & Activity Vitals** | Local Room SQLite Database + Google Health Connect | **None (0% egress)** | Read via Android Health Connect API upon explicit user permission grant. Stored locally on-device. |
| **Study Sessions & Category Tags** | Local Room SQLite Database (`study_sessions`, `study_categories`) | **None (0% egress)** | Stored on-device. Encrypted at rest via SQLCipher AES-256. |
| **Backup / Export Files** | User-selected storage directory via Android Storage Access Framework (SAF) | **None (0% egress)** | User-initiated export. Protected with optional AES-256-GCM authenticated encryption (PBKDF2-HMAC-SHA256, 100,000 iterations). |
| **Cloud Synchronization** | `CloudSyncManager` | **None (Inactive)** | Cloud sync is strictly unconfigured and unavailable (`UNCONFIGURED`). Manual sync returns an explicit failure informing the user that data remains on-device. |
| **AI Capabilities (Gemini / Firebase AI)** | Declarative Gradle SDK dependencies | **None (Inactive)** | No AI calls are executed in the application. Any future AI feature will be explicit opt-in with complete payload disclosure prior to transmission. |

---

## 3. Android Permissions Usage

The app declares only the minimum set of permissions necessary to deliver local alarm, timer, and health features:

1. **`SCHEDULE_EXACT_ALARM` & `USE_EXACT_ALARM`:** Required for firing circadian anchor notifications and wake-up alarms at the exact scheduled minute.
2. **`VIBRATE`:** For tactile alarm alerts and study session interval haptics.
3. **`POST_NOTIFICATIONS`:** For posting local system notifications for scheduled routine anchors.
4. **`USE_FULL_SCREEN_INTENT` & `WAKE_LOCK`:** For waking the device screen and presenting the full-screen alarm overlay over the lock screen.
5. **`SYSTEM_ALERT_WINDOW`:** Opt-in floating Brain Dump thought bubble over other apps. Scoped strictly to overlay rendering; never reads screen contents or third-party app state.
6. **`CAMERA`:** Scoped solely to the local barcode/QR scanner viewfinder for morning alarm dismissal missions. No camera frames, images, or barcodes are ever saved to disk or transmitted over the network.
7. **Health Connect Read Permissions (`READ_STEPS`, `READ_SLEEP`, `READ_HEART_RATE`, etc.):** Read-only access to import daily wellness metrics into local SQLite storage. Requires explicit user consent via Android's native Health Connect permissions dialog.

---

## 4. Security & Encryption Standards

- **Database at Rest:** Encrypted with **SQLCipher (AES-256)**. Database key is dynamically generated (256-bit secure random) and wrapped using the **Android Keystore** (`AndroidKeyStore`, AES-GCM).
- **Exported Backups:** Structured JSON backups support optional **AES-256-GCM** encryption with 16-byte random salt, 12-byte random IV, and PBKDF2WithHmacSHA256 key stretching (100,000 rounds).
- **Component Security:** `ActiveAlarmActivity` is non-exported (`android:exported="false"`), preventing unauthorized third-party apps from waking the screen or hijacking alarm states.

---

## 5. Contact & Audits

For questions or security audit inquiries regarding Rhythm's privacy architecture:
- GitHub Repository: [https://github.com/Jayasuriya1/Rhythm](https://github.com/Jayasuriya1/Rhythm)
- Architecture Audits: See `design/audits/PRIVACY_SECURITY_AUDIT.md`

