# Quran Scanner Reader — implementation audit

Date: 2026-11-19. Baseline: 8611a63e518f4bbd06ebc132fdbb1ae2bb5a7ab0.

Existing source retained. No APK or AAB generation requested or run.

| Phase | Status | Evidence / remaining work |
|---|---|---|
| 1 CI | Partially Completed | Four compiler errors corrected; Android compilation/resources and existing checks passed: https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44463910894 . CI unit/static/dependency coverage still being expanded. Previous failed run: https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44463364112 |
| 2 Canonical corpus | Partially Completed | Complete Tanzil Uthmani v1.1 snapshot bundled from publisher, two identical HTTPS downloads, original notice retained, 6236 entries round-trip exactly. Independent scholarly/rendering review remains. |
| 3 OCR | Partially Completed | Bundled Arabic Tesseract model and preprocessing exist; Pakistani 15-line / Madani accuracy not demonstrated. |
| 4 Camera / gallery | Partially Completed | Native CameraX and gallery source exist; physical-device and permission/lifecycle testing required. |
| 5 Ayah audio | Partially Completed | Local audio import/player source exists; no licensed authentic recordings bundled. |
| 6 Word audio / highlighting | Partially Completed | Timing import/highlight source exists; no licensed reviewed timing/audio dataset bundled. |
| 7 Native UI / WebView | Partially Completed | Existing green/gold native screens and local preview; full navigation/settings/accessibility QA outstanding. |
| 8 Performance / security | Partially Completed | No INTERNET permission; bounded imports and image processing exist. Profiling, lifecycle and security review outstanding. |
| 9 Compilation / tests | Partially Completed | Previous Python/pure Kotlin CI checks passed. Android compilation/resources passed after fixes. Expanded tests pending. |
| 10 Final delivery | Remaining | Final source ZIP and final validated report depend on remaining implementation and external assets. |

## Failure diagnosis

Latest failed job 135355198488 reported missing android.view.View in LiveScanActivity, incorrect fillViewport references in two activities (Kotlin property is isFillViewport), and missing QuranVerseCounts import in OfflineAudioPackStore. Minimal fixes preserve prior implementation.

SHA-256 proves byte integrity, not scholarly authenticity or recording rights. OCR output must remain unverified until matched against an approved corpus and reviewed. No accuracy claims for specific printings are supported by current tests.

## Corpus evidence

Publisher URL: https://tanzil.net/pub/download/index.php?agree=true&quranType=uthmani&outType=txt-2&marks=true&sajdah=true

Source SHA-256: `18c719bb3ba26d32ef457f40dad77cd28c4c5a34156833e26a8e5fcfdd246fb1`

JSONL SHA-256: `fa338471ce7cf0f08e57244458e143ceab555d1905d55e012d49da37cf541270`

Local Python provenance/structure check and six importer unit tests passed. No signed publisher checksum is asserted. Upstream copyright whitespace is deliberately preserved.
