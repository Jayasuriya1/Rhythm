# F-Droid Readiness Audit — Initial Findings

Date: 2026-11-05
Status: source-level preliminary audit. No F-Droid build or metadata validation was run.

## Blockers / decisions required

### 1. License file
README says Apache 2.0, but the inspected repository tree has no root LICENSE file. Add the full license text and any required NOTICE/attribution material before submission. Confirm all bundled assets and dependencies are compatible.

### 2. Application ID
Current applicationId: com.aistudio.rhythmfocus.mnyrqp. This appears AI Studio generated. Choose a stable public package ID before first public release. Changing it after users install will be a different Android app and cannot update the existing installation.

### 3. Google/Firebase dependencies and plugins
app/build.gradle.kts applies Google Services and Secrets plugins and includes Firebase AI, Firebase App Check Recaptcha and debug dependencies. The version catalog includes Firebase BOM and related libraries. Audit the complete resolved dependency graph and licenses. A separate F-Droid build flavor may be needed to exclude Google/Firebase-specific components and AI features. Do not assume a library is acceptable based on its wrapper alone.

### 4. AI service and privacy
metadata.json identifies a server-side Gemini API capability. .env.example documents GEMINI_API_KEY. Any AI feature that sends user text or other data off-device needs explicit opt-in, clear payload disclosure, privacy documentation and a no-AI offline fallback. Do not claim all personal data never leaves the device unless this is verified.

### 5. Cloud sync is not implemented
The inspected CloudSyncManager.triggerManualSync() returns SYNC_SUCCESS when Google Services is configured without performing data transfer or authentication. This must be corrected before any sync claim. For an offline-first F-Droid release, disable or omit this feature until genuine open and auditable sync is available.

### 6. Permissions
Manifest declares exact alarms, full-screen intent, overlay, foreground services, wake lock, camera and Health Connect read permissions. Document the user-facing purpose of each, request in context, and remove unused permissions. F-Droid review may scrutinize permissions and background behavior.

### 7. Signing and secrets
Release signing uses environment-provided secrets with a fallback keystore path; ensure no private keystore, API key or service config is committed. Check ignore rules and CI secrets. Never publish developer signing keys.

## Additional release checks
- [ ] Confirm Apache-2.0 license and add LICENSE file.
- [ ] Choose stable applicationId before first release.
- [ ] Produce an F-Droid-specific variant without incompatible dependencies.
- [ ] Inspect full resolved dependency tree and licenses.
- [ ] Verify no trackers/analytics or undisclosed network traffic.
- [ ] Document Gemini data flow and opt-in.
- [ ] Disable fake cloud-sync status.
- [ ] Review every manifest permission and background service.
- [ ] Add F-Droid metadata (application ID, categories, summary, description, license, source, issue tracker, changelog).
- [ ] Verify reproducible build and source-build instructions.
- [ ] Verify versionCode/versionName strategy and release signing.
- [ ] Test clean install, upgrade and data preservation.
- [ ] Verify app works without Google Play Services where claimed.

References:
- https://f-droid.org/en/docs/Inclusion_Policy/
- https://f-droid.org/en/docs/Build_Metadata_Reference/
- https://f-droid.org/en/docs/Reproducible_Builds/
