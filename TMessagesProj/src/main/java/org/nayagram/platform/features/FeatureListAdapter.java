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
 * FeatureListAdapter - NayaGram Feature Showcase Adapter.
 * Features Alternating Slide-In Animation (Left & Right) on scroll,
 * well-spaced and non-crowded layout with native XML Vector Drawables.
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
        features.add(new FeatureItem(
                "Ghost Mode",
                "Hidden typing indicator and online status",
                1,
                0xFF3DDC97,
                R.drawable.ic_naya_ghost
        ));

        features.add(new FeatureItem(
                "Message Scheduler",
                "Schedule messages for automatic delivery",
                2,
                0xFF4D9EFF,
                R.drawable.outline_message_time_24
        ));

        features.add(new FeatureItem(
                "Smart Auto-Reply",
                "Keyword-based automatic response system",
                3,
                0xFF9F6BFF,
                R.drawable.menu_reply
        ));

        features.add(new FeatureItem(
                "Story Saver",
                "Download HD quality stories instantly",
                4,
                0xFF4CC9F0,
                R.drawable.ic_naya_download
        ));

        features.add(new FeatureItem(
                "Anonymous Viewer",
                "View stories without revealing yourself",
                5,
                0xFF00C2A8,
                R.drawable.outline_profile_story
        ));

        features.add(new FeatureItem(
                "Anti-Delete Recovery",
                "Recover permanently deleted messages",
                6,
                0xFF7B61FF,
                R.drawable.outline_shield_check
        ));

        features.add(new FeatureItem(
                "Forward Without Quote",
                "Share media without forwarding tag",
                7,
                0xFF26C281,
                R.drawable.send_plane_24
        ));

        features.add(new FeatureItem(
                "Voice Transcription",
                "Convert voice messages to searchable text",
                8,
                0xFF00A8E8,
                R.drawable.ic_naya_voice
        ));

        features.add(new FeatureItem(
                "Smart Chat Folders",
                "Organize conversations by categories",
                9,
                0xFF8A5CF6,
                R.drawable.settings_folders
        ));

        features.add(new FeatureItem(
                "Call Protection",
                "Confirm before sending calls or voice",
                10,
                0xFF34D399,
                R.drawable.outline_profile_call_24
        ));

        features.add(new FeatureItem(
                "User ID Display",
                "See unique ID and server location",
                11,
                0xFF3B82F6,
                R.drawable.outline_profile_member_24
        ));

        features.add(new FeatureItem(
                "Modular NayaConfig",
                "Custom settings in isolated module",
                12,
                0xFF7C3AED,
                R.drawable.settings_features
        ));

        features.add(new FeatureItem(
                "Digital Wellbeing & Focus Mode",
                "Mute group pings for study, work & prayer with VIP bypass",
                13,
                0xFF00C2A8,
                R.drawable.outline_profile_mute_24
        ));

        features.add(new FeatureItem(
                "Smart Storage Doctor",
                "One-tap cache and orphaned media optimizer",
                14,
                0xFF4D9EFF,
                R.drawable.settings_data
        ));

        features.add(new FeatureItem(
                "Ultra Battery & Low-Data Saver",
                "Smart eco-mode and metered data optimization",
                15,
                0xFF00C2A8,
                R.drawable.settings_power
        ));

        features.add(new FeatureItem(
                "Biometric Chat Locker",
                "Protect sensitive chats with fingerprint and PIN",
                16,
                0xFF7B61FF,
                R.drawable.outline_header_lock_24
        ));

        features.add(new FeatureItem(
                "In-Chat Instant Translator",
                "Translate foreign messages to Bengali with LRU cache",
                17,
                0xFF3DDC97,
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

    /**
     * Alternating smooth slide-in animation from Left and Right on scroll.
     * Even positions slide in from Left, Odd positions slide in from Right.
     */
    private void applySlideAnimation(View view, int position) {
        if (position > lastAnimatedPosition) {
            // Even: slide from left (-60dp), Odd: slide from right (+60dp)
            float startTranslationX = (position % 2 == 0)
                    ? -AndroidUtilities.dp(60)
                    : AndroidUtilities.dp(60);

            view.setTranslationX(startTranslationX);
            view.setAlpha(0.0f);

            view.animate()
                    .translationX(0f)
                    .alpha(1.0f)
                    .setDuration(340)
                    .setInterpolator(new DecelerateInterpolator(1.4f))
                    .setStartDelay(Math.min(position * 30L, 180L))
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

        public FeatureItem(String title, String description, int number, int accentColor) {
            this(title, description, number, accentColor, 0);
        }
    }
}
