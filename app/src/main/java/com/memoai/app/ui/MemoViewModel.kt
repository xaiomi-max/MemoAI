package com.memoai.app.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.memoai.app.ai.DeepSeekAiEngine
import com.memoai.app.ai.LocalAiEngine
import com.memoai.app.ai.isTravelRelatedTaskByAi
import com.memoai.app.BuildConfig
import com.memoai.app.data.MemoEntity
import com.memoai.app.data.MemoRepository
import com.memoai.app.lockscreen.LockScreenTodoManager
import com.memoai.app.reminder.ReminderMode
import com.memoai.app.reminder.ReminderPermissionHelper
import com.memoai.app.reminder.ReminderScheduler
import com.memoai.app.home.WishStore
import com.memoai.app.smartitinerary.LocationHelper
import com.memoai.app.smartitinerary.SmartItineraryFollowUpPrompt
import com.memoai.app.smartitinerary.SmartItineraryPlanner
import com.memoai.app.smartitinerary.SmartItineraryScheduler
import com.memoai.app.smartitinerary.SmartItinerarySetupPrompt
import com.memoai.app.smartitinerary.SmartItineraryStore
import com.memoai.app.smartitinerary.SmartItineraryTrip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MemoType { Tasks, Ideas }

fun parseMemoType(raw: String): MemoType = when (raw) {
    MemoType.Tasks.name -> MemoType.Tasks
    else -> MemoType.Ideas
}

data class MemoUi(
    val id: Long,
    val type: MemoType,
    val summary: String,
    val userInput: String,
    val content: String,
    val highlights: List<String>,
    val timestamp: String,
    val remindAtMillis: Long? = null,
    val timeText: String? = null,
    val completed: Boolean = false,
    val reminderMode: ReminderMode? = null,
    val cloudProcessed: Boolean = false,
    val createdAtMillis: Long = 0L,
    val completedAtMillis: Long? = null
)

data class AiSettings(
    val useCloud: Boolean = false,
    val apiKey: String = "",
    val model: String = "deepseek-chat",
    val taskBreakdownEnabled: Boolean = true,
    val brainstormEnabled: Boolean = true
)

private data class PendingReminder(
    val type: MemoType,
    val summary: String,
    val userInput: String,
    val content: String,
    val remindAtMillis: Long,
    val timeText: String,
    val cloudProcessed: Boolean
)

data class StepRewardEvent(val shellCount: Int)

class MemoViewModel(
    private val repo: MemoRepository,
    private val prefs: SharedPreferences,
    private val appContext: Context
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val settings = MutableStateFlow(loadSettings())
    private val busy = MutableStateFlow(false)
    private val info = MutableStateFlow("")
    private val streamingInput = MutableStateFlow("")
    private val streamingOutput = MutableStateFlow("")
    private val autoDeleteCompleted = MutableStateFlow(prefs.getBoolean("auto_delete_completed", false))
    private val lockScreenTodoEnabled = MutableStateFlow(LockScreenTodoManager.isEnabled(prefs))
    private val smartItineraryEnabled = MutableStateFlow(prefs.getBoolean("smart_itinerary_enabled", true))
    private val pendingReminder = MutableStateFlow<PendingReminder?>(null)
    private val pendingSmartItinerarySetup = MutableStateFlow<SmartItinerarySetupPrompt?>(null)
    private val smartItineraryFollowUp = MutableStateFlow<SmartItineraryFollowUpPrompt?>(null)
    private val stepRewardEvent = MutableStateFlow<StepRewardEvent?>(null)
    private val smartItineraryStore = SmartItineraryStore(appContext)
    private var activeSmartSetup: SmartItinerarySetupPrompt? = null

    val memos: StateFlow<List<MemoUi>> = repo.observeMemos().combine(query) { list, _ ->
        list.map { it.toUi() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val aiSettings: StateFlow<AiSettings> = settings
    val isBusy: StateFlow<Boolean> = busy
    val message: StateFlow<String> = info
    val currentStreamingInput: StateFlow<String> = streamingInput
    val currentStreamingOutput: StateFlow<String> = streamingOutput
    val isAutoDeleteCompleted: StateFlow<Boolean> = autoDeleteCompleted
    val isLockScreenTodoEnabled: StateFlow<Boolean> = lockScreenTodoEnabled
    val isSmartItineraryEnabled: StateFlow<Boolean> = smartItineraryEnabled
    val smartItinerarySetup: StateFlow<SmartItinerarySetupPrompt?> = pendingSmartItinerarySetup
    val smartItineraryFollowUpPrompt: StateFlow<SmartItineraryFollowUpPrompt?> = smartItineraryFollowUp
    val reminderConfirmation: StateFlow<ReminderConfirmation?> = pendingReminder
        .map { pending ->
            pending?.let {
                ReminderConfirmation(
                    userInput = it.userInput,
                    summary = it.summary,
                    remindAtMillis = it.remindAtMillis,
                    timeText = it.timeText
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val stepReward: StateFlow<StepRewardEvent?> = stepRewardEvent

    init {
        ReminderScheduler.createNotificationChannel(appContext)
        LockScreenTodoManager.createNotificationChannel(appContext)
        viewModelScope.launch {
            if (repo.count() == 0) {
                seedData()
            }
            restoreFutureReminders()
        }
        viewModelScope.launch {
            memos.collect { list -> syncLockScreenTodos(list) }
        }
    }

    fun onAppStart() {
        viewModelScope.launch {
            restoreFutureReminders()
            syncLockScreenTodos(memos.value)
            smartItineraryFollowUp.value = smartItineraryStore.consumePendingFollowUp()
        }
    }

    fun refreshReminderAlarms() {
        viewModelScope.launch {
            restoreFutureReminders()
            syncLockScreenTodos(memos.value)
        }
    }

    fun updateAutoDeleteCompleted(enabled: Boolean) {
        autoDeleteCompleted.value = enabled
        prefs.edit().putBoolean("auto_delete_completed", enabled).apply()
    }

    fun updateLockScreenTodoEnabled(enabled: Boolean) {
        lockScreenTodoEnabled.value = enabled
        LockScreenTodoManager.setEnabled(appContext, prefs, enabled)
        if (enabled) {
            syncLockScreenTodos(memos.value)
        }
    }

    fun updateSmartItineraryEnabled(enabled: Boolean) {
        smartItineraryEnabled.value = enabled
        prefs.edit().putBoolean("smart_itinerary_enabled", enabled).apply()
    }

    fun respondToSmartItinerarySetup(acceptSuggestion: Boolean) {
        val setup = pendingSmartItinerarySetup.value ?: return
        pendingSmartItinerarySetup.value = null
        activeSmartSetup = setup
        val finalRemindAt = if (acceptSuggestion) {
            setup.suggestedDepartAtMillis
        } else {
            setup.remindAtMillis
        }
        val finalTimeText = if (acceptSuggestion) {
            setup.suggestedDepartText
        } else {
            setup.timeText
        }
        pendingReminder.value = PendingReminder(
            type = MemoType.Tasks,
            summary = setup.summary,
            userInput = setup.userInput,
            content = setup.content,
            remindAtMillis = finalRemindAt,
            timeText = finalTimeText,
            cloudProcessed = setup.cloudProcessed
        )
    }

    fun cancelSmartItinerarySetup() {
        val setup = pendingSmartItinerarySetup.value ?: return
        pendingSmartItinerarySetup.value = null
        activeSmartSetup = null
        viewModelScope.launch {
            repo.insert(
                MemoEntity(
                    type = MemoType.Tasks.name,
                    summary = setup.summary,
                    userInput = setup.userInput,
                    content = setup.content,
                    highlights = "",
                    timestamp = nowText(),
                    remindAtMillis = setup.remindAtMillis,
                    timeText = setup.timeText,
                    cloudProcessed = setup.cloudProcessed,
                    createdAtMillis = nowMillis()
                )
            )
        }
    }

    fun respondToSmartItineraryFollowUp(acceptSuggestion: Boolean) {
        val prompt = smartItineraryFollowUp.value ?: return
        smartItineraryFollowUp.value = null
        if (!acceptSuggestion || prompt.suggestedRemindAtMillis == null) return
        viewModelScope.launch {
            val memo = memos.value.firstOrNull { it.id == prompt.memoId } ?: return@launch
            val newTimeText = prompt.suggestedRemindText ?: memo.timeText.orEmpty()
            val updated = memo.copy(
                remindAtMillis = prompt.suggestedRemindAtMillis,
                timeText = newTimeText
            )
            save(updated)
            smartItineraryStore.updateFinalRemindAt(
                prompt.memoId,
                prompt.suggestedRemindAtMillis,
                newTimeText
            )
            info.value = "已更新提醒：$newTimeText"
        }
    }

    fun dismissSmartItineraryFollowUp() {
        smartItineraryFollowUp.value = null
    }

    fun confirmReminder(mode: ReminderMode) {
        val pending = pendingReminder.value ?: return
        pendingReminder.value = null
        val smartSetup = activeSmartSetup
        activeSmartSetup = null
        viewModelScope.launch {
            val id = repo.insert(
                MemoEntity(
                    type = MemoType.Tasks.name,
                    summary = pending.summary,
                    userInput = pending.userInput,
                    content = pending.content,
                    highlights = "",
                    timestamp = nowText(),
                    remindAtMillis = pending.remindAtMillis,
                    timeText = pending.timeText,
                    cloudProcessed = pending.cloudProcessed,
                    createdAtMillis = nowMillis()
                )
            )
            scheduleReminderIfNeeded(
                memoId = id,
                type = MemoType.Tasks,
                summary = pending.summary,
                userInput = pending.userInput,
                remindAtMillis = pending.remindAtMillis,
                timeText = pending.timeText
            )
            if (smartSetup != null && smartItineraryEnabled.value) {
                val home = LocationHelper.captureCurrentLocation(appContext)
                if (home != null) {
                    smartItineraryStore.saveHomeLocation(home.first, home.second)
                }
                val trip = SmartItineraryTrip(
                    memoId = id,
                    destination = smartSetup.destination,
                    finalRemindAtMillis = pending.remindAtMillis,
                    timeText = pending.timeText,
                    homeLat = home?.first,
                    homeLng = home?.second,
                    createdAtMillis = nowMillis()
                )
                smartItineraryStore.saveTrip(trip)
                SmartItineraryScheduler.scheduleFollowUps(appContext, trip)
            }
        }
    }

    fun cancelReminderConfirmation() {
        val pending = pendingReminder.value ?: return
        pendingReminder.value = null
        activeSmartSetup = null
        viewModelScope.launch {
            repo.insert(
                MemoEntity(
                    type = pending.type.name,
                    summary = pending.summary,
                    userInput = pending.userInput,
                    content = pending.content,
                    highlights = "",
                    timestamp = nowText(),
                    cloudProcessed = pending.cloudProcessed,
                    createdAtMillis = nowMillis()
                )
            )
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
            val apiKey = resolveApiKey(cfg)
            var cloudProcessed = false
            val result = try {
                if (cfg.useCloud && apiKey.isNotBlank()) {
                    cloudProcessed = true
                    DeepSeekAiEngine(apiKey, cfg.model).process(trimmed) { token ->
                        streamingOutput.value += token
                    }
                } else {
                    local.process(trimmed)
                }
            } catch (e: Exception) {
                cloudProcessed = false
                info.value = "DeepSeek 调用失败，已自动回退本地引擎"
                local.process(trimmed)
            }

            val storedContent = if (cloudProcessed) result.content else trimmed
        val finalContent = when (result.type) {
            MemoType.Tasks -> if (settings.value.taskBreakdownEnabled) storedContent else trimmed
            MemoType.Ideas -> if (settings.value.brainstormEnabled) storedContent else trimmed
        }

            if (result.type == MemoType.Tasks &&
                result.remindAtMillis != null &&
                result.remindAtMillis > System.currentTimeMillis()
            ) {
                val travelRelated = if (cloudProcessed && apiKey.isNotBlank()) {
                    isTravelRelatedTaskByAi(
                        apiKey = apiKey,
                        model = cfg.model,
                        text = "$trimmed\n${result.summary}\n${result.content}"
                    )
                } else {
                    isTravelRelatedTaskByKeyword(trimmed, result.summary, result.content)
                }
                val useSmart = smartItineraryEnabled.value &&
                    cloudProcessed &&
                    apiKey.isNotBlank() &&
                    travelRelated
                if (useSmart) {
                    pendingSmartItinerarySetup.value = SmartItineraryPlanner.buildInitialSuggestion(
                        userInput = trimmed,
                        summary = result.summary,
                        content = finalContent,
                        remindAtMillis = result.remindAtMillis,
                        timeText = result.timeText.orEmpty(),
                        cloudProcessed = cloudProcessed
                    )
                } else {
                    pendingReminder.value = PendingReminder(
                        type = result.type,
                        summary = result.summary,
                        userInput = trimmed,
                        content = finalContent,
                        remindAtMillis = result.remindAtMillis,
                        timeText = result.timeText.orEmpty(),
                        cloudProcessed = cloudProcessed
                    )
                }
                busy.value = false
                streamingInput.value = ""
                streamingOutput.value = ""
                return@launch
            }

            val id = repo.insert(
                MemoEntity(
                    type = result.type.name,
                    summary = result.summary,
                    userInput = trimmed,
                    content = finalContent,
                    highlights = "",
                    timestamp = nowText(),
                    remindAtMillis = result.remindAtMillis,
                    timeText = result.timeText,
                    cloudProcessed = cloudProcessed,
                    createdAtMillis = nowMillis()
                )
            )
            scheduleReminderIfNeeded(
                memoId = id,
                type = result.type,
                summary = result.summary,
                userInput = trimmed,
                remindAtMillis = result.remindAtMillis,
                timeText = result.timeText
            )
            busy.value = false
            streamingInput.value = ""
            streamingOutput.value = ""
        }
    }

    fun updateAiSettings(
        useCloud: Boolean,
        apiKey: String,
        model: String,
        taskBreakdownEnabled: Boolean,
        brainstormEnabled: Boolean
    ) {
        val next = AiSettings(
            useCloud = useCloud,
            apiKey = apiKey.trim(),
            model = model.trim().ifBlank { "deepseek-chat" },
            taskBreakdownEnabled = taskBreakdownEnabled,
            brainstormEnabled = brainstormEnabled
        )
        settings.value = next
        prefs.edit()
            .putBoolean("use_cloud", next.useCloud)
            .putString("api_key", next.apiKey)
            .putString("model", next.model)
            .putBoolean("task_breakdown_enabled", next.taskBreakdownEnabled)
            .putBoolean("brainstorm_enabled", next.brainstormEnabled)
            .apply()
        info.value = if (next.useCloud && resolveApiKey(next).isNotBlank()) {
            "已启用 DeepSeek 云端模型"
        } else {
            "已启用本地模型"
        }
    }

    fun clearMessage() {
        info.value = ""
    }

    fun save(edited: MemoUi) {
        viewModelScope.launch {
            val entity = MemoEntity(
                id = edited.id,
                type = edited.type.name,
                summary = edited.summary,
                userInput = edited.userInput,
                content = edited.content,
                highlights = edited.highlights.joinToString(","),
                timestamp = edited.timestamp,
                remindAtMillis = edited.remindAtMillis,
                timeText = edited.timeText,
                completed = edited.completed,
                cloudProcessed = edited.cloudProcessed,
                createdAtMillis = edited.createdAtMillis,
                completedAtMillis = edited.completedAtMillis
            )
            repo.update(entity)
            ReminderScheduler.cancel(appContext, edited.id)
            scheduleReminderIfNeeded(
                memoId = edited.id,
                type = edited.type,
                summary = edited.summary,
                userInput = edited.userInput,
                remindAtMillis = edited.remindAtMillis,
                timeText = edited.timeText
            )
        }
    }

    fun delete(item: MemoUi) {
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext, item.id)
            SmartItineraryScheduler.cancelFollowUps(appContext, item.id)
            smartItineraryStore.removeTrip(item.id)
            repo.delete(
                MemoEntity(
                    id = item.id,
                    type = item.type.name,
                    summary = item.summary,
                    userInput = item.userInput,
                    content = item.content,
                    highlights = item.highlights.joinToString(","),
                    timestamp = item.timestamp,
                    remindAtMillis = item.remindAtMillis,
                    timeText = item.timeText,
                    completed = item.completed,
                    cloudProcessed = item.cloudProcessed,
                    createdAtMillis = item.createdAtMillis,
                    completedAtMillis = item.completedAtMillis
                )
            )
        }
    }

    fun dismissStepReward() {
        stepRewardEvent.value = null
    }

    fun toggleTaskStepComplete(item: MemoUi, stepIndex: Int) {
        if (item.type != MemoType.Tasks) return
        val key = com.memoai.app.ai.CardContentUtils.stepHighlightKey(stepIndex)
        val rewardKey = "rewarded:$stepIndex"
        val wasDone = item.highlights.contains(key)
        val next = item.highlights.toMutableList()
        if (wasDone) {
            next.remove(key)
        } else {
            next.add(key)
            if (!item.highlights.contains(rewardKey)) {
                next.add(rewardKey)
                val store = WishStore(appContext)
                store.setShellBalance(store.getShellBalance() + 1)
                stepRewardEvent.value = StepRewardEvent(shellCount = 1)
            }
        }
        save(item.copy(highlights = next))
    }

    fun toggleTaskComplete(item: MemoUi) {
        if (item.type != MemoType.Tasks) return
        viewModelScope.launch {
            val completed = !item.completed
            repo.setTaskCompleted(item.id, completed)
            if (completed) {
                ReminderScheduler.cancel(appContext, item.id)
                SmartItineraryScheduler.cancelFollowUps(appContext, item.id)
                smartItineraryStore.removeTrip(item.id)
            } else {
                scheduleReminderIfNeeded(
                    memoId = item.id,
                    type = item.type,
                    summary = item.summary,
                    userInput = item.userInput,
                    remindAtMillis = item.remindAtMillis,
                    timeText = item.timeText
                )
            }
        }
    }

    private fun scheduleReminderIfNeeded(
        memoId: Long,
        type: MemoType,
        summary: String,
        userInput: String,
        remindAtMillis: Long?,
        timeText: String?
    ) {
        if (type != MemoType.Tasks || remindAtMillis == null) return
        if (remindAtMillis <= System.currentTimeMillis()) return

        ReminderScheduler.schedule(
            context = appContext,
            memoId = memoId,
            content = summary.ifBlank { userInput },
            timeText = timeText.orEmpty(),
            remindAtMillis = remindAtMillis
        )
        val label = timeText?.takeIf { it.isNotBlank() } ?: "指定时间"
        info.value = "已设置提醒：$label"
    }

    private suspend fun restoreFutureReminders() {
        val now = System.currentTimeMillis()
        repo.futureReminders(now).forEach { memo ->
            if (memo.type != MemoType.Tasks.name) return@forEach
            val remindAt = memo.remindAtMillis ?: return@forEach
            ReminderScheduler.schedule(
                context = appContext,
                memoId = memo.id,
                content = memo.summary.ifBlank { memo.userInput },
                timeText = memo.timeText.orEmpty(),
                remindAtMillis = remindAt
            )
        }
    }

    private fun loadSettings(): AiSettings {
        val buildKey = BuildConfig.DEEPSEEK_API_KEY.trim()
        val savedKey = prefs.getString("api_key", null)?.trim().orEmpty()
        val apiKey = savedKey.ifBlank { buildKey }
        val hasKey = apiKey.isNotBlank()
        val useCloud = if (prefs.contains("use_cloud")) {
            prefs.getBoolean("use_cloud", hasKey)
        } else {
            hasKey
        }
        return AiSettings(
            useCloud = useCloud,
            apiKey = apiKey,
            model = prefs.getString("model", "deepseek-chat") ?: "deepseek-chat",
            taskBreakdownEnabled = prefs.getBoolean("task_breakdown_enabled", true),
            brainstormEnabled = prefs.getBoolean("brainstorm_enabled", true)
        )
    }

    private fun resolveApiKey(cfg: AiSettings): String {
        val savedKey = prefs.getString("api_key", null)?.trim().orEmpty()
        return savedKey.ifBlank { cfg.apiKey.ifBlank { BuildConfig.DEEPSEEK_API_KEY.trim() } }
    }

    private fun isTravelRelatedTaskByKeyword(
        userInput: String,
        summary: String,
        content: String
    ): Boolean {
        val text = "$userInput $summary $content".lowercase(Locale.getDefault())
        val travelKeywords = listOf(
            "出发", "到达", "前往", "路上", "通勤", "赶车", "打车", "地铁", "公交", "高铁",
            "火车", "机场", "航班", "车站", "导航", "目的地", "路况", "堵车", "行程", "酒店", "登机"
        )
        val hasTravelWord = travelKeywords.any { text.contains(it) }
        val hasPlacePattern = Regex("(到|去|前往)[\\p{L}\\p{N}\\u4e00-\\u9fa5]{2,}").containsMatchIn(userInput)
        return hasTravelWord || hasPlacePattern
    }

    private fun MemoEntity.toUi(): MemoUi {
        val parsed = highlights.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return MemoUi(
            id = id,
            type = parseMemoType(type),
            summary = summary,
            userInput = userInput,
            content = content,
            highlights = parsed,
            timestamp = timestamp,
            remindAtMillis = remindAtMillis,
            timeText = timeText,
            completed = completed,
            cloudProcessed = cloudProcessed,
            createdAtMillis = createdAtMillis,
            completedAtMillis = completedAtMillis
        )
    }

    private fun nowMillis(): Long = System.currentTimeMillis()

    private fun syncLockScreenTodos(memos: List<MemoUi>) {
        if (!lockScreenTodoEnabled.value) {
            LockScreenTodoManager.dismiss(appContext)
            return
        }
        if (!ReminderPermissionHelper.hasNotificationPermission(appContext)) {
            LockScreenTodoManager.dismiss(appContext)
            return
        }
        val tasks = memos
            .filter { it.type == MemoType.Tasks && !it.completed }
            .take(7)
            .map {
                LockScreenTodoManager.TodoItem(
                    id = it.id,
                    title = it.summary.ifBlank { it.userInput },
                    timeText = it.timeText
                )
            }
        LockScreenTodoManager.sync(appContext, tasks)
    }

    private fun nowText(): String {
        val fmt = SimpleDateFormat("hh:mm a", Locale.CHINA)
        return fmt.format(Date())
    }

    fun buildExportJson(memos: List<MemoUi>): String {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        val arr = org.json.JSONArray()
        memos.forEach { memo ->
            val obj = org.json.JSONObject()
            obj.put("分类标签", memo.type.name)
            obj.put("摘要", memo.summary)
            obj.put(
                "创建时间",
                if (memo.createdAtMillis > 0L) {
                    dateFmt.format(Date(memo.createdAtMillis))
                } else {
                    memo.timestamp
                }
            )
            obj.put(
                "完成时间",
                if (memo.completed && memo.completedAtMillis != null) {
                    dateFmt.format(Date(memo.completedAtMillis))
                } else {
                    org.json.JSONObject.NULL
                }
            )
            val delayMillis = when {
                memo.remindAtMillis != null && memo.completedAtMillis != null ->
                    memo.completedAtMillis - memo.remindAtMillis
                memo.remindAtMillis != null && !memo.completed ->
                    System.currentTimeMillis() - memo.remindAtMillis
                else -> null
            }
            obj.put("延迟毫秒", delayMillis ?: org.json.JSONObject.NULL)
            obj.put("提醒时间", memo.timeText ?: org.json.JSONObject.NULL)
            obj.put("已完成", memo.completed)
            arr.put(obj)
        }
        return arr.toString(2)
    }

    private suspend fun seedData() {
        repo.insert(
            MemoEntity(
                type = MemoType.Tasks.name,
                summary = "使用教程",
                userInput = "使用教程",
                content = "[步骤 1] 点击输入框，用语音或文字输入内容\n[步骤 2] 系统自动分类为 Task 或 Idea\n[步骤 3] 输入如「1分钟后测试提醒」可设置到点通知",
                highlights = "",
                timestamp = "欢迎",
                cloudProcessed = true,
                createdAtMillis = nowMillis()
            )
        )
        repo.insert(
            MemoEntity(
                type = MemoType.Ideas.name,
                summary = "编辑与删除",
                userInput = "编辑与删除",
                content = "[AI 脑风暴] 1) 点击右上角画笔进入编辑页\n[AI 脑风暴] 2) 长按卡片可快速删除\n[AI 脑风暴] 3) Tasks 卡片右侧圆圈可标记完成",
                highlights = "",
                timestamp = "提示",
                cloudProcessed = true,
                createdAtMillis = nowMillis()
            )
        )
        repo.insert(
            MemoEntity(
                type = MemoType.Ideas.name,
                summary = "更多功能",
                userInput = "更多功能",
                content = "[AI 脑风暴] 1) 打开右上角设置可配置 DeepSeek\n[AI 脑风暴] 2) 可更换首页和分类页壁纸\n[AI 脑风暴] 3) 开启锁屏待办通知随时查看任务",
                highlights = "",
                timestamp = "提示",
                cloudProcessed = true,
                createdAtMillis = nowMillis()
            )
        )
    }
}

class MemoViewModelFactory(
    private val repo: MemoRepository,
    private val prefs: SharedPreferences,
    private val appContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MemoViewModel(repo, prefs, appContext) as T
    }
}
