package com.ps1.netplay.ui.compose

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object YouTubeSearchEngine {
    private const val TAG = "YouTubeSearchEngine"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // 1. Live Real-time YouTube Autocomplete Suggestions
    suspend fun getLiveSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()
        try {
            val encodedQuery = URLEncoder.encode(q, "UTF-8")
            val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&hl=ar&q=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
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

    // 2. Live Real-time YouTube Search Results (+20-30 real videos)
    suspend fun searchRealYouTube(query: String): List<YouTubeVideoItem> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty() || q == "الكل") return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(q, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "ar,en;q=0.9")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val html = response.body?.string() ?: return@withContext emptyList()

                // Parse ytInitialData = {...};
                val pattern = Regex("ytInitialData\\s*=\\s*(\\{.+?\\});<\\/script>")
                val match = pattern.find(html)
                if (match != null) {
                    val jsonStr = match.groupValues[1]
                    val json = JSONObject(jsonStr)
                    val parsed = parseYouTubeInitialData(json)
                    if (parsed.isNotEmpty()) {
                        return@withContext parsed
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search real YouTube: ${e.message}")
        }
        return@withContext emptyList()
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
                    val v = item.optJSONObject("videoRenderer") ?: item.optJSONObject("compactVideoRenderer") ?: continue

                    val videoId = v.optString("videoId")
                    if (videoId.isNullOrEmpty()) continue

                    // Title
                    val titleObj = v.optJSONObject("title")
                    val title = titleObj?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        ?: titleObj?.optString("simpleText") ?: "فيديو يوتيوب"

                    // Channel
                    val ownerObj = v.optJSONObject("ownerText") ?: v.optJSONObject("shortBylineText")
                    val channel = ownerObj?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        ?: ownerObj?.optString("simpleText") ?: "قناة يوتيوب"

                    // Duration
                    val lengthObj = v.optJSONObject("lengthText")
                    val duration = lengthObj?.optString("simpleText") ?: "فيديو"

                    // View count
                    val viewObj = v.optJSONObject("viewCountText")
                    val views = viewObj?.optString("simpleText")
                        ?: viewObj?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        ?: "مشاهدات عالية"

                    // Published time
                    val pubObj = v.optJSONObject("publishedTimeText")
                    val time = pubObj?.optString("simpleText") ?: "مؤخراً"

                    // Thumbnail
                    val thumbUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                    results.add(
                        YouTubeVideoItem(
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
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing YouTube initial data: ${e.message}")
        }
        return results
    }
}
