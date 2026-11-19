# مرحلہ 9: GitHub کی تیاری اور Build-only CI

- یہ سورس GitHub میں رکھنے کے لیے تیار کیا گیا ہے۔ نئی repository اس محیط میں ابھی تخلیق نہیں ہو سکی؛ اس اختیار کا کنکشن دستیاب نہیں۔
- نئی private repository کی تجویز: `basittv17-creat/Quran-Scanner-Reader`۔ اگر اوپن سورس کرنا ہو تو بعد میں واضح منظوری دیں۔
- `.github/workflows/source-validation.yml` صرف Kotlin compile، Android resource processing اور Python checks چلاتا ہے۔ **assemble, bundle, APK, AAB, upload-artifact, release, signing** میں سے کوئی کام نہیں۔
- GitHub Actions پر پہلی حقیقی compile کے دوران ممکنہ Gradle/API مسائل سامنے آئیں گے؛ کامیابی پہلے سے ضمانت نہیں۔
- موجودہ Quran verses.jsonl خالی ہے؛ کسی متن کو تصدیق شدہ نہ سمجھیں۔ کوئی مستند MP3 یا لفظی ٹائمنگ فائل شامل نہیں۔
- GitHub repo بننے کے بعد `tools/push_to_new_github_repo.sh` چلائیں۔ GitHub CLI میں اس اکاؤنٹ سے اجازت درکار ہوگی۔

## تازہ تصحیح — 19 نومبر 2026

اوپر تاریخی حیثیت ہے۔ repository اب public اور connected ہے، main اپڈیٹ ہو چکی ہے، مکمل Tanzil publisher corpus شامل ہے، اور حقیقی CI compilation/resources/tests/lint run کامیاب ہے۔ موجودہ مستند تفصیل اور links کے لیے `../PROGRESS_REPORT.md` دیکھیں۔ کوئی APK/AAB نہیں بنایا گیا۔
