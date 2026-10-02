package org.nayagram.platform.features;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

/**
 * FeatureListViewHolder for NayaGram Feature Showcase.
 * Premium, spacious (non-crowded) UI with native Android Vector Icons,
 * tinted rounded icon badges, generous line-height, and smooth touch ripples.
 */
public class FeatureListViewHolder extends RecyclerView.ViewHolder {

    private final FrameLayout iconBadgeContainer;
    private final ImageView iconView;
    private final TextView numberView;
    private final TextView titleView;
    private final TextView descView;
    private final ImageView chevronView;
    private final LinearLayout cardView;

    public FeatureListViewHolder(@NonNull View itemView) {
        super(itemView);
        Context context = itemView.getContext();

        FrameLayout container = (FrameLayout) itemView;
        // Spacious margins between cards (14dp vertical gap, 16dp horizontal)
        container.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(7), AndroidUtilities.dp(16), AndroidUtilities.dp(7));

        cardView = new LinearLayout(context);
        cardView.setOrientation(LinearLayout.HORIZONTAL);
        cardView.setGravity(Gravity.CENTER_VERTICAL);
        // Generous inner padding inside card for airy, non-crowded look
        cardView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(15), AndroidUtilities.dp(16), AndroidUtilities.dp(15));
        cardView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 1. Left: Rounded Vector Icon Container (46dp x 46dp)
        iconBadgeContainer = new FrameLayout(context);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                AndroidUtilities.dp(46),
                AndroidUtilities.dp(46)
        );
        iconParams.rightMargin = AndroidUtilities.dp(14);
        iconBadgeContainer.setLayoutParams(iconParams);

        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iconView.setColorFilter(Color.WHITE);
        FrameLayout.LayoutParams ivParams = new FrameLayout.LayoutParams(
                AndroidUtilities.dp(24),
                AndroidUtilities.dp(24),
                Gravity.CENTER
        );
        iconBadgeContainer.addView(iconView, ivParams);

        // 2. Center: Content (Number Badge + Title + Description)
        LinearLayout centerLayout = new LinearLayout(context);
        centerLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams centerParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        centerLayout.setLayoutParams(centerParams);

        // Title row: Number Badge + Title
        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        numberView = new TextView(context);
        numberView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        numberView.setTextColor(Color.WHITE);
        numberView.setGravity(Gravity.CENTER);
        numberView.setTypeface(AndroidUtilities.bold());
        numberView.setPadding(AndroidUtilities.dp(7), AndroidUtilities.dp(2), AndroidUtilities.dp(7), AndroidUtilities.dp(2));
        LinearLayout.LayoutParams numParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        numParams.rightMargin = AndroidUtilities.dp(8);
        numberView.setLayoutParams(numParams);

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        titleRow.addView(numberView);
        titleRow.addView(titleView);

        // Description with comfortable line-spacing (not dense)
        descView = new TextView(context);
        descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        descView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        descView.setLineSpacing(AndroidUtilities.dp(3), 1.15f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        descParams.topMargin = AndroidUtilities.dp(5);
        descView.setLayoutParams(descParams);

        centerLayout.addView(titleRow);
        centerLayout.addView(descView);

        // 3. Right: Navigation Chevron
        chevronView = new ImageView(context);
        chevronView.setImageResource(R.drawable.ic_naya_chevron_right);
        chevronView.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon));
        LinearLayout.LayoutParams chevParams = new LinearLayout.LayoutParams(
                AndroidUtilities.dp(20),
                AndroidUtilities.dp(20)
        );
        chevParams.leftMargin = AndroidUtilities.dp(10);
        chevronView.setLayoutParams(chevParams);

        // Assemble card
        cardView.addView(iconBadgeContainer);
        cardView.addView(centerLayout);
        cardView.addView(chevronView);

        container.addView(cardView);
    }

    public void bind(FeatureListAdapter.FeatureItem feature, int position) {
        if (feature == null) return;

        titleView.setText(feature.title);
        descView.setText(feature.description);
        numberView.setText(String.format("#%02d", feature.number));

        // Icon setting
        if (feature.iconRes != 0) {
            iconView.setImageResource(feature.iconRes);
            iconView.setVisibility(View.VISIBLE);
        } else {
            iconView.setVisibility(View.GONE);
        }

        // Left Icon background
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(feature.accentColor);
        iconBg.setCornerRadius(AndroidUtilities.dp(14));
        iconBadgeContainer.setBackground(iconBg);

        // Number badge background
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(feature.accentColor);
        badgeBg.setCornerRadius(AndroidUtilities.dp(5));
        numberView.setBackground(badgeBg);

        // Card background styling (spacious rounded card with 1dp subtle border)
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cardBg.setCornerRadius(AndroidUtilities.dp(16));
        cardBg.setStroke(AndroidUtilities.dp(1), Theme.getColor(Theme.key_divider));
        cardView.setBackground(cardBg);
    }
}
