package com.example.day

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.day.data.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Phase3Tests {

    @Test
    fun testCacheableQueries() {
        val repo = AgentRepository()
        val query = "What's on my schedule today?"
        
        assertNull(repo.getCachedResponse(query))
        repo.cacheResponse(query, "You have 2 tasks scheduled today.")
        assertEquals("You have 2 tasks scheduled today.", repo.getCachedResponse(query))
        
        // Clear cache
        repo.clearCache()
        assertNull(repo.getCachedResponse(query))
    }

    @Test
    fun testBuildCurrentDataStringPayload() {
        val repo = AgentRepository()
        val payload = repo.buildCurrentDataString(
            todayStr = "2026-10-22",
            tasksToday = listOf(
                Task(id = 1, name = "DSA", startTime = "09:00", endTime = "10:00", colorHex = "#436B52", date = "2026-10-22")
            ),
            todosNext7Days = listOf(
                Todo(id = 1, title = "Pay bills", dueDate = "2026-10-24", priority = 1)
            ),
            habitsWith14dLogs = listOf(
                Pair(Habit(id = 1, name = "Gym", targetHoursPerDay = 1.0), 10)
            ),
            last3Notes = listOf(
                Note(id = 1, title = "Idea", content = "Build Day App")
            )
        )

        assertTrue(payload.contains("DSA"))
        assertTrue(payload.contains("Pay bills"))
        assertTrue(payload.contains("Gym"))
        assertTrue(payload.contains("10 logs/14d"))
        assertTrue(payload.contains("Build Day App"))
    }

    @Test
    fun testProactiveSuggestionEngineStreakAtRiskAndNeglected() = runBlocking {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = "2026-10-22"

        // Mock DAO implementations via subclassing / fake objects
        val fakeTaskDao = object : TaskDao {
            override fun getTasksForDate(date: String) = flowOf(emptyList<Task>())
            override suspend fun getTasksForDateSync(date: String) = emptyList<Task>()
            override fun getAllTasks() = flowOf(emptyList<Task>())
            override fun getCompletedTasks() = flowOf(emptyList<Task>())
            override suspend fun getAllTasksSync() = emptyList<Task>()
            override suspend fun getTaskById(id: Int): Task? = null
            override suspend fun insertTask(task: Task) = 1L
            override suspend fun updateTask(task: Task) {}
            override suspend fun deleteTask(task: Task) {}
            override fun getAllTemplateTasks() = flowOf(emptyList<TemplateTask>())
            override suspend fun getAllTemplateTasksSync() = emptyList<TemplateTask>()
            override suspend fun insertTemplateTask(task: TemplateTask) = 1L
            override suspend fun updateTemplateTask(task: TemplateTask) {}
            override suspend fun deleteTemplateTask(task: TemplateTask) {}
        }

        val fakeTodoDao = object : TodoDao {
            override fun getAllTodos() = flowOf(emptyList<Todo>())
            override fun getActiveTodos() = flowOf(emptyList<Todo>())
            override fun getCompletedTodos() = flowOf(emptyList<Todo>())
            override suspend fun getTodoById(id: Int): Todo? = null
            override suspend fun getTodosForDateSync(date: String) = emptyList<Todo>()
            override suspend fun getTodosForWidgetSync(date: String) = emptyList<Todo>()
            override suspend fun getActiveTodosSync() = listOf(
                Todo(id = 1, title = "Submit report", dueDate = "2026-10-20", isCompleted = false) // Overdue by 2 days
            )
            override suspend fun insertTodo(todo: Todo) = 1L
            override suspend fun updateTodo(todo: Todo) {}
            override suspend fun deleteTodo(todo: Todo) {}
            override suspend fun deleteTodoById(id: Int) {}
        }

        val fakeHabitDao = object : HabitDao {
            override fun getAllHabits() = flowOf(emptyList<Habit>())
            override suspend fun getAllHabitsSync() = listOf(
                Habit(id = 1, name = "Gym", targetHoursPerDay = 1.0),
                Habit(id = 2, name = "Reading", targetHoursPerDay = 0.5)
            )
            override suspend fun getHabitById(id: Int): Habit? = null
            override suspend fun insertHabit(habit: Habit) = 1L
            override suspend fun updateHabit(habit: Habit) {}
            override suspend fun deleteHabit(habit: Habit) {}
            override fun getLogsForHabit(habitId: Int) = flowOf(emptyList<HabitLog>())
            override fun getLogsForHabit(habitId: Int, startDate: String) = flowOf(emptyList<HabitLog>())
            override fun getDailyTotalsForHabit(habitId: Int, startDate: String) = flowOf(emptyList<DailyHabitTotal>())
            override suspend fun getDailyTotalsForHabitSync(habitId: Int, startDate: String) = emptyList<DailyHabitTotal>()
            override fun getAllLogs() = flowOf(emptyList<HabitLog>())
            override suspend fun getAllLogsSync() = listOf(
                // Gym has a streak from yesterday (2026-10-21) but not logged today
                HabitLog(id = 1, habitId = 1, date = "2026-10-21"),
                // Reading last logged 5 days ago (2026-10-17)
                HabitLog(id = 2, habitId = 2, date = "2026-10-17")
            )
            override fun getLogsForDate(date: String) = flowOf(emptyList<HabitLog>())
            override suspend fun insertHabitLog(log: HabitLog) = 1L
            override suspend fun deleteHabitLog(habitId: Int, date: String) {}
        }

        val engine = ProactiveSuggestionEngine(fakeTaskDao, fakeTodoDao, fakeHabitDao)
        val eveningContext = AppContext(
            todayDate = todayStr,
            currentHour = 19, // 7pm -> streak at risk triggers
            currentMinutes = 19 * 60,
            wakeUpTime = "07:00"
        )

        val suggestions = engine.generateSuggestions(eveningContext)
        assertTrue("Should produce suggestions", suggestions.isNotEmpty())

        val types = suggestions.map { it.type }
        assertTrue("Should include STREAK_AT_RISK", types.contains(SuggestionType.STREAK_AT_RISK))
        assertTrue("Should include OVERDUE_TODO", types.contains(SuggestionType.OVERDUE_TODO))
        assertTrue("Should include HABIT_NEGLECTED", types.contains(SuggestionType.HABIT_NEGLECTED))
    }

    @Test
    fun testDarkModeContrastWithCharcoalHex() {
        // When user sets app color to #363636, dark mode primary must NOT be #363636 (invisible)
        val scheme = com.example.day.theme.colorSchemeFromHex("#363636", isDark = true)
        val contrast = com.example.day.theme.calculateContrast(scheme.primary, scheme.surface)
        assertTrue("Contrast between primary and surface must be >= 4.0:1 in dark mode, was $contrast", contrast >= 4.0f)
        assertNotEquals("Primary color must not be equal to surface color", scheme.surface, scheme.primary)

        // Button text / content (onPrimary) must be clearly legible on primary
        val onPrimaryContrast = com.example.day.theme.calculateContrast(scheme.onPrimary, scheme.primary)
        assertTrue("onPrimary contrast against primary must be >= 4.5:1 (WCAG AA), was $onPrimaryContrast", onPrimaryContrast >= 4.5f)
    }

    @Test
    fun testDarkModeContrastWithDarkGreen() {
        // When user sets #436B52 (forest green), dark mode primary must be brightened for visibility
        val scheme = com.example.day.theme.colorSchemeFromHex("#436B52", isDark = true)
        val contrast = com.example.day.theme.calculateContrast(scheme.primary, scheme.surface)
        assertTrue("Contrast must be >= 4.0:1 in dark mode, was $contrast", contrast >= 4.0f)

        // Button text / content (onPrimary) must be clearly legible on primary
        val onPrimaryContrast = com.example.day.theme.calculateContrast(scheme.onPrimary, scheme.primary)
        assertTrue("onPrimary contrast against primary must be >= 4.5:1 (WCAG AA), was $onPrimaryContrast", onPrimaryContrast >= 4.5f)
    }

    @Test
    fun testWidgetButtonContrasts() {
        // In dark mode with #363636 adapted accent
        val rawAccent = com.example.day.theme.parseHexColor("#363636")
        val accent = com.example.day.theme.adaptAccentForDarkMode(rawAccent)

        // Active pill text (Today / Todo / Notes)
        val activeTextColor = if (accent.luminance() > 0.40f) androidx.compose.ui.graphics.Color(0xFF121212) else androidx.compose.ui.graphics.Color.White
        val activeContrast = com.example.day.theme.calculateContrast(activeTextColor, accent)
        assertTrue("Active widget tab text contrast must be >= 4.5:1, was $activeContrast", activeContrast >= 4.5f)

        // Inactive pill text on #2E2E2E (inactiveTabDrawable)
        val inactiveTabBg = androidx.compose.ui.graphics.Color(0xFF2E2E2E)
        val inactiveTextColor = androidx.compose.ui.graphics.Color(0xFFF0F0F0)
        val inactiveContrast = com.example.day.theme.calculateContrast(inactiveTextColor, inactiveTabBg)
        assertTrue("Inactive widget tab text contrast must be >= 7.0:1 (AAA), was $inactiveContrast", inactiveContrast >= 7.0f)
    }
}
