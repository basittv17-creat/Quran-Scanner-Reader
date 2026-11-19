#!/usr/bin/env python3
"""Phase 8 source checks. Intentionally NOT an Android build or Quran recitation QA."""
from pathlib import Path
import hashlib
import subprocess
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
base = root / 'app/src/main/java/org/quranscan/reader'

def run(*args):
    subprocess.run(list(args), check=True, timeout=120)

run('python3', str(root / 'tools/test_import_tanzil.py'))
run('python3', str(root / 'tools/verify_phase6.py'))
run('python3', str(root / 'tools/test_phase7_packaging.py'))
run('python3', str(root / 'tools/test_phase8_packaging.py'))

jar = root / 'tools/.phase8-word-check.jar'
try:
    run('kotlinc', str(base / 'core/QuranVerseCounts.kt'),
        str(base / 'audio/AudioPackFormat.kt'),
        str(base / 'audio/WordTimingFormat.kt'),
        str(root / 'tools/test_phase8_word.kt'),
        '-include-runtime', '-d', str(jar))
    run('java', '-jar', str(jar))
finally:
    jar.unlink(missing_ok=True)

xml = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
ns = '{http://schemas.android.com/apk/res/android}'
assert all('INTERNET' not in p.attrib.get(ns + 'name', '') for p in xml.findall('uses-permission'))
assert (root / 'app/src/main/assets/quran/verses.jsonl').stat().st_size == 0, 'Must not claim original Quran is bundled'
model = root / 'app/src/main/assets/tessdata/ara.traineddata'
assert hashlib.sha256(model.read_bytes()).hexdigest() == 'e3206d3dc87fd50c24a0fb9f01838615911d25168f4e64415244b67d2bb3e729'
assert all(t in (base / 'RecitationActivity.kt').read_text() for t in [
    'playWord()', 'WordHighlightText.decorate', 'timingStore.install', 'corpus.isComplete()',
    'player.currentPositionMs()'])
assert all(t in (base / 'audio/OfflineWordTimingStore.kt').read_text() for t in [
    'ZipInputStream', 'stage.renameTo(active)', 'verseDigest', 'audio.entries', 'SHA-256' if False else 'sha256'])
assert 'WordHighlightText' in (base / 'audio/WordHighlightText.kt').read_text()
assert 'versionName = "0.9.0-dev"' in (root / 'app/build.gradle.kts').read_text()
print('PASS: Phase 8 offline source/module security checks and no INTERNET permission')
print('NOT RUN: Android Studio build, mobile OCR, authentic Quran corpus, reciter audio, timing accuracy')
