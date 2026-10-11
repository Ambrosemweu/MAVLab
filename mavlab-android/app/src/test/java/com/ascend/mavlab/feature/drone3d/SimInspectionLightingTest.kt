package com.ascend.mavlab.feature.drone3d

import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimInspectionLightingTest {
    @Test fun fillMaintainsIlluminanceAcrossInspectionZoomDistances() {
        for (distance in listOf(0.1f, 0.3f, 1f, 3f, 10f, 100f)) {
            val squared = distance * distance
            val illuminance = SimInspectionLighting.fillLumens(squared) / (4.0 * PI * squared)
            assertEquals(SimInspectionLighting.FillIlluminanceLux.toDouble(), illuminance, 0.01)
            assertEquals(10f * distance, SimInspectionLighting.fillRange(squared), 0.001f)
        }
    }

    @Test fun fillIsIndependentOfOrbitSide() {
        for ((x, y, z) in listOf(
            Triple(3f, 0f, 0f), Triple(-3f, 0f, 0f),
            Triple(0f, 3f, 0f), Triple(0f, -3f, 0f),
            Triple(0f, 0f, 3f), Triple(0f, 0f, -3f),
        )) {
            assertEquals(SimInspectionLighting.fillLumens(9f),
                SimInspectionLighting.fillLumens(x * x + y * y + z * z))
        }
    }

    @Test fun invalidAndExtremeDistancesRemainFiniteAndPositive() {
        for (value in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.MAX_VALUE)) {
            assertTrue(SimInspectionLighting.fillLumens(value).isFinite())
            assertTrue(SimInspectionLighting.fillLumens(value) > 0f)
            assertTrue(SimInspectionLighting.fillRange(value).isFinite())
            assertTrue(SimInspectionLighting.fillRange(value) > 0f)
        }
        assertTrue(SimInspectionLighting.FillIlluminanceLux < SimInspectionLighting.KeyIlluminanceLux)
        assertTrue(SimInspectionLighting.fillRange(9f) > sqrt(9f))
    }
}
