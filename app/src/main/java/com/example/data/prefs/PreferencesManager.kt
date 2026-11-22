package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.fitTrackDataStore: DataStore<Preferences> by preferencesDataStore(name = "fittrack_prefs")

data class UserPreferences(
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val workoutReminderEnabled: Boolean = true,
    val mealReminderEnabled: Boolean = false,
    val weightReminderEnabled: Boolean = true,
    val weeklyReportEnabled: Boolean = true,
    val prAlertsEnabled: Boolean = true,
    val workoutReminderHour: Int = 18,
    val workoutReminderMinute: Int = 0,
    val mealReminderHour: Int = 13,
    val mealReminderMinute: Int = 0,
    val weightReminderHour: Int = 7,
    val weightReminderMinute: Int = 30,
    val weeklyReportDayOfWeek: Int = Calendar.SUNDAY,
    val weeklyReportHour: Int = 18,
    val weeklyReportMinute: Int = 0,
    val defaultRestSeconds: Int = 90,
    val githubRepo: String = "",
    // Non-destructive baseline resets (full history stays preserved in Workouts & Progress)
    val volumeResetDateKey: String = "",
    val volumeResetEpochMs: Long = 0L,
    val weightTrendResetDateKey: String = "",
    val weightTrendResetEpochMs: Long = 0L
)

class PreferencesManager(private val context: Context) {
    private object Keys {
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val WORKOUT_REMINDER = booleanPreferencesKey("workout_reminder")
        val MEAL_REMINDER = booleanPreferencesKey("meal_reminder")
        val WEIGHT_REMINDER = booleanPreferencesKey("weight_reminder")
        val WEEKLY_REPORT = booleanPreferencesKey("weekly_report")
        val PR_ALERTS = booleanPreferencesKey("pr_alerts")
        val WORKOUT_HOUR = intPreferencesKey("workout_reminder_hour")
        val WORKOUT_MINUTE = intPreferencesKey("workout_reminder_minute")
        val MEAL_HOUR = intPreferencesKey("meal_reminder_hour")
        val MEAL_MINUTE = intPreferencesKey("meal_reminder_minute")
        val WEIGHT_HOUR = intPreferencesKey("weight_reminder_hour")
        val WEIGHT_MINUTE = intPreferencesKey("weight_reminder_minute")
        val WEEKLY_DAY = intPreferencesKey("weekly_report_day")
        val WEEKLY_HOUR = intPreferencesKey("weekly_report_hour")
        val WEEKLY_MINUTE = intPreferencesKey("weekly_report_minute")
        val GITHUB_REPO = stringPreferencesKey("github_repo")
        val VOLUME_RESET_DATE = stringPreferencesKey("volume_reset_date")
        val VOLUME_RESET_EPOCH = longPreferencesKey("volume_reset_epoch")
        val WEIGHT_TREND_RESET_DATE = stringPreferencesKey("weight_trend_reset_date")
        val WEIGHT_TREND_RESET_EPOCH = longPreferencesKey("weight_trend_reset_epoch")
    }

    val preferencesFlow: Flow<UserPreferences> = context.fitTrackDataStore.data.map { prefs ->
        UserPreferences(
            unitSystem = UnitSystem.fromNameSafe(prefs[Keys.UNIT_SYSTEM]),
            workoutReminderEnabled = prefs[Keys.WORKOUT_REMINDER] ?: true,
            mealReminderEnabled = prefs[Keys.MEAL_REMINDER] ?: false,
            weightReminderEnabled = prefs[Keys.WEIGHT_REMINDER] ?: true,
            weeklyReportEnabled = prefs[Keys.WEEKLY_REPORT] ?: true,
            prAlertsEnabled = prefs[Keys.PR_ALERTS] ?: true,
            workoutReminderHour = (prefs[Keys.WORKOUT_HOUR] ?: 18).coerceIn(0, 23),
            workoutReminderMinute = (prefs[Keys.WORKOUT_MINUTE] ?: 0).coerceIn(0, 59),
            mealReminderHour = (prefs[Keys.MEAL_HOUR] ?: 13).coerceIn(0, 23),
            mealReminderMinute = (prefs[Keys.MEAL_MINUTE] ?: 0).coerceIn(0, 59),
            weightReminderHour = (prefs[Keys.WEIGHT_HOUR] ?: 7).coerceIn(0, 23),
            weightReminderMinute = (prefs[Keys.WEIGHT_MINUTE] ?: 30).coerceIn(0, 59),
            weeklyReportDayOfWeek = (prefs[Keys.WEEKLY_DAY] ?: Calendar.SUNDAY).coerceIn(Calendar.SUNDAY, Calendar.SATURDAY),
            weeklyReportHour = (prefs[Keys.WEEKLY_HOUR] ?: 18).coerceIn(0, 23),
            weeklyReportMinute = (prefs[Keys.WEEKLY_MINUTE] ?: 0).coerceIn(0, 59),
            githubRepo = prefs[Keys.GITHUB_REPO] ?: "",
            volumeResetDateKey = prefs[Keys.VOLUME_RESET_DATE] ?: "",
            volumeResetEpochMs = prefs[Keys.VOLUME_RESET_EPOCH] ?: 0L,
            weightTrendResetDateKey = prefs[Keys.WEIGHT_TREND_RESET_DATE] ?: "",
            weightTrendResetEpochMs = prefs[Keys.WEIGHT_TREND_RESET_EPOCH] ?: 0L
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

    suspend fun setWorkoutReminderTime(hour: Int, minute: Int) {
        context.fitTrackDataStore.edit { prefs ->
            prefs[Keys.WORKOUT_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.WORKOUT_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setMealReminderTime(hour: Int, minute: Int) {
        context.fitTrackDataStore.edit { prefs ->
            prefs[Keys.MEAL_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.MEAL_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setWeightReminderTime(hour: Int, minute: Int) {
        context.fitTrackDataStore.edit { prefs ->
            prefs[Keys.WEIGHT_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.WEIGHT_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setWeeklyReportTime(dayOfWeek: Int, hour: Int, minute: Int) {
        context.fitTrackDataStore.edit { prefs ->
            prefs[Keys.WEEKLY_DAY] = dayOfWeek.coerceIn(Calendar.SUNDAY, Calendar.SATURDAY)
            prefs[Keys.WEEKLY_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.WEEKLY_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setVolumeReset(dateKey: String, epochMs: Long) {
        context.fitTrackDataStore.edit { prefs ->
            prefs[Keys.VOLUME_RESET_DATE] = dateKey
            prefs[Keys.VOLUME_RESET_EPOCH] = epochMs
        }
    }

    suspend fun setWeightTrendReset(dateKey: String, epochMs: Long) {
        context.fitTrackDataStore.edit { prefs ->
            prefs[Keys.WEIGHT_TREND_RESET_DATE] = dateKey
            prefs[Keys.WEIGHT_TREND_RESET_EPOCH] = epochMs
        }
    }
}
