# MAVLab Functional Drone Model Creation Plan

> **For Hermes:** Use `blender-automation` and `mavlab-functional-drone-model` before executing this plan. Use BlenderMCP for live Blender work when available. This plan is for creating the MAVLab drone model from this session, not for delegating to a separate external model-authoring agent.

**Goal:** Create a custom MAVLab functional drone model for the Android GCS digital twin simulator, with clean named components that can be controlled from real-time simulation state.

**Architecture:** Build a mobile-friendly GLB from a Blender source file. The model must expose stable runtime nodes for propellers, motors, battery, flight controller, GPS, payload, LEDs, and optional camera/gimbal so the Android SceneView layer can animate and visualize drone state. The first version prioritizes controllability, clean hierarchy, correct pivots, and Android compatibility over cinematic detail.

**Tech Stack:** Blender 5.1.2, BlenderMCP, Python/bpy, glTF/GLB export, Android SceneView/Filament runtime target, MAVLab Android asset pipeline.

---

## 1. Product Direction

MAVLab is pivoting from a lesson-tab educational app into a GCS-connected drone digital twin simulator.

The model should support this new identity:

- QGroundControl creates/uploads a mission.
- MAVLab accepts the mission through MAVLink.
- MAVLab simulates the flight.
- The 3D drone reacts to live state:
  - attitude;
  - altitude/position;
  - motor RPM;
  - propeller direction;
  - battery state;
  - GPS/compass/flight-controller health;
  - payload mass/state;
  - failures such as motor failure, GPS loss, compass interference, low battery, wind, and payload overload.

The model should feel like a serious Ascend/MAVLab simulation asset, not a generic toy quadcopter.

---

## 2. Scope

### 2.1 In Scope for v1

Create a custom Blender model with:

- quadcopter X-frame;
- clean `DroneRoot` hierarchy;
- four separately named motor nodes;
- four separately named propeller nodes;
- correct propeller pivots at motor shafts;
- translucent prop blur discs;
- visible battery module;
- visible flight-controller board;
- GPS module;
- compass/antenna representation;
- payload mount and payload box;
- basic landing gear;
- front orientation LEDs or nose marker;
- named materials for runtime state changes;
- exported `.blend` and `.glb` files;
- validation report for node names and file size.

### 2.2 Out of Scope for v1

Do not overbuild these yet:

- perfect cinematic production-quality mesh;
- full photorealistic carbon-fiber texture pipeline;
- complex rigging;
- physically simulated cables;
- real manufacturer-grade CAD precision;
- detailed internal electronics;
- payload-drop mechanism animation unless quick and clean;
- Android code changes;
- QGroundControl mission upload implementation.

The v1 model is a functional digital-twin MVP. Polish comes after the app proves it can control the model.

---

## 3. Required File Paths

Create/save source file:

```text
/home/ambrose/Downloads/Ascend/Drone SIM/assets/blender/mavlab_drone_v1.blend
```

Export runtime model:

```text
/home/ambrose/Downloads/Ascend/Drone SIM/assets/models/mavlab_drone_v1.glb
```

Optional screenshot/reference outputs:

```text
/home/ambrose/Downloads/Ascend/Drone SIM/assets/renders/mavlab_drone_v1_front.png
/home/ambrose/Downloads/Ascend/Drone SIM/assets/renders/mavlab_drone_v1_top.png
/home/ambrose/Downloads/Ascend/Drone SIM/assets/renders/mavlab_drone_v1_iso.png
```

Do not overwrite this existing Android model yet:

```text
/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/assets/models/drone.glb
```

Only copy into Android assets after validation, using a new file first:

```text
/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android/app/src/main/assets/models/mavlab_drone_v1.glb
```

---

## 4. Runtime Node Contract

The Android app will treat node names as a contract. Names must be exact and stable.

### 4.1 Minimum Required Nodes

```text
DroneRoot
Body
Motor_FL
Motor_FR
Motor_RL
Motor_RR
Prop_FL
Prop_FR
Prop_RL
Prop_RR
Battery
FlightController
GPSModule
PayloadMount
```

### 4.2 Full Recommended v1 Hierarchy

```text
DroneRoot
  Body
    Body_TopPlate
    Body_BottomPlate
    Arm_FL
    Arm_FR
    Arm_RL
    Arm_RR
  FlightController
    FC_StatusLED
  Battery
    Battery_Body
    Battery_LED_1
    Battery_LED_2
    Battery_LED_3
    Battery_LED_4
  ESC
    ESC_FL
    ESC_FR
    ESC_RL
    ESC_RR
  Motor_FL
    MotorHousing_FL
    Prop_FL
    PropBlur_FL
  Motor_FR
    MotorHousing_FR
    Prop_FR
    PropBlur_FR
  Motor_RL
    MotorHousing_RL
    Prop_RL
    PropBlur_RL
  Motor_RR
    MotorHousing_RR
    Prop_RR
    PropBlur_RR
  GPSModule
    GPS_StatusLED
  CompassModule
  Antenna
  PayloadMount
    Payload_Box
  CameraGimbal
    Gimbal_Yaw
      Gimbal_Pitch
        Camera_Body
  NavLights
    LED_FL
    LED_FR
    LED_RR
    LED_RL
  LandingGear
    LandingGear_FL
    LandingGear_FR
    LandingGear_RL
    LandingGear_RR
```

### 4.3 Naming Rules

- Use `FL`, `FR`, `RL`, `RR` consistently.
- Avoid Blender-generated names like `Cube.004` for runtime components.
- Do not use spaces in important node names.
- Do not use random imported suffixes.
- Optional helper meshes may have descriptive names, but runtime nodes must remain exact.

---

## 5. Coordinate, Scale, and Orientation Rules

Use a consistent coordinate model:

- `DroneRoot` at world origin `(0, 0, 0)`.
- Model centered on `DroneRoot`.
- Use real-world approximate scale in meters.
- Suggested v1 diagonal motor span: about `0.75m` to `0.95m`.
- Suggested body length: about `0.35m` to `0.50m`.
- Payload box should visibly fit under the frame.

Forward orientation:

- Set the drone nose toward negative Y unless there is a strong reason to use another axis.
- Add front LEDs or red/white nose marker so orientation is obvious in top view.
- Document the forward axis in this plan's completion notes after modeling.

Propeller local axes:

- Each propeller origin must sit at the motor shaft center.
- Each propeller must spin around its own local vertical/shaft axis.
- Test rotation manually in Blender before export.

---

## 6. Visual Design Direction

The drone should feel:

- technical;
- premium;
- functional;
- field-ready;
- Ascend/logistics oriented;
- plausible as a medical/logistics training drone.

Recommended aesthetic:

- matte graphite or dark carbon-like body;
- Ascend red accents;
- readable exposed modules;
- clean arms and motor housings;
- visible payload bay;
- subtle LED/status indicators;
- robust landing gear.

Avoid:

- toy-like proportions;
- cartoon styling;
- excessive bevels/polygons;
- overly glossy sci-fi plastic;
- purely decorative shapes that do not map to a simulated function.

---

## 7. Materials Contract

Create named materials that Android can later switch/tint.

Recommended materials:

```text
MAT_Body_Dark
MAT_Body_Accent_Red
MAT_Frame_Metal
MAT_Motor_Normal
MAT_Motor_Warning
MAT_Motor_Failed
MAT_Prop_Dark
MAT_PropBlur
MAT_Battery_Normal
MAT_Battery_Low
MAT_Battery_Critical
MAT_Electronics_Green
MAT_LED_Green
MAT_LED_Red
MAT_LED_Blue
MAT_LED_Amber
MAT_Payload_White
MAT_Payload_Warning
```

For v1, use procedural/colors rather than large textures. This keeps GLB small and mobile-friendly.

---

## 8. Component Behavior Mapping

This section defines why each component exists.

### 8.1 Body / Frame

Runtime behavior:

- follows simulated roll, pitch, yaw;
- moves in 3D scene based on position/altitude;
- may later shake under motor failure or high wind.

Model requirements:

- clean body object(s);
- obvious front direction;
- arms separated or at least named.

### 8.2 Motors and Propellers

Runtime behavior:

- per-motor RPM controls prop spin speed;
- motor failure stops or slows one prop;
- prop blur opacity/intensity scales with RPM;
- failed motor can switch material to warning/failed color.

Model requirements:

- four separate motor nodes;
- four separate propeller nodes;
- four optional prop blur discs;
- correct prop origins.

Suggested spin direction convention:

```text
FL: CCW
FR: CW
RL: CW
RR: CCW
```

### 8.3 Battery

Runtime behavior:

- battery percentage controls LEDs/material;
- low battery shows amber/red;
- critical battery shows red;
- future voltage sag/high current can be visualized.

Model requirements:

- separate battery module;
- visible battery LEDs or material slots.

### 8.4 Flight Controller

Runtime behavior:

- armed/disarmed state;
- mode/failsafe indicator;
- warning state.

Model requirements:

- visible board/module;
- status LED object/material.

### 8.5 GPS / Compass / Antenna

Runtime behavior:

- GPS healthy/lost;
- compass interference warning;
- telemetry/link activity.

Model requirements:

- `GPSModule` separate;
- `GPS_StatusLED` optional but preferred;
- `CompassModule` separate if included;
- `Antenna` separate.

### 8.6 Payload

Runtime behavior:

- payload mass affects simulation;
- payload visible in 3D twin;
- overload state can switch warning material;
- future payload-drop/latch animation possible.

Model requirements:

- `PayloadMount` separate;
- `Payload_Box` separate;
- visually integrated with drone, not an afterthought;
- conceptual support for up to 4 kg payload.

### 8.7 Camera/Gimbal

Runtime behavior:

- future gimbal pitch/yaw;
- camera stabilized relative to body;
- operator view later.

Model requirements:

- optional in v1, but if included use:

```text
CameraGimbal
  Gimbal_Yaw
    Gimbal_Pitch
      Camera_Body
```

---

## 9. Execution Plan

### Task 1: Prepare Project Asset Directories

**Objective:** Create clean folders for Blender source, exported models, and screenshots.

**Files:**
- Create directory: `/home/ambrose/Downloads/Ascend/Drone SIM/assets/blender/`
- Create directory: `/home/ambrose/Downloads/Ascend/Drone SIM/assets/models/`
- Create directory: `/home/ambrose/Downloads/Ascend/Drone SIM/assets/renders/`

**Steps:**

1. Create directories.
2. Verify they exist.
3. Do not add generated files to Android assets yet.

**Verification:**

```bash
python3 - <<'PY'
from pathlib import Path
root = Path('/home/ambrose/Downloads/Ascend/Drone SIM/assets')
for rel in ['blender', 'models', 'renders']:
    p = root / rel
    print(rel, p.exists(), p)
    assert p.exists()
PY
```

---

### Task 2: Start BlenderMCP Session

**Objective:** Ensure Hermes can control the active Blender scene.

**Files:** None.

**Steps:**

1. Open Blender.
2. Open 3D viewport sidebar with `N`.
3. Open `BlenderMCP` tab.
4. Click `Connect to Claude`.
5. Use BlenderMCP scene-info tool from Hermes.

**Verification:**

Expected result:

- BlenderMCP reports scene info successfully.
- No socket connection error.

If connection fails:

- confirm Blender is open;
- confirm addon is enabled;
- confirm BlenderMCP connection button was clicked;
- confirm port `9876` is not blocked.

---

### Task 3: Clean Scene and Create Root Contract

**Objective:** Create a clean Blender scene with `DroneRoot` and organized collections.

**Files:**
- Modify/Create: `/home/ambrose/Downloads/Ascend/Drone SIM/assets/blender/mavlab_drone_v1.blend`

**Steps:**

1. Delete default cube.
2. Create collection `MAVLab_Drone`.
3. Create empty `DroneRoot` at world origin.
4. Add optional text note documenting forward axis: `Forward = -Y`.
5. Save `.blend`.

**Verification:**

- Scene contains `DroneRoot`.
- `DroneRoot` transform is zeroed.
- File saved to correct path.

---

### Task 4: Create Material Palette

**Objective:** Add all named materials before modeling so objects can use stable runtime materials.

**Files:**
- Modify: `mavlab_drone_v1.blend`

**Steps:**

Create materials:

```text
MAT_Body_Dark
MAT_Body_Accent_Red
MAT_Frame_Metal
MAT_Motor_Normal
MAT_Motor_Warning
MAT_Motor_Failed
MAT_Prop_Dark
MAT_PropBlur
MAT_Battery_Normal
MAT_Battery_Low
MAT_Battery_Critical
MAT_Electronics_Green
MAT_LED_Green
MAT_LED_Red
MAT_LED_Blue
MAT_LED_Amber
MAT_Payload_White
MAT_Payload_Warning
```

**Verification:**

- Materials exist by exact name.
- `MAT_PropBlur` uses transparent/translucent settings if practical.
- LED materials use bright/emissive color if practical.

---

### Task 5: Block Out Frame and Body

**Objective:** Create the main quadcopter silhouette.

**Files:**
- Modify: `mavlab_drone_v1.blend`

**Components:**

```text
Body
Body_TopPlate
Body_BottomPlate
Arm_FL
Arm_FR
Arm_RL
Arm_RR
```

**Steps:**

1. Create top and bottom body plates.
2. Create X-frame arms to four motor positions.
3. Parent body/frame components under `DroneRoot` or an intermediate `Body` node.
4. Apply dark body and metal frame materials.
5. Add red accent strip or front marker.

**Verification:**

- Top view reads clearly as a drone.
- Front orientation is obvious.
- Node names match contract.

---

### Task 6: Add Motor Housings

**Objective:** Create four separate motor nodes with housings at correct arm ends.

**Files:**
- Modify: `mavlab_drone_v1.blend`

**Components:**

```text
Motor_FL
MotorHousing_FL
Motor_FR
MotorHousing_FR
Motor_RL
MotorHousing_RL
Motor_RR
MotorHousing_RR
```

**Steps:**

1. Define four motor positions around the body.
2. Create parent empty/node for each motor.
3. Add cylindrical motor housing geometry.
4. Assign `MAT_Motor_Normal`.
5. Ensure each motor parent is positioned at motor shaft center.

**Verification:**

- All four motor nodes exist.
- Motor spacing is symmetrical.
- Motors are parented cleanly.

---

### Task 7: Add Pivot-Correct Propellers

**Objective:** Create four separate propellers that can spin independently from RPM.

**Files:**
- Modify: `mavlab_drone_v1.blend`

**Components:**

```text
Prop_FL
Prop_FR
Prop_RL
Prop_RR
```

**Steps:**

1. Create a simple two-blade propeller mesh for one motor.
2. Set origin exactly at motor shaft center.
3. Duplicate to other motors.
4. Name each propeller exactly.
5. Parent each propeller to its corresponding motor node.
6. Test local rotation on each prop.

**Verification:**

- Each prop spins on its own shaft when rotated.
- No propeller orbits around world origin.
- Names are exact.

---

### Task 8: Add Prop Blur Discs

**Objective:** Add optional runtime nodes for high-RPM blur visualization.

**Files:**
- Modify: `mavlab_drone_v1.blend`

**Components:**

```text
PropBlur_FL
PropBlur_FR
PropBlur_RL
PropBlur_RR
```

**Steps:**

1. Create thin translucent discs at propeller planes.
2. Assign `MAT_PropBlur`.
3. Parent each blur disc to corresponding motor or prop node.
4. Keep opacity low by default.

**Verification:**

- Blur discs align with propellers.
- Discs do not obscure whole drone excessively.
- Nodes exist by exact name.

---

### Task 9: Add Battery Module

**Objective:** Create a visible separate battery with state-change targets.

**Files:**
- Modify: `mavlab_drone_v1.blend`

**Components:**

```text
Battery
Battery_Body
Battery_LED_1
Battery_LED_2
Battery_LED_3
Battery_LED_4
```

**Steps:**

1. Create battery module on top or underside.
2. Add four small LED objects or material patches.
3. Assign battery materials.
4. Make it visually removable/modular.

**Verification:**

- Battery is separate from body.
- LEDs are separate or material-addressable.
- Battery is visible in top/iso view.

---

### Task 10: Add Flight Controller, ESCs, GPS, Compass, and Antenna

**Objective:** Add readable avionics modules for simulated system health.

**Files:**
- Modify: `mavlab_drone_v1.blend`

**Components:**

```text
FlightController
FC_StatusLED
ESC
ESC_FL
ESC_FR
ESC_RL
ESC_RR
GPSModule
GPS_StatusLED
CompassModule
Antenna
```

**Steps:**

1. Add a small board-like flight controller on the body.
2. Add status LED.
3. Add ESC modules near arms/motors or central board.
4. Add GPS puck on top/mast.
5. Add compass module if separate from GPS.
6. Add antenna.

**Verification:**

- Components are named exactly.
- Avionics are visible but not cluttered.
- Status LEDs/materials are addressable.

---

### Task 11: Add Payload System

**Objective:** Give the drone a logistics/medical identity with a functional payload mount.

**Files:**
- Modify: `mavlab_drone_v1.blend`

**Components:**

```text
PayloadMount
Payload_Box
```

Optional:

```text
Payload_Latch
Payload_DropDoor
Payload_CG_Marker
```

**Steps:**

1. Add payload rails or bracket under body.
2. Add payload box/module.
3. Assign `MAT_Payload_White` or suitable medical/logistics material.
4. Add subtle red accent or label area.
5. Ensure payload does not intersect landing gear or props.

**Verification:**

- Payload looks intentional.
- Payload can plausibly represent up to 4 kg conceptually.
- Payload is separate and named.

---

### Task 12: Add Landing Gear and Optional Camera/Gimbal

**Objective:** Add landing support and optional future camera control nodes.

**Files:**
- Modify: `mavlab_drone_v1.blend`

**Components:**

```text
LandingGear
LandingGear_FL
LandingGear_FR
LandingGear_RL
LandingGear_RR
```

Optional camera:

```text
CameraGimbal
Gimbal_Yaw
Gimbal_Pitch
Camera_Body
```

**Steps:**

1. Add simple landing gear clear of payload.
2. Add camera/gimbal only if it does not complicate the v1 model.
3. Ensure gimbal nodes are separated if included.

**Verification:**

- Drone looks landable.
- Payload has ground clearance.
- Optional gimbal has clean hierarchy.

---

### Task 13: Add Navigation LEDs / Orientation Markers

**Objective:** Make front/back/left/right orientation obvious for debugging.

**Files:**
- Modify: `mavlab_drone_v1.blend`

**Components:**

```text
NavLights
LED_FL
LED_FR
LED_RR
LED_RL
```

**Steps:**

1. Add small LEDs near arm ends.
2. Use color convention such as:
   - front: green/white;
   - rear: red/amber;
   - or Ascend-specific red front accents if preferred.
3. Keep LEDs small and not cartoonish.

**Verification:**

- Orientation visible from top and isometric view.
- LED nodes are named.

---

### Task 14: Save, Screenshot, and Visual Critique

**Objective:** Review proportions before export.

**Files:**
- Modify: `mavlab_drone_v1.blend`
- Create optional screenshots under `assets/renders/`

**Steps:**

1. Save file.
2. Capture front/top/isometric screenshots.
3. Check:
   - Does it look like a serious drone?
   - Are props proportional?
   - Is payload integrated?
   - Is front orientation clear?
   - Are modules visible but not messy?

**Verification:**

- Screenshots exist or viewport inspection completed.
- Obvious issues fixed before export.

---

### Task 15: Export GLB

**Objective:** Export mobile-ready runtime asset.

**Files:**
- Create: `/home/ambrose/Downloads/Ascend/Drone SIM/assets/models/mavlab_drone_v1.glb`

**Export expectations:**

- GLB format.
- Include materials.
- Include named nodes.
- No unnecessary cameras/lights unless intentionally included.
- Apply transforms only if needed to preserve correct app orientation.
- Avoid exporting hidden junk/default cube.

**Verification:**

- GLB file exists.
- File size is reasonable, ideally under 5-10 MB.

---

### Task 16: Validate GLB Node Names

**Objective:** Prove the exported GLB contains required runtime nodes.

**Files:**
- Read: `/home/ambrose/Downloads/Ascend/Drone SIM/assets/models/mavlab_drone_v1.glb`

**Validation command:**

```bash
python3 -m venv /tmp/gltfinspect-venv
/tmp/gltfinspect-venv/bin/pip install pygltflib
/tmp/gltfinspect-venv/bin/python - <<'PY'
from pathlib import Path
from pygltflib import GLTF2
p = Path('/home/ambrose/Downloads/Ascend/Drone SIM/assets/models/mavlab_drone_v1.glb')
g = GLTF2().load(str(p))
names = [n.name for n in g.nodes if n.name]
required = [
    'DroneRoot','Body','Motor_FL','Motor_FR','Motor_RL','Motor_RR',
    'Prop_FL','Prop_FR','Prop_RL','Prop_RR','Battery','FlightController',
    'GPSModule','PayloadMount'
]
missing = [x for x in required if x not in names]
print('file:', p)
print('size_bytes:', p.stat().st_size)
print('node_count:', len(names))
print('missing:', missing)
print('nodes:')
for name in names:
    print(' -', name)
raise SystemExit(1 if missing else 0)
PY
```

**Expected:**

```text
missing: []
```

---

### Task 17: Create Model Handoff Notes

**Objective:** Document what Android integration needs to know.

**Files:**
- Create: `/home/ambrose/Downloads/Ascend/Drone SIM/assets/models/mavlab_drone_v1_handoff.md`

**Include:**

- GLB path;
- `.blend` path;
- forward axis;
- scale assumptions;
- propeller spin axis;
- propeller direction convention;
- node list;
- material list;
- missing/deferred features;
- known limitations;
- suggested Android model-controller adapter methods.

**Verification:**

- Handoff file exists.
- It matches actual exported GLB validation results.

---

## 10. Quality Gates

The model is not ready unless all gates pass.

### 10.1 Functional Gate

- [ ] Four props are separate.
- [ ] Four props have correct pivots.
- [ ] Four motors are separate/named.
- [ ] Battery is separate/named.
- [ ] FlightController is separate/named.
- [ ] GPSModule is separate/named.
- [ ] PayloadMount is separate/named.
- [ ] GLB contains exact required node names.

### 10.2 Visual Gate

- [ ] Drone reads clearly as a serious functional quadcopter.
- [ ] Front orientation is obvious.
- [ ] Payload is integrated and credible.
- [ ] Materials match premium Ascend/MAVLab direction.
- [ ] No obvious mesh intersections from normal camera angles.

### 10.3 Runtime Gate

- [ ] GLB file size acceptable for Android.
- [ ] No reliance on Blender-only modifiers/constraints for runtime behavior.
- [ ] Node names survive export.
- [ ] Model can be loaded by an external GLB viewer or Android asset test.

---

## 11. Definition of Done

v1 is done when these files exist:

```text
/home/ambrose/Downloads/Ascend/Drone SIM/assets/blender/mavlab_drone_v1.blend
/home/ambrose/Downloads/Ascend/Drone SIM/assets/models/mavlab_drone_v1.glb
/home/ambrose/Downloads/Ascend/Drone SIM/assets/models/mavlab_drone_v1_handoff.md
```

And these are true:

- required node validation passes;
- propeller pivots are verified;
- file size is acceptable;
- visual review passes;
- limitations are documented;
- Android app model replacement is not done yet unless explicitly requested.

---

## 12. Immediate Next Step

Before modeling, open Blender and start the BlenderMCP connection:

1. Open Blender.
2. Press `N` in the viewport.
3. Open `BlenderMCP` tab.
4. Click `Connect to Claude`.
5. Ask Hermes to inspect the scene and begin Task 1.

Once the connection is active, start with Task 1 and proceed sequentially. Save after each meaningful stage.
