#!/usr/bin/env python3
"""Build a local QSR-WORD-TIMING-1 ZIP from independently reviewed timing TSV files.

No audio, Quran, internet, or timing data is synthesized. Requires complete canonical
JSONL and a locally supplied QSR-AUDIO-1 manifest with the exact MP3 hashes.
The result STILL requires listening-based human review of every timed recording.

Example:
  python3 tools/build_word_timing_pack.py \
    --timings-dir /path/to/my-approved-timings \
    --audio-manifest /path/to/audio/manifest.tsv \
    --corpus app/src/main/assets/quran/verses.jsonl \
    --reciter 'Name in audio pack' --source 'Original provenance' \
    --rights 'Permission details' --output /path/to/timing-pack.zip
"""
import argparse
import hashlib
import json
import pathlib
import re
import zipfile
from import_tanzil import COUNTS

SHA = re.compile(r'^[0-9a-fA-F]{64}$')
FILENAME = re.compile(r'^(\d{3})_(\d{3})\.tsv$')


def canonical(path):
    verses = {}
    for line in pathlib.Path(path).read_text(encoding='utf-8').splitlines():
        if line.strip():
            obj = json.loads(line)
            if set(obj) != {'surah', 'ayah', 'arabic'}:
                raise ValueError('Unexpected canonical Quran data shape')
            s, a, value = obj['surah'], obj['ayah'], obj['arabic']
            if not (isinstance(s, int) and isinstance(a, int) and 1 <= s <= 114 and
                    1 <= a <= COUNTS[s - 1] and isinstance(value, str) and value):
                raise ValueError('Bad canonical Quran entry')
            if (s, a) in verses:
                raise ValueError('Duplicate Quran entry')
            verses[(s, a)] = value
    if len(verses) != 6236:
        raise ValueError('Require a COMPLETE 6236-ayah canonical corpus first')
    for s, num in enumerate(COUNTS, 1):
        if any((s, a) not in verses for a in range(1, num + 1)):
            raise ValueError('Canonical corpus has missing entries')
    return verses


def audio_entries(path):
    lines = pathlib.Path(path).read_text(encoding='utf-8').splitlines()
    if not lines or lines[0] != '# QSR-AUDIO-1':
        raise ValueError('Expected QSR-AUDIO-1 audio manifest')
    values, result = {}, {}
    for line in lines[1:]:
        if line.startswith('# '):
            k, _, v = line[2:].partition('=')
            if k in values:
                raise ValueError('Duplicate audio header')
            values[k] = v
        elif line.strip():
            cols = line.split('\t')
            if len(cols) != 4:
                raise ValueError('Invalid audio entry')
            s, a = int(cols[0]), int(cols[1])
            sha = cols[3].lower()
            if (not 1 <= s <= 114 or not 1 <= a <= COUNTS[s - 1] or
                    cols[2] != f'{s:03d}_{a:03d}.mp3' or not SHA.fullmatch(sha)):
                raise ValueError('Invalid audio entry reference/hash')
            if (s, a) in result:
                raise ValueError('Duplicate audio entry')
            result[(s, a)] = sha
    if set(values) != {'reciter', 'source', 'rights'}:
        raise ValueError('Missing audio attribution')
    return values['reciter'].strip(), result


def check_tsv(contents, expected_words):
    lines = contents.decode('utf-8').splitlines()
    if len(lines) != expected_words or not 1 <= expected_words <= 800:
        raise ValueError('Timing row count must match canonical word count')
    previous_end = -1
    for index, line in enumerate(lines, 1):
        p = line.split('\t')
        if len(p) != 3:
            raise ValueError('Timing row expects index/start/end')
        n, start, end = map(int, p)
        if not (n == index and 0 <= start < end <= 900_000 and start >= previous_end):
            raise ValueError('Overlapping, invalid, or unsorted word timings')
        previous_end = end


def build(args):
    verses = canonical(args.corpus)
    reciter, audio = audio_entries(args.audio_manifest)
    for field in ('reciter', 'source', 'rights'):
        value = getattr(args, field)
        if not (value and 1 <= len(value) <= 240 and not any(ord(c) < 32 for c in value)):
            raise ValueError('Missing or invalid attribution: ' + field)
    if args.reciter != reciter:
        raise ValueError('Timing reciter must match the installed audio pack reciter exactly')
    sources = sorted(pathlib.Path(args.timings_dir).iterdir())
    if not sources:
        raise ValueError('No timing files')
    output = pathlib.Path(args.output).resolve()
    rows, files = [], []
    for path in sources:
        if not path.is_file() or not FILENAME.fullmatch(path.name):
            raise ValueError('Timing directory may contain only ###_###.tsv files')
        match = FILENAME.fullmatch(path.name)
        s, a = int(match.group(1)), int(match.group(2))
        if (s, a) not in verses or (s, a) not in audio:
            raise ValueError(f'{s}:{a} absent from canonical Quran or audio pack')
        data = path.read_bytes()
        if len(data) > 32 * 1024:
            raise ValueError('Timing TSV too large')
        verse = verses[(s, a)]
        count = len(verse.split())
        check_tsv(data, count)
        digest = hashlib.sha256(data).hexdigest()
        text_sha = hashlib.sha256(verse.encode('utf-8')).hexdigest()
        rows.append(f'{s}\t{a}\t{path.name}\t{digest}\t{audio[(s,a)]}\t{text_sha}\t{count}')
        files.append((path.name, data))
    manifest = '\n'.join([
        '# QSR-WORD-TIMING-1',
        '# reciter=' + args.reciter,
        '# source=' + args.source,
        '# rights=' + args.rights,
        *rows, ''
    ]).encode('utf-8')
    if len(manifest) > 2 * 1024 * 1024:
        raise ValueError('Manifest too large')
    with zipfile.ZipFile(output, 'w', compression=zipfile.ZIP_DEFLATED) as z:
        z.writestr('manifest.tsv', manifest)
        for name, contents in files:
            z.writestr(name, contents)
    print(f'Created {output}; {len(files)} word-timed verses. NOT an authenticity certification.')


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--timings-dir', required=True)
    p.add_argument('--audio-manifest', required=True)
    p.add_argument('--corpus', required=True)
    p.add_argument('--reciter', required=True)
    p.add_argument('--source', required=True)
    p.add_argument('--rights', required=True)
    p.add_argument('--output', required=True)
    build(p.parse_args())
