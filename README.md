<div align="center">

# 💎 NayaGram Messenger for Android
### Official Development Repository (`NayaGramAndroid`)

<img src="https://i.postimg.cc/HxSxRdZF/Naya-Gram-Un-Official-Tele-Gram-Client-Platform.png" width="720" alt="NayaGram Unofficial Telegram Client Platform" />

<p>
  <a href="https://t.me/NayaGramPro"><img src="https://img.shields.io/badge/Telegram-@NayaGramPro-2CA5E0?style=flat-square&logo=telegram&logoColor=white" alt="Telegram"/></a>
  <a href="https://x.com/NayaGramPro"><img src="https://img.shields.io/badge/Twitter%20%2F%20X-@NayaGramPro-000000?style=flat-square&logo=x&logoColor=white" alt="Twitter"/></a>
  <a href="https://youtube.com/@NayaGramPro"><img src="https://img.shields.io/badge/YouTube-@NayaGramPro-FF0000?style=flat-square&logo=youtube&logoColor=white" alt="YouTube"/></a>
  <a href="https://facebook.com/NayaGramPro"><img src="https://img.shields.io/badge/Facebook-@NayaGramPro-1877F2?style=flat-square&logo=facebook&logoColor=white" alt="Facebook"/></a>
  <a href="mailto:support.nayagram@gmail.com"><img src="https://img.shields.io/badge/Email-support.nayagram@gmail.com-D14836?style=flat-square&logo=gmail&logoColor=white" alt="Support Email"/></a>
</p>

</div>

[**NayaGram**](https://t.me/NayaGramPro) is an independent, fast, and privacy-focused messaging client for Android built on the official open-source Telegram Android codebase.

This repository ([**NayaGramPlatform/NayaGramAndroid**](https://github.com/NayaGramPlatform/NayaGramAndroid)) is the official NayaGram Android development repository (public sanitized open-source mirror: [**NayaGramPlatform/NayaGram-Client**](https://github.com/NayaGramPlatform/NayaGram-Client)).

| Property | Value |
| :--- | :--- |
| **Application ID (Google Play)** | `org.nayagram.platform` ([**View on Google Play**](https://play.google.com/store/apps/details?id=org.nayagram.platform)) |
| **Current Version** | `v4.26.75 NG` (**Build `4097`**) |
| **License** | [**GNU General Public License v2.0 or later**](LICENSE) |
| **Privacy Policy** | [**NayaGram Privacy Policy**](https://nayagramplatform.github.io/PrivacyArticleCenter/) |

---

## 📋 Disclosure & Compliance
- **Unofficial Client Notice:** NayaGram is an independent third-party client powered by the Telegram API. It is not affiliated with Telegram FZ-LLC / Telegram Messenger Inc.
- **Build-Time Secrets:** `TELEGRAM_API_ID`, `TELEGRAM_API_HASH`, and release signing credentials are injected at build time via environment variables or CI secrets (`-P` flags) and are never hardcoded in source files.
- **Full Source Parity:** Application source code (`TMessagesProj`, `TMessagesProj_App`, native C++ submodules, and resources) matches the `v4.26.75 NG` (**Build `4097`**) release.

---

## 🛠️ Creating & Building Your NayaGram Application

Developers building from this source code must follow Telegram's API terms and the **GNU GPL v2+** license:

1. **Obtain your own `api_id` and `api_hash`** by logging in at [**https://my.telegram.org**](https://my.telegram.org) (see [**official guide**](https://core.telegram.org/api/obtaining_api_id)).
2. **Do not** use the name *Telegram* for your app — clearly inform users that your application is an independent client.
3. **Do not** reuse Telegram's official logo (white paper plane in a blue circle) as your application icon.
4. Carefully follow Telegram's [**Security Guidelines**](https://core.telegram.org/mtproto/security_guidelines) to protect user data and privacy.
5. Publish **your modified source code** under GNU GPL v2+ to remain compliant with the open-source license.

### 📚 API & Protocol Documentation
- **Telegram API Manuals:** [**https://core.telegram.org/api**](https://core.telegram.org/api)
- **MTProto Protocol Manuals:** [**https://core.telegram.org/mtproto**](https://core.telegram.org/mtproto)
- **API Key Portal:** [**https://my.telegram.org**](https://my.telegram.org)

---

## ⚙️ Compilation Guide

### Prerequisites
- **JDK 17** (Temurin / OpenJDK 17)
- **Android Studio** 2025.1+
- **Android SDK 36**, **Build-Tools `36.0.0`**, **CMake `3.22.1`**, and **Android NDK `27.2.12479018`**

### Step-by-Step Setup

1. **Clone the repository with all native submodules:**
   ```bash
   git clone --recursive --shallow-submodules https://github.com/NayaGramPlatform/NayaGramAndroid.git NayaGram
   cd NayaGram
   ```
   *(If you forgot `--recursive`, run:)*
   ```bash
   git submodule sync --recursive
   git submodule update --init --recursive --depth=1
   ```

2. **Configure your Telegram API credentials:**  
   Register your app at [**https://my.telegram.org**](https://my.telegram.org) to get your `TELEGRAM_API_ID` and `TELEGRAM_API_HASH`, then supply them via environment variables or Gradle `-P` properties (`TMessagesProj/build.gradle` injects them automatically into `BuildConfig`):
   ```bash
   export TELEGRAM_API_ID=YOUR_API_ID
   export TELEGRAM_API_HASH=YOUR_API_HASH
   ```

3. **Configure Firebase & Google Maps (optional for push notifications / map previews):**
   - Replace the `google-services.json` files (in `TMessagesProj_App/`, `TMessagesProj/`, etc.) with your own Firebase Console configuration for `org.nayagram.platform` and `org.nayagram.platform.beta`.
   - Set your Google Maps Android API key for `com.google.android.maps.v2.API_KEY` inside `TMessagesProj/config/*/AndroidManifest*.xml`.

4. **Configure Release Signing (for signed APK / AAB builds):**  
   Supply your keystore path and passwords via environment variables or `-P` flags (never commit keystores or passwords):
   ```bash
   export RELEASE_KEYSTORE_PATH=/path/to/your-release.jks
   export RELEASE_STORE_PASSWORD=your_store_password
   export RELEASE_KEY_PASSWORD=your_key_password
   export RELEASE_KEY_ALIAS=your_key_alias
   ```

5. **Open in Android Studio** using **File → Open** (do **not** use Import) or build via terminal.

---

## 💻 Terminal Build Commands

| Build Target | Command |
| :--- | :--- |
| **Debug APK** | `./gradlew :TMessagesProj_App:assembleAfatDebug` |
| **Universal Release APK** | `./gradlew :TMessagesProj_App:assembleAfatRelease` |
| **Google Play AAB (SDK 24+)** | `./gradlew :TMessagesProj_App:bundleBundleAfat_SDK23Release` |
| **Google Play AAB (Default)** | `./gradlew :TMessagesProj_App:bundleBundleAfatRelease` |

---

## 🌍 Localization & Community

We welcome community contributions for translations, bug fixes, and improvements. Pull requests are welcome, or contact us at [**support.nayagram@gmail.com**](mailto:support.nayagram@gmail.com).

- **Telegram Channel:** [**@NayaGramPro**](https://t.me/NayaGramPro)
- **Twitter / X:** [**@NayaGramPro**](https://x.com/NayaGramPro)
- **YouTube:** [**@NayaGramPro**](https://youtube.com/@NayaGramPro)
- **Facebook:** [**@NayaGramPro**](https://facebook.com/NayaGramPro)
- **Support Email:** [**support.nayagram@gmail.com**](mailto:support.nayagram@gmail.com)

---

## 📜 License & Trademarks
- Licensed under the [**GNU General Public License v2.0 or later**](LICENSE) *(see also [README_TELEGRAM.md](README_TELEGRAM.md))*.
- **Copyright © 2026 𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦.** All rights reserved.
- **Telegram** is a registered trademark of Telegram FZ-LLC / Telegram Messenger Inc.

<p align="center">
  <sub>Built with dedication, passion, and integrity by <b>𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦</b>.</sub>
</p>
