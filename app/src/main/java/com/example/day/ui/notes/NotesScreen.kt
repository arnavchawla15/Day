package com.example.day.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
    var editingFolder by remember { mutableStateOf("General") }

    var selectedFolder by remember { mutableStateOf("All Notes") }
    var showAddFolderDialog by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary

    val allFolders = remember(notes) {
        val list = mutableListOf("All Notes", "General")
        notes.forEach {
            if (it.folder.isNotBlank() && !list.contains(it.folder)) {
                list.add(it.folder)
            }
        }
        list.distinct()
    }

    val filteredNotes = remember(notes, selectedFolder) {
        if (selectedFolder == "All Notes") notes else notes.filter { it.folder.equals(selectedFolder, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Header: Screen title "Notes" + Settings button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
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
                    text = "${filteredNotes.size} thoughts & memos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

        // 1.5 Horizontal Folder Pills Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(allFolders) { folder ->
                val isSelected = selectedFolder.equals(folder, ignoreCase = true)
                val count = if (folder == "All Notes") notes.size else notes.count { it.folder.equals(folder, ignoreCase = true) }
                val label = if (count > 0) "$folder ($count)" else folder

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) primaryColor else MaterialTheme.colorScheme.surface
                        )
                        .clickable { selectedFolder = folder }
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

            // [+] Add Folder Pill
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { showAddFolderDialog = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Folder",
                            tint = primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New Folder",
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
            if (filteredNotes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedFolder == "All Notes") "No notes yet. Type below to jot a thought!" else "No notes in '$selectedFolder'.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredNotes, key = { "note_${it.id}" }) { note ->
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
                                editingFolder = note.folder
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
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            NeuTextField(
                                                value = editingContent,
                                                onValueChange = { editingContent = it },
                                                placeholder = "Edit note...",
                                                singleLine = false,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            NeuTextField(
                                                value = editingFolder,
                                                onValueChange = { editingFolder = it },
                                                placeholder = "Folder name...",
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
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
                                                viewModel.updateNote(
                                                    note.copy(
                                                        content = editingContent.trim(),
                                                        title = editingContent.take(30),
                                                        folder = editingFolder.ifBlank { "General" }.trim()
                                                    )
                                                )
                                            }
                                            editingNoteId = null
                                        },
                                        size = 32.dp,
                                        tint = primaryColor
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Folder tag
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "Folder",
                                        tint = primaryColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = note.folder,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primaryColor
                                    )
                                }

                                Text(
                                    text = formatNoteTimestamp(note.updatedAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                placeholder = if (selectedFolder == "All Notes") "Jot down a quick thought..." else "Add note to '$selectedFolder'...",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            NeuIconButton(
                onClick = {
                    if (newNoteText.isNotBlank()) {
                        viewModel.addNote(
                            title = newNoteText.take(30).trim(),
                            content = newNoteText.trim(),
                            folder = if (selectedFolder == "All Notes") "General" else selectedFolder
                        )
                        newNoteText = ""
                    }
                },
                enabled = newNoteText.isNotBlank(),
                tint = if (newNoteText.isNotBlank()) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Note",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Add Folder Dialog
    if (showAddFolderDialog) {
        var newFolderNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text("New Folder", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        text = "Create a folder to group your notes and thoughts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NeuTextField(
                        value = newFolderNameInput,
                        onValueChange = { newFolderNameInput = it },
                        placeholder = "Folder name (e.g. Work, Ideas, Journal)",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderNameInput.isNotBlank()) {
                            val trimmed = newFolderNameInput.trim()
                            selectedFolder = trimmed
                            showAddFolderDialog = false
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
                TextButton(onClick = { showAddFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatNoteTimestamp(timeMillis: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}
