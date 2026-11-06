# Container camera permission repair

User requests USB diagnosis and asks the agent to test camera access; legitimate face verification remains a user-operated task. Do not capture face images or submit identity verification.

Observed on0.2.2 in the actual guest main process: checkSelfPermission(CAMERA)=-1, host runtime CAMERA granted=true/AppOps foreground, currentOpPackageName=app.containerlab, legacy camera0/back and1/front both open successfully and release without capturing frames. This establishes a mismatched permission-query result; it does not establish successful face verification or live preview.

Repair the existing narrowly scoped HostPermissionPolicy to include CAMERA. Existing adapters must still invoke the original system Binder with the actual host identity and preserve actual denial. Keep WeChat/user0 scope, current PID/own UID checks and package/device/user exclusions. Do not map microphone or unrelated permissions. Add a compact camera permission line for the main guest process and bump to0.2.3.

The real permission policy test first fails on CAMERA UID mapping. Extend its existing scope/metadata checks to CAMERA and preserve exclusions for SMS, microphone and Wi-Fi changes. After repair, run the suite, build/package/signature/ARM64 checks, install same-signer without clearing data, repeat real guest permission and front/back open/release checks. Check API2 enumeration where practical. Record preview and user face verification as unverified until separately observed.
