# MAVLab Bug Report — MAVLink Protocol & QGC Connectivity

> **Project:** MAVLab Android Drone Simulator  
> **Date:** 2026-06-11  
> **Reported by:** Internal audit  
> **Status:** Fix committed (working tree)  
> **Affected version:** Committed `main` (clean build)

---

## Summary

A clean build of MAVLab exhibits five interrelated bugs that prevent reliable QGroundControl (QGC) connectivity, mission upload, and telemetry display. All five share a common thread: the committed MAVLink implementation deviates from the MAVLink wire specification and ArduPilot conventions.

The working tree already contains fixes for all five bugs. This document records the root causes, observed symptoms, and verification status.

---

## Root Cause: MAVLink System ID Mismatch

**Severity:** Critical  
**Symptom:** QGC sees heartbeats and shows a drone, but mission upload / Plan view reports nothing connected.

The most damaging problem is in the committed [`AppRuntime.kt`](file:///home/ambrose/Downloads/Ascend/Drone%20SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/core/common/AppRuntime.kt).

MAVLab derives its vehicle system ID from the phone's Android ID:

```kotlin
// AppRuntime.kt — Lines 165–171 (committed version)
private fun stableSystemId(context: Context): Int {
    val androidId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID,
    ).orEmpty()
    return (androidId.hashCode().absoluteValue % 250) + 1
}
```

That produces an **unpredictable ID** like `174` instead of ArduPilot's standard `1`. QGC defaults to system ID `255`, but if QGC is ever set to match MAVLab's random ID — or QGC caches an old vehicle ID — the following failure mode occurs:

1. QGC sees heartbeats and shows a drone.
2. Mission upload / Plan view says nothing is connected or uploadable.

### Fix Applied

The working tree pins MAVLab to **vehicle SYSID = 1** and adds identity-conflict detection in [`MavlinkUdpServer.handleHeartbeat()`](file:///home/ambrose/Downloads/Ascend/Drone%20SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkUdpServer.kt):

```kotlin
// AppRuntime.kt — Fixed version
private fun stableSystemId(context: Context): Int {
    return DefaultMavLabVehicleSystemId  // = 1
}
```

---

## Bug 2: Wrong MAVLink Field Layouts (Telemetry Looks Broken)

**Severity:** High  
**Symptom:** QGC HUD instruments show garbage values for airspeed, heading, throttle, and altitude.

The committed [`MavlinkMessageBuilder`](file:///home/ambrose/Downloads/Ascend/Drone%20SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkMessageBuilder.kt) had incorrect byte layouts for outbound messages:

### VFR_HUD

Fields were written in the **wrong order**. MAVLink expects:

```
airspeed → groundspeed → heading (int16) → throttle (uint16) → alt → climb
```

The committed code put `altitude` and `verticalSpeed` floats where `heading` and `throttle` belong, so QGC HUD instruments display garbage.

### BATTERY_STATUS

Had an **extra 4-byte `putInt(0)`** at the start, shifting every field after it by 4 bytes.

### Fix Applied

Both layouts corrected in the working tree. Verified by `MavlinkMessageBuilderTest`.

---

## Bug 3: Wrong Sequence Offsets in Mission Protocol

**Severity:** High  
**Symptom:** Mission download fails; setting the active waypoint is ignored.

The committed [`MavlinkUdpServer`](file:///home/ambrose/Downloads/Ascend/Drone%20SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkUdpServer.kt) read the waypoint sequence number from **byte offset 2** in several handlers. Per the MAVLink spec, `seq` is a `uint16` at **offset 0**; offset 2 is `target_system`.

| Message | Committed (wrong) | Fixed (correct) |
|---|---|---|
| `MISSION_REQUEST` / `MISSION_REQUEST_INT` | `leUInt16(2)` | `leUInt16(0)` |
| `MISSION_SET_CURRENT` | `leUInt16(2)` | `leUInt16(0)` |

This breaks mission download and setting the active waypoint. Mission upload (`MISSION_COUNT` → `MISSION_ITEM_INT`) uses different parsing and may partially work — which matches the observed symptom of "vehicle visible but missions don't work."

### Fix Applied

All sequence-number offsets corrected to `leUInt16(0)` in the working tree.

---

## Bug 4: Missing Autopilot Capability Handshake

**Severity:** Medium  
**Symptom:** QGC may refuse mission upload or fall back to legacy protocol unpredictably.

Modern QGC requests `AUTOPILOT_VERSION` (via `REQUEST_AUTOPILOT_CAPABILITIES` or `REQUEST_MESSAGE`) before mission upload to check capabilities like `MISSION_INT`. The committed code had:

- **No handler** for `REQUEST_AUTOPILOT_CAPABILITIES`
- **No handler** for `REQUEST_MESSAGE` (when requesting `AUTOPILOT_VERSION`)
- **No `autopilotVersion()` message builder**

Without responding to this handshake, QGC may refuse mission upload or fall back unpredictably.

### Fix Applied

The working tree adds:

- `autopilotVersion()` message builder in `MavlinkMessageBuilder`
- Handlers for both `REQUEST_AUTOPILOT_CAPABILITIES` and `REQUEST_MESSAGE` in `MavlinkUdpServer`

---

## Bug 5: No Target-System Filtering

**Severity:** Medium  
**Symptom:** Commands and missions addressed to other vehicles are silently mishandled.

The committed code processed inbound commands and mission messages **without checking** whether they were addressed to MAVLab's vehicle ID. Combined with the random Android-derived SYSID (Bug 1), this creates a scenario where:

1. QGC sends `MISSION_COUNT` targeted at system ID `174`.
2. MAVLab thinks it is system ID `1` (or vice versa).
3. Messages are silently mishandled or ignored.

### Fix Applied

The working tree adds `isTargetedToVehicle()` filtering and logs mismatches in Ops:

```
Ignored MISSION_COUNT target_system=174 vehicle_system=1
```

---

## Affected Files

| File | Changes |
|---|---|
| [`AppRuntime.kt`](file:///home/ambrose/Downloads/Ascend/Drone%20SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/core/common/AppRuntime.kt) | Pin vehicle SYSID to `1`, remove Android ID hashing |
| [`MavlinkMessageBuilder.kt`](file:///home/ambrose/Downloads/Ascend/Drone%20SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkMessageBuilder.kt) | Fix VFR_HUD field order, fix BATTERY_STATUS offset, add `autopilotVersion()` |
| [`MavlinkUdpServer.kt`](file:///home/ambrose/Downloads/Ascend/Drone%20SIM/mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkUdpServer.kt) | Fix `seq` offsets, add capability handlers, add target filtering, add conflict detection |

---

## Verification

| Bug | Test / Verification | Status |
|---|---|---|
| Bug 1 — System ID mismatch | Manual QGC test: SYSID `1` + QGC `255` | ✅ Fixed |
| Bug 2 — VFR_HUD / BATTERY_STATUS layout | `MavlinkMessageBuilderTest` | ✅ Fixed |
| Bug 3 — Mission seq offsets | Mission download + `MISSION_SET_CURRENT` test | ✅ Fixed |
| Bug 4 — Autopilot capability handshake | QGC mission upload flow (requests `AUTOPILOT_VERSION`) | ✅ Fixed |
| Bug 5 — Target-system filtering | Send command to wrong SYSID, verify ignored + logged | ✅ Fixed |

---

## Related Documentation

- [MAVLink System ID Spec](file:///home/ambrose/Downloads/Ascend/Drone%20SIM/docs/mavlab_mavlink_system_id_spec.md) — Full diagnosis, research, and implementation specification
- [Architecture](file:///home/ambrose/Downloads/Ascend/Drone%20SIM/docs/architecture.md) — MAVLab system architecture
- [MAVLink ID Assignment](https://mavlink.io/en/services/mavlink_id_assignment.html) — Official MAVLink specification
