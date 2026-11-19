#!/usr/bin/env python3
"""Regression tests for the independent source integrity gate."""
import json
from pathlib import Path
import re
import shutil
from tempfile import TemporaryDirectory
import unittest

from verify_corpus_provenance import ASSETS, verify_assets, verify_verses


class CorpusProvenanceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = (ASSETS / 'tanzil-uthmani-v1.1.txt').read_bytes()
        cls.corpus = (ASSETS / 'verses.jsonl').read_bytes()
        rows = re.findall(rb'^(\d+)\|', cls.source, re.MULTILINE)
        cls.counts = [rows.count(str(s).encode()) for s in range(1, 115)]

    def test_canonical_assets_pass(self):
        verify_assets()

    def test_offsetting_count_changes_fail(self):
        counts = self.counts.copy()
        counts[0] -= 1
        counts[1] += 1
        with self.assertRaisesRegex(ValueError, 'Publisher reference mismatch at 2:1'):
            verify_verses(self.source, self.corpus, counts)

    def test_swapped_ids_fail(self):
        rows = self.corpus.splitlines()
        rows[0], rows[1] = rows[1], rows[0]
        with self.assertRaisesRegex(ValueError, 'JSONL reference mismatch at 1:1'):
            verify_verses(self.source, b'\n'.join(rows), self.counts)

    def test_missing_diacritic_fails_byte_comparison(self):
        rows = self.corpus.splitlines()
        entry = json.loads(rows[0])
        entry['arabic'] = entry['arabic'].replace('\u0650', '', 1)
        rows[0] = json.dumps(entry).encode('utf-8')
        with self.assertRaisesRegex(ValueError, 'Arabic UTF-8 bytes differ at 1:1'):
            verify_verses(self.source, b'\n'.join(rows), self.counts)

    def test_jsonl_formatting_change_fails_pin(self):
        with TemporaryDirectory() as directory:
            folder = Path(directory)
            shutil.copyfile(ASSETS / 'tanzil-uthmani-v1.1.txt', folder / 'tanzil-uthmani-v1.1.txt')
            (folder / 'verses.jsonl').write_bytes(self.corpus + b'\n')
            with self.assertRaisesRegex(ValueError, 'Canonical JSONL bytes changed'):
                verify_assets(folder)

    def test_missing_attribution_link_fails(self):
        with TemporaryDirectory() as directory:
            folder = Path(directory) / 'quran'
            shutil.copytree(ASSETS, folder)
            notice = folder / 'SOURCE_AND_LICENSE.txt'
            notice.write_bytes(notice.read_bytes().replace(b'Source: https://tanzil.net/\n', b''))
            with self.assertRaisesRegex(ValueError, 'Publisher attribution'):
                verify_assets(folder)


if __name__ == '__main__':
    unittest.main()
