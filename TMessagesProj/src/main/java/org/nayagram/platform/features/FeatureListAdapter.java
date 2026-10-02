package org.nayagram.platform.features;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;

import java.util.ArrayList;
import java.util.List;

public class FeatureListAdapter extends RecyclerView.Adapter<FeatureListViewHolder> {
    
    private List<FeatureItem> features;
    private Context context;
    private static final int[] COLORS = {
            0xFF3DDC97,
            0xFF4D9EFF,
            0xFF9F6BFF,
            0xFF4CC9F0,
            0xFF00C2A8,
            0xFF7B61FF,
            0xFF26C281,
            0xFF00A8E8,
            0xFF8A5CF6,
            0xFF34D399,
            0xFF3B82F6,
            0xFF7C3AED
    };
    
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
                0xFF3DDC97
        ));
        
        features.add(new FeatureItem(
                "Message Scheduler",
                "Schedule messages for automatic delivery",
                2,
                0xFF4D9EFF
        ));
        
        features.add(new FeatureItem(
                "Smart Auto-Reply",
                "Keyword-based automatic response system",
                3,
                0xFF9F6BFF
        ));
        
        features.add(new FeatureItem(
                "Story Saver",
                "Download HD quality stories instantly",
                4,
                0xFF4CC9F0
        ));
        
        features.add(new FeatureItem(
                "Anonymous Viewer",
                "View stories without revealing yourself",
                5,
                0xFF00C2A8
        ));
        
        features.add(new FeatureItem(
                "Anti-Delete Recovery",
                "Recover permanently deleted messages",
                6,
                0xFF7B61FF
        ));
        
        features.add(new FeatureItem(
                "Forward Without Quote",
                "Share media without forwarding tag",
                7,
                0xFF26C281
        ));
        
        features.add(new FeatureItem(
                "Voice Transcription",
                "Convert voice messages to searchable text",
                8,
                0xFF00A8E8
        ));
        
        features.add(new FeatureItem(
                "Smart Chat Folders",
                "Organize conversations by categories",
                9,
                0xFF8A5CF6
        ));
        
        features.add(new FeatureItem(
                "Call Protection",
                "Confirm before sending calls or voice",
                10,
                0xFF34D399
        ));
        
        features.add(new FeatureItem(
                "User ID Display",
                "See unique ID and server location",
                11,
                0xFF3B82F6
        ));
        
        features.add(new FeatureItem(
                "Modular NayaConfig",
                "Custom settings in isolated module",
                12,
                0xFF7C3AED
        ));

        features.add(new FeatureItem(
                "Digital Wellbeing & Focus Mode",
                "Mute group pings for study, work & prayer with VIP bypass",
                13,
                0xFF00C2A8
        ));

        features.add(new FeatureItem(
                "Smart Storage Doctor",
                "One-tap cache and orphaned media optimizer",
                14,
                0xFF4D9EFF
        ));

        features.add(new FeatureItem(
                "Ultra Battery & Low-Data Saver",
                "Smart eco-mode and metered data optimization",
                15,
                0xFF00C2A8
        ));

        features.add(new FeatureItem(
                "Biometric Chat Locker",
                "Protect sensitive chats with fingerprint and PIN",
                16,
                0xFF7B61FF
        ));

        features.add(new FeatureItem(
                "In-Chat Instant Translator",
                "Translate foreign messages to Bengali with LRU cache",
                17,
                0xFF3DDC97
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
        
        public FeatureItem(String title, String description, int number, int accentColor) {
            this.title = title;
            this.description = description;
            this.number = number;
            this.accentColor = accentColor;
        }
    }
}
