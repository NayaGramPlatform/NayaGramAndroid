package org.telegram.ui;

import android.content.Context;
import android.graphics.Typeface;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.Locale;

/** NG Hub: a read-only view of loaded channel, group, and bot dialogs. */
public class NGHubActivity extends BaseFragment {
    private static final int SECTION_CHANNELS = 0;
    private static final int SECTION_GROUPS = 1;
    private static final int SECTION_BOTS = 2;

    private LinearLayout contentColumn;
    private LinearLayout activeRows;
    private TextView[] tabs;
    private TextView swipeToggle;
    private TextView layoutToggle;
    private EditText searchField;
    private int selectedSection = SECTION_CHANNELS;
    private boolean swipeNavigationEnabled = true;
    private boolean horizontalCards;
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
                    finishFragment();
                }
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12),
                AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        root.addView(page, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        TextView heading = new TextView(context);
        heading.setText("Your communities, in one place");
        heading.setTextSize(18);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        page.addView(heading, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        searchField = new EditText(context);
        searchField.setSingleLine(true);
        searchField.setTextSize(15);
        searchField.setHint("Search channels, groups, and bots");
        searchField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        searchField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        searchField.setPadding(AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14), 0);
        searchField.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14),
                Theme.getColor(Theme.key_windowBackgroundWhite)));
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
        tabRow.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(tabRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 46, 0, 0, 0, 10));
        String[] labels = {"Channels", "Groups", "Bots"};
        tabs = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int section = i;
            tabs[i] = createTab(context, labels[i]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) lp.leftMargin = AndroidUtilities.dp(5);
            tabRow.addView(tabs[i], lp);
            tabs[i].setOnClickListener(v -> showSection(section));
        }

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 42, 0, 0, 0, 8));
        TextView createGroup = createAction(context, "+ Create Group");
        TextView createChannel = createAction(context, "+ Create Channel");
        actions.addView(createGroup, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        LinearLayout.LayoutParams channelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        channelParams.leftMargin = AndroidUtilities.dp(8);
        actions.addView(createChannel, channelParams);
        createGroup.setOnClickListener(v -> presentFragment(new GroupCreateActivity(new Bundle())));
        createChannel.setOnClickListener(v -> presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_CHANNEL_CREATE)));

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

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        page.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f));
        contentColumn = new LinearLayout(context);
        contentColumn.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(contentColumn, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
                    showSection(Math.max(SECTION_CHANNELS, Math.min(SECTION_BOTS, next)));
                }
            }
            return false;
        });

        fragmentView = root;
        showSection(selectedSection);
        return fragmentView;
    }

    private TextView createAction(Context context, String label) {
        TextView action = new TextView(context);
        action.setText(label);
        action.setTextSize(13);
        action.setGravity(Gravity.CENTER);
        action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        action.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        action.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12),
                Theme.getColor(Theme.key_windowBackgroundWhite)));
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
        tab.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12),
                Theme.getColor(Theme.key_windowBackgroundWhite)));
        return tab;
    }

    private void showSection(int section) {
        selectedSection = Math.max(SECTION_CHANNELS, Math.min(SECTION_BOTS, section));
        if (contentColumn == null || getContext() == null) return;
        updateTabStyles();
        contentColumn.removeAllViews();

        if (horizontalCards) {
            HorizontalScrollView horizontal = new HorizontalScrollView(getContext());
            horizontal.setHorizontalScrollBarEnabled(false);
            activeRows = new LinearLayout(getContext());
            activeRows.setOrientation(LinearLayout.HORIZONTAL);
            horizontal.addView(activeRows, new HorizontalScrollView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            contentColumn.addView(horizontal, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        } else {
            activeRows = contentColumn;
        }

        MessagesController controller = MessagesController.getInstance(currentAccount);
        ArrayList<TLRPC.Dialog> dialogs = controller.getDialogs(0);
        if (dialogs == null || dialogs.isEmpty()) dialogs = controller.getAllDialogs();
        int visibleCount = 0;
        String query = searchField == null || searchField.getText() == null
                ? "" : searchField.getText().toString().trim().toLowerCase(Locale.ROOT);

        for (int i = 0; i < dialogs.size(); i++) {
            TLRPC.Dialog dialog = dialogs.get(i);
            if (dialog == null || DialogObject.isEncryptedDialog(dialog.id)) continue;
            TLRPC.Chat chat = null;
            TLRPC.User user = null;
            int type;
            if (DialogObject.isChatDialog(dialog.id)) {
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

        if (visibleCount == 0) {
            TextView empty = new TextView(getContext());
            empty.setText(!query.isEmpty() ? "No matches for \"" + query + "\"."
                    : selectedSection == SECTION_CHANNELS
                    ? "No channels found in the currently loaded chat list."
                    : selectedSection == SECTION_GROUPS
                    ? "No groups found in the currently loaded chat list."
                    : "No bot chats found in the currently loaded chat list.");
            empty.setTextSize(15);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(40),
                    AndroidUtilities.dp(24), AndroidUtilities.dp(40));
            empty.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            contentColumn.addView(empty, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
    }

    private void updateTabStyles() {
        if (tabs == null) return;
        int active = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
        int inactive = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setTextColor(i == selectedSection ? active : inactive);
            tabs[i].setAlpha(i == selectedSection ? 1f : 0.78f);
        }
    }

    private void addDialogRow(TLRPC.Dialog dialog, TLRPC.Chat chat, TLRPC.User user,
                              String titleText, String previewText, MessageObject lastMessage) {
        Context context = getContext();
        if (context == null || activeRows == null) return;
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(horizontalCards ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        row.setGravity(horizontalCards ? Gravity.TOP : Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12),
                AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));

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
        if (horizontalCards) {
            avatarParams.bottomMargin = AndroidUtilities.dp(10);
        } else {
            avatarParams.rightMargin = AndroidUtilities.dp(12);
        }
        row.addView(avatar, avatarParams);

        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(context);
        title.setText(titleText.isEmpty() ? "Untitled" : titleText);
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textColumn.addView(title, LayoutHelper.createLinear(horizontalCards ? AndroidUtilities.dp(210) : LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT));

        TextView preview = new TextView(context);
        preview.setText(previewText);
        preview.setTextSize(13);
        preview.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        preview.setMaxLines(horizontalCards ? 3 : 2);
        preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textColumn.addView(preview, LayoutHelper.createLinear(horizontalCards ? AndroidUtilities.dp(210) : LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
        if (!horizontalCards) {
            textColumn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
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

        LinearLayout.LayoutParams rowParams;
        if (horizontalCards) {
            rowParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(260), ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.rightMargin = AndroidUtilities.dp(10);
            row.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16),
                    Theme.getColor(Theme.key_windowBackgroundWhite)));
        } else {
            rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        activeRows.addView(row, rowParams);
        if (!horizontalCards) {
            View divider = new View(context);
            divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
            activeRows.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(1)));
        }
    }
}
