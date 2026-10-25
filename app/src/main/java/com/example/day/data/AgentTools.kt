package com.example.day.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.day.widget.DayWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Structured tool call request extracted from Gemini agent responses.
 */
data class ToolCall(
    val tool: String,
    val args: Map<String, Any?> = emptyMap()
)

/**
 * Result of executing an individual tool call.
 */
data class ToolResult(
    val tool: String,
    val success: Boolean,
    val summary: String,
    val data: Any? = null
)

/**
 * Executes agent tools against the local Room database and SharedPreferences.
 */
class AgentToolExecutor(
    private val taskDao: TaskDao,
    private val todoDao: TodoDao,
    private val habitDao: HabitDao,
    private val noteDao: NoteDao,
    private val schedulePrefsManager: SchedulePreferencesManager,
    private val geminiScheduleRepo: GeminiScheduleRepository,
    private val memoryManager: AgentMemoryManager,
    private val appContext: Context
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getTodayDateString(): String {
        return dateFormat.format(Calendar.getInstance().time)
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.trim().split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (_: Exception) {
            0
        }
    }

    private suspend fun triggerWidgetUpdate() {
        try {
            DayWidget().updateAll(appContext)
            val intent = android.content.Intent(appContext, com.example.day.widget.DayWidgetReceiver::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(appContext)
                val ids = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(appContext, com.example.day.widget.DayWidgetReceiver::class.java)
                )
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            appContext.sendBroadcast(intent)
        } catch (_: Exception) {
            // Non-fatal if glance receiver is not active
        }
    }

    suspend fun executeTool(call: ToolCall, apiKey: String? = null): ToolResult = withContext(Dispatchers.IO) {
        val toolName = call.tool.lowercase(Locale.ROOT).trim()
        val args = call.args

        try {
            when (toolName) {
                "add_task" -> handleAddTask(args)
                "delete_task" -> handleDeleteTask(args)
                "complete_task" -> handleCompleteTask(args)
                "reschedule_task" -> handleRescheduleTask(args)
                "add_todo" -> handleAddTodo(args)
                "delete_todo" -> handleDeleteTodo(args)
                "complete_todo" -> handleCompleteTodo(args)
                "add_habit" -> handleAddHabit(args)
                "delete_habit" -> handleDeleteHabit(args)
                "add_note" -> handleAddNote(args)
                "delete_note" -> handleDeleteNote(args)
                "save_timetable" -> handleSaveTimetable(args)
                "generate_schedule" -> handleGenerateSchedule(args, apiKey)
                "clear_schedule" -> handleClearSchedule(args)
                else -> ToolResult(
                    tool = call.tool,
                    success = false,
                    summary = "Unknown tool: ${call.tool}"
                )
            }
        } catch (e: Exception) {
            ToolResult(
                tool = call.tool,
                success = false,
                summary = "Error running ${call.tool}: ${e.localizedMessage}"
            )
        }
    }

    private suspend fun handleAddTask(args: Map<String, Any?>): ToolResult {
        val name = (args["name"] ?: args["title"])?.toString()?.trim() ?: "Task"
        val startTime = (args["start_time"] ?: args["startTime"])?.toString()?.trim() ?: "09:00"
        val endTime = (args["end_time"] ?: args["endTime"])?.toString()?.trim() ?: "10:00"
        val date = (args["date"])?.toString()?.trim()?.ifBlank { null } ?: getTodayDateString()
        val category = (args["category"])?.toString()?.trim() ?: "General"
        val colorHex = (args["color_hex"] ?: args["colorHex"])?.toString()?.trim() ?: "#436B52"
        val habitId = (args["habit_id"] ?: args["habitId"])?.let {
            (it as? Number)?.toInt() ?: it.toString().toIntOrNull()
        }

        val startMin = parseTimeToMinutes(startTime)
        val endMin = parseTimeToMinutes(endTime)
        val duration = if (endMin >= startMin) endMin - startMin else 60

        val task = Task(
            name = name,
            startTime = startTime,
            endTime = endTime,
            colorHex = colorHex,
            isCompleted = false,
            date = date,
            category = category,
            plannedDurationMinutes = duration,
            actualDurationMinutes = duration,
            habitId = habitId
        )

        val id = taskDao.insertTask(task)
        triggerWidgetUpdate()
        return ToolResult(
            tool = "add_task",
            success = true,
            summary = "✓ Added task '$name' ($startTime - $endTime)",
            data = id
        )
    }

    private suspend fun handleDeleteTask(args: Map<String, Any?>): ToolResult {
        val id = (args["task_id"] ?: args["id"])?.let {
            (it as? Number)?.toInt() ?: it.toString().toIntOrNull()
        }
        val name = (args["task_name"] ?: args["name"])?.toString()?.trim()
        val date = args["date"]?.toString()?.trim()

        val taskToDelete: Task? = if (id != null) {
            taskDao.getTaskById(id)
        } else if (!name.isNullOrBlank()) {
            val allTasks = if (!date.isNullOrBlank()) taskDao.getTasksForDateSync(date) else taskDao.getAllTasksSync()
            allTasks.firstOrNull { it.name.contains(name, ignoreCase = true) }
        } else null

        return if (taskToDelete != null) {
            taskDao.deleteTask(taskToDelete)
            triggerWidgetUpdate()
            ToolResult(
                tool = "delete_task",
                success = true,
                summary = "✗ Deleted task '${taskToDelete.name}'"
            )
        } else {
            ToolResult(
                tool = "delete_task",
                success = false,
                summary = "Could not find task to delete"
            )
        }
    }

    private suspend fun handleCompleteTask(args: Map<String, Any?>): ToolResult {
        val id = (args["task_id"] ?: args["id"])?.let {
            (it as? Number)?.toInt() ?: it.toString().toIntOrNull()
        }
        val name = (args["task_name"] ?: args["name"])?.toString()?.trim()
        val date = args["date"]?.toString()?.trim()

        val task: Task? = if (id != null) {
            taskDao.getTaskById(id)
        } else if (!name.isNullOrBlank()) {
            val allTasks = if (!date.isNullOrBlank()) taskDao.getTasksForDateSync(date) else taskDao.getAllTasksSync()
            allTasks.firstOrNull { it.name.contains(name, ignoreCase = true) }
        } else null

        return if (task != null) {
            val updated = task.copy(isCompleted = true)
            taskDao.updateTask(updated)

            // If task was tied to a habit, log the completion for habit analytics
            if (task.habitId != null) {
                habitDao.insertHabitLog(HabitLog(habitId = task.habitId, date = task.date))
            }
            triggerWidgetUpdate()
            ToolResult(
                tool = "complete_task",
                success = true,
                summary = "✓ Completed task '${task.name}'"
            )
        } else {
            ToolResult(
                tool = "complete_task",
                success = false,
                summary = "Could not find task to complete"
            )
        }
    }

    private suspend fun handleRescheduleTask(args: Map<String, Any?>): ToolResult {
        val id = (args["task_id"] ?: args["id"])?.let {
            (it as? Number)?.toInt() ?: it.toString().toIntOrNull()
        }
        val name = (args["task_name"] ?: args["name"])?.toString()?.trim()
        val newStartTime = (args["new_start_time"] ?: args["newStartTime"] ?: args["start_time"] ?: args["startTime"])?.toString()?.trim()
        val newEndTime = (args["new_end_time"] ?: args["newEndTime"] ?: args["end_time"] ?: args["endTime"])?.toString()?.trim()
        val newDate = (args["new_date"] ?: args["newDate"] ?: args["date"])?.toString()?.trim()

        val task: Task? = if (id != null) {
            taskDao.getTaskById(id)
        } else if (!name.isNullOrBlank()) {
            taskDao.getAllTasksSync().firstOrNull { it.name.contains(name, ignoreCase = true) }
        } else null

        if (task == null) {
            return ToolResult(tool = "reschedule_task", success = false, summary = "Could not find task to reschedule")
        }

        val updatedStartTime = newStartTime ?: task.startTime
        val updatedEndTime = newEndTime ?: task.endTime
        val updatedDate = newDate ?: task.date
        val duration = parseTimeToMinutes(updatedEndTime) - parseTimeToMinutes(updatedStartTime)

        val updated = task.copy(
            startTime = updatedStartTime,
            endTime = updatedEndTime,
            date = updatedDate,
            plannedDurationMinutes = if (duration > 0) duration else task.plannedDurationMinutes,
            actualDurationMinutes = if (duration > 0) duration else task.actualDurationMinutes
        )

        taskDao.updateTask(updated)
        triggerWidgetUpdate()
        return ToolResult(
            tool = "reschedule_task",
            success = true,
            summary = "↻ Rescheduled '${task.name}' to $updatedStartTime - $updatedEndTime"
        )
    }

    private suspend fun handleAddTodo(args: Map<String, Any?>): ToolResult {
        val title = (args["title"] ?: args["name"])?.toString()?.trim() ?: "Todo"
        val dueDate = (args["due_date"] ?: args["dueDate"] ?: args["date"])?.toString()?.trim()?.ifBlank { null }
        val dueTime = (args["due_time"] ?: args["dueTime"] ?: args["time"])?.toString()?.trim()?.ifBlank { null }
        val priority = (args["priority"])?.let {
            (it as? Number)?.toInt() ?: it.toString().toIntOrNull()
        } ?: 2
        val recurrenceType = (args["recurrence_type"] ?: args["recurrenceType"])?.toString()?.trim()?.uppercase(Locale.ROOT)
        val recurrenceDays = (args["recurrence_days"] ?: args["recurrenceDays"])?.toString()?.trim()

        val todo = Todo(
            title = title,
            dueDate = dueDate,
            dueTime = dueTime,
            priority = priority.coerceIn(1, 3),
            isCompleted = false,
            recurrenceType = recurrenceType,
            recurrenceDays = recurrenceDays
        )

        val id = todoDao.insertTodo(todo)
        triggerWidgetUpdate()
        return ToolResult(
            tool = "add_todo",
            success = true,
            summary = "✓ Added todo '$title'",
            data = id
        )
    }

    private suspend fun handleDeleteTodo(args: Map<String, Any?>): ToolResult {
        val id = (args["todo_id"] ?: args["id"])?.let {
            (it as? Number)?.toInt() ?: it.toString().toIntOrNull()
        }
        val title = (args["title"] ?: args["name"])?.toString()?.trim()

        val todo: Todo? = if (id != null) {
            todoDao.getTodoById(id)
        } else if (!title.isNullOrBlank()) {
            val all = todoDao.getTodosForWidgetSync(getTodayDateString())
            all.firstOrNull { it.title.contains(title, ignoreCase = true) }
        } else null

        return if (todo != null) {
            todoDao.deleteTodo(todo)
            triggerWidgetUpdate()
            ToolResult(
                tool = "delete_todo",
                success = true,
                summary = "✗ Deleted todo '${todo.title}'"
            )
        } else {
            ToolResult(tool = "delete_todo", success = false, summary = "Could not find todo to delete")
        }
    }

    private suspend fun handleCompleteTodo(args: Map<String, Any?>): ToolResult {
        val id = (args["todo_id"] ?: args["id"])?.let {
            (it as? Number)?.toInt() ?: it.toString().toIntOrNull()
        }
        val title = (args["title"] ?: args["name"])?.toString()?.trim()

        val todo: Todo? = if (id != null) {
            todoDao.getTodoById(id)
        } else if (!title.isNullOrBlank()) {
            val all = todoDao.getTodosForWidgetSync(getTodayDateString())
            all.firstOrNull { it.title.contains(title, ignoreCase = true) }
        } else null

        if (todo == null) {
            return ToolResult(tool = "complete_todo", success = false, summary = "Could not find todo to complete")
        }

        return if (RecurrenceHelper.isRecurring(todo)) {
            val nextDate = RecurrenceHelper.getNextDueDate(todo)
            val updated = todo.copy(dueDate = nextDate, isCompleted = false)
            todoDao.updateTodo(updated)
            triggerWidgetUpdate()
            ToolResult(
                tool = "complete_todo",
                success = true,
                summary = "✓ Completed '${todo.title}' (next: $nextDate)"
            )
        } else {
            val updated = todo.copy(isCompleted = true)
            todoDao.updateTodo(updated)
            triggerWidgetUpdate()
            ToolResult(
                tool = "complete_todo",
                success = true,
                summary = "✓ Completed todo '${todo.title}'"
            )
        }
    }

    private suspend fun handleAddHabit(args: Map<String, Any?>): ToolResult {
        val name = (args["name"] ?: args["title"])?.toString()?.trim() ?: "Habit"
        val category = (args["category"])?.toString()?.trim() ?: "Skill"
        val colorHex = (args["color_hex"] ?: args["colorHex"])?.toString()?.trim() ?: "#81C784"
        val targetHours = (args["target_hours"] ?: args["targetHours"])?.let {
            (it as? Number)?.toDouble() ?: it.toString().toDoubleOrNull()
        } ?: 1.0

        val habit = Habit(
            name = name,
            category = category,
            colorHex = colorHex,
            targetHoursPerDay = targetHours
        )

        val id = habitDao.insertHabit(habit)
        triggerWidgetUpdate()
        return ToolResult(
            tool = "add_habit",
            success = true,
            summary = "✓ Added habit '$name'",
            data = id
        )
    }

    private suspend fun handleDeleteHabit(args: Map<String, Any?>): ToolResult {
        val id = (args["habit_id"] ?: args["id"])?.let {
            (it as? Number)?.toInt() ?: it.toString().toIntOrNull()
        }
        val name = (args["name"] ?: args["title"])?.toString()?.trim()

        val habit: Habit? = if (id != null) {
            habitDao.getHabitById(id)
        } else if (!name.isNullOrBlank()) {
            val all = habitDao.getAllHabitsSync()
            all.firstOrNull { it.name.contains(name, ignoreCase = true) }
        } else null

        return if (habit != null) {
            habitDao.deleteHabit(habit)
            triggerWidgetUpdate()
            ToolResult(
                tool = "delete_habit",
                success = true,
                summary = "✗ Deleted habit '${habit.name}'"
            )
        } else {
            ToolResult(tool = "delete_habit", success = false, summary = "Could not find habit to delete")
        }
    }

    private suspend fun handleAddNote(args: Map<String, Any?>): ToolResult {
        val title = (args["title"] ?: args["name"])?.toString()?.trim() ?: "Note"
        val content = (args["content"] ?: args["body"] ?: args["text"])?.toString()?.trim() ?: ""
        val colorHex = (args["color_hex"] ?: args["colorHex"])?.toString()?.trim() ?: "#FFF9C4"

        val note = Note(
            title = title,
            content = content,
            colorHex = colorHex
        )

        val id = noteDao.insertNote(note)
        triggerWidgetUpdate()
        return ToolResult(
            tool = "add_note",
            success = true,
            summary = "✓ Saved note '$title'",
            data = id
        )
    }

    private suspend fun handleDeleteNote(args: Map<String, Any?>): ToolResult {
        val id = (args["note_id"] ?: args["id"])?.let {
            (it as? Number)?.toInt() ?: it.toString().toIntOrNull()
        }
        val title = (args["title"] ?: args["name"])?.toString()?.trim()

        val note: Note? = if (id != null) {
            noteDao.getNoteById(id)
        } else if (!title.isNullOrBlank()) {
            val all = noteDao.getNotesSync()
            all.firstOrNull { it.title.contains(title, ignoreCase = true) }
        } else null

        return if (note != null) {
            noteDao.deleteNote(note)
            triggerWidgetUpdate()
            ToolResult(
                tool = "delete_note",
                success = true,
                summary = "✗ Deleted note '${note.title}'"
            )
        } else {
            ToolResult(tool = "delete_note", success = false, summary = "Could not find note to delete")
        }
    }

    private fun handleSaveTimetable(args: Map<String, Any?>): ToolResult {
        val text = (args["timetable_text"] ?: args["timetable"] ?: args["text"])?.toString()?.trim() ?: ""
        val currentPrefs = schedulePrefsManager.getSchedulePreferences()
        schedulePrefsManager.saveSchedulePreferences(currentPrefs.copy(weeklyTimetable = text))
        memoryManager.updateTimetable(text)
        return ToolResult(
            tool = "save_timetable",
            success = true,
            summary = "✓ Updated weekly timetable"
        )
    }

    private suspend fun handleGenerateSchedule(args: Map<String, Any?>, apiKey: String?): ToolResult {
        val date = (args["date"])?.toString()?.trim()?.ifBlank { null } ?: getTodayDateString()
        val prefs = schedulePrefsManager.getSchedulePreferences()
        val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(
            try { dateFormat.parse(date) ?: Calendar.getInstance().time } catch (_: Exception) { Calendar.getInstance().time }
        )

        val existingTasks = taskDao.getTasksForDateSync(date)
        val todayTodos = todoDao.getTodosForDateSync(date)
        val habits = habitDao.getAllHabitsSync()

        val items = geminiScheduleRepo.generateDaySchedule(
            apiKey = apiKey,
            prefs = prefs,
            dayOfWeek = dayOfWeek,
            dateString = date,
            existingTasks = existingTasks,
            todayTodos = todayTodos,
            habits = habits
        )

        if (items.isEmpty()) {
            return ToolResult(
                tool = "generate_schedule",
                success = false,
                summary = "Could not generate schedule (API key or preferences missing)"
            )
        }

        val existingNames = existingTasks.map { it.name.trim().lowercase(Locale.ROOT) }.toSet()
        val currentDayTasks = existingTasks.toMutableList()
        var habitsAdded = 0
        var count = 0

        for (item in items) {
            val cleanName = item.name.trim()
            if (existingNames.contains(cleanName.lowercase(Locale.ROOT))) continue

            val itemStartMin = parseTimeToMinutes(item.startTime)
            val itemEndMin = parseTimeToMinutes(item.endTime)
            if (itemEndMin <= itemStartMin) continue

            // Avoid overlap with any existing or already added tasks
            val hasOverlap = currentDayTasks.any { task ->
                val tStart = parseTimeToMinutes(task.startTime)
                val tEnd = parseTimeToMinutes(task.endTime)
                maxOf(tStart, itemStartMin) < minOf(tEnd, itemEndMin)
            }
            if (hasOverlap) continue

            val isHabit = item.type.equals("habit", ignoreCase = true)
            if (isHabit) {
                if (habitsAdded >= 2) continue
                val alreadyHasHabit = currentDayTasks.any {
                    it.name.equals(cleanName, ignoreCase = true) ||
                    (item.habitName != null && it.name.equals(item.habitName, ignoreCase = true))
                }
                if (alreadyHasHabit) continue
            }

            val plannedMin = itemEndMin - itemStartMin
            val matchedHabit = if (isHabit) {
                habits.firstOrNull { it.name.contains(cleanName, ignoreCase = true) }
            } else null

            val task = Task(
                name = cleanName,
                startTime = item.startTime,
                endTime = item.endTime,
                colorHex = matchedHabit?.colorHex ?: "#436B52",
                isCompleted = false,
                date = date,
                category = item.type.replaceFirstChar { it.uppercase() },
                plannedDurationMinutes = if (plannedMin > 0) plannedMin else 60,
                actualDurationMinutes = if (plannedMin > 0) plannedMin else 60,
                habitId = matchedHabit?.id
            )
            taskDao.insertTask(task)
            currentDayTasks.add(task)
            if (isHabit) habitsAdded++
            count++
        }

        triggerWidgetUpdate()
        return ToolResult(
            tool = "generate_schedule",
            success = true,
            summary = "✓ Generated daily schedule for $date ($count tasks)"
        )
    }

    private suspend fun handleClearSchedule(args: Map<String, Any?>): ToolResult {
        val date = (args["date"])?.toString()?.trim()?.ifBlank { null } ?: getTodayDateString()
        val existingTasks = taskDao.getTasksForDateSync(date)
        for (t in existingTasks) {
            taskDao.deleteTask(t)
        }
        triggerWidgetUpdate()
        return ToolResult(
            tool = "clear_schedule",
            success = true,
            summary = "✗ Cleared schedule for $date (${existingTasks.size} tasks removed)"
        )
    }
}
