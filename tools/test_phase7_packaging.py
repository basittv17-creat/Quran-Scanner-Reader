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
    except ValueError: pass
    else: raise AssertionError('invalid Quran verse number accepted')
    for label, attribution in (('tab', 'Invalid\tValue'), ('newline', 'Invalid\nValue'), ('control', 'Invalid\x00Value')):
        try:
            build(source, root / f'{label}.zip', attribution, 'synthetic', 'none', False)
        except ValueError:
            pass
        else:
            raise AssertionError(f'Invalid {label} attribution accepted')
    # Python -O must not bypass the builder's safety checks.
    import subprocess, sys
    command = [sys.executable, '-O', str(Path(__file__).with_name('build_audio_pack.py')),
               str(source), '--reciter', 'Synthetic', '--source', 'test',
               '--rights', 'test', '--output', str(root / 'optimized.zip')]
    outcome = subprocess.run(command, capture_output=True, text=True, check=False)
    assert outcome.returncode != 0 and not (root / 'optimized.zip').exists(), (outcome.returncode, outcome.stderr)
print('PASS: pack builder and invalid-ayah rejection using temporary synthetic data')
