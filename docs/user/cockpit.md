# Cockpit

Cockpit is MAVLab's live vehicle-state surface. It answers four questions without requiring the user to scroll:

1. What flight and runtime state is the simulated vehicle in?
2. Who currently has control authority?
3. Is a condition degraded, blocked, or unsafe?
4. What is the safest useful next action?

## Reading the first viewport

The compact header names the current phase, such as `READY · DISARMED`, `IN FLIGHT · MANUAL`, `AUTO MISSION · LIVE`, `RETURNING HOME`, or `LANDING · ACTIVE`.

The truth strip keeps three different concepts separate:

- **Control** — `Idle`, `Controller`, `GCS Direct`, or `GCS Mission`.
- **Mode** — the simulated autopilot mode.
- **Vehicle** — disarmed, armed, landing, or landed while still armed.

Altitude, speed, battery, GPS, task progress, and warning state come from the authoritative simulation and supporting runtime flows. While the simulated vehicle is settled on the ground, the aviation instrument previews calibrated phone gyro attitude and displays `PHONE ATTITUDE`. This works before and after arming. In flight it displays `SIM ATTITUDE` and uses the simulated vehicle's attitude. If phone sensors are unavailable, it also uses simulation attitude. Phone movement never changes the altitude readout.

## Actions and recovery

The main buttons are the arm/disarm toggle and `SIM`. The toggle shows `Arm` while disarmed and `Disarm` while armed, so its label describes the next action. Arming is disabled while external control has authority or when current state does not permit arming. Both directions are disabled if runtime state is unavailable or stale. SIM opens the digital twin view.

State-specific actions remain in the task card, including Takeoff, Land, Disarm, Open Control, Mission controls, and Open Ops. The vehicle truth strip always shows the current armed state.

With Phone Sensors selected, a new local arm restores manual sensor control. Local Takeoff uses altitude hold while retaining phone attitude steering, including after switching directly to SIM. See [Control](controller.md) for this canonical control flow.

If GCS Direct or GCS Mission has authority, `Take control & land` requires confirmation. Confirming it deliberately changes authority to Controller and commands LAND. Dismissing the dialog preserves external control.

Connection and authority remain separate. `QGC connected` means a validated peer is present; it does not by itself mean QGC controls the simulated vehicle.

## Delayed and stale state

Cockpit labels late state as delayed and shows the update age. If authoritative simulation updates become stale, values remain visible as last-known data, flight actions are locked, and Cockpit routes the user to Ops. Default values are never presented as live recovery evidence.

## Telemetry and details

Charts, motor RPM, protocol messages, recording location, and other engineering details are collapsed under `Telemetry & details`.

- **Flight telemetry** combines altitude, roll, pitch, and yaw in one graph with a shared time axis. Each labelled line key shows its latest sampled value.
- **Altitude AGL** uses the amber solid line and the **left metre scale**, with ground at zero.
- **Roll, pitch, and yaw** use the **right degree scale**: roll is teal and solid, pitch is blue and dashed, yaw is purple and dotted. Zero is level for roll and pitch; yaw is the vehicle's heading angle.
- Both vertical scales are labelled with their units and adjust independently; line heights should be read against the appropriate scale. The horizontal axis runs from `10 s ago` to `Now`; a short history fills from the right as samples arrive.

The graph shows authoritative simulated-vehicle telemetry, including on the ground where the main instrument may preview phone attitude. It samples four times per second only while this section is open, retains ten seconds, and starts a fresh window when reopened.

## Contextual missions

Mission awareness is compact: a received-source label and progress/state, with a link into expanded mission controls or a current recovery action. Cockpit has no waypoint list or standalone Mission destination. Rejected uploads and unknown saved sources are shown as blocked; exact diagnostics live in [Ops](ops.md). A successful upload does not mean AUTO is running.
