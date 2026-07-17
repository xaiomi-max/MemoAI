package com.memoai.app.lockscreen

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat

class TodoForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                val tasks = LockScreenTodoManager.decodeTasks(
                    intent.getStringExtra(LockScreenTodoManager.EXTRA_TASKS_PAYLOAD)
                )
                if (tasks.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }
                LockScreenTodoManager.createNotificationChannel(this)
                val notification = LockScreenTodoManager.buildNotification(this, tasks)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        LockScreenTodoManager.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(LockScreenTodoManager.NOTIFICATION_ID, notification)
                }
                return START_STICKY
            }
        }
        stopSelf()
        return START_NOT_STICKY
    }

    companion object {
        const val ACTION_UPDATE = "com.memoai.app.lockscreen.ACTION_UPDATE_TODO"
        const val ACTION_STOP = "com.memoai.app.lockscreen.ACTION_STOP_TODO"
    }
}
