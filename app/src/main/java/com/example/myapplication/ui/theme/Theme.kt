package com.example.myapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF1B5E20), // Deep Emerald Green
    secondary = Color(0xFF00897B), // Teal
    tertiary = Color(0xFF9CCC65), // Muted Lime Green
    background = Color(0xFF121212), // Charcoal Black (true dark mode)
    surface = Color(0xFF263238), // Dark Forest Green
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color(0xFF121212), // Dark text for contrast
    onBackground = Color(0xFFE0E0E0), // Light gray for readability
    onSurface = Color(0xFFE0E0E0), // Light gray for better contrast
    surfaceVariant = Color(0xFF37474F), // Bluish Gray for a sleek look
    onSurfaceVariant = Color(0xFFB0BEC5) // Soft Gray for subtext
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32), // Forest Green
    secondary = Color(0xFF00796B), // Teal
    tertiary = Color(0xFFAED581), // Soft Lime Green
    background = Color(0xFFFAFAFA), // Soft White
    surface = Color(0xFFE8F5E9), // Light Mint Green
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color(0xFF1B5E20), // Deep Green for contrast
    onBackground = Color(0xFF37474F), // Dark Greenish Gray for readability
    onSurface = Color(0xFF37474F), // Consistent with text
    surfaceVariant = Color(0xFFC8E6C9), // Soft pastel green for balance
    onSurfaceVariant = Color(0xFF455A64) // Muted blue-gray for softer contrast
)
/**
 * Applies the custom theme to the application.
 *
 * @param darkTheme Whether the dark theme is enabled.
 * @param content The content to be displayed within the theme.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}