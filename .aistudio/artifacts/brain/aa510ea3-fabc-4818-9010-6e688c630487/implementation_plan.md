# Rhythm & Focus — Comprehensive Engineering Upgrade Plan

This document establishes the audit, architecture, and step-by-step implementation for the comprehensive engineering upgrade of **Rhythm & Focus**.

---

## Phase 0 — Audit and Baseline Summary

### Existing Architecture & Verified Capabilities
- **Language & Runtime**: 100% Kotlin 2.0 with Jetpack Compose & Material 3.
- **Data Persistence**: Room Database (Schema v8), with entities for routines, sleep, health check-ins, water, medications, study tasks, study sessions, brain dump notes, reminders, meditation sessions, and custom alarms.
- **Timer & Persistence**: `ActiveTimerDraftEntity` stores `startWallClockMillis`, `accumulatedElapsedSeconds`, `isRunning`, `isPaused`.
- **Sound Infrastructure**: `StudySoundHelper` synthesizes gentle PCM AudioTrack chimes for start and end; `MeditationAudioHelper` provides TTS and MediaPlayer support.
- **Testing Baseline**: 37 local Robolectric and unit tests currently execute and pass (`BUILD SUCCESSFUL`).
- **Health Connect**: No Health Connect SDK or permissions were currently declared in `build.gradle.kts` or `AndroidManifest.xml`.
- **Theme**: Light & Dark Calming Sage scheme exists in `Theme.kt` and `Color.kt`, with dynamic color support on Android 12+.

---

## Phase 1 — Reliable Study Timer & Micro-Break Scheduler

### Objectives:
1. **Keep-Screen-On Persisted Setting**:
   - Persist setting: `study_keep_screen_on_enabled` (Default: `false` or user toggle).
   - Dynamically apply Android `FLAG_KEEP_SCREEN_ON` on the Window via a disposable Compose effect (`DisposableEffect`) when the focus timer is active and visible.
   - Remove the flag immediately when paused, completed, stopped, navigated away, or disabled.
   - Display battery impact explanation.
2. **Arbitrary Study Sessions & Micro-Breaks Protocol**:
   - Define a rich Protocol model:
     - Total focus duration (e.g. 50 mins, 90 mins).
     - Scheduling method:
       1. *Interval-based*: Repeat short break after every X focus minutes (e.g. every 25m, 5m break).
       2. *Custom Timeline*: Explicit break points entered by elapsed minute (e.g., 12m, 27m, 44m, 61m).
     - Support an arbitrary number of breaks.
     - Break points sorted deterministically, validated, duplicate timestamps rejected, must fit strictly inside total duration.
     - Define whether breaks count toward total session duration with clear UI display.
     - Allow user to skip or extend active breaks (+1m, +5m, or skip break directly).
     - Store configuration in Room (`timer_plan_configurations` or settings).
3. **Timer Engine Correctness**:
   - Wall-clock timestamp math with `startWallClockMillis`, `accumulatedFocusSeconds`, `accumulatedBreakSeconds`, and active state enum (`FOCUS`, `SHORT_BREAK`, `LONG_BREAK`, `COMPLETED`).
   - Break transitions trigger exactly once.
   - Sounds decoupled from Compose recomposition.

---

## Phase 2 — Audio Cues & Meditation Breathing Guidance

### Objectives:
1. **Study Sound Cues Enhancement**:
   - Expand `StudySoundHelper` with independent cues:
     - Session-Start Sound (rising warm chime)
     - Break-Start Sound (soothing soft double tone)
     - Break-End Sound (gentle alert chime)
     - Session-End Sound (resonant Tibetan singing bowl)
   - Independent persistent settings and previews for each sound.
   - Respect audio focus with transient ducking.
2. **Meditation Breathing Guidance Upgrade**:
   - Upgraded `MeditationScreen` and `MeditationAudioHelper`:
     - Selectable modes:
       1. Spoken instructions (TTS)
       2. Subtle sound cues for breathing phases
       3. Optional ambient audio
       4. Optional silent visual pacing
     - Configurable breathing cycle: Inhale (seconds), Exhale (seconds), Natural Pause (seconds).
     - Distinct audio cues for Inhale vs. Exhale.
     - Natural breathing preset.
     - Proper audio focus, lifecycle cleanup, and screen-off listening explanation.

---

## Phase 3 — Health Connect Dashboard Redesign

### Objectives:
1. **Health Connect Integration**:
   - Add `androidx.health.connect:connect-client` dependency and permission declarations (`READ_STEPS`, `READ_SLEEP`, `READ_WEIGHT`, `READ_HYDRATION`).
   - Build `HealthConnectManager` checking availability, permissions, and reading real data.
2. **Dedicated Health Data Dashboard**:
   - Summary cards displaying only real metrics successfully read: Steps, Sleep Duration, Weight, Hydration.
   - Metric cards display: Most recent value, unit, time/date, data source, last sync time, permission status.
   - Refresh / Sync action & Review Permissions action.
   - Weekly and monthly trend visualizations showing missing days as missing (not fabricated zero), legible chart labels, visible date range, no fake sleep stages or hypnograms.
   - Separate wearable data from user-entered logs.

---

## Phase 4 — Modern Material 3 Design System & Dashboard

### Objectives:
1. **OLED Dark Theme**:
   - Pitch black background (`#000000`) for OLED power efficiency.
   - Deep charcoal elevated cards (`#121413` / `#161A17`).
   - Premium Teal / Sage accents with high contrast ratios.
   - Persisted theme preference: `OLED Dark`, `Standard Dark`, `Light`, `Follow System`.
2. **Main Dashboard (Today Screen)**:
   - Circular arc / Agenda card showing the next upcoming event (Alarm, Medication, Routine Anchor, Study Session, Bedtime) with local time and countdown delta, or a clear empty state with an action to create a reminder.
   - Floating Action Button with accessible action menu (Quick health check-in, routine log, study session).
   - Adaptive two-column layout on tablets / wide screens, clean single-column on phones.
3. **Advanced Settings**:
   - Collapsible groups with clean descriptions.
4. **Analytics & Audio Interface**:
   - Weekly mini-charts with real data only.
   - Audio controls card with honest playback progress.

---

## Phase 5 & 6 — Performance Optimization, Testing & Acceptance

### Objectives:
- Eliminate unnecessary recompositions with keys and derivedState.
- Audit coroutines, wake locks, audio track releases, and database query indexes.
- Run complete test suite and verification builds.
- Prepare comprehensive final delivery report.
