package org.nayagram.platform.features;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.nayagram.platform.translator.InstantTranslatorManager;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.AlertDialog;

import java.util.ArrayList;

/** Multilingual Feature Pipeline tutorial. Documentation only; it never activates feature engines. */
public final class FeatureTutorialDialog {
    private static final String TUTORIAL_EN =
            "WELCOME TO THE NAYAGRAM FEATURES TUTORIAL\n\n" +
            "This page explains how the real feature cards work. It is a guide, not a promise that every planned feature is available.\n\n" +
            "HOW TO USE THE MAIN FEATURES PAGE\n" +
            "1. Open NayaGram Features and read each card's description before changing its setting.\n" +
            "2. A working ON/OFF card changes a setting connected to Telegram's native behavior. Tap it again to switch it back.\n" +
            "3. External Link Guard can ask for confirmation before opening unfamiliar external websites when enabled. It is not a guarantee that every harmful link will be detected.\n" +
            "4. Readable Text adjusts Telegram's message text size for easier reading.\n" +
            "5. Reduce Motion follows Telegram's interface-animation preference.\n" +
            "6. Data Saver changes Telegram's native media-streaming preference. It is not a complete network optimizer.\n" +
            "7. Smart Storage Guard runs a cache scan on demand and offers cache cleanup after you review the report.\n" +
            "8. User ID & DC information is shown only where the profile screen has the required integration.\n\n" +
            "FEATURES THAT ARE NOT READY\n" +
            "Cards marked SOON are unavailable. A saved preference alone does not make an engine work. Smart Chat Folders, Modular Config, Focus Mode, Battery Saver, Chat Locker, Instant Translator, and Noise Suppression do not currently have their promised native behavior connected. Message Scheduler, Auto-Reply, Story Saver, and Anonymous Stories are disabled and must not be relied on.\n\n" +
            "CHECKS AND BACKUPS\n" +
            "Feature Health Check reports local setting values only; it is not a security certification. Export Settings Backup shares supported feature preferences and excludes account, chat, and credential data.\n\n" +
            "IMPORTANT\n" +
            "Do not rely on an unavailable feature for privacy, security, message delivery, or call quality. Use only controls clearly marked as available.\n\n" +
            "TRANSLATION NOTE\n" +
            "The selected language is machine-translated for this tutorial. Only this public tutorial text is sent for translation; this tutorial does not send your chats or contacts.";

    private static final String[] FLAGS = {
            "🇧🇩", "🇬🇧", "🇸🇦", "🇵🇰", "🇮🇳", "🇪🇸", "🇫🇷", "🇩🇪",
            "🇷🇺", "🇹🇷", "🇮🇷", "🇮🇩", "🇲🇾", "🇨🇳", "🇯🇵", "🇰🇷",
            "🇮🇹", "🇵🇹", "🇳🇱", "🇵🇱", "🇺🇦", "🇹🇭", "🇻🇳", "🇱🇰",
            "🇮🇳", "🇺🇿", "🇦🇫", "🇰🇪"
    };

    private FeatureTutorialDialog() {}

    public static void show(Context context) {
        if (context == null) return;
        InstantTranslatorManager translator = InstantTranslatorManager.getInstance(context);
        String[][] languages = InstantTranslatorManager.SUPPORTED_LANGUAGES;
        final String[] selectedCode = {translator.getTargetLanguage()};
        final LinearLayout[] selectedRows = new LinearLayout[languages.length];
        final TextView[] flagViews = new TextView[languages.length];
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(4));

        TextView intro = new TextView(context);
        intro.setText("Choose a language, then tap Details to read a step-by-step guide. This page is a tutorial, not an active feature switchboard.");
        intro.setTextColor(0xFF334155);
        intro.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        intro.setPadding(0, 0, 0, AndroidUtilities.dp(10));
        panel.addView(intro, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(context);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int selectedIndex = 0;
        for (int i = 0; i < languages.length; i++) {
            if (languages[i][0].equals(selectedCode[0])) selectedIndex = i;
            final int index = i;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
            row.setBackground(roundRect(languages[i][0].equals(selectedCode[0]) ? 0xFFE0F2FE : 0xFFF8FAFC));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(42));
            rowParams.bottomMargin = AndroidUtilities.dp(5);
            list.addView(row, rowParams);

            TextView flag = new TextView(context);
            flag.setText(FLAGS[i % FLAGS.length]);
            flag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
            flag.setGravity(Gravity.CENTER);
            row.addView(flag, new LinearLayout.LayoutParams(AndroidUtilities.dp(38), ViewGroup.LayoutParams.MATCH_PARENT));
            flagViews[i] = flag;

            TextView name = new TextView(context);
            name.setText(languages[i][1]);
            name.setTextColor(0xFF1E293B);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            name.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            selectedRows[i] = row;
            row.setOnClickListener(v -> {
                selectedCode[0] = languages[index][0];
                translator.setTargetLanguage(selectedCode[0]);
                for (int j = 0; j < selectedRows.length; j++) {
                    boolean selected = j == index;
                    selectedRows[j].setBackground(roundRect(selected ? 0xFFE0F2FE : 0xFFF8FAFC));
                    flagViews[j].animate().cancel();
                    flagViews[j].setScaleX(1f);
                    flagViews[j].setScaleY(1f);
                    if (selected) flagViews[j].animate().scaleX(1.18f).scaleY(1.18f).setDuration(180).start();
                }
            });
        }
        panel.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(285)));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("📚 Feature Pipeline · Tutorial")
                .setView(panel)
                .setPositiveButton("Details", (d, which) -> showTutorialDetails(context, translator, selectedCode[0]))
                .setNegativeButton("Close", null)
                .create();
        dialog.show();
        if (selectedIndex >= 0 && selectedIndex < flagViews.length && flagViews[selectedIndex] != null) {
            flagViews[selectedIndex].setScaleX(1f);
            flagViews[selectedIndex].setScaleY(1f);
            flagViews[selectedIndex].animate().scaleX(1.12f).scaleY(1.12f).setDuration(220).start();
        }
    }

    private static void showTutorialDetails(Context context, InstantTranslatorManager translator, String languageCode) {
        if ("en".equals(languageCode)) {
            showTranslatedText(context, TUTORIAL_EN, "English", false);
            return;
        }
        AlertDialog loading = new AlertDialog.Builder(context)
                .setTitle("🌐 Translating tutorial")
                .setMessage("Preparing the tutorial in your selected language…")
                .create();
        loading.show();
        translator.translate(TUTORIAL_EN, languageCode, new InstantTranslatorManager.TranslationCallback() {
            @Override public void onTranslationSuccess(String originalText, String translatedText, String targetLang) {
                try { loading.dismiss(); } catch (Throwable ignored) {}
                boolean fallback = translatedText == null || translatedText.trim().isEmpty() || translatedText.equals(originalText);
                showTranslatedText(context, fallback ? originalText : translatedText, getLanguageName(targetLang), fallback);
            }
            @Override public void onTranslationFailed(String originalText, String errorMessage) {
                try { loading.dismiss(); } catch (Throwable ignored) {}
                showTranslatedText(context, originalText, getLanguageName(languageCode), true);
            }
        });
    }

    private static void showTranslatedText(Context context, String text, String language, boolean fallback) {
        String heading = fallback ? "Translation unavailable; showing English tutorial.\n\n" : "Machine-translated tutorial · " + language + "\n\n";
        new AlertDialog.Builder(context)
                .setTitle("📖 Feature Tutorial")
                .setMessage(heading + text)
                .setPositiveButton("Languages", (dialog, which) -> show(context))
                .setNegativeButton("Close", null)
                .show();
    }

    private static String getLanguageName(String code) {
        for (String[] language : InstantTranslatorManager.SUPPORTED_LANGUAGES) {
            if (language[0].equals(code)) return language[1];
        }
        return code;
    }

    private static GradientDrawable roundRect(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(AndroidUtilities.dp(12));
        drawable.setStroke(AndroidUtilities.dp(1), 0xFFE2E8F0);
        return drawable;
    }
}
