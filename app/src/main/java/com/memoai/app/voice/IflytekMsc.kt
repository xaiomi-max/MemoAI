package com.memoai.app.voice

import android.content.Context
import com.memoai.app.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.lang.reflect.Proxy

internal object IflytekMsc {
    private var initialized = false
    private var recognizer: Any? = null
    private var resultBuilder = StringBuilder()
    private var pendingResult: CompletableDeferred<Result<String>>? = null

    fun isSdkPresent(): Boolean = runCatching {
        Class.forName("com.iflytek.cloud.SpeechRecognizer")
    }.isSuccess

    fun isAvailable(): Boolean = isSdkPresent() && initialized

    fun initialize(context: Context) {
        if (initialized) return
        runCatching {
            val speechUtility = Class.forName("com.iflytek.cloud.SpeechUtility")
            val speechConstant = Class.forName("com.iflytek.cloud.SpeechConstant")
            val appIdKey = speechConstant.getField("APPID").get(null) as String
            val param = "$appIdKey=${BuildConfig.IFLYTEK_APP_ID}"
            speechUtility.getMethod("createUtility", Context::class.java, String::class.java)
                .invoke(null, context.applicationContext, param)
            initialized = true
        }
    }

    suspend fun startListening(context: Context): Result<Unit> = runCatching {
        suspendCancellableCoroutine { cont ->
            if (!isSdkPresent()) {
                cont.resumeWith(Result.failure(IllegalStateException("讯飞 MSC SDK 未集成")))
                return@suspendCancellableCoroutine
            }

            resultBuilder = StringBuilder()
            pendingResult = CompletableDeferred()

            runCatching {
            val speechRecognizerClass = Class.forName("com.iflytek.cloud.SpeechRecognizer")
            val speechConstant = Class.forName("com.iflytek.cloud.SpeechConstant")
            val initListenerClass = Class.forName("com.iflytek.cloud.InitListener")
            val recognizerListenerClass = Class.forName("com.iflytek.cloud.RecognizerListener")
            val errorCodeClass = Class.forName("com.iflytek.cloud.ErrorCode")
            val success = errorCodeClass.getField("SUCCESS").getInt(null)

            val initListener = Proxy.newProxyInstance(
                initListenerClass.classLoader,
                arrayOf(initListenerClass)
            ) { _, _, args ->
                val code = args?.firstOrNull() as? Int ?: -1
                if (code != success) {
                    if (cont.isActive) cont.resumeWith(Result.failure(IllegalStateException("讯飞识别初始化失败: $code")))
                    return@newProxyInstance null
                }

                fun setParam(key: String, value: String) {
                    recognizer?.javaClass?.getMethod("setParameter", String::class.java, String::class.java)
                        ?.invoke(recognizer, key, value)
                }

                setParam(speechConstant.getField("ENGINE_TYPE").get(null) as String, speechConstant.getField("TYPE_CLOUD").get(null) as String)
                setParam(speechConstant.getField("RESULT_TYPE").get(null) as String, "json")
                setParam(speechConstant.getField("LANGUAGE").get(null) as String, "zh_cn")
                setParam(speechConstant.getField("ACCENT").get(null) as String, "mandarin")
                setParam(speechConstant.getField("ASR_PTT").get(null) as String, "1")

                val listener = buildRecognizerListener(recognizerListenerClass)
                val ret = recognizer?.javaClass
                    ?.getMethod("startListening", recognizerListenerClass)
                    ?.invoke(recognizer, listener) as? Int ?: -1
                if (ret != success) {
                    if (cont.isActive) cont.resumeWith(Result.failure(IllegalStateException("讯飞识别启动失败: $ret")))
                } else if (cont.isActive) {
                    cont.resume(Unit)
                }
                null
            }

            recognizer = speechRecognizerClass
                .getMethod("createRecognizer", Context::class.java, initListenerClass)
                .invoke(null, context.applicationContext, initListener)
        }.onFailure {
            if (cont.isActive) cont.resumeWith(Result.failure(it))
        }

            cont.invokeOnCancellation { cancelListening() }
        }
    }

    fun stopListening() {
        runCatching {
            recognizer?.javaClass?.getMethod("stopListening")?.invoke(recognizer)
        }
    }

    suspend fun awaitResult(): Result<String> {
        return pendingResult?.await() ?: Result.failure(IllegalStateException("讯飞识别未启动"))
    }

    fun cancelListening() {
        runCatching {
            recognizer?.javaClass?.getMethod("cancel")?.invoke(recognizer)
            recognizer?.javaClass?.getMethod("destroy")?.invoke(recognizer)
        }
        recognizer = null
        pendingResult?.complete(Result.failure(IllegalStateException("已取消")))
        pendingResult = null
        resultBuilder = StringBuilder()
    }

    private fun buildRecognizerListener(recognizerListenerClass: Class<*>): Any {
        return Proxy.newProxyInstance(
            recognizerListenerClass.classLoader,
            arrayOf(recognizerListenerClass)
        ) { _, method, args ->
            when (method.name) {
                "onResult" -> {
                    val resultObj = args?.getOrNull(0) ?: return@newProxyInstance null
                    val isLast = args.getOrNull(1) as? Boolean ?: true
                    val json = resultObj.javaClass.getMethod("getResultString").invoke(resultObj) as? String
                    if (!json.isNullOrBlank()) {
                        resultBuilder.append(IflytekJsonParser.parseIatText(json))
                    }
                    if (isLast) {
                        val text = resultBuilder.toString().trim()
                        pendingResult?.complete(
                            if (text.isNotBlank()) Result.success(text)
                            else Result.failure(IllegalStateException("未识别到语音内容"))
                        )
                        pendingResult = null
                    }
                }
                "onError" -> {
                    val error = args?.firstOrNull() as? Int ?: -1
                    pendingResult?.complete(Result.failure(IllegalStateException("讯飞识别失败: $error")))
                    pendingResult = null
                }
            }
            null
        }
    }
}
