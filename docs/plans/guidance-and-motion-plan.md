# Plan — Evidence-Backed Guidance Layer + Motion Pass

Decision taken: **Option (b)** — persist real dated history first, then run guidance over the stored series.
Status: **implemented**. See §7 for what was verified.

---

## 0. Corrections to the prompt's assumptions

Two things in the prompt did not survive contact with the codebase.

**There is no `NavHost`.** `RecoveryApp.kt:47` switches screens with a `when` over a
`RecoveryDestination` enum inside a `Box`. There is no `navigation-compose` dependency.
So `enterTransition`/`exitTransition` do not apply. The correct API is `AnimatedContent`
keyed on `currentDestination` — less work, no new navigation dependency, and it keeps the
route graph exactly as it is.

**Build config must change after all.** The prompt said not to touch it. Three additions
are unavoidable; all are narrow and none affect auth, signing, or SDK levels:
- `androidx.compose.material:material-icons-extended` — icons were explicitly requested
- `androidx.compose.animation:animation` — pinned explicitly rather than relied on transitively
- `kotlinx-serialization-json` + the matching Kotlin plugin — to serialise history (see §2).
  This touches **two** files: a new `[plugins]` entry in `gradle/libs.versions.toml`
  (`kotlin-serialization`, `version.ref = "kotlin"`, tracking `kotlin = "2.4.10"`) and a
  matching `alias(libs.plugins.kotlin.serialization)` line in `app/build.gradle.kts`.
  Missing either produces a confusing compile error.

Flagging rather than silently proceeding. Say the word if you'd rather avoid the
serialization dependency; the fallback is hand-rolled string encoding in DataStore, which
is more code and more fragile.

---

## 1. Research findings

Every number below is sourced. Where the app can't yet support a recommendation, it says so.

| # | Guidance | Figure | Source |
|---|---|---|---|
| R1 | Weekly aerobic activity | ≥150 min/week moderate-to-vigorous | ADA, *Standards of Care in Diabetes* §5, 2026 |
| R2 | Spread of activity | ≥3 days/week, **no more than 2 consecutive days without activity** | ADA, *Standards of Care* §5, 2026 |
| R3 | Vigorous alternative | ≥75 min/week suffices for fitter individuals | ADA, *Standards of Care* §5, 2026 |
| R4 | General adult range | 150–300 min/week moderate, or 75–150 vigorous; >300 min for added benefit | WHO, *Guidelines on Physical Activity and Sedentary Behaviour*, 2020 |
| R5 | Sedentary time | Limit it; replace with activity of **any** intensity, including light | WHO, 2020 |
| R6 | Resistance training | 2–3 sessions/week on **nonconsecutive** days | ADA, *Standards of Care* §5, 2026 |
| R7 | Combined training | Aerobic + resistance beats either alone for HbA1c, BP, strength | ADA, *Physical Activity/Exercise and Diabetes Position Statement*, Diabetes Care 39(11), 2016 |
| R8 | Sitting breaks | Breaking up sitting lowers postprandial glucose; **walking breaks outperform standing**; effect largest in type 2 diabetes; 30-min intervals beat 45–60 min | Gale et al., *Obesity Reviews*, 2025 (meta-analysis, 39 studies); Dempsey et al., *Diabetes Care* 39(1), 2016 |
| R9 | Post-meal walking | 10–15 min immediately after meals cuts postprandial excursions | DiPietro et al., *Diabetes Care* 36(10), 2013; *Scientific Reports*, 2025 |
| R10 | Distribution beats duration | 3 × 15-min post-meal walks improved 24-h glycemic control more than one 45-min walk | DiPietro et al., *Diabetes Care* 36(10), 2013 |

**Where sources diverge.** ADA sets the floor at 150 min/week; WHO frames 150–300 as a
range with benefit continuing above 300. These are compatible, not contradictory — the plan
uses **ADA 150 min as the target** (condition-specific, more conservative) and mentions the
WHO upper range only as context, never as a goal to chase.

**Where the evidence is thinner.** Optimal sitting-break *frequency* is less settled than
the fact that breaks help. R8's 30-minute figure comes from subgroup comparison, not a
head-to-head trial. The tip will be phrased as a suggestion, not a target with a number
attached to a progress bar.

### Two data gaps this research exposes

1. **R6/R7 cannot fire.** `ActivityLog` has no resistance-training field — only walk, steps,
   swim, heart points. Either add a strength-session field to the log sheet, or suppress
   those two tips. Recommend adding the field; it's the single highest-value guidance the
   app is currently blind to.
2. **Intensity is unmeasured.** The app logs *minutes*, not intensity. Mapping walk and swim
   minutes onto "moderate-intensity minutes" is an assumption. Plan: count logged walk and
   swim minutes as moderate, state that assumption in the UI once, and treat the 150-minute
   figure as an estimate rather than a verified count. Heart points (already logged) are the
   better long-term proxy if you later sync them from a real source.

---

## 2. History persistence (the option-b work)

**New model** — `data/DayRecord.kt`:
```
DayRecord(
  epochDay: Long,           // NOT LocalDate — see note below
  activity: ActivityLog,
  energy/fatigue/soreness: Int?,
  generalFeeling: GeneralFeeling?,
  level: RecoveryLevel,
  notes: String?,
)
```

**Storage** — one `@Serializable` list, JSON-encoded into a single new DataStore string key
(`history_json`). Existing keys are untouched, so current installs keep their data.
Rolling 90-day retention, trimmed on write.

**Date representation — committing to `epochDay: Long`.** `java.time.LocalDate` is available
at `minSdk 26`, but there is no `coreLibraryDesugaring` block in `app/build.gradle.kts`, so
there is no fallback if any part of the serialization path needs a `LocalDate` adapter or a
`DateTimeFormatter`. Storing the raw epoch day as a `Long` sidesteps the question entirely:
it serialises as a primitive, needs no custom adapter, and converts to `LocalDate` at the
boundary where the UI formats it. The whole storage schema hangs off this, so it is decided
here rather than left to the implementer.

**Day rollover** — the subtle part. On app start and on resume, compare the stored
`current_day_epoch` against today. If it differs, finalise the working day into `history_json`,
then reset the working state (plan checkmarks, check-in values, activity log) for the new day.
Without this the app silently overwrites one day forever, which is the current behaviour.

**Week screen** — `weekDays` and `loadBars` in `RecoveryViewModel.kt:151-173` stop being
hardcoded literals and derive from the last 7 `DayRecord`s. Days with no record render as
an explicit empty state, not a zero — a missed day and a rest day are not the same thing.

**Migration** — first launch after the update seeds today's record from whatever is already
in DataStore. No back-fill of fake history; the Week screen honestly shows one day and fills in.

---

## 3. Guidance layer design

**`domain/GuidanceEngine.kt`** — a pure object, no Android imports, fully unit-testable:
```
fun evaluate(history: List<DayRecord>, today: LocalDate): List<Tip>
```

**`domain/Tip.kt`**:
```
Tip(
  id: TipId,
  title: String,
  body: String,
  severity: INFO | SUGGESTION | ATTENTION,
  source: String,        // "ADA Standards of Care, 2026"
  minDaysRequired: Int,
)
```

**Rule catalogue** — each rule maps to a research row:

| Tip | Fires when | Needs | Source shown |
|---|---|---|---|
| `WEEKLY_MINUTES_BEHIND` | 7-day moderate minutes < 150 | 7 days | R1 |
| `WEEKLY_MINUTES_MET` | ≥150 min in trailing 7 days | 7 days | R1 / R4 |
| `TWO_DAY_GAP` | 2 consecutive zero-activity days | 3 days | R2 |
| `SPREAD_TOO_NARROW` | Minutes met but on <3 days | 7 days | R2 |
| `SITTING_BREAKS` | Low step count with logged walk minutes | 3 days | R8 |
| `POST_MEAL_WALK` | Rotating educational tip, low frequency | 1 day | R9 / R10 |
| `RESISTANCE_MISSING` | <2 strength sessions in 7 days | 7 days + new field | R6 / R7 |
| `SUSTAINED_FATIGUE` | Fatigue ≥7 **and** soreness ≥7 for 3+ days | 3 days | *App heuristic — not clinical. Labelled as such.* |

**Ranking and volume.** At most **two** tips on screen. Order: `ATTENTION` → `SUGGESTION` →
`INFO`. Tie-break on longest-unshown, so tips rotate instead of one tip pinning forever.

**Cold start.** Under `minDaysRequired`, the card shows
"Building your baseline — N more days of logging" rather than a fabricated tip. This is the
whole reason option (b) was the right call.

**Surface.** A `GuidanceCard` on `TodayScreen`, below the recovery-level card. Each tip
renders its source line in `RecoveryType.caption` / `RecoveryColors.TextSecondary`. Week
screen gets a compact weekly-minutes-vs-150 summary row.

**Medical scoping, as built:**
- Sourced educational guidance only; no individualized prescription
- Zero medication, insulin, or dosing content anywhere in the catalogue
- `SUSTAINED_FATIGUE` and the existing `onReportWarningSymptom` hook both route to
  "worth raising with your doctor" — the app never interprets a symptom
- One permanent disclaimer in Settings, plus a single quiet source line per tip. No repeated banners.

**While editing Settings, do not touch `SettingsScreen.kt:46`.** It reads "Units, reminder
times and Health Connect sync are planned for later." That sentence remains accurate — this
work adds none of the three — and an implementer adding the disclaimer nearby will be tempted
to "refresh" it. Leave it exactly as it is.

---

## 4. Motion + icon changes

| File | Change | API |
|---|---|---|
| `ui/theme/Motion.kt` **(new)** | Duration/easing tokens (`Fast 150ms`, `Standard 250ms`, `Emphasized 300ms`) + `rememberReduceMotion()` | `tween`, `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` |
| `ui/RecoveryApp.kt` | Wrap the `when` in `AnimatedContent`; direction from enum `ordinal` delta | `AnimatedContent`, `slideIntoContainer`, `fadeIn`/`fadeOut`, `togetherWith` |
| `ui/navigation/RecoveryDestination.kt` | Add `icon: ImageVector` per destination | `Icons.Outlined.Today / FactCheck / BarChart / Settings` |
| `ui/components/BottomNavBar.kt` | Icon above label; animate pill background and label colour instead of swapping two `Text`s | `animateColorAsState`, `animateFloatAsState` |
| `ui/screens/TodayScreen.kt` | Plan-item check animation; guidance card enters and resizes | `animateFloatAsState`, `AnimatedVisibility`, `animateContentSize` |
| `ui/components/Common.kt` | Recovery-level card animates colour on level change rather than snapping | `animateColorAsState` |
| `ui/screens/WeekScreen.kt` | Load bars grow from zero on first composition; day rows stagger in | `animateDpAsState`, `AnimatedVisibility` |
| `ui/screens/LogActivitySheet.kt` | Save-confirmation transition (sheet itself is already animated) | `AnimatedVisibility`, `Crossfade` |

**Constraints held:**
- All durations and easings come from the new `Motion` object — no scattered literals
- All colours stay in `RecoveryColors`; no new values
- `rememberReduceMotion()` collapses every duration to 0ms; the app stays fully functional
- Nothing exceeds 300ms; nothing blocks input
- No custom vector assets — Material icons only

---

## 5. Sequencing

Each step builds, runs, and is independently shippable.

1. **History persistence.** `DayRecord`, serialization, rollover, retention. Week screen reads
   real records. *Verify:* rollover unit tests, manual multi-day check via clock change.
2. **Resistance-session field.** Add to `ActivityLog` + Log Activity sheet. Unblocks R6/R7.
3. **Guidance engine.** Pure logic + the full tip catalogue. *Verify:* unit tests only, no UI.
4. **Guidance UI.** `GuidanceCard` on Today, weekly summary on Week, Settings disclaimer.
5. **Motion + icons.** `Motion.kt`, then the file table in §4 top to bottom.

Steps 1–2 are prerequisites for 3–4. Step 5 is independent and can be pulled forward if you
want the visible win first.

---

## 6. Risks and open questions

**Risks**
- *Rollover is the bug-prone part.* Timezone changes, device clock changes, and the app being
  killed mid-write can all corrupt or duplicate a day. Mitigation: idempotent writes keyed on
  `LocalDate`, plus explicit tests for backwards clock movement.
- *Intensity assumption.* Counting all walk and swim minutes as "moderate" will overstate
  weekly minutes for gentle walks. Stated in the UI; revisit if heart points become reliable.
- *DataStore JSON scaling.* Fine at 90 days. If retention grows past ~a year, migrate to Room.
- *Tip fatigue.* Two-tip cap and rotation are the mitigation; watch it in real use.

**One question that blocks work**

Add the resistance-training field in step 2, or ship without R6/R7 for now? This gates steps
2–3 and changes the tip catalogue, so it needs answering before implementation starts.
Recommend adding it — resistance training is the highest-value guidance the app is currently
blind to, and the field is a single number on a sheet that already exists.

Two smaller calls, defaulted unless you say otherwise: **90-day retention**, and the guidance
card **always present** rather than per-day dismissible.

---

## 7. Verification

Run on 2026-09-04 with `JAVA_HOME` pointed at Temurin 17 — the machine's default JDK is
25, which Gradle 8.9 rejects outright. This is a local toolchain issue, not a project one;
no project file was changed to work around it.

| Check | Result |
|---|---|
| `./gradlew clean assembleDebug` | **Passed** — `app-debug.apk` produced |
| `./gradlew testDebugUnitTest` | **Passed** — 46 tests, 0 failures |
| `./gradlew lintDebug` | **Passed** — 15 warnings, 0 errors, all pre-existing |

Lint's warnings are dependency-version notices, the launcher-icon monochrome tag, a
`ModifierParameter` note in untouched `Common.kt`, and `AutoboxingStateCreation` on the
pre-existing `energy`/`fatigue`/`soreness` state. Nothing new was introduced.

Lint also prints `Module was compiled with an incompatible version of Kotlin` against
`kotlin-stdlib` — AGP 8.5.2's bundled analyzer expects Kotlin 2.0 metadata and the project
is on 2.4.10. Pre-existing, and lint completes regardless.

**One bug found and fixed during implementation.** The archived level for a finished day
was computed with `loadPercentAboveBaseline`, a getter anchored to `currentDay` — which
during a rollover is not the day being filed, and after a relaunch is today. A wrong load
figure would have been written permanently into the archive. The level rule now lives in
`RecoveryLevelRules` (pure, shared between the archived and displayed paths), archiving
passes zero load, and `RecoveryLevelRulesTest` covers it.

**Run on an emulator (API 36).** Installed and driven end to end: check-in saves, the Week
screen picks up today as a real record with correct day labels and level colour, the debug
seeder fills 35 days, and the guidance card renders sourced tips. Screens verified: Today,
Check-in, Week, Settings.

**Still not verified:** the midnight rollover against a real device clock (the emulator is a
production build, so `adb shell date` needs root it does not have — it has to be driven
through Settings → Date & time), and how the motion feels in the hand rather than in a
screenshot.

**Debug seeder.** `DebugSeed` generates 35 days ending yesterday, shaped to land under the
weekly target with one strength session so the useful tips fire, and to supply the 14+ days
the 28-day load baseline needs. Behind `BuildConfig.DEBUG`, covered by `DebugSeedTest`, and
removed by "Clear all data". It stops at yesterday deliberately — today belongs to the user.

---

## Sources

- ADA, *Standards of Care in Diabetes—2026*, §5 — https://diabetesjournals.org/care/article/49/Supplement_1/S89/163932/5-Facilitating-Positive-Health-Behaviors-and-Well
- ADA, *Physical Activity/Exercise and Diabetes: Position Statement*, Diabetes Care 39(11), 2016 — https://diabetesjournals.org/care/article/39/11/2065/37249/Physical-Activity-Exercise-and-Diabetes-A-Position
- WHO, *Guidelines on Physical Activity and Sedentary Behaviour*, 2020 — https://www.who.int/europe/publications/i/item/9789240014886
- Gale et al., *Obesity Reviews*, 2025 — https://onlinelibrary.wiley.com/doi/10.1111/obr.70152
- Dempsey et al., *Diabetes Care* 39(1), 2016 — https://diabetesjournals.org/care/article/39/1/130/31522/Breaking-Up-Prolonged-Sitting-With-Standing-or
- DiPietro et al., *Diabetes Care* 36(10), 2013 — https://diabetesjournals.org/care/article/36/10/3262/30770/Three-15-min-Bouts-of-Moderate-Postmeal-Walking
- *Scientific Reports*, 2025 — https://www.nature.com/articles/s41598-025-07312-y
