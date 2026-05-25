package com.example.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object LyricsProvider {
    private const val TAG = "LyricsProvider"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
        
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /**
     * Searches Genius and retrieves the lyrics text.
     */
    suspend fun fetchLyrics(artist: String, title: String, forceGenius: Boolean = false): String = withContext(Dispatchers.IO) {
        val cleanedTitle = title.replace(Regex("(?i)lyrics|lyric|video|audio|official|\\(.*?\\)|\\[.*?\\]"), "").trim()
        val cleanedArtist = artist.replace(Regex("(?i)unknown"), "").trim()
        
        if (!forceGenius) {
            // 1. Try lrclib.net first (provides synced and plain lyrics)
            try {
                val url = okhttp3.HttpUrl.Builder()
                    .scheme("https")
                    .host("lrclib.net")
                    .addPathSegment("api")
                    .addPathSegment("search")
                    .addQueryParameter("track_name", cleanedTitle)
                    .apply {
                        if (cleanedArtist.isNotBlank()) addQueryParameter("artist_name", cleanedArtist)
                    }
                    .build()
                    
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build()
                    
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string()
                        if (bodyStr != null) {
                            val jsonArray = org.json.JSONArray(bodyStr)
                            if (jsonArray.length() > 0) {
                                val firstObj = jsonArray.getJSONObject(0)
                                val synced = if (firstObj.has("syncedLyrics") && !firstObj.isNull("syncedLyrics")) firstObj.getString("syncedLyrics") else null
                                val plain = if (firstObj.has("plainLyrics") && !firstObj.isNull("plainLyrics")) firstObj.getString("plainLyrics") else null
                                
                                if (!synced.isNullOrBlank()) {
                                    return@withContext synced
                                } else if (!plain.isNullOrBlank()) {
                                    return@withContext plain
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "lrclib scrape failed: ${e.message}")
            }
        }
        
        // 2. Fallback to Genius if lrclib fails (or if forceGenius is true)
        try {
            val queryInput = if (cleanedArtist.isBlank()) {
                cleanedTitle
            } else {
                "$cleanedArtist $cleanedTitle"
            }.trim()
            
            val query = URLEncoder.encode(queryInput, "UTF-8")
            val searchUrl = "https://genius.com/api/search/multi?q=$query"
            
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", USER_AGENT)
                .build()
                
            val songUrl = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val json = JSONObject(body)
                val sections = json.optJSONObject("response")?.optJSONArray("sections") ?: return@use null
                
                for (i in 0 until sections.length()) {
                    val section = sections.getJSONObject(i)
                    if (section.optString("type") == "song") {
                        val hits = section.optJSONArray("hits") ?: continue
                        if (hits.length() > 0) {
                            val songResult = hits.getJSONObject(0).optJSONObject("result")
                            if (songResult != null) {
                                val url = songResult.optString("url")
                                if (url.isNotEmpty()) return@use url
                            }
                        }
                    }
                }
                null
            }
            
            if (songUrl == null) {
                // Try searching with just title as a broad search fallback
                val fallbackQuery = URLEncoder.encode(cleanedTitle, "UTF-8")
                val fallbackRequest = Request.Builder()
                    .url("https://genius.com/api/search/multi?q=$fallbackQuery")
                    .header("User-Agent", USER_AGENT)
                    .build()
                val url = client.newCall(fallbackRequest).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val json = JSONObject(response.body?.string() ?: "")
                    val sections = json.optJSONObject("response")?.optJSONArray("sections") ?: return@use null
                    for (i in 0 until sections.length()) {
                        val section = sections.getJSONObject(i)
                        if (section.optString("type") == "song") {
                            val hits = section.optJSONArray("hits") ?: continue
                            if (hits.length() > 0) {
                                val song = hits.getJSONObject(0).optJSONObject("result")
                                if (song != null) return@use song.optString("url")
                            }
                        }
                    }
                    null
                }
                if (url != null) return@withContext scrapeLyricsFromUrl(url)
                return@withContext "Şarkı sözü bulunamadı."
            }
            
            return@withContext scrapeLyricsFromUrl(songUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Lyrics fetch/search failed: ${e.message}")
            return@withContext "Sözler aranırken bir hata oluştu: ${e.localizedMessage}"
        }
    }

    private fun cleanTextHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&quot;", "\"")
    }

    private fun scrapeLyricsFromUrl(url: String): String {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "Şarkı sözleri sayfasından veri alınamadı."
                val html = response.body?.string() ?: return "Veri boş geldi."
                
                // Genius standard modern format has elements starting with <div class="Lyrics__Container...
                val lyricsBuilder = StringBuilder()
                
                val containerPattern = Pattern.compile("<div[^>]*class=\"[^\"]*Lyrics__Container[^\"]*\"[^>]*>(.*?)</div>", Pattern.DOTALL)
                val matcher = containerPattern.matcher(html)
                var found = false
                
                while (matcher.find()) {
                    found = true
                    var content = matcher.group(1) ?: ""
                    content = cleanHtml(content)
                    if (content.isNotBlank()) {
                        lyricsBuilder.append(content).append("\n\n")
                    }
                }
                
                if (!found) {
                    // Try old backup parser: <div class="lyrics">
                    val oldPattern = Pattern.compile("<div[^>]*class=\"lyrics\"[^>]*>(.*?)</div>", Pattern.DOTALL)
                    val oldMatcher = oldPattern.matcher(html)
                    if (oldMatcher.find()) {
                        val content = oldMatcher.group(1) ?: ""
                        lyricsBuilder.append(cleanHtml(content))
                    }
                }
                
                val result = lyricsBuilder.toString().trim()
                if (result.isEmpty()) {
                    return "Enstrümantal veya Sözler henüz eklenmemiş."
                }
                return result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Scraping failed: ${e.message}", e)
            return "Sözler sunucudan çekilemedi."
        }
    }

    private fun cleanHtml(htmlContent: String): String {
        var text = htmlContent
        
        // Replace <br/> or <br> or <br class="..."> as new lines
        text = text.replace(Regex("(?i)<br\\s*/?>"), "\n")
        
        // Remove nested divs and spans
        text = text.replace(Regex("<[^>]*>"), "")
        
        // Resolve common HTML entities
        text = text.replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&rsquo;", "'")
            .replace("&lsquo;", "'")
            .replace("&ldquo;", "\"")
            .replace("&rdquo;", "\"")
            .replace("&#x27;", "'")
            .replace("&#x2F;", "/")
            
        return text.trim()
    }
}
