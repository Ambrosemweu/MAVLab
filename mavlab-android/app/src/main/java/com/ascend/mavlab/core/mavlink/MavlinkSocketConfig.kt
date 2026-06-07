package com.ascend.mavlab.core.mavlink

data class MavlinkSocketConfig(
    val localBindPort: Int = 14551,
    val sameDeviceHost: String = "127.0.0.1",
    val sameDeviceQgcPort: Int = 14550,
    val lanDestinations: List<UdpDestination> = emptyList(),
    val systemId: Int,
    val componentId: Int = 1,
)

data class UdpDestination(
    val host: String,
    val port: Int = 14550,
)
