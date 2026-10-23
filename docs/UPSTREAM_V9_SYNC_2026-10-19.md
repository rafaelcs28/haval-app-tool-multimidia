# Netseek v9 sync — 2026-10-19

## Range

- Previous upstream base: `824122357a0c25228a24e47c724f486361a7db9a`
- Imported upstream head: `52c991a72c2cd516468b490f689486ed2ef344e1`
- Branch: `netseek/feature/new-screen-enhancements-v9`

The sync imports the new HUD/map presentation, route geometry, radar road matching, cluster warning persistence and BACK cooldown changes published by Netseek through the head above.

## Android Auto cluster conflict

Netseek commit `3f582d454866f4010d5c8c83a2945e9af841c3e5` independently added a CLUSTER frame-stall watchdog after the `74118` base was cut. Its implementation was not imported because this tree already contains the shipped Haval Impulse watchdog and the two implementations must not run together.

The retained Haval Impulse policy:

- Measures frames actually presented by `SurfaceTexture`.
- Waits 12 seconds without a rendered frame.
- Recovers only while moving at 3 km/h or more.
- Uses a 60-second recovery backoff.
- Allows at most two recoveries per Android Auto session.
- Recycles only the CLUSTER output and keeps the AA link/navigation session alive.

The upstream `AaClusterStallPolicy.kt` and its test were intentionally omitted. `AndroidAutoClusterController.kt` and `AaClusterVideoHost.kt` retain the Haval Impulse implementation.

## Follow-up — 2026-10-21

- Imported upstream commit: `4cc5798898e0862c03e5559bce64ebf7c3a664ef`.
- The commit keeps the HUD route visible through junctions and roundabouts and advances the
  Augusta demo after its initial 300 m segment.
- The upstream patch applied without conflicts and with an identical stable patch ID.
- The bundled SportRedLite theme, the Haval Impulse CLUSTER watchdog and the transient Android
  Auto session-stop guard remain unchanged.

## Radar refresh follow-up — 2026-10-21

- Integrated Netseek PR 13 commit `749e34e500bdf85616abe473b50ace6eedcd1df9` onto the private
  `74121` source base.
- The change separates OSM, ANTT and road refresh state, retries only failed sources, validates
  downloaded caches and preserves the last valid file when replacement fails.
- CarPlay, Android Auto, themes, alert thresholds and bundled radar datasets remain unchanged.
- Release validation is assigned to the authorized Mac Mini queue before OTA publication.

## HUD/radar follow-up — 2026-10-22

- Imported `97f8ef1cbfbc45545a7581a950c9eb77a8448c0a` and
  `414a20fb2a99cbceaacad753d522289945f420b8` in order, both without conflicts.
- The AR map remains visible beside the radar card, roundabout progress follows the
  exit countdown across road-label changes, and warnings/display advance by 20 metres.
- Impulse guard: `RoundaboutTracker` clears its key, minimum and entered state on any
  non-roundabout step before considering distance jumps. A renamed roundabout still
  carries the approach minimum as upstream intended.
- Impulse guard: radar geometry, target minima, approach-confirmation samples and
  passed-camera decisions retain the original distances. Only warning thresholds
  and the output distance use the clamped 20-metre lead, exactly once.
- Regression tests cover leaving roundabouts and reset boundaries; radar tests cover
  both road-matched and straight-line cameras, early approach/near warnings, late
  acquisition within 20 metres and retention until passing the real coordinate.
- CarPlay capture, cluster watchdog, themes, OEM APKs and bridge contracts are unchanged.
- Validation/build: authorized Mac Mini queue only; vehicle validation is still required.

## Roundabout telemetry follow-up — 2026-10-23, source only

- Manually ported the functional changes of Netseek
  `7c4f9fc6ff84e2d16fa676d7afa4f037e93cc4ce` (2026-10-22 20:14:51 -03:00).
- HUD tracker progress no longer requires this tick's angle once entry is known.
- Impulse narrows retention to a zero/zero EXIT of the same travel direction,
  with a complete prior pair. A new ENTER, non-roundabout, reset/end of route,
  direction change or partial/invalid pair does not inherit old values.
- Added four test methods covering held exit/renamed road/code 8 and boundaries.
  New Kotlin tests are not yet executed: user asked to defer new version/build.
- RoundaboutTracker and physical radar-pass guards from 6712de86 are unchanged.
  No version bump, APK, signing, OTA or vehicle operation. Combined validation
  on the authorized Mini remains required before the eventual single release.

## Build validation requirement

This workspace is a sanitized snapshot without Git metadata. Android/Gradle/Kotlin validation must not run on the coordinator. Before release, move the reviewed changes into an immutable commit and submit that exact commit through `scripts/build-on-mac-mini.sh`, following `docs/BUILD_ANDROID_ON_MAC_MINI.md`.
