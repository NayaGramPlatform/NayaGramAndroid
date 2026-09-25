package org.nayagram.platform.features;

import android.content.Context;
import android.os.Bundle;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.Stories.StoriesUtilities;
import org.nayagram.platform.NayaConfig;

/**
 * AvatarProfileHelper - Chat List Avatar Profile Navigation.
 * Package: org.nayagram.platform.features
 *
 * Provides safe, clean avatar tap-to-profile navigation for NayaGram.
 * 
 * Safety & Edge-Case Rules:
 * 1. Default OFF (Play Store safe & non-intrusive; user can toggle in NG Feature hub).
 * 2. Scroll/Fling Guard: During fling or scrolling (scrollState != IDLE), touch is declined
 *    so RecyclerView absorbs it as a scroll-stop rather than mis-firing a click.
 * 3. Saved Messages & Self Chat: Returns 0 (remains user's personal storage).
 * 4. Replies & Pseudo-chats: Returns 0.
 * 5. Secret Chats & Search Results & Archive: Returns 0.
 * 6. Action Mode: Returns 0 (avatar acts as selection checkbox).
 * 7. Story Precedence: If any story ring is drawn (unread, muted, or active), story viewer takes priority.
 * 8. Back Navigation: Passes removeFragmentOnChatOpen=true so opening chat from profile
 *    removes the intermediate profile screen, ensuring Back always lands cleanly on the chat list.
 */
public final class AvatarProfileHelper {

    private AvatarProfileHelper() {}

    public static final String PREF_KEY_AVATAR_TAP_PROFILE = "ng_avatar_tap_profile";

    /**
     * Check if feature is enabled in preferences. Defaults to false for Play Store safety.
     */
    public static boolean isEnabled(Context context) {
        if (context == null) return false;
        return NayaConfig.getBoolean(context, PREF_KEY_AVATAR_TAP_PROFILE, false);
    }

    /**
     * Decides if an avatar tap should be claimed.
     * Evaluates all edge cases: scrolling, selection mode, stories, saved messages, etc.
     */
    public static boolean wantsAvatarTap(DialogsActivity fragment, DialogCell cell) {
        return getTargetPeerId(fragment, cell) != 0L;
    }

    /**
     * Resolves the target peer ID to open profile for, or 0 to decline and keep upstream behavior.
     */
    public static long getTargetPeerId(DialogsActivity fragment, DialogCell cell) {
        if (fragment == null || cell == null) return 0L;

        Context context = fragment.getParentActivity() != null ? fragment.getParentActivity() : ApplicationLoader.applicationContext;
        if (!isEnabled(context)) {
            return 0L;
        }

        // 1. Fling / Scroll Guard: If user touches avatar mid-fling, treat as scroll-stop
        if (cell.getParent() instanceof RecyclerListView) {
            RecyclerListView list = (RecyclerListView) cell.getParent();
            if (list.getScrollState() != RecyclerListView.SCROLL_STATE_IDLE) {
                return 0L;
            }
        }

        // 2. Action / Selection Mode: Avatar is a selection checkmark
        ActionBar actionBar = fragment.getActionBar();
        if (actionBar != null && actionBar.isActionModeShowed()) {
            return 0L;
        }

        // 3. Search Result & Archive Row
        if (cell.getMessageId() != 0 || cell.getDialogFolderId() != 0) {
            return 0L;
        }

        long dialogId = cell.getDialogId();
        if (dialogId == 0L || DialogObject.isEncryptedDialog(dialogId)) {
            return 0L; // Secret chats have no separate profile
        }

        int account = fragment.getCurrentAccount();

        // 4. Saved Messages & System Replies
        if (DialogObject.isUserDialog(dialogId)) {
            if (dialogId == UserConfig.getInstance(account).getClientUserId()) {
                return 0L; // Saved Messages - preserve default chat opening
            }
            if (UserObject.isReplyUser(dialogId)) {
                return 0L; // Telegram Replies pseudo-chat
            }
            if (MessagesController.getInstance(account).getUser(dialogId) == null) {
                return 0L;
            }
            return dialogId;
        } else {
            // Groups & Channels
            long chatId = -dialogId;
            if (MessagesController.getInstance(account).getChat(chatId) == null) {
                return 0L;
            }
            return dialogId;
        }
    }

    /**
     * Opens the peer profile from avatar click, respecting story precedence.
     */
    public static boolean openProfile(DialogsActivity fragment, DialogCell cell, int storyState) {
        // If story ring is active / visible, story viewer takes precedence
        if (storyState != StoriesUtilities.STATE_EMPTY) {
            return false;
        }

        long did = getTargetPeerId(fragment, cell);
        if (did == 0L) {
            return false;
        }

        Bundle args = new Bundle();
        // Opening chat from profile cleans up backstack so Back returns directly to chat list
        args.putBoolean("removeFragmentOnChatOpen", true);

        if (DialogObject.isUserDialog(did)) {
            args.putLong("user_id", did);
        } else {
            args.putLong("chat_id", -did);
        }

        ProfileActivity profileActivity = new ProfileActivity(args);
        return fragment.presentFragment(profileActivity);
    }
}
