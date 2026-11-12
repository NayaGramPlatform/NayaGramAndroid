package org.nayagram.platform.security;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.SettingsActivity;

import java.util.ArrayList;
import java.util.Collections;

/**
 * NGHiddenAccountsListActivity - Vault list of hidden accounts opened by long-pressing "Add Account" + passcode.
 */
public class NGHiddenAccountsListActivity extends UniversalFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int ACCOUNT_BASE = 1000;
    private static final int LOCK_MENU_ID = 1;
    private View emptyView;

    @Override
    protected CharSequence getTitle() {
        return "Hidden accounts";
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
        actionBar.createMenu().addItem(LOCK_MENU_ID, R.drawable.outline_header_lock_24)
                .setContentDescription("Lock hidden accounts");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == LOCK_MENU_ID) {
                    NGHiddenAccounts.lock();
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
        ArrayList<Integer> hidden = new ArrayList<>();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (NGHiddenAccounts.isHidden(a)) {
                hidden.add(a);
            }
        }
        Collections.sort(hidden, (o1, o2) -> Long.compare(UserConfig.getInstance(o1).loginTime, UserConfig.getInstance(o2).loginTime));

        if (hidden.isEmpty()) {
            items.add(UItem.asCustomShadow(getEmptyView(getContext())));
            return;
        }
        for (int account : hidden) {
            items.add(SettingsActivity.AccountCell.Factory.of(ACCOUNT_BASE + account, account));
        }
        items.add(UItem.asShadow("Tap an account to open it, or long-press to make it visible again."));
    }

    private View getEmptyView(Context context) {
        if (emptyView != null) return emptyView;
        LinearLayout layout = new LinearLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int h = (listView != null && listView.getMeasuredHeight() > 0)
                        ? listView.getMeasuredHeight() - listView.getPaddingTop() - listView.getPaddingBottom()
                        : 0;
                super.onMeasure(
                        widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(Math.max(h, AndroidUtilities.dp(260)), MeasureSpec.EXACTLY)
                );
            }
        };
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        BackupImageView imageView = new BackupImageView(context);
        imageView.setImageDrawable(new RLottieDrawable(R.raw.utyan_empty, AndroidUtilities.dp(120), AndroidUtilities.dp(120)));
        layout.addView(imageView, LayoutHelper.createLinear(120, 120, Gravity.CENTER_HORIZONTAL));

        TextView textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setTypeface(AndroidUtilities.bold());
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourceProvider));
        textView.setGravity(Gravity.CENTER);
        textView.setText("No hidden accounts yet.\nLong-press any account in Settings to hide it.");
        layout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 32, 16, 32, 0));

        emptyView = layout;
        return emptyView;
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (!item.instanceOf(SettingsActivity.AccountCell.Factory.class)) return;
        int account = item.intValue;
        if (account == UserConfig.selectedAccount) {
            finishFragment();
        } else if (LaunchActivity.instance != null) {
            LaunchActivity.instance.switchToAccount(account, true);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (!item.instanceOf(SettingsActivity.AccountCell.Factory.class)) return false;
        ItemOptions o = ItemOptions.makeOptions(this, view);
        NGHiddenAccounts.addHideOption(o, this, item.intValue);
        o.setScrimViewBackground(listView.getClipBackground(view)).show();
        return true;
    }
}
