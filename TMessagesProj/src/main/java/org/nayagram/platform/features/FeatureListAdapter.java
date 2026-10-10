package org.nayagram.platform.features;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.EditText;
import org.nayagram.platform.NayaConfig;
import org.nayagram.platform.storage.SmartStorageDoctor;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * FeatureListAdapter - NayaGram Exclusive Features Showcase.
 * Clean list without Ghost Mode / Anti-Delete (policy-compliant).
 * Supports English / বাংলা / العربية.
 */
public class FeatureListAdapter extends RecyclerView.Adapter<FeatureListViewHolder> {

    public static final int LANG_EN = 0;
    public static final int LANG_BN = 1;
    public static final int LANG_AR = 2;

    private final Context context;
    private BaseFragment fragment;
    private final List<FeatureItem> features = new ArrayList<>();
    private int currentLang = LANG_EN;

    public FeatureListAdapter(Context context) {
        this(context, null);
    }

    public FeatureListAdapter(Context context, BaseFragment fragment) {
        this.context = context;
        this.fragment = fragment;
        initializeFeatures();
    }

    public void setFragment(BaseFragment fragment) {
        this.fragment = fragment;
    }

    public void setLanguage(int lang) {
        this.currentLang = lang;
        notifyDataSetChanged();
    }

    public int getCurrentLang() {
        return currentLang;
    }

    private void initializeFeatures() {
        features.clear();

        // 2. Message Scheduler
        features.add(new FeatureItem(
                2,
                0xFF3B82F6,
                0xFF1D4ED8,
                R.drawable.msg_calendar,
                new String[]{"Message Scheduler", "মেসেজ শিডিউলার", "جدولة الرسائل"},
                new String[]{
                        "Send messages at a specific date & time automatically.",
                        "নির্দিষ্ট তারিখ ও সময়ে স্বয়ংক্রিয়ভাবে মেসেজ পাঠান।",
                        "أرسل الرسائل في تاريخ ووقت محددين تلقائيًا."
                }
        ));

        // 3. Auto-Reply
        features.add(new FeatureItem(
                3,
                0xFF10B981,
                0xFF047857,
                R.drawable.msg_bot,
                new String[]{"Auto-Reply", "অটো-রিপ্লাই", "الرد التلقائي"},
                new String[]{
                        "Keyword-based auto-reply with spam filtering & self-reply guard.",
                        "কীওয়ার্ড ভিত্তিক অটো-রিপ্লাই, স্প্যাম ফিল্টার সহ।",
                        "رد تلقائي قائم على الكلمات المفتاحية مع فلترة الرسائل المزعجة."
                }
        ));

        // 4. Story Saver
        features.add(new FeatureItem(
                4,
                0xFF8B5CF6,
                0xFF6D28D9,
                R.drawable.msg_download,
                new String[]{"Story Saver", "স্টোরি সেভার", "حفظ القصص"},
                new String[]{
                        "Download stories (photos & HD videos) in original quality with 1 click.",
                        "এক ক্লিকে মূল কোয়ালিটিতে স্টোরি (ছবি ও HD ভিডিও) ডাউনলোড করুন।",
                        "قم بتنزيل القصص (صور ومقاطع فيديو عالية الدقة) بالجودة الأصلية بنقرة واحدة."
                }
        ));

        // 5. Anonymous Stories (local preference only)
        features.add(new FeatureItem(
                5,
                0xFF6366F1,
                0xFF4338CA,
                R.drawable.msg_view_file,
                new String[]{"Anonymous Stories", "অ্যানোনিমাস স্টোরিজ", "قصص مجهولة"},
                new String[]{
                        "View friends' stories privately without appearing on viewer lists (local preference).",
                        "বন্ধুদের স্টোরি ব্যক্তিগতভাবে দেখুন (লোকাল সেটিং)।",
                        "اعرض قصص الأصدقاء بشكل خاص دون الظهور في قوائم المشاهدين (إعداد محلي)."
                }
        ));

        // 7. Clean Forward
        features.add(new FeatureItem(
                7,
                0xFFEC4899,
                0xFFBE185D,
                R.drawable.msg_forward,
                new String[]{"Clean Forward", "ক্লিন ফরওয়ার্ড", "إعادة توجيه نظيفة"},
                new String[]{
                        "Forward media or messages without the original sender name tag.",
                        "মূল প্রেরকের নাম ছাড়াই মিডিয়া বা মেসেজ ফরওয়ার্ড করুন।",
                        "أعد توجيه الوسائط أو الرسائل بدون اسم المرسل الأصلي."
                }
        ));

        // 8. Voice Transcription
        features.add(new FeatureItem(
                8,
                0xFFF59E0B,
                0xFFD97706,
                R.drawable.msg_voice_headphones,
                new String[]{"Voice Transcription", "ভয়েস ট্রান্সক্রিপশন", "تحويل الصوت إلى نص"},
                new String[]{
                        "Transcribe voice notes to text instantly in multiple languages.",
                        "ভয়েস নোট তাৎক্ষণিকভাবে টেক্সটে রূপান্তর করুন।",
                        "حوّل الملاحظات الصوتية إلى نص فورًا بعدة لغات."
                }
        ));

        // 9. Smart Chat Folders
        features.add(new FeatureItem(
                9,
                0xFF14B8A6,
                0xFF0F766E,
                R.drawable.msg_folders,
                new String[]{"Smart Chat Folders", "স্মার্ট চ্যাট ফোল্ডার", "مجلدات الدردشة الذكية"},
                new String[]{
                        "Auto-separate Users, Groups, Channels & Bots into smart folders.",
                        "ইউজার, গ্রুপ, চ্যানেল ও বট স্বয়ংক্রিয়ভাবে আলাদা ফোল্ডারে সাজান।",
                        "افصل تلقائيًا المستخدمين والمجموعات والقنوات والبوتات في مجلدات ذكية."
                }
        ));

        // 10. Call & Voice Protection
        features.add(new FeatureItem(
                10,
                0xFFF97316,
                0xFFC2410C,
                R.drawable.msg_calls,
                new String[]{"Call & Voice Protection", "কল ও ভয়েস সুরক্ষা", "حماية المكالمات والصوت"},
                new String[]{
                        "Confirmation prompt before calls & voice notes to prevent accidental taps.",
                        "ভুল করে কল বা ভয়েস নোট পাঠানোর আগে নিশ্চিতকরণ।",
                        "تأكيد قبل المكالمات والملاحظات الصوتية لتجنب النقرات العرضية."
                }
        ));

        // 11. User ID & DC Display
        features.add(new FeatureItem(
                11,
                0xFFA855F7,
                0xFF7E22CE,
                R.drawable.msg_info,
                new String[]{"User ID & DC", "ইউজার আইডি ও ডিসি", "معرف المستخدم ومركز البيانات"},
                new String[]{
                        "See your Telegram numeric ID and which data center (DC1-DC5) you're on.",
                        "আপনার টেলিগ্রাম নম্বরিক আইডি এবং ডেটা সেন্টার (DC1-DC5) দেখুন।",
                        "اطلع على معرفك الرقمي ومركز البيانات (DC1-DC5) الذي تتصل به."
                }
        ));

        // 12. Modular Config
        features.add(new FeatureItem(
                12,
                0xFF22C55E,
                0xFF15803D,
                R.drawable.msg_settings,
                new String[]{"Modular Config", "মডুলার কনফিগ", "تكوين معياري"},
                new String[]{
                        "All custom settings isolated in an independent, safe package.",
                        "সব কাস্টম সেটিংস আলাদা নিরাপদ প্যাকেজে সংরক্ষিত।",
                        "جميع الإعدادات المخصصة معزولة في حزمة مستقلة وآمنة."
                }
        ));

        // 13. Focus Mode
        features.add(new FeatureItem(
                13,
                0xFF0EA5E9,
                0xFF0369A1,
                R.drawable.msg_mute,
                new String[]{"Focus Mode", "ফোকাস মোড", "وضع التركيز"},
                new String[]{
                        "Silence non-urgent notifications. VIP contacts bypass (family & important).",
                        "অপ্রয়োজনীয় নোটিফিকেশন বন্ধ করুন। VIP কন্টাক্ট বাইপাস করতে পারে।",
                        "كتم الإشعارات غير العاجلة. جهات الاتصال المهمة تتجاوز الوضع."
                }
        ));

        // 14. Storage Doctor
        features.add(new FeatureItem(
                14,
                0xFF7C3AED,
                0xFF5B21B6,
                R.drawable.msg_delete,
                new String[]{"Storage Doctor", "স্টোরেজ ডক্টর", "طبيب التخزين"},
                new String[]{
                        "Scan & clean Telegram cache and duplicate media with one tap.",
                        "এক ট্যাপে টেলিগ্রাম ক্যাশ ও ডুপ্লিকেট মিডিয়া পরিষ্কার করুন।",
                        "امسح ونظف ذاكرة التخزين المؤقت والوسائط المكررة بنقرة واحدة."
                }
        ));

        // 15. Battery Saver
        features.add(new FeatureItem(
                15,
                0xFF16A34A,
                0xFF166534,
                R.drawable.msg2_battery,
                new String[]{"Battery Saver", "ব্যাটারি সেভার", "توفير البطارية"},
                new String[]{
                        "Below 20% charge, auto-pause video & heavy animations. Save 30-40% battery.",
                        "২০% এর নিচে চার্জ থাকলে ভিডিও ও অ্যানিমেশন অটো-পজ। ৩০-৪০% ব্যাটারি বাঁচান।",
                        "عند أقل من 20% شحن، أوقف الفيديو والرسوم المتحركة تلقائيًا. وفر 30-40% بطارية."
                }
        ));

        // 16. Chat Locker
        features.add(new FeatureItem(
                16,
                0xFF2563EB,
                0xFF1E40AF,
                R.drawable.msg_secret,
                new String[]{"Chat Locker", "চ্যাট লকার", "قفل الدردشة"},
                new String[]{
                        "Lock only sensitive chats with fingerprint & passcode. Keep privacy safe.",
                        "শুধু সংবেদনশীল চ্যাট ফিঙ্গারপ্রিন্ট ও পাসকোড দিয়ে লক করুন।",
                        "اقفل الدردشات الحساسة فقط ببصمة الإصبع ورمز المرور."
                }
        ));

        // 17. Instant Translator
        features.add(new FeatureItem(
                17,
                0xFFDB2777,
                0xFF9D174D,
                R.drawable.msg_translate,
                new String[]{"Instant Translator", "ইনস্ট্যান্ট ট্রান্সলেটর", "المترجم الفوري"},
                new String[]{
                        "Translate any foreign language message inside the chat in one tap.",
                        "চ্যাটের ভিতরে যেকোনো বিদেশি ভাষার মেসেজ এক ট্যাপে অনুবাদ করুন।",
                        "ترجم أي رسالة بلغة أجنبية داخل الدردشة بنقرة واحدة."
                }
        ));
    }

    @NonNull
    @Override
    public FeatureListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout container = new FrameLayout(context);
        container.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return new FeatureListViewHolder(container);
    }

    public boolean isFeatureEnabled(FeatureItem item) {
        NayaConfig cfg = NayaConfig.getInstance();
        if (cfg == null) return false;
        switch (item.number) {
            case 2: return cfg.isMessageScheduler();
            case 3: return cfg.isSmartAutoReply();
            case 4: return cfg.isStorySaverEnabled();
            case 5: return cfg.isAnonymousStories();
            case 7: return cfg.isForwardWithoutQuote();
            case 8: return cfg.isVoiceTranscriptionEnabled();
            case 9: return cfg.isSmartFoldersEnabled();
            case 10: return cfg.isConfirmActions();
            case 11: return cfg.isShowIdAndDc();
            case 12: return cfg.isModularConfig();
            case 13: return cfg.isFocusModeEnabled();
            case 14: return true;
            case 15: return cfg.isBatterySaverEnabled();
            case 16: return cfg.isBiometricChatLockerEnabled();
            case 17: return cfg.isInstantTranslatorEnabled();
            default: return false;
        }
    }

    private String getActionLabel(FeatureItem item) {
        if (item.number == 14) {
            return currentLang == LANG_BN ? "ক্লিন" : (currentLang == LANG_AR ? "تنظيف" : "CLEAN");
        }
        boolean enabled = isFeatureEnabled(item);
        if (item.number == 3 && enabled) {
            return currentLang == LANG_BN ? "সেট" : (currentLang == LANG_AR ? "ضبط" : "CONFIG");
        }
        if (enabled) {
            return currentLang == LANG_BN ? "অন" : (currentLang == LANG_AR ? "مفعل" : "ON");
        } else {
            return currentLang == LANG_BN ? "অফ" : (currentLang == LANG_AR ? "معطل" : "OFF");
        }
    }

    private int getLabelTextColor(FeatureItem item) {
        if (item.number == 14) return 0xFF7C3AED;
        boolean enabled = isFeatureEnabled(item);
        if (item.number == 3 && enabled) return 0xFF0284C7;
        return enabled ? 0xFF10B981 : 0xFF0284C7; // ON = Emerald Green, OFF = Royal Blue
    }

    private int getLabelBgColor(FeatureItem item) {
        if (item.number == 14) return 0x227C3AED;
        boolean enabled = isFeatureEnabled(item);
        if (item.number == 3 && enabled) return 0x200EA5E9;
        return enabled ? 0x2410B981 : 0x240284C7; // ON = Green Tint, OFF = Blue Tint
    }

    @Override
    public void onBindViewHolder(@NonNull FeatureListViewHolder holder, int position) {
        FeatureItem item = features.get(position);
        boolean enabled = isFeatureEnabled(item);
        String label = getActionLabel(item);
        int textColor = getLabelTextColor(item);
        int bgColor = getLabelBgColor(item);

        holder.bind(item, position, currentLang, enabled, label, textColor, bgColor);
        holder.setListeners(
                v -> handleFeatureClick(item, position),
                v -> {
                    showFeatureDetailsDialog(item);
                    return true;
                },
                v -> showFeatureDetailsDialog(item)
        );
    }

    private void handleFeatureClick(FeatureItem item, int position) {
        NayaConfig cfg = NayaConfig.getInstance();
        if (cfg == null) return;

        if (item.number == 14) {
            showStorageDoctorScanDialog();
            return;
        }

        if (item.number == 3 && cfg.isSmartAutoReply()) {
            showAutoReplyConfigDialog();
            return;
        }

        boolean newState = !isFeatureEnabled(item);
        switch (item.number) {
            case 2: cfg.setMessageScheduler(newState); break;
            case 3:
                cfg.setSmartAutoReply(newState);
                if (newState) showAutoReplyConfigDialog();
                break;
            case 4: cfg.setStorySaverEnabled(newState); break;
            case 5: cfg.setAnonymousStories(newState); break;
            case 7: cfg.setForwardWithoutQuote(newState); break;
            case 8: cfg.setVoiceTranscriptionEnabled(newState); break;
            case 9: cfg.setSmartFoldersEnabled(newState); break;
            case 10: cfg.setConfirmActions(newState); break;
            case 11: cfg.setShowIdAndDc(newState); break;
            case 12: cfg.setModularConfig(newState); break;
            case 13: cfg.setFocusModeEnabled(newState); break;
            case 15: cfg.setBatterySaverEnabled(newState); break;
            case 16: cfg.setBiometricChatLockerEnabled(newState); break;
            case 17: cfg.setInstantTranslatorEnabled(newState); break;
        }

        notifyItemChanged(position);

        if (fragment != null) {
            String status = newState ? "ON" : "OFF";
            BulletinFactory.of(fragment).createSimpleBulletin(
                    R.raw.done,
                    item.getCatchyTitle(currentLang) + ": " + status
            ).show();
        }
    }

    private void showStorageDoctorScanDialog() {
        if (context == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("⚡ Smart Storage Doctor");
        builder.setMessage("Scanning cache & temporary files...");

        AlertDialog progressDialog = builder.create();
        progressDialog.show();

        SmartStorageDoctor.getInstance().scanStorage(context, report -> {
            try {
                progressDialog.dismiss();
            } catch (Throwable ignored) {
            }

            if (report == null || report.totalReclaimableBytes <= 0) {
                AlertDialog.Builder cleanBldr = new AlertDialog.Builder(context);
                cleanBldr.setTitle("✨ Storage Clean");
                cleanBldr.setMessage("Your cache is completely optimized and clean! No unnecessary temporary files found.");
                cleanBldr.setPositiveButton("OK", null);
                cleanBldr.show();
                return;
            }

            AlertDialog.Builder cleanBldr = new AlertDialog.Builder(context);
            cleanBldr.setTitle("🧹 Storage Analysis");
            cleanBldr.setMessage("Found reclaimable cache:
• Total Cache: " + report.getFormattedTotal() + "
• Cached Files: " + report.fileCount + "
Clean cache now to boost NayaGram speed?");
            cleanBldr.setPositiveButton("Clean Now", (d, w) -> {
                SmartStorageDoctor.getInstance().cleanCache(context, (freed, success) -> {
                    if (fragment != null) {
                        BulletinFactory.of(fragment).createSimpleBulletin(
                                R.raw.done,
                                "Cleaned " + SmartStorageDoctor.formatSize(freed) + " cache!"
                        ).show();
                    }
                });
            });
            cleanBldr.setNegativeButton("Cancel", null);
            cleanBldr.show();
        });
    }

    private void showAutoReplyConfigDialog() {
        if (context == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("💬 Smart Auto-Reply Message");
        final EditText et = new EditText(context);
        et.setText(NayaConfig.getInstance().getAutoReplyText());
        et.setSelection(et.getText().length());
        builder.setView(et);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String text = et.getText().toString().trim();
            if (!text.isEmpty()) {
                NayaConfig.getInstance().setAutoReplyText(text);
                if (fragment != null) {
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.done, "Auto-reply message updated").show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public int getItemCount() {
        return features.size();
    }

    private void showFeatureDetailsDialog(FeatureItem item) {
        if (context == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(item.getCatchyTitle(currentLang));
        builder.setMessage(item.getDesc(currentLang) + "\n\n" +
                "Feature #" + item.number + " • NayaGram Exclusive");
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    public void showAllDetailsPipelineDialog() {
        if (context == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("✦ NayaGram Exclusive Features Pipeline ✦\n\n");
        sb.append("PRIVACY & CONTROL PIPELINE\n");
        sb.append("• Focus Mode → Quiet hours with VIP bypass\n");
        sb.append("• Biometric Chat Locker → Fingerprint-locked chats\n");
        sb.append("• Anonymous Stories → Local preference only\n\n");
        sb.append("MESSAGING & TOOLS\n");
        sb.append("• Message Scheduler → Timed delivery\n");
        sb.append("• Auto-Reply → Keyword based replies\n");
        sb.append("• Story Saver → HD download\n");
        sb.append("• Clean Forward → No sender tag\n");
        sb.append("• Voice Transcription → Speech to text\n");
        sb.append("• Smart Chat Folders → Auto organize\n\n");
        sb.append("SECURITY & OPTIMIZATION\n");
        sb.append("• Call & Voice Protection → Confirm before call\n");
        sb.append("• User ID & DC Display → See your DC\n");
        sb.append("• Storage Doctor → One-tap clean\n");
        sb.append("• Battery Saver → Low battery mode\n");
        sb.append("• Instant Translator → In-chat translate\n\n");
        sb.append("Total: 15 active exclusive features\n");
        sb.append("Policy-compliant • No Anti-Delete • No Ghost Mode");

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("All Features Pipeline");
        builder.setMessage(sb.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    /**
     * FeatureItem model used by FeatureListViewHolder.
     */
    public static class FeatureItem {
        public final int number;
        public final int accentColor;
        public final int numberBadgeColor;
        public final int iconRes;
        private final String[] titles;
        private final String[] descs;

        public FeatureItem(int number, int accentColor, int numberBadgeColor, int iconRes,
                           String[] titles, String[] descs) {
            this.number = number;
            this.accentColor = accentColor;
            this.numberBadgeColor = numberBadgeColor;
            this.iconRes = iconRes;
            this.titles = titles;
            this.descs = descs;
        }

        public String getCatchyTitle(int lang) {
            if (lang >= 0 && lang < titles.length) return titles[lang];
            return titles[0];
        }

        public String getDesc(int lang) {
            if (lang >= 0 && lang < descs.length) return descs[lang];
            return descs[0];
        }
    }
}
