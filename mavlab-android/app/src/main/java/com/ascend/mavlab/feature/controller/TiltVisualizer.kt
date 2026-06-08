package com.ascend.mavlab.feature.controller

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sqrt

@Composable
fun TiltVisualizer(
    rollNormalized: Float,
    pitchNormalized: Float,
    deadzoneRadius: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) * 0.44f
        val dotX = center.x + rollNormalized.coerceIn(-1f, 1f) * radius
        val dotY = center.y - pitchNormalized.coerceIn(-1f, 1f) * radius
        val distance = sqrt(rollNormalized * rollNormalized + pitchNormalized * pitchNormalized)
        val active = distance > deadzoneRadius

        drawCircle(
            color = Color.White.copy(alpha = 0.28f),
            center = center,
            radius = radius,
            style = Stroke(width = 2f),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.16f),
            center = center,
            radius = radius * deadzoneRadius,
            style = Stroke(width = 2f),
        )
        drawLine(
            color = Color.White.copy(alpha = 0.18f),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.18f),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 2f,
        )
        drawCircle(
            color = if (active) Color(0xFF66D9C7) else Color(0xFF8A9299),
            center = Offset(dotX, dotY),
            radius = 14f,
        )
    }
}
