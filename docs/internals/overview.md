# MAVLab Architecture Overview

Audience: Maintainers and contributors
Status: Current architecture
Applies to: v1.5.3
Last verified: 2026-09-01

MAVLab is a single-module Android application whose simulation, control, visualization, recording, and MAVLink endpoint run on the phone.

```text
Compose product surfaces
Cockpit · Controller · SIM · Mission · Ops
                  │ StateFlow + commands
                  ▼
              AppRuntime
 composition root · facade · persistence
       │                         │
       ▼                         ▼
SimulationService          MavlinkUdpServer
lifecycle + locks          UDP / QGC boundary
       │                         │
       └────────────┬────────────┘
                    ▼
         PhysicsSimulationEngine
 physics · autopilot · missions · failures
                    │
                    ▼
                DroneState
        authoritative vehicle state
```

## Ownership

### MainActivity

Starts the Compose application, presents first-launch onboarding, and communicates visibility/lifecycle actions to `SimulationService`.

### SimulationService

Owns the Android foreground-service lifecycle, runtime startup/shutdown, wake/Wi-Fi locks, and background-retention policy. See [runtime-lifecycle.md](runtime-lifecycle.md).

### AppRuntime

`AppRuntime` is a process-wide singleton and practical composition root. It coordinates the simulation engine, MAVLink server, phone sensors, controller state, sound, mission persistence, simulator location, and flight recording/export.

It exposes read-only flows to Compose, but it is not merely a thin adapter: it also owns orchestration and persistence. Documentation should describe the current boundary honestly rather than pretending a future refactor already exists.

### PhysicsSimulationEngine

The engine owns simulation progression and composes the physics model, autopilot, position controller, mission engine, failure injector, and battery/flight state. See [simulation-model.md](simulation-model.md).

### MavlinkUdpServer

The server presents MAVLab to QGroundControl as an ArduPilot-like simulated copter. It translates inbound messages into engine commands and sends state-derived telemetry to peers.

It currently depends directly on `PhysicsSimulationEngine`; the protocol/physics seam is therefore more concrete and tightly coupled than the small `SimulationEngine` interface suggests.

## State model

`DroneState` is the authoritative vehicle-state snapshot for attitude, position, velocity, armed state, mode, battery, and related flight state. Missions, failures, controller state, MAVLink identity/connection, recording, audio, and service lifecycle have separate flows.

Compose surfaces observe these states. They must not maintain a separate fake simulation state.

## Product surfaces

| Surface | Question it answers | Source area |
|---|---|---|
| Cockpit | What is the drone doing, and who controls it? | `feature/dashboard` |
| Controller | How do I control the simulation locally? | `feature/controller` |
| SIM | How is authoritative state represented physically? | `feature/drone3d` |
| Mission | What mission is loaded and progressing? | `feature/mission` |
| Ops | Is the runtime healthy and configurable? | `feature/settings`, failures, logs |

Implementation names such as `DashboardScreen` and `SettingsScreen` may lag product labels. Documentation uses product labels and links current source names.

## Cross-cutting invariants

1. UI surfaces observe engine-derived state.
2. Control authority is explicit and visible.
3. The core product remains phone-first and offline-capable.
4. Protocol claims match implemented behavior and evidence.
5. Local uncommitted behavior is not release behavior.
6. Published research remains tied to its evaluated release.
