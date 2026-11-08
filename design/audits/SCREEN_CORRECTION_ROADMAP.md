# Rhythm — Complete UI Design System Screen Correction Roadmap

**Roadmap Date:** November 8, 2026  
**Status:** Completed & Verified  
**Strategy:** Phased, incremental migration prioritizing shared components and central tokens before screen-specific changes.  
**Strict Safety Rule:** Zero modifications to Room schemas, existing user data, application ID (`com.jayasuriya.rhythm`), alarm scheduling, timer lifecycle, audio playback engine, or user-created study category colors.

---

## Phase Overview

```
┌──────────────────────────────────────────────────────────────┐
│ Phase 0: Baseline & Semantic Tokens Consolidation           │
│ - Color.kt semantic tokens check & expansion                 │
│ - CommonComponents.kt expansion (Dialogs, Metric Cards)     │
└──────────────────────────────┬───────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────┐
│ Phase 1: Full-Screen Focus Timer Modernization (P0)          │
│ - Eliminate 107 hardcoded colors in FullScreenFocusTimer     │
│ - Standardize in-session modals (Park Thought, Notes, Exit)  │
│ - Ensure 100% multi-theme fidelity (OLED, Dark, Light)      │
└──────────────────────────────┬───────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────┐
│ Phase 2: Alarm & Dismissal Suite Modernization (P1)          │
│ - AlarmEditDialog tints & success state                      │
│ - InteractiveSwipeToDismissSlider theme integration          │
│ - MyCodesLibraryScreen status badges                         │
│ - ActiveAlarmActivity emergency & warning tokens             │
│ - BarcodeScannerView CameraX viewfinder colors               │
└──────────────────────────────┬───────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────┐
│ Phase 3: Dashboard & Core Tabs Consistency Polish (P2)       │
│ - Streamline stat summary cards with MetricSummaryCard       │
│ - Polish typography and touch targets across all tabs        │
└──────────────────────────────┬───────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────┐
│ Phase 4: Mandatory Build, Lint & Multi-Theme Verification    │
│ - Debug APK Assembly & Android Lint                          │
│ - Robolectric Test Suite Execution                           │
│ - Visual verification across OLED, Dark, Light & Tablet      │
└──────────────────────────────────────────────────────────────┘
```

---

## Phase 0: Centralized Design Tokens & Shared Components

### 1. Centralized Semantic Tokens Expansion
- **File to Modify:** `app/src/main/java/com/example/ui/theme/Color.kt`
- **Existing Problems:**
  - `AlarmEditDialog`, `ActiveAlarmActivity`, `BarcodeScannerView`, and `InteractiveSwipeToDismissSlider` rely on ad-hoc hex values (`0xFF64B5F6`, `0xFF1E88E5`, `0xFFD32F2F`, `0xFF2E7D32`, etc.) because semantic roles for scanner viewfinders and alarm challenge buttons were not explicitly documented in `Color.kt`.
- **Proposed Improvements:**
  - Verify and document existing `StateHealthy`, `StateWarning`, `StateInfo`, `StateErrorDark`, `StateErrorLight` tokens.
  - Add explicit semantic tokens if needed for alarm visual presets, ensuring all alarm themes map to approved brand depth.
- **Tests Required:** Theme compilation check and color contrast verification.
- **Functional Regression Risks:** None (additive token declarations only).
- **Expected Result:** Centralized palette covering all functional alarm and scanner states.

### 2. Standardized Reusable UI Primitives
- **File to Modify:** `app/src/main/java/com/example/ui/components/CommonComponents.kt`
- **Existing Problems:**
  - Screens frequently reimplement custom dialogs, custom stat cards, and repetitive text-input wrappers with subtle visual differences.
- **Proposed Improvements:**
  - Add `RhythmConfirmationDialog`: Standard M3 dialog with icon, title, description, confirm button, and dismiss button using `MaterialTheme.colorScheme.surfaceContainer` / `surface`.
  - Add `RhythmInputDialog`: Standard single-field or multi-line text input dialog for fast notes (replaces custom park-a-thought and session-notes dialogs).
  - Add `MetricSummaryCard`: Standardized stat display component (label, formatted value, icon, and optional trend/subtext) to consolidate quick-stats in `TodayScreen` and `HealthConnectDashboardScreen`.
- **Tests Required:** Robolectric unit tests in `ExampleRobolectricTest.kt`.
- **Functional Regression Risks:** None; new composables with default parameters.
- **Expected Result:** Unified design primitives available to all screens.

---

## Phase 1: Full-Screen Focus Timer Modernization (P0 — Critical)

- **Target File:** `app/src/main/java/com/example/ui/screens/study/FullScreenFocusTimerScreen.kt`
- **Existing Problems:**
  - 107 occurrences of hardcoded hex values (`Color(0xFFA8C9B0)`, `Color(0xFF8F9E94)`, `Color(0xFF131A15)`, `Color(0xFF0F1411)`, `Color(0xFF1E2820)`, `Color(0xFF263329)`, `Color(0xFFE2E8E4)`).
  - Dialogs (`showNotesDialog`, `showParkThoughtModal`, `showEndSessionConfirmation`, `showVolumePopover`) use hardcoded dark containers and dark borders that break if the user switches to Light theme.
  - Buttons use hardcoded mint/sage colors rather than `MaterialTheme.colorScheme.primary` and `onPrimary`.
- **Proposed Visual Improvements:**
  - Replace `Color(0xFFA8C9B0)` with `MaterialTheme.colorScheme.primary`.
  - Replace `Color(0xFF8F9E94)` with `MaterialTheme.colorScheme.onSurfaceVariant`.
  - Replace `Color(0xFFE2E8E4)` with `MaterialTheme.colorScheme.onSurface`.
  - Replace `Color(0xFF131A15)`, `Color(0xFF0F1411)`, `Color(0xFF141C16)` with `MaterialTheme.colorScheme.surface` and `surfaceContainer`.
  - Replace `Color(0xFF263329)`, `Color(0xFF233226)` with `MaterialTheme.colorScheme.outlineVariant`.
  - Replace ad-hoc dialogs with `RhythmInputDialog` and `RhythmConfirmationDialog`.
  - Ensure volume slider and quick pills use `MaterialTheme.colorScheme`.
- **Required Central Tokens:** `MaterialTheme.colorScheme.primary`, `onPrimary`, `surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`, `outlineVariant`.
- **Reusable Components to Use:** `RhythmConfirmationDialog`, `RhythmInputDialog`.
- **Files Expected to Change:** `FullScreenFocusTimerScreen.kt`.
- **Tests Required:**
  - Timer countdown state machine tests in `ExampleRobolectricTest.kt`.
  - Audio lifecycle transitions (pause on pause, stop on reset).
  - Park a thought Brain Dump persistence.
- **Functional Regression Risks:** High (timer countdown ticker, audio hooks, screen awake effect, and session persistence must remain 100% operational).
- **Expected Result:** 0 hardcoded colors, seamless adaptation to OLED Dark, Standard Dark, and Light themes, perfectly calm aesthetics.
- **Dependencies:** Phase 0 (`CommonComponents.kt`).

---

## Phase 2: Alarm & Dismissal Suite Modernization (P1 — High)

### 1. Alarm Edit & Creation Dialog
- **Target File:** `app/src/main/java/com/example/ui/screens/alarm/AlarmEditDialog.kt`
- **Existing Problems:**
  - Hardcoded tints for audio and mission options (`0xFF64B5F6`, `0xFF81C784`, `0xFFFFB74D`).
  - Hardcoded success color `Color(0xFF2E7D32)` in QR code verification badge.
- **Proposed Visual Improvements:**
  - Map icon tints to `StateInfo`, `StateHealthy`, `StateWarning`, and `MaterialTheme.colorScheme.primary`.
  - Replace success badge with standard `StatusBadge` or `StateHealthy` tint.
  - Simplify nested `Surface` containers.
- **Required Central Tokens:** `StateHealthy`, `StateInfo`, `StateWarning`, `MaterialTheme.colorScheme.primary`.
- **Tests Required:** Alarm scheduling and audio crescendo persistence tests.
- **Functional Regression Risks:** Low (visual styling only; entity schema remains untouched).
- **Expected Result:** Clean, accessible alarm creation dialog following M3 tokens.

### 2. Interactive Swipe-to-Dismiss Slider
- **Target File:** `app/src/main/java/com/example/ui/screens/alarm/InteractiveSwipeToDismissSlider.kt`
- **Existing Problems:**
  - 7 hardcoded green hexes (`0xFF1B5E20`, `0xFF2E7D32`, `0xFF43A047`, `0xFF81C784`, `0xFF66BB6A`, `0xFF388E3C`).
- **Proposed Visual Improvements:**
  - Replace hardcoded track gradient with `MaterialTheme.colorScheme.primaryContainer` and `surfaceVariant`.
  - Replace thumb gradient with `MaterialTheme.colorScheme.primary` and `onPrimary`.
  - Replace chevron text tint with `MaterialTheme.colorScheme.onPrimaryContainer`.
- **Required Central Tokens:** `MaterialTheme.colorScheme.primaryContainer`, `onPrimaryContainer`, `primary`, `onPrimary`.
- **Tests Required:** Slider drag threshold and dismissal callback verification.
- **Functional Regression Risks:** Medium (swipe-to-dismiss is the primary physical safety gesture during alarm ring).
- **Expected Result:** Fluid, beautiful slider that automatically matches current theme.

### 3. QR Codes Library
- **Target File:** `app/src/main/java/com/example/ui/screens/alarm/MyCodesLibraryScreen.kt`
- **Existing Problems:**
  - 4 occurrences of hardcoded `Color(0xFF2E7D32)`.
- **Proposed Visual Improvements:**
  - Replace hardcoded badge with `StatusBadge(status = "ACTIVE")`.
- **Required Central Tokens:** `StateHealthy`.
- **Tests Required:** Code list querying and deletion tests.
- **Functional Regression Risks:** Low.
- **Expected Result:** Consistent status indicator.

### 4. Active Alarm Ringing Activity
- **Target File:** `app/src/main/java/com/example/ui/screens/alarm/ActiveAlarmActivity.kt`
- **Existing Problems:**
  - 11 hardcoded colors (`0xFFD32F2F`, `0xFF64B5F6`, `0xFFFFB74D`, `0xFF1E88E5`, `0xFFFFD54F`).
- **Proposed Visual Improvements:**
  - Replace error text and retry buttons with `MaterialTheme.colorScheme.error` and `onError`.
  - Replace warning chips with `StateWarning`.
  - Replace mission challenge button with `MaterialTheme.colorScheme.primary`.
- **Required Central Tokens:** `MaterialTheme.colorScheme.error`, `StateWarning`, `StateInfo`, `primary`.
- **Tests Required:** Lockscreen wake lock, crescendo playback service, challenge verification.
- **Functional Regression Risks:** High (failsafe alarm dismissal must never freeze or crash).
- **Expected Result:** High-visibility, accessible emergency alarm interface adhering to design system.

### 5. Barcode & QR Scanner View
- **Target File:** `app/src/main/java/com/example/ui/screens/alarm/BarcodeScannerView.kt`
- **Existing Problems:**
  - 18 hardcoded colors (`0xFF121212`, `0xFF1E293B`, `0xFF64B5F6`, `0xFFFF5252`, `0xFFFFD54F`, etc.).
- **Proposed Visual Improvements:**
  - Migrate camera overlay scrim and cards to `MaterialTheme.colorScheme.surface` with alpha.
  - Viewfinder target reticle: `MaterialTheme.colorScheme.primary` when searching, `StateHealthy` on match, `MaterialTheme.colorScheme.error` on mismatch.
  - Torch toggle and emergency override buttons use standard `FilledTonalButton` and `Button` with M3 colors.
- **Required Central Tokens:** `MaterialTheme.colorScheme.primary`, `error`, `surface`, `StateHealthy`, `StateWarning`.
- **Tests Required:** Barcode scanning match logic and emergency override verification.
- **Functional Regression Risks:** Medium (CameraX lifecycle and frame analyzer).
- **Expected Result:** Polished, professional camera viewfinder with clear semantic feedback.

---

## Phase 3: Dashboard & Core Tabs Consistency Polish (P2 — Medium)

### 1. Today Screen Metric Summaries
- **Target File:** `app/src/main/java/com/example/ui/screens/today/TodayScreen.kt`
- **Existing Problems:**
  - Minor card-in-card layout in quick stat rows; slight inconsistency in stat card padding.
- **Proposed Improvements:**
  - Adopt shared `MetricSummaryCard` for water, routine progress, and sleep quick stats.
- **Tests Required:** Today dashboard rendering tests.
- **Functional Regression Risks:** Low.
- **Expected Result:** Cleaner, more scannable dashboard.

### 2. Study Screen Category Fallback Color
- **Target File:** `app/src/main/java/com/example/ui/screens/study/StudyScreen.kt`
- **Existing Problems:**
  - Line 1948 contains fallback `Color(0xFF4D6B53)`.
- **Proposed Improvements:**
  - Replace fallback with `SagePrimaryLight` or `MaterialTheme.colorScheme.primary`.
- **Tests Required:** Study task list and category filter tests.
- **Functional Regression Risks:** Zero.
- **Expected Result:** Complete elimination of arbitrary color literals in `StudyScreen.kt`.

---

## Phase 4: Mandatory Build, Lint & Multi-Theme Verification

### 1. Build & Lint Validation
- **Commands:**
  ```bash
  gradle :app:assembleDebug :app:lintDebug --stacktrace
  ```
- **Criteria:**
  - 0 compilation errors.
  - 0 new Android Lint warnings/errors.
  - Verification of APK packaging.

### 2. Robolectric Unit Test Suite
- **Command:**
  ```bash
  gradle :app:testDebugUnitTest -ProbolectricOffline=false --stacktrace
  ```
- **Criteria:**
  - 100% test pass rate across all existing unit tests in `ExampleRobolectricTest.kt`.
  - Added unit tests for new shared components (`MetricSummaryCard`, `RhythmConfirmationDialog`, `RhythmInputDialog`).

### 3. Comprehensive Visual & Theme QA
- **Theme Matrix:**
  1. OLED Dark (`AppThemeMode.OLED_DARK`) — verified pure-black backgrounds with sage/mint accents.
  2. Standard Dark (`AppThemeMode.DARK`) — verified layered teal-charcoal surfaces.
  3. Light (`AppThemeMode.LIGHT`) — verified warm off-white canvas with dark teal text.
  4. System Following (`AppThemeMode.SYSTEM`) — verified proper dynamic switching.
- **Form-Factor Matrix:**
  1. Compact Phone (< 360dp width).
  2. Standard Phone (~400dp width).
  3. Xiaomi Pad 7 Tablet (Expanded screen, wide landscape & portrait).
- **Accessibility Criteria:**
  - Minimum 4.5:1 text contrast on all regular text; 3:1 on large text.
  - All interactive controls have at least 48dp x 48dp touch targets.
  - All icons have descriptive TalkBack `contentDescription` or null for decorative assets.
