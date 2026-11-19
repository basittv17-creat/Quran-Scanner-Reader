#!/usr/bin/env python3
"""Verify pinned publisher bytes and exact per-verse round trip; no OCR claims."""
from pathlib import Path
import hashlib
import json
from import_tanzil import parse
from validate_corpus import validate

root = Path(__file__).resolve().parent.parent
folder = root / 'app/src/main/assets/quran'
source = (folder / 'tanzil-uthmani-v1.1.txt').read_bytes()
assert hashlib.sha256(source).hexdigest() == '18c719bb3ba26d32ef457f40dad77cd28c4c5a34156833e26a8e5fcfdd246fb1', 'Publisher snapshot changed'
expected = parse(source.decode('utf-8'))
actual = [json.loads(line) for line in (folder / 'verses.jsonl').read_text().splitlines()]
assert actual == expected, 'Canonical text differs from publisher snapshot'
assert validate(folder / 'verses.jsonl')
notice = source.decode('utf-8').split('# PLEASE DO NOT REMOVE', 1)[1]
assert notice in (folder / 'SOURCE_AND_LICENSE.txt').read_text(), 'Publisher copyright notice missing'
report = json.loads((folder / 'IMPORT_REPORT.json').read_text())
assert report['source_sha256'] == hashlib.sha256(source).hexdigest()
assert report['generated_sha256'] == hashlib.sha256((folder / 'verses.jsonl').read_bytes()).hexdigest()
print('PASS: 6236 entries match pinned Tanzil snapshot verbatim, including diacritics; notice retained')
print('LIMIT: digest locally recorded from two identical HTTPS downloads; no publisher-signed digest or independent scholarly review')
