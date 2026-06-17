# MAVLab v1.5 Execution Plan

> **For Hermes / future agents:** Use the `subagent-driven-development` skill to implement this plan phase-by-phase. Do not improvise product direction. Follow the naming, architecture invariants, acceptance tests, and file paths below.

**Goal:** Ship MAVLab v1.5 as a polished, demo-ready, bootcamp-ready Android drone digital twin and training app.

**Product thesis:** MAVLab v1.5 should make the current app feel credible, teachable, and impressive before expanding into bigger v2 systems. It should polish the core loop: onboard learner -> connect/control/simulate -> run mission -> inject failure -> review/export flight.

**Reference roadmap:** `/home/ambrose/Documents/Obsidian Vault/Ascend Operating System/Ascend Labs/MAVLab_Future_Roadmap.md`, section `# 1. MAVLab v1.5 — Polish the Current Core`.

**Codebase:** `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/`

**Tech stack:** Android, Kotlin, Jetpack Compose, foreground simulation service, MAVLink v2 over UDP, QGroundControl, SceneView/3D model rendering, local file export.

---

## 0. Non-Negotiable Product Decisions

### 0.1 Product positioning

MAVLab is not “just a simulator.”

Use this framing everywhere:

> MAVLab is a phone-based drone digital twin and training platform.

v1.5 must strengthen this through polish, explainability, visual feedback, QGroundControl readiness, flight review, and export.

### 0.2 MVP user-facing tabs

Use these app surfaces for v1.5:

1. `Cockpit`
2. `Controller`
3. `Mission`
4. `SIM`
5. `Ops`

Rules:

- Use `Controller`, not `Fly`.
- Use `SIM`, not `Twin`, as the tab name.
- The term “digital twin” remains valid in copy and docs, but not as the primary tab label.
- Remove or hide `Systems` from MVP navigation.
- Existing failure/lab code should not be deleted blindly. Hide, migrate, or refactor useful parts into `Controller`, `Mission`, `SIM`, or `Ops`.

### 0.3 Core architecture invariant

Never animate the 3D SIM directly from UI controls.

Correct flow:

```text
Controller input or QGC command
  -> PhysicsSimulationEngine / MissionEngine / Autopilot / FailureInjector
  -> DroneState
  -> Cockpit + Controller + Mission + SIM + Ops + FlightRecorder
```

Wrong flow:

```text
Controller slider
  -> directly rotate 3D model
```

Both phone Controller input and QGroundControl/MAVLink mission input must update the same simulation state.

### 0.4 Control authority must be explicit

v1.5 should make control authority visible in Cockpit, SIM, Mission, and logs.

Expected authority model:

```kotlin
enum class ControlAuthority(val displayName: String) {
    IDLE("Idle"),
    CONTROLLER("Controller"),
    GCS_DIRECT("GCS Direct"),
    GCS_MISSION("GCS Mission"),
}
```

Rules:

- Local phone/manual control -> `CONTROLLER`.
- QGroundControl arm/takeoff/land/mode command without active mission -> `GCS_DIRECT`.
- QGroundControl AUTO mission execution -> `GCS_MISSION`.
- Controller inputs should warn, disable, or de-emphasize manual controls while `GCS_MISSION` is active.

---

## 1. v1.5 Scope Summary

v1.5 is a polish and execution release. It should not become a giant v2 rebuild.

### Must ship

1. Better onboarding
2. Better Cockpit UI
3. Better SIM/3D visualization
4. Better Mission view
5. Better failure lab / scenario explanations
6. Better export and flight review
7. QA checklist and demo script
8. Real QGroundControl validation

### Should ship if time allows

1. Mission replay MVP
2. Bootcamp demo scenario pack
3. Student performance report MVP
4. Screenshot/export polish
5. Public “MAVLab by Ascend Labs” README section

### Defer to v2

1. Full instructor dashboard
2. Classroom multiplayer
3. AI flight debrief
4. Medical logistics route builder
5. Advanced battery chemistry model
6. Pixhawk/USB/hardware bridge
7. Cloud dashboard
8. AR mode

---

## 2. Current Codebase Map

Use these files as the main implementation anchors.

### App shell and navigation

- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/MavLabApp.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/MainActivity.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/feature/navigation/MavLabAppShell.kt`

### Runtime/service/state

- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/core/common/AppRuntime.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/service/SimulationService.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/engine/DroneState.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/engine/ControlAuthority.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/engine/PhysicsSimulationEngine.kt`

### Controller

- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/feature/controller/ControllerScreen.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/feature/controller/ControlMapper.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/feature/controller/ControllerInputMode.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/core/sensors/PhoneSensorRepository.kt`

### Cockpit / telemetry dashboard

- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/feature/dashboard/DashboardScreen.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/core/ui/components/TelemetryCard.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/core/ui/components/RollingChart.kt`

### SIM / 3D view

- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/feature/drone3d/Drone3DScreen.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/feature/drone3d/DroneModelController.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/feature/drone3d/AltitudeInstrument.kt`

### Mission/MAVLink/QGroundControl

- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/feature/mission/MissionScreen.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/mission/Mission.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/mission/MissionUploadStatus.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/mission/MissionSnapshotCodec.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/autopilot/MissionEngine.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkUdpServer.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkMessageBuilder.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MissionUploadSession.kt`

### Failures and lessons

- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/failures/FailureScenario.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/failures/FailureInjector.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/feature/labs/LabsScreen.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/feature/lessons/LessonCatalog.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/feature/lessons/LessonScreen.kt`

### Logging/export

- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/recording/FlightRecorder.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/recording/FlightEvent.kt`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/recording/FlightSession.kt`

### Tests

- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/test/java/com/ascend/mavlab/`

### Docs

- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/README.md`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/docs/architecture.md`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/docs/test_matrix.md`
- `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/docs/protocol_guardrails.md`

---

## 3. Phase Breakdown

## Phase 0 — Baseline Audit and Release Definition

**Purpose:** Freeze what v1.5 means before coding, identify current gaps, and create a measurable checklist.

**Outcome:** A clear v1.5 implementation checklist, current-state notes, and passing baseline tests.

### Tasks

1. Run baseline build and tests.
   - Command:
     ```bash
     cd "/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android"
     GRADLE_USER_HOME="$PWD/.gradle" ./gradlew lintDebug testDebugUnitTest assembleDebug
     ```
   - Expected: build, lint, unit tests, and debug APK succeed.

2. Review current navigation.
   - Inspect `MavLabAppShell.kt`.
   - Confirm current tabs.
   - Decide whether `Labs`, `Lessons`, `Settings`, or old phase surfaces remain in primary nav or move under `Ops`/secondary menus.

3. Review runtime state shape.
   - Inspect `DroneState.kt`, `ControlAuthority.kt`, `AppRuntime.kt`.
   - Confirm whether all v1.5 UI surfaces can read the same source of truth.

4. Review QGroundControl mission upload path.
   - Inspect `MavlinkUdpServer.kt`, `MissionUploadSession.kt`, `MissionEngine.kt`, `MissionScreen.kt`.
   - Confirm support for `MISSION_COUNT`, `MISSION_REQUEST_INT`, `MISSION_ITEM_INT`, `MISSION_ACK`, `MISSION_CURRENT`, and `MISSION_ITEM_REACHED`.

5. Review current flight logging.
   - Inspect `FlightRecorder.kt`, `FlightEvent.kt`, `FlightSession.kt`.
   - Confirm current file outputs and missing export/report features.

6. Create or update a v1.5 checklist doc.
   - Suggested path: `/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/docs/v1_5_checklist.md`
   - Include every acceptance criterion from this plan.

### Acceptance criteria

- Baseline build result is known.
- Existing failures are documented before changes.
- v1.5 checklist exists in repo docs.
- No coding begins before the target scope is clear.

---

## Phase 1 — Navigation, Product Surface, and Copy Polish

**Purpose:** Make the app feel intentionally designed around the v1.5 product surfaces.

**Outcome:** Primary navigation uses the v1.5 tab model: `Cockpit`, `Controller`, `Mission`, `SIM`, `Ops`.

### Tasks

1. Update app shell tab model.
   - Modify: `feature/navigation/MavLabAppShell.kt`
   - Replace legacy/unclear tab labels with:
     - `Cockpit`
     - `Controller`
     - `Mission`
     - `SIM`
     - `Ops`

2. Map existing screens into surfaces.
   - `DashboardScreen.kt` -> Cockpit.
   - `ControllerScreen.kt` -> Controller.
   - `MissionScreen.kt` -> Mission.
   - `Drone3DScreen.kt` -> SIM.
   - MAVLink diagnostics, settings, logs, protocol status -> Ops.

3. Hide `Systems` or old lab-heavy navigation from MVP.
   - Do not delete useful failure logic.
   - If `LabsScreen.kt` remains, link it from Ops or convert its best presets into Controller/Mission failure controls.

4. Add consistent product copy.
   - Use the phrase: “phone-based drone digital twin and training platform.”
   - Avoid generic “demo simulator” wording.
   - Add “MAVLab by Ascend Labs” in the intro/about area.

5. Add v1.5 visual hierarchy rules.
   - Cockpit = live operations.
   - Controller = local/manual control.
   - Mission = autonomous route execution.
   - SIM = physical behavior visualization.
   - Ops = diagnostics, logs, export, settings.

### Acceptance criteria

- The app opens into a coherent v1.5 shell.
- Primary tab labels match the agreed names exactly.
- `Systems` is not a primary MVP tab.
- User can understand what each tab is for within 5 seconds.

---

## Phase 2 — Onboarding Upgrade

**Purpose:** Make a first-time learner understand what MAVLab is and how to use it without Ambrose explaining it live.

**Outcome:** A guided first-run experience that teaches the product story and the core workflow.

### Key files

- `feature/onboarding/OnboardingScreen.kt`
- `feature/onboarding/OnboardingPages.kt`
- `core/settings/AppSettings.kt` if onboarding completion is persisted there
- `README.md` for matching public wording

### Onboarding flow

Build these pages:

1. What is MAVLab?
   - MAVLab is a phone-based drone digital twin and training platform by Ascend Labs.

2. What is a drone digital twin?
   - Explain that the app simulates drone state, sensors, telemetry, mission behavior, and failures.

3. Understand the app surfaces.
   - Cockpit, Controller, Mission, SIM, Ops.

4. Connect to QGroundControl.
   - Explain same-phone and desktop same-Wi-Fi workflows.

5. First simulated takeoff.
   - Tell the learner where arm/takeoff/land controls live.

6. Try phone tilt control.
   - Explain calibration and manual fallback.

7. Run a basic mission.
   - Explain mission upload, waypoint progress, and AUTO mode.

8. Inject a simple failure.
   - GPS loss, wind drift, or low battery.

9. Review/export flight.
   - Explain logs, CSV, replay/report outputs.

### Implementation notes

- Add a `Skip for now` action.
- Add a `Replay onboarding` option under Ops or Settings.
- Keep copy practical and short.
- Use real tab names and avoid future-only features.

### Tests

Add/update unit tests if onboarding page data is pure Kotlin.

Suggested test:

- `OnboardingPagesTest.kt`
  - verifies the required v1.5 pages exist
  - verifies tab names are correct
  - verifies no old `Twin` / `Fly` naming appears

### Acceptance criteria

- First-run user can understand MAVLab’s purpose.
- Onboarding covers QGC, takeoff, phone control, mission, failure, and export.
- Onboarding can be replayed.
- Copy matches Ascend Labs positioning.

---

## Phase 3 — Cockpit UI Polish

**Purpose:** Make the Cockpit feel like a real drone operations interface, not just debug cards.

**Outcome:** Cockpit clearly shows live flight status, safety state, MAVLink status, and control authority.

### Key files

- `feature/dashboard/DashboardScreen.kt`
- `core/ui/components/TelemetryCard.kt`
- `core/ui/components/RollingChart.kt`
- `simulation/engine/DroneState.kt`
- `simulation/engine/ControlAuthority.kt`

### Cockpit components to add or improve

1. Artificial horizon / attitude indicator
   - Inputs: roll, pitch, yaw from `DroneState`.
   - Keep it simple but visually credible.

2. Altitude tape
   - Input: altitude.
   - Show current altitude, climb/descent direction.

3. Vertical speed indicator
   - Input: vertical speed if available, otherwise derive from altitude delta carefully.

4. Battery gauge
   - Inputs: battery percent, voltage/current if available.
   - States: normal, low, critical.

5. GPS status
   - Show lock/no lock, simulated accuracy if available.

6. MAVLink connection status
   - Show whether QGC is connected/recently heard.
   - Show local UDP port and peer if available.

7. Flight mode badge
   - Manual/Stabilize/Loiter/Auto/RTL/Land as supported.

8. Armed/disarmed state
   - Must be obvious and safety-colored.

9. Ground speed and distance from home
   - Use available state or compute from position/home.

10. Current mission item
   - Show active waypoint or “No active mission.”

11. Control authority indicator
   - Values: Idle, Controller, GCS Direct, GCS Mission.
   - This must be visible above the fold.

### UI direction

- Use cockpit-style hierarchy: status row, primary instruments, mission/connection strip, detailed cards.
- Avoid clutter.
- Warnings should be readable in a demo video.
- Teach through labels: e.g. “GPS: Locked”, “Authority: GCS Mission”.

### Tests

Suggested tests:

- `ControlAuthorityTest.kt` updates if authority rules change.
- Pure-formatting tests for display labels if helper functions are extracted.

### Acceptance criteria

- A screenshot of Cockpit immediately communicates drone state.
- Authority, battery, GPS, armed state, flight mode, and MAVLink status are visible.
- Cockpit updates from `DroneState`, not duplicate local UI state.

---

## Phase 4 — SIM / 3D Visualization Upgrade

**Purpose:** Make the SIM screen teach physical drone behavior through visual state.

**Outcome:** The drone visually reacts to simulated state: attitude, altitude, motor RPM, failures, mission context, and flight path.

### Key files

- `feature/drone3d/Drone3DScreen.kt`
- `feature/drone3d/DroneModelController.kt`
- `feature/drone3d/AltitudeInstrument.kt`
- `simulation/engine/DroneState.kt`
- `simulation/engine/MotorTelemetry.kt`
- `simulation/physics/MotorMixer.kt`
- `simulation/failures/FailureInjector.kt`

### SIM requirements

1. Hybrid chase/inspection view
   - Drone stays readable near the center.
   - Camera/background/grid/path communicates motion.
   - Drone must not simply fly out of frame.

2. Body attitude
   - Roll, pitch, yaw must map from `DroneState` to 3D model orientation.

3. Propeller spin
   - Propeller speed should respond to simulated motor RPM.
   - Failed motor visibly stops or slows.
   - If state exposes rad/s, convert with:
     ```text
     rpm = rad_per_second * 60 / (2π)
     ```

4. Altitude reference
   - Add altitude ruler, vertical marker, shadow, or simple gauge.

5. Ground grid
   - Provide visual movement/height reference.

6. Flight path trail
   - Show recent path behind the drone.
   - Cap trail length for performance.

7. Mission waypoint markers
   - Show home, active waypoint, and remaining waypoints if mission exists.

8. Return-to-launch path
   - If RTL is triggered/simulated, show the return line or home target.

9. Color-coded aircraft state
   - Normal
   - Warning
   - Critical
   - Failure

10. SIM HUD
   - Authority
   - Mode
   - Armed state
   - Altitude
   - Speed
   - Battery
   - GPS
   - Active waypoint
   - Motor/failure indicators

### Architecture notes

Create/maintain a model adapter shape like:

```text
DroneModelController
  setBodyAttitude(roll, pitch, yaw)
  setPosition(north, east, altitude)
  setMotorRpm(index, rpm)
  setMotorFailed(index, failed)
  setBatteryPercent(percent)
  setGpsStatus(lock/no_lock)
  setPayloadMass(kg)
  setWindVector(...)
  setMissionTarget(...)
```

The adapter should protect the app from future GLB replacement.

### Tests

Suggested tests:

- `DroneModelControllerTest.kt`
  - verifies attitude mapping
  - verifies motor RPM conversion
  - verifies failed motor state
  - verifies warning/critical/failure visual state mapping

### Acceptance criteria

- SIM makes telemetry feel physical.
- Propellers respond to simulated motor state.
- Failed motor/GPS/battery state is visually obvious.
- Mission waypoints and home marker are visible when mission exists.
- SIM reads from the common simulation state.

---

## Phase 5 — Mission View and QGroundControl Workflow Polish

**Purpose:** Make autonomous mission execution understandable and demo-safe.

**Outcome:** Mission tab clearly shows upload status, waypoint list, progress, ETA, current objective, and replay/export hooks.

### Key files

- `feature/mission/MissionScreen.kt`
- `simulation/mission/Mission.kt`
- `simulation/mission/MissionUploadStatus.kt`
- `simulation/mission/MissionSnapshotCodec.kt`
- `simulation/autopilot/MissionEngine.kt`
- `core/mavlink/MavlinkUdpServer.kt`
- `core/mavlink/MissionUploadSession.kt`
- `core/mavlink/MavlinkMessageBuilder.kt`

### Mission tab elements

1. Mission upload status
   - Idle
   - Receiving mission
   - Mission accepted
   - Mission rejected
   - Mission running
   - Mission complete

2. Mission list
   - Sequence number
   - Command/type
   - Latitude/longitude or local coordinates
   - Altitude
   - Target speed if available

3. Current waypoint
   - Highlight active item.

4. Waypoint progress
   - Reached / total.

5. Distance to next waypoint
   - Useful for teaching mission monitoring.

6. Mission speed
   - Current ground speed and target speed if available.

7. ETA
   - Simple estimate is acceptable for v1.5.

8. Current objective
   - “Takeoff”, “Fly to WP2”, “Return to launch”, “Land”, etc.

9. Completion percentage
   - Based on waypoint progress or route distance.

10. Mission replay entry point
   - If replay MVP ships, link to replay.
   - If not, show “Replay available after export/review” only when implemented.

### QGroundControl acceptance workflow

Must support and test:

1. QGC sends `MISSION_COUNT`.
2. MAVLab starts upload session.
3. MAVLab requests item 0 using `MISSION_REQUEST_INT` where possible.
4. QGC sends `MISSION_ITEM_INT` or `MISSION_ITEM`.
5. MAVLab stores item and requests next sequence.
6. MAVLab sends `MISSION_ACK` after all items arrive.
7. Mission appears in Mission tab.
8. AUTO mode flies uploaded waypoints.
9. MAVLab sends `MISSION_CURRENT` and `MISSION_ITEM_REACHED` during execution.
10. Reconnect does not destroy the uploaded mission unexpectedly.

### Tests

Existing tests to extend:

- `MissionUploadSessionTest.kt`
- `MissionUploadStatusTest.kt`
- `MissionEngineTest.kt`
- `MissionSnapshotCodecTest.kt`
- `MavlinkMessageBuilderTest.kt`

Add tests for:

- partial upload failure
- mission clear
- out-of-order item rejection or recovery
- mission progress percentage
- ETA formatting
- uploaded mission persistence if supported

### Acceptance criteria

- User can upload a QGC mission and see it in-app.
- User can understand which waypoint is active.
- User can explain mission progress using the UI.
- Real QGC same-phone and desktop-Wi-Fi tests pass.

---

## Phase 6 — Failure Lab / Scenario Explanation Polish

**Purpose:** Turn failures from toggles into teaching moments.

**Outcome:** Each failure preset explains what happened, what telemetry changed, what the operator should do, and what safety lesson it teaches.

### Key files

- `simulation/failures/FailureScenario.kt`
- `simulation/failures/FailureInjector.kt`
- `feature/labs/LabsScreen.kt`
- `feature/controller/ControllerScreen.kt`
- `feature/mission/MissionScreen.kt`
- `simulation/recording/FlightEvent.kt`

### v1.5 failure presets

Implement or polish these presets:

1. GPS loss
2. Low battery
3. Critical battery
4. Compass interference
5. Wind drift
6. Motor weakness
7. Lost link
8. Payload overweight
9. Barometer issue
10. Unsafe mission reserve

### For each preset, define

1. Name
2. Severity
3. What happened
4. Why it happened
5. Telemetry signature
6. Operator response
7. Safety lesson
8. Recovery condition or reset behavior
9. Whether it affects mission state, controller state, or both
10. Log event name

### Suggested data shape

```kotlin
data class FailureExplanation(
    val title: String,
    val severity: FailureSeverity,
    val whatHappened: String,
    val whyItMatters: String,
    val telemetrySignature: List<String>,
    val operatorResponse: List<String>,
    val safetyLesson: String,
)
```

### UI placement

- Controller: keep only lightweight quick toggles under `Advanced test inputs`.
- Mission: show mission-relevant warnings and unsafe mission reserve.
- Ops or Labs: full scenario catalog/details.
- Cockpit/SIM: show active failure state clearly.

### Tests

Add/update tests for:

- failure preset metadata completeness
- injector changes expected simulation state
- failure events are recorded
- reset clears active failures

### Acceptance criteria

- Each failure teaches, not just breaks the drone.
- Active failures are visible in Cockpit and SIM.
- Flight logs capture failure injection/restoration events.
- Failure reset works reliably.

---

## Phase 7 — Flight Logging, Export, and Review

**Purpose:** Make MAVLab useful for bootcamp assessment, debugging, and public demonstrations.

**Outcome:** Every flight session can be reviewed and exported with telemetry, events, mission data, and a human-readable summary/report.

### Key files

- `simulation/recording/FlightRecorder.kt`
- `simulation/recording/FlightEvent.kt`
- `simulation/recording/FlightSession.kt`
- `feature/mission/MissionScreen.kt`
- `feature/navigation/MavLabAppShell.kt` or Ops screen destination
- Add new export/review UI under Ops if needed

### Local storage target

Use app-private storage for MVP:

```text
context.filesDir/mavlab/flights/{session_id}/
  manifest.json
  telemetry.csv
  events.jsonl
  mission.json
  report.md
```

### Telemetry CSV fields

Capture at 5–10 Hz, not every physics tick.

Recommended fields:

- timestamp
- session_id
- control_authority
- armed
- mode
- latitude/local_north
- longitude/local_east
- altitude
- ground_speed
- vertical_speed
- heading
- roll
- pitch
- yaw
- throttle
- battery_percent
- battery_voltage if available
- gps_status
- motor_rpm_1
- motor_rpm_2
- motor_rpm_3
- motor_rpm_4
- active_waypoint
- failure_flags

### Event JSONL types

Capture:

- app/session started
- QGC connected
- mission upload started
- mission item received
- mission accepted/rejected
- arm/disarm
- takeoff/land
- mode changed
- mission started
- waypoint reached
- mission completed
- authority changed
- motor failure injected/restored
- GPS lost/restored
- battery warning/critical
- export generated

### Report MVP

Generate a simple Markdown report:

```markdown
# MAVLab Flight Report

## Summary
- Date/time:
- Duration:
- Control authority used:
- Mission status:
- Max altitude:
- Distance travelled:
- Battery start/end:
- Failures injected:

## Timeline
- ...

## Safety Notes
- ...

## Student / Operator Notes
- ...
```

### Export UI

Under `Ops`, provide:

- View last session
- Export telemetry CSV
- Export event log
- Export mission snapshot
- Export report
- Share/export folder if Android permissions allow

### Tests

Existing test:

- `FlightRecorderTest.kt`

Extend tests for:

- manifest creation
- telemetry CSV header/schema
- event JSONL writing
- mission snapshot export
- report generation
- sampling rate behavior

### Acceptance criteria

- A completed flight produces local artifacts.
- CSV can be opened externally.
- Report is understandable by an instructor or student.
- Failure and mission events appear in the timeline.
- Export works offline.

---

## Phase 8 — QA, Demo Script, Documentation, and Release Packaging

**Purpose:** Make v1.5 demonstrable to bootcamp students, partners, and the Ascend Labs audience.

**Outcome:** v1.5 has a repeatable demo, test checklist, docs, APK build, and public-facing explanation.

### Key files

- `README.md`
- `docs/test_matrix.md`
- `docs/protocol_guardrails.md`
- Add: `docs/v1_5_demo_script.md`
- Add: `docs/v1_5_qgc_acceptance.md`
- Add: `docs/v1_5_release_notes.md`

### Demo script

Create a 7–10 minute demo:

1. Open MAVLab and explain the positioning.
2. Show Cockpit and SIM idle state.
3. Arm and perform first simulated takeoff.
4. Switch to Controller and show phone/manual input.
5. Show SIM responding to attitude and motor state.
6. Connect QGroundControl.
7. Upload a small mission.
8. Start AUTO mission.
9. Show Mission progress and SIM waypoint markers.
10. Inject GPS loss or wind drift.
11. Explain failure telemetry and operator response.
12. Complete/abort/land safely.
13. Export flight report and telemetry CSV.

### QGC acceptance tests

Test on:

1. Same Android phone split-screen.
2. Desktop QGroundControl on same Wi-Fi.

For both, verify:

- Heartbeat/telemetry visible.
- Arm/disarm works.
- Takeoff/land commands are acknowledged.
- Mission upload succeeds.
- Mission appears in MAVLab Mission tab.
- AUTO mission progresses.
- QGC receives mission current/reached updates.
- Reconnect works.
- MAVLab remains stable for at least 10 minutes.

### Release build commands

```bash
cd "/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android"
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew lintDebug testDebugUnitTest assembleDebug
```

If release signing is configured:

```bash
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew assembleRelease
```

### Documentation updates

Update README with:

- MAVLab by Ascend Labs positioning
- v1.5 features
- build instructions
- QGC connection instructions
- demo script link
- export/logging explanation

Update architecture docs with:

- v1.5 surface model
- state flow invariant
- control authority model
- logging/export model

### Acceptance criteria

- Fresh build passes.
- Test matrix is updated.
- Demo script is reproducible.
- QGC acceptance tests are documented.
- v1.5 release notes exist.
- APK can be installed on a phone.

---

## 4. Implementation Order

Recommended order:

1. Phase 0 — Baseline Audit and Release Definition
2. Phase 1 — Navigation, Product Surface, and Copy Polish
3. Phase 2 — Onboarding Upgrade
4. Phase 3 — Cockpit UI Polish
5. Phase 4 — SIM / 3D Visualization Upgrade
6. Phase 5 — Mission View and QGroundControl Workflow Polish
7. Phase 6 — Failure Lab / Scenario Explanation Polish
8. Phase 7 — Flight Logging, Export, and Review
9. Phase 8 — QA, Demo Script, Documentation, and Release Packaging

Reasoning:

- Navigation and naming should stabilize first so every later feature lands in the right place.
- Onboarding should reflect the final surface model.
- Cockpit/SIM/Mission are the core demo loop and should be improved before export/reporting.
- Failure explanations depend on Cockpit/SIM/logging state visibility.
- Export and reports become more valuable after missions/failures are properly represented.
- QA/docs must happen after feature behavior is stable.

---

## 5. Suggested Milestones

### Milestone A — v1.5 Shell Ready

Includes:

- Phase 0
- Phase 1
- Phase 2

Demo value:

- App now feels intentionally branded and understandable.

### Milestone B — v1.5 Flight Experience Ready

Includes:

- Phase 3
- Phase 4
- Phase 5

Demo value:

- Cockpit, SIM, and Mission work together as a credible drone training loop.

### Milestone C — v1.5 Teaching + Export Ready

Includes:

- Phase 6
- Phase 7

Demo value:

- Failures become lessons; flights produce reviewable artifacts.

### Milestone D — v1.5 Release Candidate

Includes:

- Phase 8

Demo value:

- Repeatable bootcamp/partner demo with APK, docs, QGC proof, and release notes.

---

## 6. Testing Strategy

### Unit tests

Run frequently:

```bash
cd "/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android"
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest
```

Priority test areas:

- Mission upload state machine
- Mission progress and ETA
- Control authority transitions
- Drone state mapping
- Failure injector behavior
- Flight recorder/export schema
- Onboarding page data

### Lint/build

Run before merging each milestone:

```bash
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew lintDebug assembleDebug
```

### Full local gate

Run before release candidate:

```bash
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew lintDebug testDebugUnitTest assembleDebug
```

### Manual Android tests

- First-run onboarding
- Replay onboarding
- Controller tilt mode
- Manual fallback controls
- Arm/takeoff/land
- Cockpit telemetry readability
- SIM visual response
- Failure injection/reset
- Mission upload from QGC
- Mission execution
- Export artifacts

### Real-world QGC tests

- Same-phone QGC split-screen
- Desktop QGC on same Wi-Fi
- Reconnect after closing/reopening QGC
- 10-minute stability run
- Mission upload/execute/land

---

## 7. Definition of Done for MAVLab v1.5

v1.5 is done when:

1. App navigation uses `Cockpit`, `Controller`, `Mission`, `SIM`, `Ops`.
2. Onboarding explains MAVLab, QGC, takeoff, controller, mission, failure, and export.
3. Cockpit shows attitude, altitude, battery, GPS, MAVLink, flight mode, armed state, speed, mission item, and authority.
4. SIM visually reflects drone attitude, altitude, motor state, failures, mission path, waypoints, and home.
5. Mission tab shows upload status, waypoint list, active waypoint, progress, distance, speed, ETA, and objective.
6. Failure presets include explanations, telemetry signatures, operator response, and safety lessons.
7. Flights produce telemetry CSV, event log, mission snapshot, and a simple report.
8. Same-phone QGC and desktop QGC workflows are tested and documented.
9. Demo script exists and can be followed without improvisation.
10. Build/test command passes:
    ```bash
    GRADLE_USER_HOME="$PWD/.gradle" ./gradlew lintDebug testDebugUnitTest assembleDebug
    ```
11. README and docs describe MAVLab as an Ascend Labs phone-based drone digital twin and training platform.

---

## 8. Risks and Mitigations

### Risk: v1.5 scope becomes v2

Mitigation:

- Keep AI, classroom multiplayer, cloud dashboard, medical logistics route builder, and hardware bridge out of v1.5.
- Only add hooks/docs for them where necessary.

### Risk: SIM becomes fake animation

Mitigation:

- Enforce state flow invariant.
- Add tests for `DroneModelController` mapping.
- Never wire Controller UI directly to 3D transforms.

### Risk: QGroundControl looks connected but missions fail

Mitigation:

- Test full mission upload and execution, not just telemetry.
- Document `MISSION_COUNT` -> `MISSION_REQUEST_INT` -> `MISSION_ITEM_INT` -> `MISSION_ACK` behavior.

### Risk: UI becomes too cluttered

Mitigation:

- Cockpit uses hierarchy: key state first, details second.
- Advanced failure controls stay collapsed.
- Ops holds diagnostics/export instead of polluting Cockpit.

### Risk: Android export permissions become painful

Mitigation:

- Start with app-private storage.
- Add share/export intent only after local artifacts are reliable.

---

## 9. Future v2 Bridge

v1.5 should prepare but not implement the full v2 direction:

> MAVLab v2: Bootcamp + Digital Twin Edition

v1.5 should leave clean seams for:

- AI Flight Debrief
- Instructor mode
- Bootcamp assessment module
- Medical delivery scenario pack
- Payload/range simulator
- More realistic battery model
- Telemetry replay UI
- Web/cloud dashboard later

The most important v1.5 contribution to v2 is not raw feature count. It is a clean, credible, demo-ready core loop.

---

## 10. Execution Handoff

Recommended implementation method:

1. Implement one phase at a time.
2. For each phase, create a small branch or commit series.
3. Start with tests where practical.
4. Run unit tests after each major change.
5. Run the full local gate at each milestone.
6. Do a real Android/QGC test before calling Mission or Release phases done.
7. Update docs as features land, not at the very end.

Suggested branch names:

- `v1.5-shell-onboarding`
- `v1.5-cockpit-sim`
- `v1.5-mission-qgc`
- `v1.5-failures-export`
- `v1.5-release-candidate`

Suggested commit style:

- `docs: add mavlab v1.5 checklist`
- `feat: align primary navigation with v1.5 surfaces`
- `feat: expand first-run onboarding for mavlab v1.5`
- `feat: add cockpit authority and flight status instruments`
- `feat: map simulation state into sim visual indicators`
- `feat: improve mission progress and qgc upload visibility`
- `feat: add failure explanations and operator response guidance`
- `feat: export flight telemetry and report artifacts`
- `docs: add mavlab v1.5 demo script and qgc acceptance tests`
