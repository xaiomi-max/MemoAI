@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.memoai.app

import android.os.Bundle
import android.content.Context
import android.net.Uri
import android.os.SystemClock
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.MenuBook
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
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.memoai.app.data.MemoDatabase
import com.memoai.app.data.MemoRepository
import com.memoai.app.ui.AiSettings
import com.memoai.app.ui.MemoType
import com.memoai.app.ui.MemoUi
import com.memoai.app.ui.MemoViewModel
import com.memoai.app.ui.MemoViewModelFactory
import java.util.Locale
import android.graphics.Movie
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = android.graphics.Color.parseColor("#E0F2FE")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContent {
            val db = remember { MemoDatabase.get(applicationContext) }
            val repo = remember { MemoRepository(db.memoDao()) }
            val prefs = remember { getSharedPreferences("memo_ai_prefs", MODE_PRIVATE) }
            val vm: MemoViewModel = viewModel(factory = MemoViewModelFactory(repo, prefs))
            MemoAiApp(vm, prefs)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoAiApp(vm: MemoViewModel, prefs: SharedPreferences) {
    val context = LocalContext.current
    val memos by vm.memos.collectAsStateWithLifecycle()
    val query by vm.currentQuery().collectAsStateWithLifecycle()
    val aiSettings by vm.aiSettings.collectAsStateWithLifecycle()
    val busy by vm.isBusy.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    val streamingInput by vm.currentStreamingInput.collectAsStateWithLifecycle()
    val streamingOutput by vm.currentStreamingOutput.collectAsStateWithLifecycle()
    var selectedType by remember { mutableStateOf<MemoType?>(null) }
    var editing by remember { mutableStateOf<MemoUi?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showMeme by remember { mutableStateOf(false) }
    var showWallpaper by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var homeWallpaperUri by remember { mutableStateOf(loadPrefsUri(prefs, "wallpaper_home_uri")) }
    var categoryWallpaperUri by remember { mutableStateOf(loadPrefsUri(prefs, "wallpaper_category_uri")) }

    val homeWallpaperBitmap = rememberDecodedBitmap(homeWallpaperUri)
    val categoryWallpaperBitmap = rememberDecodedBitmap(categoryWallpaperUri)

    fun submitDraft() {
        val text = draft.trim()
        if (text.isBlank()) return
        vm.addByVoice(text)
        draft = ""
    }

    LaunchedEffect(msg) {
        if (msg.isNotBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            vm.clearMessage()
        }
    }

    MaterialTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (editing == null) {
                    if (selectedType == null) {
                        BottomHomeBar(
                            selectedType = selectedType,
                            value = draft,
                            onValueChange = { draft = it },
                            onSend = { submitDraft() },
                            onTap = { selectedType = it }
                        )
                    } else {
                        BottomCategoryBar(
                            selectedType = selectedType,
                            onTap = {
                                selectedType = it
                                showMeme = false
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
                    .background(Color(0xFFF8FAFC))
            ) {
                when {
                    editing != null -> EditScreen(
                        memo = editing!!,
                        onBack = { editing = null },
                        onDelete = { vm.delete(it); editing = null },
                        onSave = { vm.save(it); editing = null }
                    )
                    selectedType == null -> HomeScreen(
                        memos = memos,
                        onEdit = { editing = it },
                        onDelete = { vm.delete(it) },
                        onOpenSettings = { showSettings = true },
                        streamingInput = streamingInput,
                        streamingOutput = streamingOutput,
                        wallpaper = homeWallpaperBitmap
                    )
                    else -> CategoryScreen(
                        type = selectedType!!,
                        memos = memos,
                        query = query,
                        onQuery = vm::setSearchQuery,
                        onBack = { selectedType = null },
                        onDelete = { vm.delete(it) },
                        onOpenMeme = { if (selectedType == MemoType.Knowledge) showMeme = true },
                        onEdit = { editing = it },
                        wallpaper = categoryWallpaperBitmap
                    )
                }

                if (showMeme && selectedType == MemoType.Knowledge && editing == null) {
                    MemePage(
                        onBack = { showMeme = false },
                        onNavigate = { next ->
                            selectedType = next
                            showMeme = false
                        }
                    )
                }
                if (busy && streamingOutput.isBlank()) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF2563EB))
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            initial = aiSettings,
            onDismiss = { showSettings = false },
            onSave = { cloud, key, model ->
                vm.updateAiSettings(cloud, key, model)
                showSettings = false
            },
            onOpenWallpaper = {
                showSettings = false
                showWallpaper = true
            }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(
    initial: AiSettings,
    onDismiss: () -> Unit,
    onSave: (Boolean, String, String) -> Unit,
    onOpenWallpaper: () -> Unit
) {
    var cloud by remember(initial) { mutableStateOf(initial.useCloud) }
    var key by remember(initial) { mutableStateOf(initial.apiKey) }
    var model by remember(initial) { mutableStateOf(initial.model) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("使用 DeepSeek 云端", modifier = Modifier.weight(1f))
                    Switch(checked = cloud, onCheckedChange = { cloud = it })
                }
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("模型") }, placeholder = { Text("deepseek-chat") })
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("DeepSeek API Key") }, placeholder = { Text("sk-...") })
                Text("关闭开关时，使用本地规则模型（离线）。", fontSize = 11.sp, color = Color(0xFF64748B))
                TextButton(onClick = onOpenWallpaper, modifier = Modifier.fillMaxWidth()) { Text("壁纸设置") }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(cloud, key, model) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    memos: List<MemoUi>,
    onEdit: (MemoUi) -> Unit,
    onDelete: (MemoUi) -> Unit,
    onOpenSettings: () -> Unit,
    streamingInput: String,
    streamingOutput: String,
    wallpaper: androidx.compose.ui.graphics.ImageBitmap?
) {
    var pendingDelete by remember { mutableStateOf<MemoUi?>(null) }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(modifier = Modifier.fillMaxSize()) {
        if (wallpaper != null) {
            Image(
                bitmap = wallpaper,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.22f,
                modifier = Modifier.fillMaxSize().padding(top = topPadding)
            )
        }
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Memo", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE0F2FE)),
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, contentDescription = null, tint = Color(0xFF2563EB)) }
                }
            )
            LazyColumn(
                reverseLayout = true,
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (streamingInput.isNotBlank()) {
                    item("streaming-preview") {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextBubble(text = streamingInput) }
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = "AI 正在生成...", color = Color(0xFFA16207), fontSize = 9.sp, modifier = Modifier.background(Color(0xFFFEF9C3), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
                                    Text(
                                        text = if (streamingOutput.isBlank()) "..." else streamingOutput,
                                        fontSize = 11.sp,
                                        lineHeight = 18.sp,
                                        color = Color(0xFF334155),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                items(memos, key = { it.id }) { memo ->
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextBubble(text = memo.userInput) }
                        Spacer(modifier = Modifier.height(6.dp))
                        MemoCard(
                            memo = memo,
                            onEdit = { onEdit(memo) },
                            onDeleteRequest = { pendingDelete = it }
                        )
                    }
                }
            }
        }
    }

    if (pendingDelete != null) {
        val item = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这张卡片？") },
            text = { Text(item.summary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(item)
                        pendingDelete = null
                    }
                ) { Text("删除", color = Color(0xFFEF4444)) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } }
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
    onOpenMeme: () -> Unit,
    onEdit: (MemoUi) -> Unit,
    wallpaper: androidx.compose.ui.graphics.ImageBitmap?
) {
    var inputText by remember(type, query) { mutableStateOf(query) }
    var submittedQuery by remember(type) { mutableStateOf("") }
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val scope = rememberCoroutineScope()
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
        it.summary.contains(submittedQuery, ignoreCase = true) || it.content.contains(submittedQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationX = dragAnim.value.coerceAtLeast(0f) }
            .pointerInput(type) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        val next = (dragAnim.value + dragAmount).coerceIn(-screenWidthPx * 0.35f, screenWidthPx)
                        scope.launch { dragAnim.snapTo(next) }
                    },
                    onDragEnd = {
                        val thresholdPx = with(density) { 50.dp.toPx() }
                        val current = dragAnim.value
                        when {
                            type == MemoType.Knowledge && current < -thresholdPx -> {
                                onOpenMeme()
                                scope.launch { dragAnim.animateTo(0f, tween(160)) }
                            }
                            current > thresholdPx -> {
                                scope.launch {
                                    dragAnim.animateTo(screenWidthPx, tween(180))
                                    onBack()
                                    dragAnim.snapTo(0f)
                                }
                            }
                            else -> {
                                scope.launch { dragAnim.animateTo(0f, tween(160)) }
                            }
                        }
                    },
                    onDragCancel = { scope.launch { dragAnim.animateTo(0f, tween(160)) } }
                )
            }
    ) {
        TopAppBar(
            title = { Text(type.name, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE0F2FE))
        )
        OutlinedTextField(
            value = inputText,
            onValueChange = {
                inputText = it
                if (it.isBlank()) {
                    submittedQuery = ""
                    onQuery("")
                }
            },
            leadingIcon = { IconButton(onClick = { submitSearch() }) { Icon(Icons.Outlined.Search, contentDescription = "搜索") } },
            placeholder = { Text("在 ${type.name} 中搜索...", fontSize = 11.sp) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF1F5F9),
                unfocusedContainerColor = Color(0xFFF1F5F9),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        )
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { memo ->
                    MemoCard(
                        memo = memo,
                        onEdit = { onEdit(memo) },
                        onDeleteRequest = { pendingDelete = it },
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }
        }
    }

    if (pendingDelete != null) {
        val item = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这张卡片？") },
            text = { Text(item.summary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(item)
                        pendingDelete = null
                    }
                ) { Text("删除", color = Color(0xFFEF4444)) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun MemoCard(
    memo: MemoUi,
    onEdit: () -> Unit,
    onDeleteRequest: (MemoUi) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val badge = when (memo.type) {
        MemoType.Ideas -> Color(0xFFFEF9C3) to Color(0xFFA16207)
        MemoType.Tasks -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        MemoType.Knowledge -> Color(0xFFF3E8FF) to Color(0xFF7E22CE)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { expanded = !expanded },
                onLongClick = { onDeleteRequest(memo) }
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = memo.type.name.uppercase(Locale.getDefault()),
                    color = badge.second,
                    fontSize = 9.sp,
                    modifier = Modifier.background(badge.first, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(text = memo.timestamp, color = Color(0xFF94A3B8), fontSize = 10.sp)
                IconButton(onClick = onEdit, modifier = Modifier.size(20.dp)) { Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color(0xFFCBD5E1)) }
            }
            Text(text = memo.summary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            if (expanded) {
                Text(text = memo.content, fontSize = 11.sp, lineHeight = 18.sp, color = Color(0xFF334155), modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditScreen(memo: MemoUi, onBack: () -> Unit, onDelete: (MemoUi) -> Unit, onSave: (MemoUi) -> Unit) {
    var summary by remember(memo.id) { mutableStateOf(memo.summary) }
    var content by remember(memo.id) { mutableStateOf(memo.content) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        TopAppBar(
            title = { Text("修改卡片", fontWeight = FontWeight.Bold) },
            actions = { TextButton(onClick = { onDelete(memo) }) { Text("删除", color = Color.Red) } },
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE0F2FE))
        )
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
            textStyle = TextStyle(fontSize = 13.sp, lineHeight = 22.sp),
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
                        content = content
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

@Composable
private fun MemePage(onBack: () -> Unit, onNavigate: (MemoType?) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("meme_page", Context.MODE_PRIVATE) }
    val first = remember { !prefs.getBoolean("seen", false) }
    var showIntro by remember { mutableStateOf(first) }

    var savedUri by remember { mutableStateOf(loadGifUri(prefs)) }
    var isPlaying by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    var cheerText by remember { mutableStateOf(prefs.getString("meme_text", "") ?: "") }
    var editingCheer by remember { mutableStateOf(false) }
    var cheerDraft by remember { mutableStateOf(cheerText) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.isSuccess
        if (!ok) {
            Toast.makeText(context, "无法获取 GIF 读取权限，请重试", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        savedUri = uri
        prefs.edit().putString("gif_uri", uri.toString()).apply()
        isPlaying = false
    }

    if (showIntro) {
        AlertDialog(
            onDismissRequest = {
                showIntro = false
                prefs.edit().putBoolean("seen", true).apply()
            },
            title = { Text("🎉 恭喜你发现隐藏小彩蛋！") },
            text = { Text("这里可以存放你的专属表情包，长按上传 GIF，点击屏幕播放 1 秒。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showIntro = false
                        prefs.edit().putBoolean("seen", true).apply()
                    }
                ) { Text("知道了", color = Color(0xFF2563EB)) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount -> dragX += dragAmount },
                    onDragEnd = {
                        val thresholdPx = with(density) { 50.dp.toPx() }
                        if (dragX > thresholdPx) onBack()
                        dragX = 0f
                    },
                    onDragCancel = { dragX = 0f }
                )
            }
    ) {
        TopAppBar(
            title = { Text("Memo", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE0F2FE)),
            actions = {
                TextButton(onClick = onBack) { Text("返回", color = Color(0xFF2563EB)) }
            }
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!editingCheer && savedUri != null) isPlaying = true
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
            Box(
                modifier = Modifier
                    .offset(y = (-10).dp)
            ) {
                MemeBox(
                    uri = savedUri,
                    isPlaying = isPlaying,
                    onPick = { picker.launch(arrayOf("image/gif")) },
                    onStop = { isPlaying = false }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (!editingCheer) {
                val display = cheerText.ifBlank { "鹿小葵，加油！" }
                val displayColor = if (cheerText.isBlank()) Color(0xFF94A3B8) else Color(0xFF0F172A)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8FAFC))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            cheerDraft = cheerText
                            editingCheer = true
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = display, color = displayColor, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = cheerDraft,
                        onValueChange = { cheerDraft = it },
                        placeholder = { Text("鹿小葵，加油！") },
                        singleLine = true,
                        textStyle = TextStyle(textAlign = TextAlign.Start, fontSize = 13.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            disabledContainerColor = Color(0xFFF8FAFC),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = {
                            cheerText = cheerDraft.trim()
                            prefs.edit().putString("meme_text", cheerText).apply()
                            editingCheer = false
                        },
                        modifier = Modifier.size(46.dp).background(Color(0xFF2563EB), RoundedCornerShape(16.dp))
                    ) {
                        Text(text = "✓", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun MemeBox(uri: Uri?, isPlaying: Boolean, onPick: () -> Unit, onStop: () -> Unit) {
    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onPick() })
            },
        contentAlignment = Alignment.Center
    ) {
        if (uri == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(46.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text("长按上传 GIF", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        } else {
            GifPlayer(uri = uri, isPlaying = isPlaying, onOneSecondPlayed = onStop)
        }
    }
}

@Composable
private fun GifPlayer(uri: Uri, isPlaying: Boolean, onOneSecondPlayed: () -> Unit) {
    val context = LocalContext.current
    val decoded = produceState(initialValue = GifDecode(null, null), uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val size = pfd.statSize
                    if (size > 20L * 1024L * 1024L) {
                        return@runCatching GifDecode(null, "GIF 文件过大（超过 20MB），建议换小一点的 GIF")
                    }
                }
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val movie = Movie.decodeStream(input)
                    if (movie == null) GifDecode(null, "GIF 解码失败，请重新选择 GIF") else GifDecode(movie, null)
                } ?: GifDecode(null, "无法读取 GIF（权限不足或文件不存在）")
            }.getOrElse { e ->
                val msg = when (e) {
                    is SecurityException -> "无权读取该 GIF，请重新选择"
                    is OutOfMemoryError -> "GIF 太大导致内存不足，请换小一点的 GIF"
                    else -> "GIF 加载失败：${e.message ?: "未知错误"}"
                }
                GifDecode(null, msg)
            }
        }
    }.value

    val movie = decoded.movie
    val durationMs = remember(movie) { (movie?.duration()?.takeIf { it > 0 } ?: 1000).toInt() }
    var frameTimeMs by remember(uri) { mutableIntStateOf(0) }
    var startUptime by remember(uri) { mutableLongStateOf(0L) }

    LaunchedEffect(isPlaying, uri) {
        if (!isPlaying) {
            frameTimeMs = 0
            return@LaunchedEffect
        }
        startUptime = SystemClock.uptimeMillis()
        while (true) {
            val elapsed = SystemClock.uptimeMillis() - startUptime
            if (elapsed >= 1000) {
                frameTimeMs = 0
                onOneSecondPlayed()
                break
            }
            frameTimeMs = (elapsed % durationMs).toInt()
            kotlinx.coroutines.delay(16)
        }
    }

    if (movie == null) {
        Text(decoded.error ?: "GIF 加载失败", color = Color(0xFFEF4444), fontSize = 12.sp, textAlign = TextAlign.Center)
        return
    }

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val mw = movie.width().toFloat().takeIf { it > 0 } ?: 1f
        val mh = movie.height().toFloat().takeIf { it > 0 } ?: 1f
        val scale = min(size.width / mw, size.height / mh)
        val dx = (size.width / scale - mw) / 2f
        val dy = (size.height / scale - mh) / 2f

        drawIntoCanvas { canvas ->
            movie.setTime(frameTimeMs)
            val native = canvas.nativeCanvas
            val checkpoint = native.save()
            native.scale(scale, scale)
            movie.draw(native, dx, dy)
            native.restoreToCount(checkpoint)
        }
    }
}

private data class GifDecode(val movie: Movie?, val error: String?)

private fun loadGifUri(prefs: android.content.SharedPreferences): Uri? {
    val raw = prefs.getString("gif_uri", null) ?: return null
    return runCatching { Uri.parse(raw) }.getOrNull()
}

@Composable
private fun BottomHomeBar(
    selectedType: MemoType?,
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onTap: (MemoType?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = 10.dp, start = 14.dp, end = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("输入内容…") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF1F5F9),
                    unfocusedContainerColor = Color(0xFFF1F5F9),
                    disabledContainerColor = Color(0xFFF1F5F9),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            IconButton(
                onClick = onSend,
                modifier = Modifier.size(46.dp).background(Color(0xFF2563EB), RoundedCornerShape(18.dp))
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = "发送", tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            NavItem("Tasks", Icons.Outlined.TaskAlt, selectedType == MemoType.Tasks, modifier = Modifier.weight(1f)) { onTap(MemoType.Tasks) }
            NavItem("Ideas", Icons.Outlined.EmojiObjects, selectedType == MemoType.Ideas, modifier = Modifier.weight(1f)) { onTap(MemoType.Ideas) }
            NavItem("Knowledge", Icons.Outlined.MenuBook, selectedType == MemoType.Knowledge, modifier = Modifier.weight(1f)) { onTap(MemoType.Knowledge) }
        }
    }
}

@Composable
private fun BottomCategoryBar(selectedType: MemoType?, onTap: (MemoType?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 8.dp, start = 14.dp, end = 14.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        NavItem("Tasks", Icons.Outlined.TaskAlt, selectedType == MemoType.Tasks, modifier = Modifier.weight(1f)) { onTap(MemoType.Tasks) }
        NavItem("Ideas", Icons.Outlined.EmojiObjects, selectedType == MemoType.Ideas, modifier = Modifier.weight(1f)) { onTap(MemoType.Ideas) }
        NavItem("Knowledge", Icons.Outlined.MenuBook, selectedType == MemoType.Knowledge, modifier = Modifier.weight(1f)) { onTap(MemoType.Knowledge) }
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
    var target by remember { mutableStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        when (target) {
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
                        target = 0
                        picker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("首页壁纸") }
                TextButton(
                    onClick = {
                        target = 1
                        picker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("分类页面壁纸") }
                TextButton(
                    onClick = {
                        target = 2
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
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onClick() }
            .padding(vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (active) Color(0xFF2563EB) else Color(0xFF94A3B8))
        Text(label, fontSize = 11.sp, color = if (active) Color(0xFF2563EB) else Color(0xFF94A3B8))
    }
}

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
