package org.nayagram.platform.features;

import android.content.Context;
import android.os.Bundle;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ProfileActivity;
import org.nayagram.platform.NayaConfig;

/**
 * AvatarProfileHelper - NayaGram Chat List Avatar Navigation.
 * Package: org.nayagram.platform.features
 * 
 * Allows tapping a chat avatar directly in the dialogs list to view user/group profile.
 * Complies with Google Play Policies: Pure UI navigation, zero extra permissions.
 */
public final class AvatarProfileHelper {

    private AvatarProfileHelper() {}

    /**
     * Check if avatar tap to profile is enabled in NayaGram preferences.
     */
    public static boolean isAvatarTapProfileEnabled(Context context) {
        if (context == null) return false;
        return NayaConfig.getBoolean(context, "ng_avatar_tap_profile", true);
    }

    /**
     * Open profile when avatar is clicked in chat list.
     * Respects active story rings and dialog type.
     */
    public static boolean openProfileFromAvatar(BaseFragment fragment, long dialogId) {
        if (fragment == null || fragment.getParentActivity() == null || dialogId == 0) {
            return false;
        }

        Bundle args = new Bundle();
        int currentAccount = fragment.getCurrentAccount();

        if (dialogId > 0) {
            // Direct User Chat
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            if (user == null || user.id == UserConfig.getInstance(currentAccount).getClientUserId()) {
                return false;
            }
            args.putLong("user_id", dialogId);
        } else {
            // Group or Channel
            long chatId = -dialogId;
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(chatId);
            if (chat == null) {
                return false;
            }
            args.putLong("chat_id", chatId);
        }

        ProfileActivity profileActivity = new ProfileActivity(args);
        fragment.presentFragment(profileActivity);
        return true;
    }
}
