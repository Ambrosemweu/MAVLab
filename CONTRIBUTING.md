# Contributing to MAVLab

Thanks for your interest in MAVLab — a phone-first drone digital-twin simulator built by Ascend Labs (Ascend Drone Technologies Ltd., Nairobi).

## Orientation

MAVLab is deliberately a *first learning layer* for drone systems — **not** a SITL replacement. The architecture and the engineering bets behind it are documented in the technical paper:

- **Paper (DOI):** https://doi.org/10.5281/zenodo.21319694
- **In-repo source:** `docs/paper/paper.md`
- **Architecture deep-dive:** `docs/architecture.md`
- **Protocol invariants & guardrails:** `docs/protocol_guardrails.md`

New contributors should read §4 (phone-first architecture) and §6 (protocol-first methodology) of the paper first — they explain *why* the stack is the way it is.

## Building

```bash
cd mavlab-android
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew lintDebug testDebugUnitTest assembleDebug
```

See `docs/setup_guide.md` for the full environment setup. Prebuilt APKs live on the [releases page](https://github.com/Labs-Ascend/MAVLab/releases).

## Reporting issues

- **Bugs** → use the "Bug report" template.
- **Can't get connected on first try?** → use the "First-run problem" template, and check `docs/known_first_run_issues.md` first.

## Code of conduct

Be kind. This is an education tool used by beginners; assume good faith and explain the "why," not just the "what."

## License

Apache License 2.0. By contributing you agree your contributions are licensed under the same.
