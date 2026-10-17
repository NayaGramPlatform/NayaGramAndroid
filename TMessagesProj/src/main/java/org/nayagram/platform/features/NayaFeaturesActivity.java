package org.nayagram.platform.features;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.nayagram.platform.NayaConfig;
import org.nayagram.platform.translator.InstantTranslatorManager;
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

    private static final int MENU_PRIVACY_AUDIT = 2;
    private static final int MENU_SUPPORT = 3;
    private static final int MENU_RESTORE = 4;
    private static final int MENU_EXPORT_SETTINGS = 5;
    private static final int MENU_HEALTH_CHECK = 6;

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
        item.addSubItem(MENU_HEALTH_CHECK, R.drawable.outline_shield_check, "Feature Health Check");
        item.addSubItem(MENU_EXPORT_SETTINGS, R.drawable.settings_data, "Export Settings Backup");

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_PRIVACY_AUDIT) {
                    showPrivacyAuditDialog(context);
                } else if (id == MENU_SUPPORT) {
                    showSupportDialog(context);
                } else if (id == MENU_RESTORE) {
                    restoreAllDefaults(context);
                } else if (id == MENU_HEALTH_CHECK) {
                    showFeatureHealthCheck(context);
                } else if (id == MENU_EXPORT_SETTINGS) {
                    confirmExportSettings(context);
                }
            }
        });

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = root;

        EditText searchField = new EditText(context);
        searchField.setSingleLine(true);
        searchField.setTextSize(15);
        searchField.setHint("Search features · English / বাংলা / العربية");
        searchField.setPadding(AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14), 0);
        GradientDrawable searchBackground = new GradientDrawable();
        searchBackground.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        searchBackground.setCornerRadius(AndroidUtilities.dp(14));
        searchField.setBackground(searchBackground);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48));
        searchParams.setMargins(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        root.addView(searchField, searchParams);

        // The tutorial is a separate documentation panel, never a live feature card.
        root.addView(buildTutorialBanner(context), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView sectionTitle = new TextView(context);
        sectionTitle.setText("Verified Feature Controls");
        sectionTitle.setTextColor(0xFF075985);
        sectionTitle.setTextSize(16);
        sectionTitle.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        sectionTitle.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(5), AndroidUtilities.dp(18), AndroidUtilities.dp(2));
        root.addView(sectionTitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView sectionDescription = new TextView(context);
        sectionDescription.setText("Only features with a verified behavior path appear below.");
        sectionDescription.setTextColor(0xFF64748B);
        sectionDescription.setTextSize(12);
        sectionDescription.setPadding(AndroidUtilities.dp(18), 0, AndroidUtilities.dp(18), AndroidUtilities.dp(5));
        root.addView(sectionDescription, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(0, 0, 0, AndroidUtilities.dp(20));

        adapter = new FeatureListAdapter(context, this);
        recyclerView.setAdapter(adapter);
        root.addView(recyclerView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.setSearchQuery(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return fragmentView;
    }

    /**
     * A bright tutorial banner kept separate from the actual feature controls.
     * The Details action opens the language picker and never toggles a feature.
     */
    private View buildTutorialBanner(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(15), AndroidUtilities.dp(12),
                AndroidUtilities.dp(15), AndroidUtilities.dp(12));
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFE0F2FE, 0xFFDCFCE7});
        background.setCornerRadius(AndroidUtilities.dp(17));
        background.setStroke(AndroidUtilities.dp(1), 0xFFBAE6FD);
        card.setBackground(background);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(AndroidUtilities.dp(16), AndroidUtilities.dp(3),
                AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        card.setLayoutParams(cardParams);

        TextView title = new TextView(context);
        title.setText("📚 Feature Pipeline · Tutorial");
        title.setTextColor(0xFF075985);
        title.setTextSize(16);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        card.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView description = new TextView(context);
        description.setText("A step-by-step guide to NayaGram features. Tutorial only — it does not activate feature engines.");
        description.setTextColor(0xFF334155);
        description.setTextSize(12);
        description.setLineSpacing(AndroidUtilities.dp(1), 1.08f);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descriptionParams.topMargin = AndroidUtilities.dp(4);
        card.addView(description, descriptionParams);

        LinearLayout footer = new LinearLayout(context);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerParams.topMargin = AndroidUtilities.dp(8);
        card.addView(footer, footerParams);

        TextView languages = new TextView(context);
        languages.setText("🌐 " + InstantTranslatorManager.SUPPORTED_LANGUAGES.length + " languages");
        languages.setTextColor(0xFF166534);
        languages.setTextSize(12);
        languages.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        footer.addView(languages, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView details = new TextView(context);
        details.setText("Details  ›");
        details.setTextColor(0xFFFFFFFF);
        details.setTextSize(13);
        details.setGravity(Gravity.CENTER);
        details.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        details.setPadding(AndroidUtilities.dp(15), AndroidUtilities.dp(8),
                AndroidUtilities.dp(15), AndroidUtilities.dp(8));
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(0xFF0284C7);
        buttonBackground.setCornerRadius(AndroidUtilities.dp(12));
        details.setBackground(buttonBackground);
        details.setContentDescription("Details: open the multilingual Feature Pipeline tutorial");
        details.setOnClickListener(v -> FeatureTutorialDialog.show(context));
        footer.addView(details, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private void showFeatureHealthCheck(Context context) {
        NayaConfig config = NayaConfig.getInstance();
        StringBuilder report = new StringBuilder();
        report.append("NayaGram feature readiness\n\n");
        report.append("External Link Guard: ").append(config.isLinkGuardEnabled() ? "ON" : "OFF").append('\n');
        report.append("Readable Text: ").append(config.isReadableTextEnabled() ? "ON" : "OFF").append('\n');
        report.append("Reduce Motion: ").append(config.isReduceMotion() ? "ON" : "OFF").append('\n');
        report.append("Data Saver: ").append(config.isDataSaverEnabled() ? "ON" : "OFF").append('\n');
        report.append("Storage Scan: AVAILABLE (on demand)").append("\n\n");
        report.append("This report shows local setting state, not a security certification or a guarantee that every optional feature is active.");
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Feature Health Check");
        builder.setMessage(report.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void confirmExportSettings(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Export NayaGram Settings");
        builder.setMessage("Create a shareable JSON backup of NayaGram feature preferences? Custom auto-reply text and account/chat data are excluded.");
        builder.setPositiveButton("Export", (dialog, which) -> {
            try {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("application/json");
                send.putExtra(Intent.EXTRA_SUBJECT, "NayaGram settings backup");
                send.putExtra(Intent.EXTRA_TEXT, NayaConfig.getInstance().exportSettingsJson());
                context.startActivity(Intent.createChooser(send, "Share settings backup"));
            } catch (Exception ignored) {
                AlertDialog.Builder error = new AlertDialog.Builder(context);
                error.setTitle("Export unavailable");
                error.setMessage("No compatible sharing app was found on this device.");
                error.setPositiveButton("OK", null);
                error.show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showPrivacyAuditDialog(Context context) {
        org.nayagram.platform.ui.NGPrivacyAuditDialog.show(context);
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
        builder.setMessage("Are you sure you want to reset all NayaGram feature settings to their factory defaults?");
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
