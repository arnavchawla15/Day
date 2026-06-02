package com.example.day.ui.timetable

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day.data.Task
import com.example.day.ui.DayViewModel
import java.text.SimpleDateFormat
import java.util.*

// Pastel colors for selection
val PASTEL_COLORS = listOf(
    "#FFEBEE" to "#FFCDD2", // Red
    "#FFF3E0" to "#FFE0B2", // Orange
    "#E8F5E9" to "#C8E6C9", // Green
    "#E3F2FD" to "#BBDEFB", // Blue
    "#F3E5F5" to "#E1BEE7", // Purple
    "#E0F2F1" to "#B2DFDB", // Teal
    "#E8EAF6" to "#C5CAE9", // Indigo
    "#FFFDE7" to "#FFF9C4"  // Yellow
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: DayViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val quote by viewModel.currentQuote.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    val scrollState = rememberScrollState()

    // Parse date for title
    val dateParsed = remember(selectedDate) {
        try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdfInput.parse(selectedDate)
            val sdfOutput = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
            date?.let { sdfOutput.format(it) } ?: selectedDate
        } catch (e: Exception) {
            selectedDate
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Quote banner
            if (quote.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = quote,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Date Navigation Header - Elevated Premium Design
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { changeDateOffset(selectedDate, -1, viewModel) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .size(44.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack, 
                        contentDescription = "Previous Day",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = dateParsed,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.clickable {
                        viewModel.changeDate(viewModel.getTodayDateString())
                    }
                )

                IconButton(
                    onClick = { changeDateOffset(selectedDate, 1, viewModel) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .size(44.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowForward, 
                        contentDescription = "Next Day",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Time block calendar grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp)
                ) {
                    // Time axis column (width 56.dp)
                    Column(
                        modifier = Modifier.width(56.dp)
                    ) {
                        for (hour in 0..23) {
                            Box(
                                modifier = Modifier
                                    .height(60.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d:00", hour),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(end = 12.dp, top = 2.dp)
                                )
                            }
                        }
                    }

                    // Schedule grid lines + tasks blocks column
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1440.dp) // 24 hours * 60.dp
                    ) {
                        // Draw grid lines (airy & light layout)
                        for (hour in 0..23) {
                            val yOffset = (hour * 60).dp
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = yOffset),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                                thickness = 1.dp
                            )
                        }

                        // Overlay Task cards with entrance animations
                        tasks.forEach { task ->
                            val startMin = parseTimeToMinutes(task.startTime)
                            val endMin = parseTimeToMinutes(task.endTime)
                            val duration = (endMin - startMin).coerceAtLeast(30)

                            val yOffset = startMin.dp
                            val blockHeight = duration.dp

                            val parsedBorderColor = remember(task.colorHex) {
                                val entry = PASTEL_COLORS.find { it.second == task.colorHex || it.first == task.colorHex }
                                Color(android.graphics.Color.parseColor(entry?.second ?: "#90CAF9"))
                            }
                            
                            // 8% opacity tint background for high contrast card styling
                            val parsedBgColor = remember(parsedBorderColor) {
                                parsedBorderColor.copy(alpha = 0.08f)
                            }

                            // Slide-in + Fade entrance animation on task composition
                            var isVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                isVisible = true
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = isVisible,
                                enter = slideInVertically(animationSpec = tween(500)) { it / 2 } + fadeIn(animationSpec = tween(500)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(blockHeight)
                                    .offset(y = yOffset)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = parsedBgColor
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        // Colored Left Border (4dp)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(4.dp)
                                                .background(parsedBorderColor)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable { taskToEdit = task }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            // Checkbox tick satisfying micro-interaction animation
                                            Checkbox(
                                                checked = task.isCompleted,
                                                onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = parsedBorderColor,
                                                    checkmarkColor = Color.White
                                                )
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Smooth strike-through and text opacity reduction
                                            val textAlpha by animateFloatAsState(
                                                targetValue = if (task.isCompleted) 0.5f else 1.0f,
                                                animationSpec = tween(300),
                                                label = "textAlpha"
                                            )

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .alpha(textAlpha)
                                            ) {
                                                Text(
                                                    text = task.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF2C2C2C),
                                                    maxLines = if (duration <= 45) 1 else 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                )
                                                if (duration > 35) {
                                                    Text(
                                                        text = "${task.startTime} - ${task.endTime}",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF5C5C5C),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }

                                            IconButton(onClick = { taskToEdit = task }) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit task",
                                                    tint = Color.DarkGray.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(16.dp),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }

        // Add Dialog
        if (showAddDialog) {
            TaskDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, start, end, color ->
                    viewModel.addTask(name, start, end, color)
                    showAddDialog = false
                }
            )
        }

        // Edit Dialog
        taskToEdit?.let { task ->
            TaskDialog(
                task = task,
                onDismiss = { taskToEdit = null },
                onConfirm = { name, start, end, color ->
                    viewModel.updateTask(task.copy(name = name, startTime = start, endTime = end, colorHex = color))
                    taskToEdit = null
                },
                onDelete = {
                    viewModel.deleteTask(task)
                    taskToEdit = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(
    task: Task? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, startTime: String, endTime: String, colorHex: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(task?.name ?: "") }
    var startTime by remember { mutableStateOf(task?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(task?.endTime ?: "10:00") }
    var selectedColorHex by remember {
        mutableStateOf(task?.colorHex ?: PASTEL_COLORS[3].second) // Default to Pastel Blue
    }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.9f),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        title = {
            Text(
                text = if (task == null) "Add Task" else "Edit Task",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Task Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
 
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start Time", 
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Button(
                            onClick = {
                                val currentHour = startTime.substringBefore(":").toIntOrNull() ?: 9
                                val currentMin = startTime.substringAfter(":").toIntOrNull() ?: 0
                                TimePickerDialog(context, { _, h, m ->
                                    startTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                }, currentHour, currentMin, true).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(text = startTime, fontWeight = FontWeight.Bold)
                        }
                    }
 
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "End Time", 
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Button(
                            onClick = {
                                val currentHour = endTime.substringBefore(":").toIntOrNull() ?: 10
                                val currentMin = endTime.substringAfter(":").toIntOrNull() ?: 0
                                TimePickerDialog(context, { _, h, m ->
                                    endTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                }, currentHour, currentMin, true).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(text = endTime, fontWeight = FontWeight.Bold)
                        }
                    }
                }
 
                // Color Selection Row - Larger circles with checkmark indicators
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Accent Color",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PASTEL_COLORS.forEach { colorPair ->
                            val isSelected = selectedColorHex == colorPair.second || selectedColorHex == colorPair.first
                            val circleBgColor = Color(android.graphics.Color.parseColor(if (isSelected) colorPair.second else colorPair.first))
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(circleBgColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(android.graphics.Color.parseColor(colorPair.second)),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedColorHex = colorPair.second
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
            ) {
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
 
                Button(
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(name, startTime, endTime, selectedColorHex)
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.padding(bottom = 8.dp),
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

// Helpers
private fun parseTimeToMinutes(timeStr: String): Int {
    return try {
        val parts = timeStr.split(":")
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        h * 60 + m
    } catch (e: Exception) {
        540 // Default to 9:00
    }
}

private fun changeDateOffset(currentDateStr: String, offsetDays: Int, viewModel: DayViewModel) {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(currentDateStr) ?: Date()
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_MONTH, offsetDays)
        viewModel.changeDate(sdf.format(calendar.time))
    } catch (e: Exception) {
        // Fallback
    }
}
