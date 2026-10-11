# NayaGram Platform Stability & Call Fixes Guide
Author: NayaGram Platform Team & Assistant
Target: org.nayagram.platform

## 1. 10 Pinned Dialogs Status
- MessagesController.java: maxPinnedDialogsCountDefault = 10, maxPinnedDialogsCountPremium = 10 (Already configured).

## 2. Premium & Free Launcher Icons
- LauncherIconController.java: PREMIUM is set to true, and all other 5 icons (DEFAULT, VINTAGE, AQUA, TURBO, NOX) are set to false (Free).

## 3. VoIP & Video Call Crash Resolution
- Foreground Service Type: Ensure VoIPService in AndroidManifest.xml has:
  android:foregroundServiceType="phoneCall|camera|microphone|mediaPlayback|mediaProjection"
- Native JNI: Ensure package org.nayagram.platform matches JNI bindings and native libraries.

## 4. UI Transition & Black Screen Fix
- In ActionBarLayout.java, cancel pending AnimatorSet on fragment transitions to prevent overlapping redraws.
