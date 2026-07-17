package com.memoai.app.smartitinerary

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class SmartItineraryStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("smart_itinerary", Context.MODE_PRIVATE)

    fun saveTrip(trip: SmartItineraryTrip) {
        prefs.edit()
            .putString(tripKey(trip.memoId), tripToJson(trip).toString())
            .apply()
    }

    fun getTrip(memoId: Long): SmartItineraryTrip? {
        val raw = prefs.getString(tripKey(memoId), null) ?: return null
        return runCatching { tripFromJson(JSONObject(raw)) }.getOrNull()
    }

    fun updateFinalRemindAt(memoId: Long, finalRemindAtMillis: Long, timeText: String) {
        val trip = getTrip(memoId) ?: return
        saveTrip(trip.copy(finalRemindAtMillis = finalRemindAtMillis, timeText = timeText))
    }

    fun removeTrip(memoId: Long) {
        prefs.edit().remove(tripKey(memoId)).apply()
    }

    fun saveHomeLocation(lat: Double, lng: Double, label: String = "家") {
        prefs.edit()
            .putFloat("home_lat", lat.toFloat())
            .putFloat("home_lng", lng.toFloat())
            .putString("home_label", label)
            .apply()
    }

    fun homeLocation(): Pair<Double, Double>? {
        if (!prefs.contains("home_lat")) return null
        return prefs.getFloat("home_lat", 0f).toDouble() to prefs.getFloat("home_lng", 0f).toDouble()
    }

    fun homeLabel(): String = prefs.getString("home_label", "家") ?: "家"

    fun setPendingFollowUp(prompt: SmartItineraryFollowUpPrompt) {
        prefs.edit()
            .putString("pending_followup", followUpToJson(prompt).toString())
            .apply()
    }

    fun consumePendingFollowUp(): SmartItineraryFollowUpPrompt? {
        val raw = prefs.getString("pending_followup", null) ?: return null
        prefs.edit().remove("pending_followup").apply()
        return runCatching {
            followUpFromJson(JSONObject(raw))
        }.getOrNull()
    }

    private fun tripKey(memoId: Long) = "trip_$memoId"

    private fun tripToJson(trip: SmartItineraryTrip): JSONObject = JSONObject()
        .put("memoId", trip.memoId)
        .put("destination", trip.destination)
        .put("finalRemindAtMillis", trip.finalRemindAtMillis)
        .put("timeText", trip.timeText)
        .put("homeLat", trip.homeLat ?: JSONObject.NULL)
        .put("homeLng", trip.homeLng ?: JSONObject.NULL)
        .put("createdAtMillis", trip.createdAtMillis)

    private fun tripFromJson(json: JSONObject): SmartItineraryTrip {
        val homeLat = json.opt("homeLat")
        val homeLng = json.opt("homeLng")
        return SmartItineraryTrip(
            memoId = json.getLong("memoId"),
            destination = json.getString("destination"),
            finalRemindAtMillis = json.getLong("finalRemindAtMillis"),
            timeText = json.getString("timeText"),
            homeLat = if (homeLat == JSONObject.NULL) null else json.optDouble("homeLat"),
            homeLng = if (homeLng == JSONObject.NULL) null else json.optDouble("homeLng"),
            createdAtMillis = json.optLong("createdAtMillis")
        )
    }

    private fun followUpToJson(prompt: SmartItineraryFollowUpPrompt): JSONObject = JSONObject()
        .put("memoId", prompt.memoId)
        .put("type", prompt.type.name)
        .put("message", prompt.message)
        .put("suggestedRemindAtMillis", prompt.suggestedRemindAtMillis ?: JSONObject.NULL)
        .put("suggestedRemindText", prompt.suggestedRemindText ?: JSONObject.NULL)

    private fun followUpFromJson(json: JSONObject): SmartItineraryFollowUpPrompt {
        val suggested = json.opt("suggestedRemindAtMillis")
        val suggestedText = json.opt("suggestedRemindText")
        return SmartItineraryFollowUpPrompt(
            memoId = json.getLong("memoId"),
            type = SmartItineraryFollowUpType.valueOf(json.getString("type")),
            message = json.getString("message"),
            suggestedRemindAtMillis = if (suggested == JSONObject.NULL) null else json.optLong("suggestedRemindAtMillis"),
            suggestedRemindText = if (suggestedText == JSONObject.NULL) null else json.optString("suggestedRemindText")
        )
    }
}
