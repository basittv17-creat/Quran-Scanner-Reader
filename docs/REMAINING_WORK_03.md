# Remaining work 03 — offline audio storage and recovery

## Audit

Started from main at `eb067178a9cf5bf7f6c24d43f48c6314f99ac0fd`. The cited [44533052361](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44533052361) and initial-head [44535664372](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44535664372) source-validation jobs completed successfully. Reviewed audio and timing code, tests, source-validation workflow, progress and Urdu status, the handoff, next steps, asset and QA requirements, OCR benchmark, workstream reports, and Phase 7/8 formats. Main advanced during the work. The implementation was pushed as a fast-forward from `f54e22d31a09ac5d7615f759a587cc5234b39eca` after comparison confirmed no overlap with the staged files.

## Storage changes

- The audio importer copies the selected ZIP to a bounded private staging file so it can validate the central directory, filenames, entry count, decompressed sizes and CRCs. It parses the UTF-8 manifest before extracting clips, enforces exact entry membership, and verifies each nonempty clip against the expected SHA-256. Clip paths are flat. ZIP metadata cannot create filesystem links. Limits include 1,500 MiB unpacked, 8 MiB per clip, 1 MiB for the manifest, and a 4 MiB classic ZIP central directory.
- Import temporarily needs space for the ZIP and its extracted files at the same time. The importer checks space while copying and before extraction. The storage layer reports cancellation, a conflicting import, access and space failures, and publication that saved a pack but could not select it. The temporary ZIP must be removed before publication. Space checks are advisory and writes can still fail.
- The catalog still retains up to eight packs including legacy. A selection failure after publication leaves the complete new pack available for explicit selection. A missing or invalid ID inside the selection record fails closed for playback and removal. The pack chooser can reselect a valid pack when the selection record has a missing or invalid ID. With no selection directory, the legacy default remains and removal requires explicit selection. Path checks protect recovery and deletion from linked locations.
- Stale import and selection stages are scanned at the next corresponding import or when reopening the pack catalog. Cleanup checks generated stage names and known flat contents, skips live stages, and preserves directories with unknown entries or links. The timing store canonicalizes the Android app-files parent before using the shared recovery checks.
- Pack selection validates clip hashes inside the catalog lock. The existing playback path checks the requested clip against its expected SHA-256 on the same open descriptor used by MediaPlayer. Explicit partial packs remain supported. The existing timing reciter, recording SHA, exact canonical verse SHA and timing-byte SHA checks continue to bind word timings to the selected recording.

The latest Recitation localization work uses generic import and pack-change strings. The storage layer supplies specific diagnostics. Surfacing those diagnostics, localized retry guidance and byte counts in the UI still needs coordination with the Recitation owner. This work has no Recitation screen or resource edits.

## Verification

- Local source policy, Phase 7 and Phase 8 including their Python pack checks, and canonical corpus provenance passed. The local OCR metrics suite passed 21 tests. The standalone JUnit suite passed 25 audio archive, catalog, staging, activation and recording tests. Fixtures cover malformed and truncated ZIPs, traversal and unexpected paths, oversized decompression, malformed manifests, wrong and missing content, symlink escapes, simulated space and access failures, interrupted imports, conflicts, capacity, inactive removal and restart states.
- All 6,236 Tanzil Uthmani v1.1 ayat including harakat, original notice and attribution matched the pinned bytes. Source SHA-256 `18c719bb3ba26d32ef457f40dad77cd28c4c5a34156833e26a8e5fcfdd246fb1`. JSONL SHA-256 `fa338471ce7cf0f08e57244458e143ceab555d1905d55e012d49da37cf541270`. Source policy confirmed no INTERNET permission and retained backup exclusions.
- Local Gradle 8.10.2 was invoked with the four authorized targets. The workspace could not resolve Android Gradle Plugin 8.7.3 because the Java dependency network route was unreachable. Hosted results are recorded below.

| Checkpoint | Actions | Result |
| --- | --- | --- |
| Parent `f54e22d` | [44537256365](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44537256365) | Success. 52 JVM tests, 21 OCR metric tests, lint 0 errors / 12 warnings. |
| Storage source `c2d10f392358cd60719101591753928cb77ac0e8` | [44537343642](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44537343642) | Success. All four Gradle targets passed. 64 JVM tests with zero failures, errors or skips, 21 OCR metric tests, lint 0 errors / 13 warnings and no APK/AAB. |

The additional `UsableSpace` lint warning recommends considering reclaimable cache. The import uses currently available private space and leaves cache eviction to Android and the user. No lint suppression was added.

## Device and asset gates

The JVM tests simulate process interruption by leaving filesystem states on disk and reopening the catalog. True power loss, filesystem write ordering, Android or OEM full-volume and permission behavior, process kill during import on Android, provider cancellation and MediaPlayer playback/seek need device testing. Authentic recordings with documented redistribution rights, reviewed word timings bound to the exact recordings, and scholarly review remain blocked pending supplied evidence. Fixtures contain synthetic bytes only.

No assemble, bundle, packaging, signing or publishing tasks were invoked. No APK or AAB was generated.
