package com.example.day.ui.habit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day.data.Habit
import com.example.day.data.HabitLog
import com.example.day.ui.DayViewModel
import com.example.day.ui.common.NeuButton
import com.example.day.ui.common.NeuCard
import com.example.day.ui.common.NeuIconButton
import com.example.day.ui.common.NeuTextField
import com.example.day.ui.common.NeuTimePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HabitsScreen(
    viewModel: DayViewModel,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val logs by viewModel.habitLogs.collectAsState()

    var showAddHabitDialog by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    var habitToSchedule by remember { mutableStateOf<Habit?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Header: Screen title + count + NeuIconButton (+)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Habits",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${habits.size} tracked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeuIconButton(
                    onClick = { showAddHabitDialog = true },
                    size = 40.dp,
                    tint = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Habit",
                        modifier = Modifier.size(20.dp)
                    )
                }

                NeuIconButton(
                    onClick = onOpenSettings,
                    size = 40.dp,
                    tint = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Themes & Settings",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Dynamic Analytics Calculations
        val todayCal = remember(logs) { Calendar.getInstance() }
        val currentMonthPrefix = remember(todayCal) { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(todayCal.time) }
        val dayOfMonth = remember(todayCal) { todayCal.get(Calendar.DAY_OF_MONTH) }
        val monthLogsCount = remember(logs, currentMonthPrefix) {
            logs.filter { it.date.startsWith(currentMonthPrefix) }.map { "${it.habitId}_${it.date}" }.distinct().size
        }
        val totalPossible = (habits.size * dayOfMonth).coerceAtLeast(1)
        val monthProgress = if (habits.isNotEmpty()) (monthLogsCount.toFloat() / totalPossible).coerceIn(0f, 1f) else 0f
        val monthPct = (monthProgress * 100).toInt()

        val trendData = remember(logs) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -15)
            val midDateStr = sdf.format(c.time)
            c.add(Calendar.DAY_OF_YEAR, -15)
            val start30Str = sdf.format(c.time)
            val recentLogs = logs.count { it.date >= midDateStr }
            val priorLogs = logs.count { it.date in start30Str..<midDateStr }
            when {
                recentLogs > priorLogs -> "+${recentLogs - priorLogs} Up"
                recentLogs < priorLogs -> "${recentLogs - priorLogs} Down"
                else -> "Steady"
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // 2. Overview Row: Two NeuCards (This Month & 30-Day Trend)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // "This Month" Card with Canvas Circular Progress
                    NeuCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "This Month",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(56.dp)
                            ) {
                                val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                val primaryColor = MaterialTheme.colorScheme.primary
                                Canvas(modifier = Modifier.size(56.dp)) {
                                    val strokeWidth = 5.dp.toPx()
                                    // Outer ring
                                    drawCircle(
                                        color = trackColor,
                                        radius = (size.minDimension - strokeWidth) / 2,
                                        style = Stroke(width = strokeWidth)
                                    )
                                    // Dynamic filled progress arc
                                    if (monthProgress > 0f) {
                                        drawArc(
                                            color = primaryColor,
                                            startAngle = -90f,
                                            sweepAngle = monthProgress * 360f,
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                        )
                                    }
                                }
                                Text(
                                    text = "$monthPct%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    // "30-Day Trend" Card with Canvas Mini Line Chart
                    NeuCard(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(130.dp),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "30-Day Trend",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = trendData,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "Overall consistency",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Mini line chart drawn with Canvas
                            val lineColor = MaterialTheme.colorScheme.primary
                            val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            ) {
                                // Baseline
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, size.height * 0.8f),
                                    end = Offset(size.width, size.height * 0.8f),
                                    strokeWidth = 1.dp.toPx()
                                )

                                // Dynamic trend path
                                val path = Path().apply {
                                    val yBase = size.height * 0.75f
                                    val lift = if (monthProgress > 0.5f) size.height * 0.35f else size.height * 0.15f
                                    moveTo(0f, yBase)
                                    quadraticTo(
                                        size.width * 0.4f, yBase - (lift * 0.5f),
                                        size.width * 0.7f, yBase - (lift * 0.8f)
                                    )
                                    lineTo(size.width, yBase - lift)
                                }
                                drawPath(
                                    path = path,
                                    color = lineColor,
                                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Habits Section Header
            item {
                Text(
                    text = "YOUR HABITS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp, start = 4.dp)
                )
            }

            if (habits.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No habits tracked yet. Tap '+' to create one!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(habits, key = { "habit_${it.id}" }) { habit ->
                    HabitItemNeuCard(
                        habit = habit,
                        habitLogs = logs.filter { it.habitId == habit.id },
                        onScheduleToday = { habitToSchedule = habit },
                        onEdit = { habitToEdit = habit },
                        onDelete = { viewModel.deleteHabit(habit) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        // Note: SmartInputBar REMOVED from Habits screen as requested in brief
    }

    // Add Habit Dialog
    if (showAddHabitDialog) {
        HabitEditDialog(
            habit = null,
            onDismiss = { showAddHabitDialog = false },
            onSave = { name, category ->
                viewModel.addHabit(name, category, "#FF8800")
                showAddHabitDialog = false
            }
        )
    }

    // Edit Habit Dialog
    if (habitToEdit != null) {
        HabitEditDialog(
            habit = habitToEdit,
            onDismiss = { habitToEdit = null },
            onSave = { name, category ->
                viewModel.updateHabit(habitToEdit!!.copy(name = name, category = category))
                habitToEdit = null
            }
        )
    }

    // Schedule Habit to Today Dialog
    if (habitToSchedule != null) {
        ScheduleHabitDialog(
            habit = habitToSchedule!!,
            onDismiss = { habitToSchedule = null },
            onSchedule = { startTime, endTime ->
                viewModel.addHabitToToday(habitToSchedule!!, startTime, endTime)
                habitToSchedule = null
            }
        )
    }
}

private fun calculateHabitStreak(habitLogs: List<HabitLog>): Int {
    if (habitLogs.isEmpty()) return 0
    val dates = habitLogs.map { it.date }.distinct().toSet()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    val todayStr = sdf.format(cal.time)
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = sdf.format(cal.time)

    val startCal = when {
        dates.contains(todayStr) -> Calendar.getInstance()
        dates.contains(yesterdayStr) -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        else -> return 0
    }

    var streak = 0
    while (dates.contains(sdf.format(startCal.time))) {
        streak++
        startCal.add(Calendar.DAY_OF_YEAR, -1)
    }
    return streak
}

private fun calculateHabit30DayPercent(habitLogs: List<HabitLog>): Int {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -30)
    val cutoffStr = sdf.format(cal.time)
    val distinctDays = habitLogs.filter { it.date >= cutoffStr }.map { it.date }.distinct().size
    return ((distinctDays * 100) / 30).coerceIn(0, 100)
}

@Composable
private fun HabitItemNeuCard(
    habit: Habit,
    habitLogs: List<HabitLog>,
    onScheduleToday: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val streak = remember(habitLogs) { calculateHabitStreak(habitLogs) }
    val thirtyDayPct = remember(habitLogs) { calculateHabit30DayPercent(habitLogs) }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val isDoneToday = remember(habitLogs) { habitLogs.any { it.date == todayStr } }

    NeuCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Subtle Category Pill + Done Today Badge
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(primaryColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = habit.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = primaryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (isDoneToday) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Done today ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF22C55E),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                NeuIconButton(
                    onClick = onEdit,
                    size = 36.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Habit",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                NeuIconButton(
                    onClick = onDelete,
                    size = 36.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Habit",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats row (streak / 30d% / avg hours) + "+ Today" button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Col 1: Streak
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${streak}d",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Col 2: 30d%
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$thirtyDayPct%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "30d",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Col 3: Avg hours
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${habit.targetHoursPerDay}h",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // "+ Today" Button
                val primaryColor = MaterialTheme.colorScheme.primary
                NeuButton(
                    onClick = onScheduleToday,
                    cornerRadius = 10.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = primaryColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ Today",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitEditDialog(
    habit: Habit?,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf(habit?.name ?: "") }
    var category by remember { mutableStateOf(habit?.category ?: "Skill") }
    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (habit == null) "New Habit" else "Edit Habit",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                NeuTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Habit name (e.g. Read, Workout)"
                )
                Spacer(modifier = Modifier.height(12.dp))
                NeuTextField(
                    value = category,
                    onValueChange = { category = it },
                    placeholder = "Category (Skill, Health, Startup...)"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), category.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ScheduleHabitDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onSchedule: (startTime: String, endTime: String) -> Unit
) {
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Schedule '${habit.name}' for Today",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Assign a time block on today's agenda:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "START TIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        NeuButton(
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 12.dp
                        ) {
                            Text(startTime, fontWeight = FontWeight.Bold, color = primaryColor)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "END TIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        NeuButton(
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 12.dp
                        ) {
                            Text(endTime, fontWeight = FontWeight.Bold, color = primaryColor)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSchedule(startTime, endTime) },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Schedule", color = Color.White)
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
