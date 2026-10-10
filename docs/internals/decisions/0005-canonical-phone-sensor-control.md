# ADR-0005: Canonical armed phone-sensor control

Status: Accepted for working-tree implementation
Date recorded: 2026-10-10
Authority: User confirmed armed phone-sensor input as canonical product behavior.

## Context

Sensor registration and pilot-input mapping already belonged to AppRuntime, independently of the selected surface. However, a local arm could retain GUIDED or another autonomous mode, and a uniform motor-RPM override could mask attitude commands. Local takeoff used GUIDED position control, which replaced the phone's roll, pitch, and yaw intent even though authority was labelled Controller.

A deterministic reproduction showed a local takeoff climbing with nonzero pilot input while simulated roll remained exactly zero. This was a control-mode defect, not a reason to rotate the SIM model independently of the engine or weaken ground-contact physics.

## Decision

Armed manual phone-sensor control is a canonical product invariant and must survive all future UI/UX refactors.

- Phone Sensors input is sampled, calibrated, mapped, and applied by AppRuntime while the application is visible, independently of Cockpit, Controller, or SIM composition.
- Starting a new local Phone Sensors flight calls `PhysicsSimulationEngine.armForLocalInput`: clear a stale RPM override, restore STABILIZE (or preserve explicitly selected ALT_HOLD), apply the latest normalized pilot input, and arm with Controller authority.
- Local takeoff uses ALT_HOLD. The engine controls height and retains pilot roll/pitch/yaw steering. Centered throttle maintains the target; changing throttle adjusts height using the existing altitude-hold behavior.
- GCS takeoff remains GUIDED position control. Uploaded GCS missions remain authoritative; phone samples must not take mission authority. Explicit LAND, RTL, LOITER, and guided navigation retain their existing meaning.
- Custom Input and Direct RPM remain explicit advanced input choices. Backgrounding suspends phone input according to the existing runtime lifecycle contract.
- Phone angles are calibrated radians. Mapping produces normalized roll/pitch/yaw in [-1, 1] and throttle in [0, 1]. Motors, physics, and DroneState determine every SIM transform and flight telemetry value.
- Arming enables intent; takeoff establishes flight. Ground contact still constrains actual movement, while motor telemetry makes accepted attitude commands observable before liftoff.

## Verification and impact

| Row | Scope |
| --- | --- |
| Product surfaces | Cockpit/Controller arm path and SIM observing engine state; no new screen-owned input effects. |
| Control paths | Phone Sensors local arm/rearm and takeoff; explicit advanced modes remain selected by the user. |
| Runtime states | Foreground armed control across surfaces; background policy and process recreation are unchanged. |
| Simulation | Manual-mode setup and local takeoff mode selection change; physics equations, ground contact, failures, and battery remain the shared engine model. |
| Protocol | GCS takeoff retains GUIDED; discovery, identity, ACKs, mission protocol, and advertised capabilities are unchanged. |
| Reverse states | Disarm cuts motors; local landing and mission authority remain covered by the existing suite. |
| Evidence | Deterministic engine tests cover actual motor/attitude response, stale-mode/RPM recovery, local altitude hold, disarm, and GCS-guided isolation. Full unit suite, lint/build, and bounded device sensor/SIM checks accompany implementation. |
| Documentation | Contributor invariant, control-authority contract, simulation model, Cockpit/Controller guidance, and protocol review record. |

Recorded outcome: [2026-10-10 working-tree validation](../../validation/results/phone-sensor-control-working-tree-2026-10-10.md), including 175 passing tests, lint/build, installation, and user-confirmed phone tilt response in SIM.

## Future changes

Preserve this contract and its tests. Any intentional change to control authority or sensor acceptance requires a new explicit user decision and documented migration; a UI refactor cannot silently revise the invariant.
