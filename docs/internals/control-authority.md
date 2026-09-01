# Control-Authority Contract

Audience: Product, simulation, protocol, and UI contributors
Status: Current model
Last verified: 2026-08-31

Control authority answers:

> Which high-level source of intent currently controls the simulated vehicle?

It is not the same as flight mode, connection status, or armed state.

## Authorities

| Authority | Meaning | Expected visibility |
|---|---|---|
| `IDLE` | No active controller owns flight intent. | Cockpit shows Idle. |
| `CONTROLLER` | Built-in manual/phone input is dominant. | Controller input/source is visible. |
| `GCS_DIRECT` | QGC issued direct commands without an active mission. | Cockpit shows GCS Direct. |
| `GCS_MISSION` | Uploaded mission/autonomous GCS intent is dominant. | Mission progress and GCS Mission are visible. |

## Required properties

1. One high-level authority is dominant at a time.
2. Every authority has a visible label.
3. Every authority has an explicit exit/recovery path.
4. UI controls do not imply ownership they lack.
5. SIM and telemetry observe engine state; they do not establish authority independently.

## Important current limits

Generic joystick/manual input must not be documented as automatically preempting active `GCS_MISSION` unless implementation and tests prove that transition. Explicit commands may follow different rules.

A GCS connection indicator is not itself an autopilot failsafe. Do not claim automatic RTL or safety-mode reversion on heartbeat loss unless implemented and tested.

## Transition review

Changes identify transitions for arm/disarm, local takeoff/land, GCS mode/direct command, mission upload/start/completion/cancel, manual input during mission, heartbeat expiry, failure injection, service stop, and reset.

Primary source: `ControlAuthority.kt`, `PhysicsSimulationEngine.kt`, `AppRuntime.kt`, `MavlinkUdpServer.kt`, and the Cockpit/Controller/Mission surfaces. Focused tests should assert transitions rather than only labels.