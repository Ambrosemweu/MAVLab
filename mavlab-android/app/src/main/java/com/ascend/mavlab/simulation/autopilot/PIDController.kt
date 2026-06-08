package com.ascend.mavlab.simulation.autopilot

class PIDController(
    var kP: Float,
    var kI: Float = 0f,
    var kD: Float = 0f,
    var iMax: Float = 1f,
    var outputMin: Float = -1f,
    var outputMax: Float = 1f,
) {
    private var integral = 0f
    private var previousError = 0f
    private var initialized = false

    fun update(error: Float, dt: Float): Float {
        if (dt <= 0f) return 0f
        integral = (integral + error * dt).coerceIn(-iMax, iMax)
        val derivative = if (initialized) (error - previousError) / dt else 0f
        previousError = error
        initialized = true
        return (kP * error + kI * integral + kD * derivative)
            .coerceIn(outputMin, outputMax)
    }

    fun reset() {
        integral = 0f
        previousError = 0f
        initialized = false
    }
}
