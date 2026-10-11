package com.ascend.mavlab.core.mavlink

import com.ascend.mavlab.simulation.engine.*
import com.ascend.mavlab.simulation.safety.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.*

class SafetyMissionTransferTest {
    private val engine = PhysicsSimulationEngine()
    private val vehicle = MavlinkMessageBuilder(1, 1)
    private val sender = MavlinkMessageBuilder(255, 190)
    private val peer = UdpDestination("127.0.0.1", 15550)
    private val responses = mutableListOf<Pair<MavlinkPacket, UdpDestination>>()
    private val transfer = SafetyMissionTransfer(engine, vehicle) { bytes, to -> responses.add(requireNotNull(MavlinkParser.parse(bytes, bytes.size)) to to) }
    private fun parse(bytes: ByteArray) = requireNotNull(MavlinkParser.parse(bytes, bytes.size))
    private fun count(n: Int, type: Int) = parse(sender.missionCount(n, 1, 1, type))
    private fun item(seq: Int, command: Int = 5100, p1: Float = 0f) = SafetyItem(seq, command, 3, -1.2921, 36.8219, 5f, p1)
    private fun last() = responses.last().first

    @Test fun fenceRallyAndNavigationStoresStayIndependentAndZeroCountClearsOnlyItsType() {
        transfer.count(count(1, 2), peer, 2)
        assertEquals(51, last().messageId); assertEquals(2, last().payload[4].toInt())
        val rally = item(0)
        transfer.item(parse(sender.safetyItem(rally, 2, 1, 1)), peer, 2, false)
        assertEquals(0, last().payload[2].toInt()); assertEquals(2, last().payload[3].toInt())
        transfer.count(count(1, 1), peer, 1)
        transfer.item(parse(sender.safetyItem(item(0, 5003, 20f), 1, 1, 1)), peer, 1, false)
        assertEquals(1, engine.safetyPlans.value.fence.size)
        assertEquals(listOf(rally), engine.safetyPlans.value.rally)
        transfer.count(count(0, 1), peer, 1)
        assertTrue(engine.safetyPlans.value.fence.isEmpty())
        assertEquals(listOf(rally), engine.safetyPlans.value.rally)
        assertFalse(engine.state.value.armed)
        assertFalse(engine.safetyPlans.value.config.enabled)
    }

    @Test fun gapOtherPeerAndLostFinalAckDoNotResetOrOverwriteThePlan() {
        transfer.count(count(2, 2), peer, 2)
        val first = item(0); val second = item(1)
        transfer.item(parse(sender.safetyItem(second, 2, 1, 1)), peer, 2, false)
        assertEquals(0, last().payload[0].toInt())
        transfer.item(parse(sender.safetyItem(first, 2, 1, 1)), peer.copy(port = 15551), 2, false)
        assertEquals(14, last().payload[2].toInt())
        transfer.item(parse(sender.safetyItem(first, 2, 1, 1)), peer, 2, false)
        transfer.item(parse(sender.safetyItem(second, 2, 1, 1)), peer, 2, false)
        val plans = engine.safetyPlans.value
        transfer.item(parse(sender.safetyItem(second, 2, 1, 1)), peer, 2, false)
        assertEquals(0, last().payload[2].toInt()); assertSame(plans, engine.safetyPlans.value)
    }

    @Test fun invalidGeometryAndTimedOutUploadRetainLastAcceptedPlan() {
        val original = listOf(item(0, 5003, 20f))
        engine.replaceSafetyPlan(1, original)
        transfer.count(count(1, 1), peer, 1)
        transfer.item(parse(sender.safetyItem(item(0, 5001, 3f), 1, 1, 1)), peer, 1, false)
        assertEquals(5, last().payload[2].toInt()); assertEquals(original, engine.safetyPlans.value.fence)
        transfer.count(count(2, 1), peer, 1)
        transfer.retry(System.currentTimeMillis() + 11_000)
        assertEquals(1, last().payload[2].toInt()); assertEquals(original, engine.safetyPlans.value.fence)
    }

    @Test fun downloadedItemsPreserveTypeCommandFrameAndCoordinates() {
        val rally = item(0)
        engine.replaceSafetyPlan(2, listOf(rally))
        val request = parse(MavlinkWire.frame(51, byteArrayOf(0, 0, 1, 1, 2), 0, 255, 190))
        transfer.readItem(request, peer, 2, false)
        val b = ByteBuffer.wrap(last().payload).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(5100, b.getShort(30).toInt())
        assertEquals(3, b.get(34).toInt()); assertEquals(2, b.get(37).toInt())
        assertEquals(rally.latitude, b.getInt(16) / 1e7)
        assertEquals(5f, b.getFloat(24))
    }

    @Test fun downloadUsesOneSnapshotEvenIfAReplacementArrives() {
        val rally = item(0)
        engine.replaceSafetyPlan(2, listOf(rally))
        val listRequest = parse(MavlinkWire.frame(43, byteArrayOf(1, 1, 2), 0, 255, 190))
        transfer.readCount(listRequest, peer, 2)
        engine.replaceSafetyPlan(2, emptyList())
        transfer.readItem(parse(MavlinkWire.frame(51, byteArrayOf(0, 0, 1, 1, 2), 0, 255, 190)), peer, 2, false)
        assertEquals(73, last().messageId)
        assertEquals(5100, ByteBuffer.wrap(last().payload).order(ByteOrder.LITTLE_ENDIAN).getShort(30).toInt())
    }

    @Test fun qgcCurrentFlagOnFirstSafetyItemIsAcceptedButUnsupportedParamsAreNot() {
        transfer.count(count(1, 2), peer, 2)
        val packet = parse(sender.safetyItem(item(0), 2, 1, 1))
        val bytes = packet.payload.copyOf().apply { this[35] = 1 }
        transfer.item(packet.copy(payload = bytes), peer, 2, false)
        assertEquals(0, last().payload[2].toInt())
        transfer.count(count(1, 2), peer, 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putFloat(8, 1f)
        transfer.item(packet.copy(payload = bytes), peer, 2, false)
        assertEquals(5, last().payload[2].toInt())
        assertEquals(1, engine.safetyPlans.value.rally.size)
    }

    @Test fun fenceTelemetryAndCapabilitiesDescribeRealEngineState() {
        val state = DroneState(safety = SafetyStatus(true, true, 3, 7, 1234u, "Fence RTL", "Rally 1"))
        val fence = ByteBuffer.wrap(parse(vehicle.fenceStatus(state)).payload).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1234, fence.getInt(0)); assertEquals(7, fence.getShort(4).toInt())
        assertEquals(1, fence.get(6).toInt()); assertEquals(3, fence.get(7).toInt()); assertEquals(0, lastMitigation(parse(vehicle.fenceStatus(state))))
        val status = ByteBuffer.wrap(parse(vehicle.sysStatus(state)).payload).order(ByteOrder.LITTLE_ENDIAN)
        assertTrue(status.getInt(0) and (1 shl 20) != 0)
        assertTrue(status.getInt(4) and (1 shl 20) != 0)
        assertEquals(0, status.getInt(8) and (1 shl 20))
        val capabilities = ByteBuffer.wrap(parse(vehicle.autopilotVersion()).payload).order(ByteOrder.LITTLE_ENDIAN).getLong(0)
        assertEquals(16384L or 32768L, capabilities and (16384L or 32768L))
    }
    private fun lastMitigation(p: MavlinkPacket) = p.payload.getOrNull(8)?.toInt() ?: 0
}
