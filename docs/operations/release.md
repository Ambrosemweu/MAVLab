# MAVLab Release Runbook

Audience: Maintainers
Status: Active baseline
Last reviewed: 2026-10-12

## 1. Establish release truth

- Review Git status and decide which working-tree changes belong.
- Confirm `versionName` and `versionCode` in `mavlab-android/app/build.gradle.kts`.
- Do not ship protocol/runtime changes under notes claiming none occurred.
- Confirm user docs match the actual product surfaces and artifact name.
- Use [v1.6.0 readiness](v1.6.0-readiness.md) for this candidate. Candidate preparation, development merge, device/QGC acceptance and public release are different gates.

## 2. Run automated gates

```bash
cd mavlab-android
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest --console=plain
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew lintDebug assembleDebug --console=plain
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew bundleRelease --console=plain
```

Expected debug artifact:

`mavlab-android/app/build/outputs/apk/debug/MAVlab.apk`

Use repository release/AAB automation and the configured signing environment. Never commit signing secrets.

Local configuration produces a debug-signed APK and an unsigned release AAB. Verify signing explicitly; do not call the AAB Play-ready or directly installable. Production signing and Play Console review are separate work. Versioned notes marked `Status: Release candidate` or `Status: Draft` block tag publication.

The tag workflow re-signs the debug-variant APK using the approved stable upgrade identity before creating final checksums. Configure `MAVLAB_APK_KEYSTORE_B64`, `MAVLAB_APK_STORE_PASSWORD`, `MAVLAB_APK_KEY_ALIAS`, and `MAVLAB_APK_KEY_PASSWORD` as Actions secrets, plus `MAVLAB_APK_CERT_SHA256` as a repository variable. These names do not imply they are configured. Never print or commit values; uploading private key material requires explicit maintainer authorization. The existing opaque `MAVLAB` secret must not be assumed to hold a keystore.

**v1.6.0-only exception (maintainer approved 2026-10-12):** stable signing is deferred to the next release. Publish the verified local debug APK manually with prominent certificate/data-loss warnings and the unsigned AAB's limits. Tag CI archives comparison assets, skips stable signing and does not auto-publish or overwrite public v1.6.0 assets. All other tags retain the signing guard. This exception does not promise seamless upgrades from the official v1.5.3 APK. See [next-release signing priority](release-signing-next.md).

Verify the official previous APK certificate and choose the same signing identity for an in-place update. The guard rejects missing inputs, malformed fingerprints and mismatched certificates. A new key is a deliberate migration decision, not a fix by uninstalling user data. Local and CI builds may use different debug keys unless made stable; compare certificates, not just app IDs or filenames. The workflow does not sign the AAB or claim Play acceptance.

## 3. Validate changed surfaces

As applicable, cover onboarding; the four destinations (Cockpit, Control, SIM and Ops), including contextual mission actions and the received-route view; phone control/calibration; background handoff and explicit stop; same-phone and LAN QGC; arm/disarm, takeoff/land, direct commands, mission upload/start/completion; telemetry and reconnect; failures/reset; recording/export; and supported devices. Keep build-specific passes and explicit qualification gaps separate; a development merge is not release acceptance.

## 4. Record evidence

Create an immutable record under `docs/validation/results/` with release/tag and commit, artifact checksum, device/Android, QGC build, tester/date, automated results, manual scenarios, evidence, and known exceptions.

## 5. Update release surfaces

Update release notes, README download/version text, changed user docs, MAVLink compatibility, paper addendum/revision where needed, and Play metadata.

Verify the media pack's `SHA256SUMS`, captions, audio and source dates. Release automation currently packages `assets/media/2026-10`; review that selection for each new release rather than treating old footage as fresh acceptance. From the repository root run `python3 scripts/check-release-docs.py` for candidate metadata and local documentation/media links. Finalize candidate status and published download links only after the corresponding gates pass.

## 6. Publish and verify

Tag the reviewed commit, publish through repository automation, download and verify the published artifact, and confirm release links/instructions. Do not force-move a published tag; use a patch release unless an explicit retag decision is made.

Check APK, AAB, MP4s, screenshots, posters, media ZIP and `SHA256SUMS.txt` on the canonical release. Independently verify downloaded checksums. The personal mirror copies Git refs, not GitHub release assets. Preserve historical notes, results and paper claims; create new identified evidence for this artifact.
