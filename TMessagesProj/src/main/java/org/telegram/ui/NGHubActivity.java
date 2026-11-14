package org.telegram.ui;

import org.telegram.messenger.BuildVars;
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
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.LocaleController;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/**
 * NG Hub lists channels and groups this account created, and keeps everyone
 * else's communities in a separate section. Nothing is invented.
 */
public class NGHubActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private static final int SECTION_CHANNELS = 0;
    private static final int SECTION_GROUPS = 1;
    private static final int SECTION_BOTS = 2;
    private static final int SECTION_COMMUNITIES = 3;
    private static final int COLOR_CYAN = 0xFF0284C7;
    private static final int COLOR_TEAL = 0xFF0F766E;
    private static final int COLOR_GREEN = 0xFF16A34A;
    private static final int COLOR_BLUE = 0xFF0369A1;

    private BackupImageView accountAvatar;
    private TextView accountNameView;
    private TextView accountSubView;
    private LinearLayout contentColumn;
    private LinearLayout activeRows;
    private TextView[] tabs;
    private TextView swipeToggle;
    private TextView layoutToggle;
    private TextView joinedModeButton;
    private TextView mineModeButton;
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
    private boolean initialRenderDone = false;
    private boolean plusOpen;
    private boolean closing;
    private float touchStartX;
    private float touchStartY;
    private int observedAccount = -1;
    private final SparseIntArray dialogLoadRounds = new SparseIntArray();
    private final SparseBooleanArray allChatsLoadedForAccount = new SparseBooleanArray();
    private final SparseBooleanArray allChatsInFlightForAccount = new SparseBooleanArray();
    private final SparseBooleanArray adminedChatsLoadedForAccount = new SparseBooleanArray();
    private final SparseBooleanArray adminedChatsInFlightForAccount = new SparseBooleanArray();
    private final HashMap<Integer, HashMap<Long, TLRPC.Chat>> extraChatsByAccount = new HashMap<>();

    public NGHubActivity() {
        super();
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        registerObservers(currentAccount);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        unregisterObservers();
        super.onFragmentDestroy();
    }

    private void registerObservers(int account) {
        if (observedAccount == account) return;
        unregisterObservers();
        observedAccount = account;
        NotificationCenter nc = NotificationCenter.getInstance(observedAccount);
        nc.addObserver(this, NotificationCenter.dialogsNeedReload);
        nc.addObserver(this, NotificationCenter.updateInterfaces);
    }

    private void unregisterObservers() {
        if (observedAccount < 0) return;
        NotificationCenter nc = NotificationCenter.getInstance(observedAccount);
        nc.removeObserver(this, NotificationCenter.dialogsNeedReload);
        nc.removeObserver(this, NotificationCenter.updateInterfaces);
        observedAccount = -1;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (account != currentAccount) return;
        if (id == NotificationCenter.dialogsNeedReload) {
            showSection(selectedSection);
        } else if (id == NotificationCenter.updateInterfaces) {
            updateAccountCard();
        }
    }

    private void ensureAllSpacesLoaded(boolean resetRounds) {
        final int targetAccount = currentAccount;
        MessagesController controller = MessagesController.getInstance(targetAccount);
        if (resetRounds) {
            dialogLoadRounds.put(targetAccount, 0);
        }
        int rounds = dialogLoadRounds.get(targetAccount, 0);
        if (controller.getDialogs(0).isEmpty() && !controller.isLoadingDialogs(0)) {
            controller.loadDialogs(0, 0, 100, true);
        }
        if (rounds < 2) {
            boolean requestedPage = false;
            if (!controller.isDialogsEndReached(0) && !controller.isLoadingDialogs(0)) {
                controller.loadDialogs(0, -1, 100, false);
                requestedPage = true;
            }
            if (!controller.isDialogsEndReached(1) && !controller.isLoadingDialogs(1)) {
                controller.loadDialogs(1, -1, 100, false);
                requestedPage = true;
            }
            if (requestedPage) {
                dialogLoadRounds.put(targetAccount, rounds + 1);
            }
        }
        if (!allChatsLoadedForAccount.get(targetAccount) && !allChatsInFlightForAccount.get(targetAccount)) {
            allChatsInFlightForAccount.put(targetAccount, true);
            TLRPC.TL_messages_getAllChats req = new TLRPC.TL_messages_getAllChats();
            req.except_ids = new ArrayList<>();
            ConnectionsManager.getInstance(targetAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                allChatsInFlightForAccount.put(targetAccount, false);
                if (response instanceof TLRPC.messages_Chats) {
                    TLRPC.messages_Chats res = (TLRPC.messages_Chats) response;
                    allChatsLoadedForAccount.put(targetAccount, true);
                    if (res.chats != null && !res.chats.isEmpty()) {
                        MessagesController.getInstance(targetAccount).putChats(res.chats, false);
                        HashMap<Long, TLRPC.Chat> map = extraChatsByAccount.get(targetAccount);
                        if (map == null) {
                            map = new HashMap<>();
                            extraChatsByAccount.put(targetAccount, map);
                        }
                        for (TLRPC.Chat c : res.chats) {
                            if (c != null && !ChatObject.isNotInChat(c) && !c.min) {
                                map.put(c.id, c);
                            }
                        }
                    }
                    if (targetAccount == currentAccount && contentColumn != null) {
                        showSection(selectedSection);
                    }
                }
            }));
        }
        if (!adminedChatsLoadedForAccount.get(targetAccount) && !adminedChatsInFlightForAccount.get(targetAccount)) {
            adminedChatsInFlightForAccount.put(targetAccount, true);
            TLRPC.TL_channels_getAdminedPublicChannels req = new TLRPC.TL_channels_getAdminedPublicChannels();
            req.for_community_peer = false;
            ConnectionsManager.getInstance(targetAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                adminedChatsInFlightForAccount.put(targetAccount, false);
                if (response instanceof TLRPC.messages_Chats) {
                    TLRPC.messages_Chats res = (TLRPC.messages_Chats) response;
                    adminedChatsLoadedForAccount.put(targetAccount, true);
                    if (res.chats != null && !res.chats.isEmpty()) {
                        MessagesController.getInstance(targetAccount).putChats(res.chats, false);
                        HashMap<Long, TLRPC.Chat> map = extraChatsByAccount.get(targetAccount);
                        if (map == null) {
                            map = new HashMap<>();
                            extraChatsByAccount.put(targetAccount, map);
                        }
                        for (TLRPC.Chat c : res.chats) {
                            if (c != null && c.creator && !ChatObject.isNotInChat(c)) {
                                map.put(c.id, c);
                            }
                        }
                    }
                    if (targetAccount == currentAccount && contentColumn != null) {
                        showSection(selectedSection);
                    }
                }
            }));
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("NG Hub");
        actionBar.setAllowOverlayTitle(true);
        org.telegram.ui.ActionBar.ActionBarMenu menu = actionBar.createMenu();
        org.telegram.ui.ActionBar.ActionBarMenuItem cornerItem = menu.addItem(9920, R.drawable.msg_channel);
        cornerItem.setContentDescription("NG Hub Details");
        org.telegram.ui.Components.RLottieImageView cornerIcon = cornerItem.getIconView();
        if (cornerIcon != null) {
            android.graphics.drawable.GradientDrawable cornerBg = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFF00D4FF, 0xFF0284C7} // Vibrant Cyan Gradient
            );
            cornerBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            cornerIcon.setBackground(cornerBg);
            cornerIcon.setColorFilter(new android.graphics.PorterDuffColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN));
            android.widget.FrameLayout.LayoutParams lp = LayoutHelper.createFrame(28, 28, Gravity.CENTER);
            cornerIcon.setLayoutParams(lp);
            int pad = AndroidUtilities.dp(5);
            cornerIcon.setPadding(pad, pad, pad, pad);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    closeAnimated();
                } else if (id == 9920) {
                    openDetails();
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
        page.addView(buildAccountSwitcherCard(context));
        page.addView(buildShortcuts(context));
        page.addView(buildCreateRow(context));

        LinearLayout modeRow = new LinearLayout(context);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        joinedModeButton = createAction(context, "🌐 Joined (Other's)");
        mineModeButton = createAction(context, "👑 My Created (Owned)");
        joinedModeButton.setOnClickListener(v -> {
            if (mineOnly) {
                mineOnly = false;
                updateModeButtons();
                showSection(selectedSection);
            }
        });
        mineModeButton.setOnClickListener(v -> {
            if (!mineOnly) {
                mineOnly = true;
                updateModeButtons();
                showSection(selectedSection);
            }
        });
        modeRow.addView(joinedModeButton, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        LinearLayout.LayoutParams mineLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        mineLp.leftMargin = AndroidUtilities.dp(8);
        modeRow.addView(mineModeButton, mineLp);
        page.addView(modeRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 42, 0, 4, 0, 8));
        updateModeButtons();

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
        String[] labels = {"My Channels", "My Groups", "My Bots", "My Communities"};
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
        footer.setText("NG Hub (16 October 2026) · Channels and groups you created appear under My Created; Bot creation opens official BotFather.");
        footer.setTextSize(12.5f);
        footer.setLineSpacing(AndroidUtilities.dp(2), 1.25f);
        boolean footerDark = Theme.isCurrentThemeDark();
        footer.setTextColor(footerDark ? 0xFF94A3B8 : 0xFF64748B);
        footer.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        GradientDrawable footerBg = new GradientDrawable();
        footerBg.setCornerRadius(AndroidUtilities.dp(14));
        footerBg.setColor(footerDark ? 0x161E293B : 0x0A0284C7);
        footerBg.setStroke(AndroidUtilities.dp(1), footerDark ? 0x2238BDF8 : 0x140284C7);
        footer.setBackground(footerBg);
        page.addView(footer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 14, 0, 6));

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
        initialRenderDone = false;
        TextView loadingHint = new TextView(context);
        loadingHint.setText("Loading your channels & groups...");
        loadingHint.setTextSize(14);
        loadingHint.setGravity(Gravity.CENTER);
        loadingHint.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20));
        loadingHint.setTextColor(Theme.isCurrentThemeDark() ? 0xFFCBD5E1 : 0xFF64748B);
        contentColumn.addView(loadingHint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        root.post(() -> {
            if (!closing && getParentActivity() != null && contentColumn != null) {
                initialRenderDone = true;
                showSection(selectedSection);
            }
        });
        root.postDelayed(() -> {
            if (!closing && getParentActivity() != null) {
                ensureAllSpacesLoaded(true);
            }
        }, 250);
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (observedAccount != currentAccount) {
            registerObservers(currentAccount);
        }
        updateAccountCard();
        if (initialRenderDone) {
            showSection(selectedSection);
        }
    }

    private View buildHero(Context context) {
        boolean isDark = Theme.isCurrentThemeDark();
        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18));
        GradientDrawable heroBg = new GradientDrawable();
        heroBg.setCornerRadius(AndroidUtilities.dp(20));
        heroBg.setColor(isDark ? 0xFF1E293B : Theme.getColor(Theme.key_windowBackgroundWhite));
        heroBg.setStroke(AndroidUtilities.dp(1), isDark ? 0x2638BDF8 : 0x140284C7);
        hero.setBackground(heroBg);
        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.nayagram_intro_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("NayaGram logo");
        hero.addView(logo, LayoutHelper.createLinear(84, 84, Gravity.CENTER_HORIZONTAL));
        TextView title = new TextView(context);
        title.setText("NG Hub");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(COLOR_CYAN);
        title.setShadowLayer(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(2), 0x3038BDF8);
        title.setGravity(Gravity.CENTER);
        hero.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));
        TextView subtitle = new TextView(context);
        subtitle.setText("Manage your channels, groups, bots & communities");
        subtitle.setTextSize(13.5f);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextColor(isDark ? 0xFF94A3B8 : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        hero.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 12));
        TextView details = coloredButton(context, "Details", COLOR_CYAN);
        details.setOnClickListener(v -> openDetails());
        hero.addView(details, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 40, Gravity.CENTER_HORIZONTAL));
        LinearLayout.LayoutParams params = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12);
        hero.setLayoutParams(params);
        return hero;
    }

    private void updateAccountCard() {
        if (accountAvatar == null || accountNameView == null || accountSubView == null) return;
        TLRPC.User currentUser = UserConfig.getInstance(currentAccount).getCurrentUser();
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        avatarDrawable.setInfo(currentAccount, currentUser);
        accountAvatar.getImageReceiver().setCurrentAccount(currentAccount);
        accountAvatar.setForUserOrChat(currentUser, avatarDrawable);

        accountNameView.setText(currentUser != null ? UserObject.getUserName(currentUser) : "Active Account");
        int totalAccounts = org.nayagram.platform.security.NGHiddenAccounts.listedCount();
        String subText = (currentUser != null && currentUser.username != null && !currentUser.username.isEmpty())
                ? "@" + currentUser.username + " • " + totalAccounts + (totalAccounts > 1 ? " accounts active" : " account active")
                : totalAccounts + (totalAccounts > 1 ? " accounts active" : " account active");
        accountSubView.setText(subText);
    }

    private View buildAccountSwitcherCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        boolean isDark = Theme.isCurrentThemeDark();
        int cardBg = isDark ? 0xFF1E293B : Theme.getColor(Theme.key_windowBackgroundWhite);
        GradientDrawable cardDrawable = new GradientDrawable();
        cardDrawable.setColor(cardBg);
        cardDrawable.setCornerRadius(AndroidUtilities.dp(18));
        cardDrawable.setStroke(AndroidUtilities.dp(1), isDark ? 0x2E38BDF8 : 0x180284C7);
        card.setBackground(cardDrawable);

        TLRPC.User currentUser = UserConfig.getInstance(currentAccount).getCurrentUser();
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        avatarDrawable.setInfo(currentAccount, currentUser);
        accountAvatar = new BackupImageView(context);
        accountAvatar.setRoundRadius(AndroidUtilities.dp(16));
        accountAvatar.getImageReceiver().setCurrentAccount(currentAccount);
        accountAvatar.setForUserOrChat(currentUser, avatarDrawable);
        card.addView(accountAvatar, LayoutHelper.createLinear(40, 40, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout infoCol = new LinearLayout(context);
        infoCol.setOrientation(LinearLayout.VERTICAL);

        LinearLayout nameRow = new LinearLayout(context);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);

        accountNameView = new TextView(context);
        accountNameView.setText(currentUser != null ? UserObject.getUserName(currentUser) : "Active Account");
        accountNameView.setTextSize(15);
        accountNameView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        accountNameView.setTextColor(isDark ? 0xFFF1F5F9 : Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        nameRow.addView(accountNameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView activePill = new TextView(context);
        activePill.setText("Active");
        activePill.setTextSize(10);
        activePill.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        activePill.setTextColor(0xFF0284C7);
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(0x1F0284C7);
        pillBg.setCornerRadius(AndroidUtilities.dp(8));
        activePill.setBackground(pillBg);
        activePill.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(2), AndroidUtilities.dp(6), AndroidUtilities.dp(2));
        nameRow.addView(activePill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 6, 0, 0, 0));

        infoCol.addView(nameRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        accountSubView = new TextView(context);
        int totalAccounts = org.nayagram.platform.security.NGHiddenAccounts.listedCount();
        String subText = (currentUser != null && currentUser.username != null && !currentUser.username.isEmpty())
                ? "@" + currentUser.username + " • " + totalAccounts + (totalAccounts > 1 ? " accounts active" : " account active")
                : totalAccounts + (totalAccounts > 1 ? " accounts active" : " account active");
        accountSubView.setText(subText);
        accountSubView.setTextSize(12);
        accountSubView.setTextColor(isDark ? 0xFF94A3B8 : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        infoCol.addView(accountSubView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        card.addView(infoCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        TextView switchBtn = new TextView(context);
        switchBtn.setText("Switch 👥 ⌵");
        switchBtn.setTextSize(12);
        switchBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        switchBtn.setTextColor(Color.WHITE);
        switchBtn.setGravity(Gravity.CENTER);
        GradientDrawable btnBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF0284C7, 0xFF0369A1});
        btnBg.setCornerRadius(AndroidUtilities.dp(14));
        switchBtn.setBackground(btnBg);
        switchBtn.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        card.addView(switchBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

        card.setOnClickListener(v -> showAccountSwitchDialog(context));
        switchBtn.setOnClickListener(v -> showAccountSwitchDialog(context));

        LinearLayout.LayoutParams params = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12);
        card.setLayoutParams(params);
        return card;
    }

    private void showAccountSwitchDialog(Context context) {
        presentFragment(new org.nayagram.platform.ui.NGAccountSwitcherActivity());
        if (true) return;
        if (context == null || getParentActivity() == null) return;

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(14));

        int activatedCount = UserConfig.getActivatedAccountsCount();
        TextView header = new TextView(context);
        header.setText("👥 Switch Account (" + activatedCount + "/" + UserConfig.MAX_ACCOUNT_COUNT + ")");
        header.setTextSize(17);
        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        layout.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        ScrollView scroll = new ScrollView(context);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final AlertDialog[] dialogHolder = new AlertDialog[1];

        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!org.nayagram.platform.security.NGHiddenAccounts.isListed(a)) continue;
            final int accountIndex = a;
            TLRPC.User user = UserConfig.getInstance(a).getCurrentUser();

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
            boolean isActive = (accountIndex == currentAccount);
            row.setBackground(round(isActive ? (Theme.isCurrentThemeDark() ? 0xFF1E3A5F : 0xFFE0F2FE) : (Theme.isCurrentThemeDark() ? 0xFF1E293B : 0xFFF8FAFC), 14));

            AvatarDrawable ad = new AvatarDrawable();
            ad.setInfo(accountIndex, user);
            BackupImageView av = new BackupImageView(context);
            av.setRoundRadius(AndroidUtilities.dp(14));
            av.getImageReceiver().setCurrentAccount(accountIndex);
            av.setForUserOrChat(user, ad);
            row.addView(av, LayoutHelper.createLinear(36, 36, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);
            TextView uName = new TextView(context);
            uName.setText(UserObject.getUserName(user));
            uName.setTextSize(14);
            uName.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            uName.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            textCol.addView(uName);

            TextView uStatus = new TextView(context);
            uStatus.setText(isActive ? "Active Account" : (user != null && user.username != null && !user.username.isEmpty() ? "@" + user.username : "Account #" + (accountIndex + 1)));
            uStatus.setTextSize(11);
            uStatus.setTextColor(isActive ? 0xFF0284C7 : Theme.getColor(Theme.key_dialogTextGray2));
            textCol.addView(uStatus);

            row.addView(textCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

            if (isActive) {
                TextView checkBadge = new TextView(context);
                checkBadge.setText("✓");
                checkBadge.setTextSize(14);
                checkBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                checkBadge.setTextColor(Color.WHITE);
                checkBadge.setGravity(Gravity.CENTER);
                GradientDrawable checkBg = new GradientDrawable();
                checkBg.setColor(0xFF0284C7);
                checkBg.setCornerRadius(AndroidUtilities.dp(12));
                checkBadge.setBackground(checkBg);
                row.addView(checkBadge, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL));
            }

            row.setOnClickListener(v -> {
                if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                if (accountIndex != currentAccount && LaunchActivity.instance != null) {
                    LaunchActivity.instance.switchToAccount(accountIndex, true);
                    updateAccountCard();
                }
            });

            LinearLayout.LayoutParams lp = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6);
            list.addView(row, lp);
        }

        int containerHeight = activatedCount > 5 ? AndroidUtilities.dp(240) : ViewGroup.LayoutParams.WRAP_CONTENT;
        layout.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, containerHeight, 0, 0, 0, 10));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(layout);
        builder.setPositiveButton("Add Account", (d, w) -> showAddAccountDialog(context));
        builder.setNegativeButton("Close", (d, w) -> d.dismiss());
        dialogHolder[0] = builder.create();
        dialogHolder[0].show();
    }

    private void showAddAccountDialog(Context context) {
        int freeAccount = -1;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!UserConfig.getInstance(a).isClientActivated()) {
                freeAccount = a;
                break;
            }
        }
        if (freeAccount >= 0) {
            presentFragment(new LoginActivity(freeAccount));
        } else {
            android.widget.Toast.makeText(context, "Maximum 15 accounts limit reached", android.widget.Toast.LENGTH_SHORT).show();
        }
        if (true) return;

        boolean isBn = false;
        try {
            if (LocaleController.getInstance() != null && LocaleController.getInstance().getCurrentLocale() != null) {
                isBn = "bn".equalsIgnoreCase(LocaleController.getInstance().getCurrentLocale().getLanguage());
            }
        } catch (Throwable ignored) {}

        final int targetAccount = freeAccount;
        LinearLayout addLayout = new LinearLayout(context);
        addLayout.setOrientation(LinearLayout.VERTICAL);
        addLayout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16));

        TextView addHeader = new TextView(context);
        addHeader.setText(isBn ? "➕ একাউন্ট যোগ করুন" : "➕ Add Account");
        addHeader.setTextSize(17);
        addHeader.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        addHeader.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        addLayout.addView(addHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        final AlertDialog[] addDialogHolder = new AlertDialog[1];

        // Card 1: Phone Login
        LinearLayout phoneCard = new LinearLayout(context);
        phoneCard.setOrientation(LinearLayout.VERTICAL);
        phoneCard.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        boolean dark = Theme.isCurrentThemeDark();
        GradientDrawable phoneBg = new GradientDrawable();
        phoneBg.setColor(dark ? 0xFF1E293B : 0xFFE0F2FE);
        phoneBg.setCornerRadius(AndroidUtilities.dp(14));
        phoneBg.setStroke(AndroidUtilities.dp(1), 0xFF0284C7);
        phoneCard.setBackground(phoneBg);

        TextView phoneTitle = new TextView(context);
        phoneTitle.setText(isBn ? "📱 ফোন নম্বর দিয়ে লগইন" : "📱 Phone Number Login");
        phoneTitle.setTextSize(14);
        phoneTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        phoneTitle.setTextColor(0xFF0284C7);
        phoneCard.addView(phoneTitle);

        TextView phoneSub = new TextView(context);
        phoneSub.setText(isBn ? "টেলিগ্রামের এসএমএস কোড বা ফোন ভেরিফিকেশন" : "Standard Telegram SMS code / phone verification");
        phoneSub.setTextSize(12);
        phoneSub.setTextColor(dark ? 0xFF94A3B8 : 0xFF475569);
        phoneCard.addView(phoneSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        phoneCard.setOnClickListener(v -> {
            if (addDialogHolder[0] != null) addDialogHolder[0].dismiss();
            presentFragment(new LoginActivity(targetAccount));
        });

        addLayout.addView(phoneCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Card 2: Bot Token Login
        LinearLayout botCard = new LinearLayout(context);
        botCard.setOrientation(LinearLayout.VERTICAL);
        botCard.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        GradientDrawable botBg = new GradientDrawable();
        botBg.setColor(dark ? 0xFF1E293B : 0xFFF3E8FF);
        botBg.setCornerRadius(AndroidUtilities.dp(14));
        botBg.setStroke(AndroidUtilities.dp(1), 0xFF9333EA);
        botCard.setBackground(botBg);

        TextView botTitle = new TextView(context);
        botTitle.setText(isBn ? "🤖 বট টোকেন দিয়ে লগইন" : "🤖 Bot Token Login");
        botTitle.setTextSize(14);
        botTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        botTitle.setTextColor(0xFF9333EA);
        botCard.addView(botTitle);

        TextView botSub = new TextView(context);
        botSub.setText(isBn ? "বটফাদার এপিআই টোকেন দিয়ে ইনস্ট্যান্ট লগইন" : "Instant BotFather API token login into NayaGram");
        botSub.setTextSize(12);
        botSub.setTextColor(dark ? 0xFF94A3B8 : 0xFF475569);
        botCard.addView(botSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        botCard.setOnClickListener(v -> {
            if (addDialogHolder[0] != null) addDialogHolder[0].dismiss();
            org.nayagram.platform.auth.NGBotLoginHelper.showBotLoginDialog(NGHubActivity.this, targetAccount);
        });

        addLayout.addView(botCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        AlertDialog.Builder addBuilder = new AlertDialog.Builder(getParentActivity());
        addBuilder.setView(addLayout);
        addBuilder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        addDialogHolder[0] = addBuilder.create();
        addDialogHolder[0].show();
    }

    private View buildShortcuts(Context context) {
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row1 = new LinearLayout(context);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        TextView myChannel = coloredButton(context, "📣  My Channels", COLOR_CYAN);
        TextView myGroup = coloredButton(context, "👥  My Groups", COLOR_TEAL);
        row1.addView(myChannel, weight());
        LinearLayout.LayoutParams gap1 = weight();
        gap1.leftMargin = AndroidUtilities.dp(8);
        row1.addView(myGroup, gap1);
        grid.addView(row1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 46));

        LinearLayout rowJoined = new LinearLayout(context);
        rowJoined.setOrientation(LinearLayout.HORIZONTAL);
        TextView joinedChannels = coloredButton(context, "📺  Joined Channels", 0xFF0369A1);
        TextView joinedGroups = coloredButton(context, "💬  Joined Groups", 0xFF0D9488);
        rowJoined.addView(joinedChannels, weight());
        LinearLayout.LayoutParams gapJoined = weight();
        gapJoined.leftMargin = AndroidUtilities.dp(8);
        rowJoined.addView(joinedGroups, gapJoined);
        grid.addView(rowJoined, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 46, 0, 8, 0, 0));

        LinearLayout row2 = new LinearLayout(context);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        TextView bot = coloredButton(context, "🤖  My Bots", COLOR_GREEN);
        TextView profile = coloredButton(context, "👤  My Profile", COLOR_BLUE);
        row2.addView(bot, weight());
        LinearLayout.LayoutParams gap2 = weight();
        gap2.leftMargin = AndroidUtilities.dp(8);
        row2.addView(profile, gap2);
        grid.addView(row2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 46, 0, 8, 0, 0));

        LinearLayout row3 = new LinearLayout(context);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        TextView contacts = coloredButton(context, "📇  My Contacts", COLOR_TEAL);
        TextView liveStream = coloredButton(context, "📡  Live Stream", 0xFF0D9488);
        row3.addView(contacts, weight());
        LinearLayout.LayoutParams gap3 = weight();
        gap3.leftMargin = AndroidUtilities.dp(8);
        row3.addView(liveStream, gap3);
        grid.addView(row3, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 46, 0, 8, 0, 0));

        myChannel.setOnClickListener(v -> selectMine(SECTION_CHANNELS));
        myGroup.setOnClickListener(v -> selectMine(SECTION_GROUPS));
        joinedChannels.setOnClickListener(v -> selectJoined(SECTION_CHANNELS));
        joinedGroups.setOnClickListener(v -> selectJoined(SECTION_GROUPS));
        bot.setOnClickListener(v -> selectMine(SECTION_BOTS));
        profile.setOnClickListener(v -> openProfile());
        contacts.setOnClickListener(v -> presentFragment(new ContactsActivity(null)));
        liveStream.setOnClickListener(v -> startLiveStream());
        return grid;
    }

    private View buildCreateRow(Context context) {
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView createGroup = createAction(context, "＋ Group");
        TextView createChannel = createAction(context, "＋ Channel");
        TextView createBot = createAction(context, "＋ Bot");
        createGroup.setSingleLine(true);
        createChannel.setSingleLine(true);
        createBot.setSingleLine(true);
        actions.addView(createGroup, weight());
        LinearLayout.LayoutParams channelParams = weight();
        channelParams.leftMargin = AndroidUtilities.dp(8);
        actions.addView(createChannel, channelParams);
        LinearLayout.LayoutParams botParams = weight();
        botParams.leftMargin = AndroidUtilities.dp(8);
        actions.addView(createBot, botParams);
        createGroup.setOnClickListener(v -> presentFragment(new GroupCreateActivity(new Bundle())));
        createChannel.setOnClickListener(v -> presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_CHANNEL_CREATE)));
        createBot.setOnClickListener(v -> openBotFather());
        LinearLayout.LayoutParams params = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 42, 0, 10, 0, 8);
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

    private void updateModeButtons() {
        boolean isDark = Theme.isCurrentThemeDark();
        if (joinedModeButton != null) {
            joinedModeButton.setBackground(round(!mineOnly ? COLOR_CYAN : Theme.getColor(Theme.key_windowBackgroundWhite), 12));
            joinedModeButton.setTextColor(!mineOnly ? Color.WHITE : (isDark ? 0xFFCBD5E1 : 0xFF475569));
        }
        if (mineModeButton != null) {
            mineModeButton.setBackground(round(mineOnly ? COLOR_TEAL : Theme.getColor(Theme.key_windowBackgroundWhite), 12));
            mineModeButton.setTextColor(mineOnly ? Color.WHITE : (isDark ? 0xFFCBD5E1 : 0xFF475569));
        }
    }

    private void selectMine(int section) {
        mineOnly = true;
        updateModeButtons();
        showSection(section);
    }

    private void selectJoined(int section) {
        mineOnly = false;
        updateModeButtons();
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

        LinearLayout topBar = new LinearLayout(context);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView back = coloredButton(context, "←  Back to NG Hub", COLOR_BLUE);
        back.setOnClickListener(v -> closeDetails());
        topBar.addView(back, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 40, 0, 0, 0, 0));

        View spacer = new View(context);
        topBar.addView(spacer, LayoutHelper.createLinear(0, 1, 1f));

        TextView topLangBtn = new TextView(context);
        topLangBtn.setText("🌐 ভাষা / Lang");
        topLangBtn.setTextSize(13);
        topLangBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        topLangBtn.setTextColor(Color.WHITE);
        topLangBtn.setGravity(Gravity.CENTER);
        GradientDrawable topLangBg = new GradientDrawable();
        topLangBg.setColor(COLOR_CYAN);
        topLangBg.setCornerRadius(AndroidUtilities.dp(16));
        topLangBtn.setBackground(topLangBg);
        topLangBtn.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));
        topBar.addView(topLangBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 36, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

        column.addView(topBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        TextView langLabel = new TextView(context);
        langLabel.setText("🌐 Select Language / ভাষা পরিবর্তন:");
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
        column.addView(languages, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, 0, 0, 0, 14));

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

        topLangBtn.setOnClickListener(v -> {
            int next = (detailLang + 1) % names.length;
            applyDetails(next);
        });

        LinearLayout heroCard = new LinearLayout(context);
        heroCard.setOrientation(LinearLayout.VERTICAL);
        heroCard.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18));
        GradientDrawable head = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF0284C7, 0xFF0369A1});
        head.setCornerRadius(AndroidUtilities.dp(22));
        heroCard.setBackground(head);

        TextView versionBadge = new TextView(context);
        versionBadge.setText("🚀 Version " + BuildVars.BUILD_VERSION_STRING + " (" + BuildVars.BUILD_VERSION + ") • Official Release");
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

        detailUpdatesHeader = detailText(context, 16, COLOR_TEAL, true);
        detailUpdates = detailText(context, 14, Theme.isCurrentThemeDark() ? 0xFFE2E8F0 : 0xFF334155, false);
        column.addView(buildSectionCard(context, detailUpdatesHeader, detailUpdates), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

// Language selector moved to top header
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
                        "এটি যা করে", "এই অ্যাকাউন্টের তৈরি চ্যানেল ও গ্রুপ দেখায়, প্রোফাইল ও কন্টাক্ট লিস্ট খোলে, এবং বটের জন্য অফিসিয়াল বটফাদার খোলে।",
                        "🛡️ এটি যা করে না", "মিথ্যা চ্যানেল বা গ্রুপ বানায় না, এবং অন্য কারও কমিউনিটিকে আপনার বলে না।",
                        "🚀 সর্বশেষ আপডেট ও নতুন সুবিধাসমূহ", "• এনজি হাব থেকে সরাসরি দ্রুত অ্যাকাউন্ট সুইচ ও ১৫টি অ্যাকাউন্ট সাপোর্ট।\n• অ্যাড অ্যাকাউন্ট ও লগইন স্ক্রিন থেকে সরাসরি বট টোকেন দিয়ে লগইন।\n• চ্যাটে স্থায়ী টেক্সট স্টাইলার (বোল্ড, ইটালিক, স্পয়লার, মনো, কোট)।\n• ডিটেইলস ও টিউটোরিয়ালের উপরে এক কোণে দ্রুত ভাষা পরিবর্তনের বাটন।\n• নোভাগ্রাম স্টাইল ফোল্ডার কার্ডস ও লাইভ স্ট্রিমিং শর্টকাট।"
                };
            case 2:
                return new String[]{
                        "ar", "العربية", "إن جي هب",
                        "إن جي هب هو بيتك الشخصي للقنوات والمجموعات والبوتات التي تنشئها، مع ملفك وقائمة جهات الاتصال.",
                        "🎯 الغاية", "أن تبقى أماكنك في شاشة واحدة، وألا تُعرض مجتمعات الآخرين كأنها ملكك.",
                        "ماذا يفعل", "يعرض ما أنشأه هذا الحساب، ويفتح ملفك وجهات الاتصال، ويفتح بوت فادر الرسمي.",
                        "🛡️ ما لا يفعله", "لا يخترع قنوات أو مجموعات، ولا يضع اسمك على مجتمع شخص آخر.",
                        "🚀 آخر التحديثات والتحسينات", "• حركة انزلاقية للبطاقات من اليمين إلى اليسار مع حدود علوية وسفلية أنيقة.\n• تباين نصوص فائق الوضوح في الوضعين الداكن والفاتح.\n• اختصار مباشر لبدء البث المباشر من إن جي هب.\n• بطاقات مجلدات مستوحاة من نوفاغرام مع أيقونات ديناميكية."
                };
            case 3:
                return new String[]{
                        "ru", "Русский", "NG Hub",
                        "NG Hub — личный экран для каналов, групп и ботов, которые создали вы, а также профиля и контактов.",
                        "🎯 Задача", "Держать свои места на одном экране и не показывать чужие сообщества как ваши.",
                        "Что делает", "Показывает созданное этим аккаунтом, открывает профиль и контакты и официального BotFather.",
                        "🛡️ Чего не делает", "Не придумывает каналы или группы и не выдаёт чужое сообщество за ваше.",
                        "🚀 Новые улучшения", "• Плавная анимация карточек справа налево с верхней и нижней границей.\n• Высококонтрастный четкий текст в темной и светлой темах.\n• Быстрый запуск прямого эфира прямо из NG Hub.\n• Карточки папок в стиле Novagram с векторными иконками."
                };
            case 4:
                return new String[]{
                        "uz", "Oʻzbekcha", "NG Hub",
                        "NG Hub — o‘zingiz yaratgan kanal, guruh va botlar, hamda profil va kontaktlar uchun shaxsiy joy.",
                        "🎯 Maqsad", "O‘z joylaringizni bitta ekranda saqlash va begona hamjamiyatni sizniki deb ko‘rsatmaslik.",
                        "Nima qiladi", "Bu hisob yaratgan joylarni ko‘rsatadi, profil va kontaktlarni ochadi, bot uchun rasmiy BotFather’ni ochadi.",
                        "🛡️ Nima qilmaydi", "Soxta kanal yoki guruh o‘ylab topmaydi va birovning hamjamiyatini sizniki deb belgilamaydi.",
                        "🚀 So‘nggi yangilanishlar", "• O‘ngdan chapga kartalar silliq animatsiyasi va chiroyli chegaralar.\n• Qorong‘i va yorug‘ rejimda yuqori kontrastli aniq matnlar.\n• NG Hub ichidan to‘g‘ridan-to‘g‘ri jonli efir tugmasi.\n• Novagram uslubidagi jild kartalari va vektor ikonkalari."
                };
            case 5:
                return new String[]{
                        "hi", "हिन्दी", "एनजी हब",
                        "एनजी हब उन चैनल, ग्रुप और बॉट का निजी घर है जिन्हें आपने बनाया है, साथ में प्रोफ़ाइल और संपर्क सूची।",
                        "🎯 उद्देश्य", "अपनी जगहें एक स्क्रीन पर रखना, এবং किसी और की कम्युनिटी को अपनी बताकर न दिखाना।",
                        "यह क्या करता है", "इस खाते के बनाए चैनल और ग्रुप दिखाता है, प्रोफ़ाइल और संपर्क खोलता है, और आधिकारिक बॉटफादर खोलता है।",
                        "🛡️ यह नहीं करता", "नकली चैनल या ग्रुप नहीं गढ़ता, और किसी दूसरे की कम्युनिटी पर आपका नाम नहीं लगाता।",
                        "🚀 नवीनतम अपडेट और सुधार", "• दाएँ से बाएँ कार्ड स्लाइड एनीमेशन और आकर्षक टॉप-बॉटम बॉर्डर।\n• डार्क और लाइट मोड में स्पष्ट और चमकदार टेक्स्ट कंट्रास्ट।\n• NG Hub से सीधे एक-टैप लाइव स्ट्रीमिंग।\n• नोवाग्राम स्टाइल फोल्डर कार्ड्स और ऑटो-ऐड सपोर्ट।"
                };
            case 6:
                return new String[]{
                        "ur", "اردو", "این جی ہب",
                        "این جی ہب ان چینلز، گروپس اور بوٹس کا ذاتی گھر ہے جو آپ نے بنائے، ساتھ پروفائل اور رابطہ فہرست।",
                        "🎯 مقصد", "اپنی جگہوں کو ایک اسکرین پر رکھنا, اور کسی اور کی کمیونٹی کو اپنی بنا کر نہیں دکھانا।",
                        "یہ کیا کرتا ہے", "اس اکاؤنٹ کے بنائے چینل اور گروپ دکھاتا ہے، پروفائل اور رابطے کھولتا ہے، اور سرکاری بوٹ فادر کھولتا ہے۔",
                        "🛡️ یہ نہیں کرتا", "جعلی چینل یا گروپ نہیں بناتا، اور کسی دوسرے کی کمیونٹی کو آپ کی نہیں لکھتا।",
                        "🚀 تازہ ترین اپڈیٹس اور سہولیات", "• دائیں سے بائیں کارڈز کی خوبصورت سلائیڈ اینیمیشن اور بارڈرز۔\n• ڈارک اور لائٹ موڈ میں واضح اور روشن ٹیکسٹ کنٹراسٹ।\n• این جی ہب سے براہ راست ون-ٹیپ لائیو اسٹریمنگ۔\n• نوواگرام اسٹائل فولڈر کارڈز اور آٹو-ایڈ سپورٹ।"
                };
            default:
                return new String[]{
                        "en", "English", "NG Hub",
                        "NG Hub is your personal home for the channels, groups and bots you create, together with your profile and contact list.",
                        "🎯 Purpose", "Keep your own Telegram places on one screen, and never present someone else’s community as yours.",
                        "What it does", "It lists channels and groups this account created, opens your profile and contact list, and opens the official BotFather to create a bot.",
                        "🛡️ What it never does", "It does not invent channels or groups, and it does not label another person’s community as yours.",
                        "🚀 Latest Enhancements", "• Instant Account Switcher inside NG Hub supporting up to 15 accounts.\n• Direct Bot Token login from Add Account & Login screens.\n• In-Chat Persistent Text Styler (Bold, Italic, Spoiler, Mono, Quote).\n• Quick top-corner Language Switcher in Details & Tutorials.\n• Novagram-style folder cards & Live Streaming shortcut."
                };
        }
    }

    private void showSection(int section) {
        selectedSection = Math.max(SECTION_CHANNELS, Math.min(SECTION_COMMUNITIES, section));
        if (contentColumn == null || getContext() == null) return;
        updateModeButtons();
        updateTabStyles();
        contentColumn.removeAllViews();

        boolean noteDark = Theme.isCurrentThemeDark();
        TextView note = new TextView(getContext());
        note.setTextSize(12.5f);
        note.setTextColor(noteDark ? 0xFF94A3B8 : 0xFF475569);
        note.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(7), AndroidUtilities.dp(12), AndroidUtilities.dp(7));
        GradientDrawable noteBg = new GradientDrawable();
        noteBg.setCornerRadius(AndroidUtilities.dp(10));
        noteBg.setColor(noteDark ? 0x1838BDF8 : 0x0D0284C7);
        note.setBackground(noteBg);
        if (mineOnly) {
            if (selectedSection == SECTION_CHANNELS) {
                note.setText("Channels created by this account");
            } else if (selectedSection == SECTION_GROUPS) {
                note.setText("Groups created by this account");
            } else if (selectedSection == SECTION_BOTS) {
                note.setText("Long-press any bot in Joined to pin here");
            } else {
                note.setText("Communities created by this account");
            }
        } else {
            if (selectedSection == SECTION_CHANNELS) {
                note.setText("Channels you have joined");
            } else if (selectedSection == SECTION_GROUPS) {
                note.setText("Groups you have joined");
            } else if (selectedSection == SECTION_BOTS) {
                note.setText("Long-press a bot to add to My Bot");
            } else {
                note.setText("Communities you have joined");
            }
        }
        contentColumn.addView(note, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 10));

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
        } else {
            activeRows = contentColumn;
        }

        MessagesController controller = MessagesController.getInstance(currentAccount);
        ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>();
        HashSet<Long> mergedDialogIds = new HashSet<>();
        ArrayList<TLRPC.Dialog> allDialogs = controller.getAllDialogs();
        if (allDialogs != null) {
            for (int i = 0, n = allDialogs.size(); i < n; i++) {
                TLRPC.Dialog d = allDialogs.get(i);
                if (d != null && mergedDialogIds.add(d.id)) {
                    dialogs.add(d);
                }
            }
        }
        ArrayList<TLRPC.Dialog> mainDialogs = controller.getDialogs(0);
        if (mainDialogs != null) {
            for (int i = 0, n = mainDialogs.size(); i < n; i++) {
                TLRPC.Dialog d = mainDialogs.get(i);
                if (d != null && mergedDialogIds.add(d.id)) {
                    dialogs.add(d);
                }
            }
        }
        ArrayList<TLRPC.Dialog> archiveDialogs = controller.getDialogs(1);
        if (archiveDialogs != null) {
            for (int i = 0, n = archiveDialogs.size(); i < n; i++) {
                TLRPC.Dialog d = archiveDialogs.get(i);
                if (d != null && mergedDialogIds.add(d.id)) {
                    dialogs.add(d);
                }
            }
        }

        int visibleCount = 0;
        int[] sectionCounts = new int[4];
        HashSet<Long> seenChatIds = new HashSet<>();
        HashSet<Long> seenUserIds = new HashSet<>();
        HashSet<Long> seenCommunityIds = new HashSet<>();
        String query = searchField == null || searchField.getText() == null
                ? "" : searchField.getText().toString().trim().toLowerCase(Locale.ROOT);

        for (int i = 0, n = dialogs.size(); i < n; i++) {
            TLRPC.Dialog dialog = dialogs.get(i);
            if (dialog == null || DialogObject.isEncryptedDialog(dialog.id)) continue;
            TLRPC.Chat chat = null;
            TLRPC.User user = null;
            int type;
            if (dialog instanceof TLRPC.TL_dialogCommunity) {
                chat = controller.getChat(((TLRPC.TL_dialogCommunity) dialog).community_id);
                if (chat == null || ChatObject.isNotInChat(chat) || chat.min) continue;
                seenCommunityIds.add(chat.id);
                type = SECTION_COMMUNITIES;
            } else if (DialogObject.isChatDialog(dialog.id)) {
                chat = controller.getChat(-dialog.id);
                if (chat == null || ChatObject.isNotInChat(chat) || chat.migrated_to != null || chat.min) continue;
                if (!seenChatIds.add(chat.id)) continue;
                type = ChatObject.isChannel(chat) && !chat.megagroup ? SECTION_CHANNELS : SECTION_GROUPS;
            } else if (DialogObject.isUserDialog(dialog.id)) {
                user = controller.getUser(dialog.id);
                if (user == null || !user.bot) continue;
                if (!seenUserIds.add(user.id)) continue;
                type = SECTION_BOTS;
            } else {
                continue;
            }
            boolean owned = chat != null && chat.creator;
            if (type != SECTION_BOTS) {
                // Strict separation:
                // - In My Created (mineOnly == true), ONLY include chats this user created (owned == true).
                // - In Joined (mineOnly == false), ONLY include chats this user joined from others (!owned).
                if (mineOnly && !owned) continue;
                if (!mineOnly && owned) continue;
            } else {
                if (mineOnly && (user == null || !isMarkedMyBot(user.id))) continue;
            }
            String title = chat != null ? (chat.title == null ? "" : chat.title)
                    : (user.first_name == null ? "" : user.first_name) + (user.last_name == null ? "" : " " + user.last_name);
            MessageObject lastMessage = null;
            ArrayList<MessageObject> messages = controller.dialogMessage.get(dialog.id);
            if (messages != null && !messages.isEmpty()) lastMessage = messages.get(0);
            String preview = lastMessage == null || lastMessage.messageText == null
                    ? (type == SECTION_BOTS ? "Bot account" : buildChatFallbackPreview(chat, type == SECTION_CHANNELS))
                    : lastMessage.messageText.toString().trim();
            if (!query.isEmpty() && !title.toLowerCase(Locale.ROOT).contains(query)
                    && !preview.toLowerCase(Locale.ROOT).contains(query)) continue;
            sectionCounts[type]++;
            if (type != selectedSection) continue;
            if (visibleCount < 150 || !query.isEmpty()) {
                addDialogRow(dialog, chat, user, title, preview, lastMessage);
            }
            visibleCount++;
        }

        // Only merge extraChats from getAdminedPublicChannels / getAllChats when looking at My Created (owned == true),
        // so Joined Channels & Joined Groups strictly reflect the user's actual joined dialog list!
        if (mineOnly) {
            HashMap<Long, TLRPC.Chat> fetchedMap = extraChatsByAccount.get(currentAccount);
            if (fetchedMap != null && !fetchedMap.isEmpty()) {
                for (TLRPC.Chat chat : fetchedMap.values()) {
                    if (chat == null || !chat.creator || ChatObject.isNotInChat(chat) || chat.migrated_to != null) continue;
                    if (seenChatIds.contains(chat.id) || seenCommunityIds.contains(chat.id)) continue;
                    seenChatIds.add(chat.id);
                    boolean isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
                    int type = isChannel ? SECTION_CHANNELS : SECTION_GROUPS;
                    String title = chat.title == null ? "" : chat.title;
                    MessageObject lastMessage = null;
                    ArrayList<MessageObject> messages = controller.dialogMessage.get(-chat.id);
                    if (messages != null && !messages.isEmpty()) lastMessage = messages.get(0);
                    String preview = lastMessage == null || lastMessage.messageText == null
                            ? buildChatFallbackPreview(chat, isChannel)
                            : lastMessage.messageText.toString().trim();
                    if (!query.isEmpty() && !title.toLowerCase(Locale.ROOT).contains(query)
                            && !preview.toLowerCase(Locale.ROOT).contains(query)) continue;
                    sectionCounts[type]++;
                    if (type != selectedSection) continue;
                    if (visibleCount < 150 || !query.isEmpty()) {
                        addDialogRow(null, chat, null, title, preview, lastMessage);
                    }
                    visibleCount++;
                }
            }
        }

        updateTabCounts(sectionCounts);

        boolean stillLoadingMore = controller.isLoadingDialogs(0)
                || allChatsInFlightForAccount.get(currentAccount)
                || adminedChatsInFlightForAccount.get(currentAccount);

        if (visibleCount == 0) {
            TextView empty = new TextView(getContext());
            empty.setText(!query.isEmpty() ? "No matches for \"" + query + "\"."
                    : stillLoadingMore ? "Syncing your channels, groups, bots & communities..."
                    : selectedSection == SECTION_BOTS && mineOnly
                    ? "No bots marked as yours yet. Switch to Joined (Other's) -> Bots, long-press your bot chat, and mark it as My Bot."
                    : selectedSection == SECTION_COMMUNITIES
                    ? (mineOnly ? "You have not created any Telegram Communities yet." : "No joined Telegram Communities in your chat list.")
                    : mineOnly
                    ? (selectedSection == SECTION_CHANNELS ? "You have not created any channels on this account yet." : "You have not created any groups on this account yet.")
                    : (selectedSection == SECTION_CHANNELS ? "No joined channels found in your chat list." : "No joined groups found in your chat list."));
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

    private String buildChatFallbackPreview(TLRPC.Chat chat, boolean isChannel) {
        if (chat == null) return "No recent messages";
        return isChannel ? "Tap to open channel" : "Tap to open group";
    }

    private void updateTabCounts(int[] sectionCounts) {
        if (tabs == null || sectionCounts == null || sectionCounts.length < 4) return;
        String[] baseLabels = mineOnly
                ? new String[]{"My Channels", "My Groups", "My Bots", "Communities"}
                : new String[]{"Joined Channels", "Joined Groups", "Bots", "Communities"};
        for (int i = 0; i < tabs.length && i < baseLabels.length; i++) {
            tabs[i].setText(baseLabels[i] + " (" + sectionCounts[i] + ")");
        }
    }

    private void updateTabStyles() {
        if (tabs == null) return;
        boolean isDark = Theme.isCurrentThemeDark();
        int cardBg = isDark ? 0xFF1E293B : Theme.getColor(Theme.key_windowBackgroundWhite);
        for (int i = 0; i < tabs.length; i++) {
            boolean selected = i == selectedSection;
            tabs[i].setTextSize(12);
            tabs[i].setSingleLine(true);
            tabs[i].setEllipsize(android.text.TextUtils.TruncateAt.END);
            tabs[i].setPadding(AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6), 0);
            tabs[i].setTextColor(selected ? (isDark ? 0xFF38BDF8 : COLOR_CYAN) : (isDark ? 0xFF94A3B8 : 0xFF64748B));
            tabs[i].setAlpha(selected ? 1f : 0.88f);
            GradientDrawable tabBg = new GradientDrawable();
            tabBg.setCornerRadius(AndroidUtilities.dp(12));
            tabBg.setColor(selected ? (isDark ? 0x260EA5E9 : 0x160284C7) : cardBg);
            if (selected) {
                tabBg.setStroke(AndroidUtilities.dp(1), isDark ? 0x6638BDF8 : 0x440284C7);
            }
            tabs[i].setBackground(tabBg);
        }
    }

    private void addDialogRow(TLRPC.Dialog dialog, TLRPC.Chat chat, TLRPC.User user, String titleText, String previewText, MessageObject lastMessage) {
        Context context = getContext();
        if (context == null || activeRows == null) return;
        boolean isDark = Theme.isCurrentThemeDark();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(horizontalCards ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        row.setGravity(horizontalCards ? Gravity.TOP : Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(13), AndroidUtilities.dp(14), AndroidUtilities.dp(13));

        AvatarDrawable avatarDrawable = new AvatarDrawable();
        BackupImageView avatar = new BackupImageView(context);
        avatar.setRoundRadius(AndroidUtilities.dp(25));
        if (chat != null) {
            avatarDrawable.setInfo(currentAccount, chat);
            avatar.setForUserOrChat(chat, avatarDrawable);
        } else if (user != null) {
            avatarDrawable.setInfo(currentAccount, user);
            avatar.setForUserOrChat(user, avatarDrawable);
        }
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(50), AndroidUtilities.dp(50));
        if (horizontalCards) avatarParams.bottomMargin = AndroidUtilities.dp(10);
        else avatarParams.rightMargin = AndroidUtilities.dp(13);
        row.addView(avatar, avatarParams);

        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);

        if (horizontalCards) {
            TextView title = new TextView(context);
            title.setText(titleText.isEmpty() ? "Untitled" : titleText);
            title.setTextSize(15.5f);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            if (chat != null) {
                title.setTextColor(isDark ? 0xFF38BDF8 : COLOR_CYAN);
            } else {
                title.setTextColor(isDark ? 0xFFF8FAFC : Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            }
            title.setMaxLines(1);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textColumn.addView(title, LayoutHelper.createLinear(AndroidUtilities.dp(210), LayoutHelper.WRAP_CONTENT));
        } else {
            LinearLayout topLine = new LinearLayout(context);
            topLine.setOrientation(LinearLayout.HORIZONTAL);
            topLine.setGravity(Gravity.CENTER_VERTICAL);

            TextView title = new TextView(context);
            title.setText(titleText.isEmpty() ? "Untitled" : titleText);
            title.setTextSize(15.5f);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            if (chat != null) {
                title.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
            } else {
                title.setTextColor(isDark ? 0xFFF8FAFC : Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            }
            title.setMaxLines(1);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            topLine.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            String badgeLabel = null;
            int badgeBgColor = 0;
            int badgeTextColor = 0;
            int badgeStrokeColor = 0;
            if (chat != null) {
                if (chat.creator) {
                    badgeLabel = "✦ Creator";
                    badgeBgColor = isDark ? 0x260EA5E9 : 0x140284C7;
                    badgeTextColor = isDark ? 0xFF38BDF8 : 0xFF0284C7;
                    badgeStrokeColor = isDark ? 0x5538BDF8 : 0x330284C7;
                } else if (ChatObject.hasAdminRights(chat)) {
                    badgeLabel = "Admin";
                    badgeBgColor = isDark ? 0x2410B981 : 0x140D9488;
                    badgeTextColor = isDark ? 0xFF34D399 : 0xFF0F766E;
                    badgeStrokeColor = isDark ? 0x4410B981 : 0x280D9488;
                } else {
                    boolean isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
                    badgeLabel = isChannel ? "Channel" : "Group";
                    badgeBgColor = isDark ? 0x2064748B : 0x0F475569;
                    badgeTextColor = isDark ? 0xFF94A3B8 : 0xFF64748B;
                    badgeStrokeColor = isDark ? 0x3064748B : 0x1E475569;
                }
            } else if (user != null && user.bot) {
                boolean marked = isMarkedMyBot(user.id);
                badgeLabel = marked ? "My Bot" : "Bot";
                badgeBgColor = marked ? (isDark ? 0x2810B981 : 0x160D9488) : (isDark ? 0x268B5CF6 : 0x147C3AED);
                badgeTextColor = marked ? (isDark ? 0xFF34D399 : 0xFF0F766E) : (isDark ? 0xFFC4B5FD : 0xFF6D28D9);
                badgeStrokeColor = marked ? (isDark ? 0x4810B981 : 0x300D9488) : (isDark ? 0x3A8B5CF6 : 0x247C3AED);
            }
            if (badgeLabel != null) {
                TextView badge = new TextView(context);
                badge.setText(badgeLabel);
                badge.setTextSize(10.5f);
                badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                badge.setTextColor(badgeTextColor);
                badge.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
                GradientDrawable badgeBg = new GradientDrawable();
                badgeBg.setCornerRadius(AndroidUtilities.dp(10));
                badgeBg.setColor(badgeBgColor);
                badgeBg.setStroke(AndroidUtilities.dp(1), badgeStrokeColor);
                badge.setBackground(badgeBg);
                LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                blp.leftMargin = AndroidUtilities.dp(8);
                topLine.addView(badge, blp);
            }
            textColumn.addView(topLine, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            if (chat != null && (chat.participants_count > 0 || (chat.username != null && !chat.username.isEmpty()))) {
                StringBuilder meta = new StringBuilder();
                if (chat.participants_count > 0) {
                    boolean isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
                    meta.append(LocaleController.formatNumber(chat.participants_count, ',')).append(isChannel ? " subscribers" : " members");
                }
                if (chat.username != null && !chat.username.isEmpty()) {
                    if (meta.length() > 0) meta.append(" · ");
                    meta.append("@").append(chat.username);
                }
                TextView metaView = new TextView(context);
                metaView.setText(meta.toString());
                metaView.setTextSize(12);
                metaView.setTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
                metaView.setMaxLines(1);
                metaView.setEllipsize(android.text.TextUtils.TruncateAt.END);
                textColumn.addView(metaView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 3, 0, 0));
            }
        }

        TextView preview = new TextView(context);
        preview.setText(previewText);
        preview.setTextSize(13.5f);
        boolean isLiveStream = previewText != null && (previewText.startsWith("Live stream") || previewText.toLowerCase(Locale.ROOT).contains("live stream"));
        if (isLiveStream) {
            preview.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
            preview.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        } else {
            preview.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF475569);
        }
        preview.setMaxLines(horizontalCards ? 2 : 1);
        preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textColumn.addView(preview, LayoutHelper.createLinear(horizontalCards ? AndroidUtilities.dp(210) : LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
        row.addView(textColumn, horizontalCards
                ? LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT)
                : new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (!horizontalCards) {
            TextView chevron = new TextView(context);
            chevron.setText("›");
            chevron.setTextSize(20);
            chevron.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            chevron.setTextColor(isDark ? 0x8038BDF8 : 0x800284C7);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.leftMargin = AndroidUtilities.dp(8);
            row.addView(chevron, clp);
        }

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
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(AndroidUtilities.dp(16));
        cardBg.setColor(isDark ? 0xFF1E293B : Theme.getColor(Theme.key_windowBackgroundWhite));
        cardBg.setStroke(AndroidUtilities.dp(1), isDark ? 0x2638BDF8 : 0x160284C7);
        row.setBackground(cardBg);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            row.setForeground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        }
        if (horizontalCards) {
            rowParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(260), ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.rightMargin = AndroidUtilities.dp(10);
        } else {
            rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = AndroidUtilities.dp(10);
            row.setElevation(AndroidUtilities.dp(1));
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
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{color, Color.argb(230, Color.red(color), Color.green(color), Color.blue(color))});
        gd.setCornerRadius(AndroidUtilities.dp(16));
        gd.setStroke(AndroidUtilities.dp(1), 0x24FFFFFF);
        button.setBackground(gd);
        button.setElevation(AndroidUtilities.dp(2));
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
