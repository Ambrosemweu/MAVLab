package com.ascend.mavlab.feature.drone3d

import kotlin.math.PI
import kotlin.math.sqrt

/** Presentation only. Intensities never depend on vehicle yaw, roll or armed state. */
internal object SimInspectionLighting {
    const val EnvironmentIntensity = 25_000f
    const val KeyIlluminanceLux = 45_000f
    const val FillIlluminanceLux = 8_000f

    // Filament point lights use lumens. E = power / (4 pi r^2).
    fun fillLumens(distanceSquared: Float): Float =
        (4.0 * PI * FillIlluminanceLux * safeDistanceSquared(distanceSquared)).toFloat()

    // Keep the model far inside the light's culling/fade radius at every zoom.
    fun fillRange(distanceSquared: Float): Float =
        10f * sqrt(safeDistanceSquared(distanceSquared))

    private fun safeDistanceSquared(value: Float): Float =
        if (value.isFinite()) value.coerceIn(0.01f, 10_000f) else 1f
}
