package com.memoai.app.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import com.iflytek.sparkchain.core.SparkChain
import com.iflytek.sparkchain.core.SparkChainConfig
import com.iflytek.sparkchain.core.asr.ASR
import com.iflytek.sparkchain.core.asr.AsrCallbacks
import com.memoai.app.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal object SparkChainAsr {
    private var initialized = false
    private var asr: ASR? = null
    private var pendingResult: CompletableDeferred<Result<String>>? = null
    private var latestText = StringBuilder()
    private var audioRecord: AudioRecord? = null
    private var recordThread: Thread? = null
    private val writeEnabled = AtomicBoolean(false)
    private val sessionCounter = AtomicInteger(0)

    fun isSdkPresent(): Boolean = runCatching {
        Class.forName("com.iflytek.sparkchain.core.SparkChain")
    }.isSuccess

    fun isAvailable(): Boolean =
        isSdkPresent() && initialized && BuildConfig.IFLYTEK_API_KEY.isNotBlank() &&
            BuildConfig.IFLYTEK_API_SECRET.isNotBlank()

    fun initialize(context: Context) {
        if (initialized || !isSdkPresent()) return
        if (BuildConfig.IFLYTEK_API_KEY.isBlank() || BuildConfig.IFLYTEK_API_SECRET.isBlank()) return

        val workDir = File(context.filesDir, "iflytek").apply { mkdirs() }
        val config = SparkChainConfig.builder()
            .appID(BuildConfig.IFLYTEK_APP_ID)
            .apiKey(BuildConfig.IFLYTEK_API_KEY)
            .apiSecret(BuildConfig.IFLYTEK_API_SECRET)
            .workDir(workDir.absolutePath)

        val ret = SparkChain.getInst().init(context.applicationContext, config)
        if (ret == 0) {
            initialized = true
        }
    }

    fun startListening(context: Context): Result<Unit> = runCatching {
        require(isAvailable()) { "SparkChain 未初始化或缺少 API 密钥" }
        require(!writeEnabled.get()) { "语音识别已在进行中" }

        latestText = StringBuilder()
        pendingResult = CompletableDeferred()

        if (asr == null) {
            asr = ASR().also { it.registerCallbacks(asrCallbacks) }
        }

        val recognizer = asr ?: error("ASR 实例创建失败")
        recognizer.language("zh_cn")
        recognizer.domain("iat")
        recognizer.accent("mandarin")
        recognizer.vinfo(true)
        recognizer.dwa("wpgs")

        val sessionId = sessionCounter.incrementAndGet().toString()
        val ret = recognizer.start(sessionId)
        if (ret != 0) {
            pendingResult = null
            error("识别开启失败，错误码: $ret")
        }

        writeEnabled.set(true)
        startAudioCapture()
    }

    fun stopListening(): Result<Unit> = runCatching {
        writeEnabled.set(false)
        stopAudioCapture()
        asr?.stop(false)
    }

    fun cancelListening() {
        writeEnabled.set(false)
        stopAudioCapture()
        runCatching { asr?.stop(true) }
        pendingResult?.complete(Result.failure(IllegalStateException("已取消")))
        pendingResult = null
        latestText = StringBuilder()
    }

    suspend fun awaitResult(timeoutMs: Long = 15_000L): Result<String> {
        val deferred = pendingResult ?: return Result.failure(IllegalStateException("未开始识别"))
        return runCatching {
            withTimeout(timeoutMs) {
                deferred.await().getOrThrow()
            }
        }
    }

    private val asrCallbacks = object : AsrCallbacks {
        override fun onResult(result: ASR.ASRResult, userContext: Any?) {
            val status = result.status
            val text = result.bestMatchText.orEmpty()
            when (status) {
                0 -> latestText = StringBuilder(text)
                2 -> {
                    latestText = StringBuilder(text)
                    finishSession(latestText.toString())
                }
                else -> if (text.isNotBlank()) latestText = StringBuilder(text)
            }
        }

        override fun onError(error: ASR.ASRError, userContext: Any?) {
            finishSession(
                error = IllegalStateException(
                    "识别出错: ${error.code} ${error.errMsg}"
                )
            )
        }

        override fun onBeginOfSpeech() = Unit

        override fun onEndOfSpeech() = Unit
    }

    private fun finishSession(text: String? = null, error: Throwable? = null) {
        writeEnabled.set(false)
        val deferred = pendingResult ?: return
        pendingResult = null
        when {
            error != null -> deferred.complete(Result.failure(error))
            !text.isNullOrBlank() -> deferred.complete(Result.success(text))
            else -> deferred.complete(Result.failure(IllegalStateException("未识别到语音内容")))
        }
    }

    private fun startAudioCapture() {
        stopAudioCapture()

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        require(minBuffer > 0) { "设备不支持 PCM 录音" }

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(minBuffer * 2)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer * 2
            )
        }

        audioRecord = recorder
        recorder.startRecording()

        recordThread = Thread {
            val buffer = ByteArray(FRAME_SIZE)
            while (writeEnabled.get()) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read <= 0) continue
                val chunk = if (read == buffer.size) buffer else buffer.copyOf(read)
                val ret = asr?.write(chunk) ?: break
                if (ret != 0) break
            }
        }.also { it.start() }
    }

    private fun stopAudioCapture() {
        runCatching { recordThread?.join(500) }
        recordThread = null
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
    }

    private const val SAMPLE_RATE = 16_000
    private const val FRAME_SIZE = 1280
}
