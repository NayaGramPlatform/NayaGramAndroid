package org.telegram.utils.recorder.audio;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Encodes 16-bit mono PCM samples into a genuine Telegram Ogg/Opus voice message (.ogg)
 * using Telegram's native {@link AudioRecorderNative} encoder and computes its real waveform.
 */
public final class NGVoiceOpusWriter {

    public static final int TARGET_SAMPLE_RATE = 48_000;
    public static final String MIME_TYPE = "audio/ogg";
    private static final int FRAME_SAMPLES = 960;
    private static final int FRAME_BYTES = FRAME_SAMPLES * 2;

    private NGVoiceOpusWriter() {}

    public static final class Result {
        public final File file;
        public final long durationMs;
        @Nullable
        public final byte[] waveform;

        public Result(@NonNull File file, long durationMs, @Nullable byte[] waveform) {
            this.file = file;
            this.durationMs = durationMs;
            this.waveform = waveform;
        }
    }

    /**
     * Resamples {@code pcmSamples} (if needed) to 48 kHz mono and encodes into {@code outputFile}.
     * Must be called on a background thread.
     */
    @Nullable
    public static Result encodePcmToOgg(@NonNull File outputFile, @NonNull short[] pcmSamples, int sampleRate) {
        if (pcmSamples.length == 0) {
            return null;
        }
        int sr = sampleRate > 0 ? sampleRate : TARGET_SAMPLE_RATE;
        short[] samples48k = (sr == TARGET_SAMPLE_RATE) ? pcmSamples : resampleLinear(pcmSamples, sr, TARGET_SAMPLE_RATE);
        if (samples48k.length == 0) {
            return null;
        }

        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (outputFile.exists()) {
            outputFile.delete();
        }

        AudioRecorderNative encoder = AudioRecorderNative.create(outputFile, TARGET_SAMPLE_RATE, null, 0L, -1L);
        if (encoder == null) {
            return null;
        }

        boolean finished = false;
        try {
            ByteBuffer frame = ByteBuffer.allocateDirect(FRAME_BYTES).order(ByteOrder.nativeOrder());
            int offset = 0;
            while (offset < samples48k.length) {
                int count = Math.min(FRAME_SAMPLES, samples48k.length - offset);
                frame.clear();
                for (int i = 0; i < count; i++) {
                    frame.putShort(samples48k[offset + i]);
                }
                // Pad final partial frame with silence to full 20 ms Opus frame
                for (int i = count; i < FRAME_SAMPLES; i++) {
                    frame.putShort((short) 0);
                }
                frame.position(0);
                frame.limit(FRAME_BYTES);
                if (encoder.writeFrame(frame, FRAME_BYTES) == 0) {
                    return null;
                }
                offset += count;
            }
            finished = encoder.finish();
            if (!finished || !outputFile.exists() || outputFile.length() <= 0) {
                return null;
            }
            long durationMs = Math.max(500L, (samples48k.length * 1000L) / TARGET_SAMPLE_RATE);
            byte[] waveform = AudioRecorderNative.getWaveform(outputFile.getAbsolutePath());
            return new Result(outputFile, durationMs, waveform);
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (!finished) {
                try {
                    encoder.recycle();
                } catch (Throwable ignored) {}
            }
        }
    }

    @NonNull
    public static short[] resampleLinear(@NonNull short[] input, int srcRate, int dstRate) {
        if (input.length == 0 || srcRate <= 0 || dstRate <= 0 || srcRate == dstRate) {
            return input;
        }
        double ratio = (double) srcRate / (double) dstRate;
        int outLen = Math.max(1, (int) Math.round(input.length / ratio));
        short[] out = new short[outLen];
        int lastIdx = input.length - 1;
        for (int i = 0; i < outLen; i++) {
            double srcPos = i * ratio;
            int idx = (int) srcPos;
            if (idx >= lastIdx) {
                out[i] = input[lastIdx];
            } else {
                double frac = srcPos - idx;
                double s = input[idx] * (1.0 - frac) + input[idx + 1] * frac;
                if (s > 32767.0) s = 32767.0;
                else if (s < -32768.0) s = -32768.0;
                out[i] = (short) Math.round(s);
            }
        }
        return out;
    }
}
