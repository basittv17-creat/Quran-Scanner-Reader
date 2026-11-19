# Quran Scanner Reader — source delivery report

Date: 19 November 2026. This is an incomplete development delivery, not a release-ready application. No APK/AAB was generated. All 59 original baseline files remain present; history is retained.

## Phase assessment

| Phase | Status | Delivered / remaining |
|---|---|---|
| 1 — CI failures | Completed | Four Kotlin errors fixed. Compilation/resources passed; expanded CI also passes at the tested commit below. |
| 2 — Canonical database | Partially Completed | All 6236 Tanzil Uthmani v1.1 verses bundled from publisher; license, original bytes, exact diacritics and hashes checked. Independent scholarly/display review remains. |
| 3 — Offline OCR | Partially Completed | Existing bundled Arabic model retained; invalid verse IDs and incomplete exact matching now rejected. Real Pakistani 15-line/Madani benchmark, specialized training and confidence calibration remain. |
| 4 — Camera / gallery | Partially Completed | CameraX lifecycle guards, background gallery decode, EXIF rotation/reflection handling and bounded images. Device permission/rotation/memory QA remains. |
| 5 — Complete ayah audio | Partially Completed | Existing offline importer/player retained; background playback pauses. No rights-cleared authentic complete recording set supplied or bundled. |
| 6 — Word audio / highlighting | Partially Completed | Existing timing import/text-audio binding and highlight engine retained; timing boundary/digest tests pass. No licensed human-reviewed timings; MP3 seeking is approximate. |
| 7 — Native UI / WebView | Partially Completed | Native splash, sidebar, scrollable screens, persisted Arabic text size / keep-awake setting, app icon and restricted local WebView. Phone rendering/accessibility/localization QA remains. |
| 8 — Performance / security | Partially Completed | Immutable hash-pinned corpus cached per process; no map-copy overhead; no INTERNET permission; explicit backup exclusion; drawing object reuse; foreground highlight timer. Device profiling and full audit remain. |
| 9 — Compilation / tests / quality | Partially Completed | Kotlin/resources, 7 JVM tests, Python/pure Kotlin validations and lint pass. Remaining lint warnings, full dependency-byte provenance and physical-device QA remain. |
| 10 — Final source / report | Partially Completed | Source checkpoint ZIPs uploaded and email links sent; detailed reports and original-source preservation checked. Final complete product depends on outstanding assets and QA. |

## Tested source and Actions evidence

- Initial compiler-fix commit: https://github.com/basittv17-creat/Quran-Scanner-Reader/commit/6b26999729b3a0dda7235f4d708d0ee604214485
- Initial successful compile run: https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44463910894
- Byte-exact corpus commit: https://github.com/basittv17-creat/Quran-Scanner-Reader/commit/855f0dc2e96d0b538c3b75ad2d64fabaf98ae9c3
- Successful corpus run: https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44464473284
- Native UI / safety / quality milestone: https://github.com/basittv17-creat/Quran-Scanner-Reader/commit/4575f137d5538c8ffa519302adb1d541ca83d90c
- Lint correction / report evidence commit: https://github.com/basittv17-creat/Quran-Scanner-Reader/commit/d2086dbd8e5a206ea95511e15ab2b334dc6caefb
- Successful full quality run: https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44465501620

That full quality run reported **7 tests, 0 failures, 0 errors, 0 skipped**, and **0 lint errors / 43 warnings**. It also passed corpus provenance, Python/pure Kotlin checks, fixed dependency/source policy and the no-APK/AAB artifact check. Later final refinements are rechecked in the repository's Actions page: https://github.com/basittv17-creat/Quran-Scanner-Reader/actions . Counts above refer specifically to run 44465501620, not unobserved future runs.

Final refinements: explicit backup/transfer exclusion, app icon, RTL alignment, reused reticle geometry, foreground-only highlight timer, and WebView preview samples copied from the exact canonical 1:1 entry. No warning baseline/suppression was added.

## Failed runs and resolutions

| Run | Actual failure | Resolution |
|---|---|---|
| https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44457942408 | setup-android attempted removed SDK package `tools` | Hosted SDK setup already corrected before this session; confirmed original logs. |
| https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44463364112 | Missing View / QuranVerseCounts imports; two incorrect fillViewport property names | Minimal source corrections in 6b26999; following run passed. |
| https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44464325038 | Large-text transfer truncated publisher bytes; provenance SHA check correctly rejected them | Reuploaded exact bytes via base64 chunks, verified Git blob SHA against local files; 855f0dc run passed. |
| https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44465020069 | Kotlin and tests passed, lint rejected three DrawerLayout Gravity.START constants | Used GravityCompat.START; no lint suppression; full quality rerun passed. |

## Quran text provenance

Publisher: https://tanzil.net/ ; license: https://tanzil.net/docs/Text_License . Attribution and complete original notice retained. Verbatim redistribution is permitted under the publisher's stated terms; Arabic strings were not normalized or generated.

Exact URL: https://tanzil.net/pub/download/index.php?agree=true&quranType=uthmani&outType=txt-2&marks=true&sajdah=true

Two HTTPS downloads were byte-identical. Original source SHA-256:
`18c719bb3ba26d32ef457f40dad77cd28c4c5a34156833e26a8e5fcfdd246fb1`

JSONL SHA-256:
`fa338471ce7cf0f08e57244458e143ceab555d1905d55e012d49da37cf541270`

`tools/verify_corpus_provenance.py` checks all 114 surahs / 6236 references, exact Arabic equality including marks, pinned source/output digests and original notice. No separate signed publisher digest was supplied. Hash integrity does not substitute for scholarly review. JSONL is a container conversion; each Arabic field remains verbatim.

## Backups and delivery

Folder: https://drive.google.com/drive/folders/1XlCsJhCXl_2Hbmszx7PVHnYF8Vyco7b5

- CI fix ZIP: https://drive.google.com/file/d/1GgupDU4te-3kOJnhAoSxSse4aWo4U78D/view
- Verified corpus ZIP: https://drive.google.com/file/d/19YR2AiHAJabLfrLSq7tkjCoEe3Y91Vx_/view
- Initial expanded-quality source snapshot (before lint correction): https://drive.google.com/file/d/1jivAF1cSpnUgWKL46aaCAtci3YM5v9wU/view

CI-fix and corpus links were emailed to basittv17@gmail.com. A final checked source checkpoint is delivered separately after the last run; it is not an APK/AAB or a claim of complete app readiness. ZIP checks verify no Android packages and exact canonical-source bytes.

## What is needed to finish

See `docs/EXTERNAL_ASSETS_AND_QA.md`. Required: a complete authentic recording set with public repository/ZIP redistribution rights, licensed timings bound to those exact recordings, qualified listening/text/display reviewers, representative legally usable real Mushaf images, and Android device QA. No audio/timing data were fabricated. No uncertain OCR triggers automatic recitation. Generic script profile names are not accuracy guarantees. Unit/static tests cannot certify real Quran OCR, pronunciation or word alignment.
