package com.ascend.mavlab.simulation.physics

import kotlin.math.sqrt

data class QuadcopterParams(
    val massKg: Float = 1.5f,
    val armLengthM: Float = 0.225f,
    val ixx: Float = 0.0347f,
    val iyy: Float = 0.0347f,
    val izz: Float = 0.0977f,
    val thrustCoefficient: Float = 1.4715e-5f,
    val torqueCoefficient: Float = 1.0e-7f,
    val motorMaxSpeedRadS: Float = 1000f,
    val motorMinSpeedRadS: Float = 0f,
    val dragCoefficientXY: Float = 0.10f,
    val dragCoefficientZ: Float = 0.15f,
    val rotationalDrag: Float = 0.01f,
    val gravityMS2: Float = 9.81f,
    val batteryCapacityWh: Float = 50f,
    val batteryVoltageFull: Float = 12.6f,
    val batteryVoltageEmpty: Float = 10.5f,
    val hoverCurrentDrawA: Float = 10f,
    val maxTiltAngleRad: Float = 0.6f,
    val maxClimbRateMS: Float = 5f,
    val maxDescentRateMS: Float = 3f,
) {
    val hoverMotorSpeedRadS: Float =
        sqrt((massKg * gravityMS2) / (4f * thrustCoefficient))

    val hoverThrottle: Float =
        (hoverMotorSpeedRadS / motorMaxSpeedRadS).coerceIn(0f, 1f)
}
