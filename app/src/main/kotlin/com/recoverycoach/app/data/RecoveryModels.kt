package com.recoverycoach.app.data

import androidx.compose.ui.graphics.Color
import com.recoverycoach.app.ui.theme.RecoveryColors
import kotlinx.serialization.Serializable

/** How hard today should be, and why. Computed from the day's check-in signals. */
enum class RecoveryLevel(val title: String, val cardBg: Color, val cardText: Color) {
    NORMAL("Normal day", RecoveryColors.NormalBg, RecoveryColors.NormalText),
    EASY("Easy day", RecoveryColors.EasyBg, RecoveryColors.EasyText),
    RECOVERY("Recovery day", RecoveryColors.RecoveryBg, RecoveryColors.RecoveryText);

    val loadBarColor: Color
        get() = when (this) {
            NORMAL -> RecoveryColors.LoadNormal
            EASY -> RecoveryColors.LoadEasy
            RECOVERY -> RecoveryColors.LoadRecovery
        }
}

/** One line of today's planned activity, e.g. "Morning · 5.0 km deliberate walk". */
data class PlanItem(
    val id: String,
    val whenLabel: String,
    val title: String,
    val done: Boolean = false,
)

/** What was actually logged for the day, entered manually via the Log Activity sheet. */
@Serializable
data class ActivityLog(
    val morningWalkKm: Double = 4.12,
    val morningWalkMin: Int = 47,
    val totalWalkKm: Double = 6.8,
    val steps: Int = 9412,
    val swimM: Int = 800,
    val swimMin: Int = 42,
    val heartPoints: Int = 31,
    /**
     * Minutes of resistance / strength work. Zero means no session that day.
     * ADA counts *sessions* on nonconsecutive days rather than minutes, so the
     * guidance engine only ever asks whether this is above zero.
     */
    val strengthMin: Int = 0,
) {
    /**
     * Minutes that count toward the weekly aerobic target. Only the deliberate
     * walk and the swim have a recorded duration — incidental steps do not — so
     * those two are all this can honestly sum. Intensity is not measured, so
     * every logged minute is assumed moderate; the UI says so where the number
     * is shown.
     */
    val aerobicMinutes: Int get() = morningWalkMin + swimMin

    /** ADA counts strength participation per day, not per minute. */
    val didStrength: Boolean get() = strengthMin > 0
}

enum class GeneralFeeling(val label: String) {
    FRESH("Fresh"),
    NORMAL("Normal"),
    SLIGHTLY_TIRED("Slightly tired"),
    VERY_TIRED("Very tired"),
}

/** A past day shown in the Week screen's "Daily detail" list. */
data class WeekDayRecord(
    val name: String,
    val date: String,
    val level: RecoveryLevel,
    val walk: String,
    val total: String,
    val swim: String,
    val strength: String,
    val feedback: String,
)

/** One bar in the seven-day load chart. */
data class LoadBar(val day: String, val heightDp: Int, val color: Color)
