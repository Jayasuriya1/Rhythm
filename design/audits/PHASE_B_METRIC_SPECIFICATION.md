# Phase B: Metric Calculation Specifications & Missing-Data Rules

**Specification Date:** November 9, 2026  
**Audited Target:** `com.jayasuriya.rhythm`  
**Status:** Approved Architectural Specification  
**Authority:** Principal Android Engineer & Product Architect

---

## 1. Principles of Honest Metric Calculation

Rhythm is a personal tracking companion designed to reflect actual habits without distortion, guilt, or clinical claims. Every metric computed by the application must adhere to three foundational rules:

1. **Strict Missing Data Separation:** An unrecorded day is **unknown**, never zero. Averaging an unrecorded sleep day as 0 hours distorts a true 8-hour average into 4 hours, generating unwarranted user anxiety. Missing days are excluded from denominators in averages and rendered visually as `"--"` or empty indicators.
2. **Observational, Non-Judgmental Language:** Rhythm never diagnoses, prescribes, or claims causality. The app presents observations ("Your bedtime varied by 28 minutes this week") rather than value judgments or medical conclusions ("You have insomnia" or "Irregular routine caused your low mood").
3. **Deterministic Local Time Handling:** All daily metrics are bucketed by local civil date (`YYYY-MM-DD`). Timezone and Daylight Saving Time (DST) transitions must not distort duration calculations or drop sessions.

---

## 2. Metric-by-Metric Mathematical Specifications

### 2.1 Sleep Duration & Consistency

#### A. Duration Calculation
For a single sleep session recorded for wake date $D$ with bedtime $T_{\text{bed}}$ and wake time $T_{\text{wake}}$ (minutes from 00:00, $0 \le T < 1440$):
$$\text{Duration}(T_{\text{bed}}, T_{\text{wake}}) = \begin{cases} 
T_{\text{wake}} - T_{\text{bed}} & \text{if } T_{\text{wake}} \ge T_{\text{bed}} \\ 
(1440 - T_{\text{bed}}) + T_{\text{wake}} & \text{if } T_{\text{wake}} < T_{\text{bed}} 
\end{cases}$$
- **Units:** Minutes (formatted for display as `Xh Ym`).

#### B. Rolling Average Sleep Duration (7-Day / 30-Day)
For a date interval $W$ containing $N_{\text{recorded}}$ valid entries ($N_{\text{recorded}} \le |W|$):
$$\text{AvgDuration}(W) = \begin{cases} 
\frac{1}{N_{\text{recorded}}} \sum_{d \in W, \text{hasEntry}(d)} \text{Duration}_d & \text{if } N_{\text{recorded}} > 0 \\ 
\text{null} & \text{if } N_{\text{recorded}} = 0 
\end{cases}$$
- If $N_{\text{recorded}} = 0$, display `"--"`. Never substitute 0.

#### C. Bedtime & Wake Time Consistency (Standard Deviation)
Circadian research indicates that timing regularity is as important as total duration.
1. **Bedtime Normalization:** Bedtimes spanning midnight are normalized around 00:00:
   $$T'_{\text{bed}} = \begin{cases} 
   T_{\text{bed}} - 1440 & \text{if } T_{\text{bed}} \ge 720 \text{ (i.e. 12:00 PM to 11:59 PM)} \\ 
   T_{\text{bed}} & \text{if } T_{\text{bed}} < 720 \text{ (i.e. 12:00 AM to 11:59 AM)} 
   \end{cases}$$
2. **Mean Normalized Bedtime:**
   $$\bar{T}'_{\text{bed}} = \frac{1}{N} \sum_{i=1}^N T'_{\text{bed}, i}$$
3. **Sample Standard Deviation ($s_{\text{bed}}$):**
   $$s_{\text{bed}} = \sqrt{\frac{1}{N-1} \sum_{i=1}^N (T'_{\text{bed}, i} - \bar{T}'_{\text{bed}})^2} \quad (\text{defined for } N \ge 2)$$
- **Consistency Classification (Neutral):**
  - $s_{\text{bed}} \le 30\text{ min}$: *"Consistent timing (±[s]m)"*
  - $30\text{ min} < s_{\text{bed}} \le 60\text{ min}$: *"Moderate variation (±[s]m)"*
  - $s_{\text{bed}} > 60\text{ min}$: *"Variable timing (±[s]m)"*
  - $N < 2$: *"Need $\ge$ 2 entries to observe consistency"*

---

### 2.2 Social Rhythm Metric (SRM-5) Regularity

The Social Rhythm Metric evaluates routine stability across five circadian anchors:
1. `out_of_bed`: Getting out of bed
2. `first_contact`: First social interaction of the day
3. `main_activity`: Commencing primary work/study
4. `dinner`: Evening meal timing
5. `getting_into_bed`: Retiring into bed

#### A. Anchor Regularity Condition ("Hit" Window)
For an anchor with user target time $T_{\text{target}}$ and logged actual time $T_{\text{actual}}$:
$$\text{IsRegular}(T_{\text{actual}}, T_{\text{target}}) = \begin{cases} 
\text{true} & \text{if } |T_{\text{actual}} - T_{\text{target}}| \le 45\text{ minutes} \\ 
\text{false} & \text{otherwise} 
\end{cases}$$

#### B. Daily Routine Regularity Index ($R_{\text{day}}$)
For date $D$ with $E$ enabled anchors:
$$R_{\text{day}} = \frac{\sum_{a \in E} \mathbb{I}(\text{IsRegular}(a))}{|E|}$$
- Missing or skipped anchors count as non-regular for that day's scheduled targets, but are explicitly reported: e.g. *"3 of 5 anchors within 45m of target. 1 skipped, 1 unrecorded."*

#### C. Weekly SRM Score ($SRM_{\text{week}}$)
Across a 7-day window with total recorded anchor entries $M_{\text{recorded}}$ and expected anchor events $M_{\text{expected}} = 7 \times |E|$:
$$\text{Weekly Regularity Ratio} = \frac{\text{Total Regular Anchor Events}}{\text{Total Recorded Anchor Events}}$$
- **Observational Presentation:**
  - Display exact counts: *"21 of 28 recorded routine events occurred within 45 minutes of target."*
  - Missing entries noted neutrally: *"7 routine events unrecorded this week."*
  - Zero guilt, zero streak language.

---

### 2.3 Mood, Energy & Wellness Check-Ins

Metrics are captured on 1–5 discrete scales:
- Mood (1: Very Low to 5: Great)
- Energy (1: Exhausted to 5: High)
- Stress (1: Calm to 5: High)
- Irritability (1: None to 5: Severe)
- Activation Energy (1: Low to 5: Elevated)

#### A. Rolling Averages
For metric $X \in \{\text{mood}, \text{energy}, \text{stress}, \text{irritability}, \text{activation}\}$:
$$\bar{X} = \begin{cases} 
\frac{1}{N_{X}} \sum_{d \in W, X_d \ne \text{null}} X_d & \text{if } N_{X} > 0 \\ 
\text{null} & \text{if } N_{X} = 0 
\end{cases}$$

#### B. Trend Trajectory (Linear Slope $\beta$)
Computed via least-squares regression over days $t = 1 \dots K$ ($K \ge 3$ entries):
$$\beta = \frac{\sum_{i=1}^K (t_i - \bar{t})(X_i - \bar{X})}{\sum_{i=1}^K (t_i - \bar{t})^2}$$
- **Trend Labels:**
  - $\beta > +0.15$: *"Trending higher"*
  - $\beta < -0.15$: *"Trending lower"*
  - $-0.15 \le \beta \le +0.15$: *"Steady"*

---

### 2.4 Medication Adherence

#### A. Scheduled Dose Count ($S_{\text{total}}$)
For each active medication $m$ across date window $W$:
$$S_{\text{total}}(W) = \sum_{d \in W} \sum_{m \in \text{Medications}} \mathbb{I}(m.\text{isScheduledForDate}(d)) \times |\text{scheduledTimes}(m)|$$

#### B. Intake Classifications
Each scheduled dose on date $d$ resolves to one of:
1. `TAKEN_ON_TIME`: Actual action time within $\pm 60$ minutes of scheduled time.
2. `TAKEN_DELAYED`: Actual action time $> 60$ minutes after scheduled time.
3. `SKIPPED`: User explicitly recorded a skip (e.g. medical advice, temporary pause).
4. `UNRECORDED`: Dose time has passed without user interaction.

#### C. Honest Adherence Metric
$$\text{Adherence Rate} = \frac{N_{\text{TAKEN\_ON\_TIME}} + N_{\text{TAKEN\_DELAYED}}}{S_{\text{total}}}$$
- **User Presentation:**
  - *"13 of 14 scheduled doses recorded as taken (12 on time, 1 delayed)."*
  - *"1 dose unrecorded."*
  - Never penalize user with broken streaks or alarmist warnings.

---

### 2.5 Focus Sessions & Deep Work

#### A. Total Focus Duration
$$\text{TotalMinutes}(W) = \sum_{s \in \text{Sessions}(W)} s.\text{durationMinutes}$$
- **Daily Average (Active Days):** $\frac{\text{TotalMinutes}}{N_{\text{active days with focus}}}$
- **Daily Average (Full Week):** $\frac{\text{TotalMinutes}}{7}$ (clarified in UI as "across all 7 days")

#### B. Category Breakdown
For each study category $c$ (e.g. DSA, Frontend, Backend):
$$\text{CategoryShare}(c) = \frac{\sum_{s \in \text{Sessions}(W), s.\text{category} = c} s.\text{durationMinutes}}{\text{TotalMinutes}(W)} \times 100\%$$

---

### 2.6 Hydration

#### A. Daily Total
$$\text{DailyWater}(D) = \sum_{e \in \text{WaterEntries}(D)} e.\text{volumeMl}$$
- If no water entries exist on date $D$: marked as unrecorded (`"--"`), NOT 0 mL.

#### B. Average Across Logged Days
$$\text{AvgLoggedWater}(W) = \frac{\sum_{d \in W, \text{hasEntries}(d)} \text{DailyWater}(d)}{N_{\text{days with water entries}}}$$

---

### 2.7 Google Health Connect Vitals

| Metric | Stored Entity Field | Unit | Missing Value Rule | Observational Summary |
|---|---|---|---|---|
| **Resting Heart Rate** | `restingHeartRateBpm` | bpm | `null` $\to$ `"--"` | Weekly average resting HR |
| **Heart Rate Variability** | `hrvRmssdMs` | ms | `null` $\to$ `"--"` | RMSSD daily range & weekly mean |
| **Oxygen Saturation** | `oxygenSaturationPercent` | % | `null` $\to$ `"--"` | Minimum and average SpO2 |
| **Respiratory Rate** | `respiratoryRateRpm` | rpm | `null` $\to$ `"--"` | Average breaths/min during sleep |
| **Step Count** | `steps` | steps | `0` $\to$ `0 steps` | Total and daily average steps |
| **Active Energy** | `activeCaloriesKcal` | kcal | `0.0` $\to$ `"--"` | Daily active calorie burn |

---

## 3. Timezone & Edge-Case Rules

1. **Local Date Partitioning:**
   - Day boundaries start at `00:00:00.000` and end at `23:59:59.999` in the user's current local timezone (`ZoneId.systemDefault()`).
   - Sleep sessions spanning midnight are assigned to the **wake-up date** (e.g. sleep from 23:30 Nov 8 to 07:15 Nov 9 is attributed to Nov 9).
2. **Daylight Saving Time (DST) Handling:**
   - On the 23-hour "spring forward" day, clock jumps (e.g. 02:00 $\to$ 03:00) must not cause negative sleep durations. Epoch timestamps (`Instant`) are used for duration arithmetic.
   - On the 25-hour "fall back" day, duplicate hour intervals are resolved via monotonic epoch time.
3. **Leap Years:**
   - Date range generators must use `java.time.LocalDate.minusDays(n)` rather than fixed millisecond offsets (`n * 86,400,000L`) to ensure seamless February 29 transitions.
