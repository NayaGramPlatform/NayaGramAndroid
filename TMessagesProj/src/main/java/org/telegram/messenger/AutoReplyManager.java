package org.telegram.messenger;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Handles automatic replies for incoming messages. */
public class AutoReplyManager {
    private static AutoReplyManager instance;
    private static final String TAG = "AutoReplyManager";
    private static final String PREFS_NAME = "auto_reply_prefs";
    private static final String KEY_ENABLED = "auto_reply_enabled";
    private static final String KEY_RULES = "auto_reply_rules";

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();
    private final List<AutoReplyRule> rules = new ArrayList<>();
    private boolean autoReplyEnabled;

    private AutoReplyManager() {
        preferences = ApplicationLoader.applicationContext.getSharedPreferences(
                PREFS_NAME, android.content.Context.MODE_PRIVATE);
        loadRules();
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
        FileLog.d(TAG + ": Auto-reply " + (enabled ? "enabled" : "disabled"));
    }

    public synchronized boolean isAutoReplyEnabled() {
        return autoReplyEnabled;
    }

    public synchronized void addRule(AutoReplyRule rule) {
        if (rule == null || rule.name == null || rule.name.trim().isEmpty()
                || rule.keyword == null || rule.keyword.isEmpty() || rule.response == null) {
            return;
        }
        rules.add(rule);
        saveRules();
    }

    public synchronized void removeRule(String ruleName) {
        if (ruleName == null) {
            return;
        }
        rules.removeIf(rule -> ruleName.equals(rule.name));
        saveRules();
    }

    public synchronized List<AutoReplyRule> getRules() {
        return new ArrayList<>(rules);
    }

    public synchronized AutoReplyRule findMatchingRule(String messageText) {
        if (!autoReplyEnabled || messageText == null) {
            return null;
        }
        for (AutoReplyRule rule : rules) {
            if (rule == null || !rule.enabled || rule.keyword == null) {
                continue;
            }
            if (matchesKeyword(messageText, rule)) {
                return rule;
            }
        }
        return null;
    }

    private boolean matchesKeyword(String message, AutoReplyRule rule) {
        if (rule.keyword.isEmpty()) {
            return false;
        }
        if (!rule.regex) {
            return message.toLowerCase().contains(rule.keyword.toLowerCase());
        }
        try {
            return Pattern.compile(rule.keyword, Pattern.CASE_INSENSITIVE).matcher(message).find();
        } catch (Exception e) {
            FileLog.e(TAG + ": invalid regex for rule " + rule.name, e);
            return false;
        }
    }

    public synchronized String getAutoReplyResponse(String messageText) {
        AutoReplyRule rule = findMatchingRule(messageText);
        return rule == null ? null : rule.response;
    }

    public synchronized boolean shouldSendAutoReply(long dialogId, int accountId) {
        return autoReplyEnabled;
    }

    private synchronized void saveRules() {
        try {
            preferences.edit().putString(KEY_RULES, gson.toJson(rules)).apply();
        } catch (Exception e) {
            FileLog.e(TAG + ": failed to save rules", e);
        }
    }

    private synchronized void loadRules() {
        autoReplyEnabled = preferences.getBoolean(KEY_ENABLED, false);
        String json = preferences.getString(KEY_RULES, null);
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            Type type = new TypeToken<List<AutoReplyRule>>() {}.getType();
            List<AutoReplyRule> storedRules = gson.fromJson(json, type);
            if (storedRules != null) {
                rules.clear();
                rules.addAll(storedRules);
            }
        } catch (Exception e) {
            rules.clear();
            FileLog.e(TAG + ": failed to load rules", e);
        }
    }

    public synchronized void clearAllRules() {
        rules.clear();
        saveRules();
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
        }
    }
}
