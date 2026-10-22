package com.example.day.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object RecurrenceHelper {

    const val TYPE_DAILY = "DAILY"
    const val TYPE_EVERY_OTHER_DAY = "EVERY_OTHER_DAY"
    const val TYPE_WEEKLY = "WEEKLY"
    const val TYPE_CUSTOM = "CUSTOM"

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun isRecurring(todo: Todo): Boolean {
        return !todo.recurrenceType.isNullOrBlank() && todo.recurrenceType.uppercase(Locale.ROOT) != "NONE"
    }

    fun getNextDueDate(todo: Todo): String {
        val type = if (!todo.recurrenceType.isNullOrBlank() && todo.recurrenceType.uppercase(Locale.ROOT) != "NONE") {
            todo.recurrenceType
        } else {
            TYPE_DAILY
        }
        return calculateNextDueDate(
            currentDueDateStr = todo.dueDate,
            recurrenceType = type,
            recurrenceDays = todo.recurrenceDays
        )
    }

    fun calculateNextDueDate(
        currentDueDateStr: String?,
        recurrenceType: String,
        recurrenceDays: String?
    ): String {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val baseCal = Calendar.getInstance().apply {
            if (!currentDueDateStr.isNullOrBlank()) {
                try {
                    time = DATE_FORMAT.parse(currentDueDateStr) ?: today.time
                } catch (e: Exception) {
                    time = today.time
                }
            } else {
                time = today.time
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the base date is in the past, anchor the next recurrence search from today
        val startCal = if (baseCal.before(today)) today.clone() as Calendar else baseCal

        when (recurrenceType.uppercase(Locale.ROOT)) {
            TYPE_DAILY -> {
                startCal.add(Calendar.DAY_OF_MONTH, 1)
            }
            TYPE_EVERY_OTHER_DAY -> {
                startCal.add(Calendar.DAY_OF_MONTH, 2)
            }
            TYPE_WEEKLY -> {
                val targetDayOfWeek = parseDayOfWeek(recurrenceDays) ?: Calendar.SUNDAY
                // Advance at least 1 day, up to 7 days
                do {
                    startCal.add(Calendar.DAY_OF_MONTH, 1)
                } while (startCal.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek)
            }
            TYPE_CUSTOM -> {
                val targetDays = parseDaysOfWeekSet(recurrenceDays)
                if (targetDays.isEmpty()) {
                    startCal.add(Calendar.DAY_OF_MONTH, 1)
                } else {
                    // Advance at least 1 day until day of week matches one of the target days
                    var counter = 0
                    do {
                        startCal.add(Calendar.DAY_OF_MONTH, 1)
                        counter++
                    } while (startCal.get(Calendar.DAY_OF_WEEK) !in targetDays && counter < 14)
                }
            }
            else -> {
                startCal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return DATE_FORMAT.format(startCal.time)
    }

    fun parseDayOfWeek(dayStr: String?): Int? {
        if (dayStr.isNullOrBlank()) return null
        return when (dayStr.trim().uppercase(Locale.ROOT)) {
            "SUN", "SUNDAY" -> Calendar.SUNDAY
            "MON", "MONDAY" -> Calendar.MONDAY
            "TUE", "TUESDAY" -> Calendar.TUESDAY
            "WED", "WEDNESDAY" -> Calendar.WEDNESDAY
            "THU", "THURSDAY" -> Calendar.THURSDAY
            "FRI", "FRIDAY" -> Calendar.FRIDAY
            "SAT", "SATURDAY" -> Calendar.SATURDAY
            else -> dayStr.toIntOrNull()
        }
    }

    fun parseDaysOfWeekSet(daysStr: String?): Set<Int> {
        if (daysStr.isNullOrBlank()) return emptySet()
        val set = mutableSetOf<Int>()
        val parts = daysStr.split(",", " ", "|")
        for (part in parts) {
            val dow = parseDayOfWeek(part)
            if (dow != null) set.add(dow)
        }
        return set
    }

    fun getRecurrenceLabel(recurrenceType: String?, recurrenceDays: String?): String {
        return when (recurrenceType?.uppercase(Locale.ROOT)) {
            TYPE_DAILY -> "Daily"
            TYPE_EVERY_OTHER_DAY -> "Every other day"
            TYPE_WEEKLY -> if (!recurrenceDays.isNullOrBlank()) "Weekly on $recurrenceDays" else "Weekly"
            TYPE_CUSTOM -> if (!recurrenceDays.isNullOrBlank()) "Custom: $recurrenceDays" else "Custom days"
            else -> "Does not repeat"
        }
    }
}
