# Visual QA and Aesthetic Review Process

Aesthetic quality is verified from rendered output, not source code or AI-generated mockups alone.

## Before redesigning a screen
1. Capture the current app screen on a real/emulated device.
2. Record device width/height, density, orientation, font scale, theme and navigation context.
3. Identify the user's main task and current friction.
4. Mark visual hierarchy: focal point, primary action, supporting information and low-frequency controls.
5. Compare the screen against its approved specification and design tokens.

## Visual review dimensions
- Composition: clear focal point, balance, alignment and whitespace.
- Hierarchy: title, key metric/task, primary action, secondary content.
- Typography: scale, weight, line height, truncation and tabular numbers.
- Color: OLED black, layered surfaces, muted accent and readable contrast.
- Density: no repeated metrics, unnecessary cards or equal-weight sections.
- Components: consistent controls, radii, icon sizes and states.
- Motion: intentional, brief and non-distracting.
- Adaptive behavior: compact phone, tablet portrait/landscape, split screen and large font.
- Accessibility: TalkBack order, touch targets, content descriptions and non-color state cues.
- State coverage: first run, populated, empty, loading, error, offline, permission denied and success.

## Screenshot matrix
For every primary screen capture:
- OLED default theme
- Standard Dark
- Light
- Compact phone
- Expanded tablet portrait
- Expanded tablet landscape
- Default and enlarged font scale
- Empty and populated state

Do not require every combination for every minor screen; prioritize critical user flows.

## Comparison method
Review current and proposed screenshots side by side. Fix the largest usability/hierarchy issue first, then spacing/typography, then decorative polish. Never accept a screen because it merely looks more colorful.

## Evidence record
For each screen, store screenshots, device/configuration, issue list, decision, implementation commit and retest result under a documented QA folder. Do not commit personal health data or identifiable notifications in screenshots.
