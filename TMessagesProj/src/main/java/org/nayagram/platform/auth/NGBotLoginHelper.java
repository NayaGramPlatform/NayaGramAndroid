package org.nayagram.platform.auth;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LoginActivity;

/**
 * NGBotLoginHelper handles bot token validation, UI dialog, accessibility, and MTProto authorization.
 */
public class NGBotLoginHelper {

    /**
     * Validates standard Telegram bot token pattern: <bot_id>:<secret>
     * e.g. 123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ123456789
     */
    public static boolean isValidBotToken(String token) {
        if (TextUtils.isEmpty(token)) {
            return false;
        }
        token = token.trim();
        int colonIndex = token.indexOf(':');
        if (colonIndex <= 0 || colonIndex >= token.length() - 1) {
            return false;
        }
        String idPart = token.substring(0, colonIndex);
        String secretPart = token.substring(colonIndex + 1);

        if (idPart.length() < 7 || idPart.length() > 14) {
            return false;
        }
        for (int i = 0; i < idPart.length(); i++) {
            if (!Character.isDigit(idPart.charAt(i))) {
                return false;
            }
        }
        return secretPart.length() >= 25;
    }

    /**
     * Formats error message with user-friendly text, never leaking sensitive auth details.
     */
    public static String getFriendlyErrorMessage(TLRPC.TL_error error) {
        if (error == null) {
            return "Network connection failed. Please check your internet connection.";
        }
        String errorText = error.text != null ? error.text : "";
        if (errorText.contains("ACCESS_TOKEN_INVALID")) {
            return "The bot token is invalid. Please check the token provided by @BotFather.";
        } else if (errorText.contains("ACCESS_TOKEN_EXPIRED")) {
            return "The bot token has expired or been revoked by @BotFather.";
        } else if (errorText.startsWith("FLOOD_WAIT")) {
            String seconds = errorText.replaceAll("[^0-9]", "");
            if (!TextUtils.isEmpty(seconds)) {
                return "Too many login attempts. Please wait " + seconds + " seconds and try again.";
            }
            return "Too many login attempts. Please try again later.";
        } else if (error.code == 500 || errorText.contains("INTERNAL")) {
            return "Telegram server error. Please try again shortly.";
        } else if (error.code == -1000 || errorText.contains("NETWORK") || errorText.contains("CONNECTION")) {
            return "Network connection failed. Please check your internet connection.";
        }
        return "Telegram error: " + errorText;
    }

    public static void showBotLoginDialog(final LoginActivity activity, final int currentAccount) {
        if (activity == null || activity.getParentActivity() == null) {
            return;
        }
        Context context = activity.getParentActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(20), AndroidUtilities.dp(24), AndroidUtilities.dp(10));

        // Header icon (decorative - hidden from TalkBack to avoid noise)
        FrameLayout iconFrame = new FrameLayout(context);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(Theme.getColor(Theme.key_chats_actionBackground));
        iconFrame.setBackground(iconBg);

        ImageView botIcon = new ImageView(context);
        botIcon.setImageResource(R.drawable.msg_bot);
        botIcon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        botIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        iconFrame.addView(botIcon, LayoutHelper.createFrame(26, 26, Gravity.CENTER));
        layout.addView(iconFrame, LayoutHelper.createLinear(52, 52, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 12));

        // Title
        TextView titleView = new TextView(context);
        titleView.setText(LocaleController.getString("BotTokenDialogTitle", R.string.BotTokenDialogTitle));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        titleView.setFocusable(true);
        layout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        // Description
        TextView descView = new TextView(context);
        descView.setText(LocaleController.getString("BotTokenDialogDescription", R.string.BotTokenDialogDescription));
        descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        descView.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        descView.setGravity(Gravity.CENTER_HORIZONTAL);
        descView.setFocusable(true);
        layout.addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Input field container (min 48dp touch target)
        FrameLayout inputContainer = new FrameLayout(context);
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(AndroidUtilities.dp(10));
        inputBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        inputBg.setStroke(AndroidUtilities.dp(1), Theme.getColor(Theme.key_divider));
        inputContainer.setBackground(inputBg);

        final EditText editText = new EditText(context);
        editText.setHint("123456789:ABCdefGhIJKlmNo...");
        editText.setContentDescription("Telegram bot token input field");
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        editText.setBackground(null);
        editText.setSingleLine(false);
        editText.setMaxLines(3);
        editText.setMinimumHeight(AndroidUtilities.dp(48));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        editText.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        inputContainer.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        layout.addView(inputContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        builder.setView(layout);
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            String token = editText.getText().toString().trim();
            if (TextUtils.isEmpty(token)) {
                Toast.makeText(context, "Please enter a bot token", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidBotToken(token)) {
                AlertsCreator.showSimpleAlert(activity, "Invalid Token Format",
                        "The token format is invalid. A bot token looks like: 123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ (obtainable from @BotFather).");
                return;
            }

            performBotLogin(activity, currentAccount, token);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private static void performBotLogin(final LoginActivity activity, final int currentAccount, final String token) {
        activity.showLoginProgress();

        FileLog.d("NGBotLoginHelper: Initiating bot login with token " + NGBotSessionManager.maskToken(token));

        TL_auth_importBotAuthorization req = new TL_auth_importBotAuthorization();
        req.flags = 0;
        req.api_id = BuildVars.APP_ID;
        req.api_hash = BuildVars.APP_HASH;
        req.bot_auth_token = token;

        ConnectionsManager.getInstance(currentAccount).sendRequest(
                req,
                (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                    activity.hideLoginProgress();

                    if (error == null && response instanceof TLRPC.TL_auth_authorization) {
                        FileLog.d("NGBotLoginHelper: Bot login succeeded for " + NGBotSessionManager.maskToken(token));
                        activity.handleBotAuthSuccess((TLRPC.TL_auth_authorization) response, token);
                    } else {
                        FileLog.e("NGBotLoginHelper: Bot login failed: " + (error != null ? error.text : "null response"));
                        String errorMsg = getFriendlyErrorMessage(error);
                        AlertsCreator.showSimpleAlert(activity, "Bot Login Failed", errorMsg);
                    }
                }),
                ConnectionsManager.RequestFlagFailOnServerErrors | ConnectionsManager.RequestFlagWithoutLogin
        );
    }
}
