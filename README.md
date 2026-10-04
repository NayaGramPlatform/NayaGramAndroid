# 💎 NayaGram for Android

<p align="center">
  <img src="https://raw.githubusercontent.com/NayaGramPlatform/NayaGramAndroid/master/TMessagesProj/src/main/res/drawable-xxhdpi/ic_launcher.png" width="100" height="100" alt="NayaGram Logo" />
</p>

<p align="center">
  <b>NayaGram</b> is a messaging app with a focus on speed, privacy, and simplicity. It’s superfast, powerful, and secure.
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

## Community & Channels

- Official Telegram Channel: [@NayaGram](https://t.me/NayaGram)
- Support & Community: [@NayaGramSupport](https://t.me/NayaGramSupport)
- GitHub Repository: [NayaGramPlatform/NayaGramAndroid](https://github.com/NayaGramPlatform/NayaGramAndroid)

---

## License

NayaGram for Android is licensed under the **GNU General Public License v2.0 or later**.

*For the original Telegram Android source documentation, see [README_TELEGRAM.md](README_TELEGRAM.md).*
