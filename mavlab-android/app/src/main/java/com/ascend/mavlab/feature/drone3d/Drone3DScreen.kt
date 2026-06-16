package com.ascend.mavlab.feature.drone3d

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.filament.LightManager
import com.ascend.mavlab.core.common.AppRuntime
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.FlightMode
import com.ascend.mavlab.simulation.failures.FailureState
import com.ascend.mavlab.simulation.mission.MissionCommand
import com.ascend.mavlab.simulation.mission.MissionItem
import com.ascend.mavlab.simulation.mission.MissionProgress
import io.github.sceneview.SceneView
import io.github.sceneview.SceneScope
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun Drone3DScreen(modifier: Modifier = Modifier) {
    val state by AppRuntime.state.collectAsState()
    val failures by AppRuntime.failures.collectAsState()
    val mission by AppRuntime.missionProgress.collectAsState()
    val modelController = remember { DroneModelController() }
    val trail = remember { mutableStateListOf<SimPathPoint>() }

    LaunchedEffect(state.northMeters, state.eastMeters, state.altitudeAglMeters) {
        trail.pushTrailPoint(
            SimPathPoint(
                northMeters = state.northMeters,
                eastMeters = state.eastMeters,
                altitudeMeters = state.altitudeAglMeters,
            ),
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        DroneScene(
            state = state,
            modelController = modelController,
            modifier = Modifier.fillMaxSize(),
        )
        SimFlightOverlay(
            state = state,
            mission = mission,
            failures = failures,
            trail = trail,
            modifier = Modifier.fillMaxSize(),
        )
        SimHud(
            state = state,
            failures = failures,
            mission = mission,
            modelController = modelController,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun DroneScene(
    state: DroneState,
    modelController: DroneModelController,
    modifier: Modifier = Modifier,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val latestState = rememberUpdatedState(state)

    SceneView(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        environment = rememberEnvironment(engine),
        cameraManipulator = rememberCameraManipulator(),
    ) {
        FlightLighting(state)
        rememberModelInstance(modelLoader, DroneAssetPath)?.let { modelInstance ->
            ModelNode(
                modelInstance = modelInstance,
                scaleToUnits = 0.58f,
                autoAnimate = false,
                position = modelController.bodyPosition(state),
                rotation = modelController.bodyRotation(state),
                apply = {
                    val modelNode = this
                    onFrame = { frameTimeNanos ->
                        modelController.applyPropellerAnimation(
                            modelNode = modelNode,
                            state = latestState.value,
                            animationName = PropellerAnimationName,
                            frameTimeNanos = frameTimeNanos,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun SimHud(
    state: DroneState,
    failures: FailureState,
    mission: MissionProgress,
    modelController: DroneModelController,
    modifier: Modifier = Modifier,
) {
    val aircraftColor = aircraftVisualColor(aircraftVisualState(state, failures))
    Box(modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HudText("SIM  Physical behavior", strong = true, color = aircraftColor)
            HudText(
                "${aircraftVisualState(state, failures).label}  ${state.mode.displayName}  ${if (state.armed) "ARM" else "DISARM"}",
                strong = true,
                color = aircraftColor,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HudLabelValue("ALT", "%.1f m".format(state.altitudeAglMeters))
            HudLabelValue("VS", "%.2f m/s".format(state.verticalSpeedMS))
            HudLabelValue("BAT", "%d%%".format(state.batteryRemainingPercent.toInt()))
            HudLabelValue("FAIL", failureSummary(failures))
        }

        Column(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            HudLabelValue("GS", "%.2f m/s".format(state.groundSpeedMS), alignEnd = true)
            HudLabelValue("GPS", "${gpsLabel(state)} ${state.gpsSatellites}", alignEnd = true)
            HudLabelValue("V", "%.2f".format(state.batteryVoltageMv.toInt() / 1000f), alignEnd = true)
            HudLabelValue("MOTOR", motorFailureSummary(state), alignEnd = true)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HudText("AUTH ${state.controlAuthority.displayName}")
                HudText("WP ${waypointLabel(state, mission)}")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HudText("PATH ${trailLabel(mission)}")
                HudText("VIS ${aircraftVisualState(state, failures).label}", color = aircraftColor)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HudText("N %.1f  E %.1f".format(state.northMeters, state.eastMeters))
                HudText(
                    "HDG ${state.headingDegrees}  THR ${state.throttlePercent}%",
                )
            }
            HudText("RPM ${modelController.rpmSummary(state)}")
        }
    }
}

@Composable
private fun SimFlightOverlay(
    state: DroneState,
    mission: MissionProgress,
    failures: FailureState,
    trail: List<SimPathPoint>,
    modifier: Modifier = Modifier,
) {
    val aircraftColor = aircraftVisualColor(aircraftVisualState(state, failures))
    Canvas(modifier = modifier.padding(18.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val scale = (size.minDimension / 82f).coerceAtLeast(4f)

        fun mapPoint(northMeters: Float, eastMeters: Float): Offset {
            return Offset(
                x = center.x + (eastMeters - state.eastMeters) * scale,
                y = center.y - (northMeters - state.northMeters) * scale,
            )
        }

        val gridMeters = 10f
        val gridAlpha = 0.16f
        val horizontalOffset = ((state.northMeters % gridMeters) * scale)
        val verticalOffset = ((state.eastMeters % gridMeters) * scale)
        var y = center.y + horizontalOffset
        while (y > 0f) y -= gridMeters * scale
        while (y <= size.height) {
            drawLine(
                color = FpvHudColor.copy(alpha = gridAlpha),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += gridMeters * scale
        }
        var x = center.x - verticalOffset
        while (x > 0f) x -= gridMeters * scale
        while (x <= size.width) {
            drawLine(
                color = FpvHudColor.copy(alpha = gridAlpha),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
            )
            x += gridMeters * scale
        }

        val shadowRadius = (22f / (1f + state.altitudeAglMeters / 14f)).coerceIn(4f, 22f)
        drawCircle(
            color = Color.Black.copy(alpha = 0.22f),
            radius = shadowRadius,
            center = center + Offset(0f, 34f),
        )

        if (trail.size >= 2) {
            trail.zipWithNext().forEachIndexed { index, (start, end) ->
                val alpha = ((index + 1).toFloat() / trail.lastIndex.toFloat()).coerceIn(0.12f, 0.72f)
                drawLine(
                    color = aircraftColor.copy(alpha = alpha),
                    start = mapPoint(start.northMeters, start.eastMeters),
                    end = mapPoint(end.northMeters, end.eastMeters),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
            }
        }

        val markers = missionMarkers(state, mission)
        val home = markers.firstOrNull { it.type == SimMissionMarkerType.Home }
        if (state.mode == FlightMode.RTL && home != null) {
            drawLine(
                color = SimWarningColor.copy(alpha = 0.78f),
                start = center,
                end = mapPoint(home.northMeters, home.eastMeters),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
        }
        markers.forEach { marker ->
            val point = mapPoint(marker.northMeters, marker.eastMeters)
            val color = markerColor(marker.type)
            drawCircle(
                color = color.copy(alpha = 0.18f),
                radius = if (marker.type == SimMissionMarkerType.Active) 14f else 10f,
                center = point,
            )
            drawCircle(
                color = color,
                radius = if (marker.type == SimMissionMarkerType.Active) 6f else 4f,
                center = point,
            )
        }

        drawLine(
            color = FpvHudColor.copy(alpha = 0.62f),
            start = Offset(size.width - 30f, center.y + 72f),
            end = Offset(size.width - 30f, center.y - 72f),
            strokeWidth = 2f,
        )
        val altitudeTick = (center.y + 72f - state.altitudeAglMeters.coerceIn(0f, 40f) / 40f * 144f)
        drawLine(
            color = aircraftColor,
            start = Offset(size.width - 42f, altitudeTick),
            end = Offset(size.width - 18f, altitudeTick),
            strokeWidth = 4f,
            cap = StrokeCap.Round,
        )

        drawCircle(
            color = aircraftColor.copy(alpha = 0.20f),
            radius = 28f,
            center = center,
        )
        drawCircle(
            color = aircraftColor,
            radius = 8f,
            center = center,
        )
        drawCircle(
            color = aircraftColor,
            radius = 30f,
            center = center,
            style = Stroke(width = 2f),
        )
    }
}

@Composable
private fun HudLabelValue(
    label: String,
    value: String,
    alignEnd: Boolean = false,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        HudText(label, alpha = 0.66f)
        HudText(value, strong = true)
    }
}

@Composable
private fun HudText(
    text: String,
    strong: Boolean = false,
    alpha: Float = 0.90f,
    color: Color = FpvHudColor,
) {
    Text(
        text = text,
        style = if (strong) {
            MaterialTheme.typography.titleSmall
        } else {
            MaterialTheme.typography.labelLarge
        },
        color = color.copy(alpha = alpha),
        fontFamily = FontFamily.Monospace,
        fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Medium,
    )
}

private val FpvHudColor = Color(0xFFE8FFF7)
private val SimNormalColor = Color(0xFF6CF6B4)
private val SimWarningColor = Color(0xFFFFC857)
private val SimCriticalColor = Color(0xFFFF5B5B)
private val SimFailureColor = Color(0xFFFF8A3D)

@Composable
private fun SceneScope.FlightLighting(state: DroneState) {
    val yaw = state.yawRadians
    val rollBrightness = (abs(state.rollRadians) / MaxRollForLighting).coerceIn(0f, 1f)
    val sunDirection = Position(
        x = (-sin(yaw) * 0.45f).coerceIn(-1f, 1f),
        y = -0.82f,
        z = (-cos(yaw) * 0.45f).coerceIn(-1f, 1f),
    )
    LightNode(
        type = LightManager.Type.DIRECTIONAL,
        intensity = 95_000f + rollBrightness * 25_000f,
        direction = sunDirection,
        apply = { castShadows(true) },
    )
    LightNode(
        type = LightManager.Type.POINT,
        intensity = 4_500f,
        position = Position(x = 0f, y = 1.8f, z = 2.4f),
        apply = {
            falloff(7f)
            castShadows(false)
        },
    )
}

private fun gpsLabel(state: DroneState): String {
    return if (state.gpsFixType.toInt() >= 3) "3D fix" else "No fix"
}

private fun waypointLabel(state: DroneState, mission: MissionProgress): String {
    return mission.activeTarget?.let { target ->
        "${target.sequence + 1}/${mission.items.size} -> %.1f m".format(missionTargetDistanceMeters(state, mission))
    } ?: if (mission.loaded && mission.complete) "Complete" else "None"
}

private fun missionTargetDistanceMeters(state: DroneState, mission: MissionProgress): Float {
    val target = mission.activeTarget ?: return 0f
    val localNorth = target.localNorthMeters
    val localEast = target.localEastMeters
    if (localNorth != null && localEast != null) {
        val north = localNorth - state.northMeters
        val east = localEast - state.eastMeters
        return sqrt(north * north + east * east)
    }
    val north = ((target.latitudeDeg - state.latitudeDeg) * MetersPerLatDeg).toFloat()
    val lonScale = MetersPerLatDeg * max(0.2, cos(Math.toRadians(state.latitudeDeg)))
    val east = ((target.longitudeDeg - state.longitudeDeg) * lonScale).toFloat()
    return sqrt(north * north + east * east)
}

internal data class SimPathPoint(
    val northMeters: Float,
    val eastMeters: Float,
    val altitudeMeters: Float,
)

internal enum class SimAircraftVisualState(val label: String) {
    Normal("NORMAL"),
    Warning("WARN"),
    Critical("CRIT"),
    Failure("FAIL"),
}

internal enum class SimMissionMarkerType {
    Home,
    Active,
    Remaining,
}

internal data class SimMissionMarker(
    val type: SimMissionMarkerType,
    val northMeters: Float,
    val eastMeters: Float,
    val label: String,
)

internal fun aircraftVisualState(
    state: DroneState,
    failures: FailureState,
): SimAircraftVisualState {
    return when {
        failures.activeCount > 0 || state.motors.any { it.failed } -> SimAircraftVisualState.Failure
        state.batteryRemainingPercent.toInt() <= 15 -> SimAircraftVisualState.Critical
        state.batteryRemainingPercent.toInt() <= 30 || state.gpsFixType.toInt() < 3 -> SimAircraftVisualState.Warning
        else -> SimAircraftVisualState.Normal
    }
}

internal fun missionMarkers(
    state: DroneState,
    mission: MissionProgress,
): List<SimMissionMarker> {
    if (!mission.loaded) return emptyList()
    val markers = mutableListOf<SimMissionMarker>()
    val home = mission.items.firstOrNull()
    if (home != null) {
        markers += SimMissionMarker(
            type = SimMissionMarkerType.Home,
            northMeters = localNorthFor(state, home),
            eastMeters = localEastFor(state, home),
            label = "HOME",
        )
    }
    mission.items
        .filter { it.command.isNav && it.command != MissionCommand.RTL }
        .drop(mission.currentIndex.coerceAtLeast(0))
        .take(MaxMissionMarkers)
        .forEach { item ->
            markers += SimMissionMarker(
                type = if (item.sequence == mission.activeTarget?.sequence) {
                    SimMissionMarkerType.Active
                } else {
                    SimMissionMarkerType.Remaining
                },
                northMeters = localNorthFor(state, item),
                eastMeters = localEastFor(state, item),
                label = "WP${item.sequence + 1}",
            )
        }
    return markers.distinctBy { "${it.type}:${it.label}:${it.northMeters}:${it.eastMeters}" }
}

private fun MutableList<SimPathPoint>.pushTrailPoint(point: SimPathPoint) {
    val last = lastOrNull()
    if (last != null) {
        val north = point.northMeters - last.northMeters
        val east = point.eastMeters - last.eastMeters
        val alt = point.altitudeMeters - last.altitudeMeters
        if (sqrt(north * north + east * east + alt * alt) < MinimumTrailSpacingMeters) return
    }
    add(point)
    while (size > MaxTrailPoints) removeAt(0)
}

private fun aircraftVisualColor(state: SimAircraftVisualState): Color {
    return when (state) {
        SimAircraftVisualState.Normal -> SimNormalColor
        SimAircraftVisualState.Warning -> SimWarningColor
        SimAircraftVisualState.Critical -> SimCriticalColor
        SimAircraftVisualState.Failure -> SimFailureColor
    }
}

private fun markerColor(type: SimMissionMarkerType): Color {
    return when (type) {
        SimMissionMarkerType.Home -> Color(0xFF8ED1FF)
        SimMissionMarkerType.Active -> Color(0xFFFFF176)
        SimMissionMarkerType.Remaining -> Color(0xFFE8FFF7)
    }
}

private fun failureSummary(failures: FailureState): String {
    return if (failures.activeCount == 0) "CLEAR" else "${failures.activeCount} ACTIVE"
}

private fun motorFailureSummary(state: DroneState): String {
    return state.motors.mapIndexed { index, motor ->
        if (motor.failed || motor.rpm <= 1f && state.armed) "M${index + 1}!" else "M${index + 1}"
    }.joinToString(" ")
}

private fun trailLabel(mission: MissionProgress): String {
    return when {
        mission.loaded && mission.complete -> "MISSION COMPLETE"
        mission.loaded -> "MISSION"
        else -> "FREE FLIGHT"
    }
}

private fun localNorthFor(state: DroneState, item: MissionItem): Float {
    return item.localNorthMeters ?: ((item.latitudeDeg - state.latitudeDeg) * MetersPerLatDeg).toFloat() + state.northMeters
}

private fun localEastFor(state: DroneState, item: MissionItem): Float {
    val lonScale = MetersPerLatDeg * max(0.2, cos(Math.toRadians(state.latitudeDeg)))
    return item.localEastMeters ?: ((item.longitudeDeg - state.longitudeDeg) * lonScale).toFloat() + state.eastMeters
}

private const val DroneAssetPath = "models/drone.glb"
private const val PropellerAnimationName = "propellers_spin"
private const val MaxRollForLighting = 0.8f
private const val MetersPerLatDeg = 111_320.0
private const val MaxTrailPoints = 120
private const val MinimumTrailSpacingMeters = 0.35f
private const val MaxMissionMarkers = 8
