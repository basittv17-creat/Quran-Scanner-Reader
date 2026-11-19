#!/usr/bin/env python3
"""Structural/privacy and pure Kotlin checks for Phase 6, NOT an Android build test."""
from pathlib import Path
import hashlib
import subprocess
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
src = root / 'app/src/main/java/org/quranscan/reader'
manifest = root / 'app/src/main/AndroidManifest.xml'
xml = ET.parse(manifest).getroot()
permissions = [node.attrib.get('{http://schemas.android.com/apk/res/android}name', '')
               for node in xml.findall('uses-permission')]
assert 'android.permission.CAMERA' in permissions
assert 'android.permission.INTERNET' not in permissions
activities = xml.findall('.//activity')
assert any(a.attrib.get('{http://schemas.android.com/apk/res/android}name') == '.LiveScanActivity'
           for a in activities)
gradle = (root / 'app/build.gradle.kts').read_text()
for dep in ('camera-camera2', 'camera-lifecycle', 'camera-view', 'activity:1.9.3'):
    assert dep in gradle, dep
live = (src / 'LiveScanActivity.kt').read_text()
for required in ('STRATEGY_KEEP_ONLY_LATEST','ImageCapture.OnImageCapturedCallback',
                 'RequestPermission','ProcessCameraProvider','image.close()',
                 'OfflineArabicOcr','getString(R.string.gallery_ocr_unverified)', 'PreviewView'):
    assert required in live, required
resources = root / 'app/src/main/res'
for directory in ('values', 'values-ur'):
    strings = ET.parse(resources / directory / 'strings.xml').getroot()
    warning = strings.find("./string[@name='gallery_ocr_unverified']")
    assert warning is not None and (warning.text or '').strip(), directory
assert 'https://' not in live and 'http://' not in live
main = (src / 'MainActivity.kt').read_text()
assert 'LiveScanActivity::class.java' in main
assert 'ACTION_IMAGE_CAPTURE' not in main
model = root / 'app/src/main/assets/tessdata/ara.traineddata'
assert hashlib.sha256(model.read_bytes()).hexdigest() == 'e3206d3dc87fd50c24a0fb9f01838615911d25168f4e64415244b67d2bb3e729'
subprocess.run(['python3', str(root/'tools/verify_corpus_provenance.py')], check=True)
out = root / 'tools/.phase6-gate-check.jar'
try:
    subprocess.run(['kotlinc', str(src/'core/CameraScanGate.kt'),
        str(root/'tools/test_phase6_gate.kt'), '-include-runtime', '-d', str(out)],
        check=True, capture_output=True, text=True, timeout=90)
    subprocess.run(['java','-jar',str(out)], check=True, timeout=20)
finally:
    if out.exists(): out.unlink()
print('PASS: CameraX manifest and dependencies present')
print('PASS: live frame capture / manual capture / gallery route source checks')
print('PASS: bundled OCR SHA, zero internet permissions, bundled Quran integrity (not independent scholarly review)')
print('NOT RUN: Android SDK build, device CameraX runtime, real Mushaf OCR accuracy')
