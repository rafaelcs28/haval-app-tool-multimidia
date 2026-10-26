# CarPlay bootstrap: preserve the existing permissive policy

The first private 145 candidate passed quiescence but did not become READY. Its
native CLI started at kernel elapsed 42.50 s; the kernel recorded a SELinux policy
reload at 42.77 s and the Android framework died immediately afterwards. The
injector returned code 4, `The connection is closed`, without ARMED/resident proof.
This temporal relation motivates the change; it does not establish causality.
No out-of-memory kill was demonstrated. The unrelated ambient-lighting crash
messages do not justify disabling vehicle-control services.

Both the original kernel command line and `getenforce` show that this head unit
already boots permissive. The pinned Frida 17.5.2 initializer nevertheless invokes
`frida_selinux_patch_policy()` before argument parsing, including `--version`.
That upstream implementation writes `/sys/fs/selinux/load` without an initial
permissive check. Sources: [initializer](https://github.com/frida/frida-core/blob/bcd4a3d586f5a95cad943ddb176be23aadf1dfd7/inject/inject-glue.c),
[policy patcher](https://github.com/frida/frida-core/blob/bcd4a3d586f5a95cad943ddb176be23aadf1dfd7/lib/selinux/patch.c).
The upstream [runtime-option request](https://github.com/frida/frida-core/issues/1198)
does not supply a supported CLI switch.

## Separate, reproducible bootstrap asset

The original `res/raw/fridainject` remains unchanged. The authorized Mini builds
a separate `cpbridge-inject` asset from that exact immutable input. The input SHA
is `bdfa97cdd84ca678c5e5eea9eb6625c9528acff32b4f9413f1e5c58a3d15222e`.

The profiled AArch64 initializer first calls `frida_init_with_runtime`, restores
its frame and then tail-branches to the policy patcher. Only that final branch
at file offset `0x449f98` (VA `0x44af98`) changes from `111da614` to `c0035fd6`
(`RET`). Initialization and its return ABI are preserved. The expected derived
SHA is `32f7c702ea36c9ad1d09d2dc9e21de5f4994d98f855c4e51db01bcc6d0fd496a`.

The tool verifies the complete input digest, ELF architecture, executable mapping,
caller preimage, branch target and complete derived digest. Unknown bytes abort
the build. `--audit` is read-only; real Android output is emitted only by the
Mini's generated-assets task. The profile and tool participate in the controller
source digest. Local LLVM disassembly supports the instruction mapping; the native
REA analysis provider was unavailable, so this is not a REA decompilation claim.

The manifest contains five versioned files including the derived injector, plus
its original/derived digests, `policyMode=permissive-existing-policy-v1` and the
exact patch profile. Other application uses of the original resource are unchanged.
The extra asset increases APK disk size; it does not add another runtime process.

## Runtime admission and evidence

The runtime must already have kernel enforcement value `0`; enforcing, unknown,
changed or unreadable mode blocks the bootstrap. No code changes SELinux mode.
Before a cold attempt, a root-owned one-shot compatibility probe runs the derived
CLI's `--version` and requires the same kernel boot, policy SHA, `system_server`
PID/start and immutable injector identity before and after. A marker precedes
execution; interrupted/failed probes cannot automatically run again in the same
boot and revision. A successful root-owned receipt is adopted only if its live
environment still matches.

The approved environment is checked before the idle host restart and immediately
before each native/Java child exec. Large injector hashing belongs to preflight;
the cold gate uses its root-owned immutable-file fingerprint. The original cold
deadlines, quiescence checks, post-exit resident proofs and native/Java ownership
requirements remain mandatory. The environment is local attestation data and does
not change the endpoint's JSON binding protocol or theme bridge contract.

A successful `--version` probe verifies only initialization. It does not prove
that attach, helper or Java paths preserve the policy. Field validation must also
confirm new-boot READY, nine pinned native hooks, Java armed, and an unchanged
Android framework/policy after the complete preparation. Until then this remains
a private candidate, with automatic preparation disabled on the installed first
candidate to avoid repeating its failure. The vehicle was switched OFF and
confirmed through a fresh GWM update before this offline work.

The modified component's notice/license are bundled under
`assets/licenses/frida-bootstrap-injector/`. The eventual release also includes
`frida-bootstrap-source.tar.gz` containing its reproduction tool, profile and
upstream notices, without private application history.
