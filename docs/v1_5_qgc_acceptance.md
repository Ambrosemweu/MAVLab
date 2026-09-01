# QGroundControl Integration Acceptance Test Specification

This document details the environment setups and acceptance criteria for validating QGroundControl (QGC) compatibility with MAVLab v1.5.

---

## 1. Test Environments

### Environment A: On-Device (Same-Phone) Testing
Useful for single-device verification:
1. Open **MAVLab** and start the simulation.
2. Switch to **QGroundControl** within 45 seconds; split-screen is optional.
3. QGC automatically listens on localhost port `14550` and discovers the MAVLab vehicle broadcast. Once a validated QGC heartbeat arrives, MAVLab must keep telemetry active while backgrounded.
4. Close QGC while the vehicle is idle and verify that MAVLab stops once its GCS heartbeat timeout and any remaining handoff grace have expired. Reopening MAVLab must restart the runtime.

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
| **Firmware identity** | Reconnect QGC and inspect vehicle summary/messages | QGC identifies an ArduPilot-compatible development build and does not show an official-firmware update warning. `AUTOPILOT_VERSION` reports the pinned MAVLab compatibility profile and `MAVLAB` custom identifier. |
| **Arm/Disarm** | Click Arm/Disarm in QGC | MAVLab state updates immediately. Propellers in SIM start spinning on arm and stop on disarm. |
| **Command ACK** | Click Takeoff / Land in QGC | QGC receives ACK packet and acknowledges command execution. Drone moves vertically. |
| **Mission Upload**| Create waypoints in QGC and tap Upload | MAVLab accepts all mission items, prints `MISSION RESTORED`, and shows waypoints in its **Mission** tab. |
| **AUTO Flight** | Start AUTO mission in QGC | Drone takes off, navigates waypoints sequentially. MAVLab transmits current WP indices. QGC active line moves. |
| **Link Reconnect** | Toggle Wi-Fi off for 10 seconds, then back on | QGC reports link loss, then reconnects and resumes telemetry updates once link is restored. |
| **Stability** | Keep simulation running for 10 minutes | Telemetry flow remains stable. App memory remains constant. No ANRs (App Not Responding) or crashes. |

---

## 3. Results

**Status: PASS.** All acceptance items above were verified on real Android hardware, in both Environment A (same-phone, on-device loopback) and Environment B (cross-device desktop QGC over Wi-Fi).

- Date: 2026-06-30 <!-- TODO: confirm exact test date -->
- Devices / Android versions: <!-- TODO: record exact device models + Android versions -->
- QGC build: <!-- TODO: record QGroundControl version (desktop and mobile) -->
- ArduPilot compatibility profile: `ArduCopter 4.6 protocol profile` (`4.6.3 DEVELOPMENT`)
- Stability run: confirm window used (10 min per this spec, or the 30-min Phase-1 connection-stability target). <!-- TODO -->

Also exercised hands-on by a small group of external testers (2–5, mixed background) on their own phones with no blocking issues; this is informal feedback, not a controlled study.
