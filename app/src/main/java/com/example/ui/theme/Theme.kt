package com.example.ui.theme

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
    primary = KisumuMint,
    onPrimary = CharcoalDark,
    primaryContainer = KisumuForestGreen,
    onPrimaryContainer = Color(0xFFD4EDDA),
    secondary = KisumuLakeBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0D47A1),
    onSecondaryContainer = Color(0xFFBBDEFB),
    tertiary = KisumuLightGreen,
    background = CharcoalDark,
    onBackground = Color(0xFFE8ECE9),
    surface = CharcoalSurface,
    onSurface = Color(0xFFE8ECE9),
    surfaceVariant = CharcoalCard,
    onSurfaceVariant = CharcoalMuted,
    outline = CharcoalBorder
)

private val LightColorScheme = lightColorScheme(
    primary = KisumuForestGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E8DC),
    onPrimaryContainer = KisumuForestGreen,
    secondary = KisumuLakeBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1F5FE),
    onSecondaryContainer = Color(0xFF01579B),
    tertiary = KisumuEmerald,
    background = LightBackground,
    onBackground = TextDark,
    surface = LightSurface,
    onSurface = TextDark,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = LightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to maintain the signature Kisumu green branding
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
