# Controller

Phone Sensors is the default local input mode. While MAVLab is visible, calibrated phone tilt and rotation feed the simulated vehicle through the shared engine, including when Cockpit or SIM is selected.

1. Select Phone Sensors and calibrate with the phone in a comfortable neutral position.
2. Arm the simulated vehicle. A fresh local sensor flight restores manual control and clears old motor-RPM overrides.
3. Use Takeoff to climb to the selected height. Local takeoff uses altitude hold, so phone tilt keeps steering roll and pitch and phone rotation keeps commanding yaw.
4. View SIM to observe the same vehicle attitude, position, and motor state shown in telemetry. Changing surfaces does not interrupt sensor control.
5. Land to return to the ground, then Disarm to stop the motors.

Arming and takeoff are separate actions. On the ground, accepted attitude input changes motor commands; physical ground contact constrains vehicle movement. At centered throttle, altitude hold maintains the target height. Raising or lowering throttle adjusts the height target.

An active GCS mission owns autonomous flight intent. Custom Input and Direct RPM are explicit alternative input modes. Backgrounding MAVLab suspends phone-sensor control. These control states remain distinct from ordinary foreground Phone Sensors operation.
