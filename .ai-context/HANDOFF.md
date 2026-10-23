# Handoff

## Experimental native bridge implementation and authorized validation

User authorized commit/push to private repository and Mac Mini compilation,
explicitly without OTA publication/vehicle installation for this stage.

New VideoBridge core defaults to OEM passthrough; enabled mode requires identity
before Initialize, independent secondary factory, bounded copies and no OEM
fallback for retired secondary streams. Includes AVC configuration parser and
AndroidSecondaryDecoder (own codec/window/worker, explicit tick converter).
Host ASan/UBSan bridge fixtures and AVC parser tests passed; backend requires Mac
Mini ARM64 compilation. Android build config pins installed NDK27.1/CMake3.22.1,
builds only this native ARM64 target with static C++ runtime and excludes its .so
from APK packaging. No runtime loader/JNI/IPC/advertisement or UI added.

Remaining integration gates are explicit in README/architecture: pre-creation
OEM interception, independent Surface transfer, clock calibration, ABI-boundary
exception handling and callback quiescence on pointer reuse/destruction. Host
tests use synthetic decoder/factory; they do not prove AMediaCodec behavior.
Do not install a build or remove the native packaging exclusion as a shortcut.

## Native component implemented; initialization-order gate identified

Added native/packet_conversion.{h,cpp}, observed_video_abi.h, host CLI and C++
fixtures under scripts/carplay-native-analysis. No Android linkage or hook.
ASan/UBSan: 14 fixtures + 10,000 seeded malformed-input cases passed. Actual
C++ conversion of a ten-frame H.264 fixture preserved decoded hashes. Twelve
Python tests passed with CARPLAY_NATIVE_DIR and CARPLAY_PACKET_CONVERTER supplied.

Ghidra 12.1.4 now installed privately at /root/.local/share/haval-analysis, official
archive checksum verified; existing JDK 21 used with command-local environment.
No persistent REA/global Java changes. REA CLI full Evidence/snapshots privately
beside native capture; corrected Ghidra base is 0x100000, not ELF zero base.
Stack DWARF confirms callback ABI and 96-byte ScreenStreamPrivate. Header is an
observed layout, not permission to cast/hook arbitrary builds.

Critical next design constraint: StartSession calls Create/Initialize BEFORE
SetDelegateContext/Start(UUID). A seven-slot hook that classifies only at Start
would already have touched the singleton decoder. Need identity above creation,
verified lifecycle/reentrancy, then isolated AMediaCodec receiver and reversible
startup integration before secondary-display negotiation. No APK/OTA or car access.

## Continued native investigation and real host decoder experiment

Completed a further offline pass rather than requesting another continuation.
CoreUtils ScreenStreamCreate explicitly dlopens libcarplay_proxy_ts.so and dlsyms
ScreenStream callbacks. AirPlay maintains per-string stream contexts; the string
and integer propagate through StartSession/Start to the stack, whose observed Start
body discards their meaning and retains the shared platform context. Numeric type
semantics and replacement/namespace safety remain unproven. On the inspected
ProcessFrame branch, completion/context are null; do not generalize to every caller.

audit.py now preserves adjacent printable ADRP/ADD literals; added an exact-library
loader regression and test_dual_decoder.py. Eleven tests passed (5 portable,
5 captured-artifact, 1 real FFmpeg H.264 decoder isolation experiment). Secondary
stop/restart with changed dimensions did not stop the principal Linux process.
This is NOT OEM/Android decoder validation or map-clean support. No Android build,
APK, commit/push or car command. Private lifecycle/frame/CoreUtils disassembly and
loader-audit JSON are beside the native capture on coordinator.

Remaining engineering gate: real native receiver/loader integration preserving
the principal OEM video, and verified advertisement/negotiation. Host decoding is
not a reason to ship a flag-only APK. See the architecture report for exact limits.

## Autonomous clean-map investigation follow-up

User requested continuing useful work without repeated follow-up commands. No
vehicle operations are needed for the current offline stage. Captured native
libraries are private on coordinator in test-artifacts/carplay-apks/20261022-205614-telnet;
car was shut down with fresh P/0 km/h telemetry after collection.

Added scripts/carplay-native-analysis/{audit.py,test_audit.py,requirements.txt,README.md}
and docs/architecture/carplay-secondary-video-investigation.md. Read-only hash-pinned
AArch64 audit validates PLT→GOT and resolves the seven gVideoCallback relocations.
Nine tests passed, including four using exact captured binaries; git diff --check
passed. No Android build, source commit/push, OTA or runtime modifications.

New finding: proxy CarPlayVideoInit stores a callback table; ScreenStream wrappers
forward to seven stack callbacks. Candidate integration boundary only: indirect
linker resolution, ABI, lifetimes, completion and stream identity remain gates.
Existing global media codec/window/platform context prevents assuming a flag-only
second stream. Next: ABI/ownership analysis, then actual offline dual-decoder proof
and reversible native integration design. No generic synthetic policy test can
prove actual independent video or clean-map support from the iPhone.

## Current task

Offline APK analysis completed after the 74127 drive; no new APK built. Private technical
report and 61 REA/JADX Evidence records are on coordinator under test-artifacts/carplay-apks/
analysis-20261022. NavigationProxy confirms raw 1 left, 2 right, 4 driving-side-dependent
U-turn, 12 destination flag, 47 sharp-left. Raw 28..46 encode roundabout exit raw-27;
compact image table has 35 entries with index raw-19 for 47..53. Four central methods
match the located stock app reference; nine selected glyphs have identical decoded pixels.
Installed APKs match the fork's bundled carplay_patches assets. Original service binary
03cc69d1... not found; don't claim a full original-service diff or factory authenticity.
Time units remain unconfirmed. Cache is composed from partial updates; stale text/angles
can survive state changes. SourceName/description upstream are dropped before the public
11-field Binder, so do not extend the Parcel reads. METER/HUD RemoteViews path exists
statically but no secondary video/map or active cluster integration is proven. Next proposed
release would add only verified HUD glyphs, with unknowns blank and no ETA, preserving AA.

## Previous delivery status

Current follow-up: user requested experimental CarPlay guidance. Built source
adds CarPlayHudPolicy, CarPlayExperimentalHud and tests, routes only the two AA native-HUD
calls through a priority selector, and adds a session-only switch in CarPlayCaptureScreen.
Capture lifecycle supplies generation-scoped inputs. This phase is native HUD text only;
no arrows/codes/ETA are inferred, no theme/bridge/telemetry or AA map change. Full design,
cleanup limits and test procedure: docs/architecture/carplay-experimental-hud.md.
Source diff validation and full Mac Mini unit/release/R8/lint vital validation passed.
User approved commit/push and signed 74127 generation, without automatic installation.
Audit the immutable commit and use the Mac Mini release queue; verify tests, production
certificate and downloaded OTA before reporting readiness. No live validation claimed.

IMPORTANT scope clarification from TelasScreen: existing native HUD targets physical
display 4096 (windshield), not cluster display 3. Current implementation does NOT add
guidance to the cluster. User was informed and explicitly chose to receive 74127 for
the windshield-HUD test; publication may proceed after artifact validation. Cluster
integration is deferred separately. Unit results: 826 tests, zero failures/errors, one skipped; all 14
new policy tests passed. Build source: 3fc8ce4f203655f7e0fb0128199793a6aed6b558;
Mac Mini job haval-74126-work-3fc8ce4f203655f7-20261022T223038Z-14173.

74127 published; downloaded APK and fixed OTA manifest independently verified.
APK SHA-256 ccb606475373170680327dc96f93088955630afb0eb1f4229ef20ff1ea8bf0e3,
76072475 bytes; certificate matches pinned 74117 identity. User explicitly authorized
remote start, parked installation/version verification and shutdown afterwards.
User later took over manual installation. Coordinator automation was terminated at
download_started BEFORE any package install command. Do not claim 74127 installed.
First start failed at Telnet and shutdown was confirmed after an off retry; second
engine-on did not produce a started state and ended with fresh engine_state 0. Final
download-only attempt used the already-online head unit in P/0 km/h with engine_state 0.
Do not send further vehicle commands; user resumed control and is going to the car.
Receipt/details: coordinator artifacts/ota-74127/install-receipt.json and
manual-install-status.txt. Feature remains off by default. OTA 74127 is ready for manual
installation while parked in P. No navigation/visual test of 74127 has been performed.

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
