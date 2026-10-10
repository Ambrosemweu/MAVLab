# Mission operation

Missions are contextual across Cockpit, Control, SIM and Ops. There is no Mission tab, onboard mission authoring, sample route or Load route button.

1. Author and upload a mission using QGroundControl. Ops reports receiving counts, requested/received sequences and the exact rejection reason if upload fails.
2. Open Control's mission row. Accepted means items were received; it does not mean the vehicle is executing AUTO. Start AUTO appears when local start requirements are satisfied.
3. Confirm Start AUTO to arm the simulated vehicle and give GCS Mission authority. Local input pauses. Cockpit shows concise progress; expand Control for current/next item and essential mission numbers.
4. Select View route in SIM to inspect the received geometry alongside actual vehicle position and trail. A route ACK never animates the vehicle independently of physics.
5. To interrupt autonomous flight, confirm Take control & land. Landing replaces GCS Mission intent with Controller authority. Disarm after touchdown. No mission pause button or guaranteed airborne resume is provided.
6. Clear mission while disarmed when no upload is active, or upload a replacement from QGC. Completing a mission does not automatically disarm the vehicle; observe mode/armed state and land when required.

Mission states distinguish receiving, accepted/not running, AUTO active, stopped/not in AUTO, completed and blocked. Route progress starts at zero before execution and then follows the current item through the received list, including its QGC home-marker convention and non-spatial speed commands. It is not a count of fully executed MAVLink command semantics. Current and next item labels show the selected targets separately.

Some accepted items have partial behavior. LOITER_TIME currently uses arrival without a timed dwell. `autocontinue=false` does not implement waypoint pausing. Ops names these limitations; SIM uses hollow nodes for affected navigation items. Unknown commands and unsupported coordinate frames are rejected. See the [MAVLink contract](../internals/mavlink-contract.md).

New received missions retain their source across process recreation but restore as loaded, not as an executing flight. Older saved missions may be a demo or a real upload; their source cannot be inferred. Their items are preserved, while route display and local Start AUTO require re-upload from QGC. The deterministic demo remains an internal fixture only.

While disarmed under GCS Mission authority, **Take local control** is a confirmed return to manual STABILIZE through the existing engine mode command. Clearing route data does not silently change control authority; the action remains available even after the mission row disappears.
