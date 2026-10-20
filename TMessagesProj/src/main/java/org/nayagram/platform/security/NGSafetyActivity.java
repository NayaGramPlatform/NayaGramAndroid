package org.nayagram.platform.security;

import android.content.ClipData;
import android.content.ContentValues;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.CountDownTimer;
import android.os.Build;
import android.provider.MediaStore;
import java.io.OutputStream;
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

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.util.List;

/**
 * Settings, enrollment, and verification UI for NG Authenticator (NGSafety).
 * Local-only RFC 6238 TOTP authentication layer with 15 one-time backup codes
 * and Auto-lock settings access (Settings → NG Safety → Auto-lock).
 */
public class NGSafetyActivity extends BaseFragment {
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
    private CountDownTimer codeCountdown;
    private TextView countdownView;

    public NGSafetyActivity() {
        this(false, false);
    }

    public NGSafetyActivity(boolean gateMode) {
        this(gateMode, false);
    }

    public NGSafetyActivity(boolean gateMode, boolean startSetup) {
        this.gateMode = gateMode;
        this.startSetup = startSetup;
    }

    @Override
    public View createView(Context context) {
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("NG Authenticator");
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
                pendingSecret = NGSafety.beginSetup(context);
                showSetup(context, pendingSecret);
            } catch (Exception e) {
                toast(context, "Secure storage is unavailable. NG Authenticator was not enabled.");
                finishFragment();
            }
        } else {
            buildSettings(context);
        }
        return fragmentView;
    }

    @Override
    public void onFragmentDestroy() {
        if (codeCountdown != null) {
            codeCountdown.cancel();
            codeCountdown = null;
        }
        if (pendingSecret != null && !setupCompleted) {
            NGSafety.cancelSetup(ApplicationLoader.applicationContext);
        }
        super.onFragmentDestroy();
    }

    private void buildSettings(Context context) {
        addTitle(context, "NG Authenticator");
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
        TextView label = text(context, "NG Authenticator", 15, 0xFF075985, true);
        labels.addView(label);
        TextView status = text(context, NGSafety.isEnabled(context) ? "Enabled · code required before main page" : "Disabled by default", 12, 0xFF475569, false);
        labels.addView(status);
        Switch toggle = new Switch(context);
        toggle.setChecked(NGSafety.isEnabled(context));
        applyToggleColors(toggle, toggle.isChecked());
        card.addView(toggle);
        toggle.setOnCheckedChangeListener((button, checked) -> {
            applyToggleColors(toggle, checked);
            if (suppressToggleCallback) return;
            if (checked) {
                try {
                    pendingSecret = NGSafety.beginSetup(context);
                    showSetup(context, pendingSecret);
                } catch (Exception e) {
                    setToggleSilently(toggle, false);
                    toast(context, "Secure storage is unavailable. NG Authenticator was not enabled.");
                }
            } else {
                new AlertDialog.Builder(context)
                        .setTitle("Disable NG Authenticator?")
                        .setMessage("This removes the local authenticator secret and unused backup codes. Telegram login is not changed.")
                        .setNegativeButton("Cancel", (dialog, which) -> setToggleSilently(toggle, true))
                        .setPositiveButton("Disable", (dialog, which) -> {
                            try {
                                NGSafety.disable(context);
                                status.setText("Disabled by default");
                            } catch (Exception e) {
                                setToggleSilently(toggle, true);
                                toast(context, "Could not clear secure authenticator storage.");
                            }
                        }).show();
            }
        });

        // Auto-lock configuration card (Settings → NG Safety → Auto-lock)
        LinearLayout autoLockCard = new LinearLayout(context);
        autoLockCard.setOrientation(LinearLayout.HORIZONTAL);
        autoLockCard.setGravity(Gravity.CENTER_VERTICAL);
        autoLockCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        autoLockCard.setBackground(round(0xFFFFFFFF, 0xFFBAE6FD));
        content.addView(autoLockCard, params(0, 12));

        LinearLayout autoLockLabels = new LinearLayout(context);
        autoLockLabels.setOrientation(LinearLayout.VERTICAL);
        autoLockCard.addView(autoLockLabels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView autoLockTitle = text(context, "Auto-lock", 15, 0xFF075985, true);
        autoLockLabels.addView(autoLockTitle);

        int currentMins = NGSafety.getAutoLockMinutes(context);
        TextView autoLockSubtitle = text(context, NGSafety.getAutoLockDisplayString(currentMins), 12, 0xFF475569, false);
        autoLockLabels.addView(autoLockSubtitle);

        ImageView arrow = new ImageView(context);
        arrow.setImageResource(R.drawable.arrow_more);
        arrow.setColorFilter(new PorterDuffColorFilter(0xFF0284C7, PorterDuff.Mode.SRC_IN));
        autoLockCard.addView(arrow, new LinearLayout.LayoutParams(AndroidUtilities.dp(16), AndroidUtilities.dp(16)));

        autoLockCard.setOnClickListener(v -> presentFragment(new NGAutoLockActivity()));

        addText(context, "Privacy: Local TOTP only, no data sent to Google server. Your secret stays on this device in encrypted storage.");
        Button details = button(context, "Details and recovery help");
        content.addView(details, params(0, 8));
        details.setOnClickListener(v -> new AlertDialog.Builder(context)
                .setTitle("NG Authenticator · Help")
                .setMessage("NG Authenticator is an optional local protection layer added after Telegram login. It does not replace Telegram OTP or Telegram Two-Step Verification.\n\nSetup: scan the QR code with an authenticator app, or enter the displayed secret manually, then verify a current six-digit code.\n\nBackup codes: save all 15 codes somewhere private and secure. Each code works once; used codes cannot be reused. Do not share your secret or backup codes.\n\nIf you disable NG Authenticator, its local secret and unused backup codes are cleared. If you lose both your authenticator and backup codes, you may lose access to this local protection flow.")
                .setPositiveButton("Got it", null)
                .show());
    }

    private void applyToggleColors(Switch toggle, boolean checked) {
        int thumb = checked ? 0xFF16A34A : 0xFF7C4DFF;
        int track = checked ? 0xFFBBF7D0 : 0xFFEDE9FE;
        toggle.setThumbTintList(ColorStateList.valueOf(thumb));
        toggle.setTrackTintList(ColorStateList.valueOf(track));
    }

    private void setToggleSilently(Switch toggle, boolean checked) {
        suppressToggleCallback = true;
        toggle.setChecked(checked);
        suppressToggleCallback = false;
    }

    private void showSetup(Context context, String secret) {
        content.removeAllViews();
        ImageView brandLogo = new ImageView(context);
        brandLogo.setImageResource(R.drawable.ic_nayagram_authenticator);
        brandLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams brandLogoParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(88), AndroidUtilities.dp(88));
        brandLogoParams.gravity = Gravity.CENTER_HORIZONTAL;
        brandLogoParams.bottomMargin = AndroidUtilities.dp(8);
        content.addView(brandLogo, brandLogoParams);

        addTitle(context, "NG Authenticator");
        addText(context, "Scan this QR code, or add the secret manually. Then enter the current six-digit code to confirm setup.");
        ImageView qr = new ImageView(context);
        qr.setAdjustViewBounds(true);
        qr.setScaleType(ImageView.ScaleType.FIT_CENTER);
        final Bitmap[] qrBitmap = new Bitmap[1];
        try {
            String uri = NGSafety.getOtpAuthUri("user", secret);
            Bitmap bitmap = NGQRGenerator.generate(context, uri, 720);
            qrBitmap[0] = bitmap;
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
        if (qrBitmap[0] != null) {
            Button saveQr = button(context, "Download QR code");
            content.addView(saveQr, params(0, 8));
            saveQr.setOnClickListener(v -> saveQrImage(context, qrBitmap[0]));
        }
        TextView secretText = text(context, secret, 14, 0xFF075985, true);
        secretText.setTextIsSelectable(true);
        secretText.setGravity(Gravity.CENTER);
        secretText.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        secretText.setBackground(round(Color.WHITE, 0xFFBAE6FD));
        content.addView(secretText, params(0, 8));
        Button revealSecret = button(context, "Hide Secret");
        content.addView(revealSecret, params(0, 6));
        revealSecret.setOnClickListener(v -> {
            boolean hidden = secretText.getText().toString().equals("••••••••••••••••••••••••••••••••");
            String masked = "••••••••••••••••••••••••••••••••";
            secretText.setText(hidden ? secret : masked);
            revealSecret.setText(hidden ? "Hide Secret" : "Show Secret");
        });
        Button copy = button(context, "Copy Secret");
        content.addView(copy, params(0, 8));
        copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("NG Authenticator secret", secret));
            toast(context, "Secret copied");
        });
        buildCodeEntry(context, false);
        Button pasteSetup = button(context, "Paste authenticator code");
        content.addView(pasteSetup, params(0, 6));
        pasteSetup.setOnClickListener(v -> pasteCodeFromClipboard(context));
        Button confirm = button(context, "Verify & Enable");
        content.addView(confirm, params(0, 10));
        confirm.setOnClickListener(v -> {
            try {
                List<String> backups = NGSafety.confirmSetup(context, codeInput.getText().toString().trim());
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
                NGSafety.cancelSetup(context);
            } catch (Exception e) {
                toast(context, "Could not clear the pending secret securely.");
                return;
            }
            NGSafety.lockSession();
            finishFragment();
        });
    }

    private void showBackupCodes(Context context, List<String> backups) {
        content.removeAllViews();
        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.ic_nayagram_authenticator);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(76), AndroidUtilities.dp(76));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        content.addView(logo, logoParams);

        addTitle(context, "Save your 15 backup codes");
        addText(context, "Each code works once. Five codes are shown at a time; scroll to view the rest. Only encrypted hashes are retained on this device.");

        ScrollView codesScroll = new ScrollView(context);
        codesScroll.setVerticalScrollBarEnabled(true);
        codesScroll.setBackground(round(0xFFF0FAFF, 0xFF67D7F2));
        LinearLayout codeList = new LinearLayout(context);
        codeList.setOrientation(LinearLayout.VERTICAL);
        codeList.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
        codesScroll.addView(codeList, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for (int i = 0; i < backups.size(); i++) {
            TextView code = text(context, (i + 1) + ".  " + backups.get(i), 15, 0xFF075985, true);
            code.setGravity(Gravity.CENTER);
            code.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
            code.setBackground(round(Color.WHITE, 0xFFBAE6FD));
            codeList.addView(code, params(0, 5));
        }
        LinearLayout.LayoutParams scrollParams = params(0, 10);
        scrollParams.height = AndroidUtilities.dp(240);
        content.addView(codesScroll, scrollParams);
        TextView hint = text(context, "Swipe up or down to view all 15 codes", 12, 0xFF0284C7, true);
        hint.setGravity(Gravity.CENTER);
        content.addView(hint, params(0, 8));

        Button copy = button(context, "Copy all backup codes");
        content.addView(copy, params(0, 12));
        copy.setOnClickListener(v -> {
            StringBuilder all = new StringBuilder();
            for (int i = 0; i < backups.size(); i++) all.append(i + 1).append(". ").append(backups.get(i)).append('\n');
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("NG Authenticator backup codes", all.toString()));
            toast(context, "Backup codes copied");
        });
        Button done = button(context, "Finish");
        content.addView(done, params(0, 8));
        done.setOnClickListener(v -> finishFragment());
    }

    private void buildVerification(Context context, boolean gate) {
        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
        content.addView(hero, params(0, 12));

        TextView slogan = text(context, LocaleController.getString(R.string.NGProtectionTagline), 15, 0xFF00D4FF, true);
        slogan.setGravity(Gravity.CENTER);
        slogan.setPadding(0, 0, 0, AndroidUtilities.dp(8));
        hero.addView(slogan, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView heroLogo = new ImageView(context);
        heroLogo.setImageResource(R.drawable.nayagram_protection_hero);
        heroLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        heroLogo.setAdjustViewBounds(true);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(116), AndroidUtilities.dp(116));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        hero.addView(heroLogo, logoParams);

        TextView heroTitle = text(context, gate ? "Unlock NayaGram" : "Verify NG Authenticator", 24, 0xFF075985, true);
        heroTitle.setGravity(Gravity.CENTER);
        heroTitle.setPadding(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(4));
        hero.addView(heroTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View spacer = new View(context);
        content.addView(spacer, new LinearLayout.LayoutParams(1, AndroidUtilities.dp(24)));

        addText(context, "Enter your current six-digit authenticator code. A saved one-time backup code can also be used.");
        buildCodeEntry(context, true);
        Button pasteCode = button(context, "Paste code");
        content.addView(pasteCode, params(0, 6));
        pasteCode.setOnClickListener(v -> pasteCodeFromClipboard(context));
        Button verify = button(context, "Verify and continue");
        content.addView(verify, params(0, 10));
        verify.setOnClickListener(v -> {
            if (NGSafety.verifyStoredCode(context, codeInput.getText().toString().trim())) {
                finishFragment();
            } else {
                codeInput.setError("Invalid or already-used code");
                codeInput.setText("");
            }
        });

        TextView footer = text(context, LocaleController.getString(R.string.NGAuthenticatorCreatedDate), 12, 0xFF64748B, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, AndroidUtilities.dp(16), 0, AndroidUtilities.dp(4));
        content.addView(footer, params(0, 8));
    }

    private void saveQrImage(Context context, Bitmap bitmap) {
        if (Build.VERSION.SDK_INT < 29) {
            toast(context, "QR download requires Android 10 or newer. Use the manual secret instead.");
            return;
        }
        OutputStream output = null;
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "NG-Authenticator-QR.png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NayaGram");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
            android.net.Uri imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri == null) throw new IllegalStateException("Could not create image entry");
            output = context.getContentResolver().openOutputStream(imageUri);
            if (output == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                context.getContentResolver().delete(imageUri, null, null);
                throw new IllegalStateException("Could not write QR image");
            }
            output.close();
            output = null;
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.Images.Media.IS_PENDING, 0);
            context.getContentResolver().update(imageUri, ready, null, null);
            toast(context, "QR saved to Pictures/NayaGram");
        } catch (Exception e) {
            toast(context, "Could not save QR code. Keep the secret private.");
        } finally {
            if (output != null) try { output.close(); } catch (Exception ignored) { }
        }
    }

    private void pasteCodeFromClipboard(Context context) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null || !clipboard.hasPrimaryClip()) {
                toast(context, "Clipboard is empty");
                return;
            }
            ClipData clip = clipboard.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) {
                toast(context, "Clipboard is empty");
                return;
            }
            CharSequence value = clip.getItemAt(0).coerceToText(context);
            if (value == null) {
                toast(context, "No code found in clipboard");
                return;
            }
            String pasted = value.toString().trim().replaceAll("\\s+", "");
            codeInput.setText(pasted);
            codeInput.setSelection(codeInput.length());
            codeInput.requestFocus();
        } catch (Exception e) {
            toast(context, "Could not read clipboard");
        }
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

        countdownView = text(context, "", 15, 0xFF0369A1, true);
        countdownView.setGravity(Gravity.CENTER);
        countdownView.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        countdownView.setBackground(round(0xFFE0F7FF, 0xFF06B6D4));
        content.addView(countdownView, params(0, 8));
        startCodeCountdown();

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

    private void startCodeCountdown() {
        if (codeCountdown != null) codeCountdown.cancel();
        long elapsed = System.currentTimeMillis() % 30000L;
        long remaining = 30000L - elapsed;
        codeCountdown = new CountDownTimer(remaining, 1000L) {
            @Override public void onTick(long millisUntilFinished) {
                long seconds = Math.max(1L, (millisUntilFinished + 999L) / 1000L);
                if (countdownView != null) {
                    countdownView.setText("⏱ New code in " + seconds + "s");
                    countdownView.setTextColor(seconds <= 5 ? 0xFFB45309 : 0xFF0369A1);
                    countdownView.setBackground(round(seconds <= 5 ? 0xFFFFF7ED : 0xFFE0F7FF,
                            seconds <= 5 ? 0xFFF59E0B : 0xFF06B6D4));
                }
            }
            @Override public void onFinish() {
                if (countdownView != null) {
                    countdownView.setText("⟳ New code window");
                    countdownView.setTextColor(0xFF0369A1);
                }
                startCodeCountdown();
            }
        }.start();
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
        if (gateMode) return false;
        return super.onBackPressed(invoked);
    }
}
