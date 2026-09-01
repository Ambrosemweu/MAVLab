# MAVLink and QGroundControl Contract

Audience: Protocol contributors and release validators
Status: Current v1.5.3 contract
Last verified: 2026-09-01

This document maps what MAVLab claims at the QGroundControl boundary.

## Identity

Current defaults:

- simulated vehicle SYSID: `1`;
- autopilot COMPID: `1`;
- recommended QGC SYSID: `255`.

Current code returns SYSID 1. Older documents describing a generated unique per-install ID are aspirational, not implemented behavior.

MAVLab reports a pinned ArduCopter 4.6.3-compatible development identity and custom identifier `MAVLAB`. It never claims to be an official ArduPilot firmware build.

## UDP topology

- MAVLab attempts to bind UDP `14551`, with an ephemeral fallback if unavailable.
- Same-device QGC telemetry targets `127.0.0.1:14550`.
- LAN discovery/peer destinations may also receive telemetry.
- Before peer detection, heartbeat discovery packets go to configured destinations.
- After detection, telemetry goes to active peers.

Documents claiming bind port `14556` are stale.

## Timing and connection

The telemetry burst is nominally every 200 ms (5 Hz), and the current path includes heartbeat in that burst. Do not describe heartbeat as exactly 1 Hz without changing or measuring implementation.

The connection policy uses roughly three seconds of continuous GCS heartbeat to validate a GCS and fifteen seconds without heartbeat to expire it. Only heartbeat packets whose MAVLink vehicle type is `MAV_TYPE_GCS` establish this state. Protocol rate, peer expiry, and service-retention timing are separate concepts.

## Capability classes

| Class | Meaning |
|---|---|
| Implemented | Produces the documented effect and has focused evidence. |
| Partial | A useful subset works; limits are stated. |
| Accepted no-op | Acknowledged without the complete requested effect. |
| Unsupported | Rejected, appropriately ignored, or not advertised. |

An accepted ACK is never proof of complete implementation.

## Current inbound areas

Code includes mode changes, `COMMAND_LONG`, `COMMAND_INT`, parameters, mission upload/download/current/clear, and selected arm/takeoff/land/mission/calibration flows. Some stream-interval and calibration requests are accepted without full behavior. Keep exact classification synchronized with source and tests.

## Mission evidence

Validate upload count/sequence, item decoding, ACK behavior, engine loading, mission start/progress, clear/set-current, reconnect/re-upload, and malformed/unsupported items.

## Scope

MAVLab exposes a simulated vehicle. Protocol compatibility does not certify real-aircraft control, ArduPilot firmware equivalence, or safe hardware operation.

Primary evidence: `MavlinkUdpServer.kt`, `MavlinkMessageBuilder.kt`, `MavlinkIdentityStatus.kt`, `ArduPilotCompatibilityProfile.kt`, focused tests, and versioned records under `docs/validation/results/`.
