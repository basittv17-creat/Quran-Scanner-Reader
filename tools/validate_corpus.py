#!/usr/bin/env python3
"""Strict structural validation; not a scholarly review or proof of textual authenticity."""
import hashlib
import json
import pathlib
import sys
from import_tanzil import COUNTS, ARABIC

def validate(path):
    path = pathlib.Path(path)
    raw = path.read_bytes()
    seen = set()
    count = 0
    for line in raw.decode('utf-8').splitlines():
        if not line.strip():
            continue
        entry = json.loads(line)
        assert set(entry) == {'surah','ayah','arabic'}, 'Unexpected keys'
        s,a,t = entry['surah'],entry['ayah'],entry['arabic']
        assert type(s) is int and type(a) is int and 1 <= s <= 114 and 1 <= a <= COUNTS[s-1]
        assert isinstance(t,str) and t and ARABIC.search(t) and t == t.strip()
        assert (s,a) not in seen, f'Duplicate {s}:{a}'
        seen.add((s,a))
        count += 1
    complete = count == 6236 and all((s,a) in seen for s,k in enumerate(COUNTS,1) for a in range(1,k+1))
    digest = hashlib.sha256(raw).hexdigest()
    hashfile = path.with_suffix('.sha256')
    if hashfile.exists():
        assert hashfile.read_text().strip().lower() == digest, 'SHA-256 mismatch'
    print(f'Verses: {count} | Complete structure: {complete} | SHA256: {digest}')
    if not complete:
        print('NOT PRODUCTION READY: incomplete corpus')
    return complete

if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.exit('Usage: python3 tools/validate_corpus.py FILE')
    sys.exit(0 if validate(sys.argv[1]) else 1)
