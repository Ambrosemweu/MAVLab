# First priority for the next release: stable signing

Owner: MAVLab maintainer. Status: Required before the next public tag after v1.6.0.

The maintainer explicitly deferred signing setup for v1.6.0 and authorized that debug release with disclosed upgrade/qualification limits. Its manual-publication exception is restricted to that exact version. Do not extend it to another release without a deliberate decision.

1. Choose the permanent distribution signing identity and determine whether the private key behind official v1.5.3 is available. A certificate/APK is not a private keystore; do not infer the contents of the existing opaque MAVLAB secret.
2. Document how official and locally installed debug builds migrate. Their certificates differ; do not promise in-place updates across them. Preserve recordings/plans externally and make any required data-loss decision explicit.
3. Authorize secure keystore/password management and an independent recoverable backup. Never commit or paste private key/password values into chats, logs or documentation.
4. Configure the named Actions signing secrets and approved certificate fingerprint in the [release runbook](release.md). This requires maintainer authorization, not merely a docs change.
5. Decide the APK/AAB signing/distribution policy. Current local AABs are unsigned; a successful APK build is not Play readiness.
6. Exercise positive signing, missing/wrong-input rejection, downloaded certificate/checksum verification and real in-place upgrade tests on a named device. Verify retained data contents.
7. Publish the next tag only after signing/migration gates and its device/QGC acceptance scope are resolved. Future-tag workflows fail when stable signing inputs are missing or the approved certificate does not match.

This procedure does not establish that keys exist or a release is production-signed. See [v1.6.0 disposition](v1.6.0-readiness.md) and [upgrade guidance](../user/upgrading.md).
