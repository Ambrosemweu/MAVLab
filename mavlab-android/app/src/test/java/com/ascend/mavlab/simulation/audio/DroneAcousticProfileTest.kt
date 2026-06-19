package com.ascend.mavlab.simulation.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DroneAcousticProfileTest {
    @Test
    fun byIdReturnsMatchingProfile() {
        assertEquals(
            DroneAcousticProfile.SmallRacingQuad,
            DroneAcousticProfile.byId(DroneAcousticProfile.SmallRacingQuad.id),
        )
    }

    @Test
    fun byIdFallsBackToTrainerProfile() {
        assertEquals(
            DroneAcousticProfile.MavLabTrainerQuad,
            DroneAcousticProfile.byId("missing-profile"),
        )
    }

    @Test
    fun builtInProfilesHaveValidRanges() {
        assertTrue(DroneAcousticProfile.all.isNotEmpty())
        assertEquals(DroneAcousticProfile.all.size, DroneAcousticProfile.all.map { it.id }.toSet().size)
        DroneAcousticProfile.all.forEach { profile ->
            assertTrue(profile.bladeCount in 2..4)
            assertTrue(profile.maxReferenceRpm > 0f)
            assertTrue(profile.harmonicBrightness in 0f..1f)
            assertTrue(profile.propWashBrightness in 0f..1f)
            assertTrue(profile.whineBrightness in 0f..1f)
            assertTrue(profile.sampleBedDefault in 0f..1f)
        }
    }
}
