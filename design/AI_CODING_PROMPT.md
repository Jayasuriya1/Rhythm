# Rhythm — Mandatory AI Coding Prompt

You are contributing to the existing Rhythm Android application. Improve the existing product while preserving its identity, architecture, user data and working features.

## First action: read the project contract
Read `AGENTS.md`, `design/AI_WORKFLOW.md`, `design/PRODUCT_VISION.md`, `design/DESIGN_PHILOSOPHY.md`, `design/DESIGN_SYSTEM.md`, `design/BRAND_ASSETS.md`, `design/COMPONENTS.md`, `design/ACCESSIBILITY.md`, `design/QUALITY_GATES.md` and the relevant feature specification under `design/screens/`. Do not skip this step.

## Product identity and visual rules
Rhythm is a calm, private, offline-first daily companion for routines, sleep, focus, personal wellness records and reminders. It is supportive, not clinical or gamified. Keep a calm, modern, low-glare aesthetic. Default OLED Dark; also support Standard Dark, Light and System. Preserve the Ribbon R launcher logo and brand palette. Avoid neon, gratuitous gradients, glassmorphism, card-in-card layouts, arbitrary radii, feature palettes and clutter. Use one clear focal point and primary action per screen.

## Central theme rules
- Runtime source of truth: `app/src/main/java/com/example/ui/theme/Color.kt`, `Theme.kt`, `Type.kt`.
- Use `MaterialTheme.colorScheme` for ordinary UI surfaces, text, borders, controls and states.
- Reuse named `State*`, `HealthMetric*` and feature semantic tokens.
- Never hardcode `Color(0x...)` or hex/RGB in screen or reusable component code. Add missing semantic tokens centrally for all themes.
- Do not create another theme system or silently replace Rhythm colors with Android dynamic colors.
- Use matching `on*` foreground roles.

## Engineering constraints
- Stack: Kotlin, Jetpack Compose, Material 3, MVVM, Coroutines/Flow and Room.
- Preserve application ID `com.jayasuriya.rhythm`, Room schemas/migrations, stored data and existing features.
- During visual-only work, do not change alarms, medication logic, timer behavior or navigation.
- Privacy-first and offline-first. No analytics, tracking, cloud/network services or new dependencies without explicit approval.
- Reuse components and design for compact phones, tablets, landscape, split-screen and large fonts.

## Required implementation and validation
1. Audit current screen, related components, tokens, state, tests and latest CI baseline.
2. State the problem, files to change, intended result and regression risks.
3. Implement the smallest coherent change using existing architecture, tokens and components.
4. Preserve behavior unless a behavior change is explicitly requested; add/update appropriate tests.
5. After the final edit, run from repository root:

   ```bash
   gradle :app:assembleDebug :app:lintDebug --stacktrace
   gradle :app:testDebugUnitTest -ProbolectricOffline=false --stacktrace
   ```

   These run assembleDebug, lintDebug and Robolectric testDebugUnitTest. Repeat the failed task after a fix, then rerun the complete gate set. Never disable lint, skip tests, weaken assertions or suppress errors to get a green result.
6. The repository currently has no Gradle wrapper; use installed Gradle. CI uses JDK 17 and Gradle 9.3.1. If local checks cannot run, state **not run** and wait for GitHub Actions.
7. Inspect real screenshots in OLED Dark, Standard Dark and Light, plus relevant phone/tablet sizes. Mockups are not proof.
8. Check contrast, TalkBack, 48dp targets, font scaling, reduced motion and relevant empty/error/offline states.
9. Report files changed, behavior preserved/changed, exact test results, screenshot/device coverage and limitations.
10. Submit a focused PR. Do not merge unless latest required GitHub Actions checks pass.

## Stop and ask before
Changing app ID, database schema or stored data; removing features; changing medication/alarm/reminder/timer behavior during visual work; adding cloud/network services, analytics or SDKs; changing logo/brand identity; or guessing through conflicting requirements.

**Success means:** the feature works, fits Rhythm's visual language, remains accessible and responsive, preserves existing data and behavior, and has passed build, lint and unit-test gates with honest evidence.

## Semantic tokens
Reminder category accents, sleep-stage chart series and Focus Studio backdrop colors are centralized in `Color.kt`. Reuse those tokens. Persisted user-created study category colors are user choices; do not overwrite them.
