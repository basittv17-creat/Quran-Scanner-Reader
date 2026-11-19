#!/usr/bin/env python3
"""Check paired Android resources before running the Android toolchain."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

RES = Path(__file__).resolve().parents[1] / "app/src/main/res"
FORMAT = re.compile(r"%(?:%|([1-9][0-9]*)\$([ds]))")


class NativeResourceTest(unittest.TestCase):
    def read_locale(self, directory):
        root = ET.parse(RES / directory / "strings.xml").getroot()
        entries = {}
        for element in root:
            name = element.attrib["name"]
            self.assertNotIn(name, entries)
            if element.tag == "string":
                entries[name] = {"string": self.signature(element)}
            else:
                self.assertEqual(element.tag, "plurals")
                forms = {item.attrib["quantity"]: self.signature(item) for item in element}
                self.assertEqual(set(forms), {"one", "other"})
                self.assertEqual(len(forms), len(element))
                self.assertEqual(forms["one"], forms["other"])
                entries[name] = forms
        return entries

    def signature(self, element):
        value = "".join(element.itertext()).strip()
        self.assertTrue(value)
        self.assertNotIn("%", FORMAT.sub("", value), value)
        signature = sorted((int(index), kind) for index, kind in FORMAT.findall(value) if index)
        indices = {index for index, _ in signature}
        self.assertEqual(indices, set(range(1, len(indices) + 1)), value)
        return signature

    def test_english_and_urdu_keys_and_positional_formats_match(self):
        self.assertEqual(self.read_locale("values"), self.read_locale("values-ur"))

    def test_accessible_scan_paths_have_distinct_labels(self):
        paths = [
            "live_crop_reset", "live_scan_full_image", "live_scan_photo",
            "gallery_scan_crop", "gallery_scan_full",
        ]
        for directory in ("values", "values-ur"):
            root = ET.parse(RES / directory / "strings.xml").getroot()
            labels = [root.find(f"./string[@name='{name}']").text for name in paths]
            self.assertEqual(len(labels), len(set(labels)))


if __name__ == "__main__":
    unittest.main()
