# Container WebView network permission repair

User device reaches real WeChat8.0.78 login; official login precheck page fails ERR_CACHE_MISS. Do not fetch the user's ticketed URL or alter authentication, TLS, server responses, or account verification.

Evidence: Chromium AwSettings initializes network blocking using Context.checkSelfPermission(INTERNET); Android15 uses checkPermissionForDevice(permission,pid,uid,deviceId). Current core lacks this device-aware hook and leaves virtual package network grant metadata empty. Upstream issue68 describes the same failure mode, but user-device root cause remains a hypothesis pending test.

- [x] Write failing pure Java tests for scope, identity translation, argument preservation, API variants and denied-host metadata.
- [x] Bridge only INTERNET/ACCESS_NETWORK_STATE for current WeChat guest in virtual user0 to actual host permission checks. Preserve device identifiers and respect actual host denial; leave unrelated permissions unchanged.
- [x] Cover AMS, legacy package manager, Android15 permission manager and declared network permission metadata. Add redacted diagnostics for actual guest network grant results.
- [x] Build 0.1.1 with same signing key; preserve existing container data. Verify policy tests, compile/signature/ARM64 checks and independent review.
- [ ] Revalidate login page on user's phone; no claim of login success without it. Pending user device test after delivery.
