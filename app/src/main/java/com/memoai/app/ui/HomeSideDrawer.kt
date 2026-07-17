package com.memoai.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private val DrawerBubbleBlue = Color(0xFF2962FF)

@Composable
fun HomeSideDrawer(
    visible: Boolean,
    memos: List<MemoUi>,
    onDismiss: () -> Unit,
    onExportJson: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val panelFraction = 0.8f
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val panelWidthPx = screenWidthPx * panelFraction
    val slideProgress = remember { Animatable(0f) }
    val dismissThresholdPx = with(density) { 80.dp.toPx() }
    var rendered by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<CalendarDay?>(null) }

    suspend fun animateOpen() {
        rendered = true
        slideProgress.snapTo(0f)
        slideProgress.animateTo(1f, tween(100))
    }

    suspend fun animateClose(onFinished: () -> Unit) {
        if (closing) return
        closing = true
        slideProgress.animateTo(0f, tween(100))
        closing = false
        rendered = false
        onFinished()
    }

    fun requestClose() {
        scope.launch {
            animateClose(onDismiss)
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            animateOpen()
        } else if (rendered) {
            animateClose { }
        }
    }

    if (!rendered) return

    val panelOffset = -panelWidthPx * (1f - slideProgress.value)
    val scrimValue = 0.35f * slideProgress.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(200f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(scrimValue)
                .background(Color.Black)
                .clickable(onClick = { requestClose() })
        )
        Column(
            modifier = Modifier
                .fillMaxWidth(panelFraction)
                .fillMaxHeight()
                .offset { IntOffset(panelOffset.roundToInt(), 0) }
                .background(Color.White)
                .windowInsetsPadding(WindowInsets.statusBars)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, amount ->
                            val currentOffset = -panelWidthPx * (1f - slideProgress.value)
                            val nextOffset = (currentOffset + amount).coerceIn(-panelWidthPx, 0f)
                            val progress = 1f - (-nextOffset / panelWidthPx)
                            scope.launch { slideProgress.snapTo(progress.coerceIn(0f, 1f)) }
                        },
                        onDragEnd = {
                            val currentOffset = -panelWidthPx * (1f - slideProgress.value)
                            if (currentOffset < -dismissThresholdPx) {
                                requestClose()
                            } else {
                                scope.launch {
                                    slideProgress.animateTo(1f, tween(100))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                slideProgress.animateTo(1f, tween(100))
                            }
                        }
                    )
                }
        ) {
            DrawerHeader(onClose = { requestClose() })
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    DrawerCalendarSection(
                        memos = memos,
                        onDaySelected = { selectedDay = it }
                    )
                }
                item {
                    DrawerMenuRow(
                        icon = Icons.Outlined.FileDownload,
                        title = "导出数据",
                        onClick = { showExportDialog = true }
                    )
                }
                item {
                    DrawerMenuRow(
                        icon = Icons.Outlined.Chat,
                        title = "微信AI",
                        enabled = false,
                        onClick = {}
                    )
                }
                item {
                    DrawerMenuRow(
                        icon = Icons.Outlined.Analytics,
                        title = "习惯分析",
                        enabled = false,
                        onClick = {}
                    )
                }
            }
        }
    }

    if (showExportDialog) {
        ExportDataDialog(
            onDismiss = { showExportDialog = false },
            onExport = {
                val json = onExportJson()
                copyAndShareJson(context, json)
                showExportDialog = false
                Toast.makeText(context, "数据已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }
        )
    }

    selectedDay?.let { day ->
        CompletedTasksDayDialog(
            day = day,
            tasks = completedTasksForDay(memos, day),
            onDismiss = { selectedDay = null }
        )
    }
}

private data class CalendarDay(val year: Int, val month: Int, val day: Int)

@Composable
private fun DrawerHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "功能菜单",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1C1E),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = Color(0xFF64748B))
        }
    }
}

@Composable
private fun DrawerCalendarSection(
    memos: List<MemoUi>,
    onDaySelected: (CalendarDay) -> Unit
) {
    val today = remember { Calendar.getInstance() }
    val todayYear = today.get(Calendar.YEAR)
    val todayMonth = today.get(Calendar.MONTH)
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    var year by remember { mutableIntStateOf(todayYear) }
    var month by remember { mutableIntStateOf(todayMonth) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = DrawerBubbleBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = String.format(Locale.CHINA, "%d年%d月", year, month + 1),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1C1E),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "‹",
                fontSize = 20.sp,
                modifier = Modifier
                    .clickable {
                        val cal = Calendar.getInstance().apply {
                            set(year, month, 1)
                            add(Calendar.MONTH, -1)
                        }
                        year = cal.get(Calendar.YEAR)
                        month = cal.get(Calendar.MONTH)
                    }
                    .padding(horizontal = 8.dp)
            )
            Text(
                text = "›",
                fontSize = 20.sp,
                modifier = Modifier
                    .clickable {
                        val cal = Calendar.getInstance().apply {
                            set(year, month, 1)
                            add(Calendar.MONTH, 1)
                        }
                        year = cal.get(Calendar.YEAR)
                        month = cal.get(Calendar.MONTH)
                    }
                    .padding(horizontal = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        val weekLabels = listOf("日", "一", "二", "三", "四", "五", "六")
        Row(modifier = Modifier.fillMaxWidth()) {
            weekLabels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        val cells = remember(year, month) { buildMonthCells(year, month) }
        val rows = cells.chunked(7)
        rows.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    val day = cell.day
                    val isToday = day != null &&
                        year == todayYear &&
                        month == todayMonth &&
                        day == todayDay
                    val hasCompleted = day != null && completedTasksForDay(
                        memos,
                        CalendarDay(year, month, day)
                    ).isNotEmpty()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clickable(enabled = day != null) {
                                if (day != null) {
                                    onDaySelected(CalendarDay(year, month, day))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            if (isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(DrawerBubbleBlue, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Text(
                                    text = day.toString(),
                                    fontSize = 12.sp,
                                    color = if (hasCompleted) DrawerBubbleBlue else Color(0xFF334155),
                                    fontWeight = if (hasCompleted) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = "点击日期查看当日完成的任务",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

private data class MonthCell(val day: Int?)

private fun buildMonthCells(year: Int, month: Int): List<MonthCell> {
    val cal = Calendar.getInstance().apply {
        set(year, month, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val firstWeekday = cal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = mutableListOf<MonthCell>()
    repeat(firstWeekday) { cells.add(MonthCell(null)) }
    for (d in 1..daysInMonth) {
        cells.add(MonthCell(d))
    }
    while (cells.size % 7 != 0) {
        cells.add(MonthCell(null))
    }
    return cells
}

private fun completedTasksForDay(memos: List<MemoUi>, day: CalendarDay): List<MemoUi> {
    return memos.filter { memo ->
        memo.type == MemoType.Tasks &&
            memo.completed &&
            memo.completedAtMillis != null &&
            isSameDay(memo.completedAtMillis, day)
    }
}

private fun isSameDay(millis: Long, day: CalendarDay): Boolean {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return cal.get(Calendar.YEAR) == day.year &&
        cal.get(Calendar.MONTH) == day.month &&
        cal.get(Calendar.DAY_OF_MONTH) == day.day
}

@Composable
private fun DrawerMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = DrawerBubbleBlue.copy(alpha = alpha),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1C1E).copy(alpha = alpha)
        )
    }
}

@Composable
private fun ExportDataDialog(
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("导出数据", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1C1E))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "导出 JSON 格式，包含创建时间、完成时间、延迟、分类标签，便于自行分析。",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DrawerBubbleBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("一键导出 JSON", color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("取消", color = Color(0xFF64748B))
                }
            }
        }
    }
}

@Composable
private fun CompletedTasksDayDialog(
    day: CalendarDay,
    tasks: List<MemoUi>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .width((LocalConfiguration.current.screenWidthDp * 0.85f).dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = String.format(Locale.CHINA, "%d月%d日 已完成任务", day.month + 1, day.day),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (tasks.isEmpty()) {
                    Text("该日暂无已完成的任务", fontSize = 13.sp, color = Color(0xFF94A3B8))
                } else {
                    tasks.forEach { task ->
                        Text(
                            text = "• ${task.summary.ifBlank { task.userInput }}",
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DrawerBubbleBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("知道了", color = Color.White)
                }
            }
        }
    }
}

private fun copyAndShareJson(context: Context, json: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("MemoAI Export", json))
    runCatching {
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, json)
            putExtra(Intent.EXTRA_SUBJECT, "MemoAI 数据导出")
        }
        context.startActivity(Intent.createChooser(share, "分享导出数据"))
    }
}
