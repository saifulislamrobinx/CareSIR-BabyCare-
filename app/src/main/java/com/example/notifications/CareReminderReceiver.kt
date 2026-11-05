package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.CareSirDatabase
import com.example.data.local.ReminderCompletionEntity
import com.example.data.repository.CareSirRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class CareReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = CareSirDatabase.getInstance(context)
                val repo = CareSirRepository(db.careSirDao())
                val settings = repo.getNotificationSettingsOnce()

                val notificationId = intent.getIntExtra(CareNotificationManager.EXTRA_NOTIFICATION_ID, 9999)
                val reminderId = intent.getLongExtra(CareNotificationManager.EXTRA_REMINDER_ID, -1L)
                val title = intent.getStringExtra(CareNotificationManager.EXTRA_TITLE) ?: "CareSIR Reminder"
                val message = intent.getStringExtra(CareNotificationManager.EXTRA_MESSAGE) ?: "Baby care activity due"
                val category = intent.getStringExtra(CareNotificationManager.EXTRA_CATEGORY) ?: "CUSTOM"
                val priority = intent.getStringExtra(CareNotificationManager.EXTRA_PRIORITY) ?: "NORMAL"

                when (intent.action) {
                    CareNotificationManager.ACTION_TRIGGER_ALARM -> {
                        CareNotificationManager.showReminderNotification(
                            context = context,
                            notificationId = notificationId,
                            reminderId = reminderId,
                            title = title,
                            message = message,
                            category = category,
                            priority = priority,
                            settings = settings
                        )
                    }
                    CareNotificationManager.ACTION_MARK_DONE -> {
                        NotificationManagerCompat.from(context).cancel(notificationId)
                        if (reminderId > 0) {
                            val updated = repo.handleReminderAction(reminderId, title, category, "COMPLETED")
                            if (updated != null && !updated.isCompleted && updated.isEnabled) {
                                CareNotificationManager.scheduleCustomReminder(context, updated)
                            }
                        } else {
                            db.careSirDao().insertReminderCompletion(
                                ReminderCompletionEntity(
                                    reminderId = reminderId,
                                    reminderTitle = title,
                                    category = category,
                                    actionTaken = "COMPLETED"
                                )
                            )
                        }
                    }
                    CareNotificationManager.ACTION_SNOOZE -> {
                        NotificationManagerCompat.from(context).cancel(notificationId)
                        val snoozeTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15)
                        if (reminderId > 0) {
                            val updated = repo.handleReminderAction(reminderId, title, category, "SNOOZED")
                            if (updated != null) {
                                CareNotificationManager.scheduleCustomReminder(context, updated)
                            }
                        } else {
                            db.careSirDao().insertReminderCompletion(
                                ReminderCompletionEntity(
                                    reminderId = reminderId,
                                    reminderTitle = title,
                                    category = category,
                                    actionTaken = "SNOOZED"
                                )
                            )
                            when (category.uppercase()) {
                                "FEEDING" -> CareNotificationManager.scheduleFeedingReminder(context, snoozeTime)
                                "DIAPER" -> CareNotificationManager.scheduleDiaperReminder(context, snoozeTime)
                                "CLEANING" -> CareNotificationManager.scheduleCleaningReminder(context, title, snoozeTime)
                                "HYGIENE" -> CareNotificationManager.scheduleHygieneReminder(context, title, snoozeTime)
                            }
                        }
                    }
                    CareNotificationManager.ACTION_SKIP -> {
                        NotificationManagerCompat.from(context).cancel(notificationId)
                        if (reminderId > 0) {
                            val updated = repo.handleReminderAction(reminderId, title, category, "SKIPPED")
                            if (updated != null && !updated.isCompleted && updated.isEnabled) {
                                CareNotificationManager.scheduleCustomReminder(context, updated)
                            }
                        } else {
                            db.careSirDao().insertReminderCompletion(
                                ReminderCompletionEntity(
                                    reminderId = reminderId,
                                    reminderTitle = title,
                                    category = category,
                                    actionTaken = "SKIPPED"
                                )
                            )
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
