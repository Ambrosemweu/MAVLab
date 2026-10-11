# Navigation and display

Working-tree UI/UX behavior; this is not a published release claim.

MAVLab has four destinations: **Cockpit**, **Control**, **SIM**, and **Ops**. The compact bottom bar keeps all four names visible, including in full-screen mode. Tap a destination to open it. Tapping the selected destination leaves your current view intact.

You can also swipe left or right across passive content. Cockpit and Ops accept swipes across informational cards, text and the space around them, including the middle and lower screen. Buttons, mission actions, charts, switches and fields keep their own input. Control accepts swipes on its operational header and phone-tilt preview; SIM accepts them on its top status strip and received-route title. A swipe moves one screen at a time. Cockpit and Ops are the ends of the sequence; navigation does not loop.

A short horizontal swipe or quick flick is enough; you do not need to drag across most of the display. Small diagonal drift is allowed, and another swipe can begin while the previous screen transition finishes. A very small twitch, slow movement that is too short, or a drag back to its starting point leaves the screen unchanged.

The current and next screen slide together with your finger and settle from that same position when you release it. An interrupted or cancelled swipe eases back. Swiping past Cockpit or Ops stops at the end, and a new swipe takes over immediately from a settling animation. Partially visible neighbouring screens are previews, not active controls. SIM's 3D renderer starts after you arrive so it does not cold-start during the swipe.

Each page's drawing stays inside its own bounds. SIM's grid and flight trail cannot draw a stray line over Control or Ops while SIM is prepared offscreen.

Controls have priority. Dragging a slider, manipulating the 3D camera, panning a route, interacting with a mission action, or scrolling vertically does not change screens. Use the bottom buttons when a screen is mostly interactive. Screen-swiping is disabled during text entry and TalkBack touch exploration. Android system gestures remain available at the edges.

On Cockpit and Ops, a horizontal drag over a button cancels its tap. Release and tap normally to activate the button.

## Full screen

In **Ops → Display**, enable **Full screen** to hide Android's status and navigation bars. Reveal them temporarily using Android's system-edge gesture. MAVLab's four navigation buttons remain visible. Turn the setting off to return to standard display. The preference is saved across launches and applies to all four screens.

Standard display is the initial default. The app uses the screen edges for its background while keeping text and controls clear of camera cutouts and system-navigation areas. The bar expands when larger text needs more space. Keyboard and multi-window interaction can show Android bars even when Full screen is selected.

The Android launch window and splash background match MAVLab's dark display, avoiding a white flash before the app draws.

## Flight continuity

Changing screens or display mode does not arm, disarm, land, change control authority, or stop recording. Armed Phone Sensors input continues through the simulation engine while MAVLab is visible, including in Cockpit, Control, and SIM. GCS Mission authority and explicit recovery modes retain their control rules. Backgrounding MAVLab still suspends phone-sensor input.

Useful screen view state, including scroll positions and expanded detail, survives screen switching. Android Back closes the received-route view or expanded mission detail before following the existing exit behavior. Vehicle state remains owned by the runtime; restoring a screen does not create a new flight.
