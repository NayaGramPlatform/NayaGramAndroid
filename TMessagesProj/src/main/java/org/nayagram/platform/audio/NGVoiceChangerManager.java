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

    private static final int TONE_NONE = 0;
    private static final int TONE_BASS_DEEP = 1;
    private static final int TONE_MALE = 2;
    private static final int TONE_FEMALE = 3;

    private static final int SAMPLE_RATE = 48_000;
    private static final int PITCH_RING_SIZE = 4096;
    private static final int PITCH_RING_MASK = PITCH_RING_SIZE - 1;
    private static final int GRAIN_SIZE = 1024;
    private static final float MIN_DELAY = 64f;
    private static final float MAX_DELAY = MIN_DELAY + GRAIN_SIZE;
    private static final int DELAY_RING_SIZE = 16384;
    private static final int DELAY_RING_MASK = DELAY_RING_SIZE - 1;

    private static volatile int cachedMode = -1;

    private static final Object DSP_LOCK = new Object();
    private static final float[] pitchRing = new float[PITCH_RING_SIZE];
    private static final float[] delayRing = new float[DELAY_RING_SIZE];
    private static int pitchWriteIdx = 0;
    private static float readPosA = wrapRingStatic(-(MIN_DELAY + GRAIN_SIZE * 0.5f));
    private static float readPosB = wrapRingStatic(-(MIN_DELAY + GRAIN_SIZE));
    private static int delayIdx = 0;
    private static double robotPhase = 0.0;
    private static float lpPrev = 0f;
    private static float lpHighPrev = 0f;
    private static float hpPrevIn = 0f;
    private static float hpPrevOut = 0f;
    private static float bpX1 = 0f, bpX2 = 0f, bpY1 = 0f, bpY2 = 0f;

    private NGVoiceChangerManager() {}

    private static float wrapRingStatic(float pos) {
        float p = pos % PITCH_RING_SIZE;
        if (p < 0f) p += PITCH_RING_SIZE;
        return p;
    }

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
            case MODE_CHIPMUNK: return 0xFFF59E0B;
            case MODE_ROBOT: return 0xFF06B6D4;
            case MODE_HELIUM: return 0xFFF43F5E;
            case MODE_CAVE: return 0xFF10B981;
            case MODE_TELEPHONE: return 0xFF22C55E;
            default: return 0xFF0EA5E9;
        }
    }

    private static int getModeEndColor(int mode) {
        switch (mode) {
            case MODE_MALE: return 0xFF1D4ED8;
            case MODE_FEMALE: return 0xFFBE185D;
            case MODE_DEEP: return 0xFF5B21B6;
            case MODE_CHIPMUNK: return 0xFFD97706;
            case MODE_ROBOT: return 0xFF0E7490;
            case MODE_HELIUM: return 0xFFE11D48;
            case MODE_CAVE: return 0xFF047857;
            case MODE_TELEPHONE: return 0xFF15803D;
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
        dialogContainer.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(4), AndroidUtilities.dp(14), AndroidUtilities.dp(4));

        // Subtitle
        TextView subTitle = new TextView(context);
        subTitle.setText("Select a voice character for outgoing voice notes");
        subTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        subTitle.setTextColor(isDark ? 0xFF94A3B8 : 0xFF475569);
        subTitle.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), AndroidUtilities.dp(8));
        dialogContainer.addView(subTitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

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
        listRoot.setPadding(AndroidUtilities.dp(2), AndroidUtilities.dp(2), AndroidUtilities.dp(4), AndroidUtilities.dp(6));
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
        builder.setTitle("Voice Changer");
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

    public static void resetSession() {
        synchronized (DSP_LOCK) {
            java.util.Arrays.fill(pitchRing, 0f);
            java.util.Arrays.fill(delayRing, 0f);
            pitchWriteIdx = 0;
            readPosA = wrapRingStatic(-(MIN_DELAY + GRAIN_SIZE * 0.5f));
            readPosB = wrapRingStatic(-(MIN_DELAY + GRAIN_SIZE));
            delayIdx = 0;
            robotPhase = 0.0;
            lpPrev = 0f;
            lpHighPrev = 0f;
            hpPrevIn = 0f;
            hpPrevOut = 0f;
            bpX1 = 0f;
            bpX2 = 0f;
            bpY1 = 0f;
            bpY2 = 0f;
        }
    }

    /**
     * Processes 16-bit mono 48kHz PCM samples in-place inside {@code buffer} over {@code byteCount} bytes.
     * Leaves buffer position restored to 0 on exit.
     */
    public static void processPcmBuffer(ByteBuffer buffer, int byteCount) {
        if (buffer == null || byteCount < 2) return;
        int mode = getSelectedMode(ApplicationLoader.applicationContext);
        if (mode == MODE_NORMAL) return;

        final int samples = byteCount >> 1;
        synchronized (DSP_LOCK) {
            switch (mode) {
                case MODE_MALE:
                    processPitchShift(buffer, samples, 0.82f, TONE_MALE);
                    break;
                case MODE_FEMALE:
                    processPitchShift(buffer, samples, 1.24f, TONE_FEMALE);
                    break;
                case MODE_CHIPMUNK:
                    processPitchShift(buffer, samples, 1.48f, TONE_NONE);
                    break;
                case MODE_DEEP:
                    processPitchShift(buffer, samples, 0.72f, TONE_BASS_DEEP);
                    break;
                case MODE_HELIUM:
                    processPitchShift(buffer, samples, 1.72f, TONE_NONE);
                    break;
                case MODE_ROBOT:
                    processRobot(buffer, samples);
                    break;
                case MODE_CAVE:
                    processCaveEcho(buffer, samples);
                    break;
                case MODE_TELEPHONE:
                    processTelephone(buffer, samples);
                    break;
                default:
                    break;
            }
        }
        buffer.position(0);
    }

    private static void processPitchShift(ByteBuffer buffer, int samples, float pitchRatio, int toneMode) {
        final float halfGrain = GRAIN_SIZE * 0.5f;

        for (int i = 0; i < samples; i++) {
            int offset = i << 1;
            float in = buffer.getShort(offset);
            pitchRing[pitchWriteIdx & PITCH_RING_MASK] = in;

            float distA = wrapDistance(pitchWriteIdx, readPosA);
            float distB = wrapDistance(pitchWriteIdx, readPosB);

            if (pitchRatio > 1.0f) {
                if (distA < MIN_DELAY) {
                    readPosA = wrapRing(readPosA - GRAIN_SIZE);
                    distA += GRAIN_SIZE;
                }
                if (distA > MAX_DELAY + 64f) {
                    readPosA = wrapRing(pitchWriteIdx - (MIN_DELAY + halfGrain));
                    distA = MIN_DELAY + halfGrain;
                }
                if (distB < MIN_DELAY) {
                    readPosB = wrapRing(readPosB - GRAIN_SIZE);
                    distB += GRAIN_SIZE;
                }
                if (distB > MAX_DELAY + 64f) {
                    readPosB = wrapRing(pitchWriteIdx - MAX_DELAY);
                    distB = MAX_DELAY;
                }
            } else {
                if (distA > MAX_DELAY) {
                    readPosA = wrapRing(readPosA + GRAIN_SIZE);
                    distA -= GRAIN_SIZE;
                }
                if (distA < MIN_DELAY - 32f) {
                    readPosA = wrapRing(pitchWriteIdx - (MIN_DELAY + halfGrain));
                    distA = MIN_DELAY + halfGrain;
                }
                if (distB > MAX_DELAY) {
                    readPosB = wrapRing(readPosB + GRAIN_SIZE);
                    distB -= GRAIN_SIZE;
                }
                if (distB < MIN_DELAY - 32f) {
                    readPosB = wrapRing(pitchWriteIdx - MAX_DELAY);
                    distB = MAX_DELAY;
                }
            }

            float phaseA = Math.min(1f, Math.max(0f, (distA - MIN_DELAY) / GRAIN_SIZE));
            float phaseB = Math.min(1f, Math.max(0f, (distB - MIN_DELAY) / GRAIN_SIZE));
            float winA = 0.5f * (1f - (float) Math.cos(2.0 * Math.PI * phaseA));
            float winB = 0.5f * (1f - (float) Math.cos(2.0 * Math.PI * phaseB));
            float wSum = winA + winB;
            if (wSum < 0.001f) wSum = 1f;

            float sampleA = readInterpolated(readPosA);
            float sampleB = readInterpolated(readPosB);
            float out = (sampleA * winA + sampleB * winB) / wSum;

            if (toneMode == TONE_BASS_DEEP) {
                lpPrev = lpPrev + 0.35f * (out - lpPrev);
                out = 0.65f * out + 0.50f * lpPrev;
            } else if (toneMode == TONE_MALE) {
                // Warm natural chest resonance (~245 Hz shelf) + smooth natural upper clarity
                lpPrev = lpPrev + 0.032f * (out - lpPrev);
                lpHighPrev = lpHighPrev + 0.45f * (out - lpHighPrev);
                out = 0.80f * lpHighPrev + 0.34f * lpPrev;
            } else if (toneMode == TONE_FEMALE) {
                // Trim sub-145 Hz chest boom & add natural feminine brightness
                float hp = 0.9812f * (hpPrevOut + out - hpPrevIn);
                hpPrevIn = out;
                hpPrevOut = hp;
                lpHighPrev = lpHighPrev + 0.68f * (hp - lpHighPrev);
                out = hp * 0.86f + lpHighPrev * 0.22f;
            }

            buffer.putShort(offset, clampToShort(out));

            pitchWriteIdx = (pitchWriteIdx + 1) & PITCH_RING_MASK;
            readPosA = wrapRing(readPosA + pitchRatio);
            readPosB = wrapRing(readPosB + pitchRatio);
        }
    }

    private static float wrapRing(float pos) {
        if (pos >= PITCH_RING_SIZE) pos -= PITCH_RING_SIZE;
        if (pos < 0f) pos += PITCH_RING_SIZE;
        if (pos >= PITCH_RING_SIZE || pos < 0f) {
            pos = ((pos % PITCH_RING_SIZE) + PITCH_RING_SIZE) % PITCH_RING_SIZE;
        }
        return pos;
    }

    private static float wrapDistance(int writeIdx, float readPos) {
        float d = writeIdx - readPos;
        if (d < 0f) d += PITCH_RING_SIZE;
        return d;
    }

    private static float readInterpolated(float pos) {
        int i0 = ((int) pos) & PITCH_RING_MASK;
        int i1 = (i0 + 1) & PITCH_RING_MASK;
        float frac = pos - (int) pos;
        return pitchRing[i0] + frac * (pitchRing[i1] - pitchRing[i0]);
    }

    private static void processRobot(ByteBuffer buffer, int samples) {
        final double step = 2.0 * Math.PI * 65.0 / SAMPLE_RATE;
        for (int i = 0; i < samples; i++) {
            int offset = i << 1;
            float in = buffer.getShort(offset);
            float mod = (float) (0.70 * Math.sin(robotPhase) + 0.30 * Math.sin(robotPhase * 2.0));
            // Keep 28% dry signal so speech articulation stays crystal clear
            float out = (in * mod * 1.15f) + (in * 0.28f);
            buffer.putShort(offset, clampToShort(out));
            robotPhase += step;
            if (robotPhase >= 2.0 * Math.PI) {
                robotPhase -= 2.0 * Math.PI;
            }
        }
    }

    private static void processCaveEcho(ByteBuffer buffer, int samples) {
        final int tap1Offset = 5280;  // ~110ms
        final int tap2Offset = 10800; // ~225ms
        for (int i = 0; i < samples; i++) {
            int offset = i << 1;
            float in = buffer.getShort(offset);
            float e1 = delayRing[(delayIdx - tap1Offset) & DELAY_RING_MASK];
            float e2 = delayRing[(delayIdx - tap2Offset) & DELAY_RING_MASK];
            float wet = e1 * 0.45f + e2 * 0.30f;
            delayRing[delayIdx & DELAY_RING_MASK] = in + wet * 0.52f;
            float out = in * 0.85f + wet * 0.70f;
            buffer.putShort(offset, clampToShort(out));
            delayIdx = (delayIdx + 1) & DELAY_RING_MASK;
        }
    }

    private static void processTelephone(ByteBuffer buffer, int samples) {
        // Bandpass coefficients tuned for ~350 Hz - 3100 Hz @ 48 kHz
        final float b0 = 0.1602f, b1 = 0f, b2 = -0.1602f;
        final float a1 = -1.6485f, a2 = 0.6796f;
        for (int i = 0; i < samples; i++) {
            int offset = i << 1;
            float x0 = buffer.getShort(offset);
            float y0 = b0 * x0 + b1 * bpX1 + b2 * bpX2 - a1 * bpY1 - a2 * bpY2;
            bpX2 = bpX1;
            bpX1 = x0;
            bpY2 = bpY1;
            bpY1 = y0;
            // Subtle warm telephone saturation boost
            float norm = y0 / 32768f;
            float driven = (float) Math.tanh(norm * 2.2) * 32767f * 0.92f;
            buffer.putShort(offset, clampToShort(driven));
        }
    }

    private static short clampToShort(float value) {
        if (value > 32767f) return 32767;
        if (value < -32768f) return -32768;
        return (short) value;
    }
}
