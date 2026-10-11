package com.ascend.mavlab.simulation.audio

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProceduralDroneSynthTest {
    private fun CountDownLatch.ready() = assertTrue(await(3, TimeUnit.SECONDS), "Worker did not reach the expected boundary")

    private class Output : ProceduralAudioOutput {
        val played = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val releaseCount = AtomicInteger()
        var read: () -> Int = { 0 }
        var writer: (Int, Int) -> Int = { _, _ -> 0 }
        var playFailure = false
        var pauseFailure = false
        override val underrunCount: Int get() { check(releaseCount.get() == 0); return read() }
        override fun play() { if (playFailure) error("play failed"); played.countDown() }
        override fun write(buffer: FloatArray, offset: Int, size: Int): Int {
            check(releaseCount.get() == 0)
            return writer(offset, size)
        }
        override fun pause() { if (pauseFailure) error("pause failed") }
        override fun flush() = Unit
        override fun release() { releaseCount.incrementAndGet(); closed.countDown() }
    }

    @Test fun releaseWaitsForTheOwnersNativeReadWithoutBlockingTheCaller() {
        val reading = CountDownLatch(1)
        val finishRead = CountDownLatch(1)
        val output = Output().apply {
            writer = { _, size -> size }
            read = { reading.countDown(); check(finishRead.await(3, TimeUnit.SECONDS)); 4 }
        }
        val synth = ProceduralDroneSynth(48_000, 64) { output }
        try {
            synth.start(); reading.ready()
            synth.release() // Must return while the native call is deliberately held.
            assertEquals(0, output.releaseCount.get())
            assertFalse(synth.status.value.running)
        } finally {
            finishRead.countDown(); synth.release()
        }
        output.closed.ready()
        assertEquals(1, output.releaseCount.get())
        assertFalse(synth.status.value.running)
    }

    @Test fun immediateRestartWaitsForOldCleanupAndCannotPublishOldRunningStatus() {
        val reading = CountDownLatch(1)
        val finishRead = CountDownLatch(1)
        val first = Output().apply {
            writer = { _, size -> size }
            read = { reading.countDown(); check(finishRead.await(3, TimeUnit.SECONDS)); 99 }
        }
        val second = Output()
        val creations = AtomicInteger()
        val synth = ProceduralDroneSynth(48_000, 64) {
            if (creations.getAndIncrement() == 0) first else {
                check(first.releaseCount.get() == 1)
                second
            }
        }
        try {
            synth.start(); reading.ready()
            synth.stop(); synth.start()
            assertEquals(1, creations.get())
            assertFalse(synth.status.value.running)
            finishRead.countDown(); first.closed.ready(); second.played.ready()
            assertEquals(2, creations.get())
            assertTrue(synth.status.value.underrunCount != 99)
        } finally {
            finishRead.countDown(); synth.release()
        }
        second.closed.ready()
        assertEquals(1, second.releaseCount.get())
    }

    @Test fun nonblockingPartialAndZeroWritesRetainTheUnwrittenSamples() {
        val offsets = CopyOnWriteArrayList<Int>()
        val fullBuffer = CountDownLatch(1)
        val calls = AtomicInteger()
        val output = Output().apply {
            writer = { offset, size ->
                offsets.add(offset)
                if (calls.getAndIncrement() == 0) 0 else minOf(17, size)
            }
            read = { fullBuffer.countDown(); 2 }
        }
        val synth = ProceduralDroneSynth(48_000, 64) { output }
        try {
            synth.start(); fullBuffer.ready()
            assertEquals(listOf(0, 0, 17, 34, 51), offsets.take(5))
        } finally { synth.release() }
        output.closed.ready()
    }

    @Test fun unavailableOutputReportsFallbackRatherThanThrowing() {
        val attempted = CountDownLatch(1)
        val synth = ProceduralDroneSynth(48_000, 64) { attempted.countDown(); null }
        try {
            synth.start(); attempted.ready()
            awaitFallback(synth, "AudioTrack unavailable")
        } finally { synth.release() }
    }

    @Test fun playFailureStillDisposesTheOutput() {
        val output = Output().apply { playFailure = true }
        val synth = ProceduralDroneSynth(48_000, 64) { output }
        try {
            synth.start(); output.closed.ready()
            awaitFallback(synth, "play failed")
            assertEquals(1, output.releaseCount.get())
        } finally { synth.release() }
    }

    @Test fun readFailureIsContainedAndStopsTheSession() {
        val output = Output().apply { writer = { _, size -> size }; read = { error("native read failed") } }
        val synth = ProceduralDroneSynth(48_000, 64) { output }
        try {
            synth.start(); output.closed.ready()
            awaitFallback(synth, "native read failed")
        } finally { synth.release() }
    }

    @Test fun writeAndPauseFailureCannotSkipRelease() {
        val output = Output().apply { writer = { _, _ -> -6 }; pauseFailure = true }
        val synth = ProceduralDroneSynth(48_000, 64) { output }
        try {
            synth.start(); output.closed.ready()
            awaitFallback(synth, "AudioTrack write failed -6")
            assertEquals(1, output.releaseCount.get())
        } finally { synth.release() }
    }

    @Test fun releasedSynthDoesNotAllocateAnotherTrack() {
        val creations = AtomicInteger()
        val synth = ProceduralDroneSynth(48_000, 64) { creations.incrementAndGet(); Output() }
        synth.release(); synth.release(); synth.start()
        assertEquals(0, creations.get())
        assertFalse(synth.status.value.running)
    }

    private fun awaitFallback(synth: ProceduralDroneSynth, reason: String) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
        while (synth.status.value.fallbackReason != reason && System.nanoTime() < deadline) Thread.yield()
        assertEquals(reason, synth.status.value.fallbackReason)
        assertFalse(synth.status.value.running)
    }
}
