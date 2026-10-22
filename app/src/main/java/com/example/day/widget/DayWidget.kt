package com.example.day.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextDecoration
import androidx.glance.unit.ColorProvider
import com.example.day.MainActivity
import com.example.day.R
import com.example.day.data.AppDatabase
import com.example.day.data.HabitLog
import com.example.day.data.RecurrenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

val ACTIVE_SECTION_KEY = stringPreferencesKey("widget_active_section")

class DayWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val taskDao = db.taskDao()
        val todoDao = db.todoDao()
        val noteDao = db.noteDao()

        val appPrefs = context.getSharedPreferences("day_app_prefs", Context.MODE_PRIVATE)
        val isDark = appPrefs.getBoolean("widget_dark_mode", false)
        val primaryHex = appPrefs.getString("widget_primary_hex", "#436B52") ?: "#436B52"
        val accentColor = com.example.day.theme.parseHexColor(primaryHex)

        val quoteManager = com.example.day.data.QuoteManager(context)
        val quote = quoteManager.getCurrentQuote()

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val displayDate = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Calendar.getInstance().time)

        // Load tasks and todos for today
        val tasks = withContext(Dispatchers.IO) { taskDao.getTasksForDateSync(todayStr) }
        val todayTodos = withContext(Dispatchers.IO) { todoDao.getTodosForDateSync(todayStr) }
        val widgetTodos = withContext(Dispatchers.IO) { todoDao.getTodosForWidgetSync(todayStr) }
        val notes = withContext(Dispatchers.IO) {
            try { noteDao.getNotesSync() } catch (_: Exception) { emptyList() }
        }

        // Daily Score: matches the app Today screen exactly
        val totalTodayCount = tasks.size + todayTodos.size
        val completedTodayCount = tasks.count { it.isCompleted } + todayTodos.count { it.isCompleted }
        val dailyScore = if (totalTodayCount > 0) (completedTodayCount * 100) / totalTodayCount else 0

        // Neumorphic Theme Assets
        val bgDrawable = if (isDark) R.drawable.widget_surface_dark else R.drawable.widget_surface_light
        val cardDrawable = if (isDark) R.drawable.widget_card_dark else R.drawable.widget_card_light
        val insetDrawable = if (isDark) R.drawable.widget_inset_dark else R.drawable.widget_inset_light
        val inactiveTabDrawable = if (isDark) R.drawable.widget_tab_inactive_dark else R.drawable.widget_tab_inactive_light

        val textPrimary = if (isDark) Color(0xFFF0F0F0) else Color(0xFF4A5568)
        val textMuted = if (isDark) Color(0xFFA8A8A8) else Color(0xFFA0AEC0)

        provideContent {
            // Read active section reactively from Glance Preferences State
            val glancePrefs = currentState<Preferences>()
            val fallbackPrefs = context.getSharedPreferences("day_widget_prefs", Context.MODE_PRIVATE)
            val activeSection = glancePrefs[ACTIVE_SECTION_KEY]
                ?: fallbackPrefs.getString("active_section", "Today")
                ?: "Today"

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(bgDrawable))
                    .cornerRadius(22.dp)
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                // 1. Top row: App Name + Date (left) | Neumorphic Daily Score % badge (right)
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier.defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Day",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ColorProvider(accentColor)
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = "• $displayDate",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = ColorProvider(textMuted)
                            )
                        )
                    }

                    // Neumorphic Inset Score Badge (% only, fire emoji & count removed)
                    Row(
                        modifier = GlanceModifier
                            .background(ImageProvider(insetDrawable))
                            .cornerRadius(12.dp)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$dailyScore%",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(accentColor)
                            )
                        )
                    }
                }

                // 1.5 Quote Banner (in Neumorphic Inset container)
                if (quote.isNotBlank()) {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ImageProvider(insetDrawable))
                            .cornerRadius(10.dp)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "“$quote”",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = ColorProvider(textMuted)
                            ),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(10.dp))

                // 2. Section toggle pill button row: "Today" | "Todo" | "Notes"
                Row(
                    modifier = GlanceModifier.fillMaxWidth().height(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Today", "Todo", "Notes").forEachIndexed { index, section ->
                        if (index > 0) {
                            Spacer(modifier = GlanceModifier.width(8.dp))
                        }
                        val isActive = activeSection == section
                        val pillBgModifier = if (isActive) {
                            GlanceModifier.background(ColorProvider(accentColor))
                        } else {
                            GlanceModifier.background(ImageProvider(inactiveTabDrawable))
                        }
                        val pillTextColor = if (isActive) Color.White else textMuted

                        val clickModifier = if (isActive) {
                            GlanceModifier
                        } else {
                            GlanceModifier.clickable(
                                actionRunCallback<SwitchSectionAction>(
                                    actionParametersOf(SwitchSectionAction.SectionKey to section)
                                )
                            )
                        }

                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight()
                                .then(pillBgModifier)
                                .cornerRadius(10.dp)
                                .then(clickModifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = section,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    color = ColorProvider(pillTextColor)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(10.dp))

                // 3. Content Area based on activeSection
                Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    when (activeSection) {
                        "Today" -> {
                            if (tasks.isEmpty() && todayTodos.isEmpty()) {
                                Box(
                                    modifier = GlanceModifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "All clear for today ✓",
                                        style = TextStyle(fontSize = 13.sp, color = ColorProvider(textMuted))
                                    )
                                }
                            } else {
                                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                                    // 1. Scheduled Tasks for Today
                                    items(tasks.take(20)) { task ->
                                        val timeDisplay = "${formatTo12Hour(task.startTime)} – ${formatTo12Hour(task.endTime)}"
                                        val isDone = task.isCompleted

                                        val checkBgModifier = if (isDone) {
                                            GlanceModifier.background(ColorProvider(accentColor))
                                        } else {
                                            GlanceModifier.background(ImageProvider(insetDrawable))
                                        }

                                        Row(
                                            modifier = GlanceModifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .background(ImageProvider(cardDrawable))
                                                .cornerRadius(12.dp)
                                                .clickable(
                                                    actionRunCallback<ToggleTaskAction>(
                                                        actionParametersOf(ToggleTaskAction.TaskIdKey to task.id)
                                                    )
                                                )
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Neumorphic Checkbox indicator (single clickable Row prevents double-firing)
                                            Box(
                                                modifier = GlanceModifier
                                                    .size(20.dp)
                                                    .cornerRadius(6.dp)
                                                    .then(checkBgModifier),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isDone) {
                                                    Text(
                                                        text = "✓",
                                                        style = TextStyle(
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ColorProvider(Color.White)
                                                        )
                                                    )
                                                }
                                            }
                                            Spacer(modifier = GlanceModifier.width(10.dp))
                                            Column(modifier = GlanceModifier.defaultWeight()) {
                                                Text(
                                                    text = task.name,
                                                    style = TextStyle(
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = ColorProvider(if (isDone) textMuted else textPrimary),
                                                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                                    ),
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = timeDisplay,
                                                    style = TextStyle(
                                                        fontSize = 11.sp,
                                                        color = ColorProvider(textMuted)
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // 2. Due Todos for Today (ensures todos scheduled for today are always visible in Today tab!)
                                    items(todayTodos.take(15)) { todo ->
                                        val todoCheckBgModifier = if (todo.isCompleted) {
                                            GlanceModifier.background(ColorProvider(accentColor))
                                        } else {
                                            GlanceModifier.background(ImageProvider(insetDrawable))
                                        }

                                        val priorityDotColor = when (todo.priority) {
                                            1 -> Color(0xFFE53935)
                                            2 -> Color(0xFFFB8C00)
                                            else -> Color(0xFF43A047)
                                        }

                                        Row(
                                            modifier = GlanceModifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .background(ImageProvider(cardDrawable))
                                                .cornerRadius(12.dp)
                                                .clickable(
                                                    actionRunCallback<ToggleTodoAction>(
                                                        actionParametersOf(ToggleTodoAction.TodoIdKey to todo.id)
                                                    )
                                                )
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = GlanceModifier
                                                    .size(20.dp)
                                                    .cornerRadius(6.dp)
                                                    .then(todoCheckBgModifier),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (todo.isCompleted) {
                                                    Text(
                                                        text = "✓",
                                                        style = TextStyle(
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ColorProvider(Color.White)
                                                        )
                                                    )
                                                }
                                            }
                                            Spacer(modifier = GlanceModifier.width(10.dp))
                                            Column(modifier = GlanceModifier.defaultWeight()) {
                                                Text(
                                                    text = todo.title,
                                                    style = TextStyle(
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = ColorProvider(if (todo.isCompleted) textMuted else textPrimary),
                                                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                    ),
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = if (!todo.dueTime.isNullOrBlank()) "Today at ${todo.dueTime}" else "Due Today",
                                                    style = TextStyle(
                                                        fontSize = 11.sp,
                                                        color = ColorProvider(textMuted)
                                                    )
                                                )
                                            }
                                            Spacer(modifier = GlanceModifier.width(4.dp))
                                            Box(
                                                modifier = GlanceModifier
                                                    .size(8.dp)
                                                    .cornerRadius(4.dp)
                                                    .background(ColorProvider(priorityDotColor))
                                            ) {}
                                        }
                                    }
                                }
                            }
                        }

                        "Todo" -> {
                            if (widgetTodos.isEmpty()) {
                                Box(
                                    modifier = GlanceModifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No todos due today ✓",
                                        style = TextStyle(fontSize = 13.sp, color = ColorProvider(textMuted))
                                    )
                                }
                            } else {
                                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                                    items(widgetTodos.take(25)) { todo ->
                                        val todoCheckBgModifier = if (todo.isCompleted) {
                                            GlanceModifier.background(ColorProvider(accentColor))
                                        } else {
                                            GlanceModifier.background(ImageProvider(insetDrawable))
                                        }

                                        val priorityDotColor = when (todo.priority) {
                                            1 -> Color(0xFFE53935)
                                            2 -> Color(0xFFFB8C00)
                                            else -> Color(0xFF43A047)
                                        }

                                        Row(
                                            modifier = GlanceModifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .background(ImageProvider(cardDrawable))
                                                .cornerRadius(12.dp)
                                                .clickable(
                                                    actionRunCallback<ToggleTodoAction>(
                                                        actionParametersOf(ToggleTodoAction.TodoIdKey to todo.id)
                                                    )
                                                )
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Neumorphic Checkbox indicator (single clickable Row prevents double-firing)
                                            Box(
                                                modifier = GlanceModifier
                                                    .size(20.dp)
                                                    .cornerRadius(6.dp)
                                                    .then(todoCheckBgModifier),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (todo.isCompleted) {
                                                    Text(
                                                        text = "✓",
                                                        style = TextStyle(
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ColorProvider(Color.White)
                                                        )
                                                    )
                                                }
                                            }
                                            Spacer(modifier = GlanceModifier.width(10.dp))
                                            Column(modifier = GlanceModifier.defaultWeight()) {
                                                Text(
                                                    text = todo.title,
                                                    style = TextStyle(
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = ColorProvider(if (todo.isCompleted) textMuted else textPrimary),
                                                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                    ),
                                                    maxLines = 1
                                                )
                                                if (!todo.dueDate.isNullOrBlank() || !todo.dueTime.isNullOrBlank()) {
                                                    val dueLabel = when {
                                                        todo.dueDate == todayStr -> if (!todo.dueTime.isNullOrBlank()) "Today at ${todo.dueTime}" else "Today"
                                                        todo.dueDate != null -> todo.dueDate
                                                        else -> "Anytime"
                                                    }
                                                    Text(
                                                        text = dueLabel,
                                                        style = TextStyle(
                                                            fontSize = 11.sp,
                                                            color = ColorProvider(textMuted)
                                                        )
                                                    )
                                                }
                                            }
                                            Spacer(modifier = GlanceModifier.width(4.dp))
                                            Box(
                                                modifier = GlanceModifier
                                                    .size(8.dp)
                                                    .cornerRadius(4.dp)
                                                    .background(ColorProvider(priorityDotColor))
                                            ) {}
                                        }
                                    }
                                }
                            }
                        }

                        "Notes" -> {
                            if (notes.isEmpty()) {
                                Box(
                                    modifier = GlanceModifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No notes yet",
                                        style = TextStyle(fontSize = 13.sp, color = ColorProvider(textMuted))
                                    )
                                }
                            } else {
                                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                                    items(notes.take(25)) { note ->
                                        Row(
                                            modifier = GlanceModifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .background(ImageProvider(cardDrawable))
                                                .cornerRadius(12.dp)
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                                .clickable(actionStartActivity<MainActivity>()),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = note.content,
                                                style = TextStyle(fontSize = 12.sp, color = ColorProvider(textPrimary)),
                                                maxLines = 2,
                                                modifier = GlanceModifier.defaultWeight()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // 4. Bottom row: "+ Add" button + "Open Day" button
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(accentColor.copy(alpha = 0.15f)))
                            .cornerRadius(10.dp)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable(actionStartActivity<MainActivity>())
                    ) {
                        Text(
                            text = "+ Add",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(accentColor)
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Box(
                        modifier = GlanceModifier
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .clickable(actionStartActivity<MainActivity>())
                    ) {
                        Text(
                            text = "Open Day →",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = ColorProvider(textMuted)
                            )
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun formatTo12Hour(timeStr: String): String {
            return try {
                val parts = timeStr.trim().split(":")
                val h = parts[0].toInt()
                val m = parts[1].toInt()
                val amPm = if (h >= 12) "PM" else "AM"
                val h12 = when {
                    h == 0 -> 12
                    h > 12 -> h - 12
                    else -> h
                }
                String.format(Locale.getDefault(), "%d:%02d %s", h12, m, amPm)
            } catch (e: Exception) {
                timeStr
            }
        }
    }
}

class SwitchSectionAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val targetSection = parameters[SectionKey] ?: "Today"

        // 1. Update DataStore state for Glance so it recomposes immediately
        try {
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[ACTIVE_SECTION_KEY] = targetSection
                }
            }
        } catch (_: Exception) {}

        // 2. Synchronously write SharedPreferences as backup
        context.getSharedPreferences("day_widget_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("active_section", targetSection)
            .commit()

        // 3. Immediately trigger Glance update
        DayWidget().update(context, glanceId)

        // 4. Send broadcast to system launcher to ensure immediate redraw
        try {
            val intent = Intent(context, DayWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, DayWidgetReceiver::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
    }

    companion object {
        val SectionKey = ActionParameters.Key<String>("section_key")
    }
}

class ToggleTaskAction : ActionCallback {
    companion object {
        val TaskIdKey = ActionParameters.Key<Int>("task_id")
        private val lastToggleTimes = ConcurrentHashMap<Int, Long>()
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskIdKey] ?: return
        val now = System.currentTimeMillis()
        val lastTime = lastToggleTimes[taskId] ?: 0L
        if (now - lastTime < 500L) {
            return // Debounce rapid double-click
        }
        lastToggleTimes[taskId] = now

        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val taskDao = db.taskDao()
            val task = taskDao.getTaskById(taskId) ?: return@withContext
            val nextCompleted = !task.isCompleted
            val updatedTask = task.copy(isCompleted = nextCompleted)
            taskDao.updateTask(updatedTask)

            // Bidirectional Habit logging
            if (task.habitId != null) {
                val habitDao = db.habitDao()
                if (nextCompleted) {
                    habitDao.insertHabitLog(HabitLog(habitId = task.habitId, date = task.date))
                } else {
                    habitDao.deleteHabitLog(task.habitId, task.date)
                }
            }

            // Bidirectional Todo updating
            if (task.todoId != null) {
                val todoDao = db.todoDao()
                val todo = todoDao.getTodoById(task.todoId)
                if (todo != null) {
                    if (nextCompleted) {
                        if (RecurrenceHelper.isRecurring(todo)) {
                            val nextDate = RecurrenceHelper.getNextDueDate(todo)
                            todoDao.updateTodo(todo.copy(dueDate = nextDate, isCompleted = false))
                        } else {
                            todoDao.updateTodo(todo.copy(isCompleted = true))
                        }
                    } else {
                        todoDao.updateTodo(todo.copy(isCompleted = false))
                    }
                }
            }
        }

        DayWidget().update(context, glanceId)

        try {
            val intent = Intent(context, DayWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, DayWidgetReceiver::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
    }
}

class ToggleTodoAction : ActionCallback {
    companion object {
        val TodoIdKey = ActionParameters.Key<Int>("todo_id")
        private val lastToggleTimes = ConcurrentHashMap<Int, Long>()
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val todoId = parameters[TodoIdKey] ?: return
        val now = System.currentTimeMillis()
        val lastTime = lastToggleTimes[todoId] ?: 0L
        if (now - lastTime < 500L) {
            return // Debounce rapid double-click
        }
        lastToggleTimes[todoId] = now

        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val todoDao = db.todoDao()
            val todo = todoDao.getTodoById(todoId) ?: return@withContext

            if (todo.isCompleted) {
                todoDao.updateTodo(todo.copy(isCompleted = false))
            } else {
                if (RecurrenceHelper.isRecurring(todo)) {
                    val nextDate = RecurrenceHelper.getNextDueDate(todo)
                    todoDao.updateTodo(todo.copy(dueDate = nextDate, isCompleted = false))
                } else {
                    todoDao.updateTodo(todo.copy(isCompleted = true))
                }
            }
        }

        DayWidget().update(context, glanceId)

        try {
            val intent = Intent(context, DayWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, DayWidgetReceiver::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
    }
}
