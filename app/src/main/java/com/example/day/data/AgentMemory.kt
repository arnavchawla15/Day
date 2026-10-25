package com.example.day.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data class representing an individual chat message in the Day agent conversation.
 */
data class AgentMessage(
    val role: String, // "user" or "model"
    val content: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val toolsExecuted: List<String> = emptyList()
)

/**
 * User context holding long-term preferences, timetable notes, and user profile data.
 */
data class UserContext(
    val timetableText: String = "",
    val generalPreferences: String = "",
    val importantPeople: String = "",
    val ongoingProjects: String = ""
)

/**
 * Manages persistent storage of agent chat messages and user context via SharedPreferences.
 */
class AgentMemoryManager(context: Context) {

    private val memoryPrefs = context.getSharedPreferences(PREFS_MEMORY, Context.MODE_PRIVATE)
    private val contextPrefs = context.getSharedPreferences(PREFS_CONTEXT, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_MEMORY = "day_agent_memory"
        private const val PREFS_CONTEXT = "day_agent_context"

        private const val KEY_MESSAGES = "agent_messages"
        private const val KEY_TIMETABLE = "context_timetable"
        private const val KEY_PREFERENCES = "context_preferences"
        private const val KEY_PEOPLE = "context_people"
        private const val KEY_PROJECTS = "context_projects"

        const val MAX_PERSISTED_MESSAGES = 50
    }

    @Synchronized
    fun getMessages(): List<AgentMessage> {
        val jsonStr = memoryPrefs.getString(KEY_MESSAGES, null) ?: return emptyList()
        val list = mutableListOf<AgentMessage>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val role = obj.optString("role", "user")
                val content = obj.optString("content", "")
                val imageUri = if (obj.has("imageUri") && !obj.isNull("imageUri")) obj.optString("imageUri") else null
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                val tools = mutableListOf<String>()
                if (obj.has("toolsExecuted")) {
                    val toolsArr = obj.getJSONArray("toolsExecuted")
                    for (j in 0 until toolsArr.length()) {
                        tools.add(toolsArr.getString(j))
                    }
                }
                list.add(AgentMessage(role, content, imageUri, timestamp, tools))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @Synchronized
    fun getRecentMessages(limit: Int = 10): List<AgentMessage> {
        val all = getMessages()
        return if (all.size > limit) all.takeLast(limit) else all
    }

    @Synchronized
    fun getOlderContextSummary(limit: Int = 10): String? {
        val all = getMessages()
        if (all.size <= limit) return null
        val older = all.dropLast(limit)
        val actions = mutableListOf<String>()
        val allTools = older.flatMap { it.toolsExecuted }
        if (allTools.isNotEmpty()) {
            val habitActions = allTools.count { it.contains("habit", ignoreCase = true) }
            val taskActions = allTools.count { it.contains("task", ignoreCase = true) || it.contains("schedule", ignoreCase = true) }
            val todoActions = allTools.count { it.contains("todo", ignoreCase = true) }
            if (habitActions > 0) actions.add("worked with $habitActions habit(s)")
            if (taskActions > 0) actions.add("managed $taskActions task(s)")
            if (todoActions > 0) actions.add("completed/updated $todoActions todo(s)")
        }
        val userSnippets = older.filter { it.role == "user" }
            .map { it.content.replace("\n", " ").trim() }
            .filter { it.isNotBlank() }
            .takeLast(3)

        return if (actions.isNotEmpty()) {
            "Earlier: ${actions.joinToString(", ")}"
        } else if (userSnippets.isNotEmpty()) {
            "Earlier discussion: user mentioned ${userSnippets.joinToString("; ") { "'${it.take(30)}'" }}"
        } else {
            null
        }
    }

    @Synchronized
    fun saveMessages(messages: List<AgentMessage>) {
        val truncated = if (messages.size > MAX_PERSISTED_MESSAGES) {
            messages.takeLast(MAX_PERSISTED_MESSAGES)
        } else {
            messages
        }
        val array = JSONArray()
        for (msg in truncated) {
            val obj = JSONObject().apply {
                put("role", msg.role)
                put("content", msg.content)
                if (msg.imageUri != null) put("imageUri", msg.imageUri)
                put("timestamp", msg.timestamp)
                val toolsArr = JSONArray()
                msg.toolsExecuted.forEach { toolsArr.put(it) }
                put("toolsExecuted", toolsArr)
            }
            array.put(obj)
        }
        memoryPrefs.edit().putString(KEY_MESSAGES, array.toString()).apply()
    }

    @Synchronized
    fun addMessage(message: AgentMessage) {
        val current = getMessages().toMutableList()
        current.add(message)
        saveMessages(current)
    }

    @Synchronized
    fun clearHistory() {
        memoryPrefs.edit().remove(KEY_MESSAGES).apply()
    }

    fun getUserContext(): UserContext {
        return UserContext(
            timetableText = contextPrefs.getString(KEY_TIMETABLE, "") ?: "",
            generalPreferences = contextPrefs.getString(KEY_PREFERENCES, "") ?: "",
            importantPeople = contextPrefs.getString(KEY_PEOPLE, "") ?: "",
            ongoingProjects = contextPrefs.getString(KEY_PROJECTS, "") ?: ""
        )
    }

    fun saveUserContext(userContext: UserContext) {
        contextPrefs.edit()
            .putString(KEY_TIMETABLE, userContext.timetableText)
            .putString(KEY_PREFERENCES, userContext.generalPreferences)
            .putString(KEY_PEOPLE, userContext.importantPeople)
            .putString(KEY_PROJECTS, userContext.ongoingProjects)
            .apply()
    }

    fun updateTimetable(timetableText: String) {
        val current = getUserContext()
        saveUserContext(current.copy(timetableText = timetableText))
    }
}
