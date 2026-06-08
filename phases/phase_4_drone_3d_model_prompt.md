# Phase 4 Drone 3D Model Generation Prompt

Use this prompt to generate or commission the `drone.glb` asset for MAVLab Phase 4.

## Primary Prompt

Create a lightweight, game-ready 3D model of a realistic FPV quadcopter drone for an Android drone simulator app named MAVLab. The model should look like a modern 5-inch freestyle/racing FPV drone: compact carbon-fiber X-frame, low central electronics stack, four exposed arms, four brushless motors, four aggressive two-blade or three-blade propellers, top-mounted LiPo battery with strap, front FPV camera, small antenna, and simple exposed frame details.

The drone must be clean, readable, and suitable for real-time rendering on Android. It should look technical, sporty, and credible rather than toy-like, but it should remain simple enough for students to understand the main physical parts of an FPV quadcopter.

Design the model with these visual requirements:

- FPV X-frame layout viewed from above, with a tight low-profile body.
- Four exposed carbon-fiber arms extending diagonally from the central stack.
- Four compact brushless motor bells at the arm ends.
- Four thin FPV propellers, one above each motor, preferably three-blade props.
- Top-mounted LiPo battery with a visible battery strap.
- Central electronics stack with flight controller/ESC plates.
- Front FPV camera mounted in a small angled camera cage.
- Small rear antenna or VTX antenna, kept low-poly.
- Minimal skids or small landing pads only; avoid tall landing legs.
- Clear front orientation marker: front camera, front standoffs, or front arms should be blue, bright white, or neon green.
- Rear arms should use a different accent, such as dark gray with small red accents.
- Materials should be physically based but simple: matte carbon fiber frame, anodized metallic motor bells, black propellers, rubber battery strap, and colored orientation accents.
- Add small details such as screw heads, stack standoffs, battery lead hint, motor wires, and camera lens only if they do not increase geometry too much.
- No brand logos, no real company marks, no text that requires licensing.

The final asset must be exported as a single glTF Binary file:

```text
drone.glb
```

## Technical Requirements

The model will be loaded in a Jetpack Compose Android app using SceneView and Google Filament.

Export requirements:

- Format: glTF Binary `.glb`
- Coordinate system: Y-up
- Forward direction: +Z
- Origin/pivot: exact center of the drone body, horizontally centered between all four motors
- Scale: 1 unit = 1 meter
- Physical size: approximately 0.22 m to 0.28 m motor-to-motor diagonal, like a 5-inch FPV quad
- Triangle budget: ideally 5,000 to 8,000 triangles, maximum 10,000 triangles
- Texture budget: maximum 1024 x 1024 per texture
- Materials: glTF PBR metallic-roughness
- Mesh count: keep reasonably low, but separate important parts if animation needs them
- File size target: under 5 MB, maximum 10 MB
- Must render well without ray tracing
- Must render well under simple ambient plus directional lighting
- Must not require external texture files if possible; embed textures in the `.glb`

## Animation Requirements

Include one looping propeller animation:

- Animation name: `propellers_spin`
- Duration: 1 second
- Loopable without visible jump
- All four propellers rotate continuously around their local vertical axes
- Propeller rotation should be visually fast enough to imply powered motors
- Keep the drone body still during this animation

If the generator cannot create animation, create the propellers as separate named mesh nodes so the app can rotate them at runtime.

Preferred node names:

```text
DroneRoot
Body
Battery
FlightController
FPVCamera
Antenna
FrontLeftArm
FrontRightArm
RearLeftArm
RearRightArm
FrontLeftMotor
FrontRightMotor
RearLeftMotor
RearRightMotor
FrontLeftPropeller
FrontRightPropeller
RearLeftPropeller
RearRightPropeller
LandingGear
FrontMarker
```

## Orientation And MAVLab Telemetry Mapping

The asset will be transformed by telemetry in Phase 4:

```text
model.rotation.x = pitch
model.rotation.y = yaw
model.rotation.z = roll
model.position.y = altitudeAGL
model.position.x/z = local horizontal position
```

Because of this, the model must follow these orientation rules:

- Drone sits level when rotation is zero.
- Drone faces +Z when yaw is zero.
- Top of drone points +Y.
- Center of mass should appear near the origin.
- Propellers should sit in a flat plane above the arms.
- Battery should sit on top of the central body along +Y.
- FPV camera should point toward +Z.
- Any small skids or landing pads should extend downward along -Y.

## Style Direction

Use a practical educational simulator style:

- Clean enough to read on a phone screen.
- Realistic enough to teach FPV drone anatomy.
- Sporty and modern, but not overly cinematic, damaged, military, sci-fi, or decorative.
- Avoid excessive bevels, tiny wires, heavy grunge, transparent materials, or dense mechanical interiors.
- Use a restrained FPV palette: black carbon frame, metallic motor bells, black or smoky propellers, blue/white/neon green front accents, red rear accents, and a dark LiPo battery with a strap.

## Negative Prompt

Do not create:

- A fixed-wing aircraft
- A helicopter with one main rotor
- A military attack drone
- A drone with weapons
- A futuristic spaceship
- A toy cartoon drone
- A camera-gimbal photography drone
- A large industrial quadcopter with tall landing gear
- A DJI F450 training-frame style quad
- A very high-poly cinematic model
- A model with unreadable micro-details
- A model with brand logos or copyrighted markings
- A model with unsupported shaders, cloth simulation, particle effects, or external dependencies
- A model that faces -Z, is Z-up, or has an off-center pivot

## Acceptance Checklist

Before using the model in MAVLab, verify:

- [ ] File is named `drone.glb`
- [ ] File opens in Blender or a glTF viewer
- [ ] Model is Y-up and faces +Z
- [ ] Origin is centered in the drone body
- [ ] Scale is approximately 0.22 m to 0.28 m across
- [ ] Triangle count is at or below 10,000
- [ ] Textures are embedded or packaged correctly
- [ ] Materials use PBR metallic-roughness
- [ ] Front orientation is visually obvious
- [ ] Propellers are animated or separately named
- [ ] Model remains readable at phone-screen size
- [ ] File can be placed at `mavlab-android/app/src/main/assets/models/drone.glb`

## Short Version

Create a game-ready `.glb` FPV quadcopter drone model for an Android educational simulator. Use a realistic modern 5-inch freestyle/racing FPV X-frame with compact carbon body, four exposed arms, four brushless motors, four three-blade propellers, top-mounted LiPo battery with strap, front FPV camera, small antenna, and clear blue/white/neon front orientation accents with red rear accents. Export as Y-up, +Z forward, 1 unit = 1 meter, centered origin, 0.22 m to 0.28 m size, PBR materials, under 10k triangles, embedded textures, and one looping propeller spin animation named `propellers_spin`. Avoid logos, weapons, sci-fi styling, cartoon styling, camera-gimbal photography drone styling, high-poly geometry, external dependencies, and incorrect orientation.
