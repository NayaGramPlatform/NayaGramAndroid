package org.telegram.messenger;

/**
 * OwnMessageFilter - Prevents auto-replying to own messages, broadcast channels, and system messages
 */
public class OwnMessageFilter {
    private static final String TAG = "OwnMessageFilter";

    public static boolean canAutoReply(long fromUserId, int accountId) {
        try {
            long currentUserId = UserConfig.getInstance(accountId).getClientUserId();
            if (fromUserId == currentUserId && currentUserId != 0) {
                return false;
            }
            if (fromUserId == 777000L || fromUserId == 42470L) {
                return false;
            }
            return true;
        } catch (Exception e) {
            FileLog.e(TAG + ": Error checking own message filter", e);
            return false;
        }
    }

    public static boolean canAutoReply(int fromUserId, int accountId) {
        return canAutoReply((long) fromUserId, accountId);
    }
}
