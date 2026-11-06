# Verification-page white screen: incomplete investigation

Initial investigation phase (historical): unresolved. No product source changes, new APK build or installation were made during this investigation. The phone's installed APK SHA256 was verified as `ad2252f9b32bfa9453b2071c7a2dc4a82b05364444f8adba7a857d68164e027a`, the intermediate 0.2.0 build. The final diagnostic-gated APK remains separate.

## Reproduction and observations

The user logs the same WeChat account into the original outside app to complete the requested QR verification, then returns to the container and sees a white page. A further user-driven fresh verification attempt also produced white screen.

- Foreground window was container-owned MMWebViewUI, not a completed-login home page. Read-only JDI inspection showed its URL origin/path as `https://weixin110.qq.com/security/acct/newreadtemplate`. Query parameters were not printed or replayed.
- Container main process12666 and XWeb privileged process13337 remained alive. Multiple XWeb sandbox children exited with ApplicationExitInfo reason2/status31. Representative exits included18544 at09:55:01,23976 at10:17:08,7005 at10:30:23 and13633 at10:50:44. Status31 is a signal observation; no matching si_code or disallowed syscall was captured, so seccomp causality is unproven.
- Renderer status showed NoNewPrivs1, Seccomp2, two filters, and libxwebcore loaded. The filter count applied to all enumerated renderer threads.
- Attaching JDWP directly to renderer18544 failed its handshake and coincided with that renderer's status31 exit. Direct renderer JDWP was abandoned. Parent-process JDWP inspection worked.
- A fixed, unrelated public test MMWebViewUI was opened through the bound guest Application's normal startActivity path. It initially showed a native network-error overlay and also triggered the ordinary login screen. This alone did not prove an engine failure.
- On that public test widget only, offline HTML rendered: progress100, title `Container offline rendering test`, content height1886. Loading the public Tencent WeChat homepage subsequently produced its expected title and content height3130. Thus current XWeb can render offline content and fetch/render HTTPS; that does not establish verification-page success. A stale native error overlay could cover successfully rendered content in this diagnostic path.
- No verification URL was manually reloaded, replayed, rewritten or replaced by test HTML. No credentials, verification codes, messages, positions or attendance submissions were entered by the agent.

## Diagnostic limitations and excluded hypotheses

- The temporary Java WebErrorProbe initially watched incorrect/obfuscated Chrome-client methods. A title callback was observed but StackFrame.getArgumentValues failed with JDWP error35. Its result is not a captured verification error code. It is not a working reusable observer yet; do not ask the user to repeat verification relying on it unchanged.
- The real widget's getWebViewClient returned `com.tencent.mm.plugin.webview.core.f3`, extending core.s0 and XWeb h1. Local inspection identified old error callback r(WebView,int,String,String), modern error callback s(WebView,x0,w0), HTTP callback u, and renderer-gone callback w. WebChromeClient nt5.o2 is a different class and was the earlier incorrect observer target.
- Static last-error field ui.tools.ia.b was0 after the latest attempt; existing MMWebViewUI E3/M1/q2/p2 flags were false. These are limited snapshots, not evidence of successful loading or no errors.
- UnixFileSystemHook.getSpace0 has an apparent source JNI return-type mismatch, but actual guest File queries returned total235111829504, free70833516544 and usable70699298816 bytes. No disk-space truncation was observed, so it was not changed as a white-screen fix.
- Read-only review found a service-instance virtualization gap: bindServiceInstance can preserve an instance name while targeting a nonisolated ProxyService; virtual service/process caches also omit instance identity. No runtime evidence established that this specific path caused the current white screen, so it was not patched speculatively.
- No executed native syscall hook or background-monitor call was proven responsible for status31. Do not disable Chromium sandboxing, suppress fatal signals, forge permissions or alter security verification outcomes as a workaround.

## Native observation

A temporary own-UID native ptrace check succeeded without root. A bounded syscall tracer attached to all37 threads of renderer14321 using PTRACE_SEIZE/TRACESYSGOOD/TRACECLONE/TRACEEXIT. Two bounded observation runs did not reproduce SIGSYS and ended normally. TracerPid was verified0 afterwards. All debugger port forwards were removed. No target code/register changes or signal suppression were used.

Private helper sources, local WeChat code inspections and logs reside under excluded `.tooling/diagnostics` and `.tooling/research`; they are not distributed in the source archive. Phone screenshots can contain personal information and must not be shared or packaged.

## Remaining work

Capture a native exit during the actual verification transition, with a correctly prepared observer, then establish its causal relationship to the white screen before changing engine behavior. Current evidence does not justify blaming the user's QR steps, claiming the account has logged in, or claiming a fix. Container-only location framework checks remain separate from actual WeChat/mini-program acceptance. The phone was left at the container login page; no data clearing or uninstall was performed.

## Subsequent 0.2.1 / 0.2.2 work

The above is the earlier evidence record, not the current installed version. Product repairs and a strictly scoped debug probe followed. The local test page was visibly rendered repeatedly with progress100/content height860; guest lifecycle checks now use bound identity because the core rewrites Context package names. The probe reuses the page-owned XWeb widget.

0.2.1 guards the observed null service restart Intent and preserves the original cold-restored Activity envelope instead of replacing its target with the launcher. Failed synchronous process restoration now falls through to the finish-only proxy instead of endlessly front-queueing. The attempted OS process-death regression removed the task on this ROM; successful cold restoration remains unverified.

Additional signal-only traces of own-UID sandbox renderers17089,22953,28951 captured SIGSYS/code1/syscall29. Two recorded ioctl request0xc030de02 with fd basename ctrl. The device app has /proc/oplus_frame_boost/ctrl open, and the same device's libSchedAssistExtImpl::ofbBoostHint contains exactly this command. Its ChoreographerExtImpl.doFrameStartHook calls ofbBoostHint unconditionally. Subsequent clone220 failure in libwechatcrash is secondary crash-handler activity; do not treat all exiting threads' last syscalls as primary causes. One clean public-page close also completed normally, so this failure is intermittent. No renderer JDWP was used for these new runs.

0.2.2 opts out of this optional OEM scheduling feature only for bound WeChat/user0 sandbox renderers on OnePlus/OPPO SDK35. Uses the platform DEFAULT feature in that process's OplusFeatureCache and the existing main Choreographer extension. Leaves seccomp and signal delivery intact. The system interface and native library inspected locally are private diagnostic inputs, excluded from distribution. Reviewer notes that other preexisting thread/SF Choreographers are not patched; mechanism validation must match the observed main-thread path.

Controlled A/B validation then confirmed a delayed renderer Choreographer frame crashes with the original OEM implementation (renderer16053, ioctl0xc030de02) and completes with the DEFAULT implementation (renderer13708). Four public-page foreground/background transitions also passed. Final installed APK SHA256 was checked against the distributed0.2.2 file. The user subsequently completed the normal login/QR flow and explicitly confirmed entry into WeChat at approximately13:53. Verification renderers19720/19935 ended with exit0 in the collected traces. Real mini-program location adoption remains pending; see VERIFICATION-0.2.2.md for the full results and artifact identity.
