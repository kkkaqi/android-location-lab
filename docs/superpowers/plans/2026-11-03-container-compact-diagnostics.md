# Compact mobile diagnostics implementation plan

**Goal:** Make the copied container diagnostic fit a single mobile message, preserving useful failure evidence.

**Architecture:** A pure Java formatter reads the existing private log without changing its storage or guest behavior. Both the on-screen diagnostic and clipboard use the same summary, limited to 1600 UTF-16 characters. Summarize the latest host run, retain the two latest distinct errors across runs with historical labels, collapse process callbacks and permission checks, and redact before formatting.

**Scope:** Version 0.1.2, same package/signing key, no data removal, no engine or permission changes, no new crash collection. An absence of recorded exceptions must not be described as an absence of crashes.

- [x] Add failing tests for bounds, repeated events, run boundaries, historical errors, nested exceptions, permission failures and redaction.
- [x] Implement `DiagnosticSummary`, connect `LabLog.report`, update the clipboard label and version.
- [x] Run the full Java checks, compile the ARM64 APK, verify package/signature and source archive, document limits.
- [x] Prepare the APK for an in-place upgrade and instructions to send the new short diagnostic. Device retest remains pending.

Delivered artifact: `dist/wechat-container-lab-0.1.2.apk`, versionCode3, same signer as0.1.1; 63 Java assertions passed. SHA256: `4a1dc866ddc649c409ad20025ee67f7e7b352f148b70ed3a4a658d15624d5385`.
