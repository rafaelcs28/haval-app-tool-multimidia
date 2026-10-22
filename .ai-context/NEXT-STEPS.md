# Next steps

Current 74126: run full Mac Mini unit/release validation on an audited immutable commit,
verify pinned production certificate, publish APK and independently verify OTA manifest.
Then install only while parked in P. Test Apple Maps with capture started before route
creation; compare Waze separately. 74125 already captures the raw fields for that test.
Do not claim map-clean support or add display/video patches based on two inactive callbacks.

## Historical 74124 checklist

1. Submit the audited 74124 source commit to the Mac Mini release queue; validate tests, APK version/hash and production certificate.
2. Publish versioned 74124 release, verify downloaded APK, then update and verify app-latest manifest.
3. Notify the user. Car is currently offline; installation is a separate parked-car step.
4. During a real trip, validate roundabout progress and radar/map coexistence, including radar retention through its true coordinate.
5. Start the manual CarPlay diagnostic in Resources only when the user is ready to drive; verify callbacks and later retrieve private JSONL.
