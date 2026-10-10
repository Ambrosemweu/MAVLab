package com.ascend.mavlab.feature.controller

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sqrt

@Composable
fun TiltVisualizer(
    rollNormalized: Float,
    pitchNormalized: Float,
    deadzoneRadius: Float,
    modifier: Modifier = Modifier,
) {
    val guideColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activeColor = MaterialTheme.colorScheme.secondary
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
            color = guideColor.copy(alpha = 0.45f),
            center = center,
            radius = radius,
            style = Stroke(width = 2f),
        )
        drawCircle(
            color = guideColor.copy(alpha = 0.25f),
            center = center,
            radius = radius * deadzoneRadius,
            style = Stroke(width = 2f),
        )
        drawLine(
            color = guideColor.copy(alpha = 0.3f),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 2f,
        )
        drawLine(
            color = guideColor.copy(alpha = 0.3f),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 2f,
        )
        rotate(rollNormalized * 30f, pivot = center) {
            drawRoundRect(
                color = activeColor.copy(alpha = 0.09f),
                topLeft = Offset(center.x - radius * 0.28f, center.y - radius * 0.52f),
                size = Size(radius * 0.56f, radius * 1.04f),
                cornerRadius = CornerRadius(12f),
            )
            drawRoundRect(
                color = if (active) activeColor else guideColor,
                topLeft = Offset(center.x - radius * 0.28f, center.y - radius * 0.52f),
                size = Size(radius * 0.56f, radius * 1.04f),
                cornerRadius = CornerRadius(12f),
                style = Stroke(width = 3f),
            )
        }
        drawCircle(
            color = if (active) activeColor else guideColor,
            center = Offset(dotX, dotY),
            radius = 14f,
        )
    }
}
