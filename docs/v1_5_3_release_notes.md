# MAVLab v1.5.3 Release Notes

MAVLab v1.5.3 improves phone control, QGroundControl interoperability, Android background behavior, and contributor documentation. The updated v1.5.3 build also restores motor audio during QGroundControl handoff and extends the handoff window to one minute.

## Updated v1.5.3 build

- Motor sound now remains synchronized with the simulated vehicle while MAVLab is backgrounded for QGroundControl use.
- Arming, disarming, and motor changes commanded from QGroundControl remain audible.
- Phone-sensor input still stops immediately in the background, preventing hidden gyro control.
- The same-phone QGroundControl handoff window is now one minute.
- Stopping MAVLab, removing it from recents, or reaching the idle-disconnected timeout still releases audio, networking, simulation, and power locks.

## Phone gyro control follows the simulator

- Phone tilt input now remains available across MAVLab surfaces instead of requiring the Controller screen to stay open.
- Gyro input is applied only when Phone Tilt mode is selected and sensors are available.
- GCS mission authority remains protected: phone tilt cannot override an active QGroundControl mission.
- Leaving MAVLab suspends phone-sensor input and neutralizes the local pilot input until the app returns.

## Stable ArduPilot-compatible identity

- `AUTOPILOT_VERSION` now comes from one pinned compatibility profile rather than tracking whatever ArduPilot release is newest.
- MAVLab truthfully reports an ArduCopter 4.6.3-compatible **development** identity with custom identifier `MAVLAB`; it never claims to be official ArduPilot firmware.
- Advertised MAVLink capabilities are limited to paths MAVLab implements and tests.
- Future ArduPilot releases no longer require reactive version spoofing. Compatibility upgrades follow the documented review and QGC acceptance procedure.

## Connection-aware background telemetry

- Switching from MAVLab to QGroundControl starts a one-minute handoff window, so same-phone use no longer requires split-screen.
- A validated GCS connection, an armed vehicle, or an active AUTO mission keeps simulation and MAVLink running in the background.
- Phone sensors stop while MAVLab is backgrounded, preventing hidden gyro control; drone audio remains active while the simulation runtime is intentionally retained.
- An idle, disconnected runtime stops automatically after the handoff and heartbeat timeouts.
- Explicit close/removal and the notification's **Stop simulation** action stop immediately.
- The Android foreground service is classified as `connectedDevice`, matching its active Wi-Fi/GCS role.

## Documentation system

- Added a contributor/agent guide with project invariants, impact checks, and verification defaults.
- Reorganized documentation around user, internals, operations, validation, and archive audiences.
- Added current contracts for runtime lifecycle, control authority, simulation, MAVLink behavior, and ArduPilot compatibility.
- Corrected the documented debug artifact path to `MAVlab.apk`.
- Preserved the published technical paper as an evaluated historical snapshot rather than silently rewriting its release claims.

## Upgrade

Download `MAVLab-v1.5.3.apk` from the GitHub release and install it over the existing app. Application data and saved settings are retained during an in-place upgrade.

To build from source:

```bash
cd mavlab-android
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew lintDebug testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/MAVlab.apk
```
