# 🚀 NayaGram Android - Azmat Bhaiyya Local Build Guide

Welcome Azmat Bhaiyya! This repository is completely pre-configured and cleaned up for building **NayaGram Android** (Release APK & Google Play AAB bundle).

---

## 🛠️ 1. System Requirements & Prerequisites

Ensure the following tools are installed on your machine:
* **Operating System:** Linux, macOS, or Windows (WSL2 recommended for Windows)
* **JDK:** OpenJDK 17 or Temurin JDK 17 (set `JAVA_HOME` to JDK 17)
* **Android Studio:** Android Studio 2024.2+ (Ladybug / Meerkat / 2025.1+)
* **Android SDK:**
  - Android SDK Platform 36 (Android 16 / UpsideDownCake/Baklava)
  - Android SDK Build-Tools: `36.0.0`
  - CMake: `3.22.1` (install via Android Studio SDK Manager -> SDK Tools -> CMake)
  - Android NDK: `27.2.12479018` (install via SDK Tools -> NDK Side by side -> 27.2.12479018)

---

## 📥 2. Cloning the Repository (Crucial: Submodules Required)

Telegram relies on native C++ libraries (BoringSSL, FFmpeg, Libvpx, WebRTC, Media3). You **MUST** clone with `--recursive`:

```bash
git clone --recursive https://github.com/NayaGramPlatform/NGAndroid.git
cd NGAndroid
```

*(If you already cloned without submodules, run:)*
```bash
git submodule update --init --recursive
```

---

## ⚙️ 3. Environment & Local Configuration

1. **Create `local.properties`** in the root directory if not present:
```properties
sdk.dir=/path/to/your/Android/Sdk
ndk.dir=/path/to/your/Android/Sdk/ndk/27.2.12479018
```

2. **Signing Keystore:**
- The release keystore is located at: `TMessagesProj/config/release.keystore`
- Passwords are pre-configured in `gradle.properties`.

3. **Memory Allocation:**
In `gradle.properties`, ensure you have sufficient heap:
```properties
org.gradle.jvmargs=-Xmx6g -XX:MaxMetaspaceSize=1g
org.gradle.parallel=true
```

---

## 🔨 4. How to Build via Command Line

### A. Build Universal Release APK (For Direct Installation / Testing):
```bash
./gradlew :TMessagesProj_App:assembleAfatRelease -Pandroid.stripDebugSymbol.enable=false
```
*Output Location:*
`TMessagesProj_App/build/outputs/apk/afat/release/TMessagesProj_App-afat-release.apk`

### B. Build Google Play Release AAB Bundle (For Play Store Submission):
```bash
./gradlew :TMessagesProj_App:bundleBundleAfatRelease -Pandroid.stripDebugSymbol.enable=false
```
*Output Location:*
`TMessagesProj_App/build/outputs/bundle/bundleAfatRelease/TMessagesProj_App-bundleAfatRelease.aab`

---

## 💻 5. Opening in Android Studio

1. Open Android Studio.
2. Select **"Open"** (Do **NOT** choose "Import").
3. Select the `NGAndroid` root folder.
4. Wait for Gradle Sync to complete.
5. In the Build Variants tool window (bottom left), select:
   - Module: `:TMessagesProj_App`
   - Variant: `afatRelease` or `afatDebug`
6. Click **Build > Generate Signed Bundle / APK...** or run directly from the Run button.

---

## 🔍 6. Codebase Fixes Already Completed

All legacy Telegram & NayaGram syntax/resource bugs have been permanently resolved in this branch:
1. **Resource Names:** Cleaned double extensions (e.g. `*.png.png`).
2. **Duplicate Resources:** Removed conflicting WebP / PNG mipmaps.
3. **XML Lint:** Fixed unsupported `iconSpaceReserved` attribute in anti-delete preferences.
4. **Java Syntax:** Fixed escaped newlines in `NGSettingsActivity.java` and restored `ContactsWidgetProvider.java`.
5. **Dynamic Metadata:** Eliminated duplicate `ResLottieMeta.java` class collision.
6. **Dependencies:** Restored upstream `Media3` and `WebKit` modules in `TMessagesProj/build.gradle`.
7. **Namespace & App Package:** Internal library namespace is strictly `org.telegram.messenger` to preserve 3,000+ `R.class` references, while final application ID is `org.nayagram.platform`.
