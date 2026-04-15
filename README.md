# 🔐 CryptoSafe

*A lightweight, offline-first Android app for military-grade text encryption.*

[![GitHub release](https://img.shields.io/badge/release-v1.0.1-brightgreen)](https://github.com/yourusername/CryptoSafe/releases)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-green)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-blue)](https://developer.android.com/jetpack/compose)

---

## 📖 Table of Contents
- [✨ Features](#-features)
- [📱 Screenshots](#-screenshots)
- [🛠️ Tech Stack](#-tech-stack)
- [🚀 How to Build](#-how-to-build)
- [📦 Download](#-download)
- [🔒 Security & Privacy](#-security--privacy)
- [📄 License](#-license)
- [👨‍💻 Developer](#-developer)
- [🇸🇦 العربية](#-arabic-section)

---

## ✨ Features

| Feature | Description |
|--------|-------------|
| 🔒 **Strong Encryption** | AES‑256‑GCM with PBKDF2 key derivation (310,000 iterations). |
| 📴 **100% Offline** | No internet permission. Your data never leaves your device. |
| 👁️ **Screenshot Blocked** | Prevents accidental leaks via screenshots or screen recording. |
| 🧹 **Zero Storage** | No logs, no cache, no database. Inputs wiped from memory immediately. |
| 🌓 **Modern UI** | Sleek Material 3 design with dark theme and smooth animations. |
| 🌐 **Bilingual** | Full English and Arabic interface support. |
| 📋 **Copy & Clear** | One‑tap copy to clipboard and quick clear of sensitive fields. |

---

## 📱 Screenshots

| Home | Encrypt | Decrypt | About |
|:----:|:-------:|:-------:|:-----:|
| ![Home](screenshots/home.png) | ![Encrypt](screenshots/encrypt.png) | ![Decrypt](screenshots/decrypt.png) | ![About](screenshots/about.png) |

*Place actual screenshots in `/screenshots` folder and replace paths above.*

---

## 🛠️ Tech Stack

- **Language**: Kotlin 2.1.0
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Build System**: Gradle 8.11.1 (KTS)
- **CI/CD**: GitHub Actions
- **Min SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 15 (API 35)

---

## 🚀 How to Build

### Using GitHub Actions (recommended)
1. Fork or clone this repository.
2. Go to **Actions** tab and run the `Build Signed Release APK` workflow.
3. Download the generated `CryptoSafe-Release.zip` artifact.
4. Extract and install `app-release.apk` on your device.

### Local Build
```bash
git clone https://github.com/yourusername/CryptoSafe.git
cd CryptoSafe
./gradlew assembleRelease
The APK will be located at app/build/outputs/apk/release/.

Note: Release builds require a keystore. The CI script generates one automatically; for local builds, create your own or use assembleDebug.

📦 Download
Get the latest stable APK from the Releases page.

Current Version: v1.0.1

Size: ~4 MB

SHA‑256: (provided in release assets)

🔒 Security & Privacy
Aspect	Implementation
Encryption Algorithm	AES‑256 in GCM mode (authenticated encryption).
Key Derivation	PBKDF2 with HMAC‑SHA256, 310k iterations, 16‑byte salt.
Password Handling	CharArray used and zeroed after each operation.
Data at Rest	Nothing stored. All operations happen in RAM.
Network	No INTERNET permission declared.
Backup	android:allowBackup="false" prevents cloud backup of app data.
Screenshot	FLAG_SECURE blocks screenshots and screen recording.
Warning: If you lose your password, encrypted data cannot be recovered. Keep backups of both encrypted text and passwords in a safe place.

📄 License
This project is licensed under the MIT License – see the LICENSE file for details.

👨‍💻 Developer
CryptoSafe is maintained by [Your Name / GitHub Actions CI].

🐙 GitHub: @yourusername

📧 Email: your.email@example.com (optional)

Contributions, issues, and feature requests are welcome!
Feel free to check issues page.

🇸🇦 القسم العربي
🔐 مُشفّر آمن
تطبيق أندرويد خفيف يعمل بدون إنترنت لتشفير النصوص بتقنية عسكرية.

✨ المميزات
الميزة	الوصف
🔒 تشفير قوي	AES‑256‑GCM مع اشتقاق المفتاح PBKDF2 (310,000 تكرار).
📴 يعمل 100% بدون إنترنت	لا يحتوي على إذن INTERNET. بياناتك لا تغادر جهازك أبداً.
👁️ منع لقطة الشاشة	يحمي من التسرب العرضي عبر لقطات الشاشة أو تسجيل الشاشة.
🧹 لا تخزين نهائياً	لا سجلات، لا ذاكرة مخبأة، لا قاعدة بيانات. يتم مسح المدخلات من الذاكرة فوراً.
🌓 واجهة عصرية	تصميم Material 3 داكن مع حركات سلسة.
🌐 ثنائي اللغة	دعم كامل للغة العربية والإنجليزية.
📋 نسخ ومسح	نسخة بنقرة واحدة ومسح سريع للحقول الحساسة.
📱 لقطات الشاشة
الرئيسية	تشفير	فك تشفير	حول
https://screenshots/home.png	https://screenshots/encrypt.png	https://screenshots/decrypt.png	https://screenshots/about.png
ضع لقطات شاشة حقيقية في مجلد /screenshots واستبدل المسارات أعلاه.

🛠️ التقنيات المستخدمة
لغة البرمجة: Kotlin 2.1.0

واجهة المستخدم: Jetpack Compose (Material 3)

نظام البناء: Gradle 8.11.1 (KTS)

التكامل المستمر: GitHub Actions

الحد الأدنى للإصدار: Android 7.0 (API 24)

الإصدار المستهدف: Android 15 (API 35)

🚀 طريقة البناء
باستخدام GitHub Actions (موصى به)
قم بعمل fork أو clone للمستودع.

اذهب إلى تبويب Actions وشغّل Build Signed Release APK.

حمّل ملف CryptoSafe-Release.zip من الأدوات المُنتجة.

فك الضغط وثبّت app-release.apk على جهازك.

بناء محلي
bash
git clone https://github.com/yourusername/CryptoSafe.git
cd CryptoSafe
./gradlew assembleRelease
سيكون ملف APK في المسار app/build/outputs/apk/release/.

ملاحظة: تحتاج نسخة release إلى keystore. سكربت CI يولّد واحداً تلقائياً؛ للبناء المحلي قم بإنشاء keystore خاص بك أو استخدم assembleDebug.

📦 تحميل
احصل على أحدث نسخة مستقرة من صفحة Releases.

الإصدار الحالي: v1.0.1

الحجم: ~4 MB

بصمة SHA‑256: (مرفقة مع ملفات الإصدار)

🔒 الأمان والخصوصية
الجانب	التطبيق
خوارزمية التشفير	AES‑256 في وضع GCM (تشفير موثوق).
اشتقاق المفتاح	PBKDF2 مع HMAC‑SHA256، 310 ألف تكرار، ملح 16 بايت.
التعامل مع كلمة المرور	تستخدم CharArray ويتم تصفيرها بعد كل عملية.
البيانات المخزنة	لا شيء. جميع العمليات تتم في الذاكرة العشوائية (RAM).
الشبكة	لا يوجد إذن INTERNET.
النسخ الاحتياطي	android:allowBackup="false" يمنع النسخ الاحتياطي السحابي.
لقطة الشاشة	FLAG_SECURE يمنع التقاط الشاشة وتسجيلها.
تحذير: في حال فقدان كلمة المرور، لا يمكن استعادة البيانات المشفرة أبداً. احتفظ بنسخ احتياطية من النص المشفر وكلمات المرور في مكان آمن.

📄 الترخيص
هذا المشروع مرخص تحت MIT License – راجع ملف LICENSE للتفاصيل.

👨‍💻 المطور
CryptoSafe يصونه [اسمك / GitHub Actions CI].

🐙 GitHub: @yourusername

📧 البريد الإلكتروني: your.email@example.com (اختياري)

المساهمات والاقتراحات والتبليغ عن المشكلات مرحب بها!
تفضل بزيارة صفحة المشكلات.

<p align="center"> <sub>Made with ❤️ for privacy</sub> </p> ```

