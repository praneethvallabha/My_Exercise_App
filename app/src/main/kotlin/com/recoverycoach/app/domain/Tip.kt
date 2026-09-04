package com.recoverycoach.app.domain

/**
 * How loudly a tip should present itself. Nothing here is a diagnosis; the
 * strongest level still only ever suggests raising something with a clinician.
 */
enum class TipSeverity { INFO, SUGGESTION, ATTENTION }

enum class TipId {
    WEEKLY_MINUTES_BEHIND,
    WEEKLY_MINUTES_MET,
    TWO_DAY_GAP,
    SPREAD_TOO_NARROW,
    RESISTANCE_MISSING,
    SITTING_BREAKS,
    POST_MEAL_WALK,
    SUSTAINED_FATIGUE,
}

/**
 * One piece of guidance shown to the user.
 *
 * [source] is not optional decoration. Every tip carrying a number must name the
 * body and year it came from, so the user can check it. Tips derived from this
 * app's own thresholds rather than published guidance say so in [source] instead
 * of borrowing someone else's authority.
 */
data class Tip(
    val id: TipId,
    val title: String,
    val body: String,
    val severity: TipSeverity,
    val source: String,
)

/** Citations, kept in one place so a tip can never drift from its source. */
object TipSources {
    const val ADA_2026 = "American Diabetes Association, Standards of Care in Diabetes, 2026"
    const val ADA_POSITION_2016 = "ADA Position Statement, Diabetes Care 39(11), 2016"
    const val WHO_2020 = "WHO Guidelines on Physical Activity and Sedentary Behaviour, 2020"
    const val SITTING_META_2025 = "Gale et al., Obesity Reviews, 2025 (meta-analysis of 39 studies)"
    const val POSTMEAL_2013 = "DiPietro et al., Diabetes Care 36(10), 2013"

    /** For rules that come from this app's own thresholds, not published guidance. */
    const val APP_HEURISTIC = "This app's own threshold — not a clinical guideline"
}
