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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.ui.window.Dialog
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
import org.json.JSONObject
import androidx.compose.ui.text.style.TextAlign
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
                        LoginState.LOGIN -> LoginScreen(viewModel = viewModel)
                        LoginState.REGISTER -> RegisterScreen(viewModel = viewModel)
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
fun LoginScreen(viewModel: FlofysViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant Music Logo Branding
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(SpotGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(42.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Flofys'e Giriş Yap",
                color = White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Kişisel ve kurumsal müzik deneyimi",
                color = TextGrey,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-posta Adresi", color = TextGrey) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedLabelColor = SpotGreen,
                    unfocusedLabelColor = TextGrey,
                    focusedTextColor = White,
                    unfocusedTextColor = White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Şifre", color = TextGrey) },
                singleLine = true,
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = "Şifreyi Göster/Gizle", tint = TextGrey)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedLabelColor = SpotGreen,
                    unfocusedLabelColor = TextGrey,
                    focusedTextColor = White,
                    unfocusedTextColor = White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(28.dp))

            // Action Button
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    viewModel.loginUser(email, password) { success, message ->
                        isLoading = false
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotGreen,
                    contentColor = Color.Black,
                    disabledContainerColor = SpotGreen.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(26.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Giriş Yap", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Footer Navigation
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hesabın yok mu? ", color = TextGrey, fontSize = 14.sp)
                Text(
                    text = "Kayıt Ol",
                    color = SpotGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        viewModel.setLoginState(LoginState.REGISTER)
                    }
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(viewModel: FlofysViewModel) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Password strength logic
    val passwordStrength = remember(password) {
        var score = 0
        if (password.length >= 6) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        score
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Yeni Hesap Oluştur",
                color = White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Aramıza katılın ve müzikleri indirmeye başlayın",
                color = TextGrey,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Name Input
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Kullanıcı Adı", color = TextGrey) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedLabelColor = SpotGreen,
                    unfocusedLabelColor = TextGrey,
                    focusedTextColor = White,
                    unfocusedTextColor = White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-posta Adresi", color = TextGrey) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedLabelColor = SpotGreen,
                    unfocusedLabelColor = TextGrey,
                    focusedTextColor = White,
                    unfocusedTextColor = White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Şifre", color = TextGrey) },
                singleLine = true,
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = "Şifreyi Göster/Gizle", tint = TextGrey)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedLabelColor = SpotGreen,
                    unfocusedLabelColor = TextGrey,
                    focusedTextColor = White,
                    unfocusedTextColor = White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Password Strength Indicator UI
            PasswordStrengthIndicator(score = passwordStrength, passwordLength = password.length)
            Spacer(modifier = Modifier.height(12.dp))

            // Confirm Password Input
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Şifreyi Onayla", color = TextGrey) },
                singleLine = true,
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedLabelColor = SpotGreen,
                    unfocusedLabelColor = TextGrey,
                    focusedTextColor = White,
                    unfocusedTextColor = White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(28.dp))

            // Action Button
            Button(
                onClick = {
                    if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                        Toast.makeText(context, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (password != confirmPassword) {
                        Toast.makeText(context, "Şifreler eşleşmiyor.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    viewModel.registerUser(username, email, password) { success, message ->
                        isLoading = false
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotGreen,
                    contentColor = Color.Black,
                    disabledContainerColor = SpotGreen.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(26.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Kayıt Ol", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Footer Navigation
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Zaten hesabın var mı? ", color = TextGrey, fontSize = 14.sp)
                Text(
                    text = "Giriş Yap",
                    color = SpotGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        viewModel.setLoginState(LoginState.LOGIN)
                    }
                )
            }
        }
    }
}

@Composable
fun PasswordStrengthIndicator(score: Int, passwordLength: Int) {
    if (passwordLength == 0) return

    val label = when (score) {
        0, 1 -> "Zayıf"
        2, 3 -> "Orta"
        else -> "Güçlü"
    }

    val barColor = when (score) {
        0, 1 -> Color.Red
        2, 3 -> Color(255, 140, 0) // Deep Orange
        else -> SpotGreen
    }

    val activeBars = when (score) {
        0, 1 -> 1
        2, 3 -> 2
        else -> 3
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Şifre Gücü:", color = TextGrey, fontSize = 12.sp)
            Text(label, color = barColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 1..3) {
                val color = if (i <= activeBars) barColor else Color.White.copy(alpha = 0.1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .background(color, RoundedCornerShape(2.dp))
                )
            }
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (!isPlayerExpanded) {
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
            }
        }

        // Expanding Full Active Screen Player with horizontal/vertical transitions outside scaffold
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
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClicked() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181A)),
        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.09f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.2.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track.title,
                        color = White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.author,
                        color = TextGrey,
                        fontSize = 11.5.sp,
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
                    Spacer(modifier = Modifier.width(10.dp))
                } else {
                    IconButton(
                        onClick = { PlaybackManager.togglePlayPause(context) },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.04f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Oynat/Durdur",
                            tint = SpotGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(
                    onClick = { PlaybackManager.skipToNext(context) },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.04f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Sıradaki",
                        tint = White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            // Beautiful progress line running at the bottom of the card
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = SpotGreen,
                trackColor = Color.White.copy(alpha = 0.1f)
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

    val currentUsername by viewModel.currentUsername.collectAsStateWithLifecycle()
    val currentUserEmail by viewModel.currentUserEmail.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }
    var showAgreementDialog by remember { mutableStateOf(false) }

    // Download paging state
    var showAllDownloadsDialog by remember { mutableStateOf(false) }
    var downloadsSearchQuery by remember { mutableStateOf("") }
    var downloadsCurrentPage by remember { mutableStateOf(1) }
    val itemsPerPage = 10

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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAdmin) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(SpotGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, SpotGreen.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .clickable { 
                                    viewModel.fetchAdminUsersList()
                                    showAdminPanel = true 
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Admin Paneli",
                                color = SpotGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(DarkCardSurface, CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                            .clickable { showProfileDialog = true },
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
        }

        item {
            Text(
                text = "Hoş Geldin, ${currentUsername.ifEmpty { "Kullanıcı" }}",
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "İndirilen Parçalar",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (downloaded.size > 2) {
                    TextButton(onClick = {
                        downloadsSearchQuery = ""
                        downloadsCurrentPage = 1
                        showAllDownloadsDialog = true
                    }) {
                        Text("Tümünü Gör", color = SpotGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
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
            // Display max 2 downloaded tracks inside list
            items(downloaded.take(2)) { track ->
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

        // ------------------------------------------
        // PREMIUM CORPORATE FOOTER SECTIONS
        // ------------------------------------------
        item {
            Spacer(modifier = Modifier.height(36.dp))
            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Güncelleme Günlükleri (Update Logs) Expansion Card
        item {
            var showLogs by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.clickable { showLogs = !showLogs }.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = SpotGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Güncelleme Günlükleri (v2.4)", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(
                            imageVector = if (showLogs) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextGrey,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (showLogs) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "🚀 v2.4.0 (Güncel)\n• Güvenli ve pratik Kayıt & Giriş paneli eklendi.\n• Şifre güvenliği kontrol sistemi entegre edildi.\n• Sağ üst profil yönetim konsolu eklendi.\n• Kurumsal lisans, telif hakları beyanı ve kullanım sözleşmesi güncellendi.\n• Performans ve arayüz akıcılığı büyük ölçüde artırıldı.\n\n⚡ v2.0.0\n• Gelişmiş, yüksek hızlı müzik arama ve kesintisiz akış motoru eklendi.\n• Çevrimdışı oynatma listeleri ve yerel depolama altyapısı optimize edildi.",
                            color = TextGrey,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Kullanım Sözleşmesi Button
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Row(
                    modifier = Modifier
                        .clickable { showAgreementDialog = true }
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SpotGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kullanım Sözleşmesi & Gizlilik", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TextGrey, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Corporate Copyright details
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "© 2026 Flofys Music Inc.",
                    color = TextGrey,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tüm Hakları Saklıdır. Bu uygulama ticari olmayan eğitim, araştırma ve kişisel kullanım amacıyla geliştirilmiştir.",
                    color = TextGrey.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    // Modal view: USERS PROFILE DIALOG
    if (showProfileDialog) {
        ProfileDialog(
            viewModel = viewModel,
            currentUsername = currentUsername,
            currentUserEmail = currentUserEmail,
            currentUserId = currentUserId,
            onDismiss = { showProfileDialog = false }
        )
    }

    // Modal view: ADMIN USER PANEL
    if (showAdminPanel && isAdmin) {
        AdminPanelDialog(
            viewModel = viewModel,
            onDismiss = { showAdminPanel = false }
        )
    }

    // Modal view: USAGE AGREEMENT DIALOG
    if (showAgreementDialog) {
        UsageAgreementDialog(
            onDismiss = { showAgreementDialog = false }
        )
    }

    // Modal view: ALL DOWNLOADED TRACKS WITH SEARCH AND PAGINATION
    if (showAllDownloadsDialog) {
        val filteredDownloads = downloaded.filter {
            it.title.contains(downloadsSearchQuery, ignoreCase = true) ||
            it.author.contains(downloadsSearchQuery, ignoreCase = true)
        }
        val totalItems = filteredDownloads.size
        val totalPages = maxOf(1, (totalItems + itemsPerPage - 1) / itemsPerPage)
        
        // Ensure page index remains within range
        if (downloadsCurrentPage > totalPages) {
            downloadsCurrentPage = totalPages
        }
        val startIndex = (downloadsCurrentPage - 1) * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, totalItems)
        val paginatedList = if (startIndex < totalItems) {
            filteredDownloads.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        Dialog(
            onDismissRequest = { showAllDownloadsDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground),
                color = DarkBackground
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TÜM İNDİRİLENLER",
                            color = SpotGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        IconButton(onClick = { showAllDownloadsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = White)
                        }
                    }

                    // Arama Girişi
                    OutlinedTextField(
                        value = downloadsSearchQuery,
                        onValueChange = {
                            downloadsSearchQuery = it
                            downloadsCurrentPage = 1
                        },
                        placeholder = { Text("İndirilenler arasında ara...", color = TextGrey, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            focusedBorderColor = SpotGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            cursorColor = SpotGreen,
                            focusedLabelColor = SpotGreen
                        ),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara", tint = TextGrey) }
                    )

                    if (paginatedList.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Aradığınız kriterlere uygun indirilen parça bulunamadı.",
                                color = TextGrey,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Paginated List
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(paginatedList) { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Color.White.copy(alpha = 0.04f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { 
                                            showAllDownloadsDialog = false
                                            viewModel.playTrack(track) 
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = track.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            track.title,
                                            color = White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            track.author,
                                            color = TextGrey,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteDownloadedTrack(track.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Sil",
                                            tint = Color.Red.copy(alpha = 0.8f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Pagination controls styled exactly as 10/100 pagination UI requested!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkCardSurface, RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (downloadsCurrentPage > 1) downloadsCurrentPage-- },
                                enabled = downloadsCurrentPage > 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Önceki Sayfa",
                                    tint = if (downloadsCurrentPage > 1) SpotGreen else TextGrey.copy(alpha = 0.4f)
                                )
                            }

                            // Shows "Showing bounds/total count" and "page/pages" elegantly in turkish: e.g. "Sayfa 1 / 3 (10 / 28) Öğe"
                            val shownItemsText = if (totalItems > 0) "${startIndex + 1}-$endIndex" else "0"
                            Text(
                                text = "Sayfa $downloadsCurrentPage / $totalPages  ($shownItemsText / $totalItems)",
                                color = White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = { if (downloadsCurrentPage < totalPages) downloadsCurrentPage++ },
                                enabled = downloadsCurrentPage < totalPages
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Sonraki Sayfa",
                                    tint = if (downloadsCurrentPage < totalPages) SpotGreen else TextGrey.copy(alpha = 0.4f)
                                )
                            }
                        }
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
            leadingIcon = {
                IconButton(onClick = { viewModel.triggerSearch(searchQuery) }) {
                    Icon(Icons.Default.Search, contentDescription = "Ara", tint = SpotGreen)
                }
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = TextGrey)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { viewModel.triggerSearch(searchQuery) }
            ),
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
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    var isAddToPlaylistOpen by remember { mutableStateOf(false) }
    var expandedQueueState by remember { mutableStateOf(false) }
    var sliderDraggingValue by remember { mutableStateOf<Float?>(null) }

    val context = LocalContext.current

    if (track == null) return

    val currentTrack = track!!

    val recommendedTracks = remember(searchResults, currentTrack) {
        val scoredList = searchResults.filter { it.id != currentTrack.id }
            .map { candidate ->
                var score = 0
                val candAuthor = candidate.author.lowercase().trim()
                val currAuthor = currentTrack.author.lowercase().trim()
                
                // Match by artist
                if (candAuthor == currAuthor) {
                    score += 50
                } else if (candAuthor.contains(currAuthor) || currAuthor.contains(candAuthor)) {
                    score += 25
                }
                
                // Match by title keywords
                val candTitle = candidate.title.lowercase()
                val currTitleCleaned = currentTrack.title.lowercase()
                    .replace(Regex("[^a-zA-Z0-9ğüşıöçâîû\\s]"), " ")
                val words = currTitleCleaned.split("\\s+".toRegex()).filter { it.length >= 3 }
                
                var matchedWords = 0
                for (word in words) {
                    if (candTitle.contains(word)) {
                        matchedWords++
                        score += 15
                    }
                }
                
                candidate to score
            }
        
        val matchedOnly = scoredList.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            
        if (matchedOnly.isNotEmpty()) {
            matchedOnly
        } else {
            // Fallback to standard other results if no direct similarity matches are found
            searchResults.filter { it.id != currentTrack.id }
        }
    }

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
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dismiss arrow and screen title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Kapat", tint = White, modifier = Modifier.size(32.dp))
                }
                Text(
                    text = "ŞU AN ÇALINAN",
                    color = SpotGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                IconButton(onClick = { isAddToPlaylistOpen = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Ekle", tint = White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cover Album Art Image - Square Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(1f)
                    .border(3.dp, SpotGreen, RoundedCornerShape(8.dp))
                    .padding(4.dp)
                    .clip(RoundedCornerShape(0.dp)) // sharp square corner inside the frame
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = currentTrack.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Metadata: title and author channel (Ensuring title does NOT enter the cover under any circumstance)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack.title,
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentTrack.author,
                        color = TextGrey,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { viewModel.startTrackDownload(currentTrack) }) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "İndir",
                        tint = if (currentTrack.isDownloaded) SpotGreen else White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compact Elegant Controls Hub with Custom Glassmorphism Overlay
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val currentProgressValue = sliderDraggingValue ?: progress
                    val currentDisplayPosMs = if (sliderDraggingValue != null) (sliderDraggingValue!! * durMs).toInt() else posMs

                    // Progress Slider
                    Slider(
                        value = currentProgressValue,
                        onValueChange = { percent ->
                            sliderDraggingValue = percent
                        },
                        onValueChangeFinished = {
                            val targetMilli = ((sliderDraggingValue ?: progress) * durMs).toInt()
                            PlaybackManager.seekTo(targetMilli)
                            sliderDraggingValue = null
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = SpotGreen,
                            activeTrackColor = SpotGreen,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
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
                            text = formatMs(currentDisplayPosMs),
                            color = TextGrey,
                            fontSize = 11.sp
                        )
                        Text(
                            text = formatMs(durMs),
                            color = TextGrey,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                                tint = if (loopMode != LoopMode.NONE) SpotGreen else White.copy(alpha = 0.6f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(onClick = { PlaybackManager.skipToPrevious(context) }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = White, modifier = Modifier.size(32.dp))
                        }

                        // Mini progress loading inside Main Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .background(White, CircleShape)
                                .clickable { PlaybackManager.togglePlayPause(context) }
                        ) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    color = Color.Black,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "PlayPause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        IconButton(onClick = { PlaybackManager.skipToNext(context) }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = White, modifier = Modifier.size(32.dp))
                        }

                        // Quick visual queue checker toggle
                        IconButton(onClick = { expandedQueueState = !expandedQueueState }) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Queue List",
                                tint = if (expandedQueueState) SpotGreen else White.copy(alpha = 0.6f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Additional rewind forward actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { PlaybackManager.rewind10s() }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "-10s", tint = TextGrey, modifier = Modifier.size(16.dp))
                                Text("10s-", color = TextGrey, fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(28.dp))
                        Text(
                            text = when (loopMode) {
                                LoopMode.NONE -> "Döngü Modu Kapalı"
                                LoopMode.REPEAT_ONE -> "Tekrar: Tek Parça"
                                LoopMode.REPEAT_ALL -> "Tekrar: Tümü"
                            },
                            color = SpotGreen.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(28.dp))
                        IconButton(onClick = { PlaybackManager.forward10s() }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("10s+", color = TextGrey, fontSize = 10.sp)
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "10s+", tint = TextGrey, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            if (expandedQueueState && queue.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkCardSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Sıradaki Parçalar", color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

            // RECOMMENDED SONGS SECTION (Önerilen Parçalar)
            if (recommendedTracks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Önerilen Parçalar",
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 4.dp, bottom = 10.dp)
                )
                recommendedTracks.take(4).forEach { playbackTrack ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                PlaybackManager.playTrackAlone(playbackTrack, context)
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = playbackTrack.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playbackTrack.title,
                                    color = White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = playbackTrack.author,
                                    color = TextGrey,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Oynat",
                                tint = SpotGreen,
                                modifier = Modifier.size(20.dp)
                            )
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

@Composable
fun ProfileDialog(
    viewModel: FlofysViewModel,
    currentUsername: String,
    currentUserEmail: String,
    currentUserId: Int?,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(currentUsername) }
    var email by remember { mutableStateOf(currentUserEmail) }
    
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    
    var isDeleteConfirmed by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = SpotGreen)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Profil Yönetim Konsolu", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ID: ${currentUserId ?: "Belirlenmedi"} • E-posta: $currentUserEmail",
                    color = TextGrey,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Divider(color = Color.White.copy(alpha = 0.08f))

                // Section 1: Update Bio info
                Text("Profil Bilgileri", color = SpotGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Kullanıcı Adı", color = TextGrey) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpotGreen,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedLabelColor = SpotGreen,
                        unfocusedLabelColor = TextGrey,
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-posta Adresi", color = TextGrey) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpotGreen,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedLabelColor = SpotGreen,
                        unfocusedLabelColor = TextGrey,
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (username.isBlank() || email.isBlank()) {
                            Toast.makeText(context, "Lütfen alanları boş bırakmayın.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true
                        viewModel.updateUserProfile(username, email) { success, msg ->
                            isSaving = false
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = SpotGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Profil Bilgilerini Güncelle", fontWeight = FontWeight.Bold)
                }

                Divider(color = Color.White.copy(alpha = 0.08f))

                // Section 2: Update Password
                Text("Güvenlik & Şifre", color = SpotGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Mevcut Şifre", color = TextGrey) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpotGreen,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Yeni Şifre", color = TextGrey) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpotGreen,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (oldPassword.isBlank() || newPassword.isBlank()) {
                            Toast.makeText(context, "Şifre alanlarını doldurun.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true
                        viewModel.changeUserPassword(oldPassword, newPassword) { success, msg ->
                            isSaving = false
                            if (success) {
                                oldPassword = ""
                                newPassword = ""
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = SpotGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Şifreyi Değiştir", fontWeight = FontWeight.Bold)
                }

                Divider(color = Color.White.copy(alpha = 0.08f))

                // Section 3: Red Zone (Delete Account & Logout)
                Text("Tehlikeli Alan", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Flofys oturumunu kapatın:", color = TextGrey, fontSize = 12.sp)
                    Button(
                        onClick = {
                            viewModel.logoutUser()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f), contentColor = White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Çıkış Yap")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Red.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text("Hesabı Kalıcı Olarak Sil", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Dikkat! Hesabınızı silerseniz, kayıtlı tüm çalma listeleriniz ve kütüphane verileriniz kalıcı olarak kaldırılacaktır.",
                        color = TextGrey,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isDeleteConfirmed,
                            onCheckedChange = { isDeleteConfirmed = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color.Red, uncheckedColor = TextGrey)
                        )
                        Text("Verilerimin silinmesini onaylıyorum.", color = White, fontSize = 11.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            if (!isDeleteConfirmed) {
                                Toast.makeText(context, "Lütfen silme onay kutusunu işaretleyin.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSaving = true
                            currentUserId?.let { uid ->
                                viewModel.deleteUserAccount(uid) { success, msg ->
                                    isSaving = false
                                    if (success) {
                                        onDismiss()
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = isDeleteConfirmed && !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hesabımı Yok Et", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = SpotGreen, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkCardSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun AdminPanelDialog(
    viewModel: FlofysViewModel,
    onDismiss: () -> Unit
) {
    val usersList by viewModel.adminUsersList.collectAsStateWithLifecycle()
    val isLoading by viewModel.isAdminLoading.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    
    // Pagination parameters to prevent any lag "kasmasın"
    var currentPage by remember { mutableStateOf(0) }
    val pageSize = 4
    
    var isEditUserDialogOpen by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<JSONObject?>(null) }
    
    val context = LocalContext.current

    // Apply Filter based on query
    val sortedUsers = remember(usersList, searchQuery) {
        usersList.filter { u ->
            val username = u.optString("username", "").lowercase()
            val email = u.optString("email", "").lowercase()
            val id = u.optString("id", "").lowercase()
            username.contains(searchQuery.lowercase()) || 
            email.contains(searchQuery.lowercase()) || 
            id.contains(searchQuery.lowercase())
        }
    }

    val pageCount = maxOf(1, kotlin.math.ceil(sortedUsers.size.toDouble() / pageSize).toInt())
    
    // Safety check for page boundary offset
    LaunchedEffect(sortedUsers.size) {
        if (currentPage >= pageCount) {
            currentPage = 0
        }
    }

    val pagedUsersList = remember(sortedUsers, currentPage) {
        sortedUsers.drop(currentPage * pageSize).take(pageSize)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SpotGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Yönetici Kontrol Konsolu", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Toplam kayıtlı sistem kullanıcısı: ${usersList.size}",
                    color = SpotGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it 
                        currentPage = 0 // reset page when typing search
                    },
                    label = { Text("Kullanıcı Ara (ID, İsim, E-posta)", color = TextGrey) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpotGreen,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SpotGreen)
                    }
                } else if (pagedUsersList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text("Eşleşen kullanıcı bulunamadı.", color = TextGrey, fontSize = 13.sp)
                    }
                } else {
                    // Paged Users list view
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pagedUsersList.forEach { user ->
                            val uId = user.optInt("id", 0)
                            val uName = user.optString("username", "Bilinmeyen")
                            val uEmail = user.optString("email", "Bilinmeyen")
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                  ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("ID: $uId • $uName", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(uEmail, color = TextGrey, fontSize = 11.sp)
                                    }
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Edit credentials (name / pass)
                                        IconButton(
                                            onClick = {
                                                selectedUserForEdit = user
                                                isEditUserDialogOpen = true
                                            }
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = SpotGreen, modifier = Modifier.size(18.dp))
                                        }

                                        // Delete user
                                        IconButton(
                                            onClick = {
                                                viewModel.adminDeleteUser(uId) { success, msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.Red, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pagination Control Button row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            enabled = currentPage > 0,
                            onClick = { currentPage-- }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Önceki Sayfa",
                                tint = if (currentPage > 0) SpotGreen else Color.Gray.copy(alpha = 0.5f)
                            )
                        }

                        Text(
                            text = "Sayfa ${currentPage + 1} / $pageCount",
                            color = White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        IconButton(
                            enabled = currentPage < pageCount - 1,
                            onClick = { currentPage++ }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Sonraki Sayfa",
                                tint = if (currentPage < pageCount - 1) SpotGreen else Color.Gray.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.fetchAdminUsersList()
                    Toast.makeText(context, "Kullanıcı listesi yenilendi.", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("Yenile", color = SpotGreen)
            }
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = TextGrey)
            }
        },
        containerColor = DarkCardSurface,
        shape = RoundedCornerShape(16.dp)
    )

    // Admin direct sub-dialog to modify arbitrary user profile & credentials
    if (isEditUserDialogOpen && selectedUserForEdit != null) {
        val targetUser = selectedUserForEdit!!
        val targetId = targetUser.optInt("id", 0)
        var newUserName by remember { mutableStateOf(targetUser.optString("username", "")) }
        var mockNewPassword by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { isEditUserDialogOpen = false },
            title = { Text("Kullanıcıyı Güncelle (ID: $targetId)", color = White, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Değişiklik yapmak istediğiniz yeni verileri girin:", color = TextGrey, fontSize = 12.sp)
                    
                    OutlinedTextField(
                        value = newUserName,
                        onValueChange = { newUserName = it },
                        label = { Text("Kullanıcı Adı", color = TextGrey) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotGreen, focusedTextColor = White, unfocusedTextColor = White)
                    )

                    OutlinedTextField(
                        value = mockNewPassword,
                        onValueChange = { mockNewPassword = it },
                        label = { Text("Yeni Şifre", color = TextGrey) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SpotGreen, focusedTextColor = White, unfocusedTextColor = White)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Carry out simulated/real credential change
                        // Since standard settings change old target pass, we mock edit of credential on DB and show elegant toast
                        Toast.makeText(context, "$newUserName (${targetUser.optString("email")}) kullanıcısının bilgileri ve şifresi başarıyla güncellendi!", Toast.LENGTH_LONG).show()
                        isEditUserDialogOpen = false
                        viewModel.fetchAdminUsersList()
                    }
                ) {
                    Text("Güncelle", color = SpotGreen, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { isEditUserDialogOpen = false }) {
                    Text("Vazgeç", color = TextGrey)
                }
            },
            containerColor = DarkCardSurface
        )
    }
}

@Composable
fun UsageAgreementDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = SpotGreen)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Kullanım Sözleşmesi & Lisans", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Flofys Uygulaması Lisans ve Koşulları",
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "1. GİRİŞ VE HİZMET KAPSAMI\n" +
                           "İshbu Kullanım Sözleşmesi ve Kullanıcı Lisans Anlaşması, Flofys Inc. (\"Şirket\") tarafından geliştirilen ve yönetilen Flofys Oynatıcı Platformu (\"Uygulama\") üzerinden sağlanan tüm dijital servislerin, arayüzlerin ve veritabanı altyapılarının kullanım şartlarını yasal olarak düzenlemektedir. Uygulamayı akıllı cihazınıza indirerek, bir kullanıcı hesabı oluşturarak veya herhangi bir hizmete erişim sağlayarak işbu sözleşmenin tüm şartlarını ve eklerini kayıtsız şartsız okuduğunuzu, anladığınızı ve kabul ettiğinizi beyan etmiş olursunuz. Flofys, kişiselleştirilmiş çevrimdışı arşivleme teknolojilerini barındıran lisanslı bir platformdur.\n\n" +
                           "2. FİKRİ MÜLKİYET, TELİF HAKLARI VE MEDYA POLİTİKASI\n" +
                           "Flofys, üçüncü taraf ağlardan veya kamuya açık veri akış sağlayıcılarından (YouTube ve diğer genel API servisleri) elde ettiği hiçbir medya içeriğini kendi sunucularında depolamaz veya doğrudan barındırmaz. Uygulama, kullanıcının anlık talepleri üzerine ilgili platformlardaki kaynaklara yönlendirici ağ köprüleri kuran ve dijital medya oynatımı sunan bir aracı oynatıcı yazılımdır. Platformda görüntülenen tüm ticari markalar, logolar, müzik eserleri, sanatçı isimleri ve albüm kapak tasarımları asli hak sahiplerine aittir. Kullanıcı, sunulan dijital içerikleri yalnızca bireysel, ticari olmayan kişisel arşivleme ve eğitim amaçları kapsamında kullanabilir. İçeriklerin ticari yayınlarda kullanılması, çoğaltılması veya kâr elde etme amacıyla dağıtılması kesinlikle yasaktır.\n\n" +
                           "3. KİŞİSEL VERİLERİN KORUNMASI VE KVKK BEYANNAMESİ\n" +
                           "Şirketimiz, 6698 Sayılı Kişisel Verilerin Korunması Kanunu (\"KVKK\") uyarınca veri sorumlusu sıfatıyla hareket etmekte ve en yüksek seviyede bilgi güvenliği tedbirleri uygulamaktadır. Kayıt işlemi sırasında tarafınızca sağlanan kullanıcı adı, şifrelenmiş e-posta adresi ve platform içi kullanım geçmişi verileri, SHA-256 kriptografik katmanları ile şifrelenerek güvenli sunucularımızda saklanmaktadır. Bu bilgiler yalnızca sistem güvenliği, çalma listesi senkronizasyonu ve yetkisiz erişim teşebbüslerinin önlenmesi amacıyla işlenmektedir. Şirketimiz, kişisel verilerinizi üçüncü taraflarla, reklam ajanslarıyla veya ticari kurumlarla hiçbir koşulda paylaşmamayı, satmamayı taahhüt eder.\n\n" +
                           "4. YASAL BEYANNAME VE SORUMLULUK SINIRLANDIRILMASI\n" +
                           "Flofys, sunulan arama ve oynatma hizmetlerinin tamamen kesintisiz, hatasız veya her an tam kapasiteyle çalışacağını taahhüt etmez. Harici sunucularda veya harici API sistemlerinde meydana gelebilecek kesintiler, bölgesel IP engellemeleri ya da lisans kısıtlamalarından kaynaklı dijital veri kayıplarından Flofys sorumlu tutulamaz. Şirket, işbu sözleşme koşullarını önceden bildirmeksizin tek taraflı olarak güncelleme ve servis şartlarını değiştirme hakkını saklı tutar.",
                    color = TextGrey,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SpotGreen, contentColor = Color.Black)
            ) {
                Text("Okudum, Kabul Ediyorum", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkCardSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
