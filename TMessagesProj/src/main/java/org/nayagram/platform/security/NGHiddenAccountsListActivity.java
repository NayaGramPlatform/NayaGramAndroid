package org.nayagram.platform.security;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.Collections;

/**
 * NGHiddenAccountsListActivity - Rich Hidden Accounts Vault screen with glowing eye badges,
 * Lock Vault & Security Settings buttons, and explicit [Show] + [Open] action pills per account.
 */
public class NGHiddenAccountsListActivity extends UniversalFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int LOCK_MENU_ID = 1;
    private static final int SETTINGS_MENU_ID = 2;
    private View emptyView;

    @Override
    protected CharSequence getTitle() {
        return "Hidden Accounts Vault";
    }

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.nayagramHiddenAccountsChanged);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.nayagramHiddenAccountsChanged);
        super.onFragmentDestroy();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.nayagramHiddenAccountsChanged) {
            if (!NGHiddenAccounts.isUnlocked()) {
                finishFragment(false);
            } else if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        }
    }

    @Override
    public View createView(Context context) {
        View view = super.createView(context);
        listView.setSections();
        listView.adapter.setApplyBackground(false);
        actionBar.setAdaptiveBackground(listView);
        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(SETTINGS_MENU_ID, R.drawable.msg_settings)
                .setContentDescription("Vault Passcode & Fingerprint Settings");
        menu.addItem(LOCK_MENU_ID, R.drawable.outline_header_lock_24)
                .setContentDescription("Lock Hidden Accounts Vault");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == LOCK_MENU_ID) {
                    NGHiddenAccounts.lock();
                } else if (id == SETTINGS_MENU_ID) {
                    presentFragment(new NGHiddenAccountsActivity(-1));
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!NGHiddenAccounts.isUnlocked()) {
            finishFragment(false);
            return;
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        Context ctx = getContext();
        if (ctx == null) return;

        ArrayList<Integer> hidden = new ArrayList<>();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (NGHiddenAccounts.isHidden(a)) {
                hidden.add(a);
            }
        }
        Collections.sort(hidden, (o1, o2) -> Long.compare(UserConfig.getInstance(o1).loginTime, UserConfig.getInstance(o2).loginTime));

        items.add(UItem.asCustomShadow(buildVaultHeaderCard(ctx, hidden.size())));

        if (hidden.isEmpty()) {
            items.add(UItem.asCustomShadow(getEmptyView(ctx)));
            return;
        }
        for (int account : hidden) {
            items.add(UItem.asCustomShadow(buildHiddenAccountCard(ctx, account)));
        }
        items.add(UItem.asShadow("Tap 'Show' on any card to restore an account to your main drawer, or tap 'Open' to switch into it immediately."));
    }

    private View buildVaultHeaderCard(Context ctx, int hiddenCount) {
        boolean isDark = Theme.isCurrentThemeDark();
        LinearLayout outer = new LinearLayout(ctx);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(14), dp(10), dp(14), dp(6));

        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                isDark ? new int[]{0xFF0F172A, 0xFF1E293B} : new int[]{0xFFE0F2FE, 0xFFEEF2FF}
        );
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1.2f), isDark ? 0x5538BDF8 : 0x440284C7);
        card.setBackground(bg);

        LinearLayout topRow = new LinearLayout(ctx);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout eyeBadge = new FrameLayout(ctx);
        GradientDrawable eyeBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0EA5E9, 0xFF4F46E5}
        );
        eyeBg.setShape(GradientDrawable.OVAL);
        eyeBadge.setBackground(eyeBg);

        ImageView eyeIcon = new ImageView(ctx);
        eyeIcon.setImageResource(R.drawable.msg_view_file);
        eyeIcon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        eyeBadge.addView(eyeIcon, LayoutHelper.createFrame(24, 24, Gravity.CENTER));
        topRow.addView(eyeBadge, LayoutHelper.createLinear(44, 44));

        LinearLayout titleCol = new LinearLayout(ctx);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(ctx);
        title.setText("Hidden Accounts Vault (" + hiddenCount + ")");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(isDark ? Color.WHITE : 0xFF0F172A);
        titleCol.addView(title);

        TextView sub = new TextView(ctx);
        sub.setText(NGHiddenAccounts.isFingerprintEnabled()
                ? "Protected by Passcode & Fingerprint Biometrics"
                : "Protected by Secret Vault Passcode");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        sub.setTextColor(isDark ? 0xFF94A3B8 : 0xFF334155);
        titleCol.addView(sub);

        topRow.addView(titleCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 12, 0, 0, 0));
        card.addView(topRow);

        // Action Buttons Row: [Lock Vault Now] + [Passcode & Fingerprint]
        LinearLayout actionsRow = new LinearLayout(ctx);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout lockBtn = buildHeaderPillButton(
                ctx,
                R.drawable.outline_header_lock_24,
                "Lock Vault",
                0xFF0284C7,
                Color.WHITE,
                v -> NGHiddenAccounts.lock()
        );
        actionsRow.addView(lockBtn, LayoutHelper.createLinear(0, 38, 1f, 0, 12, 6, 0));

        LinearLayout settingsBtn = buildHeaderPillButton(
                ctx,
                R.drawable.ic_naya_fingerprint,
                "Passcode & Bio",
                isDark ? 0x26FFFFFF : 0xFFFFFFFF,
                isDark ? 0xFF38BDF8 : 0xFF0284C7,
                v -> presentFragment(new NGHiddenAccountsActivity(-1))
        );
        actionsRow.addView(settingsBtn, LayoutHelper.createLinear(0, 38, 1f, 6, 12, 0, 0));

        card.addView(actionsRow);
        outer.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return outer;
    }

    private LinearLayout buildHeaderPillButton(Context ctx, int iconRes, String label, int bgColor, int fgColor, View.OnClickListener onClick) {
        LinearLayout pill = new LinearLayout(ctx);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(12), dp(6), dp(12), dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(bgColor);
        if (bgColor == Color.WHITE || bgColor == 0x26FFFFFF) {
            bg.setStroke(dp(1), 0x550EA5E9);
        }
        pill.setBackground(bg);

        ImageView icon = new ImageView(ctx);
        icon.setImageResource(iconRes);
        icon.setColorFilter(new PorterDuffColorFilter(fgColor, PorterDuff.Mode.SRC_IN));
        pill.addView(icon, LayoutHelper.createLinear(18, 18, Gravity.CENTER_VERTICAL, 0, 0, 6, 0));

        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setTextColor(fgColor);
        pill.addView(tv);

        pill.setOnClickListener(onClick);
        return pill;
    }

    private View buildHiddenAccountCard(Context ctx, int account) {
        boolean isDark = Theme.isCurrentThemeDark();
        TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();

        LinearLayout outer = new LinearLayout(ctx);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(14), dp(5), dp(14), dp(5));

        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(12), dp(12));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(16));
        cardBg.setColor(isDark ? 0xFF1E293B : Color.WHITE);
        cardBg.setStroke(dp(1), isDark ? 0x3338BDF8 : 0x260284C7);
        card.setBackground(cardBg);

        // Avatar with small Eye Badge overlay
        FrameLayout avatarWrap = new FrameLayout(ctx);
        BackupImageView avatarView = new BackupImageView(ctx);
        avatarView.setRoundRadius(dp(24));
        AvatarDrawable avatarDrawable = new AvatarDrawable(user);
        avatarView.setForUserOrChat(user, avatarDrawable);
        avatarWrap.addView(avatarView, LayoutHelper.createFrame(48, 48, Gravity.CENTER));

        FrameLayout eyeMini = new FrameLayout(ctx);
        GradientDrawable eyeMiniBg = new GradientDrawable();
        eyeMiniBg.setShape(GradientDrawable.OVAL);
        eyeMiniBg.setColor(0xFF0EA5E9);
        eyeMiniBg.setStroke(dp(1.5f), isDark ? 0xFF1E293B : Color.WHITE);
        eyeMini.setBackground(eyeMiniBg);
        ImageView eyeMiniIcon = new ImageView(ctx);
        eyeMiniIcon.setImageResource(R.drawable.ic_naya_eye_off);
        eyeMiniIcon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        eyeMini.addView(eyeMiniIcon, LayoutHelper.createFrame(11, 11, Gravity.CENTER));
        avatarWrap.addView(eyeMini, LayoutHelper.createFrame(18, 18, Gravity.BOTTOM | Gravity.RIGHT));

        card.addView(avatarWrap, LayoutHelper.createLinear(48, 48));

        // Name + Phone/Username
        LinearLayout infoCol = new LinearLayout(ctx);
        infoCol.setOrientation(LinearLayout.VERTICAL);

        String displayName = user != null ? UserObject.getUserName(user) : ("Account " + (account + 1));
        TextView nameTv = new TextView(ctx);
        nameTv.setText(displayName);
        nameTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
        nameTv.setTypeface(AndroidUtilities.bold());
        nameTv.setTextColor(isDark ? Color.WHITE : 0xFF0F172A);
        nameTv.setSingleLine(true);
        nameTv.setEllipsize(TextUtils.TruncateAt.END);
        infoCol.addView(nameTv);

        String subLine = "Hidden account";
        if (user != null) {
            String uname = UserObject.getPublicUsername(user);
            if (!TextUtils.isEmpty(uname)) {
                subLine = "@" + uname;
            } else if (!TextUtils.isEmpty(user.phone)) {
                subLine = PhoneFormat.getInstance().format("+" + user.phone);
            }
        }
        if (account == UserConfig.selectedAccount) {
            subLine += " • Active";
        }
        TextView subTv = new TextView(ctx);
        subTv.setText(subLine);
        subTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        subTv.setTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
        subTv.setSingleLine(true);
        subTv.setEllipsize(TextUtils.TruncateAt.END);
        infoCol.addView(subTv);

        card.addView(infoCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 12, 0, 8, 0));

        // Explicit [Show] (Unhide Eye Button)
        LinearLayout showBtn = new LinearLayout(ctx);
        showBtn.setOrientation(LinearLayout.HORIZONTAL);
        showBtn.setGravity(Gravity.CENTER);
        showBtn.setPadding(dp(10), dp(7), dp(10), dp(7));
        GradientDrawable showBg = new GradientDrawable();
        showBg.setCornerRadius(dp(12));
        showBg.setColor(isDark ? 0x2210B981 : 0xFFE6F4EA);
        showBg.setStroke(dp(1), 0x6610B981);
        showBtn.setBackground(showBg);

        ImageView showIcon = new ImageView(ctx);
        showIcon.setImageResource(R.drawable.msg_view_file);
        showIcon.setColorFilter(new PorterDuffColorFilter(0xFF10B981, PorterDuff.Mode.SRC_IN));
        showBtn.addView(showIcon, LayoutHelper.createLinear(16, 16, Gravity.CENTER_VERTICAL, 0, 0, 4, 0));

        TextView showTv = new TextView(ctx);
        showTv.setText("Show");
        showTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        showTv.setTypeface(AndroidUtilities.bold());
        showTv.setTextColor(0xFF10B981);
        showBtn.addView(showTv);

        showBtn.setOnClickListener(v -> {
            NGHiddenAccounts.setHidden(account, false);
            BulletinFactory.of(this)
                    .createSimpleBulletin(R.raw.chats_infotip, "Account is now visible in your main list.")
                    .show();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        card.addView(showBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));

        // Explicit [Open →] Button
        TextView openBtn = new TextView(ctx);
        openBtn.setText("Open →");
        openBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        openBtn.setTypeface(AndroidUtilities.bold());
        openBtn.setTextColor(Color.WHITE);
        openBtn.setGravity(Gravity.CENTER);
        openBtn.setPadding(dp(12), dp(7), dp(12), dp(7));
        GradientDrawable openBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF0284C7, 0xFF4F46E5}
        );
        openBg.setCornerRadius(dp(12));
        openBtn.setBackground(openBg);
        openBtn.setOnClickListener(v -> openAccount(account));
        card.addView(openBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        card.setOnClickListener(v -> openAccount(account));
        card.setOnLongClickListener(v -> {
            ItemOptions o = ItemOptions.makeOptions(this, card);
            NGHiddenAccounts.addHideOption(o, this, account);
            o.show();
            return true;
        });

        outer.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return outer;
    }

    private void openAccount(int account) {
        if (account == UserConfig.selectedAccount) {
            finishFragment();
        } else if (LaunchActivity.instance != null) {
            LaunchActivity.instance.switchToAccount(account, true);
        }
    }

    private View getEmptyView(Context context) {
        if (emptyView != null) return emptyView;
        LinearLayout layout = new LinearLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int h = (listView != null && listView.getMeasuredHeight() > 0)
                        ? listView.getMeasuredHeight() - listView.getPaddingTop() - listView.getPaddingBottom() - dp(120)
                        : 0;
                super.onMeasure(
                        widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(Math.max(h, AndroidUtilities.dp(240)), MeasureSpec.EXACTLY)
                );
            }
        };
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        BackupImageView imageView = new BackupImageView(context);
        imageView.setImageDrawable(new RLottieDrawable(R.raw.utyan_empty, AndroidUtilities.dp(120), AndroidUtilities.dp(120)));
        layout.addView(imageView, LayoutHelper.createLinear(120, 120, Gravity.CENTER_HORIZONTAL));

        TextView textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
        textView.setTypeface(AndroidUtilities.bold());
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourceProvider));
        textView.setGravity(Gravity.CENTER);
        textView.setText("No hidden accounts in vault.\nLong-press any account in Settings to hide it.");
        layout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 32, 16, 32, 0));

        emptyView = layout;
        return emptyView;
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private int dp(float v) {
        return AndroidUtilities.dp(v);
    }
}
