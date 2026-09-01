package com.ascend.mavlab.core.mavlink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ArduPilotCompatibilityProfileTest {
    @Test
    fun currentProfileIsPinnedAndNeverClaimsOfficialArduPilotFirmware() {
        val profile = MavLabArduPilotCompatibility.Current

        assertEquals("ArduCopter 4.6 protocol profile", profile.name)
        assertEquals(FirmwareVersion(4, 6, 3, FirmwareVersionType.Development), profile.flightVersion)
        assertFalse(profile.flightVersion.type == FirmwareVersionType.Official)
        assertEquals("MAVLAB", profile.flightCustomVersionBytes().decodeAsciiField())
    }

    @Test
    fun officialArduPilotIdentityIsRejectedForMavLabProfiles() {
        assertFailsWith<IllegalArgumentException> {
            ArduPilotCompatibilityProfile(
                name = "Invalid official profile",
                flightVersion = FirmwareVersion(4, 7, 0, FirmwareVersionType.Official),
                capabilities = emptySet(),
                flightCustomVersion = "MAVLAB",
            )
        }
    }

    @Test
    fun capabilityMaskContainsOnlyDeclaredProtocolSupport() {
        val profile = MavLabArduPilotCompatibility.Current

        assertEquals(
            profile.capabilities.fold(0L) { mask, capability -> mask or capability.bit },
            profile.capabilityMask,
        )
        assertEquals(0L, profile.capabilityMask and 128L)
        assertEquals(0L, profile.capabilityMask and 256L)
    }

    private fun ByteArray.decodeAsciiField(): String {
        return String(this, Charsets.US_ASCII).trimEnd('\u0000')
    }
}
