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

## Build and test
A GitHub Actions workflow installs Gradle 9.3.1 directly, runs :app:assembleDebug and :app:lintDebug, then runs :app:testDebugUnitTest with Robolectric online mode. The app keeps offline Robolectric mode as the local default and CI opts into Maven downloads. The repository tree currently lacks gradlew, gradlew.bat and gradle-wrapper.jar, so local wrapper commands are not available. Await a successful workflow result before claiming verification. Record actual run results; a successful compile is not proof of correct behavior.

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
