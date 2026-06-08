# MAVLab Architecture

MAVLab is a standalone Android drone education simulator.

## Runtime

- `SimulationService` starts the shared simulation runtime.
- `AppRuntime` exposes state, failures, missions, MAVLink status, and control commands to Compose screens.
- `PhysicsSimulationEngine` runs the fixed-rate physics loop and owns autopilot, failures, mission progress, and battery state.

## Features

- Dashboard: telemetry cards and rolling charts.
- Controller: phone tilt and manual fallback controls.
- 3D View: bundled GLB drone visualization.
- Labs: failure injection and mission controls.
- Lessons: seven guided curriculum modules.
- Settings: release placeholder for future network and classroom settings.

## MAVLink

`MavlinkUdpServer` sends MAVLink v2 telemetry and accepts a conservative command subset for QGroundControl integration. It supports heartbeat, attitude, position, GPS, system/battery status, command acknowledgements, parameters, and basic mission download/progress telemetry.

## Release Targets

- Android 8.0+.
- Offline-first.
- Debug APK under 50 MB.
- CI verifies lint, unit tests, and debug build.
