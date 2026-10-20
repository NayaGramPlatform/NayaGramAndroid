package org.nayagram.platform;

/**
 * Legacy compatibility shim.
 *
 * NayaGram must never silently join or pin a third-party Telegram channel.
 * Channel membership and pinning are user-directed actions. These methods are
 * retained for source compatibility with older callers, but intentionally do
 * nothing.
 */
@Deprecated
public final class AutoPinChannelManager {
    public static final String OFFICIAL_CHANNEL_USERNAME = "NayaGramPro";

    private AutoPinChannelManager() {
    }

    /** @deprecated Use an explicit, user-confirmed channel open/join flow. */
    @Deprecated
    public static void checkAndPinOfficialChannel(int account) {
        // Intentionally disabled: never auto-join or auto-pin a channel.
    }

    /** @deprecated Use an explicit, user-confirmed channel open/join flow. */
    @Deprecated
    public static void checkAndPinChannel() {
        // Intentionally disabled: never auto-join or auto-pin a channel.
    }
}
