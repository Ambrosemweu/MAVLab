package com.ascend.mavlab.core.sensors

class SensorCalibration {
    private var reference = OrientationData()
    private var calibrated = false

    fun calibrate(current: OrientationData) {
        reference = current
        calibrated = true
    }

    fun apply(raw: OrientationData): OrientationData {
        if (!calibrated) return raw
        return raw.copy(
            roll = normalize(raw.roll - reference.roll),
            pitch = normalize(raw.pitch - reference.pitch),
            yaw = normalize(raw.yaw - reference.yaw),
        )
    }

    fun isCalibrated(): Boolean = calibrated

    private fun normalize(value: Float): Float {
        var angle = value
        val twoPi = (2.0 * Math.PI).toFloat()
        while (angle > Math.PI) angle -= twoPi
        while (angle < -Math.PI) angle += twoPi
        return angle
    }
}
