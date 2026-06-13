package com.ascend.mavlab.core.mavlink

import com.ascend.mavlab.simulation.mission.MissionCommand
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class MissionUploadSessionTest {
    @Test
    fun missionCountCreatesUploadSession() {
        val packet = MavlinkPacket(
            messageId = 44,
            systemId = 255,
            componentId = 190,
            payload = missionCountPayload(count = 4),
        )

        val session = MissionUploadSession.parseMissionCount(packet, UdpDestination("127.0.0.1"))

        assertNotNull(session)
        assertEquals(4, session.expectedCount)
        assertEquals(0, session.nextSequence)
    }

    @Test
    fun missionCountUsesMavlinkFieldOrder() {
        val packet = MavlinkPacket(
            messageId = 44,
            systemId = 255,
            componentId = 190,
            payload = littleEndian(4)
                .putU16(7)
                .putU8(1)
                .putU8(1)
                .array(),
        )

        val session = MissionUploadSession.parseMissionCount(packet, UdpDestination("127.0.0.1"))

        assertNotNull(session)
        assertEquals(7, session.expectedCount)
    }

    @Test
    fun missionItemIntCompletesWhenAllSequencesReceived() {
        val session = MissionUploadSession(expectedCount = 2, peer = UdpDestination("127.0.0.1"))

        val first = MissionUploadSession.parseMissionItemInt(missionItemIntPacket(sequence = 0))
        val second = MissionUploadSession.parseMissionItemInt(missionItemIntPacket(sequence = 1))

        val firstResult = session.receive(assertNotNull(first))
        val secondResult = session.receive(assertNotNull(second))

        assertIs<MissionUploadResult.RequestNext>(firstResult)
        assertEquals(1, firstResult.sequence)
        val complete = assertIs<MissionUploadResult.Complete>(secondResult)
        assertEquals(2, complete.items.size)
        assertEquals(MissionCommand.WAYPOINT, complete.items[0].command)
        assertEquals(10f, complete.items[0].altitudeAglMeters)
    }

    @Test
    fun uploadRejectsSequenceOutsideAnnouncedCount() {
        val session = MissionUploadSession(expectedCount = 1, peer = UdpDestination("127.0.0.1"))
        val item = assertNotNull(MissionUploadSession.parseMissionItemInt(missionItemIntPacket(sequence = 2)))

        val result = session.receive(item)

        assertIs<MissionUploadResult.Rejected>(result)
    }

    @Test
    fun legacyMissionItemParsesCoordinatesAndCommand() {
        val item = MissionUploadSession.parseMissionItem(legacyMissionItemPacket(sequence = 0))

        assertNotNull(item)
        assertEquals(MissionCommand.TAKEOFF, item.command)
        assertEquals(-1.2921f, item.latitudeDeg.toFloat())
        assertEquals(36.8219f, item.longitudeDeg.toFloat())
        assertEquals(12f, item.altitudeAglMeters)
    }

    @Test
    fun missionItemIntParsesChangeSpeedCommand() {
        val item = MissionUploadSession.parseMissionItemInt(
            missionItemIntPacket(
                sequence = 1,
                commandId = 178,
                param2 = 7.5f,
                latitudeDeg = 0.0,
                longitudeDeg = 0.0,
                altitudeMeters = 0f,
            ),
        )

        assertNotNull(item)
        assertEquals(MissionCommand.CHANGE_SPEED, item.command)
        assertEquals(7.5f, item.speedMetersPerSecond)
    }

    @Test
    fun staleUploadSessionExpires() {
        val session = MissionUploadSession(
            expectedCount = 1,
            peer = UdpDestination("127.0.0.1"),
            startedAtMs = System.currentTimeMillis() - MissionUploadSession.UploadTimeoutMs - 1L,
        )

        assertEquals(true, session.expired)
    }

    private fun missionCountPayload(count: Int): ByteArray {
        return littleEndian(4)
            .putU16(count)
            .putU8(1)
            .putU8(1)
            .array()
    }

    private fun missionItemIntPacket(
        sequence: Int,
        commandId: Int = 16,
        param2: Float = 2f,
        latitudeDeg: Double = -1.2921,
        longitudeDeg: Double = 36.8219,
        altitudeMeters: Float = 10f,
    ): MavlinkPacket {
        val payload = littleEndian(38)
            .putFloat(0f)
            .putFloat(param2)
            .putFloat(0f)
            .putFloat(0f)
            .putInt((latitudeDeg * 1e7).toInt())
            .putInt((longitudeDeg * 1e7).toInt())
            .putFloat(altitudeMeters)
            .putU16(sequence)
            .putU16(commandId)
            .putU8(1)
            .putU8(1)
            .putU8(6)
            .putU8(if (sequence == 0) 1 else 0)
            .putU8(1)
            .putU8(0)
            .array()
        return MavlinkPacket(
            messageId = 73,
            systemId = 255,
            componentId = 190,
            payload = payload,
        )
    }

    private fun legacyMissionItemPacket(sequence: Int): MavlinkPacket {
        val payload = littleEndian(37)
            .putFloat(0f)
            .putFloat(3f)
            .putFloat(0f)
            .putFloat(0f)
            .putFloat(-1.2921f)
            .putFloat(36.8219f)
            .putFloat(12f)
            .putU16(sequence)
            .putU16(22)
            .putU8(1)
            .putU8(1)
            .putU8(3)
            .putU8(1)
            .putU8(1)
            .array()
        return MavlinkPacket(
            messageId = 39,
            systemId = 255,
            componentId = 190,
            payload = payload,
        )
    }

    private fun littleEndian(size: Int): ByteBuffer {
        return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
    }

    private fun ByteBuffer.putU8(value: Int): ByteBuffer = put((value and 0xff).toByte())
    private fun ByteBuffer.putU16(value: Int): ByteBuffer = putShort((value and 0xffff).toShort())
}
