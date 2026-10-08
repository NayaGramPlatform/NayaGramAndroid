package org.nayagram.platform.bot;

import android.content.Context;
import android.widget.Toast;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

/**
 * Guard utility for Bot accounts to prevent NullPointerExceptions on missing phone numbers
 * and provide user-friendly limitation notices for unsupported features (Calls, Contacts, Stories).
 */
public class NGBotAccountGuard {

    public static boolean isCurrentAccountBot(int currentAccount) {
        TLRPC.User currentUser = UserConfig.getInstance(currentAccount).getCurrentUser();
        return currentUser != null && currentUser.bot;
    }

    public static String getSafePhoneNumber(TLRPC.User user) {
        if (user == null || user.phone == null || user.phone.trim().isEmpty()) {
            return user != null && user.bot ? "Bot Account (No Phone)" : "";
        }
        return user.phone;
    }

    public static boolean checkCallSupported(Context context, int currentAccount) {
        if (isCurrentAccountBot(currentAccount)) {
            if (context != null) {
                Toast.makeText(context, "Voice and Video Calls are not supported on Bot accounts.", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }

    public static boolean checkStoriesSupported(Context context, int currentAccount) {
        if (isCurrentAccountBot(currentAccount)) {
            if (context != null) {
                Toast.makeText(context, "Stories are not available for Bot accounts.", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }

    public static boolean checkContactsSyncSupported(Context context, int currentAccount) {
        if (isCurrentAccountBot(currentAccount)) {
            if (context != null) {
                Toast.makeText(context, "Contact synchronization is not available for Bot accounts.", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }
}
