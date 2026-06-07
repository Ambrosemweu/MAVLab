package com.ascend.mavlab.core.mavlink

data class MavlinkPacket(
    val messageId: Int,
    val systemId: Int,
    val componentId: Int,
    val payload: ByteArray,
)

object MavlinkParser {
    fun parse(data: ByteArray, length: Int): MavlinkPacket? {
        if (length < 8) return null
        return when (data[0].toInt() and 0xff) {
            0xfd -> parseV2(data, length)
            0xfe -> parseV1(data, length)
            else -> null
        }
    }

    private fun parseV2(data: ByteArray, length: Int): MavlinkPacket? {
        val payloadLength = data[1].toInt() and 0xff
        val frameLength = 10 + payloadLength + 2
        if (length < frameLength) return null
        val messageId = (data[7].toInt() and 0xff) or
            ((data[8].toInt() and 0xff) shl 8) or
            ((data[9].toInt() and 0xff) shl 16)
        return MavlinkPacket(
            messageId = messageId,
            systemId = data[5].toInt() and 0xff,
            componentId = data[6].toInt() and 0xff,
            payload = data.copyOfRange(10, 10 + payloadLength),
        )
    }

    private fun parseV1(data: ByteArray, length: Int): MavlinkPacket? {
        val payloadLength = data[1].toInt() and 0xff
        val frameLength = 6 + payloadLength + 2
        if (length < frameLength) return null
        return MavlinkPacket(
            messageId = data[5].toInt() and 0xff,
            systemId = data[3].toInt() and 0xff,
            componentId = data[4].toInt() and 0xff,
            payload = data.copyOfRange(6, 6 + payloadLength),
        )
    }
}
