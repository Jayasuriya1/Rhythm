# Quality and Release Gates

## Feature completion
- [ ] User problem and outcome documented.
- [ ] Screen specification approved.
- [ ] Primary action obvious.
- [ ] Loading, empty, error, disabled, offline, permission-denied and success states handled as applicable.
- [ ] Persistence and lifecycle behavior verified.
- [ ] Accessibility checked.
- [ ] OLED, Standard Dark, Light and System themes checked.
- [ ] Phone, tablet, landscape and split-screen layouts checked.
- [ ] Adjacent flows regression-tested.
- [ ] Automated tests pass.
- [ ] Documentation updated.

## Mandatory Android build and test gate
Every feature, bug fix, refactor, dependency update, manifest/resource change and Gradle configuration change must pass all three checks. Run from the repository root:

```bash
gradle :app:assembleDebug :app:lintDebug --stacktrace
gradle :app:testDebugUnitTest -ProbolectricOffline=false --stacktrace
```

| Required check | Gradle task | What it verifies |
|---|---|---|
| Assemble debug APK | `:app:assembleDebug` | Android debug compilation and APK packaging |
| Android lint | `:app:lintDebug` | Static Android/code/resource checks |
| Robolectric unit tests | `:app:testDebugUnitTest -ProbolectricOffline=false` | JVM tests with Android resources and Robolectric |

The first command executes assembleDebug and lintDebug; the second executes the Robolectric unit-test task. Do not substitute a generic build command or assume a successful assemble means lint and tests passed.

### CI environment and known repository constraints
- GitHub Actions currently uses JDK 17, Gradle 9.3.1, Android SDK platform 36.1 and Build Tools 36.0.0.
- The workflow creates a temporary debug signing key and runs Robolectric with online dependency resolution.
- The app's local Robolectric default is offline; use `-ProbolectricOffline=false` to match CI.
- The repository currently does not contain `gradlew`, `gradlew.bat` or `gradle-wrapper.jar`. Use installed `gradle` locally; do not add a wrapper as an unrelated feature change.
- If local tooling or network access prevents a check, record it as **not run**. Never claim it passed. GitHub Actions on the latest PR commit is the authoritative merge gate.

### Failure handling — mandatory
1. Read the first meaningful error, not only the final `BUILD FAILED` line. Inspect the relevant task output and stack trace.
2. Classify the failure: Kotlin/Java compilation, unresolved dependency/repository, Gradle/plugin configuration, Android resource/manifest, lint, test assertion, Robolectric SDK/runtime, or environment/toolchain.
3. Fix the root cause in the feature change. Check dependency aliases and versions in `gradle/libs.versions.toml`; check imports, package names, manifest declarations, resources and test fixtures.
4. Do not hide failures by disabling lint, skipping tests, excluding source sets, weakening assertions, adding broad suppressions, or changing CI checks without a justified reviewed reason.
5. Rerun the failed task, then rerun the complete two-command gate set after the final code edit.
6. Push the final changes and wait for the latest PR workflow to pass before declaring the feature complete or merging.

## Privacy and data
- [ ] Local storage claims verified against implementation.
- [ ] AI/network requests and payloads disclosed clearly.
- [ ] Cloud sync never reports success without verified transfer.
- [ ] Backup/export and restore tested.
- [ ] Sensitive data not unintentionally exposed in logs/notifications.
- [ ] Permissions requested in context.
- [ ] Connected-source provenance shown.
- [ ] Disconnect and deletion behavior clear.
- [ ] No unapproved tracking or analytics.

## F-Droid readiness
See [F-Droid readiness audit](F_DROID_AUDIT.md). F-Droid acceptance is a review, not an automatic consequence of an open-source license.

## Release path
Internal device testing → public GitHub pre-release/APK → stable GitHub release → F-Droid submission after policy, dependency and reproducibility review.
