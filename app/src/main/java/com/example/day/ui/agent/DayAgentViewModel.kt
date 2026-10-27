package com.example.day.ui.agent

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.day.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DayAgentViewModel(
    private val taskDao: TaskDao,
    private val todoDao: TodoDao,
    private val habitDao: HabitDao,
    private val noteDao: NoteDao,
    private val schedulePrefsManager: SchedulePreferencesManager,
    private val geminiScheduleRepo: GeminiScheduleRepository,
    private val appContext: Context
) : ViewModel() {

    private val memoryManager = AgentMemoryManager(appContext)
    private val toolExecutor = AgentToolExecutor(
        taskDao = taskDao,
        todoDao = todoDao,
        habitDao = habitDao,
        noteDao = noteDao,
        schedulePrefsManager = schedulePrefsManager,
        geminiScheduleRepo = geminiScheduleRepo,
        memoryManager = memoryManager,
        appContext = appContext
    )
    private val repository = AgentRepository()
    private val prefs = appContext.getSharedPreferences("day_app_prefs", Context.MODE_PRIVATE)

    private val _messages = MutableStateFlow<List<AgentMessage>>(emptyList())
    val messages: StateFlow<List<AgentMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _pendingToolSummary = MutableStateFlow<String?>(null)
    val pendingToolSummary: StateFlow<String?> = _pendingToolSummary.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = memoryManager.getMessages()
            _messages.value = list
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            memoryManager.clearHistory()
            _messages.value = emptyList()
        }
    }

    private var lastSentImageUri: Uri? = null

    private fun isRetryIntent(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower in listOf("retry", "retry now", "try again", "re-try", "again", "please retry") ||
                lower.startsWith("retry ")
    }

    private fun compressImageForGemini(context: Context, uri: Uri, maxDimension: Int = 1280, quality: Int = 80): ByteArray? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val origW = options.outWidth
            val origH = options.outHeight
            if (origW <= 0 || origH <= 0) {
                return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }

            var sampleSize = 1
            while (origW / (sampleSize * 2) >= maxDimension && origH / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val curW = decodedBitmap.width
            val curH = decodedBitmap.height
            val finalBitmap = if (curW > maxDimension || curH > maxDimension) {
                val scale = maxDimension.toFloat() / maxOf(curW, curH)
                val targetW = (curW * scale).toInt().coerceAtLeast(1)
                val targetH = (curH * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true)
                if (scaled != decodedBitmap) {
                    decodedBitmap.recycle()
                }
                scaled
            } else {
                decodedBitmap
            }

            val baos = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            finalBitmap.recycle()
            baos.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: Exception) {
                null
            }
        }
    }

    fun sendMessage(userText: String, imageUri: Uri? = null) {
        val trimmed = userText.trim()
        val isRetry = isRetryIntent(trimmed)
        val effectiveImageUri = imageUri ?: if (isRetry) lastSentImageUri else null

        if (trimmed.isBlank() && effectiveImageUri == null) return

        if (effectiveImageUri != null) {
            lastSentImageUri = effectiveImageUri
        }

        val effectiveText = when {
            trimmed.isNotBlank() && isRetry && effectiveImageUri != null && trimmed.length <= 15 ->
                "Please analyze the timetable/schedule image I shared and add the classes or tasks to my schedule."
            trimmed.isNotBlank() -> trimmed
            effectiveImageUri != null -> "Please analyze this image, extract any classes, tasks, or schedule details, and add them to my schedule."
            else -> trimmed
        }

        viewModelScope.launch {
            // Snapshot prior history BEFORE adding the new turn
            val priorHistory = _messages.value.takeLast(10)

            val userMsg = AgentMessage(
                role = "user",
                content = if (trimmed.isNotBlank()) trimmed else "(Sent an image)",
                imageUri = effectiveImageUri?.toString(),
                timestamp = System.currentTimeMillis()
            )

            // Update UI immediately with user message
            val currentList = _messages.value.toMutableList()
            currentList.add(userMsg)
            _messages.value = currentList
            withContext(Dispatchers.IO) { memoryManager.saveMessages(currentList) }

            _isLoading.value = true

            try {
                // Read & compress image bytes (downsamples to max 1280px JPEG ~150KB for fast, reliable upload)
                val imageBytes: ByteArray? = if (effectiveImageUri != null) {
                    withContext(Dispatchers.IO) {
                        compressImageForGemini(appContext, effectiveImageUri)
                    }
                } else null

                val apiKey = prefs.getString("gemini_api_key", "")?.trim() ?: ""
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
                val snapshot = buildCurrentSnapshot(todayStr)
                val userContext = memoryManager.getUserContext()
                val olderSummary = memoryManager.getOlderContextSummary(10)

                val response = repository.generateAgentReply(
                    apiKey = apiKey,
                    recentHistory = priorHistory,
                    userMessage = effectiveText,
                    imageBytes = imageBytes,
                    imageMimeType = "image/jpeg",
                    currentDataSnapshot = snapshot,
                    userContext = userContext,
                    olderContextSummary = olderSummary
                )

                // Execute tool calls if any
                val executedSummaries = mutableListOf<String>()
                if (response.toolCalls.isNotEmpty()) {
                    repository.clearCache()
                    for (toolCall in response.toolCalls) {
                        _pendingToolSummary.value = "Executing ${toolCall.tool}..."
                        val result = toolExecutor.executeTool(toolCall, apiKey)
                        executedSummaries.add(result.summary)
                    }
                    _pendingToolSummary.value = null
                }

                // Add model response
                val modelMsg = AgentMessage(
                    role = "model",
                    content = response.conversationalText,
                    timestamp = System.currentTimeMillis(),
                    toolsExecuted = executedSummaries
                )

                val updatedList = _messages.value.toMutableList()
                updatedList.add(modelMsg)
                _messages.value = updatedList
                withContext(Dispatchers.IO) { memoryManager.saveMessages(updatedList) }

            } catch (e: Exception) {
                val rawMsg = e.localizedMessage ?: ""
                val friendlyMsg = when {
                    rawMsg.contains("429") || rawMsg.contains("RESOURCE_EXHAUSTED") || rawMsg.contains("rate limit", ignoreCase = true) ->
                        "Hit the rate limit — wait a minute and try again ⏱️"
                    rawMsg.contains("400") || rawMsg.contains("API_KEY_INVALID") ->
                        "API key is invalid — please check your key in Settings (tap 🎨)"
                    rawMsg.contains("404") || rawMsg.contains("NOT_FOUND") ->
                        "AI service temporarily unavailable — please try again in a moment"
                    e is java.net.SocketTimeoutException || e is java.net.UnknownHostException || e is java.net.ConnectException ||
                    rawMsg.contains("timeout", ignoreCase = true) || rawMsg.contains("network", ignoreCase = true) ->
                        "No connection — check your internet and try again"
                    else ->
                        "Something went wrong — try again in a bit"
                }
                val errorMsg = AgentMessage(
                    role = "model",
                    content = friendlyMsg,
                    timestamp = System.currentTimeMillis()
                )
                val updatedList = _messages.value.toMutableList()
                updatedList.add(errorMsg)
                _messages.value = updatedList
            } finally {
                _isLoading.value = false
                _pendingToolSummary.value = null
            }
        }
    }

    private suspend fun buildCurrentSnapshot(todayStr: String): String = withContext(Dispatchers.IO) {
        val tasks = taskDao.getTasksForDateSync(todayStr)
        val todos = todoDao.getTodosForWidgetSync(todayStr)
        val habits = habitDao.getAllHabitsSync()
        val notes = noteDao.getNotesSync()

        // Feature 4B: Only tasks for today
        val taskLines = if (tasks.isEmpty()) "None" else tasks.joinToString("; ") {
            "${it.name} (${it.startTime}-${it.endTime})${if (it.isCompleted) " [COMPLETED]" else ""}"
        }

        // Feature 4B: Only todos due in next 7 days
        val cal7 = Calendar.getInstance()
        cal7.add(Calendar.DAY_OF_YEAR, 7)
        val plus7DaysStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal7.time)
        val next7DaysTodos = todos.filter { it.dueDate == null || (it.dueDate >= todayStr && it.dueDate <= plus7DaysStr) }
        val todoLines = if (next7DaysTodos.isEmpty()) "None" else next7DaysTodos.joinToString("; ") {
            "${it.title} (due: ${it.dueDate ?: "none"} ${it.dueTime ?: ""}, P${it.priority})${if (it.isCompleted) " [DONE]" else ""}"
        }

        // Feature 4B: Truncate habit history to last 14 days
        val cal14 = Calendar.getInstance()
        cal14.add(Calendar.DAY_OF_YEAR, -14)
        val cutoff14Str = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal14.time)
        val recentLogs = habitDao.getAllLogsSync().filter { it.date >= cutoff14Str }
        val habitLines = if (habits.isEmpty()) "None" else habits.joinToString("; ") { h ->
            val completionsIn14d = recentLogs.count { it.habitId == h.id }
            "${h.name} (${h.category}, ${h.targetHoursPerDay}h/day, $completionsIn14d logs/14d)"
        }

        // Feature 4B: Only last 3 notes
        val noteLines = if (notes.isEmpty()) "None" else notes.take(3).joinToString("; ") {
            "${it.title}: ${it.content.take(50)}"
        }

        val now = SimpleDateFormat("EEEE, yyyy-MM-dd HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)

        """
- Current Date/Time: $now
- Today's Date: $todayStr
- Scheduled Tasks Today: $taskLines
- Active To-dos (next 7 days): $todoLines
- Tracked Habits (14d history): $habitLines
- Recent Notes (last 3): $noteLines
        """.trimIndent()
    }
}
