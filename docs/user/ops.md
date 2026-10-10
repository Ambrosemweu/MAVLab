# Ops

Ops holds connection and mission evidence, failures, events, recordings, export and setup. A validated peer and mission authority are separate concepts.

Mission upload diagnostics appear for a loaded mission or a non-idle upload. They include exact upload phase, expected/received counts, zero-based requested/received sequences, rejection reason, stored source and item-level partial behavior. An accepted upload is not proof of AUTO execution. A restored mission is identified as stored data; restoration creates no new upload ACK.

If an upload fails, the previous loaded mission remains in the engine. Inspect the exact rejection, correct the mission in QGC and re-upload. Unknown command and unsupported-frame errors name the command/frame and item sequence. MAVLab rejects the upload rather than silently treating an unknown command as a waypoint.

Saved items with unverified source are retained for recovery. Re-upload from QGC to enable route display and local Start AUTO, or clear the stored mission in Control while disarmed. Events and recordings remain in the existing flight-log section; this change does not redesign export or GCS connection behavior.
