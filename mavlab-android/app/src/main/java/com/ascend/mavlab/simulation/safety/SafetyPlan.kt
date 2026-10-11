package com.ascend.mavlab.simulation.safety

import kotlin.math.*

/** Geographic safety items retain their MAVLink frame; altitude is MSL (0/5) or home-relative (3/6). */
data class SafetyItem(
    val sequence: Int, val command: Int, val frame: Int,
    val latitude: Double, val longitude: Double, val altitude: Float = 0f,
    val param1: Float = 0f, val param2: Float = 0f,
)

data class SafetyConfig(
    val enabled: Boolean = false, val types: Int = 7, val action: Int = 1,
    val radius: Float = 150f, val maxAltitude: Float = 100f, val minAltitude: Float = 0f,
    val margin: Float = 2f, val options: Int = 0, val rtlAltitude: Float = 15f,
    val rallyLimitKm: Float = 5f, val rallyIncludeHome: Boolean = true,
) {
    fun parameters() = listOf(
        "FENCE_ENABLE" to if (enabled) 1f else 0f, "FENCE_TYPE" to types.toFloat(),
        "FENCE_ACTION" to action.toFloat(), "FENCE_RADIUS" to radius,
        "FENCE_ALT_MAX" to maxAltitude, "FENCE_ALT_MIN" to minAltitude,
        "FENCE_MARGIN" to margin, "FENCE_OPTIONS" to options.toFloat(),
        "RTL_ALT" to rtlAltitude * 100f, "RALLY_LIMIT_KM" to rallyLimitKm,
        "RALLY_INCL_HOME" to if (rallyIncludeHome) 1f else 0f,
    )

    /** Unsupported values are refused, never echoed as successful configuration. */
    fun updated(id: String, value: Float): SafetyConfig? {
        if (!value.isFinite()) return null
        fun integer(range: IntRange) = value == value.toInt().toFloat() && value.toInt() in range
        val result = when (id) {
            "FENCE_ENABLE" -> if (integer(0..1)) copy(enabled = value == 1f) else null
            "FENCE_TYPE" -> if (integer(0..15)) copy(types = value.toInt()) else null
            "FENCE_ACTION" -> if (integer(0..2)) copy(action = value.toInt()) else null
            "FENCE_RADIUS" -> if (value in 1f..50_000f) copy(radius = value) else null
            "FENCE_ALT_MAX" -> if (value in 1f..5_000f) copy(maxAltitude = value) else null
            "FENCE_ALT_MIN" -> if (value in 0f..4_999f) copy(minAltitude = value) else null
            "FENCE_MARGIN" -> if (value in 0f..100f) copy(margin = value) else null
            "FENCE_OPTIONS" -> if (value == 0f || value == 2f) copy(options = value.toInt()) else null
            "RTL_ALT" -> if (value in 100f..500_000f) copy(rtlAltitude = value / 100f) else null
            "RALLY_LIMIT_KM" -> if (value in 0f..50f) copy(rallyLimitKm = value) else null
            "RALLY_INCL_HOME" -> if (integer(0..1)) copy(rallyIncludeHome = value == 1f) else null
            else -> null
        }
        return result?.takeIf { it.minAltitude < it.maxAltitude && it.margin < it.radius }
    }
}

data class SafetyPlans(
    val fence: List<SafetyItem> = emptyList(), val rally: List<SafetyItem> = emptyList(),
    val config: SafetyConfig = SafetyConfig(), val lastTransfer: String = "No safety plan received",
)

data class SafetyStatus(
    val enabled: Boolean = false, val breached: Boolean = false, val breachType: Int = 0,
    val breachCount: Int = 0, val breachTimeMs: UInt = 0u,
    val recovery: String = "None", val returnTarget: String = "None",
)

data class GeoPoint(val latitude: Double, val longitude: Double)
private data class Point(val x: Double, val y: Double)

/** Compiled once per upload; no JSON parsing or geometry construction in the physics loop. */
class FenceGeometry private constructor(private val regions: List<Region>) {
    private val exclusions = regions.filter { !it.inclusion }
    private val inclusionGroups = regions.filter { it.inclusion }.groupBy { it.group }.values.toList()
    private class Region(val inclusion: Boolean, val group: Int, val origin: GeoPoint,
                         val vertices: List<Point>, val radius: Double?) {
        fun contains(point: GeoPoint, inset: Double): Boolean {
            val p = project(origin, point)
            if (radius != null) return hypot(p.x, p.y) <= radius - inset
            var inside = false
            var distance = Double.POSITIVE_INFINITY
            vertices.indices.forEach { i ->
                val a = vertices[i]; val b = vertices[(i + 1) % vertices.size]
                distance = min(distance, segmentDistance(p, a, b))
                if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) inside = !inside
            }
            if (distance < 0.001) return inset <= 0.0
            return if (inset >= 0) inside && distance >= inset else inside || distance <= -inset
        }
    }

    fun allows(point: GeoPoint, union: Boolean = false, margin: Float = 0f): Boolean {
        if (exclusions.any { it.contains(point, -margin.toDouble()) }) return false
        if (inclusionGroups.isEmpty()) return true
        if (union) return inclusionGroups.any { group -> group.any { it.contains(point, margin.toDouble()) } }
        // MAVLink inclusion groups: intersection within a group, union between groups.
        return inclusionGroups.any { group -> group.all { it.contains(point, margin.toDouble()) } }
    }

    val regionCount: Int get() = regions.size

    companion object {
        const val MaxItems = 100
        fun validate(items: List<SafetyItem>, type: Int) {
            require(type == 1 || type == 2) { "Unsupported safety plan type" }
            require(items.size <= MaxItems) { "Safety plan exceeds 100 items" }
            items.forEachIndexed { index, item ->
                require(item.sequence == index) { "Non-contiguous safety sequence" }
                require(item.frame in listOf(0, 3, 5, 6)) { "Unsupported altitude frame (terrain/local not implemented)" }
                require(item.latitude.isFinite() && item.latitude in -85.0..85.0 && item.longitude.isFinite() && item.longitude in -180.0..180.0) { "Invalid safety coordinate" }
                require(item.altitude.isFinite() && abs(item.altitude) <= 10_000f && item.param1.isFinite() && item.param2.isFinite()) { "Invalid safety value" }
                require(if (type == 2) item.command == 5100 else item.command in 5000..5004) { "Unsupported safety command ${item.command}" }
                if (item.command == 5100 || item.command == 5000) {
                    require(item.param1 == 0f && item.param2 == 0f) { "Unsupported return-point parameters" }
                    if (item.frame in listOf(3, 6)) require(item.altitude >= 0f) { "Negative relative altitude" }
                }
            }
            if (type == 1) compile(items)
        }

        fun compile(items: List<SafetyItem>): FenceGeometry {
            val regions = mutableListOf<Region>()
            require(items.count { it.command == 5000 } <= 1) { "Multiple fence return points" }
            var index = 0
            while (index < items.size) {
                val item = items[index]
                val origin = GeoPoint(item.latitude, item.longitude)
                if (item.command == 5000) { index++; continue }
                require(item.param2 >= 0f && item.param2 == item.param2.toInt().toFloat()) { "Invalid inclusion group" }
                require(item.command !in listOf(5002, 5004) || item.param2 == 0f) { "Exclusion group is reserved" }
                if (item.command in listOf(5003, 5004)) {
                    require(item.param1 in 1f..50_000f) { "Invalid fence radius" }
                    regions.add(Region(item.command == 5003, item.param2.toInt(), origin, emptyList(), item.param1.toDouble()))
                    index++
                } else {
                    require(item.command in listOf(5001, 5002)) { "Unsupported fence command" }
                    val count = item.param1.toInt()
                    require(item.param1 == count.toFloat() && count in 3..MaxItems && index + count <= items.size) { "Incomplete fence polygon" }
                    val polygon = items.subList(index, index + count)
                    require(polygon.all { it.command == item.command && it.param1 == item.param1 && it.param2 == item.param2 }) { "Inconsistent fence polygon" }
                    val vertices = polygon.map { project(origin, GeoPoint(it.latitude, it.longitude)) }
                    require(vertices.all { hypot(it.x, it.y) <= 50_000 }) { "Polygon exceeds local 50 km model" }
                    require(vertices.distinct().size == count) { "Repeated fence vertex" }
                    val area = vertices.indices.sumOf { i -> val a = vertices[i]; val b = vertices[(i + 1) % count]; a.x * b.y - b.x * a.y }
                    require(abs(area) > 0.01) { "Degenerate fence polygon" }
                    for (a in vertices.indices) for (b in a + 1 until count) {
                        if (b == a + 1 || (a == 0 && b == count - 1)) continue
                        require(!intersects(vertices[a], vertices[(a + 1) % count], vertices[b], vertices[(b + 1) % count])) { "Self-intersecting fence polygon" }
                    }
                    regions.add(Region(item.command == 5001, item.param2.toInt(), origin, vertices, null))
                    index += count
                }
            }
            return FenceGeometry(regions.toList())
        }

        private fun project(origin: GeoPoint, p: GeoPoint) = Point(
            (p.longitude - origin.longitude) * 111_320.0 * cos(Math.toRadians(origin.latitude)),
            (p.latitude - origin.latitude) * 111_320.0,
        )
        private fun segmentDistance(p: Point, a: Point, b: Point): Double {
            val dx = b.x - a.x; val dy = b.y - a.y
            val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
            return hypot(p.x - a.x - t * dx, p.y - a.y - t * dy)
        }
        private fun intersects(a: Point, b: Point, c: Point, d: Point): Boolean {
            fun cross(p: Point, q: Point, r: Point) = (q.x - p.x) * (r.y - p.y) - (q.y - p.y) * (r.x - p.x)
            return (cross(a, b, c) * cross(a, b, d) <= 0 && cross(c, d, a) * cross(c, d, b) <= 0) &&
                max(min(a.x, b.x), min(c.x, d.x)) <= min(max(a.x, b.x), max(c.x, d.x)) &&
                max(min(a.y, b.y), min(c.y, d.y)) <= min(max(a.y, b.y), max(c.y, d.y))
        }
    }
}
