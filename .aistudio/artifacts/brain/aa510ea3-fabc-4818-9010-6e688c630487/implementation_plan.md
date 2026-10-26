# Rhythm & Focus — Phase 0 Audit, Bug Fixes & Improvements Plan

This plan documents the deep codebase audit of the existing **Rhythm & Focus** Android application and provides the architectural blueprint for fixing the study timer (Phase 1), implementing database-backed study category management (Phase 2), and establishing the foundation for subsequent Brain Dump, Medication, and Reminder modules.

***

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions have been confirmed and will guide Phase 1 and Phase 2 execution:

- **Timer Engine Strategy**: **Timestamp-based persistence in Room draft storage** — The timer will calculate countdown and elapsed time using wall-clock timestamps (`System.currentTimeMillis()` and `targetEndTimeMillis`) stored in a dedicated `active_timer_draft` table in Room. It will survive background/foreground transitions, screen rotation, app restarts, and system sleep without drift or lost progress.
- **Study Category Seeding**: **Seed default categories into Room with color badges** — The hardcoded categories (`DSA`, `JavaScript`, `React`, `Backend`, `Interview Prep`, `General`) will be migrated into a new `study_categories` Room table with distinct icon identifiers and M3 color tokens upon database initialization.
- **Task Category Deletion Policy**: **Reassign tasks to General category** — When deleting a category, all associated tasks will be safely reassigned to the default `General` category to ensure zero silent data loss and preserve historical study sessions.
- **Execution Scope**: As instructed, implementation begins immediately with **Phase 0 (Audit)** followed by **Phase 1 (Timer Overhaul)** and **Phase 2 (Category CRUD & Task Management)**, tested and verified before proceeding to Phase 3.

***

## PHASE 0 — Codebase Audit

### 1. Confirmed Bugs from Code Inspection

1. **Study Timer Drift & Background Freeze Bug (`MainViewModel.kt` lines 430–454)**:
   - The current timer runs a coroutine with `delay(1000)` and increments an integer `elapsedSeconds + 1`.
   - When the device sleeps, or the app is placed in the background, Android suspends non-essential coroutines, causing the timer to freeze or drift.
   - When returning to the app, the elapsed time does not catch up to actual wall-clock elapsed time.
2. **Timer State Loss on App Kill / Process Recreation**:
   - The timer state exists solely in an in-memory `StateFlow<TimerState>`. If the app process is terminated or recreated during memory pressure or rotation, active progress is permanently lost.
3. **Today Screen "Start Focus Session" Disconnect (`TodayScreen.kt` line 488)**:
   - Clicking "Start Focus Session" on the Today priority card calls `selectTaskForTimer(task)` and navigates to the Study screen, but fails to trigger `startTimer()`. The user arrives at the Study screen with the timer paused, requiring an unexpected second tap.
4. **Hardcoded Study Categories (`StudyScreen.kt` lines 27, 497)**:
   - Categories (`DSA`, `JavaScript`, `React`, `Backend`, `Interview Prep`, `General`) are hardcoded in UI lists without Room backing. Users cannot add, edit, reorder, or archive categories.
5. **Cramped Medication Cards on Today Screen (`TodayScreen.kt` lines 860–920)**:
   - On compact phone screens or narrow split-views, multiple action buttons (`Taken`, `Skip`, `Delayed`) within medication item cards cause severe horizontal squeezing, text clipping, and uncomfortable touch targets.

### 2. Features Implemented but Requiring Verification & Hardening

- **Room Local Persistence**: Schema version 1 operates reliably for basic routine, sleep, and health check-ins, but lacks tables for active timer drafts, categories, brain dump notes, and reminder schedules.
- **Adaptive Tablet Layout**: 2-column layout exists on the Today screen, but needs consistent spacing and density tuning for Xiaomi Pad 7 landscape.

### 3. Missing Features (Phases 1–5)

- Timestamp-anchored timer engine with pause accumulation, background catch-up, and interrupted session recovery.
- Category CRUD with color/icon badges, archive/restore, and safe task reassignment.
- In-app Brain Dump quick capture with Markdown/text preview, search, pin, and task conversion.
- Floating overlay service for external thought capture.
- Advanced medication schedules (multiple times per day, days of week recurrence, intake history correction).
- Centralized Reminder Center with exact alarm scheduling, quiet hours, and notification permission handling.

### 4. Database & Persistence Risks

- Direct entity modifications without database version bumping could cause SQLite migration crashes.
- Room database version must be incremented from `1` to `2` with a structured `Migration(1, 2)` or safe fallback to prevent data loss for existing routine and health records.

### 5. Files Requiring Modification in Phase 1 & 2

- `app/src/main/java/com/example/data/entity/Entities.kt`: Add `ActiveTimerDraftEntity` and `StudyCategoryEntity`.
- `app/src/main/java/com/example/data/dao/AppDaos.kt`: Add `StudyCategoryDao` and `TimerDraftDao`.
- `app/src/main/java/com/example/data/database/AppDatabase.kt`: Register new entities, bump version to `2`, add default category seeds.
- `app/src/main/java/com/example/data/repository/AppRepository.kt`: Add category CRUD, timer draft save/restore, and task reassignment.
- `app/src/main/java/com/example/ui/MainViewModel.kt`: Overhaul timer state machine with timestamp math, auto-restore on launch, and category management flows.
- `app/src/main/java/com/example/ui/screens/study/StudyScreen.kt`: Integrate category manager dialog, full category filtering, and improved timer controls.
- `app/src/main/java/com/example/ui/screens/today/TodayScreen.kt`: Auto-start focus session on Today card click.
- `app/src/test/java/com/example/ExampleRobolectricTest.kt`: Add unit tests for timer calculation, wall-clock resume, and category CRUD.

***

## 1. Overview & Core Concept

Phase 1 and Phase 2 transform the Study & Productivity module into a resilient, production-grade learning companion:
1. **Resilient Focus Timer Engine**: Replaces fragile tick-based coroutines with monotonic wall-clock timestamp calculations (`System.currentTimeMillis()`), persisting the active session in Room so progress is accurately restored even after app exit, phone lock, or device restart.
2. **Database-Backed Category Manager**: Replaces static hardcoded labels with user-defined, reorderable, colored categories backed by Room, with zero data loss when deleting or archiving categories.

***

## 2. User Experience & Visual Design

### Study Timer Workflows (Phase 1)

- **One-Tap Quick Start**: Tapping "Start Focus Session" from the Today priority card immediately initiates the timer and displays the active session.
- **Timer Modes**:
  - *Pomodoro (25m)*: 25-minute focus countdown with 5-minute break prompt.
  - *Deep Work (50m)*: 50-minute deep study interval.
  - *Custom*: User-selected duration from 5 to 180 minutes.
  - *Stopwatch*: Forward count-up with clear lap/interval indicators.
- **Session Lifecycle & Dialogs**:
  - *Running State*: Large digital readout, pulsing focus indicator, clear Pause and Stop buttons.
  - *Background/Return State*: Re-evaluates `(currentTime - startTime)` upon returning to screen; never loses elapsed study time.
  - *Completion*: Gentle alert prompting the user to confirm, edit duration, or discard the session with an optional "What did you learn?" reflection note.
  - *Interruption*: If paused or stopped early, asks whether to save partial study time or discard.

### Study Category Management (Phase 2)

- **Category Palette & Visual Markers**: Categories feature distinctive M3 color badges (e.g. Sage, Amber, Indigo, Teal, Rose, Coral) and icon indicators.
- **Management Dialog**:
  - Create category with name, optional description, and color marker.
  - Edit category name and styling.
  - Reorder categories via simple up/down or priority ranking.
  - Archive obsolete categories without deleting historical study sessions.
  - Safe Deletion with task reassignment dropdown (defaults to `General`).

***

## 3. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      Study Module Architecture                          │
│                                                                         │
│  ┌───────────────────────┐             ┌─────────────────────────────┐  │
│  │   StudyScreen Composable│            │ TodayScreen Priority Card   │  │
│  │   - FocusTimerCard    │             │ - Start Focus (Auto-Launch) │  │
│  │   - CategoryManager   │             └──────────────┬──────────────┘  │
│  │   - TaskItemCRUD      │                            │                 │
│  └───────────┬───────────┘                            │                 │
│              │                                        │                 │
│              ▼                                        ▼                 │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                         MainViewModel                             │  │
│  │  - timerState: StateFlow<TimerState>                              │  │
│  │  - startTimer() / pauseTimer() / resumeTimer() / resetTimer()     │  │
│  │  - Timestamp math: (now - startMillis - totalPausedMillis)        │  │
│  │  - Category CRUD: create, edit, archive, deleteWithReassign       │  │
│  └─────────────────────────────────┬─────────────────────────────────┘  │
│                                    │                                    │
│                                    ▼                                    │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                         AppRepository                             │  │
│  │  - activeTimerDraft: Flow<ActiveTimerDraftEntity?>                │  │
│  │  - studyCategories: Flow<List<StudyCategoryEntity>>               │  │
│  └─────────────────────────────────┬─────────────────────────────────┘  │
│                                    │                                    │
│                                    ▼                                    │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                     Room Database (Version 2)                     │  │
│  │  • active_timer_draft (id, taskId, startTime, targetSec, status)  │  │
│  │  • study_categories (id, name, colorHex, iconName, sortOrder)     │  │
│  │  • study_tasks (reassigned foreign keys, categoryId)              │  │
│  │  • study_sessions (persisted history records)                     │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### Entities & Data Models

1. **`ActiveTimerDraftEntity`**:
   - `id: Int = 1` (Single active record)
   - `taskId: Long?`
   - `taskTitle: String`
   - `category: String`
   - `mode: String` (`POMODORO`, `DEEP_WORK`, `CUSTOM`, `STOPWATCH`)
   - `startTimeMillis: Long`
   - `targetSeconds: Int`
   - `accumulatedElapsedSeconds: Int`
   - `lastPausedTimeMillis: Long?`
   - `status: String` (`IDLE`, `RUNNING`, `PAUSED`, `COMPLETED`)
2. **`StudyCategoryEntity`**:
   - `id: Long = 0` (PrimaryKey autoGenerate)
   - `name: String` (Case-insensitive unique)
   - `description: String?`
   - `colorHex: String`
   - `iconName: String`
   - `sortOrder: Int`
   - `isArchived: Boolean = false`

***

## 4. Implementation Steps & Verification Plan

### Step 1: Database Entities, DAOs & Migrations
- Define `ActiveTimerDraftEntity` and `StudyCategoryEntity` in `Entities.kt`.
- Create `TimerDraftDao` and `StudyCategoryDao` in `AppDaos.kt`.
- Bump Room database version to `2` with non-destructive fallback or migration in `AppDatabase.kt`.
- Seed 6 default categories (`DSA`, `JavaScript`, `React`, `Backend`, `Interview Prep`, `General`) on database creation.

### Step 2: Repository & ViewModel Timer Overhaul
- Update `AppRepository` with draft persistence and category operations.
- Refactor `MainViewModel` timer engine:
  - Anchor running countdown to `System.currentTimeMillis()`.
  - Handle pause/resume with accurate millisecond delta tracking.
  - Automatically restore active draft on ViewModel initialization.
  - Handle session completion and confirmation prompts.

### Step 3: UI Enhancements & Category Manager
- Update `StudyScreen.kt`:
  - Connect timer buttons (`start`, `pause`, `reset`, `record`).
  - Build `CategoryManagementDialog` for creating, editing, and deleting categories.
  - Update `AddStudyTaskDialog` and task edit dialog to use database-backed categories.
  - Fix Today screen priority card action to auto-start the focus session.

### Step 4: Verification & Testing
- Write Robolectric tests covering:
  - Timer timestamp delta calculation across simulated sleep/background intervals.
  - Timer pause, resume, and reset state transitions.
  - Category creation, duplicate name prevention, and safe task reassignment upon deletion.
- Verify compilation with `compile_applet`.
- Execute tests with `gradle :app:testDebugUnitTest`.

***

## 5. Phase 3: Brain Dump Quick Capture & Floating Overlay (Completed & Enhanced)

### Implemented Features:
1. **Persistent Room Storage**:
   - `brain_dump_notes` table added via safe Room migration `MIGRATION_2_3` (Database version 3).
   - Fields: `id`, `title`, `content`, `tag`, `isPinned`, `isArchived`, `createdAt`, `updatedAt`.
   - Automatic preview title generation via `displayTitle()` with smart 35-character cutoff.
2. **Dedicated Brain Dump Hub & Screen**:
   - `BrainDumpScreen.kt`: Search bar for filtering by keyword/tag, tag filter chips (`Idea`, `Study`, `Todo`, `Health`, `Personal`, `Distraction`), separated Pinned Thoughts and Recent Thoughts sections, active vs. archived view toggle.
3. **Modal Quick Capture & In-App Integrations**:
   - `QuickCaptureDialog`: Multi-line text field, optional custom title, tag selector chips, pin toggle, discard protection alert, and direct conversion to study task.
   - **Header TopBar**: Lightbulb action icon accessible from every screen.
   - **Today Dashboard**: `TodayBrainDumpCard` featuring active thought count, pinned/latest thought preview, and quick capture button.
   - **Study Focus Timer**: "Park Distraction / Quick Note" button directly on the active timer card to offload intrusive thoughts without pausing focus.
4. **Enhanced In-Overlay Thought Capture (No App-Switching)**:
   - `FloatingBrainDumpService`:
     - Compact circular emerald coin badge (46dp) with subtle mint border and tuck-to-edge docking.
     - In-overlay capture modal: clicking the floating badge displays an on-screen card directly over third-party apps (PDF reader, web browser, study apps) without opening the main app or interrupting workflow.
     - Includes quick tag selection, multi-line input, direct background Room database persistence (`AppDatabase.brainDumpDao().insertNote(...)`), and gentle native toast feedback.
     - Window focus properly handled so the Android software keyboard opens smoothly within the overlay.
     - Tap-outside or close button dismisses the modal and restores the compact coin.
5. **Automated Testing**:
   - Unit & Robolectric tests in `ExampleRobolectricTest.kt` verifying note creation, auto-title truncation, pinning, archiving, deletion, and study task conversion.

***

## 6. Phase 4: Medication Schedules & Intake History (Completed)

### Implemented Features:
1. **Database Schema & Safe Migration (`MIGRATION_3_4`, Version 4)**:
   - Upgraded `medications` table with:
     - `dosageForm`: Tablet, Capsule, Liquid, Inhaler, Drops, Injection, Patch, Topical, Other.
     - `scheduledTimesJson`: Comma-separated minutes from 00:00 (e.g., `480,1200` for 8:00 AM & 8:00 PM) supporting one or multiple times per day.
     - `repeatDays`: `DAILY` or comma-separated days (e.g. `MON,WED,FRI`).
     - `startDate` and optional `endDate`.
     - `isArchived`: Boolean flag for discontinued schedules.
   - Upgraded `medication_intakes` table with:
     - `medicationName`: Permanent snapshot so deleting or editing a schedule never breaks past logs.
     - `dosageSnapshot`: Dose snapshot at time of recording.
     - `isCorrected`: Retrospective editing tracker.
     - `correctedAt`: Timestamp when retrospective correction was made.
2. **Medication Center (`MedicationManagementComponents.kt`)**:
   - **Today's Schedule Subtab**: Shows all dose slots scheduled for the current day across active medications, with status badges and one-tap action buttons (`Take`, `Skip`, `Delay`).
   - **Medications Manager Subtab**: Full CRUD for medication schedules. Supports autocomplete from previously entered medication names, configurable dosage units (`mg`, `mcg`, `g`, `mL`, `tablet`, `capsule`, `drops`, `puff`, `other`), multiple scheduled times per day, repeat day selectors, reminder toggles, and safe discontinuation/archive preserving past history.
   - **Intake History Log Subtab**: Chronological log of past intake events with timestamps, notes, and retrospective correction dialog.
3. **Today Dashboard Overhaul (`TodayScreen.kt`)**:
   - Replaced cramped cards with a responsive, tablet-friendly card layout that prevents horizontal squeezing and text clipping.
   - Direct `Take`, `Skip`, `Delay` buttons with comfortable touch targets (>= 48dp).
   - Instant inline correction affordance for mis-tapped entries.
4. **Automated Verification**:
   - 14/14 unit & Robolectric tests passing covering multi-time schedule expansion, day-of-week recurrence filtering, retrospective correction tracking, and history preservation on archive.
   - Clean compilation verified via `compile_applet`.

***

## 7. Phase 5: Reminder Center, Alarms & Schedules (COMPLETED)

### Objectives & Delivered Architecture:
1. **Database Schema Version 5 & Room Migration (`MIGRATION_4_5`)**:
   - Created `ReminderEntity` (`reminders` table) with `id`, `title`, `category`, `timeMinutes`, `repeatDays`, `isEnabled`, `soundEnabled`, `vibrateEnabled`, `isUrgent`, `referenceId`, `customMessage`, `createdAt`.
   - Seeded 8 default reminders (`DEFAULT_REMINDERS`) for social rhythm anchors: Wake-Up (7:00 AM), Breakfast (8:00 AM), Mid-Day Hydration (11:00 AM), Lunch (1:00 PM), Afternoon Hydration (3:30 PM), Dinner Anchor (7:30 PM), Bedtime Wind-Down (10:00 PM), and Bedtime Rest (11:00 PM).
   - Created `ReminderDao` with reactive flows, enabled reminder sync, and reference lookup queries.
2. **Native Android Alarm & Notification Engine**:
   - `NotificationHelper.kt`: Prepares 4 dedicated M3 notification channels (`channel_medications` with High Importance & vibration; `channel_routine` with Default Importance; `channel_hydration` with Low Importance; `channel_study` with Default Importance).
   - Direct interactive notification actions: "Mark as Taken" for medication alerts (direct Room intake logging) and "+250ml Water" for hydration checks.
   - `AlarmScheduler.kt`: Handles exact alarm scheduling via `AlarmManager.setExactAndAllowWhileIdle()` when permission is granted, with graceful fallback to `setAndAllowWhileIdle()`.
   - `AlarmScheduler.isWithinQuietHours()`: Robust quiet hours calculation supporting overnight time spans (e.g., 22:00 to 07:00).
   - `ReminderReceiver.kt`: Intercepts `AlarmManager` wakeups, evaluates master enable switch, suppresses non-urgent sounds/vibrations during quiet hours, triggers notification, and reschedules next recurrence.
   - `ReminderActionReceiver.kt`: Executes background notification actions without launching full UI.
   - `BootReceiver.kt`: Restores and reschedules all active alarms upon device reboot (`BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`).
3. **Master Control & Reminder Center UI (`ReminderCenterScreen.kt` & `ReminderDialogs.kt`)**:
   - Master on/off switch to activate or pause all alarms with single tap.
   - Quiet Hours configuration card with start/end time pickers and "Bypass for Critical Medications" toggle.
   - Android 13+ `POST_NOTIFICATIONS` permission check banner and Android 12+ `SCHEDULE_EXACT_ALARM` settings redirect.
   - "Sync from Routine Anchors" and "Sync from Medications" one-tap synchronization buttons.
   - "Test Ring" button to immediately trigger a verification alert.
   - Categorized filter chips (All, Sleep & Bedtime, Routine & Meals, Medications, Hydration & Study).
   - Full Add/Edit reminder modal with AM/PM time picker, daily vs specific week days chips, sound/vibration toggles, and urgent alert switch.
4. **Navigation & Dashboard Integration**:
   - Added `REMINDERS` destination to `AppDestination` enum.
   - Quick access from TopBar bell action button (`onRemindersClick`), Tablet Side Navigation Rail, Today Screen summary card, and Settings screen.
5. **Automated Verification**:
   - 19/19 unit and Robolectric tests passing (`ExampleRobolectricTest.kt`), verifying seeded defaults, Room CRUD, quiet hours evaluation spanning midnight, next trigger calculation, and routine/medication schedule synchronization.
   - Clean compilation verified via `compile_applet`.

***

## 8. Phase 6: Interface and Usability Improvements (COMPLETED)

### Implemented Features:
1. **Adaptive Tablet & Foldable Layouts (Xiaomi Pad 7)**:
   - Evaluated `maxWidth >= 600.dp` across the application shell (`RhythmFocusApp.kt`).
   - Integrated `NavigationRail` for wide tablet screens with direct access to Today, Routine, Health, Study, Insights, Brain Dump, Reminders, and Settings.
   - Constrained all secondary screen content widths (`widthIn(max = 860.dp)`) with `Alignment.TopCenter` so forms, cards, and dashboards maintain optimal readability on landscape screens without unergonomic horizontal stretching.
   - Preserved custom responsive 2-column layout on the `TodayScreen.kt` for simultaneous view of routine timeline, health stats, medication checklist, and study focus tasks.
2. **Accessibility & Touch Target Standards**:
   - Ensured all interactive touch targets meet or exceed 48dp minimum size (`minimumInteractiveComponentSize`).
   - Added explicit `contentDescription` attributes to all icon buttons and illustrations.
   - Unique test tags (`Modifier.testTag(...)`) applied across primary and secondary controls for automated verification.
3. **Calm Visual Styling & Non-Judgmental Feedback**:
   - Applied gentle Material 3 sage/emerald and warm neutral tones, eliminating punishing red error states for missed routines or medication delays.
   - Replaced guilt-inducing wording with objective, supportive observations and next-step actions.
   - Clear visual ripples, interactive chip toggles, and snackbars for user actions.

***

## 9. Phase 7: Persistence and Data Integrity (COMPLETED)

### Implemented Features:
1. **Full Database Serialization (JSON Backup & Restore)**:
   - `DataBackupManager.kt`:
     - `exportBackup(database)`: Serializes entire SQLite Room database across all 13 tables (`routine_anchors`, `routine_entries`, `sleep_entries`, `health_checkins`, `water_entries`, `medications`, `medication_intakes`, `study_categories`, `study_tasks`, `study_sessions`, `brain_dump_notes`, `reminders`, `app_settings`) into structured, human-readable JSON with metadata (`app = "Rhythm & Focus"`, `version = 1`, `schemaVersion = 5`, `exportTimestamp`).
     - `validateBackup(jsonString)`: Validates root JSON structure, app identifier match, and computes entity summary counts without touching database state.
     - `restoreBackup(database, jsonString)`: Executes atomic validation and restoration with Room transaction safety (`withTransaction`). Safely wipes existing records and bulk-inserts all backed-up entities, with automatic re-seeding safeguards for default anchors, study categories, and reminder anchors if missing.
2. **Database Integrity & Table Health Monitoring**:
   - `getDatabaseIntegrityStats(database)`: Real-time query of total row counts across all 13 tables and verification of SQLite consistency.
   - `DatabaseIntegrityStats` data model exposed via `MainViewModel.databaseIntegrityStats` StateFlow.
3. **Settings UI & Dialog Controls (`SettingsScreen.kt`)**:
   - **Data Backup & Restore Card**:
     - "Export Backup" button: Generates JSON, displays a monospace preview, and provides a "Copy to Clipboard" button for easy off-device archival or text file export.
     - "Restore JSON" button: Multi-line text field, "Validate Backup" button displaying a live summary chip of contained records, and "Confirm & Restore" button with transaction safety.
   - **Database Health & Stats Card**:
     - Real-time status badge ("SQLite Integrity: Verified & Consistent").
     - 2-column table counts display for Routine Logs, Sleep Nights, Health Check-ins, Water Entries, Medications & Intakes, Study Tasks & Focus Sessions, Brain Dump Notes, and Active Alarms.
     - "Refresh Stats" button for on-demand table health verification.
   - **Double-Confirmation Reset**:
     - Preserved double-confirmation dialog to erase local data and cleanly re-seed default anchors and categories.
4. **Automated Verification**:
   - 23/23 unit and Robolectric tests passing in `ExampleRobolectricTest.kt`:
     - `exportBackup serializes tables to valid structured JSON with metadata and schema 5`
     - `validateBackup catches corrupt or mismatched JSON and summarizes valid backups`
     - `restoreBackup atomically restores records and safeguards default anchors and categories`
     - `databaseIntegrityStats calculates correct table counts and consistency`
   - Verified clean compilation via `compile_applet`.



