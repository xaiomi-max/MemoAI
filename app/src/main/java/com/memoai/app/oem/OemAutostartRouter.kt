package com.memoai.app.oem

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.memoai.app.oem.routes.VivoAutostartRoute
// import com.memoai.app.oem.routes.XiaomiAutostartRoute
// import com.memoai.app.oem.routes.HuaweiAutostartRoute
// import com.memoai.app.oem.routes.OppoAutostartRoute

object OemAutostartRouter {

    /**
     * 已注册的路由表。新增商店版本时在此追加对应 Route 对象。
     * 当前仅启用 vivo；其它渠道见 routes/OemAutostartRouteStubs.kt。
     */
    private val routes: List<OemAutostartRoute> = listOf(
        VivoAutostartRoute,
        // XiaomiAutostartRoute,
        // HuaweiAutostartRoute,
        // OppoAutostartRoute,
    )

    private fun routeForCurrentBuild(): OemAutostartRoute {
        val channel = OemDistributionConfig.current
        return routes.firstOrNull { it.distribution == channel }
            ?: VivoAutostartRoute
    }

    fun shouldPromptOnDevice(): Boolean = routeForCurrentBuild().matchesDevice()

    fun autostartIntents(context: Context): List<Intent> {
        return routeForCurrentBuild().autostartIntents(context) + appDetailsIntent(context)
    }

    fun batteryIntents(context: Context): List<Intent> {
        val route = routeForCurrentBuild()
        val oemIntents = route.batteryIntents(context)
        return if (oemIntents.isNotEmpty()) {
            oemIntents + appDetailsIntent(context)
        } else {
            listOf(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                },
                appDetailsIntent(context)
            )
        }
    }

    fun appDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
}
