package com.memoai.app.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.memoai.app.ai.DeepSeekAiEngine
import com.memoai.app.ai.LocalAiEngine
import com.memoai.app.data.MemoEntity
import com.memoai.app.data.MemoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MemoType { Tasks, Ideas, Knowledge }

data class MemoUi(
    val id: Long,
    val type: MemoType,
    val summary: String,
    val userInput: String,
    val content: String,
    val highlights: List<String>,
    val timestamp: String
)

data class AiSettings(
    val useCloud: Boolean = false,
    val apiKey: String = "",
    val model: String = "deepseek-chat"
)

class MemoViewModel(
    private val repo: MemoRepository,
    private val prefs: SharedPreferences
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val settings = MutableStateFlow(loadSettings())
    private val busy = MutableStateFlow(false)
    private val info = MutableStateFlow("")
    private val streamingInput = MutableStateFlow("")
    private val streamingOutput = MutableStateFlow("")

    val memos: StateFlow<List<MemoUi>> = repo.observeMemos().combine(query) { list, _ ->
        list.map { it.toUi() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val aiSettings: StateFlow<AiSettings> = settings
    val isBusy: StateFlow<Boolean> = busy
    val message: StateFlow<String> = info
    val currentStreamingInput: StateFlow<String> = streamingInput
    val currentStreamingOutput: StateFlow<String> = streamingOutput

    init {
        viewModelScope.launch {
            if (repo.count() == 0) {
                seedData()
            }
        }
    }

    fun setSearchQuery(value: String) {
        query.value = value
    }

    fun currentQuery(): StateFlow<String> = query

    fun addByVoice(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            busy.value = true
            streamingInput.value = trimmed
            streamingOutput.value = ""
            val cfg = settings.value
            val local = LocalAiEngine()
            val result = try {
                if (cfg.useCloud) {
                    if (cfg.apiKey.isBlank()) {
                        throw IllegalStateException("请先在设置页配置 DeepSeek API Key")
                    }
                    DeepSeekAiEngine(cfg.apiKey, cfg.model).process(trimmed) { token ->
                        streamingOutput.value += token
                    }
                } else {
                    local.process(trimmed)
                }
            } catch (e: Exception) {
                if (cfg.useCloud && cfg.apiKey.isBlank()) {
                    info.value = "请先配置 DeepSeek API Key"
                    busy.value = false
                    streamingInput.value = ""
                    streamingOutput.value = ""
                    return@launch
                }
                info.value = "DeepSeek 调用失败，已自动回退本地引擎"
                local.process(trimmed)
            }

            repo.insert(
                MemoEntity(
                    type = result.type.name,
                    summary = result.summary,
                    userInput = trimmed,
                    content = result.content,
                    highlights = "",
                    timestamp = nowText()
                )
            )
            busy.value = false
            streamingInput.value = ""
            streamingOutput.value = ""
        }
    }

    fun updateAiSettings(useCloud: Boolean, apiKey: String, model: String) {
        val next = AiSettings(
            useCloud = useCloud,
            apiKey = apiKey.trim(),
            model = model.trim().ifBlank { "deepseek-chat" }
        )
        settings.value = next
        prefs.edit()
            .putBoolean("use_cloud", next.useCloud)
            .putString("api_key", next.apiKey)
            .putString("model", next.model)
            .apply()
        info.value = if (next.useCloud && next.apiKey.isNotBlank()) "已启用 DeepSeek 云端模型" else "已启用本地模型"
    }

    fun clearMessage() {
        info.value = ""
    }

    fun save(edited: MemoUi) {
        viewModelScope.launch {
            repo.update(
                MemoEntity(
                    id = edited.id,
                    type = edited.type.name,
                    summary = edited.summary,
                    userInput = edited.userInput,
                    content = edited.content,
                    highlights = edited.highlights.joinToString(","),
                    timestamp = edited.timestamp
                )
            )
        }
    }

    fun delete(item: MemoUi) {
        viewModelScope.launch {
            repo.delete(
                MemoEntity(
                    id = item.id,
                    type = item.type.name,
                    summary = item.summary,
                    userInput = item.userInput,
                    content = item.content,
                    highlights = item.highlights.joinToString(","),
                    timestamp = item.timestamp
                )
            )
        }
    }

    private fun loadSettings(): AiSettings {
        return AiSettings(
            useCloud = prefs.getBoolean("use_cloud", false),
            apiKey = prefs.getString("api_key", "") ?: "",
            model = prefs.getString("model", "deepseek-chat") ?: "deepseek-chat"
        )
    }

    private fun MemoEntity.toUi(): MemoUi {
        val parsed = highlights.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return MemoUi(
            id = id,
            type = MemoType.valueOf(type),
            summary = summary,
            userInput = userInput,
            content = content,
            highlights = parsed,
            timestamp = timestamp
        )
    }

    private fun nowText(): String {
        val fmt = SimpleDateFormat("hh:mm a", Locale.CHINA)
        return fmt.format(Date())
    }

    private suspend fun seedData() {
        repo.insert(
            MemoEntity(
                type = MemoType.Tasks.name,
                summary = "使用教程",
                userInput = "使用教程",
                content = "点击输入框，使用输入法语音转文字输入内容。系统会自动分类为 Task/Idea/Knowledge 并总结要点。其中 Idea 可以头脑风暴发散思维，Knowledge 会 AI 扩展知识。",
                highlights = "",
                timestamp = "欢迎"
            )
        )
        repo.insert(
            MemoEntity(
                type = MemoType.Ideas.name,
                summary = "编辑与删除",
                userInput = "编辑与删除",
                content = "点击卡片右上角的画笔图标可以进入编辑页面修改或删除卡片，也可以长按卡片直接删除。",
                highlights = "",
                timestamp = "提示"
            )
        )
        repo.insert(
            MemoEntity(
                type = MemoType.Knowledge.name,
                summary = "更多功能",
                userInput = "更多功能",
                content = "打开右上角设置，可以开启 DeepSeek 云端 AI 以及更换壁纸等功能。",
                highlights = "",
                timestamp = "提示"
            )
        )
        repo.insert(
            MemoEntity(
                type = MemoType.Knowledge.name,
                summary = "Jetpack Compose 定义",
                userInput = "什么是 Jetpack Compose？",
                content = "什么是 Jetpack Compose？这是 Android 的现代声明式 UI 工具包。\n\n[延伸知识]：它通过函数式组件和状态驱动渲染减少模板代码，是 Kotlin 原生 UI 开发的关键能力。",
                highlights = "声明式 UI,Kotlin",
                timestamp = "昨天"
            )
        )
    }
}

class MemoViewModelFactory(
    private val repo: MemoRepository,
    private val prefs: SharedPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MemoViewModel(repo, prefs) as T
    }
}
