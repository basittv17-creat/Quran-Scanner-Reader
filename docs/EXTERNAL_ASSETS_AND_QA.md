# External assets and device QA gates

## Work 10 gate status

| Gate | Status and required evidence |
|---|---|
| Pakistani 15-line page photos | **BLOCKED.** Exact edition/layout and photograph permissions, with representative real camera captures. |
| Madani page photos | **BLOCKED.** Exact edition/layout and photograph permissions for physical pages. See [Work 07](REMAINING_WORK_07.md). |
| OCR benchmark references | **BLOCKED.** Independently reviewed ordered ayah labels for the approved photo dataset. |
| Authentic recitation | **BLOCKED.** Recording-specific authenticity evidence and explicit redistribution grant. |
| Exact word timings | **BLOCKED.** Human listening review and timing rights for the exact recording, bound to recording and text digests. |
| Quran text and display | **BLOCKED.** Qualified scholarly sign-off, including glyphs and marks shown on supported devices. Corpus identity passed automated verification. |
| Physical Android QA | **BLOCKED.** Authorized installed build, hardware and recorded camera, gallery, accessibility, playback, storage and performance results. |

## Text

Tanzil Uthmani v1.1 is bundled verbatim from the publisher. All 6236 entries and marks round-trip exactly to the retained source; two HTTPS downloads agreed. The digest was recorded locally, not signed by the publisher. A qualified reviewer still needs to compare the displayed text and font marks on supported phones. The included Uthmani edition does not establish faithful display of a Pakistani 15-line layout.

## OCR fixtures

Generic Arabic tessdata_fast is not a validated Mushaf-specific model. Existing fixture is a smoke image, not a representative Quran OCR benchmark. Required: legally usable real photos of Pakistani 15-line and Madani pages, skew/rotation/blur/low light cases, manually reviewed verse references, and per-printing error measurements. Never generate expected Quran strings. Phone tests must cover permission denial, camera absence, rotation, foreground/background transitions, memory pressure, large gallery images and corrupt files. No automatic recitation is triggered by OCR.

## Audio and word timing

No audio or timing files were added. Existing import code can check integrity/shape but cannot establish recording authenticity, pronunciation or ownership. Required: exact reciter/recording edition, written license permitting public source ZIP/repository redistribution, every clip digest, complete verse coverage, timing rights and a listening review against the same text/audio bytes. Approximate MP3 seek is not sample-accurate word playback.

Research checked 2026-11-19:

- https://api-docs.quran.com/legal/developer-terms/ distinguishes application use from distribution of content as datasets/packages. It does not provide blanket permission to publish all recordings in this public repository.
- https://github.com/QUD-Technologies/quranic-universal-audio/blob/main/LICENSE restricts public redistribution of timing datasets; its scope does not license the recitation recordings.

No assertion is made that every potential provider prohibits use. These sources are insufficient evidence to redistribute the requested assets here. A suitable rights-cleared source remains required.

## Validation limits

Compilation, resource processing, JVM tests, Python validation and lint do not execute the app on Android. No emulator/device test was run and no APK/AAB was generated. Splash, sidebar, gallery orientation, CameraX lifecycle, WebView, audio focus/interruption, font marks and accessibility need device QA. Target SDK 35 edge-to-edge/insets, large font and narrow-screen layouts need visual review.

## Camera checkpoint acceptance (source added 19 November 2026)

Deferred until separate Android package/device-test authorization:

- Background immediately while CameraX provider is initializing; foreground and confirm preview recovers. Repeat permission grant/denial, rotation and screen locking on API 23 and modern Android.
- Start a slow OCR capture, background, then return. Old-session text must never replace new results; scanning must recover after the old worker completes.
- Tap a page to focus/meter. Confirm darker/brighter steps stop at camera-supported limits and unsupported exposure reports a clear message.
- Pause auto scan, restart the activity and verify pause persists; manual capture must still work. Rapid manual taps must not overlap the analyzer.
- Confirm still ImageProxy closes before OCR, including conversion exceptions; test camera without three simultaneous output streams.
- Verify the selectable live crop against the image received by OCR for automatic analysis, manual stills and the analysis fallback. Calibrate low-light and low-detail guidance with representative real prints.

Compilation and lint cannot establish these camera runtime outcomes.

## Gallery crop acceptance

Source provides a fit-center drag selection and full-image fallback before OCR. Device QA must verify portrait/landscape images with EXIF rotations/reflections, dragging in all directions and beyond edges, tiny selections, Cancel/Back/navigation while decoding, large font, keyboard/insets and screen-reader full-image fallback. Pure crop math tests do not verify Android touch rendering or improve measured OCR accuracy by themselves.

## Continued source acceptance - 19 November 2026

New source checks do not close these manual gates:

- Verify camera JPEG sampling/oversized-output rejection and candidate scrolling on low-RAM/narrow-screen devices. No measured device memory ceiling is claimed.
- Interrupt audio/timing import before/after publication; verify old/new validated packs recover correctly. Test disk-full and power loss separately; filesystem unit tests model interruption states only.
- Retain/select packs from different reciters, preserve legacy audio, remove inactive packs, hit eight-pack capacity, corrupt selection and confirm playback fails closed. Verify mismatched timelines are unavailable.
- Rotate/recreate Recitation: selected verse/word and preferred mode restore; playback and verse confirmation do not. Confirm before every new verse; a timeline beyond recording duration must be rejected.
- Check all recitation controls, MP3 seeking/highlights, accessibility, Arabic fonts and translated strings on device. Complete localization remains TODO.

No APK/AAB was generated to perform these checks. Live reticle coordinate mapping has geometry tests. Calibrate light and detail guidance and verify camera alignment on representative devices.

## Follow-up rights review

- The [King Fahd Complex rights page](https://dm.qurancomplex.gov.sa/rights/) permits software and digital publishing use of its official Madani edition. Its [download page](https://dm.qurancomplex.gov.sa/) publishes a SHA-256 for the Hafs artwork. This gives the benchmark a publisher-approved edition to source. Representative photographs of printed pages, permission for the actual photographs, and manually reviewed verse labels remain required.
- A [Qudrat Ullah 15-line Pakistani scan](https://islam.hasnats.com/quran/printed) is attributed to a copy uploaded under CC BY-NC-ND 3.0. The publisher's authorization has not been established. Obtain explicit rights for the exact page images and benchmark use before redistributing them.
- The [Quran Foundation audio FAQ](https://api-docs.quran.com/docs/tutorials/faq/) directs redistributors to the recording-specific rights holder and restricts rehosting its own audio downloads. The [QUL timing license](https://github.com/QUD-Technologies/quranic-universal-audio/blob/main/LICENSE) does not license recordings. Authentic redistribution-cleared audio and a human-reviewed timing set bound to its exact bytes remain required.
