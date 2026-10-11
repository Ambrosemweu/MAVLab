package com.ascend.mavlab.core.mavlink

import java.nio.ByteBuffer
import java.nio.ByteOrder

import com.ascend.mavlab.simulation.engine.PhysicsSimulationEngine
import com.ascend.mavlab.simulation.safety.FenceGeometry
import com.ascend.mavlab.simulation.safety.SafetyItem

/** Typed fence/rally transactions are independent of the navigation upload and of each other. */
internal class SafetyMissionTransfer(
    private val engine: PhysicsSimulationEngine,
    private val builder: MavlinkMessageBuilder,
    private val send: (ByteArray, UdpDestination) -> Unit,
) {
    private class Upload(val type: Int, val count: Int, val peer: UdpDestination, val system: Int, val component: Int,
                         var progressAt: Long, var requestAt: Long = progressAt, var retries: Int = 0) {
        val items = mutableListOf<SafetyItem>()
        fun owns(p: MavlinkPacket, peer: UdpDestination) = this.peer == peer && system == p.systemId && component == p.componentId
    }
    private val uploads = mutableMapOf<Int, Upload>()
    private val completed = mutableMapOf<Int, Upload>()
    private data class Download(val items: List<SafetyItem>, val peer: UdpDestination, val system: Int, val component: Int, var at: Long)
    private val downloads = mutableMapOf<Int, Download>()

    private fun ack(p: MavlinkPacket, peer: UdpDestination, type: Int, result: Int, message: String) {
        engine.noteSafetyTransfer(message)
        send(builder.missionAck(result, p.systemId, p.componentId, type), peer)
    }

    @Synchronized fun count(p: MavlinkPacket, peer: UdpDestination, type: Int) {
        if (engine.state.value.armed || p.systemId == builder.systemIdForTransfers) {
            ack(p, peer, type, 14, "Safety upload denied: disarm / resolve system ID"); return
        }
        val active = uploads[type]
        if (active != null && !active.owns(p, peer)) { ack(p, peer, type, 14, "Safety upload owned by another peer"); return }
        val count = p.payload.leUInt16(0)
        if (count > FenceGeometry.MaxItems) { ack(p, peer, type, 4, "Safety plan exceeds 100 items"); return }
        completed.remove(type)
        if (count == 0) {
            uploads.remove(type)
            ack(p, peer, type, if (engine.replaceSafetyPlan(type, emptyList())) 0 else 14, "Safety plan $type cleared")
            return
        }
        val upload = Upload(type, count, peer, p.systemId, p.componentId, System.currentTimeMillis())
        uploads[type] = upload
        request(upload)
        engine.noteSafetyTransfer("Uploading ${if (type == 1) "fence" else "rally"} · 0/$count")
    }

    @Synchronized fun item(p: MavlinkPacket, peer: UdpDestination, type: Int, legacy: Boolean) {
        val parsed = parse(p, legacy)
        val upload = uploads[type]
        if (upload == null) {
            val old = completed[type]
            if (old != null && old.owns(p, peer) && System.currentTimeMillis() - old.progressAt <= 10_000 && parsed != null && old.items.getOrNull(parsed.sequence) == parsed) {
                send(builder.missionAck(0, p.systemId, p.componentId, type), peer); return
            }
            ack(p, peer, type, 1, "No active safety upload"); return
        }
        if (!upload.owns(p, peer)) { ack(p, peer, type, 14, "Safety item from other peer"); return }
        if (engine.state.value.armed) { uploads.remove(type); ack(p, peer, type, 14, "Safety upload interrupted by arming"); return }
        if (parsed == null) { uploads.remove(type); ack(p, peer, type, 5, "Invalid safety item / unsupported reserved parameters"); return }
        if (parsed.sequence !in 0 until upload.count) { uploads.remove(type); ack(p, peer, type, 13, "Invalid safety sequence"); return }
        if (parsed.sequence != upload.items.size) { request(upload); return }
        if ((type == 1 && parsed.command !in 5000..5004) || (type == 2 && parsed.command != 5100)) {
            uploads.remove(type); ack(p, peer, type, 3, "Unsupported safety command ${parsed.command}"); return
        }
        if (parsed.frame !in listOf(0, 3, 5, 6)) {
            uploads.remove(type); ack(p, peer, type, 2, "Unsupported safety frame ${parsed.frame}"); return
        }
        upload.items.add(parsed)
        upload.progressAt = System.currentTimeMillis(); upload.retries = 0
        if (upload.items.size < upload.count) {
            request(upload)
            engine.noteSafetyTransfer("Uploading safety plan $type · ${upload.items.size}/${upload.count}")
        } else {
            uploads.remove(type)
            try { FenceGeometry.validate(upload.items, type) }
            catch (e: IllegalArgumentException) { ack(p, peer, type, 5, "Rejected safety plan: ${e.message}"); return }
            if (!engine.replaceSafetyPlan(type, upload.items)) { ack(p, peer, type, 14, "Safety plan change denied"); return }
            completed[type] = upload
            send(builder.missionAck(0, p.systemId, p.componentId, type), peer)
        }
    }

    @Synchronized fun readCount(p: MavlinkPacket, peer: UdpDestination, type: Int) {
        val snapshot = items(type).toList()
        downloads[type] = Download(snapshot, peer, p.systemId, p.componentId, System.currentTimeMillis())
        send(builder.missionCount(snapshot.size, p.systemId, p.componentId, type), peer)
    }

    @Synchronized fun readItem(p: MavlinkPacket, peer: UdpDestination, type: Int, legacy: Boolean) {
        val snapshot = downloads[type]
        if (snapshot != null && (snapshot.peer != peer || snapshot.system != p.systemId || snapshot.component != p.componentId)) {
            ack(p, peer, type, 14, "Safety download owned by another peer"); return
        }
        val source = if (snapshot != null && System.currentTimeMillis() - snapshot.at < 10_000) snapshot.items else items(type)
        snapshot?.at = System.currentTimeMillis()
        val item = source.getOrNull(p.payload.leUInt16(0))
        if (item == null) { ack(p, peer, type, 13, "Invalid safety download sequence"); return }
        send(builder.safetyItem(item, type, p.systemId, p.componentId, legacy), peer)
    }

    @Synchronized fun clear(p: MavlinkPacket, peer: UdpDestination, type: Int): Boolean {
        if (!engine.clearSafetyPlans(type, clearNavigation = type == 255)) { ack(p, peer, type, 14, "Disarm before clearing safety plans"); return false }
        val types = if (type == 255) listOf(1, 2) else listOf(type)
        types.forEach { uploads.remove(it); completed.remove(it); downloads.remove(it) }
        ack(p, peer, type, 0, "Safety plan $type cleared")
        return true
    }

    @Synchronized fun retry(now: Long = System.currentTimeMillis()) {
        uploads.values.toList().forEach { u ->
            if (now - u.requestAt >= 250) {
                if (u.retries >= 5 || now - u.progressAt > 10_000) {
                    uploads.remove(u.type)
                    engine.noteSafetyTransfer("Safety upload timed out · previous plan retained")
                    send(builder.missionAck(1, u.system, u.component, u.type), u.peer)
                } else { u.retries++; request(u, now) }
            }
        }
    }

    @Synchronized fun stop() {
        if (uploads.isNotEmpty()) engine.noteSafetyTransfer("Safety upload interrupted · previous plans retained")
        uploads.clear(); completed.clear(); downloads.clear()
    }
    private fun items(type: Int) = if (type == 1) engine.safetyPlans.value.fence else engine.safetyPlans.value.rally
    private fun request(u: Upload, now: Long = System.currentTimeMillis()) {
        send(builder.missionRequestInt(u.items.size, u.system, u.component, u.type), u.peer)
        u.requestAt = now
    }

    private fun parse(p: MavlinkPacket, legacy: Boolean): SafetyItem? {
        val b = p.payload
        // QGC sets current=1 on the first item for every plan type; these flags do not execute safety items.
        if (b.size < 37 || b.leFloat(8) != 0f || b.leFloat(12) != 0f || b[35].toInt() !in 0..1 || b[36].toInt() !in 0..1) return null
        return SafetyItem(b.leUInt16(28), b.leUInt16(30), b[34].toInt() and 0xff,
            if (legacy) b.leFloat(16).toDouble() else b.leInt32(16) / 1e7,
            if (legacy) b.leFloat(20).toDouble() else b.leInt32(20) / 1e7,
            b.leFloat(24), b.leFloat(0), b.leFloat(4))
    }
    private fun ByteArray.leUInt16(offset: Int) = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).getShort(offset).toInt() and 0xffff
    private fun ByteArray.leInt32(offset: Int) = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).getInt(offset)
    private fun ByteArray.leFloat(offset: Int) = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).getFloat(offset)
}
