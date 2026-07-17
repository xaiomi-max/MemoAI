package com.memoai.app.ai

import com.memoai.app.reminder.TimeParseUtils
import com.memoai.app.ui.MemoType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AiResult(
    val type: MemoType,
    val summary: String,
    val content: String,
    val remindAtMillis: Long? = null,
    val timeText: String? = null
)

interface AiEngine {
    suspend fun process(rawText: String, onStreamToken: (String) -> Unit = {}): AiResult
}

suspend fun isTravelRelatedTaskByAi(
    apiKey: String,
    model: String,
    text: String
): Boolean = withContext(Dispatchers.IO) {
    if (apiKey.isBlank() || text.isBlank()) return@withContext false
    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val instruction = """
        你是任务分类器。请判断输入是否是“出行/到达/地点移动”类任务。
        仅输出一个单词：yes 或 no。
        判定为 yes 的例子：去机场、到公司、赶高铁、出发去xx、打车到xx、导航到xx、明天去医院。
        判定为 no 的例子：写方案、完成作业、整理文档、今晚完成比赛构想、看书、复盘。
    """.trimIndent()

    val payload = JSONObject()
        .put("model", model.ifBlank { "deepseek-chat" })
        .put(
            "messages",
            JSONArray()
                .put(JSONObject().put("role", "system").put("content", instruction))
                .put(JSONObject().put("role", "user").put("content", text))
        )
        .put("temperature", 0)
        .put("stream", false)

    val req = Request.Builder()
        .url("https://api.deepseek.com/chat/completions")
        .addHeader("Authorization", "Bearer $apiKey")
        .addHeader("Content-Type", "application/json")
        .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

    runCatching {
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@use false
            val body = res.body?.string().orEmpty()
            val content = JSONObject(body)
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
                .trim()
                .lowercase(Locale.getDefault())
            content.startsWith("yes")
        }
    }.getOrDefault(false)
}

class LocalAiEngine : AiEngine {
    override suspend fun process(rawText: String, onStreamToken: (String) -> Unit): AiResult {
        val type = classify(rawText)
        val reminder = if (type == MemoType.Tasks) {
            TimeParseUtils.parseFromNaturalLanguage(rawText)
        } else {
            null
        }

        return AiResult(
            type = type,
            summary = AiContentUtils.buildLocalSummary(rawText),
            content = rawText.trim(),
            remindAtMillis = reminder?.remindAtMillis,
            timeText = reminder?.timeText
        )
    }

    private fun classify(text: String): MemoType {
        val raw = text.trim()
        if (raw.isBlank()) return MemoType.Ideas
        val lower = raw.lowercase(Locale.getDefault())

        val knowledgeKeywords = listOf("什么是", "为什么", "怎么", "如何", "原理", "定义", "解释", "区别", "对比", "介绍", "概念", "百科")
        val isQuestion = raw.contains("？") || raw.contains("?")
        if (knowledgeKeywords.any { lower.contains(it) } ||
            (isQuestion && listOf("吗", "如何", "怎么", "是什么", "为什么").any { lower.contains(it) })
        ) {
            return MemoType.Ideas
        }

        val brainstormKeywords = listOf("如果", "可能", "能不能", "要不要", "可不可以", "或许", "设想", "灵感")
        val taskKeywords = listOf(
            "买", "去", "完成", "提交", "安排", "提醒", "联系", "打电话", "发邮件", "缴费", "预约",
            "下载", "安装", "修复", "更新", "整理", "发送", "写", "做", "要", "需要", "记得", "别忘了",
            "待办", "取", "送", "还", "洗", "开会", "打卡", "汇报", "交付", "催", "回复", "带", "拿", "换"
        )
        val taskEnglish = listOf("task", "todo", "remind")

        val hasTimeHint = Regex("""(\d{1,2}[:点时]\d{0,2}|今天|明天|后天|周[一二三四五六日天]|星期[一二三四五六日天]|下午|晚上|早上|中午|\d+\s*分钟后|\d+\s*小时后)""")
            .containsMatchIn(raw)
        val hasRelativeTime = Regex("""(\d+\s*分钟后|\d+\s*小时后)""").containsMatchIn(raw)
        val hasTaskSignal = taskKeywords.any { lower.contains(it) } ||
            taskEnglish.any { lower.contains(it) }

        if (hasRelativeTime) return MemoType.Tasks
        if (hasTimeHint && !isQuestion) return MemoType.Tasks
        if (hasTaskSignal) {
            val isBrainstorm = brainstormKeywords.any { lower.contains(it) } && !hasTimeHint
            if (!isBrainstorm) return MemoType.Tasks
        }

        val ideaKeywords = listOf("想", "打算", "计划", "准备", "希望", "可以")
        if (ideaKeywords.any { lower.contains(it) }) return MemoType.Ideas

        return MemoType.Ideas
    }
}

class DeepSeekAiEngine(
    private val apiKey: String,
    private val model: String = "deepseek-chat"
) : AiEngine {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun process(rawText: String, onStreamToken: (String) -> Unit): AiResult = withContext(Dispatchers.IO) {
        val nowText = TimeParseUtils.currentIsoTime()
        val instruction = """
            你是一个智能助手。请将输入中文内容分类并增强，返回严格 JSON：
            {"type":"Tasks|Ideas","summary":"一句话标题","content":"仅AI增强内容","remind_at":"yyyy-MM-dd HH:mm:ss 或 null","time_text":"原始时间描述或 null"}
            
            当前时间（北京时间）：$nowText
            
            规则：
            1) summary：用一句话精炼概括用户意图，必须完整可读，禁止省略号，尽量控制在 28 字以内且一行能说清。
            2) Tasks：content 仅输出 3-5 条步骤拆解，格式为「[步骤 1] xxx」，不要重复用户原文。
            3) Ideas：content 仅输出 3 行 [AI 脑风暴]，不要重复用户原文。知识类/问句也归为 Ideas。
            4) 仅 Tasks 且有明确时间时填写 remind_at 与 time_text；支持“30分钟后”“明天下午7点”“下周一上午10点”等。
            5) remind_at 使用 yyyy-MM-dd HH:mm:ss，按北京时间 +08:00 计算。
            6) 只输出 JSON，不要 markdown 代码块。
        """.trimIndent()

        val payload = JSONObject()
            .put("model", model)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", instruction))
                    .put(JSONObject().put("role", "user").put("content", rawText))
            )
            .put("stream", true)

        val req = Request.Builder()
            .url("https://api.deepseek.com/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val allText = StringBuilder()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("DeepSeek 接口错误: ${res.code}")
            val src = res.body?.source() ?: throw IllegalStateException("DeepSeek 返回为空")
            while (!src.exhausted()) {
                val line = src.readUtf8Line() ?: continue
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break
                runCatching {
                    val json = JSONObject(data)
                    val token = json.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("delta")
                        ?.optString("content")
                        .orEmpty()
                    if (token.isNotEmpty()) {
                        allText.append(token)
                        onStreamToken(token)
                    }
                }
            }
        }

        val outputText = allText.toString().trim()
        if (outputText.isBlank()) throw IllegalStateException("DeepSeek 返回内容为空")
        val parsed = JSONObject(extractJsonObject(outputText))
        val type = when (parsed.optString("type")) {
            "Tasks" -> MemoType.Tasks
            "Knowledge" -> MemoType.Ideas
            else -> MemoType.Ideas
        }

        val remindAtRaw = parsed.opt("remind_at")
        val remindAtMillis = when (remindAtRaw) {
            null, JSONObject.NULL -> null
            else -> TimeParseUtils.parseIsoTime(remindAtRaw.toString())
        }
        val timeTextRaw = parsed.opt("time_text")
        val timeText = when (timeTextRaw) {
            null, JSONObject.NULL -> null
            else -> timeTextRaw.toString().trim().ifBlank { null }
        }
        val fallbackReminder = if (type == MemoType.Tasks && remindAtMillis == null) {
            TimeParseUtils.parseFromNaturalLanguage(rawText)
        } else {
            null
        }

        val summary = parsed.optString("summary").trim().ifBlank {
            AiContentUtils.buildLocalSummary(rawText)
        }
        val content = parsed.optString("content").trim()

        AiResult(
            type = type,
            summary = summary,
            content = content,
            remindAtMillis = if (type == MemoType.Tasks) {
                remindAtMillis ?: fallbackReminder?.remindAtMillis
            } else {
                null
            },
            timeText = if (type == MemoType.Tasks) {
                timeText ?: fallbackReminder?.timeText
            } else {
                null
            }
        )
    }

    private fun extractJsonObject(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("{")) return trimmed
        val fenced = Regex("""```(?:json)?\s*([\s\S]*?)```""").find(trimmed)?.groupValues?.get(1)?.trim()
        if (!fenced.isNullOrBlank()) return fenced
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)
        return trimmed
    }
}
