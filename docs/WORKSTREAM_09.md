# Workstream 09 — dependency integrity and offline privacy

## Baseline and validation

Fetched `main` at `cc73e0ae1b59d500ca87d443b01121899c8dcbeb`. Read the progress/status reports, handoff, next steps, external assets and OCR benchmark documentation, build settings, manifest, extraction rules and source-validation workflow.

| Commit | Actions run | Raw log evidence |
| --- | --- | --- |
| `d0f730d26fa1c67431696c03d1c4f7a611fe316e` | [44514943361](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44514943361) | Success, 28 JVM tests, 7 Python OCR metric tests, lint 0 errors / 43 warnings, no APK/AAB |
| `cc73e0ae1b59d500ca87d443b01121899c8dcbeb` | [44528335088](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44528335088) | Build successful, 29 JVM tests with zero failures/errors/skips, 7 Python OCR metric tests, lint 0 errors / 42 warnings, no APK/AAB |

Local Python source policy, five new privacy tests, seven OCR metric tests and both synthetic audio/timing pack builder checks passed. Corpus provenance verified all 6,236 ayat including marks and the retained Tanzil notice. The derived corpus SHA-256 is `fa338471ce7cf0f08e57244458e143ceab555d1905d55e012d49da37cf541270`. Quran assets, licenses and recitation gating were left intact.

## Dependency evidence

Repository-level resolution prohibits project repositories and scopes JitPack to the OCR module. Versions are fixed at the direct dependency and plugin declarations. No Gradle wrapper or verification metadata is tracked. CI uses a read-only token, checkout without persisted credentials, and the hosted SDK. Raw baseline logs show the runner resolved the four named upstream repositories at checkout `11d5960a326750d5838078e36cf38b85af677262`, setup-python `a26af69be951a213d495a4c3e4e4022e16d87065`, setup-java `cf277c60eb25467037889841efdb72551f06f6c3` and gradle/actions `ed408507eac070d1f99cc633dbcf757c94c7933a`. Runner images, runtime patch selection, and Gradle cache state are separate supply-chain inputs.

The following SHA-256 values were computed from actual upstream HTTPS downloads. Google Maven means `https://dl.google.com/dl/android/maven2/` and Central means `https://repo.maven.apache.org/maven2/`; resolve paths using the standard Maven coordinate layout. The Kotlin row names a specific Gradle plugin variant. For these individual Maven/JitPack artifacts the digests are observations of served bytes. Independent publisher signature validation, source-to-binary reproducibility, and equivalence to the CI cache remain unverified.

| Artifact | Source | Observed SHA-256 |
| --- | --- | --- |
| core-splashscreen-1.0.1.aar | Google Maven | `25c813ae7795c99dd20628527043f8d0425eb2e826e6c42cb77acbb6c01dde68` |
| drawerlayout-1.2.0.aar | Google Maven | `f4ba16c51d5182497fe128e298af930f0ec6bd5497c7440a4c67042d15dd7702` |
| exifinterface-1.3.7.aar | Google Maven | `0e8f1832266c5b0667ad3d3b1098e624e49a09075493a014a7e88af01fd30ad3` |
| activity-1.9.3.aar | Google Maven | `7e45d2023dc17d715ffff3dc5792f798642c4ae11c948006b1752ab9edda5707` |
| camera-core-1.4.2.aar | Google Maven | `bbca13760ce1587502eb15723d0e17fa0a3495d084e609c4e4c8d3c2e1c85cf8` |
| camera-camera2-1.4.2.aar | Google Maven | `cafb962aa98c9a580f4d24edb8ef24bb58bf7b5ab06af612570bef4a7c08efb7` |
| camera-lifecycle-1.4.2.aar | Google Maven | `8355383d86060ba2ac260f749305b7a3bcc9fa80b534b46e814e2140871998ce` |
| camera-view-1.4.2.aar | Google Maven | `9b4881e451330fad587833dd57da275084a8a1032f9429c364c2d9641426b123` |
| gradle-8.7.3.jar (AGP) | Google Maven | `abb4f9e92c9838bd9989ba05968ec892189e16e23f42adba2e063843da04070d` |
| kotlin-gradle-plugin-2.0.21-gradle85.jar | Central | `9df5c7ff13b1fc68ac9450ca63c53111891d6f647bddec0f4398b3e7225bf6d9` |
| junit-4.13.2.jar | Central | `8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3` |
| tesseract4android-4.9.0.aar | JitPack | `bce5d6413a1a5ae3d7240033fbbc851ba3217d0a08d9769400e17a077f42cb2a` |
| gradle-8.10.2-bin.zip | Gradle distribution service | `31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26` |

The Gradle ZIP digest matched Gradle's [published release checksum](https://gradle.org/release-checksums/) before extraction. Full artifact and metadata enforcement is **BLOCKED** pending a coordinated Gradle/CI change with a clean resolution of plugin markers, all POM/module metadata, transitive and platform-specific artifacts, independent checksum/signature review, and strict verification on all four allowed tasks. Partial metadata could break builds or give false assurance. [Gradle verification guidance](https://docs.gradle.org/current/userguide/dependency_verification.html)

## Advisory triage

- Gradle 8.10.2 falls in the affected ranges for the SSL repository fallback and daemon/worker advisories. The listed 8.x fixed versions are 8.14.6 for the SSL and worker issues and 8.14.7 for stale daemon registry entries. Coordinate a compatible AGP/Kotlin/JDK validation before changing the shared toolchain. Hosted isolation and `--no-daemon` do not establish remediation. [SSL](https://github.com/gradle/gradle/security/advisories/GHSA-j5m7-59rp-24f5) · [Worker](https://github.com/gradle/gradle/security/advisories/GHSA-mvvg-497x-hmj8) · [Daemon registry](https://github.com/gradle/gradle/security/advisories/GHSA-pcp8-98xm-c6pr)
- The OCR AAR contains libjpeg, libleptonica, libpngx and libtesseract shared objects for four ABIs. Upstream 4.9.0 documents Tesseract 5.5.1, Leptonica 1.85.0, libjpeg v9f and libpng 1.6.48. The latter falls in published affected ranges. OCR receives Android-decoded bitmaps. Native PNG decoder reachability and binary patch provenance remain **BLOCKED** pending inspection and a verified patched artifact or reproducible rebuild. [OCR release](https://github.com/adaptech-cz/Tesseract4Android/releases/tag/4.9.0) · [libpng advisories](https://github.com/pnggroup/libpng/security/advisories)
- JUnit 4.13.2 is newer than the 4.13.1 fix for the TemporaryFolder disclosure issue and is a test dependency. Kotlin compiler/build plugins follow JetBrains' latest-release security policy. The AndroidX, Kotlin and Actions checks establish source coordinates or commit identities and selected artifact bytes. A complete advisory assessment of resolved transitives and packaged native code is still open. [JUnit advisory](https://github.com/advisories/GHSA-269g-pwp5-87pp) · [Kotlin policy](https://github.com/JetBrains/kotlin/security)

## Offline storage and backup

- The source manifest requests CAMERA and omits INTERNET. The source has no telemetry or cloud OCR path. The design WebView blocks network loads and disables JavaScript, file and content access. The gallery uses a selected content URI. System document providers can source content independently of the app. Device behavior and the merged manifest are separate verification surfaces.
- Audio and timing stores use app-private `filesDir`, per-import staging and flat filename allowlists before opening output files. Directories, duplicates, path separators, unexpected files and excessive decompressed byte counts are rejected. Java ZIP streaming creates ordinary output files and does not restore archive Unix modes or symbolic links. Audio SHA-256, timing SHA-256, canonical verse digest and recording binding are validated before publication. These checks do not establish license, authenticity or reviewed timings. Large bounded imports can still consume disk and CPU. Device cancellation, low space, process death, power loss and OEM data transfer remain **BLOCKED**.
- Expanded the cloud and transfer exclusion lists to include the four device-protected domains. The policy checker now rejects INTERNET across permission declaration forms, re-enabled legacy backups, and missing or weakened extraction rules. Five regression tests run through the existing Phase 8 entry point. [Android Auto Backup documentation](https://developer.android.com/identity/data/autobackup)
- Legally sourced OCR photos, rights-cleared recordings, reviewed timings, scholarly sign-off and device QA remain **BLOCKED**. No packages were requested from Gradle.

Main advanced during the audit. The two intervening commits through `00b34866e28e688c08a5ef524f2bee9232c5a550` did not overlap the five changed files. The latest upstream OCR suite passed 15 tests locally. The full Phase 8 checks also passed after provisioning the Kotlin CLI. A subsequent Actions run is required to certify the privacy change itself.
