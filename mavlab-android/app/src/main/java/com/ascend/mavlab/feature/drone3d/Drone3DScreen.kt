package com.ascend.mavlab.feature.drone3d

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ascend.mavlab.core.common.AppRuntime
import com.ascend.mavlab.simulation.engine.DroneState
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import kotlin.math.PI

@Composable
fun Drone3DScreen(modifier: Modifier = Modifier) {
    val state by AppRuntime.state.collectAsState()
    DroneScene(
        state = state,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun DroneScene(
    state: DroneState,
    modifier: Modifier = Modifier,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    SceneView(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        environment = rememberEnvironment(engine),
        cameraManipulator = rememberCameraManipulator(),
    ) {
        rememberModelInstance(modelLoader, DroneAssetPath)?.let { modelInstance ->
            ModelNode(
                modelInstance = modelInstance,
                scaleToUnits = 1.35f,
                autoAnimate = false,
                position = Position(
                    x = (state.eastMeters * ScenePositionScale).coerceIn(-4f, 4f),
                    y = (state.altitudeAglMeters * AltitudeSceneScale).coerceIn(0.15f, 4.5f),
                    z = (-state.northMeters * ScenePositionScale).coerceIn(-4f, 4f),
                ),
                rotation = Rotation(
                    x = radiansToDegrees(state.pitchRadians),
                    y = radiansToDegrees(state.yawRadians),
                    z = -radiansToDegrees(state.rollRadians),
                ),
            )
        }
    }
}

private fun radiansToDegrees(radians: Float): Float = radians * 180f / PI.toFloat()

private const val DroneAssetPath = "models/drone.glb"
private const val ScenePositionScale = 0.08f
private const val AltitudeSceneScale = 0.12f
