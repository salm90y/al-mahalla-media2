package com.ps1.netplay.ui.compose

import android.content.Context
import android.util.Log
import com.ps1.netplay.network.CloudflareClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class MovieItem(
    val id: String,
    val title: String,
    val name: String,
    val poster: String,
    val streamUrl: String,
    val category: String,
    val duration: String = "2:00:00",
    val year: String = "2024",
    val isSeries: Boolean = false,
    val rating: String = "8.8"
)

object MoviesSearchEngine {
    private const val TAG = "MoviesSearchEngine"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Default Rich Catalog from M3U Source
    val FALLBACK_CATALOG = listOf(
        MovieItem(
            id = "mov_welad_rizk_3",
            title = "ولاد رزق 3: القاضية",
            name = "ولاد رزق 3: القاضية",
            category = "أفلام سينما 2024",
            poster = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80",
            streamUrl = "http://maxshowplayer.site:2052/movie/13968296781874/20098269331298/101.mp4",
            duration = "2:04:15",
            year = "2024",
            isSeries = false,
            rating = "8.9"
        ),
        MovieItem(
            id = "mov_al_hawa_sultan",
            title = "الهوى سلطان",
            name = "الهوى سلطان",
            category = "أفلام سينما 2024",
            poster = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&auto=format&fit=crop&q=80",
            streamUrl = "http://maxshowplayer.site:2052/movie/13968296781874/20098269331298/102.mp4",
            duration = "1:52:30",
            year = "2024",
            isSeries = false,
            rating = "8.4"
        ),
        MovieItem(
            id = "mov_al_hashashin",
            title = "مسلسل الحشاشين (أبطال قلعة ألموت)",
            name = "الحشاشين",
            category = "مسلسلات تاريخية",
            poster = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80",
            streamUrl = "http://maxshowplayer.site:2052/series/13968296781874/20098269331298/201.mp4",
            duration = "الحلقة 1 - 48:20",
            year = "2024",
            isSeries = true,
            rating = "9.3"
        ),
        MovieItem(
            id = "mov_al_atawla",
            title = "مسلسل العتاولة (الجزء الأول)",
            name = "العتاولة",
            category = "مسلسلات أكشن ودراما",
            poster = "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=600&auto=format&fit=crop&q=80",
            streamUrl = "http://maxshowplayer.site:2052/series/13968296781874/20098269331298/202.mp4",
            duration = "الحلقة 1 - 42:10",
            year = "2024",
            isSeries = true,
            rating = "8.7"
        ),
        MovieItem(
            id = "mov_oppenheimer",
            title = "أوبنهايمر (Oppenheimer)",
            name = "Oppenheimer",
            category = "أفلام هوليوود مترجمة",
            poster = "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=600&auto=format&fit=crop&q=80",
            streamUrl = "http://maxshowplayer.site:2052/movie/13968296781874/20098269331298/103.mp4",
            duration = "3:00:22",
            year: "2023",
            isSeries = false,
            rating = "9.1"
        ),
        MovieItem(
            id = "mov_interstellar",
            title = "بين النجوم (Interstellar 4K)",
            name = "Interstellar",
            category = "أفلام خيال علمي",
            poster = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80",
            streamUrl = "http://maxshowplayer.site:2052/movie/13968296781874/20098269331298/104.mp4",
            duration = "2:49:00",
            year = "2014",
            isSeries = false,
            rating = "9.0"
        ),
        MovieItem(
            id = "mov_gladiator_2",
            title = "المحارب 2 (Gladiator II 2024)",
            name = "Gladiator 2",
            category = "أفلام سينما 2024",
            poster = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80",
            streamUrl = "http://maxshowplayer.site:2052/movie/13968296781874/20098269331298/105.mp4",
            duration = "2:28:10",
            year = "2024",
            isSeries = false,
            rating = "8.6"
        ),
        MovieItem(
            id = "mov_dune_2",
            title = "كثيب: الجزء الثاني (Dune: Part Two)",
            name = "Dune 2",
            category = "أفلام خيال علمي",
            poster = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
            streamUrl = "http://maxshowplayer.site:2052/movie/13968296781874/20098269331298/106.mp4",
            duration = "2:46:34",
            year = "2024",
            isSeries = false,
            rating = "8.8"
        ),
        MovieItem(
            id = "mov_al_mousim_al_rabia",
            title = "مسلسل جعفر العمدة",
            name = "جعفر العمدة",
            category = "مسلسلات دراما مصرية",
            poster = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            streamUrl = "http://maxshowplayer.site:2052/series/13968296781874/20098269331298/203.mp4",
            duration = "الحلقة 1 - 44:00",
            year: "2023",
            isSeries = true,
            rating = "8.5"
        ),
        MovieItem(
            id = "mov_doc_universe",
            title = "أسرار الكون والمجرات بجودة فائقة 4K",
            name = "أسرار الكون",
            category = "أفلام وثائقية",
            poster = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600&auto=format&fit=crop&q=80",
            streamUrl = "http://maxshowplayer.site:2052/movie/13968296781874/20098269331298/107.mp4",
            duration = "1:35:10",
            year = "2024",
            isSeries = false,
            rating = "9.2"
        ),
        MovieItem(
            id = "mov_lion_king_mufasa",
            title = "موفاسا: الأسد الملك (Mufasa: The Lion King)",
            name = "Mufasa",
            category = "أفلام أنمي وعائلة",
            poster = "https://images.unsplash.com/photo-1534188753412-3e26d0d618d6?w=600&auto=format&fit=crop&q=80",
            streamUrl = "http://maxshowplayer.site:2052/movie/13968296781874/20098269331298/108.mp4",
            duration = "1:58:00",
            year = "2024",
            isSeries = false,
            rating = "8.3"
        ),
        MovieItem(
            id = "mov_breaking_bad",
            title = "مسلسل بريكنج باد (Breaking Bad)",
            name = "Breaking Bad",
            category = "مسلسلات عالمية",
            poster = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=600&auto=format&fit=crop&q=80",
            streamUrl = "http://maxshowplayer.site:2052/series/13968296781874/20098269331298/204.mp4",
            duration = "الحلقة 1 - 58:00",
            year: "2020",
            isSeries = true,
            rating = "9.5"
        )
    )

    suspend fun searchMovies(context: Context, query: String): List<MovieItem> = withContext(Dispatchers.IO) {
        val q = query.trim()
        val baseUrl = CloudflareClient.getBaseUrl(context)
        
        try {
            val encQ = URLEncoder.encode(q, "UTF-8")
            val url = "$baseUrl/api/movies/search?q=$encQ"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AlMahallaApp/1.0")
                .build()

            httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val arr = json.optJSONArray("movies")
                        if (arr != null && arr.length() > 0) {
                            val list = mutableListOf<MovieItem>()
                            for (i in 0 until arr.length()) {
                                val item = arr.getJSONObject(i)
                                list.add(
                                    MovieItem(
                                        id = item.optString("id", "mov_$i"),
                                        title = item.optString("title", "فلم"),
                                        name = item.optString("name", item.optString("title", "فلم")),
                                        poster = item.optString("poster", ""),
                                        streamUrl = item.optString("streamUrl", ""),
                                        category = item.optString("category", "أفلام"),
                                        duration = item.optString("duration", "2:00:00"),
                                        year = item.optString("year", "2024"),
                                        isSeries = item.optBoolean("isSeries", false),
                                        rating = item.optString("rating", "8.8")
                                    )
                                )
                            }
                            return@withContext list
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Search via API failed, using fallback: ${e.message}")
        }

        // Local matching from catalog
        if (q.isEmpty() || q == "الكل") {
            return@withContext FALLBACK_CATALOG
        }

        val filtered = FALLBACK_CATALOG.filter {
            it.title.contains(q, ignoreCase = true) ||
            it.name.contains(q, ignoreCase = true) ||
            it.category.contains(q, ignoreCase = true) ||
            it.year.contains(q)
        }

        if (filtered.isNotEmpty()) {
            return@withContext filtered
        }

        // Return catalog if no exact match found so user always has great options
        return@withContext FALLBACK_CATALOG
    }
}
