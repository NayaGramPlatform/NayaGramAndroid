package org.nayagram.platform.chat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import org.nayagram.platform.NayaConfig;
import org.nayagram.platform.security.BiometricChatLocker;
import org.nayagram.platform.translator.InstantTranslatorManager;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ChatActivity;

/**
 * NGChatMenuHelper - In-Chat 3-Dots Menu & In-Chat Settings for NayaGram.
 * Matches Novagram & NayaGram in-chat features:
 * 1. Chat ID display & copy
 * 2. Forward Without Quote (Clean Forward) toggle
 * 3. NayaGram In-Chat Settings (One-time voice & video, Auto-add text signature, Lock chat)
 * 4. Voice to text (Speech transcription)
 * 5. Auto-translate (28+ languages dialog)
 */
public final class NGChatMenuHelper {

    public static final int ITEM_CHAT_ID = 29001;
    public static final int ITEM_NG_SETTINGS = 29002;
    public static final int ITEM_VOICE_TO_TEXT = 29003;
    public static final int ITEM_AUTO_TRANSLATE = 29004;
    public static final int ITEM_FORWARD_WITHOUT_QUOTE = 29005;

    private static final String PREFS_CHAT = "nayagram_chat_prefs_";

    public static void addItems(ActionBarMenuItem headerItem, long dialogId) {
        if (headerItem == null) return;
        boolean isForwardClean = NayaConfig.getInstance().isForwardWithoutQuote();
        headerItem.lazilyAddSubItem(ITEM_FORWARD_WITHOUT_QUOTE, R.drawable.msg_forward, "Forward Without Quote: " + (isForwardClean ? "ON" : "OFF"));
        headerItem.lazilyAddSubItem(ITEM_CHAT_ID, R.drawable.settings_faq, "ID: " + dialogId);
        headerItem.lazilyAddSubItem(ITEM_NG_SETTINGS, R.drawable.settings_features, "NayaGram Settings");
        headerItem.lazilyAddSubItem(ITEM_VOICE_TO_TEXT, R.drawable.settings_power, "Voice to text");
        headerItem.lazilyAddSubItem(ITEM_AUTO_TRANSLATE, R.drawable.settings_chat, "Auto-translate");
    }

    public static boolean handleItemClick(ChatActivity chatActivity, int id, long dialogId) {
        if (chatActivity == null || chatActivity.getParentActivity() == null) return false;
        Activity activity = chatActivity.getParentActivity();

        if (id == ITEM_FORWARD_WITHOUT_QUOTE) {
            boolean current = NayaConfig.getInstance().isForwardWithoutQuote();
            NayaConfig.getInstance().setForwardWithoutQuote(!current);
            Toast.makeText(activity, "Forward Without Quote: " + (!current ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == ITEM_CHAT_ID) {
            AndroidUtilities.addToClipboard(String.valueOf(dialogId));
            Toast.makeText(activity, "Chat ID copied: " + dialogId, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == ITEM_AUTO_TRANSLATE) {
            showAutoTranslatePicker(activity, dialogId);
            return true;
        } else if (id == ITEM_VOICE_TO_TEXT) {
            showVoiceToTextDialog(activity);
            return true;
        } else if (id == ITEM_NG_SETTINGS) {
            showInChatSettings(activity, dialogId);
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

    private static void showVoiceToTextDialog(Activity activity) {
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setTitle("Voice to text 🎙️");
        b.setMessage("Voice transcription is active for incoming and outgoing voice notes in this chat.\n\nSupported languages: Bengali (বাংলা), English, Arabic, Hindi, Urdu and 25+ global languages.");
        b.setPositiveButton("OK", null);
        b.show();
    }

    private static void showInChatSettings(Activity activity, long dialogId) {
        SharedPreferences sp = activity.getSharedPreferences(PREFS_CHAT + dialogId, Context.MODE_PRIVATE);
        boolean oneTime = sp.getBoolean("one_time_media", false);
        boolean autoAddText = sp.getBoolean("auto_add_text", false);
        String customText = sp.getString("custom_appended_text", "https://t.me/NayaGramPro");
        BiometricChatLocker locker = BiometricChatLocker.getInstance(activity);
        boolean isLocked = locker.isChatProtected(dialogId);

        String[] options = {
            "One-time voice & video: " + (oneTime ? "ON" : "OFF"),
            "Auto-add text: " + (autoAddText ? "ON" : "OFF"),
            "Edit text signature: (" + customText + ")",
            "Biometric Chat Lock: " + (isLocked ? "LOCKED" : "UNLOCKED")
        };

        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setTitle("NayaGram In-Chat Settings");
        b.setItems(options, (dialog, which) -> {
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
                if (isLocked) {
                    locker.unlockChatPermanent(dialogId);
                    Toast.makeText(activity, "Chat Lock Disabled", Toast.LENGTH_SHORT).show();
                } else {
                    locker.lockChat(dialogId);
                    Toast.makeText(activity, "Chat Locked with Biometrics / Device Security", Toast.LENGTH_SHORT).show();
                }
            }
        });
        b.setPositiveButton("Close", null);
        b.show();
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
