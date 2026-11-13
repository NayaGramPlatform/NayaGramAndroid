package org.nayagram.platform.security;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/**
 * NGHiddenAccountsPasscodeDialog - Luminous, theme-adaptive PIN / Password & Fingerprint Vault Unlock UI.
 * - Eliminates the black screen (no FLAG_SECURE blackout, rich luminous gradient surface).
 * - Prominent interactive Fingerprint Sensor Visual Banner Card so biometric unlock is unmistakable.
 * - Glowing eye-shield badge and tactile elevated keypad buttons.
 */
public class NGHiddenAccountsPasscodeDialog extends Dialog {

    public static final int MODE_UNLOCK = 0;
    public static final int MODE_CREATE = 1;
    public static final int MODE_CHANGE = 2;

    private final Activity activity;
    private final int mode;
    private final Runnable onSuccess;

    private int step; // 0 = verify current (unlock/change), 1 = enter new, 2 = confirm new
    private int inputType = SharedConfig.PASSCODE_TYPE_PIN;
    private String firstEntry = "";
    private final StringBuilder currentPin = new StringBuilder();

    private boolean isDarkTheme;
    private FrameLayout outerHalo;
    private FrameLayout badgeContainer;
    private ImageView badgeIcon;
    private TextView promptView;
    private TextView subPromptView;
    private TextView errorView;
    private LinearLayout fingerprintCard;
    private LinearLayout dotsRow;
    private final View[] dots = new View[4];
    private LinearLayout keypadLayout;
    private LinearLayout passwordInputRow;
    private EditText passwordEditText;
    private TextView modeSwitchBtn;

    private CancellationSignal biometricCancellation;

    public NGHiddenAccountsPasscodeDialog(Activity activity, int mode, Runnable onSuccess) {
        super(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.activity = activity;
        this.mode = mode;
        this.onSuccess = onSuccess;
        if (mode == MODE_UNLOCK) {
            this.step = 0;
            this.inputType = NGHiddenAccounts.getCodeType();
        } else if (mode == MODE_CHANGE && NGHiddenAccounts.hasCode()) {
            this.step = 0;
            this.inputType = NGHiddenAccounts.getCodeType();
        } else {
            this.step = 1;
            this.inputType = SharedConfig.PASSCODE_TYPE_PIN;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        isDarkTheme = Theme.isCurrentThemeDark();

        int topBgColor = isDarkTheme ? 0xFF0F172A : 0xFFEFF6FF;
        int midBgColor = isDarkTheme ? 0xFF1E293B : 0xFFF8FAFC;
        int botBgColor = isDarkTheme ? 0xFF0F172A : 0xFFE0F2FE;

        Window w = getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            w.setStatusBarColor(topBgColor);
            w.setNavigationBarColor(botBgColor);
        }

        Context ctx = getContext();
        FrameLayout root = new FrameLayout(ctx);
        GradientDrawable rootBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{topBgColor, midBgColor, botBgColor}
        );
        root.setBackground(rootBg);

        int primaryTextColor = isDarkTheme ? 0xFFF8FAFC : 0xFF0F172A;
        int secondaryTextColor = isDarkTheme ? 0xFF94A3B8 : 0xFF475569;

        // Top Bar
        LinearLayout topBar = new LinearLayout(ctx);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(12), dp(14), dp(16), dp(8));

        FrameLayout backPill = new FrameLayout(ctx);
        GradientDrawable backBg = new GradientDrawable();
        backBg.setShape(GradientDrawable.OVAL);
        backBg.setColor(isDarkTheme ? 0x22FFFFFF : 0x140284C7);
        backBg.setStroke(dp(1), isDarkTheme ? 0x33FFFFFF : 0x330284C7);
        backPill.setBackground(backBg);
        ImageView backBtn = new ImageView(ctx);
        backBtn.setImageResource(R.drawable.ic_ab_back);
        backBtn.setColorFilter(new PorterDuffColorFilter(primaryTextColor, PorterDuff.Mode.SRC_IN));
        backPill.addView(backBtn, LayoutHelper.createFrame(22, 22, Gravity.CENTER));
        backPill.setOnClickListener(v -> dismiss());
        topBar.addView(backPill, LayoutHelper.createLinear(42, 42));

        LinearLayout titleCol = new LinearLayout(ctx);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        TextView topTitle = new TextView(ctx);
        topTitle.setText("Hidden Accounts Vault");
        topTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        topTitle.setTypeface(AndroidUtilities.bold());
        topTitle.setTextColor(primaryTextColor);
        titleCol.addView(topTitle);

        TextView topSub = new TextView(ctx);
        topSub.setText("Protected by NayaGram Biometric & Passcode Shield");
        topSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
        topSub.setTextColor(secondaryTextColor);
        titleCol.addView(topSub);

        topBar.addView(titleCol, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 12, 0, 0, 0));

        ScrollView scrollView = new ScrollView(ctx);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);

        // Center Content Column
        LinearLayout center = new LinearLayout(ctx);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        center.setPadding(dp(20), dp(68), dp(20), dp(24));

        // Glowing Outer Halo + Eye Badge Icon
        outerHalo = new FrameLayout(ctx);
        GradientDrawable haloBg = new GradientDrawable();
        haloBg.setShape(GradientDrawable.OVAL);
        haloBg.setColor(isDarkTheme ? 0x260EA5E9 : 0x1F0284C7);
        haloBg.setStroke(dp(1.5f), isDarkTheme ? 0x5538BDF8 : 0x440284C7);
        outerHalo.setBackground(haloBg);

        badgeContainer = new FrameLayout(ctx);
        setBadgeGradient(0xFF0284C7, 0xFF4F46E5);
        badgeIcon = new ImageView(ctx);
        badgeIcon.setImageResource(R.drawable.msg_view_file);
        badgeIcon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        badgeContainer.addView(badgeIcon, LayoutHelper.createFrame(32, 32, Gravity.CENTER));
        outerHalo.addView(badgeContainer, LayoutHelper.createFrame(64, 64, Gravity.CENTER));
        center.addView(outerHalo, LayoutHelper.createLinear(82, 82, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 12));

        promptView = new TextView(ctx);
        promptView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        promptView.setTypeface(AndroidUtilities.bold());
        promptView.setTextColor(primaryTextColor);
        promptView.setGravity(Gravity.CENTER);
        center.addView(promptView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        subPromptView = new TextView(ctx);
        subPromptView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subPromptView.setTextColor(secondaryTextColor);
        subPromptView.setGravity(Gravity.CENTER);
        center.addView(subPromptView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        errorView = new TextView(ctx);
        errorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        errorView.setTypeface(AndroidUtilities.bold());
        errorView.setTextColor(0xFFEF4444);
        errorView.setGravity(Gravity.CENTER);
        errorView.setVisibility(View.INVISIBLE);
        center.addView(errorView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // 4 PIN Dots inside a sleek pill holder
        LinearLayout dotsPill = new LinearLayout(ctx);
        dotsPill.setOrientation(LinearLayout.HORIZONTAL);
        dotsPill.setGravity(Gravity.CENTER);
        dotsPill.setPadding(dp(20), dp(12), dp(20), dp(12));
        GradientDrawable dotsPillBg = new GradientDrawable();
        dotsPillBg.setCornerRadius(dp(22));
        dotsPillBg.setColor(isDarkTheme ? 0x22FFFFFF : 0xFFFFFFFF);
        dotsPillBg.setStroke(dp(1), isDarkTheme ? 0x3338BDF8 : 0x330284C7);
        dotsPill.setBackground(dotsPillBg);

        dotsRow = new LinearLayout(ctx);
        dotsRow.setOrientation(LinearLayout.HORIZONTAL);
        dotsRow.setGravity(Gravity.CENTER);
        for (int i = 0; i < 4; i++) {
            View dot = new View(ctx);
            dots[i] = dot;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(16), dp(16));
            lp.setMargins(dp(10), 0, dp(10), 0);
            dotsRow.addView(dot, lp);
        }
        dotsPill.addView(dotsRow);
        center.addView(dotsPill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 14));

        // Interactive Visual Fingerprint Banner Card (visible when fingerprint unlock is available)
        boolean showBiometricBanner = (mode == MODE_UNLOCK && NGHiddenAccounts.isFingerprintEnabled() && NGHiddenAccounts.isFingerprintHardwareAvailable());
        fingerprintCard = buildFingerprintBannerCard(ctx);
        fingerprintCard.setVisibility(showBiometricBanner ? View.VISIBLE : View.GONE);
        center.addView(fingerprintCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 12, 0, 12, 14));

        // Alphanumeric Password Input Row (used when inputType == PASSCODE_TYPE_PASSWORD)
        passwordInputRow = new LinearLayout(ctx);
        passwordInputRow.setOrientation(LinearLayout.HORIZONTAL);
        passwordInputRow.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable passBg = new GradientDrawable();
        passBg.setColor(isDarkTheme ? 0x26FFFFFF : 0xFFFFFFFF);
        passBg.setCornerRadius(dp(16));
        passBg.setStroke(dp(1.5f), 0xFF0EA5E9);
        passwordInputRow.setBackground(passBg);
        passwordInputRow.setPadding(dp(16), dp(6), dp(8), dp(6));

        passwordEditText = new EditText(ctx);
        passwordEditText.setBackground(null);
        passwordEditText.setTextColor(primaryTextColor);
        passwordEditText.setHintTextColor(secondaryTextColor);
        passwordEditText.setHint("Enter vault password");
        passwordEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passwordEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            submitPassword();
            return true;
        });
        passwordInputRow.addView(passwordEditText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView goBtn = new TextView(ctx);
        goBtn.setText("Unlock →");
        goBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        goBtn.setTypeface(AndroidUtilities.bold());
        goBtn.setTextColor(Color.WHITE);
        goBtn.setGravity(Gravity.CENTER);
        goBtn.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable goBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF0284C7, 0xFF4F46E5}
        );
        goBg.setCornerRadius(dp(12));
        goBtn.setBackground(goBg);
        goBtn.setOnClickListener(v -> submitPassword());
        passwordInputRow.addView(goBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

        center.addView(passwordInputRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 16, 0, 16, 12));

        // Switch PIN / Password button (during setup steps)
        modeSwitchBtn = new TextView(ctx);
        modeSwitchBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        modeSwitchBtn.setTypeface(AndroidUtilities.bold());
        modeSwitchBtn.setTextColor(0xFF0284C7);
        modeSwitchBtn.setPadding(dp(16), dp(8), dp(16), dp(8));
        GradientDrawable switchBg = new GradientDrawable();
        switchBg.setCornerRadius(dp(16));
        switchBg.setColor(isDarkTheme ? 0x1F38BDF8 : 0x140284C7);
        switchBg.setStroke(dp(1), 0x440EA5E9);
        modeSwitchBtn.setBackground(switchBg);
        modeSwitchBtn.setOnClickListener(v -> {
            inputType = (inputType == SharedConfig.PASSCODE_TYPE_PIN)
                    ? SharedConfig.PASSCODE_TYPE_PASSWORD
                    : SharedConfig.PASSCODE_TYPE_PIN;
            currentPin.setLength(0);
            firstEntry = "";
            if (passwordEditText != null) passwordEditText.setText("");
            updateUiState();
        });
        center.addView(modeSwitchBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 10));

        // Numeric Keypad (tactile rounded card buttons)
        keypadLayout = buildKeypad(ctx, primaryTextColor);
        center.addView(keypadLayout, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));

        scrollView.addView(center);
        root.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        root.addView(topBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));
        setContentView(root);

        updateUiState();
        if (showBiometricBanner) {
            badgeContainer.postDelayed(this::triggerBiometricPrompt, 240);
        }
    }

    private LinearLayout buildFingerprintBannerCard(Context ctx) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));

        GradientDrawable cardBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                isDarkTheme
                        ? new int[]{0x330284C7, 0x334F46E5}
                        : new int[]{0xFFE0F2FE, 0xFFEEF2FF}
        );
        cardBg.setCornerRadius(dp(16));
        cardBg.setStroke(dp(1.5f), isDarkTheme ? 0x8838BDF8 : 0x660284C7);
        card.setBackground(cardBg);

        FrameLayout fpCircle = new FrameLayout(ctx);
        GradientDrawable fpCircleBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0EA5E9, 0xFF2563EB}
        );
        fpCircleBg.setShape(GradientDrawable.OVAL);
        fpCircle.setBackground(fpCircleBg);

        ImageView fpIcon = new ImageView(ctx);
        fpIcon.setImageResource(R.drawable.ic_naya_fingerprint);
        fpIcon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        fpCircle.addView(fpIcon, LayoutHelper.createFrame(24, 24, Gravity.CENTER));
        card.addView(fpCircle, LayoutHelper.createLinear(42, 42));

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(ctx);
        title.setText("Fingerprint Unlock Active");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(isDarkTheme ? 0xFFF8FAFC : 0xFF0369A1);
        textCol.addView(title);

        TextView sub = new TextView(ctx);
        sub.setText("Touch your sensor or tap here to scan fingerprint");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
        sub.setTextColor(isDarkTheme ? 0xFFBAE6FD : 0xFF0284C7);
        textCol.addView(sub);

        card.addView(textCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 12, 0, 8, 0));

        TextView scanBadge = new TextView(ctx);
        scanBadge.setText("Scan");
        scanBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        scanBadge.setTypeface(AndroidUtilities.bold());
        scanBadge.setTextColor(Color.WHITE);
        scanBadge.setPadding(dp(12), dp(6), dp(12), dp(6));
        GradientDrawable scanBg = new GradientDrawable();
        scanBg.setCornerRadius(dp(12));
        scanBg.setColor(0xFF0284C7);
        scanBadge.setBackground(scanBg);
        card.addView(scanBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        card.setOnClickListener(v -> {
            vibrateShort();
            triggerBiometricPrompt();
        });
        return card;
    }

    private LinearLayout buildKeypad(Context ctx, int textColor) {
        LinearLayout grid = new LinearLayout(ctx);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setGravity(Gravity.CENTER_HORIZONTAL);

        String[][] rows = new String[][]{
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"}
        };
        for (String[] r : rows) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            for (String digit : r) {
                row.addView(makeDigitButton(ctx, digit, textColor));
            }
            grid.addView(row);
        }

        // Bottom row: [Fingerprint or Clear], [0], [Backspace]
        LinearLayout bottomRow = new LinearLayout(ctx);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.CENTER);

        if (mode == MODE_UNLOCK && NGHiddenAccounts.isFingerprintEnabled() && NGHiddenAccounts.isFingerprintHardwareAvailable()) {
            bottomRow.addView(makeIconKey(ctx, R.drawable.ic_naya_fingerprint, v -> triggerBiometricPrompt()));
        } else {
            bottomRow.addView(makeClearKey(ctx));
        }

        bottomRow.addView(makeDigitButton(ctx, "0", textColor));
        bottomRow.addView(makeBackspaceKey(ctx, textColor));
        grid.addView(bottomRow);

        return grid;
    }

    private View makeDigitButton(Context ctx, String digit, int textColor) {
        TextView btn = new TextView(ctx);
        btn.setText(digit);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 26);
        btn.setTypeface(AndroidUtilities.bold());
        btn.setTextColor(textColor);
        btn.setGravity(Gravity.CENTER);

        GradientDrawable keyBg = new GradientDrawable();
        keyBg.setShape(GradientDrawable.OVAL);
        keyBg.setColor(isDarkTheme ? 0x24FFFFFF : 0xFFFFFFFF);
        keyBg.setStroke(dp(1), isDarkTheme ? 0x33FFFFFF : 0x260284C7);
        btn.setBackground(keyBg);

        btn.setOnClickListener(v -> onDigitPressed(digit));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(70), dp(70));
        lp.setMargins(dp(11), dp(6), dp(11), dp(6));
        btn.setLayoutParams(lp);
        return btn;
    }

    private View makeIconKey(Context ctx, int iconRes, View.OnClickListener listener) {
        FrameLayout holder = new FrameLayout(ctx);
        GradientDrawable keyBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0EA5E9, 0xFF4F46E5}
        );
        keyBg.setShape(GradientDrawable.OVAL);
        holder.setBackground(keyBg);

        ImageView btn = new ImageView(ctx);
        btn.setImageResource(iconRes);
        btn.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        holder.addView(btn, LayoutHelper.createFrame(30, 30, Gravity.CENTER));
        holder.setOnClickListener(listener);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(70), dp(70));
        lp.setMargins(dp(11), dp(6), dp(11), dp(6));
        holder.setLayoutParams(lp);
        return holder;
    }

    private View makeClearKey(Context ctx) {
        TextView btn = new TextView(ctx);
        btn.setText("C");
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        btn.setTypeface(AndroidUtilities.bold());
        btn.setTextColor(isDarkTheme ? 0xFF94A3B8 : 0xFF64748B);
        btn.setGravity(Gravity.CENTER);

        GradientDrawable keyBg = new GradientDrawable();
        keyBg.setShape(GradientDrawable.OVAL);
        keyBg.setColor(isDarkTheme ? 0x16FFFFFF : 0xFFE2E8F0);
        btn.setBackground(keyBg);
        btn.setOnClickListener(v -> {
            vibrateShort();
            currentPin.setLength(0);
            updateDots();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(70), dp(70));
        lp.setMargins(dp(11), dp(6), dp(11), dp(6));
        btn.setLayoutParams(lp);
        return btn;
    }

    private View makeBackspaceKey(Context ctx, int textColor) {
        TextView btn = new TextView(ctx);
        btn.setText("⌫");
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        btn.setTypeface(AndroidUtilities.bold());
        btn.setTextColor(textColor);
        btn.setGravity(Gravity.CENTER);

        GradientDrawable keyBg = new GradientDrawable();
        keyBg.setShape(GradientDrawable.OVAL);
        keyBg.setColor(isDarkTheme ? 0x24FFFFFF : 0xFFFFFFFF);
        keyBg.setStroke(dp(1), isDarkTheme ? 0x33FFFFFF : 0x260284C7);
        btn.setBackground(keyBg);
        btn.setOnClickListener(v -> {
            vibrateShort();
            if (currentPin.length() > 0) {
                currentPin.deleteCharAt(currentPin.length() - 1);
                updateDots();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(70), dp(70));
        lp.setMargins(dp(11), dp(6), dp(11), dp(6));
        btn.setLayoutParams(lp);
        return btn;
    }

    private void onDigitPressed(String digit) {
        vibrateShort();
        errorView.setVisibility(View.INVISIBLE);
        if (currentPin.length() >= 4) return;
        currentPin.append(digit);
        updateDots();
        if (currentPin.length() == 4) {
            String entered = currentPin.toString();
            badgeContainer.postDelayed(() -> handleEnteredCode(entered), 90);
        }
    }

    private void submitPassword() {
        String entered = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
        if (entered.length() < 4) {
            showError("Password must be at least 4 characters");
            return;
        }
        handleEnteredCode(entered);
    }

    private void handleEnteredCode(String entered) {
        if (step == 0) {
            if (NGHiddenAccounts.checkCode(entered)) {
                if (mode == MODE_CHANGE) {
                    step = 1;
                    currentPin.setLength(0);
                    if (passwordEditText != null) passwordEditText.setText("");
                    updateUiState();
                } else {
                    succeedAndClose();
                }
            } else {
                vibrateError();
                currentPin.setLength(0);
                if (passwordEditText != null) passwordEditText.setText("");
                updateDots();
                showError("Incorrect passcode. Please try again.");
            }
        } else if (step == 1) {
            firstEntry = entered;
            step = 2;
            currentPin.setLength(0);
            if (passwordEditText != null) passwordEditText.setText("");
            updateUiState();
        } else if (step == 2) {
            if (entered.equals(firstEntry)) {
                NGHiddenAccounts.saveCode(entered, inputType);
                succeedAndClose();
            } else {
                vibrateError();
                firstEntry = "";
                step = 1;
                currentPin.setLength(0);
                if (passwordEditText != null) passwordEditText.setText("");
                updateUiState();
                showError("Passcodes do not match. Try again.");
            }
        }
    }

    private void succeedAndClose() {
        setBadgeGradient(0xFF10B981, 0xFF059669);
        errorView.setVisibility(View.INVISIBLE);
        promptView.setText("Vault Unlocked ✓");
        outerHalo.animate().scaleX(1.12f).scaleY(1.12f).setDuration(140).withEndAction(() -> {
            if (onSuccess != null) {
                onSuccess.run();
            }
            try {
                dismiss();
            } catch (Throwable ignored) {}
        }).start();
    }

    private void updateUiState() {
        boolean isPin = (inputType == SharedConfig.PASSCODE_TYPE_PIN);
        dotsRow.getParent().requestLayout();
        ((View) dotsRow.getParent()).setVisibility(isPin ? View.VISIBLE : View.GONE);
        keypadLayout.setVisibility(isPin ? View.VISIBLE : View.GONE);
        passwordInputRow.setVisibility(isPin ? View.GONE : View.VISIBLE);

        if (step == 0) {
            promptView.setText(mode == MODE_CHANGE ? "Verify Current Passcode" : "Unlock Hidden Accounts");
            subPromptView.setText(NGHiddenAccounts.isFingerprintEnabled() && NGHiddenAccounts.isFingerprintHardwareAvailable()
                    ? "Use your fingerprint sensor or enter your 4-digit PIN below"
                    : "Enter your secret passcode to view hidden accounts");
            modeSwitchBtn.setVisibility(View.GONE);
        } else if (step == 1) {
            promptView.setText(isPin ? "Create a 4-Digit Vault PIN" : "Create a Vault Password");
            subPromptView.setText("This passcode protects your hidden accounts vault");
            modeSwitchBtn.setVisibility(View.VISIBLE);
            modeSwitchBtn.setText(isPin ? "Switch to Password instead" : "Switch to 4-Digit PIN instead");
        } else {
            promptView.setText(isPin ? "Confirm Your 4-Digit PIN" : "Confirm Your Password");
            subPromptView.setText("Re-enter the same code to activate protection");
            modeSwitchBtn.setVisibility(View.GONE);
        }

        updateDots();
        if (!isPin) {
            passwordEditText.requestFocus();
            AndroidUtilities.showKeyboard(passwordEditText);
        } else {
            AndroidUtilities.hideKeyboard(passwordEditText);
        }
    }

    private void updateDots() {
        int len = currentPin.length();
        for (int i = 0; i < 4; i++) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            if (i < len) {
                d.setColor(0xFF0EA5E9);
                d.setStroke(0, 0);
            } else {
                d.setColor(Color.TRANSPARENT);
                d.setStroke(dp(2), isDarkTheme ? 0x6694A3B8 : 0x660284C7);
            }
            dots[i].setBackground(d);
        }
    }

    private void setBadgeGradient(int startColor, int endColor) {
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{startColor, endColor}
        );
        bg.setShape(GradientDrawable.OVAL);
        badgeContainer.setBackground(bg);
    }

    private void showError(String msg) {
        errorView.setText(msg);
        errorView.setVisibility(View.VISIBLE);
        AndroidUtilities.shakeView(((View) dotsRow.getParent()).getVisibility() == View.VISIBLE ? (View) dotsRow.getParent() : passwordInputRow);
    }

    private void vibrateShort() {
        if (!NGHiddenAccounts.isVibrateEnabled()) return;
        try {
            Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= 26) {
                    v.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(18);
                }
            }
        } catch (Throwable ignored) {}
    }

    private void vibrateError() {
        if (!NGHiddenAccounts.isVibrateEnabled()) return;
        try {
            Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= 26) {
                    v.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(180);
                }
            }
        } catch (Throwable ignored) {}
    }

    private void triggerBiometricPrompt() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || activity == null || activity.isFinishing()) {
            return;
        }
        try {
            if (biometricCancellation != null) {
                try { biometricCancellation.cancel(); } catch (Throwable ignored) {}
            }
            biometricCancellation = new CancellationSignal();
            BiometricPrompt.Builder builder = new BiometricPrompt.Builder(activity)
                    .setTitle("Hidden Accounts Vault")
                    .setSubtitle("Touch the fingerprint sensor to unlock your hidden accounts")
                    .setNegativeButton("Use PIN / Passcode", activity.getMainExecutor(), (dialog, which) -> {});
            builder.build().authenticate(biometricCancellation, activity.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    AndroidUtilities.runOnUIThread(() -> succeedAndClose());
                }
            });
        } catch (Throwable ignored) {}
    }

    @Override
    public void dismiss() {
        if (biometricCancellation != null) {
            try {
                biometricCancellation.cancel();
            } catch (Throwable ignored) {}
            biometricCancellation = null;
        }
        super.dismiss();
    }

    private int dp(float val) {
        return AndroidUtilities.dp(val);
    }
}
