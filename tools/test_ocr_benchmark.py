"""Synthetic metric tests only; these do not measure Quran OCR accuracy."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from ocr_benchmark import DATASET_SCHEMA, distance, evaluate, evaluate_dataset, read_rows


class BenchmarkTests(unittest.TestCase):
    def sample(self, **changes):
        row = dict(sample_id='test-only', printing='synthetic', image_sha256='a'*64,
                   rights='test only', reviewer='test fixture', expected_text='abc def',
                   raw_ocr='abc def', expected_refs=[[1, 1]], predicted_refs=[[1, 1]], latency_ms=10)
        row.update(changes)
        return row

    def test_edits(self):
        for a, b, expected in [('abc', 'axc', 1), ('', 'abc', 3), ('abc', 'ac', 1), ('a', 'abc', 2)]:
            self.assertEqual(distance(a, b), expected)
            self.assertEqual(distance(b, a), expected)

    def test_aggregate_and_abstention(self):
        result = evaluate([self.sample(), self.sample(sample_id='second', raw_ocr='abc', predicted_refs=[], latency_ms=30)])
        g = result['by_printing']['synthetic']
        self.assertEqual(g['cer'], 4/14)
        self.assertEqual(g['wer'], 1/4)
        self.assertEqual(g['verse_reference_accuracy'], .5)
        self.assertEqual(g['abstentions'], 1)
        self.assertEqual(g['latency_mean_ms'], 20)
        self.assertEqual(g['latency_p95_ms'], 30)

    def test_marks_and_insertions_preserved(self):
        g = evaluate([self.sample(expected_text='بِ', raw_ocr='ب')])['by_printing']['synthetic']
        self.assertEqual(g['cer'], .5)
        self.assertEqual(g['wer'], 1)
        g = evaluate([self.sample(expected_text='a', raw_ocr='abc')])['by_printing']['synthetic']
        self.assertEqual(g['cer'], 2)  # CER may exceed 100% with insertions.

    def test_invalid_or_empty_rejected(self):
        for changes in [dict(expected_text=' '), dict(image_sha256='bad'), dict(raw_ocr=None),
                        dict(expected_refs=[[115, 1]]), dict(predicted_refs=[[1, True]]),
                        dict(latency_ms=float('nan')), dict(reviewer='')]:
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                evaluate([self.sample(**changes)])
        with self.assertRaises(ValueError): evaluate([])
        with self.assertRaises(ValueError): evaluate([self.sample(), self.sample()])

    def test_grouping_and_wrong_prediction(self):
        r = evaluate([self.sample(predicted_refs=[[1, 2]]), self.sample(sample_id='b', printing='other')])
        self.assertEqual(r['by_printing']['synthetic']['incorrect_predictions'], 1)
        self.assertEqual(r['by_printing']['other']['verse_reference_accuracy'], 1)

    def test_provenance_reports_scripts_and_configuration_separately(self):
        common = dict(publisher='fixture publisher', device='fixture device', app_commit='c'*40,
                      model_sha256='b'*64, profile='auto')
        observations = [
            self.sample(script='indo-pak', **common),
            self.sample(sample_id='miss', script='madani', predicted_refs=[], **common),
            self.sample(sample_id='wrong', script='madani', predicted_refs=[[1, 2]], **common),
        ]
        report = evaluate(observations, require_provenance=True)
        self.assertEqual(report['schema'], 'QSR-OCR-BENCHMARK-2')
        self.assertEqual(report['by_script']['indo-pak']['verse_reference_accuracy'], 1)
        self.assertEqual(report['by_script']['madani']['abstention_rate'], .5)
        self.assertEqual(report['by_script']['madani']['false_positive_rate'], .5)
        self.assertEqual(len(report['by_configuration']), 2)
        self.assertEqual(report['by_configuration'][0]['model_sha256'], 'b'*64)

    def test_comparison_requires_complete_provenance(self):
        with self.assertRaises(ValueError):
            evaluate([self.sample()], require_provenance=True)
        with self.assertRaises(ValueError):
            evaluate([self.sample(script='madani')])


    def test_malformed_jsonl_reports_physical_line(self):
        for bad_line in (b'{', b'[]', b'null', b'{"x":1,"x":2}', bytes([255])):
            with self.subTest(bad_line=bad_line), self.assertRaisesRegex(ValueError, 'line 3'):
                list(read_rows(b'{}\r\n\n' + bad_line))

    def test_missing_required_legacy_fields(self):
        for field in self.sample():
            row = self.sample()
            del row[field]
            with self.subTest(field=field), self.assertRaises(ValueError):
                evaluate([row])

    def test_reference_bounds_duplicates_and_order(self):
        for references in ([[114, 7]], [[1, 1], [1, 1]], [[1, 0]]):
            with self.subTest(references=references), self.assertRaises(ValueError):
                evaluate([self.sample(predicted_refs=references)])
        report = evaluate([self.sample(expected_refs=[[1, 1], [1, 2]],
                                       predicted_refs=[[1, 2], [1, 1]])])
        self.assertEqual(report['by_printing']['synthetic']['false_positive_rate'], 1)

    def test_duplicate_id_across_printings(self):
        with self.assertRaisesRegex(ValueError, 'Duplicate sample_id'):
            evaluate([self.sample(), self.sample(printing='other')])

    def test_empty_ocr_and_abstention(self):
        metrics = evaluate([self.sample(raw_ocr='', predicted_refs=[])])['by_printing']['synthetic']
        self.assertEqual((metrics['char_edits'], metrics['word_edits']), (7, 2))
        self.assertEqual((metrics['cer'], metrics['wer']), (1, 1))
        self.assertEqual(metrics['abstention_rate'], 1)
        self.assertEqual(metrics['false_positive_rate'], 0)

    def test_mixed_export_in_either_order(self):
        legacy = self.sample()
        qualified = self.sample(sample_id='qualified', script='madani', publisher='fixture',
                                device='device', app_commit='c'*40, model_sha256='b'*64, profile='auto')
        for rows in ([legacy, qualified], [qualified, legacy]):
            with self.assertRaisesRegex(ValueError, 'Do not mix'):
                evaluate(rows)

    def test_configuration_distinguishes_model_within_same_printing(self):
        common = dict(script='madani', publisher='fixture', device='device',
                      app_commit='c'*40, profile='auto')
        report = evaluate([self.sample(model_sha256='b'*64, **common),
                           self.sample(sample_id='second', model_sha256='d'*64,
                                       predicted_refs=[], **common)])
        self.assertEqual(report['by_script']['madani']['samples'], 2)
        self.assertEqual(report['by_printing']['synthetic']['abstention_rate'], .5)
        self.assertEqual([(row['model_sha256'], row['metrics']['abstentions'])
                          for row in report['by_configuration']], [('b'*64, 0), ('d'*64, 1)])

    def test_cli_hashes_original_bytes_and_preserves_unicode_counts(self):
        row = self.sample(expected_text='é\u2028😀', raw_ocr='e\u0301\u2028😀')
        source = ('\r\n' + json.dumps(row, ensure_ascii=False) + '\r\n').encode('utf-8')
        with tempfile.TemporaryDirectory() as directory:
            samples = Path(directory) / 'samples.jsonl'
            output = Path(directory) / 'report.json'
            samples.write_bytes(source)
            command = [sys.executable, str(Path(__file__).with_name('ocr_benchmark.py')),
                       str(samples), '--output', str(output)]
            subprocess.run(command, check=True, capture_output=True)
            report = json.loads(output.read_text(encoding='utf-8'))
            self.assertEqual(report['input_sha256'], hashlib.sha256(source).hexdigest())
            metrics = report['by_printing']['synthetic']
            self.assertEqual((metrics['char_edits'], metrics['reference_characters']), (2, 3))
            self.assertEqual((metrics['word_edits'], metrics['reference_words']), (1, 2))
            samples.write_bytes(source + b'null\n')
            invalid = subprocess.run(command, capture_output=True)
            self.assertEqual(invalid.returncode, 2)
            self.assertIn(b'line 3', invalid.stderr)
            self.assertEqual(json.loads(output.read_text(encoding='utf-8')), report)


class DatasetTests(unittest.TestCase):
    """Metadata and byte fixtures only. No genuine images or reviewed labels."""

    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.images = Path(directory.name)
        self.manifest = dict(schema=DATASET_SCHEMA, dataset_version='fixture-1',
                             split_protocol='fixture', label_policy='fixture', captures=[
            self.capture('train', 'train'), self.capture('test', 'test'),
        ])
        self.observation = dict(
            capture_id='test', sample_id='run', image_sha256=self.manifest['captures'][1]['image_sha256'],
            device='fixture Android', app_commit='a' * 40, model_sha256='b' * 64, profile='auto',
            matcher='fixture', decision_rule='fixture', candidate_confidence=1,
            raw_ocr='ب', predicted_refs=[[1, 1]], latency_ms=10,
        )

    def capture(self, name, split):
        content = name.encode()
        digest = hashlib.sha256(content).hexdigest()
        (self.images / digest).write_bytes(content)
        return dict(
            capture_id=name, page_id=name, content_group=name, split=split, script='madani',
            printing='fixture printing', publisher='fixture publisher', capture_device='fixture camera',
            capture_setup='fixture setup', condition='skew', source_url='https://example.invalid/source',
            rights_evidence_url='https://example.invalid/rights', rights_checked_on='2026-11-19',
            edition_rights='fixture', photograph_rights='fixture', benchmark_approved=True,
            redistribution='restricted', reviewer='fixture author', independent_reviewer='fixture checker',
            review_evidence='fixture only', expected_text='بِ', expected_refs=[[1, 1]],
            original_sha256=digest, image_sha256=digest,
        )

    def score(self, *observations):
        return evaluate_dataset(self.manifest, observations or [self.observation], self.images)

    def test_strict_marks_and_confidence_outcomes_by_condition(self):
        report = self.score(self.observation, dict(self.observation, sample_id='abstained',
                            decision_rule='other', predicted_refs=[], candidate_confidence=.4))
        self.assertEqual(report['schema'], 'QSR-OCR-BENCHMARK-3')
        self.assertEqual([(r['condition'], r['metrics']['cer']) for r in report['by_condition']],
                         [('skew', .5), ('skew', .5)])
        counts = report['candidate_confidence_counts']
        self.assertEqual([(r['confidence_decile'], r['correct'], r['abstained']) for r in counts],
                         [('9', 1, 0), ('4', 0, 1)])

    def test_reject_split_leakage_by_page_alias_and_bytes(self):
        train, test = self.manifest['captures']
        for key in ('page_id', 'content_group', 'original_sha256', 'image_sha256'):
            with self.subTest(key=key), self.assertRaisesRegex(ValueError, 'Train/test overlap'):
                self.manifest['captures'][1] = dict(test, **{key: train[key]})
                self.score()

    def test_missing_review_rights_and_labels_are_rejected(self):
        test = self.manifest['captures'][1]
        for change in (dict(independent_reviewer=' FIXTURE AUTHOR '), dict(edition_rights=''),
                       dict(benchmark_approved=False), dict(expected_refs=[]),
                       dict(source_url='http://example.invalid'), dict(rights_checked_on='2026-02-30')):
            with self.subTest(change=change), self.assertRaises(ValueError):
                self.manifest['captures'][1] = dict(test, **change)
                self.score()

    def test_reject_duplicates_corrupted_bytes_and_unbound_observation(self):
        with self.assertRaisesRegex(ValueError, 'Duplicate OCR input'):
            self.score(self.observation, dict(self.observation, sample_id='alias'))
        with self.assertRaisesRegex(ValueError, 'frozen test'):
            self.score(dict(self.observation, capture_id='train'))
        with self.assertRaisesRegex(ValueError, 'differs'):
            self.score(dict(self.observation, image_sha256='c' * 64))
        with self.assertRaisesRegex(ValueError, 'Duplicate capture_id'):
            self.manifest['captures'].append(self.manifest['captures'][1])
            self.score()
        self.manifest['captures'].pop()
        (self.images / self.observation['image_sha256']).write_bytes(b'changed')
        with self.assertRaisesRegex(ValueError, 'SHA-256 mismatch'):
            self.score()

    def test_missing_or_invalid_prediction_fields(self):
        for key in ('predicted_refs', 'candidate_confidence'):
            row = dict(self.observation)
            del row[key]
            with self.subTest(key=key), self.assertRaises(ValueError):
                self.score(row)
        for score in (True, -1, float('nan'), None):
            with self.subTest(score=score), self.assertRaises(ValueError):
                self.score(dict(self.observation, candidate_confidence=score))

    def test_cli_report_is_byte_reproducible_and_binds_inputs(self):
        manifest = self.images / 'manifest.json'
        samples = self.images / 'samples.jsonl'
        output = self.images / 'report.json'
        manifest.write_text(json.dumps(self.manifest), encoding='utf-8')
        samples.write_text(json.dumps(self.observation) + '\n', encoding='utf-8')
        command = [sys.executable, str(Path(__file__).with_name('ocr_benchmark.py')), str(samples),
                   '--manifest', str(manifest), '--images', str(self.images), '--output', str(output)]
        subprocess.run(command, check=True, capture_output=True)
        first = output.read_bytes()
        subprocess.run(command, check=True, capture_output=True)
        self.assertEqual(output.read_bytes(), first)
        report = json.loads(first)
        for field, path in (('manifest_sha256', manifest), ('input_sha256', samples)):
            self.assertEqual(report[field], hashlib.sha256(path.read_bytes()).hexdigest())


if __name__ == '__main__': unittest.main()
