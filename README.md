# 💎 NayaGram for Android

<p align="center">
  <img src="https://raw.githubusercontent.com/NayaGramPlatform/NayaGramAndroid/master/TMessagesProj/src/main/res/drawable-xxhdpi/ic_launcher.png" width="100" height="100" alt="NayaGram Logo" />
</p>

<p align="center">
  <b>NayaGram</b> is a messaging app with a focus on speed, privacy, and simplicity. It’s superfast, powerful, and secure.
</p>

<p align="center">
  <a href="https://t.me/NayaGramPro"><img src="https://img.shields.io/badge/Telegram-@NayaGramPro-2CA5E0?style=flat-square&logo=telegram&logoColor=white" alt="Telegram"/></a>
  <a href="https://x.com/NayaGramPro"><img src="https://img.shields.io/badge/Twitter%20%2F%20X-@NayaGramPro-000000?style=flat-square&logo=x&logoColor=white" alt="Twitter"/></a>
  <a href="https://youtube.com/@NayaGramPro"><img src="https://img.shields.io/badge/YouTube-@NayaGramPro-FF0000?style=flat-square&logo=youtube&logoColor=white" alt="YouTube"/></a>
  <a href="https://facebook.com/NayaGramPro"><img src="https://img.shields.io/badge/Facebook-@NayaGramPro-1877F2?style=flat-square&logo=facebook&logoColor=white" alt="Facebook"/></a>
  <a href="mailto:support.nayagram@gmail.com"><img src="https://img.shields.io/badge/Email-support.nayagram@gmail.com-D14836?style=flat-square&logo=gmail&logoColor=white" alt="Support Email"/></a>
</p>

This repository contains the source code for the official [NayaGram App for Android](https://github.com/NayaGramPlatform/NayaGramAndroid).

---

## Creating your NayaGram Application

We welcome all developers to explore our source code and build applications on our platform.

There are several things we require from **all developers** for the moment:

1. [**Obtain your own api_id**](https://core.telegram.org/api/obtaining_api_id) for your application.
2. Please **do not** use the name Telegram for your app — or make sure your users understand that it is an independent client.
3. Kindly **do not** use the standard Telegram logo (white paper plane in a blue circle) as your app's logo.
4. Please study the [**security guidelines**](https://core.telegram.org/mtproto/security_guidelines) and take good care of your users' data and privacy.
5. Please remember to publish **your** code too in order to comply with the licenses.

### API & Protocol Documentation
- Telegram API manuals: https://core.telegram.org/api
- MTProto protocol manuals: https://core.telegram.org/mtproto

---

## Compilation Guide

You will require Android Studio 2025.1.4, Android NDK 27.2.12479018, Android SDK 36, and JDK 17.

1. Clone the NayaGram source code with its submodules:
   ```bash
   git clone --recursive --shallow-submodules https://github.com/NayaGramPlatform/NayaGramAndroid.git NayaGram
   ```
   In case you forgot the `--recursive` flag, change to the `NayaGram` directory and run:
   ```bash
   git submodule init && git submodule update --init --recursive --depth=1
   ```

2. Copy your keystore (`nayagrampro.jks` or `release.keystore`) into the root directory or `TMessagesProj/config`.

3. Fill out `RELEASE_KEY_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_STORE_PASSWORD` in `gradle.properties` to access your keystore.

4. Place your `google-services.json` inside `TMessagesProj/` and `TMessagesProj_App/` for Google Play Services and push notifications.

5. Open the project in Android Studio (note that it should be **opened**, NOT imported).

6. You are ready to compile NayaGram.

---

## Building from Terminal

### Debug APK (Quick local testing)
```bash
./gradlew :TMessagesProj_App:assembleAfatDebug
```

### Release APK (Standalone universal APK)
```bash
./gradlew :TMessagesProj_App:assembleAfatRelease
```

### Play Store Release Bundle (AAB)
```bash
./gradlew :TMessagesProj_App:bundleBundleAfatRelease
```

---

## Official Channels & Community

Stay connected with our global community across all official channels:

- 📢 **Telegram Channel & Group:** [@NayaGramPro](https://t.me/NayaGramPro)
- 🐦 **Twitter (X):** [@NayaGramPro](https://x.com/NayaGramPro)
- 🎥 **YouTube:** [@NayaGramPro](https://youtube.com/@NayaGramPro)
- 👥 **Facebook:** [@NayaGramPro](https://facebook.com/NayaGramPro)
- ✉️ **Support Email:** [support.nayagram@gmail.com](mailto:support.nayagram@gmail.com)

---

## License

NayaGram for Android is licensed under the **GNU General Public License v2.0 or later**.

*For the original Telegram Android source documentation, see [README_TELEGRAM.md](README_TELEGRAM.md).*
