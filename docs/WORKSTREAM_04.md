# Workstream 04 gallery image handling and restoration

Audited the project reports, handoff, next steps, device QA gates, OCR benchmark and source-validation workflow. The starting main was `dc06fc14c6fa5ef30763ca389290b91b15163b7a`. Integrated the gallery changes on `9cb237f4f83af3640e4a569f83e05012ae7312ff`, preserving Work 07's localized gallery labels and accessibility guidance, and the intervening scan guidance and benchmark changes. Camera geometry and OCR image quality logic are unchanged.

- The document picker retains its selected content URI's read grant when the provider offers one. A temporary grant still works for immediate use. Replacing or leaving the gallery releases its retained grant. An inaccessible or undecodable selection returns to Home with a message.
- Gallery decode checks header and actual bitmap dimensions, with power-of-two sampling for a 2048-pixel edge limit. The existing eight EXIF orientations and fit-center crop remain. Failed transforms and discarded images release their bitmaps. Crop allocation runs on the gallery worker.
- Gallery work is serialized across activity instances. Back, navigation and stopping the activity invalidate pending work. An already-running provider call or native OCR can finish, but its old result cannot update the UI. The gallery restores the preview and valid selection after recreation. Returning from the background during in-flight work also reopens the preview. OCR and audio start only from explicit user actions.
- Back closes an open drawer first. From gallery scan or results it returns to the image. From image opening or crop it returns to Home. Scrollable results and the full-image option remain.
- Added three pure JVM cases covering invalid saved coordinates, crop validation and fallback, and sampled/decoded size limits.

Local checks passed for the source policy, all 15 OCR benchmark metric tests, five source-policy unit tests and all 6,236 exact Tanzil Uthmani v1.1 verses, including diacritics and the original notice. Both pinned hashes were verified. The full local phase-eight script stopped because the host lacks `kotlinc`. The real-page OCR benchmark remains blocked on reviewed, rights-cleared image exports.

| Corpus asset | SHA-256 |
|---|---|
| Original Tanzil source | `18c719bb3ba26d32ef457f40dad77cd28c4c5a34156833e26a8e5fcfdd246fb1` |
| Application JSONL | `fa338471ce7cf0f08e57244458e143ceab555d1905d55e012d49da37cf541270` |

**PENDING on a physical Android device**: all EXIF rotations/reflections, large fonts and narrow screens, TalkBack traversal and full-image action, low-memory and large or corrupt images, revoked provider grants and rotation/recreation during opening or OCR. No APK/AAB package tasks were run locally. Prior backups remain documented in the project progress report.
