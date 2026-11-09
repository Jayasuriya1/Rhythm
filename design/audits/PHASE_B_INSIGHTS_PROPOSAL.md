# Phase B: Insights & Visualizations Proposal

**Proposal Date:** November 9, 2026  
**Audited Target:** `com.jayasuriya.rhythm` (`InsightsScreen.kt`)  
**Status:** Architecture & UI Proposal for Phase B Stage 2  
**Authority:** Principal Android Engineer, UX Designer & Accessibility Specialist

---

## 1. Vision & Information Architecture

The redesigned Insights experience transforms Rhythm from a raw history log into a **calm, reflective mirror of personal daily rhythm**. It answers six core questions for the user:

1. **Sleep & Circadian Timing:** *Did I go to bed and wake up at consistent times this week?*
2. **Social Rhythm Metric (SRM):** *How stable were my daily routine anchors (meals, contacts, start of day)?*
3. **Medication Schedule Reflection:** *Did I take my scheduled doses on time, and were any missed?*
4. **Mood & Energy Patterns:** *How did my wellness check-ins trend over the past 7 and 30 days?*
5. **Focus & Learning Investment:** *How many deep work hours did I log across DSA, Frontend, and Backend?*
6. **Wearables & Vitals (Optional):** *What were my resting heart rate and sleep stage distributions?*

### Screen Structure & Hierarchy

```
┌────────────────────────────────────────────────────────┐
│ Insights Header: "Observational Trends"                │
│ Period Selector: [ Past 7 Days ] [ Past 30 Days ]     │
├────────────────────────────────────────────────────────┤
│ 1. Weekly Overview Card (Recorded Days vs Unrecorded)  │
├────────────────────────────────────────────────────────┤
│ 2. Sleep Timing & Consistency Card                     │
│    - Bar chart with bedtime/waketime deviation         │
│    - Hypnogram stage breakdown (if Health Connect on)  │
├────────────────────────────────────────────────────────┤
│ 3. Social Rhythm Metric (SRM-5) Regularity Card        │
│    - Anchor hit rate (±45 min window)                  │
│    - 5-anchor distribution (Out of bed, Meals, etc.)   │
├────────────────────────────────────────────────────────┤
│ 4. Medication Adherence & Schedule Card                │
│    - On-time vs delayed vs skipped dose summary        │
├────────────────────────────────────────────────────────┤
│ 5. Mood & Energy Trend Card                            │
│    - 7-day dual sparkline / dot matrix                 │
│    - Neutral correlation observation                   │
├────────────────────────────────────────────────────────┤
│ 6. Focus & Learning Deep Work Card                     │
│    - Total focus hours & category breakdown            │
├────────────────────────────────────────────────────────┤
│ 7. Personal & Clinical Summary Export Card             │
│    - Action: Export Structured Summary (.txt / .json)  │
└────────────────────────────────────────────────────────┘
```

---

## 2. Detailed Card & Visualization Specifications

### 2.1 Sleep Timing & Consistency Card
- **Primary Metrics:**
  - Average Duration: e.g. `7h 24m` (or `"--"` if no entries).
  - Bedtime Consistency: `±28m variation` (Standard deviation).
  - Wake Time Consistency: `±19m variation`.
- **Chart Component (`AccessibleSleepBarChart`):**
  - Horizontal axis: 7 days (`Mon` through `Sun`).
  - Vertical bars representing duration with target guideline (e.g. 8h baseline).
  - **Missing Data Treatment:** Days without entries show a subtle dashed outline with label `"—"` and TalkBack description *"Tuesday: Not recorded"*.
  - **Accessibility:** Full TalkBack traversal; each bar is an interactive 48dp element announcing *"Wednesday, Nov 5: 7 hours 45 minutes of sleep. Bedtime 11:15 PM, Wake time 7:00 AM."*

### 2.2 Social Rhythm Metric (SRM-5) Regularity Card
- **Primary Metrics:**
  - Weekly Regularity Score: e.g. `24 / 30 routine anchors within 45m of target`.
  - Regularity Percentage: `80%`.
  - Missing count: `5 anchors unrecorded`.
- **Visualization:**
  - 5-row anchor status grid showing the 5 circadian anchors:
    1. Getting out of bed
    2. First social interaction
    3. Main activity kickoff
    4. Evening dinner
    5. Retiring into bed
  - Mini pill indicators for each day:
    - **Green pill (Theme primary):** On time (within ±45m).
    - **Amber pill (Theme tertiary):** Varied (>45m delta).
    - **Muted circle (Outline):** Unrecorded.
- **Observational Copy:**
  - *"Your dinner anchor was the most consistent this week (varying by an average of 14 minutes)."*

### 2.3 Medication Adherence Card
- **Primary Metrics:**
  - Doses Taken: e.g. `13 / 14 scheduled doses recorded as taken`.
  - On-Time Doses: `12 on time (within 1 hour)`.
  - Delayed Doses: `1 delayed`.
  - Skipped / Unrecorded: `1 unrecorded`.
- **Visualization:**
  - Stacked horizontal progress bar:
    - Primary color: Taken on time.
    - Tertiary color: Taken delayed.
    - Outline surface: Unrecorded / Skipped.
- **Copy:**
  - *"Prescription schedules are for your personal routine. Missing records are kept as unrecorded intervals rather than failed streaks."*

### 2.4 Mood & Energy Trajectory Card
- **Primary Metrics:**
  - 7-Day Average Mood: `3.8 / 5`
  - 7-Day Average Energy: `3.4 / 5`
  - Trajectory: *"Steady"* or *"Trending higher compared to last week"*.
- **Visualization (`AccessibleMoodSparkline`):**
  - Dot matrix line plot with points for recorded days.
  - Discrete levels (1 to 5) with semantic color coding (Calm primary for mood, warm tertiary for energy).
  - Unrecorded days show connected dashed lines without point dots, preventing the illusion of zero drops.
- **Observational Correlation:**
  - *"On days with over 7 hours of sleep, your reported energy averaged 4.0/5."* (Computed strictly if $\ge 4$ data points exist).

### 2.5 Focus & Learning Habit Card
- **Primary Metrics:**
  - Total Focus Time: e.g. `14h 30m logged this week`.
  - Daily Average (active study days): `2h 54m / study day`.
  - Completed Sessions Count: `18 sessions`.
- **Visualization:**
  - Segmented category distribution bar (e.g. 55% DSA, 30% Frontend, 15% Backend).
  - Category legend with exact hour tallies.

---

## 3. Personal & Clinical Summary Export Specification

The mock `PlannedExportCard()` is replaced by a fully functional **Local Summary Export**:

### 3.1 Export Formats
1. **Clinical / Personal Summary (Markdown & Plain Text):**
   - Designed for easy reading by the user or sharing with a healthcare provider, therapist, or personal coach.
   - Includes:
     - Header: Rhythm Personal Rhythm Summary (Date range).
     - Sleep section: Average duration, bedtime/wake consistency, hypnogram averages.
     - Social Rhythm section: SRM score, anchor variance.
     - Medication section: Active prescriptions, scheduled vs taken counts, on-time percentage.
     - Wellness section: Mood, energy, stress, and irritability 7-day averages.
     - Focus section: Total deep work hours and category investment.
   - **Privacy Filter Toggle:** Users can choose whether or not to include private freeform notes (`symptomsNotes`, `disruptionNote`, `BrainDumpNotes`).
2. **Raw Structured Backup (JSON):**
   - Complete encrypted or plaintext JSON database backup via existing `DataBackupManager`.

### 3.2 System Architecture for Export
- **Storage Access Framework (SAF):**
  - Launched via `ActivityResultContracts.CreateDocument("text/plain")` or `("text/markdown")`.
  - Allows the user to choose the destination folder (Internal Storage, SD Card, Google Drive, or Downloads) without requiring broad storage permissions.
- **100% Local Generation:**
  - Generated synchronously/coroutine-bound via `StringBuilder` in memory, streamed directly into the destination `Uri` opened with `contentResolver.openOutputStream(uri)`.
  - Zero cloud dependencies, zero external network requests.
