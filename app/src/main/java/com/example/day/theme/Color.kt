package com.example.day.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Light Mode Tokens
val NeuLightBackground = Color(0xFFEBECF0)
val NeuLightCardSurface = Color(0xFFEBECF0)
val NeuLightForeground = Color(0xFF4A5568)
val NeuLightMuted = Color(0xFFA0AEC0)
val NeuLightSecondarySurface = Color(0xFFE2E8F0)
val NeuAccentOrange = Color(0xFFFF8800)

// Dark Mode Tokens (Neutral Charcoal #363636)
val NeuDarkBackground = Color(0xFF363636)
val NeuDarkCardSurface = Color(0xFF363636)
val NeuDarkForeground = Color(0xFFF0F0F0)
val NeuDarkMuted = Color(0xFFA8A8A8)
val NeuDarkSecondarySurface = Color(0xFF2B2B2B)
val NeuDarkTeal = Color(0xFF2DD4BF)

// Overdue Alert Colors
val OverdueRed = Color(0xFFE53935)
val OverdueRedBg = Color(0xFFFFEBEE)

// Settings 8-Color Palette Row (Curated primary options)
val PRESET_COLORS = listOf(
    "#FF8800", // Orange
    "#3B82F6", // Blue
    "#2DD4BF", // Teal
    "#22C55E", // Green
    "#8B5CF6", // Purple
    "#F43F5E", // Rose
    "#F59E0B", // Amber
    "#6B7280"  // Gray
)

fun isValidHexColor(hex: String): Boolean {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return false
    return clean.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
}

fun normalizeHex(hex: String): String {
    val clean = hex.trim().removePrefix("#")
    return "#${clean.uppercase()}"
}

fun parseHexColor(hex: String, defaultColor: Color = NeuAccentOrange): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> (0xFF000000 or clean.toLong(16)).toInt()
            8 -> clean.toLong(16).toInt()
            else -> return defaultColor
        }
        Color(colorInt)
    } catch (e: Exception) {
        defaultColor
    }
}

fun colorSchemeFromHex(hexString: String, isDark: Boolean): ColorScheme {
    val accentColor = parseHexColor(hexString, NeuAccentOrange)
    return if (isDark) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.25f),
            onPrimaryContainer = Color.White,
            secondary = NeuDarkTeal,
            onSecondary = Color.Black,
            background = NeuDarkBackground,
            onBackground = NeuDarkForeground,
            surface = NeuDarkCardSurface,
            onSurface = NeuDarkForeground,
            surfaceVariant = Color(0xFF2B2B2B),
            onSurfaceVariant = NeuDarkMuted,
            outline = Color(0xFF4A4A4A),
            outlineVariant = Color(0xFF2B2B2B)
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.15f),
            onPrimaryContainer = accentColor,
            secondary = NeuLightSecondarySurface,
            onSecondary = NeuLightForeground,
            background = NeuLightBackground,
            onBackground = NeuLightForeground,
            surface = NeuLightCardSurface,
            onSurface = NeuLightForeground,
            surfaceVariant = NeuLightSecondarySurface,
            onSurfaceVariant = NeuLightMuted,
            outline = Color(0xFFD0D5DD),
            outlineVariant = Color(0xFFE2E8F0)
        )
    }
}
