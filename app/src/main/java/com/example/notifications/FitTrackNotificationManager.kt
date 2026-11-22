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

enum class FitTrackReminderType(
    val requestCode: Int,
    val notificationId: Int,
    val title: String,
    val body: String,
    val hourOfDay: Int,
    val minute: Int,
    val weeklyDayOfWeek: Int? = null
) {
    WORKOUT(
        requestCode = 1001,
        notificationId = 2001,
        title = "Time to Train",
        body = "Your Gym & Home routines are ready in FitTrack. Keep your streak alive!",
        hourOfDay = 18,
        minute = 0
    ),
    MEAL(
        requestCode = 1002,
        notificationId = 2002,
        title = "Log Your Meals & Water",
        body = "Track today's calories, protein, and hydration to keep your Daily Score climbing.",
        hourOfDay = 13,
        minute = 0
    ),
    WEIGHT(
        requestCode = 1003,
        notificationId = 2003,
        title = "Morning Weigh-In Check",
        body = "Step on the scale or check your calorie-estimated weight trend in FitTrack.",
        hourOfDay = 7,
        minute = 30
    ),
    WEEKLY_REPORT(
        requestCode = 1004,
        notificationId = 2004,
        title = "Weekly Progress Summary Ready",
        body = "Review your 7-day weight trajectory, strength volume, and nutrition consistency.",
        hourOfDay = 18,
        minute = 0,
        weeklyDayOfWeek = Calendar.SUNDAY
    );

    companion object {
        fun fromNameSafe(name: String?): FitTrackReminderType? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

/**
 * Play-Policy-compliant Android notification manager and inexact repeating reminder scheduler.
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

    fun syncScheduledReminders(context: Context, prefs: UserPreferences) {
        ensureChannels(context)
        val canNotify = hasNotificationPermission(context)

        updateReminderAlarm(context, FitTrackReminderType.WORKOUT, canNotify && prefs.workoutReminderEnabled)
        updateReminderAlarm(context, FitTrackReminderType.MEAL, canNotify && prefs.mealReminderEnabled)
        updateReminderAlarm(context, FitTrackReminderType.WEIGHT, canNotify && prefs.weightReminderEnabled)
        updateReminderAlarm(context, FitTrackReminderType.WEEKLY_REPORT, canNotify && prefs.weeklyReportEnabled)
    }

    private fun updateReminderAlarm(context: Context, type: FitTrackReminderType, enabled: Boolean) {
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

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            return
        }

        val triggerAtMillis = computeNextTriggerMillis(type)
        val intervalMillis = if (type.weeklyDayOfWeek != null) {
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

    private fun computeNextTriggerMillis(type: FitTrackReminderType): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, type.hourOfDay)
            set(Calendar.MINUTE, type.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (type.weeklyDayOfWeek != null) {
            next.set(Calendar.DAY_OF_WEEK, type.weeklyDayOfWeek)
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
            .setContentText("Your workout, nutrition, weigh-in, and PR alerts are ready to go!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your workout, nutrition, weigh-in, and PR alerts are active and scheduled.")
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
