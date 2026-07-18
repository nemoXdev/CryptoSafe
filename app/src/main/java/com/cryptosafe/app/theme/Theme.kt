package com.cryptosafe.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.cryptosafe.app.LocalizationManager

@Composable
fun CryptoSafeTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = Color(0xFF00BFA5),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF00574B),
        secondary = Color(0xFF7C4DFF),
        secondaryContainer = Color(0xFF311B92),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E2A),
        surfaceVariant = Color(0xFF2A2A3C),
        error = Color(0xFFCF6679),
        onBackground = Color.White,
        onSurface = Color.White,
    )
    val layoutDirection = if (LocalizationManager.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content
        )
    }
}
