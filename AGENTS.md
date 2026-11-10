# Greater Art working agreements

## Trust and scope

- If the requested outcome is clear, do the work. Use one relevant check for an obvious fix; do not repeat a check or add approval steps that would not change the decision. Keep the existing build, signing, and verification safeguards.
- Ask one concrete question early if different interpretations would materially change the result. For a visual reference, inspect the actual example and point to the specific moment or behavior. Confirm that interpretation before implementing an ambiguous design.
- **Ambient example (2026-11-10):** “like [this Short](https://www.youtube.com/shorts/Daab16yTHhA)” meant the opening Iron Man shot: the white-hot beam keeps its angle and color as it crosses out of the video into the UI. The later halo was not the target. Two edge-glow previews missed it. The user approved one directional-beam preview after we identified the exact shot. Next time, ask about that difference before making variants.

## Android delivery is mandatory for Android app changes

Read [the version rules](greater-art/VERSION_RULES.md) and [handoff](greater-art/HANDOFF.md). Preflight JDK, SDK, and signing before work on an Android build. Use [the Android release gate](greater-art/docs/ANDROID_RELEASE_GATE.md) to run `:app:assembleRelease` whenever assembly is possible, even when the signing key or A55 is unavailable. Host tests and debug builds are useful evidence, not release substitutes.

Report assembly, same-key signing, exact-file identity, and device smoke separately. A failed or blocked gate leaves the task incomplete, even if the source fix is ready. Do not call an APK verified without the exact versioned source, pinned signer, and a smoke test on the exact installed bytes. A doc-only task does not trigger assembly. Do not create a new signer, downgrade a device, clear its data, overwrite an artifact, push a tag, or publish as part of this gate.
