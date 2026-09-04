package com.recoverycoach.app.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

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
)

/**
 * Saves and restores the parts of [RecoveryViewModel]'s state that count as
 * "your data" — plan checkmarks, check-in answers, and logged activity.
 * The Week screen's sample trend numbers are placeholder reference data, not
 * something the user entered, so they're never persisted here.
 */
class RecoveryStore(context: Context) {
    private val dataStore = context.dataStore

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
                )
            } else {
                null
            },
        )
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
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
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
    }
}
