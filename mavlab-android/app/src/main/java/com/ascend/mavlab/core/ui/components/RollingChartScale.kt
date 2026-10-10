package com.ascend.mavlab.core.ui.components

import kotlin.math.abs
import kotlin.math.ceil

internal const val RollingChartSampleIntervalMs = 250L
internal const val RollingChartSampleCapacity = 41 // 40 intervals = 10 seconds.

internal data class RollingChartScale(val minimum: Float, val maximum: Float) {
    fun heightFraction(value: Float): Float =
        ((maximum - value) / (maximum - minimum)).coerceIn(0f, 1f)
}

internal fun rollingChartScale(values: List<Float>, symmetricAroundZero: Boolean): RollingChartScale {
    val finiteValues = values.filter { it.isFinite() }
    val magnitude = finiteValues.maxOfOrNull { if (symmetricAroundZero) abs(it) else it } ?: 0f
    val maximum = ceil(magnitude.coerceAtLeast(1f))
    return RollingChartScale(if (symmetricAroundZero) -maximum else 0f, maximum)
}

internal fun rollingChartScaleForAxis(
    series: List<ChartSeries>,
    axis: ChartAxis,
    symmetricAroundZero: Boolean,
): RollingChartScale = rollingChartScale(
    series.filter { it.axis == axis }.flatMap { it.values },
    symmetricAroundZero,
)

internal fun rollingChartTimeFraction(index: Int, sampleCount: Int): Float =
    (RollingChartSampleCapacity - sampleCount + index).toFloat() / (RollingChartSampleCapacity - 1)
