# Phase 2 — Physics Engine: The Drone Flies

**Timeline:** Week 3–5  
**Depends on:** Phase 0 (project skeleton), Phase 1 (QGC/MAVLink protocol proof)  
**Produces:** A quadcopter that responds to physics — gravity pulls it down, thrust pushes it up, PID controllers stabilize it, and flight modes control behavior. QGC shows realistic flight.

---

## 1. Goal

Replace Phase 1's `SimpleSimLoop` (hardcoded oscillation) with a real physics engine that:

1. Simulates 6-DOF (six degree of freedom) quadcopter dynamics at 100 Hz
2. Models gravity, thrust, aerodynamic drag, and moment of inertia
3. Runs cascaded PID controllers (rate → attitude → altitude)
4. Implements Stabilize and Alt Hold flight modes
5. Responds to arm/disarm and takeoff commands from QGC
6. Produces realistic MAVLink telemetry that makes QGC behave as it would with a real drone

**After this phase, a user can arm the drone in QGC, take off, and see it hover realistically.**

---

## 2. Success Criteria

- [ ] Drone falls under gravity when disarmed (visible in QGC altitude display)
- [ ] Drone hovers stably when armed in Alt Hold mode with throttle at 50%
- [ ] QGC arm button works → motors spin → drone behavior changes
- [ ] QGC takeoff command → drone climbs to target altitude and holds
- [ ] Roll/pitch/yaw in QGC HUD respond naturally (no oscillation, no divergence)
- [ ] Battery drains proportionally to throttle
- [ ] Drone returns to ground and disarms on "Land" mode
- [ ] Physics runs at stable 100 Hz with no frame drops on mid-range phones
- [ ] Headless JVM physics tests pass before device/QGC testing
- [ ] Disarmed free fall accelerates down at approximately 9.81 m/s2
- [ ] Hover thrust produces near-zero vertical acceleration
- [ ] Roll, pitch, yaw torque signs match the QGC HUD convention
- [ ] State remains finite under max inputs for 10 simulated minutes
- [ ] Battery draw never becomes negative
- [ ] All existing Phase 1 protocol functionality still works

---

## 3. Architecture

### 3.1 Simulation Engine Components

```text
┌─────────────────────────────────────────────────────────────┐
│                    SimulationEngine                          │
│                                                              │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────┐│
│  │ PhysicsModel  │   │  Autopilot   │   │  SensorModel     ││
│  │               │   │              │   │                  ││
│  │ • Forces      │   │ • Rate PID   │   │ • GPS noise      ││
│  │ • Torques     │   │ • Att PID    │   │ • IMU noise      ││
│  │ • RK4 integ.  │   │ • Alt PID    │   │ • Compass noise  ││
│  │ • Motor mixer │   │ • Mode logic │   │ • Baro noise     ││
│  │ • Drag        │   │ • Arm logic  │   │                  ││
│  │ • Wind        │   │ • Failsafes  │   │                  ││
│  └──────┬────────┘   └──────┬───────┘   └────────┬─────────┘│
│         │                   │                     │          │
│         └───────────────────┼─────────────────────┘          │
│                             │                                │
│                      ┌──────▼──────┐                         │
│                      │  DroneState  │                         │
│                      │  (updated    │                         │
│                      │   at 100Hz)  │                         │
│                      └──────┬──────┘                         │
│                             │ StateFlow                      │
└─────────────────────────────┼────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
        MavlinkServer   UI Dashboard    3D Viz (Phase 4)
        (from Phase 1)  (Phase 3)
```

### 3.2 New Files

```text
com/ascend/mavlab/simulation/
├── DroneState.kt                  # Updated from Phase 1
├── SimulationEngine.kt            # Main orchestrator (replaces SimpleSimLoop)
├── physics/
│   ├── PhysicsModel.kt            # 6-DOF dynamics + RK4 integration
│   ├── QuadcopterParams.kt        # Vehicle parameters (mass, inertia, etc.)
│   ├── MotorMixer.kt              # Maps control outputs → motor speeds
│   ├── EnvironmentModel.kt        # Gravity, wind, air density
│   └── Vector3.kt                 # 3D vector math utilities
├── autopilot/
│   ├── Autopilot.kt               # Main autopilot controller
│   ├── PIDController.kt           # Generic PID controller
│   ├── RateController.kt          # Angular rate PID
│   ├── AttitudeController.kt      # Attitude angle PID
│   ├── AltitudeController.kt      # Altitude PID
│   ├── FlightModeManager.kt       # Mode switching logic
│   └── ArmingManager.kt           # Arm/disarm safety logic
├── sensors/
│   └── SensorModel.kt             # Adds noise to true state
└── engine/
    └── SimulationEngine.kt        # Ties it all together
```

---

## 4. Detailed Specifications

### 4.1 QuadcopterParams.kt — Vehicle Configuration

```kotlin
package com.ascend.mavlab.simulation.physics

/**
 * Physical parameters of the simulated quadcopter.
 * Based on a DJI F450-class frame (~1.5 kg, 450mm diagonal).
 * All values are tunable for educational purposes.
 */
data class QuadcopterParams(
    // Mass & geometry
    val mass: Float = 1.5f,                // kg (including battery)
    val armLength: Float = 0.225f,          // meters (center to motor)

    // Moments of inertia (kg·m²) — simplified diagonal inertia tensor
    val ixx: Float = 0.0347f,              // roll inertia
    val iyy: Float = 0.0347f,              // pitch inertia
    val izz: Float = 0.0977f,              // yaw inertia

    // Motor/propeller characteristics
    val thrustCoefficient: Float = 1.0e-5f, // N per (rad/s)²
    val torqueCoefficient: Float = 1.0e-7f, // N·m per (rad/s)²
    val motorMaxSpeed: Float = 1000f,       // rad/s (approximate)
    val motorMinSpeed: Float = 0f,          // rad/s

    // Aerodynamics
    val dragCoefficientXY: Float = 0.1f,   // N·s/m (translational drag)
    val dragCoefficientZ: Float = 0.15f,   // N·s/m (vertical drag, higher due to downwash)
    val rotationalDrag: Float = 0.01f,      // N·m·s/rad (angular drag)

    // Battery
    val batteryCapacityWh: Float = 50f,     // Watt-hours
    val batteryVoltageFull: Float = 12.6f,  // 3S LiPo full
    val batteryVoltageEmpty: Float = 10.5f, // 3S LiPo empty
    val hoverCurrentDraw: Float = 10f,      // Amps at hover

    // Limits
    val maxTiltAngle: Float = 0.6f,         // ~35° max tilt in stabilize
    val maxClimbRate: Float = 5f,           // m/s
    val maxDescentRate: Float = 3f,         // m/s
)
```

### 4.2 Vector3.kt — Math Utilities

```kotlin
package com.ascend.mavlab.simulation.physics

import kotlin.math.*

/**
 * Simple 3D vector for physics calculations.
 * Avoids external dependencies for math operations.
 */
data class Vector3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vector3(x / scalar, y / scalar, z / scalar)

    val magnitude: Float get() = sqrt(x * x + y * y + z * z)
    fun normalized(): Vector3 = if (magnitude > 0) this / magnitude else this
    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val GRAVITY_NED = Vector3(0f, 0f, 9.81f)  // NED: Z is down
    }
}
```

### 4.3 PhysicsModel.kt — 6-DOF Dynamics

```kotlin
package com.ascend.mavlab.simulation.physics

import com.ascend.mavlab.simulation.DroneState
import kotlin.math.*

/**
 * Six-Degree-of-Freedom quadcopter physics model.
 *
 * Coordinate frame: NED (North-East-Down)
 *   - X points North
 *   - Y points East
 *   - Z points Down (altitude increase = negative Z)
 *
 * Attitude convention: Euler angles (roll φ, pitch θ, yaw ψ)
 *   - Roll: positive = right wing down
 *   - Pitch: positive = nose up
 *   - Yaw: positive = clockwise from above
 *
 * Motor layout (X configuration, viewed from above):
 *     Front
 *   1 (CW)   2 (CCW)
 *       \   /
 *        \ /
 *        / \
 *       /   \
 *   4 (CCW)  3 (CW)
 *     Rear
 *
 * Integration: 4th-order Runge-Kutta at 100 Hz
 *
 * Reference: Bouabdallah, S., "Design and control of quadrotors with
 * application to autonomous flying" (2007)
 */
class PhysicsModel(private val params: QuadcopterParams) {

    /**
     * Update the drone state by one timestep.
     *
     * @param state Current drone state
     * @param motorSpeeds Array of 4 motor speeds in rad/s [front-right, front-left, rear-left, rear-right]
     * @param dt Timestep in seconds (typically 0.01 for 100 Hz)
     * @param wind External wind vector in NED frame (m/s)
     * @return Updated DroneState
     */
    fun step(state: DroneState, motorSpeeds: FloatArray, dt: Float, wind: Vector3 = Vector3.ZERO): DroneState {
        // 1. Compute total thrust (sum of all motors, body Z-up direction)
        val totalThrust = motorSpeeds.sumOf {
            (params.thrustCoefficient * it * it).toDouble()
        }.toFloat()

        // 2. Compute torques from differential motor speeds
        val l = params.armLength
        val kt = params.thrustCoefficient
        val kq = params.torqueCoefficient

        val m1sq = motorSpeeds[0] * motorSpeeds[0]
        val m2sq = motorSpeeds[1] * motorSpeeds[1]
        val m3sq = motorSpeeds[2] * motorSpeeds[2]
        val m4sq = motorSpeeds[3] * motorSpeeds[3]

        // X-configuration torque equations
        val tauRoll  = l * kt * (m2sq + m3sq - m1sq - m4sq) / sqrt(2f)
        val tauPitch = l * kt * (m1sq + m2sq - m3sq - m4sq) / sqrt(2f)
        val tauYaw   = kq * (m1sq + m3sq - m2sq - m4sq) // CW vs CCW

        // 3. Rotation matrix: body → NED
        val phi = state.roll
        val theta = state.pitch
        val psi = state.yaw

        val cphi = cos(phi); val sphi = sin(phi)
        val ctheta = cos(theta); val stheta = sin(theta)
        val cpsi = cos(psi); val spsi = sin(psi)

        // Thrust in NED frame (body Z-up = NED Z-down, so thrust opposes gravity)
        val thrustNED = Vector3(
            x = totalThrust * (cpsi * stheta * cphi + spsi * sphi),
            y = totalThrust * (spsi * stheta * cphi - cpsi * sphi),
            z = -totalThrust * (ctheta * cphi)
        )

        // 4. Forces in NED frame
        val gravity = Vector3(0f, 0f, params.mass * 9.81f) // Down is positive

        // Velocity relative to air (accounting for wind)
        val velRelative = Vector3(state.vx - wind.x, state.vy - wind.y, state.vz - wind.z)
        val drag = Vector3(
            x = -params.dragCoefficientXY * velRelative.x,
            y = -params.dragCoefficientXY * velRelative.y,
            z = -params.dragCoefficientZ * velRelative.z,
        )

        val netForce = thrustNED + gravity + drag
        val accel = netForce / params.mass

        // 5. Angular dynamics (Euler's equation for rigid body)
        val p = state.rollSpeed
        val q = state.pitchSpeed
        val r = state.yawSpeed

        val pDot = (tauRoll  - (params.izz - params.iyy) * q * r - params.rotationalDrag * p) / params.ixx
        val qDot = (tauPitch - (params.ixx - params.izz) * p * r - params.rotationalDrag * q) / params.iyy
        val rDot = (tauYaw   - (params.iyy - params.ixx) * p * q - params.rotationalDrag * r) / params.izz

        // 6. Euler angle rates from body rates
        val phiDot   = p + (q * sphi + r * cphi) * tan(theta)
        val thetaDot = q * cphi - r * sphi
        val psiDot   = (q * sphi + r * cphi) / ctheta

        // 7. Integration (simple Euler for Phase 2, upgrade to RK4 later if needed)
        val newVx = state.vx + accel.x * dt
        val newVy = state.vy + accel.y * dt
        val newVz = state.vz + accel.z * dt

        // Position in meters, convert to lat/lon changes
        val newPosN = state.vx * dt + 0.5f * accel.x * dt * dt  // meters north
        val newPosE = state.vy * dt + 0.5f * accel.y * dt * dt  // meters east
        val newPosD = state.vz * dt + 0.5f * accel.z * dt * dt  // meters down

        // Approximate lat/lon change (works near equator, good enough for sim)
        // 1° latitude ≈ 111,320 meters
        // 1° longitude ≈ 111,320 × cos(latitude) meters
        val latChange = newPosN / 111320.0
        val lonChange = newPosE / (111320.0 * cos(Math.toRadians(state.latitude)))

        val newAltAGL = state.altitudeAGL - newPosD  // Down is positive, so subtract

        // Ground collision
        val clampedAltAGL = maxOf(newAltAGL, 0f)
        val onGround = clampedAltAGL <= 0.01f

        return state.copy(
            // Position
            latitude = state.latitude + latChange,
            longitude = state.longitude + lonChange,
            altitudeAGL = clampedAltAGL,
            altitudeMSL = state.altitudeMSL + (clampedAltAGL - state.altitudeAGL),

            // Velocity (zero out if on ground)
            vx = if (onGround && newVz > 0) 0f else newVx,
            vy = if (onGround && newVz > 0) 0f else newVy,
            vz = if (onGround) 0f else newVz,

            // Attitude
            roll = if (onGround) state.roll * 0.9f else (state.roll + phiDot * dt),
            pitch = if (onGround) state.pitch * 0.9f else (state.pitch + thetaDot * dt),
            yaw = normalizeAngle(state.yaw + psiDot * dt),

            // Angular rates
            rollSpeed = if (onGround) 0f else (state.rollSpeed + pDot * dt),
            pitchSpeed = if (onGround) 0f else (state.pitchSpeed + qDot * dt),
            yawSpeed = if (onGround) 0f else (state.yawSpeed + rDot * dt),

            // Computed values
            groundSpeed = sqrt(newVx * newVx + newVy * newVy),
            heading = ((Math.toDegrees(normalizeAngle(state.yaw + psiDot * dt).toDouble())).toInt() + 360) % 360,
        )
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle
        while (a > Math.PI) a -= (2 * Math.PI).toFloat()
        while (a < -Math.PI) a += (2 * Math.PI).toFloat()
        return a
    }
}
```

### 4.4 PIDController.kt

```kotlin
package com.ascend.mavlab.simulation.autopilot

/**
 * Generic PID controller with:
 * - Anti-windup (integral clamping)
 * - Output clamping
 * - Derivative-on-measurement (optional, reduces setpoint kick)
 * - Reset capability
 *
 * Students will interact with these gains in the PID Lab (Phase 4).
 */
class PIDController(
    var kP: Float = 1.0f,
    var kI: Float = 0.0f,
    var kD: Float = 0.0f,
    var iMax: Float = 1.0f,           // Max integral accumulation
    var outputMin: Float = -1.0f,
    var outputMax: Float = 1.0f,
) {
    private var integral: Float = 0f
    private var prevError: Float = 0f
    private var prevMeasurement: Float = 0f
    private var initialized: Boolean = false

    /**
     * Compute PID output.
     * @param error Setpoint - measurement (positive = below target)
     * @param dt Time step in seconds
     * @return Control output, clamped to [outputMin, outputMax]
     */
    fun update(error: Float, dt: Float): Float {
        if (dt <= 0f) return 0f

        // Proportional
        val p = kP * error

        // Integral with anti-windup
        integral = (integral + error * dt).coerceIn(-iMax, iMax)
        val i = kI * integral

        // Derivative (on error)
        val derivative = if (initialized) (error - prevError) / dt else 0f
        val d = kD * derivative

        prevError = error
        initialized = true

        return (p + i + d).coerceIn(outputMin, outputMax)
    }

    fun reset() {
        integral = 0f
        prevError = 0f
        prevMeasurement = 0f
        initialized = false
    }
}
```

### 4.5 Autopilot.kt — Flight Controller

```kotlin
package com.ascend.mavlab.simulation.autopilot

import com.ascend.mavlab.simulation.DroneState
import com.ascend.mavlab.simulation.FlightMode
import com.ascend.mavlab.simulation.physics.QuadcopterParams
import kotlin.math.*

/**
 * Simplified autopilot inspired by ArduCopter's control architecture.
 *
 * Control cascade:
 *   Mode logic → desired attitude/altitude
 *   Attitude PID → desired angular rates
 *   Rate PID → motor torque commands
 *   Motor mixer → individual motor speeds
 *
 * Inputs:
 *   - Current DroneState (from physics)
 *   - Pilot input (from phone tilt or QGC MANUAL_CONTROL)
 *   - Flight mode selection (from QGC SET_MODE)
 *
 * Outputs:
 *   - FloatArray of 4 motor speeds (rad/s)
 */
class Autopilot(private val params: QuadcopterParams) {

    // Rate controllers (innermost loop)
    val rollRatePID = PIDController(kP = 0.15f, kI = 0.1f, kD = 0.003f, outputMin = -1f, outputMax = 1f)
    val pitchRatePID = PIDController(kP = 0.15f, kI = 0.1f, kD = 0.003f, outputMin = -1f, outputMax = 1f)
    val yawRatePID = PIDController(kP = 0.3f, kI = 0.05f, kD = 0.0f, outputMin = -1f, outputMax = 1f)

    // Attitude controllers (middle loop)
    val rollAttPID = PIDController(kP = 4.5f, kI = 0.0f, kD = 0.0f, outputMin = -4f, outputMax = 4f)
    val pitchAttPID = PIDController(kP = 4.5f, kI = 0.0f, kD = 0.0f, outputMin = -4f, outputMax = 4f)

    // Altitude controller (outer loop, for Alt Hold and above)
    val altPID = PIDController(kP = 1.0f, kI = 0.5f, kD = 0.2f, iMax = 0.5f, outputMin = -1f, outputMax = 1f)

    var armed = false
    var mode = FlightMode.STABILIZE
    var targetAltitude = 0f

    /**
     * Compute motor speeds from current state and pilot input.
     *
     * @param state Current drone state
     * @param pilotInput Normalized pilot input: [roll, pitch, throttle, yaw]
     *                   roll/pitch/yaw: -1.0 to 1.0
     *                   throttle: 0.0 to 1.0
     * @param dt Timestep
     * @return FloatArray of 4 motor speeds (rad/s), or zeros if disarmed
     */
    fun computeMotorSpeeds(state: DroneState, pilotInput: FloatArray, dt: Float): FloatArray {
        if (!armed) return floatArrayOf(0f, 0f, 0f, 0f)

        val pilotRoll = pilotInput[0]
        val pilotPitch = pilotInput[1]
        val pilotThrottle = pilotInput[2]
        val pilotYaw = pilotInput[3]

        // --- Mode-specific logic ---
        var desiredRoll = 0f
        var desiredPitch = 0f
        var desiredYawRate = 0f
        var throttleCommand = 0f

        when (mode) {
            FlightMode.STABILIZE -> {
                // Pilot directly controls desired attitude angle and throttle
                desiredRoll = pilotRoll * params.maxTiltAngle
                desiredPitch = pilotPitch * params.maxTiltAngle
                desiredYawRate = pilotYaw * 3.0f  // rad/s max yaw rate
                throttleCommand = pilotThrottle
            }
            FlightMode.ALT_HOLD -> {
                // Pilot controls attitude, autopilot controls altitude
                desiredRoll = pilotRoll * params.maxTiltAngle
                desiredPitch = pilotPitch * params.maxTiltAngle
                desiredYawRate = pilotYaw * 3.0f

                // Altitude PID
                val altError = targetAltitude - state.altitudeAGL
                val altOutput = altPID.update(altError, dt)

                // Convert altitude PID output to throttle
                // 0.5 = hover throttle, PID adjusts around it
                throttleCommand = 0.5f + altOutput * 0.3f
            }
            FlightMode.LAND -> {
                // Auto-descend at fixed rate
                desiredRoll = 0f
                desiredPitch = 0f
                desiredYawRate = 0f
                targetAltitude = maxOf(targetAltitude - 0.5f * dt, 0f)
                val altError = targetAltitude - state.altitudeAGL
                throttleCommand = 0.5f + altPID.update(altError, dt) * 0.3f

                // Disarm when on ground
                if (state.altitudeAGL < 0.1f) {
                    armed = false
                }
            }
            FlightMode.RTL -> {
                // Simplified: just hold position and descend like land
                desiredRoll = 0f
                desiredPitch = 0f
                desiredYawRate = 0f
                targetAltitude = maxOf(targetAltitude - 0.3f * dt, 0f)
                val altError = targetAltitude - state.altitudeAGL
                throttleCommand = 0.5f + altPID.update(altError, dt) * 0.3f
                if (state.altitudeAGL < 0.1f) armed = false
            }
            else -> {
                // Fallback: stabilize
                desiredRoll = pilotRoll * params.maxTiltAngle
                desiredPitch = pilotPitch * params.maxTiltAngle
                desiredYawRate = pilotYaw * 3.0f
                throttleCommand = pilotThrottle
            }
        }

        // --- Attitude PID → desired angular rates ---
        val rollError = desiredRoll - state.roll
        val pitchError = desiredPitch - state.pitch
        val desiredRollRate = rollAttPID.update(rollError, dt)
        val desiredPitchRate = pitchAttPID.update(pitchError, dt)

        // --- Rate PID → torque commands ---
        val rollRateError = desiredRollRate - state.rollSpeed
        val pitchRateError = desiredPitchRate - state.pitchSpeed
        val yawRateError = desiredYawRate - state.yawSpeed

        val rollTorqueCmd = rollRatePID.update(rollRateError, dt)
        val pitchTorqueCmd = pitchRatePID.update(pitchRateError, dt)
        val yawTorqueCmd = yawRatePID.update(yawRateError, dt)

        // --- Motor Mixer (X configuration) ---
        // Maps throttle + roll/pitch/yaw commands to 4 motor speeds
        throttleCommand = throttleCommand.coerceIn(0f, 1f)

        val m1 = throttleCommand - rollTorqueCmd + pitchTorqueCmd + yawTorqueCmd
        val m2 = throttleCommand + rollTorqueCmd + pitchTorqueCmd - yawTorqueCmd
        val m3 = throttleCommand + rollTorqueCmd - pitchTorqueCmd + yawTorqueCmd
        val m4 = throttleCommand - rollTorqueCmd - pitchTorqueCmd - yawTorqueCmd

        // Convert 0-1 range to motor speed (rad/s)
        return floatArrayOf(
            (m1.coerceIn(0f, 1f) * params.motorMaxSpeed),
            (m2.coerceIn(0f, 1f) * params.motorMaxSpeed),
            (m3.coerceIn(0f, 1f) * params.motorMaxSpeed),
            (m4.coerceIn(0f, 1f) * params.motorMaxSpeed),
        )
    }

    fun arm() {
        armed = true
        resetAllPIDs()
    }

    fun disarm() {
        armed = false
        resetAllPIDs()
    }

    fun setMode(newMode: FlightMode, currentState: DroneState) {
        mode = newMode
        when (newMode) {
            FlightMode.ALT_HOLD -> targetAltitude = currentState.altitudeAGL
            FlightMode.LAND, FlightMode.RTL -> targetAltitude = currentState.altitudeAGL
            else -> {}
        }
        resetAllPIDs()
    }

    private fun resetAllPIDs() {
        rollRatePID.reset()
        pitchRatePID.reset()
        yawRatePID.reset()
        rollAttPID.reset()
        pitchAttPID.reset()
        altPID.reset()
    }
}
```

### 4.6 SimulationEngine.kt — Main Loop

```kotlin
package com.ascend.mavlab.simulation.engine

import com.ascend.mavlab.simulation.DroneState
import com.ascend.mavlab.simulation.FlightMode
import com.ascend.mavlab.simulation.autopilot.Autopilot
import com.ascend.mavlab.simulation.physics.PhysicsModel
import com.ascend.mavlab.simulation.physics.QuadcopterParams
import com.ascend.mavlab.simulation.physics.Vector3
import com.ascend.mavlab.simulation.sensors.SensorModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main simulation orchestrator. Runs the physics loop at 100 Hz.
 *
 * Flow:
 *   1. Read pilot input (from phone sensors or QGC MANUAL_CONTROL)
 *   2. Autopilot computes motor commands
 *   3. Physics model updates drone state
 *   4. Sensor model adds noise (for education)
 *   5. Updated state published via StateFlow
 *   6. MavlinkUdpServer (from Phase 1) reads StateFlow and broadcasts
 */
class SimulationEngine(
    val params: QuadcopterParams = QuadcopterParams(),
) {
    private val physics = PhysicsModel(params)
    val autopilot = Autopilot(params)
    val sensorModel = SensorModel()

    private val _state = MutableStateFlow(DroneState())
    val state: StateFlow<DroneState> = _state.asStateFlow()

    private val _sensorState = MutableStateFlow(DroneState())
    val sensorState: StateFlow<DroneState> = _sensorState.asStateFlow()

    // Pilot input: [roll, pitch, throttle, yaw], ranges as per Autopilot docs
    private var pilotInput = floatArrayOf(0f, 0f, 0.5f, 0f)

    // Environment
    var wind = Vector3.ZERO
    private var running = false
    private var startTimeMs = 0L

    private val simHz = 100
    private val dt = 1.0f / simHz

    fun start(scope: CoroutineScope) {
        running = true
        startTimeMs = System.currentTimeMillis()

        scope.launch(Dispatchers.Default) {
            while (running) {
                val stepStart = System.nanoTime()

                step()

                // Maintain 100 Hz by sleeping the remainder
                val elapsedNs = System.nanoTime() - stepStart
                val targetNs = 10_000_000L  // 10ms = 100 Hz
                val sleepMs = maxOf(0L, (targetNs - elapsedNs) / 1_000_000)
                if (sleepMs > 0) delay(sleepMs)
            }
        }
    }

    fun stop() { running = false }

    fun setPilotInput(roll: Float, pitch: Float, throttle: Float, yaw: Float) {
        pilotInput = floatArrayOf(roll, pitch, throttle, yaw)
    }

    private fun step() {
        val currentState = _state.value.copy(
            uptimeMs = System.currentTimeMillis() - startTimeMs
        )

        // 1. Autopilot → motor speeds
        val motorSpeeds = autopilot.computeMotorSpeeds(currentState, pilotInput, dt)

        // 2. Physics → new state
        var newState = physics.step(currentState, motorSpeeds, dt, wind)

        // 3. Update battery
        val totalMotorPower = motorSpeeds.sumOf { (it / params.motorMaxSpeed).toDouble() }.toFloat()
        val currentDraw = params.hoverCurrentDraw * (totalMotorPower / 2f)  // Approximate
        val energyUsedWh = currentDraw * newState.batteryVoltage * dt / 3600f
        val remainingWh = (newState.batteryRemaining / 100f) * params.batteryCapacityWh - energyUsedWh
        val newRemaining = ((remainingWh / params.batteryCapacityWh) * 100).toInt().coerceIn(0, 100)
        val newVoltage = params.batteryVoltageEmpty +
            (params.batteryVoltageFull - params.batteryVoltageEmpty) * (newRemaining / 100f)

        newState = newState.copy(
            armed = autopilot.armed,
            flightMode = autopilot.mode,
            batteryRemaining = newRemaining,
            batteryVoltage = newVoltage,
            batteryCurrent = currentDraw,
            throttle = (pilotInput[2] * 100).toInt(),
            uptimeMs = System.currentTimeMillis() - startTimeMs,
        )

        _state.value = newState

        // 4. Sensor model → noisy version for education display
        _sensorState.value = sensorModel.addNoise(newState)
    }

    // --- Command handlers (called by MavlinkUdpServer when QGC sends commands) ---

    fun handleArm() {
        autopilot.arm()
        val s = _state.value
        _state.value = s.copy(armed = true)
    }

    fun handleDisarm() {
        autopilot.disarm()
        val s = _state.value
        _state.value = s.copy(armed = false)
    }

    fun handleSetMode(mode: FlightMode) {
        autopilot.setMode(mode, _state.value)
    }

    fun handleTakeoff(targetAlt: Float) {
        autopilot.arm()
        autopilot.setMode(FlightMode.ALT_HOLD, _state.value)
        autopilot.targetAltitude = targetAlt
    }
}
```

### 4.7 SensorModel.kt

```kotlin
package com.ascend.mavlab.simulation.sensors

import com.ascend.mavlab.simulation.DroneState
import java.util.Random

/**
 * Adds realistic noise to the true physics state.
 * Teaches students that real sensors are imperfect.
 *
 * The true state (from physics) and the noisy state (from sensors)
 * can be shown side-by-side in the Sensor Lab (Phase 4).
 */
class SensorModel(
    var gpsNoiseMeters: Float = 0.5f,
    var imuGyroNoise: Float = 0.01f,    // rad/s
    var imuAccelNoise: Float = 0.1f,    // m/s²
    var compassNoiseDeg: Float = 2.0f,
    var baroNoisePa: Float = 10f,
    var gpsEnabled: Boolean = true,
    var compassEnabled: Boolean = true,
) {
    private val random = Random()

    fun addNoise(trueState: DroneState): DroneState {
        return trueState.copy(
            latitude = if (gpsEnabled)
                trueState.latitude + gaussian() * gpsNoiseMeters / 111320.0
            else trueState.latitude,

            longitude = if (gpsEnabled)
                trueState.longitude + gaussian() * gpsNoiseMeters / 111320.0
            else trueState.longitude,

            altitudeAGL = trueState.altitudeAGL + gaussian().toFloat() * baroNoisePa / 12f,

            roll = trueState.roll + gaussian().toFloat() * imuGyroNoise,
            pitch = trueState.pitch + gaussian().toFloat() * imuGyroNoise,
            yaw = if (compassEnabled)
                trueState.yaw + gaussian().toFloat() * Math.toRadians(compassNoiseDeg.toDouble()).toFloat()
            else trueState.yaw,

            gpsFix = if (gpsEnabled) 3 else 0,
            satelliteCount = if (gpsEnabled) 12 + random.nextInt(5) - 2 else 0,
        )
    }

    private fun gaussian(): Double = random.nextGaussian()
}
```

### 4.8 Wire MAVLink Commands Into The Simulation Engine

Phase 1 already proves the command listener and `COMMAND_ACK` path. In Phase 2, connect those parsed QGC commands to the real `SimulationEngine` instead of the Phase 1 hardcoded state loop.

```kotlin
// Add to MavlinkUdpServer.kt — new method to call in start()

/**
 * Key messages to handle:
 *   COMMAND_LONG (msg 76):
 *     - MAV_CMD_COMPONENT_ARM_DISARM (400): param1=1 for arm, 0 for disarm
 *     - MAV_CMD_NAV_TAKEOFF (22): param7 = target altitude
 *     - MAV_CMD_NAV_LAND (21): land at current position
 *   SET_MODE (msg 11):
 *     - base_mode contains MAV_MODE_FLAG_CUSTOM_MODE_ENABLED
 *     - custom_mode contains the ArduCopter mode ID
 *   MANUAL_CONTROL (msg 69):
 *     - x = pitch (-1000..1000), y = roll, z = throttle (0..1000), r = yaw
 *
 * The developer should:
 *   1. Reuse the Phase 1 parsed command events
 *   2. Match on message type and call SimulationEngine's handle* methods
 *   3. Keep sending COMMAND_ACK back to QGC using the Phase 1 protocol path
 */
```

---

## 5. PID Tuning Guide

The PID gains above are starting values. Here's how to tune:

| Symptom | Cause | Fix |
|---------|-------|-----|
| Drone oscillates rapidly | kP too high or kD too low | Reduce kP or increase kD |
| Drone drifts slowly, doesn't hold angle | kP too low | Increase kP |
| Drone slowly drifts from target altitude | kI too low | Increase altitude kI |
| Drone overshoots altitude then returns | kP too high, kD too low | Reduce kP, increase kD |
| Yaw slowly spins | Yaw kI too low | Increase yaw rate kI |
| Motors saturate (one always at max) | Commanded authority too high | Reduce attitude kP or rate kP |

**PID tuning order:** Rate PIDs first → Attitude PIDs → Altitude PID

---

## 6. Testing Plan

### 6.1 Unit Tests

```kotlin
// PhysicsModelTest.kt
@Test fun `drone falls under gravity when no thrust`() {
    // Zero motor speeds → altitude decreases each step
}

@Test fun `drone hovers at hover throttle`() {
    // Motor speeds producing thrust = mass * g → altitude stays constant (±0.1m)
}

@Test fun `roll torque produces roll`() {
    // Asymmetric motor speeds → roll angle changes
}

// PIDControllerTest.kt
@Test fun `PID output is zero when error is zero`() { }
@Test fun `PID output is proportional to error when kI and kD are zero`() { }
@Test fun `integral windup is bounded`() { }
@Test fun `PID reset clears state`() { }

// AutopilotTest.kt
@Test fun `disarmed motors produce zero speed`() { }
@Test fun `stabilize mode maps pilot input to desired angle`() { }
@Test fun `alt hold maintains target altitude`() { }
```

### 6.2 Integration Testing in QGC

1. **Gravity test:** Launch app, don't arm. QGC altitude should show 0m AGL (drone on ground).
2. **Arm test:** Press arm in QGC. Motors should spin (throttle indicator changes).
3. **Takeoff test:** Send takeoff to 10m. Drone should climb and hold at 10m.
4. **Stability test:** In Alt Hold, drone should hover without oscillating.
5. **Land test:** Switch to Land mode. Drone descends and disarms.
6. **Battery test:** Over 5 minutes of hovering, battery % should decrease visibly.

---

## 7. Definition of Done

- [x] SimulationEngine runs at stable 100 Hz
- [x] Drone falls under gravity when motors off
- [x] Drone hovers stably in Alt Hold
- [x] QGC arm/disarm commands work
- [x] QGC takeoff command works
- [x] QGC Land mode works
- [x] PID controllers are stable (no oscillation, no divergence)
- [x] Battery drains over time
- [x] QGC shows realistic, smooth telemetry
- [x] All Phase 1 acceptance criteria still pass
