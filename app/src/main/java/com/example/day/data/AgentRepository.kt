package com.example.day.data

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class AgentResponse(
    val conversationalText: String,
    val toolCalls: List<ToolCall>
)

class AgentRepository {

    private val responseCache = mutableMapOf<String, Pair<String, Long>>()  // query -> (response, timestamp)
    private val CACHE_TTL = 5 * 60 * 1000L  // 5 minutes

    fun getCachedResponse(query: String): String? {
        val cached = responseCache[query.lowercase().trim()] ?: return null
        return if (System.currentTimeMillis() - cached.second < CACHE_TTL) cached.first else null
    }

    fun cacheResponse(query: String, response: String) {
        responseCache[query.lowercase().trim()] = Pair(response, System.currentTimeMillis())
    }

    fun clearCache() {
        responseCache.clear()
    }

    fun buildCurrentDataString(
        todayStr: String,
        tasksToday: List<Task> = emptyList(),
        todosNext7Days: List<Todo> = emptyList(),
        habitsWith14dLogs: List<Pair<Habit, Int>> = emptyList(),
        last3Notes: List<Note> = emptyList()
    ): String {
        val taskLines = if (tasksToday.isEmpty()) "None" else tasksToday.joinToString("; ") {
            "${it.name} (${it.startTime}-${it.endTime})${if (it.isCompleted) " [COMPLETED]" else ""}"
        }
        val todoLines = if (todosNext7Days.isEmpty()) "None" else todosNext7Days.joinToString("; ") {
            "${it.title} (due: ${it.dueDate ?: "none"} ${it.dueTime ?: ""}, P${it.priority})${if (it.isCompleted) " [DONE]" else ""}"
        }
        val habitLines = if (habitsWith14dLogs.isEmpty()) "None" else habitsWith14dLogs.joinToString("; ") { (h, logs14d) ->
            "${h.name} (${h.category}, ${h.targetHoursPerDay}h/day, $logs14d logs/14d)"
        }
        val noteLines = if (last3Notes.isEmpty()) "None" else last3Notes.take(3).joinToString("; ") {
            "${it.title}: ${it.content.take(50)}"
        }
        return """
- Today's Date: $todayStr
- Scheduled Tasks Today: $taskLines
- Active To-dos (next 7 days): $todoLines
- Tracked Habits (14d history): $habitLines
- Recent Notes (last 3): $noteLines
        """.trimIndent()
    }

    private fun isCacheableQuery(query: String): Boolean {
        val q = query.lowercase().trim()
        val nonCacheKeywords = listOf("add", "delete", "remove", "complete", "finish", "done", "reschedule", "change", "cancel", "edit", "update", "create", "new", "set")
        if (nonCacheKeywords.any { q.contains(it) }) return false
        val cacheKeywords = listOf("what's on", "whats on", "what is on", "schedule today", "my schedule", "what do i have", "agenda", "today's plan", "todays plan", "show schedule", "show my tasks")
        return cacheKeywords.any { q.contains(it) }
    }

    companion object {
        private const val TAG = "AgentRepository"
        val TOOLS_REGEX = Regex("```tools\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)

        fun parseGeminiResponse(rawText: String): AgentResponse {
            val match = TOOLS_REGEX.find(rawText)
            if (match != null) {
                val toolsJson = match.groupValues[1].trim()
                val conversational = rawText.replace(match.value, "").trim()
                val tools = parseToolsJson(toolsJson)
                return AgentResponse(conversationalText = conversational, toolCalls = tools)
            }
            return AgentResponse(conversationalText = rawText.trim(), toolCalls = emptyList())
        }

        fun parseToolsJson(jsonStr: String): List<ToolCall> {
            val calls = mutableListOf<ToolCall>()
            try {
                val trimmed = jsonStr.trim()
                val array = if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else if (trimmed.startsWith("{")) {
                    JSONArray().put(JSONObject(trimmed))
                } else {
                    return emptyList()
                }

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val tool = obj.optString("tool", "")
                    val argsMap = mutableMapOf<String, Any?>()
                    if (obj.has("args")) {
                        val argsObj = obj.getJSONObject("args")
                        val keys = argsObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            argsMap[k] = argsObj.get(k)
                        }
                    }
                    if (tool.isNotBlank()) {
                        calls.add(ToolCall(tool = tool, args = argsMap))
                    }
                }
            } catch (e: Exception) {
                try {
                    Log.e(TAG, "Failed to parse tools json: $jsonStr", e)
                } catch (_: Throwable) {
                    System.err.println("Failed to parse tools json: $jsonStr")
                }
            }
            return calls
        }
    }

    suspend fun generateAgentReply(
        apiKey: String,
        recentHistory: List<AgentMessage>,
        userMessage: String,
        imageBytes: ByteArray? = null,
        imageMimeType: String = "image/jpeg",
        currentDataSnapshot: String,
        userContext: UserContext,
        olderContextSummary: String? = null
    ): AgentResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AgentResponse(
                conversationalText = "Please enter your Gemini API key in Settings (tap 🎨 in the top bar) to start chatting with Day.",
                toolCalls = emptyList()
            )
        }

        // Feature 4A: Response caching for repeated read queries
        if (imageBytes == null && isCacheableQuery(userMessage)) {
            val cached = getCachedResponse(userMessage)
            if (cached != null) {
                return@withContext AgentResponse(
                    conversationalText = cached,
                    toolCalls = emptyList()
                )
            }
        }

        try {
            val systemPrompt = buildSystemPrompt(currentDataSnapshot, userContext, olderContextSummary)
            val responseBody = callGeminiApi(
                apiKey = apiKey.trim(),
                systemPrompt = systemPrompt,
                recentHistory = recentHistory,
                latestUserText = userMessage,
                imageBytes = imageBytes,
                imageMimeType = imageMimeType
            )

            if (responseBody != null) {
                val rawReply = extractTextFromGeminiResponse(responseBody)
                val response = parseGeminiResponse(rawReply)
                if (response.toolCalls.isEmpty() && isCacheableQuery(userMessage)) {
                    cacheResponse(userMessage, response.conversationalText)
                }
                return@withContext response
            } else {
                return@withContext AgentResponse(
                    conversationalText = "No connection — check your internet and try again",
                    toolCalls = emptyList()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating agent reply", e)
            val rawMsg = e.localizedMessage ?: ""
            val friendlyMsg = when {
                rawMsg.contains("429") || rawMsg.contains("RESOURCE_EXHAUSTED") || rawMsg.contains("rate limit", ignoreCase = true) ->
                    "Hit the rate limit — wait a minute and try again ⏱️"
                rawMsg.contains("400") || rawMsg.contains("API_KEY_INVALID") ->
                    "API key is invalid — please check your key in Settings (tap 🎨)"
                rawMsg.contains("404") || rawMsg.contains("NOT_FOUND") ->
                    "AI service temporarily unavailable — please try again in a moment"
                e is java.net.SocketTimeoutException || e is java.net.UnknownHostException || e is java.net.ConnectException ||
                rawMsg.contains("timeout", ignoreCase = true) || rawMsg.contains("network", ignoreCase = true) || rawMsg.contains("No route to host", ignoreCase = true) ->
                    "No connection — check your internet and try again"
                else ->
                    "Something went wrong — try again in a bit"
            }
            return@withContext AgentResponse(
                conversationalText = friendlyMsg,
                toolCalls = emptyList()
            )
        }
    }

    private fun buildSystemPrompt(snapshot: String, context: UserContext, olderContextSummary: String? = null): String {
        val olderContextSection = if (!olderContextSummary.isNullOrBlank()) {
            "\nPrior Conversation Context:\n$olderContextSummary\n"
        } else ""

        return """
You are "Day", a capable, proactive, and friendly personal assistant and scheduler embedded in the user's Day app.
You have autonomous control to read and mutate the user's tasks, to-dos, habits, notes, timetable, and daily schedule.

Your Personality:
- Concise, warm, natural, and helpful.
- Direct: state what you did or answer clearly without conversational filler or disclaimers.
- Autonomous: when the user tells you to add, reschedule, finish, delete, or plan anything, perform the action directly.

Current App Snapshot:
$snapshot
$olderContextSection
User Profile & Preferences:
Timetable: ${if (context.timetableText.isBlank()) "None set" else context.timetableText}
Preferences: ${if (context.generalPreferences.isBlank()) "Standard" else context.generalPreferences}
Important People: ${if (context.importantPeople.isBlank()) "None" else context.importantPeople}
Ongoing Projects: ${if (context.ongoingProjects.isBlank()) "None" else context.ongoingProjects}

Tool Execution Instructions:
When you perform any database mutation (adding/editing/deleting tasks, todos, habits, notes, or setting schedule/timetable), you MUST append a tools code block at the very end of your response in this exact format:
```tools
[
  {
    "tool": "<tool_name>",
    "args": {
      "<arg_name>": <arg_value>
    }
  }
]
```

Available Tools:
1. add_task(name: String, start_time: "HH:mm", end_time: "HH:mm", date?: "yyyy-MM-dd", category?: String)
2. delete_task(task_name?: String, task_id?: Int, date?: String)
3. complete_task(task_name?: String, task_id?: Int, date?: String)
4. reschedule_task(task_name?: String, task_id?: Int, new_start_time: "HH:mm", new_end_time: "HH:mm", new_date?: String)
5. add_todo(title: String, due_date?: "yyyy-MM-dd", due_time?: "HH:mm", priority?: 1|2|3, recurrence_type?: "DAILY"|"WEEKLY"|"CUSTOM", recurrence_days?: String)
6. delete_todo(title?: String, todo_id?: Int)
7. complete_todo(title?: String, todo_id?: Int)
8. add_habit(name: String, category?: String, target_hours?: Double)
9. delete_habit(name?: String, habit_id?: Int)
10. add_note(title: String, content: String)
11. delete_note(title?: String, note_id?: Int)
12. save_timetable(timetable_text: String)
13. generate_schedule(date?: "yyyy-MM-dd")
14. clear_schedule(date?: "yyyy-MM-dd")

Rules:
- Always answer user questions accurately using their snapshot and context.
- If the user provides an image (e.g. photo of handwritten notes, lecture board, syllabus, or timetable), analyze the visual content, extract relevant tasks or schedule items, and take action.
- Only output the ```tools ... ``` fence at the END of your message if tools need to be executed.
        """.trimIndent()
    }

    private fun callGeminiApi(
        apiKey: String,
        systemPrompt: String,
        recentHistory: List<AgentMessage>,
        latestUserText: String,
        imageBytes: ByteArray?,
        imageMimeType: String
    ): String? {
        val models = listOf(
            "gemini-1.5-flash",
            "gemini-2.0-flash",
            "gemini-2.5-flash",
            "gemini-3.8-flash",
            "gemini-3.8-flash-lite"
        )
        var lastException: Exception? = null

        // Consolidate history to guarantee strict user/model turn alternation and avoid sending duplicate latest text
        val historyClean = recentHistory.filter { it.content != latestUserText }
        val consolidated = mutableListOf<Pair<String, String>>()
        var lastRole: String? = null

        for (msg in historyClean) {
            val role = if (msg.role == "model") "model" else "user"
            val text = msg.content.replace(TOOLS_REGEX, "").trim()
            if (text.isBlank()) continue

            if (role == lastRole && consolidated.isNotEmpty()) {
                val prev = consolidated.removeAt(consolidated.size - 1)
                consolidated.add(Pair(role, "${prev.second}\n$text"))
            } else {
                consolidated.add(Pair(role, text))
                lastRole = role
            }
        }

        // If history ended on a user turn, remove it so alternating turns (model -> user) is preserved
        if (consolidated.isNotEmpty() && consolidated.last().first == "user") {
            consolidated.removeAt(consolidated.size - 1)
        }

        for (model in models) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-goog-api-key", apiKey)
                conn.doOutput = true
                conn.connectTimeout = 12000
                conn.readTimeout = 30000

                val root = JSONObject()

                // System Instruction
                val systemInstructionObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    }
                    put("parts", parts)
                }
                root.put("system_instruction", systemInstructionObj)

                // Contents (alternating history + current user message)
                val contentsArray = JSONArray()

                for ((role, text) in consolidated) {
                    val item = JSONObject().apply {
                        put("role", role)
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", text))
                        }
                        put("parts", parts)
                    }
                    contentsArray.put(item)
                }

                // Current User Turn
                val latestUserObj = JSONObject().apply {
                    put("role", "user")
                    val parts = JSONArray()
                    parts.put(JSONObject().put("text", latestUserText))

                    if (imageBytes != null && imageBytes.isNotEmpty()) {
                        val base64Data = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                        val inlineData = JSONObject().apply {
                            put("mime_type", imageMimeType)
                            put("data", base64Data)
                        }
                        parts.put(JSONObject().put("inline_data", inlineData))
                    }
                    put("parts", parts)
                }
                contentsArray.put(latestUserObj)

                root.put("contents", contentsArray)

                // Generation Config with low thinking level for fast responses
                val genConfig = JSONObject().apply {
                    if (model.startsWith("gemini-3")) {
                        val thinkingConfig = JSONObject().apply {
                            put("thinkingLevel", "low")
                        }
                        put("thinkingConfig", thinkingConfig)
                    }
                    put("maxOutputTokens", 2048)
                }
                root.put("generationConfig", genConfig)

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(root.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    return BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                } else {
                    val errBody = try {
                        BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                    } catch (_: Exception) { "" }
                    lastException = Exception("Gemini API ($code): $errBody")
                    Log.w(TAG, "Model $model returned HTTP $code ($errBody), gracefully cascading to next model...")
                    continue
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Exception with model $model (${e.localizedMessage}), attempting fallback model...")
                continue
            }
        }
        if (lastException != null) throw lastException
        return null
    }

    private fun extractTextFromGeminiResponse(jsonString: String): String {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                val sb = StringBuilder()
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("text")) {
                        sb.append(p.getString("text"))
                    }
                }
                return sb.toString()
            }
        }
        return "I'm here to help, but I didn't receive a response."
    }
}
