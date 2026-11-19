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
