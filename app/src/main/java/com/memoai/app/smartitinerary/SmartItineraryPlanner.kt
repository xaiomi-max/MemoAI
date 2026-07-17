package com.memoai.app.smartitinerary

import com.memoai.app.reminder.TimeParseUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

object SmartItineraryPlanner {
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.CHINA).apply {
        timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }

    fun extractDestination(text: String): String {
        val patterns = listOf(
            Regex("""去(.{1,12}?)(?:[，,。]|$|提醒|出发)"""),
            Regex("""到(.{1,12}?)(?:[，,。]|$|提醒|出发)"""),
            Regex("""前往(.{1,12}?)(?:[，,。]|$|提醒|出发)""")
        )
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val dest = match.groupValues[1].trim()
            if (dest.isNotBlank()) return dest
        }
        return when {
            text.contains("机场") -> "机场"
            text.contains("火车站") || text.contains("高铁站") -> "火车站"
            text.contains("医院") -> "医院"
            else -> "目的地"
        }
    }

    fun parseArrivalMillis(text: String, nowMillis: Long = System.currentTimeMillis()): Long? {
        val arrivalHints = listOf("到", "去", "抵达", "到达")
        if (arrivalHints.none { text.contains(it) }) return null

        val timePattern = Pattern.compile(
            """(?:(今天|明天|后天|大后天))?\s*(?:周([一二三四五六日天])|星期([一二三四五六日天]))?\s*(?:上午|早上|中午|下午|晚上)?\s*(\d{1,2})\s*(?:[:：点时]\s*(\d{1,2}))?(?=\s*(?:去|到|抵达|到达))"""
        )
        val matcher = timePattern.matcher(text)
        if (!matcher.find()) {
            val allTimes = Regex("""(\d{1,2})\s*(?:[:：点时]\s*(\d{1,2}))?""").findAll(text).toList()
            if (allTimes.isNotEmpty()) {
                return buildMillisFromMatch(text, allTimes.first().value, nowMillis)
            }
            return null
        }
        return buildMillisFromMatcher(text, matcher, nowMillis)
    }

    fun suggestDepartAtMillis(
        userInput: String,
        remindAtMillis: Long,
        arrivalAtMillis: Long?
    ): Long {
        val destination = extractDestination(userInput)
        val bufferMinutes = when {
            destination.contains("机场") -> 50L
            destination.contains("火车") || destination.contains("高铁") -> 40L
            destination.contains("医院") -> 20L
            else -> 30L
        }
        if (arrivalAtMillis != null && arrivalAtMillis > remindAtMillis) {
            val fromArrival = arrivalAtMillis - bufferMinutes * 60_000L
            if (fromArrival < remindAtMillis) return fromArrival
        }
        return (remindAtMillis - bufferMinutes * 60_000L).coerceAtLeast(System.currentTimeMillis() + 60_000L)
    }

    fun formatTime(millis: Long): String = timeFormatter.format(millis)

    fun buildInitialSuggestion(
        userInput: String,
        summary: String,
        content: String,
        remindAtMillis: Long,
        timeText: String,
        cloudProcessed: Boolean
    ): SmartItinerarySetupPrompt {
        val destination = extractDestination(userInput)
        val arrivalAtMillis = parseArrivalMillis(userInput, System.currentTimeMillis())
        val suggestedDepartAtMillis = suggestDepartAtMillis(userInput, remindAtMillis, arrivalAtMillis)
        return SmartItinerarySetupPrompt(
            userInput = userInput,
            summary = summary,
            content = content,
            remindAtMillis = remindAtMillis,
            timeText = timeText,
            destination = destination,
            suggestedDepartAtMillis = suggestedDepartAtMillis,
            suggestedDepartText = formatTime(suggestedDepartAtMillis),
            cloudProcessed = cloudProcessed
        )
    }

    fun recalculateAfterLocationChange(
        currentRemindAtMillis: Long,
        destination: String
    ): Long {
        val extra = if (destination.contains("机场")) 20L else 15L
        return currentRemindAtMillis - extra * 60_000L
    }

    fun recalculateForTraffic(currentRemindAtMillis: Long): Long {
        return currentRemindAtMillis + 30 * 60_000L
    }

    private fun buildMillisFromMatch(text: String, matchText: String, nowMillis: Long): Long? {
        val matcher = Pattern.compile(
            """(?:(今天|明天|后天|大后天))?\s*(?:周([一二三四五六日天])|星期([一二三四五六日天]))?\s*(?:上午|早上|中午|下午|晚上)?\s*(\d{1,2})\s*(?:[:：点时]\s*(\d{1,2}))?"""
        ).matcher("$matchText $text")
        if (!matcher.find()) {
            return TimeParseUtils.parseFromNaturalLanguage(text, nowMillis)?.remindAtMillis
        }
        return buildMillisFromMatcher(text, matcher, nowMillis)
    }

    private fun buildMillisFromMatcher(
        raw: String,
        matcher: java.util.regex.Matcher,
        nowMillis: Long
    ): Long? {
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
            calendar.set(Calendar.DAY_OF_WEEK, weekDayToCalendar(weekDayWord))
            if (calendar.timeInMillis <= nowMillis) calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
        var adjustedHour = hour
        when {
            raw.contains("下午") || raw.contains("晚上") -> if (hour in 1..11) adjustedHour = hour + 12
            raw.contains("中午") -> if (hour in 1..10) adjustedHour = hour + 12
        }
        calendar.set(Calendar.HOUR_OF_DAY, adjustedHour.coerceIn(0, 23))
        calendar.set(Calendar.MINUTE, minute.coerceIn(0, 59))
        if (dayWord.isEmpty() && weekDayWord.isEmpty() && calendar.timeInMillis <= nowMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    private fun weekDayToCalendar(word: String): Int = when (word) {
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
