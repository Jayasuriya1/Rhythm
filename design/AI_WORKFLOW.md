# AI Contribution Contract — Rhythm

This is a mandatory instruction for **Google AI Studio (Gemini), Antigravity, ChatGPT, Claude, Codex and human contributors**. These rules apply to every UI, design, refactor and feature task in this repository.

## Mandatory reading order
Before editing any file, read:
1. `AGENTS.md` (repository-wide rules)
2. `design/README.md`
3. `design/PRODUCT_VISION.md`
4. `design/DESIGN_PHILOSOPHY.md`
5. `design/DESIGN_SYSTEM.md`
6. `design/BRAND_ASSETS.md`
7. `design/COMPONENTS.md`
8. The relevant screen specification and `design/QUALITY_GATES.md`

For visual tasks also read `design/VISUAL_QA.md` and `design/ACCESSIBILITY.md`.

## Absolute design rules
- The Kotlin theme package is the runtime source of truth. Never create another theme or local palette.
- Use `MaterialTheme.colorScheme` for all normal UI surfaces, text, borders and controls.
- Never hardcode hex/RGB colors in screen files or reusable components. Add missing semantic tokens centrally.
- Preserve the approved Ribbon R logo and brand palette. Do not redraw, replace, recolor or create a competing logo without explicit approval.
- Keep the product calm, modern, low-glare and editorial. Avoid neon, gratuitous gradients, glassmorphism, card-in-card layouts and decorative clutter.
- Use one clear focal point and one primary action per screen.
- Preserve OLED Dark, Standard Dark, Light and System behavior. Check all applicable themes.
- Reuse existing Compose components before creating duplicates. Follow Material 3.
- Support compact phone and expanded tablet layouts; do not assume phone-only dimensions.
- Respect font scaling, TalkBack, 48dp touch targets, contrast and reduced motion.

## Engineering and safety rules
- Inspect the existing implementation before changing it. State affected files and behavior.
- Keep visual-only changes visual-only. Do not change business logic, Room schema, stored data, application ID, alarms, timers, medication logic or navigation unless explicitly required.
- Do not remove, hide or replace existing features to simplify a screen.
- Do not add dependencies without explaining need, license, size and maintenance.
- Privacy-first and offline-first. No analytics, trackers, remote APIs or cloud sync without explicit approval.
- No medical diagnosis, treatment recommendations or unsupported health conclusions in product copy.
- Do not commit secrets, real personal/health data, fake production metrics or unlicensed assets.
- Update design documentation when a design decision changes.

## Required workflow
1. Inspect current code, tokens/components, related screens and tests.
2. Identify the user problem and current screen hierarchy.
3. Plan the smallest coherent change; separate behavior from visual work.
4. Implement using shared tokens/components.
5. Add/update previews and automated tests where practical.
6. Run relevant Gradle build, tests and lint. Never claim a command passed if it was not run.
7. Review real rendered screenshots for OLED, Standard Dark and Light; include compact phone/tablet when relevant.
8. Check accessibility, empty/loading/error/permission/offline states.
9. Report changed files, behavior preserved/changed, commands/results, screenshot/device coverage and limitations.
10. Submit a focused branch/PR. Never merge without successful required CI and review.

## Stop conditions
If instructions conflict with the current source of truth, stop and explain instead of inventing a new style. If a visual change would alter persisted data or core behavior, ask before proceeding.


## Semantic token policy
Reminder category accents, sleep-stage chart series and Focus Studio backdrop colors belong in the central `Color.kt`. Screens must reuse named tokens. Persisted user-created category colors are user data and must not be overwritten during visual cleanup.
