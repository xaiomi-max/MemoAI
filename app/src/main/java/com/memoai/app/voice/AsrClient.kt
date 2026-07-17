package com.memoai.app.voice

import android.util.Base64
import com.memoai.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.suspendCancellableCoroutine

object AsrClient {
    private const val TIMEOUT_MS = 20_000L
    private const val HOST = "iat-api.xfyun.cn"
    private const val PATH = "/v2/iat"
    private const val FRAME_SIZE = 1280

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun transcribe(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            withTimeout(TIMEOUT_MS) {
                if (IflytekMsc.isAvailable()) {
                    error("请使用讯飞 MSC 实时听写")
                }
                transcribeWithWebApi(audioFile)
            }
        }
    }

    private suspend fun transcribeWithWebApi(audioFile: File): String {
        val apiKey = BuildConfig.IFLYTEK_API_KEY.trim()
        val apiSecret = BuildConfig.IFLYTEK_API_SECRET.trim()
        if (apiKey.isBlank() || apiSecret.isBlank()) {
            error("请在 local.properties 配置 IFLYTEK_API_KEY 和 IFLYTEK_API_SECRET")
        }
        val pcm = audioFile.readBytes()
        if (pcm.isEmpty()) error("录音为空")

        return suspendCancellableCoroutine { cont ->
            val authUrl = buildAuthUrl(apiKey, apiSecret)
            val request = Request.Builder().url(authUrl).build()
            val resultBuilder = StringBuilder()
            var finished = false

            fun complete(result: Result<String>) {
                if (finished) return
                finished = true
                cont.resumeWith(result)
            }

            val webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    sendAudioFrames(webSocket, pcm)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    runCatching {
                        val root = JSONObject(text)
                        val code = root.optInt("code", -1)
                        if (code != 0) {
                            complete(Result.failure(IllegalStateException(root.optString("message", "讯飞识别失败"))))
                            webSocket.close(1000, null)
                            return
                        }
                        val data = root.optJSONObject("data") ?: return
                        val result = data.optJSONObject("result")
                        val ws = result?.optJSONArray("ws")
                        if (ws != null) {
                            resultBuilder.append(IflytekJsonParser.parseIatText(result.toString()))
                        }
                        if (data.optInt("status") == 2) {
                            val finalText = resultBuilder.toString().trim()
                            if (finalText.isBlank()) {
                                complete(Result.failure(IllegalStateException("未识别到语音内容")))
                            } else {
                                complete(Result.success(finalText))
                            }
                            webSocket.close(1000, null)
                        }
                    }.onFailure {
                        complete(Result.failure(it))
                        webSocket.close(1000, null)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    complete(Result.failure(t))
                }
            })

            cont.invokeOnCancellation {
                webSocket.cancel()
            }
        }
    }

    private fun sendAudioFrames(webSocket: WebSocket, pcm: ByteArray) {
        var offset = 0
        var status = 0
        while (offset < pcm.size) {
            val end = minOf(offset + FRAME_SIZE, pcm.size)
            val chunk = pcm.copyOfRange(offset, end)
            val frameStatus = when {
                offset == 0 -> 0
                end >= pcm.size -> 2
                else -> 1
            }
            webSocket.send(buildAudioFrame(chunk, frameStatus, status == 0))
            status = 1
            offset = end
            if (frameStatus != 2) {
                Thread.sleep(40)
            }
        }
        if (pcm.isEmpty()) {
            webSocket.send(buildAudioFrame(ByteArray(0), 2, true))
        } else if (offset >= pcm.size && pcm.size % FRAME_SIZE == 0) {
            webSocket.send(JSONObject().put("data", JSONObject().put("status", 2)).toString())
        }
    }

    private fun buildAudioFrame(chunk: ByteArray, status: Int, includeBusiness: Boolean): String {
        val root = JSONObject()
        if (includeBusiness) {
            root.put("common", JSONObject().put("app_id", BuildConfig.IFLYTEK_APP_ID))
            root.put(
                "business",
                JSONObject()
                    .put("language", "zh_cn")
                    .put("domain", "iat")
                    .put("accent", "mandarin")
                    .put("ptt", 1)
            )
        }
        root.put(
            "data",
            JSONObject()
                .put("status", status)
                .put("format", "audio/L16;rate=16000")
                .put("encoding", "raw")
                .put("audio", Base64.encodeToString(chunk, Base64.NO_WRAP))
        )
        return root.toString()
    }

    private fun buildAuthUrl(apiKey: String, apiSecret: String): String {
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
        val date = dateFormat.format(Date())
        val signatureOrigin = "host: $HOST\ndate: $date\nGET $PATH HTTP/1.1"
        val signature = hmacSha256Base64(signatureOrigin, apiSecret)
        val authorizationOrigin =
            "api_key=\"$apiKey\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"$signature\""
        val authorization = Base64.encodeToString(
            authorizationOrigin.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )
        return "wss://$HOST$PATH?" +
            "authorization=${URLEncoder.encode(authorization, "UTF-8")}" +
            "&date=${URLEncoder.encode(date, "UTF-8")}" +
            "&host=$HOST"
    }

    private fun hmacSha256Base64(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return Base64.encodeToString(mac.doFinal(data.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP)
    }
}
