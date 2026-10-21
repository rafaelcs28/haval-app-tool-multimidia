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

## Validation requirement

This workspace is a sanitized snapshot without Git metadata. Android/Gradle/Kotlin validation must not run on the coordinator. Before release, move the reviewed changes into an immutable commit and submit that exact commit through `scripts/build-on-mac-mini.sh`, following `docs/BUILD_ANDROID_ON_MAC_MINI.md`.
