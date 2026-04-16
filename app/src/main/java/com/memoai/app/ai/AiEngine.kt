package com.memoai.app.ai

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

data class AiResult(
    val type: MemoType,
    val summary: String,
    val content: String
)

interface AiEngine {
    suspend fun process(rawText: String, onStreamToken: (String) -> Unit = {}): AiResult
}

class LocalAiEngine : AiEngine {
    override suspend fun process(rawText: String, onStreamToken: (String) -> Unit): AiResult {
        val type = classify(rawText)
        val content = buildString {
            append(rawText)
            when (type) {
                MemoType.Ideas -> {
                    append("\n\n[AI 脑风暴]：1) 先做最小可用版本验证需求。")
                    append("\n[AI 脑风暴]：2) 增加离线语音识别，弱网也可记录。")
                    append("\n[AI 脑风暴]：3) 加入智能穿戴快捷入口，跑步也能记。")
                }
                MemoType.Knowledge -> {
                    append("\n\n[延伸知识]：该概念通常用于提升系统结构表达能力，建议从定义、适用场景和一个真实案例三步学习。")
                }
                MemoType.Tasks -> Unit
            }
        }
        return AiResult(
            type = type,
            summary = if (rawText.length > 12) "${rawText.take(12)}..." else rawText,
            content = content
        )
    }

    private fun classify(text: String): MemoType {
        val raw = text.trim()
        val lower = raw.lowercase(Locale.getDefault())

        val knowledgeKeywords = listOf("什么是", "为什么", "怎么", "如何", "原理", "定义", "解释", "区别", "对比", "介绍", "概念", "百科")
        val isQuestion = raw.contains("？") || raw.contains("?")
        if (knowledgeKeywords.any { lower.contains(it) } || (isQuestion && listOf("吗", "如何", "怎么").any { lower.contains(it) })) {
            return MemoType.Knowledge
        }

        val ideaKeywords = listOf("想", "打算", "计划", "准备", "希望", "可以", "如果", "可能", "要不要", "能不能", "可不可以")
        val hasIdeaContext = ideaKeywords.any { lower.contains(it) }

        val taskKeywords = listOf("买", "去", "完成", "提交", "安排", "提醒", "联系", "打电话", "发邮件", "缴费", "预约", "下载", "安装", "修复", "更新", "整理", "发送", "写", "做")
        val hasTaskVerb = taskKeywords.any { lower.contains(it) }

        val hasTimeHint = Regex("""(\d{1,2}[:点时]\d{0,2}|今天|明天|后天|周[一二三四五六日天]|星期[一二三四五六日天]|下午|晚上|早上|中午)""")
            .containsMatchIn(raw)

        if (hasTaskVerb && (!hasIdeaContext || hasTimeHint)) return MemoType.Tasks
        if (hasIdeaContext) return MemoType.Ideas
        return if (hasTaskVerb) MemoType.Tasks else MemoType.Ideas
    }
}

class DeepSeekAiEngine(
    private val apiKey: String,
    private val model: String = "deepseek-chat"
) : AiEngine {
    private val client = OkHttpClient()

    override suspend fun process(rawText: String, onStreamToken: (String) -> Unit): AiResult = withContext(Dispatchers.IO) {
        val instruction = """
            你是一个智能助手。请将输入中文内容分类并增强，返回严格 JSON：
            {"type":"Tasks|Ideas|Knowledge","summary":"不超过12字，可省略号","content":"原文+增强文本"}
            
            规则：
            1) Tasks：包含“做/去/买”等动作，content 保留原文，不加延伸。
            2) Ideas：创意类，必须追加 3 行 [AI 脑风暴]。
            3) Knowledge：定义/百科类，必须追加 1 行 [延伸知识]。
            4) content 第一行必须是原文，不改写原文。
            5) 只输出 JSON。
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
        val parsed = JSONObject(outputText)
        val type = when (parsed.optString("type")) {
            "Tasks" -> MemoType.Tasks
            "Knowledge" -> MemoType.Knowledge
            else -> MemoType.Ideas
        }
        AiResult(
            type = type,
            summary = parsed.optString("summary").ifBlank { rawText.take(12) },
            content = parsed.optString("content").ifBlank { rawText }
        )
    }
}
