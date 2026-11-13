package org.nayagram.platform.history;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.nayagram.platform.NayaConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NGMessageEditHistory - Captures, persists, and displays the full edit history timeline
 * (Original -> Edit #1 -> Edit #2 -> Current) for edited messages across all chats, groups, and channels.
 */
public final class NGMessageEditHistory {

    private static final String PREF_NAME = "nayagram_edit_history_prefs";
    private static final int MAX_REVISIONS_PER_MESSAGE = 25;
    private static final Gson GSON = new Gson();
    private static final ConcurrentHashMap<String, List<RevisionEntry>> MEMORY_CACHE = new ConcurrentHashMap<>();

    public static class RevisionEntry {
        public String text;
        public int timestamp;
        public boolean isOriginal;

        public RevisionEntry() {}

        public RevisionEntry(String text, int timestamp, boolean isOriginal) {
            this.text = text != null ? text : "";
            this.timestamp = timestamp;
            this.isOriginal = isOriginal;
        }
    }

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static String makeKey(int accountId, long dialogId, int messageId) {
        return accountId + "_" + dialogId + "_" + messageId;
    }

    /**
     * Records a message edit when oldMsg text differs from newMsg text.
     * Safe to call from both MessagesStorage (SQLite thread) and MessagesController (StageQueue).
     */
    public static synchronized void recordEdit(int accountId, long dialogId, TLRPC.Message oldMsg, TLRPC.Message newMsg) {
        try {
            if (!NayaConfig.getInstance().isSaveEditHistoryEnabled()) {
                return;
            }
            if (oldMsg == null || newMsg == null || newMsg.id <= 0) {
                return;
            }
            String oldText = oldMsg.message != null ? oldMsg.message : "";
            String newText = newMsg.message != null ? newMsg.message : "";

            // Ignore non-text edits (such as reaction counts, views, or pin state updates)
            if (TextUtils.equals(oldText, newText)) {
                return;
            }
            if (oldText.isEmpty() && newText.isEmpty()) {
                return;
            }

            if (dialogId == 0) {
                dialogId = MessageObject.getDialogId(newMsg);
            }
            String key = makeKey(accountId, dialogId, newMsg.id);
            List<RevisionEntry> history = new ArrayList<>(getRevisions(accountId, dialogId, newMsg.id));

            if (history.isEmpty()) {
                int origTime = oldMsg.date > 0 ? oldMsg.date : (int) (System.currentTimeMillis() / 1000L);
                history.add(new RevisionEntry(oldText, origTime, true));
            } else {
                RevisionEntry last = history.get(history.size() - 1);
                if (TextUtils.equals(last.text, newText)) {
                    return; // Already recorded this exact edit
                }
                if (!TextUtils.equals(last.text, oldText) && !oldText.isEmpty()) {
                    int prevTime = oldMsg.edit_date > 0 ? oldMsg.edit_date : (oldMsg.date > 0 ? oldMsg.date : (int) (System.currentTimeMillis() / 1000L));
                    history.add(new RevisionEntry(oldText, prevTime, false));
                }
            }

            int editTime = newMsg.edit_date > 0 ? newMsg.edit_date : (int) (System.currentTimeMillis() / 1000L);
            history.add(new RevisionEntry(newText, editTime, false));

            while (history.size() > MAX_REVISIONS_PER_MESSAGE) {
                // Keep the original entry at index 0, trim oldest intermediate edits
                history.remove(1);
            }

            MEMORY_CACHE.put(key, history);
            getPrefs().edit().putString(key, GSON.toJson(history)).apply();
        } catch (Throwable ignored) {}
    }

    public static List<RevisionEntry> getRevisions(int accountId, long dialogId, int messageId) {
        String key = makeKey(accountId, dialogId, messageId);
        List<RevisionEntry> cached = MEMORY_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            String json = getPrefs().getString(key, null);
            if (!TextUtils.isEmpty(json)) {
                Type type = new TypeToken<List<RevisionEntry>>() {}.getType();
                List<RevisionEntry> loaded = GSON.fromJson(json, type);
                if (loaded != null && !loaded.isEmpty()) {
                    MEMORY_CACHE.put(key, loaded);
                    return loaded;
                }
            }
        } catch (Throwable ignored) {}
        return new ArrayList<>();
    }

    public static boolean hasHistory(int accountId, long dialogId, int messageId) {
        return !getRevisions(accountId, dialogId, messageId).isEmpty();
    }

    public static int getEditCount(int accountId, long dialogId, int messageId) {
        List<RevisionEntry> list = getRevisions(accountId, dialogId, messageId);
        return Math.max(0, list.size() - 1);
    }

    public static MessageObject resolveHistoryTarget(int accountId, MessageObject selectedObject, MessageObject.GroupedMessages group) {
        if (selectedObject == null) {
            return null;
        }
        if (getEditCount(accountId, selectedObject.getDialogId(), selectedObject.getId()) > 0) {
            return selectedObject;
        }
        if (group != null && group.messages != null) {
            for (int i = 0; i < group.messages.size(); i++) {
                MessageObject m = group.messages.get(i);
                if (m != null && getEditCount(accountId, m.getDialogId(), m.getId()) > 0) {
                    return m;
                }
            }
            for (int i = 0; i < group.messages.size(); i++) {
                MessageObject m = group.messages.get(i);
                if (m != null && m.messageOwner != null && !TextUtils.isEmpty(m.messageOwner.message)
                        && ((m.messageOwner.flags & TLRPC.MESSAGE_FLAG_EDITED) != 0 || m.messageOwner.edit_date > 0)) {
                    return m;
                }
            }
            for (int i = 0; i < group.messages.size(); i++) {
                MessageObject m = group.messages.get(i);
                if (m != null && m.messageOwner != null
                        && ((m.messageOwner.flags & TLRPC.MESSAGE_FLAG_EDITED) != 0 || m.messageOwner.edit_date > 0)) {
                    return m;
                }
            }
        }
        return selectedObject;
    }

    /**
     * Shows the interactive NayaGram Message Edit History Timeline Dialog.
     */
    public static void showHistoryDialog(Context context, int accountId, MessageObject messageObject) {
        if (context == null || messageObject == null || messageObject.messageOwner == null) {
            return;
        }
        final boolean isDark = Theme.isCurrentThemeDark();
        final long dialogId = messageObject.getDialogId();
        final int messageId = messageObject.getId();
        List<RevisionEntry> revisions = new ArrayList<>(getRevisions(accountId, dialogId, messageId));

        String currentText = messageObject.messageOwner.message != null ? messageObject.messageOwner.message : "";
        int currentEditDate = messageObject.messageOwner.edit_date > 0
                ? messageObject.messageOwner.edit_date
                : messageObject.messageOwner.date;

        if (revisions.isEmpty()) {
            revisions.add(new RevisionEntry(currentText, currentEditDate, false));
        } else {
            RevisionEntry last = revisions.get(revisions.size() - 1);
            if (!TextUtils.equals(last.text, currentText) && !currentText.isEmpty()) {
                revisions.add(new RevisionEntry(currentText, currentEditDate, false));
            }
        }

        LinearLayout outer = new LinearLayout(context);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(4));

        // Summary Header
        TextView subHeader = new TextView(context);
        int editCount = Math.max(0, revisions.size() - 1);
        if (editCount > 0) {
            subHeader.setText("Captured " + editCount + (editCount == 1 ? " edit" : " edits") + " (" + revisions.size() + " versions) • Tap any card to copy");
        } else {
            subHeader.setText("Edited before NayaGram captured the original text • Tap card to copy");
        }
        subHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        subHeader.setTypeface(AndroidUtilities.bold());
        subHeader.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1E293B);
        subHeader.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), AndroidUtilities.dp(8));
        outer.addView(subHeader);

        final int maxScrollHeight = Math.min(
                AndroidUtilities.dp(340),
                Math.max(AndroidUtilities.dp(240), (int) (AndroidUtilities.displaySize.y * 0.45f))
        );
        ScrollView scrollView = new ScrollView(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int capped = MeasureSpec.makeMeasureSpec(maxScrollHeight, MeasureSpec.AT_MOST);
                super.onMeasure(widthMeasureSpec, capped);
            }
        };
        scrollView.setVerticalScrollBarEnabled(true);
        scrollView.setScrollbarFadingEnabled(false);

        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(AndroidUtilities.dp(2), AndroidUtilities.dp(2), AndroidUtilities.dp(6), AndroidUtilities.dp(6));
        scrollView.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        outer.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        final StringBuilder fullLogBuilder = new StringBuilder();

        for (int i = 0; i < revisions.size(); i++) {
            final RevisionEntry entry = revisions.get(i);
            final boolean isFirst = (i == 0 && revisions.size() > 1);
            final boolean isLatest = (i == revisions.size() - 1);

            String badgeLabel;
            int badgeColor;
            if (isFirst) {
                badgeLabel = "ORIGINAL MESSAGE";
                badgeColor = 0xFF10B981; // Emerald Green
            } else if (isLatest) {
                badgeLabel = revisions.size() > 1 ? ("CURRENT (EDIT #" + i + ") ✓") : "CURRENT EDITED VERSION";
                badgeColor = 0xFF6366F1; // Electric Indigo
            } else {
                badgeLabel = "EDIT #" + i;
                badgeColor = 0xFF0284C7; // Sky Blue
            }

            String timeFormatted = entry.timestamp > 0
                    ? LocaleController.formatDateTime(entry.timestamp, true)
                    : "";

            fullLogBuilder.append("[").append(badgeLabel).append(timeFormatted.isEmpty() ? "]" : " • " + timeFormatted + "]")
                    .append("\n").append(entry.text).append("\n\n");

            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(11), AndroidUtilities.dp(14), AndroidUtilities.dp(11));

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setCornerRadius(AndroidUtilities.dp(14));
            if (isFirst) {
                cardBg.setColor(isDark ? 0x2210B981 : 0x1210B981);
                cardBg.setStroke(AndroidUtilities.dp(1.5f), isDark ? 0xFF10B981 : 0xFF059669);
            } else if (isLatest) {
                cardBg.setColor(isDark ? 0x246366F1 : 0x126366F1);
                cardBg.setStroke(AndroidUtilities.dp(1.5f), isDark ? 0xFF818CF8 : 0xFF4F46E5);
            } else {
                cardBg.setColor(isDark ? 0xFF1E293B : 0xFFF8FAFC);
                cardBg.setStroke(AndroidUtilities.dp(1.2f), isDark ? 0xFF334155 : 0xFFCBD5E1);
            }
            card.setBackground(cardBg);

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            cardLp.bottomMargin = AndroidUtilities.dp(8);
            card.setLayoutParams(cardLp);

            // Top row: Badge + Timestamp
            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView badge = new TextView(context);
            badge.setText(badgeLabel);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f);
            badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            badge.setTextColor(Color.WHITE);
            badge.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
            GradientDrawable bBg = new GradientDrawable();
            bBg.setCornerRadius(AndroidUtilities.dp(6));
            bBg.setColor(badgeColor);
            badge.setBackground(bBg);
            topRow.addView(badge);

            TextView timeView = new TextView(context);
            timeView.setText(timeFormatted);
            timeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
            timeView.setTypeface(AndroidUtilities.bold());
            timeView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            timeView.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF475569);
            topRow.addView(timeView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            card.addView(topRow);

            // Message content text
            TextView bodyView = new TextView(context);
            bodyView.setText(TextUtils.isEmpty(entry.text) ? "(Media / Empty text)" : entry.text);
            bodyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f);
            bodyView.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
            bodyView.setPadding(0, AndroidUtilities.dp(8), 0, 0);
            card.addView(bodyView);

            card.setOnClickListener(v -> {
                AndroidUtilities.addToClipboard(entry.text);
                Toast.makeText(context, badgeLabel + " copied ✓", Toast.LENGTH_SHORT).show();
            });

            list.addView(card);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Message Edit History");
        builder.setView(outer);
        builder.setPositiveButton("Copy Timeline", (d, w) -> {
            AndroidUtilities.addToClipboard(fullLogBuilder.toString().trim());
            Toast.makeText(context, "Full edit history copied ✓", Toast.LENGTH_SHORT).show();
            d.dismiss();
        });
        builder.setNegativeButton("Close", (d, w) -> d.dismiss());
        builder.show();
    }
}
