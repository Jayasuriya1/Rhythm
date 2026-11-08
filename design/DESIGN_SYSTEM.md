# Rhythm Design System

**Status: v2.0 — canonical implementation contract.** Kotlin theme files are the runtime source of truth for colors and typography. This document defines how every screen and contributor must use them.

## 1. Theme architecture
Rhythm has three explicit visual themes plus system-following behavior:
- **OLED Dark** — default; true-black canvas and calm layered surfaces.
- **Standard Dark** — deep teal-charcoal canvas, distinct from OLED.
- **Light** — warm, low-glare off-white canvas.
- **System** — follows Android light/dark preference.

All themes are brand-controlled. Dynamic Android wallpaper colors must not silently replace Rhythm's identity. The persisted user theme preference remains authoritative. Every screen, dialog, bottom sheet, chart, notification and immersive flow must respect the selected mode unless documented feature-specific art direction requires a controlled exception.

## 2. Single source of truth
Runtime tokens:
- `app/src/main/java/com/example/ui/theme/Color.kt` — brand, semantic, theme and chart colors.
- `app/src/main/java/com/example/ui/theme/Theme.kt` — Material 3 color schemes and theme selection.
- `app/src/main/java/com/example/ui/theme/Type.kt` — typography scale.
- `app/src/main/res/drawable/ic_launcher_foreground.xml` and `ic_launcher_background.xml` — approved Ribbon R app mark.

Rules:
1. Use `MaterialTheme.colorScheme` for ordinary UI: primary, secondary, tertiary, background, surface, surfaceVariant, outline, error and matching `on*` colors.
2. Use named `State*` tokens only for semantic statuses and `HealthMetric*` tokens only for chart series.
3. Never add screen-local hex/RGB values, duplicate palettes, arbitrary gradients or feature-specific primary colors.
4. Do not use `Color(0x...)` outside the central theme package except for documented illustration/brand assets or system-provided colors.
5. Brand accents are not status colors. Status meaning must remain consistent and never rely on color alone.
6. Add missing semantic tokens centrally with light/dark/OLED values and document their purpose before use.
7. Reusable components must not hardcode theme assumptions.

## 3. Brand identity
The approved identity is the original **Ribbon R with a small check motif**, representing continuity and gentle progress. Keep it recognizable and centered in the adaptive-icon safe area; no wordmark text inside the launcher icon.

| Token | Hex | Usage |
|---|---|---|
| Deep teal | `#123C3A` | Brand depth / launcher |
| Midnight teal | `#102B2C` | Supporting brand depth |
| Mint | `#9ADCC6` | Main brand accent |
| Seafoam | `#49BBA8` | Supporting accent, sparingly |
| Champagne | `#FFE6B9` | Warm highlight, sparingly |
| Muted violet | `#BDA6DC` | Reflective accent, sparingly |
| Warm light canvas | `#F5F8F6` | Light theme |
| OLED canvas | `#000000` | OLED theme |

No neon emerald, saturated rainbow accents, gradients on routine cards or a different accent for each feature. Gradients belong only in approved brand artwork and optional focus atmosphere.

## 4. Theme behavior and accessibility
- Foregrounds use the matching `on*` role for their container.
- Target WCAG 2.2 AA: 4.5:1 normal text, 3:1 large text and essential UI components.
- Disabled states remain legible; status is never color-only.
- Missing health data is not zero.
- Charts provide labels, units, date range and text summary.
- Respect font scaling, TalkBack, touch targets and reduced motion.
- Light mode receives equal QA; it is not an afterthought.

## 5. Typography
Use shared `Typography` from `Type.kt` and Material 3 roles rather than local recurring `sp` styles. Use tabular numerals for timers and aligned numeric data. Reflow rather than shrinking text. System sans-serif remains default until font licensing and F-Droid redistribution are verified.

## 6. Layout, spacing and shape
Use a 4dp base grid: 4dp micro, 8dp compact, 12dp control/group padding, 16dp standard gutter, 24dp section separation, 32dp major separation, 48dp breathing space. Prefer composition over wrapping every section in a card. No card-in-card by default. Use tonal layering before borders, shadows or gradients. Keep Material 3 shapes consistent.

## 7. Adaptive layouts
Use available window size/window size classes, not device-brand checks. Support compact phones, medium/expanded tablets, landscape, split-screen and resizable windows. Tablets need intentional max-width or two-pane composition; never stretch a phone column edge-to-edge.

## 8. Feature art direction
Focus Studio may use atmospheric backdrops, but controls, text, dialogs and core actions use shared Rhythm tokens. Alarm screens may prioritize high-visibility interaction but still use semantic roles and accessible contrast. Feature identity never justifies a second app-wide palette.

## 9. Screen review checklist
- [ ] Shared theme roles; no local palette or unexplained literal colors.
- [ ] OLED, Standard Dark, Light and System behavior checked.
- [ ] Contrast and status semantics checked.
- [ ] Typography, spacing, shapes and icons consistent.
- [ ] Empty/loading/error/disabled/selected/success states handled.
- [ ] Phone/tablet/landscape/large-font layouts reviewed.
- [ ] TalkBack and touch targets reviewed.
- [ ] Real screenshots inspected; no mockup-only approval.


## 10. Semantic feature tokens
The central `Color.kt` also owns named tokens for reminder categories, sleep-stage chart series and focus-backdrop surfaces. Screens must import these tokens rather than define their own category or chart colors. Custom user-created study category colors remain user data and must be preserved; only default palette choices should be centralized when they are changed.
