package com.recoverycoach.app.data

/**
 * The rollover decision, kept pure so it can be tested without a device clock.
 *
 * Deliberately compares for inequality rather than "is today later". A device
 * clock that moves backwards — timezone change, manual correction, NTP sync —
 * still means the working state belongs to a different day than the one now in
 * effect, and filing it is always safer than silently overwriting it.
 */
object DayRollover {
    fun shouldRollOver(storedDayEpoch: Long?, todayEpoch: Long): Boolean =
        storedDayEpoch != null && storedDayEpoch != todayEpoch
}
