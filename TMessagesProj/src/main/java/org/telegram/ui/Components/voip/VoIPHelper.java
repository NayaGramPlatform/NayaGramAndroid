package org.telegram.ui.Components.voip;

import android.app.Activity;
import org.telegram.messenger.AccountInstance;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;

/**
 * TEMPORARY STUB - VoIPHelper was accidentally truncated in previous commits.
 * 
 * TO RESTORE FULL FILE:
 * 1. Go to https://github.com/NayaGramPlatform/NayaGramAndroid/commits/master/TMessagesProj/src/main/java/org/telegram/ui/Components/voip/VoIPHelper.java
 * 2. Find a good commit before the broken ones (e.g. the isBetaApp fix commit)
 * 3. View the file and copy the full content back.
 *
 * Or run: git checkout <good-commit-sha> -- TMessagesProj/src/main/java/org/telegram/ui/Components/voip/VoIPHelper.java
 */
public class VoIPHelper {

    public static long lastCallTime = 0;

    public static void startCall(TLRPC.User user, boolean videoCall, boolean canVideoCall, final Activity activity, TLRPC.UserFull userFull, AccountInstance accountInstance) {
        android.util.Log.e("NayaGram", "VoIPHelper is currently a stub. Please restore the full original file from git history for calls to work.");
    }

    public static void startCall(TLRPC.Chat chat, TLRPC.InputPeer peer, String hash, boolean createCall, Activity activity, BaseFragment fragment, AccountInstance accountInstance) {
        startCall(chat, peer, hash, createCall, null, activity, fragment, accountInstance);
    }

    public static void startCall(TLRPC.Chat chat, TLRPC.InputPeer peer, String hash, boolean createCall, Boolean checkJoiner, Activity activity, BaseFragment fragment, AccountInstance accountInstance) {
        android.util.Log.e("NayaGram", "VoIPHelper is currently a stub. Please restore the full original file from git history.");
    }

    public static void joinConference(Activity activity, int account, TLRPC.InputGroupCall inputGroupCall, boolean videoCall, TLRPC.GroupCall preloadedCall) {
        // stub
    }

    public static void joinConference(Activity activity, int account, TLRPC.InputGroupCall inputGroupCall, boolean videoCall, TLRPC.GroupCall preloadedCall, java.util.HashSet<Long> inviteUsers) {
        // stub
    }
}
