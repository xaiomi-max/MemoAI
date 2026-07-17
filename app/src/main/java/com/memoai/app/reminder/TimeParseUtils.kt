package com.memoai.app.reminder

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

data class ParsedReminder(
    val remindAtMillis: Long,
    val timeText: String
)

object TimeParseUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }

    fun currentIsoTime(): String = isoFormat.format(Date())

    fun parseIsoTime(timeStr: String): Long? {
        return runCatching { isoFormat.parse(timeStr.trim())?.time }.getOrNull()
    }

    fun parseFromNaturalLanguage(text: String, nowMillis: Long = System.currentTimeMillis()): ParsedReminder? {
        val raw = text.trim()
        if (raw.isEmpty()) return null

        Regex("""(\d+)\s*分钟后""").find(raw)?.let { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: return@let null
            val millis = nowMillis + minutes * 60_000L
            return ParsedReminder(millis, match.value)
        }

        Regex("""(\d+)\s*小时后""").find(raw)?.let { match ->
            val hours = match.groupValues[1].toLongOrNull() ?: return@let null
            return ParsedReminder(nowMillis + hours * 3_600_000L, match.value)
        }

        val timePattern = Pattern.compile(
            """(?:(今天|明天|后天|大后天))?\s*(?:周([一二三四五六日天])|星期([一二三四五六日天]))?\s*(?:上午|早上|中午|下午|晚上)?\s*(\d{1,2})\s*(?:[:：点时]\s*(\d{1,2}))?"""
        )
        val matcher = timePattern.matcher(raw)
        if (!matcher.find()) return null

        val dayWord = matcher.group(1).orEmpty()
        val weekDayWord = matcher.group(2) ?: matcher.group(3).orEmpty()
        val hour = matcher.group(4)?.toIntOrNull() ?: return null
        val minute = matcher.group(5)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply {
            timeInMillis = nowMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (dayWord) {
            "明天" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "后天" -> calendar.add(Calendar.DAY_OF_YEAR, 2)
            "大后天" -> calendar.add(Calendar.DAY_OF_YEAR, 3)
        }

        if (weekDayWord.isNotEmpty()) {
            val targetDay = weekDayToCalendar(weekDayWord)
            calendar.set(Calendar.DAY_OF_WEEK, targetDay)
            if (calendar.timeInMillis <= nowMillis) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        var adjustedHour = hour
        when {
            raw.contains("下午") || raw.contains("晚上") -> {
                if (hour in 1..11) adjustedHour = hour + 12
            }
            raw.contains("中午") -> {
                if (hour in 1..10) adjustedHour = hour + 12
            }
        }

        calendar.set(Calendar.HOUR_OF_DAY, adjustedHour.coerceIn(0, 23))
        calendar.set(Calendar.MINUTE, minute.coerceIn(0, 59))

        if (dayWord.isEmpty() && weekDayWord.isEmpty() && calendar.timeInMillis <= nowMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val timeText = matcher.group().trim().ifBlank { raw }
        return ParsedReminder(calendar.timeInMillis, timeText)
    }

    private fun weekDayToCalendar(word: String): Int {
        return when (word) {
            "一" -> Calendar.MONDAY
            "二" -> Calendar.TUESDAY
            "三" -> Calendar.WEDNESDAY
            "四" -> Calendar.THURSDAY
            "五" -> Calendar.FRIDAY
            "六" -> Calendar.SATURDAY
            "日", "天" -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }
    }
}
