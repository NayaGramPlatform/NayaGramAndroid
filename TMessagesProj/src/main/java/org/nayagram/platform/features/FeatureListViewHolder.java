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
 * Premium card layout with alternating colored circle number badges (#1 to #17),
 * colored squircle vector icon badges, bold typography, dynamic status pills (ON / OFF / RUN),
 * and interactive toggle & configuration triggers.
 */
public class FeatureListViewHolder extends RecyclerView.ViewHolder {

    private final FrameLayout iconBadgeWrapper;
    private final FrameLayout iconBadgeContainer;
    private final ImageView iconView;
    private final TextView numberView;
    private final TextView titleView;
    private final TextView descView;
    private final TextView detailsText;
    private final ImageView infoIcon;
    private final LinearLayout detailsBtn;
    private final LinearLayout cardView;

    public FeatureListViewHolder(@NonNull View itemView) {
        super(itemView);
        Context context = itemView.getContext();
        FrameLayout container = (FrameLayout) itemView;

        container.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6), AndroidUtilities.dp(16), AndroidUtilities.dp(6));

        cardView = new LinearLayout(context);
        cardView.setOrientation(LinearLayout.HORIZONTAL);
        cardView.setGravity(Gravity.CENTER_VERTICAL);
        cardView.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14));
        cardView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 1. Left: Rounded Vector Icon Container with top-corner Number Badge
        iconBadgeWrapper = new FrameLayout(context);
        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(
                AndroidUtilities.dp(52),
                AndroidUtilities.dp(52)
        );
        wrapperParams.rightMargin = AndroidUtilities.dp(12);
        iconBadgeWrapper.setLayoutParams(wrapperParams);

        // Icon squircle badge (46dp x 46dp)
        iconBadgeContainer = new FrameLayout(context);
        FrameLayout.LayoutParams iconBadgeParams = new FrameLayout.LayoutParams(
                AndroidUtilities.dp(46),
                AndroidUtilities.dp(46),
                Gravity.BOTTOM | Gravity.LEFT
        );
        iconBadgeContainer.setLayoutParams(iconBadgeParams);

        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iconView.setColorFilter(Color.WHITE);
        FrameLayout.LayoutParams ivParams = new FrameLayout.LayoutParams(
                AndroidUtilities.dp(24),
                AndroidUtilities.dp(24),
                Gravity.CENTER
        );
        iconBadgeContainer.addView(iconView, ivParams);
        iconBadgeWrapper.addView(iconBadgeContainer);

        // Circular number badge (20dp x 20dp) at top-left
        numberView = new TextView(context);
        numberView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        numberView.setTextColor(Color.WHITE);
        numberView.setGravity(Gravity.CENTER);
        numberView.setTypeface(AndroidUtilities.bold());
        FrameLayout.LayoutParams numParams = new FrameLayout.LayoutParams(
                AndroidUtilities.dp(20),
                AndroidUtilities.dp(20),
                Gravity.TOP | Gravity.LEFT
        );
        numberView.setLayoutParams(numParams);
        iconBadgeWrapper.addView(numberView);

        // 2. Center: Content (Title + Description)
        LinearLayout centerLayout = new LinearLayout(context);
        centerLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams centerParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        centerLayout.setLayoutParams(centerParams);

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTypeface(AndroidUtilities.bold());
        centerLayout.addView(titleView);

        descView = new TextView(context);
        descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        descView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        descView.setLineSpacing(AndroidUtilities.dp(2), 1.15f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        descParams.topMargin = AndroidUtilities.dp(3);
        descView.setLayoutParams(descParams);
        centerLayout.addView(descView);

        // 3. Right: Action & Status Button
        detailsBtn = new LinearLayout(context);
        detailsBtn.setOrientation(LinearLayout.HORIZONTAL);
        detailsBtn.setGravity(Gravity.CENTER);
        detailsBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));

        infoIcon = new ImageView(context);
        infoIcon.setImageResource(R.drawable.ic_naya_info);
        LinearLayout.LayoutParams iiParams = new LinearLayout.LayoutParams(
                AndroidUtilities.dp(14),
                AndroidUtilities.dp(14)
        );
        iiParams.rightMargin = AndroidUtilities.dp(4);
        detailsBtn.addView(infoIcon, iiParams);

        detailsText = new TextView(context);
        detailsText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        detailsText.setTypeface(AndroidUtilities.bold());
        detailsBtn.addView(detailsText);

        LinearLayout.LayoutParams detParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        detParams.leftMargin = AndroidUtilities.dp(6);
        detailsBtn.setLayoutParams(detParams);

        // Assemble card
        cardView.addView(iconBadgeWrapper);
        cardView.addView(centerLayout);
        cardView.addView(detailsBtn);

        container.addView(cardView);
    }

    public void bind(FeatureListAdapter.FeatureItem feature, int position, int lang, boolean isEnabled, String actionLabel, int labelColor, int labelBgColor) {
        if (feature == null) return;

        titleView.setText(feature.getCatchyTitle(lang));
        descView.setText(feature.getDesc(lang));
        numberView.setText(String.valueOf(feature.number));

        cardView.setContentDescription("Feature " + feature.number + ": " + feature.getCatchyTitle(lang) + ". Status: " + actionLabel + ". Tap to toggle or configure, long press for details.");
        cardView.setFocusable(true);
        iconBadgeWrapper.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        detailsBtn.setContentDescription(actionLabel + " for " + feature.getCatchyTitle(lang));

        detailsText.setText(actionLabel);
        detailsText.setTextColor(labelColor);

        GradientDrawable detBg = new GradientDrawable();
        detBg.setColor(labelBgColor);
        detBg.setCornerRadius(AndroidUtilities.dp(12));
        detailsBtn.setBackground(detBg);

        // Icon
        if (feature.iconRes != 0) {
            iconView.setImageResource(feature.iconRes);
            iconView.setVisibility(View.VISIBLE);
        } else {
            iconView.setVisibility(View.GONE);
        }

        // Left Icon background squircle
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(feature.accentColor);
        iconBg.setCornerRadius(AndroidUtilities.dp(14));
        iconBadgeContainer.setBackground(iconBg);

        // Number circle badge background
        GradientDrawable numBg = new GradientDrawable();
        numBg.setShape(GradientDrawable.OVAL);
        numBg.setColor(feature.numberBadgeColor);
        numberView.setBackground(numBg);

        // Card background styling
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cardBg.setCornerRadius(AndroidUtilities.dp(16));
        cardBg.setStroke(AndroidUtilities.dp(1), 0x14000000);
        cardView.setBackground(cardBg);
    }

    public void setListeners(View.OnClickListener clickListener, View.OnLongClickListener longClickListener, View.OnClickListener infoListener) {
        cardView.setOnClickListener(clickListener);
        detailsBtn.setOnClickListener(clickListener);
        cardView.setOnLongClickListener(longClickListener);
        infoIcon.setOnClickListener(infoListener);
    }
}
