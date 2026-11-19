#!/usr/bin/env python3
"""Expose actual JUnit counts and lint issues in Actions logs without packaging."""
from pathlib import Path
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parent.parent
reports = list((root/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
if not reports:
    print('Unit results unavailable: tests did not run or reports are missing')
for report in reports:
    suite = ET.parse(report).getroot()
    print('UNIT:', suite.get('name'), {k:suite.get(k) for k in ('tests','failures','errors','skipped')})
lint = root/'app/build/reports/lint-results-debug.xml'
if lint.exists():
    issues = ET.parse(lint).getroot().findall('issue')
    print('LINT:', sum(x.get('severity') == 'Error' for x in issues), 'errors;', sum(x.get('severity') == 'Warning' for x in issues), 'warnings')
    for issue in issues:
        loc = issue.find('location')
        print(issue.get('severity'), issue.get('id'), issue.get('message'), dict(loc.attrib) if loc is not None else '')
else:
    print('Lint results unavailable: analysis did not complete')
