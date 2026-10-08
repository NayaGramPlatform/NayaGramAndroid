package org.nayagram.platform.features;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * FeatureListAdapter - NayaGram 17 Exclusive Features Showcase.
 * Premium hero header with logo, multi-language support (English default, Bangla, Arabic),
 * catchy titles, clickable individual Details popup, All Details Pipeline action, and smooth scroll animations.
 */
public class FeatureListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_ITEM = 1;

    public static final int LANG_EN = 0;
    public static final int LANG_BN = 1;
    public static final int LANG_AR = 2;

    public static final String SUPPORT_EMAIL = "support.nayagram@gmail.com";
    private static final String PREF_LANG = "nayagram_feature_lang";

    private final List<FeatureItem> features;
    private final Context context;
    private int currentLanguage = LANG_EN;
    private int lastAnimatedPosition = -1;

    public FeatureListAdapter(Context context) {
        this.context = context;
        this.features = new ArrayList<>();
        loadLanguagePreference();
        initializeFeatures();
    }

    private void loadLanguagePreference() {
        try {
            SharedPreferences sp = context.getSharedPreferences("nayagram_config_prefs", Context.MODE_PRIVATE);
            currentLanguage = sp.getInt(PREF_LANG, LANG_EN);
        } catch (Exception ignored) {
            currentLanguage = LANG_EN;
        }
    }

    public void setLanguage(int lang) {
        this.currentLanguage = lang;
        try {
            SharedPreferences sp = context.getSharedPreferences("nayagram_config_prefs", Context.MODE_PRIVATE);
            sp.edit().putInt(PREF_LANG, lang).apply();
        } catch (Exception ignored) {
        }
        notifyDataSetChanged();
    }

    public int getLanguage() {
        return currentLanguage;
    }

    private void initializeFeatures() {

        // 2. Message Scheduler
        features.add(new FeatureItem(
                2, 0xFF3B82F6, 0xFF3B82F6, R.drawable.outline_message_time_24, "msg_scheduler",
                "Message Scheduler", "Message Scheduler", "Send messages at a specific date & time automatically.",
                "• Purpose: Automated timed delivery.\n• Importance: Powered by Android Jetpack WorkManager. Delivers critical messages even if app is closed or device restarts.\n• How to use: Schedule greetings, announcements, or work notes to send at the exact second.\n• Support: " + SUPPORT_EMAIL,
                "মেসেজ শিডিউলার", "মেসেজ শিডিউলার", "নির্দিষ্ট তারিখ ও সময়ে স্বয়ংক্রিয়ভাবে মেসেজ পাঠান।",
                "• উদ্দেশ্য: স্বয়ংক্রিয় সময়ে মেসেজ ডেলিভারি।\n• গুরুত্ব: অ্যাপ বন্ধ থাকলেও বা ফোন রিস্টার্ট হলেও সঠিক সময়ে মেসেজ চলে যাবে।\n• ব্যবহার: চ্যাটে তারিখ ও সময় সেট করে নিশ্চিন্ত থাকুন।\n• সাপোর্ট: " + SUPPORT_EMAIL,
                "جدولة الرسائل", "جدولة الرسائل", "إرسال الرسائل في تاريخ ووقت محددين تلقائياً.",
                "• الهدف: توصيل الرسائل في الوقت المحدد.\n• الأهمية: يعمل عبر Jetpack WorkManager حتى لو كان التطبيق مغلقاً.\n• الدعم: " + SUPPORT_EMAIL
        ));

        // 3. Smart Auto-Reply
        features.add(new FeatureItem(
                3, 0xFF10B981, 0xFF8B5CF6, R.drawable.ic_naya_robot, "auto_reply",
                "Smart Auto-Reply", "Auto-Reply", "Keyword-based auto-reply with spam filtering & self-reply guard.",
                "• Purpose: Instant smart automated messaging.\n• Importance: Responds to inquiries, business leads, or personal contacts when you are busy or sleeping, with loop prevention.\n• How to use: Set custom keywords or greeting template in settings.\n• Support: " + SUPPORT_EMAIL,
                "স্মার্ট অটো-রিপ্লাই", "অটো-রিপ্লাই", "ব্যস্ত থাকলে স্বয়ংক্রিয় উত্তর ও স্প্যাম ফিল্টারিং ব্যবস্থা।",
                "• উদ্দেশ্য: স্বয়ংক্রিয় বুদ্ধিমান উত্তর।\n• গুরুত্ব: মিটিং বা ঘুমের সময় কাস্টম মেসেজ দিয়ে বন্ধুদের বা ক্লায়েন্টদের স্বয়ংক্রিয় রিপ্লাই দিন।\n• ব্যবহার: সেটিংসে নিজের পছন্দের মেসেজ সেট করুন।\n• সাপোর্ট: " + SUPPORT_EMAIL,
                "الرد التلقائي الذكي", "الرد التلقائي", "رد تلقائي ذكي مع حماية ضد الرسائل المزعجة.",
                "• الهدف: الرد على الرسائل تلقائياً أثناء الانشغال.\n• الأهمية: تصفية الردود ومنع التكرار.\n• الدعم: " + SUPPORT_EMAIL
        ));

        // 4. Story Saver
        features.add(new FeatureItem(
                4, 0xFF8B5CF6, 0xFF10B981, R.drawable.ic_naya_download, "story_saver",
                "Story Saver", "Story Saver", "Download stories (photos & HD videos) in original quality with 1 click.",
                "• Purpose: Direct gallery download.\n• Importance: Saves photos and HD video stories losslessly without re-encoding or watermarking.\n• How to use: Tap the download icon in the top corner of any active story.\n• Support: " + SUPPORT_EMAIL,
                "স্টোরি সেভার", "স্টোরি সেভার", "এক ক্লিকে ফুল HD ভিডিও ও ছবি সরাসরি গ্যালারিতে সেভ করুন।",
                "• উদ্দেশ্য: স্টোরি সরাসরি গ্যালারিতে সেভ।\n• গুরুত্ব: ফুল রেজুলেশন ও সাউন্ড সহ যেকোনো স্টোরি ওয়াটারমার্ক ছাড়া ডাউনলোড করুন।\n• ব্যবহার: স্টোরি প্লে করার সময় ডাউনলোড বাটনে চাপুন।\n• সাপোর্ট: " + SUPPORT_EMAIL,
                "حفظ القصص", "حفظ القصص", "تحميل مقاطع الفيديو والصور من القصص بجودة أصلية.",
                "• الهدف: تنزيل القصص بضغطة زر دون ضغط الجودة.\n• الدعم: " + SUPPORT_EMAIL
        ));

        // 5. Anonymous Story Viewer
        features.add(new FeatureItem(
                5, 0xFF0EA5E9, 0xFF3B82F6, R.drawable.ic_naya_eye_off, "anonymous_stories",
                "Anonymous Story Viewer", "Anonymous Stories", "View friends' stories secretly without ever appearing on viewer lists.",
                "• Purpose: Incognito story viewing.\n• Importance: Browse personal or channel stories without notifying the poster or logging your profile name.\n• How to use: Enable this mode to keep your profile invisible while watching any story.\n• Support: " + SUPPORT_EMAIL,
                "গোপন স্টোরি ভিউয়ার", "গোপন স্টোরি", "বন্ধুদের স্টোরি দেখুন গোপনে, আপনার নাম ভিউয়ার লিস্টে আসবে না।",
                "• উদ্দেশ্য: ছদ্মবেশী স্টোরি ভিউ।\n• গুরুত্ব: স্টোরি দেখা হলেও পোস্টকারীর ভিউয়ার তালিকায় আপনার নাম বা ছবি কখনোই দেখাবে না।\n• ব্যবহার: অন করে নিশ্চিন্তে সবার স্টোরি দেখুন।\n• সাপোর্ট: " + SUPPORT_EMAIL,
                "مشاهدة القصص بتخفٍ", "مشاهدة متخفية", "مشاهدة القصص دون الظهور في قائمة المشاهدين.",
                "• الهدف: مشاهدة القصص بشكل غير مرئي وسري تماماً.\n• الدعم: " + SUPPORT_EMAIL
        ));

        // 7. Forward Without Quote
        features.add(new FeatureItem(
                7, 0xFF6366F1, 0xFF10B981, R.drawable.ic_naya_forward, "forward_no_quote",
                "Forward Without Quote", "Clean Forward", "Forward media or messages without the original sender name.",
                "• Purpose: Anonymous clean sharing.\n• Importance: Strips the sender's username, timestamp, and forwarded header from texts, voice notes, photos, and files.\n• How to use: Select messages and choose Clean Forward from the share dialog.\n• Support: " + SUPPORT_EMAIL,
                "ফরওয়ার্ড উইদাউট কোট", "ক্লিন ফরওয়ার্ড", "প্রেরকের নাম ও ট্যাগ ছাড়াই মেসেজ ও ফাইল ফরওয়ার্ড করুন।",
                "• উদ্দেশ্য: কপিরাইট বা ট্যাগ ছাড়া শেয়ার।\n• গুরুত্ব: মূল প্রেরকের নাম ও প্রোফাইল লিংক মুছে সম্পূর্ণ নতুন মেসেজ হিসেবে ফরওয়ার্ড হবে।\n• ব্যবহার: মেসেজ সিলেক্ট করে ফরওয়ার্ড করার সময় কোট রিমুভ করুন।\n• সাপোর্ট: " + SUPPORT_EMAIL,
                "إعادة التوجيه بدون اسم", "توجيه نظيف", "إعادة إرسال الرسائل والوسائط بدون اسم المرسل الأصلي.",
                "• الهدف: حماية الخصوصية ومشاركة المحتوى بشكل نظيف.\n• الدعم: " + SUPPORT_EMAIL
        ));

        // 8. Voice Transcription
        features.add(new FeatureItem(
                8, 0xFF8B5CF6, 0xFF3B82F6, R.drawable.ic_naya_voice, "voice_transcribe",
                "Voice Transcription", "Voice to Text", "Convert voice messages to searchable text instantly on-device.",
                "• Purpose: Speech-to-text transcription.\n• Importance: Read long voice notes quietly during meetings, in public transit, or when headphones are unavailable.\n• How to use: Tap the 'T' icon beside any incoming voice note.\n• Support: " + SUPPORT_EMAIL,
                "ভয়েস ট্রান্সক্রিপশন", "ভয়েস টু টেক্সট", "যেকোনো ভয়েস মেসেজ সরাসরি টেক্সটে রূপান্তর করুন।",
                "• উদ্দেশ্য: অডিও থেকে লেখায় রূপান্তর।\n• গুরুত্ব: মিটিংয়ে বা ভিড়ে হেডফোন না থাকলে ভয়েস না শুনেই পড়ে নিতে পারবেন।\n• ব্যবহার: অডিও মেসেজের পাশে থাকা টেক্সট আইকনে চাপুন।\n• সাপোর্ট: " + SUPPORT_EMAIL,
                "تحويل الصوت إلى نص", "صوت إلى نص", "تحويل الرسائل الصوتية إلى نصوص قابلة للقراءة والبحث.",
                "• الهدف: قراءة الصوتيات أثناء الاجتماعات أو في الأماكن الهادئة.\n• الدعم: " + SUPPORT_EMAIL
        ));

        // 9. Smart Chat Folders
        features.add(new FeatureItem(
                9, 0xFF10B981, 0xFF8B5CF6, R.drawable.settings_folders, "smart_folders",
                "Smart Chat Folders", "Smart Folders", "Auto-organize personal, group, channels & business chats.",
                "• Purpose: Automated clutter-free tab organization.\n• Importance: Sorts high-volume chats into dedicated tabs: Personal, Groups, Channels, and Bots.\n• How to use: Enjoy clean separated feeds right at the top of your chat list.\n• Support: " + SUPPORT_EMAIL,
                "স্মার্ট চ্যাট ফোল্ডার", "স্মার্ট ফোল্ডার", "ব্যক্তিগত, গ্রুপ এবং চ্যানেল চ্যাট আলাদা ট্যাবে স্বয়ংক্রিয়ভাবে সাজান।",
                "• উদ্দেশ্য: স্বয়ংক্রিয় চ্যাট অর্গানাইজেশন।\n• গুরুত্ব: ব্যক্তিগত চ্যাট, গ্রুপ, চ্যানেল এবং বট আলাদা আলাদা ট্যাবে সুন্দরভাবে বিভক্ত থাকে।\n• ব্যবহার: হোম স্ক্রিনের ওপরে ট্যাবগুলো সরাসরি পেয়ে যাবেন।\n• সাপোর্ট: " + SUPPORT_EMAIL,
                "مجلدات المحادثات الذكية", "مجلدات ذكية", "تنظيم المحادثات تلقائياً (شخصي، مجموعات، قنوات).",
                "• الهدف: تصفح سهل ومنظم لجميع محادثاتك.\n• الدعم: " + SUPPORT_EMAIL
        ));

        // 10. Call & Voice Protection
        features.add(new FeatureItem(
                10, 0xFF3B82F6, 0xFF10B981, R.drawable.outline_profile_call_24, "call_protection",
                "Call & Voice Protection", "Call Guard", "Prevent accidental calls or voice sends with a confirmation dialog.",
                "• Purpose: Pocket-call & audio accident prevention.\n• Importance: Confirms with a popup dialog before placing calls or transmitting instant voice messages.\n• How to use: Eliminates unwanted accidental calls with a single safety tap.\n• Support: " + SUPPORT_EMAIL,
                "কল ও ভয়েস প্রোটেকশন", "কল গার্ড", "ভুল করে কল বা অডিও পাঠানো রোধে নিশ্চিতকরণ পপআপ।",
                "• উদ্দেশ্য: অনাকাঙ্ক্ষিত কল ও অডিও দুর্ঘটনা রোধ।\n• গুরুত্ব: পকেটে বা অসাবধানতায় কল বাটনে চাপ পড়লেও সরাসরি ডায়াল হবে না, পারমিশন চাইবে।\n• ব্যবহার: ফিচারটি চালু রাখলেই এটি স্বয়ংক্রিয়ভাবে পাহারা দেবে।\n• সাপোর্ট: " + SUPPORT_EMAIL,
                "حماية المكالمات والصوت", "حارس الاتصال", "منع المكالمات والتسجيلات غير المقصودة بتأكيد فوري.",
                "• الهدف: تجنب الاتصالات الخاطئة والمحرجة.\n• الدعم: " + SUPPORT_EMAIL
        ));

        // 11. User ID & DC Display
        features.add(new FeatureItem(
                11, 0xFF8B5CF6, 0xFF3B82F6, R.drawable.outline_profile_member_24, "show_id_dc",
                "User ID & DC Display", "User ID & DC", "See your Telegram numeric ID and which data center (DC1-DC5) you're on.",
                "• Purpose: Real-time diagnostic insights.\n• Importance: Reveals your permanent account ID and data center assignment (e.g., DC4 Amsterdam, DC5 Singapore).\n• How to use: Viewable instantly under your profile settings header.\n• Support: " + SUPPORT_EMAIL,
                "ইউজার আইডি ও ডিসি ডিসপ্লে", "আইডি ও ডিসি", "আপনার স্থায়ী আইডি এবং কোন টেলিগ্রাম সার্ভারে আছেন তা দেখুন।",
                "• উদ্দেশ্য: অ্যাকাউন্ট ডায়াগনস্টিক।\n• গুরুত্ব: আপনার পার্মানেন্ট টেলিগ্রাম ইউজার আইডি এবং সার্ভার ডাটা সেন্টার (DC1-DC5) সরাসরি প্রোফাইলে প্রদর্শিত হয়।\n• ব্যবহার: প্রোফাইলে প্রবেশ করলেই দেখতে পাবেন।\n• সাপোর্ট: " + SUPPORT_EMAIL,
                "معرف المستخدم ومركز البيانات", "المعرف وDC", "عرض معرف حسابك ومركز البيانات الخاص بك (DC1-DC5).",
                "• الهدف: معرفة تفاصيل الخادم وتحديد الهوية الرقمية.\n• الدعم: " + SUPPORT_EMAIL
        ));

        // 12. Modular NayaConfig
        features.add(new FeatureItem(
                12, 0xFF10B981, 0xFF8B5CF6, R.drawable.settings_features, "modular_config",
                "Modular NayaConfig", "Modular Config", "All custom settings isolated in an independent, safe package.",
                "• Purpose: Robust core architecture.\n• Importance: All proprietary logic is encapsulated within org.nayagram.platform, ensuring zero core crashes and effortless updates.\n• How to use: Seamless background operation.\n• Support: " + SUPPORT_EMAIL,
                "মডুলার নয়াকনফিগ", "মডুলার কনফিগ", "সব বিশেষ ফিচার পৃথক ও নিরাপদ প্যাকেজে সুরক্ষিত থাকে।",
                "• উদ্দেশ্য: স্থিতিশীল কোড আর্কিটেকচার।\n• গুরুত্ব: টেলিগ্রামের মূল কোড অক্ষুণ্ণ রেখে org.nayagram.platform প্যাকেজে সম্পূর্ণ নিরাপদভাবে চলে, ক্র্যাশ শূন্য।\n• ব্যবহার: সম্পূর্ণ স্বয়ংক্রিয় কোর আর্কিটেকচার।\n• সাপোর্ট: " + SUPPORT_EMAIL,
                "إعدادات نايا المعيارية", "إعدادات مستقلة", "عزل جميع الميزات الخاصة في حزمة برمجية آمنة ومستقلة.",
                "• الهدف: ضمان استقرار وسرعة التطبيق دون أخطاء.\n• الدعم: " + SUPPORT_EMAIL
        ));

        // 13. Digital Wellbeing & Focus Mode
        features.add(new FeatureItem(
                13, 0xFF3B82F6, 0xFF10B981, R.drawable.ic_naya_lotus, "focus_mode",
                "Digital Wellbeing & Focus Mode", "Focus Mode", "Silence non-urgent notifications. VIP contacts bypass (family & important).",
                "• Purpose: Distraction-free productivity.\n• Importance: Silences non-critical group alerts while whitelisting VIP family and urgent contacts to ring through.\n• How to use: Activate Focus Mode during study, work, or prayer hours.\n• Support: " + SUPPORT_EMAIL,
                "ডিজিটাল ওয়েলবিয়িং ও ফোকাস মোড", "ফোকাস মোড", "কাজের সময় অপ্রয়োজনীয় নোটিফিকেশন বন্ধ রাখুন, ভিআইপি কল চালু থাকবে।",
                "• উদ্দেশ্য: নিরবচ্ছিন্ন মনোযোগ ও ইবাদতের সময়।\n• গুরুত্ব: গ্রুপ নোটিফিকেশন সাইলেন্ট থাকবে, কিন্তু পরিবার ও গুরুত্বপূর্ণ কন্টাক্ট থেকে মেসেজ এলে বাজবে।\n• ব্যবহার: কাজের শুরুতে ১-ট্যাপে অন করুন।\n• সাপোর্ট: " + SUPPORT_EMAIL,
                "الراحة الرقمية ووضع التركيز", "وضع التركيز", "كتم الإشعارات غير الضرورية مع استثناء جهات الاتصال المهمة.",
                "• الهدف: تركيز أعلى وإنتاجية بدون تشتيت أثناء العمل والدراسة.\n• الدعم: " + SUPPORT_EMAIL
        ));

        // 14. Smart Storage Doctor
        features.add(new FeatureItem(
                14, 0xFF8B5CF6, 0xFF3B82F6, R.drawable.ic_naya_broom, "storage_doctor",
                "Smart Storage Doctor", "Storage Doctor", "Scan & clean Telegram cache and duplicate media with one tap.",
                "• Purpose: Storage optimization & decluttering.\n• Importance: Scans and purges cached duplicate videos and photos without deleting any cloud chat history.\n• How to use: Tap 'Scan & Clean' to instantly reclaim gigabytes of device memory.\n• Support: " + SUPPORT_EMAIL,
                "স্মার্ট স্টোরেজ ডক্টর", "স্টোরেজ ডক্টর", "১-ট্যাপে ডুপ্লিকেট ক্যাশ ফাইল পরিষ্কার করে মেমরি খালি করুন।",
                "• উদ্দেশ্য: ফোন মেমরি ফ্রি করা।\n• গুরুত্ব: ক্লাউড চ্যাট না মুছেই ফোনের মেমরি দখল করে রাখা অপ্রয়োজনীয় ক্যাশ ফাইল ১ ক্লিকে ডিলিট করে।\n• ব্যবহার: ক্লিন বাটনে ট্যাপ করে নিমেষেই গিগাবাইট জায়গা খালি করুন।\n• সাপোর্ট: " + SUPPORT_EMAIL,
                "طبيب الذاكرة الذكي", "تنظيف الذاكرة", "فحص وحذف الملفات المكررة والمؤقتة بنقرة واحدة.",
                "• الهدف: توفير مساحة تخزين ضخمة وتسريع الهاتف.\n• الدعم: " + SUPPORT_EMAIL
        ));

        // 15. Ultra Battery & Low-Data Saver
        features.add(new FeatureItem(
                15, 0xFF10B981, 0xFF8B5CF6, R.drawable.settings_power, "battery_saver",
                "Ultra Battery & Low-Data Saver", "Battery Saver", "Below 20% charge, auto-pause video & heavy animations. Save 30-40% battery.",
                "• Purpose: Extreme energy conservation.\n• Importance: Automatically suspends background sync, animated stickers, and video autoplay when battery is low.\n• How to use: Automatically monitors battery percentage or force enable anytime.\n• Support: " + SUPPORT_EMAIL,
                "আল্ট্রা ব্যাটারি ও ডাটা সেভার", "ব্যাটারি সেভার", "চার্জ ২০% এর নিচে নামলে এনিমেশন বন্ধ করে ৩০-৪০% চার্জ বাঁচায়।",
                "• উদ্দেশ্য: চরম ব্যাটারি ও ইন্টারনেট সেভিং।\n• গুরুত্ব: চার্জ কম থাকলে স্বয়ংক্রিয়ভাবে ভিডিও অটো-প্লে ও ভারী স্টিকার বন্ধ করে দেয়।\n• ব্যবহার: এটি ফোনে চার্জের মাত্রা দেখে নিজে থেকেই এক্টিভ হয়।\n• সাপোর্ট: " + SUPPORT_EMAIL,
                "توفير فائق للبطارية والبيانات", "توفير البطارية", "إيقاف الفيديوهات والرسوم تلقائياً عند انخفاض البطارية تحت 20%.",
                "• الهدف: توفير ما يصل إلى 40% من استهلاك البطارية والإنترنت.\n• الدعم: " + SUPPORT_EMAIL
        ));

        // 16. Biometric Chat Locker
        features.add(new FeatureItem(
                16, 0xFF3B82F6, 0xFF10B981, R.drawable.ic_naya_fingerprint, "biometric_locker",
                "Biometric Chat Locker", "Chat Locker", "Lock only sensitive chats with fingerprint & passcode. Keep privacy safe.",
                "• Purpose: Selective cryptographic chat locking.\n• Importance: Lock individual secret chats with fingerprint/PIN while general chats remain quickly accessible.\n• How to use: Long press any chat -> Lock with Fingerprint.\n• Support: " + SUPPORT_EMAIL,
                "বায়োমেট্রিক চ্যাট লকার", "চ্যাট লকার", "আঙুলের ছাপ ও পিন কোড দিয়ে গোপন চ্যাট সুরক্ষিত রাখুন।",
                "• উদ্দেশ্য: নির্দিষ্ট চ্যাটে বায়োমেট্রিক তালা।\n• গুরুত্ব: পুরো অ্যাপ লক না করেও শুধু আপনার গোপন চ্যাটগুলো ফিঙ্গারপ্রিন্ট দিয়ে লক করে রাখা যায়।\n• ব্যবহার: যেকোনো চ্যাটে চেপে ধরে ফিঙ্গারপ্রিন্ট লক সেট করুন।\n• সাপোর্ট: " + SUPPORT_EMAIL,
                "قفل المحادثات بالبصمة", "قفل المحادثات", "قفل المحادثات الحساسة ببصمة الإصبع أو رمز المرور.",
                "• الهدف: حماية محادثاتك السرية بنظام أمان بيومتري فوري.\n• الدعم: " + SUPPORT_EMAIL
        ));

        // 17. In-Chat Instant Translator
        features.add(new FeatureItem(
                17, 0xFF3B82F6, 0xFF8B5CF6, R.drawable.outline_ai_translate2, "instant_translator",
                "In-Chat Instant Translator", "Instant Translator", "Translate any foreign language (English, Arabic, Urdu, etc.) to fluent Bangla with 1 tap.",
                "• Purpose: Zero-barrier multilingual chatting.\n• Importance: Translates foreign language messages in-place without copying to third-party translation apps.\n• How to use: Tap translate on any message bubble to read in clear Bangla.\n• Support: " + SUPPORT_EMAIL,
                "ইন-চ্যাট ইনস্ট্যান্ট অনুবাদক", "ইনস্ট্যান্ট অনুবাদক", "যেকোনো বিদেশি ভাষা সরাসরি চ্যাট স্ক্রিনেই বাংলায় অনুবাদ করুন।",
                "• উদ্দেশ্য: তাৎক্ষণিক অনুবাদ।\n• গুরুত্ব: ইংরেজি, আরবি, উর্দু বা যেকোনো বিদেশি ভাষা চ্যাট থেকে বের না হয়েই ১-ট্যাপে বাংলায় পড়ে নেওয়া যায়।\n• ব্যবহার: মেসেজের ওপর ট্যাপ করে ট্রান্সলেটে চাপুন।\n• সাপোর্ট: " + SUPPORT_EMAIL,
                "المترجم الفوري داخل المحادثة", "المترجم الفوري", "ترجمة أي لغة أجنبية إلى لغتك الأم بضغطة زر واحدة.",
                "• الهدف: محادثة سلسة وتخطي حاجز اللغات داخل الشات مباشرة.\n• الدعم: " + SUPPORT_EMAIL
        ));
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder hvh = new HeaderViewHolder(context);
            hvh.setAllDetailsClickListener(v -> showAllDetailsPipelineDialog());
            hvh.setLanguageChangeListener(lang -> setLanguage(lang));
            return hvh;
        } else {
            FrameLayout itemView = new FrameLayout(context);
            itemView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return new FeatureListViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FeatureListViewHolder) {
            FeatureItem feature = features.get(position - 1);
            FeatureListViewHolder vh = (FeatureListViewHolder) holder;
            vh.bind(feature, position - 1, currentLanguage);
            vh.setDetailsClickListener(v -> showSingleFeatureDialog(feature));
            applySlideAnimation(vh.itemView, position);
        } else if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(currentLanguage);
        }
    }

    private void applySlideAnimation(View view, int position) {
        if (position > lastAnimatedPosition) {
            view.setTranslationY(AndroidUtilities.dp(35));
            view.setAlpha(0.0f);
            view.animate()
                    .translationY(0f)
                    .alpha(1.0f)
                    .setDuration(320)
                    .setInterpolator(new DecelerateInterpolator(1.4f))
                    .setStartDelay(Math.min(position * 30L, 200L))
                    .start();
            lastAnimatedPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
    }

    @Override
    public int getItemCount() {
        return features.size() + 1; // +1 for Header
    }

    public void showSingleFeatureDialog(FeatureItem feature) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(feature.number + ". " + feature.getTitle(currentLanguage));
        builder.setMessage(feature.getDetails(currentLanguage));
        builder.setPositiveButton(currentLanguage == LANG_BN ? "বুঝেছি" : (currentLanguage == LANG_AR ? "موافق" : "Understood"), null);
        builder.show();
    }

    public void showAllDetailsPipelineDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (currentLanguage == LANG_BN) {
            builder.setTitle("নয়াগ্রাম প্ল্যাটফর্মের পূর্ণাঙ্গ ফিচার ও সেটিংস পাইপলাইন");
            String pipeline =
                    "✦ নয়গ্রাম প্ল্যাটফর্ম আর্কিটেকচার ও ব্যবহারবিধি ✦\n\n" +
                    "১. প্রাইভেসি ও অদৃশ্যকরণ পাইপলাইন:\n" +
                    "   • ঘোস্ট মোড -> সম্পূর্ণ অদৃশ্য অনলাইন স্ট্যাটাস ও টাইপিং\n" +
                    "   • গোপন স্টোরি -> ভিউয়ার লিস্টে না এসে স্টোরি দেখা\n" +
                    "   • অ্যান্টি-ডিলিট -> ডিলিট হওয়া মেসেজ লোকাল মেমরিতে সুরক্ষিত\n\n" +
                    "২. স্মার্ট মেসেজিং পাইপলাইন:\n" +
                    "   • মেসেজ শিডিউলার -> নির্দিষ্ট সময়ে স্বয়ংক্রিয় ডেলিভারি\n" +
                    "   • অটো-রিপ্লাই -> কিওয়ার্ডভিত্তিক বুদ্ধিমান উত্তর\n" +
                    "   • ক্লিন ফরওয়ার্ড -> প্রেরকের নাম ছাড়া ফ্রেশ ফরওয়ার্ড\n" +
                    "   • ভয়েস টু টেক্সট -> মুহূর্তের মধ্যে ভয়েস থেকে লেখা\n" +
                    "   • ইনস্ট্যান্ট অনুবাদক -> চ্যাট স্ক্রিনেই বাংলায় অনুবাদ\n\n" +
                    "৩. সিস্টেম ও পারফরম্যান্স পাইপলাইন:\n" +
                    "   • ফোকাস মোড -> কাজের সময় নোটিফিকেশন বন্ধ, ভিআইপি কল চালু\n" +
                    "   • স্টোরেজ ডক্টর -> অপ্রয়োজনীয় ক্যাশ ফাইল পরিষ্কার\n" +
                    "   • ব্যাটারি সেভার -> ২০% নিচে ৩০-৪০% চার্জ সাশ্রয়\n" +
                    "   • মডুলার নয়াকনফিগ -> নিরাপদ স্বাধীন কোর প্যাকেজ\n\n" +
                    "৪. নিরাপত্তা ও অ্যাকাউন্ট পাইপলাইন:\n" +
                    "   • বায়োমেট্রিক চ্যাট লকার -> আঙুলের ছাপে গোপন চ্যাট লক\n" +
                    "   • কল গার্ড -> ভুল করে কল যাওয়া রোধে নিশ্চয়তা পপআপ\n" +
                    "   • ইউজার আইডি ও ডিসি -> টেলিগ্রাম ডাটা সেন্টার তথ্য\n\n" +
                    "অফিসিয়াল সাপোর্ট: " + SUPPORT_EMAIL;
            builder.setMessage(pipeline);
            builder.setPositiveButton("বন্ধ করুন", null);
        } else {
            builder.setTitle("NayaGram Exclusive Features Pipeline");
            String pipeline =
                    "✦ NayaGram Complete Architecture & Pipeline ✦\n\n" +
                    "1. STEALTH & PRIVACY PIPELINE:\n" +
                    "   • Ghost Mode -> Invisible online status & typing\n" +
                    "   • Anonymous Stories -> Undetected story browsing\n" +
                    "   • Anti-Delete -> Encrypted SQLite message safety\n\n" +
                    "2. SMART MESSAGING PIPELINE:\n" +
                    "   • Message Scheduler -> Android WorkManager delivery\n" +
                    "   • Smart Auto-Reply -> Keyword auto-responder\n" +
                    "   • Clean Forward -> Forward without original quotes\n" +
                    "   • Voice Transcription -> Instant on-device speech-to-text\n" +
                    "   • Instant Translator -> 1-tap translation into Bangla\n\n" +
                    "3. PERFORMANCE & SYSTEM PIPELINE:\n" +
                    "   • Focus Mode -> VIP contact bypass & silence alerts\n" +
                    "   • Storage Doctor -> Fast 1-tap cache & duplicate cleanup\n" +
                    "   • Battery Saver -> Conserves 30-40% energy below 20%\n" +
                    "   • Modular Config -> Isolated org.nayagram.platform\n\n" +
                    "4. SECURITY & IDENTITY PIPELINE:\n" +
                    "   • Biometric Chat Locker -> Fingerprint-locked chats\n" +
                    "   • Call Guard -> Pocket-call confirmation dialog\n" +
                    "   • User ID & DC Display -> Server datacenter diagnostic\n\n" +
                    "Official Support: " + SUPPORT_EMAIL;
            builder.setMessage(pipeline);
            builder.setPositiveButton("Close", null);
        }
        builder.show();
    }

    public static class FeatureItem {
        public int number;
        public int accentColor;
        public int numberBadgeColor;
        public int iconRes;
        public String configKey;

        public String titleEn;
        public String catchyEn;
        public String descEn;
        public String detailsEn;

        public String titleBn;
        public String catchyBn;
        public String descBn;
        public String detailsBn;

        public String titleAr;
        public String catchyAr;
        public String descAr;
        public String detailsAr;

        public FeatureItem(int number, int accentColor, int numberBadgeColor, int iconRes, String configKey,
                           String titleEn, String catchyEn, String descEn, String detailsEn,
                           String titleBn, String catchyBn, String descBn, String detailsBn,
                           String titleAr, String catchyAr, String descAr, String detailsAr) {
            this.number = number;
            this.accentColor = accentColor;
            this.numberBadgeColor = numberBadgeColor;
            this.iconRes = iconRes;
            this.configKey = configKey;

            this.titleEn = titleEn;
            this.catchyEn = catchyEn;
            this.descEn = descEn;
            this.detailsEn = detailsEn;

            this.titleBn = titleBn;
            this.catchyBn = catchyBn;
            this.descBn = descBn;
            this.detailsBn = detailsBn;

            this.titleAr = titleAr;
            this.catchyAr = catchyAr;
            this.descAr = descAr;
            this.detailsAr = detailsAr;
        }

        public String getTitle(int lang) {
            if (lang == LANG_BN && titleBn != null) return titleBn;
            if (lang == LANG_AR && titleAr != null) return titleAr;
            return titleEn;
        }

        public String getCatchyTitle(int lang) {
            if (lang == LANG_BN && catchyBn != null) return catchyBn;
            if (lang == LANG_AR && catchyAr != null) return catchyAr;
            return catchyEn != null ? catchyEn : titleEn;
        }

        public String getDesc(int lang) {
            if (lang == LANG_BN && descBn != null) return descBn;
            if (lang == LANG_AR && descAr != null) return descAr;
            return descEn;
        }

        public String getDetails(int lang) {
            if (lang == LANG_BN && detailsBn != null) return detailsBn;
            if (lang == LANG_AR && detailsAr != null) return detailsAr;
            return detailsEn;
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout root;
        private final TextView allDetailsBtn;
        private final TextView badge;
        private final TextView tagText;
        private final TextView subText;
        private final TextView tabEn;
        private final TextView tabBn;
        private final TextView tabAr;
        private OnLanguageChangeListener languageChangeListener;

        public interface OnLanguageChangeListener {
            void onLanguageChanged(int lang);
        }

        public HeaderViewHolder(Context context) {
            super(new FrameLayout(context));
            root = (FrameLayout) itemView;
            root.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(8));

            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER_HORIZONTAL);
            card.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18));

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            cardBg.setCornerRadius(AndroidUtilities.dp(22));
            cardBg.setStroke(AndroidUtilities.dp(1), 0x1A000000);
            card.setBackground(cardBg);

            // Language Selector Bar
            LinearLayout langRow = new LinearLayout(context);
            langRow.setOrientation(LinearLayout.HORIZONTAL);
            langRow.setGravity(Gravity.CENTER);
            langRow.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(3), AndroidUtilities.dp(4), AndroidUtilities.dp(3));
            GradientDrawable langBg = new GradientDrawable();
            langBg.setColor(0x12000000);
            langBg.setCornerRadius(AndroidUtilities.dp(16));
            langRow.setBackground(langBg);

            tabEn = createLangTab(context, "English");
            tabBn = createLangTab(context, "বাংলা");
            tabAr = createLangTab(context, "العربية");

            tabEn.setOnClickListener(v -> { if (languageChangeListener != null) languageChangeListener.onLanguageChanged(LANG_EN); });
            tabBn.setOnClickListener(v -> { if (languageChangeListener != null) languageChangeListener.onLanguageChanged(LANG_BN); });
            tabAr.setOnClickListener(v -> { if (languageChangeListener != null) languageChangeListener.onLanguageChanged(LANG_AR); });

            langRow.addView(tabEn);
            langRow.addView(tabBn);
            langRow.addView(tabAr);

            LinearLayout.LayoutParams lrParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lrParams.bottomMargin = AndroidUtilities.dp(12);
            card.addView(langRow, lrParams);

            // Logo removed from this showcase card as requested (kept only for main features)

            // Title
            TextView title = new TextView(context);
            title.setText("NayaGram");
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
            title.setTypeface(AndroidUtilities.bold());
            title.setTextColor(0xFF0284C7);
            card.addView(title);

            // Subtitle
            subText = new TextView(context);
            subText.setText("More Privacy • More Control • A Smarter Chat");
            subText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            subText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            subParams.topMargin = AndroidUtilities.dp(3);
            subParams.bottomMargin = AndroidUtilities.dp(10);
            card.addView(subText, subParams);

            // Pill Badge
            badge = new TextView(context);
            badge.setText("✨ NayaGram Exclusive Features ✨");
            badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            badge.setTypeface(AndroidUtilities.bold());
            badge.setTextColor(Color.WHITE);
            badge.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(4));
            GradientDrawable badgeBg = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFF0284C7, 0xFF8B5CF6}
            );
            badgeBg.setCornerRadius(AndroidUtilities.dp(18));
            badge.setBackground(badgeBg);
            card.addView(badge);

            // Tagline
            tagText = new TextView(context);
            tagText.setText("Your Chat, Your Rules");
            tagText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            tagText.setTypeface(AndroidUtilities.bold());
            tagText.setTextColor(0xFF6366F1);
            LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tagParams.topMargin = AndroidUtilities.dp(8);
            tagParams.bottomMargin = AndroidUtilities.dp(12);
            card.addView(tagText, tagParams);

            // Pipeline Button
            allDetailsBtn = new TextView(context);
            allDetailsBtn.setText("📋 All Details & Feature Pipeline");
            allDetailsBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            allDetailsBtn.setTypeface(AndroidUtilities.bold());
            allDetailsBtn.setTextColor(Color.WHITE);
            allDetailsBtn.setGravity(Gravity.CENTER);
            allDetailsBtn.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(9), AndroidUtilities.dp(16), AndroidUtilities.dp(9));
            GradientDrawable btnBg = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFF3B82F6, 0xFF10B981}
            );
            btnBg.setCornerRadius(AndroidUtilities.dp(12));
            allDetailsBtn.setBackground(btnBg);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            card.addView(allDetailsBtn, btnParams);

            root.addView(card, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        }

        private TextView createLangTab(Context context, String text) {
            TextView tab = new TextView(context);
            tab.setText(text);
            tab.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            tab.setTypeface(AndroidUtilities.bold());
            tab.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
            return tab;
        }

        public void bind(int currentLang) {
            updateTabStyle(tabEn, currentLang == LANG_EN);
            updateTabStyle(tabBn, currentLang == LANG_BN);
            updateTabStyle(tabAr, currentLang == LANG_AR);

            if (currentLang == LANG_BN) {
                badge.setText("✨ নয়াগ্রাম এক্সক্লুসিভ ফিচার ও সেটিংস ✨");
                subText.setText("গোপনীয়তা • স্বাচ্ছন্দ্য • গতিময় স্মার্ট চ্যাট");
                tagText.setText("আপনার চ্যাট, আপনার নিয়ম");
                allDetailsBtn.setText("📋 সব ফিচার ও ব্যবহারের পাইপলাইন");
            } else if (currentLang == LANG_AR) {
                badge.setText("✨ ميزات ناياجرام الحصرية ✨");
                subText.setText("أكثر خصوصية • أكثر تحكماً • شات أذكى");
                tagText.setText("محادثاتك، قواعدك الخاصة");
                allDetailsBtn.setText("📋 تفاصيل جميع الميزات وخريطة العمل");
            } else {
                badge.setText("✨ NayaGram Exclusive Features ✨");
                subText.setText("More Privacy • More Control • A Smarter Chat");
                tagText.setText("Your Chat, Your Rules");
                allDetailsBtn.setText("📋 All Details & Feature Pipeline");
            }
        }

        private void updateTabStyle(TextView tab, boolean selected) {
            if (selected) {
                GradientDrawable selBg = new GradientDrawable();
                selBg.setColor(0xFF0284C7);
                selBg.setCornerRadius(AndroidUtilities.dp(12));
                tab.setBackground(selBg);
                tab.setTextColor(Color.WHITE);
            } else {
                tab.setBackground(null);
                tab.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            }
        }

        public void setAllDetailsClickListener(View.OnClickListener listener) {
            allDetailsBtn.setOnClickListener(listener);
        }

        public void setLanguageChangeListener(OnLanguageChangeListener listener) {
            this.languageChangeListener = listener;
        }
    }
}
