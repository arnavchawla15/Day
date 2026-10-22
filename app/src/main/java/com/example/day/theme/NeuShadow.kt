package com.example.day.theme

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

data class NeuShadowConfig(
    val color: Color,
    val blurRadius: Dp,
    val offset: DpOffset
)

object NeuShadow {
    // Light Mode Shadows: Crisp white highlight + rich soft shadow
    val LightHighlight = Color(0xFFFFFFFF).copy(alpha = 0.95f)
    val LightShadow = Color(0xFF9FAEC0).copy(alpha = 0.75f)

    // Dark Mode Shadows: Crisp ambient highlight + deep shadow for neutral #363636 surface
    val DarkHighlight = Color(0xFF4E4E4E).copy(alpha = 0.85f)
    val DarkShadow = Color(0xFF1E1E1E).copy(alpha = 0.95f)

    // Extruded (default resting state for cards)
    fun extruded(isDark: Boolean = false): List<NeuShadowConfig> {
        val highlight = if (isDark) DarkHighlight else LightHighlight
        val shadow = if (isDark) DarkShadow else LightShadow
        return listOf(
            NeuShadowConfig(color = highlight, blurRadius = 10.dp, offset = DpOffset((-4).dp, (-4).dp)),
            NeuShadowConfig(color = shadow, blurRadius = 10.dp, offset = DpOffset(4.dp, 4.dp))
        )
    }

    // Inset (pressed/active state)
    fun inset(isDark: Boolean = false): List<NeuShadowConfig> {
        val highlight = if (isDark) DarkHighlight else LightHighlight
        val shadow = if (isDark) DarkShadow else LightShadow
        return listOf(
            NeuShadowConfig(color = shadow, blurRadius = 8.dp, offset = DpOffset((-3).dp, (-3).dp)),
            NeuShadowConfig(color = highlight, blurRadius = 8.dp, offset = DpOffset(3.dp, 3.dp))
        )
    }
}

/**
 * Modifier to render dual neumorphic extruded shadows (top-left highlight, bottom-right shadow).
 */
fun Modifier.neuExtruded(
    cornerRadius: Dp = 16.dp,
    isDark: Boolean = false,
    highlightColor: Color = if (isDark) NeuShadow.DarkHighlight else NeuShadow.LightHighlight,
    shadowColor: Color = if (isDark) NeuShadow.DarkShadow else NeuShadow.LightShadow,
    blurRadius: Dp = 10.dp,
    offset: Dp = 4.dp
): Modifier = this.drawBehind {
    val cornerRadiusPx = cornerRadius.toPx()
    val blurPx = blurRadius.toPx().coerceAtLeast(1f)
    val offsetPx = offset.toPx()

    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas

        // Top-Left Light Highlight Shadow
        val highlightPaint = android.graphics.Paint().apply {
            color = highlightColor.toArgb()
            isAntiAlias = true
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
        nativeCanvas.drawRoundRect(
            -offsetPx,
            -offsetPx,
            size.width - offsetPx,
            size.height - offsetPx,
            cornerRadiusPx,
            cornerRadiusPx,
            highlightPaint
        )

        // Bottom-Right Dark Shadow
        val shadowPaint = android.graphics.Paint().apply {
            color = shadowColor.toArgb()
            isAntiAlias = true
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
        nativeCanvas.drawRoundRect(
            offsetPx,
            offsetPx,
            size.width + offsetPx,
            size.height + offsetPx,
            cornerRadiusPx,
            cornerRadiusPx,
            shadowPaint
        )
    }
}

/**
 * Modifier to render dual neumorphic inset inner shadows (pressed or input state).
 */
fun Modifier.neuInset(
    cornerRadius: Dp = 16.dp,
    isDark: Boolean = false,
    shadowColor: Color = if (isDark) NeuShadow.DarkShadow else NeuShadow.LightShadow,
    highlightColor: Color = if (isDark) NeuShadow.DarkHighlight else NeuShadow.LightHighlight,
    blurRadius: Dp = 8.dp,
    offset: Dp = 3.dp
): Modifier = this.drawWithContent {
    drawContent()

    val cornerRadiusPx = cornerRadius.toPx()
    val blurPx = blurRadius.toPx().coerceAtLeast(1f)
    val offsetPx = offset.toPx()

    clipRect {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            // Clip inner bounds to rounded rect
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(Offset.Zero, size),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )
                )
            }

            canvas.save()
            canvas.clipPath(path)

            // Top-left dark inner shadow
            val darkPaint = android.graphics.Paint().apply {
                color = shadowColor.toArgb()
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = offsetPx * 2
                isAntiAlias = true
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
            nativeCanvas.drawRoundRect(
                -offsetPx,
                -offsetPx,
                size.width + offsetPx,
                size.height + offsetPx,
                cornerRadiusPx,
                cornerRadiusPx,
                darkPaint
            )

            // Bottom-right light inner highlight
            val lightPaint = android.graphics.Paint().apply {
                color = highlightColor.toArgb()
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = offsetPx * 2
                isAntiAlias = true
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
            nativeCanvas.drawRoundRect(
                offsetPx,
                offsetPx,
                size.width + offsetPx * 2,
                size.height + offsetPx * 2,
                cornerRadiusPx,
                cornerRadiusPx,
                lightPaint
            )

            canvas.restore()
        }
    }
}
