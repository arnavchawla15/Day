package com.example.day.ui.template

import android.app.TimePickerDialog
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day.data.TemplateTask
import com.example.day.ui.DayViewModel
import com.example.day.ui.timetable.PASTEL_COLORS
import java.util.*

@Composable
fun TemplateScreen(
    viewModel: DayViewModel,
    modifier: Modifier = Modifier
) {
    val templateTasks by viewModel.templateTasks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TemplateTask?>(null) }
    val scrollState = rememberScrollState()

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
            // Header Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Master Template",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Define your ideal daily habits. These default tasks auto-populate every new day when first opened.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(14.dp)
                )
            }

            // Grid scroll view
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
                    Column(modifier = Modifier.width(56.dp)) {
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

                    // Blocks column
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .height(1440.dp)
                    ) {
                        val parentWidth = maxWidth

                        // Grid lines
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

                        // Draw templates
                        val layoutParamsMap = remember(templateTasks) { computeTemplateLayouts(templateTasks) }

                        templateTasks.forEach { task ->
                            val startMin = parseTimeToMinutes(task.startTime)
                            val endMin = parseTimeToMinutes(task.endTime)
                            val duration = (endMin - startMin).coerceAtLeast(30)

                            val yOffset = startMin.dp
                            val blockHeight = duration.dp

                            val layoutParams = layoutParamsMap[task.id] ?: LayoutParams(0, 1)
                            val colWidth = parentWidth / layoutParams.totalCols
                            val xOffset = colWidth * layoutParams.colIndex

                            val parsedBorderColor = remember(task.colorHex) {
                                val entry = PASTEL_COLORS.find { it.second == task.colorHex || it.first == task.colorHex }
                                Color(android.graphics.Color.parseColor(entry?.second ?: "#90CAF9"))
                            }
                            val parsedBgColor = remember(parsedBorderColor) {
                                parsedBorderColor.copy(alpha = 0.08f)
                            }

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = parsedBgColor),
                                modifier = Modifier
                                    .width(colWidth)
                                    .height(blockHeight)
                                    .offset(x = xOffset, y = yOffset)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxSize()) {
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = task.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF2C2C2C),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(task.category, fontSize = 9.sp) },
                                                    modifier = Modifier.height(18.dp)
                                                )
                                            }
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

        // FAB to add new Template
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
            Icon(Icons.Default.Add, contentDescription = "Add Habit")
        }

        if (showAddDialog) {
            TemplateTaskDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, start, end, color, category ->
                    viewModel.addTemplateTask(name, start, end, color, category)
                    showAddDialog = false
                }
            )
        }

        taskToEdit?.let { task ->
            TemplateTaskDialog(
                task = task,
                onDismiss = { taskToEdit = null },
                onConfirm = { name, start, end, color, category ->
                    viewModel.updateTemplateTask(task.copy(name = name, startTime = start, endTime = end, colorHex = color, category = category))
                    taskToEdit = null
                },
                onDelete = {
                    viewModel.deleteTemplateTask(task)
                    taskToEdit = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateTaskDialog(
    task: TemplateTask? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, startTime: String, endTime: String, colorHex: String, category: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(task?.name ?: "") }
    var category by remember { mutableStateOf(task?.category ?: "Routine") }
    var startTime by remember { mutableStateOf(task?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(task?.endTime ?: "10:00") }
    var selectedColorHex by remember {
        mutableStateOf(task?.colorHex ?: PASTEL_COLORS[3].second)
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
                text = if (task == null) "Add Habit Template" else "Edit Habit Template",
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
                    label = { Text("Habit Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Work, Exercise, Routine)") },
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

                // Accent Color Selection Row
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
                            onConfirm(name, startTime, endTime, selectedColorHex, category)
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

private fun parseTimeToMinutes(timeStr: String): Int {
    return try {
        val parts = timeStr.split(":")
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        h * 60 + m
    } catch (e: Exception) {
        540
    }
}

private data class LayoutParams(
    val colIndex: Int,
    val totalCols: Int
)

private fun computeTemplateLayouts(tasks: List<TemplateTask>): Map<Int, LayoutParams> {
    if (tasks.isEmpty()) return emptyMap()

    val taskMinutes = tasks.associate { task ->
        val start = parseTimeToMinutes(task.startTime)
        val end = parseTimeToMinutes(task.endTime)
        task.id to (start to end)
    }

    fun overlaps(id1: Int, id2: Int): Boolean {
        val range1 = taskMinutes[id1] ?: return false
        val range2 = taskMinutes[id2] ?: return false
        return !(range1.second <= range2.first || range2.second <= range1.first)
    }

    val visited = mutableSetOf<Int>()
    val clusters = mutableListOf<List<Int>>()

    for (task in tasks) {
        if (task.id in visited) continue
        
        val cluster = mutableListOf<Int>()
        val queue = ArrayDeque<Int>()
        queue.add(task.id)
        visited.add(task.id)

        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            cluster.add(curr)
            
            for (other in tasks) {
                if (other.id !in visited && overlaps(curr, other.id)) {
                    visited.add(other.id)
                    queue.add(other.id)
                }
            }
        }
        clusters.add(cluster)
    }

    val layoutMap = mutableMapOf<Int, LayoutParams>()
    for (cluster in clusters) {
        val sortedCluster = cluster.sortedWith(compareBy(
            { taskMinutes[it]?.first ?: 0 },
            { -((taskMinutes[it]?.second ?: 0) - (taskMinutes[it]?.first ?: 0)) }
        ))

        val columns = mutableListOf<MutableList<Int>>()
        
        for (taskId in sortedCluster) {
            var placed = false
            for (i in columns.indices) {
                val col = columns[i]
                val overlapsAny = col.any { colTaskId -> overlaps(taskId, colTaskId) }
                if (!overlapsAny) {
                    col.add(taskId)
                    placed = true
                    break
                }
            }
            
            if (!placed) {
                columns.add(mutableListOf(taskId))
            }
        }

        val totalCols = columns.size
        for (colIdx in columns.indices) {
            for (taskId in columns[colIdx]) {
                layoutMap[taskId] = LayoutParams(colIdx, totalCols)
            }
        }
    }

    return layoutMap
}
