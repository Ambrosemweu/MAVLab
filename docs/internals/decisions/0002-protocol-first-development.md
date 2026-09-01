# ADR-0002: Protocol-first development

Status: Accepted
Date recorded: 2026-08-31

## Context

The highest-risk early assumption was whether QGroundControl would recognize and meaningfully operate MAVLab as a simulated ArduPilot-like vehicle. Deep UI or physics work would not rescue a simulator that failed at the GCS boundary.

## Decision

Prove protocol compatibility and behavioral effect before treating higher-level polish as complete. Capabilities, acknowledgements, missions, peer routing, identity, and reconnect behavior are explicit contracts with focused tests and real-QGC validation.

## Consequences

- Protocol claims are conservative and testable.
- Accepted no-ops are separate from implemented behavior.
- Cross-boundary changes need protocol and engine evidence.
- QGC acceptance is a release gate for protocol changes.

## Replace when

Reconsider only if MAVLab deliberately stops supporting an external GCS or adopts a different primary integration contract.