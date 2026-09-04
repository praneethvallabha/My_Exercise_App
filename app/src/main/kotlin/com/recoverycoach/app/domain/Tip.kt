package com.recoverycoach.app.domain

/** Which filter a tip belongs to on the Insights screen. */
enum class TipCategory { INSIGHT, RECOVERY }

/** How prominently a tip presents itself. Ordering only — nothing here is a warning. */
enum class TipSeverity { INFO, SUGGESTION, PRIORITY }

enum class TipId {
    // Insights — your workouts measured against published guidance.
    WEEKLY_MINUTES_BEHIND,
    WEEKLY_MINUTES_MET,
    TWO_DAY_GAP,
    SPREAD_TOO_NARROW,
    RESISTANCE_MISSING,
    SITTING_BREAKS,
    POST_MEAL_WALK,

    // Recovery — how to come back from the work you did.
    STRENGTH_TOO_CLOSE,
    HIGH_SORENESS,
    SUSTAINED_FATIGUE,
    LOAD_SPIKE,
    VARY_THE_STIMULUS,
    SLEEP_BASELINE,
}

/**
 * One piece of guidance.
 *
 * [source] is the feature, not decoration: every tip names the body and year it
 * came from so it can be checked. Tips derived from this app's own thresholds say
 * so rather than borrowing someone else's authority.
 */
data class Tip(
    val id: TipId,
    val category: TipCategory,
    val title: String,
    val body: String,
    val severity: TipSeverity,
    val source: String,
)

/** Citations, in one place so a tip can never drift from its source. */
object TipSources {
    const val ADA_2026 = "ADA Standards of Care in Diabetes, 2026"
    const val ADA_POSITION_2016 = "ADA Position Statement, Diabetes Care 39(11), 2016"
    const val WHO_2020 = "WHO Physical Activity & Sedentary Behaviour Guidelines, 2020"
    const val SITTING_META_2025 = "Gale et al., Obesity Reviews, 2025 — 39-study meta-analysis"
    const val POSTMEAL_2013 = "DiPietro et al., Diabetes Care 36(10), 2013"
    const val ACSM_RECOVERY = "American College of Sports Medicine — muscle recovery guidance"
    const val AASM_2015 = "AASM & Sleep Research Society consensus, J Clin Sleep Med, 2015"

    /** For rules built on this app's own thresholds rather than published guidance. */
    const val APP_RULE = "This app's own threshold"
}
