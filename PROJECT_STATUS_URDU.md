# قرآن اسکین ریڈر — موجودہ حیثیت

19 نومبر 2026۔ یہ قابلِ ترقی سورس ہے؛ مکمل، حقیقی فون پر آزمودہ ایپ نہیں۔

- GitHub main پر source محفوظ اور CI compilation کی خرابیاں درست ہیں۔
- Tanzil کے اصل سرور سے تمام 6,236 آیات، اصل الفاظ/اعراب، لائسنس نوٹس اور SHA-256 سمیت شامل ہیں۔ الگ علمی اور فون پر نمائش کی جانچ باقی ہے۔
- Kotlin compilation، Android resources، سات JVM unit tests اور Python validations پاس ہیں۔ آخری source run 44466055230 میں lint errors صفر اور warnings 36 ہیں؛ سات unit tests میں کوئی failure یا skipped test نہیں۔
- Native splash، sidebar، settings، CameraX، gallery orientation اور محفوظ آف لائن WebView کا سورس موجود ہے؛ حقیقی فون پر QA باقی ہے۔
- مستند، redistribution کی اجازت والی مکمل آڈیو اور انسانی طور پر جانچے گئے لفظی اوقات شامل نہیں۔ موجودہ importer/player/highlighting سورس ان کا متبادل نہیں۔
- پاکستانی 15 لائن یا مدنی طباعت کی OCR درستگی کی ضمانت نہیں دی گئی۔ مشکوک OCR پر خودکار تلاوت نہیں ہوتی۔
- سورس ZIP، Drive backups اور Gmail links دستیاب ہیں۔ تفصیل، ہر مرحلے کی حیثیت اور commit/run links: `PROGRESS_REPORT.md`۔

**کوئی APK یا AAB نہیں بنایا گیا۔**

## تازہ ترقیاتی چیک پوائنٹ

کیمرہ foreground recovery، Tap focus، Exposure controls، Auto Scan preference اور پرانے OCR نتائج کو روکنے کی اصلاحات شامل ہیں۔ Runs 44481527823 اور 44481771624 کامیاب: آٹھ JVM Tests، صفر failures، Lint صفر Errors اور 39 Warnings؛ کوئی APK/AAB نہیں۔ Native Help/Exit، Settings، Insets اور Gallery navigation کی اگلی اصلاحات الگ CI کی منتظر ہیں۔ حقیقی قرآن صفحات، مستند اجازت یافتہ آڈیو، انسانی طور پر جانچے اوقات، علمی جائزہ اور Device QA ابھی باقی ہیں۔ مکمل ایپ کا دعویٰ نہیں۔

Gallery میں OCR سے پہلے منتخب حصے کو Crop کرنے کا سورس شامل ہے۔ Run 44482560224 کامیاب: نو JVM Tests، صفر failures/errors/skips، Lint صفر Errors/40 Warnings۔ آخری follow-up میں Custom View constructor، کم camera still resolution کی درخواست اور concurrent OCR model extraction کی حفاظت شامل ہے؛ اس کی CI الگ ہوگی۔ اسکرین، اشاروں، Memory اور حقیقی OCR accuracy کی فون پر تصدیق باقی ہے۔
