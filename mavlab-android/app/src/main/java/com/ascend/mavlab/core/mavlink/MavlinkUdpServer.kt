package com.ascend.mavlab.core.mavlink

import android.content.Context
import com.ascend.mavlab.simulation.engine.FlightMode
import com.ascend.mavlab.simulation.engine.PhysicsSimulationEngine
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MavlinkUdpServer(
    private val simLoop: PhysicsSimulationEngine,
    private val config: MavlinkSocketConfig = MavlinkSocketConfig(systemId = 1),
) : MavlinkEndpoint {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val builder = MavlinkMessageBuilder(config.systemId, config.componentId)
    private val mutableStatus = MutableStateFlow("Stopped")
    val status: StateFlow<String> = mutableStatus.asStateFlow()

    private var socket: DatagramSocket? = null
    private var telemetryJob: Job? = null
    private var receiveJob: Job? = null
    private var lastPeer: UdpDestination? = null
    private var lastReachedSequenceSent: Int? = null
    private var missionUploadSession: MissionUploadSession? = null

    fun start(context: Context) {
        if (socket != null) return
        val openedSocket = openSocket()
        openedSocket.broadcast = true
        openedSocket.soTimeout = 1000
        socket = openedSocket
        mutableStatus.value = "Running on UDP ${openedSocket.localPort}"

        val destinations = destinations(context)
        telemetryJob = scope.launch { telemetryLoop(destinations) }
        receiveJob = scope.launch { receiveLoop() }
    }

    override suspend fun start() {
        error("Use start(context) so broadcast destinations can be discovered.")
    }

    override suspend fun stop() {
        stopNow()
    }

    fun stopNow() {
        telemetryJob?.cancel()
        receiveJob?.cancel()
        socket?.close()
        socket = null
        mutableStatus.value = "Stopped"
    }

    private fun openSocket(): DatagramSocket {
        return try {
            DatagramSocket(config.localBindPort)
        } catch (_: Exception) {
            DatagramSocket(0)
        }
    }

    private suspend fun telemetryLoop(destinations: List<UdpDestination>) {
        while (scope.coroutineContext.isActive) {
            sendTelemetryBurst(destinations)
            delay(200)
        }
    }

    private fun sendTelemetryBurst(destinations: List<UdpDestination>) {
        val state = simLoop.state.value
        val missionProgress = simLoop.missionProgress.value
        val messages = buildList {
            add(
                builder.heartbeat(state),
            )
            add(builder.attitude(state))
            add(builder.globalPosition(state))
            add(builder.gpsRaw(state))
            add(builder.vfrHud(state))
            add(builder.sysStatus(state))
            add(builder.batteryStatus(state))
            if (missionProgress.loaded) {
                add(builder.missionCurrent(missionProgress.currentIndex.coerceAtMost(missionProgress.items.lastIndex.coerceAtLeast(0))))
                val reached = missionProgress.lastReachedSequence
                if (reached != null && reached != lastReachedSequenceSent) {
                    add(builder.missionItemReached(reached))
                    lastReachedSequenceSent = reached
                }
            }
        }
        messages.forEach { data -> sendToDestinations(data, destinations) }
    }

    private fun sendToDestinations(data: ByteArray, destinations: List<UdpDestination>) {
        val dynamicDestinations = buildList {
            addAll(destinations)
            lastPeer?.let { add(it) }
        }.distinct()

        dynamicDestinations.forEach { destination ->
            send(data, destination)
        }
    }

    private fun send(data: ByteArray, destination: UdpDestination) {
        val currentSocket = socket ?: return
        try {
            val packet = DatagramPacket(
                data,
                data.size,
                InetAddress.getByName(destination.host),
                destination.port,
            )
            currentSocket.send(packet)
        } catch (error: Exception) {
            mutableStatus.value = "Send failed: ${error.message}"
        }
    }

    private suspend fun receiveLoop() {
        val buffer = ByteArray(512)
        while (scope.coroutineContext.isActive) {
            val currentSocket = socket ?: return
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                currentSocket.receive(packet)
                val host = packet.address.hostAddress ?: return
                lastPeer = UdpDestination(host = host, port = packet.port)
                handleInbound(packet.data.copyOf(packet.length), packet.length, lastPeer)
            } catch (_: SocketTimeoutException) {
                continue
            } catch (error: Exception) {
                if (scope.coroutineContext.isActive) {
                    mutableStatus.value = "Receive failed: ${error.message}"
                }
            }
        }
    }

    private fun handleInbound(data: ByteArray, length: Int, peer: UdpDestination?) {
        val packet = MavlinkParser.parse(data, length) ?: return
        logInbound(packet, peer, length, "received")
        when (packet.messageId) {
            11 -> handleSetMode(packet, peer)
            21 -> sendParams(peer)
            23 -> ack(0, MAV_RESULT_ACCEPTED, peer, "PARAM_SET")
            39 -> handleMissionItemUpload(packet, peer, legacy = true, length = length)
            40 -> sendRequestedMissionItem(packet, peer, legacy = true)
            41 -> handleMissionSetCurrent(packet, peer, length)
            43 -> sendMissionCount(packet, peer)
            44 -> handleMissionCountUpload(packet, peer, length)
            45 -> handleMissionClearAll(packet, peer, length)
            51 -> sendRequestedMissionItem(packet, peer)
            73 -> handleMissionItemUpload(packet, peer, legacy = false, length = length)
            76 -> handleCommandLong(packet, peer)
            else -> logInbound(packet, peer, length, "unsupported")
        }
    }

    private fun handleSetMode(packet: MavlinkPacket, peer: UdpDestination?) {
        if (packet.payload.size < 6) return
        val customMode = packet.payload.leUInt32(0)
        val mode = FlightMode.fromCustomMode(customMode)
        simLoop.setMode(mode)
        simLoop.noteInbound("SET_MODE ${mode.displayName}")
        ack(0, MAV_RESULT_ACCEPTED, peer, "SET_MODE")
    }

    private fun handleCommandLong(packet: MavlinkPacket, peer: UdpDestination?) {
        if (packet.payload.size < 30) return
        val command = packet.payload.leUInt16(28)
        simLoop.noteInbound("COMMAND_LONG $command")
        when (command) {
            MAV_CMD_COMPONENT_ARM_DISARM -> {
                val arm = packet.payload.leFloat(0) >= 0.5f
                simLoop.setArmed(arm)
                ack(command, MAV_RESULT_ACCEPTED, peer, if (arm) "ARM" else "DISARM")
            }
            MAV_CMD_NAV_TAKEOFF -> {
                val requestedAltitude = packet.payload.leFloat(24)
                    .takeIf { it.isFinite() && it > 0f }
                    ?: 10f
                simLoop.takeoff(requestedAltitude)
                ack(command, MAV_RESULT_ACCEPTED, peer, "TAKEOFF")
            }
            MAV_CMD_NAV_LAND -> {
                simLoop.land()
                ack(command, MAV_RESULT_ACCEPTED, peer, "LAND")
            }
            MAV_CMD_SET_MESSAGE_INTERVAL -> {
                ack(command, MAV_RESULT_ACCEPTED, peer, "SET_MESSAGE_INTERVAL")
            }
            MAV_CMD_MISSION_START -> {
                val accepted = simLoop.missionProgress.value.loaded
                if (accepted) {
                    simLoop.setMode(FlightMode.AUTO)
                }
                ack(
                    command,
                    if (accepted) MAV_RESULT_ACCEPTED else MAV_RESULT_DENIED,
                    peer,
                    if (accepted) "MISSION_START" else "MISSION_START NO MISSION",
                )
            }
            else -> {
                ack(command, MAV_RESULT_UNSUPPORTED, peer, "UNSUPPORTED $command")
            }
        }
    }

    private fun sendParams(peer: UdpDestination?) {
        simLoop.noteInbound("PARAM_REQUEST_LIST")
        val destination = peer ?: lastPeer ?: return
        val params = listOf(
            "SYSID_THISMAV" to config.systemId.toFloat(),
            "MAV_TYPE" to 2f,
        )
        params.forEachIndexed { index, param ->
            send(builder.paramValue(param.first, param.second, index, params.size), destination)
        }
    }

    private fun sendMissionCount(packet: MavlinkPacket, peer: UdpDestination?) {
        simLoop.noteInbound("MISSION_REQUEST_LIST")
        val destination = peer ?: lastPeer ?: return
        send(
            builder.missionCount(
                count = simLoop.missionProgress.value.items.size,
                targetSystem = packet.systemId,
                targetComponent = packet.componentId,
            ),
            destination,
        )
    }

    private fun sendRequestedMissionItem(
        packet: MavlinkPacket,
        peer: UdpDestination?,
        legacy: Boolean = false,
    ) {
        if (packet.payload.size < 4) return
        val sequence = packet.payload.leUInt16(2)
        val progress = simLoop.missionProgress.value
        val item = progress.items.getOrNull(sequence) ?: return
        val destination = peer ?: lastPeer ?: return
        simLoop.noteInbound("${if (legacy) "MISSION_REQUEST" else "MISSION_REQUEST_INT"} $sequence")
        val data = if (legacy) {
            builder.missionItem(item, progress.currentIndex, packet.systemId, packet.componentId)
        } else {
            builder.missionItemInt(item, progress.currentIndex, packet.systemId, packet.componentId)
        }
        send(data, destination)
    }

    private fun handleMissionCountUpload(packet: MavlinkPacket, peer: UdpDestination?, length: Int) {
        val destination = peer ?: lastPeer ?: return
        val session = MissionUploadSession.parseMissionCount(packet, destination)
        if (session == null) {
            sendMissionAck(MAV_MISSION_ERROR, packet, destination, "MISSION_COUNT rejected")
            logInbound(packet, peer, length, "rejected invalid-count")
            return
        }
        if (session.expectedCount == 0) {
            missionUploadSession = null
            simLoop.clearMission()
            sendMissionAck(MAV_MISSION_ACCEPTED, packet, destination, "MISSION_CLEAR_EMPTY")
            logInbound(packet, peer, length, "accepted clear-empty")
            return
        }
        missionUploadSession = session
        send(
            builder.missionRequestInt(
                sequence = 0,
                targetSystem = packet.systemId,
                targetComponent = packet.componentId,
            ),
            destination,
        )
        logInbound(packet, peer, length, "accepted upload-count=${session.expectedCount} request=0")
    }

    private fun handleMissionItemUpload(
        packet: MavlinkPacket,
        peer: UdpDestination?,
        legacy: Boolean,
        length: Int,
    ) {
        val destination = peer ?: lastPeer ?: return
        val session = missionUploadSession
        if (session == null) {
            sendMissionAck(MAV_MISSION_ERROR, packet, destination, "MISSION_ITEM no session")
            logInbound(packet, peer, length, "rejected no-session")
            return
        }
        if (session.expired) {
            missionUploadSession = null
            sendMissionAck(MAV_MISSION_ERROR, packet, destination, "MISSION_UPLOAD timeout")
            logInbound(packet, peer, length, "rejected timeout")
            return
        }
        val item = if (legacy) {
            MissionUploadSession.parseMissionItem(packet)
        } else {
            MissionUploadSession.parseMissionItemInt(packet)
        }
        if (item == null) {
            missionUploadSession = null
            sendMissionAck(MAV_MISSION_INVALID, packet, destination, "MISSION_ITEM invalid")
            logInbound(packet, peer, length, "rejected invalid-item")
            return
        }
        when (val result = session.receive(item)) {
            is MissionUploadResult.RequestNext -> {
                val request = if (legacy) {
                    builder.missionRequest(result.sequence, packet.systemId, packet.componentId)
                } else {
                    builder.missionRequestInt(result.sequence, packet.systemId, packet.componentId)
                }
                send(request, destination)
                logInbound(packet, peer, length, "accepted item=${item.sequence} request=${result.sequence}")
            }
            is MissionUploadResult.Complete -> {
                missionUploadSession = null
                simLoop.loadMission(result.items)
                lastReachedSequenceSent = null
                sendMissionAck(MAV_MISSION_ACCEPTED, packet, destination, "MISSION_UPLOAD ${result.items.size}")
                logInbound(packet, peer, length, "accepted complete=${result.items.size}")
            }
            is MissionUploadResult.Rejected -> {
                missionUploadSession = null
                sendMissionAck(MAV_MISSION_INVALID_SEQUENCE, packet, destination, "MISSION_ITEM ${result.reason}")
                logInbound(packet, peer, length, "rejected ${result.reason}")
            }
        }
    }

    private fun handleMissionClearAll(packet: MavlinkPacket, peer: UdpDestination?, length: Int) {
        val destination = peer ?: lastPeer ?: return
        missionUploadSession = null
        simLoop.clearMission()
        lastReachedSequenceSent = null
        sendMissionAck(MAV_MISSION_ACCEPTED, packet, destination, "MISSION_CLEAR_ALL")
        logInbound(packet, peer, length, "accepted clear-all")
    }

    private fun handleMissionSetCurrent(packet: MavlinkPacket, peer: UdpDestination?, length: Int) {
        if (packet.payload.size < 4) return
        val destination = peer ?: lastPeer ?: return
        val sequence = packet.payload.leUInt16(2)
        val accepted = simLoop.setMissionCurrent(sequence)
        sendMissionAck(
            if (accepted) MAV_MISSION_ACCEPTED else MAV_MISSION_INVALID_SEQUENCE,
            packet,
            destination,
            if (accepted) "MISSION_SET_CURRENT $sequence" else "MISSION_SET_CURRENT INVALID $sequence",
        )
        logInbound(packet, peer, length, if (accepted) "accepted current=$sequence" else "rejected current=$sequence")
    }

    private fun sendMissionAck(type: Int, packet: MavlinkPacket, destination: UdpDestination, label: String) {
        simLoop.noteAck(label)
        send(builder.missionAck(type, packet.systemId, packet.componentId), destination)
    }

    private fun ack(command: Int, result: Int, peer: UdpDestination?, label: String) {
        simLoop.noteAck(label)
        val destination = peer ?: lastPeer ?: return
        send(builder.commandAck(command, result), destination)
    }

    private fun logInbound(packet: MavlinkPacket, peer: UdpDestination?, length: Int, result: String) {
        val peerLabel = peer?.let { "${it.host}:${it.port}" } ?: "unknown"
        simLoop.noteInbound("rx id=${packet.messageId} from=$peerLabel len=$length $result")
    }

    private fun destinations(context: Context): List<UdpDestination> {
        return buildList {
            add(UdpDestination(config.sameDeviceHost, config.sameDeviceQgcPort))
            addAll(config.lanDestinations)
            addAll(networkBroadcasts())
            add(UdpDestination("255.255.255.255", 14550))
        }.distinct()
    }

    private fun networkBroadcasts(): List<UdpDestination> {
        return runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { network ->
                    network.interfaceAddresses.mapNotNull { address ->
                        val broadcast = address.broadcast
                        if (broadcast is Inet4Address) {
                            UdpDestination(broadcast.hostAddress ?: return@mapNotNull null, 14550)
                        } else {
                            null
                        }
                    }
                }
        }.getOrDefault(emptyList())
    }

    private fun ByteArray.leUInt16(offset: Int): Int {
        return (this[offset].toInt() and 0xff) or ((this[offset + 1].toInt() and 0xff) shl 8)
    }

    private fun ByteArray.leUInt32(offset: Int): UInt {
        val value = (this[offset].toInt() and 0xff) or
            ((this[offset + 1].toInt() and 0xff) shl 8) or
            ((this[offset + 2].toInt() and 0xff) shl 16) or
            ((this[offset + 3].toInt() and 0xff) shl 24)
        return value.toUInt()
    }

    private fun ByteArray.leFloat(offset: Int): Float {
        return ByteBuffer.wrap(this, offset, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .float
    }

    private companion object {
        const val MAV_RESULT_ACCEPTED = 0
        const val MAV_RESULT_DENIED = 2
        const val MAV_RESULT_UNSUPPORTED = 3
        const val MAV_MISSION_ACCEPTED = 0
        const val MAV_MISSION_ERROR = 1
        const val MAV_MISSION_INVALID_SEQUENCE = 13
        const val MAV_MISSION_INVALID = 5
        const val MAV_CMD_NAV_TAKEOFF = 22
        const val MAV_CMD_NAV_LAND = 21
        const val MAV_CMD_MISSION_START = 300
        const val MAV_CMD_COMPONENT_ARM_DISARM = 400
        const val MAV_CMD_SET_MESSAGE_INTERVAL = 511
    }
}
