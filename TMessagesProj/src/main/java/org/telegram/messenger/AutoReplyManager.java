package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.nayagram.platform.NayaConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.QuoteSpan;
import org.telegram.ui.Components.TextStyleSpan;
import org.telegram.ui.Components.URLSpanReplacement;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AutoReplyManager - Clean Novagram-style Auto Answer engine.
 * - Single clean auto-answer message with native Telegram formatting & links.
 * - Replies once per dialog while enabled; resets answered list when turned off.
 * - Zero keyword rule clutter, zero markdown symbol artifacts.
 */
public class AutoReplyManager {
    private static AutoReplyManager instance;
    private static final String PREFS_NAME = "auto_reply_prefs";
    private static final String KEY_ENABLED = "auto_reply_enabled";
    private static final String KEY_TEXT = "auto_answer_text";
    private static final String KEY_ENTITIES = "auto_answer_entities";
    private static final String KEY_PRIVATE_ONLY = "auto_reply_private_only";
    private static final String KEY_ANSWERED_DIALOGS = "answered_dialogs";

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();
    private boolean autoReplyEnabled;
    private String autoAnswerText = "";
    private String autoAnswerEntitiesJson = null;
    private boolean privateOnly = true;
    private final Set<Long> answeredDialogs = new HashSet<>();

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
        if (!enabled) {
            answeredDialogs.clear();
            preferences.edit()
                    .putBoolean(KEY_ENABLED, false)
                    .remove(KEY_ANSWERED_DIALOGS)
                    .apply();
        } else {
            preferences.edit().putBoolean(KEY_ENABLED, true).apply();
        }
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

    public synchronized boolean hasAnsweredDialog(long dialogId) {
        return answeredDialogs.contains(dialogId);
    }

    public synchronized void markAnsweredDialog(long dialogId) {
        if (answeredDialogs.add(dialogId)) {
            saveAnsweredDialogs();
        }
    }

    public synchronized void removeAnsweredDialog(long dialogId) {
        if (answeredDialogs.remove(dialogId)) {
            saveAnsweredDialogs();
        }
    }

    public synchronized void clearAnsweredDialogs() {
        answeredDialogs.clear();
        preferences.edit().remove(KEY_ANSWERED_DIALOGS).apply();
    }

    public synchronized boolean shouldSendAutoReply(long dialogId, int accountId) {
        if (!isAutoReplyEnabled()) return false;
        if (privateOnly && dialogId <= 0) {
            return false;
        }
        return !answeredDialogs.contains(dialogId);
    }

    public synchronized String getAutoReplyResponse(String unused) {
        if (autoAnswerText != null && !autoAnswerText.trim().isEmpty()) {
            return autoAnswerText;
        }
        return null;
    }

    private void saveAnsweredDialogs() {
        try {
            preferences.edit().putString(KEY_ANSWERED_DIALOGS, gson.toJson(new ArrayList<>(answeredDialogs))).apply();
        } catch (Exception ignored) {}
    }

    private synchronized void load() {
        autoReplyEnabled = preferences.getBoolean(KEY_ENABLED, false);
        autoAnswerText = preferences.getString(KEY_TEXT, "");
        autoAnswerEntitiesJson = preferences.getString(KEY_ENTITIES, null);
        privateOnly = preferences.getBoolean(KEY_PRIVATE_ONLY, true);

        answeredDialogs.clear();
        String answeredJson = preferences.getString(KEY_ANSWERED_DIALOGS, null);
        if (answeredJson != null && !answeredJson.isEmpty()) {
            try {
                Type type = new TypeToken<List<Long>>() {}.getType();
                List<Long> stored = gson.fromJson(answeredJson, type);
                if (stored != null) {
                    answeredDialogs.addAll(stored);
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
        QuoteSpan.QuoteStyleSpan[] quotes = s.getSpans(0, s.length(), QuoteSpan.QuoteStyleSpan.class);
        if (quotes != null) {
            for (QuoteSpan.QuoteStyleSpan q : quotes) {
                int start = s.getSpanStart(q);
                int end = s.getSpanEnd(q);
                if (start >= 0 && end > start) list.add(new EntityData(7, start, end - start, null));
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
            Type type = new TypeToken<List<EntityData>>() {}.getType();
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
            int len = s.length();
            for (TLRPC.MessageEntity e : entities) {
                int start = Math.max(0, Math.min(e.offset, len));
                int end = Math.max(start, Math.min(e.offset + e.length, len));
                if (end <= start) continue;
                if (e instanceof TLRPC.TL_messageEntityBlockquote) {
                    if (s instanceof Editable) {
                        QuoteSpan.putQuoteToEditable((Editable) s, start, end, false);
                    } else {
                        QuoteSpan.putQuote(s, start, end, false);
                    }
                } else if (e instanceof TLRPC.TL_messageEntityTextUrl) {
                    s.setSpan(new URLSpanReplacement(e.url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }
}
