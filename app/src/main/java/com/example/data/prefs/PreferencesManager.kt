package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.fitTrackDataStore: DataStore<Preferences> by preferencesDataStore(name = "fittrack_prefs")

data class UserPreferences(
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val workoutReminderEnabled: Boolean = true,
    val mealReminderEnabled: Boolean = false,
    val weightReminderEnabled: Boolean = true,
    val weeklyReportEnabled: Boolean = true,
    val prAlertsEnabled: Boolean = true,
    val defaultRestSeconds: Int = 90,
    val githubRepo: String = ""
)

class PreferencesManager(private val context: Context) {
    private object Keys {
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val WORKOUT_REMINDER = booleanPreferencesKey("workout_reminder")
        val MEAL_REMINDER = booleanPreferencesKey("meal_reminder")
        val WEIGHT_REMINDER = booleanPreferencesKey("weight_reminder")
        val WEEKLY_REPORT = booleanPreferencesKey("weekly_report")
        val PR_ALERTS = booleanPreferencesKey("pr_alerts")
        val GITHUB_REPO = stringPreferencesKey("github_repo")
    }

    val preferencesFlow: Flow<UserPreferences> = context.fitTrackDataStore.data.map { prefs ->
        UserPreferences(
            unitSystem = UnitSystem.fromNameSafe(prefs[Keys.UNIT_SYSTEM]),
            workoutReminderEnabled = prefs[Keys.WORKOUT_REMINDER] ?: true,
            mealReminderEnabled = prefs[Keys.MEAL_REMINDER] ?: false,
            weightReminderEnabled = prefs[Keys.WEIGHT_REMINDER] ?: true,
            weeklyReportEnabled = prefs[Keys.WEEKLY_REPORT] ?: true,
            prAlertsEnabled = prefs[Keys.PR_ALERTS] ?: true,
            githubRepo = prefs[Keys.GITHUB_REPO] ?: ""
        )
    }

    suspend fun setUnitSystem(unitSystem: UnitSystem) {
        context.fitTrackDataStore.edit { it[Keys.UNIT_SYSTEM] = unitSystem.name }
    }

    suspend fun setGitHubRepo(repoSlug: String) {
        context.fitTrackDataStore.edit { it[Keys.GITHUB_REPO] = repoSlug.trim() }
    }

    suspend fun setReminderPreference(
        workout: Boolean? = null,
        meal: Boolean? = null,
        weight: Boolean? = null,
        weeklyReport: Boolean? = null,
        prAlerts: Boolean? = null
    ) {
        context.fitTrackDataStore.edit { prefs ->
            workout?.let { prefs[Keys.WORKOUT_REMINDER] = it }
            meal?.let { prefs[Keys.MEAL_REMINDER] = it }
            weight?.let { prefs[Keys.WEIGHT_REMINDER] = it }
            weeklyReport?.let { prefs[Keys.WEEKLY_REPORT] = it }
            prAlerts?.let { prefs[Keys.PR_ALERTS] = it }
        }
    }
}
