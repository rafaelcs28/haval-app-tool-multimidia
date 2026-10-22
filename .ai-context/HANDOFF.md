# Handoff

## Current task

Current follow-up: user requested experimental CarPlay guidance. Local uncommitted code
adds CarPlayHudPolicy, CarPlayExperimentalHud and tests, routes only the two AA native-HUD
calls through a priority selector, and adds a session-only switch in CarPlayCaptureScreen.
Capture lifecycle supplies generation-scoped inputs. This phase is native HUD text only;
no arrows/codes/ETA are inferred, no theme/bridge/telemetry or AA map change. Full design,
cleanup limits and test procedure: docs/architecture/carplay-experimental-hud.md.
Source diff validation pending below; Android/unit execution requires the Mac Mini queue.
User approved commit/push and signed 74127 generation, without automatic installation.
Audit the immutable commit and use the Mac Mini release queue; verify tests, production
certificate and downloaded OTA before reporting readiness. No live validation claimed.

Latest 74126 vehicle tests: Waze-only 6 inactive callbacks; mixed Waze/Maps 31 callbacks
with 14 state-1 (source app unidentified); Maps-only 109 with 98 state-1 and two state-2
arrival payloads that retained populated road/maneuver fields. No errors/drops; all three
manually stopped. Private raw source: coordinator test-artifacts/carplay-navigation/
20261022-126-three-tests/capture-0.jsonl (479 cumulative records, 212 new).
SHA-256 31bc64b6ed2a086ce2a7a50c0a50698089c50c6629f20e8696542d5c67db922a.

## Previous 74126 task

User authorized commit/push and signed OTA 74126 based on private 74125 commit
`0fcb11d03671eb89f3689bb55386bb1ea9963386`. Compared with 74124: two commits add the HEV
long-press overlay crash fix and readability UI changes; retain all 24 changed files.
Only CarPlay diagnostic screen/collector, a pure summary classifier and tests are modified.
No map-clean activation, AA, OEM APK, display/Surface/focus or bridge change is planned.
Added exclusive diagnostic payload counts, session duration and route start/end markers.
Built immutable source `68acf612a2dbcf9d744aaf8c7305eed4435b25a3` in Mac Mini job
`haval-74126-work-68acf612a2dbcf9d-20261022T215909Z-9946`: release/R8/lint vital passed
in 4m58s; 90 JUnit reports, 812 tests, zero failures/errors, one skipped (11 new summary tests passed).
Subsequent documentation changes do not alter this built source.

OTA 74126 published and independently downloaded/verified; fixed app-latest manifest
verified byte-for-byte. APK size 76056095; SHA-256
`4d2ed8b0ebe3a744f588c8e7f24565a4bee05f0e9bce732e462240151de51abc`.
Production certificate matches 74117:
`5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0`.
Release: https://github.com/rafaelcs28/haval-app-tool-multimidia/releases/tag/1.0.0.74.126-preview.
Coordinator artifacts: /root/projects/haval-impulse/artifacts/release-74126-68acf612
and artifacts/ota-74126. No 74126 install or road validation claimed.
Combined private report for download: /mnt/data/Relatorio-CarPlay-Haval-Waze-AppleMaps-2026-10-22.md.
The collected cumulative JSONL contains 267 records (63 Waze plus 204 Apple Maps); its
SHA-256 is da1eb760b58aa3b206be0c97d713cad3122230149f4b473f189ba323147ccdab.

Real capture on 2026-10-22 was Waze (user confirmed): two inactive/default callbacks during
546.715 seconds, zero errors/drops, clean manual stop. No useful active-route data or video
stream proven in that Waze session. Follow-up Apple Maps on installed 74124 delivered
177 callbacks over 197.647s, including 158 state-1 payloads with roads/distances, zero
errors/drops and a clean stop. Active maneuver IDs 1/2/29/47 remain raw, not mapped to AA
arrows. No video stream proven. Raw records and combined downloadable report stay private
on VPS. Compare Waze using the same start/connect ordering; no Apple Maps repeat is needed
just to establish basic delivery. 74126 is diagnostic only, not runtime TBT integration.
Installation requires a fresh parked-in-P check. This trip was user-driven; do not start
or stop the car. For future agent-initiated maintenance startup, user requires shutdown
on completion unless explicitly requested otherwise, after confirming stationary in P.

## Previous task (historical)

Prepare signed OTA `1.0.0.74.124-preview` from the private signed `74123` base with the remaining v9 HUD updates and two protective fixes.

User requested all reviewed v9 changes with protections, explicitly including `97f8ef1c`.
The car is offline. Do not install 74124 automatically; publish/verify OTA and report readiness.
74123 was installed on 2026-10-21 21:32:20 -03 after the user confirmed parked in P.
User did not drive and capture was not started. A fresh manual capture is still needed at departure.

## 74124 changes and pending validation

- Imported `97f8ef1cbfbc45545a7581a950c9eb77a8448c0a` and `414a20fb2a99cbceaacad753d522289945f420b8` as clean cherry-picks.
- AR navigation map stays visible beside a radar card; roundabout progress retains approach history through exit-road renaming.
- Ordinary/non-roundabout steps reset the tracker before any new entry detection.
- Radar warning thresholds/output have a clamped 20-metre lead, but geometry/minima and passed-camera timing retain original coordinates.
- Added regression tests for reset boundaries, early warnings, real-position retention and late acquisition for cameras with/without road geometry.
- No changes to CarPlay capture, cluster recovery, OEM assets, theme sources or bridge contract.
- `git diff --check` and full Mac Mini unit/release validation required before publication.
- Signing identity remains the pinned 74117 certificate. Release code 74124 must exceed the current OTA 74123.

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
- 74123 installation was confirmed via Package Manager; the diagnostic remains off until manually started.
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
