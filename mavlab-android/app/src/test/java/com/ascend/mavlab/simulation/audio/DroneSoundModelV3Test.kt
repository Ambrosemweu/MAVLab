package com.ascend.mavlab.simulation.audio

import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.MotorTelemetry
import com.ascend.mavlab.simulation.failures.FailureState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DroneSoundModelV3Test {
    @Test
    fun disabledSoundSilencesProceduralLayer() {
        val frame = DroneSoundModel.compute(
            state = armedState(rpm = 6000f),
            failures = FailureState(),
            settings = DroneSoundSettings(enabled = false),
        )

        assertFalse(frame.procedural.enabled)
        assertEquals(0f, frame.procedural.averageBladePassHz)
        assertEquals(0, frame.procedural.harmonicCount)
    }

    @Test
    fun disabledProceduralLayerKeepsSampleBedAmountForSoundPoolMix() {
        val frame = DroneSoundModel.compute(
            state = armedState(rpm = 6000f),
            failures = FailureState(),
            settings = DroneSoundSettings(proceduralEnabled = false, sampleBedAmount = 0.35f),
        )

        assertFalse(frame.procedural.enabled)
        assertEquals(0.35f, frame.procedural.sampleBedAmount, 0.001f)
    }

    @Test
    fun computesPerMotorBladePassFrequencies() {
        val frame = DroneSoundModel.compute(
            state = DroneState(
                armed = true,
                motors = listOf(
                    MotorTelemetry(rpm = 3000f, command = 0.3f),
                    MotorTelemetry(rpm = 6000f, command = 0.6f),
                    MotorTelemetry(rpm = 9000f, command = 0.9f),
                    MotorTelemetry(rpm = 0f, command = 0f),
                ),
            ),
            failures = FailureState(),
            settings = DroneSoundSettings(bladeCount = 2),
        )

        assertTrue(frame.procedural.enabled)
        assertEquals(listOf(100f, 200f, 300f, 0f), frame.procedural.motorBladePassHz)
        assertEquals(150f, frame.procedural.averageBladePassHz, 0.001f)
    }

    @Test
    fun bladeCountChangesBladePassFrequency() {
        val twoBlade = DroneSoundModel.compute(
            state = armedState(rpm = 6000f),
            failures = FailureState(),
            settings = DroneSoundSettings(bladeCount = 2),
        )
        val threeBlade = DroneSoundModel.compute(
            state = armedState(rpm = 6000f),
            failures = FailureState(),
            settings = DroneSoundSettings(bladeCount = 3),
        )

        assertEquals(200f, twoBlade.procedural.averageBladePassHz, 0.001f)
        assertEquals(300f, threeBlade.procedural.averageBladePassHz, 0.001f)
    }

    @Test
    fun synthQualityControlsHarmonicCount() {
        val eco = DroneSoundModel.compute(
            state = armedState(rpm = 6000f),
            failures = FailureState(),
            settings = DroneSoundSettings(synthQuality = DroneSynthQuality.ECO),
        )
        val high = DroneSoundModel.compute(
            state = armedState(rpm = 6000f),
            failures = FailureState(),
            settings = DroneSoundSettings(synthQuality = DroneSynthQuality.HIGH),
        )

        assertEquals(4, eco.procedural.harmonicCount)
        assertEquals(12, high.procedural.harmonicCount)
    }

    @Test
    fun propWashRespondsToThrottleDescentWindAndPayload() {
        val calm = DroneSoundModel.compute(
            state = armedState(rpm = 3000f, throttle = 20, verticalSpeed = 0f, groundSpeed = 0f, currentCa = 120),
            failures = FailureState(),
            settings = DroneSoundSettings(propWashAmount = 1f),
        )
        val loadedDescent = DroneSoundModel.compute(
            state = armedState(rpm = 7000f, throttle = 85, verticalSpeed = -3f, groundSpeed = 12f, currentCa = 2000),
            failures = FailureState(windGustsMs = 6f, payloadMassKg = 2f),
            settings = DroneSoundSettings(propWashAmount = 1f),
        )

        assertTrue(loadedDescent.procedural.propWashGain > calm.procedural.propWashGain)
    }

    @Test
    fun motorWhineRespondsToRpmAndProfile() {
        val cargoLowRpm = DroneSoundModel.compute(
            state = armedState(rpm = 2000f),
            failures = FailureState(),
            settings = DroneSoundSettings(
                motorWhineAmount = 1f,
                acousticProfileId = DroneAcousticProfile.CargoQuad.id,
            ),
        )
        val racingHighRpm = DroneSoundModel.compute(
            state = armedState(rpm = 12000f),
            failures = FailureState(),
            settings = DroneSoundSettings(
                motorWhineAmount = 1f,
                acousticProfileId = DroneAcousticProfile.SmallRacingQuad.id,
            ),
        )

        assertTrue(racingHighRpm.procedural.motorWhineGain > cargoLowRpm.procedural.motorWhineGain)
    }

    @Test
    fun testModeFeedsProceduralLayerWhenDisarmed() {
        val frame = DroneSoundModel.compute(
            state = DroneState(armed = false),
            failures = FailureState(),
            settings = DroneSoundSettings(testMode = true, testRpm = 4200f, bladeCount = 2),
        )

        assertTrue(frame.procedural.enabled)
        assertEquals(140f, frame.procedural.averageBladePassHz, 0.001f)
    }

    private fun armedState(
        rpm: Float,
        throttle: Int = 60,
        verticalSpeed: Float = 0f,
        groundSpeed: Float = 0f,
        currentCa: Short = 120,
    ): DroneState {
        return DroneState(
            armed = true,
            throttlePercent = throttle.toUByte(),
            verticalSpeedMS = verticalSpeed,
            groundSpeedMS = groundSpeed,
            batteryCurrentCa = currentCa,
            motors = List(4) {
                MotorTelemetry(
                    rpm = rpm,
                    command = (rpm / DroneSoundSettings.MaxReferenceRpm).coerceIn(0f, 1f),
                )
            },
        )
    }
}
