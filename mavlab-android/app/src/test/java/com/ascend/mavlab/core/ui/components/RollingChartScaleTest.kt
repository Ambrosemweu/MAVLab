package com.ascend.mavlab.core.ui.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RollingChartScaleTest {
    @Test
    fun combinedChartKeepsAltitudeMetresIndependentOfAttitudeDegrees() {
        val series = listOf(
            ChartSeries("Altitude AGL", listOf(0f, 10_000f), Color.Yellow, ChartAxis.LEFT),
            ChartSeries("Roll", listOf(-25f, 12f), Color.Cyan, ChartAxis.RIGHT),
            ChartSeries("Pitch", listOf(-10f, 15f), Color.Blue, ChartAxis.RIGHT),
            ChartSeries("Yaw", listOf(0f, 90f), Color.Magenta, ChartAxis.RIGHT),
        )

        val altitude = rollingChartScaleForAxis(series, ChartAxis.LEFT, false)
        val attitude = rollingChartScaleForAxis(series, ChartAxis.RIGHT, true)

        assertEquals(RollingChartScale(0f, 10_000f), altitude)
        assertEquals(RollingChartScale(-90f, 90f), attitude)
        assertEquals(1f, altitude.heightFraction(0f))
        assertEquals(0.5f, attitude.heightFraction(0f))
        assertEquals(0f, altitude.heightFraction(10_000f))
        assertEquals(0f, attitude.heightFraction(90f))
    }

    @Test
    fun attitudeRetainsSignsAndPlacesLevelAtTheCentre() {
        val scale = rollingChartScale(listOf(-25f, 12f), symmetricAroundZero = true)

        assertEquals(0.5f, scale.heightFraction(0f))
        assertEquals(1f, scale.heightFraction(-25f))
        assertEquals(0f, scale.heightFraction(25f))
        assertTrue(scale.heightFraction(12f) < 0.5f)
    }

    @Test
    fun altitudeUsesGroundAsTheBottomOfItsOwnScale() {
        val scale = rollingChartScale(listOf(0f, 10.2f), symmetricAroundZero = false)

        assertEquals(0f, scale.minimum)
        assertTrue(scale.maximum >= 10.2f)
        assertEquals(1f, scale.heightFraction(0f))
        assertTrue(scale.heightFraction(10.2f) < 0.1f)
    }

    @Test
    fun stationaryAndUnavailableSamplesDoNotCreateInvalidCoordinates() {
        val scale = rollingChartScale(listOf(0f, Float.NaN, Float.POSITIVE_INFINITY), true)

        assertEquals(0.5f, scale.heightFraction(0f))
        assertTrue(scale.maximum.isFinite())
        assertTrue(scale.maximum > scale.minimum)
        assertEquals(0.5f, rollingChartScale(emptyList(), true).heightFraction(0f))
    }

    @Test
    fun shortHistoryIsRightAlignedRatherThanStretchedToTenSeconds() {
        assertEquals(1f, rollingChartTimeFraction(0, 1))
        assertEquals(0.975f, rollingChartTimeFraction(0, 2))
        assertEquals(1f, rollingChartTimeFraction(1, 2))
        assertEquals(0f, rollingChartTimeFraction(0, RollingChartSampleCapacity))
        assertEquals(1f, rollingChartTimeFraction(40, RollingChartSampleCapacity))
        assertEquals(10_000L, (RollingChartSampleCapacity - 1) * RollingChartSampleIntervalMs)
    }
}
