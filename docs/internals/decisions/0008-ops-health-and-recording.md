# ADR-0008: Ops health, observed transitions and recording control

Status: Accepted for the UI/UX feature branch, not a published release.

## Decision

Ops prioritizes current health and active corrective actions, then peer/identity and conditional mission-upload evidence, runtime components, failure exercises, recording/review, recent transitions, configuration and About. SIM redesign remains a separate phase.

`AppRuntime` owns a process-local `OperationsObserver`. The existing 200 ms recording monitor samples authoritative vehicle, failure, mission, protocol and runtime state. The observer retains at most 200 state transitions and first-observed timestamps for active incidents. It does not own simulation state, alter control authority, parse every telemetry packet or run a second engine. Identical high-frequency position, attitude and battery samples do not create history entries. The history survives tab switches and activity rotation but starts anew after process recreation. First-observed time is not a claim of exact fault onset. Sampling can miss changes shorter than 200 ms.

Incidents and history are separate. A reset is a command, not immediate proof of recovered telemetry: for example GPS remains degraded until the observed fix returns, and resetting drain does not recharge a low battery. Actual GCS heartbeat expiry and injected lost link remain separate issues. Absence of a peer is a normal offline state. Validation of a heartbeat does not prove outbound telemetry delivery or grant control authority. Unknown source and upload rejection retain exact mission evidence without duplicating Control flight-progress presentation.

The Ops header additionally exposes a labelled green/red GCS link dot. Its lifecycle-aware observer maps the existing `mavlinkIdentityStatus.gcsConnected` Boolean with distinct-until-changed, rather than adding a timer, inferring connection from packets, or collecting every identity update into the header. Existing three-second heartbeat warmup and 15-second expiry remain authoritative. Disconnected red is a link state, not a new critical incident; identity conflicts remain separately visible in Peers & identity.

## Recording

Recording is opt-in, as explicitly requested by the user during Ops implementation. Automatic recording defaults to off for local flights and AUTO missions. Ops can explicitly begin a ground or flight recording, or enable automatic-on-arm capture while disarmed. Explicit Begin does not opt into future automatic capture.

The Boolean `mavlab_recording/automatic_on_arm` preference is saved through Android SharedPreferences. Missing keys, including installations that previously recorded automatically without a preference, resolve to false. Existing recordings are preserved. The selected value survives runtime and process restart. A deliberate stop while armed suppresses opted-in automatic capture for that flight; explicit Begin can resume, and disarm/runtime stop clear only that temporary suppression. They do not enable a disabled preference. This is a settings addition, not a migration of recorded files.

Recording actions and the existing sampling monitor serialize through a mutex on IO coroutines. Session-history refresh and report previews run off the UI thread. History refresh is retained when recording status is synchronized. Stop saves the current mission snapshot so review reflects final mission outcome. Reviews include sampled horizontal path, modes, authorities, altitude and battery; sampled path is observed motion, not planned route length. A report is not a safety assessment.

The existing session directory, manifest, telemetry CSV, events JSONL and mission JSON formats are retained. The Markdown report gains descriptive summary lines. Unfinished prior sessions are retained for export and are not presented as completed reports. Report previews are bounded to 64,000 characters; full files remain available through Android's export chooser. Sharing does not imply delivery to another app.

## Reverse paths and lifecycle review

Injection is confirmed with purpose and expected observation; scenario reset is explicit. Reset all failures and battery recharge require confirmation; recharge is restricted to disarmed state in Ops. Changing the simulation origin is confirmed because it clears stored mission data, and the engine rechecks disarmed eligibility.

Ops runtime Stop uses the existing `SimulationService.stopIntent`; Start uses its app-visible intent. No retention timings, locks, protocol capabilities, coordinate frames, physics or canonical foreground sensor eligibility change. Explicit Stop freezes existing engine state rather than promising a landing or disarm; restart in the same process resumes that state. Stop closes the active recording using the existing shutdown path. Background handoff still belongs to `BackgroundRuntimePolicy` and service ownership.

Ops-to-Control recovery links select the existing Control surface. Inactive product surfaces remain uncomposed. No pager or SIM overlay redesign is introduced here.

## Evidence

Focused observer tests cover truthful recovery, neutral peer absence, link distinction, exact rejection evidence, bounded transition history and filtering. Recording policy and recorder tests cover deliberate stop, default-off and opted-in next-arm capture, explicit ground sessions and sampled review data. Device evidence belongs under `docs/validation/results/`; broad QGC, background and release qualification remain separate gates.
