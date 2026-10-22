package com.example.day.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Habit entity.
 * Note: Habits at rest have NO time attached.
 * Time is only assigned when a habit is added to Today's schedule.
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String = "Skill",
    val colorHex: String = "#81C784", // default soft sage green
    val targetHoursPerDay: Double = 1.0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Habit log tracking daily completions.
 */
@Entity(tableName = "habit_logs")
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val date: String,                  // "yyyy-MM-dd" format
    val completedAt: Long = System.currentTimeMillis()
)
