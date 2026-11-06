# 0.2.0 validation — 2026-11-04

Scope: bound container WeChat/user0 only. This is a framework-validated prototype; actual WeChat map and mini-program adoption remains unverified.

## Device observations

OnePlus PKG110, Android15/SDK35, ColorOS PKG110_15.0.0.802(CN01), ARM64, WeChat8.0.78.

- 09:26: reproduced the original framework failure: LocationManager service handler ig5.n1 reached the real Android binder with guest package com.tencent.mm and host UID, causing invalid-package SecurityException. No scoped adapter appeared in that call stack.
- After preserving and wrapping the current service delegate after Application initialization, 09:30: GPS/network/fused last reads, current location and two continuous callbacks returned Wuhan 30.595000/114.297500. Cancellation stopped callbacks during the observation window.
- 09:33: host UI changed target to Shanghai 31.230400/121.473700, restarted the virtual instance and all those readbacks/callbacks changed accordingly.
- 09:40: disabled mode, restarted virtual instance, all three last-location caches returned null rather than the configured target. This verifies fallback without claiming that real location must always be available.
- The actual location-picker path repeatedly asked for permission. System package state showed fine/coarse granted, but guest Context checks returned -1. Query mapping was extended from network to exact location permission names; it delegates to the real binder rather than returning a synthetic grant.
- 09:44: guest fine/coarse=0 and background=-1, matching the system grant/denial. Re-enabled Wuhan; all three last reads, current callback, two continuous callbacks and cancellation passed the framework audit with fresh timestamps.
- System secure mock_location remained 0; app.fixedlocation MOCK_LOCATION remained deny. Original installed com.tencent.mm retained its package/UID10355. No original-app stop, uninstall, clear-data, Android test-provider or global mock-setting command was used.

The container account showed an existing notification of restricted social features, timestamped the previous day16:38, and later a login-expired dialog. The user was asked to handle login on the phone. No password, verification code, message, position or attendance submission was entered/sent. Account state prevents completing actual page validation in this session; no restriction bypass was attempted.

## Build and review

`container-lab/test.ps1`: 160 passing assertions including scope/coordinates, request lifetime, actual-grant query mapping and denial metadata, locale compatibility, bounded/redacted diagnostics and exit evidence. The new query/summary regression tests were observed failing before implementation.

`build-container.ps1`: BUILD SUCCESSFUL, 46s on final build. Package checks pass: app.containerlab, version0.2.0/code7, ARM64 ELF, signature and license asset, removed upload endpoint absent, source archive integrity/private-file exclusions.

Read-only review found no deterministic delegate recursion or permission-grant fabrication. Its audit-gate finding was fixed: enabled audit now requires actual guest fine-location permission. Background denial is not an audit failure.

Intermediate installed APK used for 09:44 runtime validation:
`ad2252f9b32bfa9453b2071c7a2dc4a82b05364444f8adba7a857d68164e027a`.

Final APK `dist/wechat-container-lab-0.2.0.apk`:
`bb4cea427cce757799627baaa34567743112464d3567d80e39522b6007cb68a1`.
Final delta adds an explicit fine-permission audit gate; it is built and packaged but not installed while the user may be completing login. Source archive includes current tests/docs, excluding local WeChat inspection code, device screenshots, SDKs and signing keys.

## Remaining acceptance

Install the final build without interrupting user login, then observe an actual WeChat location page and the requested mini-program. Current framework success is not evidence of their adoption. Other preexisting Context caches, later service rewrapping, long-running/background callbacks, PendingIntent callbacks and app-specific network positioning are not fully device-validated. No claim of account compatibility or protection from service restrictions is made.
