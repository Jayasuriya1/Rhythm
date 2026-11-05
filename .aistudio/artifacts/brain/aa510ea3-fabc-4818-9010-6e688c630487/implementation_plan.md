# Rhythm & Focus — Comprehensive Engineering Upgrade Plan

This document establishes the audit, architecture, and completed implementation for the comprehensive engineering upgrade of **Rhythm & Focus**.

---

## Phase 0 — Audit and Baseline Summary

### Existing Architecture & Verified Capabilities
- **Language & Runtime**: 100% Kotlin 2.0 with Jetpack Compose & Material 3.
- **Data Persistence**: Room Database (Schema v11), with entities for routines, sleep, health check-ins, water, medications, study tasks, study sessions, brain dump notes, reminders, meditation sessions, custom alarms, QR registered codes, and Health Connect records.
- **Timer & Persistence**: `ActiveTimerDraftEntity` stores `startWallClockMillis`, `accumulatedElapsedSeconds`, `isRunning`, `isPaused`.
- **Sound Infrastructure**: `StudySoundHelper` synthesizes gentle PCM AudioTrack chimes for start and end; `MeditationAudioHelper` provides TTS and MediaPlayer support.
- **Testing Baseline**: 49 local Robolectric and unit tests execute and pass (`BUILD SUCCESSFUL`).
- **Health Connect**: Full `androidx.health.connect:connect-client:1.1.0` integration with permission declarations and rationales.
- **Theme**: OLED Pitch Black default (`#000000` background, `#101211` / `#171A18` charcoal surfaces, `#86B393` soft sage green, `#E5A970` warm amber, `#9D97C4` muted lavender).

---

## Phase 1 — Reliable Study Timer & Micro-Break Scheduler

### Implemented:
1. **Keep-Screen-On Persisted Setting**:
   - Setting: `study_keep_screen_on_enabled` (Default: `false`).
   - Dynamically applies Android `FLAG_KEEP_SCREEN_ON` on the Window via a disposable Compose effect (`DisposableEffect`) when the focus timer is active and visible.
   - Cleans up immediately when paused, completed, stopped, navigated away, or disabled.
   - Informative battery warning in settings.
2. **Arbitrary Study Sessions & Micro-Breaks Protocol**:
   - Interval-based and custom timeline break scheduling.
   - Break points sorted deterministically, validated, duplicate timestamps rejected.
   - Skip or extend active breaks (+1m, +5m, or skip break directly).
   - Saved in Room database.
3. **Timer Engine Correctness**:
   - Wall-clock timestamp math with `startWallClockMillis`, `accumulatedFocusSeconds`, `accumulatedBreakSeconds`.
   - Single-trigger break transitions decoupled from recomposition.

---

## Phase 2 — Audio Cues & Meditation Breathing Guidance

### Implemented:
1. **Study Sound Cues Enhancement**:
   - Session-Start Sound, Break-Start Sound, Break-End Sound, Session-End Sound.
   - Independent persistent settings and previews.
   - Transient audio focus handling.
2. **Meditation Breathing Guidance Upgrade**:
   - Selectable modes: TTS, subtle tone cues, ambient audio, silent visual pacing.
   - Configurable breathing cycle: Inhale, Exhale, Natural Pause.
   - Lifecycle cleanup and audio focus management.

---

## Phase 3 — Health Connect Dashboard & Wearable Analytics

### Implemented:
1. **Health Connect Integration**:
   - `androidx.health.connect:connect-client:1.1.0` with `minSdk = 26`.
   - Declared permissions: `READ_STEPS`, `READ_DISTANCE`, `READ_TOTAL_CALORIES_BURNED`, `READ_ACTIVE_CALORIES_BURNED`, `READ_SLEEP`, `READ_HEART_RATE`, `READ_RESTING_HEART_RATE`, `READ_HEART_RATE_VARIABILITY`, `READ_RESPIRATORY_RATE`, `READ_OXYGEN_SATURATION`, `READ_HYDRATION`, `READ_EXERCISE`.
   - Multi-source deduplication preserving wearable priority over phone sensors.
2. **Dedicated Health Data Dashboard**:
   - Summary cards displaying real metrics: Steps, Active/Total Calories, Sleep Duration, Heart Rate, Resting HR, HRV RMSSD, SpO2, Respiratory Rate, Hydration, Exercise.
   - Attribution: contributing source app packages displayed clearly.
   - Filter pills (Today, 7 Days, 30 Days).
   - Sleep Analytics Screen with honest hypnogram (only displays stages recorded by wearable; displays empty state if device does not support sleep stages).
   - Heart Rate Analytics Screen with Canvas time-series chart, daily average, genuine resting HR, min/max bounds, and HRV RMSSD.

---

## Phase 4 — Modern Material 3 Design System & OLED Pitch Black

### Implemented:
1. **OLED Dark Theme**:
   - `#000000` pitch black background.
   - Elevated cards `#101211`, `#171A18`, `#1E221F`.
   - Soft sage green (`#86B393`) primary accent, warm amber (`#E5A970`), muted lavender (`#9D97C4`).
   - Persisted theme preference (`OLED_DARK` default).
2. **Main Dashboard (Today Screen)**:
   - Next Event Circular Arc Card displaying countdown, time, and type.
   - Wearable Health Connect summary card on the Today screen.
   - Floating Action Button with quick actions.
   - Adaptive two-column tablet layout (Xiaomi Pad 7).

---

## Phase 5 — Genuine Cloud Sync Architecture

### Implemented:
- `CloudSyncManager` clean architecture with genuine state reporting.
- Checks `google-services.json` availability. When unprovisioned, reports `UNCONFIGURED` without fabricating sync.
- Guarantees local Room SQLite persistence remains active and safe.
- Category sync toggles and honest manual sync trigger.

---

## Phase 6 — Verification & Testing

### Results:
- **Unit & Robolectric Tests**: 49 tests passed (0 failures, 0 errors, 0 skipped).
- **Compilation**: `compile_applet` passed successfully.
- **Platform Metadata**: Verified in sync.
