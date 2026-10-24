package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    private val shortDayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    fun todayDateString(): String = dateFormat.format(Date())

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val date = dateFormat.parse(dateStr) ?: Date()
            displayDateFormat.format(date)
        } catch (_: Exception) {
            dateStr
        }
    }

    fun formatShortDay(dateStr: String): String {
        return try {
            val date = dateFormat.parse(dateStr) ?: Date()
            shortDayFormat.format(date)
        } catch (_: Exception) {
            dateStr
        }
    }

    fun getPastDays(count: Int): List<String> {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        for (i in (count - 1) downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                time = cal.time
                add(Calendar.DAY_OF_YEAR, -i)
            }
            list.add(dateFormat.format(dayCal.time))
        }
        return list
    }

    fun currentMinutesOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun formatMinutesToTime(minutes: Int): String {
        val normalized = (minutes % 1440 + 1440) % 1440
        val hour = normalized / 60
        val min = normalized % 60
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when (val h = hour % 12) {
            0 -> 12
            else -> h
        }
        return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, min, amPm)
    }

    fun formatDurationMinutes(durationMinutes: Int): String {
        val hours = durationMinutes / 60
        val mins = durationMinutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    fun calculateDelta(targetMinutes: Int, actualMinutes: Int): String {
        val diff = actualMinutes - targetMinutes
        return when {
            diff == 0 -> "On time"
            diff > 0 -> "+${diff}m"
            else -> "${diff}m"
        }
    }

    fun greetingForCurrentTime(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Restful night"
        }
    }
}
