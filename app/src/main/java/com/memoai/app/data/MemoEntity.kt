package com.memoai.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val summary: String,
    val userInput: String,
    val content: String,
    val highlights: String,
    val timestamp: String,
    val remindAtMillis: Long? = null,
    val timeText: String? = null,
    val completed: Boolean = false,
    val cloudProcessed: Boolean = false,
    val createdAtMillis: Long = 0L,
    val completedAtMillis: Long? = null
)
