# Phase B: Automated Test Plan & Quality Assurance Matrix

**Test Plan Date:** November 9, 2026  
**Audited Target:** `com.jayasuriya.rhythm`  
**Test Suite Target:** Robolectric 4.12+ Unit Tests (`app/src/test/java/com/example/`)  
**Authority:** Principal QA Engineer & Release Manager

---

## 1. Overview & Test Architecture

To guarantee the mathematical correctness, honest missing-data handling, and zero-regression reliability of Rhythm's Insights Engine, Phase B establishes a dedicated test suite under `com.example.domain.insights`:

- **Test Harness:** JUnit 4 + Robolectric (`RobolectricTestRunner`) + `kotlinx-coroutines-test` (`StandardTestDispatcher`, `runTest`).
- **Target Test Classes:**
  1. `MetricCalculatorTest.kt`: Pure unit tests validating all mathematical formulas, standard deviations, SRM hits, and edge cases.
  2. `InsightsViewModelTest.kt`: Reactive StateFlow integration tests validating date range changes, Room DAO flow emissions, and UI state mapping.
  3. `PersonalReportExportTest.kt`: Tests validating structured markdown/text report generation and privacy redaction.

---

## 2. Comprehensive Test Case Specifications

### 2.1 Sleep Metric Calculations (`MetricCalculatorTest.kt`)

| Test ID | Test Case Name | Input Conditions | Expected Output | Rationale |
|---|---|---|---|---|
| **SL-01** | `calculateDuration_spansMidnight` | Bedtime: `23:15` (1395m), Wake: `07:30` (450m) | Duration = `495` minutes (8h 15m) | Verifies midnight crossover addition |
| **SL-02** | `calculateDuration_sameDayNap` | Bedtime: `13:00` (780m), Wake: `14:30` (870m) | Duration = `90` minutes (1h 30m) | Verifies daytime session subtraction |
| **SL-03** | `averageDuration_ignoresMissingDays` | 3 days logged (480m, 420m, 540m), 4 days unrecorded | Avg = `480` minutes (8h 0m), count = 3 | Strictly excludes unrecorded days from denominator |
| **SL-04** | `averageDuration_allMissing` | 7 days unrecorded | Avg = `null` | Returns null (renders `"--"`), never 0 |
| **SL-05** | `bedtimeConsistency_calculatesStandardDeviation` | Bedtimes: `23:00`, `23:30`, `23:15`, `00:00`, `23:45` | $s_{\text{bed}} \approx 22.8$ minutes, Category: `CONSISTENT` | Verifies circular time normalization around midnight |
| **SL-06** | `bedtimeConsistency_insufficientData` | 1 day logged | $s_{\text{bed}} = \text{null}$ | Standard deviation requires $N \ge 2$ |

---

### 2.2 Social Rhythm Metric (SRM-5) Tests

| Test ID | Test Case Name | Input Conditions | Expected Output | Rationale |
|---|---|---|---|---|
| **SRM-01** | `anchorHit_within45Minutes_isRegular` | Target: `07:00` (420m), Actual: `07:40` (460m) | `isRegular == true`, Delta = `+40m` | Within ±45 minute circadian window |
| **SRM-02** | `anchorHit_atExact45Boundary_isRegular` | Target: `19:30` (1170m), Actual: `18:45` (1125m) | `isRegular == true`, Delta = `-45m` | Exact boundary condition |
| **SRM-03** | `anchorHit_beyond45Minutes_isNonRegular` | Target: `10:00` (600m), Actual: `10:46` (646m) | `isRegular == false`, Delta = `+46m` | Outside regular window |
| **SRM-04** | `weeklyRegularity_computesRatio` | 24 regular hits, 6 irregular, 5 unrecorded | Regularity = `24 / 30` (80% of recorded events) | Clear separation of recorded vs unrecorded |
| **SRM-05** | `skippedAnchor_handledExplicitly` | Anchor marked `SKIPPED` | `isSkipped == true`, not regular, excluded from actual delta | Respects intentional user skips |

---

### 2.3 Medication Adherence Tests

| Test ID | Test Case Name | Input Conditions | Expected Output | Rationale |
|---|---|---|---|---|
| **MED-01** | `adherence_onTimeDose` | Scheduled: `08:00` (480m), Taken at: `08:25` | Status = `TAKEN_ON_TIME` (within $\pm 60$m) | Validates on-time adherence |
| **MED-02** | `adherence_delayedDose` | Scheduled: `08:00` (480m), Taken at: `10:15` | Status = `TAKEN_DELAYED` ($> 60$m delta) | Differentiates delayed intake |
| **MED-03** | `adherence_weeklyRate` | 14 scheduled doses: 12 on time, 1 delayed, 1 unrecorded | Taken count = `13 / 14` (92.8%), Unrecorded = `1` | Honest, non-judgmental adherence tally |
| **MED-04** | `adherence_respectsStartAndEndDate` | Med started 3 days ago | Pre-start days excluded from scheduled count | Prevents false misses for newly added prescriptions |

---

### 2.4 Focus & Deep Work Query Tests

| Test ID | Test Case Name | Input Conditions | Expected Output | Rationale |
|---|---|---|---|---|
| **FOC-01** | `studyHours_dateBoundedRangeQuery` | 25 sessions logged in 7 days (total 1200m) | Sum = `1200` minutes (20h 0m) | Fixes bug where limit 20 truncated older sessions |
| **FOC-02** | `studyHours_categoryBreakdown` | DSA: 600m, Frontend: 300m, Backend: 100m | DSA: 60%, Frontend: 30%, Backend: 10% | Verifies percentage distribution arithmetic |

---

### 2.5 Personal Summary Report & Privacy Redaction Tests

| Test ID | Test Case Name | Input Conditions | Expected Output | Rationale |
|---|---|---|---|---|
| **EXP-01** | `exportSummary_redactsNotesByDefault` | Report generated with `includeNotes = false` | Text contains averages & counts; zero `symptomsNotes` or thought contents | Protects private psychiatric observations |
| **EXP-02** | `exportSummary_includesNotesWhenOptedIn` | Report generated with `includeNotes = true` | Text includes freeform reflections in dedicated notes section | Fulfills explicit user choice |
| **EXP-03** | `exportSummary_offlineDeterministic` | Generate report in airplane mode | Identical output, 0 network invocations | Guaranteed air-gapped generation |

---

## 3. CI Gate Integration

All tests in this plan are executed under the project's standard CI verification gate:
```bash
./gradlew :app:assembleDebug :app:lintDebug --stacktrace
./gradlew :app:testDebugUnitTest -ProbolectricOffline=false --stacktrace
```
No PR in Phase B may be merged without 100% pass across all test cases.
