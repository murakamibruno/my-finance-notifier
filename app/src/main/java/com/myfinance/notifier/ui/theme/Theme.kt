package com.myfinance.notifier.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1DAD62),       // Forest Green
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDFDF5),
    onPrimaryContainer = Color(0xFF04291C),
    secondary = Color(0xFF6B9FFF),      // Cool Blue
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    onSurface = Color(0xFF1C2135),
    onSurfaceVariant = Color(0xFF7A8299),
    error = Color(0xFFFF6B7A),          // Debit Red
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DFFA0),        // Planance Green
    onPrimary = Color(0xFF04291C),
    primaryContainer = Color(0xFF0E4831),
    onPrimaryContainer = Color(0xFF4DFFA0),
    secondary = Color(0xFF6B9FFF),      // Cool Blue
    background = Color(0xFF0E0F11),     // Obsidian
    surface = Color(0xFF1E2128),        // Slate Dark
    onSurface = Color(0xFFF0F2F5),      // Ghost White
    onSurfaceVariant = Color(0xFF7A8299),
    error = Color(0xFFFF6B7A),          // Debit Red
    onError = Color(0xFF0E0F11)
)

@Composable
fun PlananceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
