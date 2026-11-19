#!/usr/bin/env python3
"""Static build/dependency policy. Does not attest remote dependency bytes."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET
ANDROID = '{http://schemas.android.com/apk/res/android}'
BACKUP_DOMAINS = {
    'root', 'file', 'database', 'sharedpref', 'external',
    'device_root', 'device_file', 'device_database', 'device_sharedpref',
}


def check_privacy_policy(manifest, extraction_rules):
    for element in manifest:
        if element.tag.startswith('uses-permission'):
            if element.get(ANDROID + 'name') == 'android.permission.INTERNET':
                raise ValueError('INTERNET permission is forbidden')
    application = manifest.find('application')
    if application is None:
        raise ValueError('Missing application declaration')
    for attribute, expected in {
        'usesCleartextTraffic': 'false',
        'allowBackup': 'false',
        'fullBackupContent': 'false',
        'dataExtractionRules': '@xml/data_extraction_rules',
    }.items():
        if application.get(ANDROID + attribute) != expected:
            raise ValueError(f'Unexpected application {attribute}')
    if extraction_rules.tag != 'data-extraction-rules':
        raise ValueError('Unexpected data extraction rules root')
    for mode in ('cloud-backup', 'device-transfer'):
        sections = extraction_rules.findall(mode)
        if len(sections) != 1 or sections[0].find('include') is not None:
            raise ValueError(f'Expected one exclusion-only {mode} section')
        excluded = {
            rule.get('domain') for rule in sections[0].findall('exclude')
            if rule.get('path') == '.'
        }
        if missing := BACKUP_DOMAINS - excluded:
            raise ValueError(f'Missing {mode} exclusions: {sorted(missing)}')


def main():
    root = Path(__file__).resolve().parent.parent
    source = root / 'app/src/main'
    check_privacy_policy(
        ET.parse(source / 'AndroidManifest.xml').getroot(),
        ET.parse(source / 'res/xml/data_extraction_rules.xml').getroot(),
    )
    for path in root.rglob('*.gradle.kts'):
        if 'build' in path.parts or '.gradle' in path.parts:
            continue
        source = path.read_text()
        for version in re.findall(r'(?:implementation|testImplementation)\("[^"\n]+:([^:"\n]+)"\)', source):
            assert not any(x in version.lower() for x in ('+', 'latest', 'snapshot', '[', '(')), (path,version)
    for path in (root/'.github/workflows').glob('*.y*ml'):
        text = path.read_text()
        for action in re.findall(r'^\s*(?:-\s*)?uses:\s*([^\s#]+)', text, re.MULTILINE):
            assert re.fullmatch(r'[\w./-]+@[0-9a-f]{40}', action), (path, action)
        assert not re.search(r'\b(?:assemble\w*|bundle(?:Debug|Release)|package(?:Debug|Release))\b', text), path
        assert ':app:testDebugUnitTest' in text
        assert ':app:lintDebug' in text
    assert not list(root.rglob('*.apk')) and not list(root.rglob('*.aab'))
    print('PASS: offline manifest and backup exclusions, fixed dependency versions, pinned Actions, compile/test/lint workflow and no Android packages')


if __name__ == '__main__':
    main()
