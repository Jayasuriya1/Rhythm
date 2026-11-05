# Focus Screen — Redesign Specification

Status: proposed. Keep timer and session behavior intact during visual redesign.

## Job to be done
Start, sustain, pause and finish a focus session with almost no setup friction.

## Phone composition
1. **Session context:** short title and selected task, or a gentle “What would you like to work on?” prompt.
2. **Timer hero:** visually dominant time value, mode label and subtle progress representation. Use tabular numerals and stable alignment.
3. **Primary controls:** one clear Start/Pause/Resume action; secondary Stop/Reset must be visually distinct and protected from accidental activation.
4. **Small support actions:** park distraction, sound on/off and immersive mode. Do not show all configuration controls as equal-weight buttons.
5. **Next task:** a short task picker or one recommended task; avoid exposing full task administration above the fold.
6. **Task list and history:** below the fold, searchable/filterable, with category management as a secondary action.
7. **Advanced configuration:** protocol, custom durations, soundscape, music and visual themes in focused sheets/screens.

## Immersive focus mode
- Remove navigation and unrelated dashboard content.
- Show timer, task intention, essential controls and a discreet exit.
- Support OLED black and the chosen focus atmosphere without sacrificing contrast.
- Allow sound to be disabled independently of timer operation.
- Keep distraction capture available without losing timer state.

## Visual direction
- Calm, cinematic but restrained.
- Timer typography is the hero; don't decorate it with competing charts.
- Use one accent for active progress and a secondary tone for break state.
- Background themes are optional and must not reduce text contrast.
- Motion is limited to meaningful progress/state transitions.
- Keep controls in predictable positions across modes.

## States
Ready; running; paused; break; completed; interrupted; no task; task archived/deleted; audio unavailable; permission denied; process recreation; app backgrounded; device rotated.

## Acceptance criteria
- Start/resume/pause is the most obvious action.
- Timer remains accurate through lifecycle changes and device rotation.
- Reset cannot accidentally discard a meaningful session.
- Changing mode/protocol is understandable.
- Sound and visual theme are optional.
- Task selection does not require category administration.
- TalkBack announces timer state and remaining time without excessive repeated announcements.
- Large text remains legible and controls remain reachable.

## Existing implementation notes
StudyScreen currently combines timer, task/category management, protocol setup, sound/music, focus studio, brain dump and multiple dialogs. The screen is approximately 1,936 lines. Refactor by responsibility only after preserving existing ViewModel behavior and adding regression coverage.
