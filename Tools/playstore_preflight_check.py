#!/usr/bin/env python3
"""
NayaGram Play Store Release Preflight Checker
Verifies:
1. Version code & Version name
2. 64-bit ABI support (arm64-v8a, x86_64)
3. Target SDK compliance (>= 34, Google Play requirement)
4. Exported components for Android 12+ (android:exported on intent-filters)
5. Sensitive / dangerous permissions needing Play Console declaration
"""

import sys
import os
import re
import xml.etree.ElementTree as ET

ANDROID_NS = '{http://schemas.android.com/apk/res/android}'

def log_pass(msg):
    print(f"[\033[92mPASS\033[0m] {msg}")

def log_warn(msg):
    print(f"[\033[93mWARN\033[0m] {msg}")

def log_fail(msg):
    print(f"[\033[91mFAIL\033[0m] {msg}")

def run_preflight(repo_dir="."):
    failures = 0
    warnings = 0

    print("==================================================")
    print(" NayaGram Release Preflight Check (Google Play) ")
    print("==================================================")

    # 1. Check gradle.properties
    props_path = os.path.join(repo_dir, "gradle.properties")
    props = {}
    if os.path.exists(props_path):
        with open(props_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    props[k.strip()] = v.strip()

    vcode_str = props.get("APP_VERSION_CODE")
    vname = props.get("APP_VERSION_NAME")

    try:
        vcode = int(vcode_str)
        if vcode >= 1:
            log_pass(f"versionCode: {vcode} (Valid integer >= 1)")
        else:
            log_fail(f"versionCode: {vcode} (Must be >= 1 for Google Play)")
            failures += 1
    except (TypeError, ValueError):
        log_fail(f"Invalid APP_VERSION_CODE in gradle.properties: {vcode_str}")
        failures += 1

    if vname and re.match(r"^\d+\.\d+(\.\d+)?", vname):
        log_pass(f"versionName: '{vname}' (Valid release format)")
    else:
        log_fail(f"versionName missing or invalid in gradle.properties: {vname}")
        failures += 1

    # 2. Check TMessagesProj_App/build.gradle for ABIs & Target SDK
    app_gradle = os.path.join(repo_dir, "TMessagesProj_App", "build.gradle")
    if os.path.exists(app_gradle):
        with open(app_gradle, "r", encoding="utf-8") as f:
            gradle_content = f.read()

        # Target SDK
        target_sdk_match = re.search(r"targetSdkVersion\s+(\d+)", gradle_content)
        if target_sdk_match:
            target_sdk = int(target_sdk_match.group(1))
            if target_sdk >= 34:
                log_pass(f"targetSdkVersion: {target_sdk} (Google Play requires >= 34)")
            else:
                log_fail(f"targetSdkVersion: {target_sdk} (Must be >= 34 for Google Play)")
                failures += 1
        else:
            log_fail("targetSdkVersion could not be parsed from TMessagesProj_App/build.gradle")
            failures += 1

        # 64-bit ABI support
        has_arm64 = "arm64-v8a" in gradle_content
        has_x86_64 = "x86_64" in gradle_content

        if has_arm64 and has_x86_64:
            log_pass("64-bit architecture support: arm64-v8a and x86_64 are present")
        else:
            log_fail(f"64-bit ABI support missing: arm64-v8a={has_arm64}, x86_64={has_x86_64}")
            failures += 1
    else:
        log_fail("TMessagesProj_App/build.gradle not found!")
        failures += 1

    # 3. Check Manifests for exported components
    manifest_paths = [
        os.path.join(repo_dir, "TMessagesProj", "src", "main", "AndroidManifest.xml"),
        os.path.join(repo_dir, "TMessagesProj", "config", "release", "AndroidManifest.xml")
    ]

    missing_exported = []
    declared_permissions = set()

    for mpath in manifest_paths:
        if not os.path.exists(mpath):
            continue
        try:
            tree = ET.parse(mpath)
            root = tree.getroot()

            # Find permissions
            for p in root.findall(".//uses-permission"):
                name = p.attrib.get(f"{ANDROID_NS}name")
                if name:
                    declared_permissions.add(name)

            # Check exported components
            for tag in ["activity", "activity-alias", "service", "receiver"]:
                for elem in root.findall(f".//{tag}"):
                    if elem.findall("intent-filter"):
                        exported = elem.attrib.get(f"{ANDROID_NS}exported")
                        comp_name = elem.attrib.get(f"{ANDROID_NS}name", "unknown")
                        if exported is None:
                            missing_exported.append((os.path.basename(mpath), tag, comp_name))
        except Exception as e:
            log_fail(f"Error parsing manifest {mpath}: {e}")
            failures += 1

    if not missing_exported:
        log_pass("Exported components: All components with intent-filter have explicit android:exported")
    else:
        log_fail(f"Found {len(missing_exported)} components with intent-filters missing android:exported:")
        for fname, tag, name in missing_exported:
            print(f"   -> [{fname}] <{tag} name=\"{name}\">")
        failures += 1

    # 4. Check permissions and warn about Play Console declarations
    sensitive_permissions = {
        "android.permission.SCHEDULE_EXACT_ALARM": "Requires declaration form on Google Play for alarms/reminders",
        "android.permission.USE_FULL_SCREEN_INTENT": "Restricted permission on Android 14+; required for incoming VoIP calls",
        "android.permission.ACCESS_FINE_LOCATION": "Location access; requires policy justification",
        "android.permission.ACCESS_BACKGROUND_LOCATION": "Background location; requires strict Google Play review",
        "android.permission.READ_MEDIA_IMAGES": "Scoped storage permission",
        "android.permission.READ_MEDIA_VIDEO": "Scoped storage permission",
        "android.permission.POST_NOTIFICATIONS": "Notification permission (Android 13+)"
    }

    found_sensitive = []
    for perm, note in sensitive_permissions.items():
        if perm in declared_permissions:
            found_sensitive.append((perm, note))

    if found_sensitive:
        for perm, note in found_sensitive:
            log_warn(f"Permission '{perm.split('.')[-1]}': {note}")
            warnings += 1
    else:
        log_pass("No extra high-risk permissions detected.")

    print("--------------------------------------------------")
    print(f"Preflight Results: {failures} FAIL, {warnings} WARNING(S)")
    print("--------------------------------------------------")

    if failures > 0:
        print("\033[91mPreflight checks FAILED. Do not build release AAB until resolved.\033[0m")
        return 1
    else:
        print("\033[92mAll preflight checks PASSED. Ready for Release AAB build.\033[0m")
        return 0

if __name__ == "__main__":
    path = sys.argv[1] if len(sys.argv) > 1 else "."
    sys.exit(run_preflight(path))
