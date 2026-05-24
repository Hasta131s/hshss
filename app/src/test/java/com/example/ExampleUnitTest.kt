package com.example

import com.example.api.FlofysDownloader
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testWidgetPlus() = runBlocking {
    val videoId = "60ItHLz5WEA"
    val videoUrl = "https://www.youtube.com/watch?v=$videoId"
    val encodedUrl = java.net.URLEncoder.encode(videoUrl, "UTF-8")
    
    val widgetUrl = "https://api.ytmp3.tube/widgetplus?url=$encodedUrl&title=Video+Download"
    val client = okhttp3.OkHttpClient()
    val req1 = okhttp3.Request.Builder()
        .url(widgetUrl)
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .header("Referer", "https://yt5s.to/")
        .build()

    println("--- START WIDGETPLUS TEST ---")
    try {
      client.newCall(req1).execute().use { res1 ->
        val html = res1.body?.string() ?: ""
        println("Response code: ${res1.code}")
        
        // Use flexible extraction
        val idMarker = "id=\"widget-data\""
        val idIdx = html.indexOf(idMarker)
        if (idIdx == -1) {
          println("FAIL: idMarker not found")
          return@use
        }
        
        val contentStart = html.indexOf(">", idIdx) + 1
        val jsonEnd = html.indexOf("</script>", contentStart)
        if (jsonEnd == -1) {
          println("FAIL: jsonEnd not found")
          return@use
        }
        
        val rawWidgetData = html.substring(contentStart, jsonEnd).trim()
        println("Extracted json data: $rawWidgetData")
        
        val data = org.json.JSONObject(rawWidgetData)
        val vId = data.optString("videoId")
        val token = data.optString("token")
        val timestamp = data.optString("timestamp")
        val secretToken = data.optString("encryptedVideoId")
        
        println("Parsed widget-data: videoId=$vId, token=$token, timestamp=$timestamp, secretToken=$secretToken")
        
        // 2. Query target conversion to MP3 link (forcing quality "64" to be lightweight)
        val endpointUrl = "https://api.ytmp3.tube/api/download/mp3"
        val jsonPayload = org.json.JSONObject().apply {
          put("id", vId)
          put("token", token)
          put("timestamp", timestamp)
          put("secretToken", secretToken)
          put("audioBitrate", "64")
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val reqBody = jsonPayload.toString().toRequestBody(mediaType)

        val req2 = okhttp3.Request.Builder()
            .url(endpointUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Content-Type", "application/json")
            .header("Referer", "https://api.ytmp3.tube/widgetplus?url=${encodedUrl}")
            .post(reqBody)
            .build()

        client.newCall(req2).execute().use { res2 ->
          println("Download response code: ${res2.code}")
          val body = res2.body?.string() ?: ""
          println("Download response body: $body")
          
          val resultObj = org.json.JSONObject(body)
          val link = resultObj.optString("link")
          println("FINAL DIRECT LINK: $link")
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  @Test
  fun testSearchYouTubeTracing() = runBlocking {
    val query = "never gonna give you up"
    println("--- START SEARCH YOUTUBE TRACING TEST ---")
    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
    val url = "https://www.youtube.com/results?search_query=$encodedQuery"
    val client = okhttp3.OkHttpClient()
    val request = okhttp3.Request.Builder()
        .url(url)
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .build()

    try {
      client.newCall(request).execute().use { response ->
        val html = response.body?.string() ?: ""
        println("Response code: ${response.code}")
        
        val marker = "var ytInitialData = "
        val startIdx = html.indexOf(marker)
        if (startIdx == -1) {
          println("FAIL: marker not found!")
          return@use
        }
        println("Marker found at index: $startIdx")
        
        val actualStart = startIdx + marker.length
        var endIdx = html.indexOf(";</script>", actualStart)
        println("Search for ';</script>' matched at: $endIdx")
        if (endIdx == -1) {
          endIdx = html.indexOf("};", actualStart)
          println("Search for '};' matched at: $endIdx")
        }
        if (endIdx == -1) {
          endIdx = html.indexOf("</script>", actualStart)
          println("Search for '</script>' matched at: $endIdx")
        }
        
        if (endIdx == -1) {
          println("FAIL: endIdx not found!")
          return@use
        }
        
        println("Extracting JSON from $actualStart to $endIdx")
        val rawJson = html.substring(actualStart, endIdx).trim()
        val cleanedJson = if (rawJson.endsWith(";")) rawJson.substring(0, rawJson.length - 1) else rawJson
        
        println("Cleaned JSON length: ${cleanedJson.length}")
        println("Cleaned JSON first 200 chars: ${cleanedJson.take(200)}")
        println("Cleaned JSON last 100 chars: ${cleanedJson.takeLast(100)}")
        
        val initialData = org.json.JSONObject(cleanedJson)
        println("Successfully parsed JSONObject!")
        
        // Let's trace extractVideosFromYtInitialData:
        val contents = initialData.optJSONObject("contents")
        if (contents == null) {
          println("extract FAIL: 'contents' is null!")
          return@use
        }
        val twoColumnSearchResultsRenderer = contents.optJSONObject("twoColumnSearchResultsRenderer")
        if (twoColumnSearchResultsRenderer == null) {
          println("extract FAIL: 'twoColumnSearchResultsRenderer' is null!")
          // Let's print out the root keys of initialData and contents
          println("initialData keys: ${initialData.keys().asSequence().toList()}")
          println("contents keys: ${contents.keys().asSequence().toList()}")
          return@use
        }
        val primaryContents = twoColumnSearchResultsRenderer.optJSONObject("primaryContents")
        if (primaryContents == null) {
          println("extract FAIL: 'primaryContents' is null!")
          return@use
        }
        val sectionListRenderer = primaryContents.optJSONObject("sectionListRenderer")
        if (sectionListRenderer == null) {
          println("extract FAIL: 'sectionListRenderer' is null!")
          return@use
        }
        val secContents = sectionListRenderer.optJSONArray("contents")
        if (secContents == null) {
          println("extract FAIL: 'secContents' is null!")
          return@use
        }
        
        println("secContents length: ${secContents.length()}")
        var parsedCount = 0
        for (i in 0 until secContents.length()) {
          val sectionContent = secContents.optJSONObject(i) ?: continue
          val itemSectionRenderer = sectionContent.optJSONObject("itemSectionRenderer") ?: continue
          val items = itemSectionRenderer.optJSONArray("contents") ?: continue
          println("Section $i items count: ${items.length()}")
          
          for (j in 0 until items.length()) {
            val item = items.optJSONObject(j) ?: continue
            val videoRenderer = item.optJSONObject("videoRenderer")
            if (videoRenderer != null) {
              val videoId = videoRenderer.optString("videoId")
              val titleObj = videoRenderer.optJSONObject("title")
              val titleRuns = titleObj?.optJSONArray("runs")
              val title = titleRuns?.optJSONObject(0)?.optString("text") ?: "Unknown Title"
              println("Found videoRenderer! id=$videoId title='$title'")
              parsedCount++
            } else {
              // What are the other renderers?
              println("Item $j renderer keys: ${item.keys().asSequence().toList()}")
            }
          }
        }
        println("Parsed count: $parsedCount")
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
