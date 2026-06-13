package com.ascend.mavlab.simulation.autopilot

import com.ascend.mavlab.simulation.engine.DroneState
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PositionControllerTest {
    @Test
    fun turnsTowardSideTargetBeforeTranslating() {
        val controller = PositionController()

        val input = controller.computePilotInput(
            state = DroneState(yawRadians = 0f),
            targetNorthMeters = 0f,
            targetEastMeters = 20f,
            targetAltitudeMeters = 10f,
            dt = 0.02f,
        )

        assertTrue(input.yaw > 0f)
        assertEquals(0f, input.pitch, absoluteTolerance = 0.001f)
        assertEquals(0f, input.roll, absoluteTolerance = 0.001f)
    }

    @Test
    fun fliesForwardWhenAlignedWithTargetBearing() {
        val controller = PositionController()

        val input = controller.computePilotInput(
            state = DroneState(yawRadians = (PI / 2.0).toFloat()),
            targetNorthMeters = 0f,
            targetEastMeters = 20f,
            targetAltitudeMeters = 10f,
            dt = 0.02f,
        )

        assertEquals(0f, input.yaw, absoluteTolerance = 0.001f)
        assertTrue(input.pitch > 0f)
    }

    @Test
    fun lowerMissionSpeedReducesForwardCommand() {
        val slow = PositionController().computePilotInput(
            state = DroneState(yawRadians = 0f),
            targetNorthMeters = 100f,
            targetEastMeters = 0f,
            targetAltitudeMeters = 10f,
            dt = 0.02f,
            maxHorizontalSpeedMS = 1f,
        )
        val fast = PositionController().computePilotInput(
            state = DroneState(yawRadians = 0f),
            targetNorthMeters = 100f,
            targetEastMeters = 0f,
            targetAltitudeMeters = 10f,
            dt = 0.02f,
            maxHorizontalSpeedMS = 7.5f,
        )

        assertTrue(fast.pitch > slow.pitch)
    }
}
