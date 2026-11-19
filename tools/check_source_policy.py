#!/usr/bin/env python3
"""Static build/dependency policy. Does not attest remote dependency bytes."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parent.parent
ns = '{http://schemas.android.com/apk/res/android}'
manifest = ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
assert all(x.get(ns+'name') != 'android.permission.INTERNET' for x in manifest.findall('uses-permission'))
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
print('PASS: offline manifest, fixed dependency versions, pinned Actions, compile/test/lint workflow and no Android packages')
