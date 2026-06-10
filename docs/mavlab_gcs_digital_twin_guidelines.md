# MAVLab GCS Digital Twin Guidelines

> **For Hermes / implementers:** This document is the product, architecture, and implementation guideline for pivoting MAVLab away from a lesson-tab education app into a GCS-connected drone digital twin simulator. Use the `writing-plans` and `subagent-driven-development` skills before implementing multi-step changes.

**Status:** Planning guideline

**Project root:** `/home/ambrose/Downloads/Ascend/Drone SIM/`

**Active implementation target:** `mavlab-android/`

**Core decision:** Remove the `Learn` tab from the main app experience and reposition MAVLab as a realistic Android-based drone simulator/digital twin that connects to Ground Control Station software, especially QGroundControl.

---

## 1. Product Direction

### 1.1 Old direction

MAVLab currently presents itself as an Android-first drone education simulator with guided lessons.

That direction is useful, but the `Learn` tab makes the app feel like a classroom tutorial product.

### 1.2 New direction

MAVLab should become:

> **A phone-based drone digital twin simulator that connects to real GCS software, accepts uploaded missions, simulates flight, and visualizes realistic drone component behavior in 3D.**

The educational value should come from operating the simulator, observing real telemetry, testing missions, injecting failures, and seeing the vehicle react — not from a static lesson tab.

### 1.3 New product pillars

1. **GCS Integration**
   - QGroundControl detects MAVLab as a simulated vehicle.
   - QGC can arm/disarm, change modes, upload missions, start missions, and receive telemetry.

2. **Flight Simulation**
   - MAVLab simulates physics, autopilot behavior, battery state, wind, payload, failsafes, and mission execution.

3. **3D Digital Twin**
   - The 3D drone model is not decorative.
   - It reflects live system state: attitude, position, motor RPM, propeller motion, battery, GPS, compass, failures, payload, and mission progress.

4. **Operator Training**
   - Users learn by running realistic workflows: GCS connection, mission planning, autonomous flight, fault injection, and system debugging.

---

## 2. Navigation and UX Changes

### 2.1 Remove the Learn tab

Current navigation in `MavLabAppShell.kt` includes:

- Dashboard
- Lessons / Learn
- Controller
- Drone3D
- Labs
- Settings / Ops

The `Learn` tab should be removed from the main bottom navigation.

Relevant file:

```text
mavlab-android/app/src/main/java/com/ascend/mavlab/feature/navigation/MavLabAppShell.kt
```

Current related lesson files:

```text
mavlab-android/app/src/main/java/com/ascend/mavlab/feature/lessons/Lesson.kt
mavlab-android/app/src/main/java/com/ascend/mavlab/feature/lessons/LessonCatalog.kt
mavlab-android/app/src/main/java/com/ascend/mavlab/feature/lessons/LessonEngine.kt
mavlab-android/app/src/main/java/com/ascend/mavlab/feature/lessons/LessonScreen.kt
```

### 2.2 Do not delete lesson code immediately

Initial implementation should hide/remove the tab from the main user flow, but keep the lesson code temporarily.

Reason:

- Reduces risk.
- Allows us to reuse lesson content later as docs, onboarding, teacher material, or a hidden training mode.
- Keeps the first pivot small and reversible.

Later cleanup can delete or archive lesson code after the new GCS/Twin workflow is stable.

### 2.3 Recommended new tab structure

Preferred direction:

1. **Cockpit**
   - Live flight telemetry.
   - Attitude, altitude, speed, battery, mode, arm state, GPS status, MAVLink status.

2. **Mission**
   - Uploaded QGC mission.
   - Waypoint list.
   - Current target.
   - Mission progress.
   - Load demo / clear / start AUTO controls.

3. **Twin**
   - Real-time 3D drone digital twin.
   - Body attitude, position, motor RPM, propeller behavior, payload, battery, sensor/failure visualization.

4. **Systems**
   - Motors, battery, GPS, compass, payload, wind, failure injection, and health state.

5. **Ops**
   - MAVLink settings, UDP status, system ID, QGC connection diagnostics, logs, test mode.

Minimal first step can keep existing screens and only rename/remove navigation:

- Dashboard -> Cockpit
- Controller -> Controls or keep as Fly
- 3D -> Twin
- Labs -> Systems or keep Labs temporarily
- Settings -> Ops
- Remove Learn

---

## 3. GCS Mission Upload: Required Behavior

### 3.1 Goal

A user should be able to create a mission in QGroundControl, upload it to MAVLab, and have MAVLab simulate the flight.

Target user flow:

1. Open MAVLab on Android.
2. Open QGroundControl on the same phone or desktop on the same Wi-Fi network.
3. QGC detects MAVLab as a MAVLink vehicle.
4. User creates a waypoint mission in QGC.
5. User uploads the mission.
6. MAVLab accepts and stores the uploaded mission.
7. MAVLab displays the uploaded waypoints in its Mission screen.
8. User arms/takes off/starts AUTO from QGC or MAVLab.
9. MAVLab flies the mission using its simulation engine.
10. QGC receives live telemetry, mission progress, and waypoint reached messages.

### 3.2 Current implementation status

Existing MAVLink code lives in:

```text
mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkUdpServer.kt
mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkMessageBuilder.kt
mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkPacket.kt
```

Current support includes some telemetry and mission download/progress behavior:

- HEARTBEAT
- ATTITUDE
- GLOBAL_POSITION_INT
- GPS_RAW_INT
- VFR_HUD
- SYS_STATUS
- BATTERY_STATUS
- COMMAND_ACK
- PARAM_VALUE
- MISSION_COUNT
- MISSION_ITEM_INT for existing mission items
- MISSION_CURRENT
- MISSION_ITEM_REACHED

Inbound handling currently includes:

- SET_MODE
- PARAM_REQUEST_LIST
- PARAM_SET
- MISSION_REQUEST_LIST
- MISSION_REQUEST_INT
- COMMAND_LONG
- ARM/DISARM
- TAKEOFF
- LAND
- SET_MESSAGE_INTERVAL

### 3.3 Missing mission upload support

To upload a mission from QGC into MAVLab, implement the MAVLink mission upload protocol.

Likely required inbound messages from QGC:

- `MISSION_COUNT` — QGC announces how many mission items it wants to send.
- `MISSION_ITEM_INT` — QGC sends each mission item.
- `MISSION_ITEM` — legacy fallback; support if QGC sends it.
- `MISSION_CLEAR_ALL` — clear current mission.
- `MISSION_SET_CURRENT` — set active mission item, optional but useful.
- `COMMAND_LONG` mission-related commands, depending on QGC behavior.

Likely required outbound messages from MAVLab:

- `MISSION_REQUEST_INT` — request next item from QGC.
- `MISSION_REQUEST` — legacy fallback if needed.
- `MISSION_ACK` — accept/reject upload.
- `MISSION_CURRENT` — publish current mission item.
- `MISSION_ITEM_REACHED` — publish reached waypoint.
- `COMMAND_ACK` — acknowledge mode/start/takeoff/land commands.

### 3.4 Mission upload state machine

Implement a dedicated mission upload session instead of scattering state through `MavlinkUdpServer`.

Suggested class:

```text
mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MissionUploadSession.kt
```

Suggested state:

```kotlin
data class MissionUploadSession(
    val expectedCount: Int,
    val receivedItems: MutableMap<Int, MissionItem>,
    val nextSequence: Int,
    val startedAtMs: Long,
    val peer: UdpDestination,
)
```

Suggested flow:

1. QGC sends `MISSION_COUNT`.
2. MAVLab validates count.
3. MAVLab clears temporary upload state.
4. MAVLab sends `MISSION_REQUEST_INT(seq = 0)`.
5. QGC sends `MISSION_ITEM_INT(seq = 0)`.
6. MAVLab parses and stores item 0.
7. MAVLab requests item 1.
8. Repeat until all items are received.
9. MAVLab loads mission into `PhysicsSimulationEngine` / `MissionEngine`.
10. MAVLab sends `MISSION_ACK(ACCEPTED)`.
11. Mission screen displays uploaded mission.

### 3.5 Acceptance criteria for mission upload

Minimum pass criteria:

- QGC detects MAVLab within 3 seconds.
- QGC can upload a 4-waypoint mission.
- MAVLab stores every waypoint with sequence, lat, lon, altitude, and command.
- MAVLab sends `MISSION_ACK` accepted.
- QGC does not hang on upload.
- MAVLab Mission screen shows the uploaded mission.
- Starting AUTO makes the simulated drone fly to uploaded waypoints.
- QGC receives `MISSION_CURRENT` and `MISSION_ITEM_REACHED` updates.
- Re-uploading a mission replaces the old mission cleanly.
- `MISSION_CLEAR_ALL` clears mission state.

### 3.6 Important QGC compatibility rule

Do not implement only the theoretical MAVLink spec. Test against QGroundControl and follow what QGC actually sends.

Add diagnostic logging for every inbound MAVLink message:

- message ID
- peer IP/port
- payload length
- parsed command/sequence where relevant
- accepted/unsupported result

This diagnostic information should be visible in Ops or logs.

---

## 4. 3D Digital Twin Direction

### 4.1 Goal

The 3D drone should become a functional digital twin, not a generic animated object.

It should visually respond to live state from the simulation engine:

- position
- altitude
- roll/pitch/yaw
- velocity
- motor RPM
- motor failures
- propeller spin direction
- battery state
- GPS/compass status
- wind
- payload
- mission target
- failsafe state

### 4.2 Current 3D implementation

Relevant file:

```text
mavlab-android/app/src/main/java/com/ascend/mavlab/feature/drone3d/Drone3DScreen.kt
```

Current behavior:

- Reads `AppRuntime.state`.
- Moves model using `eastMeters`, `northMeters`, and `altitudeAglMeters`.
- Rotates model using pitch, yaw, and roll.
- Runs `propellers_spin` animation when armed.

Current asset:

```text
mavlab-android/app/src/main/assets/models/drone.glb
```

Current model has useful named nodes:

- `DroneRoot`
- `FlightController`
- `ESC`
- `Battery`
- `FPVCamera`
- `Antenna`
- `FrontLeftMotor`
- `FrontRightMotor`
- `RearLeftMotor`
- `RearRightMotor`
- `FrontLeftPropeller`
- `FrontRightPropeller`
- `RearLeftPropeller`
- `RearRightPropeller`

Current animation:

- `propellers_spin`
- Targets the four propeller nodes.

This means the current model can be used for a functional prototype, but a custom model is still recommended for serious product quality.

### 4.3 Required simulation state additions

`DroneState` should expose per-motor telemetry.

Relevant file:

```text
mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/engine/DroneState.kt
```

Minimum addition:

```kotlin
val motor1Rpm: Float = 0f
val motor2Rpm: Float = 0f
val motor3Rpm: Float = 0f
val motor4Rpm: Float = 0f
```

Better future structure:

```kotlin
data class MotorTelemetry(
    val rpm: Float,
    val command: Float,
    val failed: Boolean,
    val currentA: Float = 0f,
    val temperatureC: Float = 25f,
)
```

Use simple scalar fields first if Compose/data-class friction is lower.

RPM conversion:

```text
rpm = radPerSecond * 60 / (2 * PI)
```

The values should come from the motor speeds after failure injection, not only raw autopilot output.

Relevant code path:

```text
PhysicsSimulationEngine.tick()
  -> autopilot.computeMotorOutput(...)
  -> failureInjector.applyMotorFailures(...)
  -> physics.step(...)
  -> DroneState update
```

### 4.4 Required 3D behavior

#### Body transform

Continue mapping:

- east -> x
- altitude -> y
- north -> z
- pitch -> x rotation
- yaw -> y rotation
- roll -> z rotation

But move this logic into a reusable controller layer where possible.

#### Propellers

Each propeller should rotate based on its own motor RPM:

- `FrontLeftPropeller`
- `FrontRightPropeller`
- `RearLeftPropeller`
- `RearRightPropeller`

Behavior:

- Armed + RPM > 0: spin.
- Disarmed: stop.
- Failed motor: corresponding propeller stops or spins down.
- High RPM: stronger blur/visual effect.
- CW/CCW direction should match quadcopter motor layout.

#### Motors

Motor nodes should reflect:

- RPM level
- failure state
- overload warning later
- possible heat tint later

#### Battery

Battery should reflect:

- remaining percent
- low battery warning
- voltage sag later
- failsafe trigger state

#### GPS / compass / flight controller

Visual indicators:

- GPS lock/no lock
- compass interference
- armed/disarmed
- flight mode
- failsafe state

#### Payload

Payload should be visible and connected to simulated payload mass.

For current Ascend R&D alignment, the simulator should eventually support up to 4 kg payload experiments, even if the initial UI slider remains lower.

### 4.5 Digital twin overlay

The Twin screen should eventually include an overlay/HUD:

- arm state
- mode
- altitude
- speed
- vertical speed
- battery
- motor RPM bars
- GPS status
- next waypoint
- wind vector
- failure warnings

The 3D model plus overlay should replace much of the need for static lessons.

---

## 5. Functional Drone Model Requirements

### 5.1 Decision

The app should support a custom functional drone model, but this document should only define the requirements that the Android MAVLab app expects from that model.

Model creation itself is handled separately. Do not include model-authoring workflow instructions here.

For MVP, use the current GLB as the runtime prototype.

For the real MAVLab product, the app should be ready to consume a functional GLB model with reliable node names, correct pivots, and separate components.

### 5.2 Required GLB model structure

The final model should export to GLB with stable node names.

Recommended runtime hierarchy:

```text
DroneRoot
  Body
    Body_TopPlate
    Body_BottomPlate
    FrameArm_FL
    FrameArm_FR
    FrameArm_RL
    FrameArm_RR
  FlightController
    FC_Board
    FC_StatusLED
  Battery
    Battery_Body
    Battery_LED_1
    Battery_LED_2
    Battery_LED_3
    Battery_LED_4
    Battery_Connector
  ESC
    ESC_FL
    ESC_FR
    ESC_RL
    ESC_RR
  Motor_FL
    MotorHousing_FL
    Prop_FL
    PropBlur_FL
  Motor_FR
    MotorHousing_FR
    Prop_FR
    PropBlur_FR
  Motor_RL
    MotorHousing_RL
    Prop_RL
    PropBlur_RL
  Motor_RR
    MotorHousing_RR
    Prop_RR
    PropBlur_RR
  GPSModule
    GPS_StatusLED
  CompassModule
  Antenna
  PayloadMount
    PayloadBox
  CameraGimbal
    GimbalYawAxis
    GimbalPitchAxis
    CameraBody
  NavLights
    LED_FL
    LED_FR
    LED_RL
    LED_RR
```

### 5.3 Naming conventions

Use short, stable, ASCII node names.

Preferred propeller names:

```text
Prop_FL
Prop_FR
Prop_RL
Prop_RR
```

Preferred motor names:

```text
Motor_FL
Motor_FR
Motor_RL
Motor_RR
```

Preferred optional blur nodes:

```text
PropBlur_FL
PropBlur_FR
PropBlur_RL
PropBlur_RR
```

Avoid spaces in node names.

Avoid relying on authoring-tool-only constraints at runtime. The Android app should control runtime behavior through node transforms, material state, and simulation data.

### 5.4 Pivot/origin requirements

Every animated part must have its origin/pivot placed correctly:

- Propeller origin at motor shaft center.
- Gimbal pitch origin at camera pitch axis.
- Gimbal yaw origin at yaw axis.
- Landing gear compression origin if implemented.
- Payload mount origin at attachment point.

If pivots are wrong, runtime animation will look broken.

### 5.5 Material requirements

Use materials that can be adjusted or swapped at runtime where possible:

- body material
- propeller material
- prop blur material
- motor normal material
- motor failed material
- battery normal material
- battery warning material
- LED green/red/orange materials
- payload material

### 5.6 Performance targets

Android performance matters.

Initial target:

- GLB under 5 MB if possible.
- Prefer under 50k triangles for broad device compatibility.
- Texture count minimized.
- Use compressed textures later if needed.
- Avoid excessive bone rigs.
- Prefer simple node transforms for runtime-controlled parts.

### 5.7 Runtime compatibility requirement

The Android app should be model-agnostic as long as the GLB follows the required naming and pivot conventions.

The app should not hard-code behavior to one specific visual asset beyond the node-name contract.

If a required node is missing, the app should degrade gracefully and report the missing node in diagnostics instead of crashing.

## 6. Suggested Architecture Additions

### 6.1 `DroneModelController`

Create a layer that maps simulation state to 3D model behavior.

Suggested file:

```text
mavlab-android/app/src/main/java/com/ascend/mavlab/feature/drone3d/DroneModelController.kt
```

Responsibilities:

- Locate named nodes in the loaded model.
- Apply body transform.
- Apply per-motor propeller rotations.
- Apply failure visual states.
- Apply battery/GPS/LED states later.

Suggested API:

```kotlin
class DroneModelController {
    fun updateBody(state: DroneState)
    fun updateMotors(state: DroneState, deltaSeconds: Float)
    fun updateFailures(state: DroneState, failures: FailureState)
    fun updateBattery(state: DroneState)
    fun updateMissionOverlay(state: DroneState, mission: MissionProgress)
}
```

### 6.2 `MotorTelemetry`

Suggested file:

```text
mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/engine/MotorTelemetry.kt
```

Initial version:

```kotlin
data class MotorTelemetry(
    val rpm: Float = 0f,
    val command: Float = 0f,
    val failed: Boolean = false,
)
```

If using a list inside `DroneState` creates Compose or equality issues, start with four scalar values instead.

### 6.3 `MissionUploadSession`

Suggested file:

```text
mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MissionUploadSession.kt
```

Responsibilities:

- Track upload count.
- Track received mission items.
- Track next requested sequence.
- Validate sequence numbers.
- Timeout stale upload sessions.
- Convert MAVLink mission payloads to internal `MissionItem`.

---

## 7. Implementation Roadmap

### Phase 1: Product navigation pivot

Goal:
Remove Learn tab from the main UX.

Tasks:

1. Remove `Lessons` enum entry from `MavLabTab`.
2. Remove `LessonScreen` import and route from `MavLabAppShell.kt`.
3. Optionally rename `Dashboard` to `Cockpit`.
4. Optionally rename `Drone3D` to `Twin`.
5. Build and run unit tests.

Verification:

```bash
cd "/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android"
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest lintDebug assembleDebug --console=plain
```

Expected:

```text
BUILD SUCCESSFUL
```

### Phase 2: Mission upload spike

Goal:
QGC-created mission uploads into MAVLab.

Tasks:

1. Add parser/build support for missing mission messages.
2. Add `MissionUploadSession`.
3. Handle inbound `MISSION_COUNT`.
4. Send `MISSION_REQUEST_INT`.
5. Parse inbound `MISSION_ITEM_INT`.
6. Store items until count complete.
7. Send `MISSION_ACK`.
8. Load uploaded mission into `MissionEngine`.
9. Display uploaded mission in Mission/Labs screen.
10. Test with QGC.

Verification:

- Upload 4-waypoint QGC mission.
- Confirm MAVLab accepts and displays it.
- Start AUTO and observe waypoint progression.
- Confirm QGC receives progress updates.

### Phase 3: Motor telemetry

Goal:
Expose motor RPM from simulation.

Tasks:

1. Add motor telemetry to `DroneState`.
2. Update `PhysicsSimulationEngine.tick()` to write post-failure motor speeds.
3. Convert rad/s to RPM.
4. Add unit tests for RPM values.
5. Display RPM in Systems/Labs screen.

Verification:

- Disarmed motors report 0 RPM.
- Armed hover reports non-zero RPM.
- Failed motor reports 0 RPM.
- Other motors continue reporting RPM.

### Phase 4: Functional 3D propellers

Goal:
3D propellers spin according to motor RPM.

Tasks:

1. Stop using only generic `propellers_spin` animation for all props.
2. Locate individual propeller nodes.
3. Rotate each propeller based on RPM and elapsed frame time.
4. Apply correct CW/CCW direction.
5. Stop failed motor propeller.
6. Add visual shake during motor failure.

Verification:

- Each propeller responds independently.
- Motor failure stops the matching propeller.
- Throttle increase visibly increases spin speed or blur.

### Phase 5: Functional model integration

Goal:
Make MAVLab ready to consume a functional GLB drone model that follows the runtime node contract.

Tasks:

1. Add model-node diagnostics for required nodes.
2. Report missing optional and required nodes in the Twin/Ops UI.
3. Support fallback behavior when optional nodes are absent.
4. Replace the app asset with the functional model when ready.
5. Verify node control in Android.
6. Confirm model responds to simulation state.

Verification:

- GLB loads in SceneView.
- Required nodes are detected.
- Missing optional nodes do not crash the app.
- Propeller pivots rotate correctly.
- Model responds to simulation state.

### Phase 6: Full digital twin system state

Goal:
The model reflects realistic drone subsystem behavior.

Tasks:

1. Battery LEDs / warning state.
2. GPS lock indicator.
3. Compass interference indicator.
4. Payload visibility/mass behavior.
5. Wind vector visualization.
6. Mission path/target overlay.
7. Failsafe state visualization.

Verification:

- Injecting each failure produces visible model/HUD feedback.
- Mission flight is understandable from the Twin screen alone.

---

## 8. Testing Matrix

### 8.1 Build checks

Run after code changes:

```bash
cd "/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android"
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest lintDebug assembleDebug --console=plain
```

### 8.2 QGC checks

Same-phone:

- MAVLab + QGC in split screen.
- QGC detects MAVLab within 3 seconds.
- Arm/disarm works.
- Mode change works.
- Takeoff/land works.
- Mission upload works.
- Mission execution works.
- Reconnect after MAVLab restart works.

Desktop:

- Android phone and desktop on same Wi-Fi.
- Desktop QGC detects MAVLab.
- Mission upload works.
- Commands work.

### 8.3 Android lifecycle checks

- Rotate screen while connected.
- Background MAVLab for 30 seconds and restore.
- Screen off/on.
- Notification permission flow on Android 13+.
- Split-screen for 30 minutes.
- Low-end Android device performance.

### 8.4 3D performance checks

- Twin screen does not stutter on low-cost phone.
- Propeller animation does not cause excessive recomposition.
- SceneView samples latest state without collecting 100 Hz UI updates unnecessarily.
- Model file size remains acceptable.

---

## 9. Open Questions

1. Should the old lessons become hidden `Training Mode`, or should they be deleted after the pivot?
2. Should the app use `Cockpit / Mission / Twin / Systems / Ops` as final tab names?
3. Should MAVLab imitate ArduPilot behavior closely enough for QGC to label it as ArduPilot-like?
4. Should mission upload support only `MISSION_ITEM_INT`, or also legacy `MISSION_ITEM` from day one?
5. Should the custom model represent a generic quadcopter first or the future Ascend medical logistics drone direction?
6. What is the target Android device class for graphics performance?
7. Should payload simulation align immediately to Ascend's 4 kg payload target?

---

## 10. Definition of Done for the Pivot

The pivot is successful when:

- The Learn tab is gone from the main app.
- The app clearly presents itself as a simulator/digital twin, not a lesson app.
- QGC can upload a mission to MAVLab.
- MAVLab can fly the uploaded mission.
- The Twin screen reflects live attitude, position, and motor RPM.
- A motor failure visibly stops the corresponding propeller.
- Battery/GPS/failure state is visible in the simulator UI.
- Build, lint, and unit tests pass.
- Real QGC testing has been performed on same-phone and desktop setups.

---

## 11. Immediate Next Step

Create a focused implementation plan for Phase 1 and Phase 2:

1. Remove/hide Learn tab.
2. Rename navigation around the new simulator identity.
3. Add mission upload support.
4. Add diagnostics for QGC message tracing.

Model creation will be handled separately in this session. This document only defines what MAVLab needs from the functional model at runtime.
