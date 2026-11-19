import unittest
from import_tanzil import COUNTS,parse

class ImportTests(unittest.TestCase):
    def test_reference_table(self):
        self.assertEqual(len(COUNTS),114)
        self.assertEqual(sum(COUNTS),6236)
    def test_incomplete_fails(self):
        with self.assertRaisesRegex(ValueError,'6236'):
            parse('1|1|نَصٌّ لِلاِخْتِبَارِ')
    def test_wrong_order_fails(self):
        with self.assertRaisesRegex(ValueError,'expected 1:1'):
            parse('1|2|نَصٌّ لِلاِخْتِبَارِ')
    def test_missing_diacritics_not_proof(self):
        with self.assertRaises(ValueError):
            parse('1|1|نص')
    def test_bad_range_fails(self):
        with self.assertRaisesRegex(ValueError,'out of range'):
            parse('115|1|نَصٌّ')
    def test_bad_replacement_character_fails(self):
        with self.assertRaisesRegex(ValueError,'invalid Unicode'):
            parse('1|1|نَصٌّ\ufffd')

if __name__ == '__main__': unittest.main()
