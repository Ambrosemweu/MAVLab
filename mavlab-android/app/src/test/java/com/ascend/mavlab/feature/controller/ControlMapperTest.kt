package com.ascend.mavlab.feature.controller

import com.ascend.mavlab.core.sensors.OrientationData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControlMapperTest {
    private val config = ControlConfig(expo = 1f)
    private val mapper = ControlMapper(config)

    @Test
    fun deadzoneSuppressesSmallTilt() {
        val input = mapper.map(
            orientation = OrientationData(
                roll = config.deadzoneRad * 0.5f,
                pitch = config.deadzoneRad * 0.5f,
                yaw = config.deadzoneRad * 0.5f,
            ),
            throttle = 0.5f,
        )

        assertEquals(0f, input.roll)
        assertEquals(0f, input.pitch)
        assertEquals(0f, input.yaw)
    }

    @Test
    fun largeTiltClampsToFullInput() {
        val input = mapper.map(
            orientation = OrientationData(
                roll = config.maxRollAngleRad * 2f,
                pitch = -config.maxPitchAngleRad * 2f,
                yaw = config.maxYawAngleRad * 2f,
            ),
            throttle = 2f,
        )

        assertEquals(1f, input.roll)
        assertEquals(1f, input.pitch)
        assertEquals(1f, input.yaw)
        assertEquals(1f, input.throttle)
    }

    @Test
    fun manualYawCombinesWithTiltYaw() {
        val input = mapper.map(
            orientation = OrientationData(yaw = config.maxYawAngleRad),
            throttle = 0.25f,
            manualYaw = -0.5f,
        )

        assertTrue(input.yaw in 0.49f..0.51f)
        assertEquals(0.25f, input.throttle)
    }
}
