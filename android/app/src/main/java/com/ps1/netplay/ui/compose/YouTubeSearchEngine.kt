package com.ps1.netplay.ui.compose

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object YouTubeSearchEngine {
    private const val TAG = "YouTubeSearchEngine"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // 1. Live Real-time YouTube Autocomplete Suggestions (Google Suggest API)
    suspend fun getLiveSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()
        try {
            val encodedQuery = URLEncoder.encode(q, "UTF-8")
            val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&hl=ar&q=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36")
                .header("Accept-Language", "ar,en;q=0.9")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(body)
                if (jsonArray.length() > 1) {
                    val suggestionsArray = jsonArray.getJSONArray(1)
                    val result = mutableListOf<String>()
                    for (i in 0 until suggestionsArray.length()) {
                        val text = suggestionsArray.optString(i, "")
                        if (text.isNotEmpty()) {
                            result.add(text)
                        }
                    }
                    return@withContext result
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get live suggestions: ${e.message}")
        }
        return@withContext emptyList()
    }

    // 2. Live Real-time YouTube Search Results (+20-30 real videos without any restriction)
    suspend fun searchRealYouTube(query: String): List<YouTubeVideoItem> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty() || q == "الكل") return@withContext emptyList()

        // Strategy A: YouTube InnerTube JSON API (Fastest, direct JSON, no HTML parsing needed)
        try {
            val innerTubeResults = searchViaInnerTube(q)
            if (innerTubeResults.isNotEmpty()) {
                return@withContext innerTubeResults
            }
        } catch (e: Exception) {
            Log.w(TAG, "InnerTube search fallback triggered: ${e.message}")
        }

        // Strategy B: YouTube HTML search with multiline regex
        try {
            val htmlResults = searchViaHtml(q)
            if (htmlResults.isNotEmpty()) {
                return@withContext htmlResults
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTML search failed: ${e.message}")
        }

        return@withContext emptyList()
    }

    private fun searchViaInnerTube(query: String): List<YouTubeVideoItem> {
        val url = "https://www.youtube.com/youtubei/v1/search?prettyPrint=false"
        val payload = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB")
                    put("clientVersion", "2.20240101.01.00")
                    put("hl", "ar")
                    put("gl", "US")
                })
            })
            put("query", query)
        }

        val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .header("Content-Type", "application/json")
            .header("Accept-Language", "ar,en;q=0.9")
            .header("Cookie", "PREF=hl=ar&gl=US; CONSENT=YES+cb")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val json = JSONObject(body)
            return parseYouTubeInitialData(json)
        }
    }

    private fun searchViaHtml(query: String): List<YouTubeVideoItem> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.youtube.com/results?search_query=$encodedQuery"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .header("Accept-Language", "ar,en;q=0.9")
            .header("Cookie", "PREF=hl=ar&gl=US; CONSENT=YES+cb")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val html = response.body?.string() ?: return emptyList()

            // Support both single-line and multiline ytInitialData JSON
            val pattern = Regex("""ytInitialData\s*=\s*(\{[\s\S]+?\});\s*</script>""")
            val match = pattern.find(html)
            if (match != null) {
                val jsonStr = match.groupValues[1]
                val json = JSONObject(jsonStr)
                return parseYouTubeInitialData(json)
            }
        }
        return emptyList()
    }

    private fun parseYouTubeInitialData(json: JSONObject): List<YouTubeVideoItem> {
        val results = mutableListOf<YouTubeVideoItem>()
        try {
            val contents = json.optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return emptyList()

            for (i in 0 until contents.length()) {
                val section = contents.optJSONObject(i)?.optJSONObject("itemSectionRenderer") ?: continue
                val items = section.optJSONArray("contents") ?: continue
                for (j in 0 until items.length()) {
                    val item = items.optJSONObject(j) ?: continue

                    // Direct video renderer
                    val v = item.optJSONObject("videoRenderer") ?: item.optJSONObject("compactVideoRenderer")
                    if (v != null) {
                        parseSingleVideo(v)?.let { results.add(it) }
                    }

                    // Shelf renderer (items inside shelves)
                    val shelfItems = item.optJSONObject("shelfRenderer")
                        ?.optJSONObject("content")
                        ?.optJSONObject("verticalListRenderer")
                        ?.optJSONArray("items")
                    if (shelfItems != null) {
                        for (k in 0 until shelfItems.length()) {
                            val subV = shelfItems.optJSONObject(k)?.optJSONObject("videoRenderer")
                            if (subV != null) {
                                parseSingleVideo(subV)?.let { results.add(it) }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing YouTube data: ${e.message}")
        }
        return results
    }

    private fun parseSingleVideo(v: JSONObject): YouTubeVideoItem? {
        val videoId = v.optString("videoId")
        if (videoId.isNullOrEmpty()) return null

        val titleObj = v.optJSONObject("title")
        val title = titleObj?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: titleObj?.optString("simpleText") ?: "فيديو يوتيوب"

        val ownerObj = v.optJSONObject("ownerText") ?: v.optJSONObject("shortBylineText")
        val channel = ownerObj?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: ownerObj?.optString("simpleText") ?: "قناة يوتيوب"

        val lengthObj = v.optJSONObject("lengthText")
        val duration = lengthObj?.optString("simpleText") ?: "فيديو"

        val viewObj = v.optJSONObject("viewCountText")
        val views = viewObj?.optString("simpleText")
            ?: viewObj?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: "مشاهدات عالية"

        val pubObj = v.optJSONObject("publishedTimeText")
        val time = pubObj?.optString("simpleText") ?: "مؤخراً"

        val thumbUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        return YouTubeVideoItem(
            id = videoId,
            title = title,
            channelTitle = channel,
            duration = duration,
            viewCount = views,
            publishedTime = time,
            thumbnailUrl = thumbUrl,
            category = "يوتيوب",
            isLive = duration.contains("بث") || duration.contains("مباشر") || duration.contains("Live", ignoreCase = true)
        )
    }
}
