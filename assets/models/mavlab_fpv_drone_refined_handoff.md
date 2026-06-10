# MAVLab FPV Drone Refined Model Handoff

## Source Decision

Ambrose selected `/home/ambrose/Documents/drone.glb` as the preferred visual base. The earlier procedural logistics-style MAVLab model is no longer the active direction for this pass.

The provided source model is visually closer to a realistic FPV/cinewhoop drone, but technically it imported as a mostly fused GLB:

- Source GLB nodes: `world`, `geometry_0`
- Source meshes: 1 mesh, `geometry_0`
- Source materials: 0

Because the source was not component-separated, the refinement keeps it as a visual shell named `Body_ImportedFusedVisual` and adds clean runtime-control components as separate nodes.

## Files

- Original source: `/home/ambrose/Documents/drone.glb`
- Refined Blender source: `/home/ambrose/Downloads/Ascend/Drone SIM/assets/blender/mavlab_fpv_drone_refined.blend`
- Refined runtime GLB: `/home/ambrose/Downloads/Ascend/Drone SIM/assets/models/mavlab_fpv_drone_refined.glb`
- Render preview: `/home/ambrose/Downloads/Ascend/Drone SIM/assets/renders/mavlab_fpv_drone_refined_iso.png`

## Validation Summary

- GLB size: 1704448 bytes (1.625 MB)
- Node count: 68
- Missing required nodes: []
- Review cameras/lights exported: no
- Runtime direction: FPV/cinewhoop, not logistics/payload drone

## Runtime Orientation / Scale Contract

- Forward axis: `-Y`, aligned with the FPV camera/front direction.
- Propeller spin axis: local/world `+Z` at each `Motor_*` origin.
- Propeller pivot validation:
  - FL: motor origin equals prop origin, delta 0.000000 m
  - FR: motor origin equals prop origin, delta 0.000000 m
  - RL: motor origin equals prop origin, delta 0.000000 m
  - RR: motor origin equals prop origin, delta 0.000000 m

## Propeller Direction Convention

- `Prop_FL`: CCW
- `Prop_FR`: CW
- `Prop_RL`: CW
- `Prop_RR`: CCW

## Important Runtime Nodes

- `DroneRoot`
- `Body`
- `Body_ImportedFusedVisual`
- `Motor_FL`, `Motor_FR`, `Motor_RL`, `Motor_RR`
- `MotorHousing_FL`, `MotorHousing_FR`, `MotorHousing_RL`, `MotorHousing_RR`
- `Prop_FL`, `Prop_FR`, `Prop_RL`, `Prop_RR`
- `PropBlur_FL`, `PropBlur_FR`, `PropBlur_RL`, `PropBlur_RR`
- `Battery`, `Battery_Body`, `BatteryStrap`, `Battery_LED_1..4`
- `FlightController`, `FC_ESC_Stack`, `FC_Board`, `ESC_Board`, `FC_StatusLED`
- `FPVCamera`, `FPVCamera_Body`, `FPVCamera_Lens`
- `VTX_Antenna`, `Receiver_Antenna`
- `NavLights`, `LED_FL`, `LED_FR`, `LED_RL`, `LED_RR`
- `MotorWire_FL`, `MotorWire_FR`, `MotorWire_RL`, `MotorWire_RR`

## Full Exported Node List

- Battery_Body
- Battery_LED_1
- Battery_LED_2
- Battery_LED_3
- Battery_LED_4
- BatteryStrap
- Battery
- Body_ImportedFusedVisual
- MotorWire_FL
- MotorWire_FR
- MotorWire_RL
- MotorWire_RR
- Body
- CameraMount_Bracket
- CameraMount
- ESC_Board
- FC_Board
- FC_Standoff
- FC_Standoff.001
- FC_Standoff.002
- FC_Standoff.003
- FC_ESC_Stack
- FC_StatusLED
- FlightController
- FPVCamera_Body
- FPVCamera_Lens
- FPVCamera
- MotorHousing_FL
- Prop_FL
- PropBlur_FL
- PropTip_FL_1
- PropTip_FL_2
- PropTip_FL_3
- Motor_FL
- MotorHousing_FR
- Prop_FR
- PropBlur_FR
- PropTip_FR_1
- PropTip_FR_2
- PropTip_FR_3
- Motor_FR
- MotorHousing_RL
- Prop_RL
- PropBlur_RL
- PropTip_RL_1
- PropTip_RL_2
- PropTip_RL_3
- Motor_RL
- MotorHousing_RR
- Prop_RR
- PropBlur_RR
- PropTip_RR_1
- PropTip_RR_2
- PropTip_RR_3
- Motor_RR
- LED_FL
- LED_FR
- LED_RL
- LED_RR
- NavLights
- Receiver_Antenna_Whip_L
- Receiver_Antenna_Whip_R
- Receiver_Antenna
- SourceImportRoot
- VTX_Antenna_Cap
- VTX_Antenna_Whip
- VTX_Antenna
- DroneRoot

## Exported Material List

- MAT_Battery
- MAT_LED_Green
- MAT_BatteryStrap
- MAT_Imported_Body_Carbon
- MAT_Wire
- MAT_Frame_Graphite
- MAT_Electronics_Green
- MAT_Motor_Metal
- MAT_LED_Blue
- MAT_Camera_Black
- MAT_Lens_Glass
- MAT_Prop_Dark
- MAT_PropBlur
- MAT_Prop_Tip_Red
- MAT_LED_Red
- MAT_Antenna

## Behavior Mapping

- Apply drone pose/world transform to `DroneRoot` or `Body`.
- Rotate `Prop_FL`, `Prop_FR`, `Prop_RL`, `Prop_RR` from per-motor RPM.
- Use `PropBlur_*` opacity/intensity to simulate high RPM.
- Use `Motor_*` / `MotorHousing_*` for motor failure state highlighting.
- Use `Battery_LED_1..4` and `Battery_Body` for battery percentage/low battery visualization.
- Use `FC_StatusLED` for armed/failsafe/system state.
- Use `FPVCamera` for front direction and future camera/FPV visualization.
- Use `VTX_Antenna` and `Receiver_Antenna` for telemetry/link state.
- Use `LED_FL/FR/RL/RR` for orientation and debug lights.

## Known Limitations

- The original AI-generated source mesh is fused, so the underlying frame/duct/body mesh is still one visual object.
- The corrected motors, propellers, battery, electronics, camera, antennas, LEDs, and wires are separate runtime nodes added on top of the imported visual shell.
- Some of the source model's old/fused prop geometry may still be visible below the new runtime propeller objects.
- The top battery and electronics are intentionally explicit for controllability/readability, but can be visually refined later for a more natural FPV scale.
- This is now a much better FPV/cinewhoop direction than the previous logistics-drone model, but final polish should include manual mesh cleanup/separation if you want production-quality geometry.

## Suggested Next Blender Polish Tasks

1. Manually hide/delete old fused propeller faces from `Body_ImportedFusedVisual` if they visually conflict with `Prop_*`.
2. Scale and seat `Battery_Body` more naturally onto the source frame.
3. Make the FPV camera mount match the source front bracket better.
4. Add carbon-fiber texture/normal detail to the source body shell.
5. Separate the imported frame/ducts from the fused mesh if needed for crash/failure simulation.
