# Container system-exit diagnostic plan

**Goal:** Obtain missing evidence for WeChat exits without changing guest runtime or deleting data, keeping the mobile report within1600 UTF-16 characters.

**Design:** The host activity reads its own Android ApplicationExitInfo records on its existing background worker. The two latest non-upgrade exit records are labeled historical with time, PID, reason and status. For one selected native crash, decode only signal, abort message and a few frames of the faulting thread from the Android15 tombstone protobuf; ignore memory, logs, command lines and unrelated threads. Read at most4MiB; unavailable/unsupported/malformed data gets an explicit status, never a success claim. No network upload, logcat streaming, system permissions or guest hook changes.

**Files:** `ExitSummary.java` pure record selection/formatting, `NativeCrashSummary.java` bounded wire parser, `SystemExitDiagnostics.java` Android adapter, existing `DiagnosticSummary`/`LabLog`/`LabActivity` integration, version0.1.3 and Java tests.

- [x] Add failing tests for record selection, reason/status distinction, native schema decoding, bounded/malformed input and report priority/redaction.
- [x] Implement pure formatters and Android host-only collector; make copy/refresh asynchronous and maintain1600-character cap.
- [x] Run Java checks and read-only code review, build/verify same-signer ARM64 APK and source archive.
- [x] Prepare an in-place upgrade with one reproduction/copy instruction. Device evidence remains needed before a runtime fix.

Verification:88 Java assertions passed; read-only review found no important blockers; final Android build succeeded in1m22s. Packaging verified app.containerlab/versionCode4/0.1.3, original signer and ARM64 ELF. Artifact:`dist/wechat-container-lab-0.1.3.apk`, SHA256:`1da6c52ba86d4c6ace8a9bab9267ded4379c14270cd08968155206161b5127e1`.

References: Android `ActivityManager.getHistoricalProcessExitReasons`, `ApplicationExitInfo.getTraceInputStream`, AOSP android-15.0.0_r1 `debuggerd/proto/tombstone.proto`.
