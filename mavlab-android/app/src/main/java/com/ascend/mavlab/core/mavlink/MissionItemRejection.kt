package com.ascend.mavlab.core.mavlink

import com.ascend.mavlab.simulation.mission.MissionCommand

internal data class MissionItemRejection(val ack: Int, val reason: String)

/** Values follow MAVLink MAV_FRAME and MAV_MISSION_RESULT; reject unsupported units before loading. */
internal fun missionItemRejection(packet: MavlinkPacket): MissionItemRejection? {
    if (packet.payload.size < 37) return MissionItemRejection(5, "Invalid mission item: truncated payload")
    fun unsignedShort(offset: Int) = (packet.payload[offset].toInt() and 0xff) or ((packet.payload[offset + 1].toInt() and 0xff) shl 8)
    val sequence = unsignedShort(28)
    val commandId = unsignedShort(30)
    val command = MissionCommand.entries.firstOrNull { it.mavCmdId == commandId }
        ?: return MissionItemRejection(3, "Unsupported command $commandId at sequence $sequence")
    val frame = packet.payload[34].toInt() and 0xff
    val frameValid = frame in listOf(0, 3, 5, 6) || frame == 2 && (command == MissionCommand.CHANGE_SPEED || command == MissionCommand.RTL)
    return if (frameValid) null else MissionItemRejection(2, "Unsupported frame $frame for command $commandId at sequence $sequence")
}
