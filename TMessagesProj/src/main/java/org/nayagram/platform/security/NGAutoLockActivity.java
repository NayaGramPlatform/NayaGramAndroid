package org.nayagram.platform.security;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings screen for Auto-lock NayaGram (Settings → NG Safety → Auto-lock).
 * Offers presets: 15m, 45m, 1h, 3h, 5h, 12h, Custom (Hours/Minutes), and Never.
 * Styled with rounded white cards and NayaGram Blue (#2D7FF9) checkmarks.
 */
public class NGAutoLockActivity extends BaseFragment {

    private static final int COLOR_NG_BLUE = 0xFF2D7FF9;
    private static final int COLOR_CARD_BG = 0xFFFFFFFF;
    private static final int COLOR_BORDER_UNSELECTED = 0xFFE2E8F0;
    private static final int COLOR_BORDER_SELECTED = 0xFF2D7FF9;

    private static class OptionItem {
        final String label;
        final int minutes;
        final boolean isCustom;

        OptionItem(String label, int minutes, boolean isCustom) {
            this.label = label;
            this.minutes = minutes;
            this.isCustom = isCustom;
        }
    }

    private final List<OptionItem> options = new ArrayList<>();
    private int selectedMinutes;
    private boolean isCustomSelected;
    private int customHours = 1;
    private int customMinutesVal = 30;

    private LinearLayout optionsContainer;
    private final List<View> cardViews = new ArrayList<>();
    private final List<ImageView> checkViews = new ArrayList<>();
    private LinearLayout customInputsCard;
    private EditText hoursInput;
    private EditText minutesInput;

    public NGAutoLockActivity() {
        options.add(new OptionItem("15 minutes", 15, false));
        options.add(new OptionItem("45 minutes", 45, false));
        options.add(new OptionItem("1 hour", 60, false));
        options.add(new OptionItem("3 hours", 180, false));
        options.add(new OptionItem("5 hours", 300, false));
        options.add(new OptionItem("12 hours", 720, false));
        options.add(new OptionItem("Custom (Hours/Minutes picker)", -1, true));
        options.add(new OptionItem("Never", 0, false));
    }

    @Override
    public View createView(Context context) {
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Auto-lock NayaGram");
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        int saved = NGSafety.getAutoLockMinutes(context);
        selectedMinutes = saved;
        isCustomSelected = isCustomPreset(saved);
        if (isCustomSelected) {
            customHours = saved / 60;
            customMinutesVal = saved % 60;
        }

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        fragmentView = scroll;

        TextView title = new TextView(context);
        title.setText("Auto-lock NayaGram");
        title.setTextSize(22);
        title.setTextColor(0xFF0F172A);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, 0, 0, AndroidUtilities.dp(4));
        root.addView(title);

        TextView description = new TextView(context);
        description.setText("Set how quickly NayaGram locks when placed in the background. Requires PIN or NG Authenticator when the duration expires.");
        description.setTextSize(13);
        description.setTextColor(0xFF64748B);
        description.setLineSpacing(AndroidUtilities.dp(2), 1.05f);
        description.setPadding(0, 0, 0, AndroidUtilities.dp(14));
        root.addView(description);

        optionsContainer = new LinearLayout(context);
        optionsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(optionsContainer);

        buildOptions(context);
        buildCustomPickerCard(context, root);

        Button saveButton = new Button(context);
        saveButton.setText("Save");
        saveButton.setTextColor(Color.WHITE);
        saveButton.setTextSize(16);
        saveButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        saveButton.setAllCaps(false);
        saveButton.setBackground(roundCard(COLOR_NG_BLUE, COLOR_NG_BLUE, 14, 0));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(50));
        btnParams.topMargin = AndroidUtilities.dp(16);
        btnParams.bottomMargin = AndroidUtilities.dp(10);
        root.addView(saveButton, btnParams);

        saveButton.setOnClickListener(v -> saveSetting(context));

        TextView footer = new TextView(context);
        footer.setText("Built with ❤️ in Bangladesh");
        footer.setTextSize(13);
        footer.setTextColor(0xFF64748B);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(8));
        root.addView(footer);

        return fragmentView;
    }

    private boolean isCustomPreset(int minutes) {
        if (minutes == 0 || minutes == 15 || minutes == 45 || minutes == 60 ||
            minutes == 180 || minutes == 300 || minutes == 720) {
            return false;
        }
        return true;
    }

    private void buildOptions(Context context) {
        optionsContainer.removeAllViews();
        cardViews.clear();
        checkViews.clear();

        for (int i = 0; i < options.size(); i++) {
            final OptionItem item = options.get(i);
            final int index = i;

            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));

            boolean selected = isSelected(item);
            card.setBackground(roundCard(COLOR_CARD_BG, selected ? COLOR_BORDER_SELECTED : COLOR_BORDER_UNSELECTED, 14, selected ? 2 : 1));

            TextView label = new TextView(context);
            label.setText(item.label);
            label.setTextSize(15);
            label.setTextColor(0xFF0F172A);
            label.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
            card.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            ImageView check = new ImageView(context);
            check.setImageResource(R.drawable.check_round);
            check.setColorFilter(COLOR_NG_BLUE);
            check.setVisibility(selected ? View.VISIBLE : View.GONE);
            card.addView(check, new LinearLayout.LayoutParams(AndroidUtilities.dp(22), AndroidUtilities.dp(22)));

            cardViews.add(card);
            checkViews.add(check);

            card.setOnClickListener(v -> selectOption(index));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = AndroidUtilities.dp(8);
            optionsContainer.addView(card, lp);
        }
    }

    private void buildCustomPickerCard(Context context, LinearLayout root) {
        customInputsCard = new LinearLayout(context);
        customInputsCard.setOrientation(LinearLayout.VERTICAL);
        customInputsCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        customInputsCard.setBackground(roundCard(0xFFF8FAFC, 0xFFBAE6FD, 14, 1));
        customInputsCard.setVisibility(isCustomSelected ? View.VISIBLE : View.GONE);

        TextView pickerTitle = new TextView(context);
        pickerTitle.setText("Custom Duration");
        pickerTitle.setTextSize(14);
        pickerTitle.setTextColor(0xFF0284C7);
        pickerTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pickerTitle.setPadding(0, 0, 0, AndroidUtilities.dp(8));
        customInputsCard.addView(pickerTitle);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        customInputsCard.addView(row);

        // Hours input
        LinearLayout hoursCol = new LinearLayout(context);
        hoursCol.setOrientation(LinearLayout.VERTICAL);
        row.addView(hoursCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView hoursLabel = new TextView(context);
        hoursLabel.setText("Hours (0–24)");
        hoursLabel.setTextSize(12);
        hoursLabel.setTextColor(0xFF64748B);
        hoursCol.addView(hoursLabel);

        hoursInput = new EditText(context);
        hoursInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        hoursInput.setSingleLine(true);
        hoursInput.setText(String.valueOf(customHours));
        hoursInput.setTextSize(16);
        hoursInput.setTextColor(0xFF0F172A);
        hoursInput.setBackground(roundCard(COLOR_CARD_BG, 0xFFCBD5E1, 8, 1));
        hoursInput.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));
        hoursInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        hoursCol.addView(hoursInput);

        View gap = new View(context);
        row.addView(gap, new LinearLayout.LayoutParams(AndroidUtilities.dp(12), 1));

        // Minutes input
        LinearLayout minsCol = new LinearLayout(context);
        minsCol.setOrientation(LinearLayout.VERTICAL);
        row.addView(minsCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView minsLabel = new TextView(context);
        minsLabel.setText("Minutes (0–59)");
        minsLabel.setTextSize(12);
        minsLabel.setTextColor(0xFF64748B);
        minsCol.addView(minsLabel);

        minutesInput = new EditText(context);
        minutesInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        minutesInput.setSingleLine(true);
        minutesInput.setText(String.valueOf(customMinutesVal));
        minutesInput.setTextSize(16);
        minutesInput.setTextColor(0xFF0F172A);
        minutesInput.setBackground(roundCard(COLOR_CARD_BG, 0xFFCBD5E1, 8, 1));
        minutesInput.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));
        minutesInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        minsCol.addView(minutesInput);

        LinearLayout.LayoutParams pickerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pickerParams.bottomMargin = AndroidUtilities.dp(12);
        root.addView(customInputsCard, pickerParams);
    }

    private boolean isSelected(OptionItem item) {
        if (item.isCustom) return isCustomSelected;
        if (isCustomSelected) return false;
        return selectedMinutes == item.minutes;
    }

    private void selectOption(int index) {
        OptionItem chosen = options.get(index);
        isCustomSelected = chosen.isCustom;
        if (!isCustomSelected) {
            selectedMinutes = chosen.minutes;
        }

        for (int i = 0; i < options.size(); i++) {
            boolean sel = isSelected(options.get(i));
            View card = cardViews.get(i);
            ImageView check = checkViews.get(i);

            card.setBackground(roundCard(COLOR_CARD_BG, sel ? COLOR_BORDER_SELECTED : COLOR_BORDER_UNSELECTED, 14, sel ? 2 : 1));
            check.setVisibility(sel ? View.VISIBLE : View.GONE);
            if (card instanceof LinearLayout) {
                View first = ((LinearLayout) card).getChildAt(0);
                if (first instanceof TextView) {
                    ((TextView) first).setTypeface(Typeface.DEFAULT, sel ? Typeface.BOLD : Typeface.NORMAL);
                }
            }
        }

        if (customInputsCard != null) {
            customInputsCard.setVisibility(isCustomSelected ? View.VISIBLE : View.GONE);
        }
    }

    private void saveSetting(Context context) {
        int finalMinutes;
        if (isCustomSelected) {
            int h = 0;
            int m = 0;
            try {
                if (hoursInput != null && hoursInput.getText().length() > 0) {
                    h = Integer.parseInt(hoursInput.getText().toString().trim());
                }
                if (minutesInput != null && minutesInput.getText().length() > 0) {
                    m = Integer.parseInt(minutesInput.getText().toString().trim());
                }
            } catch (Exception ignored) {
            }

            finalMinutes = h * 60 + m;
            if (finalMinutes <= 0) {
                Toast.makeText(context, "Enter at least 1 minute or select Never.", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            finalMinutes = selectedMinutes;
        }

        // Persist to Android storage
        NGSafety.setAutoLockMinutes(context, finalMinutes);

        String displayTime = NGSafety.getAutoLockDisplayString(finalMinutes);
        Toast.makeText(context, "Auto-lock set to " + displayTime + " - Alhamdulillah", Toast.LENGTH_SHORT).show();

        finishFragment();
    }

    private GradientDrawable roundCard(int fillColor, int strokeColor, int cornerDp, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(AndroidUtilities.dp(cornerDp));
        if (strokeDp > 0) {
            drawable.setStroke(AndroidUtilities.dp(strokeDp), strokeColor);
        }
        return drawable;
    }
}
