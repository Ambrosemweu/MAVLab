# SIM

The 3D vehicle observes the same authoritative attitude, position and motor state as Cockpit. Switching views does not interrupt foreground Phone Sensors input.

The working-tree 3D view starts with an elevated front three-quarter camera angle, showing the top, front camera housing and side of the drone. The initial framing is centered on the rendered vehicle and relative to its heading when SIM opens. Drag to orbit, use two fingers to pan, and pinch to zoom. Telemetry updates do not reset your chosen camera angle. Leaving SIM fully offscreen and reopening it restores the starting view. Only the inspection camera changes; the simulated vehicle's attitude, heading and flight state are not reset.

The working-tree 3D view uses neutral environment lighting, a stable main light and a gentle camera-following fill. Orbiting to the rear, sides or underside keeps visible surfaces illuminated; zooming adjusts fill power to maintain a similar brightness. Highlights and shadows still show the model's shape and materials. This is inspection lighting, not a simulated sun, and it does not change attitude, physics or control authority. The lighting probe is bundled for offline use; no new appearance setting is needed.

After a received mission is loaded, **View received route** opens a north-up spatial view. Control's **View route in SIM** opens it directly. **3D vehicle** returns to the model. There is no route control when no received mission exists.

The route preserves all received navigation coordinates and their order. Numbered nodes use received item sequence plus one; co-located nodes share a label (for example home and takeoff); speed commands count as items but have no spatial marker. Earlier items are green, the current planned target and leg are amber, remaining paths are blue, the actual vehicle is orange, and its observed trail is purple. Earlier items include the skipped QGC home marker; color alone does not establish execution. Hollow nodes denote partial command behavior described in Ops.

The view fits route, vehicle and bounded observed trail using one equal metre scale for north and east. Pinch to zoom and drag to inspect dense routes; Fit route restores the initial framing. It never snaps the vehicle onto the route or closes an open route artificially. Deviation measures distance to the finite active leg; it is unavailable for a first target without a previous navigation node or a completed route. Route geometry remains visible when position becomes stale or GPS is unavailable, but the current vehicle marker and deviation are hidden and labelled unavailable.

Rotating the phone keeps SIM and the route view selected. The route panel scrolls when space is limited, while the map retains a usable height. Drag on the map pans the route; swipe the text area to scroll the panel.

The trail records actual positions observed while SIM is composed, retaining up to 120 points with 0.35 metre minimum spacing. It is not a recording of an entire flight before SIM opened. Mission replacement clears the current observed trail. Historic received geometry remains available after completion until cleared or replaced.
