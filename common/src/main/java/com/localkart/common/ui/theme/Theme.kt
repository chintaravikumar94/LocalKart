package com.localkart.common.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---- Brand palette ----
val Primary = Color(0xFF2563EB)        // customer blue
val PrimaryDark = Color(0xFF1E40AF)
val SellerPrimary = Color(0xFF7C3AED)  // seller violet
val Accent = Color(0xFFF59E0B)         // amber
val Success = Color(0xFF16A34A)

// ---- Customer (blue) light ----
private val CustomerLight = lightColorScheme(
    primary = Primary, onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF), onPrimaryContainer = Color(0xFF0A1F52),
    secondary = Accent, onSecondary = Color(0xFF2A1E00),
    secondaryContainer = Color(0xFFFFE7B3), onSecondaryContainer = Color(0xFF271900),
    tertiary = Success, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC7F2D4), onTertiaryContainer = Color(0xFF052E15),
    background = Color(0xFFF6F8FC), onBackground = Color(0xFF121826),
    surface = Color.White, onSurface = Color(0xFF121826),
    surfaceVariant = Color(0xFFEDF1F8), onSurfaceVariant = Color(0xFF4B5566),
    outline = Color(0xFFC4CCDA), outlineVariant = Color(0xFFDDE3EC),
    error = Color(0xFFDC2626), onError = Color.White
)

// ---- Seller (violet) light ----
private val SellerLight = CustomerLight.copy(
    primary = SellerPrimary, onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF24105E)
)

// ---- Dark schemes ----
private val CustomerDark = darkColorScheme(
    primary = Color(0xFF93B4FF), onPrimary = Color(0xFF002B6B),
    primaryContainer = Color(0xFF1E3A8A), onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFFCD34D), onSecondary = Color(0xFF3A2A00),
    tertiary = Color(0xFF6EE7A0),
    background = Color(0xFF0B1220), onBackground = Color(0xFFE7EDF6),
    surface = Color(0xFF111B30), onSurface = Color(0xFFE7EDF6),
    surfaceVariant = Color(0xFF1B2742), onSurfaceVariant = Color(0xFFB7C2D6),
    outline = Color(0xFF36425C)
)
private val SellerDark = CustomerDark.copy(
    primary = Color(0xFFC9B0FF), onPrimary = Color(0xFF2A1065),
    primaryContainer = Color(0xFF4C2E91), onPrimaryContainer = Color(0xFFEADDFF)
)

// ---- Typography ----
private val AppType = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 25.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.3.sp)
)

// ---- Shapes ----
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LocalKartTheme(
    seller: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = when {
        seller && darkTheme -> SellerDark
        seller -> SellerLight
        darkTheme -> CustomerDark
        else -> CustomerLight
    }
    MaterialTheme(colorScheme = colors, typography = AppType, shapes = AppShapes, content = content)
}
