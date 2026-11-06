# 0.2.3 — camera permission query compatibility

Device: OnePlus PKG110, Android15/SDK35, ColorOS PKG110_15.0.0.802(CN01), WeChat8.0.78 ARM64. User requests agent-operated USB camera diagnosis; identity verification is performed by the user.

## Evidence and repair

On0.2.2 the host had actual CAMERA runtime permission and foreground camera AppOps. In the bound container WeChat main process, checkSelfPermission(CAMERA) returned-1 while currentOpPackageName was app.containerlab. Both legacy cameras (0/back and1/front) opened and released successfully using the normal framework API. No image was captured. The mismatched permission result could cause an SDK to stop before attempting hardware access.

The existing HostPermissionPolicy now includes CAMERA. Existing Binder adapters query the actual host's system grant/denial; they do not return unconditional grants. The scope remains bound WeChat/user0 with own UID/current PID and package/user/device constraints. Metadata reflects actual grant/denial. RECORD_AUDIO and unrelated permissions remain outside this change. No runtime permission was newly granted by the agent. No face frames or verification submissions were collected or generated.

An optional main-process camera permission diagnostic is separately guarded so query failures cannot abort Application binding. VersionCode10/versionName0.2.3, same package/signing identity; upgrade preserves data.

## Checks

- The actual HostPermissionPolicy test failed before repair on the CAMERA UID mapping assertion, then passed after repair.54 permission assertions and186 total assertions pass, including package/user/UID boundaries and denial metadata.
- In the actual upgraded guest process, checkSelfPermission(CAMERA)=0, RECORD_AUDIO=-1, and currentOpPackageName=app.containerlab.
- Camera0/back and1/front each completed open, setPreviewTexture, startPreview, stopPreview and release. Each preview ran briefly against a private SurfaceTexture; no frame pixels, photo, video or face image were retrieved or stored. This validates the API path, not visible UI rendering or biometric verification.
- Read-only review found no authorization spoofing or scope expansion; its optional diagnostic exception-handling concern was addressed before the final build.
- Repeated front/back preview API checks on the final installed build at16:01:13–14. CameraService recorded CONNECT and DISCONNECT for both devices under app.containerlab/guest PID10779; both devices were released and the debugger forward removed.

## Artifact identity

- APK: dist/wechat-container-lab-0.2.3.apk, 8,378,269 bytes, versionCode10.
- SHA256: `20b5b3c74e1e7971613eec4b994ea365bf21d445f5082a3321b7f9c638519dca`.
- Same debug signer SHA256: `4187ca36b9ebe8340ca44489229c19f03180c4c216fe32c3716aacaabce716af`.
- Installed base.apk was pulled back and its SHA256 matched the release APK.

## User verification on 2026-11-06

The user reported that face verification passed on their latest login attempt and that they entered WeChat in the container. This records the outcome of that login on their device.

## Remaining checks

Camera2-specific flows, preview orientation and long-running capture remain untested. Account restriction behavior on subsequent sessions remains open. The user previously reported that WeChat's send-location screen showed Wuhan. Location in the 校友邦 mini-program remains unverified.
