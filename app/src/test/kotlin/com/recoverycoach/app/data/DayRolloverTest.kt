package com.recoverycoach.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayRolloverTest {

    @Test
    fun `same day does not roll over`() {
        assertFalse(DayRollover.shouldRollOver(storedDayEpoch = 20_000, todayEpoch = 20_000))
    }

    @Test
    fun `next day rolls over`() {
        assertTrue(DayRollover.shouldRollOver(storedDayEpoch = 20_000, todayEpoch = 20_001))
    }

    @Test
    fun `clock moved backwards still rolls over`() {
        // A timezone change or manual clock correction must not silently overwrite
        // the working day; it belongs to the day it was entered.
        assertTrue(DayRollover.shouldRollOver(storedDayEpoch = 20_001, todayEpoch = 20_000))
    }

    @Test
    fun `first ever launch does not roll over`() {
        assertFalse(DayRollover.shouldRollOver(storedDayEpoch = null, todayEpoch = 20_000))
    }

    @Test
    fun `a long gap rolls over once, not once per missed day`() {
        assertTrue(DayRollover.shouldRollOver(storedDayEpoch = 19_900, todayEpoch = 20_000))
    }
}
