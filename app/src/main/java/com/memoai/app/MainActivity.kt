@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.memoai.app

import android.os.Bundle
import android.content.Context
import android.net.Uri
import android.os.Build
import android.content.SharedPreferences
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.memoai.app.BuildConfig
import com.memoai.app.voice.HomeVoiceInputButton
import com.memoai.app.home.CabinUnlockEvent
import com.memoai.app.home.CabinUnlockManager
import com.memoai.app.home.RedeemResult
import com.memoai.app.home.WishItem
import com.memoai.app.home.WishStore
import com.memoai.app.data.MemoDatabase
import com.memoai.app.data.MemoRepository
import com.memoai.app.ui.HomeSideDrawer
import com.memoai.app.ui.StepCompleteCelebration
import com.memoai.app.smartitinerary.SmartItineraryFollowUpPrompt
import com.memoai.app.smartitinerary.SmartItinerarySetupPrompt
import com.memoai.app.ai.CardContentUtils
import com.memoai.app.ui.AiSettings
import com.memoai.app.ui.MemoType
import com.memoai.app.ui.MemoUi
import com.memoai.app.ui.MemoViewModel
import com.memoai.app.ui.MemoViewModelFactory
import com.memoai.app.ui.ReminderConfirmation
import com.memoai.app.reminder.ReminderMode
import com.memoai.app.lockscreen.LockScreenTodoManager
import com.memoai.app.reminder.ReminderScheduler
import com.memoai.app.reminder.ReminderPermissionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private var openTasksRequest by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openTasksRequest = intent?.getBooleanExtra(LockScreenTodoManager.EXTRA_OPEN_TASKS, false) ?: false
        ReminderScheduler.createNotificationChannel(this)
        LockScreenTodoManager.createNotificationChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            val prefs = getSharedPreferences("memo_ai_prefs", MODE_PRIVATE)
            if (!alarmManager.canScheduleExactAlarms() && !prefs.getBoolean("exact_alarm_prompted", false)) {
                prefs.edit().putBoolean("exact_alarm_prompted", true).apply()
                startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
        enableEdgeToEdge()
        window.statusBarColor = android.graphics.Color.parseColor("#EDF5FC")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContent {
            val db = remember { MemoDatabase.get(applicationContext) }
            val repo = remember { MemoRepository(db.memoDao()) }
            val prefs = remember { getSharedPreferences("memo_ai_prefs", MODE_PRIVATE) }
            val vm: MemoViewModel = viewModel(factory = MemoViewModelFactory(repo, prefs, applicationContext))
            LaunchedEffect(vm) {
                vm.onAppStart()
            }
            MemoAiApp(
                vm = vm,
                prefs = prefs,
                openTasksOnLaunch = openTasksRequest,
                onTasksOpened = { openTasksRequest = false }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(LockScreenTodoManager.EXTRA_OPEN_TASKS, false)) {
            openTasksRequest = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoAiApp(
    vm: MemoViewModel,
    prefs: SharedPreferences,
    openTasksOnLaunch: Boolean = false,
    onTasksOpened: () -> Unit = {}
) {
    val context = LocalContext.current
    val memos by vm.memos.collectAsStateWithLifecycle()
    val query by vm.currentQuery().collectAsStateWithLifecycle()
    val aiSettings by vm.aiSettings.collectAsStateWithLifecycle()
    val autoDeleteCompleted by vm.isAutoDeleteCompleted.collectAsStateWithLifecycle()
    val lockScreenTodoEnabled by vm.isLockScreenTodoEnabled.collectAsStateWithLifecycle()
    val smartItineraryEnabled by vm.isSmartItineraryEnabled.collectAsStateWithLifecycle()
    val smartItinerarySetup by vm.smartItinerarySetup.collectAsStateWithLifecycle()
    val smartItineraryFollowUp by vm.smartItineraryFollowUpPrompt.collectAsStateWithLifecycle()
    val busy by vm.isBusy.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    val reminderConfirmation by vm.reminderConfirmation.collectAsStateWithLifecycle()
    val stepReward by vm.stepReward.collectAsStateWithLifecycle()
    var selectedType by remember { mutableStateOf<MemoType?>(null) }
    var editing by remember { mutableStateOf<MemoUi?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var drawerOpen by remember { mutableStateOf(false) }
    var showMeme by remember { mutableStateOf(false) }
    var homeSubTab by remember { mutableStateOf(HomeSubTab.CornerCabin) }
    var cabinUnlockEvent by remember { mutableStateOf<CabinUnlockEvent?>(null) }
    var showWallpaper by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var homeWallpaperUri by remember { mutableStateOf(loadPrefsUri(prefs, "wallpaper_home_uri")) }
    var categoryWallpaperUri by remember { mutableStateOf(loadPrefsUri(prefs, "wallpaper_category_uri")) }

    LaunchedEffect(openTasksOnLaunch) {
        if (openTasksOnLaunch) {
            selectedType = MemoType.Tasks
            editing = null
            showMeme = false
            onTasksOpened()
        }
    }

    val homeWallpaperBitmap = rememberDecodedBitmap(homeWallpaperUri)
    val categoryWallpaperBitmap = rememberDecodedBitmap(categoryWallpaperUri)

    var showOemSetup by remember {
        mutableStateOf(ReminderPermissionHelper.shouldPromptOemSetup(context, prefs))
    }
    var showNotificationPrompt by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !ReminderPermissionHelper.hasNotificationPermission(context) &&
                !prefs.getBoolean("notification_prompt_shown", false)
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        prefs.edit().putBoolean("notification_prompt_shown", true).apply()
        showNotificationPrompt = false
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshReminderAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun submitDraft(overrideText: String? = null) {
        val text = (overrideText ?: draft).trim()
        if (text.isBlank()) return
        vm.addByVoice(text)
        draft = ""
    }

    LaunchedEffect(msg) {
        if (msg.isNotBlank()) {
            val duration = if (msg.length > 36) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            Toast.makeText(context, msg, duration).show()
            vm.clearMessage()
        }
    }

    val completedTaskCount = remember(memos) {
        memos.count { it.type == MemoType.Tasks && it.completed }
    }
    var unlockTrackingReady by remember { mutableStateOf(false) }
    LaunchedEffect(completedTaskCount) {
        CabinUnlockManager.syncUnlockedCount(prefs, completedTaskCount)
        if (!unlockTrackingReady) {
            unlockTrackingReady = true
        } else {
            val event = CabinUnlockManager.onTaskCompleted(prefs, completedTaskCount)
            if (event != null) cabinUnlockEvent = event
        }
    }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (editing == null) {
                    if (selectedType == null) {
                        BottomHomeBar(
                            selectedType = selectedType,
                            showMeme = showMeme,
                            homeSubTab = homeSubTab,
                            onHomeSubTabChange = { homeSubTab = it },
                            showInput = !showMeme,
                            value = draft,
                            onValueChange = { draft = it },
                            onSend = { submitDraft(it) },
                            onTap = { type ->
                                showMeme = false
                                selectedType = type
                            },
                            onOpenMeme = {
                                selectedType = null
                                homeSubTab = HomeSubTab.CornerCabin
                                showMeme = true
                            }
                        )
                    } else {
                        BottomCategoryBar(
                            selectedType = selectedType,
                            showMeme = showMeme,
                            homeSubTab = homeSubTab,
                            onHomeSubTabChange = { homeSubTab = it },
                            onTap = {
                                selectedType = it
                                showMeme = false
                            },
                            onOpenMeme = {
                                selectedType = null
                                homeSubTab = HomeSubTab.CornerCabin
                                showMeme = true
                            }
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(CategoryUi.BgBottom)
            ) {
                when {
                    editing != null -> EditScreen(
                        memo = editing!!,
                        taskBreakdownEnabled = aiSettings.taskBreakdownEnabled,
                        brainstormEnabled = aiSettings.brainstormEnabled,
                        onBack = { editing = null },
                        onDelete = { vm.delete(it); editing = null },
                        onSave = { vm.save(it); editing = null }
                    )
                    selectedType == null -> HomeScreen(
                        memos = memos,
                        onEdit = { editing = it },
                        onDelete = { vm.delete(it) },
                        onToggleComplete = { vm.toggleTaskComplete(it) },
                        onOpenSettings = { showSettings = true },
                        onOpenDrawer = { drawerOpen = true },
                        taskBreakdownEnabled = aiSettings.taskBreakdownEnabled,
                        brainstormEnabled = aiSettings.brainstormEnabled,
                        wallpaper = homeWallpaperBitmap
                    )
                    selectedType == MemoType.Tasks -> Box(modifier = Modifier.fillMaxSize()) {
                        HomeScreen(
                            memos = memos,
                            onEdit = { editing = it },
                            onDelete = { vm.delete(it) },
                            onToggleComplete = { vm.toggleTaskComplete(it) },
                            onOpenSettings = { showSettings = true },
                            onOpenDrawer = { drawerOpen = true },
                            taskBreakdownEnabled = aiSettings.taskBreakdownEnabled,
                            brainstormEnabled = aiSettings.brainstormEnabled,
                            wallpaper = homeWallpaperBitmap
                        )
                        TasksScreen(
                            memos = memos,
                            query = query,
                            onQuery = vm::setSearchQuery,
                            onBack = { selectedType = null },
                            onDelete = { vm.delete(it) },
                            onEdit = { editing = it },
                            onToggleComplete = { vm.toggleTaskComplete(it) },
                            onToggleStepComplete = { memo, index -> vm.toggleTaskStepComplete(memo, index) },
                            taskBreakdownEnabled = aiSettings.taskBreakdownEnabled,
                            brainstormEnabled = aiSettings.brainstormEnabled,
                            cabinUnlockEvent = cabinUnlockEvent,
                            onDismissCabinUnlock = { cabinUnlockEvent = null },
                            onOpenCabin = {
                                cabinUnlockEvent = null
                                homeSubTab = HomeSubTab.CornerCabin
                                showMeme = true
                            }
                        )
                    }
                    selectedType == MemoType.Ideas -> Box(modifier = Modifier.fillMaxSize()) {
                        HomeScreen(
                            memos = memos,
                            onEdit = { editing = it },
                            onDelete = { vm.delete(it) },
                            onToggleComplete = { vm.toggleTaskComplete(it) },
                            onOpenSettings = { showSettings = true },
                            onOpenDrawer = { drawerOpen = true },
                            taskBreakdownEnabled = aiSettings.taskBreakdownEnabled,
                            brainstormEnabled = aiSettings.brainstormEnabled,
                            wallpaper = homeWallpaperBitmap
                        )
                        CategoryScreen(
                            type = MemoType.Ideas,
                            memos = memos,
                            query = query,
                            onQuery = vm::setSearchQuery,
                            onBack = { selectedType = null },
                            onDelete = { vm.delete(it) },
                            onEdit = { editing = it },
                            taskBreakdownEnabled = aiSettings.taskBreakdownEnabled,
                            brainstormEnabled = aiSettings.brainstormEnabled,
                            wallpaper = categoryWallpaperBitmap
                        )
                    }
                }

                if (showMeme && editing == null) {
                    HomeZoneScreen(
                        subTab = homeSubTab,
                        onBack = {
                            showMeme = false
                            selectedType = null
                        }
                    )
                }
                if (busy) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF2563EB))
                    }
                }
            }
        }

        if (showOemSetup) {
            OemReminderSetupDialog(
                onDismiss = {
                    ReminderPermissionHelper.markOemSetupPrompted(prefs)
                    showOemSetup = false
                },
                onOpenAutostart = {
                    val opened = ReminderPermissionHelper.openOemBackgroundSettings(context)
                    if (opened) {
                        ReminderPermissionHelper.markOemSetupPrompted(prefs)
                        showOemSetup = false
                    }
                },
                onOpenBattery = {
                    val opened = ReminderPermissionHelper.requestIgnoreBatteryOptimizations(context)
                    if (opened) {
                        ReminderPermissionHelper.markOemSetupPrompted(prefs)
                        showOemSetup = false
                    }
                }
            )
        }

        if (showSettings) {
            SettingsScreen(
                initial = aiSettings,
                autoDeleteCompleted = autoDeleteCompleted,
                lockScreenTodoEnabled = lockScreenTodoEnabled,
                smartItineraryEnabled = smartItineraryEnabled,
                onDismiss = { showSettings = false },
                onSave = { cloud, key, model, taskBreakdown, brainstorm, autoDelete, lockScreenTodo, smartItinerary ->
                    vm.updateAiSettings(cloud, key, model, taskBreakdown, brainstorm)
                    vm.updateAutoDeleteCompleted(autoDelete)
                    vm.updateLockScreenTodoEnabled(lockScreenTodo)
                    vm.updateSmartItineraryEnabled(smartItinerary)
                    showSettings = false
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
            )
        }

        if (showWallpaper) {
            WallpaperSettingsDialog(
                homeWallpaperUri = homeWallpaperUri,
                categoryWallpaperUri = categoryWallpaperUri,
                onDismiss = { showWallpaper = false },
                onUpdate = { home, category ->
                    homeWallpaperUri = home
                    categoryWallpaperUri = category
                    savePrefsUri(prefs, "wallpaper_home_uri", home)
                    savePrefsUri(prefs, "wallpaper_category_uri", category)
                }
            )
        }

        reminderConfirmation?.let { pending ->
            ReminderConfirmDialog(
                confirmation = pending,
                onConfirm = { mode -> vm.confirmReminder(mode) },
                onDismiss = { vm.cancelReminderConfirmation() }
            )
        }

        smartItinerarySetup?.let { setup ->
            SmartItinerarySetupDialog(
                setup = setup,
                onAccept = { vm.respondToSmartItinerarySetup(acceptSuggestion = true) },
                onKeep = { vm.respondToSmartItinerarySetup(acceptSuggestion = false) },
                onDismiss = { vm.cancelSmartItinerarySetup() }
            )
        }

        smartItineraryFollowUp?.let { followUp ->
            SmartItineraryFollowUpDialog(
                prompt = followUp,
                onAccept = { vm.respondToSmartItineraryFollowUp(acceptSuggestion = true) },
                onDismissPrompt = { vm.dismissSmartItineraryFollowUp() }
            )
        }

        if (showNotificationPrompt) {
            NotificationPermissionPrompt(
                onDismiss = {
                    prefs.edit().putBoolean("notification_prompt_shown", true).apply()
                    showNotificationPrompt = false
                },
                onAllow = {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            )
        }

        if (drawerOpen && selectedType == null && editing == null) {
            HomeSideDrawer(
                visible = true,
                memos = memos,
                onDismiss = { drawerOpen = false },
                onExportJson = { vm.buildExportJson(memos) }
            )
        }

        stepReward?.let { reward ->
            StepCompleteCelebration(
                shellCount = reward.shellCount,
                onDismiss = { vm.dismissStepReward() },
                modifier = Modifier.zIndex(300f)
            )
        }
        }
    }
}

@Composable
private fun SmartItinerarySetupDialog(
    setup: SmartItinerarySetupPrompt,
    onAccept: () -> Unit,
    onKeep: () -> Unit,
    onDismiss: () -> Unit
) {
    val timeFormatter = remember {
        SimpleDateFormat("HH:mm", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }
    }
    val defaultRemind = remember(setup.remindAtMillis) {
        timeFormatter.format(Date(setup.remindAtMillis))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认行程提醒", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("原文", fontSize = 12.sp, color = Color(0xFF64748B))
                Text(setup.userInput, fontSize = 14.sp, lineHeight = 20.sp)

                Text("当前提醒", fontSize = 12.sp, color = Color(0xFF64748B))
                Text(
                    text = "${setup.timeText}  →  $defaultRemind",
                    fontSize = 14.sp,
                    color = Color(0xFF15803D),
                    fontWeight = FontWeight.Medium
                )

                Text("智能建议", fontSize = 12.sp, color = Color(0xFF64748B))
                Text(
                    text = "按你现在位置，前往${setup.destination}建议 ${setup.suggestedDepartText} 出发，要改吗？",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF334155)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("采用建议（${setup.suggestedDepartText}）", color = Color(0xFF2563EB))
            }
        },
        dismissButton = {
            TextButton(onClick = onKeep) {
                Text("保持原时间", color = Color(0xFF64748B))
            }
        }
    )
}

@Composable
private fun SmartItineraryFollowUpDialog(
    prompt: SmartItineraryFollowUpPrompt,
    onAccept: () -> Unit,
    onDismissPrompt: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissPrompt,
        title = { Text("智能行程更新", fontWeight = FontWeight.Bold) },
        text = {
            Text(prompt.message, fontSize = 14.sp, lineHeight = 20.sp)
        },
        confirmButton = {
            if (prompt.suggestedRemindText != null) {
                TextButton(onClick = onAccept) {
                    Text("改为 ${prompt.suggestedRemindText}", color = Color(0xFF2563EB))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissPrompt) {
                Text("不用", color = Color(0xFF64748B))
            }
        }
    )
}

@Composable
private fun ReminderConfirmDialog(
    confirmation: ReminderConfirmation,
    onConfirm: (ReminderMode) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(ReminderMode.OVERLAY) }
    val context = LocalContext.current
    val hasNotificationPermission = remember {
        ReminderPermissionHelper.hasNotificationPermission(context)
    }
    val timeFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }
    }
    val formattedTime = remember(confirmation.remindAtMillis) {
        timeFormatter.format(Date(confirmation.remindAtMillis))
    }
    val isExpired = confirmation.remindAtMillis <= System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认创建提醒", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("原文", fontSize = 12.sp, color = Color(0xFF64748B))
                Text(confirmation.userInput, fontSize = 14.sp, lineHeight = 20.sp)

                Text("识别时间", fontSize = 12.sp, color = Color(0xFF64748B))
                Text(
                    text = if (confirmation.timeText.isNotBlank()) {
                        "${confirmation.timeText}  →  $formattedTime"
                    } else {
                        formattedTime
                    },
                    fontSize = 14.sp,
                    color = Color(0xFF15803D),
                    fontWeight = FontWeight.Medium
                )

                if (isExpired) {
                    Text(
                        "提醒时间已过，请重新输入",
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626)
                    )
                }

                Text("提醒方式", fontSize = 12.sp, color = Color(0xFF64748B))
                ReminderModeOption(
                    title = "🔔 闹钟模式",
                    subtitle = "响铃 + 震动，适合重要事项",
                    selected = selectedMode == ReminderMode.ALARM,
                    onClick = { selectedMode = ReminderMode.ALARM }
                )
                ReminderModeOption(
                    title = "💬 弹窗模式",
                    subtitle = "不响铃，以系统通知横幅弹出（应用外可见）",
                    selected = selectedMode == ReminderMode.OVERLAY,
                    onClick = { selectedMode = ReminderMode.OVERLAY }
                )
                if (selectedMode == ReminderMode.OVERLAY && !hasNotificationPermission) {
                    Text(
                        "需要开启「通知」权限，到点后才能在应用外弹出提醒",
                        fontSize = 12.sp,
                        color = Color(0xFFD97706),
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedMode) },
                enabled = !isExpired
            ) {
                Text("确认创建", color = if (isExpired) Color.Gray else Color(0xFF2563EB))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF64748B))
            }
        }
    )
}

@Composable
private fun ReminderModeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
    val backgroundColor = if (selected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B), lineHeight = 16.sp)
        }
    }
}

@Composable
private fun NotificationPermissionPrompt(
    onDismiss: () -> Unit,
    onAllow: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(SettingsUi.CardRadius),
            colors = CardDefaults.cardColors(containerColor = SettingsUi.CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(SettingsUi.BgStart, SettingsUi.BgEnd)))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFD6EBFF), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = SettingsUi.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "开启消息通知",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SettingsUi.Title
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Memo 需要通知权限，才能在待办提醒到点时及时提醒你。",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = SettingsUi.Secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "请在系统弹窗中选择「允许」，或在设置中开启 Memo 的通知权限。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = SettingsUi.Secondary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("稍后", color = SettingsUi.Primary, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onAllow,
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsUi.Primary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("去开启", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun OemReminderSetupDialog(
    onDismiss: () -> Unit,
    onOpenAutostart: () -> Unit,
    onOpenBattery: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(SettingsUi.CardRadius),
            colors = CardDefaults.cardColors(containerColor = SettingsUi.CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(SettingsUi.BgStart, SettingsUi.BgEnd)))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFD6EBFF), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.PlayCircle,
                            contentDescription = null,
                            tint = SettingsUi.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "开启后台权限",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SettingsUi.Title
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "检测到 ${com.memoai.app.oem.OemDistributionConfig.current.displayName} 手机，需要额外开启自启动和后台运行权限，否则到点提醒可能无法及时送达。",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = SettingsUi.Secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "建议依次开启：自启动 / 后台活动 → 关闭省电限制 → 允许通知横幅。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = SettingsUi.Secondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onOpenBattery,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("关闭省电限制", color = SettingsUi.Primary, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("稍后", color = SettingsUi.Primary, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onOpenAutostart,
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsUi.Primary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("去开启", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteMemoPrompt(
    summary: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(SettingsUi.CardRadius),
                colors = CardDefaults.cardColors(containerColor = SettingsUi.CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.verticalGradient(listOf(SettingsUi.BgStart, SettingsUi.BgEnd)))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFD6EBFF), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = SettingsUi.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "删除这张卡片？",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SettingsUi.Title
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = summary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = SettingsUi.Secondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "删除后无法恢复，请确认是否继续。",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = SettingsUi.Secondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("取消", color = SettingsUi.Primary, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("删除", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

private object SettingsUi {
    val Primary = Color(0xFF1296DB)
    val BgStart = Color(0xFFE5F8F6)
    val BgEnd = Color(0xFFCBE7FA)
    val CardBg = Color.White
    val Title = Color.Black
    val Secondary = Color(0xFF6E7071)
    val Placeholder = Color(0x59000000)
    val InputBg = Color(0xFFECF1F5)
    val InputBorder = Color(0x4F000000)
    val LabelGradientTop = Color(0xFFF3F8FB)
    val LabelGradientBottom = Color(0xFFECF1F5)
    val ItemGradientStart = Color(0xFFE0E7F1)
    val ItemGradientEnd = Color(0xFFD1EBF9)
    val SectionBackdrop = Color(0x33F5FAFA)
    val CardRadius = 24.dp
    val ModelFieldRadius = 11.dp
    val KeyFieldRadius = 24.dp
    val ItemRadius = 200.dp
    val CloudCardElevation = 8.dp
    val TaskCardElevation = 4.dp
    val SystemCardElevation = 10.dp
    val BottomBarElevation = 12.dp
}

private val SettingsShadowColor = Color(0x40000000)

private fun Modifier.settingsCardShadow(shape: Shape, elevation: androidx.compose.ui.unit.Dp): Modifier =
    shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = SettingsShadowColor,
        spotColor = SettingsShadowColor
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    initial: AiSettings,
    autoDeleteCompleted: Boolean,
    lockScreenTodoEnabled: Boolean,
    smartItineraryEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (Boolean, String, String, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val horizontalPadding = if (screenWidthDp < 400) 12.dp else 16.dp
    var cloud by remember(initial) { mutableStateOf(initial.useCloud) }
    var key by remember(initial) { mutableStateOf(initial.apiKey) }
    var model by remember(initial) { mutableStateOf(initial.model) }
    val buildConfigKey = remember { BuildConfig.DEEPSEEK_API_KEY.trim() }
    var autoDelete by remember(autoDeleteCompleted) { mutableStateOf(autoDeleteCompleted) }
    var lockScreenTodo by remember(lockScreenTodoEnabled) { mutableStateOf(lockScreenTodoEnabled) }
    var smartItinerary by remember(smartItineraryEnabled) { mutableStateOf(smartItineraryEnabled) }
    var taskBreakdown by remember(initial) { mutableStateOf(initial.taskBreakdownEnabled) }
    var brainstorm by remember(initial) { mutableStateOf(initial.brainstormEnabled) }
    var keyVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val switchColors = SwitchDefaults.colors(
        checkedTrackColor = SettingsUi.Primary,
        checkedThumbColor = Color.White,
        uncheckedTrackColor = Color(0xFFD1D5DB),
        uncheckedThumbColor = Color.White
    )
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = SettingsUi.InputBg,
        unfocusedContainerColor = SettingsUi.InputBg,
        disabledContainerColor = SettingsUi.InputBg,
        focusedBorderColor = SettingsUi.InputBorder,
        unfocusedBorderColor = SettingsUi.InputBorder,
        focusedTextColor = SettingsUi.Title,
        unfocusedTextColor = SettingsUi.Title
    )
    val cardShape = RoundedCornerShape(SettingsUi.CardRadius)
    val pillShape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(SettingsUi.BgStart, SettingsUi.BgEnd),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(1000f, 1800f)
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = horizontalPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = SettingsUi.Secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "设置",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SettingsUi.Title
                    )
                }

                SettingsSectionLabel("云端服务")
                SettingsCard(cardShape, SettingsUi.CloudCardElevation) {
                    SettingsToggleRow(
                        label = "启用Deepseek Api",
                        checked = cloud,
                        onCheckedChange = { cloud = it },
                        switchColors = switchColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Model", fontSize = 12.sp, color = SettingsUi.Secondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        singleLine = true,
                        shape = RoundedCornerShape(SettingsUi.ModelFieldRadius),
                        colors = fieldColors,
                        textStyle = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        placeholder = {
                            Text(
                                if (buildConfigKey.isNotBlank()) "已从 local.properties 读取" else "Deepseek API KEY",
                                color = SettingsUi.Placeholder,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        visualTransformation = if (keyVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        shape = RoundedCornerShape(SettingsUi.KeyFieldRadius),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { keyVisible = !keyVisible }) {
                            Text(
                                if (keyVisible) "隐藏 Key" else "显示 Key",
                                fontSize = 12.sp,
                                color = SettingsUi.Primary
                            )
                        }
                        TextButton(
                            onClick = {
                                if (key.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Deepseek API KEY", key))
                                    Toast.makeText(context, "已复制 API Key", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("复制 Key", fontSize = 12.sp, color = SettingsUi.Primary)
                        }
                    }
                    Text(
                        text = "默认优先调用 DeepSeek；网络异常时自动回退本地模型。API Key 请写在 local.properties（不会提交到 Git）。",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = SettingsUi.Secondary.copy(alpha = 0.76f)
                    )
                }

                SettingsSectionLabel("内容生成", Icons.Outlined.FormatListBulleted)
                SettingsCard(cardShape, SettingsUi.TaskCardElevation) {
                    SettingsToggleWithDescription(
                        label = "任务拆解",
                        description = "关闭后，Tasks 卡片只显示你的原始内容，不再自动展开为步骤。",
                        checked = taskBreakdown,
                        onCheckedChange = { taskBreakdown = it },
                        switchColors = switchColors
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsToggleWithDescription(
                        label = "AI脑风暴",
                        description = "关闭后，Ideas 卡片只显示你的原始内容，不再自动生成脑风暴条目。",
                        checked = brainstorm,
                        onCheckedChange = { brainstorm = it },
                        switchColors = switchColors
                    )
                }

                SettingsSectionLabel("任务与提醒", Icons.Outlined.FormatListBulleted)
                SettingsCard(cardShape, SettingsUi.TaskCardElevation) {
                    SettingsToggleWithDescription(
                        label = "智能行程",
                        description = "开启后，云端 API 连接时会根据位置与路况动态优化出发提醒；关闭则直接选择闹钟/弹窗提醒。",
                        checked = smartItinerary,
                        onCheckedChange = { smartItinerary = it },
                        switchColors = switchColors
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsToggleWithDescription(
                        label = "自动删除已完成的任务",
                        description = "不开启时，已完成任务会按时间继续保留在列表中。",
                        checked = autoDelete,
                        onCheckedChange = { autoDelete = it },
                        switchColors = switchColors
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsToggleWithDescription(
                        label = "锁屏显示待办清单",
                        description = "Apple 风格清单，点击后解锁并打开 Tasks。",
                        checked = lockScreenTodo,
                        onCheckedChange = { lockScreenTodo = it },
                        switchColors = switchColors
                    )
                }

                SettingsCard(cardShape, SettingsUi.SystemCardElevation) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsSystemRow(
                            icon = Icons.Outlined.PlayCircle,
                            title = "开机自启动 / 后台活动",
                            pillShape = pillShape,
                            onClick = { ReminderPermissionHelper.openOemBackgroundSettings(context) }
                        )
                        SettingsSystemRow(
                            icon = Icons.Outlined.BatteryChargingFull,
                            title = "关闭省电限制",
                            pillShape = pillShape,
                            onClick = { ReminderPermissionHelper.requestIgnoreBatteryOptimizations(context) }
                        )
                        SettingsSystemRow(
                            icon = Icons.Outlined.Notifications,
                            title = "通知设置",
                            pillShape = pillShape,
                            onClick = { ReminderPermissionHelper.openNotificationSettings(context) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = horizontalPadding, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = SettingsUi.Primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = { onSave(cloud, key, model, taskBreakdown, brainstorm, autoDelete, lockScreenTodo, smartItinerary) },
                    colors = ButtonDefaults.buttonColors(containerColor = SettingsUi.Primary),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("保存", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = SettingsUi.Secondary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text = text, fontSize = 12.sp, color = SettingsUi.Secondary)
    }
}

@Composable
private fun SettingsToggleWithDescription(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchColors: androidx.compose.material3.SwitchColors
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        SettingsToggleRow(
            label = label,
            checked = checked,
            onCheckedChange = onCheckedChange,
            switchColors = switchColors
        )
        Text(
            text = description,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = SettingsUi.Secondary.copy(alpha = 0.76f)
        )
    }
}

@Composable
private fun SettingsCard(
    shape: Shape,
    elevation: androidx.compose.ui.unit.Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardPadding = if (LocalConfiguration.current.screenWidthDp < 400) 12.dp else 14.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .settingsCardShadow(shape, elevation)
            .clip(shape)
            .background(SettingsUi.CardBg)
            .padding(horizontal = cardPadding, vertical = cardPadding),
        content = content
    )
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchColors: androidx.compose.material3.SwitchColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = SettingsUi.Title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = switchColors,
            modifier = Modifier.widthIn(min = 52.dp)
        )
    }
}

@Composable
private fun SettingsSystemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    pillShape: Shape,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(pillShape)
            .background(
                Brush.horizontalGradient(
                    listOf(SettingsUi.ItemGradientStart, SettingsUi.ItemGradientEnd)
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SettingsUi.Primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = SettingsUi.Primary, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            color = SettingsUi.Title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = SettingsUi.Secondary,
            modifier = Modifier.size(22.dp)
        )
    }
}

private object CategoryUi {
    val Bg = Color(0xFFF1F5FB)
    val BgTop = Color(0xFFEBF5FF)
    val BgBottom = Color(0xFFF5F7FA)
    val TopBar = Color(0xFFDFF1FD)
    val IconChipBg = Color(0xFFD8E6F2)
    val CardRadius = 27.dp
    val CardElevation = 12.dp
    val BubbleElevation = 6.dp
    val SearchBg = Color(0xFFF9FAFC)
}

private object HomeUi {
    val BgTop = Color(0xFFEBF5FF)
    val BgBottom = Color(0xFFF5F7FA)
    val TopBar = Color(0xFFEBF5FF)
    val Title = Color(0xFF1A1C1E)
    val Primary = Color(0xFF2962FF)
    val NavInactive = Color(0xFF90A4AE)
    val InputBg = Color(0xFFF1F3F4)
    val Placeholder = Color(0xFF9E9E9E)
    val Timestamp = Color(0xFF9E9E9E)
    val EditIcon = Color(0xFF9E9E9E)
    val CardRadius = 20.dp
    val BubbleRadius = 12.dp
    val ContentPaddingH = 16.dp
    val GroupSpacing = 12.dp
    val BubbleToCard = 8.dp
    val BubbleShadowReserve = 4.dp
    val CardElevation = 12.dp
    val BubbleElevation = CategoryUi.BubbleElevation
}

private fun Modifier.pageGradientBackground(): Modifier =
    background(Brush.verticalGradient(listOf(CategoryUi.BgTop, CategoryUi.BgBottom)))

private fun Modifier.memoCardShadow(shape: Shape, homeStyle: Boolean): Modifier {
    val elevation = if (homeStyle) HomeUi.CardElevation else CategoryUi.CardElevation
    val shadowColor = Color(0x40000000)
    return shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = shadowColor,
        spotColor = shadowColor
    )
}

private fun Modifier.bubbleShadow(shape: Shape): Modifier =
    shadow(
        elevation = HomeUi.BubbleElevation,
        shape = shape,
        clip = true,
        ambientColor = Color(0x26000000),
        spotColor = Color(0x26000000)
    )

private fun Modifier.memoDropShadow(shape: Shape): Modifier =
    memoCardShadow(shape, homeStyle = true)

private fun memoDisplayContent(
    memo: MemoUi,
    taskBreakdownEnabled: Boolean,
    brainstormEnabled: Boolean
): String = when (memo.type) {
    MemoType.Tasks -> if (taskBreakdownEnabled) memo.content else memo.userInput
    MemoType.Ideas -> if (brainstormEnabled) memo.content else memo.userInput
}

private fun memoEditableContent(
    memo: MemoUi,
    taskBreakdownEnabled: Boolean,
    brainstormEnabled: Boolean
): String {
    val source = memoDisplayContent(memo, taskBreakdownEnabled, brainstormEnabled)
    return when (memo.type) {
        MemoType.Tasks -> if (taskBreakdownEnabled) CardContentUtils.formatTaskContentForDisplay(source) else source
        MemoType.Ideas -> if (brainstormEnabled) CardContentUtils.formatIdeaContentForDisplay(source) else source
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    memos: List<MemoUi>,
    onEdit: (MemoUi) -> Unit,
    onDelete: (MemoUi) -> Unit,
    onToggleComplete: (MemoUi) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDrawer: () -> Unit,
    taskBreakdownEnabled: Boolean,
    brainstormEnabled: Boolean,
    wallpaper: androidx.compose.ui.graphics.ImageBitmap?
) {
    var pendingDelete by remember { mutableStateOf<MemoUi?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(memos.firstOrNull()?.id, memos.size) {
        if (memos.isEmpty()) return@LaunchedEffect
        delay(32)
        if (listState.layoutInfo.totalItemsCount == 0) return@LaunchedEffect
        listState.animateScrollToItem(0)
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val homeTopBarTotalHeight = statusBarTop + 64.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pageGradientBackground()
    ) {
        if (wallpaper != null) {
            Image(
                bitmap = wallpaper,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.18f,
                modifier = Modifier.fillMaxSize().padding(top = homeTopBarTotalHeight)
            )
        }
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        "Memo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeUi.Title
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Outlined.FormatListBulleted,
                            contentDescription = "功能菜单",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HomeUi.TopBar,
                    titleContentColor = HomeUi.Title
                ),
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "设置",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = HomeUi.ContentPaddingH),
                    verticalArrangement = Arrangement.spacedBy(HomeUi.GroupSpacing),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(memos, key = { it.id }) { memo ->
                        MemoBubbleGroup(
                            memo = memo,
                            onEdit = { onEdit(memo) },
                            onDeleteRequest = { pendingDelete = it },
                            onToggleComplete = onToggleComplete,
                            homeStyle = true,
                            taskBreakdownEnabled = taskBreakdownEnabled,
                            brainstormEnabled = brainstormEnabled
                        )
                    }
                }
            }
        }
    }

    if (pendingDelete != null) {
        val item = pendingDelete!!
        DeleteMemoPrompt(
            summary = item.summary,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                onDelete(item)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun HomeTextBubble(text: String) {
    val maxBubbleWidth = (LocalConfiguration.current.screenWidthDp * 0.85f).dp
    val shape = RoundedCornerShape(
        topStart = HomeUi.BubbleRadius,
        topEnd = HomeUi.BubbleRadius,
        bottomStart = HomeUi.BubbleRadius,
        bottomEnd = 2.dp
    )
    Text(
        text = text,
        color = Color.White,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = Modifier
            .widthIn(max = maxBubbleWidth)
            .bubbleShadow(shape)
            .background(HomeUi.Primary, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

@Composable
private fun MemoBubbleGroup(
    memo: MemoUi,
    onEdit: () -> Unit,
    onDeleteRequest: (MemoUi) -> Unit,
    modifier: Modifier = Modifier,
    homeStyle: Boolean = false,
    showUserBubble: Boolean = true,
    onToggleComplete: ((MemoUi) -> Unit)? = null,
    taskBreakdownEnabled: Boolean = true,
    brainstormEnabled: Boolean = true
) {
    Column(modifier = modifier) {
        if (showUserBubble) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = HomeUi.BubbleShadowReserve),
                horizontalArrangement = Arrangement.End
            ) {
                HomeTextBubble(text = memo.userInput)
            }
            Spacer(modifier = Modifier.height(HomeUi.BubbleToCard))
        }
        MemoCard(
            memo = memo,
            onEdit = onEdit,
            onDeleteRequest = onDeleteRequest,
            homeStyle = homeStyle,
            onToggleComplete = onToggleComplete,
            taskBreakdownEnabled = taskBreakdownEnabled,
            brainstormEnabled = brainstormEnabled
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTopBar(
    title: String,
    onBack: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            Box(
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Text("返回", color = HomeUi.Title, fontSize = 16.sp)
            }
        },
        title = { },
        actions = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HomeUi.Title,
                modifier = Modifier.padding(end = 4.dp)
            )
        },
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CategoryUi.TopBar,
            titleContentColor = HomeUi.Title
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksScreen(
    memos: List<MemoUi>,
    query: String,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    onDelete: (MemoUi) -> Unit,
    onEdit: (MemoUi) -> Unit,
    onToggleComplete: (MemoUi) -> Unit,
    onToggleStepComplete: (MemoUi, Int) -> Unit,
    taskBreakdownEnabled: Boolean,
    brainstormEnabled: Boolean,
    cabinUnlockEvent: CabinUnlockEvent? = null,
    onDismissCabinUnlock: () -> Unit = {},
    onOpenCabin: () -> Unit = {}
) {
    var inputText by remember(query) { mutableStateOf(query) }
    var submittedQuery by remember { mutableStateOf("") }
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val animationScope = rememberCoroutineScope()
    val dragAnim = remember { Animatable(0f) }
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    var pendingDelete by remember { mutableStateOf<MemoUi?>(null) }

    fun submitSearch() {
        submittedQuery = inputText.trim()
        onQuery(inputText)
    }

    val filtered = memos.filter {
        if (it.type != MemoType.Tasks) return@filter false
        if (submittedQuery.isBlank()) return@filter true
        it.summary.contains(submittedQuery, ignoreCase = true) ||
            memoDisplayContent(it, taskBreakdownEnabled, brainstormEnabled).contains(submittedQuery, ignoreCase = true)
    }

    val navigateBack: () -> Unit = {
        animationScope.launch {
            dragAnim.animateTo(
                screenWidthPx,
                tween(durationMillis = 140, easing = FastOutSlowInEasing)
            )
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pageGradientBackground()
            .graphicsLayer { translationX = dragAnim.value.coerceAtLeast(0f) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        val next = (dragAnim.value + dragAmount).coerceIn(0f, screenWidthPx)
                        animationScope.launch { dragAnim.snapTo(next) }
                    },
                    onDragEnd = {
                        val thresholdPx = with(density) { 50.dp.toPx() }
                        if (dragAnim.value > thresholdPx) {
                            animationScope.launch {
                                dragAnim.animateTo(
                                    screenWidthPx,
                                    tween(durationMillis = 140, easing = FastOutSlowInEasing)
                                )
                                onBack()
                            }
                        } else {
                            animationScope.launch {
                                dragAnim.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
                            }
                        }
                    },
                    onDragCancel = {
                        animationScope.launch {
                            dragAnim.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
                        }
                    }
                )
            }
    ) {
        CategoryTopBar(title = "Tasks", onBack = navigateBack)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 14.dp, end = 14.dp)
                .height(50.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(14.dp),
                    clip = false,
                    ambientColor = Color(0x26000000),
                    spotColor = Color(0x26000000)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(CategoryUi.SearchBg)
        ) {
            TextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    if (it.isBlank()) {
                        submittedQuery = ""
                        onQuery("")
                    }
                },
                leadingIcon = {
                    IconButton(
                        onClick = { submitSearch() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "搜索",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                placeholder = {
                    Text(
                        "在 Tasks 中搜索……",
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = Color(0xFF4A4A4A)
                    )
                },
                textStyle = TextStyle(fontSize = 11.sp, lineHeight = 14.sp),
                singleLine = true,
                maxLines = 1,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
            ) {
                items(filtered, key = { it.id }) { memo ->
                    MemoCard(
                        memo = memo,
                        onEdit = { onEdit(memo) },
                        onDeleteRequest = { pendingDelete = it },
                        onToggleComplete = onToggleComplete,
                        onToggleStepComplete = onToggleStepComplete,
                        taskBreakdownEnabled = taskBreakdownEnabled,
                        brainstormEnabled = brainstormEnabled,
                        modifier = Modifier.padding(horizontal = HomeUi.ContentPaddingH)
                    )
                }
            }
            if (cabinUnlockEvent != null) {
                CabinUnlockPopup(
                    event = cabinUnlockEvent,
                    onDismiss = onDismissCabinUnlock,
                    onGoToCabin = onOpenCabin,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 8.dp)
                )
            }
        }
    }

    if (pendingDelete != null) {
        val item = pendingDelete!!
        DeleteMemoPrompt(
            summary = item.summary,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                onDelete(item)
                pendingDelete = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryScreen(
    type: MemoType,
    memos: List<MemoUi>,
    query: String,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    onDelete: (MemoUi) -> Unit,
    onEdit: (MemoUi) -> Unit,
    taskBreakdownEnabled: Boolean,
    brainstormEnabled: Boolean,
    wallpaper: androidx.compose.ui.graphics.ImageBitmap?
) {
    var inputText by remember(type, query) { mutableStateOf(query) }
    var submittedQuery by remember(type) { mutableStateOf("") }
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val animationScope = rememberCoroutineScope()
    val dragAnim = remember(type) { Animatable(0f) }
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    var pendingDelete by remember(type) { mutableStateOf<MemoUi?>(null) }

    fun submitSearch() {
        submittedQuery = inputText.trim()
        onQuery(inputText)
    }

    val filtered = memos.filter {
        val inType = it.type == type
        if (!inType) return@filter false
        if (submittedQuery.isBlank()) return@filter true
        it.summary.contains(submittedQuery, ignoreCase = true) ||
            memoDisplayContent(it, taskBreakdownEnabled, brainstormEnabled).contains(submittedQuery, ignoreCase = true)
    }

    val navigateBack: () -> Unit = {
        animationScope.launch {
            dragAnim.animateTo(
                screenWidthPx,
                tween(durationMillis = 140, easing = FastOutSlowInEasing)
            )
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pageGradientBackground()
            .graphicsLayer { translationX = dragAnim.value.coerceAtLeast(0f) }
            .pointerInput(type) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        val next = (dragAnim.value + dragAmount).coerceIn(0f, screenWidthPx)
                        animationScope.launch {
                            dragAnim.snapTo(next)
                        }
                    },
                    onDragEnd = {
                        val thresholdPx = with(density) { 50.dp.toPx() }
                        if (dragAnim.value > thresholdPx) {
                            animationScope.launch {
                                dragAnim.animateTo(
                                    screenWidthPx,
                                    tween(durationMillis = 140, easing = FastOutSlowInEasing)
                                )
                                onBack()
                            }
                        } else {
                            animationScope.launch {
                                dragAnim.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
                            }
                        }
                    },
                    onDragCancel = {
                        animationScope.launch {
                            dragAnim.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
                        }
                    }
                )
            }
    ) {
        CategoryTopBar(title = type.name, onBack = navigateBack)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 14.dp, end = 14.dp)
                .height(50.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(14.dp),
                    clip = false,
                    ambientColor = Color(0x26000000),
                    spotColor = Color(0x26000000)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(CategoryUi.SearchBg)
        ) {
            TextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    if (it.isBlank()) {
                        submittedQuery = ""
                        onQuery("")
                    }
                },
                leadingIcon = {
                    IconButton(
                        onClick = { submitSearch() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "搜索",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                placeholder = {
                    Text("在 ${type.name} 中搜索……", fontSize = 11.sp, lineHeight = 14.sp, color = Color(0xFF4A4A4A))
                },
                textStyle = TextStyle(fontSize = 11.sp, lineHeight = 14.sp),
                singleLine = true,
                maxLines = 1,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxSize()) {
            if (wallpaper != null) {
                Image(
                    bitmap = wallpaper,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.22f,
                    modifier = Modifier.matchParentSize()
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
            ) {
                items(filtered, key = { it.id }) { memo ->
                    MemoCard(
                        memo = memo,
                        onEdit = { onEdit(memo) },
                        onDeleteRequest = { pendingDelete = it },
                        taskBreakdownEnabled = taskBreakdownEnabled,
                        brainstormEnabled = brainstormEnabled,
                        modifier = Modifier.padding(horizontal = HomeUi.ContentPaddingH)
                    )
                }
            }
        }
    }

    if (pendingDelete != null) {
        val item = pendingDelete!!
        DeleteMemoPrompt(
            summary = item.summary,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                onDelete(item)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun MemoCard(
    memo: MemoUi,
    onEdit: () -> Unit,
    onDeleteRequest: (MemoUi) -> Unit,
    modifier: Modifier = Modifier,
    homeStyle: Boolean = false,
    onToggleComplete: ((MemoUi) -> Unit)? = null,
    onToggleStepComplete: ((MemoUi, Int) -> Unit)? = null,
    taskBreakdownEnabled: Boolean = true,
    brainstormEnabled: Boolean = true
) {
    var expanded by remember(memo.id) { mutableStateOf(false) }
    val isCompletedTask = memo.type == MemoType.Tasks && memo.completed
    val displayContent = remember(memo, taskBreakdownEnabled, brainstormEnabled) {
        memoDisplayContent(
            memo = memo,
            taskBreakdownEnabled = taskBreakdownEnabled,
            brainstormEnabled = brainstormEnabled
        )
    }
    val hasExpandableContent = displayContent.trim().isNotBlank()
    val showStatusExtras = homeStyle || expanded
    val badge = when {
        homeStyle && memo.type == MemoType.Tasks -> Color(0xFFE8F5E9) to Color(0xFF4CAF50)
        homeStyle && memo.type == MemoType.Ideas -> Color(0xFFFFF9C4) to Color(0xFFFBC02D)
        memo.type == MemoType.Ideas -> Color(0xFFFEF9C3) to Color(0xFFA16207)
        memo.type == MemoType.Tasks -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        else -> Color(0xFFFEF9C3) to Color(0xFFA16207)
    }
    val cardRadius = if (homeStyle) HomeUi.CardRadius else CategoryUi.CardRadius
    val cardShape = RoundedCornerShape(cardRadius)
    val contentPadding = if (homeStyle) PaddingValues(16.dp) else PaddingValues(14.dp)
    val summaryFontSize = if (homeStyle) 14.sp else 12.sp
    val timestampColor = if (isCompletedTask) Color(0xFF94A3B8) else if (homeStyle) HomeUi.Timestamp else Color(0xFF94A3B8)
    val editTint = if (isCompletedTask) Color(0xFFB0B8C1) else if (homeStyle) HomeUi.EditIcon else Color(0xFFCBD5E1)
    val titleColor = if (isCompletedTask) {
        Color(0xFF94A3B8)
    } else if (homeStyle) {
        HomeUi.Title
    } else {
        Color(0xFF0F172A)
    }
    val cardBg = if (isCompletedTask) Color(0xFFF5F7FA) else Color.White
    val bodyColor = if (isCompletedTask) Color(0xFF94A3B8) else Color(0xFF334155)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .memoCardShadow(cardShape, homeStyle)
            .clip(cardShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (hasExpandableContent) expanded = !expanded },
                onLongClick = { onDeleteRequest(memo) }
            ),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = memo.type.name.uppercase(Locale.getDefault()),
                    color = badge.second,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.background(badge.first, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(text = memo.timestamp, color = timestampColor, fontSize = 10.sp)
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = editTint, modifier = Modifier.size(16.dp))
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (homeStyle) 10.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = memo.summary.ifBlank { memo.userInput },
                    fontSize = summaryFontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    textDecoration = if (isCompletedTask) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )
                if (memo.type == MemoType.Tasks && onToggleComplete != null && (homeStyle || expanded)) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clickable { onToggleComplete(memo) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCompletedTask) {
                                Icons.Outlined.CheckCircle
                            } else {
                                Icons.Outlined.RadioButtonUnchecked
                            },
                            contentDescription = if (isCompletedTask) "标记未完成" else "标记完成",
                            tint = if (isCompletedTask) Color(0xFF94A3B8) else Color(0xFFC7C7CC),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else if (!homeStyle) {
                    Spacer(modifier = Modifier.width(30.dp))
                }
            }
            if (showStatusExtras && isCompletedTask) {
                Text(
                    text = "✅ 已完成",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (showStatusExtras && onToggleComplete != null && memo.type == MemoType.Tasks && !memo.timeText.isNullOrBlank()) {
                val modeLabel = when (memo.reminderMode) {
                    ReminderMode.ALARM -> "🔔"
                    ReminderMode.OVERLAY -> "💬"
                    null -> "⏰"
                }
                Text(
                    text = "$modeLabel ${memo.timeText}",
                    color = Color(0xFF15803D),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (expanded && hasExpandableContent) {
                when (memo.type) {
                    MemoType.Tasks -> {
                        val steps = if (taskBreakdownEnabled) {
                            CardContentUtils.parseTaskSteps(displayContent)
                        } else {
                            emptyList()
                        }
                        if (steps.isNotEmpty()) {
                            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                steps.forEachIndexed { index, step ->
                                    val stepDone = CardContentUtils.isStepCompleted(memo.highlights, index)
                                    Row(verticalAlignment = Alignment.Top) {
                                        StepCircleToggle(
                                            completed = stepDone,
                                            onClick = { onToggleStepComplete?.invoke(memo, index) },
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${index + 1}. $step",
                                            fontSize = 11.sp,
                                            lineHeight = 18.sp,
                                            color = if (stepDone || isCompletedTask) Color(0xFF94A3B8) else bodyColor,
                                            textDecoration = if (stepDone || isCompletedTask) TextDecoration.LineThrough else TextDecoration.None,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = displayContent,
                                fontSize = 11.sp,
                                lineHeight = 18.sp,
                                color = bodyColor,
                                textDecoration = if (isCompletedTask) TextDecoration.LineThrough else TextDecoration.None,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    MemoType.Ideas -> {
                        val ideas = if (brainstormEnabled) {
                            CardContentUtils.parseIdeaItems(displayContent)
                        } else {
                            emptyList()
                        }
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            if (ideas.isNotEmpty()) {
                                Text(
                                    text = "AI脑风暴",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = bodyColor
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ideas.forEach { idea ->
                                    Text(
                                        text = idea,
                                        fontSize = 11.sp,
                                        lineHeight = 18.sp,
                                        color = bodyColor,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = displayContent,
                                    fontSize = 11.sp,
                                    lineHeight = 18.sp,
                                    color = bodyColor,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCircleToggle(
    completed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .then(
                if (completed) {
                    Modifier.background(Color(0xFF15803D))
                } else {
                    Modifier.border(1.5.dp, Color(0xFF94A3B8), CircleShape)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (completed) {
            Text("✓", color = Color.White, fontSize = 10.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditScreen(
    memo: MemoUi,
    taskBreakdownEnabled: Boolean,
    brainstormEnabled: Boolean,
    onBack: () -> Unit,
    onDelete: (MemoUi) -> Unit,
    onSave: (MemoUi) -> Unit
) {
    var summary by remember(memo.id) { mutableStateOf(memo.summary) }
    var content by remember(memo.id) {
        mutableStateOf(
            memoEditableContent(
                memo = memo,
                taskBreakdownEnabled = taskBreakdownEnabled,
                brainstormEnabled = brainstormEnabled
            )
        )
    }
    var remindAtMillis by remember(memo.id) { mutableStateOf(memo.remindAtMillis) }
    var timeText by remember(memo.id) { mutableStateOf(memo.timeText) }
    val bodyTextStyle = if (memo.type == MemoType.Tasks || memo.type == MemoType.Ideas) {
        TextStyle(fontSize = 11.sp, lineHeight = 18.sp, color = Color(0xFF334155))
    } else {
        TextStyle(fontSize = 13.sp, lineHeight = 22.sp)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        TopAppBar(
            title = { Text("修改卡片", fontWeight = FontWeight.Bold) },
            actions = { TextButton(onClick = { onDelete(memo) }) { Text("删除", color = Color.Red) } },
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE0F2FE))
        )
        if (memo.type == MemoType.Tasks && !timeText.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏰ 提醒：$timeText",
                    color = Color(0xFF15803D),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    remindAtMillis = null
                    timeText = null
                }) {
                    Text("取消提醒", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
        }
        TextField(
            value = summary,
            onValueChange = { summary = it },
            textStyle = TextStyle(fontSize = 13.sp, lineHeight = 22.sp),
            placeholder = { Text("卡片摘要") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF1F5F9),
                unfocusedContainerColor = Color(0xFFF1F5F9),
                disabledContainerColor = Color(0xFFF1F5F9),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
        )
        TextField(
            value = content,
            onValueChange = { content = it },
            textStyle = bodyTextStyle,
            placeholder = { Text("详细内容") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF1F5F9),
                unfocusedContainerColor = Color(0xFFF1F5F9),
                disabledContainerColor = Color(0xFFF1F5F9),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(260.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        TextButton(
            onClick = {
                onSave(
                    memo.copy(
                        summary = summary,
                        content = content,
                        remindAtMillis = remindAtMillis,
                        timeText = timeText
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .navigationBarsPadding()
                .background(Color(0xFF2563EB), RoundedCornerShape(24.dp))
                .padding(vertical = 8.dp)
        ) {
            Text("保存卡片", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

private enum class HomeSubTab(val label: String) {
    CornerCabin("角落小屋"),
    VentTreeHole("解压树洞"),
    WishExchange("心愿兑换")
}

private object CabinUi {
    val TopBar = Color(0xFFFDF8DF)
    val PageBg = Color(0xFFFBF9F1)
    val CardBg = Color.White
    val CardRadius = 20.dp
    val CardElevation = 8.dp
    val CardShadowColor = Color(0x40000000)
    val PlaceholderBeige = Color(0xFFF4E8BB)
    val Secondary = Color(0xFF6E7071)
    val Title = Color.Black
}

private fun Modifier.cabinCardShadow(shape: Shape): Modifier =
    shadow(
        elevation = CabinUi.CardElevation,
        shape = shape,
        clip = false,
        ambientColor = CabinUi.CardShadowColor,
        spotColor = CabinUi.CardShadowColor
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeZoneScreen(
    subTab: HomeSubTab,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CabinUi.PageBg)
    ) {
        TopAppBar(
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text("返回", color = CabinUi.Title, fontSize = 16.sp)
                }
            },
            title = { },
            actions = {
                Text(
                    text = "Memo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CabinUi.Title,
                    modifier = Modifier.padding(end = 4.dp)
                )
            },
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = CabinUi.TopBar,
                titleContentColor = CabinUi.Title
            )
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CabinUi.PageBg)
        ) {
            when (subTab) {
                HomeSubTab.CornerCabin -> CornerCabinContent()
                HomeSubTab.VentTreeHole -> VentTreeHoleContent()
                HomeSubTab.WishExchange -> WishExchangeContent()
            }
        }
    }
}

@Composable
private fun HomeSubTabRow(
    selected: HomeSubTab,
    onSelect: (HomeSubTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HomeSubTab.entries.forEach { tab ->
            HomeSubTabChip(
                label = tab.label,
                selected = selected == tab,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeSubTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(CabinUi.CardRadius)
    Box(
        modifier = modifier
            .height(66.dp)
            .then(if (selected) Modifier.cabinCardShadow(shape) else Modifier)
            .clip(shape)
            .background(CabinUi.CardBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = CabinUi.Title,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun HomeSubPlaceholder(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$title\n即将上线",
            fontSize = 14.sp,
            color = CabinUi.Secondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun CabinUnlockPopup(
    event: CabinUnlockEvent,
    onDismiss: () -> Unit,
    onGoToCabin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bubbleShape = RoundedCornerShape(33.dp)
    Column(
        modifier = modifier.width(252.dp),
        horizontalAlignment = Alignment.End
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = bubbleShape,
                    clip = false,
                    ambientColor = Color(0x40000000),
                    spotColor = Color(0x40000000)
                )
                .clip(bubbleShape)
                .background(Color.White)
                .clickable(onClick = onGoToCabin)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🏡小屋新变化",
                fontSize = 14.sp,
                color = CabinUi.Title,
                textAlign = TextAlign.Center
            )
            Text(
                text = "你已完成了${event.completedTasks}个tasks",
                fontSize = 14.sp,
                color = CabinUi.Title,
                textAlign = TextAlign.Center
            )
            Text(
                text = "小屋长出了${event.furnitureName}",
                fontSize = 14.sp,
                color = CabinUi.Title,
                textAlign = TextAlign.Center
            )
            Text(
                text = "快去看看吧！",
                fontSize = 14.sp,
                color = CabinUi.Title,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier
                .padding(end = 24.dp)
                .size(width = 20.dp, height = 14.dp)
                .graphicsLayer { rotationZ = -27f }
                .background(Color.White, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(onClick = onDismiss) {
            Text("关闭", fontSize = 12.sp, color = CabinUi.Secondary)
        }
    }
}

@Composable
private fun VentTreeHoleContent() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val cardShape = RoundedCornerShape(CabinUi.CardRadius)
    val options = listOf("气泡解压", "木鱼禅意", "沙子放松")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 15.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .cabinCardShadow(cardShape)
                .clip(cardShape)
                .background(CabinUi.CardBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "情绪释放区\n（待定）",
                fontSize = 14.sp,
                color = CabinUi.Secondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { label ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(123.dp)
                            .clip(RoundedCornerShape(CabinUi.CardRadius))
                            .background(CabinUi.PlaceholderBeige)
                            .clickable {
                                Toast.makeText(context, "$label 功能待定", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "待定",
                            fontSize = 12.sp,
                            color = CabinUi.Secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        color = CabinUi.Title,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CornerCabinContent() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("memo_ai_prefs", Context.MODE_PRIVATE) }
    val scrollState = rememberScrollState()
    val cardShape = RoundedCornerShape(CabinUi.CardRadius)
    val unlockedCount = remember {
        CabinUnlockManager.getStoredUnlockedCount(prefs)
            .coerceIn(0, CabinUnlockManager.totalFurnitureCount)
    }
    val totalCount = CabinUnlockManager.totalFurnitureCount
    val progress = unlockedCount.toFloat() / totalCount.toFloat()
    val completedTasks = CabinUnlockManager.getStoredCompletedTaskCount(prefs)
    val nextUnlock = CabinUnlockManager.getNextUnlock(completedTasks)
    val tasksUntilNext = nextUnlock?.let { it.first - completedTasks } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 15.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .cabinCardShadow(cardShape)
                .clip(cardShape)
                .background(CabinUi.CardBg)
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.corner_cabin_scene),
                contentDescription = "角落小屋",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(19.dp)
                        .border(1.dp, Color.Black, RoundedCornerShape(200.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(200.dp))
                            .background(Color.Black)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 14.sp,
                    color = CabinUi.Title
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "🏠当前阶段：理想家园",
                fontSize = 14.sp,
                color = CabinUi.Title,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "已解锁${unlockedCount}/${totalCount}件家具",
                fontSize = 14.sp,
                color = CabinUi.Title,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .cabinCardShadow(cardShape)
                .clip(cardShape)
                .background(CabinUi.CardBg)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 98.dp, height = 100.dp)
                    .clip(RoundedCornerShape(CabinUi.CardRadius))
                    .background(CabinUi.PlaceholderBeige)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🔒下一个家具：${nextUnlock?.second ?: "已全部解锁"}",
                    fontSize = 14.sp,
                    color = CabinUi.Title
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (nextUnlock != null) {
                        "下一个家具解锁还需完成${tasksUntilNext}个tasks"
                    } else {
                        "所有家具已解锁"
                    },
                    fontSize = 12.sp,
                    color = CabinUi.Secondary.copy(alpha = 0.76f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun WishExchangeContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { WishStore(context) }
    var wishes by remember { mutableStateOf(store.loadWishes()) }
    var shellBalance by remember { mutableIntStateOf(store.getShellBalance()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }

    fun refresh() {
        wishes = store.loadWishes()
        shellBalance = store.getShellBalance()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯心愿兑换",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = CabinUi.Title
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "贝壳余额：${shellBalance}🐚",
                    fontSize = 14.sp,
                    color = CabinUi.Title
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (wishes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    NewWishButton(onClick = { showCreateDialog = true })
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    wishes.forEach { wish ->
                        WishCard(
                            wish = wish,
                            shellBalance = shellBalance,
                            store = store,
                            onImageUpdated = { refresh() },
                            onRedeem = {
                                when (val result = store.redeemWish(wish.id)) {
                                    is RedeemResult.Success -> {
                                        refresh()
                                        val redeemed = result.wish
                                        if (redeemed.link != null) {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(redeemed.link))
                                                )
                                            }.onFailure {
                                                Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("心愿商品", redeemed.title))
                                            Toast.makeText(context, "已复制商品名，可在淘宝/京东搜索", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    RedeemResult.NotEnoughBalance ->
                                        Toast.makeText(context, "贝壳余额不足", Toast.LENGTH_SHORT).show()
                                    RedeemResult.NotFound -> refresh()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NewWishButton(onClick = { showCreateDialog = true })
                }
            }
        }

        if (showCreateDialog) {
            CreateWishDialog(
                loading = creating,
                onDismiss = {
                    if (!creating) showCreateDialog = false
                },
                onConfirm = { input ->
                    creating = true
                    scope.launch {
                        runCatching {
                            store.createWishFromInput(input)
                        }.onSuccess {
                            refresh()
                            showCreateDialog = false
                            Toast.makeText(context, "心愿已添加", Toast.LENGTH_SHORT).show()
                        }.onFailure { e ->
                            Toast.makeText(
                                context,
                                e.message ?: "创建失败",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        creating = false
                    }
                }
            )
        }
    }
}

@Composable
private fun NewWishButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(132.dp)
            .height(51.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color(0x40000000),
                spotColor = Color(0x40000000)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(CabinUi.PlaceholderBeige)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+ 新建心愿",
            fontSize = 14.sp,
            color = CabinUi.Title
        )
    }
}

@Composable
private fun CreateWishDialog(
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val cardShape = RoundedCornerShape(CabinUi.CardRadius)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (!loading) onDismiss() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .cabinCardShadow(cardShape)
                .clip(cardShape)
                .background(CabinUi.CardBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = "添加你的心愿奖励",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = CabinUi.Title
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(197.dp)
                    .clip(RoundedCornerShape(CabinUi.CardRadius))
                    .background(CabinUi.PlaceholderBeige)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (input.isBlank()) {
                    Column {
                        Text(
                            text = "粘贴你想要的奖励的商品链接到这里……",
                            fontSize = 12.sp,
                            color = CabinUi.Secondary.copy(alpha = 0.76f),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "或者描述你想要的奖励和价格",
                            fontSize = 12.sp,
                            color = CabinUi.Secondary.copy(alpha = 0.76f),
                            lineHeight = 18.sp
                        )
                    }
                }
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(fontSize = 14.sp, color = CabinUi.Title),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss, enabled = !loading) {
                    Text("取消", color = CabinUi.Secondary, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(input) },
                    enabled = !loading && input.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("保存", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WishCard(
    wish: WishItem,
    shellBalance: Int,
    store: WishStore,
    onImageUpdated: () -> Unit,
    onRedeem: () -> Unit
) {
    var imageUrl by remember(wish.id, wish.imageUrl) { mutableStateOf(wish.imageUrl) }

    LaunchedEffect(wish.id, wish.link, wish.imageUrl) {
        if (imageUrl.isNullOrBlank() && !wish.link.isNullOrBlank()) {
            store.refreshWishImage(wish.id)?.let {
                imageUrl = it.imageUrl
                onImageUpdated()
            }
        }
    }

    val cardShape = RoundedCornerShape(CabinUi.CardRadius)
    val progressAmount = shellBalance.coerceAtMost(wish.priceShells)
    val affordable = shellBalance >= wish.priceShells
    val progress = if (wish.priceShells > 0) {
        progressAmount.toFloat() / wish.priceShells.toFloat()
    } else {
        0f
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(121.dp)
            .cabinCardShadow(cardShape)
            .clip(cardShape)
            .background(CabinUi.CardBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!imageUrl.isNullOrBlank()) {
            WishRemoteImage(
                imageUrl = imageUrl!!,
                referer = wish.link,
                modifier = Modifier
                    .size(width = 98.dp, height = 100.dp)
                    .clip(RoundedCornerShape(CabinUi.CardRadius))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 98.dp, height = 100.dp)
                    .clip(RoundedCornerShape(CabinUi.CardRadius))
                    .background(CabinUi.PlaceholderBeige)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = wish.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = CabinUi.Title,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (affordable) {
                Text(
                    text = "已攒够",
                    fontSize = 14.sp,
                    color = CabinUi.Title
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(21.dp)
                            .border(1.dp, Color.Black, RoundedCornerShape(200.dp))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(200.dp))
                                .background(Color.Black)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$progressAmount/${wish.priceShells}",
                        fontSize = 12.sp,
                        color = CabinUi.Title
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${wish.priceShells}🐚",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = CabinUi.Title
            )
            if (affordable) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(67.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(75.dp))
                        .background(Color.Black)
                        .clickable(onClick = onRedeem),
                    contentAlignment = Alignment.Center
                ) {
                    Text("兑换", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun WishRemoteImage(
    imageUrl: String,
    referer: String? = null,
    modifier: Modifier = Modifier
) {
    val bitmap = produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, imageUrl) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val client = okhttp3.OkHttpClient()
                val requestBuilder = okhttp3.Request.Builder()
                    .url(imageUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                if (!referer.isNullOrBlank()) {
                    requestBuilder.header("Referer", referer)
                } else if (imageUrl.contains("alicdn.com")) {
                    requestBuilder.header("Referer", "https://www.taobao.com/")
                }
                val request = requestBuilder.build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    response.body?.byteStream()?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }
            }.getOrNull()
        }
    }.value

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(CabinUi.PlaceholderBeige),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = CabinUi.Secondary
            )
        }
    }
}

@Composable
private fun BottomNavRow(
    selectedType: MemoType?,
    showMeme: Boolean,
    onTap: (MemoType?) -> Unit,
    onOpenMeme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavChip(
            label = "Tasks",
            selected = selectedType == MemoType.Tasks && !showMeme,
            onClick = { onTap(MemoType.Tasks) },
            modifier = Modifier.weight(1f)
        )
        NavChip(
            label = "Ideas",
            selected = selectedType == MemoType.Ideas && !showMeme,
            onClick = { onTap(MemoType.Ideas) },
            modifier = Modifier.weight(1f)
        )
        NavChip(
            label = "Home",
            selected = showMeme,
            onClick = onOpenMeme,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NavChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) HomeUi.Primary else Color(0xFFE5E7EB))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun BottomHomeBar(
    selectedType: MemoType?,
    showMeme: Boolean,
    homeSubTab: HomeSubTab,
    onHomeSubTabChange: (HomeSubTab) -> Unit,
    showInput: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    onSend: (String?) -> Unit,
    onTap: (MemoType?) -> Unit,
    onOpenMeme: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var inputFocused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .imePadding()
            .padding(top = 10.dp, start = 16.dp, end = 16.dp)
    ) {
        if (showMeme) {
            HomeSubTabRow(
                selected = homeSubTab,
                onSelect = onHomeSubTabChange
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (showInput) {
            val inputHeight = 56.dp
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text("说点什么...", color = HomeUi.Placeholder) },
                    textStyle = TextStyle(fontSize = 14.sp, color = HomeUi.Title),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = HomeUi.InputBg,
                        unfocusedContainerColor = HomeUi.InputBg,
                        disabledContainerColor = HomeUi.InputBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (value.isNotBlank()) {
                            onSend(null)
                            keyboardController?.hide()
                        }
                    }),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(inputHeight)
                        .focusRequester(focusRequester)
                        .onFocusChanged { inputFocused = it.isFocused }
                )
                Spacer(modifier = Modifier.width(12.dp))
                HomeVoiceInputButton(
                    inputFocused = inputFocused,
                    inputText = value,
                    primaryColor = HomeUi.Primary,
                    disabledColor = Color(0xFFB0BEC5),
                    onSubmit = {
                        if (value.isNotBlank()) {
                            onSend(null)
                            keyboardController?.hide()
                            focusRequester.freeFocus()
                        }
                    },
                    onTranscribed = { text ->
                        onValueChange(text)
                        onSend(text)
                        keyboardController?.hide()
                        focusRequester.freeFocus()
                    },
                    modifier = Modifier.size(inputHeight)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        BottomNavRow(
            selectedType = selectedType,
            showMeme = showMeme,
            onTap = onTap,
            onOpenMeme = onOpenMeme,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 5.dp)
        )
    }
}

@Composable
private fun BottomCategoryBar(
    selectedType: MemoType?,
    showMeme: Boolean,
    homeSubTab: HomeSubTab,
    onHomeSubTabChange: (HomeSubTab) -> Unit,
    onTap: (MemoType?) -> Unit,
    onOpenMeme: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = 5.dp, start = 16.dp, end = 16.dp)
    ) {
        if (showMeme) {
            HomeSubTabRow(
                selected = homeSubTab,
                onSelect = onHomeSubTabChange
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        BottomNavRow(
            selectedType = selectedType,
            showMeme = showMeme,
            onTap = onTap,
            onOpenMeme = onOpenMeme
        )
    }
}

@Composable
private fun rememberDecodedBitmap(uri: Uri?): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    return produceState(initialValue = null as androidx.compose.ui.graphics.ImageBitmap?, uri) {
        value = if (uri == null) null else withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }.value
}

@Composable
private fun WallpaperSettingsDialog(
    homeWallpaperUri: Uri?,
    categoryWallpaperUri: Uri?,
    onDismiss: () -> Unit,
    onUpdate: (Uri?, Uri?) -> Unit
) {
    val context = LocalContext.current
    val target = remember { mutableIntStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        when (target.value) {
            0 -> onUpdate(uri, categoryWallpaperUri)
            1 -> onUpdate(homeWallpaperUri, uri)
            else -> onUpdate(uri, uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("壁纸设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    onClick = {
                        target.value = 0
                        picker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("首页壁纸") }
                TextButton(
                    onClick = {
                        target.value = 1
                        picker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("分类页面壁纸") }
                TextButton(
                    onClick = {
                        target.value = 2
                        picker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("使用同一张壁纸") }
                TextButton(
                    onClick = { onUpdate(null, null) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("恢复默认", color = Color(0xFFEF4444)) }
                Text(text = "提示：壁纸会以淡淡的效果显示，避免影响阅读。", fontSize = 11.sp, color = Color(0xFF64748B))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } }
    )
}

private fun loadPrefsUri(prefs: SharedPreferences, key: String): Uri? {
    val raw = prefs.getString(key, null) ?: return null
    return runCatching { Uri.parse(raw) }.getOrNull()
}

private fun savePrefsUri(prefs: SharedPreferences, key: String, uri: Uri?) {
    prefs.edit().putString(key, uri?.toString()).apply()
}

@Composable
private fun NavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF2563EB),
    inactiveColor: Color = Color(0xFF94A3B8),
    onClick: () -> Unit
) {
    val tint = if (active) activeColor else inactiveColor
    val interaction = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onClick() }
            .padding(top = 4.dp, bottom = 0.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = tint, textAlign = TextAlign.Center)
    }
}

// Legacy helper kept for reference; home screen uses HomeTextBubble.
@Composable
private fun TextBubble(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        modifier = Modifier
            .background(Color(0xFF2563EB), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    )
}
