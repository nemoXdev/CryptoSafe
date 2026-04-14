# 🔐 CryptoSafe / مُشفّر آمن

## 🇬🇧 English
A lightweight, offline Android app for text encryption and decryption using AES-256-GCM and PBKDF2. Built with Kotlin & Jetpack Compose. No internet permission, no servers, no tracking. Your data stays on your device.

### ✨ Features
- **100% Offline**: Works without any network connection.
- **Strong Cryptography**: AES-256-GCM + PBKDF2 (310k iterations).
- **Zero Tracking**: No analytics, no cloud, no logs.
- **Open Source**: Fully transparent. Audit it, modify it, share it.
- **Bilingual UI**: English & Arabic support out of the box.

### 🛠️ How to Build (GitHub Actions)
1. Create a public repository on GitHub.
2. Upload these files keeping the exact folder structure.
3. Go to the `Actions` tab. The `Build & Sign Release APK` workflow will run automatically.
4. Once green, download `CryptoSafe-Release.zip` from the artifacts.
5. Extract and install `app-release.apk` on any Android 7.0+ device.

### 🔒 Security & Privacy Notes
- **Signature**: The CI pipeline generates a fresh signing key per run. It proves the APK wasn't tampered with after build. It contains zero personal information.
- **Encryption Flow**: Plaintext → PBKDF2 Key → AES-GCM → Base64. Only the Base64 string and your password are needed to recover data.
- **Data Storage**: The app stores nothing. Inputs are wiped from memory immediately after processing.
- **If you lose your password, the data is unrecoverable.** This is by design.

### 📄 License
MIT. Use it freely.

---

## 🇸🇦 العربية
تطبيق أندرويد خفيف ويعمل بدون إنترنت لتشفير وفك تشفير النصوص باستخدام خوارزمية AES-256-GCM مع PBKDF2. مبني بـ Kotlin و Jetpack Compose. لا يحتوي على إذن إنترنت، لا خوادم، ولا تتبع. بياناتك تبقى على جهازك فقط.

### ✨ المميزات
- **يعمل 100% بدون إنترنت**: لا يحتاج اتصال شبكة نهائياً.
- **تشفير قوي**: AES-256-GCM + PBKDF2 (310 ألف تكرار).
- **صفر تتبع**: لا إحصائيات، لا سحابة، لا سجلات.
- **مفتوح المصدر**: شفاف بالكامل. افحصه، عدّله، شاركه.
- **واجهة ثنائية اللغة**: يدعم العربية والإنجليزية تلقائياً.

### 🛠️ طريقة البناء (عبر GitHub)
1. أنشئ مستودعاً عاماً على GitHub.
2. ارفع الملفات بنفس ترتيب المجلدات الظاهر أعلاه.
3. انتقل إلى تبويب `Actions`. ستعمل خطوة البناء تلقائياً.
4. عند اكتمالها باللون الأخضر، حمّل `CryptoSafe-Release.zip`.
5. استخرج الملف وثبّت `app-release.apk` على أي جهاز أندرويد 7.0 أو أحدث.

### 🔒 ملاحظات الأمان والخصوصية
- **التوقيع الرقمي**: ينشئ النظام مفتاح توقيع مؤقتاً أثناء البناء لإثبات أن الملف لم يُعدّل. لا يحتوي على أي بيانات شخصية.
- **آلية التشفير**: النص الأصلي → مفتاح مشتق → AES-GCM → نص Base64. تحتاج فقط النص المشفر ونفس كلمة المرور لفك التشفير.
- **حفظ البيانات**: التطبيق لا يحفظ أي شيء. يُمسح النص من الذاكرة فور المعالجة.
- **إذا نسيت كلمة المرور، لا يمكن استعادة النص أبداً.** هذا مقصود لضمان الأمان الكامل.

### 📄 الترخيص
MIT. استخدمه بحرية.
