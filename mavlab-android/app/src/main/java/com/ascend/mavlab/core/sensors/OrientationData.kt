package com.ascend.mavlab.core.sensors

data class OrientationData(
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val timestampNanos: Long = 0L,
    val source: OrientationSource = OrientationSource.Unavailable,
)

enum class OrientationSource(val label: String) {
    GameRotationVector("Game rotation"),
    RotationVector("Rotation vector"),
    Accelerometer("Accelerometer fallback"),
    Unavailable("Unavailable"),
}
