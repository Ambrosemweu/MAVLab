# MAVLab Drone Sound Version 3 Specification

> **For Hermes:** Use `drone-simulation-digital-twins`, `writing-plans`, and, if implementing, `subagent-driven-development`. This spec assumes Version 2 exists or is being implemented first. Do not skip the V2 architecture unless Ambrose explicitly changes direction.

**Feature name:** MAVLab Drone Sound V3 — Hybrid Procedural Drone Acoustics

**Goal:** Build on V2’s Controller-tunable per-motor SoundPool system by adding procedural acoustic layers for blade-pass harmonics, prop-wash broadband noise, motor/ESC whine, and maneuver/descent turbulence, while preserving the same simulation-driven architecture and Controller-tab tuning surface.

**Builds on:** `docs/drone_sound_v2_spec.md`

**Version roadmap reference:** `docs/drone_sound_versions.md`

**Primary user surface:** `Controller -> Drone Sound Lab`, with optional advanced controls.

**Runtime owner:** `AppRuntime`, through `DroneSoundController` and a new procedural synthesis engine.

**Recommended implementation level for V3:** Hybrid audio: keep V2 SoundPool loop/cue playback, add a Kotlin `AudioTrack` procedural layer first. Do not jump to Oboe/NDK unless the Kotlin prototype proves insufficient.

---

## 1. Product intent

Version 2 makes MAVLab sound responsive and useful. Version 3 makes the sound explainable.

V2 says:

```text
The drone sounds faster because the loop playback rate increased with RPM.
```

V3 should be able to say:

```text
The drone sounds faster because each motor RPM produced a blade-pass frequency, and MAVLab generated harmonics from that frequency. Prop-wash noise increased because throttle, climb rate, current draw, and payload load increased.
```

The point is not to build a perfect aeroacoustic simulator yet. The point is to add a physically meaningful procedural layer that teaches users why drone sound changes.

V3 turns sound into a STEM teaching object:

- RPM becomes audible pitch.
- Blade count becomes blade-pass frequency.
- Prop wash becomes broadband noise.
- Motor imbalance becomes beating/roughness.
- Descent becomes turbulent/choppy noise.
- Payload climb becomes louder/strained sound.
- Motor/ESC whine becomes a high-frequency technical cue.

---

## 2. Relationship to Version 2

V3 must extend V2, not replace it.

V2 architecture:

```text
DroneState + FailureState + DroneSoundSettings
  -> DroneSoundModel
  -> DroneSoundController
  -> SoundPool motor loops + warning cues
  -> Controller Drone Sound Lab
```

V3 architecture:

```text
DroneState + FailureState + DroneSoundSettings + DroneAcousticProfile
  -> DroneSoundModel
  -> DroneSoundController
      -> SoundPool motor loops + warning cues
      -> ProceduralDroneSynth / AudioTrack layer
  -> Controller Drone Sound Lab advanced controls
```

V3 must keep these V2 guarantees:

1. `AppRuntime` owns audio lifecycle.
2. `Controller` tab controls settings but does not directly play audio.
3. Default audio is driven by `DroneState.motors`, not fake UI state.
4. `Sound test mode` is the only audio-only bypass, and it must not mutate simulation state.
5. Direct RPM mode remains the best manual sound test path.
6. Sound must not affect physics.
7. Sound must continue across tab switches.
8. Sound resources must be released cleanly on runtime stop.

---

## 3. Scope

### In scope for V3

1. Preserve V2 SoundPool motor-loop playback.
2. Add a procedural synthesis layer using `AudioTrack` streaming PCM.
3. Add a `DroneAcousticProfile` model.
4. Add blade-pass frequency calculation.
5. Add per-motor harmonic oscillator bank.
6. Add filtered broadband prop-wash noise.
7. Add high-frequency motor/ESC whine.
8. Add procedural roughness modulation tied to V2 roughness.
9. Add descent/turbulence noise boost.
10. Add payload/current/load strain modulation.
11. Add Controller-tab advanced sound controls.
12. Add debug readouts for BPF, harmonic layer, prop-wash, and synth health.
13. Add unit tests for acoustic math and parameter mapping.
14. Add integration tests for settings/model behavior.
15. Keep V2 fallback: app must still produce sound if procedural synth is disabled.

### Out of scope for V3

1. Full native Oboe/NDK audio engine.
2. True 3D positional audio.
3. Doppler shift.
4. Distance attenuation.
5. Terrain/building occlusion.
6. Ground reflection.
7. Atmospheric absorption.
8. Physically calibrated SPL.
9. AI flight debrief.
10. Multiple full drone profile library with training scenarios.

Those belong to V4, V5, and V6.

---

## 4. User experience

### 4.1 Controller tab remains the sound lab

V3 should extend the existing `Drone Sound Lab` card from V2.

Do not create a separate top-level tab for sound.

V2 controls remain:

- `Sound enabled`
- `Master volume`
- `Per-motor mix`
- `Roughness`
- `Alerts enabled`
- `Sound test mode`
- `Test RPM`
- `Reset sound`

V3 adds an expandable advanced section:

```text
Advanced acoustics
```

Controls in `Advanced acoustics`:

1. `Procedural layer`
   - Type: Switch.
   - Default: `true` after V3 ships.
   - When off, sound falls back to V2 SoundPool-only behavior.

2. `Sample bed`
   - Type: Slider.
   - Range: `0.0..1.0`.
   - Default: `0.55`.
   - Controls how much V2 loop sound remains under the procedural layer.

3. `Blade harmonics`
   - Type: Slider.
   - Range: `0.0..1.0`.
   - Default: `0.60`.
   - Controls procedural blade-pass harmonic layer.

4. `Prop wash`
   - Type: Slider.
   - Range: `0.0..1.0`.
   - Default: `0.50`.
   - Controls broadband aerodynamic noise.

5. `Motor whine`
   - Type: Slider.
   - Range: `0.0..1.0`.
   - Default: `0.25`.
   - Controls high-frequency motor/ESC tone layer.

6. `Blade count`
   - Type: segmented control or small selector.
   - Values: `2`, `3`, `4`.
   - Default: `2`.
   - Affects blade-pass frequency.

7. `Acoustic profile`
   - Type: selector.
   - V3 minimum values:
     - `MAVLab Trainer Quad`
     - `Small Racing Quad`
     - `Cargo Quad`
   - If full selector is too much for V3, keep only `MAVLab Trainer Quad` internally and expose profile selection in V5.

8. `Synth quality`
   - Type: selector.
   - Values:
     - `Eco`
     - `Balanced`
     - `High`
   - Default: `Balanced`.
   - Controls number of harmonics and noise/filter update cost.

9. `Show acoustic telemetry`
   - Type: Switch.
   - Default: `false`.
   - Reveals BPF/harmonic readouts and synth performance state.

### 4.2 New live readouts

When `Show acoustic telemetry` is enabled, show:

- `Blade-pass frequency: ### Hz`
- `Harmonics: N`
- `Prop wash: ##%`
- `Motor whine: ##%`
- `Synth: Running / Disabled / Underrun risk / Fallback`
- `Sample rate: ##### Hz`
- `Buffer: ### frames`

If per-motor readout is not too cluttered, show compact values:

```text
BPF M1/M2/M3/M4: 198 / 204 / 199 / 206 Hz
```

### 4.3 Teaching copy

The expanded section should include a short explanation:

```text
Blade-pass frequency = blade count × RPM / 60. MAVLab uses this to synthesize rotor harmonics above the sample loop.
```

This turns Controller into a learning surface, not just a settings panel.

---

## 5. Acoustic model

### 5.1 Blade-pass frequency

Formula:

```text
BPF = blade_count * RPM / 60
```

For a 2-blade prop:

```text
BPF = 2 * RPM / 60
```

For a 3-blade prop:

```text
BPF = 3 * RPM / 60
```

Reference examples:

| RPM | 2-blade BPF | First 5 harmonics | 3-blade BPF | First 5 harmonics |
|---:|---:|---|---:|---|
| 1500 | 50.00 Hz | 50, 100, 150, 200, 250 | 75.00 Hz | 75, 150, 225, 300, 375 |
| 3000 | 100.00 Hz | 100, 200, 300, 400, 500 | 150.00 Hz | 150, 300, 450, 600, 750 |
| 4500 | 150.00 Hz | 150, 300, 450, 600, 750 | 225.00 Hz | 225, 450, 675, 900, 1125 |
| 6000 | 200.00 Hz | 200, 400, 600, 800, 1000 | 300.00 Hz | 300, 600, 900, 1200, 1500 |
| 7500 | 250.00 Hz | 250, 500, 750, 1000, 1250 | 375.00 Hz | 375, 750, 1125, 1500, 1875 |
| 9500 | 316.67 Hz | 316.67, 633.33, 950, 1266.67, 1583.33 | 475.00 Hz | 475, 950, 1425, 1900, 2375 |

MAVLab’s current max motor speed of about `1000 rad/s` is about `9549 RPM`, so `9500 RPM` remains a practical reference max.

### 5.2 Harmonic oscillator layer

For each motor:

```text
fundamental = blade_count * rpm / 60
harmonic_n = fundamental * n
```

Generate harmonics up to either:

- `N` harmonics, or
- a max frequency cutoff, whichever comes first.

Suggested defaults:

| Synth quality | Harmonics per motor | Max harmonic frequency |
|---|---:|---:|
| Eco | 4 | 2500 Hz |
| Balanced | 8 | 4500 Hz |
| High | 12 | 6500 Hz |

Harmonic amplitude rolloff:

```text
amplitude_n = harmonicGain / n^1.35
```

Alternative if too bright:

```text
amplitude_n = harmonicGain / n^1.6
```

Per-motor detune should come from actual motor RPM differences. Do not add large fake detune unless in sound-test mode.

### 5.3 Broadband prop-wash layer

Drone sound needs broadband noise, not just harmonic tones.

V3 should synthesize filtered noise whose gain and brightness respond to:

- average RPM,
- throttle/motor command,
- climb rate,
- descent rate,
- ground speed,
- wind gusts,
- payload mass,
- current draw.

Suggested derived values:

```kotlin
rpmNorm = (averageRpm / profile.maxReferenceRpm).coerceIn(0f, 1f)
throttleNorm = state.throttlePercent.toInt() / 100f
climbNorm = (state.verticalSpeedMS / 4f).coerceIn(0f, 1f)
descentNorm = (-state.verticalSpeedMS / 4f).coerceIn(0f, 1f)
groundSpeedNorm = (state.groundSpeedMS / 18f).coerceIn(0f, 1f)
windGustNorm = (failures.windGustsMs / 10f).coerceIn(0f, 1f)
payloadNorm = (failures.payloadMassKg / 4f).coerceIn(0f, 1f)
currentNorm = (state.batteryCurrentCa / 2500f).coerceIn(0f, 1f)
```

Suggested prop-wash gain:

```kotlin
propWashGain = settings.propWashAmount * (
    0.30f * rpmNorm +
    0.25f * throttleNorm +
    0.10f * climbNorm +
    0.15f * descentNorm +
    0.08f * groundSpeedNorm +
    0.07f * windGustNorm +
    0.05f * payloadNorm * currentNorm
).coerceIn(0f, 1f)
```

Suggested filter behavior:

- Low band body/chop: `120–450 Hz`
- Mid buzz: `500–2400 Hz`
- High hiss/air: `2500–8000 Hz`

V3 can implement a simplified single filtered-noise layer first, then split into bands later.

Minimum acceptable V3:

- one procedural noise source,
- high-pass or band-pass shaping,
- brightness increases with RPM,
- gain increases with throttle/load/descent.

### 5.4 Motor/ESC whine layer

Motor/ESC whine should be subtle.

Suggested tone sources:

```text
mechanicalMotorHz = RPM / 60
escWhineHz = 1800 + 5200 * rpmNorm
```

Use one or two quiet high-frequency oscillators:

```kotlin
whineFreq1 = 1800f + 5200f * rpmNorm
whineFreq2 = whineFreq1 * 1.37f
```

Whine gain:

```kotlin
whineGain = settings.motorWhineAmount * (0.15f + 0.85f * rpmNorm) * 0.18f
```

Do not make whine painfully sharp on phone speakers. Keep default amount low.

### 5.5 Roughness and modulation

V2 already computes roughness from RPM spread, angular rate, wind gusts, descent, and failure state.

V3 should use the same roughness signal to modulate:

- harmonic amplitude,
- noise gain,
- tiny pitch jitter,
- tremolo/fluctuation.

Suggested modulation rates:

- slow hover wobble: `2–5 Hz`,
- maneuver roughness: `6–14 Hz`,
- failure roughness: intermittent random gating or stronger tremolo.

Do not overdo it. MAVLab should sound like a drone, not a sci-fi alarm.

### 5.6 Descent/turbulence layer

During descent, especially fast descent, add choppy broadband noise.

Trigger:

```kotlin
descentNorm = (-state.verticalSpeedMS / 4f).coerceIn(0f, 1f)
```

Behavior:

- increase broadband noise gain,
- increase low/mid amplitude modulation,
- add random flutter to prop-wash layer,
- do not change simulation physics.

### 5.7 Payload/load strain

Payload and current draw should make the drone sound more loaded, especially during climb.

Suggested behavior:

```kotlin
loadStrain = (payloadNorm * climbNorm * 0.45f + currentNorm * 0.25f).coerceIn(0f, 1f)
```

Apply:

- slight harmonic gain increase,
- prop-wash gain increase,
- subtle roughness increase,
- optional lower sample-bed rate by a tiny amount if the sound feels too clean.

---

## 6. Data model changes

### 6.1 Extend DroneSoundSettings

Modify:

```text
app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundSettings.kt
```

Add fields:

```kotlin
data class DroneSoundSettings(
    val enabled: Boolean = true,
    val masterVolume: Float = 0.65f,
    val perMotorMix: Float = 0.75f,
    val roughness: Float = 0.45f,
    val alertsEnabled: Boolean = true,
    val testMode: Boolean = false,
    val testRpm: Float = 3500f,

    // V3 fields
    val proceduralEnabled: Boolean = true,
    val sampleBedAmount: Float = 0.55f,
    val bladeHarmonicsAmount: Float = 0.60f,
    val propWashAmount: Float = 0.50f,
    val motorWhineAmount: Float = 0.25f,
    val bladeCount: Int = 2,
    val acousticProfileId: String = DroneAcousticProfile.DefaultId,
    val synthQuality: DroneSynthQuality = DroneSynthQuality.BALANCED,
    val showAcousticTelemetry: Boolean = false,
)
```

Create enum:

```kotlin
enum class DroneSynthQuality {
    ECO,
    BALANCED,
    HIGH,
}
```

### 6.2 Add DroneAcousticProfile

Create:

```text
app/src/main/java/com/ascend/mavlab/simulation/audio/DroneAcousticProfile.kt
```

Suggested model:

```kotlin
data class DroneAcousticProfile(
    val id: String,
    val label: String,
    val bladeCount: Int,
    val maxReferenceRpm: Float,
    val harmonicBrightness: Float,
    val propWashBrightness: Float,
    val whineBrightness: Float,
    val sampleBedDefault: Float,
) {
    companion object {
        const val DefaultId = "mavlab_trainer_quad"

        val MavLabTrainerQuad = DroneAcousticProfile(
            id = DefaultId,
            label = "MAVLab Trainer Quad",
            bladeCount = 2,
            maxReferenceRpm = 9500f,
            harmonicBrightness = 0.65f,
            propWashBrightness = 0.55f,
            whineBrightness = 0.35f,
            sampleBedDefault = 0.55f,
        )
    }
}
```

V3 may include additional profiles if simple:

```kotlin
SmallRacingQuad
CargoQuad
```

But V3 should not spend too much time on profile library polish. The main goal is procedural hybrid sound.

### 6.3 Extend DroneSoundFrame

Modify:

```text
app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundFrame.kt
```

Add:

```kotlin
data class DroneSoundFrame(
    val enabled: Boolean,
    val motors: List<MotorSoundFrame>,
    val averageRpm: Float,
    val rpmSpreadPercent: Float,
    val activeMotorCount: Int,
    val roughness: Float,
    val alert: DroneSoundAlert,

    // V3
    val procedural: ProceduralSoundFrame = ProceduralSoundFrame.Disabled,
)

data class ProceduralSoundFrame(
    val enabled: Boolean,
    val bladeCount: Int,
    val sampleBedAmount: Float,
    val bladeHarmonicsAmount: Float,
    val propWashAmount: Float,
    val motorWhineAmount: Float,
    val averageBladePassHz: Float,
    val motorBladePassHz: List<Float>,
    val harmonicCount: Int,
    val propWashGain: Float,
    val motorWhineGain: Float,
    val loadStrain: Float,
) {
    companion object {
        val Disabled = ProceduralSoundFrame(
            enabled = false,
            bladeCount = 2,
            sampleBedAmount = 1f,
            bladeHarmonicsAmount = 0f,
            propWashAmount = 0f,
            motorWhineAmount = 0f,
            averageBladePassHz = 0f,
            motorBladePassHz = emptyList(),
            harmonicCount = 0,
            propWashGain = 0f,
            motorWhineGain = 0f,
            loadStrain = 0f,
        )
    }
}
```

### 6.4 Extend DroneSoundDebugState

Modify:

```text
app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundDebugState.kt
```

Add:

```kotlin
val proceduralEnabled: Boolean = false,
val averageBladePassHz: Float = 0f,
val harmonicCount: Int = 0,
val propWashPercent: Float = 0f,
val motorWhinePercent: Float = 0f,
val synthStatus: String = "Disabled",
val sampleRateHz: Int = 0,
val bufferFrames: Int = 0,
val underrunCount: Int = 0,
```

---

## 7. Procedural synthesis engine

### 7.1 Create ProceduralDroneSynth

Create:

```text
app/src/main/java/com/ascend/mavlab/simulation/audio/ProceduralDroneSynth.kt
```

Responsibility:

- Own `AudioTrack`.
- Run a dedicated audio render thread or coroutine dispatcher.
- Render PCM continuously.
- Receive `ProceduralSoundFrame` updates from `DroneSoundController`.
- Smooth parameter changes to avoid clicks.
- Report debug status and underruns where possible.

Suggested API:

```kotlin
class ProceduralDroneSynth(
    private val sampleRateHz: Int,
    private val bufferFrames: Int,
) {
    val status: StateFlow<ProceduralSynthStatus>

    fun start()
    fun stop()
    fun release()
    fun setFrame(frame: ProceduralSoundFrame)
    fun setMasterVolume(volume: Float)
}
```

Status:

```kotlin
data class ProceduralSynthStatus(
    val running: Boolean = false,
    val sampleRateHz: Int = 0,
    val bufferFrames: Int = 0,
    val underrunCount: Int = 0,
    val fallbackReason: String? = null,
)
```

### 7.2 AudioTrack setup

Use Android framework APIs only for V3:

```kotlin
AudioTrack.Builder()
    .setAudioAttributes(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    )
    .setAudioFormat(
        AudioFormat.Builder()
            .setSampleRate(sampleRateHz)
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
    )
    .setTransferMode(AudioTrack.MODE_STREAM)
    .setBufferSizeInBytes(bufferSizeBytes)
    .build()
```

If `ENCODING_PCM_FLOAT` causes compatibility issues, fallback to `ENCODING_PCM_16BIT`.

Query preferred sample rate and buffer size from `AudioManager`:

```kotlin
val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
val sampleRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull() ?: 48000
val framesPerBuffer = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull() ?: 192
```

V3 should handle missing properties gracefully.

### 7.3 Rendering loop rules

Inside the render loop:

- no logging,
- no allocations per sample,
- no UI calls,
- no SoundPool calls,
- no blocking on flows,
- no heavy object creation,
- use smoothed local copies of parameters.

Audio thread should read the latest frame through a thread-safe/atomic holder or synchronized copy at block boundaries, not every sample.

### 7.4 Signal chain

Per output sample:

```text
harmonic oscillator bank
+ filtered prop-wash noise
+ high-frequency motor/ESC whine
-> roughness/tremolo modulation
-> soft limiter
-> master/procedural gain
-> AudioTrack PCM
```

V2 SoundPool sample bed runs separately and is mixed by Android output. V3 should reduce SoundPool loop volume using `sampleBedAmount` to avoid overloading/clipping.

### 7.5 Soft limiter

V3 should include a simple soft limiter to prevent harsh clipping.

Suggested:

```kotlin
fun softLimit(x: Float): Float = x / (1f + abs(x))
```

or:

```kotlin
fun softLimit(x: Float): Float = tanh(x)
```

If using `tanh`, avoid expensive per-sample math if performance is poor. Approximation is acceptable.

---

## 8. Controller and SoundPool integration

Modify:

```text
app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundController.kt
```

V3 changes:

1. Create `ProceduralDroneSynth` if procedural is enabled.
2. Keep V2 SoundPool streams alive as sample bed.
3. Adjust SoundPool motor loop volume by `settings.sampleBedAmount`.
4. Send `frame.procedural` to `ProceduralDroneSynth` at 20–30 Hz.
5. If synth fails, keep V2 SoundPool fallback and expose status.
6. Release synth in `stop()`/`release()`.

Important fallback rule:

If `ProceduralDroneSynth` fails to start, V3 must not make the whole sound system fail. It should set:

```text
Synth: Fallback
```

and continue V2 SoundPool-only audio.

---

## 9. Controller UI implementation

Modify:

```text
app/src/main/java/com/ascend/mavlab/feature/controller/ControllerScreen.kt
```

Update `DroneSoundLabCard` to include:

- V2 basic controls,
- expandable `Advanced acoustics` section,
- V3 sliders/selectors,
- acoustic telemetry readouts.

Suggested composable split:

```kotlin
@Composable
private fun DroneSoundLabCard(...)

@Composable
private fun AdvancedAcousticsControls(...)

@Composable
private fun AcousticTelemetryReadouts(...)
```

Do not let this file become unmanageably large. If needed, create:

```text
app/src/main/java/com/ascend/mavlab/feature/controller/DroneSoundLabCard.kt
```

This is preferred if the UI becomes more than ~150 lines.

---

## 10. Persistence

Extend V2 persistence.

Persist:

```text
drone_sound_procedural_enabled
drone_sound_sample_bed_amount
drone_sound_blade_harmonics_amount
drone_sound_prop_wash_amount
drone_sound_motor_whine_amount
drone_sound_blade_count
drone_sound_acoustic_profile_id
drone_sound_synth_quality
drone_sound_show_acoustic_telemetry
```

Do not persist runtime synth fallback/errors.

As in V2, do not persist `testMode = true` across app restart.

---

## 11. Testing plan

### 11.1 Unit tests for acoustic math

Create:

```text
app/src/test/java/com/ascend/mavlab/simulation/audio/BladePassModelTest.kt
```

Test cases:

1. `twoBladeBpfAt6000RpmIs200Hz`
2. `threeBladeBpfAt6000RpmIs300Hz`
3. `zeroRpmProducesZeroBpf`
4. `negativeRpmIsClampedToZero`
5. `harmonicsStopAtConfiguredCount`
6. `harmonicsStopAtMaxFrequency`
7. `balancedQualityUsesExpectedHarmonicCount`

### 11.2 Unit tests for DroneSoundModel V3 fields

Modify or add:

```text
app/src/test/java/com/ascend/mavlab/simulation/audio/DroneSoundModelV3Test.kt
```

Test cases:

1. `proceduralDisabledProducesDisabledProceduralFrame`
2. `proceduralEnabledComputesBladePassForEachMotor`
3. `bladeCountSettingChangesBladePassFrequency`
4. `propWashGainIncreasesWithThrottle`
5. `propWashGainIncreasesDuringDescent`
6. `motorWhineGainIncreasesWithRpm`
7. `payloadAndCurrentIncreaseLoadStrain`
8. `soundDisabledSilencesProceduralLayer`
9. `testModeUsesTestRpmForProceduralFrame`
10. `synthQualityControlsHarmonicCount`

### 11.3 Controller tests

If existing Compose tests are available, add simple assertions that:

- `Advanced acoustics` appears in Drone Sound Lab.
- Procedural layer switch exists.
- Blade count selector exists.
- Acoustic telemetry can be toggled.

If Compose tests are too heavy, document manual verification.

### 11.4 Manual audio tests

Run on real device:

1. With procedural layer off, confirm V2 behavior still works.
2. Turn procedural layer on; sound should become brighter/more detailed.
3. Sweep Direct RPM from 0 to 9500.
4. Confirm BPF readout tracks RPM.
5. Change blade count from 2 to 3; harmonic pitch should change.
6. Increase `Blade harmonics`; tonal buzz becomes more present.
7. Increase `Prop wash`; broadband air noise increases.
8. Increase `Motor whine`; high-frequency whine increases but does not hurt.
9. Simulate descent; prop wash/choppiness increases.
10. Simulate payload/current load; sound becomes more strained.
11. Turn procedural off; SoundPool fallback remains audible.
12. Background/foreground app; no duplicate synth threads.
13. Stop service/app; no audio continues.

### 11.5 Performance tests

Check:

- no audible clicks/pops,
- no severe underruns,
- CPU acceptable on target phone,
- no UI jank while audio runs,
- no memory growth after repeated start/stop,
- emulator is not used as final audio-quality reference.

Commands:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

---

## 12. Acceptance criteria

V3 is accepted when:

1. V2 features still work.
2. Controller tab includes `Advanced acoustics` controls.
3. Procedural layer can be enabled/disabled.
4. SoundPool sample bed remains available as fallback.
5. Procedural layer produces audible harmonic detail tied to RPM.
6. Blade count changes blade-pass frequency.
7. BPF telemetry readout matches formula.
8. Prop wash layer responds to throttle, descent, and load.
9. Motor whine responds to RPM and remains subtle by default.
10. Roughness continues to respond to imbalance/failures/maneuvers.
11. Sound test mode drives both SoundPool and procedural layers without mutating `DroneState`.
12. Direct RPM mode is usable for V3 tuning.
13. AppRuntime still owns lifecycle.
14. Procedural synth failure falls back to V2 SoundPool-only sound.
15. Unit tests cover BPF, harmonics, prop wash, whine, load strain, and settings behavior.
16. `./gradlew testDebugUnitTest` passes.
17. `./gradlew assembleDebug` passes.
18. Real-device manual QA confirms no major clicks, clipping, runaway CPU, or duplicated audio.

---

## 13. Implementation tasks

### Task 1: Add V3 settings and enums

**Objective:** Extend the V2 sound settings model with procedural controls.

**Files:**

- Modify: `app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundSettings.kt`
- Create if needed: `app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSynthQuality.kt`

**Steps:**

1. Add V3 fields to `DroneSoundSettings`.
2. Add `DroneSynthQuality` enum.
3. Keep existing V2 defaults unchanged.
4. Ensure `testMode` default remains `false`.

**Verification:**

```bash
./gradlew testDebugUnitTest
```

Expected:

- Existing tests pass.

### Task 2: Add DroneAcousticProfile

**Objective:** Define profile data used by the procedural model.

**Files:**

- Create: `app/src/main/java/com/ascend/mavlab/simulation/audio/DroneAcousticProfile.kt`
- Test: `app/src/test/java/com/ascend/mavlab/simulation/audio/DroneAcousticProfileTest.kt`

**Steps:**

1. Create `DroneAcousticProfile` data class.
2. Add default `MAVLab Trainer Quad` profile.
3. Optional: add `Small Racing Quad` and `Cargo Quad` if simple.
4. Add lookup by ID.
5. Test lookup fallback to default.

**Verification:**

```bash
./gradlew testDebugUnitTest --tests "com.ascend.mavlab.simulation.audio.DroneAcousticProfileTest"
```

### Task 3: Add blade-pass and harmonic math

**Objective:** Add pure acoustic math helpers before audio rendering.

**Files:**

- Create: `app/src/main/java/com/ascend/mavlab/simulation/audio/BladePassModel.kt`
- Test: `app/src/test/java/com/ascend/mavlab/simulation/audio/BladePassModelTest.kt`

**Suggested API:**

```kotlin
object BladePassModel {
    fun bladePassHz(rpm: Float, bladeCount: Int): Float
    fun harmonics(
        fundamentalHz: Float,
        maxCount: Int,
        maxFrequencyHz: Float,
    ): List<Float>
}
```

**Verification:**

```bash
./gradlew testDebugUnitTest --tests "com.ascend.mavlab.simulation.audio.BladePassModelTest"
```

Expected examples:

- 2 blades at 6000 RPM = 200 Hz.
- 3 blades at 6000 RPM = 300 Hz.

### Task 4: Extend DroneSoundFrame with procedural frame

**Objective:** Add procedural output parameters to the pure model output.

**Files:**

- Modify: `app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundFrame.kt`
- Modify: `app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundDebugState.kt`

**Verification:**

```bash
./gradlew testDebugUnitTest
```

### Task 5: Extend DroneSoundModel for V3

**Objective:** Compute procedural frame values from simulation state and settings.

**Files:**

- Modify: `app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundModel.kt`
- Create: `app/src/test/java/com/ascend/mavlab/simulation/audio/DroneSoundModelV3Test.kt`

**Steps:**

1. Resolve acoustic profile.
2. Compute per-motor BPF.
3. Compute average BPF.
4. Compute harmonic count from synth quality.
5. Compute prop-wash gain.
6. Compute motor-whine gain.
7. Compute load strain.
8. Respect sound disabled/procedural disabled/test mode.

**Verification:**

```bash
./gradlew testDebugUnitTest --tests "com.ascend.mavlab.simulation.audio.DroneSoundModelV3Test"
```

### Task 6: Implement ProceduralDroneSynth skeleton

**Objective:** Create start/stop/release lifecycle with no complex synthesis yet.

**Files:**

- Create: `app/src/main/java/com/ascend/mavlab/simulation/audio/ProceduralDroneSynth.kt`

**Steps:**

1. Add constructor and status state.
2. Add `start`, `stop`, `release`, `setFrame`, `setMasterVolume`.
3. Do not render complex audio yet.
4. Ensure repeated start/stop is safe.

**Verification:**

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### Task 7: Add AudioTrack rendering loop

**Objective:** Render silence safely through AudioTrack before adding synthesis.

**Files:**

- Modify: `ProceduralDroneSynth.kt`

**Steps:**

1. Query sample rate and buffer size.
2. Create AudioTrack.
3. Start a dedicated render loop.
4. Render zeros.
5. Track status and underruns if available.
6. Stop/release cleanly.

**Verification:**

- Launch app on device.
- Enable procedural layer.
- Confirm no crash, no audible artifacts from silence render.

### Task 8: Add harmonic oscillator bank

**Objective:** Generate blade-pass harmonic tones from procedural frame.

**Files:**

- Modify: `ProceduralDroneSynth.kt`

**Steps:**

1. Add oscillator phase storage per motor/harmonic.
2. Use latest motor BPF values.
3. Apply harmonic rolloff.
4. Smooth gain changes.
5. Apply soft limiter.

**Verification:**

- Direct RPM sweep changes procedural pitch.
- Blade count changes pitch/BPF.
- No clicks during RPM movement.

### Task 9: Add prop-wash noise layer

**Objective:** Add filtered broadband noise controlled by prop-wash gain.

**Files:**

- Modify: `ProceduralDroneSynth.kt`

**Steps:**

1. Add lightweight noise generator.
2. Add simple one-pole filters or band shaping.
3. Increase brightness/gain with RPM and prop-wash frame values.
4. Smooth parameter changes.

**Verification:**

- Prop wash slider audibly changes broadband noise.
- Descent/load increases choppiness/noise.

### Task 10: Add motor/ESC whine layer

**Objective:** Add subtle high-frequency whine controlled by RPM and settings.

**Files:**

- Modify: `ProceduralDroneSynth.kt`

**Verification:**

- Motor whine slider works.
- Default whine is subtle.
- Max whine is audible but not painful.

### Task 11: Integrate synth with DroneSoundController

**Objective:** Run SoundPool and procedural synth together.

**Files:**

- Modify: `app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundController.kt`

**Steps:**

1. Instantiate synth.
2. Send procedural frames.
3. Apply sample bed amount to SoundPool motor loop volume.
4. Handle synth fallback.
5. Merge synth status into debug state.

**Verification:**

- Procedural off = V2 sound.
- Procedural on = hybrid sound.
- Synth failure fallback still produces V2 sound.

### Task 12: Add Controller advanced acoustics UI

**Objective:** Expose V3 controls and telemetry in Controller tab.

**Files:**

- Modify: `app/src/main/java/com/ascend/mavlab/feature/controller/ControllerScreen.kt`
- Prefer create: `app/src/main/java/com/ascend/mavlab/feature/controller/DroneSoundLabCard.kt`

**Verification:**

```bash
./gradlew assembleDebug
```

Manual:

- Controls appear.
- Settings update.
- Telemetry readouts update.

### Task 13: Extend persistence

**Objective:** Persist V3 sound settings.

**Files:**

- Modify existing sound settings persistence code from V2.

**Verification:**

- Change procedural settings.
- Restart app.
- Settings persist.
- Test mode still resets to off.

### Task 14: Real-device tuning pass

**Objective:** Tune gain staging and defaults on actual phone speaker/headphones.

**Files:**

- Modify constants in sound model/synth as needed.

**Verification:**

- No clipping.
- No harsh high-frequency whine by default.
- Sound remains useful at low volume.
- Direct RPM sweep feels smooth.

---

## 14. Design guardrails

1. V3 builds on V2; do not delete V2 SoundPool fallback.
2. Procedural synthesis must be optional and disableable.
3. Sound controls remain in Controller tab.
4. AppRuntime remains lifecycle owner.
5. Sound must follow `DroneState` by default.
6. Sound test mode must not mutate simulation state.
7. Audio must not block UI, simulation, MAVLink, or recorder.
8. Audio rendering loop must avoid allocations per sample.
9. Default settings must be safe on phone speakers.
10. V3 must not become a full research-grade acoustic simulator; it is a practical hybrid layer.
11. If performance is poor, reduce synth quality before changing architecture.
12. Only consider Oboe/NDK after V3 AudioTrack prototype proves the need.

---

## 15. Future handoff notes

V3 prepares MAVLab for later versions:

- V4 can add spatial/distance/Doppler on top of procedural sources.
- V5 can map acoustic profiles to training scenarios.
- V6 can replace AudioTrack with Oboe and log audio-derived features for AI debrief.

The most important V3 artifact is not the exact synth code. It is the separation:

```text
DroneSoundModel -> procedural parameters
ProceduralDroneSynth -> render implementation
DroneSoundController -> orchestration/fallback
Controller UI -> tuning and teaching surface
```

Keep that separation clean.
