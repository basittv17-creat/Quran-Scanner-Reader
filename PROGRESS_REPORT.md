# Quran Scanner Reader — implementation audit

Date: 2026-11-19. Baseline: 8611a63e518f4bbd06ebc132fdbb1ae2bb5a7ab0.

Existing source retained. No APK or AAB generation requested or run.

| Phase | Status | Evidence / remaining work |
|---|---|---|
| 1 CI | Completed | Four compiler errors corrected; Android compilation/resources and existing checks passed: https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44463910894 . Existing CI failure fixed and successful run confirmed. Expanded quality gates added in the next milestone. Previous failed run: https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44463364112 |
| 2 Canonical corpus | Partially Completed | Complete Tanzil Uthmani v1.1 snapshot bundled from publisher, two identical HTTPS downloads, original notice retained, 6236 entries round-trip exactly. Corpus CI passed: https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44464473284 . Independent scholarly/rendering review remains. |
| 3 OCR | Partially Completed | Bundled Arabic Tesseract model and preprocessing exist; Pakistani 15-line / Madani accuracy not demonstrated. |
| 4 Camera / gallery | Partially Completed | CameraX foreground guard, optional screen-awake setting, background gallery decoding and EXIF orientation handling added. Physical-device permission/lifecycle and image QA remain required. |
| 5 Ayah audio | Partially Completed | Local audio import/player source exists; no licensed authentic recordings bundled. |
| 6 Word audio / highlighting | Partially Completed | Timing import/highlight source exists; no licensed reviewed timing/audio dataset bundled. |
| 7 Native UI / WebView | Partially Completed | AndroidX native splash, DrawerLayout sidebar, scrollable home/settings, persisted Arabic size and scanner-awake setting, restricted local WebView added. Device/accessibility QA outstanding. |
| 8 Performance / security | Partially Completed | No INTERNET permission; process-level immutable hash-pinned corpus cache, no-copy matching snapshot, bounded images/imports, paused background playback and WebView network/navigation restrictions. Profiling and device security QA remain. |
| 9 Compilation / tests | Partially Completed | Previous Python/pure Kotlin CI checks passed. Android compilation/resources passed after fixes. Seven JUnit safety tests, lintDebug and fixed dependency/source policy checks added; awaiting expanded CI. |
| 10 Final delivery | Remaining | Final source ZIP and final validated report depend on remaining implementation and external assets. |

## Failure diagnosis

Latest failed job 135355198488 reported missing android.view.View in LiveScanActivity, incorrect fillViewport references in two activities (Kotlin property is isFillViewport), and missing QuranVerseCounts import in OfflineAudioPackStore. Minimal fixes preserve prior implementation.

SHA-256 proves byte integrity, not scholarly authenticity or recording rights. OCR output must remain unverified until matched against an approved corpus and reviewed. No accuracy claims for specific printings are supported by current tests.

## Corpus evidence

Publisher URL: https://tanzil.net/pub/download/index.php?agree=true&quranType=uthmani&outType=txt-2&marks=true&sajdah=true

Source SHA-256: `18c719bb3ba26d32ef457f40dad77cd28c4c5a34156833e26a8e5fcfdd246fb1`

JSONL SHA-256: `fa338471ce7cf0f08e57244458e143ceab555d1905d55e012d49da37cf541270`

Local Python provenance/structure check and six importer unit tests passed. No signed publisher checksum is asserted. Upstream copyright whitespace is deliberately preserved.

## Additional implementation milestone

- Text IDs now reject invalid per-surah ayah references; incomplete exact matching fails closed.
- Existing source/API structure retained. No audio/timing assets fabricated or redistributed.
- Local checks passed: corpus provenance, six importer unit tests, Python audio/timing packaging checks, static policy, Python syntax, git whitespace check.
- Android compile, new JUnit tests and lint must pass in Actions before this milestone is accepted.
- The failed transfer run https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44464325038 correctly rejected truncated publisher bytes. Fixed by byte-exact base64 transfer with local-vs-remote Git blob SHA verification in commit 855f0dc2e96d0b538c3b75ad2d64fabaf98ae9c3.
- See docs/EXTERNAL_ASSETS_AND_QA.md for concrete asset rights and device QA blockers.

## Expanded CI finding

Run https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44465020069 passed Kotlin compilation, resources and JVM tests, then failed lint on three DrawerLayout WrongConstant errors (Gravity.START instead of GravityCompat.START). Corrected all three call sites without suppressing lint. The run also reported 43 warnings, to be exposed in the next CI summary.
