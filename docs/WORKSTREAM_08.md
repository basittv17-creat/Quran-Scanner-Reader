# Workstream 08 · offline OCR benchmark

## Baseline and scope

Fetched main at `cc73e0ae1b59d500ca87d443b01121899c8dcbeb`. Reviewed the progress report, Urdu status, development handoff, next steps, external asset gates, benchmark instructions, Kotlin recognition code, tests and source-validation workflow.

[Historical run 44514943361](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44514943361) at `d0f730d26fa1c67431696c03d1c4f7a611fe316e` was successful. The decoded job log confirms 28 JVM tests, zero failures/errors/skips, 7 Python benchmark tests, lint 0 errors / 43 warnings, and no APK/AAB.

The matcher requires a complete corpus, rejects short or ambiguous normalized candidates and returns canonical text for one matching verse with user confirmation required. Its exact-match score is a fixed value. The typed contract bounds references by surah. The benchmark scores raw OCR and ordered reference lists separately. Kotlin production code, workflow, Tanzil assets and camera code were left outside the change.

## Source checks

The evaluator now parses physical JSONL records, reports malformed lines and non-object rows, and hashes the unchanged input bytes. Fifteen Python tests pass locally, covering strict Unicode edit counts, empty OCR and abstention, invalid references, required fields, sample ID uniqueness, format mixing, grouping, and report digests. Existing metric formulas are preserved.

Local corpus verification passed for all 6,236 verses including marks and notice. Original Tanzil Uthmani v1.1 SHA-256 is `18c719bb3ba26d32ef457f40dad77cd28c4c5a34156833e26a8e5fcfdd246fb1`. JSONL SHA-256 is `fa338471ce7cf0f08e57244458e143ceab555d1905d55e012d49da37cf541270`. Source policy passed. No APK/AAB task was invoked.

## External dataset and review gates

| Gate | Status and evidence |
|---|---|
| Pakistani 15-line / Indo-Pak photographs | **BLOCKED.** A [Qudrat Ullah scan lead](https://islam.hasnats.com/mushaf/qudratullah-scan/1) documents the specific source PDF and uploader's CC BY-NC-ND 3.0 claim. Publisher authorization for the source artwork and permission for redistributed benchmark derivatives remain unverified. |
| Madani photographs | **BLOCKED.** The [King Fahd Complex rights page](https://dm.qurancomplex.gov.sa/rights/) addresses use of its digital edition. The [Commons Madinah PDF](https://commons.wikimedia.org/wiki/File:Madinah_Mushaf.pdf) is digitally prepared. A matching photographed-page collection with verified edition and photographer rights remains outstanding. |
| Ground truth and scholarly verification | **BLOCKED.** Need an identified fluent transcriber and an independent qualified Quran reviewer to sign off visible text, marks, verse numbers and boundaries from exact photographs. Independent corpus/display verification is also outstanding. |
| Capture and scoring | **BLOCKED.** Need the frozen, separately hashed page inventory, split, signed-off labels, representative device exports and exact model identities. See [OCR_BENCHMARK.md](OCR_BENCHMARK.md). No real-page accuracy was measured. |

No external images or reference labels were acquired or added. Existing Drive checkpoints were left in place.
