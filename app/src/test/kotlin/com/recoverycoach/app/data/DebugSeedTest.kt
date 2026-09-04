package com.recoverycoach.app.data

import com.recoverycoach.app.domain.GuidanceEngine
import com.recoverycoach.app.domain.TipId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DebugSeedTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 4)
    private val seeded = DebugSeed.history(today)

    /**
     * Mirrors the ViewModel: the seed stops at yesterday, and today only joins the
     * series once the user has entered something. Weekly rules need the full seven,
     * so the tip assertions below evaluate the same list the app would.
     */
    private val withToday = seeded + DayRecord.of(
        date = today,
        activity = ActivityLog(morningWalkMin = 10, swimMin = 0, steps = 8_000, strengthMin = 0),
        energy = 3,
        fatigue = 2,
        soreness = 2,
        generalFeeling = GeneralFeeling.NORMAL,
        notes = "",
        level = RecoveryLevel.NORMAL,
    )

    @Test
    fun `produces the advertised number of days, ending yesterday`() {
        assertEquals(DebugSeed.DAYS, seeded.size)
        assertEquals(today.minusDays(1).toEpochDay(), seeded.last().epochDay)
        assertEquals(today.minusDays(DebugSeed.DAYS.toLong()).toEpochDay(), seeded.first().epochDay)
    }

    @Test
    fun `never writes today, which belongs to the user`() {
        assertFalse(seeded.any { it.epochDay >= today.toEpochDay() })
    }

    @Test
    fun `has no duplicate days`() {
        assertEquals(seeded.size, seeded.map { it.epochDay }.toSet().size)
    }

    @Test
    fun `leaves exactly today to complete the week`() {
        // The seed deliberately stops at yesterday, so one real logged day still
        // completes the window. The Settings caption tells the tester this.
        assertEquals(1, GuidanceEngine.evaluate(seeded, today.toEpochDay()).daysUntilFullGuidance)
        assertEquals(0, GuidanceEngine.evaluate(withToday, today.toEpochDay()).daysUntilFullGuidance)
    }

    @Test
    fun `lands just under the weekly target so the shortfall tip fires`() {
        val result = GuidanceEngine.evaluate(withToday, today.toEpochDay())
        assertTrue(
            "expected under ${GuidanceEngine.WEEKLY_AEROBIC_TARGET_MIN}, got ${result.weeklyAerobicMinutes}",
            result.weeklyAerobicMinutes < GuidanceEngine.WEEKLY_AEROBIC_TARGET_MIN,
        )
        assertTrue(result.tips.any { it.id == TipId.WEEKLY_MINUTES_BEHIND })
    }

    @Test
    fun `leaves too few strength sessions so the resistance tip fires`() {
        val result = GuidanceEngine.evaluate(withToday, today.toEpochDay())
        assertTrue(result.strengthSessions < GuidanceEngine.WEEKLY_STRENGTH_TARGET_SESSIONS)
        assertTrue(result.tips.any { it.id == TipId.RESISTANCE_MISSING })
    }

    @Test
    fun `spreads activity so no two-day gap is manufactured`() {
        val result = GuidanceEngine.evaluate(withToday, today.toEpochDay())
        assertFalse(result.tips.any { it.id == TipId.TWO_DAY_GAP })
        assertTrue(result.activeDays >= GuidanceEngine.MIN_ACTIVE_DAYS_PER_WEEK)
    }

    @Test
    fun `supplies enough days for the 28-day load baseline`() {
        // loadPercentAboveBaseline needs 14 records in (today-34)..(today-7).
        val inBaselineWindow = seeded.count {
            it.epochDay in (today.toEpochDay() - 34)..(today.toEpochDay() - 7)
        }
        assertTrue("only $inBaselineWindow days in the baseline window", inBaselineWindow >= 14)
    }

    @Test
    fun `is deterministic`() {
        assertEquals(seeded, DebugSeed.history(today))
    }
}
