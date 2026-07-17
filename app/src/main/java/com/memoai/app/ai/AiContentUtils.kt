package com.memoai.app.ai

object AiContentUtils {
    fun buildLocalSummary(rawText: String): String {
        val raw = rawText.trim().lineSequence().first().trim()
        if (raw.isBlank()) return ""
        val withoutTime = raw.replace(
            Regex("""(今天|明天|后天|大后天|下周|本周|这周|周[一二三四五六日天]|星期[一二三四五六日天]|\d{1,2}[:：点时]\d{0,2}|\d+\s*分钟后|\d+\s*小时后|下午|上午|早上|晚上|中午).*"""),
            ""
        ).trim()
        val base = withoutTime.ifBlank { raw }
        return base
    }
}
