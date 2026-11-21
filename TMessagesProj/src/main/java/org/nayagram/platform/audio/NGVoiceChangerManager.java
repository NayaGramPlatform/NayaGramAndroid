package org.nayagram.platform.audio;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * NGVoiceChangerManager - Real-time 48kHz mono PCM Voice Changer for Voice Notes.
 * Supports Normal, Male, Female, Deep Voice, Chipmunk, Robot, Helium, Cave Echo,
 * and Telephone modes with custom vector icon UI/UX and zero per-frame allocations
 * inside the audio recording thread.
 */
public final class NGVoiceChangerManager {

    private static final String PREF_NAME = "nayagram_voice_changer_prefs";
    private static final String KEY_EFFECT_MODE = "voice_effect_mode";

    public static final int MODE_NORMAL = 0;
    public static final int MODE_CHIPMUNK = 1;
    public static final int MODE_DEEP = 2;
    public static final int MODE_ROBOT = 3;
    public static final int MODE_HELIUM = 4;
    public static final int MODE_CAVE = 5;
    public static final int MODE_TELEPHONE = 6;
    public static final int MODE_MALE = 7;
    public static final int MODE_FEMALE = 8;
    private static final int MODE_MAX = MODE_FEMALE;

    private static final int[] DISPLAY_MODES = new int[] {
            MODE_NORMAL,
            MODE_MALE,
            MODE_FEMALE,
            MODE_DEEP,
            MODE_CHIPMUNK,
            MODE_ROBOT,
            MODE_HELIUM,
            MODE_CAVE,
            MODE_TELEPHONE
    };

    private static final int DEFAULT_SAMPLE_RATE = 48_000;

    private static volatile int cachedMode = -1;

    private static final Object DSP_LOCK = new Object();
    private static StudioDsp legacyDsp = null;
    private static int legacyDspMode = MODE_NORMAL;
    private static int legacyDspSampleRate = DEFAULT_SAMPLE_RATE;

    private NGVoiceChangerManager() {}

    private static SharedPreferences getPrefs(Context context) {
        if (context == null) {
            context = ApplicationLoader.applicationContext;
        }
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static int getSelectedMode(Context context) {
        if (cachedMode >= MODE_NORMAL && cachedMode <= MODE_MAX) {
            return cachedMode;
        }
        try {
            SharedPreferences prefs = getPrefs(context);
            int mode = prefs == null ? MODE_NORMAL : prefs.getInt(KEY_EFFECT_MODE, MODE_NORMAL);
            if (mode < MODE_NORMAL || mode > MODE_MAX) {
                mode = MODE_NORMAL;
            }
            cachedMode = mode;
            return mode;
        } catch (Throwable ignored) {
            return MODE_NORMAL;
        }
    }

    public static void setSelectedMode(Context context, int mode) {
        if (mode < MODE_NORMAL || mode > MODE_MAX) {
            mode = MODE_NORMAL;
        }
        cachedMode = mode;
        try {
            SharedPreferences prefs = getPrefs(context);
            if (prefs != null) prefs.edit().putInt(KEY_EFFECT_MODE, mode).apply();
        } catch (Throwable ignored) {}
    }

    public static String getModeTitle(int mode) {
        switch (mode) {
            case MODE_MALE: return "Male Voice";
            case MODE_FEMALE: return "Female Voice";
            case MODE_CHIPMUNK: return "Chipmunk (High Pitch)";
            case MODE_DEEP: return "Deep Voice (Bass Boost)";
            case MODE_ROBOT: return "Robot (Vocoder)";
            case MODE_HELIUM: return "Helium";
            case MODE_CAVE: return "Cave (Echo)";
            case MODE_TELEPHONE: return "Telephone (Bandpass)";
            default: return "Normal Voice";
        }
    }

    public static String getModeSubtitle(int mode) {
        switch (mode) {
            case MODE_MALE: return "Natural masculine pitch & warm chest resonance";
            case MODE_FEMALE: return "Smooth feminine vocal pitch & bright clarity";
            case MODE_CHIPMUNK: return "Playful fast high-pitched cartoon voice";
            case MODE_DEEP: return "Extra-deep cinematic voice with bass warmth";
            case MODE_ROBOT: return "Crisp synthetic robotic vocoder modulation";
            case MODE_HELIUM: return "Light & airy high-frequency balloon voice";
            case MODE_CAVE: return "Spacious multi-tap canyon echo reflections";
            case MODE_TELEPHONE: return "Classic retro landline narrow-band audio";
            default: return "Original natural studio-clean microphone voice";
        }
    }

    private static int getModeStartColor(int mode) {
        switch (mode) {
            case MODE_MALE: return 0xFF3B82F6;
            case MODE_FEMALE: return 0xFFEC4899;
            case MODE_DEEP: return 0xFF8B5CF6;
            case MODE_CHIPMUNK: return 0xFFD946EF;
            case MODE_ROBOT: return 0xFF06B6D4;
            case MODE_HELIUM: return 0xFFF43F5E;
            case MODE_CAVE: return 0xFF10B981;
            case MODE_TELEPHONE: return 0xFF6366F1;
            default: return 0xFF0EA5E9;
        }
    }

    private static int getModeEndColor(int mode) {
        switch (mode) {
            case MODE_MALE: return 0xFF1D4ED8;
            case MODE_FEMALE: return 0xFFBE185D;
            case MODE_DEEP: return 0xFF5B21B6;
            case MODE_CHIPMUNK: return 0xFFA21CAF;
            case MODE_ROBOT: return 0xFF0E7490;
            case MODE_HELIUM: return 0xFFE11D48;
            case MODE_CAVE: return 0xFF047857;
            case MODE_TELEPHONE: return 0xFF4338CA;
            default: return 0xFF0284C7;
        }
    }

    public static void showDialog(Context context, Runnable onChanged) {
        if (context == null) return;
        final boolean isDark = Theme.isCurrentThemeDark();
        final int initialMode = getSelectedMode(context);
        final int[] pendingMode = new int[] { initialMode };
        final boolean[] confirmedByButton = new boolean[] { false };

        LinearLayout dialogContainer = new LinearLayout(context);
        dialogContainer.setOrientation(LinearLayout.VERTICAL);
        dialogContainer.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(4));

        // Premium Studio Header Card (replaces plain AlertDialog title)
        LinearLayout headerCard = new LinearLayout(context);
        headerCard.setOrientation(LinearLayout.HORIZONTAL);
        headerCard.setGravity(Gravity.CENTER_VERTICAL);
        headerCard.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        GradientDrawable headerBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                isDark ? new int[]{0xFF1E293B, 0xFF0F172A} : new int[]{0xFFEFF6FF, 0xFFF5F3FF}
        );
        headerBg.setCornerRadius(AndroidUtilities.dp(16));
        headerBg.setStroke(AndroidUtilities.dp(1.2f), isDark ? 0xFF334155 : 0xFFC7D2FE);
        headerCard.setBackground(headerBg);

        VoiceIconView headerEmblem = new VoiceIconView(context, MODE_NORMAL);
        LinearLayout.LayoutParams emblemLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(42), AndroidUtilities.dp(42));
        emblemLp.rightMargin = AndroidUtilities.dp(12);
        headerCard.addView(headerEmblem, emblemLp);

        LinearLayout headerTextCol = new LinearLayout(context);
        headerTextCol.setOrientation(LinearLayout.VERTICAL);

        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView mainTitle = new TextView(context);
        mainTitle.setText("Voice Changer");
        mainTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18.5f);
        mainTitle.setTypeface(AndroidUtilities.bold());
        mainTitle.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        titleRow.addView(mainTitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView hdBadge = new TextView(context);
        hdBadge.setText("HD STUDIO");
        hdBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9.5f);
        hdBadge.setTypeface(AndroidUtilities.bold());
        hdBadge.setTextColor(0xFFFFFFFF);
        hdBadge.setPadding(AndroidUtilities.dp(7), AndroidUtilities.dp(2.5f), AndroidUtilities.dp(7), AndroidUtilities.dp(2.5f));
        GradientDrawable hdBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF2563EB, 0xFF7C3AED}
        );
        hdBg.setCornerRadius(AndroidUtilities.dp(8));
        hdBadge.setBackground(hdBg);
        LinearLayout.LayoutParams hdLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        hdLp.leftMargin = AndroidUtilities.dp(8);
        titleRow.addView(hdBadge, hdLp);
        headerTextCol.addView(titleRow);

        TextView subTitle = new TextView(context);
        subTitle.setText("Crystal-clear studio voice characters");
        subTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.2f);
        subTitle.setTextColor(isDark ? 0xFF94A3B8 : 0xFF475569);
        subTitle.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        headerTextCol.addView(subTitle);

        headerCard.addView(headerTextCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams headerCardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        headerCardLp.bottomMargin = AndroidUtilities.dp(10);
        dialogContainer.addView(headerCard, headerCardLp);

        // Live Active Character Banner
        LinearLayout activeBanner = new LinearLayout(context);
        activeBanner.setOrientation(LinearLayout.HORIZONTAL);
        activeBanner.setGravity(Gravity.CENTER_VERTICAL);
        activeBanner.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));

        final VoiceIconView bannerIcon = new VoiceIconView(context, initialMode);
        LinearLayout.LayoutParams bannerIconLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(30), AndroidUtilities.dp(30));
        bannerIconLp.rightMargin = AndroidUtilities.dp(10);
        activeBanner.addView(bannerIcon, bannerIconLp);

        LinearLayout bannerTextCol = new LinearLayout(context);
        bannerTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView bannerKicker = new TextView(context);
        bannerKicker.setText("ACTIVE CHARACTER");
        bannerKicker.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10f);
        bannerKicker.setTypeface(AndroidUtilities.bold());
        bannerKicker.setLetterSpacing(0.08f);
        bannerKicker.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        bannerTextCol.addView(bannerKicker);

        final TextView bannerTitle = new TextView(context);
        bannerTitle.setText(getModeTitle(initialMode));
        bannerTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
        bannerTitle.setTypeface(AndroidUtilities.bold());
        bannerTitle.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        bannerTitle.setSingleLine(true);
        bannerTitle.setEllipsize(TextUtils.TruncateAt.END);
        bannerTextCol.addView(bannerTitle);

        activeBanner.addView(bannerTextCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final SelectionTickView bannerTick = new SelectionTickView(context);
        bannerTick.setChecked(true, false);
        activeBanner.addView(bannerTick, new LinearLayout.LayoutParams(AndroidUtilities.dp(22), AndroidUtilities.dp(22)));

        updateBannerStyle(activeBanner, bannerKicker, initialMode, isDark);

        LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bannerLp.bottomMargin = AndroidUtilities.dp(10);
        dialogContainer.addView(activeBanner, bannerLp);

        // Scrollable voice character card list
        final int maxScrollHeight = Math.min(
                AndroidUtilities.dp(340),
                Math.max(AndroidUtilities.dp(240), (int) (AndroidUtilities.displaySize.y * 0.44f))
        );
        ScrollView scrollView = new ScrollView(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int capped = MeasureSpec.makeMeasureSpec(maxScrollHeight, MeasureSpec.AT_MOST);
                super.onMeasure(widthMeasureSpec, capped);
            }
        };
        scrollView.setVerticalScrollBarEnabled(true);
        scrollView.setScrollbarFadingEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        LinearLayout listRoot = new LinearLayout(context);
        listRoot.setOrientation(LinearLayout.VERTICAL);
        listRoot.setPadding(AndroidUtilities.dp(2), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(8));
        scrollView.addView(listRoot, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        dialogContainer.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        final LinearLayout[] rowViews = new LinearLayout[DISPLAY_MODES.length];
        final TextView[] titleViews = new TextView[DISPLAY_MODES.length];
        final SelectionTickView[] tickViews = new SelectionTickView[DISPLAY_MODES.length];

        for (int i = 0; i < DISPLAY_MODES.length; i++) {
            final int index = i;
            final int mode = DISPLAY_MODES[i];

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(11), AndroidUtilities.dp(10), AndroidUtilities.dp(11), AndroidUtilities.dp(10));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowLp.bottomMargin = AndroidUtilities.dp(7);
            row.setLayoutParams(rowLp);

            VoiceIconView iconView = new VoiceIconView(context, mode);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(40), AndroidUtilities.dp(40));
            iconLp.rightMargin = AndroidUtilities.dp(12);
            row.addView(iconView, iconLp);

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);

            TextView titleTv = new TextView(context);
            titleTv.setText(getModeTitle(mode));
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
            titleTv.setTypeface(AndroidUtilities.bold());
            titleTv.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
            titleTv.setSingleLine(true);
            titleTv.setEllipsize(TextUtils.TruncateAt.END);
            textCol.addView(titleTv);

            TextView descTv = new TextView(context);
            descTv.setText(getModeSubtitle(mode));
            descTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.8f);
            descTv.setTextColor(isDark ? 0xFF94A3B8 : 0xFF475569);
            descTv.setMaxLines(2);
            descTv.setPadding(0, AndroidUtilities.dp(1.5f), 0, 0);
            textCol.addView(descTv);

            row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            SelectionTickView tickView = new SelectionTickView(context);
            LinearLayout.LayoutParams tickLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(24), AndroidUtilities.dp(24));
            tickLp.leftMargin = AndroidUtilities.dp(10);
            row.addView(tickView, tickLp);

            rowViews[i] = row;
            titleViews[i] = titleTv;
            tickViews[i] = tickView;

            row.setOnClickListener(v -> {
                pendingMode[0] = mode;
                setSelectedMode(context, mode);
                resetSession();
                updateRowStates(rowViews, titleViews, tickViews, mode, isDark, index);
                bannerIcon.setMode(mode);
                bannerTitle.setText(getModeTitle(mode));
                updateBannerStyle(activeBanner, bannerKicker, mode, isDark);
                bannerTick.setChecked(true, true);
                if (onChanged != null) {
                    onChanged.run();
                }
            });

            listRoot.addView(row);
        }

        updateRowStates(rowViews, titleViews, tickViews, pendingMode[0], isDark, -1);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogContainer);
        builder.setPositiveButton("Select", (dialog, which) -> {
            confirmedByButton[0] = true;
            int selected = pendingMode[0];
            setSelectedMode(context, selected);
            resetSession();
            if (onChanged != null) {
                onChanged.run();
            }
            dialog.dismiss();
            showVoiceSelectedPopup(context, selected);
        });
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> {
            if (!confirmedByButton[0] && pendingMode[0] != initialMode) {
                showVoiceSelectedPopup(context, pendingMode[0]);
            }
        });
        dialog.show();
    }

    private static void updateBannerStyle(LinearLayout banner, TextView kicker, int mode, boolean isDark) {
        int accent = getModeStartColor(mode);
        int bgColor = isDark ? blendAlpha(accent, 0xFF0F172A, 0.16f) : blendAlpha(accent, 0xFFFFFFFF, 0.10f);
        int strokeColor = (accent & 0x00FFFFFF) | (isDark ? 0x66000000 : 0x44000000);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(14));
        bg.setColor(bgColor);
        bg.setStroke(AndroidUtilities.dp(1.2f), strokeColor);
        banner.setBackground(bg);
        kicker.setTextColor(isDark ? lightenColor(accent, 0.30f) : accent);
    }

    private static void updateRowStates(
            LinearLayout[] rowViews,
            TextView[] titleViews,
            SelectionTickView[] tickViews,
            int selectedMode,
            boolean isDark,
            int animateIndex
    ) {
        for (int i = 0; i < DISPLAY_MODES.length; i++) {
            int mode = DISPLAY_MODES[i];
            boolean selected = (mode == selectedMode);
            int accent = getModeStartColor(mode);

            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setCornerRadius(AndroidUtilities.dp(14));
            if (selected) {
                rowBg.setColor(isDark ? blendAlpha(accent, 0xFF0F172A, 0.22f) : blendAlpha(accent, 0xFFFFFFFF, 0.12f));
                rowBg.setStroke(AndroidUtilities.dp(1.8f), isDark ? lightenColor(accent, 0.20f) : accent);
                titleViews[i].setTextColor(isDark ? 0xFFFFFFFF : blendAlpha(accent, 0xFF0F172A, 0.72f));
            } else {
                rowBg.setColor(isDark ? 0xFF1E293B : 0xFFF8FAFC);
                rowBg.setStroke(AndroidUtilities.dp(1f), isDark ? 0xFF334155 : 0xFFE2E8F0);
                titleViews[i].setTextColor(isDark ? 0xFFF1F5F9 : 0xFF0F172A);
            }
            rowViews[i].setBackground(rowBg);
            tickViews[i].setChecked(selected, i == animateIndex);
        }
    }

    private static void showVoiceSelectedPopup(Context context, int mode) {
        if (context == null) return;
        if (context instanceof Activity) {
            Activity act = (Activity) context;
            if (act.isFinishing() || act.isDestroyed()) return;
        }
        try {
            final boolean isDark = Theme.isCurrentThemeDark();
            final int accentStart = getModeStartColor(mode);
            final int accentEnd = getModeEndColor(mode);
            final String title = getModeTitle(mode);

            final Dialog popup = new Dialog(context);
            popup.requestWindowFeature(Window.FEATURE_NO_TITLE);
            Window window = popup.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                WindowManager.LayoutParams wlp = window.getAttributes();
                wlp.gravity = Gravity.CENTER;
                wlp.width = WindowManager.LayoutParams.WRAP_CONTENT;
                wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.setAttributes(wlp);
            }

            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER_HORIZONTAL);
            card.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(20), AndroidUtilities.dp(24), AndroidUtilities.dp(18));

            GradientDrawable cardBg = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    isDark
                            ? new int[]{0xFF0F172A, 0xFF1E293B}
                            : new int[]{0xFFFFFFFF, 0xFFF8FAFC}
            );
            cardBg.setCornerRadius(AndroidUtilities.dp(22));
            cardBg.setStroke(AndroidUtilities.dp(1.6f), (accentStart & 0x00FFFFFF) | (isDark ? 0x99000000 : 0x55000000));
            card.setBackground(cardBg);

            // Radiant vector checkmark badge
            SelectionTickView checkBadge = new SelectionTickView(context);
            checkBadge.setBadgeColors(0xFF10B981, 0xFF059669);
            checkBadge.setChecked(true, false);
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(48), AndroidUtilities.dp(48));
            badgeLp.bottomMargin = AndroidUtilities.dp(12);
            card.addView(checkBadge, badgeLp);

            TextView headingTv = new TextView(context);
            headingTv.setText(mode == MODE_NORMAL ? "Normal Voice Restored" : "Voice Character Activated");
            headingTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.5f);
            headingTv.setTypeface(AndroidUtilities.bold());
            headingTv.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
            headingTv.setGravity(Gravity.CENTER);
            card.addView(headingTv);

            // Character Preview Pill with custom vector icon
            LinearLayout pill = new LinearLayout(context);
            pill.setOrientation(LinearLayout.HORIZONTAL);
            pill.setGravity(Gravity.CENTER);
            pill.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setCornerRadius(AndroidUtilities.dp(16));
            pillBg.setColor(isDark ? blendAlpha(accentStart, 0xFF0F172A, 0.22f) : blendAlpha(accentStart, 0xFFFFFFFF, 0.12f));
            pillBg.setStroke(AndroidUtilities.dp(1.2f), (accentStart & 0x00FFFFFF) | 0x66000000);
            pill.setBackground(pillBg);

            VoiceIconView pillIcon = new VoiceIconView(context, mode);
            LinearLayout.LayoutParams pillIconLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(28), AndroidUtilities.dp(28));
            pillIconLp.rightMargin = AndroidUtilities.dp(9);
            pill.addView(pillIcon, pillIconLp);

            TextView pillLabel = new TextView(context);
            pillLabel.setText(title);
            pillLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
            pillLabel.setTypeface(AndroidUtilities.bold());
            pillLabel.setTextColor(isDark ? lightenColor(accentStart, 0.35f) : accentEnd);
            pill.addView(pillLabel);

            LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            pillLp.topMargin = AndroidUtilities.dp(10);
            pillLp.bottomMargin = AndroidUtilities.dp(9);
            card.addView(pill, pillLp);

            TextView messageTv = new TextView(context);
            messageTv.setText(mode == MODE_NORMAL
                    ? "Outgoing voice notes will sound natural without effects"
                    : "Outgoing voice notes will now sound like " + title);
            messageTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
            messageTv.setTextColor(isDark ? 0xFF94A3B8 : 0xFF475569);
            messageTv.setGravity(Gravity.CENTER);
            messageTv.setMaxWidth(AndroidUtilities.dp(240));
            card.addView(messageTv);

            card.setOnClickListener(v -> {
                try { popup.dismiss(); } catch (Throwable ignored) {}
            });

            popup.setContentView(card);
            card.setScaleX(0.88f);
            card.setScaleY(0.88f);
            card.setAlpha(0f);
            popup.show();
            card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(170).start();

            AndroidUtilities.runOnUIThread(() -> {
                try {
                    if (context instanceof Activity) {
                        Activity act = (Activity) context;
                        if (act.isFinishing() || act.isDestroyed()) return;
                    }
                    if (popup.isShowing()) {
                        popup.dismiss();
                    }
                } catch (Throwable ignored) {}
            }, 1750);
        } catch (Throwable ignored) {}
    }

    private static int blendAlpha(int fg, int bg, float alpha) {
        float inv = 1f - alpha;
        int r = Math.round(Color.red(fg) * alpha + Color.red(bg) * inv);
        int g = Math.round(Color.green(fg) * alpha + Color.green(bg) * inv);
        int b = Math.round(Color.blue(fg) * alpha + Color.blue(bg) * inv);
        return Color.rgb(r, g, b);
    }

    private static int lightenColor(int color, float amount) {
        int r = Math.min(255, Math.round(Color.red(color) + (255 - Color.red(color)) * amount));
        int g = Math.min(255, Math.round(Color.green(color) + (255 - Color.green(color)) * amount));
        int b = Math.min(255, Math.round(Color.blue(color) + (255 - Color.blue(color)) * amount));
        return Color.rgb(r, g, b);
    }

    /**
     * Custom vector-drawn Voice Character Icon Badge (no plain Unicode emojis).
     */
    private static final class VoiceIconView extends View {
        private int mode;
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Path path = new Path();
        private int lastW = -1;
        private int lastMode = -1;

        VoiceIconView(Context context, int mode) {
            super(context);
            this.mode = mode;

            strokePaint.setColor(Color.WHITE);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeCap(Paint.Cap.ROUND);
            strokePaint.setStrokeJoin(Paint.Join.ROUND);

            fillPaint.setColor(Color.WHITE);
            fillPaint.setStyle(Paint.Style.FILL);

            glossPaint.setColor(0x28FFFFFF);
            glossPaint.setStyle(Paint.Style.STROKE);
        }

        void setMode(int mode) {
            if (this.mode != mode) {
                this.mode = mode;
                this.lastMode = -1;
                invalidate();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            if (lastW != w || lastMode != mode) {
                bgPaint.setShader(new LinearGradient(
                        0, 0, w, h,
                        getModeStartColor(mode),
                        getModeEndColor(mode),
                        Shader.TileMode.CLAMP
                ));
                lastW = w;
                lastMode = mode;
            }

            float size = Math.min(w, h);
            float r = size * 0.30f;
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, r, r, bgPaint);

            glossPaint.setStrokeWidth(Math.max(1.5f, size * 0.03f));
            float inset = glossPaint.getStrokeWidth() * 0.5f;
            rect.set(inset, inset, w - inset, h - inset);
            canvas.drawRoundRect(rect, r - inset, r - inset, glossPaint);

            float u = size / 24f;
            strokePaint.setStrokeWidth(Math.max(1.8f, 1.75f * u));
            path.reset();

            switch (mode) {
                case MODE_MALE: {
                    // Mars / Masculine voice circle + arrow + bass bar
                    canvas.drawCircle(9.8f * u, 13.8f * u, 4.3f * u, strokePaint);
                    canvas.drawLine(12.9f * u, 10.7f * u, 17.6f * u, 6.0f * u, strokePaint);
                    path.moveTo(13.6f * u, 6.0f * u);
                    path.lineTo(17.6f * u, 6.0f * u);
                    path.lineTo(17.6f * u, 10.0f * u);
                    canvas.drawPath(path, strokePaint);
                    // Subtle chest wave inside circle
                    canvas.drawLine(8.2f * u, 13.8f * u, 8.2f * u, 14.0f * u, strokePaint);
                    canvas.drawLine(9.8f * u, 12.4f * u, 9.8f * u, 15.2f * u, strokePaint);
                    canvas.drawLine(11.4f * u, 13.2f * u, 11.4f * u, 14.4f * u, strokePaint);
                    break;
                }
                case MODE_FEMALE: {
                    // Venus / Feminine symbol + bright harmonic wave
                    canvas.drawCircle(12.0f * u, 9.6f * u, 4.4f * u, strokePaint);
                    canvas.drawLine(12.0f * u, 14.0f * u, 12.0f * u, 19.6f * u, strokePaint);
                    canvas.drawLine(9.2f * u, 16.9f * u, 14.8f * u, 16.9f * u, strokePaint);
                    canvas.drawLine(10.3f * u, 9.6f * u, 10.3f * u, 9.8f * u, strokePaint);
                    canvas.drawLine(12.0f * u, 8.1f * u, 12.0f * u, 11.1f * u, strokePaint);
                    canvas.drawLine(13.7f * u, 9.6f * u, 13.7f * u, 9.8f * u, strokePaint);
                    break;
                }
                case MODE_DEEP: {
                    // Subwoofer / deep bass speaker driver + bass waves
                    canvas.drawCircle(10.0f * u, 12.0f * u, 5.2f * u, strokePaint);
                    canvas.drawCircle(10.0f * u, 12.0f * u, 2.0f * u, fillPaint);
                    rect.set(14.5f * u, 8.2f * u, 19.0f * u, 15.8f * u);
                    canvas.drawArc(rect, -55f, 110f, false, strokePaint);
                    rect.set(15.5f * u, 6.0f * u, 21.5f * u, 18.0f * u);
                    canvas.drawArc(rect, -55f, 110f, false, strokePaint);
                    break;
                }
                case MODE_CHIPMUNK: {
                    // Energetic high-pitch waveform equalizer bars
                    canvas.drawLine(5.5f * u, 13.0f * u, 5.5f * u, 11.0f * u, strokePaint);
                    canvas.drawLine(8.7f * u, 16.8f * u, 8.7f * u, 7.2f * u, strokePaint);
                    canvas.drawLine(12.0f * u, 19.0f * u, 12.0f * u, 5.0f * u, strokePaint);
                    canvas.drawLine(15.3f * u, 16.0f * u, 15.3f * u, 8.0f * u, strokePaint);
                    canvas.drawLine(18.5f * u, 13.5f * u, 18.5f * u, 10.5f * u, strokePaint);
                    break;
                }
                case MODE_ROBOT: {
                    // Geometric robot head with antenna & visor
                    canvas.drawLine(12.0f * u, 4.4f * u, 12.0f * u, 7.2f * u, strokePaint);
                    canvas.drawCircle(12.0f * u, 4.2f * u, 1.1f * u, fillPaint);
                    rect.set(6.0f * u, 7.3f * u, 18.0f * u, 17.6f * u);
                    canvas.drawRoundRect(rect, 3.0f * u, 3.0f * u, strokePaint);
                    canvas.drawCircle(9.6f * u, 11.4f * u, 1.1f * u, fillPaint);
                    canvas.drawCircle(14.4f * u, 11.4f * u, 1.1f * u, fillPaint);
                    canvas.drawLine(9.2f * u, 14.8f * u, 14.8f * u, 14.8f * u, strokePaint);
                    break;
                }
                case MODE_HELIUM: {
                    // Floating balloon silhouette + curled string
                    rect.set(7.4f * u, 4.5f * u, 16.6f * u, 14.6f * u);
                    canvas.drawOval(rect, strokePaint);
                    path.moveTo(12.0f * u, 14.8f * u);
                    path.quadTo(13.6f * u, 16.8f * u, 11.4f * u, 18.2f * u);
                    path.quadTo(10.0f * u, 19.2f * u, 12.2f * u, 20.2f * u);
                    canvas.drawPath(path, strokePaint);
                    // Highlight arc inside balloon
                    rect.set(9.3f * u, 6.4f * u, 13.5f * u, 11.0f * u);
                    canvas.drawArc(rect, 195f, 75f, false, strokePaint);
                    break;
                }
                case MODE_CAVE: {
                    // Mountain cave peaks + echo wave above
                    path.moveTo(4.6f * u, 17.6f * u);
                    path.lineTo(9.8f * u, 9.2f * u);
                    path.lineTo(13.2f * u, 14.2f * u);
                    path.lineTo(15.6f * u, 11.0f * u);
                    path.lineTo(19.4f * u, 17.6f * u);
                    path.close();
                    canvas.drawPath(path, strokePaint);
                    rect.set(9.0f * u, 5.2f * u, 15.0f * u, 9.6f * u);
                    canvas.drawArc(rect, 215f, 110f, false, strokePaint);
                    break;
                }
                case MODE_TELEPHONE: {
                    // Classic phone handset + signal waves
                    path.moveTo(6.5f * u, 8.2f * u);
                    path.cubicTo(6.5f * u, 13.6f * u, 10.4f * u, 17.5f * u, 15.8f * u, 17.5f * u);
                    path.lineTo(17.5f * u, 15.8f * u);
                    path.lineTo(14.8f * u, 13.4f * u);
                    path.lineTo(13.2f * u, 14.5f * u);
                    path.cubicTo(11.3f * u, 13.6f * u, 10.4f * u, 12.7f * u, 9.5f * u, 10.8f * u);
                    path.lineTo(10.6f * u, 9.2f * u);
                    path.lineTo(8.2f * u, 6.5f * u);
                    path.close();
                    canvas.drawPath(path, strokePaint);
                    rect.set(13.0f * u, 5.5f * u, 18.8f * u, 11.3f * u);
                    canvas.drawArc(rect, -85f, 80f, false, strokePaint);
                    break;
                }
                default: {
                    // MODE_NORMAL: Studio Microphone capsule + stand
                    rect.set(9.6f * u, 5.0f * u, 14.4f * u, 13.2f * u);
                    canvas.drawRoundRect(rect, 2.4f * u, 2.4f * u, strokePaint);
                    rect.set(7.4f * u, 7.8f * u, 16.6f * u, 15.8f * u);
                    canvas.drawArc(rect, 0f, 180f, false, strokePaint);
                    canvas.drawLine(12.0f * u, 15.8f * u, 12.0f * u, 19.0f * u, strokePaint);
                    canvas.drawLine(9.2f * u, 19.0f * u, 14.8f * u, 19.0f * u, strokePaint);
                    break;
                }
            }
        }
    }

    /**
     * Custom vector-drawn selection checkmark circle with optional pop animation.
     */
    private static final class SelectionTickView extends View {
        private boolean checked;
        private int startColor = 0xFF10B981;
        private int endColor = 0xFF059669;
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path checkPath = new Path();
        private int lastW = -1;

        SelectionTickView(Context context) {
            super(context);
            haloPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStyle(Paint.Style.STROKE);
            checkPaint.setStyle(Paint.Style.STROKE);
            checkPaint.setColor(Color.WHITE);
            checkPaint.setStrokeCap(Paint.Cap.ROUND);
            checkPaint.setStrokeJoin(Paint.Join.ROUND);
        }

        void setBadgeColors(int startColor, int endColor) {
            this.startColor = startColor;
            this.endColor = endColor;
            this.lastW = -1;
            invalidate();
        }

        void setChecked(boolean checked, boolean animate) {
            boolean changed = (this.checked != checked);
            this.checked = checked;
            invalidate();
            if (animate && checked && changed) {
                setScaleX(0.75f);
                setScaleY(0.75f);
                animate().scaleX(1.08f).scaleY(1.08f).setDuration(120)
                        .withEndAction(() -> animate().scaleX(1f).scaleY(1f).setDuration(90).start())
                        .start();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            float cx = w * 0.5f;
            float cy = h * 0.5f;
            float radius = Math.min(w, h) * 0.44f;

            if (checked) {
                if (lastW != w) {
                    fillPaint.setShader(new LinearGradient(
                            0, 0, w, h, startColor, endColor, Shader.TileMode.CLAMP
                    ));
                    lastW = w;
                }
                haloPaint.setColor((startColor & 0x00FFFFFF) | 0x38000000);
                haloPaint.setStrokeWidth(Math.max(2f, w * 0.08f));
                canvas.drawCircle(cx, cy, radius + haloPaint.getStrokeWidth() * 0.3f, haloPaint);
                canvas.drawCircle(cx, cy, radius, fillPaint);

                checkPaint.setStrokeWidth(Math.max(2f, w * 0.11f));
                checkPath.reset();
                checkPath.moveTo(w * 0.29f, h * 0.52f);
                checkPath.lineTo(w * 0.44f, h * 0.67f);
                checkPath.lineTo(w * 0.72f, h * 0.36f);
                canvas.drawPath(checkPath, checkPaint);
            } else {
                boolean dark = Theme.isCurrentThemeDark();
                ringPaint.setColor(dark ? 0xFF475569 : 0xFFCBD5E1);
                ringPaint.setStrokeWidth(Math.max(1.5f, w * 0.075f));
                canvas.drawCircle(cx, cy, radius - ringPaint.getStrokeWidth() * 0.5f, ringPaint);
            }
        }
    }

    /**
     * Frame callback for streaming voice-changed PCM frames into the native Opus encoder.
     */
    public interface FrameSink {
        void write(ByteBuffer frame, int byteCount);
    }

    /**
     * Per-recording streaming session for Telegram's AudioCapture (V2 recorder).
     * Runs AFTER RNNoise so background microphone noise is already removed before vocal shaping.
     * Uses NGSonic (Apache-2.0 PICOLA pitch-synchronous speech engine) for crystal-clear voice
     * shifting without granular warble, buzz, or clipping, then drains the tail cleanly on finish().
     */
    public static final class Session {
        private final StudioDsp dsp;
        private final ByteBuffer outBuf;
        private short[] inShorts;
        private long outputSamples = 0L;

        private Session(StudioDsp dsp, int frameSamples) {
            this.dsp = dsp;
            this.outBuf = ByteBuffer.allocateDirect(frameSamples * 2).order(ByteOrder.nativeOrder());
            this.inShorts = new short[frameSamples];
        }

        public long getOutputSamples() {
            return outputSamples;
        }

        public long lengthOf(long micSamples) {
            float speed = dsp.speed;
            return speed == 1.0f ? micSamples : (long) (micSamples / speed);
        }

        public void push(ByteBuffer pcm, int byteCount, FrameSink sink) {
            if (pcm == null || byteCount < 2 || sink == null) return;
            int samples = byteCount >> 1;
            if (inShorts.length < samples) {
                inShorts = new short[samples];
            }
            ByteOrder prevOrder = pcm.order();
            pcm.order(ByteOrder.nativeOrder());
            pcm.position(0);
            pcm.limit(samples << 1);
            pcm.asShortBuffer().get(inShorts, 0, samples);
            pcm.order(prevOrder);

            short[] processed = dsp.process(inShorts, samples);
            emitSamples(processed, sink);
        }

        public void finish(FrameSink sink) {
            if (sink == null) return;
            short[] tail = dsp.drain();
            emitSamples(tail, sink);
            if (outBuf.position() > 0) {
                emitBuffer(outBuf.position(), sink);
            }
        }

        private void emitSamples(short[] samples, FrameSink sink) {
            if (samples == null || samples.length == 0) return;
            for (int i = 0; i < samples.length; i++) {
                outBuf.putShort(samples[i]);
                if (!outBuf.hasRemaining()) {
                    emitBuffer(outBuf.capacity(), sink);
                }
            }
        }

        private void emitBuffer(int bytes, FrameSink sink) {
            outBuf.position(0);
            outBuf.limit(bytes);
            sink.write(outBuf, bytes);
            outputSamples += (bytes >> 1);
            outBuf.clear();
        }
    }

    /**
     * Direct variable-length PCM processor for legacy MediaController AudioRecord path
     * (zero-dropout streaming with full tail drain on stop).
     */
    public static final class DirectEffect {
        private final StudioDsp dsp;

        private DirectEffect(StudioDsp dsp) {
            this.dsp = dsp;
        }

        public short[] process(short[] input, int count) {
            return dsp.process(input, count);
        }

        public short[] drain() {
            return dsp.drain();
        }
    }

    /**
     * Creates a direct streaming effect for MediaController's AudioRecord path,
     * or returns {@code null} when Normal Voice (bypass) is active.
     */
    public static DirectEffect createDirectEffect(int sampleRate) {
        int mode = getSelectedMode(ApplicationLoader.applicationContext);
        if (mode == MODE_NORMAL) {
            return null;
        }
        int sr = sampleRate > 0 ? sampleRate : DEFAULT_SAMPLE_RATE;
        return new DirectEffect(new StudioDsp(mode, sr));
    }

    /**
     * Creates a new streaming voice changer session for the currently selected character,
     * or returns {@code null} when Normal Voice (bypass) is active.
     */
    public static Session createSession(int sampleRate, int frameSamples) {
        int mode = getSelectedMode(ApplicationLoader.applicationContext);
        if (mode == MODE_NORMAL) {
            return null;
        }
        int sr = sampleRate > 0 ? sampleRate : DEFAULT_SAMPLE_RATE;
        int fs = frameSamples > 0 ? frameSamples : 960;
        return new Session(new StudioDsp(mode, sr), fs);
    }

    public static void resetSession() {
        synchronized (DSP_LOCK) {
            legacyDsp = null;
            legacyDspMode = MODE_NORMAL;
        }
    }

    /**
     * In-place PCM processing fallback (with jitter-cushion queue so pitch/formant frames never drop out).
     */
    public static void processPcmBuffer(ByteBuffer buffer, int byteCount) {
        processPcmBuffer(buffer, byteCount, DEFAULT_SAMPLE_RATE);
    }

    public static void processPcmBuffer(ByteBuffer buffer, int byteCount, int sampleRate) {
        if (buffer == null || byteCount < 2) return;
        int mode = getSelectedMode(ApplicationLoader.applicationContext);
        if (mode == MODE_NORMAL) return;

        final int samples = byteCount >> 1;
        final int sr = sampleRate > 0 ? sampleRate : DEFAULT_SAMPLE_RATE;
        synchronized (DSP_LOCK) {
            if (legacyDsp == null || legacyDspMode != mode || legacyDspSampleRate != sr) {
                legacyDsp = new StudioDsp(mode, sr);
                legacyDspMode = mode;
                legacyDspSampleRate = sr;
            }
            ByteOrder prevOrder = buffer.order();
            buffer.order(ByteOrder.nativeOrder());
            buffer.position(0);
            buffer.limit(samples << 1);
            short[] in = new short[samples];
            buffer.asShortBuffer().get(in, 0, samples);

            short[] out = legacyDsp.processFixedLength(in, samples);
            buffer.position(0);
            buffer.limit(samples << 1);
            buffer.asShortBuffer().put(out, 0, samples);
            buffer.order(prevOrder);
            buffer.position(0);
        }
    }

    /**
     * Studio-grade real-time vocal DSP engine combining:
     * 1. Cepstral STFT FormantWarp (independent vocal tract length control for genuine Male/Female/Helium/Deep voices)
     * 2. NGSonic (PICOLA pitch-synchronous pitch shifting with zero granular buzz)
     * 3. Zero-phase STFT Spectral Vocoder (clean robotic voice with zero static)
     * 4. Freeverb (8 damped feedback combs + 4 series allpasses + 200 Hz HPF for lush Cave acoustics)
     * 5. Exact RBJ Butterworth bandpass (clean Telephone) & soft-knee tanh limiter
     */
    private static final class StudioDsp {
        private static final float WARP_HEADROOM = 0.5f;

        final int mode;
        final int sampleRate;
        final float speed;
        final float outputGain;
        private final float inputScale;

        private final FormantWarp warp;
        private int warpLagLeft;
        private final NGSonic sonic;
        private final SpectralRobot spectralRobot;
        private final Freeverb caveReverb;
        private final int spectralLag;
        private final int fadeLength;

        // Gentle 65 Hz DC & breath-plosive high-pass before analysis
        private final float dcAlpha;
        private float dcPrevIn = 0f;
        private float dcPrevOut = 0f;

        // Telephone 2nd-order Butterworth bandpass biquad (340 Hz - 3100 Hz)
        private final float bpB0, bpB1, bpB2, bpA1, bpA2;
        private float bpX1 = 0f, bpX2 = 0f, bpY1 = 0f, bpY2 = 0f;

        // Jitter-buffered FIFO for fixed-length fallback callers
        private short[] legacyQueue = new short[32768];
        private int legacyQueued = 0;
        private boolean legacyStarted = false;

        StudioDsp(int mode, int sampleRate) {
            this.mode = mode;
            this.sampleRate = sampleRate;

            float semitones = 0f;
            Float formantSemitones = null;
            float gain = 1.0f;
            float sp = 1.0f;

            switch (mode) {
                case MODE_MALE:
                    // Genuine adult male: deep -6 st pitch with natural -3 st vocal tract formants (zero monster mud)
                    semitones = -6.0f;
                    formantSemitones = -3.0f;
                    gain = 1.02f;
                    break;
                case MODE_FEMALE:
                    // Genuine adult female: bright +9 st pitch with natural +3.5 st vocal tract formants (zero chipmunk)
                    semitones = 9.0f;
                    formantSemitones = 3.5f;
                    gain = 1.10f;
                    break;
                case MODE_DEEP:
                    // Deep cinematic narrator voice: -8 st pitch, -4.5 st formants
                    semitones = -8.0f;
                    formantSemitones = -4.5f;
                    gain = 1.04f;
                    break;
                case MODE_HELIUM:
                    // Real helium gas acoustics: formants jump +9 st while pitch rises +3 st
                    semitones = 3.0f;
                    formantSemitones = 9.0f;
                    gain = 0.96f;
                    break;
                case MODE_CHIPMUNK:
                    // Playful crisp cartoon voice: +12 st pitch with +5.5 st formants (clear, never shrill)
                    semitones = 12.0f;
                    formantSemitones = 5.5f;
                    gain = 1.08f;
                    break;
                case MODE_ROBOT:
                    gain = 1.15f;
                    break;
                case MODE_CAVE:
                    gain = 1.0f;
                    break;
                case MODE_TELEPHONE:
                    gain = 1.02f;
                    break;
                default:
                    break;
            }

            this.speed = sp;
            this.outputGain = gain;

            // NGSonic moves formants along with pitch, so FormantWarp shifts only the residual difference
            if (formantSemitones != null && Math.abs(formantSemitones - semitones) > 0.05f) {
                float warpFactor = (float) Math.pow(2.0, (formantSemitones - semitones) / 12.0);
                this.warp = new FormantWarp(warpFactor, sampleRate);
                this.warpLagLeft = this.warp.size;
                this.inputScale = 1.0f / WARP_HEADROOM;
            } else {
                this.warp = null;
                this.warpLagLeft = 0;
                this.inputScale = 1.0f;
            }

            if (semitones != 0f || sp != 1.0f) {
                float pitchFactor = (float) Math.pow(2.0, semitones / 12.0);
                this.sonic = new NGSonic(sampleRate, 1, sp, pitchFactor, sampleRate);
            } else {
                this.sonic = null;
            }

            if (mode == MODE_ROBOT) {
                int hop = Math.max(64, Math.round(sampleRate / 120.0f));
                this.spectralRobot = new SpectralRobot(1024, hop);
                this.spectralLag = this.spectralRobot.size;
            } else {
                this.spectralRobot = null;
                this.spectralLag = 0;
            }

            if (mode == MODE_CAVE) {
                this.caveReverb = new Freeverb(sampleRate, 0.70f, 0.30f, 35.0f);
                this.fadeLength = sampleRate;
            } else {
                this.caveReverb = null;
                this.fadeLength = 0;
            }

            this.dcAlpha = (float) Math.exp(-2.0 * Math.PI * 65.0 / sampleRate);

            // Exact RBJ Audio EQ Cookbook Bandpass (center ~1150 Hz, Q ~0.85 -> ~340 Hz to 3100 Hz)
            double w0 = 2.0 * Math.PI * 1150.0 / sampleRate;
            double alpha = Math.sin(w0) / (2.0 * 0.85);
            double a0 = 1.0 + alpha;
            this.bpB0 = (float) (alpha / a0);
            this.bpB1 = 0f;
            this.bpB2 = (float) (-alpha / a0);
            this.bpA1 = (float) (-2.0 * Math.cos(w0) / a0);
            this.bpA2 = (float) ((1.0 - alpha) / a0);
        }

        short[] process(short[] input, int count) {
            if (input == null || count <= 0) {
                return new short[0];
            }

            // 1. Clean 65 Hz DC/rumble removal so pitch tracking & formant envelopes stay rock-steady
            short[] clean = new short[count];
            for (int i = 0; i < count; i++) {
                float x = input[i] / 32768f;
                float hp = dcAlpha * (dcPrevOut + x - dcPrevIn);
                dcPrevIn = x;
                dcPrevOut = hp;
                clean[i] = floatToShort(hp);
            }

            // 2. Cepstral FormantWarp (if active) with headroom protection and leading-lag trim
            short[] source = (warp != null) ? runWarp(clean, count) : clean;

            // 3. Pitch-synchronous shifting via NGSonic (zero granular buzzing)
            short[] pitched;
            if (sonic != null) {
                if (source.length > 0) {
                    sonic.queueInput(ShortBuffer.wrap(source, 0, source.length));
                }
                pitched = readSonicOutput();
            } else {
                pitched = source;
            }

            // 4. Clean post stages (Spectral Robot / Freeverb Cave / Telephone) + soft-knee limiter
            applyStages(pitched);
            return pitched;
        }

        short[] drain() {
            short[] warpRest = (warp != null) ? runWarp(new short[warp.size], warp.size) : new short[0];
            short[] rest;
            if (sonic != null) {
                if (warpRest.length > 0) {
                    sonic.queueInput(ShortBuffer.wrap(warpRest, 0, warpRest.length));
                }
                sonic.queueEndOfStream();
                rest = readSonicOutput();
            } else {
                rest = warpRest;
            }

            short[] out = new short[rest.length + spectralLag + fadeLength];
            System.arraycopy(rest, 0, out, 0, rest.length);
            applyStages(out);
            if (fadeLength > 0) {
                int fadeStart = rest.length + spectralLag;
                for (int i = 0; i < fadeLength; i++) {
                    float fade = 1f - (float) i / (float) fadeLength;
                    out[fadeStart + i] = (short) (out[fadeStart + i] * fade);
                }
            }
            return out;
        }

        short[] processFixedLength(short[] input, int count) {
            short[] produced = process(input, count);
            if (produced.length > 0) {
                if (legacyQueued + produced.length > legacyQueue.length) {
                    int nextCap = Math.max(legacyQueue.length * 2, legacyQueued + produced.length);
                    short[] grown = new short[nextCap];
                    System.arraycopy(legacyQueue, 0, grown, 0, legacyQueued);
                    legacyQueue = grown;
                }
                System.arraycopy(produced, 0, legacyQueue, legacyQueued, produced.length);
                legacyQueued += produced.length;
            }
            // Wait until queue holds a 2-frame cushion before outputting so Sonic/Warp never click or underrun
            if (!legacyStarted && legacyQueued >= count * 2) {
                legacyStarted = true;
            }
            if (legacyQueued > count * 8) {
                int drop = legacyQueued - count * 2;
                System.arraycopy(legacyQueue, drop, legacyQueue, 0, legacyQueued - drop);
                legacyQueued -= drop;
            }
            short[] out = new short[count];
            if (!legacyStarted) {
                return out;
            }
            int take = Math.min(count, legacyQueued);
            if (take > 0) {
                System.arraycopy(legacyQueue, 0, out, 0, take);
                System.arraycopy(legacyQueue, take, legacyQueue, 0, legacyQueued - take);
                legacyQueued -= take;
            }
            return out;
        }

        private short[] runWarp(short[] input, int count) {
            int skip = Math.min(warpLagLeft, count);
            short[] out = new short[count - skip];
            for (int i = 0; i < count; i++) {
                float y = warp.process(input[i] / 32768f) * WARP_HEADROOM;
                if (i >= skip) {
                    out[i - skip] = floatToShort(y);
                }
            }
            warpLagLeft -= skip;
            return out;
        }

        private short[] readSonicOutput() {
            int outBytes = sonic.getOutputSize();
            int outSamples = outBytes >> 1;
            if (outSamples <= 0) {
                return new short[0];
            }
            short[] out = new short[outSamples];
            sonic.getOutput(ShortBuffer.wrap(out));
            return out;
        }

        private void applyStages(short[] samples) {
            if (samples == null || samples.length == 0) return;
            for (int i = 0; i < samples.length; i++) {
                float x = (samples[i] / 32768f) * inputScale;
                if (spectralRobot != null) {
                    x = spectralRobot.process(x);
                } else if (caveReverb != null) {
                    x = x * 0.85f + caveReverb.process(x) * 0.30f;
                } else if (mode == MODE_TELEPHONE) {
                    float y = bpB0 * x + bpB1 * bpX1 + bpB2 * bpX2 - bpA1 * bpY1 - bpA2 * bpY2;
                    bpX2 = bpX1;
                    bpX1 = x;
                    bpY2 = bpY1;
                    bpY1 = y;
                    x = (float) Math.tanh(y * 1.35) * 0.94f;
                }
                x *= outputGain;
                samples[i] = floatToShort(softLimit(x));
            }
        }

        private static float softLimit(float x) {
            float a = Math.abs(x);
            if (a <= 0.90f) return x;
            float bent = 0.90f + 0.10f * (float) Math.tanh((a - 0.90f) / 0.10f);
            return x < 0f ? -bent : bent;
        }

        private static short floatToShort(float x) {
            if (x >= 1.0f) return 32767;
            if (x <= -1.0f) return -32768;
            return (short) (x * 32767f);
        }
    }

    /**
     * Cepstral STFT Formant Warper: shifts vocal tract resonances (formants) independently of pitch.
     * 75% overlapping Hann windows, 1.5 ms cepstral lifter (excluding harmonics), clamped +/-18 dB log gain
     * so quiet room noise between words is never amplified.
     */
    private static final class FormantWarp {
        private static final float LIFTER_SECONDS = 0.0015f;
        private static final int LIFTER_TAPER = 16;
        private static final float MAX_LOG_GAIN = 2.0794415f; // ln(8)

        private final float factor;
        final int size;
        private final int hop;
        private final int half;
        private final int lifter;
        private final float scale;

        private final float[] window;
        private final float[] input;
        private final float[] output;
        private final float[] re;
        private final float[] im;
        private final float[] cepstrumRe;
        private final float[] cepstrumIm;
        private final float[] envelope;
        private final Radix2Fft fft;
        private int pos = 0;
        private int sinceFrame = 0;

        FormantWarp(float factor, int sampleRate) {
            this.factor = factor;
            this.size = sampleRate > 32000 ? 2048 : 1024;
            this.hop = size / 4;
            this.half = size / 2;
            this.lifter = Math.max(8, Math.round(sampleRate * LIFTER_SECONDS));
            this.scale = hop / (0.375f * size);
            this.window = new float[size];
            for (int i = 0; i < size; i++) {
                this.window[i] = (float) (0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / size));
            }
            this.input = new float[size];
            this.output = new float[size];
            this.re = new float[size];
            this.im = new float[size];
            this.cepstrumRe = new float[size];
            this.cepstrumIm = new float[size];
            this.envelope = new float[half + 1];
            this.fft = new Radix2Fft(size);
        }

        float process(float x) {
            input[pos] = x;
            float y = output[pos];
            output[pos] = 0f;
            pos = (pos + 1) % size;
            if (++sinceFrame == hop) {
                sinceFrame = 0;
                processFrame();
            }
            return y;
        }

        private void processFrame() {
            for (int i = 0; i < size; i++) {
                re[i] = input[(pos + i) % size] * window[i];
                im[i] = 0f;
            }
            fft.transform(re, im, false);

            for (int k = 0; k <= half; k++) {
                float mag = (float) Math.hypot(re[k], im[k]);
                float level = (float) Math.log(mag + 1e-9f);
                cepstrumRe[k] = level;
                cepstrumIm[k] = 0f;
                if (k > 0 && k < half) {
                    cepstrumRe[size - k] = level;
                    cepstrumIm[size - k] = 0f;
                }
            }
            fft.transform(cepstrumRe, cepstrumIm, true);

            for (int n = 0; n < size; n++) {
                int q = (n <= half) ? n : (size - n);
                if (q < lifter) {
                    // keep full spectral envelope
                } else if (q < lifter + LIFTER_TAPER) {
                    cepstrumRe[n] *= (float) (0.5 + 0.5 * Math.cos(Math.PI * (q - lifter) / LIFTER_TAPER));
                } else {
                    cepstrumRe[n] = 0f;
                }
                cepstrumIm[n] = 0f;
            }
            fft.transform(cepstrumRe, cepstrumIm, false);

            for (int k = 0; k <= half; k++) {
                envelope[k] = cepstrumRe[k];
            }

            for (int k = 0; k <= half; k++) {
                float from = k / factor;
                int below = (int) from;
                float target;
                if (below >= half) {
                    target = envelope[half];
                } else {
                    target = envelope[below] + (envelope[below + 1] - envelope[below]) * (from - below);
                }
                float diff = target - envelope[k];
                if (diff > MAX_LOG_GAIN) diff = MAX_LOG_GAIN;
                else if (diff < -MAX_LOG_GAIN) diff = -MAX_LOG_GAIN;
                float gain = (float) Math.exp(diff);
                re[k] *= gain;
                im[k] *= gain;
                if (k > 0 && k < half) {
                    re[size - k] = re[k];
                    im[size - k] = -im[k];
                }
            }
            fft.transform(re, im, true);
            for (int i = 0; i < size; i++) {
                output[(pos + i) % size] += re[i] * window[i] * scale;
            }
        }
    }

    /**
     * Zero-phase STFT Spectral Robot Vocoder (clean robotic timbre with zero buzz/static).
     */
    private static final class SpectralRobot {
        final int size;
        private final int hop;
        private final int half;
        private final float scale;
        private final float[] window;
        private final float[] input;
        private final float[] output;
        private final float[] re;
        private final float[] im;
        private final Radix2Fft fft;
        private int pos = 0;
        private int sinceFrame = 0;

        SpectralRobot(int size, int hop) {
            this.size = size;
            this.hop = hop;
            this.half = size / 2;
            this.scale = hop / (0.375f * size);
            this.window = new float[size];
            for (int i = 0; i < size; i++) {
                this.window[i] = (float) (0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / size));
            }
            this.input = new float[size];
            this.output = new float[size];
            this.re = new float[size];
            this.im = new float[size];
            this.fft = new Radix2Fft(size);
        }

        float process(float x) {
            input[pos] = x;
            float y = output[pos];
            output[pos] = 0f;
            pos = (pos + 1) % size;
            if (++sinceFrame == hop) {
                sinceFrame = 0;
                processFrame();
            }
            return y;
        }

        private void processFrame() {
            for (int i = 0; i < size; i++) {
                re[i] = input[(pos + i) % size] * window[i];
                im[i] = 0f;
            }
            fft.transform(re, im, false);
            re[0] = 0f;
            im[0] = 0f;
            for (int k = 1; k <= half; k++) {
                float mag = (float) Math.hypot(re[k], im[k]);
                re[k] = (k % 2 == 0) ? mag : -mag;
                im[k] = 0f;
                if (k < half) {
                    re[size - k] = re[k];
                    im[size - k] = 0f;
                }
            }
            fft.transform(re, im, true);
            for (int i = 0; i < size; i++) {
                output[(pos + i) % size] += re[i] * window[i] * scale;
            }
        }
    }

    /**
     * In-place power-of-two Radix-2 FFT.
     */
    private static final class Radix2Fft {
        private final int size;
        private final float[] cosTable;
        private final float[] sinTable;
        private final int[] reversed;

        Radix2Fft(int size) {
            this.size = size;
            this.cosTable = new float[size / 2];
            this.sinTable = new float[size / 2];
            for (int i = 0; i < size / 2; i++) {
                double angle = 2.0 * Math.PI * i / size;
                this.cosTable[i] = (float) Math.cos(angle);
                this.sinTable[i] = (float) Math.sin(angle);
            }
            this.reversed = new int[size];
            int j = 0;
            for (int i = 1; i < size; i++) {
                int bit = size >> 1;
                while ((j & bit) != 0) {
                    j ^= bit;
                    bit >>= 1;
                }
                j ^= bit;
                this.reversed[i] = j;
            }
        }

        void transform(float[] re, float[] im, boolean inverse) {
            for (int i = 0; i < size; i++) {
                int j = reversed[i];
                if (i < j) {
                    float tr = re[i];
                    re[i] = re[j];
                    re[j] = tr;
                    float ti = im[i];
                    im[i] = im[j];
                    im[j] = ti;
                }
            }
            for (int length = 2; length <= size; length <<= 1) {
                int half = length >> 1;
                int step = size / length;
                for (int start = 0; start < size; start += length) {
                    for (int k = 0; k < half; k++) {
                        float wr = cosTable[k * step];
                        float wi = inverse ? sinTable[k * step] : -sinTable[k * step];
                        int a = start + k;
                        int b = a + half;
                        float tr = re[b] * wr - im[b] * wi;
                        float ti = re[b] * wi + im[b] * wr;
                        re[b] = re[a] - tr;
                        im[b] = im[a] - ti;
                        re[a] += tr;
                        im[a] += ti;
                    }
                }
            }
            if (inverse) {
                float s = 1f / size;
                for (int i = 0; i < size; i++) {
                    re[i] *= s;
                    im[i] *= s;
                }
            }
        }
    }

    /**
     * Freeverb (Jezar at Dreampoint, public domain): 8 damped parallel combs + 4 series allpasses + 200 Hz HPF.
     */
    private static final class Freeverb {
        private static final int[] COMB_TUNINGS = {1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617};
        private static final int[] ALLPASS_TUNINGS = {556, 441, 341, 225};

        private final float[][] combs;
        private final int[] combPos;
        private final float[] combStore;
        private final float[][] allpasses;
        private final int[] allpassPos;
        private final float feedback;
        private final float damp;
        private final float[] preDelay;
        private int preDelayPos = 0;
        private final float hpCoef;
        private float hpOut = 0f;
        private float hpIn = 0f;

        Freeverb(int sampleRate, float room, float dampParam, float preDelayMs) {
            float scale = sampleRate / 44100f;
            this.combs = new float[COMB_TUNINGS.length][];
            for (int i = 0; i < COMB_TUNINGS.length; i++) {
                this.combs[i] = new float[Math.max(16, (int) (COMB_TUNINGS[i] * scale))];
            }
            this.combPos = new int[COMB_TUNINGS.length];
            this.combStore = new float[COMB_TUNINGS.length];
            this.allpasses = new float[ALLPASS_TUNINGS.length][];
            for (int i = 0; i < ALLPASS_TUNINGS.length; i++) {
                this.allpasses[i] = new float[Math.max(16, (int) (ALLPASS_TUNINGS[i] * scale))];
            }
            this.allpassPos = new int[ALLPASS_TUNINGS.length];
            this.feedback = room * 0.28f + 0.70f;
            this.damp = dampParam * 0.40f;
            this.preDelay = preDelayMs > 0f ? new float[Math.max(1, (int) (sampleRate * preDelayMs / 1000f))] : null;
            this.hpCoef = (float) Math.exp(-2.0 * Math.PI * 200.0 / sampleRate);
        }

        float process(float dry) {
            hpOut = hpCoef * (hpOut + dry - hpIn);
            hpIn = dry;
            float x = hpOut;
            float input = x;
            if (preDelay != null) {
                float delayed = preDelay[preDelayPos];
                preDelay[preDelayPos] = x;
                preDelayPos = (preDelayPos + 1) % preDelay.length;
                input = delayed;
            }
            input *= 0.030f;
            float out = 0f;
            for (int c = 0; c < combs.length; c++) {
                float[] line = combs[c];
                int p = combPos[c];
                float y = line[p];
                combStore[c] = y * (1f - damp) + combStore[c] * damp;
                line[p] = input + combStore[c] * feedback;
                combPos[c] = (p + 1) % line.length;
                out += y;
            }
            for (int a = 0; a < allpasses.length; a++) {
                float[] line = allpasses[a];
                int p = allpassPos[a];
                float buffered = line[p];
                line[p] = out + buffered * 0.5f;
                allpassPos[a] = (p + 1) % line.length;
                out = buffered - out;
            }
            return out * 3.0f;
        }
    }
}
