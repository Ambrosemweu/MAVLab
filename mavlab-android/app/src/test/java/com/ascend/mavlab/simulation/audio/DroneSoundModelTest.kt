package com.ascend.mavlab.simulation.audio

import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.MotorTelemetry
import com.ascend.mavlab.simulation.failures.FailureState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DroneSoundModelTest {
    @Test
    fun disabledSoundProducesSilentMotors() {
        val frame = DroneSoundModel.compute(
            state = armedState(4000f),
            failures = FailureState(),
            settings = DroneSoundSettings(enabled = false),
        )

        assertTrue(frame.motors.all { it.volume == 0f })
        assertEquals(0, frame.activeMotorCount)
    }

    @Test
    fun disarmedDroneProducesSilentMotorsWhenNotInTestMode() {
        val frame = DroneSoundModel.compute(
            state = armedState(4000f).copy(armed = false),
            failures = FailureState(),
            settings = DroneSoundSettings(testMode = false),
        )

        assertTrue(frame.motors.all { it.volume == 0f })
        assertEquals(0, frame.activeMotorCount)
    }

    @Test
    fun testModeProducesAudibleVirtualMotorsWhenDisarmed() {
        val frame = DroneSoundModel.compute(
            state = DroneState(armed = false),
            failures = FailureState(),
            settings = DroneSoundSettings(testMode = true, testRpm = 3500f),
        )

        assertEquals(4, frame.activeMotorCount)
        assertTrue(frame.motors.all { it.volume > 0f })
        assertTrue(frame.averagePlaybackRate > 0.55f)
    }

    @Test
    fun higherRpmIncreasesPlaybackRate() {
        val low = DroneSoundModel.compute(
            state = armedState(2000f),
            failures = FailureState(),
            settings = DroneSoundSettings(),
        )
        val high = DroneSoundModel.compute(
            state = armedState(8000f),
            failures = FailureState(),
            settings = DroneSoundSettings(),
        )

        assertTrue(high.averagePlaybackRate > low.averagePlaybackRate)
    }

    @Test
    fun failedMotorIsSilent() {
        val frame = DroneSoundModel.compute(
            state = DroneState(
                armed = true,
                motors = listOf(
                    MotorTelemetry(rpm = 4000f, command = 0.5f, failed = true),
                    MotorTelemetry(rpm = 4000f, command = 0.5f),
                    MotorTelemetry(rpm = 4000f, command = 0.5f),
                    MotorTelemetry(rpm = 4000f, command = 0.5f),
                ),
            ),
            failures = FailureState(motorFailureMask = 1),
            settings = DroneSoundSettings(),
        )

        assertEquals(0f, frame.motors[0].volume)
        assertEquals(3, frame.activeMotorCount)
    }

    @Test
    fun rpmSpreadIncreasesRoughness() {
        val equal = DroneSoundModel.compute(
            state = DroneState(
                armed = true,
                motors = List(4) { MotorTelemetry(rpm = 4000f, command = 0.5f) },
            ),
            failures = FailureState(),
            settings = DroneSoundSettings(roughness = 1f),
        )
        val spread = DroneSoundModel.compute(
            state = DroneState(
                armed = true,
                motors = listOf(
                    MotorTelemetry(rpm = 2500f, command = 0.4f),
                    MotorTelemetry(rpm = 4000f, command = 0.5f),
                    MotorTelemetry(rpm = 5500f, command = 0.6f),
                    MotorTelemetry(rpm = 7000f, command = 0.7f),
                ),
            ),
            failures = FailureState(),
            settings = DroneSoundSettings(roughness = 1f),
        )

        assertTrue(spread.roughness > equal.roughness)
    }

    @Test
    fun lowBatteryProducesLowBatteryAlert() {
        val frame = DroneSoundModel.compute(
            state = armedState(3500f).copy(batteryRemainingPercent = 25.toByte()),
            failures = FailureState(),
            settings = DroneSoundSettings(),
        )

        assertEquals(DroneSoundAlert.LOW_BATTERY, frame.alert)
    }

    @Test
    fun criticalBatteryOverridesLowBatteryAlert() {
        val frame = DroneSoundModel.compute(
            state = armedState(3500f).copy(batteryRemainingPercent = 10.toByte()),
            failures = FailureState(),
            settings = DroneSoundSettings(),
        )

        assertEquals(DroneSoundAlert.CRITICAL_BATTERY, frame.alert)
    }

    @Test
    fun unsafeReserveProducesAlertWhenBatteryHealthy() {
        val frame = DroneSoundModel.compute(
            state = armedState(3500f),
            failures = FailureState(unsafeMissionReserveActive = true),
            settings = DroneSoundSettings(),
        )

        assertEquals(DroneSoundAlert.UNSAFE_RESERVE, frame.alert)
    }

    @Test
    fun perMotorMixChangesIndividualRates() {
        val state = DroneState(
            armed = true,
            motors = listOf(
                MotorTelemetry(rpm = 2000f, command = 0.3f),
                MotorTelemetry(rpm = 3500f, command = 0.4f),
                MotorTelemetry(rpm = 5500f, command = 0.6f),
                MotorTelemetry(rpm = 8000f, command = 0.8f),
            ),
        )
        val smoothed = DroneSoundModel.compute(
            state = state,
            failures = FailureState(),
            settings = DroneSoundSettings(perMotorMix = 0f),
        )
        val individualized = DroneSoundModel.compute(
            state = state,
            failures = FailureState(),
            settings = DroneSoundSettings(perMotorMix = 1f),
        )
        val smoothedSpread = smoothed.motors.maxOf { it.playbackRate } - smoothed.motors.minOf { it.playbackRate }
        val individualSpread = individualized.motors.maxOf { it.playbackRate } - individualized.motors.minOf { it.playbackRate }

        assertTrue(individualSpread > smoothedSpread)
    }

    private fun armedState(rpm: Float): DroneState {
        return DroneState(
            armed = true,
            motors = List(4) { MotorTelemetry(rpm = rpm, command = (rpm / 9500f).coerceIn(0f, 1f)) },
        )
    }
}
