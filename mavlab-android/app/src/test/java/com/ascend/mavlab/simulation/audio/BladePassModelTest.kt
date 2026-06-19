package com.ascend.mavlab.simulation.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BladePassModelTest {
    @Test
    fun computesBladePassFrequencyFromRpmAndBladeCount() {
        assertEquals(200f, BladePassModel.bladePassHz(rpm = 6000f, bladeCount = 2), 0.001f)
        assertEquals(300f, BladePassModel.bladePassHz(rpm = 6000f, bladeCount = 3), 0.001f)
    }

    @Test
    fun clampsInvalidRpmAndBladeCount() {
        assertEquals(0f, BladePassModel.bladePassHz(rpm = -100f, bladeCount = 2), 0.001f)
        assertEquals(100f, BladePassModel.bladePassHz(rpm = 6000f, bladeCount = 0), 0.001f)
    }

    @Test
    fun harmonicsStopAtCountAndFrequencyLimit() {
        assertEquals(
            listOf(200f, 400f, 600f),
            BladePassModel.harmonics(fundamentalHz = 200f, maxCount = 8, maxFrequencyHz = 650f),
        )
        assertEquals(
            listOf(200f, 400f),
            BladePassModel.harmonics(fundamentalHz = 200f, maxCount = 2, maxFrequencyHz = 650f),
        )
    }

    @Test
    fun invalidHarmonicInputsReturnEmptyList() {
        assertTrue(BladePassModel.harmonics(fundamentalHz = 0f, maxCount = 8, maxFrequencyHz = 650f).isEmpty())
        assertTrue(BladePassModel.harmonics(fundamentalHz = 200f, maxCount = 0, maxFrequencyHz = 650f).isEmpty())
        assertTrue(BladePassModel.harmonics(fundamentalHz = 200f, maxCount = 8, maxFrequencyHz = 0f).isEmpty())
    }
}
