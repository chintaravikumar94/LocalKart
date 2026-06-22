package com.localkart.common.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// LocalKart brand palette
val Primary = Color(0xFF2563EB)      // blue
val PrimaryDark = Color(0xFF1E40AF)
val Accent = Color(0xFFF59E0B)       // amber
val SellerPrimary = Color(0xFF7C3AED) // violet for seller app

private val LightColors = lightColorScheme(
    primary = Primary, secondary = Accent, primaryContainer = PrimaryDark
)
private val DarkColors = darkColorScheme(
    primary = Primary, secondary = Accent
)

@Composable
fun LocalKartTheme(
    seller: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val base = if (darkTheme) DarkColors else LightColors
    val colors = if (seller) base.copy(primary = SellerPrimary) else base
    MaterialTheme(colorScheme = colors, content = content)
}
