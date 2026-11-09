# Rhythm — Product & Design Principles

**Document Version:** 1.0  
**Status:** Canonical Principles for Product, Design, Engineering & QA  
**Package:** `com.jayasuriya.rhythm`  
**License:** Apache License 2.0  

---

## 1. Overview & Core Philosophy

Rhythm is built on the belief that personal technology should bring calm, clarity, and consistency to daily life—not anxiety, pressure, or sensory overload. 

Every design decision, interaction pattern, and engineering implementation must strictly adhere to these product principles.

---

## 2. User Experience & Visual Design Principles

### 2.1 Calm, Low-Glare Aesthetics
- **Eye-Friendly Atmosphere:** Primary default canvas is true OLED Dark (`#000000`) to minimize ocular strain, battery consumption, and nighttime stimulation.
- **First-Class Theme Support:** Standard Dark (deep teal-charcoal `#123C3A`/`#102B2C`), Warm Light (`#F5F8F6`), and System Following are first-class, fully tested experiences—never afterthoughts.
- **Restrained Color Palette:** Grounded in nature: deep teal, muted mint (`#9ADCC6`), gentle seafoam (`#49BBA8`), warm champagne (`#FFE6B9`), and muted violet (`#BDA6DC`).
- **No Visual Noise:** Strictly avoid neon emeralds, saturated rainbow gradients, glossy glassmorphism, or heavy drop shadows.

### 2.2 Clear Information Hierarchy & Progressive Disclosure
- **One Primary Action:** Every screen, dialog, and card must present one obvious primary action.
- **Progressive Disclosure:** Essential information is visible immediately; secondary details, advanced filters, and historical raw data are accessible via deliberate user action.
- **Tonal Layering Over Cards:** Differentiate content sections using subtle tonal depth and spacing rather than wrapping every element in a card with heavy borders.
- **No Card-in-Card Clutter:** Strictly prohibit nesting cards inside cards.

### 2.3 Minimal Cognitive Load & Short Workflows
- **Respect User Time:** Frequent daily actions (logging a routine anchor, confirming a medication dose, starting a study session, or recording a mood check-in) must require **≤ 2 taps**.
- **Context Preservation:** Users must be able to park an intrusive thought or take a 2-minute breathing break without losing their active timer state or place in the app.
- **Sensible Defaults:** Provide well-calibrated, sensible defaults for study intervals, alarm volumes, and reminder schedules to prevent decision fatigue.

### 2.4 Respect Variable Daily Capacity (Zero Guilt)
- **Life Fluctuates:** A user's energy, health, focus, and available time vary day to day. Rhythm must never treat a lower-activity day as a personal failing.
- **No Streaks or Punitive Resets:** Strictly no streak counters that reset to zero with guilt-inducing copy or broken-chain animations.
- **Optional Everything:** Every check-in, anchor, and meditation session is optional. Users can skip, dismiss, or snooze without judgment.
- **Supportive Tone:** Copy is calm, empathetic, and neutral. Never use scolding ("You missed your target!"), shame-based encouragement ("Don't break your streak!"), or toxic positivity.

---

## 3. Data Integrity & Meaningful Tracking Principles

### 3.1 Neutral Observational Language
- **Observations, Not Diagnoses:** Present data as clear, objective observations (e.g., *"Sleep was 1 hour shorter than your usual range"*), never as clinical evaluations (e.g., *"You are showing signs of insomnia"*).
- **No Unsupported Causal Claims:** Never claim that Event A caused Mood B (e.g., *"Late coffee caused high anxiety"*). Show correlation and timelines neutrally; let the user and their healthcare provider draw clinical conclusions.
- **Honest Missing Data Handling:** Missing or unrecorded days are explicitly labeled as *"No entry recorded"* or `"--"`. **Missing data is NEVER treated as zero** in averages, calculations, or charts.

### 3.2 Purposeful Visualizations
- **Charts Must Answer Questions:** Only include a chart if it directly answers a practical question for the user (e.g., *"Did my bedtime drift later this week?"*).
- **Accessible Chart Ergonomics:** Every visualization must provide legible units, clear date boundaries, accessible textual summaries, and high-contrast color distinction for colorblind users.

---

## 4. Privacy, Security & Ethics Principles

### 4.1 100% Offline-First by Default
- **Local Storage Primacy:** All personal wellness, medication, thought notes, and routine data are stored exclusively in the local Room SQLite database on the user's physical device.
- **Zero Third-Party Trackers:** No Google Analytics, Firebase Analytics, Crashlytics, Mixpanel, or advertisement SDKs are present.
- **No Silent Uploads:** No user records are transmitted across the network. If cloud sync or external integrations are ever introduced, they must be 100% opt-in with explicit destination and payload disclosure.

### 4.2 Sensitive Data Protection at Rest
- **SQLCipher Encryption:** The local SQLite database is encrypted at rest using AES-256 via SQLCipher, with key derivation protected by the hardware-backed Android Keystore.
- **Encrypted User Backups:** Exported JSON backups support user-passphrase protection using AES-256-GCM authenticated encryption (PBKDF2-HMAC-SHA256, 100,000 iterations).

### 4.3 Respectful Android Permissions
- **Contextual Request Flow:** Permissions (Camera, Exact Alarm, Notifications, Health Connect, System Alert Window) are requested strictly in-context with transparent rationales.
- **Zero Data Harvesting:** The camera is used solely as a local live viewfinder for QR/barcode alarm dismissal missions; frames are never stored, analyzed for tracking, or transmitted.

---

## 5. Engineering, Reliability & Quality Principles

### 5.1 Architecture & Persistence Protection
- **Preserve Existing Architecture:** Re-use established repository patterns, domain ViewModels, and Room DAOs. Do not introduce competing architecture layers or duplicate ViewModels.
- **Preserve User Data & Schemas:** Never drop tables or execute destructive migrations. Maintain Room schema export (`exportSchema = true`) and verify all migrations with automated tests.
- **Enforce Transaction Safety:** Multi-step database mutations must be wrapped in `database.withTransaction` to guarantee ACID atomicity.

### 5.2 Background Reliability & System Integration
- **Dependable Scheduling:** Use `AlarmManager.setAlarmClock` with `RTC_WAKEUP` for time-critical alarms and medication reminders.
- **Lifecycle Resilience:** Timers, alarms, and background audio must survive process recreation, screen locks, and system time/timezone shifts.
- **Modern Power Management:** Replace deprecated `FULL_WAKE_LOCK` with `PARTIAL_WAKE_LOCK` strictly for background audio and `FLAG_KEEP_SCREEN_ON` for active lockscreen activities.

### 5.3 Non-Negotiable Quality Gates
Every code change must pass all three CI-equivalent quality gates before being committed or merged:
1. `:app:assembleDebug` — Clean compilation and packaging.
2. `:app:lintDebug` — Android lint with 0 fatal errors.
3. `:app:testDebugUnitTest -ProbolectricOffline=false` — Complete JVM and Robolectric test suite execution.

*Core Rule:* Never hide failures by disabling lint rules, skipping tests, or weakening assertions. Never report an unrun check as passed.

---

## 6. What to Strictly Avoid (Anti-Patterns)

| Anti-Pattern | Why It Is Prohibited in Rhythm |
|---|---|
| **Competitive Streaks & Gamification** | Causes anxiety, guilt, and abandonment when a streak is broken due to illness or fatigue. |
| **Medical / Clinical Claims** | Dangerous and unethical; Rhythm is a self-tracking companion, not a medical device. |
| **Card-in-Card Nesting** | Destroys visual hierarchy, wastes horizontal screen real estate, and increases visual clutter. |
| **Screen-Local Hex Colors** | Breaks multi-theme support (OLED, Dark, Light) and fragments brand identity. |
| **Silent Exception Swallowing** | Hides underlying failures and causes difficult-to-diagnose runtime bugs. |
| **Unrequested Notifications** | Violates user peace of mind; notifications must be user-configured and strictly essential. |
| **Treating Missing Data as Zero** | Distorts statistical averages and misleads the user regarding their actual patterns. |
| **Bloated Dependencies** | Increases APK size, introduces security/licensing risks, and threatens F-Droid inclusion. |
