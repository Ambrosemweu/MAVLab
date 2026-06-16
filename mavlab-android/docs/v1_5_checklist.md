# MAVLab v1.5 Checklist

Phase 0 audit date: 2026-06-16

## Release Definition

MAVLab v1.5 is a polish and execution release for the current Android app.
The product positioning is:

> MAVLab is a phone-based drone digital twin and training platform by Ascend Labs.

The MVP primary surfaces are fixed as:

1. Cockpit
2. Controller
3. Mission
4. SIM
5. Ops

The release must preserve the core state-flow invariant:

```text
Controller input or QGC command
  -> PhysicsSimulationEngine / MissionEngine / Autopilot / FailureInjector
  -> DroneState
  -> Cockpit + Controller + Mission + SIM + Ops + FlightRecorder
```

The SIM must not be animated directly from UI controls.

## Phase 0 Baseline Result

Command run from `mavlab-android`:

```bash
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew lintDebug testDebugUnitTest assembleDebug
```

Result:

- Status: Passed
- Duration: about 2 minutes
- Tasks: 51 actionable tasks, 8 executed, 43 up-to-date
- Lint report: `app/build/reports/lint-results-debug.html`

No build, lint, or unit-test failures were present before v1.5 implementation
work began.

## Current-State Audit

### Navigation

- `feature/navigation/MavLabAppShell.kt` already exposes the v1.5 primary tab
  labels: `Cockpit`, `Controller`, `Mission`, `SIM`, and `Ops`.
- `Systems` is not a primary tab.
- `DashboardScreen` maps to Cockpit.
- `ControllerScreen` maps to Controller.
- `MissionScreen` maps to Mission.
- `Drone3DScreen` maps to SIM.
- `SettingsScreen` currently acts as Ops.
- Ops already shows MAVLink, QGC identity, recording, troubleshooting, and QA
  diagnostics.

Current gaps for later phases:

- Ops is still implemented as `SettingsScreen`; later phases should decide
  whether to rename or split this destination.
- Replay onboarding is not visible in Ops yet.
- Failure/lab surfaces are not yet part of the v1.5 navigation model.

### Runtime State

- `DroneState` is the shared state shape for armed state, flight mode, control
  authority, position, attitude, velocities, battery, GPS, motors, and MAVLink
  diagnostic labels.
- `ControlAuthority` already defines `IDLE`, `CONTROLLER`, `GCS_DIRECT`, and
  `GCS_MISSION` with the expected display names.
- `AppRuntime` exposes shared flows for drone state, failures, mission progress,
  mission upload status, recording status, phone sensors, MAVLink status, and
  MAVLink identity status.
- Phone sensor pilot input is suppressed while an armed `GCS_MISSION` owns
  control.
- Flight recording watches shared `DroneState`, mission progress, mode changes,
  and authority changes.

Current gaps for later phases:

- Some copy and comments still mention older phase language.
- Later UI phases still need to make authority visible above the fold on all
  operational surfaces.
- Failure state and export/report state need clearer first-class presentation.

### QGroundControl Mission Upload

Current support found in `MavlinkUdpServer.kt`, `MissionUploadSession.kt`,
`MissionEngine.kt`, and `MissionScreen.kt`:

- Receives `MISSION_COUNT` and starts an upload session.
- Requests uploaded items with `MISSION_REQUEST_INT`.
- Accepts `MISSION_ITEM_INT`.
- Accepts legacy `MISSION_ITEM`.
- Sends `MISSION_ACK` on accepted, invalid, denied, timeout, and sequence
  failure paths.
- Supports mission list download via `MISSION_REQUEST_LIST` and requested
  mission items via `MISSION_REQUEST_INT` or legacy `MISSION_REQUEST`.
- Supports `MISSION_CLEAR_ALL`.
- Supports `MISSION_SET_CURRENT`.
- Sends `MISSION_CURRENT` during telemetry bursts when a mission is loaded.
- Sends `MISSION_ITEM_REACHED` once for newly reached mission items.
- `MissionScreen` shows upload status, mode, control authority, active target,
  vehicle local position, and waypoint rows.

Current gaps for later phases:

- Real QGroundControl same-phone and desktop Wi-Fi acceptance remains required
  before Mission or Release phases can be called complete.
- Mission UI still needs ETA, distance to next waypoint, target speed, objective
  copy, and clearer progress/completion treatment.
- Mission persistence exists through `MissionPersistence`, but reconnect
  behavior still needs explicit release acceptance coverage.

### Flight Logging and Export

Current support found in `FlightRecorder.kt`, `FlightEvent.kt`,
`FlightSession.kt`, `AppRuntime.kt`, and Ops:

- Flight sessions are written under app-private
  `files/mavlab/flights/{session_id}/`.
- Current artifacts are:
  - `manifest.json`
  - `telemetry.csv`
  - `events.jsonl`
  - `mission.json`
- Telemetry is sampled by the runtime recorder loop at about 5 Hz.
- Events currently include recording start/stop, arm, mode changes, authority
  changes, and mission snapshot changes.
- Ops shows active or last session metadata and local path.

Current gaps for later phases:

- `report.md` is not generated yet.
- Export/share UI is not implemented yet.
- Event coverage needs mission upload lifecycle, waypoint reached, mission
  completion, failure injection/restoration, QGC connection, and export events.
- Telemetry CSV needs the final v1.5 schema, including session ID, active
  waypoint, GPS status, and failure flags.

## Existing Tests

Relevant test anchors already exist under
`app/src/test/java/com/ascend/mavlab/`:

- `core/mavlink/MavlinkMessageBuilderTest.kt`
- `core/mavlink/MissionUploadSessionTest.kt`
- `simulation/autopilot/MissionEngineTest.kt`
- `simulation/engine/ControlAuthorityTest.kt`
- `feature/drone3d/DroneModelControllerTest.kt`
- `simulation/mission/MissionSnapshotCodecTest.kt`
- `simulation/mission/MissionUploadStatusTest.kt`
- `simulation/recording/FlightRecorderTest.kt`

## Phase Acceptance Checklist

### Phase 0 - Baseline Audit and Release Definition

- [x] Baseline build result is known.
- [x] Existing failures are documented before changes.
- [x] v1.5 checklist exists in repo docs.
- [x] No app coding begins before the target scope is clear.

### Phase 1 - Navigation, Product Surface, and Copy Polish

- [x] The app opens into a coherent v1.5 shell.
- [x] Primary tab labels match the agreed names exactly.
- [x] `Systems` is not a primary MVP tab.
- [x] User can understand what each tab is for within 5 seconds.

### Phase 2 - Onboarding Upgrade

- [x] First-run user can understand MAVLab's purpose.
- [x] Onboarding covers QGC, takeoff, phone control, mission, failure, and
  export.
- [x] Onboarding can be replayed.
- [x] Copy matches Ascend Labs positioning.

### Phase 3 - Cockpit UI Polish

- [x] A screenshot of Cockpit immediately communicates drone state.
- [x] Authority, battery, GPS, armed state, flight mode, and MAVLink status are
  visible.
- [x] Cockpit updates from `DroneState`, not duplicate local UI state.

### Phase 4 - SIM / 3D Visualization Upgrade

- [x] SIM makes telemetry feel physical.
- [x] Propellers respond to simulated motor state.
- [x] Failed motor/GPS/battery state is visually obvious.
- [x] Mission waypoints and home marker are visible when mission exists.
- [x] SIM reads from the common simulation state.

### Phase 5 - Mission View and QGroundControl Workflow Polish

- [x] User can upload a QGC mission and see it in-app.
- [x] User can understand which waypoint is active.
- [x] User can explain mission progress using the UI.
- [ ] Real QGC same-phone and desktop-Wi-Fi tests pass.

### Phase 6 - Failure Lab / Scenario Explanation Polish

- [ ] Each failure teaches, not just breaks the drone.
- [ ] Active failures are visible in Cockpit and SIM.
- [ ] Flight logs capture failure injection/restoration events.
- [ ] Failure reset works reliably.

### Phase 7 - Flight Logging, Export, and Review

- [ ] A completed flight produces local artifacts.
- [ ] CSV can be opened externally.
- [ ] Report is understandable by an instructor or student.
- [ ] Failure and mission events appear in the timeline.
- [ ] Export works offline.

### Phase 8 - QA, Demo Script, Documentation, and Release Packaging

- [ ] Fresh build passes.
- [ ] Test matrix is updated.
- [ ] Demo script is reproducible.
- [ ] QGC acceptance tests are documented.
- [ ] v1.5 release notes exist.
- [ ] APK can be installed on a phone.

## Definition of Done Checklist

- [x] App navigation uses `Cockpit`, `Controller`, `Mission`, `SIM`, `Ops`.
- [x] Onboarding explains MAVLab, QGC, takeoff, controller, mission, failure,
  and export.
- [x] Cockpit shows attitude, altitude, battery, GPS, MAVLink, flight mode,
  armed state, speed, mission item, and authority.
- [x] SIM visually reflects drone attitude, altitude, motor state, failures,
  mission path, waypoints, and home.
- [x] Mission tab shows upload status, waypoint list, active waypoint, progress,
  distance, speed, ETA, and objective.
- [ ] Failure presets include explanations, telemetry signatures, operator
  response, and safety lessons.
- [ ] Flights produce telemetry CSV, event log, mission snapshot, and a simple
  report.
- [ ] Same-phone QGC and desktop QGC workflows are tested and documented.
- [ ] Demo script exists and can be followed without improvisation.
- [x] Build/test command passes:
  `GRADLE_USER_HOME="$PWD/.gradle" ./gradlew lintDebug testDebugUnitTest assembleDebug`
- [x] README and docs describe MAVLab as an Ascend Labs phone-based drone
  digital twin and training platform.

## Phase 0 Decision Notes

- Phase 1 should start from an already-correct primary tab model, then focus on
  shell polish, copy, Ops structure, and replay onboarding access rather than
  basic renaming.
- Mission and QGC protocol support are present enough for UI polish, but real
  QGC acceptance must remain a release gate.
- Logging has a useful MVP base, but export/report is not yet instructor-ready.
- Later implementation must preserve existing mission/failure/lesson code unless
  the visible product flow explicitly requires relocation or hiding.
