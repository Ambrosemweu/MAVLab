package com.ascend.mavlab.simulation.safety

import java.io.*
import java.util.Base64

/** Versioned, bounded snapshot independent of the navigation mission's existing persisted format. */
object SafetyPlanCodec {
    fun encode(plans: SafetyPlans): String {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(1)
            val params = plans.config.parameters()
            out.writeInt(params.size)
            params.forEach { (id, value) -> out.writeUTF(id); out.writeFloat(value) }
            listOf(plans.fence, plans.rally).forEach { items ->
                out.writeInt(items.size)
                items.forEach { i ->
                    out.writeInt(i.sequence); out.writeInt(i.command); out.writeInt(i.frame)
                    out.writeDouble(i.latitude); out.writeDouble(i.longitude)
                    out.writeFloat(i.altitude); out.writeFloat(i.param1); out.writeFloat(i.param2)
                }
            }
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }

    fun decode(snapshot: String?): SafetyPlans? {
        if (snapshot == null || snapshot.length > 100_000) return null
        return try {
            DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(snapshot))).use { input ->
                require(input.readInt() == 1)
                var config = SafetyConfig()
                val keys = config.parameters().map { it.first }.toSet()
                require(input.readInt() == keys.size)
                val seen = mutableSetOf<String>()
                val values = mutableMapOf<String, Float>()
                repeat(keys.size) {
                    val id = input.readUTF()
                    require(id in keys && seen.add(id))
                    values[id] = input.readFloat()
                }
                // Validate dependencies against their final values, not incidental serialization order.
                val order = listOf("FENCE_MARGIN", "FENCE_RADIUS", "FENCE_ALT_MAX", "FENCE_ALT_MIN") + keys.filter { it !in listOf("FENCE_MARGIN", "FENCE_RADIUS", "FENCE_ALT_MAX", "FENCE_ALT_MIN") }
                order.forEach { config = requireNotNull(config.updated(it, values.getValue(it))) }
                fun items(type: Int): List<SafetyItem> {
                    val count = input.readInt()
                    require(count in 0..FenceGeometry.MaxItems)
                    val items = List(count) { SafetyItem(input.readInt(), input.readInt(), input.readInt(),
                        input.readDouble(), input.readDouble(), input.readFloat(), input.readFloat(), input.readFloat()) }
                    FenceGeometry.validate(items, type)
                    return items
                }
                val fence = items(1); val rally = items(2)
                require(input.available() == 0)
                SafetyPlans(fence, rally, config, "Restored safety plans")
            }
        } catch (_: IllegalArgumentException) { null } catch (_: IOException) { null }
    }
}
