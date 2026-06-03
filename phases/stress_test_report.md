# MAVLab Stress Test Report

Date: 2026-06-03

Scope: The folder currently contains architecture and phase-planning documents, not runnable application source. This stress test is therefore an architecture, protocol, product, and implementation-risk stress test based on the current docs.

## Executive Verdict

The standalone direction is the right product bet for the target market: offline, phone-first, no Docker, no cloud, no Python bridge. The highest-risk assumption is not the physics engine. The highest-risk assumption is that QGroundControl will reliably treat MAVLab as an ArduPilot-like vehicle over Android UDP with only the currently listed MAVLink messages and simple port behavior.

Applied roadmap correction: Phase 0 should be scaffold-only. The first behavior build should be a hard Phase 1 protocol spike before investing in physics, UI, or lessons. If Phase 1 does not prove bidirectional QGC control on the same phone and on a second device, the rest of the roadmap is built on an unproven integration.

## Critical Breakpoints

### 1. UDP connection model was underspecified

Original docs said the MAVLink server used UDP port `14550` on `0.0.0.0`, and later said MAVLab broadcasts to `0.0.0.0:14550`. `0.0.0.0` is a bind address, not a valid broadcast destination.

Stress scenario:

- Same-phone QGC is already listening on `127.0.0.1:14550`.
- MAVLab sends heartbeats from an ephemeral UDP source port.
- QGC detects the vehicle.
- QGC sends arm/mode/takeoff commands back to the source endpoint or its configured link endpoint.
- MAVLab is not listening on that source endpoint, so commands disappear.

Applied correction:

- Define the socket model explicitly:
  - Local bind port for MAVLab command receive.
  - Destination endpoint for QGC discovery.
  - Broadcast address for LAN mode, such as subnet broadcast or explicit QGC IP.
  - Whether send and receive share one bound socket.
  - How QGC endpoint discovery is recorded after first inbound packet.

Do not start Phase 2 until this works.

### 2. QGC compatibility is deeper than the display message list

The original protocol proof message set covered basic display, but QGC usually exercises more than display telemetry once it identifies a vehicle. It may request parameters, stream rates, mission state, capabilities, home position, command ACKs, and autopilot metadata.

Stress scenario:

- QGC receives `HEARTBEAT`, `ATTITUDE`, and `GLOBAL_POSITION_INT`.
- QGC labels the vehicle as connected.
- User presses Arm.
- MAVLab changes internal state, but does not send `COMMAND_ACK`, consistent base mode flags, or expected ArduPilot-ish status.
- QGC UI remains confused or retries commands.

Applied correction:

- Treat `COMMAND_ACK`, `SET_MESSAGE_INTERVAL`, parameter list, and mode acknowledgement as Phase 1 requirements, not late-phase polish.
- Add a MAVLink compliance trace test: capture QGC startup traffic and list every message QGC sends before the app is considered "ready."

### 3. Fixed `systemId = 1` would break classroom scale

The original architecture mentioned multiple students and different system IDs, but the sample server hardcoded `systemId = 1`.

Stress scenario:

- 20 students run MAVLab on the same Wi-Fi.
- Instructor QGC receives 20 heartbeats with the same system ID.
- Vehicles collapse into one vehicle or commands target the wrong simulated drone.

Applied correction:

- Generate a stable per-install system ID in the range 1-250.
- Show and allow changing the ID in settings.
- Include `system_id` collision detection in classroom setup docs.

### 4. Android lifecycle and foreground service rules are under-tested

The plan relies on split-screen with QGC. That means MAVLab must continue simulation and UDP IO when partially visible, resized, backgrounded briefly, orientation-changed, or interrupted by notification permission prompts.

Stress scenario:

- User opens MAVLab, starts QGC split-screen, rotates phone, receives a call, returns.
- Simulation coroutine or UDP socket is cancelled.
- QGC still shows stale vehicle state or loses command channel.

Applied correction:

- Phase 1 acceptance should include:
  - 30 minutes in split-screen.
  - Activity rotation.
  - Screen off/on.
  - App moved to background for 30 seconds and restored.
  - Android 13+ notification permission behavior.
  - Foreground-service notification visible and accurate.

### 5. Simulation state must not drive Compose at 100 Hz

The docs place `DroneState` at 100 Hz and feed UI dashboard and 3D visualization from it. If Compose collects raw 100 Hz state, charts, cards, and 3D can cause unnecessary recomposition and battery drain.

Stress scenario:

- Physics runs at 100 Hz.
- MAVLink explorer logs messages.
- Dashboard charts update.
- 3D SceneView renders.
- Phone sensor flow emits at game rate.
- App misses frames, drains battery, or triggers ANR on low-cost phones.

Applied correction:

- Use separate output rates:
  - Physics: 100 Hz fixed timestep.
  - MAVLink attitude: 10-30 Hz depending on QGC request.
  - UI cards: 5-10 Hz.
  - Charts: 10-20 Hz ring buffers.
  - 3D: render-frame driven, sampling latest state.
  - MAVLink explorer: bounded list with backpressure and filters.

### 6. Physics correctness needs automated invariants before UX polish

The roadmap assumes a custom 6-DOF model is only medium complexity. The risk is not writing equations; it is sign conventions, units, numerical stability, and controller tuning.

Stress scenario:

- NED frame uses Z down.
- QGC altitude fields use positive altitude above ground/MSL.
- Internal drone falls "down" while displayed altitude appears to climb, or vertical velocity sign is inverted.

Applied correction:

- Add deterministic physics tests before QGC tests:
  - Disarmed free fall: acceleration approximately 9.81 m/s2 down.
  - Hover thrust: four motors at hover speed produce near-zero vertical acceleration.
  - Roll torque sign: positive roll command produces expected body rotation.
  - Yaw torque sign matches QGC HUD.
  - Energy/battery draw stays non-negative.
  - State remains finite under max inputs for 10 simulated minutes.

### 7. Sensor fallback was missing

The original controller spec used `TYPE_GAME_ROTATION_VECTOR` and threw if unavailable. Some low-end or older devices may lack this sensor or report unstable timing.

Stress scenario:

- Student installs on a low-cost Android phone.
- Sensor is missing.
- Controller screen crashes instead of falling back or disabling tilt mode.

Applied correction:

- Add fallback order:
  - `TYPE_GAME_ROTATION_VECTOR`
  - `TYPE_ROTATION_VECTOR`
  - accelerometer plus gyroscope fusion if feasible
  - no-tilt mode with on-screen sticks
- Phase 3 acceptance now includes at least one low-end phone, not just Pixel-class hardware.

### 8. The roadmap front-loads too much confidence

The 20-week plan includes custom physics, PID, MAVLink/QGC integration, phone controller, 3D, labs, missions, failures, lessons, Play Store release, CI, and teacher docs. That is possible only if Phase 0, Phase 1, and Phase 2 are sharply constrained.

Stress scenario:

- Team spends weeks building UI and lessons.
- Later discovers QGC command flow, mission upload, or Android UDP behavior needs substantial redesign.

Applied correction:

- Reorder the first milestones:
  1. Phase 0: buildable Android skeleton, package seams, protocol guardrail docs.
  2. Phase 1A: QGC detects MAVLab on same phone and second device.
  3. Phase 1B: QGC can arm/disarm, set mode, request params, and receive ACKs.
  4. Phase 1C: QGC disconnect/reconnect and two-device system ID tests pass.
  5. Phase 2A: headless physics engine tests pass on JVM.
  6. Phase 2B: QGC sees stable takeoff, hover, land.
  6. Only then build polished dashboard and 3D.

## Minimum Stress-Test Matrix

### Protocol

- Same-phone QGC split-screen detects vehicle within 3 seconds.
- Desktop QGC on same Wi-Fi detects vehicle.
- Arm, disarm, mode change, takeoff, land all receive ACKs.
- QGC parameter refresh completes without hanging.
- QGC reconnects after MAVLab restart.
- MAVLab reconnects after QGC restart.
- Two MAVLab devices on the same network do not collide.
- Twenty simulated devices with unique system IDs can be displayed by instructor QGC.

### Physics

- 10 minutes simulated hover remains bounded.
- Full throttle climb and zero throttle descent remain finite.
- Max roll/pitch/yaw commands do not create NaN/Inf.
- Wind gusts do not destabilize into impossible values.
- Motor failure creates controlled degradation, not numerical explosion.
- Battery depletion triggers failsafe and landing.

### Android Runtime

- 30 minutes continuous split-screen operation.
- Screen rotation while connected.
- App backgrounded and restored.
- Screen off/on.
- Low battery mode enabled.
- Device thermal throttling.
- No network, Wi-Fi off, Wi-Fi on, hotspot mode.
- Android 8, Android 11, Android 13, Android 15.

### UI and Education

- Dashboard does not collect 100 Hz raw state directly.
- MAVLink Explorer caps memory usage.
- Charts use bounded ring buffers.
- Lesson checks are state-based, not only button-based.
- Onboarding handles QGC not installed.
- User can fly without QGC using in-app controls.

## Recommended Next Action

Build the roadmap in this order:

1. Phase 0 skeleton: app shell, packages, placeholder interfaces, guardrail docs.
2. Phase 1 protocol spike: one foreground service, one bound UDP socket, core telemetry, command listener, `COMMAND_ACK`, minimal params, and inbound QGC logging.
3. Phase 2 physics: headless invariant tests first, then QGC hover/takeoff/land.

The pass/fail gate is simple: before Phase 2, QGC must detect, arm, change mode, take off, land, disconnect, reconnect, and work both same-phone and second-device. Until that is proven, every later behavior feature is speculative.
