package org.nayagram.platform.features;

import android.content.Context;
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

import org.nayagram.platform.NayaConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * FeatureListAdapter - NayaGram 17 Exclusive Features Showcase.
 * Premium hero header with logo, catchy titles, bold typography,
 * individual Details popup, All Details Pipeline action, and smooth scroll animations.
 */
public class FeatureListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_ITEM = 1;

    public static final String SUPPORT_EMAIL = "support.nayagram@gmail.com";

    private final List<FeatureItem> features;
    private final Context context;
    private int lastAnimatedPosition = -1;

    public FeatureListAdapter(Context context) {
        this.context = context;
        this.features = new ArrayList<>();
        initializeFeatures();
    }

    private void initializeFeatures() {
        // 1. Ghost Mode
        features.add(new FeatureItem(
                "Ghost Mode",
                "Ghost Mode",
                "Hide typing status & online presence. Browse in stealth mode.",
                "• Purpose: 100% stealth privacy.
• Importance: Your contacts will never see you 'Online', typing actions are concealed, and read receipts (blue ticks) are paused.
• How to use: Toggle on to browse contacts, read channels, and check messages incognito.
• Support: " + SUPPORT_EMAIL,
                1,
                0xFF6366F1, // Purple/Indigo
                0xFF10B981, // Green
                R.drawable.ic_naya_ghost,
                "ghost_mode"
        ));

        // 2. Message Scheduler
        features.add(new FeatureItem(
                "Message Scheduler",
                "Message Scheduler",
                "Send messages at a specific date & time automatically.",
                "• Purpose: Automated timed delivery.
• Importance: Powered by Android Jetpack WorkManager. Delivers critical messages even if app is closed or device restarts.
• How to use: Schedule greetings, announcements, or work notes to send at the exact second.
• Support: " + SUPPORT_EMAIL,
                2,
                0xFF3B82F6, // Blue
                0xFF3B82F6, // Blue
                R.drawable.outline_message_time_24,
                "msg_scheduler"
        ));

        // 3. Smart Auto-Reply
        features.add(new FeatureItem(
                "Smart Auto-Reply",
                "Auto-Reply",
                "Keyword-based auto-reply with spam filtering & self-reply guard.",
                "• Purpose: Instant smart automated messaging.
• Importance: Responds to inquiries, business leads, or personal contacts when you are busy or sleeping, with loop prevention.
• How to use: Set custom keywords or greeting template in settings.
• Support: " + SUPPORT_EMAIL,
                3,
                0xFF10B981, // Teal/Green
                0xFF8B5CF6, // Purple
                R.drawable.ic_naya_robot,
                "auto_reply"
        ));

        // 4. Story Saver
        features.add(new FeatureItem(
                "Story Saver",
                "Story Saver",
                "Download stories (photos & HD videos) in original quality with 1 click.",
                "• Purpose: Direct gallery download.
• Importance: Saves photos and HD video stories losslessly without re-encoding or watermarking.
• How to use: Tap the download icon in the top corner of any active story.
• Support: " + SUPPORT_EMAIL,
                4,
                0xFF8B5CF6, // Purple
                0xFF10B981, // Green
                R.drawable.ic_naya_download,
                "story_saver"
        ));

        // 5. Anonymous Story Viewer
        features.add(new FeatureItem(
                "Anonymous Story Viewer",
                "Anonymous Stories",
                "View friends' stories secretly without ever appearing on viewer lists.",
                "• Purpose: Incognito story viewing.
• Importance: Browse personal or channel stories without notifying the poster or logging your profile name.
• How to use: Enable this mode to keep your profile invisible while watching any story.
• Support: " + SUPPORT_EMAIL,
                5,
                0xFF0EA5E9, // Sky Blue
                0xFF3B82F6, // Blue
                R.drawable.ic_naya_eye_off,
                "anonymous_stories"
        ));

        // 6. Anti-Delete Recovery
        features.add(new FeatureItem(
                "Anti-Delete Recovery",
                "Anti-Delete",
                "Keep deleted messages safely stored in local encrypted SQLite.",
                "• Purpose: Chat recovery protection.
• Importance: When a sender revokes or deletes messages, NayaGram retains an encrypted copy in your local SQLite so context is never lost.
• How to use: Works automatically in background. Deleted messages show with a shield marker.
• Support: " + SUPPORT_EMAIL,
                6,
                0xFF10B981, // Green
                0xFF8B5CF6, // Purple
                R.drawable.outline_shield_check,
                "anti_delete"
        ));

        // 7. Forward Without Quote
        features.add(new FeatureItem(
                "Forward Without Quote",
                "Clean Forward",
                "Forward media or messages without the original sender name.",
                "• Purpose: Anonymous clean sharing.
• Importance: Strips the sender's username, timestamp, and forwarded header from texts, voice notes, photos, and files.
• How to use: Select messages and choose Clean Forward from the share dialog.
• Support: " + SUPPORT_EMAIL,
                7,
                0xFF6366F1, // Purple
                0xFF10B981, // Green
                R.drawable.ic_naya_forward,
                "forward_no_quote"
        ));

        // 8. Voice Transcription
        features.add(new FeatureItem(
                "Voice Transcription",
                "Voice to Text",
                "Convert voice messages to searchable text instantly on-device.",
                "• Purpose: Speech-to-text transcription.
• Importance: Read long voice notes quietly during meetings, in public transit, or when headphones are unavailable.
• How to use: Tap the 'T' icon beside any incoming voice note.
• Support: " + SUPPORT_EMAIL,
                8,
                0xFF8B5CF6, // Purple
                0xFF3B82F6, // Blue
                R.drawable.ic_naya_voice,
                "voice_transcribe"
        ));

        // 9. Smart Chat Folders
        features.add(new FeatureItem(
                "Smart Chat Folders",
                "Smart Folders",
                "Auto-organize personal, group, channels & business chats.",
                "• Purpose: Automated clutter-free tab organization.
• Importance: Sorts high-volume chats into dedicated tabs: Personal, Groups, Channels, and Bots.
• How to use: Enjoy clean separated feeds right at the top of your chat list.
• Support: " + SUPPORT_EMAIL,
                9,
                0xFF10B981, // Teal
                0xFF8B5CF6, // Purple
                R.drawable.settings_folders,
                "smart_folders"
        ));

        // 10. Call & Voice Protection
        features.add(new FeatureItem(
                "Call & Voice Protection",
                "Call Guard",
                "Prevent accidental calls or voice sends with a confirmation dialog.",
                "• Purpose: Pocket-call & audio accident prevention.
• Importance: Confirms with a popup dialog before placing calls or transmitting instant voice messages.
• How to use: Eliminates unwanted accidental calls with a single safety tap.
• Support: " + SUPPORT_EMAIL,
                10,
                0xFF3B82F6, // Blue
                0xFF10B981, // Green
                R.drawable.outline_profile_call_24,
                "call_protection"
        ));

        // 11. User ID & DC Display
        features.add(new FeatureItem(
                "User ID & DC Display",
                "User ID & DC",
                "See your Telegram numeric ID and which data center (DC1-DC5) you're on.",
                "• Purpose: Real-time diagnostic insights.
• Importance: Reveals your permanent account ID and data center assignment (e.g., DC4 Amsterdam, DC5 Singapore).
• How to use: Viewable instantly under your profile settings header.
• Support: " + SUPPORT_EMAIL,
                11,
                0xFF8B5CF6, // Purple
                0xFF3B82F6, // Blue
                R.drawable.outline_profile_member_24,
                "show_id_dc"
        ));

        // 12. Modular NayaConfig
        features.add(new FeatureItem(
                "Modular NayaConfig",
                "Modular Config",
                "All custom settings isolated in an independent, safe package.",
                "• Purpose: Robust core architecture.
• Importance: All proprietary logic is encapsulated within org.nayagram.platform, ensuring zero core crashes and effortless updates.
• How to use: Seamless background operation.
• Support: " + SUPPORT_EMAIL,
                12,
                0xFF10B981, // Teal
                0xFF8B5CF6, // Purple
                R.drawable.settings_features,
                "modular_config"
        ));

        // 13. Digital Wellbeing & Focus Mode
        features.add(new FeatureItem(
                "Digital Wellbeing & Focus Mode",
                "Focus Mode",
                "Silence non-urgent notifications. VIP contacts bypass (family & important).",
                "• Purpose: Distraction-free productivity.
• Importance: Silences non-critical group alerts while whitelisting VIP family and urgent contacts to ring through.
• How to use: Activate Focus Mode during study, work, or prayer hours.
• Support: " + SUPPORT_EMAIL,
                13,
                0xFF3B82F6, // Blue
                0xFF10B981, // Green
                R.drawable.ic_naya_lotus,
                "focus_mode"
        ));

        // 14. Smart Storage Doctor
        features.add(new FeatureItem(
                "Smart Storage Doctor",
                "Storage Doctor",
                "Scan & clean Telegram cache and duplicate media with one tap.",
                "• Purpose: Storage optimization & decluttering.
• Importance: Scans and purges cached duplicate videos and photos without deleting any cloud chat history.
• How to use: Tap 'Scan & Clean' to instantly reclaim gigabytes of device memory.
• Support: " + SUPPORT_EMAIL,
                14,
                0xFF8B5CF6, // Purple
                0xFF3B82F6, // Blue
                R.drawable.ic_naya_broom,
                "storage_doctor"
        ));

        // 15. Ultra Battery & Low-Data Saver
        features.add(new FeatureItem(
                "Ultra Battery & Low-Data Saver",
                "Battery Saver",
                "Below 20% charge, auto-pause video & heavy animations. Save 30-40% battery.",
                "• Purpose: Extreme energy conservation.
• Importance: Automatically suspends background sync, animated stickers, and video autoplay when battery is low.
• How to use: Automatically monitors battery percentage or force enable anytime.
• Support: " + SUPPORT_EMAIL,
                15,
                0xFF10B981, // Teal
                0xFF8B5CF6, // Purple
                R.drawable.settings_power,
                "battery_saver"
        ));

        // 16. Biometric Chat Locker
        features.add(new FeatureItem(
                "Biometric Chat Locker",
                "Chat Locker",
                "Lock only sensitive chats with fingerprint & passcode. Keep privacy safe.",
                "• Purpose: Selective cryptographic chat locking.
• Importance: Lock individual secret chats with fingerprint/PIN while general chats remain quickly accessible.
• How to use: Long press any chat -> Lock with Fingerprint.
• Support: " + SUPPORT_EMAIL,
                16,
                0xFF3B82F6, // Blue
                0xFF10B981, // Green
                R.drawable.ic_naya_fingerprint,
                "biometric_locker"
        ));

        // 17. In-Chat Instant Translator
        features.add(new FeatureItem(
                "In-Chat Instant Translator",
                "Instant Translator",
                "Translate any foreign language (English, Arabic, Urdu, etc.) to fluent Bangla with 1 tap.",
                "• Purpose: Zero-barrier multilingual chatting.
• Importance: Translates foreign language messages in-place without copying to third-party translation apps.
• How to use: Tap translate on any message bubble to read in clear Bangla.
• Support: " + SUPPORT_EMAIL,
                17,
                0xFF3B82F6, // Blue
                0xFF8B5CF6, // Purple
                R.drawable.outline_ai_translate2,
                "instant_translator"
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
            vh.bind(feature, position - 1);
            vh.setDetailsClickListener(v -> showSingleFeatureDialog(feature));
            applySlideAnimation(vh.itemView, position);
        } else if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind();
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

    /**
     * Shows the single feature details dialog when Details is tapped.
     */
    public void showSingleFeatureDialog(FeatureItem feature) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(feature.number + ". " + feature.title);
        builder.setMessage(feature.detailedGuide);
        builder.setPositiveButton("Understood", null);
        builder.show();
    }

    /**
     * Shows the full All Details Pipeline overview dialog for the entire 17 features.
     */
    public void showAllDetailsPipelineDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("NayaGram 17-Features Pipeline");
        String pipeline =
                "✦ NayaGram Complete Architecture & Pipeline ✦

" +
                "1. STEALTH & PRIVACY PIPELINE:
" +
                "   • Ghost Mode -> Invisible online status & typing
" +
                "   • Anonymous Stories -> Undetected story browsing
" +
                "   • Anti-Delete -> Encrypted SQLite message safety

" +
                "2. SMART MESSAGING PIPELINE:
" +
                "   • Message Scheduler -> Android WorkManager delivery
" +
                "   • Smart Auto-Reply -> Keyword auto-responder
" +
                "   • Clean Forward -> Forward without original quotes
" +
                "   • Voice Transcription -> Instant on-device speech-to-text
" +
                "   • Instant Translator -> 1-tap translation into Bangla

" +
                "3. PERFORMANCE & SYSTEM PIPELINE:
" +
                "   • Focus Mode -> VIP contact bypass & silence alerts
" +
                "   • Storage Doctor -> Fast 1-tap cache & duplicate cleanup
" +
                "   • Battery Saver -> Conserves 30-40% energy below 20%
" +
                "   • Modular Config -> Isolated org.nayagram.platform

" +
                "4. SECURITY & IDENTITY PIPELINE:
" +
                "   • Biometric Chat Locker -> Fingerprint-locked chats
" +
                "   • Call Guard -> Pocket-call confirmation dialog
" +
                "   • User ID & DC Display -> Server datacenter diagnostic

" +
                "Official Support: " + SUPPORT_EMAIL;
        builder.setMessage(pipeline);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    public static class FeatureItem {
        public String title;
        public String catchyTitle;
        public String description;
        public String detailedGuide;
        public int number;
        public int accentColor;
        public int numberBadgeColor;
        public int iconRes;
        public String configKey;

        public FeatureItem(String title, String catchyTitle, String description, String detailedGuide,
                           int number, int accentColor, int numberBadgeColor, int iconRes, String configKey) {
            this.title = title;
            this.catchyTitle = catchyTitle;
            this.description = description;
            this.detailedGuide = detailedGuide;
            this.number = number;
            this.accentColor = accentColor;
            this.numberBadgeColor = numberBadgeColor;
            this.iconRes = iconRes;
            this.configKey = configKey;
        }
    }

    /**
     * Header View displaying NayaGram logo, branding, and All Details Pipeline button.
     */
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout root;
        private final TextView allDetailsBtn;

        public HeaderViewHolder(Context context) {
            super(new FrameLayout(context));
            root = (FrameLayout) itemView;
            root.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(10));

            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER_HORIZONTAL);
            card.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(22), AndroidUtilities.dp(20), AndroidUtilities.dp(20));

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            cardBg.setCornerRadius(AndroidUtilities.dp(22));
            cardBg.setStroke(AndroidUtilities.dp(1), 0x1A000000);
            card.setBackground(cardBg);

            // Logo
            ImageView logoView = new ImageView(context);
            logoView.setImageResource(R.drawable.nayagram_intro_logo);
            LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(
                    AndroidUtilities.dp(64),
                    AndroidUtilities.dp(64)
            );
            logoParams.bottomMargin = AndroidUtilities.dp(10);
            card.addView(logoView, logoParams);

            // Title
            TextView title = new TextView(context);
            title.setText("NayaGram");
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
            title.setTypeface(AndroidUtilities.bold());
            title.setTextColor(0xFF0284C7); // Premium Sky Blue
            card.addView(title);

            // Subtitle
            TextView sub = new TextView(context);
            sub.setText("More Privacy • More Control • A Smarter Chat");
            sub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            sub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            subParams.topMargin = AndroidUtilities.dp(4);
            subParams.bottomMargin = AndroidUtilities.dp(12);
            card.addView(sub, subParams);

            // Pill Badge: 17 Exclusive Features
            TextView badge = new TextView(context);
            badge.setText("✨ 17 Exclusive Features ✨");
            badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            badge.setTypeface(AndroidUtilities.bold());
            badge.setTextColor(Color.WHITE);
            badge.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(5), AndroidUtilities.dp(14), AndroidUtilities.dp(5));
            GradientDrawable badgeBg = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFF0284C7, 0xFF8B5CF6}
            );
            badgeBg.setCornerRadius(AndroidUtilities.dp(20));
            badge.setBackground(badgeBg);
            card.addView(badge);

            // Tagline: Your Chat • Your Rules
            TextView tagline = new TextView(context);
            tagline.setText("Your Chat, Your Rules");
            tagline.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            tagline.setTypeface(AndroidUtilities.bold());
            tagline.setTextColor(0xFF6366F1);
            LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tagParams.topMargin = AndroidUtilities.dp(10);
            tagParams.bottomMargin = AndroidUtilities.dp(14);
            card.addView(tagline, tagParams);

            // All Details & Pipeline Action Button
            allDetailsBtn = new TextView(context);
            allDetailsBtn.setText("📋 All Details & Feature Pipeline");
            allDetailsBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            allDetailsBtn.setTypeface(AndroidUtilities.bold());
            allDetailsBtn.setTextColor(Color.WHITE);
            allDetailsBtn.setGravity(Gravity.CENTER);
            allDetailsBtn.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(10), AndroidUtilities.dp(18), AndroidUtilities.dp(10));
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
            btnParams.topMargin = AndroidUtilities.dp(4);
            card.addView(allDetailsBtn, btnParams);

            root.addView(card, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        }

        public void bind() {}

        public void setAllDetailsClickListener(View.OnClickListener listener) {
            allDetailsBtn.setOnClickListener(listener);
        }
    }
}
