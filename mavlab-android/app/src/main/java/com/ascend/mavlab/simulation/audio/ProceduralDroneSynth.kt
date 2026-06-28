package com.ascend.mavlab.simulation.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ProceduralDroneSynth(
    private val sampleRateHz: Int,
    private val bufferFrames: Int,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableStatus = MutableStateFlow(
        ProceduralSynthStatus(
            sampleRateHz = sampleRateHz,
            bufferFrames = bufferFrames,
        ),
    )
    private val harmonicPhases = Array(MotorCount) { FloatArray(MaxHarmonics) }
    private val whinePhases = FloatArray(2)
    private val renderBuffer = FloatArray(bufferFrames.coerceAtLeast(64))
    private var audioTrack: AudioTrack? = null
    private var renderJob: Job? = null
    @Volatile private var latestFrame: ProceduralSoundFrame = ProceduralSoundFrame.Disabled
    @Volatile private var masterVolume: Float = 0f
    private var noiseState = 0x13579BDF
    private var filteredNoise = 0f

    val status: StateFlow<ProceduralSynthStatus> = mutableStatus.asStateFlow()

    fun start() {
        if (renderJob != null) return
        val track = createAudioTrack()
        if (track == null) {
            mutableStatus.value = mutableStatus.value.copy(
                running = false,
                fallbackReason = "AudioTrack unavailable",
            )
            return
        }
        audioTrack = track
        try {
            track.play()
        } catch (error: RuntimeException) {
            track.release()
            audioTrack = null
            mutableStatus.value = mutableStatus.value.copy(
                running = false,
                fallbackReason = error.localizedMessage ?: "AudioTrack play failed",
            )
            return
        }
        mutableStatus.value = mutableStatus.value.copy(running = true, fallbackReason = null)
        renderJob = scope.launch {
            while (isActive) {
                val activeTrack = audioTrack ?: break
                val frame = latestFrame
                render(frame, renderBuffer)
                val written = activeTrack.write(renderBuffer, 0, renderBuffer.size, AudioTrack.WRITE_BLOCKING)
                if (written < 0) {
                    mutableStatus.value = mutableStatus.value.copy(
                        running = false,
                        fallbackReason = "AudioTrack write failed $written",
                    )
                    break
                }
                mutableStatus.value = mutableStatus.value.copy(
                    running = true,
                    underrunCount = activeTrack.underrunCount,
                    fallbackReason = null,
                )
            }
        }
    }

    fun stop() {
        renderJob?.cancel()
        renderJob = null
        audioTrack?.pause()
        audioTrack?.flush()
        mutableStatus.value = mutableStatus.value.copy(running = false)
    }

    fun release() {
        stop()
        audioTrack?.release()
        audioTrack = null
    }

    fun setFrame(frame: ProceduralSoundFrame) {
        latestFrame = frame
    }

    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
    }

    private fun createAudioTrack(): AudioTrack? {
        val minBytes = AudioTrack.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        if (minBytes <= 0) return null
        val bufferBytes = maxOf(minBytes, renderBuffer.size * FloatBytes * 6)
        return try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRateHz)
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferBytes)
                .build()
        } catch (_: RuntimeException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun render(frame: ProceduralSoundFrame, output: FloatArray) {
        if (!frame.enabled || masterVolume <= 0.001f) {
            output.fill(0f)
            return
        }
        val harmonicGain = frame.bladeHarmonicsAmount * 0.08f
        val propWashGain = frame.propWashGain * 0.32f
        val whineGain = frame.motorWhineGain * 0.35f
        val roughness = frame.roughness
        val motorBpf = frame.motorBladePassHz
        val harmonicCount = frame.harmonicCount.coerceIn(0, MaxHarmonics)
        val maxHarmonicFrequency = frame.maxHarmonicFrequencyHz.coerceAtLeast(0f)
        val whineBase = (1800f + 5200f * rpmNormFromFrame(frame)).coerceIn(600f, 7200f)

        for (sampleIndex in output.indices) {
            var sample = 0f
            for (motorIndex in 0 until minOf(MotorCount, motorBpf.size)) {
                val fundamental = motorBpf[motorIndex]
                if (fundamental <= 0f) continue
                for (harmonicIndex in 1..harmonicCount) {
                    val frequency = fundamental * harmonicIndex
                    if (frequency > maxHarmonicFrequency || frequency >= sampleRateHz * 0.48f) break
                    val phaseIndex = harmonicIndex - 1
                    val phase = harmonicPhases[motorIndex][phaseIndex]
                    val amplitude = harmonicGain / harmonicIndex.pow1p35()
                    sample += sin(phase) * amplitude
                    harmonicPhases[motorIndex][phaseIndex] = advancePhase(phase, frequency)
                }
            }

            val noise = nextNoise()
            val brightness = (0.04f + 0.28f * rpmNormFromFrame(frame) + 0.18f * frame.loadStrain).coerceIn(0.04f, 0.5f)
            filteredNoise += (noise - filteredNoise) * brightness
            val turbulence = 1f + sin(sampleIndex * 0.035f) * roughness * 0.28f
            sample += filteredNoise * propWashGain * turbulence

            sample += whineSample(0, whineBase, whineGain)
            sample += whineSample(1, whineBase * 1.37f, whineGain * 0.45f)

            val tremolo = 1f + sin(sampleIndex * 0.021f) * roughness * 0.08f
            output[sampleIndex] = softLimit(sample * tremolo * masterVolume)
        }
    }

    private fun whineSample(index: Int, frequency: Float, gain: Float): Float {
        val safeFrequency = frequency.coerceIn(80f, sampleRateHz * 0.46f)
        val value = sin(whinePhases[index]) * gain
        whinePhases[index] = advancePhase(whinePhases[index], safeFrequency)
        return value
    }

    private fun rpmNormFromFrame(frame: ProceduralSoundFrame): Float {
        val rpm = if (frame.bladeCount <= 0) 0f else frame.averageBladePassHz * 60f / frame.bladeCount
        return (rpm / DroneSoundSettings.MaxReferenceRpm).coerceIn(0f, 1f)
    }

    private fun advancePhase(phase: Float, frequency: Float): Float {
        var next = phase + TwoPi * frequency / sampleRateHz
        if (next > TwoPi) next %= TwoPi
        return next
    }

    private fun nextNoise(): Float {
        noiseState = noiseState * 1103515245 + 12345
        val normalized = ((noiseState ushr 8) and 0xFFFF) / 32767.5f - 1f
        return normalized.coerceIn(-1f, 1f)
    }

    private fun softLimit(value: Float): Float {
        return value / (1f + abs(value))
    }

    private fun Int.pow1p35(): Float {
        return when (this) {
            1 -> 1f
            2 -> 2.55f
            3 -> 4.41f
            4 -> 6.50f
            5 -> 8.78f
            6 -> 11.20f
            7 -> 13.75f
            8 -> 16.47f
            9 -> 19.31f
            10 -> 22.30f
            11 -> 25.43f
            else -> 28.62f
        }
    }

    private companion object {
        const val MotorCount = 4
        const val MaxHarmonics = 12
        const val FloatBytes = 4
        const val TwoPi = (PI * 2.0).toFloat()
    }
}
