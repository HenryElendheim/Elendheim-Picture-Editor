package com.elendheim.pictureeditor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The Elendheim palette: dark grey base, soft red accent. Dark first, always.
private val Background = Color(0xFF12100F)
private val Surface = Color(0xFF1C1A19)
private val SurfaceHigh = Color(0xFF262322)
private val SoftRed = Color(0xFFD65A5A)
private val OnDark = Color(0xFFEDE8E6)
private val OnDim = Color(0xFFB4ADAA)

// High contrast pushes text brighter and the accent stronger for low vision.
private val OnDarkHC = Color(0xFFFFFFFF)
private val SoftRedHC = Color(0xFFFF7A7A)
private val BackgroundHC = Color(0xFF000000)

private fun elendheimScheme(highContrast: Boolean) = darkColorScheme(
    primary = if (highContrast) SoftRedHC else SoftRed,
    onPrimary = Color(0xFF1A0E0E),
    secondary = if (highContrast) SoftRedHC else SoftRed,
    background = if (highContrast) BackgroundHC else Background,
    onBackground = if (highContrast) OnDarkHC else OnDark,
    surface = if (highContrast) Color(0xFF141312) else Surface,
    onSurface = if (highContrast) OnDarkHC else OnDark,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = if (highContrast) OnDarkHC else OnDim,
    outline = if (highContrast) Color(0xFF8A8582) else Color(0xFF4A4644)
)

/**
 * Wraps the app in the Elendheim look. High contrast is a live toggle from
 * settings, so the whole UI re-colours the moment it changes.
 */
@Composable
fun ElendheimTheme(
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = elendheimScheme(highContrast),
        typography = Typography(),
        content = content
    )
}
