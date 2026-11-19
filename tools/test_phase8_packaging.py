#!/usr/bin/env python3
"""Check that the pack builder rejects incomplete Quran and invalid timing rows."""
import argparse
from pathlib import Path
import tempfile
import sys
sys.path.insert(0, str(Path(__file__).resolve().parent))
from build_word_timing_pack import build, canonical, check_tsv, audio_entries

with tempfile.TemporaryDirectory() as temp:
    d = Path(temp)
    partial = d / 'verses.jsonl'
    partial.write_text('{"surah":1,"ayah":1,"arabic":"نص اختباري"}\n', encoding='utf-8')
    try:
        canonical(partial)
        raise AssertionError('Incomplete Quran text must be rejected')
    except ValueError as e:
        assert '6236' in str(e)
    check_tsv(b'1\t0\t200\n2\t201\t350\n', 2)
    for invalid in (b'1\t0\t200\n2\t199\t350\n', b'1\t0\t0\n2\t201\t350\n', b'1\t0\t200\n'):
        try:
            check_tsv(invalid, 2)
            raise AssertionError('Invalid timing was accepted')
        except ValueError:
            pass
    m = d / 'audio_manifest.tsv'
    m.write_text('# QSR-AUDIO-1\n# reciter=Test\n# source=Test\n# rights=Test\n1\t1\t001_001.mp3\t' + 'aa'*32 + '\n')
    reciter, rows = audio_entries(m)
    assert reciter == 'Test' and rows[(1,1)] == 'aa'*32
    t = d / 'timings'; t.mkdir()
    (t / '001_001.tsv').write_text('1\t0\t200\n2\t200\t350\n')
    opts = argparse.Namespace(timings_dir=t, audio_manifest=m, corpus=partial,
                              reciter='Test', source='Test', rights='Test', output=d / 'out.zip')
    try:
        build(opts)
        raise AssertionError('Builder accepted incomplete Quran')
    except ValueError:
        pass
print('PASS: Python timing pack builder safety and incomplete Quran rejection')
