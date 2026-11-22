# Automatic Quran Scan → Canonical Arabic Reader → Offline Recitation

Updated 22 November 2026. Approved product requirement for the existing **native Kotlin** Quran-Scanner-Reader app, on **main**.

## User journey
1. Home offers two prominent choices: **Live Quran Scan** and **Upload Quran Picture**.
2. Selecting a gallery photo or screenshot immediately starts local decoding, OCR and verse matching; the user can crop/retry after results.
3. Live CameraX preview periodically attempts offline OCR with manual capture available.
4. OCR from a page can contain **multiple** verses: match the corpus's complete verse strings inside page OCR, display the surah/ayah reference and the **unaltered Tanzil Uthmani v1.1** Arabic with all canonical diacritics. Keep the raw OCR separately marked unverified.
5. For a **single, long, dominant, unambiguous** complete match, open the native Arabic reader and start offline playback only if that specific local audio recording exists and passes the stored SHA-256 check. Otherwise show a missing-audio/import message. Multiple/short/partial/noisy matches require review and cannot autoplay.
6. Preserve playback controls, existing selection confirmation on ambiguous cases, font scaling, and word highlighting only when correct reviewed timing packages are installed.

## Important limitations and acceptance
- Generic Arabic Tesseract has not been validated for Pakistani 15-line and Madani printed Quran pages. No claim of comprehensive Quran OCR accuracy; exact-match tests use synthetic data only. Full product work requires permissioned photographed pages, independently labeled references, CER/WER/reference accuracy metrics, reciter rights and authentic recordings, reviewed word timings, scholarly check, and physical-device QA.
- No internet permission, uploaded images, cloud OCR, unauthorized audio, modified Quran assets, or Flutter rewrite.
- **No APK/AAB**, packaging, signing or publishing tasks. Only source policy, Python/Kotlin compilation, Android resources, JVM tests, lint, corpus/digest verification. Device QA deferred until explicit approval to package.
- Store milestones and source ZIPs in versioned Drive backups and Gmail; never overwrite historical copies. Source-only CI passing is not release certification.
