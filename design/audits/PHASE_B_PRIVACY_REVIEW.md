# Phase B: Privacy Review & Data Flow Security Audit

**Review Date:** November 9, 2026  
**Audited Target:** `com.jayasuriya.rhythm`  
**Auditor:** Principal Privacy Engineer & Security Specialist  
**Status:** Approved Security & Privacy Evaluation

---

## 1. Threat Model & Privacy Boundary Analysis

Rhythm processes highly sensitive personal categories:
- **Circadian & Sleep Data:** Bedtimes, wake times, duration, and wearable hypnograms.
- **Psychiatric & Mood Observations:** Mood ratings (1–5), energy levels, stress, irritability, activation energy, and freeform psychiatric symptom descriptions.
- **Prescription Medication Information:** Medication names, exact dosages, intake schedules, and adherence histories.
- **Cognitive & Personal Thoughts:** Brain Dump notes, study topics, and personal reflections.

### Core Threat Vectors Evaluated:
1. **Network Exfiltration Risk:** Third-party analytics SDKs, crash reporters, or ad libraries transmitting health observations to external endpoints.
2. **Local Storage Tampering / Plaintext Exposure:** Physical access or device backup tools reading SQLite database records directly from flash storage.
3. **Android IPC / Component Redirection:** Malicious apps installed on the device invoking exported activities or content providers to dump personal health metrics.
4. **Summary Export Leakage:** Accidental inclusion of sensitive freeform psychiatric notes when sharing a summary report with a doctor, family member, or coach.

---

## 2. Security & Privacy Audit Findings

### 2.1 Network Isolation (Zero-Network Architecture)
- **Manifest Check:**
  - `android.permission.INTERNET` is **completely omitted** from `AndroidManifest.xml`.
  - Android OS will actively reject socket connections and HTTP requests initiated by the application UID at the kernel level (`Permission denied (errno 13)`).
  - **Status:** **PASS (100% Air-Gapped)**.

### 2.2 Local Storage Encryption at Rest
- **Database Architecture:**
  - Room 2.6.1 operates over **SQLCipher** with 256-bit AES-GCM database encryption (`DatabaseMigrationHelper.kt`).
  - Encryption key is generated via hardware-backed Android KeyStore (`DatabaseKeyManager.kt`), protecting against offline storage extraction via ADB or physical flash memory dumping.
  - **Status:** **PASS**.

### 2.3 Component & IPC Security
- **Manifest Component Verification:**
  - `ActiveAlarmActivity`: Set to `android:exported="false"`, preventing external apps from launching alarms or listening to audio triggers.
  - `HealthConnectManager`: Interacts only with official Google Health Connect IPC (`androidx.health.connect.client`) using explicit permission contracts.
  - No ContentProviders or Services are exported without signature permissions.
  - **Status:** **PASS**.

### 2.4 Personal Report Export Privacy Protections
- **Issue Identified in Mock Export:**
  - The previous planned export card did not distinguish between structured numerical trends (sleep hours, SRM score) and sensitive qualitative text (e.g. `symptomsNotes` like *"Felt overwhelmed and anxious after meeting"*).
- **Mandatory Privacy Safeguard for Phase B Implementation:**
  - **Dual-Level Export Redaction:**
    1. **Numerical & Observational Only (Default):** Includes aggregate metrics, averages, standard deviations, and adherence counts. Strips all qualitative text notes.
    2. **Complete Record (Explicit Opt-In):** User must explicitly check a checkbox: *"Include freeform notes and personal thoughts"* before notes are appended to the export.
  - **No Cloud Upload:** Export is written directly to a user-selected file URI using Android Storage Access Framework (`ACTION_CREATE_DOCUMENT`), avoiding temporary world-readable cache storage.

---

## 3. Privacy Compliance Checklist for Phase B Implementation

| Check | Requirement | Verification Method | Status |
|---|---|---|---|
| **P-01** | Zero external telemetry or analytics | Static code inspection & manifest audit | Verified (No Internet) |
| **P-02** | Local metric calculation only | All calculations performed in Kotlin domain layer | Verified |
| **P-03** | Sensitive note redaction on export | Redaction toggle on export dialog | Required in PR B2 |
| **P-04** | Ephemeral memory handling | Export strings streamed and cleared from memory | Required in PR B2 |
| **P-05** | SAF direct streaming | Stream directly to target Uri; no public temp files | Required in PR B2 |
