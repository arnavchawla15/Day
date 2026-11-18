package com.example.day.ui.todo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day.data.RecurrenceHelper
import com.example.day.data.SubtaskHelper
import com.example.day.data.Todo
import com.example.day.data.TodoSubtask
import com.example.day.theme.OverdueRed
import com.example.day.theme.neuExtruded
import com.example.day.theme.neuInset
import com.example.day.ui.DayViewModel
import com.example.day.ui.common.NeuButton
import com.example.day.ui.common.NeuCard
import com.example.day.ui.common.NeuCheckbox
import com.example.day.ui.common.NeuIconButton
import com.example.day.ui.common.NeuTextField
import com.example.day.ui.common.NeuTimePickerDialog
import com.example.day.ui.common.SmartInputBar
import com.example.day.ui.common.isAppDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    viewModel: DayViewModel,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeTodos by viewModel.activeTodos.collectAsState()
    val completedTodos by viewModel.completedTodos.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showAddListDialog by remember { mutableStateOf(false) }
    var todoToEdit by remember { mutableStateOf<Todo?>(null) }
    var isCompletedExpanded by remember { mutableStateOf(false) }
    var selectedList by remember { mutableStateOf("All") }

    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val todayStr = viewModel.getTodayDateString()

    // Available Custom Lists
    val allLists = remember(activeTodos, completedTodos) {
        val lists = mutableListOf("All", "General")
        (activeTodos + completedTodos).forEach {
            if (it.listName.isNotBlank() && !lists.contains(it.listName)) {
                lists.add(it.listName)
            }
        }
        lists.distinct()
    }

    // Filter todos by selected list
    val listFilteredActive = remember(activeTodos, selectedList) {
        if (selectedList == "All") activeTodos else activeTodos.filter { it.listName.equals(selectedList, ignoreCase = true) }
    }
    val listFilteredCompleted = remember(completedTodos, selectedList) {
        if (selectedList == "All") completedTodos else completedTodos.filter { it.listName.equals(selectedList, ignoreCase = true) }
    }

    // Grouping active todos
    val overdueTodos = listFilteredActive.filter { it.dueDate != null && it.dueDate < todayStr }
    val todayTodos = listFilteredActive.filter { it.dueDate == todayStr }
    val upcomingTodos = listFilteredActive.filter { it.dueDate != null && it.dueDate > todayStr }
    val noDueDateTodos = listFilteredActive.filter { it.dueDate == null }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Header: Screen title + count of remaining items + NeuIconButton (+)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Todo",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${listFilteredActive.size} remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeuIconButton(
                    onClick = { showAddDialog = true },
                    size = 40.dp,
                    tint = primaryColor
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Todo",
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

        // 1.5 Custom Lists Row (Horizontal Pills)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(allLists) { list ->
                val isSelected = selectedList.equals(list, ignoreCase = true)
                val count = if (list == "All") activeTodos.size else activeTodos.count { it.listName.equals(list, ignoreCase = true) }
                val label = if (count > 0) "$list ($count)" else list

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) primaryColor else MaterialTheme.colorScheme.surface
                        )
                        .clickable { selectedList = list }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // [+] Add List Pill
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { showAddListDialog = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New List",
                            tint = primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New List",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val onSubtaskToggle: (Todo, String) -> Unit = { itemTodo, subtaskId ->
                val steps = SubtaskHelper.parseSubtasks(itemTodo.subtasksJson).map { step ->
                    if (step.id == subtaskId) step.copy(isCompleted = !step.isCompleted) else step
                }
                viewModel.updateTodo(itemTodo.copy(subtasksJson = SubtaskHelper.serializeSubtasks(steps)))
            }

            // 2. Section: Overdue
            if (overdueTodos.isNotEmpty()) {
                item {
                    Text(
                        text = "OVERDUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = OverdueRed,
                        modifier = Modifier.padding(top = 10.dp, bottom = 8.dp, start = 4.dp)
                    )
                }
                items(overdueTodos, key = { "overdue_${it.id}" }) { todo ->
                    TodoItemCard(
                        todo = todo,
                        todayStr = todayStr,
                        isOverdue = true,
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleTodoCompletion(todo)
                        },
                        onEdit = { todoToEdit = todo },
                        onDelete = { viewModel.deleteTodo(todo) },
                        onSubtaskToggle = { onSubtaskToggle(todo, it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 3. Section: Today
            if (todayTodos.isNotEmpty()) {
                item {
                    Text(
                        text = "TODAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 4.dp)
                    )
                }
                items(todayTodos, key = { "today_${it.id}" }) { todo ->
                    TodoItemCard(
                        todo = todo,
                        todayStr = todayStr,
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleTodoCompletion(todo)
                        },
                        onEdit = { todoToEdit = todo },
                        onDelete = { viewModel.deleteTodo(todo) },
                        onSubtaskToggle = { onSubtaskToggle(todo, it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 4. Section: Upcoming
            if (upcomingTodos.isNotEmpty()) {
                item {
                    Text(
                        text = "UPCOMING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 4.dp)
                    )
                }
                items(upcomingTodos, key = { "upcoming_${it.id}" }) { todo ->
                    TodoItemCard(
                        todo = todo,
                        todayStr = todayStr,
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleTodoCompletion(todo)
                        },
                        onEdit = { todoToEdit = todo },
                        onDelete = { viewModel.deleteTodo(todo) },
                        onSubtaskToggle = { onSubtaskToggle(todo, it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 5. Section: No Due Date
            if (noDueDateTodos.isNotEmpty()) {
                item {
                    Text(
                        text = "NO DUE DATE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 4.dp)
                    )
                }
                items(noDueDateTodos, key = { "nodate_${it.id}" }) { todo ->
                    TodoItemCard(
                        todo = todo,
                        todayStr = todayStr,
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleTodoCompletion(todo)
                        },
                        onEdit = { todoToEdit = todo },
                        onDelete = { viewModel.deleteTodo(todo) },
                        onSubtaskToggle = { onSubtaskToggle(todo, it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 6. Section: Completed (collapsible, does not disappear permanently!)
            if (listFilteredCompleted.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isCompletedExpanded = !isCompletedExpanded }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMPLETED (${listFilteredCompleted.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = if (isCompletedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle completed",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isCompletedExpanded) {
                    items(listFilteredCompleted, key = { "completed_${it.id}" }) { todo ->
                        TodoItemCard(
                            todo = todo,
                            todayStr = todayStr,
                            isDimmed = true,
                            onToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleTodoCompletion(todo)
                            },
                            onEdit = { todoToEdit = todo },
                            onDelete = { viewModel.deleteTodo(todo) },
                            onSubtaskToggle = { onSubtaskToggle(todo, it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Bottom Smart Gemini NLP Bar
        SmartInputBar(
            viewModel = viewModel,
            hintText = "Try 'Call accountant tomorrow p1'",
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Add Custom List Dialog
    if (showAddListDialog) {
        var newListNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddListDialog = false },
            title = { Text("New Todo List", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        text = "Create a custom list to organize your tasks.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NeuTextField(
                        value = newListNameInput,
                        onValueChange = { newListNameInput = it },
                        placeholder = "List name (e.g. Work, Shopping, Fitness)",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newListNameInput.isNotBlank()) {
                            val trimmed = newListNameInput.trim()
                            selectedList = trimmed
                            showAddListDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Create", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddListDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Todo Dialog
    if (showAddDialog) {
        TodoEditBottomSheet(
            todo = null,
            initialList = selectedList,
            availableLists = allLists,
            onDismiss = { showAddDialog = false },
            onSave = { title, dueDate, dueTime, priority, recurrence, recurrenceDays, listName, subtasksJson ->
                viewModel.addTodo(
                    title = title,
                    dueDate = dueDate,
                    dueTime = dueTime,
                    priority = priority,
                    recurrenceType = recurrence,
                    recurrenceDays = recurrenceDays,
                    listName = listName,
                    subtasksJson = subtasksJson
                )
                showAddDialog = false
            }
        )
    }

    // Edit Todo Dialog
    if (todoToEdit != null) {
        TodoEditBottomSheet(
            todo = todoToEdit,
            initialList = todoToEdit!!.listName,
            availableLists = allLists,
            onDismiss = { todoToEdit = null },
            onSave = { title, dueDate, dueTime, priority, recurrence, recurrenceDays, listName, subtasksJson ->
                viewModel.updateTodo(
                    todoToEdit!!.copy(
                        title = title,
                        dueDate = dueDate,
                        dueTime = dueTime,
                        priority = priority,
                        recurrenceType = recurrence,
                        recurrenceDays = recurrenceDays,
                        listName = listName,
                        subtasksJson = subtasksJson
                    )
                )
                todoToEdit = null
            }
        )
    }
}

@Composable
private fun TodoItemCard(
    todo: Todo,
    todayStr: String,
    isOverdue: Boolean = false,
    isDimmed: Boolean = false,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSubtaskToggle: (subtaskId: String) -> Unit = {}
) {
    var showActions by remember { mutableStateOf(false) }
    var showSubtasks by remember { mutableStateOf(false) }
    val subtasks = remember(todo.subtasksJson) { SubtaskHelper.parseSubtasks(todo.subtasksJson) }
    val completedSubtasksCount = subtasks.count { it.isCompleted }

    NeuCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isDimmed || todo.isCompleted) 0.5f else 1.0f),
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(14.dp),
        onClick = { showActions = !showActions }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeuCheckbox(
                    checked = todo.isCompleted,
                    onCheckedChange = { onToggle() }
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = todo.title,
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (isOverdue) OverdueRed else MaterialTheme.colorScheme.onBackground
                        )
                        if (RecurrenceHelper.isRecurring(todo)) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Recurring",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))

                    val dueLabel = when {
                        todo.dueDate == todayStr -> if (!todo.dueTime.isNullOrBlank()) "Today at ${todo.dueTime}" else "Today"
                        todo.dueDate != null -> todo.dueDate
                        else -> null
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (dueLabel != null) {
                            Text(
                                text = dueLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isOverdue) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (todo.listName.isNotBlank() && !todo.listName.equals("General", ignoreCase = true)) {
                            Text(
                                text = "• ${todo.listName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (subtasks.isNotEmpty()) {
                            Text(
                                text = "• $completedSubtasksCount/${subtasks.size} steps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { showSubtasks = !showSubtasks }
                            )
                        }
                    }
                }

                // Priority dot (P1=red, P2=orange, P3=green, P4=blue)
                val priorityColor = when (todo.priority) {
                    1 -> Color(0xFFE53935)
                    2 -> Color(0xFFFB8C00)
                    3 -> Color(0xFF43A047)
                    else -> Color(0xFF1E88E5)
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(priorityColor)
                )
            }

            // Steps Checklist (Subtasks)
            if (subtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 38.dp, top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subtasks.forEach { step ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSubtaskToggle(step.id) }
                        ) {
                            NeuCheckbox(
                                checked = step.isCompleted,
                                onCheckedChange = { onSubtaskToggle(step.id) },
                                size = 18.dp,
                                cornerRadius = 5.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = step.title,
                                fontSize = 13.sp,
                                textDecoration = if (step.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (step.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            // Expanded action buttons on tap
            AnimatedVisibility(visible = showActions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
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
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = OverdueRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp, color = OverdueRed)
                    }
                }
            }
        }
    }
}

/**
 * TodoEditBottomSheet: Bottom sheet / dialog with Material 3 DatePicker and TimePicker dial
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoEditBottomSheet(
    todo: Todo?,
    initialList: String = "General",
    availableLists: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        dueDate: String?,
        dueTime: String?,
        priority: Int,
        recurrence: String,
        recurrenceDays: String,
        listName: String,
        subtasksJson: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(todo?.title ?: "") }
    var dueDate by remember { mutableStateOf(todo?.dueDate ?: "") }
    var dueTime by remember { mutableStateOf(todo?.dueTime ?: "") }
    var priority by remember { mutableIntStateOf(todo?.priority ?: 3) }
    var recurrence by remember { mutableStateOf(todo?.recurrenceType ?: "NONE") }
    var recurrenceDays by remember { mutableStateOf(todo?.recurrenceDays ?: "") }
    var isRecurrenceExpanded by remember { mutableStateOf(false) }

    var listName by remember { mutableStateOf(todo?.listName ?: if (initialList != "All") initialList else "General") }
    var subtasks by remember { mutableStateOf(SubtaskHelper.parseSubtasks(todo?.subtasksJson)) }
    var newStepText by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (dueDate.isNotBlank()) {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dueDate)?.time
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        } else System.currentTimeMillis()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (todo == null) "New To-Do" else "Edit To-Do",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                NeuTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Task title..."
                )

                Spacer(modifier = Modifier.height(12.dp))

                // List / Category Selection
                Text("List:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                NeuTextField(
                    value = listName,
                    onValueChange = { listName = it },
                    placeholder = "List name (e.g. General, Work, Personal)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Steps / Subtasks Section
                Text("Steps / Subtasks:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))

                if (subtasks.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subtasks.forEachIndexed { index, step ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${step.title}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                NeuIconButton(
                                    onClick = {
                                        subtasks = subtasks.filterIndexed { i, _ -> i != index }
                                    },
                                    size = 28.dp,
                                    tint = OverdueRed
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove step",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeuTextField(
                        value = newStepText,
                        onValueChange = { newStepText = it },
                        placeholder = "Add a step...",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    NeuIconButton(
                        onClick = {
                            if (newStepText.isNotBlank()) {
                                subtasks = subtasks + TodoSubtask(title = newStepText.trim())
                                newStepText = ""
                            }
                        },
                        size = 36.dp,
                        tint = primaryColor
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Step",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Due Date Picker Button (Bug 4)
                NeuCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                    contentPadding = PaddingValues(12.dp),
                    onClick = { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Select Date",
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (dueDate.isNotBlank()) dueDate else "Set due date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (dueDate.isNotBlank()) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (dueDate.isNotBlank()) {
                            Text(
                                text = "Clear",
                                fontSize = 12.sp,
                                color = OverdueRed,
                                modifier = Modifier.clickable { dueDate = "" }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Due Time Picker Button with Clock Dial Picker
                NeuCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                    contentPadding = PaddingValues(12.dp),
                    onClick = { showTimePicker = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Select Time",
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (dueTime.isNotBlank()) "Due at $dueTime" else "Set due time (clock dial)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (dueTime.isNotBlank()) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (dueTime.isNotBlank()) {
                            Text(
                                text = "Clear",
                                fontSize = 12.sp,
                                color = OverdueRed,
                                modifier = Modifier.clickable { dueTime = "" }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Recurrence Section (Feature 1B)
                Text("Recurrence:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))

                val currentRecurrenceLabel = when (recurrence.uppercase(Locale.ROOT)) {
                    "DAILY" -> "Daily"
                    "EVERY_OTHER_DAY" -> "Every other day"
                    "WEEKLY" -> if (recurrenceDays.isNotBlank()) "Weekly ($recurrenceDays)" else "Weekly"
                    "CUSTOM" -> if (recurrenceDays.isNotBlank()) "Custom ($recurrenceDays)" else "Custom"
                    else -> "None"
                }

                NeuCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                    contentPadding = PaddingValues(12.dp),
                    onClick = { isRecurrenceExpanded = !isRecurrenceExpanded }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Recurrence",
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recurrence: $currentRecurrenceLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Icon(
                            imageVector = if (isRecurrenceExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle recurrence",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (isRecurrenceExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "NONE" to "None",
                                "DAILY" to "Daily",
                                "EVERY_OTHER_DAY" to "Every other day"
                            ).forEach { (type, label) ->
                                NeuRecurrenceChip(
                                    text = label,
                                    isSelected = recurrence.equals(type, ignoreCase = true),
                                    onClick = { recurrence = type }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "WEEKLY" to "Weekly",
                                "CUSTOM" to "Custom"
                            ).forEach { (type, label) ->
                                NeuRecurrenceChip(
                                    text = label,
                                    isSelected = recurrence.equals(type, ignoreCase = true),
                                    onClick = {
                                        recurrence = type
                                        if (type == "WEEKLY" && recurrenceDays.isBlank()) {
                                            recurrenceDays = "MON"
                                        } else if (type == "CUSTOM" && recurrenceDays.isBlank()) {
                                            recurrenceDays = "MON,WED,FRI"
                                        }
                                    }
                                )
                            }
                        }

                        // Day chips when Weekly
                        if (recurrence.equals("WEEKLY", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Select day:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                                val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                days.forEachIndexed { idx, d ->
                                    val isSelected = recurrenceDays.equals(d, ignoreCase = true)
                                    NeuRecurrenceChip(
                                        text = dayLabels[idx],
                                        isSelected = isSelected,
                                        onClick = { recurrenceDays = d }
                                    )
                                }
                            }
                        }

                        // Day chips when Custom
                        if (recurrence.equals("CUSTOM", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Select active days:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                                val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                val selectedSet = recurrenceDays.split(",")
                                    .map { it.trim().uppercase(Locale.ROOT) }
                                    .filter { it.isNotBlank() }
                                    .toMutableSet()

                                days.forEachIndexed { idx, d ->
                                    val isSelected = selectedSet.contains(d)
                                    NeuRecurrenceChip(
                                        text = dayLabels[idx],
                                        isSelected = isSelected,
                                        onClick = {
                                            if (isSelected) {
                                                selectedSet.remove(d)
                                            } else {
                                                selectedSet.add(d)
                                            }
                                            recurrenceDays = days.filter { selectedSet.contains(it) }.joinToString(",")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Priority:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        1 to "🔴 P1",
                        2 to "🟠 P2",
                        3 to "🟢 P3",
                        4 to "🔵 P4"
                    ).forEach { (p, label) ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            title.trim(),
                            dueDate.ifBlank { null },
                            dueTime.ifBlank { null },
                            priority,
                            recurrence,
                            recurrenceDays,
                            listName.ifBlank { "General" },
                            SubtaskHelper.serializeSubtasks(subtasks)
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        dueDate = sdf.format(Date(selectedMillis))
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = primaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        NeuTimePickerDialog(
            initialTime = if (dueTime.isNotBlank()) dueTime else "14:00",
            title = "Set Due Time",
            onTimeSelected = { dueTime = it },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
fun NeuRecurrenceChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppDark()
    val primaryColor = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(10.dp)

    val chipModifier = modifier
        .then(
            if (isSelected) {
                Modifier
                    .neuInset(cornerRadius = 10.dp, isDark = isDark)
                    .background(primaryColor.copy(alpha = 0.16f), shape)
                    .border(1.dp, primaryColor, shape)
            } else {
                Modifier
                    .neuExtruded(cornerRadius = 10.dp, isDark = isDark)
                    .background(MaterialTheme.colorScheme.surface, shape)
            }
        )
        .clip(shape)
        .clickable(onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 7.dp)

    Box(modifier = chipModifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
