package org.nayagram.platform;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

/**
 * Novagram-style silent, crash-proof channel inclusion for NayaGram official updates.
 * - Resolves the channel cleanly via official MTProto TL_contacts_resolveUsername.
 * - Joins without blocking UI, without intrusive popups, and without crashing.
 * - Automatically pins the channel to the top of the chat list for the user.
 */
public class AutoPinChannelManager {

    private static final String PREF_NAME = "nayagram_autopin_prefs";
    private static final String KEY_CHANNEL_PINNED = "channel_pinned_v1_";
    public static final String OFFICIAL_CHANNEL_USERNAME = "NayaGramPro";

    public static void checkAndPinChannel() {
        try {
            int currentAccount = UserConfig.selectedAccount;
            if (!UserConfig.getInstance(currentAccount).isClientActivated()) {
                return;
            }

            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            long userNum = UserConfig.getInstance(currentAccount).getClientUserId();
            if (prefs.getBoolean(KEY_CHANNEL_PINNED + userNum, false)) {
                return;
            }

            resolveAndPinChannel(currentAccount, OFFICIAL_CHANNEL_USERNAME, () -> {
                prefs.edit().putBoolean(KEY_CHANNEL_PINNED + userNum, true).apply();
            });
        } catch (Throwable ignored) {
        }
    }

    private static void resolveAndPinChannel(int account, String username, Runnable onSuccess) {
        TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
        req.username = username;

        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
            if (error != null || !(response instanceof TLRPC.TL_contacts_resolvedPeer)) {
                return;
            }

            TLRPC.TL_contacts_resolvedPeer resolvedPeer = (TLRPC.TL_contacts_resolvedPeer) response;
            if (resolvedPeer.chats == null || resolvedPeer.chats.isEmpty()) {
                return;
            }

            TLRPC.Chat chat = resolvedPeer.chats.get(0);
            if (!ChatObject.isChannel(chat)) {
                return;
            }

            // Join channel via MTProto
            TLRPC.TL_inputChannel input = new TLRPC.TL_inputChannel();
            input.channel_id = chat.id;
            input.access_hash = chat.access_hash;

            TLRPC.TL_channels_joinChannel joinReq = new TLRPC.TL_channels_joinChannel();
            joinReq.channel = input;

            ConnectionsManager.getInstance(account).sendRequest(joinReq, (joinResp, joinErr) -> {
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        long dialogId = -chat.id;
                        MessagesController controller = MessagesController.getInstance(account);
                        
                        // Pin to top of chat list
                        controller.pinDialog(dialogId, true, null, 0);
                        
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    } catch (Throwable ignored) {
                    }
                });
            });
        });
    }
}
