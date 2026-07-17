package com.memoai.app.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.memoai.app.data.MemoDatabase
import com.memoai.app.reminder.ReminderScheduler
import com.memoai.app.ui.MemoType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodoCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_COMPLETE_TASK) return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("memo_ai_prefs", Context.MODE_PRIVATE)
                val dao = MemoDatabase.get(context).memoDao()
                val entity = dao.getById(taskId) ?: return@launch
                if (entity.type != MemoType.Tasks.name || entity.completed) return@launch

                dao.setCompleted(taskId, true, System.currentTimeMillis())
                ReminderScheduler.cancel(context, taskId)

                if (LockScreenTodoManager.isEnabled(prefs)) {
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

    companion object {
        const val ACTION_COMPLETE_TASK = "com.memoai.app.action.COMPLETE_TASK"
        const val EXTRA_TASK_ID = "task_id"
    }
}
