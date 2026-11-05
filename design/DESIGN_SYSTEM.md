# Rhythm Design System

Status: foundation v1.2. The OLED palette values and full typography scale have now been applied in Color.kt and Type.kt on this branch. They are an initial implementation, not yet visually approved; contrast and rendered screenshots still require QA. The existing theme modes remain in place.

## 1. Theme modes
- **OLED Pitch Black — default**
- Standard Dark
- Light
- Follow System

All screens, dialogs, bottom sheets, charts, notifications and immersive experiences must honor the selected mode. Theme preference must persist. Dynamic color should not unexpectedly override the brand palette unless the user explicitly opts in.

## 2. OLED-first semantic palette

| Token | Proposed value | Purpose |
|---|---|---|
| background | #000000 | OLED canvas |
| surface | #0C100E | Main surface |
| surface-container | #121714 | Grouped content |
| surface-elevated | #1A211D | Dialogs / elevated layers |
| on-surface | #F1F4F1 | Primary text |
| on-surface-variant | #A2AAA5 | Secondary text |
| primary | #A8C9B0 | Muted mint/sage action |
| on-primary | #0B1710 | Text on primary |
| primary-container | #20362A | Selected / supporting surface |
| secondary | #B7C4BA | Neutral secondary emphasis |
| tertiary | #C7B7E8 | Optional reflective accent |
| warning | #D8B477 | Attention / caution |
| error | #E38A83 | Destructive / error |
| outline | #303A33 | Subtle borders |

These are design proposals, not yet implemented values. Prefer a restrained primary accent over the current bright emerald #10B981 / #34D399 combination. Keep health-series colors stable and semantically defined.

## 3. Light theme
Use a warm-neutral canvas (#F6F7F2 direction), white/off-white surfaces, deep forest primary, high-contrast dark text and the same semantic status meanings. Light mode must receive equal QA.

## 4. Typography
| Role | Target | Weight |
|---|---:|---|
| Display | 32–36sp | Medium |
| Headline large | 28sp | Medium |
| Headline medium | 22sp | Semibold |
| Title large | 20sp | Semibold |
| Title medium | 16sp | Semibold |
| Body large | 16sp / 24sp line | Normal |
| Body medium | 14sp / 20sp line | Normal |
| Label large | 14sp | Medium |
| Label small | 12sp | Medium |

Define the full Material typography scale; current Type.kt only customizes bodyLarge. Use system sans-serif unless font licensing and F-Droid redistribution are confirmed. Respect font scaling; use tabular numerals for timer values.

## 5. Spacing and layout
Use a 4dp base grid: 4dp micro-gap, 8dp compact, 12dp component padding, 16dp standard gutter, 24dp section gap, 32dp major separation, 48dp breathing space. Define maximum content widths and responsive gutters. Do not stretch content across a wide tablet.

## 6. Shape, elevation and surfaces
Small controls 10–12dp; standard grouped surfaces 16–20dp; hero surfaces up to 24dp only when justified. Prefer tonal layering over shadows. No card-in-card by default. Use open sections, dividers and typographic grouping where content belongs together.

## 7. Icons, illustration and imagery
Use one coherent Material icon family. Standard 24dp, compact 20dp. No emoji-only controls. Custom illustration should be rare, consistent and purposeful. Use original or properly licensed assets; never copy reference-site artwork.

## 8. Motion
Motion explains state, hierarchy or feedback. Keep it short and calm, avoid continuous dashboard motion, and respect system animation settings. Do not animate essential data into view in a way that delays access.

## 9. Component states
Every interactive component defines default, pressed, focused, disabled, selected, loading and error states as applicable. Include empty, first-run, permission-denied, offline, stale-data and success states at screen level.

## 10. Adaptive design
Use window size classes / available window size rather than device-brand checks. Design compact phone, medium, expanded tablet, landscape and resizable-window compositions. On tablets, use intentional two-pane or max-width content, not merely two long card columns.
