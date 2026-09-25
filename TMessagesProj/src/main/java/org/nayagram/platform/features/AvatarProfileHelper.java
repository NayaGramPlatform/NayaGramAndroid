package org.nayagram.platform.features;

import android.content.Context;
import android.os.Bundle;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.Stories.StoriesUtilities;
import org.nayagram.platform.NayaConfig;

/**
 * AvatarProfileHelper - NayaGram Chat List Avatar Navigation.
 * Package: org.nayagram.platform.features
 *
 * Handles:
 * 1. User & Group Avatar Click -> Opens Profile.
 * 2. Active Story Ring Priority -> If story is available, story viewing wins.
 * 3. Fling/Scroll suppression -> Touch events ignored during fast scrolling.
 * 4. Long Press Safety -> Standard preview/action preserved.
 * 5. Safe Back Navigation -> Fragment stack returns cleanly to chat list.
 *
 * Google Play Console Compliance:
 * - 100% Client-side UI Navigation.
 * - Zero background processing, zero extra Android permissions, zero tracking.
 */
public final class AvatarProfileHelper {

    private AvatarProfileHelper() {}

    /**
     * Preference key for Avatar Tap Profile navigation.
     */
    public static final String PREF_KEY_AVATAR_TAP_PROFILE = "ng_avatar_tap_profile";

    /**
     * Check if avatar tap to profile feature is enabled.
     */
    public static boolean isAvatarTapProfileEnabled(Context context) {
        if (context == null) return true;
        return NayaConfig.getBoolean(context, PREF_KEY_AVATAR_TAP_PROFILE, true);
    }

    /**
     * Decide if avatar should claim touch event.
     * Prevents false triggers during fling or when stories take priority.
     *
     * @param isScrolling Whether the parent RecyclerView / list is actively flinging/scrolling.
     * @param hasStories Whether the avatar has an active, unviewed story ring.
     * @return true if avatar tap to profile can be processed.
     */
    public static boolean shouldClaimAvatarTap(boolean isScrolling, boolean hasStories) {
        if (isScrolling) {
            return false; // Suppress during fast scroll
        }
        if (hasStories) {
            return false; // Story ring takes precedence
        }
        return true;
    }

    /**
     * Open profile when avatar is clicked in chat list.
     *
     * @param fragment Current parent fragment (DialogsActivity)
     * @param dialogId ID of the chat/user
     * @param storyState Story ring rendering state (e.g., StoriesUtilities.STATE_EMPTY)
     * @param isScrolling True if list is flinging
     * @return true if profile navigation succeeded
     */
    public static boolean onAvatarClicked(BaseFragment fragment, long dialogId, int storyState, boolean isScrolling) {
        if (fragment == null || fragment.getParentActivity() == null || dialogId == 0) {
            return false;
        }

        // 1. Check if flinging/scrolling
        if (isScrolling) {
            return false;
        }

        // 2. Story priority check: If story is drawn/active, let story viewer take over
        if (storyState != StoriesUtilities.STATE_EMPTY) {
            return false;
        }

        // 3. Check user preference
        if (!isAvatarTapProfileEnabled(fragment.getParentActivity())) {
            return false;
        }

        int currentAccount = fragment.getCurrentAccount();
        Bundle args = new Bundle();

        if (dialogId > 0) {
            // Direct User
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            if (user == null || user.id == UserConfig.getInstance(currentAccount).getClientUserId()) {
                return false; // Don't navigate on own self-chat or non-existent user
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

        // 4. Back navigation: Set clean navigation to return to DialogsActivity
        ProfileActivity profileActivity = new ProfileActivity(args);
        // presentFragment pushes onto the backstack naturally so Android/Telegram Back button pops back
        return fragment.presentFragment(profileActivity);
    }
}
