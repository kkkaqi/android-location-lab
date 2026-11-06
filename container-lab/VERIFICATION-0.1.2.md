# 0.1.2 compact diagnostic verification

## Scope

The user reported that mobile chat truncated the full diagnostic. This update only summarizes the existing private host log for on-screen display and clipboard. It does not fix or suppress WeChat's safe mode, add exit-history collection, or modify the engine or permission patch.

## Evidence

- Red phase: the initial formatter retained the old full-export behavior; the new compact diagnostic test failed `mobile report must fit 1600 characters` with 300 repeated XWeb callbacks.
- Java checks: 9 import, 24 network mapping, 6 redaction and 24 compact summary assertions passed (63 total).
- Summary cases cover size bounds, old run omission, repeated process counts, network success/denial/recovery, launch-vs-visible-page distinction, absence-of-exception caveat, deepest cause and guest stack, historical errors after restart, sensitive URL parameter redaction, huge exception text, duplicate errors, historical version metadata and long process names.
- A read-only review identified four additional accuracy cases; regression assertions now cover a failed host attach as a fresh run, permission exceptions overriding old success, cross-process error deduplication and selecting a frame from the deepest cause segment. Follow-up review found no further important defects.
- Final Android build succeeded (`:app:assembleDebug`, ARM64, SDK35). Delivery packaging uses `tools/package_container.py` to validate the signature, application ID, ARM64 ELF architecture, license asset and source archive. APK metadata and hash are written to `dist/container-apk-info.txt` and `dist/container-SHA256SUMS.txt`.

## Device follow-up

Install 0.1.2 in place without clearing data. Open the host and copy the compact diagnostic; if reproducing a failure, return to the host immediately afterwards and copy again. Device validation remains pending. The latest two errors may be historical and are explicitly labeled; original private records retain the existing bounded rotation behavior.
