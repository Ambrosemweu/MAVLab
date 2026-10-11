package com.ascend.mavlab.feature.drone3d

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Initial inspection framing only; never a vehicle-attitude override. */
internal object SimInspectionCamera {
    const val Distance = 1.05f
    const val AzimuthDegrees = 35f
    const val ElevationDegrees = 28f

    fun homeEye(target: SimCameraPoint, yawRadians: Float): SimCameraPoint {
        // The asset's forward-facing camera is on local +Z. Rotate this
        // front/right offset by the same Y-axis yaw used by the model node.
        val azimuth = AzimuthDegrees * PI.toFloat() / 180f
        val elevation = ElevationDegrees * PI.toFloat() / 180f
        val yaw = yawRadians.takeIf { it.isFinite() } ?: 0f
        val horizontalDistance = Distance * cos(elevation)
        return SimCameraPoint(
            x = target.x + horizontalDistance * sin(azimuth + yaw),
            y = target.y + Distance * sin(elevation),
            z = target.z + horizontalDistance * cos(azimuth + yaw),
        )
    }
}

// Plain geometry keeps camera-home calculations testable without loading
// SceneView/Filament's native renderer or its JVM-21 vector dependency.
internal data class SimCameraPoint(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f)
