package com.memoai.app.voice

import android.Manifest
import android.content.pm.PackageManager
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.memoai.app.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val MicButtonShape = RoundedCornerShape(12.dp)

private enum class ActionButtonState {
    Mic,
    Check,
    Loading
}

@Composable
fun HomeVoiceInputButton(
    inputFocused: Boolean,
    inputText: String,
    primaryColor: Color,
    disabledColor: Color,
    onSubmit: () -> Unit,
    onTranscribed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val voiceHelper = remember { VoiceInputHelper() }
    val scaleAnim = remember { Animatable(1f) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var isRecording by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var longPressJob by remember { mutableStateOf<Job?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (!granted) {
            Toast.makeText(context, "需要麦克风权限才能语音输入", Toast.LENGTH_SHORT).show()
        }
    }

    val cancelThresholdPx = with(density) { VoiceInputHelper.CANCEL_SWIPE_DP.dp.toPx() }

    val buttonState = when {
        isLoading -> ActionButtonState.Loading
        inputFocused -> ActionButtonState.Check
        else -> ActionButtonState.Mic
    }

    val checkEnabled = inputFocused && inputText.isNotBlank() && !isLoading
    val backgroundColor = when (buttonState) {
        ActionButtonState.Check -> if (checkEnabled) primaryColor else disabledColor
        else -> primaryColor
    }

    fun animateScale(target: Float, durationMs: Int) {
        scope.launch {
            scaleAnim.animateTo(
                targetValue = target,
                animationSpec = if (target > 1f) {
                    spring(dampingRatio = 0.45f, stiffness = 500f)
                } else {
                    tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
                }
            )
        }
    }

    fun startRecording() {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (SparkChainAsr.isAvailable()) {
            scope.launch {
                val started = SparkChainAsr.startListening(context)
                started.onSuccess {
                    isRecording = true
                    animateScale(1.3f, 200)
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }.onFailure {
                    Toast.makeText(context, "无法开始语音识别", Toast.LENGTH_SHORT).show()
                }
            }
            return
        }
        runCatching {
            voiceHelper.startRecording(context)
            isRecording = true
            animateScale(1.3f, 200)
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }.onFailure {
            Toast.makeText(context, "无法开始录音", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecordingAndTranscribe() {
        if (SparkChainAsr.isAvailable() && isRecording) {
            isRecording = false
            animateScale(1f, 250)
            isLoading = true
            scope.launch {
                SparkChainAsr.stopListening()
                val result = SparkChainAsr.awaitResult()
                isLoading = false
                result.fold(
                    onSuccess = { text ->
                        if (text.isNotBlank()) onTranscribed(text)
                    },
                    onFailure = {
                        val message = if (it is TimeoutCancellationException) {
                            "语音识别超时，请输入文字"
                        } else {
                            "语音识别失败，请输入文字"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
            return
        }
        val file = voiceHelper.stopRecording()
        isRecording = false
        animateScale(1f, 250)
        if (file == null) return
        isLoading = true
        scope.launch {
            val result = AsrClient.transcribe(file)
            file.delete()
            isLoading = false
            result.fold(
                onSuccess = { text ->
                    if (text.isNotBlank()) {
                        onTranscribed(text)
                    }
                },
                onFailure = {
                    val message = if (it is TimeoutCancellationException) {
                        "语音识别超时，请输入文字"
                    } else {
                        "语音识别失败，请输入文字"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    fun cancelRecording(showToast: Boolean) {
        longPressJob?.cancel()
        longPressJob = null
        if (SparkChainAsr.isAvailable()) {
            SparkChainAsr.cancelListening()
        } else {
            voiceHelper.cancelRecording()
        }
        if (isRecording) {
            isRecording = false
            animateScale(1f, 250)
            if (showToast) {
                Toast.makeText(context, "已取消", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            RecordingRipple(primaryColor = primaryColor)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scaleAnim.value)
                .clip(MicButtonShape)
                .background(backgroundColor)
                .then(
                    when (buttonState) {
                        ActionButtonState.Mic -> Modifier.pointerInput(hasAudioPermission, isLoading) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var cancelled = false
                                var recordingStarted = false
                                val downY = down.position.y

                                longPressJob?.cancel()
                                longPressJob = scope.launch {
                                    delay(VoiceInputHelper.LONG_PRESS_DELAY_MS)
                                    if (!cancelled) {
                                        recordingStarted = true
                                        startRecording()
                                    }
                                }

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    if (change.position.y - downY < -cancelThresholdPx) {
                                        cancelled = true
                                        cancelRecording(showToast = true)
                                        break
                                    }
                                    if (change.positionChange() != androidx.compose.ui.geometry.Offset.Zero) {
                                        change.consume()
                                    }
                                }

                                longPressJob?.cancel()
                                longPressJob = null
                                if (cancelled) return@awaitEachGesture
                                if (recordingStarted) {
                                    stopRecordingAndTranscribe()
                                }
                            }
                        }
                        ActionButtonState.Check -> Modifier.clickable(
                            enabled = checkEnabled,
                            onClick = onSubmit
                        )
                        ActionButtonState.Loading -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (buttonState) {
                ActionButtonState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
                ActionButtonState.Check -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = "提交",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                ActionButtonState.Mic -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = "语音输入",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingRipple(primaryColor: Color) {
    val transition = rememberInfiniteTransition(label = "recordingRipple")
    val rippleScale by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )
    val rippleAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha"
    )
    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(rippleScale)
            .alpha(rippleAlpha)
            .clip(MicButtonShape)
            .background(primaryColor.copy(alpha = 0.35f))
    )
}
