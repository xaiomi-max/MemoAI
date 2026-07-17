package com.memoai.app.oem

import android.content.Context
import android.content.Intent

/**
 * 各分发渠道的 OEM 权限跳转路由。
 * 新增商店版本时：实现此接口并在 [OemAutostartRouter] 中注册即可。
 */
interface OemAutostartRoute {
    val distribution: OemDistribution

    /** 当前设备是否属于该渠道的目标机型（用于决定是否弹出引导）。 */
    fun matchesDevice(): Boolean

    /** 自启动 / 后台活动设置页，按优先级排列。 */
    fun autostartIntents(context: Context): List<Intent>

    /** 省电 / 后台白名单设置页（可选，空列表则走系统默认）。 */
    fun batteryIntents(context: Context): List<Intent> = emptyList()
}
