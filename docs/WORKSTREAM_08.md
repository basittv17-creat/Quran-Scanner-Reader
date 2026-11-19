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

## Published checkpoint

[Commit 00b3486](https://github.com/basittv17-creat/Quran-Scanner-Reader/commit/00b34866e28e688c08a5ef524f2bee9232c5a550) was added to main after fast-forwarding over the concurrent localization commit `74ce21d`. The [Actions log for run 44528998064](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44528998064/job/135577047004) records successful Kotlin compilation and resource processing, 29 JVM tests with zero failures/errors/skips, 15 Python benchmark tests, lint 0 errors / 40 warnings, and no APK/AAB. The full source suite ran on Actions. Local Gradle and kotlinc were unavailable.

[Versioned source ZIP for 00b3486](https://drive.google.com/file/d/1qDWDSHSvcZ1dhFVIjVT5OhktFRBNr1hH/view) was uploaded as a new file. SHA-256: `757d959c13c3c8d04b276762088d116940fea5f4666fcb521ba005e5dff7508a`. Local ZIP validation checked archive integrity, both exact corpus digests and absence of APK/AAB entries.

The current app uses ranked candidate suggestions in its camera and gallery flows. The protocol now specifies how to record predictions without confusing alternatives with successive visible ayahs. A [Commons CC0 image lead](https://commons.wikimedia.org/wiki/File:Quran_in_a_page.jpg) lacks verified target edition and photographed-page provenance and was excluded.
