package com.example.day.ui.today

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Palette
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
import com.example.day.ui.common.NeuCard
import com.example.day.ui.common.NeuCheckbox
import com.example.day.ui.common.NeuIconButton
import com.example.day.ui.common.NeuTextField
import com.example.day.ui.common.NeuTimePickerDialog
import com.example.day.ui.common.SmartInputBar
import java.text.SimpleDateFormat
import java.util.Calendar
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

    val completedTasksCount = tasks.count { it.isCompleted } + todayTodos.count { it.isCompleted }
    val totalCount = tasks.size + todayTodos.size
    val dailyScore = if (totalCount > 0) (completedTasksCount * 100) / totalCount else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Header row: Title "Today" (left) + Action buttons: Add Task & Themes/Quotes (right)
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
            if (tasks.isEmpty() && todayTodos.isEmpty()) {
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
                // Scheduled Tasks
                items(tasks, key = { "task_${it.id}" }) { task ->
                    TodayTaskCard(
                        task = task,
                        accentColor = primaryColor,
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleTaskCompletion(task)
                        },
                        onDelete = {
                            viewModel.deleteTask(task)
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Due Todos (Bug 2: Todo -> Today reflection)
                if (todayTodos.isNotEmpty()) {
                    item {
                        Text(
                            text = "DUE TODAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp, start = 4.dp)
                        )
                    }
                    items(todayTodos, key = { "todo_${it.id}" }) { todo ->
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
}

@Composable
private fun TodayTaskCard(
    task: Task,
    accentColor: Color,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = task.isCompleted
    val isHabit = task.habitId != null

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
                Text(
                    text = "${formatTo12Hour(task.startTime)} – ${formatTo12Hour(task.endTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
