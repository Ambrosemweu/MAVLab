# MAVLab Validation and Evidence

A checklist describes what should happen. It does not prove that a particular build passed.

## Validation specification

Defines setup, steps, expected behavior, failure criteria, and evidence to capture.

## Validation result

An immutable record of one execution. Store results under `results/` with descriptive names such as `v1.5.3-2026-09-01-pixel7-qgc-4.4.3.md`.

Every result includes:

- MAVLab version, commit, and artifact checksum;
- device and Android version;
- QGC version/build and connection mode;
- tester and date/timezone;
- automated commands and summarized output;
- each manual scenario result;
- logs, screenshots, or traces;
- deviations and known issues.

## Evidence levels

| Level | Evidence | Suitable claim |
|---|---|---|
| Unit | Focused JVM tests | Logic/encoding/transitions work in tested cases. |
| Build/lint | Gradle lint and assembly | Source compiles and static gates pass. |
| Device | Installed app on named hardware/emulator | Android integration works there. |
| QGC | Named QGC build and recorded workflow | External workflow works there. |
| Release | Published artifact independently installed | Shipped artifact works as recorded. |

## Current evidence warning

Existing files may combine mutable checklists with blanket `PASS` labels while retaining device/build/date placeholders. Treat these as historical draft evidence until converted into identified result records.

The published paper reports a specific evaluated release and test count. Preserve it as a snapshot; later results belong in new records and, when needed, a versioned revision or addendum.