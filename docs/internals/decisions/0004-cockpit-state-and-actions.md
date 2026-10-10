# ADR-0004: Cockpit state derivation, ground preview, and actions

Status: Accepted for working-tree implementation
Date recorded: 2026-10-09
Updated: 2026-10-10 following user review of the Cockpit implementation.

## Context

The previous Cockpit rendered a flat metric grid, displayed ordinary flight actions together, and could substitute phone orientation for simulated vehicle attitude. The UI/UX research requires state-led presentation, authoritative data, visible authority, stale-data handling, and truthful reverse paths.

The simulation engine does not yet publish one explicit flight-phase enum. Android service lifecycle, simulation state, mission state, recording state, failures, and MAVLink identity are separate authoritative sources.

## Decision

- `CockpitUiState` is presentation state derived from existing authoritative flows. It does not become a second simulation model.
- The Cockpit instrument previews calibrated phone roll/pitch/yaw in radians while the vehicle is settled on the ground (AGL <= 0.25 m and absolute vertical speed <= 0.2 m/s), including armed ground states. It labels this `PHONE ATTITUDE`. Otherwise it uses `DroneState` and labels it `SIM ATTITUDE`. Unavailable/invalid/uninitialised sensors or stale/unavailable vehicle data disable the preview. Altitude and vertical speed always remain engine telemetry, and the engineering chart remains simulated attitude.
- `AppRuntime` publishes a typed runtime lifecycle and the elapsed-realtime timestamp of the latest engine-state emission.
- State is live through 1.5 seconds, delayed through 5 seconds, and stale afterward. Stale or unavailable state locks flight actions.
- Flight phase is conservatively derived from armed state, mode, altitude, vertical speed, and recently completed recording evidence. It must not invent takeoff, landing, or completion from one decorative animation.
- The main dock always contains the arm/disarm toggle and SIM. Following user review, the toggle label describes the next action: Arm while disarmed, Disarm while armed. Arming is disabled under external authority, off the ground, or during critical alerts; both directions disable with unavailable/stale runtime state. SIM always opens the digital twin. Contextual Takeoff, Land, Disarm, navigation, and recovery actions appear in the task card. All flight commands still route through `AppRuntime` and the simulation engine.
- Taking authority from `GCS_DIRECT` or `GCS_MISSION` to land requires an explicit confirmation dialog that states the authority consequence.
- Runtime Start/Stop remains owned by `SimulationService` and its notification. Cockpit routes unavailable runtime state to Ops.
- Charts and engineering details remain collapsed and do not sample while hidden.
- Following user review on 2026-10-10, one combined Flight telemetry chart identifies each trace with a labelled line key and latest sampled value. Altitude AGL uses the left metre scale with zero at ground; roll/pitch/yaw share the right signed degree scale, with solid/dashed/dotted trace styles matching their keys. Scales are calculated independently so metres and degrees are never treated as a shared unit. Altitude/roll/pitch/yaw use amber/teal/blue/purple respectively. The bounded window holds 41 samples at 250 ms intervals (10 seconds), aligns the newest sample to Now, and resets on reopening so hidden periods are not joined into a continuous trace.

## Consequences

- The first viewport prioritizes state, authority, health, and next action.
- Connection cannot be confused with control authority.
- The state derivation can be tested independently of Compose.
- Flight-phase derivation remains an approximation until the engine publishes an explicit phase contract. Any future engine phase replaces the corresponding presentation inference.
- `Take control & land` is an explicit user choice rather than an incidental authority transition caused by phone motion.

## Replace when

Replace the derived flight phase and freshness thresholds when the engine/runtime publishes tested equivalents with better evidence. Keep the action and authority rules unless a later ADR changes the control contract.

## Impact review for the 2026-10-10 adjustment

| Row | Scope and verification |
| --- | --- |
| Product surfaces | Cockpit instrument, fixed dock, and task actions; SIM destination uses existing navigation. |
| Control paths | Existing calibrated phone-sensor flow supplies ground preview; existing engine commands supply arm/disarm. Unit tests cover external-authority arming restrictions. |
| Runtime states | Unit tests cover unavailable/stale data and armed flight. Foreground sensor registration is checked on device. Background handoff and process recreation are outside this adjustment. |
| Simulation | Altitude, speed, flight attitude, recording, and digital twin remain engine-derived; no physics/autopilot changes. |
| Protocol | Not applicable: no MAVLink transport, capability, command, or mission-protocol changes. |
| Reverse states | The Arm/Disarm button toggles armed state and labels the next action; task-card landing and explicit external takeover remain available. |
| Evidence | Focused instrument/state unit tests, full debug unit suite, lint, debug build, and device smoke checks. Physical tilt and installed control checks remain bounded device evidence rather than release qualification. |
| Documentation | Cockpit user guide, this ADR, and product-surface description reflect the reviewed behavior. |
