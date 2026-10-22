package com.example.day.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun DayTheme(
    primaryHex: String = "#FF8800",
    isDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = colorSchemeFromHex(primaryHex, isDark)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DayTypography,
        content = content
    )
}
