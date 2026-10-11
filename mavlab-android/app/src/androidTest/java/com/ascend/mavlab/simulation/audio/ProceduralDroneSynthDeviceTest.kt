package com.ascend.mavlab.simulation.audio

import android.media.AudioTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class ProceduralDroneSynthDeviceTest {
    @Test fun nativeTrackSurvivesRepeatedStopRestartAndRelease() = runBlocking {
        val tracks = mutableListOf<AudioTrack>()
        val synth = ProceduralDroneSynth(48_000, 256) {
            AudioTrack.Builder()
                .setAudioFormat(android.media.AudioFormat.Builder().setSampleRate(48_000)
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_FLOAT).build())
                .setBufferSizeInBytes(16_384).setTransferMode(AudioTrack.MODE_STREAM).build()
                .also { synchronized(tracks) { tracks.add(it) } }
                .let(::AndroidProceduralAudioOutput)
        }
        try {
            repeat(30) {
                synth.start()
                withTimeout(3_000) { while (!synth.status.value.running) delay(2) }
                delay(5)
                synth.stop()
                assertFalse(synth.status.value.running)
            }
            synth.start()
            withTimeout(3_000) { while (!synth.status.value.running) delay(2) }
        } finally { synth.release() }
        withTimeout(3_000) {
            while (synchronized(tracks) { tracks.any { it.state != AudioTrack.STATE_UNINITIALIZED } }) delay(2)
        }
        assertEquals(31, synchronized(tracks) { tracks.size })
        assertFalse(synth.status.value.running)
        assertNull(synth.status.value.fallbackReason)
    }
}
