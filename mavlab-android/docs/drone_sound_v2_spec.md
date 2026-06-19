# MAVLab Drone Sound Version 2 Specification

> **For Hermes:** Use the `drone-simulation-digital-twins` and `writing-plans` skills before implementing this spec. If implementation is delegated, use `subagent-driven-development` task-by-task.

**Feature name:** MAVLab Drone Sound V2 — Controller-Tunable Per-Motor Sound

**Goal:** Add a realistic, simulation-driven drone sound system where the Controller tab exposes playable sound settings and the audio responds to actual MAVLab motor RPM, throttle, motor failures, battery/load state, and maneuver intensity.

**Primary user surface:** `Controller` tab.

**Runtime owner:** `AppRuntime`, not the Compose screen lifecycle.

**Recommended implementation level for V2:** SoundPool-based per-motor loop playback with per-motor pitch/rate/volume modulation, failure roughness, warning cues, and Controller-tab tuning controls.

---

## 1. Product intent

Drone sound should become a training signal, not only ambience.

In MAVLab V2, users should be able to open the `Controller` tab, arm the drone, move throttle or direct RPM, and immediately hear the drone sound change. The sound should make the digital twin feel alive and should help learners build intuition for:

- motor RPM,
- throttle/load,
- climb/descent strain,
- motor imbalance,
- motor failure,
- low battery warning,
- wind/gust roughness,
- payload-induced strain,
- direct RPM testing.

This version is intentionally practical. It should not attempt full aeroacoustic simulation yet. It should use MAVLab’s existing simulation telemetry and produce convincing adaptive audio at low engineering risk.

---

## 2. Scope

### In scope for V2

1. Global sound enable/disable.
2. Master volume slider.
3. Per-motor sound streams for four motors.
4. Pitch/rate modulation from each motor’s RPM.
5. Volume modulation from each motor’s RPM and command.
6. Motor failure behavior: failed motors fade or sputter.
7. Roughness/imbalance control derived from RPM spread and failure state.
8. Low-battery warning cue.
9. Optional link-loss / unsafe reserve warning cue if implementation time allows.
10. Controller-tab sound settings panel.
11. Sound test mode inside Controller tab.
12. Direct RPM mode should be the primary manual test path.
13. Pure Kotlin `DroneSoundModel` unit tests.
14. Clean start/stop/release through `AppRuntime`.

### Out of scope for V2

1. Full procedural blade-pass synthesis.
2. Native Oboe / NDK audio engine.
3. Terrain/building occlusion.
4. Physically calibrated SPL levels.
5. True 3D positional audio.
6. Doppler shift.
7. Ground reflection.
8. Atmospheric absorption.
9. Multiple drone-type acoustic profiles beyond a single default profile.
10. AI debrief integration.

Those belong to later versions documented in `docs/drone_sound_versions.md`.

---

## 3. Current MAVLab integration points

Project root:

```text
/home/ambrose/Downloads/Ascend/Drone SIM/mavlab-android
```

### Runtime owner

Modify:

```text
app/src/main/java/com/ascend/mavlab/core/common/AppRuntime.kt
```

`AppRuntime` owns:

- `PhysicsSimulationEngine`,
- `state: StateFlow<DroneState>`,
- `failures: StateFlow<FailureState>`,
- `CoroutineScope`,
- Android `Context` during `start(context)`.

Audio should start and stop here so sound is not tied to a single Compose tab.

Expected additions:

```kotlin
private var droneSoundController: DroneSoundController? = null

val soundSettings: StateFlow<DroneSoundSettings>
    get() = droneSoundController?.settings ?: fallbackSoundSettings

val soundDebugState: StateFlow<DroneSoundDebugState>
    get() = droneSoundController?.debugState ?: fallbackSoundDebugState

fun setDroneSoundEnabled(enabled: Boolean)
fun setDroneSoundMasterVolume(volume: Float)
fun setDroneSoundRoughness(value: Float)
fun setDroneSoundPerMotorMix(value: Float)
fun setDroneSoundAlertEnabled(enabled: Boolean)
fun setDroneSoundTestMode(enabled: Boolean)
fun setDroneSoundTestRpm(rpm: Float)
```

Actual implementation can use more compact naming, but Controller UI must not manipulate SoundPool directly.

### Controller tab UI

Modify:

```text
app/src/main/java/com/ascend/mavlab/feature/controller/ControllerScreen.kt
```

This is the requested user-facing control surface.

Add a new card below input mode / advanced test inputs, preferably before flight mode controls:

```text
Drone Sound Lab
```

This card should expose sound controls and live sound telemetry.

### Simulation state

Read:

```text
app/src/main/java/com/ascend/mavlab/simulation/engine/DroneState.kt
app/src/main/java/com/ascend/mavlab/simulation/engine/MotorTelemetry.kt
app/src/main/java/com/ascend/mavlab/simulation/failures/FailureInjector.kt
```

Use:

- `DroneState.armed`,
- `DroneState.throttlePercent`,
- `DroneState.verticalSpeedMS`,
- `DroneState.groundSpeedMS`,
- `DroneState.rollSpeedRadS`,
- `DroneState.pitchSpeedRadS`,
- `DroneState.yawSpeedRadS`,
- `DroneState.batteryRemainingPercent`,
- `DroneState.batteryCurrentCa`,
- `DroneState.motors`,
- `MotorTelemetry.rpm`,
- `MotorTelemetry.command`,
- `MotorTelemetry.failed`,
- `FailureState.windGustsMs`,
- `FailureState.motorFailureMask`,
- `FailureState.payloadMassKg`,
- `FailureState.lostLinkActive`,
- `FailureState.unsafeMissionReserveActive`.

---

## 4. User experience in Controller tab

### 4.1 Drone Sound Lab card

Add a Controller card titled:

```text
Drone Sound Lab
```

Subtitle:

```text
Simulation-driven motor sound. Tune the acoustic response while using Controller or Direct RPM mode.
```

Controls:

1. `Sound enabled`
   - Type: Switch.
   - Default: `true`.
   - When off, all motor loops and warning cues fade to silence.

2. `Master volume`
   - Type: Slider.
   - Range: `0.0..1.0`.
   - Default: `0.65`.
   - Display: `65%`.
   - Applies after all simulation-derived gains.

3. `Per-motor mix`
   - Type: Slider.
   - Range: `0.0..1.0`.
   - Default: `0.75`.
   - Meaning:
     - `0.0`: mostly averaged single-drone sound.
     - `1.0`: individual motor differences are emphasized.
   - In V2, this controls how strongly each motor’s own RPM changes its stream rate/volume compared to average RPM.

4. `Roughness`
   - Type: Slider.
   - Range: `0.0..1.0`.
   - Default: `0.45`.
   - Meaning:
     - Controls additional audible instability from RPM spread, wind gusts, failures, and aggressive maneuvers.
   - This should not change physics. It only changes sound.

5. `Alerts enabled`
   - Type: Switch.
   - Default: `true`.
   - Controls low battery, link loss, and unsafe mission reserve beeps.

6. `Sound test mode`
   - Type: Switch.
   - Default: `false`.
   - When enabled, sound can be tested even if the drone is disarmed.
   - Must be clearly labeled as audio-only.
   - Text:
     ```text
     Audio-only test. Does not affect simulation state.
     ```

7. `Test RPM`
   - Type: Slider.
   - Range: `0..9500`.
   - Default: `3500`.
   - Enabled only when Sound test mode is enabled.
   - It feeds the sound model only, not `PhysicsSimulationEngine`.

8. `Reset sound`
   - Type: Button.
   - Resets settings to defaults.

Live readouts:

- `Avg RPM: ####`
- `RPM spread: ##%`
- `Sound rate: #.##x`
- `Roughness: ##%`
- `Active motors: #/4`
- `Alert: None / Low battery / Link lost / Unsafe reserve`

### 4.2 Interaction with Direct RPM mode

Direct RPM mode is the best V2 test path.

Expected behavior:

- User selects `DIRECT_RPM` input mode.
- User arms drone.
- User moves the Direct RPM slider.
- Motor RPM changes in `DroneState.motors`.
- Drone sound pitch and volume change immediately.
- Failed motors should not sound like healthy motors.

Important invariant:

The Controller tab may expose sound settings, but sound must be driven by `DroneState` by default. Do not fake motor sound directly from UI controls except in explicit `Sound test mode`.

### 4.3 Interaction with GCS mission authority

If `state.controlAuthority == ControlAuthority.GCS_MISSION`, Controller manual input is paused. Sound settings should remain editable because they are not flight controls.

Expected behavior:

- Controller input controls can remain disabled during mission authority.
- Drone Sound Lab controls remain enabled.
- Sound continues following mission-generated `DroneState.motors`.

---

## 5. Audio behavior specification

### 5.1 Motor stream model

V2 should use four motor streams:

```text
Motor FL
Motor FR
Motor RL
Motor RR
```

If the current motor telemetry list does not encode physical labels, use index labels:

```text
Motor 1
Motor 2
Motor 3
Motor 4
```

Each stream uses the same base loop sample initially, with individual rate and volume modulation.

Required sound asset:

```text
app/src/main/res/raw/drone_motor_loop.wav
```

Optional assets:

```text
app/src/main/res/raw/drone_motor_rough_loop.wav
app/src/main/res/raw/drone_warning_beep.wav
app/src/main/res/raw/drone_motor_sputter.wav
```

For V2, one high-quality seamless mono loop is acceptable. Additional assets improve quality but must not block implementation.

### 5.2 Arming and disarming

If `state.armed == false` and sound test mode is false:

- all motor streams fade to silence,
- active motor count reads `0/4`,
- sound engine remains allocated if feature is enabled,
- no motor loop should be audible.

Fade timing:

- fade in: `250 ms`,
- fade out: `180 ms`.

Avoid clicks/pops.

### 5.3 RPM mapping

Constants:

```kotlin
private const val MaxReferenceRpm = 9500f
private const val MinimumAudibleRpm = 80f
private const val IdleRate = 0.55f
private const val MaxRate = 2.0f
```

Derived:

```kotlin
rpmNorm = (rpm / MaxReferenceRpm).coerceIn(0f, 1f)
playbackRate = IdleRate + (MaxRate - IdleRate) * rpmNorm
```

If `rpm < MinimumAudibleRpm`, motor volume should fade to `0f`.

### 5.4 Volume mapping

For each motor:

```kotlin
rpmNorm = (rpm / MaxReferenceRpm).coerceIn(0f, 1f)
commandNorm = command.coerceIn(0f, 1f)
baseMotorGain = 0.12f + 0.68f * rpmNorm + 0.20f * commandNorm
```

Then apply:

```kotlin
motorGain = baseMotorGain * masterVolume * enabledGain * failureGain * testOrArmedGate
```

Where:

- `enabledGain = 1f` if sound enabled, else smoothly approaches `0f`.
- `failureGain = 0f..1f` depending on motor failure behavior.
- `testOrArmedGate = 1f` if armed or sound test mode, else `0f`.

Clamp final stream gain to `0f..1f`.

### 5.5 Per-motor mix behavior

Compute average healthy RPM:

```kotlin
avgHealthyRpm = healthyMotors.averageOf { it.rpm }
```

For each motor, mix its own RPM with the average RPM:

```kotlin
mixedRpm = avgHealthyRpm * (1f - perMotorMix) + motor.rpm * perMotorMix
```

Use `mixedRpm` for playback rate.

This gives a useful tuning behavior:

- low per-motor mix = smoother drone body sound,
- high per-motor mix = more audible motor differences.

### 5.6 Roughness behavior

Roughness should increase audible instability without affecting physics.

Inputs:

- user roughness slider,
- RPM spread,
- angular rate,
- wind gusts,
- descent rate,
- motor failures.

Suggested normalized sources:

```kotlin
rpmSpreadNorm = (stdDevRpm / avgRpm).coerceIn(0f, 0.35f) / 0.35f
angularRateNorm = ((abs(rollSpeedRadS) + abs(pitchSpeedRadS) + abs(yawSpeedRadS)) / 6f).coerceIn(0f, 1f)
windGustNorm = (failures.windGustsMs / 10f).coerceIn(0f, 1f)
descentNorm = (-state.verticalSpeedMS / 4f).coerceIn(0f, 1f)
failureNorm = if (anyMotorFailed) 1f else 0f
```

Combined:

```kotlin
computedRoughness = (
    0.35f * rpmSpreadNorm +
    0.20f * angularRateNorm +
    0.15f * windGustNorm +
    0.15f * descentNorm +
    0.15f * failureNorm
).coerceIn(0f, 1f)

finalRoughness = (userRoughness * computedRoughness).coerceIn(0f, 1f)
```

How to express roughness in SoundPool V2:

- small random rate modulation per motor,
- subtle volume tremolo,
- stronger fluctuation during failure,
- optional rough loop layer crossfade if `drone_motor_rough_loop.wav` exists.

Example rate modulation:

```kotlin
rateJitter = 1f + randomSigned() * 0.025f * finalRoughness
finalRate = playbackRate * rateJitter
```

Do not make roughness cartoonish. It should feel like mechanical instability, not a siren.

### 5.7 Motor failure behavior

If `MotorTelemetry.failed == true`:

V2 default behavior:

- fade that motor stream down to `0f` over `250 ms`,
- increase roughness for remaining motors,
- if `drone_motor_sputter.wav` exists, play short sputter cue once at failure transition.

Alternative optional mode later:

- failed motor produces intermittent sputter loop instead of silence.

Failure should be obvious but not painfully loud.

### 5.8 Battery/load behavior

Use these as subtle sound modifiers:

- Higher current draw: slightly louder/brighter.
- Lower battery: optional slight roughness increase.
- Payload mass: slightly increased gain and roughness under climb.

Suggested V2 behavior:

```kotlin
currentNorm = (state.batteryCurrentCa / 2500f).coerceIn(0f, 1f)
payloadNorm = (failures.payloadMassKg / 4f).coerceIn(0f, 1f)
climbNorm = (state.verticalSpeedMS / 4f).coerceIn(0f, 1f)
loadGain = 1f + 0.08f * currentNorm + 0.06f * payloadNorm * climbNorm
```

Apply `loadGain` carefully and clamp output to prevent clipping.

### 5.9 Warning cues

If alerts enabled, V2 should support at least:

1. Low battery warning:
   - Trigger if `batteryRemainingPercent <= 30`.
   - Do not beep continuously every frame.
   - Beep cadence: one short beep every `5 seconds` at 30% or below.

2. Critical battery warning:
   - Trigger if `batteryRemainingPercent <= 15`.
   - Beep cadence: two short beeps every `3 seconds`.

Optional V2 stretch cues:

3. Link lost:
   - if `failures.lostLinkActive == true`, slow warning pulse.

4. Unsafe mission reserve:
   - if `failures.unsafeMissionReserveActive == true`, short warning chirp.

Warning cues must respect:

- sound enabled,
- alerts enabled,
- master volume.

---

## 6. Data model

Create:

```text
app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundSettings.kt
```

Suggested model:

```kotlin
data class DroneSoundSettings(
    val enabled: Boolean = true,
    val masterVolume: Float = 0.65f,
    val perMotorMix: Float = 0.75f,
    val roughness: Float = 0.45f,
    val alertsEnabled: Boolean = true,
    val testMode: Boolean = false,
    val testRpm: Float = 3500f,
)
```

Create:

```text
app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundFrame.kt
```

Suggested model:

```kotlin
data class DroneSoundFrame(
    val enabled: Boolean,
    val motors: List<MotorSoundFrame>,
    val averageRpm: Float,
    val rpmSpreadPercent: Float,
    val activeMotorCount: Int,
    val roughness: Float,
    val alert: DroneSoundAlert,
)

data class MotorSoundFrame(
    val index: Int,
    val rpm: Float,
    val volume: Float,
    val playbackRate: Float,
    val failed: Boolean,
)

enum class DroneSoundAlert {
    NONE,
    LOW_BATTERY,
    CRITICAL_BATTERY,
    LINK_LOST,
    UNSAFE_RESERVE,
}
```

Create:

```text
app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundDebugState.kt
```

Suggested model:

```kotlin
data class DroneSoundDebugState(
    val averageRpm: Float = 0f,
    val rpmSpreadPercent: Float = 0f,
    val activeMotorCount: Int = 0,
    val averagePlaybackRate: Float = 0f,
    val roughness: Float = 0f,
    val alertLabel: String = "None",
)
```

---

## 7. Core sound model

Create:

```text
app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundModel.kt
```

Responsibility:

- pure Kotlin,
- no Android imports,
- deterministic enough for unit tests,
- converts `DroneState + FailureState + DroneSoundSettings` into `DroneSoundFrame`.

Suggested function:

```kotlin
object DroneSoundModel {
    fun compute(
        state: DroneState,
        failures: FailureState,
        settings: DroneSoundSettings,
    ): DroneSoundFrame
}
```

Rules:

1. If sound disabled, all volumes are `0f`.
2. If disarmed and not test mode, all motor volumes are `0f`.
3. If test mode, synthesize four virtual motors using `settings.testRpm`.
4. Failed motors get `volume = 0f` by default.
5. Healthy motors use RPM-driven volume and rate.
6. Alert state is computed but actual beep cadence belongs in controller/player layer.
7. No SoundPool calls inside the model.

---

## 8. Audio controller

Create:

```text
app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundController.kt
```

Responsibilities:

- owns `SoundPool`,
- loads raw audio assets,
- observes `state`, `failures`, and settings,
- computes `DroneSoundFrame`,
- updates motor stream rates and volumes,
- applies fades/smoothing,
- exposes debug state for Controller UI,
- releases audio resources.

Suggested constructor:

```kotlin
class DroneSoundController(
    context: Context,
    private val state: StateFlow<DroneState>,
    private val failures: StateFlow<FailureState>,
    private val scope: CoroutineScope,
)
```

Suggested public API:

```kotlin
val settings: StateFlow<DroneSoundSettings>
val debugState: StateFlow<DroneSoundDebugState>

fun start()
fun stop()
fun release()
fun updateSettings(transform: (DroneSoundSettings) -> DroneSoundSettings)
fun resetSettings()
```

Update frequency:

```text
20–30 Hz
```

Do not call `SoundPool.setRate()` and `SoundPool.setVolume()` at full physics tick rate if not needed.

### SoundPool setup

Use:

```kotlin
AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_GAME)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()
```

Then:

```kotlin
SoundPool.Builder()
    .setMaxStreams(8)
    .setAudioAttributes(audioAttributes)
    .build()
```

Stream budget:

- 4 motor loops,
- 1 optional rough loop,
- 1 warning cue,
- spare streams for future cues.

---

## 9. Persistence

V2 should persist sound settings between app launches if possible.

Preferred simple implementation:

- use SharedPreferences with key prefix `drone_sound_`,
- load settings when `DroneSoundController` is created,
- save when settings change.

Suggested keys:

```text
drone_sound_enabled
drone_sound_master_volume
drone_sound_per_motor_mix
drone_sound_roughness
drone_sound_alerts_enabled
```

Do not persist `testMode` as enabled by default after app restart. It should reset to `false` on startup.

`testRpm` may persist, but test mode itself should not.

---

## 10. Controller UI implementation details

In `ControllerScreen.kt`:

Collect:

```kotlin
val soundSettings by AppRuntime.soundSettings.collectAsState()
val soundDebug by AppRuntime.soundDebugState.collectAsState()
```

Add composable:

```kotlin
DroneSoundLabCard(
    settings = soundSettings,
    debugState = soundDebug,
    onEnabledChange = AppRuntime::setDroneSoundEnabled,
    onMasterVolumeChange = AppRuntime::setDroneSoundMasterVolume,
    onPerMotorMixChange = AppRuntime::setDroneSoundPerMotorMix,
    onRoughnessChange = AppRuntime::setDroneSoundRoughness,
    onAlertsEnabledChange = AppRuntime::setDroneSoundAlertEnabled,
    onTestModeChange = AppRuntime::setDroneSoundTestMode,
    onTestRpmChange = AppRuntime::setDroneSoundTestRpm,
    onReset = AppRuntime::resetDroneSoundSettings,
)
```

Preferred location:

- after `AdvancedTestInputs`,
- before the selected input mode controls,
- or immediately after Direct RPM controls if you want it visually tied to sound testing.

Recommended placement for discoverability:

```text
Title
Description
Sensor status
InputModeSelector
AdvancedTestInputs
DroneSoundLabCard
Selected Controller mode controls
FlightModeSelector
Arm / Takeoff / Land
```

The card should remain visible regardless of input mode.

---

## 11. Testing plan

### 11.1 Unit tests

Create:

```text
app/src/test/java/com/ascend/mavlab/simulation/audio/DroneSoundModelTest.kt
```

Required test cases:

1. `disabledSoundProducesSilentMotors`
   - settings enabled false,
   - expect all motor volumes 0.

2. `disarmedDroneProducesSilentMotorsWhenNotInTestMode`
   - state armed false,
   - test mode false,
   - expect all motor volumes 0.

3. `testModeProducesAudibleVirtualMotorsWhenDisarmed`
   - state armed false,
   - test mode true,
   - test RPM 3500,
   - expect motor volume > 0 and playback rate > idle.

4. `higherRpmIncreasesPlaybackRate`
   - compare 2000 RPM and 8000 RPM,
   - expect 8000 RPM frame has higher average playback rate.

5. `failedMotorIsSilent`
   - one `MotorTelemetry.failed = true`,
   - expect that motor’s volume is 0.

6. `rpmSpreadIncreasesRoughness`
   - compare equal RPM motors vs spread RPM motors,
   - expect roughness higher for spread RPM.

7. `lowBatteryProducesLowBatteryAlert`
   - battery percent 25,
   - expect `DroneSoundAlert.LOW_BATTERY`.

8. `criticalBatteryOverridesLowBatteryAlert`
   - battery percent 10,
   - expect `DroneSoundAlert.CRITICAL_BATTERY`.

9. `unsafeReserveProducesAlertWhenBatteryHealthy`
   - battery healthy,
   - unsafe reserve true,
   - expect unsafe reserve alert.

10. `perMotorMixChangesIndividualRates`
    - uneven motor RPMs,
    - compare perMotorMix 0 vs 1,
    - expect rates more different at 1.

Run:

```bash
./gradlew testDebugUnitTest
```

### 11.2 Manual tests

1. App launches without sound crash.
2. Controller tab shows Drone Sound Lab card.
3. Sound enabled toggle mutes/unmutes audio.
4. Master volume changes loudness.
5. Direct RPM mode changes pitch smoothly.
6. Arm/disarm fades sound in/out.
7. Test mode plays sound while disarmed and does not affect simulation telemetry.
8. Motor failure makes sound rougher/asymmetric.
9. Low battery warning plays only at cadence, not continuously.
10. Background/foreground does not leak or duplicate audio streams.
11. Switching tabs does not stop sound.
12. Stopping the service releases audio resources.

### 11.3 Device tests

Test on:

- built-in phone speaker,
- wired headphones if available,
- Bluetooth headphones,
- emulator only for crash checks, not audio quality judgement.

Bluetooth latency is acceptable for ambience but should not be treated as a precise training signal.

---

## 12. Acceptance criteria

V2 is accepted when:

1. `Controller` tab contains a working `Drone Sound Lab` card.
2. Sound can be enabled/disabled from Controller.
3. Master volume works.
4. Per-motor mix works.
5. Roughness slider audibly affects instability without changing physics.
6. Test mode works while disarmed and does not alter `DroneState`.
7. Direct RPM mode changes sound pitch and loudness in real time.
8. Four motor streams are updated from `DroneState.motors`.
9. Failed motor telemetry causes that motor’s sound to disappear or sputter.
10. Low/critical battery warning cues work when alerts are enabled.
11. AppRuntime owns audio lifecycle.
12. Sound continues across tab switches.
13. `DroneSoundModelTest` covers the core mapping logic.
14. `./gradlew testDebugUnitTest` passes.
15. No audio resources leak after service stop/app close.

---

## 13. Implementation tasks

### Task 1: Create audio data models

Files:

- Create: `app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundSettings.kt`
- Create: `app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundFrame.kt`
- Create: `app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundDebugState.kt`

Goal:

- Establish stable types for settings, computed audio frames, and UI debug readouts.

Verification:

```bash
./gradlew testDebugUnitTest
```

Expected:

- Existing tests still pass.

### Task 2: Implement pure DroneSoundModel

Files:

- Create: `app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundModel.kt`
- Create: `app/src/test/java/com/ascend/mavlab/simulation/audio/DroneSoundModelTest.kt`

Goal:

- Convert simulation state into testable sound parameters before touching Android audio APIs.

Verification:

```bash
./gradlew testDebugUnitTest --tests "com.ascend.mavlab.simulation.audio.DroneSoundModelTest"
```

Expected:

- New sound model tests pass.

### Task 3: Add raw sound assets

Files:

- Create: `app/src/main/res/raw/drone_motor_loop.wav`
- Optional: `app/src/main/res/raw/drone_warning_beep.wav`
- Optional: `app/src/main/res/raw/drone_motor_sputter.wav`

Goal:

- Provide loop/cue assets for SoundPool.

Asset rules:

- Use short seamless WAV/OGG loops.
- Prefer mono.
- Keep files small.
- Avoid harsh clipping.
- Do not use unlicensed commercial audio.

Verification:

```bash
./gradlew assembleDebug
```

Expected:

- Resources compile successfully.

### Task 4: Implement DroneSoundController

Files:

- Create: `app/src/main/java/com/ascend/mavlab/simulation/audio/DroneSoundController.kt`

Goal:

- Own SoundPool, load assets, observe state/settings, and update streams at 20–30 Hz.

Verification:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Expected:

- Tests pass and debug APK builds.

### Task 5: Wire audio lifecycle into AppRuntime

Files:

- Modify: `app/src/main/java/com/ascend/mavlab/core/common/AppRuntime.kt`

Goal:

- Start sound controller in `AppRuntime.start(context)`.
- Stop/release in `AppRuntime.stop()`.
- Expose settings/debug state and setter functions to UI.

Verification:

```bash
./gradlew testDebugUnitTest
```

Expected:

- Existing runtime tests pass.

Manual verification:

- Launch app.
- Switch tabs.
- Sound does not duplicate.
- Stop app/service.
- Sound stops.

### Task 6: Add Controller Sound Lab UI

Files:

- Modify: `app/src/main/java/com/ascend/mavlab/feature/controller/ControllerScreen.kt`

Goal:

- Add the requested Controller-tab sound settings and live readouts.

Verification:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Manual verification:

- Drone Sound Lab card appears.
- Controls update AppRuntime settings.
- Debug readouts respond to RPM.

### Task 7: Add persistence

Files:

- Modify or create under: `app/src/main/java/com/ascend/mavlab/simulation/audio/`

Goal:

- Persist sound settings except `testMode`.

Verification:

- Change master volume.
- Restart app.
- Volume persists.
- Test mode resets to off.

### Task 8: Manual QA and tuning

Files:

- No required code files unless tuning constants need adjustment.

Goal:

- Tune rate, volume, roughness, fade, and warning cadence on a real phone.

Verification:

- Direct RPM sweep from 0 to 9500 RPM sounds smooth.
- No clicks/pops during arm/disarm.
- No clipping at max volume.
- Failed motor is audible as an event/change.

---

## 14. Design guardrails

1. Controller UI controls settings only. It must not directly play audio except through AppRuntime/sound controller APIs.
2. Default sound source is simulation state, not UI slider state.
3. Test mode must be explicitly audio-only.
4. Sound must not affect physics.
5. Sound must not require Android microphone permission.
6. Sound must not block MAVLink, simulation, or UI threads.
7. SoundPool updates should be throttled to 20–30 Hz.
8. Avoid memory leaks: always release SoundPool.
9. Avoid adding heavyweight middleware for V2.
10. Keep the implementation replaceable so V3 can add procedural synthesis later.

---

## 15. Suggested reference labels

Use these user-facing labels exactly unless there is a strong design reason to change them:

- `Drone Sound Lab`
- `Sound enabled`
- `Master volume`
- `Per-motor mix`
- `Roughness`
- `Alerts enabled`
- `Sound test mode`
- `Test RPM`
- `Reset sound`
- `Audio-only test. Does not affect simulation state.`

---

## 16. Future handoff notes

This spec intentionally defines V2 as a SoundPool/per-motor adaptive system. Future versions should not rewrite V2 prematurely. The key architectural decision is to separate:

```text
DroneSoundModel: pure simulation-to-audio parameters
DroneSoundController: Android audio playback
ControllerScreen: user controls and debug display
AppRuntime: lifecycle and stable app-level API
```

That separation makes it possible to replace SoundPool with AudioTrack or Oboe later without rewriting the Controller UI or simulation model.
