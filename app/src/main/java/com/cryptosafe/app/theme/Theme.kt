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

/**
 * ثيم "خزنة ونحاس" (Vault & Brass)
 * ------------------------------------------------------------
 * فكرة التصميم: تطبيق عن الأمان والخصوصية والصناديق المشفّرة - بدل الألوان
 * التقنية الفاقعة المعتادة (تركواز/بنفسجي "AI عام")، اخترنا لوحة مستوحاة من
 * فكرة الخزنة الحقيقية: أخضر-تركوازي غامق هادئ (المعدن/الأمان) + نحاسي دافئ
 * (المفتاح/القفل)، على خلفية داكنة دافئة (مو أسود باهت افتراضي).
 *
 * للرجوع للثيم القديم: استبدل هذا الملف بنسخة Theme_Original_Backup.kt
 * (تم تسليمها كملف منفصل) - لا حاجة لأي تعديل بأي ملف آخر.
 */

// ---- لوحة الألوان المسمّاة (Token System) ----
private val Ink = Color(0xFF13201E)          // خلفية عميقة دافئة (مو أسود بحت)
private val InkSurface = Color(0xFF1B2926)   // سطح البطاقات الأساسي
private val InkSurfaceVariant = Color(0xFF25352F) // سطح مرتفع شوي (بطاقات ثانوية، فقاعات الدردشة)
private val Paper = Color(0xFFEBE7DC)        // نص/محتوى - أبيض دافئ بلون الورق، مو أبيض صافي

private val Vault = Color(0xFF5FAE9B)        // اللون الأساسي - تركوازي-أخضر هادئ (الخزنة)
private val VaultOnPrimary = Color(0xFF08211C)
private val VaultContainer = Color(0xFF2C5C50)
private val VaultOnContainer = Color(0xFFD9F0E9)

private val Brass = Color(0xFFD3A94F)        // اللون الثانوي - نحاسي دافئ (المفتاح)
private val BrassOnSecondary = Color(0xFF2B2005)
private val BrassContainer = Color(0xFF4A3A17)
private val BrassOnContainer = Color(0xFFF3E3BB)

private val Brick = Color(0xFFE0917A)        // للأخطاء/التحذيرات - طوبي دافئ، مو أحمر صارخ
private val BrickOnError = Color(0xFF3A130A)
private val BrickContainer = Color(0xFF592A1D)
private val BrickOnContainer = Color(0xFFF7DCD1)

private val Outline = Color(0xFF5C6D66)      // حدود/فواصل هادئة
private val MutedText = Color(0xFFB9C7C0)    // نص ثانوي (تواريخ، تلميحات)

// ---- شكل حقول الإدخال "المملوءة": خلفية داكنة هادئة تنسجم مع الخلفية العامة،
// وحدّ نحاسي بارز عند التركيز (بدل الخلفية الفاتحة الصفراء المتعبة للعين) ----
val FieldFill = Color(0xFF25352F)            // خلفية داكنة هادئة (سطح مرتفع)
val FieldFillFocused = Color(0xFF2A3B34)     // أفتح قليلاً عند التركيز
val FieldText = Color(0xFFEBE7DC)            // نص فاتح يقرأ بوضوح فوق الخلفية الغامقة
val FieldBorderFocused = Brass               // حد نحاسي بارز عند التركيز

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

// خط العناوين Serif (يدي إحساس "دفتر/سجل" مميز) + خط النص العادي.
// الخطين مدمجين بالنظام (offline، بدون تحميل شبكة) - يحافظ على فلسفة "بدون إنترنت".
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
