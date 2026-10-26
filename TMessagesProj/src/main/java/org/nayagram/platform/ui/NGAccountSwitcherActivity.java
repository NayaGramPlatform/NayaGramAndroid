package org.nayagram.platform.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.LoginActivity;

public class NGAccountSwitcherActivity extends BaseFragment {

    private LinearLayout accountsContainer;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("👥 Switch Account");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    showAddAccountOptions(context);
                }
            }
        });

        org.telegram.ui.ActionBar.ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(1, R.drawable.outline_add_account);

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(24));

        int activatedCount = UserConfig.getActivatedAccountsCount();

        TextView titleView = new TextView(context);
        titleView.setText("👥 Active Accounts (" + activatedCount + "/" + UserConfig.MAX_ACCOUNT_COUNT + ")");
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        content.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        accountsContainer = new LinearLayout(context);
        accountsContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(accountsContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        refreshAccountsList(context);

        // Add Account Card
        LinearLayout addCard = new LinearLayout(context);
        addCard.setOrientation(LinearLayout.VERTICAL);
        addCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        addCard.setBackground(createRoundBg(Theme.isCurrentThemeDark() ? 0xFF1E293B : 0xFFFFFFFF, 16));

        TextView addTitle = new TextView(context);
        addTitle.setText("➕ Add New Account");
        addTitle.setTextSize(15);
        addTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        addTitle.setTextColor(0xFF0284C7);
        addCard.addView(addTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        TextView addDesc = new TextView(context);
        addDesc.setText("Log in with phone number or bot token to manage up to 15 accounts.");
        addDesc.setTextSize(13);
        addDesc.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        addCard.addView(addDesc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        TextView addBtn = new TextView(context);
        addBtn.setText("Add Account Options");
        addBtn.setTextSize(14);
        addBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        addBtn.setTextColor(Color.WHITE);
        addBtn.setGravity(Gravity.CENTER);
        addBtn.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        addBtn.setBackground(createRoundBg(0xFF0284C7, 12));
        addBtn.setOnClickListener(v -> showAddAccountOptions(context));
        addCard.addView(addBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        content.addView(addCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        scrollView.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((FrameLayout) fragmentView).addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    private void refreshAccountsList(Context context) {
        if (accountsContainer == null) return;
        accountsContainer.removeAllViews();

        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!UserConfig.getInstance(a).isClientActivated()) continue;
            final int accountIndex = a;
            TLRPC.User user = UserConfig.getInstance(a).getCurrentUser();

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
            boolean isActive = (accountIndex == currentAccount);
            row.setBackground(createRoundBg(isActive ? (Theme.isCurrentThemeDark() ? 0xFF1E3A5F : 0xFFE0F2FE) : (Theme.isCurrentThemeDark() ? 0xFF1E293B : 0xFFFFFFFF), 14));

            AvatarDrawable ad = new AvatarDrawable();
            ad.setInfo(accountIndex, user);
            BackupImageView av = new BackupImageView(context);
            av.setRoundRadius(AndroidUtilities.dp(18));
            av.getImageReceiver().setCurrentAccount(accountIndex);
            av.setForUserOrChat(user, ad);
            row.addView(av, LayoutHelper.createLinear(44, 44, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);

            TextView uName = new TextView(context);
            uName.setText(UserObject.getUserName(user));
            uName.setTextSize(15);
            uName.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            uName.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            textCol.addView(uName);

            TextView uStatus = new TextView(context);
            uStatus.setText(isActive ? "Active Account" : (user != null && user.username != null && !user.username.isEmpty() ? "@" + user.username : "Account #" + (accountIndex + 1)));
            uStatus.setTextSize(12);
            uStatus.setTextColor(isActive ? 0xFF0284C7 : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            textCol.addView(uStatus);

            row.addView(textCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

            if (isActive) {
                TextView checkBadge = new TextView(context);
                checkBadge.setText("✓");
                checkBadge.setTextSize(14);
                checkBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                checkBadge.setTextColor(Color.WHITE);
                checkBadge.setGravity(Gravity.CENTER);
                checkBadge.setBackground(createRoundBg(0xFF0284C7, 12));
                row.addView(checkBadge, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL));
            }

            row.setOnClickListener(v -> {
                if (accountIndex != currentAccount && LaunchActivity.instance != null) {
                    LaunchActivity.instance.switchToAccount(accountIndex, true);
                    finishFragment();
                }
            });

            accountsContainer.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
        }
    }

    private void showAddAccountOptions(Context context) {
        int freeAccount = -1;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!UserConfig.getInstance(a).isClientActivated()) {
                freeAccount = a;
                break;
            }
        }
        if (freeAccount < 0) {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.error, "Maximum 15 accounts limit reached").show();
            return;
        }

        final int targetAccount = freeAccount;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("➕ Add Account Option");
        CharSequence[] options = new CharSequence[] {
            "📱 Phone Number Login",
            "🤖 Bot Token Login"
        };
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                presentFragment(new LoginActivity(targetAccount));
            } else if (which == 1) {
                org.nayagram.platform.auth.NGBotLoginHelper.showBotLoginDialog(NGAccountSwitcherActivity.this, targetAccount);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private GradientDrawable createRoundBg(int color, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(AndroidUtilities.dp(radiusDp));
        return gd;
    }
}
