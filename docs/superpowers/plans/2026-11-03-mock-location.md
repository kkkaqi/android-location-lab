# Mock Location Implementation Plan

> **For agentic workers:** Execute inline with executing-plans; the user has authorized building the agreed no-root app. Steps use checkbox tracking.

**Goal:** Deliver a signed no-root Android APK providing explicit fixed mock location and stop controls.

**Architecture:** A pure Java coordinate/session core owns transactional provider lifetime. An Android backend writes system test providers; a user-started foreground service schedules updates; one Activity handles input and setup.

**Tech Stack:** Java 8 bytecode, Android SDK/build-tools 35, native Android Views, Python SDK/bootstrap and build automation; optional standard Gradle Android project.

## Global Constraints

- Android 12+, target/compile SDK 35; primary phone Android 15.
- No root, network permission, analytics, mock-detection bypass, automatic boot startup, or hidden operation.
- WeChat send-location and live-sharing require explicit physical-device validation.

### Task 1: Core and tests
Files: `tests/CoreTest.java`, `app/src/main/java/app/fixedlocation/Coordinates.java`, `MockSession.java`.
Interfaces: `Coordinates.parse(String latitude, String longitude)`, immutable latitude/longitude; `MockSession.Backend.ensureAuthorized()`, `install(String)`, `send(String, Coordinates)`, `remove(String)`; `start`, `tick`, `stop`, `providers`.
- [x] Write executable Java assertions for boundaries, NaN/infinity, empty input, lifecycle, start rollback, tick failure and independent cleanup.
- [x] Compile and run against missing core, observe expected failure, then implement and run all cases to green.

### Task 2: Android app
Files: `AndroidBackend.java`, `MockLocationService.java`, `MainActivity.java`, manifest and resources.
- [x] Implement provider setup, location fields, cleanup and service foreground notification. Fail clearly on missing mock app authorization or location permission.
- [x] Implement scrollable single-screen Chinese input, inline validation, settings shortcuts and service status; follow system theme and insets.
- [x] Use `START_NOT_STICKY`; stop and clean up on explicit stop, task removal or service destruction. Persist coordinates only, not running state.

### Task 3: Deliver and verify
Files: `tools/bootstrap.py`, `tools/build.py`, `build.ps1`, Gradle project files, `README.md`, `dist/`.
- [x] Download official SDK archives with checksum validation into ignored local tooling; use existing JDK, do not change global configuration.
- [x] Build with aapt2/javac/d8/zipalign/apksigner; create a local test signing key and retain it for reproducible updates.
- [x] Verify package, min/target SDK, permissions and signing. Run core tests and inspect app resources; report inability to run physical WeChat checks if no phone is connected.
- [x] Provide installation steps, developer-option setup, coordinate-system caveat and the two WeChat acceptance checks.
