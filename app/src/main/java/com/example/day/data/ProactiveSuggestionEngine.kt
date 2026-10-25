package com.example.day.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class AppContext(
    val todayDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val currentMinutes: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 + Calendar.getInstance().get(Calendar.MINUTE),
    val wakeUpTime: String = "07:00"
)

enum class SuggestionType {
    HABIT_NEGLECTED,      // habit not done in X days
    OVERDUE_TODO,         // todo was due yesterday/earlier
    LIGHT_DAY,            // today has fewer than 3 tasks scheduled
    STREAK_AT_RISK,       // habit streak about to break (not done today, it's evening)
    PRODUCTIVE_REMINDER   // it's past wake time and nothing is scheduled
}

data class ProactiveSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val type: SuggestionType,
    val message: String,
    val actionLabel: String?,  // e.g. "Add to today"
    val actionData: String?    // JSON or prompt text for the action to execute
)

class ProactiveSuggestionEngine(
    private val taskDao: TaskDao,
    private val todoDao: TodoDao,
    private val habitDao: HabitDao
) {
    suspend fun generateSuggestions(context: AppContext): List<ProactiveSuggestion> {
        val suggestions = mutableListOf<ProactiveSuggestion>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = context.todayDate

        val tasks = taskDao.getTasksForDateSync(todayStr)
        val todos = todoDao.getActiveTodosSync()
        val habits = habitDao.getAllHabitsSync()
        val habitLogs = habitDao.getAllLogsSync()

        val wakeUpParts = context.wakeUpTime.split(":").mapNotNull { it.toIntOrNull() }
        val wakeUpMinutes = if (wakeUpParts.size == 2) wakeUpParts[0] * 60 + wakeUpParts[1] else 7 * 60

        // 1. STREAK_AT_RISK:
        // Habit has streak > 0 AND no log for today AND current time > 6pm (18:00)
        if (context.currentHour >= 18) {
            for (habit in habits) {
                val logsForHabit = habitLogs.filter { it.habitId == habit.id }
                val streak = calculateStreak(logsForHabit, todayStr, sdf)
                val loggedToday = logsForHabit.any { it.date == todayStr }
                if (streak > 0 && !loggedToday) {
                    suggestions.add(
                        ProactiveSuggestion(
                            type = SuggestionType.STREAK_AT_RISK,
                            message = "Your ${habit.name} streak ($streak days) is at risk — you haven't logged it today ⚠️",
                            actionLabel = "Add to today",
                            actionData = "Add ${habit.name} to today's schedule"
                        )
                    )
                }
            }
        }

        // 2. HABIT_NEGLECTED:
        // Any habit with no HabitLog entry in the last 3+ days
        val todayDateObj = try { sdf.parse(todayStr) } catch (_: Exception) { Date() }
        for (habit in habits) {
            val logsForHabit = habitLogs.filter { it.habitId == habit.id }
            val latestLog = logsForHabit.maxByOrNull { it.date }
            if (latestLog != null) {
                val lastDate = sdf.parse(latestLog.date)
                if (lastDate != null && todayDateObj != null) {
                    val daysDiff = ((todayDateObj.time - lastDate.time) / (1000 * 60 * 60 * 24)).toInt()
                    if (daysDiff >= 3) {
                        suggestions.add(
                            ProactiveSuggestion(
                                type = SuggestionType.HABIT_NEGLECTED,
                                message = "You haven't done ${habit.name} in $daysDiff days 👀",
                                actionLabel = "Add to today",
                                actionData = "Add ${habit.name} to today's schedule"
                            )
                        )
                    }
                }
            } else {
                val createdDate = Date(habit.createdAt)
                val daysDiff = if (todayDateObj != null) {
                    ((todayDateObj.time - createdDate.time) / (1000 * 60 * 60 * 24)).toInt()
                } else 3
                if (daysDiff >= 3) {
                    suggestions.add(
                        ProactiveSuggestion(
                            type = SuggestionType.HABIT_NEGLECTED,
                            message = "You haven't done ${habit.name} in $daysDiff days 👀",
                            actionLabel = "Add to today",
                            actionData = "Add ${habit.name} to today's schedule"
                        )
                    )
                }
            }
        }

        // 3. OVERDUE_TODO:
        // Any todo with dueDate < today and isCompleted = false
        for (todo in todos) {
            val dueDateStr = todo.dueDate
            if (!todo.isCompleted && !dueDateStr.isNullOrBlank() && dueDateStr < todayStr) {
                val dueDate = sdf.parse(dueDateStr)
                val daysAgo = if (dueDate != null && todayDateObj != null) {
                    ((todayDateObj.time - dueDate.time) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
                } else 1
                suggestions.add(
                    ProactiveSuggestion(
                        type = SuggestionType.OVERDUE_TODO,
                        message = "'${todo.title}' was due $daysAgo days ago — still on your list",
                        actionLabel = "Add to today",
                        actionData = "Schedule overdue todo '${todo.title}' for today"
                    )
                )
            }
        }

        // 4. PRODUCTIVE_REMINDER:
        // Current time >= wakeUpTime AND no tasks scheduled for today
        if (context.currentMinutes >= wakeUpMinutes && tasks.isEmpty()) {
            suggestions.add(
                ProactiveSuggestion(
                    type = SuggestionType.PRODUCTIVE_REMINDER,
                    message = "Morning! Nothing on your schedule yet — tell me what's happening today ☀️",
                    actionLabel = "Plan day",
                    actionData = "Plan my day"
                )
            )
        }

        // 5. LIGHT_DAY:
        // Fewer than 3 tasks scheduled for today AND current time < 10am
        if (tasks.size < 3 && context.currentHour < 10 && tasks.isNotEmpty()) {
            suggestions.add(
                ProactiveSuggestion(
                    type = SuggestionType.LIGHT_DAY,
                    message = "Your day looks pretty open — want me to build your schedule? 📅",
                    actionLabel = "Build schedule",
                    actionData = "Build my schedule for today"
                )
            )
        }

        // Priority ordering: STREAK_AT_RISK, OVERDUE_TODO, HABIT_NEGLECTED, PRODUCTIVE_REMINDER, LIGHT_DAY
        val priorityMap = mapOf(
            SuggestionType.STREAK_AT_RISK to 1,
            SuggestionType.OVERDUE_TODO to 2,
            SuggestionType.HABIT_NEGLECTED to 3,
            SuggestionType.PRODUCTIVE_REMINDER to 4,
            SuggestionType.LIGHT_DAY to 5
        )

        return suggestions
            .sortedBy { priorityMap[it.type] ?: 99 }
            .take(3)
    }

    private fun calculateStreak(logs: List<HabitLog>, todayStr: String, sdf: SimpleDateFormat): Int {
        if (logs.isEmpty()) return 0
        val dates = logs.map { it.date }.distinct().toSet()
        val parsedToday = try { sdf.parse(todayStr) } catch (_: Exception) { null }

        val cal = Calendar.getInstance().apply {
            if (parsedToday != null) time = parsedToday
        }
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        val streakCal = Calendar.getInstance().apply {
            if (parsedToday != null) time = parsedToday
        }

        val activeCal = when {
            dates.contains(todayStr) -> streakCal
            dates.contains(yesterdayStr) -> streakCal.apply { add(Calendar.DAY_OF_YEAR, -1) }
            else -> return 0
        }

        var streak = 0
        while (dates.contains(sdf.format(activeCal.time))) {
            streak++
            activeCal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }
}
