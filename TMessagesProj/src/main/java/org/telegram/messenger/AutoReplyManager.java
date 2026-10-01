package org.telegram.messenger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * AutoReplyManager - Handles automatic replies for messages
 * Features:
 * - Create auto-reply templates
 * - Keyword-based matching
 * - Enable/disable auto-reply
 * - Per-chat auto-reply settings
 */
public class AutoReplyManager {
    
    private static AutoReplyManager instance;
    private SharedPreferences preferences;
    private List<AutoReplyRule> rules;
    private boolean autoReplyEnabled;
    private static final String TAG = "AutoReplyManager";
    
    private AutoReplyManager() {
        this.preferences = ApplicationLoader.applicationContext.getSharedPreferences(
                "auto_reply_prefs",
                android.content.Context.MODE_PRIVATE
        );
        this.rules = new ArrayList<>();
        loadRules();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized AutoReplyManager getInstance() {
        if (instance == null) {
            instance = new AutoReplyManager();
        }
        return instance;
    }
    
    /**
     * Enable/disable auto-reply
     */
    public void setAutoReplyEnabled(boolean enabled) {
        autoReplyEnabled = enabled;
        preferences.edit().putBoolean("auto_reply_enabled", enabled).apply();
        FileLog.d(TAG + ": Auto-reply " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if auto-reply is enabled
     */
    public boolean isAutoReplyEnabled() {
        return autoReplyEnabled;
    }
    
    /**
     * Add auto-reply rule
     */
    public void addRule(AutoReplyRule rule) {
        rules.add(rule);
        saveRules();
        FileLog.d(TAG + ": Added auto-reply rule: " + rule.name);
    }
    
    /**
     * Remove auto-reply rule
     */
    public void removeRule(String ruleName) {
        rules.removeIf(r -> r.name.equals(ruleName));
        saveRules();
        FileLog.d(TAG + ": Removed auto-reply rule: " + ruleName);
    }
    
    /**
     * Get all rules
     */
    public List<AutoReplyRule> getRules() {
        return new ArrayList<>(rules);
    }
    
    /**
     * Find matching rule for message
     */
    public AutoReplyRule findMatchingRule(String messageText) {
        if (!autoReplyEnabled || messageText == null) {
            return null;
        }
        
        for (AutoReplyRule rule : rules) {
            if (!rule.enabled) {
                continue;
            }
            
            if (matchesKeyword(messageText, rule.keyword)) {
                return rule;
            }
        }
        
        return null;
    }
    
    /**
     * Check if message matches keyword
     */
    private boolean matchesKeyword(String message, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return false;
        }
        
        try {
            // Try regex matching
            Pattern pattern = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
            return pattern.matcher(message).find();
        } catch (Exception e) {
            // Fallback to simple substring matching
            return message.toLowerCase().contains(keyword.toLowerCase());
        }
    }
    
    /**
     * Get auto-reply response for message
     */
    public String getAutoReplyResponse(String messageText) {
        AutoReplyRule rule = findMatchingRule(messageText);
        if (rule != null) {
            return rule.response;
        }
        return null;
    }
    
    /**
     * Check if auto-reply should be sent for this dialog
     */
    public boolean shouldSendAutoReply(long dialogId, int accountId) {
        if (!autoReplyEnabled) {
            return false;
        }
        
        // TODO: Add per-chat settings to check if auto-reply is disabled for specific chat
        return true;
    }
    
    /**
     * Save rules to storage
     */
    private void saveRules() {
        // TODO: Implement serialization to SharedPreferences or database
    }
    
    /**
     * Load rules from storage
     */
    private void loadRules() {
        // TODO: Implement deserialization from SharedPreferences or database
        autoReplyEnabled = preferences.getBoolean("auto_reply_enabled", false);
    }
    
    /**
     * Clear all rules
     */
    public void clearAllRules() {
        rules.clear();
        saveRules();
        FileLog.d(TAG + ": Cleared all auto-reply rules");
    }
    
    /**
     * Auto-reply rule data class
     */
    public static class AutoReplyRule {
        public String name;
        public String keyword;
        public String response;
        public boolean enabled;
        public boolean regex;
        
        public AutoReplyRule(String name, String keyword, String response) {
            this.name = name;
            this.keyword = keyword;
            this.response = response;
            this.enabled = true;
            this.regex = false;
        }
        
        public AutoReplyRule(String name, String keyword, String response, boolean regex) {
            this.name = name;
            this.keyword = keyword;
            this.response = response;
            this.enabled = true;
            this.regex = regex;
        }
        
        @Override
        public String toString() {
            return "AutoReplyRule{" +
                    "name='" + name + '\'' +
                    ", keyword='" + keyword + '\'' +
                    ", response='" + response + '\'' +
                    ", enabled=" + enabled +
                    ", regex=" + regex +
                    '}';
        }
    }
}
