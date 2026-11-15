# 🚀 NayaGram Android — Developer & Contributor Build Guide

Complete instructions for building **NayaGram Android** (`org.nayagram.platform`, **v4.26.75 NG / Build 4097**) from [**NayaGramPlatform/NayaGramAndroid**](https://github.com/NayaGramPlatform/NayaGramAndroid).

---

## 🛠️ 1. System Requirements & Prerequisites

* **Operating System:** Linux, macOS, or Windows (WSL2 recommended)
* **JDK:** OpenJDK 17 or Temurin JDK 17 (`JAVA_HOME` configured to JDK 17)
* **Android Studio:** Android Studio 2025.1+
* **Android SDK & Native Tooling:**
  - Android SDK Platform: `36`
  - Android SDK Build-Tools: `36.0.0`
  - CMake: `3.22.1`
  - Android NDK: `27.2.12479018`

---

## 📥 2. Cloning the Repository (Submodules Required)

NayaGram uses native C++ libraries (`boringssl`, `ffmpeg`, `libvpx`, `tgcalls`, `tdlib`, `media3`, etc.). Always clone with `--recursive`:

```bash
git clone --recursive https://github.com/NayaGramPlatform/NayaGramAndroid.git
cd NayaGramAndroid
```

*(If you already cloned without submodules, initialize them via:)*
```bash
git submodule sync --recursive
git submodule update --init --recursive
```

---

## ⚙️ 3. Credentials & Local Environment Setup

For security, private API keys, Firebase configs, and signing keystores are never hardcoded into source code.

1. **Create `local.properties`** in the project root:
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   ndk.dir=/path/to/your/Android/Sdk/ndk/27.2.12479018
   ```

2. **Get & Set Telegram API Credentials:**
   Log in at [**https://my.telegram.org**](https://my.telegram.org) (see [**API ID instructions**](https://core.telegram.org/api/obtaining_api_id)) to obtain your own `api_id` and `api_hash`, then export them or pass them as Gradle `-P` flags:
   ```bash
   export TELEGRAM_API_ID=YOUR_API_ID
   export TELEGRAM_API_HASH=YOUR_API_HASH
   ```

3. **Firebase & Maps Configuration:**
   - Replace `google-services.json` in `TMessagesProj_App/` and sibling app modules with your own Firebase project file if you need FCM push notifications.
   - Replace `com.google.android.maps.v2.API_KEY` inside `TMessagesProj/config/*/AndroidManifest*.xml` with your own Google Maps Android API key if map previews are needed.

4. **Signing Keystore (for Release APK / AAB builds):**
   Supply your keystore via environment variables or `-P` flags:
   ```bash
   export RELEASE_KEYSTORE_PATH=/path/to/your-release.jks
   export RELEASE_STORE_PASSWORD=your_store_password
   export RELEASE_KEY_PASSWORD=your_key_password
   export RELEASE_KEY_ALIAS=your_key_alias
   ```

---

## 🔨 4. Building via Command Line

* **Build Universal Release APK:**
  ```bash
  ./gradlew :TMessagesProj_App:assembleAfatRelease
  ```
* **Build Google Play Release AAB (SDK 24+):**
  ```bash
  ./gradlew :TMessagesProj_App:bundleBundleAfat_SDK23Release
  ```

---

## 💻 5. Opening in Android Studio

1. Open **Android Studio** and select **Open** *(do **not** choose Import)*.
2. Select the `NayaGramAndroid` root folder and wait for Gradle Sync to finish.
3. Ensure `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` are available in your environment or `local` Gradle properties *(never commit secrets to Git)*.
4. In **Build Variants**, select `:TMessagesProj_App` → `afatDebug` or `afatRelease` and run.
