# Visual Screenshot Review — Initial Evidence Pass

Date: 2026-11-05
Source: screenshots already committed under public/assets/aistudio/
Status: visual inspection completed for the listed captures. These captures appear to predate some newer source changes on this branch; re-capture the current build before treating every observation as a current regression.

## Screens inspected

- Today: screenshot_1_58aa8fe5.png (lower scroll area)
- Routine: screenshot_2_505b53a2.png, screenshot_2_e14f69b4.png
- Health: screenshot_4_68a7a7d4.png
- Study: screenshot_5_0545290f.png, screenshot_1_a61dc79e.png, screenshot_3_e6182306.png
- Meditation: screenshot_6_69bffce1.png
- Settings: screenshot_7_833029d3.png
- Alarm / QR Studio / code library: screenshot_1_17f7f48d.png, screenshot_8_1b62f7d1.png, screenshot_9_e95bef3d.png
- Brain dump: screenshot_10_d0c9f906.png
- Xiaomi Pad 7 / expanded layout: Screenshot_2026-11-05-09-15-23-279...jpg, Screenshot_2026-11-05-09-15-25-341...jpg, Screenshot_2026-11-05-09-16-57-890...jpg, Screenshot_2026-11-05-09-17-09-144...jpg, Screenshot_2026-11-05-09-18-45-660...jpg

## Observations

### P1 — Today: duplicated empty-state action and nested surface
The sleep summary shows a “Log Sleep” action in the section header and another in its empty state. The empty state is also rendered as a card inside the parent sleep card. A floating quick action overlaps nearby content in the captured scroll position.
**Branch change:** removed the duplicate header CTA when no sleep entry exists and replaced the nested empty-state card with a compact inline empty state and one action. Retest FAB placement on current build.

### P1 — Routine: oversized repeated anchor cards
Each anchor uses a large card, a repeated target/actual/variance panel and three actions. “Done Now” wraps over two lines in the captured phone width. The page feels like a stack of forms rather than a clear timeline.
**Recommendation:** redesign as a compact vertical timeline with one primary action and contextual secondary actions; show variance only when an actual time exists.

### P1 — Study: responsive action layout
The captured phone screenshot shows “New Task” compressed into a very narrow tall button. The current branch changes the task header actions to accessible 48dp icon buttons with descriptions. Retest at compact widths.
The captured screenshots also show repeated DSA filter chips and a very large green timer surface. Current source already deduplicates filter categories by normalized name; verify current data/build before changing category persistence. The timer hero should use a restrained surface and clear primary/secondary action hierarchy.

### P1 — Navigation label wrapping
“ Meditation ” wraps onto two lines in the captured bottom navigation. The current branch shortens the navigation label to “Meditate”; retest on phone and tablet.

### P1 — Alarm editor complexity
The New Custom Alarm dialog contains many settings sections and an internal scroll area, with controls partially clipped in the captured state. Consider a dedicated full-screen alarm editor or progressive sections (Basics → Schedule → Sound → Dismissal → Appearance), preserving a quick basic-alarm path.

### P2 — Meditation controls wrap and compete
Guidance mode labels and breathing presets wrap into narrow multi-line buttons; one preset appears as a tall narrow control. Use adaptive chip flow/wrapping with consistent minimum widths and keep advanced timing sliders in a secondary settings section.

### P2 — Brain dump tag row
Tag choices extend beyond the visible width. Use a FlowRow/wrapping layout or an intentional horizontally scrollable chip group with clear affordance.

### P2 — QR Studio accent inconsistency
The registered-code panel uses a strong amber/orange surface that competes with the restrained OLED sage palette. Keep the QR preview high contrast, but make the surrounding surface neutral and use amber only as a semantic accent.

### P2 — Tablet dialogs and content density
On Xiaomi Pad 7 captures, protocol and focus-studio dialogs occupy much of the available workspace. They are usable but dense. Consider a wide side sheet or dedicated settings pane for advanced configuration; retain a focused primary timer view.

### P2 — Expanded navigation density
The tablet rail exposes many destinations at once. Preserve discoverability but visually separate primary destinations from utilities (Brain Dump, Reminders, Alarms, Settings) and test touch/label scanning.

### P2 — Settings status presentation
The integrations area uses large nested containers and status blocks. Current branch corrects the cloud-sync wording. Further redesign should use compact rows with clear states, provenance and links to details.

## Current-source changes made after these captures
- OLED palette softened to muted sage/mint.
- Full Material typography scale implemented.
- Routine target-time editor now calls a persistence operation rather than marking the anchor complete.
- Cloud sync now fails closed rather than reporting fake success.
- Study task actions changed to compact icon buttons.
- Meditation bottom-navigation label shortened.
- Today sleep empty state now has one action and no nested card.

## Next visual QA pass
Rebuild current branch, capture Today, Routine, Study, Health, Meditation, Alarm editor and Settings on phone and Xiaomi Pad 7 in portrait/landscape. Compare against the redesign specifications and verify no behavior regression.
