# MAVLab MAVLink System ID / QGC Connection Spec

> Scope: diagnosis + implementation specification only. This document defines how MAVLab should identify itself on MAVLink when mimicking ArduPilot and connecting to GCS software such as QGroundControl.

**Project:** MAVLab Android

**Problem reported:**

A same-phone QGC test used system ID `174` and UDP `14550`. QGC could see a drone, but MAVLab/QGC mission upload flow behaved like nothing was connected. The app then added conflict detection that changes MAVLab to the next system ID when it sees a collision. That created a bad loop where the operator keeps switching to the next system ID.

**Affected files to inspect during implementation:**

```text
mavlab-android/app/src/main/java/com/ascend/mavlab/core/common/AppRuntime.kt
mavlab-android/app/src/main/java/com/ascend/mavlab/core/settings/AppSettings.kt
mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkSocketConfig.kt
mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkUdpServer.kt
mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkMessageBuilder.kt
mavlab-android/app/src/main/java/com/ascend/mavlab/feature/settings/SettingsScreen.kt
```

---

## 1. Diagnosis

The current behavior is probably wrong because MAVLab is treating a MAVLink system ID conflict as something it should automatically fix by changing the vehicle ID at runtime.

That is unsafe for a GCS connection.

A MAVLink vehicle system ID is part of the vehicle identity. QGC discovers a vehicle by heartbeat and then sends mission/command traffic targeted at that vehicle system ID. If MAVLab starts as system ID `174`, QGC discovers vehicle `174`, then MAVLab silently moves to `175`, QGC can be left with stale vehicle identity/state. That can produce exactly the symptom:

```text
QGC can see a drone / heartbeat appears,
but mission upload or Plan view says no connected vehicle / nothing uploadable.
```

The current code has this risky section:

```text
MavlinkUdpServer.handleHeartbeat()
  if inbound HEARTBEAT.systemId == vehicleSystemId:
      nextVehicleSystemId()
      rebuild MavlinkMessageBuilder
      onSystemIdChanged(nextSystemId)
```

This means a GCS heartbeat that happens to use the same system ID as MAVLab causes the vehicle to move while the link is alive.

The deeper issue is not that MAVLab needs a magical system ID. The issue is:

1. MAVLab should use a stable ArduPilot-like vehicle system ID.
2. QGC should use a different GCS system ID.
3. MAVLab should not auto-renumber itself during a live connection.
4. If a conflict is detected, MAVLab should report it and tell the user exactly what to change.

---

## 2. MAVLink / ArduPilot Research Findings

### 2.1 MAVLink ID rule

MAVLink identifies systems and components by:

```text
(system_id, component_id)
```

Each MAVLink system on the network must have a unique system ID.

A MAVLink system may contain multiple components sharing the same system ID, but those components must have unique component IDs.

Reference:

- https://mavlink.io/en/services/mavlink_id_assignment.html

Relevant MAVLink guidance:

- any ID in range `1..255` may be used
- autopilots typically default to system ID `1`
- GCS applications typically use IDs near `255`
- MAVLink SDKs often use middle-range IDs

### 2.2 ArduPilot vehicle system ID

ArduPilot’s vehicle system ID parameter is historically:

```text
SYSID_THISMAV
```

Newer ArduPilot code is moving this under the MAVLink parameter namespace as:

```text
MAV_SYSID
```

ArduPilot default:

```text
vehicle system ID = 1
```

ArduPilot source reference:

- `libraries/GCS_MAVLink/GCS.cpp`
- `MAV_SYSID_DEFAULT` is `1` for normal vehicle builds.
- AntennaTracker is a special exception with default `2`.

Reference source:

- https://github.com/ArduPilot/ardupilot/blob/master/libraries/GCS_MAVLink/GCS.cpp

ArduPilot parameter docs:

- https://ardupilot.org/copter/docs/parameters-Copter-stable-V4.5.0.html#sysid-thismav-mavlink-system-id-of-this-vehicle

### 2.3 ArduPilot ground-station system ID

ArduPilot’s GCS system ID parameter is historically:

```text
SYSID_MYGCS
```

Newer ArduPilot code uses:

```text
MAV_GCS_SYSID
```

ArduPilot default:

```text
GCS system ID = 255
```

ArduPilot source reference:

- `libraries/GCS_MAVLink/GCS.cpp`
- `_GCS_SYSID` default is `255`

Reference source:

- https://github.com/ArduPilot/ardupilot/blob/master/libraries/GCS_MAVLink/GCS.cpp

### 2.4 QGroundControl default system ID

QGroundControl’s MAVLink System ID setting defaults to:

```text
255
```

QGC metadata source:

```json
{
  "name": "gcsMavlinkSystemID",
  "shortDesc": "MAVLink system identifier (1-255) for this ground station.",
  "type": "uint32",
  "default": 255,
  "min": 1,
  "max": 255,
  "label": "MAVLink System ID"
}
```

Reference source:

- https://github.com/mavlink/qgroundcontrol/blob/master/src/Settings/Mavlink.SettingsGroup.json

---

## 3. Decision

MAVLab should mimic ArduPilot by default.

Therefore:

```text
MAVLab vehicle system ID: 1
MAVLab autopilot component ID: 1
QGC ground-station system ID: 255
UDP receive/bind port: 14551 for MAVLab same-device compatibility
QGC UDP listening port: 14550
```

### Why not use 174?

`174` is not wrong according to MAVLink, but it is not the right default for an ArduPilot-like single-vehicle simulator.

It appears MAVLab currently derives the system ID from Android ID:

```kotlin
return (androidId.hashCode().absoluteValue % 250) + 1
```

This can produce arbitrary IDs such as `174`. That is bad for a product that needs predictable QGC setup and ArduPilot-like behavior.

### Is there a dedicated MAVLab system ID that cannot conflict?

No.

MAVLink does not reserve a special dedicated system ID for MAVLab. The correct strategy is not to hunt for a magic ID. The correct strategy is to follow MAVLink/ArduPilot convention:

- vehicle/autopilot: `1`
- GCS/QGC: `255`
- additional simulated vehicles: `2`, `3`, `4`, ...
- companion/SDK tools: middle-range IDs, but only if they are separate MAVLink systems

---

## 4. Required Behavior

### 4.1 Stable vehicle ID

MAVLab must use a stable vehicle system ID for the entire process/session.

Default:

```kotlin
const val DefaultMavLabVehicleSystemId = 1
const val DefaultMavLabAutopilotComponentId = 1
const val DefaultQgcSystemId = 255
```

Do not derive the default system ID from Android ID.

Do not mutate the vehicle system ID after MAVLink starts.

### 4.2 No automatic next-ID switching

Remove the behavior where MAVLab moves to `nextVehicleSystemId()` after receiving a colliding heartbeat.

Bad behavior:

```text
MAVLab starts as 174
QGC heartbeat says 174
MAVLab moves itself to 175
QGC now has stale vehicle identity
operator changes QGC/MAVLab again
cycle repeats
```

Correct behavior:

```text
MAVLab starts as 1
QGC heartbeat says 255
No conflict
Mission upload proceeds
```

If a conflict happens:

```text
MAVLab starts as 1
QGC heartbeat says 1
MAVLab keeps system ID 1
MAVLab shows a clear error: GCS is using vehicle system ID 1. Set QGC MAVLink System ID to 255.
```

### 4.3 Conflict detection should diagnose, not self-change

MAVLab should still detect a conflict, but the action should be diagnostic only.

Current behavior:

```text
collision -> mutate system ID
```

Required behavior:

```text
collision -> mark link as conflicted -> show Ops warning -> keep vehicle ID stable
```

Suggested status model:

```kotlin
data class MavlinkIdentityStatus(
    val vehicleSystemId: Int = 1,
    val vehicleComponentId: Int = 1,
    val lastGcsSystemId: Int? = null,
    val lastGcsComponentId: Int? = null,
    val identityConflict: Boolean = false,
    val recommendedGcsSystemId: Int = 255,
    val message: String = "",
)
```

Example warning:

```text
MAVLink identity conflict
MAVLab vehicle SYSID is 1.
QGC heartbeat is also using SYSID 1.
Set QGC MAVLink System ID to 255 and reconnect.
Do not change MAVLab vehicle SYSID unless running multiple simulated vehicles.
```

### 4.4 Target filtering

MAVLab should parse target fields in inbound commands/mission messages and only act when the message is for:

```text
target_system == MAVLab vehicle system ID
or target_system == 0  // broadcast, where MAVLink message semantics allow broadcast
```

For QGC mission upload, this matters a lot.

If QGC sends mission upload targeted to system ID `174`, but MAVLab has changed itself to `175`, MAVLab may ignore it or answer inconsistently.

Required rule:

- If message has `target_system` and it is neither `0` nor `vehicleSystemId`, ignore and log:

```text
Ignored MISSION_COUNT target_system=174 vehicle_system=1
```

- Do not use packet source system ID as proof that the message targets MAVLab.

### 4.5 PARAM_VALUE must advertise the same stable ID

When QGC requests parameters, MAVLab should report:

```text
SYSID_THISMAV = 1
MAV_SYSID = 1  // optional newer ArduPilot-compatible alias
SYSID_MYGCS = 255
MAV_GCS_SYSID = 255  // optional newer ArduPilot-compatible alias
```

MVP may only need old ArduPilot names, but adding both old and new names is useful for compatibility.

Current code already returns:

```kotlin
"SYSID_THISMAV" to vehicleSystemId.toFloat()
```

Spec requirement:

- keep value stable
- do not let it change at runtime due to collision
- include GCS ID parameter for clarity

---

## 5. Recommended MAVLab Defaults

### Single MAVLab simulator + QGC

```text
MAVLab vehicle SYSID: 1
MAVLab component ID: 1
QGC MAVLink System ID: 255
MAVLab bind UDP: 14551
QGC UDP: 14550
```

### Two MAVLab simulators on same network

```text
MAVLab A vehicle SYSID: 1
MAVLab B vehicle SYSID: 2
QGC MAVLink System ID: 255
```

### Multiple GCS tools

If using QGC plus another GCS, keep QGC at `255` and assign the second GCS another high ID, for example:

```text
QGC: 255
Second GCS: 254
MAVLab: 1
```

### MAVSDK / companion app

If adding a companion/SDK process later, use a middle-range ID such as:

```text
MAVSDK/companion: 190
```

But that companion should not pretend to be the vehicle.

---

## 6. Required Code Changes

### 6.1 Replace Android-derived system ID

Current code:

```kotlin
private fun stableSystemId(context: Context): Int {
    val androidId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID,
    ).orEmpty()
    return (androidId.hashCode().absoluteValue % 250) + 1
}
```

Required behavior:

```kotlin
private fun stableSystemId(context: Context): Int {
    return DefaultMavLabVehicleSystemId
}
```

Better future behavior:

```kotlin
private fun stableSystemId(context: Context): Int {
    return settingsRepository.vehicleSystemId ?: DefaultMavLabVehicleSystemId
}
```

Do not use Android ID hashing.

### 6.2 Update AppSettings default

Current:

```kotlin
fun defaults(systemId: Int = 1): AppSettings
```

Keep default `1` and ensure runtime actually uses it.

### 6.3 Update MavlinkSocketConfig defaults

Recommended:

```kotlin
data class MavlinkSocketConfig(
    val localBindPort: Int = 14551,
    val sameDeviceHost: String = "127.0.0.1",
    val sameDeviceQgcPort: Int = 14550,
    val lanDestinations: List<UdpDestination> = emptyList(),
    val systemId: Int = DefaultMavLabVehicleSystemId,
    val componentId: Int = MAV_COMP_ID_AUTOPILOT1,
)
```

### 6.4 Remove runtime auto-renumbering

Remove or disable:

```kotlin
nextVehicleSystemId()
onSystemIdChanged(nextSystemId)
builder = MavlinkMessageBuilder(nextSystemId, config.componentId)
```

from heartbeat collision handling.

Replacement:

```kotlin
if (collision) {
    identityStatus.value = identityStatus.value.copy(
        lastGcsSystemId = packet.systemId,
        lastGcsComponentId = packet.componentId,
        identityConflict = true,
        recommendedGcsSystemId = 255,
        message = "QGC/GCS is using MAVLab vehicle SYSID $vehicleSystemId. Set QGC MAVLink System ID to 255.",
    )
    simLoop.noteInbound("MAVLink SYSID conflict: GCS=${packet.systemId}, vehicle=$vehicleSystemId")
    return
}
```

### 6.5 Add identity status to Ops

Ops should show:

```text
Vehicle SYSID: 1
Vehicle COMPID: 1
Expected QGC SYSID: 255
Last GCS SYSID: 255
Last GCS COMPID: 190
Identity: OK
```

If conflict:

```text
Identity: CONFLICT
QGC is using MAVLab's vehicle system ID.
Set QGC MAVLink System ID to 255.
Restart QGC link after changing it.
```

---

## 7. QGC Setup Instructions for User Documentation

Add this to Ops or a help card:

```text
Recommended QGC setup for MAVLab:

1. Open QGroundControl settings.
2. Go to MAVLink settings.
3. Set QGC MAVLink System ID to 255.
4. Keep MAVLab Vehicle System ID at 1.
5. For same-phone testing, use UDP 14550 for QGC and MAVLab bind 14551.
6. Restart QGC or reconnect the UDP link after changing System ID.
```

Warning:

```text
Do not set QGC System ID to the same value as MAVLab Vehicle System ID.
If MAVLab is vehicle 1, QGC should be 255.
```

---

## 8. Mission Upload Rules Related to SYSID

Mission upload should only proceed when identity is valid.

Before accepting `MISSION_COUNT`, check:

1. Source system is not equal to MAVLab vehicle system ID.
2. Target system equals MAVLab vehicle system ID or broadcast, depending on message semantics.
3. Identity status is not conflicted.

If conflicted, reject mission upload clearly:

```text
MISSION_ACK: MAV_MISSION_DENIED
Reason: MAVLink system ID conflict. Set QGC system ID to 255.
```

If QGC targets the wrong vehicle ID:

```text
Ignore message and log target mismatch.
```

Ops should show:

```text
Mission upload blocked: QGC targeted SYSID 174, MAVLab vehicle SYSID is 1.
```

---

## 9. Tests to Add

### 9.1 Default identity test

```text
AppRuntime / MavlinkSocketConfig default vehicle system ID is 1.
```

### 9.2 No Android hash test

```text
stableSystemId() does not derive arbitrary values from ANDROID_ID.
```

### 9.3 Collision does not mutate vehicle ID

Given:

```text
MAVLab vehicleSystemId = 1
Inbound HEARTBEAT from sysid = 1
```

Expect:

```text
vehicleSystemId remains 1
identityConflict = true
recommendedGcsSystemId = 255
status contains conflict warning
```

### 9.4 Normal QGC heartbeat accepted

Given:

```text
MAVLab vehicleSystemId = 1
Inbound QGC HEARTBEAT from sysid = 255
```

Expect:

```text
identityConflict = false
lastGcsSystemId = 255
vehicleSystemId remains 1
```

### 9.5 Target mismatch ignored

Given:

```text
MAVLab vehicleSystemId = 1
MISSION_COUNT target_system = 174
```

Expect:

```text
MAVLab does not start mission upload
Ops logs target mismatch
```

### 9.6 Correct mission upload accepted

Given:

```text
MAVLab vehicleSystemId = 1
QGC system ID = 255
MISSION_COUNT target_system = 1
```

Expect:

```text
MAVLab starts upload session
sends MISSION_REQUEST_INT targeted to sysid 255
accepts mission items
sends MISSION_ACK accepted
```

---

## 10. Acceptance Criteria

This issue is fixed when:

- MAVLab defaults to vehicle system ID `1`.
- MAVLab defaults to component ID `1`.
- QGC is expected to use system ID `255`.
- MAVLab no longer derives default SYSID from Android ID.
- MAVLab no longer changes its own SYSID after receiving a heartbeat.
- SYSID conflict detection remains, but only as a diagnostic warning.
- Ops clearly shows vehicle SYSID, QGC SYSID, and identity health.
- Mission upload is blocked with a clear reason if SYSID conflict exists.
- Mission upload works when MAVLab is `1` and QGC is `255`.
- QGC same-phone test can detect MAVLab, upload a mission, and start AUTO without the operator constantly switching IDs.

---

## 11. Recommended Immediate Manual Test

1. Reset MAVLab vehicle SYSID to `1`.
2. Open QGC settings.
3. Set QGC MAVLink System ID to `255`.
4. Keep QGC UDP listening on `14550`.
5. Start MAVLab.
6. Confirm Ops shows:

```text
Vehicle SYSID: 1
Last GCS SYSID: 255
Identity: OK
```

7. Create a 4-waypoint mission in QGC.
8. Upload mission.
9. Confirm Mission tab receives all waypoints.
10. Start AUTO.
11. Confirm Cockpit says `Control: GCS Mission`.

---

## 12. Non-Goals

Do not create a proprietary MAVLab reserved SYSID.

Do not auto-increment MAVLab vehicle ID on conflict.

Do not tell the user to keep trying random system IDs.

Do not make QGC and MAVLab share the same system ID.

Do not solve multi-vehicle fleet management in this MVP beyond allowing a manually configured vehicle SYSID later.
