package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.CustomReminderEntity
import com.example.data.local.NotificationSettingsEntity
import java.util.Calendar

object CareNotificationManager {

    const val CHANNEL_FEEDING = "caresir_channel_feeding"
    const val CHANNEL_DIAPER = "caresir_channel_diaper"
    const val CHANNEL_HYGIENE = "caresir_channel_hygiene"
    const val CHANNEL_CLEANING = "caresir_channel_cleaning"
    const val CHANNEL_CUSTOM = "caresir_channel_custom"

    const val ACTION_TRIGGER_ALARM = "com.example.notifications.ACTION_TRIGGER_ALARM"
    const val ACTION_MARK_DONE = "com.example.notifications.ACTION_MARK_DONE"
    const val ACTION_SNOOZE = "com.example.notifications.ACTION_SNOOZE"
    const val ACTION_SKIP = "com.example.notifications.ACTION_SKIP"

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_MESSAGE = "extra_message"
    const val EXTRA_CATEGORY = "extra_category"
    const val EXTRA_PRIORITY = "extra_priority"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    private const val REQ_CODE_FEEDING = 9001
    private const val REQ_CODE_DIAPER = 9002
    private const val REQ_CODE_CLEANING = 9003
    private const val REQ_CODE_HYGIENE_BASE = 9100
    private const val REQ_CODE_CUSTOM_BASE = 10000

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_FEEDING,
                    "Feeding Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Smart reminders for baby feeding schedules and intervals"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_DIAPER,
                    "Diaper Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders for diaper checks and changes"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_HYGIENE,
                    "Hygiene Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily newborn personal hygiene routine reminders"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_CLEANING,
                    "Equipment Cleaning Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders for bottle and breast pump cleaning & sterilization"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_CUSTOM,
                    "Custom Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "User-created custom parenting and baby-care reminders"
                    enableVibration(true)
                }
            )
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isWithinQuietHours(settings: NotificationSettingsEntity, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!settings.quietHoursEnabled) return false
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val start = settings.quietHoursStartHour
        val end = settings.quietHoursEndHour
        return if (start <= end) {
            hour in start until end
        } else {
            // Spans midnight (e.g. 22:00 to 06:00)
            hour >= start || hour < end
        }
    }

    fun scheduleFeedingReminder(context: Context, triggerAtMillis: Long) {
        scheduleExactOrAllowIdle(
            context = context,
            requestCode = REQ_CODE_FEEDING,
            reminderId = -1L,
            title = "Feeding Reminder",
            message = "It's time for your baby's next scheduled feeding session.",
            category = "FEEDING",
            priority = "HIGH",
            triggerAtMillis = triggerAtMillis
        )
    }

    fun scheduleDiaperReminder(context: Context, triggerAtMillis: Long) {
        scheduleExactOrAllowIdle(
            context = context,
            requestCode = REQ_CODE_DIAPER,
            reminderId = -2L,
            title = "Diaper Check Reminder",
            message = "Time for a quick diaper check to keep your little one comfortable.",
            category = "DIAPER",
            priority = "NORMAL",
            triggerAtMillis = triggerAtMillis
        )
    }

    fun scheduleCleaningReminder(context: Context, activityName: String, triggerAtMillis: Long) {
        scheduleExactOrAllowIdle(
            context = context,
            requestCode = REQ_CODE_CLEANING,
            reminderId = -3L,
            title = "Equipment Cleaning: $activityName",
            message = "Time to clean or sterilize feeding equipment safely.",
            category = "CLEANING",
            priority = "NORMAL",
            triggerAtMillis = triggerAtMillis
        )
    }

    fun scheduleHygieneReminder(context: Context, activityName: String, triggerAtMillis: Long) {
        val code = REQ_CODE_HYGIENE_BASE + (activityName.hashCode() and 0x7F)
        scheduleExactOrAllowIdle(
            context = context,
            requestCode = code,
            reminderId = -4L,
            title = "Baby Hygiene: $activityName",
            message = "Scheduled gentle hygiene care: $activityName",
            category = "HYGIENE",
            priority = "NORMAL",
            triggerAtMillis = triggerAtMillis
        )
    }

    fun scheduleCustomReminder(context: Context, reminder: CustomReminderEntity) {
        val code = REQ_CODE_CUSTOM_BASE + reminder.id.toInt()
        if (!reminder.isEnabled || reminder.isCompleted) {
            cancelAlarm(context, code)
            return
        }
        if (reminder.scheduledTimeMillis <= System.currentTimeMillis() - 60_000L) {
            return
        }
        scheduleExactOrAllowIdle(
            context = context,
            requestCode = code,
            reminderId = reminder.id,
            title = reminder.title,
            message = reminder.description.ifBlank { "Scheduled CareSIR reminder (${reminder.category})" },
            category = reminder.category,
            priority = reminder.priority,
            triggerAtMillis = reminder.scheduledTimeMillis
        )
    }

    fun cancelCustomReminder(context: Context, reminderId: Long) {
        cancelAlarm(context, REQ_CODE_CUSTOM_BASE + reminderId.toInt())
    }

    private fun scheduleExactOrAllowIdle(
        context: Context,
        requestCode: Int,
        reminderId: Long,
        title: String,
        message: String,
        category: String,
        priority: String,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, CareReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_CATEGORY, category)
            putExtra(EXTRA_PRIORITY, priority)
            putExtra(EXTRA_NOTIFICATION_ID, requestCode)
        }

        // Cancel any previous pending intent with the same requestCode to prevent duplicates
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val safeTrigger = maxOf(triggerAtMillis, System.currentTimeMillis() + 2_000L)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTrigger, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTrigger, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTrigger, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, safeTrigger, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, safeTrigger, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, CareReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showReminderNotification(
        context: Context,
        notificationId: Int,
        reminderId: Long,
        title: String,
        message: String,
        category: String,
        priority: String,
        settings: NotificationSettingsEntity
    ) {
        createNotificationChannels(context)
        if (!hasNotificationPermission(context)) return

        // Check category toggle
        val categoryAllowed = when (category.uppercase()) {
            "FEEDING" -> settings.feedingNotificationsEnabled
            "DIAPER" -> settings.diaperNotificationsEnabled
            "HYGIENE" -> settings.hygieneNotificationsEnabled
            "CLEANING" -> settings.cleaningNotificationsEnabled
            else -> settings.customNotificationsEnabled
        }
        if (!categoryAllowed) return
        if (isWithinQuietHours(settings) && priority.uppercase() != "HIGH") return

        val channelId = when (category.uppercase()) {
            "FEEDING" -> CHANNEL_FEEDING
            "DIAPER" -> CHANNEL_DIAPER
            "HYGIENE" -> CHANNEL_HYGIENE
            "CLEANING" -> CHANNEL_CLEANING
            else -> CHANNEL_CUSTOM
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val donePendingIntent = buildActionPendingIntent(
            context,
            notificationId * 10 + 1,
            ACTION_MARK_DONE,
            notificationId,
            reminderId,
            title,
            category
        )
        val snoozePendingIntent = buildActionPendingIntent(
            context,
            notificationId * 10 + 2,
            ACTION_SNOOZE,
            notificationId,
            reminderId,
            title,
            category
        )
        val skipPendingIntent = buildActionPendingIntent(
            context,
            notificationId * 10 + 3,
            ACTION_SKIP,
            notificationId,
            reminderId,
            title,
            category
        )

        val compatPriority = when (priority.uppercase()) {
            "HIGH" -> NotificationCompat.PRIORITY_HIGH
            "LOW" -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(compatPriority)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Done", donePendingIntent)
            .addAction(0, "Snooze 15m", snoozePendingIntent)
            .addAction(0, "Skip", skipPendingIntent)

        if (!settings.soundEnabled) {
            builder.setSilent(true)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Handled gracefully if permission was revoked
        }
    }

    private fun buildActionPendingIntent(
        context: Context,
        requestCode: Int,
        action: String,
        notificationId: Int,
        reminderId: Long,
        title: String,
        category: String
    ): PendingIntent {
        val intent = Intent(context, CareReminderReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_CATEGORY, category)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
