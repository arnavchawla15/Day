package com.example.day.ui.common

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day.data.ParsedResult
import com.example.day.ui.DayViewModel
import kotlinx.coroutines.launch

@Composable
fun SmartInputBar(
    viewModel: DayViewModel,
    hintText: String = "Try 'Gym at 7am to 8am'",
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var pendingItems by remember { mutableStateOf<List<ParsedResult>>(emptyList()) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary

    val handleSubmit = {
        if (text.isNotBlank() && !isSubmitting) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            isSubmitting = true
            val inputText = text.trim()
            text = ""

            coroutineScope.launch {
                try {
                    val parsedList = viewModel.parseSmartInputMulti(inputText)
                    isSubmitting = false
                    if (parsedList.size > 1) {
                        // Multi-item confirmation row (Feature 3C)
                        pendingItems = parsedList
                    } else if (parsedList.isNotEmpty()) {
                        viewModel.addParsedItems(parsedList) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Could not parse input", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    isSubmitting = false
                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Multi-item Confirmation Preview (Feature 3C)
        if (pendingItems.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Adding ${pendingItems.size} items:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "✓ Confirm",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier
                                .clickable {
                                    val itemsToAdd = pendingItems
                                    pendingItems = emptyList()
                                    viewModel.addParsedItems(itemsToAdd) { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(4.dp)
                        )

                        Text(
                            text = "✗ Cancel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clickable {
                                    pendingItems = emptyList()
                                }
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(pendingItems) { item ->
                        val (icon, label) = when (item) {
                            is ParsedResult.TaskResult -> Icons.Default.AccessTime to "Task: ${item.title} (${item.startTime}-${item.endTime})"
                            is ParsedResult.TodoResult -> Icons.Default.CheckCircle to "Todo: ${item.title}${if (item.priority != "P3") " [${item.priority}]" else ""}"
                            is ParsedResult.HabitResult -> Icons.Default.Repeat to "Habit: ${item.name}"
                            is ParsedResult.NoteResult -> Icons.Default.Description to "Note: ${item.title}"
                        }

                        NeuCard(
                            cornerRadius = 8.dp,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = hintText,
                singleLine = false,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { handleSubmit() })
            )

            Spacer(modifier = Modifier.width(10.dp))

            NeuIconButton(
                onClick = { handleSubmit() },
                enabled = text.isNotBlank() && !isSubmitting,
                tint = if (text.isNotBlank()) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = primaryColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
