package com.aether.assistant.core.web

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

/** Free web access via DuckDuckGo HTML + direct page fetch. No paid search API. */
class WebRepository {
    private val client = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun search(query: String): Result<List<WebResult>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://html.duckduckgo.com/html/?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
            val html = get(url)
            val doc = Jsoup.parse(html)
            doc.select(".result").mapNotNull { el ->
                val a = el.selectFirst(".result__a") ?: return@mapNotNull null
                val snippet = el.selectFirst(".result__snippet")?.text().orEmpty()
                WebResult(a.text(), a.absUrl("href"), snippet)
            }.take(8)
        }
    }

    suspend fun readPage(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(url.startsWith("http://") || url.startsWith("https://")) { "Only http(s) URLs are allowed" }
            val html = get(url)
            Jsoup.parse(html).body()?.text()?.take(12000) ?: "(empty page)"
        }
    }

    private fun get(url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "AetherAssistant/1.0 (+local-first android assistant)")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            return resp.body?.string() ?: ""
        }
    }

    data class WebResult(val title: String, val url: String, val snippet: String)
}
