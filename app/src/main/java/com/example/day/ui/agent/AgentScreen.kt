package com.example.day.ui.agent

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day.R
import com.example.day.data.AgentMessage
import com.example.day.ui.DayViewModel
import com.example.day.ui.common.NeuButton
import com.example.day.ui.common.NeuCard
import com.example.day.ui.common.NeuIconButton
import com.example.day.ui.common.NeuTextField
import com.example.day.ui.common.isAppDark
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AgentScreen(
    dayViewModel: DayViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val agentViewModel = dayViewModel.agentViewModel

    val messages by agentViewModel.messages.collectAsState()
    val isLoading by agentViewModel.isLoading.collectAsState()
    val pendingSummary by agentViewModel.pendingToolSummary.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDark = isAppDark()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_day_logo),
                    contentDescription = "Day Logo",
                    tint = primaryColor,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Day",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "your personal assistant",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeuIconButton(
                    onClick = { showClearConfirm = true },
                    size = 40.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
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
                        contentDescription = "Theme Settings",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ── Chat Area / Empty State ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                AgentEmptyState(
                    primaryColor = primaryColor,
                    onQuickPrompt = { prompt ->
                        if (prompt.endsWith(": ") || prompt.endsWith(":\n")) {
                            inputText = prompt
                        } else {
                            agentViewModel.sendMessage(prompt)
                        }
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp)
                ) {
                    items(messages) { message ->
                        AgentChatBubble(
                            message = message,
                            primaryColor = primaryColor,
                            isDark = isDark,
                            context = context
                        )
                    }

                    if (isLoading) {
                        item {
                            AgentLoadingBubble(
                                pendingSummary = pendingSummary,
                                primaryColor = primaryColor,
                                isDark = isDark
                            )
                        }
                    }
                }
            }
        }

        // ── Attached Image Preview ────────────────────────────────────────────
        if (selectedImageUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bitmap = remember(selectedImageUri) {
                        loadBitmapFromUri(context, selectedImageUri.toString(), 200, 200)
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Selected image preview",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Image attached",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove attached image",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ── Bottom Input Row ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Photo picker button
            NeuIconButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                size = 46.dp,
                tint = if (selectedImageUri != null) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Attach Photo",
                    modifier = Modifier.size(22.dp)
                )
            }

            // Text Field
            NeuTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = "Ask Day anything or assign a task...",
                singleLine = false,
                modifier = Modifier.weight(1f),
                cornerRadius = 16.dp
            )

            // Send Button
            val canSend = (inputText.isNotBlank() || selectedImageUri != null) && !isLoading
            NeuIconButton(
                onClick = {
                    if (canSend) {
                        val text = inputText
                        val uri = selectedImageUri
                        inputText = ""
                        selectedImageUri = null
                        agentViewModel.sendMessage(text, uri)
                    }
                },
                size = 46.dp,
                enabled = canSend,
                tint = if (canSend) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Confirmation dialog for clearing chat
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Chat History?") },
            text = { Text("This will remove all recent messages with Day. Your schedule, todos, and notes will not be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        agentViewModel.clearHistory()
                        showClearConfirm = false
                        Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AgentEmptyState(
    primaryColor: Color,
    onQuickPrompt: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        NeuCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_day_logo),
                    contentDescription = "Day Logo",
                    tint = primaryColor,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Meet Day",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your autonomous AI assistant. I can schedule your day, manage todos, track habits, and remember your notes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Quick Actions:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickActionChip(
                        title = "📅 Build my day",
                        subtitle = "Auto-plan schedule from timetable & todos",
                        onClick = { onQuickPrompt("Build my day schedule based on my timetable and todos") }
                    )
                    QuickActionChip(
                        title = "📝 Add a todo",
                        subtitle = "Fast task entry with priority & recurrence",
                        onClick = { onQuickPrompt("Add a todo: ") }
                    )
                    QuickActionChip(
                        title = "💪 Log a habit",
                        subtitle = "Check off habits & track streak",
                        onClick = { onQuickPrompt("Log habit: ") }
                    )
                    QuickActionChip(
                        title = "🗓️ Set weekly timetable",
                        subtitle = "Store fixed classes & lecture times",
                        onClick = { onQuickPrompt("Here is my weekly timetable:\n") }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AgentChatBubble(
    message: AgentMessage,
    primaryColor: Color,
    isDark: Boolean,
    context: Context
) {
    val isUser = message.role == "user"
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        NeuCard(
            modifier = Modifier.widthIn(max = 320.dp),
            cornerRadius = 16.dp,
            contentPadding = PaddingValues(14.dp)
        ) {
            Column {
                // Attached image thumbnail in bubble
                if (message.imageUri != null) {
                    val bitmap = remember(message.imageUri) {
                        loadBitmapFromUri(context, message.imageUri, 600, 600)
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Message attached image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 22.sp
                )

                // Tool execution summary pills
                if (message.toolsExecuted.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (summary in message.toolsExecuted) {
                            val (bg, textCol) = when {
                                summary.startsWith("✓") -> if (isDark) Pair(Color(0xFF1B3820), Color(0xFF81C784)) else Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
                                summary.startsWith("✗") -> if (isDark) Pair(Color(0xFF3E1C1C), Color(0xFFEF9A9A)) else Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
                                summary.startsWith("↻") -> if (isDark) Pair(Color(0xFF192E46), Color(0xFF90CAF9)) else Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
                                else -> if (isDark) Pair(Color(0xFF2E2C1A), Color(0xFFFFF59D)) else Pair(Color(0xFFFFFDE7), Color(0xFFF57F17))
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = bg
                            ) {
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = textCol,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun AgentLoadingBubble(
    pendingSummary: String?,
    primaryColor: Color,
    isDark: Boolean
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        NeuCard(
            modifier = Modifier.widthIn(max = 240.dp),
            cornerRadius = 16.dp,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BouncingDotsIndicator(color = primaryColor)
                Text(
                    text = pendingSummary ?: "Day is thinking...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BouncingDotsIndicator(color: Color) {
    val transition = rememberInfiniteTransition(label = "dots")
    val dot1Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 140, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 280, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(y = dot1Offset.dp)
                .size(6.dp)
                .background(color, CircleShape)
        )
        Box(
            modifier = Modifier
                .offset(y = dot2Offset.dp)
                .size(6.dp)
                .background(color, CircleShape)
        )
        Box(
            modifier = Modifier
                .offset(y = dot3Offset.dp)
                .size(6.dp)
                .background(color, CircleShape)
        )
    }
}

private fun loadBitmapFromUri(context: Context, uriString: String, reqWidth: Int, reqHeight: Int): ImageBitmap? {
    return try {
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(stream, null, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            context.contentResolver.openInputStream(uri)?.use { stream2 ->
                BitmapFactory.decodeStream(stream2, null, options)?.asImageBitmap()
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
