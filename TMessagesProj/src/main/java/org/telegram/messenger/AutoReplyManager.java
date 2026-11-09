package org.telegram.messenger;

import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.nayagram.platform.NayaConfig;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Handles automatic replies for incoming messages and keyword rules. */
public class AutoReplyManager {
    private static AutoReplyManager instance;
    private static final String TAG = "AutoReplyManager";
    private static final String PREFS_NAME = "auto_reply_prefs";
    private static final String KEY_ENABLED = "auto_reply_enabled";
    private static final String KEY_RULES = "auto_reply_rules";
    private static final String KEY_RULES_VERSION = "auto_reply_rules_version";
    private static final int CURRENT_RULES_VERSION = 6;

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
        try {
            NayaConfig.getInstance().setSmartAutoReply(enabled);
        } catch (Throwable ignored) {}
        FileLog.d(TAG + ": Auto-reply " + (enabled ? "enabled" : "disabled"));
    }

    public synchronized boolean isAutoReplyEnabled() {
        try {
            return autoReplyEnabled || NayaConfig.getInstance().isSmartAutoReply();
        } catch (Throwable e) {
            return autoReplyEnabled;
        }
    }

    public synchronized void addRule(AutoReplyRule rule) {
        if (rule == null || rule.name == null || rule.name.trim().isEmpty()
                || rule.keyword == null || rule.keyword.trim().isEmpty() || rule.response == null) {
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
        if (messageText == null) {
            return null;
        }
        String lowerMessage = messageText.toLowerCase(Locale.ROOT).trim();
        for (AutoReplyRule rule : rules) {
            if (rule == null || !rule.enabled || rule.keyword == null) {
                continue;
            }
            if (matchesKeyword(lowerMessage, rule)) {
                return rule;
            }
        }
        return null;
    }

    private boolean matchesKeyword(String message, AutoReplyRule rule) {
        if (rule.keyword == null || rule.keyword.trim().isEmpty()) {
            return false;
        }
        if (!rule.regex) {
            return message.contains(rule.keyword.toLowerCase(Locale.ROOT).trim());
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
        return isAutoReplyEnabled();
    }

    private synchronized void saveRules() {
        try {
            preferences.edit()
                    .putString(KEY_RULES, gson.toJson(rules))
                    .putInt(KEY_RULES_VERSION, CURRENT_RULES_VERSION)
                    .apply();
        } catch (Exception e) {
            FileLog.e(TAG + ": failed to save rules", e);
        }
    }

    private synchronized void loadRules() {
        autoReplyEnabled = preferences.getBoolean(KEY_ENABLED, false);
        int version = preferences.getInt(KEY_RULES_VERSION, 0);
        String json = preferences.getString(KEY_RULES, null);
        if (json == null || json.isEmpty() || version < CURRENT_RULES_VERSION) {
            seedDefaultRules();
            return;
        }
        try {
            Type type = new TypeToken<List<AutoReplyRule>>() {}.getType();
            List<AutoReplyRule> storedRules = gson.fromJson(json, type);
            boolean hasLegacy = false;
            if (storedRules != null) {
                for (AutoReplyRule r : storedRules) {
                    if (r != null && r.keyword != null) {
                        String kw = r.keyword.toLowerCase(Locale.ROOT);
                        if (kw.contains("bkash") || kw.contains("nagad") || kw.equals("dam")) {
                            hasLegacy = true;
                            break;
                        }
                    }
                }
            }
            if (hasLegacy || storedRules == null || storedRules.isEmpty()) {
                seedDefaultRules();
            } else {
                rules.clear();
                rules.addAll(storedRules);
            }
        } catch (Exception e) {
            rules.clear();
            seedDefaultRules();
            FileLog.e(TAG + ": failed to load rules", e);
        }
    }

    public synchronized void seedDefaultRules() {
        rules.clear();
        // Default rules list kept clean and empty of forced templates. User creates custom rules as desired.
        saveRules();
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
            this.enabled = true;
        }
    }
}
