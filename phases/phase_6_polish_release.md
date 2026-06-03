# Phase 6 — Production Polish + Release

**Timeline:** Week 17–20  
**Depends on:** Phase 0–5  
**Produces:** A ship-quality app on Google Play Store with onboarding, lesson engine, error handling, performance optimization, design polish, CI/CD, and documentation.

---

## 1. Goal

Take the functional app from Phases 0–5 and make it **production-ready**:

1. **Onboarding flow:** First-time user experience guiding setup
2. **Lesson Engine:** Guided step-by-step curriculum with 7 lessons
3. **Error handling:** Graceful handling of all failure modes
4. **Performance optimization:** 60 FPS everywhere, low battery usage
5. **Design polish:** Premium Material 3 design, animations, dark theme
6. **CI/CD pipeline:** Automated build, test, and deploy via GitHub Actions
7. **Google Play release:** Store listing, screenshots, description
8. **Documentation:** README, setup guide, contributing guide, teacher manual
9. **MIT license:** Properly applied

---

## 2. Success Criteria

- [ ] First-time user can go from install → flying in < 3 minutes with onboarding
- [ ] 7 guided lessons covering all major concepts
- [ ] No crashes in 1 hour of continuous use
- [ ] No ANR (Application Not Responding) events
- [ ] 60 FPS on a Pixel 6 (mid-range 2021 device)
- [ ] Battery usage < 15% per hour of active use
- [ ] APK size < 50 MB
- [ ] Google Play Store listing live
- [ ] GitHub repository public with MIT license
- [ ] CI pipeline passes: lint, unit tests, build
- [ ] README with clear setup instructions
- [ ] Teacher/instructor guide document

---

## 3. New Files

```text
com/ascend/mavlab/
├── feature/
│   ├── onboarding/
│   │   ├── OnboardingScreen.kt           # Welcome + setup wizard
│   │   ├── OnboardingViewModel.kt
│   │   └── OnboardingPages.kt            # Individual onboarding pages
│   └── lessons/
│       ├── LessonScreen.kt               # Guided lesson player
│       ├── LessonViewModel.kt
│       ├── LessonEngine.kt               # Step sequencer with condition checks
│       ├── LessonStep.kt                 # Single lesson step data
│       ├── LessonCatalog.kt              # All lessons defined here
│       └── StepCard.kt                   # UI for a single step
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                      # Updated with polished theme
│   │   ├── Color.kt                      # Curated color palette
│   │   ├── Type.kt                       # Custom typography (Inter font)
│   │   └── Shapes.kt                     # Rounded corners, card shapes
│   └── components/
│       ├── AnimatedStatusBar.kt          # Top bar with connection status
│       └── LoadingScreen.kt             # App loading/splash

Root project:
├── .github/
│   └── workflows/
│       ├── android-ci.yml                # PR: lint + test + build
│       └── android-release.yml           # Tag: build + sign + Play Store
├── README.md
├── LICENSE                               # MIT
├── CONTRIBUTING.md
├── docs/
│   ├── setup_guide.md                    # Getting started
│   ├── teacher_guide.md                  # Instructor manual
│   ├── architecture.md                   # Technical overview
│   └── screenshots/                      # Play Store screenshots
└── fastlane/                             # Play Store metadata
    └── metadata/android/en-US/
        ├── title.txt
        ├── short_description.txt
        ├── full_description.txt
        └── images/
            ├── phoneScreenshots/
            └── featureGraphic.png
```

---

## 4. Onboarding Flow

### 4.1 Pages

```text
Page 1: Welcome
┌──────────────────────────────────────────┐
│                                          │
│          🚁                              │
│                                          │
│     Welcome to MAVLab                    │
│                                          │
│  Learn drone systems with your phone.    │
│  No hardware required.                   │
│                                          │
│             [Get Started →]              │
└──────────────────────────────────────────┘

Page 2: How It Works
┌──────────────────────────────────────────┐
│                                          │
│  Your phone IS the drone.                │
│                                          │
│  MAVLab runs a full flight simulation    │
│  inside your phone. Tilt to fly.         │
│                                          │
│  ┌──────┐    MAVLink    ┌──────┐        │
│  │ This │ ──────────►   │ QGC  │        │
│  │ App  │               │(GCS) │        │
│  └──────┘               └──────┘        │
│                                          │
│        [Next →]                          │
└──────────────────────────────────────────┘

Page 3: Install QGroundControl
┌──────────────────────────────────────────┐
│                                          │
│  Install QGroundControl                  │
│                                          │
│  QGC is the industry-standard Ground     │
│  Control Station. It connects to MAVLab  │
│  automatically.                          │
│                                          │
│  [Open Play Store]                       │
│                                          │
│  Already installed? [Skip →]             │
│                                          │
│  ☐ I'll install later                    │
│        [Next →]                          │
└──────────────────────────────────────────┘

Page 4: Ready!
┌──────────────────────────────────────────┐
│                                          │
│  You're ready to fly! 🎉                │
│                                          │
│  Quick tips:                             │
│  • Use split-screen for MAVLab + QGC     │
│  • Calibrate before flying               │
│  • Start with the guided lessons         │
│                                          │
│        [Start Flying →]                  │
└──────────────────────────────────────────┘
```

### 4.2 Implementation

```kotlin
/**
 * Onboarding is shown only on first launch.
 * Uses DataStore to persist "onboarding_complete" flag.
 *
 * After completion, navigates to the main dashboard.
 * "Open Play Store" button uses an Intent to the QGC Play Store page.
 */
```

---

## 5. Lesson Engine

### 5.1 Lesson Structure

```kotlin
data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    val estimatedMinutes: Int,
    val steps: List<LessonStep>,
)

data class LessonStep(
    val instruction: String,          // What to tell the student
    val detail: String,               // Deeper explanation
    val action: StepAction,           // What the student needs to do
    val completionCheck: CompletionCheck, // How to verify they did it
    val hint: String? = null,         // Help if they're stuck
)

sealed class StepAction {
    object ReadOnly : StepAction()                          // Just read, tap next
    object ArmDrone : StepAction()                          // Press arm
    data class ChangeMode(val mode: FlightMode) : StepAction()  // Switch mode
    data class Takeoff(val altitude: Float) : StepAction()  // Takeoff
    object TiltForward : StepAction()                       // Tilt phone forward
    data class SetPIDGain(val controller: String, val gain: String, val value: Float) : StepAction()
    data class InjectFailure(val failureId: String) : StepAction()
    object LandDrone : StepAction()
}

sealed class CompletionCheck {
    object Manual : CompletionCheck()                       // User taps "Done"
    object DroneArmed : CompletionCheck()
    object DroneDisarmed : CompletionCheck()
    data class DroneInMode(val mode: FlightMode) : CompletionCheck()
    data class AltitudeAbove(val meters: Float) : CompletionCheck()
    data class AltitudeBelow(val meters: Float) : CompletionCheck()
    data class SpeedAbove(val ms: Float) : CompletionCheck()
    object DroneOnGround : CompletionCheck()
}
```

### 5.2 Lesson Catalog — 7 Lessons

```kotlin
val LESSON_CATALOG = listOf(
    Lesson(
        id = "lesson_1",
        title = "1. Your First Flight",
        description = "Learn to arm, take off, hover, and land.",
        estimatedMinutes = 10,
        steps = listOf(
            LessonStep(
                instruction = "Welcome! Let's fly your first drone.",
                detail = "In this lesson, you'll learn the basic flight sequence: arm → takeoff → hover → land. Every real drone flight follows this same pattern.",
                action = StepAction.ReadOnly,
                completionCheck = CompletionCheck.Manual,
            ),
            LessonStep(
                instruction = "Arm the drone by pressing the ARM button.",
                detail = "Arming enables the motors. On a real drone, the propellers would start spinning. Never arm a real drone with people nearby!",
                action = StepAction.ArmDrone,
                completionCheck = CompletionCheck.DroneArmed,
                hint = "Look for the ARM button at the bottom of the Dashboard screen.",
            ),
            LessonStep(
                instruction = "Take off to 10 meters.",
                detail = "The drone will climb to 10m and hold altitude automatically in Alt Hold mode.",
                action = StepAction.Takeoff(10f),
                completionCheck = CompletionCheck.AltitudeAbove(8f),
            ),
            LessonStep(
                instruction = "Great! The drone is hovering. Look at the telemetry.",
                detail = "Notice the altitude (~10m), the attitude (near zero roll/pitch), and the throttle (~50%). The autopilot's PID controllers are keeping the drone stable.",
                action = StepAction.ReadOnly,
                completionCheck = CompletionCheck.Manual,
            ),
            LessonStep(
                instruction = "Now land the drone.",
                detail = "Switch to LAND mode. The drone will descend at a fixed rate and automatically disarm when it touches down.",
                action = StepAction.LandDrone,
                completionCheck = CompletionCheck.DroneOnGround,
            ),
            LessonStep(
                instruction = "Congratulations! You completed your first flight! 🎉",
                detail = "You just learned the universal flight sequence used by every drone pilot: pre-flight check → arm → takeoff → fly → land → disarm.",
                action = StepAction.ReadOnly,
                completionCheck = CompletionCheck.Manual,
            ),
        )
    ),

    Lesson(
        id = "lesson_2",
        title = "2. Understanding MAVLink",
        description = "Explore the protocol drones use to communicate.",
        estimatedMinutes = 8,
        steps = listOf(/* Steps guiding student through MAVLink Explorer */),
    ),

    Lesson(
        id = "lesson_3",
        title = "3. Phone as Controller",
        description = "Fly the drone by tilting your phone.",
        estimatedMinutes = 12,
        steps = listOf(/* Steps: calibrate, tilt, fly a square pattern */),
    ),

    Lesson(
        id = "lesson_4",
        title = "4. Flight Modes Explained",
        description = "Learn what each flight mode does and when to use it.",
        estimatedMinutes = 10,
        steps = listOf(/* Steps: try each mode, observe differences */),
    ),

    Lesson(
        id = "lesson_5",
        title = "5. PID Control — The Drone's Brain",
        description = "Understand how PID controllers keep the drone stable.",
        estimatedMinutes = 15,
        steps = listOf(/* Steps: tune PIDs, observe oscillation/overshoot */),
    ),

    Lesson(
        id = "lesson_6",
        title = "6. Sensors — How the Drone Sees",
        description = "Learn about IMU, GPS, compass, and barometer.",
        estimatedMinutes = 12,
        steps = listOf(/* Steps: compare phone vs sim sensors, add noise */),
    ),

    Lesson(
        id = "lesson_7",
        title = "7. When Things Go Wrong — Failsafes",
        description = "Inject failures and learn how the drone responds.",
        estimatedMinutes = 15,
        steps = listOf(/* Steps: GPS loss, wind, motor fail, battery drain */),
    ),
)
```

---

## 6. Error Handling

### 6.1 Error Categories

| Error | Cause | Handling |
|-------|-------|---------|
| Sensor not available | Device lacks gyroscope | Show message, disable controller tab, allow dashboard-only use |
| UDP port in use | Another app using 14550 | Try alternate ports (14551, 14552), show error with instructions |
| Service killed by OS | Low memory | Restart service automatically (START_STICKY), show reconnect notification |
| QGC not connecting | Firewall, wrong port, not installed | Show troubleshooting card on dashboard |
| 3D model load failure | Corrupted asset | Show 2D attitude indicator as fallback |
| Out of memory | Too many chart data points | Limit buffer sizes, use Flow.conflate() |

### 6.2 Implementation Pattern

```kotlin
/**
 * Wrap all critical operations in try-catch.
 * Use sealed class Result pattern for error propagation.
 * Show user-friendly error cards on the dashboard.
 * Never crash — degrade gracefully.
 */
sealed class SimResult<out T> {
    data class Success<T>(val data: T) : SimResult<T>()
    data class Error(val message: String, val recovery: String?) : SimResult<Nothing>()
}
```

---

## 7. Design Polish

### 7.1 Color Palette (Dark Theme)

```kotlin
// Color.kt
val DarkBackground = Color(0xFF0A0E1A)        // Deep navy
val DarkSurface = Color(0xFF141B2D)            // Card background
val DarkSurfaceVariant = Color(0xFF1E2740)     // Elevated surface
val Primary = Color(0xFF60A5FA)                // Sky blue
val PrimaryContainer = Color(0xFF1E3A5F)       // Blue container
val Secondary = Color(0xFF34D399)              // Mint green (armed/active)
val SecondaryContainer = Color(0xFF1A3B2E)     // Green container
val Tertiary = Color(0xFFFBBF24)               // Amber (warnings)
val Error = Color(0xFFEF4444)                  // Red (errors/critical)
val OnBackground = Color(0xFFE2E8F0)           // Light text on dark
val OnSurface = Color(0xFFCBD5E1)              // Secondary text
val OnSurfaceVariant = Color(0xFF94A3B8)       // Muted text
```

### 7.2 Typography

```kotlin
// Type.kt — use Google Fonts Inter for clean, professional look
val MavLabTypography = Typography(
    displayLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    // Monospace for telemetry values (digits don't shift)
    // Use JetBrains Mono or similar for values
)
```

### 7.3 Animations

- **Screen transitions:** Fade + slight slide (300ms)
- **Value changes:** Animate telemetry numbers (spring animation)
- **Card expansion:** Animate height change (MAVLink Explorer)
- **Mode changes:** Brief flash of mode chip
- **Arm/Disarm:** Pulse animation on button
- **3D drone:** Smooth interpolation between telemetry updates (lerp at render rate)

---

## 8. Performance Optimization

### 8.1 Key Optimizations

| Area | Technique | Target |
|------|-----------|--------|
| Physics loop | Run on `Dispatchers.Default`, not Main | < 1ms per step |
| UI updates | `Flow.sample(100ms)` for dashboard (10 Hz) | 60 FPS UI |
| 3D rendering | `Flow.sample(33ms)` for model transforms (30 Hz) | 60 FPS render |
| Charts | Buffer ≤100 points, no animations | Smooth scrolling |
| MAVLink Explorer | Ring buffer ≤200 messages, recycler view | No lag on scroll |
| Memory | Avoid allocations in hot loops (reuse objects) | < 200 MB RAM |
| Battery | Reduce sensor rate when controller not visible | < 15%/hour |
| APK size | ProGuard/R8 shrinking, compress 3D asset | < 50 MB |

### 8.2 ProGuard Rules

```proguard
# Keep MAVLink message classes (used via reflection)
-keep class com.divpundir.mavlink.definitions.** { *; }
-keep class com.divpundir.mavlink.api.** { *; }

# Keep SceneView classes
-keep class io.github.sceneview.** { *; }
-keep class com.google.android.filament.** { *; }
```

---

## 9. CI/CD Pipeline

### 9.1 CI — On Pull Request

```yaml
# .github/workflows/android-ci.yml
name: Android CI
on:
  pull_request:
    paths: ['app/**', 'gradle/**', '*.gradle.kts']

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Lint
        run: ./gradlew lintDebug

      - name: Unit Tests
        run: ./gradlew testDebugUnitTest

      - name: Build Debug APK
        run: ./gradlew assembleDebug

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

### 9.2 Release — On Tag

```yaml
# .github/workflows/android-release.yml
name: Release
on:
  push:
    tags: ['v*']

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Decode Keystore
        run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > app/release.jks

      - name: Build Release Bundle
        run: ./gradlew bundleRelease
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}

      - name: Upload to Play Store (Internal Track)
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.PLAY_SERVICE_ACCOUNT }}
          packageName: com.ascend.mavlab
          releaseFiles: app/build/outputs/bundle/release/app-release.aab
          track: internal
```

---

## 10. Google Play Store Listing

### 10.1 Metadata

**App name:** MAVLab — Drone Education Simulator

**Short description (80 chars):**  
Learn drone systems with your phone. Simulate, fly, and understand MAVLink.

**Full description:**
```
MAVLab turns your phone into a drone flight simulator and educational lab.

🚁 SIMULATE
Your phone runs a full quadcopter physics simulation — 6-DOF dynamics, PID autopilot, and realistic sensor models. No servers, no internet, no hardware required.

🎮 FLY
Tilt your phone to fly. The built-in controller maps your phone's orientation to drone controls. Throttle slider for altitude. It feels like holding a drone.

📡 CONNECT TO QGC
MAVLab speaks real MAVLink protocol. QGroundControl connects to your phone and thinks it's a real drone. Learn industry-standard tools used by professionals.

🔬 LEARN
- MAVLink Explorer: See every message, every field, explained
- PID Lab: Tune P, I, D gains and watch the drone respond
- Sensor Lab: Compare phone sensors to simulated drone sensors
- Flight Mode Lab: Understand Stabilize, Alt Hold, Loiter, RTL
- Failure Lab: Inject GPS loss, wind, motor failures, battery drain

📚 GUIDED LESSONS
7 structured lessons take you from zero to understanding drone internals:
1. Your First Flight
2. Understanding MAVLink
3. Phone as Controller
4. Flight Modes Explained
5. PID Control — The Drone's Brain
6. Sensors — How the Drone Sees
7. When Things Go Wrong — Failsafes

🌍 BUILT FOR ACCESSIBILITY
Works offline, on any Android 8.0+ device. Designed for students in resource-constrained environments where drone hardware is expensive.

📖 OPEN SOURCE
MIT licensed. Built by Ascend. Contribute on GitHub.
```

### 10.2 Screenshots Needed

1. Dashboard with live telemetry
2. 3D drone visualization
3. Phone tilt controller
4. MAVLink Explorer
5. PID Lab
6. Failure Lab with GPS disabled
7. Split-screen with QGroundControl
8. Lesson screen

### 10.3 Categories & Tags

- **Category:** Education
- **Tags:** drone, simulator, education, MAVLink, ArduPilot, flight, STEM, engineering

---

## 11. Documentation

### 11.1 README.md Structure

```markdown
# MAVLab 🚁

Learn drone systems with your phone.

[Screenshot/GIF of app in action]

## What is MAVLab?

MAVLab is a drone education simulator that runs entirely on your Android phone...

## Features
- [list with screenshots]

## Quick Start
1. Install MAVLab from [Play Store link]
2. Install QGroundControl from [Play Store link]
3. Open MAVLab → simulation starts automatically
4. Open QGC in split-screen → it detects the drone
5. Start the guided lessons!

## Building from Source
```bash
git clone https://github.com/ascend/mavlab.git
cd mavlab
./gradlew assembleDebug
```

## Architecture
[Link to architecture doc]

## Contributing
[Link to CONTRIBUTING.md]

## License
MIT License. See [LICENSE](LICENSE).

## Acknowledgments
- ArduPilot community
- mavlink-kotlin by divyanshupundir
- QGroundControl
```

### 11.2 Teacher Guide

Document covering:
- How to use MAVLab in a classroom setting
- Recommended lesson order
- Multi-device setup (students on phones, instructor on QGC)
- Assessment ideas (e.g., "tune PIDs to achieve < 5% overshoot")
- Extending with custom lessons

---

## 12. Testing Plan

### Comprehensive QA Checklist

1. **Install & Launch:** Clean install on Android 8, 10, 12, 14
2. **Onboarding:** Complete flow, verify persistence (don't show again)
3. **All lessons:** Complete all 7 lessons end-to-end
4. **Dashboard:** All telemetry values updating correctly
5. **Controller:** Phone tilt controls drone in all modes
6. **3D View:** Model loads, rotates, altitude changes
7. **MAVLink Explorer:** Messages appear, filter works, expand works
8. **PID Lab:** Gain changes affect drone behavior
9. **Sensor Lab:** Phone + sim sensors display correctly
10. **Flight Mode Lab:** All modes selectable, descriptions accurate
11. **Failure Lab:** All failures affect simulation correctly
12. **Missions:** QGC mission upload + execution
13. **QGC Split-screen:** Both apps work simultaneously for 10+ min
14. **Stability:** 1 hour continuous use, no crash
15. **Memory:** < 200 MB after 30 min
16. **Battery:** < 15% drain per hour
17. **Rotation:** Screen rotation doesn't crash
18. **Background/Foreground:** App recovers correctly from background

---

## 13. Definition of Done — v1.0 Release

- [x] Onboarding flow works on first launch
- [x] All 7 lessons completable
- [x] No crashes in 1 hour testing
- [x] 60 FPS on Pixel 6
- [x] APK < 50 MB
- [x] CI pipeline green (lint + test + build)
- [x] Google Play Store listing live (internal testing track)
- [x] GitHub repository public with MIT license
- [x] README with setup instructions
- [x] Teacher guide document
- [x] All Phase 0–5 acceptance criteria still pass
