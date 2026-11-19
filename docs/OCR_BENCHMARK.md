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

The app's conservative matcher verifies one whole verse at a time. Its
internal normalization and exact-match score serve candidate selection. Export
raw OCR for strict CER/WER and record only references actually emitted by the
tested path. Keep user confirmation outcomes separate from engine predictions.
