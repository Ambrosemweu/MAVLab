package com.ascend.mavlab.core.mavlink

import com.ascend.mavlab.simulation.engine.DroneState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class MavlinkMessageBuilder(
    private val systemId: Int,
    private val componentId: Int,
) {
    private var sequence = 0

    fun heartbeat(state: DroneState): ByteArray {
        val payload = littleEndian(9)
            .putInt(state.mode.customMode.toInt())
            .putU8(MAV_TYPE_QUADROTOR)
            .putU8(MAV_AUTOPILOT_ARDUPILOTMEGA)
            .putU8(MAV_MODE_FLAG_CUSTOM_MODE_ENABLED or if (state.armed) MAV_MODE_FLAG_SAFETY_ARMED else 0)
            .putU8(if (state.armed) MAV_STATE_ACTIVE else MAV_STATE_STANDBY)
            .putU8(3)
            .array()
        return frame(messageId = 0, crcExtra = 50, payload = payload)
    }

    fun attitude(state: DroneState): ByteArray {
        val payload = littleEndian(28)
            .putInt(state.uptimeMs.toInt())
            .putFloat(state.rollRadians)
            .putFloat(state.pitchRadians)
            .putFloat(state.yawRadians)
            .putFloat(state.rollSpeedRadS)
            .putFloat(state.pitchSpeedRadS)
            .putFloat(state.yawSpeedRadS)
            .array()
        return frame(messageId = 30, crcExtra = 39, payload = payload)
    }

    fun globalPosition(state: DroneState): ByteArray {
        val payload = littleEndian(28)
            .putInt(state.uptimeMs.toInt())
            .putInt((state.latitudeDeg * 1e7).roundToInt())
            .putInt((state.longitudeDeg * 1e7).roundToInt())
            .putInt((state.altitudeMslMeters * 1000).roundToInt())
            .putInt((state.altitudeAglMeters * 1000).roundToInt())
            .putShort((state.groundSpeedMS * 100).roundToInt().toShort())
            .putShort(0)
            .putShort((state.verticalSpeedMS * 100).roundToInt().toShort())
            .putU16((state.headingDegrees * 100).coerceAtLeast(0))
            .array()
        return frame(messageId = 33, crcExtra = 104, payload = payload)
    }

    fun gpsRaw(state: DroneState): ByteArray {
        val payload = littleEndian(30)
            .putLong(System.currentTimeMillis() * 1000L)
            .putInt((state.latitudeDeg * 1e7).roundToInt())
            .putInt((state.longitudeDeg * 1e7).roundToInt())
            .putInt((state.altitudeMslMeters * 1000).roundToInt())
            .putU16(80)
            .putU16(120)
            .putU16((state.groundSpeedMS * 100).roundToInt())
            .putU16((state.headingDegrees * 100).coerceAtLeast(0))
            .putU8(state.gpsFixType.toInt())
            .putU8(state.gpsSatellites.toInt())
            .array()
        return frame(messageId = 24, crcExtra = 24, payload = payload)
    }

    fun vfrHud(state: DroneState): ByteArray {
        val payload = littleEndian(20)
            .putFloat(state.groundSpeedMS)
            .putFloat(state.groundSpeedMS)
            .putFloat(state.altitudeMslMeters)
            .putFloat(state.verticalSpeedMS)
            .putShort(state.headingDegrees)
            .putU16(if (state.armed) 45 else 0)
            .array()
        return frame(messageId = 74, crcExtra = 20, payload = payload)
    }

    fun sysStatus(state: DroneState): ByteArray {
        val payload = littleEndian(31)
            .putInt(1)
            .putInt(1)
            .putInt(1)
            .putU16(250)
            .putU16(state.batteryVoltageMv.toInt())
            .putShort(state.batteryCurrentCa)
            .put(state.batteryRemainingPercent)
            .putU16(0)
            .putU16(0)
            .putU16(0)
            .putU16(0)
            .putU16(0)
            .putU16(0)
            .array()
        return frame(messageId = 1, crcExtra = 124, payload = payload)
    }

    fun batteryStatus(state: DroneState): ByteArray {
        val payload = littleEndian(36)
            .putInt(0)
            .putU16(state.batteryVoltageMv.toInt())
            .putU16(UShort.MAX_VALUE.toInt())
            .putU16(UShort.MAX_VALUE.toInt())
            .putU16(UShort.MAX_VALUE.toInt())
            .putU16(UShort.MAX_VALUE.toInt())
            .putU16(UShort.MAX_VALUE.toInt())
            .putU16(UShort.MAX_VALUE.toInt())
            .putU16(UShort.MAX_VALUE.toInt())
            .putU16(UShort.MAX_VALUE.toInt())
            .putU16(UShort.MAX_VALUE.toInt())
            .putShort(state.batteryCurrentCa)
            .putInt(-1)
            .put(state.batteryRemainingPercent)
            .putU8(0)
            .putU8(0)
            .putU8(3)
            .putU8(0)
            .array()
        return frame(messageId = 147, crcExtra = 154, payload = payload)
    }

    fun commandAck(command: Int, result: Int): ByteArray {
        val payload = littleEndian(3)
            .putU16(command)
            .putU8(result)
            .array()
        return frame(messageId = 77, crcExtra = 143, payload = payload)
    }

    fun paramValue(id: String, value: Float, index: Int, count: Int): ByteArray {
        val paramId = ByteArray(16)
        id.encodeToByteArray().copyInto(paramId, endIndex = minOf(id.length, 16))
        val payload = littleEndian(25)
            .putFloat(value)
            .putU16(count)
            .putU16(index)
            .put(paramId)
            .putU8(MAV_PARAM_TYPE_REAL32)
            .array()
        return frame(messageId = 22, crcExtra = 220, payload = payload)
    }

    private fun frame(messageId: Int, crcExtra: Int, payload: ByteArray): ByteArray {
        val header = ByteArray(10)
        header[0] = 0xfd.toByte()
        header[1] = payload.size.toByte()
        header[2] = 0
        header[3] = 0
        header[4] = nextSequence().toByte()
        header[5] = systemId.toByte()
        header[6] = componentId.toByte()
        header[7] = (messageId and 0xff).toByte()
        header[8] = ((messageId ushr 8) and 0xff).toByte()
        header[9] = ((messageId ushr 16) and 0xff).toByte()

        val checksum = MavlinkX25.crc(header.copyOfRange(1, header.size), payload, crcExtra)
        return header + payload + byteArrayOf(
            (checksum and 0xff).toByte(),
            ((checksum ushr 8) and 0xff).toByte(),
        )
    }

    private fun nextSequence(): Int {
        val current = sequence
        sequence = (sequence + 1) and 0xff
        return current
    }

    private fun littleEndian(size: Int): ByteBuffer {
        return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
    }

    private fun ByteBuffer.putU8(value: Int): ByteBuffer = put((value and 0xff).toByte())
    private fun ByteBuffer.putU16(value: Int): ByteBuffer = putShort((value and 0xffff).toShort())

    private object MavlinkX25 {
        fun crc(headerWithoutMagic: ByteArray, payload: ByteArray, extra: Int): Int {
            var crc = 0xffff
            (headerWithoutMagic + payload + byteArrayOf(extra.toByte())).forEach { byte ->
                var tmp = (byte.toInt() and 0xff) xor (crc and 0xff)
                tmp = tmp xor (tmp shl 4)
                crc = ((crc ushr 8) xor (tmp shl 8) xor (tmp shl 3) xor (tmp ushr 4)) and 0xffff
            }
            return crc
        }
    }

    private companion object {
        const val MAV_TYPE_QUADROTOR = 2
        const val MAV_AUTOPILOT_ARDUPILOTMEGA = 3
        const val MAV_MODE_FLAG_CUSTOM_MODE_ENABLED = 1
        const val MAV_MODE_FLAG_SAFETY_ARMED = 128
        const val MAV_STATE_STANDBY = 3
        const val MAV_STATE_ACTIVE = 4
        const val MAV_PARAM_TYPE_REAL32 = 9
    }
}
