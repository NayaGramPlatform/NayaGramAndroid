package org.nayagram.platform.translator;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * InstantTranslatorManager - In-Chat Dual-Language Instant Translator for NayaGram.
 * 100% Google Play Store Compliant: Translates incoming messages from foreign languages
 * (English, Arabic, Urdu, Russian, Turkish, etc.) to Bengali ("bn") or user's target language.
 * Includes LRU Cache to prevent redundant network calls and save battery/data.
 */
public final class InstantTranslatorManager {

    private static final String PREFS_NAME = "nayagram_translator_prefs";
    private static final String KEY_TARGET_LANG = "target_language";
    private static final String DEFAULT_LANG = "bn"; // Bengali

    private static volatile InstantTranslatorManager sInstance;
    private final SharedPreferences preferences;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // LRU Cache for translated strings (stores up to 300 recent messages)
    private final Map<String, String> translationCache = new LinkedHashMap<String, String>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 300;
        }
    };

    private InstantTranslatorManager(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static InstantTranslatorManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (InstantTranslatorManager.class) {
                if (sInstance == null) {
                    sInstance = new InstantTranslatorManager(context);
                }
            }
        }
        return sInstance;
    }

    public interface TranslationCallback {
        void onTranslationSuccess(String originalText, String translatedText, String targetLang);
        void onTranslationFailed(String originalText, String errorMessage);
    }

    public String getTargetLanguage() {
        return preferences.getString(KEY_TARGET_LANG, DEFAULT_LANG);
    }

    public void setTargetLanguage(String langCode) {
        preferences.edit().putString(KEY_TARGET_LANG, langCode).apply();
    }

    /**
     * Asynchronously translate message text. Checks LRU cache first for instant response.
     */
    public void translate(String text, String targetLang, TranslationCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) callback.onTranslationFailed(text, "Empty text");
            return;
        }

        final String lang = (targetLang != null && !targetLang.isEmpty()) ? targetLang : getTargetLanguage();
        final String cacheKey = lang + "::" + text;

        synchronized (translationCache) {
            if (translationCache.containsKey(cacheKey)) {
                String cached = translationCache.get(cacheKey);
                if (callback != null) {
                    callback.onTranslationSuccess(text, cached, lang);
                }
                return;
            }
        }

        executor.execute(() -> {
            try {
                String encodedText = URLEncoder.encode(text, "UTF-8");
                String urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="
                        + lang + "&dt=t&q=" + encodedText;

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    String rawJson = sb.toString();
                    String translated = parseGtxResponse(rawJson);

                    synchronized (translationCache) {
                        translationCache.put(cacheKey, translated);
                    }

                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onTranslationSuccess(text, translated, lang);
                        }
                    });
                } else {
                    final int responseCode = conn.getResponseCode();
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onTranslationFailed(text, "HTTP " + responseCode);
                        }
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onTranslationFailed(text, e.getMessage());
                    }
                });
            }
        });
    }

            private String parseGtxResponse(String rawJson) {
        try {
            // Standard gtx response: [[["translated text","original text",null,null,...]]]
            int firstQuote = rawJson.indexOf('"');
            if (firstQuote != -1) {
                int secondQuote = rawJson.indexOf('"', firstQuote + 1);
                if (secondQuote != -1) {
                    return rawJson.substring(firstQuote + 1, secondQuote)
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"");
                }
            }
        } catch (Exception ignored) {}
        return rawJson;
    }
}
