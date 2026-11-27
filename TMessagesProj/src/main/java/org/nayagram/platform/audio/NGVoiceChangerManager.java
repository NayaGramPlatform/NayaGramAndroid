package org.nayagram.platform.audio;

import android.animation.ValueAnimator;
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
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Locale;

/**
 * NGVoiceChangerManager - Studio-grade 48 kHz mono PCM Voice Changer for NayaGram Voice Notes.
 *
 * Clean vocal pipeline:
 * 1. 2nd-order 95 Hz Butterworth HPF + Adaptive Soft-Knee Vocal Noise Gate (suppresses distant room noise
 *    instead of amplifying it, keeping the speaker's own voice loud and crystal clear).
 * 2. NGSonic (Apache-2.0 PICOLA) pitch & speed engine FIRST on the clean, phase-intact vocal waveform
 *    (zero STFT block ringing and zero metallic iron-clanging artifacts).
 * 3. Zero-latency minimum-phase RBJ Biquad vocal-tract formant EQ & speech-gated character FX
 *    (Man, Woman, Child, Mouse, Monster, Deep, Helium, Chipmunk, Robot, Alien, Hoarseness,
 *    Echo, Cave, Telephone, Record Speed, Modulation).
 * 4. Per-voice customizable Pitch, Speed, Effect Mix, Noise Gate, instant English Demo preview,
 *    and integrated Text-to-Voice Studio.
 */
public final class NGVoiceChangerManager {

    private static final String PREF_NAME = "nayagram_voice_changer_prefs";
    private static final String KEY_EFFECT_MODE = "voice_effect_mode";
    private static final String KEY_NOISE_GATE = "voice_noise_gate_v2";
    private static final String KEY_AUTO_DEMO = "voice_auto_demo_en";
    private static final String KEY_PITCH_PREFIX = "voice_custom_pitch_";
    private static final String KEY_SPEED_PREFIX = "voice_custom_speed_";
    private static final String KEY_FX_PREFIX = "voice_custom_fx_";

    public static final int MODE_NORMAL = 0;      // Real Voice (100% pure bypass)
    public static final int MODE_CHIPMUNK = 1;
    public static final int MODE_DEEP = 2;
    public static final int MODE_ROBOT = 3;
    public static final int MODE_HELIUM = 4;
    public static final int MODE_CAVE = 5;
    public static final int MODE_TELEPHONE = 6;
    public static final int MODE_MALE = 7;
    public static final int MODE_FEMALE = 8;
    public static final int MODE_CHILD = 9;
    public static final int MODE_MOUSE = 10;
    public static final int MODE_MONSTER = 11;
    public static final int MODE_ALIEN = 12;
    public static final int MODE_HOARSE = 13;
    public static final int MODE_ECHO = 14;
    public static final int MODE_SPEED = 15;
    public static final int MODE_MODULATION = 16;
    private static final int MODE_MAX = MODE_MODULATION;

    private static final int[] DISPLAY_MODES = new int[] {
            MODE_NORMAL,
            MODE_CHILD,
            MODE_MALE,
            MODE_FEMALE,
            MODE_MONSTER,
            MODE_MOUSE,
            MODE_HELIUM,
            MODE_CHIPMUNK,
            MODE_DEEP,
            MODE_ROBOT,
            MODE_ALIEN,
            MODE_HOARSE,
            MODE_ECHO,
            MODE_CAVE,
            MODE_TELEPHONE,
            MODE_SPEED,
            MODE_MODULATION
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

    public static boolean isNoiseGateEnabled(Context context) {
        try {
            SharedPreferences prefs = getPrefs(context);
            return prefs == null || prefs.getBoolean(KEY_NOISE_GATE, true);
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static void setNoiseGateEnabled(Context context, boolean enabled) {
        try {
            SharedPreferences prefs = getPrefs(context);
            if (prefs != null) prefs.edit().putBoolean(KEY_NOISE_GATE, enabled).apply();
        } catch (Throwable ignored) {}
    }

    public static boolean isAutoDemoEnabled(Context context) {
        try {
            SharedPreferences prefs = getPrefs(context);
            return prefs == null || prefs.getBoolean(KEY_AUTO_DEMO, true);
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static void setAutoDemoEnabled(Context context, boolean enabled) {
        try {
            SharedPreferences prefs = getPrefs(context);
            if (prefs != null) prefs.edit().putBoolean(KEY_AUTO_DEMO, enabled).apply();
        } catch (Throwable ignored) {}
    }

    // -------------------------------------------------------------------------
    // Per-Voice Defaults & Custom Persisted Parameters
    // -------------------------------------------------------------------------

    public static float getDefaultPitch(int mode) {
        switch (mode) {
            case MODE_MALE: return -4.5f;
            case MODE_FEMALE: return 5.5f;
            case MODE_CHILD: return 7.5f;
            case MODE_MOUSE: return 12.0f;
            case MODE_MONSTER: return -8.5f;
            case MODE_DEEP: return -6.5f;
            case MODE_HELIUM: return 10.0f;
            case MODE_CHIPMUNK: return 11.0f;
            case MODE_ROBOT: return -1.0f;
            case MODE_ALIEN: return 2.0f;
            case MODE_HOARSE: return -2.0f;
            case MODE_ECHO: return 0.0f;
            case MODE_CAVE: return -1.5f;
            case MODE_TELEPHONE: return 0.0f;
            case MODE_SPEED: return 3.0f;
            case MODE_MODULATION: return 6.0f;
            default: return 0.0f;
        }
    }

    public static float getDefaultSpeed(int mode) {
        switch (mode) {
            case MODE_CHILD: return 1.05f;
            case MODE_MOUSE: return 1.15f;
            case MODE_MONSTER: return 0.92f;
            case MODE_DEEP: return 0.97f;
            case MODE_HELIUM: return 1.06f;
            case MODE_CHIPMUNK: return 1.10f;
            case MODE_SPEED: return 1.22f;
            default: return 1.00f;
        }
    }

    public static float getDefaultFxMix(int mode) {
        switch (mode) {
            case MODE_NORMAL: return 0.0f;
            case MODE_ROBOT: return 0.65f;
            case MODE_ALIEN: return 0.65f;
            case MODE_HOARSE: return 0.55f;
            case MODE_ECHO: return 0.50f;
            case MODE_CAVE: return 0.45f;
            case MODE_TELEPHONE: return 0.75f;
            case MODE_MONSTER: return 0.45f;
            default: return 0.55f;
        }
    }

    public static float getCustomPitch(Context context, int mode) {
        if (mode == MODE_NORMAL) return 0f;
        try {
            SharedPreferences prefs = getPrefs(context);
            if (prefs != null && prefs.contains(KEY_PITCH_PREFIX + mode)) {
                return clamp(prefs.getFloat(KEY_PITCH_PREFIX + mode, getDefaultPitch(mode)), -12f, 12f);
            }
        } catch (Throwable ignored) {}
        return getDefaultPitch(mode);
    }

    public static float getCustomSpeed(Context context, int mode) {
        if (mode == MODE_NORMAL) return 1f;
        try {
            SharedPreferences prefs = getPrefs(context);
            if (prefs != null && prefs.contains(KEY_SPEED_PREFIX + mode)) {
                return clamp(prefs.getFloat(KEY_SPEED_PREFIX + mode, getDefaultSpeed(mode)), 0.70f, 1.45f);
            }
        } catch (Throwable ignored) {}
        return getDefaultSpeed(mode);
    }

    public static float getCustomFxMix(Context context, int mode) {
        if (mode == MODE_NORMAL) return 0f;
        try {
            SharedPreferences prefs = getPrefs(context);
            if (prefs != null && prefs.contains(KEY_FX_PREFIX + mode)) {
                return clamp(prefs.getFloat(KEY_FX_PREFIX + mode, getDefaultFxMix(mode)), 0f, 1f);
            }
        } catch (Throwable ignored) {}
        return getDefaultFxMix(mode);
    }

    public static void saveCustomParams(Context context, int mode, float pitch, float speed, float fxMix) {
        if (mode == MODE_NORMAL) return;
        try {
            SharedPreferences prefs = getPrefs(context);
            if (prefs != null) {
                prefs.edit()
                        .putFloat(KEY_PITCH_PREFIX + mode, clamp(pitch, -12f, 12f))
                        .putFloat(KEY_SPEED_PREFIX + mode, clamp(speed, 0.70f, 1.45f))
                        .putFloat(KEY_FX_PREFIX + mode, clamp(fxMix, 0f, 1f))
                        .apply();
            }
        } catch (Throwable ignored) {}
    }

    public static void resetCustomParams(Context context, int mode) {
        try {
            SharedPreferences prefs = getPrefs(context);
            if (prefs != null) {
                prefs.edit()
                        .remove(KEY_PITCH_PREFIX + mode)
                        .remove(KEY_SPEED_PREFIX + mode)
                        .remove(KEY_FX_PREFIX + mode)
                        .apply();
            }
        } catch (Throwable ignored) {}
    }

    public static String getModeTitle(int mode) {
        switch (mode) {
            case MODE_MALE: return "Man Voice";
            case MODE_FEMALE: return "Woman Voice";
            case MODE_CHILD: return "Child Voice";
            case MODE_MOUSE: return "Mouse";
            case MODE_MONSTER: return "Monster";
            case MODE_CHIPMUNK: return "Chipmunk";
            case MODE_DEEP: return "Deep Bass";
            case MODE_ROBOT: return "Robot";
            case MODE_ALIEN: return "Alien";
            case MODE_HOARSE: return "Hoarseness";
            case MODE_HELIUM: return "Helium";
            case MODE_ECHO: return "Echo";
            case MODE_CAVE: return "Cave Reverb";
            case MODE_TELEPHONE: return "Telephone";
            case MODE_SPEED: return "Record Speed (Thick / Thin)";
            case MODE_MODULATION: return "Modulation (Custom Pitch)";
            default: return "Real Voice";
        }
    }

    public static String getModeSubtitle(int mode) {
        switch (mode) {
            case MODE_MALE: return "Rich natural man voice with warm chest tone";
            case MODE_FEMALE: return "Clear bright woman voice with smooth tone";
            case MODE_CHILD: return "Cute energetic kid voice with crisp clarity";
            case MODE_MOUSE: return "Tiny fast squeaky cartoon mouse voice";
            case MODE_MONSTER: return "Heavy deep cinematic monster growl";
            case MODE_CHIPMUNK: return "Playful crisp high-pitched animated voice";
            case MODE_DEEP: return "Studio bass narrator voice with clear diction";
            case MODE_ROBOT: return "Clean speech-gated cyber vocoder (zero noise buzz)";
            case MODE_ALIEN: return "Wobbly sci-fi extraterrestrial frequency warp";
            case MODE_HOARSE: return "Husky whisper-grit voice with warm midrange";
            case MODE_HELIUM: return "Bright airy helium balloon vocal lift";
            case MODE_ECHO: return "Warm studio repeat echo (customizable mix)";
            case MODE_CAVE: return "Spacious canyon hall echo with clean noise gate";
            case MODE_TELEPHONE: return "Classic crisp radio & landline vocal filter";
            case MODE_SPEED: return "Custom slow thick or fast thin recording speed";
            case MODE_MODULATION: return "Fine-tune exact pitch shift from -12 to +12 st";
            default: return "Original natural microphone (zero DSP processing)";
        }
    }

    private static int getModeStartColor(int mode) {
        switch (mode) {
            case MODE_MALE: return 0xFF2563EB;
            case MODE_FEMALE: return 0xFFEC4899;
            case MODE_CHILD: return 0xFFF59E0B;
            case MODE_MOUSE: return 0xFFFB7185;
            case MODE_MONSTER: return 0xFFDC2626;
            case MODE_CHIPMUNK: return 0xFFD946EF;
            case MODE_DEEP: return 0xFF4F46E5;
            case MODE_ROBOT: return 0xFF06B6D4;
            case MODE_ALIEN: return 0xFF22C55E;
            case MODE_HOARSE: return 0xFF78716C;
            case MODE_HELIUM: return 0xFF14B8A6;
            case MODE_ECHO: return 0xFF3B82F6;
            case MODE_CAVE: return 0xFF10B981;
            case MODE_TELEPHONE: return 0xFF6366F1;
            case MODE_SPEED: return 0xFF0EA5E9;
            case MODE_MODULATION: return 0xFF8B5CF6;
            default: return 0xFF0284C7;
        }
    }

    private static int getModeEndColor(int mode) {
        switch (mode) {
            case MODE_MALE: return 0xFF1D4ED8;
            case MODE_FEMALE: return 0xFFF43F5E;
            case MODE_CHILD: return 0xFFEA580C;
            case MODE_MOUSE: return 0xFFE11D48;
            case MODE_MONSTER: return 0xFF7F1D1D;
            case MODE_CHIPMUNK: return 0xFF9333EA;
            case MODE_DEEP: return 0xFF7C3AED;
            case MODE_ROBOT: return 0xFF0891B2;
            case MODE_ALIEN: return 0xFF15803D;
            case MODE_HOARSE: return 0xFF44403C;
            case MODE_HELIUM: return 0xFF0D9488;
            case MODE_ECHO: return 0xFF1D4ED8;
            case MODE_CAVE: return 0xFF047857;
            case MODE_TELEPHONE: return 0xFF4338CA;
            case MODE_SPEED: return 0xFF2563EB;
            case MODE_MODULATION: return 0xFF6D28D9;
            default: return 0xFF38BDF8;
        }
    }

    private static String getModeBadgeGlyph(int mode) {
        switch (mode) {
            case MODE_NORMAL: return "RV";
            case MODE_MALE: return "M";
            case MODE_FEMALE: return "W";
            case MODE_CHILD: return "KD";
            case MODE_MOUSE: return "MS";
            case MODE_MONSTER: return "MN";
            case MODE_CHIPMUNK: return "CM";
            case MODE_DEEP: return "DB";
            case MODE_ROBOT: return "RB";
            case MODE_ALIEN: return "AL";
            case MODE_HOARSE: return "HR";
            case MODE_HELIUM: return "He";
            case MODE_ECHO: return "EC";
            case MODE_CAVE: return "CV";
            case MODE_TELEPHONE: return "PH";
            case MODE_SPEED: return "SP";
            case MODE_MODULATION: return "±";
            default: return "RV";
        }
    }

    // -------------------------------------------------------------------------
    // Dialog Entry Points (Backwards compatible + ChatActivity-aware)
    // -------------------------------------------------------------------------

    public static void showDialog(Context context, Runnable onChanged) {
        showDialogInternal(context, null, onChanged);
    }

    public static void showDialog(ChatActivity chatActivity, Runnable onChanged) {
        Context ctx = chatActivity != null ? chatActivity.getParentActivity() : null;
        showDialogInternal(ctx, chatActivity, onChanged);
    }

    private static void showDialogInternal(
            @Nullable Context context,
            @Nullable ChatActivity chatActivity,
            @Nullable Runnable onChanged
    ) {
        if (context == null) return;
        if (context instanceof Activity) {
            Activity act = (Activity) context;
            if (act.isFinishing() || act.isDestroyed()) return;
        }

        final int initialMode = getSelectedMode(context);
        final int[] pendingMode = new int[]{initialMode};
        final boolean[] confirmedByButton = new boolean[]{false};
        final boolean[] autoDemo = new boolean[]{isAutoDemoEnabled(context)};
        final boolean[] noiseGate = new boolean[]{isNoiseGateEnabled(context)};
        final boolean[] customizeOpen = new boolean[]{false};
        final boolean isDark = Theme.isCurrentThemeDark();

        LinearLayout dialogContainer = new LinearLayout(context);
        dialogContainer.setOrientation(LinearLayout.VERTICAL);
        dialogContainer.setPadding(
                AndroidUtilities.dp(14),
                AndroidUtilities.dp(12),
                AndroidUtilities.dp(14),
                AndroidUtilities.dp(4)
        );

        // 1. Studio Header Card
        LinearLayout headerCard = new LinearLayout(context);
        headerCard.setOrientation(LinearLayout.VERTICAL);
        headerCard.setPadding(AndroidUtilities.dp(13), AndroidUtilities.dp(11), AndroidUtilities.dp(13), AndroidUtilities.dp(11));
        GradientDrawable headerBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                isDark ? new int[]{0xFF172554, 0xFF1E1B4B} : new int[]{0xFFEFF6FF, 0xFFF5F3FF}
        );
        headerBg.setCornerRadius(AndroidUtilities.dp(16));
        headerBg.setStroke(AndroidUtilities.dp(1f), isDark ? 0x5538BDF8 : 0xFFBFDBFE);
        headerCard.setBackground(headerBg);

        LinearLayout headerTop = new LinearLayout(context);
        headerTop.setOrientation(LinearLayout.HORIZONTAL);
        headerTop.setGravity(Gravity.CENTER_VERTICAL);

        StudioHeaderIconView headerIcon = new StudioHeaderIconView(context);
        LinearLayout.LayoutParams hIconLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(38), AndroidUtilities.dp(38));
        hIconLp.rightMargin = AndroidUtilities.dp(10);
        headerTop.addView(headerIcon, hIconLp);

        LinearLayout headerTextCol = new LinearLayout(context);
        headerTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView mainTitle = new TextView(context);
        mainTitle.setText("Voice Studio Pro");
        mainTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.5f);
        mainTitle.setTypeface(AndroidUtilities.bold());
        mainTitle.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        headerTextCol.addView(mainTitle);

        TextView subTitle = new TextView(context);
        subTitle.setText("Noise-gated clarity • Tap any voice for English demo");
        subTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
        subTitle.setTextColor(isDark ? 0xFF94A3B8 : 0xFF475569);
        headerTextCol.addView(subTitle);

        headerTop.addView(headerTextCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        headerCard.addView(headerTop);

        // Quick tools strip inside header: Auto Demo toggle + Text-to-Voice button
        LinearLayout toolsRow = new LinearLayout(context);
        toolsRow.setOrientation(LinearLayout.HORIZONTAL);
        toolsRow.setPadding(0, AndroidUtilities.dp(9), 0, 0);

        final TextView autoDemoBtn = makeChip(context, "", isDark);
        Runnable refreshAutoDemoChip = () -> {
            autoDemoBtn.setText(autoDemo[0] ? "🔊 Auto Demo: ON" : "🔇 Auto Demo: OFF");
            styleChip(autoDemoBtn, autoDemo[0], 0xFF0284C7, isDark);
        };
        refreshAutoDemoChip.run();
        autoDemoBtn.setOnClickListener(v -> {
            autoDemo[0] = !autoDemo[0];
            setAutoDemoEnabled(context, autoDemo[0]);
            if (!autoDemo[0]) NGVoiceDemoPlayer.stopPlayback();
            refreshAutoDemoChip.run();
        });
        toolsRow.addView(autoDemoBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t2vBtn = makeChip(context, "✍ Text to Voice", isDark);
        styleChip(t2vBtn, true, 0xFF7C3AED, isDark);
        LinearLayout.LayoutParams t2vLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        t2vLp.leftMargin = AndroidUtilities.dp(7);
        toolsRow.addView(t2vBtn, t2vLp);

        headerCard.addView(toolsRow);

        LinearLayout.LayoutParams headerCardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerCardLp.bottomMargin = AndroidUtilities.dp(9);
        dialogContainer.addView(headerCard, headerCardLp);

        // 2. Active Character Banner with Customize & Play Demo actions
        LinearLayout activeBanner = new LinearLayout(context);
        activeBanner.setOrientation(LinearLayout.HORIZONTAL);
        activeBanner.setGravity(Gravity.CENTER_VERTICAL);
        activeBanner.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));

        final VoiceIconView bannerIcon = new VoiceIconView(context, initialMode);
        LinearLayout.LayoutParams bannerIconLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(32), AndroidUtilities.dp(32));
        bannerIconLp.rightMargin = AndroidUtilities.dp(9);
        activeBanner.addView(bannerIcon, bannerIconLp);

        LinearLayout bannerTextCol = new LinearLayout(context);
        bannerTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView bannerKicker = new TextView(context);
        bannerKicker.setText("ACTIVE VOICE");
        bannerKicker.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9.5f);
        bannerKicker.setTypeface(AndroidUtilities.bold());
        bannerKicker.setLetterSpacing(0.08f);
        bannerTextCol.addView(bannerKicker);

        final TextView bannerTitle = new TextView(context);
        bannerTitle.setText(getModeTitle(initialMode));
        bannerTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.8f);
        bannerTitle.setTypeface(AndroidUtilities.bold());
        bannerTitle.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        bannerTitle.setSingleLine(true);
        bannerTitle.setEllipsize(TextUtils.TruncateAt.END);
        bannerTextCol.addView(bannerTitle);

        activeBanner.addView(bannerTextCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView customizeToggleBtn = makeChip(context, "⚙ Tune", isDark);
        customizeToggleBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
        activeBanner.addView(customizeToggleBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        updateBannerStyle(activeBanner, bannerKicker, initialMode, isDark);

        LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bannerLp.bottomMargin = AndroidUtilities.dp(8);
        dialogContainer.addView(activeBanner, bannerLp);

        // 3. Per-Voice Customizer Panel (collapsible)
        final LinearLayout customPanel = new LinearLayout(context);
        customPanel.setOrientation(LinearLayout.VERTICAL);
        customPanel.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        GradientDrawable cpBg = new GradientDrawable();
        cpBg.setCornerRadius(AndroidUtilities.dp(14));
        cpBg.setColor(isDark ? 0xFF1E293B : 0xFFF8FAFC);
        cpBg.setStroke(AndroidUtilities.dp(1f), isDark ? 0xFF334155 : 0xFFE2E8F0);
        customPanel.setBackground(cpBg);
        customPanel.setVisibility(View.GONE);

        final TextView pitchLabel = new TextView(context);
        pitchLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        pitchLabel.setTypeface(AndroidUtilities.bold());
        pitchLabel.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1E293B);
        customPanel.addView(pitchLabel);

        final SeekBar pitchSeek = new SeekBar(context);
        pitchSeek.setMax(240); // -12.0 to +12.0 in 0.1 steps
        customPanel.addView(pitchSeek);

        final TextView speedLabel = new TextView(context);
        speedLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        speedLabel.setTypeface(AndroidUtilities.bold());
        speedLabel.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1E293B);
        speedLabel.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        customPanel.addView(speedLabel);

        final SeekBar speedSeek = new SeekBar(context);
        speedSeek.setMax(75); // 0.70x to 1.45x
        customPanel.addView(speedSeek);

        final TextView fxLabel = new TextView(context);
        fxLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        fxLabel.setTypeface(AndroidUtilities.bold());
        fxLabel.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1E293B);
        fxLabel.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        customPanel.addView(fxLabel);

        final SeekBar fxSeek = new SeekBar(context);
        fxSeek.setMax(100); // 0% to 100%
        customPanel.addView(fxSeek);

        LinearLayout cpFooter = new LinearLayout(context);
        cpFooter.setOrientation(LinearLayout.HORIZONTAL);
        cpFooter.setPadding(0, AndroidUtilities.dp(8), 0, 0);

        final TextView gateBtn = makeChip(context, "", isDark);
        Runnable refreshGateChip = () -> {
            gateBtn.setText(noiseGate[0] ? "🛡 Noise Gate: ON" : "Noise Gate: OFF");
            styleChip(gateBtn, noiseGate[0], 0xFF10B981, isDark);
        };
        refreshGateChip.run();
        gateBtn.setOnClickListener(v -> {
            noiseGate[0] = !noiseGate[0];
            setNoiseGateEnabled(context, noiseGate[0]);
            resetSession();
            refreshGateChip.run();
        });
        cpFooter.addView(gateBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));

        final TextView testBtn = makeChip(context, "▶ Test", isDark);
        styleChip(testBtn, true, 0xFF0284C7, isDark);
        LinearLayout.LayoutParams testLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f);
        testLp.leftMargin = AndroidUtilities.dp(6);
        cpFooter.addView(testBtn, testLp);

        final TextView resetBtn = makeChip(context, "Reset", isDark);
        styleChip(resetBtn, false, 0xFF64748B, isDark);
        LinearLayout.LayoutParams rstLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.7f);
        rstLp.leftMargin = AndroidUtilities.dp(6);
        cpFooter.addView(resetBtn, rstLp);

        customPanel.addView(cpFooter);

        LinearLayout.LayoutParams cpLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cpLp.bottomMargin = AndroidUtilities.dp(8);
        dialogContainer.addView(customPanel, cpLp);

        final boolean[] syncingSliders = new boolean[]{false};

        final Runnable triggerDemoForCurrent = () -> {
            int m = pendingMode[0];
            NGVoiceDemoPlayer.playEnglishDemo(
                    context,
                    m,
                    getCustomPitch(context, m),
                    getCustomSpeed(context, m),
                    getCustomFxMix(context, m),
                    noiseGate[0],
                    (playing, error) -> {
                        if (error != null) {
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        };

        final Runnable syncCustomPanel = () -> {
            int m = pendingMode[0];
            boolean tunable = (m != MODE_NORMAL);
            customizeToggleBtn.setVisibility(tunable ? View.VISIBLE : View.GONE);
            if (!tunable) {
                customizeOpen[0] = false;
                customPanel.setVisibility(View.GONE);
                return;
            }
            styleChip(customizeToggleBtn, customizeOpen[0], getModeStartColor(m), isDark);
            customPanel.setVisibility(customizeOpen[0] ? View.VISIBLE : View.GONE);

            syncingSliders[0] = true;
            float p = getCustomPitch(context, m);
            float sp = getCustomSpeed(context, m);
            float fx = getCustomFxMix(context, m);
            pitchSeek.setProgress(Math.round((p + 12f) * 10f));
            speedSeek.setProgress(Math.round((sp - 0.70f) * 100f));
            fxSeek.setProgress(Math.round(fx * 100f));
            pitchLabel.setText(String.format(Locale.US, "Pitch: %+.1f st", p));
            speedLabel.setText(String.format(Locale.US, "Speed: %.2fx", sp));
            fxLabel.setText(String.format(Locale.US, "Effect Intensity: %d%%", Math.round(fx * 100f)));
            syncingSliders[0] = false;
        };

        SeekBar.OnSeekBarChangeListener sliderListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (syncingSliders[0]) return;
                int m = pendingMode[0];
                if (m == MODE_NORMAL) return;
                float p = (pitchSeek.getProgress() / 10f) - 12f;
                float sp = 0.70f + (speedSeek.getProgress() / 100f);
                float fx = fxSeek.getProgress() / 100f;
                pitchLabel.setText(String.format(Locale.US, "Pitch: %+.1f st", p));
                speedLabel.setText(String.format(Locale.US, "Speed: %.2fx", sp));
                fxLabel.setText(String.format(Locale.US, "Effect Intensity: %d%%", Math.round(fx * 100f)));
                saveCustomParams(context, m, p, sp, fx);
                resetSession();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (autoDemo[0]) {
                    triggerDemoForCurrent.run();
                }
            }
        };
        pitchSeek.setOnSeekBarChangeListener(sliderListener);
        speedSeek.setOnSeekBarChangeListener(sliderListener);
        fxSeek.setOnSeekBarChangeListener(sliderListener);

        customizeToggleBtn.setOnClickListener(v -> {
            customizeOpen[0] = !customizeOpen[0];
            syncCustomPanel.run();
        });
        testBtn.setOnClickListener(v -> triggerDemoForCurrent.run());
        resetBtn.setOnClickListener(v -> {
            int m = pendingMode[0];
            resetCustomParams(context, m);
            resetSession();
            syncCustomPanel.run();
            if (autoDemo[0]) triggerDemoForCurrent.run();
            Toast.makeText(context, "Reset to studio default", Toast.LENGTH_SHORT).show();
        });
        syncCustomPanel.run();

        // 4. Scrollable Voice Character Cards
        final int maxScrollHeight = Math.min(
                AndroidUtilities.dp(340),
                Math.max(AndroidUtilities.dp(220), (int) (AndroidUtilities.displaySize.y * 0.42f))
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
        listRoot.setPadding(AndroidUtilities.dp(2), AndroidUtilities.dp(2), AndroidUtilities.dp(4), AndroidUtilities.dp(8));
        scrollView.addView(listRoot, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialogContainer.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final LinearLayout[] rowViews = new LinearLayout[DISPLAY_MODES.length];
        final TextView[] titleViews = new TextView[DISPLAY_MODES.length];
        final SelectionTickView[] tickViews = new SelectionTickView[DISPLAY_MODES.length];

        for (int i = 0; i < DISPLAY_MODES.length; i++) {
            final int index = i;
            final int mode = DISPLAY_MODES[i];

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(9), AndroidUtilities.dp(10), AndroidUtilities.dp(9));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = AndroidUtilities.dp(7);
            row.setLayoutParams(rowLp);

            VoiceIconView iconView = new VoiceIconView(context, mode);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(38), AndroidUtilities.dp(38));
            iconLp.rightMargin = AndroidUtilities.dp(10);
            row.addView(iconView, iconLp);

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);

            TextView titleTv = new TextView(context);
            titleTv.setText(getModeTitle(mode));
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
            titleTv.setTypeface(AndroidUtilities.bold());
            titleTv.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
            titleTv.setSingleLine(true);
            titleTv.setEllipsize(TextUtils.TruncateAt.END);
            textCol.addView(titleTv);

            TextView descTv = new TextView(context);
            descTv.setText(getModeSubtitle(mode));
            descTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.4f);
            descTv.setTextColor(isDark ? 0xFF94A3B8 : 0xFF475569);
            descTv.setMaxLines(2);
            descTv.setPadding(0, AndroidUtilities.dp(1f), 0, 0);
            textCol.addView(descTv);

            row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            // Compact per-row English Demo play button
            TextView playDemoBtn = new TextView(context);
            playDemoBtn.setText("▶");
            playDemoBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
            playDemoBtn.setGravity(Gravity.CENTER);
            playDemoBtn.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
            GradientDrawable playBg = new GradientDrawable();
            playBg.setShape(GradientDrawable.OVAL);
            playBg.setColor(isDark ? 0x2638BDF8 : 0x1A0284C7);
            playDemoBtn.setBackground(playBg);
            LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(30), AndroidUtilities.dp(30));
            playLp.leftMargin = AndroidUtilities.dp(6);
            row.addView(playDemoBtn, playLp);

            SelectionTickView tickView = new SelectionTickView(context);
            LinearLayout.LayoutParams tickLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(22), AndroidUtilities.dp(22));
            tickLp.leftMargin = AndroidUtilities.dp(8);
            row.addView(tickView, tickLp);

            rowViews[i] = row;
            titleViews[i] = titleTv;
            tickViews[i] = tickView;

            Runnable selectMode = () -> {
                pendingMode[0] = mode;
                setSelectedMode(context, mode);
                resetSession();
                updateRowStates(rowViews, titleViews, tickViews, mode, isDark, index);
                bannerIcon.setMode(mode);
                bannerTitle.setText(getModeTitle(mode));
                updateBannerStyle(activeBanner, bannerKicker, mode, isDark);
                if (mode == MODE_SPEED || mode == MODE_MODULATION || mode == MODE_ECHO) {
                    customizeOpen[0] = true;
                }
                syncCustomPanel.run();
                if (onChanged != null) {
                    onChanged.run();
                }
            };

            row.setOnClickListener(v -> {
                selectMode.run();
                if (autoDemo[0]) {
                    triggerDemoForCurrent.run();
                }
            });

            playDemoBtn.setOnClickListener(v -> {
                selectMode.run();
                triggerDemoForCurrent.run();
            });

            listRoot.addView(row);
        }

        updateRowStates(rowViews, titleViews, tickViews, pendingMode[0], isDark, -1);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogContainer);
        builder.setPositiveButton("Select", (dialog, which) -> {
            confirmedByButton[0] = true;
            NGVoiceDemoPlayer.stopPlayback();
            int selected = pendingMode[0];
            setSelectedMode(context, selected);
            resetSession();
            if (onChanged != null) {
                onChanged.run();
            }
            dialog.dismiss();
            showVoiceSelectedPopup(context, selected);
        });
        builder.setNegativeButton("Close", (dialog, which) -> {
            NGVoiceDemoPlayer.stopPlayback();
            dialog.dismiss();
        });

        final AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> {
            NGVoiceDemoPlayer.stopPlayback();
            if (!confirmedByButton[0] && pendingMode[0] != initialMode) {
                showVoiceSelectedPopup(context, pendingMode[0]);
            }
        });

        t2vBtn.setOnClickListener(v -> {
            NGVoiceDemoPlayer.stopPlayback();
            dialog.dismiss();
            NGTextToVoiceHelper.showDialog(context, chatActivity);
        });

        dialog.show();
    }

    private static TextView makeChip(Context ctx, String text, boolean isDark) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(7), AndroidUtilities.dp(8), AndroidUtilities.dp(7));
        styleChip(tv, false, 0xFF0284C7, isDark);
        return tv;
    }

    private static void styleChip(TextView tv, boolean active, int accent, boolean isDark) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(10));
        if (active) {
            bg.setColor(accent);
            tv.setTextColor(Color.WHITE);
        } else {
            bg.setColor(isDark ? 0xFF0F172A : 0xFFE2E8F0);
            bg.setStroke(AndroidUtilities.dp(1f), isDark ? 0xFF334155 : 0xFFCBD5E1);
            tv.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF334155);
        }
        tv.setBackground(bg);
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
                    isDark ? new int[]{0xFF0F172A, 0xFF1E293B} : new int[]{0xFFFFFFFF, 0xFFF8FAFC}
            );
            cardBg.setCornerRadius(AndroidUtilities.dp(22));
            cardBg.setStroke(AndroidUtilities.dp(1.6f), (accentStart & 0x00FFFFFF) | (isDark ? 0x99000000 : 0x55000000));
            card.setBackground(cardBg);

            SelectionTickView checkBadge = new SelectionTickView(context);
            checkBadge.setBadgeColors(0xFF10B981, 0xFF059669);
            checkBadge.setChecked(true, false);
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(46), AndroidUtilities.dp(46));
            badgeLp.bottomMargin = AndroidUtilities.dp(10);
            card.addView(checkBadge, badgeLp);

            TextView headingTv = new TextView(context);
            headingTv.setText(mode == MODE_NORMAL ? "Real Voice Restored" : "Voice Character Active");
            headingTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f);
            headingTv.setTypeface(AndroidUtilities.bold());
            headingTv.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
            headingTv.setGravity(Gravity.CENTER);
            card.addView(headingTv);

            LinearLayout pill = new LinearLayout(context);
            pill.setOrientation(LinearLayout.HORIZONTAL);
            pill.setGravity(Gravity.CENTER_VERTICAL);
            pill.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setCornerRadius(AndroidUtilities.dp(12));
            pillBg.setColor(isDark ? blendAlpha(accentStart, 0xFF0F172A, 0.25f) : blendAlpha(accentStart, 0xFFFFFFFF, 0.14f));
            pill.setBackground(pillBg);

            VoiceIconView icon = new VoiceIconView(context, mode);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(24), AndroidUtilities.dp(24));
            iconLp.rightMargin = AndroidUtilities.dp(8);
            pill.addView(icon, iconLp);

            TextView nameTv = new TextView(context);
            nameTv.setText(title);
            nameTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
            nameTv.setTypeface(AndroidUtilities.bold());
            nameTv.setTextColor(isDark ? 0xFFFFFFFF : accentStart);
            pill.addView(nameTv);

            LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pillLp.topMargin = AndroidUtilities.dp(10);
            card.addView(pill, pillLp);

            popup.setContentView(card);
            popup.setCancelable(true);
            popup.setCanceledOnTouchOutside(true);

            card.setScaleX(0.86f);
            card.setScaleY(0.86f);
            card.setAlpha(0f);
            popup.show();

            card.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(220)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();

            AndroidUtilities.runOnUIThread(() -> {
                try {
                    if (popup.isShowing()) {
                        popup.dismiss();
                    }
                } catch (Throwable ignored) {}
            }, 1350);
        } catch (Throwable ignored) {}
    }

    // -------------------------------------------------------------------------
    // Custom Vector Icon & Selection Tick Views
    // -------------------------------------------------------------------------

    private static final class StudioHeaderIconView extends View {
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        StudioHeaderIconView(Context context) {
            super(context);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeCap(Paint.Cap.ROUND);
            strokePaint.setColor(Color.WHITE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            rect.set(0, 0, w, h);
            bgPaint.setShader(new LinearGradient(0, 0, w, h, 0xFF0EA5E9, 0xFF7C3AED, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, w * 0.28f, h * 0.28f, bgPaint);

            strokePaint.setStrokeWidth(w * 0.08f);
            float cy = h * 0.5f;
            float[] xs = new float[]{0.26f, 0.38f, 0.50f, 0.62f, 0.74f};
            float[] hs = new float[]{0.14f, 0.26f, 0.34f, 0.24f, 0.14f};
            for (int i = 0; i < xs.length; i++) {
                float x = w * xs[i];
                float half = h * hs[i];
                canvas.drawLine(x, cy - half, x, cy + half, strokePaint);
            }
        }
    }

    private static final class VoiceIconView extends View {
        private int mode;
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        VoiceIconView(Context context, int mode) {
            super(context);
            this.mode = mode;
            textPaint.setColor(Color.WHITE);
            textPaint.setTypeface(AndroidUtilities.bold());
            textPaint.setTextAlign(Paint.Align.CENTER);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setColor(0x33FFFFFF);
        }

        void setMode(int mode) {
            this.mode = mode;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;
            rect.set(0, 0, w, h);
            bgPaint.setShader(new LinearGradient(
                    0, 0, w, h, getModeStartColor(mode), getModeEndColor(mode), Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, w * 0.30f, h * 0.30f, bgPaint);

            ringPaint.setStrokeWidth(Math.max(1.5f, w * 0.04f));
            canvas.drawRoundRect(
                    w * 0.06f, h * 0.06f, w * 0.94f, h * 0.94f,
                    w * 0.26f, h * 0.26f, ringPaint
            );

            String glyph = getModeBadgeGlyph(mode);
            textPaint.setTextSize(w * (glyph.length() > 1 ? 0.36f : 0.44f));
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float ty = h * 0.5f - (fm.ascent + fm.descent) * 0.5f;
            canvas.drawText(glyph, w * 0.5f, ty, textPaint);
        }
    }

    private static final class SelectionTickView extends View {
        private boolean checked = false;
        private float progress = 0f;
        private int startColor = 0xFF0EA5E9;
        private int endColor = 0xFF2563EB;
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path tickPath = new Path();

        SelectionTickView(Context context) {
            super(context);
            ringPaint.setStyle(Paint.Style.STROKE);
            tickPaint.setStyle(Paint.Style.STROKE);
            tickPaint.setStrokeCap(Paint.Cap.ROUND);
            tickPaint.setStrokeJoin(Paint.Join.ROUND);
            tickPaint.setColor(Color.WHITE);
        }

        void setBadgeColors(int start, int end) {
            this.startColor = start;
            this.endColor = end;
            invalidate();
        }

        void setChecked(boolean checked, boolean animate) {
            this.checked = checked;
            if (!animate) {
                progress = checked ? 1f : 0f;
                invalidate();
                return;
            }
            ValueAnimator anim = ValueAnimator.ofFloat(progress, checked ? 1f : 0f);
            anim.setDuration(190);
            anim.addUpdateListener(a -> {
                progress = (float) a.getAnimatedValue();
                invalidate();
            });
            anim.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            float cx = w * 0.5f;
            float cy = h * 0.5f;
            float r = Math.min(w, h) * 0.45f;

            if (progress <= 0.01f) {
                ringPaint.setStrokeWidth(Math.max(2f, w * 0.08f));
                ringPaint.setColor(Theme.isCurrentThemeDark() ? 0xFF475569 : 0xFFCBD5E1);
                canvas.drawCircle(cx, cy, r, ringPaint);
                return;
            }

            fillPaint.setShader(new LinearGradient(0, 0, w, h, startColor, endColor, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, r * (0.75f + 0.25f * progress), fillPaint);

            tickPaint.setStrokeWidth(Math.max(2.2f, w * 0.11f));
            tickPath.reset();
            tickPath.moveTo(w * 0.29f, h * 0.52f);
            tickPath.lineTo(w * 0.44f, h * 0.67f);
            tickPath.lineTo(w * 0.72f, h * 0.36f);
            canvas.drawPath(tickPath, tickPaint);
        }
    }

    private static int blendAlpha(int fg, int bg, float alpha) {
        float inv = 1f - alpha;
        int r = Math.round(((fg >> 16) & 0xFF) * alpha + ((bg >> 16) & 0xFF) * inv);
        int g = Math.round(((fg >> 8) & 0xFF) * alpha + ((bg >> 8) & 0xFF) * inv);
        int b = Math.round((fg & 0xFF) * alpha + (bg & 0xFF) * inv);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int lightenColor(int color, float amount) {
        return blendAlpha(0xFFFFFFFF, color, amount);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    // -------------------------------------------------------------------------
    // Public Streaming & Offline PCM Audio APIs (100% Backwards-Compatible)
    // -------------------------------------------------------------------------

    public interface FrameSink {
        void write(ByteBuffer frame, int byteCount);
    }

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

    public static DirectEffect createDirectEffect(int sampleRate) {
        Context ctx = ApplicationLoader.applicationContext;
        int mode = getSelectedMode(ctx);
        if (mode == MODE_NORMAL) {
            return null;
        }
        int sr = sampleRate > 0 ? sampleRate : DEFAULT_SAMPLE_RATE;
        return new DirectEffect(new StudioDsp(
                mode,
                sr,
                getCustomPitch(ctx, mode),
                getCustomSpeed(ctx, mode),
                getCustomFxMix(ctx, mode),
                isNoiseGateEnabled(ctx)
        ));
    }

    public static Session createSession(int sampleRate, int frameSamples) {
        Context ctx = ApplicationLoader.applicationContext;
        int mode = getSelectedMode(ctx);
        if (mode == MODE_NORMAL) {
            return null;
        }
        int sr = sampleRate > 0 ? sampleRate : DEFAULT_SAMPLE_RATE;
        int fs = frameSamples > 0 ? frameSamples : 960;
        return new Session(new StudioDsp(
                mode,
                sr,
                getCustomPitch(ctx, mode),
                getCustomSpeed(ctx, mode),
                getCustomFxMix(ctx, mode),
                isNoiseGateEnabled(ctx)
        ), fs);
    }

    public static void resetSession() {
        synchronized (DSP_LOCK) {
            legacyDsp = null;
            legacyDspMode = MODE_NORMAL;
        }
    }

    /**
     * Offline PCM processor used by English Demo Player & Text-to-Voice Studio so previews and
     * sent Text-to-Voice notes sound 100% identical to live Voice Changer recordings.
     */
    @NonNull
    public static short[] processOfflinePcm(
            int mode,
            float pitchSemitones,
            float speed,
            float fxMix,
            boolean noiseGate,
            @NonNull short[] input,
            int sampleRate
    ) {
        if (input.length == 0 || mode == MODE_NORMAL) {
            return input;
        }
        int sr = sampleRate > 0 ? sampleRate : DEFAULT_SAMPLE_RATE;
        StudioDsp dsp = new StudioDsp(mode, sr, pitchSemitones, speed, fxMix, noiseGate);
        short[] body = dsp.process(input, input.length);
        short[] tail = dsp.drain();
        short[] out = new short[body.length + tail.length];
        System.arraycopy(body, 0, out, 0, body.length);
        System.arraycopy(tail, 0, out, body.length, tail.length);
        return out;
    }

    public static void processPcmBuffer(ByteBuffer buffer, int byteCount) {
        processPcmBuffer(buffer, byteCount, DEFAULT_SAMPLE_RATE);
    }

    public static void processPcmBuffer(ByteBuffer buffer, int byteCount, int sampleRate) {
        if (buffer == null || byteCount < 2) return;
        Context ctx = ApplicationLoader.applicationContext;
        int mode = getSelectedMode(ctx);
        if (mode == MODE_NORMAL) return;

        final int samples = byteCount >> 1;
        final int sr = sampleRate > 0 ? sampleRate : DEFAULT_SAMPLE_RATE;
        synchronized (DSP_LOCK) {
            if (legacyDsp == null || legacyDspMode != mode || legacyDspSampleRate != sr) {
                legacyDsp = new StudioDsp(
                        mode,
                        sr,
                        getCustomPitch(ctx, mode),
                        getCustomSpeed(ctx, mode),
                        getCustomFxMix(ctx, mode),
                        isNoiseGateEnabled(ctx)
                );
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

    // -------------------------------------------------------------------------
    // Studio-Grade Clean Vocal DSP Engine (Zero Noise Boost, Zero Iron Clang)
    // -------------------------------------------------------------------------

    private static final class StudioDsp {
        final int mode;
        final int sampleRate;
        final float speed;
        final float fxMix;
        final boolean noiseGateEnabled;

        // Stage 1: Clean speech frontend (BEFORE NGSonic!)
        private final Biquad preHpf;        // 95 Hz 2nd-order Butterworth rumble/hum cut
        private final VocalNoiseGate gate;  // Adaptive soft-knee noise gate (-20 dB floor)

        // Stage 2: Clean PICOLA pitch/speed shifter (phase-intact, zero metallic ringing)
        private final NGSonic sonic;

        // Stage 3: Zero-latency minimum-phase vocal-tract formant EQ & speech-gated character FX
        private final Biquad eqLow;         // Chest / sub-bass shaping
        private final Biquad eqMid;         // Formant / presence shaping
        private final Biquad eqAirLpf;      // Gentle 9.5 kHz anti-sibilance polish
        private final Biquad phoneBandpass; // Telephone 340-3200 Hz RBJ bandpass

        private final StudioEcho echo;
        private final Freeverb caveReverb;
        private final int tailSamples;

        // Clean speech-gated character oscillators (Robot / Alien / Hoarse)
        private double modPhase1 = 0.0;
        private double modPhase2 = 0.0;
        private double lfoPhase = 0.0;
        private final float[] robotComb;
        private int robotCombPos = 0;
        private float robotCombLp = 0f;
        private float hoarseLp = 0f;
        private int rngState = 0x13579BDF;

        // Post-stage speech activity tracker (keeps FX strictly silent during pauses)
        private float postEnv = 0f;
        private final float postAttack;
        private final float postRelease;

        // Gentle broadcast vocal auto-leveler (only boosts active speech, never silence)
        private float agcRms = 0.14f;
        private final float agcAlpha;
        private final float outputGain;

        // Jitter FIFO for fixed-length fallback callers
        private short[] legacyQueue = new short[32768];
        private int legacyQueued = 0;
        private boolean legacyStarted = false;

        StudioDsp(int mode, int sampleRate, float pitchSemitones, float speedParam, float fxMixParam, boolean noiseGateEnabled) {
            this.mode = mode;
            this.sampleRate = sampleRate;
            this.speed = clamp(speedParam, 0.70f, 1.45f);
            this.fxMix = clamp(fxMixParam, 0f, 1f);
            this.noiseGateEnabled = noiseGateEnabled;

            // Stage 1: Clean 95 Hz Butterworth HPF + adaptive vocal noise gate (zero stacked EQ)
            this.preHpf = Biquad.highPass(sampleRate, 95.0, 0.707);
            this.gate = new VocalNoiseGate(sampleRate);

            // Stage 2: NGSonic runs FIRST on clean, un-smeared speech!
            float clampedSt = clamp(pitchSemitones, -12.0f, 12.0f);
            if (Math.abs(clampedSt) > 0.05f || Math.abs(this.speed - 1.0f) > 0.01f) {
                float pitchFactor = (float) Math.pow(2.0, clampedSt / 12.0);
                this.sonic = new NGSonic(sampleRate, 1, this.speed, pitchFactor, sampleRate);
            } else {
                this.sonic = null;
            }

            // Stage 3: Smooth minimum-phase vocal-tract formant EQ per voice character
            float eqScale = 0.55f + 0.70f * this.fxMix; // lets user scale character tone smoothly
            Biquad low = null;
            Biquad mid = null;
            Biquad phone = null;
            StudioEcho ec = null;
            Freeverb rev = null;
            float[] rComb = null;
            int tail = 0;
            float outGain = 1.06f;

            switch (mode) {
                case MODE_MALE:
                    low = Biquad.peaking(sampleRate, 165.0, 0.9, 3.5 * eqScale);
                    mid = Biquad.peaking(sampleRate, 2400.0, 1.1, 2.0 * eqScale);
                    outGain = 1.08f;
                    break;
                case MODE_FEMALE:
                    low = Biquad.lowShelf(sampleRate, 200.0, -2.8 * eqScale);
                    mid = Biquad.peaking(sampleRate, 2800.0, 0.9, 2.2 * eqScale);
                    outGain = 1.12f;
                    break;
                case MODE_CHILD:
                    low = Biquad.lowShelf(sampleRate, 230.0, -3.2 * eqScale);
                    mid = Biquad.peaking(sampleRate, 3000.0, 0.9, 2.4 * eqScale);
                    outGain = 1.12f;
                    break;
                case MODE_MOUSE:
                    low = Biquad.lowShelf(sampleRate, 300.0, -3.8 * eqScale);
                    mid = Biquad.peaking(sampleRate, 3400.0, 0.95, 2.2 * eqScale);
                    outGain = 1.14f;
                    break;
                case MODE_MONSTER:
                    low = Biquad.peaking(sampleRate, 125.0, 0.85, 4.8 * eqScale);
                    mid = Biquad.peaking(sampleRate, 950.0, 1.2, 2.2 * eqScale);
                    rev = new Freeverb(sampleRate, 0.55f, 0.55f, 18.0f);
                    tail = (int) (sampleRate * 0.45f);
                    outGain = 1.10f;
                    break;
                case MODE_DEEP:
                    low = Biquad.peaking(sampleRate, 140.0, 0.85, 4.2 * eqScale);
                    mid = Biquad.peaking(sampleRate, 2500.0, 1.0, 2.2 * eqScale);
                    outGain = 1.08f;
                    break;
                case MODE_HELIUM:
                    low = Biquad.lowShelf(sampleRate, 260.0, -3.4 * eqScale);
                    mid = Biquad.peaking(sampleRate, 3200.0, 0.95, 2.3 * eqScale);
                    outGain = 1.10f;
                    break;
                case MODE_CHIPMUNK:
                    low = Biquad.lowShelf(sampleRate, 260.0, -4.0 * eqScale);
                    mid = Biquad.peaking(sampleRate, 3400.0, 1.0, 3.0 * eqScale);
                    outGain = 1.12f;
                    break;
                case MODE_ROBOT:
                    // Clean short 5.5 ms comb resonance + dual-harmonic ring-mod (100% speech-gated)
                    rComb = new float[Math.max(32, Math.round(sampleRate * 0.0055f))];
                    mid = Biquad.peaking(sampleRate, 1800.0, 1.2, 2.8);
                    outGain = 1.12f;
                    break;
                case MODE_ALIEN:
                    rComb = new float[Math.max(32, Math.round(sampleRate * 0.0080f))];
                    mid = Biquad.peaking(sampleRate, 2200.0, 1.1, 2.5);
                    outGain = 1.10f;
                    break;
                case MODE_HOARSE:
                    low = Biquad.peaking(sampleRate, 190.0, 1.0, 2.5 * eqScale);
                    mid = Biquad.peaking(sampleRate, 1450.0, 1.1, 2.8 * eqScale);
                    outGain = 1.12f;
                    break;
                case MODE_ECHO:
                    int delayMs = Math.round(240 + 260 * this.fxMix); // 240ms..500ms warm studio echo
                    float fb = 0.25f + 0.35f * this.fxMix;
                    ec = new StudioEcho(sampleRate, delayMs, fb);
                    tail = (int) (sampleRate * 0.85f);
                    outGain = 1.05f;
                    break;
                case MODE_CAVE:
                    rev = new Freeverb(sampleRate, 0.72f, 0.40f, 32.0f);
                    tail = (int) (sampleRate * 0.85f);
                    outGain = 1.05f;
                    break;
                case MODE_TELEPHONE:
                    phone = Biquad.bandPass(sampleRate, 1250.0, 0.85);
                    outGain = 1.18f;
                    break;
                default:
                    mid = Biquad.peaking(sampleRate, 2600.0, 1.0, 1.8);
                    outGain = 1.08f;
                    break;
            }

            this.eqLow = low;
            this.eqMid = mid;
            this.eqAirLpf = Biquad.lowPass(sampleRate, Math.min(7600.0, sampleRate * 0.42), 0.707);
            this.phoneBandpass = phone;
            this.echo = ec;
            this.caveReverb = rev;
            this.robotComb = rComb;
            this.tailSamples = tail;
            this.outputGain = outGain;

            this.postAttack = (float) Math.exp(-1.0 / (sampleRate * 0.006));
            this.postRelease = (float) Math.exp(-1.0 / (sampleRate * 0.140));
            this.agcAlpha = (float) Math.exp(-1.0 / (sampleRate * 0.250));
        }

        short[] process(short[] input, int count) {
            if (input == null || count <= 0) {
                return new short[0];
            }

            // 1. Clean 95 Hz rumble HPF + Adaptive Soft-Knee Vocal Noise Gate + Presence clarity
            short[] clean = new short[count];
            for (int i = 0; i < count; i++) {
                float x = input[i] / 32768f;
                x = preHpf.process(x);
                if (noiseGateEnabled) {
                    x = gate.process(x);
                }
                clean[i] = floatToShort(softLimit(x));
            }

            // 2. Pitch & speed shifting via NGSonic FIRST on clean phase-intact vocal waveform
            short[] pitched;
            if (sonic != null) {
                sonic.queueInput(ShortBuffer.wrap(clean, 0, count));
                pitched = readSonicOutput();
            } else {
                pitched = clean;
            }

            // 3. Zero-latency vocal-tract EQ, speech-gated FX, vocal leveler & soft limiter
            applyPostStages(pitched);
            return pitched;
        }

        short[] drain() {
            short[] rest;
            if (sonic != null) {
                sonic.queueEndOfStream();
                rest = readSonicOutput();
            } else {
                rest = new short[0];
            }
            short[] out = new short[rest.length + tailSamples];
            System.arraycopy(rest, 0, out, 0, rest.length);
            applyPostStages(out);
            if (tailSamples > 0) {
                int fadeStart = rest.length;
                for (int i = 0; i < tailSamples; i++) {
                    float fade = 1f - ((float) i / (float) tailSamples);
                    out[fadeStart + i] = (short) (out[fadeStart + i] * fade * fade);
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

        private void applyPostStages(short[] samples) {
            if (samples == null || samples.length == 0) return;
            final double twoPi = 2.0 * Math.PI;
            final double invSr = 1.0 / sampleRate;

            for (int i = 0; i < samples.length; i++) {
                float x = samples[i] / 32768f;

                // Track post-Sonic speech envelope so modulation/reverb never excite quiet noise
                float absX = Math.abs(x);
                if (absX > postEnv) {
                    postEnv = postAttack * postEnv + (1f - postAttack) * absX;
                } else {
                    postEnv = postRelease * postEnv + (1f - postRelease) * absX;
                }
                // Smooth 0..1 speech activity weight (strictly 0 below -44 dBFS)
                float speechWeight = clamp((postEnv - 0.006f) / 0.025f, 0f, 1f);

                // Vocal-tract minimum-phase EQ
                if (eqLow != null) x = eqLow.process(x);
                if (eqMid != null) x = eqMid.process(x);

                // Clean character FX (strictly gated by speechWeight!)
                if (mode == MODE_ROBOT && robotComb != null) {
                    float delayedRaw = robotComb[robotCombPos];
                    robotCombLp = robotCombLp * 0.65f + delayedRaw * 0.35f;
                    float delayed = robotCombLp;
                    robotComb[robotCombPos] = x + delayed * 0.24f * speechWeight;
                    robotCombPos = (robotCombPos + 1) % robotComb.length;

                    modPhase1 += twoPi * 72.0 * invSr;
                    modPhase2 += twoPi * 144.0 * invSr;
                    if (modPhase1 > twoPi) modPhase1 -= twoPi;
                    if (modPhase2 > twoPi) modPhase2 -= twoPi;
                    float carrier = (float) (0.72 * Math.sin(modPhase1) + 0.28 * Math.sin(modPhase2));
                    float robotic = (x * 0.55f + delayed * 0.45f) * (0.45f + 0.55f * carrier);
                    float wet = (0.35f + 0.45f * fxMix) * speechWeight;
                    x = x * (1f - wet * 0.65f) + robotic * wet * 1.25f;
                } else if (mode == MODE_ALIEN && robotComb != null) {
                    lfoPhase += twoPi * 4.5 * invSr;
                    if (lfoPhase > twoPi) lfoPhase -= twoPi;
                    double instFreq = 58.0 + 36.0 * Math.sin(lfoPhase);
                    modPhase1 += twoPi * instFreq * invSr;
                    if (modPhase1 > twoPi) modPhase1 -= twoPi;

                    float delayedRaw = robotComb[robotCombPos];
                    robotCombLp = robotCombLp * 0.65f + delayedRaw * 0.35f;
                    float delayed = robotCombLp;
                    robotComb[robotCombPos] = x + delayed * 0.20f * speechWeight;
                    robotCombPos = (robotCombPos + 1) % robotComb.length;

                    float alien = (x * 0.65f + delayed * 0.35f) * (float) (0.50 + 0.50 * Math.sin(modPhase1));
                    float wet = (0.35f + 0.50f * fxMix) * speechWeight;
                    x = x * (1f - wet * 0.55f) + alien * wet * 1.25f;
                } else if (mode == MODE_HOARSE) {
                    // Authentic husky/raspy vocal fry modulation (strictly tied to active speech!)
                    rngState = rngState * 1664525 + 1013904223;
                    float white = ((rngState >> 9) & 0xFFFF) / 32768f - 1f;
                    hoarseLp = hoarseLp * 0.78f + white * 0.22f; // warm band-limited breath texture
                    float raspDepth = (0.22f + 0.35f * fxMix) * speechWeight;
                    float modulated = x * (1f - raspDepth * 0.45f + raspDepth * hoarseLp);
                    x = (float) Math.tanh(modulated * (1.15f + 0.35f * fxMix));
                } else if (mode == MODE_MONSTER) {
                    // Subtle warm sub-saturation + gated dark room resonance
                    float drive = 1.15f + 0.55f * fxMix;
                    float shaped = (float) Math.tanh(x * drive) / (float) Math.tanh(drive);
                    if (caveReverb != null) {
                        float wetRev = caveReverb.process(shaped * speechWeight);
                        x = shaped * 0.88f + wetRev * (0.14f + 0.16f * fxMix);
                    } else {
                        x = shaped;
                    }
                } else if (mode == MODE_ECHO && echo != null) {
                    float wetEcho = echo.process(x * speechWeight);
                    float wetAmt = 0.22f + 0.48f * fxMix;
                    x = x * 0.90f + wetEcho * wetAmt;
                } else if (mode == MODE_CAVE && caveReverb != null) {
                    float wetCave = caveReverb.process(x * speechWeight);
                    float wetAmt = 0.18f + 0.32f * fxMix;
                    x = x * 0.86f + wetCave * wetAmt;
                } else if (mode == MODE_TELEPHONE && phoneBandpass != null) {
                    float bp = phoneBandpass.process(x);
                    float drive = 1.20f + 0.50f * fxMix;
                    x = (float) Math.tanh(bp * drive) * 0.94f;
                }

                // Gentle 9.5 kHz anti-sibilance polish (keeps voice silky, zero harsh hiss)
                x = eqAirLpf.process(x);

                // Speech-only soft auto-leveler (brings soft voice forward WITHOUT boosting silence!)
                if (speechWeight > 0.35f) {
                    agcRms = agcAlpha * agcRms + (1f - agcAlpha) * Math.max(absX, 0.04f);
                }
                float targetGain = clamp(0.20f / Math.max(agcRms, 0.06f), 0.90f, 1.85f);
                float appliedGain = 1f + (targetGain - 1f) * speechWeight;

                x *= outputGain * appliedGain;
                samples[i] = floatToShort(softLimit(x));
            }
        }

        private static float softLimit(float x) {
            float a = Math.abs(x);
            if (a <= 0.88f) return x;
            float bent = 0.88f + 0.12f * (float) Math.tanh((a - 0.88f) / 0.12f);
            return x < 0f ? -bent : bent;
        }

        private static short floatToShort(float x) {
            if (x >= 1.0f) return 32767;
            if (x <= -1.0f) return -32768;
            return (short) (x * 32767f);
        }
    }

    /**
     * Adaptive Soft-Knee Vocal Noise Gate / Expander.
     * Tracks ambient background floor slowly while opening instantly (~5 ms) on human speech,
     * attenuating distant fan/room/traffic noise by ~20 dB without chopping soft consonants.
     */
    private static final class VocalNoiseGate {
        private final float fastAttack;
        private final float fastRelease;
        private final float floorRise;
        private final float floorFall;
        private final float gainAttack;
        private final float gainRelease;

        private float fastEnv = 0f;
        private float noiseFloor = 0.008f; // starts near -42 dBFS
        private float smoothGain = 0.12f;

        VocalNoiseGate(int sampleRate) {
            this.fastAttack = (float) Math.exp(-1.0 / (sampleRate * 0.004));
            this.fastRelease = (float) Math.exp(-1.0 / (sampleRate * 0.080));
            this.floorRise = (float) Math.exp(-1.0 / (sampleRate * 1.800));
            this.floorFall = (float) Math.exp(-1.0 / (sampleRate * 0.120));
            this.gainAttack = (float) Math.exp(-1.0 / (sampleRate * 0.005));
            this.gainRelease = (float) Math.exp(-1.0 / (sampleRate * 0.160));
        }

        float process(float x) {
            float absX = Math.abs(x);
            if (absX > fastEnv) {
                fastEnv = fastAttack * fastEnv + (1f - fastAttack) * absX;
            } else {
                fastEnv = fastRelease * fastEnv + (1f - fastRelease) * absX;
            }

            // Track steady background noise floor slowly upwards, fast downwards; clamp safely
            if (fastEnv < noiseFloor) {
                noiseFloor = floorFall * noiseFloor + (1f - floorFall) * fastEnv;
            } else if (fastEnv < noiseFloor * 2.2f) {
                noiseFloor = floorRise * noiseFloor + (1f - floorRise) * fastEnv;
            }
            noiseFloor = clamp(noiseFloor, 0.0025f, 0.028f);

            float openThresh = Math.max(0.014f, noiseFloor * 2.6f);
            float closeThresh = openThresh * 0.45f;

            float targetGain;
            if (fastEnv >= openThresh) {
                targetGain = 1.0f;
            } else if (fastEnv <= closeThresh) {
                targetGain = 0.10f; // -20 dB smooth background reduction
            } else {
                float t = (fastEnv - closeThresh) / (openThresh - closeThresh);
                // Smooth cubic ease curve (zero gate chatter)
                float smooth = t * t * (3f - 2f * t);
                targetGain = 0.10f + 0.90f * smooth;
            }

            if (targetGain > smoothGain) {
                smoothGain = gainAttack * smoothGain + (1f - gainAttack) * targetGain;
            } else {
                smoothGain = gainRelease * smoothGain + (1f - gainRelease) * targetGain;
            }
            return x * smoothGain;
        }
    }

    /**
     * Warm LP-Damped Studio Echo (3.5 kHz feedback damping -> musical, zero metallic ring).
     */
    private static final class StudioEcho {
        private final float[] line;
        private int pos = 0;
        private final float feedback;
        private final float lpAlpha;
        private float lpState = 0f;

        StudioEcho(int sampleRate, int delayMs, float feedback) {
            int len = Math.max(64, (int) (sampleRate * (delayMs / 1000.0)));
            this.line = new float[len];
            this.feedback = clamp(feedback, 0.05f, 0.72f);
            this.lpAlpha = (float) (1.0 - Math.exp(-2.0 * Math.PI * 3500.0 / sampleRate));
        }

        float process(float x) {
            float delayed = line[pos];
            lpState += lpAlpha * (delayed - lpState);
            line[pos] = x + lpState * feedback;
            pos = (pos + 1) % line.length;
            return lpState;
        }
    }

    /**
     * Exact RBJ Audio EQ Cookbook 2nd-order minimum-phase Biquad filter (zero block latency, zero ringing).
     */
    private static final class Biquad {
        private final float b0, b1, b2, a1, a2;
        private float x1 = 0f, x2 = 0f, y1 = 0f, y2 = 0f;

        private Biquad(double b0, double b1, double b2, double a0, double a1, double a2) {
            this.b0 = (float) (b0 / a0);
            this.b1 = (float) (b1 / a0);
            this.b2 = (float) (b2 / a0);
            this.a1 = (float) (a1 / a0);
            this.a2 = (float) (a2 / a0);
        }

        float process(float x) {
            float y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
            return y;
        }

        static Biquad highPass(int sr, double freq, double q) {
            double w0 = 2.0 * Math.PI * freq / sr;
            double cos = Math.cos(w0);
            double alpha = Math.sin(w0) / (2.0 * q);
            return new Biquad((1 + cos) / 2.0, -(1 + cos), (1 + cos) / 2.0, 1 + alpha, -2 * cos, 1 - alpha);
        }

        static Biquad lowPass(int sr, double freq, double q) {
            double w0 = 2.0 * Math.PI * freq / sr;
            double cos = Math.cos(w0);
            double alpha = Math.sin(w0) / (2.0 * q);
            return new Biquad((1 - cos) / 2.0, 1 - cos, (1 - cos) / 2.0, 1 + alpha, -2 * cos, 1 - alpha);
        }

        static Biquad bandPass(int sr, double freq, double q) {
            double w0 = 2.0 * Math.PI * freq / sr;
            double cos = Math.cos(w0);
            double alpha = Math.sin(w0) / (2.0 * q);
            return new Biquad(alpha, 0.0, -alpha, 1 + alpha, -2 * cos, 1 - alpha);
        }

        static Biquad peaking(int sr, double freq, double q, double gainDb) {
            double a = Math.pow(10.0, gainDb / 40.0);
            double w0 = 2.0 * Math.PI * freq / sr;
            double cos = Math.cos(w0);
            double alpha = Math.sin(w0) / (2.0 * q);
            return new Biquad(1 + alpha * a, -2 * cos, 1 - alpha * a, 1 + alpha / a, -2 * cos, 1 - alpha / a);
        }

        static Biquad lowShelf(int sr, double freq, double gainDb) {
            double a = Math.pow(10.0, gainDb / 40.0);
            double w0 = 2.0 * Math.PI * freq / sr;
            double cos = Math.cos(w0);
            double sin = Math.sin(w0);
            double sqrtA = Math.sqrt(a);
            double alpha = sin / 2.0 * Math.sqrt(2.0);
            double b0 = a * ((a + 1) - (a - 1) * cos + 2 * sqrtA * alpha);
            double b1 = 2 * a * ((a - 1) - (a + 1) * cos);
            double b2 = a * ((a + 1) - (a - 1) * cos - 2 * sqrtA * alpha);
            double a0 = (a + 1) + (a - 1) * cos + 2 * sqrtA * alpha;
            double a1 = -2 * ((a - 1) + (a + 1) * cos);
            double a2 = (a + 1) + (a - 1) * cos - 2 * sqrtA * alpha;
            return new Biquad(b0, b1, b2, a0, a1, a2);
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
