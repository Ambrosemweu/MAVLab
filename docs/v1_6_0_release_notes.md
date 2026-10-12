# MAVLab v1.6.0 Release Notes

Status: v1.6.0 approved for publication with documented signing and qualification deferrals.
Android version code: 19. Release date: 2026-10-12.

Release attachments contain only `MAVLab-v1.6.0.apk` and `MAVLab-v1.6.0.aab`. Demos and screenshots remain in the repository's media gallery, not as release attachments.

## Important installation warning

Stable release signing is deferred by the maintainer to the next release. This version distributes the locally verified **debug-signed APK**, not a production-signed APK. Its certificate differs from the official v1.5.3 APK, so it **cannot update that published APK in place**. Matching local development installations may update in place; do not assume all v1.5.3 installations use the same key.

Do not uninstall or clear data simply to bypass Android's signature check. Export valuable recordings, save QGC plans externally and note your settings first. Removing the previous app erases its local data/settings; this release provides no automatic signing-key/data migration. Only change installations after explicitly accepting that consequence. See [upgrade guidance](https://github.com/Labs-Ascend/MAVLab/blob/v1.6.0/docs/user/upgrading.md).

The AAB is unsigned, not directly installable and not Play-ready. Stable APK/AAB signing, secure key backup and a deliberate upgrade/migration policy are the first priority for the next release.

MAVLab v1.6.0 brings a clearer phone-first interface, continuous guarded navigation, improved digital-twin visibility and engine-owned simulated fence/rally behavior. The core learning loop remains offline on Android; QGroundControl is optional for local control.

## Four focused surfaces

- Cockpit makes state, authority, telemetry and recovery actions easier to read. Combined altitude/attitude charts use explicit metre/degree axes.
- Control keeps deliberate flight actions accessible, including at larger text sizes, and separates Phone Sensors, Custom Input, Direct RPM and external authority.
- SIM starts centered at an elevated three-quarter view. Inspection lighting keeps the drone visible when orbiting. Received routes display planned geometry and actual observed vehicle/trail, not cosmetic motion.
- Ops prioritizes health/recovery, validated GCS status, upload evidence, failures, runtime configuration and recording review.
- Missions are contextual across these views; the separate Mission destination and built-in sample route are removed from primary navigation.

## Navigation and onboarding

- Continuous page dragging/settling with compact labelled navigation; saved optional full-screen display in Ops.
- Passive Cockpit/Ops content accepts horizontal swipes, including the middle/lower screen. Sliders, charts, fields, route pan/zoom and the 3D camera retain their gestures. Navigation does not wrap.
- Dark launch styling avoids the previous bright opening flash; offscreen drawing is clipped to its page.
- Nine-step onboarding uses the current visual style, buttons and swipes. **Start flying** opens Cockpit without arming or recording. Illustrations remain labelled learning examples.

## Control, runtime and recordings

- Foreground armed Phone Sensors input continues through the engine across Cockpit, Control and SIM. Local takeoff retains attitude steering while controlling altitude.
- Native procedural audio stop/restart is serialized so an AudioTrack is not released underneath its render worker.
- Recording is **off by default**, including AUTO. Begin in Ops or explicitly enable recording on arm while disarmed. Stop/save pauses automatic restart for that armed flight; sessions can be reviewed/exported.
- Existing one-minute QGC handoff, connection/flight-aware background retention and background phone-input suspension remain. Stop simulation freezes state; it does not land the vehicle.

## MAVLink and simulated safety plans

- Unsigned MAVLink 2 is emitted for discovery, telemetry, parameters, commands and missions. Supported inbound MAVLink 1/2 frames are CRC-checked; batched datagrams and omitted trailing zeros are handled.
- Targeted ACKs, extended landed state, missing-item retries, upload ownership and bounded duplicate-final-ACK handling improve newer QGC interactions. Invalid/incomplete replacements retain the accepted plan.
- Navigation, fence and rally have independent stores. Safety lists validate supported geographic frames, polygons/circles and rally points; unsupported items are not silently accepted.
- Fence enforcement is explicitly enabled. Actionable breaches create visible **Fence recovery** authority and drive engine RTL/LAND. Land/disarm and deliberate disable/manual-mode reverse paths remain available.
- RTL selects an eligible rally/home point, climbs if needed, returns, lands and disarms. Mission RTL completes at touchdown. No GPS/eligible return falls back to local landing. Ground RTL never launches.
- Safety plans/configuration persist separately. Changing simulation origin clears geographic navigation/fence/rally plans and disables fencing.

## Important limits

This is a simulated vehicle, not official ArduPilot firmware or a real-aircraft safety system. Ground is flat and returns are straight-line, not obstacle/terrain-aware. SmartRTL, predictive fence braking, terrain frames and MAVLink signing are unsupported. LOITER_TIME has arrival but not timed dwell; `autocontinue=false` does not pause at a waypoint. SET_MESSAGE_INTERVAL accepts requests without dynamic rate selection. See the [MAVLink contract](https://github.com/Labs-Ascend/MAVLab/blob/v1.6.0/docs/internals/mavlink-contract.md) and [flight boundaries guide](https://github.com/Labs-Ascend/MAVLab/blob/v1.6.0/docs/user/flight-boundaries.md).

## Upgrade and evidence

Use `MAVLab-v1.6.0.apk` only with the signing warning above understood. An in-place update preserves settings/recordings only when the signing identity matches; older missions with unknown source require re-upload before route display/local AUTO. Save your QGC plan and reconnect after upgrading to refresh capabilities. Upload safety plans while disarmed; uploading never enables fencing.

[Sound-enabled demos and four screenshots](https://github.com/Labs-Ascend/MAVLab/blob/v1.6.0/assets/media/2026-10/README.md) show local control and AUTO/RTL initiation, not a complete fence/rally acceptance flight.

Earlier working-tree evidence includes 307 unit tests and seven isolated Android protocol/flight checks on the fence/rally build, plus separate earlier UI/layout/audio phone suites. Those APKs used version 1.5.3/code 18; they are not the final v1.6.0 artifact. Live QGC nonempty safety-plan transfer/recovery, final candidate device acceptance and verified published artifacts remain separate gates in [release readiness](https://github.com/Labs-Ascend/MAVLab/blob/v1.6.0/docs/operations/v1.6.0-readiness.md). No blanket release PASS is claimed.

Ambrose subsequently confirmed actual fence and rally uploads completed without errors on the previously installed update. This user-reported success is recorded in readiness, not promoted to independently verified candidate download/recovery acceptance.

The [1.6.0 candidate host gate](https://github.com/Labs-Ascend/MAVLab/blob/v1.6.0/docs/validation/results/2026-10-12-v1.6.0-premerge-host.md) then passed with 307 freshly rerun unit tests, successful APK/AAB assembly and lint (zero errors, 34 warnings). Documentation/tooling and isolated packaging/signing-fixture checks passed. The newly versioned APK has not been installed/accepted on the phone. The maintainer authorized publication with this and the recorded broader device/QGC qualification gaps carried forward; publishing is not a new device-test PASS.

Only v1.6.0 has an explicitly authorized manual-publication exception. Tag CI archives its own comparison build but does not replace public assets with a runner-generated debug key. Future tag releases remain guarded by stable signing requirements; no private key or signing secret was created/uploaded for this release.
