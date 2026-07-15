package com.example.day.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val startTime: String, // "HH:mm" format
    val endTime: String,   // "HH:mm" format
    val colorHex: String,  // Hex color string (e.g., "#FFCDD2")
    val isCompleted: Boolean = false,
    val date: String,      // "yyyy-MM-dd" format
    val templateTaskId: Int? = null,
    val category: String = "General",
    val plannedDurationMinutes: Int = 0,
    val actualDurationMinutes: Int = 0
)
