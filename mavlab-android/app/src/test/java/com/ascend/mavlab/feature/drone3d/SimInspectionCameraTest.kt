package com.ascend.mavlab.feature.drone3d

import com.ascend.mavlab.simulation.engine.DroneState
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimInspectionCameraTest {
    @Test
    fun initialViewIsElevatedFrontRightThreeQuarter() {
        val eye = SimInspectionCamera.homeEye(SimCameraPoint(), 0f)

        assertTrue(eye.x > 0f && eye.y > 0f && eye.z > 0f)
        assertEquals(35f, atan2(eye.x, eye.z) * 180f / PI.toFloat(), 0.001f)
        assertEquals(28f, atan2(eye.y, sqrt(eye.x * eye.x + eye.z * eye.z)) * 180f / PI.toFloat(), 0.001f)
        assertEquals(1.05f, length(eye), 0.001f)
    }

    @Test
    fun quarterTurnHeadingRotatesEyeWithTheModelYaw() {
        val front = SimInspectionCamera.homeEye(SimCameraPoint(), 0f)
        val turned = SimInspectionCamera.homeEye(SimCameraPoint(), PI.toFloat() / 2f)

        assertEquals(front.z, turned.x, 0.001f)
        assertEquals(front.y, turned.y, 0.001f)
        assertEquals(-front.x, turned.z, 0.001f)
    }

    @Test
    fun referenceHeadingAndTranslatedVehicleRetainTheSameFraming() {
        val state = DroneState(northMeters = 829f, eastMeters = 371f, altitudeAglMeters = 18f, yawRadians = 177f * PI.toFloat() / 180f)
        // Rendered scene coordinates, not raw mission metres. The screen
        // supplies these from bodyPosition; no native renderer is needed here.
        val target = SimCameraPoint(0.85f, 1.03f, -0.85f)
        val eye = SimInspectionCamera.homeEye(target, state.yawRadians)
        val offset = SimCameraPoint(eye.x - target.x, eye.y - target.y, eye.z - target.z)

        assertEquals(SimInspectionCamera.Distance, length(offset), 0.001f)
        assertEquals(28f, atan2(offset.y, sqrt(offset.x * offset.x + offset.z * offset.z)) * 180f / PI.toFloat(), 0.001f)
        // 177 + 35 wraps to -148 degrees in atan2.
        assertEquals(-148f, atan2(offset.x, offset.z) * 180f / PI.toFloat(), 0.001f)
        assertEquals(177f * PI.toFloat() / 180f, state.yawRadians)
        assertEquals(18f, state.altitudeAglMeters)
    }

    @Test
    fun equivalentWrappedHeadingsHaveTheSameHomeEye() {
        val target = SimCameraPoint(0.5f, 0.8f, -0.3f)
        val first = SimInspectionCamera.homeEye(target, 0.7f)
        val wrapped = SimInspectionCamera.homeEye(target, 0.7f + 2f * PI.toFloat())

        assertEquals(first.x, wrapped.x, 0.001f)
        assertEquals(first.y, wrapped.y, 0.001f)
        assertEquals(first.z, wrapped.z, 0.001f)
    }

    @Test
    fun invalidHeadingCannotPoisonTheCameraHome() {
        val target = SimCameraPoint(0f, 0.35f, 0f)
        val fallback = SimInspectionCamera.homeEye(target, 0f)
        for (yaw in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(fallback, SimInspectionCamera.homeEye(target, yaw))
        }
    }

    private fun length(position: SimCameraPoint): Float = sqrt(position.x * position.x + position.y * position.y + position.z * position.z)
}
