"""Synthetic metric tests only; these do not measure Quran OCR accuracy."""
import unittest
from ocr_benchmark import distance, evaluate


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


if __name__ == '__main__': unittest.main()
