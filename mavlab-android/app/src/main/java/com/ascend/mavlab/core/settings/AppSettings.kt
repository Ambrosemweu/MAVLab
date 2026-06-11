package com.ascend.mavlab.core.settings

import com.ascend.mavlab.core.mavlink.DefaultMavLabVehicleSystemId
import com.ascend.mavlab.core.mavlink.MavlinkSocketConfig

data class AppSettings(
    val mavlink: MavlinkSocketConfig,
) {
    companion object {
        fun defaults(systemId: Int = DefaultMavLabVehicleSystemId): AppSettings {
            return AppSettings(
                mavlink = MavlinkSocketConfig(systemId = systemId),
            )
        }
    }
}
