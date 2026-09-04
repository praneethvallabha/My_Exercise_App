package com.recoverycoach.app.data

import java.time.LocalDate

/**
 * Generates plausible history so the guidance rules can be exercised without
 * logging for a real month. Debug builds only — every call site is behind
 * `BuildConfig.DEBUG`.
 *
 * Deliberately shaped to land just under the weekly aerobic target with a single
 * strength session, so the two most useful tips both fire and the 28-day baseline
 * has enough days to activate. It is fake data by construction, which is why it
 * never runs in a release build and why "Clear all data" removes it completely.
 */
object DebugSeed {

    /** Enough days that the 28-day load baseline (which needs 14) switches on. */
    const val DAYS = 35

    /**
     * Builds [DAYS] records ending yesterday. Today is left alone — it belongs to
     * whatever the user has actually entered.
     */
    fun history(today: LocalDate): List<DayRecord> = (1..DAYS).map { daysBack ->
        val date = today.minusDays(daysBack.toLong())
        val restDay = daysBack % 7 == 0
        // The trailing week is lighter than the weeks before it, so the seeded
        // state reads as "slightly behind" rather than uniformly average.
        val walkMin = if (restDay) 0 else if (daysBack <= 7) 8 else 35
        val swimMin = if (!restDay && daysBack % 3 == 0) 40 else 0
        val strengthMin = if (!restDay && daysBack % 5 == 0) 30 else 0
        val fatigue = if (restDay) 5 else 2 + (daysBack % 3)
        val soreness = 1 + (daysBack % 3)

        DayRecord.of(
            date = date,
            activity = ActivityLog(
                morningWalkKm = if (walkMin == 0) 0.0 else (walkMin / 11.0 * 100).toInt() / 100.0,
                morningWalkMin = walkMin,
                totalWalkKm = if (restDay) 1.8 else 6.4,
                steps = if (restDay) 3_000 else 8_000 + (daysBack % 5) * 400,
                swimM = if (swimMin == 0) 0 else 800,
                swimMin = swimMin,
                heartPoints = walkMin / 2 + swimMin / 2,
                strengthMin = strengthMin,
            ),
            energy = 3,
            fatigue = fatigue,
            soreness = soreness,
            generalFeeling = if (restDay) GeneralFeeling.SLIGHTLY_TIRED else GeneralFeeling.NORMAL,
            notes = "",
            level = RecoveryLevelRules.levelFor(soreness, fatigue, loadPercentAboveBaseline = 0),
        )
    }.sortedBy { it.epochDay }
}
