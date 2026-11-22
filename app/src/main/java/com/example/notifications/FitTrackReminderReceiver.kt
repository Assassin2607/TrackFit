package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.prefs.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives scheduled inexact reminder alarms and BOOT_COMPLETED broadcasts
 * so enabled FitTrack reminders persist across device restarts.
 */
class FitTrackReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val appContext = context.applicationContext

        when (action) {
            FitTrackNotificationManager.ACTION_FIRE_REMINDER -> {
                val type = FitTrackReminderType.fromNameSafe(
                    intent.getStringExtra(FitTrackNotificationManager.EXTRA_REMINDER_TYPE)
                ) ?: return
                FitTrackNotificationManager.postReminderNotification(appContext, type)
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val prefs = PreferencesManager(appContext).preferencesFlow.first()
                        FitTrackNotificationManager.syncScheduledReminders(appContext, prefs)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
