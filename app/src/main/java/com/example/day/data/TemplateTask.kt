package com.example.day.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "template_tasks")
data class TemplateTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val startTime: String, // "HH:mm" format
    val endTime: String,   // "HH:mm" format
    val colorHex: String,  // Hex color string (e.g., "#FFCDD2")
    val category: String   // Category name (e.g., "Work", "Exercise")
)
