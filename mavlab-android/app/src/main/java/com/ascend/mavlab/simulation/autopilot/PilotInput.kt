package com.ascend.mavlab.simulation.autopilot

data class PilotInput(
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val throttle: Float = 0.5f,
    val yaw: Float = 0f,
)
