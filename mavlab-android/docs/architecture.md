# MAVLab Architecture

MAVLab is planned as a standalone Android-first education simulator.

## Active Phase Order

1. Phase 0: project skeleton and architecture guardrails
2. Phase 1: QGC/MAVLink protocol proof
3. Phase 2: physics engine
4. Phase 3: phone controller and telemetry dashboard
5. Phase 4: 3D visualization and education modules
6. Phase 5: failures and missions
7. Phase 6: polish and release

## Phase 0 Boundaries

Phase 0 creates only the project shell and package seams. It must not implement MAVLink, UDP, QGroundControl, physics, phone sensor mapping, 3D rendering, failures, missions, or lessons.

## Core Modules

- `core/mavlink`: MAVLink endpoint interfaces and socket configuration
- `core/sensors`: phone sensor abstraction
- `core/settings`: default settings structures
- `simulation/engine`: simulation engine interface and state shell
- `simulation/physics`: future 6-DOF physics model
- `simulation/autopilot`: future flight controller
- `feature/*`: Compose feature placeholders
- `service`: foreground service boundary
