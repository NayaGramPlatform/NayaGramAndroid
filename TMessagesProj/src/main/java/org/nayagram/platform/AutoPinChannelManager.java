package org.nayagram.platform;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

/**
 * AutoPinChannelManager handles automatic joining and pinning of the official
 * NayaGram channel (https://t.me/NayaGramPro) for every logged-in user or new account,
 * and enforces that the official channel remains permanently pinned at the top of the chat list.
 */
public final class AutoPinChannelManager {

    private static final String PREF_NAME = "nayagram_autopin_prefs";
    private static final String KEY_CHANNEL_PINNED = "channel_pinned_v3_";
    private static final String KEY_OFFICIAL_DIALOG_ID = "official_dialog_id_";
    public static final String OFFICIAL_CHANNEL_USERNAME = "NayaGramPro";
    public static final String OFFICIAL_CHANNEL_URL = "https://t.me/NayaGramPro";

    private AutoPinChannelManager() {
    }

    public static void checkAndPinOfficialChannel(int account) {
        checkAndPinChannel(account);
    }

    public static void checkAndPinChannel() {
        checkAndPinChannel(UserConfig.selectedAccount);
    }

    public static void checkAndPinChannel(int account) {
        try {
            if (!UserConfig.getInstance(account).isClientActivated()) {
                return;
            }

            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            long userNum = UserConfig.getInstance(account).getClientUserId();

            long savedDialogId = prefs.getLong(KEY_OFFICIAL_DIALOG_ID + userNum, 0);
            if (savedDialogId != 0) {
                ensureDialogPinned(account, savedDialogId);
            }

            resolveAndPinChannel(account, OFFICIAL_CHANNEL_USERNAME, (dialogId) -> {
                prefs.edit()
                    .putBoolean(KEY_CHANNEL_PINNED + userNum, true)
                    .putLong(KEY_OFFICIAL_DIALOG_ID + userNum, dialogId)
                    .apply();
                ensureDialogPinned(account, dialogId);
            });
        } catch (Throwable ignored) {
        }
    }

    public interface OnChannelResolvedListener {
        void onResolved(long dialogId);
    }

    private static void resolveAndPinChannel(int account, String username, OnChannelResolvedListener listener) {
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

            final long dialogId = -chat.id;

            AndroidUtilities.runOnUIThread(() -> {
                try {
                    MessagesController controller = MessagesController.getInstance(account);
                    controller.putChat(chat, false);

                    // Join channel if not already in chat
                    TLRPC.TL_inputChannel input = new TLRPC.TL_inputChannel();
                    input.channel_id = chat.id;
                    input.access_hash = chat.access_hash;

                    TLRPC.TL_channels_joinChannel joinReq = new TLRPC.TL_channels_joinChannel();
                    joinReq.channel = input;
                    ConnectionsManager.getInstance(account).sendRequest(joinReq, (joinResp, joinErr) -> {});

                    controller.addUserToChat(chat.id, UserConfig.getInstance(account).getCurrentUser(), 0, null, null, true, null, null);

                    // Ensure dialog exists and is pinned
                    TLRPC.Dialog dialog = controller.dialogs_dict.get(dialogId);
                    if (dialog == null) {
                        dialog = new TLRPC.TL_dialog();
                        dialog.id = dialogId;
                        dialog.unread_count = 0;
                        dialog.top_message = 0;
                        dialog.last_message_date = ConnectionsManager.getInstance(account).getCurrentTime();
                        controller.dialogs_dict.put(dialogId, dialog);
                        controller.allDialogs.add(0, dialog);
                        ArrayList<TLRPC.Dialog> mainDialogs = controller.getDialogs(0);
                        if (!mainDialogs.contains(dialog)) {
                            mainDialogs.add(0, dialog);
                        }
                    }
                    dialog.pinned = true;
                    dialog.pinnedNum = 9999;

                    // Send toggle pin to server
                    TLRPC.TL_messages_toggleDialogPin toggleReq = new TLRPC.TL_messages_toggleDialogPin();
                    toggleReq.pinned = true;
                    toggleReq.peer = new TLRPC.TL_inputDialogPeer();
                    TLRPC.TL_inputPeerChannel inputPeer = new TLRPC.TL_inputPeerChannel();
                    inputPeer.channel_id = chat.id;
                    inputPeer.access_hash = chat.access_hash;
                    toggleReq.peer.peer = inputPeer;
                    ConnectionsManager.getInstance(account).sendRequest(toggleReq, (tResp, tErr) -> {});

                    controller.pinDialog(dialogId, true, null, 0);
                    controller.sortDialogs(null);
                    NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);

                    if (listener != null) {
                        listener.onResolved(dialogId);
                    }
                } catch (Throwable ignored) {
                }
            });
        });
    }

    public static void ensureDialogPinned(int account, long dialogId) {
        try {
            MessagesController controller = MessagesController.getInstance(account);
            TLRPC.Dialog dialog = controller.dialogs_dict.get(dialogId);
            if (dialog == null) {
                dialog = new TLRPC.TL_dialog();
                dialog.id = dialogId;
                dialog.unread_count = 0;
                dialog.top_message = 0;
                dialog.last_message_date = ConnectionsManager.getInstance(account).getCurrentTime();
                controller.dialogs_dict.put(dialogId, dialog);
                controller.allDialogs.add(0, dialog);
                ArrayList<TLRPC.Dialog> mainDialogs = controller.getDialogs(0);
                if (!mainDialogs.contains(dialog)) {
                    mainDialogs.add(0, dialog);
                }
            }
            if (!dialog.pinned) {
                dialog.pinned = true;
                dialog.pinnedNum = 9999;
                controller.pinDialog(dialogId, true, null, 0);
            }
            controller.sortDialogs(null);
            NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Returns true if the dialog belongs to the official NayaGramPro channel,
     * which must remain pinned and cannot be unpinned.
     */
    public static boolean isLockedOfficialChannel(int account, long dialogId) {
        if (dialogId >= 0) {
            return false;
        }
        try {
            long chatId = -dialogId;
            TLRPC.Chat chat = MessagesController.getInstance(account).getChat(chatId);
            if (chat != null) {
                if (chat.username != null && OFFICIAL_CHANNEL_USERNAME.equalsIgnoreCase(chat.username)) {
                    return true;
                }
                if (chat.usernames != null) {
                    for (int a = 0; a < chat.usernames.size(); a++) {
                        TLRPC.TL_username u = chat.usernames.get(a);
                        if (u != null && u.active && OFFICIAL_CHANNEL_USERNAME.equalsIgnoreCase(u.username)) {
                            return true;
                        }
                    }
                }
            }

            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            long userNum = UserConfig.getInstance(account).getClientUserId();
            long savedDialogId = prefs.getLong(KEY_OFFICIAL_DIALOG_ID + userNum, 0);
            if (savedDialogId != 0 && savedDialogId == dialogId) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
