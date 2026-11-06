# WeChat post-login locale query compatibility fix

## Observed cause

USB debug authorization succeeded. Full private crash stack confirmed `yf.queueIdle` dereferences null chatting View. Read-only installed APK inspection located `NewChattingTabUI.c` and its creation in `g()`. A main-thread JDI exception probe then directly observed `g()` bytecode index519 -> MultiTaskUIC -> `b3.l.getContextForLanguage` -> `LocaleManager.getApplicationLocales` -> SecurityException for READ_APP_SPECIFIC_LOCALES. Kernel UID10435 belongs to host, while argument package is the original installed WeChat UID10355. The exception prevents completing chatting View creation; `e()` catches it and the later idle callback dereferences the null View. The earlier NewTips Byte exception has its own local catch and was not sufficient evidence of this root cause.

## Narrow change

SDK33+ locale service proxy. Only `getApplicationLocales(com.tencent.mm, actualHostSystemUser)` from bound WeChat virtual-user0 maps package to actual installed host, invoking the original binder. No synthetic grants, locale writes, other-package/user changes, View null suppression, login changes or data deletion. New guest contexts use replaced service binder; any existing host-context LocaleManager service reference is updated as well.

- [x] Add red/green pure policy regression checks for mapping, cloning, cross-package/user/signature boundaries.
- [x] Implement proxy and version0.1.5; 110 Java assertions pass.
- [x] Read-only review, final compile/package/signature verification.
- [x] ADB in-place install and real-device launch verification, documenting actual results separately from local tests.

Result: `container-lab/VERIFICATION-0.1.5.md`. The final package with bound-config guard is installed. Guest main PID23852 remained stable at10/20/30s with zero new main-thread exceptions, and screenshot confirmed the home/chat list in the container task. Long-term/all-feature coverage and location remain outside this validation.

Evidence paths (private local tooling, not distributed): `.tooling/diagnostics/main-exceptions-probe.txt`, `wechat-yf-dexdump.txt`, `NewChattingTabUI.java`. WeChat code is research input only and is not redistributed in the project source/APK.
