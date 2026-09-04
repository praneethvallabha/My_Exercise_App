package com.recoverycoach.app.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.recoverycoach.app.domain.GuidanceEngine
import com.recoverycoach.app.domain.GuidanceResult
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private fun defaultPlanItems(doneIds: Set<String> = emptySet()) = listOf(
    PlanItem("morning", "Morning", "5.0 km deliberate walk", done = "morning" in doneIds),
    PlanItem("breakfast", "After breakfast", "10 min easy walk", done = "breakfast" in doneIds),
    PlanItem("lunch", "After lunch", "10 min easy walk", done = "lunch" in doneIds),
    PlanItem("dinner", "After dinner", "10 min easy walk", done = "dinner" in doneIds),
    PlanItem("evening", "Evening", "800 m swim, if comfortable", done = "evening" in doneIds),
)

/**
 * Holds all of the app's state.
 *
 * Today's working state (plan checkmarks, check-in answers, logged activity) is
 * persisted via [RecoveryStore]. At midnight — strictly, the first time the app
 * notices the calendar day has changed — the working day is filed into a real
 * dated archive and the working state resets. Week trends and guidance both read
 * that archive, so nothing shown to the user is invented.
 */
class RecoveryViewModel(application: Application) : AndroidViewModel(application) {

    private val store = RecoveryStore(application)

    var history by mutableStateOf<List<DayRecord>>(emptyList())
        private set

    /** True once the user has saved a check-in or activity for today. */
    var dayTouched by mutableStateOf(false)
        private set

    private var currentDay: LocalDate = LocalDate.now()

    init {
        viewModelScope.launch { restore() }
    }

    private suspend fun restore() {
        val saved = store.load()
        val today = LocalDate.now()
        val storedDay = saved.currentDayEpoch

        if (DayRollover.shouldRollOver(storedDay, today.toEpochDay())) {
            val finishedDay = LocalDate.ofEpochDay(storedDay!!)
            if (saved.dayTouched) store.upsertDay(finishedRecord(finishedDay, saved))
            store.resetDayState(today.toEpochDay())
            applyDefaults()
        } else {
            if (storedDay == null) store.saveCurrentDayEpoch(today.toEpochDay())
            saved.doneItemIds?.let { planItems = defaultPlanItems(it) }
            saved.energy?.let { energy = it }
            saved.fatigue?.let { fatigue = it }
            saved.soreness?.let { soreness = it }
            saved.generalFeeling?.let { generalFeeling = it }
            saved.notes?.let { notes = it }
            saved.activity?.let { activity = it }
            dayTouched = saved.dayTouched
        }

        currentDay = today
        history = store.load().history // reloaded: a rollover above may have just filed a day
    }

    /** Re-checks the calendar day. Called when the app returns to the foreground. */
    fun refreshForToday() {
        if (currentDay == LocalDate.now()) return
        viewModelScope.launch { restore() }
    }

    private fun applyDefaults() {
        planItems = defaultPlanItems()
        energy = 3
        fatigue = 4
        soreness = 3
        soreDetailsExpanded = false
        generalFeeling = GeneralFeeling.NORMAL
        notes = ""
        activity = ActivityLog()
        dayTouched = false
    }

    private fun finishedRecord(date: LocalDate, saved: PersistedState) = DayRecord.of(
        date = date,
        activity = saved.activity ?: ActivityLog(),
        energy = saved.energy ?: 3,
        fatigue = saved.fatigue ?: 0,
        soreness = saved.soreness ?: 0,
        generalFeeling = saved.generalFeeling ?: GeneralFeeling.NORMAL,
        notes = saved.notes.orEmpty(),
        // The load-vs-baseline comparison for a past day is not recoverable from
        // what was persisted, and `loadPercentAboveBaseline` is anchored to
        // `currentDay` — which during a rollover is not the day being filed. Pass
        // zero rather than a number belonging to a different day: soreness and
        // fatigue decide the level in every case except a load spike, and a wrong
        // load figure would be written into the archive permanently.
        level = RecoveryLevelRules.levelFor(saved.soreness ?: 0, saved.fatigue ?: 0, loadPercentAboveBaseline = 0),
    )

    // ---- Today's plan ----------------------------------------------------

    var planItems by mutableStateOf(defaultPlanItems())
        private set

    fun togglePlanItem(id: String) {
        planItems = planItems.map { if (it.id == id) it.copy(done = !it.done) else it }
        viewModelScope.launch {
            store.savePlanDoneIds(planItems.filter { it.done }.map { it.id }.toSet())
        }
    }

    // ---- Evening check-in --------------------------------------------------

    // These are plain public vars rather than private-set-plus-setter-function:
    // a same-named `fun setEnergy(...)` alongside `var energy` is a "platform
    // declaration clash" in Kotlin — both compile to the same JVM method
    // (`setEnergy(int)`), which the compiler rejects even though one is private.
    var energy by mutableStateOf(3)

    /** Leg / body fatigue, 0 (none) to 10 (severe). */
    var fatigue by mutableStateOf(4)

    /** Highest soreness reported, 0 (none) to 10 (severe). Entered via "Add pain / soreness details". */
    var soreness by mutableStateOf(3)

    var soreDetailsExpanded by mutableStateOf(false)
        private set

    fun toggleSoreDetails() {
        soreDetailsExpanded = !soreDetailsExpanded
    }

    var generalFeeling by mutableStateOf(GeneralFeeling.NORMAL)

    var notes by mutableStateOf("")

    fun saveCheckIn() {
        dayTouched = true
        viewModelScope.launch {
            store.saveCheckIn(energy, fatigue, soreness, generalFeeling, notes)
        }
    }

    // ---- Actual activity (logged via the Log Activity sheet) --------------

    var activity by mutableStateOf(ActivityLog())
        private set

    fun updateActivity(updated: ActivityLog) {
        activity = updated
        dayTouched = true
        viewModelScope.launch { store.saveActivity(updated) }
    }

    // ---- Settings ----------------------------------------------------------

    /** Wipes persisted data — the archive included — and resets every field. */
    fun resetAllData() {
        applyDefaults()
        history = emptyList()
        viewModelScope.launch {
            store.clearAll()
            store.saveCurrentDayEpoch(LocalDate.now().toEpochDay())
        }
    }

    /**
     * Fills the archive with generated days so guidance can be exercised without
     * a month of real logging. Debug builds only — the caller checks
     * `BuildConfig.DEBUG`. Today's working state is left untouched.
     */
    fun seedSampleHistory() {
        val seeded = DebugSeed.history(LocalDate.now())
        history = seeded
        viewModelScope.launch { store.replaceHistory(seeded) }
    }

    // ---- Recommendation ------------------------------------------------

    /**
     * Today as it currently stands, so guidance and trends include it rather than
     * waiting for midnight. Null until the user has actually entered something —
     * an untouched day is not evidence of a rest day.
     */
    private val provisionalToday: DayRecord?
        get() = if (dayTouched) {
            DayRecord.of(currentDay, activity, energy, fatigue, soreness, generalFeeling, notes, recommendedLevel)
        } else {
            null
        }

    /** The archive plus today, which is what every trend and rule should read. */
    val series: List<DayRecord>
        get() = (history.filterNot { it.epochDay == currentDay.toEpochDay() } + listOfNotNull(provisionalToday))
            .sortedBy { it.epochDay }

    /**
     * Recent 7-day aerobic load against the preceding 28-day average, as a
     * percentage above baseline. Returns 0 until there is enough real history for
     * the comparison to mean anything — a made-up baseline would drive a real
     * recommendation.
     */
    val loadPercentAboveBaseline: Int
        get() {
            val today = currentDay.toEpochDay()
            val recent = history.filter { it.epochDay in (today - 6)..today }
            val baseline = history.filter { it.epochDay in (today - 34)..(today - 7) }
            if (recent.isEmpty() || baseline.size < 14) return 0
            val baselineWeekly = baseline.sumOf { it.aerobicMinutes }.toDouble() / baseline.size * 7
            if (baselineWeekly <= 0.0) return 0
            val recentWeekly = recent.sumOf { it.aerobicMinutes }.toDouble()
            return (((recentWeekly - baselineWeekly) / baselineWeekly) * 100).roundToInt()
        }

    /** True once [loadPercentAboveBaseline] has enough history to be meaningful. */
    val hasLoadBaseline: Boolean
        get() = history.count { it.epochDay in (currentDay.toEpochDay() - 34)..(currentDay.toEpochDay() - 7) } >= 14

    val recommendedLevel: RecoveryLevel
        get() = RecoveryLevelRules.levelFor(soreness, fatigue, loadPercentAboveBaseline)

    val recommendationReasons: List<String>
        get() = when (recommendedLevel) {
            RecoveryLevel.RECOVERY -> listOf(
                "Weight-bearing pain reached $soreness/10, your recovery threshold.",
            )
            RecoveryLevel.EASY -> buildList {
                if (fatigue >= RecoveryLevelRules.EASY_FATIGUE) add("Fatigue $fatigue/10 is at your easy threshold.")
                if (loadPercentAboveBaseline > RecoveryLevelRules.EASY_LOAD_PERCENT) {
                    add("Recent load is $loadPercentAboveBaseline% above your 28-day baseline.")
                }
            }
            RecoveryLevel.NORMAL -> buildList {
                add("No rule triggered an easier day.")
                if (hasLoadBaseline) {
                    add("Recent load is within your 28-day baseline.")
                } else {
                    add("Still building a 28-day baseline from your logged days.")
                }
            }
        }

    /** The bold stop-and-seek-assessment line shown only on a recovery day. */
    val recoveryWarning: String?
        get() = if (recommendedLevel == RecoveryLevel.RECOVERY) {
            "Stop the deliberate walk and seek assessment if this does not settle."
        } else {
            null
        }

    // ---- Guidance ----------------------------------------------------------

    val guidance: GuidanceResult
        get() = GuidanceEngine.evaluate(series, currentDay.toEpochDay())

    // ---- Week / trends (derived from the real archive) ---------------------

    private val weekWindow: List<DayRecord>
        get() = series.filter { it.epochDay in (currentDay.toEpochDay() - 6)..currentDay.toEpochDay() }

    val weekWalkTotalKm: Double
        get() = (weekWindow.sumOf { it.activity.totalWalkKm } * 10).roundToInt() / 10.0

    val weekExerciseMinutes: Int get() = weekWindow.sumOf { it.aerobicMinutes }

    val weekRecoveryDays: Int get() = weekWindow.count { it.level == RecoveryLevel.RECOVERY }

    val weekSwimMinutes: Int get() = weekWindow.sumOf { it.activity.swimMin }

    val weekStrengthSessions: Int get() = weekWindow.count { it.didStrength }

    /** Days actually logged in the trailing week — the honest denominator. */
    val weekDaysLogged: Int get() = weekWindow.size

    val baselineDaysLogged: Int get() = history.size

    /**
     * Reads [history] rather than [series] on purpose: today is still in progress,
     * and a partial day would drag the long-run reference down every morning. The
     * seven-day trend above does include today — the two numbers are meant to
     * answer different questions.
     */
    val baselineWalkKmPerWeek: Double
        get() {
            if (history.isEmpty()) return 0.0
            val perDay = history.sumOf { it.activity.totalWalkKm } / history.size
            return (perDay * 7 * 10).roundToInt() / 10.0
        }

    /**
     * Seven bars, one per calendar day, oldest first. A day with no record is a
     * zero-height bar rather than a gap — a missed day and a rest day look
     * different in the detail list below, but both read as no load here.
     */
    val loadBars: List<LoadBar>
        get() {
            val byDay = weekWindow.associateBy { it.epochDay }
            val peak = weekWindow.maxOfOrNull { it.aerobicMinutes }?.takeIf { it > 0 } ?: 1
            return (6 downTo 0).map { back ->
                val day = currentDay.minusDays(back.toLong())
                val record = byDay[day.toEpochDay()]
                LoadBar(
                    day = day.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())),
                    heightDp = record?.let { (it.aerobicMinutes.toDouble() / peak * MAX_BAR_DP).roundToInt() } ?: 0,
                    color = (record?.level ?: RecoveryLevel.NORMAL).loadBarColor,
                )
            }
        }

    val weekDays: List<WeekDayRecord>
        get() = weekWindow.sortedByDescending { it.epochDay }.map { record ->
            WeekDayRecord(
                name = record.date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())),
                date = record.date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())),
                level = record.level,
                walk = if (record.activity.morningWalkKm > 0) {
                    "${record.activity.morningWalkKm} km · ${record.activity.morningWalkMin} min"
                } else "—",
                total = "${record.activity.totalWalkKm} km · ${String.format(Locale.getDefault(), "%,d", record.activity.steps)} steps",
                swim = if (record.activity.swimM > 0) "${record.activity.swimM} m · ${record.activity.swimMin} min" else "—",
                strength = if (record.didStrength) "${record.activity.strengthMin} min" else "—",
                feedback = "Fatigue ${record.fatigue}/10 · soreness ${record.soreness}/10",
            )
        }

    private companion object {
        const val MAX_BAR_DP = 100
    }
}
