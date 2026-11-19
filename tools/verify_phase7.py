#!/usr/bin/env python3
"""Phase 7 checks. NOT a substitute for Android SDK build or Quran audio QA."""
import hashlib
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

root=Path(__file__).resolve().parent.parent
subprocess.run(['python3',str(root/'tools/verify_phase6.py')],check=True)
subprocess.run(['python3',str(root/'tools/test_phase7_packaging.py')],check=True)
base=root/'app/src/main/java/org/quranscan/reader'
jar=root/'tools/.phase7-audio-check.jar'
try:
    r=subprocess.run(['kotlinc',str(base/'core/QuranVerseCounts.kt'),
     str(base/'audio/AudioPackFormat.kt'),str(root/'tools/test_phase7_audio.kt'),
     '-include-runtime','-d',str(jar)],capture_output=True,text=True,timeout=90)
    assert r.returncode==0,r.stderr
    subprocess.run(['java','-jar',str(jar)],check=True,timeout=20)
finally:
    jar.unlink(missing_ok=True)
xml=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
assert all('INTERNET' not in p.attrib.get('{http://schemas.android.com/apk/res/android}name','')
           for p in xml.findall('uses-permission'))
assert any(a.attrib.get('{http://schemas.android.com/apk/res/android}name')=='.RecitationActivity'
           for a in xml.findall('.//activity'))
app=(base/'RecitationActivity.kt').read_text()
store=(base/'audio/OfflineAudioPackStore.kt').read_text()
player=(base/'audio/OfflineAyahPlayer.kt').read_text()
assert all(term in app for term in ['ActivityResultContracts.OpenDocument','playSelected','corpus.isComplete()'])
assert all(term in store for term in ['ZipInputStream','stage.renameTo(active)','SHA-256','FileOutputStream','1500L'])
assert 'android.media.MediaPlayer' in player
assert (root/'app/src/main/assets/quran/verses.jsonl').stat().st_size == 0
assert (root/'app/src/main/assets/audio/README.txt').exists()
print('PASS: Phase 7 source, local pack importer, audio player, no INTERNET permission')
print('NOT RUN: Android APK build, physical playback, religious audio review, full corpus import')
