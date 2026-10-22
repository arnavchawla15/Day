package com.example.day.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ScheduledItem(
    val name: String,
    val startTime: String,
    val endTime: String,
    val type: String, // "task", "habit", "class", "break"
    val habitName: String? = null
)

class GeminiScheduleRepository(private val context: Context) {

    suspend fun generateDaySchedule(
        apiKey: String?,
        prefs: SchedulePreferences,
        dayOfWeek: String,
        dateString: String,
        existingTasks: List<Task>,
        todayTodos: List<Todo>,
        habits: List<Habit>
    ): List<ScheduledItem> = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank()) {
            Log.d(TAG, "Gemini API key is blank. Skipping AI auto-schedule.")
            return@withContext emptyList()
        }

        try {
            val responseJson = callGeminiScheduleApi(
                apiKey = apiKey.trim(),
                prefs = prefs,
                dayOfWeek = dayOfWeek,
                dateString = dateString,
                existingTasks = existingTasks,
                todayTodos = todayTodos,
                habits = habits
            ) ?: return@withContext emptyList()

            return@withContext parseScheduleJson(responseJson)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating day schedule via Gemini", e)
            return@withContext emptyList()
        }
    }

    private fun callGeminiScheduleApi(
        apiKey: String,
        prefs: SchedulePreferences,
        dayOfWeek: String,
        dateString: String,
        existingTasks: List<Task>,
        todayTodos: List<Todo>,
        habits: List<Habit>
    ): String? {
        val models = listOf(
            "gemini-3.8-flash",
            "gemini-3.8-flash-lite",
            "gemini-3.5-flash",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite"
        )

        val existingTasksSummary = if (existingTasks.isEmpty()) "None" else {
            existingTasks.joinToString("; ") { "${it.name} (${it.startTime}-${it.endTime})" }
        }

        val todayTodosSummary = if (todayTodos.isEmpty()) "None" else {
            todayTodos.joinToString("; ") { "${it.title} (priority: ${it.priority}, due: ${it.dueTime ?: "anytime"})" }
        }

        val habitsSummary = if (habits.isEmpty()) "None" else {
            habits.joinToString("; ") { "${it.name} [category: ${it.category}]" }
        }

        val systemPrompt = """
You are a personal scheduling assistant for a university student. 
Your job is to generate a realistic, achievable daily schedule.

Rules:
1. Never schedule anything before wake-up time or after sleep time
2. Leave at least 30 minutes buffer between tasks
3. Respect fixed class times from the timetable — these are non-negotiable
4. Fit in habits the user has specified in their instructions
5. Include short breaks (15-30 min) after every 2 hours of work
6. Don't overschedule — a realistic day has 6-8 focused tasks max
7. If a todo is due today, try to fit it in at a reasonable time
8. Output ONLY valid JSON, no explanation text

Output format:
{
  "schedule": [
    {
      "name": "Gym",
      "startTime": "07:00",
      "endTime": "08:30",
      "type": "habit",
      "habitName": "Gym"
    }
  ]
}
""".trimIndent()

        val promptContext = """
$systemPrompt

User Parameters:
- Today's Date: $dateString
- Day of Week: $dayOfWeek
- Wake-up Time: ${prefs.wakeUpTime}
- Sleep Time: ${prefs.sleepTime}
- User's Weekly Timetable:
${prefs.weeklyTimetable.ifBlank { "No fixed timetable specified." }}
- User's General Instructions:
${prefs.generalInstructions.ifBlank { "No extra instructions." }}
- User's Habit Library (must match habitName exactly if type='habit'):
$habitsSummary
- Already Scheduled Tasks for Today (do NOT duplicate or overlap):
$existingTasksSummary
- To-Dos Due Today:
$todayTodosSummary
""".trimIndent()

        val jsonBody = JSONObject().apply {
            val contents = org.json.JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = org.json.JSONArray().apply {
                        put(JSONObject().put("text", promptContext))
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
                conn.connectTimeout = 12000
                conn.readTimeout = 12000

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(jsonBody.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val root = JSONObject(response)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return parts.getJSONObject(0).optString("text")
                        }
                    }
                } else {
                    Log.w(TAG, "Gemini API HTTP Error $code: ${conn.responseMessage}, cascading to next model...")
                    continue
                }
            } catch (e: Exception) {
                // Try next model
            }
        }
        return null
    }

    private fun parseScheduleJson(rawText: String): List<ScheduledItem> {
        val clean = rawText
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val startIdx = clean.indexOf('{')
        val endIdx = clean.lastIndexOf('}')
        if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) return emptyList()

        val jsonString = clean.substring(startIdx, endIdx + 1)
        val json = JSONObject(jsonString)
        val scheduleArray = json.optJSONArray("schedule") ?: return emptyList()

        val result = mutableListOf<ScheduledItem>()
        for (i in 0 until scheduleArray.length()) {
            val item = scheduleArray.optJSONObject(i) ?: continue
            val name = item.optString("name", "").trim()
            val startTime = item.optString("startTime", "").trim()
            val endTime = item.optString("endTime", "").trim()
            val type = item.optString("type", "task").trim().lowercase()
            val habitName = if (item.has("habitName") && !item.isNull("habitName")) item.getString("habitName").trim() else null

            if (name.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()) {
                result.add(
                    ScheduledItem(
                        name = name,
                        startTime = startTime,
                        endTime = endTime,
                        type = type,
                        habitName = if (habitName.isNullOrBlank()) null else habitName
                    )
                )
            }
        }
        return result
    }

    companion object {
        private const val TAG = "GeminiScheduleRepo"
    }
}
