package com.ascend.mavlab.core.mavlink

import com.ascend.mavlab.simulation.engine.DroneState
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MavlinkParserTest {
    // Independent fixtures generated with pymavlink 2.4.50 common dialect, seq=0.
    private val heartbeat = hex("fd0900000001010000000000000002035d0303d605")
    private val v1Heartbeat = hex("fe09000101000000000002035d03034cc6")
    private val disarm = hex("fd20000000ffbe4c000000000000000000000000000000000000000000000000000000000000900101018916")
    private val speed = hex("fd23000000ffbe4900000000803f0000c842000080bf000000000000000000000000000000000300b2000101026d97")

    @Test
    fun outboundHeartbeatMatchesGeneratedLibraryByteForByte() {
        assertContentEquals(heartbeat, MavlinkMessageBuilder(1, 1).heartbeat(DroneState()))
    }

    @Test
    fun legacyHeartbeatStillDecodesWithValidatedChecksum() {
        assertEquals(0, assertNotNull(MavlinkParser.parse(v1Heartbeat, v1Heartbeat.size)).messageId)
    }

    @Test
    fun compressedDisarmRestoresZeroConfirmationByte() {
        val packet = assertNotNull(MavlinkParser.parse(disarm, disarm.size))
        assertEquals(76, packet.messageId)
        assertEquals(33, packet.payload.size)
        assertEquals(0, packet.payload[32].toInt())
        assertEquals(1, packet.payload[30].toInt())
    }

    @Test
    fun compressedSpeedItemRestoresZeroCurrentAndAutocontinue() {
        val packet = assertNotNull(MavlinkParser.parse(speed, speed.size))
        assertEquals(37, packet.payload.size)
        val item = assertNotNull(MissionUploadSession.parseMissionItemInt(packet))
        assertEquals(3, item.sequence)
        assertEquals(100f, item.speedMetersPerSecond)
        assertEquals(false, item.autocontinue)
    }

    @Test
    fun everyFrameInMixedVersionDatagramIsDecodedInOrder() {
        val data = v1Heartbeat + disarm + speed + heartbeat
        assertEquals(listOf(0, 76, 73, 0), MavlinkParser.parseAll(data).map { it.messageId })
    }

    @Test
    fun indexParameterReadRestoresOmittedNameBytes() {
        val bytes = hex("fd04000000ffbe140000000001012c2f")
        val packet = assertNotNull(MavlinkParser.parse(bytes, bytes.size))
        assertEquals(20, packet.payload.size)
        assertTrue(packet.payload.drop(4).all { it == 0.toByte() })
    }

    @Test
    fun broadcastClearRestoresBothZeroTargets() {
        val bytes = hex("fd01000000ffbe2d0000006ca7")
        val packet = assertNotNull(MavlinkParser.parse(bytes, bytes.size))
        assertContentEquals(byteArrayOf(0, 0), packet.payload)
    }

    @Test
    fun badChecksumAndIncompleteFramesCannotBecomeCommands() {
        val corrupted = disarm.copyOf().apply { this[10] = 1 }
        assertNull(MavlinkParser.parse(corrupted, corrupted.size))
        assertEquals(listOf(0), MavlinkParser.parseAll(corrupted + heartbeat).map { it.messageId })
        for (length in 0 until disarm.size) assertNull(MavlinkParser.parse(disarm, length))
        assertNull(MavlinkParser.parse(disarm, disarm.size + 1))
    }

    @Test
    fun signedOrUnknownIncompatibilityFramesAreNotExecuted() {
        for (flag in listOf(1, 2)) {
            val header = disarm.copyOf().apply { this[2] = flag.toByte() }
            val crc = MavlinkWire.crc(header, 1, header.size - 2, 152)
            header[header.size - 2] = crc.toByte()
            header[header.size - 1] = (crc ushr 8).toByte()
            val flagged = if (flag == 1) header + ByteArray(13) else header
            assertEquals(listOf(0), MavlinkParser.parseAll(flagged + heartbeat).map { it.messageId })
        }
    }

    @Test
    fun unknown24BitIdIsNotMistakenForItsLowByte() {
        val unknown = heartbeat.copyOf().apply { this[8] = 1 }
        assertEquals(listOf(0), MavlinkParser.parseAll(unknown + heartbeat).map { it.messageId })
    }

    @Test
    fun sequenceWrapsAndCommandAckTargetsRequester() {
        val builder = MavlinkMessageBuilder(1, 1)
        repeat(256) { assertEquals(it, builder.heartbeat(DroneState())[4].toInt() and 0xff) }
        val bytes = builder.commandAck(512, 3, 255, 190)
        assertEquals(0, bytes[4].toInt())
        val packet = assertNotNull(MavlinkParser.parse(bytes, bytes.size))
        assertEquals(255, packet.payload[8].toInt() and 0xff)
        assertEquals(190, packet.payload[9].toInt() and 0xff)
        assertEquals(3, packet.payload[2].toInt())
    }

    @Test
    fun extendedStateReflectsEngineAltitude() {
        val builder = MavlinkMessageBuilder(1, 1)
        for ((state, landed) in listOf(DroneState() to 1, DroneState(armed = true, altitudeAglMeters = 5f) to 2)) {
            val bytes = builder.extendedSysState(state)
            val packet = assertNotNull(MavlinkParser.parse(bytes, bytes.size))
            assertEquals(245, packet.messageId)
            assertEquals(landed, packet.payload[1].toInt())
        }
    }

    private fun hex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
