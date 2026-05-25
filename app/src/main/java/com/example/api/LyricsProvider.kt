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
    suspend fun fetchLyrics(artist: String, title: String): String = withContext(Dispatchers.IO) {
        val cleanedTitle = title.replace(Regex("(?i)lyrics|lyric|video|audio|official|\\(.*?\\)|\\[.*?\\]"), "").trim()
        val cleanedArtist = artist.replace(Regex("(?i)unknown"), "").trim()
        
        // 1. Try lyricsposter.com first
        if (cleanedArtist.isNotEmpty() && cleanedTitle.isNotEmpty()) {
            val fromPoster = scrapeLyricsFromPoster(cleanedArtist, cleanedTitle)
            if (fromPoster != null) {
                return@withContext fromPoster
            }
        }
        
        // 2. Fallback to Genius if poster fails
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

    private fun scrapeLyricsFromPoster(artist: String, song: String): String? {
        try {
            val encodedArtist = URLEncoder.encode(artist.lowercase(), "UTF-8").replace("+", "%20")
            val encodedSong = URLEncoder.encode(song.lowercase(), "UTF-8").replace("+", "%20")
            val url = "https://www.lyricsposter.com/lyrics/$encodedArtist/$encodedSong"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val html = response.body?.string() ?: return null
                
                // Using method 2 from python: text extraction
                // Since Kotlin doesn't have BeautifulSoup built-in, we use string parsing mimicking it, or regex.
                
                // Let's try to extract with regex for the container first
                // Look for <h3>Lyrics</h3> and following <div>
                var pattern = Pattern.compile("(?i)<h3>\\s*Lyrics\\s*</h3>\\s*<div[^>]*>(.*?)</div>", Pattern.DOTALL)
                var matcher = pattern.matcher(html)
                if (matcher.find()) {
                    var content = matcher.group(1) ?: ""
                    content = cleanHtml(content)
                    if (content.isNotBlank()) return content
                }
                
                // Regex Method 1 fallback
                pattern = Pattern.compile("(?i)Lyrics\\s*</h3>\\s*<p[^>]*>(.*?)</p>", Pattern.DOTALL)
                matcher = pattern.matcher(html)
                if (matcher.find()) {
                    var content = matcher.group(1) ?: ""
                    content = cleanHtml(content)
                    if (content.isNotBlank()) return content
                }
                
                // Extract plain text and parse like Method 2
                val textOnly = html.replace(Regex("<br\\s*/?>"), "\n").replace(Regex("<[^>]*>"), "")
                if (textOnly.contains("Lyrics")) {
                    val parts = textOnly.split("Lyrics")
                    if (parts.size > 1) {
                        val partAfterLyrics = parts[1]
                        val lyricsPart = partAfterLyrics.split("Songs People Send")[0]
                        val lines = lyricsPart.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                        
                        if (lines.isNotEmpty()) {
                            var startIndex = 0
                            if (lines[0].startsWith("+") || lines[0].all { it.isDigit() }) {
                                startIndex = 1
                            }
                            if (startIndex < lines.size) {
                                val result = lines.subList(startIndex, lines.size).joinToString("\n").trim()
                                // Sanity check to avoid returning huge chunks of garbage if parsing failed
                                if (result.length > 20 && result.length < 5000) {
                                    // Make sure it looks like lyrics and not JS or CSS
                                    if (!result.contains("function()") && !result.contains("{")) {
                                        return cleanTextHtml(result)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Poster scrape failed: ${e.message}")
        }
        return null
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
