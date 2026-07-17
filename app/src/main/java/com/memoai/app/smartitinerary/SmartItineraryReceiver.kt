package com.memoai.app.smartitinerary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.memoai.app.reminder.ReminderScheduler
import java.util.Calendar
import java.util.TimeZone

class SmartItineraryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.createNotificationChannel(context)
        val memoId = intent.getLongExtra(EXTRA_MEMO_ID, 0L)
        if (memoId <= 0L) return

        val store = SmartItineraryStore(context)
        val trip = store.getTrip(memoId) ?: return

        when (intent.action) {
            SmartItineraryScheduler.ACTION_LOCATION_CHECK -> handleLocationCheck(context, store, trip)
            SmartItineraryScheduler.ACTION_TRAFFIC_CHECK -> handleTrafficCheck(context, store, trip)
        }
    }

    private fun handleLocationCheck(
        context: Context,
        store: SmartItineraryStore,
        trip: SmartItineraryTrip
    ) {
        val moved = LocationHelper.hasMovedFromHome(context, trip.homeLat, trip.homeLng)
        if (!moved) return

        val suggested = SmartItineraryPlanner.recalculateAfterLocationChange(
            trip.finalRemindAtMillis,
            trip.destination
        )
        val prompt = SmartItineraryFollowUpPrompt(
            memoId = trip.memoId,
            type = SmartItineraryFollowUpType.LOCATION_CHANGED,
            message = "位置变了（当前不在${store.homeLabel()}），要重新算提醒吗？",
            suggestedRemindAtMillis = suggested,
            suggestedRemindText = SmartItineraryPlanner.formatTime(suggested)
        )
        store.setPendingFollowUp(prompt)
        postPromptNotification(context, trip.memoId, prompt.message)
    }

    private fun handleTrafficCheck(
        context: Context,
        store: SmartItineraryStore,
        trip: SmartItineraryTrip
    ) {
        val hour = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).get(Calendar.HOUR_OF_DAY)
        val congested = hour in 7..9
        if (!congested) return

        val suggested = SmartItineraryPlanner.recalculateForTraffic(trip.finalRemindAtMillis)
        val prompt = SmartItineraryFollowUpPrompt(
            memoId = trip.memoId,
            type = SmartItineraryFollowUpType.TRAFFIC_CONGESTION,
            message = "⚠️ 路况拥堵，建议 ${SmartItineraryPlanner.formatTime(suggested)} 出发，要改吗？",
            suggestedRemindAtMillis = suggested,
            suggestedRemindText = SmartItineraryPlanner.formatTime(suggested)
        )
        store.setPendingFollowUp(prompt)
        postPromptNotification(context, trip.memoId, prompt.message)
    }

    private fun postPromptNotification(context: Context, memoId: Long, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("智能行程提醒")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context)
            .notify((memoId + 10_000L).and(0x7FFFFFFFL).toInt(), notification)
    }

    companion object {
        const val EXTRA_MEMO_ID = "memo_id"
    }
}
