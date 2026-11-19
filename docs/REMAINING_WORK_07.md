# Remaining Work 07 — Pakistani and Madani OCR benchmark readiness

19 November 2026 · `main` · Android package `org.quranscan.reader`

**Acquisition, real-image accuracy and confidence calibration are BLOCKED.**
Intake accepted zero real images and zero independently reviewed labels. No real OCR score was run.

## Publisher evidence and authorization required

Primary sources checked on 19 November 2026. The publisher listings identify candidate editions. Record the specific year or print run and imprint from the copy before intake.

| Candidate | Verified publisher evidence and scope | Gate |
|---|---|---|
| Taj Company Ltd, 15 Line Hafizi, item 126 | Publisher identifies an Arabic-only 15-line edition. Store footer reserves all rights. Store terms give no grant covering capture, OCR benchmark use or redistribution of the interior pages or listing photography. [Edition](https://www.tajquran.com/products/holy-quran-15-line-hafizi-126) · [Publisher](https://tajquran.com/) · [Terms](https://tajquran.com/policies/terms-of-service) | **BLOCKED.** Obtain edition/layout authorization from Taj or the confirmed rights holder, plus a grant for each photograph from its photographer or authorized owner. |
| Darussalam, 15 Lines Quran Pak (Jumbo), SKU 95978759 | Publisher identifies 15 lines, Indo-Pak/Persian script and two-color printing. The product listing gives no benchmark or image redistribution permission. [Edition and publisher](https://darussalam.pk/products/15-lines-quran-pak-jumbo) | **BLOCKED.** Obtain the equivalent rights for the named imprint and exact photographs or commission new captures with a signed photographer grant. |
| King Fahd Glorious Quran Printing Complex, Madinah Mushaf (Hafs), official print-project vectors | The digital-use notice, retrieved from the publisher's indexed Arabic page, describes free use of its designated Illustrator release in personal, institutional, publishing, web and software contexts inside and outside Saudi Arabia, with restrictions stated for commercial physical Mushaf printing/import in Saudi Arabia. The rights URL redirected during direct retrieval. The current product page identifies the Hafs AI package, SHA-256 `c2fa8d93a7c4f689b58eb32756e1ae56cd1b7ce4826f63e09723be8b092224fb`. The older project page lists another Hafs package hash. The general site license reserves the Complex's images except specifically released material. [Release-specific rights](https://dm.qurancomplex.gov.sa/rights/) · [Current release](https://qurancomplex.gov.sa/mushaf-printing) · [Older project](https://dm.qurancomplex.gov.sa/) · [General license](https://policy.qurancomplex.gov.sa/?Lan=en) | **BLOCKED for camera pages.** Confirm the exact physical edition/layout and permission to photograph and benchmark it with the Complex, plus photographer rights. A separate publisher-vector evaluation would need a grant traceable to the selected archive covering the intended rasterization, transformations and distribution. Camera evaluation needs actual captures. |

For each physical printing request a dated written grant identifying imprint,
edition/print run, covered pages, grantor and chain of authority. Confirm permission
to make and retain photographs, create crops and transformed inputs, run offline OCR
and share with the designated reviewers. For any proposed public release, also
confirm redistribution of original and derived files in datasets, repository and
backups with downstream download, territory, duration, credits and conditions.
Confirm label rights where necessary. Keep third-party scans with unknown rights out
of intake. Preserve the full URLs and dated publisher notices or grants with the
intake manifest. No publisher or photographer was contacted in this session.

## Source audit and validation

Inspected the project progress and Urdu status, 19 November handoff, Next Steps,
External Assets and QA, OCR Benchmark, source-validation workflow and related
workstream reports. Reviewed the evaluator, existing tests, the printed Arabic smoke
fixture, conservative matcher, recognition contract and candidate comparison.
The smoke fixture is for OCR integration checks. The strict benchmark counts Arabic
marks as supplied, independently of the app's normalized matcher. The matcher
requires a unique whole-verse match against the complete corpus and returns a
candidate requiring confirmation. Camera/gallery suggestions are reviewed manually.

The session started at `c4b4ec3764beb34808c31bd76a4434b19922ebd6`.
The actual [Actions job](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44535430865/job/135599437704)
logged 42 JVM tests, zero failures/errors/skips, 15 OCR metric tests, lint 0 errors
and 31 warnings, and no APK/AAB. Main advanced to `eb067178a9cf5bf7f6c24d43f48c6314f99ac0fd`
through independent corpus and timing work. Those changes were fast-forwarded
before validating this work.

- `tools/ocr_benchmark.py` adds optional versioned manifest intake and checked image-byte binding, test-only evaluation, duplicate and split leakage checks, per-condition metrics, coverage, confidence outcome counts and deterministic hash-bound reports. Legacy exports remain readable.
- `tools/test_ocr_benchmark.py` adds six deterministic intake tests covering strict marks and confidence, split leakage, rights/review/reference validation, duplicate and broken image binding, prediction validation and byte-identical CLI reruns. The existing malformed JSONL test also checks duplicate keys. Fixture bytes and miniature strings exercise the validator only.
- `docs/OCR_BENCHMARK.md` documents capture conditions, original and derived bytes, rights evidence, independent human labels, held-out splits, decision rules and report interpretation.
- Local Python discovery passed 38 tests including 21 OCR tests. Source policy and the pinned Tanzil provenance check passed. The local Phase 8 wrapper requires `kotlinc`, which is unavailable here. The hosted workflow runs the four allowed Gradle tasks.

Tanzil Uthmani v1.1 retains all 6,236 verses, exact marks, notice and attribution.
Publisher snapshot SHA-256 is
`18c719bb3ba26d32ef457f40dad77cd28c4c5a34156833e26a8e5fcfdd246fb1`.
Bundled JSONL SHA-256 is
`fa338471ce7cf0f08e57244458e143ceab555d1905d55e012d49da37cf541270`.
App, manifest, model, corpus and historical backups were preserved. No APK/AAB was
generated.

## Human review still required

Clear the selected prints and actual captures with rights holders. Assign a
qualified transcriber and an independent reviewer to sign off on the marks,
visible regions and ordered references against the identified edition. Freeze
the train/test inventory before tuning and review the held-out export for
coverage gaps. Run the offline Android path on authorized devices once approved
inputs and an authorized build are available.
