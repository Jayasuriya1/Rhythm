# Rhythm & Focus

> **A calm, mindful companion for daily routines, sleep & health tracking, medication management, focused study, and reliable custom alarms.**

**Rhythm & Focus** is an offline-first Android application built with **Modern Jetpack Compose**, **Kotlin Coroutines & Flow**, and **Room Database**. It empowers users to establish stabilizing circadian rhythms, conduct focused study blocks, manage daily medications and health check-ins, and wake up reliably with fail-safe behavioral alarms.

---

## 📱 Core Features & Architecture

### 1. ⏰ Dedicated Custom Alarm System
Engineered for deep-sleepers and discipline-focused routines, providing hardware-level reliability and failsafe dismissals:
- **Lock-Screen Activation:** Bypasses system keyguards and wakes device displays instantly using `setShowWhenLocked(true)`, `setTurnScreenOn(true)`, and `FLAG_KEEP_SCREEN_ON` via `ActiveAlarmActivity`.
- **Custom Wallpapers & Scrim Overlays:** Personalize alarm screens with custom gallery photos or choose from 5 curated dynamic gradients (*Midnight Indigo*, *Sunrise Gold*, *Deep Forest*, *Sunset Crimson*, *Aurora Borealis*).
- **Layered Clock Typography:** 4 typography clock styles optimized for glanceable legibility:
  - *Modern Digital* (clean, ultra-legible modern font)
  - *Terminal Monospace* (retro terminal aesthetics)
  - *Classic Serif* (timeless editorial styling)
  - *Elegant Minimalist* (contemporary, spacious design)
- **QR / Barcode CAPTCHA Mission (Optional):** Integrated real-time **CameraX** and **ZXing** barcode analyzer. Prevent alarm silencing until a registered physical item's barcode (e.g., toothpaste, coffee mug, book) is scanned.
- **Physical Volume & Action Interception:** Intercepts hardware volume buttons and disables back/recent navigations to eliminate accidental muting before mission completion.
- **Chained Backup Alarm:** Automated secondary alarm generator scheduling an aggressive fallback alert exactly 5 minutes after the primary alarm if dismissed or ignored.
- **Linear Volume Fade-In (Crescendo):** Smoothly transitions volume from silence to full volume across selectable windows (Off, 30s, 60s, 2m, or 5m).
- **Local Audio Picker & Looping:** Select custom audio files directly from device storage or use system tones, looped seamlessly through a persistent media foreground service (`CustomAlarmPlaybackService`).
- **Exact Alarming & Boot Survival:** Utilizes `AlarmManager.setAlarmClock()` to pierce low-power Doze mode down to the second, automatically restored on device reboot via `BootReceiver`.

---

### 2. 🌅 Social Rhythm Metric (SRM) & Routine Anchors
Stabilize circadian rhythms through daily social zeitgebers:
- **5 Core Anchors:** Out of Bed, First Social Contact, Main Activity, Dinner, and Getting into Bed.
- **Target vs. Actual Deltas:** Automatic calculation of daily variances (e.g., `+15m`, `-10m`, `On time`).
- **Circadian Stability Index:** Real-time percentage scoring of daily habit consistency.
- **Smart Reminders & Quiet Hours:** Configurable anchor reminders with quiet hours suppression and customizable Text-to-Speech (TTS) voice announcements.

---

### 3. 🎯 Deep Focus & Study Management
Structured productivity powered by the Pomodoro technique:
- **Flexible Timer Modes:** Pomodoro (25m), Short Break (5m), Long Break (15m), Stopwatch, and Custom Timers.
- **Resilient Background State:** Timer drafts automatically sync to Room DB, surviving process death and orientation changes with wall-clock catch-up.
- **Task Organization & Categorization:** Color-coded study categories (*DSA*, *JavaScript*, *React*, *Backend*, *Interview Prep*, *General*, and user-created categories) with daily priorities.
- **Study Log Analytics:** Track completed study minutes, category distributions, and post-session reflection notes.
- **Audio Chimes:** Optional start and completion bell sounds.

---

### 4. 💡 Brain Dump & Distraction Scratchpad
Protect focus by rapidly offloading intrusive thoughts:
- **Instant Distraction Capture:** Fast modal or drawer capture without breaking timer flow.
- **Tagging & Pinning:** Organize notes by tags (*Study*, *Idea*, *Task*, *Health*, *Distraction*) and pin critical items.
- **One-Tap Task Conversion:** Seamlessly promote brain dump thoughts into structured study tasks with estimated duration and category assignment.

---

### 5. 💊 Medication & Supplement Schedule
Accurate adherence tracking with multi-dose schedules:
- **Multi-Time Regimens:** Flexible daily dosages (e.g., 8:00 AM & 8:00 PM), dosage forms (capsule, tablet, liquid), and instructions.
- **Intake Logging & Retrospective Edits:** Log doses as *Taken* or *Skipped* with timestamps, including retroactive adjustments for missed entries.
- **Refill Tracking & History:** Low-supply warnings and adherence history charts.

---

### 6. 🌙 Sleep & Health Telemetry
Holistic wellness tracking linked to your daily rhythm:
- **Sleep Quality Log:** Track bedtime, wake time, duration, sleep debt relative to personal targets, and quality ratings (1–5 stars).
- **Daily Health Check-Ins:** Gauge mood, energy, stress, irritability, and activation energy.
- **Hydration Tracker:** Visual daily water intake progress with quick-add increments.

---

### 7. 🧘 Guided Meditation & Mindfulness
Mindful resets tailored for high-focus work:
- **5 Guided Sequences:** Mindful Breathing, Body Scan, 5-4-3-2-1 Sensory Grounding, Mindful Observation, and Study Break Reset.
- **Calming Visual Pacer:** Step-by-step calming prompts with ambient timer and post-session reflections saved to history.

---

### 8. 🔒 Local-First Data Sovereignty & Portability
- **100% Offline & Private:** All data resides strictly on the local device in an encrypted Room SQLite database—no mandatory cloud accounts or external servers.
- **Atomic Backup & Restore:** Complete JSON export and import via `DataBackupManager` with validation and versioning safeguard.

---

## 🛠 Tech Stack & Architecture

| Layer | Technologies |
|---|---|
| **Language** | Kotlin 2.0+ (100% Kotlin) |
| **UI Framework** | Jetpack Compose, Material Design 3 (M3) |
| **Architecture** | MVVM (Model-View-ViewModel) + Clean Architecture |
| **Concurrency** | Kotlin Coroutines, StateFlow, SharedFlow |
| **Local Database** | Room Database (KSP, Foreign Keys, Version 8 Migrations) |
| **Camera & Scanning** | AndroidX CameraX (Core, Camera2, Lifecycle, View 1.4.1) + ZXing Core 3.5.3 |
| **Audio & Media** | Android MediaPlayer, AudioFocusManager, Media Playback Foreground Service |
| **System Scheduling** | Android `AlarmManager` (`setAlarmClock`, `canScheduleExactAlarms`), `BroadcastReceiver` |
| **Unit Testing** | Robolectric, JUnit 4, AndroidX Test, Kotlinx Coroutines Test |

---

## 📂 Project Structure

```
app/src/main/
├── AndroidManifest.xml              # Permissions, Services, Receivers & Activities
├── java/com/example/
│   ├── MainActivity.kt              # App entry point, edge-to-edge setup, root Scaffold
│   ├── data/
│   │   ├── database/AppDatabase.kt  # Room database definition (Schema v8), seeders
│   │   ├── entity/Entities.kt       # All entity definitions (Alarms, Routines, Sleep, Tasks, Meds)
│   │   ├── dao/AppDaos.kt           # Room DAOs with reactive Flow queries
│   │   └── repository/AppRepository.kt # Centralized repository & business coordination
│   ├── receiver/
│   │   ├── CustomAlarmReceiver.kt   # Triggers alarm playback & launches full-screen UI
│   │   ├── BootReceiver.kt          # Re-registers exact alarms on reboot
│   │   └── AlarmReceiver.kt         # Routine anchor & medication notifications
│   ├── service/
│   │   └── CustomAlarmPlaybackService.kt # Foreground media service with volume crescendo
│   ├── ui/
│   │   ├── MainViewModel.kt         # Central UI state management & flows
│   │   ├── RhythmFocusApp.kt        # Primary navigation & modal dialog router
│   │   ├── screens/
│   │   │   ├── alarm/
│   │   │   │   ├── ActiveAlarmActivity.kt # Full-screen lock-screen dismiss view
│   │   │   │   ├── AlarmSectionScreen.kt  # Alarms list & management screen
│   │   │   │   ├── AlarmEditDialog.kt     # Create/edit alarm dialog with file & barcode pickers
│   │   │   │   └── BarcodeScannerView.kt  # Embedded CameraX & ZXing camera scanner
│   │   │   ├── RoutineScreen.kt     # Social rhythm anchors & stability metrics
│   │   │   ├── StudyScreen.kt       # Pomodoro timer, tasks & category analytics
│   │   │   ├── BrainDumpScreen.kt   # Fleeting thought capture & task conversion
│   │   │   ├── MedicationScreen.kt  # Medication schedule & intake tracker
│   │   │   ├── SleepHealthScreen.kt # Sleep debt & daily wellness check-ins
│   │   │   └── MeditationScreen.kt  # Guided breathing & mindfulness timer
│   │   ├── components/CommonComponents.kt # Top bars, navigation bars, cards & badges
│   │   └── theme/                   # Material 3 colors, typography, shapes
│   └── util/
│       ├── CustomAlarmScheduler.kt  # Exact alarm scheduling & chained backup alarms
│       ├── AlarmScheduler.kt        # Reminder scheduling & quiet hours calculation
│       ├── DataBackupManager.kt     # JSON export, schema validation & atomic restore
│       └── DateTimeUtils.kt         # Delta formatting & date utilities
└── res/                             # Vector drawables, themes, and string resources
```

---

## 🚀 Building & Testing

### Prerequisites
- Android Studio Ladybug / Meerkat or Gradle 8+
- Android SDK 35 (compileSdk 35, targetSdk 35, minSdk 26)
- Java 17 or Java 21 JDK

### Build Commands
```bash
# Compile Debug APK
gradle :app:assembleDebug

# Run Unit & Robolectric Tests (37 comprehensive test suites)
gradle :app:testDebugUnitTest

# Check Codebase Lint
gradle :app:lintDebug
```

---

## 📋 Permissions Declared

- `android.permission.SCHEDULE_EXACT_ALARM` & `USE_EXACT_ALARM`: Guarantees exact-to-the-second alarm firing.
- `android.permission.USE_FULL_SCREEN_INTENT`: Displays alarm activity immediately over lock screens and keyguards.
- `android.permission.WAKE_LOCK`: Lights up and preserves screen activation during active alarms.
- `android.permission.CAMERA`: Required for the real-time QR / Barcode scanning dismissal mission.
- `android.permission.FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Ensures uninterrupted ringtone crescendo playback.
- `android.permission.VIBRATE`: Enables customizable haptic pulse feedback during alarms.
- `android.permission.RECEIVE_BOOT_COMPLETED`: Automatically reschedules alarms following system restarts.

---

## 📄 License
This project is licensed under the Apache 2.0 License. All rights reserved.
