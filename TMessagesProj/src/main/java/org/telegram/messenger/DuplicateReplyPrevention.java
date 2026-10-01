package org.telegram.messenger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * DuplicateReplyPrevention - Prevents auto-replies to the same incoming message
 * Tracks recently replied message IDs to avoid duplicate responses
 */
public class DuplicateReplyPrevention {
    private static DuplicateReplyPrevention instance;
    private static final String TAG = "DuplicateReplyPrevention";
    private static final int MAX_TRACKED_MESSAGES = 500;
    private static final long CLEANUP_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes

    private final Set<Long> repliedMessageIds = Collections.synchronizedSet(new HashSet<>());
    private long lastCleanupTime = System.currentTimeMillis();

    private DuplicateReplyPrevention() {
    }

    /**
     * Get singleton instance
     */
    public static synchronized DuplicateReplyPrevention getInstance() {
        if (instance == null) {
            instance = new DuplicateReplyPrevention();
        }
        return instance;
    }

    /**
     * Check if message has already been replied to
     * @param messageId The incoming message ID
     * @return true if already replied, false otherwise
     */
    public synchronized boolean hasAlreadyReplied(long messageId) {
        performCleanupIfNeeded();
        return repliedMessageIds.contains(messageId);
    }

    /**
     * Mark message as replied
     * @param messageId The incoming message ID
     */
    public synchronized void markAsReplied(long messageId) {
        performCleanupIfNeeded();
        repliedMessageIds.add(messageId);
        if (repliedMessageIds.size() > MAX_TRACKED_MESSAGES) {
            // Remove oldest entries if we exceed max capacity
            int toRemove = repliedMessageIds.size() - MAX_TRACKED_MESSAGES;
            repliedMessageIds.stream().limit(toRemove).forEach(repliedMessageIds::remove);
        }
        FileLog.d(TAG + ": Marked message " + messageId + " as replied");
    }

    /**
     * Perform cleanup of old entries periodically
     */
    private void performCleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CLEANUP_INTERVAL_MS) {
            repliedMessageIds.clear();
            lastCleanupTime = now;
            FileLog.d(TAG + ": Cleaned up replied message tracking");
        }
    }

    /**
     * Clear all tracked messages
     */
    public synchronized void clear() {
        repliedMessageIds.clear();
        lastCleanupTime = System.currentTimeMillis();
    }
}
