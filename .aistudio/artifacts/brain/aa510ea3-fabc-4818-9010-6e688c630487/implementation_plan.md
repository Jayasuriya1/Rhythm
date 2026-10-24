# Rhythm & Focus — Native Android Implementation Plan

Rhythm & Focus is a native Android application designed to help users maintain daily consistency, log health and sleep markers, track study priorities, and build healthy habits in a calm, supportive, and non-judgmental environment.

***

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and design decisions have been confirmed and will guide the implementation:

- **Confirmed Visual Aesthetic**: **Calming Sage** — A restorative, nature-inspired palette utilizing soft sage greens, soothing slates, and warm cream surfaces with high-contrast text and gentle accent pills.
- **Confirmed Study & Focus System**: **Customizable Focus Timer & Stopwatch** — Accommodates flexible focus intervals (e.g., 25/5 min Pomodoro, 50/10 min deep work, or custom time targets) alongside a stopwatch mode, non-punitive break intervals, and an "I'm stuck" small-step decomposer.
- **Confirmed First-Launch Experience**: **Clean Empty States with Guided Quick-Logs** — No fake/fabricated metrics or forced demo records. Clean zero-states provide friendly, immediate one-tap actions to log an entry.
- **Confirmed Milestone Scope**: **Milestone 1 Core Foundation** — Native Jetpack Compose + Material 3 UI, adaptive layout (Compact bottom bar / Expanded navigation rail + 2-column landscape/tablet dashboard for devices like Xiaomi Pad 7), and complete Room local database persistence. Advanced reminders, cloud Firestore sync, and Health Connect will follow in subsequent phases as requested.

***

## 1. Overview & Core Concept

- **What It Does**: Rhythm & Focus provides a unified, offline-first personal dashboard with 5 primary modules:
  1. **Today**: Greeting, date, routine anchor progress, sleep recap, mood & energy quick check-in, medication schedule overview, water tracker, and active study priority.
  2. **Routine**: Social rhythm anchors (Wake up, First social contact, Main activity, Dinner, Bedtime) tracking target vs. actual times without punitive streaks.
  3. **Health**: Distinct tracking for Mood, Energy, Stress, Irritability, Sleep quality & duration, Hydration quick logging, and clinician-friendly symptom notes.
  4. **Study**: Priority task list categorized by topic (DSA, JS/React, Backend, Interviews), customizable focus timer, study history, and "I'm stuck" step helper.
  5. **Insights**: Visual trends for sleep duration, routine anchor variance, mood/energy over time, hydration totals, and study hours with clear missing-data indicators.
  6. **Settings**: Theme controls, notification permission guide, data backup status, and clean database management.
- **Target Audience / Persona**: Individuals seeking sustainable daily routines, students or developers preparing for interviews, and anyone wanting a low-friction, respectful health diary without gamified guilt or cognitive overload.
- **Key Value**: Low friction, complete privacy, 100% offline functionality, and clean design that respects the user's emotional state.

***

## 2. User Experience & Visual Design

### Key User Flows

1. **Morning Quick Check-In**:
   - User opens the app → Sees personalized greeting and Today's summary.
   - Taps "Log Sleep" to record last night's bedtime, wake time, and sleep quality (1–5 rating).
   - Taps "Quick Mood & Energy" sliders/chips to record morning state in under 10 seconds.
2. **Routine Anchor Tracking**:
   - Under the Routine tab, 5 default anchors display target times (e.g., Wake-up 07:00, First Contact 09:00, Main Activity 10:00, Dinner 19:30, Bed 23:00).
   - User can mark an anchor as **Completed Now** (auto-capturing actual time and calculating delta), **Log Custom Time**, or **Skip**.
3. **Medication Management**:
   - User configures medication name, dose, unit, instructions, and target times.
   - On Today dashboard or Health tab, user marks intake as Taken, Skipped, Delayed, or Missed with timestamp verification.
4. **Focused Study Session**:
   - User picks or creates Today's Main Priority task.
   - Launches Focus Timer (customizable 25/50 min or Stopwatch).
   - Upon timer completion, user confirms, edits, or discards the session log with optional notes on what was learned.
   - If overwhelmed, the user taps **"I'm Stuck"** to decompose the task into one concrete 5-minute starting step.
5. **Insights & Reflective Analysis**:
   - Inspects weekly routine variance and sleep patterns.
   - Missing days are cleanly marked as "Unrecorded" rather than negative scores.

### Visual Identity & Theme (Calming Sage)

- **Color Tokens**:
  - `Primary`: Sage Green (`#4D6B53` Light / `#88B291` Dark)
  - `Secondary`: Warm Slate (`#5A645E` Light / `#B2BEB5` Dark)
  - `Tertiary`: Soft Amber Sand (`#D4A373` / `#E9C46A`)
  - `Background & Surface`: Warm Cream / Linen (`#F8F9F6` Light) & Deep Forest Slate (`#161A17` Dark)
  - `Surface Variant`: Tinted sage container (`#E3ECE4` Light / `#242D26` Dark)
  - `Outline`: Gentle border slate (`#C4CCC5`)
- **Typography**: Clean, readable sans-serif typography using Material 3 `Typography` with comfortable line heights, explicit `sp` dimensions, and prominent headers for effortless readability.
- **Adaptive Layout**:
  - **Phone (Compact)**: Single-column scrollable views with bottom `NavigationBar` and floating action buttons.
  - **Tablet & Landscape (Xiaomi Pad 7 / Expanded)**: Side `NavigationRail`, two-column dashboard layout on Today and Insights (e.g., Routine & Health on left pane, Study & Quick Log on right pane) preventing stretched layouts.

***

## 3. Key Product Decisions & Trade-Offs

- **Local Storage via Room**:
  - *Chosen Approach*: Room Database with SQLite tables for `RoutineAnchor`, `RoutineLog`, `HealthCheckin`, `SleepRecord`, `WaterEntry`, `MedicationItem`, `MedicationLog`, `StudyTask`, and `StudySession`.
  - *Why*: Guaranteed offline privacy, instant latency, zero network dependency, and strict adherence to the project specification where Room is the primary milestone storage.
- **Honest Data Architecture**:
  - *Chosen Approach*: Missing days are stored as non-existent records, not 0-values or failures. No artificial streaks, no guilt messaging.
  - *Why*: Research in behavioural and circadian rhythm tracking proves guilt streaks provoke app abandonment.
- **Clear Separation of Metrics**:
  - *Chosen Approach*: Mood, Energy, Stress, and Irritability remain independent scalar metrics rather than a single aggregated composite score.
  - *Why*: Allows users and clinicians to see distinct patterns (e.g., high energy with high irritability).

***

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Rhythm & Focus App UI                           │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐ │
│  │   Today   │ │  Routine  │ │  Health   │ │   Study   │ │ Insights  │ │
│  │ Dashboard │ │  Anchors  │ │ & Meds    │ │ & Focus   │ │ & Reports │ │
│  └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ │
└────────┼─────────────┼─────────────┼─────────────┼─────────────┼───────┘
         │             │             │             │             │
         ▼             ▼             ▼             ▼             ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     ViewModel & State Layer                            │
│  • TodayViewModel           • RoutineViewModel   • HealthViewModel     │
│  • StudyViewModel           • InsightsViewModel  • SettingsViewModel   │
│  State: StateFlow<UiState> reactive to Room Flow observers             │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Repository Layer                                │
│  • AppRepository (Encapsulates data operations & query transforms)     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    Room Database (SQLite Local)                        │
│  DAOs: RoutineDao, HealthDao, MedicationDao, StudyDao                  │
│  Entities:                                                             │
│   - RoutineLogEntity (date, anchorId, targetTime, actualTime, status)  │
│   - HealthLogEntity (date, mood, energy, stress, irritability, notes)   │
│   - SleepLogEntity (date, bedTime, wakeTime, durationMin, quality)     │
│   - WaterLogEntity (id, timestamp, volumeMl)                           │
│   - MedicationEntity & MedicationIntakeEntity                          │
│   - StudyTaskEntity (id, title, category, priority, status)            │
│   - StudySessionEntity (id, taskId, durationMinutes, type, notes)      │
└────────────────────────────────────────────────────────────────────────┘
```

### Milestone 1 Implementation Scope

1. **Application Foundation**:
   - Update `metadata.json` and `res/values/strings.xml` with "Rhythm & Focus".
   - Setup Calming Sage Material 3 color system in `Theme.kt` and `Color.kt`.
2. **Room Database Entities & DAOs**:
   - Complete type-safe schemas for Routine, Health, Sleep, Water, Medication, and Study.
   - Singleton `AppDatabase` with repository abstraction.
3. **Adaptive Shell & Navigation**:
   - Responsive Scaffold adapting between bottom navigation (compact) and navigation rail (expanded/tablet).
   - Seamless switching between Today, Routine, Health, Study, Insights, and Settings.
4. **Feature Screens**:
   - **Today Screen**: Summary cards, quick check-in bottom sheets, water counter, and active study task.
   - **Routine Screen**: 5 social rhythm anchors, target vs actual delta, status toggling (Done/Skip).
   - **Health Screen**: Sleep logger, Mood & Energy sliders, Medication manager with dose logging, and Hydration quick-logger.
   - **Study Screen**: Main priority selector, sub-task list by category, customizable focus timer with start/pause/reset/complete dialog, and "I'm stuck" step breakdown dialog.
   - **Insights Screen**: 7-day visual trend cards for sleep, routine adherence, mood/energy, and study hours with zero-data indicators.
   - **Settings Screen**: Theme selection, notification permissions info, database reset & export preview.
5. **Verification**:
   - Full Gradle compilation with `compile_applet`.
   - Verification of state persistence across configuration changes.
