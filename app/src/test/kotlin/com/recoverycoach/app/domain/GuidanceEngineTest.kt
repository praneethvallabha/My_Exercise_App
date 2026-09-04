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
    offset: Long,
    walkMin: Int = 0,
    swimMin: Int = 0,
    strengthMin: Int = 0,
    steps: Int = 10_000,
    fatigue: Int = 2,
    soreness: Int = 2,
) = DayRecord(
    epochDay = TODAY + offset,
    activity = ActivityLog(
        morningWalkMin = walkMin,
        steps = steps,
        swimMin = swimMin,
        strengthMin = strengthMin,
    ),
    energy = 3,
    fatigue = fatigue,
    soreness = soreness,
    generalFeelingName = GeneralFeeling.NORMAL.name,
    notes = "",
    levelName = RecoveryLevel.NORMAL.name,
)

/** A compliant week: 7 days x 30 min, strength on two nonconsecutive days. */
private fun goodWeek() = (0..6).map { back ->
    day(-back.toLong(), walkMin = 30, strengthMin = if (back == 1 || back == 4) 30 else 0)
}

class GuidanceEngineTest {

    private fun eval(history: List<DayRecord>, loadPercent: Int = 0) =
        GuidanceEngine.evaluate(history, TODAY, loadPercent)

    private fun insightIds(history: List<DayRecord>) = eval(history).insights.map { it.id }
    private fun recoveryIds(history: List<DayRecord>, loadPercent: Int = 0) =
        eval(history, loadPercent).recovery.map { it.id }

    // ---- Filter separation ------------------------------------------------

    @Test
    fun `every tip lands in the filter matching its category`() {
        val result = eval(goodWeek())
        assertTrue(result.insights.all { it.category == TipCategory.INSIGHT })
        assertTrue(result.recovery.all { it.category == TipCategory.RECOVERY })
    }

    @Test
    fun `both filters always have something to show`() {
        listOf(emptyList(), goodWeek(), (0..6).map { day(-it.toLong()) }).forEach { history ->
            assertTrue(eval(history).insights.isNotEmpty())
            assertTrue(eval(history).recovery.isNotEmpty())
        }
    }

    // ---- Cold start -------------------------------------------------------

    @Test
    fun `no history still offers educational guidance but no weekly verdict`() {
        val result = eval(emptyList())
        assertEquals(GuidanceEngine.WINDOW_DAYS, result.daysUntilFullGuidance)
        assertFalse(result.insights.any { it.id == TipId.WEEKLY_MINUTES_BEHIND })
        assertTrue(result.insights.any { it.id == TipId.POST_MEAL_WALK })
        assertTrue(result.recovery.any { it.id == TipId.SLEEP_BASELINE })
    }

    @Test
    fun `weekly rules stay silent until a full week is logged`() {
        val partial = (0..4).map { day(-it.toLong(), walkMin = 5) }
        val ids = insightIds(partial)
        assertFalse(ids.contains(TipId.WEEKLY_MINUTES_BEHIND))
        assertFalse(ids.contains(TipId.RESISTANCE_MISSING))
        assertEquals(2, eval(partial).daysUntilFullGuidance)
    }

    // ---- Insights ---------------------------------------------------------

    @Test
    fun `shortfall is reported with the gap and a per-day figure`() {
        val history = (0..6).map { day(-it.toLong(), walkMin = 10) }
        val result = eval(history)
        assertEquals(70, result.weeklyAerobicMinutes)
        val tip = result.insights.first { it.id == TipId.WEEKLY_MINUTES_BEHIND }
        assertTrue(tip.title.contains("80"))
        assertTrue(tip.source.contains("2026"))
    }

    @Test
    fun `meeting the target is info, not a nag`() {
        val tip = eval(goodWeek()).insights.first { it.id == TipId.WEEKLY_MINUTES_MET }
        assertEquals(TipSeverity.INFO, tip.severity)
        assertEquals(210, eval(goodWeek()).weeklyAerobicMinutes)
    }

    @Test
    fun `swim minutes count toward the aerobic total`() {
        assertEquals(175, eval((0..6).map { day(-it.toLong(), walkMin = 10, swimMin = 15) }).weeklyAerobicMinutes)
    }

    @Test
    fun `concentrating the week triggers the spread tip`() {
        val history = listOf(day(0, walkMin = 80), day(-1, walkMin = 80)) +
            (2..6).map { day(-it.toLong()) }
        assertTrue(insightIds(history).contains(TipId.SPREAD_TOO_NARROW))
    }

    @Test
    fun `two consecutive inactive days are the top insight`() {
        val history = (2..6).map { day(-it.toLong(), walkMin = 30) } + listOf(day(-1), day(0))
        val result = eval(history)
        assertEquals(TipId.TWO_DAY_GAP, result.insights.first().id)
        assertEquals(TipSeverity.PRIORITY, result.insights.first().severity)
    }

    @Test
    fun `a single rest day is not a gap`() {
        val history = (1..6).map { day(-it.toLong(), walkMin = 30) } + listOf(day(0))
        assertFalse(insightIds(history).contains(TipId.TWO_DAY_GAP))
    }

    @Test
    fun `days before logging started are unknown, not skipped`() {
        val history = listOf(day(-1, walkMin = 30), day(0, walkMin = 30))
        assertFalse(insightIds(history).contains(TipId.TWO_DAY_GAP))
    }

    @Test
    fun `too little strength work is flagged, enough is not`() {
        assertTrue(insightIds((0..6).map { day(-it.toLong(), walkMin = 30) }).contains(TipId.RESISTANCE_MISSING))
        assertFalse(insightIds(goodWeek()).contains(TipId.RESISTANCE_MISSING))
    }

    @Test
    fun `repeated low step days suggest breaking up sitting`() {
        val history = (0..6).map { day(-it.toLong(), walkMin = 30, steps = 3_000) }
        val tip = eval(history).insights.first { it.id == TipId.SITTING_BREAKS }
        assertTrue(tip.source.contains(TipSources.APP_RULE))
    }

    // ---- Recovery ---------------------------------------------------------

    @Test
    fun `back-to-back strength days are flagged with the 48 hour rule`() {
        val history = (0..6).map { back ->
            day(-back.toLong(), walkMin = 30, strengthMin = if (back == 2 || back == 3) 30 else 0)
        }
        val tip = eval(history).recovery.first { it.id == TipId.STRENGTH_TOO_CLOSE }
        assertTrue(tip.body.contains("${GuidanceEngine.STRENGTH_REST_HOURS} hours"))
        assertEquals(TipSources.ACSM_RECOVERY, tip.source)
    }

    @Test
    fun `nonconsecutive strength days are fine`() {
        assertFalse(recoveryIds(goodWeek()).contains(TipId.STRENGTH_TOO_CLOSE))
    }

    @Test
    fun `high soreness today surfaces a recovery tip`() {
        val history = (1..6).map { day(-it.toLong(), walkMin = 30) } + listOf(day(0, walkMin = 30, soreness = 8))
        assertTrue(recoveryIds(history).contains(TipId.HIGH_SORENESS))
    }

    @Test
    fun `sustained fatigue needs the full run of days`() {
        val sustained = (0..6).map { day(-it.toLong(), walkMin = 30, fatigue = 9) }
        assertTrue(recoveryIds(sustained).contains(TipId.SUSTAINED_FATIGUE))

        val oneBadDay = (1..6).map { day(-it.toLong(), walkMin = 30) } + listOf(day(0, walkMin = 30, fatigue = 9))
        assertFalse(recoveryIds(oneBadDay).contains(TipId.SUSTAINED_FATIGUE))
    }

    @Test
    fun `a load spike is only flagged above the threshold`() {
        assertTrue(recoveryIds(goodWeek(), loadPercent = 45).contains(TipId.LOAD_SPIKE))
        assertFalse(recoveryIds(goodWeek(), loadPercent = 10).contains(TipId.LOAD_SPIKE))
    }

    // ---- Presentation and content ----------------------------------------

    @Test
    fun `priority tips sort above suggestions and info`() {
        val history = (2..6).map { day(-it.toLong(), walkMin = 30) } + listOf(day(-1), day(0))
        listOf(eval(history).insights, eval(history).recovery).forEach { list ->
            val ordinals = list.map { it.severity.ordinal }
            assertEquals(ordinals.sortedDescending(), ordinals)
        }
    }

    @Test
    fun `every tip names a source`() {
        listOf(emptyList(), goodWeek(), (0..6).map { day(-it.toLong(), walkMin = 10, soreness = 9, fatigue = 9) })
            .forEach { history ->
                (eval(history).insights + eval(history).recovery).forEach {
                    assertTrue("${it.id} has no source", it.source.isNotBlank())
                }
            }
    }

    @Test
    fun `no tip carries medical-advice boilerplate or dosing content`() {
        val banned = listOf(
            "insulin", "dose", "dosing", "medication", "metformin",
            "not medical advice", "consult", "diagnos",
        )
        listOf(goodWeek(), (0..6).map { day(-it.toLong(), walkMin = 10, soreness = 9, fatigue = 9) })
            .forEach { history ->
                (eval(history).insights + eval(history).recovery).forEach { tip ->
                    val text = "${tip.title} ${tip.body}".lowercase()
                    banned.forEach { word ->
                        assertFalse("${tip.id} contains '$word'", text.contains(word))
                    }
                }
            }
    }

    @Test
    fun `records outside the seven day window are ignored`() {
        val history = (0..6).map { day(-it.toLong(), walkMin = 30) } + listOf(day(-10, walkMin = 500))
        assertEquals(210, eval(history).weeklyAerobicMinutes)
        assertEquals(7, eval(history).daysLogged)
    }
}
