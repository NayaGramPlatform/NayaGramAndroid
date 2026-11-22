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
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.ContactsController;
import org.telegram.ui.LaunchActivity;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
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
        // Redact any possible token patterns or sensitive echoes from raw error
        String sanitized = errorText.replaceAll("\\b\\d{6,12}:[A-Za-z0-9_-]{20,}\\b", "[REDACTED_TOKEN]");
        return "Telegram error: " + sanitized;
    }

    public static void showBotLoginDialog(final LoginActivity activity, final int currentAccount) {
        showBotLoginDialog((BaseFragment) activity, currentAccount);
    }

    public static void showBotLoginDialog(final BaseFragment fragment, final int currentAccount) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        Context context = fragment.getParentActivity();

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
                AlertsCreator.showSimpleAlert(fragment, "Invalid Token Format",
                        "The token format is invalid. A bot token looks like: 123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ (obtainable from @BotFather).");
                return;
            }

            performBotLogin(fragment, currentAccount, token);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private static void performBotLogin(final BaseFragment fragment, final int currentAccount, final String token) {
        final AlertDialog[] progress = new AlertDialog[1];
        if (fragment instanceof LoginActivity) {
            ((LoginActivity) fragment).showLoginProgress();
        } else if (fragment != null && fragment.getParentActivity() != null) {
            progress[0] = new AlertDialog(fragment.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
            progress[0].showDelayed(300);
        }
        FileLog.d("NGBotLoginHelper: Initiating bot login with token " + NGBotSessionManager.maskToken(token));
        ConnectionsManager cm = ConnectionsManager.getInstance(currentAccount);
        if (cm.isTestBackend()) {
            cm.switchBackend(false);
        }
        cm.cleanup(false);
        TL_auth_importBotAuthorization req = new TL_auth_importBotAuthorization();
        req.flags = 0;
        req.api_id = BuildVars.APP_ID;
        req.api_hash = BuildVars.APP_HASH;
        req.bot_auth_token = token;
        cm.sendRequest(
                req,
                (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                    if (fragment instanceof LoginActivity) {
                        ((LoginActivity) fragment).hideLoginProgress();
                    } else if (progress[0] != null) {
                        try { progress[0].dismiss(); } catch (Exception ignored) {}
                    }
                    if (error == null && response instanceof TLRPC.TL_auth_authorization) {
                        FileLog.d("NGBotLoginHelper: Bot login succeeded for " + NGBotSessionManager.maskToken(token));
                        if (fragment instanceof LoginActivity) {
                            ((LoginActivity) fragment).handleBotAuthSuccess((TLRPC.TL_auth_authorization) response, token);
                        } else {
                            handleBotAuthSuccessDirect(currentAccount, (TLRPC.TL_auth_authorization) response, token);
                        }
                    } else {
                        String rawErr = error != null ? error.text : "null response";
                        String safeErr = rawErr != null ? rawErr.replaceAll("\\b\\d{6,12}:[A-Za-z0-9_-]{20,}\\b", "[REDACTED_TOKEN]") : "null";
                        FileLog.e("NGBotLoginHelper: Bot login failed: " + safeErr);
                        String errorMsg = getFriendlyErrorMessage(error);
                        AlertsCreator.showSimpleAlert(fragment, "Bot Login Failed", errorMsg);
                    }
                }),
                ConnectionsManager.RequestFlagFailOnServerErrors
                        | ConnectionsManager.RequestFlagWithoutLogin
                        | ConnectionsManager.RequestFlagTryDifferentDc
                        | ConnectionsManager.RequestFlagEnableUnauthorized
        );
    }

    public static void handleBotAuthSuccessDirect(int currentAccount, TLRPC.TL_auth_authorization res, String token) {
        if (res == null || res.user == null) return;
        MessagesController.getInstance(currentAccount).cleanup();
        ConnectionsManager.getInstance(currentAccount).setUserId(res.user.id);
        UserConfig.getInstance(currentAccount).clearConfig();
        MessagesController.getInstance(currentAccount).cleanup();
        UserConfig.getInstance(currentAccount).setCurrentUser(res.user);
        UserConfig.getInstance(currentAccount).saveConfig(true);
        MessagesStorage.getInstance(currentAccount).cleanup(true);
        java.util.ArrayList<TLRPC.User> users = new java.util.ArrayList<>();
        users.add(res.user);
        MessagesStorage.getInstance(currentAccount).putUsersAndChats(users, null, true, true);
        MessagesController.getInstance(currentAccount).putUser(res.user, false);
        ContactsController.getInstance(currentAccount).checkAppAccount();
        MessagesController.getInstance(currentAccount).checkPromoInfo(true);
        ConnectionsManager.getInstance(currentAccount).updateDcSettings();
        MessagesController.getInstance(currentAccount).loadAppConfig();
        MessagesController.getInstance(currentAccount).loadWebBrowserConfig();
        MessagesController.getInstance(currentAccount).checkPeerColors(false);
        if (token != null) {
            NGBotSessionManager.saveBotSession(currentAccount, token, res.user);
        }
        MessagesController.getInstance(currentAccount).putDialogsEndReachedAfterRegistration();
        seedBotWelcomeWorkspace(currentAccount, res.user);
        if (LaunchActivity.instance != null) {
            LaunchActivity.instance.switchToAccount(currentAccount, true);
        }
    }

    /**
     * Seeds a local Saved Messages guide thread for Bot sessions so the chat list is not blank
     * and the full Chat UI (Composer, Floating Formatting Bar, Voice Changer, Themes, Hub) can be inspected.
     */
    public static void seedBotWelcomeWorkspace(int currentAccount, TLRPC.User botUser) {
        if (botUser == null || !botUser.bot || botUser.id == 0) {
            return;
        }
        try {
            int now = Math.max(ConnectionsManager.getInstance(currentAccount).getCurrentTime(), (int) (System.currentTimeMillis() / 1000L));
            String handle = !TextUtils.isEmpty(botUser.username) ? "@" + botUser.username : "Bot Account";

            TLRPC.TL_message m1 = new TLRPC.TL_message();
            m1.id = 1;
            m1.dialog_id = botUser.id;
            m1.peer_id = new TLRPC.TL_peerUser();
            m1.peer_id.user_id = botUser.id;
            m1.from_id = new TLRPC.TL_peerUser();
            m1.from_id.user_id = botUser.id;
            m1.date = now - 2;
            m1.out = false;
            m1.unread = false;
            m1.media = new TLRPC.TL_messageMediaEmpty();
            m1.message = "Welcome to NayaGram (" + handle + ")! Open this chat to explore the Smart Composer, Floating Formatting Bar, Glowing AI button, Voice Changer Studio HD, Stickers, Themes, and NayaGram Hub.";

            TLRPC.TL_message m2 = new TLRPC.TL_message();
            m2.id = 2;
            m2.dialog_id = botUser.id;
            m2.peer_id = new TLRPC.TL_peerUser();
            m2.peer_id.user_id = botUser.id;
            m2.from_id = new TLRPC.TL_peerUser();
            m2.from_id.user_id = botUser.id;
            m2.date = now;
            m2.out = false;
            m2.unread = false;
            m2.media = new TLRPC.TL_messageMediaEmpty();
            m2.message = "Telegram Bot API Note: Bot accounts can reply to any user who sends /start to " + handle + ". Per Telegram MTProto limits, VoIP Audio/Video calls and initiating unsolicited DMs require a phone-verified user account.";

            TLRPC.TL_dialog dialog = new TLRPC.TL_dialog();
            dialog.id = botUser.id;
            dialog.peer = new TLRPC.TL_peerUser();
            dialog.peer.user_id = botUser.id;
            dialog.top_message = m2.id;
            dialog.last_message_date = m2.date;
            dialog.read_inbox_max_id = m2.id;
            dialog.read_outbox_max_id = m2.id;
            dialog.unread_count = 0;
            dialog.pinned = true;
            dialog.pinnedNum = 1;
            dialog.folder_id = 0;

            TLRPC.TL_messages_dialogs dialogsRes = new TLRPC.TL_messages_dialogs();
            dialogsRes.dialogs.add(dialog);
            dialogsRes.messages.add(m2);
            dialogsRes.users.add(botUser);

            java.util.ArrayList<TLRPC.Message> msgs = new java.util.ArrayList<>(2);
            msgs.add(m1);
            msgs.add(m2);

            MessagesStorage.getInstance(currentAccount).putMessages(msgs, true, true, false, 0, org.telegram.ui.ChatActivity.MODE_DEFAULT, 0);
            MessagesStorage.getInstance(currentAccount).putDialogs(dialogsRes, 0);
            MessagesController.getInstance(currentAccount).processLoadedDialogs(dialogsRes, null, null, 0, 0, 20, 0, false, false, true);
        } catch (Throwable t) {
            FileLog.e("NGBotLoginHelper: seedBotWelcomeWorkspace failed", t);
        }
    }
}
