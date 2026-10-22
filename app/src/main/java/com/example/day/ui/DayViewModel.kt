package com.example.day.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.day.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class DayViewModel(
    private val taskDao: TaskDao,
    private val todoDao: TodoDao,
    private val habitDao: HabitDao,
    private val noteDao: NoteDao,
    val quoteManager: QuoteManager,
    private val appContext: Context
) : ViewModel() {

    private val prefs = appContext.getSharedPreferences("day_app_prefs", Context.MODE_PRIVATE)

    val selectedDate = MutableStateFlow(getTodayDateString())

    // Tasks for the selected date on Today schedule
    val tasks: StateFlow<List<Task>> = selectedDate
        .flatMapLatest { date -> taskDao.getTasksForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<Task>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Todos
    val activeTodos: StateFlow<List<Todo>> = todoDao.getActiveTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTodos: StateFlow<List<Todo>> = todoDao.getCompletedTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTodos: StateFlow<List<Todo>> = todoDao.getAllTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Todos matching the selected date (Bug 2: Todo -> Today reflection, shows active and completed)
    val todayTodos: StateFlow<List<Todo>> = combine(selectedDate, allTodos) { date, todos ->
        todos.filter { it.dueDate == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Habits (at rest, no time attached)
    val habits: StateFlow<List<Habit>> = habitDao.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitLogs: StateFlow<List<HabitLog>> = habitDao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyStreak: StateFlow<Int> = combine(
        taskDao.getCompletedTasks(),
        habitDao.getAllLogs()
    ) { tasks, logs ->
        val completedDates = mutableSetOf<String>()
        tasks.forEach { completedDates.add(it.date) }
        logs.forEach { completedDates.add(it.date) }
        calculateDayStreak(completedDates)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Notes
    val notes: StateFlow<List<Note>> = noteDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Templates (legacy master templates support)
    val templateTasks: StateFlow<List<TemplateTask>> = taskDao.getAllTemplateTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Quotes
    val currentQuote = MutableStateFlow("")
    val isDailyQuoteEnabled = MutableStateFlow(true)
    val personalQuote = MutableStateFlow("")

    // Gemini & Theme Preferences
    private val geminiRepository = GeminiRepository()
    val geminiApiKey = MutableStateFlow(prefs.getString("gemini_api_key", "") ?: "")
    val appPrimaryHex = MutableStateFlow(prefs.getString("app_primary_hex", "#436B52") ?: "#436B52")
    val widgetPrimaryHex = MutableStateFlow(prefs.getString("widget_primary_hex", "#436B52") ?: "#436B52")
    val appDarkMode = MutableStateFlow(prefs.getBoolean("app_dark_mode", false))
    val widgetDarkMode = MutableStateFlow(prefs.getBoolean("widget_dark_mode", false))

    // AI Schedule Preferences & Repository (Feature 2)
    private val schedulePreferencesManager = SchedulePreferencesManager(appContext)
    val schedulePreferences: StateFlow<SchedulePreferences> = schedulePreferencesManager.schedulePreferences
    val isGeneratingSchedule = MutableStateFlow(false)
    val scheduleSnackbarMessage = MutableSharedFlow<String>()
    private val geminiScheduleRepository = GeminiScheduleRepository(appContext)

    // Editing State (for overlays or direct deep-links)
    val editTaskId = MutableStateFlow<Int>(-1)
    val editTodo = MutableStateFlow<Todo?>(null)

    // Agent PA Chat ViewModel (Feature 2B)
    val agentViewModel: com.example.day.ui.agent.DayAgentViewModel by lazy {
        com.example.day.ui.agent.DayAgentViewModel(
            taskDao = taskDao,
            todoDao = todoDao,
            habitDao = habitDao,
            noteDao = noteDao,
            schedulePrefsManager = schedulePreferencesManager,
            geminiScheduleRepo = geminiScheduleRepository,
            appContext = appContext
        )
    }

    init {
        isDailyQuoteEnabled.value = quoteManager.isDailyQuoteEnabled()
        personalQuote.value = quoteManager.getPersonalQuote()
        refreshQuote()

        // Observe selectedDate and initialize template tasks if needed
        viewModelScope.launch {
            selectedDate.collect { date ->
                checkAndInitializeDate(date)
            }
        }

        // Trigger AI Auto-Schedule on startup (Feature 2D)
        checkAndRunAutoSchedule()
    }

    fun saveGeminiApiKey(key: String) {
        geminiApiKey.value = key.trim()
        prefs.edit().putString("gemini_api_key", key.trim()).apply()
    }

    suspend fun validateGeminiKey(key: String): Result<Boolean> {
        return geminiRepository.validateApiKey(key)
    }

    fun saveAppThemeSettings(primaryHex: String, isDark: Boolean) {
        appPrimaryHex.value = primaryHex
        appDarkMode.value = isDark
        prefs.edit()
            .putString("app_primary_hex", primaryHex)
            .putBoolean("app_dark_mode", isDark)
            .apply()
    }

    fun saveWidgetThemeSettings(primaryHex: String, isDark: Boolean) {
        widgetPrimaryHex.value = primaryHex
        widgetDarkMode.value = isDark
        prefs.edit()
            .putString("widget_primary_hex", primaryHex)
            .putBoolean("widget_dark_mode", isDark)
            .apply()
        updateWidget()
    }

    fun processSmartInput(text: String, onSuccess: (String) -> Unit = {}, onError: (String) -> Unit = {}) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val result = geminiRepository.parseInput(geminiApiKey.value, text)
                when (result) {
                    is ParsedResult.TaskResult -> {
                        val plannedMin = parseTimeToMinutes(result.endTime) - parseTimeToMinutes(result.startTime)
                        val task = Task(
                            name = result.title,
                            startTime = result.startTime,
                            endTime = result.endTime,
                            colorHex = appPrimaryHex.value,
                            date = selectedDate.value,
                            category = "Task",
                            plannedDurationMinutes = plannedMin,
                            actualDurationMinutes = plannedMin
                        )
                        taskDao.insertTask(task)
                        onSuccess("Added task: '${result.title}'")
                    }
                    is ParsedResult.TodoResult -> {
                        val todo = Todo(
                            title = result.title,
                            dueDate = result.dueDate ?: selectedDate.value,
                            dueTime = result.dueTime,
                            priority = when (result.priority.uppercase()) {
                                "P1", "1" -> 1
                                "P2", "2" -> 2
                                "P4", "4" -> 4
                                else -> 3
                            },
                            recurrenceType = result.recurrenceType,
                            recurrenceDays = result.recurrenceDays
                        )
                        todoDao.insertTodo(todo)
                        onSuccess("Added to-do: '${result.title}'")
                    }
                    is ParsedResult.HabitResult -> {
                        val habit = Habit(
                            name = result.name,
                            category = result.category,
                            colorHex = appPrimaryHex.value
                        )
                        habitDao.insertHabit(habit)
                        onSuccess("Added habit: '${result.name}'")
                    }
                    is ParsedResult.NoteResult -> {
                        val note = Note(
                            title = result.title,
                            content = result.content,
                            colorHex = "#FFF9C4"
                        )
                        noteDao.insertNote(note)
                        onSuccess("Added note: '${result.title}'")
                    }
                }
                updateWidget()
            } catch (e: Exception) {
                onError("Error parsing input: ${e.localizedMessage}")
            }
        }
    }

    suspend fun parseSmartInputMulti(text: String): List<ParsedResult> {
        return geminiRepository.parseMultipleInputs(geminiApiKey.value, text)
    }

    fun addParsedItems(items: List<ParsedResult>, onFinished: (String) -> Unit) {
        viewModelScope.launch {
            var count = 0
            for (res in items) {
                when (res) {
                    is ParsedResult.TaskResult -> {
                        val plannedMin = parseTimeToMinutes(res.endTime) - parseTimeToMinutes(res.startTime)
                        val task = Task(
                            name = res.title,
                            startTime = res.startTime,
                            endTime = res.endTime,
                            colorHex = appPrimaryHex.value,
                            date = selectedDate.value,
                            category = "Task",
                            plannedDurationMinutes = plannedMin,
                            actualDurationMinutes = plannedMin
                        )
                        taskDao.insertTask(task)
                        count++
                    }
                    is ParsedResult.TodoResult -> {
                        val todo = Todo(
                            title = res.title,
                            dueDate = res.dueDate ?: selectedDate.value,
                            dueTime = res.dueTime,
                            priority = when (res.priority.uppercase()) {
                                "P1", "1" -> 1
                                "P2", "2" -> 2
                                "P4", "4" -> 4
                                else -> 3
                            },
                            isCompleted = false,
                            recurrenceType = res.recurrenceType,
                            recurrenceDays = res.recurrenceDays
                        )
                        todoDao.insertTodo(todo)
                        count++
                    }
                    is ParsedResult.HabitResult -> {
                        val habit = Habit(
                            name = res.name,
                            category = res.category,
                            colorHex = appPrimaryHex.value
                        )
                        habitDao.insertHabit(habit)
                        count++
                    }
                    is ParsedResult.NoteResult -> {
                        val note = Note(
                            title = res.title,
                            content = res.content,
                            colorHex = "#FFF9C4"
                        )
                        noteDao.insertNote(note)
                        count++
                    }
                }
            }
            updateWidget()
            onFinished("Added $count item${if (count > 1) "s" else ""} ✓")
        }
    }

    // ── AI Auto-Schedule Logic (Feature 2) ──────────────────────────────────

    fun saveSchedulePreferences(preferences: SchedulePreferences) {
        schedulePreferencesManager.saveSchedulePreferences(preferences)
    }

    fun checkAndRunAutoSchedule() {
        val prefs = schedulePreferences.value
        if (!prefs.autoScheduleEnabled) return

        val todayStr = getTodayDateString()
        if (prefs.lastAutoScheduleDate == todayStr) return

        val nowCal = Calendar.getInstance()
        val currentMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
        val wakeUpMinutes = parseTimeToMinutes(prefs.wakeUpTime)
        if (currentMinutes < wakeUpMinutes) return

        runAutoScheduleInternal(todayStr)
    }

    fun regenerateTodaySchedule() {
        val todayStr = getTodayDateString()
        runAutoScheduleInternal(todayStr, isRegeneration = true)
    }

    private fun runAutoScheduleInternal(targetDate: String, isRegeneration: Boolean = false) {
        viewModelScope.launch {
            if (isGeneratingSchedule.value) return@launch
            isGeneratingSchedule.value = true

            try {
                val dayOfWeek = SimpleDateFormat("EEEE", Locale.US).format(Calendar.getInstance().time)
                val existingTodayTasks = taskDao.getTasksForDateSync(targetDate)
                val currentTodayTodos = todoDao.getTodosForDateSync(targetDate).filter { !it.isCompleted }
                val currentHabits = habitDao.getAllHabitsSync()

                if (isRegeneration) {
                    val toDelete = existingTodayTasks.filter { it.todoId == null && it.templateTaskId == null }
                    toDelete.forEach { taskDao.deleteTask(it) }
                }

                val refreshedExistingTasks = taskDao.getTasksForDateSync(targetDate)
                val existingNames = refreshedExistingTasks.map { it.name.trim().lowercase(Locale.ROOT) }.toSet()

                val scheduledItems = geminiScheduleRepository.generateDaySchedule(
                    apiKey = geminiApiKey.value,
                    prefs = schedulePreferences.value,
                    dayOfWeek = dayOfWeek,
                    dateString = targetDate,
                    existingTasks = refreshedExistingTasks,
                    todayTodos = currentTodayTodos,
                    habits = currentHabits
                )

                if (scheduledItems.isNotEmpty()) {
                    val habitMap = currentHabits.associateBy { it.name.trim().lowercase(Locale.ROOT) }

                    for (item in scheduledItems) {
                        val cleanName = item.name.trim()
                        if (existingNames.contains(cleanName.lowercase(Locale.ROOT))) {
                            continue
                        }

                        val taskColor = appPrimaryHex.value

                        when (item.type.lowercase(Locale.ROOT)) {
                            "habit" -> {
                                val matchedHabit = item.habitName?.let { habitMap[it.lowercase(Locale.ROOT)] }
                                    ?: habitMap[cleanName.lowercase(Locale.ROOT)]
                                val plannedMin = parseTimeToMinutes(item.endTime) - parseTimeToMinutes(item.startTime)
                                val task = Task(
                                    name = cleanName,
                                    startTime = item.startTime,
                                    endTime = item.endTime,
                                    colorHex = matchedHabit?.colorHex ?: taskColor,
                                    isCompleted = false,
                                    date = targetDate,
                                    category = matchedHabit?.category ?: "Habit",
                                    habitId = matchedHabit?.id,
                                    plannedDurationMinutes = plannedMin,
                                    actualDurationMinutes = plannedMin
                                )
                                taskDao.insertTask(task)
                            }
                            "break" -> {
                                val plannedMin = parseTimeToMinutes(item.endTime) - parseTimeToMinutes(item.startTime)
                                val task = Task(
                                    name = "Break 🍵",
                                    startTime = item.startTime,
                                    endTime = item.endTime,
                                    colorHex = "#90A4AE",
                                    isCompleted = false,
                                    date = targetDate,
                                    category = "Break",
                                    plannedDurationMinutes = plannedMin,
                                    actualDurationMinutes = plannedMin
                                )
                                taskDao.insertTask(task)
                            }
                            "class" -> {
                                val plannedMin = parseTimeToMinutes(item.endTime) - parseTimeToMinutes(item.startTime)
                                val task = Task(
                                    name = cleanName,
                                    startTime = item.startTime,
                                    endTime = item.endTime,
                                    colorHex = "#5C6BC0",
                                    isCompleted = false,
                                    date = targetDate,
                                    category = "Class",
                                    plannedDurationMinutes = plannedMin,
                                    actualDurationMinutes = plannedMin
                                )
                                taskDao.insertTask(task)
                            }
                            else -> {
                                val plannedMin = parseTimeToMinutes(item.endTime) - parseTimeToMinutes(item.startTime)
                                val task = Task(
                                    name = cleanName,
                                    startTime = item.startTime,
                                    endTime = item.endTime,
                                    colorHex = taskColor,
                                    isCompleted = false,
                                    date = targetDate,
                                    category = "Task",
                                    plannedDurationMinutes = plannedMin,
                                    actualDurationMinutes = plannedMin
                                )
                                taskDao.insertTask(task)
                            }
                        }
                    }

                    schedulePreferencesManager.updateLastAutoScheduleDate(targetDate)
                    scheduleSnackbarMessage.emit("Schedule generated for today ✓")
                    updateWidget()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isGeneratingSchedule.value = false
            }
        }
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }

    fun refreshQuote() {
        currentQuote.value = quoteManager.getCurrentQuote()
    }

    fun changeDate(date: String) {
        selectedDate.value = date
    }



    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            h * 60 + m
        } catch (e: Exception) {
            0
        }
    }

    private fun checkAndInitializeDate(date: String) {
        viewModelScope.launch {
            if (!quoteManager.isDateInitialized(date)) {
                // Auto-fill template tasks for new days if any exist
                val templates = taskDao.getAllTemplateTasksSync()
                templates.forEach { template ->
                    val plannedMin = parseTimeToMinutes(template.endTime) - parseTimeToMinutes(template.startTime)
                    val task = Task(
                        name = template.name,
                        startTime = template.startTime,
                        endTime = template.endTime,
                        colorHex = template.colorHex,
                        isCompleted = false,
                        date = date,
                        templateTaskId = template.id,
                        category = template.category,
                        plannedDurationMinutes = plannedMin,
                        actualDurationMinutes = plannedMin
                    )
                    taskDao.insertTask(task)
                }

                // If today is a date matching recurring todos that are active, they are naturally due
                quoteManager.setDateInitialized(date)
                updateWidget()
            }
        }
    }

    // ── Task Operations (Today's Schedule) ──────────────────────────────────

    fun addTask(
        name: String,
        startTime: String,
        endTime: String,
        colorHex: String,
        category: String = "General",
        todoId: Int? = null,
        habitId: Int? = null
    ) {
        viewModelScope.launch {
            val plannedMin = parseTimeToMinutes(endTime) - parseTimeToMinutes(startTime)
            val task = Task(
                name = name,
                startTime = startTime,
                endTime = endTime,
                colorHex = colorHex,
                date = selectedDate.value,
                templateTaskId = null,
                category = category,
                plannedDurationMinutes = plannedMin,
                actualDurationMinutes = plannedMin,
                todoId = todoId,
                habitId = habitId
            )
            taskDao.insertTask(task)
            updateWidget()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            val actualMin = parseTimeToMinutes(task.endTime) - parseTimeToMinutes(task.startTime)
            val updatedTask = task.copy(actualDurationMinutes = actualMin)
            taskDao.updateTask(updatedTask)
            updateWidget()
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val nextState = !task.isCompleted
            val updatedTask = task.copy(isCompleted = nextState)
            taskDao.updateTask(updatedTask)

            // If linked to a habit, log or unlog habit completion
            if (task.habitId != null) {
                if (nextState) {
                    habitDao.insertHabitLog(HabitLog(habitId = task.habitId, date = task.date))
                } else {
                    habitDao.deleteHabitLog(task.habitId, task.date)
                }
            }

            // If linked to a todo, toggle todo completion accordingly
            if (task.todoId != null) {
                val todo = todoDao.getTodoById(task.todoId)
                if (todo != null) {
                    if (nextState) {
                        handleTodoCompletion(todo)
                    } else {
                        todoDao.updateTodo(todo.copy(isCompleted = false))
                    }
                }
            }

            updateWidget()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            updateWidget()
        }
    }

    // ── Todo Operations ─────────────────────────────────────────────────────

    fun addTodo(
        title: String,
        dueDate: String? = null,
        dueTime: String? = null,
        priority: Int = 2,
        recurrenceType: String? = null,
        recurrenceDays: String? = null
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val todo = Todo(
                title = title.trim(),
                dueDate = dueDate,
                dueTime = dueTime,
                priority = priority,
                isCompleted = false,
                recurrenceType = recurrenceType,
                recurrenceDays = recurrenceDays
            )
            todoDao.insertTodo(todo)
            updateWidget()
        }
    }

    fun updateTodo(todo: Todo) {
        viewModelScope.launch {
            todoDao.updateTodo(todo)
            updateWidget()
        }
    }

    fun completeTodo(todo: Todo) {
        viewModelScope.launch {
            handleTodoCompletion(todo)
            updateWidget()
        }
    }

    fun toggleTodoCompletion(todo: Todo) {
        viewModelScope.launch {
            if (todo.isCompleted) {
                // Uncomplete
                todoDao.updateTodo(todo.copy(isCompleted = false))
            } else {
                handleTodoCompletion(todo)
            }
            updateWidget()
        }
    }

    private suspend fun handleTodoCompletion(todo: Todo) {
        if (RecurrenceHelper.isRecurring(todo)) {
            // Recurring todo: calculate next due date and reset completion
            val nextDueDate = RecurrenceHelper.getNextDueDate(todo)
            val updated = todo.copy(
                dueDate = nextDueDate,
                isCompleted = false
            )
            todoDao.updateTodo(updated)
        } else {
            // Keep the completed todo in the database marked as completed (do not delete)
            todoDao.updateTodo(todo.copy(isCompleted = true))
        }
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            todoDao.deleteTodo(todo)
            updateWidget()
        }
    }

    fun addTodoToToday(todo: Todo, startTime: String, endTime: String) {
        viewModelScope.launch {
            val plannedMin = parseTimeToMinutes(endTime) - parseTimeToMinutes(startTime)
            val colorHex = when (todo.priority) {
                1 -> "#E57373" // High priority coral
                2 -> "#FFB74D" // Med priority amber
                else -> "#81C784" // Low priority sage
            }
            val task = Task(
                name = todo.title,
                startTime = startTime,
                endTime = endTime,
                colorHex = colorHex,
                date = getTodayDateString(),
                category = "Todo",
                todoId = todo.id,
                plannedDurationMinutes = plannedMin,
                actualDurationMinutes = plannedMin
            )
            taskDao.insertTask(task)
            updateWidget()
        }
    }

    // ── Habit Operations (At rest, no time attached) ────────────────────────

    fun addHabit(name: String, category: String = "Skill", colorHex: String = "#81C784") {
        if (name.isBlank()) return
        viewModelScope.launch {
            val habit = Habit(
                name = name.trim(),
                category = category,
                colorHex = colorHex
            )
            habitDao.insertHabit(habit)
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.updateHabit(habit)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.deleteHabit(habit)
        }
    }

    fun addHabitToToday(habit: Habit, startTime: String, endTime: String) {
        viewModelScope.launch {
            val plannedMin = parseTimeToMinutes(endTime) - parseTimeToMinutes(startTime)
            val task = Task(
                name = habit.name,
                startTime = startTime,
                endTime = endTime,
                colorHex = habit.colorHex,
                date = getTodayDateString(),
                category = "Habit",
                habitId = habit.id,
                plannedDurationMinutes = plannedMin,
                actualDurationMinutes = plannedMin
            )
            taskDao.insertTask(task)
            updateWidget()
        }
    }

    fun logHabitCompletion(habitId: Int, date: String = getTodayDateString()) {
        viewModelScope.launch {
            habitDao.insertHabitLog(HabitLog(habitId = habitId, date = date))
        }
    }

    // ── Note Operations ─────────────────────────────────────────────────────

    fun addNote(title: String, content: String, colorHex: String = "#FFF9C4") {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            val note = Note(
                title = title.trim(),
                content = content.trim(),
                colorHex = colorHex,
                updatedAt = System.currentTimeMillis()
            )
            noteDao.insertNote(note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }

    // ── Template Operations ─────────────────────────────────────────────────

    fun addTemplateTask(name: String, startTime: String, endTime: String, colorHex: String, category: String) {
        viewModelScope.launch {
            val template = TemplateTask(
                name = name,
                startTime = startTime,
                endTime = endTime,
                colorHex = colorHex,
                category = category
            )
            taskDao.insertTemplateTask(template)
        }
    }

    fun updateTemplateTask(template: TemplateTask) {
        viewModelScope.launch {
            taskDao.updateTemplateTask(template)
        }
    }

    fun deleteTemplateTask(template: TemplateTask) {
        viewModelScope.launch {
            taskDao.deleteTemplateTask(template)
        }
    }

    // ── Quotes & Preferences ────────────────────────────────────────────────

    fun updateQuoteSettings(isDaily: Boolean, personal: String) {
        quoteManager.setDailyQuoteEnabled(isDaily)
        quoteManager.setPersonalQuote(personal)
        isDailyQuoteEnabled.value = isDaily
        personalQuote.value = personal
        refreshQuote()
        updateWidget()
    }

    fun calculateDayStreak(completedDates: Set<String>): Int {
        if (completedDates.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val todayStr = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        val currentCal = when {
            completedDates.contains(todayStr) -> Calendar.getInstance()
            completedDates.contains(yesterdayStr) -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            else -> return 0
        }

        var streak = 0
        while (completedDates.contains(sdf.format(currentCal.time))) {
            streak++
            currentCal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    private fun updateWidget() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.day.widget.DayWidget().updateAll(appContext)
                val intent = Intent(appContext, com.example.day.widget.DayWidgetReceiver::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    val appWidgetManager = AppWidgetManager.getInstance(appContext)
                    val ids = appWidgetManager.getAppWidgetIds(
                        ComponentName(appContext, com.example.day.widget.DayWidgetReceiver::class.java)
                    )
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                appContext.sendBroadcast(intent)
            } catch (e: Exception) {
                // Ignore widget update errors
            }
        }
    }
}
