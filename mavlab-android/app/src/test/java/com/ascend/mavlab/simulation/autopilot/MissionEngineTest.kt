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
    fun loadStartsAtTakeoffAfterQgcHomeMarker() {
        val engine = MissionEngine()

        engine.load(
            listOf(
                missionItem(sequence = 0),
                missionItem(sequence = 1).copy(command = MissionCommand.TAKEOFF),
                missionItem(sequence = 2),
            ),
        )

        assertEquals(1, engine.progress.value.currentIndex)
        assertEquals(MissionCommand.TAKEOFF, engine.progress.value.activeTarget?.command)
    }

    @Test
    fun loadDoesNotSkipFirstWaypointWhenNoTakeoffFollowsHomeMarker() {
        val engine = MissionEngine()

        engine.load(
            listOf(
                missionItem(sequence = 0),
                missionItem(sequence = 1),
            ),
        )

        assertEquals(0, engine.progress.value.currentIndex)
        assertEquals(0, engine.progress.value.activeTarget?.sequence)
    }

    @Test
    fun setCurrentRejectsMissingMissionSequence() {
        val engine = MissionEngine()
        engine.load(listOf(missionItem(sequence = 0)))

        val updated = engine.setCurrent(3)

        assertFalse(updated)
        assertEquals(0, engine.progress.value.currentIndex)
    }

    @Test
    fun updateUsesPinnedLocalCoordinatesWhenAvailable() {
        val engine = MissionEngine()
        engine.load(
            listOf(
                missionItem(sequence = 0).copy(
                    latitudeDeg = 0.0,
                    longitudeDeg = 0.0,
                    localNorthMeters = 0f,
                    localEastMeters = 0f,
                    altitudeAglMeters = 0f,
                    acceptanceRadiusMeters = 1f,
                ),
            ),
        )

        val progress = engine.update(com.ascend.mavlab.simulation.engine.DroneState())

        assertTrue(progress.complete)
        assertEquals(0, progress.lastReachedSequence)
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
