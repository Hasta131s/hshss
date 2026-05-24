package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SpotGreen,
    onPrimary = Color.Black,
    secondary = DarkGreen,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = White,
    surface = DarkSurface,
    onSurface = White,
    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = TextGrey
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for Flofys matching Spotify
    dynamicColor: Boolean = false, // Disable dynamic colors to protect the Flofys dark-green branding
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
