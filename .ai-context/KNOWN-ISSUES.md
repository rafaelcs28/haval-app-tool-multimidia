# Known issues

- Cold interception/loaded-code attestation now exist in source but are not Android
  compiled or device-tested. Frida model tests do not prove real ARM64 trampolines,
  module-observer/linker timing or protections. Installer refuses already-loaded
  OEM modules; it must run before their cold load. Successful/uncertain script and
  native ownership remain pinned; never detach/unload it under live callbacks.
  Provisioning and actual teardown coverage remain absent. Full RX attestation is
  not integrity proof for writable relocated data, other modules or hostile root.
  Older missing-interceptor notes below describe previous implementation stages.

- Native entry/capture/auth source is not an installed OEM hook. Cold interception,
  loaded-module attestation and app/token/channel provisioning remain absent.
  Capture refuses old/unscoped streams and cross-thread Start; actual disconnect
  coverage still needs mapping. CF UUID retention does not retain the OEM stream.
  Unix peer authentication is tested on Linux; Android SELinux/credentials are
  untested. Token provisioner must use random secrets and a suspend-aware clock.
  Permit principal revision differs from Surface/decoder generation. New source
  has no Android compilation; native packaging/default-off safeguards remain.

- Concrete dispatch quiescence only covers entries routed through its shared
  admission gate. It cannot drain calls predating interception or arbitrary OEM
  teardown/epoch events outside it. Initial OEM bootstrap/attestation/auth remains
  absent. Frozen admission delays callbacks; measure mutex/refcount and barrier
  latency on hardware before activation. No original target may point back into
  the dispatcher, or recursion would result.

- HookInstaller is an inert coordinator, not a deployed hook engine. Real peer
  authentication, loaded-module/trampoline attestation and OEM-wide quiescence
  are backend obligations not implemented by the fake tests. Failed publication
  after registration deliberately retains a closed adapter/modules/UUID until
  process exit and forbids retries; no automatic uninstall is implemented.

- Static captured getter ownership is traced, but the outer OEM delegate retains
  the result again. Overall reference balance is not established; preserve this
  behavior and do not compensate in our getter adapter. Live module/target and
  principal epoch attestation remain absent; see carplay-getter-ownership.md.

- Warm native bridge installation requires fresh proof/adoption of the existing principal stream before hook switch; otherwise its old Initialize was never observed and frames would reject. The new adoption API does not prove pointer lifetime/epoch itself. No live installer/capture handshake exists yet.
- Owned getter replacement is conditional on a verified owned-return contract and trusted nonthrowing release. C++ exception containment does not cover native pointer faults; an already committed response cannot be withdrawn by close.

- The Debug-only consumer/host and seven JUnit methods compiled on Mini and all seven passed in source 1f1a1f7d. They are not Android device/Binder/GPU tests. No application call site exists. Revoke failure intentionally retains one queue/view/context and blocks further consumers; a hung Binder holds its worker/gate until recovery/peer death. Device lifecycle validation is required.
- Host AA/safety guards are not an atomic global display arbiter. A real exclusive slot, authoritative fresh elapsedRealtime snapshots and immediate owner/AA/safety events remain mandatory before activation.

- Experimental Binder/Surface/JNI transport has compile/portable-test validation only; no actual Android lifecycle, queue independence or iPhone secondary-video negotiation has been tested. It is Debug-only and the native library remains excluded.
- Different Surface wrappers or ANativeWindow pointers do not prove independent BufferQueues. A fresh consumer and coordinated destruction/revocation are required before deployment.

- Vehicle sessions on 2026-10-22 confirmed inactive/default Waze payloads and 158 Apple Maps state-1 payloads with roads/distances. Application and start/connect sequence differed; Waze limitations are not yet isolated. Maneuver/ETA semantics remain unvalidated.
- A successful oneway registration is transport acceptance, not evidence of iPhone link or active route.
- The device-side Parcel self-test passed 102 checks in that vehicle session; future firmware compatibility still requires a fresh capture, not just a successful build.
- The legacy CarPlay regression checker has known base drift; compare its exact output with the 74122 base and do not weaken it.
- Private capture ends on process death or sixty minutes and rotates after three MiB; retrieve it promptly.
- No second CarPlay video feed or map-clean functionality is provided by this diagnostic release.
