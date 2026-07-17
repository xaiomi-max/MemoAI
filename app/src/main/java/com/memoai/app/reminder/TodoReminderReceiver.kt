package com.memoai.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class TodoReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.createNotificationChannel(context)

        val content = intent.getStringExtra(EXTRA_CONTENT).orEmpty().ifBlank { "有新的待办事项" }
        val timeText = intent.getStringExtra(EXTRA_TIME_TEXT).orEmpty()
        val memoId = intent.getLongExtra(EXTRA_MEMO_ID, 0L)

        val title = if (timeText.isNotEmpty()) "⏰ $timeText 到了" else "⏰ 待办提醒"
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(context)
            .notify(memoId.and(0x7FFFFFFF).toInt(), notification)
    }

    companion object {
        const val EXTRA_MEMO_ID = "memo_id"
        const val EXTRA_CONTENT = "content"
        const val EXTRA_TIME_TEXT = "time_text"
    }
}
