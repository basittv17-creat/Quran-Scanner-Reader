# مرحلہ 5 — آف لائن عربی OCR کا ابتدائی انجن

**تیار:** Tesseract4Android 4.9.0 کے ذریعے Kotlin OCR رابطہ؛ Apache-2.0 Arabic `ara.traineddata` ماڈل ایپ کے اندر شامل؛ گیلری/کیمرے سے حاصل کردہ تصویر کو بیک گراؤنڈ تھریڈ میں پڑھنا؛ OCR کا خام غیر مصدقہ نتیجہ دکھانا؛ اور مکمل مستند ڈیٹابیس موجود ہونے پر صرف آیات کی ممکنہ تجاویز دکھانا۔

**موجودہ حد:** یہ عام عربی OCR ماڈل ہے، خصوصی قرآنی ماڈل نہیں؛ پاکستانی 15 لائن اور سعودی مدنی متن پر نتائج کی جانچ نہیں ہوئی۔ ہر زیر زبر درست ہونے کا دعویٰ نہیں کیا جا سکتا۔ OCR کا متن کسی بھی صورت مستند متن نہیں سمجھا جائے گا۔

**مرحلہ 4 نامکمل ہے:** مکمل 6,236 آیات کی مستند فائل ابھی اس پیکیج میں نہیں؛ محفوظ امپورٹر موجود ہے لیکن دیتابیس خالی ہے۔ لہٰذا آیت کی تصدیق اور تلاوت ابھی بند ہیں۔

**فائلیں:**
- `app/src/main/java/org/quranscan/reader/ocr/OfflineArabicOcr.kt`: مکمل آف لائن انجن رابطہ، چار تک شناختی کوششیں، اعلیٰ OCR اعتماد پر جلد واپسی، ماڈل کی SHA-256 جانچ۔
- `app/src/main/java/org/quranscan/reader/core/VerseCandidates.kt`: محتاط چار-لفظی match candidate، کم از کم 75% مسلسل لفظی مطابقت؛ خودکار تلاوت نہیں۔
- `app/src/main/assets/tessdata/ara.traineddata`: عربی ماڈل (تصدیق شدہ SHA-256)۔
- `tools/test_phase5_core.kt`: Kotlin کے خالص matching logic کے 5 test checks۔
- `tools/verify_phase5.py`: مقامی model/offline integrity checks۔
- `tools/fixtures/printed_arabic_smoke.png`: عام عربی متن کا مصنوعی smoke test، قرآن کے رسم الخط کی تصدیق **نہیں**۔

**اہم:** Tesseract JNI/Android لائبریری Gradle سے **بلڈ کے وقت** حاصل ہوگی؛ ایپ کے چلتے وقت انٹرنیٹ یا model download کی ضرورت نہیں۔ ایک بار APK بلڈ ہونے پر OCR انجن بند نیٹ ورک میں کام کرنے کے لیے ترتیب دیا گیا ہے، لیکن اصل Android بلڈ اور موبائل ٹیسٹ ابھی نہیں ہوئے۔

**اگلا عملی مرحلہ:** CameraX (تصویر کا صحیح resolution، permission، focus/lighting، throttling)، پھر اردو/مدنی حقیقی مطبوعہ صفحات پر benchmark، canonical corpus review اور verified ayah/word audio۔
