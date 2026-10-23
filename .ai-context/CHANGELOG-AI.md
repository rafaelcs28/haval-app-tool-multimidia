# AI change log

## 2026-10-22 — native bridge Mac Mini validation passed

- Private source commit 07567766ab6746a5fcc10ca2dfa64a06463e9915 pushed with consent.
- Audited/allowlisted Mac Mini job built and linked ARM64 backend and assembled
  Debug successfully in 2m9s; no Android build occurred on the coordinator.
- Fetched hashes verified; bridge .so/C++ shared runtime excluded from APK and
  all four existing APK ABIs retained. Debug artifact is not an OTA candidate.
- Native bridge passed ThreadSanitizer; all host ASan/UBSan and 12 Python tests
  passed. No Android runtime/CarPlay or vehicle validation claimed.

## 2026-10-22 — default-off native bridge prototype

- Implemented fixed-route OEM/secondary core, identity-before-creation gate,
  per-stream serialized decoder ownership and independent secondary shutdown.
- Added bounded avcC SPS/PPS parser and Android codec/window/worker backend,
  requiring explicit timestamp conversion; unknown completion variants rejected.
- Host ASan/UBSan routing/failure/concurrency tests and AVC fuzz fixtures passed.
- Added compile-only ARM64 CMake integration, excluding the result from APK and
  statically linking C++ to preserve existing runtime dependencies/ABIs.
- User approved private commit/push and Mac Mini validation only; no OTA/install.

## 2026-10-22 — first isolated native component and recovered callback ABI

- Recovered exact callback signatures/layout from captured stack DWARF and
  cross-checked main display/Start/ProcessData with Ghidra through REA CLI.
- Identified Initialize-before-UUID ordering; table-only routing is unsafe.
- Added bounded native NAL conversion and observed ABI header, not linked to app.
- Host ASan/UBSan passed 14 fixtures + 10,000 seeded cases; twelve Python tests
  passed, including real H.264 native-conversion/decode hash equality.
- No Android build, interception, vehicle change, publication or clean-map claim.

## 2026-10-22 — native loader/identity tracing and dual host decoder test

- Traced explicit CoreUtils proxy dlopen/dlsym and AirPlay per-string identity path.
- Extended audit with adjacent printable literal evidence and loader regression.
- Added real synthetic H.264 FFmpeg decoder lifecycle test with independent
  secondary stop/restart and changed dimensions; all 11 tests passed.
- No OEM/Android decoder, second CarPlay negotiation, vehicle or APK claims.

## 2026-10-22 — offline CarPlay secondary-video audit

- Added hash-pinned, read-only AArch64 function/PLT/callback-table auditor and tests.
- Identified seven stack video callbacks forwarded by the native proxy, without
  treating that boundary as a safe hook or proven second stream.
- Nine Python tests passed with private captured binaries; no OEM files committed.
- Documented native singleton blockers, loader/ABI/ownership gates and real
  dual-decoder validation requirements; no Android/runtime/vehicle changes.

## 2026-10-22 — experimental windshield HUD CarPlay text (74127)

- Added session-only opt-in text feed, generation/expiry/priority policy and regression tests.
- Only AA native-HUD deliveries use the selector; all AA fields and telemetry remain intact.
- Shared theme arrow fallback makes unknown CarPlay codes unsafe; therefore no CP JSON or guessed arrows are published.
- State-1 text only; arrival/pending/recalc/null/unknown states withdraw input, with documented existing HUD hide delay.
- Updated private-session evidence and architecture; no raw route data committed. Mac Mini signed release/R8/lint vital passed, 826 tests, zero failures/errors, one skipped; all 14 policy tests passed.
- User explicitly accepted windshield HUD display 4096 rather than cluster display 3 for this test. APK/OTA published and downloaded hashes verified; certificate matches 74117. User later took over manual installation; automation stopped at download_started before package install. Prior shutdown confirmed; no further vehicle commands authorized by this continuation.

## 2026-10-22 — OTA 74126 diagnostic preparation on 74125

- Compared 74125 against 74124; preserved HEV overlay crash fix and readability changes.
- Added pure, tested diagnostic payload classification; raw values and wire transport remain unchanged.
- Added null/inactive/candidate/unconfirmed counters, duration and route start/end markers.
- Prevented misleading “Orientações recebidas” status for non-null inactive/default callbacks.
- Waze vehicle capture delivered inactive defaults; follow-up Apple Maps provided 177 callbacks including 158 active-state payloads with roads/distances. No video stream proven.
- No private raw logs/report committed; Mac Mini unit/release/R8/lint vital passed: 812 tests, zero failures/errors, one skipped, including 11 new summary tests.
- Signed 74126 published, downloaded APK hash matched and fixed OTA manifest verified; pinned certificate matches 74117. No installation or road validation of 74126 performed.

## 2026-10-22 — OTA 74124 preparation

- Imported upstream `97f8ef1cbfbc45545a7581a950c9eb77a8448c0a` and `414a20fb2a99cbceaacad753d522289945f420b8` without conflicts.
- Kept AR map/radar coexistence and roundabout countdown changes.
- Guarded null roundabout steps before carrying approach minima across identities.
- Kept raw radar geometry and passed-camera thresholds while applying the 20-metre lead once to warning thresholds and displayed distance.
- Added six roundabout regression tests and three radar tests, each radar case exercising both road geometry and line fallback.
- Updated radar architecture and upstream provenance. Full unit tests and signed release assembly are assigned to the Mac Mini queue.

## 2026-10-21 — OTA 74123 preparation

- Imported Netseek PR 14 at `15ce75fa07d4cbe4bedd00f308a9ce2e298a6cf0` onto the signed 74122 base.
- Added manual sixty-minute CarPlay navigation capture in Resources with counts and app/incident markers.
- Added bounded Parcel decoding and a real-Android self-test before subscribing; unknown enums remain raw.
- Added generation-scoped bindings, explicit cleanup, bounded callback admission and private rotating no-backup logs.
- Added JVM tests for decoder order/truncation, log bounds/failures and concurrent queue admission.
- No publisher, AA, OEM APK, display, bridge or alert-threshold changes. Vehicle delivery remains unverified.
- `git diff --check` passed. Android unit tests and signed release build are assigned to the Mac Mini queue.
- The static CarPlay regression lock reports five missing legacy tokens/paths. All five are identical in the 74122 baseline; protected source bytes and OEM APK hashes are unchanged.
- First Mac Mini run compiled production and tests, then reported 791 tests, one failure and one skip. The new filesystem-failure test exposed rotation accepting a directory in place of a log file. Added explicit regular-file checks; rerun required. No failed artifact was published.
- Final Mac Mini run on `8918d3a771acb0ab21f717a01e0583840a53f02a`: 791 tests, zero failures/errors, one skipped; signed release, R8 and lint vital passed. Production certificate matched the pinned identity. Published 74123 and verified both downloaded APK SHA-256 and fixed OTA manifest. Vehicle installation, Android self-test execution and real guidance delivery remain pending.

## 2026-10-21 — OTA 74122

- Integrated Netseek PR 13 commit `749e34e500bdf85616abe473b50ace6eedcd1df9` onto the private `74121` source base.
- Scope is limited to radar cache validation, per-source refresh/retry state, settings text, tests and radar documentation.
- Preserved Android Auto cluster recovery, CarPlay, themes, alert thresholds and bundled radar datasets unchanged.
- Validation is delegated to the authorized Mac Mini release queue for unit tests, R8, lint vital, release assembly and signing-certificate verification.
