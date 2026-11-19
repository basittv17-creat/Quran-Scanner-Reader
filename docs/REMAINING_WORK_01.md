# Remaining Work 01 — Camera lifecycle, crop mapping and safety

## Scope and changes

Audited main at `c4321b17f76676d9027818d4fe6efac90eb151af`, six commits ahead of the supplied camera reference. Reviewed the central reports, handoff, next steps, external asset and QA requirements, benchmark protocol, available workstream reports, source-validation workflow, camera activity, reticle, scan gate, and tests. Reference run [44533052361](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44533052361) and the initial main run [44536233246](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44536233246) completed successfully. During the audit, Work 05 changed the activity layout and gave the preview a stable height, preventing status text wrapping from changing the preview viewport. The crop commit incorporates that change and the concurrent Work 02 OCR change.

| File | Change |
| --- | --- |
| `app/src/main/java/org/quranscan/reader/LiveScanActivity.kt` | Both in-memory capture and analyzer frame conversion call the same crop-or-full-image routine before rotation and resizing. |
| `app/src/main/java/org/quranscan/reader/core/LiveScanRegion.kt` | Reject malformed direct selections and explicitly reject invalid viewport edge ordering. Resolve missing or unusable mapping to the full decoded bitmap. |
| `app/src/test/java/org/quranscan/reader/LiveScanRegionTest.kt` | Test all quarter turns with wide analyzer and tall sampled JPEG inputs, offset viewports, uneven decode sizes, outward rounding, malformed bounds, and full-image fallback. Reversed user drags still produce ordered bounds. |

The opt-in full-image action uses the full decoded buffer. A selection spanning the preview uses the CameraX crop rectangle. The rear preview, analyzer, and optional still capture share one CameraX ViewPort within a UseCaseGroup. Each proxy provides its own source dimensions, crop rectangle, and rotation. The selection is inverse-rotated into that proxy's raw viewport and scaled to decoded bitmap dimensions with outward integer rounding. Preview plus analysis remains the fallback when the three-stream combination is unsupported. See [CameraX ViewPort](https://developer.android.com/reference/androidx/camera/core/ViewPort) and [CameraX configuration](https://developer.android.com/media/camera/camerax/configuration).

## Lifecycle review

- Camera permission is checked before binding and capture, and before delivering OCR. A denied or revoked permission releases the binding when detected. Missing rear camera, provider initiation errors, and binding errors have guidance and retry paths. Camera state observation and use-case removal belong to the activity.
- Pause invalidates the session before clearing the analyzer and unbinding. Resume, display rotation, and preview dimension changes can start another session. Provider, capture, and OCR callbacks check session identity. OCR UI delivery also checks selection identity, permission, and activity exit. Older results cannot replace the current UI after a rebind or crop change.
- Still and analyzer claims are mutually exclusive. An analyzer-only manual request is consumed once and can run while automatic scanning is paused. Late completion cannot clear a newer claim.
- Both proxy delivery paths close their image in `finally`, including early returns and conversion errors. JPEG capture is in memory with decode limits. The conversion disposes of intermediate bitmaps and OCR recycles its bitmap in `finally`. Already-running native OCR can finish after pause, with stale UI delivery suppressed. Review candidates still require explicit user action.

## GitHub Actions

The [exact crop commit cc6126d](https://github.com/basittv17-creat/Quran-Scanner-Reader/commit/cc6126d6eaf6b7792d44e8c7ffa977fa268e8bd5) reached main by a non-forced fast-forward. Its [run 44537101027](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44537101027) passed Python checks, Kotlin production compilation, and resource processing. JVM test compilation stopped on a type mismatch in the pre-existing Work 02 `ScanImageQualityTest.kt:38`, also seen in run 44536790953. The exact run has no JVM or lint totals.

Work 02 corrected the signed ARGB literal in `f54e22d`, which includes the crop changes. [Run 44537256365](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44537256365) completed all four authorized Gradle tasks. Its job log reports 52 passing JVM tests with zero failures, errors, or skips, 21 passing Python benchmark tests, and lint with 0 errors and 12 warnings. Both runs confirmed no APK/AAB.

## Verification and backup

- Local Kotlin/JUnit camera tests passed 12, with zero failures, errors, or skips. This includes eight crop tests and four session gate tests. The Phase 8 source suite passed, including pure Kotlin checks and synthetic pack validation. Python passed 21 benchmark, 6 source policy, 6 corpus provenance, 6 Tanzil importer, and 2 native resource tests, with zero failures or skips. Gradle and an Android SDK were unavailable locally.
- Corpus verification confirmed all 6,236 ordered references match the pinned Tanzil Uthmani v1.1 bytes, including diacritics and the attribution notice. Tanzil source SHA-256 is `18c719bb3ba26d32ef457f40dad77cd28c4c5a34156833e26a8e5fcfdd246fb1`. JSONL SHA-256 is `fa338471ce7cf0f08e57244458e143ceab555d1905d55e012d49da37cf541270`.
- [Versioned source ZIP for cc6126d](https://drive.google.com/file/d/11Ughrm3ObWu54JXcvduZ2762n1agBxqV/view) contains 1,565,852 bytes. Verified ZIP integrity, exact corpus bytes including the license file, and absence of APK/AAB entries before upload. ZIP SHA-256 is `ae2abe54213e137d80b0c99440260ec48bb5e8e5177ed146c3a8f8a75c4267b9`. Earlier backups were preserved.

## Pending device and external checks

**PENDING** on physical Android hardware with an authorized existing installation. Compare the reticle and OCR crop for still capture and analyzer frames at 0°, 90°, 180°, and 270°, across different aspect ratios, and after layout changes. Exercise denial, revocation, missing hardware, camera initialization and bind retry, screen lock, foreground transitions, rapid taps, delayed callbacks, proxy teardown, and fallback. Verify large text and TalkBack in English and Urdu. JVM geometry and gate tests establish deterministic behavior only.

**BLOCKED** for real-page OCR until rights-cleared Pakistani 15-line and Madani captures and independently reviewed labels are available. Authentic recitation recordings, reviewed matching word timings, and qualified Quran text and rendering review remain external requirements.
