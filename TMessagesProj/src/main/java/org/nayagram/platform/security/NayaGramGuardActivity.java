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
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.util.List;

/** Settings, enrollment and verification UI for the optional local-only NayaGram Guard. */
public class NayaGramGuardActivity extends BaseFragment {
    private final boolean gateMode;
    private LinearLayout content;
    private EditText codeInput;
    private String pendingSecret;
    private boolean suppressToggleCallback;

    public NayaGramGuardActivity() {
        this(false);
    }

    public NayaGramGuardActivity(boolean gateMode) {
        this.gateMode = gateMode;
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
        } else {
            buildSettings(context);
        }
        return fragmentView;
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
        addText(context, "Privacy: Local TOTP only, no data sent to Google server. Your secret stays on this device in encrypted storage.");
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
                showBackupCodes(context, backups);
            } catch (Exception e) {
                toast(context, "Setup could not be saved securely.");
            }
        });
        Button cancel = button(context, "Cancel setup");
        content.addView(cancel, params(0, 8));
        cancel.setOnClickListener(v -> {
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
            for (String code : backups) all.append(code).append('\\n');
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
        codeInput = new EditText(context);
        codeInput.setSingleLine(true);
        codeInput.setTextSize(22);
        codeInput.setLetterSpacing(0.22f);
        codeInput.setGravity(Gravity.CENTER);
        codeInput.setHint(allowBackup ? "000000 / backup code" : "000000");
        codeInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        codeInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(allowBackup ? 10 : 6)});
        codeInput.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        codeInput.setBackground(round(Color.WHITE, 0xFF06B6D4));
        content.addView(codeInput, params(0, 8));
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
