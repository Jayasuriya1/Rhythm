# F-Droid Release & FOSS Compliance Guide

**Target Package:** `com.jayasuriya.rhythm`  
**License:** Apache License 2.0  
**Build Tool:** Gradle (Wrapper / Standalone CLI)  
**Target SDK:** 36 (Android 15+)  
**Minimum SDK:** 26 (Android 8.0 Oreo)  

---

## 1. Compliance Checklist

| F-Droid Inclusion Requirement | Status | Verification Details |
|---|---|---|
| **Root Open-Source License** | ✅ Compliant | Root `LICENSE` contains full Apache License 2.0. |
| **Stable Package Identity** | ✅ Compliant | `applicationId = "com.jayasuriya.rhythm"` (migrated from AI Studio placeholder). |
| **Zero Proprietary Binary Blobs** | ✅ Compliant | Core app uses open-source libraries: AndroidX, Jetpack Compose, Kotlin Coroutines, Room, SQLCipher (Community Edition 4.20.0). |
| **No Google Play Services Hard Dependency** | ✅ Compliant | Build completes cleanly without `google-services.json`. The Google Services Gradle plugin logs a missing file warning and is non-blocking. |
| **Zero Third-Party Advertising / Tracking SDKs** | ✅ Compliant | No AdMob, Firebase Analytics, Crashlytics, or tracking libraries are integrated. |
| **Privacy & Anti-Features Transparency** | ✅ Compliant | No anti-features required. Health Connect integration uses standard Android 14+ platform APIs (`androidx.health.connect:connect-client`). |
| **Reproducible Build Steps** | ✅ Compliant | Standard Gradle build commands without proprietary signing keys required. |

---

## 2. Fastlane Metadata Structure

The repository includes standard Fastlane metadata in `fastlane/metadata/android/en-US/`:
- `title.txt`: `Rhythm & Focus`
- `short_description.txt`: `Calm circadian social rhythm, sleep, medication, and focus companion.`
- `full_description.txt`: Detailed product overview and privacy guarantees.

---

## 3. Reproducible Build Instructions for F-Droid

F-Droid build engines can build the release APK using:

```bash
# Clean project
gradle clean

# Build release APK without signing or with F-Droid key
gradle :app:assembleRelease --no-daemon --stacktrace
```

The resulting unsigned APK is located at:
`app/build/outputs/apk/release/app-release-unsigned.apk`

