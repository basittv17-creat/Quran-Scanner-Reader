#!/usr/bin/env python3
"""Score reviewed OCR exports locally. Never generates Quran ground truth or runs OCR.

Each JSONL row: sample_id, printing, image_sha256, rights, reviewer,
expected_text, raw_ocr, expected_refs, predicted_refs, latency_ms.
Refs are ordered [surah, ayah] pairs. Strict CER preserves every Unicode mark;
WER splits whitespace only. Recognition is exact ordered reference-list accuracy.
"""
import argparse
import hashlib
import json
import math
from pathlib import Path
from import_tanzil import COUNTS


def distance(expected, actual):
    """Levenshtein edit distance using O(min(n,m)) memory."""
    if len(expected) < len(actual):
        expected, actual = actual, expected
    previous = list(range(len(actual) + 1))
    for i, left in enumerate(expected, 1):
        current = [i]
        for j, right in enumerate(actual, 1):
            current.append(min(current[-1] + 1, previous[j] + 1,
                               previous[j - 1] + (left != right)))
        previous = current
    return previous[-1]


def refs(value, allow_empty=False):
    if not isinstance(value, list) or (not value and not allow_empty):
        raise ValueError('References must be a list of [surah, ayah] pairs')
    result = []
    for pair in value:
        if (not isinstance(pair, list) or len(pair) != 2 or
                any(type(x) is not int for x in pair)):
            raise ValueError('Invalid reference pair')
        s, a = pair
        if not 1 <= s <= 114 or not 1 <= a <= COUNTS[s - 1] or pair in result:
            raise ValueError('Out-of-range or duplicate reference')
        result.append(pair)
    return result


PROVENANCE_FIELDS = ('script', 'publisher', 'device', 'app_commit', 'model_sha256', 'profile')
SCRIPTS = ('indo-pak', 'madani')


def is_hex_digest(value, size):
    return isinstance(value, str) and len(value) == size and all(c in '0123456789abcdef' for c in value)


def provenance(row):
    values = tuple(row.get(name) for name in PROVENANCE_FIELDS)
    for field, value in zip(PROVENANCE_FIELDS, values):
        if not isinstance(value, str) or not value.strip():
            raise ValueError('Missing run provenance: ' + field)
    script, _, _, app_commit, model_sha256, _ = values
    if script not in SCRIPTS:
        raise ValueError('Script must be indo-pak or madani')
    if not is_hex_digest(app_commit, 40) or not is_hex_digest(model_sha256, 64):
        raise ValueError('Invalid full app commit or model SHA-256')
    return values


def record(groups, key, observation):
    group = groups.setdefault(key, dict(samples=0, char_edits=0, reference_characters=0,
        word_edits=0, reference_words=0, exact_reference_matches=0, abstentions=0,
        incorrect_predictions=0, latency_ms=[]))
    group['samples'] += 1
    for field, value in observation.items():
        if field == 'latency_ms':
            group[field].append(value)
        else:
            group[field] += value


def summarize(group):
    samples = group['samples']
    group['cer'] = group['char_edits'] / group['reference_characters']
    group['wer'] = group['word_edits'] / group['reference_words']
    group['verse_reference_accuracy'] = group['exact_reference_matches'] / samples
    group['abstention_rate'] = group['abstentions'] / samples
    group['false_positive_rate'] = group['incorrect_predictions'] / samples
    latencies = sorted(group.pop('latency_ms'))
    group['latency_mean_ms'] = sum(latencies) / samples
    group['latency_p95_ms'] = latencies[math.ceil(samples * .95) - 1]
    return group


def evaluate(rows, require_provenance=False):
    groups, script_groups, configurations, seen = {}, {}, {}, set()
    with_provenance = None
    count = 0
    for row in rows:
        for name in ('sample_id', 'printing', 'rights', 'reviewer', 'expected_text'):
            if not isinstance(row.get(name), str) or not row[name].strip():
                raise ValueError('Missing reviewed sample field: ' + name)
        sample_id = row['sample_id']
        if sample_id in seen:
            raise ValueError('Duplicate sample_id: ' + sample_id)
        seen.add(sample_id)
        digest = row.get('image_sha256')
        if not is_hex_digest(digest, 64):
            raise ValueError('Missing lowercase image SHA-256')
        expected, actual = row['expected_text'], row.get('raw_ocr')
        if not isinstance(actual, str) or max(len(expected), len(actual)) > 20000:
            raise ValueError('OCR strings must be bounded to 20000 code points')
        expected_refs = refs(row.get('expected_refs'))
        predicted_refs = refs(row.get('predicted_refs'), allow_empty=True)
        latency = row.get('latency_ms')
        if type(latency) not in (int, float) or not math.isfinite(latency) or latency < 0:
            raise ValueError('Invalid measured latency')
        qualified = require_provenance or any(name in row for name in PROVENANCE_FIELDS)
        if with_provenance is not None and qualified != with_provenance:
            raise ValueError('Do not mix legacy rows and rows with run provenance')
        with_provenance = qualified
        config = provenance(row) if qualified else None
        expected_words = expected.split()
        observation = dict(
            char_edits=distance(expected, actual), reference_characters=len(expected),
            word_edits=distance(expected_words, actual.split()), reference_words=len(expected_words),
            exact_reference_matches=predicted_refs == expected_refs, abstentions=not predicted_refs,
            incorrect_predictions=bool(predicted_refs) and predicted_refs != expected_refs, latency_ms=latency)
        record(groups, row['printing'], observation)
        if config:
            record(script_groups, config[0], observation)
            record(configurations, (*config, row['printing']), observation)
        count += 1
    if not count:
        raise ValueError('No reviewed real-image samples supplied; accuracy is unavailable')
    report = {'schema': 'QSR-OCR-BENCHMARK-2' if with_provenance else 'QSR-OCR-BENCHMARK-1',
              'samples': count, 'by_printing': {label: summarize(stats) for label, stats in groups.items()},
              'limit': 'Operator-supplied reviewed exports; metadata alone does not certify rights or authenticity. No release accuracy guarantee.'}
    if with_provenance:
        report['by_script'] = {label: summarize(stats) for label, stats in script_groups.items()}
        report['by_configuration'] = [
            {**dict(zip((*PROVENANCE_FIELDS, 'printing'), config)), 'metrics': summarize(stats)}
            for config, stats in sorted(configurations.items())
        ]
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('samples', type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--require-provenance', action='store_true',
                        help='require script, publisher, device, app commit, model hash and profile on every row')
    args = parser.parse_args()
    source = args.samples.read_bytes()
    rows = (json.loads(line) for line in source.decode('utf-8').splitlines() if line.strip())
    report = evaluate(rows, require_provenance=args.require_provenance)
    report['input_sha256'] = hashlib.sha256(source).hexdigest()
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f"Scored {report['samples']} samples; report: {args.output}")


if __name__ == '__main__':
    main()
