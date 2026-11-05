# Rhythm UX Audit — Second Source Review

Date: 2026-11-05
Branch: docs/product-design-system
Baseline inspected: main commit bfde88ce799579f0a215e47d641e4266312347a2 plus current feature-branch docs.
Status: source inspection only. Screenshots are present in public/assets/aistudio but have not yet been visually inspected in this audit. No device run, build or user test has been performed.

## Confirmed findings

### P0 — Cloud sync can report success without syncing
CloudSyncManager.triggerManualSync() sets SYNCING and then SYNC_SUCCESS with a timestamp when Google Services is configured. The inspected method performs no authentication, upload, download, conflict resolution or repository transfer. This can mislead users into trusting a backup that does not exist. Do not expose a successful sync state or last-sync timestamp until a real verified transfer completes. Add a blocking issue and fix before claiming cloud sync.

### P0 — Routine target-time edit appears not to persist the new target
RoutineScreen's EditAnchorTargetDialog onConfirm receives newTargetMinutes but calls viewModel.markRoutineDoneNow(anchor), then closes. The inspected callback does not pass newTargetMinutes to a target-update operation. The edit UI therefore appears to mark the anchor complete instead of saving the selected target. Verify the intended repository API and fix/test before release.

### P1 — Privacy messaging and integrations need reconciliation
Settings copy says all health, sleep, routine and study records are 100% local and no personal telemetry is transmitted. The project includes Firebase AI, Firebase App Check and Gemini API configuration, and a CloudSyncManager. Document exactly which data stays local, which requests leave the device, what data is sent to AI services, and what is currently unconfigured. Do not make an unconditional “no data transmitted” claim without verifying every network path.

### P1 — F-Droid release blockers
README declares Apache 2.0 but the inspected repository tree contains no LICENSE file. The applicationId is AI Studio-generated (com.aistudio.rhythmfocus.mnyrqp). Build plugins/dependencies include Google Services, Firebase AI, Firebase App Check Recaptcha and debug artifacts, and Secrets Gradle Plugin. These require a separate F-Droid-compatible build/dependency audit and likely build flavors. See F_DROID_AUDIT.md.

### P1 — Today dashboard is dense and card-heavy
TodayScreen contains a wide-screen two-column dashboard with next event, wearable summary, routine, sleep, mood/energy, mindfulness, reminders, priority study, brain dump and hydration. The phone layout also has multiple sections. The current implementation has a large 1,586-line screen file. Risk: too many equal-weight modules and duplicated information. Redesign around the next action and progressive disclosure; keep tablet content max-width and intentionally grouped.

### P1 — Focus screen mixes core session and configuration
StudyScreen is approximately 1,936 lines and holds timer, task selection, category management, protocol configuration, sound/music, focus studio, brain dump and multiple dialogs. The timer should dominate; move low-frequency configuration into separate screens/sheets and extract composables without changing behavior.

### P1 — Typography system incomplete
Type.kt only customizes bodyLarge while the rest of Material defaults are implicit. Define the complete scale and check tabular timer numerals, font scaling and long labels.

### P1 — Theme intent and current palette need alignment
The source defaults to OLED Pitch Black, matching the original app brief. Existing OLED accents include bright emerald #10B981 and #34D399. Refine toward muted mint/sage and establish all semantic roles. Previous design foundation's light-first/warm-ivory direction was inconsistent with the original brief; v1.1 corrects this.

### P1 — Navigation has many primary choices
Current compact nav exposes six destinations: Today, Routine, Health, Study, Meditation, Insights. Expanded nav exposes additional Brain Dump, Reminders, Alarms and Settings. Validate a simpler hierarchy without hiding frequent tasks or removing capabilities.

### P2 — Shared component governance
CommonComponents.kt has shared UI such as RhythmTopBar, but screens still define many local cards and controls. Inventory and standardize before adding new wrappers. Avoid a universal card component.

### P2 — Adaptive layouts need device-independent rules
The app has a 600dp expanded breakpoint and a two-column Today layout. Good foundation for tablets, but test Xiaomi Pad 7 portrait/landscape, split-screen, resizable windows and content max-width. Avoid relying on a brand-specific device check.

### P2 — Insights includes a planned export notice
InsightsScreen renders PlannedExportCard. A visible “planned” feature can reduce trust. Either implement it, move it to a roadmap/help area, or remove the placeholder after product approval.

### P2 — Large composable files increase iteration risk
TodayScreen, StudyScreen, HealthScreen, SettingsScreen and AlarmEditDialog are large. Extract by feature responsibility and UI state, not arbitrary line count. Keep business logic in ViewModels/repositories.

## Factual boundaries
- This audit did not run the app, inspect screenshots visually, execute tests or validate every click path.
- Health Connect manager has real SDK integration code, but end-to-end device sync and all metrics are not verified here.
- CloudSyncManager's inspected manual sync method is a placeholder success path, not evidence of cloud synchronization.
- README's local encryption claim and backup/restore correctness still require implementation-level verification.

## Next actions
1. Fix or explicitly disable false-positive cloud sync success.
2. Fix routine target editing and add regression test.
3. Reconcile privacy copy and AI/network disclosure.
4. Complete F-Droid dependency, license and build-flavor audit.
5. Redesign Today and Focus first, with screenshots on phone and tablet.
6. Complete health, alarm, medication, reminder and settings screen audits.
7. Run build, unit tests, lint and physical-device QA.
