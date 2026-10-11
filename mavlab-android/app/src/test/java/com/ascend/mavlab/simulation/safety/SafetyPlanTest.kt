package com.ascend.mavlab.simulation.safety

import kotlin.test.*

class SafetyPlanTest {
    private fun point(north: Double, east: Double) = GeoPoint(north / 111_320, east / 111_320)
    private fun circle(seq: Int, north: Double, east: Double, inclusion: Boolean, radius: Float = 10f, group: Float = 0f): SafetyItem {
        val p = point(north, east)
        return SafetyItem(seq, if (inclusion) 5003 else 5004, 0, p.latitude, p.longitude, param1 = radius, param2 = group)
    }
    private fun polygon(points: List<Pair<Double, Double>>, inclusion: Boolean = true) = points.mapIndexed { index, (n, e) ->
        val p = point(n, e)
        SafetyItem(index, if (inclusion) 5001 else 5002, 0, p.latitude, p.longitude, param1 = points.size.toFloat())
    }

    @Test fun circlesExclusionsAndReturnMarginAreGeometric() {
        val geometry = FenceGeometry.compile(listOf(circle(0, 0.0, 0.0, true, 30f), circle(1, 0.0, 15.0, false, 3f)))
        assertTrue(geometry.allows(point(0.0, 0.0)))
        assertFalse(geometry.allows(point(0.0, 15.0)))
        assertFalse(geometry.allows(point(0.0, 31.0)))
        assertTrue(geometry.allows(point(0.0, 29.0)))
        assertFalse(geometry.allows(point(0.0, 29.0), margin = 2f))
        assertFalse(geometry.allows(point(0.0, 19.0), margin = 2f))
    }

    @Test fun inclusionGroupsIntersectAndUnionOptionUnites() {
        val overlapping = FenceGeometry.compile(listOf(circle(0, 0.0, 0.0, true), circle(1, 0.0, 10.0, true)))
        assertTrue(overlapping.allows(point(0.0, 5.0)))
        assertFalse(overlapping.allows(point(0.0, -5.0)))
        assertTrue(overlapping.allows(point(0.0, -5.0), union = true))
        val groups = FenceGeometry.compile(listOf(circle(0, 0.0, 0.0, true), circle(1, 0.0, 30.0, true, group = 1f)))
        assertTrue(groups.allows(point(0.0, 30.0)))
        assertFalse(groups.allows(point(0.0, 15.0)))
    }

    @Test fun concavePolygonAndBoundaryAreHandled() {
        val items = polygon(listOf(0.0 to 0.0, 0.0 to 20.0, 10.0 to 10.0, 20.0 to 20.0, 20.0 to 0.0))
        FenceGeometry.validate(items, 1)
        val geometry = FenceGeometry.compile(items)
        assertTrue(geometry.allows(point(10.0, 5.0)))
        assertFalse(geometry.allows(point(10.0, 18.0)))
        assertTrue(geometry.allows(point(0.0, 10.0)))
    }

    @Test fun invalidPolygonNeverBecomesAPlan() {
        val bow = polygon(listOf(0.0 to 0.0, 20.0 to 20.0, 0.0 to 20.0, 20.0 to 0.0))
        assertFailsWith<IllegalArgumentException> { FenceGeometry.validate(bow, 1) }
        assertFailsWith<IllegalArgumentException> { FenceGeometry.validate(bow.dropLast(1), 1) }
        assertFailsWith<IllegalArgumentException> { FenceGeometry.validate(polygon(listOf(0.0 to 0.0, 10.0 to 10.0, 20.0 to 20.0)), 1) }
    }

    @Test fun unsupportedCommandsFramesAndNonfiniteValuesAreRejected() {
        val valid = SafetyItem(0, 5100, 3, -1.0, 36.0, 20f)
        FenceGeometry.validate(listOf(valid), 2)
        listOf(valid.copy(frame = 10), valid.copy(command = 16), valid.copy(latitude = Double.NaN), valid.copy(altitude = -1f))
            .forEach { assertFailsWith<IllegalArgumentException> { FenceGeometry.validate(listOf(it), 2) } }
        assertNull(SafetyConfig().updated("FENCE_ACTION", 3f))
        assertNull(SafetyConfig().updated("FENCE_OPTIONS", 1f))
        assertNull(SafetyConfig().updated("FENCE_TYPE", 1.5f))
        assertNull(SafetyConfig().updated("FENCE_RADIUS", Float.NaN))
    }

    @Test fun snapshotsRoundTripAndCorruptOrFutureVersionsFailClosed() {
        val plans = SafetyPlans(listOf(circle(0, 0.0, 0.0, true)), listOf(SafetyItem(0, 5100, 3, -1.0, 36.0, 20f)),
            SafetyConfig(enabled = true, action = 2, minAltitude = 5f, maxAltitude = 150f, rallyIncludeHome = false))
        val restored = assertNotNull(SafetyPlanCodec.decode(SafetyPlanCodec.encode(plans)))
        assertEquals(plans.fence, restored.fence); assertEquals(plans.rally, restored.rally); assertEquals(plans.config, restored.config)
        assertNull(SafetyPlanCodec.decode("corrupt"))
        val future = java.util.Base64.getEncoder().encodeToString(byteArrayOf(0, 0, 0, 2))
        assertNull(SafetyPlanCodec.decode(future))
        assertNull(SafetyPlanCodec.decode(SafetyPlanCodec.encode(plans).dropLast(12)))
        val narrow = SafetyPlans(config = SafetyConfig(radius = 1f, margin = 0f, maxAltitude = 1f, minAltitude = .5f))
        assertEquals(narrow.config, assertNotNull(SafetyPlanCodec.decode(SafetyPlanCodec.encode(narrow))).config)
    }
}
