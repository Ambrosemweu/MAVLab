# Protocol Guardrails

These rules guided the Phase 1 implementation and remain the gate before Phase 2 physics starts.

## UDP Rules

- `0.0.0.0` is only a bind address.
- Never send packets to `0.0.0.0`.
- Same-phone QGC mode must define exactly how MAVLab and QGC share UDP ports.
- LAN mode must use an explicit QGC peer IP or a valid subnet broadcast address.
- MAVLab must listen for QGC commands on a known socket.

## MAVLink Rules

- Phase 1 sends `COMMAND_ACK` for supported commands.
- Phase 1 handles or explicitly denies `SET_MESSAGE_INTERVAL`.
- Phase 1 answers `PARAM_REQUEST_LIST` with a minimal parameter set.
- Every unsupported inbound QGC message should be logged.
- Each app install must use a stable, user-visible MAVLink system ID.
- `system_id = 1` is allowed only as a temporary development default.

## Phase Gate

Do not start Phase 2 physics until QGC can:

- detect MAVLab on the same phone
- detect MAVLab from desktop QGC on the same Wi-Fi
- arm and disarm
- change flight mode
- send takeoff and land
- receive ACKs
- disconnect and reconnect
- distinguish two MAVLab phones on the same network
