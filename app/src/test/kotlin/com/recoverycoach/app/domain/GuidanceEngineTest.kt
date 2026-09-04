package com.recoverycoach.app.domain

import com.recoverycoach.app.data.ActivityLog
import com.recoverycoach.app.data.DayRecord
import com.recoverycoach.app.data.GeneralFeeling
import com.recoverycoach.app.data.RecoveryLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val TODAY = 20_000L

private fun day(
    offsetFromToday: Long,
    walkMin: Int = 0,
    swimMin: Int = 0,
    strengthMin: Int = 0,
    steps: Int = 10_000,
    fatigue: Int = 2,
    soreness: Int = 2,
    level: RecoveryLevel = RecoveryLevel.NORMAL,
) = DayRecord(
    epochDay = TODAY + offsetFromToday,
    activity = ActivityLog(
        morningWalkKm = 0.0,
        morningWalkMin = walkMin,
        totalWalkKm = 0.0,
        steps = steps,
        swimM = 0,
        swimMin = swimMin,
        heartPoints = 0,
        strengthMin = strengthMin,
    ),
    energy = 3,
    fatigue = fatigue,
    soreness = soreness,
    generalFeelingName = GeneralFeeling.NORMAL.name,
    notes = "",
    levelName = level.name,
)

/** A full, comfortably-compliant week: 7 days x 30 min, strength twice. */
private fun compliantWeek() = (0..6).map { back ->
    day(-back.toLong(), walkMin = 30, strengthMin = if (back == 1 || back == 3) 30 else 0)
}

class GuidanceEngineTest {

    private fun evaluate(history: List<DayRecord>) = GuidanceEngine.evaluate(history, TODAY)

    private fun tipIds(history: List<DayRecord>) = evaluate(history).tips.map { it.id }

    // ---- Cold start -------------------------------------------------------

    @Test
    fun `no history asks for a baseline rather than inventing a tip`() {
        val result = evaluate(emptyList())
        assertEquals(0, result.daysLogged)
        assertEquals(GuidanceEngine.WINDOW_DAYS, result.daysUntilFullGuidance)
        assertFalse(result.tips.any { it.id == TipId.WEEKLY_MINUTES_BEHIND })
    }

    @Test
    fun `weekly rules stay silent until a full week is logged`() {
        val partial = (0..4).map { day(-it.toLong(), walkMin = 5) }
        val ids = tipIds(partial)
        assertFalse(ids.contains(TipId.WEEKLY_MINUTES_BEHIND))
        assertFalse(ids.contains(TipId.RESISTANCE_MISSING))
        assertFalse(ids.contains(TipId.SPREAD_TOO_NARROW))
        assertEquals(2, evaluate(partial).daysUntilFullGuidance)
    }

    @Test
    fun `baseline countdown reaches zero on a full week`() {
        assertEquals(0, evaluate(compliantWeek()).daysUntilFullGuidance)
    }

    // ---- Weekly minutes ---------------------------------------------------

    @Test
    fun `falling short of 150 minutes reports the shortfall`() {
        val history = (0..6).map { day(-it.toLong(), walkMin = 10) } // 70 min
        val result = evaluate(history)
        assertEquals(70, result.weeklyAerobicMinutes)
        val tip = result.tips.find { it.id == TipId.WEEKLY_MINUTES_BEHIND }
        assertTrue(tip != null)
        assertTrue(tip!!.title.contains("80"))
        assertTrue(tip.source.contains("2026"))
    }

    @Test
    fun `meeting the target is reported as info, not a warning`() {
        val tip = evaluate(compliantWeek()).tips.find { it.id == TipId.WEEKLY_MINUTES_MET }
        // 210 minutes across the week clears the 150 target.
        assertEquals(210, evaluate(compliantWeek()).weeklyAerobicMinutes)
        if (tip != null) assertEquals(TipSeverity.INFO, tip.severity)
    }

    @Test
    fun `swim minutes count toward the aerobic total`() {
        val history = (0..6).map { day(-it.toLong(), walkMin = 10, swimMin = 15) }
        assertEquals(175, evaluate(history).weeklyAerobicMinutes)
    }

    // ---- Spread and gaps --------------------------------------------------

    @Test
    fun `hitting the target on too few days suggests spreading it`() {
        val history = listOf(
            day(0, walkMin = 80),
            day(-1, walkMin = 80),
        ) + (2..6).map { day(-it.toLong(), walkMin = 0) }
        assertTrue(tipIds(history).contains(TipId.SPREAD_TOO_NARROW))
    }

    @Test
    fun `spread tip stays quiet when activity is already spread`() {
        assertFalse(tipIds(compliantWeek()).contains(TipId.SPREAD_TOO_NARROW))
    }

    @Test
    fun `two consecutive inactive days raise attention`() {
        val history = (2..6).map { day(-it.toLong(), walkMin = 30) } +
            listOf(day(-1, walkMin = 0), day(0, walkMin = 0))
        val tip = evaluate(history).tips.find { it.id == TipId.TWO_DAY_GAP }
        assertTrue(tip != null)
        assertEquals(TipSeverity.ATTENTION, tip!!.severity)
    }

    @Test
    fun `a single rest day is not a gap`() {
        val history = (1..6).map { day(-it.toLong(), walkMin = 30) } + listOf(day(0, walkMin = 0))
        assertFalse(tipIds(history).contains(TipId.TWO_DAY_GAP))
    }

    @Test
    fun `days before the user started logging are unknown, not inactive`() {
        // Only two days exist, both active. Nothing before them should be read
        // as a gap just because no record is there.
        val history = listOf(day(-1, walkMin = 30), day(0, walkMin = 30))
        assertFalse(tipIds(history).contains(TipId.TWO_DAY_GAP))
    }

    // ---- Resistance -------------------------------------------------------

    @Test
    fun `a week without strength work suggests resistance training`() {
        val history = (0..6).map { day(-it.toLong(), walkMin = 30) }
        val tip = evaluate(history).tips.find { it.id == TipId.RESISTANCE_MISSING }
        assertTrue(tip != null)
        assertTrue(tip!!.source.contains("2026"))
    }

    @Test
    fun `two strength sessions clears the resistance tip`() {
        assertEquals(2, evaluate(compliantWeek()).strengthSessions)
        assertFalse(tipIds(compliantWeek()).contains(TipId.RESISTANCE_MISSING))
    }

    // ---- Sitting and strain ----------------------------------------------

    @Test
    fun `repeated low step days suggest breaking up sitting`() {
        val history = (0..6).map { day(-it.toLong(), walkMin = 30, steps = 3_000) }
        val tip = evaluate(history).tips.find { it.id == TipId.SITTING_BREAKS }
        assertTrue(tip != null)
        // The advice is sourced, but the step threshold is ours and says so.
        assertTrue(tip!!.source.contains("not a clinical guideline"))
    }

    @Test
    fun `sustained high fatigue and soreness is flagged as an app heuristic`() {
        val history = (0..6).map { day(-it.toLong(), walkMin = 30, fatigue = 8, soreness = 8) }
        val tip = evaluate(history).tips.find { it.id == TipId.SUSTAINED_FATIGUE }
        assertTrue(tip != null)
        assertEquals(TipSources.APP_HEURISTIC, tip!!.source)
        // It must never interpret the symptom itself.
        assertTrue(tip.body.contains("your doctor"))
    }

    @Test
    fun `high fatigue alone does not trigger the strain tip`() {
        val history = (0..6).map { day(-it.toLong(), walkMin = 30, fatigue = 9, soreness = 1) }
        assertFalse(tipIds(history).contains(TipId.SUSTAINED_FATIGUE))
    }

    // ---- Presentation rules ----------------------------------------------

    @Test
    fun `never shows more than two tips`() {
        // A deliberately bad week: no activity, no strength, low steps, high strain.
        val history = (0..6).map {
            day(-it.toLong(), walkMin = 0, steps = 500, fatigue = 9, soreness = 9)
        }
        assertEquals(GuidanceEngine.MAX_VISIBLE_TIPS, evaluate(history).tips.size)
    }

    @Test
    fun `attention outranks suggestion and info`() {
        val history = (0..6).map {
            day(-it.toLong(), walkMin = 0, steps = 500, fatigue = 9, soreness = 9)
        }
        assertTrue(evaluate(history).tips.all { it.severity == TipSeverity.ATTENTION })
    }

    @Test
    fun `every tip names a source`() {
        val histories = listOf(
            emptyList(),
            compliantWeek(),
            (0..6).map { day(-it.toLong(), walkMin = 0, steps = 500, fatigue = 9, soreness = 9) },
            (0..6).map { day(-it.toLong(), walkMin = 10) },
        )
        histories.forEach { history ->
            evaluate(history).tips.forEach { tip ->
                assertTrue("${tip.id} has no source", tip.source.isNotBlank())
            }
        }
    }

    @Test
    fun `no tip mentions medication or dosing`() {
        val banned = listOf("insulin", "dose", "dosing", "medication", "metformin", "mg")
        val histories = listOf(
            compliantWeek(),
            (0..6).map { day(-it.toLong(), walkMin = 0, steps = 500, fatigue = 9, soreness = 9) },
            (0..6).map { day(-it.toLong(), walkMin = 10) },
        )
        histories.forEach { history ->
            evaluate(history).tips.forEach { tip ->
                val text = "${tip.title} ${tip.body}".lowercase()
                banned.forEach { word ->
                    assertFalse("${tip.id} mentions '$word'", text.contains(word))
                }
            }
        }
    }

    @Test
    fun `records outside the seven day window are ignored`() {
        val history = (0..6).map { day(-it.toLong(), walkMin = 30) } +
            listOf(day(-10, walkMin = 500))
        assertEquals(210, evaluate(history).weeklyAerobicMinutes)
        assertEquals(7, evaluate(history).daysLogged)
    }
}
