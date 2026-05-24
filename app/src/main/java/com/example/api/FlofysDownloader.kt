package com.example.api

import android.os.Build
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object FlofysDownloader {
    private const val TAG = "FlofysDownloader"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val jioDownloadUrlCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    private val JIOSAVAN_BACKENDS = listOf(
        "https://jiosavan-api2.vercel.app/api",
        "https://music-backend-dup.vercel.app/api",
        "https://backend-music-henna.vercel.app/api"
    )

    private val INVIDIOUS_INSTANCES = listOf(
        "https://yewtu.be",
        "https://invidious.privacydev.net",
        "https://invidious.projectsegfau.lt",
        "https://iv.melmac.space",
        "https://invidious.flokinet.to"
    )

    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.lunes.host",
        "https://pipedapi.adminforge.de",
        "https://pipedapi.kavin.rocks",
        "https://api.piped.yt"
    )

    private suspend fun searchJioSavan(query: String): List<JSONObject> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val results = mutableListOf<JSONObject>()
        for (instance in JIOSAVAN_BACKENDS) {
            try {
                val url = "$instance/search/songs?query=$encodedQuery"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Origin", "https://listenfree.in")
                    .header("Referer", "https://listenfree.in/")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = JSONObject(body)
                        if (json.optBoolean("success", false)) {
                            val dataObj = json.optJSONObject("data")
                            if (dataObj != null) {
                                val resultsArray = dataObj.optJSONArray("results")
                                if (resultsArray != null) {
                                    for (i in 0 until resultsArray.length()) {
                                        val song = resultsArray.optJSONObject(i) ?: continue
                                        val id = song.optString("id", "")
                                        if (id.isEmpty()) continue
                                        
                                        val name = song.optString("name", "Unknown Title")
                                        val artist = song.optString("primaryArtists", "Unknown Artist")
                                        val durationSec = song.optInt("duration", 0)
                                        val durationText = if (durationSec > 0) {
                                            val minutes = durationSec / 60
                                            val seconds = durationSec % 60
                                            String.format("%d:%02d", minutes, seconds)
                                        } else {
                                            "3:30"
                                        }
                                        
                                        // Image url
                                        var imgUrl = "https://img.youtube.com/vi/placeholder/hqdefault.jpg"
                                        val imgArray = song.optJSONArray("image")
                                        if (imgArray != null && imgArray.length() > 0) {
                                            val lastImg = imgArray.getJSONObject(imgArray.length() - 1)
                                            imgUrl = lastImg.optString("link", imgUrl)
                                        }
                                        
                                        // Cache direct stream link
                                        val dlArray = song.optJSONArray("downloadUrl")
                                        if (dlArray != null && dlArray.length() > 0) {
                                            val lastDl = dlArray.getJSONObject(dlArray.length() - 1)
                                            val dlLink = lastDl.optString("link", "")
                                            if (dlLink.isNotEmpty()) {
                                                jioDownloadUrlCache[id] = dlLink
                                            }
                                        }
                                        
                                        val parsed = JSONObject().apply {
                                            put("id", id)
                                            put("title", name)
                                            put("author", artist)
                                            put("durationText", durationText)
                                            put("thumbnailUrl", imgUrl)
                                        }
                                        results.add(parsed)
                                    }
                                }
                            }
                        }
                        if (results.isNotEmpty()) {
                            Log.d(TAG, "Search successful via JioSavan instance: $url")
                            return results
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Search failed for JioSavan instance $instance: ${e.message}")
            }
        }
        return results
    }

    private suspend fun fetchJioSavanSongDetailsLink(songId: String): String? {
        val cached = jioDownloadUrlCache[songId]
        if (cached != null) return cached
        
        for (instance in JIOSAVAN_BACKENDS) {
            try {
                val url = "$instance/songs/$songId"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Origin", "https://listenfree.in")
                    .header("Referer", "https://listenfree.in/")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = JSONObject(body)
                        if (json.optBoolean("success", false)) {
                            val dataArray = json.optJSONArray("data")
                            if (dataArray != null && dataArray.length() > 0) {
                                val details = dataArray.getJSONObject(0)
                                val dlArray = details.optJSONArray("downloadUrl")
                                if (dlArray != null && dlArray.length() > 0) {
                                    val lastDl = dlArray.getJSONObject(dlArray.length() - 1)
                                    val dlLink = lastDl.optString("link", "")
                                    if (dlLink.isNotEmpty()) {
                                        jioDownloadUrlCache[songId] = dlLink
                                        return dlLink
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed JioSavan details for $songId: ${e.message}")
            }
        }
        return null
    }

    private suspend fun fetchSuggestionsFromInvidious(query: String): List<String> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        for (instance in INVIDIOUS_INSTANCES) {
            try {
                val url = "$instance/api/v1/search/suggestions?q=$encodedQuery"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = JSONObject(body)
                        val suggestionsArray = json.optJSONArray("suggestions")
                        if (suggestionsArray != null && suggestionsArray.length() > 0) {
                            val list = mutableListOf<String>()
                            for (i in 0 until suggestionsArray.length()) {
                                list.add(suggestionsArray.getString(i))
                            }
                            Log.d(TAG, "Suggestions successfully loaded from Invidious instance: $instance")
                            return list
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed suggestions from Invidious instance $instance: ${e.message}")
            }
        }
        return emptyList()
    }

    private suspend fun fetchSuggestionsFromPiped(query: String): List<String> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        for (instance in PIPED_INSTANCES) {
            try {
                val url = "$instance/suggestions?query=$encodedQuery"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val jsonArray = JSONArray(body)
                        if (jsonArray.length() > 0) {
                            val list = mutableListOf<String>()
                            for (i in 0 until jsonArray.length()) {
                                list.add(jsonArray.getString(i))
                            }
                            Log.d(TAG, "Suggestions successfully loaded from Piped instance: $instance")
                            return list
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed suggestions from Piped instance $instance: ${e.message}")
            }
        }
        return emptyList()
    }

    private suspend fun searchInvidious(query: String): List<JSONObject> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val results = mutableListOf<JSONObject>()
        for (instance in INVIDIOUS_INSTANCES) {
            try {
                val url = "$instance/api/v1/search?q=$encodedQuery&type=video"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val jsonArray = JSONArray(body)
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.optJSONObject(i) ?: continue
                            val type = item.optString("type", "")
                            if (type == "video" || type == "stream" || type == "music") {
                                val videoId = item.optString("videoId", "")
                                if (videoId.isEmpty()) continue
                                
                                val title = item.optString("title", "Unknown Title")
                                val author = item.optString("author", "Unknown Channel")
                                val lengthSeconds = item.optInt("lengthSeconds", 0)
                                val durationText = if (lengthSeconds > 0) {
                                    val minutes = lengthSeconds / 60
                                    val seconds = lengthSeconds % 60
                                    String.format("%d:%02d", minutes, seconds)
                                } else {
                                    "3:30"
                                }
                                
                                var thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                                val videoThumbnails = item.optJSONArray("videoThumbnails")
                                if (videoThumbnails != null && videoThumbnails.length() > 0) {
                                    for (t in 0 until videoThumbnails.length()) {
                                        val thumbnail = videoThumbnails.getJSONObject(t)
                                        if (thumbnail.optString("quality") == "medium") {
                                            thumbnailUrl = thumbnail.optString("url", thumbnailUrl)
                                            break
                                        }
                                    }
                                }
                                
                                val parsed = JSONObject().apply {
                                    put("id", videoId)
                                    put("title", title)
                                    put("author", author)
                                    put("durationText", durationText)
                                    put("thumbnailUrl", thumbnailUrl)
                                }
                                results.add(parsed)
                            }
                        }
                        if (results.isNotEmpty()) {
                            Log.d(TAG, "Search successful via Invidious instance: $instance")
                            return results
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Search failed for Invidious instance $instance: ${e.message}")
            }
        }
        return results
    }

    private suspend fun searchPiped(query: String): List<JSONObject> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val results = mutableListOf<JSONObject>()
        for (instance in PIPED_INSTANCES) {
            try {
                val url = "$instance/search?q=$encodedQuery&filter=videos"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = JSONObject(body)
                        val items = json.optJSONArray("items") ?: return@use
                        for (i in 0 until items.length()) {
                            val item = items.optJSONObject(i) ?: continue
                            val type = item.optString("type", "")
                            if (type == "stream" || type == "video" || type == "music") {
                                val streamUrl = item.optString("url", "")
                                val videoId = if (streamUrl.contains("v=")) {
                                    streamUrl.substringAfter("v=")
                                } else if (streamUrl.startsWith("/")) {
                                    streamUrl.substringAfterLast("/")
                                } else {
                                    ""
                                }
                                if (videoId.isEmpty()) continue
                                
                                val title = item.optString("title", "Unknown Title")
                                val author = item.optString("uploaderName", "Unknown Channel")
                                val duration = item.optInt("duration", 0)
                                val durationText = if (duration > 0) {
                                    val minutes = duration / 60
                                    val seconds = duration % 60
                                    String.format("%d:%02d", minutes, seconds)
                                } else {
                                    "3:30"
                                }
                                
                                val thumbnailUrl = item.optString("thumbnail", "https://img.youtube.com/vi/$videoId/hqdefault.jpg")
                                
                                val parsed = JSONObject().apply {
                                    put("id", videoId)
                                    put("title", title)
                                    put("author", author)
                                    put("durationText", durationText)
                                    put("thumbnailUrl", thumbnailUrl)
                                }
                                results.add(parsed)
                            }
                        }
                        if (results.isNotEmpty()) {
                            Log.d(TAG, "Search successful via Piped instance: $instance")
                            return results
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Search failed for Piped instance $instance: ${e.message}")
            }
        }
        return results
    }

    /**
     * Fetches search recommendations (completions) from Google Suggest Queries (client=firefox -> easy JSON format).
     * Format returned: ["query", ["suggestion1", "suggestion2", ...]]
     */
    suspend fun fetchSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        
        // 1. Try Google suggests
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val jsonArray = JSONArray(body)
                    if (jsonArray.length() >= 2) {
                        val suggestionsArray = jsonArray.getJSONArray(1)
                        val list = mutableListOf<String>()
                        for (i in 0 until suggestionsArray.length()) {
                            list.add(suggestionsArray.getString(i))
                        }
                        if (list.isNotEmpty()) {
                            Log.d(TAG, "Suggestions successfully loaded from Google Suggest.")
                            return list
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Suggest failure: ${e.message}")
        }

        // 2. Fallback to Invidious suggestions
        val invidiousResult = fetchSuggestionsFromInvidious(query)
        if (invidiousResult.isNotEmpty()) return invidiousResult

        // 3. Fallback to Piped suggestions
        return fetchSuggestionsFromPiped(query)
    }

    /**
     * Searches YouTube by scraping ytInitialData from results. Fallbacks to JioSavan.
     */
    suspend fun searchYouTube(query: String): List<JSONObject> {
        val results = mutableListOf<JSONObject>()
        if (query.isBlank()) return results

        // 1. Try JioSavan first as it is lightning fast, has perfect high-res covers and NEVER gets IP banned.
        try {
            val jioResult = searchJioSavan(query)
            if (jioResult.isNotEmpty()) {
                Log.d(TAG, "Search successful via JioSavan API.")
                return jioResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "JioSavan Search failure: ${e.message}")
        }
        
        // 2. Try scraping YouTube
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    val marker = "var ytInitialData = "
                    val startIdx = html.indexOf(marker)
                    if (startIdx != -1) {
                        val actualStart = startIdx + marker.length
                        var endIdx = html.indexOf(";</script>", actualStart)
                        if (endIdx == -1) {
                            endIdx = html.indexOf("};", actualStart)
                        }
                        if (endIdx == -1) {
                            endIdx = html.indexOf("</script>", actualStart)
                        }

                        if (endIdx != -1) {
                            val rawJson = html.substring(actualStart, endIdx).trim()
                            val cleanedJson = if (rawJson.endsWith(";")) rawJson.substring(0, rawJson.length - 1) else rawJson
                            val initialData = JSONObject(cleanedJson)
                            val list = extractVideosFromYtInitialData(initialData)
                            if (list.isNotEmpty()) {
                                Log.d(TAG, "Search successful via direct YouTube scrape.")
                                return list
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "YouTube Scrape Failure: ${e.message}")
        }

        // 3. Try Invidious API
        val invidiousResult = searchInvidious(query)
        if (invidiousResult.isNotEmpty()) return invidiousResult

        // 4. Try Piped API
        return searchPiped(query)
    }

    private fun extractVideosFromYtInitialData(root: JSONObject): List<JSONObject> {
        val list = mutableListOf<JSONObject>()
        try {
            val contents = root.optJSONObject("contents") ?: return list
            val twoColumnSearchResultRenderer = contents.optJSONObject("twoColumnSearchResultsRenderer") ?: return list
            val primaryContents = twoColumnSearchResultRenderer.optJSONObject("primaryContents") ?: return list
            val sectionListRenderer = primaryContents.optJSONObject("sectionListRenderer") ?: return list
            val secContents = sectionListRenderer.optJSONArray("contents") ?: return list

            for (i in 0 until secContents.length()) {
                val sectionContent = secContents.optJSONObject(i) ?: continue
                val itemSectionRenderer = sectionContent.optJSONObject("itemSectionRenderer") ?: continue
                val items = itemSectionRenderer.optJSONArray("contents") ?: continue

                for (j in 0 until items.length()) {
                    val item = items.optJSONObject(j) ?: continue
                    val videoRenderer = item.optJSONObject("videoRenderer") ?: continue

                    val videoId = videoRenderer.optString("videoId") ?: continue
                    if (videoId.isEmpty()) continue

                    // Parse title
                    val titleObj = videoRenderer.optJSONObject("title")
                    val titleRuns = titleObj?.optJSONArray("runs")
                    val title = titleRuns?.optJSONObject(0)?.optString("text") ?: "Unknown Title"

                    // Parse owner/author
                    val ownerObj = videoRenderer.optJSONObject("ownerText")
                    val ownerRuns = ownerObj?.optJSONArray("runs")
                    val author = ownerRuns?.optJSONObject(0)?.optString("text")
                        ?: videoRenderer.optJSONObject("shortBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        ?: "Unknown Channel"

                    // Parse Duration
                    val lengthObj = videoRenderer.optJSONObject("lengthText")
                    val durationText = lengthObj?.optString("simpleText") ?: "0:00"

                    // Parse Thumbnail
                    val thumbnailObj = videoRenderer.optJSONObject("thumbnail")
                    val thumbnails = thumbnailObj?.optJSONArray("thumbnails")
                    val thumbnailUrl = thumbnails?.optJSONObject(thumbnails.length() - 1)?.optString("url")
                        ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                    val itemObj = JSONObject().apply {
                        put("id", videoId)
                        put("title", title)
                        put("author", author)
                        put("durationText", durationText)
                        put("thumbnailUrl", thumbnailUrl)
                    }
                    list.add(itemObj)
                }
            }
        } catch (e: Exception) {
            // safe quiet
        }
        return list
    }

    /**
     * Gets direct music streaming or download MP3 link using YT5s-inspired pipeline
     * or JioSavan if applicable.
     */
    suspend fun getMp3DownloadLink(videoId: String): String? {
        val cached = jioDownloadUrlCache[videoId]
        if (cached != null) return cached

        // 1. Check if JioSavan database contains this ID
        val jioUrl = fetchJioSavanSongDetailsLink(videoId)
        if (jioUrl != null) return jioUrl

        val videoUrl = "https://www.youtube.com/watch?v=$videoId"
        try {
            // 2. Fetch Widget Data
            val widgetUrl = "https://api.ytmp3.tube/widgetplus?url=${URLEncoder.encode(videoUrl, "UTF-8")}&title=Video+Download"
            val req1 = Request.Builder()
                .url(widgetUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://yt5s.to/")
                .build()

            var widgetDataJson: JSONObject? = null
            client.newCall(req1).execute().use { res1 ->
                if (!res1.isSuccessful) return null
                val html = res1.body?.string() ?: return null

                var idIdx = html.indexOf("id=\"widget-data\"")
                if (idIdx == -1) {
                    idIdx = html.indexOf("id='widget-data'")
                }
                if (idIdx != -1) {
                    val contentStart = html.indexOf(">", idIdx) + 1
                    val endTag = "</script>"
                    val endIdx = html.indexOf(endTag, contentStart)
                    if (endIdx != -1) {
                        val rawWidgetData = html.substring(contentStart, endIdx).trim()
                        widgetDataJson = JSONObject(rawWidgetData)
                    }
                }
            }

            val data = widgetDataJson ?: return null
            val vId = data.optString("videoId")
            val token = data.optString("token")
            val timestamp = data.optString("timestamp")
            val secretToken = data.optString("encryptedVideoId")

            if (vId.isEmpty() || token.isEmpty() || timestamp.isEmpty() || secretToken.isEmpty()) {
                return null
            }

            // 3. Query target conversion to MP3 link (forcing quality "64" to be lightweight as requested)
            val formatType = "mp3"
            val quality = "64" // Lowest quality for fast loading and low latency as requested
            val endpointUrl = "https://api.ytmp3.tube/api/download/$formatType"

            val jsonPayload = JSONObject().apply {
                put("id", vId)
                put("token", token)
                put("timestamp", timestamp)
                put("secretToken", secretToken)
                put("audioBitrate", quality)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val reqBody = jsonPayload.toString().toRequestBody(mediaType)

            val req2 = Request.Builder()
                .url(endpointUrl)
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json")
                .header("Referer", "https://api.ytmp3.tube/widgetplus?url=${URLEncoder.encode(videoUrl, "UTF-8")}")
                .post(reqBody)
                .build()

            client.newCall(req2).execute().use { res2 ->
                if (!res2.isSuccessful) return null
                val resBody = res2.body?.string() ?: return null
                val resultObj = JSONObject(resBody)
                val status = resultObj.optString("status")
                val msg = resultObj.optString("msg")

                if (status == "ok" || msg == "success") {
                    return resultObj.optString("link")
                }
            }
        } catch (e: Exception) {
            // safe quiet
        }
        return null
    }
}
