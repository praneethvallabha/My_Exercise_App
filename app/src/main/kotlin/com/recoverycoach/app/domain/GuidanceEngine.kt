package com.recoverycoach.app.domain

import com.recoverycoach.app.data.DayRecord

/**
 * What [GuidanceEngine.evaluate] produces for a given day.
 *
 * [daysUntilFullGuidance] drives the cold-start state: until enough real days
 * have accumulated, the UI says how many are still needed rather than inventing
 * a tip from a week that does not exist yet.
 */
data class GuidanceResult(
    val daysLogged: Int,
    val weeklyAerobicMinutes: Int,
    val weeklyAerobicTargetMinutes: Int,
    val activeDays: Int,
    val strengthSessions: Int,
    val tips: List<Tip>,
    val daysUntilFullGuidance: Int,
)

/**
 * Turns accumulated [DayRecord]s into a short list of sourced, actionable tips.
 *
 * Pure logic on purpose: no Android imports, no clock reads, no I/O. `today` is
 * passed in so every rule is deterministic and testable.
 */
object GuidanceEngine {

    /** ADA 2026: at least 150 min/week moderate-to-vigorous aerobic activity. */
    const val WEEKLY_AEROBIC_TARGET_MIN = 150

    /** ADA 2026: activity spread over at least 3 days per week. */
    const val MIN_ACTIVE_DAYS_PER_WEEK = 3

    /** ADA 2026: never more than 2 consecutive days without activity. */
    const val MAX_CONSECUTIVE_INACTIVE_DAYS = 2

    /** ADA 2026: 2-3 resistance sessions per week on nonconsecutive days. */
    const val WEEKLY_STRENGTH_TARGET_SESSIONS = 2

    /**
     * An app heuristic, not a guideline. Published sedentary-behaviour guidance
     * is directional ("sit less") rather than numeric, so this threshold is ours
     * and is labelled as such wherever the tip appears.
     */
    const val LOW_STEP_DAY_THRESHOLD = 6_000

    /** Ditto — the fatigue/soreness pair that suggests a sustained hard patch. */
    const val SUSTAINED_STRAIN_SCORE = 7
    const val SUSTAINED_STRAIN_DAYS = 3

    const val WINDOW_DAYS = 7
    private const val SHORT_WINDOW_DAYS = 3

    fun evaluate(history: List<DayRecord>, todayEpochDay: Long): GuidanceResult {
        val window = history
            .filter { it.epochDay in (todayEpochDay - WINDOW_DAYS + 1)..todayEpochDay }
            .sortedBy { it.epochDay }

        val daysLogged = window.size
        val weeklyAerobic = window.sumOf { it.aerobicMinutes }
        val activeDays = window.count { it.aerobicMinutes > 0 }
        val strengthSessions = window.count { it.didStrength }

        val tips = buildList {
            twoDayGap(window, todayEpochDay)?.let(::add)
            sustainedStrain(window)?.let(::add)
            weeklyMinutes(window, weeklyAerobic)?.let(::add)
            spread(window, weeklyAerobic, activeDays)?.let(::add)
            resistance(window, strengthSessions)?.let(::add)
            sittingBreaks(window)?.let(::add)
            add(postMealWalk())
        }

        return GuidanceResult(
            daysLogged = daysLogged,
            weeklyAerobicMinutes = weeklyAerobic,
            weeklyAerobicTargetMinutes = WEEKLY_AEROBIC_TARGET_MIN,
            activeDays = activeDays,
            strengthSessions = strengthSessions,
            tips = rank(tips, todayEpochDay),
            daysUntilFullGuidance = (WINDOW_DAYS - daysLogged).coerceAtLeast(0),
        )
    }

    /**
     * At most two tips on screen. Severity decides first; within a severity the
     * order rotates by date so a single tip cannot pin the card forever.
     */
    private fun rank(tips: List<Tip>, todayEpochDay: Long): List<Tip> {
        if (tips.isEmpty()) return emptyList()
        return tips
            .sortedWith(
                compareByDescending<Tip> { it.severity.ordinal }
                    .thenBy { (it.id.ordinal + todayEpochDay) % tips.size },
            )
            .take(MAX_VISIBLE_TIPS)
    }

    const val MAX_VISIBLE_TIPS = 2

    // ---- Rules ------------------------------------------------------------

    private fun weeklyMinutes(window: List<DayRecord>, weeklyAerobic: Int): Tip? {
        if (window.size < WINDOW_DAYS) return null
        return if (weeklyAerobic < WEEKLY_AEROBIC_TARGET_MIN) {
            Tip(
                id = TipId.WEEKLY_MINUTES_BEHIND,
                title = "${WEEKLY_AEROBIC_TARGET_MIN - weeklyAerobic} min short this week",
                body = "You logged $weeklyAerobic of $WEEKLY_AEROBIC_TARGET_MIN minutes. " +
                    "Guidance for adults with type 2 diabetes is at least $WEEKLY_AEROBIC_TARGET_MIN minutes " +
                    "of moderate activity a week. Intensity is not measured here, so this counts every " +
                    "logged walk and swim minute as moderate.",
                severity = TipSeverity.SUGGESTION,
                source = TipSources.ADA_2026,
            )
        } else {
            Tip(
                id = TipId.WEEKLY_MINUTES_MET,
                title = "Weekly target met",
                body = "$weeklyAerobic minutes logged against a $WEEKLY_AEROBIC_TARGET_MIN minute target. " +
                    "Benefit keeps accruing above this, but there is no need to chase a bigger number.",
                severity = TipSeverity.INFO,
                source = "${TipSources.ADA_2026}; ${TipSources.WHO_2020}",
            )
        }
    }

    private fun spread(window: List<DayRecord>, weeklyAerobic: Int, activeDays: Int): Tip? {
        if (window.size < WINDOW_DAYS) return null
        if (weeklyAerobic < WEEKLY_AEROBIC_TARGET_MIN) return null
        if (activeDays >= MIN_ACTIVE_DAYS_PER_WEEK) return null
        return Tip(
            id = TipId.SPREAD_TOO_NARROW,
            title = "Try spreading it wider",
            body = "You hit the weekly minutes across only $activeDays " +
                "${if (activeDays == 1) "day" else "days"}. Guidance is to spread activity over at " +
                "least $MIN_ACTIVE_DAYS_PER_WEEK days a week rather than concentrating it.",
            severity = TipSeverity.SUGGESTION,
            source = TipSources.ADA_2026,
        )
    }

    /**
     * Scans real calendar days, so a day with no record counts as inactive —
     * but only from the first logged day onward. Days before the user started
     * logging are unknown, not inactive.
     */
    private fun twoDayGap(window: List<DayRecord>, todayEpochDay: Long): Tip? {
        val first = window.firstOrNull() ?: return null
        val activeDays = window.filter { it.aerobicMinutes > 0 }.map { it.epochDay }.toSet()

        var run = 0
        for (day in first.epochDay..todayEpochDay) {
            run = if (day in activeDays) 0 else run + 1
        }
        if (run < MAX_CONSECUTIVE_INACTIVE_DAYS) return null

        return Tip(
            id = TipId.TWO_DAY_GAP,
            title = "$run days without logged activity",
            body = "Guidance is to go no more than $MAX_CONSECUTIVE_INACTIVE_DAYS consecutive days " +
                "without activity. Even a short easy walk restarts the clock.",
            severity = TipSeverity.ATTENTION,
            source = TipSources.ADA_2026,
        )
    }

    private fun resistance(window: List<DayRecord>, strengthSessions: Int): Tip? {
        if (window.size < WINDOW_DAYS) return null
        if (strengthSessions >= WEEKLY_STRENGTH_TARGET_SESSIONS) return null
        return Tip(
            id = TipId.RESISTANCE_MISSING,
            title = "No strength work logged",
            body = "Guidance is $WEEKLY_STRENGTH_TARGET_SESSIONS-3 resistance sessions a week on " +
                "nonconsecutive days. Combined aerobic and resistance work improves glucose control " +
                "more than either on its own.",
            severity = TipSeverity.SUGGESTION,
            source = "${TipSources.ADA_2026}; ${TipSources.ADA_POSITION_2016}",
        )
    }

    private fun sittingBreaks(window: List<DayRecord>): Tip? {
        if (window.size < SHORT_WINDOW_DAYS) return null
        val recent = window.takeLast(SHORT_WINDOW_DAYS)
        val lowStepDays = recent.count { it.activity.steps < LOW_STEP_DAY_THRESHOLD }
        if (lowStepDays < 2) return null
        return Tip(
            id = TipId.SITTING_BREAKS,
            title = "Long sitting stretches likely",
            body = "Step counts have been low on $lowStepDays of the last $SHORT_WINDOW_DAYS days. " +
                "Breaking up long sitting with short walks lowers post-meal glucose — walking breaks " +
                "work better than simply standing, and the effect is largest in type 2 diabetes.",
            severity = TipSeverity.SUGGESTION,
            source = "${TipSources.SITTING_META_2025}. Low-step threshold: ${TipSources.APP_HEURISTIC}",
        )
    }

    private fun postMealWalk() = Tip(
        id = TipId.POST_MEAL_WALK,
        title = "Short walks after meals",
        body = "Three 15-minute walks after meals improved 24-hour glucose control more than one " +
            "45-minute walk in a trial of older adults at risk of impaired glucose tolerance. " +
            "Timing matters as much as total duration.",
        severity = TipSeverity.INFO,
        source = TipSources.POSTMEAL_2013,
    )

    /**
     * This one is ours, not a clinical rule, and says so. It never interprets a
     * symptom — it points at a clinician and stops.
     */
    private fun sustainedStrain(window: List<DayRecord>): Tip? {
        if (window.size < SUSTAINED_STRAIN_DAYS) return null
        val recent = window.takeLast(SUSTAINED_STRAIN_DAYS)
        val allStrained = recent.all {
            it.fatigue >= SUSTAINED_STRAIN_SCORE && it.soreness >= SUSTAINED_STRAIN_SCORE
        }
        if (!allStrained) return null
        return Tip(
            id = TipId.SUSTAINED_FATIGUE,
            title = "Fatigue and soreness both high for $SUSTAINED_STRAIN_DAYS days",
            body = "That is a longer stretch than usual. If it does not settle with easier days, " +
                "it is worth raising with your doctor. This app does not interpret symptoms.",
            severity = TipSeverity.ATTENTION,
            source = TipSources.APP_HEURISTIC,
        )
    }
}
