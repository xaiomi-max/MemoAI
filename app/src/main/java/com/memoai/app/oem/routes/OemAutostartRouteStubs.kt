package com.memoai.app.oem.routes

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.memoai.app.oem.OemAutostartRoute
import com.memoai.app.oem.OemDistribution

/**
 * 小米应用商店版本（占位，后续启用 productFlavor xiaomi 时使用）。
 *
 * 启用步骤：
 * 1. build.gradle.kts 增加 flavor `xiaomi`，DISTRIBUTION_CHANNEL = "xiaomi"
 * 2. 在 [com.memoai.app.oem.OemAutostartRouter] 的 routes 列表中取消注释 XiaomiAutostartRoute
 */
@Suppress("unused")
object XiaomiAutostartRoute : OemAutostartRoute {
    override val distribution: OemDistribution = OemDistribution.XIAOMI

    override fun matchesDevice(): Boolean {
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        val brand = android.os.Build.BRAND.lowercase()
        return manufacturer.contains("xiaomi") || brand.contains("redmi")
    }

    override fun autostartIntents(context: Context): List<Intent> = listOf(
        Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        )
    )
}

/**
 * 华为应用商店版本（占位）。
 */
@Suppress("unused")
object HuaweiAutostartRoute : OemAutostartRoute {
    override val distribution: OemDistribution = OemDistribution.HUAWEI

    override fun matchesDevice(): Boolean {
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        val brand = android.os.Build.BRAND.lowercase()
        return manufacturer.contains("huawei") || brand.contains("honor")
    }

    override fun autostartIntents(context: Context): List<Intent> = listOf(
        Intent().setComponent(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        )
    )
}

/**
 * OPPO / 一加应用商店版本（占位）。
 */
@Suppress("unused")
object OppoAutostartRoute : OemAutostartRoute {
    override val distribution: OemDistribution = OemDistribution.OPPO

    override fun matchesDevice(): Boolean {
        val brand = android.os.Build.BRAND.lowercase()
        return brand.contains("oppo") || brand.contains("oneplus") || brand.contains("realme")
    }

    override fun autostartIntents(context: Context): List<Intent> = listOf(
        Intent().setComponent(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        )
    )
}
