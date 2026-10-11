package com.ascend.mavlab.feature.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingPagesTest {
    @Test
    fun v15PagesCoverRequiredFlow() {
        assertEquals(
            listOf(
                "What is MAVLab?",
                "One shared runtime",
                "Four surfaces, one drone",
                "Connect a ground station",
                "Fly with phone tilt",
                "Your first takeoff",
                "Run a mission",
                "Test failures safely",
                "Review and export",
            ),
            OnboardingPages.map { it.title },
        )
    }

    @Test
    fun surfacePageNamesTheFourTabs() {
        val surfacePage = OnboardingPages.first { it.visual == OnboardingVisual.SurfaceGallery }
        assertTrue(!surfacePage.body.contains("Mission"))
        listOf("Cockpit", "Control", "SIM", "Ops").forEach { tab ->
            assertTrue(surfacePage.body.contains(tab), "Missing tab name: $tab")
        }
    }

    @Test
    fun qgcPageIsTheOnlyPageWithQgcAction() {
        val actionPages = OnboardingPages.filter { it.showQGroundControlAction }
        assertEquals(listOf("Connect a ground station"), actionPages.map { it.title })
    }

    @Test
    fun firstPageIsHeroAndLastInvitesTheUserToStartFlying() {
        assertEquals(OnboardingVisual.DroneHero, OnboardingPages.first().visual)
        assertEquals("Start flying", OnboardingPages.last().primaryAction)
    }

    @Test
    fun everyPageUsesADistinctVisual() {
        val visuals = OnboardingPages.map { it.visual }
        assertEquals(visuals.size, visuals.distinct().size)
    }

    @Test
    fun copyMentionsCoreTrainingWorkflow() {
        val copy = OnboardingPages.joinToString(" ") { page ->
            "${page.title} ${page.body}"
        }
        listOf(
            "phone-based drone digital twin",
            "QGroundControl",
            "takeoff",
            "tilt",
            "mission",
            "failure",
            "export",
        ).forEach { required ->
            assertTrue(copy.contains(required, ignoreCase = true), "Missing onboarding topic: $required")
        }
    }

    @Test
    fun recordingIsExplicitlyOptInAndNeverPromisedForEveryFlight() {
        val page = OnboardingPages.last()
        assertTrue(page.body.contains("off by default"))
        assertTrue(page.body.contains("Ops"))
        assertTrue(page.body.contains("Stop and save"))
        assertTrue(!page.body.contains("Every flight"))
    }

    @Test
    fun calibrationPrecedesFlightAndLandingPrecedesDisarm() {
        val tilt = OnboardingPages.indexOfFirst { it.visual == OnboardingVisual.TiltPad }
        val flight = OnboardingPages.indexOfFirst { it.visual == OnboardingVisual.ArmTakeoffLand }
        assertTrue(tilt < flight)
        assertTrue(OnboardingPages[tilt].body.contains("Calibrate in Control before arming"))
        assertTrue(OnboardingPages[flight].body.contains("Land before Disarm"))
    }

    @Test
    fun optionalGcsAndIllustrationsCannotClaimLiveState() {
        assertTrue(OnboardingPages.first { it.visual == OnboardingVisual.QgcLink }.body.contains("optional"))
        assertTrue(OnboardingPages.all { it.previewLabel.isNotBlank() })
        assertEquals("Example route · not uploaded", OnboardingPages.first { it.visual == OnboardingVisual.MissionPlot }.previewLabel)
        assertEquals("Demo sliders · no flight commands", OnboardingPages.first { it.visual == OnboardingVisual.TiltPad }.previewLabel)
    }
}
