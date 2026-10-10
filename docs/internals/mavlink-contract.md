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

The canonical phone-sensor correction changes local takeoff to ALT_HOLD so local attitude steering remains effective. MAVLink takeoff still passes GCS_DIRECT authority to the engine and retains GUIDED position control; command ACKs and capability declarations are unchanged. Focused engine tests cover this separation.

## Mission evidence

Validate upload count/sequence, item decoding, ACK behavior, engine loading, mission start/progress, clear/set-current, reconnect/re-upload, and malformed/unsupported items.

## Scope

MAVLab exposes a simulated vehicle. Protocol compatibility does not certify real-aircraft control, ArduPilot firmware equivalence, or safe hardware operation.

Primary evidence: `MavlinkUdpServer.kt`, `MavlinkMessageBuilder.kt`, `MavlinkIdentityStatus.kt`, `ArduPilotCompatibilityProfile.kt`, focused tests, and versioned records under `docs/validation/results/`.

## Mission consolidation boundary changes (UI/UX branch)

- Upload completion records GCS source; persisted restoration is not a new upload ACK. AUTO mission progress requires an armed autopilot; mode/ACK alone does not establish execution.
- Unknown mission command IDs are rejected with UNSUPPORTED (3). Unsupported frames are rejected with UNSUPPORTED_FRAME (2); diagnostics name command/frame/zero-based sequence. Invalid values retain INVALID (5). Accepted command IDs are 16, 22, 21, 20, 19 and 178. An unsuccessful replacement does not remove the previous loaded mission.
- Navigation coordinates accept geographic GLOBAL / GLOBAL_INT (0/5, MSL) or GLOBAL_RELATIVE_ALT / INT (3/6, home-relative metres). Frame MISSION (2) accepts only CHANGE_SPEED or RTL. Local/body/terrain frames are rejected; no terrain model is implemented. Raw MSL altitude converts to AGL at engine load using current simulation ground MSL. Flat simulation ground makes relative-home altitude equal AGL. See [MAVLink frame definitions](https://mavlink.io/en/messages/common.html#MAV_FRAME) and [mission results](https://mavlink.io/en/messages/common.html#MAV_MISSION_RESULT).
- LOITER_TIME is **partial**: arrival is implemented, timed dwell is not. autocontinue=false is an **accepted no-op for pausing**: the engine still advances on arrival. These limitations appear in Ops and affected navigation nodes are hollow in SIM. CHANGE_SPEED retains the existing speed handling and has no spatial node.
- Local Control's Start AUTO/Clear use guarded application actions; this does not redefine external mission start/clear ACKs, add pause/resume support, or advertise new capabilities. Live QGC upload/rejection/start/land interoperability needs separate evidence.
