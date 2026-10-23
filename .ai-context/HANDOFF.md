# Handoff

## Static OEM getter caller contract traced — 2026-10-23

See docs/architecture/carplay-getter-ownership.md for exact hashes, ELF/Ghidra
coordinates and six retained REA Evidence IDs. New read-only REA xrefs/function
queries traced getter -> stack displays delegate -> platform delegate -> server
info dictionary. Getter-owned replacement/release-once is supported for the exact
captured artifacts. Outer delegate adds CFRetain; do not duplicate or compensate
it in the getter hook. Entire OEM lifetime/refcount balance remains unproven.
No runtime source change/build/OTA/car operation. Existing Mini validation remains
the tested baseline. Next: authenticated quiescent installation with attested
live modules/principal identity, then real exclusive display ownership feeds.

## Combined Mini validation PASSED — source 1f1a1f7d

Private immutable source 1f1a1f7d9d6bb063377da7a170da7ac55ae5f51e pushed to
release/impulse-74125. Audited control digest
2a5246d117daef016b4bf08d06883b7b0f80de7cdceeb4f58491a654b5f55819.
Job haval-74126-work-1f1a1f7d9d6bb063-20261023T101355Z-5837 succeeded in 2m19s,
47 tasks including Java/Kotlin, ARM64 JNI/CMake/link, testDebugUnitTest and Debug.
XML totals: 93 suites, 837 tests, 0 failures, 0 errors, 1 skipped (RADAR_SEED_OUT
unset for RadarSeedGenerator.generate). Seven experimental Debug JUnit methods
actually executed and passed: policy 3, retirement 4. AA telemetry 12, roundabout
tracker 6 and radar engine 19 also passed. This is not GPU/Binder/vehicle testing.

Fetched SHA256SUMS verified. APK 148581473 bytes, SHA256
8fd85d22552d39a757aa936d3063b500994e378e5d03b4be5633e4e4f825a125.
Four ABIs retained, bridge/shared C++ runtime absent; four experimental classes
present in Debug DEX. Manifest unchanged (a2565b90f5bb8bda888b424c18759c3a73a2e9db596ac84f65a24cdce1360e30).
Actual linked AArch64 ELF SHA ee71ae6f66b23c6dc69c310724cc2efefee8da2f540ad0a54b6b5d9a06c0ddc3;
both JNI exports and all runtime C entries verified on Mini. Native .so excluded.

Private receipt/artifact: coordinator artifacts/carplay-combined-1f1a1f7d/VALIDACAO.md.
Internal Debug only, NOT production-signed, do not install over vehicle release.
No version bump, OTA, production signing, car commands or local Android builds.
Next gates remain real getter ownership/module/live-principal proof, authenticated
quiescent OEM loader, actual app/AA/safety provider and parked hardware negotiation.

## Combined Mac Mini validation preparation (historical, completed above)

User approved proceeding with compilation/JNI and unit validation, without version
change or OTA. Preparing an audited immutable private commit containing the Netseek
guarded port and experimental CarPlay source. assembleDebug now depends on
testDebugUnitTest so the existing authorized Debug queue runs the JVM suite,
including seven previously unexecuted src/testDebug methods. No release signing
or install is requested. Native library stays excluded and no app entry enables it.

## OEM C adapter/getter and explicit warm adoption implemented, no build

Added oem_runtime_adapter.{h,cpp,test.cpp}, noexcept C callbacks/session/getter/close
and diagnostic entry points. C++ faults retire secondary only; not a native signal
or bad-pointer recovery mechanism. Enabled getter requires owned original-return
contract and nonthrowing release. Successful replacement releases original once;
failure preserves original pointer/status. Null/error original results pass through.

Getter-specific private path permits repeated/concurrent immutable advertisements
on one generation without new decoder. Ordinary advertisement API stays single-use.
Commit is the ownership linearization point: a pre-close committed response can
return after close; secondary remains revoked. No promise of withdrawing that array.
One atomic registration reservation covers legacy ingress and runtime adapter,
preventing stacking/replacement. Adapters/modules stay pinned for process lifetime.

Found warm-start requirement: host requires CarPlay already on D0, so enabling a
bridge without the existing principal record would reject that stream's frames.
Added explicit adoptPrincipal through core/ingress/runtime/C API. It validates known
principal type+UUID, binds an existing stream and NEVER invokes OEM Initialize.
Actual loader must prove live principal/epoch, retain UUID and quiesce callbacks
before hook switch; no first-stream guesses, stale pointers or automatic adoption.

Six adapter process modes passed ASan/UBSan and TSan: off/enabled, legacy-first,
registration race, warm adoption and close-getter. Includes concurrent repeated
queries, exception containment, release accounting, original error/status preservation
and primary continuity. Prior ingress/session/runtime regressions recompiled/passed.
No actual OEM getter, Surface/codec/phone or Android execution; all fixtures synthetic.

Source C adapter is not a loader: no hook installation/injection, live attestation,
app entry or enabled Android bootstrap. No build/version/commit/push/signing/OTA/car.
Seven Debug JUnit tests remain unexecuted; pending combined Mini validation and
parked capture/ownership/lifecycle/negotiation tests. Native library stays excluded.

## Native runtime integration implemented and host-tested, no build/version

Added secondary_runtime.{h,cpp,test.cpp}: ties ingress, advertisement and decoder
factory to one exclusive session generation, default-off exact OEM passthrough.
Enabled runtime validates matching bridge/display identities, readiness, APIs and
owned module/UUID lifetime token (not binary attestation). Secondary session/factory
creation requires committed advertisement. Geometry wrapper rejects a different
video size. Close revokes captured generation only; old runtime cannot stop a new
generation or principal. SecondarySession.advertised/bindRuntime support the gate.

Android runtimeForSurface now checks actual native window dimensions, consumes
the explicit clock-backed factory, retains session/generation and constructs the
runtime; failure revokes captured generation. Adapter/NDK not compiled this batch.
No C/JNI enabled bootstrap or OEM hook installs it; existing C bootstrap is still
passthrough-only. Future loader must bind exception-contained C trampolines,
verified originals and retained CFL/module lifetimes, with quiescent teardown.

Six portable integration scenarios passed ASan/UBSan (20 repeated runs) and TSan:
default passthrough; ad before secondary create/configure/NAL conversion/frame;
principal unaffected by close; duplicate/mismatched runtime; stale-generation
close; geometry mismatch; primary delivery during blocked secondary submit/close.
Callbacks/arrays/decoder are synthetic, NOT actual OEM, Surface or decoded video.
Session regression recompiled and passed with current generation binding code.

Still no new Java/Kotlin/Android build, commit/push/version/signing/OTA/car action.
Seven Debug JUnit methods remain unexecuted. Worktree includes Netseek fix and all
experimental source. Next gates: authenticated actual loader/getter ownership,
authoritative display/AA/safety provider and app entry, clock/physical measurements,
combined Mini validation, then parked hardware negotiation/lifecycle tests.

## Experimental cluster host connected to consumer in source, no build

Added Debug-only CarPlaySecondaryClusterHost and CarPlayClusterHostPolicy, plus
three src/testDebug policy tests (not compiled/executed). Host owns only its child
in an explicit empty FrameLayout on D3. It creates no Presentation or app entry
point and changes no production layout, manifest, resolution or native handoff.

Requires fresh <=1000ms elapsedRealtime snapshot affirming opt-in, exclusive slot,
known AA inactivity, native CarPlay on D0, safety overlays/other apps clear. Direct
read-only checks also deny AA session/request/Surface or non-D0 CarPlay state.
Clock includes deep sleep so cached authorizations expire over vehicle sleep.
Provider must be fast/nonblocking and authoritative, not initial STOPPED defaults.
Checks on start, pre-draw, grant ACK, onInputsChanged and active-only 250ms poll.

Close detaches only its own consumer and unsubscribes callbacks; onHidden precedes
onRetired, which waits for GPU queue retirement. Consumer.isRetired handles start
failure before listener registration. Quarantine may retain view/context until
recovery; single-consumer gate bounds it. No automatic resume or AA mutations.
This is not a global atomic display arbiter: real owner/safety/AA event integration
still required, as are authenticated OEM bootstrap, clock and real negotiation.

Native session ASan/UBSan/TSan and advertisement/lease regressions passed again;
these do not validate new Java. No Java/Kotlin/Android build, version, commit/push,
signing, OTA or car command. All source remains in /tmp/haval-74126-work, including
the Netseek patch. Combined Mini compilation/unit tests and parked testing pending.

## Fresh display-3 consumer implemented in source, no build

Added internalDebug Java CarPlaySecondarySurfaceConsumer + CarPlayConsumerRetirement
and four src/testDebug JUnit methods. View starts hidden; explicit start requires
main thread, attached hardware-accelerated display 3, authenticated endpoint/token
and generation, and no existing texture. External SurfaceTextures are rejected.
It creates Surface from its own new TextureView callback, never the main output.
No call site, manifest entry, Presentation or display-policy change added.

Serial worker handles grant/revoke. onReady is grant acknowledgement only, not
video/maps. Size change closes the one-shot consumer. Framework texture destruction
returns application release ownership, retained until revoke ACK/peer death and
detachment. Unconfirmed revoke quarantines one queue and blocks new consumers;
resource releases execute outside the state monitor with a single drain owner.
explicit retry/peer death can finish. Host must close AND detach/remove the view.
Hung Binder can hold that worker/gate; GPU/codec teardown requires parked testing.

New Java/JUnit NOT compiled or executed per source-only/no-version instruction.
Native session ASan/UBSan + TSan, advertisement and lease host regressions rerun
successfully; these do not test new Android code. No Gradle/NDK build, commit/push,
version, signing, OTA, installation or vehicle command. Netseek port preserved.
Remaining: real host/AA visibility ownership, authenticated OEM loader/bootstrap,
clock validation, linking onReady to coordinated negotiation, actual iPhone/video
acceptance, and combined Mini validation. Library stays excluded; no working map claim.

## Generation coordinator implemented, no build/version

Added secondary_session.{h,cpp,test.cpp}: explicit default-off session, exclusive
lease binding, monotonic generation, one successful publication, and serialized
publish/revoke. Final owned-array handoff holds session and lease guards, including
against direct decoder retirement. Preparation remains outside the session lock;
stale/closed preparations cannot publish. Duplicate close waits for resource stop.
Publisher must be noexcept, prompt, non-reentrant and only transfer array ownership.
Factories/backends cannot reenter the coordinator under lease locks. Callers must
pin coordinator/config/borrowed array/UUID/module lifetimes until they finish.

Surface registry now retains session/generation after explicit factory creation;
JNI revoke removes the registry entry then closes the session. surfaceRouteReady
means active before/after publication, while session.ready means not yet advertised.
New CMake source and Android changes remain uncompiled per user's no-build request.
Host ASan/UBSan and TSan session tests passed, including blocked prepare/publication,
direct revoke, stale generation/rearm, duplicate close, backend failure, concurrent
single publication and another session progressing. No actual OEM publish tested.

At that earlier stage no loader/consumer/bootstrap/phone negotiation or map rendering
was added; the newer consumer source is described above. Close does
not withdraw an advertisement already handed off; callback quiescence still needed
before stream-address reuse/unload. Netseek port remains in working tree for later
combined version. No commit/push, APK/version, signing, OTA or car operation.

## Netseek imported and CarPlay continued — no version/build requested

User authorized importing 7c4f9fc6 and continuing CarPlay, explicitly postponing
a new version until a single combined release. Functional upstream port applied
manually to HudOverlayController and AndroidAutoNavigationTelemetry, preserving
our RoundaboutTracker/radar guards. Retention requires zero/zero EXIT, same
direction, complete prior pair. Four new Kotlin test methods cover road rename,
code 8, ENTER/direction change, partial/malformed data and reset/route boundaries.
Kotlin/JVM tests NOT executed; no Android/Gradle/NDK build submitted or version bump.

CarPlay: new secondary_advertisement.{h,cpp} prepares an owned copy of the display
array and appends the captured AltScreen API only with explicit geometry/identity
and route readiness. Default-off returns untouched borrowed original. RAII/move
ownership and rollback covered with fake arrays under ASan/UBSan. No runtime hook.
Added DecoderLease.active and surfaceRouteReady(handle); lease host ASan/UBSan
and TSan passed; existing bridge/ingress host and observer Node mocks passed.
New Android sources/native additions require future authorized Mini validation.

Two readiness checks alone are not atomic with revoke; the source coordinator
above now guards commit/generation/retirement. Independent consumer, authenticated
OEM loader/bootstrap, clock validation and actual iPhone negotiation remain gates.
Changes currently in the working tree /tmp/haval-74126-work; no commit/push or
build performed for this batch. No OTA, signing, installation or car command.

## Independent Surface transport compiled on Mac Mini

Added Debug/leanDebug-only CarPlaySurfaceEndpoint Binder class (no manifest,
registration, call sites or implicit library load), exact UID/32-byte session
token, single generation/grant, remote owner death and explicit revoke. Native
bounded handle registry imports its own ANativeWindow reference; a one-shot
surfaceDecoderFactory requires explicit clock conversion. Portable DecoderLease
serializes revoke/submission and retires only its own backend. CMake includes
both new native sources, still excludes .so from APK. Release Java unchanged.

Host lease ASan/UBSan and TSan passed; existing Python 14, observer Node mocks,
bridge/ingress host regressions passed. Actual Binder/Surface/JNI not exercised.
Source 7e707bccc120d0eab4319a1d02ea1cc2e1bbfbc5 pushed privately. Mac Mini job
haval-74126-work-7e707bccc120d0ea-20261023T083027Z-20507 passed Java compilation,
ARM64 CMake/link and assembleDebug in 1m55s (40 tasks). Native ELF64 AArch64 SHA
cf576150f93cb2f6d0ba088b2aa4cfa6bd95d574a97a926d6b44c12ea610841c;
both JNI Surface exports and surfaceDecoderFactory verified. Fetched checksums
passed; APK SHA bc136b0c1b375bc27a7d0462e9945dc18b01866542efa7ae1083f251ab7ec01f,
148548705 bytes. Debug class present in DEX; manifest unchanged from prior build,
four ABIs preserved, native bridge/shared C++ runtime absent. Internal Debug only,
not production signed or installable OTA; no JVM test suite in this job.
Receipt: artifacts/carplay-surface-7e707bc/VALIDACAO.md on coordinator.
New consumer/cluster Surface,
authenticated loader/bootstrap, clock validation and advertisement remain gates.
Surface provenance must prove independent BufferQueue; pointer inequality cannot.

Static REA confirmed captured AltScreen API passes type 111 to shared dictionary
builder (accepts 110/111), with DWARF argument widths. Evidence retained privately;
see architecture document. No iPhone acceptance proven, no advertisement enabled,
no car access, OTA or installation. Production remains unchanged.

## OEM ingress adapter and bounded observer validated on Mac Mini

Added native/oem_ingress.{h,cpp}, C ABI exception containment, pre-Initialize
thread-local identity, captured 32-bit CFL string API and one-shot process pin.
C bootstrap is passthrough only; no OEM hooks installed or APK inclusion enabled.
Original targets must be unmodified/trampolines and stay loaded; no unload API.
Added hash-pinned offline profile generator, inert explicit-start Frida observer
and Node mock tests. Hooks instrument code and need parked validation; no pixels
or raw UUIDs captured. Finalize event denotes entry, not resource-release proof.

Validation: Python 14/14 with private binaries and converter; Node syntax/mock
tests passed; ingress passthrough/enabled tests passed ASan/UBSan and TSan.
Source 8e2f20468bbee92bfacb7cc208c82bcae92c297c pushed privately. Mac Mini job
haval-74126-work-8e2f20468bbee92b-20261023T013650Z-32539 succeeded in 2m;
40 tasks executed, including configure/build/link ARM64 and assembleDebug.
Actual ELF64 AArch64 exports all 11 impulse_carplay C symbols, SHA256
807edb5c8e7ffedb3ecca6bf4976ac189a7c72312477ba0f7b65ffd89cd7c1de.
Fetched SHA256SUMS verified. APK excludes bridge/shared C++ runtime and keeps four
existing ABIs. APK byte-identical to previous internal validation artifact:
befc0fc48f44901f2dedf39fb85d267de6cbcccc22ea15a9b36082da96342fab,
148548705 bytes. Expected: all new native work remains excluded from packaging.
Not production signed; do not install. No JVM unit suite in this Debug job.
Private receipt: artifacts/carplay-oem-ingress-8e2f204/VALIDACAO.md on coordinator.
No local Android build, OTA, install or vehicle command. Production 127 unchanged.
Static REA decompilation confirms captured UpTicks uses monotonic nanoseconds;
frame path also supports synchronized NTP conversion. Runtime calibration pending.

Remaining gates: actual loader/interception, independent Surface IPC, second-video
advertisement/negotiation and parked lifecycle/priority tests. Do not claim clean
CarPlay map works. Current sources keep Android runtime and OEM packages untouched.

## Native bridge validation completed on Mac Mini

Built source 07567766ab6746a5fcc10ca2dfa64a06463e9915, pushed privately to
release/impulse-74125 after explicit authorization. Reviewed control digest
cc9c51fc77190f0f698437021ee3a7ba59b4c56536681efdbd087c958ceb8f15
allowlisted. Job haval-74126-work-07567766ab6746a5-20261023T011011Z-24713
succeeded: ARM64 CMake configure/build/link and assembleDebug, 2m9s. No new
Android JVM unit suite was run by this Debug job.

Fetched artifact and checksums verified; ZIP confirms bridge .so and shared C++
runtime absent, existing four ABIs retained. APK is internal Debug validation,
NOT production-signed OTA and must not be installed over the car release.
SHA256 befc0fc48f44901f2dedf39fb85d267de6cbcccc22ea15a9b36082da96342fab,
148548705 bytes. Private receipts at coordinator artifacts/carplay-native-bridge-0756776.

Host bridge also passed ThreadSanitizer in addition to ASan/UBSan. AVC parser
10,000 seeded cases; NAL converter 14 fixtures + 10,000 cases; Python 12 passed.
Runtime remains entirely unwired/excluded. No loader, Surface transfer, clock
calibration, secondary advertisement, user toggle or Android runtime test yet.
No OTA publication or vehicle operation; do not imply clean-map functionality.

Mac job native output also inspected directly (read-only): ELF64 AArch64 shared
object SHA256 839d58ce5a8e2759e7d2fbce19e47e1c1c7942103facd63fb193cfa1cd54e174.
This confirms an actual linked native binary, intentionally excluded from APK.

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
