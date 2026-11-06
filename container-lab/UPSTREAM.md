# Upstream and local changes

Core: https://github.com/TeguFy/blackbox-android
Pinned commit: bf1623b7d45157bbc87be46a29352dd6bbbde9fa
License file retained at vendor/blackbox/LICENSE (Apache License 2.0).
The original README, source notices and submodule source directories are preserved.

Local modifications:
- New minimal Java host with separate application ID app.containerlab.
- Core namespace/JNI names remain unchanged.
- Automatic crash-log upload removed; LogSender implementation removed; destination ID empty.
- Automatic logcat-to-public-Downloads capture and logcat clearing disabled.
- Host disables VPN, optional root hiding, daemon service and FLAG_SECURE override settings.
- Android version helpers Tiramisu/U use SDK 33/34 respectively.
- Core native build restricted to ARM64 and pinned NDK r28c; Java source/target 17.
- Host verifies reported import package and actual container installation; launch request is never described as a visible guest UI success.
- Installation/launch preflight queries the core binder directly, avoiding installed-system-app fallback wrappers.
- Stub activity and pending-activity raw target fallbacks are disabled; unresolved explicit WeChat intents do not fall back to Android launching the real app.
- Guest application callbacks include the actual Application class name and do not imply that WeChat initialized successfully.
- Prototype min/target SDK are both 28 to preserve the core's legacy API behavior; primary device validation remains Android 15 only, not a claim of all-version support.
- Windows build strips only generated AIDL command comments that otherwise produce invalid Java Unicode escapes in paths containing a `user` package segment.
- 0.1.1: guest WeChat/user0 INTERNET and ACCESS_NETWORK_STATE checks map to actual host identity through original binder calls, including Android15 device-aware AMS and permission-manager variants. Device IDs and unrelated permission branches remain unchanged by this patch; metadata reflects actual host normal-network grants only.
- 0.1.1: local diagnostic URL parameter values such as ticket/token/code/phone are redacted on write and export. This is not a claim to sanitize arbitrary JSON or all personal data.
- 0.1.2: host-only compact diagnostic display/clipboard limited to 1600 UTF-16 characters, with latest-run process/permission aggregation and prioritized recent exception summaries. Private raw log storage and guest/core behavior remain unchanged.
- 0.1.3: host-activity-only asynchronous own-package ApplicationExitInfo collection, with bounded native tombstone signal/abort/fault-thread summaries based on AOSP Android15's documented protobuf schema. No guest hook, permission, account state or storage clearing changes; full memory/log buffers are not exported. The existing 1600-character export budget is retained.
- 0.1.4: guest-slot Thread pre-handler observation with original-handler forwarding, and on-demand own-UID E/F logcat snapshots matched to the latest selected self/Java exit PID/time. No exit suppression, nativeExit hook, added permission, account/data reset or upload. Failure to access the hidden pre-handler/logcat remains an explicit diagnostic limitation.
- 0.1.5: SDK33+ locale binder proxy maps only bound WeChat virtual-user0 own-app getApplicationLocales queries to the real host package/system user and delegates to the original system binder. Read-only lookup only; locale writes and other packages/users unchanged. Fix motivated by direct JDI evidence of READ_APP_SPECIFIC_LOCALES denial inside NewChattingTabUI.g before the later null-View failure. Installed WeChat code used for local analysis is not redistributed.
- 0.2.0: per-WeChat/user0 OWN_MODE controls with validated coordinates and verified core persistence, scoped Android15 last/current/listener/PendingIntent callback delivery, fresh elapsed realtime timestamps, cancellation/death cleanup, and virtual-only app restarts after enable/disable. Outside processes and Android test providers are not modified. Legacy provider-property reads avoid a duplicate binder call, and disabled provider queries delegate normally. Guest framework audit is explicitly separate from WeChat adoption.
- 0.2.0 follow-up: rebind the current cached framework location delegate after guest Application initialization, preserving its existing wrapper. HostPermissionPolicy/HostPermissions extend the previous network-only query mapping to exact foreground/background location permission names, returning the original binder's actual grants or denials. Guest audits check foreground grant, target coordinates, freshness, current callbacks and listener cancellation; disabled audits inspect last caches without exporting real coordinates. Compact diagnostics invalidate old location audit results on configuration changes or relaunch.

The older pengliangAndroid/VirtualLocation repository is research evidence only and is not included in this fork.
Built-in Dobby static archives are supplied by upstream; their machine code was not independently reconstructed or audited.
