# AI change log

## 2026-10-21 — OTA 74123 preparation

- Imported Netseek PR 14 at `15ce75fa07d4cbe4bedd00f308a9ce2e298a6cf0` onto the signed 74122 base.
- Added manual sixty-minute CarPlay navigation capture in Resources with counts and app/incident markers.
- Added bounded Parcel decoding and a real-Android self-test before subscribing; unknown enums remain raw.
- Added generation-scoped bindings, explicit cleanup, bounded callback admission and private rotating no-backup logs.
- Added JVM tests for decoder order/truncation, log bounds/failures and concurrent queue admission.
- No publisher, AA, OEM APK, display, bridge or alert-threshold changes. Vehicle delivery remains unverified.
- `git diff --check` passed. Android unit tests and signed release build are assigned to the Mac Mini queue.
- The static CarPlay regression lock reports five missing legacy tokens/paths. All five are identical in the 74122 baseline; protected source bytes and OEM APK hashes are unchanged.

## 2026-10-21 — OTA 74122

- Integrated Netseek PR 13 commit `749e34e500bdf85616abe473b50ace6eedcd1df9` onto the private `74121` source base.
- Scope is limited to radar cache validation, per-source refresh/retry state, settings text, tests and radar documentation.
- Preserved Android Auto cluster recovery, CarPlay, themes, alert thresholds and bundled radar datasets unchanged.
- Validation is delegated to the authorized Mac Mini release queue for unit tests, R8, lint vital, release assembly and signing-certificate verification.
