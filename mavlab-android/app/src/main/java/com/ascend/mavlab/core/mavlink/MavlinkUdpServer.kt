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
        when (packet.messageId) {
            11 -> handleSetMode(packet, peer)
            21 -> sendParams(peer)
            23 -> ack(0, MAV_RESULT_ACCEPTED, peer, "PARAM_SET")
            43 -> sendMissionCount(peer)
            51 -> sendRequestedMissionItem(packet, peer)
            76 -> handleCommandLong(packet, peer)
            else -> simLoop.noteInbound("msg ${packet.messageId}")
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

    private fun sendMissionCount(peer: UdpDestination?) {
        simLoop.noteInbound("MISSION_REQUEST_LIST")
        val destination = peer ?: lastPeer ?: return
        send(builder.missionCount(simLoop.missionProgress.value.items.size), destination)
    }

    private fun sendRequestedMissionItem(packet: MavlinkPacket, peer: UdpDestination?) {
        if (packet.payload.size < 4) return
        val sequence = packet.payload.leUInt16(2)
        val progress = simLoop.missionProgress.value
        val item = progress.items.getOrNull(sequence) ?: return
        val destination = peer ?: lastPeer ?: return
        simLoop.noteInbound("MISSION_REQUEST_INT $sequence")
        send(builder.missionItemInt(item, progress.currentIndex), destination)
    }

    private fun ack(command: Int, result: Int, peer: UdpDestination?, label: String) {
        simLoop.noteAck(label)
        val destination = peer ?: lastPeer ?: return
        send(builder.commandAck(command, result), destination)
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
        const val MAV_RESULT_UNSUPPORTED = 3
        const val MAV_CMD_NAV_TAKEOFF = 22
        const val MAV_CMD_NAV_LAND = 21
        const val MAV_CMD_COMPONENT_ARM_DISARM = 400
        const val MAV_CMD_SET_MESSAGE_INTERVAL = 511
    }
}
