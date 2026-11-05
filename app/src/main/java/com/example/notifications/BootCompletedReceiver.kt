package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.CareSirDatabase
import com.example.data.repository.CareSirRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
        if (intent?.action !in validActions) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = CareSirDatabase.getInstance(context)
                val repo = CareSirRepository(db.careSirDao())
                val settings = repo.getNotificationSettingsOnce()
                val now = System.currentTimeMillis()

                if (settings.feedingNotificationsEnabled && (settings.nextFeedingAlarmMillis ?: 0L) > now) {
                    CareNotificationManager.scheduleFeedingReminder(context, settings.nextFeedingAlarmMillis!!)
                }
                if (settings.diaperNotificationsEnabled && (settings.nextDiaperAlarmMillis ?: 0L) > now) {
                    CareNotificationManager.scheduleDiaperReminder(context, settings.nextDiaperAlarmMillis!!)
                }
                if (settings.cleaningNotificationsEnabled && (settings.nextCleaningAlarmMillis ?: 0L) > now) {
                    CareNotificationManager.scheduleCleaningReminder(context, "Feeding Equipment", settings.nextCleaningAlarmMillis!!)
                }
                if (settings.customNotificationsEnabled) {
                    val activeReminders = repo.getAllActiveCustomRemindersOnce()
                    activeReminders.forEach { reminder ->
                        if (reminder.scheduledTimeMillis > now) {
                            CareNotificationManager.scheduleCustomReminder(context, reminder)
                        } else if (reminder.repeatType != "NEVER") {
                            val nextTime = repo.calculateNextRecurringTime(reminder, now)
                            if (nextTime != null) {
                                val updated = reminder.copy(scheduledTimeMillis = nextTime, updatedAt = now)
                                db.careSirDao().updateCustomReminder(updated)
                                CareNotificationManager.scheduleCustomReminder(context, updated)
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
