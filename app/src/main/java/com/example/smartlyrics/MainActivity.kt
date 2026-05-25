package com.example.smartlyrics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch

@Serializable
data class SyncWord(
    val word: String,
    val start: Long,
    val end: Long,
    val confidence: Double
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                SmartLyricsScreen()
            }
        }
    }
}

@Composable
fun SmartLyricsScreen() {
    var isProcessing by remember { mutableStateOf(false) }
    var processingProgress by remember { mutableStateOf(0f) }
    var isPlaying by remember { mutableStateOf(false) }
    
    // Simulate real-time player position
    var currentTimeMs by remember { mutableStateOf(0L) }
    
    // Load Sample JSON based on specification
    val sampleJson = """
    [
      { "word": "kalbim", "start": 1230, "end": 1450, "confidence": 0.97 },
      { "word": "yanıyor", "start": 1520, "end": 1780, "confidence": 0.94 },
      { "word": "bu", "start": 2000, "end": 2100, "confidence": 0.93 },
      { "word": "gece", "start": 2150, "end": 2500, "confidence": 0.99 },
      { "word": "yok", "start": 3000, "end": 3500, "confidence": 0.88 },
      { "word": "çaresi", "start": 3600, "end": 4200, "confidence": 0.91 }
    ]
    """.trimIndent()
    
    var syncWords by remember { mutableStateOf<List<SyncWord>>(emptyList()) }
    
    val scope = rememberCoroutineScope()
    
    fun processAndPlay() {
        if (isPlaying) {
            isPlaying = false
            return
        }
        
        if (syncWords.isEmpty()) {
            scope.launch {
                isProcessing = true
                // Simulate: Alignment işlemi: 3 dakikalık bir şarkı için ~6 saniye
                for(i in 1..60) {
                    delay(100)
                    processingProgress = i / 60f
                }
                syncWords = Json.decodeFromString<List<SyncWord>>(sampleJson)
                isProcessing = false
                isPlaying = true
                currentTimeMs = 0L
            }
        } else {
            isPlaying = true
        }
    }
    
    // Real-time playback loop (Checks every 100ms as requested)
    LaunchedEffect(isPlaying) {
        while(isPlaying) {
            delay(100)
            currentTimeMs += 100
            
            // Loop for demo purposes
            if (currentTimeMs > 6000L) {
                currentTimeMs = 0L
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Yapay Zeka Söz Senkronizasyonu",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(30.dp))
            
            if (isProcessing) {
                CircularProgressIndicator(
                    progress = processingProgress,
                    color = Color(0xFF00FF7F),
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Spleeter Vokal Ayrıştırılıyor ve MFA Zamanlama Çıkarılıyor...",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            } else {
                Button(
                    onClick = { processAndPlay() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF7F))
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPlaying) "Durdur" else "İşle & Başlat", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Text("Zaman: ${currentTimeMs}ms", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(20.dp))
            
            if (syncWords.isNotEmpty()) {
                // Drift Simulation indicator
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Senkronize Edildi (MFA Viterbi Aligner)", color = Color(0xFF00FF7F), fontSize = 12.sp)
                    Text("Drift: ±12ms", color = Color.Gray, fontSize = 12.sp)
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        // Karaokee style sentence grouping (simulated)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    
                    itemsIndexed(syncWords) { idx, word ->
                        val isActive = currentTimeMs >= word.start && currentTimeMs <= word.end
                        val isPassed = currentTimeMs > word.end
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    when {
                                        isActive -> Color(0xFF00FF7F).copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    }, 
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = word.word,
                                fontSize = if (isActive) 26.sp else 20.sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                color = when {
                                    isActive -> Color(0xFF00FF7F)
                                    isPassed -> Color.White.copy(alpha = 0.5f)
                                    else -> Color.White
                                }
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${word.start} - ${word.end} ms",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Confidence: %${(word.confidence * 100).toInt()}",
                                    fontSize = 10.sp,
                                    color = if (word.confidence >= 0.90) Color(0xFF00FF7F) else Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
