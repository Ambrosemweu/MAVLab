package com.ascend.mavlab.simulation.engine

data class MotorTelemetry(
    val rpm: Float = 0f,
    val command: Float = 0f,
    val failed: Boolean = false,
)
