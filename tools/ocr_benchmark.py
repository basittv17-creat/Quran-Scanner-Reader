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
from datetime import date
from pathlib import Path
from urllib.parse import urlsplit
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


def evaluate(rows, require_provenance=False, configuration_fields=()):
    groups, script_groups, configurations, seen = {}, {}, {}, set()
    with_provenance = None
    count = 0
    for row in rows:
        if not isinstance(row, dict):
            raise ValueError('Expected an observation object')
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
            key = (*config, row['printing'], *(row[field] for field in configuration_fields))
            record(configurations, key, observation)
        count += 1
    if not count:
        raise ValueError('No reviewed real-image samples supplied; accuracy is unavailable')
    report = {'schema': 'QSR-OCR-BENCHMARK-2' if with_provenance else 'QSR-OCR-BENCHMARK-1',
              'samples': count, 'by_printing': {label: summarize(stats) for label, stats in groups.items()},
              'limit': 'Operator-supplied reviewed exports; metadata alone does not certify rights or authenticity. No release accuracy guarantee.'}
    if with_provenance:
        report['by_script'] = {label: summarize(stats) for label, stats in script_groups.items()}
        report['by_configuration'] = [
            {**dict(zip((*PROVENANCE_FIELDS, 'printing', *configuration_fields), config)), 'metrics': summarize(stats)}
            for config, stats in sorted(configurations.items())
        ]
    return report


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError('Duplicate JSON key: ' + key)
        result[key] = value
    return result


def read_rows(source):
    # JSONL uses LF delimiters, even when a JSON string contains U+2028.
    for number, line in enumerate(source.split(b'\n'), 1):
        if not line.strip():
            continue
        try:
            row = json.loads(line.decode('utf-8'), object_pairs_hook=unique_object)
        except ValueError as exc:
            raise ValueError(f'Invalid JSONL at line {number}: {exc}') from exc
        if not isinstance(row, dict):
            raise ValueError(f'Expected a JSON object at line {number}')
        yield row


DATASET_SCHEMA = 'QSR-OCR-DATASET-1'
CONDITIONS = ('baseline', 'low-light', 'blur', 'skew', 'rotation')


def require_text(row, fields):
    for field in fields:
        value = row.get(field)
        if not isinstance(value, str) or not value.strip():
            raise ValueError('Missing dataset field: ' + field)


def capture_inventory(rows, images):
    captures, assignments, verified = {}, {}, set()
    for row in rows:
        require_text(row, (
            'capture_id', 'page_id', 'content_group', 'printing', 'publisher', 'script',
            'source_url', 'rights_evidence_url', 'edition_rights', 'photograph_rights',
            'rights_checked_on', 'capture_device', 'capture_setup', 'reviewer',
            'independent_reviewer', 'review_evidence', 'expected_text',
        ))
        for field in ('source_url', 'rights_evidence_url'):
            url = urlsplit(row[field])
            if url.scheme != 'https' or not url.hostname or url.username is not None:
                raise ValueError('Expected an HTTPS evidence URL: ' + field)
        if date.fromisoformat(row['rights_checked_on']).isoformat() != row['rights_checked_on']:
            raise ValueError('Expected an ISO calendar date for rights_checked_on')
        if row.get('benchmark_approved') is not True:
            raise ValueError('Benchmark permission must be approved')
        if row.get('redistribution') not in ('approved', 'restricted'):
            raise ValueError('Record whether redistribution is approved or restricted')
        if row['reviewer'].strip().casefold() == row['independent_reviewer'].strip().casefold():
            raise ValueError('Independent reviewers must have distinct identifiers')
        refs(row.get('expected_refs'))
        if row['script'] not in SCRIPTS or row.get('condition') not in CONDITIONS:
            raise ValueError('Invalid script or capture condition')
        split = row.get('split')
        if split not in ('train', 'test'):
            raise ValueError('Split must be train or test')
        capture_id = row['capture_id']
        if capture_id in captures:
            raise ValueError('Duplicate capture_id: ' + capture_id)
        for key in ('page_id', 'content_group', 'original_sha256', 'image_sha256'):
            value = row.get(key)
            if key.endswith('_sha256') and not is_hex_digest(value, 64):
                raise ValueError('Invalid dataset digest: ' + key)
            # A digest can occur as both an original and an OCR input.
            identity = ('digest' if key.endswith('_sha256') else key, value)
            if assignments.setdefault(identity, split) != split:
                raise ValueError('Train/test overlap for ' + key + ': ' + value)
            if key.endswith('_sha256') and value not in verified:
                with (images / value).open('rb') as image:
                    if hashlib.file_digest(image, 'sha256').hexdigest() != value:
                        raise ValueError('Image SHA-256 mismatch: ' + value)
                verified.add(value)
        captures[capture_id] = row

    if set(assignments.values()) != {'train', 'test'}:
        raise ValueError('Dataset inventory must contain both train and test captures')
    return captures


def evaluate_dataset(manifest, observations, images):
    if (not isinstance(manifest, dict) or manifest.get('schema') != DATASET_SCHEMA
            or not isinstance(manifest.get('captures'), list)):
        raise ValueError('Expected a ' + DATASET_SCHEMA + ' manifest')
    require_text(manifest, ('dataset_version', 'split_protocol', 'label_policy'))
    if any(not isinstance(row, dict) for row in manifest['captures']):
        raise ValueError('Expected capture objects')
    captures = capture_inventory(manifest['captures'], images)
    by_condition, confidence_counts, coverage, seen, scored = {}, {}, {}, set(), []
    for observation in observations:
        require_text(observation, ('capture_id', 'matcher', 'decision_rule'))
        capture = captures.get(observation['capture_id'])
        if capture is None or capture['split'] != 'test':
            raise ValueError('Observation must reference a frozen test capture')
        if observation.get('image_sha256') != capture['image_sha256']:
            raise ValueError('Observation image SHA-256 differs from the test capture')
        row = {**observation, **{key: capture[key] for key in (
            'printing', 'publisher', 'script', 'image_sha256', 'reviewer', 'expected_text',
            'expected_refs',
        )}, 'rights': capture['rights_evidence_url']}
        config = provenance(row)
        key = (*config, row['printing'], observation['matcher'], observation['decision_rule'])
        duplicate = (capture['image_sha256'], *key)
        if duplicate in seen:
            raise ValueError('Duplicate OCR input and configuration')
        seen.add(duplicate)
        coverage.setdefault(key, set()).add(observation['capture_id'])
        score = observation.get('candidate_confidence')
        if ('candidate_confidence' not in observation or
                (score is not None and (type(score) not in (int, float) or not 0 <= score <= 1))):
            raise ValueError('Expected null or a candidate_confidence between 0 and 1')
        predicted = refs(row.get('predicted_refs'), allow_empty=True)
        if predicted and score is None:
            raise ValueError('A prediction requires a candidate confidence')
        bucket = 'unavailable' if score is None else str(min(int(score * 10), 9))
        counts = confidence_counts.setdefault((*key, bucket), dict(samples=0, correct=0, wrong=0, abstained=0))
        counts['samples'] += 1
        outcome = 'abstained' if not predicted else ('correct' if predicted == row['expected_refs'] else 'wrong')
        counts[outcome] += 1
        by_condition.setdefault((*key, capture['condition']), []).append(row)
        scored.append(row)

    report = evaluate(scored, require_provenance=True, configuration_fields=('matcher', 'decision_rule'))
    fields = (*PROVENANCE_FIELDS, 'printing', 'matcher', 'decision_rule')
    report.update(schema='QSR-OCR-BENCHMARK-3', dataset_version=manifest['dataset_version'])
    report['by_condition'] = [
        {**dict(zip((*fields, 'condition'), key)), 'metrics': evaluate(rows)['by_printing'][key[len(PROVENANCE_FIELDS)]]}
        for key, rows in sorted(by_condition.items())
    ]
    eligible = {}
    for capture in captures.values():
        if capture['split'] == 'test':
            stratum = (capture['script'], capture['publisher'], capture['printing'])
            eligible[stratum] = eligible.get(stratum, 0) + 1
    for config in report['by_configuration']:
        key = tuple(config[field] for field in fields)
        stratum = tuple(config[field] for field in ('script', 'publisher', 'printing'))
        config['eligible_test_captures'] = eligible[stratum]
        config['observed_test_captures'] = len(coverage[key])
    report['candidate_confidence_counts'] = [
        {**dict(zip((*fields, 'confidence_decile'), key)), **counts}
        for key, counts in sorted(confidence_counts.items())
    ]
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('samples', type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--require-provenance', action='store_true',
                        help='require script, publisher, device, app commit, model hash and profile on every row')
    parser.add_argument('--manifest', type=Path, help='versioned rights-approved capture inventory')
    parser.add_argument('--images', type=Path, help='directory of images named by lowercase SHA-256')
    args = parser.parse_args()
    if (args.manifest is None) != (args.images is None):
        parser.error('--manifest and --images must be supplied together')
    try:
        source = args.samples.read_bytes()
        if args.manifest:
            manifest_bytes = args.manifest.read_bytes()
            report = evaluate_dataset(json.loads(manifest_bytes, object_pairs_hook=unique_object), read_rows(source), args.images)
            report['manifest_sha256'] = hashlib.sha256(manifest_bytes).hexdigest()
        else:
            report = evaluate(read_rows(source), require_provenance=args.require_provenance)
    except (ValueError, OSError) as exc:
        parser.error(str(exc))
    report['input_sha256'] = hashlib.sha256(source).hexdigest()
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True, allow_nan=False) + '\n', encoding='utf-8')
    print(f"Scored {report['samples']} samples; report: {args.output}")


if __name__ == '__main__':
    main()
