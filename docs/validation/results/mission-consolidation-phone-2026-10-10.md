# Contextual mission consolidation — 2026-10-10

## Identity and scope

- Implementation commit: `3dde4a2419ef0eccb5f01ee759f37d108fe72479` on `feature/mavlab-uiux-refinements`.
- MAVLab 1.5.3/code 18; debug APK SHA-256: `20169010d2fb5527eab444e55e92b3e0db5f1ca9f9c9ee917a8ad35232674f6e`.
- Device: Samsung SM-G970U, Android 12; tester: Codex; date: 2026-10-10, Africa/Nairobi (UTC+03).
- Scope: [ADR-0007](../../internals/decisions/0007-contextual-missions.md), four destinations and distributed mission presentation. This is a development checkpoint, not a release or completion of every UI/UX phase.
- QGroundControl was not used. The protocol check used an isolated synthetic GCS peer, SYSID 255/COMPID 190, through an ADB shell Toybox netcat bridge to MAVLab's loopback UDP port 14551. No real-aircraft endpoint was involved. No live QGC version or interoperability outcome applies.

## Automated result

```bash
cd mavlab-android
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest lintDebug assembleDebug --max-workers=2 --console=plain
```

**BUILD SUCCESSFUL in 8m 1s; 213 tests, zero failures, errors or skips.** Lint and debug assembly passed. [Gradle log](evidence/mission-gradle-gate-2026-10-10.log).

The gate built the implementation tree before its commit; no executable code changed between that successful gate, phone checks and the implementation commit. This subsequent validation checkpoint adds documentation and captured evidence only.

Focused coverage includes provenance migration, MSL-to-AGL/local coordinate normalization, unsupported command/frame rejection, disarmed AUTO non-execution, mission phase/action guards, replacement-upload honesty, complete route topology, equal metre scaling, off-route vehicle position, finite-leg deviation, and terminal upload sequence retention. Existing phone sensor, custom input, Direct RPM, authority and lifecycle regressions remain in the full suite.

## Final APK phone checks

`adb install -r` returned Success and retained application data. Package metadata reported 1.5.3/code 18 with update time **2026-10-10 15:30:18**. The installed package's SHA-256 matched the artifact identified above. [Install log](evidence/mission-phone-install-2026-10-10.log); [machine-readable results](evidence/mission-phone-results-2026-10-10.json).

The following checks ran on the physical phone with that final APK:

1. Four bottom destinations appeared: Cockpit, Control, SIM, Ops. Without received mission data, Control had no mission row and SIM had no route action.
2. A five-item mission (home marker, takeoff, two waypoints, land) was received through the MAVLink upload handshake and returned mission ACK 0. Control showed accepted/not running, 0% before execution, the source, current/next item and horizontal route distance. [Expanded Control](evidence/mission-control-accepted-2026-10-10.png).
3. A cold process restart restored received items/source while remaining disarmed. Ops showed Upload: Idle, source GCS_UPLOAD, and explicitly stated no upload ACK occurred in that process; the last event was MISSION RESTORED. Restoration did not restart flight.
4. SIM showed all navigation nodes, grouped co-located labels, route/vehicle colors, north-up geometry, metre scale and actual position. Rotation kept SIM's route view selected. Scrolling exposed a full readable map in landscape, instead of the previous collapsed map. [Portrait](evidence/mission-route-portrait-2026-10-10.png); [landscape](evidence/mission-route-landscape-2026-10-10.png).
5. At temporary font scale 1.5, the route, legend and explanatory text remained accessible by scrolling; Control's mission row and flight actions remained visible. [Larger text](evidence/mission-route-large-text-2026-10-10.png). The original font scale was 0.8 and was restored along with the original rotation settings.
6. Confirming Start AUTO armed the simulated vehicle and established GCS Mission authority. Local input was visibly paused. Cockpit showed concise mission awareness and a landing recovery action; its primary actions remained Disarm and SIM. Observed state included altitude 10.9 m AGL and ground speed 2.9 m/s. SIM subsequently showed actual vehicle north position 87.3 m, from a 0 m starting position. [Cockpit](evidence/mission-cockpit-active-2026-10-10.png); [active route](evidence/mission-route-active-2026-10-10.png).
7. A replacement containing command 999 at zero-based sequence 0 returned mission ACK 3. Ops retained the prior five items and showed the exact rejection and last requested sequence 0. Control explicitly showed that the prior AUTO mission was still executing, rather than presenting the rejection as a flight stop. [Ops](evidence/mission-ops-rejected-2026-10-10.png); [Control](evidence/mission-control-rejected-active-2026-10-10.png).
8. Confirmed Take control & land changed authority to Controller and commanded LAND through the engine. The vehicle reached ground and became disarmed. Confirmed Clear mission removed the row/route action; SIM showed 0.0 m AGL, zero motor RPM and free flight. The original mission preferences were then restored byte-for-byte from the pre-check backup, and their original empty mission state was confirmed after relaunch. Recordings and other application preferences were not cleared.

Earlier working APK checks prompted progress wording, grouped labels, opaque route background, preserved sequence diagnostics and rotation/layout fixes. Earlier results do not substitute for the final APK checks listed above.

## Impact matrix

| Row | Scope and verification |
| --- | --- |
| Product surfaces | Four-tab shell; concise Cockpit mission awareness; conditional Control row/actions; SIM received-route view; Ops upload diagnostics; onboarding names updated. Final phone portrait, route landscape and larger-text observations; onboarding content/build tests. |
| Control paths | Local/manual and global Phone Sensors ownership remain canonical. AUTO visibly suspends local input and landing restores local authority. Existing runtime/engine regressions pass; final phone AUTO/recovery observed. Fresh armed physical tilt, custom-input and Direct RPM exercises were not repeated in this record. |
| Runtime states | Foreground AUTO, activity rotation, cold restoration and explicit local recovery checked on the phone. Background GCS handoff/retention remains unchanged and was not requalified on hardware. Saving tab/view selection does not restore an executing flight. |
| Simulation | Engine owns all movement/progress; source/history, altitude normalization and armed AUTO guard are covered by tests. Final phone movement and ground landing observed. Physics equations, failure injection and battery model are otherwise unchanged. |
| Protocol | Completed upload/source and unsupported replacement ACKs observed through Android loopback UDP. Unknown commands/unsupported frames and altitude metadata have focused tests. Identity, capabilities, discovery and external start contracts are unchanged. No LAN round-trip or live QGC qualification is claimed. |
| Reverse states | Start confirmation, takeover/land confirmation, disarmed clear confirmation and post-clear absence checked on the phone. Disarmed external-authority Take local control is covered by presentation tests; stable disarmed AUTO resolves to Idle in the existing engine. Mission pause/resume is not added. Connection, failure-reset and recording flows are unchanged. |
| Evidence | 213 passing JVM tests, lint, assembly, matching installed APK checksum, controlled protocol outcomes and named-device screenshots. No emulator evidence is used for this acceptance record. |
| Documentation | User guides, four-surface definition, glossary, runtime/control/simulation/MAVLink contracts and ADR-0007 reviewed. The previous surface proposal is preserved exactly in the archive. |

## Remaining boundaries

Live QGC upload/download, end-to-end mission completion, background handoff, dense-route pinch/pan interaction and broader accessibility/device qualification need separate evidence. LOITER_TIME dwell and autocontinue=false pausing remain partial behavior named in Ops. Pen.dev design revisions and the remaining UI/UX phases are outside this checkpoint. No tag, release, merge to main or real-aircraft compatibility claim is made.
