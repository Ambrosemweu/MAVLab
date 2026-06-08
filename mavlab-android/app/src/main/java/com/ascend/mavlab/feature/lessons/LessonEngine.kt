package com.ascend.mavlab.feature.lessons

import com.ascend.mavlab.core.common.AppRuntime
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.failures.FailureScenarios
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.mission.MissionProgress

object LessonEngine {
    fun perform(action: StepAction) {
        when (action) {
            StepAction.ReadOnly -> Unit
            StepAction.ArmDrone -> AppRuntime.setArmed(true)
            is StepAction.ChangeMode -> AppRuntime.setMode(action.mode)
            is StepAction.Takeoff -> AppRuntime.takeoff(action.altitude)
            is StepAction.InjectFailure -> {
                FailureScenarios.firstOrNull { it.id == action.failureId }
                    ?.let(AppRuntime::applyFailureScenario)
            }
            StepAction.LoadMission -> AppRuntime.loadDemoMission()
            StepAction.StartMission -> AppRuntime.startAutoMission()
            StepAction.LandDrone -> AppRuntime.land()
        }
    }

    fun isComplete(
        check: CompletionCheck,
        state: DroneState,
        failures: FailureState,
        mission: MissionProgress,
    ): Boolean {
        return when (check) {
            CompletionCheck.Manual -> false
            CompletionCheck.DroneArmed -> state.armed
            CompletionCheck.DroneDisarmed -> !state.armed
            is CompletionCheck.DroneInMode -> state.mode == check.mode
            is CompletionCheck.AltitudeAbove -> state.altitudeAglMeters >= check.meters
            is CompletionCheck.AltitudeBelow -> state.altitudeAglMeters <= check.meters
            is CompletionCheck.ActiveFailure -> activeFailure(check.failureId, failures)
            CompletionCheck.MissionLoaded -> mission.loaded
            CompletionCheck.DroneOnGround -> state.altitudeAglMeters <= 0.15f && !state.armed
        }
    }

    private fun activeFailure(id: String, failures: FailureState): Boolean {
        return when (id) {
            "gps_loss" -> !failures.gpsEnabled
            "gps_drift" -> failures.gpsNoiseMultiplier >= 4.5f
            "windy_day" -> failures.windSpeedMs >= 7f
            "motor_failure" -> failures.hasMotorFailure
            "battery_low" -> failures.batteryDrainMultiplier >= 9f
            "compass_interference" -> kotlin.math.abs(failures.compassOffsetDeg) >= 40f
            "heavy_payload" -> failures.payloadMassKg >= 0.9f
            else -> false
        }
    }
}
