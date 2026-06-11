package com.ascend.mavlab.core.mavlink

import com.ascend.mavlab.simulation.engine.DroneState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MavlinkMessageBuilderTest {
    @Test
    fun socketConfigDefaultsToArduPilotVehicleIdentity() {
        val config = MavlinkSocketConfig()

        assertEquals(1, config.systemId)
        assertEquals(1, config.componentId)
        assertEquals(14551, config.localBindPort)
        assertEquals(14550, config.sameDeviceQgcPort)
    }

    @Test
    fun autopilotVersionAdvertisesMissionIntAndMavlink2() {
        val builder = MavlinkMessageBuilder(systemId = 174, componentId = 1)

        val data = builder.autopilotVersion()
        val packet = MavlinkParser.parse(data, length = data.size)

        assertNotNull(packet)
        assertEquals(0xfe, data[0].toInt() and 0xff)
        assertEquals(148, packet.messageId)
        assertEquals(174, packet.systemId)
        val capabilities = packet.payload.leUInt64(0)
        assertTrue(capabilities and MAV_PROTOCOL_CAPABILITY_MISSION_INT != 0L)
        assertTrue(capabilities and MAV_PROTOCOL_CAPABILITY_MAVLINK2 != 0L)
    }

    @Test
    fun missionCountUsesMavlinkFieldOrder() {
        val builder = MavlinkMessageBuilder(systemId = 1, componentId = 1)

        val data = builder.missionCount(count = 4, targetSystem = 255, targetComponent = 190)
        val packet = MavlinkParser.parse(data, length = data.size)

        assertNotNull(packet)
        assertEquals(0xfe, data[0].toInt() and 0xff)
        assertEquals(44, packet.messageId)
        assertEquals(4, packet.payload.size)
        assertEquals(4, packet.payload.leUInt16(0))
        assertEquals(255, packet.payload[2].toInt() and 0xff)
        assertEquals(190, packet.payload[3].toInt() and 0xff)
    }

    @Test
    fun missionRequestIntUsesMavlinkFieldOrder() {
        val builder = MavlinkMessageBuilder(systemId = 1, componentId = 1)

        val data = builder.missionRequestInt(sequence = 3, targetSystem = 255, targetComponent = 190)
        val packet = MavlinkParser.parse(data, length = data.size)

        assertNotNull(packet)
        assertEquals(0xfe, data[0].toInt() and 0xff)
        assertEquals(51, packet.messageId)
        assertEquals(4, packet.payload.size)
        assertEquals(3, packet.payload.leUInt16(0))
        assertEquals(255, packet.payload[2].toInt() and 0xff)
        assertEquals(190, packet.payload[3].toInt() and 0xff)
    }

    @Test
    fun vfrHudUsesMavlinkFieldOrder() {
        val builder = MavlinkMessageBuilder(systemId = 174, componentId = 1)
        val state = DroneState(
            groundSpeedMS = 4.5f,
            headingDegrees = 123,
            throttlePercent = 67u.toUByte(),
            altitudeMslMeters = 1807.25f,
            verticalSpeedMS = -1.5f,
        )

        val data = builder.vfrHud(state)
        val packet = MavlinkParser.parse(data, length = data.size)

        assertNotNull(packet)
        assertEquals(74, packet.messageId)
        assertEquals(4.5f, packet.payload.leFloat(0))
        assertEquals(4.5f, packet.payload.leFloat(4))
        assertEquals(123, packet.payload.leInt16(8))
        assertEquals(67, packet.payload.leUInt16(10))
        assertEquals(1807.25f, packet.payload.leFloat(12))
        assertEquals(-1.5f, packet.payload.leFloat(16))
    }

    @Test
    fun batteryStatusUsesMavlinkFieldOrder() {
        val builder = MavlinkMessageBuilder(systemId = 174, componentId = 1)
        val state = DroneState(
            batteryVoltageMv = 12_100u.toUShort(),
            batteryCurrentCa = 345,
            batteryRemainingPercent = 76,
        )

        val data = builder.batteryStatus(state)
        val packet = MavlinkParser.parse(data, length = data.size)

        assertNotNull(packet)
        assertEquals(147, packet.messageId)
        assertEquals(12_100, packet.payload.leUInt16(0))
        assertEquals(345, packet.payload.leInt16(20))
        assertEquals(-1, packet.payload.leInt32(22))
        assertEquals(-1, packet.payload.leInt32(26))
        assertEquals(76, packet.payload[30].toInt())
        assertEquals(0, packet.payload[31].toInt())
        assertEquals(0, packet.payload[32].toInt())
        assertEquals(3, packet.payload[33].toInt())
    }

    private fun ByteArray.leUInt64(offset: Int): Long {
        var value = 0L
        for (index in 0 until 8) {
            value = value or ((this[offset + index].toLong() and 0xffL) shl (8 * index))
        }
        return value
    }

    private fun ByteArray.leFloat(offset: Int): Float {
        return java.nio.ByteBuffer.wrap(this, offset, 4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .float
    }

    private fun ByteArray.leUInt16(offset: Int): Int {
        return (this[offset].toInt() and 0xff) or ((this[offset + 1].toInt() and 0xff) shl 8)
    }

    private fun ByteArray.leInt16(offset: Int): Int {
        return leUInt16(offset).toShort().toInt()
    }

    private fun ByteArray.leInt32(offset: Int): Int {
        return (this[offset].toInt() and 0xff) or
            ((this[offset + 1].toInt() and 0xff) shl 8) or
            ((this[offset + 2].toInt() and 0xff) shl 16) or
            (this[offset + 3].toInt() shl 24)
    }

    private companion object {
        const val MAV_PROTOCOL_CAPABILITY_MISSION_INT = 4L
        const val MAV_PROTOCOL_CAPABILITY_MAVLINK2 = 8192L
    }
}
