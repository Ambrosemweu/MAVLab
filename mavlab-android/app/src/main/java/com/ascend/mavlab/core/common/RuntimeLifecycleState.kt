package com.ascend.mavlab.core.common

/**
 * Process-wide lifecycle of the authoritative on-device simulation runtime.
 *
 * This is deliberately separate from MAVLink transport status. A listening UDP
 * socket does not prove that simulation state is current, and a connected peer
 * does not own control authority.
 */
enum class RuntimeLifecycleState {
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    ERROR,
}
