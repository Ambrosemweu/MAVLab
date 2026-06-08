package com.ascend.mavlab.core.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.sqrt

class PhoneSensorRepository(
    private val sensorManager: SensorManager,
) {
    fun activeSource(): OrientationSource {
        return when {
            sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null ->
                OrientationSource.GameRotationVector
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ->
                OrientationSource.RotationVector
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null ->
                OrientationSource.Accelerometer
            else -> OrientationSource.Unavailable
        }
    }

    fun orientationFlow(): Flow<OrientationData> = callbackFlow {
        val gameRotation = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val sensor = gameRotation ?: rotation ?: accelerometer

        if (sensor == null) {
            trySend(OrientationData(source = OrientationSource.Unavailable))
            close()
            return@callbackFlow
        }

        val source = when (sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR -> OrientationSource.GameRotationVector
            Sensor.TYPE_ROTATION_VECTOR -> OrientationSource.RotationVector
            else -> OrientationSource.Accelerometer
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        fromAccelerometer(event, source)
                    } else {
                        fromRotationVector(event, source)
                    },
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private fun fromRotationVector(
        event: SensorEvent,
        source: OrientationSource,
    ): OrientationData {
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientation)
        return OrientationData(
            yaw = orientation[0],
            pitch = orientation[1],
            roll = orientation[2],
            timestampNanos = event.timestamp,
            source = source,
        )
    }

    private fun fromAccelerometer(
        event: SensorEvent,
        source: OrientationSource,
    ): OrientationData {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val roll = atan2(y, z)
        val pitch = atan2(-x, sqrt(y * y + z * z))
        return OrientationData(
            roll = roll,
            pitch = pitch,
            yaw = 0f,
            timestampNanos = event.timestamp,
            source = source,
        )
    }
}
