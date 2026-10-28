package com.example.day.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs

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

/**
 * Calculates WCAG relative contrast ratio between two colors (range: 1.0 to 21.0).
 */
fun calculateContrast(c1: Color, c2: Color): Float {
    val l1 = c1.luminance()
    val l2 = c2.luminance()
    val bright = maxOf(l1, l2)
    val dark = minOf(l1, l2)
    return (bright + 0.05f) / (dark + 0.05f)
}

/**
 * Converts sRGB floats (0..1) to HSV (h: 0..360, s: 0..1, v: 0..1) in pure Kotlin.
 */
fun rgbToHsv(r: Float, g: Float, b: Float): FloatArray {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    var h = 0f
    val s = if (max == 0f) 0f else delta / max
    val v = max

    if (delta != 0f) {
        h = when (max) {
            r -> ((g - b) / delta) % 6f
            g -> ((b - r) / delta) + 2f
            else -> ((r - g) / delta) + 4f
        } * 60f
        if (h < 0f) h += 360f
    }
    return floatArrayOf(h, s, v)
}

/**
 * Converts HSV (h: 0..360, s: 0..1, v: 0..1) back to sRGB Color in pure Kotlin.
 */
fun hsvToRgb(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1f - abs(((h / 60f) % 2f) - 1f))
    val m = v - c

    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = 1f
    )
}

/**
 * Optimizes the accent color for dark mode (#363636 surface).
 * - If user chose #363636, black, or dark grey, defaults to vibrant teal (#2DD4BF) so buttons/icons are crisp.
 * - If user chose a dark hue (e.g. dark forest green #436B52), preserves hue while brightening value to ~0.92
 *   for rich, high-contrast dark-mode visibility (WCAG AAA compliant).
 */
fun adaptAccentForDarkMode(color: Color, background: Color = NeuDarkBackground): Color {
    val r = color.red
    val g = color.green
    val b = color.blue
    val hsv = rgbToHsv(r, g, b)
    val h = hsv[0]
    val s = hsv[1]
    val v = hsv[2]

    // If near-neutral (grey / black / charcoal like #363636)
    if (s < 0.12f) {
        return if (v < 0.65f) {
            NeuDarkTeal
        } else {
            color
        }
    }

    // If contrast is already strong on dark background (>= 3.8:1)
    val contrast = calculateContrast(color, background)
    if (contrast >= 3.8f && color.luminance() > background.luminance()) {
        return color
    }

    // Brighten value and soften saturation for dark mode pastel visibility
    val targetS = (s * 0.75f).coerceIn(0.25f, 0.60f)
    val targetV = 0.92f
    return hsvToRgb(h, targetS, targetV)
}

/**
 * Optimizes the accent color for light mode (#EBECF0 surface).
 */
fun adaptAccentForLightMode(color: Color, background: Color = NeuLightBackground): Color {
    val contrast = calculateContrast(color, background)
    if (contrast >= 2.8f && color.luminance() < background.luminance()) {
        return color
    }
    val r = color.red
    val g = color.green
    val b = color.blue
    val hsv = rgbToHsv(r, g, b)
    val h = hsv[0]
    val s = hsv[1]
    if (s < 0.12f) {
        return Color(0xFF2D3748)
    }
    return hsvToRgb(h, (s * 1.1f).coerceIn(0.6f, 0.95f), 0.55f)
}

fun colorSchemeFromHex(hexString: String, isDark: Boolean): ColorScheme {
    val parsedColor = parseHexColor(hexString, NeuAccentOrange)
    val accentColor = if (isDark) adaptAccentForDarkMode(parsedColor) else adaptAccentForLightMode(parsedColor)

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
            onSurfaceVariant = Color(0xFFCCCCCC), // Brightened for clear icon & label readability
            outline = Color(0xFF555555),
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
