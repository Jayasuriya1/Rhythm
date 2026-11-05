# Rhythm UX Audit — Initial Source Baseline

Date: 2026-11-05
Inspected baseline: main at bfde88ce799579f0a215e47d641e4266312347a2
Status: initial source inspection, not a complete visual/usability audit.

## Inspected
README; ui/theme/Color.kt, Theme.kt, Type.kt; ui/components/CommonComponents.kt; ui/RhythmFocusApp.kt; and a Study screen implementation excerpt.

## Findings

### P1 — Feature breadth competes for attention
README documents alarms/QR, routine anchors, focus/study, brain dump, medication, sleep/health, meditation and backup. The UI tree also includes Today, Insights, reminders and settings. Risk: users may not know where to begin. Proposal: validate Today, Routine, Focus, Health and Insights as primary destinations; keep settings and alarm access contextual. Do not change navigation until full audit/user validation.

### P1 — Study screen is information-dense
Observed timer, task/priority management, category manager, filters, task list, recent sessions, sound settings, focus studio, brain dump, protocol configuration and multiple dialogs. Risk: timer competes with secondary administration. Proposal: make timer/current task dominant and progressively disclose category management, sound configuration and history.

### P1 — Typography roles are underspecified
Type.kt customizes bodyLarge while most roles remain Material defaults. Risk: inconsistent hierarchy. Define full scale and validate large font scaling.

### P1 — Shared component governance needs work
CommonComponents.kt contains reusable UI, but screens make local layout/styling decisions. Inventory existing components before creating replacements; standardize variants.

### P2 — Multiple palettes need semantic governance
Color.kt defines Sage light/dark, OLED and health metric colors; Theme.kt selects themes. Map screens to semantic roles and document stable metric colors.

### P2 — Top bar may crowd compact screens
RhythmTopBar supports date navigation, Today, brain dump, alarms and overflow. Test narrow widths; move low-frequency actions to contextual overflow.

### P2 — Health provenance and missing states
README describes sleep, mood, energy, stress, medication and telemetry; latest inspected main commit adds Health Connect integration. Distinguish connected data, user entries, permission denial, stale data and no data.

### P2 — Local-first privacy promise requires verification
README claims encrypted local Room storage and backup/restore. Verify encryption implementation, external integrations, logs, dependencies and backup handling before repeating the encryption claim in store copy.

## Not yet verified
Every rendered screen/dialog; actual navigation on device; TalkBack/font scaling; contrast; physical-device alarm/timer behavior; database encryption; Health Connect mappings/sync; F-Droid dependency/license compliance; build/test/lint results.

## Next evidence-gathering steps
Capture screenshots of Today, Routine, Focus, Health, Insights, Alarm, Medication and Settings. Inspect complete screen sources and routing. Run build, unit tests and lint. Test critical flows on a physical device. Update findings with screenshot and source evidence.
