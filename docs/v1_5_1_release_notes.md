# MAVLab v1.5.1 Release Notes

MAVLab v1.5.1 is a protocol and connectivity release focused on deeper QGroundControl integration: GCS-commanded home position, COMMAND_INT support, simultaneous multi-GCS telemetry, and a fix for intermittent "Communication Lost" warnings.

---

## What's New in v1.5.1

### 1. GCS Home Position Support
- **Set Home from QGC:** `MAV_CMD_DO_SET_HOME` is now accepted (both "use current position" and map-coordinate forms). Use QGC's "Set Home Here" to move the RTL return point — including mid-flight.
- **HOME_POSITION Telemetry:** MAVLab now streams the `HOME_POSITION` message (~1 Hz), sends it immediately after a set-home, and answers `MAV_CMD_GET_HOME_POSITION` and `REQUEST_MESSAGE` queries. QGC displays the launch marker and accurate distance-to-home.
- **ArduPilot-Consistent Semantics:** Arming still captures home at the drone's current position; a GCS set-home afterwards redirects RTL to the new point. Setting home never teleports the drone — matching real autopilot behavior.

### 2. COMMAND_INT Protocol Support
- **New Transport Handled:** `COMMAND_INT` (message #75) is now dispatched into the same command handler as `COMMAND_LONG`. Previously these packets were dropped silently with no acknowledgement even though the capability was advertised, causing QGC command timeouts.
- **Every supported command** (arm/disarm, takeoff, land, set mode, mission start, change speed, set home, and more) now works over both transports, and unsupported commands receive an honest `UNSUPPORTED` acknowledgement.

### 3. Multi-GCS Telemetry & Link Stability
- **Simultaneous GCS Connections:** Telemetry now streams to every active GCS peer in parallel (peers expire after 10 s of silence). Split-screen QGC on the phone and desktop QGC over Wi-Fi can run at the same time without stealing the stream from each other.
- **Peer Validation:** Only datagrams that parse as MAVLink from a non-vehicle system ID register a telemetry peer — stray network packets can no longer redirect the stream.
- **Fixed "Communication Lost" with Screen Off:** The simulation service now holds a Wi-Fi lock and partial wake lock, preventing Android Wi-Fi power saving and Doze from stalling UDP heartbeats when the screen is off or MAVLab is backgrounded.

---

## Upgrade Instructions
1. Download `MAVLab-v1.5.1.apk` from the GitHub Releases page and install it over the existing app, or build from source:
   ```bash
   cd mavlab-android
   GRADLE_USER_HOME="$PWD/.gradle" ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/MAVlab.apk
   ```
2. Connect QGroundControl, arm, and try "Set Home Here" on the map after takeoff — the launch marker moves, and RTL returns to it.
3. Optionally connect desktop QGC and split-screen QGC at the same time; both now receive full telemetry.
