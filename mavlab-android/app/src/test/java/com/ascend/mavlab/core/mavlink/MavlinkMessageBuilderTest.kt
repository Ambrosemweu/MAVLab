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
        assertEquals(0f, packet.payload.leFloat(0))
        assertEquals(4.5f, packet.payload.leFloat(4))
        assertEquals(1807.25f, packet.payload.leFloat(8))
        assertEquals(-1.5f, packet.payload.leFloat(12))
        assertEquals(123, packet.payload.leInt16(16))
        assertEquals(67, packet.payload.leUInt16(18))
    }

    @Test
    fun initialGlobalPositionReportsZeroRelativeAltitudeForQgcTakeoff() {
        val builder = MavlinkMessageBuilder(systemId = 174, componentId = 1)

        val data = builder.globalPosition(DroneState())
        val packet = MavlinkParser.parse(data, length = data.size)

        assertNotNull(packet)
        assertEquals(33, packet.messageId)
        assertEquals(1805_000, packet.payload.leInt32(12))
        assertEquals(0, packet.payload.leInt32(16))
    }

    @Test
    fun globalPositionUsesVelocityComponentsNotAltitudeOrSpeedMagnitude() {
        val builder = MavlinkMessageBuilder(systemId = 174, componentId = 1)
        val state = DroneState(
            altitudeMslMeters = 1805f,
            northVelocityMS = 3.5f,
            eastVelocityMS = -1.25f,
            verticalSpeedMS = 0.5f,
            groundSpeedMS = 1805f,
        )

        val data = builder.globalPosition(state)
        val packet = MavlinkParser.parse(data, length = data.size)

        assertNotNull(packet)
        assertEquals(33, packet.messageId)
        assertEquals(350, packet.payload.leInt16(20))
        assertEquals(-125, packet.payload.leInt16(22))
        assertEquals(-50, packet.payload.leInt16(24))
    }

    @Test
    fun qgcSpeedTelemetryIsClampedAndDoesNotMirrorMslAltitude() {
        val builder = MavlinkMessageBuilder(systemId = 174, componentId = 1)
        val state = DroneState(
            altitudeMslMeters = 1805f,
            groundSpeedMS = 1805f,
        )

        val vfrHud = assertNotNull(MavlinkParser.parse(builder.vfrHud(state), length = 28))
        val gpsRaw = assertNotNull(MavlinkParser.parse(builder.gpsRaw(state), length = 38))

        assertEquals(0f, vfrHud.payload.leFloat(0))
        assertEquals(80f, vfrHud.payload.leFloat(4))
        assertEquals(0f, vfrHud.payload.leFloat(12))
        assertEquals(8000, gpsRaw.payload.leUInt16(24))
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

    @Test
    fun heartbeatCrcIsCorrect() {
        val builder = MavlinkMessageBuilder(systemId = 1, componentId = 1)
        val state = DroneState() // default armed=false, mode=STABILIZE
        val data = builder.heartbeat(state)

        assertEquals(17, data.size) // header (6) + payload (9) + crc (2)
        assertEquals(0xfe, data[0].toInt() and 0xff) // magic
        assertEquals(9, data[1].toInt() and 0xff) // payload length
        assertEquals(0, data[2].toInt() and 0xff) // sequence
        assertEquals(1, data[3].toInt() and 0xff) // system ID
        assertEquals(1, data[4].toInt() and 0xff) // component ID
        assertEquals(0, data[5].toInt() and 0xff) // message ID
        
        // Expected payload: [0, 0, 0, 0, 2, 3, 93, 3, 3]
        assertEquals(0, data[6].toInt() and 0xff)
        assertEquals(0, data[7].toInt() and 0xff)
        assertEquals(0, data[8].toInt() and 0xff)
        assertEquals(0, data[9].toInt() and 0xff)
        assertEquals(2, data[10].toInt() and 0xff)
        assertEquals(3, data[11].toInt() and 0xff)
        assertEquals(93, data[12].toInt() and 0xff)
        assertEquals(3, data[13].toInt() and 0xff)
        assertEquals(3, data[14].toInt() and 0xff)

        // CRC low byte and high byte (expected CRC: 0xc64c)
        assertEquals(0x4c, data[15].toInt() and 0xff)
        assertEquals(0xc6, data[16].toInt() and 0xff)
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
