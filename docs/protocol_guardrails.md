# MAVLink Protocol Invariants & Guardrails

This document outlines the safety guardrails, state invariants, and network rules governing MAVLink communication between MAVLab and Ground Control Stations (GCS) like QGroundControl.

---

## 1. System & Component ID Guardrails

MAVLab enforces a strict identity separation to prevent loopbacks, self-hearbeats, or network conflicts, especially when run in split-screen mode on the same device as the GCS:

- **MAVLab Vehicle ID:** Defaults to `SYSID = 1`, `COMPID = 1` (Autopilot).
- **Recommended GCS ID:** Defaults to `SYSID = 255`, `COMPID = 190`.
- **Self-Heartbeat Guardrail:**
  - MAVLab ignores all inbound MAVLink packets where the sender `System ID` matches the local vehicle `System ID` (typically `1`).
  - This prevents the simulator from attempting to connect to itself or parsing its own looped-back UDP broadcasts as GCS commands.

---

## 2. Heartbeat Rate & Connectivity State Invariants

- **Heartbeat Rate:**
  - `MavlinkUdpServer` broadcasts a `HEARTBEAT` message (ID `#0`) exactly once per second (1Hz).
- **GCS Connection State:**
  - **Connection Warmup:** A GCS connection is marked active (`gcsConnected = true`) only after receiving continuous heartbeat signals from the GCS for at least 3 seconds.
  - **Connection Timeout:** If no heartbeats are received from the GCS for more than 15 seconds, the connection is marked disconnected.
  - **Failsafe Reversion:** When GCS connection drops during an autonomous AUTO mission, the autopilot reverts control authority from `GCS_MISSION` to safety defaults (e.g. holds position or RTL) to prevent flyaways.

---

## 3. Control Authority Model

MAVLab uses a priority-based single-writer authority model to arbitrate between local user inputs and remote GCS commands:

```
[IDLE]
  |
  +--> [CONTROLLER] (Tilt / Manual On-Screen Sliders)
  |
  +--> [GCS_MISSION] (Autonomous QGC Mission / Guided Coordinates)
```

- **Manual Override:** Local operator inputs (e.g. tapping "LAND", disarming, or taking manual joystick control) immediately preempt the active `GCS_MISSION` authority and shift the state back to `CONTROLLER`.
- **Race Condition Prevention:** Autopilot state commands (armed, mode, coordinates) must explicitly verify the active `ControlAuthority` before writing to the state flow.

---

## 4. UDP Network & Port Mapping Invariants

To establish standard UDP socket communication:
- **Local Bind Port:** MAVLab binds to UDP port `14556` to receive packets from the GCS.
- **Remote Target Port:** MAVLab sends telemetry packets to UDP port `14550` (the standard QGroundControl listen port).
- **Unicast Transition:** When a GCS heartbeat is received, MAVLab records the sender's IP address and port and switches from UDP broadcast to targeted unicast to reduce network congestion.
