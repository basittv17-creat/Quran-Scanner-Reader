# External assets and device QA gates

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
