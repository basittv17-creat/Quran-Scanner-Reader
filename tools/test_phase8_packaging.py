#!/usr/bin/env python3
"""Check rejection and hash binding with temporary, synthetic pack fixtures."""
import argparse
import hashlib
import json
import zipfile
from pathlib import Path
import tempfile
import sys
sys.path.insert(0, str(Path(__file__).resolve().parent))
from build_audio_pack import build as build_audio
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

    # Exercise both builders together. The clip bytes and word windows are
    # synthetic test inputs and exist only in this temporary directory.
    corpus = Path(__file__).resolve().parents[1] / 'app/src/main/assets/quran/verses.jsonl'
    first_verse = json.loads(corpus.read_bytes().split(b'\n', 1)[0])
    arabic = first_verse['arabic']
    assert (first_verse['surah'], first_verse['ayah']) == (1, 1)
    word_count = len(arabic.split())
    timing_bytes = ''.join(
        f'{index}\t{(index - 1) * 100}\t{index * 100}\n'
        for index in range(1, word_count + 1)
    ).encode('utf-8')
    (t / '001_001.tsv').write_bytes(timing_bytes)
    clips = d / 'clips'
    clips.mkdir()
    opts.corpus = corpus

    for clip_bytes in (b'synthetic fixture one', b'synthetic fixture two'):
        (clips / '001_001.mp3').write_bytes(clip_bytes)
        audio_pack = d / 'audio.zip'
        assert build_audio(clips, audio_pack, 'Test', 'Synthetic fixture', 'Test only', False) == 1
        with zipfile.ZipFile(audio_pack) as archive:
            m.write_bytes(archive.read('manifest.tsv'))
        build(opts)
        with zipfile.ZipFile(opts.output) as archive:
            assert sorted(archive.namelist()) == ['001_001.tsv', 'manifest.tsv']
            assert archive.read('001_001.tsv') == timing_bytes
            row = archive.read('manifest.tsv').decode().splitlines()[-1].split('\t')
            assert row == [
                '1', '1', '001_001.tsv',
                hashlib.sha256(timing_bytes).hexdigest(),
                hashlib.sha256(clip_bytes).hexdigest(),
                hashlib.sha256(arabic.encode('utf-8')).hexdigest(),
                str(word_count),
            ]

print('PASS: Python timing pack builder rejection and synthetic audio/text digest binding')
