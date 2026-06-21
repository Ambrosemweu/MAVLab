# MAVLab Product Surface Definition

> Purpose: define exactly what every MAVLab tab/surface owns before more implementation. This prevents building blindly and keeps the app coherent after Phase 1 and Phase 2.

**Project root:** `/home/ambrose/Downloads/Ascend/Drone SIM/`

**Android target:** `mavlab-android/`

**Reference guideline:** `docs/mavlab_gcs_digital_twin_guidelines.md`

**Current implementation baseline:** Phase 1 and Phase 2 are implemented in code:

- `Cockpit / Controller / SIM / Systems / Ops` bottom navigation currently exists.
- `Learn` is removed from bottom navigation, but lesson code remains archived in source.
- QGC mission upload support exists through `MissionUploadSession`, `MISSION_COUNT`, `MISSION_ITEM_INT`, legacy `MISSION_ITEM`, `MISSION_CLEAR_ALL`, `MISSION_SET_CURRENT`, `MISSION_ACK`, and mission loading into the simulation engine.
- Unit tests pass with:
  - `GRADLE_USER_HOME="$PWD/.gradle" ./gradlew testDebugUnitTest --console=plain`

**Updated product decision:** remove `Systems` from the main navigation for now. Systems was mainly educational/lab-oriented and its functionality is not defined sharply enough for the current MVP. Keep the underlying failure/system simulation capability in code where useful, but surface only a lightweight subset inside `Controller` and later bring back a stronger `Systems` screen in a future MAVLab version.

---

## 1. Product Frame

MAVLab is not a lesson app, and it is not a thin wrapper around expert-only drone simulation tools.

MAVLab is:

> A user-friendly phone-based drone simulation and digital-twin platform that lets learners understand drone systems before they have to master ROS, Gazebo, ArduPilot/PX4 SITL, Docker, MAVProxy, Linux networking, or professional simulator infrastructure. It can be controlled either by built-in phone/manual inputs or by real GCS software such as QGroundControl, then visualizes the resulting drone state through cockpit telemetry and a functional 3D twin.

The core product job is to flatten the drone-simulation learning curve: one friendly app first, professional toolchains later.

The educational value should come from operation:

- connect GCS
- arm
- take off
- upload mission
- execute mission
- test manual control
- inject a small number of important failure cases
- observe telemetry
- observe the 3D twin reaction
- diagnose what happened

---

## 2. Product Control Model

MAVLab must make one thing very clear:

> Who is currently controlling the simulated drone?

There are two primary control sources.

### 2.1 Built-in Controller control

This is MAVLab’s local/manual control path.

Sources:

- raw phone sensor input
- calibrated phone tilt input
- custom/manual sliders or sticks
- local arm/takeoff/land buttons
- lightweight local failure toggles, if enabled

Flow:

```text
Phone sensors / manual inputs
  -> Controller input mapper
  -> PilotInput / local commands
  -> PhysicsSimulationEngine
  -> DroneState
  -> Cockpit telemetry
  -> SIM model transforms and motor behavior
```

### 2.2 GCS mission control

This is the QGroundControl/MAVLink path.

Sources:

- QGC arm/disarm command
- QGC takeoff/land command
- QGC mode change
- QGC mission upload
- QGC mission start
- future QGC guided commands

Flow:

```text
QGroundControl
  -> MAVLink UDP
  -> MavlinkUdpServer
  -> command / mission parser
  -> MissionEngine / Autopilot
  -> PhysicsSimulationEngine
  -> DroneState
  -> Cockpit telemetry
  -> SIM model transforms and motor behavior
  -> MAVLink telemetry back to QGC
```

### 2.3 Control authority rule

Only one high-level authority should be dominant at a time.

| Authority | Meaning | UI behavior |
| --- | --- | --- |
| `Controller` | Built-in phone/manual controls are driving the drone. | Cockpit says `Control: Controller`. Controller controls are enabled. Mission controls are idle. |
| `GCS Mission` | Uploaded QGC mission/AUTO mode is driving the drone. | Cockpit says `Control: GCS Mission`. Controller manual controls are visually secondary or paused. |
| `GCS Direct` | QGC is commanding mode/arm/takeoff/land but no mission is active. | Cockpit says `Control: GCS Direct`. Controller can be available but should show possible authority conflict. |
| `Idle` | No active authority. | Cockpit says `Control: Idle`. Drone may be disarmed or waiting. |

The app must not leave the user guessing whether the drone is obeying phone inputs or a QGC mission.

---

## 3. Global UX Rule

Every tab must answer one clean user question.

| Tab | Question it answers | Primary user mode |
| --- | --- | --- |
| Cockpit | What is the drone doing right now, and who is controlling it? | Observe / command essentials |
| Controller | How do I manually control or perturb the drone from the phone? | Manual/local control |
| Mission | What mission did QGC/demo load, and how is it progressing? | GCS/autonomous mission operation |
| SIM | How is the physical drone reacting visually to the current simulation state? | Visual digital twin |
| Ops | Is MAVLink/QGC/app infrastructure healthy? | Diagnostics / configuration |

Recommended MVP tabs:

1. Cockpit
2. Controller
3. Mission
4. SIM
5. Ops

Deferred tab:

- Systems

Systems can return in a future version when it has a sharper purpose, for example a dedicated subsystem health bench for motors, battery, GPS, compass, payload, and fault-tree debugging.

---

## 4. Tab Definitions

### 4.1 Cockpit

**One-line definition:** Cockpit is the live flight instrument panel and authority/status surface.

**It owns:**

- Current control authority:
  - `Idle`
  - `Controller`
  - `GCS Direct`
  - `GCS Mission`
- Current flight mode
- Armed/disarmed state
- Altitude AGL
- Ground speed
- Vertical speed
- Heading
- Battery percent and voltage
- GPS fix and satellite count
- Roll/pitch/yaw
- Throttle
- Attitude/altitude charts
- MAVLink/GCS connection indicator
- Current mission summary if mission is active
- Last inbound MAVLink message summary
- Last ACK summary
- Essential command buttons only:
  - Arm/disarm
  - Takeoff
  - Land
  - emergency stop/reset, future

**It should not own:**

- Full phone tilt/manual control tuning
- Failure injection sliders
- Long MAVLink diagnostics
- Full mission waypoint list
- 3D model internals

**Current code:**

- `mavlab-android/app/src/main/java/com/ascend/mavlab/feature/dashboard/DashboardScreen.kt`
- Navigation label is already `Cockpit`, but file/class still says `DashboardScreen`.

**Needed cleanup:**

- Add a clear `Control: Controller / GCS Mission / GCS Direct / Idle` telemetry card.
- Add `GCS: Connected / Listening / Last packet ...` status.
- Rename `DashboardScreen` package/file to `CockpitScreen` later, after the UX is stable.
- Keep Cockpit lightweight and glanceable.

**Acceptance test:**

A user can open MAVLab and answer within 5 seconds:

- Is the drone connected/running?
- Is it armed?
- What mode is it in?
- Is it controlled by Controller/manual input or by QGC mission?
- What attitude/altitude/speed is it at?
- Is QGC talking to it?

---

### 4.2 Controller

**One-line definition:** Controller is the local manual-control surface: raw phone input, custom input, and a small number of non-clunky test perturbations.

**It owns:**

- Input source selector:
  - `Phone sensors`
  - `Custom/manual input`
- Phone sensor source status
- Calibration
- Tilt visualizer
- Manual/custom input controls:
  - throttle
  - roll
  - pitch
  - yaw
- Flight mode selector for local/manual flight modes
- Arm/takeoff/land convenience controls
- Lightweight failure/perturbation controls only if they directly help manual testing:
  - motor failure quick toggle
  - GPS loss quick toggle
  - wind quick preset
  - reset perturbations

**It should not own:**

- A full Systems lab
- Ten sliders that make the controller feel clunky
- QGC mission upload state
- MAVLink network settings
- Deep telemetry charts
- 3D visual debugging

**Controller must affect SIM:**

Every Controller input must route through the actual simulation engine, not only animate the model directly.

Correct path:

```text
Controller input
  -> Simulation engine
  -> DroneState
  -> SIM
```

Wrong path:

```text
Controller input
  -> SIM only
```

The SIM should react to Controller by showing:

- roll/pitch/yaw body attitude
- throttle-driven motor RPM
- propeller speed changes
- altitude/position changes when physics says the drone moves
- failed motor stop/spin-down if a failure is toggled

**Input modes:**

#### Phone sensors

Use the phone as the controller:

- phone roll -> drone roll command
- phone pitch -> drone pitch command
- throttle slider -> collective thrust command
- yaw slider or future gesture -> yaw command

This is good for immersive manual flight.

#### Custom/manual input

Use sliders/sticks without requiring physical phone motion:

- roll slider
- pitch slider
- yaw slider
- throttle slider

This is good for testing and demos because the user can deliberately set a specific attitude command and watch the SIM respond.

**Anti-clutter rule:**

Controller should show only the active input mode by default. Advanced failure/perturbation controls should be collapsed under something like:

```text
Advanced test inputs
```

Do not make Controller look like a laboratory control panel.

**Current code:**

- `mavlab-android/app/src/main/java/com/ascend/mavlab/feature/controller/ControllerScreen.kt`
- Navigation label is already `Controller`, but heading still says `Controller`.

**Needed cleanup:**

- Heading should say `Controller` or `Phone Flight Controls`.
- Add explicit input-source selector: `Phone sensors` vs `Custom input`.
- Add authority warning when `GCS Mission` is active.
- Move only the most important failure toggles from old Systems/Labs into an advanced collapsed section.

**Acceptance test:**

A user can test SIM response without QGC:

1. Open Controller.
2. Select `Custom input`.
3. Arm.
4. Increase throttle.
5. SIM propellers speed up.
6. Change roll/pitch/yaw.
7. SIM attitude changes.
8. Toggle one motor failure.
9. Matching motor/prop visually changes.
10. Cockpit says `Control: Controller`.

---

### 4.3 Mission

**One-line definition:** Mission is where uploaded or demo missions are inspected, started, cleared, and monitored.

**It owns:**

- Current mission source:
  - QGC uploaded
  - demo mission
  - none
- Waypoint list
- Active waypoint
- Mission progress count
- Reached/active/queued status
- Start AUTO
- Clear mission
- Load demo mission
- Re-upload replacement behavior
- Mission protocol status:
  - upload in progress
  - upload accepted
  - upload rejected
  - last sequence requested/received
- GCS mission control authority handoff

**It should not own:**

- Wind/failure/payload sliders
- Phone tilt control
- UDP settings
- General flight telemetry except mission-relevant status

**GCS mission must affect SIM:**

GCS mission input must not be a fake waypoint list. It must drive the same simulation state as manual flight.

Correct path:

```text
QGC mission upload
  -> MissionUploadSession
  -> MissionEngine
  -> Autopilot
  -> PhysicsSimulationEngine
  -> DroneState
  -> SIM
```

SIM must reflect mission execution through:

- body pitch/roll/yaw while navigating
- changing altitude
- changing heading/yaw toward target
- motor RPM changes from maneuvering
- active waypoint/target overlay
- mission path or breadcrumb trail
- waypoint reached events

**Current code:**

Mission UI currently lives inside:

- `mavlab-android/app/src/main/java/com/ascend/mavlab/feature/labs/LabsScreen.kt`

Mission upload protocol code lives in:

- `mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MissionUploadSession.kt`
- `mavlab-android/app/src/main/java/com/ascend/mavlab/core/mavlink/MavlinkUdpServer.kt`

Mission simulation code lives in:

- `mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/autopilot/MissionEngine.kt`
- `mavlab-android/app/src/main/java/com/ascend/mavlab/simulation/mission/Mission.kt`

**Needed cleanup:**

- Extract mission UI from `LabsScreen.kt` into a first-class `feature/mission/MissionScreen.kt`.
- Add a bottom-nav `Mission` tab.
- Remove `Systems` from bottom nav.
- Add mission upload-state diagnostics from the MAVLink layer, not just final waypoint list.
- Set control authority to `GCS Mission` when AUTO mission execution is active.

**Acceptance test:**

With QGC:

1. QGC detects MAVLab.
2. User uploads a 4-waypoint mission.
3. Mission tab shows 4 waypoints.
4. Start AUTO flies the mission.
5. Cockpit says `Control: GCS Mission`.
6. Active waypoint updates.
7. SIM attitude/position/RPM respond to mission flight.
8. QGC receives `MISSION_CURRENT` and `MISSION_ITEM_REACHED`.
9. Re-upload replaces old mission.
10. Clear removes mission.

---

### 4.4 SIM

**One-line definition:** SIM is the visual digital-twin/simulation view of the drone body and components reacting to live simulation state.

**It owns:**

- 3D model display
- Body attitude mapping
- Position/altitude visualization
- Propeller animation
- Future per-motor RPM animation
- Future motor-failure visual stop/spin-down
- Battery visual state
- GPS/compass visual state
- Payload visual state
- Mission target/path overlay
- Wind/failsafe visual overlay
- Digital-twin HUD:
  - control authority
  - mode
  - arm state
  - altitude
  - speed
  - battery
  - GPS
  - motor RPM bars
  - active waypoint
  - active failure warnings

**It should not own:**

- Sliders for changing failures
- Full MAVLink logs
- Mission editing
- Phone controller calibration

**Current code:**

- `mavlab-android/app/src/main/java/com/ascend/mavlab/feature/drone3d/Drone3DScreen.kt`

Current behavior:

- Loads `models/drone.glb`.
- Maps east/north/altitude to 3D position.
- Maps pitch/yaw/roll to 3D rotation.
- Uses `propellers_spin` animation when armed.
- Adds dynamic lighting.

**Needed cleanup:**

- Rename `Drone3DScreen` to `SIMScreen` later.
- Introduce `DroneModelController` or equivalent adapter.
- Stop treating propellers as one global armed animation after motor telemetry exists.
- Add overlay HUD so SIM can be understood without jumping back to Cockpit.

---

## 5. Big Decision: How Should the 3D Drone Move?

Question:

> Should the 3D drone model change its position with the background moving with it, or should the drone remain intact/centered and only show inputs like motor RPM, pitch, yaw, and roll?

Recommended answer:

> Use a hybrid SIM view, but default to a stabilized chase/inspection view where the drone stays near the center and the world/path/background moves or the camera follows it.

### 5.1 Why not let the drone simply fly away from the camera?

If the model moves freely through a fixed scene, it quickly becomes hard to see:

- motor behavior
- propeller speed
- attitude
- component state
- payload state
- subtle failures

That is bad for a digital twin because the user loses the object they are supposed to inspect.

### 5.2 Why not keep the drone completely static?

If the drone never translates at all, mission flight feels fake. The user cannot understand:

- waypoint progress
- ground track
- climb/descent
- drift from wind
- RTL/landing behavior

That is bad for GCS simulation because the mission has no spatial meaning.

### 5.3 Default mode: Chase/inspection SIM

This should be the default.

Behavior:

- Drone remains close to screen center.
- Drone rotates in real time with pitch/roll/yaw.
- Drone rises/falls slightly or camera reframes altitude.
- Propellers show motor RPM.
- Background/grid/horizon/path moves relative to the drone.
- Camera follows the drone smoothly.
- Mission path/waypoint marker moves in the scene so progress is visible.

Mental model:

```text
The drone is flying through the simulated world,
but the camera is following it like a chase camera.
```

This gives both:

- clear component inspection
- real spatial motion

### 5.4 Optional mode: Bench/diagnostic SIM

This can be added later.

Behavior:

- Drone stays fixed in the center.
- No world translation.
- Only attitude, motor RPM, battery, GPS, failure lights, and payload change.

Use case:

- testing Controller custom inputs
- demonstrating motor RPM
- checking failure visuals
- debugging model pivots

Mental model:

```text
The drone is on a virtual test stand.
We are inspecting behavior, not navigating through space.
```

### 5.5 Optional mode: World/map SIM

This can be added later if needed.

Behavior:

- Drone moves across a larger world/map scene.
- Camera can orbit, follow, or stay fixed.
- Mission route is spatially visible.

Use case:

- showing full mission path
- demos where route matters more than component details

### 5.6 MVP decision

For MVP, implement:

```text
SIM default = Chase/inspection view
```

Meaning:

- Keep the drone readable and near center.
- Move the grid/ground/path/camera around it to communicate travel.
- Always show attitude and motor behavior directly on the model.
- Add a small HUD showing actual numeric position/altitude so the user knows it is truly moving in simulation.

---

## 6. Deferred Systems Tab

`Systems` should be removed from main navigation for now.

Reason:

- It currently feels like an educational/lab leftover.
- Its functionality is not clearly defined enough for MVP.
- Some functions are useful, but they should be moved closer to where the user needs them.

### 6.1 What moves out of Systems now

Move or keep only lightweight controls:

| Old Systems function | MVP destination | Notes |
| --- | --- | --- |
| Motor failure toggle | Controller advanced section | Useful for immediate SIM response testing. |
| GPS loss toggle | Controller advanced section or Ops diagnostic later | Keep simple. |
| Wind preset | Controller advanced section | Use presets, not many sliders. |
| Payload mass | Defer or keep hidden/dev-only | Needs better product definition. |
| Battery drain multiplier | Defer | Too lab-like for MVP. |
| Compass offset | Defer | Bring back when Systems is defined. |
| Detailed health table | Future Systems | Not MVP. |

### 6.2 Future Systems concept

Systems can return later as:

> A subsystem health and failure-analysis bench for motors, battery, GPS, compass, payload, wind, and failsafes.

But it should return only when it has:

- clear system cards
- clear health states
- failure cause/effect explanation
- strong SIM integration
- strong Cockpit telemetry integration

---

## 7. Ops

**One-line definition:** Ops is the technical operations and diagnostics panel for MAVLink, QGC, app service, logs, and configuration.

**It owns:**

- MAVLink UDP status
- Local bind port
- QGC destination assumptions
- System ID/component ID
- Last inbound MAVLink message
- Last ACK
- Recent MAVLink log ring buffer, future
- Mission upload protocol trace, future
- QGC troubleshooting
- Release QA checklist
- Runtime service status
- Reconnect/reset tools, future

**It should not own:**

- Normal flying controls
- Mission waypoint operation except diagnostics
- Failure experiment controls except diagnostic visibility
- 3D model controls

**Current code:**

- `mavlab-android/app/src/main/java/com/ascend/mavlab/feature/settings/SettingsScreen.kt`
- Navigation label is already `Ops`, but screen heading still says `Settings`.

**Needed cleanup:**

- Rename `SettingsScreen` to `OpsScreen` later.
- Heading should say `Ops` or `Operations`.
- Add actual settings only after the core flow is stable.
- Replace static text with structured diagnostics where possible.

**Acceptance test:**

When QGC fails to connect or mission upload hangs, Ops tells us:

- whether MAVLab UDP is running
- what port/system ID it is using
- whether any packets are arriving
- last inbound message ID
- last mission upload event
- last ACK sent
- troubleshooting next action

---

## 8. Current Naming Debt

The user-facing nav has pivoted, but internal code names still reflect the old app:

| User tab | Current implementation name | Desired later name |
| --- | --- | --- |
| Cockpit | `DashboardScreen` | `CockpitScreen` |
| Controller | `ControllerScreen` | `ControllerScreen` |
| SIM | `Drone3DScreen` | `SIMScreen` |
| Ops | `SettingsScreen` | `OpsScreen` |
| Mission | currently embedded in `LabsScreen` | `MissionScreen` |
| Systems | `LabsScreen` | remove from MVP nav; maybe future `SystemsScreen` |

Do not do a giant rename while functional work is unstable. Rename only when tests are green and the tab responsibility is clear.

---

## 9. Recommended Next Build Sequence

Since Phase 1 and Phase 2 are already implemented, the next milestone should remove ambiguity.

### Phase 2.5: MVP navigation correction

Goal: remove unclear Systems tab and make the current tabs match the new product model.

Tasks:

1. Remove `Systems` from bottom navigation.
2. Create first-class `Mission` tab from the existing mission UI inside `LabsScreen.kt`.
3. Keep old Systems/Labs code only if needed as source for moved controls; otherwise hide it from main nav.
4. Keep Controller heading clear; use `Controller` with subtitle `Phone Flight Controls`.
5. Change Ops heading from `Settings` to `Operations`.
6. Add Cockpit control-authority card: `Idle / Controller / GCS Direct / GCS Mission`.
7. Run unit tests.

### Phase 2.6: Controller simplification and authority

Goal: make Controller useful without becoming clunky.

Tasks:

1. Add input source selector: `Phone sensors` vs `Custom input`.
2. Show only controls for the selected input source.
3. Add collapsed `Advanced test inputs` section.
4. Move only motor failure, GPS loss, wind preset, and reset into advanced section.
5. Disable or warn on Controller controls when `GCS Mission` authority is active.
6. Ensure every Controller input changes simulation state, not just UI/model state.
7. Run unit tests.

### Phase 3: Control authority model

Goal: make Cockpit and SIM understand who is driving the drone.

Tasks:

1. Add control authority state to simulation/app runtime.
2. Set authority to `Controller` when local manual input is active.
3. Set authority to `GCS Direct` when QGC command input is received without mission execution.
4. Set authority to `GCS Mission` when AUTO mission is active.
5. Show authority in Cockpit.
6. Show authority in SIM HUD.
7. Run tests.

### Phase 4: Motor telemetry and GCS-to-SIM physical response

Goal: make both Controller and QGC mission inputs visibly affect the SIM.

Tasks:

1. Add motor telemetry to `DroneState`.
2. Populate telemetry from post-failure motor speeds in `PhysicsSimulationEngine.tick()`.
3. Add unit tests for disarmed, armed, manual input, mission flight, and failed motor RPM.
4. Show motor RPM in Cockpit or SIM HUD.
5. Use RPM to animate props independently.
6. Verify QGC mission changes pitch/yaw/roll/altitude/RPM in SIM.

### Phase 5: SIM chase/inspection view

Goal: make the SIM readable and spatially meaningful.

Tasks:

1. Keep drone near the center of the view.
2. Make camera or background follow the simulated position.
3. Add simple grid/ground/path visual reference.
4. Add mission target marker.
5. Add SIM HUD.
6. Add optional diagnostic fixed/bench mode later.

---

## 10. Product Decision: MVP Tab Set

Use five tabs for now:

1. Cockpit
2. Controller
3. Mission
4. SIM
5. Ops

Do not include Systems in MVP navigation.

Why:

- Systems is not yet sharply defined.
- Mission is core to the GCS digital twin pivot.
- Controller can absorb only the most important manual/failure test controls.
- Ops handles technical diagnostics.
- SIM handles visual state.

---


## 11. Flight Logging Decision

Yes, MAVLab should log flights.

Flight logging is not just a nice-to-have. It turns MAVLab from a visual simulator into a test, training, replay, and engineering tool.

### 11.1 What counts as a flight session

A flight session should start when one of these happens:

- user arms the drone
- QGC arms the drone
- takeoff command is received
- AUTO mission starts
- user manually starts recording from Ops, future

A flight session should end when one of these happens:

- drone disarms after flight
- mission completes and vehicle lands/disarms
- user manually stops recording
- app/service shuts down safely

### 11.2 What to log

Log at two levels.

#### A. High-rate state samples

Suggested sample rate for MVP: 5-10 Hz.

Fields:

- timestamp
- session id
- control authority: `Controller`, `GCS Direct`, `GCS Mission`, `Idle`
- armed state
- flight mode
- latitude/longitude
- north/east position
- altitude AGL
- ground speed
- vertical speed
- heading
- roll/pitch/yaw
- throttle
- battery percent
- battery voltage
- GPS fix type
- satellite count
- motor RPM 1-4, after motor telemetry exists
- active waypoint index
- active failure/perturbation flags

#### B. Event log

Events should be sparse and human-readable.

Examples:

- app started simulation service
- QGC connected / first packet received
- mission upload started
- mission item received
- mission upload accepted/rejected
- arm/disarm
- takeoff/land
- mode changed
- mission started
- waypoint reached
- control authority changed
- motor failure toggled
- GPS lost/restored
- low battery/failsafe triggered

### 11.3 Where data should be stored

For Android MVP, store flight logs locally in the app's private storage.

Recommended path shape:

```text
context.filesDir/mavlab/flights/{session_id}/
  manifest.json
  telemetry.csv
  events.jsonl
  mission.json
```

Why app-private storage first:

- no storage permission complexity
- safer for user data
- works offline
- easier to manage retention/deletion
- logs survive app restarts but are still app-owned

Later export options:

- share/export ZIP from Ops
- save CSV/JSON to Downloads through Android document picker
- upload to Ascend cloud/backend, future
- export MAVLink `.tlog` or ULog-like format, future

### 11.4 File roles

#### `manifest.json`

One summary file per session.

Contains:

- session id
- start/end time
- duration
- app version
- simulator version/schema version
- vehicle type/config
- control source summary
- mission source: QGC/demo/manual/none
- max altitude
- max speed
- distance estimate
- battery used
- event counts
- abnormal end reason, if any

#### `telemetry.csv`

Machine-friendly time-series telemetry.

Used for:

- charts
- analysis
- debugging physics/autopilot behavior
- comparing Controller vs GCS mission behavior
- detecting oscillations or bad controller tuning

CSV is best for MVP because it is easy to inspect, graph, and import into Python/Excel.

#### `events.jsonl`

One JSON object per line.

Used for:

- replay timeline
- debugging QGC/MAVLink issues
- explaining what happened during a failure
- showing a flight history detail view

#### `mission.json`

Stores the uploaded/demo mission snapshot used for the flight.

Used for:

- mission replay
- checking whether execution matched planned waypoints
- comparing QGC mission upload payload to simulated behavior

### 11.5 What flight logs can be used for

Flight logs unlock several product and engineering features.

#### Replay

A user can replay a previous flight in SIM:

```text
Flight log -> time slider -> DroneState frames -> SIM animation
```

This is powerful for demos, training, debugging, and showing failures after the fact.

#### Debrief

After a mission, MAVLab can show:

- route flown
- altitude profile
- speed profile
- battery used
- waypoint timing
- failures triggered
- control source changes
- warnings/errors

#### QGC debugging

Logs help answer:

- Did QGC actually send the mission?
- Which MAVLink messages arrived?
- Did MAVLab request the right mission item sequence?
- Did mission execution start?
- Did QGC receive waypoint progress?

#### Physics/autopilot tuning

Logs help tune:

- PID controller response
- motor output behavior
- wind response
- payload behavior
- battery drain model
- failsafe thresholds

#### Training and assessment, future

If MAVLab later returns to training features, logs can score:

- safe takeoff/landing
- smooth control
- mission completion
- response to failure
- over-control or unstable flying

#### Dataset generation, future

Simulated logs can become datasets for:

- autonomy experiments
- anomaly detection
- predictive maintenance demos
- Ascend drone R&D analysis

### 11.6 What not to do in MVP

Do not build a heavy cloud logging platform now.

Do not log every physics tick at 100+ Hz into UI-readable files by default; it may create huge logs and hurt Android performance.

Do not store only pretty screenshots/videos. The important asset is structured telemetry and events.

Do not make flight logging depend on QGC. Built-in Controller sessions should log too.

### 11.7 MVP implementation recommendation

Implement a lightweight `FlightRecorder` service/module:

```text
simulation/recording/FlightRecorder.kt
simulation/recording/FlightSession.kt
simulation/recording/FlightEvent.kt
```

Responsibilities:

- open session directory
- write manifest
- append telemetry CSV rows at 5-10 Hz
- append event JSONL records
- save mission snapshot
- close session cleanly
- expose recent sessions to Ops later

Initial UI:

- Cockpit: show `Recording` indicator when active.
- Ops: show last flight session summary and export path later.
- SIM replay UI can wait until after live SIM behavior is strong.


## 12. Definition of Done for “Clearly Defined”

The app surface is clearly defined when:

- Every tab has one purpose.
- Systems is not in MVP navigation.
- Cockpit clearly shows current control authority.
- Controller supports phone sensors and custom/manual input without clutter.
- Controller inputs affect simulation state and therefore affect SIM.
- Mission is first-class and shows QGC/demo mission progress.
- GCS mission inputs affect physics state and therefore affect SIM: motor RPM, pitch, roll, yaw, altitude, heading, and position.
- SIM uses a chase/inspection view by default: drone stays readable while the world/path/camera communicates movement.
- Ops exposes enough diagnostics to debug QGC connection/upload.
- Build, lint, and tests pass after implementation.
