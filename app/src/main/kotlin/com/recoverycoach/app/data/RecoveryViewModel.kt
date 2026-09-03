package com.recoverycoach.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Holds all of the app's state in memory for now. There is no persistence or
 * real health-data source yet (the design calls this out explicitly: "Health
 * data can fill this automatically in Phase 2; for now, confirm manually.").
 * Everything here is designed to be swapped for a real repository later
 * without the screens needing to change.
 */
class RecoveryViewModel : ViewModel() {

    // ---- Today's plan ----------------------------------------------------

    var planItems by mutableStateOf(
        listOf(
            PlanItem("morning", "Morning", "5.0 km deliberate walk", done = false),
            PlanItem("breakfast", "After breakfast", "10 min easy walk", done = true),
            PlanItem("lunch", "After lunch", "10 min easy walk", done = true),
            PlanItem("dinner", "After dinner", "10 min easy walk", done = false),
            PlanItem("evening", "Evening", "800 m swim, if comfortable", done = true),
        )
    )
        private set

    fun togglePlanItem(id: String) {
        planItems = planItems.map { if (it.id == id) it.copy(done = !it.done) else it }
    }

    // ---- Evening check-in --------------------------------------------------

    var energy by mutableStateOf(3)
        private set

    fun setEnergy(value: Int) {
        energy = value
    }

    /** Leg / body fatigue, 0 (none) to 10 (severe). */
    var fatigue by mutableStateOf(4)
        private set

    fun setFatigue(value: Int) {
        fatigue = value.coerceIn(0, 10)
    }

    /** Highest soreness reported, 0 (none) to 10 (severe). Entered via "Add pain / soreness details". */
    var soreness by mutableStateOf(3)
        private set

    fun setSoreness(value: Int) {
        soreness = value.coerceIn(0, 10)
    }

    var soreDetailsExpanded by mutableStateOf(false)
        private set

    fun toggleSoreDetails() {
        soreDetailsExpanded = !soreDetailsExpanded
    }

    var generalFeeling by mutableStateOf(GeneralFeeling.NORMAL)
        private set

    fun setGeneralFeeling(feeling: GeneralFeeling) {
        generalFeeling = feeling
    }

    var notes by mutableStateOf("")
        private set

    fun setNotes(value: String) {
        notes = value
    }

    fun saveCheckIn() {
        // No persistence yet — the check-in values already live in this
        // ViewModel's state and immediately drive `recommendedLevel` above.
        // This hook exists so a real save-to-disk step has somewhere to go.
    }

    // ---- Actual activity (logged via the Log Activity sheet) --------------

    var activity by mutableStateOf(ActivityLog())
        private set

    fun updateActivity(updated: ActivityLog) {
        activity = updated
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
