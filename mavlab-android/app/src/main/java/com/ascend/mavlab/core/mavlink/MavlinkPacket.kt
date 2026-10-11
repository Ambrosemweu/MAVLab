package com.ascend.mavlab.core.mavlink

data class MavlinkPacket(
    val messageId: Int,
    val systemId: Int,
    val componentId: Int,
    val payload: ByteArray,
)

object MavlinkParser {
    fun parse(data: ByteArray, length: Int): MavlinkPacket? {
        return parseAll(data, length).firstOrNull()
    }

    /** A UDP datagram may carry multiple frames. Never execute unchecked/signed commands. */
    fun parseAll(data: ByteArray, length: Int = data.size): List<MavlinkPacket> = buildList {
        if (length !in 0..data.size) return@buildList
        var offset = 0
        while (offset + 8 <= length) {
            val magic = data[offset].toInt() and 0xff
            if (magic != 0xfd && magic != 0xfe) {
                offset++
                continue
            }
            val v2 = magic == 0xfd
            val header = if (v2) 10 else 6
            if (offset + header + 2 > length) break
            val size = data[offset + 1].toInt() and 0xff
            val flags = if (v2) data[offset + 2].toInt() and 0xff else 0
            val end = offset + header + size
            val frameEnd = end + 2 + if (flags and 1 != 0) 13 else 0
            if (frameEnd > length) break
            val idOffset = offset + if (v2) 7 else 5
            val id = (data[idOffset].toInt() and 0xff) or
                (if (v2) ((data[idOffset + 1].toInt() and 0xff) shl 8) or
                    ((data[idOffset + 2].toInt() and 0xff) shl 16) else 0)
            val definition = MavlinkWire.definitions[id]
            val crc = (data[end].toInt() and 0xff) or ((data[end + 1].toInt() and 0xff) shl 8)
            if (flags == 0 && definition != null &&
                (v2 || size == definition.baseLength) &&
                crc == MavlinkWire.crc(data, offset + 1, end, definition.crcExtra)
            ) {
                add(MavlinkPacket(
                    messageId = id,
                    systemId = data[offset + if (v2) 5 else 3].toInt() and 0xff,
                    componentId = data[offset + if (v2) 6 else 4].toInt() and 0xff,
                    // Restore omitted zero bytes before field-level validation/target checks.
                    payload = data.copyOfRange(offset + header, end).copyOf(maxOf(size, definition.baseLength)),
                ))
            }
            offset = frameEnd
        }
    }
}
