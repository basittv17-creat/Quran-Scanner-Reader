# Quran Scanner Reader — source delivery report

## Current source and validation

Main was audited at `d0f730d26fa1c67431696c03d1c4f7a611fe316e`. [Run 44514943361](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44514943361) passed with 28 JVM tests and lint 0 errors / 43 warnings. Live crop, bounded decoding, audio catalog and import recovery, and persistent recitation mode were already present.

| Source | GitHub Actions | Verified evidence |
|---|---|---|
| `8f16855704fae449035bf4077e241d74652f8472` | [44527493461](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44527493461) | Settings, Help and About in English and Urdu. Screen restoration added. 28 JVM tests and lint 0 errors / 42 warnings. |
| `e8835dcbb850729c09abff281994307652715c64` | [44527849953](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44527849953) | Explicit full-camera-image option, saved with the existing crop state. 29 JVM tests and lint 0 errors / 42 warnings. |

Both runs passed source-only Kotlin compilation, resource processing, 7 Python OCR benchmark tests, source policy, and the no-APK/AAB gate. JVM tests reported zero failures, errors and skips. All 6,236 Tanzil Uthmani v1.1 verses and both corpus digests were verified. Camera alignment and accessibility on physical Android remain pending.

[Run 44528335088](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44528335088) passed for `cc73e0ae1b59d500ca87d443b01121899c8dcbeb` with 29 JVM tests, zero failures/errors/skips, lint 0 errors / 42 warnings and no APK/AAB. JitPack resolves only the Tesseract4Android module. See the [dependency and private-storage review](docs/NEXT_STEPS.md) for outstanding byte-integrity and native-library advisory work.

[Run 44528737203](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44528737203) passed for `74ce21d33895d6239d5c33d2bf2c9855343fd12e` with 29 JVM tests, zero failures/errors/skips, lint 0 errors / 40 warnings and no APK/AAB. Primary live scan and recitation controls use English and Urdu resources and wrap at larger font sizes. Device layout and TalkBack checks remain pending.

The [full-image source backup](https://drive.google.com/file/d/1XB5QkQhFVGDYeafQn7uHSFpuOzhumTig/view) is a new versioned ZIP. Its SHA-256 is `f1ae9a40db7f04cb3d08d9b1d843f6b507bbcd8fbbe106058f8542032dd27cc9`. Local archive validation checked ZIP integrity, both Quran corpus digests and absence of APK/AAB entries.


The [development handoff](docs/NEXT_STEPS.md) records the live crop, OCR diagnostics, gallery restore, audio safety, localized navigation, source-validation pins, and local preview checkpoints. Historical results follow.

Date: 19 November 2026. This is an incomplete development delivery, not a release-ready application. No APK/AAB was generated. All 59 original baseline files remain present; history is retained.

## Current verified continuation (supersedes older counts below)

Application/test/tool source `e53b8a2aa4547d58605d38178bd1174800d727c0`, Actions [44506883081](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44506883081), completed/success: **20 JVM tests, 0 failures/errors/skips; lint 0 errors / 46 warnings**, Python/pure Kotlin/corpus/Kotlin/resources checks passed; no APK/AAB. Five new source checkpoints have observed successful CI. Session baseline was 18 commits / 79 tracked files, not the stale 10-commit baseline. All original 59 baseline files and exact Quran assets remain.

Delivered: bounded camera JPEG decode and scrollable review; recoverable imports; persistent recitation mode/replay and selection state; fresh verse comparison; background data reads; eight-pack retention/reciter selection/inactive removal; fail-closed selection and recording-duration timing checks. This is a partial development delivery. Implementation TODOs and external gates are explicitly separated in docs/DEVELOPMENT_HANDOFF_2026-11-19.md. No percentage or release-readiness claim is made.

Final application source ZIP: https://drive.google.com/file/d/1m3rlUiAiuDpwYg00_mvLvLARcbzloL6v/view ; SHA-256 `ea0d2fed54864f104afdeba325511f3ff3084a425cb1561aadaebcca8b567624`. v19-v22 checkpoints/reports are separately backed up; milestone Gmail notifications sent. A following report-only snapshot/CI and consolidated report provide final delivery evidence.

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

## Final observed source validation

Source commit: https://github.com/basittv17-creat/Quran-Scanner-Reader/commit/db655274d5d3a3b53c257e580d791a81a88d5986

Actions: https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44466055230 — **completed / success**. Kotlin compilation, resources, Python/pure Kotlin checks and source policy passed. **7 JVM tests, 0 failures/errors/skipped; lint 0 errors and 36 warnings. No APK/AAB found.** These final refinements reduced warnings from 43 to 36 without suppression. Remaining warnings concern fixed older dependencies/target API and translation/locale handling; the full issue list is in the Actions report step. No runtime/device/recording-quality certification is implied.

The following commit only updates reports; application source remains the validated db65527 source.

## 2026-11-19 continued development — audit and OCR measurement

Rechecked main at `1835f41fcc42faeae79993cc2597b99f35d564e4`: 10 commits, 71 tracked files; latest Actions run 44466318864 completed successfully. Read the supplied roadmap ZIP; its earlier lint failure has already been fixed in main.

Corpus provenance rerun locally: all 6236 entries match pinned publisher bytes including marks. Added an offline OCR-export evaluator with strict character/word edit rates, exact ordered reference-list recognition accuracy, abstention/wrong-prediction counts, per-printing groups and measured mean/p95 latency. Five synthetic metric unit tests pass locally. Synthetic tests are not Mushaf accuracy evidence. Real legally usable reviewed image exports remain BLOCKED; no accuracy percentage is claimed.

Fixed obsolete Phase 7 validation that still required an empty corpus. CI now tests benchmark calculations in addition to the existing Android and corpus gates. Local environment has no Gradle/kotlinc installation; Android and pure Kotlin checks use GitHub Actions. No APK/AAB tasks were invoked.

### OCR measurement checkpoint verified

Commit `507157fc79acdce7dad38012d1e7ebeaa39707d4`, Actions https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44479977288 completed successfully. Logs confirm 7 JVM tests (zero failures/errors/skips), lint 0 errors / 36 warnings, five benchmark unit tests and no APK/AAB. Source ZIP https://drive.google.com/file/d/1MEIjAvFLCb_swUyrsBvBXnxo00796-aw/view ; PDF https://drive.google.com/file/d/1kEIyhMkhA_h-aGviI1LOu7l6Gr60Zxb_/view . SHA-256: `abcbb2f5352f6d8fa73a374b0ec253d1c28d8da284e5175f02735e54821beb1d`. Gmail notification sent to requested address.

### Native audio controls and interruptions

Replaced synchronous MediaPlayer preparation with prepareAsync. Delayed callbacks check current player identity; Pause during preparation/seek prevents later background start. Playback waits for seek completion, checks timing against exact audio duration, requests audio focus with API 23 fallback and pauses on focus loss/headphone disconnect. Resume is explicit. Added previous/next ayah selection across all surahs, without auto-play or wrapping at Quran boundaries. A JVM test checks all adjacent references and round trips. Device playback/interrupt/seek QA remains DEFERRED under the no-APK rule. No recording/timing assets added.

### Camera lifecycle and manual controls checkpoint

Audit started from `dc48d48425df144b6d1f21365965fd7d1b0c0938` (12 commits). Actions run 44480551742 was observed completed/success. Camera opening is now retried on each foreground start, with session tokens rejecting obsolete provider/OCR callbacks after backgrounding. Manual capture claims the processing gate atomically, and closes still-image camera buffers before OCR. Added tap focus/metering, bounded darker/brighter exposure controls, and persisted automatic-scan pause preference. No OCR certainty, crop accuracy or device QA claims are made.

Local source policy, complete corpus provenance and five OCR metric tests passed. Local Gradle/kotlinc are unavailable; Android compilation/resources/JVM/lint must be verified by the new GitHub Actions run. Device lifecycle, camera focus/exposure, rotation and layouts remain deferred under the no-APK rule. Licensed real-page fixtures, recordings, reviewed word timings and scholarly review remain blockers.

### Camera checkpoint verified; native navigation/insets follow-up

`c1106a0951c49bb98731d2583eb47c0ae3224cd0`: run https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44481527823 succeeded. Lifecycle dispatch refinement `5752a35201061e7fb7977b5f08a5ede1b0a154fd`: run https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44481771624 succeeded. Both reported 8 JVM tests, zero failures/errors/skips, lint zero errors / 39 warnings, no APK/AAB.

Camera source v13 backup: https://drive.google.com/file/d/19Rada_0M-fAmKKJRlw--F9sIP-rDKzoe/view ; report https://drive.google.com/file/d/1OVIpuSvVLggGecCTQl4KWPQBXZuTP69P/view . ZIP SHA-256 `2a0951938409a91c81c60feaa5e6fb89417cee5c08be7e1d14d090e2fba30368`. This earlier ZIP contains c1106a0; the final source checkpoint includes the lifecycle dispatch correction.

Next native UI changes add Help and Exit navigation, prominent Live Quran Scan / Upload Quran Picture actions, auto-scan preference in Settings, offline storage/privacy guidance and system bar/cutout/keyboard insets for native screens and the preview. Navigating between native screens invalidates pending gallery callbacks. Source policy/diff checks pass locally; Android CI for this follow-up is required. Visual/accessibility/keyboard tests still require authorized device QA. No new recordings, timings or OCR truth data added.

### Native UI verified; optional gallery crop

UI commit `5ea42507430f5f5b773fa319b825035c6f87fb70`, Actions https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44482128467 completed successfully: 8 JVM tests, no failed/skipped tests, lint 0 errors / 39 warnings, Python checks and no-APK gate passed.

Added a bounded decoded-image crop preview before gallery OCR: drag in the fitted image, clamp/reverse bounds safely, scan selection or full image, or cancel. Crop math has synthetic JVM coverage for reversed/out-of-range drags, fractional edges, tiny and non-finite input. Text bytes are never interpreted or changed by crop math. Full-image fallback remains available for accessibility. Gesture/EXIF/layout rendering and OCR effects still need device and reviewed real-image testing. Camera reticle remains a guide. See docs/DEVELOPMENT_HANDOFF_2026-11-19.md for explicit implementation TODOs as well as external blockers.

### Gallery checkpoint verified; final source refinements

Gallery crop commit `71114fa9ffe52c4f81c621382dff4a921ee2d224`, run https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44482560224 completed/success: 9 JVM tests with zero failures/errors/skips, lint 0 errors / 40 warnings; full Python/Kotlin/resources/source checks passed, no APK/AAB. The new warning was a custom-view tooling constructor; the follow-up uses a standard Context/AttributeSet constructor without suppressing lint.

Final refinements also request a smaller still-camera resolution (hardware may choose a nearby supported size; this is not a proven memory ceiling) and serialize OCR model extraction so overlapping gallery/camera workers cannot race on the same partial file. Android CI for these refinements follows this commit. Device/real-image tests remain deferred. All 59 baseline files are retained; verified corpus assets are unchanged.

Native UI source ZIP: https://drive.google.com/file/d/1Bh9ZoZS-p7a-HRkQdutDsuQ5frfNHOeQ/view ; SHA-256 `126aa8bcb1409c6ec96c009cf493e63ec7d0698fb64b6267d3ce2497642d3ce6`. Camera v13 report/ZIP notification was sent to basittv17@gmail.com. The final tested source ZIP/PDF will be a new version, preserving older backups.

## Latest observed verified source checkpoint

Source: `5e5506695b8876e4c76ae13d6eb9c26e22d0015a` (17 commits / 79 tracked files before this report-only update). Actions https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44482901626 completed/success. Kotlin compilation, resources, Python/pure Kotlin validations, all **9 JVM tests (0 failures/errors/skipped)** and lint **0 errors / 39 warnings** passed. The custom-view constructor warning is resolved without suppression. The no-APK/AAB gate passed. Original 59 source files and exact verified Quran assets are preserved.

All five development commits in this session have observed successful Actions results: 44481527823, 44481771624, 44482128467, 44482560224 and 44482901626. This following report-only update changes no application/test/asset/build files and is also checked by Actions. No claim of completed OCR, audio, timings, device QA or final release is made.

Implementation still needed: live camera crop mapping, calibrated blur guidance, multiple audio-pack selection/storage management, recitation-mode persistence, complete localization/accessibility/rotation restoration and dependency-byte verification. External needs: reviewed licensed Pakistani/Madani image fixtures, authentic rights-cleared recordings, human-reviewed timings, qualified scholarly/display review and authorized device QA. See docs/DEVELOPMENT_HANDOFF_2026-11-19.md for resumable steps.

## Continued audit and bounded camera decoding

Re-audited main `4851b169ca92081907575370c06bd1075a22d3e1`: 18 commits, latest run 44483235204 completed/success. Preserved all prior source and canonical assets. Camera JPEG decoding now reads dimensions and samples before allocating the bitmap; oversized analysis buffers and compressed captures are rejected before conversion. Gallery uses the same overflow-safe, ceiling-based sampling calculation. Candidate review controls share a scroll area so they remain reachable on smaller screens. A new JVM test covers invalid, boundary, very large and maximum integer dimensions. Local source policy, corpus provenance and five OCR metric tests passed; Android CI follows this checkpoint. These bounds do not establish a measured device RAM ceiling; runtime/performance QA remains deferred under the no-APK rule.

### Memory checkpoint verified; recoverable pack publication

Commit `94727e4b9ff72b1bdaa9c5a5569bdff8630b5571`, Actions 44504375784 completed/success: 10 JVM tests, zero failures/errors/skips, lint 0 errors / 39 warnings. Source ZIP https://drive.google.com/file/d/1_Ay-ZxdtIw5I9alDNtKy5snkrlM6pbo4/view; SHA-256 `87bcd0b7f65be9dc9dfeab927d4fb2d02926c3c7ce25e9eac847d82ce23b156a`. Report backed up and Gmail notification sent to the requested address. Roadmap PDF and work-prompt ZIP were read; their older lint failures are already fixed.

Next source checkpoint adds shared recoverable audio/timing directory publication. On process interruption before publication the old pack is restored; after publication the validated new pack wins. Legacy single backups can recover, ambiguous backups remain untouched, and import loops honor interruption before activation. Five synthetic filesystem tests cover interruption states, failed publication/rollback, invalid staging and legacy ambiguity. Existing byte/manifest validation remains required. This does not promise power-loss durability or device process-death QA. Old stale stage directories are retained rather than guessed valid. Android CI follows this commit.

### Recovery verified; recitation state and explicit comparison

Commit `7a957be2e43fca6861c67c70ad074ad223c126c2`, run 44505052171 completed/success: 15 JVM tests (zero failures/errors/skips), lint 0 errors / 40 warnings, Python checks and no APK/AAB. Source ZIP https://drive.google.com/file/d/1XC5RppISUaZinyE5iaBd7Yl9o4oTR3IF/view; SHA-256 `3b390cfb853cd59f25695df5ab4a7b2fea09f2a2ce8ccd7906fc08f5c941e57c`. Report and Gmail notification saved. The extra warning concerns a translatable UI string; no suppressions added.

Next checkpoint persists full-ayah/follow-words/selected-word mode, adds explicit play/replay controls, restores verse/selected-word state after recreation without resuming playback, and requires a fresh checkbox confirmation against the printed verse before playback/resume. Corpus/manifest/timing reads move to the background worker; obsolete selection callbacks are rejected. New user-facing mode/confirmation strings use Android resources. Device rotation/rendering/playback QA still deferred; no new audio or timings bundled.

### Recitation state verified; multiple local audio packs

Commit `37b4c0a65cb05550664bbe6a81002a3f14833ca5`, run 44505655076 completed/success: 15 JVM tests, zero failures/errors/skips; lint 0 errors / 42 warnings. Source ZIP https://drive.google.com/file/d/1rT5gPWaCGw_cUU7yrkQ93mUQvgrcfdQP/view; SHA-256 `34bdbc093217cba660d8d389b58b306b62eb78f0250c0299a29e99cb733f07db`. Gmail notification sent. Android mode/confirmation strings are resource-based; existing hardcoded strings still need full localization.

Next checkpoint preserves up to eight local audio packs instead of replacing the previous recording set. A recoverable selection record chooses an immutable validated directory; legacy active audio remains selectable. Native dialogs select reciter/pack and remove only inactive packs with confirmation. Exact timing/audio/text binding remains enforced, so switching recordings cannot silently reuse mismatched timings. Five catalog tests cover retention, legacy selection, interruption recovery, unsafe IDs, deletion and capacity limits. A complete pack interrupted before selection may remain safely stored and selectable. This is not full storage/low-space/device QA. No recordings or timings were added.

### Audio catalog verified; final safety refinements

Commit `cf25dc5ab43c297ad1d475c9cbd2be9cad344fbd`, Actions 44506327553 completed/success: 20 JVM tests with zero failures/errors/skips, lint 0 errors / 46 warnings; all Python/Kotlin/resources/source/no-APK gates passed. Source ZIP https://drive.google.com/file/d/1X16ac6R2iLsBYTj4aUj7wTPtu5GVmxIX/view; SHA-256 `c64dcaa47de358b1eb1f552a8d8b6628af94ea78b5e8035cfa9716ae1ff1fb1d`. Report backed up and Gmail notification sent.

Follow-up rejects a missing selection record inside an existing selection directory instead of silently falling back to legacy audio; catalog tests now cover this corruption. Full-ayah word highlighting additionally checks the entire timeline end against prepared recording duration. Import errors/success notices survive refreshing the verse. The existing standalone Phase 7 validator is now executed by the combined Phase 8 gate, with updated real method names and catalog publication assertion; no tests disabled. Android CI follows this checkpoint.
