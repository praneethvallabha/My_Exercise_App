package com.recoverycoach.app.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DayRecordTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `survives a serialization round trip`() {
        val original = DayRecord.of(
            date = LocalDate.of(2026, 9, 4),
            activity = ActivityLog(morningWalkMin = 40, swimMin = 20, strengthMin = 25, steps = 9000),
            energy = 4,
            fatigue = 3,
            soreness = 2,
            generalFeeling = GeneralFeeling.FRESH,
            notes = "felt good",
            level = RecoveryLevel.NORMAL,
        )

        val encoded = json.encodeToString(DayRecord.serializer(), original)
        val restored = json.decodeFromString(DayRecord.serializer(), encoded)

        assertEquals(original, restored)
        assertEquals(LocalDate.of(2026, 9, 4), restored.date)
        assertEquals(RecoveryLevel.NORMAL, restored.level)
        assertEquals(GeneralFeeling.FRESH, restored.generalFeeling)
    }

    @Test
    fun `aerobic minutes count walking and swimming only`() {
        val record = ActivityLog(morningWalkMin = 45, swimMin = 30, strengthMin = 60)
        // Strength is real work but not aerobic minutes; counting it would
        // inflate the weekly total against an aerobic target.
        assertEquals(75, record.aerobicMinutes)
    }

    @Test
    fun `any strength minutes count as a session`() {
        assertEquals(true, ActivityLog(strengthMin = 1).didStrength)
        assertEquals(false, ActivityLog(strengthMin = 0).didStrength)
    }

    @Test
    fun `an unknown enum name falls back rather than crashing`() {
        val raw = """{"epochDay":20000,"activity":{},"energy":3,"fatigue":3,"soreness":3,""" +
            """"generalFeelingName":"REMOVED_IN_A_LATER_VERSION","notes":"","levelName":"ALSO_GONE"}"""
        val restored = json.decodeFromString(DayRecord.serializer(), raw)
        assertEquals(RecoveryLevel.NORMAL, restored.level)
        assertEquals(GeneralFeeling.NORMAL, restored.generalFeeling)
    }
}
