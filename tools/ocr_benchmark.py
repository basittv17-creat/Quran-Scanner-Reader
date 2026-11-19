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


def evaluate(rows):
    groups, seen = {}, set()
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
        if (not isinstance(digest, str) or len(digest) != 64 or
                any(c not in '0123456789abcdef' for c in digest)):
            raise ValueError('Missing lowercase image SHA-256')
        expected, actual = row['expected_text'], row.get('raw_ocr')
        if not isinstance(actual, str) or max(len(expected), len(actual)) > 20000:
            raise ValueError('OCR strings must be bounded to 20000 code points')
        expected_refs = refs(row.get('expected_refs'))
        predicted_refs = refs(row.get('predicted_refs'), allow_empty=True)
        latency = row.get('latency_ms')
        if type(latency) not in (int, float) or not math.isfinite(latency) or latency < 0:
            raise ValueError('Invalid measured latency')
        group = groups.setdefault(row['printing'], dict(samples=0, char_edits=0,
            reference_characters=0, word_edits=0, reference_words=0,
            exact_reference_matches=0, abstentions=0, incorrect_predictions=0,
            latency_ms=[]))
        group['samples'] += 1
        group['char_edits'] += distance(expected, actual)
        group['reference_characters'] += len(expected)
        group['word_edits'] += distance(expected.split(), actual.split())
        group['reference_words'] += len(expected.split())
        correct = predicted_refs == expected_refs
        group['exact_reference_matches'] += correct
        group['abstentions'] += not predicted_refs
        group['incorrect_predictions'] += bool(predicted_refs) and not correct
        group['latency_ms'].append(latency)
        count += 1
    if not count:
        raise ValueError('No reviewed real-image samples supplied; accuracy is unavailable')
    for group in groups.values():
        group['cer'] = group['char_edits'] / group['reference_characters']
        group['wer'] = group['word_edits'] / group['reference_words']
        group['verse_reference_accuracy'] = group['exact_reference_matches'] / group['samples']
        latencies = sorted(group.pop('latency_ms'))
        group['latency_mean_ms'] = sum(latencies) / len(latencies)
        group['latency_p95_ms'] = latencies[math.ceil(len(latencies) * .95) - 1]
    return {'schema': 'QSR-OCR-BENCHMARK-1', 'samples': count, 'by_printing': groups,
            'limit': 'Operator-supplied reviewed exports; metadata alone does not certify rights or authenticity. No release accuracy guarantee.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('samples', type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    source = args.samples.read_bytes()
    report = evaluate(json.loads(line) for line in source.decode('utf-8').splitlines() if line.strip())
    report['input_sha256'] = hashlib.sha256(source).hexdigest()
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f"Scored {report['samples']} samples; report: {args.output}")


if __name__ == '__main__':
    main()
