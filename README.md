# Quran Scan Reader — Phase 9 (native Android / offline-first development source)


## Android APK

Open the latest successful main-branch run of [Android checks and APK](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/workflows/source-validation.yml), download `quran-scanner-reader-apk` from **Artifacts**, and extract `app-debug.apk`. GitHub wraps the single APK in a ZIP. Main pushes and manual runs on main produce an installable debug-signed APK for testing. Pull requests run the checks before packaging is enabled on main.

The APK includes camera/gallery OCR and the bundled Tanzil text. The reader offers normal speed and optional 75% playback for practice. Authentic recitation and word-by-word playback require a compatible imported audio pack and reviewed timing pack. See [external assets and device QA](docs/EXTERNAL_ASSETS_AND_QA.md) for current validation gaps. Gallery and camera can start recitation automatically from a unique full-ayah match when its local recording is installed. Possible matches still require manual review. The gallery retries sideways or upside-down images when the initial OCR confidence is low.

Narrow camera or gallery crops receive an enlarged, padded OCR pass. A close short-line match can offer a full canonical ayah for review. Autoplay still requires a unique full-text match.

For mixed-language images, the scanner searches the Arabic-script portions for contiguous matches in the Quran. It shows candidate surah, ayah and word positions, highlights the matched words in the canonical text, and plays the selected range after confirmation when matching local audio and word timings are installed.


## Audio pack builder hardening — verified 19 November 2026

[PR #2](https://github.com/basittv17-creat/Quran-Scanner-Reader/pull/2) was merged as [`f197db1`](https://github.com/basittv17-creat/Quran-Scanner-Reader/commit/f197db1a16ac518613958d7104004aa749d556b8). [GitHub Actions 44580417501](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44580417501) completed **success** on the merged `main` commit: 64 JVM tests (zero failures/errors/skips), 22 Python OCR metric tests, Android Kotlin/resources compilation and source policy passed; Android lint reported **0 errors / 13 warnings**; the CI confirmed **no APK/AAB generated**.

The user-supplied offline audio pack builder now enforces attribution control-character, filename, verse-range and byte-size validation even with Python `-O`, with synthetic regression checks. The original Tanzil corpus, app audio and word timestamps were not changed. Real Pakistani/Madani photo OCR benchmarks, reciter/redistribution rights, reviewed word timings, independent scholarly review and physical Android-device QA remain unresolved. Source-only PR snapshot is a separate checkpoint, not a release-certified app.


See [the current development handoff](docs/NEXT_STEPS.md) for source checkpoints, Actions evidence, and remaining device/asset gates.

**بِسْمِ اللّٰهِ الرَّحْمَٰنِ الرَّحِيمِ**

This Android Studio **source** project is not a complete verified Quran reader and
uses an offline runtime without INTERNET permission.

## Current verified continuation - 19 November 2026

Latest application source: `e53b8a2aa4547d58605d38178bd1174800d727c0`; [Actions 44506883081](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44506883081) completed/success. **20 JVM tests, zero failures/errors/skips; lint 0 errors / 46 warnings**. The existing Phase 7 audio-format validator now also runs in the combined CI gate. No tests were disabled and no APK/AAB was generated.

New source checkpoints bound camera JPEG allocation before decoding; scroll candidate reviews; recover interrupted audio/timing activation; persist recitation mode and selected verse/word without auto-play; require fresh printed-verse comparison; move local-data reads off the UI thread; retain/select up to eight audio packs and remove inactive packs. Legacy recordings are retained. Word timelines remain bound to exact audio/text hashes and must fit the prepared recording duration.

These are source and CI results, not device certification. Real-page OCR benchmarks, authentic permitted recordings, reviewed timings and scholarly/rendering review remain external gates. Live camera crop mapping, calibrated quality guidance, full localization/accessibility, other-screen state restoration, dependency-byte verification and broader storage/performance QA remain implementation work. See [current handoff](docs/DEVELOPMENT_HANDOFF_2026-11-19.md). Older sections below retain historical checkpoint evidence.

## Current scope

- Kotlin Android Native app, AndroidX splash, green/gold screens, sidebar, persisted settings and a restricted offline WebView style preview.
- Gallery and CameraX frame sampling (Phase 5/6 source), Arabic `tessdata_fast`
  offline OCR model. This model has not been trained/tested for every Pakistani/
  Madani Quran printing and cannot validate harakat precisely.
- Local offline candidate matching is fail-closed when Quran corpus is missing.
- Phase 7: offline user-provided verse MP3 ZIP import, SHA checks, local `MediaPlayer`.
- **Phase 8:** user-supplied verified-on-device word-timing ZIP import;
  verse text/audio SHA binding, strict timing validation, highlight current word
  without modifying Arabic text, previous/next word and play a selected word
  segment of the ayah recording. See `docs/PHASE8_WORD_TIMING_FORMAT.md`.
- No cloud services or HTTP calls are used by the runtime. Gradle needs developer-
  computer network access during initial build sync.

## Hard blockers before a useful / reliable Quran scanner can be released

1. The complete 6,236-verse Tanzil Uthmani v1.1 publisher snapshot is bundled,
   with untouched source, license notice, SHA-256 and exact round-trip validation.
   Independent scholarly review and physical-device rendering QA remain outstanding.
2. No permitted reciter MP3 files are included. User must obtain and independently
   review appropriately licensed recordings and import the valid audio ZIP.
3. No licensed human-reviewed word timing data are included. Use the new pack
   builder with approved timestamp and matching exact audio files.
4. Android Kotlin compilation and resources passed in GitHub Actions. Comprehensive OCR
   accuracy tests on Pakistani 15-line / Madani printings, emulator and physical-phone testing remain outstanding.
5. Visual UI, diacritic quality control, accessibility, memory/latency profiling,
   privacy review and final release packaging all need additional work.

## Build / verification

Android Studio recommended; JDK 17; Android SDK 35; minSdk 23 (Android 6+);
AGP 8.7.3; Kotlin 2.0.21; CameraX 1.4.2; Tesseract4Android 4.9.0.
**No Gradle wrapper included**; GitHub Actions provisions Gradle 8.10.2. See the Android APK section for current build instructions.

Pure-code checks (require Python 3, `kotlinc`, Java):

```bash
python3 tools/verify_phase8.py
```

Important: checks do **not** establish correct Quran text, Quran recitation,
Arabic OCR, word timing, or a successful Android build.

## Project files

- Native UI: `app/src/main/java/org/quranscan/reader/*.kt`
- Offline OCR: `app/src/main/java/org/quranscan/reader/ocr/`
- Canonical Quran structure and matching: `.../core/`
- Offline audio and timing: `.../audio/`
- Offline design preview: `app/src/main/assets/preview.html` (WebView displays
  visual design only, not camera or audio)
- Previous milestones: `docs/PHASE5_URDU.md`, `docs/PHASE6_URDU.md`, `docs/PHASE7_URDU.md`
- New timing format: `docs/PHASE8_WORD_TIMING_FORMAT.md`, `docs/PHASE8_URDU.md`
- Tanzil text licensing: `app/src/main/assets/quran/SOURCE_AND_LICENSE.txt`

## Sources and rights

Tanzil Quran Text (Uthmani, v1.1), © Tanzil Project, CC BY 3.0: https://tanzil.net/docs/Text_License
(complete publisher snapshot bundled). Tesseract4Android + Arabic tessdata_fast (Apache-2.0),
attribution in `docs/APACHE-2.0.txt` and `assets/tessdata/DEBIAN_SOURCE_AND_LICENSE.txt`.
Any audio/word timing pack is user supplied: its metadata and checksums do NOT
independently establish authenticity, tajweed accuracy, licensing, or timings.


## Phase 9 history: source-only CI

Native green/gold home actions updated and GitHub Actions workflow added in
`.github/workflows/source-validation.yml`. The workflow only runs Python checks,
Kotlin compilation and Android resource processing; **never assemble APK, bundle
AAB, sign, or publish**. The repository is connected and main is updated using the GitHub connection. Historical milestone notes are retained for context.

## Current audit — 19 November 2026

GitHub repository is connected. The previous Actions run failed on missing View / QuranVerseCounts imports and incorrect ScrollView property names; these are corrected. The next Actions result is the authority for Android compilation. No APK/AAB commands are authorized. The complete publisher Quran snapshot is now bundled; audio/timing assets remain absent. See `PROGRESS_REPORT.md` for the current phase assessment.

## Canonical text provenance

Downloaded twice from the Tanzil HTTPS publisher endpoint on 2026-11-19; both responses were byte-identical. Original source and complete copyright block are retained in assets/quran. `python3 tools/verify_corpus_provenance.py` checks 114 surahs, 6,236 references, source/output SHA-256 and exact Arabic equality including every mark. These checks do not replace independent scholarly review. Tanzil permits verbatim redistribution with attribution; see https://tanzil.net/docs/Text_License. No publisher-signed checksum was available.

## Expanded verification and UI

CI now runs source/dependency version policy, publisher corpus provenance, Python/pure Kotlin checks, `:app:compileDebugKotlin`, `:app:processDebugResources`, `:app:testDebugUnitTest`, and `:app:lintDebug`. No packaging tasks are used. Seven JVM safety tests cover ambiguous/incomplete matches, invalid references, frame throttling, timing boundaries/digests and unsafe audio manifests. Gallery decoding runs off the main thread with EXIF orientation handling. The canonical corpus is immutable and cached once per process. These improvements require Android device QA; consult `docs/EXTERNAL_ASSETS_AND_QA.md`.

## Latest source delivery

The full quality run [44465501620](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44465501620) passed compilation, resources, seven JVM tests (zero failed/skipped), Python checks and lint (zero errors, 43 warnings). Final small security/UI refinements are checked by the following Actions run. The original 59 source files are retained. See [PROGRESS_REPORT.md](PROGRESS_REPORT.md) for each phase, commit/run links, resolved failures, exact text digests and external asset/device blockers. **This remains an incomplete development source delivery.**

Final source validation: [44466055230](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44466055230), commit `db655274d5d3a3b53c257e580d791a81a88d5986`: success, seven tests with zero failures/errors/skips, lint zero errors and 36 warnings, all source/Python checks passed, no APK/AAB. Subsequent report-only changes leave application source unchanged.

## Camera and native UI continuation

Camera focus/metering, bounded exposure controls, persisted auto-pause, foreground recovery and stale-session rejection now have compile-only CI evidence (runs 44481527823 and 44481771624). Native Help/Exit, Settings privacy/storage guidance, safe system/keyboard insets and gallery callback navigation guards are the next source checkpoint. See PROGRESS_REPORT.md for exact tested commits, backup hashes and remaining manual gates. The reticle is still a guide for full-frame OCR; real Mushaf OCR accuracy and device behavior remain unverified.

Gallery now offers drag-to-crop before offline OCR, with a full-image fallback. Crop geometry is covered by a ninth JVM safety test; run 44482560224 passed. Final constructor/model-extraction/still-resolution refinements have a separate following CI run. Detailed remaining implementation tasks and external QA gates are tracked in docs/DEVELOPMENT_HANDOFF_2026-11-19.md. No APK/AAB has been produced.

Latest observed application-source validation: commit `5e5506695b8876e4c76ae13d6eb9c26e22d0015a`, [Actions 44482901626](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44482901626), completed/success: nine JVM tests, zero failures/errors/skips, lint zero errors / 39 warnings, all Python/Kotlin/resources/source gates passed, no APK/AAB. The following report-only checkpoint changes no application source. This remains a partial development delivery with explicit implementation and external QA gates in docs/DEVELOPMENT_HANDOFF_2026-11-19.md.
