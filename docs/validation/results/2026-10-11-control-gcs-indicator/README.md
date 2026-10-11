# Control drawing isolation and Ops GCS indicator

Date: 2026-10-11 (Africa/Nairobi). Tester: Codex. Branch: `feature/mavlab-uiux-refinements`; baseline commit `7948d64403fcb0768b41d9b761e78885f930f3f6` plus uncommitted changes, including the preceding SIM camera work. This is working-tree verification, not a release. No commit, push or publication was performed.

Device: Samsung SM_G970U (`R58M83HFYYY`), Android 12, 1080 × 2280, 480 dpi, font scale 0.8, full-screen preference enabled. Version 1.5.3/code 18, installed at 15:49:31 local time. APK SHA-256: `6a74c414b2dcfbdba292e66043868371d8b4777ac01de7442fc9ebd157b6c849`. QGC version was not established; this does not qualify QGC interoperability.

## Root cause and change

The [original Control capture](evidence/control-before.png) shows a thin line near its right edge. The prepared neighbouring SIM page draws its first grid line at negative Canvas coordinates. Only the overall pager viewport was clipped, allowing this offscreen drawing into Control. `ScreenSwipePager` now clips each page wrapper. The grid remains on SIM; page preparation, gestures, motion and lifecycle rules are unchanged.

Ops has a green/red dot and explicit **GCS connected/disconnected** text beside its title. It observes the existing validated heartbeat connection Boolean, distinct-until-changed and lifecycle-aware. No new timer, packet-derived heuristic, control command, protocol identity or connection threshold was added. Green denotes heartbeat presence, not control authority or successful delivery of outbound telemetry. Red is normal for offline use, not a new critical issue.

## Evidence

- The new isolated Compose [clipping regression failed before the fix](evidence/clipping-before-fix.txt): an offscreen red grid appeared inside the active page at pixel x=861. This is an intentional baseline failure, not a passing run.
- The final [phone suite](evidence/phone-tests.txt) passed **19/19 tests** in 15.899 seconds: established navigation/launch coverage, the new clipping regression, and a GCS indicator test checking both rendered dot colours and text through disconnected → connected → disconnected. No simultaneous phone inputs or captures were issued during the suite.
- The [updated actual Control screen](evidence/control-after.png) has no right-side grid line. It remained disarmed with Idle authority; no arm, calibration, mission, recording, failure or runtime-stop action was used.
- The [actual Ops screen](evidence/ops-connected.png) and [UI hierarchy](evidence/ops-connected.xml) show a green **GCS connected** indicator matching **GCS heartbeat validated** in peer diagnostics. It was left on Ops, disarmed and recording stopped. The already-present peer was not reconfigured or identified as a particular GCS version.
- Existing [heartbeat unit tests](evidence/heartbeat-unit-tests.xml) verify three-second validation warmup, heartbeat continuity, GCS heartbeat type and 15-second expiry. The final unit XML totals are 248 tests, zero failures/errors/skips.
- Final [host gate](evidence/gradle-gate.txt): `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --max-workers=2 --console=plain` passed in 4m 46s. [Lint](evidence/lint.txt): zero errors, 34 existing warnings. `git diff --check` passed. APK hash was rechecked after the gate and matches the installed artifact above.

The actual app's disconnected/expiry screen was not forced by turning off networking or stopping the existing peer. Disconnected colour/text are device-tested in isolation; heartbeat expiry is unit-tested. TalkBack operation, large-font/landscape layout, live-QGC interoperability, armed flight and background runtime are not newly device-qualified here. The emulator stayed stopped.

## Impact matrix and documentation review

- **Product surfaces:** Control is visually isolated from SIM overflow; Ops gains the link indicator. The shared page clipping also contains drawings on Cockpit/SIM/Ops. Established navigation regression passes; onboarding and contextual mission actions unchanged.
- **Control paths:** local/manual, Phone Sensors, GCS direct and GCS missions unchanged; no screen-owned input eligibility introduced.
- **Runtime states:** foreground visual checks and preview lifecycle tests passed. Background handoff, armed/AUTO execution, explicit stop and process-retention policy unchanged; no new qualification claimed.
- **Simulation:** physics, autopilot, failures, battery, missions and state publication unchanged.
- **Protocol:** existing heartbeat state read-only; discovery, peers, telemetry, commands, parameters, mission handling, identity and capabilities unchanged. Existing warmup/expiry unit coverage retained.
- **Reverse states:** indicator test covers connection/disconnection presentation. Flight, runtime, failures and recording reverse actions unchanged and not exercised.
- **Evidence:** full unit/build/lint gate, focused device rendering tests, existing navigation/launch suite and actual-app screenshots. Limits above remain explicit.
- **Documentation:** [Ops guide](../../../user/ops.md), [navigation guide](../../../user/navigation-and-display.md), [ADR-0008](../../../internals/decisions/0008-ops-health-and-recording.md) and [ADR-0009](../../../internals/decisions/0009-display-and-navigation.md) reviewed/updated. No new persisted format or glossary term; runtime/MAVLink contracts do not change.
