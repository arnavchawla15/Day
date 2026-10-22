package com.example.day.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SchedulePreferences(
    val weeklyTimetable: String = "",
    val generalInstructions: String = "",
    val wakeUpTime: String = "07:00",
    val sleepTime: String = "23:00",
    val autoScheduleEnabled: Boolean = false,
    val lastAutoScheduleDate: String = ""
)

class SchedulePreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _schedulePreferences = MutableStateFlow(loadPreferences())
    val schedulePreferences: StateFlow<SchedulePreferences> = _schedulePreferences.asStateFlow()

    fun getSchedulePreferences(): SchedulePreferences {
        return loadPreferences()
    }

    private fun loadPreferences(): SchedulePreferences {
        return SchedulePreferences(
            weeklyTimetable = prefs.getString(KEY_WEEKLY_TIMETABLE, "") ?: "",
            generalInstructions = prefs.getString(KEY_GENERAL_INSTRUCTIONS, "") ?: "",
            wakeUpTime = prefs.getString(KEY_WAKE_UP_TIME, "07:00") ?: "07:00",
            sleepTime = prefs.getString(KEY_SLEEP_TIME, "23:00") ?: "23:00",
            autoScheduleEnabled = prefs.getBoolean(KEY_AUTO_SCHEDULE_ENABLED, false),
            lastAutoScheduleDate = prefs.getString(KEY_LAST_AUTO_SCHEDULE_DATE, "") ?: ""
        )
    }

    fun saveSchedulePreferences(preferences: SchedulePreferences) {
        prefs.edit()
            .putString(KEY_WEEKLY_TIMETABLE, preferences.weeklyTimetable)
            .putString(KEY_GENERAL_INSTRUCTIONS, preferences.generalInstructions)
            .putString(KEY_WAKE_UP_TIME, preferences.wakeUpTime)
            .putString(KEY_SLEEP_TIME, preferences.sleepTime)
            .putBoolean(KEY_AUTO_SCHEDULE_ENABLED, preferences.autoScheduleEnabled)
            .putString(KEY_LAST_AUTO_SCHEDULE_DATE, preferences.lastAutoScheduleDate)
            .apply()
        _schedulePreferences.value = preferences
    }

    fun updateLastAutoScheduleDate(date: String) {
        prefs.edit()
            .putString(KEY_LAST_AUTO_SCHEDULE_DATE, date)
            .apply()
        _schedulePreferences.value = _schedulePreferences.value.copy(lastAutoScheduleDate = date)
    }

    companion object {
        private const val PREFS_NAME = "day_schedule_prefs"
        private const val KEY_WEEKLY_TIMETABLE = "weekly_timetable"
        private const val KEY_GENERAL_INSTRUCTIONS = "general_instructions"
        private const val KEY_WAKE_UP_TIME = "wake_up_time"
        private const val KEY_SLEEP_TIME = "sleep_time"
        private const val KEY_AUTO_SCHEDULE_ENABLED = "auto_schedule_enabled"
        private const val KEY_LAST_AUTO_SCHEDULE_DATE = "last_auto_schedule_date"
    }
}
