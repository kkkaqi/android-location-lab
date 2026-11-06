# Container-only location implementation

- [x] Add red/green policy tests for coordinate bounds/non-finite values, package/user/binding/OWN_MODE scope and callback shapes.
- [x] Implement guest-only modern callback adapter and cancellable periodic subscriptions using the existing per-package location config.
- [x] Add host controls, verified persistence and virtual-only restart; update diagnostics/version.

Review fixes included bypassing all legacy mock hooks outside the strict own scope, respecting LocationRequest maxUpdates/duration with monotonic expiry, and cleanup on failed registration. Private host log is used for guest framework audit, separate from actual WeChat adoption.
- [x] Reproduce cached LocationManager bypass on device; preserve and wrap the current delegate after guest Application initialization.
- [x] Map own-guest location permission queries to actual host grants/denials, preserving denied background location. Device readback changed from fine/coarse=-1 to 0, background remains -1.
- [x] Run 160 Java checks, read-only reviews, build and verify signed ARM64 package. USB runtime validation used an intermediate 0.2.0 build; final additional diagnostic permission gate is built, not yet installed while the user is handling login.
- [x] Validate enabled Wuhan, changed Shanghai, disabled last-cache reads and callback cancellation through guest framework APIs; recheck system mock switch and outside mock-app authorization.
- [ ] Install final diagnostic-gated build when it will not interrupt user login; observe actual WeChat location page and mini-program without sending messages or attendance submissions. Phone currently reports login expired, requiring user interaction. Do not mark actual adoption complete.

2026-11-04 evidence and artifact identities: container-lab/VERIFICATION-0.2.0.md. No clearing, uninstalling or account-restriction workarounds.
