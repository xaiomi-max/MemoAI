package com.memoai.app.lockscreen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.memoai.app.MainActivity
import com.memoai.app.R
import org.json.JSONArray
import org.json.JSONObject

object LockScreenTodoManager {
    const val CHANNEL_ID = "lock_screen_todo"
    const val CHANNEL_NAME = "锁屏待办"
    const val NOTIFICATION_ID = 9001
    const val EXTRA_OPEN_TASKS = "open_tasks"
    const val EXTRA_TASKS_PAYLOAD = "tasks_payload"
    const val PREF_ENABLED = "lock_screen_todo_enabled"

    data class TodoItem(
        val id: Long,
        val title: String,
        val timeText: String?
    )

    fun isEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(PREF_ENABLED, true)

    fun setEnabled(context: Context, prefs: SharedPreferences, enabled: Boolean) {
        prefs.edit().putBoolean(PREF_ENABLED, enabled).apply()
        if (!enabled) {
            dismiss(context)
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "在锁屏显示待办清单，点击后打开 Tasks"
            setShowBadge(true)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun sync(context: Context, tasks: List<TodoItem>) {
        createNotificationChannel(context)
        TodoLockScreenWidget.updateAll(context)

        if (tasks.isEmpty()) {
            dismiss(context)
            return
        }

        val intent = Intent(context, TodoForegroundService::class.java).apply {
            action = TodoForegroundService.ACTION_UPDATE
            putExtra(EXTRA_TASKS_PAYLOAD, encodeTasks(tasks))
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun dismiss(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        val stopIntent = Intent(context, TodoForegroundService::class.java).apply {
            action = TodoForegroundService.ACTION_STOP
        }
        context.startService(stopIntent)
    }

    fun buildNotification(context: Context, tasks: List<TodoItem>): Notification {
        val pendingIntent = openTasksPendingIntent(context)
        val count = tasks.size
        val summary = if (count == 1) "1 项待办" else "$count 项待办"

        val viewsCollapsed = buildRemoteViews(context, tasks, expanded = false)
        val viewsExpanded = buildRemoteViews(context, tasks, expanded = true)

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("待办事项")
            .setSummaryText(summary)
        tasks.take(6).forEach { item ->
            inboxStyle.addLine("○ ${buildLine(item)}")
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_todo_notification)
            .setContentTitle("待办事项")
            .setContentText(summary)
            .setStyle(inboxStyle)
            .setCustomContentView(viewsCollapsed)
            .setCustomBigContentView(viewsExpanded)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setLocalOnly(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun encodeTasks(tasks: List<TodoItem>): String {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("timeText", task.timeText.orEmpty())
                }
            )
        }
        return array.toString()
    }

    fun decodeTasks(payload: String?): List<TodoItem> {
        if (payload.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(payload)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        TodoItem(
                            id = obj.getLong("id"),
                            title = obj.getString("title"),
                            timeText = obj.optString("timeText").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun openTasksPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_TASKS, true)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun completePendingIntent(context: Context, taskId: Long): PendingIntent {
        val intent = Intent(context, TodoCompleteReceiver::class.java).apply {
            action = TodoCompleteReceiver.ACTION_COMPLETE_TASK
            putExtra(TodoCompleteReceiver.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildRemoteViews(
        context: Context,
        tasks: List<TodoItem>,
        expanded: Boolean
    ): RemoteViews {
        val layoutId = if (expanded) {
            R.layout.notification_lock_screen_todos_expanded
        } else {
            R.layout.notification_lock_screen_todos_collapsed
        }
        val views = RemoteViews(context.packageName, layoutId)
        val limit = if (expanded) 6 else 3
        val visible = tasks.take(limit)

        views.setTextViewText(R.id.todo_title, "待办事项")
        views.setTextViewText(
            R.id.todo_subtitle,
            if (tasks.size == 1) "1 项待完成" else "${tasks.size} 项待完成"
        )

        val rowIds = listOf(
            Triple(R.id.todo_row_1, R.id.todo_text_1, R.id.todo_check_1),
            Triple(R.id.todo_row_2, R.id.todo_text_2, R.id.todo_check_2),
            Triple(R.id.todo_row_3, R.id.todo_text_3, R.id.todo_check_3),
            Triple(R.id.todo_row_4, R.id.todo_text_4, R.id.todo_check_4),
            Triple(R.id.todo_row_5, R.id.todo_text_5, R.id.todo_check_5),
            Triple(R.id.todo_row_6, R.id.todo_text_6, R.id.todo_check_6)
        )

        rowIds.forEachIndexed { index, (rowId, textId, checkId) ->
            if (index < visible.size) {
                val item = visible[index]
                views.setViewVisibility(rowId, android.view.View.VISIBLE)
                views.setTextViewText(textId, buildLine(item))
                views.setOnClickPendingIntent(checkId, completePendingIntent(context, item.id))
            } else {
                views.setViewVisibility(rowId, android.view.View.GONE)
            }
        }

        if (expanded && tasks.size > limit) {
            views.setViewVisibility(R.id.todo_more, android.view.View.VISIBLE)
            views.setTextViewText(R.id.todo_more, "还有 ${tasks.size - limit} 项…")
        } else {
            views.setViewVisibility(R.id.todo_more, android.view.View.GONE)
        }

        return views
    }

    private fun buildLine(item: TodoItem): String {
        val title = item.title.trim().ifBlank { "未命名待办" }
        val time = item.timeText?.takeIf { it.isNotBlank() }
        return if (time != null) "$title  ·  $time" else title
    }
}
