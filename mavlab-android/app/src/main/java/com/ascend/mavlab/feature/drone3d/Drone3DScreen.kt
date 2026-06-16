package com.ascend.mavlab.feature.drone3d

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.filament.LightManager
import com.ascend.mavlab.core.common.AppRuntime
import com.ascend.mavlab.core.sensors.OrientationSource
import com.ascend.mavlab.simulation.engine.ControlAuthority
import com.ascend.mavlab.simulation.engine.DroneState
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
    val mission by AppRuntime.missionProgress.collectAsState()
    val sensorSource by AppRuntime.phoneSensorSource.collectAsState()
    val sensorOrientation by AppRuntime.phoneSensorOrientation.collectAsState()
    val modelController = remember { DroneModelController() }
    val gcsMissionOwnsAttitude = state.armed && state.controlAuthority == ControlAuthority.GCS_MISSION
    val usePhoneAttitude = sensorSource != OrientationSource.Unavailable && !gcsMissionOwnsAttitude
    Box(modifier = modifier.fillMaxSize()) {
        DroneScene(
            state = state,
            modelController = modelController,
            modifier = Modifier.fillMaxSize(),
        )
        SimHud(
            state = state,
            mission = mission,
            modelController = modelController,
            yawRadians = if (usePhoneAttitude) sensorOrientation.yaw else state.yawRadians,
            rollRadians = if (usePhoneAttitude) sensorOrientation.roll else state.rollRadians,
            pitchRadians = if (usePhoneAttitude) sensorOrientation.pitch else state.pitchRadians,
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
                scaleToUnits = 1.35f,
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
    mission: MissionProgress,
    modelController: DroneModelController,
    yawRadians: Float,
    rollRadians: Float,
    pitchRadians: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HudText("SIM  Physical behavior", strong = true)
            HudText("${state.mode.displayName}  ${if (state.armed) "ARM" else "DISARM"}", strong = true)
        }

        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HudLabelValue("ALT", "%.1f m".format(state.altitudeAglMeters))
            HudLabelValue("VS", "%.2f m/s".format(state.verticalSpeedMS))
            HudLabelValue("BAT", "%d%%".format(state.batteryRemainingPercent.toInt()))
        }

        Column(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            HudLabelValue("GS", "%.2f m/s".format(state.groundSpeedMS), alignEnd = true)
            HudLabelValue("GPS", "${gpsLabel(state)} ${state.gpsSatellites}", alignEnd = true)
            HudLabelValue("V", "%.2f".format(state.batteryVoltageMv.toInt() / 1000f), alignEnd = true)
        }

        AltitudeInstrument(
            altitudeMeters = state.altitudeAglMeters,
            verticalSpeedMetersPerSecond = state.verticalSpeedMS,
            yawRadians = yawRadians,
            rollRadians = rollRadians,
            pitchRadians = pitchRadians,
            armed = state.armed,
            modifier = Modifier
                .align(Alignment.BottomCenter)
        )

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
) {
    Text(
        text = text,
        style = if (strong) {
            MaterialTheme.typography.titleSmall
        } else {
            MaterialTheme.typography.labelLarge
        },
        color = FpvHudColor.copy(alpha = alpha),
        fontFamily = FontFamily.Monospace,
        fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Medium,
    )
}

private val FpvHudColor = Color(0xFFE8FFF7)

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
    val north = ((target.latitudeDeg - state.latitudeDeg) * MetersPerLatDeg).toFloat()
    val lonScale = MetersPerLatDeg * max(0.2, cos(Math.toRadians(state.latitudeDeg)))
    val east = ((target.longitudeDeg - state.longitudeDeg) * lonScale).toFloat()
    return sqrt(north * north + east * east)
}

private const val DroneAssetPath = "models/drone.glb"
private const val PropellerAnimationName = "propellers_spin"
private const val MaxRollForLighting = 0.8f
private const val MetersPerLatDeg = 111_320.0
