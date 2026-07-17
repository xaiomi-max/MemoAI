package com.memoai.app.oem

import com.memoai.app.BuildConfig

object OemDistributionConfig {
    /** 当前 APK 面向的分发渠道，由 Gradle BuildConfig 注入。 */
    val current: OemDistribution = OemDistribution.fromChannelId(BuildConfig.DISTRIBUTION_CHANNEL)
}
