package com.ascend.mavlab.simulation.physics

data class EnvironmentModel(
    val windNedMS: Vector3 = Vector3.ZERO,
    val airDensityKgM3: Float = 1.225f,
)
