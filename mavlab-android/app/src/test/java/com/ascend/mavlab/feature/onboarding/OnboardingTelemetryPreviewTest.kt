package com.ascend.mavlab.feature.onboarding

import androidx.compose.ui.graphics.Color
import com.ascend.mavlab.core.ui.components.ChartAxis
import com.ascend.mavlab.core.ui.components.ChartLineStyle
import com.ascend.mavlab.core.ui.components.RollingChartSampleCapacity
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingTelemetryPreviewTest {
    @Test fun tracesMatchCockpitUnitsColorsAndLineStyles() {
        val series = OnboardingTelemetryPreview.series
        assertEquals(listOf("Altitude AGL", "Roll", "Pitch", "Yaw"), series.map { it.label })
        assertEquals(listOf(ChartAxis.LEFT, ChartAxis.RIGHT, ChartAxis.RIGHT, ChartAxis.RIGHT), series.map { it.axis })
        assertEquals(listOf(ChartLineStyle.SOLID, ChartLineStyle.SOLID, ChartLineStyle.DASHED, ChartLineStyle.DOTTED), series.map { it.lineStyle })
        assertEquals(listOf(Color(0xFFF5B942), Color(0xFF2DD4BF), Color(0xFF60A5FA), Color(0xFFC4B5FD)), series.map { it.color })
        assertEquals("m", OnboardingTelemetryPreview.leftAxis.unit)
        assertEquals("°", OnboardingTelemetryPreview.rightAxis.unit)
        assertTrue(OnboardingTelemetryPreview.rightAxis.symmetricAroundZero)
        assertTrue(OnboardingTelemetryPreview.sourceLabel.contains("not live telemetry"))
    }

    @Test fun smoothSamplesFillTheActualTenSecondChartWithoutJumps() {
        OnboardingTelemetryPreview.series.forEach { series ->
            assertEquals(RollingChartSampleCapacity, series.values.size)
            assertTrue(series.values.all { it.isFinite() })
            assertTrue(series.values.zipWithNext().all { (a, b) -> abs(b - a) < 1f }, series.label)
        }
        val altitude = OnboardingTelemetryPreview.series.first().values
        assertEquals(0f, altitude.first())
        assertEquals(10f, altitude.last())
        assertTrue(altitude.zipWithNext().all { (a, b) -> b >= a })
        assertTrue(altitude.takeLast(10).all { it == 10f })
    }
}
