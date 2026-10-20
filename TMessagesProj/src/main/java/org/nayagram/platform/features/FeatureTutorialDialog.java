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
            "8. When enabled, your own profile shows your Telegram ID and current account data-center ID; long-press the line to copy.\n\n" +
            "FEATURE AVAILABILITY\\n" +
            "Only verified, working features are shown in the active Features page. Planned or incomplete feature cards have been removed from the active list so they cannot be mistaken for working controls. A saved preference alone does not activate a feature engine. Do not rely on any feature unless its behavior has been implemented and verified.\\n\\n" +
            "CHECKS AND BACKUPS\n" +
            "Feature Health Check reports local setting values only; it is not a security certification. Export Settings Backup shares supported feature preferences and excludes account, chat, and credential data.\n\n" +
            "IMPORTANT\n" +
            "Do not rely on an unavailable feature for privacy, security, message delivery, or call quality. Use only controls clearly marked as available.\n\n" +
            "TRANSLATION NOTE\n" +
            "The selected language is machine-translated for this tutorial. Only this public tutorial text is sent for translation; this tutorial does not send your chats or contacts.";

    private static String getFlagForLanguage(String code) {
        switch (code) {
            case "bn": return "🇧🇩";
            case "en": return "🇬🇧";
            case "ar": return "🇸🇦";
            case "ur": return "🇵🇰";
            case "hi":
            case "ta":
            case "te": return "🇮🇳";
            case "es": return "🇪🇸";
            case "fr": return "🇫🇷";
            case "de": return "🇩🇪";
            case "ru": return "🇷🇺";
            case "tr": return "🇹🇷";
            case "fa": return "🇮🇷";
            case "id": return "🇮🇩";
            case "ms": return "🇲🇾";
            case "zh-CN": return "🇨🇳";
            case "ja": return "🇯🇵";
            case "ko": return "🇰🇷";
            case "it": return "🇮🇹";
            case "pt": return "🇵🇹";
            case "nl": return "🇳🇱";
            case "pl": return "🇵🇱";
            case "uk": return "🇺🇦";
            case "th": return "🇹🇭";
            case "vi": return "🇻🇳";
            case "uz": return "🇺🇿";
            case "ps": return "🇦🇫";
            case "sw": return "🇰🇪";
            default: return "🌐";
        }
    }

    private FeatureTutorialDialog() {}

    public static void show(Context context) {
        if (context == null) return;
        InstantTranslatorManager translator = InstantTranslatorManager.getInstance(context);
        String[][] languages = InstantTranslatorManager.SUPPORTED_LANGUAGES;
        final String[] selectedCode = {translator.getTargetLanguage()};
        final LinearLayout[] selectedRows = new LinearLayout[languages.length];
        final TextView[] flagViews = new TextView[languages.length];
        final TextView[] selectedMarks = new TextView[languages.length];
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(4));

        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        GradientDrawable heroBackground = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFE0F2FE, 0xFFDCFCE7});
        heroBackground.setCornerRadius(AndroidUtilities.dp(18));
        heroBackground.setStroke(AndroidUtilities.dp(1), 0xFFBAE6FD);
        hero.setBackground(heroBackground);

        TextView heroTitle = new TextView(context);
        heroTitle.setText("🌐 NayaGram Feature Guide");
        heroTitle.setTextColor(0xFF075985);
        heroTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        heroTitle.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        hero.addView(heroTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView intro = new TextView(context);
        intro.setText("Choose your language and tap Details for a step-by-step tutorial. This page only explains features; it never turns them on or off.");
        intro.setTextColor(0xFF334155);
        intro.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        intro.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(8));
        hero.addView(intro, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView languageCount = new TextView(context);
        languageCount.setText("✨ " + languages.length + " tutorial languages · Tutorial only");
        languageCount.setTextColor(0xFF166534);
        languageCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        languageCount.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        hero.addView(languageCount, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroParams.bottomMargin = AndroidUtilities.dp(12);
        panel.addView(hero, heroParams);

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
            flag.setText(getFlagForLanguage(languages[i][0]));
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

            TextView selectedMark = new TextView(context);
            selectedMark.setText(languages[i][0].equals(selectedCode[0]) ? "✓" : "");
            selectedMark.setTextColor(0xFF047857);
            selectedMark.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            selectedMark.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
            selectedMark.setGravity(Gravity.CENTER);
            row.addView(selectedMark, new LinearLayout.LayoutParams(AndroidUtilities.dp(24), ViewGroup.LayoutParams.MATCH_PARENT));
            selectedMarks[i] = selectedMark;
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
                    selectedMarks[j].setText(selected ? "✓" : "");
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
        // A gentle staggered bounce gives every language flag a visible, lightweight animation.
        for (int i = 0; i < flagViews.length; i++) {
            final TextView flag = flagViews[i];
            flag.setScaleX(1f);
            flag.setScaleY(1f);
            flag.animate().cancel();
            flag.animate().scaleX(1.12f).scaleY(1.12f)
                    .setStartDelay(i * 18L).setDuration(130)
                    .withEndAction(() -> flag.animate().scaleX(1f).scaleY(1f).setDuration(130).start())
                    .start();
        }
        if (selectedIndex >= 0 && selectedIndex < flagViews.length) {
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
        final boolean[] requestCancelled = {false};
        loading.setOnCancelListener(dialog -> requestCancelled[0] = true);
        loading.show();
        translator.translate(TUTORIAL_EN, languageCode, new InstantTranslatorManager.TranslationCallback() {
            @Override public void onTranslationSuccess(String originalText, String translatedText, String targetLang) {
                if (requestCancelled[0]) return;
                try { loading.dismiss(); } catch (Throwable ignored) {}
                boolean fallback = translatedText == null || translatedText.trim().isEmpty() || translatedText.equals(originalText);
                showTranslatedText(context, fallback ? originalText : translatedText, getLanguageName(targetLang), fallback);
            }
            @Override public void onTranslationFailed(String originalText, String errorMessage) {
                if (requestCancelled[0]) return;
                try { loading.dismiss(); } catch (Throwable ignored) {}
                showTranslatedText(context, originalText, getLanguageName(languageCode), true);
            }
        });
    }

    private static void showTranslatedText(Context context, String text, String language, boolean fallback) {
        String heading = fallback ? "Translation unavailable; showing English tutorial.\n\n" : "Machine-translated tutorial · " + language + "\n\n";
        new AlertDialog.Builder(context)
                .setTitle("📖 Feature Pipeline · Details")
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
