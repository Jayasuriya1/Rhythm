# Information Architecture

Proposed target model; validate against current navigation code, user behavior and screenshots before changing routes or removing functionality.

## Five primary destinations
1. **Today** — daily overview and next helpful action.
2. **Routine** — social rhythm anchors, targets, reminders and schedule.
3. **Focus** — timer, tasks, categories, session history and distraction capture.
4. **Health** — sleep, mood, energy, stress, medication logs, hydration and connected data.
5. **Insights** — trends, history and consistency summaries.

Settings should live in a predictable top-level location without competing for primary navigation. Alarms should be accessible from Today and a dedicated alarm hub.

## Today hierarchy
Compact greeting/date; next action; anchor progress; contextual reminder/check-in; small optional summaries; secondary links. Never show placeholder metrics as real data.

## Focus hierarchy
Timer and current task; primary controls; park distraction; task selection; recent sessions; advanced sound/theme settings. Category administration must not compete with starting a session.

## Health organization
Group by user intent: check-in, sleep, medication, hydration, connected sources. Distinguish self-entered from connected data. Missing, permission-denied, stale and zero values are different states.

## Navigation acceptance
Labels are understandable; back behavior is predictable; selected state is not color-only; no critical action requires guessing an icon; notification destinations open the right context; layouts work with TalkBack, large text and compact widths.
