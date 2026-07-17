package com.memoai.app.oem.routes

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import com.memoai.app.oem.OemAutostartRoute
import com.memoai.app.oem.OemDistribution

/** vivo / iQOO 应用商店版本 — 自启动与后台权限跳转。 */
object VivoAutostartRoute : OemAutostartRoute {
    override val distribution: OemDistribution = OemDistribution.VIVO

    override fun matchesDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("vivo") ||
            brand.contains("vivo") ||
            brand.contains("iqoo")
    }

    override fun autostartIntents(context: android.content.Context): List<Intent> {
        val pkg = context.packageName
        return listOf(
            component(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            ),
            component(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
            ),
            component(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.SoftPermissionDetailActivity"
            ).apply { putExtra("packagename", pkg) },
            component(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
            ),
            // OriginOS 4+ 部分机型
            component(
                "com.vivo.applicationbehaviorengine",
                "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"
            )
        )
    }

    override fun batteryIntents(context: android.content.Context): List<Intent> {
        val pkg = context.packageName
        return listOf(
            component(
                "com.iqoo.powersaving",
                "com.iqoo.powersaving.PowerSavingManagerActivity"
            ),
            component(
                "com.vivo.abe",
                "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"
            ).apply { putExtra("packagename", pkg) }
        )
    }

    private fun component(packageName: String, className: String): Intent {
        return Intent().setComponent(ComponentName(packageName, className))
    }
}
