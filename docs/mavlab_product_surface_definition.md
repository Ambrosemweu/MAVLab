# MAVLab Product Surface Definition

The UI/UX feature branch uses four surfaces in this order: **Cockpit, Control, SIM, Ops**. This describes branch behavior, not a published release. Mission is a contextual state shared by those surfaces, not a fifth destination.

| Surface | Primary job | Mission responsibility |
| --- | --- | --- |
| Cockpit | Live vehicle state, authority and recovery | Concise authority/progress and a useful recovery or Control link |
| Control | Local input and deliberate flight actions | Conditional collapsed mission row; essential progress and valid engine actions when expanded |
| SIM | Digital twin and spatial understanding | Received route, all navigation nodes, active leg, actual position/trail and measured deviation |
| Ops | Technical evidence and recovery | Active incidents/recovery, peers/identity, exact upload evidence, runtime components, failure exercises, recording/review and bounded transition history |

Every view reads `DroneState`, `MissionProgress` and upload status from the shared runtime. The engine owns mission execution; a UI route, ACK or connected peer cannot create flight progress. `CONTROLLER` remains the local control-authority enum even though its destination is now called Control.

No received mission means no route layer. Product navigation has no sample mission or Load route button. Older saved items with unknown source remain stored, but require a new upload before local AUTO start or route display. GCS Mission visibly pauses local inputs; confirmed Take control & land uses the existing engine recovery path.

Core operation stays phone-first and offline-capable. QGC authors missions; MAVLab does not add an onboard mission planner, pause/resume contract, or cloud prerequisite.

See [ADR-0007](internals/decisions/0007-contextual-missions.md), [Control](user/controller.md), [Mission operation](user/mission.md), [SIM](user/sim.md), and [Ops](user/ops.md). The [prior surface proposal](archive/product-surface-definition-before-mission-consolidation.md) is preserved as history.
