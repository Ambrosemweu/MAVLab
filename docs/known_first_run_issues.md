# Known first-run issues (read this before posting)

This guidance applies to v1.6.0 unless noted. If none matches, open a [First-run problem](https://github.com/Labs-Ascend/MAVLab/issues/new?template=first_run_problem.md) issue.

## 1. QGroundControl doesn't discover the vehicle

- **Same phone (on-device):** QGC listens on `127.0.0.1:14550`. Open MAVLab first, then switch to QGC within the one-minute handoff window. No split-screen is needed.
- **System IDs:** MAVLab currently uses system ID `1` and component ID `1`; QGC normally uses GCS system ID `255`. Unique per-install vehicle IDs are not implemented. Use an isolated simulator network; do not connect same-ID simulators or real aircraft together.
- **QGC version drift:** save your plan and reconnect after upgrading. MAVLab v1.6.0 emits unsigned MAVLink 2 for newer QGC; v1.5.3's older wire behavior can cause missing ACK/timeouts. Changing SYSID cannot fix that mismatch. Report the exact QGC build and MAVLab version.

## 2. Desktop QGC over Wi-Fi — no link

- Phone and computer must be on the **same subnet**.
- Many "guest" Wi-Fi networks and some routers use **client isolation / AP isolation**, which blocks device-to-device UDP. Use a trusted LAN or a phone hotspot.
- A desktop **firewall** may block UDP 14550 — allow it inbound.

## 3. APK won't install

The official v1.5.3 and this v1.6.0 debug APK have different signing certificates; an in-place update between them is not supported. Do not uninstall or clear data merely to bypass that error. See [backup and installation consequences](user/upgrading.md).

Android blocks sideloaded APKs by default. When prompted, allow installs from the app you opened the APK with (file manager / browser). This is normal.

## 4. Backgrounded networking

When MAVLab is backgrounded, phone-sensor input stops immediately, but motor audio remains synchronized with the retained simulation so QGroundControl arm and flight commands stay audible. Telemetry remains active for a one-minute GCS handoff and continues while a validated GCS heartbeat is present or the vehicle is armed/running an AUTO mission. If MAVLab is idle and disconnected, the runtime, audio, wake/Wi-Fi locks, and service notification stop automatically. Returning to MAVLab restarts them. Explicitly closing MAVLab or tapping **Stop simulation** in its notification stops immediately.

## 5. Mission upload fails / no progress

Keep the simulated vehicle **disarmed** while diagnosing uploads. Open Ops → Mission upload for the exact requested/received sequence and rejection. An accepted upload is not an executing AUTO mission. Do not repeatedly send arm/takeoff commands to resolve a timeout; a command can execute even if its ACK is not received. See [mission operation](user/mission.md).

## 6. Fence/rally upload errors

Install the updated candidate, save your QGC plan and reconnect to refresh capabilities. Navigation, fence and rally are separate lists. Upload safety plans while disarmed; invalid/unsupported coordinates, geometry or commands are rejected without replacing the accepted plan. Uploading a fence does not enable it. Review [flight boundaries](user/flight-boundaries.md); SmartRTL, terrain frames and obstacle routing are not supported.

## Still stuck?

Open a [First-run problem](https://github.com/Labs-Ascend/MAVLab/issues/new?template=first_run_problem.md) issue with MAVLab version, device/Android, exact QGC build, connection mode and the error text. Include Ops upload/ACK evidence when available.
