# Installing or upgrading to v1.6.0

Applies to v1.6.0, distributed as a debug build with stable signing deferred to the next release.

Use an in-place installation from the same signing identity; do not uninstall or clear app data. Land/disarm and stop/save recording before upgrading. Export recordings you want an independent backup of. Android may refuse an upgrade signed by a different key; do not delete data to work around that without a deliberate backup decision.

The distributed v1.6.0 debug build and official v1.5.3 APK use different certificates. **The v1.6.0 APK cannot update that official v1.5.3 installation in place.** Matching local development builds are a separate lineage and may update in place.

Removing the old app erases local recordings/settings/plans unless backed up externally. Export valuable recordings, save QGC plans and note settings before deciding to change installations; this version has no automatic data migration. Do not uninstall merely to silence a signature error. Only proceed if you have preserved what you need and explicitly accept the data loss. Stable signing and migration policy are the next-release priority. See [release disposition](../operations/v1.6.0-readiness.md).

## What changes

- Navigation becomes **Cockpit · Control · SIM · Ops**. Missions move into a contextual Control row, Cockpit progress, SIM's received-route view and Ops upload evidence.
- There is no sample route, Load route or separate Mission destination. Upload a plan from QGC to show real received geometry.
- Onboarding ends with **Start flying**, not an arm command. Existing completion is retained; replay from Ops → About MAVLab if desired.
- Full screen is optional in Ops → Display. Existing app settings and compact labelled navigation remain available.
- Recording stays opt-in. Existing saved sessions remain; opening the app, uploading a mission or starting AUTO does not implicitly enable automatic capture.

## Saved data and missions

Audio, app, navigation and recording settings are retained by an in-place update. Received mission source remains stored. Older saved missions with unknown source are preserved, but need re-upload before local Start AUTO or route display. Restoring a plan is not restoring an armed/executing flight.

Fence/rally plans and their configuration use separate versioned storage. They start empty/disabled when no snapshot exists. Uploading does not enable fencing. Changing the simulated GPS origin deliberately clears navigation/fence/rally data and disables fencing; land/disarm and save the plan externally before doing this.

## Refresh QGroundControl

Save your QGC plan before restarting/reconnecting it. The updated capability handshake advertises the implemented bounded safety-plan service and uses MAVLink 2. Keep MAVLab disarmed during transfer checks; inspect Ops for navigation evidence and Flight boundaries & rally for separate safety lists.

An upload may still fail for unsupported frames, commands or invalid geometry. Do not repeatedly issue arm/takeoff to diagnose a missing ACK. Check the actual engine state, exact error and version/build first. If a disarmed mission authority remains after clearing navigation, use the confirmed **Take local control** action in Control rather than assuming clear changes authority.

See [mission operation](mission.md), [flight boundaries](flight-boundaries.md), [recording in Ops](ops.md) and [troubleshooting](../known_first_run_issues.md). Build-specific preservation and interoperability evidence is separate from this guidance; [release readiness](../operations/v1.6.0-readiness.md) identifies what remains.
