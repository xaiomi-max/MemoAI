package com.memoai.app.home

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class WishItem(
    val id: String,
    val title: String,
    val description: String,
    val link: String?,
    val imageUrl: String?,
    val priceShells: Int,
    val savedShells: Int
) {
    val isAffordable: Boolean = savedShells >= priceShells
    val progress: Float =
        if (priceShells > 0) savedShells.toFloat() / priceShells.toFloat() else 0f
}

class WishStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("wish_exchange", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    fun getShellBalance(): Int = prefs.getInt("shell_balance", 0)

    fun setShellBalance(value: Int) {
        prefs.edit().putInt("shell_balance", value.coerceAtLeast(0)).apply()
    }

    fun loadWishes(): List<WishItem> {
        val raw = prefs.getString("wishes_json", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        WishItem(
                            id = o.getString("id"),
                            title = o.getString("title"),
                            description = o.getString("description"),
                            link = o.optString("link").takeIf { it.isNotBlank() },
                            imageUrl = o.optString("imageUrl").takeIf { it.isNotBlank() },
                            priceShells = o.getInt("priceShells"),
                            savedShells = o.getInt("savedShells")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveWishes(wishes: List<WishItem>) {
        val arr = JSONArray()
        wishes.forEach { wish ->
            arr.put(
                JSONObject().apply {
                    put("id", wish.id)
                    put("title", wish.title)
                    put("description", wish.description)
                    put("link", wish.link ?: "")
                    put("imageUrl", wish.imageUrl ?: "")
                    put("priceShells", wish.priceShells)
                    put("savedShells", wish.savedShells)
                }
            )
        }
        prefs.edit().putString("wishes_json", arr.toString()).apply()
    }

    suspend fun createWishFromInput(input: String): WishItem = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        require(trimmed.isNotBlank()) { "请输入心愿内容" }

        val link = extractUrl(trimmed)
        val withoutUrl = trimmed.replace(link ?: "", "").trim()
        val price = extractPrice(trimmed) ?: 100
        val title = extractTitle(withoutUrl, link)
        val imageUrl = link?.let { fetchProductImage(it) }

        val wish = WishItem(
            id = UUID.randomUUID().toString(),
            title = title,
            description = trimmed,
            link = link,
            imageUrl = imageUrl,
            priceShells = price,
            savedShells = 0
        )
        saveWishes(loadWishes() + wish)
        wish
    }

    fun redeemWish(id: String): RedeemResult {
        val wishes = loadWishes()
        val wish = wishes.firstOrNull { it.id == id } ?: return RedeemResult.NotFound
        val balance = getShellBalance()
        if (balance < wish.priceShells) return RedeemResult.NotEnoughBalance
        setShellBalance(balance - wish.priceShells)
        saveWishes(wishes.filter { it.id != id })
        return RedeemResult.Success(wish)
    }

    private fun extractUrl(text: String): String? =
        Regex("https?://[^\\s\"'<>]+").find(text)?.value

    private fun extractPrice(text: String): Int? {
        val patterns = listOf(
            Regex("(\\d+)\\s*🐚"),
            Regex("(\\d+)\\s*贝壳"),
            Regex("(\\d+)\\s*⭐"),
            Regex("价格[：:]?\\s*(\\d+)"),
            Regex("(\\d+)\\s*元")
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) return match.groupValues[1].toIntOrNull()
        }
        return Regex("\\d+").findAll(text).map { it.value.toInt() }.lastOrNull()
    }

    private fun extractTitle(text: String, link: String?): String {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val candidates = lines.filter { line ->
            !line.contains("http") && !line.matches(Regex("^\\d+\\s*🐚?$"))
        }
        val raw = candidates.firstOrNull()
            ?: lines.firstOrNull()
            ?: if (link != null) "商品心愿" else "心愿奖励"
        return raw
            .replace(Regex("\\d+\\s*🐚"), "")
            .replace(Regex("\\d+\\s*贝壳"), "")
            .trim()
            .take(40)
            .ifBlank { "心愿奖励" }
    }

    private fun extractMetaContent(html: String, property: String): String? {
        val patterns = listOf(
            Regex(
                "<meta[^>]+property=[\"']$property[\"'][^>]+content=[\"']([^\"']+)[\"']",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+property=[\"']$property[\"']",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                "<meta[^>]+name=[\"']$property[\"'][^>]+content=[\"']([^\"']+)[\"']",
                RegexOption.IGNORE_CASE
            )
        )
        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    suspend fun refreshWishImage(wishId: String): WishItem? = withContext(Dispatchers.IO) {
        val wishes = loadWishes()
        val wish = wishes.firstOrNull { it.id == wishId } ?: return@withContext null
        if (!wish.imageUrl.isNullOrBlank()) return@withContext wish
        val link = wish.link ?: return@withContext null
        val imageUrl = fetchProductImage(link) ?: return@withContext null
        val updated = wish.copy(imageUrl = imageUrl)
        saveWishes(wishes.map { if (it.id == wishId) updated else it })
        updated
    }

    private fun fetchProductImage(url: String): String? {
        fetchProductHtml(url)?.let { html ->
            listOfNotNull(
                extractMetaContent(html, "og:image"),
                extractMetaContent(html, "twitter:image"),
                extractTaobaoImage(html),
                extractJsonLdImage(html)
            ).map { normalizeImageUrl(it) }
                .firstOrNull { it.startsWith("http") }
                ?.let { return it }
        }
        return null
    }

    private fun fetchProductHtml(url: String): String? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) " +
                    "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"
            )
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .header("Referer", "https://www.taobao.com/")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.string()
        }
    }.getOrNull()

    private fun extractTaobaoImage(html: String): String? {
        val patterns = listOf(
            Regex("""["']picUrl["']\s*:\s*["']([^"']+)["']"""),
            Regex("""["']pic["']\s*:\s*["']([^"']+)["']"""),
            Regex("""["']itemImg["']\s*:\s*["']([^"']+)["']"""),
            Regex("""(https?://img\.alicdn\.com/[^"'\s<>]+)"""),
            Regex("""(//img\.alicdn\.com/[^"'\s<>]+)"""),
            Regex("""data-src=["']([^"']*alicdn[^"']+)["']"""),
            Regex("""data-ks-lazyload=["']([^"']*alicdn[^"']+)["']""")
        )
        for (pattern in patterns) {
            val match = pattern.find(html) ?: continue
            val value = match.groupValues.getOrNull(1) ?: match.value
            if (value.contains("alicdn") || value.startsWith("http") || value.startsWith("//")) {
                return value
            }
        }
        return null
    }

    private fun extractJsonLdImage(html: String): String? {
        val pattern = Regex(
            """<script[^>]+type=["']application/ld\+json["'][^>]*>([\s\S]*?)</script>""",
            RegexOption.IGNORE_CASE
        )
        for (match in pattern.findAll(html)) {
            val jsonText = match.groupValues[1].trim()
            runCatching {
                val json = JSONObject(jsonText)
                json.optString("image").takeIf { it.isNotBlank() }?.let { return it }
                json.optJSONArray("image")?.optString(0)?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return null
    }

    private fun normalizeImageUrl(raw: String): String {
        var url = raw.trim()
            .replace("\\u002F", "/")
            .replace("\\/", "/")
        if (url.startsWith("//")) url = "https:$url"
        if (url.startsWith("http://")) url = "https://${url.removePrefix("http://")}"
        return url
    }
}

sealed class RedeemResult {
    data class Success(val wish: WishItem) : RedeemResult()
    data object NotFound : RedeemResult()
    data object NotEnoughBalance : RedeemResult()
}
