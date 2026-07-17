package com.memoai.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.memoai.app.data.MemoDatabase
import com.memoai.app.lockscreen.LockScreenTodoManager
import com.memoai.app.ui.MemoType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = MemoDatabase.get(context).memoDao()
                val now = System.currentTimeMillis()
                dao.futureReminders(now).forEach { memo ->
                    if (memo.type != MemoType.Tasks.name) return@forEach
                    val remindAt = memo.remindAtMillis ?: return@forEach
                    ReminderScheduler.schedule(
                        context = context,
                        memoId = memo.id,
                        content = memo.summary.ifBlank { memo.userInput },
                        timeText = memo.timeText.orEmpty(),
                        remindAtMillis = remindAt
                    )
                }
                LockScreenTodoManager.createNotificationChannel(context)
                if (LockScreenTodoManager.isEnabled(
                        context.getSharedPreferences("memo_ai_prefs", Context.MODE_PRIVATE)
                    )
                ) {
                    val tasks = dao.observeIncompleteTasks(MemoType.Tasks.name, 7)
                        .map {
                            LockScreenTodoManager.TodoItem(
                                id = it.id,
                                title = it.summary.ifBlank { it.userInput },
                                timeText = it.timeText
                            )
                        }
                    LockScreenTodoManager.sync(context, tasks)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
