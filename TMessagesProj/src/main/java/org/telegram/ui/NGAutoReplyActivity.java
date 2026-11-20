package org.telegram.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.AutoReplyManager;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextCaption;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Switch;
import org.telegram.ui.Components.URLSpanReplacement;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * NGAutoReplyActivity - Private 1-on-1 Inbox Auto Answer screen with full inbox-style
 * media composer (Photo, Video, Voice Note recorder, Audio, File), live chat bubble preview,
 * rich text formatting & hyperlink tools, and offline/background reliability status.
 * Groups, supergroups, broadcast channels, bots & Telegram Business cloud away are excluded.
 */
public class NGAutoReplyActivity extends BaseFragment {

    private static final int MENU_DONE = 1;
    private static final int MENU_TOGGLE = 3;

    private static final int REQ_PICK_PHOTO = 4101;
    private static final int REQ_PICK_VIDEO = 4102;
    private static final int REQ_PICK_AUDIO = 4103;
    private static final int REQ_PICK_FILE = 4104;
    private static final int REQ_PERM_RECORD_AUDIO = 4110;

    private boolean isDarkTheme;
    private Switch autoAnswerSwitch;
    private TextView autoAnswerStatusBadge;
    private ActionBarMenuItem toggleMenuItem;
    private EditTextCaption autoAnswerEditText;
    private boolean suppressTextWatcher = false;

    // Live Inbox Preview card views
    private LinearLayout previewMediaContainer;
    private TextView previewMessageText;
    private TextView previewMetaStamp;

    // Attached media chip & inline voice recorder views
    private LinearLayout attachedMediaBar;
    private LinearLayout voiceRecordBar;
    private TextView voiceRecordTimerText;

    // Voice recording & audio playback state
    private MediaRecorder voiceRecorder;
    private File activeRecordingFile;
    private long recordingStartElapsedMs = 0L;
    private boolean isRecordingVoice = false;
    private boolean recordingIsOpus = false;
    private final Runnable recordingTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRecordingVoice || voiceRecordTimerText == null) return;
            long elapsedSec = Math.max(0L, (SystemClock.elapsedRealtime() - recordingStartElapsedMs) / 1000L);
            voiceRecordTimerText.setText(formatDuration((int) elapsedSec) + "  •  Recording voice note...");
            AndroidUtilities.runOnUIThread(this, 250L);
        }
    };

    private MediaPlayer audioPlayer;
    private boolean isPlayingAudio = false;
    private TextView activePlayButtonRef;

    @Override
    public View createView(Context context) {
        isDarkTheme = Theme.isCurrentThemeDark();
        final boolean isDark = isDarkTheme;

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Auto answer");
        actionBar.setCastShadows(false);
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        actionBar.setItemsColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarWhiteSelector), false);
        actionBar.setTitleColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));

        ActionBarMenu menu = actionBar.createMenu();
        boolean currentEnabled = AutoReplyManager.getInstance().isAutoReplyEnabled();
        toggleMenuItem = menu.addItem(MENU_TOGGLE, currentEnabled ? R.drawable.msg_notifications : R.drawable.msg_mute);
        toggleMenuItem.setContentDescription("Toggle Auto Answer");
        menu.addItem(MENU_DONE, R.drawable.ic_ab_done);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    saveCurrentEditor();
                    finishFragment();
                } else if (id == MENU_DONE) {
                    saveCurrentEditor();
                    AndroidUtilities.hideKeyboard(autoAnswerEditText);
                    BulletinFactory.of(NGAutoReplyActivity.this)
                            .createSimpleBulletin(R.raw.done, "Private inbox auto answer saved")
                            .show();
                    finishFragment();
                } else if (id == MENU_TOGGLE) {
                    boolean newState = !AutoReplyManager.getInstance().isAutoReplyEnabled();
                    AutoReplyManager.getInstance().setAutoReplyEnabled(newState);
                    toggleMenuItem.setIcon(newState ? R.drawable.msg_notifications : R.drawable.msg_mute);
                    if (autoAnswerSwitch != null) {
                        autoAnswerSwitch.setChecked(newState, true);
                    }
                    if (autoAnswerStatusBadge != null) {
                        updateStatusPillBadge(autoAnswerStatusBadge, newState, 0xFF10B981, isDark);
                    }
                    BulletinFactory.of(NGAutoReplyActivity.this)
                            .createSimpleBulletin(
                                    newState ? R.raw.done : R.raw.chats_infotip,
                                    newState
                                            ? "Auto-answer ON (1-on-1 private inbox only)"
                                            : "Auto-answer turned OFF"
                            )
                            .show();
                }
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);
        root.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(28));
        scrollView.addView(column, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 1. Private Inbox Auto Answer Card (strictly 1-on-1 private human chats only;
        //    group/channel toggle & Telegram Business Cloud Away removed permanently)
        LinearLayout settingsCard = createSectionCard(context, isDark);

        TextView sectionHeader = new TextView(context);
        sectionHeader.setText("PRIVATE INBOX AUTO ANSWER");
        sectionHeader.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        sectionHeader.setTypeface(AndroidUtilities.bold());
        sectionHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        sectionHeader.setLetterSpacing(0.06f);
        sectionHeader.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        settingsCard.addView(sectionHeader);

        // Permanent Private-Only Safety Shield Banner
        LinearLayout shieldBanner = new LinearLayout(context);
        shieldBanner.setOrientation(LinearLayout.HORIZONTAL);
        shieldBanner.setGravity(Gravity.CENTER_VERTICAL);
        shieldBanner.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(9), AndroidUtilities.dp(12), AndroidUtilities.dp(9));
        GradientDrawable shieldBg = new GradientDrawable();
        shieldBg.setCornerRadius(AndroidUtilities.dp(12));
        shieldBg.setColor(isDark ? 0x2210B981 : 0x1610B981);
        shieldBg.setStroke(AndroidUtilities.dp(1), isDark ? 0x5534D399 : 0x4410B981);
        shieldBanner.setBackground(shieldBg);

        FrameLayout shieldIconWrap = new FrameLayout(context);
        GradientDrawable shieldIconBg = new GradientDrawable();
        shieldIconBg.setShape(GradientDrawable.OVAL);
        shieldIconBg.setColor(0xFF10B981);
        shieldIconWrap.setBackground(shieldIconBg);
        ImageView shieldIcon = new ImageView(context);
        shieldIcon.setImageResource(R.drawable.msg_openprofile);
        shieldIcon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        shieldIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        shieldIconWrap.addView(shieldIcon, LayoutHelper.createFrame(16, 16, Gravity.CENTER));
        shieldBanner.addView(shieldIconWrap, LayoutHelper.createLinear(28, 28, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));

        LinearLayout shieldTextCol = new LinearLayout(context);
        shieldTextCol.setOrientation(LinearLayout.VERTICAL);
        TextView shieldTitle = new TextView(context);
        shieldTitle.setText("1-on-1 Private Human Inbox Only");
        shieldTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        shieldTitle.setTypeface(AndroidUtilities.bold());
        shieldTitle.setTextColor(isDark ? 0xFF34D399 : 0xFF047857);
        shieldTextCol.addView(shieldTitle);

        TextView shieldSub = new TextView(context);
        shieldSub.setText("Groups, supergroups, channels & bots are permanently excluded.");
        shieldSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        shieldSub.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF334155);
        shieldSub.setPadding(0, AndroidUtilities.dp(1), 0, 0);
        shieldTextCol.addView(shieldSub);

        shieldBanner.addView(shieldTextCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        settingsCard.addView(shieldBanner, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 14, 0, 14, 10));

        settingsCard.addView(createHairlineDivider(context, isDark));

        AutoReplyManager mgr = AutoReplyManager.getInstance();
        boolean initialEnabled = mgr.isAutoReplyEnabled();
        autoAnswerSwitch = new Switch(context, createVividSwitchProvider(0xFF10B981, isDark));
        autoAnswerSwitch.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        autoAnswerSwitch.setDrawIconType(1);
        autoAnswerSwitch.setChecked(initialEnabled, false);
        autoAnswerStatusBadge = createStatusPillBadge(context, initialEnabled, 0xFF10B981, isDark);

        settingsCard.addView(createSwitchRow(
                context,
                R.drawable.ic_naya_robot,
                0xFF10B981,
                "Enable Auto Answer",
                "Replies automatically in 1-on-1 private inbox (online & offline background)",
                autoAnswerStatusBadge,
                autoAnswerSwitch,
                isDark,
                v -> {
                    boolean next = !autoAnswerSwitch.isChecked();
                    autoAnswerSwitch.setChecked(next, true);
                    updateStatusPillBadge(autoAnswerStatusBadge, next, 0xFF10B981, isDark);
                    if (toggleMenuItem != null) {
                        toggleMenuItem.setIcon(next ? R.drawable.msg_notifications : R.drawable.msg_mute);
                    }
                    AutoReplyManager.getInstance().setAutoReplyEnabled(next);
                }
        ));



        column.addView(settingsCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // 2. Live Inbox Preview Card (shows exact outgoing chat bubble with media + formatting)
        LinearLayout previewCard = createSectionCard(context, isDark);
        LinearLayout previewHeaderRow = new LinearLayout(context);
        previewHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        previewHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
        previewHeaderRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(8));

        TextView previewHeader = new TextView(context);
        previewHeader.setText("LIVE INBOX PREVIEW");
        previewHeader.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        previewHeader.setTypeface(AndroidUtilities.bold());
        previewHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        previewHeader.setLetterSpacing(0.06f);
        previewHeaderRow.addView(previewHeader, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView previewTag = new TextView(context);
        previewTag.setText("PRIVATE CHAT");
        previewTag.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10f);
        previewTag.setTypeface(AndroidUtilities.bold());
        previewTag.setTextColor(isDark ? 0xFF34D399 : 0xFF047857);
        previewTag.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
        GradientDrawable previewTagBg = new GradientDrawable();
        previewTagBg.setCornerRadius(AndroidUtilities.dp(8));
        previewTagBg.setColor(isDark ? 0x2610B981 : 0x1610B981);
        previewTag.setBackground(previewTagBg);
        previewHeaderRow.addView(previewTag);
        previewCard.addView(previewHeaderRow);

        // Chat canvas backdrop
        LinearLayout chatCanvas = new LinearLayout(context);
        chatCanvas.setOrientation(LinearLayout.VERTICAL);
        chatCanvas.setGravity(Gravity.END);
        chatCanvas.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        GradientDrawable canvasBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                isDark
                        ? new int[]{0xFF0B1324, 0xFF13203A}
                        : new int[]{0xFFE8F4FD, 0xFFF1F8FE}
        );
        canvasBg.setCornerRadius(AndroidUtilities.dp(14));
        canvasBg.setStroke(AndroidUtilities.dp(1), isDark ? 0xFF1E293B : 0xFFD6E9F8);
        chatCanvas.setBackground(canvasBg);

        // Outgoing message bubble
        LinearLayout bubble = new LinearLayout(context);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(AndroidUtilities.dp(11), AndroidUtilities.dp(9), AndroidUtilities.dp(11), AndroidUtilities.dp(8));
        GradientDrawable bubbleBg = new GradientDrawable();
        bubbleBg.setColor(isDark ? 0xFF1E3A5F : 0xFFDCF8C6);
        bubbleBg.setCornerRadii(new float[]{
                AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(4), AndroidUtilities.dp(4),
                AndroidUtilities.dp(16), AndroidUtilities.dp(16)
        });
        bubbleBg.setStroke(AndroidUtilities.dp(1), isDark ? 0x4438BDF8 : 0x4410B981);
        bubble.setBackground(bubbleBg);

        previewMediaContainer = new LinearLayout(context);
        previewMediaContainer.setOrientation(LinearLayout.VERTICAL);
        previewMediaContainer.setVisibility(View.GONE);
        bubble.addView(previewMediaContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        previewMessageText = new TextView(context);
        previewMessageText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
        previewMessageText.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        previewMessageText.setLinkTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        previewMessageText.setLineSpacing(AndroidUtilities.dp(1.5f), 1.0f);
        bubble.addView(previewMessageText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        previewMetaStamp = new TextView(context);
        previewMetaStamp.setText("Auto-reply  •  ✓✓");
        previewMetaStamp.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10.5f);
        previewMetaStamp.setTypeface(AndroidUtilities.bold());
        previewMetaStamp.setTextColor(isDark ? 0xFF93C5FD : 0xFF059669);
        previewMetaStamp.setGravity(Gravity.END);
        previewMetaStamp.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        bubble.addView(previewMetaStamp, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout.LayoutParams bubbleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bubbleLp.leftMargin = AndroidUtilities.dp(24);
        bubble.setMinimumWidth(AndroidUtilities.dp(160));
        chatCanvas.addView(bubble, bubbleLp);

        previewCard.addView(chatCanvas, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 14, 0, 14, 14));
        column.addView(previewCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // 3. Reply Message & Media Composer Card (inbox-style)
        LinearLayout editorCard = createSectionCard(context, isDark);

        LinearLayout editorHeaderRow = new LinearLayout(context);
        editorHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        editorHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
        editorHeaderRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(10));

        LinearLayout headerTitleCol = new LinearLayout(context);
        headerTitleCol.setOrientation(LinearLayout.VERTICAL);

        TextView editorHeader = new TextView(context);
        editorHeader.setText("REPLY MESSAGE & MEDIA");
        editorHeader.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        editorHeader.setTypeface(AndroidUtilities.bold());
        editorHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        editorHeader.setLetterSpacing(0.06f);
        headerTitleCol.addView(editorHeader);

        TextView editorSubHeader = new TextView(context);
        editorSubHeader.setText("Attach photo, video, voice note, audio or file just like inbox chat");
        editorSubHeader.setTextColor(isDark ? 0xFF94A3B8 : 0xFF475569);
        editorSubHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        editorSubHeader.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        headerTitleCol.addView(editorSubHeader);

        editorHeaderRow.addView(headerTitleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView resetBtn = new TextView(context);
        resetBtn.setText("↻ Reset sent");
        resetBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        resetBtn.setTypeface(AndroidUtilities.bold());
        resetBtn.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        resetBtn.setPadding(AndroidUtilities.dp(11), AndroidUtilities.dp(6), AndroidUtilities.dp(11), AndroidUtilities.dp(6));
        GradientDrawable resetBg = new GradientDrawable();
        resetBg.setColor(isDark ? 0x260284C7 : 0x140284C7);
        resetBg.setCornerRadius(AndroidUtilities.dp(10));
        resetBg.setStroke(AndroidUtilities.dp(1), isDark ? 0x6638BDF8 : 0x4D0284C7);
        resetBtn.setBackground(resetBg);
        resetBtn.setOnClickListener(v -> {
            AutoReplyManager.getInstance().clearAnsweredDialogs();
            BulletinFactory.of(NGAutoReplyActivity.this)
                    .createSimpleBulletin(R.raw.chats_infotip, "Answered chats history cleared")
                    .show();
        });
        editorHeaderRow.addView(resetBtn);
        editorCard.addView(editorHeaderRow);

        editorCard.addView(createHairlineDivider(context, isDark));

        // Inbox-style 4-button Media Attachment Strip
        LinearLayout mediaStrip = new LinearLayout(context);
        mediaStrip.setOrientation(LinearLayout.HORIZONTAL);
        mediaStrip.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
        mediaStrip.addView(createMediaAttachTile(context, R.drawable.msg_photos, 0xFF0EA5E9, "Photo", isDark, v -> launchPicker("image/*", REQ_PICK_PHOTO)));
        mediaStrip.addView(createMediaAttachTile(context, R.drawable.msg_video, 0xFF8B5CF6, "Video", isDark, v -> launchPicker("video/*", REQ_PICK_VIDEO)));
        mediaStrip.addView(createMediaAttachTile(context, R.drawable.input_mic, 0xFFEC4899, "Voice", isDark, v -> showVoiceOrAudioDialog(context)));
        mediaStrip.addView(createMediaAttachTile(context, R.drawable.msg_sendfile, 0xFFF59E0B, "File", isDark, v -> launchPicker("*/*", REQ_PICK_FILE)));
        editorCard.addView(mediaStrip, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Inline Voice Note Recorder Bar (hidden unless recording)
        voiceRecordBar = buildVoiceRecordBar(context, isDark);
        voiceRecordBar.setVisibility(View.GONE);
        editorCard.addView(voiceRecordBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 4, 12, 4));

        // Active Attached Media Bar (visible when media is attached)
        attachedMediaBar = new LinearLayout(context);
        attachedMediaBar.setOrientation(LinearLayout.HORIZONTAL);
        attachedMediaBar.setGravity(Gravity.CENTER_VERTICAL);
        attachedMediaBar.setVisibility(View.GONE);
        editorCard.addView(attachedMediaBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 4, 12, 4));

        // Spacious EditTextCaption for Message / Media Caption
        FrameLayout inputBoxWrap = new FrameLayout(context);
        inputBoxWrap.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(isDark ? 0xFF0F172A : 0xFFF8FAFC);
        inputBg.setCornerRadius(AndroidUtilities.dp(14));
        inputBg.setStroke(AndroidUtilities.dp(1.2f), isDark ? 0xFF334155 : 0xFFCBD5E1);

        autoAnswerEditText = new EditTextCaption(context, null);
        autoAnswerEditText.setBackground(inputBg);
        autoAnswerEditText.setGravity(Gravity.TOP | Gravity.START);
        autoAnswerEditText.setMinLines(5);
        autoAnswerEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
        autoAnswerEditText.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        autoAnswerEditText.setHintTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
        autoAnswerEditText.setLinkTextColor(0xFF0EA5E9);
        autoAnswerEditText.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        autoAnswerEditText.setSingleLine(false);
        autoAnswerEditText.setPadding(
                AndroidUtilities.dp(14),
                AndroidUtilities.dp(13),
                AndroidUtilities.dp(14),
                AndroidUtilities.dp(13)
        );

        String savedText = mgr.hasValidMedia() ? mgr.getRawAutoAnswerText() : mgr.getAutoAnswerText();
        String savedEntities = mgr.getAutoAnswerEntitiesJson();
        suppressTextWatcher = true;
        autoAnswerEditText.setText(savedText);
        if (savedEntities != null && !savedEntities.isEmpty()) {
            AutoReplyManager.restoreSpansFromJson(autoAnswerEditText.getText(), savedEntities);
        } else if (savedText != null && (savedText.contains("**") || savedText.contains("__") || savedText.contains("||") || savedText.contains("`") || savedText.contains("["))) {
            convertLegacyMarkdownToSpans(autoAnswerEditText.getText());
        }
        suppressTextWatcher = false;
        autoAnswerEditText.setSelection(autoAnswerEditText.getText() != null ? autoAnswerEditText.getText().length() : 0);

        autoAnswerEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {
                if (suppressTextWatcher) return;
                saveCurrentEditor();
                refreshPreviewBubble(context);
            }
        });

        inputBoxWrap.addView(autoAnswerEditText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        editorCard.addView(inputBoxWrap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Formatting & Link Toolbar docked BELOW the text editor
        TextView toolbarLabel = new TextView(context);
        toolbarLabel.setText("TEXT FORMATTING & HYPERLINK TOOLS");
        toolbarLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f);
        toolbarLabel.setTypeface(AndroidUtilities.bold());
        toolbarLabel.setTextColor(isDark ? 0xFF94A3B8 : 0xFF475569);
        toolbarLabel.setLetterSpacing(0.05f);
        toolbarLabel.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(2), AndroidUtilities.dp(16), AndroidUtilities.dp(4));
        editorCard.addView(toolbarLabel);

        editorCard.addView(buildFormattingToolbar(context, isDark));

        View bottomDivider = new View(context);
        bottomDivider.setBackgroundColor(isDark ? 0xFF334155 : 0xFFE2E8F0);
        editorCard.addView(bottomDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 4, 0, 0));

        // Footer row with Save button
        LinearLayout footerRow = new LinearLayout(context);
        footerRow.setOrientation(LinearLayout.HORIZONTAL);
        footerRow.setGravity(Gravity.CENTER_VERTICAL);
        footerRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        TextView footerHint = new TextView(context);
        footerHint.setText("✓ Auto-saves text, links & media for offline private replies");
        footerHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        footerHint.setTypeface(AndroidUtilities.bold());
        footerHint.setTextColor(isDark ? 0xFF34D399 : 0xFF059669);
        footerRow.addView(footerHint, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView saveNowBtn = new TextView(context);
        saveNowBtn.setText("Save");
        saveNowBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        saveNowBtn.setTypeface(AndroidUtilities.bold());
        saveNowBtn.setTextColor(Color.WHITE);
        saveNowBtn.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(7), AndroidUtilities.dp(16), AndroidUtilities.dp(7));
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setColor(0xFF0284C7);
        saveBg.setCornerRadius(AndroidUtilities.dp(10));
        saveNowBtn.setBackground(saveBg);
        saveNowBtn.setOnClickListener(v -> {
            saveCurrentEditor();
            AndroidUtilities.hideKeyboard(autoAnswerEditText);
            BulletinFactory.of(NGAutoReplyActivity.this)
                    .createSimpleBulletin(R.raw.done, "Auto answer message & media saved")
                    .show();
        });
        footerRow.addView(saveNowBtn);
        editorCard.addView(footerRow);

        column.addView(editorCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // Bottom info note
        TextView bottomInfoNote = new TextView(context);
        bottomInfoNote.setText("Private Inbox Protection: Auto Answer replies exclusively to direct 1-on-1 human chats and never sends messages in groups, supergroups, channels, or bots. It runs silently in the background and offline while keeping your last-seen status private.");
        bottomInfoNote.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        bottomInfoNote.setTextColor(isDark ? 0xFF94A3B8 : 0xFF475569);
        bottomInfoNote.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        bottomInfoNote.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(2), AndroidUtilities.dp(8), AndroidUtilities.dp(12));
        column.addView(bottomInfoNote, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        refreshAttachedMediaUi(context);
        refreshPreviewBubble(context);

        fragmentView = root;
        return fragmentView;
    }

    private View createMediaAttachTile(Context context, int iconRes, int color, String label, boolean isDark, View.OnClickListener onClick) {
        LinearLayout tile = new LinearLayout(context);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(9));
        tile.setClickable(true);
        tile.setOnClickListener(onClick);

        GradientDrawable tileBg = new GradientDrawable();
        tileBg.setCornerRadius(AndroidUtilities.dp(14));
        tileBg.setColor(isDark ? ((color & 0x00FFFFFF) | 0x22000000) : ((color & 0x00FFFFFF) | 0x14000000));
        tileBg.setStroke(AndroidUtilities.dp(1), (color & 0x00FFFFFF) | (isDark ? 0x66000000 : 0x44000000));
        tile.setBackground(tileBg);

        FrameLayout circle = new FrameLayout(context);
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(color);
        circle.setBackground(circleBg);

        ImageView iv = new ImageView(context);
        iv.setImageResource(iconRes);
        iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iv.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        circle.addView(iv, LayoutHelper.createFrame(18, 18, Gravity.CENTER));
        tile.addView(circle, LayoutHelper.createLinear(34, 34, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 5));

        TextView tv = new TextView(context);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setTextColor(isDark ? 0xFFF1F5F9 : color);
        tv.setSingleLine(true);
        tile.addView(tv);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = AndroidUtilities.dp(3);
        lp.rightMargin = AndroidUtilities.dp(3);
        tile.setLayoutParams(lp);
        return tile;
    }

    private LinearLayout buildVoiceRecordBar(Context context, boolean isDark) {
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(13));
        bg.setColor(isDark ? 0x33EF4444 : 0x18EF4444);
        bg.setStroke(AndroidUtilities.dp(1.2f), 0xFFEF4444);
        bar.setBackground(bg);

        View redDot = new View(context);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(0xFFEF4444);
        redDot.setBackground(dotBg);
        bar.addView(redDot, LayoutHelper.createLinear(10, 10, Gravity.CENTER_VERTICAL, 0, 0, 9, 0));

        voiceRecordTimerText = new TextView(context);
        voiceRecordTimerText.setText("0:00  •  Recording voice note...");
        voiceRecordTimerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        voiceRecordTimerText.setTypeface(AndroidUtilities.bold());
        voiceRecordTimerText.setTextColor(isDark ? 0xFFFECACA : 0xFFB91C1C);
        bar.addView(voiceRecordTimerText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView cancelBtn = new TextView(context);
        cancelBtn.setText("Cancel");
        cancelBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        cancelBtn.setTypeface(AndroidUtilities.bold());
        cancelBtn.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF475569);
        cancelBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
        cancelBtn.setOnClickListener(v -> stopVoiceRecording(false));
        bar.addView(cancelBtn);

        TextView stopSaveBtn = new TextView(context);
        stopSaveBtn.setText("Attach");
        stopSaveBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        stopSaveBtn.setTypeface(AndroidUtilities.bold());
        stopSaveBtn.setTextColor(Color.WHITE);
        stopSaveBtn.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setCornerRadius(AndroidUtilities.dp(9));
        saveBg.setColor(0xFF10B981);
        stopSaveBtn.setBackground(saveBg);
        stopSaveBtn.setOnClickListener(v -> stopVoiceRecording(true));
        bar.addView(stopSaveBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 6, 0, 0, 0));

        return bar;
    }

    private void showVoiceOrAudioDialog(Context context) {
        if (isRecordingVoice) {
            stopVoiceRecording(true);
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle("Voice Note or Audio");
        b.setItems(new CharSequence[]{
                "Record Voice Note (Microphone)",
                "Choose Audio File from Device"
        }, (d, which) -> {
            if (which == 0) {
                requestOrStartVoiceRecording();
            } else {
                launchPicker("audio/*", REQ_PICK_AUDIO);
            }
        });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(b.create());
    }

    private void requestOrStartVoiceRecording() {
        Activity act = getParentActivity();
        if (act == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && act.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            act.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_PERM_RECORD_AUDIO);
            return;
        }
        startVoiceRecording();
    }

    @Override
    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_PERM_RECORD_AUDIO) {
            if (grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecording();
            } else {
                BulletinFactory.of(this)
                        .createSimpleBulletin(R.raw.error, "Microphone permission is required to record a voice note")
                        .show();
            }
        }
    }

    private void startVoiceRecording() {
        Activity act = getParentActivity();
        if (act == null) return;
        stopAudioPlayback();
        File dir = AutoReplyManager.getMediaStorageDir(act);
        if (dir == null) {
            BulletinFactory.of(this)
                    .createSimpleBulletin(R.raw.error, "Storage unavailable for voice note")
                    .show();
            return;
        }

        boolean started = false;
        // Prefer native OGG/Opus on Android 10+ so Telegram sends a true playable Voice Note waveform
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File oggFile = new File(dir, "ar_voice_" + System.currentTimeMillis() + ".ogg");
            if (tryStartRecorder(oggFile, MediaRecorder.OutputFormat.OGG, MediaRecorder.AudioEncoder.OPUS)) {
                activeRecordingFile = oggFile;
                recordingIsOpus = true;
                started = true;
            }
        }
        if (!started) {
            File m4aFile = new File(dir, "ar_voice_" + System.currentTimeMillis() + ".m4a");
            if (tryStartRecorder(m4aFile, MediaRecorder.OutputFormat.MPEG_4, MediaRecorder.AudioEncoder.AAC)) {
                activeRecordingFile = m4aFile;
                recordingIsOpus = false;
                started = true;
            }
        }

        if (!started) {
            BulletinFactory.of(this)
                    .createSimpleBulletin(R.raw.error, "Could not start voice recording on this device")
                    .show();
            return;
        }

        isRecordingVoice = true;
        recordingStartElapsedMs = SystemClock.elapsedRealtime();
        if (voiceRecordBar != null) {
            voiceRecordBar.setVisibility(View.VISIBLE);
        }
        AndroidUtilities.cancelRunOnUIThread(recordingTimerRunnable);
        AndroidUtilities.runOnUIThread(recordingTimerRunnable);
    }

    private boolean tryStartRecorder(File outFile, int outputFormat, int audioEncoder) {
        MediaRecorder mr = null;
        try {
            mr = new MediaRecorder();
            mr.setAudioSource(MediaRecorder.AudioSource.MIC);
            mr.setOutputFormat(outputFormat);
            mr.setAudioEncoder(audioEncoder);
            mr.setAudioChannels(1);
            mr.setAudioSamplingRate(48000);
            mr.setAudioEncodingBitRate(64000);
            mr.setOutputFile(outFile.getAbsolutePath());
            mr.prepare();
            mr.start();
            voiceRecorder = mr;
            return true;
        } catch (Throwable t) {
            FileLog.e("NGAutoReplyActivity: recorder start failed for format " + outputFormat, t);
            if (mr != null) {
                try { mr.reset(); } catch (Throwable ignored) {}
                try { mr.release(); } catch (Throwable ignored) {}
            }
            try {
                if (outFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    outFile.delete();
                }
            } catch (Throwable ignored) {}
            return false;
        }
    }

    private void stopVoiceRecording(boolean save) {
        AndroidUtilities.cancelRunOnUIThread(recordingTimerRunnable);
        boolean wasRecording = isRecordingVoice;
        isRecordingVoice = false;
        if (voiceRecordBar != null) {
            voiceRecordBar.setVisibility(View.GONE);
        }

        int durationSec = wasRecording
                ? Math.max(1, (int) Math.round((SystemClock.elapsedRealtime() - recordingStartElapsedMs) / 1000.0))
                : 0;

        MediaRecorder mr = voiceRecorder;
        voiceRecorder = null;
        if (mr != null) {
            try {
                mr.stop();
            } catch (Throwable ignored) {
                save = false;
            }
            try {
                mr.reset();
                mr.release();
            } catch (Throwable ignored) {}
        }

        File recFile = activeRecordingFile;
        activeRecordingFile = null;
        if (!wasRecording || recFile == null) return;

        if (save && recFile.exists() && recFile.length() > 0) {
            String mime = recordingIsOpus ? "audio/ogg" : "audio/mp4";
            String name = recordingIsOpus ? "Voice Note.ogg" : "Voice Note.m4a";
            AutoReplyManager.getInstance().setAutoAnswerMedia(
                    AutoReplyManager.MEDIA_TYPE_VOICE,
                    recFile.getAbsolutePath(),
                    name,
                    mime,
                    durationSec
            );
            Context ctx = getParentActivity();
            if (ctx != null) {
                refreshAttachedMediaUi(ctx);
                refreshPreviewBubble(ctx);
            }
            BulletinFactory.of(this)
                    .createSimpleBulletin(R.raw.done, "Voice note attached (" + formatDuration(durationSec) + ")")
                    .show();
        } else {
            try {
                if (recFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    recFile.delete();
                }
            } catch (Throwable ignored) {}
        }
    }

    private void launchPicker(String mimeType, int requestCode) {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(mimeType);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, false);
            startActivityForResult(Intent.createChooser(intent, "Select media"), requestCode);
        } catch (Throwable t) {
            FileLog.e("NGAutoReplyActivity: picker failed", t);
            BulletinFactory.of(this)
                    .createSimpleBulletin(R.raw.error, "No app available to pick this media")
                    .show();
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        final int mediaType;
        final String fallbackExt;
        if (requestCode == REQ_PICK_PHOTO) {
            mediaType = AutoReplyManager.MEDIA_TYPE_PHOTO;
            fallbackExt = "jpg";
        } else if (requestCode == REQ_PICK_VIDEO) {
            mediaType = AutoReplyManager.MEDIA_TYPE_VIDEO;
            fallbackExt = "mp4";
        } else if (requestCode == REQ_PICK_AUDIO) {
            mediaType = AutoReplyManager.MEDIA_TYPE_AUDIO;
            fallbackExt = "mp3";
        } else if (requestCode == REQ_PICK_FILE) {
            mediaType = AutoReplyManager.MEDIA_TYPE_DOCUMENT;
            fallbackExt = "bin";
        } else {
            return;
        }

        final Uri uri = data.getData();
        final Context appCtx = getParentActivity() != null ? getParentActivity().getApplicationContext() : null;
        if (appCtx == null) return;

        Utilities.globalQueue.postRunnable(() -> {
            AutoReplyManager.CopiedMediaResult copied = AutoReplyManager.copyUriToMediaStorage(appCtx, uri, fallbackExt);
            if (copied == null) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (getParentActivity() != null) {
                        BulletinFactory.of(NGAutoReplyActivity.this)
                                .createSimpleBulletin(R.raw.error, "Could not attach file (max 100 MB)")
                                .show();
                    }
                });
                return;
            }
            int durationSec = 0;
            if (mediaType == AutoReplyManager.MEDIA_TYPE_VIDEO || mediaType == AutoReplyManager.MEDIA_TYPE_AUDIO) {
                durationSec = extractMediaDurationSeconds(copied.path);
            }
            int resolvedType = mediaType;
            if (mediaType == AutoReplyManager.MEDIA_TYPE_DOCUMENT && !TextUtils.isEmpty(copied.mimeType)) {
                String m = copied.mimeType.toLowerCase(Locale.ROOT);
                if (m.startsWith("image/")) {
                    resolvedType = AutoReplyManager.MEDIA_TYPE_PHOTO;
                } else if (m.startsWith("video/")) {
                    resolvedType = AutoReplyManager.MEDIA_TYPE_VIDEO;
                    durationSec = extractMediaDurationSeconds(copied.path);
                } else if (m.startsWith("audio/")) {
                    resolvedType = AutoReplyManager.MEDIA_TYPE_AUDIO;
                    durationSec = extractMediaDurationSeconds(copied.path);
                }
            }
            final int finalType = resolvedType;
            final int finalDuration = durationSec;
            AutoReplyManager.getInstance().setAutoAnswerMedia(
                    finalType,
                    copied.path,
                    copied.displayName,
                    copied.mimeType,
                    finalDuration
            );
            AndroidUtilities.runOnUIThread(() -> {
                Context uiCtx = getParentActivity();
                if (uiCtx == null) return;
                stopAudioPlayback();
                refreshAttachedMediaUi(uiCtx);
                refreshPreviewBubble(uiCtx);
                BulletinFactory.of(NGAutoReplyActivity.this)
                        .createSimpleBulletin(
                                R.raw.done,
                                AutoReplyManager.getMediaTypeLabel(finalType) + " attached for private auto-reply"
                        )
                        .show();
            });
        });
    }

    private static int extractMediaDurationSeconds(String path) {
        if (TextUtils.isEmpty(path)) return 0;
        MediaMetadataRetriever mmr = null;
        try {
            mmr = new MediaMetadataRetriever();
            mmr.setDataSource(path);
            String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (!TextUtils.isEmpty(dur)) {
                return Math.max(1, (int) Math.round(Long.parseLong(dur) / 1000.0));
            }
        } catch (Throwable ignored) {
        } finally {
            if (mmr != null) {
                try { mmr.release(); } catch (Throwable ignored) {}
            }
        }
        return 0;
    }

    private void refreshAttachedMediaUi(Context context) {
        if (attachedMediaBar == null || context == null) return;
        attachedMediaBar.removeAllViews();

        AutoReplyManager mgr = AutoReplyManager.getInstance();
        boolean hasMedia = mgr.hasValidMedia();
        if (autoAnswerEditText != null) {
            autoAnswerEditText.setHint(hasMedia
                    ? "Add an optional caption for your attached media..."
                    : "Write your private inbox auto-answer message...\ne.g. Assalamu Alaikum! I am currently busy and will reply soon.");
        }
        if (!hasMedia) {
            attachedMediaBar.setVisibility(View.GONE);
            return;
        }

        int type = mgr.getMediaType();
        String path = mgr.getMediaPath();
        String name = mgr.getMediaName();
        int duration = mgr.getMediaDuration();
        int accent = getMediaAccentColor(type);

        attachedMediaBar.setVisibility(View.VISIBLE);
        attachedMediaBar.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(9), AndroidUtilities.dp(10), AndroidUtilities.dp(9));
        GradientDrawable barBg = new GradientDrawable();
        barBg.setCornerRadius(AndroidUtilities.dp(13));
        barBg.setColor(isDarkTheme ? ((accent & 0x00FFFFFF) | 0x24000000) : ((accent & 0x00FFFFFF) | 0x14000000));
        barBg.setStroke(AndroidUtilities.dp(1.2f), (accent & 0x00FFFFFF) | (isDarkTheme ? 0x88000000 : 0x55000000));
        attachedMediaBar.setBackground(barBg);

        // Left thumbnail or media icon
        View leading = createMediaLeadingThumb(context, type, path, accent, 44);
        attachedMediaBar.addView(leading, LayoutHelper.createLinear(44, 44, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));

        // Info text column
        LinearLayout infoCol = new LinearLayout(context);
        infoCol.setOrientation(LinearLayout.VERTICAL);

        TextView titleTv = new TextView(context);
        titleTv.setText(TextUtils.isEmpty(name) ? AutoReplyManager.getMediaTypeLabel(type) : name);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        titleTv.setTypeface(AndroidUtilities.bold());
        titleTv.setTextColor(isDarkTheme ? 0xFFF8FAFC : 0xFF0F172A);
        titleTv.setSingleLine(true);
        titleTv.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        infoCol.addView(titleTv);

        StringBuilder meta = new StringBuilder(AutoReplyManager.getMediaTypeLabel(type));
        if (duration > 0) {
            meta.append("  •  ").append(formatDuration(duration));
        }
        long bytes = new File(path).length();
        if (bytes > 0) {
            meta.append("  •  ").append(formatFileSize(bytes));
        }
        TextView metaTv = new TextView(context);
        metaTv.setText(meta.toString());
        metaTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.8f);
        metaTv.setTextColor(isDarkTheme ? 0xFFCBD5E1 : 0xFF475569);
        metaTv.setSingleLine(true);
        metaTv.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        infoCol.addView(metaTv);

        attachedMediaBar.addView(infoCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Play preview button for voice/audio
        if (type == AutoReplyManager.MEDIA_TYPE_VOICE || type == AutoReplyManager.MEDIA_TYPE_AUDIO) {
            TextView playBtn = new TextView(context);
            playBtn.setText(isPlayingAudio ? "■ Stop" : "▶ Play");
            playBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
            playBtn.setTypeface(AndroidUtilities.bold());
            playBtn.setTextColor(Color.WHITE);
            playBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
            GradientDrawable playBg = new GradientDrawable();
            playBg.setCornerRadius(AndroidUtilities.dp(9));
            playBg.setColor(accent);
            playBtn.setBackground(playBg);
            playBtn.setOnClickListener(v -> toggleAudioPreview(path, playBtn));
            attachedMediaBar.addView(playBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 6, 0, 6, 0));
        }

        // Remove button
        FrameLayout removeWrap = new FrameLayout(context);
        GradientDrawable remBg = new GradientDrawable();
        remBg.setShape(GradientDrawable.OVAL);
        remBg.setColor(isDarkTheme ? 0x33EF4444 : 0x1CEF4444);
        removeWrap.setBackground(remBg);
        ImageView remIv = new ImageView(context);
        remIv.setImageResource(R.drawable.msg_delete);
        remIv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        remIv.setColorFilter(new PorterDuffColorFilter(0xFFEF4444, PorterDuff.Mode.SRC_IN));
        removeWrap.addView(remIv, LayoutHelper.createFrame(18, 18, Gravity.CENTER));
        removeWrap.setOnClickListener(v -> {
            stopAudioPlayback();
            AutoReplyManager.getInstance().clearAutoAnswerMedia();
            refreshAttachedMediaUi(context);
            refreshPreviewBubble(context);
            BulletinFactory.of(NGAutoReplyActivity.this)
                    .createSimpleBulletin(R.raw.chats_infotip, "Attached media removed")
                    .show();
        });
        attachedMediaBar.addView(removeWrap, LayoutHelper.createLinear(32, 32, Gravity.CENTER_VERTICAL, 2, 0, 0, 0));
    }

    private void refreshPreviewBubble(Context context) {
        if (context == null || previewMediaContainer == null || previewMessageText == null) return;
        previewMediaContainer.removeAllViews();

        AutoReplyManager mgr = AutoReplyManager.getInstance();
        boolean hasMedia = mgr.hasValidMedia();
        if (hasMedia) {
            previewMediaContainer.setVisibility(View.VISIBLE);
            int type = mgr.getMediaType();
            String path = mgr.getMediaPath();
            String name = mgr.getMediaName();
            int duration = mgr.getMediaDuration();
            int accent = getMediaAccentColor(type);

            if (type == AutoReplyManager.MEDIA_TYPE_PHOTO || type == AutoReplyManager.MEDIA_TYPE_VIDEO) {
                FrameLayout mediaFrame = new FrameLayout(context);
                mediaFrame.setClipToOutline(true);
                mediaFrame.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(11));
                    }
                });

                ImageView iv = new ImageView(context);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setBackgroundColor(isDarkTheme ? 0xFF0F172A : 0xFFE2E8F0);
                Bitmap bmp = decodePreviewBitmap(type, path, AndroidUtilities.dp(220), AndroidUtilities.dp(150));
                if (bmp != null) {
                    iv.setImageBitmap(bmp);
                } else {
                    iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    iv.setImageResource(type == AutoReplyManager.MEDIA_TYPE_VIDEO ? R.drawable.msg_video : R.drawable.msg_photos);
                    iv.setColorFilter(new PorterDuffColorFilter(accent, PorterDuff.Mode.SRC_IN));
                }
                mediaFrame.addView(iv, LayoutHelper.createFrame(220, 140));

                if (type == AutoReplyManager.MEDIA_TYPE_VIDEO) {
                    TextView durPill = new TextView(context);
                    durPill.setText("▶  " + (duration > 0 ? formatDuration(duration) : "Video"));
                    durPill.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f);
                    durPill.setTypeface(AndroidUtilities.bold());
                    durPill.setTextColor(Color.WHITE);
                    durPill.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
                    GradientDrawable durBg = new GradientDrawable();
                    durBg.setCornerRadius(AndroidUtilities.dp(8));
                    durBg.setColor(0xAA0F172A);
                    durPill.setBackground(durBg);
                    mediaFrame.addView(durPill, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START, 8, 0, 0, 8));
                }

                previewMediaContainer.addView(mediaFrame, LayoutHelper.createLinear(220, 140));
            } else if (type == AutoReplyManager.MEDIA_TYPE_VOICE || type == AutoReplyManager.MEDIA_TYPE_AUDIO) {
                LinearLayout voiceRow = new LinearLayout(context);
                voiceRow.setOrientation(LinearLayout.HORIZONTAL);
                voiceRow.setGravity(Gravity.CENTER_VERTICAL);
                voiceRow.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(7), AndroidUtilities.dp(10), AndroidUtilities.dp(7));
                GradientDrawable vbg = new GradientDrawable();
                vbg.setCornerRadius(AndroidUtilities.dp(12));
                vbg.setColor(isDarkTheme ? 0x260EA5E9 : 0x2210B981);
                voiceRow.setBackground(vbg);

                TextView playPill = new TextView(context);
                playPill.setText("▶");
                playPill.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
                playPill.setTextColor(Color.WHITE);
                playPill.setGravity(Gravity.CENTER);
                GradientDrawable pbg = new GradientDrawable();
                pbg.setShape(GradientDrawable.OVAL);
                pbg.setColor(accent);
                playPill.setBackground(pbg);
                playPill.setOnClickListener(v -> toggleAudioPreview(path, playPill));
                voiceRow.addView(playPill, LayoutHelper.createLinear(34, 34, Gravity.CENTER_VERTICAL, 0, 0, 9, 0));

                LinearLayout waveCol = new LinearLayout(context);
                waveCol.setOrientation(LinearLayout.VERTICAL);

                WaveformPreviewView wave = new WaveformPreviewView(context, isDarkTheme ? 0xFF38BDF8 : 0xFF059669);
                waveCol.addView(wave, LayoutHelper.createLinear(135, 18));

                TextView sub = new TextView(context);
                String lbl = type == AutoReplyManager.MEDIA_TYPE_VOICE ? "Voice message" : (TextUtils.isEmpty(name) ? "Audio" : name);
                sub.setText(lbl + (duration > 0 ? "  •  " + formatDuration(duration) : ""));
                sub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f);
                sub.setTextColor(isDarkTheme ? 0xFFCBD5E1 : 0xFF334155);
                sub.setSingleLine(true);
                sub.setEllipsize(TextUtils.TruncateAt.END);
                sub.setPadding(0, AndroidUtilities.dp(2), 0, 0);
                waveCol.addView(sub);

                voiceRow.addView(waveCol, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
                previewMediaContainer.addView(voiceRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            } else {
                LinearLayout docRow = new LinearLayout(context);
                docRow.setOrientation(LinearLayout.HORIZONTAL);
                docRow.setGravity(Gravity.CENTER_VERTICAL);
                docRow.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(7), AndroidUtilities.dp(10), AndroidUtilities.dp(7));
                GradientDrawable dbg = new GradientDrawable();
                dbg.setCornerRadius(AndroidUtilities.dp(11));
                dbg.setColor(isDarkTheme ? 0x26F59E0B : 0x1AF59E0B);
                docRow.setBackground(dbg);

                FrameLayout docIconWrap = new FrameLayout(context);
                GradientDrawable dibg = new GradientDrawable();
                dibg.setShape(GradientDrawable.OVAL);
                dibg.setColor(accent);
                docIconWrap.setBackground(dibg);
                ImageView div = new ImageView(context);
                div.setImageResource(R.drawable.msg_sendfile);
                div.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
                docIconWrap.addView(div, LayoutHelper.createFrame(18, 18, Gravity.CENTER));
                docRow.addView(docIconWrap, LayoutHelper.createLinear(34, 34, Gravity.CENTER_VERTICAL, 0, 0, 9, 0));

                LinearLayout dCol = new LinearLayout(context);
                dCol.setOrientation(LinearLayout.VERTICAL);
                TextView dName = new TextView(context);
                dName.setText(TextUtils.isEmpty(name) ? "Attachment" : name);
                dName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
                dName.setTypeface(AndroidUtilities.bold());
                dName.setTextColor(isDarkTheme ? 0xFFF8FAFC : 0xFF0F172A);
                dName.setSingleLine(true);
                dName.setMaxWidth(AndroidUtilities.dp(170));
                dName.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                dCol.addView(dName);

                TextView dSize = new TextView(context);
                dSize.setText(formatFileSize(new File(path).length()));
                dSize.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f);
                dSize.setTextColor(isDarkTheme ? 0xFFCBD5E1 : 0xFF475569);
                dCol.addView(dSize);
                docRow.addView(dCol);

                previewMediaContainer.addView(docRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            }
        } else {
            previewMediaContainer.setVisibility(View.GONE);
        }

        Editable currentEditable = autoAnswerEditText != null ? autoAnswerEditText.getText() : null;
        if (currentEditable != null && !currentEditable.toString().trim().isEmpty()) {
            SpannableStringBuilder previewCopy = new SpannableStringBuilder(currentEditable);
            previewMessageText.setText(previewCopy);
            previewMessageText.setVisibility(View.VISIBLE);
        } else if (hasMedia) {
            previewMessageText.setVisibility(View.GONE);
        } else {
            previewMessageText.setText(AutoReplyManager.getInstance().getAutoAnswerText());
            previewMessageText.setVisibility(View.VISIBLE);
        }
    }

    private View createMediaLeadingThumb(Context context, int type, String path, int accent, int sizeDp) {
        if (type == AutoReplyManager.MEDIA_TYPE_PHOTO || type == AutoReplyManager.MEDIA_TYPE_VIDEO) {
            Bitmap bmp = decodePreviewBitmap(type, path, AndroidUtilities.dp(sizeDp), AndroidUtilities.dp(sizeDp));
            if (bmp != null) {
                ImageView iv = new ImageView(context);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageBitmap(bmp);
                iv.setClipToOutline(true);
                iv.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(10));
                    }
                });
                return iv;
            }
        }
        FrameLayout wrap = new FrameLayout(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(10));
        bg.setColor(accent);
        wrap.setBackground(bg);

        ImageView iv = new ImageView(context);
        int iconRes;
        if (type == AutoReplyManager.MEDIA_TYPE_PHOTO) iconRes = R.drawable.msg_photos;
        else if (type == AutoReplyManager.MEDIA_TYPE_VIDEO) iconRes = R.drawable.msg_video;
        else if (type == AutoReplyManager.MEDIA_TYPE_VOICE || type == AutoReplyManager.MEDIA_TYPE_AUDIO) iconRes = R.drawable.input_mic;
        else iconRes = R.drawable.msg_sendfile;
        iv.setImageResource(iconRes);
        iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iv.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        wrap.addView(iv, LayoutHelper.createFrame(22, 22, Gravity.CENTER));
        return wrap;
    }

    private static Bitmap decodePreviewBitmap(int type, String path, int reqW, int reqH) {
        if (TextUtils.isEmpty(path)) return null;
        try {
            if (type == AutoReplyManager.MEDIA_TYPE_VIDEO) {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                try {
                    mmr.setDataSource(path);
                    Bitmap frame = mmr.getFrameAtTime(0);
                    if (frame != null && (frame.getWidth() > reqW * 2 || frame.getHeight() > reqH * 2)) {
                        float scale = Math.min((float) reqW / frame.getWidth(), (float) reqH / frame.getHeight());
                        int nw = Math.max(1, Math.round(frame.getWidth() * scale));
                        int nh = Math.max(1, Math.round(frame.getHeight() * scale));
                        Bitmap scaled = Bitmap.createScaledBitmap(frame, nw, nh, true);
                        if (scaled != frame) frame.recycle();
                        return scaled;
                    }
                    return frame;
                } finally {
                    try { mmr.release(); } catch (Throwable ignored) {}
                }
            } else {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, opts);
                int sample = 1;
                while (opts.outWidth / (sample * 2) >= reqW && opts.outHeight / (sample * 2) >= reqH) {
                    sample *= 2;
                }
                opts.inJustDecodeBounds = false;
                opts.inSampleSize = Math.max(1, sample);
                return BitmapFactory.decodeFile(path, opts);
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void toggleAudioPreview(String path, TextView button) {
        if (isPlayingAudio) {
            stopAudioPlayback();
            return;
        }
        try {
            audioPlayer = new MediaPlayer();
            audioPlayer.setDataSource(path);
            audioPlayer.setOnCompletionListener(mp -> stopAudioPlayback());
            audioPlayer.setOnErrorListener((mp, what, extra) -> {
                stopAudioPlayback();
                return true;
            });
            audioPlayer.prepare();
            audioPlayer.start();
            isPlayingAudio = true;
            activePlayButtonRef = button;
            if (button != null) {
                button.setText(button.getText().length() <= 2 ? "■" : "■ Stop");
            }
        } catch (Throwable t) {
            FileLog.e("NGAutoReplyActivity: audio preview failed", t);
            stopAudioPlayback();
            BulletinFactory.of(this)
                    .createSimpleBulletin(R.raw.error, "Cannot play this audio file")
                    .show();
        }
    }

    private void stopAudioPlayback() {
        isPlayingAudio = false;
        if (activePlayButtonRef != null) {
            activePlayButtonRef.setText(activePlayButtonRef.getText().length() <= 2 ? "▶" : "▶ Play");
            activePlayButtonRef = null;
        }
        if (audioPlayer != null) {
            try {
                if (audioPlayer.isPlaying()) {
                    audioPlayer.stop();
                }
            } catch (Throwable ignored) {}
            try {
                audioPlayer.release();
            } catch (Throwable ignored) {}
            audioPlayer = null;
        }
    }

    private static int getMediaAccentColor(int type) {
        switch (type) {
            case AutoReplyManager.MEDIA_TYPE_PHOTO: return 0xFF0EA5E9;
            case AutoReplyManager.MEDIA_TYPE_VIDEO: return 0xFF8B5CF6;
            case AutoReplyManager.MEDIA_TYPE_VOICE: return 0xFFEC4899;
            case AutoReplyManager.MEDIA_TYPE_AUDIO: return 0xFF10B981;
            default: return 0xFFF59E0B;
        }
    }

    private static String formatDuration(int seconds) {
        int m = Math.max(0, seconds) / 60;
        int s = Math.max(0, seconds) % 60;
        return String.format(Locale.US, "%d:%02d", m, s);
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024f);
        return String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f));
    }

    private static final class WaveformPreviewView extends View {
        private static final float[] BARS = {
                0.28f, 0.55f, 0.82f, 0.44f, 0.95f, 0.68f, 0.35f, 0.78f,
                0.92f, 0.50f, 0.64f, 0.88f, 0.40f, 0.72f, 0.58f, 0.30f, 0.62f, 0.42f
        };
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        WaveformPreviewView(Context context, int color) {
            super(context);
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;
            float step = w / (float) BARS.length;
            float barW = Math.max(AndroidUtilities.dp(2.2f), step * 0.55f);
            float rad = barW * 0.5f;
            for (int i = 0; i < BARS.length; i++) {
                float bh = Math.max(AndroidUtilities.dp(3), h * BARS[i]);
                float left = i * step + (step - barW) * 0.5f;
                float top = (h - bh) * 0.5f;
                canvas.drawRoundRect(left, top, left + barW, top + bh, rad, rad, paint);
            }
        }
    }

    private Theme.ResourcesProvider createVividSwitchProvider(int checkedTrackColor, boolean isDark) {
        return new Theme.ResourcesProvider() {
            @Override
            public int getColor(int key) {
                if (key == Theme.key_switchTrackChecked) {
                    return checkedTrackColor;
                } else if (key == Theme.key_switchTrack) {
                    return isDark ? 0xFF475569 : 0xFFCBD5E1;
                } else if (key == Theme.key_windowBackgroundWhite) {
                    return Color.WHITE;
                }
                return Theme.getColor(key);
            }
        };
    }

    private TextView createStatusPillBadge(Context context, boolean checked, int activeColor, boolean isDark) {
        TextView badge = new TextView(context);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
        badge.setTypeface(AndroidUtilities.bold());
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
        updateStatusPillBadge(badge, checked, activeColor, isDark);
        return badge;
    }

    private void updateStatusPillBadge(TextView badge, boolean checked, int activeColor, boolean isDark) {
        if (badge == null) return;
        badge.setText(checked ? "● ON" : "○ OFF");
        badge.setTextColor(checked ? activeColor : (isDark ? 0xFFCBD5E1 : 0xFF475569));
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(AndroidUtilities.dp(8));
        gd.setColor(checked ? ((activeColor & 0x00FFFFFF) | (isDark ? 0x33000000 : 0x1A000000)) : (isDark ? 0xFF334155 : 0xFFF1F5F9));
        gd.setStroke(AndroidUtilities.dp(1), checked ? activeColor : (isDark ? 0xFF475569 : 0xFFCBD5E1));
        badge.setBackground(gd);
    }

    private LinearLayout createSectionCard(Context context, boolean isDark) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(AndroidUtilities.dp(16));
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setStroke(AndroidUtilities.dp(1), isDark ? 0xFF334155 : 0xFFE2E8F0);
        card.setBackground(bg);
        card.setClipToOutline(true);
        return card;
    }

    private View createSwitchRow(Context context, int iconRes, int iconColor, String title, String subtitle, TextView statusBadge, Switch sw, boolean isDark, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) {
            row.setBackgroundResource(tv.resourceId);
        }
        row.setClickable(true);
        row.setOnClickListener(onClick);

        FrameLayout iconWrap = new FrameLayout(context);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.RECTANGLE);
        iconBg.setCornerRadius(AndroidUtilities.dp(10));
        iconBg.setColor(iconColor);
        iconWrap.setBackground(iconBg);

        ImageView iconView = new ImageView(context);
        iconView.setImageResource(iconRes);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iconView.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        iconWrap.addView(iconView, LayoutHelper.createFrame(20, 20, Gravity.CENTER));
        row.addView(iconWrap, LayoutHelper.createLinear(36, 36, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        texts.addView(titleView);

        if (!TextUtils.isEmpty(subtitle)) {
            TextView subView = new TextView(context);
            subView.setText(subtitle);
            subView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
            subView.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF334155);
            subView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
            texts.addView(subView);
        }
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (statusBadge != null) {
            row.addView(statusBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 6, 0, 8, 0));
        }
        row.addView(sw, LayoutHelper.createLinear(37, 22, Gravity.CENTER_VERTICAL, 0, 0, 0, 0));
        return row;
    }

    private View createHairlineDivider(Context context, boolean isDark) {
        View d = new View(context);
        d.setBackgroundColor(isDark ? 0xFF334155 : 0xFFE2E8F0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.leftMargin = AndroidUtilities.dp(16);
        lp.rightMargin = AndroidUtilities.dp(16);
        d.setLayoutParams(lp);
        return d;
    }

    private View buildFormattingToolbar(Context context, boolean isDark) {
        HorizontalScrollView hsv = new HorizontalScrollView(context);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        hsv.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(8));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        hsv.addView(row);

        row.addView(createFormatChip(context, "B Bold", 0xFF1D4ED8, isDark, v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedBold(); })));
        row.addView(createFormatChip(context, "I Italic", 0xFF6366F1, isDark, v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedItalic(); })));
        row.addView(createFormatChip(context, "</> Mono", 0xFF0284C7, isDark, v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedMono(); })));
        row.addView(createFormatChip(context, "✦ Spoiler", 0xFF8B5CF6, isDark, v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedSpoiler(); })));
        row.addView(createFormatChip(context, "❝ Quote", 0xFF4F46E5, isDark, v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedQuote(); })));
        row.addView(createFormatChip(context, "S̶ Strike", 0xFF0EA5E9, isDark, v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedStrike(); })));
        row.addView(createFormatChip(context, "U̲ Under", 0xFF06B6D4, isDark, v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedUnderline(); })));
        row.addView(createFormatChip(context, "🔗 Link", 0xFF10B981, isDark, v -> {
            if (autoAnswerEditText == null) return;
            showCreateOrEditLinkDialog(context, autoAnswerEditText);
        }));
        row.addView(createFormatChip(context, "Aa Style", 0xFFEC4899, isDark, v -> org.nayagram.platform.textstyle.NGTextStyleHelper.showDialog(context)));
        row.addView(createFormatChip(context, "✕ Clear", 0xFF64748B, isDark, v -> applyFormat(et -> {
            selectWordOrAll(et);
            et.makeSelectedRegular();
            Editable ed = et.getText();
            if (ed != null) {
                int s = Math.max(0, et.getSelectionStart());
                int e = Math.max(s, et.getSelectionEnd());
                URLSpanReplacement[] urls = ed.getSpans(s, e, URLSpanReplacement.class);
                if (urls != null) {
                    for (URLSpanReplacement u : urls) ed.removeSpan(u);
                }
            }
        })));
        return hsv;
    }

    private interface FormatAction {
        void run(EditTextCaption et);
    }

    private void applyFormat(FormatAction action) {
        if (autoAnswerEditText == null) return;
        suppressTextWatcher = true;
        try {
            action.run(autoAnswerEditText);
        } finally {
            suppressTextWatcher = false;
        }
        autoAnswerEditText.invalidate();
        saveCurrentEditor();
        if (getParentActivity() != null) {
            refreshPreviewBubble(getParentActivity());
        }
    }

    private TextView createFormatChip(Context context, String label, int accentColor, boolean isDark, View.OnClickListener onClick) {
        TextView btn = new TextView(context);
        btn.setText(label);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        btn.setTypeface(AndroidUtilities.bold());
        btn.setTextColor(isDark ? Color.WHITE : accentColor);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(AndroidUtilities.dp(11), AndroidUtilities.dp(7), AndroidUtilities.dp(11), AndroidUtilities.dp(7));
        btn.setFocusable(false);
        btn.setFocusableInTouchMode(false);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor((accentColor & 0x00FFFFFF) | (isDark ? 0x33000000 : 0x16000000));
        gd.setCornerRadius(AndroidUtilities.dp(10));
        gd.setStroke(AndroidUtilities.dp(1.2f), (accentColor & 0x00FFFFFF) | (isDark ? 0x99000000 : 0x55000000));
        btn.setBackground(gd);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = AndroidUtilities.dp(7);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(onClick);
        return btn;
    }

    private void selectWordOrAll(EditTextCaption et) {
        if (et == null) return;
        Editable text = et.getText();
        if (text == null || text.length() == 0) return;
        int start = et.getSelectionStart();
        int end = et.getSelectionEnd();
        if (start != end && start >= 0 && end >= 0) return;
        int len = text.length();
        int wStart = Math.min(Math.max(0, start), len);
        int wEnd = wStart;
        while (wStart > 0 && !Character.isWhitespace(text.charAt(wStart - 1))) {
            wStart--;
        }
        while (wEnd < len && !Character.isWhitespace(text.charAt(wEnd))) {
            wEnd++;
        }
        if (wStart < wEnd) {
            et.setSelection(wStart, wEnd);
        } else {
            et.setSelection(0, len);
        }
    }

    private void showCreateOrEditLinkDialog(Context context, EditTextCaption targetEditText) {
        if (context == null || targetEditText == null) return;
        final boolean isDark = Theme.isCurrentThemeDark();
        final Editable editable = targetEditText.getText();
        final int rawStart = targetEditText.getSelectionStart();
        final int rawEnd = targetEditText.getSelectionEnd();
        final int selStart = editable != null ? Math.max(0, Math.min(Math.min(rawStart, rawEnd), editable.length())) : 0;
        final int selEnd = editable != null ? Math.max(selStart, Math.min(Math.max(rawStart, rawEnd), editable.length())) : 0;

        String selectedText = (editable != null && selEnd > selStart)
                ? editable.subSequence(selStart, selEnd).toString()
                : "";

        String existingUrl = "https://";
        if (editable != null && selEnd > selStart) {
            URLSpanReplacement[] existingSpans = editable.getSpans(selStart, selEnd, URLSpanReplacement.class);
            if (existingSpans != null && existingSpans.length > 0 && !TextUtils.isEmpty(existingSpans[0].getURL())) {
                existingUrl = existingSpans[0].getURL();
            }
        }

        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle(LocaleController.getString(R.string.CreateLink));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));

        TextView titleLabel = new TextView(context);
        titleLabel.setText("Display Text");
        titleLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        titleLabel.setTypeface(AndroidUtilities.bold());
        titleLabel.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1E293B);
        layout.addView(titleLabel);

        EditText titleInput = new EditText(context);
        titleInput.setHint("e.g. Visit Website / Contact Support");
        titleInput.setText(selectedText);
        titleInput.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
        titleInput.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleInput.setSingleLine(true);
        layout.addView(titleInput);

        TextView urlLabel = new TextView(context);
        urlLabel.setText("URL (Hyperlink)");
        urlLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        urlLabel.setTypeface(AndroidUtilities.bold());
        urlLabel.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1E293B);
        urlLabel.setPadding(0, AndroidUtilities.dp(12), 0, 0);
        layout.addView(urlLabel);

        EditText urlInput = new EditText(context);
        urlInput.setHint("https://t.me/...");
        urlInput.setText(existingUrl);
        urlInput.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
        urlInput.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        urlInput.setSingleLine(true);
        urlInput.setSelection(urlInput.getText().length());
        layout.addView(urlInput);

        b.setView(layout);
        b.setPositiveButton(LocaleController.getString(R.string.OK), (d, w) -> {
            String title = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
            String url = urlInput.getText() != null ? urlInput.getText().toString().trim() : "";
            if (url.isEmpty() || url.equalsIgnoreCase("https://") || url.equalsIgnoreCase("http://")) {
                BulletinFactory.of(NGAutoReplyActivity.this)
                        .createSimpleBulletin(R.raw.error, "Please enter a valid URL")
                        .show();
                return;
            }
            if (!url.contains("://") && !url.startsWith("mailto:") && !url.startsWith("tg:")) {
                url = "https://" + url;
            }
            if (title.isEmpty()) {
                title = url;
            }

            Editable targetEditable = targetEditText.getText();
            if (targetEditable != null) {
                suppressTextWatcher = true;
                try {
                    int safeStart = Math.max(0, Math.min(selStart, targetEditable.length()));
                    int safeEnd = Math.max(safeStart, Math.min(selEnd, targetEditable.length()));
                    if (safeEnd > safeStart) {
                        targetEditable.replace(safeStart, safeEnd, title);
                    } else {
                        targetEditable.insert(safeStart, title);
                    }
                    int linkEnd = safeStart + title.length();
                    AutoReplyManager.applyVisualLinkSpan(targetEditable, safeStart, linkEnd, url);
                    targetEditText.setSelection(linkEnd);
                } finally {
                    suppressTextWatcher = false;
                }
                targetEditText.invalidate();
                saveCurrentEditor();
                refreshPreviewBubble(context);
                BulletinFactory.of(NGAutoReplyActivity.this)
                        .createSimpleBulletin(R.raw.done, "Hyperlink saved: " + title)
                        .show();
            }
        });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(b.create());
    }

    private void saveCurrentEditor() {
        if (autoAnswerEditText == null) return;
        Editable s = autoAnswerEditText.getText();
        String plain = s != null ? s.toString() : "";
        String json = AutoReplyManager.serializeSpansToJson(s);
        AutoReplyManager.getInstance().setAutoAnswerText(plain, json);
    }

    @Override
    public int getNavigationBarColor() {
        return Theme.getColor(Theme.key_windowBackgroundGray);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveCurrentEditor();
        stopAudioPlayback();
        if (isRecordingVoice) {
            stopVoiceRecording(false);
        }
    }

    @Override
    public void onFragmentDestroy() {
        stopAudioPlayback();
        if (isRecordingVoice) {
            stopVoiceRecording(false);
        }
        super.onFragmentDestroy();
    }

    public static void convertLegacyMarkdownToSpans(Editable editable) {
        if (editable == null || editable.length() == 0) return;
        try {
            CharSequence[] arr = new CharSequence[]{editable.toString()};
            ArrayList<TLRPC.MessageEntity> entities = MediaDataController.getInstance(UserConfig.selectedAccount).getEntities(arr, true);
            if (entities != null && !entities.isEmpty() && arr[0] != null) {
                editable.replace(0, editable.length(), arr[0]);
                MediaDataController.addTextStyleRuns(entities, arr[0], editable);
                for (TLRPC.MessageEntity entity : entities) {
                    if (entity instanceof TLRPC.TL_messageEntityTextUrl) {
                        AutoReplyManager.applyVisualLinkSpan(editable, entity.offset, entity.offset + entity.length, entity.url);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
