# Phase A: Complete Application UI & Screen Inventory

**Audit Date:** November 9, 2026  
**Audited Target:** `com.jayasuriya.rhythm`  
**Auditor:** Principal Android Engineer & UI Architect  
**Methodology:** Comprehensive AST code analysis, live emulator inspection (`emulator-5554`), UI hierarchy XML extraction, and runtime Compose preview analysis.  

---

## 1. Executive Inventory Summary

Rhythm comprises **23 distinct user-facing UI surfaces**:
- **10 Primary Navigation Destinations:** Today, Routine, Health, Study, Meditate, Insights, Brain Dump, Reminders, Alarms, Settings.
- **3 Secondary Deep-Dive Analytics Screens:** Health Connect Dashboard, Sleep Analytics (Hypnogram), Heart Rate & HRV Analytics.
- **2 Dedicated Immersive Activities / Views:** Full-Screen Focus Timer Room, Active Alarm Lockscreen Ringing Activity.
- **6 Advanced Modal Dialogs & Sheets:** Alarm Edit Dialog, Barcode/QR Camera Scanner View, Study Protocol Config Dialog, Focus Studio Audio/Backdrop Dialog, Quick Capture Dialog, Reminder Configuration Dialog.
- **2 System Integration Surfaces:** Floating Brain Dump Overlay Bubble (`FloatingBrainDumpService`), CameraX Barcode Scanner.

---

## 2. Navigation Destination Inventory (`AppDestination`)

The application defines 13 canonical navigation destinations in `com.example.ui.MainViewModel.kt`:

| Destination Enum | Display Label | Primary Source File | ViewModel Bindings | Entry Path |
|---|---|---|---|---|
| `TODAY` | "Today" | `TodayScreen.kt` | `MainViewModel`, `RoutineViewModel`, `HealthViewModel`, `StudyViewModel` | Default launch destination; bottom nav tab 1; top back target |
| `ROUTINE` | "Routine" | `RoutineScreen.kt` | `RoutineViewModel`, `MainViewModel` | Bottom nav tab 2; tablet rail item 2; Next Arc card deep-link |
| `HEALTH` | "Health" | `HealthScreen.kt` | `HealthViewModel`, `MainViewModel` | Bottom nav tab 3; tablet rail item 3; Today check-in prompt |
| `HEALTH_CONNECT` | "Health & Wearables" | `HealthConnectDashboardScreen.kt` | `HealthViewModel`, `MainViewModel` | Deep-link from Today Wearable Card; Health screen link |
| `SLEEP_ANALYTICS` | "Sleep Analytics" | `SleepAnalyticsScreen.kt` | `HealthViewModel`, `MainViewModel` | Health Connect dashboard; Today sleep summary card |
| `HEART_RATE_ANALYTICS` | "Heart Rate Analytics" | `HeartRateAnalyticsScreen.kt` | `HealthViewModel`, `MainViewModel` | Health Connect dashboard cardiac section |
| `STUDY` | "Study" | `StudyScreen.kt` | `StudyViewModel`, `MainViewModel` | Bottom nav tab 4; tablet rail item 4; Today study card |
| `MEDITATION` | "Meditation" (Mind) | `MeditationScreen.kt` | `HealthViewModel`, `MainViewModel` | Bottom nav tab 5; tablet rail item 5 |
| `BRAIN_DUMP` | "Brain Dump" | `BrainDumpScreen.kt` | `BrainDumpViewModel`, `MainViewModel` | Tablet rail item 7; top-bar Brain Dump icon; widget/intent action |
| `REMINDERS` | "Reminders" | `ReminderCenterScreen.kt` | `MainViewModel` | Tablet rail item 8; top-bar More Options dropdown |
| `ALARMS` | "Alarms" | `AlarmSectionScreen.kt` | `AlarmViewModel`, `MainViewModel` | Tablet rail item 9; top-bar Alarms icon on Study/Mind tabs |
| `INSIGHTS` | "Insights" | `InsightsScreen.kt` | `MainViewModel`, `RoutineViewModel`, `HealthViewModel` | Bottom nav tab 6; tablet rail item 6 |
| `SETTINGS` | "Settings" | `SettingsScreen.kt` | `SettingsViewModel`, `MainViewModel` | Tablet rail item 10; top-bar More Options dropdown |

---

## 3. Exhaustive Surface-by-Surface Inventory

---

### Surface 1: Today / Daily Hub Screen
- **Source Files:** 
  - `app/src/main/java/com/example/ui/screens/today/TodayScreen.kt` (1,609 lines)
  - `app/src/main/java/com/example/ui/screens/today/NextEventCircularArcCard.kt` (317 lines)
  - `app/src/main/java/com/example/ui/screens/today/WearableHealthConnectCard.kt` (304 lines)
- **ViewModel Dependencies:** `MainViewModel`, `RoutineViewModel`, `HealthViewModel`, `StudyViewModel`.
- **Key Interactions:**
  - Date navigation: Previous Day, Next Day, Today jump.
  - Next Circadian Anchor: 1-tap "Complete" / "Done Now", "Full Routine" shortcut.
  - Main Study Priority: Quick "Choose Study Priority" dialog launcher, 1-tap timer launch.
  - Quick Thought Parking: "Capture Thought" modal, "View All" Brain Dump navigation.
  - Sleep Summary: "Log sleep" manual entry dialog.
  - Mood & Energy State: "Check In" 5-point slider modal.
  - Medication Intake Schedule: 1-tap "Take Dose", "Skip", "Delay".
  - Wearable Health Summary: Sync trigger, permission prompt, drill-down to Health Connect.
  - Floating Action Button: Quick actions popup menu.
- **States Handled:**
  - *Empty State:* "No Daily Priority Set", "No thoughts parked", "Sleep not recorded yet", "No Check-In Recorded". Handled cleanly with calm prompts.
  - *Loading State:* Health Connect sync progress indicator.
  - *Error State:* Health Connect permission failure banner.
  - *Offline State:* 100% native offline functionality; no network indicator needed.

---

### Surface 2: Routine Schedule & Circadian Anchors Screen
- **Source File:** `app/src/main/java/com/example/ui/screens/routine/RoutineScreen.kt` (472 lines)
- **ViewModel Dependencies:** `RoutineViewModel`, `MainViewModel`.
- **Key Interactions:**
  - Circadian anchor list: Wake up, Morning Light, Main Activity, Wind Down, Bedtime.
  - 1-tap "Log now" (stamps current system timestamp).
  - "Log time" custom time-picker modal.
  - "Skip" anchor action with zero streak penalty.
  - Target time adjustment dialogs with sliders.
  - Circadian guidelines educational expandable card.
- **States Handled:**
  - *Empty State:* Default 5 standard circadian anchors auto-seeded.
  - *Logged State:* Displays exact recorded timestamp with variance badge (e.g., "+15m from target").
  - *Skipped State:* Displays neutral "Skipped" badge without warning colors.

---

### Surface 3: Health Overview & Daily Check-In Screen
- **Source File:** `app/src/main/java/com/example/ui/screens/health/HealthScreen.kt` (788 lines)
- **ViewModel Dependencies:** `HealthViewModel`, `MainViewModel`.
- **Sub-Tabs:** Daily State, Sleep, Hydration, Medications.
- **Key Interactions:**
  - Daily State: 5-level discrete selector cards for Mood, Energy, Stress Level, and Irritability.
  - Sleep: Bedtime/Wake time picker, sleep quality slider (1–5), calculated duration.
  - Hydration: Water intake stepper (+250mL quick-add chips), daily goal progress bar.
  - Medications: Prescribed medications list, schedule cards, adherence logs.
- **States Handled:**
  - *Empty State:* Neutral observational copy explaining unrecorded metrics.
  - *Interactive Feedback:* Instant selection state highlighting with high-contrast borders.

---

### Surface 4: Health Connect & Wearables Dashboard
- **Source File:** `app/src/main/java/com/example/ui/screens/health/HealthConnectDashboardScreen.kt` (787 lines)
- **ViewModel Dependencies:** `HealthViewModel`, `MainViewModel`.
- **Key Interactions:**
  - Permission grant workflow button.
  - Pull-to-refresh / manual sync button.
  - Vitals grid: Steps, Active Calories, Resting Heart Rate, HRV RMSSD, Blood Oxygen, Respiratory Rate.
  - Drill-down navigation to Sleep Analytics and Heart Rate Analytics.
- **States Handled:**
  - *Permission Denied / Revoked State:* Rationale card with direct link to Android Health Connect settings.
  - *Not Installed State:* Guidance card explaining Google Health Connect requirements.
  - *Missing Telemetry:* Unrecorded vitals displayed as `"--"` rather than 0.

---

### Surface 5: Sleep Analytics & Hypnogram Screen
- **Source File:** `app/src/main/java/com/example/ui/screens/health/SleepAnalyticsScreen.kt` (444 lines)
- **ViewModel Dependencies:** `HealthViewModel`, `MainViewModel`.
- **Key Interactions:**
  - Date interval picker (Today, 7-day, 30-day).
  - Canvas-drawn hypnogram timeline showing Deep, Light, REM, and Awake stages.
  - Sleep consistency score and sleep debt calculation.
  - Stage duration breakdown legend pills with percentages.
- **States Handled:**
  - *No Wearable Data:* Clean prompt to log sleep manually.
  - *Manual Entry Only:* Renders flat duration bar without fake hypnogram stages.

---

### Surface 6: Heart Rate & HRV Analytics Screen
- **Source File:** `app/src/main/java/com/example/ui/screens/health/HeartRateAnalyticsScreen.kt` (289 lines)
- **ViewModel Dependencies:** `HealthViewModel`, `MainViewModel`.
- **Key Interactions:**
  - Time-series continuous canvas chart of daily heart rate samples.
  - Daily minimum, maximum, and resting heart rate summaries.
  - HRV RMSSD daily trend point.
- **States Handled:**
  - *Empty State:* Renders calm empty placeholder if wearable data is absent for the day.

---

### Surface 7: Medication Management Components
- **Source File:** `app/src/main/java/com/example/ui/screens/health/MedicationManagementComponents.kt` (1,264 lines)
- **ViewModel Dependencies:** `HealthViewModel`, `MainViewModel`.
- **Key Interactions:**
  - Add Medication Dialog: Name, dosage, frequency, specific schedule times.
  - Intake compliance logging: "TAKEN", "SKIPPED", "DELAYED".
  - Active vs. Inactive medication filtering.
  - Delete medication confirmation with automatic cascading reminder cancellation.
- **States Handled:**
  - *Empty State:* "No active medications scheduled".
  - *Taken State:* Green checkmark badge with confirmed intake time.

---

### Surface 8: Study & Task Management Screen
- **Source File:** `app/src/main/java/com/example/ui/screens/study/StudyScreen.kt` (1,952 lines)
- **ViewModel Dependencies:** `StudyViewModel`, `MainViewModel`.
- **Key Interactions:**
  - Timer Preset Selector: 25m Pomodoro, 50m Deep Work, Custom minutes, Protocol, Stopwatch.
  - Inline countdown timer with Start, Pause, Reset controls.
  - "✨ Concentration Soundscapes & Music" drawer launcher.
  - Task & Category Filter Chip carousel (All, DSA, JavaScript, React, Backend, etc.).
  - Add Task Dialog, Manage Categories Dialog.
  - "Focus Mode" button launching full-screen immersion.
- **States Handled:**
  - *Empty State:* "No Tasks in Category".
  - *Active Timer Draft:* Auto-restores interrupted timer session from Room draft.

---

### Surface 9: Full-Screen Focus Timer Room
- **Source File:** `app/src/main/java/com/example/ui/screens/study/FullScreenFocusTimerScreen.kt` (1,493 lines)
- **ViewModel Dependencies:** `StudyViewModel`, `MainViewModel`.
- **Key Interactions:**
  - 100% viewport coverage with edge-to-edge system bar hiding (`ImmersiveModeHelper`).
  - Circular animated countdown ring with tabular monospace numbers.
  - Top action bar: Ready/Active/Break status indicator, Soundscape selector, Volume popover, Focus Studio launcher, Exit button.
  - Bottom action bar: Start/Pause toggle, End Session confirmation dialog.
  - Quick tools: "Park a Thought" modal, "Session Notes" editor, "Environment" backdrop selector.
- **States Handled:**
  - *Focus Interval State:* Countdown with primary brand colors.
  - *Break Interval State:* Soothing break message and secondary calm color palette.
  - *Session Complete State:* Celebration dialog with total focus minutes and session note save.

---

### Surface 10: Focus Backdrop Audio Studio Dialog
- **Source File:** `app/src/main/java/com/example/ui/screens/study/FocusBackdropAudioStudioDialog.kt` (1,474 lines)
- **Child File:** `FocusAestheticTheme.kt` (918 lines)
- **ViewModel Dependencies:** `StudyViewModel`, `MainViewModel`.
- **Tabs:** Ambient Soundscapes, Offline Music Player, Wallpaper Themes, Online Focus, Environment.
- **Key Interactions:**
  - Soundscape selector: Procedural Brown Noise, Pink Noise, Gentle Rain, Forest Birds, Ocean Waves, Fireplace, etc.
  - Volume slider and master audio toggle.
  - Offline music file picker via Android Storage Access Framework (SAF).
  - Backdrop theme carousel (10 procedural aesthetic themes).
- **States Handled:**
  - *Audio Active State:* Animated equalizer bars on selected soundscape card.

---

### Surface 11: Study Protocol Configuration Dialog
- **Source File:** `app/src/main/java/com/example/ui/screens/study/StudyProtocolConfigDialog.kt` (1,070 lines)
- **ViewModel Dependencies:** `StudyViewModel`, `MainViewModel`.
- **Key Interactions:**
  - Multi-session protocol builder (Focus duration, Micro-break, Section break, Total sessions).
  - Visual interval preview timeline.
  - Custom break naming and guidance prompts.
- **States Handled:**
  - *Input Validation:* Prevents zero or negative interval durations.

---

### Surface 12: Meditation & Guided Breathing Screen
- **Source File:** `app/src/main/java/com/example/ui/screens/meditation/MeditationScreen.kt` (1,196 lines)
- **ViewModel Dependencies:** `HealthViewModel`, `MainViewModel`.
- **Key Interactions:**
  - Breathing pattern selector: Mindful Breathing, Box Breathing (4-4-4-4), 4-7-8 Relaxing, Deep Calm.
  - Interactive expanding/contracting breathing visualizer circle on Canvas.
  - Session timer (3m, 5m, 10m, 15m, 20m).
  - Guidance modes: Spoken Instructions, Breathing Sound Cues, Ambient Audio, Silent Visual Pacing.
  - Voice pacing and speech rate sliders.
- **States Handled:**
  - *Inhale / Hold / Exhale / Rest Transitions:* Smooth animated scale with matching textual cues.

---

### Surface 13: Brain Dump & Fast Capture Screen
- **Source Files:** 
  - `app/src/main/java/com/example/ui/screens/braindump/BrainDumpScreen.kt` (301 lines)
  - `app/src/main/java/com/example/ui/screens/braindump/BrainDumpComponents.kt` (572 lines)
- **ViewModel Dependencies:** `BrainDumpViewModel`, `MainViewModel`.
- **Key Interactions:**
  - Search query bar across thought titles and contents.
  - Tag filter chip carousel (All, Focus, Task, Idea, Question, Archived).
  - Quick capture Floating Action Button.
  - Note card options: Pin, Archive, Edit, Delete, "Convert to Study Task".
  - Floating thought bubble launcher toggle.
- **States Handled:**
  - *Empty State:* "No thoughts parked right now. Unload distracting thoughts anytime."
  - *No Search Results:* "No matching thoughts found."

---

### Surface 14: Quick Capture Modal Dialog
- **Source File:** `app/src/main/java/com/example/ui/screens/braindump/BrainDumpComponents.kt` (Lines 310–540)
- **ViewModel Dependencies:** `BrainDumpViewModel`, `MainViewModel`.
- **Key Interactions:**
  - Fast title and body text entry with autofocus.
  - Tag selector chips.
  - Pin toggle.
  - 1-tap "Convert to Study Task Directly".
- **States Handled:**
  - Validates non-empty title or content before enabling Save.

---

### Surface 15: Floating Brain Dump Overlay Bubble Service
- **Source File:** `app/src/main/java/com/example/service/FloatingBrainDumpService.kt` (724 lines)
- **Key Interactions:**
  - Draggable floating bubble rendered over other apps using `SYSTEM_ALERT_WINDOW`.
  - Expand to quick note input card; collapse to edge magnet on release.
  - Direct local Room database insertion without opening full app.
- **States Handled:**
  - *Permission Missing:* Prompts user to grant overlay permission in system settings.

---

### Surface 16: Reminder Center & Interval Notifications
- **Source Files:** 
  - `app/src/main/java/com/example/ui/screens/reminders/ReminderCenterScreen.kt` (848 lines)
  - `app/src/main/java/com/example/ui/screens/reminders/ReminderDialogs.kt` (1,121 lines)
- **ViewModel Dependencies:** `MainViewModel`.
- **Key Interactions:**
  - Master reminder toggle switch.
  - System notification permission banner and rationale.
  - Quiet hours configuration card (Active window, bypass for critical medications).
  - Sync buttons: "Sync Routines", "Sync Meds", "Deduplicate Alarms".
  - Category filters: All, Sleep & Bedtime, Routine & Meals, Medications, Study, Hydration.
  - Add/Edit Reminder Dialog: Time, repeat days, sound type, TTS voice announcement text.
- **States Handled:**
  - *Permission Denied:* Prompts with direct Android 13+ permission request.

---

### Surface 17: Dedicated Alarms Section Screen
- **Source File:** `app/src/main/java/com/example/ui/screens/alarm/AlarmSectionScreen.kt` (679 lines)
- **ViewModel Dependencies:** `AlarmViewModel`, `MainViewModel`.
- **Tabs:** Alarms, QR Studio, My Codes Library.
- **Key Interactions:**
  - Active alarm cards with toggle switches, crescendo indicators, and dismissal mission tags.
  - "+ New Alarm" button and FAB.
  - "Preview Screen" button launching `ActiveAlarmActivity` in preview mode.
  - "Test Real Ring in 5s" full lifecycle broadcast test.
  - Edit and Delete alarm actions.
- **States Handled:**
  - *Exact Alarm Permission Check:* Warns if `SCHEDULE_EXACT_ALARM` is revoked on Android 12+.

---

### Surface 18: Alarm Creation & Edit Dialog
- **Source File:** `app/src/main/java/com/example/ui/screens/alarm/AlarmEditDialog.kt` (1,724 lines)
- **ViewModel Dependencies:** `AlarmViewModel`, `MainViewModel`.
- **Key Interactions:**
  - Digital 12h/24h time picker with AM/PM selector.
  - Day of week recurrence chips.
  - Sound selector with volume slider and 0–120s crescendo fade-in slider.
  - Dismissal mission selector: Standard Swipe vs. Barcode/QR Code Mission.
  - Backup chained alarm fallback toggle (+5m, +10m).
- **States Handled:**
  - *Code Validation:* Prevents selecting Barcode Mission if no QR/barcodes are registered.

---

### Surface 19: Active Alarm Ringing Activity
- **Source File:** `app/src/main/java/com/example/ui/screens/alarm/ActiveAlarmActivity.kt` (507 lines)
- **Key Interactions:**
  - Full-screen wake lock on lockscreen (`setShowWhenLocked(true)`, `setTurnScreenOn(true)`, `FLAG_KEEP_SCREEN_ON`).
  - Alarm title, current digital clock, and day-of-week display.
  - Audio playback with crescendo volume ramp.
  - Dismissal slider ([`InteractiveSwipeToDismissSlider`](file:///e:/Programming/personal%20app/Rhythm/app/src/main/java/com/example/ui/screens/alarm/InteractiveSwipeToDismissSlider.kt)).
  - Emergency dismissal failsafe (requires 5 consecutive taps).
  - Barcode scanner transition if QR mission is configured.
- **States Handled:**
  - *Lockscreen Presentation:* Fully non-exported (`exported="false"`) launched via `setFullScreenIntent`.

---

### Surface 20: Barcode & QR Camera Scanner View
- **Source File:** `app/src/main/java/com/example/ui/screens/alarm/BarcodeScannerView.kt` (686 lines)
- **Key Interactions:**
  - Live CameraX viewfinder overlay with scanning reticle.
  - Real-time ML Kit / ZXing barcode recognition.
  - Flashlight torch toggle button.
  - Matching code verification against registered Room codes.
  - Emergency manual passkey override option.
- **States Handled:**
  - *Camera Permission Denied:* Shows rationale with settings redirect.
  - *Scan Match Success:* Haptic vibration feedback and alarm shutdown.

---

### Surface 21: QR Code Studio & Registered Codes Library
- **Source Files:** 
  - `app/src/main/java/com/example/ui/screens/alarm/QrCodeStudioScreen.kt` (581 lines)
  - `app/src/main/java/com/example/ui/screens/alarm/MyCodesLibraryScreen.kt` (576 lines)
- **ViewModel Dependencies:** `AlarmViewModel`, `MainViewModel`.
- **Key Interactions:**
  - QR Code generator: Enter label (e.g. "Bathroom Mirror"), generate 2D matrix bitmap.
  - Save to Room library; share or print QR code.
  - Barcode scanner to register existing commercial products (e.g. toothpaste barcode).
  - Delete registered codes.
- **States Handled:**
  - *Empty State:* "No registered codes found. Create a QR code or scan a barcode to use alarm missions."

---

### Surface 22: Insights & Weekly Trends Screen
- **Source File:** `app/src/main/java/com/example/ui/screens/insights/InsightsScreen.kt` (391 lines)
- **ViewModel Dependencies:** `MainViewModel`, `RoutineViewModel`, `HealthViewModel`.
- **Key Interactions:**
  - 7-day observational cards for Sleep Consistency, Mood & Energy, and Study Time.
  - Daily entry presence summaries (e.g. "Sleep: 5 / 7 days recorded").
  - Date navigation stepper.
- **States Handled:**
  - *Missing Data:* Displayed neutrally as "Unrecorded" without negative scores or broken streaks.

---

### Surface 23: Settings & System Integrations Screen
- **Source File:** `app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt` (1,105 lines)
- **ViewModel Dependencies:** `SettingsViewModel`, `MainViewModel`.
- **Key Interactions:**
  - Display Theme: OLED Pitch Black, Standard Dark, Light, Follow System.
  - Audio Preferences: Allow background audio when app minimized, keep screen on.
  - Floating Thought Bubble permission toggle.
  - Database Diagnostics: Table record counts, database integrity check.
  - Backup & Restore: Export JSON (with optional AES-256-GCM password encryption), Import JSON backup (with atomic rollback and Merge mode).
- **States Handled:**
  - *Export/Import Success & Errors:* Clear feedback dialogs with specific error messages for wrong passwords or corrupted files.

---

## 4. Reusable Shared Components Inventory (`CommonComponents.kt`)

Located in `app/src/main/java/com/example/ui/components/CommonComponents.kt` (781 lines):

1. **`RhythmTopBar`:** Universal header supporting screen title, date stepper, Today button, Quick Brain Dump icon, Alarms icon, and More Options dropdown.
2. **`SectionHeader`:** Standardized section titles with optional trailing action or count badge.
3. **`CalmEmptyState`:** Reusable empty-state card with title, supportive body text, optional illustration icon, and optional primary action button.
4. **`StatusBadge`:** Semantic status pills (`Healthy`, `Warning`, `Error`, `Neutral`, `Info`) with high-contrast text and accessible backgrounds.
5. **`CircularArcProgress`:** Canvas-based arc progress indicator used on Today next-event card.
6. **`TimePickerDialogContainer`:** Reusable M3 time picker wrapper dialog.
7. **`ConfirmActionDialog`:** Standard cancellation/confirmation modal dialog.
