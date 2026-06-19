package com.ascend.mavlab.simulation.audio

import com.ascend.mavlab.simulation.engine.DroneState
import com.ascend.mavlab.simulation.engine.MotorTelemetry
import com.ascend.mavlab.simulation.failures.FailureState
import kotlin.math.abs
import kotlin.math.sqrt

object DroneSoundModel {
    fun compute(
        state: DroneState,
        failures: FailureState,
        settings: DroneSoundSettings,
    ): DroneSoundFrame {
        val safeSettings = settings.sanitized()
        val sourceMotors = if (safeSettings.testMode) {
            List(4) {
                MotorTelemetry(
                    rpm = safeSettings.testRpm,
                    command = safeSettings.testRpm / MaxReferenceRpm,
                    failed = false,
                )
            }
        } else {
            state.motors.withFallbackMotors()
        }
        val gate = if (state.armed || safeSettings.testMode) 1f else 0f
        val enabledGain = if (safeSettings.enabled) 1f else 0f
        val healthyRpms = sourceMotors
            .filterNot { it.failed }
            .map { it.rpm.coerceAtLeast(0f) }
        val averageHealthyRpm = healthyRpms
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toFloat()
            ?: 0f
        val averageAllRpm = sourceMotors.map { it.rpm.coerceAtLeast(0f) }.average().toFloat()
        val stdDev = rpmStdDev(sourceMotors.map { it.rpm.coerceAtLeast(0f) }, averageAllRpm)
        val rpmSpreadPercent = if (averageAllRpm > 1f) {
            (stdDev / averageAllRpm * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }
        val finalRoughness = computeRoughness(
            state = state,
            failures = failures,
            settings = safeSettings,
            averageRpm = averageAllRpm,
            stdDevRpm = stdDev,
            anyMotorFailed = sourceMotors.any { it.failed },
        )
        val loadGain = loadGain(state, failures)
        val motors = sourceMotors.mapIndexed { index, motor ->
            val motorRpm = motor.rpm.coerceAtLeast(0f)
            val mixedRpm = averageHealthyRpm * (1f - safeSettings.perMotorMix) + motorRpm * safeSettings.perMotorMix
            val rpmNorm = (motorRpm / MaxReferenceRpm).coerceIn(0f, 1f)
            val commandNorm = motor.command.coerceIn(0f, 1f)
            val baseMotorGain = if (motorRpm < MinimumAudibleRpm) {
                0f
            } else {
                0.12f + 0.68f * rpmNorm + 0.20f * commandNorm
            }
            val failureGain = if (motor.failed) 0f else 1f
            val volume = (baseMotorGain * safeSettings.masterVolume * enabledGain * failureGain * gate * loadGain)
                .coerceIn(0f, 1f)
            MotorSoundFrame(
                index = index,
                rpm = motorRpm,
                volume = volume,
                playbackRate = playbackRate(mixedRpm),
                failed = motor.failed,
            )
        }
        val audibleMotors = motors.count { it.volume > 0.001f }
        val averagePlaybackRate = motors
            .takeIf { it.isNotEmpty() }
            ?.map { it.playbackRate }
            ?.average()
            ?.toFloat()
            ?: 0f
        val procedural = computeProceduralFrame(
            state = state,
            failures = failures,
            settings = safeSettings,
            motors = sourceMotors,
            averageRpm = averageAllRpm,
            roughness = finalRoughness,
            gate = gate,
            enabledGain = enabledGain,
        )

        return DroneSoundFrame(
            enabled = safeSettings.enabled,
            motors = motors,
            averageRpm = if (gate > 0f) averageAllRpm else 0f,
            rpmSpreadPercent = rpmSpreadPercent,
            activeMotorCount = audibleMotors,
            averagePlaybackRate = if (gate > 0f && enabledGain > 0f) averagePlaybackRate else 0f,
            roughness = if (gate > 0f && enabledGain > 0f) finalRoughness else 0f,
            alert = computeAlert(state, failures, safeSettings),
            procedural = procedural,
        )
    }

    private fun computeProceduralFrame(
        state: DroneState,
        failures: FailureState,
        settings: DroneSoundSettings,
        motors: List<MotorTelemetry>,
        averageRpm: Float,
        roughness: Float,
        gate: Float,
        enabledGain: Float,
    ): ProceduralSoundFrame {
        if (!settings.enabled || !settings.proceduralEnabled || gate <= 0f || enabledGain <= 0f) {
            return ProceduralSoundFrame.Disabled.copy(sampleBedAmount = settings.sampleBedAmount)
        }
        val profile = DroneAcousticProfile.byId(settings.acousticProfileId)
        val bladeCount = settings.bladeCount.takeIf { it in 2..4 } ?: profile.bladeCount
        val bpfValues = motors.map { motor ->
            if (motor.failed) 0f else BladePassModel.bladePassHz(motor.rpm, bladeCount)
        }
        val averageBpf = bpfValues.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0f
        val harmonicCount = BladePassModel
            .harmonics(averageBpf, settings.synthQuality.harmonicCount, settings.synthQuality.maxHarmonicFrequencyHz)
            .size
        val rpmNorm = (averageRpm / profile.maxReferenceRpm).coerceIn(0f, 1f)
        val throttleNorm = state.throttlePercent.toInt() / 100f
        val climbNorm = (state.verticalSpeedMS / 4f).coerceIn(0f, 1f)
        val descentNorm = (-state.verticalSpeedMS / 4f).coerceIn(0f, 1f)
        val groundSpeedNorm = (state.groundSpeedMS / 18f).coerceIn(0f, 1f)
        val windGustNorm = (failures.windGustsMs / 10f).coerceIn(0f, 1f)
        val payloadNorm = (failures.payloadMassKg / 4f).coerceIn(0f, 1f)
        val currentNorm = (state.batteryCurrentCa.toFloat() / 2500f).coerceIn(0f, 1f)
        val propWashGain = settings.propWashAmount * profile.propWashBrightness * (
            0.30f * rpmNorm +
                0.25f * throttleNorm +
                0.10f * climbNorm +
                0.15f * descentNorm +
                0.08f * groundSpeedNorm +
                0.07f * windGustNorm +
                0.05f * payloadNorm * currentNorm
            ).coerceIn(0f, 1f)
        val motorWhineGain = settings.motorWhineAmount * profile.whineBrightness * (0.15f + 0.85f * rpmNorm) * 0.18f
        val loadStrain = (payloadNorm * climbNorm * 0.45f + currentNorm * 0.25f).coerceIn(0f, 1f)

        return ProceduralSoundFrame(
            enabled = true,
            bladeCount = bladeCount,
            sampleBedAmount = settings.sampleBedAmount,
            bladeHarmonicsAmount = settings.bladeHarmonicsAmount * profile.harmonicBrightness,
            propWashAmount = settings.propWashAmount,
            motorWhineAmount = settings.motorWhineAmount,
            averageBladePassHz = averageBpf,
            motorBladePassHz = bpfValues,
            harmonicCount = harmonicCount,
            maxHarmonicFrequencyHz = settings.synthQuality.maxHarmonicFrequencyHz,
            propWashGain = propWashGain.coerceIn(0f, 1f),
            motorWhineGain = motorWhineGain.coerceIn(0f, 1f),
            loadStrain = loadStrain,
            roughness = (roughness + loadStrain * 0.12f + descentNorm * 0.08f).coerceIn(0f, 1f),
        )
    }

    private fun computeAlert(
        state: DroneState,
        failures: FailureState,
        settings: DroneSoundSettings,
    ): DroneSoundAlert {
        if (!settings.enabled || !settings.alertsEnabled) return DroneSoundAlert.NONE
        val percent = state.batteryRemainingPercent.toInt()
        return when {
            percent <= 15 -> DroneSoundAlert.CRITICAL_BATTERY
            percent <= 30 -> DroneSoundAlert.LOW_BATTERY
            failures.lostLinkActive -> DroneSoundAlert.LINK_LOST
            failures.unsafeMissionReserveActive -> DroneSoundAlert.UNSAFE_RESERVE
            else -> DroneSoundAlert.NONE
        }
    }

    private fun computeRoughness(
        state: DroneState,
        failures: FailureState,
        settings: DroneSoundSettings,
        averageRpm: Float,
        stdDevRpm: Float,
        anyMotorFailed: Boolean,
    ): Float {
        val rpmSpreadNorm = if (averageRpm > 1f) {
            (stdDevRpm / averageRpm).coerceIn(0f, 0.35f) / 0.35f
        } else {
            0f
        }
        val angularRateNorm = ((abs(state.rollSpeedRadS) + abs(state.pitchSpeedRadS) + abs(state.yawSpeedRadS)) / 6f)
            .coerceIn(0f, 1f)
        val windGustNorm = (failures.windGustsMs / 10f).coerceIn(0f, 1f)
        val descentNorm = (-state.verticalSpeedMS / 4f).coerceIn(0f, 1f)
        val failureNorm = if (anyMotorFailed) 1f else 0f
        val computed = (
            0.35f * rpmSpreadNorm +
                0.20f * angularRateNorm +
                0.15f * windGustNorm +
                0.15f * descentNorm +
                0.15f * failureNorm
            ).coerceIn(0f, 1f)
        return (settings.roughness * computed).coerceIn(0f, 1f)
    }

    private fun loadGain(state: DroneState, failures: FailureState): Float {
        val currentNorm = (state.batteryCurrentCa.toFloat() / 2500f).coerceIn(0f, 1f)
        val payloadNorm = (failures.payloadMassKg / 4f).coerceIn(0f, 1f)
        val climbNorm = (state.verticalSpeedMS / 4f).coerceIn(0f, 1f)
        return (1f + 0.08f * currentNorm + 0.06f * payloadNorm * climbNorm).coerceIn(1f, 1.18f)
    }

    private fun playbackRate(rpm: Float): Float {
        val rpmNorm = (rpm / MaxReferenceRpm).coerceIn(0f, 1f)
        return IdleRate + (MaxRate - IdleRate) * rpmNorm
    }

    private fun rpmStdDev(values: List<Float>, average: Float): Float {
        if (values.isEmpty()) return 0f
        val variance = values.sumOf { value ->
            val diff = value - average
            (diff * diff).toDouble()
        } / values.size.toDouble()
        return sqrt(variance).toFloat()
    }

    private fun List<MotorTelemetry>.withFallbackMotors(): List<MotorTelemetry> {
        if (isEmpty()) return List(4) { MotorTelemetry() }
        return if (size >= 4) take(4) else this + List(4 - size) { MotorTelemetry() }
    }

    private const val MaxReferenceRpm = DroneSoundSettings.MaxReferenceRpm
    private const val MinimumAudibleRpm = 80f
    private const val IdleRate = 0.55f
    private const val MaxRate = 2.0f
}
