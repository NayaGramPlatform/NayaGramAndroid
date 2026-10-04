package org.nayagram.platform;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

/**
 * AutoPinChannelManager - Automatically joins and pins the official @NayaGramPro channel
 * at the top of the chat list for every user on login or app start (identical to Novagram's system).
 */
public final class AutoPinChannelManager {

    private static final String CHANNEL_USERNAME = "NayaGramPro";
    private static final String PREFS = "nayagram_autopin_prefs";
    private static final String KEY_PINNED = "channel_pinned_acc_";

    public static void checkAndPinOfficialChannel(int account) {
        if (!UserConfig.getInstance(account).isClientActivated()) {
            return;
        }

        SharedPreferences sp = ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (sp.getBoolean(KEY_PINNED + account, false)) {
            return;
        }

        TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
        req.username = CHANNEL_USERNAME;

        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
            if (error == null && response instanceof TLRPC.TL_contacts_resolvedPeer) {
                TLRPC.TL_contacts_resolvedPeer peer = (TLRPC.TL_contacts_resolvedPeer) response;
                TLRPC.Chat targetChat = null;
                Long channelId = null;

                if (peer.peer instanceof TLRPC.TL_peerChannel) {
                    channelId = peer.peer.channel_id;
                }

                if (channelId != null) {
                    for (TLRPC.Chat c : peer.chats) {
                        if (c.id == channelId) {
                            targetChat = c;
                            break;
                        }
                    }
                }
                if (targetChat == null && !peer.chats.isEmpty()) {
                    targetChat = peer.chats.get(0);
                }

                if (targetChat != null && ChatObject.isChannel(targetChat)) {
                    final TLRPC.Chat fChat = targetChat;
                    TLRPC.TL_inputChannel input = new TLRPC.TL_inputChannel();
                    input.channel_id = fChat.id;
                    input.access_hash = fChat.access_hash;

                    TLRPC.TL_channels_joinChannel joinReq = new TLRPC.TL_channels_joinChannel();
                    joinReq.channel = input;

                    ConnectionsManager.getInstance(account).sendRequest(joinReq, (joinResp, joinErr) -> {
                        long dialogId = -fChat.id;
                        MessagesController.getInstance(account).pinDialog(dialogId, true, null, -1);
                        sp.edit().putBoolean(KEY_PINNED + account, true).apply();
                    });
                }
            }
        });
    }
}
