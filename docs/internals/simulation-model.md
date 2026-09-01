# Simulation Model

Audience: Simulation, control, and validation contributors
Status: Current high-level contract
Last verified: 2026-08-31

MAVLab models a quadcopter closely enough to teach control, telemetry, missions, and failures. It is not ArduPilot SITL, a certified flight-dynamics model, or proof that an identical physical aircraft will behave the same way.

## Update loop

`PhysicsSimulationEngine` schedules a nominal 10 ms tick (100 Hz). Scheduler cadence, elapsed-time integration, UI refresh, and telemetry rates are distinct.

```text
control source / mission intent
  -> control authority and flight mode
  -> autopilot / position controller
  -> motor mixing and failure effects
  -> physics integration and environment
  -> battery / mission / safety state
  -> DroneState publication
  -> UI, recording, sound, and MAVLink observers
```

## Components

- `PhysicsModel` — thrust, torque, drag, wind-relative velocity, position, attitude, ground contact, and geographic conversion.
- `Autopilot` — cascaded control behavior for supported modes.
- `PositionController` — position/velocity intent for autonomous modes.
- `MissionEngine` — uploaded mission progression.
- `FailureInjector` — simulated faults and perturbations.
- `DroneState` — authoritative vehicle-state snapshot.

## Supported high-level modes

STABILIZE, ALT_HOLD, AUTO, GUIDED, LOITER, RTL, and LAND exist in the current engine. A familiar mode name does not guarantee complete ArduCopter equivalence.

## Frames and units

Every boundary states its frame and unit explicitly:

- body axes and motor layout;
- local position/velocity frame;
- geographic latitude/longitude/altitude conversion;
- altitude reference (AGL or MSL);
- angle units (degrees or radians);
- speed and distance units;
- normalized or physical motor/throttle values.

Convert once near the boundary and test the conversion.

## Fidelity language

Documentation classifies behavior as:

1. **Modeled** — implemented and tested.
2. **Approximated** — useful simplification with stated limits.
3. **Not modeled** — outside current scope.

“Digital twin” means state-linked simulation and visualization, not certified replication of an airframe.

## Determinism and evidence

Tests control time, initial state, parameters, mission, and failure inputs where practical. New randomness exposes or records a seed. Performance/fidelity claims include scenario, device/build, method, and comparison target.

Source areas: `simulation/engine`, `simulation/physics`, `simulation/autopilot`, `simulation/mission`, and `simulation/failures`. Real-device evidence is still needed for timing, rendering, sensors, and Android lifecycle behavior.