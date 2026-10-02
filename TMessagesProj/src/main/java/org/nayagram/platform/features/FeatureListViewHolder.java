package org.nayagram.platform.features;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/**
 * FeatureListViewHolder for NayaGram Feature Showcase
 */
public class FeatureListViewHolder extends RecyclerView.ViewHolder {

    private final TextView numberView;
    private final TextView titleView;
    private final TextView descView;
    private final LinearLayout cardView;

    public FeatureListViewHolder(@NonNull View itemView) {
        super(itemView);
        Context context = itemView.getContext();

        FrameLayout container = (FrameLayout) itemView;
        container.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(6));

        cardView = new LinearLayout(context);
        cardView.setOrientation(LinearLayout.VERTICAL);
        cardView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        cardView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Top Row: Number Badge + Title
        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        numberView = new TextView(context);
        numberView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        numberView.setTextColor(Color.WHITE);
        numberView.setGravity(Gravity.CENTER);
        numberView.setTypeface(AndroidUtilities.bold());
        numberView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
        
        LinearLayout.LayoutParams numParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        numParams.rightMargin = AndroidUtilities.dp(10);
        numberView.setLayoutParams(numParams);

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        ));

        topRow.addView(numberView);
        topRow.addView(titleView);

        // Description
        descView = new TextView(context);
        descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        descView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        descView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        descParams.topMargin = AndroidUtilities.dp(6);
        descView.setLayoutParams(descParams);

        cardView.addView(topRow);
        cardView.addView(descView);

        container.addView(cardView);
    }

    public void bind(FeatureListAdapter.FeatureItem feature, int position) {
        if (feature == null) return;

        titleView.setText(feature.title);
        descView.setText(feature.description);
        numberView.setText(String.format("#%02d", feature.number));

        // Number badge background
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(feature.accentColor);
        badgeBg.setCornerRadius(AndroidUtilities.dp(6));
        numberView.setBackground(badgeBg);

        // Card background styling
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cardBg.setCornerRadius(AndroidUtilities.dp(12));
        cardBg.setStroke(AndroidUtilities.dp(1), Theme.getColor(Theme.key_divider));
        cardView.setBackground(cardBg);
    }
}
