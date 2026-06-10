# MAVLab SIM Altitude Indicator Spec

> Scope: visual/interaction specification only. This is not an implementation patch.

**Target screen:** `SIM`

**Reference image:** `/home/ambrose/Downloads/at.jpg`

**Goal:** Replace the simple bottom-center altitude chip with a more instrument-like altitude/attitude indicator inspired by the reference: circular aviation HUD styling, artificial-horizon language, readable altitude scale, and a centered fixed aircraft/drone reference marker.

---

## 1. Product Intent

The SIM screen should feel less like a generic telemetry overlay and more like a real flight instrument.

The altitude indicator should communicate:

- current altitude above ground level
- whether the drone is climbing, descending, or level
- relationship between drone attitude and horizon
- stable center reference for the pilot/operator
- aviation-grade visual language without becoming too complex for mobile

This indicator is not only a number. It should feel like an aircraft instrument integrated into the SIM HUD.

---

## 2. Placement

### Primary placement

Place the indicator at the middle-bottom of the SIM screen.

```text
┌───────────────────────────────┐
│                               │
│            3D SIM             │
│                               │
│                               │
│                               │
│          [ALTITUDE            │
│           INDICATOR]          │
│                               │
│ N/E + RPM bottom row remains  │
└───────────────────────────────┘
```

### Positioning rules

- Horizontally centered.
- Anchored above the existing bottom telemetry row.
- Must not cover the drone body center during normal chase/inspection view.
- Must not cover the bottom navigation bar.
- Must remain visible in portrait and landscape.

### Recommended Compose alignment

```kotlin
Modifier
    .align(Alignment.BottomCenter)
    .padding(bottom = 56.dp)
```

If the bottom telemetry row becomes crowded, increase bottom padding to `72.dp`.

---

## 3. Overall Shape

The reference is a circular aviation instrument. For MAVLab SIM, use a compact circular or semi-circular instrument.

### MVP shape

Use a circular gauge with a slightly clipped lower section if needed for space.

Recommended size:

- Phone portrait: `150.dp` to `180.dp` diameter
- Tablet/landscape: `190.dp` to `230.dp` diameter
- Minimum: `132.dp`
- Maximum: `240.dp`

### Visual structure

From outside to inside:

1. thin outer glow/ring
2. dark mechanical bezel
3. fine tick ring
4. inner blue/brown artificial horizon disc
5. fixed orange drone reference marker
6. altitude tape/scale and numeric readout

---

## 4. Color Palette

Use the reference image’s aviation colors, adjusted to match MAVLab’s SIM HUD.

### Outer bezel

- outer ring: near-black / graphite
  - `#101418`
- secondary ring: dark grey
  - `#252B30`
- tick marks: muted grey
  - `#5E6870`
- subtle outer highlight/glow:
  - `#E8FFF7` at low alpha, around `0.18`

### Sky / ground horizon

- sky blue:
  - `#6DB7D8`
- sky highlight:
  - `#A7DCF1` at low alpha
- ground brown:
  - `#4B2716`
- ground warm gradient:
  - `#7B4320` at low alpha

### Instrument markings

- primary marks/text:
  - `#F4FFF9`
- secondary marks:
  - `#C8E9DF`
- active accent orange:
  - `#FF8A1C`
- warning accent, if altitude is unsafe:
  - `#FF4D3D`

### Background transparency

The instrument should not be fully opaque.

- bezel opacity: `0.82` to `0.94`
- horizon disc opacity: `0.72` to `0.88`
- glass/glare overlay: optional, low alpha `0.08` to `0.14`

---

## 5. Typography

Use a cockpit/HUD style.

### Font

Preferred:

- `FontFamily.Monospace`

Fallback:

- default Material typography with `FontWeight.SemiBold`

### Text hierarchy

1. Main altitude number
   - large
   - bold
   - center readable
   - example: `24.8 m`

2. Label
   - small uppercase
   - example: `ALT AGL`

3. Vertical speed
   - smaller
   - signed number
   - example: `VS +0.42 m/s`

4. Scale labels
   - small white numbers around/toward the inner horizon scale
   - examples: `10`, `20`, `30`

---

## 6. Instrument Components

### 6.1 Outer bezel

Draw a circular bezel around the indicator.

Required details:

- dark circular ring
- fine radial ticks around the edge
- slightly thicker ticks every 10 degrees or equivalent interval
- small orange triangular markers at cardinal or major positions

This gives the indicator the mechanical cockpit feel seen in the reference.

### 6.2 Artificial horizon disc

Inside the gauge, draw a horizon background:

- top half: sky blue
- bottom half: brown ground
- horizontal white horizon line between them

The horizon should respond to drone attitude if practical:

- roll rotates the horizon disc opposite the drone roll
- pitch shifts the horizon line vertically

For the first implementation, it is acceptable for the horizon to be mostly decorative, but it should use real `DroneState.rollRadians` and `DroneState.pitchRadians` as soon as possible.

### 6.3 Fixed drone reference marker

The center marker should stay fixed while the horizon moves behind it.

Inspired by the reference image:

- orange central wings/arms line
- small center notch or diamond
- optional orange triangle above center

Recommended MAVLab shape:

```text
       △
───────◆───────
       │
```

Meaning:

- horizontal orange line = drone attitude reference
- center diamond/notch = current reference point
- upper triangle = forward/up reference cue

Color:

- `#FF8A1C`
- optional dark shadow under marker for contrast

### 6.4 Altitude scale

The indicator needs a clear altitude scale, not only a number.

MVP recommendation:

- vertical scale centered inside the gauge
- major labels every `10 m`
- minor ticks every `5 m`
- current altitude remains fixed at center marker
- scale scrolls vertically as altitude changes

Example behavior:

If current altitude is `24 m`, visible labels could be:

```text
40
30
20  ← near center/current reference
10
0
```

But because the indicator is compact, only show nearby values:

- current rounded altitude
- `+10 m`
- `-10 m`
- `+20 m`
- `-20 m`

### 6.5 Main altitude readout

Place the main altitude number at the bottom-center or directly below the center marker.

Recommended text:

```text
ALT AGL
24.8 m
VS +0.42 m/s
```

Alternative, if space is tight:

```text
24.8 m
```

But the label `ALT AGL` should exist somewhere because altitude source matters.

### 6.6 Vertical speed cue

Add a climb/descent cue:

- `VS +0.42 m/s` if climbing
- `VS -0.38 m/s` if descending
- `VS 0.00 m/s` if level

Optional visual:

- upward small arrow when `verticalSpeedMS > +0.1`
- downward small arrow when `< -0.1`
- neutral dash when between `-0.1` and `+0.1`

---

## 7. Data Binding

The indicator must read from `DroneState` only.

Required fields:

- `state.altitudeAglMeters`
- `state.verticalSpeedMS`
- `state.rollRadians`
- `state.pitchRadians`
- optional: `state.armed`
- optional: `state.controlAuthority`
- optional: `state.mode`

Do not compute altitude from the 3D model position.

Correct data flow:

```text
PhysicsSimulationEngine
  -> DroneState.altitudeAglMeters
  -> SIM altitude indicator
```

Wrong data flow:

```text
3D model y position
  -> altitude indicator
```

---

## 8. Behavior

### Normal state

- Indicator is visible at all times on SIM.
- Shows altitude with one decimal place.
- Shows vertical speed with sign and two decimal places.
- Horizon and marker remain readable over the 3D scene.

### Disarmed state

When drone is disarmed:

- altitude still displays
- vertical speed displays
- marker remains visible
- optional label: `DISARM` should remain in the main SIM HUD, not inside this instrument unless space allows

### Climbing

When `verticalSpeedMS > +0.1`:

- show `VS +x.xx m/s`
- optional subtle upward arrow
- optional slightly brighter accent

### Descending

When `verticalSpeedMS < -0.1`:

- show `VS -x.xx m/s`
- optional downward arrow
- if altitude is low, use warning color

### Low altitude warning

If armed and altitude is below safe threshold:

Threshold suggestion:

```text
armed && altitudeAglMeters < 1.5 m && verticalSpeedMS < -0.2 m/s
```

Then:

- border/accent shifts from orange to red
- optional text: `LOW ALT`

Do not make this too noisy during normal takeoff/landing.

---

## 9. Layout Detail

Recommended internal layout:

```text
        outer circular tick ring
    ┌─────────────────────────┐
    │   sky / horizon area    │
    │       ▲ orange cue      │
    │  20 ──────── 20         │
    │  10 ──────── 10         │
    │────────◆──────── horizon│
    │  10 ──────── 10         │
    │  20 ──────── 20         │
    │                         │
    │        ALT AGL          │
    │        24.8 m           │
    │      VS +0.42 m/s       │
    └─────────────────────────┘
```

Since the SIM screen already has other HUD text, do not overfill the instrument. Prioritize:

1. altitude number
2. center marker
3. horizon line
4. vertical speed
5. scale ticks
6. decorative bezel

---

## 10. Implementation Recommendation

Create a dedicated composable rather than keeping it as an inline chip.

Suggested file:

```text
mavlab-android/app/src/main/java/com/ascend/mavlab/feature/drone3d/AltitudeInstrument.kt
```

Suggested composable:

```kotlin
@Composable
fun AltitudeInstrument(
    altitudeMeters: Float,
    verticalSpeedMetersPerSecond: Float,
    rollRadians: Float,
    pitchRadians: Float,
    armed: Boolean,
    modifier: Modifier = Modifier,
)
```

Use `Canvas` for the instrument face.

Use normal Compose `Text` overlays for crisp readouts if Canvas text becomes inconvenient.

### Suggested implementation layers

```text
Box
  Canvas: outer bezel, ticks, horizon, scale lines, marker
  Column/Text overlay: ALT AGL, altitude number, vertical speed
```

---

## 11. Compose Drawing Notes

### Canvas should draw

- circular dark bezel
- tick marks around circumference
- sky/ground clipped circle
- horizon line
- pitch ladder lines
- fixed orange marker
- optional low-alt warning border

### Text overlay should draw

- `ALT AGL`
- `24.8 m`
- `VS +0.42 m/s`

Reason: Compose `Text` gives better font rendering and easier alignment than Canvas text.

---

## 12. Acceptance Criteria

The spec is implemented correctly when:

- SIM has a bottom-center circular or semi-circular altitude instrument.
- It visually resembles the reference aviation instrument, not a plain rectangular chip.
- It shows altitude in meters with one decimal place.
- It shows vertical speed with sign.
- It includes an artificial horizon style: blue sky, brown ground, white horizon line.
- It includes an orange fixed drone reference marker.
- It includes tick marks/scale markings so it feels like an instrument.
- It reads from `DroneState`, not 3D model transforms.
- It remains readable over bright and dark 3D backgrounds.
- It does not hide key SIM content or bottom navigation.
- It works in portrait and landscape.
- Unit/build verification passes after implementation.

---

## 13. Non-Goals

Do not implement a full aircraft PFD yet.

Do not add airspeed, compass tape, attitude bank angle labels, or full autopilot annunciator inside this component.

Do not make it interactive.

Do not use external image assets for the gauge face in MVP. Draw it in Compose so it can react to live simulation state.

Do not connect it directly to MAVLink telemetry. It should use MAVLab’s internal `DroneState`.

---

## 14. Future Enhancements

Later, this altitude instrument can evolve into a full SIM flight display:

- animated altitude tape
- target altitude bug
- mission target altitude marker
- low altitude warning band
- terrain proximity indicator
- bank angle scale
- pitch ladder responsive to pitch
- mode-colored frame: Controller / GCS Direct / GCS Mission
- compact mode for small phones
- expanded mode for tablets
