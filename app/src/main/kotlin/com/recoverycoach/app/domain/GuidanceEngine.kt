package com.recoverycoach.app.domain

import com.recoverycoach.app.data.DayRecord

/**
 * What [GuidanceEngine.evaluate] produces for a given day, split by the two
 * filters on the Insights screen.
 */
data class GuidanceResult(
    val daysLogged: Int,
    val weeklyAerobicMinutes: Int,
    val weeklyAerobicTargetMinutes: Int,
    val activeDays: Int,
    val strengthSessions: Int,
    val weeklyStrengthTarget: Int,
    val daysUntilFullGuidance: Int,
    val insights: List<Tip>,
    val recovery: List<Tip>,
)

/**
 * Turns logged days into sourced, actionable guidance.
 *
 * Pure logic: no Android imports, no clock reads, no I/O. `today` is passed in so
 * every rule is deterministic and testable.
 */
object GuidanceEngine {

    /** ADA 2026: at least 150 min/week moderate-to-vigorous aerobic activity. */
    const val WEEKLY_AEROBIC_TARGET_MIN = 150

    /** ADA 2026: spread over at least 3 days per week. */
    const val MIN_ACTIVE_DAYS_PER_WEEK = 3

    /** ADA 2026: never more than 2 consecutive days without activity. */
    const val MAX_CONSECUTIVE_INACTIVE_DAYS = 2

    /** ADA 2026: 2-3 resistance sessions per week on nonconsecutive days. */
    const val WEEKLY_STRENGTH_TARGET_SESSIONS = 2

    /** ACSM: at least 48 hours before working the same muscle group again. */
    const val STRENGTH_REST_HOURS = 48

    /** AASM/SRS consensus: 7 or more hours a night for adults. */
    const val SLEEP_TARGET_HOURS = 7

    /** Ours, not a guideline — where "sore enough to ease off" sits on the 0-10 scale. */
    const val HIGH_SORENESS = 6
    const val SUSTAINED_FATIGUE = 7
    const val SUSTAINED_FATIGUE_DAYS = 3

    /** Ours — step count below which a day reads as mostly sitting. */
    const val LOW_STEP_DAY = 6_000

    /** Ours — how far above the 28-day baseline counts as a spike. */
    const val LOAD_SPIKE_PERCENT = 30

    const val WINDOW_DAYS = 7
    private const val SHORT_WINDOW_DAYS = 3

    fun evaluate(history: List<DayRecord>, todayEpochDay: Long, loadPercentAboveBaseline: Int = 0): GuidanceResult {
        val window = history
            .filter { it.epochDay in (todayEpochDay - WINDOW_DAYS + 1)..todayEpochDay }
            .sortedBy { it.epochDay }

        val weeklyAerobic = window.sumOf { it.aerobicMinutes }
        val activeDays = window.count { it.aerobicMinutes > 0 }
        val strengthSessions = window.count { it.didStrength }

        val insights = buildList {
            twoDayGap(window, todayEpochDay)?.let(::add)
            weeklyMinutes(window, weeklyAerobic)?.let(::add)
            spread(window, weeklyAerobic, activeDays)?.let(::add)
            resistance(window, strengthSessions)?.let(::add)
            sittingBreaks(window)?.let(::add)
            add(postMealWalk())
        }

        val recovery = buildList {
            strengthTooClose(window)?.let(::add)
            highSoreness(window)?.let(::add)
            sustainedFatigue(window)?.let(::add)
            loadSpike(loadPercentAboveBaseline)?.let(::add)
            add(varyTheStimulus())
            add(sleepBaseline())
        }

        return GuidanceResult(
            daysLogged = window.size,
            weeklyAerobicMinutes = weeklyAerobic,
            weeklyAerobicTargetMinutes = WEEKLY_AEROBIC_TARGET_MIN,
            activeDays = activeDays,
            strengthSessions = strengthSessions,
            weeklyStrengthTarget = WEEKLY_STRENGTH_TARGET_SESSIONS,
            daysUntilFullGuidance = (WINDOW_DAYS - window.size).coerceAtLeast(0),
            insights = insights.sortedByDescending { it.severity.ordinal },
            recovery = recovery.sortedByDescending { it.severity.ordinal },
        )
    }

    // ---- Insights ---------------------------------------------------------

    private fun weeklyMinutes(window: List<DayRecord>, weeklyAerobic: Int): Tip? {
        if (window.size < WINDOW_DAYS) return null
        return if (weeklyAerobic < WEEKLY_AEROBIC_TARGET_MIN) {
            Tip(
                id = TipId.WEEKLY_MINUTES_BEHIND,
                category = TipCategory.INSIGHT,
                title = "${WEEKLY_AEROBIC_TARGET_MIN - weeklyAerobic} minutes short this week",
                body = "You logged $weeklyAerobic of $WEEKLY_AEROBIC_TARGET_MIN minutes. Roughly " +
                    "${((WEEKLY_AEROBIC_TARGET_MIN - weeklyAerobic) / 7.0).toInt() + 1} extra minutes " +
                    "a day closes it. Walk and swim minutes are counted as moderate — intensity is not measured.",
                severity = TipSeverity.SUGGESTION,
                source = TipSources.ADA_2026,
            )
        } else {
            Tip(
                id = TipId.WEEKLY_MINUTES_MET,
                category = TipCategory.INSIGHT,
                title = "Weekly target cleared",
                body = "$weeklyAerobic minutes against a $WEEKLY_AEROBIC_TARGET_MIN minute target. " +
                    "Benefit keeps accruing up to about 300 minutes, but there is nothing to chase here.",
                severity = TipSeverity.INFO,
                source = "${TipSources.ADA_2026} · ${TipSources.WHO_2020}",
            )
        }
    }

    private fun spread(window: List<DayRecord>, weeklyAerobic: Int, activeDays: Int): Tip? {
        if (window.size < WINDOW_DAYS || weeklyAerobic < WEEKLY_AEROBIC_TARGET_MIN) return null
        if (activeDays >= MIN_ACTIVE_DAYS_PER_WEEK) return null
        return Tip(
            id = TipId.SPREAD_TOO_NARROW,
            category = TipCategory.INSIGHT,
            title = "All of it landed on $activeDays ${if (activeDays == 1) "day" else "days"}",
            body = "You hit the minutes, but concentrated. Spreading the same volume over at least " +
                "$MIN_ACTIVE_DAYS_PER_WEEK days gives better glucose control than stacking it.",
            severity = TipSeverity.SUGGESTION,
            source = TipSources.ADA_2026,
        )
    }

    /**
     * Counts real calendar days, so a day with no record is inactive — but only
     * from the first logged day onward. Days before you started logging are
     * unknown, not skipped.
     */
    private fun twoDayGap(window: List<DayRecord>, todayEpochDay: Long): Tip? {
        val first = window.firstOrNull() ?: return null
        val active = window.filter { it.aerobicMinutes > 0 }.map { it.epochDay }.toSet()
        var run = 0
        for (day in first.epochDay..todayEpochDay) {
            run = if (day in active) 0 else run + 1
        }
        if (run < MAX_CONSECUTIVE_INACTIVE_DAYS) return null
        return Tip(
            id = TipId.TWO_DAY_GAP,
            category = TipCategory.INSIGHT,
            title = "$run days without logged activity",
            body = "Guidance is no more than $MAX_CONSECUTIVE_INACTIVE_DAYS consecutive days off. " +
                "A short easy walk today restarts the clock.",
            severity = TipSeverity.PRIORITY,
            source = TipSources.ADA_2026,
        )
    }

    private fun resistance(window: List<DayRecord>, strengthSessions: Int): Tip? {
        if (window.size < WINDOW_DAYS || strengthSessions >= WEEKLY_STRENGTH_TARGET_SESSIONS) return null
        return Tip(
            id = TipId.RESISTANCE_MISSING,
            category = TipCategory.INSIGHT,
            title = if (strengthSessions == 0) "No strength work this week" else "One strength session this week",
            body = "Target is $WEEKLY_STRENGTH_TARGET_SESSIONS-3 sessions a week on nonconsecutive days. " +
                "Aerobic plus resistance beats either alone for glucose control, blood pressure and strength.",
            severity = TipSeverity.SUGGESTION,
            source = "${TipSources.ADA_2026} · ${TipSources.ADA_POSITION_2016}",
        )
    }

    private fun sittingBreaks(window: List<DayRecord>): Tip? {
        if (window.size < SHORT_WINDOW_DAYS) return null
        val recent = window.takeLast(SHORT_WINDOW_DAYS)
        val lowDays = recent.count { it.activity.steps < LOW_STEP_DAY }
        if (lowDays < 2) return null
        return Tip(
            id = TipId.SITTING_BREAKS,
            category = TipCategory.INSIGHT,
            title = "Low step days: $lowDays of the last $SHORT_WINDOW_DAYS",
            body = "Breaking long sitting with short walks lowers post-meal glucose. Walking breaks beat " +
                "standing, and the effect is largest in type 2 diabetes.",
            severity = TipSeverity.SUGGESTION,
            source = "${TipSources.SITTING_META_2025} · threshold: ${TipSources.APP_RULE}",
        )
    }

    private fun postMealWalk() = Tip(
        id = TipId.POST_MEAL_WALK,
        category = TipCategory.INSIGHT,
        title = "Time walks to your meals",
        body = "Three 15-minute walks after meals improved 24-hour glucose control more than a single " +
            "45-minute walk. Same total time, better result.",
        severity = TipSeverity.INFO,
        source = TipSources.POSTMEAL_2013,
    )

    // ---- Recovery ---------------------------------------------------------

    private fun strengthTooClose(window: List<DayRecord>): Tip? {
        val strengthDays = window.filter { it.didStrength }.map { it.epochDay }.sorted()
        val backToBack = strengthDays.zipWithNext().any { (a, b) -> b - a == 1L }
        if (!backToBack) return null
        return Tip(
            id = TipId.STRENGTH_TOO_CLOSE,
            category = TipCategory.RECOVERY,
            title = "Strength sessions on back-to-back days",
            body = "Leave at least $STRENGTH_REST_HOURS hours before working the same muscle group again. " +
                "If you want consecutive days, alternate what you train.",
            severity = TipSeverity.PRIORITY,
            source = TipSources.ACSM_RECOVERY,
        )
    }

    private fun highSoreness(window: List<DayRecord>): Tip? {
        val latest = window.lastOrNull() ?: return null
        if (latest.soreness < HIGH_SORENESS) return null
        return Tip(
            id = TipId.HIGH_SORENESS,
            category = TipCategory.RECOVERY,
            title = "Soreness at ${latest.soreness}/10",
            body = "Delayed soreness usually peaks 24-48 hours after a harder session and settles on its " +
                "own. Keep moving — easy walking or swimming rather than a repeat of what caused it.",
            severity = TipSeverity.PRIORITY,
            source = "${TipSources.ACSM_RECOVERY} · threshold: ${TipSources.APP_RULE}",
        )
    }

    private fun sustainedFatigue(window: List<DayRecord>): Tip? {
        if (window.size < SUSTAINED_FATIGUE_DAYS) return null
        val recent = window.takeLast(SUSTAINED_FATIGUE_DAYS)
        if (!recent.all { it.fatigue >= SUSTAINED_FATIGUE }) return null
        return Tip(
            id = TipId.SUSTAINED_FATIGUE,
            category = TipCategory.RECOVERY,
            title = "Fatigue above $SUSTAINED_FATIGUE for $SUSTAINED_FATIGUE_DAYS days running",
            body = "That is a longer stretch than a normal training response. Pull the next few days back " +
                "to easy volume and let it reset before pushing again.",
            severity = TipSeverity.PRIORITY,
            source = TipSources.APP_RULE,
        )
    }

    private fun loadSpike(loadPercentAboveBaseline: Int): Tip? {
        if (loadPercentAboveBaseline <= LOAD_SPIKE_PERCENT) return null
        return Tip(
            id = TipId.LOAD_SPIKE,
            category = TipCategory.RECOVERY,
            title = "Load is $loadPercentAboveBaseline% above your baseline",
            body = "A jump this size is where niggles start. Hold this volume for a week before adding " +
                "more, rather than stacking another increase on top.",
            severity = TipSeverity.SUGGESTION,
            source = TipSources.APP_RULE,
        )
    }

    private fun varyTheStimulus() = Tip(
        id = TipId.VARY_THE_STIMULUS,
        category = TipCategory.RECOVERY,
        title = "Rotate what you train",
        body = "Avoid the same type of session on consecutive days. Alternating muscle groups or swapping " +
            "walking for swimming lets tissue rebuild while you still train.",
        severity = TipSeverity.INFO,
        source = TipSources.ACSM_RECOVERY,
    )

    private fun sleepBaseline() = Tip(
        id = TipId.SLEEP_BASELINE,
        category = TipCategory.RECOVERY,
        title = "$SLEEP_TARGET_HOURS+ hours is the recovery floor",
        body = "Sleep is where the adaptation actually happens. The adult consensus is $SLEEP_TARGET_HOURS " +
            "or more hours a night on a regular basis — short nights blunt everything else you do here.",
        severity = TipSeverity.INFO,
        source = TipSources.AASM_2015,
    )
}
