package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Spannable;
import android.text.Spanned;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.nayagram.platform.NayaConfig;
import org.telegram.messenger.MediaDataController;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.TextStyleSpan;
import org.telegram.ui.Components.URLSpanReplacement;
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
    private static final String KEY_ENTITIES = "auto_answer_entities";
    private static final String KEY_PRIVATE_ONLY = "auto_reply_private_only";
    private static final String KEY_RULES = "auto_reply_rules";

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();
    private final List<AutoReplyRule> rules = new ArrayList<>();
    private boolean autoReplyEnabled;
    private String autoAnswerText = "";
    private String autoAnswerEntitiesJson = null;
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
        setAutoAnswerText(text, null);
    }

    public synchronized void setAutoAnswerText(String text, String entitiesJson) {
        autoAnswerText = text != null ? text : "";
        autoAnswerEntitiesJson = entitiesJson;
        preferences.edit()
                .putString(KEY_TEXT, autoAnswerText)
                .putString(KEY_ENTITIES, autoAnswerEntitiesJson)
                .apply();
    }

    public synchronized String getAutoAnswerEntitiesJson() {
        return autoAnswerEntitiesJson;
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
    public synchronized AutoReplyRule findMatchingRule(String messageText) {
        if (messageText == null || rules.isEmpty()) return null;
        String lower = messageText.toLowerCase(Locale.ROOT).trim();
        for (AutoReplyRule rule : rules) {
            if (rule != null && rule.enabled && rule.keyword != null && !rule.keyword.trim().isEmpty()) {
                if (matchesKeyword(lower, rule)) {
                    return rule;
                }
            }
        }
        return null;
    }

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
        autoAnswerEntitiesJson = preferences.getString(KEY_ENTITIES, null);
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

    public static class EntityData {
        public int type; // 1=Bold, 2=Italic, 3=Mono, 4=Spoiler, 5=Strike, 6=Underline, 7=Quote, 8=Url
        public int offset;
        public int length;
        public String url;

        public EntityData() {}
        public EntityData(int type, int offset, int length, String url) {
            this.type = type;
            this.offset = offset;
            this.length = length;
            this.url = url;
        }
    }

    public static String serializeSpansToJson(CharSequence text) {
        if (!(text instanceof Spannable)) return null;
        Spannable s = (Spannable) text;
        List<EntityData> list = new ArrayList<>();
        TextStyleSpan[] spans = s.getSpans(0, s.length(), TextStyleSpan.class);
        if (spans != null) {
            for (TextStyleSpan span : spans) {
                int start = s.getSpanStart(span);
                int end = s.getSpanEnd(span);
                if (start < 0 || end <= start) continue;
                TextStyleSpan.TextStyleRun run = span.getTextStyleRun();
                if (run == null) continue;
                int f = run.flags;
                if ((f & TextStyleSpan.FLAG_STYLE_BOLD) != 0) list.add(new EntityData(1, start, end - start, null));
                if ((f & TextStyleSpan.FLAG_STYLE_ITALIC) != 0) list.add(new EntityData(2, start, end - start, null));
                if ((f & TextStyleSpan.FLAG_STYLE_MONO) != 0) list.add(new EntityData(3, start, end - start, null));
                if ((f & TextStyleSpan.FLAG_STYLE_SPOILER) != 0) list.add(new EntityData(4, start, end - start, null));
                if ((f & TextStyleSpan.FLAG_STYLE_STRIKE) != 0) list.add(new EntityData(5, start, end - start, null));
                if ((f & TextStyleSpan.FLAG_STYLE_UNDERLINE) != 0) list.add(new EntityData(6, start, end - start, null));
            }
        }
        URLSpanReplacement[] urls = s.getSpans(0, s.length(), URLSpanReplacement.class);
        if (urls != null) {
            for (URLSpanReplacement u : urls) {
                int start = s.getSpanStart(u);
                int end = s.getSpanEnd(u);
                if (start >= 0 && end > start) list.add(new EntityData(8, start, end - start, u.getURL()));
            }
        }
        if (list.isEmpty()) return null;
        try {
            return new Gson().toJson(list);
        } catch (Exception e) {
            return null;
        }
    }

    public static ArrayList<TLRPC.MessageEntity> parseEntitiesFromJson(String json) {
        ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>();
        if (json == null || json.isEmpty()) return entities;
        try {
            Type type = new TypeToken<List<EntityData>>(){}.getType();
            List<EntityData> list = new Gson().fromJson(json, type);
            if (list == null) return entities;
            for (EntityData e : list) {
                if (e == null || e.length <= 0) continue;
                TLRPC.MessageEntity entity = null;
                if (e.type == 1) entity = new TLRPC.TL_messageEntityBold();
                else if (e.type == 2) entity = new TLRPC.TL_messageEntityItalic();
                else if (e.type == 3) entity = new TLRPC.TL_messageEntityCode();
                else if (e.type == 4) entity = new TLRPC.TL_messageEntitySpoiler();
                else if (e.type == 5) entity = new TLRPC.TL_messageEntityStrike();
                else if (e.type == 6) entity = new TLRPC.TL_messageEntityUnderline();
                else if (e.type == 7) entity = new TLRPC.TL_messageEntityBlockquote();
                else if (e.type == 8 && e.url != null) {
                    TLRPC.TL_messageEntityTextUrl urlEntity = new TLRPC.TL_messageEntityTextUrl();
                    urlEntity.url = e.url;
                    entity = urlEntity;
                }
                if (entity != null) {
                    entity.offset = e.offset;
                    entity.length = e.length;
                    entities.add(entity);
                }
            }
        } catch (Exception ignored) {}
        return entities;
    }

    public static void restoreSpansFromJson(Spannable s, String json) {
        if (s == null || json == null || json.isEmpty()) return;
        ArrayList<TLRPC.MessageEntity> entities = parseEntitiesFromJson(json);
        if (!entities.isEmpty()) {
            MediaDataController.addTextStyleRuns(entities, s, s);
            for (TLRPC.MessageEntity e : entities) {
                if (e instanceof TLRPC.TL_messageEntityTextUrl) {
                    s.setSpan(new URLSpanReplacement(e.url), e.offset, e.offset + e.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    public static class AutoReplyRule {
        public String name;
        public String keyword;
        public String response;
        public String entitiesJson;
        public boolean enabled = true;
        public boolean regex;

        public AutoReplyRule(String name, String keyword, String response) {
            this(name, keyword, response, null, false);
        }

        public AutoReplyRule(String name, String keyword, String response, String entitiesJson) {
            this(name, keyword, response, entitiesJson, false);
        }

        public AutoReplyRule(String name, String keyword, String response, String entitiesJson, boolean regex) {
            this.name = name;
            this.keyword = keyword;
            this.response = response;
            this.entitiesJson = entitiesJson;
            this.regex = regex;
            this.enabled = true;
        }
    }
}
