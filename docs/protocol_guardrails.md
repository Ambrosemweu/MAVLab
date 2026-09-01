# MAVLink Protocol Invariants & Guardrails

This document outlines the safety guardrails, state invariants, and network rules governing MAVLink communication between MAVLab and Ground Control Stations (GCS) like QGroundControl.

---

## 1. System & Component ID Guardrails

MAVLab enforces a strict identity separation to prevent loopbacks, self-hearbeats, or network conflicts, especially when run on the same device as the GCS:

- **MAVLab Vehicle ID:** Defaults to `SYSID = 1`, `COMPID = 1` (Autopilot).
- **Recommended GCS ID:** Defaults to `SYSID = 255`, `COMPID = 190`.
- **Self-Heartbeat Guardrail:**
  - MAVLab ignores all inbound MAVLink packets where the sender `System ID` matches the local vehicle `System ID` (typically `1`).
  - This prevents the simulator from attempting to connect to itself or parsing its own looped-back UDP broadcasts as GCS commands.

---

## 2. Heartbeat Rate & Connectivity State Invariants

- **Heartbeat Rate:**
  - `MavlinkUdpServer` currently sends a telemetry burst every 200 ms (nominally 5 Hz), including `HEARTBEAT` while a peer is active. Treat this as implementation timing, not a guaranteed external rate contract.
- **GCS Connection State:**
  - **Connection Warmup:** A GCS connection is marked active (`gcsConnected = true`) only after receiving continuous heartbeat signals from the GCS for at least 3 seconds.
  - **Connection Timeout:** If no heartbeats are received from the GCS for more than 15 seconds, the connection is marked disconnected.
  - **Retention Boundary:** GCS connection state controls background-runtime retention. It does not currently implement a documented automatic RTL/hold transition when the heartbeat expires.

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
- **Local Bind Port:** MAVLab attempts to bind UDP port `14551` and falls back to an ephemeral port if it is unavailable.
- **Remote Target Port:** MAVLab sends telemetry packets to UDP port `14550` (the standard QGroundControl listen port).
- **Peer Transition:** Before peer detection, discovery heartbeats go to configured loopback/LAN destinations. After inbound traffic identifies active peers, full telemetry is sent to those peers until their activity expires.

---

## 5. ArduPilot Compatibility Identity

- MAVLab uses `MAV_AUTOPILOT_ARDUPILOTMEGA` so stock QGroundControl selects its
  ArduPilot vehicle plugin.
- MAVLab is not an official ArduPilot build. `AUTOPILOT_VERSION` must use
  `FIRMWARE_VERSION_TYPE_DEV` (`0`), never `FIRMWARE_VERSION_TYPE_OFFICIAL`
  (`255`).
- The compatibility baseline is pinned in `ArduPilotCompatibilityProfile.kt`.
  Do not change it merely because ArduPilot publishes a newer stable release.
- Middleware, Android OS, and board versions must be reported as `0` unless
  MAVLab has a truthful, separately versioned value for them.
- Capability bits must have matching handlers and contract tests before they
  are advertised.

See [ArduPilot Compatibility Contract](ardupilot_compatibility.md) for the
upgrade and release procedure.
