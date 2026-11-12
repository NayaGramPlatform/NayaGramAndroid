package org.nayagram.platform.security;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * NGHiddenAccountsActivity - Settings screen for Hidden Accounts (Novagram HiddenAccountsScreen style).
 */
public class NGHiddenAccountsActivity extends UniversalFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int TURN_ON = 1;
    private static final int CHANGE_CODE = 2;
    private static final int FINGERPRINT = 3;
    private static final int TURN_OFF = 4;
    private static final int VIBRATE = 5;
    private static final int HIDE_ON_BACKGROUND = 6;

    private int pendingHideAccount = -1;

    public NGHiddenAccountsActivity() {
        this(-1);
    }

    public NGHiddenAccountsActivity(int pendingHideAccount) {
        this.pendingHideAccount = pendingHideAccount;
    }

    public static void open(BaseFragment fragment) {
        if (fragment == null) return;
        if (NGHiddenAccounts.hasCode()) {
            NGHiddenAccounts.askCode(fragment, () -> {
                NGHiddenAccounts.unlock();
                fragment.presentFragment(new NGHiddenAccountsActivity(), false, true);
            });
        } else {
            fragment.presentFragment(new NGHiddenAccountsActivity());
        }
    }

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
            if (lockedAgain()) {
                finishFragment(false);
            } else if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        }
    }

    private boolean lockedAgain() {
        return NGHiddenAccounts.hasCode() && !NGHiddenAccounts.isUnlocked();
    }

    @Override
    public View createView(Context context) {
        View view = super.createView(context);
        listView.setSections();
        listView.adapter.setApplyBackground(false);
        actionBar.setAdaptiveBackground(listView);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (lockedAgain()) {
            finishFragment(false);
            return;
        }
        if (pendingHideAccount >= 0 && NGHiddenAccounts.hasCode()) {
            int acc = pendingHideAccount;
            pendingHideAccount = -1;
            NGHiddenAccounts.requestHide(this, acc);
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (!NGHiddenAccounts.hasCode()) {
            items.add(UItem.asTopView(
                    "Hide extra accounts behind a private passcode. Long-press any account in Settings to hide it, and long-press Add Account to view hidden accounts.",
                    R.raw.utyan_passcode
            ));
            items.add(UItem.asButton(TURN_ON, "Turn On Hidden Accounts").accent());
            items.add(UItem.asShadow(null));
            return;
        }

        items.add(UItem.asTopView(
                "Hidden accounts stay concealed in your account list. Long-press Add Account in Settings and enter your passcode to open them.",
                R.raw.utyan_passcode
        ));
        items.add(UItem.asButton(CHANGE_CODE, "Change passcode"));
        if (NGHiddenAccounts.isFingerprintHardwareAvailable()) {
            items.add(UItem.asCheck(FINGERPRINT, "Unlock with fingerprint").setChecked(NGHiddenAccounts.isFingerprintEnabled()));
        }
        items.add(UItem.asCheck(VIBRATE, "Vibrate on tap").setChecked(NGHiddenAccounts.isVibrateEnabled()));
        items.add(UItem.asShadow(null));
        items.add(UItem.asCheck(HIDE_ON_BACKGROUND, "Hide when leaving app").setChecked(NGHiddenAccounts.isHideOnBackground()));
        items.add(UItem.asShadow("Automatically lock hidden accounts when you leave NayaGram or lock your screen."));
        items.add(UItem.asButton(TURN_OFF, "Turn Off Hidden Accounts").red());
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case TURN_ON:
            case CHANGE_CODE:
                NGHiddenAccounts.openSetOrChangeCode(this, () -> {
                    if (pendingHideAccount >= 0 && NGHiddenAccounts.hasCode()) {
                        int acc = pendingHideAccount;
                        pendingHideAccount = -1;
                        NGHiddenAccounts.requestHide(this, acc);
                    }
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                });
                break;
            case FINGERPRINT: {
                boolean next = !NGHiddenAccounts.isFingerprintEnabled();
                NGHiddenAccounts.setFingerprintEnabled(next);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(next);
                }
                break;
            }
            case VIBRATE: {
                boolean next = !NGHiddenAccounts.isVibrateEnabled();
                NGHiddenAccounts.setVibrateEnabled(next);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(next);
                }
                break;
            }
            case HIDE_ON_BACKGROUND: {
                boolean next = !NGHiddenAccounts.isHideOnBackground();
                NGHiddenAccounts.setHideOnBackground(next);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(next);
                }
                break;
            }
            case TURN_OFF:
                confirmTurnOff();
                break;
        }
    }

    private void confirmTurnOff() {
        Context context = getParentActivity();
        if (context == null) return;
        AlertDialog dialog = new AlertDialog.Builder(context, resourceProvider)
                .setTitle("Turn Off Hidden Accounts")
                .setMessage("All hidden accounts will become visible in your main account list again and your hidden passcode will be removed.")
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .setPositiveButton("Turn Off", (d, w) -> {
                    NGHiddenAccounts.disable();
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                })
                .create();
        showDialog(dialog);
        View btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (btn instanceof TextView) {
            ((TextView) btn).setTextColor(Theme.getColor(Theme.key_text_RedBold, resourceProvider));
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
