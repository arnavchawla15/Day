package com.example.day.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Todo entity representing a to-do item.
 * Supports priority, due dates, due times, and recurring patterns.
 */
@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dueDate: String? = null,        // "yyyy-MM-dd" format
    val dueTime: String? = null,        // "HH:mm" format
    val priority: Int = 2,              // 1: High, 2: Medium, 3: Low
    val isCompleted: Boolean = false,
    val recurrenceType: String? = null, // "DAILY", "EVERY_OTHER_DAY", "WEEKLY", "CUSTOM", or null
    val recurrenceDays: String? = null, // e.g. "SUNDAY" or "MON,WED,FRI"
    val createdAt: Long = System.currentTimeMillis()
)
