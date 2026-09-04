package com.recoverycoach.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "recovery_coach")

/** Everything [RecoveryStore] can load back after an app restart. Any field left null wasn't saved yet. */
data class PersistedState(
    val doneItemIds: Set<String>?,
    val energy: Int?,
    val fatigue: Int?,
    val soreness: Int?,
    val generalFeeling: GeneralFeeling?,
    val notes: String?,
    val activity: ActivityLog?,
    /** The calendar day the working state above belongs to. Null before the first run. */
    val currentDayEpoch: Long?,
    /** True once the user has saved a check-in or an activity log for [currentDayEpoch]. */
    val dayTouched: Boolean,
    val history: List<DayRecord>,
)

/**
 * Saves and restores the user's own data: today's working state (plan checkmarks,
 * check-in answers, logged activity) plus a rolling archive of finished days.
 *
 * Finished days live in one JSON document under a single key. That is comfortable
 * at [RETENTION_DAYS] days; if retention ever grows past roughly a year this should
 * move to Room rather than growing the blob.
 */
class RecoveryStore(context: Context) {
    private val dataStore = context.dataStore

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun load(): PersistedState {
        val prefs = try {
            dataStore.data.first()
        } catch (e: Exception) {
            emptyPreferences()
        }
        val feelingName = prefs[Keys.GENERAL_FEELING]
        return PersistedState(
            doneItemIds = prefs[Keys.DONE_PLAN_ITEM_IDS],
            energy = prefs[Keys.ENERGY],
            fatigue = prefs[Keys.FATIGUE],
            soreness = prefs[Keys.SORENESS],
            generalFeeling = feelingName?.let { name ->
                GeneralFeeling.entries.find { it.name == name }
            },
            notes = prefs[Keys.NOTES],
            activity = if (prefs.contains(Keys.ACTIVITY_MORNING_WALK_KM)) {
                ActivityLog(
                    morningWalkKm = prefs[Keys.ACTIVITY_MORNING_WALK_KM] ?: 0.0,
                    morningWalkMin = prefs[Keys.ACTIVITY_MORNING_WALK_MIN] ?: 0,
                    totalWalkKm = prefs[Keys.ACTIVITY_TOTAL_WALK_KM] ?: 0.0,
                    steps = prefs[Keys.ACTIVITY_STEPS] ?: 0,
                    swimM = prefs[Keys.ACTIVITY_SWIM_M] ?: 0,
                    swimMin = prefs[Keys.ACTIVITY_SWIM_MIN] ?: 0,
                    heartPoints = prefs[Keys.ACTIVITY_HEART_POINTS] ?: 0,
                    strengthMin = prefs[Keys.ACTIVITY_STRENGTH_MIN] ?: 0,
                )
            } else {
                null
            },
            currentDayEpoch = prefs[Keys.CURRENT_DAY_EPOCH],
            dayTouched = prefs[Keys.DAY_TOUCHED] ?: false,
            history = decodeHistory(prefs[Keys.HISTORY_JSON]),
        )
    }

    private fun decodeHistory(raw: String?): List<DayRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        // A corrupt or half-written blob must not brick the app; an empty archive
        // is recoverable, a crash loop on every launch is not.
        return try {
            json.decodeFromString(ListSerializer(DayRecord.serializer()), raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun savePlanDoneIds(doneIds: Set<String>) {
        dataStore.edit { it[Keys.DONE_PLAN_ITEM_IDS] = doneIds }
    }

    suspend fun saveCheckIn(energy: Int, fatigue: Int, soreness: Int, generalFeeling: GeneralFeeling, notes: String) {
        dataStore.edit {
            it[Keys.ENERGY] = energy
            it[Keys.FATIGUE] = fatigue
            it[Keys.SORENESS] = soreness
            it[Keys.GENERAL_FEELING] = generalFeeling.name
            it[Keys.NOTES] = notes
            it[Keys.DAY_TOUCHED] = true
        }
    }

    suspend fun saveActivity(activity: ActivityLog) {
        dataStore.edit {
            it[Keys.ACTIVITY_MORNING_WALK_KM] = activity.morningWalkKm
            it[Keys.ACTIVITY_MORNING_WALK_MIN] = activity.morningWalkMin
            it[Keys.ACTIVITY_TOTAL_WALK_KM] = activity.totalWalkKm
            it[Keys.ACTIVITY_STEPS] = activity.steps
            it[Keys.ACTIVITY_SWIM_M] = activity.swimM
            it[Keys.ACTIVITY_SWIM_MIN] = activity.swimMin
            it[Keys.ACTIVITY_HEART_POINTS] = activity.heartPoints
            it[Keys.ACTIVITY_STRENGTH_MIN] = activity.strengthMin
            it[Keys.DAY_TOUCHED] = true
        }
    }

    suspend fun saveCurrentDayEpoch(epochDay: Long) {
        dataStore.edit { it[Keys.CURRENT_DAY_EPOCH] = epochDay }
    }

    /**
     * Files a finished day. Idempotent on [DayRecord.epochDay], so a rollover that
     * runs twice — app killed mid-write, clock moved backwards and forwards again —
     * replaces the day rather than duplicating it.
     */
    suspend fun upsertDay(record: DayRecord) {
        dataStore.edit { prefs ->
            val merged = (decodeHistory(prefs[Keys.HISTORY_JSON]).filterNot { it.epochDay == record.epochDay } + record)
                .sortedBy { it.epochDay }
                .takeLast(RETENTION_DAYS)
            prefs[Keys.HISTORY_JSON] = json.encodeToString(ListSerializer(DayRecord.serializer()), merged)
        }
    }

    /**
     * Replaces the whole archive. Only the debug seeder uses this — normal use
     * goes through [upsertDay], which preserves what is already filed.
     */
    suspend fun replaceHistory(records: List<DayRecord>) {
        dataStore.edit { prefs ->
            val trimmed = records.sortedBy { it.epochDay }.takeLast(RETENTION_DAYS)
            prefs[Keys.HISTORY_JSON] = json.encodeToString(ListSerializer(DayRecord.serializer()), trimmed)
        }
    }

    /** Clears the working day only. The history archive is untouched. */
    suspend fun resetDayState(newDayEpoch: Long) {
        dataStore.edit {
            it.remove(Keys.DONE_PLAN_ITEM_IDS)
            it.remove(Keys.ENERGY)
            it.remove(Keys.FATIGUE)
            it.remove(Keys.SORENESS)
            it.remove(Keys.GENERAL_FEELING)
            it.remove(Keys.NOTES)
            it.remove(Keys.ACTIVITY_MORNING_WALK_KM)
            it.remove(Keys.ACTIVITY_MORNING_WALK_MIN)
            it.remove(Keys.ACTIVITY_TOTAL_WALK_KM)
            it.remove(Keys.ACTIVITY_STEPS)
            it.remove(Keys.ACTIVITY_SWIM_M)
            it.remove(Keys.ACTIVITY_SWIM_MIN)
            it.remove(Keys.ACTIVITY_HEART_POINTS)
            it.remove(Keys.ACTIVITY_STRENGTH_MIN)
            it[Keys.DAY_TOUCHED] = false
            it[Keys.CURRENT_DAY_EPOCH] = newDayEpoch
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    companion object {
        /** Rolling archive length. Comfortable as a single JSON document. */
        const val RETENTION_DAYS = 90
    }

    private object Keys {
        val DONE_PLAN_ITEM_IDS = stringSetPreferencesKey("done_plan_item_ids")
        val ENERGY = intPreferencesKey("energy")
        val FATIGUE = intPreferencesKey("fatigue")
        val SORENESS = intPreferencesKey("soreness")
        val GENERAL_FEELING = stringPreferencesKey("general_feeling")
        val NOTES = stringPreferencesKey("notes")
        val ACTIVITY_MORNING_WALK_KM = doublePreferencesKey("activity_morning_walk_km")
        val ACTIVITY_MORNING_WALK_MIN = intPreferencesKey("activity_morning_walk_min")
        val ACTIVITY_TOTAL_WALK_KM = doublePreferencesKey("activity_total_walk_km")
        val ACTIVITY_STEPS = intPreferencesKey("activity_steps")
        val ACTIVITY_SWIM_M = intPreferencesKey("activity_swim_m")
        val ACTIVITY_SWIM_MIN = intPreferencesKey("activity_swim_min")
        val ACTIVITY_HEART_POINTS = intPreferencesKey("activity_heart_points")
        val ACTIVITY_STRENGTH_MIN = intPreferencesKey("activity_strength_min")
        val CURRENT_DAY_EPOCH = longPreferencesKey("current_day_epoch")
        val DAY_TOUCHED = booleanPreferencesKey("day_touched")
        val HISTORY_JSON = stringPreferencesKey("history_json")
    }
}
