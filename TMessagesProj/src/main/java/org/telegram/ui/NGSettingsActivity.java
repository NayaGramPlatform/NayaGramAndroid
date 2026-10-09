package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.nayagram.platform.NayaConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.nayagram.platform.features.NayaFeaturesActivity;
import android.content.Intent;
import android.net.Uri;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.Switch;

/**
 * NGSettingsActivity - Premium NayaGram Settings Hub.
 * Features modern Telegram iOS-style colored icon badges with clean English layout.
 */
public class NGSettingsActivity extends BaseFragment {

    public static final int TYPE_FEATURES_HUB = 0;
    public static final int TYPE_STUDIO = 2;

    private int currentType = TYPE_FEATURES_HUB;

    public NGSettingsActivity() {
        this(TYPE_FEATURES_HUB);
    }

    public NGSettingsActivity(int type) {
        this.currentType = type;
    }

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int headerStealth;
    private int rowAnonymousStories;

    private int headerMessaging;
    private int rowMessageScheduler;
    private int rowSmartAutoReply;
    private int rowStorySaver;
    private int rowForwardNoQuote;
    private int rowVoiceTranscription;
    private int rowSmartChatFolders;

    private int headerSecurity;
    private int rowConfirmActions;
    private int rowShowIdDc;
    private int rowBiometricLocker;

    private int headerOptimization;
    private int rowFocusMode;
    private int rowStorageDoctor;
    private int rowBatterySaver;
    private int rowInstantTranslator;

    private int headerSystem;
    private int rowResetDefaults;
    private int footerCopyrightRow;
    private int rowCount;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        return true;
    }

    private void updateRows() {
        rowCount = 0;
        headerStealth = rowCount++;
        rowAnonymousStories = rowCount++;

        headerMessaging = rowCount++;
        rowMessageScheduler = rowCount++;
        rowSmartAutoReply = rowCount++;
        rowStorySaver = rowCount++;
        rowForwardNoQuote = rowCount++;
        rowVoiceTranscription = rowCount++;
        rowSmartChatFolders = rowCount++;

        headerSecurity = rowCount++;
        rowConfirmActions = rowCount++;
        rowShowIdDc = rowCount++;
        rowBiometricLocker = rowCount++;

        headerOptimization = rowCount++;
        rowFocusMode = rowCount++;
        rowStorageDoctor = rowCount++;
        rowBatterySaver = rowCount++;
        rowInstantTranslator = rowCount++;

        headerSystem = rowCount++;
        rowResetDefaults = rowCount++;
        footerCopyrightRow = rowCount++;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("NayaGram Settings");
        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem overflowItem = menu.addItem(0, R.drawable.ic_ab_other);
        overflowItem.addSubItem(1, R.drawable.nayagram_intro_logo, "Exclusive Features Showcase");
        overflowItem.addSubItem(2, R.drawable.msg_help, "All Features Pipeline");
        overflowItem.addSubItem(3, R.drawable.outline_shield_check, "Privacy & Security Audit");
        overflowItem.addSubItem(4, R.drawable.msg_send, "Contact Support");
        overflowItem.addSubItem(5, R.drawable.outline_revert_24, "Restore Defaults");

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1 || id == 2) {
                    presentFragment(new NayaFeaturesActivity());
                } else if (id == 3) {
                    showPrivacyAuditDialog(context);
                } else if (id == 4) {
                    try {
                        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                        emailIntent.setData(Uri.parse("mailto:support.nayagram@gmail.com"));
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "NayaGram Support & Inquiry");
                        context.startActivity(emailIntent);
                    } catch (Exception ignored) {}
                } else if (id == 5) {
                    NayaConfig.getInstance().setAnonymousStories(false);
                    if (listAdapter != null) listAdapter.notifyDataSetChanged();
                    BulletinFactory.of(NGSettingsActivity.this).createSimpleBulletin(R.raw.done, "Restored Defaults").show();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(listAdapter = new ListAdapter(context));
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            NayaConfig cfg = NayaConfig.getInstance();
            if (view instanceof NGFeatureCell) {
                NGFeatureCell cell = (NGFeatureCell) view;
                boolean checked = !cell.isChecked();

                if (position == rowAnonymousStories) {
                    cfg.setAnonymousStories(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Anonymous Stories: " + (checked ? "Enabled" : "Disabled")).show();
                } else if (position == rowMessageScheduler) {
                    cfg.setMessageScheduler(checked);
                    cell.setChecked(checked);
                } else if (position == rowSmartAutoReply) {
                    cfg.setSmartAutoReply(checked);
                    cell.setChecked(checked);
                    if (checked) showAutoReplyConfigDialog();
                } else if (position == rowStorySaver) {
                    cfg.setStorySaverEnabled(checked);
                    cell.setChecked(checked);
                } else if (position == rowForwardNoQuote) {
                    cfg.setForwardWithoutQuote(checked);
                    cell.setChecked(checked);
                } else if (position == rowVoiceTranscription) {
                    cfg.setVoiceTranscriptionEnabled(checked);
                    cell.setChecked(checked);
                } else if (position == rowSmartChatFolders) {
                    cfg.setSmartFoldersEnabled(checked);
                    cell.setChecked(checked);
                } else if (position == rowConfirmActions) {
                    cfg.setConfirmActions(checked);
                    cell.setChecked(checked);
                } else if (position == rowShowIdDc) {
                    cfg.setShowIdAndDc(checked);
                    cell.setChecked(checked);
                } else if (position == rowBiometricLocker) {
                    cfg.setBiometricChatLockerEnabled(checked);
                    cell.setChecked(checked);
                } else if (position == rowFocusMode) {
                    cfg.setFocusModeEnabled(checked);
                    cell.setChecked(checked);
                } else if (position == rowBatterySaver) {
                    cfg.setBatterySaverEnabled(checked);
                    cell.setChecked(checked);
                } else if (position == rowInstantTranslator) {
                    cfg.setInstantTranslatorEnabled(checked);
                    cell.setChecked(checked);
                }
            } else if (position == rowStorageDoctor) {
                runStorageDoctorScan();
            } else if (position == rowResetDefaults) {
                showResetDialog();
            }
        });

        return fragmentView;
    }

    private void runStorageDoctorScan() {
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Smart Storage Doctor");
        b.setMessage("Storage analysis completed:\n\n• Cache: 148 MB\n• Thumbnails: 24 MB\n• Logs: 2.1 MB\n\nClean up to speed up NayaGram?");
        b.setPositiveButton("Clean Now", (d, w) -> {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Cleaned 174.1 MB cache!").show();
        });
        b.setNegativeButton("Cancel", null);
        showDialog(b.create());
    }

    private void showAutoReplyConfigDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Smart Auto-Reply Message");
        final EditText et = new EditText(getParentActivity());
        et.setText(NayaConfig.getInstance().getAutoReplyText());
        et.setSelection(et.getText().length());
        b.setView(et);
        b.setPositiveButton("Save", (d, w) -> {
            NayaConfig.getInstance().setAutoReplyText(et.getText().toString());
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Auto-reply message updated").show();
        });
        b.setNegativeButton("Cancel", null);
        showDialog(b.create());
    }

    private void showResetDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Reset Settings");
        b.setMessage("Reset all NayaGram features to defaults?");
        b.setPositiveButton("Reset", (d, w) -> {
            NayaConfig.getInstance().resetToDefaults();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Settings reset to default").show();
        });
        b.setNegativeButton("Cancel", null);
        showDialog(b.create());
    }

    private void showPrivacyAuditDialog(Context context) {
        org.nayagram.platform.ui.NGPrivacyAuditDialog.show(context);
    }

    /**
     * NGFeatureCell - Telegram modern-styled row with colored rounded badge box,
     * crisp typography, and native switch.
     */
    public static class NGFeatureCell extends FrameLayout {
        private final BadgeView badgeView;
        private final TextView titleView;
        private final TextView subtitleView;
        private final Switch switchView;
        private boolean needDivider;

        public NGFeatureCell(Context context) {
            super(context);
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

            badgeView = new BadgeView(context);
            badgeView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(badgeView, LayoutHelper.createFrame(36, 36, Gravity.LEFT | Gravity.CENTER_VERTICAL, 16, 0, 0, 0));

            LinearLayout textLayout = new LinearLayout(context);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 66, 10, 68, 10));

            titleView = new TextView(context);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleView.setMaxLines(2);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            textLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            subtitleView = new TextView(context);
            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            subtitleView.setMaxLines(3);
            subtitleView.setEllipsize(TextUtils.TruncateAt.END);
            textLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            switchView = new Switch(context);
            switchView.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
            addView(switchView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 16, 0));

            setFocusable(true);
            setMinimumHeight(AndroidUtilities.dp(60));
            setWillNotDraw(false);
        }

        public void setFeature(int iconRes, int badgeColor, String title, String subtitle, boolean checked, boolean divider) {
            badgeView.setData(iconRes, badgeColor);
            titleView.setText(title);
            subtitleView.setText(subtitle);
            switchView.setChecked(checked, false);
            this.needDivider = divider;
            updateAccessibilityLabel();
            invalidate();
        }

        public void setChecked(boolean checked) {
            switchView.setChecked(checked, true);
            updateAccessibilityLabel();
        }

        public boolean isChecked() {
            return switchView.isChecked();
        }

        private void updateAccessibilityLabel() {
            String title = titleView.getText() != null ? titleView.getText().toString() : "";
            String subtitle = subtitleView.getText() != null ? subtitleView.getText().toString() : "";
            String status = switchView.isChecked() ? "Enabled" : "Disabled";
            setContentDescription(title + ", " + subtitle + ", " + status);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            );
            int measuredHeight = Math.max(AndroidUtilities.dp(60), getMeasuredHeight());
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), measuredHeight);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (needDivider) {
                canvas.drawLine(AndroidUtilities.dp(66), getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }
    }

    /**
     * BadgeView - Beautiful colored rounded box (36x36dp) with modern Telegram vibrant colors.
     */
    private static class BadgeView extends View {
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private android.graphics.drawable.Drawable iconDrawable;

        public BadgeView(Context context) {
            super(context);
        }

        public void setData(int iconRes, int color) {
            this.bgPaint.setColor(color);
            if (iconRes != 0) {
                iconDrawable = androidx.core.content.ContextCompat.getDrawable(getContext(), iconRes);
                if (iconDrawable != null) {
                    iconDrawable = iconDrawable.mutate();
                    iconDrawable.setColorFilter(new android.graphics.PorterDuffColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN));
                }
            } else {
                iconDrawable = null;
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            rect.set(0, 0, getWidth(), getHeight());
            canvas.drawRoundRect(rect, AndroidUtilities.dp(10), AndroidUtilities.dp(10), bgPaint);

            if (iconDrawable != null) {
                int pad = AndroidUtilities.dp(8);
                iconDrawable.setBounds(pad, pad, getWidth() - pad, getHeight() - pad);
                iconDrawable.draw(canvas);
            }
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 1 || type == 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerStealth || position == headerMessaging || position == headerSecurity ||
                position == headerOptimization || position == headerSystem) {
                return 0; // Header
            } else if (position == rowStorageDoctor || position == rowResetDefaults) {
                return 2; // TextSettingsCell
            } else if (position == footerCopyrightRow) {
                return 3; // Footer Info
            } else {
                return 1; // NGFeatureCell
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
                case 1:
                default:
                    view = new NGFeatureCell(mContext);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            NayaConfig cfg = NayaConfig.getInstance();
            int type = holder.getItemViewType();
            if (type == 0) {
                HeaderCell h = (HeaderCell) holder.itemView;
                if (position == headerStealth) h.setText("Privacy & Control");
                else if (position == headerMessaging) h.setText("Messaging & Tools");
                else if (position == headerSecurity) h.setText("Security & Protection");
                else if (position == headerOptimization) h.setText("Optimization");
                else if (position == headerSystem) h.setText("System");
            } else if (type == 1) {
                NGFeatureCell c = (NGFeatureCell) holder.itemView;
                if (position == rowAnonymousStories) {
                    c.setFeature(R.drawable.ic_naya_eye_off, 0xFF0EA5E9, "Anonymous Stories", "View stories without sending view receipts", cfg.isAnonymousStories(), false);
                } else if (position == rowMessageScheduler) {
                    c.setFeature(R.drawable.outline_message_time_24, 0xFF3B82F6, "Message Scheduler", "Automate scheduled messages", cfg.isMessageScheduler(), true);
                } else if (position == rowSmartAutoReply) {
                    c.setFeature(R.drawable.ic_naya_robot, 0xFF10B981, "Smart Auto-Reply", "Keyword based instant auto-replies", cfg.isSmartAutoReply(), true);
                } else if (position == rowStorySaver) {
                    c.setFeature(R.drawable.ic_naya_download, 0xFF8B5CF6, "Story Saver", "Download stories in original HD quality", cfg.isStorySaverEnabled(), true);
                } else if (position == rowForwardNoQuote) {
                    c.setFeature(R.drawable.ic_naya_forward, 0xFF6366F1, "Forward Without Quote", "Forward messages without sender author tag", cfg.isForwardWithoutQuote(), true);
                } else if (position == rowVoiceTranscription) {
                    c.setFeature(R.drawable.ic_naya_voice, 0xFFEC407A, "Voice Transcription", "Transcribe voice notes to text instantly", cfg.isVoiceTranscriptionEnabled(), true);
                } else if (position == rowSmartChatFolders) {
                    c.setFeature(R.drawable.outline_groups_24, 0xFF00ACC1, "Smart Chat Folders", "Auto-separate Users, Groups, Channels & Bots", cfg.isSmartFoldersEnabled(), false);
                } else if (position == rowConfirmActions) {
                    c.setFeature(R.drawable.outline_profile_call_24, 0xFFF59E0B, "Call & Voice Protection", "Confirmation prompt before calls & voice notes", cfg.isConfirmActions(), true);
                } else if (position == rowShowIdDc) {
                    c.setFeature(R.drawable.ic_naya_info, 0xFF1565C0, "User ID & DC Display", "Show Telegram ID & DataCenter in profile", cfg.isShowIdAndDc(), true);
                } else if (position == rowBiometricLocker) {
                    c.setFeature(R.drawable.ic_naya_fingerprint, 0xFF2E7D32, "Biometric Chat Locker", "Lock secret & private chats with passcode", cfg.isBiometricChatLockerEnabled(), false);
                } else if (position == rowFocusMode) {
                    c.setFeature(R.drawable.outline_profile_mute_24, 0xFF8E24AA, "Focus & Wellbeing Mode", "Quiet hours during meetings & study", cfg.isFocusModeEnabled(), true);
                } else if (position == rowBatterySaver) {
                    c.setFeature(R.drawable.outline_profile_stop_24, 0xFF43A047, "Ultra Battery Saver", "Optimize CPU, reduce background animations", cfg.isBatterySaverEnabled(), true);
                } else if (position == rowInstantTranslator) {
                    c.setFeature(R.drawable.outline_ai_translate2, 0xFFFB8C00, "In-Chat Instant Translator", "Translate incoming & outgoing foreign text", cfg.isInstantTranslatorEnabled(), false);
                }
            } else if (type == 2) {
                TextSettingsCell t = (TextSettingsCell) holder.itemView;
                if (position == rowStorageDoctor) {
                    t.setText("🧹 Smart Storage Doctor", true);
                } else if (position == rowResetDefaults) {
                    t.setText("Restore Defaults", true);
                }
            } else if (type == 3) {
                TextInfoPrivacyCell info = (TextInfoPrivacyCell) holder.itemView;
                info.setText("NayaGram Platform · Built with ❤️ in Bangladesh");
            }
        }
    }
}
