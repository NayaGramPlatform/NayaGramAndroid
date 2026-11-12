package org.nayagram.platform.security;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
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
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/**
 * NGHiddenAccountsPasscodeDialog - Sleek Novagram-inspired full-screen PIN / Password & Fingerprint lock modal.
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

    private FrameLayout badgeContainer;
    private ImageView badgeIcon;
    private TextView promptView;
    private TextView errorView;
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
        Window w = getWindow();
        if (w != null) {
            w.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            w.setStatusBarColor(0xFF0B1120);
            w.setNavigationBarColor(0xFF0B1120);
        }

        Context ctx = getContext();
        FrameLayout root = new FrameLayout(ctx);
        GradientDrawable rootBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF0B1120, 0xFF111827, 0xFF0F172A}
        );
        root.setBackground(rootBg);

        // Top Bar
        LinearLayout topBar = new LinearLayout(ctx);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(8), dp(16), dp(16), dp(8));

        ImageView backBtn = new ImageView(ctx);
        backBtn.setImageResource(R.drawable.ic_ab_back);
        backBtn.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        backBtn.setPadding(dp(12), dp(12), dp(12), dp(12));
        backBtn.setOnClickListener(v -> dismiss());
        topBar.addView(backBtn, LayoutHelper.createLinear(48, 48));

        TextView topTitle = new TextView(ctx);
        topTitle.setText("Hidden accounts");
        topTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        topTitle.setTypeface(AndroidUtilities.bold());
        topTitle.setTextColor(Color.WHITE);
        topBar.addView(topTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

        root.addView(topBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // Center Content Column
        LinearLayout center = new LinearLayout(ctx);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        center.setPadding(dp(24), dp(72), dp(24), dp(24));

        // Circular badge icon (Novagram purple -> emerald on unlock)
        badgeContainer = new FrameLayout(ctx);
        setBadgeColor(0xFF7C3AED);
        badgeIcon = new ImageView(ctx);
        badgeIcon.setImageResource(R.drawable.ic_naya_eye_off);
        badgeIcon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        badgeContainer.addView(badgeIcon, LayoutHelper.createFrame(34, 34, Gravity.CENTER));
        center.addView(badgeContainer, LayoutHelper.createLinear(74, 74, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 20));

        promptView = new TextView(ctx);
        promptView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        promptView.setTypeface(AndroidUtilities.bold());
        promptView.setTextColor(Color.WHITE);
        promptView.setGravity(Gravity.CENTER);
        center.addView(promptView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        errorView = new TextView(ctx);
        errorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        errorView.setTextColor(0xFFF87171);
        errorView.setGravity(Gravity.CENTER);
        errorView.setVisibility(View.INVISIBLE);
        center.addView(errorView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        // 4 PIN Dots
        dotsRow = new LinearLayout(ctx);
        dotsRow.setOrientation(LinearLayout.HORIZONTAL);
        dotsRow.setGravity(Gravity.CENTER);
        for (int i = 0; i < 4; i++) {
            View dot = new View(ctx);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(Color.TRANSPARENT);
            d.setStroke(dp(2), 0x80FFFFFF);
            dot.setBackground(d);
            dots[i] = dot;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(16), dp(16));
            lp.setMargins(dp(10), 0, dp(10), 0);
            dotsRow.addView(dot, lp);
        }
        center.addView(dotsRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 24));

        // Alphanumeric Password Input Row (used only when inputType == PASSCODE_TYPE_PASSWORD)
        passwordInputRow = new LinearLayout(ctx);
        passwordInputRow.setOrientation(LinearLayout.HORIZONTAL);
        passwordInputRow.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable passBg = new GradientDrawable();
        passBg.setColor(0x1EFFFFFF);
        passBg.setCornerRadius(dp(14));
        passBg.setStroke(dp(1), 0x44FFFFFF);
        passwordInputRow.setBackground(passBg);
        passwordInputRow.setPadding(dp(16), dp(4), dp(8), dp(4));

        passwordEditText = new EditText(ctx);
        passwordEditText.setBackground(null);
        passwordEditText.setTextColor(Color.WHITE);
        passwordEditText.setHintTextColor(0x88FFFFFF);
        passwordEditText.setHint("Password");
        passwordEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passwordEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            submitPassword();
            return true;
        });
        passwordInputRow.addView(passwordEditText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView goBtn = new TextView(ctx);
        goBtn.setText("→");
        goBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        goBtn.setTypeface(AndroidUtilities.bold());
        goBtn.setTextColor(Color.WHITE);
        goBtn.setGravity(Gravity.CENTER);
        GradientDrawable goBg = new GradientDrawable();
        goBg.setShape(GradientDrawable.OVAL);
        goBg.setColor(0xFF7C3AED);
        goBtn.setBackground(goBg);
        goBtn.setOnClickListener(v -> submitPassword());
        passwordInputRow.addView(goBtn, LayoutHelper.createLinear(40, 40, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

        center.addView(passwordInputRow, LayoutHelper.createLinear(280, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        // Switch PIN / Password button (only during setup steps)
        modeSwitchBtn = new TextView(ctx);
        modeSwitchBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        modeSwitchBtn.setTextColor(0xFF38BDF8);
        modeSwitchBtn.setPadding(dp(16), dp(8), dp(16), dp(8));
        modeSwitchBtn.setOnClickListener(v -> {
            inputType = (inputType == SharedConfig.PASSCODE_TYPE_PIN)
                    ? SharedConfig.PASSCODE_TYPE_PASSWORD
                    : SharedConfig.PASSCODE_TYPE_PIN;
            currentPin.setLength(0);
            firstEntry = "";
            if (passwordEditText != null) passwordEditText.setText("");
            updateUiState();
        });
        center.addView(modeSwitchBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));

        // Numeric Keypad (Novagram clean rounded keys)
        keypadLayout = buildKeypad(ctx);
        center.addView(keypadLayout, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 0));

        root.addView(center, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        setContentView(root);

        updateUiState();
        if (mode == MODE_UNLOCK && NGHiddenAccounts.isFingerprintEnabled() && NGHiddenAccounts.isFingerprintHardwareAvailable()) {
            badgeContainer.postDelayed(this::triggerBiometricPrompt, 220);
        }
    }

    private LinearLayout buildKeypad(Context ctx) {
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
                row.addView(makeDigitButton(ctx, digit));
            }
            grid.addView(row);
        }

        // Bottom row: [Fingerprint or Spacer], [0], [Backspace]
        LinearLayout bottomRow = new LinearLayout(ctx);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.CENTER);

        if (mode == MODE_UNLOCK && NGHiddenAccounts.isFingerprintEnabled() && NGHiddenAccounts.isFingerprintHardwareAvailable()) {
            bottomRow.addView(makeIconKey(ctx, R.drawable.ic_naya_fingerprint, v -> triggerBiometricPrompt()));
        } else {
            View spacer = new View(ctx);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(74), dp(74));
            slp.setMargins(dp(12), dp(8), dp(12), dp(8));
            bottomRow.addView(spacer, slp);
        }

        bottomRow.addView(makeDigitButton(ctx, "0"));
        bottomRow.addView(makeBackspaceKey(ctx));
        grid.addView(bottomRow);

        return grid;
    }

    private View makeDigitButton(Context ctx, String digit) {
        TextView btn = new TextView(ctx);
        btn.setText(digit);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 28);
        btn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        btn.setTextColor(Color.WHITE);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(Theme.createSelectorDrawable(0x28FFFFFF, 1));
        btn.setOnClickListener(v -> onDigitPressed(digit));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(74), dp(74));
        lp.setMargins(dp(12), dp(8), dp(12), dp(8));
        btn.setLayoutParams(lp);
        return btn;
    }

    private View makeIconKey(Context ctx, int iconRes, View.OnClickListener listener) {
        ImageView btn = new ImageView(ctx);
        btn.setImageResource(iconRes);
        btn.setColorFilter(new PorterDuffColorFilter(0xFF38BDF8, PorterDuff.Mode.SRC_IN));
        btn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        btn.setPadding(dp(20), dp(20), dp(20), dp(20));
        btn.setBackground(Theme.createSelectorDrawable(0x28FFFFFF, 1));
        btn.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(74), dp(74));
        lp.setMargins(dp(12), dp(8), dp(12), dp(8));
        btn.setLayoutParams(lp);
        return btn;
    }

    private View makeBackspaceKey(Context ctx) {
        TextView btn = new TextView(ctx);
        btn.setText("⌫");
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        btn.setTextColor(0xCCFFFFFF);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(Theme.createSelectorDrawable(0x28FFFFFF, 1));
        btn.setOnClickListener(v -> {
            vibrateShort();
            if (currentPin.length() > 0) {
                currentPin.deleteCharAt(currentPin.length() - 1);
                updateDots();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(74), dp(74));
        lp.setMargins(dp(12), dp(8), dp(12), dp(8));
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
                showError("Incorrect passcode");
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
        setBadgeColor(0xFF22C55E);
        errorView.setVisibility(View.INVISIBLE);
        badgeContainer.animate().scaleX(1.12f).scaleY(1.12f).setDuration(140).withEndAction(() -> {
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
        dotsRow.setVisibility(isPin ? View.VISIBLE : View.GONE);
        keypadLayout.setVisibility(isPin ? View.VISIBLE : View.GONE);
        passwordInputRow.setVisibility(isPin ? View.GONE : View.VISIBLE);

        if (step == 0) {
            promptView.setText(mode == MODE_CHANGE ? "Enter current passcode" : "Enter passcode");
            modeSwitchBtn.setVisibility(View.GONE);
        } else if (step == 1) {
            promptView.setText(isPin ? "Create a 4-digit PIN" : "Create a password");
            modeSwitchBtn.setVisibility(View.VISIBLE);
            modeSwitchBtn.setText(isPin ? "Use Password instead" : "Use 4-Digit PIN instead");
        } else {
            promptView.setText(isPin ? "Re-enter your PIN" : "Re-enter your password");
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
                d.setColor(Color.WHITE);
                d.setStroke(0, 0);
            } else {
                d.setColor(Color.TRANSPARENT);
                d.setStroke(dp(2), 0x80FFFFFF);
            }
            dots[i].setBackground(d);
        }
    }

    private void setBadgeColor(int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        badgeContainer.setBackground(bg);
    }

    private void showError(String msg) {
        errorView.setText(msg);
        errorView.setVisibility(View.VISIBLE);
        AndroidUtilities.shakeView(dotsRow.getVisibility() == View.VISIBLE ? dotsRow : passwordInputRow);
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
            biometricCancellation = new CancellationSignal();
            BiometricPrompt.Builder builder = new BiometricPrompt.Builder(activity)
                    .setTitle("Hidden accounts")
                    .setSubtitle("Use your fingerprint to unlock hidden accounts")
                    .setNegativeButton("Use passcode", activity.getMainExecutor(), (dialog, which) -> {});
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

    private int dp(int val) {
        return AndroidUtilities.dp(val);
    }
}
