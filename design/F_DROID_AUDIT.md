# F-Droid Readiness Audit — Initial Findings

Date: 2026-11-05
Status: preliminary source audit; no F-Droid build or metadata validation was run.

## Blockers / decisions

### 1. License file
A root Apache-2.0 LICENSE has been added on release-prep PR #10; merge and CI validation are still pending. Verify all bundled assets and dependencies and add any required NOTICE material before distribution.

### 2. Application ID
The approved public applicationId is `com.jayasuriya.rhythm` (proposed on PR #11; merge and CI validation pending). This replaces the AI Studio-generated `com.aistudio.rhythmfocus.mnyrqp`. Changing it after publication creates a different Android app and cannot update existing installs.

### 3. Google/Firebase dependencies
app/build.gradle.kts applies Google Services and Secrets plugins and includes Firebase AI, Firebase App Check Recaptcha and debug dependencies. Audit the resolved dependency graph and licenses. A dedicated F-Droid flavor may be required to exclude Google/Firebase-specific components. Do not infer compatibility from the top-level artifact alone.

### 4. AI service and privacy
metadata.json declares a server-side Gemini API capability and .env.example documents GEMINI_API_KEY. Any feature sending user content off-device needs explicit opt-in, payload disclosure, privacy documentation and offline fallback where feasible. The complete call path is not verified.

### 5. Cloud sync
The original manual sync path falsely reported success without transfer. Current feature branch changes it to fail closed and report that cloud sync is not implemented. Keep it disabled until genuine authenticated, auditable sync exists.

### 6. Permissions
Manifest declares exact alarms, full-screen intent, overlay, foreground services, wake lock, camera and Health Connect read permissions. Document each user-facing purpose, request in context and remove unused permissions. Review background behavior and F-Droid policy.

### 7. Signing and secrets
Release signing uses environment-provided secrets with a fallback keystore path. Ensure no private keystore, API key or service configuration is committed. Verify ignore rules and CI secret handling.

### 8. Reproducible build tooling
The inspected tree contains gradle-wrapper.properties but no gradlew, gradlew.bat or gradle-wrapper.jar. Add and verify the standard wrapper or document a reproducible accepted build process. Do not assume wrapper commands work.

## Additional checklist
- [ ] Add full LICENSE and required notices.
- [ ] Choose stable applicationId before public release.
- [ ] Audit all resolved dependencies and licenses.
- [ ] Add F-Droid flavor excluding incompatible dependencies/features if needed.
- [ ] Review every manifest permission and network path.
- [ ] Document Gemini data flow and explicit opt-in.
- [ ] Keep cloud sync disabled until real implementation.
- [ ] Add F-Droid metadata: package ID, categories, summary, description, license, source, issue tracker and changelog.
- [ ] Verify reproducible build and source-build instructions.
- [ ] Verify clean install, upgrade and data preservation.
- [ ] Verify offline functionality and operation without Google Play Services where claimed.

References:
- https://f-droid.org/en/docs/Inclusion_Policy/
- https://f-droid.org/en/docs/Build_Metadata_Reference/
- https://f-droid.org/en/docs/Reproducible_Builds/
