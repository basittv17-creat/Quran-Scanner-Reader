# Quran Scan Reader — Phase 9 (native Android / offline-first development source)

**بِسْمِ اللّٰهِ الرَّحْمَٰنِ الرَّحِيمِ**

This Android Studio **source** project is not a complete verified Quran reader and
**has not been built as an APK**. The intended runtime has no INTERNET permission.

## Current scope

- Kotlin Android Native app, green/gold screens and an offline local WebView style preview.
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

1. `app/src/main/assets/quran/verses.jsonl` is still **EMPTY**, and cannot be
   claimed as a verified 6,236-ayah Quran. Import verbatim verified Tanzil v1.1
   text with `tools/import_tanzil.py` after separately obtaining the correct source
   and reviewing the publisher's license and provenance. Never generate/guess ayahs.
2. No permitted reciter MP3 files are included. User must obtain and independently
   review appropriately licensed recordings and import the valid audio ZIP.
3. No licensed human-reviewed word timing data are included. Use the new pack
   builder with approved timestamp and matching exact audio files.
4. No comprehensive OCR accuracy tests on Pakistani 15-line / Madani printings,
   Android SDK compile, emulator or physical-phone testing have taken place.
5. Visual UI, diacritic quality control, accessibility, memory/latency profiling,
   privacy review and final release packaging all need additional work.

## Build / verification

Android Studio recommended; JDK 17; Android SDK 35; minSdk 23 (Android 6+);
AGP 8.7.3; Kotlin 2.0.21; CameraX 1.4.2; Tesseract4Android 4.9.0.
**No Gradle wrapper included** and Android SDK build has not been run here.

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
(complete text NOT yet bundled). Tesseract4Android + Arabic tessdata_fast (Apache-2.0),
attribution in `docs/APACHE-2.0.txt` and `assets/tessdata/DEBIAN_SOURCE_AND_LICENSE.txt`.
Any audio/word timing pack is user supplied: its metadata and checksums do NOT
independently establish authenticity, tajweed accuracy, licensing, or timings.


## Phase 9: GitHub-ready / build-only CI

Native green/gold home actions updated and GitHub Actions workflow added in
`.github/workflows/source-validation.yml`. The workflow only runs Python checks,
Kotlin compilation and Android resource processing; **never assemble APK, bundle
AAB, sign, or publish**. The Github repository has **not** been created from this
environment; see `docs/PHASE9_GITHUB_URDU.md` and the optional user-run
`tools/push_to_new_github_repo.sh`.

## Current audit — 19 November 2026

GitHub repository is connected. The previous Actions run failed on missing View / QuranVerseCounts imports and incorrect ScrollView property names; these are corrected. The next Actions result is the authority for Android compilation. No APK/AAB commands are authorized. The bundled Quran file remains empty and audio/timing assets are absent. See `PROGRESS_REPORT.md` for the current phase assessment.
