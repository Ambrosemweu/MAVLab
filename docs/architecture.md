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

MAVLab uses a priority-based single-writer authority model to manage who can control the drone (manual pilot vs. autonomous GCS):
- **Authority Levels:**
  1. `IDLE` — Default pre-armed state.
  2. `CONTROLLER` — Manual control from on-screen sliders or phone tilt sensors.
  3. `GCS_MISSION` — Autonomous command sequences from QGroundControl.
- **Safety Preemption:** Manual inputs (disarming, manual landing, guided offsets, or joysticks) instantly override the active `GCS_MISSION` authority and shift the state back to `CONTROLLER` mode.

## Logging & Export Model

MAVLab automatically records flight telemetry and events to private storage:
- **Session Life Cycle:** Arming the vehicle creates a new session. Disarming automatically closes the session, computes flight envelope stats, and writes `report.md`.
- **Session Output Files:**
  - `manifest.json` — Identifies session start, end, and path.
  - `telemetry.csv` — High-resolution telemetry data including coordinates, velocities, waypoint, and active failures.
  - `events.jsonl` — Timeline logs (modes, battery alerts, connection, and failures).
  - `mission.json` — Backup snapshot of uploaded waypoints.
  - `report.md` — Human-readable markdown flight summary and safety review.
- **Sharing Mechanism:** Uses Android `FileProvider` to package these session files securely and expose them through standard Android Sharesheet intents.

## Surface Model & Features

MAVLab is organized into 5 primary functional tabs:
1. **Cockpit (Dashboard):** Live telemetry indicators and rolling graphs.
2. **Controller:** Manual joystick and phone sensor tilt control interfaces.
3. **SIM (3D View):** Real-time 3D drone attitude and waypoint visualization using a bundled GLB model.
4. **Mission:** Autonomous waypoint map tracking and demo mission triggers.
5. **Ops (Settings & Labs):** Failure scenario injection catalog and flight log history viewing/sharing.

## MAVLink Integration

`MavlinkUdpServer` sends MAVLink v2 telemetry and accepts a conservative command subset for QGroundControl integration. It supports heartbeat, attitude, position, GPS, system/battery status, command acknowledgements, parameters, and basic mission download/progress telemetry.

## Release Targets

- Android 8.0+.
- Offline-first.
- Debug APK under 50 MB.
- CI verifies lint, unit tests, and debug build.
