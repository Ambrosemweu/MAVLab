package com.ascend.mavlab.core.mavlink

data class MavlinkIdentityStatus(
    val vehicleSystemId: Int = DefaultMavLabVehicleSystemId,
    val vehicleComponentId: Int = DefaultMavLabAutopilotComponentId,
    val lastGcsSystemId: Int? = null,
    val lastGcsComponentId: Int? = null,
    val identityConflict: Boolean = false,
    val recommendedGcsSystemId: Int = DefaultQgcSystemId,
    val message: String = "",
) {
    val healthLabel: String
        get() = if (identityConflict) "CONFLICT" else "OK"
}
