package com.ascend.mavlab.feature.drone3d

import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.MotorTelemetry
import com.ascend.mavlab.simulation.failures.FailureState
import kotlin.test.Test
import kotlin.test.assertEquals

class Drone3DScreenTest {
    @Test
    fun aircraftVisualStatePrioritizesFailuresAndBatteryWarnings() {
        assertEquals(
            SimAircraftVisualState.Normal,
            aircraftVisualState(DroneState(batteryRemainingPercent = 80), FailureState()),
        )
        assertEquals(
            SimAircraftVisualState.Warning,
            aircraftVisualState(DroneState(batteryRemainingPercent = 25), FailureState()),
        )
        assertEquals(
            SimAircraftVisualState.Critical,
            aircraftVisualState(DroneState(batteryRemainingPercent = 12), FailureState()),
        )
        assertEquals(
            SimAircraftVisualState.Failure,
            aircraftVisualState(
                DroneState(motors = listOf(MotorTelemetry(rpm = 0f, failed = true))),
                FailureState(),
            ),
        )
        assertEquals(
            SimAircraftVisualState.Failure,
            aircraftVisualState(DroneState(batteryRemainingPercent = 12), FailureState(gpsEnabled = false)),
        )
    }

}
