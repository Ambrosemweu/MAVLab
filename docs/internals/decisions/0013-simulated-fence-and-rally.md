# 0013 — Engine-owned simulated fence and rally behavior

Date: 2026-10-11
Status: implemented in the working tree; host/device evidence is recorded separately, live QGC acceptance is not inferred.

## Context

Newer QGC can upload navigation, fence and rally plans in one flow. Earlier MAVLab rejected the two safety types. Suppressing errors by accepting unused uploads would violate protocol truth. The user selected full simulated flight behavior, not storage-only compatibility.

## Decision

Keep three independent bounded mission stores. Compile and validate safety geometry at atomic commit, persist safety/configuration in a separately versioned snapshot, and enforce through PhysicsSimulationEngine. Safety transfer ownership, retries, typed responses and error behavior are separate from navigation transactions. Advertise fence/rally service only with the implemented engine path.

Use explicit `SAFETY_RECOVERY` authority for an actionable breach so automatic recovery never masquerades as a GCS command or phone steering. Keep the response latched through return/landing, with LAND/DISARM and explicit disable/manual-mode reverse paths. Reuse the authoritative position controller and autopilot; add no new service, Python bridge or external simulation dependency. Armed flight/background retention and opt-in recording policy remain intact.

RTL is staged return-to-eligible-point then local LAND/automatic touchdown disarm. A navigation mission's RTL uses this same path and terminates at landing. A home/rally endpoint must satisfy enabled fence limits; unavailable GPS/no eligible endpoint falls back to landing locally. No terrain, SmartRTL, predictive braking or obstacle-route planner is claimed.

## Consequences

Ops exposes enablement, stored data, breach/response and clear/disable actions. Cockpit and Control name fence recovery and actual return destination. A disabled fence leaves ordinary phone-sensor steering unchanged. Safety changes are disarmed-only except explicit disable while armed. Location changes clear geographic plan data. Root user/contract/glossary documents are canonical; APK install, synthetic Android checks and real QGC upload/flight evidence remain separate acceptance claims.

See [contract](../mavlink-contract.md), [user workflow](../../user/flight-boundaries.md), and [validation results](../../validation/results/).
