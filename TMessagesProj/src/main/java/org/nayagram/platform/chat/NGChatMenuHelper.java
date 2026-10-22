package org.nayagram.platform.chat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.nayagram.platform.NayaConfig;
import org.nayagram.platform.security.BiometricChatLocker;
import org.nayagram.platform.translator.InstantTranslatorManager;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;

/**
 * NGChatMenuHelper - In-Chat 3-Dots Menu & In-Chat Settings for NayaGram.
 * Matches Novagram & NayaGram in-chat features:
 * 1. Chat ID display & copy
 * 2. NayaGram In-Chat Settings
 * 3. Auto-translate
 */
public final class NGChatMenuHelper {

    public static final int ITEM_CHAT_ID = 29001;
    public static final int ITEM_NG_SETTINGS = 29002;
    public static final int ITEM_AUTO_TRANSLATE = 29004;

    private static final String PREFS_CHAT = "nayagram_chat_prefs_";

    public static void addItems(ActionBarMenuItem headerItem, long dialogId) {
        if (headerItem == null) return;
        headerItem.lazilyAddSubItem(ITEM_CHAT_ID, R.drawable.settings_faq, "ID: " + dialogId).setColors(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem), Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon));
        headerItem.lazilyAddSubItem(ITEM_NG_SETTINGS, R.drawable.settings_features, "NayaGram Settings").setColors(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem), Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon));
        headerItem.lazilyAddSubItem(ITEM_AUTO_TRANSLATE, R.drawable.settings_chat, "Auto-translate").setColors(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem), Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon));
    }

    public static boolean handleItemClick(ChatActivity chatActivity, int id, long dialogId) {
        if (chatActivity == null || chatActivity.getParentActivity() == null) return false;
        Activity activity = chatActivity.getParentActivity();

        if (id == ITEM_CHAT_ID) {
            AndroidUtilities.addToClipboard(String.valueOf(dialogId));
            Toast.makeText(activity, "Chat ID copied: " + dialogId, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == ITEM_AUTO_TRANSLATE) {
            showAutoTranslatePicker(activity, dialogId);
            return true;
        } else if (id == ITEM_NG_SETTINGS) {
            showInChatSettings(chatActivity, activity, dialogId);
            return true;
        }
        return false;
    }

    private static void showAutoTranslatePicker(Activity activity, long dialogId) {
        String[][] langs = InstantTranslatorManager.SUPPORTED_LANGUAGES;
        String[] titles = new String[langs.length + 1];
        titles[0] = "Off ✓";
        for (int i = 0; i < langs.length; i++) {
            titles[i + 1] = langs[i][1];
        }

        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setTitle("Auto-translate");
        b.setItems(titles, (dialog, which) -> {
            SharedPreferences sp = activity.getSharedPreferences(PREFS_CHAT + dialogId, Context.MODE_PRIVATE);
            if (which == 0) {
                sp.edit().remove("auto_translate_target").apply();
                Toast.makeText(activity, "Auto-translate: Off", Toast.LENGTH_SHORT).show();
            } else {
                String selectedCode = langs[which - 1][0];
                String selectedName = langs[which - 1][1];
                sp.edit().putString("auto_translate_target", selectedCode).apply();
                Toast.makeText(activity, "Auto-translate set to: " + selectedName, Toast.LENGTH_SHORT).show();
            }
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private static void showInChatSettings(ChatActivity chatActivity, Activity activity, long dialogId) {
        SharedPreferences sp = activity.getSharedPreferences(PREFS_CHAT + dialogId, Context.MODE_PRIVATE);
        boolean oneTime = sp.getBoolean("one_time_media", false);
        boolean autoAddText = sp.getBoolean("auto_add_text", false);
        String customText = sp.getString("custom_appended_text", "https://t.me/NayaGramPro");
        BiometricChatLocker locker = BiometricChatLocker.getInstance(activity);
        boolean isLocked = locker.isChatProtected(dialogId);

        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
        list.addView(coloredRow(activity, 0xFF0EA5E9, "1", "One-time voice & video", oneTime ? "ON" : "OFF"));
        list.addView(coloredRow(activity, 0xFF10B981, "T", "Auto-add text", autoAddText ? "ON" : "OFF"));
        list.addView(coloredRow(activity, 0xFF8B5CF6, "S", "Edit text signature", customText));
        list.addView(coloredRow(activity, 0xFFEC407A, "L", "Biometric Chat Lock", isLocked ? "LOCKED" : "UNLOCKED"));

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(list);
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setTitle("NayaGram In-Chat Settings");
        b.setView(scroll);
        b.setPositiveButton("Close", null);
        AlertDialog dialog = b.show();
        for (int i = 0; i < list.getChildCount(); i++) {
            final int which = i;
            list.getChildAt(i).setOnClickListener(v -> {
                dialog.dismiss();
                if (which == 0) {
                    boolean newVal = !oneTime;
                    sp.edit().putBoolean("one_time_media", newVal).apply();
                    Toast.makeText(activity, "One-time media: " + (newVal ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
                } else if (which == 1) {
                    boolean newVal = !autoAddText;
                    sp.edit().putBoolean("auto_add_text", newVal).apply();
                    Toast.makeText(activity, "Auto-add text: " + (newVal ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
                } else if (which == 2) {
                    showEditTextSignatureDialog(activity, sp);
                } else if (which == 3) {
                    if (!isLocked && !NayaConfig.getInstance().isBiometricChatLockerEnabled()) {
                        Toast.makeText(activity, "Enable Biometric Chat Locker in NayaGram Settings first.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (isLocked) {
                        locker.unlockChatPermanent(dialogId);
                        Toast.makeText(activity, "Chat Lock Disabled", Toast.LENGTH_SHORT).show();
                    } else {
                        locker.lockChat(dialogId);
                        Toast.makeText(activity, "Chat locked. Reopen it and authenticate to continue.", Toast.LENGTH_SHORT).show();
                        // Close the currently visible conversation so locking it takes effect immediately.
                        chatActivity.finishFragment();
                    }
                }
            });
        }
    }

    private static View coloredRow(Context context, int color, String mark, String title, String value) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF8FAFC);
        bg.setCornerRadius(AndroidUtilities.dp(14));
        row.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = AndroidUtilities.dp(8);
        row.setLayoutParams(lp);

        TextView badge = new TextView(context);
        badge.setText(mark);
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(color);
        badgeBg.setCornerRadius(AndroidUtilities.dp(12));
        badge.setBackground(badgeBg);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(AndroidUtilities.dp(36), AndroidUtilities.dp(36));
        blp.rightMargin = AndroidUtilities.dp(12);
        badge.setLayoutParams(blp);
        row.addView(badge);

        LinearLayout text = new LinearLayout(context);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(0xFF111827);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TextView valueView = new TextView(context);
        valueView.setText(value);
        valueView.setTextColor(0xFF6B7280);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        valueView.setMaxLines(1);
        valueView.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(titleView);
        text.addView(valueView);
        row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private static void showEditTextSignatureDialog(Activity activity, SharedPreferences sp) {
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setTitle("Edit Auto-add text");
        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(sp.getString("custom_appended_text", "https://t.me/NayaGramPro"));
        b.setView(input);
        b.setPositiveButton("Save", (dialog, which) -> {
            String txt = input.getText().toString().trim();
            sp.edit().putString("custom_appended_text", txt).apply();
            Toast.makeText(activity, "Saved: " + txt, Toast.LENGTH_SHORT).show();
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }
}
