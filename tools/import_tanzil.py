#!/usr/bin/env python3
"""Import a verbatim Tanzil pipe-delimited 6236 verse corpus for offline Android use.

Example:
 python3 tools/import_tanzil.py --input ~/Downloads/quran-uthmani.txt \
   --output app/src/main/assets/quran/verses.jsonl

No network operations. Rejects incomplete/malformed sources. Never edits Quran text.
"""
import argparse
import hashlib
import json
import pathlib
import re

# Hafs verse counts, surahs 1 through 114; total 6236.
COUNTS = (7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,
          98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,
          75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,
          24,13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,
          40,46,42,29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,
          5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6)
assert len(COUNTS) == 114 and sum(COUNTS) == 6236
ARABIC = re.compile(r'[\u0621-\u064a]')


def parse(text):
    results = []
    for n, line in enumerate(text.splitlines(), 1):
        if not line.strip() or line.startswith('#'):
            continue
        # Exact pipe segmentation preserves the Arabic string; do not trim or normalize it.
        parts = line.split('|', 2)
        if len(parts) != 3:
            raise ValueError(f'Line {n}: expected surah|ayah|Arabic')
        s, a, arabic = parts
        if not s.isascii() or not a.isascii() or not s.isdecimal() or not a.isdecimal():
            raise ValueError(f'Line {n}: invalid numeric reference')
        sid, aid = int(s), int(a)
        if not 1 <= sid <= 114 or not 1 <= aid <= COUNTS[sid-1]:
            raise ValueError(f'Line {n}: reference out of range: {sid}:{aid}')
        if not arabic or not ARABIC.search(arabic) or arabic != arabic.strip():
            raise ValueError(f'Line {n}: invalid/whitespace-modified Arabic verse')
        if '\ufeff' in arabic or '\ufffd' in arabic:
            raise ValueError(f'Line {n}: invalid Unicode characters')
        expected_index = len(results)
        es = next(i+1 for i, total in enumerate(COUNTS) if expected_index < sum(COUNTS[:i+1]))
        ea = expected_index - sum(COUNTS[:es-1]) + 1
        if (sid, aid) != (es, ea):
            raise ValueError(f'Line {n}: expected {es}:{ea}, got {sid}:{aid}')
        results.append({'surah': sid, 'ayah': aid, 'arabic': arabic})
    if len(results) != 6236:
        raise ValueError(f'Expected 6236 verses, received {len(results)}')
    return results


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--input', required=True, type=pathlib.Path)
    p.add_argument('--output', required=True, type=pathlib.Path)
    args = p.parse_args()
    source = args.input.read_bytes()
    # retain original source for independent checksums/attribution; no normalization
    verses = parse(source.decode('utf-8-sig'))
    blob = ''.join(json.dumps(v, ensure_ascii=False, separators=(',', ':'))+'\n' for v in verses).encode('utf-8')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(blob)
    args.output.with_suffix('.sha256').write_text(hashlib.sha256(blob).hexdigest()+'\n', encoding='ascii')
    report = {'status':'STRUCTURALLY_VALID_ONLY_NOT_SCHOLAR_REVIEWED', 'source_sha256':hashlib.sha256(source).hexdigest(),
              'generated_sha256':hashlib.sha256(blob).hexdigest(), 'verses':len(verses),
              'source':'Tanzil Uthmani v1.1 — verify original source and preserve license'}
    (args.output.parent/'IMPORT_REPORT.json').write_text(json.dumps(report,indent=2)+'\n', encoding='utf-8')
    print(json.dumps(report, indent=2))

if __name__ == '__main__':
    main()
