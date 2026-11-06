# Location diagnostics update

Scope: investigate the user's OnePlus Ace 5 / ColorOS 15 report: Baidu initially shows the target then actual location after about 10 seconds; the service still reports running. Mini-program location also differs. These observations do not establish whether service updates stop or target SDKs ignore/replace system mock results.

- [x] Add failing tests for stale heartbeat, maximum scheduling gap and fresh/stale/mock/mismatching readback classification.
- [x] Add UpdateHealth metrics and per-provider last-known-location readback with age, mock marker and distance classification. Explicitly label these as cached system results, not target-app acceptance or fresh sensor requests.
- [x] Display update count/last update/gaps, mark delayed heartbeat instead of claiming fresh injection, persist recent diagnostics locally and expose user-controlled clipboard copy. Provide battery settings shortcut without forcing exemptions or additional background permissions.
- [x] Ship version 1.0.1 using the existing signing key. Keep 1.0.0 artifact. Update README with diagnostic procedure and SDK compatibility limits.
- [x] Run core tests, compile and signature checks; independent review; package sources. Actual ColorOS and WeChat/Baidu behavior remain pending user-device validation.

No root, hooking, mock flag removal, target-app modification, network identity changes, or unsupported claims of a universal fix. Do not change injection frequency blindly.
