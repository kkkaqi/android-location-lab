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

The older pengliangAndroid/VirtualLocation repository is research evidence only and is not included in this fork.
Built-in Dobby static archives are supplied by upstream; their machine code was not independently reconstructed or audited.
