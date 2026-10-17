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
    private final List<FeatureItem> allFeatures = new ArrayList<>();
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
        allFeatures.clear();

        // 9. Smart Chat Folders
        allFeatures.add(new FeatureItem(
                9,
                0xFF14B8A6,
                0xFF0F766E,
                R.drawable.msg_folders,
                new String[]{"Smart Chat Folders", "স্মার্ট চ্যাট ফোল্ডার", "مجلدات الدردشة الذكية"},
                new String[]{
                        "Not active yet. This card will be enabled after native Telegram folder integration is implemented.",
                        "এখনো চালু নয়। টেলিগ্রামের নেটিভ ফোল্ডার সংযোগ সম্পন্ন হলে ফিচারটি চালু হবে।",
                        "غير نشط حاليًا. سيتم تفعيله بعد تنفيذ تكامل مجلدات تيليجرام الأصلي."
                }
        ));



        // 11. User ID & DC Display
        allFeatures.add(new FeatureItem(
                11,
                0xFFA855F7,
                0xFF7E22CE,
                R.drawable.msg_info,
                new String[]{"User ID & DC", "ইউজার আইডি ও ডিসি", "معرف المستخدم ومركز البيانات"},
                new String[]{
                        "Show your Telegram ID and current account data-center ID on your own profile. Long-press to copy.",
                        "নিজের প্রোফাইলে টেলিগ্রাম আইডি ও বর্তমান অ্যাকাউন্টের ডেটা-সেন্টার আইডি দেখুন। কপি করতে চেপে ধরুন।",
                        "اعرض معرف تيليجرام ومعرف مركز بيانات الحساب الحالي في ملفك الشخصي. اضغط مطولًا للنسخ."
                }
        ));

        // 12. Modular Config
        allFeatures.add(new FeatureItem(
                12,
                0xFF22C55E,
                0xFF15803D,
                R.drawable.msg_settings,
                new String[]{"Modular Config", "মডুলার কনফিগ", "تكوين معياري"},
                new String[]{
                        "Not active yet. The toggle does not change how settings are stored or isolated.",
                        "এখনো চালু নয়। এই টগল সেটিংস সংরক্ষণ বা আলাদা রাখার পদ্ধতি বদলায় না।",
                        "غير نشط حاليًا. لا يغيّر هذا المفتاح طريقة حفظ الإعدادات أو عزلها."
                }
        ));

        // 13. Focus Mode
        allFeatures.add(new FeatureItem(
                13,
                0xFF0EA5E9,
                0xFF0369A1,
                R.drawable.msg_mute,
                new String[]{"Focus Mode", "ফোকাস মোড", "وضع التركيز"},
                new String[]{
                        "Not active yet. Notification scheduling and VIP bypass are not connected.",
                        "এখনো চালু নয়। নোটিফিকেশন সময়সূচি ও VIP ব্যতিক্রম সংযুক্ত হয়নি।",
                        "غير نشط حاليًا. لم يتم ربط جدولة الإشعارات واستثناء جهات VIP."
                }
        ));

        // 15. Battery Saver
        allFeatures.add(new FeatureItem(
                15,
                0xFF16A34A,
                0xFF166534,
                R.drawable.msg2_battery,
                new String[]{"Battery Saver", "ব্যাটারি সেভার", "توفير البطارية"},
                new String[]{
                        "Not active yet. Low-battery detection and automatic playback controls are not connected.",
                        "এখনো চালু নয়। কম ব্যাটারি শনাক্তকরণ ও স্বয়ংক্রিয় প্লেব্যাক নিয়ন্ত্রণ সংযুক্ত হয়নি।",
                        "غير نشط حاليًا. لم يتم ربط اكتشاف انخفاض البطارية والتحكم التلقائي بالتشغيل."
                }
        ));

        // 16. Chat Locker
        allFeatures.add(new FeatureItem(
                16,
                0xFF2563EB,
                0xFF1E40AF,
                R.drawable.msg_secret,
                new String[]{"Chat Locker", "চ্যাট লকার", "قفل الدردشة"},
                new String[]{
                        "Not active yet. No chat is protected by this preference alone.",
                        "এখনো চালু নয়। শুধু এই সেটিং দিয়ে কোনো চ্যাট সুরক্ষিত হয় না।",
                        "غير نشط حاليًا. هذا الإعداد وحده لا يحمي أي محادثة."
                }
        ));

        // 17. Instant Translator
        allFeatures.add(new FeatureItem(
                17,
                0xFFDB2777,
                0xFF9D174D,
                R.drawable.msg_translate,
                new String[]{"Instant Translator", "ইনস্ট্যান্ট ট্রান্সলেটর", "المترجم الفوري"},
                new String[]{
                        "Not active yet. No translation service or in-chat action is connected.",
                        "এখনো চালু নয়। অনুবাদ সেবা বা চ্যাটের ভেতরের অ্যাকশন সংযুক্ত হয়নি।",
                        "غير نشط حاليًا. لم يتم ربط خدمة ترجمة أو إجراء داخل المحادثة."
                }
        ));

        // 18. Adaptive Network & Data Saver
        allFeatures.add(new FeatureItem(
                18,
                0xFF059669,
                0xFF047857,
                R.drawable.msg2_data,
                new String[]{"Network Data Saver", "নেটওয়ার্ক ও ডেটা সেভার", "موفّر بيانات الشبكة"},
                new String[]{
                        "Uses Telegram's native media-streaming preference. It is not a full network optimizer.",
                        "টেলিগ্রামের নিজস্ব মিডিয়া স্ট্রিমিং সেটিং ব্যবহার করে। এটি সম্পূর্ণ নেটওয়ার্ক অপ্টিমাইজার নয়।",
                        "يستخدم إعداد تيليجرام الأصلي لبث الوسائط، وليس محسنًا شاملًا للشبكة."
                }
        ));

        // 19. WebRTC Acoustic Noise Suppression
        allFeatures.add(new FeatureItem(
                19,
                0xFF2563EB,
                0xFF1D4ED8,
                R.drawable.msg_calls,
                new String[]{"Acoustic Noise Shield", "অডিও নয়েজ সাপ্রেশন", "حاجب الضوضاء الصوتية"},
                new String[]{
                        "Not active yet. The call audio pipeline has no verified noise-suppression wiring.",
                        "এখনো চালু নয়। কলের অডিও পাইপলাইনে নয়েজ সাপ্রেশন সংযুক্ত থাকার নিশ্চয়তা নেই।",
                        "غير نشط حاليًا. لم يتم التحقق من ربط تقليل الضوضاء بمسار صوت المكالمة."
                }
        ));

        // 20. Smart Storage & Cache Auto-Limiter
        allFeatures.add(new FeatureItem(
                20,
                0xFF7C3AED,
                0xFF6D28D9,
                R.drawable.settings_data,
                new String[]{"Smart Storage Guard", "স্মার্ট স্টোরেজ গার্ড", "حارس التخزين الذكي"},
                new String[]{
                        "Scan app cache on demand and review the report before cleaning. No automatic storage limiter runs.",
                        "প্রয়োজনে অ্যাপের ক্যাশ স্ক্যান করুন এবং পরিষ্কারের আগে রিপোর্ট দেখুন। স্বয়ংক্রিয় স্টোরেজ লিমিটার চলে না।",
                        "افحص ذاكرة التطبيق عند الطلب وراجع التقرير قبل التنظيف. لا يعمل محدد تخزين تلقائي."
                }
        ));

        // 21. External Link Guard (functional: Browser.java checks this preference)
        allFeatures.add(new FeatureItem(
                21,
                0xFFDC2626,
                0xFFB91C1C,
                R.drawable.media_link_24,
                new String[]{"External Link Guard", "বাহ্যিক লিংক সুরক্ষা", "حماية الروابط الخارجية"},
                new String[]{
                        "Ask before opening unfamiliar external websites.",
                        "অপরিচিত বাহ্যিক ওয়েবসাইট খোলার আগে নিশ্চিতকরণ নিন।",
                        "اطلب التأكيد قبل فتح مواقع خارجية غير مألوفة."
                }
        ));

        // 22. Readable Text (functional: applies Telegram's native message font size)
        allFeatures.add(new FeatureItem(
                22,
                0xFF7C3AED,
                0xFF6D28D9,
                R.drawable.settings_language,
                new String[]{"Readable Text", "সহজপাঠ্য লেখা", "نص أسهل للقراءة"},
                new String[]{
                        "Increase message text size for comfortable reading.",
                        "আরামদায়কভাবে পড়তে মেসেজের লেখা বড় করুন।",
                        "كبّر نص الرسائل لقراءة أكثر راحة."
                }
        ));

        // 23. Reduce Motion (functional: applies Telegram's animation preference)
        allFeatures.add(new FeatureItem(
                23,
                0xFF64748B,
                0xFF475569,
                R.drawable.settings_power,
                new String[]{"Reduce Motion", "অ্যানিমেশন কমান", "تقليل الحركة"},
                new String[]{
                        "Reduce interface animations while keeping messaging and calls available.",
                        "মেসেজিং ও কল চালু রেখেই ইন্টারফেসের অ্যানিমেশন কমান।",
                        "قلل حركات الواجهة مع استمرار الرسائل والمكالمات."
                }
        ));
        // Only verified, native-backed features belong on the main feature page.
        // Planned or tutorial-only items stay documented in FeatureTutorialDialog.
        for (FeatureItem item : allFeatures) {
            if (isMainPageFeature(item)) {
                features.add(item);
            }
        }
    }

    /** Filters the showcase across English, Bengali and Arabic titles/descriptions. */
    public void setSearchQuery(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        features.clear();
        for (FeatureItem item : allFeatures) {
            if (isMainPageFeature(item) && (normalized.isEmpty() || matchesFeature(item, normalized))) {
                features.add(item);
            }
        }
        notifyDataSetChanged();
    }

    private boolean matchesFeature(FeatureItem item, String query) {
        for (int lang = 0; lang < 3; lang++) {
            if (item.getCatchyTitle(lang).toLowerCase(java.util.Locale.ROOT).contains(query)
                    || item.getDesc(lang).toLowerCase(java.util.Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
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
        if (cfg == null || isImplementationPending(item)) return false;
        switch (item.number) {
            case 2: return cfg.isMessageScheduler();
            case 3: return cfg.isSmartAutoReply();
            case 4: return cfg.isStorySaverEnabled();
            case 5: return cfg.isAnonymousStories();
            case 18: return cfg.isDataSaverEnabled();
            case 20: return false; // Action card: runs a scan instead of representing a toggle
            case 21: return cfg.isLinkGuardEnabled();
            case 22: return cfg.isReadableTextEnabled();
            case 23: return cfg.isReduceMotion();
            case 11: return cfg.isShowIdAndDc();
            case 14: return true;
            default: return false;
        }
    }

    /**
     * The main page only exposes features with a verified behavior path.
     * Feature Pipeline is documentation and never contributes feature cards.
     */
    private boolean isMainPageFeature(FeatureItem item) {
        switch (item.number) {
            case 11: // User ID & active data-center ID on profile
            case 18: // Telegram native media-streaming preference
            case 20: // On-demand storage scan and reviewed cache cleanup
            case 21: // External Link Guard
            case 22: // Telegram native message font size
            case 23: // Telegram interface animation preference
                return true;
            default:
                return false;
        }
    }

    /** Features whose UI is present but whose native behavior is not implemented yet. */
    private boolean isImplementationPending(FeatureItem item) {
        switch (item.number) {
            case 2:  // Message Scheduler has no delivery engine
            case 3:  // Auto-Reply has no sending engine
            case 4:  // Story Saver is disabled
            case 5:  // Anonymous Stories is disabled
            case 9:  // Smart Chat Folders
            case 13: // Focus Mode
            case 15: // Battery Saver
            case 16: // Chat Locker
            case 17: // Instant Translator
            case 19: // Noise Suppression
                return true;
            default:
                return false;
        }
    }

    private String getActionLabel(FeatureItem item) {
        if (item.number == 20) {
            return currentLang == LANG_BN ? "স্ক্যান" : (currentLang == LANG_AR ? "فحص" : "SCAN");
        }
        if (isImplementationPending(item)) {
            return currentLang == LANG_BN ? "পরে" : (currentLang == LANG_AR ? "لاحقًا" : "SOON");
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
        if (item.number == 20) return 0xFF7C3AED;
        if (isImplementationPending(item)) return 0xFF64748B;
        boolean enabled = isFeatureEnabled(item);
        if (item.number == 3 && enabled) return 0xFF0284C7;
        return enabled ? 0xFF10B981 : 0xFF0284C7; // ON = Emerald Green, OFF = Royal Blue
    }

    private int getLabelBgColor(FeatureItem item) {
        if (item.number == 20) return 0x227C3AED;
        if (isImplementationPending(item)) return 0x2264748B;
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
                    showFeatureDetailsDialog(item, position + 1);
                    return true;
                },
                v -> showFeatureDetailsDialog(item, position + 1)
        );
    }

    private void handleFeatureClick(FeatureItem item, int position) {
        NayaConfig cfg = NayaConfig.getInstance();
        if (cfg == null) return;

        if (item.number == 20) {
            showStorageDoctorScanDialog();
            return;
        }
        if (isImplementationPending(item)) {
            showFeatureDetailsDialog(item, position + 1);
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
            case 18: cfg.setDataSaverEnabled(newState); break;
            case 21: cfg.setLinkGuardEnabled(newState); break;
            case 22: cfg.setReadableTextEnabled(newState); break;
            case 23: cfg.setReduceMotion(newState); break;
            case 11: cfg.setShowIdAndDc(newState); break;
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
            cleanBldr.setMessage("Found reclaimable cache:\n- Total Cache: " + report.getFormattedTotal() + "\n- Cached Files: " + report.fileCount + "\nClean cache now to boost NayaGram speed?");
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

    private void showFeatureDetailsDialog(FeatureItem item, int displayNumber) {
        if (context == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(item.getCatchyTitle(currentLang));
        String availability = isImplementationPending(item)
                ? (currentLang == LANG_BN ? "\n\nএই ফিচারটি এখনো কার্যকর নয়। নেটিভ বাস্তবায়ন ও পরীক্ষা শেষ না হওয়া পর্যন্ত এটি চালু করা যাবে না।"
                : (currentLang == LANG_AR ? "\n\nهذه الميزة غير نشطة بعد. لن يتم تفعيلها قبل اكتمال التنفيذ والاختبار."
                : "\n\nNot available yet. It will remain inactive until native implementation and testing are complete."))
                : "";
        builder.setMessage(item.getDesc(currentLang) + availability + "\n\n" +
                "Feature #" + displayNumber + " • NayaGram Exclusive");
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
