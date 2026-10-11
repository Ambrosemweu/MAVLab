# Attribution-only Git history cleanup

Date: 2026-10-12, Africa/Nairobi. Operator: Codex; approved by Ambrose after explaining published-history rewrite, force-push and release-provenance consequences.

Purpose: remove Claude co-author credits from this project's published Git history. This is not a new application release or a claim that Claude had repository access.

## Offline verification

The working tree was clean before the rewrite. A complete verified Git bundle, original local/canonical/mirror refs and release/workflow metadata were backed up outside the checkout. A separate fresh mirror clone was rewritten with pinned `git-filter-repo` 2.47.0.

- Removed **13** `Co-Authored-By: Claude ... <noreply@anthropic.com>` lines and **10** `Claude-Session` lines.
- Verified all **89** original commits against their mapped counterparts using raw commit objects: exactly the intended message lines and parent IDs changed; file-tree objects, author/committer identities and dates, message encoding and all other message content were preserved.
- Verified all three local branch and five annotated-tag trees were identical before/after. Annotated tag names, messages, tagger identities and dates were preserved; only affected object targets/IDs changed. v1.5.0 predates the removed credits and was unchanged.
- No commits were pruned. Rewritten repository object integrity passed `git fsck --full`.
- Original-to-rewritten IDs are recorded in [commit-map.txt](commit-map.txt). Original IDs in earlier immutable validation records remain historical evidence; use this map rather than silently modifying those records.

The existing local-only `feature/mavlab-uiux-upgrade` branch was preserved and mapped, not newly published. Canonical updates use explicit branch/tag targets and force-with-lease against recorded pre-rewrite refs; no branches or releases are deleted. Other clones must reconcile with the rewritten history instead of merging or pushing the old lineage back.

## Release provenance

| Release tag | Original commit | Source-identical rewritten commit |
|---|---|---|
| v1.5.0 | `ae7298526544be86cb76f7c5fcdd38b1cbfc2378` | unchanged |
| v1.5.1 | `d7123e57ccdb94dd9a3e64a075c82c8188db846e` | `98b0e2f4b1982afd53d84bf047f2e06a02c77e1b` |
| v1.5.2 | `7ab86221146122ce343a0f867a1d72b7d13478cb` | `2f19a1268b8cfbe654a300138021b28f6838d431` |
| v1.5.3 | `703b7f90d717ff386ee9a232bbc5a51a38af3ca0` | `f57f053846460f68d575382777d0688258e4cf37` |
| v1.6.0 | `35c87cf2316d212faa538ba44a7ae296b16916c9` | `b9590c91be1e9c51373763223290f48d69859999` |

The release-publishing workflow is temporarily paused for the ref-only update and then restored, preventing old release jobs from rebuilding or replacing published artifacts. No APK/AAB/media re-upload, version bump, signing change, Play upload or new phone/QGC test is part of this operation. Existing signing and device-qualification deferrals still apply. GitHub-managed source archives naturally contain the rewritten Git metadata; independently attached release binaries remain unchanged.

## Publication checks

- Atomic canonical push completed for the two published branches and four affected release tags using exact pre-rewrite ref leases. Local branches/tags were synchronized by an atomic ref transaction without resetting or rewriting working-tree files.
- Android Release workflow restored to `active` after the push. No release rebuild was dispatched for the tag maintenance.
- [Feature mirror](https://github.com/Labs-Ascend/MAVLab/actions/runs/38183740066) and [main mirror](https://github.com/Labs-Ascend/MAVLab/actions/runs/38183740288) passed; canonical and mirror branch/tag refs were checked directly. The [release-note sync](https://github.com/Labs-Ascend/MAVLab/actions/runs/38183740302) passed and retained the existing release body.
- Before/after API snapshots confirmed all **five releases and twenty attached assets** retained their release/asset IDs, names, published dates, public/prerelease status, notes, sizes and content digests. v1.6.0 APK SHA-256 remains `11bc0a1d8c3ee25222bd1a40d623392f0d11423e770aad3ea4f43302ce69c05a`; AAB remains `097f19b11ace6996588d518cf84d03e2a7a1273d2d48feb9f6b52851ab95ead0`.
- Local lint/unit/APK/AAB gate passed in 7 seconds (96 tasks: one executed, 95 up-to-date). This reused valid existing test results; it is not a freshly rerun unit or phone suite. Nine documentation-tool tests and release metadata/local-path/whitespace checks passed. Current application source is byte-identical to the pre-maintenance source.
- Contributor/agent guidance now explicitly forbids adding future Claude co-author/session trailers while preserving genuine human attribution. This is a documented project rule, not a global Claude configuration change or server-side commit hook.

Application impact: Cockpit/Control/SIM/Ops/onboarding, manual/sensor/GCS control, foreground/background/armed/AUTO/stop/recreation, physics/autopilot/failures/battery, MAVLink protocol and all reverse-state behavior are **not changed** by this maintenance. Verification is Git-object/source equivalence plus the existing host build gate, not a new device or flight acceptance pass. Documentation impact is the attribution policy and this explicit provenance mapping.
