# Onboarding

Working-tree UI/UX feature-branch behavior; this is not a published release claim.

MAVLab's nine-step introduction explains the simulated vehicle, shared state, four surfaces, optional QGroundControl connection, phone calibration, first flight, received missions, failure exercises and recording/review. It uses the existing dark navy, blue-accented visual style.

Swipe horizontally to move between pages, or use **Get started**, **Next** and **Back**. Android Back returns to the previous onboarding page; on the first page it retains normal Android Back behavior. The progress rail and step count show the committed page. The tour does not wrap at either end, and swiping past the final page does not finish it.

**Skip for now** and the final **Start flying** action finish the introduction and open Cockpit. Neither action arms the simulated vehicle, starts a mission or begins recording. Completion is saved using the existing preference, so this update does not force returning users through onboarding again. Replay it from **Ops → About MAVLab → Replay onboarding**. The selected tutorial page survives activity recreation; completing/skipping a replay still opens Cockpit.

The recording illustration uses the same graph component as Cockpit: amber altitude in metres, teal roll, dashed blue pitch and dotted purple yaw in degrees, with a ten-second time axis. Smooth static example traces demonstrate the layout; they are labelled **not live telemetry**, are not a saved flight, and do not change or filter the actual flight graphs.

Replaying the tour does not stop an existing simulation or change control authority. Land and disarm in Control first if you want to review it without an active flight.

The action buttons stay below the page content. On short displays or with larger text, scroll the copy and illustrations vertically. Vertical scrolling and the demo throttle/yaw sliders keep their own gestures; they do not advance the page. During TalkBack touch exploration or text entry, page swiping is disabled and the labelled navigation buttons remain available. Android's animation-duration setting applies to the pager.

All flight instruments, route and recording charts shown here are labelled learning previews, not live vehicle state. The demo sliders change only their tutorial values. The example route is not uploaded or loaded into the engine; no mission is created by onboarding. **Get QGroundControl** opens the official download/install documentation in a browser, not a flight command or a promise that QGC is already installed. The GCS illustration explains green connected/red disconnected link states; control authority remains separate.

The first-flight sequence is calibration in Control, **Arm → Takeoff → Land → Disarm**. Arming and takeoff are separate, and disarming cuts the motors. Received missions are operated in Control, followed in SIM and diagnosed in Ops. Recording is **off by default**: begin explicitly in Ops or enable recording on arm while disarmed, then stop/save before reviewing/exporting. See [Control](controller.md), [Mission operation](mission.md), [Ops](ops.md), and [Navigation and display](navigation-and-display.md).
