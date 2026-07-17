package com.memoai.app.reminder

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.memoai.app.oem.OemAutostartRouter
import com.memoai.app.oem.OemDistributionConfig

object ReminderPermissionHelper {
    private const val OEM_PROMPTED = "oem_setup_prompted"

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun shouldPromptOemSetup(context: Context, prefs: SharedPreferences): Boolean {
        if (prefs.getBoolean(OEM_PROMPTED, false)) return false
        return OemAutostartRouter.shouldPromptOnDevice()
    }

    fun markOemSetupPrompted(prefs: SharedPreferences) {
        prefs.edit().putBoolean(OEM_PROMPTED, true).apply()
    }

    fun openOemBackgroundSettings(context: Context): Boolean {
        val opened = startFirstAvailable(context, OemAutostartRouter.autostartIntents(context))
        if (!opened) {
            Toast.makeText(context, oemAutostartFailMessage(), Toast.LENGTH_LONG).show()
        }
        return opened
    }

    fun requestIgnoreBatteryOptimizations(context: Context): Boolean {
        val opened = startFirstAvailable(context, OemAutostartRouter.batteryIntents(context))
        if (!opened) {
            Toast.makeText(context, "无法打开省电设置，请在系统设置中手动关闭省电限制", Toast.LENGTH_LONG).show()
        }
        return opened
    }

    fun openNotificationSettings(context: Context): Boolean {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            OemAutostartRouter.appDetailsIntent(context)
        }
        return startFirstAvailable(
            context,
            listOf(intent, OemAutostartRouter.appDetailsIntent(context))
        )
    }

    private fun oemAutostartFailMessage(): String {
        val name = OemDistributionConfig.current.displayName
        return "无法打开${name}自启动设置，请在「设置 → 应用 → Memo → 权限」中手动开启"
    }

    private fun startFirstAvailable(context: Context, intents: List<Intent>): Boolean {
        for (raw in intents) {
            val intent = Intent(raw)
            applyLaunchFlags(context, intent)
            if (!canLaunch(context, intent)) continue
            try {
                context.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
                continue
            } catch (_: SecurityException) {
                continue
            } catch (_: Exception) {
                continue
            }
        }
        return false
    }

    private fun canLaunch(context: Context, intent: Intent): Boolean {
        val component = intent.component
        if (component != null) {
            return try {
                context.packageManager.getActivityInfo(component, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
        return intent.resolveActivity(context.packageManager) != null
    }

    private fun applyLaunchFlags(context: Context, intent: Intent) {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
