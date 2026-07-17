package com.memoai.app.lockscreen

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.memoai.app.R
import com.memoai.app.data.MemoDatabase
import com.memoai.app.data.MemoEntity
import com.memoai.app.ui.MemoType
import kotlinx.coroutines.runBlocking

class TodoLockScreenWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        Thread {
            try {
                refreshWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, TodoLockScreenWidget::class.java)
            )
            if (ids.isEmpty()) return
            refreshWidgets(context, manager, ids)
        }

        private fun refreshWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val tasks = runBlocking {
                MemoDatabase.get(context).memoDao()
                    .observeIncompleteTasks(MemoType.Tasks.name, 6)
            }
            appWidgetIds.forEach { widgetId ->
                appWidgetManager.updateAppWidget(widgetId, buildWidgetViews(context, tasks))
            }
        }

        private fun buildWidgetViews(context: Context, tasks: List<MemoEntity>): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_lock_screen_todos)
            views.setOnClickPendingIntent(
                R.id.widget_root,
                LockScreenTodoManager.openTasksPendingIntent(context)
            )

            views.setTextViewText(
                R.id.widget_title,
                if (tasks.isEmpty()) "待办事项" else "待办事项 · ${tasks.size}"
            )

            val rowIds = listOf(
                R.id.widget_row_1 to R.id.widget_text_1,
                R.id.widget_row_2 to R.id.widget_text_2,
                R.id.widget_row_3 to R.id.widget_text_3,
                R.id.widget_row_4 to R.id.widget_text_4
            )

            if (tasks.isEmpty()) {
                views.setViewVisibility(R.id.widget_empty, android.view.View.VISIBLE)
                rowIds.forEach { (rowId, _) ->
                    views.setViewVisibility(rowId, android.view.View.GONE)
                }
            } else {
                views.setViewVisibility(R.id.widget_empty, android.view.View.GONE)
                rowIds.forEachIndexed { index, (rowId, textId) ->
                    if (index < tasks.size) {
                        val memo = tasks[index]
                        val title = memo.summary.ifBlank { memo.userInput }
                        views.setViewVisibility(rowId, android.view.View.VISIBLE)
                        views.setTextViewText(textId, title)
                    } else {
                        views.setViewVisibility(rowId, android.view.View.GONE)
                    }
                }
            }

            return views
        }
    }
}
