package com.ascend.mavlab.feature.drone3d

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.mission.MissionProgress
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MissionRoutePanel(state: DroneState, mission: MissionProgress, trail: List<SimPathPoint>, positionLive: Boolean, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val points = remember(mission) { missionRoutePoints(mission) }
    val nodeGroups = remember(points) { points.groupBy { it.north to it.east }.values.toList() }
    val description = remember(points) { "Received route. " + points.joinToString { "Item ${it.sequence + 1}, ${it.command}, ${it.stage.label}, north ${it.north} metres, east ${it.east} metres" } }
    val textColor = MaterialTheme.colorScheme.onSurface
    var zoom by remember(mission.items) { mutableFloatStateOf(1f) }
    var pan by remember(mission.items) { mutableStateOf(Offset.Zero) }
    val deviation = if (positionLive) activeRouteDeviationMeters(points, state) else null
    Surface(modifier, color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val mapHeight = (maxHeight - 270.dp).coerceAtLeast(220.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Received mission route", style = MaterialTheme.typography.titleLarge)
                        Text("North up · local metres · ${mission.items.size} items", style = MaterialTheme.typography.bodySmall)
                        Text(if (state.armed && state.mode == com.ascend.mavlab.simulation.engine.FlightMode.AUTO) "AUTO executing" else "${state.mode.displayName} · route is not executing AUTO", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onClose) { Text("3D vehicle") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pinch to zoom · drag to pan", style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Text("Fit route") }
                }
                Text(if (!positionLive) "Position unavailable or stale · vehicle marker hidden" else "Vehicle N %.1f m · E %.1f m".format(state.northMeters, state.eastMeters), color = if (positionLive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Text(deviation?.let { "Current leg deviation · %.1f m".format(it) } ?: "Current leg deviation unavailable", style = MaterialTheme.typography.bodySmall)
                if (points.isEmpty()) {
                    Text("Received items have no drawable navigation coordinates. Inspect Ops.", modifier = Modifier.heightIn(min = mapHeight))
                } else {
                    Canvas(Modifier.height(mapHeight).fillMaxWidth().pointerInput(points) {
                        detectTransformGestures { _, shift, factor, _ ->
                            zoom = (zoom * factor).coerceIn(0.5f, 16f)
                            pan += shift
                        }
                    }.semantics {
                        contentDescription = description + if (positionLive) ". Vehicle north ${state.northMeters}, east ${state.eastMeters}. Deviation ${deviation ?: "unavailable"}" else ". Vehicle position not live"
                    }) {
                        val map = fitMissionRoute(points, state.takeIf { positionLive }, trail, size.width, size.height, 30.dp.toPx())
                        val center = Offset(size.width / 2, size.height / 2)
                        fun point(north: Float, east: Float) = center + (Offset(map.x(east), map.y(north)) - center) * zoom + pan
                        val muted = Color(0xFF7B8FA5)
                        drawLine(muted.copy(alpha = 0.2f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height))
                        drawLine(muted.copy(alpha = 0.2f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2))
                        points.zipWithNext().forEach { (start, end) ->
                            drawLine(routeColor(end.stage), point(start.north, start.east), point(end.north, end.east), 3.dp.toPx())
                        }
                        trail.zipWithNext().forEach { (start, end) ->
                            drawLine(Color(0xFFB98AFF).copy(alpha = 0.8f), point(start.northMeters, start.eastMeters), point(end.northMeters, end.eastMeters), 1.5.dp.toPx())
                        }
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = 12.dp.toPx()
                            color = textColor.toArgb()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        nodeGroups.forEach { group ->
                            val node = group.firstOrNull { it.stage == RouteStage.ACTIVE } ?: group.firstOrNull { it.stage == RouteStage.REMAINING } ?: group.first()
                            val center = point(node.north, node.east)
                            drawCircle(routeColor(node.stage), 9.dp.toPx(), center, style = if (group.any { it.partial }) Stroke(2.dp.toPx()) else androidx.compose.ui.graphics.drawscope.Fill)
                            val label = group.joinToString("·") { (it.sequence + 1).toString() }
                            drawIntoCanvas { it.nativeCanvas.drawText(label, center.x, center.y - 13.dp.toPx(), paint) }
                        }
                        if (positionLive) {
                            val vehicle = point(state.northMeters, state.eastMeters)
                            val color = Color(0xFFFF8C5A)
                            drawCircle(color, 6.dp.toPx(), vehicle)
                            drawCircle(color, 10.dp.toPx(), vehicle, style = Stroke(2.dp.toPx()))
                            drawLine(color, vehicle, vehicle + Offset(sin(state.yawRadians) * 22.dp.toPx(), -cos(state.yawRadians) * 22.dp.toPx()), 3.dp.toPx())
                        }
                        val scaleMeters = 40.dp.toPx() / (map.pixelsPerMeter * zoom)
                        val scaleY = size.height - 5.dp.toPx()
                        drawLine(muted, Offset(5.dp.toPx(), scaleY), Offset(45.dp.toPx(), scaleY), 2.dp.toPx())
                        drawIntoCanvas { it.nativeCanvas.drawText("%.1f m".format(scaleMeters), 25.dp.toPx(), scaleY - 5.dp.toPx(), paint) }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("● Earlier items", color = routeColor(RouteStage.COMPLETED), style = MaterialTheme.typography.labelSmall)
                    Text("● Current target", color = routeColor(RouteStage.ACTIVE), style = MaterialTheme.typography.labelSmall)
                    Text("● Remaining", color = routeColor(RouteStage.REMAINING), style = MaterialTheme.typography.labelSmall)
                    Text("● Vehicle", color = Color(0xFFFF8C5A), style = MaterialTheme.typography.labelSmall)
                    Text("━ Actual trail", color = Color(0xFFB98AFF), style = MaterialTheme.typography.labelSmall)
                }
                Text("Numbers follow received item order; co-located nodes share a label. Speed commands have no spatial marker. Hollow nodes have partial behavior; see Ops. Trail records positions observed while SIM is open.", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun routeColor(stage: RouteStage): Color = when (stage) {
    RouteStage.COMPLETED -> Color(0xFF54BFA5)
    RouteStage.ACTIVE -> Color(0xFFFFCF5C)
    RouteStage.REMAINING -> Color(0xFF6BAEFF)
}
