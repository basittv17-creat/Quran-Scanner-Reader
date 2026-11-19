#!/usr/bin/env python3
"""Pack independently licensed local verse MP3s. Does not validate reciter/tajweed/rights.
Usage: python3 tools/build_audio_pack.py PATH_TO_MP3_FOLDER --reciter ... --source ...
       --rights ... --output my_audio.zip [--require-complete]
Input filenames MUST look like 001_001.mp3, 002_255.mp3, etc.
"""
from __future__ import annotations
import argparse
import hashlib
from pathlib import Path
import re
import zipfile

COUNTS = [
    7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,
    98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,
    75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,
    24,13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,
    40,46,42,29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,
    5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6
]


def build(folder: Path, output: Path, reciter: str, source: str, rights: str, complete=False):
    if not folder.is_dir():
        raise ValueError('MP3 source folder must exist')
    for label, value in (('reciter', reciter), ('source', source), ('rights', rights)):
        if not value or not 1 <= len(value.strip()) <= 240 or any(ord(c) < 32 for c in value):
            raise ValueError(f'Invalid {label} attribution')
    files = sorted(folder.iterdir())
    if not files or len(files) > 6236 or any(not p.is_file() or p.is_symlink() for p in files):
        raise ValueError('Audio source must contain 1 to 6236 regular files without symlinks')
    lines = ['# QSR-AUDIO-1',f'# reciter={reciter}',f'# source={source}',f'# rights={rights}']
    total = 0
    for p in files:
        if not re.fullmatch(r'\d{3}_\d{3}\.mp3', p.name):
            raise ValueError(f'Invalid filename: {p.name}')
        surah,ayah = map(int,p.stem.split('_'))
        if not 1 <= surah <= 114 or not 1 <= ayah <= COUNTS[surah - 1]:
            raise ValueError(f'Invalid Quran verse reference: {p.name}')
        size = p.stat().st_size
        if not 0 < size <= 8 * 1024 * 1024:
            raise ValueError(f'Invalid clip size: {p.name}')
        total += size
        if total > 1500 * 1024 * 1024:
            raise ValueError('Audio pack exceeds 1500 MiB size limit')
        digest = hashlib.sha256(p.read_bytes()).hexdigest()
        lines.append(f'{surah}\t{ayah}\t{p.name}\t{digest}')
    if complete:
        expected = {f'{s:03}_{a:03}.mp3' for s,n in enumerate(COUNTS,1) for a in range(1,n+1)}
        if {p.name for p in files} != expected:
            raise ValueError('Not a complete set of 6,236 verses')
    output.parent.mkdir(parents=True,exist_ok=True)
    if output.resolve() in (p.resolve() for p in files):
        raise ValueError('Output cannot overwrite an input recording')
    with zipfile.ZipFile(output,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=5) as z:
        z.writestr('manifest.tsv','\n'.join(lines)+'\n')
        for p in files:z.write(p,p.name)
    return len(files)

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('folder',type=Path)
    parser.add_argument('--reciter',required=True)
    parser.add_argument('--source',required=True)
    parser.add_argument('--rights',required=True)
    parser.add_argument('--output',required=True,type=Path)
    parser.add_argument('--require-complete',action='store_true')
    a=parser.parse_args()
    count=build(a.folder,a.output,a.reciter,a.source,a.rights,a.require_complete)
    print(f'Built local audio ZIP: {count} entries. Audio accuracy and rights NOT certified.')
