package org.nayagram.platform.audio;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.utils.recorder.audio.NGVoiceOpusWriter;

import java.io.File;

/**
 * NGTextToVoiceHelper - Converts typed text (English or Bangla) into a crystal-clear
 * voice preview or genuine Telegram Ogg/Opus voice message using the active Voice Changer character.
 */
public final class NGTextToVoiceHelper {

    private NGTextToVoiceHelper() {}

    public static void showDialog(@Nullable Context context, @Nullable ChatActivity chatActivity) {
        final Context ctx = chatActivity != null && chatActivity.getParentActivity() != null
                ? chatActivity.getParentActivity()
                : context;
        if (ctx == null) return;
        if (ctx instanceof Activity) {
            Activity act = (Activity) ctx;
            if (act.isFinishing() || act.isDestroyed()) return;
        }

        final boolean isDark = Theme.isCurrentThemeDark();
        final boolean canSend = chatActivity != null && chatActivity.getDialogId() != 0;
        final int[] langMode = new int[]{NGVoiceDemoPlayer.LANG_AUTO};
        final float[] speechRate = new float[]{1.0f};
        final boolean[] busy = new boolean[]{false};

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(8));

        // Header banner
        LinearLayout header = new LinearLayout(ctx);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        GradientDrawable headerBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                isDark ? new int[]{0xFF172554, 0xFF1E1B4B} : new int[]{0xFFEFF6FF, 0xFFF5F3FF}
        );
        headerBg.setCornerRadius(AndroidUtilities.dp(16));
        headerBg.setStroke(AndroidUtilities.dp(1f), isDark ? 0x5538BDF8 : 0xFFBFDBFE);
        header.setBackground(headerBg);

        TextView titleTv = new TextView(ctx);
        titleTv.setText("Text to Voice Studio");
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f);
        titleTv.setTypeface(AndroidUtilities.bold());
        titleTv.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        header.addView(titleTv);

        TextView subTv = new TextView(ctx);
        subTv.setText(canSend
                ? "Type in English or বাংলা — convert & send as a real voice note"
                : "Type in English or বাংলা — preview speech in any voice character");
        subTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.2f);
        subTv.setTextColor(isDark ? 0xFF94A3B8 : 0xFF475569);
        subTv.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        header.addView(subTv);

        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLp.bottomMargin = AndroidUtilities.dp(12);
        root.addView(header, headerLp);

        // Active Voice Character Row (tap to switch voice!)
        LinearLayout voiceRow = new LinearLayout(ctx);
        voiceRow.setOrientation(LinearLayout.HORIZONTAL);
        voiceRow.setGravity(Gravity.CENTER_VERTICAL);
        voiceRow.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(9), AndroidUtilities.dp(12), AndroidUtilities.dp(9));
        GradientDrawable voiceBg = new GradientDrawable();
        voiceBg.setCornerRadius(AndroidUtilities.dp(12));
        voiceBg.setColor(isDark ? 0xFF1E293B : 0xFFF8FAFC);
        voiceBg.setStroke(AndroidUtilities.dp(1f), isDark ? 0xFF334155 : 0xFFE2E8F0);
        voiceRow.setBackground(voiceBg);

        final TextView voiceLabel = new TextView(ctx);
        voiceLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        voiceLabel.setTypeface(AndroidUtilities.bold());
        voiceLabel.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        voiceLabel.setSingleLine(true);
        voiceLabel.setEllipsize(TextUtils.TruncateAt.END);
        Runnable refreshVoiceLabel = () -> {
            int m = NGVoiceChangerManager.getSelectedMode(ctx);
            voiceLabel.setText("Voice: " + NGVoiceChangerManager.getModeTitle(m));
        };
        refreshVoiceLabel.run();
        voiceRow.addView(voiceLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView changeVoiceBtn = new TextView(ctx);
        changeVoiceBtn.setText("Change");
        changeVoiceBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        changeVoiceBtn.setTypeface(AndroidUtilities.bold());
        changeVoiceBtn.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        changeVoiceBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
        voiceRow.addView(changeVoiceBtn);

        voiceRow.setOnClickListener(v -> NGVoiceChangerManager.showDialog(ctx, refreshVoiceLabel));
        LinearLayout.LayoutParams vLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vLp.bottomMargin = AndroidUtilities.dp(10);
        root.addView(voiceRow, vLp);

        // Language selector row: Auto / English / বাংলা
        LinearLayout langRow = new LinearLayout(ctx);
        langRow.setOrientation(LinearLayout.HORIZONTAL);
        final String[] langLabels = new String[]{"Auto (EN/বাংলা)", "English", "বাংলা"};
        final int[] langCodes = new int[]{
                NGVoiceDemoPlayer.LANG_AUTO, NGVoiceDemoPlayer.LANG_ENGLISH, NGVoiceDemoPlayer.LANG_BANGLA
        };
        final TextView[] langChips = new TextView[langLabels.length];
        Runnable updateLangChips = () -> {
            for (int i = 0; i < langChips.length; i++) {
                boolean sel = langMode[0] == langCodes[i];
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(AndroidUtilities.dp(10));
                bg.setColor(sel ? (isDark ? 0xFF0284C7 : 0xFF0284C7) : (isDark ? 0xFF1E293B : 0xFFF1F5F9));
                langChips[i].setBackground(bg);
                langChips[i].setTextColor(sel ? Color.WHITE : (isDark ? 0xFFCBD5E1 : 0xFF334155));
            }
        };
        for (int i = 0; i < langLabels.length; i++) {
            final int idx = i;
            TextView chip = new TextView(ctx);
            chip.setText(langLabels[i]);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
            chip.setTypeface(AndroidUtilities.bold());
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(7), AndroidUtilities.dp(8), AndroidUtilities.dp(7));
            chip.setOnClickListener(v -> {
                langMode[0] = langCodes[idx];
                updateLangChips.run();
            });
            langChips[i] = chip;
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) clp.leftMargin = AndroidUtilities.dp(6);
            langRow.addView(chip, clp);
        }
        updateLangChips.run();
        LinearLayout.LayoutParams langLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        langLp.bottomMargin = AndroidUtilities.dp(10);
        root.addView(langRow, langLp);

        // Multi-line text input box
        final EditText input = new EditText(ctx);
        input.setHint("Write text to convert into voice...");
        input.setHintTextColor(isDark ? 0xFF64748B : 0xFF94A3B8);
        input.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        input.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setMinLines(3);
        input.setMaxLines(6);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        GradientDrawable editBg = new GradientDrawable();
        editBg.setCornerRadius(AndroidUtilities.dp(12));
        editBg.setColor(isDark ? 0xFF0F172A : 0xFFFFFFFF);
        editBg.setStroke(AndroidUtilities.dp(1.2f), isDark ? 0xFF334155 : 0xFFCBD5E1);
        input.setBackground(editBg);
        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.bottomMargin = AndroidUtilities.dp(10);
        root.addView(input, inLp);

        // Speech rate slider (0.75x - 1.35x)
        final TextView rateLabel = new TextView(ctx);
        rateLabel.setText("Speaking Speed: 1.00x");
        rateLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.2f);
        rateLabel.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF334155);
        root.addView(rateLabel);

        SeekBar rateBar = new SeekBar(ctx);
        rateBar.setMax(60); // 0..60 -> 0.75..1.35
        rateBar.setProgress(25); // 1.00
        rateBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speechRate[0] = 0.75f + (progress / 100f);
                rateLabel.setText(String.format(java.util.Locale.US, "Speaking Speed: %.2fx", speechRate[0]));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbLp.bottomMargin = AndroidUtilities.dp(10);
        root.addView(rateBar, sbLp);

        // Status label
        final TextView statusTv = new TextView(ctx);
        statusTv.setText("");
        statusTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        statusTv.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        statusTv.setVisibility(View.GONE);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stLp.bottomMargin = AndroidUtilities.dp(8);
        root.addView(statusTv, stLp);

        // Action Buttons row: Preview + Send Voice Note
        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        final TextView previewBtn = makeActionButton(ctx, "▶ Preview", isDark ? 0xFF1E293B : 0xFFE0F2FE, isDark ? 0xFF38BDF8 : 0xFF0369A1);
        btnRow.addView(previewBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView sendBtn;
        if (canSend) {
            sendBtn = makeActionButton(ctx, "Send Voice Note", 0xFF0284C7, Color.WHITE);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.25f);
            slp.leftMargin = AndroidUtilities.dp(8);
            btnRow.addView(sendBtn, slp);
        } else {
            sendBtn = null;
        }
        root.addView(btnRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(ctx);
        scroll.addView(root);

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setView(scroll);
        builder.setNegativeButton("Close", (d, w) -> {
            NGVoiceDemoPlayer.stopPlayback();
            d.dismiss();
        });
        final AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> NGVoiceDemoPlayer.stopPlayback());

        previewBtn.setOnClickListener(v -> {
            if (busy[0]) return;
            String txt = input.getText() == null ? "" : input.getText().toString().trim();
            if (txt.isEmpty()) {
                Toast.makeText(ctx, "Type some text first", Toast.LENGTH_SHORT).show();
                return;
            }
            busy[0] = true;
            statusTv.setText("Generating voice preview...");
            statusTv.setVisibility(View.VISIBLE);
            int mode = NGVoiceChangerManager.getSelectedMode(ctx);
            NGVoiceDemoPlayer.synthesizeTextWithVoice(
                    ctx,
                    txt,
                    langMode[0],
                    speechRate[0],
                    mode,
                    NGVoiceChangerManager.getCustomPitch(ctx, mode),
                    NGVoiceChangerManager.getCustomSpeed(ctx, mode),
                    NGVoiceChangerManager.getCustomFxMix(ctx, mode),
                    NGVoiceChangerManager.isNoiseGateEnabled(ctx),
                    new NGVoiceDemoPlayer.PcmCallback() {
                        @Override
                        public void onSuccess(short[] pcm48k) {
                            statusTv.setText("Playing voice preview...");
                            NGVoiceDemoPlayer.playPcmAsync(pcm48k, () -> {
                                busy[0] = false;
                                statusTv.setVisibility(View.GONE);
                            });
                        }

                        @Override
                        public void onError(String message) {
                            busy[0] = false;
                            statusTv.setText(message);
                            Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
                        }
                    }
            );
        });

        if (sendBtn != null && chatActivity != null) {
            sendBtn.setOnClickListener(v -> {
                if (busy[0]) return;
                String txt = input.getText() == null ? "" : input.getText().toString().trim();
                if (txt.isEmpty()) {
                    Toast.makeText(ctx, "Type some text first", Toast.LENGTH_SHORT).show();
                    return;
                }
                busy[0] = true;
                statusTv.setText("Encoding Ogg/Opus voice message...");
                statusTv.setVisibility(View.VISIBLE);
                int mode = NGVoiceChangerManager.getSelectedMode(ctx);
                NGVoiceDemoPlayer.synthesizeTextWithVoice(
                        ctx,
                        txt,
                        langMode[0],
                        speechRate[0],
                        mode,
                        NGVoiceChangerManager.getCustomPitch(ctx, mode),
                        NGVoiceChangerManager.getCustomSpeed(ctx, mode),
                        NGVoiceChangerManager.getCustomFxMix(ctx, mode),
                        NGVoiceChangerManager.isNoiseGateEnabled(ctx),
                        new NGVoiceDemoPlayer.PcmCallback() {
                            @Override
                            public void onSuccess(short[] pcm48k) {
                                Utilities.stageQueue.postRunnable(() -> {
                                    int currentAccount = chatActivity.getCurrentAccount();
                                    long dialogId = chatActivity.getDialogId();
                                    MessageObject threadMsg = chatActivity.getThreadMessage();

                                    long localId = SharedConfig.getLastLocalId();
                                    SharedConfig.saveConfig();
                                    TLRPC.TL_document doc = new TLRPC.TL_document();
                                    doc.file_reference = new byte[0];
                                    doc.dc_id = Integer.MIN_VALUE;
                                    doc.id = localId;
                                    doc.user_id = UserConfig.getInstance(currentAccount).getClientUserId();
                                    doc.mime_type = NGVoiceOpusWriter.MIME_TYPE;

                                    File audioDir = FileLoader.getDirectory(FileLoader.MEDIA_DIR_AUDIO);
                                    File outFile = new File(audioDir, System.currentTimeMillis() + "_" + FileLoader.getAttachFileName(doc));
                                    NGVoiceOpusWriter.Result encoded = NGVoiceOpusWriter.encodePcmToOgg(outFile, pcm48k, NGVoiceDemoPlayer.SAMPLE_RATE);

                                    AndroidUtilities.runOnUIThread(() -> {
                                        busy[0] = false;
                                        if (encoded == null || !encoded.file.exists()) {
                                            statusTv.setText("Failed to encode voice note");
                                            Toast.makeText(ctx, "Could not encode voice message", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        doc.date = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
                                        doc.size = encoded.file.length();

                                        TLRPC.TL_documentAttributeAudio attr = new TLRPC.TL_documentAttributeAudio();
                                        attr.voice = true;
                                        attr.duration = encoded.durationMs / 1000.0;
                                        if (encoded.waveform != null && encoded.waveform.length > 0) {
                                            attr.waveform = encoded.waveform;
                                            attr.flags |= 4;
                                        }
                                        doc.attributes.add(attr);

                                        SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(
                                                doc,
                                                null,
                                                encoded.file.getAbsolutePath(),
                                                dialogId,
                                                threadMsg,
                                                threadMsg,
                                                null,
                                                null,
                                                null,
                                                null,
                                                true,
                                                0,
                                                0,
                                                0,
                                                null,
                                                null,
                                                false
                                        );
                                        SendMessagesHelper.getInstance(currentAccount).sendMessage(params);
                                        Toast.makeText(ctx, "Voice note sent", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    });
                                });
                            }

                            @Override
                            public void onError(String message) {
                                busy[0] = false;
                                statusTv.setText(message);
                                Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
                            }
                        }
                );
            });
        }

        dialog.show();
    }

    private static TextView makeActionButton(Context ctx, String text, int bgColor, int textColor) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setTextColor(textColor);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(11), AndroidUtilities.dp(12), AndroidUtilities.dp(11));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(12));
        bg.setColor(bgColor);
        tv.setBackground(bg);
        return tv;
    }
}
