package com.memoai.app.voice

import org.json.JSONArray
import org.json.JSONObject

internal object IflytekJsonParser {
    fun parseIatText(raw: String): String {
        if (raw.isBlank()) return ""
        return runCatching {
            val root = JSONObject(raw.trim())
            val ws = root.optJSONArray("ws") ?: return@runCatching ""
            parseWords(ws)
        }.getOrElse {
            runCatching {
                val array = JSONArray(raw.trim())
                buildString {
                    for (i in 0 until array.length()) {
                        append(parseIatText(array.optString(i)))
                    }
                }
            }.getOrDefault(raw)
        }
    }

    private fun parseWords(ws: JSONArray): String {
        val builder = StringBuilder()
        for (i in 0 until ws.length()) {
            val cw = ws.optJSONObject(i)?.optJSONArray("cw") ?: continue
            for (j in 0 until cw.length()) {
                val word = cw.optJSONObject(j)?.optString("w").orEmpty()
                if (word.isNotBlank()) builder.append(word)
            }
        }
        return builder.toString()
    }
}
