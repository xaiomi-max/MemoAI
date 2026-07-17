package com.memoai.app.smartitinerary

enum class SmartItineraryFollowUpType {
    LOCATION_CHANGED,
    TRAFFIC_CONGESTION
}

data class SmartItineraryTrip(
    val memoId: Long,
    val destination: String,
    val finalRemindAtMillis: Long,
    val timeText: String,
    val homeLat: Double?,
    val homeLng: Double?,
    val createdAtMillis: Long
)

data class SmartItinerarySetupPrompt(
    val userInput: String,
    val summary: String,
    val content: String,
    val remindAtMillis: Long,
    val timeText: String,
    val destination: String,
    val suggestedDepartAtMillis: Long,
    val suggestedDepartText: String,
    val cloudProcessed: Boolean
)

data class SmartItineraryFollowUpPrompt(
    val memoId: Long,
    val type: SmartItineraryFollowUpType,
    val message: String,
    val suggestedRemindAtMillis: Long?,
    val suggestedRemindText: String?
)
