# 0.2.2 verification — OnePlus Android15 renderer compatibility

Device: OnePlus PKG110, Android15/SDK35, ColorOS PKG110_15.0.0.802(CN01), ARM64. Guest WeChat8.0.78. Same package app.containerlab and debug signer as earlier builds; upgrades preserve guest data.

## Changes and checks

- Fixed an observed null service restart Intent parsing crash. Five direct factory checks pass.
- Preserved the original guest Activity envelope after cold process restoration; failed restoration stops requeueing and leaves the unchanged proxy to its existing finish-only implementation. Success after OS task restoration remains unverified: this ROM removed the test task during the attempted process-death test.
- Fixed guest Activity lifecycle identification to use the bound virtual application rather than rewritten Context package names. The fixed USB local-page probe uses a unique action and the page-owned XWeb widget. It checks debug host, bound WeChat/user0, exact Activity, marker and fixed public URL; it never replaces a verification page.
- Opted out of optional OEM UI scheduling only for bound user0 WeChat sandbox renderer processes on OnePlus/OPPO SDK35. Installs IOplusUIFirstManager.DEFAULT in that process's OplusFeatureCache and the existing main Choreographer extension before guest Application construction. No sandbox or signal changes. Other already-created thread/SF Choreographers are not traversed.
- Debug builds schedule one delayed renderer frame callback, 10 seconds after setup, for mechanism observation. It writes only a fixed log message. This instrumentation does not establish successful login.
- 179 assertions passed, including14 scope exclusions/inclusions. ARM64 packaging and APK signature verification passed. Review found no important blocker; the additional preexisting-Choreographer boundary remains documented.

## Device evidence

Before the OEM compatibility patch, own-UID signal-only native traces captured SIGSYS/code1/syscall29 in sandbox main threads17089,22953,28951. The latter two recorded ioctl request0xc030de02/fd basename ctrl. The same device exposes /proc/oplus_frame_boost/ctrl in the app's descriptors, and its libSchedAssistExtImpl::ofbBoostHint emits precisely that request. The OEM ChoreographerExtImpl calls ofbBoostHint during frame start. Secondary clone220 failures occurred in libwechatcrash after the first trap.

With0.2.2, the fixed local test page rendered visibly with progress100/height860. The process log confirmed the default feature and one existing main Choreographer were updated. Four background/foreground cycles completed without the earlier SIGSYS. A subsequent build's delayed frame callback executed at13:46:15 in renderer13708; the renderer remained alive. Native status retained NoNewPrivs1, Seccomp2 and two filters. Native observers forward fatal signals; they do not conceal the crash.

A controlled private baseline used exactly the same delayed frame callback while retaining the original OEM feature in the staging build only. Renderer16053 logged baseline setup at13:47:49, then trapped syscall29/ioctl0xc030de02 before the delayed callback completion message. Its crash handler subsequently trapped clone220. The patched renderer13708 completed that callback without the trap. This is direct A/B evidence for the tested scheduling conflict, distinct from the still-unverified real login outcome. The baseline build and extracted device code are excluded from distributed source/artifacts. Workspace sources were kept patched throughout, and the final release is rebuilt from those sources after the baseline run.

## Remaining acceptance

**Subsequent user report: account restricted.** After entering WeChat, the user reported a ban/restriction with the stated reason "使用模拟器或者外挂，非官方软件". The precise restriction duration/type and platform decision details are not yet available. Stop real-account container login/location testing. Successful rendering/login does not establish platform acceptance or account safety, and the mini-program location outcome remains unverified. Do not repeat login or use another account as a substitute for addressing this restriction; account recovery should use the official client and official support/appeal flow.

After installing the final patched APK, the user completed the normal verification flow and explicitly confirmed entry into WeChat at approximately13:53. Actual verification-page renderers19720 and19935 ended with exit0 in the signal traces; no matching SIGSYS was captured for those runs. This device/user login regression passed; it does not establish general compatibility for other accounts or devices.

Actual WeChat/mini-program location adoption is still pending. The current target is Wuhan30.595000,114.297500, and the latest main-process framework location audit passed at13:50:54. The user was asked to open the mini-program's location page and report its current address. Do not infer real page adoption from these API checks or submit attendance during verification.

## Final artifacts

- APK: dist/wechat-container-lab-0.2.2.apk, versionCode9, 8,377,929 bytes.
- SHA256: `63c85fab21bceb83fe9c910f0a0e44e96a14078cd274f44b9e4a5db964eabdc0`.
- Debug signer SHA256: `4187ca36b9ebe8340ca44489229c19f03180c4c216fe32c3716aacaabce716af`.
- Pulled installed app.containerlab/base.apk from the device after the final upgrade and confirmed it matches this APK hash, excluding accidental retention of the private baseline build.
- The phone was opened to the guest LoginPasswordUI at13:50:54, and the user was asked to complete their normal login/QR steps. No account credentials were entered by the agent.
