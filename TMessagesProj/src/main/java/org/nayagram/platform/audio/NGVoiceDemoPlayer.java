package org.nayagram.platform.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.utils.recorder.audio.NGVoiceOpusWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Offline English Voice Demo & Text-to-Speech PCM synthesizer for NayaGram Voice Studio.
 * Uses Android's built-in TextToSpeech engine to render clean 48 kHz mono PCM, caches the
 * English demo clip in memory for instant (<50 ms) voice preview playback through the exact
 * {@link NGVoiceChangerManager} DSP pipeline, and supports English & Bangla Text-to-Voice.
 */
public final class NGVoiceDemoPlayer {

    public static final int SAMPLE_RATE = 48_000;
    public static final int LANG_AUTO = 0;
    public static final int LANG_ENGLISH = 1;
    public static final int LANG_BANGLA = 2;

    private static final String ENGLISH_DEMO_TEXT =
            "Hello! Welcome to NayaGram Voice Studio. This is your live voice preview.";

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NGVoiceDemoWorker");
        t.setDaemon(true);
        return t;
    });

    private static final Object PLAY_LOCK = new Object();
    private static final AtomicInteger PLAY_GEN = new AtomicInteger(0);
    private static volatile AudioTrack activeTrack = null;

    private static final Object CACHE_LOCK = new Object();
    private static volatile short[] cachedEnglishDemo48k = null;

    private NGVoiceDemoPlayer() {}

    public interface PcmCallback {
        void onSuccess(@NonNull short[] pcm48k);
        void onError(@NonNull String message);
    }

    public interface StatusCallback {
        void onState(boolean playing, @Nullable String error);
    }

    /**
     * Plays a live English voice demo through the exact StudioDsp for {@code mode} and custom parameters.
     */
    public static void playEnglishDemo(
            @Nullable Context context,
            int mode,
            float pitchSemitones,
            float speed,
            float fxMix,
            boolean noiseGate,
            @Nullable StatusCallback callback
    ) {
        final Context appCtx = resolveContext(context);
        if (appCtx == null) {
            postStatus(callback, false, "Audio context unavailable");
            return;
        }
        final int gen = PLAY_GEN.incrementAndGet();
        stopActiveTrack();

        WORKER.execute(() -> {
            if (gen != PLAY_GEN.get()) return;
            short[] basePcm = getOrSynthesizeEnglishDemo(appCtx);
            if (gen != PLAY_GEN.get()) return;
            if (basePcm == null || basePcm.length == 0) {
                postStatus(callback, false, "Install/enable Android Text-to-Speech (English) to hear voice demos");
                return;
            }
            short[] rendered = NGVoiceChangerManager.processOfflinePcm(
                    mode, pitchSemitones, speed, fxMix, noiseGate, basePcm, SAMPLE_RATE
            );
            if (gen != PLAY_GEN.get()) return;
            postStatus(callback, true, null);
            playPcmBlocking(rendered, SAMPLE_RATE, gen);
            if (gen == PLAY_GEN.get()) {
                postStatus(callback, false, null);
            }
        });
    }

    /**
     * Synthesizes arbitrary user text (Auto / English / Bangla) into clean 48 kHz mono PCM
     * and applies the selected Voice Changer DSP parameters.
     */
    public static void synthesizeTextWithVoice(
            @Nullable Context context,
            @NonNull String text,
            int langMode,
            float speechRate,
            int voiceMode,
            float pitchSemitones,
            float voiceSpeed,
            float fxMix,
            boolean noiseGate,
            @NonNull PcmCallback callback
    ) {
        final Context appCtx = resolveContext(context);
        if (appCtx == null) {
            AndroidUtilities.runOnUIThread(() -> callback.onError("App context unavailable"));
            return;
        }
        final String cleanText = text.trim();
        if (cleanText.isEmpty()) {
            AndroidUtilities.runOnUIThread(() -> callback.onError("Please type some text first"));
            return;
        }
        WORKER.execute(() -> {
            Locale targetLocale = chooseLocale(cleanText, langMode);
            short[] raw48k = synthesizeToPcm48k(appCtx, cleanText, targetLocale, speechRate);
            if (raw48k == null || raw48k.length == 0) {
                AndroidUtilities.runOnUIThread(() ->
                        callback.onError("Speech engine could not synthesize this text. Check device TTS language settings."));
                return;
            }
            short[] processed = NGVoiceChangerManager.processOfflinePcm(
                    voiceMode, pitchSemitones, voiceSpeed, fxMix, noiseGate, raw48k, SAMPLE_RATE
            );
            AndroidUtilities.runOnUIThread(() -> callback.onSuccess(processed));
        });
    }

    /**
     * Plays a 48 kHz mono PCM buffer asynchronously (canceling any currently playing preview).
     */
    public static void playPcmAsync(@NonNull short[] pcm48k, @Nullable Runnable onDone) {
        final int gen = PLAY_GEN.incrementAndGet();
        stopActiveTrack();
        WORKER.execute(() -> {
            if (gen != PLAY_GEN.get()) return;
            playPcmBlocking(pcm48k, SAMPLE_RATE, gen);
            if (onDone != null) {
                AndroidUtilities.runOnUIThread(onDone);
            }
        });
    }

    public static void stopPlayback() {
        PLAY_GEN.incrementAndGet();
        stopActiveTrack();
    }

    private static void stopActiveTrack() {
        synchronized (PLAY_LOCK) {
            AudioTrack track = activeTrack;
            activeTrack = null;
            if (track != null) {
                try {
                    if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                        track.pause();
                        track.flush();
                        track.stop();
                    }
                } catch (Throwable ignored) {}
                try {
                    track.release();
                } catch (Throwable ignored) {}
            }
        }
    }

    private static void playPcmBlocking(@NonNull short[] pcm, int sampleRate, int gen) {
        if (pcm.length == 0 || gen != PLAY_GEN.get()) return;
        int minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        int bufBytes = Math.max(minBuf * 2, 8192);
        AudioTrack track = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(bufBytes)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build();
            } else {
                track = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufBytes,
                        AudioTrack.MODE_STREAM
                );
            }
            if (track.getState() != AudioTrack.STATE_INITIALIZED) {
                track.release();
                return;
            }
            synchronized (PLAY_LOCK) {
                if (gen != PLAY_GEN.get()) {
                    track.release();
                    return;
                }
                activeTrack = track;
            }
            track.play();
            int offset = 0;
            final int chunk = 2048;
            while (offset < pcm.length && gen == PLAY_GEN.get()) {
                int len = Math.min(chunk, pcm.length - offset);
                int written = track.write(pcm, offset, len);
                if (written <= 0) {
                    break;
                }
                offset += written;
            }
            // Brief tail drain so the last syllable doesn't clip
            if (gen == PLAY_GEN.get()) {
                short[] silence = new short[2400];
                track.write(silence, 0, silence.length);
                try {
                    Thread.sleep(60);
                } catch (InterruptedException ignored) {}
            }
        } catch (Throwable ignored) {
        } finally {
            synchronized (PLAY_LOCK) {
                if (activeTrack == track) {
                    activeTrack = null;
                }
            }
            if (track != null) {
                try {
                    track.stop();
                } catch (Throwable ignored) {}
                try {
                    track.release();
                } catch (Throwable ignored) {}
            }
        }
    }

    @Nullable
    private static short[] getOrSynthesizeEnglishDemo(@NonNull Context context) {
        short[] cached = cachedEnglishDemo48k;
        if (cached != null && cached.length > 0) {
            return cached;
        }
        synchronized (CACHE_LOCK) {
            if (cachedEnglishDemo48k != null && cachedEnglishDemo48k.length > 0) {
                return cachedEnglishDemo48k;
            }
            short[] synth = synthesizeToPcm48k(context, ENGLISH_DEMO_TEXT, Locale.US, 1.0f);
            if (synth != null && synth.length > 0) {
                cachedEnglishDemo48k = synth;
            }
            return synth;
        }
    }

    @NonNull
    public static Locale chooseLocale(@NonNull String text, int langMode) {
        if (langMode == LANG_ENGLISH) {
            return Locale.US;
        }
        if (langMode == LANG_BANGLA) {
            return new Locale("bn", "BD");
        }
        // Auto-detect Bangla script range U+0980..U+09FF
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x0980 && c <= 0x09FF) {
                return new Locale("bn", "BD");
            }
        }
        return Locale.US;
    }

    @Nullable
    private static short[] synthesizeToPcm48k(
            @NonNull Context context,
            @NonNull String text,
            @NonNull Locale locale,
            float speechRate
    ) {
        File cacheDir = context.getCacheDir();
        if (cacheDir == null) return null;
        File wavFile = new File(cacheDir, "ng_tts_" + UUID.randomUUID().toString() + ".wav");

        final CountDownLatch initLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(1);
        final AtomicInteger initStatus = new AtomicInteger(TextToSpeech.ERROR);
        final AtomicReference<TextToSpeech> ttsRef = new AtomicReference<>();
        final AtomicReference<Boolean> synthOk = new AtomicReference<>(Boolean.FALSE);

        try {
            final String utteranceId = "ng_utt_" + System.nanoTime();
            TextToSpeech tts = new TextToSpeech(context.getApplicationContext(), status -> {
                initStatus.set(status);
                initLatch.countDown();
            });
            ttsRef.set(tts);

            if (!initLatch.await(5, TimeUnit.SECONDS) || initStatus.get() != TextToSpeech.SUCCESS) {
                return null;
            }

            // Pick best available locale with smart fallbacks (bn-BD -> bn-IN -> bn -> en-US -> default)
            int langRes = tts.setLanguage(locale);
            if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                if ("bn".equalsIgnoreCase(locale.getLanguage())) {
                    langRes = tts.setLanguage(new Locale("bn", "IN"));
                    if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                        langRes = tts.setLanguage(new Locale("bn"));
                    }
                }
                if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                    langRes = tts.setLanguage(Locale.US);
                }
                if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.ENGLISH);
                }
            }

            float clampedRate = Math.max(0.55f, Math.min(1.8f, speechRate));
            tts.setSpeechRate(clampedRate);
            tts.setPitch(1.0f);

            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String id) {}

                @Override
                public void onDone(String id) {
                    if (utteranceId.equals(id)) {
                        synthOk.set(Boolean.TRUE);
                        doneLatch.countDown();
                    }
                }

                @Override
                public void onError(String id) {
                    if (utteranceId.equals(id)) {
                        doneLatch.countDown();
                    }
                }

                @Override
                public void onError(String id, int errorCode) {
                    if (utteranceId.equals(id)) {
                        doneLatch.countDown();
                    }
                }
            });

            Bundle params = new Bundle();
            int res = tts.synthesizeToFile(text, params, wavFile, utteranceId);
            if (res != TextToSpeech.SUCCESS) {
                return null;
            }

            if (!doneLatch.await(15, TimeUnit.SECONDS) || !Boolean.TRUE.equals(synthOk.get())) {
                return null;
            }
            if (!wavFile.exists() || wavFile.length() < 44) {
                return null;
            }
            return readWavToPcm48k(wavFile);
        } catch (Throwable ignored) {
            return null;
        } finally {
            TextToSpeech tts = ttsRef.get();
            if (tts != null) {
                try {
                    tts.stop();
                    tts.shutdown();
                } catch (Throwable ignored) {}
            }
            try {
                if (wavFile.exists()) {
                    wavFile.delete();
                }
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Parses a standard PCM WAV file (8/16-bit, mono or stereo, any sample rate) into 48 kHz mono PCM.
     */
    @Nullable
    private static short[] readWavToPcm48k(@NonNull File file) {
        byte[] data;
        try (FileInputStream fis = new FileInputStream(file)) {
            int len = (int) file.length();
            if (len < 44 || len > 32 * 1024 * 1024) return null;
            data = new byte[len];
            int pos = 0;
            while (pos < len) {
                int r = fis.read(data, pos, len - pos);
                if (r <= 0) break;
                pos += r;
            }
            if (pos < 44) return null;
        } catch (IOException e) {
            return null;
        }

        // Verify RIFF/WAVE header
        if (data[0] != 'R' || data[1] != 'I' || data[2] != 'F' || data[3] != 'F'
                || data[8] != 'W' || data[9] != 'A' || data[10] != 'V' || data[11] != 'E') {
            return null;
        }

        int channels = 1;
        int sampleRate = 22050;
        int bitsPerSample = 16;
        int dataOffset = -1;
        int dataSize = 0;

        int offset = 12;
        while (offset + 8 <= data.length) {
            int chunkId = ((data[offset] & 0xFF) << 24)
                    | ((data[offset + 1] & 0xFF) << 16)
                    | ((data[offset + 2] & 0xFF) << 8)
                    | (data[offset + 3] & 0xFF);
            int chunkSize = readLeInt(data, offset + 4);
            int payload = offset + 8;
            if (chunkSize < 0 || payload > data.length) break;

            if (chunkId == 0x666d7420) { // "fmt "
                if (payload + 16 <= data.length) {
                    channels = Math.max(1, readLeShort(data, payload + 2));
                    sampleRate = readLeInt(data, payload + 4);
                    bitsPerSample = readLeShort(data, payload + 14);
                }
            } else if (chunkId == 0x64617461) { // "data"
                dataOffset = payload;
                dataSize = Math.min(chunkSize, data.length - payload);
                break;
            }
            int next = payload + chunkSize + (chunkSize & 1);
            if (next <= offset) break;
            offset = next;
        }

        if (dataOffset < 0 || dataSize <= 0 || sampleRate <= 0) {
            return null;
        }

        short[] mono;
        if (bitsPerSample == 16) {
            int bytesPerFrame = channels * 2;
            int frameCount = dataSize / bytesPerFrame;
            mono = new short[frameCount];
            for (int i = 0; i < frameCount; i++) {
                int base = dataOffset + i * bytesPerFrame;
                if (channels == 1) {
                    mono[i] = (short) ((data[base] & 0xFF) | (data[base + 1] << 8));
                } else {
                    int sum = 0;
                    for (int c = 0; c < channels; c++) {
                        int p = base + c * 2;
                        sum += (short) ((data[p] & 0xFF) | (data[p + 1] << 8));
                    }
                    mono[i] = (short) (sum / channels);
                }
            }
        } else if (bitsPerSample == 8) {
            int frameCount = dataSize / channels;
            mono = new short[frameCount];
            for (int i = 0; i < frameCount; i++) {
                int base = dataOffset + i * channels;
                int sum = 0;
                for (int c = 0; c < channels; c++) {
                    sum += ((data[base + c] & 0xFF) - 128) << 8;
                }
                mono[i] = (short) (sum / channels);
            }
        } else {
            return null;
        }

        // Trim leading/trailing dead silence so previews and voice notes start crisply
        mono = trimSilence(mono, sampleRate);
        return NGVoiceOpusWriter.resampleLinear(mono, sampleRate, SAMPLE_RATE);
    }

    @NonNull
    private static short[] trimSilence(@NonNull short[] pcm, int sampleRate) {
        if (pcm.length < 200) return pcm;
        int threshold = 180;
        int start = 0;
        while (start < pcm.length && Math.abs(pcm[start]) < threshold) {
            start++;
        }
        int end = pcm.length - 1;
        while (end > start && Math.abs(pcm[end]) < threshold) {
            end--;
        }
        int pad = Math.min(sampleRate / 25, 1600); // ~40 ms natural breath margin
        start = Math.max(0, start - pad);
        end = Math.min(pcm.length - 1, end + pad);
        if (end <= start) return pcm;
        int len = end - start + 1;
        short[] trimmed = new short[len];
        System.arraycopy(pcm, start, trimmed, 0, len);
        return trimmed;
    }

    private static int readLeShort(byte[] b, int o) {
        return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8);
    }

    private static int readLeInt(byte[] b, int o) {
        return (b[o] & 0xFF)
                | ((b[o + 1] & 0xFF) << 8)
                | ((b[o + 2] & 0xFF) << 16)
                | ((b[o + 3] & 0xFF) << 24);
    }

    @Nullable
    private static Context resolveContext(@Nullable Context context) {
        if (context != null) return context.getApplicationContext();
        return ApplicationLoader.applicationContext;
    }

    private static void postStatus(@Nullable StatusCallback cb, boolean playing, @Nullable String error) {
        if (cb == null) return;
        AndroidUtilities.runOnUIThread(() -> cb.onState(playing, error));
    }
}
