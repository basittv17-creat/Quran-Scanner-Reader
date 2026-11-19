# Phase 7: Offline ayah audio pack — format and rights

**No reciter audio is included in this development ZIP.** You must secure documented
permission for any recordings distributed with the app. Audio available on public
websites is not automatically licensed for offline bundling or redistribution.

## Expected ZIP

Root-only entries (no directories), strict ASCII filenames:

- `manifest.tsv` (UTF-8)
- `001_001.mp3` (Surah 1, Ayah 1), `001_002.mp3`, ... up to `114_006.mp3`

`manifest.tsv` format (example with a **placeholder digest**, NOT a real audio clip):

```text
# QSR-AUDIO-1
# reciter=RECITER NAME — not independently verified
# source=DOCUMENTED RECORDING SOURCE
# rights=SPECIFIC PERMISSION OR LICENSE REFERENCE
1\t1\t001_001.mp3\t<ACTUAL_SHA256_OF_THE_MP3>
```

The four columns use literal tab characters. All clip names must be formatted with
three-digit surah and three-digit ayah, e.g. `002_255.mp3`. Accepted as **partial**
packs; a complete pack is exactly 6,236 clips matching the Quranic verse counts.

## Install safety

`OfflineAudioPackStore` uses Android Storage Access Framework and app-private storage.
It rejects unknown paths, repeated names, out-of-range verse IDs, mismatched SHA-256
and ZIP entries missing from the manifest. Limits: at most 6,237 entries, 8 MiB per
MP3, 1 MiB manifest, and 1,500 MiB uncompressed total. A new pack is activated only
after all hashes and structure have passed; an existing pack is retained on failure
where filesystem rollback permits. **Import replaces rather than merges packs.**

All playback uses Android's local `MediaPlayer` and *private local file paths only*.
The installed APK has no INTERNET permission. Build/dependency sync may use internet
on the developer computer. A user-supplied recording is **NOT** independently
verified, no matter its checksum or how the manifest describes its rights.

## Current blockers

1. The authentic complete 6,236-verse Quran corpus (`assets/quran/verses.jsonl`) is
   still empty. Playback is **disabled** until a fully vetted corpus is present.
2. No real licensed audio recordings are packaged or authenticated.
3. Word-by-word clips and timing/highlighting are a future milestone (Phase 8).
4. Android Studio build and physical-device tests are not yet performed.

For a text source see [Tanzil](https://tanzil.net/docs/Text_License); audio rights
must be checked separately. Never confuse SHA integrity with authentic qira'ah.
