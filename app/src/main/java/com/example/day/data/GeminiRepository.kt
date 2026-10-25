package com.example.day.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class ParsedResult {
    data class TaskResult(val title: String, val startTime: String, val endTime: String) : ParsedResult()
    data class TodoResult(
        val title: String,
        val dueDate: String?,
        val dueTime: String?,
        val priority: String,
        val recurrenceType: String? = null,
        val recurrenceDays: String? = null
    ) : ParsedResult()
    data class HabitResult(val name: String, val category: String) : ParsedResult()
    data class NoteResult(val title: String, val content: String) : ParsedResult()
}

class GeminiRepository {

    suspend fun parseInput(apiKey: String?, input: String): ParsedResult = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (!apiKey.isNullOrBlank()) {
            try {
                val apiResult = callGeminiApi(apiKey.trim(), trimmed)
                if (apiResult != null) {
                    return@withContext apiResult
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return@withContext fallbackParse(trimmed)
    }

    suspend fun parseMultipleInputs(apiKey: String?, input: String): List<ParsedResult> = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val lines = if (trimmed.contains("\n")) {
            trimmed.split("\n")
        } else if (trimmed.contains(",") && !trimmed.contains("to") && !trimmed.contains("-")) {
            trimmed.split(",")
        } else {
            listOf(trimmed)
        }.map { it.trim() }.filter { it.isNotBlank() }

        if (lines.size <= 1) {
            return@withContext listOf(parseInput(apiKey, trimmed))
        }

        val results = mutableListOf<ParsedResult>()
        for (item in lines) {
            results.add(parseInput(apiKey, item))
        }
        return@withContext results
    }

    suspend fun validateApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isBlank()) return@withContext Result.failure(Exception("API key cannot be empty"))

        // Fast check using official models list endpoint
        try {
            val listUrl = URL("https://generativelanguage.googleapis.com/v1beta/models?key=$key")
            val listConn = listUrl.openConnection() as HttpURLConnection
            listConn.requestMethod = "GET"
            listConn.setRequestProperty("x-goog-api-key", key)
            listConn.connectTimeout = 8000
            listConn.readTimeout = 8000
            val listCode = listConn.responseCode
            if (listCode in 200..299) {
                return@withContext Result.success(true)
            } else if (listCode in 400..403) {
                val errBody = try {
                    BufferedReader(InputStreamReader(listConn.errorStream ?: listConn.inputStream)).use { it.readText() }
                } catch (_: Exception) { "" }
                val errorMsg = if (errBody.contains("API_KEY_INVALID") || errBody.contains("API key not valid")) {
                    "Invalid API key. Please check Google AI Studio."
                } else {
                    "API key verification failed (HTTP $listCode)"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            // If models GET fails due to network or timeout, continue to generateContent cascade
        }

        val models = listOf(
            "gemini-1.5-flash",
            "gemini-2.0-flash",
            "gemini-2.5-flash",
            "gemini-3.8-flash"
        )
        var lastErr = "Please check your internet connection."

        for (model in models) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-goog-api-key", key)
                conn.doOutput = true
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val jsonBody = JSONObject().apply {
                    val contents = org.json.JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val parts = org.json.JSONArray().apply {
                                put(JSONObject().put("text", "hi"))
                            }
                            put("parts", parts)
                        }
                        put(contentObj)
                    }
                    put("contents", contents)
                }

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(jsonBody.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    return@withContext Result.success(true)
                } else if (code == 404 || code == 503 || code == 429 || code == 500) {
                    continue
                } else {
                    val errBody = try {
                        BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                    } catch (_: Exception) { "" }
                    lastErr = if (errBody.contains("API_KEY_INVALID")) {
                        "Invalid API key. Please check Google AI Studio."
                    } else {
                        "HTTP $code: Please check key validity."
                    }
                }
            } catch (e: Exception) {
                lastErr = e.localizedMessage ?: "Network error"
            }
        }
        Result.failure(Exception(lastErr))
    }

    private fun callGeminiApi(apiKey: String, input: String): ParsedResult? {
        val models = listOf(
            "gemini-1.5-flash",
            "gemini-2.0-flash",
            "gemini-2.5-flash",
            "gemini-3.8-flash",
            "gemini-3.8-flash-lite"
        )
        val now = Calendar.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
        val dayOfWeek = SimpleDateFormat("EEEE", Locale.US).format(now.time)

        val systemPrompt = """
You are a parser for a productivity app. Parse the user's input and return ONLY valid JSON.

The user can create 4 types of items:
1. task — a scheduled time block for today (has start and end time)
2. todo — a to-do item (may have a due date, priority, recurrence)  
3. habit — a habit to track (no time, just a name and category)
4. note — a quick note or thought

Parse this input: "$input"

Today is $todayStr. Day of week: $dayOfWeek.

Return JSON in exactly this format:
{
  "type": "task" | "todo" | "habit" | "note",
  "name": "string",
  "startTime": "HH:mm" or null,
  "endTime": "HH:mm" or null,
  "dueDate": "YYYY-MM-DD" or null,
  "priority": 1|2|3|4 or null,
  "recurrence": "NONE"|"DAILY"|"EVERY_OTHER_DAY"|"WEEKLY:MON"|"CUSTOM:MON,WED,FRI" or null,
  "category": "Skill"|"Health"|"Startup"|"Personal" or null,
  "noteBody": "string" or null
}

Rules:
- If input mentions a time range (e.g. "7am to 8am", "9-10"), it's a task
- If input mentions "tomorrow", "next week", "due", it's a todo
- If input mentions "habit", "every day", "track", it's a habit
- If input starts with "note:", "remember:", or is a thought/idea, it's a note
- For priority: p1/urgent/critical=1, p2/high=2, p3/medium=3, p4/low=4, default=3
- Times must be in 24-hour HH:mm format
- Return ONLY the JSON object, no markdown, no explanation
""".trimIndent()

        val jsonBody = JSONObject().apply {
            val contents = org.json.JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = org.json.JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)
        }

        for (model in models) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(jsonBody.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val responseJson = JSONObject(responseText)
                    val candidates = responseJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            var rawText = parts.getJSONObject(0).optString("text", "")
                            rawText = rawText.replace("```json", "").replace("```", "").trim()
                            val startIdx = rawText.indexOf('{')
                            val endIdx = rawText.lastIndexOf('}')
                            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                                rawText = rawText.substring(startIdx, endIdx + 1)
                            }

                            val parsed = JSONObject(rawText)
                            val type = parsed.optString("type", "todo").trim().lowercase(Locale.ROOT)
                            val name = parsed.optString("name", input).trim()

                            return when (type) {
                                "task" -> ParsedResult.TaskResult(
                                    title = name.ifBlank { input },
                                    startTime = parsed.optString("startTime", "09:00").ifBlank { "09:00" },
                                    endTime = parsed.optString("endTime", "10:00").ifBlank { "10:00" }
                                )
                                "habit" -> ParsedResult.HabitResult(
                                    name = name.ifBlank { input },
                                    category = parsed.optString("category", "Skill").ifBlank { "Skill" }
                                )
                                "note" -> ParsedResult.NoteResult(
                                    title = name.ifBlank { input.take(30) },
                                    content = parsed.optString("noteBody", name).ifBlank { input }
                                )
                                else -> {
                                    val recurrenceStr = parsed.optString("recurrence", "NONE")
                                    val (recType, recDays) = parseRecurrenceField(recurrenceStr)
                                    val priorityInt = parsed.optInt("priority", 3)
                                    ParsedResult.TodoResult(
                                        title = name.ifBlank { input },
                                        dueDate = parsed.optString("dueDate", "").ifBlank { null },
                                        dueTime = parsed.optString("dueTime", "").ifBlank { null },
                                        priority = "P$priorityInt",
                                        recurrenceType = recType,
                                        recurrenceDays = recDays
                                    )
                                }
                            }
                        }
                    }
                } else {
                    continue
                }
            } catch (e: Exception) {
                // Try next model
            }
        }
        return null
    }

    private fun parseRecurrenceField(recurrence: String?): Pair<String?, String?> {
        if (recurrence.isNullOrBlank() || recurrence.equals("NONE", ignoreCase = true)) {
            return Pair("NONE", null)
        }
        val clean = recurrence.trim().uppercase(Locale.ROOT)
        return when {
            clean.startsWith("WEEKLY:") -> Pair("WEEKLY", clean.substringAfter("WEEKLY:").trim())
            clean.startsWith("CUSTOM:") -> Pair("CUSTOM", clean.substringAfter("CUSTOM:").trim())
            clean == "DAILY" -> Pair("DAILY", null)
            clean == "EVERY_OTHER_DAY" -> Pair("EVERY_OTHER_DAY", null)
            clean == "WEEKLY" -> Pair("WEEKLY", "MON")
            clean == "CUSTOM" -> Pair("CUSTOM", "MON,WED,FRI")
            else -> Pair(clean, null)
        }
    }

    private fun fallbackParse(input: String): ParsedResult {
        val lower = input.lowercase(Locale.ROOT)
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(cal.time)
        val tomorrowCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        val tomorrowStr = sdf.format(tomorrowCal.time)

        // Improved regex patterns:
        val timeRangeRegex = Regex("""(\d{1,2}(?::\d{2})?\s*(?:am|pm)?)\s*(?:to|-|–)\s*(\d{1,2}(?::\d{2})?\s*(?:am|pm)?)""", RegexOption.IGNORE_CASE)
        val tomorrowRegex = Regex("""tomorrow|tmr|tmrw""", RegexOption.IGNORE_CASE)
        val habitRegex = Regex("""habit|every\s+day|daily|track|streak""", RegexOption.IGNORE_CASE)
        val noteRegex = Regex("""^(note:|remember:|idea:|thought:)""", RegexOption.IGNORE_CASE)
        val priorityRegex = Regex("""p([1-4])|#(urgent|high|medium|low)""", RegexOption.IGNORE_CASE)

        // 1. Note detection
        if (noteRegex.containsMatchIn(input) || lower.startsWith("note:") || lower.startsWith("memo:")) {
            val content = input.replace(noteRegex, "").trim().ifBlank { input }
            return ParsedResult.NoteResult(
                title = content.take(30),
                content = content
            )
        }

        // 2. Time range detection -> Task
        val timeMatch = timeRangeRegex.find(input)
        if (timeMatch != null) {
            var startRaw = timeMatch.groupValues[1].trim()
            var endRaw = timeMatch.groupValues[2].trim()
            val startLower = startRaw.lowercase(Locale.ROOT)
            val endLower = endRaw.lowercase(Locale.ROOT)

            if (!startLower.contains("am") && !startLower.contains("pm")) {
                if (endLower.contains("am")) {
                    startRaw += "am"
                } else if (endLower.contains("pm")) {
                    val startH = startRaw.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
                    val endH = endRaw.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
                    if (startH in 1..7 && endH in 1..7) {
                        startRaw += "pm"
                    } else if (startH > endH && startH in 8..11) {
                        startRaw += "am"
                    } else {
                        startRaw += "pm"
                    }
                }
            }

            val cleanTitle = input.replace(timeMatch.value, "")
                .replace(Regex("""\bat\b""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\btoday\b""", RegexOption.IGNORE_CASE), "")
                .trim()
            return ParsedResult.TaskResult(
                title = cleanTitle.ifBlank { "Task" },
                startTime = parseTo24Hr(startRaw, "09:00"),
                endTime = parseTo24Hr(endRaw, "10:00")
            )
        }

        // 3. Habit detection
        if (habitRegex.containsMatchIn(input) && !tomorrowRegex.containsMatchIn(input)) {
            val habitName = input.replace(habitRegex, "")
                .replace("track", "", ignoreCase = true)
                .replace("my", "", ignoreCase = true)
                .trim()
            return ParsedResult.HabitResult(
                name = habitName.ifBlank { "New Habit" },
                category = when {
                    lower.contains("gym") || lower.contains("run") || lower.contains("water") || lower.contains("walk") -> "Health"
                    lower.contains("code") || lower.contains("read") || lower.contains("study") -> "Skill"
                    lower.contains("startup") || lower.contains("work") -> "Startup"
                    else -> "Personal"
                }
            )
        }

        // 4. To-Do detection (default)
        val isTomorrow = tomorrowRegex.containsMatchIn(input)
        val isToday = lower.contains("today")
        val dueDate = when {
            isTomorrow -> tomorrowStr
            isToday -> todayStr
            else -> null
        }

        val priorityMatch = priorityRegex.find(input)
        val priority = when {
            priorityMatch != null -> {
                val pNum = priorityMatch.groupValues[1]
                val pWord = priorityMatch.groupValues[2].lowercase(Locale.ROOT)
                when {
                    pNum.isNotBlank() -> "P$pNum"
                    pWord == "urgent" -> "P1"
                    pWord == "high" -> "P2"
                    pWord == "low" -> "P4"
                    else -> "P3"
                }
            }
            lower.contains("p1") || lower.contains("urgent") -> "P1"
            lower.contains("p2") || lower.contains("high") -> "P2"
            lower.contains("p4") || lower.contains("low") -> "P4"
            else -> "P3"
        }

        val cleanTitle = input
            .replace(tomorrowRegex, "")
            .replace(priorityRegex, "")
            .replace(Regex("""\btoday\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\bdue\b""", RegexOption.IGNORE_CASE), "")
            .trim()

        val isRecurring = lower.contains("every day") || lower.contains("daily")
        return ParsedResult.TodoResult(
            title = cleanTitle.ifBlank { input },
            dueDate = dueDate,
            dueTime = null,
            priority = priority,
            recurrenceType = if (isRecurring) "DAILY" else "NONE",
            recurrenceDays = null
        )
    }

    private fun parseTo24Hr(timeStr: String, defaultTime: String): String {
        return try {
            val clean = timeStr.trim().lowercase(Locale.ROOT)
            if (clean.contains("am") || clean.contains("pm")) {
                val isPm = clean.contains("pm")
                val isAm = clean.contains("am")
                val digitsOnly = clean.replace("am", "").replace("pm", "").trim()
                val parts = digitsOnly.split(":")
                var h = parts[0].toInt()
                val m = if (parts.size > 1) parts[1].toInt() else 0
                if (isPm && h < 12) h += 12
                if (isAm && h == 12) h = 0
                String.format(Locale.US, "%02d:%02d", h, m)
            } else if (clean.contains(":")) {
                val parts = clean.split(":")
                val h = parts[0].padStart(2, '0')
                val m = parts[1].padStart(2, '0')
                "$h:$m"
            } else {
                val h = clean.toInt().toString().padStart(2, '0')
                "$h:00"
            }
        } catch (e: Exception) {
            defaultTime
        }
    }
}
