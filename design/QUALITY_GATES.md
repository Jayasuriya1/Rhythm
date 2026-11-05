# Quality and Release Gates

## Feature completion
- [ ] User problem and outcome documented.
- [ ] Screen specification approved.
- [ ] Primary action obvious.
- [ ] Loading, empty, error, disabled and success states handled as applicable.
- [ ] Persistence and lifecycle behavior verified.
- [ ] Accessibility checked.
- [ ] Light and dark themes checked.
- [ ] Adjacent flows regression-tested.
- [ ] Automated tests pass.
- [ ] Documentation updated.

## Build and test
Use the checked-in Gradle wrapper if present. README documents :app:assembleDebug, :app:testDebugUnitTest and :app:lintDebug. Verify wrapper and task names; record actual results, never infer success from compilation alone.

## Privacy and data
- [ ] Local storage claims verified against implementation.
- [ ] Backup/export and restore tested.
- [ ] Sensitive data not unintentionally exposed in logs/notifications.
- [ ] Permissions requested in context.
- [ ] Connected-source provenance shown.
- [ ] Disconnect and deletion behavior clear.
- [ ] No unapproved tracking or analytics.

## F-Droid readiness
- [ ] FLOSS license declared and compatible.
- [ ] All dependencies/assets have compatible licenses.
- [ ] No proprietary tracking/ads SDK in F-Droid build.
- [ ] Google Play Services, Firebase, Crashlytics and proprietary dependencies absent from F-Droid flavor or replaced with compliant alternatives.
- [ ] Source build uses accepted public toolchains.
- [ ] No embedded API keys or secrets.
- [ ] No unapproved runtime executable downloads.
- [ ] Stable unique application ID.
- [ ] Reproducible build requirements reviewed.
- [ ] Functional, useful and maintained app.
- [ ] Metadata, screenshots, icon and changelog ready.

Policy: https://f-droid.org/en/docs/Inclusion_Policy/
Reproducible builds: https://f-droid.org/en/docs/Reproducible_Builds/

## Release path
Internal device testing → public GitHub pre-release/APK → stable GitHub release → F-Droid submission after policy and reproducibility review.
