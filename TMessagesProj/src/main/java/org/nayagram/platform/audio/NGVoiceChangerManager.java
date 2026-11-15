package org.nayagram.platform.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.AlertDialog;

import java.nio.ByteBuffer;

/**
 * NGVoiceChangerManager - Real-time 48kHz mono PCM Voice Changer for Voice Notes.
 * Supports Normal, Chipmunk, Deep Voice, Robot, Helium, Cave Echo, and Telephone modes
 * with zero per-frame allocations inside the audio recording thread.
 */
public final class NGVoiceChangerManager {
    private static final String PREF_NAME = "nayagram_voice_changer_prefs";
    private static final String KEY_EFFECT_MODE = "voice_effect_mode";

    public static final int MODE_NORMAL = 0;
    public static final int MODE_CHIPMUNK = 1;
    public static final int MODE_DEEP = 2;
    public static final int MODE_ROBOT = 3;
    public static final int MODE_HELIUM = 4;
    public static final int MODE_CAVE = 5;
    public static final int MODE_TELEPHONE = 6;

    private static final int SAMPLE_RATE = 48_000;
    private static final int PITCH_RING_SIZE = 4096;
    private static final int PITCH_RING_MASK = PITCH_RING_SIZE - 1;
    private static final int GRAIN_SIZE = 1024;

    private static final int DELAY_RING_SIZE = 16384;
    private static final int DELAY_RING_MASK = DELAY_RING_SIZE - 1;

    private static volatile int cachedMode = -1;

    private static final Object DSP_LOCK = new Object();
    private static final float[] pitchRing = new float[PITCH_RING_SIZE];
    private static final float[] delayRing = new float[DELAY_RING_SIZE];
    private static int pitchWriteIdx = 0;
    private static float readPosA = 0f;
    private static float readPosB = GRAIN_SIZE * 0.5f;
    private static int delayIdx = 0;
    private static double robotPhase = 0.0;
    private static float lpPrev = 0f;
    private static float bpX1 = 0f, bpX2 = 0f, bpY1 = 0f, bpY2 = 0f;

    private NGVoiceChangerManager() {}

    private static SharedPreferences getPrefs(Context context) {
        if (context == null) {
            context = ApplicationLoader.applicationContext;
        }
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static int getSelectedMode(Context context) {
        if (cachedMode >= MODE_NORMAL && cachedMode <= MODE_TELEPHONE) {
            return cachedMode;
        }
        try {
            int mode = getPrefs(context).getInt(KEY_EFFECT_MODE, MODE_NORMAL);
            if (mode < MODE_NORMAL || mode > MODE_TELEPHONE) {
                mode = MODE_NORMAL;
            }
            cachedMode = mode;
            return mode;
        } catch (Throwable ignored) {
            return MODE_NORMAL;
        }
    }

    public static void setSelectedMode(Context context, int mode) {
        if (mode < MODE_NORMAL || mode > MODE_TELEPHONE) {
            mode = MODE_NORMAL;
        }
        cachedMode = mode;
        try {
            getPrefs(context).edit().putInt(KEY_EFFECT_MODE, mode).apply();
        } catch (Throwable ignored) {}
    }

    public static String getModeTitle(int mode) {
        switch (mode) {
            case MODE_CHIPMUNK: return "🐿️ Chipmunk (High Pitch)";
            case MODE_DEEP: return "🎙️ Deep Voice (Bass Boost)";
            case MODE_ROBOT: return "🤖 Robot (Vocoder)";
            case MODE_HELIUM: return "🎈 Helium";
            case MODE_CAVE: return "🏔️ Cave (Echo)";
            case MODE_TELEPHONE: return "☎️ Telephone (Bandpass)";
            default: return "🗣️ Normal Voice";
        }
    }

    public static void showDialog(Context context, Runnable onChanged) {
        if (context == null) return;
        final int current = getSelectedMode(context);
        final int[] modes = {
                MODE_NORMAL,
                MODE_CHIPMUNK,
                MODE_DEEP,
                MODE_ROBOT,
                MODE_HELIUM,
                MODE_CAVE,
                MODE_TELEPHONE
        };
        final CharSequence[] items = new CharSequence[modes.length];
        for (int i = 0; i < modes.length; i++) {
            String title = getModeTitle(modes[i]);
            items[i] = (modes[i] == current) ? (title + "  ✓") : title;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Voice Changer (Voice Notes)");
        builder.setItems(items, (dialog, which) -> {
            if (which >= 0 && which < modes.length) {
                int selected = modes[which];
                setSelectedMode(context, selected);
                resetSession();
                Toast.makeText(context, "Voice Changer: " + getModeTitle(selected), Toast.LENGTH_SHORT).show();
                if (onChanged != null) {
                    onChanged.run();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    public static void resetSession() {
        synchronized (DSP_LOCK) {
            java.util.Arrays.fill(pitchRing, 0f);
            java.util.Arrays.fill(delayRing, 0f);
            pitchWriteIdx = 0;
            readPosA = 0f;
            readPosB = GRAIN_SIZE * 0.5f;
            delayIdx = 0;
            robotPhase = 0.0;
            lpPrev = 0f;
            bpX1 = 0f;
            bpX2 = 0f;
            bpY1 = 0f;
            bpY2 = 0f;
        }
    }

    /**
     * Processes 16-bit mono 48kHz PCM samples in-place inside {@code buffer} over {@code byteCount} bytes.
     * Leaves buffer position restored to 0 on exit.
     */
    public static void processPcmBuffer(ByteBuffer buffer, int byteCount) {
        if (buffer == null || byteCount < 2) return;
        int mode = getSelectedMode(ApplicationLoader.applicationContext);
        if (mode == MODE_NORMAL) return;

        final int samples = byteCount >> 1;
        synchronized (DSP_LOCK) {
            switch (mode) {
                case MODE_CHIPMUNK:
                    processPitchShift(buffer, samples, 1.48f, false);
                    break;
                case MODE_DEEP:
                    processPitchShift(buffer, samples, 0.72f, true);
                    break;
                case MODE_HELIUM:
                    processPitchShift(buffer, samples, 1.72f, false);
                    break;
                case MODE_ROBOT:
                    processRobot(buffer, samples);
                    break;
                case MODE_CAVE:
                    processCaveEcho(buffer, samples);
                    break;
                case MODE_TELEPHONE:
                    processTelephone(buffer, samples);
                    break;
                default:
                    break;
            }
        }
        buffer.position(0);
    }

    private static void processPitchShift(ByteBuffer buffer, int samples, float pitchRatio, boolean bassWarmth) {
        // Precomputed relative offset limits ensure smooth crossfaded dual-head granular overlap-add
        final float delta = pitchRatio - 1.0f;
        final int halfGrain = GRAIN_SIZE >> 1;

        for (int i = 0; i < samples; i++) {
            int offset = i << 1;
            float in = buffer.getShort(offset);
            pitchRing[pitchWriteIdx & PITCH_RING_MASK] = in;

            float distA = wrapDistance(pitchWriteIdx, readPosA);
            float distB = wrapDistance(pitchWriteIdx, readPosB);

            if (pitchRatio > 1.0f) {
                if (distA < 32f || distA > GRAIN_SIZE) {
                    readPosA = (pitchWriteIdx - halfGrain) & PITCH_RING_MASK;
                    distA = halfGrain;
                }
                if (distB < 32f || distB > GRAIN_SIZE) {
                    readPosB = (pitchWriteIdx - halfGrain) & PITCH_RING_MASK;
                    distB = halfGrain;
                }
            } else {
                if (distA < 32f || distA > GRAIN_SIZE) {
                    readPosA = (pitchWriteIdx - 64) & PITCH_RING_MASK;
                    distA = 64f;
                }
                if (distB < 32f || distB > GRAIN_SIZE) {
                    readPosB = (pitchWriteIdx - 64) & PITCH_RING_MASK;
                    distB = 64f;
                }
            }

            float phaseA = Math.min(1f, Math.max(0f, distA / GRAIN_SIZE));
            float phaseB = Math.min(1f, Math.max(0f, distB / GRAIN_SIZE));
            float winA = 0.5f * (1f - (float) Math.cos(2.0 * Math.PI * phaseA));
            float winB = 0.5f * (1f - (float) Math.cos(2.0 * Math.PI * phaseB));
            float wSum = winA + winB;
            if (wSum < 0.001f) wSum = 1f;

            float sampleA = readInterpolated(readPosA);
            float sampleB = readInterpolated(readPosB);
            float out = (sampleA * winA + sampleB * winB) / wSum;

            if (bassWarmth) {
                lpPrev = lpPrev + 0.35f * (out - lpPrev);
                out = 0.65f * out + 0.50f * lpPrev;
            }

            buffer.putShort(offset, clampToShort(out));

            pitchWriteIdx = (pitchWriteIdx + 1) & PITCH_RING_MASK;
            readPosA += pitchRatio;
            if (readPosA >= PITCH_RING_SIZE) readPosA -= PITCH_RING_SIZE;
            if (readPosA < 0f) readPosA += PITCH_RING_SIZE;

            readPosB += pitchRatio;
            if (readPosB >= PITCH_RING_SIZE) readPosB -= PITCH_RING_SIZE;
            if (readPosB < 0f) readPosB += PITCH_RING_SIZE;

            if (Math.abs(delta) < 0.01f) {
                readPosA = (pitchWriteIdx - halfGrain) & PITCH_RING_MASK;
            }
        }
    }

    private static float wrapDistance(int writeIdx, float readPos) {
        float d = writeIdx - readPos;
        if (d < 0f) d += PITCH_RING_SIZE;
        return d;
    }

    private static float readInterpolated(float pos) {
        int i0 = ((int) pos) & PITCH_RING_MASK;
        int i1 = (i0 + 1) & PITCH_RING_MASK;
        float frac = pos - (int) pos;
        return pitchRing[i0] + frac * (pitchRing[i1] - pitchRing[i0]);
    }

    private static void processRobot(ByteBuffer buffer, int samples) {
        final double step = 2.0 * Math.PI * 65.0 / SAMPLE_RATE;
        for (int i = 0; i < samples; i++) {
            int offset = i << 1;
            float in = buffer.getShort(offset);
            float mod = (float) (0.70 * Math.sin(robotPhase) + 0.30 * Math.sin(robotPhase * 2.0));
            // Keep 28% dry signal so speech articulation stays crystal clear
            float out = (in * mod * 1.15f) + (in * 0.28f);
            buffer.putShort(offset, clampToShort(out));
            robotPhase += step;
            if (robotPhase >= 2.0 * Math.PI) {
                robotPhase -= 2.0 * Math.PI;
            }
        }
    }

    private static void processCaveEcho(ByteBuffer buffer, int samples) {
        final int tap1Offset = 5280;  // ~110ms
        final int tap2Offset = 10800; // ~225ms
        for (int i = 0; i < samples; i++) {
            int offset = i << 1;
            float in = buffer.getShort(offset);
            float e1 = delayRing[(delayIdx - tap1Offset) & DELAY_RING_MASK];
            float e2 = delayRing[(delayIdx - tap2Offset) & DELAY_RING_MASK];
            float wet = e1 * 0.45f + e2 * 0.30f;
            delayRing[delayIdx & DELAY_RING_MASK] = in + wet * 0.52f;
            float out = in * 0.85f + wet * 0.70f;
            buffer.putShort(offset, clampToShort(out));
            delayIdx = (delayIdx + 1) & DELAY_RING_MASK;
        }
    }

    private static void processTelephone(ByteBuffer buffer, int samples) {
        // Bandpass coefficients tuned for ~350 Hz – 3100 Hz @ 48 kHz
        final float b0 = 0.1602f, b1 = 0f, b2 = -0.1602f;
        final float a1 = -1.6485f, a2 = 0.6796f;
        for (int i = 0; i < samples; i++) {
            int offset = i << 1;
            float x0 = buffer.getShort(offset);
            float y0 = b0 * x0 + b1 * bpX1 + b2 * bpX2 - a1 * bpY1 - a2 * bpY2;
            bpX2 = bpX1;
            bpX1 = x0;
            bpY2 = bpY1;
            bpY1 = y0;
            // Subtle warm telephone saturation boost
            float norm = y0 / 32768f;
            float driven = (float) Math.tanh(norm * 2.2) * 32767f * 0.92f;
            buffer.putShort(offset, clampToShort(driven));
        }
    }

    private static short clampToShort(float value) {
        if (value > 32767f) return 32767;
        if (value < -32768f) return -32768;
        return (short) value;
    }
}
