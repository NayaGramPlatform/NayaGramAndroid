package org.nayagram.platform.features;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.nayagram.platform.NayaConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;

/**
 * NayaFeaturesActivity - Official UI screen displaying NayaGram exclusive features.
 * Features an interactive 3-dots menu with support options, pipeline overview, and restore defaults.
 */
public class NayaFeaturesActivity extends BaseFragment {

    private static final int MENU_ALL_DETAILS = 1;
    private static final int MENU_PRIVACY_AUDIT = 2;
    private static final int MENU_SUPPORT = 3;
    private static final int MENU_RESTORE = 4;

    public static final String SUPPORT_EMAIL = "support.nayagram@gmail.com";

    private RecyclerView recyclerView;
    private FeatureListAdapter adapter;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("NayaGram Features");

        // 3-dots overflow menu
        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem item = menu.addItem(0, R.drawable.ic_ab_other);
        item.addSubItem(MENU_ALL_DETAILS, R.drawable.msg_help, "All Features Pipeline");
        item.addSubItem(MENU_PRIVACY_AUDIT, R.drawable.outline_shield_check, "Privacy & Stealth Audit");
        item.addSubItem(MENU_SUPPORT, R.drawable.msg_send, "Contact Support");
        item.addSubItem(MENU_RESTORE, R.drawable.msg_delete, "Restore Defaults");

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_ALL_DETAILS) {
                    if (adapter != null) {
                        adapter.showAllDetailsPipelineDialog();
                    }
                } else if (id == MENU_PRIVACY_AUDIT) {
                    showPrivacyAuditDialog(context);
                } else if (id == MENU_SUPPORT) {
                    showSupportDialog(context);
                } else if (id == MENU_RESTORE) {
                    restoreAllDefaults(context);
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout container = (FrameLayout) fragmentView;

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(0, 0, 0, AndroidUtilities.dp(20));

        adapter = new FeatureListAdapter(context);
        recyclerView.setAdapter(adapter);

        container.addView(recyclerView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        return fragmentView;
    }

    private void showPrivacyAuditDialog(Context context) {
        NayaConfig cfg = NayaConfig.getInstance();
        StringBuilder audit = new StringBuilder();
        audit.append("✦ NayaGram Privacy & Security Status ✦\n\n");
        audit.append("• Anonymous Stories: ").append(cfg.isAnonymousStories() ? "[ACTIVE - Hidden]" : "[Inactive]").append("\n");
        audit.append("• Biometric Locker: ").append(cfg.isBiometricChatLockerEnabled() ? "[ACTIVE - Locked]" : "[Inactive]").append("\n");
        audit.append("• Focus Mode: ").append(cfg.isFocusModeEnabled() ? "[ACTIVE]" : "[Inactive]").append("\n\n");
        audit.append("Your Telegram session is safeguarded with MTProto 2.0 encryption and local biometric security.");

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("🛡️ Privacy & Stealth Audit");
        builder.setMessage(audit.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showSupportDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("💬 NayaGram Official Support");
        builder.setMessage("Need help, feature requests, or have questions regarding NayaGram?\n\nOfficial Email: " + SUPPORT_EMAIL + "\n\nOur support team is available 24/7 to assist you.");
        builder.setPositiveButton("Email Us", (dialog, which) -> {
            try {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:" + SUPPORT_EMAIL));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "NayaGram Support & Inquiry");
                context.startActivity(emailIntent);
            } catch (Exception ignored) {
            }
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void restoreAllDefaults(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("🔄 Restore Default Settings");
        builder.setMessage("Are you sure you want to reset all 17 exclusive features to their initial factory settings?");
        builder.setPositiveButton("Reset", (dialog, which) -> {
            NayaConfig.getInstance().resetToDefaults();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "All features restored to defaults").show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
