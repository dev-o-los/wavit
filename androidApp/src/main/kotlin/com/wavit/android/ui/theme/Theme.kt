package com.wavit.android.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6C93FF),
    secondary = Color(0xFF8692A6),
    tertiary = Color(0xFFEF8354),
    background = Color(0xFF0F1117),
    surface = Color(0xFF181B24),
    surfaceVariant = Color(0xFF242836),
    onPrimary = Color(0xFF001E50),
    onBackground = Color(0xFFE2E4EC),
    onSurface = Color(0xFFE2E4EC)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2F54EB),
    secondary = Color(0xFF596579),
    tertiary = Color(0xFFD9531E),
    background = Color(0xFFF8F9FD),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8EAF2),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF191C22),
    onSurface = Color(0xFF191C22)
)

@Composable
fun WavitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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

