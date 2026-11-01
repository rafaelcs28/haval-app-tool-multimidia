# CarPlay clean-map pipeline trace

## Purpose

This checkpoint separates two failures that previously looked identical after a
successful Surface grant: policy retirement before the next ActivityManager sample,
and a native video path that never presents a frame. It does not claim that the map
renders and it does not change the principal CarPlay stream (type 110).

The five-minute experiment remains explicit. Android Auto, safety UI, competing
display-3 content, endpoint authentication, generation ownership and bounded
retirement remain fail-closed.

## Active-only freshness grace

Admission and Surface grant still require the normal 1000 ms task snapshot age.
Only after the endpoint has granted the Surface may an already-active host keep that
Surface for a bounded transient read gap:

- the previous known-positive observation may be at most 2000 ms old;
- the replacement `am stack list` read must have started after that observation;
- an in-flight read or a completed unknown read is tolerated for at most 1000 ms;
- a completed known negative (`CarPlay` no longer on display 0, or present on display
  3) retires immediately;
- live Android Auto, safety UI, slot ownership and competing-app gates remain
  immediate and never borrow this grace.

The task sampler uses the read start time as the observation time. Slow reads do not
become artificially fresh when they finish. Reads longer than 1000 ms publish
`unknown`, retain the last known observation only for the bounded active grace, and
wait at least 100 ms before another shell read.

## Payload-free native milestones

The injected image writes bounded warning-level logcat events under `ImpulseCPMap`.
Each line contains only boot-relative milliseconds, generation, stage, status and
numeric counters or dimensions. It never logs Binder tokens, UUIDs, device identity,
route text, coordinates, encoded packets or decoded pixels.

Milestones cover:

1. activation and OEM display-getter entry/result;
2. type-111 session creation and initialization;
3. dimensions and AVCC receipt;
4. packet/IDR receipt and MediaCodec input/retry/failure;
5. output format and rendered output-buffer release;
6. `TextureView` presentation on the app side;
7. shutdown summaries.

Hot-path counters are emitted only on the first event and powers of two. The app
reads the complete filtered tag, deduplicates structured lines in memory, and copies
them into the persistent cluster event log at Surface readiness, first presentation,
failure, retirement and test end. Forced final captures are queued, so a failure
inside the normal cooldown cannot hide the terminal evidence.

## Field interpretation

- `surface_ready` with no `getter_entry`: activation did not reach the OEM getter.
- getter events with no `session_111`: the secondary advertisement was not consumed.
- `session_111` with no `dimensions`/`avcc`: the OEM session exists but video metadata
  was not routed.
- metadata with no `packet`: no encoded secondary frames reached the bridge.
- packets with no `codec_input`: conversion/configuration or decoder admission failed.
- codec input with no `codec_render`: MediaCodec did not produce a rendered output.
- codec render with no app `frame_presented`: the Surface/TextureView presentation
  boundary failed.
- app `frame_presented` while the cluster stays black: investigate display composition,
  geometry and visibility rather than the video producer.

These are diagnostic discriminators, not proof of visible output. A successful field
result still requires owner observation plus the persisted milestones from the same
generation.
