# AI Contribution Contract — Rhythm

This is a mandatory instruction for **Google AI Studio (Gemini), Antigravity, ChatGPT, Claude, Codex and human contributors**. These rules apply to every UI, design, refactor and feature task in this repository.

## Mandatory reading order
Before editing any file, read:
1. `AGENTS.md`
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
- Use `MaterialTheme.colorScheme` for normal UI surfaces, text, borders and controls.
- Never hardcode hex/RGB colors in screen files or reusable components. Add missing semantic tokens centrally.
- Preserve the approved Ribbon R logo and brand palette.
- Keep the product calm, modern, low-glare and editorial. Avoid neon, gratuitous gradients, glassmorphism, card-in-card layouts and decorative clutter.
- Use one clear focal point and one primary action per screen.
- Preserve OLED Dark, Standard Dark, Light and System behavior.
- Reuse existing Compose components before creating duplicates. Support compact phones and expanded tablets.
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

## Mandatory feature implementation workflow
1. **Baseline first:** inspect relevant code, Gradle configuration, dependency catalog, manifest, tests and latest CI. Identify pre-existing failures.
2. **Plan small:** state user problem, affected files, expected behavior and regression risks. Implement one coherent vertical slice.
3. **Respect configuration:** do not casually upgrade AGP, Kotlin, Gradle, SDK, Compose, KSP, Room or plugin versions. Verify dependency aliases and compatibility; do not invent aliases.
4. **Implement safely:** reuse established architecture and components. Add/update tests for changed logic, persistence and important UI interactions.
5. **Run the exact CI-equivalent gates after implementation.** Required for every feature, bug fix, refactor, dependency, manifest, resource or Gradle change.
6. **Fix and repeat:** inspect the first meaningful error and stack trace, fix root cause, rerun failed task, then rerun the complete gate set. Never hide errors by disabling lint, skipping tests, weakening assertions, excluding source files or broad suppressions.
7. **Regression review:** check adjacent flows, lifecycle, process recreation, permission denial, empty/loading/error/offline states, accessibility, themes and device sizes.
8. **Evidence-based handoff:** report exact commands, pass/fail, tests added, screenshots/device coverage and anything not run. Distinguish CI from local and physical-device verification.
9. **PR discipline:** submit a focused branch/PR. Do not merge until the required GitHub Actions workflow succeeds on the latest PR commit.

## Exact build and test commands
The current CI uses JDK 17, Gradle 9.3.1, Android SDK platform 36.1 and Build Tools 36.0.0. Run from repository root:

```bash
gradle :app:assembleDebug :app:lintDebug --stacktrace
gradle :app:testDebugUnitTest -ProbolectricOffline=false --stacktrace
```

These cover the three mandatory checks:
- `:app:assembleDebug` — compile and package debug APK.
- `:app:lintDebug` — Android lint.
- `:app:testDebugUnitTest -ProbolectricOffline=false` — JVM/Robolectric unit tests with CI online dependency resolution.

The first command executes assemble and lint; the second executes unit tests. Do not substitute generic `build` or claim assemble alone covers all checks.

### Environment notes
- The repository currently has no `gradlew`, `gradlew.bat` or `gradle-wrapper.jar`; use installed `gradle`. Do not add a wrapper incidentally.
- If local Gradle/JDK/SDK differ from CI, report the mismatch. GitHub Actions is authoritative.
- Local Robolectric defaults offline; use `-ProbolectricOffline=false` to match CI.
- CI creates a temporary debug signing key. Never commit keystores, credentials, `.env` or secrets.
- If the environment cannot run Gradle, mark checks **not run** and wait for latest PR CI before declaring completion.

## Required workflow summary
1. Inspect implementation, tokens/components, related screens and tests.
2. Identify user problem and intended hierarchy.
3. Plan smallest coherent change; separate visual and behavior work.
4. Implement using shared tokens/components.
5. Add/update tests and previews where practical.
6. Run both exact Gradle commands above after the final edit. If code changes after a successful run, rerun.
7. Review real screenshots for OLED, Standard Dark and Light; include compact phone/tablet when relevant.
8. Check accessibility and empty/loading/error/permission/offline states.
9. Report changed files, preserved/changed behavior, exact results and limitations.
10. Submit focused PR; never merge without successful required CI.

## Stop conditions
If instructions conflict with the source of truth, stop and explain. Ask before changes that risk persisted data or core behavior.

## Semantic token policy
Reminder category accents, sleep-stage chart series and Focus Studio backdrop colors belong in central `Color.kt` tokens. Reuse named tokens. Persisted user-created study category colors are user data and must not be overwritten.
