package com.ascend.mavlab.feature.controller

import kotlin.math.PI

data class ControlConfig(
    val maxRollAngleRad: Float = (PI / 6.0).toFloat(),
    val maxPitchAngleRad: Float = (PI / 6.0).toFloat(),
    val maxYawAngleRad: Float = (PI / 4.0).toFloat(),
    val deadzoneRad: Float = Math.toRadians(3.0).toFloat(),
    val expo: Float = 1.45f,
)
