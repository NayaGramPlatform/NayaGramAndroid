package org.nayagram.platform.features;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

import java.util.ArrayList;
import java.util.List;

/**
 * FeatureListAdapter - NayaGram 17 Exclusive Features Showcase Adapter.
 * Clean English typography, high-contrast colored icon badges (Green, Blue, Purple, Cyan),
 * and precise titles and descriptions matching the official NayaGram feature list.
 */
public class FeatureListAdapter extends RecyclerView.Adapter<FeatureListViewHolder> {

    private final List<FeatureItem> features;
    private final Context context;
    private int lastAnimatedPosition = -1;

    public FeatureListAdapter(Context context) {
        this.context = context;
        this.features = new ArrayList<>();
        initializeFeatures();
    }

    private void initializeFeatures() {
        // 1. Ghost Mode (Green)
        features.add(new FeatureItem(
                "Ghost Mode",
                "Hide typing status & online presence. Browse in stealth mode.",
                1,
                0xFF10B981,
                R.drawable.ic_naya_ghost
        ));

        // 2. Message Scheduler (Blue)
        features.add(new FeatureItem(
                "Message Scheduler",
                "Send messages at a specific date & time (Android Jetpack WorkManager).",
                2,
                0xFF3B82F6,
                R.drawable.outline_message_time_24
        ));

        // 3. Smart Auto-Reply (Green)
        features.add(new FeatureItem(
                "Smart Auto-Reply",
                "Keyword-based auto-reply, self-reply protection & spam filtering cache.",
                3,
                0xFF10B981,
                R.drawable.menu_reply
        ));

        // 4. Story Saver (Purple)
        features.add(new FeatureItem(
                "Story Saver",
                "Download stories (photos & HD videos) without compression with 1 click.",
                4,
                0xFF8B5CF6,
                R.drawable.ic_naya_download
        ));

        // 5. Anonymous Story Viewer (Sky Blue)
        features.add(new FeatureItem(
                "Anonymous Story Viewer",
                "View friends' stories secretly. Your name will never appear in the viewer list.",
                5,
                0xFF0EA5E9,
                R.drawable.outline_profile_story
        ));

        // 6. Anti-Delete Recovery (Green)
        features.add(new FeatureItem(
                "Anti-Delete Recovery",
                "Even if the sender deletes messages, it stays safe in local SQLite.",
                6,
                0xFF10B981,
                R.drawable.outline_shield_check
        ));

        // 7. Forward Without Quote (Blue)
        features.add(new FeatureItem(
                "Forward Without Quote",
                "Forward media or messages without the original sender name.",
                7,
                0xFF3B82F6,
                R.drawable.send_plane_24
        ));

        // 8. Voice Transcription (Purple)
        features.add(new FeatureItem(
                "Voice Transcription",
                "Convert voice messages to searchable text instantly.",
                8,
                0xFF8B5CF6,
                R.drawable.ic_naya_voice
        ));

        // 9. Smart Chat Folders (Violet / Purple)
        features.add(new FeatureItem(
                "Smart Chat Folders",
                "Auto-organize personal, group, channels & business chats.",
                9,
                0xFF8B5CF6,
                R.drawable.settings_folders
        ));

        // 10. Call & Voice Protection (Green)
        features.add(new FeatureItem(
                "Call & Voice Protection",
                "Prevent accidental calls or voice messages with confirmation dialog.",
                10,
                0xFF10B981,
                R.drawable.outline_profile_call_24
        ));

        // 11. User ID & DC Display (Blue)
        features.add(new FeatureItem(
                "User ID & DC Display",
                "See your Telegram numeric ID and which data center (DC1-DC5) you're on.",
                11,
                0xFF3B82F6,
                R.drawable.outline_profile_member_24
        ));

        // 12. Modular NayaConfig (Purple)
        features.add(new FeatureItem(
                "Modular NayaConfig",
                "All custom settings are kept in a separate, safe package (org.nayagram.platform).",
                12,
                0xFF8B5CF6,
                R.drawable.settings_features
        ));

        // 13. Digital Wellbeing & Focus Mode (Green)
        features.add(new FeatureItem(
                "Digital Wellbeing & Focus Mode",
                "Silence non-urgent notifications. VIP contacts bypass (family & important).",
                13,
                0xFF10B981,
                R.drawable.outline_profile_mute_24
        ));

        // 14. Smart Storage Doctor (Purple)
        features.add(new FeatureItem(
                "Smart Storage Doctor",
                "Scan & clean Telegram cache and duplicate media with one tap.",
                14,
                0xFF8B5CF6,
                R.drawable.settings_data
        ));

        // 15. Ultra Battery & Low-Data Saver (Green)
        features.add(new FeatureItem(
                "Ultra Battery & Low-Data Saver",
                "Below 20% charge, auto-pause video & heavy animations. Save 30-40% battery & data.",
                15,
                0xFF10B981,
                R.drawable.settings_power
        ));

        // 16. Biometric Chat Locker (Cyan / Blue)
        features.add(new FeatureItem(
                "Biometric Chat Locker",
                "Lock only sensitive chats with fingerprint & passcode. Keep your privacy safe.",
                16,
                0xFF0EA5E9,
                R.drawable.outline_header_lock_24
        ));

        // 17. In-Chat Instant Translator (Blue)
        features.add(new FeatureItem(
                "In-Chat Instant Translator",
                "Translate any foreign language (English, Arabic, Urdu, etc.) to fluent Bangla with 1 tap.",
                17,
                0xFF3B82F6,
                R.drawable.outline_ai_translate2
        ));
    }

    @NonNull
    @Override
    public FeatureListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout itemView = new FrameLayout(context);
        itemView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return new FeatureListViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FeatureListViewHolder holder, int position) {
        FeatureItem feature = features.get(position);
        holder.bind(feature, position);
        applySlideAnimation(holder.itemView, position);
    }

    private void applySlideAnimation(View view, int position) {
        if (position > lastAnimatedPosition) {
            float startTranslationX = (position % 2 == 0)
                    ? -AndroidUtilities.dp(40)
                    : AndroidUtilities.dp(40);

            view.setTranslationX(startTranslationX);
            view.setAlpha(0.0f);

            view.animate()
                    .translationX(0f)
                    .alpha(1.0f)
                    .setDuration(280)
                    .setInterpolator(new DecelerateInterpolator(1.2f))
                    .setStartDelay(Math.min(position * 25L, 150L))
                    .start();

            lastAnimatedPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull FeatureListViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
    }

    @Override
    public int getItemCount() {
        return features.size();
    }

    public static class FeatureItem {
        public String title;
        public String description;
        public int number;
        public int accentColor;
        public int iconRes;

        public FeatureItem(String title, String description, int number, int accentColor, int iconRes) {
            this.title = title;
            this.description = description;
            this.number = number;
            this.accentColor = accentColor;
            this.iconRes = iconRes;
        }
    }
}
