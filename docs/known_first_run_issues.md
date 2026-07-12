# Known first-run issues (read this before posting)

Most "it won't connect" reports are one of the items below. If none matches, open a [First-run problem](../../issues/new?template=first_run_problem.md) issue.

## 1. QGroundControl doesn't discover the vehicle

- **Split-screen (same phone):** QGC listens on `127.0.0.1:14550`. Make sure MAVLab is running *before* you open QGC, or tap refresh in QGC.
- **System IDs:** MAVLab defaults to system ID `1`; QGC's GCS system ID defaults to `255`. If you share a network with other MAVLink devices, set a unique MAVLab system ID per device (the paper §6 covers per-install IDs for classroom networks).
- **QGC version drift:** very new QGC builds may expect stream-interval acknowledgements earlier than older ones. If discovery is flaky, try the QGC version noted in `docs/v1_5_release_notes.md`.

## 2. Desktop QGC over Wi-Fi — no link

- Phone and computer must be on the **same subnet**.
- Many "guest" Wi-Fi networks and some routers use **client isolation / AP isolation**, which blocks device-to-device UDP. Use a trusted LAN or a phone hotspot.
- A desktop **firewall** may block UDP 14550 — allow it inbound.

## 3. APK won't install

Android blocks sideloaded APKs by default. When prompted, allow installs from the app you opened the APK with (file manager / browser). This is normal.

## 4. Split-screen throttling

Some Android versions throttle a backgrounded app's networking. Keep MAVLab and QGC both **visible** in split-screen; don't minimize MAVLab.

## 5. Mission upload fails / no progress

Confirm QGC is set to accept missions and that the vehicle is armed or in an appropriate mode. See `docs/v1_5_qgc_acceptance.md` for the full acceptance path.

## Still stuck?

Open a [First-run problem](../../issues/new?template=first_run_problem.md) issue with your environment details. We answer quickly.
