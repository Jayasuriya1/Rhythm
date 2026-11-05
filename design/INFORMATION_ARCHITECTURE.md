# Information Architecture

Status: current structure documented; target simplification remains a hypothesis requiring task-based user validation.

## Current navigation observed in source
The current compact navigation includes Today, Routine, Health, Study, Meditation and Insights. Additional destinations include Brain Dump, Reminders, Alarms and Settings. Expanded layouts use a NavigationRail and expose additional destinations.

This is a lot of top-level choices. Do not remove or merge features blindly. Measure which tasks users need daily and which are occasional.

## Candidate organization to validate
- **Today:** daily overview and next useful action.
- **Focus:** timer, current task, distraction capture and session history.
- **Routine:** anchors, targets and reminders.
- **Wellbeing:** sleep, mood, energy, medication, hydration and connected health data.
- **Insights:** trends and history.
- **More / contextual destinations:** meditation, brain dump, alarms, reminders and settings, depending on usage and discoverability testing.

This is a proposal, not an approved route migration. Consider keeping Meditation as a primary destination if user testing demonstrates frequent use.

## Today hierarchy
Compact greeting/date; next action; routine anchor status; contextual reminder/check-in; small optional summaries; secondary links. Never show placeholder metrics as real data. Avoid repeating the same metric in several cards.

## Focus hierarchy
Timer and current task; primary controls; park distraction; lightweight task selection; recent sessions; advanced sound/theme/protocol settings. Do not let task/category administration compete with starting a session.

## Health organization
Group by user intent: check-in, sleep, medication, hydration, connected sources. Distinguish self-entered from Health Connect. Missing, permission-denied, stale and zero values are different states.

## Navigation acceptance
Labels are understandable; back behavior predictable; selected state not color-only; no critical action requires guessing an icon; notification destinations open the correct context; compact, expanded, landscape and large-text layouts work.
