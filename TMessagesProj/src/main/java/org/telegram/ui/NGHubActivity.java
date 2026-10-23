package org.telegram.ui;
import org.telegram.ui.Stories.recorder.StoryRecorder;
import org.telegram.messenger.FileLog;
import android.app.Activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.nayagram.platform.NayaConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.Locale;

/**
 * NG Hub lists channels and groups this account created, and keeps everyone
 * else's communities in a separate section. Nothing is invented.
 */
public class NGHubActivity extends BaseFragment {
    private static final int SECTION_CHANNELS = 0;
    private static final int SECTION_GROUPS = 1;
    private static final int SECTION_BOTS = 2;
    private static final int SECTION_COMMUNITIES = 3;
    private static final int COLOR_CYAN = 0xFF0284C7;
    private static final int COLOR_TEAL = 0xFF0F766E;
    private static final int COLOR_GREEN = 0xFF16A34A;
    private static final int COLOR_BLUE = 0xFF0369A1;

    private LinearLayout contentColumn;
    private LinearLayout activeRows;
    private TextView[] tabs;
    private TextView swipeToggle;
    private TextView layoutToggle;
    private TextView audienceToggle;
    private EditText searchField;
    private FrameLayout plusSheet;
    private TextView plusButton;
    private FrameLayout detailsOverlay;
    private TextView detailTitle;
    private TextView detailLead;
    private TextView detailPurpose;
    private TextView detailDoes;
    private TextView detailNever;
    private TextView detailPurposeHeader;
    private TextView detailDoesHeader;
    private TextView detailNeverHeader;
    private TextView detailUpdatesHeader;
    private TextView detailUpdates;
    private LinearLayout languageRow;
    private int selectedSection = SECTION_CHANNELS;
    private int detailLang;
    private boolean swipeNavigationEnabled = true;
    private boolean horizontalCards;
    private boolean mineOnly = true;
    private boolean plusOpen;
    private boolean closing;
    private float touchStartX;
    private float touchStartY;

    public NGHubActivity() {
        super();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("NG Hub");
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    closeAnimated();
                }
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        root.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(96));
        scrollView.addView(page, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        page.addView(buildHero(context));
        page.addView(buildShortcuts(context));
        page.addView(buildCreateRow(context));

        audienceToggle = createAction(context, "My Spaces");
        audienceToggle.setOnClickListener(v -> {
            mineOnly = !mineOnly;
            audienceToggle.setText(mineOnly ? "My Spaces" : "All Joined");
            showSection(selectedSection);
        });
        page.addView(audienceToggle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 42, 0, 4, 0, 8));

        searchField = new EditText(context);
        searchField.setSingleLine(true);
        searchField.setTextSize(15);
        searchField.setHint("Search channels, groups, bots, and communities");
        searchField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        searchField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        searchField.setPadding(AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14), 0);
        searchField.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), Theme.getColor(Theme.key_windowBackgroundWhite)));
        page.addView(searchField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 46, 0, 0, 0, 12));
        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                showSection(selectedSection);
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        LinearLayout tabRow = new LinearLayout(context);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(tabRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 46, 0, 0, 0, 10));
        String[] labels = {"Channels", "Groups", "Bots", "Communities"};
        tabs = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int section = i;
            tabs[i] = createTab(context, labels[i]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) lp.leftMargin = AndroidUtilities.dp(5);
            tabRow.addView(tabs[i], lp);
            tabs[i].setOnClickListener(v -> showSection(section));
        }

        LinearLayout toggles = new LinearLayout(context);
        toggles.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(toggles, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 38, 0, 0, 0, 8));
        swipeToggle = createAction(context, "⇆ Swipe: On");
        layoutToggle = createAction(context, "▦ Layout: List");
        toggles.addView(swipeToggle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        layoutParams.leftMargin = AndroidUtilities.dp(8);
        toggles.addView(layoutToggle, layoutParams);
        swipeToggle.setOnClickListener(v -> {
            swipeNavigationEnabled = !swipeNavigationEnabled;
            swipeToggle.setText(swipeNavigationEnabled ? "⇆ Swipe: On" : "⇆ Swipe: Off");
            swipeToggle.setAlpha(swipeNavigationEnabled ? 1f : 0.72f);
        });
        layoutToggle.setOnClickListener(v -> {
            horizontalCards = !horizontalCards;
            layoutToggle.setText(horizontalCards ? "▤ Layout: Cards" : "▦ Layout: List");
            showSection(selectedSection);
        });

        contentColumn = new LinearLayout(context);
        contentColumn.setOrientation(LinearLayout.VERTICAL);
        page.addView(contentColumn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        scrollView.setOnTouchListener((view, event) -> {
            if (!swipeNavigationEnabled) return false;
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchStartX = event.getX();
                touchStartY = event.getY();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                float dx = event.getX() - touchStartX;
                float dy = event.getY() - touchStartY;
                if (Math.abs(dx) > AndroidUtilities.dp(72) && Math.abs(dx) > Math.abs(dy) * 1.25f) {
                    int next = selectedSection + (dx < 0 ? 1 : -1);
                    showSection(Math.max(SECTION_CHANNELS, Math.min(SECTION_COMMUNITIES, next)));
                }
            }
            return false;
        });

        TextView footer = new TextView(context);
        footer.setText("NG Hub was introduced on 16 October 2026. My Channel and My Group list only places this account created. Other communities stay in Everyone. Create Bot opens Telegram's official BotFather.");
        footer.setTextSize(12);
        footer.setTextColor(Theme.isCurrentThemeDark() ? 0xFF94A3B8 : 0xFF475569);
        footer.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(16), AndroidUtilities.dp(4), AndroidUtilities.dp(8));
        page.addView(footer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        plusSheet = buildPlusSheet(context);
        plusSheet.setVisibility(View.GONE);
        plusSheet.setAlpha(0f);
        plusSheet.setScaleX(0.86f);
        plusSheet.setScaleY(0.86f);
        FrameLayout.LayoutParams sheetParams = new FrameLayout.LayoutParams(AndroidUtilities.dp(220), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END);
        sheetParams.rightMargin = AndroidUtilities.dp(16);
        sheetParams.bottomMargin = AndroidUtilities.dp(86);
        root.addView(plusSheet, sheetParams);

        plusButton = new TextView(context);
        plusButton.setText("+");
        plusButton.setTextSize(28);
        plusButton.setGravity(Gravity.CENTER);
        plusButton.setTextColor(0xFFF4FFF8);
        plusButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        plusButton.setContentDescription("Open create menu");
        GradientDrawable plusBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF22C55E, 0xFF15803D});
        plusBg.setShape(GradientDrawable.OVAL);
        plusBg.setStroke(AndroidUtilities.dp(2), 0xCCFFFFFF);
        plusButton.setBackground(plusBg);
        plusButton.setElevation(AndroidUtilities.dp(8));
        plusButton.setOnClickListener(v -> togglePlus());
        FrameLayout.LayoutParams plusParams = new FrameLayout.LayoutParams(AndroidUtilities.dp(58), AndroidUtilities.dp(58), Gravity.BOTTOM | Gravity.END);
        plusParams.rightMargin = AndroidUtilities.dp(16);
        plusParams.bottomMargin = AndroidUtilities.dp(18);
        root.addView(plusButton, plusParams);

        detailsOverlay = buildDetails(context);
        detailsOverlay.setVisibility(View.GONE);
        root.addView(detailsOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        fragmentView = root;
        showSection(selectedSection);
        if (!NayaConfig.getInstance().isReduceMotion()) {
            root.setAlpha(0f);
            root.setTranslationY(AndroidUtilities.dp(16));
            root.post(() -> root.animate().alpha(1f).translationY(0f).setDuration(280).start());
        }
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        showSection(selectedSection);
    }

    private View buildHero(Context context) {
        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        hero.setBackground(round(Theme.getColor(Theme.key_windowBackgroundWhite), 20));
        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.nayagram_intro_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("NayaGram logo");
        hero.addView(logo, LayoutHelper.createLinear(88, 88, Gravity.CENTER_HORIZONTAL));
        TextView title = new TextView(context);
        title.setText("NG Hub");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(COLOR_CYAN);
        title.setShadowLayer(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(2), 0x3038BDF8);
        title.setGravity(Gravity.CENTER);
        hero.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));
        TextView subtitle = new TextView(context);
        subtitle.setText("Your own channels, groups, bots, profile and contacts. Other communities are never labeled as yours.");
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        hero.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 10));
        TextView details = coloredButton(context, "Details", COLOR_CYAN);
        details.setOnClickListener(v -> openDetails());
        hero.addView(details, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 40, Gravity.CENTER_HORIZONTAL));
        LinearLayout.LayoutParams params = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12);
        hero.setLayoutParams(params);
        return hero;
    }

    private View buildShortcuts(Context context) {
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row1 = new LinearLayout(context);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        TextView channel = coloredButton(context, "📣  My Channel", COLOR_CYAN);
        TextView group = coloredButton(context, "👥  My Group", COLOR_TEAL);
        row1.addView(channel, weight());
        LinearLayout.LayoutParams gap = weight();
        gap.leftMargin = AndroidUtilities.dp(8);
        row1.addView(group, gap);
        grid.addView(row1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        LinearLayout row2 = new LinearLayout(context);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        TextView bot = coloredButton(context, "🤖  My Bot", COLOR_GREEN);
        TextView profile = coloredButton(context, "👤  My Profile", COLOR_BLUE);
        row2.addView(bot, weight());
        LinearLayout.LayoutParams gap2 = weight();
        gap2.leftMargin = AndroidUtilities.dp(8);
        row2.addView(profile, gap2);
        LinearLayout.LayoutParams row2Params = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 0, 8, 0, 0);
        grid.addView(row2, row2Params);
        LinearLayout row3 = new LinearLayout(context);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        TextView contacts = coloredButton(context, "📇  My Contact List", COLOR_TEAL);
        TextView liveStream = coloredButton(context, "📡  Live Stream", 0xFF0D9488);
        row3.addView(contacts, weight());
        LinearLayout.LayoutParams gap3 = weight();
        gap3.leftMargin = AndroidUtilities.dp(8);
        row3.addView(liveStream, gap3);
        grid.addView(row3, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 0, 8, 0, 0));

        channel.setOnClickListener(v -> selectMine(SECTION_CHANNELS));
        group.setOnClickListener(v -> selectMine(SECTION_GROUPS));
        bot.setOnClickListener(v -> selectMine(SECTION_BOTS));
        profile.setOnClickListener(v -> openProfile());
        contacts.setOnClickListener(v -> presentFragment(new ContactsActivity(null)));
        liveStream.setOnClickListener(v -> startLiveStream());
        return grid;
    }

    private View buildCreateRow(Context context) {
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView createGroup = createAction(context, "＋  Create Group");
        TextView createChannel = createAction(context, "＋  Create Channel");
        TextView createBot = createAction(context, "＋  Create Bot");
        actions.addView(createGroup, weight());
        LinearLayout.LayoutParams channelParams = weight();
        channelParams.leftMargin = AndroidUtilities.dp(8);
        actions.addView(createChannel, channelParams);
        LinearLayout.LayoutParams botParams = weight();
        botParams.leftMargin = AndroidUtilities.dp(8);
        actions.addView(createBot, botParams);
        TextView liveStreamAction = createAction(context, "📡  Live Stream");
        LinearLayout.LayoutParams liveParams = weight();
        liveParams.leftMargin = AndroidUtilities.dp(8);
        actions.addView(liveStreamAction, liveParams);
        createGroup.setOnClickListener(v -> presentFragment(new GroupCreateActivity(new Bundle())));
        createChannel.setOnClickListener(v -> presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_CHANNEL_CREATE)));
        createBot.setOnClickListener(v -> openBotFather());
        liveStreamAction.setOnClickListener(v -> startLiveStream());
        LinearLayout.LayoutParams params = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 42, 0, 12, 0, 8);
        actions.setLayoutParams(params);
        return actions;
    }

    private FrameLayout buildPlusSheet(Context context) {
        FrameLayout wrap = new FrameLayout(context);
        LinearLayout menu = new LinearLayout(context);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
        menu.setBackground(round(Theme.getColor(Theme.key_windowBackgroundWhite), 18));
        menu.addView(menuItem(context, "📣  Create Channel", v -> {
            togglePlus();
            presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_CHANNEL_CREATE));
        }));
        menu.addView(menuItem(context, "👥  Create Group", v -> {
            togglePlus();
            presentFragment(new GroupCreateActivity(new Bundle()));
        }));
        menu.addView(menuItem(context, "🤖  Create Bot", v -> {
            togglePlus();
            openBotFather();
        }));
        menu.addView(menuItem(context, "📇  My Contact List", v -> {
            togglePlus();
            presentFragment(new ContactsActivity(null));
        }));
        menu.addView(menuItem(context, "📡  Live Stream", v -> {
            togglePlus();
            startLiveStream();
        }));
        wrap.addView(menu, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return wrap;
    }

    private TextView menuItem(Context context, String label, View.OnClickListener listener) {
        TextView item = new TextView(context);
        item.setText(label);
        item.setTextSize(14);
        item.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        item.setTextColor(COLOR_BLUE);
        item.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        item.setOnClickListener(listener);
        return item;
    }

    private void selectMine(int section) {
        mineOnly = true;
        if (audienceToggle != null) audienceToggle.setText("My Spaces");
        showSection(section);
    }

    private void openProfile() {
        TLRPC.User me = UserConfig.getInstance(currentAccount).getCurrentUser();
        if (me == null) return;
        Bundle args = new Bundle();
        args.putLong("user_id", me.id);
        presentFragment(new ProfileActivity(args));
    }

    private void startLiveStream() {
        Activity activity = getParentActivity();
        if (activity == null) return;
        long myId = UserConfig.getInstance(currentAccount).getClientUserId();
        try {
            StoryRecorder.getInstance(activity, currentAccount)
                    .selectedPeerId(myId)
                    .setMode(StoryRecorder.MODE_LIVE)
                    .open(null);
        } catch (Throwable e) {
            FileLog.e(e);
            try {
                StoryRecorder.getInstance(activity, currentAccount)
                        .selectedPeerId(myId)
                        .open(null);
            } catch (Throwable ignored) {
            }
        }
    }

    private void openBotFather() {
        if (getParentActivity() != null) {
            Browser.openUrl(getParentActivity(), "https://t.me/BotFather");
        }
    }

    private void togglePlus() {
        plusOpen = !plusOpen;
        plusButton.animate()
                .rotation(plusOpen ? 45f : 0f)
                .scaleX(plusOpen ? 1.08f : 1f)
                .scaleY(plusOpen ? 1.08f : 1f)
                .setDuration(220)
                .start();
        plusButton.setContentDescription(plusOpen ? "Close create menu" : "Open create menu");
        plusSheet.setVisibility(View.VISIBLE);
        plusSheet.setPivotX(plusSheet.getWidth());
        plusSheet.setPivotY(plusSheet.getHeight());
        plusSheet.animate()
                .alpha(plusOpen ? 1f : 0f)
                .scaleX(plusOpen ? 1f : 0.86f)
                .scaleY(plusOpen ? 1f : 0.86f)
                .setDuration(240)
                .withEndAction(() -> {
                    if (!plusOpen && plusSheet != null) plusSheet.setVisibility(View.GONE);
                })
                .start();
    }

    private void closeAnimated() {
        if (closing) return;
        if (detailsOverlay != null && detailsOverlay.getVisibility() == View.VISIBLE) {
            closeDetails();
            return;
        }
        closing = true;
        View view = fragmentView;
        if (view == null || NayaConfig.getInstance().isReduceMotion()) {
            finishFragment();
            return;
        }
        view.animate().alpha(0f).translationY(AndroidUtilities.dp(12)).setDuration(180).withEndAction(this::finishFragment).start();
    }

    private LinearLayout buildSectionCard(Context context, TextView header, TextView body) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        int cardBg = Theme.isCurrentThemeDark() ? 0xFF1E293B : Theme.getColor(Theme.key_windowBackgroundWhite);
        GradientDrawable cardDrawable = new GradientDrawable();
        cardDrawable.setColor(cardBg);
        cardDrawable.setCornerRadius(AndroidUtilities.dp(18));
        cardDrawable.setStroke(AndroidUtilities.dp(1), Theme.isCurrentThemeDark() ? 0x2E38BDF8 : 0x14000000);
        card.setBackground(cardDrawable);
        if (header != null) {
            card.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
        }
        if (body != null) {
            body.setLineSpacing(AndroidUtilities.dp(2), 1.25f);
            card.addView(body, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
        return card;
    }

    private FrameLayout buildDetails(Context context) {
        FrameLayout overlay = new FrameLayout(context);
        overlay.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        ScrollView scroll = new ScrollView(context);
        overlay.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(28));
        scroll.addView(column, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView back = coloredButton(context, "←  Back to NG Hub", COLOR_BLUE);
        back.setOnClickListener(v -> closeDetails());
        column.addView(back, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 40, 0, 0, 0, 14));

        LinearLayout heroCard = new LinearLayout(context);
        heroCard.setOrientation(LinearLayout.VERTICAL);
        heroCard.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18));
        GradientDrawable head = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF0284C7, 0xFF0369A1});
        head.setCornerRadius(AndroidUtilities.dp(22));
        heroCard.setBackground(head);

        TextView versionBadge = new TextView(context);
        versionBadge.setText("🚀 Version 2.2 • Enhanced October 2026 Edition");
        versionBadge.setTextSize(11);
        versionBadge.setTextColor(0xFFE0F2FE);
        versionBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(0x28FFFFFF);
        badgeBg.setCornerRadius(AndroidUtilities.dp(10));
        versionBadge.setBackground(badgeBg);
        versionBadge.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
        heroCard.addView(versionBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        detailTitle = detailText(context, 26, 0xFFFFFFFF, true);
        detailLead = detailText(context, 14, 0xFFE0F2FE, false);
        heroCard.addView(detailTitle);
        heroCard.addView(detailLead, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
        column.addView(heroCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        detailPurposeHeader = detailText(context, 16, COLOR_CYAN, true);
        detailPurpose = detailText(context, 14, Theme.isCurrentThemeDark() ? 0xFFE2E8F0 : 0xFF334155, false);
        column.addView(buildSectionCard(context, detailPurposeHeader, detailPurpose), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        detailDoesHeader = detailText(context, 16, COLOR_BLUE, true);
        detailDoes = detailText(context, 14, Theme.isCurrentThemeDark() ? 0xFFE2E8F0 : 0xFF334155, false);
        column.addView(buildSectionCard(context, detailDoesHeader, detailDoes), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        detailNeverHeader = detailText(context, 16, 0xFF10B981, true);
        detailNever = detailText(context, 14, Theme.isCurrentThemeDark() ? 0xFFE2E8F0 : 0xFF334155, false);
        column.addView(buildSectionCard(context, detailNeverHeader, detailNever), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        detailUpdatesHeader = detailText(context, 16, 0xFFF59E0B, true);
        detailUpdates = detailText(context, 14, Theme.isCurrentThemeDark() ? 0xFFE2E8F0 : 0xFF334155, false);
        column.addView(buildSectionCard(context, detailUpdatesHeader, detailUpdates), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        TextView langLabel = new TextView(context);
        langLabel.setText("🌐 Language Selection / ভাষা পরিবর্তন");
        langLabel.setTextSize(13);
        langLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        langLabel.setTextColor(COLOR_CYAN);
        langLabel.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), AndroidUtilities.dp(6));
        column.addView(langLabel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        languageRow = new LinearLayout(context);
        languageRow.setOrientation(LinearLayout.HORIZONTAL);
        HorizontalScrollView languages = new HorizontalScrollView(context);
        languages.setHorizontalScrollBarEnabled(false);
        languages.addView(languageRow);
        column.addView(languages, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, 0, 0, 0, 8));
        String[] names = {"🇬🇧 English", "🇧🇩 বাংলা", "🇸🇦 العربية", "🇷🇺 Русский", "🇺🇿 Oʻzbekcha", "🇮🇳 हिन्दी", "🇵🇰 اردو"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            TextView chip = createTab(context, names[i]);
            chip.setPadding(AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14), 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            if (i > 0) lp.leftMargin = AndroidUtilities.dp(6);
            languageRow.addView(chip, lp);
            chip.setOnClickListener(v -> applyDetails(index));
        }
        applyDetails(0);
        return overlay;
    }

    private void applyDetails(int index) {
        detailLang = index;
        String[] pack = detailPack(index);
        boolean rtl = "ar".equals(pack[0]) || "ur".equals(pack[0]);
        int dir = rtl ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR;
        int gravity = rtl ? Gravity.RIGHT : Gravity.LEFT;
        bindDetail(detailTitle, pack[2], dir, gravity);
        bindDetail(detailLead, pack[3], dir, gravity);
        bindDetail(detailPurposeHeader, pack[4], dir, gravity);
        bindDetail(detailPurpose, pack[5], dir, gravity);
        bindDetail(detailDoesHeader, pack[6], dir, gravity);
        bindDetail(detailDoes, pack[7], dir, gravity);
        bindDetail(detailNeverHeader, pack[8], dir, gravity);
        bindDetail(detailNever, pack[9], dir, gravity);
        if (pack.length > 11) {
            bindDetail(detailUpdatesHeader, pack[10], dir, gravity);
            bindDetail(detailUpdates, pack[11], dir, gravity);
        }
        if (languageRow == null) return;
        for (int i = 0; i < languageRow.getChildCount(); i++) {
            View child = languageRow.getChildAt(i);
            child.setAlpha(i == index ? 1f : 0.65f);
        }
    }

    private void bindDetail(TextView view, String text, int direction, int gravity) {
        if (view == null) return;
        view.setText(text);
        view.setTextDirection(direction);
        view.setGravity(gravity);
    }

    private void openDetails() {
        applyDetails(detailLang);
        detailsOverlay.setVisibility(View.VISIBLE);
        if (NayaConfig.getInstance().isReduceMotion()) return;
        detailsOverlay.setAlpha(0f);
        detailsOverlay.setTranslationY(AndroidUtilities.dp(18));
        detailsOverlay.animate().alpha(1f).translationY(0f).setDuration(260).start();
    }

    private void closeDetails() {
        if (NayaConfig.getInstance().isReduceMotion()) {
            detailsOverlay.setVisibility(View.GONE);
            return;
        }
        detailsOverlay.animate().alpha(0f).translationY(AndroidUtilities.dp(12)).setDuration(180).withEndAction(() -> {
            detailsOverlay.setVisibility(View.GONE);
            detailsOverlay.setTranslationY(0f);
        }).start();
    }

    private String[] detailPack(int index) {
        switch (index) {
            case 1:
                return new String[]{
                        "bn", "বাংলা", "এনজি হাব",
                        "এনজি হাব আপনার নিজের তৈরি চ্যানেল, গ্রুপ ও বট, আর প্রোফাইল ও কন্টাক্ট লিস্টের একটি ব্যক্তিগত ঘর।",
                        "🎯 উদ্দেশ্য", "নিজের টেলিগ্রাম জায়গাগুলো এক স্ক্রিনে রাখা, এবং অন্যের কমিউনিটিকে নিজের বলে না দেখানো।",
                        "✨ এটি যা করে", "এই অ্যাকাউন্টের তৈরি চ্যানেল ও গ্রুপ দেখায়, প্রোফাইল ও কন্টাক্ট লিস্ট খোলে, এবং বটের জন্য অফিসিয়াল বটফাদার খোলে।",
                        "🛡️ এটি যা করে না", "মিথ্যা চ্যানেল বা গ্রুপ বানায় না, এবং অন্য কারও কমিউনিটিকে আপনার বলে না।",
                        "🚀 সর্বশেষ আপডেট ও নতুন সুবিধাসমূহ", "• ডান থেকে বামে কার্ড স্লাইড এনিমেশন ও প্রিমিয়াম টপ-বটম বর্ডার।\n• ডার্ক ও লাইট মোডে স্পষ্ট ও উজ্জ্বল টেক্সট কন্ট্রাস্ট।\n• এনজি হাব থেকে সরাসরি এক-ট্যাপ লাইভ স্ট্রিমিং ও ব্রডকাস্ট।\n• নোভাগ্রাম স্টাইল ফোল্ডার কার্ডস ও ডাইনামিক ভেক্টর আইকন।"
                };
            case 2:
                return new String[]{
                        "ar", "العربية", "إن جي هب",
                        "إن جي هب هو بيتك الشخصي للقنوات والمجموعات والبوتات التي تنشئها، مع ملفك وقائمة جهات الاتصال.",
                        "🎯 الغاية", "أن تبقى أماكنك في شاشة واحدة، وألا تُعرض مجتمعات الآخرين كأنها ملكك.",
                        "✨ ماذا يفعل", "يعرض ما أنشأه هذا الحساب، ويفتح ملفك وجهات الاتصال، ويفتح بوت فادر الرسمي.",
                        "🛡️ ما لا يفعله", "لا يخترع قنوات أو مجموعات، ولا يضع اسمك على مجتمع شخص آخر.",
                        "🚀 آخر التحديثات والتحسينات", "• حركة انزلاقية للبطاقات من اليمين إلى اليسار مع حدود علوية وسفلية أنيقة.\n• تباين نصوص فائق الوضوح في الوضعين الداكن والفاتح.\n• اختصار مباشر لبدء البث المباشر من إن جي هب.\n• بطاقات مجلدات مستوحاة من نوفاغرام مع أيقونات ديناميكية."
                };
            case 3:
                return new String[]{
                        "ru", "Русский", "NG Hub",
                        "NG Hub — личный экран для каналов, групп и ботов, которые создали вы, а также профиля и контактов.",
                        "🎯 Задача", "Держать свои места на одном экране и не показывать чужие сообщества как ваши.",
                        "✨ Что делает", "Показывает созданное этим аккаунтом, открывает профиль и контакты и официального BotFather.",
                        "🛡️ Чего не делает", "Не придумывает каналы или группы и не выдаёт чужое сообщество за ваше.",
                        "🚀 Новые улучшения", "• Плавная анимация карточек справа налево с верхней и нижней границей.\n• Высококонтрастный четкий текст в темной и светлой темах.\n• Быстрый запуск прямого эфира прямо из NG Hub.\n• Карточки папок в стиле Novagram с векторными иконками."
                };
            case 4:
                return new String[]{
                        "uz", "Oʻzbekcha", "NG Hub",
                        "NG Hub — o‘zingiz yaratgan kanal, guruh va botlar, hamda profil va kontaktlar uchun shaxsiy joy.",
                        "🎯 Maqsad", "O‘z joylaringizni bitta ekranda saqlash va begona hamjamiyatni sizniki deb ko‘rsatmaslik.",
                        "✨ Nima qiladi", "Bu hisob yaratgan joylarni ko‘rsatadi, profil va kontaktlarni ochadi, bot uchun rasmiy BotFather’ni ochadi.",
                        "🛡️ Nima qilmaydi", "Soxta kanal yoki guruh o‘ylab topmaydi va birovning hamjamiyatini sizniki deb belgilamaydi.",
                        "🚀 So‘nggi yangilanishlar", "• O‘ngdan chapga kartalar silliq animatsiyasi va chiroyli chegaralar.\n• Qorong‘i va yorug‘ rejimda yuqori kontrastli aniq matnlar.\n• NG Hub ichidan to‘g‘ridan-to‘g‘ri jonli efir tugmasi.\n• Novagram uslubidagi jild kartalari va vektor ikonkalari."
                };
            case 5:
                return new String[]{
                        "hi", "हिन्दी", "एनजी हब",
                        "एनजी हब उन चैनल, ग्रुप और बॉट का निजी घर है जिन्हें आपने बनाया है, साथ में प्रोफ़ाइल और संपर्क सूची।",
                        "🎯 उद्देश्य", "अपनी जगहें एक स्क्रीन पर रखना, এবং किसी और की कम्युनिटी को अपनी बताकर न दिखाना।",
                        "✨ यह क्या करता है", "इस खाते के बनाए चैनल और ग्रुप दिखाता है, प्रोफ़ाइल और संपर्क खोलता है, और आधिकारिक बॉटफादर खोलता है।",
                        "🛡️ यह नहीं करता", "नकली चैनल या ग्रुप नहीं गढ़ता, और किसी दूसरे की कम्युनिटी पर आपका नाम नहीं लगाता।",
                        "🚀 नवीनतम अपडेट और सुधार", "• दाएँ से बाएँ कार्ड स्लाइड एनीमेशन और आकर्षक टॉप-बॉटम बॉर्डर।\n• डार्क और लाइट मोड में स्पष्ट और चमकदार टेक्स्ट कंट्रास्ट।\n• NG Hub से सीधे एक-टैप लाइव स्ट्रीमिंग।\n• नोवाग्राम स्टाइल फोल्डर कार्ड्स और ऑटो-ऐड सपोर्ट।"
                };
            case 6:
                return new String[]{
                        "ur", "اردو", "این جی ہب",
                        "این جی ہب ان چینلز، گروپس اور بوٹس کا ذاتی گھر ہے جو آپ نے بنائے، ساتھ پروفائل اور رابطہ فہرست।",
                        "🎯 مقصد", "اپنی جگہوں کو ایک اسکرین پر رکھنا, اور کسی اور کی کمیونٹی کو اپنی بنا کر نہیں دکھانا।",
                        "✨ یہ کیا کرتا ہے", "اس اکاؤنٹ کے بنائے چینل اور گروپ دکھاتا ہے، پروفائل اور رابطے کھولتا ہے، اور سرکاری بوٹ فادر کھولتا ہے۔",
                        "🛡️ یہ نہیں کرتا", "جعلی چینل یا گروپ نہیں بناتا، اور کسی دوسرے کی کمیونٹی کو آپ کی نہیں لکھتا।",
                        "🚀 تازہ ترین اپڈیٹس اور سہولیات", "• دائیں سے بائیں کارڈز کی خوبصورت سلائیڈ اینیمیشن اور بارڈرز۔\n• ڈارک اور لائٹ موڈ میں واضح اور روشن ٹیکسٹ کنٹراسٹ।\n• این جی ہب سے براہ راست ون-ٹیپ لائیو اسٹریمنگ۔\n• نوواگرام اسٹائل فولڈر کارڈز اور آٹو-ایڈ سپورٹ।"
                };
            default:
                return new String[]{
                        "en", "English", "NG Hub",
                        "NG Hub is your personal home for the channels, groups and bots you create, together with your profile and contact list.",
                        "🎯 Purpose", "Keep your own Telegram places on one screen, and never present someone else’s community as yours.",
                        "✨ What it does", "It lists channels and groups this account created, opens your profile and contact list, and opens the official BotFather to create a bot.",
                        "🛡️ What it never does", "It does not invent channels or groups, and it does not label another person’s community as yours.",
                        "🚀 Latest Enhancements", "• Right-to-left cards slide animation with subtle top & bottom borders.\n• High-contrast crisp typography in Dark and Light modes.\n• One-tap Live Streaming shortcut directly from NG Hub.\n• Novagram-style folder cards with vector icons & auto-add support."
                };
        }
    }

    private void showSection(int section) {
        selectedSection = Math.max(SECTION_CHANNELS, Math.min(SECTION_COMMUNITIES, section));
        if (contentColumn == null || getContext() == null) return;
        updateTabStyles();
        contentColumn.removeAllViews();

        TextView note = new TextView(getContext());
        note.setTextSize(13);
        note.setTextColor(Theme.isCurrentThemeDark() ? 0xFFCBD5E1 : 0xFF475569);
        note.setPadding(0, 0, 0, AndroidUtilities.dp(8));
        if (mineOnly) {
            note.setText(selectedSection == SECTION_BOTS
                    ? "My Bot shows bots you marked as yours. Tap All Joined to see bot chats from every folder, then long-press your bot to add or remove it from My Bot. Telegram's chat list does not expose bot ownership automatically."
                    : selectedSection == SECTION_COMMUNITIES
                    ? "Telegram Communities are listed separately from channels and groups. This section uses Telegram's actual community dialog entries; linked chats remain in their own sections."
                    : "Only channels and groups this account created. They appear here automatically from your chat list.");
        } else {
            note.setText("All Joined: channels, groups, bots and Telegram Communities available in this account. Community entries are kept separate from their linked chats; ownership is shown only when Telegram confirms you are the creator.");
        }
        contentColumn.addView(note, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (horizontalCards) {
            LinearLayout cardsWrapper = new LinearLayout(getContext());
            cardsWrapper.setOrientation(LinearLayout.VERTICAL);

            View topBorder = new View(getContext());
            topBorder.setBackgroundColor(Theme.isCurrentThemeDark() ? 0x3300D4FF : 0x1F0088CC);
            cardsWrapper.addView(topBorder, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 6, 0, 6));

            HorizontalScrollView horizontal = new HorizontalScrollView(getContext());
            horizontal.setHorizontalScrollBarEnabled(false);
            activeRows = new LinearLayout(getContext());
            activeRows.setOrientation(LinearLayout.HORIZONTAL);
            horizontal.addView(activeRows, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            cardsWrapper.addView(horizontal, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            View bottomBorder = new View(getContext());
            bottomBorder.setBackgroundColor(Theme.isCurrentThemeDark() ? 0x3300D4FF : 0x1F0088CC);
            cardsWrapper.addView(bottomBorder, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 6, 0, 6));

            contentColumn.addView(cardsWrapper, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            if (!NayaConfig.getInstance().isReduceMotion()) {
                cardsWrapper.setTranslationX(AndroidUtilities.dp(48));
                cardsWrapper.setAlpha(0.2f);
                cardsWrapper.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(300)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator(1.4f))
                        .start();
            }
        } else {
            activeRows = contentColumn;
        }

        MessagesController controller = MessagesController.getInstance(currentAccount);
        // Read dialogs from every folder first (including archived chats), then merge
        // the main folder without duplicating entries. The old fallback only loaded
        // all folders when the main folder was empty, hiding archived bot chats.
        ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>();
        ArrayList<TLRPC.Dialog> allDialogs = controller.getAllDialogs();
        if (allDialogs != null) dialogs.addAll(allDialogs);
        ArrayList<TLRPC.Dialog> mainDialogs = controller.getDialogs(0);
        if (mainDialogs != null) {
            for (TLRPC.Dialog mainDialog : mainDialogs) {
                if (mainDialog == null) continue;
                boolean alreadyPresent = false;
                for (TLRPC.Dialog existing : dialogs) {
                    if (existing != null && existing.id == mainDialog.id
                            && existing.getClass() == mainDialog.getClass()) {
                        alreadyPresent = true;
                        break;
                    }
                }
                if (!alreadyPresent) dialogs.add(mainDialog);
            }
        }
        int visibleCount = 0;
        String query = searchField == null || searchField.getText() == null
                ? "" : searchField.getText().toString().trim().toLowerCase(Locale.ROOT);

        if (dialogs != null) {
            for (int i = 0; i < dialogs.size(); i++) {
                TLRPC.Dialog dialog = dialogs.get(i);
                if (dialog == null || DialogObject.isEncryptedDialog(dialog.id)) continue;
                TLRPC.Chat chat = null;
                TLRPC.User user = null;
                int type;
                if (dialog instanceof TLRPC.TL_dialogCommunity) {
                    // Telegram represents a Community as its own dialog wrapper.
                    // Do not classify it as a broadcast channel or an ordinary group.
                    chat = controller.getChat(((TLRPC.TL_dialogCommunity) dialog).community_id);
                    if (chat == null || ChatObject.isNotInChat(chat)) continue;
                    type = SECTION_COMMUNITIES;
                } else if (DialogObject.isChatDialog(dialog.id)) {
                    chat = controller.getChat(-dialog.id);
                    if (chat == null || ChatObject.isNotInChat(chat)) continue;
                    type = ChatObject.isChannel(chat) && !chat.megagroup ? SECTION_CHANNELS : SECTION_GROUPS;
                } else if (DialogObject.isUserDialog(dialog.id)) {
                    user = controller.getUser(dialog.id);
                    if (user == null || !user.bot) continue;
                    type = SECTION_BOTS;
                } else {
                    continue;
                }
                boolean owned = chat != null && chat.creator;
                if (mineOnly && type != SECTION_BOTS && !owned) continue;
                if (mineOnly && type == SECTION_BOTS && (user == null || !isMarkedMyBot(user.id))) continue;
                // Bot ownership is not exposed by the dialog object; My Bot uses an explicit local user selection.
                if (type != selectedSection) continue;
                String title = chat != null ? (chat.title == null ? "" : chat.title)
                        : (user.first_name == null ? "" : user.first_name) + (user.last_name == null ? "" : " " + user.last_name);
                MessageObject lastMessage = null;
                ArrayList<MessageObject> messages = controller.dialogMessage.get(dialog.id);
                if (messages != null && !messages.isEmpty()) lastMessage = messages.get(0);
                String preview = lastMessage == null || lastMessage.messageText == null
                        ? (type == SECTION_BOTS ? "Bot account" : "No recent message")
                        : lastMessage.messageText.toString().trim();
                if (!query.isEmpty() && !title.toLowerCase(Locale.ROOT).contains(query)
                        && !preview.toLowerCase(Locale.ROOT).contains(query)) continue;
                addDialogRow(dialog, chat, user, title, preview, lastMessage);
                visibleCount++;
            }
        }

        if (visibleCount == 0) {
            TextView empty = new TextView(getContext());
            empty.setText(!query.isEmpty() ? "No matches for \"" + query + "\"."
                    : selectedSection == SECTION_BOTS && mineOnly
                    ? "No bots marked as yours yet. Tap All Joined, long-press your bot chat, and choose My Bot. You can also open BotFather to view bots created by your account."
                    : selectedSection == SECTION_COMMUNITIES
                    ? "No Telegram Community entries are available in the loaded chat list."
                    : mineOnly ? "Nothing you created is in this list yet."
                    : "No joined channels, groups, bots or communities are available in the loaded chat list.");
            empty.setTextSize(15);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24));
            empty.setTextColor(Theme.isCurrentThemeDark() ? 0xFFCBD5E1 : 0xFF64748B);
            contentColumn.addView(empty, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            if (selectedSection == SECTION_BOTS && mineOnly && query.isEmpty()) {
                TextView botFather = coloredButton(getContext(), "Open official BotFather", COLOR_GREEN);
                botFather.setOnClickListener(v -> openBotFather());
                contentColumn.addView(botFather, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, 0, 4, 0, 8));
            }
        }
    }

    private void updateTabStyles() {
        if (tabs == null) return;
        boolean isDark = Theme.isCurrentThemeDark();
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setTextColor(i == selectedSection ? COLOR_CYAN : (isDark ? 0xFFCBD5E1 : 0xFF64748B));
            tabs[i].setAlpha(i == selectedSection ? 1f : 0.78f);
        }
    }

    private void addDialogRow(TLRPC.Dialog dialog, TLRPC.Chat chat, TLRPC.User user, String titleText, String previewText, MessageObject lastMessage) {
        Context context = getContext();
        if (context == null || activeRows == null) return;
        boolean isDark = Theme.isCurrentThemeDark();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(horizontalCards ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        row.setGravity(horizontalCards ? Gravity.TOP : Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        AvatarDrawable avatarDrawable = new AvatarDrawable();
        BackupImageView avatar = new BackupImageView(context);
        avatar.setRoundRadius(AndroidUtilities.dp(24));
        if (chat != null) {
            avatarDrawable.setInfo(currentAccount, chat);
            avatar.setForUserOrChat(chat, avatarDrawable);
        } else if (user != null) {
            avatarDrawable.setInfo(currentAccount, user);
            avatar.setForUserOrChat(user, avatarDrawable);
        }
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(48), AndroidUtilities.dp(48));
        if (horizontalCards) avatarParams.bottomMargin = AndroidUtilities.dp(10);
        else avatarParams.rightMargin = AndroidUtilities.dp(12);
        row.addView(avatar, avatarParams);

        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(context);
        title.setText(titleText.isEmpty() ? "Untitled" : titleText);
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        if (chat != null) {
            title.setTextColor(0xFF00D4FF); // Cyan title for channels & groups
        } else {
            title.setTextColor(isDark ? 0xFFF8FAFC : Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        }
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textColumn.addView(title, LayoutHelper.createLinear(horizontalCards ? AndroidUtilities.dp(210) : LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView preview = new TextView(context);
        preview.setText(previewText);
        preview.setTextSize(13);
        boolean isLiveStream = previewText != null && (previewText.startsWith("Live stream") || previewText.toLowerCase(Locale.ROOT).contains("live stream"));
        if (isLiveStream) {
            preview.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
            preview.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        } else {
            preview.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF475569);
        }
        preview.setMaxLines(horizontalCards ? 3 : 2);
        preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textColumn.addView(preview, LayoutHelper.createLinear(horizontalCards ? AndroidUtilities.dp(210) : LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
        row.addView(textColumn, horizontalCards
                ? LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT)
                : new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.setOnClickListener(v -> {
            Bundle args = new Bundle();
            if (chat != null) args.putLong("chat_id", chat.id);
            else if (user != null) args.putLong("user_id", user.id);
            else return;
            presentFragment(new ChatActivity(args));
        });
        if (user != null && user.bot) {
            row.setOnLongClickListener(v -> {
                boolean marked = !isMarkedMyBot(user.id);
                getMyBotsPreferences().edit().putBoolean("bot_" + user.id, marked).apply();
                Toast.makeText(context, marked ? "Added to My Bot" : "Removed from My Bot", Toast.LENGTH_SHORT).show();
                showSection(selectedSection);
                return true;
            });
            row.setContentDescription(titleText + (isMarkedMyBot(user.id) ? ", marked as My Bot. Long press to remove." : ", long press to add to My Bot."));
        }

        LinearLayout.LayoutParams rowParams;
        if (horizontalCards) {
            rowParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(260), ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.rightMargin = AndroidUtilities.dp(10);
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setCornerRadius(AndroidUtilities.dp(16));
            cardBg.setColor(isDark ? 0xFF1E293B : Theme.getColor(Theme.key_windowBackgroundWhite));
            cardBg.setStroke(AndroidUtilities.dp(1), isDark ? 0x2438BDF8 : 0x14000000);
            row.setBackground(cardBg);
        } else {
            rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        }
        activeRows.addView(row, rowParams);
    }

    private SharedPreferences getMyBotsPreferences() {
        Context context = getParentActivity();
        if (context == null) context = org.telegram.messenger.ApplicationLoader.applicationContext;
        return context.getSharedPreferences("ng_hub_my_bots", Context.MODE_PRIVATE);
    }

    private boolean isMarkedMyBot(long userId) {
        return getMyBotsPreferences().getBoolean("bot_" + userId, false);
    }

    private TextView createAction(Context context, String label) {
        TextView action = new TextView(context);
        action.setText(label);
        action.setTextSize(13);
        action.setGravity(Gravity.CENTER);
        action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        action.setTextColor(COLOR_CYAN);
        action.setBackground(round(Theme.getColor(Theme.key_windowBackgroundWhite), 12));
        action.setPadding(AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6), 0);
        action.setContentDescription(label);
        return action;
    }

    private TextView createTab(Context context, String label) {
        TextView tab = new TextView(context);
        tab.setText(label);
        tab.setTextSize(14);
        tab.setGravity(Gravity.CENTER);
        tab.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tab.setContentDescription(label);
        tab.setBackground(round(Theme.getColor(Theme.key_windowBackgroundWhite), 12));
        tab.setTextColor(COLOR_CYAN);
        return tab;
    }

    private TextView coloredButton(Context context, String label, int color) {
        TextView button = new TextView(context);
        button.setText(label);
        button.setTextSize(13);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(0xFFF7FBFF);
        button.setBackground(round(color, 16));
        button.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        button.setContentDescription(label);
        return button;
    }

    private TextView detailText(Context context, int size, int color, boolean bold) {
        TextView text = new TextView(context);
        text.setTextSize(size);
        text.setTextColor(color);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4));
        return text;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(AndroidUtilities.dp(radiusDp));
        return drawable;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
    }
}
