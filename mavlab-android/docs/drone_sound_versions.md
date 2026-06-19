# MAVLab Drone Sound Versions Roadmap

This document tracks the planned evolution of MAVLab’s drone sound system. It exists separately from the V2 implementation spec so future work can reference the larger direction without bloating the immediate implementation.

Current target:

```text
Version 2: Controller-tunable per-motor adaptive sound
```

Detailed V2 spec:

```text
docs/drone_sound_v2_spec.md
```

---

## Version philosophy

MAVLab sound should evolve from a simple demo feature into a digital-twin training signal.

The system should move through these levels:

1. Make the drone feel alive.
2. Make motor RPM and throttle understandable by ear.
3. Make failures and payload/load changes audible.
4. Make the sound physically explainable.
5. Eventually connect sound, telemetry, and AI debrief.

Architecture should stay replaceable:

```text
DroneState + FailureState
  -> DroneSoundModel
  -> DroneSoundController
  -> Android audio backend
  -> Controller/SIM/Ops user surfaces
```

Do not bind core audio behavior directly to a Compose screen. `AppRuntime` should own lifecycle.

---

## Version 1 — Basic Drone Sound

### Status

Reference only. This was the simplest possible first step. We are skipping directly to Version 2 for implementation.

### Goal

Add basic drone ambience that responds to arm/disarm and average motor RPM.

### User value

- Makes MAVLab feel alive.
- Gives immediate audio feedback when the drone is armed.
- Lets users hear throttle/RPM changes at a basic level.

### Core behavior

- One looped drone sample.
- Sound starts when armed.
- Sound fades out when disarmed.
- Playback rate follows average RPM.
- Volume follows throttle/RPM.
- Basic mute/volume control.

### Inputs

- `DroneState.armed`
- `DroneState.throttlePercent`
- average of `DroneState.motors[].rpm`

### Audio backend

Recommended:

- `SoundPool`

### UI surface

Could live in `Ops` or `Controller`, but if implemented today it should live in `Controller` for immediate testing.

### Assets

Minimum:

```text
app/src/main/res/raw/drone_motor_loop.wav
```

### Acceptance criteria

- Motor sound starts/stops with arm/disarm.
- Pitch rises with average RPM.
- Volume slider works.
- No crash on app background/foreground.

### Limitations

- No per-motor differences.
- No failure sound.
- No roughness model.
- Easy to sound like a static loop.

---

## Version 2 — Controller-Tunable Per-Motor Adaptive Sound

### Status

Current implementation target.

### Detailed spec

```text
docs/drone_sound_v2_spec.md
```

### Goal

Add simulation-driven per-motor sound with Controller-tab controls for tuning, testing, and training.

### User value

- The Controller tab becomes a sound lab.
- Direct RPM mode can be used to demonstrate how RPM affects sound.
- Motor failures and imbalance become audible.
- Low battery and safety states can produce warning cues.
- The sound starts to teach operator intuition.

### Core behavior

- Four motor audio streams.
- Each motor stream follows that motor’s RPM.
- Per-motor mix slider controls how strongly individual motor differences are heard.
- Roughness slider controls audible instability from RPM spread, wind, descent, angular rates, and failures.
- Sound test mode allows audio-only testing while disarmed.
- Low/critical battery warning cues.
- Sound settings controlled inside the `Controller` tab.

### Inputs

- `DroneState.armed`
- `DroneState.throttlePercent`
- `DroneState.verticalSpeedMS`
- `DroneState.groundSpeedMS`
- `DroneState.rollSpeedRadS`
- `DroneState.pitchSpeedRadS`
- `DroneState.yawSpeedRadS`
- `DroneState.batteryRemainingPercent`
- `DroneState.batteryCurrentCa`
- `DroneState.motors[].rpm`
- `DroneState.motors[].command`
- `DroneState.motors[].failed`
- `FailureState.windGustsMs`
- `FailureState.motorFailureMask`
- `FailureState.payloadMassKg`
- `FailureState.lostLinkActive`
- `FailureState.unsafeMissionReserveActive`

### Audio backend

Recommended:

- `SoundPool`

Why:

- Fast to implement.
- Android-native.
- Good enough for adaptive loop playback.
- Lower risk than NDK/Oboe.

### UI surface

Primary:

```text
Controller -> Drone Sound Lab
```

Controls:

- Sound enabled
- Master volume
- Per-motor mix
- Roughness
- Alerts enabled
- Sound test mode
- Test RPM
- Reset sound

### Assets

Minimum:

```text
app/src/main/res/raw/drone_motor_loop.wav
```

Optional:

```text
app/src/main/res/raw/drone_motor_rough_loop.wav
app/src/main/res/raw/drone_warning_beep.wav
app/src/main/res/raw/drone_motor_sputter.wav
```

### Acceptance criteria

See `docs/drone_sound_v2_spec.md`.

### Limitations

- Still sample-loop based.
- Pitch changes via playback rate, not physical synthesis.
- No true blade-pass harmonic generation.
- No 3D positional audio.
- No Doppler.
- No distance attenuation.

---

## Version 3 — Hybrid Procedural Drone Acoustics

### Status

Future, with detailed specification now available.

Detailed V3 spec:

```text
docs/drone_sound_v3_hybrid_procedural_spec.md
```

### Goal

Add procedural audio layers so MAVLab sound becomes more physically explainable and less dependent on looped samples.

### User value

- Sound responds more naturally to RPM changes.
- Per-motor differences sound less like four copies of the same sample.
- MAVLab can explain the relationship between RPM, blade count, blade-pass frequency, and harmonics.
- Better training value for drone engineering students.

### Core behavior

Keep V2 sample loops, but add procedural layers:

1. Blade-passing harmonic layer.
2. Filtered broadband prop-wash noise.
3. Motor/ESC whine layer.
4. Maneuver roughness layer.
5. Descent/prop-wash turbulence layer.

### Key acoustic model

Blade-pass frequency:

```text
BPF = blade_count * RPM / 60
```

For a 2-blade prop:

```text
BPF = 2 * RPM / 60
```

At 6000 RPM:

```text
BPF = 200 Hz
```

Generate harmonics:

```text
200 Hz, 400 Hz, 600 Hz, 800 Hz, ...
```

### Inputs

Everything from V2, plus:

- blade count,
- prop radius,
- max reference RPM by drone profile,
- optional drone acoustic profile.

### Audio backend options

Recommended first:

- `AudioTrack` in streaming mode for procedural layer,
- keep `SoundPool` for loop/cue playback.

Possible architecture:

```text
SoundPool: sample body / loop / warning cues
AudioTrack: procedural harmonics + filtered noise
```

### UI surface

Controller remains the primary tuning surface.

Add optional advanced controls behind an expanded panel:

- Harmonic layer amount
- Prop wash amount
- Motor whine amount
- Blade count
- Acoustic profile selector

### New files likely needed

```text
app/src/main/java/com/ascend/mavlab/simulation/audio/ProceduralDroneSynth.kt
app/src/main/java/com/ascend/mavlab/simulation/audio/DroneAcousticProfile.kt
app/src/main/java/com/ascend/mavlab/simulation/audio/BladePassModel.kt
```

### Acceptance criteria

- Procedural BPF tone tracks RPM.
- Sound remains smooth during RPM sweeps.
- No clicks/pops/underruns during normal use.
- User can hear difference between sample-only and hybrid mode.
- Unit tests verify BPF calculations.

### Risks

- Kotlin `AudioTrack` rendering may suffer underruns if not carefully threaded.
- Bad procedural synthesis can sound artificial or irritating.
- Needs careful gain staging to avoid clipping.

---

## Version 4 — Spatial and Environmental Sound

### Status

Future.

### Goal

Make MAVLab sound respond to where the drone is relative to the viewer/listener and to the environment.

### User value

- Flybys feel realistic.
- Distance and altitude are intuitive.
- Students understand acoustic signatures during approach, departure, landing, and low-altitude operations.
- Medical drone logistics missions feel more cinematic and realistic.

### Core behavior

- Distance attenuation.
- Stereo panning.
- Listener-relative direction effects.
- Doppler shift for flybys.
- Ground reflection approximation.
- High-frequency rolloff with distance.
- Optional occlusion by terrain/buildings later.

### Inputs

- drone position,
- drone velocity,
- listener/camera position,
- listener/camera orientation,
- altitude above ground,
- terrain/building occlusion if available,
- environment profile.

### Audio backend options

Possible:

- `AudioTrack` hybrid engine,
- Android spatializer where appropriate,
- Oboe if low-latency custom spatial processing becomes necessary.

### UI surface

Primary controls should not clutter Controller. V4 can expose simple controls in Controller and deeper diagnostics in SIM/Ops.

Controller controls:

- Spatial audio enabled
- Listener mode: Camera / Pilot / Ground observer

SIM controls:

- Sound field visualization
- Listener position marker

Ops controls:

- Environment profile
- Debug acoustic telemetry

### Acceptance criteria

- Sound gets quieter with distance.
- Flyby pitch shift is subtle but detectable.
- Camera/listener mode changes pan/attenuation.
- Low-altitude flight has stronger ground-proximity character.

### Risks

- Overcomplicating the system before sound quality is strong.
- Spatial effects may be hard to judge on phone speakers.
- Bluetooth latency may make flyby cues feel disconnected.

---

## Version 5 — Drone Profiles and Training Scenarios

### Status

Future.

### Goal

Support multiple drone acoustic profiles and use sound as part of training/failure drills.

### User value

MAVLab becomes a serious operator-training tool where users learn to identify drone state and failure modes by telemetry, visual behavior, and sound.

### Core behavior

Drone acoustic profiles:

- small racing quad,
- MAVLab trainer quad,
- Ascend medical logistics quad,
- heavy payload quad,
- hexacopter,
- low-noise prop profile,
- damaged prop profile.

Training drills:

- identify motor failure by sound,
- identify prop imbalance,
- identify overloaded payload,
- identify low battery under load,
- identify descent turbulence,
- compare normal hover vs gusty hover,
- compare no-payload vs 4 kg payload climb.

### Inputs

Everything from V2-V4, plus:

- selected drone profile,
- training scenario state,
- scoring/debrief state.

### UI surface

Controller:

- profile selector,
- sound training mode,
- quick failure audio drills.

SIM:

- visual scenario playback.

Ops:

- debrief/export.

### Acceptance criteria

- Profiles sound meaningfully different.
- Training drill can hide failure cause and ask user to diagnose.
- Debrief shows what was heard and why.

---

## Version 6 — Native Oboe Engine and AI Debrief Integration

### Status

Long-term future.

### Goal

Move from adaptive sample playback to a production-grade native audio engine and connect audio signatures to post-flight AI debrief.

### User value

- MAVLab becomes a high-fidelity drone digital-twin platform.
- Sound is treated as a first-class telemetry modality.
- AI debrief can explain mission behavior through sound and state.

### Core behavior

- Native Oboe C++ audio callback.
- Real-time procedural oscillator bank.
- Broadband rotor-noise model.
- Motor/ESC whine synthesis.
- Dynamic range control.
- Better spatial processing.
- Audio event logging.
- Flight debrief uses sound-state features.

### Possible audio-derived debrief features

- average acoustic load,
- roughness over time,
- motor imbalance events,
- RPM strain under payload,
- high-throttle exposure,
- warning cue timeline,
- acoustic signature of failure drills.

### AI debrief examples

```text
During the climb segment, the motor sound became louder and rougher because payload mass increased the thrust requirement and battery voltage sag increased current draw.
```

```text
A left-side motor failure was audible as a sudden loss of one motor stream followed by increased roughness in the remaining motors.
```

```text
The descent sounded choppy because the simulated vehicle entered disturbed downwash; this matched the increase in vertical descent rate and roughness score.
```

### Acceptance criteria

- Native engine performs reliably on target Android devices.
- No audio underruns during normal simulation.
- Audio telemetry can be exported with flight session data.
- Debrief can reference sound-related events accurately.

### Risks

- NDK complexity.
- More difficult debugging.
- Needs careful architecture so the audio engine does not destabilize the app.

---

## Version decision table

| Version | Main technique | Main value | Current priority |
|---|---|---|---|
| V1 | One SoundPool loop | Basic life/feedback | Skip/reference |
| V2 | Four SoundPool motor streams | Per-motor adaptive training sound | Build now |
| V3 | Hybrid SoundPool + AudioTrack | Procedural RPM/BPF realism | Next after V2 |
| V4 | Spatial/environment model | Distance/flyby realism | Later |
| V5 | Profiles + drills | Operator training product | Later |
| V6 | Oboe + AI debrief | Production digital-twin audio | Long-term |

---

## Research reference anchors

Useful sources from the research pass:

- NASA — Identification and Prediction of Broadband Noise for a Small Quadcopter  
  https://ntrs.nasa.gov/citations/20220010078

- NASA — Acoustic Wind Tunnel Measurements of a Quadcopter in Hover and Forward Flight Conditions  
  https://ntrs.nasa.gov/citations/20200002564

- NASA — Identification and Reduction of Interactional Noise of a Quadcopter in Hover and Forward Flight Conditions  
  https://ntrs.nasa.gov/citations/20220006250

- Tinney & Sirohi — Multirotor Drone Noise at Static Thrust  
  https://arc.aiaa.org/doi/abs/10.2514/1.J056827

- JASA — Broadband noise modulation of multirotor aircraft  
  https://pubs.aip.org/asa/jasa/article/157/2/924/3334479/Broadband-noise-modulation-of-multirotor-aircrafta

- Drone Acoustic Analysis for Predicting Psychoacoustic Annoyance  
  https://arxiv.org/html/2410.22208

---

## Implementation guardrail for all versions

Never let audio controls bypass the simulator unless explicitly in `Sound test mode`.

Default path must remain:

```text
Controller/QGC input
  -> PhysicsSimulationEngine
  -> DroneState.motors
  -> DroneSoundModel
  -> DroneSoundController
```

This keeps MAVLab honest as a digital twin instead of turning sound into decorative UI noise.
