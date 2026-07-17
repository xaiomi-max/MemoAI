package com.memoai.app.voice

import android.content.Context
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.FileOutputStream

class VoiceInputHelper {
    private var audioRecord: android.media.AudioRecord? = null
    private var audioFile: File? = null
    private var recordingStartTime = 0L
    private var recording = false
    private var recordThread: Thread? = null

    val isRecording: Boolean
        get() = recording

    fun startRecording(context: Context) {
        stopInternal(deleteFile = true)
        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.pcm")
        val minBuffer = android.media.AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        require(minBuffer > 0) { "设备不支持 PCM 录音" }

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.media.AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    android.media.AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(minBuffer * 2)
                .build()
        } else {
            @Suppress("DEPRECATION")
            android.media.AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer * 2
            )
        }

        audioFile = file
        audioRecord = recorder
        recordingStartTime = System.currentTimeMillis()
        recording = true
        recorder.startRecording()

        recordThread = Thread {
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(FRAME_SIZE)
                while (recording) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) output.write(buffer, 0, read)
                }
            }
        }.also { it.start() }
    }

    fun stopRecording(): File? {
        val file = audioFile
        val duration = System.currentTimeMillis() - recordingStartTime
        stopInternal(deleteFile = false)
        if (file == null || !file.exists() || duration < MIN_RECORD_DURATION_MS || file.length() <= 0L) {
            file?.delete()
            return null
        }
        return file
    }

    fun cancelRecording() {
        stopInternal(deleteFile = true)
    }

    private fun stopInternal(deleteFile: Boolean) {
        recording = false
        runCatching { recordThread?.join(500) }
        recordThread = null
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
        if (deleteFile) {
            audioFile?.delete()
        }
        audioFile = null
        recordingStartTime = 0L
    }

    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val FRAME_SIZE = 1280
        const val MIN_RECORD_DURATION_MS = 1000L
        const val LONG_PRESS_DELAY_MS = 200L
        const val CANCEL_SWIPE_DP = 50f
    }
}
