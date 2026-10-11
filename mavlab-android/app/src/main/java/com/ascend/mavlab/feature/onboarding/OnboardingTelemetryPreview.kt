package com.ascend.mavlab.feature.onboarding

import androidx.compose.ui.graphics.Color
import com.ascend.mavlab.core.ui.components.ChartAxis
import com.ascend.mavlab.core.ui.components.ChartAxisSpec
import com.ascend.mavlab.core.ui.components.ChartLineStyle
import com.ascend.mavlab.core.ui.components.ChartSeries
import com.ascend.mavlab.core.ui.components.RollingChartSampleCapacity
import kotlin.math.PI
import kotlin.math.sin

/** Static illustration only; never subscribes to or modifies simulation state. */
internal object OnboardingTelemetryPreview {
    // Match Cockpit's telemetry palette, axis assignment and trace styles.
    private val altitudeColor = Color(0xFFF5B942)
    private val rollColor = Color(0xFF2DD4BF)
    private val pitchColor = Color(0xFF60A5FA)
    private val yawColor = Color(0xFFC4B5FD)

    val leftAxis = ChartAxisSpec("Altitude", "m", false, altitudeColor)
    val rightAxis = ChartAxisSpec("Attitude", "°", true, Color(0xFFE7EEF9))
    const val sourceLabel = "Example recording · not live telemetry"

    // The same 41 samples/10 seconds as the live chart. Smooth synthetic signals
    // avoid the old sparse, hand-drawn corners without filtering real telemetry.
    private fun samples(signal: (Float) -> Float) = List(RollingChartSampleCapacity) { index ->
        signal(index.toFloat() / (RollingChartSampleCapacity - 1))
    }

    val series = listOf(
        ChartSeries("Altitude AGL", samples { t ->
            val ascent = (t / .6f).coerceIn(0f, 1f)
            10f * ascent * ascent * (3f - 2f * ascent)
        }, altitudeColor, ChartAxis.LEFT),
        ChartSeries("Roll", samples { t ->
            5f * sin(2f * PI.toFloat() * t) * (1f - .7f * t)
        }, rollColor, ChartAxis.RIGHT),
        ChartSeries("Pitch", samples { t ->
            3f * sin(2f * PI.toFloat() * (t - .125f)) * (1f - .7f * t)
        }, pitchColor, ChartAxis.RIGHT, ChartLineStyle.DASHED),
        ChartSeries("Yaw", samples { t ->
            4f * sin(PI.toFloat() * t)
        }, yawColor, ChartAxis.RIGHT, ChartLineStyle.DOTTED),
    )
}
