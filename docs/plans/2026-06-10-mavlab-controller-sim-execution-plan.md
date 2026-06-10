# MAVLab Controller + SIM Execution Plan

> **For Hermes:** Use `subagent-driven-development` skill to implement this plan phase-by-phase. Treat this document as the conductor score: every agent should know what to play, when to enter, where to look, and how to verify that the whole orchestra still sounds like one product.

**Goal:** Convert MAVLab into a coherent five-surface GCS-connected drone simulator: Cockpit, Controller, Mission, SIM, and Ops. Remove Systems from MVP navigation, make Controller and GCS mission flows drive the same physics state, make SIM reflect real state, and add local flight logging for replay/debug/debrief.

**Architecture:** All inputs must flow through `PhysicsSimulationEngine` and produce `DroneState`; UI surfaces must observe state rather than fake behavior locally. QGC/MAVLink mission commands and built-in Controller inputs are different control sources, but they converge into the same simulation state, telemetry, SIM visuals, and flight logs. SIM is the visual digital-twin surface: it should use a chase/inspection default view where the drone remains readable while world/path/camera movement communicates travel.

**Tech Stack:** Android Kotlin, Jetpack Compose, Kotlin coroutines/StateFlow, SceneView/Filament, MAVLink UDP, local app-private file storage, Gradle.

---

## 0. Product Naming Convention

The final MVP tab names are:

1. `Cockpit`
2. `Controller`
3. `Mission`
4. `SIM`
5. `Ops`

Do not use `Fly` as a product surface name anymore.

Do not use `Twin` as a product surface name anymore.

The phrase “digital twin” can still be used conceptually in docs and marketing, but the UI tab/screen name is `SIM`.

### Naming mapping

| Old/current user label | New user label | Current implementation file | Future implementation name |
| --- | --- | --- | --- |
| Dashboard/Cockpit | Cockpit | `feature/dashboard/DashboardScreen.kt` | `feature/cockpit/CockpitScreen.kt` later |
| Fly | Controller | `feature/controller/ControllerScreen.kt` | keep `ControllerScreen.kt` |
| Twin/Drone3D | SIM | `feature/drone3d/Drone3DScreen.kt` | `feature/sim/SimScreen.kt` later |
| Labs/Systems | remove from MVP nav | `feature/labs/LabsScreen.kt` | hidden/deferred source only |
| Settings/Ops | Ops | `feature/settings/SettingsScreen.kt` | `feature/ops/OpsScreen.kt` later |
| Mission inside Labs | Mission | `feature/labs/LabsScreen.kt` | `feature/mission/MissionScreen.kt` |

### Product rule

Do not do a giant package/file rename in the first pass unless tests stay green. First fix user-facing labels and navigation. Internal package renames can come later.

---

## 1. Required Reference Material

Before implementing any phase, read these files.

### Product/design docs

- `/home/ambrose/Downloads/Ascend/Drone SIM/docs/mavlab_product_surface_definition.md`
  - Source of truth for tabs, Controller/SIM naming, control authority, flight logging, and MVP scope.

- `/home/ambrose/Downloads/Ascend/Drone SIM/docs/mavlab_gcs_digital_twin_guidelines.md`
  - Source of truth for QGC mission upload, MAVLink behavior, motor telemetry, model node expectations, and testing matrix.

- `/home/ambrose/Downloads/Ascend/Drone SIM/docs/mavlab_drone_model_creation_plan.md`
  - Reference only if SIM/model visual requirements need confirmation.

### Core Android files

- `mavlab-android/app/src/main/java/com/ascend/mavlab/feature/navigation/MavLabAppShell.kt`
  - Bottom navigation and screen routing.

- `mavlab-android/app/src/main/java/com/ascend/mavlab/core/common/AppRuntime.kt`
  - Runtime singleton, state exposure, commands to simulation.

- `mavlab-android/app/src/main/java/com/ascend/mavlab/service/SimulationService.kt`
  - Background simulation/service lifecycle.

- `mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/engine/DroneState.kt`
  - Main observable state. Add authority, motor telemetry, recording state here if simple.

- `mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/engine/PhysicsSimulationEngine.kt`
  - Main simulation engine. Controller/GCS inputs must converge here.

- `mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/autopilot/Autopilot.kt`
  - Converts pilot input to motor outputs.

- `mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/autopilot/MissionEngine.kt`
  - Mission execution/progress.

- `mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/mission/Mission.kt`
  - Mission item/progress data structures.

### MAVLink/QGC files

- `mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkUdpServer.kt`
  - Inbound QGC commands, mission upload, telemetry loop, diagnostics.

- `mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkMessageBuilder.kt`
  - Outbound MAVLink messages.

- `mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MissionUploadSession.kt`
  - Existing mission upload session parser/state machine.

- `mavlab-android/app/src/test/java/com/ascend/mavlab/core/mavlink/MissionUploadSessionTest.kt`
  - Existing mission upload tests.

### UI files

- `mavlab-android/app/src/main/java/com/ascend/mavlab/feature/dashboard/DashboardScreen.kt`
  - Cockpit screen.

- `mavlab-android/app/src/main/java/com/ascend/mavlab/feature/controller/ControllerScreen.kt`
  - Controller screen.

- `mavlab-android/app/src/main/java/com/ascend/mavlab/feature/labs/LabsScreen.kt`
  - Source for current mission UI and failure controls. Do not leave this as a main MVP tab.

- `mavlab-android/app/src/main/java/com/ascend/mavlab/feature/drone3d/Drone3DScreen.kt`
  - Current SIM/3D screen implementation.

- `mavlab-android/app/src/main/java/com/ascend/mavlab/feature/settings/SettingsScreen.kt`
  - Ops screen.

- `mavlab-android/app/src/main/java/com/ascend/mavlab/feature/onboarding/OnboardingPages.kt`
  - Must reflect Cockpit/Controller/Mission/SIM/Ops, not Fly/Twin/Systems.

- `mavlab-android/app/src/main/java/com/ascend/mavlab/feature/onboarding/OnboardingScreen.kt`
  - Onboarding chips/text may still mention old names.

### Tests to inspect or extend

- `mavlab-android/app/src/test/java/com/ascend/mavlab/simulation/autopilot/MissionEngineTest.kt`
- `mavlab-android/app/src/test/java/com/ascend/mavlab/simulation/autopilot/AutopilotTest.kt`
- `mavlab-android/app/src/test/java/com/ascend/mavlab/simulation/physics/PhysicsModelTest.kt`
- `mavlab-android/app/src/test/java/com/ascend/mavlab/feature/controller/ControlMapperTest.kt`

---

## 2. Global Invariants

Every implementation task must preserve these invariants.

### Input-state-output invariant

No UI input should directly fake SIM behavior.

Correct:

```text
Controller input or QGC command
  -> PhysicsSimulationEngine / MissionEngine / Autopilot
  -> DroneState
  -> Cockpit + Mission + SIM + Ops + FlightRecorder
```

Wrong:

```text
Controller slider
  -> rotate 3D model directly
```

### Authority invariant

There must be one current high-level control authority:

```kotlin
enum class ControlAuthority {
    IDLE,
    CONTROLLER,
    GCS_DIRECT,
    GCS_MISSION,
}
```

Cockpit and SIM must show it.

Controller must warn or visually de-emphasize manual controls when `GCS_MISSION` is active.

### Logging invariant

Flight logging must record both Controller sessions and GCS sessions. It must not depend on QGC.

### MVP scope invariant

Remove Systems from main navigation. Do not delete useful simulation/failure code yet. Hide it, move only lightweight controls to Controller, and defer a full Systems screen to a future version.

### Performance invariant

Do not push 100 Hz physics updates directly into heavyweight UI, file writes, or SceneView node updates without sampling/throttling.

---

## 3. Build and Verification Commands

Run from:

```bash
cd "/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android"
```

Fast unit verification:

```bash
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest --console=plain
```

Full verification after phase completion:

```bash
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest lintDebug assembleDebug --console=plain
```

Expected result:

```text
BUILD SUCCESSFUL
```

If full verification fails, fix before moving to the next phase.

---

# Phase 1: Product Navigation and Naming Correction

**Goal:** Make the app surface match the new MVP: Cockpit, Controller, Mission, SIM, Ops. Remove Systems from MVP nav. Do not yet do risky broad package renames.

**Reference:**

- `docs/mavlab_product_surface_definition.md`, sections 0-4 and 10.
- `feature/navigation/MavLabAppShell.kt`
- `feature/onboarding/OnboardingPages.kt`
- `feature/onboarding/OnboardingScreen.kt`

## Task 1.1: Update bottom navigation labels

**Objective:** Replace user-facing Fly/Twin/Systems labels with Controller/SIM/Mission and remove Systems from main nav.

**Files:**

- Modify: `mavlab-android/app/src/main/java/com/ascend/mavlab/feature/navigation/MavLabAppShell.kt`
- Later create/route: `feature/mission/MissionScreen.kt` in Phase 2.

**Current nav to inspect:**

```kotlin
Cockpit("Cockpit", "Flight Cockpit", Icons.Filled.Analytics),
Controls("Fly", "Phone Flight Controls", Icons.Filled.ControlCamera),
Twin("Twin", "3D Digital Twin", Icons.Filled.Route),
Systems("Systems", "Systems & Missions", Icons.Filled.Science),
Settings("Ops", "Operations", Icons.Filled.Settings),
```

**Target nav:**

```kotlin
Cockpit("Cockpit", "Flight Cockpit", Icons.Filled.Analytics),
Controller("Controller", "Phone Flight Controls", Icons.Filled.ControlCamera),
Mission("Mission", "GCS Mission", Icons.Filled.Route),
Sim("SIM", "3D Flight SIM", Icons.Filled.Science),
Ops("Ops", "Operations", Icons.Filled.Settings),
```

If icons need to remain available from current imports, reuse existing icons first. Visual icon polish can wait.

**Implementation notes:**

- Rename enum entries user-facing at least: `Controls` -> `Controller`, `Twin` -> `Sim`, `Settings` -> `Ops`.
- Remove `Systems` enum entry from bottom nav.
- Temporarily route `Mission` to existing mission UI only after Phase 2 extraction. If implementing Phase 1 and Phase 2 together, do extraction first.

**Verification:**

- Compile after route is valid.
- Bottom nav shows exactly: Cockpit, Controller, Mission, SIM, Ops.

## Task 1.2: Update onboarding copy

**Objective:** Remove old names from onboarding.

**Files:**

- Modify: `feature/onboarding/OnboardingPages.kt`
- Modify: `feature/onboarding/OnboardingScreen.kt`

**Search terms:**

```text
Fly
Twin
Systems
```

**Target copy concept:**

```text
Use Cockpit, Controller, Mission, SIM, and Ops to operate the simulator, test missions, inspect live vehicle state, and diagnose QGC connections.
```

**Verification:**

- No user-facing onboarding text says Fly, Twin, or Systems.

## Task 1.3: Update screen headings only

**Objective:** Align screen headings with new product names without risky file renames.

**Files:**

- Modify: `feature/controller/ControllerScreen.kt`
- Modify: `feature/drone3d/Drone3DScreen.kt`
- Modify: `feature/settings/SettingsScreen.kt`

**Target:**

- Controller heading: `Controller`
- Controller subtitle: `Phone sensors or custom inputs drive the simulated drone.`
- SIM heading/HUD label: `SIM`
- Ops heading: `Ops`

**Do not:**

- Rename packages yet.
- Delete Labs/System code yet.

**Verification:**

Run:

```bash
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest --console=plain
```

---

# Phase 2: Extract Mission as First-Class Screen

**Goal:** Move mission operation out of Labs/Systems into a dedicated Mission tab.

**Reference:**

- `docs/mavlab_product_surface_definition.md`, Mission section.
- `docs/mavlab_gcs_digital_twin_guidelines.md`, section 3.
- `feature/labs/LabsScreen.kt`
- `simulation/mission/Mission.kt`
- `simulation/autopilot/MissionEngine.kt`
- `core/mavlink/MissionUploadSession.kt`
- `core/mavlink/MavlinkUdpServer.kt`

## Task 2.1: Create Mission package and screen

**Objective:** Extract only mission UI from `LabsScreen.kt` into a dedicated screen.

**Files:**

- Create: `mavlab-android/app/src/main/java/com/ascend/mavlab/feature/mission/MissionScreen.kt`
- Modify: `feature/labs/LabsScreen.kt` only if removing mission block from it.

**Move/copy these concepts from LabsScreen:**

- `MissionLab(...)`
- `MissionStatusCard(...)`
- mission item list
- `Load demo`
- `Start Auto`
- `Clear`
- `Guided N` / `Guided E` can be deferred or moved to Controller advanced if they are more like manual test commands.

**Target `MissionScreen` responsibilities:**

- collect `AppRuntime.state`
- collect `AppRuntime.missionProgress`
- show source/status: QGC uploaded / demo / none if available, otherwise show `Mission loaded` or `No mission loaded`
- show active target
- show waypoint list
- show mission progress
- expose buttons: Load demo, Start Auto, Clear

**Minimal structure:**

```kotlin
@Composable
fun MissionScreen(modifier: Modifier = Modifier) {
    val droneState by AppRuntime.state.collectAsState()
    val mission by AppRuntime.missionProgress.collectAsState()

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Mission", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "QGC/demo mission execution and waypoint progress.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            // Mission status + controls + waypoint list
        }
    }
}
```

**Verification:**

- Mission screen compiles.
- Mission screen shows existing loaded mission state.

## Task 2.2: Route Mission in bottom nav

**Objective:** Add Mission to `MavLabAppShell` route.

**Files:**

- Modify: `feature/navigation/MavLabAppShell.kt`

**Expected routing:**

```kotlin
MavLabTab.Mission -> MissionScreen(modifier)
MavLabTab.Sim -> Drone3DScreen(modifier)
MavLabTab.Ops -> SettingsScreen(modifier)
```

**Verification:**

- App compiles.
- Bottom nav has Mission.
- Systems is gone.

## Task 2.3: Hide Labs/Systems from MVP nav

**Objective:** Keep Labs code for reference, but remove from user navigation.

**Files:**

- Modify: `feature/navigation/MavLabAppShell.kt`
- Possibly leave: `feature/labs/LabsScreen.kt`

**Do not delete yet:**

- failure scenarios
- failure injector
- LabsScreen until its useful controls are migrated or intentionally archived

**Verification:**

Search:

```bash
# Use Hermes search_files, not shell grep, if using tools.
```

No bottom-nav label should be `Systems`.

---

# Phase 3: Control Authority Model

**Goal:** Cockpit, Controller, Mission, SIM, and logs know who is currently driving the drone.

**Reference:**

- `docs/mavlab_product_surface_definition.md`, Product Control Model.
- `simulation/engine/DroneState.kt`
- `simulation/engine/PhysicsSimulationEngine.kt`
- `core/common/AppRuntime.kt`
- `core/mavlink/MavlinkUdpServer.kt`
- `feature/dashboard/DashboardScreen.kt`
- `feature/controller/ControllerScreen.kt`
- `feature/drone3d/Drone3DScreen.kt`

## Task 3.1: Add ControlAuthority enum

**Objective:** Create a simple authority enum shared by simulation/UI.

**Files:**

- Create: `mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/engine/ControlAuthority.kt`
- Modify: `DroneState.kt`

**Implementation:**

```kotlin
package com.ascend.mavlab.simulation.engine

enum class ControlAuthority(val displayName: String) {
    IDLE("Idle"),
    CONTROLLER("Controller"),
    GCS_DIRECT("GCS Direct"),
    GCS_MISSION("GCS Mission"),
}
```

Add to `DroneState`:

```kotlin
val controlAuthority: ControlAuthority = ControlAuthority.IDLE
```

**Test:**

- Add or update a unit test that default `DroneState().controlAuthority == ControlAuthority.IDLE`.

**Verification:**

Run unit tests.

## Task 3.2: Set authority from Controller input

**Objective:** When local Controller input is applied, state authority becomes `CONTROLLER` unless GCS mission owns authority.

**Files:**

- Modify: `AppRuntime.kt`
- Modify: `PhysicsSimulationEngine.kt`
- Modify: `ControllerScreen.kt` if needed.

**Rule:**

- `setPilotInput(...)` should mark intended local control.
- If mode is AUTO with active mission, manual inputs should not silently steal authority; show warning instead.
- If disarmed and only sliders move, authority can remain `IDLE` until armed/takeoff, or become `CONTROLLER_READY`. MVP enum does not include READY, so use `CONTROLLER` only when armed or takeoff/manual command is active.

**Acceptance behavior:**

- Arm/takeoff from Controller -> Cockpit shows `Control: Controller`.
- Custom input while armed -> state remains `CONTROLLER`.

## Task 3.3: Set authority from QGC direct commands

**Objective:** QGC arm/disarm/takeoff/land/mode commands set `GCS_DIRECT`, unless mission execution starts.

**Files:**

- Modify: `MavlinkUdpServer.kt`
- Modify: `PhysicsSimulationEngine.kt` or `AppRuntime.kt` command APIs.

**Places to inspect:**

- `handleCommandLong(...)`
- `handleSetMode(...)`

**Rules:**

- `COMMAND_LONG ARM/DISARM` from QGC -> `GCS_DIRECT`
- `TAKEOFF` from QGC -> `GCS_DIRECT`
- `LAND` from QGC -> `GCS_DIRECT`
- `SET_MODE` from QGC -> `GCS_DIRECT` except AUTO mission start.

## Task 3.4: Set authority from GCS mission

**Objective:** AUTO mission execution sets `GCS_MISSION`.

**Files:**

- Modify: `MavlinkUdpServer.kt`
- Modify: `PhysicsSimulationEngine.kt`
- Modify: `MissionEngine.kt` if needed.

**Places to inspect:**

- `MAV_CMD_MISSION_START`
- `startAutoMission`
- mode `FlightMode.AUTO`
- mission progress loaded/active state

**Rule:**

- If mission loaded and mission start command accepted -> `GCS_MISSION`.
- If mission complete and vehicle lands/disarms -> return to `IDLE` or `GCS_DIRECT` depending state.

## Task 3.5: Display authority in Cockpit

**Objective:** Cockpit must answer “who is controlling the drone?” within five seconds.

**Files:**

- Modify: `feature/dashboard/DashboardScreen.kt`

**Implementation:**

Add a telemetry card near Mode/Armed:

```kotlin
TelemetryCard("Control", state.controlAuthority.displayName, Modifier.weight(1f), accent = true)
```

**Verification:**

- Cockpit shows Control.
- Controller command changes it to Controller.
- QGC mission changes it to GCS Mission.

## Task 3.6: Display authority in SIM HUD placeholder

**Objective:** SIM must show authority even before full HUD polish.

**Files:**

- Modify: `feature/drone3d/Drone3DScreen.kt`

**Simple MVP:**

Wrap SceneView in a `Box`, overlay a small `ElevatedCard` or text chip with:

- SIM
- authority
- mode
- armed
- altitude

**Verification:**

- SIM visible overlay updates with state.

---

# Phase 4: Controller Simplification and Advanced Test Inputs

**Goal:** Controller supports phone sensors and custom inputs without becoming clunky. Move only essential failure/perturbation controls from old Systems/Labs into a collapsed advanced section.

**Reference:**

- `docs/mavlab_product_surface_definition.md`, Controller and Deferred Systems sections.
- `feature/controller/ControllerScreen.kt`
- `feature/labs/LabsScreen.kt`
- `simulation/failures/FailureInjector.kt`
- `simulation/failures/FailureScenario.kt`
- `simulation/failures/FailureState.kt`
- `core/common/AppRuntime.kt`

## Task 4.1: Add Controller input mode enum

**Objective:** Make selected input mode explicit.

**Files:**

- Create: `feature/controller/ControllerInputMode.kt` or keep private enum in `ControllerScreen.kt`.

**Implementation:**

```kotlin
enum class ControllerInputMode(val label: String) {
    PHONE_SENSORS("Phone sensors"),
    CUSTOM_INPUT("Custom input"),
}
```

**Rule:**

- Default to `PHONE_SENSORS` only if sensor source is available.
- Otherwise default to `CUSTOM_INPUT`.

## Task 4.2: Add input mode selector UI

**Objective:** Show `Phone sensors` vs `Custom input` as chips or segmented buttons.

**Files:**

- Modify: `ControllerScreen.kt`

**Behavior:**

- Phone sensors mode shows calibration, sensor source, tilt visualizer, throttle/yaw controls.
- Custom input mode shows roll/pitch/yaw/throttle sliders.
- Do not show both modes at the same time.

**Verification:**

- Switch modes.
- UI becomes simpler, not longer.

## Task 4.3: Add GCS mission authority warning

**Objective:** Controller should not pretend it owns the drone during GCS mission execution.

**Files:**

- Modify: `ControllerScreen.kt`

**Behavior:**

If `state.controlAuthority == ControlAuthority.GCS_MISSION`, show a visible note:

```text
GCS Mission is active. Manual Controller inputs are paused or secondary until mission control is released.
```

Options:

- disable sliders while mission authority is active, or
- allow changes but do not route them to simulation until authority changes.

MVP recommendation: disable input controls during active GCS Mission to avoid confusion.

## Task 4.4: Move lightweight advanced test inputs

**Objective:** Keep Controller useful but not lab-like.

**Files:**

- Modify: `ControllerScreen.kt`
- Reference: `LabsScreen.kt` for old failure controls.

**Advanced section title:**

```text
Advanced test inputs
```

**Include only:**

- motor failure quick toggle: Motor 1-4
- GPS loss toggle
- wind preset: None / Light / Strong
- reset perturbations

**Do not include in MVP Controller:**

- battery drain multiplier
- compass offset slider
- payload mass slider
- detailed failure scenarios list
- all old lab sliders

**Implementation notes:**

- Use compact chips, not many long sliders.
- If Compose has no expandable component in project, use a simple boolean with an `OutlinedButton` to show/hide.

**Verification:**

- Default Controller is not clunky.
- Advanced section is hidden by default.
- Motor failure toggles still affect simulation state.

---

# Phase 5: Motor Telemetry and Physical State Mapping

**Goal:** Both Controller and GCS Mission produce motor RPM/attitude/position state that SIM can visualize.

**Reference:**

- `docs/mavlab_gcs_digital_twin_guidelines.md`, sections 4.3 and 4.4.
- `simulation/engine/DroneState.kt`
- `simulation/engine/PhysicsSimulationEngine.kt`
- `simulation/autopilot/Autopilot.kt`
- `simulation/failures/FailureInjector.kt`
- `simulation/physics/PhysicsModel.kt`
- `simulation/physics/MotorMixer.kt`

## Task 5.1: Add MotorTelemetry model

**Objective:** Create a small data class for motor telemetry.

**Files:**

- Create: `simulation/engine/MotorTelemetry.kt`
- Modify: `DroneState.kt`

**Implementation:**

```kotlin
package com.ascend.mavlab.simulation.engine

data class MotorTelemetry(
    val rpm: Float = 0f,
    val command: Float = 0f,
    val failed: Boolean = false,
)
```

In `DroneState`, either use:

```kotlin
val motors: List<MotorTelemetry> = List(4) { MotorTelemetry() }
```

or use scalar fields if Compose/data-class friction appears:

```kotlin
val motor1Rpm: Float = 0f
val motor2Rpm: Float = 0f
val motor3Rpm: Float = 0f
val motor4Rpm: Float = 0f
```

MVP recommendation: use scalar fields first if that is easier and lower risk.

## Task 5.2: Populate RPM from post-failure motor speeds

**Objective:** Motor RPM reflects the actual simulated motor state after failures.

**Files:**

- Modify: `PhysicsSimulationEngine.kt`
- Possibly inspect: `FailureInjector.kt`, `PhysicsModel.kt`, `MotorMixer.kt`

**Rule:**

Use post-failure motor speed values, not raw autopilot output.

RPM conversion:

```kotlin
rpm = radPerSecond * 60f / (2f * PI.toFloat())
```

**Acceptance:**

- disarmed: all RPM 0
- armed hover: RPM > 0
- failed motor: failed motor RPM 0 or near 0
- other motors continue reporting RPM

## Task 5.3: Add motor telemetry tests

**Objective:** Prevent fake or stale RPM values.

**Files:**

- Create or modify: `app/src/test/java/com/ascend/mavlab/simulation/engine/PhysicsSimulationEngineTest.kt`

**Test cases:**

1. `disarmedMotorsReportZeroRpm`
2. `armedTakeoffReportsNonZeroRpm`
3. `failedMotorReportsZeroRpm`
4. `missionAutoFlightReportsChangingRpm`

**Verification:**

Run:

```bash
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest --console=plain
```

## Task 5.4: Show RPM in Cockpit or SIM HUD

**Objective:** Make motor telemetry visible before full prop control is implemented.

**Files:**

- Modify: `DashboardScreen.kt` and/or `Drone3DScreen.kt`

**MVP:**

Show compact values:

```text
RPM: 1200 / 1190 / 1215 / 1185
```

or four small bars in SIM HUD.

---

# Phase 6: SIM Screen Functionalization

**Goal:** SIM is not decorative. It must reflect live simulation state from Controller and GCS Mission.

**Reference:**

- `docs/mavlab_product_surface_definition.md`, SIM section and 3D movement decision.
- `docs/mavlab_gcs_digital_twin_guidelines.md`, 3D Digital Twin Direction and Functional Drone Model Requirements.
- `feature/drone3d/Drone3DScreen.kt`
- `app/src/main/assets/models/drone.glb`

## Task 6.1: Rename user-facing SIM labels

**Objective:** UI says SIM, not Twin.

**Files:**

- Modify: `Drone3DScreen.kt`
- Modify: `MavLabAppShell.kt`
- Modify onboarding if not already done.

**Do not yet rename file/package unless doing a dedicated refactor.**

## Task 6.2: Create DroneModelController adapter

**Objective:** Separate simulation-to-model mapping from Compose screen layout.

**Files:**

- Create: `feature/drone3d/DroneModelController.kt`
- Modify: `Drone3DScreen.kt`

**Responsibilities:**

- apply body transform
- map state altitude/position to scene position/camera model
- later locate propeller nodes
- later rotate propellers by RPM
- later expose missing-node diagnostics

**Suggested initial API:**

```kotlin
class DroneModelController {
    fun bodyPosition(state: DroneState): Position
    fun bodyRotation(state: DroneState): Rotation
    fun rpmSummary(state: DroneState): String
}
```

If SceneView node mutation API is difficult, start with pure calculation functions and tests.

## Task 6.3: Implement SIM chase/inspection default

**Objective:** Drone remains readable while world/path/camera communicates movement.

**Files:**

- Modify: `Drone3DScreen.kt`
- Create helper if needed: `feature/drone3d/SimCameraController.kt`

**MVP behavior:**

- keep model near center
- still reflect altitude with slight vertical offset or camera framing
- show numeric north/east/altitude in HUD
- if adding grid/path is too much now, at least show mission target and current position text

**Do not:**

- let the drone disappear from camera as position grows
- make a fake static-only bench view as default

## Task 6.4: Add SIM HUD

**Objective:** SIM can be understood without jumping to Cockpit.

**Files:**

- Modify: `Drone3DScreen.kt`

**HUD fields:**

- `SIM`
- authority
- mode
- armed/disarmed
- altitude
- speed
- battery
- GPS
- active waypoint if mission loaded
- RPM values/bars if available

## Task 6.5: Replace global prop animation with RPM-driven prop behavior

**Objective:** Independent motors affect independent propellers.

**Files:**

- Modify: `Drone3DScreen.kt`
- Modify: `DroneModelController.kt`

**Reference node names from guidelines/current model:**

- `FrontLeftPropeller`
- `FrontRightPropeller`
- `RearLeftPropeller`
- `RearRightPropeller`

**Behavior:**

- disarmed: props stop
- armed + RPM > 0: props spin
- failed motor: matching prop stops/spins down
- different RPM values should be representable visually later

**Risk:**

SceneView node lookup/mutation may require inspection. If direct node control is difficult, keep global animation temporarily and finish motor telemetry/HUD first. Do not block the whole product on propeller node control.

---

# Phase 7: GCS Mission to SIM Integration

**Goal:** QGC mission is not just a list; it visibly drives the drone through physics and SIM.

**Reference:**

- `docs/mavlab_gcs_digital_twin_guidelines.md`, section 3.
- `core/mavlink/MavlinkUdpServer.kt`
- `core/mavlink/MissionUploadSession.kt`
- `simulation/autopilot/MissionEngine.kt`
- `simulation/engine/PhysicsSimulationEngine.kt`
- `feature/mission/MissionScreen.kt`
- `feature/drone3d/Drone3DScreen.kt`

## Task 7.1: Add mission upload status state

**Objective:** Mission/Ops can show upload progress and failures.

**Files:**

- Create: `simulation/mission/MissionUploadStatus.kt` or equivalent.
- Modify: `AppRuntime.kt` or `PhysicsSimulationEngine.kt`
- Modify: `MavlinkUdpServer.kt`

**State should include:**

- idle/uploading/accepted/rejected
- expected count
- received count
- last requested sequence
- last received sequence
- last error/reject reason

## Task 7.2: Show upload status in Mission

**Objective:** Mission screen explains what QGC upload is doing.

**Files:**

- Modify: `MissionScreen.kt`

**Examples:**

```text
Upload: Receiving 3/4 from QGC
Upload: Accepted 4 waypoints
Upload: Rejected invalid sequence 2
```

## Task 7.3: Ensure mission execution changes physics state

**Objective:** AUTO mission flight changes pitch/roll/yaw/position/RPM.

**Files:**

- Inspect/modify: `MissionEngine.kt`
- Inspect/modify: `PhysicsSimulationEngine.kt`
- Inspect/modify: `PositionController.kt`

**Acceptance:**

During AUTO mission:

- `DroneState.mode == AUTO`
- `controlAuthority == GCS_MISSION`
- active waypoint changes
- heading changes toward target
- altitude changes as required
- motor RPM changes from maneuvering
- SIM reflects these changes

## Task 7.4: Add mission path/target overlay to SIM

**Objective:** SIM communicates spatial mission progress.

**Files:**

- Modify: `Drone3DScreen.kt`
- Possibly create: `feature/drone3d/MissionOverlay.kt`

**MVP:**

- text marker: `WP 2 -> 12.5 m`
- line/path can wait if SceneView primitive drawing is slow

**Later:**

- path line
- active waypoint marker
- breadcrumb trail

---

# Phase 8: Flight Recording MVP

**Goal:** Log Controller and GCS flights locally for replay/debug/debrief.

**Reference:**

- `docs/mavlab_product_surface_definition.md`, Flight Logging Decision.
- `simulation/engine/DroneState.kt`
- `simulation/mission/Mission.kt`
- `AppRuntime.kt`
- `SimulationService.kt`

## Task 8.1: Create recording data models

**Files:**

- Create: `simulation/recording/FlightSession.kt`
- Create: `simulation/recording/FlightEvent.kt`

**FlightSession fields:**

```kotlin
data class FlightSession(
    val id: String,
    val startedAtMs: Long,
    val endedAtMs: Long? = null,
    val directoryPath: String,
)
```

**FlightEvent fields:**

```kotlin
data class FlightEvent(
    val timestampMs: Long,
    val type: String,
    val message: String,
)
```

## Task 8.2: Create FlightRecorder

**Files:**

- Create: `simulation/recording/FlightRecorder.kt`

**Storage path:**

```text
context.filesDir/mavlab/flights/{session_id}/
  manifest.json
  telemetry.csv
  events.jsonl
  mission.json
```

**Responsibilities:**

- start session
- append telemetry CSV at 5-10 Hz
- append events JSONL
- save mission snapshot when mission starts/upload accepted
- close session and write manifest

**Do not:**

- build cloud upload now
- write 100 Hz physics logs by default
- request broad external storage permissions

## Task 8.3: Integrate recorder lifecycle

**Files:**

- Modify: `SimulationService.kt` or `AppRuntime.kt`
- Modify: `PhysicsSimulationEngine.kt` if recorder samples state there.

**Start recording when:**

- drone arms
- takeoff command happens
- AUTO mission starts

**Stop recording when:**

- drone disarms after flight
- app/service shuts down
- future manual stop

## Task 8.4: Add recording indicator to Cockpit

**Files:**

- Modify: `DashboardScreen.kt`
- Possibly add state to `DroneState` or `AppRuntime`.

**MVP display:**

```text
Recording: Active
```

or

```text
Last log: mavlab/flights/2026...
```

## Task 8.5: Add recording tests

**Files:**

- Create: `app/src/test/java/com/ascend/mavlab/simulation/recording/FlightRecorderTest.kt`

**Tests:**

1. starts session and creates directory/files
2. writes telemetry header and row
3. appends event JSONL line
4. closes manifest

If Android `Context.filesDir` makes pure unit tests hard, abstract storage behind a base `File` injected into `FlightRecorder`.

---

# Phase 9: Ops Diagnostics

**Goal:** Ops becomes the place to debug QGC, mission upload, and logs.

**Reference:**

- `feature/settings/SettingsScreen.kt`
- `MavlinkUdpServer.kt`
- `FlightRecorder.kt`
- `docs/mavlab_product_surface_definition.md`, Ops section.

## Task 9.1: Rename user-facing Settings heading to Ops

**Files:**

- Modify: `SettingsScreen.kt`

**Target:**

```text
Ops
```

Subtitle:

```text
MAVLink, QGC, recording, and runtime diagnostics.
```

## Task 9.2: Show MAVLink diagnostics

**Fields:**

- UDP status
- system ID
- last inbound
- last ACK
- mission upload status
- QGC troubleshooting copy

## Task 9.3: Show last flight log summary

**Fields:**

- last session id
- start/end time or active recording
- file path in app storage if available
- export button later

**Do not implement Android share/export until local recording is stable.**

---

# Phase 10: Real-Device / QGC Acceptance Pass

**Goal:** Validate the actual product flow, not only unit tests.

## Task 10.1: Same-phone QGC test

**Setup:**

- MAVLab on Android
- QGroundControl on same phone, split-screen if possible

**Acceptance:**

1. QGC detects MAVLab within 3 seconds.
2. Cockpit shows GCS/listening status.
3. QGC can arm/disarm.
4. QGC can upload 4-waypoint mission.
5. Mission tab shows waypoints.
6. Start mission.
7. Cockpit shows `Control: GCS Mission`.
8. SIM shows attitude/position/RPM response.
9. QGC receives mission current/reached messages.
10. Flight log is written.

## Task 10.2: Desktop QGC test

**Setup:**

- Android phone and desktop on same Wi-Fi
- Desktop QGC

**Acceptance:**

Same as same-phone test, plus reconnect after app restart.

## Task 10.3: Controller manual test

**Acceptance:**

1. Open Controller.
2. Select custom input.
3. Arm/takeoff.
4. Cockpit shows `Control: Controller`.
5. Change roll/pitch/yaw/throttle.
6. SIM responds.
7. Toggle motor failure in advanced section.
8. SIM/Cockpit reflect the failure.
9. Flight log is written.

## Task 10.4: Regression build

Run:

```bash
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest lintDebug assembleDebug --console=plain
```

Expected:

```text
BUILD SUCCESSFUL
```

---

# Final Definition of Done

The implementation is done when:

- Bottom nav is exactly: Cockpit, Controller, Mission, SIM, Ops.
- No MVP user-facing text says Fly, Twin, or Systems as a tab name.
- Systems is removed from main navigation but useful code is not recklessly deleted.
- Mission is first-class and displays QGC/demo mission progress.
- Cockpit shows control authority.
- Controller supports phone sensors and custom/manual input without clutter.
- Controller advanced section contains only lightweight test perturbations.
- QGC commands and missions set correct authority.
- Controller and GCS Mission both drive physics state, not fake UI behavior.
- SIM reflects state: attitude, position/altitude, authority, mission status, and motor RPM when implemented.
- SIM default view is chase/inspection, not “drone flies away forever” and not “static fake bench only.”
- Flight logs are written locally for both Controller and GCS sessions.
- Ops exposes MAVLink and recording diagnostics.
- Unit tests pass.
- Full build/lint/assemble passes.
- Real QGC same-phone and desktop tests have been performed.

---

# Conductor Notes for AI Agents

1. Do not improvise product names. Use `Controller` and `SIM`.
2. Do not resurrect Systems in MVP navigation.
3. Do not fake SIM movement from UI controls. SIM must consume `DroneState`.
4. Do not build cloud logging now.
5. Do not delete lesson/lab code unless explicitly requested; hide/defer first.
6. After every phase, run tests.
7. After every major behavior phase, update `docs/mavlab_product_surface_definition.md` if reality diverges.
8. If SceneView node-level prop control blocks progress, ship motor telemetry + SIM HUD first, then return to node animation.
9. If QGC behavior differs from MAVLink theory, follow QGC and document the observed packet flow in Ops/diagnostics.
