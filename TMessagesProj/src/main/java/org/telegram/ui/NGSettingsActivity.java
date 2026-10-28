package org.telegram.ui;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import org.telegram.messenger.NotificationCenter;

import org.nayagram.platform.ui.NayaAboutActivity;

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
    private int rowVerifiedBadge;
    private int rowChannelVerifiedBadge;
    private int rowAnonymousStories;
    private int rowGhostMode = -1;
    private int rowApkBlock = -1;

    private int headerMessaging;
    private int rowOneTimeVoiceVideo;
    private int rowAutoAddText;
    private int rowEditText;
    private int rowMessageScheduler;
    private int rowSmartAutoReply;
    private int rowStorySaver;
    private int rowSmartChatFolders;

    private int headerSecurity;
    private int rowShowIdDc;
    private int rowHidePhoneNumber;
    private int rowThemeMode;
    private int rowBiometricLocker;

    private int headerOptimization;
    private int rowFocusMode;
    private int rowStorageDoctor;
    private int rowBatterySaver;
    private int rowInstantTranslator;

    private int headerInternational;
    private int rowIntlClock;
    private int rowLinkGuard;
    private int rowReadableText;
    private int rowReduceMotion;
    private int rowChatListDetails;
    private int rowPauseGifs;
    private int rowSaveData;

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
        rowVerifiedBadge = rowCount++;
        rowChannelVerifiedBadge = rowCount++;
        rowAnonymousStories = -1;
        rowGhostMode = -1;
        rowApkBlock = -1;

        headerMessaging = rowCount++;
        rowOneTimeVoiceVideo = rowCount++;
        rowAutoAddText = rowCount++;
        rowEditText = rowCount++;
        rowMessageScheduler = -1;
        rowSmartAutoReply = -1;
        rowStorySaver = -1;
        rowSmartChatFolders = rowCount++;

        headerSecurity = rowCount++;
        rowShowIdDc = rowCount++;
        rowHidePhoneNumber = rowCount++;
        rowThemeMode = rowCount++;
        rowBiometricLocker = rowCount++;
        // Hidden until the APK/ZIP protection behavior is implemented and verified.
        rowApkBlock = -1;

        // Hide inactive feature placeholders instead of showing non-working controls.
        headerOptimization = -1;
        rowFocusMode = -1;
        rowStorageDoctor = -1;
        rowBatterySaver = -1;
        rowInstantTranslator = -1;

        headerInternational = rowCount++;
        rowIntlClock = rowCount++;
        // These rows were marked for removal from this feature UI.
        rowLinkGuard = -1;
        rowReadableText = -1;
        rowReduceMotion = -1;
        rowChatListDetails = -1;
        rowPauseGifs = -1;
        rowSaveData = -1;

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
        overflowItem.setIconColor(0xFF00D4FF);
        overflowItem.addSubItem(1, R.drawable.nayagram_intro_logo, "Exclusive Features Showcase").setIconColor(0xFF7C3AED);
        overflowItem.addSubItem(3, R.drawable.outline_shield_check, "Privacy & Security Audit").setIconColor(0xFF059669);
        overflowItem.addSubItem(4, R.drawable.msg_send, "Contact Support").setIconColor(0xFF2563EB);
        overflowItem.addSubItem(7, R.drawable.msg_admins, "Leadership & Admin").setIconColor(0xFF7C3AED);
        overflowItem.addSubItem(6, R.drawable.ic_naya_info, "About NayaGram").setIconColor(0xFF0284C7);
        overflowItem.addSubItem(5, R.drawable.outline_revert_24, "Restore Defaults").setIconColor(0xFFDB2777);

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
                } else if (id == 7 || id == 8) {
                    presentFragment(new org.nayagram.platform.ui.NGAdminActivity());
                } else if (id == 6) {
                    presentFragment(new NayaAboutActivity());
                } else if (id == 5) {
                    NayaConfig.getInstance().resetToDefaults();
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
                if (isPendingFeaturePosition(position)) {
                    showFeatureDetailSheet(position);
                    return;
                }
                boolean checked = !cell.isChecked();

                if (position == rowVerifiedBadge) {
                    cfg.setVerifiedBadgeEnabled(checked);
                    cell.setChecked(checked);
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_NAME);
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface);
                    BulletinFactory.of(this).createSimpleBulletin(
                        R.drawable.outline_shield_check,
                        "User Profile Verified Badge: " + (checked ? "Enabled" : "Disabled")
                    ).show();
                } else if (position == rowChannelVerifiedBadge) {
                    cfg.setChannelVerifiedBadgeEnabled(checked);
                    cell.setChecked(checked);
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_NAME);
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface);
                    BulletinFactory.of(this).createSimpleBulletin(
                        R.drawable.msg_channel,
                        "Channel & Group Verified Badge: " + (checked ? "Enabled" : "Disabled")
                    ).show();
                } else if (position == rowAnonymousStories) {
                    cfg.setAnonymousStories(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Anonymous Stories: " + (checked ? "Enabled" : "Disabled")).show();
                } else if (position == rowGhostMode) {
                    cell.setChecked(false);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Ghost Mode is hidden").show();
                } else if (position == rowApkBlock) {
                    cfg.setApkBlockEnabled(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "APK & ZIP Security Shield: " + (checked ? "Enabled" : "Disabled")).show();
                } else if (position == rowMessageScheduler) {
                    cfg.setMessageScheduler(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Message Scheduler: " + (checked ? "On" : "Off")).show();
                } else if (position == rowSmartAutoReply) {
                    cfg.setSmartAutoReply(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Smart Auto-Reply: " + (checked ? "On" : "Off")).show();
                    if (checked) showAutoReplyConfigDialog();
                } else if (position == rowOneTimeVoiceVideo) {
                    cfg.setOneTimeVoiceVideoEnabled(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "One-time voice & video: " + (checked ? "On" : "Off")).show();
                } else if (position == rowAutoAddText) {
                    cfg.setAutoAddTextEnabled(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Auto-add text: " + (checked ? "On" : "Off")).show();
                    if (checked) showAutoAddTextConfigDialog();
                } else if (position == rowEditText) {
                    showAutoAddTextConfigDialog();
                } else if (position == rowStorySaver) {
                    cfg.setStorySaverEnabled(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Story Saver: " + (checked ? "On" : "Off")).show();

                } else if (position == rowSmartChatFolders) {
                    // Use Telegram's existing folder editor instead of a preference-only fake toggle.
                    cfg.setSmartFoldersEnabled(true);
                    cell.setChecked(true);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Opening native chat folder management").show();
                    presentFragment(new FiltersSetupActivity());

                } else if (position == rowHidePhoneNumber) {
                    cfg.setHidePhoneNumberEnabled(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, checked ? "Phone Privacy Enabled" : "Phone Privacy Disabled").show();
                } else if (position == rowThemeMode) {
                    int nextMode = (cfg.getAppThemeMode() + 1) % 3;
                    cfg.setAppThemeMode(nextMode);
                    try {
                        if (nextMode == NayaConfig.THEME_MODE_LIGHT) {
                            Theme.applyTheme(Theme.getCurrentTheme());
                        } else {
                            Theme.applyTheme(Theme.getCurrentNightTheme(), true);
                        }
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme);
                    } catch (Throwable ignored) {}
                    if (listAdapter != null) listAdapter.notifyDataSetChanged();
                    String modeName = nextMode == NayaConfig.THEME_MODE_LIGHT ? "Light Slate" : (nextMode == NayaConfig.THEME_MODE_NEON_AURORA ? "Naya Neon Aurora (Cyan-Purple)" : "Dark Slate Blue");
                    BulletinFactory.of(NGSettingsActivity.this).createSimpleBulletin(R.raw.done, "Theme: " + modeName).show();
                } else if (position == rowShowIdDc) {
                    cfg.setShowIdAndDc(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "User ID & DC: " + (checked ? "On" : "Off")).show();
                } else if (position == rowBiometricLocker) {
                    if (checked) {
                        if (!org.nayagram.platform.security.BiometricChatLocker.isDeviceSecurityAvailable(getParentActivity())) {
                            AlertDialog.Builder securityDialog = new AlertDialog.Builder(getParentActivity());
                            securityDialog.setTitle("Secure your device first");
                            securityDialog.setMessage("Set a PIN, password, or pattern in Android Security Settings before enabling Biometric Chat Locker. Face or fingerprint unlock can then be used when supported by your device.");
                            securityDialog.setNegativeButton("Cancel", null);
                            securityDialog.setPositiveButton("Open Settings", (dialog, which) -> {
                                try {
                                    getParentActivity().startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS));
                                } catch (Exception ignored) {
                                    android.widget.Toast.makeText(getParentActivity(), "Open Android Settings and set a secure screen lock", android.widget.Toast.LENGTH_LONG).show();
                                }
                            });
                            showDialog(securityDialog.create());
                            return;
                        }
                        cfg.setBiometricChatLockerEnabled(true);
                        cell.setChecked(true);
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.done,
                                "Locker enabled. Open a chat's NayaGram In-Chat Settings to protect it.").show();
                    } else {
                        AlertDialog.Builder disableDialog = new AlertDialog.Builder(getParentActivity());
                        disableDialog.setTitle("Turn off Biometric Chat Locker?");
                        disableDialog.setMessage("You will not be able to protect additional chats. Existing protected chats stay locked until you open each chat's NayaGram In-Chat Settings and unlock it. Continue?");
                        disableDialog.setNegativeButton("Keep enabled", null);
                        disableDialog.setPositiveButton("Turn off", (dialog, which) -> {
                            cfg.setBiometricChatLockerEnabled(false);
                            cell.setChecked(false);
                            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Biometric Chat Locker disabled for new locks").show();
                        });
                        showDialog(disableDialog.create());
                    }
                } else if (position == rowFocusMode) {
                    cfg.setFocusModeEnabled(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Focus Mode: " + (checked ? "On" : "Off")).show();
                } else if (position == rowBatterySaver) {
                    cfg.setBatterySaverEnabled(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Battery Saver: " + (checked ? "On" : "Off")).show();
                } else if (position == rowInstantTranslator) {
                    cfg.setInstantTranslatorEnabled(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Instant Translator: " + (checked ? "On" : "Off")).show();
                } else if (position == rowIntlClock) {
                    cfg.setInternationalClock(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "24-hour clock: " + (checked ? "On" : "Off")).show();
                } else if (position == rowLinkGuard) {
                    cfg.setLinkGuardEnabled(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Link check: " + (checked ? "On" : "Off")).show();
                } else if (position == rowReadableText) {
                    cfg.setReadableTextEnabled(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Larger text: " + (checked ? "On" : "Off")).show();
                } else if (position == rowReduceMotion) {
                    cfg.setReduceMotion(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Reduce motion: " + (checked ? "On" : "Off")).show();
                } else if (position == rowChatListDetails) {
                    cfg.setChatListDetails(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Chat list details: " + (checked ? "On" : "Off")).show();
                } else if (position == rowPauseGifs) {
                    cfg.setPauseGifAutoplay(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Pause GIFs: " + (checked ? "On" : "Off")).show();
                } else if (position == rowSaveData) {
                    cfg.setDataSaverEnabled(checked);
                    cell.setChecked(checked);
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Save mobile data: " + (checked ? "On" : "Off")).show();
                }
            } else if (position == rowStorageDoctor) {
                runStorageDoctorScan();
            } else if (position == rowResetDefaults) {
                showResetDialog();
            }
        });

        listView.setOnItemLongClickListener((view, position) -> {
            showFeatureDetailSheet(position);
            return true;
        });

        return fragmentView;
    }

    private void runStorageDoctorScan() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("⚡ Smart Storage Doctor");
        b.setMessage("Scanning cache & temporary files...");
        AlertDialog progressDialog = b.create();
        showDialog(progressDialog);

        org.nayagram.platform.storage.SmartStorageDoctor.getInstance().scanStorage(getParentActivity(), report -> {
            try {
                progressDialog.dismiss();
            } catch (Throwable ignored) {
            }

            if (report == null || report.totalReclaimableBytes <= 0) {
                AlertDialog.Builder cleanB = new AlertDialog.Builder(getParentActivity());
                cleanB.setTitle("Storage Clean");
                cleanB.setMessage("Your cache is completely optimized and clean! No unnecessary temporary files found.");
                cleanB.setPositiveButton("OK", null);
                showDialog(cleanB.create());
                return;
            }

            AlertDialog.Builder cleanB = new AlertDialog.Builder(getParentActivity());
            cleanB.setTitle("🧹 Storage Analysis");
            cleanB.setMessage("Found reclaimable cache:\n\n" +
                    "• Total Cache: " + report.getFormattedTotal() + "\n" +
                    "• Cached Files: " + report.fileCount + "\n\n" +
                    "Clean cache now to boost NayaGram speed?");
            cleanB.setPositiveButton("Clean Now", (d, w) -> {
                org.nayagram.platform.storage.SmartStorageDoctor.getInstance().cleanCache(getParentActivity(), (freed, success) -> {
                    BulletinFactory.of(this).createSimpleBulletin(
                            R.raw.done,
                            "Cleaned " + org.nayagram.platform.storage.SmartStorageDoctor.formatSize(freed) + " cache!"
                    ).show();
                });
            });
            cleanB.setNegativeButton("Cancel", null);
            showDialog(cleanB.create());
        });
    }

    private void showAutoAddTextConfigDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Auto-add text");
        b.setMessage("Append your custom text or username to every message you send");
        final EditText et = new EditText(getParentActivity());
        et.setText(NayaConfig.getInstance().getAutoAddText());
        et.setSelection(et.getText().length());
        b.setView(et);
        b.setPositiveButton("Save", (d, w) -> {
            String text = et.getText().toString().trim();
            NayaConfig.getInstance().setAutoAddText(text.isEmpty() ? "@NGSupreme" : text);
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Auto-add text saved").show();
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


    private boolean isPendingFeaturePosition(int position) {
        return position == rowFocusMode
                || position == rowBatterySaver
                || position == rowInstantTranslator;
    }

    private void showFeatureDetailSheet(int position) {
        if (getParentActivity() == null) return;
        String title;
        String body;
        if (position == rowVerifiedBadge) {
            AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
            b.setTitle("Blue Verified Badge");
            b.setMessage("When enabled, your NayaGram account will display the official blue verification checkmark badge next to your name across chats, group messages, and user profile.");
            b.setPositiveButton("Got it", null);
            showDialog(b.create());
            return;
        }
        if (position == rowAnonymousStories) {
            title = "Anonymous Stories";
            body = "Privacy preference for story views.\n\nStory view behavior still follows Telegram server rules. This toggle does not hide you from Telegram if the server requires a view receipt.";
        } else if (position == rowMessageScheduler) {
            title = "Message Scheduler";
            body = "Use Telegram's built-in schedule (long-press Send) to send later.\n\nThis switch stores your preference. It does not replace Telegram's official scheduler.";
        } else if (position == rowSmartAutoReply) {
            title = "Smart Auto-Reply";
            body = "Saves an auto-reply message you can copy and send.\n\nTurning it on opens the message editor. It does not silently message people in the background.";
        } else if (position == rowStorySaver) {
            title = "Story Saver";
            body = "Preference for saving stories you are allowed to view.\n\nDownload only content you have permission to save.";

        } else if (position == rowSmartChatFolders) {
            title = "Smart Chat Folders";
            body = "Opens Telegram's native chat-folder manager so you can create, edit, and organize your own folders using Telegram's existing filter system. Folder membership and chat filtering remain managed by Telegram.";

        } else if (position == rowHidePhoneNumber) {
            title = "Screen Share Phone Privacy";
            body = "Blur and mask your phone number during screen sharing.";
        } else if (position == rowThemeMode) {
            title = "Naya Theme Mode";
            body = "Switch between Dark Slate, Light Slate, and Naya Neon Aurora themes.";
        } else if (position == rowShowIdDc) {
            title = "User ID & DC Display";
            body = "Shows Telegram user ID and data-center hints where the profile screen supports it.";
        } else if (position == rowBiometricLocker) {
            title = "Biometric Chat Locker";
            body = "Protect individual chats with your device's secure screen lock, fingerprint, or face unlock when supported. Enable this feature here, then open the chosen chat's three-dot menu → NayaGram In-Chat Settings → Biometric Chat Lock. The chat is closed immediately and authentication is required before it can be opened again.\n\nA secure PIN, password, or pattern must be configured in Android Settings first. Turning this master switch off prevents new chat locks; existing protected chats remain locked until you unlock them individually. This is a chat-entry lock, not a separate hidden vault.";
        } else if (position == rowFocusMode) {
            title = "Focus & Wellbeing Mode";
            body = "Not active yet. Quiet hours, notification scheduling and exceptions are not connected.\n\nIt does not mute Telegram notifications by itself unless you also change notification settings.";
        } else if (position == rowStorageDoctor) {
            title = "Smart Storage Doctor";
            body = "Opens a storage summary and a cleanup confirmation.\n\nThe numbers are an estimate. Chats and media you sent are not deleted.";
        } else if (position == rowBatterySaver) {
            title = "Ultra Battery Saver";
            body = "Not active yet. Low-battery detection and automatic playback or background-work controls are not connected.\n\nTelegram's own sync still runs so messages arrive.";
        } else if (position == rowInstantTranslator) {
            title = "In-Chat Instant Translator";
            body = "Not active yet. This screen is not connected to a translation service or an in-chat translate action.\n\nIt does not send your chats to a private NayaGram server.";
        } else if (position == rowIntlClock) {
            title = "24-hour clock";
            body = "Shows times as 13:45 instead of 1:45 PM. Useful when you travel or talk to people in other countries.\n\nThis only changes how times look on your phone. Telegram is not contacted.";
        } else if (position == rowLinkGuard) {
            title = "Check unknown links";
            body = "Asks before opening a website that is not Telegram.\n\nTelegram links such as t.me still open normally. You can cancel a suspicious link.";
        } else if (position == rowReadableText) {
            title = "Larger text";
            body = "Raises the chat text size to at least 18. Turning it off restores your previous size.\n\nThis uses Telegram's own text-size setting.";
        } else if (position == rowReduceMotion) {
            title = "Reduce motion";
            body = "Turns off extra interface animations on this phone. Messages, calls, and accounts are unchanged.";
        } else if (position == rowChatListDetails) {
            title = "Chat list details";
            body = "Shows three lines in the chat list so names and last messages are easier to scan.";
        } else if (position == rowPauseGifs) {
            title = "Pause GIFs";
            body = "Stops GIFs from playing by themselves in chats. You can still open them. This uses Telegram's own autoplay setting.";
        } else if (position == rowSaveData) {
            title = "Save mobile data";
            body = "Does not download media until you open it. Useful on limited mobile plans. This uses Telegram's stream-media setting.";
        } else if (position == rowResetDefaults) {
            title = "Restore Defaults";
            body = "Resets NayaGram feature switches to their original values. Your Telegram chats are not deleted.";
        } else {
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(title);
        b.setMessage(body);
        b.setPositiveButton("Got it", null);
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
            addView(badgeView, LayoutHelper.createFrame(36, 36, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, LocaleController.isRTL ? 0 : 16, LocaleController.isRTL ? 16 : 0, 0));

            LinearLayout textLayout = new LinearLayout(context);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, LocaleController.isRTL ? 68 : 66, 10, LocaleController.isRTL ? 66 : 68, 10));

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
            addView(switchView, LayoutHelper.createFrame(37, 20, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 16, 0));

            setFocusable(true);
            setMinimumHeight(AndroidUtilities.dp(60));
            setWillNotDraw(false);
        }

        public void setFeature(int iconRes, int badgeColor, String title, String subtitle, boolean checked, boolean divider) {
            badgeView.setData(iconRes, badgeColor);
            titleView.setText(title);
            subtitleView.setText(subtitle);
            boolean unavailable = title.contains("· SOON");
            switchView.setVisibility(unavailable ? View.GONE : View.VISIBLE);
            switchView.setChecked(unavailable ? false : checked, false);
            this.needDivider = divider;
            updateAccessibilityLabel();
            invalidate();
        }

        public void setActionFeature(int iconRes, int badgeColor, String title, String subtitle, boolean divider) {
            setFeature(iconRes, badgeColor, title, subtitle, false, divider);
            // Action rows navigate to a native manager; they are not on/off preferences.
            switchView.setVisibility(View.GONE);
            updateAccessibilityLabel();
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
            String status = title.contains("· SOON") ? "Unavailable" : (switchView.isChecked() ? "Enabled" : "Disabled");
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
                position == headerOptimization || position == headerInternational || position == headerSystem) {
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
                if (position == headerStealth) h.setText("Account & Verification");
                else if (position == headerMessaging) h.setText("Messaging & Tools");
                else if (position == headerSecurity) h.setText("Security & Protection");
                else if (position == headerOptimization) h.setText("Optimization");
                else if (position == headerInternational) h.setText("International");
                else if (position == headerSystem) h.setText("System");
            } else if (type == 1) {
                NGFeatureCell c = (NGFeatureCell) holder.itemView;
                if (position == rowVerifiedBadge) {
                    c.setFeature(
                        R.drawable.outline_shield_check,
                        0xFF0284C7,
                        "User Profile Verified Badge",
                        "Show blue checkmark badge on user profile and name (Off by default)",
                        cfg.isVerifiedBadgeEnabled(),
                        true
                    );
                } else if (position == rowChannelVerifiedBadge) {
                    c.setFeature(
                        R.drawable.msg_channel,
                        0xFF7C3AED,
                        "Channel & Group Verified Badge",
                        "Show blue checkmark badge on channels and groups (Off by default)",
                        cfg.isChannelVerifiedBadgeEnabled(),
                        true
                    );
                } else if (position == rowAnonymousStories) {
                    c.setFeature(R.drawable.ic_naya_eye_off, 0xFF0EA5E9, "Anonymous Stories", "View stories without sending view receipts", cfg.isAnonymousStories(), false);
                } else if (position == rowGhostMode) {
                    c.setFeature(R.drawable.ic_naya_eye_off, 0xFF64748B, "Ghost Mode", "Experimental. Hidden while SHOW_EXPERIMENTAL is false.", false, false);
                } else if (position == rowApkBlock) {
                    c.setFeature(R.drawable.outline_shield_check, 0xFF00D4FF, "APK & ZIP Security Shield", "Block downloading or receiving malicious files", cfg.isApkBlockEnabled(), true);
                } else if (position == rowHidePhoneNumber) {
                    c.setFeature(R.drawable.ic_naya_eye_off, 0xFF6366F1, "Screen Share Phone Privacy", "Blur & mask personal phone number with blue-purple background and eye toggle", cfg.isHidePhoneNumberEnabled(), true);
                } else if (position == rowThemeMode) {
                    int mode = cfg.getAppThemeMode();
                    String modeName = mode == NayaConfig.THEME_MODE_LIGHT ? "Light Slate" : (mode == NayaConfig.THEME_MODE_NEON_AURORA ? "Naya Neon Aurora (Cyan-Purple)" : "Dark Slate Blue");
                    c.setActionFeature(R.drawable.msg_theme, 0xFF8B5CF6, "Naya Theme Mode", modeName, true);
                } else if (position == rowMessageScheduler) {
                    c.setFeature(R.drawable.outline_message_time_24, 0xFF3B82F6, "Message Scheduler", "Automate scheduled messages", cfg.isMessageScheduler(), true);
                } else if (position == rowSmartAutoReply) {
                    c.setFeature(R.drawable.ic_naya_robot, 0xFF10B981, "Smart Auto-Reply", "Keyword based instant auto-replies", cfg.isSmartAutoReply(), true);
                } else if (position == rowOneTimeVoiceVideo) {
                    c.setFeature(R.drawable.menu_feature_voice, 0xFF0284C7, "One-time voice & video", "Send voice messages and round videos as view-once by default", cfg.isOneTimeVoiceVideoEnabled(), true);
                } else if (position == rowAutoAddText) {
                    c.setFeature(R.drawable.msg_edit, 0xFF8B5CF6, "Auto-add text", "Append your text to every message you send", cfg.isAutoAddTextEnabled(), true);
                } else if (position == rowEditText) {
                    c.setActionFeature(R.drawable.msg_settings_old, 0xFF10B981, "Edit text", cfg.getAutoAddText(), false);
                } else if (position == rowStorySaver) {
                    c.setFeature(R.drawable.ic_naya_download, 0xFF8B5CF6, "Story Saver", "Download stories in original HD quality", cfg.isStorySaverEnabled(), true);
                } else if (position == rowSmartChatFolders) {
                    c.setActionFeature(R.drawable.outline_groups_24, 0xFF10B981, "Smart Chat Folders", "Create and manage Telegram chat folders", false);
                } else if (position == rowShowIdDc) {
                    c.setFeature(R.drawable.ic_naya_info, 0xFF1565C0, "User ID & DC Display", "Show Telegram ID & DataCenter in profile", cfg.isShowIdAndDc(), true);
                } else if (position == rowBiometricLocker) {
                    c.setFeature(R.drawable.ic_naya_fingerprint, 0xFF10B981, "Biometric Chat Locker", "Protect selected chats with fingerprint, face unlock, or device screen lock", cfg.isBiometricChatLockerEnabled(), true);
                } else if (position == rowFocusMode) {
                    c.setFeature(R.drawable.outline_profile_mute_24, 0xFF64748B, "Focus & Wellbeing Mode · SOON", "Not active yet · Notification scheduling is not connected", false, true);
                } else if (position == rowBatterySaver) {
                    c.setFeature(R.drawable.outline_profile_stop_24, 0xFF64748B, "Ultra Battery Saver · SOON", "Not active yet · Battery-aware controls are not connected", false, true);
                } else if (position == rowInstantTranslator) {
                    c.setFeature(R.drawable.outline_ai_translate2, 0xFF64748B, "In-Chat Instant Translator · SOON", "Not active yet · No translation action is connected", false, false);
                } else if (position == rowIntlClock) {
                    c.setFeature(R.drawable.outline_message_time_24, 0xFF0288D1, "24-hour clock", "Show 13:45 instead of 1:45 PM", cfg.isInternationalClock(), true);
                } else if (position == rowLinkGuard) {
                    c.setFeature(R.drawable.media_link_24, 0xFFD32F2F, "Check unknown links", "Ask before opening a website that is not Telegram", cfg.isLinkGuardEnabled(), true);
                } else if (position == rowReadableText) {
                    c.setFeature(R.drawable.settings_language, 0xFF5E35B1, "Larger text", "Easier reading for mixed languages", cfg.isReadableTextEnabled(), true);
                } else if (position == rowReduceMotion) {
                    c.setFeature(R.drawable.settings_power, 0xFF64748B, "Reduce motion", "Fewer animations, easier on the eyes", cfg.isReduceMotion(), true);
                } else if (position == rowChatListDetails) {
                    c.setFeature(R.drawable.settings_chat, 0xFF0284C7, "Chat list details", "Three lines so chats are easier to scan", cfg.isChatListDetails(), true);
                } else if (position == rowPauseGifs) {
                    c.setFeature(R.drawable.settings_data, 0xFF7C3AED, "Pause GIFs", "GIFs stay still until you open them", cfg.isPauseGifAutoplay(), true);
                } else if (position == rowSaveData) {
                    c.setFeature(R.drawable.settings_data, 0xFF059669, "Save mobile data", "Download photos and videos only when opened", cfg.isSaveMobileData(), false);
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
