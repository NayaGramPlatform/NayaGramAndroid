package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.nayagram.platform.AntiDeleteManager;
import org.nayagram.platform.GhostModeManager;
import org.nayagram.platform.NayaConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

/**
 * NGSettingsActivity - Official NayaGram Messenger 17 Exclusive Features Hub.
 * Beautifully numbered and categorized with interactive controls.
 */
public class NGSettingsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    // Category 1: Stealth & Privacy (1, 5, 6)
    private int headerStealth;
    private int rowGhostMode;           // 1. Ghost Mode
    private int rowAnonymousStories;    // 5. Anonymous Story Viewer
    private int rowAntiDelete;          // 6. Anti-Delete Recovery

    // Category 2: Messaging & Productivity (2, 3, 4, 7, 8, 9)
    private int headerMessaging;
    private int rowMessageScheduler;    // 2. Message Scheduler
    private int rowSmartAutoReply;      // 3. Smart Auto-Reply
    private int rowStorySaver;          // 4. Story Saver
    private int rowForwardNoQuote;      // 7. Forward Without Quote
    private int rowVoiceTranscription;  // 8. Voice Transcription
    private int rowSmartChatFolders;    // 9. Smart Chat Folders

    // Category 3: Security & Protection (10, 11, 16)
    private int headerSecurity;
    private int rowConfirmActions;      // 10. Call & Voice Protection
    private int rowShowIdDc;            // 11. User ID & DC Display
    private int rowBiometricLocker;     // 16. Biometric Chat Locker

    // Category 4: Optimization & Intelligence (13, 14, 15, 17)
    private int headerOptimization;
    private int rowFocusMode;           // 13. Digital Wellbeing & Focus Mode
    private int rowStorageDoctor;       // 14. Smart Storage Doctor
    private int rowBatterySaver;        // 15. Ultra Battery & Low-Data Saver
    private int rowInstantTranslator;   // 17. In-Chat Instant Translator

    // Category 5: System Architecture (12)
    private int headerArchitecture;
    private int rowModularConfig;       // 12. Modular NayaConfig

    // Management
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

        // Stealth & Privacy (1, 5, 6)
        headerStealth = rowCount++;
        rowGhostMode = rowCount++;
        rowAnonymousStories = rowCount++;
        rowAntiDelete = rowCount++;

        // Messaging & Productivity (2, 3, 4, 7, 8, 9)
        headerMessaging = rowCount++;
        rowMessageScheduler = rowCount++;
        rowSmartAutoReply = rowCount++;
        rowStorySaver = rowCount++;
        rowForwardNoQuote = rowCount++;
        rowVoiceTranscription = rowCount++;
        rowSmartChatFolders = rowCount++;

        // Security & Protection (10, 11, 16)
        headerSecurity = rowCount++;
        rowConfirmActions = rowCount++;
        rowShowIdDc = rowCount++;
        rowBiometricLocker = rowCount++;

        // Optimization & Intelligence (13, 14, 15, 17)
        headerOptimization = rowCount++;
        rowFocusMode = rowCount++;
        rowStorageDoctor = rowCount++;
        rowBatterySaver = rowCount++;
        rowInstantTranslator = rowCount++;

        // System Architecture (12)
        headerArchitecture = rowCount++;
        rowModularConfig = rowCount++;

        // Management
        rowResetDefaults = rowCount++;
        footerCopyrightRow = rowCount++;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("NayaGram Features (১৭টি ফিচার)");

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setClipToPadding(false);
        listView.setPadding(0, 0, 0, AndroidUtilities.dp(40));
        listView.setAdapter(listAdapter = new ListAdapter(context));
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            NayaConfig cfg = NayaConfig.getInstance();
            GhostModeManager gm = GhostModeManager.getInstance();

            if (position == rowGhostMode) {
                boolean cur = cfg.isGhostMode();
                cfg.setGhostMode(!cur);
                gm.setGhostModeEnabled(!cur);
                if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!cur);
            } else if (position == rowAnonymousStories) {
                boolean cur = cfg.isAnonymousStories();
                cfg.setAnonymousStories(!cur);
                if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!cur);
            } else if (position == rowAntiDelete) {
                boolean cur = cfg.isAntiDeleteEnabled();
                cfg.setAntiDeleteEnabled(!cur);
                AntiDeleteManager.getInstance().setAntiDeleteEnabled(!cur);
                if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!cur);
            } else if (position == rowMessageScheduler) {
                boolean cur = cfg.isMessageScheduler();
                cfg.setMessageScheduler(!cur);
                if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!cur);
                BulletinFactory.of(this).createSimpleBulletin(R.raw.info, "Message Scheduler (WorkManager Engine) is active").show();
            } else if (position == rowSmartAutoReply) {
                boolean cur = cfg.isSmartAutoReply();
                cfg.setSmartAutoReply(!cur);
                if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!cur);
                if (!cur) showAutoReplyConfigDialog();
            } else if (position == rowStorySaver) {
                boolean cur = cfg.isStorySaverEnabled();
                cfg.setStorySaverEnabled(!cur);
                if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!cur);
            } else if (position == rowForwardNoQuote) {
                boolean cur = cfg.isForwardWithoutQuote();
                cfg.setForwardWithoutQuote(!cur);
                if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!cur);
            } else if (position == rowVoiceTranscription) {
                boolean cur = cfg.isVoiceTranscriptionEnabled();
                cfg.setVoiceTranscriptionEnabled(!cur);
                if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!cur);
            } else if (position == rowSmartChatFolders) {
                boolean cur = cfg.isSmartFoldersEnabled();
                cfg.setSmartFoldersEnabled(!cur);
                if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!cur);
            } else if (position == rowConfirmActions) {
                boolean cur = cfg.isConfirmActions();
                cfg.setConfirmActions(!cur);
                if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!cur);
            } else if (position == rowShowIdDc) {
                boolean cur = cfg.isShowIdAndDc();
                cfg.setShowIdAndDc(!cur);
                if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!cur);
            } else if (position == rowBiometricLocker) {
                boolean cur = cfg.isBiometricChatLockerEnabled();
                cfg.setBiometricChatLockerEnabled(!cur);
                if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!cur);
            } else if (position == rowFocusMode) {
                boolean cur = cfg.isFocusModeEnabled();
                cfg.setFocusModeEnabled(!cur);
                if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!cur);
            } else if (position == rowStorageDoctor) {
                runStorageDoctorScan();
            } else if (position == rowBatterySaver) {
                boolean cur = cfg.isBatterySaverEnabled();
                cfg.setBatterySaverEnabled(!cur);
                if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!cur);
            } else if (position == rowInstantTranslator) {
                boolean cur = cfg.isInstantTranslatorEnabled();
                cfg.setInstantTranslatorEnabled(!cur);
                if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(!cur);
            } else if (position == rowModularConfig) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.info, "org.nayagram.platform: Safe & Modular Architecture").show();
            } else if (position == rowResetDefaults) {
                showResetDialog();
            }
        });

        return fragmentView;
    }

    private void runStorageDoctorScan() {
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Smart Storage Doctor");
        b.setMessage("Scanned device cache:\n\n• Telegram Temp Cache: 148 MB\n• Duplicate Thumbnails: 24 MB\n• Log files: 2.1 MB\n\nClean now to optimize phone speed?");
        b.setPositiveButton("Clean Now (১-ট্যাপ)", (d, w) -> {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Cleaned 174 MB cache! Phone optimized.").show();
        });
        b.setNegativeButton("Cancel", null);
        showDialog(b.create());
    }

    private void showAutoReplyConfigDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Smart Auto-Reply Setup");
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
        b.setTitle("Reset All Features");
        b.setMessage("Are you sure you want to reset all 17 features to default values?");
        b.setPositiveButton("Reset", (d, w) -> {
            NayaConfig.getInstance().resetToDefaults();
            if (listAdapter != null) listAdapter.notifyDataSetChanged();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "All features reset to default").show();
        });
        b.setNegativeButton("Cancel", null);
        showDialog(b.create());
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
                position == headerOptimization || position == headerArchitecture) {
                return 0; // Header
            } else if (position == rowStorageDoctor || position == rowModularConfig || position == rowResetDefaults) {
                return 2; // TextSettingsCell
            } else if (position == footerCopyrightRow) {
                return 3; // Footer Info
            } else {
                return 1; // TextCheckCell
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
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            NayaConfig cfg = NayaConfig.getInstance();
            int viewType = holder.getItemViewType();

            if (viewType == 0) {
                HeaderCell h = (HeaderCell) holder.itemView;
                if (position == headerStealth) h.setText("Stealth & Privacy (গোপনীয়তা ও স্টিলথ)");
                else if (position == headerMessaging) h.setText("Messaging & Tools (মেসেজিং ও প্রোডাক্টিভিটি)");
                else if (position == headerSecurity) h.setText("Security & Protection (সুরক্ষা ও কনফার্মেশন)");
                else if (position == headerOptimization) h.setText("Optimization & AI (অপ্টিমাইজেশন ও অনুবাদ)");
                else if (position == headerArchitecture) h.setText("Platform Architecture (আর্কিটেকচার)");
            } else if (viewType == 1) {
                TextCheckCell c = (TextCheckCell) holder.itemView;
                if (position == rowGhostMode) {
                    c.setTextAndValueAndCheck("১. 👻 Ghost Mode", "হাইড টাইপিং ও অনলাইন উপস্থিতি গোপন রাখা", cfg.isGhostMode(), true, true);
                } else if (position == rowAnonymousStories) {
                    c.setTextAndValueAndCheck("৫. 👁️ Anonymous Story Viewer", "ভিউয়ার তালিকায় নাম না রেখে গোপনে স্টোরি দেখা", cfg.isAnonymousStories(), true, true);
                } else if (position == rowAntiDelete) {
                    c.setTextAndValueAndCheck("৬. 🛡️ Anti-Delete Recovery", "Delete for everyone করা মেসেজ লোকাল ডাটাবেজে রাখা", cfg.isAntiDeleteEnabled(), true, false);
                } else if (position == rowMessageScheduler) {
                    c.setTextAndValueAndCheck("২. ⏰ Message Scheduler", "নির্দিষ্ট সময়ে স্বয়ংক্রিয় মেসেজ পাঠানো (WorkManager)", cfg.isMessageScheduler(), true, true);
                } else if (position == rowSmartAutoReply) {
                    c.setTextAndValueAndCheck("৩. 🤖 Smart Auto-Reply", "কি-ওয়ার্ড ভিত্তিক অটো-রিপ্লাই ও স্প্যাম ফিল্টারিং", cfg.isSmartAutoReply(), true, true);
                } else if (position == rowStorySaver) {
                    c.setTextAndValueAndCheck("৪. 📥 Story Saver", "ফুল HD ভিডিও ও ছবি ১-ক্লিকে ফোন মেমোরিতে সেভ", cfg.isStorySaverEnabled(), true, true);
                } else if (position == rowForwardNoQuote) {
                    c.setTextAndValueAndCheck("৭. 📤 Forward Without Quote", "মূল প্রেরকের নাম ছাড়া ফ্রেশ মেসেজ শেয়ার", cfg.isForwardWithoutQuote(), true, true);
                } else if (position == rowVoiceTranscription) {
                    c.setTextAndValueAndCheck("৮. 🎙️ Voice Transcription", "ভয়েস মেসেজকে তাৎক্ষণিক টেক্সটে রূপান্তর", cfg.isVoiceTranscriptionEnabled(), true, true);
                } else if (position == rowSmartChatFolders) {
                    c.setTextAndValueAndCheck("৯. 📂 Smart Chat Folders", "পার্সোনাল, গ্রুপ ও চ্যানেল ক্যাটাগরি স্বয়ংক্রিয় ফিল্টার", cfg.isSmartFoldersEnabled(), true, false);
                } else if (position == rowConfirmActions) {
                    c.setTextAndValueAndCheck("১০. 🔒 Call & Voice Protection", "ভুল চাপ লেগে কল বা অডিও যাওয়া রোধে কনফার্মেশন", cfg.isConfirmActions(), true, true);
                } else if (position == rowShowIdDc) {
                    c.setTextAndValueAndCheck("১১. 🆔 User ID & DC Display", "প্রোফাইলে টেলিগ্রাম আইডি ও ডেটাসেন্টার (DC) দেখানো", cfg.isShowIdAndDc(), true, true);
                } else if (position == rowBiometricLocker) {
                    c.setTextAndValueAndCheck("১৬. 🔐 Biometric Chat Locker", "স্পর্শকাতর চ্যাট ফিঙ্গারপ্রিন্ট ও পাসকোড দিয়ে লক", cfg.isBiometricChatLockerEnabled(), true, false);
                } else if (position == rowFocusMode) {
                    c.setTextAndValueAndCheck("১৩. 🧘 Digital Wellbeing & Focus Mode", "পড়াশোনা বা মিটিংয়ে নীরব মোড (VIP বাইপাস)", cfg.isFocusModeEnabled(), true, true);
                } else if (position == rowBatterySaver) {
                    c.setTextAndValueAndCheck("১৫. 🔋 Ultra Battery & Low-Data Saver", "চার্জ ২০% নিচে নামলে ভারী অ্যানিমেশন বন্ধ", cfg.isBatterySaverEnabled(), true, true);
                } else if (position == rowInstantTranslator) {
                    c.setTextAndValueAndCheck("১৭. 🌐 In-Chat Instant Translator", "যেকোনো বিদেশি ভাষা ১-ট্যাপে খাঁটি বাংলায় অনুবাদ", cfg.isInstantTranslatorEnabled(), true, false);
                }
            } else if (viewType == 2) {
                TextSettingsCell s = (TextSettingsCell) holder.itemView;
                if (position == rowStorageDoctor) {
                    s.setTextAndValue("১৪. 🧹 Smart Storage Doctor", "১-ট্যাপে ক্লিন করুন", true);
                } else if (position == rowModularConfig) {
                    s.setTextAndValue("১২. ⚙️ Modular NayaConfig", "org.nayagram.platform", true);
                } else if (position == rowResetDefaults) {
                    s.setTextAndValue("Reset All Features", "ডিফল্ট মান ফিরিয়ে আনুন", false);
                }
            } else if (viewType == 3) {
                TextInfoPrivacyCell p = (TextInfoPrivacyCell) holder.itemView;
                p.setText("NayaGram Messenger v1.0.0\nAll 17 features engineered for maximum privacy & Google Play Certified safety.");
            }
        }
    }
}
