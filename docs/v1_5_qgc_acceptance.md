# QGroundControl Integration Acceptance Test Specification

This document details the environment setups and acceptance criteria for validating QGroundControl (QGC) compatibility with MAVLab v1.5.

---

## 1. Test Environments

### Environment A: Split-Screen Android Testing
Useful for single-device verification:
1. Enable developer options on your Android device and enable **split-screen/multi-window for all apps**.
2. Open **MAVLab** and drag it to occupy the top half of the screen.
3. Open **QGroundControl** on the bottom half of the screen.
4. QGC will automatically listen on localhost port `14550` and discover the MAVLab vehicle broadcast.

### Environment B: Cross-Device Local Wi-Fi Testing
Useful for representative field simulation:
1. Connect both the Android device (running MAVLab) and a computer (running QGC Desktop) to the **same Wi-Fi router/hotspot**.
2. Ensure no firewall blocks UDP port `14550` on the computer.
3. Launch MAVLab on the phone and QGC on the desktop. The UDP broadcast from MAVLab will auto-register the drone on the desktop QGC screen.

---

## 2. Acceptance Checklist

| Item | Action | Verification |
|---|---|---|
| **Discovery** | Open both apps | QGC discovers MAVLab within 5 seconds. Audio announcement: *"Armed"* or *"Disarmed"*. Heartbeat indicators turn green. |
| **Arm/Disarm** | Click Arm/Disarm in QGC | MAVLab state updates immediately. Propellers in SIM start spinning on arm and stop on disarm. |
| **Command ACK** | Click Takeoff / Land in QGC | QGC receives ACK packet and acknowledges command execution. Drone moves vertically. |
| **Mission Upload**| Create waypoints in QGC and tap Upload | MAVLab accepts all mission items, prints `MISSION RESTORED`, and shows waypoints in its **Mission** tab. |
| **AUTO Flight** | Start AUTO mission in QGC | Drone takes off, navigates waypoints sequentially. MAVLab transmits current WP indices. QGC active line moves. |
| **Link Reconnect** | Toggle Wi-Fi off for 10 seconds, then back on | QGC reports link loss, then reconnects and resumes telemetry updates once link is restored. |
| **Stability** | Keep simulation running for 10 minutes | Telemetry flow remains stable. App memory remains constant. No ANRs (App Not Responding) or crashes. |
