# Interaction Guidelines

## General
Use direct manipulation and clear labels. Preserve drafts where reasonable. Confirm meaningful destructive actions. Give immediate feedback. Never silently discard input.

## Timers
Show mode, remaining time and state. Distinguish start/pause/resume/reset. Confirm reset when meaningful elapsed work could be lost. Preserve timer correctness across supported lifecycle events. Completion offers a clear next action without forcing reflection.

## Alarms
Distinguish enabled, disabled, snoozed and completed. Show next trigger and recurrence. Explain exact-alarm/notification permissions in context. Do not claim guaranteed delivery until scheduling and permission state are confirmed. Test scanner failures and offer an accessible alternative where feasible.

## Medication
Display medication name and schedule as entered by the user. Separate Taken, Skipped and Not recorded. Make historical edits explicit. Never recommend dose changes or advise doubling a missed dose. Keep sensitive notification content private by default.

## Check-ins
Keep scales short and optional; allow skipping. Explain entries are self-reported. Do not force mood entry before other features. Avoid diagnostic interpretations.

## Integrations
Request permission only when the user initiates a relevant feature. Explain data access. Show connection state, last sync and errors. Permission granted does not mean sync succeeded. Provide disconnect and data-retention information.

## Errors and empty states
Explain what happened in plain language, provide a recovery action, preserve user input and distinguish no data, unavailable data, permission denied and sync failure.
