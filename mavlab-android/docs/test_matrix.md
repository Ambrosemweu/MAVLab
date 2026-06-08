# Test Matrix

## Devices

- Android 8 device or emulator
- Android 11 device or emulator
- Android 13 device or emulator
- Android 15 device or emulator
- Low-end Android phone for sensor fallback testing
- Pixel-class phone for performance baseline

## Protocol Tests

- Same-phone QGroundControl split-screen
- Desktop QGroundControl on the same Wi-Fi
- Wi-Fi off/on
- Explicit QGC IP
- Valid subnet broadcast
- App background and restore
- Screen rotation
- Screen off/on
- QGC restart while MAVLab keeps running
- MAVLab restart while QGC keeps running
- Two MAVLab devices on the same network

## Phase 0 Checks

- App opens without behavior implementation
- Placeholder tabs render
- Foreground service class exists but is not active by default
- No MAVLink, UDP, physics, or sensor behavior is implemented

## Phase Checks

- Phase 1: protocol ACKs, params, rates, reconnects, system IDs
- Phase 2: deterministic physics invariants before device tests
- Phase 3: bounded UI update rates, sensor fallback, calibration, throttle altitude control, and split-screen QGC operation
- Phase 4: 3D model loads and follows simulator attitude without blocking dashboard telemetry
- Phase 5: failure toggles affect physics/telemetry, demo mission progresses, Guided offsets move the drone, GPS loss downgrades assisted modes, low battery triggers RTL
- Phase 6: onboarding completion persists, all seven lessons are navigable, CI passes, APK remains under 50 MB, docs and release metadata exist

## Release QA

- Clean install and complete onboarding
- Run Lesson 1 end-to-end
- Load demo mission from Labs and start Auto
- Inject GPS loss and verify Alt Hold failsafe
- Install with QGroundControl and confirm telemetry
- Leave app running for 1 hour with no crash or ANR
- Confirm debug APK size is below 50 MB
