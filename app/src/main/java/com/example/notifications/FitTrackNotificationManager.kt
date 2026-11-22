package com.example.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.prefs.UserPreferences
import java.util.Calendar
import java.util.Locale

enum class FitTrackReminderType(
    val requestCode: Int,
    val notificationId: Int,
    val title: String,
    val body: String,
    val defaultHourOfDay: Int,
    val defaultMinute: Int,
    val isWeekly: Boolean = false
) {
    WORKOUT(
        requestCode = 1001,
        notificationId = 2001,
        title = "Time to Train",
        body = "Your Gym & Home routines are ready in FitTrack. Keep your streak alive!",
        defaultHourOfDay = 18,
        defaultMinute = 0
    ),
    MEAL(
        requestCode = 1002,
        notificationId = 2002,
        title = "Log Your Meals & Water",
        body = "Track today's calories, protein, and hydration to keep your Daily Score climbing.",
        defaultHourOfDay = 13,
        defaultMinute = 0
    ),
    WEIGHT(
        requestCode = 1003,
        notificationId = 2003,
        title = "Morning Weigh-In Check",
        body = "Step on the scale or check your calorie-estimated weight trend in FitTrack.",
        defaultHourOfDay = 7,
        defaultMinute = 30
    ),
    WEEKLY_REPORT(
        requestCode = 1004,
        notificationId = 2004,
        title = "Weekly Progress Summary Ready",
        body = "Review your 7-day weight trajectory, strength volume, and nutrition consistency.",
        defaultHourOfDay = 18,
        defaultMinute = 0,
        isWeekly = true
    );

    companion object {
        fun fromNameSafe(name: String?): FitTrackReminderType? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

/**
 * Play-Policy-compliant Android notification manager and customizable inexact repeating reminder scheduler.
 * Uses POST_NOTIFICATIONS runtime checks (Android 13+) and AlarmManager.setInexactRepeating
 * without requesting restricted exact-alarm permissions.
 */
object FitTrackNotificationManager {

    const val CHANNEL_REMINDERS = "fittrack_reminders"
    const val CHANNEL_PR_ALERTS = "fittrack_pr_alerts"
    const val ACTION_FIRE_REMINDER = "com.example.notifications.ACTION_FIRE_REMINDER"
    const val EXTRA_REMINDER_TYPE = "extra_reminder_type"

    private const val NOTIFICATION_ID_PR = 3001
    private const val NOTIFICATION_ID_TEST = 3002

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val remindersChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Daily & Weekly Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Workout, meal logging, weigh-in, and weekly progress reminders."
        }

        val prChannel = NotificationChannel(
            CHANNEL_PR_ALERTS,
            "Personal Record Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Instant celebration alerts when you break a strength or rep personal record."
        }

        manager.createNotificationChannel(remindersChannel)
        manager.createNotificationChannel(prChannel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val osEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED && osEnabled
        } else {
            osEnabled
        }
    }

    fun formatTime12Hour(hourOfDay: Int, minute: Int): String {
        val safeHour = hourOfDay.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val amPm = if (safeHour < 12) "AM" else "PM"
        val displayHour = when {
            safeHour == 0 -> 12
            safeHour > 12 -> safeHour - 12
            else -> safeHour
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, safeMinute, amPm)
    }

    fun dayOfWeekLabel(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.SUNDAY -> "Sundays"
        Calendar.MONDAY -> "Mondays"
        Calendar.TUESDAY -> "Tuesdays"
        Calendar.WEDNESDAY -> "Wednesdays"
        Calendar.THURSDAY -> "Thursdays"
        Calendar.FRIDAY -> "Fridays"
        Calendar.SATURDAY -> "Saturdays"
        else -> "Sundays"
    }

    fun dayOfWeekShort(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.SUNDAY -> "Sun"
        Calendar.MONDAY -> "Mon"
        Calendar.TUESDAY -> "Tue"
        Calendar.WEDNESDAY -> "Wed"
        Calendar.THURSDAY -> "Thu"
        Calendar.FRIDAY -> "Fri"
        Calendar.SATURDAY -> "Sat"
        else -> "Sun"
    }

    fun syncScheduledReminders(context: Context, prefs: UserPreferences) {
        ensureChannels(context)
        val canNotify = hasNotificationPermission(context)

        updateReminderAlarm(
            context = context,
            type = FitTrackReminderType.WORKOUT,
            enabled = canNotify && prefs.workoutReminderEnabled,
            hourOfDay = prefs.workoutReminderHour,
            minute = prefs.workoutReminderMinute
        )
        updateReminderAlarm(
            context = context,
            type = FitTrackReminderType.MEAL,
            enabled = canNotify && prefs.mealReminderEnabled,
            hourOfDay = prefs.mealReminderHour,
            minute = prefs.mealReminderMinute
        )
        updateReminderAlarm(
            context = context,
            type = FitTrackReminderType.WEIGHT,
            enabled = canNotify && prefs.weightReminderEnabled,
            hourOfDay = prefs.weightReminderHour,
            minute = prefs.weightReminderMinute
        )
        updateReminderAlarm(
            context = context,
            type = FitTrackReminderType.WEEKLY_REPORT,
            enabled = canNotify && prefs.weeklyReportEnabled,
            hourOfDay = prefs.weeklyReportHour,
            minute = prefs.weeklyReportMinute,
            weeklyDayOfWeek = prefs.weeklyReportDayOfWeek
        )
    }

    private fun updateReminderAlarm(
        context: Context,
        type: FitTrackReminderType,
        enabled: Boolean,
        hourOfDay: Int,
        minute: Int,
        weeklyDayOfWeek: Int? = null
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, FitTrackReminderReceiver::class.java).apply {
            action = ACTION_FIRE_REMINDER
            putExtra(EXTRA_REMINDER_TYPE, type.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            type.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel existing alarm before rescheduling with updated time
        alarmManager.cancel(pendingIntent)

        if (!enabled) {
            pendingIntent.cancel()
            return
        }

        val triggerAtMillis = computeNextTriggerMillis(
            hourOfDay = hourOfDay,
            minute = minute,
            weeklyDayOfWeek = if (type.isWeekly) (weeklyDayOfWeek ?: Calendar.SUNDAY) else null
        )
        val intervalMillis = if (type.isWeekly) {
            AlarmManager.INTERVAL_DAY * 7L
        } else {
            AlarmManager.INTERVAL_DAY
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            intervalMillis,
            pendingIntent
        )
    }

    fun computeNextTriggerMillis(
        hourOfDay: Int,
        minute: Int,
        weeklyDayOfWeek: Int? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val next = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hourOfDay.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (weeklyDayOfWeek != null) {
            next.set(Calendar.DAY_OF_WEEK, weeklyDayOfWeek.coerceIn(Calendar.SUNDAY, Calendar.SATURDAY))
            if (!next.after(now)) {
                next.add(Calendar.WEEK_OF_YEAR, 1)
            }
        } else if (!next.after(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }

    @SuppressLint("MissingPermission")
    fun postReminderNotification(context: Context, type: FitTrackReminderType): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(type.title)
            .setContentText(type.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(type.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(createOpenAppIntent(context, type.requestCode))
            .setAutoCancel(true)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(type.notificationId, notification)
            true
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    fun postPrAlertNotification(context: Context, workoutName: String, prCount: Int): Boolean {
        if (prCount <= 0 || !hasNotificationPermission(context)) return false
        ensureChannels(context)

        val title = if (prCount == 1) "New Personal Record!" else "$prCount New Personal Records!"
        val body = "You crushed $prCount PR${if (prCount > 1) "s" else ""} during $workoutName. Check your PR log in FitTrack."

        val notification = NotificationCompat.Builder(context, CHANNEL_PR_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(createOpenAppIntent(context, NOTIFICATION_ID_PR))
            .setAutoCancel(true)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PR, notification)
            true
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    fun postTestNotification(context: Context): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_PR_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("FitTrack Notifications Active")
            .setContentText("Your personalized workout, meal, weigh-in, and PR alerts are ready!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your personalized workout, meal, weigh-in, and PR alerts are active and scheduled.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(createOpenAppIntent(context, NOTIFICATION_ID_TEST))
            .setAutoCancel(true)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEST, notification)
            true
        }.getOrDefault(false)
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun createOpenAppIntent(context: Context, requestCode: Int): PendingIntent {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
