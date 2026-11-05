# Rhythm UX Audit — Second Source Review

Date: 2026-11-05
Branch: docs/product-design-system
Baseline: main commit bfde88ce799579f0a215e47d641e4266312347a2; follow-up code fixes are on this branch.
Status: source inspection plus a first visual review of committed screenshots. The captures appear to predate some newer branch changes. No app build, test execution or current-device re-capture has been performed.

## Confirmed findings and current branch changes

### P0 — Cloud sync previously reported success without transfer
Original CloudSyncManager.triggerManualSync() set SYNC_SUCCESS and a timestamp when Google Services was configured, without authentication or data transfer.
**Branch change:** CloudSyncManager now fails closed, stays UNCONFIGURED and returns an explicit unsupported result until real sync exists. Settings copy now says sync is not implemented and no longer presents it as active.
**Remaining:** add/verify tests; implement real authenticated transfer only after backend, consent, conflict and privacy design.

### P1 — Routine target-time editor previously saved the wrong action
Original RoutineScreen callback received newTargetMinutes but called markRoutineDoneNow(anchor).
**Branch change:** added MainViewModel.updateRoutineAnchorTarget(), which validates 0..1439, persists anchor.copy(defaultTargetMinutes=...), and updates feedback. Dialog now calls that method. Added a Room-backed regression test to verify persistence without creating a completion entry.
**Remaining:** run tests/build and verify the UI on device.

### P1 — Privacy messaging and integrations need reconciliation
Settings previously claimed no personal telemetry was transmitted. Firebase AI, App Check and Gemini API configuration exist in the project, but actual AI network call paths and payloads have not been fully verified.
**Branch change:** softened Settings copy to describe local Room storage, permission-based Health Connect and the current absence of cloud sync. This is interim copy, not a completed data-flow audit.
**Remaining:** inventory all network clients/SDK calls and disclose exact payloads, destinations, opt-in and fallback behavior.

### P1 — F-Droid release blockers
README declares Apache 2.0 but repository tree has no LICENSE file. ApplicationId is AI Studio-generated (com.aistudio.rhythmfocus.mnyrqp). Google Services, Firebase AI, Firebase App Check and Secrets plugins/dependencies need a resolved dependency/license audit. See F_DROID_AUDIT.md.

### P1 — Today dashboard is dense and card-heavy
TodayScreen has phone and wide two-column layouts with next event, wearable summary, routine, sleep, mood/energy, mindfulness, reminders, priority study, brain dump, hydration and medication. It is approximately 1,586 lines. Redesign around the next action and progressive disclosure; avoid repeating metrics and keep tablet content max-width.

### P1 — Focus screen mixes session and configuration
StudyScreen is approximately 1,936 lines and combines timer, task selection, category management, protocol, sound/music, focus studio, brain dump and dialogs. Timer should dominate; move low-frequency configuration to contextual surfaces and extract composables without behavior changes.

### P1 — Typography and palette need completion
Type.kt originally customized only bodyLarge. On this branch, the full Material typography scale has been defined and OLED primary/secondary accents have been softened from bright emerald to muted sage/mint. This is implemented but not yet visually validated. Contrast, font scaling and screenshots remain required.

### P1 — Navigation has many primary choices
Compact navigation exposes Today, Routine, Health, Study, Meditation and Insights. Expanded navigation exposes Brain Dump, Reminders, Alarms and Settings. Validate hierarchy with task-based user testing before moving or removing destinations.

### P2 — Shared component governance
CommonComponents.kt contains reusable elements, but screens define many local cards and controls. Inventory and standardize; avoid a universal card wrapper.

### P2 — Adaptive layouts
The app uses a 600dp expanded breakpoint, NavigationRail and two-column Today. Test Xiaomi Pad 7 portrait/landscape, split-screen, resizable windows and font scaling. Prefer window size classes and max content widths over brand-specific checks.

### P2 — Planned export notice
InsightsScreen renders PlannedExportCard. A visible planned feature can reduce trust. Implement it, move it to roadmap/help, or remove the placeholder after product approval.

### P2 — Large composable files
TodayScreen, StudyScreen, HealthScreen, SettingsScreen and AlarmEditDialog are large. Extract by responsibility after visual/behavioral audit, preserving tests and state ownership.

## Build tooling finding
The inspected repository tree contains gradle-wrapper.properties but does not show gradlew, gradlew.bat or gradle-wrapper.jar. A GitHub Actions workflow was added to install Gradle 9.3.1 directly and run debug assembly plus lint. No workflow run has yet been observed. Robolectric offline mode is now configurable: local builds remain offline by default and CI opts into online runtime downloads. The workflow includes unit tests; final result is still pending.

## Factual boundaries
- A subset of repository screenshots has been visually inspected; see VISUAL_SCREENSHOT_REVIEW.md. These captures may predate current branch code.
- No current-branch app build, tests, lint, device validation or user testing has been performed.
- Health Connect manager contains real SDK integration code, but end-to-end device sync and every metric are not verified.
- CloudSyncManager's current branch behavior is intentionally unavailable; no cloud sync is implemented.
- README's encryption claim and backup/restore correctness still require implementation-level verification.

## Next actions
1. Run tests/build after restoring a reproducible Gradle wrapper/build environment.
2. Verify cloud sync fail-closed and routine target persistence fixes.
3. Complete privacy/network data-flow audit.
4. Complete F-Droid dependency, license and build-flavor audit.
5. Capture and visually inspect Today and Focus screenshots on phone and tablet.
6. Redesign Today and Focus from approved specs.
7. Complete health, alarm, medication, reminder and settings audits.
