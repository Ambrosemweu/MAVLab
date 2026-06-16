package com.ascend.mavlab.feature.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingPagesTest {
    @Test
    fun v15PagesCoverRequiredFlow() {
        assertEquals(
            listOf(
                "What is MAVLab?",
                "What is a drone digital twin?",
                "Understand the app surfaces",
                "Connect QGroundControl",
                "First simulated takeoff",
                "Try phone tilt control",
                "Run a basic mission",
                "Inject a simple failure",
                "Review and export flight",
            ),
            OnboardingPages.map { it.title },
        )
    }

    @Test
    fun surfacePageUsesV15TabNames() {
        val surfacePage = OnboardingPages.first { it.title == "Understand the app surfaces" }
        val text = "${surfacePage.body} ${surfacePage.chips.joinToString(" ")}"

        listOf("Cockpit", "Controller", "Mission", "SIM", "Ops").forEach { tab ->
            assertTrue(text.contains(tab), "Missing tab name: $tab")
        }
    }

    @Test
    fun qgcPageIsTheOnlyPageWithQgcAction() {
        val actionPages = OnboardingPages.filter { it.showQGroundControlAction }

        assertEquals(listOf("Connect QGroundControl"), actionPages.map { it.title })
    }

    @Test
    fun copyAvoidsOldPrimaryTabNames() {
        val copy = OnboardingPages.joinToString("\n") { page ->
            "${page.title}\n${page.body}\n${page.primaryAction}\n${page.chips.joinToString(" ")}"
        }

        assertFalse(copy.contains("Twin"), "Onboarding should use SIM, not Twin")
        assertFalse(copy.contains("Fly"), "Onboarding should use Controller, not Fly")
    }

    @Test
    fun copyMentionsCoreTrainingWorkflow() {
        val copy = OnboardingPages.joinToString(" ") { page ->
            "${page.title} ${page.body} ${page.chips.joinToString(" ")}"
        }

        listOf(
            "phone-based drone digital twin",
            "QGroundControl",
            "takeoff",
            "phone tilt",
            "mission",
            "failure",
            "export",
        ).forEach { required ->
            assertTrue(copy.contains(required, ignoreCase = true), "Missing onboarding topic: $required")
        }
    }
}
