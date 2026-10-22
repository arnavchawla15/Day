package com.example.day.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Note entity for minimalist quick thoughts and memos.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val colorHex: String = "#FFF9C4", // soft pastel yellow default
    val updatedAt: Long = System.currentTimeMillis()
)
