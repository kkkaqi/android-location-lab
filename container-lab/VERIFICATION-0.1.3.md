# 0.1.3 exit diagnostic verification

User evidence: version0.1.2 on OnePlus PKG110/Android15/WeChat8.0.78 exits immediately after launch, with no recorded Java exception. Guest main Application callback appears twice; this does not establish a cause or prove a process restart.

## Scope and source

Only host-side evidence collection changed. The core still returns an empty historical-exit list to virtual guests; collection is therefore explicitly restricted to the host main process and its own real package UID. No runtime crash fix is claimed.

Public Android APIs: `ActivityManager.getHistoricalProcessExitReasons`, `ApplicationExitInfo` timestamp/PID/process/reason/status/description/packageUid and native trace. Native field numbers follow AOSP `android-15.0.0_r1/debuggerd/proto/tombstone.proto`. The original bounded reader exports selected metadata and a few fault-thread frames, not memory or log contents. Trace availability is not guaranteed.

## Local verification

- Red: exit selection test failed when update records crowded crash evidence; native decoding test failed without schema decoding; mobile integration failed without inclusion of system evidence.
- 88 Java assertions passed: 9 import, 24 network mapping, 6 redaction, 28 compact/mobile, 10 exit summaries and 11 protobuf decode.
- Tests include fault TID selection across field order, bad varint/tag/length, oversized input, skipping unrelated threads/memory, redaction, update filtering, status-vs-reason and shared1600-character budget.
- Android build, signature/ARM64/package/source-archive checks are required before delivery; results emitted by build-container.ps1 and tools/package_container.py.

Device validation pending: same-package in-place install, reproduce once and immediately copy the new compact diagnostic. Existing records are explicitly historical and must be matched by timestamp. A new APK alone does not fix a root cause not yet identified.
