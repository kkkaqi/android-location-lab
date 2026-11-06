# 0.1.5 locale-read fix verification

## Direct evidence

Authorized USB connection to OnePlus PKG110, Android15/ColorOS15, WeChat8.0.78. The original ordinary stack only showed null View.setVisibility in yf.queueIdle. Installed-code inspection identified NewChattingTabUI.c and creation in g(). A main-thread JDI probe observed SecurityException `getApplicationLocales: Neither user 10435 nor current process has android.permission.READ_APP_SPECIFIC_LOCALES`, with stack NewChattingTabUI.g bci519 -> MultiTaskUIC -> language context -> LocaleManager -> system locale service. This leaves the View uncreated before the later idle callback failure. An earlier Byte-null exception was handled locally by WeChat and was not used as the fix target.

## Scope

SDK33+ locale service read proxy, explicitly requiring non-null bound guest configuration, WeChat virtual-user0, own-package query and actual host system-user. Clone arguments, map package to actual host and invoke original binder. No read permission grants, setters, other-package/user changes, exception suppression or guest APK edits. Existing host LocaleManager reference and ServiceManager locale cache point to the proxy.

## Verification

- Red/green own-locale policy check; 110 Java assertions pass, including argument cloning and other-user/package/signature exclusions.
- Read-only review found no blocking issue; suggested bound-config guard was added before final delivery.
- Final Android build succeeded; APK versionCode6/versionName0.1.5, same signer, ARM64 ELF, attribution and source-archive checks passed.
- ADB `install -r` succeeded, preserving existing data/login. No clear-data/uninstall was performed.
- Before fix, login succeeded but initial home/chat preloading produced the null-View crash. After fix, actual device shows the WeChat home/chat list under app.containerlab ProxyActivity$P0, not original WeChat's separately installed task.
- Final package retest sampled at10/20/30s: same guest main PID23852, zero new main-thread exceptions, foreground task remained container ProxyActivity$P0. Final screenshot visually confirms the home/chat-list UI. This verifies this startup path, not all WeChat features or long-term stability.
- Temporary debugger forwarding removed. Phone remains connected for user-directed follow-up.

SHA256:`1298c72bafe2ea46e7e404e3694f8dfe9d7a141e4a01f229f841d40c2a2435cf`.

Device screenshots/probe logs and the installed WeChat APK are private local research inputs under ignored `.tooling`, excluded from distributed source. Location is still not connected in this prototype.
