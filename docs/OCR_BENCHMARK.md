# Offline OCR benchmark exports

Run `python3 tools/ocr_benchmark.py reviewed-samples.jsonl --require-provenance --output report.json`.
The tool evaluates exported observations; it does not run the Android engine.
Use legally usable real Pakistani 15-line / Indo-Pak and Madani photographs,
separate printing labels and blur/light/rotation conditions. Keep train and test
pages separate. Record device, app commit, OCR model SHA and preprocessing profile
with each dataset's provenance. Review text and references independently before scoring.

Each JSONL object must contain:

| Field | Meaning |
|---|---|
| sample_id | Unique stable image/scan ID |
| printing | Printing/condition group, e.g. madani-low-light |
| script | `indo-pak` or `madani` |
| publisher | Exact publisher or edition identifier |
| device | Device and Android version |
| app_commit | Full 40-character app source SHA |
| model_sha256 | SHA-256 of the exact model used |
| profile | Profile and preprocessing configuration |
| image_sha256 | Lowercase 64-character digest of the exact scanned image |
| rights | Provenance and permission reference |
| reviewer | Ground-truth review identifier |
| expected_text | Human-reviewed transcription of the visible region, including marks |
| raw_ocr | Unedited engine output, empty string allowed for failed scans |
| expected_refs | Ordered list of visible verse references, each `[surah, ayah]` |
| predicted_refs | Ordered predicted reference list; `[]` for abstention |
| latency_ms | Finite nonnegative measured engine latency |

CER is total Unicode code-point edits / expected code points; WER is total
whitespace-token edits / expected tokens. No Arabic normalization is applied:
missing diacritics count as errors. Insertions can produce rates above 1.
Verse accuracy requires the complete ordered reference list to match. An abstention
counts as a recognition miss and is reported separately from a wrong prediction.
False positive rate is wrong nonempty predictions divided by all samples.
Abstention rate is empty predictions divided by all samples. Rates are
micro-averaged. Compare devices, editions and models using the per-configuration
breakdown; per-script and per-printing results pool their respective observations.
P95 uses nearest rank. Exports in the previous format remain readable when
`--require-provenance` is omitted. An export cannot mix legacy and qualified rows.

The report binds the input export SHA. Metadata is supplied by the operator and
cannot prove review, licensing, image digest correctness or representative coverage.
The checked-in metric tests use synthetic strings only. No real-page benchmark
has been completed, and no Quran OCR accuracy claim is established.

## Capture guidance diagnostics

The live and gallery screens report elapsed OCR time in milliseconds, measured from
image quality sampling through Tesseract inference. This includes model preparation
and excludes image decoding, verse matching, and UI rendering. Record cold and
warm runs separately when comparing latency.

Guidance uses mean luma and the percentage of sampled horizontal and vertical
pixel pairs with a luma difference of at least 22 out of 255. The initial
thresholds are mean luma below 65 and strong edges below 0.6%. Low detail can
come from blur, a blank area, or low-contrast print. These are advisory signals
for trying another capture. They do not suppress OCR or establish Quran
recognition accuracy. Calibrate each signal with reviewed real pages and
representative devices before relying on it in accuracy comparisons.

## Real-page collection and evaluation protocol

1. Select a named Hafs edition for each script. Photograph the edition's title
   and imprint pages and record publisher, edition, year or print run, volume
   and printed page number. Assign a stable edition/page ID. Obtain written
   permission or an applicable published license for the exact edition artwork
   and each photographer's images. Confirm the intended benchmark use and
   redistribution of originals, crops and processed derivatives, plus any
   attribution or share-alike terms.
2. Maintain a versioned manifest outside the app. For each capture record its
   page ID, original filename and SHA-256, source URL or photographer and
   acquisition date, license evidence and attribution, and exact OCR input
   SHA-256. Record crop coordinates, orientation, transformation order and
   tool versions to reproduce derived inputs. Recompute both hashes from
   the stored bytes before scoring and link the manifest by sample ID.
3. Freeze page-level splits before tuning. Keep all captures, crops, rotations,
   devices and repeat observations of the same page together. Check for
   duplicate artwork across editions and group reprints and matching content
   when testing generalization to unseen passages. Record the split algorithm,
   seed, page inventory and manifest SHA-256. Check split membership and
   cross-split exact and visually near-duplicate images. Disclose any unknown
   upstream model training overlap. Use development pages for tuning and
   reserve the test pages for the fixed configurations.
4. For each edition, preselect pages with varied marks and verse boundaries.
   Include opening and closing pages, interior pages, and regions containing
   complete, partial and multiple ayahs. Use at least two representative
   Android devices when device testing is authorized. Capture a baseline
   (diffuse light, steady, upright) and paired variations of light (dim and
   glare), blur (defocus and motion), and rotation (e.g. ±15° roll and 90°
   orientation). Vary one factor at a time first and predeclare any combined
   conditions, target sample counts and exclusion criteria. Record actual
   light measurements or setup, blur method, angle, device and OS, camera or
   gallery path, distance, resolution and capture sequence. Retain failures.
5. A fluent reviewer transcribes the visible text and marks with a frozen
   policy for reading order, line breaks, verse ornaments, marginal text and
   clipped glyphs. An independent qualified Quran reviewer checks the image,
   transcription and ordered surah/ayah numbers against the identified edition
   and authoritative references. Record both reviewer IDs, review dates and
   adjudication for disagreements. Resolve illegibility before viewing OCR
   output and log exclusions. Preserve the signed-off reference export and
   its hash. Keep these labels separate from the bundled Tanzil corpus.
6. Freeze the Android app commit, OCR engine version, model release and
   exact model hash, profile, preprocessing and matcher settings. Run the
   same eligible captures across model variants paired by capture ID.
   Export an independent scan ID for each observation, untouched raw OCR,
   emitted reference predictions (empty on abstention), and measured latency.
   Use stable printing/condition labels and distinguish cold and warm runs
   in provenance. Log infrastructure failures and report their counts.
7. Score offline with the command above. Archive the test manifest, export
   and report hashes, evaluator commit and Python version. Report denominators
   by edition, condition, device and model with CER/WER, exact ordered ref
   accuracy, incorrect predictions, abstentions and latency. Report how
   partial and multiverse images affect the exact-list metric. Repeated
   captures are correlated. Use page-level resampling if reporting confidence
   intervals.

ConservativeVerseMatcher verifies one whole verse at a time. The current
camera and gallery activities use VerseCandidates.suggest and present
candidate verses for manual review. For each evaluation, name the tested
path and freeze how it converts results into predictions. For ranked
suggestions, document the selection and abstention rule. A ranked list of
alternatives cannot be interpreted as an ordered list of visible ayahs.
Use raw OCR for strict CER/WER and record user confirmation separately.

## Versioned dataset intake

Use the existing evaluator with the frozen capture manifest and the exact local
input bytes:

```sh
python3 tools/ocr_benchmark.py observations.jsonl --manifest manifest.json \
  --images verified-images --output report.json
```

The manifest is one JSON object with `schema: "QSR-OCR-DATASET-1"`, a
`dataset_version`, a `split_protocol` (algorithm, seed and inventory reference),
a `label_policy` (approved transcription and review rules), and a `captures` array.
Freeze and archive the manifest before exporting predictions. Increment the
dataset version for any corrected label, changed input or split change.
Keep evidence and images in access-controlled offline storage where required
by their terms. Admission requires checking the underlying permissions and
review records.

| Capture fields | Required evidence |
|---|---|
| `capture_id`, `page_id`, `content_group`, `split` | Stable capture, imprint/page and passage or artwork-group identifiers. Use `train` for development and `test` for held-out evaluation. Give crops and other variants the parent page ID. Group overlapping content and reprints that could leak across the split. |
| `script`, `publisher`, `printing` | `indo-pak` or `madani`, named publisher and precise imprint/edition/year or print-run identifier. Keep condition out of the printing label for this schema. |
| `source_url`, `rights_evidence_url`, `rights_checked_on` | HTTPS edition/capture and permission evidence URLs, including private authenticated evidence URLs where appropriate, and the review date as YYYY-MM-DD. Preserve dated copies of the terms and written grants offline. |
| `edition_rights`, `photograph_rights`, `benchmark_approved`, `redistribution` | Record rights holders, applicable clauses or grant IDs, approved internal uses and attribution. Set approval to JSON `true` only after clearance. Record redistribution as `approved` or `restricted`. A restricted dataset stays in the approved environment. |
| `original_sha256`, `image_sha256` | Lowercase SHA-256 of original capture and exact OCR input. Store each under its digest filename in the local image directory. |
| `condition`, `capture_device`, `capture_setup` | One of `baseline`, `low-light`, `blur`, `skew`, `rotation`. Record device/OS, acquisition date, lens/distance, dimensions, target and measured lighting, blur method, perspective tilt or roll/orientation, and ordered processing parameters with tool versions. Label each paired variation by its primary condition. Record combined factors in the setup. |
| `expected_text`, `expected_refs`, `reviewer`, `independent_reviewer`, `review_evidence` | Exact reviewed transcription and nonempty ordered reference pairs. Record distinct human reviewer IDs and reference their dated sign-off and adjudication. Give reviewers access to the exact OCR input. Keep labels blind to predictions. |

Use one JSONL observation per exact input and configuration. Supply
`sample_id`, `capture_id`, `image_sha256`, `device`, `app_commit`,
`model_sha256`, `profile`, `matcher`, `decision_rule`, `raw_ocr`,
`predicted_refs`, `latency_ms` and `candidate_confidence`.
Record the engine version, input path and cold/warm setup in the profile.
Use the top decision score on a 0–1 scale, or explicit `null` when
unavailable and the matcher abstained. Treat a matcher's score as a
diagnostic and record any deterministic score conversion in the rule.
Freeze tie handling, threshold and failure handling on train before
measuring held-out test observations. Include blank OCR and abstentions.
The manifest supplies all expected transcription, references and rights fields.

The evaluator hashes the original and OCR input bytes, checks the entire
inventory for exact cross-split hash and canonical page/content-group overlap,
and scores only referenced test captures. Check visually similar images and
page aliases during intake. Collect a paired test observation for every
planned configuration that supports the capture. Archive the run plan and
explain coverage gaps against the eligible and observed counts. Compare
performance by printing, condition, device, model and decision rule. The
confidence table counts correct, wrong and abstained decisions in score
deciles (9 includes 1.0), plus an unavailable bucket. These counts do not
establish calibrated probabilities. Bind reruns to the manifest/export SHA
values, evaluator commit and Python version.
