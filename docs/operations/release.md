# MAVLab Release Runbook

Audience: Maintainers
Status: Active baseline
Last verified: 2026-09-01

## 1. Establish release truth

- Review Git status and decide which working-tree changes belong.
- Confirm `versionName` and `versionCode` in `mavlab-android/app/build.gradle.kts`.
- Do not ship protocol/runtime changes under notes claiming none occurred.
- Confirm user docs match the actual product surfaces and artifact name.

## 2. Run automated gates

```bash
cd mavlab-android
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest --console=plain
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew lintDebug assembleDebug --console=plain
```

Expected debug artifact:

`mavlab-android/app/build/outputs/apk/debug/MAVlab.apk`

Use repository release/AAB automation and the configured signing environment. Never commit signing secrets.

## 3. Validate changed surfaces

As applicable, cover onboarding; all five tabs; phone control/calibration; background handoff and explicit stop; same-phone and LAN QGC; arm/disarm, takeoff/land, direct commands, mission upload/start/completion; telemetry and reconnect; failures/reset; recording/export; and supported devices.

## 4. Record evidence

Create an immutable record under `docs/validation/results/` with release/tag and commit, artifact checksum, device/Android, QGC build, tester/date, automated results, manual scenarios, evidence, and known exceptions.

## 5. Update release surfaces

Update release notes, README download/version text, changed user docs, MAVLink compatibility, paper addendum/revision where needed, and Play metadata.

## 6. Publish and verify

Tag the reviewed commit, publish through repository automation, download and verify the published artifact, and confirm release links/instructions. Do not force-move a published tag; use a patch release unless an explicit retag decision is made.
