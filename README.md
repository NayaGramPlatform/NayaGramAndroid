# 💎 NayaGram for Android

<p align="center">
  <img src="https://raw.githubusercontent.com/NayaGramPlatform/NayaGramAndroid/master/TMessagesProj/src/main/res/drawable-xxhdpi/ic_launcher.png" width="110" alt="NayaGram Logo" />
</p>

<p align="center">
  <b>A modern, high-speed, and privacy-focused messaging platform powered by Telegram MTProto.</b>
</p>

<p align="center">
  <a href="https://github.com/NayaGramPlatform/NayaGramAndroid/actions"><img src="https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square" alt="Build Status"/></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Platform Android"/></a>
  <a href="https://github.com/NayaGramPlatform/NayaGramAndroid/releases"><img src="https://img.shields.io/badge/Version-1.0.0%20(1000)-blue?style=flat-square" alt="Version"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv2%20%2F%20GPLv3-yellow?style=flat-square" alt="License"/></a>
</p>

---

## 📖 Overview

**NayaGram** is an enhanced, highly-optimized messaging client built on top of the world-class Telegram Android open-source ecosystem. Designed with simplicity, elegant aesthetics, and pure performance in mind, NayaGram provides a smooth and distraction-free communication experience.

> ℹ️ *For the original official Telegram documentation and reproducible build instructions, please refer to [README_TELEGRAM.md](README_TELEGRAM.md).*

---

## ✨ Key Features

- ⚡ **Ultra-Fast & Lightweight:** Optimized memory handling and instant message synchronization via Telegram MTProto API.
- 🛡️ **Privacy & Security First:** End-to-end secret chats, advanced passcodes, and standard zero-tracking compliance.
- 🎨 **Modern Clean UI:** Beautiful Material Design 3 inspired elements, custom color palettes, and intuitive typography.
- 🚫 **Distraction-Free Experience:** 100% ad-free core chatting experience with zero annoying interruptions.
- 🗂️ **Organized Chat Tabs:** Seamless categorization for personal chats, groups, channels, and bots.
- 🌐 **Global Multi-Account Support:** Switch between multiple profiles effortlessly without logging out.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin & Java (Java 17 compatible)
- **Native Core:** C++ / NDK (`27.2.12479018`) with BoringSSL & LibYUV
- **Build System:** Gradle with custom Android Gradle Plugin configurations
- **Target SDK:** Android 14+ (API 34 / 35 / 36)
- **Minimum SDK:** Android 5.0 (API 21)
- **Protocol:** Telegram MTProto 2.0

---

## 🚀 Building from Source

### Prerequisites
1. **JDK 17** (OpenJDK 17 recommended)
2. **Android Studio Ladybug (2024.2+)** or Android Command-line Tools
3. **Android SDK** (API 34 / 35 installed)
4. **Android NDK** (`27.2.12479018`)

### Clone the Repository
```bash
git clone https://github.com/NayaGramPlatform/NayaGramAndroid.git
cd NayaGramAndroid
git submodule update --init --recursive
```

### Build Commands

#### 1. Build Debug APK (For rapid local testing):
```bash
./gradlew :TMessagesProj_App:assembleAfatDebug
```
*Output location:* `TMessagesProj_App/build/outputs/apk/afat/debug/`

#### 2. Build Release APK (Standalone Universal):
```bash
./gradlew :TMessagesProj_App:assembleAfatRelease
```
*Output location:* `TMessagesProj_App/build/outputs/apk/afat/release/`

#### 3. Build Google Play Store Release Bundle (AAB):
```bash
./gradlew :TMessagesProj_App:bundleBundleAfatRelease
```
*Output location:* `TMessagesProj_App/build/outputs/bundle/bundleAfatRelease/`

---

## 🔑 App Signing & Keystore

The project is pre-configured with secure keystore properties via `gradle.properties`:
- **File:** `nayagrampro.jks`
- **Key Alias:** `nayagrampro_key`
- **Version Code:** `1000`
- **Version Name:** `1.0.0`

---

## 📜 License & Compliance

NayaGram Android is distributed under the GNU General Public License (GPL) v2 or later, following the official Telegram FOSS guidelines.

- Original Source: [Telegram for Android](https://github.com/DrKLO/Telegram)
- Original Telegram README: [README_TELEGRAM.md](README_TELEGRAM.md)

---

<p align="center">
  Crafted with ❤️ by the <b>NayaGram Team</b>
</p>
