package com.recoverycoach.app.data

import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * One finished day of real, user-entered data.
 *
 * The date is stored as a raw epoch day rather than a [LocalDate]. There is no
 * `coreLibraryDesugaring` block in this module, so keeping the serialized form a
 * primitive avoids needing a `LocalDate` adapter at all; conversion happens at
 * the UI boundary via [date].
 *
 * Enums are stored by name for the same reason — [RecoveryLevel] carries Compose
 * `Color` constructor arguments, which have no business in a persisted document.
 */
@Serializable
data class DayRecord(
    val epochDay: Long,
    val activity: ActivityLog,
    val energy: Int,
    val fatigue: Int,
    val soreness: Int,
    val generalFeelingName: String,
    val notes: String,
    val levelName: String,
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)

    val level: RecoveryLevel
        get() = RecoveryLevel.entries.find { it.name == levelName } ?: RecoveryLevel.NORMAL

    val generalFeeling: GeneralFeeling
        get() = GeneralFeeling.entries.find { it.name == generalFeelingName } ?: GeneralFeeling.NORMAL

    /** Convenience passthrough so guidance rules read naturally. */
    val aerobicMinutes: Int get() = activity.aerobicMinutes

    val didStrength: Boolean get() = activity.didStrength

    companion object {
        fun of(
            date: LocalDate,
            activity: ActivityLog,
            energy: Int,
            fatigue: Int,
            soreness: Int,
            generalFeeling: GeneralFeeling,
            notes: String,
            level: RecoveryLevel,
        ) = DayRecord(
            epochDay = date.toEpochDay(),
            activity = activity,
            energy = energy,
            fatigue = fatigue,
            soreness = soreness,
            generalFeelingName = generalFeeling.name,
            notes = notes,
            levelName = level.name,
        )
    }
}
