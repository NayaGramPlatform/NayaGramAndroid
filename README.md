# 💎 NayaGram messenger for Android

<p align="center">
  <img src="https://raw.githubusercontent.com/NayaGramPlatform/NayaGramAndroid/master/TMessagesProj/src/main/res/drawable-xxhdpi/ic_launcher.png" width="100" height="100" alt="NayaGram Logo" />
</p>

<p align="center">
  <a href="https://t.me/NayaGramPro"><img src="https://img.shields.io/badge/Telegram-@NayaGramPro-2CA5E0?style=flat-square&logo=telegram&logoColor=white" alt="Telegram"/></a>
  <a href="https://x.com/NayaGramPro"><img src="https://img.shields.io/badge/Twitter%20%2F%20X-@NayaGramPro-000000?style=flat-square&logo=x&logoColor=white" alt="Twitter"/></a>
  <a href="https://youtube.com/@NayaGramPro"><img src="https://img.shields.io/badge/YouTube-@NayaGramPro-FF0000?style=flat-square&logo=youtube&logoColor=white" alt="YouTube"/></a>
  <a href="https://facebook.com/NayaGramPro"><img src="https://img.shields.io/badge/Facebook-@NayaGramPro-1877F2?style=flat-square&logo=facebook&logoColor=white" alt="Facebook"/></a>
  <a href="mailto:support.nayagram@gmail.com"><img src="https://img.shields.io/badge/Email-support.nayagram@gmail.com-D14836?style=flat-square&logo=gmail&logoColor=white" alt="Support Email"/></a>
</p>

[NayaGram](https://t.me/NayaGramPro) is a messaging app with a focus on speed and security. It’s superfast, simple and free.

This repo contains the official source code for [NayaGram App for Android](https://github.com/NayaGramPlatform/NayaGramAndroid).

## Creating your NayaGram Application

We welcome all developers to use our API and source code to create applications on our platform.
There are several things we require from **all developers** for the moment.

1. [**Obtain your own api_id**](https://core.telegram.org/api/obtaining_api_id) for your application.
2. Please **do not** use the name Telegram for your app — or make sure your users understand that it is unofficial.
3. Kindly **do not** use our standard logo (white paper plane in a blue circle) as your app's logo.
4. Please study our [**security guidelines**](https://core.telegram.org/mtproto/security_guidelines) and take good care of your users' data and privacy.
5. Please remember to publish **your** code too in order to comply with the licences.

### API, Protocol documentation
Telegram API manuals: https://core.telegram.org/api  
MTproto protocol manuals: https://core.telegram.org/mtproto  

### Compilation Guide
**Note**: In order to support [reproducible builds](https://core.telegram.org/reproducible-builds), this repo contains release.keystore, google-services.json and filled variables inside BuildVars.java. Before publishing your own APKs please make sure to replace all these files with your own.

You will require Android Studio 2025.1.4, Android NDK 27.2.12479018 and Android SDK 36.

1. Clone the NayaGram source code with its submodules:
   ```bash
   git clone --recursive --shallow-submodules https://github.com/NayaGramPlatform/NayaGramAndroid.git NayaGram
   ```
   In case you forgot the `--recursive` flag, change to the `NayaGram` directory and run:
   ```bash
   git submodule init && git submodule update --init --recursive --depth=1
   ```

2. Copy your release.keystore into TMessagesProj/config

3. Fill out RELEASE_KEY_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_STORE_PASSWORD in gradle.properties to access your release.keystore

4. Go to https://console.firebase.google.com/, create two android apps with application IDs `org.nayagram.platform` and `org.nayagram.platform.beta`, turn on firebase messaging and download google-services.json, which should be copied to the same folder as TMessagesProj.

5. Open the project in the Studio (note that it should be opened, NOT imported).

6. Fill out values in TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java – there’s a link for each of the variables showing where and which data to obtain.

7. You are ready to compile NayaGram.

---

### Terminal Build Tasks

- **Debug APK:** `./gradlew :TMessagesProj_App:assembleAfatDebug`
- **Release APK:** `./gradlew :TMessagesProj_App:assembleAfatRelease`
- **Play Store Release AAB:** `./gradlew :TMessagesProj_App:bundleBundleAfatRelease`

---

### Localization
We welcome community contributions for localization and translations. Please submit pull requests or reach out at [support.nayagram@gmail.com](mailto:support.nayagram@gmail.com).

### Community & Channels
- **Telegram:** [@NayaGramPro](https://t.me/NayaGramPro)
- **Twitter / X:** [@NayaGramPro](https://x.com/NayaGramPro)
- **YouTube:** [@NayaGramPro](https://youtube.com/@NayaGramPro)
- **Facebook:** [@NayaGramPro](https://facebook.com/NayaGramPro)
- **Email:** support.nayagram@gmail.com

---

### License
NayaGram for Android is licensed under the **GNU General Public License v2.0 or later**.  
*For original Telegram documentation, see [README_TELEGRAM.md](README_TELEGRAM.md).*

---

## 🏛️ About 𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦

**𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦** is an independent technology initiative dedicated to delivering next-generation, high-performance, and privacy-respecting communication tools. We believe that digital privacy, freedom of speech, and high-speed global connectivity are fundamental rights for every user worldwide.

### ⚖️ Copyright & Trademarks
- **Copyright © 2026 𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦.** All rights reserved.
- **Telegram** is a registered trademark of Telegram FZ-LLC / Telegram Messenger Inc.
- **NayaGram** is an independent client application built on the official open-source Telegram Android codebase under the terms of the **GNU General Public License (GPL) v2.0 or later**.
- For brand guidelines, media inquiries, or official partnerships, contact us at [support.nayagram@gmail.com](mailto:support.nayagram@gmail.com).

<p align="center">
  <sub>Built with dedication, passion, and integrity by <b>𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦</b>.</sub>
</p>
