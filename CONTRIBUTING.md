# Contributing to MAVLab

MAVLab is an Android-first drone education simulator. Keep changes focused, testable, and useful for students learning flight systems.

## Local Setup

1. Install Android Studio or Android SDK command-line tools.
2. Install Android SDK 35 and Java 17.
3. Open `mavlab-android/` as the Android project.
4. Build and test:

```bash
cd mavlab-android
GRADLE_USER_HOME="$PWD/.gradle" ./gradlew lintDebug testDebugUnitTest assembleDebug
```

## Contribution Guidelines

- Keep simulator behavior deterministic where possible.
- Add JVM tests for physics, controller mapping, mission, and failure logic.
- Avoid network-dependent runtime features unless there is an offline fallback.
- Keep APK size under 50 MB.
- Do not commit local SDK paths, build outputs, or device-specific files.
