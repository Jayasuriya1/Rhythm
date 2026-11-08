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

## Mandatory feature implementation workflow
1. **Baseline first.** Inspect the relevant code, Gradle configuration, dependency catalog, manifest, existing tests and latest CI result. Identify the current failure baseline before changing code.
2. **Plan small.** State the user problem, files likely to change, expected behavior and regression risks. Implement one coherent vertical slice; do not generate a large batch of unrelated changes.
3. **Respect project configuration.** Do not casually upgrade AGP, Kotlin, Gradle, SDK, Compose, KSP, Room or plugin versions. Do not invent dependency aliases. Add a dependency only after checking whether an equivalent library is already present, its compatible version, repository availability and license.
4. **Implement safely.** Reuse established architecture, naming, state handling and components. Keep UI, domain and persistence responsibilities separated. Add/update unit tests for changed logic, Room tests for persistence/migrations, and Compose/UI tests for important interactions where practical.
5. **Run the exact CI-equivalent gates after implementation.** This is required for every feature, bug fix, refactor, dependency, manifest, resource or Gradle change—not only before release.
6. **Fix and repeat.** If any command fails, inspect the first meaningful compiler/lint/test error and its complete relevant stack trace. Fix the root cause; do not hide errors by disabling lint, skipping tests, weakening assertions, excluding source files, suppressing warnings broadly or changing CI to green without a valid technical reason. Rerun the failed command, then rerun the complete gate set.
7. **Regression review.** Check adjacent user flows, lifecycle, process recreation, permission denial, empty/loading/error/offline states, accessibility and applicable themes/device sizes.
8. **Evidence-based handoff.** Report exact commands, pass/fail status, relevant failure fixes, tests added, and anything not run. Distinguish GitHub CI evidence from local and physical-device verification.
9. **PR discipline.** Submit a focused branch/PR. Do not merge until the required GitHub Actions workflow succeeds on the latest PR commit.

## Exact build and test commands
The repository CI currently uses Gradle 9.3.1, JDK 17, Android SDK platform 36.1 and Build Tools 36.0.0. It runs these commands from the repository root:

```bash
gradle :app:assembleDebug :app:lintDebug --stacktrace
gradle :app:testDebugUnitTest -ProbolectricOffline=false --stacktrace
```

These are the three required checks:
- ` :app:assembleDebug ` — compile/package the debug APK.
- ` :app:lintDebug ` — run Android lint for the debug variant.
- ` :app:testDebugUnitTest -ProbolectricOffline=false ` — run JVM/Robolectric unit tests using CI's online dependency resolution mode.

The first Gradle command runs assemble and lint as separate Gradle tasks in one invocation. The second runs the Robolectric unit-test task. Do not replace these with a generic `build` task or claim that assembling alone covers lint/tests.

### Environment-specific notes
- The repository currently has no Gradle wrapper scripts/jar. Use the installed `gradle` command in the AI Studio/terminal environment; do not invent or commit a wrapper as an incidental feature change.
- CI pins Gradle 9.3.1 and JDK 17. If local Gradle/JDK/SDK versions differ, record that mismatch; GitHub Actions is the authoritative gate.
- Local Robolectric defaults to offline mode. Use `-ProbolectricOffline=false` exactly as CI does so missing Robolectric artifacts can be downloaded.
- CI creates a temporary debug signing key. Do not commit a keystore, passwords, `.env`, Firebase config secrets or personal credentials.
- If the environment cannot run Gradle (no installed Gradle/JDK/SDK, no network, or restricted terminal), do not claim validation. Explain the limitation and rely on the PR workflow; wait for its result before marking the feature complete.

## Required workflow
1. Inspect current code, tokens/components, related screens and tests.
2. Identify the user problem and current screen hierarchy.
3. Plan the smallest coherent change; separate behavior from visual work.
4. Implement using shared tokens/components.
5. Add/update previews and automated tests where practical.
6. Run **both exact Gradle commands above** after the final code edit. If you edit code after a successful run, rerun the gates.
7. Review real rendered screenshots for OLED, Standard Dark and Light; include compact phone/tablet when relevant.
8. Check accessibility, empty/loading/error/permission/offline states.
9. Report changed files, behavior preserved/changed, exact commands/results, screenshot/device coverage and limitations.
10. Submit a focused branch/PR. Never merge without successful required CI and review.

## Stop conditions
If instructions conflict with the current source of truth, stop and explain instead of inventing a new style. If a visual change would alter persisted data or core behavior, ask before proceeding.
