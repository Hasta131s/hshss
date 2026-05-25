package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Track
import com.example.service.PlaybackManager
import com.example.service.PlaybackManager.LyricsState
import com.example.smartlyrics.WordTiming
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.SpotGreen
import com.example.ui.theme.TextGrey
import com.example.ui.theme.White

// Extension method for FlowRow since we need to display words seamlessly
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AILyricsWidget(currentTrack: Track) {
    val lyrics by PlaybackManager.currentTrackLyrics.collectAsStateWithLifecycle()
    val state by PlaybackManager.lyricsState.collectAsStateWithLifecycle()
    val alignedLyrics by PlaybackManager.alignedLyrics.collectAsStateWithLifecycle()
    
    var isExpanded by remember { mutableStateOf(false) }
    val posMs by PlaybackManager.currentPositionMs.collectAsStateWithLifecycle()
    val durMs by PlaybackManager.durationMs.collectAsStateWithLifecycle()

    // 3. Gerçek Zamanlı Oynatma ve Tepki
    // Her 100ms'de bir güncellenmesi posMs değişkeni ile sağlanır
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { isExpanded = true },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E102E)),
        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "AI Senkronize Sözler",
                        tint = SpotGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Söz Görüntüleyici",
                        color = White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Genişlet",
                        color = SpotGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            when (state) {
                LyricsState.LOADING -> {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SpotGreen, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                }
                LyricsState.ERROR -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Sözler henüz yüklenemedi.", color = TextGrey, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = { PlaybackManager.loadLyricsForCurrentTrack() }) {
                            Text("Tekrar Dene", color = SpotGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                LyricsState.SUCCESS -> {
                    if (alignedLyrics.isNotEmpty()) {
                        // Compact View: Highlight exactly ONE active line
                        val activeLineIndex = alignedLyrics.lastOrNull { posMs >= it.startMs }?.lineId ?: 0
                        val wordsInLine = alignedLyrics.filter { it.lineId == activeLineIndex }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                wordsInLine.forEach { word ->
                                    val isActive = posMs >= word.startMs && posMs < word.endMs
                                    Text(
                                        text = word.word + " ",
                                        color = if (isActive) SpotGreen else White.copy(alpha = 0.4f),
                                        fontSize = if (isActive) 15.sp else 13.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    } else {
                        // Fallback scrolling raw text
                        val rawLyrics = lyrics ?: ""
                        val scrollState = rememberScrollState()
                        LaunchedEffect(posMs, durMs, scrollState.maxValue) {
                            if (durMs > 0 && scrollState.maxValue > 0) {
                                val ratio = posMs.toFloat() / durMs
                                scrollState.scrollTo((ratio * scrollState.maxValue).toInt())
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .verticalScroll(scrollState, enabled = false)
                        ) {
                            Text(
                                text = rawLyrics,
                                color = White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                LyricsState.IDLE -> {
                    TextButton(
                        onClick = { PlaybackManager.loadLyricsForCurrentTrack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Şarkı Sözlerini Yükle", color = SpotGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    
    if (isExpanded) {
        Dialog(
            onDismissRequest = { isExpanded = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F0619))
                    .systemBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTrack.title,
                                color = White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentTrack.author,
                                color = SpotGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.08f), CircleShape).size(40.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Sözleri Kapat", tint = White, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    if (alignedLyrics.isNotEmpty()) {
                        Text("Yapay Zeka Motoru: Aktif (±50ms Senkron)", color = SpotGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Standart Metin Görüntüleyici", color = TextGrey.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (state) {
                        LyricsState.LOADING -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = SpotGreen)
                            }
                        }
                        LyricsState.ERROR, LyricsState.IDLE -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Şarkı sözleri yüklenemedi.", color = TextGrey)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { PlaybackManager.loadLyricsForCurrentTrack() },
                                        colors = ButtonDefaults.buttonColors(containerColor = SpotGreen)
                                    ) {
                                        Text("Tekrar dene", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        LyricsState.SUCCESS -> {
                            if (alignedLyrics.isNotEmpty()) {
                                // Group into lines based on lineId
                                val groupedLines = alignedLyrics.groupBy { it.lineId }
                                val listState = rememberLazyListState()
                                val activeLineId = alignedLyrics.lastOrNull { posMs >= it.startMs }?.lineId ?: 0
                                
                                LaunchedEffect(activeLineId) {
                                    if (groupedLines.isNotEmpty()) {
                                        listState.animateScrollToItem(index = (activeLineId - 2).coerceAtLeast(0))
                                    }
                                }
                                
                                Box(modifier = Modifier.weight(1f)) {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(22.dp),
                                        contentPadding = PaddingValues(vertical = 40.dp)
                                    ) {
                                        itemsIndexed(groupedLines.keys.toList().sorted()) { index, lineId ->
                                            val wordsInLine = groupedLines[lineId] ?: emptyList()
                                            val isLineActive = lineId == activeLineId
                                            val isLinePassed = lineId < activeLineId
                                            
                                            FlowRow(modifier = Modifier.fillMaxWidth()) {
                                                wordsInLine.forEach { word ->
                                                    // Karaoka Highlight logic: 
                                                    // if current posMs is inside word bounds -> fully bright and active
                                                    // if posMs is past the word (or line passed) -> bright enough but not currently "singing" 
                                                    // if posMs is before the word -> dimmed
                                                    val isWordActive = posMs >= word.startMs && posMs < word.endMs
                                                    val isWordPassed = posMs >= word.endMs
                                                    
                                                    val wordColor = if (isWordActive) {
                                                        SpotGreen // Currently singing
                                                    } else if (isWordPassed || isLinePassed) {
                                                        Color.White // Already sung
                                                    } else {
                                                        Color.White.copy(alpha = 0.35f) // To be sung
                                                    }
                                                    
                                                    Text(
                                                        text = word.word + " ",
                                                        color = wordColor,
                                                        fontSize = if (isWordActive) 24.sp else 22.sp,
                                                        fontWeight = if (isWordActive) FontWeight.ExtraBold else FontWeight.Bold,
                                                        modifier = Modifier.clickable {
                                                            PlaybackManager.seekTo(word.startMs.toInt())
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(bottom = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Verified, contentDescription = null, tint = SpotGreen, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("S2T Yapay Zeka Kelime Senkronizasyonu", color = White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                val rawLyrics = lyrics ?: ""
                                val scrollState = rememberScrollState()
                                LaunchedEffect(posMs, durMs, scrollState.maxValue) {
                                    if (durMs > 0 && scrollState.maxValue > 0) {
                                        val ratio = posMs.toFloat() / durMs
                                        scrollState.scrollTo((ratio * scrollState.maxValue).toInt())
                                    }
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(scrollState)
                                            .padding(vertical = 40.dp)
                                    ) {
                                        Text(
                                            text = rawLyrics,
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Normal,
                                            modifier = Modifier.fillMaxWidth(),
                                            lineHeight = 28.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
