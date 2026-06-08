package com.ascend.mavlab.simulation.physics

import kotlin.math.sqrt

data class Vector3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vector3(x / scalar, y / scalar, z / scalar)

    val magnitude: Float
        get() = sqrt(x * x + y * y + z * z)

    fun normalized(): Vector3 = if (magnitude > 0f) this / magnitude else ZERO

    companion object {
        val ZERO = Vector3()
    }
}
