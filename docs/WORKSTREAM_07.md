# Workstream 07 native localization and accessibility

## Source audit

Main advanced from the supplied audit reference `d0f730d` to `74ce21d33895d6239d5c33d2bf2c9855343fd12e` during review. Reviewed the progress and Urdu status reports, development handoff, next steps, external asset and QA gates, OCR benchmark notes, and source-validation workflow, along with native activities, the gallery crop view, resources, manifest and JVM test inventory.

- `8f16855` already localized Settings, Help and About and restored those destinations and the design preview. Gallery URI and crop bounds already survive recreation. The gallery OCR results screen currently reopens the crop on recreation.
- `e8835dc` added a full camera image option and saves it with the crop state. `74ce21d` localized several camera and recitation controls and let button labels wrap. Preserve those changes and the manifest's RTL support.
- MainActivity still has English gallery prompts, OCR status, candidate references and errors. The gallery preview uses remaining vertical space between its instructions and buttons. Its full-image action and drawer need short-viewport and large-font checks.
- LiveScanActivity still has English camera status, OCR errors, candidate labels and result summaries. Candidate scrolling and the full-image fallback need TalkBack and narrow-screen review. Check status announcements for repeated automatic scans and keep camera crop coordinates independent of UI direction.
- RecitationActivity still has English pack and playback feedback, clip counts and word selection messages. Resource plurals and positional reference formats are needed in those call sites. Check input labels, focus order and line wrapping. Existing recreation restores verse/word selection and leaves playback stopped and confirmation unchecked.
- The logs for [run 44514943361](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44514943361) identify real SetTextI18n warnings in recitation. That historical run reported 28 JVM tests, seven benchmark tests and lint 0 errors / 43 warnings. The current changes leave lint configuration alone.

## Gallery crop checkpoint

Localized the crop preview's existing accessibility description for English and Urdu. It points to the full-image action as a gesture-free option. The remaining activity changes require coordination with Work 04, Work 03 and Work 06 respectively.

Local checks passed for all 97 English/Urdu string names and positional placeholders, seven Python OCR benchmark metric tests, the source policy, and verbatim corpus provenance for all 6,236 ayat with both pinned hashes retained. Android tasks use the existing source-validation workflow. No APK/AAB generation is authorized.

TalkBack, physical device visual checks, RTL traversal, large font, narrow screens and rotation behavior are **PENDING**. No real-image OCR, scholarly or listening review was performed.

## Verified checkpoint and concurrent changes

Commit `dc06fc14c6fa5ef30763ca389290b91b15163b7a` passed [Actions run 44529237079](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44529237079). Logs confirm Kotlin compilation, resource processing, 29 JVM tests with zero failures, errors or skips, 15 Python OCR metric tests, lint 0 errors / 40 warnings, and no APK/AAB. The run logged SetTextI18n warnings and no missing or extra translation warnings. Five local source-policy tests also passed.

Subsequent commit `5d352d4` localized gallery prompts and camera feedback, candidate labels and some recitation feedback. [44529374633](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44529374633) and [44529532565](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44529532565) failed before Android compilation because the Phase 6 validator expected the old English literal. `d858ead` updated the validator to check the resource call and both locales. Pack-management plurals, remaining activity status strings, screen-reader announcements and short-viewport layouts are still follow-up items. Device and TalkBack QA remain **PENDING**.

[Source ZIP](https://drive.google.com/file/d/1Nvnpz-5exvS4KTVLROU7KDYoOibOpeOY/view) contains the validated `dc06fc1` checkpoint. SHA-256 `f5acef3e54c520fddfd7e1b39cab469890899bdfa8d3c8097e034095dca70a37`. Verified ZIP integrity, both Quran corpus digests and absence of APK/AAB entries. Drive confirmed the file in the existing backup folder and its 1,514,322-byte size.
