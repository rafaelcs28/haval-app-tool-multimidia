# Next steps

Current experimental text code: user approved commit/push/build. Review diff and tests,
then run authorized Mac Mini unit/release validation
before any 74127 publication. No installation authorization is implied. Native HUD only,
default off, no theme contract changes or turn-code guesses. See carplay-experimental-hud.md.

## Previous 74126 delivery

Current 74126: Mac Mini tests/release passed (812 tests, zero failures/errors, one skipped).
Returned certificate/APK, published download and fixed OTA manifest verified. Install only parked in P.
Apple Maps already delivered active-state text/distance data on 74124; next compare Waze
with identical capture-before-connection/route ordering. Investigate OEM maneuver/unit/time
semantics before an opt-in TBT adapter; preserve AA and do not claim map-clean video support.

## Historical 74124 checklist

1. Submit the audited 74124 source commit to the Mac Mini release queue; validate tests, APK version/hash and production certificate.
2. Publish versioned 74124 release, verify downloaded APK, then update and verify app-latest manifest.
3. Notify the user. Car is currently offline; installation is a separate parked-car step.
4. During a real trip, validate roundabout progress and radar/map coexistence, including radar retention through its true coordinate.
5. Start the manual CarPlay diagnostic in Resources only when the user is ready to drive; verify callbacks and later retrieve private JSONL.
