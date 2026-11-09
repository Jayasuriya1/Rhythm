# Rhythm — Product Vision & North Star

**Document Version:** 2.0  
**Status:** Canonical Product Direction  
**Package:** `com.jayasuriya.rhythm`  
**License:** Apache License 2.0  

---

## 1. Executive Mission & Identity

Rhythm helps people understand their daily patterns and maintain more consistent routines through a calm, private, reliable, and easy-to-use Android companion.

Modern life frequently disrupts circadian stability, sleep patterns, and daily habits. Rhythm serves as a steady anchor—combining social rhythm stabilization, focus timers, gentle reminders, medication adherence, sleep tracking, and mindful reflections into a single, cohesive daily companion.

Rhythm is designed to be:
- **A Daily Companion:** Always ready to support the user at key moments of the day without clamoring for attention.
- **Calm & Grounding:** Low-glare, eye-friendly visual design with zero guilt-inducing streaks or high-pressure notifications.
- **Strictly Private:** Offline-first architecture where 100% of personal health and cognitive data resides exclusively on the user's device under SQLCipher AES-256 encryption.
- **Predictable & Dependable:** Alarms that ring on time, timers that survive backgrounding, and reminders that respect quiet hours.

---

## 2. Supported Capabilities

Rhythm provides integrated, non-intrusive support across ten core functional domains:

1. **Daily Routine & Social Rhythm Tracking:** Circadian anchor tracking (wake up, morning light, deep work, wind down, bedtime) rooted in the Social Rhythm Metric (SRM).
2. **Sleep & Wellness Observations:** Tracking self-reported sleep duration and quality, augmented with wearable hypnogram stages (Deep, Light, REM, Awake) via Health Connect.
3. **Mood, Energy & Clarity Check-Ins:** Low-friction, non-judgmental 1–5 scale check-ins to monitor daily emotional and mental bandwidth.
4. **Medication Schedule & Adherence:** Time-sensitive medication reminders, dose logging, and historical intake tracking ("TAKEN", "SKIPPED", "DELAYED").
5. **Study & Focused Activities:** Customizable interval timers, Pomodoro and custom study protocols, ambient procedural soundscapes, and session note-taking.
6. **Personal Reflection & Brain Dump:** Frictionless thought capture modal and floating overlay bubble to park intrusive thoughts without losing focus.
7. **Meditation & Mindfulness:** Interactive guided breathing visualizers (Box Breathing, 4-7-8, Deep Calm, Resonance) with tactile haptics.
8. **Custom Alarms & Exact Reminders:** Reliable wake-up alarms with volume crescendo and barcode/QR verification missions to prevent accidental snooze.
9. **Health Connect Data Integration:** Direct, permission-controlled wearable vitals import (steps, resting heart rate, HRV, active calories).
10. **Personal Historical Insights:** 7-day, weekly, and monthly observational trends that reveal patterns without pressure.

---

## 3. Explicit Non-Goals & Boundaries

To preserve trust, safety, and ethical boundaries, Rhythm is strictly defined by what it is **not**:

- **NOT a Diagnostic Tool:** Rhythm never diagnoses medical or psychiatric conditions (bipolar disorder, depression, insomnia, ADHD, etc.).
- **NOT a Clinical Replacement:** Rhythm does not replace a psychiatrist, physician, therapist, or healthcare professional.
- **NOT a Medication Recommendation Engine:** Rhythm tracks schedules set by the user or their doctor; it never prescribes, alters, or recommends medications or dosages.
- **NOT a Predictive Mood Engine:** Rhythm does not claim to predict manic or depressive episodes or guarantee psychological outcomes.
- **NOT a Competitive Productivity App:** Rhythm rejects gamification, public leaderboards, XP points, and social comparison.
- **NOT a Guilt-Inducing Streak System:** Rhythm never punishes missed days, resets streaks to zero with shaming copy, or treats incomplete tracking as failure.

---

## 4. Product Philosophy & Six Core Filters

Every existing feature, proposed change, and UI element must satisfy at least one of these six guiding questions:

1. **Does it help someone understand their daily patterns?**  
   Does it reveal trends in sleep, routine timing, or mood clearly and honestly?
2. **Does it help someone maintain a consistent routine?**  
   Does it support waking up, eating, working, and resting at steady circadian hours?
3. **Does it reduce friction in completing an important daily action?**  
   Can a user log a medication, start a focus timer, or check in with 1–2 taps?
4. **Does it make personal tracking easier?**  
   Does it respect user time, support progressive disclosure, and eliminate unnecessary form fields?
5. **Does it help users reflect on their recorded information?**  
   Does it present observational history neutrally without making unsupported claims?
6. **Does it improve reliability, safety, privacy, or accessibility?**  
   Does it safeguard user data, ensure alarms fire on time, and make the app usable for everyone?

*Rule:* If a proposed feature does not meaningfully satisfy this philosophy, it does not belong in Rhythm—regardless of how technically novel it may be.

---

## 5. The Four Core Daily Moments

Rather than an arbitrary collection of disconnected tools, the entire user experience of Rhythm is structured around four understandable moments in a person's day:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        RHYTHM DAILY LIFECYCLE                           │
├───────────────────┬───────────────────┬───────────────────┬─────────────┤
│   MORNING START   │ DAYTIME MAINTAIN  │  EVENING REFLECT  │   WEEKLY    │
│  Understand sleep │  Log routine      │  Reflect on day   │  Review SRM │
│  Check-in mood    │  Medication reminder│  Bedtime prep    │  Med trends │
│  Review anchors   │  Focus session    │  Wind down        │  Sleep consistency │
│  Take morning meds│  Park thoughts    │  Evening meds     │  Export report │
└───────────────────┴───────────────────┴───────────────────┴─────────────┘
```

### 5.1 Morning — Start the Day
Help the user:
- Understand their previous night's sleep duration and quality.
- Record an optional, 5-second mood and energy check-in.
- See today's scheduled circadian routine anchors (e.g. morning light, breakfast).
- See morning medication reminders clearly.
- Identify the single next helpful action without feeling overwhelmed.

### 5.2 Daytime — Maintain Rhythm
Help the user:
- Mark circadian anchors as they occur.
- Receive timely, respectful medication reminders.
- Launch focused study or work sessions with calming ambient audio.
- Capture distracting thoughts immediately into Brain Dump without leaving flow.
- Take short, guided mindfulness and breathing breaks.
- Review progress calmly without pressure or competition.

### 5.3 Evening — Reflect and Wind Down
Help the user:
- Record an evening mood, stress, and clarity check-in.
- Review the day's routine anchor completions gently.
- Log evening medication intake.
- Prepare for sleep with bedtime reminders and soothing breathing exercises.
- Transition into rest without stimulating visual glare or late-night notifications.

### 5.4 Weekly — Understand Patterns
Help the user:
- Review weekly sleep consistency and sleep debt patterns.
- Understand routine regularity through the Social Rhythm Metric.
- Observe mood and energy fluctuations alongside routine anchors.
- Review medication adherence for personal awareness or clinical discussions.
- Identify neutral observations worth discussing with a doctor or therapist.
- Export an encrypted or structured personal report if desired.

---

## 6. The Three Questions for the Today Dashboard

The Today screen is Rhythm's primary hearth. It must never attempt to display every feature simultaneously. Instead, it must cleanly answer three fundamental questions:

1. **How am I doing today, based on what I have recorded?**  
   Concise summary of sleep, current energy/mood, and completed anchors.
2. **What important activity or reminder is coming next?**  
   The immediate next circadian anchor, upcoming medication dose, or scheduled focus block.
3. **Is there anything useful I should record or review?**  
   A gentle prompt for an unrecorded anchor, water intake, or rapid thought capture.

---

## 7. Success & Quality Metrics

We measure Rhythm's success by **usefulness, dependability, and calm**, never by screen time or addictive engagement:

- **Frictionless Interaction:** Main workflows (check-in, medication log, timer start) completed in ≤ 2 taps.
- **Alarm Dependability:** Alarms fire at the exact target minute across all Android power states, timezones, and reboots.
- **Absolute Privacy:** 0 bytes of user data leaked or transmitted across the network; verified by automated CI privacy gates.
- **Zero-Pressure Philosophy:** 0 guilt-inducing alerts; missing days are acknowledged neutrally without penalty.
- **Universal Accessibility:** 100% WCAG 2.2 AA contrast compliance across OLED, Standard Dark, and Light themes.
