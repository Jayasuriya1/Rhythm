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
Every feature, bug fix, refactor, dependency update, manifest/resource change and Gradle configuration change must pass all three checks. Run from repository root:

```bash
gradle :app:assembleDebug :app:lintDebug --stacktrace
gradle :app:testDebugUnitTest -ProbolectricOffline=false --stacktrace
```

| Required check | Gradle task | Purpose |
|---|---|---|
| Assemble debug APK | `:app:assembleDebug` | Compile and package debug APK |
| Android lint | `:app:lintDebug` | Static Android/code/resource checks |
| Robolectric unit tests | `:app:testDebugUnitTest -ProbolectricOffline=false` | JVM tests with Android resources and Robolectric |

The first command executes assembleDebug and lintDebug; the second executes the unit-test task. A successful assemble alone does not mean lint and tests passed.

### CI environment and constraints
- GitHub Actions uses JDK 17, Gradle 9.3.1, Android SDK platform 36.1 and Build Tools 36.0.0.
- CI creates a temporary debug signing key and runs Robolectric with online dependency resolution.
- Local Robolectric defaults offline; use `-ProbolectricOffline=false` to match CI.
- The repository currently lacks `gradlew`, `gradlew.bat` and `gradle-wrapper.jar`. Use installed `gradle`; do not add wrapper files incidentally.
- If local tooling/network prevents a check, mark it **not run**. Never claim it passed. Latest GitHub Actions on the PR is the authoritative merge gate.

### Failure handling
1. Read the first meaningful error and relevant stack trace, not just the final `BUILD FAILED`.
2. Classify it: compile, dependency/repository, Gradle/plugin, resource/manifest, lint, assertion, Robolectric runtime or environment.
3. Fix the root cause. Verify dependency aliases/versions in `gradle/libs.versions.toml`, imports, package names, manifests, resources and test fixtures.
4. Never hide failures by disabling lint, skipping tests, excluding source sets, weakening assertions or broad suppressions.
5. Rerun the failed task, then rerun the complete gate set after the final edit.
6. Push final changes and wait for latest PR workflow to pass before declaring completion or merging.

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
