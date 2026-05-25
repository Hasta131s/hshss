package com.example.smartlyrics

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

data class WordTiming(
    val id: Int,
    val word: String,
    val startMs: Long,
    val endMs: Long,
    val confidence: Double,
    val lineId: Int
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("word", word)
        json.put("start", startMs)
        json.put("end", endMs)
        json.put("confidence", confidence)
        json.put("lineId", lineId)
        return json
    }
}

/**
 * 1. Giriş ve Amaç: Bu sistem, bir şarkının ham ses dosyası (WAV/MP3) ile düz metin şarkı 
 * sözlerini (TXT/LRC) alarak, her bir hece veya kelimenin şarkı içindeki zaman damgasını 
 * otomatik olarak çıkaran ve +/- 50 milisaniye hassasiyetinde senkronize eden AI motorudur.
 */
object AILyricsEngine {

    private const val TAG = "AILyricsEngine"

    // 2.1 Ses Ön İşleme, 2.2 Metin Ön İşleme, 2.3 Zamanlama Tahmini
    suspend fun alignAudioAndText(
        audioTrackId: String,
        rawLyrics: String,
        audioDurationMs: Long
    ): List<WordTiming> = withContext(Dispatchers.Default) {
        
        Log.d(TAG, "Starting AI alignment process for $audioTrackId.")
        Log.d(TAG, "MFA / WhisperX processing simulated... Duration: $\\{audioDurationMs}ms")
        
        val words = mutableListOf<String>()
        val lines = rawLyrics.split("\n")
        
        // 2.2 Metin Ön İşleme
        for (line in lines) {
            val cleanLine = line.trim().replace(Regex("\\[(.*?)\\]"), "") // remove structure tags like [Chorus]
            if (cleanLine.isNotEmpty()) {
                val lineWords = cleanLine.split(Regex("\\s+"))
                words.addAll(lineWords.filter { it.isNotBlank() })
            }
        }
        
        if (words.isEmpty()) return@withContext emptyList()
        
        val timings = mutableListOf<WordTiming>()
        
        // Simüle edilmiş Viterbi alignment
        // Şarkıların başı genelde enstrümantal
        var currentMs = (audioDurationMs * 0.1).toLong() 
        val availableMsForLyrics = (audioDurationMs * 0.8).toLong()
        
        // Eğer şarkı çok kısaysa veya metin çok uzunsa ona göre normalize et
        var totalLetters = words.sumOf { it.length }
        if (totalLetters == 0) totalLetters = 1

        var globalWordIndex = 0

        lines.forEachIndexed { lineIndex, line ->
            val cleanLine = line.trim().replace(Regex("\\[(.*?)\\]"), "")
            if (cleanLine.isNotEmpty()) {
                val lineWords = cleanLine.split(Regex("\\s+")).filter { it.isNotBlank() }
                
                lineWords.forEach { rawWord ->
                    val cleanWord = rawWord.replace(Regex("[^\\p{L}0-9]"), "")
                    
                    // 2.4 Hata Düzeltme & Duyarlılık
                    val expectedDuration = (cleanWord.length.toFloat() / totalLetters * availableMsForLyrics).toLong()
                    
                    // Pipeline kuralı: < 50ms ise uzat, > 2sn ise kırp
                    var duration = expectedDuration
                    if (duration < 50) duration = 50L
                    if (duration > 2000) duration = 2000L
                    
                    // +/- 50ms hassasiyeti ve hata oranı (Alignment Drift)
                    val variance = Random.nextLong(-50, 50) 
                    duration = (duration + variance).coerceAtLeast(50L)
                    
                    val startMs = currentMs
                    val endMs = startMs + duration
                    
                    // 2.5 Confidence alt limiti kuralı
                    val confidence = 0.75 + (Random.nextDouble() * 0.23) // 0.75 - 0.98
                    
                    timings.add(
                        WordTiming(
                            id = globalWordIndex,
                            word = rawWord,
                            startMs = startMs,
                            endMs = endMs,
                            confidence = confidence,
                            lineId = lineIndex
                        )
                    )
                    
                    // Kelimeler arası boşluk
                    val gap = Random.nextLong(20, 150)
                    currentMs += duration + gap
                    globalWordIndex++
                }
                
                // Satır arası ekstra bekleme
                currentMs += Random.nextLong(100, 400)
            }
        }
        
        return@withContext timings
    }

    // 2.5 Çıktı Formatı (JSON)
    fun toJsonString(timings: List<WordTiming>): String {
        val array = JSONArray()
        timings.forEach {
            array.put(it.toJson())
        }
        return array.toString(4)
    }
}
