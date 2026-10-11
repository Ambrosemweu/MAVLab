# ADR-0006: Controller operational layout and shared input application

Status: Accepted for working-tree implementation
Date: 2026-10-10
Source: Controller requirements in the MAVLab UIUX Upgrade Consolidated Specification, reconciled with the engine and ADR-0005.

## Decision

Controller has a persistent operational header, a scrolling primary input/setup area and a persistent flight-action dock. Landscape uses a compact header and places the dock beside the controls when font scale is at most 1.15 and the content viewport is at least 240 dp tall. Larger text or shorter content viewports retain a full-width bottom action row. Portrait status fields flow at their natural widths instead of forcing narrow equal columns. Authority, input selection, mode, armed state, battery and navigation/link health derive from shared state. A GCS peer is not an authority selector.

Input selection and calibration require disarmed live state and local authority. Direct RPM additionally requires an explicit confirmation and starts at zero. Airborne disarm and external takeover require confirmation. Normal local landing remains a single action. Stale/unavailable state uses the Cockpit freshness thresholds and disables flight commands rather than implying that default values are live.

`ControllerUiState` derives availability, explanations and contextual actions without owning vehicle state. Phone visualization is explicitly an input preview. Sliders show normalized percentages and RPM units. Sensor sensitivity remains the canonical runtime mapping; this redesign does not implement the timed calibration or dual-stick gestures illustrated in design references.

Custom-input application and Direct-RPM override management now belong to AppRuntime instead of Controller composition effects. Setters apply custom PilotInput immediately, and a fresh custom arm uses the same engine manual-mode recovery as local sensor arm. Direct RPM is applied only while armed with Controller authority. Runtime state observation clears its override when external mission/direct authority appears, including through MAVLink. Leaving the mode, disarming or local landing clears the override. Advanced input therefore continues across selected surfaces without a second control owner.

The sensor acceptance invariant and mapping from ADR-0005 are unchanged. In particular, GCS_MISSION blocks sensor input and GCS_DIRECT may be followed by local input under the existing engine contract. This change does not introduce a persistent external-direct lock or a new mission takeover protocol; the confirmed recovery action calls the existing local Land command.

## Documentation review

AppRuntime and navigation changes require explicit review. User guidance describes locks, flight/reverse actions, calibration, input units and external authority limits. The runtime lifecycle contract identifies shared input ownership; service retention, background sensor suspension and process restoration are unchanged. The control-authority and simulation documents describe custom manual-mode recovery and global advanced-input application. MAVLink command parsing, ACK behavior, identity and advertised capabilities are unchanged.

The initial Controller implementation composed only the selected tab. The subsequent guarded pager is documented in [ADR-0009](0009-display-and-navigation.md); slider gestures still cannot switch tabs. Expanded learning, failure and sound tools do not start new simulation or sensor owners.

## Verification

Focused presentation tests cover unavailable sensors/custom fallback, stale/stopped runtime, local armed control, mission/direct authority, connected-peer independence, landing/disarm, critical faults and Direct RPM. Runtime tests exercise custom physical response without Controller composition, Direct RPM motor response/mission suspension, override clearing and the existing canonical phone arm/takeoff path. Build and named-device evidence are recorded separately under docs/validation/results.

## Checkpoint boundary

During this implementation the user supplied an approved Mission Consolidation Decision and revised consolidated specification: four surfaces, Control replacing Controller, and mission information distributed contextually. The user explicitly chose to handle that consolidation next. This checkpoint retains the current Controller name and five-destination shell; it does not claim the new mission architecture is implemented. Its source-of-truth review and deferred final-device evidence are recorded in the [Controller checkpoint result](../../validation/results/controller-upgrade-working-tree-2026-10-10.md).
