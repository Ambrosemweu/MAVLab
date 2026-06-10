package com.ascend.mavlab.feature.drone3d

import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun AltitudeInstrument(
    altitudeMeters: Float,
    verticalSpeedMetersPerSecond: Float,
    yawRadians: Float,
    rollRadians: Float,
    pitchRadians: Float,
    armed: Boolean,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val diameter = if (isLandscape) 184.dp else 154.dp
    val lowAltitudeWarning = armed &&
        altitudeMeters < LowAltitudeWarningMeters &&
        verticalSpeedMetersPerSecond < LowAltitudeDescentMetersPerSecond
    val accent = if (lowAltitudeWarning) InstrumentWarning else InstrumentOrange

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAltitudeInstrumentFace(
                altitudeMeters = altitudeMeters,
                yawRadians = yawRadians,
                rollRadians = rollRadians,
                pitchRadians = pitchRadians,
                accent = accent,
                lowAltitudeWarning = lowAltitudeWarning,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isLandscape) 18.dp else 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((-1).dp),
        ) {
            Text(
                text = "ALT AGL",
                color = InstrumentSecondary.copy(alpha = 0.88f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (isLandscape) 9.sp else 8.sp,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "%.1f m".format(altitudeMeters),
                color = if (lowAltitudeWarning) InstrumentWarning else InstrumentText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = if (isLandscape) 19.sp else 16.sp,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "VS %+.2f m/s".format(verticalSpeedMetersPerSecond),
                color = InstrumentSecondary.copy(alpha = 0.9f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (isLandscape) 9.sp else 8.sp,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
            )
            if (lowAltitudeWarning) {
                Text(
                    text = "LOW ALT",
                    color = InstrumentWarning,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun DrawScope.drawAltitudeInstrumentFace(
    altitudeMeters: Float,
    yawRadians: Float,
    rollRadians: Float,
    pitchRadians: Float,
    accent: Color,
    lowAltitudeWarning: Boolean,
) {
    val side = min(size.width, size.height)
    val center = Offset(size.width / 2f, size.height / 2f)
    val outerRadius = side * 0.48f
    val bezelWidth = side * 0.055f
    val horizonRadius = side * 0.35f

    drawCircle(
        color = InstrumentGlow.copy(alpha = if (lowAltitudeWarning) 0.24f else 0.14f),
        radius = outerRadius,
        center = center,
        style = Stroke(width = side * 0.026f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                InstrumentGraphite.copy(alpha = 0.62f),
                InstrumentBezel.copy(alpha = 0.9f),
                InstrumentGraphite.copy(alpha = 0.92f),
            ),
            center = center,
            radius = outerRadius,
        ),
        radius = outerRadius,
        center = center,
    )
    drawCircle(
        color = InstrumentDarkGrey.copy(alpha = 0.88f),
        radius = outerRadius - bezelWidth / 2f,
        center = center,
        style = Stroke(width = bezelWidth),
    )
    val yawDegrees = (yawRadians * RadToDeg).normalizeDegrees()
    drawTickRing(center, outerRadius, side, accent, yawDegrees)
    drawHorizonDisc(center, horizonRadius, side, rollRadians, pitchRadians)
    drawAltitudeScale(center, horizonRadius, side, altitudeMeters)
    drawCircle(
        color = InstrumentGlow.copy(alpha = 0.18f),
        radius = horizonRadius,
        center = center,
        style = Stroke(width = side * 0.008f),
    )
    drawFixedDroneMarker(center, horizonRadius, side, accent)
    drawGlassOverlay(center, horizonRadius)

    drawCircle(
        color = if (lowAltitudeWarning) InstrumentWarning.copy(alpha = 0.86f) else accent.copy(alpha = 0.62f),
        radius = outerRadius - side * 0.012f,
        center = center,
        style = Stroke(width = side * 0.012f),
    )
    drawYawCue(center, outerRadius, side, yawDegrees, accent)
}

private fun DrawScope.drawTickRing(
    center: Offset,
    outerRadius: Float,
    side: Float,
    accent: Color,
    yawDegrees: Float,
) {
    for (degree in 0 until 360 step 5) {
        val major = degree % 30 == 0
        val medium = degree % 10 == 0
        val angleRadians = (degree - yawDegrees - 90f) * PI.toFloat() / 180f
        val tickLength = when {
            major -> side * 0.046f
            medium -> side * 0.033f
            else -> side * 0.019f
        }
        val outer = outerRadius - side * 0.024f
        val inner = outer - tickLength
        val color = if (major) InstrumentText.copy(alpha = 0.86f) else InstrumentMuted.copy(alpha = 0.72f)
        drawLine(
            color = color,
            start = polarPoint(center, inner, angleRadians),
            end = polarPoint(center, outer, angleRadians),
            strokeWidth = if (major) side * 0.009f else side * 0.005f,
            cap = StrokeCap.Round,
        )
    }

    listOf(0, 90, 180, 270).forEach { degree ->
        drawCardinalMarker(center, outerRadius - side * 0.062f, side, degree - yawDegrees, accent)
    }
}

private fun DrawScope.drawYawCue(
    center: Offset,
    outerRadius: Float,
    side: Float,
    yawDegrees: Float,
    accent: Color,
) {
    val headingText = "%03d".format(yawDegrees.roundToInt() % 360)
    drawLine(
        color = accent.copy(alpha = 0.86f),
        start = Offset(center.x, center.y - outerRadius + side * 0.056f),
        end = Offset(center.x, center.y - outerRadius + side * 0.116f),
        strokeWidth = side * 0.01f,
        cap = StrokeCap.Round,
    )
    drawInstrumentText(
        text = headingText,
        x = center.x,
        y = center.y - outerRadius + side * 0.19f,
        textSize = side * 0.052f,
        color = InstrumentText.copy(alpha = 0.78f),
        align = Paint.Align.CENTER,
    )
}

private fun DrawScope.drawHorizonDisc(
    center: Offset,
    radius: Float,
    side: Float,
    rollRadians: Float,
    pitchRadians: Float,
) {
    val horizonPath = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }
    val rollDegrees = (-rollRadians * RadToDeg).coerceIn(-80f, 80f)
    val pitchOffset = (pitchRadians * RadToDeg * side * 0.009f)
        .coerceIn(-radius * 0.5f, radius * 0.5f)

    clipPath(horizonPath) {
        drawCircle(
            color = InstrumentGraphite.copy(alpha = 0.86f),
            radius = radius,
            center = center,
        )
        rotate(degrees = rollDegrees, pivot = center) {
            val wide = radius * 2.6f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        InstrumentSkyHighlight.copy(alpha = 0.86f),
                        InstrumentSky.copy(alpha = 0.82f),
                    ),
                    startY = center.y - wide + pitchOffset,
                    endY = center.y + pitchOffset,
                ),
                topLeft = Offset(center.x - wide, center.y - wide + pitchOffset),
                size = Size(wide * 2f, wide),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        InstrumentGroundWarm.copy(alpha = 0.78f),
                        InstrumentGround.copy(alpha = 0.88f),
                    ),
                    startY = center.y + pitchOffset,
                    endY = center.y + wide + pitchOffset,
                ),
                topLeft = Offset(center.x - wide, center.y + pitchOffset),
                size = Size(wide * 2f, wide),
            )
            drawLine(
                color = InstrumentText.copy(alpha = 0.88f),
                start = Offset(center.x - radius * 1.1f, center.y + pitchOffset),
                end = Offset(center.x + radius * 1.1f, center.y + pitchOffset),
                strokeWidth = side * 0.009f,
                cap = StrokeCap.Round,
            )
            drawPitchLadder(center, radius, side, pitchOffset)
        }
    }
}

private fun DrawScope.drawPitchLadder(
    center: Offset,
    radius: Float,
    side: Float,
    pitchOffset: Float,
) {
    listOf(-20, -10, 10, 20).forEach { pitch ->
        val y = center.y + pitchOffset - pitch * side * 0.006f
        if (y > center.y - radius * 0.82f && y < center.y + radius * 0.82f) {
            val halfWidth = if (pitch % 20 == 0) radius * 0.34f else radius * 0.24f
            drawLine(
                color = InstrumentText.copy(alpha = 0.48f),
                start = Offset(center.x - halfWidth, y),
                end = Offset(center.x + halfWidth, y),
                strokeWidth = side * 0.005f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawAltitudeScale(
    center: Offset,
    radius: Float,
    side: Float,
    altitudeMeters: Float,
) {
    val pixelsPerMeter = side * 0.015f
    val baseAltitude = (floor(altitudeMeters / 5f) * 5f).roundToInt()
    val scaleX = center.x + radius * 0.54f
    val labelX = scaleX - side * 0.07f
    val minY = center.y - radius * 0.74f
    val maxY = center.y + radius * 0.36f

    for (meters in baseAltitude - 25..baseAltitude + 25 step 5) {
        if (meters < 0) continue
        val y = center.y - (meters - altitudeMeters) * pixelsPerMeter
        if (y < minY || y > maxY) continue

        val major = meters % 10 == 0
        val tickLength = if (major) side * 0.054f else side * 0.035f
        drawLine(
            color = if (major) InstrumentText.copy(alpha = 0.78f) else InstrumentSecondary.copy(alpha = 0.54f),
            start = Offset(scaleX, y),
            end = Offset(scaleX + tickLength, y),
            strokeWidth = if (major) side * 0.007f else side * 0.004f,
            cap = StrokeCap.Round,
        )
        if (major) {
            drawInstrumentText(
                text = meters.toString(),
                x = labelX,
                y = y + side * 0.011f,
                textSize = side * 0.055f,
                color = InstrumentText.copy(alpha = 0.82f),
                align = Paint.Align.RIGHT,
            )
        }
    }

    drawLine(
        color = InstrumentOrange.copy(alpha = 0.62f),
        start = Offset(scaleX - side * 0.02f, center.y),
        end = Offset(scaleX + side * 0.076f, center.y),
        strokeWidth = side * 0.01f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawFixedDroneMarker(
    center: Offset,
    horizonRadius: Float,
    side: Float,
    accent: Color,
) {
    val shadowOffset = side * 0.008f
    val wingGap = horizonRadius * 0.13f
    val wingOuter = horizonRadius * 0.48f
    val markerWidth = side * 0.017f
    val y = center.y

    drawLine(
        color = Color.Black.copy(alpha = 0.58f),
        start = Offset(center.x - wingOuter, y + shadowOffset),
        end = Offset(center.x - wingGap, y + shadowOffset),
        strokeWidth = markerWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color.Black.copy(alpha = 0.58f),
        start = Offset(center.x + wingGap, y + shadowOffset),
        end = Offset(center.x + wingOuter, y + shadowOffset),
        strokeWidth = markerWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = accent,
        start = Offset(center.x - wingOuter, y),
        end = Offset(center.x - wingGap, y),
        strokeWidth = markerWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = accent,
        start = Offset(center.x + wingGap, y),
        end = Offset(center.x + wingOuter, y),
        strokeWidth = markerWidth,
        cap = StrokeCap.Round,
    )

    val diamondSize = side * 0.032f
    val diamond = Path().apply {
        moveTo(center.x, center.y - diamondSize)
        lineTo(center.x + diamondSize, center.y)
        lineTo(center.x, center.y + diamondSize)
        lineTo(center.x - diamondSize, center.y)
        close()
    }
    drawPath(path = diamond, color = Color.Black.copy(alpha = 0.56f))
    drawPath(path = diamond, color = accent)

    drawLine(
        color = accent,
        start = Offset(center.x, center.y + diamondSize * 1.2f),
        end = Offset(center.x, center.y + diamondSize * 2.35f),
        strokeWidth = side * 0.008f,
        cap = StrokeCap.Round,
    )

    val triangleTop = center.y - horizonRadius * 0.47f
    val triangleSize = side * 0.033f
    val triangle = Path().apply {
        moveTo(center.x, triangleTop)
        lineTo(center.x - triangleSize, triangleTop + triangleSize * 1.42f)
        lineTo(center.x + triangleSize, triangleTop + triangleSize * 1.42f)
        close()
    }
    drawPath(path = triangle, color = Color.Black.copy(alpha = 0.5f))
    drawPath(path = triangle, color = accent.copy(alpha = 0.94f))
}

private fun DrawScope.drawGlassOverlay(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.13f),
                Color.Transparent,
            ),
            center = Offset(center.x - radius * 0.32f, center.y - radius * 0.4f),
            radius = radius * 0.95f,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawCardinalMarker(
    center: Offset,
    radius: Float,
    side: Float,
    degree: Float,
    color: Color,
) {
    val angleRadians = (degree - 90f) * PI.toFloat() / 180f
    val point = polarPoint(center, radius, angleRadians)
    val base = polarPoint(center, radius - side * 0.031f, angleRadians)
    val normal = Offset(-sin(angleRadians), cos(angleRadians))
    val halfBase = side * 0.014f
    val path = Path().apply {
        moveTo(point.x, point.y)
        lineTo(base.x + normal.x * halfBase, base.y + normal.y * halfBase)
        lineTo(base.x - normal.x * halfBase, base.y - normal.y * halfBase)
        close()
    }
    drawPath(path = path, color = color.copy(alpha = 0.9f))
}

private fun DrawScope.drawInstrumentText(
    text: String,
    x: Float,
    y: Float,
    textSize: Float,
    color: Color,
    align: Paint.Align,
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        this.textSize = textSize
        this.textAlign = align
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

private fun polarPoint(center: Offset, radius: Float, angleRadians: Float): Offset {
    return Offset(
        x = center.x + cos(angleRadians) * radius,
        y = center.y + sin(angleRadians) * radius,
    )
}

private fun Float.normalizeDegrees(): Float {
    val normalized = this % 360f
    return if (normalized < 0f) normalized + 360f else normalized
}

private const val RadToDeg = 180f / PI.toFloat()
private const val LowAltitudeWarningMeters = 1.5f
private const val LowAltitudeDescentMetersPerSecond = -0.2f

private val InstrumentGraphite = Color(0xFF101418)
private val InstrumentDarkGrey = Color(0xFF252B30)
private val InstrumentBezel = Color(0xFF252B30)
private val InstrumentMuted = Color(0xFF5E6870)
private val InstrumentGlow = Color(0xFFE8FFF7)
private val InstrumentSky = Color(0xFF6DB7D8)
private val InstrumentSkyHighlight = Color(0xFFA7DCF1)
private val InstrumentGround = Color(0xFF4B2716)
private val InstrumentGroundWarm = Color(0xFF7B4320)
private val InstrumentText = Color(0xFFF4FFF9)
private val InstrumentSecondary = Color(0xFFC8E9DF)
private val InstrumentOrange = Color(0xFFFF8A1C)
private val InstrumentWarning = Color(0xFFFF4D3D)
