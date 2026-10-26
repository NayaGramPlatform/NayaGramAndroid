package org.nayagram.platform.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class NGAdminDialogHelper {

    public static void showAdminSheet(BaseFragment fragment, Context context, int currentAccount) {
        if (context == null) return;

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20));
        layout.setBackground(createRoundBg(Theme.isCurrentThemeDark() ? 0xFF1E293B : 0xFFFFFFFF, 20));

        // Header
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView shieldIcon = new ImageView(context);
        shieldIcon.setImageResource(R.drawable.msg_admins);
        shieldIcon.setColorFilter(new android.graphics.PorterDuffColorFilter(0xFF0284C7, android.graphics.PorterDuff.Mode.SRC_IN));
        headerRow.addView(shieldIcon, LayoutHelper.createLinear(32, 32, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout titleCol = new LinearLayout(context);
        titleCol.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(context);
        title.setText("NG Admin & Leadership");
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleCol.addView(title);

        TextView subtitle = new TextView(context);
        subtitle.setText("Official Founders, Admins & Support Team");
        subtitle.setTextSize(12);
        subtitle.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        titleCol.addView(subtitle);

        headerRow.addView(titleCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));
        layout.addView(headerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        final AlertDialog[] dialogHolder = new AlertDialog[1];

        // Card 1: Super Admin @NGSupreme
        layout.addView(createAdminCard(context, "👑", "Super Admin (Founder & Owner)", "@NGSupreme", 0xFFF59E0B, v -> {
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            MessagesController.getInstance(currentAccount).openByUserName("NGSupreme", fragment, 1);
        }), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Card 2: Admin @NG_HuzayfaSheikh
        layout.addView(createAdminCard(context, "🛡️", "Admin (Co-Founder)", "@NG_HuzayfaSheikh", 0xFF0284C7, v -> {
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            MessagesController.getInstance(currentAccount).openByUserName("NG_HuzayfaSheikh", fragment, 1);
        }), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Card 3: Official Support Channel @NayaGramPro
        layout.addView(createAdminCard(context, "📢", "Official Support Channel", "@NayaGramPro", 0xFF10B981, v -> {
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            MessagesController.getInstance(currentAccount).openByUserName("NayaGramPro", fragment, 1);
        }), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Close Button
        TextView closeBtn = new TextView(context);
        closeBtn.setText("Close");
        closeBtn.setTextSize(14);
        closeBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        closeBtn.setBackground(createRoundBg(0xFF0284C7, 12));
        closeBtn.setOnClickListener(v -> {
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
        });
        layout.addView(closeBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(layout);
        dialogHolder[0] = builder.create();
        dialogHolder[0].show();
    }

    private static View createAdminCard(Context context, String emoji, String role, String username, int accentColor, View.OnClickListener clickListener) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        boolean dark = Theme.isCurrentThemeDark();
        card.setBackground(createCardBg(dark ? 0xFF0F172A : 0xFFF8FAFC, accentColor, 14));

        TextView emojiBadge = new TextView(context);
        emojiBadge.setText(emoji);
        emojiBadge.setTextSize(18);
        card.addView(emojiBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 12, 0));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView roleText = new TextView(context);
        roleText.setText(role);
        roleText.setTextSize(14);
        roleText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        roleText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        textCol.addView(roleText);

        TextView userText = new TextView(context);
        userText.setText(username);
        userText.setTextSize(12);
        userText.setTextColor(accentColor);
        textCol.addView(userText);

        card.addView(textCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        ImageView chevron = new ImageView(context);
        chevron.setImageResource(R.drawable.msg_arrowright);
        chevron.setColorFilter(new android.graphics.PorterDuffColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN));
        card.addView(chevron, LayoutHelper.createLinear(16, 16, Gravity.CENTER_VERTICAL));

        card.setOnClickListener(clickListener);
        return card;
    }

    private static GradientDrawable createCardBg(int bgColor, int borderColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(AndroidUtilities.dp(radiusDp));
        gd.setStroke(AndroidUtilities.dp(1), borderColor);
        return gd;
    }

    private static GradientDrawable createRoundBg(int color, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(AndroidUtilities.dp(radiusDp));
        return gd;
    }
}
