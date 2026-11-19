#!/usr/bin/env python3
"""Regression tests for the offline manifest and backup policies."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

from check_source_policy import ANDROID, check_privacy_policy


class PrivacyPolicyTest(unittest.TestCase):
    def setUp(self):
        source = Path(__file__).resolve().parents[1] / 'app/src/main'
        self.manifest = ET.parse(source / 'AndroidManifest.xml').getroot()
        self.rules = ET.parse(source / 'res/xml/data_extraction_rules.xml').getroot()

    def test_checked_in_source_satisfies_policy(self):
        check_privacy_policy(self.manifest, self.rules)

    def test_rejects_internet_in_both_permission_declarations(self):
        for tag in ('uses-permission', 'uses-permission-sdk-23'):
            with self.subTest(tag=tag):
                permission = ET.SubElement(self.manifest, tag, {ANDROID + 'name': 'android.permission.INTERNET'})
                with self.assertRaisesRegex(ValueError, 'INTERNET'):
                    check_privacy_policy(self.manifest, self.rules)
                self.manifest.remove(permission)

    def test_rejects_reenabled_backup(self):
        self.manifest.find('application').set(ANDROID + 'allowBackup', 'true')
        with self.assertRaisesRegex(ValueError, 'allowBackup'):
            check_privacy_policy(self.manifest, self.rules)

    def test_rejects_partial_device_transfer_exclusion(self):
        exclusion = self.rules.find("./device-transfer/exclude[@domain='device_file']")
        exclusion.set('path', 'some-directory')
        with self.assertRaisesRegex(ValueError, 'device-transfer.*device_file'):
            check_privacy_policy(self.manifest, self.rules)

    def test_rejects_cloud_include(self):
        ET.SubElement(self.rules.find('cloud-backup'), 'include', {'domain': 'file', 'path': '.'})
        with self.assertRaisesRegex(ValueError, 'exclusion-only cloud-backup'):
            check_privacy_policy(self.manifest, self.rules)


if __name__ == '__main__':
    unittest.main()
