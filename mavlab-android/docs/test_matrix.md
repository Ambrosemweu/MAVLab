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

## Later Phase Checks

- Phase 1: protocol ACKs, params, rates, reconnects, system IDs
- Phase 2: deterministic physics invariants before device tests
- Phase 3: bounded UI update rates and sensor fallback
- Phase 4: 3D frame pacing and bounded MAVLink explorer memory
