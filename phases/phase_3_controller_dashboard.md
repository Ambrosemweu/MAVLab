# Phase 3 — Phone Controller + Telemetry Dashboard

**Timeline:** Week 6–8  
**Depends on:** Phase 1 (QGC/MAVLink protocol), Phase 2 (Physics engine, autopilot)  
**Produces:** A phone-tilt controller that flies the simulated drone + a real-time telemetry dashboard with charts.

---

## 1. Goal

1. **Phone tilt controller:** Tilting the phone physically controls the simulated drone (roll, pitch, yaw, throttle)
2. **Telemetry dashboard:** Cards and charts displaying real-time drone state (attitude, altitude, speed, battery, GPS, heading)
3. **Arm/Disarm/Takeoff/Land buttons:** On-screen controls for flight commands
4. **Mode selector:** Switch between Stabilize, Alt Hold, Loiter, RTL, Land from the app

**After this phase, a student can physically tilt their phone to fly the simulated drone, see all telemetry updating live, and use QGC simultaneously in split-screen.**

---

## 2. Success Criteria

- [ ] Tilting phone forward → drone pitches forward → moves on QGC map
- [ ] Tilting phone left → drone rolls left → moves on QGC map
- [ ] On-screen throttle slider controls altitude
- [ ] Phone rotation (yaw) controls drone yaw
- [ ] Calibration button zeroes the current phone orientation as "neutral"
- [ ] ±3° deadzone prevents noise-induced drift
- [ ] Sensor fallback exists for devices without `TYPE_GAME_ROTATION_VECTOR`
- [ ] On-screen controls remain usable when tilt sensors are unavailable
- [ ] Dashboard shows: roll, pitch, yaw, altitude, groundspeed, vertical speed, heading, battery %, battery voltage, GPS status, flight mode, armed status
- [ ] At least 2 rolling charts (attitude and altitude) updating in real time
- [ ] Dashboard cards and charts sample from bounded UI-rate streams, not raw 100 Hz physics state
- [ ] Arm/Disarm/Takeoff/Land buttons function correctly
- [ ] Mode selector works and QGC reflects the mode change
- [ ] Controller and dashboard are accessible via bottom navigation tabs
- [ ] App remains stable for 30+ minutes of active flight
- [ ] Controller tested on at least one low-end Android phone, not only Pixel-class hardware

---

## 3. Architecture

### 3.1 New Components

```text
┌────────────────────────────────────────────────────────────────┐
│                        MAVLab App                               │
│                                                                 │
│  ┌─────────────────┐  ┌──────────────────┐  ┌───────────────┐ │
│  │  Phone Sensors   │  │  SimulationEngine │  │  MAVLink      │ │
│  │  (SensorManager) │  │  (from Phase 2)   │  │  Server       │ │
│  │                  │  │                   │  │  (Phase 1)    │ │
│  │  Orientation     │─►│  pilotInput       │─►│  broadcasts   │ │
│  │  → roll/pitch/yaw│  │  → autopilot      │  │  to QGC       │ │
│  └─────────────────┘  │  → physics         │  └───────────────┘ │
│                       └────────┬───────────┘                    │
│                                │ StateFlow                      │
│                       ┌────────▼───────────┐                    │
│                       │  Dashboard UI       │                    │
│                       │                     │                    │
│                       │  Tab 1: Dashboard   │                    │
│                       │  Tab 2: Controller  │                    │
│                       │                     │                    │
│                       │  • Telemetry cards  │                    │
│                       │  • Charts (Vico)    │                    │
│                       │  • Flight controls  │                    │
│                       │  • Mode selector    │                    │
│                       └─────────────────────┘                    │
└────────────────────────────────────────────────────────────────┘
```

### 3.2 New Files

```text
com/ascend/mavlab/
├── sensors/
│   ├── PhoneSensorRepository.kt       # Reads phone orientation via SensorManager
│   ├── OrientationData.kt             # Data class for phone orientation
│   └── SensorCalibration.kt           # Calibration logic (zero reference)
├── controller/
│   ├── ControlMapper.kt               # Maps phone tilt → pilot input [-1..1]
│   └── ControlConfig.kt               # Deadzone, expo, max angle settings
├── ui/
│   ├── navigation/
│   │   └── MavLabNavigation.kt        # Bottom nav: Dashboard | Controller
│   ├── screens/
│   │   ├── DashboardScreen.kt         # Telemetry dashboard
│   │   ├── ControllerScreen.kt        # Tilt controller UI
│   │   └── HomeScreen.kt              # Updated from Phase 1
│   ├── components/
│   │   ├── TelemetryCard.kt           # Reusable stat card
│   │   ├── AttitudeIndicator.kt       # Artificial horizon widget
│   │   ├── ThrottleSlider.kt          # Vertical throttle slider
│   │   ├── TiltVisualizer.kt          # Shows current phone tilt
│   │   ├── FlightModeChip.kt          # Mode selection chips
│   │   ├── ArmButton.kt               # Arm/disarm toggle
│   │   └── RollingChart.kt            # Real-time chart wrapper (Vico)
│   └── viewmodel/
│       ├── DashboardViewModel.kt      # Collects state for dashboard
│       └── ControllerViewModel.kt     # Manages sensor input + mapping
```

### 3.3 New Dependencies

Add to `gradle/libs.versions.toml`:
```toml
[versions]
vico = "2.1.0"
navigation-compose = "2.8.0"
hilt = "2.51"

[libraries]
vico-compose-m3 = { group = "com.patrykandpatrick.vico", name = "compose-m3", version.ref = "vico" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
```

---

## 4. Detailed Specifications

### 4.1 PhoneSensorRepository.kt

```kotlin
package com.ascend.mavlab.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Reads phone orientation using GAME_ROTATION_VECTOR sensor.
 *
 * Why GAME_ROTATION_VECTOR (not ROTATION_VECTOR):
 *   - ROTATION_VECTOR uses the magnetometer, which is noisy and
 *     affected by nearby magnets/metal
 *   - GAME_ROTATION_VECTOR uses only gyro + accelerometer
 *   - More stable for controller use
 *   - Doesn't drift in short sessions (good enough for our use)
 *
 * Outputs: roll, pitch, yaw in radians at ~50 Hz (SENSOR_DELAY_GAME)
 */
class PhoneSensorRepository(
    private val sensorManager: SensorManager
) {
    /**
     * Flow of phone orientation data.
     * Automatically registers/unregisters the sensor listener.
     */
    fun orientationFlow(): Flow<OrientationData> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: throw IllegalStateException("Game Rotation Vector sensor not available")

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)

                // orientation[0] = azimuth (yaw), [-π, π]
                // orientation[1] = pitch, [-π/2, π/2]
                // orientation[2] = roll, [-π, π]
                trySend(OrientationData(
                    yaw = orientation[0],
                    pitch = orientation[1],
                    roll = orientation[2],
                    timestamp = event.timestamp,
                ))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener, sensor, SensorManager.SENSOR_DELAY_GAME
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}

data class OrientationData(
    val roll: Float,    // radians
    val pitch: Float,   // radians
    val yaw: Float,     // radians
    val timestamp: Long // nanoseconds
)
```

### 4.2 SensorCalibration.kt

```kotlin
package com.ascend.mavlab.sensors

/**
 * Stores a "zero reference" orientation so the user can hold the phone
 * in any comfortable position and call it "neutral."
 *
 * Usage:
 *   1. User holds phone in their preferred position
 *   2. User taps "Calibrate"
 *   3. We record the current orientation as the zero reference
 *   4. All subsequent readings are relative to this reference
 */
class SensorCalibration {
    private var referenceRoll: Float = 0f
    private var referencePitch: Float = 0f
    private var referenceYaw: Float = 0f
    private var calibrated: Boolean = false

    fun calibrate(currentOrientation: OrientationData) {
        referenceRoll = currentOrientation.roll
        referencePitch = currentOrientation.pitch
        referenceYaw = currentOrientation.yaw
        calibrated = true
    }

    fun apply(raw: OrientationData): OrientationData {
        if (!calibrated) return raw
        return OrientationData(
            roll = raw.roll - referenceRoll,
            pitch = raw.pitch - referencePitch,
            yaw = raw.yaw - referenceYaw,
            timestamp = raw.timestamp,
        )
    }

    fun isCalibrated(): Boolean = calibrated
}
```

Fallback order:

1. `TYPE_GAME_ROTATION_VECTOR`
2. `TYPE_ROTATION_VECTOR`
3. Accelerometer/gyroscope fusion if practical
4. Disable tilt mode and keep on-screen controls available

Do not crash the controller screen when a sensor is unavailable.

### 4.3 ControlMapper.kt

```kotlin
package com.ascend.mavlab.controller

import com.ascend.mavlab.sensors.OrientationData
import kotlin.math.abs
import kotlin.math.sign

/**
 * Maps calibrated phone orientation to pilot control input.
 *
 * Features:
 *   - Deadzone: small tilts are ignored (prevents drift from sensor noise)
 *   - Expo curve: finer control near center, full authority at extremes
 *   - Max angle clamping: limits input range
 *
 * Output: FloatArray [roll, pitch, throttle, yaw] in ranges expected by Autopilot:
 *   roll:     -1.0 to 1.0
 *   pitch:    -1.0 to 1.0
 *   throttle:  0.0 to 1.0  (from UI slider, not phone tilt)
 *   yaw:      -1.0 to 1.0
 */
class ControlMapper(
    private val config: ControlConfig = ControlConfig()
) {
    /**
     * Map phone orientation to normalized control values.
     *
     * @param orientation Calibrated phone orientation (relative to zero)
     * @param throttle Raw throttle from UI slider (0.0 to 1.0)
     * @return [roll, pitch, throttle, yaw]
     */
    fun map(orientation: OrientationData, throttle: Float): FloatArray {
        val roll = mapAxis(orientation.roll, config.maxRollAngle, config.deadzoneRad, config.expo)
        val pitch = mapAxis(-orientation.pitch, config.maxPitchAngle, config.deadzoneRad, config.expo)
        val yaw = mapAxis(orientation.yaw, config.maxYawAngle, config.deadzoneRad, config.expo)

        return floatArrayOf(roll, pitch, throttle.coerceIn(0f, 1f), yaw)
    }

    /**
     * Map a single axis value through deadzone → expo → clamp.
     *
     * @param value Raw angle in radians
     * @param maxAngle Maximum input angle in radians
     * @param deadzone Deadzone in radians (values below this → 0)
     * @param expo Expo factor (1.0 = linear, 2.0 = square, 1.5 = moderate)
     * @return Normalized output -1.0 to 1.0
     */
    private fun mapAxis(value: Float, maxAngle: Float, deadzone: Float, expo: Float): Float {
        val absVal = abs(value)

        // Below deadzone → zero
        if (absVal < deadzone) return 0f

        // Remove deadzone offset
        val adjusted = (absVal - deadzone) / (maxAngle - deadzone)

        // Clamp to 0-1
        val clamped = adjusted.coerceIn(0f, 1f)

        // Apply expo curve
        val curved = Math.pow(clamped.toDouble(), expo.toDouble()).toFloat()

        // Restore sign
        return curved * sign(value)
    }
}

data class ControlConfig(
    val maxRollAngle: Float = Math.toRadians(30.0).toFloat(),   // 30° = full stick
    val maxPitchAngle: Float = Math.toRadians(30.0).toFloat(),
    val maxYawAngle: Float = Math.toRadians(45.0).toFloat(),
    val deadzoneRad: Float = Math.toRadians(3.0).toFloat(),     // ±3° deadzone
    val expo: Float = 1.5f,                                      // moderate expo curve
)
```

### 4.4 DashboardScreen.kt — Telemetry Dashboard

```kotlin
package com.ascend.mavlab.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// Collect drone state from SimulationEngine.state via ViewModel

/**
 * Main telemetry dashboard showing all key drone parameters.
 *
 * Layout (scrollable column):
 *
 * ┌──────────────────────────────────────────┐
 * │  [Armed/Disarmed indicator] [Flight Mode]│
 * ├──────────────────────────────────────────┤
 * │  ┌──────────┐  ┌──────────┐  ┌────────┐ │
 * │  │ Altitude │  │ Speed    │  │ Battery│ │
 * │  │ 10.5m    │  │ 2.3 m/s  │  │ 85%    │ │
 * │  └──────────┘  └──────────┘  └────────┘ │
 * ├──────────────────────────────────────────┤
 * │  ┌──────────┐  ┌──────────┐  ┌────────┐ │
 * │  │ Roll     │  │ Pitch    │  │ Yaw    │ │
 * │  │ 5.2°     │  │ -3.1°    │  │ 90°    │ │
 * │  └──────────┘  └──────────┘  └────────┘ │
 * ├──────────────────────────────────────────┤
 * │  ┌──────────┐  ┌──────────┐  ┌────────┐ │
 * │  │ GPS Fix  │  │ Sats     │  │ Voltage│ │
 * │  │ 3D       │  │ 12       │  │ 12.4V  │ │
 * │  └──────────┘  └──────────┘  └────────┘ │
 * ├──────────────────────────────────────────┤
 * │  Attitude Chart (roll/pitch/yaw)         │
 * │  [═══════════════════════════════]       │
 * ├──────────────────────────────────────────┤
 * │  Altitude Chart                          │
 * │  [═══════════════════════════════]       │
 * ├──────────────────────────────────────────┤
 * │  [ARM]  [TAKEOFF]  [LAND]  [RTL]        │
 * └──────────────────────────────────────────┘
 *
 * Implementation notes:
 *   - Use LazyColumn or scrollable Column
 *   - TelemetryCard is a reusable component showing label + value + unit
 *   - Charts use Vico library (CartesianChartHost)
 *   - Commands trigger SimulationEngine methods
 *   - All values update at 10 Hz (use Flow.sample(100.milliseconds))
 */
@Composable
fun DashboardScreen(/* viewModel: DashboardViewModel */) {
    // Collect state from ViewModel
    // Build UI as described above
    // Key implementation details in TelemetryCard and RollingChart components
}
```

### 4.5 ControllerScreen.kt — Phone Tilt Controller

```kotlin
package com.ascend.mavlab.ui.screens

/**
 * Full-screen controller interface.
 *
 * Layout:
 *
 * ┌──────────────────────────────────────────┐
 * │  Flight Mode: [ALT HOLD ▼]              │
 * ├──────────────────────────────────────────┤
 * │                                          │
 * │        ┌──────────────────┐              │
 * │        │  Tilt Visualizer │              │
 * │        │                  │              │
 * │        │    ●──────────   │   ┌───────┐  │
 * │        │   (crosshair    │   │ T     │  │
 * │        │    showing      │   │ H  ▲  │  │
 * │        │    current      │   │ R  │  │  │
 * │        │    tilt angle)  │   │ O  │  │  │
 * │        │                  │   │ T  │  │  │
 * │        └──────────────────┘   │ T  ▼  │  │
 * │                               │ L     │  │
 * │                               │ E     │  │
 * │                               └───────┘  │
 * │                                          │
 * ├──────────────────────────────────────────┤
 * │  [CALIBRATE]    [ARM/DISARM]             │
 * ├──────────────────────────────────────────┤
 * │  Roll: 5.2°  Pitch: -3.1°  Yaw: 90°    │
 * │  Throttle: 50%   Alt: 10.5m              │
 * └──────────────────────────────────────────┘
 *
 * Tilt Visualizer:
 *   - A circular area with a crosshair at center
 *   - A dot moves from center based on phone tilt
 *   - Inside deadzone circle: dot is gray (no input)
 *   - Outside deadzone: dot is green (active input)
 *
 * Throttle Slider:
 *   - Vertical slider on the right side
 *   - 0% at bottom, 100% at top
 *   - In Alt Hold mode: centered at 50% (hover), deviation = climb/descend rate
 *   - In Stabilize mode: direct throttle control
 *
 * Calibrate Button:
 *   - Records current phone orientation as "neutral"
 *   - All control inputs are relative to this position
 *   - Essential for comfortable use at any phone angle
 *
 * Implementation notes:
 *   - TiltVisualizer is a custom Canvas composable
 *   - ThrottleSlider is a custom vertical Slider
 *   - Phone orientation comes from PhoneSensorRepository
 *   - ControlMapper converts orientation → pilot input
 *   - Pilot input is sent to SimulationEngine.setPilotInput()
 *   - Update rate: orientation at ~50 Hz, UI at ~30 Hz, control at ~20 Hz
 */
```

### 4.6 TiltVisualizer.kt — Custom Canvas Component

```kotlin
package com.ascend.mavlab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Circular tilt visualizer showing where the phone is tilted.
 *
 * @param rollNormalized Roll value from -1 to 1 (left to right)
 * @param pitchNormalized Pitch value from -1 to 1 (back to forward)
 * @param deadzoneRadius Normalized deadzone radius (e.g., 0.1)
 */
@Composable
fun TiltVisualizer(
    rollNormalized: Float,
    pitchNormalized: Float,
    deadzoneRadius: Float = 0.1f,
    modifier: Modifier = Modifier.size(200.dp)
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = minOf(size.width, size.height) / 2 * 0.9f

        // Outer circle (max range)
        drawCircle(
            color = Color.Gray.copy(alpha = 0.3f),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )

        // Deadzone circle
        drawCircle(
            color = Color.Gray.copy(alpha = 0.2f),
            radius = radius * deadzoneRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1f)
        )

        // Crosshair lines
        drawLine(Color.Gray.copy(alpha = 0.3f),
            Offset(centerX - radius, centerY),
            Offset(centerX + radius, centerY))
        drawLine(Color.Gray.copy(alpha = 0.3f),
            Offset(centerX, centerY - radius),
            Offset(centerX, centerY + radius))

        // Dot position
        val dotX = centerX + rollNormalized * radius
        val dotY = centerY - pitchNormalized * radius // invert Y for screen coords

        val distFromCenter = kotlin.math.sqrt(
            (rollNormalized * rollNormalized + pitchNormalized * pitchNormalized).toDouble()
        ).toFloat()

        val dotColor = if (distFromCenter > deadzoneRadius) Color.Green else Color.Gray

        // Dot
        drawCircle(
            color = dotColor,
            radius = 12f,
            center = Offset(dotX, dotY)
        )
    }
}
```

### 4.7 ThrottleSlider.kt

```kotlin
package com.ascend.mavlab.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Vertical throttle slider.
 *
 * @param value Current throttle (0.0 to 1.0)
 * @param onValueChange Callback when user drags the slider
 */
@Composable
fun ThrottleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("THR", style = MaterialTheme.typography.labelSmall)
        Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)

        // Vertical slider via rotation
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = -90f
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .width(200.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            )
        )
    }
}
```

### 4.8 RollingChart.kt — Real-Time Charts

```kotlin
package com.ascend.mavlab.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.sample
import kotlin.time.Duration.Companion.milliseconds

/**
 * Real-time rolling chart using Vico library.
 *
 * Displays multiple series (e.g., roll + pitch + yaw) as a scrolling line chart.
 * Keeps the last 100 data points per series.
 *
 * Implementation approach:
 *   1. Collect data from Flow at 10 Hz (using Flow.sample(100.milliseconds))
 *   2. Maintain a circular buffer of 100 points per series
 *   3. Update Vico's CartesianChartModelProducer in a LaunchedEffect
 *   4. DISABLE animation (diffAnimationSpec = null) for smooth real-time display
 *
 * Usage example:
 *   RollingChart(
 *       dataFlows = listOf(
 *           "Roll" to rollFlow,
 *           "Pitch" to pitchFlow,
 *           "Yaw" to yawFlow,
 *       ),
 *       yAxisLabel = "Degrees",
 *   )
 *
 * Key Vico components to use:
 *   - CartesianChartHost: main chart composable
 *   - CartesianChartModelProducer: data source
 *   - rememberCartesianChart(): chart configuration
 *   - rememberLineCartesianLayer(): line rendering layer
 *
 * Performance notes:
 *   - Use Flow.sample() to cap at 10 Hz rendering
 *   - Use Flow.conflate() if data arrives faster than UI can render
 *   - Disable animations for real-time data
 *   - Keep buffer to ≤100 points per series
 */
@Composable
fun RollingChart(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
) {
    // Implementation using Vico's CartesianChartHost
    // See Vico documentation: https://github.com/patrykandpatrick/vico
}
```

### 4.9 Navigation Setup

```kotlin
package com.ascend.mavlab.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

enum class MavLabScreen(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    CONTROLLER("Controller", Icons.Default.Gamepad),
}

/**
 * Bottom navigation with two tabs: Dashboard and Controller.
 * More tabs added in Phase 4 (3D, MAVLink Explorer, etc.)
 */
@Composable
fun MavLabNavigation() {
    val navController = rememberNavController()
    var selectedScreen by remember { mutableStateOf(MavLabScreen.DASHBOARD) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MavLabScreen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = selectedScreen == screen,
                        onClick = {
                            selectedScreen = screen
                            navController.navigate(screen.name) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = MavLabScreen.DASHBOARD.name,
        ) {
            composable(MavLabScreen.DASHBOARD.name) {
                DashboardScreen()
            }
            composable(MavLabScreen.CONTROLLER.name) {
                ControllerScreen()
            }
        }
    }
}
```

---

## 5. Controller Flow — Complete Pipeline

```text
Phone tilts
    ↓
SensorManager (GAME_ROTATION_VECTOR, 50 Hz)
    ↓
PhoneSensorRepository.orientationFlow() (Flow<OrientationData>)
    ↓
SensorCalibration.apply() (subtract zero reference)
    ↓
ControlMapper.map() (deadzone → expo → normalize to -1..1)
    ↓
SimulationEngine.setPilotInput(roll, pitch, throttle, yaw)
    ↓
Autopilot.computeMotorSpeeds() (PID cascade)
    ↓
PhysicsModel.step() (6-DOF dynamics)
    ↓
DroneState updated (100 Hz)
    ↓
Rate-limited output streams
    ├─ MAVLink telemetry to QGC
    ├─ UI cards at 5-10 Hz
    └─ Charts using bounded ring buffers
    ↓
QGC and dashboard show movement without collecting raw 100 Hz state directly
```

---

## 6. Design System Notes

### 6.1 Color Scheme

Use Material 3 dynamic color (Material You) with a fallback dark theme:

- Primary: Deep blue (#1565C0) — represents sky
- Secondary: Amber (#FF8F00) — represents warnings
- Tertiary: Green (#2E7D32) — represents armed/active
- Error: Red — battery low, failures
- Background: Dark (#121212) — easy on eyes, looks professional

### 6.2 TelemetryCard Design

Each card should:
- Have a subtle colored left border indicating category (blue=attitude, green=navigation, amber=battery)
- Show: icon, label, value (large), unit (small)
- Update values smoothly (no flickering)
- Be tappable to expand with more detail (Phase 4)

### 6.3 Typography

- Values: Large, monospace font (so digits don't shift widths)
- Labels: Small, subdued
- Units: Small, next to value

---

## 7. Testing Plan

### 7.1 Sensor Tests

1. **Calibration:** Hold phone flat → calibrate → tilt 30° right → roll shows ~30°
2. **Deadzone:** Tilt <3° in any direction → control output stays at 0
3. **Full range:** Tilt 30° → control output reaches ±1.0
4. **Expo curve:** Small tilts produce proportionally smaller outputs than large tilts

### 7.2 Controller Integration

1. **Forward flight:** Tilt phone forward → drone pitch changes → QGC map shows north movement
2. **Lateral flight:** Tilt phone right → drone rolls right → QGC map shows east movement
3. **Throttle:** Slide throttle up → drone climbs → QGC altitude increases
4. **Yaw:** Rotate phone clockwise → drone heading changes in QGC
5. **Hover stability:** Hold phone neutral → drone hovers still (no drift)

### 7.3 Dashboard Tests

1. **All values display:** Every telemetry field has a visible card
2. **Values update:** Values change as drone state changes
3. **Charts update:** Lines move smoothly at ~10 Hz
4. **Mode changes:** Tapping mode chips changes mode in sim + QGC
5. **Arm/Disarm:** Button toggles correctly, QGC reflects change

---

## 8. Definition of Done

- [x] Phone tilt controls the drone (forward/back/left/right/yaw)
- [x] Throttle slider controls altitude
- [x] Calibrate button works
- [x] Deadzone prevents noise drift
- [x] Dashboard shows all telemetry values
- [x] At least 2 rolling charts (attitude + altitude)
- [x] Arm/Disarm/Takeoff/Land buttons work
- [x] Mode selector works
- [x] QGC reflects all actions from the app
- [x] Bottom navigation between Dashboard and Controller
- [x] All prior phase functionality intact
