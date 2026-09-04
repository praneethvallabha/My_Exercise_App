package com.recoverycoach.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecoveryLevelRulesTest {

    @Test
    fun `soreness at the threshold forces a recovery day`() {
        assertEquals(RecoveryLevel.RECOVERY, RecoveryLevelRules.levelFor(soreness = 5, fatigue = 0, loadPercentAboveBaseline = 0))
    }

    @Test
    fun `soreness outranks everything else`() {
        assertEquals(RecoveryLevel.RECOVERY, RecoveryLevelRules.levelFor(soreness = 9, fatigue = 0, loadPercentAboveBaseline = 0))
    }

    @Test
    fun `fatigue at the threshold gives an easy day`() {
        assertEquals(RecoveryLevel.EASY, RecoveryLevelRules.levelFor(soreness = 0, fatigue = 4, loadPercentAboveBaseline = 0))
    }

    @Test
    fun `a load spike alone gives an easy day`() {
        assertEquals(RecoveryLevel.EASY, RecoveryLevelRules.levelFor(soreness = 0, fatigue = 0, loadPercentAboveBaseline = 16))
    }

    @Test
    fun `load at the threshold is not yet a spike`() {
        assertEquals(RecoveryLevel.NORMAL, RecoveryLevelRules.levelFor(soreness = 0, fatigue = 0, loadPercentAboveBaseline = 15))
    }

    @Test
    fun `nothing triggered is a normal day`() {
        assertEquals(RecoveryLevel.NORMAL, RecoveryLevelRules.levelFor(soreness = 2, fatigue = 2, loadPercentAboveBaseline = 5))
    }

    @Test
    fun `archiving a day passes zero load, so only soreness and fatigue decide it`() {
        // Regression: the archived level used to be computed with the load percent
        // of whatever day `currentDay` happened to hold, not the day being filed.
        // With zero load the result must depend on nothing but the day's own answers.
        assertEquals(RecoveryLevel.NORMAL, RecoveryLevelRules.levelFor(soreness = 3, fatigue = 3, loadPercentAboveBaseline = 0))
        assertEquals(RecoveryLevel.EASY, RecoveryLevelRules.levelFor(soreness = 3, fatigue = 6, loadPercentAboveBaseline = 0))
        assertEquals(RecoveryLevel.RECOVERY, RecoveryLevelRules.levelFor(soreness = 7, fatigue = 6, loadPercentAboveBaseline = 0))
    }
}
