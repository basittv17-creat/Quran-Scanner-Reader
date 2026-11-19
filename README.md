# Quran Scan Reader — Phase 9 (native Android / offline-first development source)

**بِسْمِ اللّٰهِ الرَّحْمَٰنِ الرَّحِيمِ**

This Android Studio **source** project is not a complete verified Quran reader and
**has not been built as an APK**. The intended runtime has no INTERNET permission.

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
**No Gradle wrapper included**; GitHub Actions provisions Gradle 8.10.2. Compilation/resources have passed; no APK/AAB was built.

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


## Phase 9: GitHub-ready / build-only CI

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
