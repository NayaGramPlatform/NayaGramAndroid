# -*- coding: utf-8 -*-
"""Visual-only call backdrop. Does not touch VoIP, mute, or network logic."""
import pathlib
import sys

def patch(path, old, new, label):
    p = pathlib.Path(path)
    t = p.read_text(encoding="utf-8")
    if new in t:
        print("already", label)
        return False
    if old not in t:
        print("MISSING", label)
        sys.exit(1)
    p.write_text(t.replace(old, new, 1), encoding="utf-8")
    print("patched", label)
    return True

changed = False
changed |= patch(
    "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ThemeColors.java",
    "        defaultColors[key_voipgroup_actionBarUnscrolled] = 0xff191F26;\n"
    "        defaultColors[key_voipgroup_listViewBackgroundUnscrolled] = 0xff222A33;\n",
    "        defaultColors[key_voipgroup_actionBarUnscrolled] = 0xff1A1464;\n"
    "        defaultColors[key_voipgroup_listViewBackgroundUnscrolled] = 0xff1C2F55;\n",
    "theme unscrolled",
)
changed |= patch(
    "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ThemeColors.java",
    "        defaultColors[key_voipgroup_actionBar] = 0xff0F1317;\n",
    "        defaultColors[key_voipgroup_actionBar] = 0xff140E3A;\n",
    "theme actionBar",
)
changed |= patch(
    "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ThemeColors.java",
    "        defaultColors[key_voipgroup_listViewBackground] = 0xff1C2229;\n"
    "        defaultColors[key_voipgroup_dialogBackground] = 0xff1C2229;\n",
    "        defaultColors[key_voipgroup_listViewBackground] = 0xff1A2A4C;\n"
    "        defaultColors[key_voipgroup_dialogBackground] = 0xff162444;\n",
    "theme list",
)
changed |= patch(
    "TMessagesProj/src/main/java/org/telegram/ui/GroupCallActivity.java",
    "    private final int[] gradientColors = new int[2];\n",
    "    private final int[] gradientColors = new int[2];\n"
    "    private final Paint callBackdropPaint = new Paint(Paint.ANTI_ALIAS_FLAG);\n"
    "    private int callBackdropKey;\n",
    "backdrop fields",
)
changed |= patch(
    "TMessagesProj/src/main/java/org/telegram/ui/GroupCallActivity.java",
    "                    shadowDrawable.setBounds(0, (int) top, getMeasuredWidth(), height);\n"
    "                    shadowDrawable.draw(canvas);\n",
    "                    shadowDrawable.setBounds(0, (int) top, getMeasuredWidth(), height);\n"
    "                    shadowDrawable.draw(canvas);\n"
    "                    int backdropW = getMeasuredWidth();\n"
    "                    int backdropKey = backdropW * 31 + getMeasuredHeight();\n"
    "                    if (callBackdropPaint.getShader() == null || callBackdropKey != backdropKey) {\n"
    "                        callBackdropKey = backdropKey;\n"
    "                        callBackdropPaint.setShader(new LinearGradient(0, top, backdropW, height, new int[]{0xFF24145C, 0xFF123A86, 0xFF0B6A86, 0xFF145C4A}, new float[]{0f, 0.42f, 0.74f, 1f}, Shader.TileMode.CLAMP));\n"
    "                    }\n"
    "                    callBackdropPaint.setAlpha((int) (255 * (1f - renderersContainer.progressToFullscreenMode)));\n"
    "                    canvas.drawRect(0, top, backdropW, height, callBackdropPaint);\n",
    "group backdrop",
)
changed |= patch(
    "TMessagesProj/src/main/java/org/telegram/ui/GroupCallActivity.java",
    "            getWindow().setNavigationBarColor(0xff000000);\n",
    "            getWindow().setNavigationBarColor(0xff145C4A);\n",
    "nav bar",
)
changed |= patch(
    "TMessagesProj/src/main/java/org/telegram/ui/VoIPFragment.java",
    "        frameLayout.setBackgroundColor(0xff000000);\n",
    "        frameLayout.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{0xFF1A1464, 0xFF0E4C92, 0xFF0B6E86, 0xFF145C4A}));\n",
    "voip backdrop",
)
print("done", changed)
