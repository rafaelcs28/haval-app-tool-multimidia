# Netseek v9 — HUD colours and direction arrow, 2026-10-26

Integrated `feature/new-screen-enhancements-v9` from `30e301fb00f30bafa0f540206fca48f3e2fe912e`
through `edc0cc610cc5540809e6862b1b3d958ab4bb02ae` on top of the private 144 source.

- `13e0341b2ef5f7872ffe272b90dfe2799b91c87b`: three direction-arrow modes, including
  a stationary arrow while the next maneuver is hidden; uniform blue preset.
- `edc0cc610cc5540809e6862b1b3d958ab4bb02ae`: custom hue/saturation/brightness picker,
  compact colour-part chips and solid power ticks instead of radial shading.

The merge had no conflicts. The added preference defaults to false, preserving
the previous meaning of an already-saved `chevronsAlways` setting. The previous
mode is now labelled “Com próxima manobra”; the new “Sempre” also needs the new
preference. Stopped/no-navigation/direction restrictions remain in the model.

Retained local changes include RPM bar colours and default redline behaviour,
the text-only CarPlay AR guard and its tests, Wi-Fi priorities, AA decoder recovery
and the 144 preparation reader/proof guards. No theme assets, resolution, bridge
contract, OEM projection APKs, build controls or version values changed.

Portable validation: 11 visibility tests, 31 bootstrap lifecycle tests, OEM CarPlay
regression lock and `git diff --check` passed. Full JVM/Debug validation runs only
on the authorized Mac Mini from an audited immutable commit; its outcome is recorded
in the current handoff. Android UI instrumentation and physical HUD rendering were
not run as part of these portable checks.

This source sync does not publish a production APK, update OTA metadata, install
on the vehicle or resolve the separate 144 DCM/Bluetooth preparation diagnosis.
