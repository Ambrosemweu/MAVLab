package com.ascend.mavlab.simulation.autopilot

import com.ascend.mavlab.simulation.mission.MissionCommand
import com.ascend.mavlab.simulation.mission.MissionItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MissionEngineTest {
    @Test
    fun setCurrentSelectsExistingMissionSequence() {
        val engine = MissionEngine()
        engine.load(
            listOf(
                missionItem(sequence = 0),
                missionItem(sequence = 1),
                missionItem(sequence = 2),
            ),
        )

        val updated = engine.setCurrent(2)

        assertTrue(updated)
        assertEquals(2, engine.progress.value.currentIndex)
        assertEquals(2, engine.progress.value.activeTarget?.sequence)
    }

    @Test
    fun setCurrentRejectsMissingMissionSequence() {
        val engine = MissionEngine()
        engine.load(listOf(missionItem(sequence = 0)))

        val updated = engine.setCurrent(3)

        assertFalse(updated)
        assertEquals(0, engine.progress.value.currentIndex)
    }

    private fun missionItem(sequence: Int): MissionItem {
        return MissionItem(
            sequence = sequence,
            command = MissionCommand.WAYPOINT,
            latitudeDeg = -1.2921 + sequence * 0.0001,
            longitudeDeg = 36.8219,
            altitudeAglMeters = 10f,
        )
    }
}
