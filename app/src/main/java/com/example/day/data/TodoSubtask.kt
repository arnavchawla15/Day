package com.example.day.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Subtask step inside a Todo item.
 */
data class TodoSubtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
)

object SubtaskHelper {
    fun parseSubtasks(json: String?): List<TodoSubtask> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<TodoSubtask>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TodoSubtask(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeSubtasks(subtasks: List<TodoSubtask>): String {
        if (subtasks.isEmpty()) return ""
        val array = JSONArray()
        for (item in subtasks) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("isCompleted", item.isCompleted)
            array.put(obj)
        }
        return array.toString()
    }
}
