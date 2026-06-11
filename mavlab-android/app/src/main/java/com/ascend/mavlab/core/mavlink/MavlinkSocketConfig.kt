package com.ascend.mavlab.core.mavlink

const val DefaultMavLabVehicleSystemId = 1
const val DefaultMavLabAutopilotComponentId = 1
const val DefaultQgcSystemId = 255

data class MavlinkSocketConfig(
    val localBindPort: Int = 14551,
    val sameDeviceHost: String = "127.0.0.1",
    val sameDeviceQgcPort: Int = 14550,
    val lanDestinations: List<UdpDestination> = emptyList(),
    val systemId: Int = DefaultMavLabVehicleSystemId,
    val componentId: Int = DefaultMavLabAutopilotComponentId,
)

data class UdpDestination(
    val host: String,
    val port: Int = 14550,
)
