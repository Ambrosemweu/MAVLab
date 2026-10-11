# SIM initial inspection camera — working-tree verification

Date: 2026-10-11, Africa/Nairobi. Tester: Codex. Baseline commit `7948d64403fcb0768b41d9b761e78885f930f3f6` plus these uncommitted changes on `feature/mavlab-uiux-refinements`; not a release. QGC was not used to qualify this change.

## Change and cause

The requested reference is `/home/ambrose/Downloads/drf.jpeg`: elevated front three-quarter framing with the forward camera housing toward the lower left. SIM now snapshots an orbit home at 35 degrees azimuth, 28 degrees elevation and 1.05 scene units distance, relative to the vehicle's heading and rendered body position when the renderer opens. Telemetry does not continuously reset that camera.

Initial phone checking found the model below the new camera target. Source review identified SceneView 4.16.10's default automatic parent centering, which canceled MAVLab's supplied model placement. Disabling `autoCenterContent` and `autoFitContent` makes the explicit camera and engine-derived model coordinates agree. A provisional per-frame pose workaround was removed; `DroneModelController`, propeller animation and engine state remain unchanged. See [ADR-0011](../../../internals/decisions/0011-sim-inspection-camera.md) and [SIM guidance](../../../user/sim.md).

## Identity and passing evidence

Device: Samsung SM_G970U (`R58M83HFYYY`), Android 12, 1080 × 2280, 480 dpi. Saved full-screen enabled. App version 1.5.3/code 18, installed 2026-10-11 at 15:32:12 Africa/Nairobi. Tested APK SHA-256: `0b2631df0869000fb9e57a2a3e4c30583b0334831c59b6e76db020be992b1d5d`.

- [Final host gate](evidence/gradle-gate.txt): `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --max-workers=2 --console=plain`, successful in 14 seconds with incremental caches. Unit XML totals: 248 tests, zero failures/errors/skips. [Five camera geometry tests](evidence/camera-unit-tests.xml) cover angle/distance, positive yaw, translated reference heading, wrapped headings and invalid-heading fallback. [Lint](evidence/lint.txt): zero errors, 34 existing warnings.
- [Final phone regression suite](evidence/navigation-phone-tests.txt): 17/17 navigation/launch tests passed in 15.461 seconds, with no simultaneous capture or touch commands during that run. These do not constitute native rendering qualification.
- Actual app: [baseline](evidence/camera-before.png), [new initial view](evidence/camera-after-home.png), [matching UI state](evidence/camera-after-home.xml). The top, front housing and side are readable in the requested general orientation. The user confirmed: “nice, the drone is now well centered”. This is visual acceptance of centering, not a pixel-exact recreation of the reference.
- A camera drag `(750,1100) → (390,1100)`, 700 ms, produced a different [orbit view](evidence/camera-orbit.png); [HUD state](evidence/camera-orbit.xml) remained disarmed, Idle, zero throttle and zero RPM. Cockpit → SIM restored the [initial framing](evidence/camera-reentry.png) and [state](evidence/camera-reentry.xml). No arm, mission, recording, failure or runtime-stop action was invoked.
- After the final regression suite, reopened the actual app and left it on SIM at the default three-quarter angle: [ready screen](evidence/camera-ready.png), [UI state](evidence/camera-ready.xml), disarmed and zero RPM.

Installing/relaunching resets the nonpersisted simulation runtime; baseline and updated captures are not a continuous-flight comparison. The emulator remained stopped. No commit, push, tag or release was made for this change.

## Failed harness attempts and limits

A standalone SceneView instrumentation test was attempted to automate translated/tilted native model checks. Model loading aborted in Filament with “This thread has not been adopted” on a test dispatcher: [first run](evidence/phone-tests-initial-harness.txt), [native trace](evidence/test-harness-thread-crash.txt). Explicit main-dispatcher/manual-clock test configuration did not resolve it: [retry](evidence/phone-tests-harness-retry.txt), [trace](evidence/test-harness-thread-crash-retry.txt). That failing experimental test was removed; the final suite above is the established 17-test suite, not an 18-test PASS. Normal app visual checks succeeded; no production loading/threading changes were introduced to work around the harness.

An initial host test also loaded SceneView's JVM-21 vector dependency into the JVM-17 unit runner. Camera mathematics now uses a plain internal point type and converts to renderer vectors only at the scene boundary; no Java target change was needed.

Automated native translated/tilted rendering, physical pinch/pan acceptance, armed-flight/background/AUTO behavior, and broader device performance are not newly qualified here. Camera geometry is unit-tested; actual-app starting orientation, orbit and re-entry are phone-checked.

## Impact matrix

- Product surfaces: SIM 3D vehicle initial camera/placement only. Cockpit, Control, Ops, contextual route panel and onboarding unchanged.
- Control paths: local/manual, Phone Sensors, GCS direct and GCS mission eligibility unchanged. Camera gestures issue no flight commands.
- Runtime: existing deferred start and fully-offscreen renderer disposal retained. Camera home is remembered only for the renderer lifetime; no service/background/process-retention changes.
- Simulation: physics, autopilot, failures, battery, mission progression and state publication unchanged. Disabling cosmetic parent centering preserves the existing state-derived model transform.
- Protocol: discovery, peers, telemetry, commands, parameters, mission protocol, identity and capabilities not applicable to this presentation-only change.
- Reverse states: orbit and SIM leave/re-entry checked; arm/disarm, takeoff/land, start/stop, connect/disconnect, inject/reset and recording unchanged.
- Evidence: focused geometry tests, host gate/lint, actual-app captures/user confirmation and established phone navigation tests. Native automation limitation recorded above.
- Documentation: SIM user guide and ADR-0011 added/reviewed; no persisted format or new glossary term.
