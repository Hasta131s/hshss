package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Playlist
import com.example.data.Track
import com.example.service.LoopMode
import com.example.service.PlaybackManager
import com.example.ui.AppTab
import com.example.ui.FlofysViewModel
import com.example.ui.LoginState
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.SpotGreen
import com.example.ui.theme.TextGrey
import com.example.ui.theme.White
import com.example.ui.theme.Zinc700

class MainActivity : ComponentActivity() {

    private val viewModel: FlofysViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check & Request Speech and notification permissions
        checkPermissions()

        setContent {
            MyApplicationTheme {
                val loginState by viewModel.loginState.collectAsStateWithLifecycle()
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (loginState) {
                        LoginState.SPLASH -> SplashScreen()
                        LoginState.LOGGING_IN -> LoginProgressScreen()
                        LoginState.SUCCESS -> MainDashboard(viewModel = viewModel)
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissionsNeeded = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 101)
        }
    }
}

// ==========================================
// SPLASH & LOGIN OVERLAYS (AUTOMATED)
// ==========================================

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant Vector styled Spotify-like circle wave logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(SpotGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(52.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Flofys",
                color = White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Müziğin Akış Noktası",
                color = TextGrey,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun LoginProgressScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = SpotGreen,
                strokeWidth = 4.dp,
                modifier = Modifier.size(50.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Otomatik Giriş Yapılıyor...",
                color = White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Profil yükleniyor ve senkronize ediliyor",
                color = TextGrey,
                fontSize = 12.sp
            )
        }
    }
}

// ==========================================
// MAIN DASHBOARD (TABS + MINI PLAYER)
// ==========================================

@Composable
fun MainDashboard(viewModel: FlofysViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val playingTrack by PlaybackManager.currentTrack.collectAsStateWithLifecycle()
    
    var isPlayerExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            ) {
                // Persistent mini player sticking above navigation bar
                playingTrack?.let { track ->
                    MiniPlayerRow(
                        track = track,
                        onClicked = { isPlayerExpanded = true }
                    )
                }

                // Beautiful Bottom Navigation standard
                NavigationBar(
                    containerColor = DarkSurface,
                    modifier = Modifier.navigationBarsPadding(),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == AppTab.HOME,
                        onClick = { viewModel.setTab(AppTab.HOME) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Ana Sayfa") },
                        label = { Text("Ana Sayfa") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SpotGreen,
                            selectedTextColor = SpotGreen,
                            unselectedIconColor = TextGrey,
                            unselectedTextColor = TextGrey,
                            indicatorColor = DarkCardSurface
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.SEARCH,
                        onClick = { viewModel.setTab(AppTab.SEARCH) },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Ara") },
                        label = { Text("Arama") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SpotGreen,
                            selectedTextColor = SpotGreen,
                            unselectedIconColor = TextGrey,
                            unselectedTextColor = TextGrey,
                            indicatorColor = DarkCardSurface
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.LIBRARY,
                        onClick = { viewModel.setTab(AppTab.LIBRARY) },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Kütüphane") },
                        label = { Text("Kütüphanem") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SpotGreen,
                            selectedTextColor = SpotGreen,
                            unselectedIconColor = TextGrey,
                            unselectedTextColor = TextGrey,
                            indicatorColor = DarkCardSurface
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.DOWNLOADER,
                        onClick = { viewModel.setTab(AppTab.DOWNLOADER) },
                        icon = { Icon(Icons.Default.Check, contentDescription = "Yükleyici") },
                        label = { Text("Yükleyici") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SpotGreen,
                            selectedTextColor = SpotGreen,
                            unselectedIconColor = TextGrey,
                            unselectedTextColor = TextGrey,
                            indicatorColor = DarkCardSurface
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            when (currentTab) {
                AppTab.HOME -> HomeTab(viewModel = viewModel)
                AppTab.SEARCH -> SearchTab(viewModel = viewModel)
                AppTab.LIBRARY -> LibraryTab(viewModel = viewModel)
                AppTab.DOWNLOADER -> DownloaderTab(viewModel = viewModel)
            }

            // Interactive Speech Mic floating widget in main screen for voice command support
            val isListeningVoice by viewModel.isListeningVoice.collectAsStateWithLifecycle()
            val voiceCommandResult by viewModel.voiceCommandResult.collectAsStateWithLifecycle()

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    if (voiceCommandResult.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkCardSurface,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .widthIn(max = 200.dp)
                                .border(1.dp, SpotGreen, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = voiceCommandResult,
                                color = White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    FloatingActionButton(
                        onClick = { viewModel.startSpeechRecognition() },
                        containerColor = if (isListeningVoice) Color.Red else SpotGreen,
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Sesli Kontrol Ses Dinleyici"
                        )
                    }
                }
            }

            // Expanding Full Active Screen Player with horizontal transitions
            AnimatedVisibility(
                visible = isPlayerExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                FullPlayerScreen(
                    viewModel = viewModel,
                    onDismiss = { isPlayerExpanded = false }
                )
            }
        }
    }
}

// ==========================================
// MINI PLAYER COMPONENT
// ==========================================

@Composable
fun MiniPlayerRow(
    track: Track,
    onClicked: () -> Unit
) {
    val isPlaying by PlaybackManager.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by PlaybackManager.isPlayingBuffering.collectAsStateWithLifecycle()
    val progress by PlaybackManager.playbackProgress.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable { onClicked() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.95f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Şu an çalıyor: ${track.title}",
                        color = White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.author,
                        color = TextGrey,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (isBuffering) {
                    CircularProgressIndicator(
                        color = SpotGreen,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    IconButton(onClick = { PlaybackManager.togglePlayPause(context) }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Oynat/Durdur",
                            tint = White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                IconButton(onClick = { PlaybackManager.skipToNext(context) }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Sıradaki",
                        tint = White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Beautiful progress line running at the bottom of the card
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.6.dp),
                color = SpotGreen,
                trackColor = Color.White.copy(alpha = 0.08f)
            )
        }
    }
}

// ==========================================
// TABS IMPLEMENTATION
// ==========================================

// ------------------------------------------
// HOME TAB (ANA SAYFA)
// ------------------------------------------

@Composable
fun HomeTab(viewModel: FlofysViewModel) {
    val history by viewModel.historyItems.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val downloaded by viewModel.downloadedTracks.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
    ) {
        // Elegant Dark Header branding
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(SpotGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.size(width = 15.dp, height = 2.dp).background(Color.Black, CircleShape))
                            Box(modifier = Modifier.size(width = 11.dp, height = 2.dp).background(Color.Black, CircleShape))
                            Box(modifier = Modifier.size(width = 7.dp, height = 2.dp).background(Color.Black, CircleShape))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Flofys",
                        color = White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(DarkCardSurface, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profil",
                        tint = White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "İyi Günler",
                color = White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
            )
        }

        // Quick shortcut grid for playlists
        item {
            if (playlists.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .height(140.dp)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(playlists.take(4)) { playlist ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(DarkCardSurface, RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.setTab(AppTab.LIBRARY) }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(SpotGreen, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (playlist.iconIdentifier == "favorite") Icons.Default.Favorite else Icons.Default.List,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = playlist.name,
                                    color = White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recently played section
        item {
            Text(
                text = "En Son Çalınanlar",
                color = White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
            )
        }

        if (history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Henüz parça oynatılmadı.\nArama panelinden müzik bulabilirsiniz.",
                        color = TextGrey,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(history) { historyItem ->
                        val track = Track(
                            id = historyItem.trackId,
                            title = historyItem.title,
                            author = historyItem.author,
                            durationText = historyItem.durationText,
                            thumbnailUrl = historyItem.thumbnailUrl,
                            localFilePath = historyItem.localFilePath,
                            isDownloaded = historyItem.localFilePath != null
                        )
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { viewModel.playTrack(track) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                            ) {
                                AsyncImage(
                                    model = track.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = track.title,
                                color = White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.author,
                                color = TextGrey,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Downloaded items count tile
        item {
            Text(
                text = "İndirilen Parçalar",
                color = White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
            )
        }

        if (downloaded.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCardSurface, RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = SpotGreen, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Henüz indirilmiş müzik bulunmuyor.\nMüzikleri doğrudan cihazınıza indirebilirsiniz.",
                            color = TextGrey,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            items(downloaded) { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                        .clickable { viewModel.playTrack(track) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(track.author, color = TextGrey, fontSize = 11.sp, maxLines = 1)
                    }
                    IconButton(onClick = { viewModel.deleteDownloadedTrack(track.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.Red, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// SEARCH TAB (YOUTUBE ARAMA & SUGGEST)
// ------------------------------------------

@Composable
fun SearchTab(viewModel: FlofysViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
    ) {
        Text(
            text = "Arama",
            color = White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // Custom search text bar - Beautifully rounded matching Design HTML input style
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            placeholder = { Text("Şarkı, sanatçı veya albüm ismi...", color = TextGrey, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGrey) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = TextGrey)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedBorderColor = SpotGreen,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = DarkCardSurface,
                unfocusedContainerColor = DarkCardSurface
            ),
            shape = RoundedCornerShape(24.dp) // Exact search bar rounded-full likeness
        )

        // Suggestions Dropdown View in real-time as typing
        if (suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column {
                    suggestions.take(6).forEach { word ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.triggerSearch(word)
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextGrey, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(word, color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SpotGreen)
            }
        } else if (searchResults.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(searchResults) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                            .clickable { viewModel.playTrack(track) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${track.author} • ${track.durationText}",
                                color = TextGrey,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Play/Download Actions
                        val progress = activeDownloads[track.id]
                        if (progress != null) {
                            CircularProgressIndicator(
                                progress = progress,
                                color = SpotGreen,
                                strokeWidth = 3.dp,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(4.dp)
                            )
                        } else {
                            IconButton(onClick = { viewModel.startTrackDownload(track) }) {
                                Icon(
                                    imageVector = if (track.isDownloaded) Icons.Default.Check else Icons.Default.Check,
                                    contentDescription = "İndir",
                                    tint = if (track.isDownloaded) SpotGreen else TextGrey
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Standard category grids typical of Spotify
            Text(
                text = "Hepsine Göz At",
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            val genres = listOf(
                Pair("Pop", Color(0xFFE1306C)),
                Pair("Chill", Color(0xFF3897F0)),
                Pair("Hip-Hop", Color(0xFF1DB954)),
                Pair("Klasik", Color(0xFFFF9F00)),
                Pair("Rock", Color(0xFF9E00FF)),
                Pair("Akustik", Color(0xFFFD1D1D))
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(genres) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(item.second, RoundedCornerShape(12.dp))
                            .clickable { viewModel.updateSearchQuery(item.first); viewModel.triggerSearch(item.first) }
                            .padding(12.dp)
                    ) {
                        Text(
                            text = item.first,
                            color = White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// LIBRARY TAB (KÜTÜPHANEM - PLAYLISTS)
// ------------------------------------------

@Composable
fun LibraryTab(viewModel: FlofysViewModel) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var isCreatingDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    var playlistDescInput by remember { mutableStateOf("") }

    // Selected playlist details sub-view or standard lists
    var activeLibraryPlaylist by remember { mutableStateOf<Playlist?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
    ) {
        if (activeLibraryPlaylist != null) {
            val playlist = activeLibraryPlaylist!!
            val playlistTracks by viewModel.getTracksForPlaylist(playlist.id).collectAsStateWithLifecycle(emptyList())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activeLibraryPlaylist = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = White)
                }
                Text(
                    text = playlist.name,
                    color = White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.deletePlaylist(playlist.id); activeLibraryPlaylist = null }) {
                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }

            Text(
                playlist.description ?: "Çalma listesi",
                color = TextGrey,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
            )

            if (playlistTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bu çalma listesi boş.\nArama kısmından müzik ekleyebilirsiniz.",
                        color = TextGrey,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(playlistTracks) { i, track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                                .clickable { viewModel.playQueue(playlistTracks, i) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = track.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(track.author, color = TextGrey, fontSize = 11.sp, maxLines = 1)
                            }
                            IconButton(onClick = { viewModel.removeTrackFromPlaylist(playlist.id, track.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = TextGrey, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Müzik Kitaplığın",
                    color = White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { isCreatingDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Yeni Çalma Listesi", tint = SpotGreen, modifier = Modifier.size(28.dp))
                }
            }

            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Hiç çalma listeniz yok.\nSağ üstteki + butonuna basarak oluşturun.",
                        color = TextGrey,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(playlists) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                                .clickable { activeLibraryPlaylist = playlist }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(SpotGreen.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                    .border(1.dp, SpotGreen.copy(alpha = 0.20f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (playlist.iconIdentifier == "favorite") Icons.Default.Favorite else Icons.Default.List,
                                    contentDescription = null,
                                    tint = SpotGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    playlist.name,
                                    color = White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    playlist.description ?: "Çalma listesi",
                                    color = TextGrey,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add Dialog logic
        if (isCreatingDialog) {
            AlertDialog(
                onDismissRequest = { isCreatingDialog = false },
                title = { Text("Yeni Çalma Listesi", color = White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = playlistNameInput,
                            onValueChange = { playlistNameInput = it },
                            label = { Text("İsim") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White, 
                                unfocusedTextColor = White,
                                focusedBorderColor = SpotGreen,
                                unfocusedBorderColor = Zinc700,
                                focusedLabelColor = SpotGreen,
                                unfocusedLabelColor = TextGrey
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = playlistDescInput,
                            onValueChange = { playlistDescInput = it },
                            label = { Text("Açıklama") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White, 
                                unfocusedTextColor = White,
                                focusedBorderColor = SpotGreen,
                                unfocusedBorderColor = Zinc700,
                                focusedLabelColor = SpotGreen,
                                unfocusedLabelColor = TextGrey
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (playlistNameInput.isNotBlank()) {
                            viewModel.createCustomPlaylist(playlistNameInput, playlistDescInput)
                            playlistNameInput = ""
                            playlistDescInput = ""
                            isCreatingDialog = false
                        }
                    }) {
                        Text("Oluştur", color = SpotGreen, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isCreatingDialog = false }) {
                        Text("İptal", color = TextGrey)
                    }
                },
                containerColor = DarkCardSurface,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

// ------------------------------------------
// DOWNLOADER PANEL (MÜZİK YÜKLEYİCİ GÜNLÜK)
// ------------------------------------------

@Composable
fun DownloaderTab(viewModel: FlofysViewModel) {
    val downloadLogs by viewModel.downloadLogs.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
    ) {
        Text(
            text = "Yükleyici Terminali",
            color = White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Active processes cards
        if (activeDownloads.isNotEmpty()) {
            Text(
                text = "Aktif Veri Dönüştürmeleri",
                color = SpotGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            activeDownloads.forEach { (trackId, progress) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCardSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Parça " + trackId,
                            color = White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        CircularProgressIndicator(
                            progress = progress,
                            color = SpotGreen,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            color = SpotGreen,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Terminal text log view
        Text(
            text = "Faaliyet Geçmişi",
            color = TextGrey,
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, DarkCardSurface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(downloadLogs) { log ->
                    Text(
                        text = log,
                        color = if (log.contains("HATA")) Color.Red else if (log.contains("BAŞARILI")) SpotGreen else TextGrey,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// ==========================================
// EXPANDED MUSIC PLAYER COMPONENT
// ==========================================

@Composable
fun FullPlayerScreen(
    viewModel: FlofysViewModel,
    onDismiss: () -> Unit
) {
    val track by PlaybackManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by PlaybackManager.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by PlaybackManager.isPlayingBuffering.collectAsStateWithLifecycle()
    val progress by PlaybackManager.playbackProgress.collectAsStateWithLifecycle()
    val posMs by PlaybackManager.currentPositionMs.collectAsStateWithLifecycle()
    val durMs by PlaybackManager.durationMs.collectAsStateWithLifecycle()
    val loopMode by PlaybackManager.loopMode.collectAsStateWithLifecycle()
    val queue by PlaybackManager.playbackQueue.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    var isAddToPlaylistOpen by remember { mutableStateOf(false) }
    var expandedQueueState by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (track == null) return

    val currentTrack = track!!

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkCardSurface,
                        DarkBackground
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dismiss arrow and screen title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Kapat", tint = White, modifier = Modifier.size(32.dp))
                }
                Text(
                    text = "Şu An Çalınan",
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = { isAddToPlaylistOpen = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Ekle", tint = White)
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Cover Album Art Image
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = currentTrack.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Metadata: title and author channel
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack.title,
                        color = White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentTrack.author,
                        color = TextGrey,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { viewModel.startTrackDownload(currentTrack) }) {
                    Icon(
                        imageVector = if (currentTrack.isDownloaded) Icons.Default.Check else Icons.Default.Check,
                        contentDescription = "İndir",
                        tint = if (currentTrack.isDownloaded) SpotGreen else White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Slider
            Slider(
                value = progress,
                onValueChange = { percent ->
                    val pos = (percent * durMs).toInt()
                    PlaybackManager.seekTo(pos)
                },
                colors = SliderDefaults.colors(
                    thumbColor = SpotGreen,
                    activeTrackColor = SpotGreen,
                    inactiveTrackColor = DarkCardSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Duration and Position labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(posMs),
                    color = TextGrey,
                    fontSize = 12.sp
                )
                Text(
                    text = formatMs(durMs),
                    color = TextGrey,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Music actions layout: Row centering Play buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Loop mode toggle
                IconButton(onClick = {
                    val nextLoop = when (loopMode) {
                        LoopMode.NONE -> LoopMode.REPEAT_ONE
                        LoopMode.REPEAT_ONE -> LoopMode.REPEAT_ALL
                        LoopMode.REPEAT_ALL -> LoopMode.NONE
                    }
                    PlaybackManager.setLoopMode(nextLoop)
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Loop",
                        tint = if (loopMode != LoopMode.NONE) SpotGreen else White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = { PlaybackManager.skipToPrevious(context) }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = White, modifier = Modifier.size(36.dp))
                }

                // Mini progress loading inside Main Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .background(White, CircleShape)
                        .clickable { PlaybackManager.togglePlayPause(context) }
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "PlayPause",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(onClick = { PlaybackManager.skipToNext(context) }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = White, modifier = Modifier.size(36.dp))
                }

                // Quick visual queue checker toggle
                IconButton(onClick = { expandedQueueState = !expandedQueueState }) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Queue List",
                        tint = if (expandedQueueState) SpotGreen else White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Additional rewind forward actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = { PlaybackManager.rewind10s() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "-10", tint = TextGrey, modifier = Modifier.size(20.dp))
                        Text("10s", color = TextGrey, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.width(36.dp))
                IconButton(onClick = { PlaybackManager.forward10s() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("10s+", color = TextGrey, fontSize = 11.sp)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "10+", tint = TextGrey, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Small active active indicator status of loop
            Text(
                text = when (loopMode) {
                    LoopMode.NONE -> "Döngü Modu Kapalı"
                    LoopMode.REPEAT_ONE -> "Tekrar Modu: Tek Parça"
                    LoopMode.REPEAT_ALL -> "Tekrar Modu: Tümü"
                },
                color = SpotGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            if (expandedQueueState && queue.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkCardSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Sıradaki Parçalar", color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn {
                            items(queue) { qTrack ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = qTrack.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        qTrack.title,
                                        color = if (qTrack.id == currentTrack.id) SpotGreen else White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add to playlist overlay dialog
        if (isAddToPlaylistOpen) {
            AlertDialog(
                onDismissRequest = { isAddToPlaylistOpen = false },
                title = { Text("Çalma Listesine Ekle", color = White) },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(playlists) { playlist ->
                            TextButton(
                                onClick = {
                                    viewModel.addTrackToPlaylist(playlist.id, currentTrack)
                                    isAddToPlaylistOpen = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(playlist.name, color = SpotGreen, modifier = Modifier.align(Alignment.CenterVertically))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { isAddToPlaylistOpen = false }) {
                        Text("Kapat", color = TextGrey)
                    }
                },
                containerColor = DarkCardSurface
            )
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
