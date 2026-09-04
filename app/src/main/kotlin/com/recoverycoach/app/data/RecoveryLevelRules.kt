package com.recoverycoach.app.data

/**
 * How a day's recovery level is decided. Pure so it can be tested directly, and
 * shared so a day being archived and a day being displayed cannot drift apart.
 */
object RecoveryLevelRules {
    const val RECOVERY_SORENESS = 5
    const val EASY_FATIGUE = 4
    const val EASY_LOAD_PERCENT = 15

    fun levelFor(soreness: Int, fatigue: Int, loadPercentAboveBaseline: Int): RecoveryLevel = when {
        soreness >= RECOVERY_SORENESS -> RecoveryLevel.RECOVERY
        fatigue >= EASY_FATIGUE || loadPercentAboveBaseline > EASY_LOAD_PERCENT -> RecoveryLevel.EASY
        else -> RecoveryLevel.NORMAL
    }
}
