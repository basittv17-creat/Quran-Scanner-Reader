# تازہ تصدیق شدہ ترقیاتی حیثیت — 19 نومبر 2026

تازہ جانچ میں کیمرے کی پوری تصویر اسکین کرنے کا اختیار اور ترتیبات، مدد اور تعارف کے انگریزی اور اردو وسائل شامل ہوئے۔ [GitHub Actions 44527849953](https://github.com/basittv17-creat/Quran-Scanner-Reader/actions/runs/44527849953) میں 29 JVM ٹیسٹ کامیاب ہوئے، کوئی ٹیسٹ ناکام یا چھوڑا نہیں گیا۔ Lint میں صفر خامیاں اور 42 تنبیہات ہیں۔ APK یا AAB نہیں بنائی گئی۔ اصل فون پر کیمرے، آڈیو اور رسائی کی جانچ ابھی باقی ہے۔

## حالیہ سورس اور تصدیق

[تازہ ترقیاتی ہینڈ آف](docs/NEXT_STEPS.md) میں لائیو کراپ، گیلری کی بحالی، آڈیو کی سالمیت، اردو اور انگریزی نیویگیشن، OCR جانچ اور باقی شرائط درج ہیں۔ اس کے بعد کی تفصیل سابقہ مراحل کا ریکارڈ ہے۔

- آخری Application Source: `e53b8a2aa4547d58605d38178bd1174800d727c0`۔ Run 44506883081 کامیاب ہے۔
- 20 JVM Tests، صفر failures/errors/skips؛ Lint صفر Errors اور 46 Warnings۔ Python، اصل قرآنی متن، Audio Format، Kotlin اور Resources کی جانچ کامیاب۔ کوئی Test بند یا Warning suppress نہیں کی۔
- بڑی Camera JPEG کو Bitmap بنانے سے پہلے محدود Decode، قابلِ Scroll آیت Review، آڈیو/ٹائمنگ Import کی interruption recovery اور درآمد کے دوران Playback کی حفاظت شامل ہے۔
- تلاوت کے تین محفوظ مقامی موڈ، Replay، Rotation کے بعد آیت/لفظ کا انتخاب بحال؛ Playback خود شروع نہیں ہوتی۔ ہر نئی آیت پر مکمل متن اور حوالہ ملانے کی تازہ تصدیق مطلوب ہے۔ مقامی ڈیٹا Background میں پڑھا جاتا ہے۔
- آٹھ تک الگ آڈیو پیک محفوظ، قاری/پیک کا انتخاب اور غیر فعال پیک حذف کرنے کی سہولت شامل ہے؛ پرانا آڈیو محفوظ رہتا ہے۔ ٹائمنگ کے آڈیو/متن ہیش اور ریکارڈنگ کے دورانیے کی جانچ برقرار ہے۔
- اصل 6,236 آیات، اعراب، ہیش، Attribution اور تمام بنیادی فائلیں محفوظ ہیں۔ نئے اجازت یافتہ آڈیو یا حقیقی OCR نمونے شامل نہیں کیے گئے۔
- Live Camera Crop Mapping، روشنی/Blur کی calibrated رہنمائی، مکمل ترجمہ/Accessibility، باقی Screens کی State Restoration، Dependency byte verification اور Storage/Performance کی مزید جانچ باقی ہے۔ یہ کام صرف بیرونی Assets کی رکاوٹ نہیں ہیں۔
- حقیقی پاکستانی/مدنی صفحات کی جائز reviewed تصاویر، مستند اجازت یافتہ ریکارڈنگ، انسانی جانچے لفظی اوقات، علمی/Display جائزہ اور Android Device QA باقی ہیں۔ APK کی ممانعت کے تحت Package پر منحصر Device QA ملتوی ہے۔

**یہ جزوی ترقیاتی سورس ہے؛ مکمل یا Release-ready ایپ کا دعویٰ نہیں۔ کوئی APK/AAB نہیں بنایا گیا۔**

تفصیلی Checkpoints، CI، Backups اور بحالی کی ہدایات PROGRESS_REPORT.md اور docs/DEVELOPMENT_HANDOFF_2026-11-19.md میں ہیں۔ نیچے سابقہ تاریخ محفوظ ہے۔

---

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

## آخری تصدیق شدہ سورس

Commit `5e5506695b8876e4c76ae13d6eb9c26e22d0015a` کی Run 44482901626 کامیاب ہے: Kotlin، Resources، Python checks، نو JVM Tests، صفر failures/errors/skips؛ Lint صفر Errors اور 39 Warnings۔ کوئی APK/AAB نہیں بنا۔ Constructor Warning درست ہے۔ نئے سورس میں Camera lifecycle/focus/exposure، Gallery Crop، Help/Exit، Settings/privacy اور Insets کی اصلاحات شامل ہیں۔ تمام اصل 59 فائلیں اور Corpus محفوظ ہیں۔ Live Camera Crop، Blur calibration، مکمل Audio Pack management/localization اور فون QA ابھی باقی ہیں؛ مستند آڈیو، لفظی اوقات اور حقیقی صفحات کی benchmark جانچ بھی مطلوب ہے۔
