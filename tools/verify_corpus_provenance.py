#!/usr/bin/env python3
"""Check the pinned Tanzil snapshot and Android assets independently of the importer."""
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / 'app/src/main/assets/quran'
CORE = ROOT / 'app/src/main/java/org/quranscan/reader/core'
SOURCE_SHA256 = '18c719bb3ba26d32ef457f40dad77cd28c4c5a34156833e26a8e5fcfdd246fb1'
CORPUS_SHA256 = 'fa338471ce7cf0f08e57244458e143ceab555d1905d55e012d49da37cf541270'
NOTICE_MARKER = b'# PLEASE DO NOT REMOVE'


def require(condition, message):
    if not condition:
        raise ValueError(message)


def verify_verses(source, corpus, counts):
    rows, separator, _ = source.partition(NOTICE_MARKER)
    require(separator, 'Publisher copyright notice missing')
    expected = [(str(s).encode(), str(a).encode())
                for s, count in enumerate(counts, 1) for a in range(1, count + 1)]
    source_rows = rows.rstrip(b'\r\n').splitlines()
    corpus_rows = corpus.splitlines()
    require(len(counts) == 114 and all(count > 0 for count in counts)
            and len(expected) == len(source_rows) == len(corpus_rows) == 6236,
            'Unexpected verse or surah count')
    for (surah, ayah), original, derived in zip(expected, source_rows, corpus_rows):
        reference = f'{surah.decode()}:{ayah.decode()}'
        original_fields = original.split(b'|', 2)
        require(len(original_fields) == 3 and tuple(original_fields[:2]) == (surah, ayah),
                f'Publisher reference mismatch at {reference}')
        entry = json.loads(derived.decode('utf-8'))
        require(isinstance(entry, dict) and set(entry) == {'surah', 'ayah', 'arabic'}
                and type(entry['surah']) is int and type(entry['ayah']) is int
                and (entry['surah'], entry['ayah']) == (int(surah), int(ayah)),
                f'JSONL reference mismatch at {reference}')
        require(isinstance(entry['arabic'], str)
                and entry['arabic'].encode('utf-8') == original_fields[2],
                f'Arabic UTF-8 bytes differ at {reference}')


def verify_assets(folder=ASSETS, core=CORE):
    source = (folder / 'tanzil-uthmani-v1.1.txt').read_bytes()
    corpus = (folder / 'verses.jsonl').read_bytes()
    require(hashlib.sha256(source).hexdigest() == SOURCE_SHA256, 'Publisher snapshot changed')
    require(hashlib.sha256(corpus).hexdigest() == CORPUS_SHA256, 'Canonical JSONL bytes changed')
    require((folder / 'verses.sha256').read_text(encoding='ascii').strip() == CORPUS_SHA256,
            'Bundled hash sidecar mismatch')

    loader = (core / 'OfflineCorpus.kt').read_text(encoding='utf-8')
    require(re.search(r'\bconst val CORPUS_SHA256\s*=\s*"' + CORPUS_SHA256 + '"', loader),
            'Android corpus digest pin mismatch')
    count_source = (core / 'QuranVerseCounts.kt').read_text(encoding='utf-8')
    table = re.search(r'\bval counts:\s*IntArray\s*=\s*intArrayOf\(([\d,\s]+)\)', count_source)
    require(table and re.search(r'\bconst val totalVerses\s*=\s*6236\b', count_source),
            'Android verse count declaration missing or changed')
    counts = [int(value) for value in table[1].strip().rstrip(',').split(',')]
    verify_verses(source, corpus, counts)

    _, separator, notice = source.partition(NOTICE_MARKER)
    attribution = (folder / 'SOURCE_AND_LICENSE.txt').read_bytes()
    require(separator + notice in attribution, 'Publisher copyright notice changed or missing')
    require(all(line in attribution.splitlines() for line in (
        b'Source: https://tanzil.net/',
        b'License: https://tanzil.net/docs/Text_License',
        b'Source SHA-256: ' + SOURCE_SHA256.encode('ascii'),
    )), 'Publisher attribution, license link or recorded digest missing')
    report = json.loads((folder / 'IMPORT_REPORT.json').read_text(encoding='utf-8'))
    require(report['source_sha256'] == SOURCE_SHA256
            and report['generated_sha256'] == CORPUS_SHA256
            and report['verses'] == 6236, 'Import report disagrees with pinned corpus')


if __name__ == '__main__':
    verify_assets()
    print('PASS: 6236 ordered references match pinned Tanzil UTF-8 bytes, including diacritics; notice retained')
    print('LIMIT: local digest pins establish byte identity. Independent scholarly and rendering review remain pending.')
