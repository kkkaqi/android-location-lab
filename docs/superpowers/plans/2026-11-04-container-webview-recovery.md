# Container verification-page recovery investigation

User authorizes continued repair and USB diagnosis of container WeChat. The current evidence contains distinct blank LauncherUI and blank MMWebViewUI states; neither proves successful login. Locale reads currently work. LauncherUI's logged-out redirect can reach LoginPasswordUI through the virtual activity manager; broad caught exceptions are not by themselves causal.

Implementation within the existing authorized debugging scope:

- Fix the observed ProxyServiceRecord.create(null) crash by representing a restarted service with no virtual target as an empty record. Existing dispatcher guards then return without launching stale guest components. Add a regression that executes the actual record factory with null.
- Add a debug-build-only, fixed USB probe entry on LabActivity. It opens only a container WeChat public-test MMWebViewUI and adds a local raw XWeb widget through the existing guest lifecycle callback. Require bound WeChat/user0, exact test marker and exact public URL before touching the view. Test HTML explicitly identifies itself and performs no login, network submission or verification. No arbitrary target/URL parameter is accepted.
- Log only guest Activity class lifecycle names so copied diagnostics can distinguish launcher, login and web pages without recording intent extras.
- Package a same-signer upgrade as0.2.1, preserving private data. Use the local page for foreground/background reproduction and native signal observation from child startup, forwarding natural signals and retaining Chromium sandboxing. Do not label verification-page/login/location adoption fixed without actual device evidence.

Remaining: obtain an evidence-backed cause and validate a repair of the real verification flow. The USB probe is instrumentation, not a claim of that repair.

## Follow-up: renderer OEM scheduling compatibility

The fixed local page now renders visibly (progress100, height860). Identity checks use bound guest configuration because Activity Context package names are rewritten to the host. The probe reuses its page-owned widget and uses a unique action.

Two native observations captured SIGSYS/code1/syscall29. The second captured ioctl request0xc030de02 and fd named ctrl. On this same device, the app has /proc/oplus_frame_boost/ctrl open; its libSchedAssistExtImpl::ofbBoostHint contains exactly this ioctl command, and boot ChoreographerExtImpl unconditionally invokes ofbBoostHint at frame start. This is stronger evidence for an OEM scheduling/sandbox conflict, but the relationship to the user's real verification white screen remains to be retested.

0.2.2 will opt out of the optional OEM scheduling implementation using the system's DEFAULT feature, only inside bound user0 WeChat sandbox renderer processes on OnePlus/OPPO Android15. Set the process-local feature cache and any existing main Choreographer extension before guest Application creation. Keep seccomp, fatal signal delivery, verification and outside apps unchanged. Validate cache/extension identity and repeated public-page foreground transitions on device before requesting another real login test. Add scope regressions including non-renderer, outside/unbound, other user, OEM and SDK exclusions.

Also preserve the complete original pending Activity envelope after process restoration. If synchronous restart fails to restore AppConfig, leave the proxy envelope to the existing finish-only ProxyActivity rather than infinitely front-queueing the transaction. The attempted OS kill/restore regression removed the test task on this ROM, so this path lacks an end-to-end success regression and must be reported as such.
