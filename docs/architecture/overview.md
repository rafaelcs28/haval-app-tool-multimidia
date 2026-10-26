# Architecture Overview

Updated: 2026-08-06

## What this app is

Android app `HavalShisuku` (`br.com.redesurftank.havalshisuku`) for Haval/GWM head units: vehicle integration via AIDL/Shizuku, custom cluster UI on secondary displays, and projection helpers (Android Auto / CarPlay tooling lives partly under `scripts/`).

## Stack

- Android Kotlin/Java (Gradle Kotlin DSL)
- Jetpack Compose for the main settings UI
- Shizuku / privileged shell for OEM service access
- WebView inside a `Presentation` for the cluster dashboard
- Theme frontends in JS/CSS/HTML (Parcel + inline packaging)
- `tools/headunit-dev/` for deploy / logs / diagnostics

## Cluster UI (current)

Themes are **self-contained** Web apps under contract `v1.0`. The host does not own screen FSMs for the dashboard; the loaded theme owns screens and navigation.

- Architecture summary: [`themes-contract-v1.md`](themes-contract-v1.md)
- Author guide: [`cluster-widgets/Themes/THEME_GUIDE.md`](../../cluster-widgets/Themes/THEME_GUIDE.md)
- Sources: `cluster-widgets/source/v1.0/`
- OTA packages: `cluster-widgets/Themes/v1.0/`
- Legacy (non-contract): `cluster-widgets/source/noncontract/`

## Related files

- `app/src/main/java/br/com/redesurftank/havalshisuku/services/ForegroundService.java`
- `…/managers/ServiceManager.java`
- `…/managers/ProjectorManager.java`
- `…/managers/ThemeManager.kt`
- `…/projectors/InstrumentProjector2.kt`
- `…/bridge/ThemeBridgeImpl.kt`

## Risks

- Bootstrap / Shizuku / ServiceManager changes can block startup.
- Secondary-display / projector changes can blank the cluster or break AA/CarPlay cutouts.
- Theme bridge or contract edits without a compatibility check break installed themes.
- WebView/JS errors can freeze a theme (“pretty but frozen” if a module aborts at load).

## Open questions

- Which firmwares/models are primary targets.
- How long `noncontract/` themes remain supported in the field.

## 2026-10-26 — v9 HUD update integrated for143

Upstream `30e301fb` adds configurable HUD colours, circular power arcs, segmented
RPM, and roundabout route geometry. Existing saved HUD settings remain valid;
new colour preferences have defaults. The EV power percentage uses the existing
vehicle telemetry key, with voltage/current calculation as fallback. Theme
contractv1.0, bridge1.0.2, cluster resolution and installed theme assets stay
unchanged. A local correction applies RPM colour choices to the bar gauge while
preserving its original white/red defaults. Full JVM/Release validation passed on the authorized Mini (143, source `8e463602`).
The public APK and preview manifest were independently verified. Actual HUD geometry
and maps require field validation.
