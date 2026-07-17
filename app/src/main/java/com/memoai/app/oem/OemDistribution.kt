package com.memoai.app.oem

/**
 * 应用商店 / OEM 分发渠道。
 * 当前默认 vivo；后续可为不同商店增加 productFlavor 并切换 [BuildConfig.DISTRIBUTION_CHANNEL]。
 */
enum class OemDistribution(
    val channelId: String,
    val displayName: String
) {
    VIVO("vivo", "vivo"),
    XIAOMI("xiaomi", "小米"),
    HUAWEI("huawei", "华为"),
    OPPO("oppo", "OPPO"),
    GENERIC("generic", "Android");

    companion object {
        fun fromChannelId(value: String): OemDistribution =
            entries.firstOrNull { it.channelId.equals(value, ignoreCase = true) } ?: VIVO
    }
}
