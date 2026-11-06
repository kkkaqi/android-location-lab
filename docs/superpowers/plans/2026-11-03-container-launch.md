# Container launch implementation plan

> Execute inline with executing-plans. User approved an independent import/start-only prototype. Use requesting-code-review for the engine and final host changes.

**Goal:** Build an independently installable ARM64 no-root host that imports and requests launch of installed WeChat, with explicit diagnostics and no location changes in this phase.

**Architecture:** Native Android host delegates guest installation and launch to a pinned BlackBox core. Host identity and data are separate from existing apps. Vendor sources and licenses are kept with modification records.

**Tech Stack:** Java host, pinned Android SDK/NDK, Gradle, upstream Java/C++ library and reflection compiler.

- [x] Inspect original VirtualLocation source and APK; record target/ABI/licensing blockers.
- [x] Obtain user approval for independent guest instance and staged import/start verification.
- [x] Audit replacement core; pin source and preserve license; remove unrelated sample UI/modules from host dependencies.
- [x] Write and run failing validation tests, implement minimal host policy and rerun.
- [x] Implement independent host Application initialization, import/start controls and explicit error/launch-request diagnostics.
- [x] Resolve build inputs and compile ARM64 APK. Verify ABI, signing, permissions and manifest; do not claim device launch success without device evidence.
- [x] Independent code review; fix concrete blockers. Deliver APK/source if build succeeds, otherwise report exact remaining blockers without a fake container artifact.
