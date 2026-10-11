# Simulated flight boundaries and rally points

Applies to v1.6.0 simulated behavior, not a real-aircraft safety system. See release notes and validation records for tested scope and carried-forward qualification.

Create geographic inclusion/exclusion polygons or circles and rally points in QGroundControl, then upload while MAVLab is disarmed. Navigation mission, fence and rally data are independent; an empty fence/rally upload clears only that list. A rejected replacement retains the last accepted list. The app accepts at most 100 items in each safety list and does not support terrain-relative coordinates.

In Ops, expand **Flight boundaries & rally** to inspect accepted item/point counts, the last transfer outcome, enforcement state, breach count and return destination. Uploading is not the same as enabling the fence. Verify the configuration in QGC first, then enable it while disarmed using Ops or `FENCE_ENABLE=1` in QGC.

QGC parameters select:

- `FENCE_TYPE`: 1 maximum altitude, 2 home-radius circle, 4 uploaded regions, 8 minimum altitude; add values to combine them.
- `FENCE_ALT_MAX` / `FENCE_ALT_MIN`: metres relative to home. The minimum starts monitoring after first reaching the floor and is inactive during landing.
- `FENCE_RADIUS`: metres around home. `FENCE_MARGIN`: return-endpoint clearance, not predictive braking.
- `FENCE_ACTION`: 0 report only, 1 RTL with local landing fallback, 2 land here. SmartRTL/BRAKE are not implemented.
- `FENCE_OPTIONS`: 0 intersects inclusion regions in the same group; 2 allows any inclusion region. Exclusion regions always forbid entry.
- `RALLY_LIMIT_KM`: ignore rally points farther than this distance; 0 removes that limit. `RALLY_INCL_HOME=1` lets home compete as the nearest return destination. `RTL_ALT` sets home return altitude in centimetres; a rally uses its own uploaded altitude.

An actionable breach switches the engine to **Fence recovery**, clears any direct-RPM override, and drives actual simulated flight. It does not move just the 3D model. Recovery continues after re-entering the boundary so the simulated vehicle can finish landing. RTL climbs if needed, returns to the nearest eligible rally/home destination, lands and disarms. With no eligible return or no GPS, it lands locally. A mission RTL is terminal; completion is recorded at touchdown.

You can always land or disarm. To resume manual flight deliberately, disable fencing in Ops and then select a manual mode in Control. Disabling enforcement alone does not cancel RTL or LAND. **Clear fence & rally** is disarmed-only, disables enforcement and leaves the navigation mission intact. QGC typed clear removes the list without changing enablement or simple home/altitude fence parameters.

Safety plans and configuration survive normal app process recreation; an armed flight does not. Changing the simulation origin in Ops removes old-location navigation/fence/rally data and disables fencing. Ordinary manual phone-sensor flight remains unchanged when fencing is disabled, and automatic recording is still opt-in.

MAVLab has flat simulation ground and straight-line return control. It does not plan around buildings, terrain or exclusion areas along the travel path. Put rally/home destinations and return paths where your simulated scenario makes sense. Endpoint eligibility is not path safety certification.

After installing this update, save your QGC plan and reconnect QGC so it refreshes the vehicle capabilities. A fresh handshake prevents stale assumptions from an earlier MAVLab build. Valid supported uploads should no longer receive the previous blanket fence/rally UNSUPPORTED response; genuinely unsupported or invalid items still produce an error rather than being silently accepted.

Technical limits and primary references are in the [MAVLink contract](../internals/mavlink-contract.md#simulated-fence-and-rally-contract-working-tree).
