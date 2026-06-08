package com.ascend.mavlab.simulation.failures

data class FailureScenario(
    val id: String,
    val title: String,
    val description: String,
    val learningGoal: String,
    val apply: (FailureInjector) -> Unit,
)

val FailureScenarios = listOf(
    FailureScenario(
        id = "gps_loss",
        title = "GPS Signal Lost",
        description = "GPS is unavailable, so assisted position modes degrade to altitude hold.",
        learningGoal = "Observe how autonomous flight depends on healthy positioning.",
        apply = { it.setGpsEnabled(false) },
    ),
    FailureScenario(
        id = "gps_drift",
        title = "GPS Drift",
        description = "GPS noise is increased to simulate multipath around buildings.",
        learningGoal = "See why noisy position data causes wandering in hold modes.",
        apply = { it.setGpsNoiseMultiplier(5f) },
    ),
    FailureScenario(
        id = "windy_day",
        title = "Strong Wind",
        description = "A strong easterly wind with gusts pushes the aircraft off track.",
        learningGoal = "Watch the controller fight external disturbance.",
        apply = {
            it.setWindSpeedMs(8f)
            it.setWindDirectionDeg(90f)
            it.setWindGustsMs(2f)
        },
    ),
    FailureScenario(
        id = "motor_failure",
        title = "Motor 3 Failure",
        description = "One motor produces no thrust.",
        learningGoal = "Understand why quadcopters have limited motor redundancy.",
        apply = { it.setMotorFailed(index = 2, failed = true) },
    ),
    FailureScenario(
        id = "battery_low",
        title = "Fast Battery Drain",
        description = "Battery drain is accelerated until the RTL failsafe triggers.",
        learningGoal = "Observe low-battery failsafe behavior.",
        apply = { it.setBatteryDrainMultiplier(10f) },
    ),
    FailureScenario(
        id = "compass_interference",
        title = "Compass Interference",
        description = "Heading is offset by nearby magnetic interference.",
        learningGoal = "See how bad heading data degrades navigation.",
        apply = { it.setCompassOffsetDeg(45f) },
    ),
    FailureScenario(
        id = "heavy_payload",
        title = "Heavy Payload",
        description = "Adds mass so climb response and battery life degrade.",
        learningGoal = "Relate payload mass to thrust margin.",
        apply = { it.setPayloadMassKg(1f) },
    ),
)
