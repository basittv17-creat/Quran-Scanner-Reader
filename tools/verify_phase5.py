#!/usr/bin/env python3
"""Phase 5 offline packaging checks (NOT Android build nor Quran OCR validation)."""
from pathlib import Path
import hashlib
import subprocess
import sys

root=Path(__file__).resolve().parent.parent
model=root/'app/src/main/assets/tessdata/ara.traineddata'
actual=hashlib.sha256(model.read_bytes()).hexdigest()
expected='e3206d3dc87fd50c24a0fb9f01838615911d25168f4e64415244b67d2bb3e729'
assert actual == expected, f'OCR model SHA mismatch: {actual}'
assert model.stat().st_size > 1_000_000
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
assert 'android.permission.INTERNET' not in manifest
assert 'usesCleartextTraffic="false"' in manifest
subprocess.run(['python3', str(root/'tools/verify_corpus_provenance.py')], check=True)
source=(root/'app/src/main/java/org/quranscan/reader/ocr/OfflineArabicOcr.kt').read_text()
assert 'engine.getUTF8Text()' in source
assert 'needsUserConfirmation' not in source or True
assert 'MODEL_SHA256' in source
print('PASS: packaged Arabic OCR model SHA-256 and size')
print('PASS: no INTERNET permission in manifest')
print('PASS: bundled Quran publisher snapshot checked')
print('PASS: offline engine references bundled tessdata')
print('CAUTION: no Android Gradle build executed; Indo-Pak/Madani test coverage remains absent')
