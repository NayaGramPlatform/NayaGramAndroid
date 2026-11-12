package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.nayagram.platform.NayaConfig;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * AutoReplyManager - Clean Novagram & Graph Messenger style auto-reply engine.
 * - Simple main auto-answer text (like Novagram AutoAnswer.kt).
 * - Optional custom keyword rules (like Graph Messenger).
 * - Zero forced preset rules, zero cooldown traps, zero broken markdown tags.
 */
public class AutoReplyManager {
    private static AutoReplyManager instance;
    private static final String TAG = "AutoReplyManager";
    private static final String PREFS_NAME = "auto_reply_prefs";
    private static final String KEY_ENABLED = "auto_reply_enabled";
    private static final String KEY_TEXT = "auto_answer_text";
    private static final String KEY_PRIVATE_ONLY = "auto_reply_private_only";
    private static final String KEY_RULES = "auto_reply_rules";

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();
    private final List<AutoReplyRule> rules = new ArrayList<>();
    private boolean autoReplyEnabled;
    private String autoAnswerText = "";
    private boolean privateOnly = true;

    private AutoReplyManager() {
        preferences = ApplicationLoader.applicationContext.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);
        load();
    }

    public static synchronized AutoReplyManager getInstance() {
        if (instance == null) {
            instance = new AutoReplyManager();
        }
        return instance;
    }

    public synchronized void setAutoReplyEnabled(boolean enabled) {
        autoReplyEnabled = enabled;
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
        try {
            NayaConfig.getInstance().setSmartAutoReply(enabled);
        } catch (Throwable ignored) {}
    }

    public synchronized boolean isAutoReplyEnabled() {
        try {
            return autoReplyEnabled || NayaConfig.getInstance().isSmartAutoReply();
        } catch (Throwable e) {
            return autoReplyEnabled;
        }
    }

    public synchronized String getAutoAnswerText() {
        return autoAnswerText != null ? autoAnswerText : "";
    }

    public synchronized void setAutoAnswerText(String text) {
        autoAnswerText = text != null ? text : "";
        preferences.edit().putString(KEY_TEXT, autoAnswerText).apply();
    }

    public synchronized boolean isPrivateOnly() {
        return privateOnly;
    }

    public synchronized void setPrivateOnly(boolean val) {
        privateOnly = val;
        preferences.edit().putBoolean(KEY_PRIVATE_ONLY, val).apply();
    }

    public synchronized void addRule(AutoReplyRule rule) {
        if (rule == null || rule.keyword == null || rule.keyword.trim().isEmpty() || rule.response == null) {
            return;
        }
        rules.add(rule);
        saveRules();
    }

    public synchronized void updateRule(int index, AutoReplyRule rule) {
        if (index >= 0 && index < rules.size() && rule != null) {
            rules.set(index, rule);
            saveRules();
        }
    }

    public synchronized void setRuleEnabled(int index, boolean enabled) {
        if (index >= 0 && index < rules.size()) {
            rules.get(index).enabled = enabled;
            saveRules();
        }
    }

    public synchronized void deleteRule(int index) {
        if (index >= 0 && index < rules.size()) {
            rules.remove(index);
            saveRules();
        }
    }

    public synchronized void clearAllRules() {
        rules.clear();
        saveRules();
    }

    public synchronized List<AutoReplyRule> getRules() {
        return new ArrayList<>(rules);
    }

    /**
     * Resolves response for an incoming message:
     * 1. Checks custom keyword rules first (Graph Messenger style).
     * 2. If no keyword matches, falls back to the main auto-answer text (Novagram style).
     */
    public synchronized String getAutoReplyResponse(String messageText) {
        if (messageText != null && !rules.isEmpty()) {
            String lower = messageText.toLowerCase(Locale.ROOT).trim();
            for (AutoReplyRule rule : rules) {
                if (rule != null && rule.enabled && rule.keyword != null && !rule.keyword.trim().isEmpty()) {
                    if (matchesKeyword(lower, rule)) {
                        return rule.response;
                    }
                }
            }
        }
        // Fallback to main Novagram auto-reply message
        if (autoAnswerText != null && !autoAnswerText.trim().isEmpty()) {
            return autoAnswerText;
        }
        return null;
    }

    private boolean matchesKeyword(String message, AutoReplyRule rule) {
        if (!rule.regex) {
            return message.contains(rule.keyword.toLowerCase(Locale.ROOT).trim());
        }
        try {
            return Pattern.compile(rule.keyword, Pattern.CASE_INSENSITIVE).matcher(message).find();
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized boolean shouldSendAutoReply(long dialogId, int accountId) {
        if (!isAutoReplyEnabled()) return false;
        if (privateOnly && dialogId < 0) {
            // Group or channel
            return false;
        }
        return true;
    }

    private synchronized void saveRules() {
        try {
            preferences.edit().putString(KEY_RULES, gson.toJson(rules)).apply();
        } catch (Exception ignored) {}
    }

    private synchronized void load() {
        autoReplyEnabled = preferences.getBoolean(KEY_ENABLED, false);
        autoAnswerText = preferences.getString(KEY_TEXT, "");
        privateOnly = preferences.getBoolean(KEY_PRIVATE_ONLY, true);

        rules.clear();
        String json = preferences.getString(KEY_RULES, null);
        if (json != null && !json.isEmpty()) {
            try {
                Type type = new TypeToken<List<AutoReplyRule>>() {}.getType();
                List<AutoReplyRule> stored = gson.fromJson(json, type);
                if (stored != null) {
                    // Filter out any legacy preset rules (e.g. bkash, nagad, 16 rules)
                    for (AutoReplyRule r : stored) {
                        if (r != null && r.keyword != null && !r.keyword.trim().isEmpty()) {
                            String kw = r.keyword.toLowerCase(Locale.ROOT);
                            if (!kw.contains("bkash") && !kw.contains("nagad") && !kw.contains("salam")) {
                                rules.add(r);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    public static class AutoReplyRule {
        public String name;
        public String keyword;
        public String response;
        public boolean enabled = true;
        public boolean regex;

        public AutoReplyRule(String name, String keyword, String response) {
            this(name, keyword, response, false);
        }

        public AutoReplyRule(String name, String keyword, String response, boolean regex) {
            this.name = name;
            this.keyword = keyword;
            this.response = response;
            this.regex = regex;
            this.enabled = true;
        }
    }
}
