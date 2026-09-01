package com.ascend.mavlab.core.mavlink

enum class FirmwareVersionType(val wireValue: Int) {
    Development(0),
    Alpha(64),
    Beta(128),
    ReleaseCandidate(192),
    Official(255),
}

data class FirmwareVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val type: FirmwareVersionType,
) {
    init {
        require(major in 0..255) { "Firmware major version must fit in one byte" }
        require(minor in 0..255) { "Firmware minor version must fit in one byte" }
        require(patch in 0..255) { "Firmware patch version must fit in one byte" }
    }

    val encoded: Int
        get() = (major shl 24) or
            (minor shl 16) or
            (patch shl 8) or
            type.wireValue

    val displayName: String
        get() = "$major.$minor.$patch ${type.name.lowercase()}"
}

enum class MavlinkProtocolCapability(val bit: Long) {
    MissionFloat(1L),
    ParameterFloat(2L),
    MissionInt(4L),
    CommandInt(8L),
    Mavlink2(8192L),
}

data class ArduPilotCompatibilityProfile(
    val name: String,
    val flightVersion: FirmwareVersion,
    val capabilities: Set<MavlinkProtocolCapability>,
    val flightCustomVersion: String,
) {
    init {
        require(name.isNotBlank()) { "Compatibility profile name must not be blank" }
        require(flightVersion.type != FirmwareVersionType.Official) {
            "MAVLab must not advertise itself as an official ArduPilot firmware release"
        }
        require(flightCustomVersion.isNotBlank()) { "Custom version must identify MAVLab" }
        require(flightCustomVersion.length <= CustomVersionLength) {
            "MAVLink custom version is limited to $CustomVersionLength ASCII characters"
        }
        require(flightCustomVersion.all { it.code in PrintableAsciiRange }) {
            "Custom version must contain printable ASCII characters only"
        }
    }

    val capabilityMask: Long
        get() = capabilities.fold(0L) { mask, capability -> mask or capability.bit }

    fun flightCustomVersionBytes(): ByteArray {
        return ByteArray(CustomVersionLength).also { bytes ->
            flightCustomVersion.encodeToByteArray().copyInto(bytes)
        }
    }

    companion object {
        const val CustomVersionLength = 8
        private val PrintableAsciiRange = 0x20..0x7e
    }
}

object MavLabArduPilotCompatibility {
    /**
     * ArduCopter behavior baseline currently covered by MAVLab's protocol and
     * QGroundControl acceptance tests. Updating this version is a compatibility
     * decision, not a response to the latest-version notification in QGC.
     */
    val Current = ArduPilotCompatibilityProfile(
        name = "ArduCopter 4.6 protocol profile",
        flightVersion = FirmwareVersion(
            major = 4,
            minor = 6,
            patch = 3,
            type = FirmwareVersionType.Development,
        ),
        capabilities = setOf(
            MavlinkProtocolCapability.MissionFloat,
            MavlinkProtocolCapability.ParameterFloat,
            MavlinkProtocolCapability.MissionInt,
            MavlinkProtocolCapability.CommandInt,
            MavlinkProtocolCapability.Mavlink2,
        ),
        flightCustomVersion = "MAVLAB",
    )
}
