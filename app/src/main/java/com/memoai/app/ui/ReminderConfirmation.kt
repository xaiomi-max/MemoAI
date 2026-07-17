package com.memoai.app.ui

data class ReminderConfirmation(
    val userInput: String,
    val summary: String,
    val remindAtMillis: Long,
    val timeText: String
)
