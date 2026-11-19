# Offline OCR benchmark exports

Run `python3 tools/ocr_benchmark.py reviewed-samples.jsonl --output report.json`.
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
Rates are micro-averaged within each printing group; p95 uses nearest rank.

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
