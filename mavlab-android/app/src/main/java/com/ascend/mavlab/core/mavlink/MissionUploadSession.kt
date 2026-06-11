package com.ascend.mavlab.core.mavlink

import com.ascend.mavlab.simulation.mission.MissionCommand
import com.ascend.mavlab.simulation.mission.MissionItem
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MissionUploadSession(
    val expectedCount: Int,
    val peer: UdpDestination,
    val startedAtMs: Long = System.currentTimeMillis(),
) {
    private val receivedItems = mutableMapOf<Int, MissionItem>()

    val receivedCount: Int get() = receivedItems.size
    val nextSequence: Int
        get() = (0 until expectedCount).firstOrNull { receivedItems[it] == null } ?: expectedCount
    val complete: Boolean get() = receivedItems.size == expectedCount
    val expired: Boolean get() = System.currentTimeMillis() - startedAtMs > UploadTimeoutMs

    fun receive(item: MissionItem): MissionUploadResult {
        if (item.sequence !in 0 until expectedCount) {
            return MissionUploadResult.Rejected("invalid seq ${item.sequence}")
        }
        receivedItems[item.sequence] = item
        return if (complete) {
            MissionUploadResult.Complete(receivedItems.values.sortedBy { it.sequence })
        } else {
            MissionUploadResult.RequestNext(nextSequence)
        }
    }

    companion object {
        const val MaxMissionItems = 100
        const val UploadTimeoutMs = 10_000L

        fun parseMissionCount(packet: MavlinkPacket, peer: UdpDestination): MissionUploadSession? {
            if (packet.payload.size < 4) return null
            val count = packet.payload.leUInt16(0)
            if (count !in 0..MaxMissionItems) return null
            return MissionUploadSession(
                expectedCount = count,
                peer = peer,
            )
        }

        fun parseMissionItemInt(packet: MavlinkPacket): MissionItem? {
            if (packet.payload.size < 37) return null
            val latitudeDeg = packet.payload.leInt32(16) / 1e7
            val longitudeDeg = packet.payload.leInt32(20) / 1e7
            return missionItem(
                sequence = packet.payload.leUInt16(28),
                commandId = packet.payload.leUInt16(30),
                latitudeDeg = latitudeDeg,
                longitudeDeg = longitudeDeg,
                altitudeAglMeters = packet.payload.leFloat(24),
                acceptanceRadiusMeters = packet.payload.leFloat(4),
                autocontinue = packet.payload[36].toInt() != 0,
            )
        }

        fun parseMissionItem(packet: MavlinkPacket): MissionItem? {
            if (packet.payload.size < 37) return null
            return missionItem(
                sequence = packet.payload.leUInt16(28),
                commandId = packet.payload.leUInt16(30),
                latitudeDeg = packet.payload.leFloat(16).toDouble(),
                longitudeDeg = packet.payload.leFloat(20).toDouble(),
                altitudeAglMeters = packet.payload.leFloat(24),
                acceptanceRadiusMeters = packet.payload.leFloat(4),
                autocontinue = packet.payload[36].toInt() != 0,
            )
        }

        private fun missionItem(
            sequence: Int,
            commandId: Int,
            latitudeDeg: Double,
            longitudeDeg: Double,
            altitudeAglMeters: Float,
            acceptanceRadiusMeters: Float,
            autocontinue: Boolean,
        ): MissionItem? {
            if (!latitudeDeg.isFinite() || !longitudeDeg.isFinite() || !altitudeAglMeters.isFinite()) {
                return null
            }
            return MissionItem(
                sequence = sequence,
                command = MissionCommand.fromMavCmdId(commandId),
                latitudeDeg = latitudeDeg,
                longitudeDeg = longitudeDeg,
                altitudeAglMeters = altitudeAglMeters.coerceAtLeast(0f),
                acceptanceRadiusMeters = acceptanceRadiusMeters
                    .takeIf { it.isFinite() && it > 0f }
                    ?: 2f,
                autocontinue = autocontinue,
            )
        }
    }
}

sealed interface MissionUploadResult {
    data class RequestNext(val sequence: Int) : MissionUploadResult
    data class Complete(val items: List<MissionItem>) : MissionUploadResult
    data class Rejected(val reason: String) : MissionUploadResult
}

private fun ByteArray.leUInt16(offset: Int): Int {
    return (this[offset].toInt() and 0xff) or ((this[offset + 1].toInt() and 0xff) shl 8)
}

private fun ByteArray.leInt32(offset: Int): Int {
    return (this[offset].toInt() and 0xff) or
        ((this[offset + 1].toInt() and 0xff) shl 8) or
        ((this[offset + 2].toInt() and 0xff) shl 16) or
        (this[offset + 3].toInt() shl 24)
}

private fun ByteArray.leFloat(offset: Int): Float {
    return ByteBuffer.wrap(this, offset, 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .float
}
