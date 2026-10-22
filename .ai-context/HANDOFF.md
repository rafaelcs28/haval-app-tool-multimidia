# Handoff

## Current task

Prepare signed OTA `1.0.0.74.123-preview` from the private signed `74122` base with manual CarPlay navigation capture.

User approved OTA generation/signing and installation after notifying them that the APK is ready.
The car was offline during implementation. Wait for the user to turn it on; install while stopped,
then have the user start Resources > Diagnóstico de navegação CarPlay > Iniciar captura.

## Diagnostic extension

- Netseek PR 14 is imported at `15ce75fa07d4cbe4bedd00f308a9ce2e298a6cf0`.
- `navigation/CarPlayGuidanceReader.kt`, `CarPlayGuidanceParcel.kt`, `CarPlayCaptureGate.kt`,
  `CarPlayCaptureFiles.kt`, `CarPlayNavigationCapture.kt`, `CarPlayCaptureScreen.kt` implement capture.
- `FeaturesHubScreen.kt` adds its manual entry. Existing AA/HUD/CarPlay visual code is unchanged.
- The reader/queue/files have JVM tests; the actual Android Parcel adapter self-tests before subscription.
- `docs/architecture/carplay-navigation-capture.md` describes lifecycle, privacy, retrieval and vehicle steps.
- Records are in device-protected `noBackupFilesDir/carplay-navigation`, three files of at most 1 MiB each.
- Capture is off by default, lasts at most sixty minutes, continues after leaving its screen and ends on process death.
- Source rules and transaction layout derive from PR 14 static evidence, not a live car confirmation.
- Binding uses flags zero and transactions 7/8 only. No service auto-start, focus, Surface or video commands.
- Check actual callback counts before the drive; oneway subscription success alone is insufficient.

## Delivery and remaining vehicle validation

- Built commit: `8918d3a771acb0ab21f717a01e0583840a53f02a`.
- Mac Mini job: `haval-v9-sync-20261021-8918d3a771acb0ab-20261022T002004Z-18745`.
- JUnit XML: 88 reports, 791 tests, zero failures/errors and one skipped. Release, R8 and lint vital passed.
- The first run exposed accepting directories as rotating log files; regular-file guards fixed the regression before publication.
- Certificate SHA-256 matches 74117–74122: `5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0`.
- Published release: `https://github.com/rafaelcs28/haval-app-tool-multimidia/releases/tag/1.0.0.74.123-preview`.
- APK SHA-256: `49835cf40f4a307be98b5e81d831a0c32746d0a881b938d6800964fad0d4b526`.
- Downloaded APK matched the hash before updating app-latest. Downloaded fixed OTA manifest matches the local 74123 manifest.
- Source is saved in private branch `rafaelcs28/haval-impulse:release/impulse-74123-carplay-capture`; the post-build handoff commit changes documentation only.
- Coordinator artifacts and detailed signing handoff remain under `/root/projects/haval-impulse`.
- Installation is still pending the user's car-online message. The diagnostic remains off by default until the user starts it.
- User will message at arrival; collect all private JSONL files via authorized Telnet/Tailscale.
- Do not claim live monitoring, successful Binder delivery, or parser self-test execution until the car is online.

## Previous release context

## Changes

- Applied upstream PR commit `749e34e500bdf85616abe473b50ace6eedcd1df9` as a clean cherry-pick.
- Added source-specific OSM, ANTT and road refresh state and partial-failure retries.
- Added safe cache replacement and validation fallback to APK-bundled radar data.
- Added focused unit tests and updated radar architecture documentation.

## Risks

- The first run may check ANTT and make road data eligible sooner because legacy installs do not have all new per-source state.
- Manual refresh forces camera sources; road association remains governed by its seven-/thirty-day policy.
- Vehicle behavior still requires post-install observation; no production cache should be deleted to test failure handling.

## Validation

- `git cherry-pick` completed without conflicts.
- `git diff --check` passed.
- Full Android validation must run only through the authorized Mac Mini release queue.

## Previous release steps

1. Submit the immutable final commit as release `74122` / `1.0.0.74.122-preview`.
2. Verify queue tests, release APK, SHA-256 and production certificate fingerprint.
3. Publish the versioned GitHub prerelease and then atomically update the fixed `app-latest/app-preview.json` OTA manifest.
