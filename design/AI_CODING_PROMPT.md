# Rhythm — Mandatory AI Coding Prompt

You are contributing to the existing Rhythm Android application. You are not designing a new app. Your task is to improve the existing product while preserving its identity, architecture, user data and working features.

## First action: read the project contract
Read these repository files before planning or editing:
- `AGENTS.md`
- `design/AI_WORKFLOW.md`
- `design/PRODUCT_VISION.md`
- `design/DESIGN_PHILOSOPHY.md`
- `design/DESIGN_SYSTEM.md`
- `design/BRAND_ASSETS.md`
- `design/COMPONENTS.md`
- `design/ACCESSIBILITY.md`
- `design/QUALITY_GATES.md`
- The relevant feature specification under `design/screens/`

Do not skip this step. If the environment cannot access these files, ask for them instead of inventing design rules.

## Product identity
Rhythm is a calm, private, offline-first daily companion for routines, sleep, focus, personal wellness records and reminders. It is supportive, not clinical or gamified. It must never shame users, diagnose conditions or make unsupported health claims.

## Visual identity — mandatory
- Calm, modern, low-glare, editorial wellness aesthetic.
- Default theme OLED Dark; also support Standard Dark, Light and System.
- Brand colors: deep teal `#123C3A`, midnight teal `#102B2C`, muted mint `#9ADCC6`, seafoam `#49BBA8`, champagne `#FFE6B9`, muted violet `#BDA6DC`, warm light canvas `#F5F8F6`, OLED canvas `#000000`.
- Use mint sparingly as the primary accent. Champagne and violet are secondary accents, not status colors.
- Preserve the existing Ribbon R launcher logo. Never replace or redraw it without explicit approval.
- No neon, gratuitous gradients, glassmorphism, card-in-card layouts, arbitrary radii, feature-specific palettes or decorative clutter.
- One clear focal point and one primary action per screen.

## Central theme rules — absolute
- Runtime source of truth: `app/src/main/java/com/example/ui/theme/Color.kt`, `Theme.kt`, `Type.kt`.
- Use `MaterialTheme.colorScheme` for ordinary UI colors, surfaces, text, borders, controls and states.
- Use existing named `State*` tokens for semantic status and `HealthMetric*` tokens for chart series.
- Never hardcode `Color(0x...)` or hex/RGB values in screen code or reusable components.
- If a token is missing, add it centrally for all applicable themes and document it before use.
- Do not add another theme system or let Android dynamic colors silently replace the Rhythm brand.
- Text foregrounds must use the matching `on*` role for the container.

## Engineering constraints
- Inspect existing implementation before making changes.
- Stack: Kotlin, Jetpack Compose, Material 3, MVVM, Coroutines/Flow and Room.
- Preserve the existing application ID `com.jayasuriya.rhythm`.
- Do not alter Room schemas, migrations, persisted data, alarms, medication logic, timer behavior, navigation or existing features during visual-only work.
- Keep privacy-first and offline-first. No analytics, tracking, cloud/network services or new dependencies without explicit approval.
- Do not add unlicensed fonts, icons, illustrations or audio.
- Use existing components before creating duplicates.
- Design intentionally for compact phones and expanded tablets, landscape, split-screen and large font settings.

## Required implementation process
1. Audit the current screen, related components, tokens, state and tests.
2. Explain the problem and intended change briefly.
3. Implement the smallest coherent change using existing tokens and components.
4. Preserve existing behavior unless the task explicitly approves a behavior change.
5. Add/update previews and automated tests where practical.
6. Run these exact CI-equivalent commands from the repository root after the final edit:

   ```bash
   gradle :app:assembleDebug :app:lintDebug --stacktrace
   gradle :app:testDebugUnitTest -ProbolectricOffline=false --stacktrace
   ```

   These execute assembleDebug, lintDebug and testDebugUnitTest. Repeat the full gate set after fixing any failure. Do not skip tests, disable lint, weaken assertions or suppress errors just to obtain a green result. The project currently has no Gradle wrapper; use installed Gradle. GitHub Actions uses Gradle 9.3.1 and JDK 17 and is the authoritative result if local tooling is unavailable.
7. Inspect real screenshots in OLED Dark, Standard Dark and Light, plus relevant phone/tablet sizes. Generated mockups are not proof of implementation quality.
8. Check contrast, TalkBack, 48dp touch targets, font scaling and reduced motion.
9. Report files changed, behavior preserved/changed, exact test results, screenshot/device coverage and remaining limitations. Clearly say if any command could not be run.
10. Submit a focused branch/PR. Do not merge unless the latest PR commit has a successful required GitHub Actions run.

## Stop and ask before
- Changing app identity/package ID, database schema or stored data.
- Removing or replacing an existing feature.
- Changing medication, alarm, reminder or timer behavior during a design task.
- Adding network/cloud integrations, analytics or third-party SDKs.
- Changing the approved logo or brand palette.
- Resolving conflicting requirements by guessing.

**Success means:** the feature works, fits Rhythm's established visual language, remains accessible and responsive, preserves user data and existing behavior, and has passed the required build, lint and unit-test gates with honest evidence.
