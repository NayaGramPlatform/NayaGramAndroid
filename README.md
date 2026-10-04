# NayaGram for Android

**NayaGram** is a messaging application powered by the Telegram MTProto protocol, with a strong focus on speed, privacy, and an elegant, distraction-free user experience.

This repository contains the official source code for **NayaGram for Android** (2026 Edition).

---

## Overview & Guidelines

We welcome all developers and contributors to explore the source code. If you are creating your own application based on this repository, please adhere to the following:

1. [**Obtain your own api_id**](https://core.telegram.org/api/obtaining_api_id) from the official Telegram core platform.
2. Ensure your application brand and identity are distinct and clearly communicated to users.
3. Study and follow the [**MTProto Security Guidelines**](https://core.telegram.org/mtproto/security_guidelines) to maintain user privacy and communication security.
4. Publish your modified source code in compliance with the **GNU General Public License (GPL)**.

---

## API & Protocol Documentation

- Telegram API Documentation: https://core.telegram.org/api
- MTProto Protocol Manual: https://core.telegram.org/mtproto

---

## Compilation Guide

### Prerequisites
- **Android Studio** (2024.2+ / 2025.1+ / 2026.1)
- **JDK 17** (OpenJDK 17 recommended)
- **Android SDK** API 34, 35, or 36
- **Android NDK** `27.2.12479018`

### 1. Clone the Source Code
Clone the repository together with all native submodules:

```bash
git clone --recursive https://github.com/NayaGramPlatform/NayaGramAndroid.git NayaGram
```

If you already cloned without the `--recursive` flag, initialize submodules manually:

```bash
cd NayaGram
git submodule update --init --recursive
```

### 2. Configuration & Signing
The project is pre-configured with signing properties in `gradle.properties`:
- Set your signing key credentials (`RELEASE_STORE_FILE`, `RELEASE_KEY_ALIAS`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_PASSWORD`).
- Ensure `google-services.json` is placed in both `TMessagesProj/` and `TMessagesProj_App/` for Firebase Cloud Messaging (FCM).
- Adjust application versioning in `gradle.properties`:
  ```properties
  APP_VERSION_CODE=1000
  APP_VERSION_NAME=1.0.0
  APP_PACKAGE=org.nayagram.platform
  ```

### 3. Open in Android Studio
1. Open Android Studio.
2. Select **Open** (choose the project root folder `NayaGram`, do NOT use "Import").
3. Allow Gradle to perform the initial sync.

---

## Build Tasks

You can compile APKs and App Bundles directly from the command line:

### Debug APK (For development and testing)
```bash
./gradlew :TMessagesProj_App:assembleAfatDebug
```
*Output: `TMessagesProj_App/build/outputs/apk/afat/debug/`*

### Release APK (Standalone Universal APK)
```bash
./gradlew :TMessagesProj_App:assembleAfatRelease
```
*Output: `TMessagesProj_App/build/outputs/apk/afat/release/`*

### Google Play Release Bundle (AAB)
```bash
./gradlew :TMessagesProj_App:bundleBundleAfatRelease
```
*Output: `TMessagesProj_App/build/outputs/bundle/bundleAfatRelease/`*

---

## Security & Privacy

Security and data protection are fundamental principles of NayaGram. 
If you identify any security vulnerability or issue, please responsibly disclose it to the NayaGram security team.

---

## License

NayaGram for Android is licensed under the **GNU General Public License v2.0 or later**.

For the original Telegram source code and documentation, refer to [README_TELEGRAM.md](README_TELEGRAM.md).
