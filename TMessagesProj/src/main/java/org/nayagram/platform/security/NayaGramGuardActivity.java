package org.nayagram.platform.security;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.util.List;

/** Settings, enrollment and verification UI for the optional local-only NayaGram Guard. */
public class NayaGramGuardActivity extends BaseFragment {
    private final boolean gateMode;
    private final boolean startSetup;
    private LinearLayout content;
    private EditText codeInput;
    private FrameLayout codeFrame;
    private LinearLayout digitBoxes;
    private TextView backupModeToggle;
    private boolean backupEntryMode;
    private String pendingSecret;
    private boolean suppressToggleCallback;
    private boolean setupCompleted;

    public NayaGramGuardActivity() {
        this(false, false);
    }

    public NayaGramGuardActivity(boolean gateMode) {
        this(gateMode, false);
    }

    public NayaGramGuardActivity(boolean gateMode, boolean startSetup) {
        this.gateMode = gateMode;
        this.startSetup = startSetup;
    }

    @Override
    public View createView(Context context) {
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(gateMode ? "NayaGram Guard" : "NayaGram Guard");
        actionBar.setBackButtonImage(gateMode ? 0 : R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if (id == -1 && !gateMode) finishFragment();
            }
        });

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(18), AndroidUtilities.dp(20), AndroidUtilities.dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        fragmentView = scroll;

        if (gateMode) {
            buildVerification(context, true);
        } else if (startSetup) {
            try {
                pendingSecret = NayaGramGuard.beginSetup(context);
                showSetup(context, pendingSecret);
            } catch (Exception e) {
                toast(context, "Secure storage is unavailable. Guard was not enabled.");
                finishFragment();
            }
        } else {
            buildSettings(context);
        }
        return fragmentView;
    }

    @Override
    public void onFragmentDestroy() {
        if (pendingSecret != null && !setupCompleted) {
            NayaGramGuard.cancelSetup(ApplicationLoader.applicationContext);
        }
        super.onFragmentDestroy();
    }

    private void buildSettings(Context context) {
        addTitle(context, "🛡️ NayaGram Guard");
        addText(context, "An optional app-level authenticator layer. Telegram OTP and Telegram Two-Step Verification remain unchanged.");
        LinearLayout card = new LinearLayout(context);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        card.setBackground(round(0xFFFFFFFF, 0xFFBAE6FD));
        card.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(card, params(0, 12));
        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(LinearLayout.VERTICAL);
        card.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView label = text(context, "NayaGram Guard (Google Authenticator)", 15, 0xFF075985, true);
        labels.addView(label);
        TextView status = text(context, NayaGramGuard.isEnabled(context) ? "Enabled · code required before main page" : "Disabled by default", 12, 0xFF475569, false);
        labels.addView(status);
        Switch toggle = new Switch(context);
        toggle.setChecked(NayaGramGuard.isEnabled(context));
        card.addView(toggle);
        toggle.setOnCheckedChangeListener((button, checked) -> {
            if (suppressToggleCallback) return;
            if (checked) {
                try {
                    pendingSecret = NayaGramGuard.beginSetup(context);
                    showSetup(context, pendingSecret);
                } catch (Exception e) {
                    setToggleSilently(toggle, false);
                    toast(context, "Secure storage is unavailable. Guard was not enabled.");
                }
            } else {
                new org.telegram.ui.ActionBar.AlertDialog.Builder(context)
                        .setTitle("Disable NayaGram Guard?")
                        .setMessage("This removes the local authenticator secret and unused backup codes. Telegram login is not changed.")
                        .setNegativeButton("Cancel", (dialog, which) -> setToggleSilently(toggle, true))
                        .setPositiveButton("Disable", (dialog, which) -> {
                            try {
                                NayaGramGuard.disable(context);
                                status.setText("Disabled by default");
                            } catch (Exception e) {
                                setToggleSilently(toggle, true);
                                toast(context, "Could not clear secure Guard storage.");
                            }
                        }).show();
            }
        });
        Button autoLock = button(context, "Auto-Lock timeout: " + autoLockLabel(NayaGramGuard.getAutoLockTimeoutMs(context)));
        content.addView(autoLock, params(0, 10));
        autoLock.setOnClickListener(v -> {
            String[] choices = {"Immediately", "15 minutes", "45 minutes", "1 hour", "3 hours", "5 hours", "12 hours", "Custom"};
            long[] values = {0L, 15L * 60_000L, 45L * 60_000L, 60L * 60_000L, 3L * 60L * 60_000L, 5L * 60L * 60_000L, 12L * 60L * 60_000L};
            new org.telegram.ui.ActionBar.AlertDialog.Builder(context)
                    .setTitle("NG Guard Auto-Lock")
                    .setItems(choices, (dialog, which) -> {
                        if (which < values.length) {
                            saveAutoLockTimeout(context, autoLock, values[which]);
                        } else {
                            EditText custom = new EditText(context);
                            custom.setInputType(InputType.TYPE_CLASS_NUMBER);
                            custom.setHint("Minutes (1–10080)");
                            new org.telegram.ui.ActionBar.AlertDialog.Builder(context)
                                    .setTitle("Custom Auto-Lock")
                                    .setView(custom)
                                    .setNegativeButton("Cancel", null)
                                    .setPositiveButton("Save", (d, w) -> {
                                        String raw = custom.getText().toString().trim();
                                        try {
                                            long minutes = Long.parseLong(raw);
                                            if (minutes < 1 || minutes > 10080) {
                                                toast(context, "Enter 1–10080 minutes.");
                                                return;
                                            }
                                            saveAutoLockTimeout(context, autoLock, minutes * 60_000L);
                                        } catch (Exception e) {
                                            toast(context, "Enter a valid number of minutes.");
                                        }
                                    }).show();
                        }
                    }).show();
        });
        addText(context, "Auto-Lock controls how long Guard stays verified while NayaGram is in the background. If secure settings cannot be read, Guard locks.");
        addText(context, "Privacy: Local TOTP only, no data sent to Google server. Your secret stays on this device in encrypted storage.");
    }

    private void saveAutoLockTimeout(Context context, Button button, long timeoutMs) {
        try {
            NayaGramGuard.setAutoLockTimeoutMs(context, timeoutMs);
            button.setText("Auto-Lock timeout: " + autoLockLabel(timeoutMs));
            toast(context, "Auto-Lock timeout saved.");
        } catch (Exception e) {
            toast(context, "Could not save Auto-Lock timeout securely.");
        }
    }

    private String autoLockLabel(long timeoutMs) {
        if (timeoutMs == 0L) return "Immediately";
        if (timeoutMs % (60L * 60_000L) == 0L) return (timeoutMs / (60L * 60_000L)) + " hour" + (timeoutMs == 60L * 60_000L ? "" : "s");
        return (timeoutMs / 60_000L) + " minutes";
    }

    private void setToggleSilently(Switch toggle, boolean checked) {
        suppressToggleCallback = true;
        toggle.setChecked(checked);
        suppressToggleCallback = false;
    }

    private void showSetup(Context context, String secret) {
        content.removeAllViews();
        addTitle(context, "Scan with Google Authenticator");
        addText(context, "Scan this QR code, or add the secret manually. Then enter the current six-digit code to confirm setup.");
        ImageView qr = new ImageView(context);
        qr.setAdjustViewBounds(true);
        qr.setScaleType(ImageView.ScaleType.FIT_CENTER);
        try {
            String uri = NayaGramGuard.getOtpAuthUri("user", secret);
            BitMatrix matrix = new MultiFormatWriter().encode(uri, BarcodeFormat.QR_CODE, 640, 640);
            Bitmap bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < 640; x++) {
                for (int y = 0; y < 640; y++) bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
            qr.setImageBitmap(bitmap);
            qr.setBackground(round(Color.WHITE, 0xFFBAE6FD));
            qr.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
            LinearLayout.LayoutParams qrParams = params(0, 14);
            qrParams.width = AndroidUtilities.dp(240);
            qrParams.height = AndroidUtilities.dp(240);
            qrParams.gravity = Gravity.CENTER_HORIZONTAL;
            content.addView(qr, qrParams);
        } catch (Exception e) {
            addText(context, "QR could not be generated. Use the manual secret below.");
        }
        TextView secretText = text(context, secret, 14, 0xFF075985, true);
        secretText.setTextIsSelectable(true);
        secretText.setGravity(Gravity.CENTER);
        secretText.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        secretText.setBackground(round(Color.WHITE, 0xFFBAE6FD));
        content.addView(secretText, params(0, 8));
        Button copy = button(context, "Copy Secret");
        content.addView(copy, params(0, 8));
        copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("NayaGram Guard secret", secret));
            toast(context, "Secret copied");
        });
        buildCodeEntry(context, false);
        Button confirm = button(context, "Verify & Enable Guard");
        content.addView(confirm, params(0, 10));
        confirm.setOnClickListener(v -> {
            try {
                List<String> backups = NayaGramGuard.confirmSetup(context, codeInput.getText().toString().trim());
                if (backups == null) {
                    codeInput.setError("Enter the current valid 6-digit code");
                    return;
                }
                setupCompleted = true;
                showBackupCodes(context, backups);
            } catch (Exception e) {
                toast(context, "Setup could not be saved securely.");
            }
        });
        Button cancel = button(context, "Cancel setup");
        content.addView(cancel, params(0, 8));
        cancel.setOnClickListener(v -> {
            try {
                NayaGramGuard.cancelSetup(context);
            } catch (Exception e) {
                toast(context, "Could not clear the pending secret securely.");
                return;
            }
            NayaGramGuard.lockSession();
            finishFragment();
        });
    }

    private void showBackupCodes(Context context, List<String> backups) {
        content.removeAllViews();
        addTitle(context, "Save your 10 backup codes");
        addText(context, "Each code works once. Store these privately; they are shown only now. Only encrypted hashes are retained on this device.");
        for (int i = 0; i < backups.size(); i++) {
            TextView code = text(context, (i + 1) + ".  " + backups.get(i), 15, 0xFF075985, true);
            code.setGravity(Gravity.CENTER);
            code.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(7), AndroidUtilities.dp(10), AndroidUtilities.dp(7));
            code.setBackground(round(Color.WHITE, 0xFFBAE6FD));
            content.addView(code, params(0, 5));
        }
        Button copy = button(context, "Copy backup codes");
        content.addView(copy, params(0, 12));
        copy.setOnClickListener(v -> {
            StringBuilder all = new StringBuilder();
            for (String code : backups) all.append(code).append('\n');
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("NayaGram Guard backup codes", all.toString()));
            toast(context, "Backup codes copied");
        });
        Button done = button(context, "Finish");
        content.addView(done, params(0, 8));
        done.setOnClickListener(v -> finishFragment());
    }

    private void buildVerification(Context context, boolean gate) {
        addTitle(context, gate ? "🔐 Unlock NayaGram" : "Verify NayaGram Guard");
        addText(context, "Enter your current six-digit authenticator code. A saved one-time backup code can also be used.");
        buildCodeEntry(context, true);
        Button verify = button(context, "Verify and continue");
        content.addView(verify, params(0, 10));
        verify.setOnClickListener(v -> {
            if (NayaGramGuard.verifyStoredCode(context, codeInput.getText().toString().trim())) {
                finishFragment();
            } else {
                codeInput.setError("Invalid or already-used code");
                codeInput.setText("");
            }
        });
    }

    private void buildCodeEntry(Context context, boolean allowBackup) {
        backupEntryMode = false;
        codeFrame = new FrameLayout(context);
        codeFrame.setBackground(round(Color.WHITE, 0xFF06B6D4));

        digitBoxes = new LinearLayout(context);
        digitBoxes.setOrientation(LinearLayout.HORIZONTAL);
        digitBoxes.setGravity(Gravity.CENTER);
        digitBoxes.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6));
        for (int i = 0; i < 6; i++) {
            TextView box = text(context, "", 22, 0xFF075985, true);
            box.setGravity(Gravity.CENTER);
            box.setBackground(round(Color.WHITE, 0xFF06B6D4));
            LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(0, AndroidUtilities.dp(44), 1f);
            boxParams.setMargins(AndroidUtilities.dp(3), 0, AndroidUtilities.dp(3), 0);
            digitBoxes.addView(box, boxParams);
        }
        codeFrame.addView(digitBoxes, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(58), Gravity.CENTER));

        codeInput = new EditText(context);
        codeInput.setSingleLine(true);
        codeInput.setTextSize(22);
        codeInput.setLetterSpacing(0.22f);
        codeInput.setGravity(Gravity.CENTER);
        codeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        codeInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        codeInput.setTextColor(Color.TRANSPARENT);
        codeInput.setHintTextColor(Color.TRANSPARENT);
        codeInput.setCursorVisible(false);
        codeInput.setBackgroundColor(Color.TRANSPARENT);
        codeInput.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
        codeFrame.addView(codeInput, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(58), Gravity.CENTER));
        codeInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!backupEntryMode && digitBoxes != null) {
                    for (int i = 0; i < digitBoxes.getChildCount(); i++) {
                        TextView box = (TextView) digitBoxes.getChildAt(i);
                        box.setText(i < s.length() ? String.valueOf(s.charAt(i)) : "");
                    }
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });
        content.addView(codeFrame, params(0, 8));

        Button pasteCode = button(context, "Paste code");
        content.addView(pasteCode, params(0, 6));
        pasteCode.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null || !clipboard.hasPrimaryClip()) {
                toast(context, "Clipboard has no code to paste.");
                return;
            }
            CharSequence clip = clipboard.getPrimaryClip().getItemAt(0).coerceToText(context);
            String value = clip == null ? "" : clip.toString();
            if (backupEntryMode) {
                value = value.replaceAll("\\s+", "").toUpperCase(Locale.US);
                if (value.length() > 10) value = value.substring(0, 10);
            } else {
                value = value.replaceAll("\\D", "");
                if (value.length() > 6) value = value.substring(0, 6);
            }
            codeInput.setText(value);
            codeInput.setSelection(value.length());
        });

        if (allowBackup) {
            backupModeToggle = text(context, "Use a one-time backup code", 13, 0xFF0284C7, true);
            backupModeToggle.setGravity(Gravity.CENTER);
            backupModeToggle.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4));
            content.addView(backupModeToggle, params(0, 8));
            backupModeToggle.setOnClickListener(v -> {
                backupEntryMode = !backupEntryMode;
                codeInput.setText("");
                if (backupEntryMode) {
                    digitBoxes.setVisibility(View.GONE);
                    codeInput.setAlpha(1f);
                    codeInput.setTextColor(0xFF075985);
                    codeInput.setHintTextColor(0xFF94A3B8);
                    codeInput.setCursorVisible(true);
                    codeInput.setHint("Enter backup code");
                    codeInput.setGravity(Gravity.CENTER);
                    codeInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
                    codeInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
                    codeInput.setBackground(round(Color.WHITE, 0xFF06B6D4));
                    backupModeToggle.setText("Use authenticator code");
                } else {
                    digitBoxes.setVisibility(View.VISIBLE);
                    codeInput.setAlpha(0.01f);
                    codeInput.setTextColor(Color.TRANSPARENT);
                    codeInput.setHintTextColor(Color.TRANSPARENT);
                    codeInput.setCursorVisible(false);
                    codeInput.setHint("");
                    codeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                    codeInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
                    codeInput.setBackgroundColor(Color.TRANSPARENT);
                    backupModeToggle.setText("Use a one-time backup code");
                }
                codeInput.requestFocus();
            });
        }
        codeFrame.setOnClickListener(v -> {
            codeInput.requestFocus();
            codeInput.setSelection(codeInput.length());
        });
    }

    private void addTitle(Context context, String value) {
        TextView title = text(context, value, 22, 0xFF075985, true);
        title.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(8));
        content.addView(title, params(0, 6));
    }

    private void addText(Context context, String value) {
        TextView label = text(context, value, 13, 0xFF334155, false);
        label.setLineSpacing(AndroidUtilities.dp(2), 1.05f);
        content.addView(label, params(0, 10));
    }

    private TextView text(Context context, String value, int size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(Context context, String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setBackground(round(0xFF0284C7, 0xFF06B6D4));
        return button;
    }

    private GradientDrawable round(int fill, int stroke) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{fill, fill});
        drawable.setCornerRadius(AndroidUtilities.dp(14));
        drawable.setStroke(AndroidUtilities.dp(1), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams params(int weight, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        lp.bottomMargin = AndroidUtilities.dp(bottom);
        return lp;
    }

    private void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        // Returning false consumes the back press in Telegram's BaseFragment contract.
        if (gateMode) return false;
        return super.onBackPressed(invoked);
    }
}
