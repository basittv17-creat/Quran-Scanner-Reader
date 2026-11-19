# Quran Scanner Reader | Development handoff

19 November 2026, Asia/Karachi. Source-only Android work continues in the existing repository.

| Checkpoint | Source commit | GitHub Actions | Evidence |
|---|---|---|---|
| Audited baseline | `04b39b5` | [44507426141](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44507426141) | 20 JVM tests, lint 0 errors / 46 warnings |
| Selectable live crop, localized resources, OCR guidance | `840f79a` | [44512771258](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44512771258) | 26 tests, lint 0 / 47 |
| Gallery restoration and loading | `26a2fcd` | [44513012884](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44513012884) | 26 tests, lint 0 / 48 |
| Stale import recovery | `80e0003` | [44513191090](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44513191090) | 27 tests, lint 0 / 48 |
| Recording hash before playback | `bf31ddd` | [44513413555](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44513413555) | 28 tests, lint 0 / 45 |
| Ayah and OCR comparison | `b7a89c9` | [44513603101](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44513603101) | 28 tests, lint 0 / 45 |
| Home and sidebar localization | `eaf7175` | [44513752561](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44513752561) | 28 tests, lint 0 / 43 |
| Pin Actions to upstream commits | `0df7b88` | [44513820327](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44513820327) | 28 tests, lint 0 / 43 |
| OCR metrics by script and configuration | `0227080` | [44514029889](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44514029889) | 28 JVM tests, 7 Python metric tests, lint 0 / 43 |
| Responsive local WebView preview | `a8b61b65675414f1604fe77b8b0a77543206c231` | [44514227926](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44514227926) | 28 JVM tests, lint 0 / 43 |

The initial crop commit `4aab8de` compiled and passed 23 JVM tests. [Run 44512392079](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44512392079) failed on 17 missing Urdu locale entries. `840f79a` added the entries, preserved the Urdu wording, supplied English defaults, and passed lint. Neither tests nor lint rules were disabled.

## Feature acceptance

| Area | Source and evidence | Outstanding work |
|---|---|---|
| A and J. Home, sidebar, settings | **Source Implemented** for primary English/Urdu labels and background corpus load. Existing local preferences retained. | Finish screen strings and general navigation restoration. Verify TalkBack, RTL, large fonts and narrow layouts. |
| B. Live camera | **Automated Tests Passed** for four quarter-turns, offset viewport, sampled JPEG, and invalid bounds. Shared CameraX viewport fills the visible preview. Both capture paths use the crop. Crop retained on recreation. | **Runtime QA Pending** for crop alignment per device, rapid rotation, analysis-only fallback, camera permission and background recovery. |
| C. Gallery | **Source Implemented** for URI/crop restoration, off-thread decode, EXIF handling, low-memory decode failure, and scrollable results. Recreated OCR screen returns to crop review and requires a new explicit scan. | **Runtime QA Pending** for URI grants, mirrored images, interruption, large files and memory pressure. |
| D. Pakistani and Madani OCR | **Automated Tests Passed** for diagnostic math and benchmarks. Generic Arabic model and profiles remain. Guidance measures light, fine detail, and elapsed OCR time. | **Blocked — Reviewed Real-Page Dataset Required** for accuracy, threshold calibration, preprocessing or script-model comparisons, and false-positive assessment on real pages. |
| E. Quran corpus | **Automated Tests Passed**. Same 6,236 Tanzil Uthmani v1.1 ayat, raw and derived hashes, verse IDs, harakat and notices. | **Scholarly Review Pending** and Arabic/harakat rendering on supported devices. |
| F and I. Matching and Arabic reading box | **Source Implemented** for showing bounded raw OCR alongside the canonical ayah and reference in review. Corpus-integrity and matcher tests retained. Manual confirmation remains mandatory for playback. | **Runtime QA Pending** for comparison layout and selection. Real-page recognition quality uses gate D. |
| G. Offline audio | **Automated Tests Passed** for staged directory cleanup and hash rejection. Playback verifies and reuses the same recording descriptor. The import catalog and focus controls remain. | **External Asset Required** for rights-cleared reciter recordings. Test interruption, disk full, MediaPlayer, focus and headphone disconnect on Android. |
| H. Word-by-word | **Source Implemented** with validated audio/text/hash binding and duration guards, visual highlighting, word controls, and approximate MP3 seeks. | **External Asset Required** for independently reviewed exact-audio timings. Validate word boundaries and consider alternate high-precision audio formats with licensed fixtures. |
| K. WebView | **Source Implemented** for a responsive menu and visual preview, exact local asset navigation, CSP and release on navigation. | **Runtime QA Pending** for API 23 fallback, font scale, and in-app fragment navigation. |
| L. Privacy, storage and dependencies | **Automated Tests Passed** for source policy and process-local import staging. Actions use upstream commit SHAs; checkout credentials are not persisted. | Gradle artifact byte verification and dependency advisory review still open. Device RAM, CPU, battery and storage profiling and disk-full QA pending. |

Android device and emulator tests requiring a package are **Deferred — APK Authorization Required**.

## Resume from current main

1. Fetch main and confirm the newest successful source-validation run against its exact Git SHA. Confirm the absence of APK/AAB.
2. For the next source change, finish extracting Settings, Help, About and remaining scanner/recitation strings into matched English and Urdu resources. Add restoration for these destinations and an accessible selection control for live crop. Verify translation key parity, all existing source tests and source-only CI.
3. Prepare Gradle verification metadata using the existing four authorized source-only tasks and verified upstream artifact provenance. Review changes to downloaded artifacts independently. Do not weaken or bypass dependency verification to make CI pass.
4. Use the benchmark's `--require-provenance` workflow only after receiving licensed, reviewed Pakistani and Madani photo exports. Preserve both canonical Quran hashes and obtain qualified scholarly review separately.
5. When APK authorization is granted, exercise camera crop with a known quadrant pattern in portrait, landscape and reverse orientations, both automatic and still capture. Then run gallery, TalkBack, reader, audio, low-storage and performance cases from [EXTERNAL_ASSETS_AND_QA.md](EXTERNAL_ASSETS_AND_QA.md).

No package generation tasks are authorized. Keep all earlier Drive backups. Current source is integrated through [Pull Request #1](https://github.com/basittv17-creat/Quran-Scanner-Reader/pull/1).
