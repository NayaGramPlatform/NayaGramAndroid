# -*- coding: utf-8 -*-
import pathlib
import re

def patch_gradle():
    p = pathlib.Path("gradle.properties")
    t = p.read_text(encoding="utf-8")
    t2, code_count = re.subn(r"(?m)^APP_VERSION_CODE=.*$", "APP_VERSION_CODE=4048", t, count=1)
    t2, name_count = re.subn(r"(?m)^APP_VERSION_NAME=.*$", "APP_VERSION_NAME=4.16.45 NG", t2, count=1)
    if code_count != 1 or name_count != 1:
        raise SystemExit("APP_VERSION_CODE / APP_VERSION_NAME setting missing or duplicated")
    if t2 == t:
        print("gradle already 4048 / 4.16.45 NG")
    else:
        p.write_text(t2, encoding="utf-8")
        print("gradle version updated to 4048 / 4.16.45 NG")

def patch_profile():
    p = pathlib.Path("TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java")
    t = p.read_text(encoding="utf-8")
    if "nayaIdCopy" in t:
        print("profile copy already")
        return
    old = '''            try {
                if (org.nayagram.platform.NayaConfig.getInstance().isShowIdAndDc() && user.id > 0) {
                    int dc = user.photo != null ? user.photo.dc_id : 0;
                    String idLine = "ID " + user.id + (dc > 0 ? " \u00b7 DC" + dc : "");
'''
    new = '''            final String[] nayaIdCopy = new String[1];
            final String[] nayaDcCopy = new String[1];
            final String[] nayaFullCopy = new String[1];
            try {
                if (org.nayagram.platform.NayaConfig.getInstance().isShowIdAndDc() && user.id > 0) {
                    int dc = user.photo != null ? user.photo.dc_id : 0;
                    String idLine = "ID " + user.id + (dc > 0 ? " \u00b7 DC" + dc : "");
                    nayaIdCopy[0] = Long.toString(user.id);
                    nayaDcCopy[0] = dc > 0 ? ("DC" + dc) : "";
                    String nayaUser = user.username != null && user.username.length() > 0 ? ("@" + user.username) : "";
                    nayaFullCopy[0] = idLine + (nayaUser.length() > 0 ? (" \u00b7 " + nayaUser) : "");
'''
    if old not in t:
        raise SystemExit("profile id block missing")
    t = t.replace(old, new, 1)
    anchor = "onlineTextView[a].setDrawablePadding(dp(9));"
    insert = '''onlineTextView[a].setDrawablePadding(dp(9));
                if (nayaIdCopy[0] != null) {
                    onlineTextView[a].setOnLongClickListener(v -> {
                        String full = nayaFullCopy[0] != null ? nayaFullCopy[0] : nayaIdCopy[0];
                        AndroidUtilities.addToClipboard(full);
                        Toast.makeText(getContext(), "Copied", Toast.LENGTH_SHORT).show();
                        return true;
                    });
                    onlineTextView[a].setOnTouchListener((v, ev) -> {
                        if (ev.getAction() != android.view.MotionEvent.ACTION_UP) {
                            return false;
                        }
                        SimpleTextView tv = (SimpleTextView) v;
                        String text = tv.getText() != null ? tv.getText().toString() : "";
                        int idIdx = text.lastIndexOf("ID ");
                        if (idIdx < 0) {
                            return false;
                        }
                        float x = ev.getX();
                        float idX = tv.getPaint().measureText(text.substring(0, idIdx));
                        int dcIdx = text.lastIndexOf(" \u00b7 DC");
                        if (dcIdx >= 0 && nayaDcCopy[0] != null && nayaDcCopy[0].length() > 0) {
                            float dcX = tv.getPaint().measureText(text.substring(0, dcIdx + 3));
                            if (x >= dcX) {
                                AndroidUtilities.addToClipboard(nayaDcCopy[0]);
                                Toast.makeText(getContext(), "DC Copied!", Toast.LENGTH_SHORT).show();
                                return true;
                            }
                        }
                        if (x >= idX) {
                            AndroidUtilities.addToClipboard(nayaIdCopy[0]);
                            Toast.makeText(getContext(), "ID Copied!", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        return false;
                    });
                }
'''
    if t.count(anchor) != 1:
        raise SystemExit("padding anchor count " + str(t.count(anchor)))
    t = t.replace(anchor, insert.rstrip("\n"), 1)
    p.write_text(t, encoding="utf-8")
    print("profile copy wired")

def patch_voip_service():
    p = pathlib.Path("TMessagesProj/src/main/java/org/telegram/messenger/voip/VoIPService.java")
    t = p.read_text(encoding="utf-8")
    old_catch = '} catch (Exception x) {\n\t\t\tif (BuildVars.LOGS_ENABLED) {\n\t\t\t\tFileLog.e("error initializing voip controller", x);\n\t\t\t}\n\t\t\tcallFailed();\n\t\t}'
    new_catch = '} catch (Throwable x) {\n\t\t\tandroid.util.Log.e("NayaGramCall", "onCreate init", x);\n\t\t\ttry {\n\t\t\t\tcallFailed();\n\t\t\t} catch (Throwable ignored) {\n\t\t\t}\n\t\t}'
    if old_catch not in t:
        if 'NayaGramCall", "onCreate init"' in t:
            print("voip catch already")
        else:
            raise SystemExit("voip catch missing")
    else:
        t = t.replace(old_catch, new_catch, 1)
        print("voip catch -> Throwable")
    old_type = "\t\tif (Build.VERSION.SDK_INT >= 34) {\n\t\t\ttype |= 4; // ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL\n\t\t}\n"
    if old_type in t:
        t = t.replace(old_type, "\t\t// phoneCall FGS type crashes non-dialer clients (NayaGram keeps stopping)\n", 1)
        print("removed PHONE_CALL fgs type")
    else:
        print("phoneCall type already removed or pattern miss")
    marker = "\t\tif (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {\n\t\t\tif (callIShouldHavePutIntoIntent != null) {"
    if "startForeground fallback" in t:
        print("fg wrap already")
    elif marker not in t:
        raise SystemExit("fg marker missing")
    else:
        t = t.replace(marker, "\t\tif (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {\n\t\t\ttry {\n\t\t\tif (callIShouldHavePutIntoIntent != null) {", 1)
        end_marker = "\t\t}\n\t}\n\n\tprivate void checkUpdateBluetoothHeadset()"
        idx = t.find(end_marker)
        if idx < 0:
            raise SystemExit("fg end missing")
        fallback = """\t\t\t} catch (Throwable fg) {
\t\t\t\tandroid.util.Log.e("NayaGramCall", "startForeground", fg);
\t\t\t\ttry {
\t\t\t\t\tint safeType = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK;
\t\t\t\t\tif (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
\t\t\t\t\t\tsafeType |= android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
\t\t\t\t\t}
\t\t\t\t\tif (foregroundNotification != null) {
\t\t\t\t\t\tif (Build.VERSION.SDK_INT >= 33) {
\t\t\t\t\t\t\tstartForeground(foregroundId, foregroundNotification, safeType);
\t\t\t\t\t\t} else {
\t\t\t\t\t\t\tstartForeground(foregroundId, foregroundNotification);
\t\t\t\t\t\t}
\t\t\t\t\t}
\t\t\t\t} catch (Throwable fg2) {
\t\t\t\t\tandroid.util.Log.e("NayaGramCall", "startForeground fallback", fg2);
\t\t\t\t\ttry { stopSelf(); } catch (Throwable ignored) {}
\t\t\t\t}
\t\t\t}
\t\t}
\t}

\tprivate void checkUpdateBluetoothHeadset()"""
        t = t[:idx] + fallback + t[idx+len(end_marker):]
        print("fg wrapped")
    p.write_text(t, encoding="utf-8")

def patch_voip_helper():
    p = pathlib.Path("TMessagesProj/src/main/java/org/telegram/ui/Components/voip/VoIPHelper.java")
    t = p.read_text(encoding="utf-8")
    if "safeCreateGroupCall" in t:
        print("helper already")
        return
    t = t.replace(
        "GroupCallActivity.create((LaunchActivity) activity,",
        "safeCreateGroupCall(activity,",
    )
    method = '''
\tprivate static void safeCreateGroupCall(Activity activity, AccountInstance account, TLRPC.Chat chat, TLRPC.InputPeer peer, boolean hasFew, String hash) {
\t\tif (!(activity instanceof LaunchActivity) || activity.isFinishing()) {
\t\t\tandroid.util.Log.e("NayaGramCall", "skip GroupCallActivity.create");
\t\t\treturn;
\t\t}
\t\ttry {
\t\t\tGroupCallActivity.create((LaunchActivity) activity, account, chat, peer, hasFew, hash);
\t\t} catch (Throwable e) {
\t\t\tandroid.util.Log.e("NayaGramCall", "GroupCallActivity.create", e);
\t\t}
\t}

'''
    anchor = "\tpublic static long lastCallTime = 0;\n"
    if anchor not in t:
        raise SystemExit("helper anchor missing")
    t = t.replace(anchor, anchor + method, 1)
    if t.count("safeCreateGroupCall(activity,") < 1:
        raise SystemExit("no call sites replaced")
    p.write_text(t, encoding="utf-8")
    print("helper safeCreateGroupCall x", t.count("safeCreateGroupCall(activity,"))

if __name__ == "__main__":
    patch_gradle()
    patch_profile()
    patch_voip_service()
    patch_voip_helper()
    print("done")
