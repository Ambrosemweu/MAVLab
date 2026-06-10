# MAVLab Drone v1 Handoff Notes

## Files

- Blender source: `/home/ambrose/Downloads/Ascend/Drone SIM/assets/blender/mavlab_drone_v1.blend`
- Runtime GLB: `/home/ambrose/Downloads/Ascend/Drone SIM/assets/models/mavlab_drone_v1.glb`
- Render references:
  - `/home/ambrose/Downloads/Ascend/Drone SIM/assets/renders/mavlab_drone_v1_iso.png`
  - `/home/ambrose/Downloads/Ascend/Drone SIM/assets/renders/mavlab_drone_v1_top.png`
  - `/home/ambrose/Downloads/Ascend/Drone SIM/assets/renders/mavlab_drone_v1_front.png`

## Validation Summary

- GLB size: 523368 bytes (0.499 MB)
- GLB node count: 96
- Missing required runtime nodes: []
- Export cleanup: no review cameras/lights/text-note nodes in final GLB validation run.
- Android replacement status: not copied over/replacing the existing Android asset yet.

## Runtime Orientation / Scale Contract

- Forward axis: `-Y`
- Root transform: `DroneRoot` centered at world origin.
- Scale: meters; v1 visual motor layout uses motor positions at approximately ±0.38 m on X/Y.
- Propeller spin axis: local/world vertical `+Z` at each motor shaft.
- Propeller pivot validation:
  - FL: prop origin equals motor origin, delta 0.000000 m
  - FR: prop origin equals motor origin, delta 0.000000 m
  - RL: prop origin equals motor origin, delta 0.000000 m
  - RR: prop origin equals motor origin, delta 0.000000 m

## Propeller Direction Convention

- `Prop_FL`: CCW
- `Prop_FR`: CW
- `Prop_RL`: CW
- `Prop_RR`: CCW

The Android controller can apply signed RPM using this convention, or use absolute RPM plus a per-node direction table.

## Required Runtime Nodes Present

- `DroneRoot`
- `Body`
- `Motor_FL`
- `Motor_FR`
- `Motor_RL`
- `Motor_RR`
- `Prop_FL`
- `Prop_FR`
- `Prop_RL`
- `Prop_RR`
- `Battery`
- `FlightController`
- `GPSModule`
- `PayloadMount`

## Full Exported Node List

- Antenna_Base
- Antenna_Whip
- Antenna
- Battery_Body
- Battery_LED_1
- Battery_LED_2
- Battery_LED_3
- Battery_LED_4
- Battery_RedReleaseTab
- Battery
- Arm_FL
- Arm_FL_RedAccent
- Arm_FR
- Arm_FR_RedAccent
- Arm_RL
- Arm_RR
- Body_BottomPlate
- Body_RedNoseMarker
- Body_Spine_Red
- Body_TopPlate
- Cable_FL
- Cable_FR
- Cable_RL
- Cable_RR
- Body
- Camera_Body
- Camera_Lens
- Gimbal_Pitch_Hinge
- Gimbal_Pitch
- Gimbal_Yaw_Ring
- Gimbal_Yaw
- CameraGimbal
- Compass_Body
- Compass_StatusLED
- CompassModule
- ESC_FL
- ESC_FL_Stripe
- ESC_FR
- ESC_FR_Stripe
- ESC_RL
- ESC_RL_Stripe
- ESC_RR
- ESC_RR_Stripe
- ESC
- FC_Chip_Main
- FC_StatusLED
- FlightController_Board
- FlightController
- GPS_Mast
- GPS_Puck
- GPS_StatusLED
- GPSModule
- LandingFoot_FL
- LandingFoot_FR
- LandingFoot_RL
- LandingFoot_RR
- LandingGear_FL
- LandingGear_FR
- LandingGear_RL
- LandingGear_RR
- LandingGear
- MotorHousing_FL
- MotorTopCap_FL
- Prop_FL
- PropBlur_FL
- Motor_FL
- MotorHousing_FR
- MotorTopCap_FR
- Prop_FR
- PropBlur_FR
- Motor_FR
- MotorHousing_RL
- MotorTopCap_RL
- Prop_RL
- PropBlur_RL
- Motor_RL
- MotorHousing_RR
- MotorTopCap_RR
- Prop_RR
- PropBlur_RR
- Motor_RR
- LED_FL
- LED_FR
- LED_RL
- LED_RR
- NavLights
- Payload_Box
- Payload_CG_Marker
- Payload_Crossbar_Front
- Payload_Crossbar_Rear
- Payload_Latch
- Payload_Rail_L
- Payload_Rail_R
- Payload_RedMedicalStripe
- PayloadMount
- DroneRoot

## Exported Material List

- MAT_Frame_Metal
- MAT_Cable_Dark
- MAT_Battery_Normal
- MAT_LED_Green
- MAT_Body_Accent_Red
- MAT_Body_Dark
- MAT_LED_Blue
- MAT_LED_Amber
- MAT_Electronics_Green
- MAT_Motor_Normal
- MAT_Prop_Dark
- MAT_PropBlur
- MAT_LED_Red
- MAT_Payload_White

## Behavior Mapping

- `DroneRoot` / `Body`: apply simulated pose and world movement.
- `Prop_FL`, `Prop_FR`, `Prop_RL`, `Prop_RR`: rotate by per-motor RPM.
- `PropBlur_FL`, `PropBlur_FR`, `PropBlur_RL`, `PropBlur_RR`: adjust opacity/intensity with RPM.
- `Motor_FL`, `Motor_FR`, `Motor_RL`, `Motor_RR`: use material switching/tinting for warnings/failures.
- `Battery` and `Battery_LED_1..4`: show battery percentage and low/critical state.
- `FlightController` and `FC_StatusLED`: armed/failsafe/status visualization.
- `GPSModule` and `GPS_StatusLED`: GPS lock/healthy/lost state.
- `CompassModule`: compass warning/interference state.
- `Antenna`: telemetry/link activity indicator target.
- `PayloadMount` and `Payload_Box`: payload mass/state visualization.
- `CameraGimbal`, `Gimbal_Yaw`, `Gimbal_Pitch`, `Camera_Body`: optional future camera/gimbal control.
- `NavLights` and `LED_FL/FR/RL/RR`: orientation/debug state.

## Suggested Android Adapter API

```kotlin
interface DroneModelController {
    fun setBodyPose(rollRad: Float, pitchRad: Float, yawRad: Float)
    fun setMotorRpm(fl: Float, fr: Float, rl: Float, rr: Float)
    fun setMotorFailed(index: Int, failed: Boolean)
    fun setBatteryPercent(percent: Float)
    fun setGpsHealthy(healthy: Boolean)
    fun setPayloadMassKg(massKg: Float)
}
```

## Known Limitations / v1 Notes

- This is a functional blockout/MVP asset, not a final cinematic production model.
- Materials are procedural/simple colors for a small mobile-friendly GLB.
- Prop blur discs are intentionally translucent runtime nodes; Android should control their opacity based on RPM.
- No payload-drop animation is included yet, though `Payload_Latch` and `Payload_CG_Marker` are present.
- No Blender-only constraints are required for runtime behavior.
- The current GLB is validated and suitable for initial SceneView/node-control tests.
