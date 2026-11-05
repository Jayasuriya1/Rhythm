# Rhythm Design System

Status: proposed foundation v1. These are design targets, not proof that current screens conform. Map tokens to Compose Material 3 semantic roles and validate on device before implementation.

## Color direction

| Semantic token | Proposed value | Purpose |
|---|---|---|
| Canvas | #F7F5EF | Warm ivory background |
| Surface | #FFFEFA | Main content |
| Surface subtle | #F0EFE8 | Quiet grouped areas |
| Ink | #262923 | Primary text |
| Ink muted | #70756D | Secondary text |
| Forest | #587461 | Primary action / selection |
| Sage soft | #DCE7DC | Low-emphasis container |
| Lavender | #AAA0D4 | Focus accent, sparingly |
| Midnight | #333C5A | Sleep accent, sparingly |
| Amber | #D6A66A | Attention/time accent |
| Danger | #B54747 | Error/destructive |

Use semantic Material roles, not ad hoc screen colors. Keep existing Sage and OLED themes during migration. Consider deep charcoal/green-black dark surfaces and retain OLED black as an optional variant. Status must not rely on color alone.

## Typography targets

| Role | Size | Weight |
|---|---:|---|
| Display | 32–36sp | Medium |
| Headline large | 28sp | Medium |
| Headline medium | 22sp | Semibold |
| Title large | 20sp | Semibold |
| Title medium | 16sp | Semibold |
| Body large | 16sp | Normal |
| Body medium | 14sp | Normal |
| Label large | 14sp | Medium |
| Label small | 12sp | Medium |

Use Android system sans-serif unless a bundled font's license and F-Droid compatibility are confirmed. Respect font scaling; avoid clipping. Use tabular numerals for timers where possible.

## Spacing (4dp base grid)
4dp micro-gap; 8dp compact; 12dp component padding; 16dp screen gutter; 24dp section gap; 32dp major separation; 48dp large breathing space. Avoid arbitrary values without reason.

## Shape and elevation
Small controls 10–12dp; cards/sheets 16–20dp; hero surfaces up to 24dp when justified. Prefer tonal separation over shadows. Avoid nested cards.

## Icons and motion
Use one coherent Material icon family. Standard icon 24dp, compact 20dp. Describe meaningful icons for accessibility. Motion must explain a transition or confirmation, remain subtle, and respect system animation settings.

## States
Every interactive component defines default, pressed, focused, disabled, selected and applicable loading/error states.

## Themes
Support Light, Dark and System; preserve OLED as optional. Semantic meaning and contrast remain consistent across themes.
