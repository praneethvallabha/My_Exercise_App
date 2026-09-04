package com.recoverycoach.app.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

private fun defaultPlanItems(doneIds: Set<String> = setOf("breakfast", "lunch", "evening")) = listOf(
    PlanItem("morning", "Morning", "5.0 km deliberate walk", done = "morning" in doneIds),
    PlanItem("breakfast", "After breakfast", "10 min easy walk", done = "breakfast" in doneIds),
    PlanItem("lunch", "After lunch", "10 min easy walk", done = "lunch" in doneIds),
    PlanItem("dinner", "After dinner", "10 min easy walk", done = "dinner" in doneIds),
    PlanItem("evening", "Evening", "800 m swim, if comfortable", done = "evening" in doneIds),
)

/**
 * Holds all of the app's state. Plan checkmarks, check-in answers, and
 * logged activity are persisted via [RecoveryStore] (DataStore) so they
 * survive an app restart; everything else (recommendation rules, Week's
 * sample trend numbers) is still derived/placeholder, exactly as before.
 */
class RecoveryViewModel(application: Application) : AndroidViewModel(application) {

    private val store = RecoveryStore(application)

    init {
        viewModelScope.launch {
            val saved = store.load()
            saved.doneItemIds?.let { planItems = defaultPlanItems(it) }
            saved.energy?.let { energy = it }
            saved.fatigue?.let { fatigue = it }
            saved.soreness?.let { soreness = it }
            saved.generalFeeling?.let { generalFeeling = it }
            saved.notes?.let { notes = it }
            saved.activity?.let { activity = it }
        }
    }

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
        viewModelScope.launch {
            store.saveCheckIn(energy, fatigue, soreness, generalFeeling, notes)
        }
    }

    // ---- Actual activity (logged via the Log Activity sheet) --------------

    var activity by mutableStateOf(ActivityLog())
        private set

    fun updateActivity(updated: ActivityLog) {
        activity = updated
        viewModelScope.launch { store.saveActivity(updated) }
    }

    // ---- Settings ----------------------------------------------------------

    /** Wipes persisted data and resets every field back to its Phase 1 default. */
    fun resetAllData() {
        planItems = defaultPlanItems()
        energy = 3
        fatigue = 4
        soreness = 3
        soreDetailsExpanded = false
        generalFeeling = GeneralFeeling.NORMAL
        notes = ""
        activity = ActivityLog()
        viewModelScope.launch { store.clearAll() }
    }

    // ---- Recommendation ------------------------------------------------

    /**
     * Recent load vs. the 28-day baseline, as a percentage above baseline.
     * Real step/distance history isn't wired up yet, so this uses the
     * Phase 1 example value until a data source exists.
     */
    val recentLoadPercentAboveBaseline: Int = 23

    val recommendedLevel: RecoveryLevel
        get() = when {
            soreness >= 5 -> RecoveryLevel.RECOVERY
            fatigue >= 4 || recentLoadPercentAboveBaseline > 15 -> RecoveryLevel.EASY
            else -> RecoveryLevel.NORMAL
        }

    val recommendationReasons: List<String>
        get() = when (recommendedLevel) {
            RecoveryLevel.RECOVERY -> listOf(
                "Weight-bearing pain reached $soreness/10, your recovery threshold.",
            )
            RecoveryLevel.EASY -> buildList {
                if (fatigue >= 4) add("Fatigue $fatigue/10 is at your easy threshold.")
                if (recentLoadPercentAboveBaseline > 15) {
                    add("Recent load is $recentLoadPercentAboveBaseline% above your 28-day baseline.")
                }
            }
            RecoveryLevel.NORMAL -> listOf(
                "No rule triggered an easier day.",
                "Yesterday's check-in was within your usual range.",
            )
        }

    /** The bold stop-and-seek-assessment line shown only on a recovery day. */
    val recoveryWarning: String?
        get() = if (recommendedLevel == RecoveryLevel.RECOVERY) {
            "Stop the deliberate walk and seek assessment if this does not settle."
        } else {
            null
        }

    // ---- Week / trends (placeholder history until real logging accumulates) --

    val weekWalkTotalKm = 44.6
    val weekExerciseMinutes = 388
    val weekRecoveryDays = 1
    val weekSwimMinutes = 126
    val baselineWalkKmPerWeek = 36.2
    val baselineDaysLogged = 26

    val loadBars: List<LoadBar> = listOf(
        LoadBar("Fri", 46, RecoveryLevel.NORMAL.loadBarColor),
        LoadBar("Sat", 78, RecoveryLevel.NORMAL.loadBarColor),
        LoadBar("Sun", 24, RecoveryLevel.RECOVERY.loadBarColor),
        LoadBar("Mon", 64, RecoveryLevel.NORMAL.loadBarColor),
        LoadBar("Tue", 92, RecoveryLevel.NORMAL.loadBarColor),
        LoadBar("Wed", 71, RecoveryLevel.NORMAL.loadBarColor),
        LoadBar("Thu", 38, RecoveryLevel.EASY.loadBarColor),
    )

    val weekDays: List<WeekDayRecord> = listOf(
        WeekDayRecord("Wednesday", "2 Sep", RecoveryLevel.NORMAL, "5.10 km · 52 min", "7.9 km · 11,204 steps", "800 m · 41 min", "Fatigue 4/10 · soreness 3/10"),
        WeekDayRecord("Tuesday", "1 Sep", RecoveryLevel.NORMAL, "5.40 km · 55 min", "9.2 km · 12,880 steps", "1,000 m · 44 min", "Fatigue 3/10 · soreness 2/10"),
        WeekDayRecord("Monday", "31 Aug", RecoveryLevel.EASY, "3.20 km · 36 min", "5.4 km · 7,610 steps", "—", "Fatigue 5/10 · soreness 4/10"),
    )
}
