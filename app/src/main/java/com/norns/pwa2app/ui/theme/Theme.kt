package com.norns.pwa2app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FluentBlueDark,
    onPrimary = FluentInk,
    secondary = FluentCyan,
    tertiary = FluentTeal,
    background = Color(0xFF09111D),
    onBackground = Color(0xFFF3F7FD),
    surface = FluentSurfaceDark,
    onSurface = Color(0xFFF3F7FD),
    surfaceVariant = FluentSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFD0D9E7),
    outline = Color(0xFF52637D),
    outlineVariant = Color(0x403B4B63)
)

private val LightColorScheme = lightColorScheme(
    primary = FluentBlue,
    onPrimary = Color.White,
    secondary = FluentCyan,
    tertiary = FluentTeal,
    background = FluentCloud,
    onBackground = FluentInk,
    surface = FluentSurfaceLight,
    onSurface = FluentInk,
    surfaceVariant = FluentSurfaceVariantLight,
    onSurfaceVariant = FluentSlate,
    outline = FluentOutline,
    outlineVariant = Color(0x80DCE5F1)
)

@Composable
fun PWA2APPTheme(
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
        typography = Typography,
        content = content
    )
}
