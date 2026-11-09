# Rhythm — Master Product Development & Modernization Roadmap

**Document Version:** 1.0  
**Status:** Canonical Roadmap & Execution Plan  
**Package:** `com.jayasuriya.rhythm`  
**License:** Apache License 2.0  

---

## 1. Executive Summary & Progression Strategy

This roadmap details the sequential transformation of Rhythm from a feature-rich Android app into a calm, private, reliable, and cohesive daily rhythm companion.

The work is organized into four sequential phases (Phases A through D), culminating in a dedicated Product Coherence milestone:

```
[Phase A: Daily Experience & UX]
         │
         ▼
[Phase B: Insights & Meaningful Tracking]
         │
         ▼
[Phase C: Notifications & Background Reliability]
         │
         ▼
[Phase D: Privacy & Release Readiness]
         │
         ▼
[Milestone E: Product Coherence & Daily Workflow Integration]
```

### Operational Protocol
Each phase consists of two rigorous stages:
- **STAGE 1:** Audit, investigate, document, and plan.
- **STAGE 2:** Implement approved improvements through small, focused PRs.
- *Strict Rule:* Never silently transition from audit to implementation without explicit approval.

---

## 2. Phase A — Daily Experience and UX

**Objective:** Make Rhythm intuitive, calm, accessible, and easy to use every day across all form factors.

### Task A.1: Screen-by-Screen Inventory & State Discovery (Audit)
- **Phase / Category:** Phase A — Audit
- **Priority:** P0 (Baseline Prerequisite)
- **User Impact:** Ensures no user workflows, hidden dialogs, or edge states are broken or forgotten during UX modernization.
- **Technical Dependencies:** Existing codebase inspection across `com.example.ui.screens.*`.
- **Acceptance Criteria:**
  - Complete inventory of all 23 user-facing screens, dialogs, overlays, and bottom sheets documented in `design/audits/PHASE_A_SCREEN_INVENTORY.md`.
  - Every surface maps to exact source files, viewmodel bindings, and state contracts.
- **Testing Requirements:** Code inspection; manual walkthrough in emulator.
- **Estimated Complexity:** Medium

### Task A.2: Screen-by-Screen UX & Visual QA Audit
- **Phase / Category:** Phase A — Audit
- **Priority:** P0
- **User Impact:** Identifies visual clutter, inconsistent spacing, card nesting, and accessibility bottlenecks.
- **Technical Dependencies:** Task A.1.
- **Acceptance Criteria:**
  - Complete audit report in `design/audits/PHASE_A_SCREEN_AUDIT.md`.
  - Catalog of all 153 hardcoded `Color(0x...)` occurrences and local font style overrides across `FullScreenFocusTimerScreen`, `BarcodeScannerView`, `ActiveAlarmActivity`, `InteractiveSwipeToDismissSlider`, and `AlarmEditDialog`.
  - Evaluation of primary and secondary actions on every screen.
- **Testing Requirements:** Screenshot comparisons across OLED Dark, Standard Dark, and Light themes.
- **Estimated Complexity:** Medium

### Task A.3: Accessibility & Responsive Layout Audit
- **Phase / Category:** Phase A — Audit
- **Priority:** P0
- **User Impact:** Guarantees usability for people using TalkBack, large font display scaling, compact phones, and tablets.
- **Technical Dependencies:** Task A.1.
- **Acceptance Criteria:**
  - Complete audit in `design/audits/PHASE_A_ACCESSIBILITY_AUDIT.md`.
  - Verification of 48dp minimum touch targets, WCAG 2.2 AA contrast ratios (4.5:1 text), font scaling without text clipping, and semantic TalkBack traversal orders.
  - Multi-window, split-screen, and tablet (width >= 600dp) layout evaluation.
- **Testing Requirements:** UI Automator / Accessibility Scanner checks, font scaling testing at 200%.
- **Estimated Complexity:** Medium

### Task A.4: Navigation Simplification & Destination Architecture (Audit)
- **Phase / Category:** Phase A — Audit
- **Priority:** P1
- **User Impact:** Eliminates duplicate navigation paths, clarifies labels, and provides predictable back-stack behavior.
- **Technical Dependencies:** Task A.1.
- **Acceptance Criteria:**
  - Complete navigation review in `design/audits/PHASE_A_NAVIGATION_AUDIT.md`.
  - Analysis of 10-destination tablet rail vs. 6-destination bottom bar.
  - Plan to integrate secondary screens (`HEALTH_CONNECT`, `SLEEP_ANALYTICS`, `HEART_RATE_ANALYTICS`) cleanly without navigation dead ends.
- **Testing Requirements:** Manual navigation path tracing and BackHandler testing.
- **Estimated Complexity:** Small

### Task A.5: Today Dashboard Redesign Proposal
- **Phase / Category:** Phase A — Architecture & Proposal
- **Priority:** P0
- **User Impact:** Directly answers the three core daily questions without visual overwhelm or excessive cards.
- **Technical Dependencies:** Tasks A.1–A.4.
- **Acceptance Criteria:**
  - Written design proposal and layout concepts in `design/audits/PHASE_A_DASHBOARD_PROPOSAL.md`.
  - Comparison of 2–3 alternative dashboard concepts focusing on progressive disclosure.
  - Specific recommendations for greeting, next anchor card, sleep summary, quick mood check-in, and upcoming meds.
- **Testing Requirements:** Layout review and user journey walk-through.
- **Estimated Complexity:** Medium

### Task A.6: Phase A Implementation Roadmap & Deliverables Signoff
- **Phase / Category:** Phase A — Roadmap
- **Priority:** P0
- **User Impact:** Sets a safe, reviewable implementation order for UI changes.
- **Technical Dependencies:** Tasks A.1–A.5.
- **Acceptance Criteria:**
  - Creation of `design/audits/PHASE_A_CORRECTION_ROADMAP.md` organizing implementation into small PRs:
    1. Shared components and theme color token cleanup.
    2. Navigation bar and back-handler improvements.
    3. Today screen redesign.
    4. Screen-by-screen color and layout migrations.
    5. Accessibility and responsive polish.
- **Testing Requirements:** Documentation review.
- **Estimated Complexity:** Small

### Task A.7: Phase A Implementation — Core Component & Theme Harmonization (PR A1)
- **Phase / Category:** Phase A — Implementation
- **Priority:** P0
- **User Impact:** Establishes consistent, calm visual surfaces and eliminates theme-switching glitches in light mode.
- **Technical Dependencies:** Task A.6 approval.
- **Acceptance Criteria:**
  - Migrate all hardcoded colors in `FullScreenFocusTimerScreen.kt`, `BarcodeScannerView.kt`, `ActiveAlarmActivity.kt`, and `InteractiveSwipeToDismissSlider.kt` to central semantic tokens in `Color.kt` and `MaterialTheme.colorScheme`.
  - Expand `CommonComponents.kt` with standard `MetricSummaryCard`, `ScreenContainer`, and dialog wrappers.
  - Zero regression in timer loops or alarm dismissal.
- **Testing Requirements:** `:app:assembleDebug`, `:app:lintDebug`, `:app:testDebugUnitTest`, visual screenshot review across 3 themes.
- **Estimated Complexity:** Large

### Task A.8: Phase A Implementation — Navigation & Back-Stack Polish (PR A2)
- **Phase / Category:** Phase A — Implementation
- **Priority:** P1
- **User Impact:** Predictable back navigation across phones and tablets; clean label readability.
- **Technical Dependencies:** Task A.7.
- **Acceptance Criteria:**
  - Standardize navigation hierarchy; fix text truncation in compact phone bottom bar.
  - Ensure back navigation from all sub-screens returns cleanly to `TODAY`.
- **Testing Requirements:** Mandatory Gradle gate; Compose UI tests for navigation.
- **Estimated Complexity:** Small

### Task A.9: Phase A Implementation — Modernized Today Dashboard (PR A3)
- **Phase / Category:** Phase A — Implementation
- **Priority:** P0
- **User Impact:** Users instantly understand how they are doing and what action matters next without cognitive overload.
- **Technical Dependencies:** Tasks A.7, A.8.
- **Acceptance Criteria:**
  - Implement approved Today screen layout answering the three questions.
  - Eliminate card-in-card nesting; provide progressive disclosure for deep metrics.
- **Testing Requirements:** Mandatory Gradle gate; Robolectric UI tests; screenshot tests on phone and tablet.
- **Estimated Complexity:** Large

---

## 3. Phase B — Insights and Meaningful Tracking

**Objective:** Help users understand their recorded patterns through neutral, non-judgmental observations without clinical claims.

### Task B.1: Data Inventory & Metric Audit (Audit)
- **Phase / Category:** Phase B — Audit
- **Priority:** P0
- **User Impact:** Identifies which metrics are reliably recorded vs. partially implemented, preventing misleading user statistics.
- **Technical Dependencies:** Phase A completion.
- **Acceptance Criteria:**
  - Create `design/audits/PHASE_B_DATA_INVENTORY.md` auditing all 20 Room entities and Health Connect vitals.
  - Audit mood, energy, sleep, Social Rhythm Metric (SRM), medication adherence, and focus session records.
- **Testing Requirements:** Data schema inspection and DAO query testing.
- **Estimated Complexity:** Medium

### Task B.2: Metric Calculation Specifications & Missing-Data Rules (Audit)
- **Phase / Category:** Phase B — Audit
- **Priority:** P0
- **User Impact:** Guarantees honest metrics; prevents distorted averages caused by treating missing data as zero.
- **Technical Dependencies:** Task B.1.
- **Acceptance Criteria:**
  - Create `design/audits/PHASE_B_METRIC_SPECIFICATION.md` defining exact mathematical formulas, units, partial-day handling, and timezone transitions for every metric.
  - Strict rule: unrecorded days are marked missing (`"--"`), never zero.
  - Strict rule: observational copy only; zero diagnostic or causal claims.
- **Testing Requirements:** Unit test specifications for all calculation functions.
- **Estimated Complexity:** Medium

### Task B.3: Insights & Visualizations Proposal (Audit)
- **Phase / Category:** Phase B — Architecture & Proposal
- **Priority:** P1
- **User Impact:** Replaces decorative charts with meaningful visualizations that answer practical questions.
- **Technical Dependencies:** Tasks B.1, B.2.
- **Acceptance Criteria:**
  - Create `design/audits/PHASE_B_INSIGHTS_PROPOSAL.md` specifying Daily, Weekly, and Monthly insight cards.
  - Accessible chart specifications with legible units, dates, and textual summaries.
  - Weekly report and export format specification.
- **Testing Requirements:** Documentation review and mock data review.
- **Estimated Complexity:** Medium

### Task B.4: Phase B Privacy Review & Test Plan (Audit)
- **Phase / Category:** Phase B — Audit
- **Priority:** P0
- **User Impact:** Ensures insights and exports never leak sensitive psychiatric or medication data.
- **Technical Dependencies:** Task B.3.
- **Acceptance Criteria:**
  - Create `design/audits/PHASE_B_PRIVACY_REVIEW.md` and `PHASE_B_TEST_PLAN.md`.
  - Verify that reports are generated 100% locally with zero external network transmission.
- **Testing Requirements:** Automated privacy data flow assertions.
- **Estimated Complexity:** Small

### Task B.5: Phase B Implementation — Correctness & Daily/Weekly Insights (PR B1 & B2)
- **Phase / Category:** Phase B — Implementation
- **Priority:** P0
- **User Impact:** Accurate, encouraging reflection on weekly sleep, social rhythms, and focus habits.
- **Technical Dependencies:** Phase B audit approval.
- **Acceptance Criteria:**
  - Implement validated metric calculation domain engine with unit tests.
  - Update `InsightsScreen` with accessible charts, clear units, and neutral language.
  - Support encrypted/structured personal report generation locally via SAF.
- **Testing Requirements:** Mandatory Gradle gate; comprehensive unit tests covering missing data, DST shifts, and leap years.
- **Estimated Complexity:** Large

---

## 4. Phase C — Notifications and Background Reliability

**Objective:** Ensure alarms, reminders, and timers behave with rock-solid predictability across Android power states, timezones, and reboots.

### Task C.1: Scheduling & Android Lifecycle Audit (Audit)
- **Phase / Category:** Phase C — Audit
- **Priority:** P0
- **User Impact:** Identifies edge cases where alarms or reminders might be delayed, missed, or duplicated.
- **Technical Dependencies:** Phase B completion.
- **Acceptance Criteria:**
  - Create `design/audits/PHASE_C_ALARM_AUDIT.md`, `PHASE_C_NOTIFICATION_AUDIT.md`, and `PHASE_C_BACKGROUND_WORK_AUDIT.md`.
  - Audit `AlarmManager.setAlarmClock`, exact alarm permissions (`SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`), WorkManager, wake lock lifecycles, and notification channels.
- **Testing Requirements:** Platform lifecycle code analysis.
- **Estimated Complexity:** Medium

### Task C.2: Failure Scenario & Timer Resilience Audit (Audit)
- **Phase / Category:** Phase C — Audit
- **Priority:** P0
- **User Impact:** Prevents timer resets when backgrounded and guarantees alarm dismissal reliability.
- **Technical Dependencies:** Task C.1.
- **Acceptance Criteria:**
  - Create `design/audits/PHASE_C_TIMER_AUDIT.md` and `PHASE_C_DEVICE_TEST_MATRIX.md`.
  - Document failure modes: process death, reboot recovery, battery saver, Doze mode, DST shifts, audio focus contention.
- **Testing Requirements:** Automated Robolectric tests for broadcast receiver intents and timer state persistence.
- **Estimated Complexity:** Medium

### Task C.3: Phase C Implementation — Alarm & Timer Hardening (PR C1)
- **Phase / Category:** Phase C — Implementation
- **Priority:** P0
- **User Impact:** Dependable wake-up alarms and study timers that never lose session progress.
- **Technical Dependencies:** Phase C audit approval.
- **Acceptance Criteria:**
  - Persist active timer timestamps using `SystemClock.elapsedRealtime()` so countdowns survive process recreation accurately.
  - Ensure immediate release of `PARTIAL_WAKE_LOCK` upon alarm stop.
  - Audio focus management cleanly pauses/resumes background ambient soundscapes on incoming calls.
- **Testing Requirements:** Mandatory Gradle gate; `AlarmSecurityAndWakeLockTest`, `AlarmTimeZoneAndBootResilienceTest`.
- **Estimated Complexity:** Medium

---

## 5. Phase D — Privacy and Release Readiness

**Objective:** Guarantee strict data transparency, verify F-Droid compliance, and prepare for secure, verified public distribution.

### Task D.1: Data-Flow & Privacy Audit (Audit)
- **Phase / Category:** Phase D — Audit
- **Priority:** P0
- **User Impact:** 100% confidence that personal wellness, thoughts, and medication records are never leaked.
- **Technical Dependencies:** Phase C completion.
- **Acceptance Criteria:**
  - Create `design/audits/PHASE_D_DATA_FLOW_INVENTORY.md` and `PHASE_D_PRIVACY_AUDIT.md`.
  - Verify 0 bytes egress; verify SQLCipher AES-256 encryption at rest; verify optional AES-256-GCM backup encryption.
- **Testing Requirements:** `PrivacyDataFlowTest` passing with 0 network calls.
- **Estimated Complexity:** Small

### Task D.2: Security & Keystore Audit (Audit)
- **Phase / Category:** Phase D — Audit
- **Priority:** P0
- **User Impact:** Protects against data loss during app updates, device reboots, and backup restorations.
- **Technical Dependencies:** Task D.1.
- **Acceptance Criteria:**
  - Create `design/audits/PHASE_D_SECURITY_AUDIT.md`.
  - Audit Android Keystore key management, key loss recovery strategies, and SQLCipher database migration safety.
- **Testing Requirements:** `DatabaseKeyManagerTest` and `DataBackupManagerTest`.
- **Estimated Complexity:** Small

### Task D.3: Dependency, Licensing & F-Droid Compliance Audit (Audit)
- **Phase / Category:** Phase D — Audit
- **Priority:** P0
- **User Impact:** Full legal compliance and seamless inclusion into F-Droid without proprietary blockers.
- **Technical Dependencies:** Task D.1.
- **Acceptance Criteria:**
  - Create `design/audits/PHASE_D_DEPENDENCY_LICENSE_AUDIT.md` and `PHASE_D_FDROID_AUDIT.md`.
  - Audit all direct and transitive dependencies; verify Apache 2.0 license compatibility; confirm zero proprietary binary blobs.
  - Verify CC0 / Public Domain licensing for all procedural ambient soundscapes (`THIRD_PARTY_AUDIO.md`).
- **Testing Requirements:** License review and reproducible build verification (`gradle clean :app:assembleRelease`).
- **Estimated Complexity:** Small

### Task D.4: Release Packaging & Verification (Implementation & Signoff)
- **Phase / Category:** Phase D — Implementation
- **Priority:** P0
- **User Impact:** High-performance, optimized release APK ready for public installation.
- **Technical Dependencies:** Tasks D.1–D.3.
- **Acceptance Criteria:**
  - Create `design/audits/PHASE_D_RELEASE_CHECKLIST.md`.
  - Configure ProGuard/R8 keep rules for SQLCipher, Room, and Moshi without breaking reflection.
  - Perform clean install, upgrade test, and backup restore test on release build.
- **Testing Requirements:** Mandatory Gradle gate; release APK assembly and smoke test.
- **Estimated Complexity:** Medium

---

## 6. Milestone E — Product Coherence & Daily Workflow Integration

**Objective:** Unify all features into one harmonious companion structured around the Four Daily Moments.

### Task E.1: Daily Moments End-to-End Workflow Audit (Audit)
- **Phase / Category:** Milestone E — Audit
- **Priority:** P0
- **User Impact:** Ensures Rhythm feels like a single calm companion rather than 10 disconnected tools.
- **Technical Dependencies:** Phases A through D completed.
- **Acceptance Criteria:**
  - Create `design/PRODUCT_COHERENCE_AUDIT.md` and `design/DAILY_WORKFLOW_SPECIFICATION.md`.
  - Map every major screen and feature to one of the Four Daily Moments (Morning, Daytime, Evening, Weekly).
  - Measure interaction friction (taps and screens required) for core daily tasks.
- **Testing Requirements:** User journey walkthrough and heuristic evaluation.
- **Estimated Complexity:** Medium

### Task E.2: Today Dashboard Final Specification (Design)
- **Phase / Category:** Milestone E — Design
- **Priority:** P0
- **User Impact:** Flawless daily dashboard execution answering the three core questions seamlessly.
- **Technical Dependencies:** Task E.1.
- **Acceptance Criteria:**
  - Create `design/TODAY_DASHBOARD_FINAL_SPEC.md` and `design/PRODUCT_ACCEPTANCE_CRITERIA.md`.
  - Finalized component layout, progressive disclosure triggers, and contextual day-part transitions (Morning vs Daytime vs Evening).
- **Testing Requirements:** Design and specification review.
- **Estimated Complexity:** Small

### Task E.3: Final Daily Workflow Integration & Polish (Implementation PR)
- **Phase / Category:** Milestone E — Implementation
- **Priority:** P0
- **User Impact:** Effortless daily transitions: morning sleep review → daytime routine anchor & focus → evening wind down → weekly pattern review.
- **Technical Dependencies:** Tasks E.1, E.2 approval.
- **Acceptance Criteria:**
  - Implement dynamic time-of-day contextual card emphasis on the Today screen.
  - Smooth 1-tap shortcuts for logging active routine anchors and confirming scheduled medications.
  - Seamless returning to previous task when parking thoughts into Brain Dump or finishing a breathing break.
- **Testing Requirements:** Full CI gate suite: `:app:assembleDebug`, `:app:lintDebug`, `:app:testDebugUnitTest -ProbolectricOffline=false`.
- **Estimated Complexity:** Large

### Task E.4: Final Release Candidate Sign-Off & Verification
- **Phase / Category:** Milestone E — Verification
- **Priority:** P0
- **User Impact:** Trustworthy, polished release ready for users.
- **Technical Dependencies:** Task E.3 merged and verified on CI.
- **Acceptance Criteria:**
  - Complete verification checklist in `design/PRODUCT_ACCEPTANCE_CRITERIA.md`.
  - Passing GitHub Actions CI run on `main`.
  - Zero critical crashes, zero memory leaks, and zero accessibility violations.
- **Testing Requirements:** Full automated test suite; release APK verification.
- **Estimated Complexity:** Medium

---

## 7. Master Task Matrix Summary

| Task ID | Phase / Milestone | Title | Priority | Complexity | Dependencies |
|---|---|---|---|---|---|
| **A.1** | Phase A (UX) | Complete Application Inventory (Audit) | P0 | Medium | None |
| **A.2** | Phase A (UX) | Screen-by-Screen UX & Visual QA Audit | P0 | Medium | A.1 |
| **A.3** | Phase A (UX) | Accessibility & Responsive Layout Audit | P0 | Medium | A.1 |
| **A.4** | Phase A (UX) | Navigation Simplification Audit | P1 | Small | A.1 |
| **A.5** | Phase A (UX) | Today Dashboard Redesign Proposal | P0 | Medium | A.1–A.4 |
| **A.6** | Phase A (UX) | Phase A Implementation Roadmap Signoff | P0 | Small | A.1–A.5 |
| **A.7** | Phase A (UX) | PR A1: Component & Theme Color Harmonization | P0 | Large | A.6 |
| **A.8** | Phase A (UX) | PR A2: Navigation & Back-Stack Polish | P1 | Small | A.7 |
| **A.9** | Phase A (UX) | PR A3: Modernized Today Dashboard | P0 | Large | A.7, A.8 |
| **B.1** | Phase B (Data) | Data Inventory & Metric Audit | P0 | Medium | Phase A |
| **B.2** | Phase B (Data) | Metric Calculations & Missing Data Spec | P0 | Medium | B.1 |
| **B.3** | Phase B (Data) | Insights & Visualizations Proposal | P1 | Medium | B.1, B.2 |
| **B.4** | Phase B (Data) | Privacy Review & Test Plan | P0 | Small | B.3 |
| **B.5** | Phase B (Data) | PR B1/B2: Insights Engine & Screen Polish | P0 | Large | B.1–B.4 |
| **C.1** | Phase C (Reliability) | Scheduling & Lifecycle Audit | P0 | Medium | Phase B |
| **C.2** | Phase C (Reliability) | Failure Scenario & Timer Resilience Audit | P0 | Medium | C.1 |
| **C.3** | Phase C (Reliability) | PR C1: Alarm, Reminder & Timer Hardening | P0 | Medium | C.1, C.2 |
| **D.1** | Phase D (Release) | Data-Flow & Privacy Verification Audit | P0 | Small | Phase C |
| **D.2** | Phase D (Release) | Security & Keystore Audit | P0 | Small | D.1 |
| **D.3** | Phase D (Release) | Licensing & F-Droid Compliance Audit | P0 | Small | D.1 |
| **D.4** | Phase D (Release) | Release Packaging & ProGuard/R8 Signoff | P0 | Medium | D.1–D.3 |
| **E.1** | Milestone E (Coherence) | Four Daily Moments Workflow Audit | P0 | Medium | Phases A–D |
| **E.2** | Milestone E (Coherence) | Today Dashboard Final Specification | P0 | Small | E.1 |
| **E.3** | Milestone E (Coherence) | PR E1: Daily Workflow Integration Polish | P0 | Large | E.1, E.2 |
| **E.4** | Milestone E (Coherence) | Final Release Candidate Sign-Off | P0 | Medium | E.3 |
