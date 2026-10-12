# v1.6.0 build-only release assets and contributor-sidebar follow-up

Date: 2026-10-12, Africa/Nairobi. Operator: Codex. Maintainer: Ambrose explicitly requested that release attachments contain only app builds and supplied a screenshot showing Claude still listed in GitHub's sidebar.

Application remains MAVLab 1.6.0/code 19. Starting main commit: `627d314acf20dce6b430f6fb40895945e05caf7c`. The v1.6.0 annotated tag and source-identical target `b9590c91be1e9c51373763223290f48d69859999` are not changed by this maintenance. No APK/AAB replacement, signing change, phone/QGC action or new application release was performed.

## Release attachment cleanup — verified

The original twelve-attachment package was copied to a durable local backup before deletion; all eleven original binary checksums passed. Before/after GitHub release metadata was retained alongside the files. Original repository media and supplied recordings/screenshots were not deleted.

Ten explicitly resolved attachments were removed from the official [v1.6.0 release](https://github.com/Labs-Ascend/MAVLab/releases/tag/v1.6.0): four screenshot JPEGs, two poster JPEGs, two demo MP4s, the media ZIP and `SHA256SUMS.txt`.

The API now reports exactly these two uploaded attachments. Their original asset IDs, names, sizes and content digests match the pre-cleanup metadata:

| Build | SHA-256 |
|---|---|
| `MAVLab-v1.6.0.apk` | `11bc0a1d8c3ee25222bd1a40d623392f0d11423e770aad3ea4f43302ce69c05a` |
| `MAVLab-v1.6.0.aab` | `097f19b11ace6996588d518cf84d03e2a7a1273d2d48feb9f6b52851ab95ead0` |

The APK remains debug-signed and the AAB unsigned; previous signing/upgrade/qualification warnings still apply. GitHub's automatic source ZIP/tar archives are not uploaded release assets and are not deleted. Historical publication records describe the original twelve-attachment execution; their removed public asset links do not describe current availability. Current build integrity is identified above, while the full original manifest is retained in the local recovery backup.

## Publishing policy and regression checks

Release automation stages only APK/AAB builds and explicitly lists those two paths for both release creation and existing-release upload. This prevents stale media or checksum files from being attached through a broad directory wildcard. Checksums remain internal CI/validation evidence, not public release attachments. README/gallery media remain available through repository links.

Four focused fixture tests execute the actual workflow shell steps with disposable files and a mocked `gh` command: build-only staging, build-only existing-release upload, build-only new-release creation and failure when a required build is missing. They deliberately introduce a stale video and checksum file to prove those are not uploaded. Both main CI and future tag validation run these tests; fixtures never publish or replace real builds. Existing nine documentation-tool tests and metadata/local-link/whitespace checks are retained.

Local verification: four release-asset tests and nine documentation-tool tests passed; release YAML parsed and all six shell steps passed `bash -n`. The full lint/unit/debug APK/release AAB gate passed in 1m 24s (96 tasks: one executed, 95 up-to-date), reusing valid existing test outputs. This is not a new phone test or a freshly rerun unit suite. Checksum comparison after asset deletion confirmed the two published builds were not replaced.

Application impact rows (surfaces, controls, runtime, simulation, protocol and reverse states) are not changed. This operation changes release presentation, automation/tests and current documentation, not engine behavior or device acceptance.

## Contributor sidebar — not yet verified fixed

Ambrose's `Screenshot from 2026-10-12 07-32-56.png` visibly shows two sidebar entries, Ambrosemweu and Claude. It disproves an assumption that a clean contributor API response alone established the visible sidebar had refreshed.

Read-only checks found no Claude/Anthropic author identity, co-author trailer or session trailer in reachable local history corresponding to published branches/tags. Canonical refs remain on the approved rewritten lineage. Both the contributor API (including anonymous entries) and contributor statistics returned only Ambrosemweu with 90 authored commits.

This discrepancy is consistent with a stale GitHub-derived sidebar view, but the exact internal sidebar cache has not been independently identified. GitHub documents that [repository statistics are cached by default-branch SHA and a default-branch push resets that cache](https://docs.github.com/en/rest/metrics/statistics#about-repository-statistics). The meaningful build-only publishing/policy commit provides that reset; it does not guarantee the separately rendered sidebar refreshes immediately.

No second history rewrite, repository rename, default-branch switch, deletion/recreation, access change or external support message was performed to force the UI. Visible sidebar acceptance remains pending a hard refresh/user check; if it still shows Claude, escalate the documented discrepancy to GitHub Support rather than claim removal is complete or alter unrelated repository settings.
