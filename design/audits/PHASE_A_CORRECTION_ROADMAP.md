# Phase A: Implementation & Correction Roadmap

**Status:** Phase A Stage 2 Implementation Complete (PR A1, PR A2, PR A3, PR A4 verified with 100% CI gates green)  
**Audit Source Documents:** 
- `design/audits/PHASE_A_SCREEN_INVENTORY.md`
- `design/audits/PHASE_A_SCREEN_AUDIT.md`
- `design/audits/PHASE_A_ACCESSIBILITY_AUDIT.md`
- `design/audits/PHASE_A_NAVIGATION_AUDIT.md`
- `design/audits/PHASE_A_DASHBOARD_PROPOSAL.md`  
**Package:** `com.jayasuriya.rhythm`  
**Guiding Constraint:** Strictly non-destructive. No database schema changes, zero data loss, zero regression in alarm firing or focus timer loops.  

---

## 1. Implementation Phasing Strategy

To maintain rigorous quality gates and ensure reviewability, the Phase A findings are partitioned into **four discrete, focused Pull Requests (PRs)**:

```
[PR A1: P0] Component Expansion & Theme Color Harmonization
     │       (Migrate 154 hardcoded colors; centralize shared cards & tokens)
     ▼
[PR A2: P1] Navigation Architecture & Backstack Integrity
     │       (Unified top-bar quick actions, hierarchical backstack navigation)
     ▼
[PR A3: P0] Today Dashboard Modernization (Four Daily Moments)
     │       (Concept 1 implementation, progressive disclosure, 1-tap actions)
     ▼
[PR A4: P1] Accessibility, Touch Target & Responsive Polish
             (Resolve NAF warnings, 48dp minimum targets, tablet max-width)
```

---

## 2. Detailed Pull Request Specifications

---

### PR A1: Component Expansion & Theme Color Harmonization
- **Priority:** **P0 (Critical Brand & Theme Integrity)**
- **User Problem:** In Light Theme, `FullScreenFocusTimerScreen` and `BarcodeScannerView` force dark charcoal surfaces and mint text, causing severe contrast failures (1.8:1) and broken visual appearance.
- **Scope:**
  - Migrate all 154 hardcoded `Color(0x...)` occurrences to `MaterialTheme.colorScheme` tokens and centralized semantic tokens in `Color.kt`.
  - Expand `CommonComponents.kt` with reusable `MetricSummaryCard`, `ScreenContainer`, and standard dialog wrappers.
- **Exact Files Affected:**
  - `app/src/main/java/com/example/ui/theme/Color.kt`
  - `app/src/main/java/com/example/ui/components/CommonComponents.kt`
  - `app/src/main/java/com/example/ui/screens/study/FullScreenFocusTimerScreen.kt` (107 colors migrated)
  - `app/src/main/java/com/example/ui/screens/alarm/BarcodeScannerView.kt` (18 colors migrated)
  - `app/src/main/java/com/example/ui/screens/alarm/ActiveAlarmActivity.kt` (11 colors migrated)
  - `app/src/main/java/com/example/ui/screens/alarm/InteractiveSwipeToDismissSlider.kt` (7 colors migrated)
  - `app/src/main/java/com/example/ui/screens/alarm/AlarmEditDialog.kt` (6 colors migrated)
  - `app/src/main/java/com/example/ui/screens/alarm/MyCodesLibraryScreen.kt` (4 colors migrated)
  - `app/src/main/java/com/example/ui/screens/study/StudyScreen.kt` (1 color migrated)
- **Database / Persistence Impact:** **None (0%).** Strictly presentation-layer tokens.
- **Regression Risks:** Low. Must ensure active alarm dismissal threshold and camera scanner reticle remain 100% functional.
- **Tests Required:**
  - Robolectric screenshot tests across OLED Dark, Standard Dark, and Light themes.
  - Existing `AlarmSecurityAndWakeLockTest` and `ExampleRobolectricTest` regression execution.
- **Acceptance Criteria:**
  - Zero hardcoded `Color(0x...)` outside `Color.kt` and procedural atmospheric backdrops.
  - Light mode displays crisp, warm off-white canvas with dark text (contrast ≥ 10:1).
  - All CI gates pass: `:app:assembleDebug`, `:app:lintDebug`, `:app:testDebugUnitTest`.

---

### PR A2: Navigation Architecture & Backstack Integrity
- **Priority:** **P1 (Information Architecture & User Flow)**
- **User Problem:** Alarms and Reminders are hidden or missing from top bars; pressing system back on sub-screens (Sleep Analytics, Health Connect) jumps immediately to Today instead of returning to the parent screen.
- **Scope:**
  - Implement a hierarchical backstack history in `MainViewModel` and `RhythmFocusApp.kt`.
  - Standardize tab labeling ("Meditate" across all phone and tablet surfaces).
  - Ensure Alarms, Reminders, and Brain Dump quick-action icons are uniformly accessible across all screens.
- **Exact Files Affected:**
  - `app/src/main/java/com/example/ui/MainViewModel.kt`
  - `app/src/main/java/com/example/ui/RhythmFocusApp.kt`
  - `app/src/main/java/com/example/ui/components/CommonComponents.kt` (`RhythmTopBar`)
- **Database / Persistence Impact:** None.
- **Regression Risks:** Low. Must verify system back gesture does not trap the user or exit the app unexpectedly.
- **Tests Required:**
  - Robolectric navigation tests verifying backstack popping from `SleepAnalytics` → `HealthConnect` → `Health` → `Today`.
- **Acceptance Criteria:**
  - System back navigation traverses sub-screens hierarchically.
  - Alarms and Reminders are immediately discoverable on every screen.
  - All CI gates pass.

---

### PR A3: Today Dashboard Modernization (Four Daily Moments)
- **Priority:** **P0 (Core Daily Product Experience)**
- **User Problem:** Today screen currently renders 7 massive vertical cards requiring extensive scrolling; morning users face up to 5 empty state cards at once.
- **Scope:**
  - Implement Concept 1 from `PHASE_A_DASHBOARD_PROPOSAL.md`.
  - Daily Pulse header answering *"How am I doing today?"* (Sleep + Mood + Anchors).
  - Contextual time-of-day card answering *"What is coming next?"* (Circadian Anchor + Scheduled Medication).
  - Direct 1-tap logging for routine anchors and medication doses without leaving the Today screen.
  - Progressive disclosure drawer/expandable section for deep wearable health telemetry.
- **Exact Files Affected:**
  - `app/src/main/java/com/example/ui/screens/today/TodayScreen.kt`
  - `app/src/main/java/com/example/ui/screens/today/NextEventCircularArcCard.kt`
  - `app/src/main/java/com/example/ui/screens/today/WearableHealthConnectCard.kt`
- **Database / Persistence Impact:** None. Consumes existing `RoutineViewModel`, `HealthViewModel`, and `StudyViewModel` state flows.
- **Regression Risks:** Medium. Must ensure anchor completion and medication intake timestamps update the database reactively.
- **Tests Required:**
  - Robolectric tests verifying 1-tap anchor completion and medication confirmation from Today screen.
  - Compose preview and screenshot tests for Morning, Daytime, and Evening moments.
- **Acceptance Criteria:**
  - All 3 core questions answered above the fold (≤ 600dp viewport).
  - Zero card-in-card nesting.
  - Empty state consolidated into a single gentle, supportive prompt.
  - All CI gates pass.

---

### PR A4: Accessibility, Touch Targets & Responsive Polish
- **Priority:** **P1 (Universal Usability & Compliance)**
- **User Problem:** Several custom clickable chips and switches fail Android Accessibility Scanner heuristics (`NAF="true"`); tablet layouts stretch text awkwardly across wide viewports.
- **Scope:**
  - Enforce 48 × 48 dp minimum touch bounds across all chips, radio buttons, and toggle switches.
  - Add explicit content descriptions and contextual TalkBack announcements.
  - Constrain wide tablet content to maximum readable width (`widthIn(max = 840.dp)`).
  - Add custom TalkBack action to `InteractiveSwipeToDismissSlider` (double-tap to dismiss).
- **Exact Files Affected:**
  - `app/src/main/java/com/example/ui/screens/routine/RoutineScreen.kt`
  - `app/src/main/java/com/example/ui/screens/meditation/MeditationScreen.kt`
  - `app/src/main/java/com/example/ui/screens/reminders/ReminderCenterScreen.kt`
  - `app/src/main/java/com/example/ui/screens/alarm/AlarmSectionScreen.kt`
  - `app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt`
  - `app/src/main/java/com/example/ui/screens/alarm/InteractiveSwipeToDismissSlider.kt`
- **Database / Persistence Impact:** None.
- **Regression Risks:** Low.
- **Tests Required:**
  - Accessibility scanner automated checks.
  - Font scaling tests at 150% and 200%.
- **Acceptance Criteria:**
  - Zero `NAF="true"` warnings in UI automator / accessibility scanner.
  - 100% WCAG 2.2 AA touch target compliance.
  - Tablet layouts visually balanced with centered readable columns.
  - All CI gates pass.

---

## 3. Approval Gate & Execution Summary

Phase A Stage 2 implementation has proceeded strictly via the four documented, focused PRs:
1. **PR A1:** Component Expansion & Theme Color Harmonization — **COMPLETE** (Verified, 154 hardcoded colors migrated)
2. **PR A2:** Navigation Architecture & Backstack Integrity — **COMPLETE** (`feat-phase-a-pr2-navigation-backstack`, commit `fdce366`)
3. **PR A3:** Today Dashboard Modernization — **COMPLETE** (`feat-phase-a-pr3-today-dashboard`, commit `c63dd7a`)
4. **PR A4:** Accessibility, Touch Targets & Responsive Polish — **COMPLETE** (`feat-phase-a-pr4-accessibility-polish`, commit `784eb54`)

### Phase A Gate Verification
- **Gate 1 (Build & Lint):** `:app:assembleDebug :app:lintDebug --stacktrace` — **100% Green (0 errors, 0 warnings)**
- **Gate 2 (Unit Tests):** `:app:testDebugUnitTest -ProbolectricOffline=false --stacktrace` — **100% Green (128 passed, 0 skipped, 0 failed)**
- **Database & Architecture Protection:** Room schema v11 preserved, zero schema bumps, zero data migrations required, zero regression in alarm scheduling or audio services.
