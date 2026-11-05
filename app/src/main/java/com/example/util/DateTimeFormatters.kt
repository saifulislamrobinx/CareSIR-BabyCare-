package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object DateTimeFormatters {

    fun formatAgeFromDob(dobMillis: Long, nowMillis: Long = System.currentTimeMillis(), isBangla: Boolean = false): String {
        if (dobMillis <= 0L || dobMillis > nowMillis) {
            return if (isBangla) "নবজাতক (০ দিন)" else "Newborn (0 days old)"
        }
        val diffDays = TimeUnit.MILLISECONDS.toDays(nowMillis - dobMillis).toInt().coerceAtLeast(0)
        return when {
            diffDays < 14 -> {
                if (isBangla) "$diffDays দিন বয়সী" else "$diffDays ${if (diffDays == 1) "day" else "days"} old"
            }
            diffDays < 60 -> {
                val weeks = diffDays / 7
                val remDays = diffDays % 7
                if (isBangla) {
                    if (remDays > 0) "$weeks সপ্তাহ $remDays দিন বয়সী" else "$weeks সপ্তাহ বয়সী"
                } else {
                    if (remDays > 0) "$weeks ${if (weeks == 1) "week" else "weeks"} and $remDays ${if (remDays == 1) "day" else "days"} old"
                    else "$weeks ${if (weeks == 1) "week" else "weeks"} old"
                }
            }
            diffDays < 365 -> {
                val months = diffDays / 30
                val remWeeks = (diffDays % 30) / 7
                if (isBangla) {
                    if (remWeeks > 0) "$months মাস এবং $remWeeks সপ্তাহ বয়সী" else "$months মাস বয়সী"
                } else {
                    if (remWeeks > 0) "$months ${if (months == 1) "month" else "months"} and $remWeeks ${if (remWeeks == 1) "week" else "weeks"} old"
                    else "$months ${if (months == 1) "month" else "months"} old"
                }
            }
            else -> {
                val years = diffDays / 365
                val remMonths = (diffDays % 365) / 30
                if (isBangla) {
                    if (remMonths > 0) "$years বছর এবং $remMonths মাস বয়সী" else "$years বছর বয়সী"
                } else {
                    if (remMonths > 0) "$years ${if (years == 1) "year" else "years"} and $remMonths ${if (remMonths == 1) "month" else "months"} old"
                    else "$years ${if (years == 1) "year" else "years"} old"
                }
            }
        }
    }

    fun formatTime(timestampMillis: Long?, use24Hour: Boolean = false): String {
        if (timestampMillis == null || timestampMillis <= 0L) return "--:--"
        val pattern = if (use24Hour) "HH:mm" else "hh:mm a"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestampMillis))
    }

    fun formatDate(timestampMillis: Long?, pattern: String = "dd MMM yyyy"): String {
        if (timestampMillis == null || timestampMillis <= 0L) return "--"
        val safePattern = pattern.ifBlank { "dd MMM yyyy" }
        return try {
            SimpleDateFormat(safePattern, Locale.getDefault()).format(Date(timestampMillis))
        } catch (e: Exception) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestampMillis))
        }
    }

    fun formatDateTime(timestampMillis: Long?, datePattern: String = "dd MMM yyyy", use24Hour: Boolean = false): String {
        if (timestampMillis == null || timestampMillis <= 0L) return "--"
        return "${formatDate(timestampMillis, datePattern)} • ${formatTime(timestampMillis, use24Hour)}"
    }

    fun formatRelativeTime(targetMillis: Long?, nowMillis: Long = System.currentTimeMillis(), isBangla: Boolean = false): String {
        if (targetMillis == null || targetMillis <= 0L) return if (isBangla) "নির্ধারিত নয়" else "Not scheduled"
        val diffMillis = targetMillis - nowMillis
        val absMinutes = TimeUnit.MILLISECONDS.toMinutes(abs(diffMillis))
        val hours = absMinutes / 60
        val mins = absMinutes % 60

        val durationText = when {
            hours > 24 -> {
                val days = hours / 24
                if (isBangla) "${days}দিন" else "${days}d ${hours % 24}h"
            }
            hours > 0 -> if (isBangla) "${hours}ঘণ্টা ${mins}মিনিট" else "${hours}h ${mins}m"
            else -> if (isBangla) "${mins}মিনিট" else "${mins}m"
        }

        return if (diffMillis >= 0) {
            if (absMinutes == 0L) {
                if (isBangla) "এখনই" else "Due now"
            } else {
                if (isBangla) "$durationText বাকি" else "in $durationText"
            }
        } else {
            if (absMinutes == 0L) {
                if (isBangla) "এইমাত্র" else "Just now"
            } else {
                if (isBangla) "$durationText আগে" else "$durationText ago"
            }
        }
    }

    fun formatCountdownStatus(targetMillis: Long?, nowMillis: Long = System.currentTimeMillis(), isBangla: Boolean = false): Pair<String, Boolean> {
        if (targetMillis == null || targetMillis <= 0L) {
            return (if (isBangla) "নির্ধারিত নয়" else "Not scheduled") to false
        }
        val diffMillis = targetMillis - nowMillis
        val absMinutes = TimeUnit.MILLISECONDS.toMinutes(abs(diffMillis))
        val hours = absMinutes / 60
        val mins = absMinutes % 60
        val timePart = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        return if (diffMillis < -60_000L) {
            val label = if (isBangla) "বিলম্বিত ($timePart)" else "Overdue by $timePart"
            label to true
        } else if (diffMillis <= 60_000L) {
            val label = if (isBangla) "এখনই সময়" else "Due now"
            label to false
        } else {
            val label = if (isBangla) "$timePart বাকি" else "in $timePart"
            label to false
        }
    }

    fun formatDurationSeconds(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val hrs = safe / 3600
        val mins = (safe % 3600) / 60
        val secs = safe % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun startOfDayMillis(timestampMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestampMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDayMillis(timestampMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestampMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}
