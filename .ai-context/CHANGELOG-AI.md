# AI change log

## 2026-10-23 — real Android loaded-code proof and isolated smoke passed

- Read real Android9/API28/ARM64 OEM maps and bounded RX memory; all three segments
  and13 bodies match v3 profile. Installed74127, resident OEM process unchanged.
- Audited immutable9ceda01b compiled on authorized Mini, JVM837/0 failures/errors/
  1 skip. New native ELF hash e7c12e3b... verified on Mini/VPS/car. Debug APK exactly
  unchanged and not installed. Native packaging remains excluded, no release/OTA.
- Added guarded isolated smoke JS in32a030d8; actual Android disposable sleep
  process passed ABI/guards, module observer and ARM64 replacement/trampoline/revert.
  Did not hook/inject into OEM or activate native bootstrap/secondary advertisement.
- Resolved Telnet IUCLC/Base64 corruption via verified lowercase hex/gzip transfer;
  discarded copies never loaded, temporary stage/child cleaned. HTTP/ADB attempts
  did not change authorization or firewall. Fresh P/0/engine0 before/after; no power.
- User permits a useful new CarPlay version; app activation/integration still missing,
  so no misleading clean-map release. Full cold OEM passthrough remains device gate.

## 2026-10-23 — cold Frida initial interception and full executable attestation

- Added inert cold-only installer with PID/cmdline validation, absent-module gate,
  synchronous new-module observation, full file/mapped RX/function attestation,
  bridge ELF validation and native pre-patching module pins. replaceFast originals
  feed existing C bridge; no JavaScript frame callback path or new ARM64 relocator.
- Added native prepare/bootstrap/release with exact RTLD_NOLOAD ownership and
  CoreUtils dlsym checks. Partial failure reverts only in cold window; code must
  reattest before releasing. Ambiguous state/rollback stays process/script pinned.
- 26 Node model scenarios passed; native 15 modes passed ASan/UBSan + TSan;
  Python 16 passed/1 existing converter skip. Warm-refusal test was updated to
  account for the new read-only process identity check. No production behavior
  changed, no Android build/version/signing/OTA or car operation.
- REA traced pinned stack initializer roots/helpers/message constructor; no video
  session or worker creation in inspected bodies. Frida API contracts checked
  against official docs. Android/runtime compatibility remains unproven.

## 2026-10-23 — OEM ABI entries, callback session capture and Unix authorization

- Added nine C entry thunks, gated observation/lifecycle boundary, retained CF UUID
  capture with parent-session TLS/epochs and stop/finalize race invalidation.
- Added single-use UID/token/request/revision/deadline permits, generation checks
  through publication, and real kernel-credential Unix packet endpoint. Protocol
  has no pointer/code/path fields; tokens are erased and ancillary rights rejected.
- Added exact-artifact entry manifest/tests without changing the observer digest.
  Fresh REA CF retain/release wrapper evidence retained privately.
- Final 14 portable modes passed ASan/UBSan and TSan, Python 15 passed/1 skip.
  An initial synthetic exception assertion was corrected to check runtime-level
  containment (the inner boundary catches first); final suite passed. A socket
  test helper name collision was qualified as ::send and recompiled successfully.
- No actual OEM interceptor/module attestation/bootstrap provisioning or car use.
  No Android build/version/signing/OTA; native packaging exclusion retained.

## 2026-10-23 — concrete callback dispatch admission and route publication

- Added CoordinatedDispatch with RAII active calls, nonblocking freeze on idle,
  pending-entry admission, nested-call support, scoped thaw and single complete
  route-table publication. Added DispatchPreparedHooks integration with the
  coordinator, explicit native owners and capture/epoch providers.
- Eleven modes passed ASan/UBSan and TSan on final portable source; new cases
  exercise a blocked frame, nested callbacks, callback/capture/epoch exceptions,
  competing/cancelled freeze, all nine targets and principal forwarding after
  warm install/secondary close without original Initialize.
- Not a live inline hook engine: initial OEM interception, real authentication,
  module attestation and capture remain missing. No Android build/release or car use.

## 2026-10-23 — inert coordinated hook installation source

- Added explicit trusted preparation and quiescent publication capabilities;
  default disabled, nonblocking coordinator contention, fresh principal adoption
  and epoch recheck before process registration. Pins installation ownership in
  adapter. Failed atomic publication closes secondary and permanently consumes
  the process slot, retaining resources rather than risking an unload/retry.
- Added fake-backend success/failure/competing-registration modes, preserving
  original frame forwarding and zero original Initialize calls. No actual hook
  engine, authentication backend or vehicle operation. Android build deferred.
- Final source compiled on the Linux host with g++ C++17/Wall/Wextra/Werror.
  All nine modes passed ASan/UBSan and TSan (18 final mode executions), no sanitizer
  diagnostics. These are portable C++ tests, not coordinator-host Android builds.

## 2026-10-23 — exact OEM getter ownership and downstream caller analysis

- REA/Ghidra read-only queries traced the captured getter through displays delegate,
  platform copy-property and server info response. Getter call/status/extra retain
  corroborated in assembly; inline CFL displays constants correlated from ELF.
- Documented exact coordinates/hashes, six Evidence IDs, replacement ownership and
  the unresolved overall OEM reference balance. No speculative compensating release.
- No runtime edits, Android build, new version/OTA, library publication or car access.

## 2026-10-23 — combined Mac Mini Java/JNI/ARM64 and unit validation passed

- Source 1f1a1f7d pushed privately, exact build-control digest reviewed/allowlisted.
  Debug now depends on testDebugUnitTest, so the authorized queue actually runs it.
- Mini job finished successfully in 2m19s, 47 tasks. XML: 837 tests across 93 suites,
  zero failures/errors, one intentionally skipped radar seed generator (unset output).
  All seven experimental Debug unit tests executed/passed, plus AA/radar regressions.
- Actual linked AArch64 ELF and JNI/runtime exports verified; fetched hashes passed.
  Debug APK keeps four ABIs and unchanged manifest; native bridge remains excluded.
- No version bump, production signing, publication, install or vehicle operation.
  Debug artifact is internal and must not replace the vehicle release.

## 2026-10-23 — OEM C boundary, repeat getter and warm adoption source

- Added noexcept adapter C entries, exact original error/array fallback and owned
  replacement release accounting. Repeated/concurrent queries reuse generation,
  not a decoder. Legacy/runtime registration share a one-shot atomic reservation.
- Added explicit principal adoption without OEM Initialize for a future verified
  warm install; no automatic live capture/hook or source of principal proof added.
- Adapter six process modes passed ASan/UBSan and TSan, including concurrent getter,
  close during preparation and registration contention. Ingress/session/runtime
  regressions recompiled/passed. Tests use fake OEM arrays/callbacks/decoder only.
- No Android build, version, commit/push, signing, OTA or car commands. Caller must
  still validate real ownership/epoch/module targets and quiesce installation.

## 2026-10-23 — integrated native runtime, source/host tests only

- Added runtime linking pre-create ingress, generation/advertisement and secondary
  decoder, with exclusive runtime binding, geometry guard and principal isolation.
- Surface adapter now validates native window size before factory/runtime setup;
  Android additions remain uncompiled and no hook/enabled C bootstrap is installed.
- Six synthetic runtime scenarios passed ASan/UBSan (20 repeats) and TSan, including
  primary progress while secondary submit/close blocks. Current session regression
  recompiled/passed. No claim of OEM/Surface/codec/video execution.
- No Android build/version, commit/push, signing, OTA or vehicle commands. Combined
  Mini validation and seven Debug JUnit methods remain pending.

## 2026-10-23 — guarded experimental cluster host source

- Added dedicated-slot host connecting the consumer, with read-only AA/native
  CarPlay guards, fresh explicit authorization and teardown of its child only.
- Snapshot clock uses elapsedRealtime to expire permissions across deep sleep;
  checked on start/pre-draw/grant ACK/events plus active-only 250ms polling.
- Added three policy JUnit methods, not compiled/executed. Prior native regressions
  passed; no claim of host lifecycle/device validation or atomic global arbitration.
- No app call site, production layout/manifest change, build/version, commit/push,
  signing, OTA, install, AA mutation or vehicle command. Combined validation pending.

## 2026-10-23 — fresh cluster consumer source, not built

- Added explicitly enabled Debug-only display-3 TextureView consumer with fresh
  texture, off-UI-thread Binder grant/revoke, no principal Surface reuse or call site.
- Retains queue until confirmed producer retirement and framework detachment;
  unconfirmed revoke holds one bounded quarantine slot, preventing new consumers.
- Added four retirement JUnit tests (not executed). Native session ASan/UBSan/TSan,
  advertisement and lease regressions rerun successfully; not Android validation.
- No Java/Android build, version, commit/push, signing, OTA, install or car commands.

## 2026-10-23 — generation-scoped CarPlay session, source only

- Added SecondarySession coordinator, exclusive lease binding and guarded atomic
  ownership handoff against session/direct lease revoke; stale generations reject.
- Surface transport now retains session/generation and closes it on JNI revocation.
- Host session tests passed ASan/UBSan and TSan; fake backends/arrays only, no OEM
  execution. Includes close during preparation, parallel publication, rearm, direct
  retire and independent session progress. Prior lease/advertisement tests passed.
- No Android build, version bump, commit/push, signing, OTA or car commands. Current
  source requires combined Mini validation later; loader/consumer remain missing.

## 2026-10-23 — Netseek port and secondary advertisement source, no release

- Ported 7c4f9fc6 functional HUD/AA fix with narrower same-direction zeroed-EXIT
  retention, four boundary test methods, preserving radar/tracker protections.
- Added default-off copied-array alternate advertisement builder with explicit
  captured ABI, geometry, UUID and readiness checks, ownership/rollback tests.
- Added lease active-state and native Surface route readiness inspection. New
  host advertisement ASan/UBSan and lease ASan/UBSan/TSan passed; prior native
  bridge/ingress and observer mock tests passed. No Kotlin/Android tests run.
- User requested no new version yet: no build, version bump, signing, OTA, install,
  commit/push or vehicle command. Future combined Mini validation required.

## 2026-10-23 — independent Surface transport prototype

- Added explicit Debug-only Binder Surface endpoint and native bounded registry,
  with UID/token checks, owner death, single claim and independent revoke.
- Portable decoder lease passed ASan/UBSan and TSan, including blocked submission
  revocation and an independent decoder continuing to submit. Python 14 and prior
  bridge/ingress/observer host regressions passed; no Android runtime test claimed.
- REA/DWARF verified alternate-display API/signature and captured type 111; common
  dictionary builder accepts 110/111. No advertisement hook or runtime assumption.
- Native library remains excluded; no loader, Surface consumer, OTA or car commands.
- Source 7e707bc passed authorized Mac Mini Java/ARM64/assembleDebug in 1m55s.
  JNI exports and DEX class verified, manifest unchanged, four ABIs retained;
  native bridge still excluded. Fetched checksums verified, internal Debug only.

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

## OEM ingress and observation preparation

Added bounded pre-creation identity adapter and exception-safe C ABI, default
passthrough, plus exact-library profile generator and manual bounded observer.
Python 14 tests, Node observer mocks and native ingress ASan/UBSan + TSan passed.
No runtime integration, OTA publication or vehicle operation. See README and handoff.
Private source 8e2f204 compiled/linked ARM64 and assembled Debug on Mac Mini in
2m; all 11 C exports verified. New native code remains excluded, so APK is byte
identical to the previous validation build. Checksums verified; no JVM suite or
vehicle runtime test claimed. Private coordinator receipt retained.
# Cold CarPlay startup gate and auto-arm tooling — source staged, 2026-10-23

- Added an opt-in 10-second gate before `CarPlayService.init()` in a staged OEM
  service patch. Default startup remains immediate; release requires PID plus a
  fresh 32-hex nonce and otherwise times out to stock.
- Added a reviewed-profile launcher generator with fixed bridge bounds and
  phase-only status messages.
- Added a Mac Mini build task that produces, signs and re-decodes the staged OEM
  APK without replacing the currently bundled asset.
- Local validation: six Python tests, 26 existing cold-interceptor Node models,
  shell syntax and diff checks passed. Android/Mini build still pending at this
  changelog entry; no APK, OTA or vehicle change was made.

- First immutable Mini run failed at aapt2 manifest relinking because the OEM
  APK has no resources table. Follow-up preserves the original binary manifest
  with Apktool `--copy-original`; no staged APK was produced by the failed run.
# 2026-10-23 — embed reviewed default-off CarPlay cold gate

- Incorporated the exact Mini-validated `TsCarPlayService` cold-gate artifact,
  SHA-256 `ad019ce091ef55bbf57f7e242ce9cdb647a8596f1e1f7d28d0c7f340957dba01`.
- The gate is inert without an explicit 32-hex nonce property and remains a
  passthrough-only test; no D3 Surface, second-display announcement or clean map.
- Debug and Release builds now re-decode and semantically verify the committed
  artifact instead of patching/re-signing it a second time.
