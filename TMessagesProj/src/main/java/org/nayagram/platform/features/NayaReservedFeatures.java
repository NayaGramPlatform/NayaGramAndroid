package org.nayagram.platform.features;

/**
 * NayaReservedFeatures - Internal feature stubs reserved for future releases (v1.1.0+).
 * STRICTLY DORMANT: Completely isolated from public UI, settings, and reflection.
 * Accessible only to internal maintainers in source code.
 */
public final class NayaReservedFeatures {

    private NayaReservedFeatures() {}

    // Feature 1: Voice to Text (Local Android SpeechRecognizer / Offline Vosk)
    public static final class VoiceToTextEngine {
        public static final boolean IS_ENABLED = false; // Dormant
        public static boolean isSupported() { return false; }
        public static void transcribeAudio(String audioPath, Object callback) {
            // Reserved for v1.1.0 local speech recognition
        }
    }

    // Feature 2: In-Chat Instant Translator
    public static final class InstantTranslator {
        public static final boolean IS_ENABLED = false; // Dormant
        public static void translateMessage(String text, String targetLang, Object callback) {
            // Reserved for v1.1.0 in-chat translation engine
        }
    }

    // Feature 3: Custom Bengali & Global Fonts / Chat Bubbles
    public static final class CustomFontManager {
        public static final boolean IS_ENABLED = false; // Dormant
        public static final String[] SUPPORTED_FONTS = {
            "SolaimanLipi", "Kalpurush", "HindSiliguri", "Roboto", "OpenSans"
        };
    }

    // Feature 4: Bottom / Top Chat Tabs
    public static final class ChatTabsLayout {
        public static final boolean IS_ENABLED = false; // Dormant
        public enum TabType { ALL, DIRECT, GROUPS, CHANNELS, BOTS, UNREAD }
    }

    // Feature 5: Per-Chat Biometric Lock
    public static final class BiometricChatLock {
        public static final boolean IS_ENABLED = false; // Dormant
        public static boolean isChatLocked(long dialogId) { return false; }
    }
}
