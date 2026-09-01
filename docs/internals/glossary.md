# MAVLab Glossary

Audience: Everyone changing or documenting MAVLab
Status: Living canonical vocabulary
Last verified: 2026-08-31

## Product and runtime

**MAVLab** — the Android application and its on-device drone simulation runtime.

**Simulated vehicle** — the drone represented by MAVLab state and exposed through the UI and MAVLink; never a real aircraft.

**Simulation runtime** — the engine, protocol server, recording, sensor, sound, and supporting state coordinated by `AppRuntime` and owned through `SimulationService`.

**Digital twin view** — the SIM surface rendering authoritative simulation state. Here “digital twin” means state-linked simulation and visualization, not certified fidelity to a specific airframe.

**Product surface** — Cockpit, Controller, SIM, Mission, or Ops.

## Control and flight

**Flight mode** — autopilot behavior such as STABILIZE, ALT_HOLD, AUTO, GUIDED, LOITER, RTL, or LAND.

**Control authority** — dominant high-level flight intent: `IDLE`, `CONTROLLER`, `GCS_DIRECT`, or `GCS_MISSION`.

**Pilot input** — roll, pitch, yaw, and throttle intent from phone sensors or built-in controls.

**Mission** — an ordered set of autonomous commands/waypoints uploaded or loaded into the simulation.

**Failure injection** — a deliberate simulated fault or environmental perturbation.

## Protocol

**GCS** — ground control station software. QGroundControl is MAVLab’s active integration target.

**QGC** — QGroundControl.

**MAVLink** — telemetry and command protocol between MAVLab’s simulated vehicle and a GCS.

**SYSID** — MAVLink system identifier. MAVLab currently defaults to vehicle SYSID 1; QGC commonly uses 255.

**COMPID** — component identifier inside a MAVLink system.

**Peer** — a MAVLink endpoint detected by MAVLab and eligible for telemetry/command exchange.

**Accepted no-op** — a request that receives acceptance without the complete requested effect; not full implementation.

## State and evidence

**DroneState** — authoritative vehicle-state snapshot published by the physics engine.

**AGL** — altitude above ground level.

**MSL** — altitude relative to mean sea level.

**Working-tree behavior** — behavior present in local uncommitted files; not a released capability.

**Validation specification** — repeatable description of what to test and how.

**Validation result** — immutable execution record tied to a version/commit, device, Android version, QGC version, tester, date, and evidence.

**ADR** — architecture decision record: context, decision, consequences, and replacement conditions.