# Phase 4 — 3D Visualization + Education Modules

**Timeline:** Week 9–12  
**Depends on:** Phase 1 (QGC/MAVLink protocol), Phase 2 (Physics), Phase 3 (Controller + Dashboard)  
**Produces:** 3D drone visualization, MAVLink Explorer, PID Lab, Sensor Lab, Flight Mode Lab

---

## 1. Goal

Transform MAVLab from a functional tool into an **educational experience**:

1. **3D Drone Visualization:** A 3D quadcopter model that moves in real time with telemetry (rotates with roll/pitch/yaw, rises with altitude)
2. **MAVLink Explorer:** Live feed of MAVLink messages with field-by-field explanations
3. **PID Lab:** Interactive sliders to tune P, I, D gains and see the effect live
4. **Sensor Lab:** Side-by-side view of phone sensors vs. simulated drone sensors
5. **Flight Mode Lab:** Descriptions of each flight mode with live demonstration

---

## 2. Success Criteria

- [ ] 3D drone model rotates smoothly with roll/pitch/yaw at ≥30 FPS
- [ ] 3D drone altitude changes visually (moves up/down with alt changes)
- [ ] Ground plane and grid visible for spatial reference
- [ ] Flight path trail shows where the drone has been
- [ ] MAVLink Explorer shows live messages with timestamps
- [ ] Tapping a MAVLink message expands it with field explanations
- [ ] PID Lab sliders change gains and the effect is immediately visible in the drone's behavior
- [ ] PID Lab shows a target-vs-actual response chart
- [ ] Sensor Lab displays phone accelerometer, gyroscope, and compass data
- [ ] Sensor Lab shows simulated drone sensor data alongside phone data
- [ ] Flight Mode Lab explains each mode with text + lets user switch and observe
- [ ] Navigation tabs updated: Dashboard | Controller | 3D View | Labs
- [ ] App runs at 60 FPS on a Pixel 6-equivalent device

---

## 3. New Files

```text
com/ascend/mavlab/
├── feature/
│   ├── drone3d/
│   │   ├── Drone3DScreen.kt          # 3D visualization screen
│   │   ├── Drone3DViewModel.kt       # State management for 3D scene
│   │   └── DroneSceneConfig.kt       # Camera, lighting, scene setup
│   ├── mavlink_explorer/
│   │   ├── MavlinkExplorerScreen.kt  # Live MAVLink message feed
│   │   ├── MavlinkExplorerViewModel.kt
│   │   ├── MessageCard.kt            # Expandable message card
│   │   └── FieldExplanations.kt      # Educational text for each MAVLink field
│   ├── pid_lab/
│   │   ├── PIDLabScreen.kt           # PID tuning interface
│   │   ├── PIDLabViewModel.kt
│   │   ├── GainSlider.kt             # Labeled slider for P/I/D values
│   │   └── ResponseChart.kt          # Target vs actual response chart
│   ├── sensor_lab/
│   │   ├── SensorLabScreen.kt        # Phone + drone sensor comparison
│   │   ├── SensorLabViewModel.kt
│   │   └── SensorGaugeRow.kt         # Row of sensor value gauges
│   └── flight_modes/
│       ├── FlightModeLabScreen.kt    # Mode descriptions + live switching
│       ├── FlightModeLabViewModel.kt
│       └── ModeCard.kt               # Card with mode info + activate button
├── assets/
│   └── models/
│       └── drone.glb                 # 3D drone model (bundled in APK)
```

---

## 4. 3D Visualization — Detailed Specification

### 4.1 Technology: SceneView + Google Filament

SceneView wraps Google Filament for Jetpack Compose:

```kotlin
// build.gradle.kts
implementation("io.github.sceneview:sceneview:4.16.10")
```

### 4.2 Scene Setup

```kotlin
package com.ascend.mavlab.feature.drone3d

/**
 * 3D drone visualization.
 *
 * Scene elements:
 *   1. Drone GLB model — loaded from assets
 *   2. Ground plane — flat grid at y=0
 *   3. Sky environment — subtle gradient or IBL
 *   4. Camera — follows drone, user can orbit/zoom
 *   5. Lighting — ambient + directional sun light
 *   6. Flight path trail — line strip from position history
 *   7. Home marker — pin at launch position
 *
 * Telemetry mapping:
 *   - model.rotation.x = pitch (in degrees)
 *   - model.rotation.y = yaw (in degrees)
 *   - model.rotation.z = roll (in degrees)
 *   - model.position.y = altitudeAGL (meters, 1:1 scale)
 *   - model.position.x/z = relative position from home (meters)
 *
 * Update rate: 30 Hz (using Flow.sample(33.milliseconds))
 * The physics runs at 100 Hz internally, but the 3D scene
 * only needs 30 FPS for smooth visual experience.
 */
```

### 4.3 Drone3DScreen.kt — Implementation Skeleton

```kotlin
@Composable
fun Drone3DScreen(viewModel: Drone3DViewModel = hiltViewModel()) {
    val droneState by viewModel.droneState.collectAsStateWithLifecycle()

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        // Camera configuration
        cameraManipulator = rememberCameraManipulator(
            // Start camera at 45° angle, 15m away
        ),
        // Environment/skybox
        environment = rememberEnvironment(engine),
    ) {
        // 1. Load drone model
        val droneModelInstance = rememberModelInstance(
            modelLoader = modelLoader,
            assetFileLocation = "models/drone.glb",
        )

        // 2. Create drone node and update every frame
        droneModelInstance?.let { instance ->
            val droneNode = remember {
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 0.5f, // half meter drone
                    autoAnimate = true,  // propeller animation
                )
            }

            // Update transform from telemetry
            LaunchedEffect(droneState) {
                droneNode.rotation = Rotation(
                    x = Math.toDegrees(droneState.pitch.toDouble()).toFloat(),
                    y = Math.toDegrees(droneState.yaw.toDouble()).toFloat(),
                    z = Math.toDegrees(droneState.roll.toDouble()).toFloat(),
                )
                droneNode.position = Position(
                    x = 0f, // simplified: center drone in scene
                    y = droneState.altitudeAGL,
                    z = 0f,
                )
            }
        }

        // 3. Ground plane (grid)
        // Create a flat plane at y=0 using a simple material
        // or load a grid texture

        // 4. Flight path trail (stretch goal)
        // Maintain a list of past positions
        // Render as a LineNode or series of small sphere nodes
    }
}
```

### 4.4 3D Asset — Drone Model

**Requirements for the GLB file:**
- Format: glTF Binary (.glb)
- Polygon count: ≤ 10,000 triangles
- Materials: PBR metallic-roughness
- Textures: ≤ 1024×1024
- Animations: 1 animation for propeller rotation (all 4 rotors)
- Orientation: Y-up, facing +Z forward
- Scale: 1 unit = 1 meter (drone should be ~0.5m across)
- Color-coded: front arms different color from rear for orientation

**Sources (CC0 or CC-BY):**
- Sketchfab: search "quadcopter glb downloadable" filter CC0
- Create in Blender: File → Export → glTF 2.0 (.glb)
- Use a simple geometric quad model for MVP (4 arms, 4 circles for rotors, box for body)

**Bundle location:** `app/src/main/assets/models/drone.glb`

---

## 5. MAVLink Explorer — Detailed Specification

### 5.1 Screen Design

```text
┌──────────────────────────────────────────┐
│  MAVLink Explorer                [Pause] │
│  ─────────────────────────────────────── │
│  Filter: [All ▼] [ATTITUDE] [POSITION]  │
│  ─────────────────────────────────────── │
│  ┌────────────────────────────────────┐  │
│  │ 12:34:56.789  HEARTBEAT (#0)      │  │
│  │  ▸ Tap to expand                   │  │
│  ├────────────────────────────────────┤  │
│  │ 12:34:56.890  ATTITUDE (#30)      │  │
│  │  roll: 0.052 rad (2.98°)          │  │
│  │  pitch: -0.031 rad (-1.78°)       │  │
│  │  yaw: 1.571 rad (90.0°)           │  │
│  │  ▸ Tap to see field explanations   │  │
│  ├────────────────────────────────────┤  │
│  │ 12:34:57.140  GLOBAL_POSITION_INT │  │
│  │  lat: -1.2921°  lon: 36.8219°     │  │
│  │  alt: 1690.5m MSL                 │  │
│  │  ▸ Tap to expand                   │  │
│  └────────────────────────────────────┘  │
│  [Auto-scroll: ON]  [Clear]  [Messages: 247] │
└──────────────────────────────────────────┘
```

### 5.2 Field Explanations

For each MAVLink message type, provide educational text explaining what each field means and why it matters. Example for ATTITUDE:

```kotlin
val attitudeExplanations = mapOf(
    "roll" to FieldExplanation(
        name = "Roll",
        unit = "radians",
        description = "Rotation around the forward axis. Positive = right side down. " +
            "The drone tilts to move left/right.",
        droneRelevance = "Roll controls lateral movement. In Stabilize mode, " +
            "the autopilot uses the roll PID to keep the drone level.",
        range = "-π to π",
    ),
    "pitch" to FieldExplanation(
        name = "Pitch",
        unit = "radians",
        description = "Rotation around the lateral axis. Positive = nose up. " +
            "The drone tilts forward/backward to move.",
        droneRelevance = "Pitch controls forward/backward movement. " +
            "A drone pitched forward produces a horizontal thrust component.",
        range = "-π/2 to π/2",
    ),
    // ... yaw, rollspeed, pitchspeed, yawspeed
)
```

### 5.3 Implementation Notes

- Use a `LazyColumn` with a ring buffer of ~200 messages
- Each item is a `MessageCard` that expands on tap
- Filter chips at the top to show specific message types
- Pause button freezes the feed for inspection
- Auto-scroll toggle to stay at the bottom
- Message rate counter ("ATTITUDE: 10/s, POSITION: 4/s")
- Color-code message types (blue for attitude, green for position, amber for system)

---

## 6. PID Lab — Detailed Specification

### 6.1 Screen Design

```text
┌──────────────────────────────────────────┐
│  PID Lab                                 │
│  ─────────────────────────────────────── │
│  Controller: [Roll Rate ▼]              │
│  ─────────────────────────────────────── │
│  ┌─────────────────────┐                │
│  │  P: 0.15             │ [──●─────────]│
│  │  I: 0.10             │ [────●───────]│
│  │  D: 0.003            │ [●───────────]│
│  └─────────────────────┘                │
│  ─────────────────────────────────────── │
│  Response Chart                          │
│  ┌────────────────────────────────────┐  │
│  │  ──── Target (setpoint)            │  │
│  │  ──── Actual (measurement)         │  │
│  │                                    │  │
│  │  ╱‾‾‾‾‾‾‾‾‾‾‾‾‾──────────────    │  │
│  │ ╱                                  │  │
│  │╱  ╱‾‾╲                             │  │
│  │  ╱    ╲_____________________________│  │
│  └────────────────────────────────────┘  │
│  ─────────────────────────────────────── │
│  Overshoot: 12%   Settling: 0.8s        │
│  Steady-state error: 0.5%               │
│  ─────────────────────────────────────── │
│  [Reset Gains]  [Apply to Sim]           │
│  ─────────────────────────────────────── │
│  Tips:                                   │
│  • P too high → oscillation              │
│  • I too high → overshoot + slow settle  │
│  • D too high → jittery response         │
│  • Start with P only, then add D, then I │
└──────────────────────────────────────────┘
```

### 6.2 PID Selection

Allow selecting which PID to tune:
- Roll Rate PID
- Pitch Rate PID
- Yaw Rate PID
- Roll Attitude PID
- Pitch Attitude PID
- Altitude PID

### 6.3 Implementation

```kotlin
/**
 * PID Lab implementation approach:
 *
 * 1. Dropdown to select which PID controller to tune
 * 2. Three sliders (P, I, D) bound to the selected PID's gains
 * 3. "Apply to Sim" button writes the new gains to the Autopilot's PID objects
 * 4. The drone immediately responds with the new gains
 * 5. A response chart shows setpoint vs actual for the controlled variable
 *
 * Response chart data comes from:
 *   - Setpoint: the desired value (e.g., desired roll angle = 0 for hover)
 *   - Actual: the measured value (e.g., actual roll angle from physics)
 *
 * The chart uses Vico, same as dashboard charts.
 *
 * Computed metrics:
 *   - Overshoot: max(actual) - setpoint, as percentage
 *   - Settling time: time to stay within 2% of setpoint
 *   - Steady-state error: final error after settling
 *
 * Step response test:
 *   When user taps "Test Step Response":
 *   1. Record current setpoint
 *   2. Briefly disturb the drone (add a roll/pitch impulse)
 *   3. Record the response over 5 seconds
 *   4. Plot and compute metrics
 */
```

---

## 7. Sensor Lab — Detailed Specification

### 7.1 Screen Design

```text
┌──────────────────────────────────────────┐
│  Sensor Lab                              │
│  ─────────────────────────────────────── │
│  Phone Sensors (Real)    Drone Sensors (Sim) │
│  ─────────────────────────────────────── │
│  Accelerometer           IMU Accelerometer  │
│  X: 0.12 m/s²            X: 0.15 m/s²      │
│  Y: -0.05 m/s²           Y: -0.08 m/s²     │
│  Z: -9.78 m/s²           Z: -9.81 m/s²     │
│  ─────────────────────────────────────── │
│  Gyroscope               IMU Gyroscope      │
│  X: 0.002 rad/s          X: 0.003 rad/s     │
│  Y: -0.001 rad/s         Y: -0.002 rad/s    │
│  Z: 0.000 rad/s          Z: 0.001 rad/s     │
│  ─────────────────────────────────────── │
│  Magnetometer            Compass            │
│  Heading: 92.3°           Heading: 90.0°    │
│  ─────────────────────────────────────── │
│  GPS                      Simulated GPS     │
│  Lat: -1.29210            Lat: -1.29211     │
│  Lon: 36.82190            Lon: 36.82189     │
│  Fix: 3D / 15 sats       Fix: 3D / 12 sats │
│  ─────────────────────────────────────── │
│  Barometer                Sim Barometer     │
│  Pressure: 83,100 Pa     Alt: 1690m MSL    │
│  ─────────────────────────────────────── │
│  ┌──────────────────────────────────┐    │
│  │  [Toggle: Phone as Drone Body]   │    │
│  │  When ON: phone sensors REPLACE  │    │
│  │  simulated sensors               │    │
│  └──────────────────────────────────┘    │
│  ─────────────────────────────────────── │
│  Noise Controls:                         │
│  GPS Noise:    [──●───] 0.5m             │
│  IMU Noise:    [───●──] 0.1 m/s²         │
│  Compass Noise:[──●───] 2.0°             │
│  ─────────────────────────────────────── │
│  [Disable GPS]  [Disable Compass]        │
└──────────────────────────────────────────┘
```

### 7.2 Implementation Notes

- Left column reads from `PhoneSensorRepository` (real phone hardware)
- Right column reads from `SimulationEngine.sensorState` (noisy sim data)
- "Phone as Drone Body" toggle feeds phone sensors directly into the autopilot
- Noise sliders modify `SensorModel` parameters in real time
- Disable toggles set `gpsEnabled`/`compassEnabled` on `SensorModel`
- Students can see how disabling GPS triggers mode changes (e.g., drop from Loiter to Alt Hold)
- This directly teaches why sensor redundancy matters

---

## 8. Flight Mode Lab — Detailed Specification

### 8.1 Screen Design

```text
┌──────────────────────────────────────────┐
│  Flight Mode Lab                         │
│  ─────────────────────────────────────── │
│  Current Mode: ALT HOLD                  │
│  ─────────────────────────────────────── │
│  ┌────────────────────────────────────┐  │
│  │ STABILIZE                    [TRY] │  │
│  │ Manual throttle. Autopilot levels  │  │
│  │ the drone when sticks are centered.│  │
│  │ PIDs active: Rate + Attitude       │  │
│  │ Use for: Learning to fly manually  │  │
│  ├────────────────────────────────────┤  │
│  │ ALT HOLD  ✓ Active          [TRY] │  │
│  │ Autopilot holds altitude. Pilot    │  │
│  │ controls tilt. Throttle adjusts    │  │
│  │ target altitude.                   │  │
│  │ PIDs active: Rate+Att+Altitude     │  │
│  │ Use for: Steady hovering           │  │
│  ├────────────────────────────────────┤  │
│  │ LOITER                       [TRY] │  │
│  │ Holds position + altitude using    │  │
│  │ GPS. Pilot input overrides.        │  │
│  │ PIDs active: All (Rate→Att→Alt→Pos)│  │
│  │ Requires: GPS 3D fix               │  │
│  │ Use for: Hands-free hovering       │  │
│  ├────────────────────────────────────┤  │
│  │ RTL                          [TRY] │  │
│  │ Returns to launch point and lands. │  │
│  │ No pilot input needed.             │  │
│  │ PIDs active: All                   │  │
│  │ Use for: Emergency / lost signal   │  │
│  ├────────────────────────────────────┤  │
│  │ LAND                         [TRY] │  │
│  │ Descends at fixed rate and disarms │  │
│  │ on touchdown.                      │  │
│  │ PIDs active: Rate+Att+Altitude     │  │
│  │ Use for: Ending a flight safely    │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

### 8.2 Implementation

- Each `ModeCard` contains: mode name, description, active PIDs, use case, and a "TRY" button
- Tapping "TRY" calls `SimulationEngine.handleSetMode()` and the drone immediately changes behavior
- The active mode is highlighted
- A small animation or indicator shows which PID loops are active for the current mode
- If a mode requires GPS and GPS is disabled (in Sensor Lab), show a warning

---

## 9. Navigation Update

Update the bottom navigation to include new tabs:

```kotlin
enum class MavLabScreen(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    CONTROLLER("Controller", Icons.Default.Gamepad),
    DRONE_3D("3D View", Icons.Default.ViewInAr),
    LABS("Labs", Icons.Default.Science),
}
```

The "Labs" tab contains a sub-navigation or tab row for:
- MAVLink Explorer
- PID Lab
- Sensor Lab
- Flight Mode Lab

---

## 10. Performance Requirements

| Component | Target | Measurement |
|-----------|--------|-------------|
| 3D rendering | 60 FPS on Pixel 6 | Android GPU profiler |
| 3D model load | < 1 second | Cold start timing |
| Chart updates | 10 Hz, no jank | Visual smoothness |
| MAVLink Explorer | 200 messages in list, no lag | Scroll performance |
| PID sliders | Instant visual response | No delay between slider drag and drone behavior change |
| Memory | < 200 MB total app RAM | Android profiler |

---

## 11. Testing Plan

### 3D Visualization
1. Drone model loads without errors
2. Tilting the phone in controller mode → model rotates correspondingly in 3D view
3. Takeoff → model rises; Land → model descends
4. Camera orbit (touch gestures) works without moving the drone
5. Ground plane visible at y=0

### MAVLink Explorer
1. Messages appear in chronological order
2. Filter chips correctly hide/show message types
3. Expanding a message shows field explanations
4. Pause button stops new messages from appearing
5. Message rate counter is accurate (e.g., ATTITUDE shows ~10/s)

### PID Lab
1. Changing P slider → drone responds differently to disturbances
2. Very high P → visible oscillation
3. Adding D → oscillation dampens
4. Adding I → steady-state error reduces
5. "Reset Gains" restores defaults

### Sensor Lab
1. Phone accelerometer/gyroscope/compass values update in real time
2. Simulated sensor values are close to but not identical to true state (noise visible)
3. Increasing GPS noise → position values jitter more
4. Disabling GPS → GPS fix shows 0, satellite count shows 0
5. "Phone as Drone Body" toggle changes simulation behavior

---

## 12. Definition of Done

- [x] 3D drone model renders and responds to telemetry
- [x] MAVLink Explorer shows live messages with explanations
- [x] PID Lab allows real-time gain tuning with visible effects
- [x] Sensor Lab shows phone + simulated sensors side by side
- [x] Flight Mode Lab describes and activates each mode
- [x] Navigation updated with new tabs
- [x] 60 FPS on mid-range device
- [x] All prior phase functionality intact
