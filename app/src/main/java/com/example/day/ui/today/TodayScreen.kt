package com.example.day.ui.today

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day.R
import com.example.day.data.Task
import com.example.day.data.Todo
import com.example.day.ui.DayViewModel
import com.example.day.ui.common.NeuButton
import com.example.day.ui.common.NeuCard
import com.example.day.ui.common.NeuCheckbox
import com.example.day.ui.common.NeuIconButton
import com.example.day.ui.common.NeuTextField
import com.example.day.ui.common.NeuTimePickerDialog
import com.example.day.ui.common.SmartInputBar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TodayScreen(
    viewModel: DayViewModel,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val todayTodos by viewModel.todayTodos.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dailyStreak by viewModel.dailyStreak.collectAsState()
    val currentQuote by viewModel.currentQuote.collectAsState()
    val isQuoteEnabled by viewModel.isDailyQuoteEnabled.collectAsState()
    val schedulePrefs by viewModel.schedulePreferences.collectAsState()
    val isGeneratingSchedule by viewModel.isGeneratingSchedule.collectAsState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        viewModel.scheduleSnackbarMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    // Deduplicate todos that are already scheduled as tasks for today (Fulfills Note 7)
    val scheduledTodoIds = remember(tasks) { tasks.mapNotNull { it.todoId }.toSet() }
    val scheduledTaskNames = remember(tasks) { tasks.map { it.name.trim().lowercase(Locale.ROOT) }.toSet() }
    val unscheduledTodayTodos = remember(todayTodos, scheduledTodoIds, scheduledTaskNames) {
        todayTodos.filter { todo ->
            !scheduledTodoIds.contains(todo.id) &&
            !scheduledTaskNames.contains(todo.title.trim().lowercase(Locale.ROOT))
        }
    }

    val completedTasksCount = tasks.count { it.isCompleted } + unscheduledTodayTodos.count { it.isCompleted }
    val totalCount = tasks.size + unscheduledTodayTodos.size
    val dailyScore = if (totalCount > 0) (completedTasksCount * 100) / totalCount else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Header row: Title "Today" (left) + Action buttons: Share, Add Task, Themes/Settings (right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Share Schedule Button (Fulfills Note 4)
                NeuIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        shareSchedule(context, selectedDate, tasks, unscheduledTodayTodos)
                    },
                    size = 40.dp,
                    tint = primaryColor
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Schedule",
                        modifier = Modifier.size(18.dp)
                    )
                }

                NeuIconButton(
                    onClick = { showAddTaskDialog = true },
                    size = 40.dp,
                    tint = primaryColor
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task",
                        modifier = Modifier.size(20.dp)
                    )
                }

                NeuIconButton(
                    onClick = onOpenSettings,
                    size = 40.dp,
                    tint = primaryColor
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Themes & Settings",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Date Navigation Strip: Spacious and centered
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NeuIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.changeDate(getOffsetDate(selectedDate, -1))
                },
                size = 36.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Day",
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formatHeaderDate(selectedDate),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (schedulePrefs.autoScheduleEnabled) {
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isGeneratingSchedule) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = primaryColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "✨ Regenerate",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = primaryColor,
                            modifier = Modifier.clickable {
                                viewModel.regenerateTodaySchedule()
                            }
                        )
                    }
                }
            }

            NeuIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.changeDate(getOffsetDate(selectedDate, 1))
                },
                size = 36.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Day",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // 2. Streak + Score row: two NeuCards side by side
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Left card: streak
                    NeuCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "Streak",
                                    tint = primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$dailyStreak",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "day streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Right card: Daily score %
                    NeuCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrackChanges,
                                    contentDescription = "Score",
                                    tint = primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$dailyScore%",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "today's score",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Quote: italic text, muted color, no card wrapper (supports rotating or custom quote)
            if (currentQuote.isNotBlank()) {
                item {
                    Text(
                        text = "\"$currentQuote\"",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // 4. Empty State
            if (tasks.isEmpty() && unscheduledTodayTodos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nothing scheduled yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Scheduled Tasks (Fulfills Note 3: editable like tasks in todo section)
                items(tasks, key = { "task_${it.id}" }) { task ->
                    TodayTaskCard(
                        task = task,
                        accentColor = primaryColor,
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleTaskCompletion(task)
                        },
                        onEdit = {
                            taskToEdit = task
                        },
                        onDelete = {
                            viewModel.deleteTask(task)
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Due Todos (Deduplicated: excludes any todo already scheduled as a task for today)
                if (unscheduledTodayTodos.isNotEmpty()) {
                    item {
                        Text(
                            text = "DUE TODAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp, start = 4.dp)
                        )
                    }
                    items(unscheduledTodayTodos, key = { "todo_${it.id}" }) { todo ->
                        TodayTodoCard(
                            todo = todo,
                            onToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleTodoCompletion(todo)
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // 5. SmartInputBar: stays at bottom with NeuTextField style
        SmartInputBar(
            viewModel = viewModel,
            hintText = "Try 'Read book for 30m'",
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Add Task Dialog with Sliding Clock Wheel (NeuTimePickerDialog)
    if (showAddTaskDialog) {
        CreateTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { name, start, end ->
                viewModel.addTask(
                    name = name,
                    startTime = start,
                    endTime = end,
                    colorHex = "#FF8800"
                )
                showAddTaskDialog = false
            }
        )
    }

    // Edit Task Dialog (Fulfills Note 3: make today's tasks editable)
    if (taskToEdit != null) {
        EditTaskDialog(
            task = taskToEdit!!,
            onDismiss = { taskToEdit = null },
            onConfirm = { updatedName, updatedStart, updatedEnd ->
                viewModel.updateTask(
                    taskToEdit!!.copy(
                        name = updatedName,
                        startTime = updatedStart,
                        endTime = updatedEnd
                    )
                )
                taskToEdit = null
            }
        )
    }
}

@Composable
private fun TodayTaskCard(
    task: Task,
    accentColor: Color,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = task.isCompleted
    val isHabit = task.habitId != null
    var showActions by remember { mutableStateOf(false) }

    NeuCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isDone) 0.5f else 1.0f),
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(14.dp),
        onClick = { showActions = !showActions }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Accent Bar (3.dp wide, dynamic accent color, rounded — ONLY if task is habit-linked)
                if (isHabit) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                NeuCheckbox(
                    checked = isDone,
                    onCheckedChange = { onToggle() }
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    // Scheduled todos appear with allotted time, not an end time (Fulfills Note 6)
                    val isScheduledTodo = task.todoId != null || task.category.equals("Todo", ignoreCase = true)
                    val timeDisplay = if (isScheduledTodo) {
                        "At ${formatTo12Hour(task.startTime)}"
                    } else {
                        "${formatTo12Hour(task.startTime)} – ${formatTo12Hour(task.endTime)}"
                    }

                    Text(
                        text = timeDisplay,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                NeuIconButton(
                    onClick = onEdit,
                    size = 34.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                NeuIconButton(
                    onClick = onDelete,
                    size = 34.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expanded action buttons on tap (Fulfills Note 3: matching todo section)
            AnimatedVisibility(visible = showActions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeuButton(
                        onClick = onEdit,
                        cornerRadius = 8.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    NeuButton(
                        onClick = onDelete,
                        cornerRadius = 8.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = com.example.day.theme.OverdueRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp, color = com.example.day.theme.OverdueRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayTodoCard(
    todo: Todo,
    onToggle: () -> Unit
) {
    val isDone = todo.isCompleted

    NeuCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isDone) 0.5f else 1.0f),
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox-only style: NO left accent bar
            NeuCheckbox(
                checked = isDone,
                onCheckedChange = { onToggle() }
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (!todo.dueTime.isNullOrBlank()) "Due at ${formatTo12Hour(todo.dueTime)}" else "Due today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Priority indicator dot
            val priorityColor = when (todo.priority) {
                1 -> Color(0xFFE53935)
                2 -> Color(0xFFFB8C00)
                3 -> Color(0xFF43A047)
                else -> Color(0xFF1E88E5)
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(priorityColor)
            )
        }
    }
}

@Composable
private fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, start: String, end: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Scheduled Task",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                NeuTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Task title (e.g. Deep Work, Gym)..."
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Time Block:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Start Time Button (triggers clock dial picker)
                    NeuCard(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 12.dp,
                        contentPadding = PaddingValues(10.dp),
                        onClick = { showStartTimePicker = true }
                    ) {
                        Column {
                            Text("Start", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatTo12Hour(startTime), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // End Time Button (triggers clock dial picker)
                    NeuCard(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 12.dp,
                        contentPadding = PaddingValues(10.dp),
                        onClick = { showEndTimePicker = true }
                    ) {
                        Column {
                            Text("End", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatTo12Hour(endTime), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), startTime, endTime)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Add Task", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showStartTimePicker) {
        NeuTimePickerDialog(
            initialTime = startTime,
            title = "Select Start Time",
            onTimeSelected = { startTime = it },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        NeuTimePickerDialog(
            initialTime = endTime,
            title = "Select End Time",
            onTimeSelected = { endTime = it },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

@Composable
private fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onConfirm: (name: String, start: String, end: String) -> Unit
) {
    var name by remember { mutableStateOf(task.name) }
    var startTime by remember { mutableStateOf(task.startTime) }
    var endTime by remember { mutableStateOf(task.endTime) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Scheduled Task",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                NeuTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Task title..."
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Time Block:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Start Time Button
                    NeuCard(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 12.dp,
                        contentPadding = PaddingValues(10.dp),
                        onClick = { showStartTimePicker = true }
                    ) {
                        Column {
                            Text("Start", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatTo12Hour(startTime), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // End Time Button
                    NeuCard(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 12.dp,
                        contentPadding = PaddingValues(10.dp),
                        onClick = { showEndTimePicker = true }
                    ) {
                        Column {
                            Text("End", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatTo12Hour(endTime), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), startTime, endTime)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showStartTimePicker) {
        NeuTimePickerDialog(
            initialTime = startTime,
            title = "Select Start Time",
            onTimeSelected = { startTime = it },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        NeuTimePickerDialog(
            initialTime = endTime,
            title = "Select End Time",
            onTimeSelected = { endTime = it },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

private fun shareSchedule(context: Context, dateStr: String, tasks: List<Task>, todos: List<Todo>) {
    val displayDate = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val d = sdf.parse(dateStr)
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(d ?: Date())
    } catch (_: Exception) {
        dateStr
    }

    val sb = StringBuilder()
    sb.append("📅 Schedule for $displayDate\n\n")

    if (tasks.isEmpty() && todos.isEmpty()) {
        sb.append("No tasks scheduled for today.\n")
    } else {
        if (tasks.isNotEmpty()) {
            sb.append("⏰ Scheduled Tasks:\n")
            tasks.sortedBy { it.startTime }.forEach { task ->
                val status = if (task.isCompleted) "✓ " else "• "
                val isScheduledTodo = task.todoId != null || task.category.equals("Todo", ignoreCase = true)
                val timeStr = if (isScheduledTodo) {
                    formatTo12Hour(task.startTime)
                } else {
                    "${formatTo12Hour(task.startTime)} - ${formatTo12Hour(task.endTime)}"
                }
                sb.append("$status$timeStr: ${task.name}\n")
            }
            sb.append("\n")
        }

        if (todos.isNotEmpty()) {
            sb.append("📋 To-Dos Due Today:\n")
            todos.forEach { todo ->
                val status = if (todo.isCompleted) "✓ " else "• "
                val timeStr = if (!todo.dueTime.isNullOrBlank()) " (${formatTo12Hour(todo.dueTime)})" else ""
                sb.append("$status${todo.title}$timeStr\n")
            }
        }
    }

    val scheduleText = sb.toString().trim()

    // 1. Copy to clipboard
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("Day Schedule", scheduleText)
    clipboard?.setPrimaryClip(clip)

    Toast.makeText(context, "Schedule copied to clipboard! Choose where to share:", Toast.LENGTH_SHORT).show()

    // 2. Open Android system share chooser
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, scheduleText)
        putExtra(Intent.EXTRA_SUBJECT, "My Schedule for $displayDate")
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Schedule")
    context.startActivity(shareIntent)
}

private fun formatTo12Hour(timeStr: String): String {
    return try {
        val parts = timeStr.trim().split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        val amPm = if (hour >= 12) "PM" else "AM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val minStr = if (minute < 10) "0$minute" else "$minute"
        "$hour12:$minStr $amPm"
    } catch (e: Exception) {
        timeStr
    }
}

private fun getOffsetDate(dateStr: String, offsetDays: Int): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    cal.time = sdf.parse(dateStr) ?: Calendar.getInstance().time
    cal.add(Calendar.DAY_OF_YEAR, offsetDays)
    return sdf.format(cal.time)
}

private fun formatHeaderDate(dateStr: String): String {
    val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfOut = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val date = sdfIn.parse(dateStr) ?: return dateStr
    return sdfOut.format(date)
}
