# Today Screen — Redesign Specification

Status: proposed. Validate with current screenshots and task-based testing before changing behavior.

## Job to be done
In a few seconds, understand what matters next and act without scanning a wall of metrics.

## Phone composition (top to bottom)
1. **Greeting / date:** compact; preserve date navigation in the app bar. Avoid repeating a large date header.
2. **Next up hero:** one focal surface showing the next due routine anchor or next scheduled event. Show its time, short label and one contextual action. Do not display a circular progress graphic and another equally prominent routine dashboard competing for attention.
3. **Priority focus:** show the daily priority task with a clear Start focus action. If no task exists, offer Create/select task; do not show a blank card.
4. **Routine progress:** a compact timeline/list summary with completed, upcoming and skipped states. Avoid duplicating the exact next-event details already in the hero.
5. **Wellbeing snapshot:** a restrained section with only available data: sleep, mood/energy and Health Connect summary. Use compact rows/metrics, source labels and coverage. Hide or collapse metrics without data; never use placeholder values.
6. **Quick logs:** medication due state, hydration quick-add and check-in. Prioritize by user-configured schedule and current time; never imply a medication dose is due based on a guessed schedule.
7. **Capture / secondary tools:** brain dump, meditation and reminders remain discoverable but should not compete with the next action. Use compact rows or a “More for today” section.

## What to reduce
- Repeated routine information between next-event and routine-summary modules.
- Separate large cards for every metric.
- Multiple equal-weight calls to action.
- Decorative progress bars when the underlying data does not support a meaningful goal.
- Always-visible secondary modules when they push core actions below the fold.

## OLED art direction
- True black canvas.
- Next-up hero uses a subtle charcoal surface and one restrained sage accent.
- Timer/priority uses strong typography and a clearly labeled action.
- Wellbeing values use neutral text; metric colors are secondary and stable.
- Dividers and open sections should replace unnecessary card borders.
- Avoid bright gradients and glow on the everyday dashboard.

## Tablet / expanded layout
- Keep the main task column at a readable maximum width.
- Use a purposeful two-pane composition: next action/routine on one side; priority focus and wellbeing on the other.
- Do not stretch each card to half the screen width if its content is compact.
- Test portrait tablet, landscape tablet, split-screen and resizable windows.
- Keep the same content hierarchy as phone, not a separate product.

## Required states
First run; no task; no routine anchors; partial routine day; all anchors complete; no Health Connect; permission denied; authorized but empty; stale sync; medication none/due/recorded; active focus session; selected historical date; database error.

## Acceptance criteria
- New user can identify the next action within five seconds.
- Start/resume focus is reachable without task administration.
- Routine summary does not repeat the hero.
- No fake metrics or ambiguous zero/no-data states.
- Core content fits the first viewport on common phone sizes without hiding the primary action.
- TalkBack announces section order and action labels.
- Large text does not clip or collapse controls.
- Tablet layout has intentional max widths and balanced visual weight.

## Existing implementation notes
Current TodayScreen reads routine, sleep, check-in, hydration, medication, priority task, brain dump and Health Connect state. It renders a two-column expanded layout and a long single-column phone layout. Preserve all existing feature entry points while simplifying the visual hierarchy.
