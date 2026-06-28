package com.ascend.mavlab.simulation.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.ascend.mavlab.R
import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.failures.FailureState
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DroneSoundController(
    context: Context,
    private val state: StateFlow<DroneState>,
    private val failures: StateFlow<FailureState>,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
    private val proceduralSynth = ProceduralDroneSynth(
        sampleRateHz = preferredSampleRateHz(appContext),
        bufferFrames = preferredBufferFrames(appContext),
    )
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private var soundPool: SoundPool? = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(audioAttributes)
        .build()
    private val mutableSettings = MutableStateFlow(loadSettings())
    private val mutableDebugState = MutableStateFlow(DroneSoundDebugState())
    private val motorStreamIds = IntArray(MotorCount)
    private val motorVolumes = FloatArray(MotorCount)
    private val motorRates = FloatArray(MotorCount) { 0.55f }
    private var motorSampleId = 0
    private var warningSampleId = 0
    private var loadedSampleCount = 0
    private var samplesLoaded = false
    private var updateJob: Job? = null
    private var lastAlertAtMs = 0L
    private var lastAlert = DroneSoundAlert.NONE

    val settings: StateFlow<DroneSoundSettings> = mutableSettings.asStateFlow()
    val debugState: StateFlow<DroneSoundDebugState> = mutableDebugState.asStateFlow()

    init {
        soundPool?.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                loadedSampleCount += 1
                samplesLoaded = loadedSampleCount >= RequiredSampleCount
            }
        }
        motorSampleId = soundPool?.load(appContext, R.raw.drone_motor_loop, 1) ?: 0
        warningSampleId = soundPool?.load(appContext, R.raw.drone_warning_beep, 1) ?: 0
    }

    fun start() {
        if (updateJob != null) return
        proceduralSynth.start()
        updateJob = scope.launch {
            while (isActive) {
                updateAudioFrame()
                delay(UpdateIntervalMs)
            }
        }
    }

    fun stop() {
        updateJob?.cancel()
        updateJob = null
        proceduralSynth.stop()
        stopStreams()
        mutableDebugState.value = DroneSoundDebugState()
    }

    fun release() {
        stop()
        proceduralSynth.release()
        soundPool?.release()
        soundPool = null
        samplesLoaded = false
    }

    fun updateSettings(transform: (DroneSoundSettings) -> DroneSoundSettings) {
        val next = transform(mutableSettings.value).sanitized()
        mutableSettings.value = next
        persistSettings(next)
    }

    fun resetSettings() {
        val reset = DroneSoundSettings()
        mutableSettings.value = reset
        persistSettings(reset)
    }

    private fun updateAudioFrame() {
        val frame = DroneSoundModel.compute(
            state = state.value,
            failures = failures.value,
            settings = mutableSettings.value,
        )
        proceduralSynth.setMasterVolume(mutableSettings.value.masterVolume)
        proceduralSynth.setFrame(frame.procedural)
        mutableDebugState.value = DroneSoundDebugState.fromFrame(frame, proceduralSynth.status.value)
        if (!samplesLoaded || motorSampleId == 0) return
        val sampleBedGain = if (frame.procedural.enabled) frame.procedural.sampleBedAmount else 1f

        frame.motors.forEach { motor ->
            val index = motor.index
            if (index !in 0 until MotorCount) return@forEach
            val targetVolume = motor.volume * sampleBedGain
            motorVolumes[index] = smoothVolume(motorVolumes[index], targetVolume)
            val targetRate = motor.playbackRate.coerceIn(MinPlaybackRate, MaxPlaybackRate)
            if (motorStreamIds[index] == 0 && motorVolumes[index] > 0.001f) {
                motorRates[index] = targetRate
                motorStreamIds[index] = soundPool?.play(
                    motorSampleId,
                    0f,
                    0f,
                    1,
                    -1,
                    targetRate,
                ) ?: 0
            }
            val streamId = motorStreamIds[index]
            if (streamId != 0) {
                motorRates[index] = smoothRate(motorRates[index], targetRate)
                val roughJitter = roughRateJitter(index, frame.roughness)
                val rate = (motorRates[index] * roughJitter).coerceIn(MinPlaybackRate, MaxPlaybackRate)
                soundPool?.setRate(streamId, rate)
                soundPool?.setVolume(streamId, motorVolumes[index], motorVolumes[index])
            }
        }
        maybePlayAlert(frame.alert)
    }

    private fun maybePlayAlert(alert: DroneSoundAlert) {
        val pool = soundPool ?: return
        if (warningSampleId == 0 || alert == DroneSoundAlert.NONE) {
            lastAlert = alert
            return
        }
        val now = System.currentTimeMillis()
        val cadence = when (alert) {
            DroneSoundAlert.CRITICAL_BATTERY -> 3000L
            DroneSoundAlert.LOW_BATTERY -> 5000L
            DroneSoundAlert.LINK_LOST -> 5500L
            DroneSoundAlert.UNSAFE_RESERVE -> 6500L
            DroneSoundAlert.NONE -> Long.MAX_VALUE
        }
        if (alert != lastAlert || now - lastAlertAtMs >= cadence) {
            val volume = (mutableSettings.value.masterVolume * AlertGain).coerceIn(0f, 1f)
            pool.play(warningSampleId, volume, volume, 2, 0, 1f)
            if (alert == DroneSoundAlert.CRITICAL_BATTERY) {
                scope.launch {
                    delay(CriticalSecondBeepDelayMs)
                    soundPool?.play(warningSampleId, volume, volume, 2, 0, 1f)
                }
            }
            lastAlertAtMs = now
            lastAlert = alert
        }
    }

    private fun stopStreams() {
        val pool = soundPool ?: return
        motorStreamIds.forEachIndexed { index, streamId ->
            if (streamId != 0) {
                pool.stop(streamId)
                motorStreamIds[index] = 0
                motorVolumes[index] = 0f
            }
        }
    }

    private fun smoothVolume(current: Float, target: Float): Float {
        val coefficient = if (target > current) FadeInCoefficient else FadeOutCoefficient
        return (current + (target - current) * coefficient).coerceIn(0f, 1f)
    }

    private fun smoothRate(current: Float, target: Float): Float {
        return (current + (target - current) * 0.35f).coerceIn(MinPlaybackRate, MaxPlaybackRate)
    }

    private fun roughRateJitter(index: Int, roughness: Float): Float {
        if (roughness <= 0.001f) return 1f
        val phase = System.nanoTime() / 120_000_000.0 + index * 1.7
        return (1f + sin(phase).toFloat() * 0.025f * roughness).coerceIn(0.96f, 1.04f)
    }

    private fun loadSettings(): DroneSoundSettings {
        return DroneSoundSettings(
            enabled = prefs.getBoolean(KeyEnabled, true),
            masterVolume = prefs.getFloat(KeyMasterVolume, 0.65f),
            perMotorMix = prefs.getFloat(KeyPerMotorMix, 0.75f),
            roughness = prefs.getFloat(KeyRoughness, 0.45f),
            alertsEnabled = prefs.getBoolean(KeyAlertsEnabled, true),
            testMode = false,
            testRpm = prefs.getFloat(KeyTestRpm, 3500f),
            proceduralEnabled = prefs.getBoolean(KeyProceduralEnabled, true),
            sampleBedAmount = prefs.getFloat(KeySampleBedAmount, 0.55f),
            bladeHarmonicsAmount = prefs.getFloat(KeyBladeHarmonicsAmount, 0.60f),
            propWashAmount = prefs.getFloat(KeyPropWashAmount, 0.50f),
            motorWhineAmount = prefs.getFloat(KeyMotorWhineAmount, 0.25f),
            bladeCount = prefs.getInt(KeyBladeCount, 2),
            acousticProfileId = prefs.getString(KeyAcousticProfileId, DroneAcousticProfile.DefaultId) ?: DroneAcousticProfile.DefaultId,
            synthQuality = runCatching {
                DroneSynthQuality.valueOf(prefs.getString(KeySynthQuality, DroneSynthQuality.BALANCED.name) ?: DroneSynthQuality.BALANCED.name)
            }.getOrDefault(DroneSynthQuality.BALANCED),
            showAcousticTelemetry = prefs.getBoolean(KeyShowAcousticTelemetry, false),
        ).sanitized()
    }

    private fun persistSettings(settings: DroneSoundSettings) {
        prefs.edit()
            .putBoolean(KeyEnabled, settings.enabled)
            .putFloat(KeyMasterVolume, settings.masterVolume)
            .putFloat(KeyPerMotorMix, settings.perMotorMix)
            .putFloat(KeyRoughness, settings.roughness)
            .putBoolean(KeyAlertsEnabled, settings.alertsEnabled)
            .putFloat(KeyTestRpm, settings.testRpm)
            .putBoolean(KeyProceduralEnabled, settings.proceduralEnabled)
            .putFloat(KeySampleBedAmount, settings.sampleBedAmount)
            .putFloat(KeyBladeHarmonicsAmount, settings.bladeHarmonicsAmount)
            .putFloat(KeyPropWashAmount, settings.propWashAmount)
            .putFloat(KeyMotorWhineAmount, settings.motorWhineAmount)
            .putInt(KeyBladeCount, settings.bladeCount)
            .putString(KeyAcousticProfileId, settings.acousticProfileId)
            .putString(KeySynthQuality, settings.synthQuality.name)
            .putBoolean(KeyShowAcousticTelemetry, settings.showAcousticTelemetry)
            .apply()
    }

    private fun preferredSampleRateHz(context: Context): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull() ?: 48000
    }

    private fun preferredBufferFrames(context: Context): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull() ?: 192
    }

    private companion object {
        const val MotorCount = 4
        const val UpdateIntervalMs = 40L
        const val MinPlaybackRate = 0.5f
        const val MaxPlaybackRate = 2.0f
        const val FadeInCoefficient = 0.15f
        const val FadeOutCoefficient = 0.22f
        const val AlertGain = 0.48f
        const val CriticalSecondBeepDelayMs = 180L
        const val RequiredSampleCount = 2
        const val PrefsName = "drone_sound"
        const val KeyEnabled = "drone_sound_enabled"
        const val KeyMasterVolume = "drone_sound_master_volume"
        const val KeyPerMotorMix = "drone_sound_per_motor_mix"
        const val KeyRoughness = "drone_sound_roughness"
        const val KeyAlertsEnabled = "drone_sound_alerts_enabled"
        const val KeyTestRpm = "drone_sound_test_rpm"
        const val KeyProceduralEnabled = "drone_sound_procedural_enabled"
        const val KeySampleBedAmount = "drone_sound_sample_bed_amount"
        const val KeyBladeHarmonicsAmount = "drone_sound_blade_harmonics_amount"
        const val KeyPropWashAmount = "drone_sound_prop_wash_amount"
        const val KeyMotorWhineAmount = "drone_sound_motor_whine_amount"
        const val KeyBladeCount = "drone_sound_blade_count"
        const val KeyAcousticProfileId = "drone_sound_acoustic_profile_id"
        const val KeySynthQuality = "drone_sound_synth_quality"
        const val KeyShowAcousticTelemetry = "drone_sound_show_acoustic_telemetry"
    }
}
