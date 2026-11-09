# Phase A: Today Dashboard Modernization & Progressive Disclosure Proposal

**Audit & Design Date:** November 9, 2026  
**Target Screen:** `app/src/main/java/com/example/ui/screens/today/TodayScreen.kt`  
**Author:** Principal Android Architect, Product Designer & UX Specialist  
**Canonical Direction:** `design/PRODUCT_VISION.md`, `design/PRODUCT_PRINCIPLES.md`  

---

## 1. Executive Problem Statement

The current `TodayScreen.kt` (1,609 lines) acts as an indiscriminate aggregator rather than an intelligent, calm daily companion. 

### Current Flaws:
1. **The "Everything Everywhere" Anti-Pattern:** It simultaneously renders 7 massive, full-width cards:
   - Next Event Circular Arc Card
   - Main Study Priority Card
   - Daily Routine Anchors List Card
   - Brain Dump Thought Parking Card
   - Sleep Summary Card
   - Mood & Energy Check-in Card
   - Wearable Health Connect Summary Card
   - Floating Action Button Quick Menu
2. **Infinite Vertical Scroll:** Requires 4–5 full scroll swipes on a standard 6.3-inch phone display.
3. **The "Empty Dashboard Paradox":** When a user opens Rhythm in the morning, they are confronted with up to **five simultaneous empty state cards**, creating visual clutter, decision paralysis, and an unintended feeling of falling behind.
4. **Failure to Answer the Three Core Questions:**
   - *How am I doing today?* (Buried halfway down the screen).
   - *What is coming next?* (Only shows routine anchors; ignores scheduled medications and study blocks).
   - *Is there anything useful I should record?* (Scattered across 4 different cards).

---

## 2. Core Architectural Requirement: The Three Questions

Per `PRODUCT_VISION.md §6`, every design iteration of the Today screen must immediately and effortlessly answer three questions above the fold:

```
┌────────────────────────────────────────────────────────────────────────┐
│                   THE THREE TODAY DASHBOARD QUESTIONS                  │
├────────────────────────────────────────────────────────────────────────┤
│ 1. HOW AM I DOING TODAY?                                               │
│    (Sleep summary + current energy/mood state + completed anchors)    │
│                                                                        │
│ 2. WHAT IS COMING NEXT?                                                │
│    (The immediate next circadian anchor, upcoming medication, or timer)│
│                                                                        │
│ 3. WHAT IS USEFUL TO RECORD OR REVIEW?                                 │
│    (Gentle 1-tap prompts: log wake time, take morning meds, park idea)│
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Three Alternative Design Concepts

---

### Concept 1: The Adaptive "Four Daily Moments" Dashboard (RECOMMENDED)

#### Core Philosophy:
The dashboard adapts its primary visual emphasis based on the user's current moment of the day:
- **Morning (05:00 – 11:59):** Emphasizes Sleep Understanding + Mood/Energy Check-in + Morning Anchors + Morning Medication. Study and evening wind-down are collapsed into a clean summary pill.
- **Daytime (12:00 – 17:59):** Emphasizes Next Routine Anchor + Active Study/Task Priority + Midday Meds + Quick Thought Parking.
- **Evening (18:00 – 04:59):** Emphasizes Day Reflection + Evening Mood Check-in + Bedtime Wind-Down Anchor + Night Medication.

#### Wireframe & Visual Composition:

```
┌─────────────────────────────────────────────────────────┐
│ RhythmTopBar: Date Stepper, Today Jump, BrainDump, Gear │
├─────────────────────────────────────────────────────────┤
│ Greeting: "Good morning, Jayasuriya"                    │
│ "Monday, November 9"                                    │
│                                                         │
│ ┌─────────────────────────────────────────────────────┐ │ [Question 1:
│ │ 🌟 DAILY PULSE (Status Bar Summary)                 │ │  How am I
│ │ Sleep: 7h 20m (85%) • Mood: Neutral • Anchors: 1/5  │ │  doing?]
│ └─────────────────────────────────────────────────────┘ │
│                                                         │
│ ┌─────────────────────────────────────────────────────┐ │ [Question 2:
│ │ ⏳ NEXT IMMEDIATE ANCHOR (Circular Arc Focus)       │ │  What is
│ │ Getting out of bed in 24m (7:00 AM)                 │ │  coming next?]
│ │ [ Log Now (1-Tap) ]      [ Full Routine ]           │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                         │
│ ┌─────────────────────────────────────────────────────┐ │ [Question 3:
│ │ 💊 SCHEDULED MEDICATION (Actionable Prompt)         │ │  What is useful
│ │ Morning: Vitamin D3 (1000 IU) due at 8:00 AM        │ │  to record?]
│ │ [ Take Dose (1-Tap) ]    [ Snooze 15m ]             │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                         │
│ ┌─────────────────────────────────────────────────────┐ │ [Progressive
│ │ 🎯 TODAY'S FOCUS PRIORITY (Single Compact Card)     │ │  Disclosure:
│ │ Task: "Complete Dynamic Programming Practice"       │ │  Secondary
│ │ [ Start 25m Timer ]      [ Change Priority ]        │ │  actions]
│ └─────────────────────────────────────────────────────┘ │
│                                                         │
│ ▼ [ View All Daily Details & Health Telemetry ] (Expand)│
└─────────────────────────────────────────────────────────┘
```

#### Pros:
- **Zero Scroll for Core Actions:** Answers all 3 questions within the top 600dp viewport.
- **Maximum Context Relevance:** Irrelevant tasks (e.g., bedtime wind-down cards in the morning) do not crowd out morning actions.
- **Frictionless 1-Tap Interactions:** Users complete routine check-ins and medication confirmation right from the home screen without navigating to sub-screens.

#### Cons:
- Requires time-of-day awareness in `TodayScreen` composable logic.

---

### Concept 2: The Unified Circadian Timeline Agenda

#### Core Philosophy:
Merges routine anchors, medication doses, study blocks, and sleep intervals into a single chronologically sorted vertical timeline.

#### Wireframe:
```
┌─────────────────────────────────────────────────────────┐
│ Greeting & Date                                         │
│                                                         │
│ TIMELINE FOR TODAY:                                     │
│  07:00 AM  ● Wake Up Anchor  ──► [ Logged: 7:05 AM ✓ ]  │
│  07:30 AM  ○ Morning Light   ──► [ Log Now ]  [ Skip ]  │
│  08:00 AM  💊 Vitamin D3      ──► [ Take Dose ]         │
│  10:00 AM  🎯 DSA Study Block ──► [ Start 25m Timer ]   │
│  01:00 PM  ○ Lunch Anchor     ──► [ Scheduled ]         │
│  10:30 PM  ○ Bedtime Winddown ──► [ Scheduled ]         │
└─────────────────────────────────────────────────────────┘
```

#### Pros:
- Clear chronological sequence.
- Users see their whole day as one cohesive circadian flow.

#### Cons:
- Can become visually busy if the user has many medications or routine anchors.
- Less space for sleep hypnograms or health check-in widgets.

---

### Concept 3: Compact Metric Summary Pillars + Focus Action

#### Core Philosophy:
Replaces all large cards with 3 horizontal KPI summary cards (Sleep, Rhythm, Focus), followed by a single "Hero Next Action" container.

#### Wireframe:
```
┌─────────────────────────────────────────────────────────┐
│ Greeting & Date                                         │
│                                                         │
│ [ 😴 Sleep ]      [ ⏰ Rhythm ]      [ 🎯 Focus ]       │
│   7h 20m             2 / 5 Anchors      50m Logged      │
│   Good Quality       Next: 10:00 AM     1 Task Done     │
│                                                         │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ HERO NEXT ACTION:                                   │ │
│ │ Start Morning Study Block (25m Pomodoro)            │ │
│ │ [ Start Focus Session ]                             │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                         │
│ [ Park a Thought ]   [ Quick Mood Check-In (1–5) ]      │
└─────────────────────────────────────────────────────────┘
```

#### Pros:
- Extremely concise; almost zero vertical scrolling.
- High visual symmetry.

#### Cons:
- Medication schedules are compressed into an icon badge.
- Less space for detailed circadian guidance.

---

## 4. Concept Comparison & Recommendation

| Evaluative Dimension | Current Today Screen | Concept 1: Adaptive Moments | Concept 2: Unified Timeline | Concept 3: Compact Pillars |
|---|---|---|---|---|
| **Answers Question 1 (How am I doing?)** | Poor (Scattered) | **Exceptional (Daily Pulse)** | Fair | Good |
| **Answers Question 2 (What is next?)** | Fair (Only routine) | **Exceptional (Anchor + Meds)**| Exceptional | Good |
| **Answers Question 3 (What to record?)** | Poor (5 empty states) | **Exceptional (1-Tap prompts)**| Good | Fair |
| **Viewport Efficiency (< 600dp height)** | Poor (> 1500dp) | **Exceptional (≤ 580dp)** | Good (800dp) | Exceptional (450dp) |
| **Cognitive Load on Empty Days** | Severe (5 empty cards) | **Calm & Grounding (1 pulse)** | Moderate | Low |
| **Accessibility & Touch Targets** | Moderate (NAF alerts) | **100% Compliant (≥ 48dp)** | Good | Moderate |

### Official Recommendation: Concept 1 (Adaptive Four Daily Moments)
Concept 1 directly fulfills Rhythm’s North Star:
- Adapts to morning, daytime, and evening moments seamlessly.
- Eliminates card-in-card nesting.
- Delivers 1-tap logging for medications and routine anchors directly from the hearth.
- Progressively discloses deep telemetry and historical graphs without cluttering the daily view.
