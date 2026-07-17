package com.memoai.app.smartitinerary

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar
import java.util.TimeZone

object SmartItineraryScheduler {
    const val ACTION_LOCATION_CHECK = "com.memoai.app.action.SMART_ITINERARY_LOCATION"
    const val ACTION_TRAFFIC_CHECK = "com.memoai.app.action.SMART_ITINERARY_TRAFFIC"

    fun scheduleFollowUps(context: Context, trip: SmartItineraryTrip) {
        val finalCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply {
            timeInMillis = trip.finalRemindAtMillis
        }
        val locationAt = (finalCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, -1)
            }
        }.timeInMillis

        val trafficAt = (finalCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, -1)
            }
        }.timeInMillis

        scheduleAction(context, trip.memoId, ACTION_LOCATION_CHECK, locationAt)
        scheduleAction(context, trip.memoId, ACTION_TRAFFIC_CHECK, trafficAt)
    }

    fun cancelFollowUps(context: Context, memoId: Long) {
        cancelAction(context, memoId, ACTION_LOCATION_CHECK)
        cancelAction(context, memoId, ACTION_TRAFFIC_CHECK)
    }

    private fun scheduleAction(context: Context, memoId: Long, action: String, atMillis: Long) {
        if (atMillis <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = (memoId.toString() + action).hashCode()
        val intent = Intent(context, SmartItineraryReceiver::class.java).apply {
            this.action = action
            putExtra(SmartItineraryReceiver.EXTRA_MEMO_ID, memoId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        }
    }

    private fun cancelAction(context: Context, memoId: Long, action: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = (memoId.toString() + action).hashCode()
        val intent = Intent(context, SmartItineraryReceiver::class.java).apply { this.action = action }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
