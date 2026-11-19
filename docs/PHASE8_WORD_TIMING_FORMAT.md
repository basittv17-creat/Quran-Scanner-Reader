# Phase 8 — Offline word-by-word following / word segment playback

**Development source, not a Quran-certification claim.** This phase adds two controls:
(1) highlight the current canonical Arabic word during *existing local ayah audio*,
(2) seek within that same recorded ayah to hear one selected word. MP3 seeking may
start/end approximately, especially on old devices.

## Required sources, in order

1. Import and independently review **all 6,236 ayahs** as verbatim Tanzil text;
   source and license in `app/src/main/assets/quran/SOURCE_AND_LICENSE.txt`.
2. Obtain legal permission and review the reciter audio for correctness; import an
   `QSR-AUDIO-1` pack via the native app. The app contains **no MP3s**.
3. Obtain word start/end timing data for the **same exact MP3 bytes**, text edition
   and reciter. The app contains **no fabricated alignment data**. Word timestamps
   are NOT automatically derived or authenticated.
4. Build and import the user-supplied `QSR-WORD-TIMING-1` ZIP. A source hash and
   correct structural timing cannot guarantee alignment quality.

## Manifest and timing file format

ZIP at root only. No folders; at most 6,236 timing files and a `manifest.tsv`.
UTF-8 `manifest.tsv` example with placeholder digests:

```text
# QSR-WORD-TIMING-1
# reciter=SAME NAME AS QSR-AUDIO-1
# source=SPECIFIC TIMING SOURCE AND REVIEW INFORMATION
# rights=EXPLICIT LICENCE OR PERMISSION
1\t1\t001_001.tsv\t<TIMING_SHA256>\t<MP3_SHA256>\t<EXACT_CANONICAL_VERSE_UTF8_SHA256>\t4
```

Use literal TABs; columns: surah, ayah, filename, hash of TSV, hash of exact
installed MP3, hash of exact UTF-8 Arabic canonical verse, number of whitespace-
delimited words in the canonical source. Every record is tied to a specific verse.
`001_001.tsv` contains exactly one row per canonical word (no header):

```text
1\t<start_ms>\t<end_ms>
2\t<start_ms>\t<end_ms>
```

All timestamps are millisecond offsets **from the start of this ayah's MP3**.
Windows must have start >= 0, end > start, no overlaps, consecutive indices and
last end <= 900,000 ms. Timing gaps yield no highlighted word. These are safety
bounds, not a claim of recitation accuracy.

Build from real, separately reviewed local inputs:

```bash
python3 tools/build_word_timing_pack.py \
 --timings-dir /your/reviewed/verse-tsvs \
 --audio-manifest /your/mp3/manifest.tsv \
 --corpus app/src/main/assets/quran/verses.jsonl \
 --reciter 'REVIEWED RECITER' --source 'DATASET URL / VERSION' \
 --rights 'LICENSE / WRITTEN CONSENT' --output ~/qsr-word-timings.zip
```

`OfflineWordTimingStore` reads a ZIP through the Android file picker, rejects
path traversal, duplicate and oversized files, and validates the complete
import before activation. It rechecks sha256, audio-manifest identity and
canonical text before using any verse timing. A partial timing pack is allowed;
words without trusted timings cannot be highlighted or isolated.

## Important limitations

- There is no source of verified **Pakistan 15-line OCR training** in this ZIP.
- No audio, canonical full Quran, or word timestamps are currently bundled.
- The app can be *100% offline at runtime once offline assets are installed*,
  but a developer must acquire approved source assets and Gradle dependencies.
- Diacritics are not generated/changed: display comes only from canonical text.
- No automatic recitation starts from an uncertain OCR result.
- Word segment accuracy, tajweed, audio rights and print-variant accuracy all
  require expert human review and actual device tests prior to public release.

Candidate public datasets for separate research (do NOT auto-bundle):
- https://github.com/cpfair/quran-align — word-alignment project
- https://github.com/its3li/quran-word-timings — CC0 timing data for one reciter;
  licensing of accompanying MP3s must be checked independently
