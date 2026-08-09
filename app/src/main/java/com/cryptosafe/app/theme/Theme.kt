package com.cryptosafe.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.cryptosafe.app.LocalizationManager




private val Ink = Color(0xFF13201E)          
private val InkSurface = Color(0xFF1B2926)   
private val InkSurfaceVariant = Color(0xFF25352F) 
private val Paper = Color(0xFFEBE7DC)        

private val Vault = Color(0xFF5FAE9B)        
private val VaultOnPrimary = Color(0xFF08211C)
private val VaultContainer = Color(0xFF2C5C50)
private val VaultOnContainer = Color(0xFFD9F0E9)

private val Brass = Color(0xFFD3A94F)        
private val BrassOnSecondary = Color(0xFF2B2005)
private val BrassContainer = Color(0xFF4A3A17)
private val BrassOnContainer = Color(0xFFF3E3BB)

private val Brick = Color(0xFFE0917A)        
private val BrickOnError = Color(0xFF3A130A)
private val BrickContainer = Color(0xFF592A1D)
private val BrickOnContainer = Color(0xFFF7DCD1)

private val Outline = Color(0xFF5C6D66)      
private val MutedText = Color(0xFFB9C7C0)    



val FieldFill = Color(0xFF25352F)            
val FieldFillFocused = Color(0xFF2A3B34)     
val FieldText = Color(0xFFEBE7DC)            
val FieldBorderFocused = Brass               

private val VaultColorScheme = darkColorScheme(
    primary = Vault,
    onPrimary = VaultOnPrimary,
    primaryContainer = VaultContainer,
    onPrimaryContainer = VaultOnContainer,

    secondary = Brass,
    onSecondary = BrassOnSecondary,
    secondaryContainer = BrassContainer,
    onSecondaryContainer = BrassOnContainer,

    background = Ink,
    onBackground = Paper,

    surface = InkSurface,
    onSurface = Paper,
    surfaceVariant = InkSurfaceVariant,
    onSurfaceVariant = MutedText,

    error = Brick,
    onError = BrickOnError,
    errorContainer = BrickContainer,
    onErrorContainer = BrickOnContainer,

    outline = Outline,
    outlineVariant = Outline.copy(alpha = 0.4f),

    inverseSurface = Paper,
    inverseOnSurface = Ink,
)


private val VaultTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium),
        titleMedium = base.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium),
    )
}

private val VaultShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun CryptoSafeTheme(content: @Composable () -> Unit) {
    val layoutDirection = if (LocalizationManager.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = VaultColorScheme,
            typography = VaultTypography,
            shapes = VaultShapes,
            content = content
        )
    }
}
