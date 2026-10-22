# AI change log

## 2026-10-22 — OTA 74126 diagnostic preparation on 74125

- Compared 74125 against 74124; preserved HEV overlay crash fix and readability changes.
- Added pure, tested diagnostic payload classification; raw values and wire transport remain unchanged.
- Added null/inactive/candidate/unconfirmed counters, duration and route start/end markers.
- Prevented misleading “Orientações recebidas” status for non-null inactive/default callbacks.
- Waze vehicle capture confirmed Binder delivery only; active route/map video remain unproven.
- No private raw logs/report committed; Mac Mini unit/release validation pending.

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
