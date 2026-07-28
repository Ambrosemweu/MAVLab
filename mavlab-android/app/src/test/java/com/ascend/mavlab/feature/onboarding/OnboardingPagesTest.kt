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
                "Five surfaces, one drone",
                "Connect a ground station",
                "Your first takeoff",
                "Fly with phone tilt",
                "Run a mission",
                "Test failures safely",
                "Review and export",
            ),
            OnboardingPages.map { it.title },
        )
    }

    @Test
    fun surfacePageNamesTheFiveTabs() {
        val surfacePage = OnboardingPages.first { it.visual == OnboardingVisual.SurfaceGallery }
        listOf("Cockpit", "Controller", "SIM", "Mission", "Ops").forEach { tab ->
            assertTrue(surfacePage.body.contains(tab), "Missing tab name: $tab")
        }
    }

    @Test
    fun qgcPageIsTheOnlyPageWithQgcAction() {
        val actionPages = OnboardingPages.filter { it.showQGroundControlAction }
        assertEquals(listOf("Connect a ground station"), actionPages.map { it.title })
    }

    @Test
    fun firstPageIsHeroAndLastStartsFlying() {
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
}
