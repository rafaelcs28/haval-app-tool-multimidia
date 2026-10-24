# Haval Impulse 74132 — Netseek v9 integration

Version: `1.0.0.74.132-preview`, version code `74132`.

The release branch starts at the complete personal 74131 source `a427ddde`,
imports the namespace and Java endpoint fixes from `2f6e6961` and `568522b8`,
and merges Netseek v9 through `165359a30e71c0957d2b89597b5b8675629f1ce6`.
That includes all 16 upstream commits after the PR's recorded base `7c4f9fc6`.
The upstream review PR and this personal release branch remain separate.

## Resulting behavior

- HUD: revised turn chevrons, roundabout exit progress, destination flag,
  optional map outlines and distance-based maneuver visibility; direct font
  loading, three settings tabs and live configuration during the settings demo.
- OSM map: ahead-of-car prefetch, loading progress, preferred responding server
  and temporary suppression of unavailable servers.
- Cluster: warning acknowledgements reset when the car switches from off to on,
  including a soft power cycle without a head-unit reboot.
- Radar: upstream now treats the camera as 20 metres before its database point,
  including the passed decision. This replaces 74131's personal rule that kept
  the alert until after the original coordinate. Tests cover the new decision
  with and without road geometry. Cache replacement supports Windows as well.
- CarPlay experiment: corrected Android linker namespace pinning and an explicit
  Java bridge bundle; callback identity, delayed callback retirement and cleanup
  remain guarded. These fixes have not yet succeeded in a physical cold test.

## Deliberate conflict resolutions

Keep the personal identity, readable UI, SportRedLite, connectivity, Wi-Fi priority,
update sources and the recurrent AA frame-stall recovery from 74131. Do not run
the older upstream watchdog alongside that recovery. Keep the narrow AA
roundabout angle-retention policy and tests. None of the latest 16 commits
changes those AA implementations.

`RoundaboutTracker` combines the personal immediate non-roundabout reset with
upstream's new next-leg progress calculation. The projector combines the new
warning-reset behavior with the existing CarPlay slot, lease and AA/OEM guards.
No new theme bridge keys, contract version, display resolution or automatic
experimental activation are introduced.

Assembly runs JVM tests. The OEM cold-gate patch task stays manual: assembling
the app must not regenerate or mount an OEM APK. The bridge native library
remains excluded from the APK and the Java agent remains a separate field tool.

## Validation and limits

Offline validation: 33 native-analysis Python tests discovered, 32 passed and
one optional converter skipped; eight patch/guard Python tests passed; nine
endpoint lifecycle and 30 cold-interceptor JavaScript scenarios passed; observer
probe models passed. The scoped pinned Java bundle was reproduced with SHA-256
`1a429e6d1118e429628ffde3d3172290dd339b39b1b5388cb36558d1797ee895`.
Private captured OEM binaries were read locally and are not part of the commit.

The unmodified legacy overlay sentinels still fail in five places. Inspection
found a real policy divergence as well as moved theme sources: the current
projector authorizes menus by card state rather than the historical physical-input
session gate. Do not call this checker passing or fix it by replacing tokens.
The checker only received a numeric-literal correction so `alpha = 0.25f` does
not falsely match zero; four tests retain detection of actual transparent bypasses.
OEM APK hashes and other guarded paths remain unchanged.

Android/JVM validation and signing are assigned only to the authorized Mac Mini
queue for the audited immutable source commit. The existing queue signing
identity must equal the installed 74131 certificate:
`5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0`.
Signing material stays on that host. Record the job, source commit, resulting
artifact hash and test results with the private artifact receipt.

Completed validation: source `b266cfe6a3af04be50ca1afd2e5eaaee6cf84875`, Mini job
`haval-impulse-132-b266cfe6a3af04be-20261024T232851Z-21744`, successful in
4 minutes 52 seconds with 81 executed tasks. The JVM report contains 93 suites,
852 tests, 851 passed, one skipped, zero failures and zero errors. Release lint,
Kotlin/Java compilation, ARM64 CMake and signed assembly completed. Queue output
checksums and APK ZIP integrity were verified; actual manifest and signature were
checked on the Mini. Version/package and the certificate above match expectations.
The returned APK is 76,105,763 bytes with SHA-256
`c529fdc15ec66b1e82f9de29f15f059d6133455136f9b14067c34e55c682040c`.
Both bundled OEM APK hashes are unchanged and the native bridge is absent from
the APK as required. The full private receipt is retained with the artifact.

Generating this candidate does not publish an OTA or authorize installation.
Installation remains deferred until the owner coordinates a parked session.
Review the overlay contract discrepancy before that deployment. A physical
CarPlay passthrough, endpoint acknowledgement, accepted secondary stream and
first rendered D3 frame remain unproven.
