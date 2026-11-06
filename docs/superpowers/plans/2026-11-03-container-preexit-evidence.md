# Pre-exit exception evidence plan

User evidence: 0.1.3 host p0 exited at18:05:55, reason1 EXIT_SELF/status1, while local ordinary uncaught handler recorded no exception. Root cause remains unknown; exit1 is not proof of an anti-container check.

Design: observe Android15's Thread exception pre-handler in guest slots, preserving the prior handler and forwarding the identical Throwable. Installation uses hidden reflection after core attachment and explicitly reports unavailable if unsupported. No exception suppression or exit interception. When own-host exit evidence selects reason1/4, query a bounded own-UID E/F logcat snapshot and match the exact exiting PID within30s before/3s after its timestamp. Existing native path stays unchanged. No permissions, network upload, data clear or account changes; output still1600 UTF-16 characters.

- [x] Red/green tests for forwarding order/identity, observer failure, platform-handler behavior and PID/time/secret/error selection.
- [x] Implement pre-handler observer and bounded background logcat snapshot, version0.1.4.
- [x] Review, build and verify signature/ARM64/source archive.
- [x] Prepare in-place test with immediate reproduction and copy; device evidence remains pending.

Final verification:101 Java checks, read-only review with follow-up correction, Android build success in1m36s, original signer, ARM64 ELF and source archive validation. Artifact:`dist/wechat-container-lab-0.1.4.apk`, SHA256:`a2f87212b9b2855deb2e3fc169c10abdcac3a8888609c4d4a8ebbaafe33b11c7`.

Source verified directly from AOSP android-15.0.0_r1 libcore Thread.java: dispatchUncaughtException calls the pre-handler first, then application handlers. Runtime.java nativeExit is used by both exit/halt paths, so code1 alone is insufficient to identify the caller. No Runtime.nativeExit hook was introduced.
