# MAVLab Architecture

MAVLab is a standalone Android drone education simulator.

## Runtime

- `SimulationService` starts the shared simulation runtime.
- `AppRuntime` exposes state, failures, missions, MAVLink status, and control commands to Compose screens.
- `PhysicsSimulationEngine` runs the fixed-rate physics loop and owns autopilot, failures, mission progress, and battery state.

## Data & State Flow Invariant

MAVLab enforces a single source of truth architecture:
- `PhysicsSimulationEngine` runs a periodic physics loop updating drone dynamics.
- State changes write directly to Kotlin `StateFlow` structures inside the engine.
- `AppRuntime` exposes these state flows as read-only streams.
- Jetpack Compose screens observe these states as read-only state variables, ensuring atomic, unidirectional data flow and rendering accuracy.

## Control Authority Model

Control authority is observed from the engine: `IDLE`, `CONTROLLER`, `GCS_DIRECT` or `GCS_MISSION`. A connected peer is not automatically the controlling source. Phone sensor and generic manual input do not bypass active mission authority; explicit local landing/disarm and other implemented commands have their own engine paths. See the canonical [control-authority contract](internals/control-authority.md).

## Logging & Export Model

On the UI/UX feature branch, recording is off by default. Ops offers explicit Begin/Stop-save and an optional, persisted automatic-on-arm setting; existing released builds used automatic arm capture. See [ADR-0008](internals/decisions/0008-ops-health-and-recording.md).
- **Session Life Cycle:** Explicit Begin, or arming after Ops opt-in, creates a session. Stop/save, disarm or runtime stop closes an active session and writes `report.md`. Deliberate Stop/save prevents opted-in automatic capture from restarting during the same armed flight.
- **Session Output Files:**
  - `manifest.json` — Identifies session start, end, and path.
  - `telemetry.csv` — High-resolution telemetry data including coordinates, velocities, waypoint, and active failures.
  - `events.jsonl` — Timeline logs (modes, battery alerts, connection, and failures).
  - `mission.json` — Backup snapshot of uploaded waypoints.
  - `report.md` — Human-readable Markdown session summary and recorded scenario notes; not a safety assessment.
- **Sharing Mechanism:** Uses Android `FileProvider` to package these session files securely and expose them through standard Android Sharesheet intents.

## Surface Model & Features

The UI/UX feature branch uses four destinations: Cockpit, Control, SIM and Ops. Missions appear contextually in these surfaces. See the [surface definition](mavlab_product_surface_definition.md) and [Ops guide](user/ops.md) for current branch behavior. Published release descriptions remain separate from this development checkpoint.

## MAVLink Integration

`MavlinkUdpServer` sends MAVLink v2 telemetry and accepts a conservative command subset for QGroundControl integration. It supports heartbeat, attitude, position, GPS, system/battery status, command acknowledgements, parameters, and basic mission download/progress telemetry.

## Release Targets

- Android 8.0+.
- Offline-first.
- Debug APK under 50 MB.
- CI verifies lint, unit tests, and debug build.
