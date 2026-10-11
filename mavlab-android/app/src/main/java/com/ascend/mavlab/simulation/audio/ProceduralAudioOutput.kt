package com.ascend.mavlab.simulation.audio

import android.media.AudioTrack

/** Narrow boundary for deterministic session-lifetime tests without native audio. */
internal interface ProceduralAudioOutput {
    val underrunCount: Int
    fun play()
    fun write(buffer: FloatArray, offset: Int, size: Int): Int
    fun pause()
    fun flush()
    fun release()
}

internal class AndroidProceduralAudioOutput(private val track: AudioTrack) : ProceduralAudioOutput {
    override val underrunCount: Int get() = track.underrunCount
    override fun play() = track.play()
    // Nonblocking writes allow cancellation even if the native output stops consuming.
    // The owner handles zero/partial writes without discarding the remaining samples.
    override fun write(buffer: FloatArray, offset: Int, size: Int): Int =
        track.write(buffer, offset, size, AudioTrack.WRITE_NON_BLOCKING)
    override fun pause() = track.pause()
    override fun flush() = track.flush()
    override fun release() = track.release()
}
