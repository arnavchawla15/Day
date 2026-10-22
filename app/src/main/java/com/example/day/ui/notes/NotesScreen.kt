package com.example.day.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day.data.Note
import com.example.day.ui.DayViewModel
import com.example.day.ui.common.NeuCard
import com.example.day.ui.common.NeuIconButton
import com.example.day.ui.common.NeuTextField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    viewModel: DayViewModel,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState()
    var newNoteText by remember { mutableStateOf("") }
    var editingNoteId by remember { mutableStateOf<Int?>(null) }
    var editingContent by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Header: Screen title "Notes" + Settings button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${notes.size} thoughts & memos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (notes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No notes yet. Type below to jot a thought!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(notes, key = { "note_${it.id}" }) { note ->
                    val isEditing = editingNoteId == note.id

                    NeuCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(16.dp),
                        onClick = {
                            if (!isEditing) {
                                editingNoteId = note.id
                                editingContent = note.content
                            }
                        }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (isEditing) {
                                        NeuTextField(
                                            value = editingContent,
                                            onValueChange = { editingContent = it },
                                            placeholder = "Edit note...",
                                            singleLine = false,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Text(
                                            text = note.content,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                if (isEditing) {
                                    NeuIconButton(
                                        onClick = {
                                            if (editingContent.isNotBlank()) {
                                                viewModel.updateNote(note.copy(content = editingContent.trim(), title = editingContent.take(30)))
                                            }
                                            editingNoteId = null
                                        },
                                        size = 32.dp,
                                        tint = MaterialTheme.colorScheme.primary
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = "Save inline edit",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    NeuIconButton(
                                        onClick = { viewModel.deleteNote(note) },
                                        size = 32.dp,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete note",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = formatNoteTimestamp(note.updatedAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // 3. Add note at bottom: NeuTextField (inset style) with inline + button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuTextField(
                value = newNoteText,
                onValueChange = { newNoteText = it },
                placeholder = "Jot down a quick thought...",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            NeuIconButton(
                onClick = {
                    if (newNoteText.isNotBlank()) {
                        viewModel.addNote(
                            title = newNoteText.take(30).trim(),
                            content = newNoteText.trim()
                        )
                        newNoteText = ""
                    }
                },
                enabled = newNoteText.isNotBlank(),
                tint = if (newNoteText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Note",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatNoteTimestamp(timeMillis: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}
