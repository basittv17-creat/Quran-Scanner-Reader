# Workstream 03 — Camera lifecycle and permission safety

19 November 2026 · Android package `org.quranscan.reader` · branch `main`

## Audit

Read the project progress and Urdu status, development handoff, next steps, external assets and QA, OCR benchmark, and source-validation workflow. Inspected the camera activity, scan gate, crop geometry, manifest, localization resources, and tests. The [reference run 44514943361](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44514943361) actually logged 28 passing JVM tests, 7 Python benchmark tests, lint 0 errors / 43 warnings, and no APK/AAB.

Initial checkout was `5d352d46f5e75cb0936b6df9634d64b605468dd9`. The [44529374633](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44529374633) and [44529532565](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44529532565) logs showed that the Phase 6 validator still searched for the old literal OCR warning after localization. Work 07 corrected the assertion upstream in `d858ead`. The camera change builds on `5e605d7021fe8f8878e16e45f046f8e5a17b6df3`, whose [run 44530150766](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44530150766) completed successfully.

## Camera source checkpoint

[e3065999c7616c677745471b9ae975ea2c9b0d9c](https://github.com/basittv17-creat/Quran-Scanner-Reader/commit/e3065999c7616c677745471b9ae975ea2c9b0d9c)

| File | Change |
|---|---|
| `LiveScanActivity.kt` | Request camera permission in the foreground and verify it before binding, capture, and OCR delivery. Detect absent rear cameras and offer recovery through another scan attempt or Android app settings. Start a fresh binding on resume or display rotation and release it on pause. Unbind only this activity's use cases. Keep the preview-plus-analysis fallback for unsupported three-stream combinations. Release frame and still proxies before offline OCR. Invalidate stale conversion, OCR, and capture UI callbacks by session and crop selection. Use a bounded, rotated full frame if crop bounds cannot be resolved. |
| `core/CameraScanGate.kt` | Add session-scoped ownership of still capture, pending manual frame, and analyzer claims. Coalesce repeated requests and check ownership when completing work. Retain the monotonic automatic interval. |
| `CameraSessionGateTest.kt` | Cover repeated taps, manual throttling bypass while automatic scanning is paused, exclusivity between stills and frames, invalidation across rebinds, and stale completion after reopening. |
| `values/strings.xml`, `values-ur/strings.xml` | Add matching missing-camera and retry guidance. |

Work 01's preview-to-image mapping stays in `LiveScanRegion`. A failed mapping now falls back at the activity boundary. Work 02's image quality guidance and offline OCR processing are preserved. Work 07's localization and accessibility changes are retained. Tap focus, exposure controls, the saved crop and full-image selection, and the saved automatic scan preference continue through the existing controls.

Local source policy and 15 Python benchmark metric tests passed. Corpus validation verified all 6,236 Tanzil Uthmani v1.1 verses, including marks and notice. The local Phase 8 wrapper reached the Phase 6 Kotlin step and stopped because `kotlinc` is unavailable locally. The hosted workflow supplies Kotlin, Gradle and the Android SDK.

## Hosted source validation

[Actions run 44533052361](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44533052361) completed successfully. The job logs show all four authorized Gradle tasks completed, 42 JVM tests with 0 failures, errors or skips (including four camera gate tests), 15 Python OCR benchmark metric tests, and lint 0 errors / 31 warnings. The camera feature check adds a ChromeOS lint warning for requiring a rear camera. Existing target-resolution APIs also report deprecation warnings. The log and local archive checks found no APK/AAB. No package, signing or publishing task was requested or run.

## Backup

[Quran-Scanner-Reader-work03-e306599-source.zip](https://drive.google.com/file/d/1WzFeB4T3a6_CH5bhMAFQrH1SF0EIAXnn/view) is a new 1,526,653-byte source snapshot of the camera commit. Existing backups were preserved. ZIP integrity, absence of APK/AAB entries, and both Quran data digests were checked against the archived bytes.

- ZIP SHA-256 `d8d0a49cc5f185c9c5a79f64bf0cf40c6679892d640e3bdfd0dbe5505ee4b0be`
- Publisher source SHA-256 `18c719bb3ba26d32ef457f40dad77cd28c4c5a34156833e26a8e5fcfdd246fb1`
- Bundled JSONL SHA-256 `fa338471ce7cf0f08e57244458e143ceab555d1905d55e012d49da37cf541270`

## Blocked external verification

- **BLOCKED** — Physical CameraX testing needs authorized devices with an existing authorized build. Exercise denial and revocation, lack of rear camera, provider failure and retry, screen lock, rotation including 180 degrees, background and foreground, rapid taps, delayed OCR, and crop fallback. Native OCR that has already started can finish on the worker. Session invalidation suppresses its output.
- **BLOCKED** — Real-page OCR scoring needs legally usable Pakistani 15-line and Madani photos and independently reviewed labels.
- **BLOCKED** — Authentic redistribution-cleared recordings, matching human-reviewed timings, qualified Quran text and display review, and on-device accessibility evidence are pending.
