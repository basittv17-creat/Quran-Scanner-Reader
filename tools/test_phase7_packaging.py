#!/usr/bin/env python3
"""Checks that local pack builder follows audio manifest naming/hash rules.
Synthetic files are NOT recitation samples and are not shipped in app or ZIP.
"""
import tempfile
import zipfile
from pathlib import Path
from build_audio_pack import build

with tempfile.TemporaryDirectory() as td:
    root=Path(td)
    source=root/'clips';source.mkdir()
    (source/'001_001.mp3').write_bytes(b'not real mp3: test fixture only')
    output=root/'pack.zip'
    assert build(source,output,'test fixture','synthetic tests','no distribution',False)==1
    with zipfile.ZipFile(output) as z:
        assert sorted(z.namelist())==['001_001.mp3','manifest.tsv']
        assert '001_001.mp3' in z.read('manifest.tsv').decode()
    invalid = source/'001_999.mp3';invalid.write_bytes(b'bad test input')
    try: build(source,root/'invalid.zip','test','synthetic','none',False)
    except AssertionError: pass
    else: raise AssertionError('invalid Quran verse number accepted')
print('PASS: pack builder and invalid-ayah rejection using temporary synthetic data')
